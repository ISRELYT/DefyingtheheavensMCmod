package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CircuitBoard;
import com.example.defyingtheheavens.PuppetBlueprint;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * A puppet's circuits drawn as they glow in the world for one with Qi Sense on (Puppetry): the board's lines of qi as azure ribbons on a
 * square (a frame lying on a Puppet Workbench, a panel on a puppet's back), joined as the board joins them, a diamond at each node, the
 * core's socket a brighter one. Drawn into Qi Sense's own glow (QiSenseTreasures#glow), never into a buffer someone else is holding.
 */
final class CircuitGlow {
	private static final float[] INK = {0.30f, 0.80f, 1.0f};
	private static final int[] DX = {0, 1, 0, -1, 1, 1, -1, -1}, DY = {-1, 0, 1, 0, -1, 1, 1, -1};

	/**
	 * {@code strokes} (a board of {@code size} x {@code size}, its drawing as Puppetry#strokes keeps it) on the square from {@code corner}
	 * along {@code across} (its x) and {@code down} (its y), facing {@code normal}, as bright as {@code glow}.
	 */
	static void draw(VertexConsumer buffer, Matrix4f matrix, byte[] strokes, int size, PuppetBlueprint blueprint, Vec3 corner, Vec3 across, Vec3 down,
			Vec3 normal, float glow) {
		if (strokes.length < size * size) return;
		CircuitBoard.Stroke[] all = CircuitBoard.Stroke.values();
		List<PuppetBlueprint.Node> nodes = blueprint.nodes(size);
		boolean[] node = new boolean[size * size];
		for (PuppetBlueprint.Node n : nodes) node[n.y() * size + n.x()] = true;
		int centre = size / 2;
		// The lines' and marks' widths by the square's cells (a bench's board is broad, a puppet's panel small).
		double cell = across.length() / size, half = cell * 0.4;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int b = strokes[y * size + x];
				CircuitBoard.Stroke s = b > 0 && b < all.length ? all[b] : CircuitBoard.Stroke.NONE;
				if (!s.conducts()) continue;
				Vec3 c = at(corner, across, down, size, x, y);
				double w = s == CircuitBoard.Stroke.THICK ? half * 1.7 : half;
				diamond(buffer, matrix, c, across, down, w * 1.3, glow);
				for (int d = 0; d < 8; d++) {
					if (d >= 4 && s != CircuitBoard.Stroke.DIAGONAL) break;
					int nx = x + DX[d], ny = y + DY[d];
					if (nx < 0 || ny < 0 || nx >= size || ny >= size) continue;
					int i = ny * size + nx;
					boolean joins;
					if (nx == centre && ny == centre) joins = d < 4;
					else if (node[i]) joins = d < 4;
					else {
						int o = strokes[i];
						CircuitBoard.Stroke there = o > 0 && o < all.length ? all[o] : CircuitBoard.Stroke.NONE;
						joins = d < 4 ? there.conducts() : there == CircuitBoard.Stroke.DIAGONAL;
					}
					if (!joins) continue;
					Vec3 mid = c.add(at(corner, across, down, size, nx, ny)).scale(0.5);
					ribbon(buffer, matrix, c, mid, normal, w, glow);
				}
			}
		}
		for (PuppetBlueprint.Node n : nodes) diamond(buffer, matrix, at(corner, across, down, size, n.x(), n.y()), across, down, cell * 0.5, glow * 0.6f);
		diamond(buffer, matrix, at(corner, across, down, size, centre, centre), across, down, cell * 0.7, glow);
	}

	private static Vec3 at(Vec3 corner, Vec3 across, Vec3 down, int size, int x, int y) {
		return corner.add(across.scale((x + 0.5) / size)).add(down.scale((y + 0.5) / size));
	}

	private static void ribbon(VertexConsumer buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 normal, double half, float glow) {
		Vec3 along = b.subtract(a);
		Vec3 side = along.cross(normal);
		if (side.lengthSqr() < 1.0e-9) return;
		Vec3 halo = side.normalize().scale(half * 2.4);
		side = side.normalize().scale(half);
		float edge = 0.18f * glow, core = 0.9f * glow;
		quad(buffer, matrix, a.add(halo), 0, a.add(side), edge, b.add(side), edge, b.add(halo), 0);
		quad(buffer, matrix, a.subtract(side), edge, a.subtract(halo), 0, b.subtract(halo), 0, b.subtract(side), edge);
		quad(buffer, matrix, a.add(side), edge, a, core, b, core, b.add(side), edge);
		quad(buffer, matrix, a, core, a.subtract(side), edge, b.subtract(side), edge, b, core);
	}

	private static void diamond(VertexConsumer buffer, Matrix4f matrix, Vec3 c, Vec3 across, Vec3 down, double r, float glow) {
		Vec3 u = across.normalize().scale(r), v = down.normalize().scale(r);
		float a = 0.9f * glow;
		quad(buffer, matrix, c.add(u), a * 0.3f, c.add(v), a * 0.3f, c.subtract(u), a * 0.3f, c.subtract(v), a * 0.3f);
		quad(buffer, matrix, c.add(u.scale(0.5)), a, c.add(v.scale(0.5)), a, c.subtract(u.scale(0.5)), a, c.subtract(v.scale(0.5)), a);
	}

	private static void quad(VertexConsumer buffer, Matrix4f matrix, Vec3 p0, float a0, Vec3 p1, float a1, Vec3 p2, float a2, Vec3 p3, float a3) {
		vertex(buffer, matrix, p0, a0);
		vertex(buffer, matrix, p1, a1);
		vertex(buffer, matrix, p2, a2);
		vertex(buffer, matrix, p3, a3);
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix, Vec3 p, float alpha) {
		buffer.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(INK[0], INK[1], INK[2], alpha).endVertex();
	}

	private CircuitGlow() {}
}
