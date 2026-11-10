package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/**
 * What the Inscription Brush can draw on Talisman Paper (Talismans: the user's "Build as designed", 2026-11-10). Each pattern: how it is
 * used ({@link Use}: thrown, burnt on oneself, or the older talismans' own ways), how hard it is to draw (a rank of CraftMastery's Talismans
 * to draw it surely), the share of one's qi pool it takes, the practice it teaches, its own short wait between two (vanilla's item
 * cooldown, shown on the hotbar), and what else goes into it besides the paper and the cinnabar.
 */
public enum TalismanPattern {
	/** Thrown: it bursts into flame where it lands, burning all round it. */
	FIRE("fire", Use.THROWN, QiElement.FIRE, 0, 0.08, 30, () -> ModItems.FIRE_TALISMAN, 1),
	/** Thrown: the Five Thunders strike where it lands, one bolt on whoever it hits or stands there. */
	FIVE_THUNDER("five_thunder", Use.THROWN, QiElement.METAL, 2, 0.15, 60, () -> ModItems.FIVE_THUNDER_TALISMAN, 1),
	/** Thrown: a burst of frost, slowing and chilling all round where it lands. */
	FROST("frost", Use.THROWN, QiElement.WATER, 0, 0.08, 30, () -> ModItems.FROST_TALISMAN, 1),
	/** Thrown: holds the one it strikes where it stands for a few seconds, unless it stands a realm or more above it. */
	BINDING("binding", Use.THROWN, QiElement.EARTH, 1, 0.12, 100, () -> ModItems.BINDING_TALISMAN, 1),
	/** Burnt: a shell of golden light that soaks up harm for half a minute. */
	GOLDEN_LIGHT("golden_light", Use.BURNT, QiElement.METAL, 1, 0.15, 600, () -> ModItems.GOLDEN_LIGHT_TALISMAN, 1),
	/** Burnt: swift feet for a minute. */
	DIVINE_SPEED("divine_speed", Use.BURNT, QiElement.WOOD, 0, 0.06, 200, () -> ModItems.DIVINE_SPEED_TALISMAN, 1),
	/** Burnt: a flash a hundred blocks or more the way one faces, onto safe ground. */
	THOUSAND_LI_ESCAPE("thousand_li_escape", Use.BURNT, null, 2, 0.20, 600, () -> ModItems.THOUSAND_LI_ESCAPE_TALISMAN, 1),
	/** Burnt: beasts and cultivators don't notice one for a minute, until one strikes. */
	CONCEALMENT("concealment", Use.BURNT, QiElement.WATER, 1, 0.12, 1200, () -> ModItems.CONCEALMENT_TALISMAN, 1),
	/** The Yang Fire Talisman (YangFireTalismanItem), drawn as it is painted at a crafting table: blaze powder and a gold nugget in it too. */
	YANG_FIRE("yang_fire", Use.OWN, QiElement.FIRE, 0, 0.05, 0, () -> ModItems.YANG_FIRE_TALISMAN, 1, () -> Items.BLAZE_POWDER, () -> Items.GOLD_NUGGET),
	/** The Ghost-Quelling Talisman (GhostQuellingTalismanItem): Yin Jade in it too, two to a drawing as at the crafting table. */
	GHOST_QUELLING("ghost_quelling", Use.OWN, QiElement.YIN, 1, 0.08, 0, () -> ModItems.GHOST_QUELLING_TALISMAN, 2, () -> ModItems.YIN_JADE);

	/** How it is used: thrown (TalismanEntity), burnt on oneself (TalismanItem#use), or the older talismans' own ways. */
	public enum Use { THROWN, BURNT, OWN }

	private final String id;
	public final Use use;
	/** The element its power is of (the harm it deals: QiDamageSource), or null. */
	public final QiElement element;
	/** The Talismans rank (CraftMastery.Rank ordinal) that draws it surely; each short of it, 12% more fail. */
	public final int difficulty;
	/** The share of one's qi pool a drawing takes. */
	public final double qiShare;
	/** Its own wait between two burnt or thrown (ticks; 0: none), on whoever uses it. */
	public final int cooldown;
	private final Supplier<Item> item;
	/** How many a drawing makes. */
	public final int count;
	private final List<Supplier<Item>> extras;

	@SafeVarargs
	TalismanPattern(String id, Use use, QiElement element, int difficulty, double qiShare, int cooldown, Supplier<Item> item, int count,
			Supplier<Item>... extras) {
		this.id = id;
		this.use = use;
		this.element = element;
		this.difficulty = difficulty;
		this.qiShare = qiShare;
		this.cooldown = cooldown;
		this.item = item;
		this.count = count;
		this.extras = List.of(extras);
	}

	public String getId() { return id; }

	public Item item() { return item.get(); }

	/** What goes into it beyond the paper and the cinnabar. */
	public List<Item> extras() { return extras.stream().map(Supplier::get).toList(); }

	/** Its power is the drawer's realm's, written on it (the older talismans' are their own). */
	public boolean hasPower() { return use != Use.OWN; }

	/** The practice a drawing teaches: one, and one more for each step of difficulty. */
	public int practice() { return 1 + difficulty; }

	public Component getDisplayName() { return Component.translatable(item().getDescriptionId()); }

	public static TalismanPattern byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}

	/** The pattern whose talisman {@code item} is, or null. */
	public static TalismanPattern of(Item item) {
		for (TalismanPattern pattern : values()) if (pattern.item() == item) return pattern;
		return null;
	}
}
