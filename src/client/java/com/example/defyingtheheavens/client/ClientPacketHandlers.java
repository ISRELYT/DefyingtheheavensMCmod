package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Alignment;
import com.example.defyingtheheavens.Formation;
import com.example.defyingtheheavens.Gesture;
import com.example.defyingtheheavens.Journal;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.NpcTitle;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.SoulProjection;
import com.example.defyingtheheavens.SoulRiver;
import com.example.defyingtheheavens.Stage;
import com.example.defyingtheheavens.TalkIntent;
import com.example.defyingtheheavens.TwoPaths;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientPacketHandlers {
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.NETHERWORLD_SYNC, (client, handler, buf, sender) -> {
			double ghostQi = buf.readDouble();
			double ghostQiMax = buf.readDouble();
			double density = buf.readDouble();
			float corruption = buf.readFloat();
			int trend = buf.readByte();
			int held = buf.readVarInt();
			client.execute(() -> ClientNetherworldData.set(ghostQi, ghostQiMax, density, corruption, trend, held));
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.METHODS, (client, handler, buf, sender) -> client.execute(ClientMethods.read(buf)));
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.GHOST, (client, handler, buf, sender) -> {
			int entity = buf.readVarInt();
			boolean ghost = buf.readBoolean();
			boolean king = buf.readBoolean();
			int look = buf.readByte();
			TwoPaths.Look drawn = look >= 0 && look < TwoPaths.Look.values().length ? TwoPaths.Look.values()[look] : TwoPaths.Look.HUMAN;
			client.execute(() -> ClientGhosts.set(entity, ghost, king, drawn));
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.GHOST_FADE, (client, handler, buf, sender) -> {
			int rgb = buf.readInt();
			int close = buf.readVarInt();
			int hold = buf.readVarInt();
			int open = buf.readVarInt();
			client.execute(() -> ClientGhosts.fade(rgb, close, hold, open));
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.QUELLED, (client, handler, buf, sender) -> {
			int entity = buf.readVarInt();
			int ticks = buf.readVarInt();
			client.execute(() -> {
				net.minecraft.world.entity.Entity bound = client.level == null ? null : client.level.getEntity(entity);
				if (bound != null) ClientQuelled.set(bound, ticks);
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.JUDGE_GAZE, (client, handler, buf, sender) -> {
			float gaze = buf.readFloat();
			client.execute(() -> ClientJudgeGaze.set(gaze));
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SOUL_RIVER, (client, handler, buf, sender) -> {
			int route = buf.readVarInt();
			int tick = buf.readVarInt();
			SoulRiver.Route[] routes = SoulRiver.Route.values();
			client.execute(() -> ClientSoulRiver.set(route >= 0 && route < routes.length ? routes[route] : SoulRiver.Route.NONE, tick));
		});
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
			int alignment = buf.readInt();
			double gearQiBonus = buf.readDouble();
			int[] wheel = new int[PlayerCultivation.WHEEL_SLOTS];
			for (int i = 0; i < wheel.length; i++) wheel[i] = buf.readVarInt() - 1;
			int soulState = buf.readVarInt();
			boolean heavensLeave = buf.readBoolean();
			int favour = buf.readVarInt();
			int physique = buf.readVarInt();
			int roots = buf.readVarInt();
			int bodyRefining = buf.readVarInt();
			long forms = buf.readVarLong();
			int[] statPoints = new int[com.example.defyingtheheavens.StatPoints.Stat.values().length];
			for (int i = 0; i < statPoints.length; i++) statPoints[i] = buf.readVarInt();
			com.example.defyingtheheavens.Ability selected = com.example.defyingtheheavens.Ability.byIndex(buf.readVarInt() - 1);
			double placeQi = buf.readDouble();
			float soulWounds = buf.readFloat();
			double lifeUsed = buf.readDouble(), lifeSpan = buf.readDouble();
			boolean lifeDead = buf.readBoolean();
			int longevityPills = buf.readVarInt();
			int[] axisProgress = new int[com.example.defyingtheheavens.FourAxis.Axis.values().length];
			for (int i = 0; i < axisProgress.length; i++) axisProgress[i] = buf.readVarInt();
			com.example.defyingtheheavens.FourAxis.Axis obsession = com.example.defyingtheheavens.FourAxis.Axis.byIndex(buf.readVarInt());
			int pace = buf.readVarInt();
			int techniques = buf.readVarInt();
			java.util.Set<com.example.defyingtheheavens.Technique> known = java.util.EnumSet.noneOf(com.example.defyingtheheavens.Technique.class);
			for (com.example.defyingtheheavens.Technique technique : com.example.defyingtheheavens.Technique.values()) {
				if ((techniques & (1 << technique.ordinal())) != 0) known.add(technique);
			}
			int manualCount = buf.readVarInt();
			java.util.Set<com.example.defyingtheheavens.Spell> manual = java.util.EnumSet.noneOf(com.example.defyingtheheavens.Spell.class);
			for (int i = 0; i < manualCount; i++) {
				com.example.defyingtheheavens.Spell spell = com.example.defyingtheheavens.Spell.byIndex(buf.readVarInt());
				if (spell != null) manual.add(spell);
			}
			boolean inClone = buf.readBoolean();
			client.execute(() -> {
				ClientCultivationData.get().setTechniques(known);
				ClientCultivationData.get().setManualSpells(manual);
				ClientCultivationData.setInClone(inClone);
				for (com.example.defyingtheheavens.FourAxis.Axis axis : com.example.defyingtheheavens.FourAxis.Axis.values()) {
					ClientCultivationData.get().setAxisProgress(axis, axisProgress[axis.ordinal()]);
				}
				ClientCultivationData.get().setObsession(obsession);
				ClientCultivationData.get().setMeditationSpeed(pace);
				ClientCultivationData.get().setSoulWounds(soulWounds);
				ClientCultivationData.lifespan(lifeUsed, lifeSpan, lifeDead);
				ClientCultivationData.get().setLongevityPills(longevityPills);
				ClientCultivationData.physique(physique, roots, bodyRefining, forms); // first: a shattered body tempers past Peak
				for (com.example.defyingtheheavens.StatPoints.Stat stat : com.example.defyingtheheavens.StatPoints.Stat.values()) {
					ClientCultivationData.get().setStatPoints(stat, statPoints[stat.ordinal()]);
				}
				ClientCultivationData.get().setSelectedAbility(selected);
				ClientCultivationData.get().setPlaceQi(placeQi);
				ClientCultivationData.update(realm, stage, cultivation, qi, meditating, lowerRealmBound, inUpperRealm,
						disabledAbilities, pressureStages, pressurePenalty, mortal, mortalStage, tempering, preparedRealm, preparedBonus, qiBoost,
						pillResistance, medicinalQi, alignment, gearQiBonus, wheel, soulState, heavensLeave, favour);
			});
		});

		// The Astral Realm's seed: the same stars and planets as the server's (AstralPlanets).
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.ASTRAL_SEED, (client, handler, buf, sender) -> {
			long seed = buf.readLong();
			client.execute(() -> com.example.defyingtheheavens.AstralPlanets.forSeed(seed));
		});

		// "Name Your Obsession" before the tribulation into Heavenly Being (Obsession).
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.OBSESSION_ASK, (client, handler, buf, sender) -> {
			boolean ghost = buf.readBoolean();
			client.execute(() -> client.setScreen(new ObsessionScreen(ghost)));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SOUL_SELF, (client, handler, buf, sender) -> {
			SoulProjection.State state = SoulProjection.State.byIndex(buf.readVarInt());
			int ticks = buf.readVarInt();
			Vec3 body = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
			int radius = buf.readVarInt();
			float health = buf.readFloat();
			float max = buf.readFloat();
			int host = buf.readVarInt() - 1;
			float tether = buf.readFloat();
			float ownQi = buf.readFloat();
			float ownMax = buf.readFloat();
			boolean ghostPool = buf.readBoolean();
			boolean bodyHere = buf.readBoolean();
			boolean soulTears = buf.readBoolean();
			client.execute(() -> ClientSoul.self(state, ticks, body, radius, health, max, host, tether, ownQi, ownMax, ghostPool, bodyHere, soulTears));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.BODY_LOOK, (client, handler, buf, sender) -> {
			int id = buf.readVarInt();
			int kind = buf.readByte();
			java.util.UUID player = null;
			net.minecraft.network.chat.Component name = null;
			String textures = null, signature = null;
			if (kind == 1) {
				player = buf.readUUID();
				name = buf.readComponent();
				if (buf.readBoolean()) {
					textures = buf.readUtf();
					signature = buf.readNullable(net.minecraft.network.FriendlyByteBuf::readUtf);
				}
			}
			int npc = kind == 2 ? buf.readVarInt() : -1;
			java.util.UUID who = player;
			net.minecraft.network.chat.Component called = name;
			String value = textures, signed = signature;
			client.execute(() -> ClientBodies.set(id, kind, who, called, value, signed, npc));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SOUL_SEEN, (client, handler, buf, sender) -> {
			int id = buf.readVarInt();
			boolean out = buf.readBoolean();
			Vec3 body = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
			int host = buf.readVarInt() - 1;
			float tether = buf.readFloat();
			boolean inside = buf.readBoolean();
			client.execute(() -> ClientSoul.seen(id, out, body, host, tether, inside));
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

		// The player's sect and title (SectMembership#sendStatus), for the Stats tab.
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SECT_STATUS, (client, handler, buf, sender) -> {
			String name = buf.readUtf();
			int title = buf.readVarInt();
			client.execute(() -> ClientSect.set(name, title));
		});
		// The Sect tab's page (SectMembership#sendInfo).
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SECT_INFO, (client, handler, buf, sender) -> {
			ClientSect.Info info = ClientSect.read(buf);
			client.execute(() -> ClientSect.setInfo(info));
		});
		// In one's clone or not, and how the main body fares (Clones), for the HUD.
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.CLONE_STATE, (client, handler, buf, sender) -> {
			boolean in = buf.readBoolean();
			float health = buf.readFloat(), max = buf.readFloat();
			client.execute(() -> ClientClone.set(in, health, max));
		});
		// What may be written out on a Blank Manual (TechniqueManualItem#offerInscription): the screen to pick it from.
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.INSCRIBE_OPTIONS, (client, handler, buf, sender) -> {
			int count = buf.readVarInt();
			List<int[]> options = new ArrayList<>(count);
			for (int i = 0; i < count; i++) options.add(new int[] {buf.readVarInt(), buf.readVarInt()});
			client.execute(() -> client.setScreen(new InscribeScreen(options)));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.FORMATIONS, (client, handler, buf, sender) -> {
			int count = buf.readVarInt();
			List<Formation> formations = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				UUID id = buf.readUUID();
				Formation.Kind kind = buf.readEnum(Formation.Kind.class);
				BlockPos center = buf.readBlockPos();
				int radius = buf.readVarInt();
				int rank = buf.readInt();
				UUID owner = buf.readBoolean() ? buf.readUUID() : null;
				int trustedCount = buf.readVarInt();
				List<UUID> trusted = new ArrayList<>(trustedCount);
				for (int t = 0; t < trustedCount; t++) trusted.add(buf.readUUID());
				formations.add(Formation.view(id, kind, center, radius, rank, owner, trusted));
			}
			client.execute(() -> ClientFormations.update(formations));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.BARRIER_RIPPLE, (client, handler, buf, sender) -> {
			BlockPos centre = buf.readBlockPos();
			int radius = buf.readVarInt();
			Formation.Kind kind = buf.readEnum(Formation.Kind.class);
			net.minecraft.world.phys.Vec3 way = new net.minecraft.world.phys.Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
			float strength = buf.readFloat();
			client.execute(() -> BarrierRipples.add(centre, radius, kind, way, strength));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.FORMATION_CORE, (client, handler, buf, sender) -> {
			BlockPos pos = buf.readBlockPos();
			int radius = buf.readVarInt();
			boolean wanted = buf.readBoolean(), raised = buf.readBoolean();
			double battery = buf.readDouble(), max = buf.readDouble(), supply = buf.readDouble();
			int veins = buf.readVarInt(), rank = buf.readInt(), cooldown = buf.readVarInt();
			String owner = buf.readUtf();
			int trustedCount = buf.readVarInt();
			List<FormationCoreScreen.Trusted> trusted = new ArrayList<>(trustedCount);
			for (int t = 0; t < trustedCount; t++) trusted.add(new FormationCoreScreen.Trusted(buf.readUUID(), buf.readUtf()));
			FormationCoreScreen.State state = new FormationCoreScreen.State(pos, radius, wanted, raised, battery, max, supply, veins, rank,
					cooldown, owner, trusted);
			client.execute(() -> FormationCoreScreen.receive(state));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.TALK_OPEN, (client, handler, buf, sender) -> {
			int entity = buf.readVarInt();
			String name = buf.readUtf();
			NpcTitle title = NpcTitle.byIndex(buf.readVarInt());
			String sect = buf.readUtf();
			int rank = buf.readInt();
			int faction = buf.readVarInt();
			int regard = buf.readInt();
			int intents = buf.readVarInt();
			Alignment.Faction path = Alignment.Faction.values()[Math.max(0, Math.min(Alignment.Faction.values().length - 1, faction))];
			TalkSession session = new TalkSession(entity, name, title, sect, rank, path, regard, intents);
			client.execute(() -> TalkSession.open(session));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.TALK_DIALOGUE, (client, handler, buf, sender) -> {
			int entity = buf.readVarInt();
			int regard = buf.readInt();
			TalkIntent intent = TalkIntent.byIndex(buf.readVarInt());
			Component line = buf.readComponent();
			int count = buf.readVarInt();
			List<TalkSession.Reply> replies = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				int id = buf.readVarInt();
				Component label = buf.readComponent();
				boolean enabled = buf.readBoolean();
				Component note = buf.readBoolean() ? buf.readComponent() : null;
				replies.add(new TalkSession.Reply(id, label, enabled, note));
			}
			TalkSession.Dialogue dialogue = new TalkSession.Dialogue(intent, line, replies);
			client.execute(() -> TalkSession.dialogue(entity, regard, dialogue));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.TALK_CLOSE, (client, handler, buf, sender) -> {
			int entity = buf.readVarInt();
			Component reason = buf.readComponent();
			client.execute(() -> TalkSession.close(entity, reason));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.GESTURE, (client, handler, buf, sender) -> {
			int entity = buf.readVarInt();
			Gesture gesture = Gesture.byIndex(buf.readVarInt());
			client.execute(() -> ClientGestures.start(entity, gesture));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.NPC_SAY, (client, handler, buf, sender) -> {
			int entity = buf.readVarInt();
			Component line = buf.readComponent();
			int ticks = buf.readVarInt();
			client.execute(() -> ClientNpcSpeech.say(entity, line, ticks));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.JOURNAL, (client, handler, buf, sender) -> {
			List<Journal.Entry> journal = Journal.read(buf);
			client.execute(() -> ClientJournal.set(journal));
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

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SWORD_FLIGHT, (client, handler, buf, sender) -> {
			int entityId = buf.readVarInt();
			boolean riding = buf.readBoolean();
			client.execute(() -> SwordFlightClient.set(entityId, riding));
		});

		// An open Storage Ring's space, in one packet or several (ClientStorageRing).
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.STORAGE_RING, (client, handler, buf, sender) -> {
			int containerId = buf.readVarInt();
			int grade = buf.readVarInt();
			boolean fresh = buf.readBoolean();
			java.util.List<ClientStorageRing.Entry> entries = new java.util.ArrayList<>();
			while (buf.readBoolean()) entries.add(new ClientStorageRing.Entry(buf.readItem(), buf.readVarLong()));
			client.execute(() -> ClientStorageRing.receive(containerId, grade, fresh, entries));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.MEDITATION_STATE, (client, handler, buf, sender) -> {
			UUID id = buf.readUUID();
			boolean meditating = buf.readBoolean();
			boolean floats = buf.readBoolean();
			client.execute(() -> ClientMeditationTracker.set(id, meditating, floats));
		});

		// The element spells: the auras on a being (ClientSpellAuras), and a spell's passing moment to draw (ClientSpellFx).
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SPELL_AURAS, (client, handler, buf, sender) -> {
			int entityId = buf.readVarInt();
			int count = buf.readVarInt();
			int[] auras = new int[count], left = new int[count];
			for (int i = 0; i < count; i++) {
				auras[i] = buf.readVarInt();
				left[i] = buf.readVarInt();
			}
			client.execute(() -> ClientSpellAuras.set(entityId, auras, left));
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SPELL_FX, (client, handler, buf, sender) -> {
			com.example.defyingtheheavens.SpellFx.Kind kind = com.example.defyingtheheavens.SpellFx.Kind.byIndex(buf.readVarInt());
			net.minecraft.world.phys.Vec3 at = new net.minecraft.world.phys.Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
			net.minecraft.world.phys.Vec3 way = new net.minecraft.world.phys.Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
			float a = buf.readFloat(), b = buf.readFloat();
			client.execute(() -> ClientSpellFx.add(kind, at, way, a, b));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.TRIBULATION_STATE, (client, handler, buf, sender) -> {
			boolean active = buf.readBoolean();
			int strikesLeft = buf.readVarInt();
			int realm = buf.readVarInt();
			int stage = buf.readVarInt();
			boolean ghost = buf.readBoolean();
			client.execute(() -> ClientTribulationData.update(active, strikesLeft, realm, stage, ghost));
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.LIFESPAN_TRIBULATION, (client, handler, buf, sender) -> {
			boolean active = buf.readBoolean();
			float power = buf.readFloat();
			boolean paused = buf.readBoolean();
			boolean ghost = buf.readBoolean();
			client.execute(() -> {
				ClientTribulationData.updateLifespan(active, power, paused, ghost);
				// The client's record knows it too: no Longevity Pill under it (PillItem's refusal, on both sides).
				ClientCultivationData.get().setLifespanTrial(active ? Math.round(power * com.example.defyingtheheavens.LifespanTribulation.FULL_POWER_TICKS) : -1);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SPACE_YEARS, (client, handler, buf, sender) -> {
			boolean inSpace = buf.readBoolean();
			double trip = buf.readDouble(), left = buf.readDouble(), home = buf.readDouble();
			client.execute(() -> ClientTribulationData.updateSpace(inSpace, trip, left, home));
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
