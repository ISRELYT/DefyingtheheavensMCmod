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
 * {@link #RESUME_FRACTION} of its maximum. Never on the Soul River, whose current carries the living; in the Spatial Gap one
 * flies as anywhere (SpatialTrialHandler). One may meditate in the air, the flight paid for all the while (MeditationManager#steady):
 * a pool run dry drops the meditator out of it.
 * <p>
 * Sword Flight ({@link SwordFlight}, from Foundation Building) takes off the same way and is run here too: with a sword in
 * the main hand and that ability on, the cultivator rides the sword instead, at its lower cost and speed. Only one of the
 * two is ever switched on ({@link Ability#exclusiveWith}): a rider who puts the sword away mid-air falls. Qi Flight comes
 * on when the golden core first forms (switching Sword Flight off), and a save from before the rule with both on keeps
 * Qi Flight; either way the player is told, and switches back on the ability wheel or the Abilities tab.
 * <p>
 * These abilities own {@code mayfly} (and the flying speed) for survival and adventure players, granting and revoking it
 * every tick; creative and spectator players are left to vanilla.
 * <p>
 * The dead, and the living in a Ghost King's form, fly freely (TwoPaths#fliesFreely): at every realm, at Qi Flight's speed
 * and swifter by each layer of Wandering Soul Steps from the fourth ({@link #freeSpeed}), spending nothing. Qi Flight and Sword
 * Flight themselves are the heaven path's.
 * <p>
 * Qi Flight and the free flight fly at creative flight's own speed at every realm, for fine work (the user's ask, 2026-10-23: from
 * Void Shattering on they had been far swifter, the more with every stage, and hard to steer finely); swiftness is Meteor Flight's,
 * their fast mode, switched on and off ({@link MeteorFlight}: where one looks, as fast as one's realm allows). Where that isn't flown,
 * in the Spatial Gap, switching it on gives this flight the realm's former swiftness instead ({@link #speedIn}). Space's flight is its
 * own, swifter still (AstralRealm#spaceSpeed). The server lets a flier move as far a packet as its flight carries it
 * ({@link #moveAllowanceSq}), or a guest of the game would be snapped back.
 */
public final class QiFlight {
	/** After running dry, flight returns (silently) at this fraction of the pool. */
	public static final double RESUME_FRACTION = 0.05;
	/**
	 * The drift the dead once flew at without Ghost Flight (they fly at Qi Flight's speed now: TwoPaths#fliesFreely): kept so a
	 * speed set to it before is still known as this class's (see {@link #setFlight}).
	 */
	public static final float GHOST_SPEED = SwordFlight.QI_FLIGHT_SPEED * 0.6f;

	/** Players whose qi ran dry in flight and who haven't gathered back to {@link #RESUME_FRACTION} yet. */
	private static final Set<UUID> EXHAUSTED = new HashSet<>();
	/** Whether each online player's true realm had awakened flight last tick, to announce the ability when it is first gained. */
	private static final Map<UUID, Boolean> REALM_ALLOWED = new HashMap<>();
	/** The same for Sword Flight. */
	private static final Map<UUID, Boolean> REALM_ALLOWED_SWORD = new HashMap<>();
	/** The dimension each online player was in last tick: a change means their client's flight must be told again (see {@link #tick}). */
	private static final Map<UUID, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> DIMENSION = new HashMap<>();

	/**
	 * Called every tick by {@link QiManager} for each living player, before the pool gathers.
	 *
	 * @return qi per second the player spends on flight this tick (0 when not flying on qi), to set against this tick's
	 *         gathering
	 */
	static double tick(ServerPlayer p, PlayerCultivation c) {
		double cost = fly(p, c);
		// Meteor Flight, the fast mode of Qi Flight and of the free flight (MeteorFlight): flying fast on qi costs more.
		MeteorFlight.update(p, c);
		return cost > 0 && MeteorFlight.isStreaking(p) ? MeteorFlight.streakCost(c, cost) : cost;
	}

	/** {@link #tick}'s flight: granted or not, at what speed, and what it costs a second at Qi Flight's own rate. */
	private static double fly(ServerPlayer p, PlayerCultivation c) {
		UUID id = p.getUUID();
		// Carried into another dimension by a teleport (ServerPlayer#teleportTo: the rifts, the Soul River, the Netherworld's ways, a
		// ghost's return to the living world), the client rebuilds its player with no right to fly, and nothing tells it otherwise
		// (vanilla's portals do, its teleports don't): the flight owned here (mayfly) would stay refused until something about it
		// changed. So a new dimension tells the client again.
		if (DIMENSION.put(id, p.level().dimension()) != p.level().dimension()) p.onUpdateAbilities();
		// Announced by the true realm: Realm Suppress can hold a cultivator below Core Formation for a while (grounding them),
		// and the greeting shouldn't repeat each time that lifts.
		boolean awakened = c.canFlyOnQi();
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
		boolean swordAwakened = !c.isMortal() && c.canRideSword();
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
		// Space (AstralRealm): there is no ground, so everyone there flies, always, on no qi, at a speed of their realm; a Ghost King
		// too, the one of the dead the void bears (AstralRealm#canBearTheVoid).
		if (ModDimensions.isSpace(p.level().dimension()) && !Incorporeal.isRuledGhost(p)) {
			SwordFlight.update(p, false);
			EXHAUSTED.remove(id);
			if (p.isSpectator()) return 0;
			setFlight(p, true, AstralRealm.spaceSpeed(c));
			if (!p.getAbilities().flying) {
				p.getAbilities().flying = true;
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
		// A ghost flies on no qi and no sword, at every realm and as fast as Qi Flight or more (TwoPaths#fliesFreely: ghostly creatures
		// need no ground; the user's rule, 2026-10-18). Never on the Soul River, nor while a Ghost-Quelling Talisman binds it (QuelledEffect).
		if (Incorporeal.isGhost(p)) {
			SwordFlight.update(p, false);
			EXHAUSTED.remove(id);
			if (!ModDimensions.isSoulRiver(p.level().dimension()) && !p.hasEffect(ModEffects.QUELLED)) setFlight(p, true, freeSpeed(p, c));
			else land(p);
			return 0;
		}
		// The Soul River's current carries everyone living on it; the Spatial Gap carries nobody: whoever can fly flies it as anywhere
		// (SpatialTrialHandler: its old trials, which carried those below Heavenly Being, are gone).
		boolean carried = ModDimensions.isSoulRiver(p.level().dimension()); // not isPassage: that is the gap too
		// One of the living in a Ghost King's form flies as the dead do, whatever its realms, for nothing (the Soul River carries it as
		// it carries everyone living, and a talisman binds it as it binds them).
		if (TwoPaths.inGhostForm(c) && !carried) {
			SwordFlight.update(p, false);
			EXHAUSTED.remove(id);
			if (!p.hasEffect(ModEffects.QUELLED)) setFlight(p, true, freeSpeed(p, c));
			else land(p);
			return 0;
		}
		// At most one of these is switched on (Ability#exclusiveWith); a rider without a sword in hand has no flight.
		boolean onSword = SwordFlight.canRide(p, c);
		boolean onQi = !onSword && c.canFlyOnQi() && c.isAbilityEnabled(Ability.QI_FLIGHT);
		// Neither (switched off on the Abilities tab, no sword in hand): no flight, and one in progress ends.
		if (!onQi && !onSword || carried) {
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
		setFlight(p, true, onSword ? SwordFlight.SWORD_SPEED : speedIn(p.level().dimension(), c));
		if (!p.getAbilities().flying) {
			SwordFlight.update(p, false);
			return 0;
		}

		// On a planet's land (AstralRealm) flight costs nothing: nobody is stranded on a world of thin qi.
		double cost = ModDimensions.isPlanetSurface(p.level().dimension()) ? 0
				: onSword ? c.swordFlightCostPerSecond() : c.qiFlightCostPerSecond();
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
		if (swordWentOff) p.displayClientMessage(Component.translatable(ModLang.MSG_FLIGHT_EXCLUSIVE), true);
	}

	/** No flight: revoked, any flight in progress ends, and the speed is vanilla's again. */
	private static void land(ServerPlayer p) {
		SwordFlight.update(p, false);
		setFlight(p, false, SwordFlight.QI_FLIGHT_SPEED);
	}

	/**
	 * The speed Qi Flight and the free flight had from Void Shattering on, before they went back to creative flight's at every realm
	 * (2026-10-23, Meteor Flight's coming): Void Shattering 2.4 to 3 times vanilla's, Sacred Vessel 3.2 to 3.8, Entering Nirvana 4 to
	 * 4.9, True Immortal 6 to 7.2. Still theirs in the Spatial Gap with Meteor Flight switched on ({@link #speedIn}), and known as this
	 * class's when saved with a player ({@link #isOurs}).
	 */
	private static float formerSpeed(Realm realm, Stage stage) {
		int s = stage.ordinal();
		return switch (realm) {
			case VOID_SHATTERING -> 0.12f + s * 0.01f;
			case SACRED_VESSEL -> 0.16f + s * 0.01f;
			case ENTERING_NIRVANA -> 0.20f + s * 0.015f;
			case TRUE_IMMORTAL -> 0.30f + s * 0.02f;
			default -> SwordFlight.QI_FLIGHT_SPEED;
		};
	}

	/** {@link #formerSpeed} of {@code c}'s higher path, as the realm lets them wield it (either path: TwoPaths#higherRank). */
	private static float formerSpeed(PlayerCultivation c) {
		int rank = TwoPaths.higherRank(c, true);
		if (c.isMortal() || rank < 0) return SwordFlight.QI_FLIGHT_SPEED;
		int stages = Stage.values().length;
		return formerSpeed(Realm.byIndex(rank / stages), Stage.byIndex(rank % stages));
	}

	/**
	 * Qi Flight's speed (and the free flight's, before Wandering Soul Steps) for {@code c} in {@code dimension}: creative flight's own;
	 * but where Meteor Flight's own flight isn't flown and this one is (the Spatial Gap: MeteorFlight#flownIn), with Meteor Flight
	 * switched on, the swiftness the realm had there before Meteor Flight came ({@link #formerSpeed}: Void Shattering 2.4 to 3 times
	 * creative flight's up to True Immortal 6 to 7.2; the user's ask, 2026-10-23: "make sure you can still reach previous flight speeds
	 * in space and the spatial gap"). Space has its own speeds either way (AstralRealm#spaceSpeed).
	 */
	static float speedIn(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, PlayerCultivation c) {
		if (c.isAbilityActive(Ability.METEOR_FLIGHT) && !MeteorFlight.flownIn(dimension)) return formerSpeed(c);
		return SwordFlight.QI_FLIGHT_SPEED;
	}

	/**
	 * How fast one who flies freely flies (TwoPaths#fliesFreely): Qi Flight's speed (creative flight's, at every realm), and from the
	 * fourth layer of Wandering Soul Steps (GhostMethods#STEPS_FLIGHT_FROM) a tenth more for each layer, +60% at the ninth. Swifter
	 * yet is Meteor Flight's (MeteorFlight).
	 */
	public static float freeSpeed(PlayerCultivation c) {
		return freeSpeed(SwordFlight.QI_FLIGHT_SPEED, c.netherworld().layer(GhostMethod.WANDERING_SOUL_STEPS));
	}

	/** {@link #freeSpeed} where {@code p} is: in the Spatial Gap with Meteor Flight on, from the realm's former swiftness ({@link #speedIn}). */
	private static float freeSpeed(ServerPlayer p, PlayerCultivation c) {
		return freeSpeed(speedIn(p.level().dimension(), c), c.netherworld().layer(GhostMethod.WANDERING_SOUL_STEPS));
	}

	static float freeSpeed(float base, int stepsLayer) {
		int past = Math.max(0, stepsLayer - GhostMethods.STEPS_FLIGHT_FROM + 1);
		return base * (1 + GhostMethods.STEPS_FLIGHT_PER_LAYER * past);
	}

	/**
	 * The farthest a flier of {@code speed} (vanilla flying speed) goes in a tick, sprinting and climbing at once: across, twice the
	 * speed pushed against the air's 0.91 (22.2 times it); up, three times it against 0.6 (7.5 times it).
	 */
	public static double stepAt(float speed) {
		return Math.hypot(22.2 * speed, 7.5 * speed);
	}

	/**
	 * Packets come in bunched (lag, a relay, a server stalled a moment: vanilla measures every packet waiting from where the tick began),
	 * several ticks' flight in one: a flier is let move this many ticks of it a packet, a second's.
	 */
	private static final double BUNCHED_TICKS = 20;

	/** How far a flier of {@code speed} may go in one movement packet: {@link #BUNCHED_TICKS} ticks of its farthest step ({@link #stepAt}). */
	public static double reachAt(float speed) {
		return stepAt(speed) * BUNCHED_TICKS;
	}

	/**
	 * How far (squared) the server lets {@code p} move in one movement packet (ServerGamePacketListenerImplMixin): vanilla's
	 * {@code limit} (10 blocks, whoever hosts the game unchecked), or for a flier the more its flight carries it ({@link #reachAt},
	 * or {@link #BUNCHED_TICKS} ticks of Meteor Flight's top speed: MeteorFlight#topPerTick): in space (AstralRealm#spaceSpeed) or
	 * flying fast, a guest would be snapped back mid-flight otherwise.
	 */
	public static float moveAllowanceSq(ServerPlayer p, float limit) {
		if (!p.getAbilities().flying) return limit;
		double reach = reachAt(p.getAbilities().getFlyingSpeed());
		PlayerCultivation c = CultivationManager.get(p);
		if (MeteorFlight.mayStreak(p, c)) reach = Math.max(reach, MeteorFlight.topPerTick(c) * BUNCHED_TICKS);
		return (float) Math.max(limit, reach * reach);
	}

	/**
	 * Whether {@code speed} is one set here: Qi Flight's (at any realm, as it is or was: {@link #formerSpeed}), a sword's, the free
	 * flight's, space's or the last one set for that player. Told by value, so a speed saved with the player (vanilla keeps it) is
	 * still known after a relog.
	 */
	private static boolean isOurs(UUID id, float speed) {
		if (speed == SwordFlight.SWORD_SPEED || speed == GHOST_SPEED || Float.valueOf(speed).equals(SPEED_SET.get(id))) return true;
		for (Realm realm : Realm.values()) {
			for (Stage stage : Stage.values()) {
				float flight = formerSpeed(realm, stage), space = AstralRealm.spaceSpeed(realm, stage);
				if (speed == flight || speed == space || speed == AstralRealm.starborn(space)) return true; // a Starborn Body's too
				for (int layer = GhostMethods.STEPS_FLIGHT_FROM; layer <= GhostMethods.LAYERS; layer++) if (speed == freeSpeed(flight, layer)) return true;
			}
		}
		return false;
	}

	/**
	 * Grants or revokes flight and sets its speed; revoking also ends a flight in progress. Only sends an update when
	 * something changes. A speed not set here ({@link #isOurs}: another mod's, or a command's) is left alone.
	 */
	private static void setFlight(ServerPlayer p, boolean mayFly, float speed) {
		Abilities abilities = p.getAbilities();
		float current = abilities.getFlyingSpeed();
		boolean speedChanges = current != speed && isOurs(p.getUUID(), current);
		if (abilities.mayfly == mayFly && (mayFly || !abilities.flying) && !speedChanges) return;
		abilities.mayfly = mayFly;
		if (!mayFly) abilities.flying = false;
		if (speedChanges) {
			abilities.setFlyingSpeed(speed);
			SPEED_SET.put(p.getUUID(), speed);
		}
		p.onUpdateAbilities();
	}

	/** The last speed set here for each player (space's speeds vary by realm, so "ours" can't be told by value alone). */
	private static final Map<UUID, Float> SPEED_SET = new HashMap<>();

	public static void forget(UUID id) {
		EXHAUSTED.remove(id);
		SPEED_SET.remove(id);
		REALM_ALLOWED.remove(id);
		REALM_ALLOWED_SWORD.remove(id);
		DIMENSION.remove(id);
		SwordFlight.forget(id);
		MeteorFlight.forget(id);
	}

	private QiFlight() {}
}
