package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ModPackets {
	/** C2S: toggle meditation. */
	public static final ResourceLocation TOGGLE_MEDITATION = new ResourceLocation(DefyingTheHeavens.MOD_ID, "toggle_meditation");
	/** C2S: attempt a realm breakthrough. */
	public static final ResourceLocation BREAKTHROUGH = new ResourceLocation(DefyingTheHeavens.MOD_ID, "breakthrough");
	/** C2S: switch an ability on or off (its ordinal). */
	public static final ResourceLocation TOGGLE_ABILITY = new ResourceLocation(DefyingTheHeavens.MOD_ID, "toggle_ability");
	/** S2C: full cultivation state for the owning player. */
	public static final ResourceLocation SYNC = new ResourceLocation(DefyingTheHeavens.MOD_ID, "sync");
	/** S2C: "player X is/isn't meditating", for rendering the lotus pose. */
	public static final ResourceLocation MEDITATION_STATE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "meditation_state");
	/** S2C: tribulation started/ended + strikes still to come, for the HUD overlay. */
	public static final ResourceLocation TRIBULATION_STATE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "tribulation_state");
	/** S2C: every spatial storm in the gap, once a second, to everyone in the gap (see {@link SpatialStorms}). */
	public static final ResourceLocation SPATIAL_STORMS = new ResourceLocation(DefyingTheHeavens.MOD_ID, "spatial_storms");
	/** S2C: one spatial storm bolt, to everyone in the gap within range. */
	public static final ResourceLocation SPATIAL_STORM_STRIKE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "spatial_storm_strike");
	/** S2C: "player X is drawing in qi at this rate", with the meditation heartbeat, to Qi Sense users (see {@link QiSense}). */
	public static final ResourceLocation QI_ABSORPTION = new ResourceLocation(DefyingTheHeavens.MOD_ID, "qi_absorption");
	/** S2C: the cultivators inside the user's domain and the domains touching it (see {@link ConsciousnessDomainHandler}). */
	public static final ResourceLocation CONSCIOUSNESS_DOMAIN = new ResourceLocation(DefyingTheHeavens.MOD_ID, "consciousness_domain");
	/** S2C: another cultivator's domain has just touched the user's. */
	public static final ResourceLocation CONSCIOUSNESS_ALERT = new ResourceLocation(DefyingTheHeavens.MOD_ID, "consciousness_alert");
	/** S2C: entities near the player under Realm Suppress, for drawing the pressure (see {@link RealmSuppressSystem}). */
	public static final ResourceLocation PRESSED_ENTITIES = new ResourceLocation(DefyingTheHeavens.MOD_ID, "pressed_entities");

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(TOGGLE_MEDITATION,
				(server, player, handler, buf, responseSender) -> server.execute(() -> MeditationManager.toggle(player)));
		ServerPlayNetworking.registerGlobalReceiver(BREAKTHROUGH,
				(server, player, handler, buf, responseSender) -> server.execute(() -> CultivationManager.tryBreakthrough(player)));
		ServerPlayNetworking.registerGlobalReceiver(TOGGLE_ABILITY, (server, player, handler, buf, responseSender) -> {
			int ability = buf.readVarInt();
			server.execute(() -> CultivationManager.toggleAbility(player, ability));
		});
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
		buf.writeVarInt(c.getDisabledAbilityMask());
		buf.writeVarInt(c.getPressureStages());
		buf.writeDouble(c.getPressurePenalty());
		buf.writeBoolean(c.isMortal());
		buf.writeVarInt(c.getPreparedRealm() == null ? -1 : c.getPreparedRealm().ordinal());
		buf.writeDouble(c.getPreparedBonus());
		buf.writeDouble(c.getQiBoost());
		buf.writeDouble(c.getPillResistance());
		ServerPlayNetworking.send(player, SYNC, buf);
	}

	public static void sendQiAbsorption(ServerPlayer to, ServerPlayer meditator, double cultivationPerSecond) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUUID(meditator.getUUID());
		buf.writeFloat((float) cultivationPerSecond);
		ServerPlayNetworking.send(to, QI_ABSORPTION, buf);
	}

	public static void sendConsciousness(ServerPlayer to, List<ConsciousnessDomainHandler.SensedCultivator> cultivators,
										 List<ConsciousnessDomainHandler.TouchingDomain> domains) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(cultivators.size());
		for (ConsciousnessDomainHandler.SensedCultivator sensed : cultivators) {
			buf.writeVarInt(sensed.entityId());
			buf.writeVarInt(sensed.realm().ordinal());
			buf.writeVarInt(sensed.stage().ordinal());
			buf.writeBoolean(sensed.pressed());
		}
		buf.writeVarInt(domains.size());
		for (ConsciousnessDomainHandler.TouchingDomain domain : domains) {
			buf.writeVarInt(domain.entityId());
			buf.writeDouble(domain.x());
			buf.writeDouble(domain.y());
			buf.writeDouble(domain.z());
			buf.writeFloat(domain.radius());
		}
		ServerPlayNetworking.send(to, CONSCIOUSNESS_DOMAIN, buf);
	}

	/** Tells {@code to} that {@code other}'s domain touches theirs: who, and the realm they show. */
	public static void sendConsciousnessAlert(ServerPlayer to, ServerPlayer other, PlayerCultivation otherCultivation) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(other.getId());
		buf.writeUtf(other.getGameProfile().getName());
		buf.writeVarInt(otherCultivation.getEffectiveRealm().ordinal());
		buf.writeVarInt(otherCultivation.getEffectiveStage().ordinal());
		ServerPlayNetworking.send(to, CONSCIOUSNESS_ALERT, buf);
	}

	public static void sendPressedEntities(ServerPlayer to, List<RealmSuppressSystem.PressedVisual> pressed) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(pressed.size());
		for (RealmSuppressSystem.PressedVisual visual : pressed) {
			buf.writeVarInt(visual.entityId());
			buf.writeFloat(visual.penalty());
			buf.writeBoolean(visual.heavy());
		}
		ServerPlayNetworking.send(to, PRESSED_ENTITIES, buf);
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
