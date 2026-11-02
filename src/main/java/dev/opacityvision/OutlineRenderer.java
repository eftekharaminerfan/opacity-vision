package dev.opacityvision;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

/**
 * Draws a wireframe-style outline around highlighted blocks near the player.
 * The outline is built from 12 thin boxes per block, using the same debug
 * render type Fabric's own render-event tests use.
 */
public final class OutlineRenderer {
	private static final int MAX_BLOCKS = 3000;
	private static final int SCAN_INTERVAL_TICKS = 10;
	private static final float THICKNESS = 0.035f;
	private static final float LO = -0.004f;
	private static final float HI = 1.004f;

	private static final long[] found = new long[MAX_BLOCKS];
	private static volatile int foundCount = 0;
	private static int cooldown = 0;

	private OutlineRenderer() {
	}

	/** Called every client tick: periodically rescans for highlighted blocks. */
	public static void tick(Minecraft mc) {
		Config cfg = Config.get();
		if (!cfg.enabled || !cfg.outline || mc.level == null || mc.player == null) {
			foundCount = 0;
			cooldown = 0;
			return;
		}
		if (--cooldown > 0) {
			return;
		}
		cooldown = SCAN_INTERVAL_TICKS;

		BlockPos center = mc.player.blockPosition();
		int r = cfg.outlineRange;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		long[] buffer = new long[MAX_BLOCKS];
		int n = 0;

		scan:
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					BlockState state = mc.level.getBlockState(cursor);
					if (state.isAir() || !cfg.isHighlighted(state)) {
						continue;
					}
					buffer[n++] = cursor.asLong();
					if (n >= MAX_BLOCKS) {
						break scan;
					}
				}
			}
		}

		System.arraycopy(buffer, 0, found, 0, n);
		foundCount = n;
	}

	/** Registered on LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN. */
	public static void render(LevelRenderContext context) {
		Config cfg = Config.get();
		int n = foundCount;
		if (!cfg.enabled || !cfg.outline || n == 0) {
			return;
		}

		PoseStack poseStack = context.poseStack();
		Vec3 cam = context.levelState().cameraRenderState.pos;
		VertexConsumer consumer = context.bufferSource().getBuffer(RenderTypes.debugFilledBox());
		int color = 0xFF000000 | cfg.outlineRgb();

		poseStack.pushPose();
		poseStack.translate(-cam.x, -cam.y, -cam.z);
		for (int i = 0; i < n; i++) {
			long packed = found[i];
			drawEdges(poseStack, consumer, BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed), color);
		}
		poseStack.popPose();
	}

	private static void drawEdges(PoseStack ps, VertexConsumer vc, int bx, int by, int bz, int color) {
		float t = THICKNESS;
		float[] near = {LO, HI - t};

		for (float a : near) {
			for (float b : near) {
				// edges running along X, Y and Z
				box(ps, vc, bx + LO, by + a, bz + b, bx + HI, by + a + t, bz + b + t, color);
				box(ps, vc, bx + a, by + LO, bz + b, bx + a + t, by + HI, bz + b + t, color);
				box(ps, vc, bx + a, by + b, bz + LO, bx + a + t, by + b + t, bz + HI, color);
			}
		}
	}

	private static void box(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
		Matrix4f m = ps.last().pose();

		// North
		vc.addVertex(m, x0, y0, z0).setColor(color);
		vc.addVertex(m, x1, y0, z0).setColor(color);
		vc.addVertex(m, x1, y1, z0).setColor(color);
		vc.addVertex(m, x0, y1, z0).setColor(color);
		// South
		vc.addVertex(m, x1, y0, z1).setColor(color);
		vc.addVertex(m, x0, y0, z1).setColor(color);
		vc.addVertex(m, x0, y1, z1).setColor(color);
		vc.addVertex(m, x1, y1, z1).setColor(color);
		// West
		vc.addVertex(m, x0, y0, z1).setColor(color);
		vc.addVertex(m, x0, y0, z0).setColor(color);
		vc.addVertex(m, x0, y1, z0).setColor(color);
		vc.addVertex(m, x0, y1, z1).setColor(color);
		// East
		vc.addVertex(m, x1, y0, z0).setColor(color);
		vc.addVertex(m, x1, y0, z1).setColor(color);
		vc.addVertex(m, x1, y1, z1).setColor(color);
		vc.addVertex(m, x1, y1, z0).setColor(color);
		// Top
		vc.addVertex(m, x0, y1, z0).setColor(color);
		vc.addVertex(m, x1, y1, z0).setColor(color);
		vc.addVertex(m, x1, y1, z1).setColor(color);
		vc.addVertex(m, x0, y1, z1).setColor(color);
		// Bottom
		vc.addVertex(m, x0, y0, z1).setColor(color);
		vc.addVertex(m, x1, y0, z1).setColor(color);
		vc.addVertex(m, x1, y0, z0).setColor(color);
		vc.addVertex(m, x0, y0, z0).setColor(color);
	}
}
