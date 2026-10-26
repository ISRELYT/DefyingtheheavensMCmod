package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Great Void's Demon Palaces (DemonPalaces, DemonPalacePlan, DemonPalacePieces, PalaceSculpt; the user's yes, 2026-10-25): every
 * race's palace laid out with its great torii, its Way of Beasts, the blood ring at its heart, the Sovereign's hall and the colossus
 * behind it, nothing standing on another and all of it within the chunks worldgen draws a structure in, with the posts a Holy Land's
 * twenty need; the race of each land; its name and its master's title; its hoards' blood of the Great Void's realms; its great beasts
 * whole (nothing of them floating free: the user's rule for the elders, 2026-10-25), and its stone nothing a spring opens in; and the
 * Holy Lands' sites shared alike.
 */
public class DemonPalaceGameTests implements FabricGameTest {
	private static final Bloodline[] RACES = {Bloodline.TIGER, Bloodline.LIZARD, Bloodline.FOX, Bloodline.DRAGON};

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyRacesPalaceHoldsItsTwenty(GameTestHelper helper) {
		for (Bloodline race : RACES) {
			for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
				List<StructurePiece> pieces = DemonPalacePlan.layOut(race.ordinal() * 104729L + facing.ordinal(), facing, race);
				String what = race.getId() + " " + facing;
				Map<SectPiece.Kind, Integer> counts = new EnumMap<>(SectPiece.Kind.class);
				CompoundTag blueprint = null;
				SectPiece hall = null;
				List<SectPiece> buildings = new ArrayList<>();
				for (StructurePiece piece : pieces) {
					SectPiece sect = (SectPiece) piece;
					counts.merge(sect.kind(), 1, Integer::sum);
					if (sect.kind() == SectPiece.Kind.PLAZA) blueprint = sect.extra().getCompound("Blueprint");
					if (sect.kind() == SectPiece.Kind.PALACE_HALL) hall = sect;
					helper.assertTrue(sect.race == race, "Every piece of a palace its race's (" + what + ", " + sect.kind() + ")");
					// Worldgen draws a structure's pieces only in the eight chunks round its start (here the middle of the grounds, with
					// a few blocks to spare: SectStructure#HOLY_SPOTS).
					BoundingBox box = sect.getBoundingBox();
					helper.assertTrue(Math.max(Math.max(-box.minX(), box.maxX()), Math.max(-box.minZ(), box.maxZ())) <= 131,
							"Every piece within the chunks worldgen draws it in (" + what + ", " + sect.kind() + " " + box + ")");
					// The blood ring's square holds the plaza at its heart: it and the grounds stand under or round the rest.
					if (sect.kind() == SectPiece.Kind.TERRACE || sect.kind() == SectPiece.Kind.PLAZA || sect.kind() == SectPiece.Kind.PALACE_POOL) continue;
					for (SectPiece other : buildings) {
						helper.assertTrue(!other.getBoundingBox().intersects(box), "Nothing of a palace stands on another (" + what + ", " + sect.kind()
								+ " and " + other.kind() + ")");
					}
					buildings.add(sect);
				}
				helper.assertTrue(counts.getOrDefault(SectPiece.Kind.PALACE_GATE, 0) == 3 && counts.getOrDefault(SectPiece.Kind.PALACE_STATUE, 0) == 4
						&& counts.getOrDefault(SectPiece.Kind.PALACE_POOL, 0) == 1 && counts.getOrDefault(SectPiece.Kind.PALACE_HALL, 0) == 1
						&& counts.getOrDefault(SectPiece.Kind.PALACE_COLOSSUS, 0) == 1 && counts.getOrDefault(SectPiece.Kind.MEDITATION_HALL, 0) == 4
						&& counts.getOrDefault(SectPiece.Kind.ELDER_HOUSE, 0) == 6 && counts.getOrDefault(SectPiece.Kind.TREASURY, 0) == 1
						&& counts.getOrDefault(SectPiece.Kind.TRAINING, 0) == 1, "Its great torii and two lesser, four beasts on the way, the blood ring, "
						+ "the hall and the colossus, four disciples' halls, six elders' houses, the vault and the sparring ground (" + what + ": " + counts + ")");
				helper.assertTrue(blueprint != null && race.getId().equals(blueprint.getString("Race")), "Its blueprint names its race (" + what + ")");
				helper.assertTrue(blueprint.getInt("Capacity") == DemonPalaces.MEMBERS, "Room for all twenty (" + what + ")");
				int radius = blueprint.getInt("Radius");
				helper.assertTrue(radius > 100 && radius <= Formations.HOLY_MAX_RADIUS, "Its barrier round all of it (" + what + ": " + radius + ")");
				CompoundTag posts = blueprint.getCompound("Posts");
				helper.assertTrue(posts.getLongArray("MASTER_SEAT").length == 1 && posts.getLongArray("CORE").length == 4
						&& posts.getLongArray("GATE").length == 1 && posts.getLongArray("GATE_GUARD").length == 2
						&& posts.getLongArray("TREASURY").length > 0 && posts.getLongArray("TRAINING").length > 0
						&& posts.getLongArray("MEDITATION").length >= 13 && posts.getLongArray("DWELLING").length >= DemonPalaces.MEMBERS - 1
						&& posts.getLongArray("WAY").length > 10 && posts.getLongArray("COURTYARD").length > 8, "The posts its people keep (" + what + ")");
				BlockPos seat = BlockPos.of(posts.getLongArray("MASTER_SEAT")[0]);
				helper.assertTrue(hall != null && hall.getBoundingBox().isInside(seat), "The Sovereign's seat in the Sovereign's hall (" + what + ")");
				// The elders' houses first among the homes, nearest the plaza first.
				long[] homes = posts.getLongArray("DWELLING");
				BlockPos core = BlockPos.of(posts.getLongArray("CORE")[0]);
				helper.assertTrue(BlockPos.of(homes[0]).distSqr(core) <= BlockPos.of(homes[5]).distSqr(core), "The nearest elder's house first (" + what + ")");
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachLandItsRace(GameTestHelper helper) {
		var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		RandomSource random = RandomSource.create(3);
		Object[][] lands = {{ModBiomes.DAO_FIELD_METAL, Bloodline.TIGER}, {ModBiomes.DAO_FIELD_FIRE, Bloodline.FOX}, {ModBiomes.DAO_FIELD_EARTH, Bloodline.LIZARD},
				{ModBiomes.DAO_FIELD_WOOD, Bloodline.DRAGON}, {ModBiomes.DAO_FIELD_WATER, Bloodline.DRAGON}};
		for (Object[] land : lands) {
			@SuppressWarnings("unchecked")
			Holder<Biome> biome = biomes.getHolderOrThrow((net.minecraft.resources.ResourceKey<Biome>) land[0]);
			for (int i = 0; i < 20; i++) helper.assertTrue(DemonPalaces.raceOf(biome, random) == land[1], "A Dao field's palace its element's race: " + land[0]);
		}
		Holder<Biome> steppes = biomes.getHolderOrThrow(ModBiomes.VOID_STEPPES);
		Set<Bloodline> seen = java.util.EnumSet.noneOf(Bloodline.class);
		for (int i = 0; i < 400; i++) seen.add(DemonPalaces.raceOf(steppes, random));
		helper.assertTrue(seen.size() == 4 && !seen.contains(Bloodline.HUMAN), "On the steppes any of the four races: " + seen);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void itsNameAndItsSovereign(GameTestHelper helper) {
		RandomSource random = RandomSource.create(11);
		for (Bloodline race : RACES) {
			String word = Character.toUpperCase(race.getId().charAt(0)) + race.getId().substring(1);
			for (int i = 0; i < 30; i++) {
				String name = SectNames.palace(random, race);
				helper.assertTrue(name.endsWith(" " + word + " Demon Palace") && name.length() > (word + " Demon Palace").length() + 2,
						"A palace named for its race: " + name);
			}
		}
		helper.assertTrue(key(NpcTitle.SECT_MASTER.getDisplayName(true, true)).equals(ModLang.titleKey("demon_sovereign"))
				&& key(NpcTitle.SECT_MASTER.getDisplayName(false, true)).equals(ModLang.titleKey("patriarch"))
				&& key(NpcTitle.SECT_MASTER.getDisplayName(true, false)).equals(ModLang.titleKey("clan_head"))
				&& key(NpcTitle.ELDER.getDisplayName(true, true)).equals(ModLang.titleKey(NpcTitle.ELDER.getId())),
				"A palace's master its Demon Sovereign, a Holy Land's its Patriarch, a den's its Clan Head; the elders elders");
		helper.succeed();
	}

	private static String key(net.minecraft.network.chat.Component c) {
		return c.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void itsHoardsKeepTheGreatVoidsBlood(GameTestHelper helper) {
		RandomSource random = RandomSource.create(5);
		int[] seen = new int[Realm.values().length];
		int found = 0;
		for (int i = 0; i < 20000; i++) {
			ItemStack blood = DemonBlood.hoarded(Bloodline.DRAGON, true, true, SectPiece.TREASURY_LOOT, random);
			if (blood.isEmpty()) continue;
			found++;
			Realm realm = DemonBloodItem.realm(blood);
			seen[realm.ordinal()]++;
		}
		helper.assertTrue(found > 1500 && found < 3300, "A palace's hoard keeps a phial now and then: " + found);
		for (Realm realm : Realm.values()) {
			boolean taught = realm.ordinal() >= Realm.HEAVENLY_BEING.ordinal() && realm.ordinal() <= Realm.ENTERING_NIRVANA.ordinal();
			helper.assertTrue(taught == (seen[realm.ordinal()] > 0), "Of the Great Void's realms, Heavenly Being to Entering Nirvana (its library's): "
					+ realm + " " + seen[realm.ordinal()]);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void itsGreatBeastsAreWhole(GameTestHelper helper) {
		for (Bloodline race : RACES) {
			for (boolean big : new boolean[] {true, false}) {
				DemonPalacePieces.Sculpture sculpture = big ? DemonPalacePieces.colossus(race) : DemonPalacePieces.statue(race);
				PalaceSculpt.Grid g = sculpture.grid();
				String what = race.getId() + (big ? " colossus" : " statue");
				// Every voxel of it joined to the ground through the rest (corners touching count: a voxelled tube steps diagonally).
				BitSet reached = new BitSet();
				ArrayDeque<int[]> queue = new ArrayDeque<>();
				int all = 0, eyes = 0, top = 0;
				for (int y = 0; y < g.h; y++) {
					for (int z = 0; z < g.d; z++) {
						for (int x = 0; x < g.w; x++) {
							PalaceSculpt.Paint paint = g.get(x, y, z);
							if (paint == null) continue;
							all++;
							top = Math.max(top, y);
							if (paint == PalaceSculpt.Paint.EYE) eyes++;
							if (y == 0) {
								reached.set(x + g.w * (z + g.d * y));
								queue.add(new int[] {x, y, z});
							}
						}
					}
				}
				while (!queue.isEmpty()) {
					int[] at = queue.poll();
					for (int dx = -1; dx <= 1; dx++) {
						for (int dy = -1; dy <= 1; dy++) {
							for (int dz = -1; dz <= 1; dz++) {
								int x = at[0] + dx, y = at[1] + dy, z = at[2] + dz;
								if (g.get(x, y, z) == null) continue;
								int i = x + g.w * (z + g.d * y);
								if (reached.get(i)) continue;
								reached.set(i);
								queue.add(new int[] {x, y, z});
							}
						}
					}
				}
				helper.assertTrue(reached.cardinality() == all, "Nothing of the " + what + " floats free of it: " + (all - reached.cardinality()) + " of " + all);
				helper.assertTrue(eyes > 0, "The " + what + " has its eyes");
				helper.assertTrue(top < g.h - 1, "Nothing of the " + what + " cut off by its grid's top");
				helper.assertTrue(sculpture.laid().cardinality() < all, "The " + what + " only a shell (its inside never seen)");
				if (big) helper.assertTrue(top > 35, "The colossus towers (" + what + ": " + top + " high)");
			}
		}
		helper.succeed();
	}

	/** What springs open in (vanilla's spring_water and spring_lava): never in what a palace builds. */
	private static final Set<Block> SPRINGS_OPEN = Set.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DEEPSLATE, Blocks.TUFF,
			Blocks.CALCITE, Blocks.DIRT);

	@GameTest(template = EMPTY_STRUCTURE)
	public void builtOfItsRacesStone(GameTestHelper helper) {
		Set<SectPalette> palettes = new java.util.HashSet<>();
		for (Bloodline race : RACES) {
			SectPalette p = SectPalette.palace(race);
			palettes.add(p);
			for (Block block : new Block[] {p.foundation, p.paving, p.path, p.wallBase, p.wall, p.pillar, p.beam, p.floor, p.roof, p.roofTrim, p.accent,
					p.baseWall}) {
				helper.assertTrue(!SPRINGS_OPEN.contains(block), "Nothing a spring opens in, in a " + race.getId() + " palace: " + block);
			}
			helper.assertTrue(p.race == race, "A palace's palette its race's");
		}
		helper.assertTrue(palettes.size() == 4, "Each race's palace its own colours");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theHolyLandsSitesShared(GameTestHelper helper) {
		StructureSet set = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(DefyingTheHeavens.id("holy_lands"));
		helper.assertTrue(set != null, "The Holy Lands' structure set");
		int holy = 0, palace = 0;
		for (StructureSet.StructureSelectionEntry entry : set.structures()) {
			if (entry.structure().is(ModStructures.HOLY_LAND)) holy = entry.weight();
			if (entry.structure().is(ModStructures.DEMON_PALACE)) palace = entry.weight();
		}
		helper.assertTrue(holy > 0 && holy == palace, "Holy Lands and Demon Palaces alike at its sites: " + holy + " and " + palace);
		helper.succeed();
	}
}
