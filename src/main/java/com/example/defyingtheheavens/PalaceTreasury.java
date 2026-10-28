package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A ghost palace's treasury (GhostPalacePiece#treasury): a vault beside the hall, its chests holding the dead's wealth (spirit
 * stones, Yin Jade, Ghost Jade Slips, the Netherworld's herbs and pills). Its king keeps it, as a sect's elders keep theirs
 * (SectManager#treasurySeal): while the king lives and stands in its palace (loaded), its seal holds the chests shut and
 * unbreakable. One a full realm above the king, by the stronger of their two paths (TwoPaths#body), breaks it with a touch, and it
 * lies open {@link #SEAL_RECAST}; a king slain, or its throne empty, leaves it open to whoever comes. The dead may open it too: it
 * is their own kind's (CultivationEvents lets a ghost's hand on its chests). Laying hands on it is theft, whether it opens or not:
 * the king and its retainers turn on the thief, and think the worse of them once a day.
 */
public final class PalaceTreasury {
	/** The treasury chests' loot (data/defying-the-heavens/loot_tables/chests/ghost_palace_treasury.json). */
	public static final net.minecraft.resources.ResourceLocation LOOT = DefyingTheHeavens.id("chests/ghost_palace_treasury");
	/** How far from its throne (sideways) a palace's chests lie, and up or down: the platform is 41 blocks square. */
	private static final int REACH = GhostPalacePiece.HALF + 2, REACH_Y = 8;
	/** How long a broken seal stays broken before its king casts it anew (5 minutes), as a sect's elders do theirs. */
	public static final long SEAL_RECAST = 6000;

	/** Each palace's seal (by its throne), broken until this game time. Not saved: a restart finds it whole. */
	private static final Map<BlockPos, Long> BROKEN = new HashMap<>();
	/** The game day each thief last robbed each palace (throne, player): it counts against them once a day. */
	private static final Map<String, Long> ROBBED = new HashMap<>();

	/** The throne of the palace whose walls {@code pos} lies within, or null (outside the Netherworld, none). */
	static BlockPos palaceOf(ServerLevel level, BlockPos pos) {
		if (!ModDimensions.isNetherworld(level.dimension())) return null;
		for (BlockPos throne : GhostPalaces.get(level).thrones().keySet()) {
			if (Math.abs(throne.getX() - pos.getX()) <= REACH && Math.abs(throne.getZ() - pos.getZ()) <= REACH
					&& Math.abs(throne.getY() - pos.getY()) <= REACH_Y) return throne;
		}
		return null;
	}

	/**
	 * Whether {@code pos} is one of a palace's treasury chests (a palace keeps no other chest), or since 2026-10-28 one of its storage
	 * formations (StorageFormations: the treasuries raised since keep their wealth in those; one a player set down is theirs, under its
	 * own seal).
	 */
	public static boolean isTreasury(ServerLevel level, BlockPos pos) {
		return (level.getBlockEntity(pos) instanceof ChestBlockEntity
				|| level.getBlockEntity(pos) instanceof StorageFormationBlockEntity formation && formation.getOwner() == null)
				&& palaceOf(level, pos) != null;
	}

	/** The king keeping {@code throne}'s treasury: alive and loaded; null if slain, its throne empty, or away. */
	static GhostCultivatorNpc keeper(ServerLevel level, BlockPos throne) {
		UUID id = GhostPalaces.get(level).thrones().get(throne);
		return id != null && level.getEntity(id) instanceof GhostCultivatorNpc king && king.isAlive() ? king : null;
	}

	/** The realm of the seal on {@code throne}'s treasury (its king's), or null while it lies open. */
	public static Realm seal(ServerLevel level, BlockPos throne) {
		Long brokenUntil = BROKEN.get(throne);
		if (brokenUntil != null && level.getGameTime() < brokenUntil) return null;
		GhostCultivatorNpc king = keeper(level, throne);
		return king == null ? null : CultivatorNpc.realmOf(king.getRank());
	}

	/**
	 * {@code player} lays hands on the treasury chest at {@code pos} (opening or breaking it). Theft either way (the court turns on
	 * them). Returns whether it gives: open (no keeper, or its seal broken); else sealed, the player told so, or the seal broken now
	 * by one a realm above the king (who opens it with the next touch). A creative player, or a chest outside any palace, is let be.
	 */
	public static boolean touch(ServerLevel level, ServerPlayer player, BlockPos pos) {
		BlockPos throne = palaceOf(level, pos);
		if (throne == null || player.isCreative()) return true;
		theft(level, player, throne);
		Realm seal = seal(level, throne);
		if (seal == null) return true;
		int strength = TwoPaths.body(CultivationManager.get(player), true).rank();
		if (!seal.isLast() && strength >= PlayerCultivation.rank(seal.next(), Stage.EARLY)) {
			BROKEN.put(throne.immutable(), level.getGameTime() + SEAL_RECAST);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.05);
			level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 0.6f);
			player.displayClientMessage(Component.translatable(ModLang.MSG_PALACE_SEAL_BROKEN), true);
			return false;
		}
		player.displayClientMessage(Component.translatable(ModLang.MSG_PALACE_SEALED, seal.getDisplayName()), true);
		return false;
	}

	/** Whether the treasury chest at {@code pos} is sealed shut now (for the hand that isn't the one reaching: no alarm). */
	public static boolean sealed(ServerLevel level, BlockPos pos) {
		BlockPos throne = palaceOf(level, pos);
		return throne != null && seal(level, throne) != null;
	}

	/** Theft from {@code throne}'s treasury: its king and retainers that sense the thief turn on them; once a day, less regard. */
	private static void theft(ServerLevel level, ServerPlayer player, BlockPos throne) {
		long day = Regard.day(level.getGameTime());
		Long before = ROBBED.put(throne.asLong() + ":" + player.getUUID(), day);
		boolean first = before == null || before != day;
		AABB court = new AABB(throne).inflate(REACH, REACH_Y, REACH);
		for (GhostCultivatorNpc npc : level.getEntitiesOfClass(GhostCultivatorNpc.class, court, n -> n.isAlive()
				&& (n.getTitle() == NpcTitle.GHOST_KING || n.getTitle() == NpcTitle.GHOST_RETAINER))) {
			if (first) npc.changeRegard(player, Regard.TREASURY_ROBBED);
			if (npc.senses(player) && npc.getTarget() == null) npc.setTarget(player);
		}
	}

	/** A server stopping: no seal stays broken, no theft remembered. */
	public static void clear() {
		BROKEN.clear();
		ROBBED.clear();
	}

	private PalaceTreasury() {}
}
