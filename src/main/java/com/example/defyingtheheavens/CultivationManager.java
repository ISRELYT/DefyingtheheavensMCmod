package com.example.defyingtheheavens;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CultivationManager {
	public static PlayerCultivation get(ServerPlayer player) {
		return CultivationSavedData.forServer(player.server).getOrCreate(player.getUUID());
	}

	public static void markDirty(MinecraftServer server) {
		CultivationSavedData.forServer(server).setDirty();
	}

	public static void sync(ServerPlayer player) {
		ModPackets.sendSync(player, get(player), MeditationManager.isMeditating(player.getUUID()));
	}

	/** Called after any change to realm/stage: stats, save flag, client sync. */
	public static void refresh(ServerPlayer player) {
		CultivationStats.apply(player, get(player));
		markDirty(player.server);
		sync(player);
	}

	/** Breakthrough button: no longer an instant rank-up, it starts the Heavenly Tribulation. */
	public static void tryBreakthrough(ServerPlayer player) {
		TribulationManager.start(player);
	}

	/** Abilities tab switch. Only abilities the player's realm has awakened can be switched. */
	public static void toggleAbility(ServerPlayer player, int index) {
		Ability ability = Ability.byIndex(index);
		PlayerCultivation c = get(player);
		if (ability == null || !ability.isUnlocked(c)) return;
		c.setAbilityEnabled(ability, !c.isAbilityEnabled(ability));
		markDirty(player.server);
		sync(player);
	}

	private CultivationManager() {}
}
