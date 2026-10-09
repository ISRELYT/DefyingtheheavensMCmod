package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Qi Flight, the ability of Core Formation (the golden core) and above, switchable on the Abilities tab: the cultivator flies as in creative mode (double-tap
 * jump), paying their realm's flight cost in qi for every second in the air while still gathering. Where gathering keeps
 * up with the cost (the Upper Realm, and everywhere from Heavenly Being on; see {@link Realm}) flight never ends.
 * Elsewhere a pool that runs dry drops them out of the sky, and flight returns once it has gathered back to
 * {@link #RESUME_FRACTION} of its maximum. Never in the Spatial Gap, where the trial decides the direction.
 * <p>
 * This ability owns {@code mayfly} for survival and adventure players, granting and revoking it every tick; creative and
 * spectator players are left to vanilla.
 */
public final class QiFlight {
	/** After running dry, flight returns (silently) at this fraction of the pool. */
	public static final double RESUME_FRACTION = 0.05;

	/** Players whose qi ran dry in flight and who haven't gathered back to {@link #RESUME_FRACTION} yet. */
	private static final Set<UUID> EXHAUSTED = new HashSet<>();
	/** Whether each online player's realm allowed flight last tick, to announce the ability when it is first gained. */
	private static final Map<UUID, Boolean> REALM_ALLOWED = new HashMap<>();

	/**
	 * Called every tick by {@link QiManager} for each living player, before the pool gathers.
	 *
	 * @return qi per second the player spends on flight this tick (0 when not flying on qi), to set against this tick's
	 *         gathering
	 */
	static double tick(ServerPlayer p, PlayerCultivation c) {
		UUID id = p.getUUID();
		boolean realmAllows = c.getEffectiveRealm().canFlyOnQi();
		Boolean before = REALM_ALLOWED.put(id, realmAllows);
		if (realmAllows && Boolean.FALSE.equals(before)) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_QI_FLIGHT_GAINED));
		}

		if (p.isCreative() || p.isSpectator()) return 0;
		boolean inGap = SpatialTrialHandler.isInTrial(id) || p.level().dimension() == ModDimensions.SPATIAL_GAP;
		// Switched off on the Abilities tab: no flight, and one in progress ends.
		if (!realmAllows || !c.isAbilityEnabled(Ability.QI_FLIGHT) || inGap) {
			setMayFly(p, false);
			return 0;
		}

		if (EXHAUSTED.contains(id)) {
			if (c.getQi() < c.maxQi() * RESUME_FRACTION) {
				setMayFly(p, false);
				return 0;
			}
			EXHAUSTED.remove(id);
		}
		// Granted before any flight is paid for: a player flying without mayfly (a flight carried over from a relog, say)
		// would otherwise be kicked by dedicated servers for "floating too long".
		setMayFly(p, true);
		if (!p.getAbilities().flying) return 0;

		double cost = c.qiFlightCostPerSecond();
		// Affordable as long as this tick's gathering and what's left cover it (always, where flight is sustained).
		if (c.getQi() + (c.qiGatherPerSecond() - cost) / 20.0 >= 0) return cost;
		// Ran dry mid-flight: whatever is left goes too, and the cultivator falls.
		c.setQi(0);
		EXHAUSTED.add(id);
		setMayFly(p, false);
		p.displayClientMessage(Component.translatable(ModLang.MSG_QI_FLIGHT_EXHAUSTED, (int) (RESUME_FRACTION * 100)), true);
		return 0;
	}

	/** Grants or revokes flight; revoking also ends a flight in progress. Only sends an update when something changes. */
	private static void setMayFly(ServerPlayer p, boolean mayFly) {
		Abilities abilities = p.getAbilities();
		if (abilities.mayfly == mayFly && (mayFly || !abilities.flying)) return;
		abilities.mayfly = mayFly;
		if (!mayFly) abilities.flying = false;
		p.onUpdateAbilities();
	}

	public static void forget(UUID id) {
		EXHAUSTED.remove(id);
		REALM_ALLOWED.remove(id);
	}

	private QiFlight() {}
}
