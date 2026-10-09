package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Qi Sense, awakened at Core Formation and switchable on the Abilities tab, costs no qi: while it is on, the cultivator sees
 * the plane of Qi. The client draws it (the world drained of colour, elemental qi drifting through it; see
 * QiSenseClientHandler). The server's part is announcing the ability and telling Qi Sense users when a nearby cultivator is
 * meditating, and how fast they draw qi in, so everyone who can see it watches the qi flow into them.
 */
public final class QiSense {
	/** Whether each online player's true realm had awakened Qi Sense last tick, to announce it when it is first gained. */
	private static final Map<UUID, Boolean> AWAKENED = new HashMap<>();

	/** Called every tick by {@link QiManager} for each living player. */
	static void tick(ServerPlayer p, PlayerCultivation c) {
		// The true realm, so a cultivator pressed below Core Formation for a while isn't greeted again when it lifts.
		boolean awakened = c.getRealm().canSenseQi();
		Boolean before = AWAKENED.put(p.getUUID(), awakened);
		if (awakened && Boolean.FALSE.equals(before)) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_QI_SENSE_GAINED));
		}
	}

	/**
	 * A meditating player draws qi in at {@code cultivationPerSecond}: tells everyone who can see it (the meditator and every
	 * tracking player with Qi Sense on), so their clients draw the inflow. Any meditator's inflow shows, Qi Sense or not, so
	 * a Core Formation elder watches the qi flow into a Qi Refining disciple. Sent with the meditation heartbeat; clients
	 * forget it when that stops.
	 */
	static void broadcastAbsorption(ServerPlayer meditator, PlayerCultivation c, double cultivationPerSecond) {
		if (c.isAbilityActive(Ability.QI_SENSE)) ModPackets.sendQiAbsorption(meditator, meditator, cultivationPerSecond);
		for (ServerPlayer other : PlayerLookup.tracking(meditator)) {
			if (CultivationManager.get(other).isAbilityActive(Ability.QI_SENSE)) {
				ModPackets.sendQiAbsorption(other, meditator, cultivationPerSecond);
			}
		}
	}

	public static void forget(UUID id) {
		AWAKENED.remove(id);
	}

	private QiSense() {}
}
