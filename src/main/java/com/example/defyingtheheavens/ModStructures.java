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
 *   chunks, since so few of the islands are wide and level enough.</li>
 * </ul>
 */
public final class ModStructures {
	public static final ResourceKey<Structure> SECT = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("sect"));
	public static final ResourceKey<Structure> SECT_UPPER_REALM = ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id("sect_upper_realm"));

	public static final Codec<SectStructure> SECT_CODEC = Structure.simpleCodec(SectStructure::new);
	public static final StructureType<SectStructure> SECT_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			DefyingTheHeavens.id("sect"), () -> SECT_CODEC);
	public static final StructurePieceType SECT_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			DefyingTheHeavens.id("sect_piece"), (StructurePieceType.ContextlessType) SectPiece::new);

	/** Touching this class registers the types. */
	public static void register() {
	}

	private ModStructures() {}
}
