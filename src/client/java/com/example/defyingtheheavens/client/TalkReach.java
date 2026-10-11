package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.TalkSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Talking from a few steps off: a right-click on a cultivator up to {@link TalkSystem#REACH} blocks away starts a talk (C2S
 * talk_request), not just one within arm's length, where vanilla's own interaction already reaches it. Called from
 * MinecraftMixin before a right-click is handled; true when it was a talk (the click goes no further).
 */
public final class TalkReach {
	public static boolean tryTalk(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.gameMode == null || mc.player.isSpectator() || mc.player.isPassenger()) return false;
		if (ClientSoul.isOut()) return false; // a projected soul's right-click is for possessing
		HitResult hit = mc.hitResult;
		if (hit != null && hit.getType() == HitResult.Type.ENTITY) return false; // within arm's length: vanilla's interaction
		Vec3 eye = mc.player.getEyePosition();
		Vec3 look = mc.player.getViewVector(1.0f);
		Vec3 end = eye.add(look.scale(TalkSystem.REACH));
		AABB box = mc.player.getBoundingBox().expandTowards(look.scale(TalkSystem.REACH)).inflate(1.0);
		EntityHitResult found = ProjectileUtil.getEntityHitResult(mc.player, eye, end, box,
				entity -> entity instanceof CultivatorNpc && entity.isAlive() && !entity.isSpectator(), TalkSystem.REACH * TalkSystem.REACH);
		if (found == null) return false;
		// Something solid nearer than it (a wall, the block being looked at) takes the click instead.
		if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceToSqr(eye) < found.getLocation().distanceToSqr(eye)) return false;
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(found.getEntity().getId());
		ClientPlayNetworking.send(ModPackets.TALK_REQUEST, buf);
		mc.player.swing(InteractionHand.MAIN_HAND);
		return true;
	}

	private TalkReach() {}
}
