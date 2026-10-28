package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The grades an artifact is forged at (the Artifact Forge, ArtifactRecipes; the user's asks, 2026-10-28), Mortal to Immortal, in the
 * clothing's and pills' colours: what each is forged of ({@link #metal}, two spirit stones of its own grade, {@link #stone}, and from
 * Spirit up a demon core of at least {@link #coreRealm}), how long the working takes ({@link #seconds}) and the qi it draws each second
 * from the forger ({@link #qiPerSecond}: what a cultivator of the realm the grade answers to gathers in a second), and how hard it is
 * ({@link #difficulty}, CraftMastery: the rank of forging at which it never fails; Immortal grade fails even a Grandmaster now and then).
 */
public enum ArtifactGrade {
	//        id          colour                       secs  qi/s  difficulty
	MORTAL(  "mortal",    ChatFormatting.WHITE,        20,    1,   0),
	SPIRIT(  "spirit",    ChatFormatting.GREEN,        30,    3,   1),
	EARTH(   "earth",     ChatFormatting.AQUA,         45,    8,   2),
	HEAVEN(  "heaven",    ChatFormatting.LIGHT_PURPLE, 60,   25,   3),
	IMMORTAL("immortal",  ChatFormatting.GOLD,         90,  200,   4);

	private final String id;
	private final ChatFormatting colour;
	private final int seconds;
	private final double qiPerSecond;
	private final int difficulty;

	ArtifactGrade(String id, ChatFormatting colour, int seconds, double qiPerSecond, int difficulty) {
		this.id = id;
		this.colour = colour;
		this.seconds = seconds;
		this.qiPerSecond = qiPerSecond;
		this.difficulty = difficulty;
	}

	public String getId() { return id; }
	public ChatFormatting colour() { return colour; }
	/** How long the working takes over an ordinary fire. */
	public int seconds() { return seconds; }
	/** The qi the forge draws from its forger each second of the working. */
	public double qiPerSecond() { return qiPerSecond; }
	/** The rank of forging (CraftMastery#Rank ordinal) at which a working of this grade no longer fails. */
	public int difficulty() { return difficulty; }
	/** The practice in forging one working of it teaches (half, if it fails). */
	public int practice() { return (difficulty + 1) * 2; }

	public Component getDisplayName() { return Component.translatable(ModLang.artifactGrade(id)).withStyle(colour); }

	/** What its blades and fittings are forged of: iron, gold, diamond, netherite, Immortal Jade. */
	public Item metal() {
		return switch (this) {
			case MORTAL -> Items.IRON_INGOT;
			case SPIRIT -> Items.GOLD_INGOT;
			case EARTH -> Items.DIAMOND;
			case HEAVEN -> Items.NETHERITE_INGOT;
			case IMMORTAL -> ModItems.IMMORTAL_JADE;
		};
	}

	/** The spirit stones its working burns: its own grade's. */
	public Item stone() {
		return switch (this) {
			case MORTAL -> ModItems.LOW_SPIRIT_STONE;
			case SPIRIT -> ModItems.MID_SPIRIT_STONE;
			case EARTH -> ModItems.HIGH_SPIRIT_STONE;
			case HEAVEN -> ModItems.SUPREME_SPIRIT_STONE;
			case IMMORTAL -> ModItems.IMMORTAL_SPIRIT_STONE;
		};
	}

	/** The least realm of the demon core its working needs (null: none, Mortal grade). */
	public Realm coreRealm() {
		return switch (this) {
			case MORTAL -> null;
			case SPIRIT -> Realm.QI_REFINING;
			case EARTH -> Realm.CORE_FORMATION;
			case HEAVEN -> Realm.NASCENT_SOUL;
			case IMMORTAL -> Realm.DAO_INTEGRATION;
		};
	}

	/** Proof against fire and lava, as netherite is. */
	public boolean fireproof() {
		return this == HEAVEN || this == IMMORTAL;
	}

	public static ArtifactGrade byIndex(int index) {
		return values()[Math.max(0, Math.min(values().length - 1, index))];
	}
}
