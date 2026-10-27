package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.InnerRealm;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.QiElement;
import net.minecraft.client.Minecraft;

/**
 * What a soul meditating in the Inner Realm is given by its body's surroundings, as the server reckons them (ModPackets#INNER_PLACE,
 * MeditationManager#sendBodyPlace): the boost of the mats, pedestals, height and calm about the body, and each element's share of the
 * qi there. The Cultivation tab reckons its rate and years from these while the soul is in there (CultivationScreen), not from the
 * island about the soul (the user's report of 2026-10-27: the tab showed the body's boosts gone). Only while the player is in the
 * Inner Realm: anywhere else the tab reckons from where they stand, as ever.
 */
public final class ClientInnerPlace {
	private static double boost = 1.0;
	private static double[] shares;

	static void set(double bodyBoost, double[] elementShares) {
		boost = bodyBoost;
		shares = elementShares;
	}

	/** Whether the player is in the Inner Realm with its body's surroundings told. */
	public static boolean active() {
		Minecraft mc = Minecraft.getInstance();
		return shares != null && mc.level != null && ModDimensions.isInnerRealm(mc.level.dimension());
	}

	/** The meditation's whole boost in there: the body's surroundings', and the Inner Realm's own (InnerRealm#CULTIVATION_BONUS). */
	public static double boost() {
		return boost * InnerRealm.CULTIVATION_BONUS;
	}

	/** {@code element}'s share of the qi about the body. */
	public static double share(QiElement element) {
		return shares != null && element.ordinal() < shares.length ? shares[element.ordinal()] : 0;
	}

	/** Leaving a world (a disconnect): nothing kept. */
	public static void clear() {
		boost = 1.0;
		shares = null;
	}

	private ClientInnerPlace() {}
}
