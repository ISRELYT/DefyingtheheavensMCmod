package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The Immortal Realm (phase 2 of the realm plan, 2026-10-25), above the Great Void: an endless sea of clouds with jade peaks rising
 * out of it (ModNoiseSettings#immortalRealm; the clouds the client's, ImmortalRealmClient), its qi a thousand times the lower realms'
 * (PlayerCultivation#IMMORTAL_QI_MULTIPLIER). Reached through the Immortal Gate over the Great Void's void sea when it opens
 * (ImmortalGate), coming out at the Southern Heavenly Gate about its 0, 0. The Gate is the only way down (the user's rule, 2026-10-25):
 * space will not tear there (SpatialTrialHandler#tear), and one who falls off a peak through the clouds below {@link #FALL_Y} is borne
 * back up onto the nearest land ({@link #bearUp}, VoidFallHandler), the landing harmless. Nobody below Entering Nirvana comes there (the
 * user's rule, 2026-10-25: "there just shouldnt be a way for people less than entering nirvana to get it and people less than EN
 * shouldnt spawn"): the Gate lets none below it through (ImmortalGate#mayPass), its wanderers are of it and above (NpcSpawner#IMMORTAL_FLOOR),
 * and its census never holds one of them back below it (HighRealms#IMMORTAL_REALM_QUOTA). One who falls below it there stays.
 */
public final class ImmortalRealm {
	/** Falling this far below the clouds' top, the sea of clouds bears one back up. */
	public static final int FALL_Y = ModDimensions.IMMORTAL_CLOUD_Y - 80;
	/** Borne up this far over the ground one is set down on. */
	static final int SET_DOWN_ABOVE = 3;

	/**
	 * {@code p} fell through the sea of clouds: borne back up onto the land nearest where they fell (found by numbers, the massifs' mask
	 * and the generator's heights, outside any sect's barrier), the landing harmless; the Southern Heavenly Gate's ground if none is near.
	 */
	public static void bearUp(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		BlockPos column = SpatialArrivals.target(level, Mth.floor(p.getX()), Mth.floor(p.getZ()));
		int top = level.getChunkSource().getGenerator().getBaseHeight(column.getX(), column.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level,
				level.getChunkSource().randomState());
		if (top <= ModDimensions.IMMORTAL_CLOUD_Y) {
			column = new BlockPos(0, 0, 0);
			top = ModNoiseSettings.GATE_Y + 1;
		}
		SpatialArrivals.prepare(level, column);
		BlockPos ground = SpatialArrivals.ready(level, column) ? SpatialArrivals.groundNear(level, column) : null;
		double x = ground != null ? ground.getX() + 0.5 : column.getX() + 0.5, z = ground != null ? ground.getZ() + 0.5 : column.getZ() + 0.5;
		double y = (ground != null ? ground.getY() : top) + SET_DOWN_ABOVE;
		p.teleportTo(level, x, y, z, p.getYRot(), p.getXRot());
		p.connection.resetPosition();
		p.fallDistance = 0;
		p.setDeltaMovement(Vec3.ZERO);
		CultivationManager.get(p).setFallProtected(true);
		CultivationManager.sync(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_CLOUDS_BEAR_UP).withStyle(ChatFormatting.GOLD), true);
	}

	private ImmortalRealm() {}
}
