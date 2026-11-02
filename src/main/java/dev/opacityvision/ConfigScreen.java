package dev.opacityvision;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings screen (default key: K). Changes are applied to the world when you close it.
 */
public final class ConfigScreen extends Screen {
	private static final int WIDTH = 200;
	private static final int ROW = 24;

	private final @Nullable Screen parent;
	private final Config cfg = Config.get();

	private Button enabledButton;
	private Button opacityLabel;
	private Button highlightLabel;
	private Button oresButton;
	private Button outlineButton;
	private Button colorButton;
	private Button innerFacesButton;

	public ConfigScreen(@Nullable Screen parent) {
		super(Component.literal("Opacity Vision"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - WIDTH / 2;
		int y = Math.max(10, this.height / 2 - 105);

		Button title = this.addRenderableWidget(Button.builder(this.title, b -> { }).pos(x, y).size(WIDTH, 20).build());
		title.active = false;
		y += ROW;

		enabledButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
			cfg.enabled = !cfg.enabled;
			refresh();
		}).pos(x, y).size(WIDTH, 20).build());
		y += ROW;

		opacityLabel = stepper(x, y, () -> cfg.opacity, v -> cfg.opacity = v);
		y += ROW;

		highlightLabel = stepper(x, y, () -> cfg.highlightOpacity, v -> cfg.highlightOpacity = v);
		y += ROW;

		oresButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
			cfg.highlightOres = !cfg.highlightOres;
			cfg.invalidateHighlightCache();
			refresh();
		}).pos(x, y).size(WIDTH, 20).build());
		y += ROW;

		outlineButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
			cfg.outline = !cfg.outline;
			refresh();
		}).pos(x, y).size(WIDTH, 20).build());
		y += ROW;

		colorButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
			cfg.outlineColorIndex = (cfg.outlineColorIndex + 1) % Config.OUTLINE_COLORS.length;
			refresh();
		}).pos(x, y).size(WIDTH, 20).build());
		y += ROW;

		innerFacesButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
			cfg.showInnerFaces = !cfg.showInnerFaces;
			refresh();
		}).pos(x, y).size(WIDTH, 20).build());
		y += ROW + 6;

		this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
				.pos(x, y).size(WIDTH, 20).build());

		refresh();
	}

	/** A "[-]  label  [+]" row that changes a 0..1 value in 5% steps. Returns the label button. */
	private Button stepper(int x, int y, Supplier<Float> getter, java.util.function.Consumer<Float> setter) {
		Button label = Button.builder(Component.empty(), b -> { }).pos(x + 22, y).size(WIDTH - 44, 20).build();
		label.active = false;

		this.addRenderableWidget(Button.builder(Component.literal("-"), b -> {
			setter.accept(step(getter.get(), -0.05f));
			refresh();
		}).pos(x, y).size(20, 20).build());
		this.addRenderableWidget(label);
		this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
			setter.accept(step(getter.get(), +0.05f));
			refresh();
		}).pos(x + WIDTH - 20, y).size(20, 20).build());
		return label;
	}

	private static float step(float value, float delta) {
		return Config.clamp01(Math.round((value + delta) * 20f) / 20f);
	}

	private void refresh() {
		enabledButton.setMessage(Component.literal("Opacity Vision: " + onOff(cfg.enabled)));
		opacityLabel.setMessage(Component.literal("Block opacity: " + percent(cfg.opacity)));
		highlightLabel.setMessage(Component.literal("Highlight opacity: " + percent(cfg.highlightOpacity)));
		oresButton.setMessage(Component.literal("Highlight ores: " + onOff(cfg.highlightOres)));
		outlineButton.setMessage(Component.literal("Outlines: " + onOff(cfg.outline)));
		colorButton.setMessage(Component.literal(String.format("Outline colour: #%06X", cfg.outlineRgb())));
		innerFacesButton.setMessage(Component.literal("Inner faces: " + onOff(cfg.showInnerFaces)));
	}

	private static String onOff(boolean value) {
		return value ? "ON" : "OFF";
	}

	private static String percent(float value) {
		return Math.round(value * 100f) + "%";
	}

	@Override
	public void onClose() {
		cfg.invalidateHighlightCache();
		Config.save();
		OpacityVisionClient.requestReload(0);
		this.minecraft.setScreen(parent);
	}
}
