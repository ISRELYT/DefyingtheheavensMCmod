package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.GhostSystem;
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

	/**
	 * In hardcore, vanilla makes a respawned player a spectator. The dead go to the Netherworld for good instead (GhostSystem: the
	 * Wheel won't take them back): a player who respawned a ghost stays one, in their own game mode.
	 */
	@ModifyExpressionValue(method = "handleClientCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isHardcore()Z"))
	private boolean dth$theDeadAreNoSpectators(boolean hardcore) {
		return hardcore && !GhostSystem.isGhost(player);
	}

	/**
	 * Vanilla snaps back anyone but the game's host who moves more than 10 blocks in one movement packet (the squared limit, 100); from
	 * Void Shattering on one flies far faster than that (QiFlight#realmSpeed, AstralRealm#spaceSpeed). A flier may move as far as its
	 * flight carries it, several ticks of it for packets that come in bunched (QiFlight#moveAllowanceSq).
	 */
	@ModifyExpressionValue(method = "handleMovePlayer", at = @At(value = "CONSTANT", args = "floatValue=100.0"))
	private float dth$roomForTheSwift(float limit) {
		return com.example.defyingtheheavens.QiFlight.moveAllowanceSq(player, limit);
	}

	/**
	 * A player leaving: the mod's work on it (CultivationEvents#leaving) here, on the server thread, before vanilla saves and removes
	 * them. Fabric's DISCONNECT may come first on the network thread (the connection closed), and vanilla's save would race it.
	 */
	@org.spongepowered.asm.mixin.injection.Inject(method = "onDisconnect", at = @At("HEAD"))
	private void dth$leaving(net.minecraft.network.chat.Component reason, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		com.example.defyingtheheavens.CultivationEvents.leaving(player);
	}

	/** One whose body is held by another's soul (Possession) is a spectator only to watch it: no teleporting off to anyone else. */
	@org.spongepowered.asm.mixin.injection.Inject(method = "handleTeleportToEntityPacket", cancellable = true, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
			shift = At.Shift.AFTER))
	private void dth$heldStayWithTheirBody(net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket packet,
										   org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		if (com.example.defyingtheheavens.Possession.isHeld(player.getUUID())) ci.cancel();
	}
}
