package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.RingSlot;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	@Shadow public ServerPlayer player;

	/**
	 * The creative inventory syncs edits slot by slot, and vanilla only accepts menu slots 1-45 (up to the offhand).
	 * Without this, rings put on or taken off in creative mode would only change on the client.
	 * <p>
	 * MixinExtras' @ModifyExpressionValue rather than @ModifyConstant: other mods that add inventory slots (Trinkets)
	 * raise the same limit, and only one @ModifyConstant may claim a constant, while these chain. The limit becomes the
	 * last ring slot's actual index, so it stays right whatever order the mods' slots were added in.
	 */
	@ModifyExpressionValue(method = "handleSetCreativeModeSlot", at = @At(value = "CONSTANT", args = "intValue=45"))
	private int dth$acceptRingSlots(int lastSlot) {
		int last = lastSlot;
		for (Slot slot : player.inventoryMenu.slots) {
			if (slot instanceof RingSlot) last = Math.max(last, slot.index);
		}
		return last;
	}
}
