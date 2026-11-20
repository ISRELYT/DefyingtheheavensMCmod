package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The soul in a puppet's Soul-Binding Ring (Puppetry, phase 2: the user's "i want all puppets to be able to harbour souls of players or NPCs
 * and animals and stuff, or be hollow", 2026-11-17): what it was ({@link Kind}), its name, its kind of creature, a cultivator's element,
 * rank and arts; and how far the puppet's circuits have copied it ({@link #copied}): whole at PuppetSouls#COPY_TICKS, the real soul gone on
 * and the copy the puppet's own for good ({@link #whole}). A puppet with none is hollow.
 */
public final class PuppetSoul {
	/** What the soul was: a beast's (an animal or a demon beast), a cultivator's, a player's (phase 2d: PuppetPlayers; its type their UUID). */
	public enum Kind {
		BEAST("beast"), CULTIVATOR("cultivator"), PLAYER("player");

		private final String id;

		Kind(String id) {
			this.id = id;
		}

		public String getId() { return id; }
	}

	/**
	 * The bent of a mind grown of its own (phase 2c: a Saint's puppet, its copy whole, three days under control), from what it did most while
	 * it grew: blows landed, time on watch, harm borne, ground covered, arts cast.
	 */
	public enum Trait {
		FIERCE("fierce"), WATCHFUL("watchful"), STEADFAST("steadfast"), SWIFT("swift"), CANNY("canny");

		private final String id;

		Trait(String id) {
			this.id = id;
		}

		public String getId() { return id; }

		public net.minecraft.network.chat.Component getDisplayName() {
			return net.minecraft.network.chat.Component.translatable(ModLang.puppetTrait(id));
		}

		public static @Nullable Trait byIndex(int index) {
			return index < 0 || index >= values().length ? null : values()[index];
		}
	}

	public final Kind kind;
	/** Its name as it was known: a cultivator's own, a beast's kind's. */
	public final String name;
	/** Its creature's entity type (its id), for a beast's ways. */
	public final String type;
	/** A cultivator's element (null for none, and for a beast). */
	public final @Nullable QiElement element;
	/** A cultivator's rank (PlayerCultivation#rank); a beast's nothing. */
	public final int rank;
	/** A cultivator's own arts the puppet casts (one or two of its spells); none for a beast. */
	public final List<Spell> arts;
	private int copied;
	private boolean whole;
	// A mind of its own growing (phase 2c): the ticks it has grown, what it has done most (a weight for each trait), and once grown, its
	// bent and the name it took.
	private int grown;
	private final double[] marks = new double[Trait.values().length];
	private @Nullable Trait trait;
	private String ownName = "";

	public PuppetSoul(Kind kind, String name, String type, @Nullable QiElement element, int rank, List<Spell> arts) {
		this.kind = kind;
		this.name = name;
		this.type = type;
		this.element = element;
		this.rank = rank;
		this.arts = List.copyOf(arts);
	}

	/** Ticks its puppet's circuits have spent copying it under its puppeteer's control. */
	public int copied() { return copied; }

	/** The copy is whole: the real soul has gone on, and what is left is the puppet's own. */
	public boolean whole() { return whole; }

	/** The real soul is still in the ring (the copy not yet whole). */
	public boolean real() { return !whole; }

	/** {@code ticks} more copying; whether the copy has just come whole. */
	public boolean copy(int ticks) {
		if (whole) return false;
		copied = Math.min(PuppetSouls.COPY_TICKS, copied + ticks);
		if (copied < PuppetSouls.COPY_TICKS) return false;
		whole = true;
		return true;
	}

	/** {@code ticks} of the copy lost (a player's while released: PuppetPlayers), never below nothing nor once whole. */
	public void uncopy(int ticks) {
		if (!whole) copied = Math.max(0, copied - ticks);
	}

	/** A player's soul: that player's id (its type); null for any other. */
	public java.util.@Nullable UUID player() {
		if (kind != Kind.PLAYER) return null;
		try {
			return java.util.UUID.fromString(type);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** Moved into another puppet (PuppetSouls#move): its copying begun again there. */
	public PuppetSoul movedAnew() {
		return new PuppetSoul(kind, name, type, element, rank, arts);
	}

	/** How far the copy has come, 0 to 100. */
	public int copiedPercent() {
		return whole ? 100 : (int) (100L * copied / PuppetSouls.COPY_TICKS);
	}

	// --- A mind of its own (phase 2c) ---

	/** Growing a mind of its own: the copy whole, no mind grown yet. */
	public boolean growing() {
		return whole && trait == null;
	}

	/** {@code weight} more toward {@code toward}, while it grows (what it did most decides its bent). */
	public void mark(Trait toward, double weight) {
		if (growing()) marks[toward.ordinal()] += weight;
	}

	/** {@code ticks} more growing; whether it has grown enough for a mind of its own (PuppetSouls#GROW_TICKS). */
	public boolean grow(int ticks) {
		if (!growing()) return false;
		grown = Math.min(PuppetSouls.GROW_TICKS, grown + ticks);
		return grown >= PuppetSouls.GROW_TICKS;
	}

	/** Ticks it has grown. */
	public int grown() { return grown; }

	/** The bent its marks lean to: the heaviest; with none or a tie among the heaviest, one of those at random. */
	public Trait leaning(net.minecraft.util.RandomSource random) {
		double most = 0;
		for (double m : marks) most = Math.max(most, m);
		List<Trait> best = new ArrayList<>();
		for (Trait t : Trait.values()) if (most <= 0 || Math.abs(marks[t.ordinal()] - most) < 1.0e-9) best.add(t);
		return best.get(random.nextInt(best.size()));
	}

	/** Grown a mind of its own: its bent and its name. */
	public void awaken(Trait trait, String name) {
		this.trait = trait;
		this.ownName = name;
	}

	/** Its mind's bent (null: none grown). */
	public @Nullable Trait trait() { return trait; }

	/** The name it took (empty: none grown). */
	public String ownName() { return ownName; }

	/** What it goes by: the name it took, else the name it was known by. */
	public String goesBy() {
		return trait != null && !ownName.isEmpty() ? ownName : name;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("Kind", kind.getId());
		tag.putString("Name", name);
		tag.putString("Type", type);
		if (element != null) tag.putString("Element", element.name());
		tag.putInt("Rank", rank);
		ListTag arts = new ListTag();
		for (Spell spell : this.arts) arts.add(StringTag.valueOf(spell.name()));
		tag.put("Arts", arts);
		tag.putInt("Copied", copied);
		tag.putBoolean("Whole", whole);
		tag.putInt("Grown", grown);
		ListTag leaning = new ListTag();
		for (double m : marks) leaning.add(net.minecraft.nbt.DoubleTag.valueOf(m));
		tag.put("Marks", leaning);
		if (trait != null) {
			tag.putString("Trait", trait.getId());
			tag.putString("OwnName", ownName);
		}
		return tag;
	}

	public static @Nullable PuppetSoul load(CompoundTag tag) {
		Kind kind = null;
		for (Kind k : Kind.values()) if (k.getId().equals(tag.getString("Kind"))) kind = k;
		if (kind == null) return null;
		QiElement element = null;
		if (tag.contains("Element", Tag.TAG_STRING)) {
			try {
				element = QiElement.valueOf(tag.getString("Element"));
			} catch (IllegalArgumentException ignored) {
			}
		}
		List<Spell> arts = new ArrayList<>();
		for (Tag t : tag.getList("Arts", Tag.TAG_STRING)) {
			try {
				arts.add(Spell.valueOf(t.getAsString()));
			} catch (IllegalArgumentException ignored) {
			}
		}
		PuppetSoul soul = new PuppetSoul(kind, tag.getString("Name"), tag.getString("Type"), element, tag.getInt("Rank"), arts);
		soul.copied = Math.max(0, Math.min(PuppetSouls.COPY_TICKS, tag.getInt("Copied")));
		soul.whole = tag.getBoolean("Whole");
		soul.grown = Math.max(0, Math.min(PuppetSouls.GROW_TICKS, tag.getInt("Grown")));
		ListTag leaning = tag.getList("Marks", Tag.TAG_DOUBLE);
		for (int i = 0; i < Math.min(leaning.size(), soul.marks.length); i++) soul.marks[i] = leaning.getDouble(i);
		for (Trait t : Trait.values()) if (t.getId().equals(tag.getString("Trait"))) soul.trait = t;
		soul.ownName = soul.trait == null ? "" : tag.getString("OwnName");
		return soul;
	}
}
