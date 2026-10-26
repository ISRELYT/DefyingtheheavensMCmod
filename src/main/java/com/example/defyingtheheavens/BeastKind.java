package com.example.defyingtheheavens;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;

/**
 * The wild demon beasts (the user's ask, 2026-10-23): beasts in their animal forms, not yet able to think, with a rank on the
 * cultivation scale (DemonBeast) from Qi Refining Early up to Core Formation Grand Perfection, each kind in its tier's ranks, in its
 * own lands (biome tags {@code beast/<id>}), with its own body (its model: BeastModels on the client), its way of moving, its
 * signature move (DemonBeastGoals), its temper, its size, and what it leaves besides its core (DemonCoreItem). And the elders of the
 * demon races' kin, Void Shattering to Entering Nirvana, who keep their races' blood in the Great Void (Tier#ELDER, BloodPools).
 * <p>
 * Append-only: a kind's id names its entity type ({@code defying-the-heavens:<id>}).
 */
public enum BeastKind {
	//           id                       tier            element           body               moves             signature           temper  width height speed  health armour  colours (egg)
	IRON_HIDE_BOAR("iron_hide_boar", Tier.COMMON, QiElement.EARTH, Body.QUADRUPED, Moves.GROUND, Signature.CHARGE, true, 1.0f, 0.9f, 0.3, 1.1, 1.3, 0x5A4636, 0xC8B49A),
	SHADOW_WOLF("shadow_wolf", Tier.COMMON, QiElement.YIN, Body.QUADRUPED, Moves.GROUND, Signature.PACK, true, 0.7f, 0.9f, 0.34, 0.8, 0.8, 0x26222E, 0x8A5AD8),
	JADE_VIPER("jade_viper", Tier.COMMON, QiElement.WOOD, Body.SERPENT, Moves.GROUND, Signature.VENOM, true, 0.8f, 0.4f, 0.28, 0.7, 0.8, 0x2E8A4A, 0xD8E06A),
	ROCK_SHELL_TURTLE("rock_shell_turtle", Tier.COMMON, QiElement.WATER, Body.TURTLE, Moves.AMPHIBIOUS, Signature.SHELL, false, 1.0f, 0.7f, 0.18, 1.4, 2.5, 0x6A6A62, 0x5A8A3A),
	WIND_HAWK("wind_hawk", Tier.COMMON, QiElement.METAL, Body.BIRD, Moves.FLYING, Signature.SWOOP, true, 0.6f, 0.6f, 0.3, 0.6, 0.6, 0x5A6A82, 0xE8E8F0),
	FIRE_MONKEY("fire_monkey", Tier.COMMON, QiElement.FIRE, Body.APE, Moves.GROUND, Signature.THROW, true, 0.6f, 1.0f, 0.32, 0.8, 0.8, 0xC8481E, 0xF0C850),

	BLOODFANG_TIGER("bloodfang_tiger", Tier.UNCOMMON, QiElement.METAL, Body.QUADRUPED, Moves.GROUND, Signature.POUNCE, true, 1.2f, 1.2f, 0.34, 1.1, 1.1, 0x9A2A1A, 0x1A1010),
	FROST_APE("frost_ape", Tier.UNCOMMON, QiElement.WATER, Body.APE, Moves.GROUND, Signature.ICE_SLAM, true, 1.4f, 2.0f, 0.27, 1.4, 1.2, 0xE8F0F4, 0x6AB0E0),
	EARTHQUAKE_BEAR("earthquake_bear", Tier.UNCOMMON, QiElement.EARTH, Body.QUADRUPED, Moves.GROUND, Signature.POUND, true, 1.6f, 1.5f, 0.26, 1.6, 1.4, 0x5A3A22, 0x8A8A82),
	IRON_JAW_CROCODILE("iron_jaw_crocodile", Tier.UNCOMMON, QiElement.WATER, Body.CROCODILE, Moves.AMPHIBIOUS, Signature.DRAG, true, 1.4f, 0.6f, 0.24, 1.3, 1.8, 0x2E3E26, 0xA8A85A),
	LIGHTNING_ANTELOPE("lightning_antelope", Tier.UNCOMMON, QiElement.METAL, Body.QUADRUPED, Moves.GROUND, Signature.SPARK_CHARGE, false, 0.9f, 1.5f, 0.38, 0.9, 0.8, 0xC8A06A, 0x6AC8FF),
	EMBER_FOX("ember_fox", Tier.UNCOMMON, QiElement.FIRE, Body.QUADRUPED, Moves.GROUND, Signature.BREATH, true, 0.8f, 0.9f, 0.34, 0.9, 0.9, 0xD8501E, 0xFFD040),

	GOLDEN_WINGED_ROC("golden_winged_roc", Tier.RARE, QiElement.METAL, Body.BIRD, Moves.FLYING, Signature.DIVE, true, 2.2f, 1.6f, 0.3, 1.3, 1.1, 0xC89A2A, 0x6A4A22),
	JADE_SCALED_PYTHON("jade_scaled_python", Tier.RARE, QiElement.WOOD, Body.SERPENT, Moves.GROUND, Signature.COIL, true, 1.4f, 0.9f, 0.26, 1.6, 1.4, 0x2E7A4A, 0xE0C04A),
	HORNED_THUNDER_RHINO("horned_thunder_rhino", Tier.RARE, QiElement.EARTH, Body.QUADRUPED, Moves.GROUND, Signature.STUN_CHARGE, true, 2.0f, 1.9f, 0.27, 1.8, 2.0, 0x6A6A72, 0x6AC8FF),
	FLOOD_DRAGON("flood_dragon", Tier.RARE, QiElement.WATER, Body.DRAGON, Moves.SWIMMING, Signature.WATER_JET, true, 2.0f, 1.2f, 0.9, 1.5, 1.3, 0x1E6A8A, 0xD8F0F0),

	// The elders (the realm plan, 2026-10-25): one of each demon race's kin, keeping its race's blood in the Great Void's blood pools
	// (BloodPools), never born wild.
	ELDER_WHITE_TIGER("elder_white_tiger", Tier.ELDER, QiElement.METAL, Body.QUADRUPED, Moves.GROUND, Signature.POUNCE, true, 3.2f, 4.4f, 0.4, 1.5, 1.4, 0xECECE4, 0x22222A),
	NINE_TAILED_FOX("nine_tailed_fox", Tier.ELDER, QiElement.FIRE, Body.QUADRUPED, Moves.GROUND, Signature.BREATH, true, 2.0f, 3.4f, 0.45, 1.2, 1.1, 0xF6F2EA, 0xFF7A2A),
	MOUNTAIN_LIZARD_KING("mountain_lizard_king", Tier.ELDER, QiElement.EARTH, Body.CROCODILE, Moves.GROUND, Signature.POUND, true, 3.6f, 2.4f, 0.3, 1.9, 2.2, 0x2E3428, 0x6ADA8A),
	AZURE_DRAGON("azure_dragon", Tier.ELDER, QiElement.WOOD, Body.DRAGON, Moves.FLYING, Signature.DIVE, true, 4.0f, 3.6f, 0.4, 1.7, 1.5, 0x2A78B0, 0xE8C040),

	// The primordial beasts (the realm plan's phase 2, 2026-10-26): the Immortal Realm's world bosses, one of each there at a time
	// (PrimordialBeasts), True Immortals of Grand Perfection a little stronger than their realm (PrimordialBeast#MIGHT), never born wild.
	// Their own ways in the air (PrimordialBeast): the Kun-Peng's size here the Kun's, the Peng's its own (PrimordialBeast#getDimensions).
	KUN_PENG("kun_peng", Tier.PRIMORDIAL, QiElement.WATER, Body.FISH, Moves.FLYING, Signature.BREACH, true, 16.0f, 9.6f, 0.3, 1.0, 1.0, 0x24506E, 0xE8E8F0),
	TRUE_PHOENIX("true_phoenix", Tier.PRIMORDIAL, QiElement.FIRE, Body.BIRD, Moves.FLYING, Signature.REBIRTH, true, 9.6f, 8.0f, 0.55, 1.0, 1.0, 0xC8241E, 0xFFC830),
	TRUE_DRAGON("true_dragon", Tier.PRIMORDIAL, QiElement.WOOD, Body.DRAGON, Moves.FLYING, Signature.STORM, true, 11.2f, 9.6f, 0.5, 1.0, 1.0, 0xD8A830, 0xE8F4FF);

	/**
	 * How common a kind is, and the ranks it comes in (PlayerCultivation#rank: realm and stage as one number): the common beasts of Qi
	 * Refining, the uncommon from Qi Refining Late to Foundation Building Grand Perfection, the rare from Foundation Building Late up to
	 * Core Formation Grand Perfection, the strongest of the wild; and the elders, from Void Shattering Early to Entering Nirvana Grand
	 * Perfection, who are never born wild (no weight, no lands: each rises over its blood pool, BloodPools); and the primordial beasts,
	 * True Immortals of Grand Perfection all, who come only to their lairs in the Immortal Realm (PrimordialBeasts).
	 */
	public enum Tier {
		COMMON(0, 3, 60, 20), UNCOMMON(2, 7, 30, 40), RARE(6, 11, 10, 40), ELDER(28, 39, 0, 0), PRIMORDIAL(43, 43, 0, 0);

		public final int minRank, maxRank;
		/** Its weight among a land's beasts in the lower realms, and in the Upper Realm (richer in qi: its beasts the stronger). */
		public final int weight, upperWeight;

		Tier(int minRank, int maxRank, int weight, int upperWeight) {
			this.minRank = minRank;
			this.maxRank = maxRank;
			this.weight = weight;
			this.upperWeight = upperWeight;
		}
	}

	/** Its build (the model's plan on the client: BeastModels), and so how it moves and strikes. A fish: the Kun, in the sea of clouds. */
	public enum Body { QUADRUPED, SERPENT, TURTLE, BIRD, APE, CROCODILE, DRAGON, FISH }

	/** Where it goes: on foot, on foot and in the water, in the air, in the water only. */
	public enum Moves { GROUND, AMPHIBIOUS, FLYING, SWIMMING }

	/**
	 * Its signature move (DemonBeastGoals); the primordial beasts' their own ways (PrimordialBeast): the Kun breaking from the clouds as the
	 * Peng, the Phoenix reborn, the Dragon's storm.
	 */
	public enum Signature { CHARGE, PACK, VENOM, SHELL, SWOOP, THROW, POUNCE, ICE_SLAM, POUND, DRAG, SPARK_CHARGE, BREATH, DIVE, COIL, STUN_CHARGE, WATER_JET,
		BREACH, REBIRTH, STORM }

	private final String id;
	public final Tier tier;
	public final QiElement element;
	public final Body body;
	public final Moves moves;
	public final Signature signature;
	/** Whether it goes for what comes near (else it only fights back: the turtle, the antelope). */
	public final boolean territorial;
	/** Its size at its kind's lowest rank (blocks; an elder's as the game draws it, {@link #modelScale}), growing with its rank (DemonBeast#scale). */
	public final float width, height;
	/** Its speed, and its health and armour by a cultivator's of its rank (CultivationStats#applyNpc). */
	public final double speed, healthScale, armourScale;
	/** Its spawn egg's colours. */
	public final int eggBase, eggSpots;
	/** Its lands: {@code #defying-the-heavens:beast/<id>}. */
	public final TagKey<Biome> lands;

	BeastKind(String id, Tier tier, QiElement element, Body body, Moves moves, Signature signature, boolean territorial, float width, float height,
			double speed, double healthScale, double armourScale, int eggBase, int eggSpots) {
		this.id = id;
		this.tier = tier;
		this.element = element;
		this.body = body;
		this.moves = moves;
		this.signature = signature;
		this.territorial = territorial;
		this.width = width;
		this.height = height;
		this.speed = speed;
		this.healthScale = healthScale;
		this.armourScale = armourScale;
		this.eggBase = eggBase;
		this.eggSpots = eggSpots;
		this.lands = TagKey.create(Registries.BIOME, DefyingTheHeavens.id("beast/" + id));
	}

	public String getId() { return id; }

	public Component getDisplayName() { return Component.translatable("entity." + DefyingTheHeavens.MOD_ID + "." + id); }

	/** A pack, how many come together (the wolves hunt in packs; the monkeys come in troops). */
	public int packSize(net.minecraft.util.RandomSource random) {
		return switch (this) {
			case SHADOW_WOLF -> 3 + random.nextInt(3);
			case FIRE_MONKEY -> 2 + random.nextInt(2);
			case LIGHTNING_ANTELOPE -> 1 + random.nextInt(3);
			default -> 1;
		};
	}

	/** What it leaves besides its core: its hide, meat, bones, feathers, scales. */
	public Item[] remains() {
		return switch (this) {
			case IRON_HIDE_BOAR -> new Item[] {Items.PORKCHOP, Items.LEATHER};
			case SHADOW_WOLF -> new Item[] {Items.BONE};
			case JADE_VIPER, JADE_SCALED_PYTHON -> new Item[] {Items.STRING, Items.SPIDER_EYE};
			case ROCK_SHELL_TURTLE -> new Item[] {Items.SCUTE};
			case WIND_HAWK, GOLDEN_WINGED_ROC -> new Item[] {Items.FEATHER, Items.CHICKEN};
			case FIRE_MONKEY, EMBER_FOX -> new Item[] {Items.BLAZE_POWDER};
			case BLOODFANG_TIGER, EARTHQUAKE_BEAR, LIGHTNING_ANTELOPE -> new Item[] {Items.LEATHER, Items.BEEF};
			case FROST_APE -> new Item[] {Items.LEATHER, Items.SNOWBALL};
			case IRON_JAW_CROCODILE, HORNED_THUNDER_RHINO -> new Item[] {Items.LEATHER, Items.BONE};
			case FLOOD_DRAGON -> new Item[] {Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS};
			case ELDER_WHITE_TIGER -> new Item[] {Items.LEATHER, Items.BONE};
			case NINE_TAILED_FOX -> new Item[] {Items.BLAZE_POWDER, Items.LEATHER};
			case MOUNTAIN_LIZARD_KING -> new Item[] {Items.SCUTE, Items.BONE};
			case AZURE_DRAGON -> new Item[] {Items.PRISMARINE_SHARD, Items.BONE};
			case KUN_PENG -> new Item[] {Items.PRISMARINE_CRYSTALS, Items.FEATHER};
			case TRUE_PHOENIX -> new Item[] {Items.BLAZE_ROD, Items.FEATHER};
			case TRUE_DRAGON -> new Item[] {Items.PRISMARINE_SHARD, Items.BONE};
		};
	}

	/** Whether it is one of the elders (Tier#ELDER), who keep the blood pools. */
	public boolean isElder() {
		return tier == Tier.ELDER;
	}

	/** Whether it is one of the primordial beasts (Tier#PRIMORDIAL), the Immortal Realm's world bosses. */
	public boolean isPrimordial() {
		return tier == Tier.PRIMORDIAL;
	}

	/** How many times its realm's strength it has, its body and its blows: a primordial beast's more (PrimordialBeast#MIGHT), the rest their own. */
	public double might() {
		return isPrimordial() ? PrimordialBeast.MIGHT : 1;
	}

	/**
	 * How much larger the game draws it than its model (BeastModels), its size ({@link #width}, {@link #height}) the same: the elders huge
	 * (the user, 2026-10-25, of the White Tiger: "make it huge"), the dragon, longest of them, a little less. It grows from there with
	 * its rank (DemonBeast#scale).
	 */
	public float modelScale() {
		return switch (this) {
			case ELDER_WHITE_TIGER, NINE_TAILED_FOX, MOUNTAIN_LIZARD_KING -> 2.0f;
			case AZURE_DRAGON -> 1.6f;
			// The primordial beasts, vast (the user, 2026-10-26: "make all of them larger"): the Kun some fifty blocks long (the spell's,
			// tools/spell-art's Void#kun), the Peng's wings sixty across, the Phoenix's forty, the Dragon, longest of all, some eighty.
			case KUN_PENG -> 6.4f;
			case TRUE_PHOENIX -> 5.5f;
			case TRUE_DRAGON -> 5.1f;
			default -> 1.0f;
		};
	}

	public static BeastKind byId(String id) {
		for (BeastKind kind : values()) if (kind.id.equals(id)) return kind;
		return null;
	}
}
