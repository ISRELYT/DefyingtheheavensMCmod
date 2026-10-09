package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ModPackets {
	/** C2S: toggle meditation. */
	public static final ResourceLocation TOGGLE_MEDITATION = new ResourceLocation(DefyingTheHeavens.MOD_ID, "toggle_meditation");
	/** C2S: attempt a realm breakthrough. */
	public static final ResourceLocation BREAKTHROUGH = new ResourceLocation(DefyingTheHeavens.MOD_ID, "breakthrough");
	/** S2C: full cultivation state for the owning player. */
	public static final ResourceLocation SYNC = new ResourceLocation(DefyingTheHeavens.MOD_ID, "sync");
	/** S2C: "player X is/isn't meditating", for rendering the lotus pose. */
	public static final ResourceLocation MEDITATION_STATE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "meditation_state");
	/** S2C: tribulation started/ended + strikes still to come, for the HUD overlay. */
	public static final ResourceLocation TRIBULATION_STATE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "tribulation_state");

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(TOGGLE_MEDITATION,
				(server, player, handler, buf, responseSender) -> server.execute(() -> MeditationManager.toggle(player)));
		ServerPlayNetworking.registerGlobalReceiver(BREAKTHROUGH,
				(server, player, handler, buf, responseSender) -> server.execute(() -> CultivationManager.tryBreakthrough(player)));
	}

	public static void sendSync(ServerPlayer player, PlayerCultivation c, boolean meditating) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(c.getRealm().ordinal());
		buf.writeVarInt(c.getStage().ordinal());
		buf.writeDouble(c.getCultivation());
		buf.writeDouble(c.getQi());
		buf.writeBoolean(meditating);
		buf.writeBoolean(c.isLowerRealmBound());
		buf.writeBoolean(c.isInUpperRealm());
		ServerPlayNetworking.send(player, SYNC, buf);
	}

	/** Tells the player and everyone tracking them. */
	public static void broadcastMeditation(ServerPlayer player, boolean meditating) {
		ServerPlayNetworking.send(player, MEDITATION_STATE, meditationBuf(player, meditating));
		for (ServerPlayer other : PlayerLookup.tracking(player)) {
			ServerPlayNetworking.send(other, MEDITATION_STATE, meditationBuf(player, meditating));
		}
	}

	public static void sendTribulation(ServerPlayer player, boolean active, int strikesLeft, Realm targetRealm, Stage targetStage) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(active);
		buf.writeVarInt(strikesLeft);
		buf.writeVarInt(targetRealm.ordinal());
		buf.writeVarInt(targetStage.ordinal());
		ServerPlayNetworking.send(player, TRIBULATION_STATE, buf);
	}

	private static FriendlyByteBuf meditationBuf(ServerPlayer player, boolean meditating) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUUID(player.getUUID());
		buf.writeBoolean(meditating);
		return buf;
	}

	private ModPackets() {}
}
