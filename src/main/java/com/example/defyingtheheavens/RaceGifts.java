package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What each demon race is born with (the user's design, 2026-10-23), whatever path it walks:
 * <ul>
 *   <li><b>Tiger:</b> its blows {@link #TIGER_HARM} as hard; it sees in the dark; from Core Formation, Tiger's Roar ({@link #roar}).</li>
 *   <li><b>Lizard:</b> its blows venomous ({@link #onMeleeHit}); Shed Skin ({@link #onHurt}); it swims fast and breathes underwater.</li>
 *   <li><b>Fox:</b> Charm (NPCs think {@link #CHARM} the better of it); from Core Formation, Illusion ({@link #illusion}); its tails.</li>
 *   <li><b>Dragon:</b> Qi Flight a realm sooner (PlayerCultivation#canFlyOnQi); {@link #DRAGON_HEALTH} the health; Dragon's Might on
 *       what stands far below it; and it cultivates the Demon Path the slowest (DemonPath#racePace).</li>
 * </ul>
 * A half-blood (DemonPath#isHalfBlood: a human with a race's blood in it, the user's ask, 2026-10-24) has them at half strength
 * ({@link #strength}): the blows ×1.075, the venom, roar and Illusion half as long (the roar milder), Shed Skin to 37.5%, Charm 5, a
 * dragon's health ×1.1 and its might half as far; sight in the dark and breath under water whole (they can't be halved), but not a
 * lizard's swift swimming nor a dragon's early flight.
 */
public final class RaceGifts {
	public static final float TIGER_HARM = 1.15f;
	/** Tiger's Roar: how far, for how long (ticks) its weakness and slowness last, its cooldown (ticks), its qi (of the pool). */
	public static final int ROAR_RADIUS = 10, ROAR_TICKS = 100, ROAR_COOLDOWN = 600;
	public static final double ROAR_QI = 0.10;
	/** It cows those this many stages (or more) below the tiger. */
	public static final int ROAR_STAGES_BELOW = 2;
	/** Shed Skin: below this share of its health it sheds, healed to the other share; once in this long (ticks: ten minutes). */
	public static final float SHED_BELOW = 0.25f, SHED_TO = 0.5f;
	public static final int SHED_COOLDOWN = 12000;
	/** A lizard's venom, this long (ticks). */
	public static final int VENOM_TICKS = 100;
	/** Illusion: how long unseen (ticks), its cooldown (ticks), its qi (of the pool). */
	public static final int ILLUSION_TICKS = 120, ILLUSION_COOLDOWN = 900;
	public static final double ILLUSION_QI = 0.10;
	public static final int CHARM = 10;
	public static final double DRAGON_HEALTH = 1.2;
	/** Dragon's Might: how far, over those this many realms (or more) below it. */
	public static final double MIGHT_RADIUS = 8;
	public static final int MIGHT_REALMS_BELOW = 2;

	/** When each may roar, shed its skin, or cast an illusion again (game time). */
	private static final Map<UUID, Long> ROAR_READY = new HashMap<>(), SHED_READY = new HashMap<>(), ILLUSION_READY = new HashMap<>();
	/** Those unseen by their Illusion now, and until when: a blow of theirs breaks it. */
	private static final Map<UUID, Long> ILLUSION_UNTIL = new HashMap<>();

	/** The race whose gifts {@code player} has: its own, or a half-blood's (DemonPath#race). */
	private static Bloodline race(ServerPlayer player) {
		return DemonPath.race(CultivationManager.get(player));
	}

	/** How strong they are in it: a demon's whole, a half-blood's half (DemonPath#giftStrength). */
	private static double strength(ServerPlayer player) {
		return DemonPath.giftStrength(CultivationManager.get(player));
	}

	/** {@code c}'s rank for the gifts that weigh it: the highest of its paths (-1 none). */
	private static int rank(PlayerCultivation c) {
		return TwoPaths.higherRank(c, true);
	}

	/**
	 * Where another stands to be cowed or pressed: a player by its body (TwoPaths#weighedRank), a cultivator by its rank, a demon beast
	 * (or any other CultivatorEntity) by its own, anything else as a mortal. Before the sweep of 2026-10-23 a beast stood as a mortal:
	 * a roar cowed a Core Formation one, and a dragon's might slowed it.
	 */
	private static int rankOf(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) return TwoPaths.weighedRank(CultivationManager.get(player));
		if (entity instanceof CultivatorNpc npc) return npc.isMortal() ? -1 : npc.getRank();
		if (entity instanceof CultivatorEntity other) return other.isMortal() ? -1 : PlayerCultivation.rank(other.getCultivationRealm(), other.getCultivationStage());
		return -1;
	}

	/**
	 * Who a race's gift may weigh on: not its own, the spared (HeavenSpells#spared: allies, pets, players it may not harm), nor a soul or
	 * one of the dead out of the living's reach (Incorporeal).
	 */
	private static boolean reaches(ServerPlayer from, LivingEntity other) {
		return other != from && other.isAlive() && !HeavenSpells.spared(from, other) && !(other instanceof Player player && Incorporeal.is(player));
	}

	/** Whether {@code player} may use a race's act now: alive and in the world, in a body (no soul out, none of the ruled dead). */
	private static boolean mayAct(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator()) return false;
		if (SoulProjection.isAwayFromBody(player.getUUID()) || SoulProjection.isSoul(player)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return false;
		}
		if (Incorporeal.isRuledGhost(player)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_CANNOT), true);
			return false;
		}
		return true;
	}

	// --- Once a second ---

	/** The standing gifts, once a second: a tiger's sight in the dark, a lizard's in the water, a dragon's might. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive() || player.isSpectator()) continue;
			switch (race(player)) {
				case TIGER -> player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
				case LIZARD -> {
					if (player.isInWaterOrBubble()) {
						player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, false));
						// The swift swimming a whole lizard's: a half-blood only breathes there.
						if (strength(player) >= 1) player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, false));
					}
				}
				case DRAGON -> dragonsMight(player);
				default -> {
				}
			}
		}
		long now = server.overworld().getGameTime();
		ILLUSION_UNTIL.values().removeIf(until -> until < now);
	}

	/**
	 * Dragon's Might: those within {@link #MIGHT_RADIUS} standing {@link #MIGHT_REALMS_BELOW} realms or more below it are slowed while near;
	 * not by a dragon's soul out of its body or its ghost, nor on those it spares ({@link #reaches}).
	 */
	private static void dragonsMight(ServerPlayer dragon) {
		if (Incorporeal.is(dragon) || SoulProjection.isAwayFromBody(dragon.getUUID())) return;
		int realm = rank(CultivationManager.get(dragon));
		if (realm < 0) return;
		int stages = Stage.values().length;
		int dragonRealm = realm / stages;
		double radius = MIGHT_RADIUS * strength(dragon); // a half-blood's, half as far
		for (LivingEntity other : dragon.serverLevel().getEntitiesOfClass(LivingEntity.class, dragon.getBoundingBox().inflate(radius),
				e -> reaches(dragon, e) && e.distanceToSqr(dragon) <= radius * radius)) {
			int otherRank = rankOf(other);
			int otherRealm = otherRank < 0 ? -1 : otherRank / stages;
			if (dragonRealm - otherRealm < MIGHT_REALMS_BELOW) continue;
			other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, true, true, true), dragon);
		}
	}

	// --- Blows ---

	/**
	 * What {@code caster}'s blow from {@code source} comes to: a tiger's own blows of the hand (a player's attack: not its spells, nor
	 * a flight's smash or a core's blast, which also came {@link #TIGER_HARM} as hard before the sweep of 2026-10-23) the harder.
	 */
	public static float outgoing(ServerPlayer caster, DamageSource source, float amount) {
		if (race(caster) != Bloodline.TIGER || source.getDirectEntity() != caster || !source.is(DamageTypes.PLAYER_ATTACK) || HeavenSpells.isQi(source)) return amount;
		return (float) (amount * (1 + (TIGER_HARM - 1) * strength(caster)));
	}

	/** {@code attacker}'s own blow lands on {@code target}: a lizard's venom (stronger from Core Formation, again from Heavenly Being); a fox's Illusion broken. */
	public static void onMeleeHit(ServerPlayer attacker, LivingEntity target) {
		Bloodline race = race(attacker);
		if (race == Bloodline.LIZARD) {
			int realm = Math.max(0, rank(CultivationManager.get(attacker))) / Stage.values().length;
			int amplifier = realm >= Realm.HEAVENLY_BEING.ordinal() ? 2 : realm >= Realm.CORE_FORMATION.ordinal() ? 1 : 0;
			target.addEffect(new MobEffectInstance(MobEffects.POISON, (int) Math.round(VENOM_TICKS * strength(attacker)), amplifier), attacker);
		}
		breakIllusion(attacker);
	}

	/**
	 * A fox's Illusion broken, if it is up: by a blow of its own, any harm it does (an arrow, a spell: Physiques#onHurt) or a spell it casts
	 * (Spells#cast). Before the sweep of 2026-10-23 only a blow of the hand broke it, so a fox unseen could cast and shoot freely.
	 */
	public static void breakIllusion(ServerPlayer fox) {
		if (ILLUSION_UNTIL.remove(fox.getUUID()) == null) return;
		fox.removeEffect(MobEffects.INVISIBILITY);
		fox.displayClientMessage(Component.translatable(ModLang.MSG_ILLUSION_BROKEN), true);
	}

	/** {@code victim} has lost health: a lizard brought below {@link #SHED_BELOW} of its health sheds its skin, once in {@link #SHED_COOLDOWN}. */
	public static void onHurt(LivingEntity victim) {
		if (!(victim instanceof ServerPlayer player) || !player.isAlive() || race(player) != Bloodline.LIZARD) return;
		if (player.getHealth() >= player.getMaxHealth() * SHED_BELOW) return;
		long now = player.serverLevel().getGameTime();
		if (SHED_READY.getOrDefault(player.getUUID(), 0L) > now) return;
		SHED_READY.put(player.getUUID(), now + SHED_COOLDOWN);
		player.setHealth((float) (player.getMaxHealth() * (SHED_BELOW + (SHED_TO - SHED_BELOW) * strength(player))));
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.ITEM_SLIME, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.6, 0.4, 0.1);
		level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_TURTLE, SoundSource.PLAYERS, 1.0f, 0.7f);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_SHED_SKIN, SHED_COOLDOWN / 1200).withStyle(ChatFormatting.GREEN));
	}

	// --- The acts ---

	/** Tiger's Roar (Ability#TIGERS_ROAR): those within {@link #ROAR_RADIUS} {@link #ROAR_STAGES_BELOW} stages or more below it weakened and slowed. */
	public static void roar(ServerPlayer tiger) {
		if (!mayAct(tiger)) return;
		PlayerCultivation c = CultivationManager.get(tiger);
		long now = tiger.serverLevel().getGameTime();
		long ready = ROAR_READY.getOrDefault(tiger.getUUID(), 0L);
		if (ready > now) {
			tiger.displayClientMessage(Component.translatable(ModLang.MSG_GIFT_COOLDOWN, (ready - now + 19) / 20), true);
			return;
		}
		double cost = c.maxQi() * ROAR_QI;
		if (!c.consumeQi(cost)) {
			tiger.displayClientMessage(Component.translatable(ModLang.MSG_GIFT_NO_QI, Math.round(cost)), true);
			return;
		}
		ROAR_READY.put(tiger.getUUID(), now + ROAR_COOLDOWN);
		int rank = rank(c);
		double strength = strength(tiger);
		int ticks = (int) Math.round(ROAR_TICKS * strength), amplifier = strength >= 1 ? 1 : 0; // a half-blood's, half as long and milder
		int cowed = 0;
		for (LivingEntity other : tiger.serverLevel().getEntitiesOfClass(LivingEntity.class, tiger.getBoundingBox().inflate(ROAR_RADIUS),
				e -> reaches(tiger, e) && e.distanceToSqr(tiger) <= ROAR_RADIUS * ROAR_RADIUS)) {
			if (rank - rankOf(other) < ROAR_STAGES_BELOW) continue;
			other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, amplifier), tiger);
			other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier), tiger);
			cowed++;
		}
		ServerLevel level = tiger.serverLevel();
		level.playSound(null, tiger.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.6f, 0.9f);
		level.sendParticles(ParticleTypes.SONIC_BOOM, tiger.getX(), tiger.getEyeY() - 0.2, tiger.getZ(), 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.CLOUD, tiger.getX(), tiger.getY() + 1, tiger.getZ(), 30, ROAR_RADIUS * 0.3, 0.4, ROAR_RADIUS * 0.3, 0.05);
		tiger.displayClientMessage(Component.translatable(ModLang.MSG_ROARED, cowed), true);
		CultivationManager.sync(tiger);
	}

	/** Illusion (Ability#ILLUSION): unseen for {@link #ILLUSION_TICKS}; a blow of its own breaks it. */
	public static void illusion(ServerPlayer fox) {
		if (!mayAct(fox)) return;
		PlayerCultivation c = CultivationManager.get(fox);
		long now = fox.serverLevel().getGameTime();
		long ready = ILLUSION_READY.getOrDefault(fox.getUUID(), 0L);
		if (ready > now) {
			fox.displayClientMessage(Component.translatable(ModLang.MSG_GIFT_COOLDOWN, (ready - now + 19) / 20), true);
			return;
		}
		double cost = c.maxQi() * ILLUSION_QI;
		if (!c.consumeQi(cost)) {
			fox.displayClientMessage(Component.translatable(ModLang.MSG_GIFT_NO_QI, Math.round(cost)), true);
			return;
		}
		int unseen = (int) Math.round(ILLUSION_TICKS * strength(fox)); // a half-blood's, half as long
		ILLUSION_READY.put(fox.getUUID(), now + ILLUSION_COOLDOWN);
		ILLUSION_UNTIL.put(fox.getUUID(), now + unseen);
		fox.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, unseen, 0, false, false, true));
		ServerLevel level = fox.serverLevel();
		level.sendParticles(ParticleTypes.WITCH, fox.getX(), fox.getY() + 1, fox.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
		level.playSound(null, fox.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 1.2f);
		fox.displayClientMessage(Component.translatable(ModLang.MSG_ILLUSION, unseen / 20), true);
		CultivationManager.sync(fox);
	}

	// --- Standing ---

	/** What NPCs think the more of {@code c} for its blood: a fox's Charm. */
	public static int regardShift(PlayerCultivation c) {
		return DemonPath.race(c) == Bloodline.FOX ? (int) Math.round(CHARM * DemonPath.giftStrength(c)) : 0;
	}

	/** What {@code c}'s health is multiplied by for its blood: a dragon's the more. */
	public static double healthFactor(PlayerCultivation c) {
		return DemonPath.race(c) == Bloodline.DRAGON ? 1 + (DRAGON_HEALTH - 1) * DemonPath.giftStrength(c) : 1.0;
	}

	/** Whether {@code c} has a race's gift-ability of {@code race}, from Core Formation on any path. */
	public static boolean hasAct(PlayerCultivation c, Bloodline race) {
		return DemonPath.race(c) == race && TwoPaths.awakens(c, Realm.CORE_FORMATION, true) != null;
	}

	/**
	 * {@code player} is leaving: an Illusion up ends (its invisibility with it, else saved on the player to hide them past it as they come
	 * back), while the roar's, the shed's and the Illusion's cooldowns outlast the logout (before the sweep of 2026-10-23 a relog made the
	 * roar and the Illusion ready again at once). They go with the server ({@link #clear}).
	 */
	public static void onDisconnect(ServerPlayer player) {
		if (ILLUSION_UNTIL.remove(player.getUUID()) != null) player.removeEffect(MobEffects.INVISIBILITY);
	}

	/** Everything of {@code id}'s (the game tests). */
	public static void forget(UUID id) {
		ROAR_READY.remove(id);
		ILLUSION_READY.remove(id);
		ILLUSION_UNTIL.remove(id);
	}

	/** The server stopped: game times mean nothing in the next world opened. */
	public static void clear() {
		ROAR_READY.clear();
		SHED_READY.clear();
		ILLUSION_READY.clear();
		ILLUSION_UNTIL.clear();
	}

	/** For the game tests: Shed Skin ready again. */
	static void readyAgain(UUID id) {
		SHED_READY.remove(id);
		ROAR_READY.remove(id);
		ILLUSION_READY.remove(id);
	}

	private RaceGifts() {}
}
