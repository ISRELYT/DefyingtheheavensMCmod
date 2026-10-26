package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.Law;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Law domains this client sees (LawDomains on the server tells it, LAW_DOMAIN to all who see their owners), each drawn round its owner
 * in the world as it unfurls, stands and folds away:
 * <ul>
 *   <li>its sphere: a membrane in its Law's colour, faint, brighter toward its rim as a bubble is (its near half alone from outside, so
 *       its colour isn't doubled; from within, fainter still, for every way one looks crosses it), and over it the Law's own stuff in
 *       a pattern that drifts round: Metal's blade-light, Wood's vines, Water's waters, Fire's tongues of flame licking up it, Earth's
 *       molten veins, Space's stars, Time's rings, Life and Death's black smoke over its bone-white;</li>
 *   <li>its seal on the ground under its owner, as wide as the sphere is there, turning slowly (the five elements' seals; Space's 空 and
 *       Time's 時; Life and Death's taiji; drawn large, {@link #seal}), and a ring running out over it every two seconds;</li>
 *   <li>motes of the Law drifting in it: glints of metal, leaves turning, bubbles rising, embers rising, dust falling, stars
 *       twinkling, rings wheeling round, wisps drawn inward.</li>
 * </ul>
 * See-through (SpellRender#clear) more than glow, so it shows by day. Not drawn round an owner this client can't see (an Illusion).
 */
public final class ClientLawDomains {
	/**
	 * A domain seen: its Law and reach, when it unfurled and (once folded away) when it folded (client game time, -1 while it stands), and
	 * the world it was seen in (entity ids are another world's after a change of worlds: what was seen in the last is let go).
	 */
	private record Seen(Law law, float radius, long since, long folded, Level level) {}

	private static final Map<Integer, Seen> DOMAINS = new HashMap<>();
	/** Ticks it takes to unfurl to its full reach, and to fade away once folded. */
	private static final float GROW = 14, FADE = 12;
	private static final double DRAW_RANGE = 224;
	/** The sphere's mesh: bands from pole to pole, segments round. */
	private static final int RINGS = 24, SEGMENTS = 48;
	/** The membrane's alpha from outside at its middle and at its rim, and from within; the pattern's, from outside and from within. */
	private static final float MEMBRANE = 0.07f, MEMBRANE_RIM = 0.30f, MEMBRANE_INSIDE = 0.04f, PATTERN = 0.42f, PATTERN_INSIDE = 0.12f;
	private static final float[] WHITE = {1, 1, 1};

	/** The server's word: entity {@code id}'s domain is {@code law}'s, {@code radius} round ({@code law} null: folded away). */
	public static void set(int id, Law law, float radius) {
		Level level = Minecraft.getInstance().level;
		if (level == null) return;
		long now = now();
		Seen was = DOMAINS.get(id);
		if (was != null && was.level() != level) was = null;
		if (law == null) {
			if (was != null && was.folded() < 0) DOMAINS.put(id, new Seen(was.law(), was.radius(), was.since(), now, level));
			else if (was == null) DOMAINS.remove(id);
			return;
		}
		boolean standing = was != null && was.law() == law && was.folded() < 0;
		DOMAINS.put(id, new Seen(law, radius, standing ? was.since() : now, -1, level));
	}

	private static long now() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? 0 : mc.level.getGameTime();
	}

	/** Forgets those folded away and faded, any from before a jump in time, and those seen in another world. */
	public static void tick() {
		if (DOMAINS.isEmpty()) return;
		long now = now();
		Level level = Minecraft.getInstance().level;
		DOMAINS.values().removeIf(s -> s.level() != level || s.folded() >= 0 && now - s.folded() > FADE + 2 || now < s.since() - 40);
	}

	public static void clear() {
		DOMAINS.clear();
	}

	/** WorldRenderEvents.AFTER_ENTITIES: every domain seen whose owner is near. */
	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || DOMAINS.isEmpty()) return;
		Vec3 cam = context.camera().getPosition();
		float partial = context.tickDelta();
		float time = mc.level.getGameTime() + partial;
		Quaternionf facing = context.camera().rotation();
		PoseStack pose = context.matrixStack();
		MultiBufferSource buffers = SpellBuffers.begin(context.consumers() == null ? mc.renderBuffers().bufferSource() : context.consumers());
		try {
			for (Map.Entry<Integer, Seen> entry : List.copyOf(DOMAINS.entrySet())) {
				Seen seen = entry.getValue();
				Entity owner = seen.level() == mc.level ? mc.level.getEntity(entry.getKey()) : null;
				if (owner == null || owner != mc.player && owner.isInvisibleTo(mc.player)) continue;
				float shown = shown(seen, time);
				if (shown <= 0) continue;
				Vec3 centre = owner.getPosition(partial).add(0, owner.getBbHeight() / 2, 0);
				if (centre.distanceTo(cam) > DRAW_RANGE + seen.radius()) continue;
				float radius = seen.radius() * SpellRender.easeOut((time - seen.since()) / GROW);
				if (radius < 0.5f) continue;
				draw(buffers, pose, facing, cam, owner, centre, seen.law(), radius, shown, time);
			}
		} finally {
			SpellBuffers.end();
		}
	}

	/** How much of it shows: quickly in as it unfurls, fading out over {@link #FADE} once folded. */
	private static float shown(Seen seen, float time) {
		float in = Mth.clamp((time - seen.since()) / 4f, 0, 1);
		float out = seen.folded() < 0 ? 1 : Mth.clamp(1 - (time - seen.folded()) / FADE, 0, 1);
		return Math.min(in, out);
	}

	private static void draw(MultiBufferSource buffers, PoseStack pose, Quaternionf facing, Vec3 cam, Entity owner, Vec3 centre, Law law, float r,
							 float shown, float time) {
		float[] tint = SpellRender.rgb(law.color());
		pose.pushPose();
		pose.translate(centre.x - cam.x, centre.y - cam.y, centre.z - cam.z);
		Vec3 eye = cam.subtract(centre);
		boolean inside = eye.lengthSqr() < (double) r * r;
		// The membrane: the middle of the glow's texture, a flat light, rim-lit.
		shell(buffers.getBuffer(SpellRender.clear(SpellRender.GLOW)), pose.last(), r, eye, inside, tint, shown, null, time);
		shell(buffers.getBuffer(SpellRender.clear(skin(law))), pose.last(), r * 0.995f, eye, inside, skinTint(law, tint), shown, law, time);
		seal(buffers, pose, owner, centre, r, tint, shown, time, law);
		motes(buffers, pose, facing, r, tint, shown, time, law, owner.getId());
		pose.popPose();
	}

	// --- The sphere ---

	/**
	 * The sphere of radius {@code r} about the pose's origin, {@code eye} the camera from it: from outside its near half, brighter toward
	 * the rim; from within all of it, faint. {@code law}: the Law's pattern ({@link #skin}, wrapped and drifting round); null: the membrane.
	 */
	private static void shell(VertexConsumer vc, PoseStack.Pose pose, float r, Vec3 eye, boolean inside, float[] rgb, float shown, Law law, float time) {
		float[] frame = law == null || frames(law) <= 1 ? new float[]{0, 1} : SpellRender.frame(time, law == Law.FIRE ? 0.5f : 0.25f, frames(law));
		float uScale = law == null ? 0 : around(law), vScale = law == null ? 0 : frames(law) > 1 ? frame[1] - frame[0] : up(law);
		float uDrift = law == null ? 0.5f : time * drift(law), vStart = law == null ? 0.5f : frames(law) > 1 ? frame[0] : time * drift(law) * 0.3f;
		for (int i = 0; i < RINGS; i++) {
			double p0 = Math.PI * i / RINGS, p1 = Math.PI * (i + 1) / RINGS;
			for (int j = 0; j < SEGMENTS; j++) {
				double t0 = Math.PI * 2 * j / SEGMENTS, t1 = Math.PI * 2 * (j + 1) / SEGMENTS;
				if (!inside) {
					// Its near half alone: where the surface faces the eye, (eye - n r) . n > 0.
					double pm = (p0 + p1) / 2, tm = (t0 + t1) / 2;
					double nx = Math.sin(pm) * Math.cos(tm), ny = Math.cos(pm), nz = Math.sin(pm) * Math.sin(tm);
					if ((eye.x - nx * r) * nx + (eye.y - ny * r) * ny + (eye.z - nz * r) * nz <= 0) continue;
				}
				float u0 = uDrift + uScale * j / SEGMENTS, u1 = uDrift + uScale * (j + 1) / SEGMENTS;
				float v0 = vStart + vScale * i / RINGS, v1 = vStart + vScale * (i + 1) / RINGS;
				if (law == null) u0 = u1 = v0 = v1 = 0.5f;
				corner(vc, pose, r, p0, t0, eye, inside, rgb, shown, law, u0, v0);
				corner(vc, pose, r, p1, t0, eye, inside, rgb, shown, law, u0, v1);
				corner(vc, pose, r, p1, t1, eye, inside, rgb, shown, law, u1, v1);
				corner(vc, pose, r, p0, t1, eye, inside, rgb, shown, law, u1, v0);
			}
		}
	}

	/** One corner of the sphere: its light by how edge-on it is seen (a bubble's rim), faint from within. */
	private static void corner(VertexConsumer vc, PoseStack.Pose pose, float r, double polar, double azimuth, Vec3 eye, boolean inside, float[] rgb,
							   float shown, Law law, float u, float v) {
		float nx = (float) (Math.sin(polar) * Math.cos(azimuth)), ny = (float) Math.cos(polar), nz = (float) (Math.sin(polar) * Math.sin(azimuth));
		float x = nx * r, y = ny * r, z = nz * r;
		float alpha;
		if (inside) {
			alpha = law == null ? MEMBRANE_INSIDE : PATTERN_INSIDE;
		} else {
			double dx = eye.x - x, dy = eye.y - y, dz = eye.z - z;
			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
			float rim = 1 - (float) Math.min(1, Math.abs(dx * nx + dy * ny + dz * nz) / Math.max(1.0e-4, distance));
			alpha = law == null ? MEMBRANE + (MEMBRANE_RIM - MEMBRANE) * rim * rim : PATTERN * (0.45f + 0.55f * rim);
		}
		SpellRender.vertex(vc, pose, x, y, z, rgb[0], rgb[1], rgb[2], alpha * shown, u, v, SpellRender.FULL_BRIGHT, nx, ny, nz);
	}

	/** The Law's own stuff the sphere is patterned with. */
	private static ResourceLocation skin(Law law) {
		return switch (law) {
			case METAL -> SpellRender.SLASH;
			case WOOD -> SpellRender.VINE;
			case WATER -> SpellRender.WATER;
			case FIRE -> SpellRender.FLAME;
			case EARTH -> SpellRender.VEINS;
			case SPACE -> SpellRender.STAR;
			case TIME -> SpellRender.RING;
			case LIFE_AND_DEATH -> SpellRender.SMOKE;
		};
	}

	/**
	 * What the pattern is tinted: the Law's colour, but for the stuff drawn in its own colours (vines, veins), only a touch of it; and Life
	 * and Death's smoke the black beside its bone-white membrane (white on a day's sky, it was all but lost: seen in game, 2026-10-26).
	 */
	private static float[] skinTint(Law law, float[] tint) {
		if (law == Law.LIFE_AND_DEATH) return DEATH;
		if (law != Law.WOOD && law != Law.EARTH) return tint;
		return new float[]{Mth.lerp(0.3f, 1, tint[0]), Mth.lerp(0.3f, 1, tint[1]), Mth.lerp(0.3f, 1, tint[2])};
	}

	private static final float[] DEATH = {0.15f, 0.15f, 0.18f};

	/** Frames in the pattern's strip (Water's and Fire's are drawn moving), else 1. */
	private static int frames(Law law) {
		return law == Law.WATER ? SpellRender.WATER_FRAMES : law == Law.FIRE ? SpellRender.FLAME_FRAMES : 1;
	}

	/** How often the pattern goes round the sphere, and pole to pole (a strip's one frame spans it). */
	private static float around(Law law) {
		return switch (law) {
			case METAL -> 6;
			case FIRE -> 10;
			case SPACE -> 14;
			case TIME -> 12;
			default -> 8;
		};
	}

	private static float up(Law law) {
		return switch (law) {
			case METAL, LIFE_AND_DEATH -> 3;
			case SPACE -> 7;
			case TIME -> 6;
			default -> 4;
		};
	}

	/** How fast the pattern drifts round it (texture widths a tick). */
	private static float drift(Law law) {
		return switch (law) {
			case METAL -> 0.004f;
			case WATER -> 0.002f;
			case FIRE -> 0.001f;
			case EARTH -> 0.0005f;
			case SPACE -> 0.0015f;
			case TIME -> -0.003f; // the other way round, as the hands of a clock go seen from beneath
			case LIFE_AND_DEATH -> 0.003f;
			default -> 0.001f;
		};
	}

	// --- The seal under it ---

	/**
	 * Its seal on the ground under its owner, as wide as the sphere is there, turning slowly, both seen by day (see-through) and glowing; and a
	 * ring running out over it every two seconds. Over no ground it reaches, the seal lies at the owner's feet.
	 */
	private static void seal(MultiBufferSource buffers, PoseStack pose, Entity owner, Vec3 centre, float r, float[] tint, float shown, float time, Law law) {
		float drop = groundBelow(owner, centre, r);
		if (drop < 0) drop = owner.getBbHeight() / 2;
		float across = Mth.sqrt(Math.max(0, r * r - drop * drop));
		if (across < 0.5f) return;
		ResourceLocation seal = seal(law);
		float spin = time * (law == Law.TIME ? -0.006f : 0.004f);
		pose.pushPose();
		pose.translate(0, -drop + 0.06f, 0);
		SpellRender.disc(buffers.getBuffer(SpellRender.clear(seal)), pose.last(), across, 0, spin, tint, 0.5f * shown, SpellRender.FULL_BRIGHT);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(seal)), pose.last(), across, 0.01f, spin, tint, 0.45f * shown, SpellRender.FULL_BRIGHT);
		float wave = (time % 40) / 40f;
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RING)), pose.last(), across * SpellRender.easeOut(wave), 0.02f, 0, tint,
				0.7f * shown * (1 - wave), SpellRender.FULL_BRIGHT);
		pose.popPose();
	}

	/**
	 * Its seal (tools/beast-art Icons#writeDomains, 1024 square: it lies tens of blocks across, where the wards' 256 would show its pixels):
	 * the five elements' seals, Space's 空 and Time's 時 in the rune's rings, Life and Death's the rune's taiji.
	 */
	private static ResourceLocation seal(Law law) {
		return SEALS.computeIfAbsent(law, l -> DefyingTheHeavens.id("textures/spell/domain_seal_" + l.getId() + ".png"));
	}

	private static final Map<Law, ResourceLocation> SEALS = new java.util.EnumMap<>(Law.class);

	/** How far below the sphere's middle the ground under {@code owner} is (the top of what stands there), within its reach; or -1. */
	private static float groundBelow(Entity owner, Vec3 centre, float r) {
		Level level = owner.level();
		Vec3 feet = owner.position();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int reach = (int) Math.ceil(r);
		for (int dy = 0; dy <= reach; dy++) {
			pos.set(Mth.floor(feet.x), Mth.floor(feet.y + 0.5) - dy, Mth.floor(feet.z));
			if (!level.isInWorldBounds(pos)) break;
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getCollisionShape(level, pos);
			if (shape.isEmpty()) continue;
			float drop = (float) (centre.y - (pos.getY() + shape.max(Direction.Axis.Y)));
			return drop <= r && drop >= -0.5f ? Math.max(0, drop) : -1;
		}
		return -1;
	}

	// --- Motes in it ---

	/** Motes of the Law in the sphere, more the wider it is, each living a few seconds and moving as its Law's stuff does. */
	private static void motes(MultiBufferSource buffers, PoseStack pose, Quaternionf facing, float r, float[] tint, float shown, float time, Law law, int seed) {
		VertexConsumer vc = buffers.getBuffer(mote(law));
		float[] colour = law == Law.WOOD || law == Law.WATER || law == Law.EARTH ? WHITE : tint;
		int count = 16 + (int) r;
		float size = 0.25f + r * 0.012f;
		for (int i = 0; i < count; i++) {
			long s = seed * 131L + i * 17L;
			float life = 50 + SpellRender.hash(s) * 70;
			float t = ((time + SpellRender.hash(s + 1) * life) % life) / life;
			double a = SpellRender.hash(s + 2) * Math.PI * 2, b = Math.acos(2 * SpellRender.hash(s + 3) - 1);
			float d = (0.2f + 0.72f * SpellRender.hash(s + 4)) * r;
			if (law == Law.TIME) a += t * Math.PI * 0.6; // wheeling round it
			if (law == Law.WOOD) a += t * 1.5;
			if (law == Law.LIFE_AND_DEATH) d *= 1 - 0.55f * t; // drawn in toward its owner
			float x = (float) (Math.sin(b) * Math.cos(a) * d), y = (float) (Math.cos(b) * d), z = (float) (Math.sin(b) * Math.sin(a) * d);
			switch (law) {
				case FIRE, WATER -> y += t * 3; // embers and bubbles rising
				case EARTH -> y -= t * 2.5f; // dust settling
				default -> {}
			}
			if (x * x + y * y + z * z > r * r) continue; // drifted out of it
			float alpha = shown * Mth.sin(Mth.PI * t) * (law == Law.SPACE || law == Law.METAL ? 0.6f + 0.4f * Mth.sin(time * 0.4f + i) : 1);
			float roll = law == Law.WOOD || law == Law.TIME ? time * 0.05f + i : 0;
			SpellRender.billboard(vc, pose, facing, x, y, z, size * (0.7f + 0.6f * SpellRender.hash(s + 5)), roll, colour, alpha, 0, 0, 1, 1,
					SpellRender.FULL_BRIGHT);
		}
	}

	/** The motes' look: glints, leaves, bubbles, embers, dust, stars, rings, wisps. */
	private static RenderType mote(Law law) {
		return switch (law) {
			case METAL -> SpellRender.glow(SpellRender.SPARK);
			case WOOD -> SpellRender.clear(SpellRender.LEAF);
			case WATER -> SpellRender.clear(SpellRender.BUBBLE);
			case FIRE -> SpellRender.glow(SpellRender.SPARK);
			case EARTH -> SpellRender.clear(SpellRender.DUST);
			case SPACE -> SpellRender.glow(SpellRender.STAR);
			case TIME -> SpellRender.glow(SpellRender.RING);
			case LIFE_AND_DEATH -> SpellRender.clear(SpellRender.SMOKE);
		};
	}

	private ClientLawDomains() {}
}
