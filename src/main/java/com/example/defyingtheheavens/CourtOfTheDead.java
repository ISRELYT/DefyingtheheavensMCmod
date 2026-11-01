package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Court of the Dead (CultivationMethod#COURT_OF_THE_DEAD; the methods overhaul's phase 3): a Ghost King of the players (GhostSystem#isKing)
 * practising it takes a ghost palace whose throne stands empty (its king slain: GhostPalaces) by standing at the throne
 * {@link #CLAIM_SECONDS} seconds: the palace is its, and its court's retainers its subjects (GhostCultivatorNpc#isAlly: their grudge let go,
 * they turn on it no more, and no NPC king rises to that throne while it holds it). Holding court, standing within its palace, every
 * {@link #TRIBUTE_SECONDS} seconds each living subject brings tribute: {@link #SHARE} of the ghost path's stage, twice as much for each realm
 * the subject stands above the king and half for each below, never less than {@link #FLOOR} (CultivationMethods#grant). One no longer a
 * Ghost King (born again, its path fallen) gives the throne up, empty for whoever comes.
 */
public final class CourtOfTheDead {
	public static final int CLAIM_SECONDS = 5, TRIBUTE_SECONDS = 60;
	public static final double CLAIM_REACH = 4, SHARE = 0.02, FLOOR = 0.0025;
	/** How near its throne a king holds court: within its palace (41 blocks across: GhostPalacePiece#HALF), and a little. */
	public static final double COURT_REACH = GhostPalacePiece.HALF + 2;

	/** How long each king has stood at an empty throne, and how long since its last tribute (seconds); not saved. */
	private static final Map<UUID, Integer> STANDING = new HashMap<>(), SINCE_TRIBUTE = new HashMap<>();

	/** Each second of a player (NetherworldManager#second): a throne given up, taken, or its tribute brought. */
	static void second(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		if (!ModDimensions.isNetherworld(level.dimension())) return;
		UUID id = p.getUUID();
		BlockPos held = GhostPalaces.heldBy(level, id);
		PlayerCultivation c = CultivationManager.get(p);
		boolean king = GhostSystem.isKing(p);
		if (held != null && !king) {
			GhostPalaces.vacate(level, held);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_COURT_LOST).withStyle(ChatFormatting.GRAY));
			return;
		}
		if (!king || CultivationMethods.practised(c, TwoPaths.Path.GHOST) != CultivationMethod.COURT_OF_THE_DEAD
				|| !CultivationMethods.holdsVolume(c, TwoPaths.Path.GHOST)) {
			STANDING.remove(id);
			return;
		}
		if (held == null) {
			BlockPos throne = GhostPalaces.vacantNear(level, p.blockPosition(), CLAIM_REACH);
			if (throne == null) {
				STANDING.remove(id);
				return;
			}
			if (STANDING.merge(id, 1, Integer::sum) < CLAIM_SECONDS) return;
			STANDING.remove(id);
			GhostPalaces.claim(level, throne, id);
			SINCE_TRIBUTE.put(id, 0);
			for (GhostCultivatorNpc subject : subjects(level, throne)) {
				if (subject.getTarget() == p) subject.setTarget(null);
				if (subject.getLastHurtByMob() == p) subject.setLastHurtByMob(null);
			}
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, throne.getX() + 0.5, throne.getY() + 1, throne.getZ() + 0.5, 60, 0.6, 1.0, 0.6, 0.04);
			level.playSound(null, throne, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 0.5f);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_COURT_CLAIMED, subjects(level, throne).size()).withStyle(ChatFormatting.LIGHT_PURPLE));
			CultivationManager.markDirty(p.server);
			return;
		}
		if (p.position().distanceToSqr(Vec3.atCenterOf(held)) > COURT_REACH * COURT_REACH) return; // away from its palace: no court held
		if (SINCE_TRIBUTE.merge(id, 1, Integer::sum) < TRIBUTE_SECONDS) return;
		SINCE_TRIBUTE.put(id, 0);
		List<GhostCultivatorNpc> subjects = subjects(level, held);
		if (subjects.isEmpty()) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_COURT_EMPTY).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		Realm realm = GhostMethods.ghostRealm(c.netherworld());
		double share = 0;
		for (GhostCultivatorNpc subject : subjects) {
			share += tribute(subject.getCultivationRealm().ordinal(), realm == null ? 0 : realm.ordinal());
			level.sendParticles(ParticleTypes.SOUL, subject.getX(), subject.getY() + 1.2, subject.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
		}
		CultivationMethods.grant(p, TwoPaths.Path.GHOST, CultivationMethod.COURT_OF_THE_DEAD, share);
	}

	/** A subject of {@code subject}'s realm's tribute to a king whose ghost path stands in {@code king} (realm ordinals). */
	public static double tribute(int subject, int king) {
		return Math.max(FLOOR, SHARE * Math.pow(2, subject - king));
	}

	/** The court's retainers of the palace whose throne is {@code throne}, alive: a player king's subjects. */
	static List<GhostCultivatorNpc> subjects(ServerLevel level, BlockPos throne) {
		return level.getEntitiesOfClass(GhostCultivatorNpc.class, new AABB(throne).inflate(COURT_REACH, 16, COURT_REACH),
				ghost -> ghost.isAlive() && ghost.servesThrone(throne));
	}

	/** A player left. */
	public static void forget(UUID id) {
		STANDING.remove(id);
		SINCE_TRIBUTE.remove(id);
	}

	private CourtOfTheDead() {}
}
