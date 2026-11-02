package dev.opacityvision;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.loader.api.FabricLoader;

/**
 * User settings, saved to config/opacityvision.json.
 */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("opacityvision.json");
	private static Config instance = new Config();

	/** Outline colours the settings screen cycles through (RGB). */
	public static final int[] OUTLINE_COLORS = {0xFFFFFF, 0xFFE94A, 0x4AF0FF, 0x55FF55, 0xFF5555, 0xFF66FF};

	// ---- saved settings ----
	/** Master switch. */
	public boolean enabled = false;
	/** Opacity of normal blocks, 0.0 (invisible) to 1.0 (vanilla). */
	public float opacity = 0.25f;
	/** Opacity of highlighted blocks (ores etc.), 0.0 to 1.0. */
	public float highlightOpacity = 1.0f;
	/** Treat every block whose id ends in "_ore" as highlighted. */
	public boolean highlightOres = true;
	/** Extra highlighted block ids, e.g. "minecraft:ancient_debris". */
	public List<String> highlightBlocks = new ArrayList<>(List.of("minecraft:ancient_debris", "minecraft:spawner"));
	/** Draw an outline around highlighted blocks. */
	public boolean outline = true;
	/** Index into OUTLINE_COLORS. */
	public int outlineColorIndex = 1;
	/** How far (in blocks) to search for highlighted blocks to outline. */
	public int outlineRange = 24;
	/** Also draw faces between two normal blocks (looks foggy but shows depth; heavier on FPS). */
	public boolean showInnerFaces = false;

	// ---- runtime only ----
	private transient final Map<Block, Boolean> highlightCache = new ConcurrentHashMap<>();

	public static Config get() {
		return instance;
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				Config loaded = GSON.fromJson(reader, Config.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				OpacityVisionClient.LOGGER.warn("Could not read {}, using defaults", FILE, e);
			}
		}
		instance.sanitize();
		save();
	}

	public static void save() {
		try (Writer writer = Files.newBufferedWriter(FILE)) {
			GSON.toJson(instance, writer);
		} catch (IOException e) {
			OpacityVisionClient.LOGGER.warn("Could not save {}", FILE, e);
		}
	}

	private void sanitize() {
		opacity = clamp01(opacity);
		highlightOpacity = clamp01(highlightOpacity);
		if (highlightBlocks == null) {
			highlightBlocks = new ArrayList<>();
		}
		outlineColorIndex = Math.floorMod(outlineColorIndex, OUTLINE_COLORS.length);
		outlineRange = Math.max(4, Math.min(64, outlineRange));
		highlightCache.clear();
	}

	/** Call after changing highlightOres / highlightBlocks. */
	public void invalidateHighlightCache() {
		highlightCache.clear();
	}

	public boolean isHighlighted(BlockState state) {
		return highlightCache.computeIfAbsent(state.getBlock(), this::computeHighlighted);
	}

	private boolean computeHighlighted(Block block) {
		String id = BuiltInRegistries.BLOCK.getKey(block).toString();
		if (highlightOres && id.endsWith("_ore")) {
			return true;
		}
		return highlightBlocks.contains(id);
	}

	public int outlineRgb() {
		return OUTLINE_COLORS[Math.floorMod(outlineColorIndex, OUTLINE_COLORS.length)];
	}

	public static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}
}
