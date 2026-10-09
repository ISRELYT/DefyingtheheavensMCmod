package com.example.defyingtheheavens.compat;

import com.example.defyingtheheavens.CultivationFruitBlock;
import com.example.defyingtheheavens.CultivationFruitBlockEntity;
import com.example.defyingtheheavens.DefyingTheHeavens;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade integration (only loaded when Jade is installed, through the "jade" entrypoint in fabric.mod.json): looking at
 * a Cultivation Fruit on a tree shows its age. The client already knows the age (the fruit's block entity syncs its
 * clock), so no server-side data provider is needed.
 */
@WailaPlugin(DefyingTheHeavens.MOD_ID)
public class JadePlugin implements IWailaPlugin {
	public static final ResourceLocation FRUIT_AGE = DefyingTheHeavens.id("fruit_age");

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(FruitAge.INSTANCE, CultivationFruitBlock.class);
	}

	private enum FruitAge implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			if (accessor.getBlockEntity() instanceof CultivationFruitBlockEntity fruit) {
				tooltip.add(Component.translatable("item.defying-the-heavens.cultivation_fruit.age", fruit.age()).withStyle(ChatFormatting.GOLD));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return FRUIT_AGE;
		}
	}
}
