package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.GhostSpells;
import com.example.defyingtheheavens.SpellConstruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import static com.example.defyingtheheavens.client.BlockMasses.noise;
import static com.example.defyingtheheavens.client.BlockMasses.smooth;

/**
 * More of the great spells made of the world's own blocks (SpellConstruct Form BLOCKS, as BlockMasses lays out, meshes and draws them; the
 * user's ask, 2026-10-25: "polish all the skills again old and new", of shapes of GeckoLib's that "look a bit weird"), each laid out here
 * with its foot (or heart) at the construct's place and its front toward +z:
 * <ul>
 * <li>{@code MOON_MASS}: The Frozen Moon Falls' moon of ice: packed and blue ice, its seas of snow, its craters rimmed with snow; struck, it
 * shatters, its eight pieces flung out and tumbling down.</li>
 * <li>{@code CRESCENT_MASS}: Frozen Moon in the Well's crescent of ice, standing over the well of still black water that holds its reflection;
 * shattering, its pieces falling into the well.</li>
 * <li>{@code METEOR_MASS}: Blazing Meteor's rock: blackstone and basalt cracked open on magma, a trail of fire and smoke streaming off it.</li>
 * <li>{@code PAGODA_MASS}: the Soul-Locking Pagoda: seven tiers of blackstone, bone pillars and iron bars, soul lanterns hanging from its eaves.</li>
 * <li>{@code WALL_MASS}: a length of the walls of Fengdu, black stone, crenellated, a gate-tower at its middle, soul fire at its ends;
 * toppling in on the city's heart.</li>
 * <li>{@code BRIDGE_MASS}: a span of the Naihe Bridge, the bridge of no return: an arch of deepslate brick, its rails of blackstone, soul
 * lanterns on its posts.</li>
 * </ul>
 */
final class SpellMasses {
	private SpellMasses() {}

	private static BlockState of(net.minecraft.world.level.block.Block block) {
		return block.defaultBlockState();
	}

	// --- Laying out ---

	static void layout(SpellConstruct.Kind kind, BlockMasses.Layout layout, float size, float value, long seed) {
		switch (kind) {
			case MOON_MASS -> moon(layout, Math.max(2, size), seed);
			case CRESCENT_MASS -> crescent(layout, Math.max(3, size), seed);
			case METEOR_MASS -> meteor(layout, Math.max(1.5f, size), seed);
			case PAGODA_MASS -> pagoda(layout, Math.max(6, size), Math.max(10, value), seed);
			case WALL_MASS -> wall(layout, Math.max(8, size), Math.max(6, value), seed);
			case BRIDGE_MASS -> bridge(layout, Math.max(8, size), Math.max(3, value), seed);
			default -> {}
		}
	}

	/** Which of its eight pieces a block about a heart is in (by its octant), for what shatters into them. */
	private static int octant(int x, int y, int z) {
		return (x >= 0 ? 1 : 0) | (y >= 0 ? 2 : 0) | (z >= 0 ? 4 : 0);
	}

	/** The ice moon, {@code radius} round about its heart at 0: ice, its seas of snow, its craters, in eight pieces for its shattering. */
	private static void moon(BlockMasses.Layout layout, float radius, long seed) {
		int r = Mth.ceil(radius) + 1;
		// Its craters: bowls in its face, each a place on it and a round.
		int craters = 7;
		double[][] bowls = new double[craters][4];
		for (int i = 0; i < craters; i++) {
			double a = Math.PI * 2 * noise(seed, i, 1, 1), b = Math.acos(2 * noise(seed, i, 2, 2) - 1);
			bowls[i] = new double[] {Math.sin(b) * Math.cos(a), Math.cos(b), Math.sin(b) * Math.sin(a), radius * (0.18 + 0.2 * noise(seed, i, 3, 3))};
		}
		for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
			double d = Math.sqrt(x * x + y * y + z * z);
			if (d > radius) continue;
			double nx = x / Math.max(1e-6, d), ny = y / Math.max(1e-6, d), nz = z / Math.max(1e-6, d);
			boolean rim = false;
			boolean hollow = false;
			for (double[] bowl : bowls) {
				double across = Math.acos(Mth.clamp(nx * bowl[0] + ny * bowl[1] + nz * bowl[2], -1, 1)) * radius;
				if (across < bowl[3] && d > radius - bowl[3] * 0.45 * (1 - across / bowl[3] * across / bowl[3])) hollow = true;
				else if (across < bowl[3] * 1.25 && d > radius - 1.5) rim = true;
			}
			if (hollow) continue;
			float sea = smooth(seed + 7, (float) (nx * 6 + nz * 3), (float) (ny * 6 - nz * 2), 2.2f);
			float n = noise(seed, x, y, z);
			BlockState ice;
			if (d < radius - 2.5) ice = n < 0.5f ? of(Blocks.BLUE_ICE) : of(Blocks.PACKED_ICE);
			else if (rim) ice = n < 0.6f ? of(Blocks.SNOW_BLOCK) : of(Blocks.POWDER_SNOW);
			else if (sea > 0.6f) ice = n < 0.7f ? of(Blocks.SNOW_BLOCK) : of(Blocks.CALCITE);
			else ice = n < 0.1f ? of(Blocks.BLUE_ICE) : n < 0.25f ? of(Blocks.ICE) : of(Blocks.PACKED_ICE);
			layout.set(x, y, z, ice, octant(x, y, z));
		}
	}

	/** The crescent of ice, {@code radius} across (its heart at {@code radius} over its foot), standing upright facing +z, in pieces. */
	private static void crescent(BlockMasses.Layout layout, float radius, long seed) {
		int r = Mth.ceil(radius) + 1;
		float shift = radius * 0.42f, inner = radius * 0.86f;
		for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) {
			double d = Math.sqrt(x * x + y * y);
			if (d > radius) continue;
			double di = Math.sqrt((x - shift) * (x - shift) + (y - shift * 0.25) * (y - shift * 0.25));
			if (di < inner) continue; // the dark of it
			int thick = Math.max(1, Mth.ceil(2.2 * Math.min(1, (d - 0) / radius + 0.2) * (1 - Math.pow(Math.abs(y) / radius, 4))));
			for (int z = -thick; z <= thick; z++) {
				float n = noise(seed, x, y, z);
				BlockState ice = Math.abs(z) == thick ? (n < 0.3f ? of(Blocks.SNOW_BLOCK) : of(Blocks.PACKED_ICE)) : n < 0.5f ? of(Blocks.BLUE_ICE) : of(Blocks.PACKED_ICE);
				int sector = Mth.clamp((int) ((Math.atan2(y, x) + Math.PI) / (Math.PI * 2) * 6), 0, 5);
				layout.set(x, y + Mth.ceil(radius), z, ice, sector);
			}
		}
	}

	/** Blazing Meteor's rock, {@code radius} round about its heart: blackstone and basalt, cracked open on magma, glowing within. */
	private static void meteor(BlockMasses.Layout layout, float radius, long seed) {
		int r = Mth.ceil(radius) + 1;
		for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
			double d = Math.sqrt(x * x + y * y + z * z);
			if (d > radius * (0.82 + 0.3 * noise(seed, x >> 1, y >> 1, z >> 1))) continue;
			float crack = smooth(seed + 3, x + y * 0.7f, z - y * 0.4f, 1.8f);
			float n = noise(seed, x, y, z);
			BlockState rock;
			if (Math.abs(crack - 0.5f) < 0.07f || d < radius * 0.45) rock = n < 0.6f ? of(Blocks.MAGMA_BLOCK) : of(Blocks.SHROOMLIGHT);
			else if (n < 0.35f) rock = of(Blocks.BLACKSTONE);
			else if (n < 0.6f) rock = of(Blocks.BASALT);
			else if (n < 0.8f) rock = of(Blocks.NETHERRACK);
			else rock = of(Blocks.OBSIDIAN);
			layout.set(x, y, z, rock, 0);
		}
	}

	/** The Soul-Locking Pagoda, {@code radius} round at its foot and {@code height} tall: seven tiers, each narrower, eaves, lanterns, a spire. */
	private static void pagoda(BlockMasses.Layout layout, float radius, float height, long seed) {
		int tiers = 7;
		float tierHigh = Math.max(4, height * 0.88f / tiers);
		BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
		for (int t = 0; t < tiers; t++) {
			float f = t / (float) tiers;
			float wall = radius * (0.5f - 0.27f * f), eave = wall + Math.max(2.0f, radius * 0.2f * (1 - f * 0.4f));
			int y0 = Mth.floor(t * tierHigh), y1 = Mth.floor((t + 1) * tierHigh);
			int rw = Mth.ceil(eave) + 1;
			for (int x = -rw; x <= rw; x++) for (int z = -rw; z <= rw; z++) {
				// Octagonal: its corners cut.
				float ax = Math.abs(x + 0.5f), az = Math.abs(z + 0.5f), oct = Math.max(Math.max(ax, az), (ax + az) * 0.7071f);
				for (int y = y0; y < y1; y++) {
					// Its eaves: a roof of tiles over the storey, its corners turned up.
					boolean cornerOut = Math.abs(ax - az) < 1.6f && oct > eave - 1.6f;
					if (y == y1 - 1 || y == y1 - 2 && cornerOut) {
						if (oct > eave + (cornerOut ? 0.6f : 0)) continue;
						layout.set(x, y, z, oct > wall + 0.5f ? of(Blocks.DEEPSLATE_TILES) : of(Blocks.POLISHED_BLACKSTONE_BRICKS), 0);
						continue;
					}
					if (oct > wall) continue;
					boolean shell = oct > wall - 1.2f;
					if (!shell) {
						if (y == y0) layout.set(x, y, z, of(Blocks.POLISHED_BLACKSTONE), 0); // its floor
						continue;
					}
					// Bone pillars at its corners, iron bars between, its windows glowing with soul fire.
					boolean corner = Math.abs(ax - az) < 1.2f || ax < 1.2f || az < 1.2f;
					if (corner) layout.set(x, y, z, of(Blocks.BONE_BLOCK), 0);
					else if (y > y0 && y < y1 - 2 && noise(seed, x, t, z) < 0.5f) layout.set(x, y, z, of(Blocks.IRON_BARS), 0);
					else layout.set(x, y, z, noise(seed, x, y, z) < 0.15f ? of(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS) : of(Blocks.BLACKSTONE), 0);
				}
			}
			// Soul fire within each tier, seen through its bars; lanterns hung from the eaves' corners.
			layout.set(0, y0 + 1, 0, of(Blocks.SOUL_CAMPFIRE), 0);
			for (int k = 0; k < 8; k++) {
				double a = Math.PI * 2 * k / 8 + Math.PI / 8;
				int lx = Mth.floor(Math.cos(a) * (eave - 0.5)), lz = Mth.floor(Math.sin(a) * (eave - 0.5));
				layout.set(lx, y1 - 3, lz, lantern, 0);
			}
		}
		// Its spire: rings of gold over iron, bone at its tip.
		int top = Mth.floor(tiers * tierHigh);
		for (int y = top; y < top + Math.max(3, height * 0.14f); y++) {
			layout.set(0, y, 0, (y - top) % 3 == 1 ? of(Blocks.GOLD_BLOCK) : of(Blocks.CHAIN), 0);
		}
		layout.set(0, top + Mth.ceil(Math.max(3, height * 0.14f)), 0, of(Blocks.BONE_BLOCK), 0);
	}

	/** A length of the walls of Fengdu, {@code length} long and {@code high} tall (its foot at 0, its face toward +z): crenellated, a gate-tower. */
	private static void wall(BlockMasses.Layout layout, float length, float high, long seed) {
		int half = Mth.ceil(length / 2), h = Mth.ceil(high), thick = Math.max(2, Mth.ceil(high * 0.22f));
		int tower = Math.max(3, Mth.ceil(length * 0.13f)), towerHigh = Mth.ceil(high * 1.45f);
		for (int x = -half; x <= half; x++) {
			boolean inTower = Math.abs(x) <= tower;
			int top = inTower ? towerHigh : h;
			for (int z = -thick; z <= thick + (inTower ? 1 : 0); z++) {
				for (int y = -2; y <= top; y++) {
					// Its gate: an arch through the tower's foot.
					if (inTower && Math.abs(x) <= tower * 0.32f && y < high * 0.4f + Math.sqrt(Math.max(0, tower * 0.32f * tower * 0.32f - x * x)) * 0.6f) continue;
					// Merlons along its top, two blocks of every three, on both its faces.
					if (y > top - 2 && y >= (inTower ? towerHigh : h) - 1 && y == top && (Math.floorMod(x, 3) == 2 || z != thick + (inTower ? 1 : 0) && z != -thick)) continue;
					float n = noise(seed, x, y, z);
					BlockState stone = y < 1 && n < 0.4f ? of(Blocks.COBBLED_DEEPSLATE)
							: n < 0.06f ? of(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS)
							: (y == 0 || y == top - 1 ? of(Blocks.POLISHED_BLACKSTONE) : of(Blocks.POLISHED_BLACKSTONE_BRICKS));
					layout.set(x, y, z, stone, 0);
				}
			}
			// Soul fire burning in braziers along its walk.
			if (!inTower && Math.floorMod(x, 6) == 3) layout.set(x, h, 0, of(Blocks.SOUL_CAMPFIRE), 0);
		}
		// Its tower's roof, and lanterns either side of its gate.
		for (int x = -tower - 1; x <= tower + 1; x++) for (int z = -thick - 1; z <= thick + 2; z++) layout.set(x, towerHigh + 1, z, of(Blocks.DEEPSLATE_TILES), 0);
		BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState();
		for (int s = -1; s <= 1; s += 2) layout.set(s * (Mth.ceil(tower * 0.32f) + 1), Mth.floor(high * 0.45f), thick + 2, lantern, 0);
		// Soul fire at its ends.
		for (int s = -1; s <= 1; s += 2) layout.set(s * half, h + 1, 0, of(Blocks.SOUL_CAMPFIRE), 0);
	}

	/** A span of the Naihe Bridge, {@code length} long and {@code half} the half of its width (its deck's middle at 0, along z): an arch. */
	private static void bridge(BlockMasses.Layout layout, float length, float half, long seed) {
		int l = Mth.ceil(length / 2), w = Mth.ceil(half * 0.55f);
		BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState();
		for (int z = -l; z <= l; z++) {
			float f = z / (float) l;
			// Its deck humped a little at its middle, as an arched bridge's is; its arch beneath, its piers at its ends.
			int deck = Mth.floor(3.2f * (1 - f * f));
			int under = Mth.floor(-1 - 5 * (1 - Math.sqrt(Math.max(0, 1 - f * f))));
			for (int x = -w; x <= w; x++) {
				for (int y = under; y <= deck; y++) {
					float n = noise(seed, x, y, z);
					BlockState stone = y == deck ? (n < 0.2f ? of(Blocks.CRACKED_DEEPSLATE_BRICKS) : of(Blocks.DEEPSLATE_BRICKS))
							: n < 0.15f ? of(Blocks.CRACKED_DEEPSLATE_BRICKS) : n < 0.3f ? of(Blocks.COBBLED_DEEPSLATE) : of(Blocks.DEEPSLATE_BRICKS);
					layout.set(x, y, z, stone, 0);
				}
				// Its rails.
				if (Math.abs(x) == w) layout.set(x, deck + 1, z, Math.floorMod(z, 4) == 0 ? of(Blocks.POLISHED_BLACKSTONE) : of(Blocks.POLISHED_BLACKSTONE_WALL), 0);
			}
			// Lanterns on its rail posts.
			if (Math.floorMod(z, 8) == 0) for (int s = -1; s <= 1; s += 2) layout.set(s * w, deck + 2, z, lantern, 0);
		}
	}

	// --- Drawing ---

	static void draw(SpellConstruct c, BlockMasses.Mass mass, float partial, float age, PoseStack pose, MultiBufferSource buffers) {
		String phase = c.phase();
		float since = c.phaseAge(partial), left = c.life() - age, size = c.size();
		pose.pushPose();
		switch (c.kind()) {
			case MOON_MASS -> {
				// Turning slowly as it falls; shattering, its eight pieces flung out and tumbling down.
				boolean shattered = "shatter".equals(phase);
				pose.mulPose(Axis.YP.rotationDegrees(age * 0.6f));
				pose.mulPose(Axis.XP.rotationDegrees(age * 0.35f));
				for (int part = 0; part < 8; part++) {
					pose.pushPose();
					if (shattered) {
						float t = since;
						float dx = (part & 1) == 1 ? 1 : -1, dy = (part & 2) == 2 ? 1 : -1, dz = (part & 4) == 4 ? 1 : -1;
						pose.translate(dx * t * 0.35f, dy * t * 0.2f - t * t * 0.03f, dz * t * 0.35f);
						pose.mulPose(Axis.XP.rotationDegrees(t * 4 * dx));
						pose.mulPose(Axis.ZP.rotationDegrees(t * 3 * dz));
						float s = Math.max(0, 1 - t / 30);
						pose.scale(s, s, s);
					}
					BlockMasses.draw(mass, part, pose);
					pose.popPose();
				}
				if (!shattered) frost(pose, buffers, size, age, c.getId());
			}
			case CRESCENT_MASS -> {
				// Rising up out of the well, then shining; shattering, its pieces falling into the well.
				float rise = SpellRender.easeOut(age / com.example.defyingtheheavens.WaterSpells.WELL_RISES);
				pose.translate(0, -(1 - rise) * size * 2, 0);
				boolean shattered = "shatter".equals(phase);
				for (int part = 0; part < 6; part++) {
					pose.pushPose();
					if (shattered) {
						float t = since;
						double a = -Math.PI + (part + 0.5) * Math.PI * 2 / 6;
						pose.translate(Math.cos(a) * t * 0.2f, -t * t * 0.05f, (part % 2 == 0 ? 1 : -1) * t * 0.1f);
						pose.mulPose(Axis.ZP.rotationDegrees(t * (part % 2 == 0 ? 3 : -3)));
						float s = Math.max(0, 1 - t / 24);
						pose.scale(s, s, s);
					}
					BlockMasses.draw(mass, part, pose);
					pose.popPose();
				}
				well(pose, buffers, size, age, rise * (shattered ? Math.max(0, 1 - since / 20) : 1));
			}
			case METEOR_MASS -> {
				if ("impact".equals(phase)) {
					float s = Math.max(0, 1 - since / 6);
					if (s <= 0) break;
					pose.scale(s, s, s);
				}
				pose.mulPose(Axis.XP.rotationDegrees(age * 7));
				pose.mulPose(Axis.ZP.rotationDegrees(age * 4.5f));
				BlockMasses.draw(mass, 0, pose);
				pose.popPose();
				pose.pushPose();
				if (!"impact".equals(phase)) trail(c, pose, buffers, size, age, partial);
			}
			case PAGODA_MASS -> {
				if ("land".equals(phase) && since < 10) pose.translate(0, Mth.sin(since * 0.9f) * (1 - since / 10) * 0.4f, 0);
				BlockMasses.draw(mass, 0, pose);
			}
			case WALL_MASS -> {
				// Heaving up out of the land; toppling in on the city's heart (toward +z) about its foot's inner edge.
				float rise = SpellRender.easeOut(age / GhostSpells.FENGDU_RISES);
				pose.translate(0, -(1 - rise) * (c.value() + 4), 0);
				if (rise < 1) pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 1.6f) * 0.8f * (1 - rise)));
				if ("fall".equals(phase)) {
					float t = Mth.clamp(since / GhostSpells.FENGDU_FALLS, 0, 1);
					float edge = Math.max(2, c.value() * 0.22f);
					pose.translate(0, 0, edge);
					pose.mulPose(Axis.XP.rotationDegrees(88 * t * t));
					pose.translate(0, 0, -edge);
				}
				BlockMasses.draw(mass, 0, pose);
			}
			case BRIDGE_MASS -> {
				// Rising up out of the river of the dead below it into its place.
				float rise = SpellRender.easeOut(age / 12);
				pose.translate(0, -(1 - rise) * 8, 0);
				if (left < 16) pose.translate(0, -(1 - left / 16) * (1 - left / 16) * 10, 0);
				BlockMasses.draw(mass, 0, pose);
			}
			default -> {}
		}
		pose.popPose();
	}

	/** The cold streaming up off the ice moon as it falls, and its light. */
	private static void frost(PoseStack pose, MultiBufferSource buffers, float radius, float age, long seed) {
		MultiBufferSource spell = SpellBuffers.begin(buffers);
		try {
			Quaternionf facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
			VertexConsumer glow = spell.getBuffer(SpellRender.glow(SpellRender.GLOW));
			SpellRender.billboard(glow, pose, facing, 0, 0, 0, radius * 4, 0, new float[] {0.6f, 0.85f, 1.0f}, 0.35f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			VertexConsumer mist = spell.getBuffer(SpellRender.clear(SpellRender.MIST));
			for (int i = 0; i < 12; i++) {
				float t = (age * 0.05f + i / 12f) % 1;
				double a = SpellRender.hash(seed * 31 + i) * Math.PI * 2;
				float r = radius * (0.6f + 0.4f * t);
				SpellRender.billboard(mist, pose, facing, (float) Math.cos(a) * r, radius * 0.5f + t * radius * 2.2f, (float) Math.sin(a) * r, radius * (0.8f + t), i,
						new float[] {0.88f, 0.95f, 1.0f}, 0.45f * (1 - t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		} finally {
			SpellBuffers.end();
		}
	}

	/** The well of still black water under the crescent, its reflection shining up out of it. */
	private static void well(PoseStack pose, MultiBufferSource buffers, float radius, float age, float shown) {
		if (shown <= 0) return;
		MultiBufferSource spell = SpellBuffers.begin(buffers);
		try {
			VertexConsumer dark = spell.getBuffer(SpellRender.clear(SpellRender.WATER));
			SpellRender.disc(dark, pose.last(), radius * 1.3f, 0.12f, age * 0.003f, new float[] {0.03f, 0.05f, 0.1f}, 0.9f * shown, SpellRender.FULL_BRIGHT);
			VertexConsumer light = spell.getBuffer(SpellRender.glow(SpellRender.GLOW));
			SpellRender.disc(light, pose.last(), radius * 0.6f, 0.16f, 0, new float[] {0.75f, 0.9f, 1.0f}, 0.55f * shown * (0.85f + 0.15f * Mth.sin(age * 0.1f)),
					SpellRender.FULL_BRIGHT);
			VertexConsumer ring = spell.getBuffer(SpellRender.glow(SpellRender.RING));
			SpellRender.disc(ring, pose.last(), radius * 1.32f, 0.18f, age * 0.01f, new float[] {0.6f, 0.85f, 1.0f}, 0.5f * shown, SpellRender.FULL_BRIGHT);
		} finally {
			SpellBuffers.end();
		}
	}

	/** The fire and smoke streaming off the meteor, back the way it came. */
	private static void trail(SpellConstruct c, PoseStack pose, MultiBufferSource buffers, float radius, float age, float partial) {
		Vec3 way = c.getPosition(partial).subtract(c.getPosition(Math.max(0, partial - 1)));
		Vec3 back = way.lengthSqr() < 1.0e-6 ? new Vec3(0, 1, 0) : way.normalize().scale(-1);
		Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(c.getPosition(partial));
		MultiBufferSource spell = SpellBuffers.begin(buffers);
		try {
			Quaternionf facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
			VertexConsumer glow = spell.getBuffer(SpellRender.glow(SpellRender.GLOW));
			SpellRender.billboard(glow, pose, facing, 0, 0, 0, radius * 5, 0, new float[] {1.0f, 0.5f, 0.15f}, 0.6f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			VertexConsumer flames = spell.getBuffer(SpellRender.glow(SpellRender.FLAME));
			VertexConsumer smoke = spell.getBuffer(SpellRender.clear(SpellRender.SMOKE));
			for (int i = 0; i < 18; i++) {
				float t = (age * 0.12f + i / 18f) % 1;
				Vec3 p = back.scale(radius * (0.6 + t * 7)).add((SpellRender.hash(i * 7L) - 0.5) * radius * t, (SpellRender.hash(i * 9L) - 0.5) * radius * t,
						(SpellRender.hash(i * 11L) - 0.5) * radius * t);
				float s = radius * (1.6f - t);
				SpellRender.billboard(flames, pose, facing, (float) p.x, (float) p.y, (float) p.z, s, i, new float[] {1.0f, 0.55f + 0.3f * (1 - t), 0.2f}, 0.9f * (1 - t),
						0, (i % 8) / 8f, 1, (i % 8 + 1) / 8f, SpellRender.FULL_BRIGHT);
				Vec3 q = back.scale(radius * (2 + t * 10));
				SpellRender.billboard(smoke, pose, facing, (float) q.x, (float) q.y, (float) q.z, radius * (1.2f + 2 * t), i, new float[] {0.15f, 0.12f, 0.12f},
						0.5f * (1 - t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		} finally {
			SpellBuffers.end();
		}
	}
}
