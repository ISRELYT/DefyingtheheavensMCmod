package com.example.defyingtheheavens;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The map of the worlds in the Dao Codex (Codex, its page "map"; drawn by the client's CodexDiagrams). The user's ask, 2026-10-28: "a
 * diagram displaying all the realms and like, have branches between them showing which ones you can get to from what one. And hovering
 * over them should only show ??? if you haven't been there before. If you have been there it should show its name and the qi amount and
 * a recommended realm to be at to go there". And the same day: "always shows how to get to the realms before you've been there
 * (everything else still ??? until you go there)": a world's way in shows from the first.
 * <p>
 * Each world: the advancements that tell a player has been there (any one of them; "path#criterion" for one criterion of an advancement;
 * the client also keeps the worlds it has seen its player in, CodexState), its qi against the lower realms' and its ghostly qi against a
 * soul sand valley's (both read from the numbers the game runs on), the realm to reach before going (the realm its way opens at, or
 * where it stops being deadly), and its way in. {@link #WAYS}: which worlds lead to which.
 */
public final class CodexMap {
	public enum World {
		OVERWORLD("overworld", Level.OVERWORLD, "", null, null, "Overworld", "Where everyone begins."),
		NETHER("nether", Level.NETHER, "minecraft:story/enter_the_nether", Realm.QI_REFINING, null, "Nether", "Through a nether portal."),
		THE_END("the_end", Level.END, "minecraft:story/enter_the_end", Realm.FOUNDATION_BUILDING, null, "The End",
				"Through an end portal, deep in a stronghold."),
		SPATIAL_GAP("spatial_gap", ModDimensions.SPATIAL_GAP, "worlds/spatial_gap", Realm.HEAVENLY_BEING, null, "Spatial Gap",
				"Through the rift at the very centre of the Overworld, or a Spatial Tear. Only those who can fly stay in it."),
		JADE_FIRMAMENT("jade_firmament", ModDimensions.UPPER_REALM, "worlds/jade_firmament", Realm.HEAVENLY_BEING, null, "Jade Firmament",
				"Rise through the Spatial Gap's upper layers."),
		ASTRAL("astral", null, "worlds/astral", AstralRealm.MIN_REALM, null, "Astral Realm",
				"Fly high above the Overworld or the Jade Firmament."),
		GREAT_VOID("great_void", ModDimensions.GREAT_VOID, "worlds/great_void", Realm.VOID_SHATTERING, null, "Great Void",
				"Through a Void Breach, torn high in the Jade Firmament's sky."),
		IMMORTAL_REALM("immortal_realm", ModDimensions.IMMORTAL_REALM, "worlds/immortal_realm", Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION,
				"Immortal Realm", "Through the Immortal Gate beyond the Shore of Nirvana, while it is open."),
		CHAOS_FRONTIER("chaos_frontier", null, "worlds/chaos_frontier", Realm.TRUE_IMMORTAL, null, "Chaos Frontier",
				"Travel far out to the Immortal Realm's edge."),
		NETHERWORLD("netherworld", ModDimensions.NETHERWORLD, "springs/root#netherworld|springs/soul_river|springs/ghost", Realm.NASCENT_SOUL, null,
				"Netherworld", "The dead wake at its Ghost Gate. The living walk into the rift at the very centre of the Nether."),
		INNER_REALM("inner_realm", ModDimensions.INNER_REALM, "arts/inner_realm", Realm.QI_REFINING, null, "Inner Realm",
				"Meditate for a while, anywhere but the Netherworld."),
		SMALL_WORLD("small_world", ModDimensions.SMALL_WORLD, "worlds/small_world|worlds/root#small_world", Realm.TRUE_IMMORTAL, null,
				"Small Worlds", "A True Immortal refines a World Seed into one, and opens its door wherever they stand. Guests come in at their word.");

		private final String id;
		/** Its dimension (null: the Astral Realm's two, the Chaos Frontier's part of the Immortal Realm's; see {@link #at}). */
		private final ResourceKey<Level> dimension;
		private final List<String> visits;
		private final Realm recommended;
		private final Stage recommendedStage;
		private final String english;
		private final String way;

		World(String id, ResourceKey<Level> dimension, String visits, Realm recommended, Stage recommendedStage, String english, String way) {
			this.id = id;
			this.dimension = dimension;
			this.visits = visits.isEmpty() ? List.of() : List.of(visits.split("\\|"));
			this.recommended = recommended;
			this.recommendedStage = recommendedStage;
			this.english = english;
			this.way = way;
		}

		public String id() { return id; }

		public String english() { return english; }

		/** Its way in, in English. */
		public String wayEnglish() { return way; }

		public String nameKey() { return ModLang.codexWorld(id); }

		public String wayKey() { return ModLang.codexWorldWay(id); }

		/**
		 * The advancements that tell it has been visited, any one of them: the mod's own by their path ("worlds/great_void"), others by
		 * their whole id ("minecraft:story/enter_the_nether"), "#criterion" after one for that criterion alone (an advancement done by any
		 * one of its criteria stops hearing the rest). None: known from the first day (the Overworld).
		 */
		public List<String> visits() { return visits; }

		public boolean always() { return visits.isEmpty(); }

		/** The realm to reach before going there (null: anyone may), and the stage of it (null: any). */
		public Realm recommended() { return recommended; }

		public Stage recommendedStage() { return recommendedStage; }

		/** Reached from anywhere, with no way drawn to it: the Inner Realm by meditating, a small world by its door. */
		public boolean anywhere() {
			return this == INNER_REALM || this == SMALL_WORLD;
		}

		/**
		 * Its qi against the lower realms', at its thinnest: the realm's own (RealmSuppressionHandler#realmQi), the Jade Firmament's
		 * (PlayerCultivation#placeFactor), space's for the Astral Realm, and for the Inner Realm how much faster it cultivates than where
		 * the body sits ({@link #qiOfTheBody}).
		 */
		public double qi() {
			return switch (this) {
				case JADE_FIRMAMENT -> PlayerCultivation.UPPER_REALM_QI_MULTIPLIER;
				case ASTRAL -> AstralRealm.SPACE_QI;
				case INNER_REALM -> InnerRealm.CULTIVATION_BONUS;
				case CHAOS_FRONTIER -> RealmSuppressionHandler.realmQi(ModDimensions.IMMORTAL_REALM);
				default -> RealmSuppressionHandler.realmQi(dimension);
			};
		}

		/** Its qi at its richest: a planet's, at most; elsewhere the same everywhere. */
		public double qiTop() {
			return this == ASTRAL ? AstralPlanets.MAX_QI : qi();
		}

		/** The Inner Realm's qi is its body's place's, a little faster. */
		public boolean qiOfTheBody() {
			return this == INNER_REALM;
		}

		/** Its ghostly qi against a soul sand valley's, at its thinnest (GhostQi; 0: none). */
		public double ghostQi() {
			return switch (this) {
				case OVERWORLD -> GhostQi.OVERWORLD;
				case NETHER -> GhostQi.NETHER;
				case NETHERWORLD -> GhostQi.NETHERWORLD;
				case GREAT_VOID -> GhostQi.ANCIENT_BATTLEFIELD;
				default -> 0;
			};
		}

		/** Its ghostly qi at its richest: a soul sand valley in the Nether, the Nine Nether Peaks in the Netherworld. */
		public double ghostQiTop() {
			return switch (this) {
				case NETHER -> GhostQi.SOUL_SAND_VALLEY;
				case NETHERWORLD -> GhostQi.NETHERWORLD * GhostQi.PEAKS_FACTOR;
				default -> ghostQi();
			};
		}

		/** Ghostly qi only on some of its lands: the Great Void's Ancient Battlefields, none on the rest. */
		public boolean ghostQiInPlaces() {
			return this == GREAT_VOID;
		}

		/** The world (x, z) of {@code level} is in, or null on a way between worlds (the Void Breach, the Gate's passage, the Soul River). */
		public static World at(Level level, double x, double z) {
			ResourceKey<Level> where = level.dimension();
			if (ChaosFrontier.isIn(level, x, z)) return CHAOS_FRONTIER;
			if (ModDimensions.isAstral(where)) return ASTRAL;
			for (World world : values()) if (world.dimension == where) return world;
			return null;
		}
	}

	/** A world's visit's advancement ("worlds/great_void": the mod's own; "minecraft:story/enter_the_nether"), its "#criterion" aside. */
	public static ResourceLocation advancementOf(String visit) {
		int hash = visit.indexOf('#');
		String name = hash < 0 ? visit : visit.substring(0, hash);
		return new ResourceLocation(name.contains(":") ? name : DefyingTheHeavens.MOD_ID + ":" + name);
	}

	/** A world's visit's criterion ("netherworld" of "springs/root#netherworld"), or null for the whole advancement. */
	public static String criterionOf(String visit) {
		int hash = visit.indexOf('#');
		return hash < 0 ? null : visit.substring(hash + 1);
	}

	/** A way between two worlds, both ways (a world's way back is its way in, turned round). */
	public record Way(World from, World to) {}

	/** Which worlds lead to which: up from the Overworld through the Spatial Gap, down from it to the dead. */
	public static final List<Way> WAYS = List.of(
			new Way(World.OVERWORLD, World.NETHER),
			new Way(World.OVERWORLD, World.THE_END),
			new Way(World.OVERWORLD, World.SPATIAL_GAP),
			new Way(World.OVERWORLD, World.ASTRAL),
			new Way(World.NETHER, World.NETHERWORLD),
			new Way(World.SPATIAL_GAP, World.JADE_FIRMAMENT),
			new Way(World.JADE_FIRMAMENT, World.ASTRAL),
			new Way(World.JADE_FIRMAMENT, World.GREAT_VOID),
			new Way(World.GREAT_VOID, World.IMMORTAL_REALM),
			new Way(World.IMMORTAL_REALM, World.CHAOS_FRONTIER));

	private CodexMap() {}
}
