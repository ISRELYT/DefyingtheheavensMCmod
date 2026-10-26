package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Immortal Realm's immortal sects (phase 2, 2026-10-25): their twenty, none below Entering Nirvana, their Sect Lord (an Earth Immortal
 * since phase 3) and their names;
 * their sites out over the sea of clouds, open sky under them; and their grounds as worldgen lays them out: every building on floating
 * islands (the user's words: "the ground court buildings should still exist just on floating islands"), each well within its island at
 * its top, the islands apart and at their heights, every bridge landing on both its islands, all of it within the chunks worldgen places
 * it in, the blueprint's twenty homes, the Sect Lord's seat on the highest island, and a barrier round it all.
 */
public class ImmortalSectsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void anImmortalSectsTwenty(GameTestHelper helper) {
		RandomSource random = RandomSource.create(4);
		for (int n = 0; n < 6; n++) {
			int[] ranks = ImmortalSects.foundingRanks(random, ImmortalSects.MEMBERS);
			// Its Sect Lord an Earth Immortal (phase 3), short of that realm's Grand Perfection.
			helper.assertTrue(ranks[0] >= PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.EARLY) && ranks[0] <= PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.LATE),
					"Its Sect Lord an Earth Immortal of Early to Late (" + ranks[0] + ")");
			for (int i = 1; i <= 2; i++) helper.assertTrue(ranks[i] >= PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.LATE), "Its grand elders True Immortals of Late or more");
			for (int i = 3; i <= 6; i++) {
				helper.assertTrue(ranks[i] >= PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY) && ranks[i] <= PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.MID),
						"Its elders True Immortals of Early or Mid");
			}
			for (int i = 7; i < ranks.length; i++) {
				helper.assertTrue(ranks[i] >= ImmortalSects.FLOOR && ranks[i] <= PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION),
						"Its disciples of Entering Nirvana (" + ranks[i] + ")");
			}
		}
		helper.assertTrue(ImmortalSects.FLOOR == PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY)
				&& SectManager.floorRank(ModDimensions.IMMORTAL_REALM) == ImmortalSects.FLOOR, "None below Entering Nirvana, its new disciples at its first stage");
		for (Alignment.Faction faction : Alignment.Faction.values()) {
			String name = SectNames.immortal(random, faction);
			helper.assertTrue(name.endsWith(" Immortal Sect") || name.endsWith(" Immortal Palace") || name.endsWith(" Immortal Mountain"), "Named as an immortal sect: " + name);
		}
		helper.assertTrue(NpcTitle.SECT_MASTER.getDisplayName(false, false, true).getContents() instanceof TranslatableContents t && t.getKey().equals(ModLang.titleKey("sect_lord"))
				&& NpcTitle.ELDER.getDisplayName(false, false, true).getContents() instanceof TranslatableContents e && e.getKey().equals(ModLang.titleKey("elder")),
				"Its master its Sect Lord, its elders as any sect's");
		helper.assertTrue(Formations.IMMORTAL_MAX_RADIUS > Formations.HOLY_MAX_RADIUS, "Its barrier reaches round its islands, wider than a Holy Land's");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void immortalSectsFloatOverTheClouds(GameTestHelper helper) {
		ImmortalCitiesGameTests.Land land = ImmortalCitiesGameTests.land(helper);
		net.minecraft.world.level.levelgen.RandomState state = land.state();
		List<ModNoiseSettings.CitySite> sites = new ArrayList<>();
		for (int cx = -4; cx <= 4; cx++) {
			for (int cz = -4; cz <= 4; cz++) {
				ModNoiseSettings.CitySite site = ModNoiseSettings.sectSite(state, cx, cz);
				if (site == null) continue;
				helper.assertTrue(site.equals(ModNoiseSettings.sectSite(state, cx, cz)) && Math.floorDiv(site.x(), ModNoiseSettings.SECT_CELL) == cx
						&& Math.floorDiv(site.z(), ModNoiseSettings.SECT_CELL) == cz, "A sect's site the same each time asked, in its own cell");
				helper.assertTrue(Math.hypot(site.x() - ModNoiseSettings.MAIN_X, site.z() - ModNoiseSettings.MAIN_Z)
						> ModNoiseSettings.MAIN_OUTER + ModNoiseSettings.SECT_SEA_REACH, "Clear of the capital's island");
				for (int i = Math.floorDiv(site.x() - 500, ModNoiseSettings.CITY_CELL); i <= Math.floorDiv(site.x() + 500, ModNoiseSettings.CITY_CELL); i++) {
					for (int j = Math.floorDiv(site.z() - 500, ModNoiseSettings.CITY_CELL); j <= Math.floorDiv(site.z() + 500, ModNoiseSettings.CITY_CELL); j++) {
						ModNoiseSettings.CitySite city = ModNoiseSettings.citySite(state, i, j);
						helper.assertTrue(city == null || Math.hypot(city.x() - site.x(), city.z() - site.z())
								> ModNoiseSettings.CITY_DISTRICT_OUTER + ModNoiseSettings.SECT_SEA_REACH, "No city's land near a sect");
					}
				}
				helper.assertTrue(Math.abs(site.level() - ModNoiseSettings.SECT_LEVEL_Y) <= 6 && site.level() - ModDimensions.IMMORTAL_CLOUD_Y >= 50,
						"Its islands high over the clouds (" + site.level() + ")");
				sites.add(site);
			}
		}
		helper.assertTrue(sites.size() >= 81 / 6, "Sects about the realm (" + sites.size() + " of 81 cells)");
		// Open sky under them as the terrain makes it (no massif, no pinnacle in the clearing), and the structure finding it so.
		var registries = helper.getLevel().registryAccess();
		int built = 0, tried = 0;
		for (ModNoiseSettings.CitySite site : sites) {
			if (tried++ >= 6) break;
			for (int ring = 0; ring <= 3; ring++) {
				for (int k = 0; k < Math.max(1, ring * 8); k++) {
					double angle = Math.PI * 2 * k / Math.max(1, ring * 8);
					int x = site.x() + (int) Math.round(Math.cos(angle) * ring * 55), z = site.z() + (int) Math.round(Math.sin(angle) * ring * 55);
					int top = land.generator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, land.heights(), state);
					helper.assertTrue(top <= ModDimensions.IMMORTAL_CLOUD_Y - ImmortalSectPlan.SKY_CLEAR, "Open sky under a sect (land at Y " + top + ", "
							+ (x - site.x()) + ", " + (z - site.z()) + " off its site)");
				}
			}
			net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(site.x() >> 4 & ~1, site.z() >> 4 & ~1);
			var context = new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(registries, land.generator(), land.generator().getBiomeSource(),
					state, helper.getLevel().getStructureManager(), helper.getLevel().getSeed(), chunk, land.heights(), biome -> true);
			int y = ImmortalSectPlan.skyLevel(context, new SectStructure.Frame(Direction.NORTH, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), ImmortalSectPlan.W,
					ImmortalSectPlan.D), site);
			if (y != SectStructure.NO_SITE) built++;
		}
		helper.assertTrue(built == Math.min(6, sites.size()), "Every site holds its sect (" + built + " of " + Math.min(6, sites.size()) + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anImmortalSectsIslands(GameTestHelper helper) {
		// The islands apart: a block more than the most their rims waver, and six blocks of sky, between any two.
		ImmortalSectPlan.Isle[] isles = ImmortalSectPlan.ISLES;
		for (int i = 0; i < isles.length; i++) {
			for (int j = 0; j < isles.length; j++) {
				if (i == j) continue;
				ImmortalSectPlan.Isle a = isles[i], b = isles[j];
				for (int k = 0; k < 72; k++) {
					double angle = Math.PI * 2 * k / 72, pu = a.u() + Math.cos(angle) * a.ru() * 1.08, pv = a.v() + Math.sin(angle) * a.rv() * 1.08;
					double du = (pu - b.u()) / (b.ru() * 1.08 + 6), dv = (pv - b.v()) / (b.rv() * 1.08 + 6);
					helper.assertTrue(du * du + dv * dv > 1, "Two islands apart (" + a + ", " + b + ")");
				}
			}
		}
		for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST}) {
			List<StructurePiece> pieces = SectStructure.layOutImmortal(7L + facing.ordinal(), facing);
			Map<SectPiece.Kind, List<SectPiece>> kinds = new EnumMap<>(SectPiece.Kind.class);
			for (StructurePiece piece : pieces) kinds.computeIfAbsent(((SectPiece) piece).kind(), k -> new ArrayList<>()).add((SectPiece) piece);
			Object[][] want = {{SectPiece.Kind.PEAK_ISLAND, 14}, {SectPiece.Kind.PEAK_BRIDGE, 13}, {SectPiece.Kind.HOLY_GATE, 1}, {SectPiece.Kind.HOLY_STATUE, 4},
					{SectPiece.Kind.HOLY_TOWER, 2}, {SectPiece.Kind.PLAZA, 1}, {SectPiece.Kind.MEDITATION_HALL, 4}, {SectPiece.Kind.ALTAR, 1},
					{SectPiece.Kind.TREASURY, 1}, {SectPiece.Kind.TRAINING, 1}, {SectPiece.Kind.PAGODA, 1}, {SectPiece.Kind.HOLY_ALCHEMY, 1},
					{SectPiece.Kind.GARDEN, 2}, {SectPiece.Kind.ELDER_HOUSE, 6}, {SectPiece.Kind.MAIN_HALL, 1}, {SectPiece.Kind.TERRACE, 0}};
			for (Object[] w : want) {
				int count = kinds.getOrDefault((SectPiece.Kind) w[0], List.of()).size();
				helper.assertTrue(count == (Integer) w[1], facing + ": " + w[1] + " " + w[0] + " (" + count + ")");
			}
			// Everything within the eight chunks of its start that worldgen places pieces in (its frame about the start chunk's middle, 0, 0).
			for (StructurePiece piece : pieces) {
				BoundingBox box = piece.getBoundingBox();
				helper.assertTrue(box.minX() >= -135 && box.maxX() <= 135 && box.minZ() >= -135 && box.maxZ() <= 135, facing + ": within its chunks (" + box + ")");
			}
			// The islands at their heights over the core's (64 here), the Sect Lord's the highest.
			List<SectPiece> islands = kinds.get(SectPiece.Kind.PEAK_ISLAND);
			int highest = Integer.MIN_VALUE;
			for (SectPiece island : islands) {
				int rise = island.getBoundingBox().minY() - 64;
				boolean planned = false;
				for (ImmortalSectPlan.Isle isle : isles) planned |= isle.rise() == rise;
				helper.assertTrue(planned, facing + ": an island at its height (" + rise + ")");
				highest = Math.max(highest, rise);
			}
			helper.assertTrue(highest == ImmortalSectPlan.LORD.rise(), facing + ": the Sect Lord's island the highest");
			// Worldgen grows none of the realm's trees or bamboo on them (SectClearingMixin): each island its sect's grounds, out to a margin.
			for (SectPiece island : islands) {
				BoundingBox box = island.getBoundingBox();
				BlockPos middle = new BlockPos((box.minX() + box.maxX()) / 2, box.minY(), (box.minZ() + box.maxZ()) / 2);
				helper.assertTrue(island.onGrounds(middle, 4) && !island.onGrounds(middle.offset(box.getXSpan() + 10, 0, 0), 4), facing + ": an island its sect's grounds");
			}
			// Every building on an island, well within its rim, at its top.
			for (Map.Entry<SectPiece.Kind, List<SectPiece>> entry : kinds.entrySet()) {
				if (entry.getKey().isPeak()) continue;
				for (SectPiece building : entry.getValue()) {
					BoundingBox box = building.getBoundingBox();
					SectPiece under = null;
					for (SectPiece island : islands) {
						if (ImmortalSectPieces.onIsland(island, (box.minX() + box.maxX()) / 2, (box.minZ() + box.maxZ()) / 2, 1.0)) under = island;
					}
					boolean within = under != null;
					for (int[] c : new int[][] {{box.minX(), box.minZ()}, {box.maxX(), box.minZ()}, {box.minX(), box.maxZ()}, {box.maxX(), box.maxZ()}}) {
						within &= under != null && ImmortalSectPieces.onIsland(under, c[0], c[1], 0.95);
					}
					helper.assertTrue(within && box.minY() == under.getBoundingBox().minY(), facing + ": the " + entry.getKey() + " on its island, within its rim");
				}
			}
			// Every bridge lands on an island at both ends, at that island's top.
			for (SectPiece bridge : kinds.get(SectPiece.Kind.PEAK_BRIDGE)) {
				for (int z : new int[] {1, bridge.depth() - 2}) {
					for (int x : new int[] {0, 4}) {
						BlockPos end = bridge.toWorld(x, ImmortalSectPieces.deck(bridge, z), z);
						boolean landed = false;
						for (SectPiece island : islands) landed |= ImmortalSectPieces.onIsland(island, end.getX(), end.getZ(), 1.0) && end.getY() == island.getBoundingBox().minY();
						helper.assertTrue(landed, facing + ": a bridge's end on an island's top (" + end.toShortString() + ")");
					}
				}
				for (int z = 1; z < bridge.depth(); z++) {
					int step = ImmortalSectPieces.deck(bridge, z) - ImmortalSectPieces.deck(bridge, z - 1);
					helper.assertTrue(Math.abs(step) <= 1, facing + ": a bridge steps a block at a time");
				}
			}
			// The blueprint: twenty homes, the Sect Lord's seat on the highest island, a barrier round every building.
			SectPiece plaza = kinds.get(SectPiece.Kind.PLAZA).get(0);
			CompoundTag blueprint = plaza.extra().getCompound("Blueprint");
			CompoundTag posts = blueprint.getCompound("Posts");
			long[] seat = posts.getLongArray("MASTER_SEAT");
			helper.assertTrue(blueprint.getInt("Capacity") == ImmortalSects.MEMBERS && posts.getLongArray("DWELLING").length >= ImmortalSects.MEMBERS - 1
					&& seat.length == 1 && BlockPos.of(seat[0]).getY() > 64 + ImmortalSectPlan.LORD.rise(), facing + ": room for its twenty, the Sect Lord's seat on the highest island");
			int radius = blueprint.getInt("Radius");
			BlockPos core = plaza.toWorld(SectPiece.PLAZA_RADIUS, 2, SectPiece.PLAZA_RADIUS);
			for (Map.Entry<SectPiece.Kind, List<SectPiece>> entry : kinds.entrySet()) {
				if (entry.getKey().isPeak()) continue;
				for (SectPiece building : entry.getValue()) {
					BoundingBox box = building.getBoundingBox();
					for (int[] c : new int[][] {{box.minX(), box.minZ()}, {box.maxX(), box.minZ()}, {box.minX(), box.maxZ()}, {box.maxX(), box.maxZ()}}) {
						double dx = c[0] - core.getX(), dz = c[1] - core.getZ(), dy = Math.max(0, box.maxY() - core.getY());
						helper.assertTrue(Math.sqrt(dx * dx + dz * dz + dy * dy) <= radius, facing + ": its barrier round its " + entry.getKey() + " (" + radius + ")");
					}
				}
			}
			helper.assertTrue(radius <= Formations.IMMORTAL_MAX_RADIUS, facing + ": its barrier no wider than the realm lets one be (" + radius + ")");
			helper.assertTrue(posts.getLongArray("WAY").length > 30 && posts.getLongArray("GATE_GUARD").length == 2,
					facing + ": ways over the bridges for its people to walk, two at its gate");
		}
		helper.succeed();
	}
}
