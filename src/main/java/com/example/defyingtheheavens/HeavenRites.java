package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;

/**
 * Heaven-Petitioning Rites (CultivationMethod#HEAVEN_RITES; the methods overhaul's phase 2): one practising them gathers nothing by
 * meditation, but petitions the heavens at a Heaven Altar as the Ritual to the Heavens is made (HeavenRitual: offerings on its tray, a
 * sneaking empty hand, fifteen still seconds), once the heavens' leave is had. A rite granted is a quarter of the heaven path's stage
 * ({@link #SHARE}: CultivationMethods#grant, its years as the method has them: no talent, a sixteenth).
 * <ul>
 *   <li>Only at the right hour ({@link #rightHour}): dawn, a night of the full moon, or a thunderstorm.</li>
 *   <li>One rite an altar a day (HeavenAltarBlockEntity#riteDay), granted or refused.</li>
 *   <li>The offering it wants grows nine times a realm ({@link #scale}): a Proper one is 9 low spirit stones' worth at Qi Refining,
 *   59,049 at Heavenly Being, and the chance is the ritual's for the offering so weighed, the altar's grandeur and height.</li>
 *   <li>The heavens weigh one's virtue ({@link #virtue}): the righteous heard more often, the devilish less.</li>
 *   <li>A rite refused brings their rebuke ({@link #rebuke}): lightning on the petitioner.</li>
 * </ul>
 * Every cultivator learns its first volume when the heavens grant the Ritual to the Heavens (HeavenRitual#answer); its volumes to Heavenly
 * Being are in Method Manuals.
 */
public final class HeavenRites {
	public static final double SHARE = 0.25;
	/** The rebuke: this share of the petitioner's health. */
	public static final float REBUKE = 0.3f;

	/** Whether {@code c} petitions by the rites: practising them on the heaven path, the heavens' leave had. */
	public static boolean practises(PlayerCultivation c) {
		return !c.needsHeavensLeave() && CultivationMethods.practised(c, TwoPaths.Path.HEAVEN) == CultivationMethod.HEAVEN_RITES;
	}

	/** Dawn (the hour round sunrise), a night of the full moon, or a thunderstorm, by {@code level}'s own sky. */
	public static boolean rightHour(ServerLevel level) {
		if (level.isThundering()) return true;
		long time = level.getDayTime() % HeavenRitual.DAY;
		if (time >= 22_800 || time < 1_200) return true; // dawn
		return level.getMoonPhase() == 0 && time >= 13_000 && time < 23_000; // the full moon's night
	}

	/** What a rite's offering is divided by at {@code realm}, so its worth weighs as a Qi Refining one would: nine times a realm. */
	public static double scale(Realm realm) {
		return Math.pow(9, Math.min(realm.ordinal(), Realm.HEAVENLY_BEING.ordinal()));
	}

	/** The heavens weighing {@code c}'s virtue: x1.5 at the most righteous, x0.5 at the most devilish (Alignment, -100 to 100). */
	public static double virtue(PlayerCultivation c) {
		return 1 + Math.max(-100, Math.min(100, c.getAlignment())) / 200.0;
	}

	/** Why {@code player} can't petition at {@code altar} now (a message), or null: the hour, the altar's rite today, the bar full, no volume. */
	static Component refusal(ServerPlayer player, PlayerCultivation c, HeavenAltarBlockEntity altar) {
		if (!rightHour(player.serverLevel())) return Component.translatable(ModLang.MSG_RITE_HOUR);
		if (altar.riteDay() >= HeavenRitual.today(player.server)) return Component.translatable(ModLang.MSG_RITE_ALTAR_TODAY);
		if (c.isStalled()) return Component.translatable(ModLang.MSG_RITE_FULL);
		if (!HeavenMethods.canGain(c)) return Component.translatable(ModLang.MSG_METHOD_NO_VOLUME, CultivationMethod.HEAVEN_RITES.getDisplayName(),
				c.getRealm().getDisplayName());
		return null;
	}

	/** The heavens' rebuke of a refused rite: lightning on the petitioner, {@link #REBUKE} of its health (never its death). */
	static void rebuke(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt != null) {
			bolt.moveTo(player.getX(), player.getY(), player.getZ());
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
		if (!player.isCreative()) player.setHealth(Math.max(1, player.getHealth() - player.getMaxHealth() * REBUKE));
	}

	private HeavenRites() {}
}
