package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A cultivator's moral path, from {@link #MIN} (a devil) to {@link #MAX} (a paragon of the righteous path). Every NPC has one
 * (all members of a sect share their sect's), and so does every player ({@link PlayerCultivation#getAlignment()}).
 * <ul>
 *   <li>Below {@link #DEMONIC_BELOW}: the demonic path, dressed in red and black.</li>
 *   <li>{@link #DEMONIC_BELOW} to {@link #RIGHTEOUS_ABOVE}: neutral, dressed in blue.</li>
 *   <li>Above {@link #RIGHTEOUS_ABOVE}: the righteous path, dressed in white.</li>
 * </ul>
 * Righteous and demonic cultivators attack each other on sight; neutral ones keep to themselves unless provoked.
 */
public final class Alignment {
	public static final int MIN = -200;
	public static final int MAX = 200;
	public static final int DEMONIC_BELOW = -50;
	public static final int RIGHTEOUS_ABOVE = 50;

	/** Killing a peaceful cultivator who never raised a hand against you: this much, plus {@link #PER_REALM} per realm of theirs. */
	public static final int MURDER_PENALTY = 10;
	/** Slaying a demonic cultivator: this much, plus {@link #PER_REALM} per realm of theirs. */
	public static final int SLAYING_REWARD = 6;
	public static final int PER_REALM = 2;

	public enum Faction {
		DEMONIC("demonic", 0xE0413A),
		NEUTRAL("neutral", 0x6FA8FF),
		RIGHTEOUS("righteous", 0xF2F2F2);

		private final String id;
		private final int color;

		Faction(String id, int color) {
			this.id = id;
			this.color = color;
		}

		public String getId() { return id; }
		/** Text colour for nameplates and the menu. */
		public int getColor() { return color; }
		public Component getDisplayName() { return Component.translatable(ModLang.factionKey(id)); }

		/** The righteous and the demonic can't share the same sky; the neutral quarrel with no one unprovoked. */
		public boolean isEnemyOf(Faction other) {
			return this == RIGHTEOUS && other == DEMONIC || this == DEMONIC && other == RIGHTEOUS;
		}
	}

	public static int clamp(int alignment) {
		return Mth.clamp(alignment, MIN, MAX);
	}

	public static Faction factionOf(int alignment) {
		if (alignment < DEMONIC_BELOW) return Faction.DEMONIC;
		if (alignment > RIGHTEOUS_ABOVE) return Faction.RIGHTEOUS;
		return Faction.NEUTRAL;
	}

	/** "Righteous (+120)", "Neutral (0)", "Demonic (-150)", in the faction's colour. */
	public static MutableComponent describe(int alignment) {
		Faction faction = factionOf(alignment);
		String number = alignment > 0 ? "+" + alignment : Integer.toString(alignment);
		return Component.translatable(ModLang.ALIGNMENT_VALUE, faction.getDisplayName(), number).withStyle(style -> style.withColor(faction.getColor()));
	}

	/** A rogue cultivator's alignment: anything from devil to saint, evenly. */
	public static int randomRogue(RandomSource random) {
		return MIN + random.nextInt(MAX - MIN + 1);
	}

	/** A sect's alignment, shared by all its members: 40% righteous, 30% neutral, 30% demonic, never near a boundary. */
	public static int randomSect(RandomSource random) {
		float roll = random.nextFloat();
		if (roll < 0.4f) return 70 + random.nextInt(131);
		if (roll < 0.7f) return -30 + random.nextInt(61);
		return -200 + random.nextInt(131);
	}

	/**
	 * What a player's kill of an NPC cultivator does to the player's alignment: slaying a demonic cultivator raises it;
	 * killing one who was peaceful toward the player (never attacked them, never hunted them) lowers it. Anything else (a
	 * righteous or neutral cultivator who attacked first, say) leaves it alone.
	 */
	public static int killShift(int victimAlignment, boolean victimProvoked, Realm victimRealm, boolean victimMortal) {
		int realmBonus = victimMortal ? 0 : PER_REALM * victimRealm.ordinal();
		if (factionOf(victimAlignment) == Faction.DEMONIC) return SLAYING_REWARD + realmBonus;
		if (!victimProvoked) return -(MURDER_PENALTY + realmBonus);
		return 0;
	}

	private Alignment() {}
}
