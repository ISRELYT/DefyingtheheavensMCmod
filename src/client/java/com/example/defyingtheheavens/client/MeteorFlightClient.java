package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.Bulldozer;
import com.example.defyingtheheavens.MeteorFlight;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.client.mixin.ClientLevelAccessor;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Meteor Flight on the client ({@link MeteorFlight}): the local flier steered where they look while forward is held (from
 * MeteorFlightMixin, in place of Player#travel), at cruise or, sprinting, up to their realm's top speed, held back where the land
 * ahead hasn't come yet; the word to the server whether forward is held; who flies fast and how hot they are, from the server's
 * word (oneself too); the body laid out along the way it goes, a fist out ahead ({@link #layOut}, {@link #pose}); and the sights:
 * a trail of vapour behind one flying fast, and a ring of air where the sound barrier broke. The heat is on the body itself, glowing
 * gold ablaze and red as it cools (SpellAuraLayer#meteorHeat): no flames or glow thrown about it (the user's word, 2026-10-23); in
 * first person on one's arm (SpellAuraLayer#firstPersonArm) and at the screen's edges ({@link #renderHud}).
 * <p>
 * With the boost held (sprint) and going fast, the way ahead is bulldozed ({@link #clearAhead}, Bulldozer): what one would run into
 * is gone on one's own screen before one reaches it, and the server told; skimming the ground (or a ceiling) slides along it at speed
 * rather than losing it; and the ground never ends the flight ({@link #keepAloft}: the user's ask, 2026-10-23).
 */
public final class MeteorFlightClient {
	/** The top speed is reached from a standstill in this many ticks; a lower aim is come down to twice as fast. */
	private static final double ACCEL_TICKS = 30, BRAKE_TICKS = 15;
	/** The way flown turns this share of the way to the look each tick: a little weight at speed. */
	private static final double TURN = 0.45;
	/**
	 * The land must have come this many ticks' flight ahead (looked for every {@link #CHUNK_STEP} blocks)... Few: a server sends land
	 * only so far (its view distance, some 128 blocks), so a long reach would hold back even a flight over land long made (run 1 of
	 * 2026-10-23: 30 ticks kept a True Immortal to a third of its speed).
	 */
	private static final int AHEAD_TICKS = 8;
	private static final double CHUNK_STEP = 8;
	/** ...or the flier is held back to reach its edge no sooner, and creeps at least this fast (blocks a tick). */
	private static final double EDGE_SPEED = 0.25;
	/** Shown laid out only faster than this (blocks a tick): not while just setting off. */
	private static final double LAID_OUT_FROM = 0.3;
	/** The server's word about another player is forgotten after this long without another. */
	private static final long EXPIRY_MS = 1500;
	/** How far off the sights are drawn. */
	private static final double DRAW_RANGE = 192;
	private static final int TRAIL_TICKS = 16, RING_TICKS = 16;
	private static final float[] VAPOUR = {0.85f, 0.92f, 1.0f}, RING = {0.92f, 0.96f, 1.0f};

	// --- The local flier ---

	/** Blocks a tick along the way flown, and that way. */
	private static double speed;
	private static Vec3 heading;
	/** Forward held in Meteor Flight this tick; the boost held down with it (sprinting); and what the server was last told of each. */
	private static boolean steering, flatOut, told, toldFlat;
	/** Whether the server's gamerule lets the bulldozer clear the way (its word, MeteorFlight#sync; nothing cleared until it says). */
	private static boolean smashes;

	/** Whether the local player flies by Meteor Flight now (as MeteorFlight#mayStreak on the server). */
	public static boolean active(LocalPlayer p) {
		PlayerCultivation c = ClientCultivationData.get();
		return c.isAbilityActive(Ability.METEOR_FLIGHT) && p.getAbilities().flying && !p.isCreative() && !p.isSpectator() && !p.isPassenger()
				&& !SwordFlightClient.isRiding(p) && !ClientSoul.isOut() && MeteorFlight.flownIn(p.level().dimension());
	}

	/**
	 * The local player's flight this tick (MeteorFlightMixin, at the head of Player#travel): with forward held in Meteor Flight,
	 * along the line of sight, free of gravity, sped up toward cruise or (sprinting) the top speed and held back where the land
	 * ahead hasn't come; let go, vanilla's flight takes it from there, the momentum and all. @return whether it flew here
	 */
	public static boolean travel(LocalPlayer player) {
		steering = active(player) && player.input.forwardImpulse > 0;
		if (!steering) {
			speed = player.getDeltaMovement().length();
			heading = null;
			flatOut = false;
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		PlayerCultivation c = ClientCultivationData.get();
		double top = MeteorFlight.topPerTick(c);
		boolean sprinting = mc.options.keySprint.isDown();
		double aim = sprinting ? top : MeteorFlight.cruisePerTick(c);
		Vec3 look = player.getViewVector(1.0f);
		Vec3 turned = heading == null ? look : heading.lerp(look, TURN);
		heading = turned.lengthSqr() < 1.0e-6 ? look : turned.normalize();
		double land = landAhead(player, heading, aim);
		aim = Math.min(aim, land);
		speed = speed < aim ? Math.min(aim, speed + top / ACCEL_TICKS) : Math.max(aim, speed - top / BRAKE_TICKS);
		speed = Math.min(speed, land); // never on into land that hasn't come, braking or not
		Vec3 v = heading.scale(speed);
		// Never into the void below the world.
		if (player.getY() + v.y < player.level().getMinBuildHeight() - 2 && v.y < 0) v = new Vec3(v.x, 0, v.z);
		// Fast with the boost held: the way ahead bulldozed before it's flown.
		boolean dozing = sprinting && smashes && speed >= Bulldozer.FROM_SPEED;
		if (dozing && v.lengthSqr() > 1.0e-6) clearAhead(player, v.normalize(), speed);
		Vec3 before = player.position();
		player.setDeltaMovement(v);
		player.move(MoverType.SELF, v);
		double went = player.position().distanceTo(before);
		// Met the ground (or a ceiling) and nothing ahead: the rest of the way along it, the speed kept, not lost to it.
		if (went < speed * 0.9 && player.verticalCollision && !player.horizontalCollision) {
			Vec3 along = new Vec3(v.x, 0, v.z);
			if (along.lengthSqr() > 1.0e-6) {
				along = along.normalize();
				double rest = speed - went;
				if (dozing) clearAhead(player, along, rest);
				Vec3 mid = player.position();
				player.move(MoverType.SELF, along.scale(rest));
				went += player.position().distanceTo(mid);
			}
		}
		// Stopped short (what it can't go through, or what the bulldozer may not take): the speed is what it went.
		if (went < speed * 0.9) speed = went;
		// The boost held down (all the heat asks of a flier the land holds back, at MeteorFlight#HEAT_FLOOR or more).
		flatOut = sprinting;
		player.setDeltaMovement(heading.scale(speed));
		player.fallDistance = 0;
		keepAloft(player);
		player.calculateEntityAnimation(false);
		return true;
	}

	/**
	 * (At the end of the flight's tick, MeteorFlightMixin.) In Meteor Flight with the boost held, on the ground or not, not taken for
	 * standing on it: LocalPlayer#aiStep ends one's flight the moment one touches the ground, and swooping low it set one walking
	 * (the user's ask, 2026-10-23). Let go of the boost to land.
	 */
	public static void keepAloft(LocalPlayer player) {
		if (player.onGround() && active(player) && Minecraft.getInstance().options.keySprint.isDown()) player.setOnGround(false);
	}

	/**
	 * The bulldozer (Bulldozer): what stands in the way of the body flown {@code dir} (a unit vector) at {@code speed}, out to
	 * Bulldozer#reach, cleared on this client at once (as a block one mines is, through vanilla's block prediction), a few crumbling,
	 * and named to the server (C2S meteor_doze), which takes it too and acknowledges: whatever it won't take comes back.
	 */
	private static void clearAhead(LocalPlayer player, Vec3 dir, double speed) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) return;
		List<BlockPos> cleared = Bulldozer.sweep(level, player, player.getBoundingBox(), dir, Bulldozer.reach(speed), player.getY(), Bulldozer.MOST_A_TICK);
		if (cleared.isEmpty()) return;
		int sequence = 0;
		int shown = 0;
		try (BlockStatePredictionHandler predicting = ((ClientLevelAccessor) level).dth$getPredictions().startPredicting()) {
			sequence = predicting.currentSequence();
			for (BlockPos pos : cleared) {
				BlockState state = level.getBlockState(pos);
				if (shown < Bulldozer.SHOWN_A_TICK && (shown == 0 || level.random.nextInt(3) == 0)) {
					shown++;
					level.levelEvent(player, 2001, pos, Block.getId(state)); // its sound and pieces, here at once
				}
				level.setBlock(pos, state.getFluidState().createLegacyBlock(), 11); // as vanilla clears a block one mines
			}
		}
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(sequence);
		buf.writeVarInt(cleared.size());
		for (BlockPos pos : cleared) buf.writeLong(pos.asLong());
		ClientPlayNetworking.send(ModPackets.METEOR_DOZE, buf);
	}

	/**
	 * The fastest {@code p} may go along {@code dir} (blocks a tick) to reach no sooner than {@link #AHEAD_TICKS} ticks on the
	 * edge of the land this client has, or {@code aim} if it reaches that far.
	 */
	private static double landAhead(LocalPlayer p, Vec3 dir, double aim) {
		double reach = aim * AHEAD_TICKS;
		for (double d = CHUNK_STEP; d <= reach; d += CHUNK_STEP) {
			Vec3 at = p.position().add(dir.scale(d));
			if (!p.level().getChunkSource().hasChunk(SectionPos.blockToSectionCoord(at.x), SectionPos.blockToSectionCoord(at.z))) {
				return Math.max(EDGE_SPEED, (d - CHUNK_STEP) / AHEAD_TICKS);
			}
		}
		return aim;
	}

	// --- Who flies fast, and how hot ---

	private record Seen(boolean streaking, float heat, long at) {}

	private static final Map<Integer, Seen> SEEN = new ConcurrentHashMap<>();
	/** Heat as drawn, by entity id, eased toward the server's word. */
	private static final Map<Integer, Float> SHOWN = new HashMap<>();

	/** The server's word (S2C meteor_state) about a player: flying fast or not, how hot, and (for oneself) if the way may be cleared. */
	public static void set(int entityId, boolean streaking, float heat, boolean smashes) {
		SEEN.put(entityId, new Seen(streaking, heat, System.currentTimeMillis()));
		LocalPlayer self = Minecraft.getInstance().player;
		if (self != null && self.getId() == entityId) MeteorFlightClient.smashes = smashes;
	}

	/** Whether {@code entity} flies fast now: the local player by its own steering, others by the server's word. */
	public static boolean isStreaking(Entity entity) {
		if (!(entity instanceof Player player) || !player.isAlive()) return false;
		if (player == Minecraft.getInstance().player) return steering && speed > LAID_OUT_FROM;
		Seen seen = SEEN.get(player.getId());
		return seen != null && seen.streaking() && System.currentTimeMillis() - seen.at() <= EXPIRY_MS;
	}

	/** How hot {@code entity} looks, 0 to 1. */
	public static float heat(Entity entity) {
		Float shown = SHOWN.get(entity.getId());
		return shown == null ? 0 : shown;
	}

	// --- The body laid out ---

	/**
	 * (LivingEntityRendererMixin, at the tail of setupRotations.) The body turned to the way the flier looks, then laid along it,
	 * head first, about its own middle (so it stays where its box is): flat flying level, head down diving, upright climbing.
	 */
	public static void layOut(LivingEntity entity, PoseStack pose, float partial) {
		float pitch = Mth.lerp(partial, entity.xRotO, entity.getXRot());
		float half = entity.getBbHeight() * 0.5f;
		// By the look's yaw (sent with every move), not the head's: vanilla tells one who starts watching a player its head only once
		// it turns, so a flier holding its course was seen lying the wrong way (2026-10-23, run 3).
		float yaw = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
		float body = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
		pose.mulPose(Axis.YP.rotationDegrees(Mth.wrapDegrees(body - yaw)));
		pose.translate(0, half, 0);
		pose.mulPose(Axis.XP.rotationDegrees(-90.0f - pitch));
		pose.translate(0, -half, 0);
	}

	/** (HumanoidModelMixin.) Laid out: the head raised to look ahead, the right fist out in front, the left arm and the legs trailing. */
	public static void pose(HumanoidModel<?> m, float ageInTicks) {
		m.head.xRot = -1.05f;
		m.head.yRot = 0;
		m.head.zRot = 0;
		m.hat.copyFrom(m.head);
		m.body.xRot = 0;
		m.rightArm.xRot = -(float) Math.PI + 0.12f;
		m.rightArm.yRot = 0;
		m.rightArm.zRot = 0.1f;
		m.leftArm.xRot = 0.15f;
		m.leftArm.yRot = 0;
		m.leftArm.zRot = -0.1f;
		float flutter = Mth.sin(ageInTicks * 0.6f) * 0.06f;
		m.rightLeg.xRot = 0.05f + flutter;
		m.leftLeg.xRot = 0.05f - flutter;
		m.rightLeg.yRot = 0;
		m.leftLeg.yRot = 0;
		m.rightLeg.zRot = 0.03f;
		m.leftLeg.zRot = -0.03f;
	}

	// --- Sights ---

	/** A point of a trail: where the body's middle was, and when. */
	private record Point(Vec3 at, long tick) {}

	private static final Map<Integer, Deque<Point>> TRAILS = new HashMap<>();
	/** Where each player's middle was last tick (for how fast others go). */
	private static final Map<Integer, Vec3> LAST = new HashMap<>();

	private record Boom(Vec3 at, Vec3 dir, long born) {}

	private static final List<Boom> BOOMS = new ArrayList<>();

	/** The server's word (S2C meteor_boom): the sound barrier broke at {@code at}, going {@code dir}. */
	public static void boom(Vec3 at, Vec3 dir) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		BOOMS.add(new Boom(at, dir, mc.level.getGameTime()));
		// A burst of air round the way it went.
		Vec3[] basis = basis(dir);
		for (int i = 0; i < 20; i++) {
			double a = i * Math.PI * 2 / 20;
			Vec3 out = basis[0].scale(Math.cos(a)).add(basis[1].scale(Math.sin(a)));
			mc.level.addParticle(ParticleTypes.CLOUD, at.x + out.x * 0.8, at.y + out.y * 0.8, at.z + out.z * 0.8, out.x * 0.45, out.y * 0.45, out.z * 0.45);
		}
	}

	/** Each client tick: the word to the server, the heat eased, the trails laid and let fade, and the rings aged. */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) return;
		boolean want = steering && active(mc.player), flat = want && flatOut;
		if (want != told || flat != toldFlat) {
			told = want;
			toldFlat = flat;
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeBoolean(want);
			buf.writeBoolean(flat);
			ClientPlayNetworking.send(ModPackets.METEOR_STREAK, buf);
		}
		if (mc.isPaused()) return;
		long now = mc.level.getGameTime();
		long ms = System.currentTimeMillis();
		for (Player player : mc.level.players()) {
			int id = player.getId();
			Seen seen = SEEN.get(id);
			float target = seen != null && ms - seen.at() <= EXPIRY_MS ? seen.heat() : 0;
			float shown = SHOWN.getOrDefault(id, 0f);
			shown += (target - shown) * 0.2f;
			if (target == 0 && shown < 0.005f) SHOWN.remove(id);
			else SHOWN.put(id, shown);
			Vec3 middle = player.position().add(0, player.getBbHeight() * 0.5, 0);
			Vec3 before = LAST.put(id, middle);
			double going = before == null ? 0 : middle.distanceTo(before);
			// A trail of vapour behind one flying faster than Qi Flight ever goes.
			if (isStreaking(player) && going > MeteorFlight.NORMAL_SPRINT && going < 64) {
				TRAILS.computeIfAbsent(id, k -> new ArrayDeque<>()).addFirst(new Point(middle, now));
			}
		}
		for (Iterator<Map.Entry<Integer, Deque<Point>>> it = TRAILS.entrySet().iterator(); it.hasNext(); ) {
			Deque<Point> trail = it.next().getValue();
			while (!trail.isEmpty() && now - trail.peekLast().tick() > TRAIL_TICKS) trail.removeLast();
			if (trail.isEmpty()) it.remove();
		}
		BOOMS.removeIf(b -> now - b.born() > RING_TICKS);
		LAST.keySet().removeIf(id -> mc.level.getEntity(id) == null);
	}

	/** WorldRenderEvents.AFTER_TRANSLUCENT: the trails and the rings, in added light. */
	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || TRAILS.isEmpty() && BOOMS.isEmpty()) return;
		Vec3 cam = context.camera().getPosition();
		float partial = context.tickDelta();
		float now = mc.level.getGameTime() + partial;
		Matrix4f matrix = context.matrixStack().last().pose();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (Map.Entry<Integer, Deque<Point>> entry : TRAILS.entrySet()) {
			Entity entity = mc.level.getEntity(entry.getKey());
			List<Vec3> points = new ArrayList<>();
			List<Float> ages = new ArrayList<>();
			if (entity != null) {
				points.add(entity.getPosition(partial).add(0, entity.getBbHeight() * 0.5, 0));
				ages.add(0f);
			}
			for (Point point : entry.getValue()) {
				points.add(point.at());
				ages.add(Math.max(0, now - point.tick()));
			}
			if (points.size() < 2 || points.get(0).distanceToSqr(cam) > DRAW_RANGE * DRAW_RANGE) continue;
			ribbon(buffer, matrix, cam, points, ages, 0.9, VAPOUR, 0.3f);
		}
		for (Boom boom : BOOMS) {
			float age = now - boom.born();
			if (age < 0 || age > RING_TICKS || boom.at().distanceToSqr(cam) > DRAW_RANGE * DRAW_RANGE) continue;
			float life = 1 - age / RING_TICKS;
			double radius = 0.8 + age * 0.55;
			ring(buffer, matrix, boom.at().subtract(cam), boom.dir(), radius, 0.5 + age * 0.08, RING, (float) Math.pow(life, 1.5) * 0.7f);
		}
		BufferBuilder.RenderedBuffer rendered = buffer.endOrDiscardIfEmpty();
		if (rendered != null) BufferUploader.drawWithShader(rendered);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/** The heat's colour: yellow-white ablaze, orange as it falls, red as the body cools (SpellAuraLayer#meteorHeat). */
	public static float[] heatColour(float heat) {
		if (heat >= 0.75f) return lerp((heat - 0.75f) / 0.25f, new float[] {1.0f, 0.75f, 0.3f}, new float[] {1.0f, 0.95f, 0.65f});
		if (heat >= MeteorFlight.ABLAZE) return lerp((heat - 0.5f) / 0.25f, new float[] {1.0f, 0.45f, 0.12f}, new float[] {1.0f, 0.75f, 0.3f});
		return lerp(heat / 0.5f, new float[] {0.85f, 0.12f, 0.05f}, new float[] {1.0f, 0.45f, 0.12f});
	}

	/** How far the flames have caught at {@code heat}, 0 to 1 (about MeteorFlight#ABLAZE). */
	public static float ablaze(float heat) {
		return Mth.clamp((heat - 0.4f) / 0.2f, 0, 1);
	}

	/** How strongly the body glows as it cools at {@code heat}, 0 to 1 (whole from MeteorFlight#ABLAZE up). */
	public static float cooling(float heat) {
		return Math.min(1, heat / MeteorFlight.ABLAZE);
	}

	// --- In first person ---

	/** The hotbar and what sits over it, this far either side of the bottom's middle (GUI pixels): the fire lies lowest there. */
	private static final float HOTBAR_CLEAR = 125;
	/** How deep the fire may go (of its deepest, at the bottom's corners): under the hotbar, at the top's corners, over its middle. */
	private static final float BOTTOM_LOW = 0.3f, TOP_CORNER = 0.55f, TOP_MIDDLE = 0.35f;
	/** Points of the fire's front this far apart along the edge (GUI pixels), and this many round each corner. */
	private static final float FRONT_STEP = 3;
	private static final int CORNER_POINTS = 8;
	/** The fire from the edge (0) to its tips (1): how far in each band is, and its colour there; then its heart, in added light. */
	private static final float[] BANDS = {0, 0.3f, 0.65f, 1};
	private static final float[][] BAND_RGBA = {{1.0f, 0.92f, 0.6f, 0.95f}, {1.0f, 0.68f, 0.18f, 0.9f}, {1.0f, 0.36f, 0.06f, 0.6f}, {0.75f, 0.1f, 0.02f, 0}};
	private static final float[] HEART_BANDS = {0, 0.25f, 0.55f};
	private static final float[][] HEART_RGBA = {{1.0f, 0.9f, 0.6f, 0.4f}, {1.0f, 0.7f, 0.3f, 0.2f}, {1.0f, 0.5f, 0.2f, 0}};

	/**
	 * (GuiMixin, before the rest of the HUD, which is drawn over it.) One's own heat round the screen, in first person (the user's
	 * asks, 2026-10-23: the fire seen there too, all the way round, flowing, not rows of little flames alike crossing at the corners):
	 * ablaze, one front of fire round the whole edge, its flames each their own height and shifting, rising from the bottom's middle
	 * up both sides and over the top, deepest at the bottom's corners and lowest under the hotbar, over a glow of their colour; as it
	 * cools the fire dies down to the edge, which glows red, fading.
	 */
	public static void renderHud(GuiGraphics graphics, float tickDelta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson() || mc.getCameraEntity() != mc.player) return;
		float heat = heat(mc.player);
		if (heat < 0.02f) return;
		int width = graphics.guiWidth(), height = graphics.guiHeight();
		float time = mc.player.tickCount + tickDelta;
		// Behind all the HUD draws after it: the hotbar is drawn back at -90, and the glow at 0 hid it (run 13 of 2026-10-23).
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, -200);
		// No hotter than orange: yellow-white is lost against the sky (run 11 of 2026-10-23).
		float[] rgb = heatColour(Math.min(heat, 0.6f));
		float ablaze = ablaze(heat);
		float glow = 0.5f * ablaze + 0.85f * cooling(heat) * (1 - ablaze);
		int colour = (int) (rgb[0] * 255) << 16 | (int) (rgb[1] * 255) << 8 | (int) (rgb[2] * 255);
		float pulse = 0.85f + 0.15f * Mth.sin(time * 0.4f);
		int depth = Math.max(16, Math.min(width, height) / 6);
		for (int i = 0; i < 12; i++) {
			int alpha = (int) (130 * glow * pulse * (1 - i / 12f) * (1 - i / 12f));
			if (alpha <= 0) continue;
			int argb = alpha << 24 | colour;
			int step = depth * i / 12, next = depth * (i + 1) / 12;
			graphics.fill(0, step, width, next, argb);
			graphics.fill(0, height - next, width, height - step, argb);
			graphics.fill(step, next, next, height - next, argb);
			graphics.fill(width - next, next, width - step, height - next, argb);
		}
		if (ablaze > 0) fire(graphics, width, height, time, ablaze);
		graphics.pose().popPose();
	}

	/** The fire round the edge, {@code fade} of its full strength (lower as it dies down). */
	private static void fire(GuiGraphics graphics, int width, int height, float time, float fade) {
		Front front = new Front(width, height, Math.min(width, height) * 0.24f * (0.35f + 0.65f * fade));
		int n = front.points.size();
		float whole = front.u, blend = front.deepest * 2;
		float[] depth = new float[n], flicker = new float[n];
		for (int i = 0; i < n; i++) {
			float[] p = front.points.get(i);
			float u = p[5];
			// Each half its own fire, flowing from the bottom's middle; the two meet over the top's.
			float right = smooth((u - (whole / 2 - blend)) / (2 * blend));
			depth[i] = front.deepest * p[4] * Mth.lerp(right, flame(u, time, 0), flame(whole - u, time, 57.3f));
			flicker[i] = 0.8f + 0.2f * noise(u * 0.2f, time * 0.15f);
		}
		graphics.flush();
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Matrix4f m = graphics.pose().last().pose();
		RenderSystem.defaultBlendFunc();
		bands(m, front.points, depth, flicker, BANDS, BAND_RGBA, fade);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		bands(m, front.points, depth, flicker, HEART_BANDS, HEART_RGBA, fade);
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/** The fire's bands from each point of the front to the next: from {@code at[b]} of the flame's depth in to {@code at[b + 1]}. */
	private static void bands(Matrix4f m, List<float[]> points, float[] depth, float[] flicker, float[] at, float[][] rgba, float fade) {
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i + 1 < points.size(); i++) {
			for (int b = 0; b + 1 < at.length; b++) {
				bandVertex(buffer, m, points.get(i), depth[i], at, rgba, b, fade * (b == 0 ? 1 : flicker[i]));
				bandVertex(buffer, m, points.get(i + 1), depth[i + 1], at, rgba, b, fade * (b == 0 ? 1 : flicker[i + 1]));
				bandVertex(buffer, m, points.get(i + 1), depth[i + 1], at, rgba, b + 1, fade * flicker[i + 1]);
				bandVertex(buffer, m, points.get(i), depth[i], at, rgba, b + 1, fade * flicker[i]);
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static void bandVertex(BufferBuilder buffer, Matrix4f m, float[] p, float depth, float[] at, float[][] rgba, int band, float alpha) {
		float in = depth * at[band];
		float[] c = rgba[band];
		buffer.vertex(m, p[0] + p[2] * in, p[1] + p[3] * in, 0).color(c[0], c[1], c[2], c[3] * alpha).endVertex();
	}

	/**
	 * The fire's height at {@code u} along the way round (0 to about 1.2): broad licks flowing along it and changing as they go,
	 * sharp tongues among them.
	 */
	private static float flame(float u, float time, float seed) {
		float body = Mth.clamp((noise((u - time * 1.8f) * 0.04f + seed, time * 0.03f) - 0.2f) / 0.6f, 0, 1);
		body = body * body * (3 - 2 * body);
		float tongue = 1 - Math.abs(noise((u - time * 3.2f) * 0.13f + seed * 2, time * 0.08f) * 2 - 1);
		tongue *= tongue * tongue;
		return 0.22f + 0.6f * body + 0.35f * tongue * (0.35f + 0.65f * body);
	}

	/** Smooth noise, 0 to 1, a new value at each whole {@code x} and {@code y}. */
	private static float noise(float x, float y) {
		int x0 = Mth.floor(x), y0 = Mth.floor(y);
		float fx = smooth(x - x0), fy = smooth(y - y0);
		return Mth.lerp(fy, Mth.lerp(fx, hash(x0, y0), hash(x0 + 1, y0)), Mth.lerp(fx, hash(x0, y0 + 1), hash(x0 + 1, y0 + 1)));
	}

	private static float hash(int x, int y) {
		int h = x * 374761393 + y * 668265263;
		h = (h ^ h >>> 13) * 1274126177;
		return ((h ^ h >>> 16) & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	/**
	 * The way round the screen's edge, from the bottom's middle left, up, over, down and home. Each point: where it is on the edge,
	 * the way in from there (swept round the corners, so the fire's tips round them on a quarter circle), how deep its fire may go
	 * (of the deepest), and how far round it is.
	 */
	private static final class Front {
		final List<float[]> points = new ArrayList<>();
		final float deepest;
		float u, lastX, lastY;

		Front(int width, int height, float deepest) {
			this.deepest = deepest;
			float w = width, h = height, mid = w / 2, clear = Math.min(HOTBAR_CLEAR, mid * 0.6f);
			for (float x = mid; x > 0; x -= FRONT_STEP) edge(x, h, 0, -1, bottom(x, mid, clear));
			turn(0, h, -90, 0, 1);
			for (float y = h; y > 0; y -= FRONT_STEP) edge(0, y, 1, 0, side(y, h));
			turn(0, 0, 0, 90, TOP_CORNER);
			for (float x = 0; x < w; x += FRONT_STEP) edge(x, 0, 0, 1, top(x, mid));
			turn(w, 0, 90, 180, TOP_CORNER);
			for (float y = 0; y < h; y += FRONT_STEP) edge(w, y, -1, 0, side(y, h));
			turn(w, h, 180, 270, 1);
			for (float x = w; x > mid; x -= FRONT_STEP) edge(x, h, 0, -1, bottom(x, mid, clear));
			edge(mid, h, 0, -1, BOTTOM_LOW);
		}

		private void edge(float x, float y, float nx, float ny, float reach) {
			if (!points.isEmpty()) u += (float) Math.hypot(x - lastX, y - lastY);
			points.add(new float[] {x, y, nx, ny, reach, u});
			lastX = x;
			lastY = y;
		}

		/** Round a corner at (x, y): the way in swept from {@code from} to {@code to} degrees. */
		private void turn(float x, float y, float from, float to, float reach) {
			float arc = deepest * reach * (float) (Math.PI / 2) / CORNER_POINTS;
			for (int i = 0; i < CORNER_POINTS; i++) {
				if (i == 0) u += (float) Math.hypot(x - lastX, y - lastY);
				else u += arc;
				double a = Math.toRadians(from + (to - from) * i / CORNER_POINTS);
				points.add(new float[] {x, y, (float) Math.cos(a), (float) Math.sin(a), reach, u});
			}
			u += arc;
			lastX = x;
			lastY = y;
		}

		/** Along the bottom: lowest under the hotbar, deepest at the corners. */
		private static float bottom(float x, float mid, float clear) {
			float off = Math.abs(x - mid);
			if (off <= clear) return BOTTOM_LOW;
			return BOTTOM_LOW + (1 - BOTTOM_LOW) * (float) Math.pow((off - clear) / Math.max(1, mid - clear), 0.7);
		}

		/** Up the sides: deeper toward the bottom. */
		private static float side(float y, float h) {
			return TOP_CORNER + (1 - TOP_CORNER) * (float) Math.pow(y / h, 1.3);
		}

		/** Along the top: thinner, least over its middle. */
		private static float top(float x, float mid) {
			float e = Math.abs(x - mid) / mid;
			return TOP_MIDDLE + (TOP_CORNER - TOP_MIDDLE) * e * e;
		}
	}

	private static float[] lerp(float t, float[] a, float[] b) {
		return new float[] {Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2])};
	}

	/** Two directions square to {@code dir} and to each other. */
	private static Vec3[] basis(Vec3 dir) {
		Vec3 u = dir.cross(Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
		u = u.lengthSqr() < 1.0e-8 ? new Vec3(1, 0, 0) : u.normalize();
		return new Vec3[] {u, dir.cross(u).normalize()};
	}

	/** A ring of air square to {@code dir} about {@code at} (camera-relative), {@code width} across, brightest at {@code radius}. */
	private static void ring(BufferBuilder buffer, Matrix4f matrix, Vec3 at, Vec3 dir, double radius, double width, float[] rgb, float alpha) {
		Vec3[] b = basis(dir);
		int sides = 40;
		double[] radii = {radius - width / 2, radius, radius + width / 2};
		float[] alphas = {0, alpha, 0};
		for (int i = 0; i < sides; i++) {
			double a0 = i * Math.PI * 2 / sides, a1 = (i + 1) * Math.PI * 2 / sides;
			Vec3 d0 = b[0].scale(Math.cos(a0)).add(b[1].scale(Math.sin(a0))), d1 = b[0].scale(Math.cos(a1)).add(b[1].scale(Math.sin(a1)));
			for (int band = 0; band < 2; band++) {
				Vec3 in0 = at.add(d0.scale(radii[band])), in1 = at.add(d1.scale(radii[band]));
				Vec3 out0 = at.add(d0.scale(radii[band + 1])), out1 = at.add(d1.scale(radii[band + 1]));
				vertex(buffer, matrix, in0, rgb, alphas[band]);
				vertex(buffer, matrix, out0, rgb, alphas[band + 1]);
				vertex(buffer, matrix, out1, rgb, alphas[band + 1]);
				vertex(buffer, matrix, in0, rgb, alphas[band]);
				vertex(buffer, matrix, out1, rgb, alphas[band + 1]);
				vertex(buffer, matrix, in1, rgb, alphas[band]);
			}
		}
	}

	/** A ribbon through the points turned to the camera, {@code width} at its head, narrowing and fading with age. */
	private static void ribbon(BufferBuilder buffer, Matrix4f matrix, Vec3 cam, List<Vec3> points, List<Float> ages, double width, float[] rgb,
							   float brightness) {
		int n = points.size();
		Vec3[] left = new Vec3[n], right = new Vec3[n];
		float[] alpha = new float[n];
		for (int i = 0; i < n; i++) {
			Vec3 p = points.get(i);
			Vec3 along = points.get(Math.max(0, i - 1)).subtract(points.get(Math.min(n - 1, i + 1)));
			Vec3 side = along.cross(p.subtract(cam));
			if (side.lengthSqr() < 1.0e-8) side = new Vec3(0, 1, 0);
			float life = 1.0f - Math.min(1.0f, ages.get(i) / TRAIL_TICKS);
			side = side.normalize().scale(width * life / 2);
			left[i] = p.add(side).subtract(cam);
			right[i] = p.subtract(side).subtract(cam);
			alpha[i] = life * life * brightness;
		}
		for (int i = 0; i < n - 1; i++) {
			vertex(buffer, matrix, left[i], rgb, alpha[i]);
			vertex(buffer, matrix, right[i], rgb, alpha[i]);
			vertex(buffer, matrix, right[i + 1], rgb, alpha[i + 1]);
			vertex(buffer, matrix, left[i], rgb, alpha[i]);
			vertex(buffer, matrix, right[i + 1], rgb, alpha[i + 1]);
			vertex(buffer, matrix, left[i + 1], rgb, alpha[i + 1]);
		}
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 at, float[] rgb, float alpha) {
		buffer.vertex(matrix, (float) at.x, (float) at.y, (float) at.z).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
	}

	/** Disconnect. */
	public static void clear() {
		SEEN.clear();
		SHOWN.clear();
		TRAILS.clear();
		LAST.clear();
		BOOMS.clear();
		speed = 0;
		heading = null;
		steering = flatOut = told = toldFlat = smashes = false;
	}

	private MeteorFlightClient() {}
}
