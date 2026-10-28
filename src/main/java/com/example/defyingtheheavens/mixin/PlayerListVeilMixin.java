package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Concealments;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A sound or a block's event from within a concealment reaches only those it doesn't hide from (Concealments#broadcast). */
@Mixin(PlayerList.class)
public abstract class PlayerListVeilMixin {
	@Shadow @Final private MinecraftServer server;

	@Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
	private void dth$veiledSounds(Player except, double x, double y, double z, double radius, ResourceKey<Level> dimension, Packet<?> packet,
			CallbackInfo ci) {
		if (Concealments.broadcast(server, except, x, y, z, radius, dimension, packet)) ci.cancel();
	}
}
