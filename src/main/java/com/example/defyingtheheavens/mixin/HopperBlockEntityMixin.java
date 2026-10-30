package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.LegendaryWeaponItem;
import com.example.defyingtheheavens.SectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
	/**
	 * A hopper (or a hopper minecart: both ask here) draws nothing out of a sect's treasury or a ghost palace's (SectManager#treasuryAt):
	 * their rules are for hands, and a hopper set under a chest would empty it with no alarm. Putting things in is left as it was.
	 * What it finds above is an empty container, not none: with none, vanilla takes up what lies loose there instead, and Fabric's
	 * transfer API looks the chest up by its own way and draws from it all the same.
	 */
	@Inject(method = "getSourceContainer", at = @At("RETURN"), cancellable = true)
	private static void dth$noDrawingFromTreasuries(Level level, Hopper hopper, CallbackInfoReturnable<Container> cir) {
		if (cir.getReturnValue() == null || !(level instanceof ServerLevel server)) return;
		BlockPos above = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
		if (SectManager.treasuryAt(server, above)) cir.setReturnValue(new SimpleContainer(0));
	}

	/**
	 * A hopper (or a hopper minecart) taking up a legend lying loose: it takes a copy and throws the lying stack away whole, which Legends
	 * would count the legend's loss (to reform at home a day on, the hopper's one a hollow echo then). The lying stack emptied first, as a
	 * player's picking it up leaves it.
	 */
	@Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
	private static void dth$legendTakenUpNotLost(Container container, ItemEntity item, CallbackInfoReturnable<Boolean> cir) {
		if (item.getItem().getItem() instanceof LegendaryWeaponItem) item.getItem().setCount(0);
	}
}
