package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lei Zhen the Tribulation Breaker, who carries Heaven's Tribulation Spear (Legend; phase 3a of the Artifact Forge), as the user would have
 * him (2026-10-28): "Lei Zhen should be insane and attack anyone that comes into his consciousness ... he should roam the immortal realm
 * under his storm", and "at realm 14" (Golden Immortal, the fourteenth counting Qi Refining the first: {@link #RANK}).
 * <ul>
 *   <li><b>He roams</b> the Immortal Realm's wilds from one far point to the next ({@link #LEG_MIN} to {@link #LEG_MAX} apart, within
 *   {@link #RING_MIN} to {@link #RING_MAX} of the Southern Heavenly Gate, each clear of the realm's cities and sects as the primordial beasts'
 *   lairs are: PrimordialBeasts#clearOfPeople) at {@link #PACE}, whether anyone is near or not: where he is and where he goes are the
 *   spear's entry in Legends (its site, its way). He stands in the world only while someone is near ({@link #APPEAR}); with nobody near for
 *   a while ({@link #GONE}), he goes on unseen.</li>
 *   <li><b>His storm</b>, as the True Dragon's (the user's word, 2026-10-28: "the storm that follows lei zhen shouldnt be a tribulation
 *   cloud, just make the sky grey and a thunderstorm like the true dragon"): near him the realm's sky and fog darken to the storm's grey
 *   (the client's ImmortalRealmClient, by the spear in his hand), and lightning strikes about him now and then, the oftener while he
 *   fights, seen and heard and harming nothing ({@link #storm}).</li>
 *   <li><b>Mad</b>: whoever comes into his consciousness ({@link #CONSCIOUSNESS}: a domain's whole reach), player or cultivator, he flies at
 *   and fights, raving; and he calls the spear's bolt down on whoever he fights (its art's harm only: its brand on a tribulation is a
 *   player's to lay, wielding it, the user's word: LegendaryWeaponItem#heavensBrand).</li>
 *   <li>Slain, he drops the spear where he falls (Legends#onHolderDeath). Lost and reformed, it is his again, and he walks the realm with
 *   it once more.</li>
 * </ul>
 */
public final class LeiZhen {
	public static final String NAME = "Lei Zhen";
	/** The tag his body carries (besides a legend holder's, Legends#HOLDER_TAG). */
	public static final String TAG = DefyingTheHeavens.MOD_ID + "_lei_zhen";
	/** Golden Immortal Grand Perfection: the fourteenth realm (the user's word), at its peak. */
	public static final int RANK = PlayerCultivation.rank(Realm.GOLDEN_IMMORTAL, Stage.GRAND_PERFECTION);
	/** His consciousness: anyone within it, he goes for. */
	public static final double CONSCIOUSNESS = ConsciousnessDomainHandler.MAX_RADIUS;
	/** Flown within this of whoever he goes for, he fights them (within the combat goal's own reach, CultivatorGoals.CombatGoal#fights). */
	static final double CLOSE = 80;
	/** He comes into the world when someone is this near where he is (sideways); nobody within {@link #GONE} for {@link #GONE_AFTER} checks, he leaves it. */
	static final double APPEAR = 192, GONE = 256;
	static final int GONE_AFTER = 15;
	/** His way's points: this far apart at the least and the most, and within this ring about the Southern Heavenly Gate. */
	static final int LEG_MIN = 300, LEG_MAX = 700, RING_MIN = 800, RING_MAX = 2600;
	/** How fast he roams (blocks a tick), seen or unseen: some three blocks a second, a day's walk some seven thousand. */
	static final double PACE = 0.15;
	/** How fast he flies at whoever comes into his consciousness. */
	static final double RUSH = 0.9;
	/** He flies this high over the land, or over the sea of clouds. */
	static final int ALOFT = 8;
	/** The spear's bolt at whoever he fights: one time in three each two seconds, while they are within this. */
	static final double BOLT_REACH = 48;
	/** His storm's lightning: the chance of a bolt each two seconds, roaming and fighting, and how far from him it strikes. */
	static final float STORM_ROAMING = 0.3f, STORM_FIGHTING = 0.6f;
	static final double STORM_NEAR = 8, STORM_FAR = 36;
	/** What is told of where he roams is told anew this often (checks: two minutes). */
	static final int RETELL = 60;
	/** He raves at a player he turns on at most this often (ticks). */
	static final long RAVE_EVERY = 600;
	/** How many lines of his raving there are (ModLang#leiZhenRave). */
	public static final int RAVES = 4;

	/** Whoever he flies at, not yet near enough to fight. */
	private static @Nullable UUID heading;
	/** Checks with nobody near him, and since what is told of him was told anew. */
	private static int unseen, told;
	/** When he last raved at each player. */
	private static final Map<UUID, Long> RAVED = new HashMap<>();

	public static boolean is(CultivatorNpc npc) {
		return npc.getTags().contains(TAG);
	}

	/** Whether mad Lei Zhen goes for {@code target}: anyone at all, living, but one in creative or watching, or a cultivator hidden from him. */
	public static boolean goesFor(CultivatorNpc lei, LivingEntity target) {
		if (target == lei || !target.isAlive() || target.isRemoved()) return false;
		if (target instanceof Player player) return !player.isCreative() && !player.isSpectator();
		return target instanceof CultivatorNpc npc && !npc.isConcealed();
	}

	// --- Every two seconds (Legends#tick) ---

	/** Where he first walks: by the world's seed, somewhere out in his ring, clear of the realm's people. */
	static BlockPos start(ServerLevel level) {
		RandomSource random = RandomSource.create(level.getSeed() ^ 0x1E12E7L);
		RandomState state = level.getChunkSource().randomState();
		BlockPos at = null;
		for (int t = 0; t < 64; t++) {
			double angle = random.nextDouble() * Math.PI * 2, out = 1200 + random.nextDouble() * 800;
			int x = (int) Math.round(ImmortalGate.HEAVEN_X + Math.cos(angle) * out), z = (int) Math.round(ImmortalGate.HEAVEN_Z + Math.sin(angle) * out);
			at = new BlockPos(x, level.getMinBuildHeight(), z);
			if (PrimordialBeasts.clearOfPeople(state, x, z)) break;
		}
		return at;
	}

	/**
	 * Lei Zhen's two seconds: his way's next point when he reaches the last; unseen, on along it, and into the world when someone comes near;
	 * in it, where he is noted, out of it when nobody has been near for a while, and his madness and his storm.
	 */
	static void tick(ServerLevel level, Legends data, Legends.Entry entry) {
		if (entry.state == Legends.State.ABROAD || entry.state == Legends.State.LOST || entry.site == null) return; // slain: none walks the realm until the spear reforms
		if (entry.toward == null || flat(entry.site, entry.toward) < 24) {
			entry.toward = nextPoint(level, entry.site, level.getRandom());
			data.setDirty();
		}
		CultivatorNpc lei = Legends.keeper(level, Legend.TRIBULATION_SPEAR);
		if (lei == null && entry.state == Legends.State.HELD) {
			// Out of the world: his ground unloaded about him. He goes on unseen from where he was (the body left there goes when it is found:
			// Legends#stray).
			entry.state = Legends.State.RESTING;
			entry.holder = null;
			entry.holderName = "";
			data.setDirty();
		}
		if (++told >= RETELL) {
			told = 0;
			Legends.noteRumour(level.getServer(), entry);
		}
		if (lei == null) {
			entry.site = step(entry.site, entry.toward, PACE * 40, level.getMinBuildHeight());
			data.setDirty();
			BlockPos column = new BlockPos(entry.site.getX(), ModDimensions.IMMORTAL_CLOUD_Y, entry.site.getZ());
			if (Legends.someoneNear(level, entry.site, APPEAR) && level.isPositionEntityTicking(column)) come(level, data, entry);
			return;
		}
		entry.site = new BlockPos(lei.getBlockX(), level.getMinBuildHeight(), lei.getBlockZ());
		data.setDirty();
		if (!Legends.someoneNear(level, lei.blockPosition(), GONE)) {
			if (++unseen >= GONE_AFTER) go(level, data, entry, lei);
			return;
		}
		unseen = 0;
		madness(level, lei);
		storm(level, lei);
	}

	/** Lei Zhen into the world where he is on his way, aloft under his storm, the spear in his hand. */
	static @Nullable CultivatorNpc come(ServerLevel level, Legends data, Legends.Entry entry) {
		CultivatorNpc lei = CultivatorNpc.createFor(level);
		if (lei == null) return null;
		RandomSource random = level.getRandom();
		Vec3 at = aloft(level, entry.site.getX() + 0.5, entry.site.getZ() + 0.5);
		lei.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
		lei.becomeRogue(RANK, 0, random);
		lei.setRank(RANK, true); // the realm's one Golden Immortal, whatever room it has for them (HighRealms), as the Court's own are
		lei.equip(random);
		lei.setCustomName(Component.literal(NAME));
		lei.addTag(TAG);
		Legends.hold(lei, entry.serial > 0 ? Legends.restack(entry) : Legends.issue(level.getServer(), Legend.TRIBULATION_SPEAR));
		level.addFreshEntity(lei);
		lei.setQiFlying(true);
		entry.state = Legends.State.HELD;
		entry.holder = lei.getUUID();
		entry.holderName = NAME;
		data.setDirty();
		unseen = 0;
		heading = null;
		bolt(level, lei.getX(), lei.getZ()); // a thunderclap where he comes
		DefyingTheHeavens.LOGGER.info("Lei Zhen comes into the world at {}", lei.blockPosition().toShortString());
		return lei;
	}

	/** Lei Zhen out of the world (nobody near): on his way unseen from where he is, the spear with him still. */
	static void go(ServerLevel level, Legends data, Legends.Entry entry, CultivatorNpc lei) {
		entry.site = new BlockPos(lei.getBlockX(), level.getMinBuildHeight(), lei.getBlockZ());
		entry.state = Legends.State.RESTING;
		entry.holder = null;
		entry.holderName = "";
		data.setDirty();
		lei.discard(); // and the spear in his hand with him: the record's still, he comes again with it
		unseen = 0;
		heading = null;
	}

	/**
	 * His madness: whoever is in his consciousness, the nearest, he flies at, and fights once near ({@link #CLOSE}), raving at a player; at
	 * whoever he fights, now and then, the spear's bolt.
	 */
	static void madness(ServerLevel level, CultivatorNpc lei) {
		LivingEntity target = lei.getTarget();
		if (target == null || !target.isAlive()) {
			LivingEntity found = null;
			double best = Double.MAX_VALUE;
			for (LivingEntity one : ConsciousnessDomainHandler.entitiesIn(lei, CONSCIOUSNESS, LivingEntity.class, e -> goesFor(lei, e))) {
				double d = lei.distanceToSqr(one);
				if (d < best) {
					best = d;
					found = one;
				}
			}
			heading = found == null ? null : found.getUUID();
			if (found != null && best <= CLOSE * CLOSE) {
				lei.setTarget(found);
				lei.markAggressed(found);
			}
			if (found instanceof ServerPlayer player) rave(level, lei, player);
		}
		target = lei.getTarget();
		if (target != null && target.isAlive() && lei.distanceTo(target) <= BOLT_REACH && level.getRandom().nextInt(3) == 0
				&& lei.getMainHandItem().getItem() instanceof LegendaryWeaponItem spear && spear.legend() == Legend.TRIBULATION_SPEAR) {
			LegendaryWeaponItem.heavensBolt(level, lei, target.position(), 3);
		}
	}

	/** One of his ravings at {@code player}, and the thunder with it (now and then: {@link #RAVE_EVERY}). */
	private static void rave(ServerLevel level, CultivatorNpc lei, ServerPlayer player) {
		long now = level.getGameTime();
		Long last = RAVED.get(player.getUUID());
		if (last != null && now - last < RAVE_EVERY) return;
		RAVED.put(player.getUUID(), now);
		TalkSystem.say(player, lei, Component.translatable(ModLang.leiZhenRave(level.getRandom().nextInt(RAVES))).withStyle(ChatFormatting.LIGHT_PURPLE), 0);
		player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.8f, 0.5f);
	}

	/** His storm's lightning: a bolt about him now and then ({@link #STORM_ROAMING}), the oftener while he fights ({@link #STORM_FIGHTING}). */
	static void storm(ServerLevel level, CultivatorNpc lei) {
		RandomSource random = level.getRandom();
		if (random.nextFloat() >= (lei.getTarget() != null ? STORM_FIGHTING : STORM_ROAMING)) return;
		double angle = random.nextDouble() * Math.PI * 2, r = STORM_NEAR + random.nextDouble() * (STORM_FAR - STORM_NEAR);
		bolt(level, lei.getX() + Math.cos(angle) * r, lei.getZ() + Math.sin(angle) * r);
	}

	/**
	 * A bolt of his storm at (x, z), down onto the land there or the sea of clouds: vanilla's lightning, as the True Dragon's storm's is
	 * (PrimordialBeast#bolt), seen and heard, harming nothing and kindling nothing.
	 */
	static void bolt(ServerLevel level, double x, double z) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt == null) return;
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
		bolt.moveTo(x, Math.max(ground, ModDimensions.IMMORTAL_CLOUD_Y), z);
		bolt.setVisualOnly(true);
		level.addFreshEntity(bolt);
	}

	// --- Each tick ---

	/**
	 * While he stands in the world with nobody to fight: aloft (as he is mad, he walks nowhere), flying at whoever he has sensed, or on along
	 * his way.
	 */
	static void steer(MinecraftServer server) {
		ServerLevel level = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (level == null || level.players().isEmpty()) return;
		Legends.Entry entry = Legends.get(server).entry(Legend.TRIBULATION_SPEAR);
		if (entry.state != Legends.State.HELD || entry.holder == null) return;
		if (!(level.getEntity(entry.holder) instanceof CultivatorNpc lei) || !lei.isAlive()) return;
		if (lei.getTarget() != null) return; // the fight is the combat goal's
		if (!lei.isQiFlying()) lei.setQiFlying(true);
		Entity sensed = heading == null ? null : level.getEntity(heading);
		if (sensed instanceof LivingEntity living && goesFor(lei, living) && lei.distanceTo(living) <= CONSCIOUSNESS + 16) {
			lei.flyToward(living.position().add(0, 2, 0), RUSH);
			return;
		}
		heading = null;
		if (entry.toward == null) return;
		double dx = entry.toward.getX() + 0.5 - lei.getX(), dz = entry.toward.getZ() + 0.5 - lei.getZ(), d = Math.sqrt(dx * dx + dz * dz);
		if (d < 1) return;
		lei.flyToward(aloft(level, lei.getX() + dx / d * 12, lei.getZ() + dz / d * 12), PACE);
	}

	// --- His way ---

	/** Where he flies over (x, z): {@link #ALOFT} over the land there, or over the sea of clouds. */
	static Vec3 aloft(ServerLevel level, double x, double z) {
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
		return new Vec3(x, Math.max(ground, ModDimensions.IMMORTAL_CLOUD_Y) + ALOFT, z);
	}

	/**
	 * His way's next point from {@code from}: {@link #LEG_MIN} to {@link #LEG_MAX} off, within his ring, clear of the realm's people; failing
	 * that, the last tried within the ring; failing that (out past it), back toward the ring's middle.
	 */
	static BlockPos nextPoint(ServerLevel level, BlockPos from, RandomSource random) {
		RandomState state = level.getChunkSource().randomState();
		BlockPos within = null;
		for (int t = 0; t < 48; t++) {
			double angle = random.nextDouble() * Math.PI * 2, leg = LEG_MIN + random.nextDouble() * (LEG_MAX - LEG_MIN);
			int x = (int) Math.round(from.getX() + Math.cos(angle) * leg), z = (int) Math.round(from.getZ() + Math.sin(angle) * leg);
			double out = Math.hypot(x - ImmortalGate.HEAVEN_X, z - ImmortalGate.HEAVEN_Z);
			if (out < RING_MIN || out > RING_MAX) continue;
			within = new BlockPos(x, level.getMinBuildHeight(), z);
			if (PrimordialBeasts.clearOfPeople(state, x, z)) return within;
		}
		if (within != null) return within;
		double angle = Math.atan2(from.getZ() - ImmortalGate.HEAVEN_Z, from.getX() - ImmortalGate.HEAVEN_X), middle = (RING_MIN + RING_MAX) / 2.0;
		return new BlockPos((int) Math.round(ImmortalGate.HEAVEN_X + Math.cos(angle) * middle), level.getMinBuildHeight(),
				(int) Math.round(ImmortalGate.HEAVEN_Z + Math.sin(angle) * middle));
	}

	/** {@code from} moved {@code by} toward {@code to} (sideways), not past it. */
	static BlockPos step(BlockPos from, BlockPos to, double by, int y) {
		double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ(), d = Math.sqrt(dx * dx + dz * dz);
		if (d <= by) return new BlockPos(to.getX(), y, to.getZ());
		return new BlockPos((int) Math.round(from.getX() + dx / d * by), y, (int) Math.round(from.getZ() + dz / d * by));
	}

	private static double flat(BlockPos a, BlockPos b) {
		return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
	}

	/** A server stopping: what he had sensed forgotten. */
	static void clear() {
		heading = null;
		unseen = 0;
		told = 0;
		RAVED.clear();
	}

	private LeiZhen() {}
}
