package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpellAura;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The spell auras that clothe a body (on everything drawn with a humanoid model: players, cultivators): a skin over it of the ward's
 * stuff (Hardened Body's thin stone, Azure Coating's water, Ancient Bark Skin, Mountain Stance's slabs, Vajra Body's gold, Emerald
 * Healing's green shimmer, Crimson Tempering's ember veins), Flame Armour's armour of living flame with its crest and shoulder fires,
 * and Flame Fist's fire round both fists. Each grows in as it is cast and fades as it ends. A demon's beast's head and tail (BeastLayer)
 * are clothed in each skin too, in place of the man's head (the user's rule, 2026-10-23: whatever coats the body coats a race's whole
 * form).
 */
public class SpellAuraLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
	private static final float[] WHITE = {1, 1, 1};
	private static final float[] FIRE_MID = {1.0f, 0.55f, 0.15f}, FIRE_CORE = {1.0f, 0.92f, 0.65f};
	/** How far (blocks) the thin and thick shells stand off the body (the armour models' 0.5 and 1 pixel, drawn at 15/16): a beast's skin as far off it. */
	private static final float THIN_GROW = 0.03f, THICK_GROW = 0.06f;
	private final HumanoidModel<T> thin, thick;
	/** The beast's head and tail of the body being drawn now (BeastHeadModels), posed as it, or null for a man's. */
	private BeastHeadModels.Head beast;

	public SpellAuraLayer(RenderLayerParent<T, M> parent, EntityRendererProvider.Context context) {
		super(parent);
		this.thin = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
		this.thick = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource source, int light, T entity, float walk, float walkSpeed, float partial, float age, float headYaw,
					   float headPitch) {
		if (!ClientSpellAuras.any(entity) && MeteorFlightClient.heat(entity) < 0.02f || entity.isInvisible()) return;
		MultiBufferSource buffers = SpellBuffers.begin(source);
		beast = BeastHeadModels.of(entity);
		if (beast != null) beast.follow(getParentModel());
		try {
			draw(pose, buffers, light, entity, partial, age);
		} finally {
			SpellBuffers.end();
			beast = null;
		}
	}

	private void draw(PoseStack pose, MultiBufferSource buffers, int light, T entity, float partial, float age) {
		M parent = getParentModel();
		float hardened = ClientSpellAuras.shown(entity, SpellAura.HARDENED_BODY, partial, 10, 20);
		if (hardened > 0) skin(pose, buffers, thin, SpellRender.clearLit(SpellRender.STONE_SHELL), new float[]{0.95f, 0.85f, 0.72f}, 0.55f * hardened, light);
		float coating = ClientSpellAuras.shown(entity, SpellAura.AZURE_COATING, partial, 8, 20);
		if (coating > 0) {
			float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
			skin(pose, buffers, thin, RenderType.energySwirl(SpellRender.WATER, 0, fv[0] + age * 0.002f), new float[]{0.55f, 0.75f, 1.0f}, 0.65f * coating,
					SpellRender.FULL_BRIGHT);
		}
		float emerald = ClientSpellAuras.shown(entity, SpellAura.EMERALD_HEALING, partial, 8, 20);
		if (emerald > 0) {
			float pulse = 0.25f + 0.1f * Mth.sin(age * 0.2f);
			skin(pose, buffers, thin, RenderType.energySwirl(SpellRender.WATER, age * 0.004f, age * 0.006f), new float[]{0.3f, 1.0f, 0.4f}, pulse * emerald,
					SpellRender.FULL_BRIGHT);
		}
		float tempering = ClientSpellAuras.shown(entity, SpellAura.CRIMSON_TEMPERING, partial, 10, 30);
		if (tempering > 0) {
			float pulse = 0.7f + 0.3f * Mth.sin(age * 0.25f);
			skin(pose, buffers, parent, SpellRender.glow(SpellRender.VEINS), new float[]{1.0f, 0.5f, 0.25f}, pulse * tempering, SpellRender.FULL_BRIGHT);
		}
		float bark = ClientSpellAuras.shown(entity, SpellAura.ANCIENT_BARK_SKIN, partial, 12, 20);
		if (bark > 0) skin(pose, buffers, thin, SpellRender.clearLit(SpellRender.BARK), WHITE, 0.92f * bark, light);
		float stance = ClientSpellAuras.shown(entity, SpellAura.MOUNTAIN_STANCE, partial, 10, 20);
		if (stance > 0) skin(pose, buffers, thick, SpellRender.clearLit(SpellRender.STONE_SHELL), new float[]{0.8f, 0.75f, 0.7f}, 0.95f * stance, light);
		float vajra = ClientSpellAuras.shown(entity, SpellAura.VAJRA_BODY, partial, 10, 20);
		if (vajra > 0) {
			skin(pose, buffers, thin, SpellRender.clearLit(SpellRender.GOLD), WHITE, 0.7f * vajra, SpellRender.FULL_BRIGHT);
			skin(pose, buffers, thin, RenderType.energySwirl(SpellRender.GOLD, age * 0.02f, 0), new float[]{1.0f, 0.9f, 0.5f}, 0.4f * vajra, SpellRender.FULL_BRIGHT);
		}
		float armour = ClientSpellAuras.shown(entity, SpellAura.FLAME_ARMOUR, partial, 10, 20);
		if (armour > 0) flameArmour(pose, buffers, parent, age, armour);
		float fist = ClientSpellAuras.shown(entity, SpellAura.FLAME_FIST, partial, 6, 20);
		if (fist > 0) {
			fists(pose, buffers, parent.rightArm, -1, age, fist);
			fists(pose, buffers, parent.leftArm, 1, age + 7, fist);
		}
		float heat = MeteorFlightClient.heat(entity);
		if (heat > 0.02f) meteorHeat(pose, buffers, parent, age, heat);
	}

	/**
	 * Meteor Flight's heat (MeteorFlightClient), as the user's video shows it (2026-10-23: "not just a glow around them"): ablaze,
	 * the body itself turned to glowing gold, the skin showing dimly through, flame flowing over it; cooling, a red glow over the
	 * body that fades. No flames thrown off it.
	 */
	private void meteorHeat(PoseStack pose, MultiBufferSource buffers, M parent, float age, float heat) {
		heatSkins(heat, age, (type, outer, rgb, alpha) -> skin(pose, buffers, outer ? thin : parent, type, rgb, alpha, SpellRender.FULL_BRIGHT));
	}

	/** One skin of the heat: in {@code type}, over the body itself or (outer) just over it, tinted {@code rgb} at {@code alpha}. */
	private interface HeatSkin {
		void draw(RenderType type, boolean outer, float[] rgb, float alpha);
	}

	/**
	 * The skins Meteor Flight's heat lays over a body (all of it: {@link #meteorHeat}; one's own arm in first person: {@link
	 * #firstPersonArm}): ablaze, gold near solid with its light added on top and flame flowing back over it; cooling (and as it
	 * catches), a red glow over it that fades.
	 */
	private static void heatSkins(float heat, float age, HeatSkin skin) {
		float[] rgb = MeteorFlightClient.heatColour(heat);
		float ablaze = MeteorFlightClient.ablaze(heat), cooling = MeteorFlightClient.cooling(heat);
		if (ablaze > 0) {
			skin.draw(SpellRender.clear(SpellRender.GOLD), true, rgb, 0.78f * ablaze);
			skin.draw(SpellRender.glow(SpellRender.GOLD), false, rgb, 0.7f * ablaze);
			float[] fv = SpellRender.frame(age, 0.6f, SpellRender.FLAME_FRAMES);
			skin.draw(RenderType.energySwirl(SpellRender.FLAME, age * 0.03f, fv[0]), true, new float[] {1.0f, 0.75f, 0.35f}, 0.85f * ablaze);
		}
		if (ablaze < 1) skin.draw(SpellRender.glow(SpellRender.GOLD), false, rgb, 0.8f * cooling * (1 - ablaze));
	}

	/** A skin over the whole body in {@code type}, posed as the body: a beast's head and tail too, in place of a man's head. */
	private void skin(PoseStack pose, MultiBufferSource buffers, HumanoidModel<T> shell, RenderType type, float[] rgb, float alpha, int light) {
		if (shell != getParentModel()) getParentModel().copyPropertiesTo(shell);
		shell.setAllVisible(true);
		shell.hat.visible = false;
		shell.head.visible = beast == null;
		VertexConsumer vc = buffers.getBuffer(type);
		shell.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, rgb[0], rgb[1], rgb[2], alpha);
		shell.head.visible = true; // (the body's own model, for whatever draws it after)
		if (beast != null) beast.renderGrown(pose, vc, grow(shell), light, OverlayTexture.NO_OVERLAY, rgb[0], rgb[1], rgb[2], alpha);
	}

	/** How far {@code shell}'s skin stands off the body, for a beast's head and tail to be clothed alike (0: on the body itself). */
	private float grow(HumanoidModel<T> shell) {
		return shell == thick ? THICK_GROW : shell == thin ? THIN_GROW : 0;
	}

	/**
	 * Flame Armour: an armour of living flame over the body (the thick shell, in moving flame), a crest of three tongues over the helm,
	 * fire leaping from the shoulders and burning down the forearms and shins.
	 */
	private void flameArmour(PoseStack pose, MultiBufferSource buffers, M parent, float age, float shown) {
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.FLAME_FRAMES);
		parent.copyPropertiesTo(thick);
		thick.setAllVisible(true);
		thick.hat.visible = false;
		thick.head.visible = beast == null;
		VertexConsumer fire = buffers.getBuffer(RenderType.energySwirl(SpellRender.FLAME, age * 0.01f, fv[0]));
		thick.renderToBuffer(pose, fire, SpellRender.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 0.6f, 0.25f, 0.9f * shown);
		thick.head.visible = true;
		if (beast != null) beast.renderGrown(pose, fire, THICK_GROW, SpellRender.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 0.6f, 0.25f, 0.9f * shown);
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		// The helm's crest and horns.
		tongues(pose, flames, parent.head, new Vector3f(0, -9.5f, -1), 0.32f, 0.7f, age, shown, 3);
		tongues(pose, flames, parent.head, new Vector3f(-3.5f, -8.5f, -2), 0.22f, 0.45f, age + 5, shown, 2);
		tongues(pose, flames, parent.head, new Vector3f(3.5f, -8.5f, -2), 0.22f, 0.45f, age + 9, shown, 2);
		// Shoulders, forearms, the back, the shins.
		tongues(pose, flames, parent.rightArm, new Vector3f(-1, -2, 0), 0.4f, 0.55f, age + 3, shown, 3);
		tongues(pose, flames, parent.leftArm, new Vector3f(1, -2, 0), 0.4f, 0.55f, age + 11, shown, 3);
		tongues(pose, flames, parent.rightArm, new Vector3f(-1, 7, 0), 0.3f, 0.4f, age + 6, shown, 2);
		tongues(pose, flames, parent.leftArm, new Vector3f(1, 7, 0), 0.3f, 0.4f, age + 14, shown, 2);
		tongues(pose, flames, parent.body, new Vector3f(0, 4, 3), 0.5f, 0.6f, age + 2, shown, 3);
		tongues(pose, flames, parent.rightLeg, new Vector3f(0, 8, -1), 0.3f, 0.4f, age + 8, shown, 2);
		tongues(pose, flames, parent.leftLeg, new Vector3f(0, 8, -1), 0.3f, 0.4f, age + 13, shown, 2);
	}

	/**
	 * Flame Fist: fire wreathing the fist at the end of {@code arm} and running up the forearm, a white-hot glow in it within a wider
	 * one of orange, tongues licking up from it, sparks thrown off it.
	 */
	private void fists(PoseStack pose, MultiBufferSource buffers, ModelPart arm, int side, float age, float shown) {
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		tongues(pose, flames, arm, new Vector3f(side * 0.5f, 9.5f, 0), 0.6f, 0.9f, age, shown, 5);
		tongues(pose, flames, arm, new Vector3f(side * 0.5f, 6, 0), 0.4f, 0.5f, age + 4, shown, 3);
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		Vector3f hand = at(arm, new Vector3f(side * 0.5f, 10, 0));
		crossed(pose, glow, hand, 1.1f, 1.1f, 0.3f, FIRE_MID, 0.55f * shown, 0, 0, 1, 1);
		crossed(pose, glow, hand, 0.45f, 0.45f, 0.3f, FIRE_CORE, 0.9f * shown, 0, 0, 1, 1);
		VertexConsumer sparks = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 4; i++) {
			float t = (age * 0.06f + i / 4f) % 1;
			long s = i * 31L + (long) (age * 0.06f + i / 4f);
			Vector3f spark = new Vector3f(hand.x + (SpellRender.hash(s) - 0.5f) * 0.6f * t, hand.y - t * 0.9f, hand.z + (SpellRender.hash(s + 1) - 0.5f) * 0.6f * t);
			crossed(pose, sparks, spark, 0.12f, 0.12f, 0.3f, FIRE_CORE, shown * (1 - t), 0, 0, 1, 1);
		}
	}

	/**
	 * {@code count} tongues of flame rising (always upward) from the point {@code local} of {@code part} (its pixels), {@code width}
	 * wide and {@code height} tall (blocks), each flickering on its own.
	 */
	private static void tongues(PoseStack pose, VertexConsumer vc, ModelPart part, Vector3f local, float width, float height, float age, float shown, int count) {
		Vector3f at = at(part, local);
		for (int i = 0; i < count; i++) {
			float jitterX = (SpellRender.hash(i * 7L + (long) local.y) - 0.5f) * width * 0.6f;
			float jitterZ = (SpellRender.hash(i * 13L + (long) local.x) - 0.5f) * width * 0.6f;
			float tall = height * (0.75f + 0.35f * Mth.sin(age * 0.4f + i * 2.1f)) * shown;
			float[] fv = SpellRender.frame(age + i * 3, 0.5f, SpellRender.FLAME_FRAMES);
			Vector3f base = new Vector3f(at.x + jitterX, at.y + 0.08f, at.z + jitterZ);
			crossed(pose, vc, base, width * shown, tall, 0, i % 2 == 0 ? FIRE_MID : FIRE_CORE, 0.95f * shown, 0, fv[0], 1, fv[1]);
		}
	}

	/** Where {@code local} (a part's own pixels) is in the model's space (blocks, y down): through the part's pivot and turn. */
	private static Vector3f at(ModelPart part, Vector3f local) {
		Vector3f p = new Vector3f(local).div(16);
		new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot).transform(p);
		return p.add(part.x / 16, part.y / 16, part.z / 16);
	}

	/**
	 * Two upright quads crossed at right angles (three, turned 60 degrees apart, for {@code spread} > 0) standing on {@code base} in
	 * model space (up is -y there), {@code width} wide and {@code height} tall: a flame seen from any side.
	 */
	private static void crossed(PoseStack pose, VertexConsumer vc, Vector3f base, float width, float height, float spread, float[] rgb, float alpha,
								float u0, float v0, float u1, float v1) {
		PoseStack.Pose p = pose.last();
		int planes = 3;
		for (int k = 0; k < planes; k++) {
			double a = Math.PI * k / planes;
			float dx = (float) Math.cos(a) * width / 2, dz = (float) Math.sin(a) * width / 2;
			float yb = base.y + (spread > 0 ? height / 2 : 0), yt = base.y - height + (spread > 0 ? height / 2 : 0);
			SpellRender.vertex(vc, p, base.x - dx, yb, base.z - dz, rgb[0], rgb[1], rgb[2], alpha, u0, v1, SpellRender.FULL_BRIGHT, 0, 0, 1);
			SpellRender.vertex(vc, p, base.x + dx, yb, base.z + dz, rgb[0], rgb[1], rgb[2], alpha, u1, v1, SpellRender.FULL_BRIGHT, 0, 0, 1);
			SpellRender.vertex(vc, p, base.x + dx, yt, base.z + dz, rgb[0], rgb[1], rgb[2], alpha, u1, v0, SpellRender.FULL_BRIGHT, 0, 0, 1);
			SpellRender.vertex(vc, p, base.x - dx, yt, base.z - dz, rgb[0], rgb[1], rgb[2], alpha, u0, v0, SpellRender.FULL_BRIGHT, 0, 0, 1);
		}
	}

	/**
	 * First person: the auras on the player's own arm (PlayerRendererMixin): Flame Fist's fire round the fist, the skins of the
	 * wards (the arm drawn again in them), and Meteor Flight's heat (the user's ask, 2026-10-23: the fire seen in first person too).
	 */
	public static void firstPersonArm(PoseStack pose, MultiBufferSource source, LivingEntity player, ModelPart arm, ModelPart sleeve, int light, boolean right) {
		if (!ClientSpellAuras.any(player) && MeteorFlightClient.heat(player) < 0.02f) return;
		MultiBufferSource buffers = SpellBuffers.begin(source);
		try {
			firstPersonAuras(pose, buffers, player, arm, right);
		} finally {
			SpellBuffers.end();
		}
	}

	private static void firstPersonAuras(PoseStack pose, MultiBufferSource buffers, LivingEntity player, ModelPart arm, boolean right) {
		float partial = net.minecraft.client.Minecraft.getInstance().getFrameTime();
		float age = player.tickCount + partial;
		float coating = ClientSpellAuras.shown(player, SpellAura.AZURE_COATING, partial, 8, 20);
		if (coating > 0) armSkin(pose, buffers, arm, RenderType.energySwirl(SpellRender.WATER, 0, age * 0.01f), new float[]{0.55f, 0.75f, 1.0f}, 0.6f * coating);
		float tempering = ClientSpellAuras.shown(player, SpellAura.CRIMSON_TEMPERING, partial, 10, 30);
		if (tempering > 0) armSkin(pose, buffers, arm, SpellRender.glow(SpellRender.VEINS), new float[]{1.0f, 0.5f, 0.25f}, tempering);
		float hardened = ClientSpellAuras.shown(player, SpellAura.HARDENED_BODY, partial, 10, 20);
		if (hardened > 0) armSkin(pose, buffers, arm, SpellRender.clearLit(SpellRender.STONE_SHELL), new float[]{0.95f, 0.85f, 0.72f}, 0.55f * hardened);
		float vajra = ClientSpellAuras.shown(player, SpellAura.VAJRA_BODY, partial, 10, 20);
		if (vajra > 0) armSkin(pose, buffers, arm, SpellRender.clearLit(SpellRender.GOLD), WHITE, 0.7f * vajra);
		float bark = ClientSpellAuras.shown(player, SpellAura.ANCIENT_BARK_SKIN, partial, 12, 20);
		if (bark > 0) armSkin(pose, buffers, arm, SpellRender.clearLit(SpellRender.BARK), WHITE, 0.92f * bark);
		float fist = Math.max(ClientSpellAuras.shown(player, SpellAura.FLAME_FIST, partial, 6, 20), ClientSpellAuras.shown(player, SpellAura.FLAME_ARMOUR, partial, 10, 20));
		if (fist > 0) {
			VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
			tongues(pose, flames, arm, new Vector3f(right ? -0.5f : 0.5f, 9.5f, 0), 0.4f, 0.55f, age, fist, 4);
			VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
			crossed(pose, glow, at(arm, new Vector3f(right ? -0.5f : 0.5f, 10, 0)), 0.6f, 0.6f, 0.3f, FIRE_MID, 0.6f * fist, 0, 0, 1, 1);
		}
		float heat = MeteorFlightClient.heat(player);
		if (heat > 0.02f) heatSkins(heat, age, (type, outer, rgb, alpha) -> armSkin(pose, buffers, arm, type, rgb, alpha));
	}

	private static void armSkin(PoseStack pose, MultiBufferSource buffers, ModelPart arm, RenderType type, float[] rgb, float alpha) {
		pose.pushPose();
		pose.scale(1.06f, 1.02f, 1.06f);
		arm.render(pose, buffers.getBuffer(type), SpellRender.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, rgb[0], rgb[1], rgb[2], alpha);
		pose.popPose();
	}
}
