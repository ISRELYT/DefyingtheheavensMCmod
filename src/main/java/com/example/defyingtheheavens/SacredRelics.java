package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.player.Player;

/**
 * Who keeps a Sacred Vessel's altar (SacredRelicFeature; the realm plan's "guarded", 2026-10-25): the first time anyone comes within
 * {@link #WAKE_REACH} of an altar whose vessel still lies on it (in its frame), a Relic Keeper rises there, one of Void Shattering
 * (NpcTitle#RELIC_KEEPER): it keeps to the altar ({@link #ROAM}: CultivatorGoals.WanderGoal) and goes for any player who comes within
 * {@link #GUARD_REACH} of it, whatever their path (CultivatorGoals.HostilityGoal). Beaten, the vessel is theirs to take; no other keeper
 * comes to that altar (its frame is marked, {@link #KEPT}).
 */
public final class SacredRelics {
	/** An altar's keeper rises when someone comes this near it; keeps this near it; goes for a player this near it. */
	static final double WAKE_REACH = 24, GUARD_REACH = 12;
	public static final int ROAM = 6;
	/** The mark on an altar's frame once its keeper has risen. */
	static final String KEPT = DefyingTheHeavens.MOD_ID + ".relic_kept";

	/** Every two seconds: round each player in the Great Void, an altar not yet kept gets its keeper. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 40 != 21) return;
		ServerLevel level = server.getLevel(ModDimensions.GREAT_VOID);
		if (level == null) return;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			for (GlowItemFrame frame : level.getEntitiesOfClass(GlowItemFrame.class, player.getBoundingBox().inflate(WAKE_REACH),
					f -> f.getItem().getItem() instanceof SacredVesselItem && !f.getTags().contains(KEPT))) {
				frame.addTag(KEPT);
				rise(level, frame.blockPosition(), level.getRandom());
			}
		}
	}

	/** A keeper rising at the altar whose vessel lies at {@code altar}: of Void Shattering, any stage, of any path, beside the plinth. */
	static CultivatorNpc rise(ServerLevel level, BlockPos altar, RandomSource random) {
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		if (npc == null) return null;
		npc.moveTo(altar.getX() + 0.5 + (random.nextBoolean() ? 2 : -2), altar.getY() - 1, altar.getZ() + 0.5, random.nextFloat() * 360, 0);
		npc.becomeRogue(PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.byIndex(random.nextInt(3))), Alignment.randomRogue(random), random);
		npc.setTitle(NpcTitle.RELIC_KEEPER);
		npc.setSettledAt(altar);
		level.addFreshEntity(npc);
		level.playSound(null, altar, net.minecraft.sounds.SoundEvents.WARDEN_EMERGE, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.8f);
		return npc;
	}

	/** Whether {@code npc} keeps an altar. */
	public static boolean isKeeper(CultivatorNpc npc) {
		return npc.getTitle() == NpcTitle.RELIC_KEEPER && npc.settledAt() != null;
	}

	/** Whether keeper {@code npc} goes for {@code target}: a player (not in creative) come within {@link #GUARD_REACH} of its altar. */
	public static boolean guards(CultivatorNpc npc, LivingEntity target) {
		if (!(target instanceof Player player) || player.isCreative() || player.isSpectator()) return false;
		BlockPos altar = npc.settledAt();
		return target.distanceToSqr(altar.getX() + 0.5, altar.getY(), altar.getZ() + 0.5) <= GUARD_REACH * GUARD_REACH;
	}

	private SacredRelics() {}
}
