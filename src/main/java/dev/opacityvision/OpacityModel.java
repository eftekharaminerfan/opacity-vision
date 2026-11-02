package dev.opacityvision;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;

/**
 * Wraps every block model. While Opacity Vision is on, quads are re-emitted with
 * the configured opacity (or skipped completely when the opacity is 0).
 */
public final class OpacityModel extends WrapperBlockStateModel {
	public OpacityModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random, Predicate<@Nullable Direction> cullTest) {
		Config cfg = Config.get();

		if (!cfg.enabled) {
			wrapped.emitQuads(emitter, level, pos, state, random, cullTest);
			return;
		}

		boolean highlighted = cfg.isHighlighted(state);
		float alpha = highlighted ? cfg.highlightOpacity : cfg.opacity;

		// Fully invisible: emit nothing at all (this is the cheap "x-ray" path).
		if (alpha <= 0.004f) {
			return;
		}

		// Highlighted blocks must show all their faces, otherwise ores buried in
		// stone would be culled. Normal blocks only do so if "inner faces" is on.
		boolean showAllFaces = highlighted || cfg.showInnerFaces;
		boolean translucent = alpha < 0.996f;

		if (!showAllFaces && !translucent) {
			wrapped.emitQuads(emitter, level, pos, state, random, cullTest);
			return;
		}

		final float a = alpha;
		QuadTransform transform = quad -> {
			if (showAllFaces) {
				quad.cullFace(null);
			}
			if (translucent) {
				quad.chunkLayer(ChunkSectionLayer.TRANSLUCENT);
				for (int i = 0; i < 4; i++) {
					int c = quad.color(i);
					int newAlpha = Math.round(((c >>> 24) & 0xFF) * a);
					quad.color(i, (newAlpha << 24) | (c & 0x00FFFFFF));
				}
			}
			return true;
		};

		emitter.pushTransform(transform);
		wrapped.emitQuads(emitter, level, pos, state, random, showAllFaces ? dir -> false : cullTest);
		emitter.popTransform();
	}

	/** The geometry depends on live settings, so don't let the renderer cache it. */
	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random) {
		return Config.get().enabled ? null : wrapped.createGeometryKey(level, pos, state, random);
	}
}
