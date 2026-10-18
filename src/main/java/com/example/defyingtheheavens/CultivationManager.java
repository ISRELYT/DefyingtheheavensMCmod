package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public final class CultivationManager {
	/** The client's mirror of its own player's cultivation; the client sets this, the server never reads it. */
	public static Supplier<PlayerCultivation> clientMirror = PlayerCultivation::new;

	public static PlayerCultivation get(ServerPlayer player) {
		return CultivationSavedData.forServer(player.server).getOrCreate(player.getUUID());
	}

	/** For code that runs on both sides (items): the server's record, or on the client the synced mirror of the local player. */
	public static PlayerCultivation forPlayer(Player player) {
		return player instanceof ServerPlayer serverPlayer ? get(serverPlayer) : clientMirror.get();
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
		if (Incorporeal.isGhost(player)) { // the dead break through nothing (a Ghost King neither: its method is its way)
			player.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_CANNOT), true);
			return;
		}
		TribulationManager.start(player);
	}

	/** Abilities tab switch. Only abilities the player's realm has awakened can be switched. */
	public static void toggleAbility(ServerPlayer player, int index) {
		Ability ability = Ability.byIndex(index);
		PlayerCultivation c = get(player);
		// The dead are all soul already: there is no body to send a soul out of (a Ghost King, or a creative ghost, neither).
		if (ability == Ability.SOUL_PROJECTION && c.netherworld().ghost() && !c.isMortal() && c.getEffectiveRealm().canProjectSoul()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_CANNOT), true);
			return;
		}
		if (ability == null || !ability.isUnlocked(c)) return;
		// Soul Projection is an act: its switch sends the soul out, or calls it home (the switch shows where it is).
		if (ability == Ability.SOUL_PROJECTION) {
			SoulProjection.toggle(player);
			return;
		}
		// So are detonating the golden core (asked twice) and tearing space open (into the Spatial Gap, or out of it).
		if (ability == Ability.CORE_DETONATION) {
			CoreDetonation.toggle(player);
			return;
		}
		if (ability == Ability.SPATIAL_TEAR) {
			SpatialTrialHandler.tear(player);
			return;
		}
		c.setAbilityEnabled(ability, !c.isAbilityEnabled(ability));
		markDirty(player.server);
		sync(player);
		// Pressure starts or lifts at once, not at the next scan; and those let up off (TalkSystem's release) are pressed anew.
		if (ability == Ability.REALM_SUPPRESS) {
			RealmSuppressSystem.forgetReleases(player);
			RealmSuppressSystem.refreshNow();
		}
		// Opening or closing Qi Sense changes which projected souls this player perceives: the tracker re-asks at once.
		if (ability == Ability.QI_SENSE) SoulProjection.refreshTracking(player);
		// The other form taken, or the true one again, for all to see at once.
		if (ability == Ability.FORM_SHIFT) TwoPaths.shifted(player);
	}

	/**
	 * The ability wheel editor (Abilities tab): put an ability in a slot, or empty it ({@code index} -1). Only awakened
	 * abilities can be placed; an unknown slot or ability (a client with a different list) is ignored.
	 */
	public static void setWheelSlot(ServerPlayer player, int slot, int index) {
		PlayerCultivation c = get(player);
		Ability ability = index < 0 ? null : Ability.byIndex(index);
		if (slot < 0 || slot >= PlayerCultivation.WHEEL_SLOTS || index >= 0 && (ability == null || !ability.isUnlocked(c))) return;
		c.setWheelSlot(slot, ability);
		markDirty(player.server);
		sync(player);
	}

	/**
	 * The ability wheel: select an ability for the Use Selected Ability key, or select none ({@code index} -1). Only an awakened
	 * ability can be selected; an unknown one is ignored.
	 */
	public static void selectAbility(ServerPlayer player, int index) {
		PlayerCultivation c = get(player);
		Ability ability = index < 0 ? null : Ability.byIndex(index);
		if (index >= 0 && (ability == null || !ability.isUnlocked(c))) return;
		c.setSelectedAbility(ability);
		markDirty(player.server);
		sync(player);
	}

	private CultivationManager() {}
}
