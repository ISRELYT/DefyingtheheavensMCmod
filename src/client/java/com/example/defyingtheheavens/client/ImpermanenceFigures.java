package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.SpellConstruct;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.io.Reader;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Black and White Impermanence drawn as beings are (SpellConstruct WHITE_IMPERMANENCE_FIGURE, BLACK_IMPERMANENCE_FIGURE; the user's ask,
 * 2026-10-25, of their glowing ghosts: "it looks retarded try a diff style"): two tall figures of the underworld modelled as Minecraft
 * models its mobs, boxes painted (models/figure/&lt;id&gt;.json, textures/entity/spell/&lt;id&gt;.png: tools/spell-tex/figures.py, which draws
 * them as here too), lit by the world, their eyes and marks glowing of themselves (&lt;id&gt;_glow.png). The White in white mourning robes and
 * a tall white hat, smiling, its long red tongue hanging from its mouth, its mourning staff hung with paper in its right hand; the Black
 * in black, scowling, red-eyed, the plaque of the underworld's summons in its left. They hover a little off the ground, their robes'
 * hems dissolving into mist (the White's pale, the Black's dark smoke), swaying, their hair, tongue and paper stirring; and they move as
 * their spell calls ({@code strike}: the White raises its staff high and brings it down, the Black thrusts out its hand and its chain
 * lashes from it; {@code take}: both reach for their quarry's soul). Their size their height in men.
 */
final class ImpermanenceFigures {
	private record Figure(ModelPart root, Map<String, ModelPart> parts, ResourceLocation texture, ResourceLocation glow) {}

	private static final Map<SpellConstruct.Kind, Figure> FIGURES = new EnumMap<>(SpellConstruct.Kind.class);
	private static final float[] WHITE_MIST = {0.92f, 0.96f, 1.0f}, BLACK_SMOKE = {0.06f, 0.04f, 0.08f}, TEAL = {0.4f, 0.95f, 1.0f};
	/** How long each move takes (ticks): the White's swing lands as GhostSpells#Impermanence strikes, 6 ticks in. */
	private static final float STRIKE = 12, TAKE = 20;

	static void render(SpellConstruct construct, float partial, PoseStack pose, MultiBufferSource buffers, int light, Vec3 cam) {
		Figure figure = figure(construct.kind());
		if (figure == null) return;
		boolean white = construct.kind() == SpellConstruct.Kind.WHITE_IMPERMANENCE_FIGURE;
		float age = construct.tickCount + partial;
		float shown = SpellRender.fade(age, construct.life(), 14, SpellConstruct.FADE);
		if (shown <= 0) return;
		float size = Math.max(0.5f, construct.size());
		String move = construct.phase();
		float moving = construct.phaseAge(partial);
		// Rising up out of the underworld's whirl as it comes, turning as it rises (the user's ask, 2026-10-26: "needs some more vfx and
		// animations, its a little bland rn"); then floating, bobbing a little; lunging in as it strikes, pulled back as it tears the soul away.
		float come = SpellRender.easeOut(Mth.clamp(age / 18, 0, 1));
		float rising = (1 - come) * 2.4f * size;
		float spin = (1 - come) * (1 - come) * 220 * (white ? 1 : -1);
		float bob = (0.1f + 0.06f * Mth.sin(age * 0.05f)) * size * Mth.sin(age * 0.09f + (white ? 0 : 1.6f)) * come;
		float lunge = 0;
		if ("strike".equals(move)) {
			float p = Math.min(1, moving / STRIKE);
			lunge = white ? 0.35f * size * SpellRender.ease(Mth.clamp((p - 0.4f) / 0.25f, 0, 1)) * (1 - SpellRender.ease(Mth.clamp((p - 0.9f) / 0.6f, 0, 1)))
					: 0.25f * size * SpellRender.easeOut(Math.min(1, p / 0.3f));
		}
		if ("take".equals(move)) {
			float reach = SpellRender.easeOut(Math.min(1, moving / (TAKE * 0.4f))), yank = SpellRender.ease(Mth.clamp((moving - TAKE * 0.5f) / 5, 0, 1));
			lunge = size * (0.3f * reach - 0.45f * yank);
		}
		pose(figure, white, age, move, moving);
		float yaw = construct.drawnYaw(partial);
		pose.pushPose();
		Vec3 ahead = Vec3.directionFromRotation(0, yaw);
		pose.translate(ahead.x * lunge, bob + 0.15f * size - rising, ahead.z * lunge);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw + spin));
		pose.scale(size, size, size);
		pose.scale(-1.0f, -1.0f, 1.0f);
		pose.translate(0.0f, -1.501f, 0.0f);
		VertexConsumer body = buffers.getBuffer(RenderType.entityTranslucent(figure.texture()));
		figure.root().render(pose, body, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, shown);
		VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(figure.glow()));
		figure.root().render(pose, glow, SpellRender.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, shown);
		pose.popPose();
		mist(pose, buffers, white, age, size, shown, cam);
		MultiBufferSource spell = SpellBuffers.begin(buffers);
		try {
			underworld(construct, pose, spell, white, age, size, shown, come, move, moving, partial);
		} finally {
			SpellBuffers.end();
		}
	}

	private static final float[] PAPER = {1.0f, 0.95f, 0.78f}, CRIMSON_PAPER = {0.75f, 0.1f, 0.12f}, SOUL = {0.55f, 1.0f, 0.95f}, CRIMSON = {0.95f, 0.18f, 0.2f};

	/**
	 * What comes with them out of the underworld: a whirl of it open under each (the White's pale, the Black's dark, ringed in ghost fire); its
	 * light about them, pulsing; spirit paper fluttering round them (the White's pale joss paper, the Black's crimson talismans); a wisp of ghost
	 * fire floating at their shoulder like a lantern; as the White's staff comes down, a shock of pale light bursting over the ground at its
	 * quarry's feet, and as the Black's chain lashes out, a crimson one; and as they take its soul, its light drawn out of it in streams into
	 * their reaching hands.
	 */
	private static void underworld(SpellConstruct c, PoseStack pose, MultiBufferSource buffers, boolean white, float age, float size, float shown, float come, String move,
								   float moving, float partial) {
		org.joml.Quaternionf facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
		float[] fire = white ? SOUL : CRIMSON, paper = white ? PAPER : CRIMSON_PAPER;
		// The whirl open under it.
		float open = Mth.clamp(age / 8, 0, 1) * shown;
		VertexConsumer smoke = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
		pose.pushPose();
		pose.translate(0, 0.06f, 0);
		SpellRender.disc(smoke, pose.last(), size * 0.75f * open, 0, age * 0.05f * (white ? 1 : -1), white ? new float[] {0.75f, 0.85f, 0.9f} : new float[] {0.03f, 0.02f, 0.04f},
				(white ? 0.5f : 0.85f) * open, SpellRender.FULL_BRIGHT);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RUNE)), pose.last(), size * 0.62f * open, 0.02f, -age * 0.03f, fire, 0.5f * open, SpellRender.FULL_BRIGHT);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RING)), pose.last(), size * 0.82f * open * (1 + 0.04f * Mth.sin(age * 0.3f)), 0.04f, 0, fire,
				0.7f * open, SpellRender.FULL_BRIGHT);
		pose.popPose();
		// Ghost fire licking up round the whirl's rim as it comes.
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(c.getPosition(partial));
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10 + age * 0.02;
			float tall = size * (0.25f + 0.2f * Mth.sin(age * 0.4f + i * 2.3f)) * (0.4f + 0.6f * (1 - come) + 0.3f);
			SpellRender.flameTongue(flames, pose, eye, (float) Math.cos(a) * size * 0.7f, 0.05f, (float) Math.sin(a) * size * 0.7f, size * 0.18f, tall, age + i * 3, fire,
					0.8f * open, SpellRender.FLAME_FRAMES);
		}
		// Its light about it, pulsing.
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		float pulse = 0.85f + 0.15f * Mth.sin(age * 0.17f);
		SpellRender.billboard(glow, pose, facing, 0, size * 1.1f, 0, size * 2.6f * pulse, 0, white ? new float[] {0.8f, 1.0f, 1.0f} : new float[] {0.25f, 0.6f, 0.65f},
				(white ? 0.22f : 0.16f) * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		// Spirit paper fluttering round it, rising and turning.
		VertexConsumer sheets = buffers.getBuffer(SpellRender.clear(SpellRender.GLOW));
		for (int i = 0; i < 9; i++) {
			float t = (age * 0.012f + i / 9f) % 1;
			double a = i * 2.39996 + age * (white ? 0.035 : -0.035);
			float r = size * (0.55f + 0.25f * Mth.sin(age * 0.05f + i));
			pose.pushPose();
			pose.translate(Math.cos(a) * r, size * (0.2f + 1.8f * t), Math.sin(a) * r);
			pose.mulPose(Axis.YP.rotation((float) -a + age * 0.05f));
			pose.mulPose(Axis.XP.rotation(Mth.sin(age * 0.3f + i) * 0.9f));
			pose.mulPose(Axis.ZP.rotation(Mth.cos(age * 0.23f + i * 1.7f) * 0.6f));
			float w = size * 0.07f, h = size * 0.12f, a0 = 0.9f * shown * Math.min(1, (1 - t) * 4) * Math.min(1, t * 6);
			SpellRender.quad(sheets, pose.last(), SpellRender.v(-w, -h, 0), SpellRender.v(-w, h, 0), SpellRender.v(w, h, 0), SpellRender.v(w, -h, 0), paper, a0, 0.47f, 0.47f, 0.53f,
					0.53f, SpellRender.FULL_BRIGHT);
			pose.popPose();
		}
		// A wisp of ghost fire floating at its shoulder, a lantern of it.
		float lx = (white ? -1 : 1) * size * 0.55f, ly = size * (1.55f + 0.08f * Mth.sin(age * 0.12f)), lz = size * 0.1f;
		SpellRender.billboard(glow, pose, facing, lx, ly, lz, size * 0.75f * pulse, 0, fire, 0.55f * shown * come, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		SpellRender.flameTongue(flames, pose, eye, lx, ly - size * 0.12f, lz, size * 0.2f, size * 0.38f, age, fire, 0.95f * shown * come, SpellRender.FLAME_FRAMES);
		// Its blow on its quarry: a shock bursting over the ground at its feet.
		net.minecraft.world.entity.Entity quarry = c.target();
		if (quarry == null) return;
		Vec3 at = quarry.getPosition(partial).subtract(c.getPosition(partial));
		if ("strike".equals(move)) {
			float landed = moving - STRIKE * 0.55f;
			if (landed > 0 && landed < 14) {
				float t = landed / 14;
				pose.pushPose();
				pose.translate(at.x, at.y + 0.08f, at.z);
				SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RING)), pose.last(), size * (0.3f + 2.2f * SpellRender.easeOut(t)), 0, 0, fire, 0.9f * (1 - t) * shown,
						SpellRender.FULL_BRIGHT);
				SpellRender.disc(smoke, pose.last(), size * (0.4f + 1.6f * SpellRender.easeOut(t)), 0.02f, t, white ? WHITE_MIST : BLACK_SMOKE, 0.6f * (1 - t) * shown,
						SpellRender.FULL_BRIGHT);
				pose.popPose();
				SpellRender.billboard(glow, pose, facing, (float) at.x, (float) at.y + 1, (float) at.z, size * 2.2f * (1 - t), 0, fire, 0.7f * (1 - t) * shown, 0, 0, 1, 1,
						SpellRender.FULL_BRIGHT);
			}
		}
		// Its soul drawn out of it in streams into their reaching hands.
		if ("take".equals(move) && moving < TAKE + 6) {
			float strength = Math.min(1, moving / 6) * Math.max(0, 1 - (moving - TAKE) / 6) * shown;
			Vec3 hands = new Vec3(0, size * 1.55f, 0).add(Vec3.directionFromRotation(0, c.drawnYaw(partial)).scale(size * 0.7));
			Vec3 from = at.add(0, quarry.getBbHeight() * 0.6, 0);
			VertexConsumer motes = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
			for (int i = 0; i < 18; i++) {
				float t = (moving * 0.07f + i / 18f) % 1;
				Vec3 p = from.lerp(hands, t).add(0, Math.sin(Math.PI * t) * size * 0.4, 0)
						.add(Mth.sin(age * 0.3f + i) * 0.2f * Mth.sin((float) Math.PI * t), 0, Mth.cos(age * 0.27f + i) * 0.2f * Mth.sin((float) Math.PI * t));
				SpellRender.billboard(motes, pose, facing, (float) p.x, (float) p.y, (float) p.z, size * 0.12f, 0, SOUL, strength, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
			SpellRender.billboard(glow, pose, facing, (float) from.x, (float) from.y, (float) from.z, size * 1.2f, 0, SOUL, 0.5f * strength, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			SpellRender.billboard(glow, pose, facing, (float) hands.x, (float) hands.y, (float) hands.z, size * 0.9f, 0, SOUL, 0.6f * strength, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** The figure posed at {@code age} ticks (its float, its sway; its {@code move} under way {@code moving} ticks, if any): radians, model pixels. */
	private static void pose(Figure f, boolean white, float age, String move, float moving) {
		for (ModelPart part : f.parts().values()) part.resetPose();
		float breathe = Mth.sin(age * 0.08f);
		// Rising, it leans back, then settles forward over its quarry.
		float come = SpellRender.easeOut(Mth.clamp(age / 18, 0, 1));
		set(f, "body", 6 - 14 * (1 - come), 0, 2 * Mth.sin(age * 0.06f));
		ModelPart body = f.parts().get("body");
		if (body != null) body.y += -7 - 1.5f * breathe;
		set(f, "skirt", -4 + 5 * Mth.sin(age * 0.11f), 0, 3 * Mth.sin(age * 0.07f));
		// It looks about now and again, then back to its quarry.
		float glance = Mth.sin(age * 0.031f) > 0.7f ? (Mth.sin(age * 0.031f) - 0.7f) / 0.3f * (white ? 25 : -25) : 0;
		set(f, "head", 4 + 2 * Mth.sin(age * 0.05f), glance, (white ? 3 : -3) + 4 * Mth.sin(age * 0.045f));
		set(f, "hat", 0, 0, 3 * Mth.sin(age * 0.07f + 1));
		set(f, "hair", 8 + 6 * Mth.sin(age * 0.09f), 0, 4 * Mth.sin(age * 0.07f));
		set(f, "tongue", -6 + 9 * Mth.sin(age * 0.13f), 0, 8 * Mth.sin(age * 0.1f));
		set(f, "right_arm", -18 + 7 * Mth.sin(age * 0.07f), 0, 8 + 3 * Mth.sin(age * 0.05f));
		set(f, "left_arm", -10 - 7 * Mth.sin(age * 0.07f), 0, -10 - 3 * Mth.sin(age * 0.05f));
		set(f, "staff", 18 + 4 * Mth.sin(age * 0.07f), 0, 0);
		set(f, "plaque", 14 + 5 * Mth.sin(age * 0.09f), 0, 0);
		set(f, "streamers", 14 * Mth.sin(age * 0.2f), 0, 12 * Mth.sin(age * 0.17f));
		if ("strike".equals(move) && moving < STRIKE + 8) {
			float p = Math.min(1, moving / STRIKE);
			if (white) {
				// The staff raised high over its head, held a moment, then brought down hard, the body thrown in after it.
				float up = SpellRender.ease(Math.min(1, p / 0.4f)), down = SpellRender.ease(Mth.clamp((p - 0.5f) / 0.15f, 0, 1));
				set(f, "right_arm", -18 - 160 * up + 140 * down, 0, 8 - 6 * up);
				set(f, "left_arm", -10 - 40 * up + 30 * down, 0, -10 - 10 * up);
				set(f, "body", 6 - 12 * up + 30 * down, 0, 0);
				set(f, "head", 4 - 10 * up + 18 * down, 0, 3);
				set(f, "streamers", 50 * down * (1 - p), 0, 25 * Mth.sin(moving * 0.9f));
			} else {
				// Its hand thrust out at its quarry, its chain lashing from it, the body recoiling a little as it bites (held out while it holds it).
				float thrust = SpellRender.easeOut(Math.min(1, p / 0.3f)), recoil = Mth.clamp((p - 0.3f) / 0.2f, 0, 1) * (1 - Mth.clamp((p - 0.5f) / 0.3f, 0, 1));
				set(f, "right_arm", -18 - 76 * thrust + 10 * recoil, -12 * thrust, 8);
				set(f, "left_arm", -10 + 20 * thrust, 0, -16 * thrust);
				set(f, "body", 6 + 10 * thrust - 6 * recoil, -14 * thrust, 0);
				set(f, "plaque", 14 + 40 * thrust, 0, 0);
			}
		} else if (!white && "strike".equals(move)) {
			// Holding its chain taut, hauling on it.
			float haul = Mth.sin(age * 0.25f);
			set(f, "right_arm", -88 + 6 * haul, -10, 8);
			set(f, "body", 12 - 3 * haul, -12, 0);
		}
		if ("take".equals(move)) {
			// Both reach for its soul, leaning in to it, then tear it away.
			float reach = SpellRender.easeOut(Math.min(1, moving / (TAKE * 0.4f))), yank = SpellRender.ease(Mth.clamp((moving - TAKE * 0.5f) / 5, 0, 1));
			set(f, "right_arm", -18 - 70 * reach + 50 * yank, 12 * reach, 8 + 10 * yank);
			set(f, "left_arm", -10 - 78 * reach + 55 * yank, -12 * reach, -10 - 10 * yank);
			set(f, "body", 6 + 16 * reach - 22 * yank, 0, 0);
			set(f, "head", 4 + 14 * reach - 18 * yank, 0, 0);
		}
	}

	private static void set(Figure f, String name, float x, float y, float z) {
		ModelPart part = f.parts().get(name);
		if (part == null) return;
		part.xRot = x * Mth.DEG_TO_RAD;
		part.yRot = y * Mth.DEG_TO_RAD;
		part.zRot = z * Mth.DEG_TO_RAD;
	}

	/** Its hem dissolving into mist about its foot (the White's pale, the Black's dark smoke), and cold wisps drifting up off it. */
	private static void mist(PoseStack pose, MultiBufferSource buffers, boolean white, float age, float size, float shown, Vec3 cam) {
		net.minecraft.client.Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		org.joml.Quaternionf facing = camera.rotation();
		VertexConsumer smoke = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
		float[] rgb = white ? WHITE_MIST : BLACK_SMOKE;
		for (int i = 0; i < 9; i++) {
			float t = (age * 0.015f + i / 9f) % 1;
			double a = i * 2.39996 + age * 0.004;
			float r = size * (0.25f + 0.25f * t);
			SpellRender.billboard(smoke, pose, facing, (float) Math.cos(a) * r, size * (0.12f + 0.5f * t), (float) Math.sin(a) * r, size * (0.6f + 0.5f * t), i, rgb,
					(white ? 0.42f : 0.6f) * (1 - t) * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
		VertexConsumer wisps = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 6; i++) {
			float t = (age * 0.025f + i / 6f) % 1;
			double a = i * 1.7 + age * 0.03;
			float r = size * 0.35f;
			SpellRender.billboard(wisps, pose, facing, (float) Math.cos(a) * r, size * (0.3f + 1.6f * t), (float) Math.sin(a) * r, size * 0.12f, 0, white ? WHITE_MIST : TEAL,
					0.7f * (1 - t) * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** {@code kind}'s figure, built from its model the first time it is drawn (null if it can't be read). */
	private static Figure figure(SpellConstruct.Kind kind) {
		Figure f = FIGURES.get(kind);
		if (f != null || FIGURES.containsKey(kind)) return f;
		try {
			f = build(kind.id);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Couldn't build the figure {}", kind.id, e);
		}
		FIGURES.put(kind, f);
		return f;
	}

	/** Forgets the figures (the resources reloaded: they are built again from their models). */
	static void forget() {
		FIGURES.clear();
	}

	private static Figure build(String id) throws Exception {
		ResourceLocation at = DefyingTheHeavens.id("models/figure/" + id + ".json");
		JsonObject json;
		try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(at)) {
			json = JsonParser.parseReader(reader).getAsJsonObject();
		}
		JsonArray texture = json.getAsJsonArray("texture");
		MeshDefinition mesh = new MeshDefinition();
		Map<String, PartDefinition> defined = new HashMap<>();
		java.util.List<String[]> order = new java.util.ArrayList<>();
		for (JsonElement e : json.getAsJsonArray("parts")) {
			JsonObject part = e.getAsJsonObject();
			String parent = part.get("parent").getAsString();
			PartDefinition under = parent.isEmpty() ? mesh.getRoot() : defined.get(parent);
			if (under == null) throw new IllegalStateException("part " + part.get("name") + " comes before its parent " + parent);
			CubeListBuilder cubes = CubeListBuilder.create();
			for (JsonElement b : part.getAsJsonArray("boxes")) {
				JsonObject box = b.getAsJsonObject();
				float[] from = floats(box.getAsJsonArray("from")), size = floats(box.getAsJsonArray("size"));
				float[] uv = floats(box.getAsJsonArray("uv"));
				float grow = box.has("grow") ? box.get("grow").getAsFloat() : 0;
				cubes.texOffs((int) uv[0], (int) uv[1]).mirror(box.has("mirror") && box.get("mirror").getAsBoolean())
						.addBox(from[0], from[1], from[2], size[0], size[1], size[2], new CubeDeformation(grow));
			}
			float[] pivot = floats(part.getAsJsonArray("pivot"));
			String name = part.get("name").getAsString();
			defined.put(name, under.addOrReplaceChild(name, cubes, PartPose.offset(pivot[0], pivot[1], pivot[2])));
			order.add(new String[] {name, parent});
		}
		ModelPart root = LayerDefinition.create(mesh, texture.get(0).getAsInt(), texture.get(1).getAsInt()).bakeRoot();
		Map<String, ModelPart> parts = new HashMap<>();
		for (String[] np : order) parts.put(np[0], (np[1].isEmpty() ? root : parts.get(np[1])).getChild(np[0]));
		return new Figure(root, parts, DefyingTheHeavens.id("textures/entity/spell/" + id + ".png"), DefyingTheHeavens.id("textures/entity/spell/" + id + "_glow.png"));
	}

	private static float[] floats(JsonArray a) {
		float[] out = new float[a.size()];
		for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsFloat();
		return out;
	}

	private ImpermanenceFigures() {}
}
