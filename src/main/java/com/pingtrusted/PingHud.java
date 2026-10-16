package com.pingtrusted;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/**
 * Draws ping markers on the HUD. The block's position is projected onto the screen using the camera and FOV.
 * If the block is off-screen (or behind you) the marker sticks to the screen edge and points the way.
 */
final class PingHud {
	private static final int MARGIN = 14;
	private static final int GOLD = 0xFFFFD700;
	private static final int BLACK = 0xFF000000;
	private static final int WHITE = 0xFFFFFFFF;

	private PingHud() {
	}

	static void render(GuiGraphics g, DeltaTracker tickCounter) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.player == null || mc.level == null) return;

		// Pings from another dimension are never shown (they only appear in chat).
		List<PingMarkers.Marker> markers = PingMarkers.active(PingMarkers.dimensionName(mc.level));
		if (markers.isEmpty()) return;

		Vec3 cam = mc.gameRenderer.getMainCamera().position();
		Vec3 forward = mc.player.getLookAngle();
		Vec3 right = forward.cross(new Vec3(0, 1, 0));

		if (right.lengthSqr() < 1.0E-6) return; // looking straight up/down, skip this frame

		right = right.normalize();
		Vec3 up = right.cross(forward);

		double tanHalfFov = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
		int w = g.guiWidth();
		int h = g.guiHeight();
		double aspect = (double) w / h;
		Font font = mc.font;

		for (PingMarkers.Marker m : markers) {
			Vec3 d = new Vec3(m.x(), m.y(), m.z()).subtract(cam);
			double depth = d.dot(forward);
			double dx = d.dot(right);
			double dy = d.dot(up);
			double dist = d.length();

			double sx = 0;
			double sy = 0;
			boolean onScreen = false;

			if (depth > 0.05) {
				sx = w / 2.0 * (1 + dx / depth / (tanHalfFov * aspect));
				sy = h / 2.0 * (1 - dy / depth / tanHalfFov);
				onScreen = sx >= MARGIN && sx <= w - MARGIN && sy >= MARGIN && sy <= h - MARGIN;
			}

			if (!onScreen) {
				// Push the marker out to the screen edge in the direction of the block.
				double vx = dx;
				double vy = -dy;
				double len = Math.hypot(vx, vy);

				if (len < 1.0E-6) {
					vx = 0;
					vy = 1;
					len = 1;
				}

				vx /= len;
				vy /= len;

				double scaleX = (w / 2.0 - MARGIN) / Math.max(Math.abs(vx), 1.0E-6);
				double scaleY = (h / 2.0 - MARGIN) / Math.max(Math.abs(vy), 1.0E-6);
				double scale = Math.min(scaleX, scaleY);

				sx = w / 2.0 + vx * scale;
				sy = h / 2.0 + vy * scale;
			}

			int ix = (int) sx;
			int iy = (int) sy;

			// Marker: gold square with a black outline.
			g.fill(ix - 5, iy - 5, ix + 6, iy + 6, BLACK);
			g.fill(ix - 4, iy - 4, ix + 5, iy + 5, GOLD);

			String line1 = m.user() + " - " + m.block();
			String line2 = formatDistance(dist);

			int textY = Math.max(2, Math.min(iy + 9, h - 22));
			g.drawCenteredString(font, line1, clampX(ix, font.width(line1), w), textY, WHITE);
			g.drawCenteredString(font, line2, clampX(ix, font.width(line2), w), textY + 10, GOLD);
		}
	}

	private static int clampX(int x, int textWidth, int screenWidth) {
		int half = textWidth / 2 + 2;
		return Math.max(half, Math.min(x, screenWidth - half));
	}

	private static String formatDistance(double blocks) {
		if (blocks >= 1000) return String.format("%.1fkm", blocks / 1000.0);
		return (int) blocks + "m";
	}
}
