package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Yin corruption (阴蚀): the Netherworld is the realm of the dead, and its yin seeps into any living body that walks there. It
 * fills from nothing to full over a stay that depends on how strong the body is ({@link #fullStaySeconds}); when it is full,
 * the body gives way and the visitor walks on as a ghost, where they stand, with everything they carry (see GhostSystem).
 * Outside the Netherworld it ebbs away again ({@link #FADE_PER_SECOND}); on the Soul River it holds still.
 * <p>
 * A full stay, by true realm, before stage and Netherworld Breathing Art:
 * <table>
 *   <tr><td>Below Nascent Soul (mortals too)</td><td>{@link #STAY_BELOW_NASCENT_SOUL} s: they have no business there</td></tr>
 *   <tr><td>Nascent Soul</td><td>{@link #STAY_NASCENT_SOUL} s (5 min)</td></tr>
 *   <tr><td>Heavenly Being</td><td>{@link #STAY_HEAVENLY_BEING} s (10 min)</td></tr>
 *   <tr><td>Four Axis</td><td>{@link #STAY_FOUR_AXIS} s (20 min)</td></tr>
 * </table>
 * times the stage's power (Early 1.0 to Grand Perfection 1.3), times 1 + {@link #BREATHING_STAY_PER_LAYER} for each foundation
 * layer (the Netherworld Breathing Art's and those after it, up to {@link #STAY_LAYERS_MAX}): a Nascent Soul Early stays 5 min,
 * a Four Axis Grand Perfection 1200 x 1.3 = 26 min, with the Breathing Art perfected 26 x 1.9 = about 49 min, and with two
 * foundations perfected or more 26 x 2.8 = about 73 min.
 * <p>
 * What changes it: Netherworld Veil halves the rate ({@link #VEIL_FACTOR}); a Yang Preservation Pill stops it for a while; a
 * Yang Fire Talisman burns {@link #TALISMAN_CLEANSE} of it away at once; a ghostly blow drives {@link #GHOSTLY_BLOW} in.
 */
public final class YinCorruption {
	public static final int STAY_BELOW_NASCENT_SOUL = 60;
	public static final int STAY_NASCENT_SOUL = 5 * 60;
	public static final int STAY_HEAVENLY_BEING = 10 * 60;
	public static final int STAY_FOUR_AXIS = 20 * 60;
	public static final double BREATHING_STAY_PER_LAYER = 0.10;
	/** Foundation layers past this many lengthen the stay no further: two foundations' worth, +180%. */
	public static final int STAY_LAYERS_MAX = 18;
	/** Netherworld Veil: the yin seeps in at half the rate. */
	public static final double VEIL_FACTOR = 0.5;
	/** A blow from a Hungry Ghost or a ghost drives in this much at once: 5% of a full stay. */
	public static final double GHOSTLY_BLOW = 0.05;
	/** A Yang Fire Talisman burns away a third: a Nascent Soul Early's 100 s of the Netherworld. */
	public static final double TALISMAN_CLEANSE = 1.0 / 3.0;
	/** Outside the Netherworld (and off the river) it ebbs away completely over 5 minutes: 1/300 a second. */
	public static final double FADE_PER_SECOND = 1.0 / 300.0;
	/** The visitor is warned as it passes each of these. */
	private static final double[] WARNINGS = {0.5, 0.75, 0.9};
	private static final String[] WARNING_KEYS = {ModLang.MSG_YIN_HALF, ModLang.MSG_YIN_THREE_QUARTERS, ModLang.MSG_YIN_NEAR};

	/** Seconds from no corruption to full, for this cultivator with {@code breathingLayers} of Netherworld Breathing Art. */
	public static double fullStaySeconds(PlayerCultivation c, int breathingLayers) {
		double base;
		double power = 1.0;
		if (c.isMortal() || c.getRealm().ordinal() < Realm.NASCENT_SOUL.ordinal()) {
			base = STAY_BELOW_NASCENT_SOUL;
		} else {
			base = switch (c.getRealm()) {
				case NASCENT_SOUL -> STAY_NASCENT_SOUL;
				case HEAVENLY_BEING -> STAY_HEAVENLY_BEING;
				default -> STAY_FOUR_AXIS;
			};
		}
		if (!c.isMortal()) power = c.getStage().getPowerMultiplier();
		return base * power * (1 + BREATHING_STAY_PER_LAYER * Math.max(0, Math.min(STAY_LAYERS_MAX, breathingLayers)));
	}

	/** Corruption gained a second in the Netherworld (1 is full), with the Veil on or off. */
	public static double perSecond(PlayerCultivation c, int breathingLayers, boolean veil) {
		if (c.physique() == Physique.YIN_SOUL_BODY) return 0; // never takes hold
		return 1.0 / fullStaySeconds(c, breathingLayers) * (veil ? VEIL_FACTOR : 1.0);
	}

	/**
	 * Whether the Netherworld's yin works on this player at all: alive in body (not a ghost), not in creative or spectator, and not
	 * born with a Yin Soul Body (Physique).
	 */
	public static boolean affects(ServerPlayer player) {
		return player.isAlive() && !player.isCreative() && !player.isSpectator() && !GhostSystem.isGhost(player)
				&& CultivationManager.get(player).physique() != Physique.YIN_SOUL_BODY;
	}

	/**
	 * One second of it for {@code player}: rising in the Netherworld (unless a pill holds it still), ebbing anywhere but there and
	 * the river. Warns as it passes each mark, and gives the body over to the yin when it is full. Returns the trend for the HUD:
	 * 1 rising, -1 ebbing, 0 holding still.
	 */
	public static int second(ServerPlayer player, PlayerCultivation c, long gameTime) {
		NetherworldState s = c.netherworld();
		double before = s.yinCorruption();
		int trend = step(c, ModDimensions.isNetherworld(player.level().dimension()) && affects(player),
				ModDimensions.isSoulRiver(player.level().dimension()), GhostMethods.foundationLayers(c), c.isAbilityActive(Ability.NETHERWORLD_VEIL),
				gameTime);
		double after = s.yinCorruption();
		for (int i = 0; i < WARNINGS.length; i++) {
			if (before < WARNINGS[i] && after >= WARNINGS[i]) {
				player.displayClientMessage(Component.translatable(WARNING_KEYS[i]).withStyle(ChatFormatting.DARK_AQUA), true);
				player.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.6f + 0.15f * i);
			}
		}
		if (after >= 1.0 && trend > 0) GhostSystem.overtakenByYin(player);
		return trend;
	}

	/**
	 * The rule itself, one second of it: rising while {@code seeping} (a living body in the Netherworld) unless a pill holds it
	 * still at {@code gameTime}; holding on the river; ebbing anywhere else. Returns the trend (1, -1 or 0).
	 */
	public static int step(PlayerCultivation c, boolean seeping, boolean onRiver, int breathingLayers, boolean veil, long gameTime) {
		NetherworldState s = c.netherworld();
		if (seeping) {
			if (gameTime < s.yinPausedUntil()) return 0;
			s.setYinCorruption(s.yinCorruption() + perSecond(c, breathingLayers, veil));
			return 1;
		}
		if (onRiver || s.yinCorruption() <= 0) return 0;
		s.setYinCorruption(s.yinCorruption() - FADE_PER_SECOND);
		return -1;
	}

	/** A Yang Fire Talisman's flame: burns away {@link #TALISMAN_CLEANSE}; returns how much it took. */
	public static double cleanse(PlayerCultivation c, double amount) {
		NetherworldState s = c.netherworld();
		double before = s.yinCorruption();
		s.setYinCorruption(before - amount);
		return before - s.yinCorruption();
	}

	private YinCorruption() {}
}
