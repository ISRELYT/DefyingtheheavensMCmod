package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Qi surges: every minute or so of meditation, qi rises and offers a short minigame. Play it well and the meditation falls
 * into Qi Harmony (see MeditationManager#harmony); ignore it and it passes. Nothing stays on screen between surges.
 * <ul>
 *   <li>Screen games, anywhere (drawn and judged by the client's QiSurgeHud, which reports back through
 *   MeditationManager#onCirculation): the Small Heavenly Circuit, the Five Elements cycle and Breath rhythm.</li>
 *   <li>Inner Realm games, played on foot on the island and run here: Qi wisps, the Leyline sequence and, from Nascent
 *   Soul, the Heart demon.</li>
 * </ul>
 */
public final class QiSurges {
	public enum Game {
		CIRCUIT(true), ELEMENTS(true), BREATH(true), WISPS(false), SEQUENCE(false), HEART_DEMON(false);

		public final boolean screen;

		Game(boolean screen) { this.screen = screen; }

		public static Game byIndex(int i) { return i >= 0 && i < values().length ? values()[i] : null; }
	}

	/** The first surge comes this long after sitting down (ticks), then one every INTERVAL_MIN..MAX after the last ends. */
	public static final int FIRST_SURGE = 600;
	public static final int INTERVAL_MIN = 1000, INTERVAL_MAX = 1600;
	/** How long a screen surge waits to be played before it passes (the client says so sooner; this is a safety net). */
	public static final int SCREEN_WINDOW = 300;
	private static final int SCREEN_TIMEOUT = 900;
	/** A heart demon comes at most this often (ticks), and only from Nascent Soul. */
	private static final int DEMON_COOLDOWN = 6000;

	// Qi wisps
	private static final int WISP_COUNT = 6;
	private static final int WISP_EVERY = 30;
	private static final int WISP_LIFE = 180;
	private static final double WISP_REACH_SQR = 1.4 * 1.4;
	/** Each wisp is worth this many seconds of the meditation's current rate. */
	public static final double WISP_SECONDS = 6;

	// Leyline sequence
	private static final int CUE_EVERY = 18;
	private static final int CUE_GLOW = 14;
	private static final int SEQUENCE_TIME = 400;
	public static final double SEQUENCE_SECONDS = 30;

	// Heart demon
	private static final int DEMON_TIME = 1200;
	public static final double DEMON_SECONDS = 120;
	public static final double DEMON_QI_LOSS = 0.3;

	private static final class State {
		int nextAt = FIRST_SURGE;
		Game active;
		Game last;
		int startedAt;
		// Wisps
		final List<Vec3> wisps = new ArrayList<>();
		final List<Integer> wispBorn = new ArrayList<>();
		int wispsSpawned, wispsCaught;
		// Sequence
		final List<BlockPos> tiles = new ArrayList<>();
		int cueShown, stepIndex;
		boolean inputPhase;
		int inputEndsAt;
		// Demon
		UUID demon;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();
	/** When each player last met their heart demon (server tick), across meditations. */
	private static final Map<UUID, Integer> LAST_DEMON = new HashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();

	/** The game running for this player right now, or null. */
	public static Game active(UUID player) {
		State s = STATES.get(player);
		return s == null ? null : s.active;
	}

	/** Called every meditation tick with the session's age. */
	public static void tick(ServerPlayer p, int ticks, boolean settled) {
		State s = STATES.computeIfAbsent(p.getUUID(), id -> new State());
		if (s.active == null) {
			if (ticks >= s.nextAt && settled) begin(p, s, pick(p, s, ticks), ticks);
			return;
		}
		switch (s.active) {
			case CIRCUIT, ELEMENTS, BREATH -> { if (ticks - s.startedAt > SCREEN_TIMEOUT) finish(p, s, ticks); }
			case WISPS -> tickWisps(p, s, ticks);
			case SEQUENCE -> tickSequence(p, s, ticks);
			case HEART_DEMON -> tickDemon(p, s, ticks);
		}
	}

	/** Starts {@code game} now (also for tests and commands). */
	public static void begin(ServerPlayer p, Game game) {
		State s = STATES.computeIfAbsent(p.getUUID(), id -> new State());
		if (s.active != null) end(p);
		s = STATES.computeIfAbsent(p.getUUID(), id -> new State());
		begin(p, s, game, MeditationManager.sessionTicks(p));
	}

	private static Game pick(ServerPlayer p, State s, int ticks) {
		List<Game> pool = new ArrayList<>();
		for (Game g : Game.values()) {
			if (g == s.last) continue; // never the same twice running
			int weight = 0;
			if (g.screen) weight = 1;
			else if (InnerRealm.isInside(p)) {
				if (g == Game.HEART_DEMON) {
					boolean ready = CultivationManager.get(p).getRealm().ordinal() >= Realm.NASCENT_SOUL.ordinal()
							&& (!LAST_DEMON.containsKey(p.getUUID())
									|| p.server.getTickCount() - LAST_DEMON.get(p.getUUID()) >= DEMON_COOLDOWN);
					weight = ready ? 2 : 0;
				} else weight = 3;
			}
			for (int i = 0; i < weight; i++) pool.add(g);
		}
		return pool.isEmpty() ? Game.CIRCUIT : pool.get(RANDOM.nextInt(pool.size()));
	}

	private static void begin(ServerPlayer p, State s, Game game, int ticks) {
		s.active = game;
		s.last = game;
		s.startedAt = ticks;
		ServerLevel level = p.serverLevel();
		if (game.screen) {
			ModPackets.sendSurge(p, game.ordinal(), SCREEN_WINDOW);
			return;
		}
		level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9f, 1.3f);
		switch (game) {
			case WISPS -> {
				s.wisps.clear();
				s.wispBorn.clear();
				s.wispsSpawned = s.wispsCaught = 0;
				p.displayClientMessage(Component.translatable(ModLang.MSG_WISPS), true);
			}
			case SEQUENCE -> {
				s.tiles.clear();
				s.cueShown = s.stepIndex = 0;
				s.inputPhase = false;
				BlockPos centre = InnerRealm.islandCentre(p);
				int radius = InnerRealm.islandRadius(CultivationManager.get(p).getRealm());
				int length = 3 + Math.min(3, CultivationManager.get(p).getRealm().ordinal());
				for (int tries = 0; s.tiles.size() < length && tries < 200; tries++) {
					int dx = RANDOM.nextInt(radius * 2 + 1) - radius, dz = RANDOM.nextInt(radius * 2 + 1) - radius;
					if (dx * dx + dz * dz > (radius - 0.5) * (radius - 0.5) || (dx == 0 && dz == 0)) continue;
					BlockPos tile = centre.offset(dx, 0, dz);
					if (!s.tiles.contains(tile) && level.getBlockState(tile).is(ModBlocks.SOUL_CRYSTAL)) s.tiles.add(tile);
				}
				p.displayClientMessage(Component.translatable(ModLang.MSG_SEQUENCE), true);
			}
			case HEART_DEMON -> {
				LAST_DEMON.put(p.getUUID(), p.server.getTickCount());
				HeartDemonEntity demon = HeartDemonEntity.summon(p);
				s.demon = demon == null ? null : demon.getUUID();
				if (demon == null) finish(p, s, ticks);
				else p.displayClientMessage(Component.translatable(ModLang.MSG_DEMON), true);
			}
			default -> { }
		}
	}

	/** Ends the current game (if any) and schedules the next surge. */
	private static void finish(ServerPlayer p, State s, int ticks) {
		if (s.active == Game.HEART_DEMON && s.demon != null && p.serverLevel().getEntity(s.demon) instanceof HeartDemonEntity demon) {
			demon.discard();
		}
		if (s.active == Game.SEQUENCE) for (BlockPos tile : s.tiles) SoulCrystalBlock.setGlow(p.serverLevel(), tile, 0, 0);
		s.active = null;
		s.demon = null;
		s.wisps.clear();
		s.wispBorn.clear();
		s.tiles.clear();
		s.nextAt = ticks + INTERVAL_MIN + RANDOM.nextInt(INTERVAL_MAX - INTERVAL_MIN + 1);
	}

	/** Meditation ended: whatever was running goes, and the next session starts the clock afresh. */
	public static void end(ServerPlayer p) {
		State s = STATES.remove(p.getUUID());
		if (s == null) return;
		if (s.active != null && s.active.screen) ModPackets.sendSurge(p, -1, 0);
		finish(p, s, 0);
	}

	public static void forget(UUID id) {
		STATES.remove(id);
		LAST_DEMON.remove(id);
	}

	/**
	 * A screen game's result from the client. Returns true if a screen game was running (so the result counts); either
	 * way it is over.
	 */
	public static boolean acceptScreenResult(ServerPlayer p, int ticks) {
		State s = STATES.get(p.getUUID());
		if (s == null || s.active == null || !s.active.screen) return false;
		finish(p, s, ticks);
		return true;
	}

	// --- Qi wisps ---

	private static void tickWisps(ServerPlayer p, State s, int ticks) {
		ServerLevel level = p.serverLevel();
		int age = ticks - s.startedAt;
		if (s.wispsSpawned < WISP_COUNT && age % WISP_EVERY == 0) {
			BlockPos centre = InnerRealm.islandCentre(p);
			int radius = InnerRealm.islandRadius(CultivationManager.get(p).getRealm());
			double a = RANDOM.nextDouble() * Math.PI * 2, r = 1.5 + RANDOM.nextDouble() * (radius - 1.5);
			s.wisps.add(new Vec3(centre.getX() + 0.5 + Math.cos(a) * r, centre.getY() + 1.6 + RANDOM.nextDouble() * 0.6,
					centre.getZ() + 0.5 + Math.sin(a) * r));
			s.wispBorn.add(ticks);
			s.wispsSpawned++;
			level.playSound(null, BlockPos.containing(s.wisps.get(s.wisps.size() - 1)), SoundEvents.AMETHYST_CLUSTER_STEP,
					SoundSource.PLAYERS, 0.6f, 1.6f);
		}
		Vec3 body = p.position().add(0, 0.9, 0);
		for (int i = s.wisps.size() - 1; i >= 0; i--) {
			int born = s.wispBorn.get(i);
			Vec3 at = s.wisps.get(i).add(Math.sin((ticks + born) * 0.07) * 0.02, Math.sin((ticks + born) * 0.11) * 0.01,
					Math.cos((ticks + born) * 0.07) * 0.02);
			s.wisps.set(i, at);
			if (at.distanceToSqr(body) <= WISP_REACH_SQR) {
				s.wispsCaught++;
				MeditationManager.addBonusSeconds(p, WISP_SECONDS);
				level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.08);
				level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f,
						1.0f + 0.15f * s.wispsCaught);
				remove(s, i);
			} else if (ticks - born > WISP_LIFE) {
				level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.01);
				remove(s, i);
			} else if (ticks % 2 == 0) {
				float fade = 1 - (ticks - born) / (float) WISP_LIFE;
				level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.06, 0.06, 0.06, 0.004);
				if (fade > 0.3f) level.sendParticles(ParticleTypes.GLOW, at.x, at.y, at.z, 1, 0.12, 0.12, 0.12, 0.0);
			}
		}
		if (s.wispsSpawned >= WISP_COUNT && s.wisps.isEmpty()) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_WISPS_DONE, s.wispsCaught, WISP_COUNT), true);
			if (s.wispsCaught * 2 >= WISP_COUNT) MeditationManager.harmony(p);
			finish(p, s, ticks);
		}
	}

	private static void remove(State s, int i) {
		s.wisps.remove(i);
		s.wispBorn.remove(i);
	}

	// --- Leyline sequence ---

	private static void tickSequence(ServerPlayer p, State s, int ticks) {
		if (s.tiles.isEmpty()) {
			finish(p, s, ticks);
			return;
		}
		ServerLevel level = p.serverLevel();
		int age = ticks - s.startedAt;
		if (!s.inputPhase) {
			if (age >= CUE_EVERY && age % CUE_EVERY == 0 && s.cueShown < s.tiles.size()) {
				BlockPos tile = s.tiles.get(s.cueShown);
				SoulCrystalBlock.setGlow(level, tile, SoulCrystalBlock.GLOW_CUE, CUE_GLOW);
				level.playSound(null, tile, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, pitch(s.cueShown));
				s.cueShown++;
			} else if (s.cueShown >= s.tiles.size() && age >= CUE_EVERY * (s.tiles.size() + 1)) {
				s.inputPhase = true;
				s.inputEndsAt = ticks + SEQUENCE_TIME;
				p.displayClientMessage(Component.translatable(ModLang.MSG_SEQUENCE_GO), true);
			}
			return;
		}
		if (ticks > s.inputEndsAt) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SEQUENCE_FAIL), true);
			finish(p, s, ticks);
		}
	}

	private static float pitch(int index) { return (float) Math.pow(2, (index * 2) / 12.0) * 0.8f; }

	/** A soul stepped onto a Soul Crystal tile (see SoulCrystalBlock#stepOn). */
	public static void onStep(ServerPlayer p, BlockPos pos) {
		State s = STATES.get(p.getUUID());
		if (s == null || s.active != Game.SEQUENCE || !s.inputPhase) return;
		int index = s.tiles.indexOf(pos);
		if (index < 0 || index < s.stepIndex) return; // other tiles, and those already walked, don't count
		ServerLevel level = p.serverLevel();
		if (index != s.stepIndex) {
			level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.9f, 0.6f);
			p.displayClientMessage(Component.translatable(ModLang.MSG_SEQUENCE_FAIL), true);
			finish(p, s, MeditationManager.sessionTicks(p));
			return;
		}
		SoulCrystalBlock.setGlow(level, pos, SoulCrystalBlock.GLOW_CUE, CUE_GLOW);
		level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.9f, pitch(index));
		s.stepIndex++;
		if (s.stepIndex >= s.tiles.size()) {
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.5f);
			level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.6, 0.4, 0.6, 0.05);
			MeditationManager.addBonusSeconds(p, SEQUENCE_SECONDS);
			MeditationManager.harmony(p);
			finish(p, s, MeditationManager.sessionTicks(p));
		}
	}

	// --- Heart demon ---

	private static void tickDemon(ServerPlayer p, State s, int ticks) {
		HeartDemonEntity demon = s.demon != null && p.serverLevel().getEntity(s.demon) instanceof HeartDemonEntity d ? d : null;
		if (demon == null || !demon.isAlive()) {
			// Slain (HeartDemonEntity#die calls onDemonSlain first, so reaching here means it vanished some other way).
			finish(p, s, ticks);
			return;
		}
		if (ticks - s.startedAt > DEMON_TIME) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_DEMON_FADE), true);
			finish(p, s, ticks);
		}
	}

	/** The player slew their heart demon. */
	public static void onDemonSlain(ServerPlayer p) {
		State s = STATES.get(p.getUUID());
		if (s == null || s.active != Game.HEART_DEMON) return;
		s.demon = null;
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.8f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_DEMON_WIN), true);
		MeditationManager.addBonusSeconds(p, DEMON_SECONDS);
		MeditationManager.harmony(p);
		finish(p, s, MeditationManager.sessionTicks(p));
	}

	/**
	 * Damage to a soul in the Inner Realm. It never breaks the meditation, falls don't hurt, and a blow that would kill
	 * means the heart demon has won: the soul is thrown back to its body, shaken and drained of qi. Returns whether the
	 * damage goes ahead.
	 */
	public static boolean allowInnerDamage(ServerPlayer p, DamageSource source, float amount) {
		if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return false;
		if (amount < p.getHealth()) return true;
		PlayerCultivation c = CultivationManager.get(p);
		c.setQi(c.getQi() - c.maxQi() * DEMON_QI_LOSS);
		p.setHealth(Math.max(1.0f, p.getMaxHealth() * 0.2f));
		p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
		p.displayClientMessage(Component.translatable(ModLang.MSG_DEMON_LOSE), true);
		MeditationManager.stop(p, true);
		CultivationManager.sync(p);
		return false;
	}

	private QiSurges() {}
}
