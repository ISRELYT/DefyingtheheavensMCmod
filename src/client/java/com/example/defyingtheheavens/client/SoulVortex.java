package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.GhostSpells;
import com.example.defyingtheheavens.SpellConstruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Ten Thousand Souls Ascend, drawn by hand (SpellConstruct SOUL_VORTEX; the user's ask, 2026-10-25, of its helix of the mod's ghosts: "it looks
 * retarded try a diff style"): a vortex of souls, as Minecraft's own souls rise off soul sand. A pit of the dark opens on the land, a ring of
 * ghost fire round its mouth; out of it a whirlwind of ghost fire winds up, widening as it rises; and up it the souls pour by the hundred,
 * round and round, each a wailing face of the dead as it comes up out of the dark that burns away into a wisp of cold blue fire as it climbs
 * (the soul particle's own frames, from its face to its flame), streams of blue fire twisting up among them. As it goes back down into the
 * dark ({@code descend}) the whole of it is dragged down into the pit, the souls pouring back down, and the pit closes.
 */
final class SoulVortex {
	/** The soul particle's frames, its face first and its flame last (vanilla's), and the soul fire's strip (32 frames, top to bottom). */
	private static final ResourceLocation[] SOULS = new ResourceLocation[11];
	private static final ResourceLocation SOUL_FIRE = new ResourceLocation("minecraft", "textures/block/soul_fire_0.png");
	private static final int SOUL_FIRE_FRAMES = 32;

	static {
		for (int i = 0; i < SOULS.length; i++) SOULS[i] = new ResourceLocation("minecraft", "textures/particle/soul_" + i + ".png");
	}

	/** How many souls are in it at once, and how long one takes to climb it (ticks). */
	private static final int SOUL_COUNT = 420;
	private static final float CLIMB = 70;
	private static final float[] TEAL = {0.3f, 0.95f, 1.0f}, PALE = {0.75f, 1.0f, 1.0f}, DEEP = {0.1f, 0.45f, 0.75f}, DARK = {0.0f, 0.02f, 0.03f},
			WHITE = {1, 1, 1};

	static void render(SpellConstruct vortex, float partial, PoseStack pose, MultiBufferSource buffers, Quaternionf camera, Vec3 cam) {
		float age = vortex.tickCount + partial;
		float shown = SpellRender.fade(age, vortex.life(), 6, SpellConstruct.FADE);
		if (shown <= 0) return;
		float radius = Math.max(2, vortex.size()), high = Math.max(8, vortex.value());
		// Rising out of the dark, and (descending) dragged back down into it.
		float rise = SpellRender.easeOut(age / GhostSpells.SOULS_RISES);
		boolean down = "descend".equals(vortex.phase());
		float sinking = down ? SpellRender.ease(vortex.phaseAge(partial) / GhostSpells.SOULS_DESCENDS) : 0;
		float tall = high * rise * (1 - sinking);
		float open = Math.min(rise * 1.4f, 1) * (1 - Math.max(0, sinking - 0.6f) / 0.4f);

		// The pit: darkness lying on the land, a ring of ghost fire round its mouth, light welling up out of it.
		VertexConsumer smoke = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
		SpellRender.disc(smoke, pose.last(), radius * 1.15f * open, 0.08f, age * 0.01f, DARK, 0.85f * shown, SpellRender.FULL_BRIGHT);
		SpellRender.disc(smoke, pose.last(), radius * 0.8f * open, 0.1f, -age * 0.015f, DARK, 0.9f * shown, SpellRender.FULL_BRIGHT);
		VertexConsumer ring = buffers.getBuffer(SpellRender.glow(SpellRender.RING));
		SpellRender.disc(ring, pose.last(), radius * 1.2f * open, 0.14f, age * 0.02f, TEAL, 0.9f * shown, SpellRender.FULL_BRIGHT);
		SpellRender.disc(ring, pose.last(), radius * 0.9f * open, 0.16f, -age * 0.03f, DEEP, 0.7f * shown, SpellRender.FULL_BRIGHT);
		VertexConsumer fire = buffers.getBuffer(SpellRender.glow(SOUL_FIRE));
		int rim = 22;
		for (int i = 0; i < rim; i++) {
			double a = Math.PI * 2 * i / rim + age * 0.01;
			float r = radius * 1.18f * open;
			float h = (1.6f + 1.2f * SpellRender.hash(i * 7L)) * (0.8f + 0.2f * Mth.sin(age * 0.3f + i)) * open;
			SpellRender.flameTongue(fire, pose, cam, (float) Math.cos(a) * r, 0, (float) Math.sin(a) * r, h * 0.8f, h, age + i * 5, WHITE, 0.95f * shown, SOUL_FIRE_FRAMES);
		}
		if (tall < 0.5f) return;

		// The whirlwind of ghost fire, three walls of it turning and twisting up, widening as they rise.
		for (int layer = 0; layer < 3; layer++) {
			float spin = age * (0.06f + 0.025f * layer) * (layer % 2 == 0 ? 1 : -1);
			float base = radius * (0.35f + 0.12f * layer), top = radius * (0.95f + 0.2f * layer);
			pose.pushPose();
			pose.mulPose(Axis.YP.rotation(spin));
			int frame = Math.floorMod((int) (age * 0.5f) + layer * 9, SOUL_FIRE_FRAMES);
			float v0 = frame / (float) SOUL_FIRE_FRAMES;
			SpellRender.cylinder(fire, pose.last(), tall, h -> base + (top - base) * (float) Math.sqrt(h), 14, 28, layer == 1 ? PALE : TEAL,
					(0.5f - 0.1f * layer) * shown, 4 + layer, 1f / SOUL_FIRE_FRAMES, 0, v0, 1.6f + layer * 0.6f, SpellRender.FULL_BRIGHT, h -> Math.min(1, h * 6) * (1 - h * h));
			pose.popPose();
		}
		// Streams of blue fire twisting up among the souls.
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		for (int s = 0; s < 6; s++) {
			for (int k = 0; k < 16; k++) {
				float f = ((age * 0.02f + k / 16f + s * 0.13f) % 1);
				double a = s * Math.PI / 3 + f * Math.PI * 4 + age * 0.05;
				float r = radius * (0.4f + 0.55f * f);
				float y = f * tall;
				SpellRender.billboard(glow, pose, camera, (float) Math.cos(a) * r, y, (float) Math.sin(a) * r, 1.1f + f * 1.4f, 0, TEAL, 0.5f * (1 - f) * shown,
						0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		}

		// The souls, pouring up round it: faces as they come up out of the dark, wisps of cold fire as they near its top (poured back down, descending).
		VertexConsumer[] frames = new VertexConsumer[SOULS.length];
		for (int i = 0; i < SOUL_COUNT; i++) {
			float speed = 0.85f + 0.3f * SpellRender.hash(i * 3L + 1);
			float climbed = ((age * speed / CLIMB + SpellRender.hash(i * 3L + 2)) % 1);
			if (down) climbed = Math.max(0, climbed - sinking * 1.2f);
			float y = climbed * tall;
			if (y < 0.2f) continue;
			double a = SpellRender.hash(i * 3L) * Math.PI * 2 + age * (0.07f + 0.04f * SpellRender.hash(i * 5L)) + climbed * Math.PI * 3;
			float r = radius * (0.25f + 0.75f * SpellRender.hash(i * 7L + 3)) * (0.55f + 0.6f * climbed);
			int frame = Mth.clamp((int) (climbed * SOULS.length * 1.05f), 0, SOULS.length - 1);
			if (frames[frame] == null) frames[frame] = buffers.getBuffer(frame < 5 ? SpellRender.clear(SOULS[frame]) : SpellRender.glow(SOULS[frame]));
			float size = (0.9f + 0.7f * climbed) * (0.8f + 0.4f * SpellRender.hash(i * 11L));
			float alpha = Math.min(1, climbed * 8) * (1 - Math.max(0, climbed - 0.85f) / 0.15f) * shown;
			SpellRender.billboard(frames[frame], pose, camera, (float) Math.cos(a) * r, y, (float) Math.sin(a) * r, size, 0, frame < 5 ? WHITE : PALE, alpha,
					0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	private SoulVortex() {}
}
