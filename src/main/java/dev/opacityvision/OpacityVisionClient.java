package dev.opacityvision;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public final class OpacityVisionClient implements ClientModInitializer {
	public static final String MOD_ID = "opacityvision";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final float STEP = 0.05f;

	private static KeyMapping toggleKey;
	private static KeyMapping moreKey;
	private static KeyMapping lessKey;
	private static KeyMapping outlineKey;
	private static KeyMapping markKey;
	private static KeyMapping screenKey;

	/** Ticks until chunks are rebuilt (debounced so held keys don't rebuild every tick). -1 = nothing pending. */
	private static int reloadCountdown = -1;

	@Override
	public void onInitializeClient() {
		Config.load();

		// Wrap every block model so we can change its opacity at chunk-build time.
		ModelLoadingPlugin.register(pluginContext ->
				pluginContext.modifyBlockModelAfterBake().register(
						ModelModifier.WRAP_LAST_PHASE,
						(model, context) -> new OpacityModel(model)));

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		toggleKey = key("toggle", GLFW.GLFW_KEY_X, category);
		moreKey = key("more", GLFW.GLFW_KEY_RIGHT_BRACKET, category);
		lessKey = key("less", GLFW.GLFW_KEY_LEFT_BRACKET, category);
		outlineKey = key("outline", GLFW.GLFW_KEY_O, category);
		markKey = key("mark", GLFW.GLFW_KEY_L, category);
		screenKey = key("screen", GLFW.GLFW_KEY_K, category);

		ClientTickEvents.END_CLIENT_TICK.register(OpacityVisionClient::tick);
		LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(OutlineRenderer::render);
	}

	private static KeyMapping key(String name, int glfwKey, KeyMapping.Category category) {
		return KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key." + MOD_ID + "." + name, InputConstants.Type.KEYSYM, glfwKey, category));
	}

	/** Rebuild all chunk meshes soon (after settings changed). */
	public static void requestReload(int delayTicks) {
		reloadCountdown = Math.max(0, delayTicks);
	}

	private static void tick(Minecraft mc) {
		Config cfg = Config.get();

		while (toggleKey.consumeClick()) {
			cfg.enabled = !cfg.enabled;
			message(mc, "Opacity Vision: " + (cfg.enabled ? "ON" : "OFF"));
			changed(8);
		}
		while (moreKey.consumeClick()) {
			adjustOpacity(mc, cfg, +STEP);
		}
		while (lessKey.consumeClick()) {
			adjustOpacity(mc, cfg, -STEP);
		}
		while (outlineKey.consumeClick()) {
			cfg.outline = !cfg.outline;
			message(mc, "Outlines: " + (cfg.outline ? "ON" : "OFF"));
			Config.save();
		}
		while (markKey.consumeClick()) {
			toggleLookedAtBlock(mc, cfg);
		}
		while (screenKey.consumeClick()) {
			mc.setScreen(new ConfigScreen(mc.screen));
		}

		OutlineRenderer.tick(mc);

		if (reloadCountdown > 0) {
			reloadCountdown--;
		} else if (reloadCountdown == 0) {
			reloadCountdown = -1;
			if (mc.level != null) {
				mc.levelRenderer.allChanged();
			}
		}
	}

	private static void adjustOpacity(Minecraft mc, Config cfg, float delta) {
		cfg.opacity = Config.clamp01(Math.round((cfg.opacity + delta) * 20f) / 20f);
		String suffix = cfg.enabled ? "" : "  (Opacity Vision is OFF - press X)";
		message(mc, "Block opacity: " + Math.round(cfg.opacity * 100f) + "%" + suffix);
		changed(10);
	}

	private static void toggleLookedAtBlock(Minecraft mc, Config cfg) {
		if (mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			message(mc, "Look at a block first");
			return;
		}
		BlockState state = mc.level.getBlockState(hit.getBlockPos());
		String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

		if (cfg.highlightBlocks.remove(id)) {
			message(mc, "No longer highlighting " + id);
		} else if (cfg.highlightOres && id.endsWith("_ore")) {
			message(mc, id + " is already highlighted as an ore (turn off \"Highlight ores\" in settings)");
			return;
		} else {
			cfg.highlightBlocks.add(id);
			message(mc, "Highlighting " + id);
		}
		cfg.invalidateHighlightCache();
		changed(8);
	}

	private static void changed(int reloadDelay) {
		Config.save();
		requestReload(reloadDelay);
	}

	private static void message(Minecraft mc, String text) {
		mc.gui.setOverlayMessage(Component.literal(text), false);
	}
}
