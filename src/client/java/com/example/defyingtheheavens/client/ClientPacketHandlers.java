package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientPacketHandlers {
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SYNC, (client, handler, buf, sender) -> {
			// Read on the network thread, apply on the client thread.
			int realm = buf.readVarInt();
			int stage = buf.readVarInt();
			double cultivation = buf.readDouble();
			double qi = buf.readDouble();
			boolean meditating = buf.readBoolean();
			boolean lowerRealmBound = buf.readBoolean();
			boolean inUpperRealm = buf.readBoolean();
			int disabledAbilities = buf.readVarInt();
			int pressureStages = buf.readVarInt();
			double pressurePenalty = buf.readDouble();
			boolean mortal = buf.readBoolean();
			int mortalStage = buf.readVarInt();
			double tempering = buf.readDouble();
			int preparedRealm = buf.readVarInt();
			double preparedBonus = buf.readDouble();
			double qiBoost = buf.readDouble();
			double pillResistance = buf.readDouble();
			double medicinalQi = buf.readDouble();
			client.execute(() -> ClientCultivationData.update(realm, stage, cultivation, qi, meditating, lowerRealmBound, inUpperRealm,
					disabledAbilities, pressureStages, pressurePenalty, mortal, mortalStage, tempering, preparedRealm, preparedBonus, qiBoost,
					pillResistance, medicinalQi));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SURGE, (client, handler, buf, sender) -> {
			int game = buf.readVarInt();
			int window = buf.readVarInt();
			client.execute(() -> QiSurgeHud.start(game, window));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.INNER_FADE, (client, handler, buf, sender) -> {
			int ticks = buf.readVarInt();
			client.execute(() -> InnerRealmFade.begin(ticks));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.QI_ABSORPTION, (client, handler, buf, sender) -> {
			UUID id = buf.readUUID();
			float rate = buf.readFloat();
			client.execute(() -> QiSenseClientHandler.absorption(id, rate));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.CONSCIOUSNESS_DOMAIN, (client, handler, buf, sender) -> {
			int count = buf.readVarInt();
			List<ConsciousnessRenderer.Sensed> cultivators = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				cultivators.add(new ConsciousnessRenderer.Sensed(buf.readVarInt(), Realm.byIndex(buf.readVarInt()), Stage.byIndex(buf.readVarInt()),
						buf.readBoolean()));
			}
			int domainCount = buf.readVarInt();
			List<ConsciousnessRenderer.Domain> domains = new ArrayList<>(domainCount);
			for (int i = 0; i < domainCount; i++) {
				domains.add(new ConsciousnessRenderer.Domain(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat()));
			}
			client.execute(() -> ConsciousnessRenderer.update(cultivators, domains));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.CONSCIOUSNESS_ALERT, (client, handler, buf, sender) -> {
			int entityId = buf.readVarInt();
			String name = buf.readUtf();
			Realm realm = Realm.byIndex(buf.readVarInt());
			Stage stage = Stage.byIndex(buf.readVarInt());
			client.execute(() -> ConsciousnessRenderer.alert(entityId, name, realm, stage));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.PRESSED_ENTITIES, (client, handler, buf, sender) -> {
			int count = buf.readVarInt();
			List<SuppressionClient.Pressed> pressed = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				pressed.add(new SuppressionClient.Pressed(buf.readVarInt(), buf.readFloat(), buf.readBoolean()));
			}
			client.execute(() -> SuppressionClient.update(pressed));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.MEDITATION_STATE, (client, handler, buf, sender) -> {
			UUID id = buf.readUUID();
			boolean meditating = buf.readBoolean();
			client.execute(() -> ClientMeditationTracker.set(id, meditating));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.TRIBULATION_STATE, (client, handler, buf, sender) -> {
			boolean active = buf.readBoolean();
			int strikesLeft = buf.readVarInt();
			int realm = buf.readVarInt();
			int stage = buf.readVarInt();
			client.execute(() -> ClientTribulationData.update(active, strikesLeft, realm, stage));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SPATIAL_STORMS, (client, handler, buf, sender) -> {
			int loops = buf.readInt();
			double playerY = buf.readDouble();
			int count = buf.readVarInt();
			List<ClientSpatialStorms.StormState> storms = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				storms.add(new ClientSpatialStorms.StormState(buf.readVarInt(), buf.readInt(), buf.readDouble(), buf.readDouble(),
						buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt()));
			}
			client.execute(() -> ClientSpatialStorms.update(loops, playerY, storms));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SPATIAL_STORM_STRIKE, (client, handler, buf, sender) -> {
			int storm = buf.readVarInt();
			double x1 = buf.readDouble();
			double y1 = buf.readDouble();
			double z1 = buf.readDouble();
			double x2 = buf.readDouble();
			double y2 = buf.readDouble();
			double z2 = buf.readDouble();
			long seed = buf.readLong();
			boolean hit = buf.readBoolean();
			client.execute(() -> ClientSpatialStorms.strike(storm, x1, y1, z1, x2, y2, z2, seed, hit));
		});
	}

	private ClientPacketHandlers() {}
}
