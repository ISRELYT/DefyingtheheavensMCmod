package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.InnerBodyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** A meditator's body, drawn as them (their own skin) sitting cross-legged (see HumanoidModelMixin). */
public class InnerBodyRenderer extends LivingEntityRenderer<InnerBodyEntity, PlayerModel<InnerBodyEntity>> {
	public InnerBodyRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(InnerBodyEntity body) {
		return skinOf(body.getOwnerId());
	}

	/** A player's skin, from the tab list when they're known to this client, else their default skin. */
	static ResourceLocation skinOf(UUID id) {
		if (id == null) return DefaultPlayerSkin.getDefaultSkin();
		Minecraft mc = Minecraft.getInstance();
		PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
		return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
	}

	@Override
	protected boolean shouldShowName(InnerBodyEntity body) {
		return false;
	}
}
