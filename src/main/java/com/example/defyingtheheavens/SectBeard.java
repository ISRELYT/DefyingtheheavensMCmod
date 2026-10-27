package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;

/**
 * The land drawn up to the sects' and the cities' buildings' paving (NoiseBasedChunkGeneratorMixin). Their structures are {@code beard_thin}
 * (each building on its own ground, the land shaped round it: the user's ask, 2026-10-25), and vanilla's Beardifier draws the land up to the
 * floor of a piece's box only for a village's pieces (their "ground level delta" of 1: the box's floor is their ground); every other piece's
 * land it leaves a block under its box's floor. A building's box's floor is its paving, the ground's top block there (as the Immortal
 * Realm's capital has it, ImmortalCapital#beard), so the land lay a block under every door: a meditation hall's step a block and a half
 * up, the palace's first stair two, and no walker went in (the user, 2026-10-27: "none of the NPCs are cultivating"; tour survey1). Here each
 * such piece is reckoned again with the village's delta, and as vanilla reckoned it taken away: the rest of vanilla's shaping is as it was.
 * Not the grounds' own pieces (their boxes lie at the world's floor).
 */
public final class SectBeard {
	private SectBeard() {}

	/** How near a chunk a piece's box must come to shape its land (vanilla's reach, Beardifier#forStructuresInChunk). */
	private static final int REACH = 12;

	/** Vanilla's Beardifier for {@code pos}, with the sects' and cities' buildings' land drawn up a block more, or vanilla's when there are none near. */
	public static Beardifier raised(StructureManager structures, ChunkPos pos, Beardifier vanilla) {
		ObjectList<Beardifier.Rigid> raised = new ObjectArrayList<>(), asVanilla = new ObjectArrayList<>();
		for (var start : structures.startsForStructure(pos, structure -> structure.terrainAdaptation() == TerrainAdjustment.BEARD_THIN)) {
			for (StructurePiece piece : start.getPieces()) {
				if (!(piece instanceof SectPiece sect) || grounds(sect.kind()) || !piece.isCloseToChunk(pos, REACH)) continue;
				raised.add(new Beardifier.Rigid(piece.getBoundingBox(), TerrainAdjustment.BEARD_THIN, 1));
				asVanilla.add(new Beardifier.Rigid(piece.getBoundingBox(), TerrainAdjustment.BEARD_THIN, 0));
			}
		}
		return raised.isEmpty() ? vanilla : new Raised(vanilla, raised, asVanilla);
	}

	/** The grounds' pieces, laid on the land as it lies, their boxes at the world's floor. */
	private static boolean grounds(SectPiece.Kind kind) {
		return kind == SectPiece.Kind.TERRACE || kind == SectPiece.Kind.CITY_GROUNDS;
	}

	/** Vanilla's shaping, the buildings' as vanilla had them taken away, theirs drawn a block higher put in. */
	private static final class Raised extends Beardifier {
		private final Beardifier vanilla, asVanilla;

		Raised(Beardifier vanilla, ObjectList<Beardifier.Rigid> raised, ObjectList<Beardifier.Rigid> asVanilla) {
			super(raised.iterator(), new ObjectArrayList<JigsawJunction>().iterator());
			this.vanilla = vanilla;
			this.asVanilla = new Beardifier(asVanilla.iterator(), new ObjectArrayList<JigsawJunction>().iterator()) {};
		}

		@Override
		public double compute(DensityFunction.FunctionContext context) {
			return vanilla.compute(context) - asVanilla.compute(context) + super.compute(context);
		}
	}
}
