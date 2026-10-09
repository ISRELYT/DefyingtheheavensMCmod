package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.RingContainer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	/**
	 * The creative inventory syncs edits slot by slot, and vanilla only accepts menu slots 1-45 (up to the offhand).
	 * Without this, rings put on or taken off in creative mode would only change on the client.
	 */
	@ModifyConstant(method = "handleSetCreativeModeSlot", constant = @Constant(intValue = 45))
	private int dth$acceptRingSlots(int lastSlot) {
		return RingContainer.FIRST_MENU_SLOT + RingContainer.SIZE - 1;
	}
}
