package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpellAura;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The spell auras drawn round a being in the world (any being, not only those with a man's shape): the Golden Bell over its caster,
 * a being frozen in a block of ice, Karmic Fire burning on its victim with its seals turning, the press of Heavy Earth Seal, life
 * flowing out of or into a being, the phoenix's wings of Phoenix Nirvana, the steel vortex of Steel Hurricane, ripples under one
 * walking on water with Azure Breathing, leaves rising round one under Emerald Healing.
 */
public final class SpellAuraWorld {
	private static final float[] WHITE = {1, 1, 1};
	private static final float[] FIRE_MID = {1.0f, 0.55f, 0.15f}, KARMA = {1.0f, 0.5f, 0.3f}, ICE = {0.7f, 0.9f, 1.0f}, LIFE = {0.35f, 1.0f, 0.45f};
	private static final float[] GOLD = {1.0f, 0.85f, 0.45f}, EARTH = {0.85f, 0.6f, 0.3f};

	/** WorldRenderEvents.AFTER_ENTITIES. */
	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || ClientSpellAuras.all().isEmpty()) return;
		Vec3 cam = context.camera().getPosition();
		float partial = context.tickDelta();
		Quaternionf facing = context.camera().rotation();
		PoseStack pose = context.matrixStack();
		MultiBufferSource.BufferSource own = context.consumers() == null ? mc.renderBuffers().bufferSource() : null;
		MultiBufferSource buffers = own != null ? own : context.consumers();
		for (Map.Entry<Integer, EnumMap<SpellAura, long[]>> entry : List.copyOf(ClientSpellAuras.all().entrySet())) {
			Entity entity = mc.level.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity e) || e.isInvisible()) continue;
			Vec3 at = e.getPosition(partial);
			if (at.distanceToSqr(cam) > 128 * 128) continue;
			float age = e.tickCount + partial;
			pose.pushPose();
			pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
			Vec3 eye = cam.subtract(at);
			float bell = ClientSpellAuras.shown(e, SpellAura.GOLDEN_BELL, partial, 1, 10);
			if (bell > 0) bell(pose, buffers, facing, e, partial, bell);
			float frozen = ClientSpellAuras.shown(e, SpellAura.FROZEN, partial, 3, 10);
			if (frozen > 0) frozen(pose, buffers, facing, e, frozen);
			float karma = ClientSpellAuras.shown(e, SpellAura.KARMIC_FIRE, partial, 5, 15);
			if (karma > 0) karmic(pose, buffers, facing, eye, e, age, karma);
			float pressed = ClientSpellAuras.shown(e, SpellAura.PRESSED, partial, 4, 8);
			if (pressed > 0) pressed(pose, buffers, e, age, pressed);
			float life = ClientSpellAuras.shown(e, SpellAura.LIFE_FLOW, partial, 5, 10);
			if (life > 0) lifeGlow(pose, buffers, facing, e, age, life);
			float nirvana = ClientSpellAuras.shown(e, SpellAura.PHOENIX_NIRVANA, partial, 15, 30);
			if (nirvana > 0) wings(pose, buffers, e, partial, age, nirvana);
			float hurricane = ClientSpellAuras.shown(e, SpellAura.STEEL_HURRICANE, partial, 2, 6);
			if (hurricane > 0) vortex(pose, buffers, facing, e, age, hurricane);
			float breathing = ClientSpellAuras.shown(e, SpellAura.AZURE_BREATHING, partial, 5, 20);
			if (breathing > 0) breathing(pose, buffers, facing, e, age, breathing);
			float emerald = ClientSpellAuras.shown(e, SpellAura.EMERALD_HEALING, partial, 8, 20);
			if (emerald > 0) leavesRising(pose, buffers, facing, e, age, emerald);
			pose.popPose();
		}
		if (own != null) own.endBatch();
	}

	/**
	 * The Golden Bell: a great bell of translucent gold over its caster, its waist engraved, its lip flared; it comes down over them
	 * as it is raised, turns slowly, and shines brighter as it takes a blow.
	 */
	private static void bell(PoseStack pose, MultiBufferSource buffers, Quaternionf facing, LivingEntity e, float partial, float shown) {
		float age = ClientSpellAuras.age(e, SpellAura.GOLDEN_BELL, partial);
		float drop = SpellRender.easeOut(Math.min(1, age / 8));
		float h = e.getBbHeight() + 0.7f, r = Math.max(e.getBbWidth(), 0.6f) * 1.1f + 0.3f;
		pose.pushPose();
		pose.translate(0, (1 - drop) * 3 - 0.15f, 0);
		pose.mulPose(Axis.YP.rotation(age * 0.02f));
		// A bell's outline by height: the flared lip, the waist, the shoulder rounding to the crown.
		SpellRender.Radius profile = t -> r * (t < 0.15f ? 1.15f - t : t < 0.75f ? 1.0f - (t - 0.15f) * 0.25f : (0.85f - (t - 0.75f) * 2.4f));
		SpellRender.cylinder(buffers.getBuffer(SpellRender.clear(SpellRender.GOLD)), pose.last(), h, profile, 8, 20, new float[]{1.0f, 0.9f, 0.6f}, 0.42f * shown,
				4, 1, 0, 0, 0, SpellRender.FULL_BRIGHT, null);
		SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.RUNE)), pose.last(), h, t -> profile.at(t) * 1.01f, 8, 20, GOLD, 0.35f * shown,
				3, 0.4f, age * 0.01f, 0.3f, 0, SpellRender.FULL_BRIGHT, t -> t > 0.3f && t < 0.6f ? 1 : 0);
		// The crown's knob.
		SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.GOLD)), pose.last(), 0.18f, 6, 8, WHITE, 0.6f * shown, 1, 1, 0, 0, SpellRender.FULL_BRIGHT, null);
		pose.popPose();
	}

	/** Frozen: the being in a block of ice, rough and clouded, shards jutting from it, a cold light in it. */
	private static void frozen(PoseStack pose, MultiBufferSource buffers, Quaternionf facing, LivingEntity e, float shown) {
		float w = e.getBbWidth() / 2 + 0.2f, h = e.getBbHeight() + 0.25f;
		VertexConsumer ice = buffers.getBuffer(SpellRender.clear(SpellRender.ICE));
		SpellRender.box(ice, pose.last(), -w, -0.05f, -w, w, h * shown, w, WHITE, 0.6f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		for (int i = 0; i < 6; i++) {
			long s = e.getId() * 17L + i;
			double a = SpellRender.hash(s) * Math.PI * 2;
			float y = SpellRender.hash(s + 1) * h;
			pose.pushPose();
			pose.translate(Math.cos(a) * w, y, Math.sin(a) * w);
			pose.mulPose(Axis.YP.rotation((float) -a));
			pose.mulPose(Axis.ZP.rotationDegrees(-70 + SpellRender.hash(s + 2) * 40));
			List<Vec3> shard = List.of(new Vec3(0, 0, 0), new Vec3(0, 0.25, 0), new Vec3(0, 0.5 * shown, 0));
			SpellRender.tube(ice, pose.last(), shard, new float[]{0.12f, 0.08f, 0}, 4, WHITE, 0.75f, 1, 0, SpellRender.FULL_BRIGHT, null);
			pose.popPose();
		}
		SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose, facing, 0, h / 2, 0, h * 1.6f, 0, ICE, 0.25f * shown, 0, 0, 1, 1,
				SpellRender.FULL_BRIGHT);
	}

	/** Karmic Fire: crimson-gold fire burning up the being, seals of karma turning round it, ash rising. */
	private static void karmic(PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 eye, LivingEntity e, float age, float shown) {
		float w = e.getBbWidth() / 2 + 0.15f, h = e.getBbHeight();
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.KARMA));
		for (int i = 0; i < 9; i++) {
			double a = Math.PI * 2 * i / 9 + age * 0.03;
			float tall = h * (0.7f + 0.4f * Mth.sin(age * 0.3f + i)) * shown;
			SpellRender.flameTongue(flames, pose, eye, (float) Math.cos(a) * w, 0, (float) Math.sin(a) * w, w * 1.4f, tall, age + i * 4, WHITE, 0.95f * shown,
					SpellRender.FLAME_FRAMES);
		}
		VertexConsumer rune = buffers.getBuffer(SpellRender.glow(SpellRender.RUNE));
		for (int k = 0; k < 3; k++) {
			pose.pushPose();
			pose.translate(0, h * (0.2f + 0.3f * k), 0);
			SpellRender.disc(rune, pose.last(), w + 0.5f + k * 0.15f, 0, age * (0.05f + k * 0.02f) * (k % 2 == 0 ? 1 : -1), KARMA, 0.55f * shown, SpellRender.FULL_BRIGHT);
			pose.popPose();
		}
		VertexConsumer ash = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 10; i++) {
			long s = e.getId() * 31L + i;
			float y = (age * 0.06f + SpellRender.hash(s) * h * 1.5f) % (h * 1.5f);
			double a = SpellRender.hash(s + 1) * Math.PI * 2;
			SpellRender.billboard(ash, pose, facing, (float) Math.cos(a) * w * 1.2f, y, (float) Math.sin(a) * w * 1.2f, 0.15f, 0, KARMA, shown * (1 - y / (h * 1.5f)),
					0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** Heavy Earth Seal on a foe: a seal of earth-light under its feet and the weight bearing down on it. */
	private static void pressed(PoseStack pose, MultiBufferSource buffers, LivingEntity e, float age, float shown) {
		float w = e.getBbWidth() + 0.6f;
		VertexConsumer rune = buffers.getBuffer(SpellRender.glow(SpellRender.RUNE));
		SpellRender.disc(rune, pose.last(), w, 0.05f, age * 0.04f, EARTH, 0.85f * shown, SpellRender.FULL_BRIGHT);
		pose.pushPose();
		pose.translate(0, e.getBbHeight() + 0.4f, 0);
		SpellRender.disc(rune, pose.last(), w * 0.8f, 0, -age * 0.06f, EARTH, 0.6f * shown, SpellRender.FULL_BRIGHT);
		pose.popPose();
	}

	/** Life Force Drain or Gain on a being: a green glow and motes swirling about it. */
	private static void lifeGlow(PoseStack pose, MultiBufferSource buffers, Quaternionf facing, LivingEntity e, float age, float shown) {
		float h = e.getBbHeight(), w = e.getBbWidth() / 2 + 0.3f;
		SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose, facing, 0, h / 2, 0, h * 1.8f, 0, LIFE, 0.3f * shown, 0, 0, 1, 1,
				SpellRender.FULL_BRIGHT);
		VertexConsumer motes = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 12; i++) {
			double a = age * 0.15 + i * Math.PI * 2 / 12;
			float y = (float) ((Math.sin(age * 0.1 + i) * 0.5 + 0.5) * h);
			SpellRender.billboard(motes, pose, facing, (float) Math.cos(a) * w, y, (float) Math.sin(a) * w, 0.2f, 0, LIFE, shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** Phoenix Nirvana: faint wings of fire folded at the caster's back, stirring, waiting to rise. */
	private static void wings(PoseStack pose, MultiBufferSource buffers, LivingEntity e, float partial, float age, float shown) {
		float yaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
		VertexConsumer feathers = buffers.getBuffer(SpellRender.glow(SpellRender.FEATHER));
		pose.pushPose();
		pose.translate(0, e.getBbHeight() * 0.72f, 0);
		pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
		pose.translate(0, 0, 0.2f);
		float stir = Mth.sin(age * 0.08f) * 6;
		for (int side = -1; side <= 1; side += 2) {
			for (int f = 0; f < 6; f++) {
				pose.pushPose();
				pose.translate(side * 0.15f, 0, 0);
				pose.mulPose(Axis.ZP.rotationDegrees(side * (20 + f * 14 + stir)));
				PoseStack.Pose p = pose.last();
				float len = 0.7f + f * 0.1f, w = 0.22f;
				SpellRender.quad(feathers, p, SpellRender.v(-w / 2, 0, 0), SpellRender.v(w / 2, 0, 0), SpellRender.v(w / 2, len, 0), SpellRender.v(-w / 2, len, 0),
						WHITE, 0.55f * shown, 0, 1, 1, 0, SpellRender.FULL_BRIGHT);
				pose.popPose();
			}
		}
		pose.popPose();
	}

	/** Steel Hurricane: a whirl of steel wind round the dashing body, along the way it goes, sparks thrown off it. */
	private static void vortex(PoseStack pose, MultiBufferSource buffers, Quaternionf facing, LivingEntity e, float age, float shown) {
		Vec3 motion = e.getDeltaMovement();
		if (motion.lengthSqr() < 1.0e-4) motion = e.getLookAngle();
		Vec3 dir = motion.normalize();
		float yaw = (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
		float pitch = (float) Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
		pose.pushPose();
		pose.translate(0, e.getBbHeight() / 2, 0);
		pose.mulPose(Axis.YP.rotationDegrees(yaw));
		pose.mulPose(Axis.XP.rotationDegrees(90 - pitch));
		pose.translate(0, -1.2f, 0);
		VertexConsumer wind = buffers.getBuffer(SpellRender.glow(SpellRender.WIND));
		for (int layer = 0; layer < 3; layer++) {
			int l = layer;
			pose.pushPose();
			pose.mulPose(Axis.YP.rotation(age * (0.9f + layer * 0.3f)));
			SpellRender.cylinder(wind, pose.last(), 2.6f, t -> 0.5f + (1 - t) * 0.5f + l * 0.12f, 4, 12, new float[]{0.85f, 0.9f, 1.0f}, 0.8f * shown, 2, 1, 0,
					0, 0.8f, SpellRender.FULL_BRIGHT, t -> Mth.sin((float) Math.PI * t));
			pose.popPose();
		}
		pose.popPose();
		VertexConsumer spark = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 8; i++) {
			long s = e.getId() * 13L + i + (long) (age / 2);
			SpellRender.billboard(spark, pose, facing, (SpellRender.hash(s) - 0.5f) * 1.6f, SpellRender.hash(s + 1) * e.getBbHeight(), (SpellRender.hash(s + 2) - 0.5f) * 1.6f,
					0.25f, 0, WHITE, shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** Azure Breathing: ripples spreading from the feet of one walking on water; bubbles of breath about the head of one under it. */
	private static void breathing(PoseStack pose, MultiBufferSource buffers, Quaternionf facing, LivingEntity e, float age, float shown) {
		Minecraft mc = Minecraft.getInstance();
		boolean onWater = mc.level.getFluidState(e.blockPosition().below()).is(net.minecraft.tags.FluidTags.WATER) && !e.isInWater();
		if (onWater) {
			VertexConsumer ripple = buffers.getBuffer(SpellRender.clear(SpellRender.RIPPLE));
			for (int k = 0; k < 2; k++) {
				float t = ((age + k * 10) % 20) / 20f;
				SpellRender.disc(ripple, pose.last(), 0.4f + t * 1.6f, 0.02f, 0, WHITE, 0.7f * (1 - t) * shown, SpellRender.FULL_BRIGHT);
			}
		}
		if (e.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
			VertexConsumer bubbles = buffers.getBuffer(SpellRender.clear(SpellRender.BUBBLE));
			for (int i = 0; i < 5; i++) {
				long s = e.getId() * 7L + i;
				float y = e.getEyeHeight() + (age * 0.05f + SpellRender.hash(s)) % 1.2f;
				SpellRender.billboard(bubbles, pose, facing, (SpellRender.hash(s + 1) - 0.5f) * 0.5f, y, (SpellRender.hash(s + 2) - 0.5f) * 0.5f, 0.12f, 0, WHITE, 0.8f * shown,
						0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		}
	}

	/** Emerald Healing: small leaves of light rising round the healed, turning as they go. */
	private static void leavesRising(PoseStack pose, MultiBufferSource buffers, Quaternionf facing, LivingEntity e, float age, float shown) {
		VertexConsumer leaves = buffers.getBuffer(SpellRender.glow(SpellRender.LEAF));
		float h = e.getBbHeight(), w = e.getBbWidth() / 2 + 0.35f;
		for (int i = 0; i < 8; i++) {
			long s = e.getId() * 19L + i;
			float t = (age * 0.02f + SpellRender.hash(s)) % 1;
			double a = SpellRender.hash(s + 1) * Math.PI * 2 + t * 3;
			SpellRender.billboard(leaves, pose, facing, (float) Math.cos(a) * w, t * h * 1.2f, (float) Math.sin(a) * w, 0.22f, age * 0.1f + i, LIFE,
					shown * Mth.sin((float) Math.PI * t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	private SpellAuraWorld() {}
}
