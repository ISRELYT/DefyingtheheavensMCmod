package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ModPackets {
	/** C2S: toggle meditation. */
	public static final ResourceLocation TOGGLE_MEDITATION = new ResourceLocation(DefyingTheHeavens.MOD_ID, "toggle_meditation");
	/** C2S: attempt a realm breakthrough. */
	public static final ResourceLocation BREAKTHROUGH = new ResourceLocation(DefyingTheHeavens.MOD_ID, "breakthrough");
	/** C2S: the outcome of the qi circulation game (see MeditationManager#onCirculation): 0 clean circuit, 1 broken, 2 deviation. */
	public static final ResourceLocation CIRCULATION = new ResourceLocation(DefyingTheHeavens.MOD_ID, "circulation");
	/** C2S: switch an ability on or off (its ordinal). */
	public static final ResourceLocation TOGGLE_ABILITY = new ResourceLocation(DefyingTheHeavens.MOD_ID, "toggle_ability");
	/** S2C: a qi surge's screen game starts (game ordinal, window in ticks; -1 ends any running), see QiSurges. */
	public static final ResourceLocation SURGE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "surge");
	/** S2C: the soul is about to turn inward: fade the screen to black (see InnerRealm#FADE_TICKS). */
	public static final ResourceLocation INNER_FADE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "inner_fade");
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
	/** S2C: the raised formations near the player (see {@link Formations}): drawn under Qi Sense, and walked through by their owner. */
	public static final ResourceLocation FORMATIONS = new ResourceLocation(DefyingTheHeavens.MOD_ID, "formations");
	/** S2C: a Formation Core's state, opening (or refreshing) its screen. */
	public static final ResourceLocation FORMATION_CORE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "formation_core");
	/** C2S: the open core screen asks for a fresh state (its position). */
	public static final ResourceLocation FORMATION_CORE_QUERY = new ResourceLocation(DefyingTheHeavens.MOD_ID, "formation_core_query");
	/** C2S: the owner set a core's radius and switch (position, radius, on). */
	public static final ResourceLocation FORMATION_CORE_CONFIGURE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "formation_core_configure");
	/** C2S: the owner trusts a player by name (position, true, name) or stops trusting one (position, false, UUID). */
	public static final ResourceLocation FORMATION_CORE_TRUST = new ResourceLocation(DefyingTheHeavens.MOD_ID, "formation_core_trust");
	/** Players further than this from a core can't work its screen. */
	private static final double CORE_REACH = 8;

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(FORMATION_CORE_QUERY, (server, player, handler, buf, responseSender) -> {
			BlockPos pos = buf.readBlockPos();
			server.execute(() -> {
				if (player.distanceToSqr(Vec3.atCenterOf(pos)) <= CORE_REACH * CORE_REACH && player.level().isLoaded(pos)
						&& player.level().getBlockEntity(pos) instanceof FormationCoreBlockEntity core && core.canControl(player)) {
					sendFormationCore(player, core);
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(FORMATION_CORE_CONFIGURE, (server, player, handler, buf, responseSender) -> {
			BlockPos pos = buf.readBlockPos();
			int radius = buf.readVarInt();
			boolean on = buf.readBoolean();
			server.execute(() -> {
				if (player.distanceToSqr(Vec3.atCenterOf(pos)) <= CORE_REACH * CORE_REACH && player.level().isLoaded(pos)
						&& player.level().getBlockEntity(pos) instanceof FormationCoreBlockEntity core) {
					core.configure(player, radius, on);
					sendFormationCore(player, core);
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(FORMATION_CORE_TRUST, (server, player, handler, buf, responseSender) -> {
			BlockPos pos = buf.readBlockPos();
			boolean add = buf.readBoolean();
			String name = add ? buf.readUtf(16) : null;
			UUID id = add ? null : buf.readUUID();
			server.execute(() -> {
				if (player.distanceToSqr(Vec3.atCenterOf(pos)) <= CORE_REACH * CORE_REACH && player.level().isLoaded(pos)
						&& player.level().getBlockEntity(pos) instanceof FormationCoreBlockEntity core) {
					if (add) core.trustByName(player, name);
					else core.distrust(player, id);
					sendFormationCore(player, core);
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(TOGGLE_MEDITATION,
				(server, player, handler, buf, responseSender) -> server.execute(() -> MeditationManager.toggle(player)));
		ServerPlayNetworking.registerGlobalReceiver(BREAKTHROUGH,
				(server, player, handler, buf, responseSender) -> server.execute(() -> CultivationManager.tryBreakthrough(player)));
		ServerPlayNetworking.registerGlobalReceiver(CIRCULATION, (server, player, handler, buf, responseSender) -> {
			int outcome = buf.readVarInt();
			server.execute(() -> MeditationManager.onCirculation(player, outcome));
		});
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
		buf.writeVarInt(c.getMortalStage().ordinal());
		buf.writeDouble(c.getTempering());
		buf.writeVarInt(c.getPreparedRealm() == null ? -1 : c.getPreparedRealm().ordinal());
		buf.writeDouble(c.getPreparedBonus());
		buf.writeDouble(c.getQiBoost());
		buf.writeDouble(c.getPillResistance());
		buf.writeDouble(c.getMedicinalQi());
		buf.writeInt(c.getAlignment());
		buf.writeDouble(c.getGearQiBonus());
		ServerPlayNetworking.send(player, SYNC, buf);
	}

	public static void sendSurge(ServerPlayer player, int game, int windowTicks) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(game);
		buf.writeVarInt(windowTicks);
		ServerPlayNetworking.send(player, SURGE, buf);
	}

	public static void sendInnerFade(ServerPlayer player) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(InnerRealm.FADE_TICKS);
		ServerPlayNetworking.send(player, INNER_FADE, buf);
	}

	public static void sendFormations(ServerPlayer to, List<Formation> formations) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(formations.size());
		for (Formation formation : formations) {
			buf.writeUUID(formation.id);
			buf.writeEnum(formation.kind);
			buf.writeBlockPos(formation.center);
			buf.writeVarInt(formation.getRadius());
			buf.writeInt(formation.getRank());
			buf.writeBoolean(formation.getOwner() != null);
			if (formation.getOwner() != null) buf.writeUUID(formation.getOwner());
			buf.writeVarInt(formation.getTrusted().size());
			for (UUID player : formation.getTrusted()) buf.writeUUID(player);
		}
		ServerPlayNetworking.send(to, FORMATIONS, buf);
	}

	public static void sendFormationCore(ServerPlayer to, FormationCoreBlockEntity core) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(core.getBlockPos());
		buf.writeVarInt(core.getRadius());
		buf.writeBoolean(core.isWanted());
		buf.writeBoolean(core.isRaised());
		buf.writeDouble(core.getBattery());
		buf.writeDouble(FormationCoreBlockEntity.BATTERY);
		buf.writeDouble(core.supply());
		buf.writeVarInt(core.getVeins());
		buf.writeInt(core.getRank());
		buf.writeVarInt(core.cooldownSeconds());
		buf.writeUtf(core.getOwnerName());
		buf.writeVarInt(core.getTrusted().size());
		for (Map.Entry<UUID, String> player : core.getTrusted().entrySet()) {
			buf.writeUUID(player.getKey());
			buf.writeUtf(player.getValue());
		}
		ServerPlayNetworking.send(to, FORMATION_CORE, buf);
	}

	/** Tells {@code to} that an NPC's domain touches theirs: what it is called, and the realm it shows. */
	public static void sendConsciousnessAlert(ServerPlayer to, int entityId, String name, Realm realm, Stage stage) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entityId);
		buf.writeUtf(name);
		buf.writeVarInt(realm.ordinal());
		buf.writeVarInt(stage.ordinal());
		ServerPlayNetworking.send(to, CONSCIOUSNESS_ALERT, buf);
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
