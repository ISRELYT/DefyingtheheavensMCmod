package com.example.defyingtheheavens;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * The sect structure's types. The structures themselves and where they may appear are data files, one pair per realm so
 * each can be spaced for its own land (never the Nether or the End):
 * <ul>
 *   <li>{@code worldgen/structure/sect.json}, biomes {@code #defying-the-heavens:has_structure/sect} (open Overworld land),
 *   placed by {@code worldgen/structure_set/sects.json}: one try per 32 x 32 chunks, kept 6 chunks clear of villages;</li>
 *   <li>{@code worldgen/structure/sect_upper_realm.json}, biomes {@code #defying-the-heavens:has_structure/sect_upper_realm}
 *   (every Upper Realm island biome), placed by {@code worldgen/structure_set/sects_upper_realm.json}: one try per 14 x 14
 *   chunks, since so few of the islands are wide and level enough;</li>
 *   <li>{@code worldgen/structure/holy_land.json}, biomes {@code #defying-the-heavens:has_structure/holy_land} (the Great Void's
 *   steppes and Dao fields), placed by {@code worldgen/structure_set/holy_lands.json}, the Great Void's Holy Lands rare (the user's
 *   call, 2026-10-25: about one in 2,000 x 2,000 blocks; surveyed that day, one try in twenty finds a continent wide and level enough
 *   for HolyLandPlan's grounds);</li>
 *   <li>{@code worldgen/structure/demon_palace.json} (a den: DenStructure), biomes {@code #defying-the-heavens:has_structure/demon_palace}
 *   (the same lands), in the same structure set as the Holy Lands, alike in weight: each site tries one of the two (the other after
 *   it if the first finds no ground, as it seldom will where the first didn't), so never one on top of the other. The set tries a
 *   site every 20 x 20 chunks (it was 28 x 28 with the Holy Lands alone: half as many sites each), each of the two still about one in
 *   2,000 x 2,000 blocks (the user's call for the palaces, as rare as the Holy Lands).</li>
 * </ul>
 * And the Netherworld's Ghost Kings' palaces: {@code worldgen/structure/ghost_palace.json}, biomes
 * {@code #defying-the-heavens:has_structure/ghost_palace} (the Fields of the Other Shore, the Bone Wastes), placed by
 * {@code worldgen/structure_set/ghost_palaces.json}: one try per 24 x 24 chunks (about one in five comes to a palace: the
 * biomes, level ground, the Gate's clearance).
 */
public final class ModStructures {
	public static final ResourceKey<Structure> SECT = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("sect"));
	public static final ResourceKey<Structure> SECT_UPPER_REALM = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("sect_upper_realm"));
	public static final ResourceKey<Structure> HOLY_LAND = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("holy_land"));
	public static final ResourceKey<Structure> DEMON_PALACE = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("demon_palace"));

	public static final Codec<SectStructure> SECT_CODEC = Structure.simpleCodec(SectStructure::new);
	public static final StructureType<SectStructure> SECT_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			DefyingTheHeavens.id("sect"), () -> SECT_CODEC);
	public static final StructurePieceType SECT_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			DefyingTheHeavens.id("sect_piece"), (StructurePieceType.ContextlessType) SectPiece::new);

	/**
	 * The demon clans' dens (DenStructure): {@code worldgen/structure/demon_clan.json}, biomes {@code #has_structure/demon_clan}
	 * (the races' lands, tags {@code clan/<race>}), placed by {@code structure_set/demon_clans.json} (one try per 44 x 44 chunks,
	 * kept clear of villages; of sects by DenStructure itself); and {@code demon_clan_upper_realm.json} on the Upper Realm's islands
	 * ({@code structure_set/demon_clans_upper_realm.json}).
	 */
	public static final ResourceKey<Structure> DEMON_CLAN = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("demon_clan"));
	public static final ResourceKey<Structure> DEMON_CLAN_UPPER_REALM = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("demon_clan_upper_realm"));
	public static final Codec<DenStructure> DEN_CODEC = Structure.simpleCodec(DenStructure::new);
	public static final StructureType<DenStructure> DEN_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			DefyingTheHeavens.id("den"), () -> DEN_CODEC);

	public static final ResourceKey<Structure> GHOST_PALACE = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("ghost_palace"));
	public static final Codec<GhostPalaceStructure> GHOST_PALACE_CODEC = Structure.simpleCodec(GhostPalaceStructure::new);
	public static final StructureType<GhostPalaceStructure> GHOST_PALACE_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			DefyingTheHeavens.id("ghost_palace"), () -> GHOST_PALACE_CODEC);
	public static final StructurePieceType GHOST_PALACE_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			DefyingTheHeavens.id("ghost_palace_piece"), (StructurePieceType.ContextlessType) GhostPalacePiece::new);

	/**
	 * The Immortal Realm's other cities (ImmortalCityStructure, phase 2): {@code worldgen/structure/immortal_city.json}, biomes
	 * {@code #has_structure/immortal_city} (the Jade Peaks, the Immortal Gardens), {@code terrain_adaptation} beard_thin (each building on its
	 * own ground, the land shaped round it), placed by {@code structure_set/immortal_cities.json}: tried every other chunk, built only at its
	 * cell's site (ModNoiseSettings#citySite, the ground rolled gently for it). The Jade Capital on the main island is no structure
	 * (ImmortalCapital).
	 */
	public static final ResourceKey<Structure> IMMORTAL_CITY = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("immortal_city"));
	public static final Codec<ImmortalCityStructure> IMMORTAL_CITY_CODEC = Structure.simpleCodec(ImmortalCityStructure::new);
	public static final StructureType<ImmortalCityStructure> IMMORTAL_CITY_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			DefyingTheHeavens.id("immortal_city"), () -> IMMORTAL_CITY_CODEC);
	/**
	 * The Immortal Realm's immortal sects (ImmortalSects, ImmortalSectPlan; phase 2): {@code worldgen/structure/immortal_sect.json} (a sect,
	 * SectStructure), biomes {@code #has_structure/immortal_sect} (the Jade Peaks, the Immortal Gardens, the Celestial Heights), beard_thin,
	 * placed by {@code structure_set/immortal_sects.json}: tried every other chunk, built only at its cell's site (ModNoiseSettings#sectSite,
	 * the land rolled gently for it, clear of the main island and the cities).
	 */
	public static final ResourceKey<Structure> IMMORTAL_SECT = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("immortal_sect"));

	/** Touching this class registers the types. */
	public static void register() {
	}

	private ModStructures() {}
}
