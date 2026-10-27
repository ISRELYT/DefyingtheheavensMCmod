package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.GhostQi;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.QiElement;
import com.example.defyingtheheavens.client.mixin.PostChainAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The client half of {@link com.example.defyingtheheavens.QiSense}. While the ability is on, the world loses its colour (a
 * desaturating post effect, faded in and out) and qi drifts through it as small glowing motes in the colours of the five
 * elements, mixed by the land they gather in ({@link QiElement#pick}), as thick as the place's qi ({@link #airQi}): sparse in the
 * lower realms, ten times as thick in the Upper Realm, a hundred times in the Great Void, a thousand in the Immortal Realm, thin in
 * space; and over the Great Void's battlefields the cyan yin of their ghostly qi besides, as thick as it ({@link #yinAir}). Around
 * any meditating cultivator, motes are drawn in from all sides, faster the stronger their gathering.
 * <p>
 * The motes are not vanilla particles: those are drawn into the world, which the post effect then greys. They live here
 * and are drawn on {@link SenseOverlay}, laid over the frame after the post effect, so they keep their colour.
 */
public final class QiSenseClientHandler {
	private static final ResourceLocation MONOCHROME = DefyingTheHeavens.id("shaders/post/qi_sense.json");
	/** Seconds the colour takes to drain away or come back. */
	private static final float FADE_SECONDS = 0.8f;
	/** Saturation and brightness of the plane of Qi: grey, a little dimmed, so the qi stands out. */
	private static final float MONO_SATURATION = 0.0f;
	private static final float MONO_BRIGHTNESS = 0.85f;

	/** Ambient motes born per tick around the viewer where qi is as thick as the lower realms' (and the Spatial Gap's), times {@link #airQi}. */
	private static final float AMBIENT_PER_TICK = 0.5f;
	/** The Netherworld is as thick with its ghostly qi as the Upper Realm is with qi ({@link PlayerCultivation#UPPER_REALM_QI_MULTIPLIER}). */
	private static final double NETHERWORLD_AIR = PlayerCultivation.UPPER_REALM_QI_MULTIPLIER;
	private static final double SPAWN_RADIUS = 20;
	/** Motes left this far behind (the viewer flew off) are dropped. */
	private static final double KEEP_RADIUS = 30;
	/**
	 * Room for the Immortal Realm's thousandfold air (some 45,000 motes about the viewer, the Great Void's hundredfold some 4,500: the
	 * user's ask, 2026-10-27, "make sure that qi motes are scaled accordingly in the immortal realm") and meditators drawing it in.
	 */
	private static final int MAX_MOTES = 50_000;
	/** How long an ambient mote lives on average (ticks): with the rate, how many the air holds. */
	private static final int AMBIENT_LIFE = 90;
	/**
	 * Where the air holds more than this many (more than the Great Void's), those further than {@link #LOD_NEAR} from the eye are drawn
	 * as a soft dot of their colour {@link #FAR_DOT} times their size, with no halo of their own: the thousands of the Immortal Realm's
	 * air cost no more to draw than the Great Void's did. Nearer, and anywhere thinner, each is drawn whole.
	 */
	private static final int LOD_FROM = 6000;
	private static final double LOD_NEAR = 9;
	private static final float FAR_DOT = 1.8f;
	/** Far motes further off the line of sight than this (its cosine, squared) are out of view: skipped before any other test. */
	private static final float FAR_CONE = 0.067f;
	/** Outside the Upper Realm (lower realms and the Spatial Gap), a meditator draws in this share of the motes. */
	private static final float LOWER_REALM_INFLOW = 0.5f;
	/** Meditators further than this from the viewer aren't drawn absorbing. */
	private static final double ABSORPTION_RANGE = 48;
	/** Absorption info lapses after this long without the server's heartbeat (sent every second while meditating). */
	private static final long ABSORPTION_EXPIRY_MS = 2500;
	/** Motes are born this far from a meditator, then drawn in. */
	private static final double INFLOW_MIN = 4.5;
	private static final double INFLOW_MAX = 7.0;
	/**
	 * A meditator draws in motes by how fast it gathers ({@link #inflow}): one more for every doubling of it in this many (some 0.8 a
	 * realm), and past {@link #INFLOW_KNEE} (Four Axis's, in the Upper Realm) {@link #INFLOW_PAST_KNEE} times as many again, up to
	 * {@link #INFLOW_CAP} a tick.
	 */
	private static final double INFLOW_PER_DOUBLING = 0.35;
	/**
	 * The old ceiling, 6 a tick, which Dao Integration reached so that every realm after it drew in as many (the user's ask, 2026-10-22:
	 * "raise the qi particle cap so Dao Integration through True Immortal keep drawing visibly more"): past it each realm draws in some
	 * 1.6 a tick more, Dao Integration Early about 7, Void Shattering 9, Sacred Vessel 10.5, Entering Nirvana 12, True Immortal 14.
	 */
	private static final double INFLOW_KNEE = 6, INFLOW_PAST_KNEE = 2, INFLOW_CAP = 16;
	/** Corners of each mote's disc. */
	private static final int GLOW_SEGMENTS = 8;
	/** The glow around a mote: this many times its size, this bright at the middle. */
	private static final float HALO_SCALE = 2.6f;
	private static final float HALO_ALPHA = 0.45f;

	private static final class Mote {
		final QiElement element;
		final float size;
		final float phase;
		final int life;
		/** For inflow motes: whose meditation draws this one in. */
		final UUID drawnTo;
		/** For inflow motes drawn into an NPC cultivator: its entity id (-1 for none). */
		int drawnToEntity = -1;
		double x, y, z, prevX, prevY, prevZ;
		double vx, vy, vz;
		int age;

		Mote(QiElement element, double x, double y, double z, float size, int life, UUID drawnTo, RandomSource random) {
			this.element = element;
			this.x = prevX = x;
			this.y = prevY = y;
			this.z = prevZ = z;
			this.size = size;
			this.life = life;
			this.drawnTo = drawnTo;
			this.phase = random.nextFloat() * Mth.TWO_PI;
			this.vx = (random.nextDouble() - 0.5) * 0.02;
			this.vy = (random.nextDouble() - 0.5) * 0.02;
			this.vz = (random.nextDouble() - 0.5) * 0.02;
		}
	}

	private record Absorbing(float rate, long seenAt) {}

	private static final List<Mote> MOTES = new ArrayList<>();
	private static final Map<UUID, Absorbing> ABSORBING = new HashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();
	private static ClientLevel lastLevel;
	private static float ambientCarry;
	/** The share of the motes born about the viewer that are yin of the Great Void's battlefields ({@link #yinAir}), this tick. */
	private static float yinShare;
	/** Whether the air about the viewer holds so many that the far ones are drawn as dots ({@link #LOD_FROM}). */
	private static boolean crowded;
	private static final Map<UUID, Float> INFLOW_CARRY = new HashMap<>();
	private static final Map<Integer, Float> NPC_INFLOW_CARRY = new HashMap<>();

	private static PostChain monochrome;
	private static boolean monochromeFailed;
	private static int chainWidth, chainHeight;
	/** 1 = full colour, 0 = the plane of Qi; eased toward the ability's state every frame. */
	private static float colour = 1;
	private static long lastFrame;

	public static boolean isActive() {
		Minecraft mc = Minecraft.getInstance();
		// Switched on (a soul sets out with it open, as it is how a nascent soul sees, but may shut it: see SoulProjection).
		return mc.player != null && !mc.player.isSpectator() && ClientCultivationData.get().isQiSenseOn();
	}

	/** From the QI_ABSORPTION packet: {@code id} is meditating and drawing qi in at {@code rate} cultivation per second. */
	public static void absorption(UUID id, float rate) {
		ABSORBING.put(id, new Absorbing(rate, Util.getMillis()));
	}

	// --- Motes ---

	public static void tick(Minecraft mc) {
		if (mc.level != lastLevel) {
			MOTES.clear(); // another dimension or server: the motes belonged to the old one
			lastLevel = mc.level;
		}
		if (!isActive() || mc.level == null) {
			MOTES.clear();
			ABSORBING.clear();
			INFLOW_CARRY.clear();
			NPC_INFLOW_CARRY.clear();
			return;
		}
		if (mc.isPaused()) return;
		Player self = mc.player;
		Vec3 eye = self.getEyePosition();
		long now = Util.getMillis();
		ABSORBING.values().removeIf(a -> now - a.seenAt() > ABSORPTION_EXPIRY_MS);

		ClientLevel level = mc.level;
		// In one pass (the Great Void's air holds thousands: removing them one by one would shift the list each time).
		MOTES.removeIf(mote -> !move(mote, level) || mote.x - eye.x > KEEP_RADIUS || eye.x - mote.x > KEEP_RADIUS
				|| Math.abs(mote.y - eye.y) > KEEP_RADIUS || Math.abs(mote.z - eye.z) > KEEP_RADIUS);

		boolean immortal = ModDimensions.hasImmortalQi(mc.level.dimension()); // a small world's qi the Immortal Realm's
		boolean greatVoid = ModDimensions.isGreatVoid(mc.level.dimension());
		boolean upperRealm = ModDimensions.isUpperRealm(mc.level.dimension()) || greatVoid || immortal;
		// As thick as the place's qi (the user's ask, 2026-10-25: the Great Void's a hundred times the lower realms', as its qi; the
		// Immortal Realm's a thousand), and on the Great Void's battlefields their ghostly qi's yin over it, as thick (2026-10-27).
		double air = airQi(mc.level), yin = yinAir(mc.level, eye);
		yinShare = air + yin <= 0 ? 0 : (float) (yin / (air + yin));
		crowded = AMBIENT_PER_TICK * (air + yin) * AMBIENT_LIFE > LOD_FROM;
		ambientCarry += AMBIENT_PER_TICK * (float) (air + yin);
		while (ambientCarry >= 1 && MOTES.size() < MAX_MOTES) {
			ambientCarry--;
			spawnAmbient(mc.level, eye);
		}
		ambientCarry = Math.min(ambientCarry, 1);

		for (Map.Entry<UUID, Absorbing> entry : ABSORBING.entrySet()) {
			Player meditator = mc.level.getPlayerByUUID(entry.getKey());
			float gathering = entry.getValue().rate();
			if (meditator == null || gathering <= 0 || !ClientMeditationTracker.isMeditating(entry.getKey())
					|| meditator.distanceToSqr(eye) > ABSORPTION_RANGE * ABSORPTION_RANGE) continue;
			// Stronger gathering pulls in more: in the Upper Realm ~2 motes a tick at Qi Refining, ~5 for a Heavenly Being, ~14 for a True
			// Immortal (inflow); the thin qi of the lower realms gives half that.
			float carry = INFLOW_CARRY.getOrDefault(entry.getKey(), 0f) + inflow(gathering) * (upperRealm ? 1 : LOWER_REALM_INFLOW);
			while (carry >= 1 && MOTES.size() < MAX_MOTES) {
				carry--;
				spawnInflow(mc.level, meditator);
			}
			INFLOW_CARRY.put(entry.getKey(), Math.min(carry, 1));
		}
		INFLOW_CARRY.keySet().retainAll(ABSORBING.keySet());

		// NPC cultivators meditating nearby draw the land's qi in just the same (at a tenth of a player's rate, so less of it).
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof CultivatorNpc npc) || !npc.isMeditating() || npc.isMortal() || !npc.isAlive()
					|| npc.distanceToSqr(eye) > ABSORPTION_RANGE * ABSORPTION_RANGE) continue;
			seen.add(npc.getId());
			Realm realm = npc.getDisplayedRealm();
			double gathering = realm.getCultivationPerSecond() * npc.getDisplayedStage().getPowerMultiplier() * CultivatorNpc.NPC_RATE
					* (immortal ? PlayerCultivation.IMMORTAL_QI_MULTIPLIER : greatVoid ? PlayerCultivation.GREAT_VOID_QI_MULTIPLIER
							: upperRealm ? PlayerCultivation.UPPER_REALM_QI_MULTIPLIER : 1.0);
			float carry = NPC_INFLOW_CARRY.getOrDefault(npc.getId(), 0f) + inflow(gathering) * (upperRealm ? 1 : LOWER_REALM_INFLOW);
			while (carry >= 1 && MOTES.size() < MAX_MOTES) {
				carry--;
				spawnInflow(mc.level, npc);
			}
			NPC_INFLOW_CARRY.put(npc.getId(), Math.min(carry, 1));
		}
		NPC_INFLOW_CARRY.keySet().retainAll(seen);
	}

	/**
	 * QiVeinBlock's client hook (its animateTick): a Qi Vein gathers qi from the air, so under Qi Sense motes of water and
	 * wood qi drift in toward its open faces.
	 */
	public static void veinGathers(Level level, BlockPos pos, RandomSource random) {
		if (!isActive() || MOTES.size() >= MAX_MOTES) return;
		for (int i = 0; i < 2; i++) {
			Direction face = Direction.getRandom(random);
			if (!level.getBlockState(pos.relative(face)).isAir()) continue;
			Vec3 centre = Vec3.atCenterOf(pos);
			Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
			double out = 1.2 + random.nextDouble() * 1.4;
			Vec3 from = centre.add(normal.scale(out)).add((random.nextDouble() - 0.5) * 1.2, (random.nextDouble() - 0.5) * 1.2,
					(random.nextDouble() - 0.5) * 1.2);
			Vec3 velocity = centre.subtract(from).normalize().scale(0.05);
			QiElement element = random.nextFloat() < 0.6f ? QiElement.WATER : QiElement.WOOD;
			emit(element, from.x, from.y, from.z, velocity.x, velocity.y, velocity.z, 0.05f + random.nextFloat() * 0.03f, 25 + random.nextInt(15), null);
		}
	}

	/**
	 * Motes a tick drawn into a meditator gathering {@code gathering} cultivation a second: 0.6 and {@link #INFLOW_PER_DOUBLING} for
	 * every doubling of it, those past {@link #INFLOW_KNEE} counting {@link #INFLOW_PAST_KNEE} times, at most {@link #INFLOW_CAP}.
	 */
	static float inflow(double gathering) {
		double motes = 0.6 + INFLOW_PER_DOUBLING * Math.log1p(Math.max(0, gathering)) / Math.log(2);
		if (motes > INFLOW_KNEE) motes = INFLOW_KNEE + (motes - INFLOW_KNEE) * INFLOW_PAST_KNEE;
		return (float) Math.min(INFLOW_CAP, motes);
	}

	/** One step of a mote's life. @return false once it is spent */
	private static boolean move(Mote mote, ClientLevel level) {
		mote.prevX = mote.x;
		mote.prevY = mote.y;
		mote.prevZ = mote.z;
		if (++mote.age >= mote.life) return false;
		if (mote.drawnTo != null || mote.drawnToEntity >= 0) {
			Entity meditator = mote.drawnTo != null ? level.getPlayerByUUID(mote.drawnTo) : level.getEntity(mote.drawnToEntity);
			if (meditator == null || !meditator.isAlive()) return false;
			Vec3 core = meditator.position().add(0, 0.7, 0);
			double dx = core.x - mote.x, dy = core.y - mote.y, dz = core.z - mote.z;
			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (distance < 0.35) return false; // absorbed
			// Pulled in harder the closer it gets, with a slight swirl around the meditator.
			double pull = 0.03 + 0.12 / Math.max(1, distance);
			double swirl = 0.025 * Math.min(1, distance / INFLOW_MAX);
			mote.vx = mote.vx * 0.86 + dx / distance * pull - dz / distance * swirl;
			mote.vy = mote.vy * 0.86 + dy / distance * pull;
			mote.vz = mote.vz * 0.86 + dz / distance * pull + dx / distance * swirl;
		} else {
			// Each element drifts its own way: fire rises, water and earth settle, wood floats up, metal hangs still, and yin
			// sinks slowly, wavering like breath on cold air.
			float t = mote.age * 0.08f + mote.phase;
			double lift = switch (mote.element) {
				case FIRE -> 0.012;
				case WOOD -> 0.005;
				case WATER -> -0.003;
				case EARTH -> -0.004;
				case METAL -> 0;
				case YIN -> -0.002;
			};
			double sway = mote.element == QiElement.WATER || mote.element == QiElement.YIN ? 0.006
					: mote.element == QiElement.METAL ? 0.001 : 0.003;
			mote.vx = mote.vx * 0.96 + Mth.sin(t) * sway * 0.2;
			mote.vy = mote.vy * 0.96 + lift * 0.1;
			mote.vz = mote.vz * 0.96 + Mth.cos(t * 0.9f) * sway * 0.2;
		}
		mote.x += mote.vx;
		mote.y += mote.vy;
		mote.z += mote.vz;
		return true;
	}

	private static void spawnAmbient(ClientLevel level, Vec3 eye) {
		// Uniform through the sphere around the viewer.
		double r = SPAWN_RADIUS * Math.cbrt(RANDOM.nextDouble());
		double yaw = RANDOM.nextDouble() * Math.PI * 2;
		double pitch = Math.acos(2 * RANDOM.nextDouble() - 1);
		double x = eye.x + r * Math.sin(pitch) * Math.cos(yaw);
		double y = eye.y + r * Math.cos(pitch);
		double z = eye.z + r * Math.sin(pitch) * Math.sin(yaw);
		BlockPos pos = BlockPos.containing(x, y, z);
		if (!level.isLoaded(pos) || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return; // not inside stone
		QiElement element = pickAt(level, pos);
		MOTES.add(new Mote(element, x, y, z, 0.06f + RANDOM.nextFloat() * 0.05f, AMBIENT_LIFE - 30 + RANDOM.nextInt(60), null, RANDOM));
	}

	/**
	 * The element of a mote born at {@code pos}: by its land's mix (QiElement#pick); where the Great Void's battlefields' ghostly qi flows,
	 * yin by its share of the air about the viewer ({@link #yinShare}).
	 */
	private static QiElement pickAt(ClientLevel level, BlockPos pos) {
		if (yinShare > 0 && RANDOM.nextFloat() < yinShare && GhostQi.density(level.dimension(), level.getBiome(pos)) > 0) return QiElement.YIN;
		return QiElement.pick(level.getBiome(pos), isUnderground(level, pos), RANDOM);
	}

	private static void spawnInflow(ClientLevel level, Entity meditator) {
		double r = INFLOW_MIN + RANDOM.nextDouble() * (INFLOW_MAX - INFLOW_MIN);
		double yaw = RANDOM.nextDouble() * Math.PI * 2;
		double pitch = Math.acos(2 * RANDOM.nextDouble() - 1);
		double x = meditator.getX() + r * Math.sin(pitch) * Math.cos(yaw);
		double y = meditator.getY() + 0.7 + r * Math.cos(pitch) * 0.6; // a flattened sphere: qi comes in from the land around
		double z = meditator.getZ() + r * Math.sin(pitch) * Math.sin(yaw);
		BlockPos pos = BlockPos.containing(x, y, z);
		if (!level.isLoaded(pos)) return;
		QiElement element = pickAt(level, pos);
		boolean player = meditator instanceof Player;
		Mote mote = new Mote(element, x, y, z, 0.05f + RANDOM.nextFloat() * 0.03f, 140, player ? meditator.getUUID() : null, RANDOM);
		if (!player) mote.drawnToEntity = meditator.getId();
		MOTES.add(mote);
	}

	/**
	 * Adds a mote of qi given off by something in the world (a spirit treasure, see {@link QiSenseTreasures}): at (x, y, z),
	 * moving (vx, vy, vz), living {@code life} ticks; drawn into {@code drawnTo}'s meditation if that isn't null, otherwise
	 * drifting like the land's own qi of its element. Ignored while Qi Sense is off or the motes are at their limit.
	 */
	public static void emit(QiElement element, double x, double y, double z, double vx, double vy, double vz, float size, int life, UUID drawnTo) {
		if (!isActive() || MOTES.size() >= MAX_MOTES) return;
		Mote mote = new Mote(element, x, y, z, size, life, drawnTo, RANDOM);
		mote.vx = vx;
		mote.vy = vy;
		mote.vz = vz;
		MOTES.add(mote);
	}

	/**
	 * How thick qi is in the air about the viewer, against the lower realms' (1): in the Netherworld its ghostly qi (nearly all yin,
	 * see QiElement), {@link #NETHERWORLD_AIR}; anywhere else the viewer's own measure of where they stand
	 * ({@link PlayerCultivation#placeFactor}: the Upper Realm's ten, the Great Void's hundred, space's quarter, a planet's own).
	 */
	private static double airQi(ClientLevel level) {
		if (ModDimensions.isNetherworld(level.dimension())) return NETHERWORLD_AIR;
		return ClientCultivationData.get().placeFactor();
	}

	/**
	 * How thick yin is in the air about the viewer over what their land's mix holds of it: the Great Void's battlefields' ghostly qi
	 * (GhostQi#ANCIENT_BATTLEFIELD, a hundred) as thick as its density, on the Netherworld's measure (its ten as thick as
	 * {@link #NETHERWORLD_AIR}): as many cyan motes as the Great Void's own qi has motes of the five. None anywhere else (the
	 * Netherworld's and a Soul Sand Valley's yin is in their land's mix, QiElement; the living world shows none).
	 */
	private static double yinAir(ClientLevel level, Vec3 eye) {
		if (!ModDimensions.isGreatVoid(level.dimension())) return 0;
		return GhostQi.density(level.dimension(), level.getBiome(BlockPos.containing(eye))) * NETHERWORLD_AIR / GhostQi.NETHERWORLD;
	}

	/** No open sky above and below sea level, in a realm that has a sky: caves lean to earth and metal. */
	private static boolean isUnderground(ClientLevel level, BlockPos pos) {
		return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling() && pos.getY() < level.getSeaLevel()
				&& level.getBrightness(LightLayer.SKY, pos) < level.getMaxLightLevel();
	}

	public static boolean hasMotes() {
		return !MOTES.isEmpty();
	}

	/**
	 * Draws the motes on {@link SenseOverlay}, facing the camera: with {@code halo} false a solid core in the element's core
	 * colour, laid over the world so it stays vivid even against a bright grey sky; with {@code halo} true a wider, faint
	 * glow in its glow colour added around it (the overlay sets the matching blending for each pass).
	 */
	static void renderMotes(WorldRenderContext context, boolean halo) {
		Camera camera = context.camera();
		Vec3 cam = camera.getPosition();
		Vector3f left = camera.getLeftVector();
		Vector3f up = camera.getUpVector();
		Vector3f look = camera.getLookVector();
		float partial = context.tickDelta();
		Matrix4f matrix = context.matrixStack().last().pose();
		Frustum frustum = context.frustum();
		float[] cos = new float[GLOW_SEGMENTS + 1];
		float[] sin = new float[GLOW_SEGMENTS + 1];
		for (int i = 0; i <= GLOW_SEGMENTS; i++) {
			cos[i] = Mth.cos(i * Mth.TWO_PI / GLOW_SEGMENTS);
			sin[i] = Mth.sin(i * Mth.TWO_PI / GLOW_SEGMENTS);
		}

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (Mote mote : MOTES) {
			float alpha = envelope(mote, partial);
			if (alpha <= 0.01f) continue;
			double x = Mth.lerp(partial, mote.prevX, mote.x), y = Mth.lerp(partial, mote.prevY, mote.y), z = Mth.lerp(partial, mote.prevZ, mote.z);
			float px = (float) (x - cam.x), py = (float) (y - cam.y), pz = (float) (z - cam.z);
			// In crowded air (LOD_FROM) a far one is a soft dot with no halo of its own, looked for by the line of sight alone.
			float distanceSq = px * px + py * py + pz * pz;
			boolean far = crowded && distanceSq > LOD_NEAR * LOD_NEAR;
			float size;
			if (far) {
				float ahead = px * look.x() + py * look.y() + pz * look.z();
				if (halo || ahead <= 0 || ahead * ahead < distanceSq * FAR_CONE) continue;
				size = mote.size * FAR_DOT;
			} else {
				size = halo ? mote.size * HALO_SCALE : mote.size;
				// Only those in view (most of the thousands about the viewer in the Great Void are behind or beside them).
				if (frustum != null && !frustum.isVisible(new AABB(x - size, y - size, z - size, x + size, y + size, z + size))) continue;
			}
			int color = halo ? mote.element.getGlowColor() : mote.element.getCoreColor();
			float r = (color >> 16 & 0xFF) / 255f, g = (color >> 8 & 0xFF) / 255f, b = (color & 0xFF) / 255f;
			// The core is solid to two thirds of its radius, then softens; the halo fades from the middle out, and so does a far dot.
			float middle = halo ? alpha * HALO_ALPHA : alpha;
			float rim = halo || far ? 0 : alpha * 0.85f;
			int step = far ? 2 : 1; // a far dot has four corners
			for (int i = 0; i < GLOW_SEGMENTS; i += step) {
				buffer.vertex(matrix, px, py, pz).color(r, g, b, middle).endVertex();
				buffer.vertex(matrix, px + (left.x() * cos[i] + up.x() * sin[i]) * size, py + (left.y() * cos[i] + up.y() * sin[i]) * size,
						pz + (left.z() * cos[i] + up.z() * sin[i]) * size).color(r, g, b, rim).endVertex();
				buffer.vertex(matrix, px + (left.x() * cos[i + step] + up.x() * sin[i + step]) * size, py + (left.y() * cos[i + step] + up.y() * sin[i + step]) * size,
						pz + (left.z() * cos[i + step] + up.z() * sin[i + step]) * size).color(r, g, b, rim).endVertex();
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	/** Brightness over a mote's life: fades in, holds, fades out; metal glints. */
	private static float envelope(Mote mote, float partial) {
		float age = mote.age + partial;
		float alpha = Math.min(1, age / 12f) * Math.min(1, (mote.life - age) / 20f);
		if (mote.drawnTo != null) alpha = Math.min(1, age / 6f); // inflow vanishes into the meditator instead
		if (mote.element == QiElement.METAL) alpha *= 0.65f + 0.35f * Mth.sin(age * 0.45f + mote.phase);
		return Math.max(0, alpha) * 0.9f;
	}

	// --- Monochrome ---

	/** Runs the desaturating post effect on the frame, eased in and out (from {@link SenseOverlay#beforeEntityOutline}). */
	static void applyMonochrome(float partialTick) {
		long now = Util.getMillis();
		float seconds = Math.min(0.25f, (now - lastFrame) / 1000f);
		lastFrame = now;
		float target = isActive() ? 0 : 1;
		float step = seconds / FADE_SECONDS;
		colour = target < colour ? Math.max(target, colour - step) : Math.min(target, colour + step);
		if (colour >= 1) return;

		PostChain chain = monochromeChain();
		if (chain == null) return;
		float eased = colour * colour * (3 - 2 * colour);
		float saturation = Mth.lerp(eased, MONO_SATURATION, 1);
		float brightness = Mth.lerp(eased, MONO_BRIGHTNESS, 1);
		for (PostPass pass : ((PostChainAccessor) chain).dth$getPasses()) {
			pass.getEffect().safeGetUniform("Saturation").set(saturation);
			pass.getEffect().safeGetUniform("ColorScale").set(brightness, brightness, brightness);
		}
		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.resetTextureMatrix();
		chain.process(partialTick);
		Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
	}

	/** Built on first use and kept in step with the window's size; null (logged once) if the shader can't load. */
	private static PostChain monochromeChain() {
		Minecraft mc = Minecraft.getInstance();
		if (monochrome == null) {
			if (monochromeFailed) return null;
			try {
				monochrome = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), MONOCHROME);
				chainWidth = -1;
			} catch (Exception e) {
				monochromeFailed = true;
				DefyingTheHeavens.LOGGER.error("Qi Sense: couldn't load the monochrome post effect {}", MONOCHROME, e);
				return null;
			}
		}
		int width = mc.getMainRenderTarget().width;
		int height = mc.getMainRenderTarget().height;
		if (width != chainWidth || height != chainHeight) {
			monochrome.resize(width, height);
			chainWidth = width;
			chainHeight = height;
		}
		return monochrome;
	}

	/** Resource reload: the post effect is rebuilt from the new resources on next use. */
	public static void closeMonochrome() {
		if (monochrome != null) monochrome.close();
		monochrome = null;
		monochromeFailed = false;
	}

	/** Disconnect. */
	public static void clear() {
		MOTES.clear();
		ABSORBING.clear();
		INFLOW_CARRY.clear();
		NPC_INFLOW_CARRY.clear();
		ambientCarry = 0;
		colour = 1;
		lastLevel = null;
	}

	private QiSenseClientHandler() {}
}
