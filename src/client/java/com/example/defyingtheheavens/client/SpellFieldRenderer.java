package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpellField;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything an element spell leaves standing (SpellField), by its kind: rain clouds, prisons of water and of thorns, a yang bomb,
 * streams of life, a tornado of fire, a small sun, mist, a jet of water, thorn spears, a lotus, a quake's cracks, a great wave, sword
 * light, a sea of flames, bamboo, a rain of life, a seal of earth, a river from the sky, a field of frost, a domain of swords,
 * binding vines, the World Tree and its vines, a mountain rising, a sphere of sea, an array of four swords, a ridge of spikes, a beam
 * of frost, a ball of water gathering, a ring of swords, a glowing mark where something will fall, a boulder forming. Each grows in
 * and fades out by its life ({@link SpellField#progress}); the field's own numbers say how big and which way.
 */
public class SpellFieldRenderer extends EntityRenderer<SpellField> {
	private static final float[] WHITE = {1, 1, 1};
	private static final float[] FIRE_CORE = {1.0f, 0.95f, 0.75f}, FIRE_MID = {1.0f, 0.55f, 0.15f}, FIRE_OUTER = {0.95f, 0.22f, 0.06f};
	private static final float[] WATER_GLOW = {0.35f, 0.75f, 1.0f}, ICE = {0.65f, 0.9f, 1.0f}, LIFE = {0.35f, 1.0f, 0.45f}, GOLD = {1.0f, 0.82f, 0.4f};
	private static final float[] SWORD_LIGHT = {1.0f, 0.95f, 0.8f}, EARTH = {0.85f, 0.6f, 0.3f};

	public SpellFieldRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0;
	}

	@Override
	public ResourceLocation getTextureLocation(SpellField field) {
		return SpellRender.GLOW;
	}

	@Override
	public boolean shouldRender(SpellField field, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void render(SpellField field, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
		float age = field.tickCount + partial;
		Vec3 at = field.getPosition(partial);
		Vec3 cam = entityRenderDispatcher.camera.getPosition().subtract(at);
		Quaternionf facing = entityRenderDispatcher.cameraOrientation();
		float life = Math.max(1, field.life());
		pose.pushPose();
		switch (field.kind()) {
			case RAIN_CLOUD -> rainCloud(field, pose, buffers, facing, cam, age, life, false);
			case SPRING_RAIN -> springRain(field, pose, buffers, facing, cam, age, life);
			case WATER_PRISON -> waterSphere(field, pose, buffers, facing, age, life, field.radius(), true);
			case BOUNDLESS_SEA -> {
				pose.translate(0, -field.radius(), 0);
				waterSphere(field, pose, buffers, facing, age, life, field.radius(), false);
			}
			case BRISTLE_PRISON -> bristles(field, pose, buffers, age, life, light, false);
			case VINE_BINDING -> bristles(field, pose, buffers, age, life, light, true);
			case YANG_BOMB -> yangBomb(field, pose, buffers, facing, cam, age);
			case LIFE_STREAM -> lifeStream(field, pose, buffers, facing, at, partial, age, life);
			case CRIMSON_SWIRL -> swirl(field, pose, buffers, facing, cam, age, life);
			case SOLAR_SUN -> sun(field, pose, buffers, facing, age, life);
			case MIST_VEIL -> mist(field, pose, buffers, facing, age, life);
			case TIDAL_LANCE -> lance(field, pose, buffers, facing, age);
			case THORN_SPEARS -> spears(field, pose, buffers, at, age, life, light);
			case LOTUS -> lotus(field, pose, buffers, facing, cam, age, life, light);
			case EARTHQUAKE -> quake(field, pose, buffers, facing, at, age, life, light);
			case GREAT_TIDE -> tide(field, pose, buffers, facing, age, life);
			case SWORD_LIGHT -> swordLight(field, pose, buffers, facing, age, life);
			case SEA_OF_FLAMES -> seaOfFlames(field, pose, buffers, facing, cam, age, life);
			case BAMBOO -> bamboo(field, pose, buffers, age, life, light);
			case HEAVY_SEAL -> seal(field, pose, buffers, facing, age, life);
			case RIVER_TORRENT -> river(field, pose, buffers, facing, age, life);
			case FROZEN_FIELD -> frozenField(field, pose, buffers, facing, cam, age, life);
			case SWORD_DOMAIN -> domain(field, pose, buffers, facing, age, life);
			case WORLD_TREE -> worldTree(field, pose, buffers, facing, age, life, light);
			case TREE_VINE -> treeVine(field, pose, buffers, at, partial, age, life, light);
			case MOUNTAIN_SEAL -> mountain(field, pose, buffers, age, life, light);
			case SWORD_ARRAY -> array(field, pose, buffers, facing, age, life);
			case EARTH_SPIKES -> spikes(field, pose, buffers, at, age, life, light);
			case FROST_BEAM -> frostBeam(field, pose, buffers, facing, at, partial, age, life);
			case BLAST -> blast(field, pose, buffers, facing, age, life);
			case WATER_GATHER -> gather(field, pose, buffers, facing, age, life);
			case SWORD_RING -> swordRing(field, pose, buffers, facing, age, life);
			case METEOR_MARK -> mark(field, pose, buffers, age, life);
			case BOULDER_FORMING -> {
				float g = SpellRender.easeOut(age / life);
				SpellModels.boulder(pose, buffers, SpellRender.sprite(field.data() == 0 ? Block.getId(Blocks.DIRT.defaultBlockState()) : field.data()),
						0.95f * g, age * 0.3f, light, field.getId(), null);
			}
			default -> {}
		}
		pose.popPose();
	}

	private static void glowBall(MultiBufferSource buffers, PoseStack pose, Quaternionf facing, float x, float y, float z, float size, float[] rgb, float alpha) {
		SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose, facing, x, y, z, size, 0, rgb, alpha, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
	}

	/** A seeded number from 0 to 1, the same every frame. */
	private static float h(long seed) {
		return SpellRender.hash(seed);
	}

	/** Where the ground is under a point (relative to {@code origin}): the top of the first solid block within 8 below 3 above, else 0. */
	private static float groundAt(Vec3 origin, double dx, double dz, double baseY) {
		var level = Minecraft.getInstance().level;
		Entity viewer = Minecraft.getInstance().player; // a ClipContext needs an entity for its collision rules (null fails in 1.20.1)
		if (level == null || viewer == null) return (float) baseY;
		Vec3 from = origin.add(dx, baseY + 3, dz), to = origin.add(dx, baseY - 8, dz);
		var hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
		return hit.getType() == HitResult.Type.MISS ? (float) baseY : (float) (hit.getLocation().y - origin.y);
	}

	// --- Water ---

	/**
	 * Rain Call: a heavy grey cloud of piled billows, slowly turning, rain streaking down from its belly to the ground ({@code height}
	 * below it), rings where it lands.
	 */
	private void rainCloud(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, float age, float life, boolean green) {
		float r = field.radius(), fall = field.height();
		float show = SpellRender.fade(age, life, 30, 40);
		long seed = field.data();
		VertexConsumer clouds = buffers.getBuffer(SpellRender.clear(SpellRender.CLOUD));
		for (int i = 0; i < 22; i++) {
			double a = h(seed + i) * Math.PI * 2 + age * 0.004;
			double d = Math.sqrt(h(seed + i * 3 + 1)) * r * 0.85;
			float y = (h(seed + i * 5 + 2) - 0.5f) * 1.6f;
			float size = (3.2f + h(seed + i * 7 + 3) * 3.0f) * Math.max(0.3f, show);
			float shade = 0.42f + 0.25f * h(seed + i * 11 + 4) + y * 0.08f;
			SpellRender.billboard(clouds, pose, facing, (float) (Math.cos(a) * d), y, (float) (Math.sin(a) * d), size, (float) (age * 0.003 + i),
					new float[]{shade, shade, shade + 0.03f}, 0.92f * show, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
		// The rain: streaks falling the whole way, each its own place and pace.
		VertexConsumer rain = buffers.getBuffer(SpellRender.clear(SpellRender.RAIN));
		VertexConsumer rings = buffers.getBuffer(SpellRender.clear(SpellRender.RIPPLE));
		int drops = (int) (r * r * 1.5f);
		for (int i = 0; i < drops; i++) {
			long s = seed * 31 + i;
			double a = h(s) * Math.PI * 2, d = Math.sqrt(h(s + 1)) * r;
			float x = (float) (Math.cos(a) * d), z = (float) (Math.sin(a) * d);
			float phase = (age * 0.9f + h(s + 2) * fall) % fall;
			float y = -1 - phase;
			float yaw = (float) Math.atan2(cam.x - x, cam.z - z);
			pose.pushPose();
			pose.translate(x, y, z);
			pose.mulPose(Axis.YP.rotation(yaw));
			PoseStack.Pose p = pose.last();
			SpellRender.quad(rain, p, SpellRender.v(-0.04f, 0, 0), SpellRender.v(-0.04f, -1.1f, 0), SpellRender.v(0.04f, -1.1f, 0), SpellRender.v(0.04f, 0, 0),
					new float[]{0.75f, 0.85f, 1.0f}, 0.7f * show, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			pose.popPose();
			if (i % 4 == 0 && phase > fall - 1.5f) {
				float ring = 1 - (fall - phase) / 1.5f;
				pose.pushPose();
				pose.translate(x, -fall + 0.05f, z);
				SpellRender.disc(rings, pose.last(), 0.15f + ring * 0.4f, 0, 0, WHITE, 0.6f * (1 - ring) * show, SpellRender.FULL_BRIGHT);
				pose.popPose();
			}
		}
	}

	/** Spring of Myriad Life: a soft green rain of life falling all round, glowing motes drifting up through it. */
	private void springRain(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, float age, float life) {
		float r = field.radius(), top = field.height();
		float show = SpellRender.fade(age, life, 20, 30);
		VertexConsumer rain = buffers.getBuffer(SpellRender.glow(SpellRender.RAIN));
		for (int i = 0; i < 260; i++) {
			long s = field.getId() * 31L + i;
			double a = h(s) * Math.PI * 2, d = Math.sqrt(h(s + 1)) * r;
			float x = (float) (Math.cos(a) * d), z = (float) (Math.sin(a) * d);
			float y = top - (age * 0.7f + h(s + 2) * top) % top;
			float yaw = (float) Math.atan2(cam.x - x, cam.z - z);
			pose.pushPose();
			pose.translate(x, y, z);
			pose.mulPose(Axis.YP.rotation(yaw));
			SpellRender.quad(rain, pose.last(), SpellRender.v(-0.05f, 0, 0), SpellRender.v(-0.05f, -0.9f, 0), SpellRender.v(0.05f, -0.9f, 0), SpellRender.v(0.05f, 0, 0),
					LIFE, 0.6f * show, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			pose.popPose();
		}
		VertexConsumer motes = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 80; i++) {
			long s = field.getId() * 17L + i;
			double a = h(s) * Math.PI * 2 + age * 0.01, d = Math.sqrt(h(s + 1)) * r;
			float y = (age * 0.05f + h(s + 2) * 6) % 6;
			SpellRender.billboard(motes, pose, facing, (float) (Math.cos(a) * d), y, (float) (Math.sin(a) * d), 0.3f, 0, LIFE, show * (1 - y / 6), 0, 0, 1, 1,
					SpellRender.FULL_BRIGHT);
		}
	}

	/**
	 * A sphere of water ({@code radius}, standing on the field: Water Prison round its prisoner; Boundless Sea round its point): moving
	 * water, its skin swelling and rippling, currents turning inside, bubbles rising. A prison's strength ({@code value}) shows: the
	 * weaker, the more it shudders.
	 */
	private void waterSphere(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life, float radius, boolean prison) {
		float show = SpellRender.fade(age, life, 6, 6);
		float shudder = prison ? (1 - field.value()) * 0.12f : 0;
		pose.translate(0, radius, 0);
		float r = radius * (0.4f + 0.6f * SpellRender.easeOut(Math.min(1, age / 6)));
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		SpellRender.Wobble wobble = (polar, azimuth) -> 1 + (0.035f + shudder) * Mth.sin((float) (azimuth * 4 + age * 0.35)) * Mth.sin((float) (polar * 3 + age * 0.27))
				+ shudder * 0.5f * Mth.sin(age * 2.1f + (float) polar * 7);
		SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), r, 14, 22, WHITE, 0.65f * show, 3, (fv[1] - fv[0]) * 3,
				age * 0.006f, fv[0], SpellRender.FULL_BRIGHT, wobble);
		SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), r * 0.97f, 12, 18, new float[]{0.55f, 0.75f, 1.0f}, 0.3f * show,
				2, (fv[1] - fv[0]) * 2, -age * 0.01f, fv[0], SpellRender.FULL_BRIGHT, wobble);
		// Currents turning inside: two rings of water wheeling on tilted axes.
		for (int k = 0; k < 2; k++) {
			pose.pushPose();
			pose.mulPose(Axis.XP.rotationDegrees(55 + k * 70));
			pose.mulPose(Axis.YP.rotationDegrees(age * (k == 0 ? 6 : -4)));
			List<Vec3> ring = new ArrayList<>();
			float[] radii = new float[25];
			for (int i = 0; i < 25; i++) {
				double a = Math.PI * 2 * i / 24 * 0.85;
				ring.add(new Vec3(Math.cos(a) * r * 0.7, 0, Math.sin(a) * r * 0.7));
				radii[i] = r * 0.07f * Mth.sin((float) Math.PI * i / 24);
			}
			SpellRender.tube(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), ring, radii, 6, new float[]{0.8f, 0.95f, 1.0f}, 0.5f * show,
					0.3f, fv[0], SpellRender.FULL_BRIGHT, null);
			pose.popPose();
		}
		glowBall(buffers, pose, facing, 0, 0, 0, r * 2.2f, WATER_GLOW, 0.18f * show);
		VertexConsumer bubbles = buffers.getBuffer(SpellRender.clear(SpellRender.BUBBLE));
		for (int i = 0; i < 18; i++) {
			long s = field.getId() * 13L + i;
			double a = h(s) * Math.PI * 2;
			float d = h(s + 1) * r * 0.75f;
			float y = -r * 0.8f + (age * 0.06f * (0.6f + h(s + 2)) * r + h(s + 3) * r * 1.6f) % (r * 1.6f);
			SpellRender.billboard(bubbles, pose, facing, (float) Math.cos(a) * d, y, (float) Math.sin(a) * d, 0.1f + h(s + 4) * 0.12f * r, 0, WHITE, 0.8f * show,
					0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** Tidal Lance: a jet of water from the caster's hands, its surface racing forward, spiralling sheaths round it, foam where it strikes. */
	private void lance(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age) {
		float length = Math.max(1, field.value());
		pose.mulPose(Axis.YP.rotationDegrees(-field.yaw()));
		pose.mulPose(Axis.XP.rotationDegrees(field.pitch()));
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		List<Vec3> line = new ArrayList<>();
		float[] radii = new float[9];
		for (int i = 0; i < 9; i++) {
			line.add(new Vec3(0, 0, length * i / 8.0));
			radii[i] = 0.18f + 0.12f * (i / 8f) + 0.03f * Mth.sin(age * 1.5f + i);
		}
		SpellRender.tube(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), line, radii, 10, WHITE, 0.85f, 0.4f, fv[0] - age * 0.25f,
				SpellRender.FULL_BRIGHT, null);
		for (int k = 0; k < 3; k++) {
			List<Vec3> spiral = new ArrayList<>();
			float[] r = new float[30];
			for (int i = 0; i < 30; i++) {
				double t = i / 29.0;
				double a = -age * 0.7 + k * Math.PI * 2 / 3 + t * length * 1.2;
				double rad = 0.32 + 0.1 * t;
				spiral.add(new Vec3(Math.cos(a) * rad, Math.sin(a) * rad, t * length));
				r[i] = 0.04f;
			}
			SpellRender.tube(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), spiral, r, 5, new float[]{0.85f, 0.95f, 1.0f}, 0.7f, 0.5f, fv[0],
					SpellRender.FULL_BRIGHT, null);
		}
		VertexConsumer foam = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
		for (int i = 0; i < 6; i++) {
			long s = field.getId() * 7L + i + (long) (age / 2);
			SpellRender.billboard(foam, pose, facing, (h(s) - 0.5f) * 0.9f, (h(s + 1) - 0.5f) * 0.9f, length + (h(s + 2) - 0.5f) * 0.6f, 0.8f + h(s + 3) * 0.6f, h(s + 4) * 6,
					new float[]{0.85f, 0.95f, 1.0f}, 0.7f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/**
	 * Great Tide: a wall of water as wide as the field's radius and as tall as its height, its face rising to a crest that curls over,
	 * white foam along the lip, spray flung off it; its ends sinking away.
	 */
	private void tide(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		float w = field.radius(), hgt = field.height();
		float show = SpellRender.fade(age, life, 8, 10);
		pose.mulPose(Axis.YP.rotationDegrees(-field.yaw()));
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		// The wave's section, from its foot behind to the curling lip ahead (z forward, y up), as shares of its height.
		float[][] profile = {{-6, 0}, {-4, 0.25f}, {-2.2f, 0.6f}, {-0.8f, 0.88f}, {0.3f, 1.0f}, {1.3f, 0.98f}, {2.0f, 0.86f}, {2.3f, 0.7f}, {2.1f, 0.58f}};
		int across = 16;
		VertexConsumer water = buffers.getBuffer(SpellRender.clear(SpellRender.WATER));
		for (int i = 0; i < across; i++) {
			float x0 = -w / 2 + w * i / across, x1 = -w / 2 + w * (i + 1) / across;
			float e0 = Mth.sin((float) Math.PI * i / across), e1 = Mth.sin((float) Math.PI * (i + 1) / across);
			for (int k = 0; k < profile.length - 1; k++) {
				float z0 = profile[k][0], y0 = profile[k][1] * hgt, z1 = profile[k + 1][0], y1 = profile[k + 1][1] * hgt;
				float sway0 = Mth.sin(age * 0.3f + i * 0.6f) * 0.3f, sway1 = Mth.sin(age * 0.3f + (i + 1) * 0.6f) * 0.3f;
				float v0 = fv[0] + k * 0.12f - age * 0.02f, v1 = fv[0] + (k + 1) * 0.12f - age * 0.02f;
				boolean lip = k >= profile.length - 4;
				float[] c = lip ? new float[]{0.9f, 0.97f, 1.0f} : WHITE;
				SpellRender.quad(water, pose.last(), SpellRender.v(x0, y0 * e0 * show, z0 + sway0), SpellRender.v(x0, y1 * e0 * show, z1 + sway0),
						SpellRender.v(x1, y1 * e1 * show, z1 + sway1), SpellRender.v(x1, y0 * e1 * show, z0 + sway1), c, 0.85f * show, i / (float) across, v0,
						(i + 1) / (float) across, v1, SpellRender.FULL_BRIGHT);
			}
		}
		// Foam along the lip, spray off it.
		VertexConsumer foam = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
		for (int i = 0; i < 24; i++) {
			long s = field.getId() * 29L + i + (long) (age / 3);
			float x = -w / 2 + w * (i + h(s) * 0.5f) / 24;
			float edge = Mth.sin((float) Math.PI * (x + w / 2) / w);
			SpellRender.billboard(foam, pose, facing, x, hgt * 0.95f * edge * show + h(s + 1), 1.2f + h(s + 2) * 1.5f, 1.4f + h(s + 3) * 1.4f, h(s + 4) * 6,
					WHITE, 0.75f * show, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** Heavenly River Torrent: a river along the sky over its line, pouring down in a sheet onto it, foaming where it lands. */
	private void river(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		float length = field.height(), width = field.radius() * 2;
		float pour = SpellRender.easeOut(Math.min(1, age / 6));
		float show = SpellRender.fade(age, life, 2, 10);
		pose.mulPose(Axis.YP.rotationDegrees(-field.yaw()));
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		VertexConsumer water = buffers.getBuffer(SpellRender.clear(SpellRender.WATER));
		float top = 22, bottom = top * (1 - pour);
		int segments = 12;
		for (int i = 0; i < segments; i++) {
			float z0 = length * i / segments, z1 = length * (i + 1) / segments;
			// The river in the sky, then its falling sheet.
			SpellRender.quad(water, pose.last(), SpellRender.v(-width, top, z0), SpellRender.v(width, top, z0), SpellRender.v(width, top, z1), SpellRender.v(-width, top, z1),
					WHITE, 0.8f * show, 0, fv[0] + z0 * 0.1f - age * 0.05f, 1, fv[0] + z1 * 0.1f - age * 0.05f, SpellRender.FULL_BRIGHT);
			for (int side = -1; side <= 1; side += 2) {
				float x = side * width * 0.5f;
				SpellRender.quad(water, pose.last(), SpellRender.v(x, top, z0), SpellRender.v(x, bottom, z0), SpellRender.v(x, bottom, z1), SpellRender.v(x, top, z1),
						WHITE, 0.75f * show, fv[0] - age * 0.12f, 0, fv[0] - age * 0.12f + 2, 1, SpellRender.FULL_BRIGHT);
			}
		}
		if (pour >= 1) {
			VertexConsumer foam = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
			for (int i = 0; i < 30; i++) {
				long s = field.getId() * 23L + i + (long) (age / 2);
				SpellRender.billboard(foam, pose, facing, (h(s) - 0.5f) * width * 1.6f, h(s + 1) * 1.5f, h(s + 2) * length, 1.8f + h(s + 3) * 1.5f, h(s + 4) * 6,
						WHITE, 0.7f * show, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		}
	}

	/** Torrential Spring gathering: streams of water spiralling in from all round into a growing ball. */
	private void gather(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		float g = SpellRender.easeOut(age / life);
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		float r = field.radius() * g;
		if (r > 0.05f) {
			SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), r, 12, 18, WHITE, 0.8f, 2, (fv[1] - fv[0]) * 2, age * 0.01f, fv[0],
					SpellRender.FULL_BRIGHT, (polar, azimuth) -> 1 + 0.08f * Mth.sin((float) (azimuth * 3 + age * 0.5)) * Mth.sin((float) (polar * 4)));
		}
		for (int k = 0; k < 8; k++) {
			List<Vec3> stream = new ArrayList<>();
			float[] radii = new float[16];
			double base = k * Math.PI * 2 / 8;
			for (int i = 0; i < 16; i++) {
				double t = i / 15.0;
				double reach = r + (1 - t) * 7 * (1 - g * 0.7);
				double a = base + age * 0.15 + t * 2.2;
				stream.add(new Vec3(Math.cos(a) * reach, (1 - t) * (k % 2 == 0 ? -4 : 3) * (1 - g * 0.5), Math.sin(a) * reach));
				radii[i] = (float) (0.12 + 0.18 * t) * (1 - g * 0.3f);
			}
			SpellRender.tube(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), stream, radii, 6, WHITE, 0.75f, 0.3f, fv[0] - age * 0.1f,
					SpellRender.FULL_BRIGHT, null);
		}
		glowBall(buffers, pose, facing, 0, 0, 0, r * 2.5f + 1, WATER_GLOW, 0.3f);
	}

	/** Mist Veil: a dome of slow mist round its caster, its billows drifting and turning. */
	private void mist(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		float r = field.radius();
		float show = SpellRender.fade(age, life, 30, 40);
		VertexConsumer vc = buffers.getBuffer(SpellRender.clear(SpellRender.MIST));
		for (int i = 0; i < 70; i++) {
			long s = field.data() * 37L + i;
			double a = h(s) * Math.PI * 2 + age * 0.003 * (h(s + 5) - 0.5);
			double d = (0.25 + 0.75 * Math.sqrt(h(s + 1))) * r;
			float y = h(s + 2) * 5.5f * (float) (1 - d / r * 0.5);
			SpellRender.billboard(vc, pose, facing, (float) (Math.cos(a) * d), y, (float) (Math.sin(a) * d), 4.5f + h(s + 3) * 3, (float) (age * 0.004 + i),
					new float[]{0.86f, 0.9f, 0.95f}, 0.55f * show, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/**
	 * Frozen Field: the cold running out across the ground as a bright ring of frost, the frosted ground behind it, snow glinting in the
	 * air; the whole field dimming as it thaws.
	 */
	private void frozenField(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, float age, float life) {
		float spread = 60;
		float r = field.radius() * Math.min(1, age / spread);
		float thaw = 1 - Mth.clamp((age - spread) / Math.max(1, life - spread), 0, 1);
		VertexConsumer ground = buffers.getBuffer(SpellRender.clear(SpellRender.ICE));
		pose.pushPose();
		SpellRender.disc(ground, pose.last(), r, 0.15f, 0, new float[]{0.85f, 0.95f, 1.0f}, 0.35f * thaw, SpellRender.FULL_BRIGHT);
		pose.popPose();
		if (age < spread + 10) {
			VertexConsumer ring = buffers.getBuffer(SpellRender.glow(SpellRender.RING));
			SpellRender.disc(ring, pose.last(), r * 1.08f + 1, 0.3f, age * 0.01f, ICE, Math.max(0, 1 - (age - spread) / 10), SpellRender.FULL_BRIGHT);
		}
		// Snow glinting near the viewer (within the field).
		VertexConsumer snow = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 90; i++) {
			long s = field.getId() * 41L + i;
			float x = (float) cam.x + (h(s) - 0.5f) * 24, z = (float) cam.z + (h(s + 1) - 0.5f) * 24;
			if (x * x + z * z > r * r) continue;
			float y = (float) cam.y + 8 - (age * 0.08f + h(s + 2) * 16) % 16;
			SpellRender.billboard(snow, pose, facing, x, y, z, 0.12f, 0, WHITE, 0.8f * thaw, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** Absolute Zero's beam: a line of white cold from the caster's hands to the frozen, a spiral of frost round it. */
	private void frostBeam(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 at, float partial, float age, float life) {
		Entity target = field.target();
		if (target == null) return;
		Vec3 to = target.getPosition(partial).add(0, target.getBbHeight() / 2, 0).subtract(at);
		float show = SpellRender.fade(age, life, 1, 8);
		List<Vec3> line = new ArrayList<>();
		float[] radii = new float[10];
		for (int i = 0; i < 10; i++) {
			line.add(to.scale(i / 9.0));
			radii[i] = 0.12f + 0.05f * Mth.sin(age * 2 + i);
		}
		SpellRender.tube(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), line, radii, 6, ICE, show, 0.2f, 0, SpellRender.FULL_BRIGHT, null);
		List<Vec3> spiral = new ArrayList<>();
		float[] r = new float[40];
		Vec3 dir = to.normalize();
		Vec3 side = dir.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(dir).normalize();
		for (int i = 0; i < 40; i++) {
			double t = i / 39.0, a = t * 18 - age;
			spiral.add(to.scale(t).add(side.scale(Math.cos(a) * 0.3)).add(up.scale(Math.sin(a) * 0.3)));
			r[i] = 0.04f;
		}
		SpellRender.tube(buffers.getBuffer(SpellRender.clear(SpellRender.ICE)), pose.last(), spiral, r, 4, WHITE, 0.8f * show, 0.5f, 0, SpellRender.FULL_BRIGHT, null);
		glowBall(buffers, pose, facing, (float) to.x, (float) to.y, (float) to.z, 3, ICE, 0.6f * show);
	}

	// --- Fire ---

	/** A Remote Yang Bomb: a little sun of yang fire, pulsing, seen only by its caster and by those with Qi Sense open. */
	private void yangBomb(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, float age) {
		var mc = Minecraft.getInstance();
		Entity owner = field.owner();
		if (owner != mc.player && !QiSenseClientHandler.isActive()) return;
		float pulse = 1 + 0.15f * Mth.sin(age * 0.4f);
		glowBall(buffers, pose, facing, 0, 0, 0, 2.2f * pulse, FIRE_MID, 0.45f);
		glowBall(buffers, pose, facing, 0, 0, 0, 1.0f * pulse, new float[]{1.0f, 0.95f, 0.7f}, 0.95f);
		glowBall(buffers, pose, facing, 0, 0, 0, 0.45f, WHITE, 1);
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.YANG));
		for (int i = 0; i < 6; i++) {
			double a = i * Math.PI * 2 / 6 + age * 0.05;
			SpellRender.flameTongue(flames, pose, cam, (float) Math.cos(a) * 0.18f, -0.15f, (float) Math.sin(a) * 0.18f, 0.35f, 0.6f, age + i * 4, WHITE, 0.8f,
					SpellRender.FLAME_FRAMES);
		}
		VertexConsumer spark = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 5; i++) {
			double a = age * 0.12 + i * Math.PI * 2 / 5;
			SpellRender.billboard(spark, pose, facing, (float) Math.cos(a) * 0.55f, (float) Math.sin(a * 2) * 0.2f, (float) Math.sin(a) * 0.55f, 0.18f, 0, WHITE, 0.9f,
					0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/**
	 * Crimson Swirl: a funnel of fire narrow at the ground and wide at the top, its walls of flame wheeling round at different paces,
	 * a white-hot core, embers spiralling up it and smoke above.
	 */
	private void swirl(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, float age, float life) {
		float r = field.radius(), hgt = field.height();
		float show = SpellRender.fade(age, life, 15, 25);
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.FLAME_FRAMES);
		VertexConsumer flame = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		for (int layer = 0; layer < 4; layer++) {
			float scale = 1 - layer * 0.18f;
			int l = layer;
			pose.pushPose();
			pose.mulPose(Axis.YP.rotation(age * (0.35f + layer * 0.12f) * (layer % 2 == 0 ? 1 : -0.7f)));
			SpellRender.cylinder(flame, pose.last(), hgt * show, t -> r * scale * (0.25f + 1.05f * t * t + 0.06f * Mth.sin(t * 9 + age * 0.3f + l)), 10, 14,
					l == 0 ? FIRE_OUTER : l == 3 ? FIRE_CORE : FIRE_MID, 0.75f * show, 3, fv[1] - fv[0], 0, fv[0], 1.6f, SpellRender.FULL_BRIGHT,
					t -> Math.min(1, t * 6) * (1 - t * 0.5f));
			pose.popPose();
		}
		VertexConsumer core = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		SpellRender.cylinder(core, pose.last(), hgt * 0.8f * show, t -> 0.4f + t * 0.6f, 6, 8, FIRE_CORE, 0.6f * show, 1, 1, 0, 0, 0, SpellRender.FULL_BRIGHT,
				t -> 1 - t);
		VertexConsumer spark = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 40; i++) {
			long s = field.data() * 13L + i;
			float t = (age * 0.015f * (0.6f + h(s)) + h(s + 1)) % 1;
			double a = h(s + 2) * Math.PI * 2 + age * 0.4 + t * 6;
			float rr = r * (0.25f + 1.05f * t * t) * (0.7f + 0.3f * h(s + 3));
			SpellRender.billboard(spark, pose, facing, (float) Math.cos(a) * rr, t * hgt * show, (float) Math.sin(a) * rr, 0.3f, 0, FIRE_MID, show * (1 - t),
					0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
		VertexConsumer smoke = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
		for (int i = 0; i < 10; i++) {
			long s = field.data() * 7L + i;
			double a = h(s) * Math.PI * 2 + age * 0.05;
			SpellRender.billboard(smoke, pose, facing, (float) Math.cos(a) * r * 1.2f, hgt * show + h(s + 1) * 3, (float) Math.sin(a) * r * 1.2f, 4 + h(s + 2) * 3,
					(float) (h(s + 3) * 6 + age * 0.01), new float[]{0.2f, 0.18f, 0.17f}, 0.55f * show, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** Solar Field: a small sun overhead: its boiling face, a corona, long rays wheeling slowly round it, and a great warm glow. */
	private void sun(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		float show = SpellRender.fade(age, life, 20, 20);
		float r = field.radius() * (0.2f + 0.8f * show);
		float[] fv = SpellRender.frame(age, 0.25f, SpellRender.SUN_FRAMES);
		SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.SUN)), pose.last(), r, 12, 18, WHITE, show, 1, fv[1] - fv[0], age * 0.003f, fv[0],
				SpellRender.FULL_BRIGHT, (polar, azimuth) -> 1 + 0.02f * Mth.sin((float) (azimuth * 6 + age * 0.2)));
		glowBall(buffers, pose, facing, 0, 0, 0, r * 4, FIRE_CORE, 0.8f * show);
		glowBall(buffers, pose, facing, 0, 0, 0, r * 9, FIRE_MID, 0.45f * show);
		glowBall(buffers, pose, facing, 0, 0, 0, r * 22, new float[]{1.0f, 0.7f, 0.3f}, 0.18f * show);
		VertexConsumer rays = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 3; i++) {
			SpellRender.billboard(rays, pose, facing, 0, 0, 0, r * 10, r * 10, age * 0.004f * (i + 1) + i * 0.6f, FIRE_CORE, 0.5f * show, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** Sea of Flames: the ground ablaze far round: a red glow over it, tongues of fire everywhere rising and falling, embers drifting up. */
	private void seaOfFlames(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, float age, float life) {
		float r = field.radius();
		float show = SpellRender.fade(age, life, 10, 20);
		float spread = Math.min(1, age / 10);
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		SpellRender.disc(glow, pose.last(), r * 1.3f * spread, 0.2f, 0, FIRE_OUTER, 0.6f * show, SpellRender.FULL_BRIGHT);
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		for (int i = 0; i < 160; i++) {
			long s = field.data() * 53L + i;
			double a = h(s) * Math.PI * 2, d = Math.sqrt(h(s + 1)) * r * spread;
			float x = (float) (Math.cos(a) * d), z = (float) (Math.sin(a) * d);
			float rise = 0.6f + 0.4f * Mth.sin(age * 0.15f + i);
			float size = (1.2f + h(s + 2) * 1.8f) * rise * show;
			SpellRender.flameTongue(flames, pose, cam, x, 0, z, size * 0.7f, size * 1.6f, age + i * 3, FIRE_MID, 0.9f * show, SpellRender.FLAME_FRAMES);
		}
		VertexConsumer spark = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 60; i++) {
			long s = field.data() * 59L + i;
			double a = h(s) * Math.PI * 2, d = Math.sqrt(h(s + 1)) * r * spread;
			float y = (age * 0.08f + h(s + 2) * 6) % 6;
			SpellRender.billboard(spark, pose, facing, (float) (Math.cos(a) * d), y, (float) (Math.sin(a) * d), 0.25f, 0, FIRE_MID, show * (1 - y / 6), 0, 0, 1, 1,
					SpellRender.FULL_BRIGHT);
		}
	}

	/** Where something burning will fall: a ring of runes glowing on the ground, closing and brightening until it lands. */
	private void mark(SpellField field, PoseStack pose, MultiBufferSource buffers, float age, float life) {
		float t = Math.min(1, age / life);
		float[] colour = field.data() == 1 ? EARTH : field.data() == 2 ? GOLD : FIRE_MID;
		VertexConsumer rune = buffers.getBuffer(SpellRender.glow(SpellRender.RUNE));
		float r = field.radius();
		SpellRender.disc(rune, pose.last(), r * (1.4f - 0.4f * t), 0.12f, age * 0.03f, colour, 0.4f + 0.6f * t, SpellRender.FULL_BRIGHT);
		VertexConsumer ring = buffers.getBuffer(SpellRender.glow(SpellRender.RING));
		SpellRender.disc(ring, pose.last(), r * (2.5f - 1.5f * t), 0.15f, 0, colour, 0.7f * t, SpellRender.FULL_BRIGHT);
	}

	/** A blast of fire: a sphere of flame swelling and fading. */
	private void blast(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		float t = Math.min(1, age / life);
		float r = field.radius() * SpellRender.easeOut(t);
		glowBall(buffers, pose, facing, 0, 0, 0, r * 2.5f, FIRE_MID, 1 - t);
		glowBall(buffers, pose, facing, 0, 0, 0, r * 1.2f, FIRE_CORE, 1 - t);
	}

	// --- Wood ---

	/**
	 * Mysterious Bristle Prison and Myriad Vine Binding: thick brambles burst up from the ground round their prisoner, growing upward
	 * in twisting curves and closing over its head, every one bristling with thorns; they writhe a little as they hold, and draw back
	 * into the ground when they let go. A cage struck weaker ({@code value}) shows its brambles cracked short. The binding's are green
	 * vines, leafy, without the cage's crown.
	 */
	private void bristles(SpellField field, PoseStack pose, MultiBufferSource buffers, float age, float life, int light, boolean vines) {
		float r = field.radius(), hgt = field.height();
		float grow = SpellRender.easeOut(Math.min(1, age / 10));
		float retreat = Mth.clamp((life - age) / 8, 0, 1);
		float reach = grow * retreat * (0.45f + 0.55f * field.value());
		long seed = field.data();
		int count = vines ? 7 : 9;
		VertexConsumer stem = buffers.getBuffer(SpellRender.solid(SpellRender.VINE));
		VertexConsumer thorns = buffers.getBuffer(SpellRender.solid(SpellRender.BARK));
		VertexConsumer leaves = buffers.getBuffer(SpellRender.solid(SpellRender.LEAF));
		float[] tint = vines ? new float[]{0.65f, 1.0f, 0.55f} : WHITE;
		for (int k = 0; k < count; k++) {
			double base = Math.PI * 2 * k / count + h(seed + k) * 0.4;
			double turn = (vines ? 1.6 : 0.9) * (k % 2 == 0 ? 1 : -1);
			List<Vec3> path = new ArrayList<>();
			int steps = 16;
			float[] radii = new float[steps + 1];
			for (int i = 0; i <= steps; i++) {
				float t = i / (float) steps;
				if (t > reach) break;
				double a = base + turn * t + Mth.sin(age * 0.12f + k) * 0.06;
				// Out from the ground, up round the prisoner, and (a cage's) over toward the crown above its head.
				double ring = vines ? r * (1.05 - 0.25 * t) : r * (0.65 + 0.55 * Mth.sin((float) Math.PI * Math.min(1, t * 1.05f))) * (t > 0.82f ? 1 - (t - 0.82f) * 3.2f : 1);
				double y = vines ? hgt * t : hgt * (t < 0.82 ? t / 0.82 : 1 + (t - 0.82) * 0.4);
				double writhe = Mth.sin(age * 0.2f + k * 1.7f + t * 6) * 0.05;
				path.add(new Vec3(Math.cos(a) * (ring + writhe), y, Math.sin(a) * (ring + writhe)));
				radii[i] = (vines ? 0.07f : 0.09f) * (1 - t * 0.6f) + 0.015f;
			}
			if (path.size() < 2) continue;
			float[] rr = java.util.Arrays.copyOf(radii, path.size());
			SpellRender.tube(stem, pose.last(), path, rr, 6, tint, 1, 1.6f, k * 0.3f, light, null);
			// Thorns: little cones along it, pointing out and back.
			for (int i = 1; i < path.size() - 1; i++) {
				for (int side = 0; side < (vines ? 1 : 2); side++) {
					long s = seed * 97 + k * 31 + i * 3 + side;
					if (h(s) < (vines ? 0.55f : 0.25f)) continue;
					Vec3 p = path.get(i);
					Vec3 along = path.get(i + 1).subtract(path.get(i - 1)).normalize();
					Vec3 out = new Vec3(p.x, 0, p.z).normalize().scale(side == 0 ? 1 : -1).add(0, (h(s + 1) - 0.5) * 0.8, 0).normalize();
					Vec3 tip = p.add(out.scale(rr[i] + (vines ? 0.12f : 0.22f))).subtract(along.scale(0.06));
					thorn(thorns, pose.last(), p.add(out.scale(rr[i] * 0.6)), tip, rr[i] * 0.55f, light);
				}
				if (vines && i % 3 == 1) {
					Vec3 p = path.get(i);
					leaf(leaves, pose, p, (float) (base + i), 0.28f, light);
				}
			}
		}
	}

	/** One thorn: a little four-sided cone from {@code root} to {@code tip}. */
	private static void thorn(VertexConsumer vc, PoseStack.Pose pose, Vec3 root, Vec3 tip, float radius, int light) {
		Vec3 dir = tip.subtract(root).normalize();
		Vec3 side = dir.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(dir).normalize();
		Vec3[] base = {root.add(side.scale(radius)), root.add(up.scale(radius)), root.add(side.scale(-radius)), root.add(up.scale(-radius))};
		float[] dark = {0.55f, 0.45f, 0.35f};
		for (int i = 0; i < 4; i++) {
			Vec3 a = base[i], b = base[(i + 1) % 4];
			SpellRender.quad(vc, pose, SpellRender.v(a), SpellRender.v(tip), SpellRender.v(tip), SpellRender.v(b), dark, 1, 0, 0, 0.3f, 0.3f, light);
		}
	}

	/** A leaf hanging at {@code at}, turned {@code angle}. */
	private static void leaf(VertexConsumer vc, PoseStack pose, Vec3 at, float angle, float size, int light) {
		pose.pushPose();
		pose.translate(at.x, at.y, at.z);
		pose.mulPose(Axis.YP.rotation(angle));
		pose.mulPose(Axis.XP.rotationDegrees(-30));
		PoseStack.Pose p = pose.last();
		SpellRender.quad(vc, p, SpellRender.v(-size / 2, 0, 0), SpellRender.v(-size / 2, size, 0), SpellRender.v(size / 2, size, 0), SpellRender.v(size / 2, 0, 0),
				WHITE, 1, 0, 1, 1, 0, light);
		pose.popPose();
	}

	/**
	 * Life Force Drain and Gain: a stream of emerald motes between the being (or herb) and the caster, arching a little, flowing toward
	 * the one that receives; a faint green thread under it.
	 */
	private void lifeStream(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 at, float partial, float age, float life) {
		Entity caster = field.owner();
		if (caster == null) return;
		Vec3 hands = caster.getPosition(partial).add(0, caster.getBbHeight() * 0.65, 0).subtract(at);
		boolean drain = field.data() == 1;
		Vec3 from = drain ? Vec3.ZERO : hands, to = drain ? hands : Vec3.ZERO;
		float show = SpellRender.fade(age, life, 6, 10);
		Vec3 mid = from.add(to).scale(0.5).add(0, from.distanceTo(to) * 0.18 + 0.5, 0);
		List<Vec3> arc = new ArrayList<>();
		float[] widths = new float[16], alphas = new float[16];
		for (int i = 0; i < 16; i++) {
			double t = i / 15.0;
			arc.add(bezier(from, mid, to, t));
			widths[i] = 0.18f;
			alphas[i] = 0.45f * show;
		}
		Vec3 cam = entityRenderDispatcher.camera.getPosition().subtract(at);
		SpellRender.ribbon(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), arc, widths, cam, LIFE, alphas, 0, 0, SpellRender.FULL_BRIGHT);
		VertexConsumer motes = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		VertexConsumer leaves = buffers.getBuffer(SpellRender.glow(SpellRender.LEAF));
		for (int i = 0; i < 26; i++) {
			long s = field.getId() * 31L + i;
			float t = (age * 0.03f + h(s)) % 1;
			Vec3 p = bezier(from, mid, to, t).add((h(s + 1) - 0.5) * 0.4 * Mth.sin((float) Math.PI * t), (h(s + 2) - 0.5) * 0.4 * Mth.sin((float) Math.PI * t), 0);
			SpellRender.billboard(i % 5 == 0 ? leaves : motes, pose, facing, (float) p.x, (float) p.y, (float) p.z, i % 5 == 0 ? 0.25f : 0.22f, age * 0.2f + i,
					LIFE, show, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
		glowBall(buffers, pose, facing, (float) from.x, (float) from.y, (float) from.z, 1.4f, LIFE, 0.35f * show);
	}

	private static Vec3 bezier(Vec3 a, Vec3 c, Vec3 b, double t) {
		double u = 1 - t;
		return a.scale(u * u).add(c.scale(2 * u * t)).add(b.scale(t * t));
	}

	/** Thorn Spear Forest: spears of thorny wood burst out of the ground all over the field, quiver, then sink back. */
	private void spears(SpellField field, PoseStack pose, MultiBufferSource buffers, Vec3 at, float age, float life, int light) {
		float r = field.radius();
		VertexConsumer wood = buffers.getBuffer(SpellRender.solid(SpellRender.VINE));
		VertexConsumer dust = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
		Quaternionf facing = entityRenderDispatcher.cameraOrientation();
		for (int i = 0; i < 46; i++) {
			long s = field.data() * 71L + i;
			double a = h(s) * Math.PI * 2, d = Math.sqrt(h(s + 1)) * r;
			float x = (float) (Math.cos(a) * d), z = (float) (Math.sin(a) * d);
			float delay = h(s + 2) * 4;
			float rise = SpellRender.easeOut((age - delay) / 4) * Mth.clamp((life - age) / 10, 0, 1);
			if (rise <= 0) continue;
			float tall = (2.0f + h(s + 3) * 2.8f) * rise;
			float lean = (h(s + 4) - 0.5f) * 0.5f;
			float quiver = Mth.sin(age * 1.4f + i) * 0.02f * (1 - Math.min(1, (age - 6) / 10));
			float y = groundAt(at, x, z, 0);
			List<Vec3> spine = List.of(new Vec3(x, y - 0.3, z), new Vec3(x + (lean + quiver) * tall * 0.5, y + tall * 0.5, z + lean * tall * 0.3),
					new Vec3(x + (lean + quiver) * tall, y + tall, z + lean * tall * 0.6));
			SpellRender.tube(wood, pose.last(), spine, new float[]{0.16f, 0.1f, 0.0f}, 5, WHITE, 1, 1, i, light, null);
			if (age - delay < 8) SpellRender.billboard(dust, pose, facing, x, y + 0.3f, z, 1.2f, i, WHITE, 0.6f * (1 - (age - delay) / 8), 0, 0, 1, 1, light);
		}
	}

	/** Lotus of Rebirth: a great lotus at the caster's feet, its pad spread wide, two rings of petals opening, a glowing heart, light rising. */
	private void lotus(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, float age, float life, int light) {
		float r = field.radius();
		float open = SpellRender.easeOut(Math.min(1, age / 20)) * Mth.clamp((life - age) / 15, 0, 1);
		VertexConsumer pad = buffers.getBuffer(SpellRender.clear(SpellRender.LOTUS_LEAF));
		SpellRender.disc(pad, pose.last(), r * 0.45f * Math.max(0.2f, open), 0.05f, age * 0.002f, WHITE, 0.9f * open, SpellRender.FULL_BRIGHT);
		VertexConsumer petals = buffers.getBuffer(SpellRender.solid(SpellRender.PETAL));
		for (int ringI = 0; ringI < 2; ringI++) {
			int n = ringI == 0 ? 12 : 8;
			float len = (ringI == 0 ? 1.6f : 1.1f) * Math.max(0.1f, open);
			float tilt = 85 - open * (ringI == 0 ? 50 : 30);
			for (int i = 0; i < n; i++) {
				pose.pushPose();
				pose.translate(0, 0.12f, 0);
				pose.mulPose(Axis.YP.rotation((float) (Math.PI * 2 * i / n + ringI * 0.3 + age * 0.003)));
				pose.mulPose(Axis.XP.rotationDegrees(tilt));
				PoseStack.Pose p = pose.last();
				float w = len * 0.42f;
				SpellRender.quad(petals, p, SpellRender.v(-w / 2, 0, 0.2f), SpellRender.v(-w / 2, len, 0.2f), SpellRender.v(w / 2, len, 0.2f), SpellRender.v(w / 2, 0, 0.2f),
						WHITE, 1, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				pose.popPose();
			}
		}
		glowBall(buffers, pose, facing, 0, 0.5f, 0, 2.2f * open, new float[]{1.0f, 0.9f, 0.6f}, 0.8f);
		glowBall(buffers, pose, facing, 0, 0.4f, 0, r * 1.2f * open, LIFE, 0.2f);
		VertexConsumer motes = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 40; i++) {
			long s = field.getId() * 43L + i;
			double a = h(s) * Math.PI * 2 + age * 0.02, d = Math.sqrt(h(s + 1)) * r;
			float y = (age * 0.06f + h(s + 2) * 5) % 5;
			SpellRender.billboard(motes, pose, facing, (float) (Math.cos(a) * d), y, (float) (Math.sin(a) * d), 0.25f, 0, i % 3 == 0 ? GOLD : LIFE, open * (1 - y / 5), 0, 0, 1, 1,
					SpellRender.FULL_BRIGHT);
		}
	}

	/** Heaven-Piercing Bamboo: a giant shoot bursting up out of the ground, ringed with nodes, leaves at its crown; it sinks back after. */
	private void bamboo(SpellField field, PoseStack pose, MultiBufferSource buffers, float age, float life, int light) {
		float r = field.radius(), hgt = field.height();
		float burst = SpellRender.easeOut(Math.min(1, age / 4)) * (1 + 0.08f * Mth.sin(Math.min(age, 12) * 1.2f) * Math.max(0, 1 - age / 12));
		float sink = Mth.clamp((life - age) / 12, 0, 1);
		float tall = hgt * burst * sink;
		if (tall < 0.1f) return;
		VertexConsumer stalk = buffers.getBuffer(SpellRender.solid(SpellRender.BAMBOO));
		List<Vec3> spine = List.of(new Vec3(0, -1, 0), new Vec3(0.05, tall * 0.5, 0.05), new Vec3(0.12, tall, 0.1));
		SpellRender.tube(stalk, pose.last(), spine, new float[]{r, r * 0.92f, r * 0.75f}, 10, WHITE, 1, 1 / (r * 4f), 0, light, null);
		// Nodes: rings a little proud of the stalk.
		VertexConsumer node = buffers.getBuffer(SpellRender.solid(SpellRender.BAMBOO));
		for (float y = 2.5f; y < tall - 0.5f; y += 2.8f) {
			float rr = r * (1 - y / tall * 0.25f) + 0.05f;
			List<Vec3> ring = List.of(new Vec3(y / tall * 0.08, y - 0.1, y / tall * 0.07), new Vec3(y / tall * 0.08, y + 0.1, y / tall * 0.07));
			SpellRender.tube(node, pose.last(), ring, new float[]{rr, rr}, 10, new float[]{0.75f, 0.8f, 0.55f}, 1, 0.1f, 0.2f, light, null);
		}
		VertexConsumer leaves = buffers.getBuffer(SpellRender.solid(SpellRender.LEAF));
		for (int i = 0; i < 10; i++) {
			long s = field.data() * 13L + i;
			leaf(leaves, pose, new Vec3((h(s) - 0.5) * 1.2, tall - h(s + 1) * 2.5, (h(s + 2) - 0.5) * 1.2), (float) (h(s + 3) * 6.3), 0.9f, light);
		}
	}

	/**
	 * The World Tree: it grows out of the ground over its first two seconds: a vast trunk of bark, great roots spreading at its foot,
	 * boughs reaching out and up, a crown of leaves over all; motes of life drifting under it. It withers back at the end.
	 */
	private void worldTree(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life, int light) {
		float grow = SpellRender.easeOut(Math.min(1, age / 40));
		float wither = Mth.clamp((life - age) / 30, 0, 1);
		float g = grow * wither;
		if (g <= 0.01f) return;
		float tall = field.height() * g;
		long seed = field.data();
		VertexConsumer bark = buffers.getBuffer(SpellRender.solid(SpellRender.BARK));
		// The trunk, swelling at its foot.
		List<Vec3> trunk = new ArrayList<>();
		float[] tr = new float[9];
		for (int i = 0; i < 9; i++) {
			float t = i / 8f;
			trunk.add(new Vec3(Mth.sin(t * 3 + seed % 5) * 0.8f * g, -2 + (tall + 2) * t, Mth.cos(t * 2.4f + seed % 3) * 0.6f * g));
			tr[i] = (3.2f - 2.0f * t + (t < 0.12f ? (0.12f - t) * 14 : 0)) * g;
		}
		SpellRender.tube(bark, pose.last(), trunk, tr, 14, WHITE, 1, 0.15f, 0, light, null);
		// Roots spreading over the ground.
		for (int k = 0; k < 8; k++) {
			double a = Math.PI * 2 * k / 8 + h(seed + k) * 0.5;
			List<Vec3> root = new ArrayList<>();
			float[] rr = new float[6];
			for (int i = 0; i < 6; i++) {
				float t = i / 5f;
				double d = (2 + t * 9 * g);
				root.add(new Vec3(Math.cos(a + t * 0.3) * d, 0.8 - t * 1.4 + Mth.sin(t * 5) * 0.3, Math.sin(a + t * 0.3) * d));
				rr[i] = (1.2f - 1.0f * t) * g;
			}
			SpellRender.tube(bark, pose.last(), root, rr, 8, WHITE, 1, 0.2f, k, light, null);
		}
		// Boughs.
		List<Vec3> crowns = new ArrayList<>();
		for (int k = 0; k < 9; k++) {
			double a = Math.PI * 2 * k / 9 + h(seed + k * 3) * 0.6;
			float from = 0.45f + 0.4f * h(seed + k * 5);
			Vec3 start = trunk.get(Math.min(8, (int) (from * 8)));
			List<Vec3> bough = new ArrayList<>();
			float[] br = new float[6];
			for (int i = 0; i < 6; i++) {
				float t = i / 5f;
				double d = t * (8 + 6 * h(seed + k * 7)) * g;
				bough.add(start.add(Math.cos(a) * d, t * (4 + 4 * h(seed + k * 11)) * g, Math.sin(a) * d));
				br[i] = (0.9f - 0.75f * t) * g;
			}
			SpellRender.tube(bark, pose.last(), bough, br, 8, WHITE, 1, 0.3f, k, light, null);
			crowns.add(bough.get(5));
		}
		crowns.add(trunk.get(8));
		// The crown: clusters of leaves at the boughs' ends and over the top.
		VertexConsumer leaves = buffers.getBuffer(SpellRender.solid(SpellRender.LEAF));
		for (int c = 0; c < crowns.size(); c++) {
			Vec3 centre = crowns.get(c);
			for (int i = 0; i < 26; i++) {
				long s = seed * 101 + c * 37 + i;
				double a = h(s) * Math.PI * 2, b = h(s + 1) * Math.PI;
				double d = (2.5 + h(s + 2) * 3.5) * g;
				Vec3 at = centre.add(Math.cos(a) * Math.sin(b) * d, Math.cos(b) * d * 0.6 + 1, Math.sin(a) * Math.sin(b) * d);
				leaf(leaves, pose, at, (float) (h(s + 3) * 6.3), (1.2f + h(s + 4)) * g, light);
			}
		}
		VertexConsumer motes = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 60; i++) {
			long s = seed * 7 + i;
			double a = h(s) * Math.PI * 2 + age * 0.01, d = Math.sqrt(h(s + 1)) * field.radius();
			float y = (age * 0.05f + h(s + 2) * 20) % 20;
			SpellRender.billboard(motes, pose, facing, (float) (Math.cos(a) * d), y, (float) (Math.sin(a) * d), 0.35f, 0, LIFE, g * (1 - y / 20), 0, 0, 1, 1,
					SpellRender.FULL_BRIGHT);
		}
	}

	/** One of the World Tree's vines: down from a bough to the being it holds, sagging a little, writhing, leaves along it. */
	private void treeVine(SpellField field, PoseStack pose, MultiBufferSource buffers, Vec3 at, float partial, float age, float life, int light) {
		Entity target = field.target();
		if (target == null) return;
		double a = field.radius();
		Vec3 bough = new Vec3(Math.cos(a) * 7, 24, Math.sin(a) * 7);
		Vec3 to = target.getPosition(partial).add(0, target.getBbHeight() * 0.6, 0).subtract(at);
		float reach = SpellRender.easeOut(Math.min(1, age / 10)) * Mth.clamp((life - age) / 8, 0, 1);
		List<Vec3> vine = new ArrayList<>();
		float[] radii = new float[14];
		for (int i = 0; i < 14; i++) {
			double t = i / 13.0 * reach;
			Vec3 p = bough.lerp(to, t).add(0, -Math.sin(Math.PI * t) * 2.5, 0);
			p = p.add(Math.sin(age * 0.2 + i) * 0.2, 0, Math.cos(age * 0.17 + i) * 0.2);
			vine.add(p);
			radii[i] = 0.22f - 0.1f * (float) (i / 13.0);
		}
		SpellRender.tube(buffers.getBuffer(SpellRender.solid(SpellRender.VINE)), pose.last(), vine, radii, 6, new float[]{0.7f, 1.0f, 0.6f}, 1, 0.8f, 0, light, null);
		VertexConsumer leaves = buffers.getBuffer(SpellRender.solid(SpellRender.LEAF));
		for (int i = 2; i < 14; i += 3) leaf(leaves, pose, vine.get(i), i * 1.7f, 0.5f, light);
		// Coils round the held.
		if (reach >= 1) {
			List<Vec3> coil = new ArrayList<>();
			float[] cr = new float[20];
			float w = target.getBbWidth() * 0.7f + 0.2f;
			for (int i = 0; i < 20; i++) {
				double t = i / 19.0, ang = t * Math.PI * 5 + age * 0.05;
				coil.add(to.add(Math.cos(ang) * w, (t - 0.6) * target.getBbHeight(), Math.sin(ang) * w));
				cr[i] = 0.08f;
			}
			SpellRender.tube(buffers.getBuffer(SpellRender.solid(SpellRender.VINE)), pose.last(), coil, cr, 5, new float[]{0.7f, 1.0f, 0.6f}, 1, 0.8f, 0, light, null);
		}
	}

	// --- Earth ---

	/** Earthquake: cracks glowing with the earth's fire spread over the ground, stones jumping, dust rolling out in rings. */
	private void quake(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 at, float age, float life, int light) {
		float r = field.radius();
		float show = SpellRender.fade(age, life, 4, 12);
		VertexConsumer cracks = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		for (int k = 0; k < 12; k++) {
			double a = Math.PI * 2 * k / 12 + h(field.getId() + k);
			List<Vec3> line = new ArrayList<>();
			float[] widths = new float[8], alphas = new float[8];
			for (int i = 0; i < 8; i++) {
				double d = r * Math.min(1, age / 10) * i / 7;
				double jag = (h(field.getId() * 3L + k * 11 + i) - 0.5) * 1.6;
				double x = Math.cos(a) * d - Math.sin(a) * jag, z = Math.sin(a) * d + Math.cos(a) * jag;
				line.add(new Vec3(x, groundAt(at, x, z, 0) + 0.06, z));
				widths[i] = 0.35f * (1 - i / 8f) + 0.08f;
				alphas[i] = show * (0.7f + 0.3f * Mth.sin(age * 0.8f + i));
			}
			SpellRender.ribbon(cracks, pose.last(), line, widths, new Vec3(0, 100, 0), EARTH, alphas, 0, 0, SpellRender.FULL_BRIGHT);
		}
		TextureAtlasSprite sprite = SpellRender.sprite(field.data() == 0 ? Block.getId(Blocks.STONE.defaultBlockState()) : field.data());
		VertexConsumer stones = buffers.getBuffer(SpellRender.blocks());
		for (int i = 0; i < 30; i++) {
			long s = field.getId() * 59L + i;
			double a = h(s) * Math.PI * 2, d = Math.sqrt(h(s + 1)) * r;
			float x = (float) (Math.cos(a) * d), z = (float) (Math.sin(a) * d);
			float hop = Math.abs(Mth.sin(age * 0.35f + i)) * 1.2f * show;
			float c = 0.12f + h(s + 2) * 0.2f;
			float y = groundAt(at, x, z, 0) + hop;
			pose.pushPose();
			pose.translate(x, y, z);
			pose.mulPose(Axis.YP.rotation(age * 0.2f + i));
			pose.mulPose(Axis.XP.rotation(age * 0.3f + i));
			SpellRender.box(stones, pose.last(), -c, -c, -c, c, c, c, WHITE, 1, sprite.getU0(), sprite.getV0(), sprite.getU(6), sprite.getV(6), light);
			pose.popPose();
		}
		VertexConsumer dust = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
		for (int i = 0; i < 24; i++) {
			double a = Math.PI * 2 * i / 24;
			float d = (age * 0.6f) % r;
			SpellRender.billboard(dust, pose, facing, (float) Math.cos(a) * d, 0.6f, (float) Math.sin(a) * d, 2.2f, i, WHITE, 0.5f * show * (1 - d / r), 0, 0, 1, 1, light);
		}
	}

	/** Heavy Earth Seal: a vast seal of earth-light on the ground about its caster, turning slowly, pulsing, with lines of weight falling into it. */
	private void seal(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		float r = field.radius();
		float show = SpellRender.fade(age, life, 8, 12);
		float pulse = 0.75f + 0.25f * Mth.sin(age * 0.4f);
		VertexConsumer rune = buffers.getBuffer(SpellRender.glow(SpellRender.RUNE));
		SpellRender.disc(rune, pose.last(), r, 0.1f, age * 0.01f, EARTH, show * pulse, SpellRender.FULL_BRIGHT);
		SpellRender.disc(rune, pose.last(), r * 0.55f, 0.12f, -age * 0.02f, GOLD, show * 0.8f, SpellRender.FULL_BRIGHT);
		VertexConsumer streaks = buffers.getBuffer(SpellRender.glow(SpellRender.RAIN));
		Vec3 cam = entityRenderDispatcher.camera.getPosition().subtract(field.getPosition(0));
		for (int i = 0; i < 70; i++) {
			long s = field.getId() * 13L + i;
			double a = h(s) * Math.PI * 2, d = Math.sqrt(h(s + 1)) * r;
			float x = (float) (Math.cos(a) * d), z = (float) (Math.sin(a) * d);
			float y = 8 - (age * 0.9f + h(s + 2) * 8) % 8;
			float yaw = (float) Math.atan2(cam.x - x, cam.z - z);
			pose.pushPose();
			pose.translate(x, y, z);
			pose.mulPose(Axis.YP.rotation(yaw));
			SpellRender.quad(streaks, pose.last(), SpellRender.v(-0.06f, 0, 0), SpellRender.v(-0.06f, -2, 0), SpellRender.v(0.06f, -2, 0), SpellRender.v(0.06f, 0, 0),
					EARTH, 0.6f * show, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			pose.popPose();
		}
	}

	/** Mountain Seal rising: a mound of the ground's own rock bursting up over its point before the stone stands. */
	private void mountain(SpellField field, PoseStack pose, MultiBufferSource buffers, float age, float life, int light) {
		float rise = SpellRender.easeOut(Math.min(1, age / 12));
		float r = field.radius(), hgt = field.height();
		TextureAtlasSprite sprite = SpellRender.sprite(field.data() == 0 ? Block.getId(Blocks.STONE.defaultBlockState()) : field.data());
		VertexConsumer rock = buffers.getBuffer(SpellRender.blocks());
		pose.translate(0, -hgt * (1 - rise), 0);
		int rings = 6, segments = 14;
		for (int i = 0; i < rings; i++) {
			for (int j = 0; j < segments; j++) {
				float t0 = i / (float) rings, t1 = (i + 1) / (float) rings;
				double a0 = Math.PI * 2 * j / segments, a1 = Math.PI * 2 * (j + 1) / segments;
				float r0 = r * (1 - t0 * t0) * (0.85f + 0.3f * h(field.getId() * 7L + i * 31 + j)), r1 = r * (1 - t1 * t1) * (0.85f + 0.3f * h(field.getId() * 7L + (i + 1) * 31 + j));
				float r0b = r * (1 - t0 * t0) * (0.85f + 0.3f * h(field.getId() * 7L + i * 31 + (j + 1) % segments));
				float r1b = r * (1 - t1 * t1) * (0.85f + 0.3f * h(field.getId() * 7L + (i + 1) * 31 + (j + 1) % segments));
				SpellRender.quad(rock, pose.last(), SpellRender.v((float) Math.cos(a0) * r0, hgt * t0, (float) Math.sin(a0) * r0),
						SpellRender.v((float) Math.cos(a0) * r1, hgt * t1, (float) Math.sin(a0) * r1), SpellRender.v((float) Math.cos(a1) * r1b, hgt * t1, (float) Math.sin(a1) * r1b),
						SpellRender.v((float) Math.cos(a1) * r0b, hgt * t0, (float) Math.sin(a1) * r0b), new float[]{0.85f, 0.85f, 0.85f}, 1,
						sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), light);
			}
		}
	}

	/**
	 * Earth Dragon Surge: a ridge of jagged spikes of the ground's own rock bursting up along the ground behind its front as it runs
	 * out, each leaning the way it goes, then sinking back a few seconds later.
	 */
	private void spikes(SpellField field, PoseStack pose, MultiBufferSource buffers, Vec3 at, float age, float life, int light) {
		float length = field.radius();
		float reach = field.value() * length;
		float pace = 1.5f;
		TextureAtlasSprite sprite = SpellRender.sprite(field.data() == 0 ? Block.getId(Blocks.STONE.defaultBlockState()) : field.data());
		VertexConsumer rock = buffers.getBuffer(SpellRender.blocks());
		Vec3 way = Vec3.directionFromRotation(0, field.yaw());
		Vec3 side = new Vec3(-way.z, 0, way.x);
		for (int i = 1; i <= (int) reach; i++) {
			float born = i / pace;
			float rise = SpellRender.easeOut((age - born) / 4);
			float sink = Mth.clamp((life - 10 - age) / 10, 0, 1);
			float s = rise * sink;
			if (s <= 0) continue;
			for (int k = 0; k < 2; k++) {
				long seed = field.getId() * 71L + i * 2 + k;
				double off = (h(seed) - 0.5) * 1.4;
				double dx = way.x * i + side.x * off, dz = way.z * i + side.z * off;
				float y = groundAt(at, dx, dz, 0);
				float tall = (1.2f + h(seed + 1) * 2.2f) * s;
				pose.pushPose();
				pose.translate(dx, y - 0.3, dz);
				SpellRender.spike(rock, pose.last(), 0.45f + h(seed + 2) * 0.3f, tall, (float) way.x * 0.25f, (float) way.z * 0.25f, 5, sprite, WHITE, 1, light, seed);
				pose.popPose();
			}
		}
	}

	// --- Metal ---

	/** Piercing Sword Light: a blinding line of sword light out to where it struck, its edges flickering, a sword's shape at its head. */
	private void swordLight(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		float length = field.height();
		float show = SpellRender.fade(age, life, 1, 10);
		pose.mulPose(Axis.YP.rotationDegrees(-field.yaw()));
		pose.mulPose(Axis.XP.rotationDegrees(field.pitch()));
		List<Vec3> line = new ArrayList<>();
		float[] radii = new float[6];
		for (int i = 0; i < 6; i++) {
			line.add(new Vec3(0, 0, length * i / 5.0));
			radii[i] = 0.18f * show * (1 + 0.15f * Mth.sin(age * 3 + i));
		}
		SpellRender.tube(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), line, radii, 8, WHITE, show, 0.2f, 0, SpellRender.FULL_BRIGHT, null);
		float[] wide = new float[6];
		for (int i = 0; i < 6; i++) wide[i] = radii[i] * 3.5f;
		SpellRender.tube(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), line, wide, 8, SWORD_LIGHT, 0.45f * show, 0.2f, 0, SpellRender.FULL_BRIGHT, null);
		pose.pushPose();
		pose.translate(0, 0, length - 1.5f);
		SpellModels.jian(pose, buffers, 2.2f, SpellRender.FULL_BRIGHT, 1, null);
		pose.popPose();
	}

	/** Sword Domain: swords wheeling round the caster in two rings turning against each other, darting in and out, a circle of light beneath. */
	private void domain(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		float r = field.radius();
		float show = SpellRender.fade(age, life, 10, 10);
		int count = Math.max(6, field.data());
		VertexConsumer rune = buffers.getBuffer(SpellRender.glow(SpellRender.RUNE));
		SpellRender.disc(rune, pose.last(), r, 0.1f, age * 0.02f, GOLD, 0.6f * show, SpellRender.FULL_BRIGHT);
		for (int i = 0; i < count; i++) {
			boolean upper = i % 2 == 0;
			double a = Math.PI * 2 * i / count + age * (upper ? 0.06 : -0.045);
			float dart = Math.max(0, Mth.sin(age * 0.3f + i * 1.3f)) * 0.25f;
			float d = r * (0.62f - dart) * show;
			pose.pushPose();
			pose.translate(Math.cos(a) * d, upper ? 2.4f : 1.0f, Math.sin(a) * d);
			pose.mulPose(Axis.YP.rotation((float) (-a + (upper ? Math.PI : 0))));
			pose.mulPose(Axis.XP.rotationDegrees(upper ? -10 : 10));
			SpellModels.jian(pose, buffers, 1.3f, SpellRender.FULL_BRIGHT, show, null);
			pose.popPose();
		}
	}

	/** Sword Rain forming: swords appearing one by one in a ring over the point, points down, gathering light. */
	private void swordRing(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		int count = Math.max(1, field.data());
		float r = field.radius() * 0.8f;
		for (int i = 0; i < count; i++) {
			float appear = Mth.clamp((age - i * life / count * 0.7f) / 4, 0, 1);
			if (appear <= 0) continue;
			double a = Math.PI * 2 * i / count + age * 0.03;
			pose.pushPose();
			pose.translate(Math.cos(a) * r, 0, Math.sin(a) * r);
			pose.mulPose(Axis.XP.rotationDegrees(90));
			SpellModels.jian(pose, buffers, 1.3f, SpellRender.FULL_BRIGHT, appear, null);
			pose.popPose();
			glowBall(buffers, pose, facing, (float) Math.cos(a) * r, 0, (float) Math.sin(a) * r, 1.2f, SWORD_LIGHT, 0.4f * appear);
		}
		VertexConsumer rune = buffers.getBuffer(SpellRender.glow(SpellRender.RUNE));
		SpellRender.disc(rune, pose.last(), r * 1.3f, 0.5f, age * 0.05f, GOLD, 0.6f, SpellRender.FULL_BRIGHT);
	}

	/**
	 * Four Swords Array: four swords as tall as towers driven into the ground at the corners of the square, a wall of golden light
	 * between each pair, runes running round them, a seal across the ground inside.
	 */
	private void array(SpellField field, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float life) {
		float half = field.radius(), tall = field.height();
		float show = SpellRender.fade(age, life, 1, 15);
		float drop = SpellRender.easeOut(Math.min(1, age / 10));
		for (int c = 0; c < 4; c++) {
			float x = (c == 0 || c == 3 ? -1 : 1) * half, z = (c < 2 ? -1 : 1) * half;
			pose.pushPose();
			pose.translate(x, 40 * (1 - drop) - 3, z);
			pose.mulPose(Axis.XP.rotationDegrees(-90));
			SpellModels.jian(pose, buffers, tall, SpellRender.FULL_BRIGHT, 1, new float[]{1.0f, 0.95f, 0.8f});
			pose.popPose();
		}
		if (drop < 1) return;
		VertexConsumer wall = buffers.getBuffer(SpellRender.glow(SpellRender.RUNE));
		float wallH = tall * 0.6f;
		float[][] corners = {{-half, -half}, {half, -half}, {half, half}, {-half, half}};
		for (int c = 0; c < 4; c++) {
			float[] a = corners[c], b = corners[(c + 1) % 4];
			float scroll = age * 0.01f;
			SpellRender.quad(wall, pose.last(), SpellRender.v(a[0], 0, a[1]), SpellRender.v(a[0], wallH, a[1]), SpellRender.v(b[0], wallH, b[1]), SpellRender.v(b[0], 0, b[1]),
					GOLD, 0.35f * show, scroll, 0, scroll + 3, 1, SpellRender.FULL_BRIGHT);
		}
		SpellRender.disc(wall, pose.last(), half * 1.3f, 0.1f, age * 0.01f, GOLD, 0.5f * show, SpellRender.FULL_BRIGHT);
	}
}
