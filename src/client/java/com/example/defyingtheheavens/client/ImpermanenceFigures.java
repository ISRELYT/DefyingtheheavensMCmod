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
		// Rising up out of the mist as it comes: a little lower while it forms.
		float rising = (1 - SpellRender.easeOut(Mth.clamp(age / 16, 0, 1))) * 1.2f * size;
		pose(figure, white, age, construct.phase(), construct.phaseAge(partial));
		pose.pushPose();
		pose.translate(0, -rising, 0);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - construct.drawnYaw(partial)));
		pose.scale(size, size, size);
		pose.scale(-1.0f, -1.0f, 1.0f);
		pose.translate(0.0f, -1.501f, 0.0f);
		VertexConsumer body = buffers.getBuffer(RenderType.entityTranslucent(figure.texture()));
		figure.root().render(pose, body, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, shown);
		VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(figure.glow()));
		figure.root().render(pose, glow, SpellRender.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, shown);
		pose.popPose();
		mist(pose, buffers, white, age, size, shown, cam);
	}

	/** The figure posed at {@code age} ticks (its float, its sway; its {@code move} under way {@code moving} ticks, if any): radians, model pixels. */
	private static void pose(Figure f, boolean white, float age, String move, float moving) {
		for (ModelPart part : f.parts().values()) part.resetPose();
		float breathe = Mth.sin(age * 0.08f);
		set(f, "body", 6, 0, 0);
		ModelPart body = f.parts().get("body");
		if (body != null) body.y += -7 - 1.5f * breathe;
		set(f, "skirt", -4 + 3 * Mth.sin(age * 0.11f), 0, 2 * Mth.sin(age * 0.07f));
		set(f, "head", 4 + 2 * Mth.sin(age * 0.05f), 0, white ? 3 : -3);
		set(f, "hair", 8 + 4 * Mth.sin(age * 0.09f), 0, 0);
		set(f, "tongue", -6 + 5 * Mth.sin(age * 0.13f), 0, 4 * Mth.sin(age * 0.1f));
		set(f, "right_arm", -18 + 4 * Mth.sin(age * 0.07f), 0, 8);
		set(f, "left_arm", -10 - 4 * Mth.sin(age * 0.07f), 0, -10);
		set(f, "staff", 18, 0, 0);
		set(f, "plaque", 14, 0, 0);
		set(f, "streamers", 10 * Mth.sin(age * 0.2f), 0, 8 * Mth.sin(age * 0.17f));
		if ("strike".equals(move) && moving < STRIKE + 8) {
			float p = Math.min(1, moving / STRIKE);
			if (white) {
				// The staff raised high over its head, then brought down hard.
				float up = SpellRender.ease(Math.min(1, p / 0.45f)), down = SpellRender.ease(Mth.clamp((p - 0.45f) / 0.2f, 0, 1));
				set(f, "right_arm", -18 - 150 * up + 130 * down, 0, 8 - 6 * up);
				set(f, "body", 6 - 8 * up + 22 * down, 0, 0);
			} else {
				// Its hand thrust out at its quarry, its chain lashing from it (held out while it holds it).
				float thrust = SpellRender.easeOut(Math.min(1, p / 0.3f));
				set(f, "right_arm", -18 - 72 * thrust, -10 * thrust, 8);
				set(f, "body", 6 + 8 * thrust, -12 * thrust, 0);
			}
		} else if (!white && "strike".equals(move)) {
			set(f, "right_arm", -90 + 3 * Mth.sin(age * 0.2f), -10, 8);
			set(f, "body", 14, -12, 0);
		}
		if ("take".equals(move)) {
			// Both reach for its soul, leaning in to it.
			float reach = SpellRender.easeOut(Math.min(1, moving / (TAKE * 0.4f)));
			set(f, "right_arm", -18 - 62 * reach, 12 * reach, 8);
			set(f, "left_arm", -10 - 70 * reach, -12 * reach, -10);
			set(f, "body", 6 + 14 * reach, 0, 0);
			set(f, "head", 4 + 14 * reach, 0, 0);
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
