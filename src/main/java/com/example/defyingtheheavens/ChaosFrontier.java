package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Chaos Frontier (phase 3c of the realm plan, 2026-10-26: "the realm's edge, where chaos before heaven and earth still churns"): the
 * Immortal Realm out past some four thousand blocks from the main island's heart. Its land breaks up into floating shards from
 * ModNoiseSettings#FRONTIER_FROM (no city, sect or wanderer of the realm's comes past there: ModNoiseSettings's sites, NpcSpawner), and from
 * {@link #EDGE} on it is the Frontier's own (its biome, ModBiomes#CHAOS_FRONTIER; its look, the client's); further out its shards join
 * into one wasteland floating over the sea of clouds (ModNoiseSettings#WASTE_FROM). There:
 * <ul>
 *   <li><b>Its storm</b> ({@link #storms}, each second): a player in it unsheltered (not creative, not a spectator) loses
 *   {@link #STORM_SHARE} of their health to chaos (ModDamageTypes#CHAOS_STORM: through armour and every ward; it breaks a meditation as any
 *   harm does, and a soul turned inward is pulled back to its body left in it), and their body does not mend of itself ({@link #exposed}:
 *   FoodDataMixin; spells and pills still heal). Sheltered
 *   ({@link #sheltered}) within a lit Law domain, their own or one whose owner is no foe of theirs (LawDomains#shelters), or in the First
 *   Flame's light (FirstFlame#inLight).</li>
 *   <li><b>Chaos Fiends</b> ({@link #spawnTick}, ChaosFiend): up to {@link #NEAR_CAP} about each player in it, come out of the air, the
 *   stronger the deeper ({@link #fiendRanks}): True Immortals of Middle to Grand Perfection within {@link #SHALLOW} blocks past its edge,
 *   Earth Immortals to {@link #DEEP}, Heaven Immortals beyond. Never in peace, nor with the world's spawning or the beasts' off.</li>
 * </ul>
 * The Primordial Chaos keeps its lair out in it (PrimordialBeasts), and the First Flame burns on its island there (FirstFlame).
 */
public final class ChaosFrontier {
	/** Where the Frontier is its own (its biome, its storm, its fiends): half way through the land's breaking up. */
	public static final double EDGE = (ModNoiseSettings.FRONTIER_FROM + ModNoiseSettings.FRONTIER_TO) / 2;
	/** The share of a body's health its storm takes each second. */
	public static final float STORM_SHARE = 0.01f;
	/** Past the edge, within this the fiends are True Immortals, within this Earth Immortals, beyond it Heaven Immortals. */
	public static final double SHALLOW = 1000, DEEP = 2000;
	/** How often each player in it is tried for fiends (ticks), and at most this many within {@link #NEAR} of them. */
	static final int SPAWN_INTERVAL = 100, NEAR_CAP = 6;
	static final double NEAR = 64;
	/** Fiends come this far from the player, and no nearer the First Flame than its light and this. */
	static final int MIN_DISTANCE = 20, MAX_DISTANCE = 48;
	static final double FLAME_KEEP = 24;

	/** Those in it (for the word of crossing in and out), and those its storm has unsheltered now. */
	private static final Set<UUID> INSIDE = new HashSet<>(), EXPOSED = new HashSet<>();

	// --- Where it is ---

	/** How far column (x, z) lies from the main island's heart. */
	public static double out(double x, double z) {
		return Math.hypot(x - ModNoiseSettings.MAIN_X, z - ModNoiseSettings.MAIN_Z);
	}

	/** How far past the Frontier's edge column (x, z) lies (less than 0 short of it). */
	public static double depth(double x, double z) {
		return out(x, z) - EDGE;
	}

	/** Whether column (x, z) of {@code level} is in the Chaos Frontier: the Immortal Realm's, past its edge. */
	public static boolean isIn(Level level, double x, double z) {
		return ModDimensions.isImmortalRealm(level.dimension()) && depth(x, z) >= 0;
	}

	/** Whether column (x, z) lies where the realm's land breaks up, or past it: none of the realm's people come there. */
	public static boolean pastSettled(double x, double z) {
		return out(x, z) >= ModNoiseSettings.FRONTIER_FROM;
	}

	// --- Its storm ---

	/** Each tick: its storm each second, its fiends now and then. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 == 0) storms(server);
		if (server.getTickCount() % SPAWN_INTERVAL == 7) spawnTick(server);
	}

	/** Whether {@code p} stands unsheltered in the Frontier's storm now (as its last second found them): their body does not mend of itself. */
	public static boolean exposed(ServerPlayer p) {
		return EXPOSED.contains(p.getUUID());
	}

	/** Whether {@code p} is sheltered from the storm: in the First Flame's light, or within a lit Law domain that shelters them. */
	public static boolean sheltered(ServerPlayer p) {
		return FirstFlame.inLight(p.level().dimension(), p.position()) || LawDomains.shelters(p);
	}

	/**
	 * A second of the storm, for every player: in the Frontier or not ({@link #weather}). A soul turned inward (InnerRealm) that left its body
	 * out in the storm unsheltered is pulled back into it, as any harm to the body pulls it (no sitting the storm out in the Inner Realm).
	 */
	static void storms(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (InnerRealm.isInside(p) && bodyInTheStorm(p)) MeditationManager.stop(p, true);
			weather(p, p.isAlive() && isIn(p.level(), p.getX(), p.getZ()));
		}
	}

	/** Whether {@code p}'s body, sitting while its soul is in the Inner Realm, sits in the Frontier out of the First Flame's light. */
	private static boolean bodyInTheStorm(ServerPlayer p) {
		ResourceKey<Level> dimension = InnerRealm.bodyDimension(p);
		BlockPos body = InnerRealm.bodyPos(p);
		return ModDimensions.isImmortalRealm(dimension) && depth(body.getX(), body.getZ()) >= 0 && !FirstFlame.inLight(dimension, Vec3.atBottomCenterOf(body));
	}

	/**
	 * A second of the storm for {@code p}, {@code in} the Frontier or not: told of crossing in and out; unsheltered in it, harmed and kept
	 * from mending ({@link #exposed}), told when that begins and when it ends.
	 */
	static void weather(ServerPlayer p, boolean in) {
		UUID id = p.getUUID();
		if (in && INSIDE.add(id)) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_CHAOS_ENTER).withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (!in && INSIDE.remove(id) && p.isAlive()) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_CHAOS_LEAVE).withStyle(ChatFormatting.GRAY));
		}
		boolean exposed = in && !p.isCreative() && !p.isSpectator() && !sheltered(p);
		if (exposed) {
			if (EXPOSED.add(id)) p.displayClientMessage(Component.translatable(ModLang.MSG_CHAOS_EXPOSED).withStyle(ChatFormatting.LIGHT_PURPLE), true);
			p.hurt(ModDamageTypes.chaosStorm(p.level()), p.getMaxHealth() * STORM_SHARE);
		} else if (EXPOSED.remove(id) && in) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_CHAOS_SHELTERED).withStyle(ChatFormatting.GOLD), true);
		}
	}

	/** A logout: forgotten. */
	public static void onDisconnect(ServerPlayer p) {
		INSIDE.remove(p.getUUID());
		EXPOSED.remove(p.getUUID());
	}

	/** The server stopping: all forgotten. */
	public static void clear() {
		INSIDE.clear();
		EXPOSED.clear();
	}

	// --- Its fiends ---

	/** The ranks a Chaos Fiend comes in, {lowest, highest}, {@code depth} blocks past the Frontier's edge. */
	public static int[] fiendRanks(double depth) {
		Realm realm = depth < SHALLOW ? Realm.TRUE_IMMORTAL : depth < DEEP ? Realm.EARTH_IMMORTAL : Realm.HEAVEN_IMMORTAL;
		Stage from = realm == Realm.TRUE_IMMORTAL ? Stage.MID : Stage.EARLY;
		return new int[] {PlayerCultivation.rank(realm, from), PlayerCultivation.rank(realm, Stage.GRAND_PERFECTION)};
	}

	/** Each player in the Frontier tried for fiends ({@link #trySpawnNear}), as the world's beasts are tried (BeastSpawner). */
	static void spawnTick(MinecraftServer server) {
		ServerLevel level = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (level == null || level.players().isEmpty() || level.getDifficulty() == Difficulty.PEACEFUL) return;
		if (!level.getGameRules().getBoolean(ModGameRules.DEMON_BEASTS_SPAWN) || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
		for (ServerPlayer p : level.players()) {
			if (p.isSpectator() || !p.isAlive() || !isIn(level, p.getX(), p.getZ())) continue;
			trySpawnNear(level, p);
		}
	}

	/**
	 * One try round {@code p}: a spot in the open air {@link #MIN_DISTANCE} to {@link #MAX_DISTANCE} off, in the Frontier, out of the First
	 * Flame's light (by {@link #FLAME_KEEP} more), no player nearer; there a fiend or two of that depth's ranks. A spot in the land (the
	 * wasteland's ground under one walking it) gives way to the next. @return how many came
	 */
	static int trySpawnNear(ServerLevel level, ServerPlayer p) {
		RandomSource random = level.getRandom();
		int near = level.getEntitiesOfClass(ChaosFiend.class, p.getBoundingBox().inflate(NEAR), ChaosFiend::isAlive).size();
		if (near >= NEAR_CAP) return 0;
		for (int attempt = 0; attempt < 6; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2, d = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
			double x = p.getX() + Math.cos(angle) * d, z = p.getZ() + Math.sin(angle) * d;
			double y = Mth.clamp(p.getY() + random.nextInt(17) - 8, ModDimensions.IMMORTAL_CLOUD_Y + 4, level.getMaxBuildHeight() - 24);
			BlockPos at = BlockPos.containing(x, y, z);
			if (!level.isLoaded(at) || !level.isPositionEntityTicking(at) || !isIn(level, x, z)) continue;
			double keep = FirstFlame.LIGHT + FLAME_KEEP;
			if (new Vec3(x, y, z).distanceToSqr(FirstFlame.heart()) < keep * keep) continue;
			if (level.getNearestPlayer(x, y, z, MIN_DISTANCE - 4, false) != null) continue;
			int[] ranks = fiendRanks(depth(x, z));
			int pack = BeastKind.CHAOS_FIEND.packSize(random), came = 0;
			for (int i = 0; i < pack && near + came < NEAR_CAP; i++) {
				BlockPos spot = i == 0 ? at : at.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
				if (fiend(level, spot, ranks[0] + random.nextInt(ranks[1] - ranks[0] + 1)) != null) came++;
				else if (i == 0) break;
			}
			if (came == 0) continue;
			return came;
		}
		return 0;
	}

	/** A Chaos Fiend of {@code rank} at {@code at} (in the open air), or null if there is no room for it. */
	static ChaosFiend fiend(ServerLevel level, BlockPos at, int rank) {
		if (!(ModEntities.beast(BeastKind.CHAOS_FIEND).create(level) instanceof ChaosFiend fiend)) return null;
		fiend.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		fiend.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null, null);
		fiend.setRank(rank);
		if (!level.noCollision(fiend)) return null;
		level.addFreshEntity(fiend);
		return fiend;
	}

	private ChaosFrontier() {}
}
