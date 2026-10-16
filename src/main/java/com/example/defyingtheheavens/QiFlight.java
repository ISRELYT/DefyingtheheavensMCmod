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
 * {@link #RESUME_FRACTION} of its maximum. Never on a trial through the Spatial Gap, where the gap decides the direction; one
 * who tore their own way in (SpatialTrialHandler#tear) flies there as anywhere.
 * <p>
 * Sword Flight ({@link SwordFlight}, from Foundation Building) takes off the same way and is run here too: with a sword in
 * the main hand and that ability on, the cultivator rides the sword instead, at its lower cost and speed. Only one of the
 * two is ever switched on ({@link Ability#exclusiveWith}): a rider who puts the sword away mid-air falls. Qi Flight comes
 * on when the golden core first forms (switching Sword Flight off), and a save from before the rule with both on keeps
 * Qi Flight; either way the player is told, and switches back on the ability wheel or the Abilities tab.
 * <p>
 * These abilities own {@code mayfly} (and the flying speed) for survival and adventure players, granting and revoking it
 * every tick; creative and spectator players are left to vanilla.
 */
public final class QiFlight {
	/** After running dry, flight returns (silently) at this fraction of the pool. */
	public static final double RESUME_FRACTION = 0.05;
	/** A ghost's own flight, without Ghost Flight: a drift through the air, at this share of Qi Flight's speed. */
	public static final float GHOST_SPEED = SwordFlight.QI_FLIGHT_SPEED * 0.6f;

	/** Players whose qi ran dry in flight and who haven't gathered back to {@link #RESUME_FRACTION} yet. */
	private static final Set<UUID> EXHAUSTED = new HashSet<>();
	/** Whether each online player's true realm had awakened flight last tick, to announce the ability when it is first gained. */
	private static final Map<UUID, Boolean> REALM_ALLOWED = new HashMap<>();
	/** The same for Sword Flight. */
	private static final Map<UUID, Boolean> REALM_ALLOWED_SWORD = new HashMap<>();

	/**
	 * Called every tick by {@link QiManager} for each living player, before the pool gathers.
	 *
	 * @return qi per second the player spends on flight this tick (0 when not flying on qi), to set against this tick's
	 *         gathering
	 */
	static double tick(ServerPlayer p, PlayerCultivation c) {
		UUID id = p.getUUID();
		// Announced by the true realm: Realm Suppress can hold a cultivator below Core Formation for a while (grounding them),
		// and the greeting shouldn't repeat each time that lifts.
		boolean awakened = c.getRealm().canFlyOnQi();
		Boolean before = REALM_ALLOWED.put(id, awakened);
		if (awakened && Boolean.FALSE.equals(before)) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_QI_FLIGHT_GAINED));
			// The new flight comes on, and the sword's goes off: one flight at a time.
			boolean swordWasOn = c.isAbilityEnabled(Ability.SWORD_FLIGHT);
			if (swordWasOn || !c.isAbilityEnabled(Ability.QI_FLIGHT)) {
				c.setAbilityEnabled(Ability.QI_FLIGHT, true);
				flightsChanged(p, swordWasOn);
			}
		}
		boolean swordAwakened = !c.isMortal() && c.getRealm().canRideSword();
		Boolean swordBefore = REALM_ALLOWED_SWORD.put(id, swordAwakened);
		if (swordAwakened && Boolean.FALSE.equals(swordBefore)) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_SWORD_FLIGHT_GAINED));
		}
		// A save from before the two were exclusive (or a cultivator set straight to Core Formation) can have both on: Qi
		// Flight keeps it, as it was all a Core Formation cultivator had before Sword Flight.
		if (c.isAbilityEnabled(Ability.QI_FLIGHT) && c.isAbilityEnabled(Ability.SWORD_FLIGHT) && Ability.QI_FLIGHT.isUnlocked(c)
				&& Ability.SWORD_FLIGHT.isUnlocked(c)) {
			c.setAbilityEnabled(Ability.QI_FLIGHT, true);
			flightsChanged(p, true);
		}

		// A projected soul flies its own way (see SoulProjection); the permission only keeps the server from calling it "floating".
		if (SoulProjection.isSoul(p)) {
			SwordFlight.update(p, false);
			if (!p.getAbilities().mayfly) {
				p.getAbilities().mayfly = true;
				p.onUpdateAbilities();
			}
			return 0;
		}
		if (p.isCreative() || p.isSpectator()) {
			// Vanilla's flight again, at vanilla's speed (a ride doesn't carry over into creative).
			SwordFlight.update(p, false);
			setFlight(p, p.getAbilities().mayfly, SwordFlight.QI_FLIGHT_SPEED);
			return 0;
		}
		// A ghost flies on no qi and no sword: the dead need no ground, and drift through the air at {@link #GHOST_SPEED}; on its
		// method's flight (Wandering Soul Steps' Ghost Flight, paid for in ghostly qi: NetherworldManager) as fast as Qi Flight.
		// Never on the Soul River, nor while a Ghost-Quelling Talisman binds it (QuelledEffect).
		if (Incorporeal.isGhost(p)) {
			SwordFlight.update(p, false);
			if (!ModDimensions.isPassage(p.level().dimension()) && !p.hasEffect(ModEffects.QUELLED))
				setFlight(p, true, c.isAbilityActive(Ability.GHOST_FLIGHT) ? SwordFlight.QI_FLIGHT_SPEED : GHOST_SPEED);
			else land(p);
			return 0;
		}
		// The gap's current carries every trial, but one who tore their own way in flies there as anywhere (SpatialTrialHandler#tear).
		boolean inGap = !SpatialTrialHandler.isTravelling(id) && (SpatialTrialHandler.isInTrial(id) || ModDimensions.isPassage(p.level().dimension()));
		// At most one of these is switched on (Ability#exclusiveWith); a rider without a sword in hand has no flight.
		boolean onSword = SwordFlight.canRide(p, c);
		boolean onQi = !onSword && c.getEffectiveRealm().canFlyOnQi() && c.isAbilityEnabled(Ability.QI_FLIGHT);
		// Neither (switched off on the Abilities tab, no sword in hand): no flight, and one in progress ends.
		if (!onQi && !onSword || inGap) {
			land(p);
			return 0;
		}

		if (EXHAUSTED.contains(id)) {
			if (c.getQi() < c.maxQi() * RESUME_FRACTION) {
				land(p);
				return 0;
			}
			EXHAUSTED.remove(id);
		}
		// Granted before any flight is paid for: a player flying without mayfly (a flight carried over from a relog, say)
		// would otherwise be kicked by dedicated servers for "floating too long".
		setFlight(p, true, onSword ? SwordFlight.SWORD_SPEED : SwordFlight.QI_FLIGHT_SPEED);
		if (!p.getAbilities().flying) {
			SwordFlight.update(p, false);
			return 0;
		}

		double cost = onSword ? c.swordFlightCostPerSecond() : c.qiFlightCostPerSecond();
		// Affordable as long as this tick's gathering and what's left cover it (always, where flight is sustained).
		if (c.getQi() + (c.qiGatherPerSecond() - cost) / 20.0 >= 0) {
			SwordFlight.update(p, onSword);
			return cost;
		}
		// Ran dry mid-flight: whatever is left goes too, and the cultivator falls.
		c.setQi(0);
		EXHAUSTED.add(id);
		land(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_QI_FLIGHT_EXHAUSTED, (int) (RESUME_FRACTION * 100)), true);
		return 0;
	}

	/** The flight switches changed here rather than by the player: the menu and wheel follow, and (if Sword Flight went off) they're told. */
	private static void flightsChanged(ServerPlayer p, boolean swordWentOff) {
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		if (swordWentOff) p.sendSystemMessage(Component.translatable(ModLang.MSG_FLIGHT_EXCLUSIVE));
	}

	/** No flight: revoked, any flight in progress ends, and the speed is vanilla's again. */
	private static void land(ServerPlayer p) {
		SwordFlight.update(p, false);
		setFlight(p, false, SwordFlight.QI_FLIGHT_SPEED);
	}

	/**
	 * Grants or revokes flight and sets its speed; revoking also ends a flight in progress. Only sends an update when
	 * something changes. A speed other than Qi Flight's or a sword's (another mod's, or a command's) is left alone.
	 */
	private static void setFlight(ServerPlayer p, boolean mayFly, float speed) {
		Abilities abilities = p.getAbilities();
		float current = abilities.getFlyingSpeed();
		boolean ours = current == SwordFlight.QI_FLIGHT_SPEED || current == SwordFlight.SWORD_SPEED || current == GHOST_SPEED;
		boolean speedChanges = ours && current != speed;
		if (abilities.mayfly == mayFly && (mayFly || !abilities.flying) && !speedChanges) return;
		abilities.mayfly = mayFly;
		if (!mayFly) abilities.flying = false;
		if (speedChanges) abilities.setFlyingSpeed(speed);
		p.onUpdateAbilities();
	}

	public static void forget(UUID id) {
		EXHAUSTED.remove(id);
		REALM_ALLOWED.remove(id);
		REALM_ALLOWED_SWORD.remove(id);
		SwordFlight.forget(id);
	}

	private QiFlight() {}
}
