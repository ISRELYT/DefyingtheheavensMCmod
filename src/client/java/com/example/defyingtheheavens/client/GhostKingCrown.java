package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * A Ghost King's crown (GhostSystem#isKing; a palace's king, GhostCultivatorNpc): a band of bone round the brow, a little
 * wider than the head, and points of bone rising from it, the tallest over the brow, soul fire burning on the tallest
 * ({@link #FLAMES}). Worn on the head, turning and nodding with it (copy the head's pose onto {@link #part}), and drawn in its
 * own colours over the dead's light (SoulRenderer#drawCrown).
 * <p>
 * Units are the model's pixels, about the neck, as the human head's (the box from (-4, -8, -4) to (4, 0, 4), its face toward
 * -z). Texture {@code textures/entity/ghost/king_crown.png}, 32 x 32: the band's front 0,0 and back 0,3 (9 x 2 x 1), its sides
 * 0,6 and 16,6 (1 x 2 x 7), the points 20,0 (1 wide and deep, up to 4 tall).
 */
public final class GhostKingCrown {
	/** How far the crown and its flames rise over a man's head, in blocks at a man's size (a king's plate goes above it). */
	public static final float HEIGHT = 0.5f;
	static final ResourceLocation TEXTURE = DefyingTheHeavens.id("textures/entity/ghost/king_crown.png");
	/** The tips of the points that burn (x, y, z in the head's pixels): over the brow, at the front corners, at the sides. */
	static final float[][] FLAMES = {{0.0f, -14.0f, -4.0f}, {-4.0f, -13.0f, -4.0f}, {4.0f, -13.0f, -4.0f}, {-4.0f, -13.0f, 0.0f},
			{4.0f, -13.0f, 0.0f}};
	private static final ModelLayerLocation LAYER = new ModelLayerLocation(DefyingTheHeavens.id("ghost_king_crown"), "main");
	private static ModelPart crown;

	public static void register() {
		EntityModelLayerRegistry.registerModelLayer(LAYER, GhostKingCrown::definition);
	}

	/** The crown, baked the first time it is asked for: pose it as the head it sits on, then render it. */
	static ModelPart part() {
		if (crown == null) crown = Minecraft.getInstance().getEntityModels().bakeLayer(LAYER).getChild("crown");
		return crown;
	}

	static LayerDefinition definition() {
		MeshDefinition mesh = new MeshDefinition();
		CubeListBuilder cubes = CubeListBuilder.create()
				// The band, half a pixel out from the head all round, its top two pixels over the head's.
				.texOffs(0, 0).addBox(-4.5f, -10.0f, -4.5f, 9, 2, 1)
				.texOffs(0, 3).addBox(-4.5f, -10.0f, 3.5f, 9, 2, 1)
				.texOffs(0, 6).addBox(-4.5f, -10.0f, -3.5f, 1, 2, 7)
				.texOffs(16, 6).addBox(3.5f, -10.0f, -3.5f, 1, 2, 7)
				// The points: the tallest over the brow, then the front's corners and the sides', smaller ones between and behind.
				.texOffs(20, 0).addBox(-0.5f, -14.0f, -4.5f, 1, 4, 1)
				.texOffs(20, 0).addBox(-4.5f, -13.0f, -4.5f, 1, 3, 1)
				.texOffs(20, 0).addBox(3.5f, -13.0f, -4.5f, 1, 3, 1)
				.texOffs(20, 0).addBox(-2.5f, -12.0f, -4.5f, 1, 2, 1)
				.texOffs(20, 0).addBox(1.5f, -12.0f, -4.5f, 1, 2, 1)
				.texOffs(20, 0).addBox(-4.5f, -13.0f, -0.5f, 1, 3, 1)
				.texOffs(20, 0).addBox(3.5f, -13.0f, -0.5f, 1, 3, 1)
				.texOffs(20, 0).addBox(-4.5f, -12.0f, 3.5f, 1, 2, 1)
				.texOffs(20, 0).addBox(3.5f, -12.0f, 3.5f, 1, 2, 1)
				.texOffs(20, 0).addBox(-0.5f, -12.0f, 3.5f, 1, 2, 1);
		mesh.getRoot().addOrReplaceChild("crown", cubes, PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32);
	}

	private GhostKingCrown() {}
}
