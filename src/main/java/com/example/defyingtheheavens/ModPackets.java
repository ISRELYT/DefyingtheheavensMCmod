package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
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
	/** C2S: put an ability in an ability wheel slot (slot, then the ability's ordinal, or -1 to empty the slot). */
	public static final ResourceLocation SET_WHEEL_SLOT = new ResourceLocation(DefyingTheHeavens.MOD_ID, "set_wheel_slot");
	/** S2C: a qi surge's screen game starts (game ordinal, window in ticks; -1 ends any running), see QiSurges. */
	public static final ResourceLocation SURGE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "surge");
	/** S2C: the soul is about to turn inward: fade the screen to black (see InnerRealm#FADE_TICKS). */
	public static final ResourceLocation INNER_FADE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "inner_fade");
	/** S2C: the player's sect and their title in it, for the Stats tab (SectMembership#sendStatus). */
	public static final ResourceLocation SECT_STATUS = new ResourceLocation(DefyingTheHeavens.MOD_ID, "sect_status");
	/** C2S: the Sect tab opening asks for its page; S2C SECT_INFO is it (SectMembership#sendInfo). */
	public static final ResourceLocation SECT_INFO_REQUEST = new ResourceLocation(DefyingTheHeavens.MOD_ID, "sect_info_request");
	public static final ResourceLocation SECT_INFO = new ResourceLocation(DefyingTheHeavens.MOD_ID, "sect_info");
	/**
	 * C2S: the Sect tab's buttons for a soul in a sect member's body (SectMembership#leaveAsBody, #banish): 0 leave the sect as the
	 * body, or 1 and a member's id to cast them out.
	 */
	public static final ResourceLocation SECT_ACT = new ResourceLocation(DefyingTheHeavens.MOD_ID, "sect_act");
	/** S2C: whether the player is in their clone now, and how their main body fares where it sits (Clones), for the HUD. */
	public static final ResourceLocation CLONE_STATE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "clone_state");
	/**
	 * S2C: what the player can write out on the Blank Manual in their off hand (TechniqueManualItem#writable): kinds (0 a technique, 1 a
	 * spell) and ordinals, opening the inscription screen; C2S inscribe: the one chosen.
	 */
	public static final ResourceLocation INSCRIBE_OPTIONS = new ResourceLocation(DefyingTheHeavens.MOD_ID, "inscribe_options");
	public static final ResourceLocation INSCRIBE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "inscribe");
	/** C2S: put points into a stat (StatPoints): the stat's place, then how many. */
	public static final ResourceLocation SPEND_STAT_POINTS = new ResourceLocation(DefyingTheHeavens.MOD_ID, "spend_stat_points");
	/** C2S: select an ability on the wheel for the Use Selected Ability key: its place (-1: none). */
	public static final ResourceLocation SELECT_ABILITY = new ResourceLocation(DefyingTheHeavens.MOD_ID, "select_ability");
	/** C2S: the Law meditation in the Immortal Realm comprehends (Laws#focus): its place (-1: none). */
	public static final ResourceLocation LAW_FOCUS = new ResourceLocation(DefyingTheHeavens.MOD_ID, "law_focus");
	/** C2S: a level owed for a weathered Decay spent on a Law (Laws#choose): its place. */
	public static final ResourceLocation LAW_CHOOSE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "law_choose");
	/** S2C: full cultivation state for the owning player. */
	public static final ResourceLocation SYNC = new ResourceLocation(DefyingTheHeavens.MOD_ID, "sync");
	/** S2C: "player X (entity id) is/isn't riding their sword" (see {@link SwordFlight}), repeated once a second while they ride. */
	public static final ResourceLocation SWORD_FLIGHT = new ResourceLocation(DefyingTheHeavens.MOD_ID, "sword_flight");
	/** S2C: "player X is/isn't meditating", for rendering the lotus pose. */
	public static final ResourceLocation MEDITATION_STATE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "meditation_state");
	/** S2C: tribulation started/ended + strikes still to come, for the HUD overlay. */
	public static final ResourceLocation TRIBULATION_STATE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "tribulation_state");
	/** S2C: the lifespan tribulation running or not, its power, whether it waits (beyond the gap), the Netherworld's (LifespanTribulation). */
	public static final ResourceLocation LIFESPAN_TRIBULATION = new ResourceLocation(DefyingTheHeavens.MOD_ID, "lifespan_tribulation");
	/** S2C: the years flying through space has cost this trip, the years left, and the years flying straight home would cost (AstralRealm); or none, out of space. */
	public static final ResourceLocation SPACE_YEARS = new ResourceLocation(DefyingTheHeavens.MOD_ID, "space_years");
	/** C2S: the Cultivation tab's pace button: the next pace for meditating (Lifespan#cycleMeditationSpeed). */
	public static final ResourceLocation MEDITATION_SPEED = new ResourceLocation(DefyingTheHeavens.MOD_ID, "meditation_speed");
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
	/** S2C: a barrier was struck (centre, radius, kind, the way out from its centre to the blow, strength): it ripples. */
	public static final ResourceLocation BARRIER_RIPPLE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "barrier_ripple");
	/** S2C: a Formation Core's state, opening (or refreshing) its screen. */
	public static final ResourceLocation FORMATION_CORE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "formation_core");
	/** C2S: the open core screen asks for a fresh state (its position). */
	public static final ResourceLocation FORMATION_CORE_QUERY = new ResourceLocation(DefyingTheHeavens.MOD_ID, "formation_core_query");
	/** C2S: the owner set a core's radius and switch (position, radius, on). */
	public static final ResourceLocation FORMATION_CORE_CONFIGURE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "formation_core_configure");
	/** C2S: the owner trusts a player by name (position, true, name) or stops trusting one (position, false, UUID). */
	public static final ResourceLocation FORMATION_CORE_TRUST = new ResourceLocation(DefyingTheHeavens.MOD_ID, "formation_core_trust");
	/**
	 * S2C: the player's own soul (see {@link SoulProjection}): state, channel ticks left, where the body sits, the tether's reach,
	 * the body's health and maximum, the possessed being's entity id (-1 for none) and the tether's remaining strength (0-1).
	 */
	public static final ResourceLocation SOUL_SELF = new ResourceLocation(DefyingTheHeavens.MOD_ID, "soul_self");
	public static final ResourceLocation BODY_LOOK = new ResourceLocation(DefyingTheHeavens.MOD_ID, "body_look");
	/**
	 * S2C: a soul someone perceives: its entity id, whether it is still out, where its body sits, the being it possesses (-1) and
	 * its tether's strength. Repeated twice a second while it stays out; clients forget it without.
	 */
	public static final ResourceLocation SOUL_SEEN = new ResourceLocation(DefyingTheHeavens.MOD_ID, "soul_seen");
	/** C2S: a Heavenly Being with Qi Sense on strikes at the tether of the soul with this entity id (checked on the server). */
	public static final ResourceLocation SOUL_TETHER_STRIKE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "soul_tether_strike");
	/** C2S: forward held in Meteor Flight, or let go, and flying flat out or not (MeteorFlight#setStreaking; the server checks it may). */
	public static final ResourceLocation METEOR_STREAK = new ResourceLocation(DefyingTheHeavens.MOD_ID, "meteor_streak");
	/**
	 * C2S: the blocks Meteor Flight's bulldozer cleared ahead on the flier's screen (Bulldozer), and the prediction sequence they were
	 * cleared under (MeteorFlight#bulldoze takes what may go and acknowledges it).
	 */
	public static final ResourceLocation METEOR_DOZE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "meteor_doze");
	/**
	 * S2C: a player (entity id) flying fast in Meteor Flight or not, how hot (0-255), and whether the gamerule lets the bulldozer
	 * clear the way, to them and those tracking them.
	 */
	public static final ResourceLocation METEOR_STATE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "meteor_state");
	/** S2C: a player (entity id) broke the sound barrier here, going this way: the ring of air (MeteorFlightClient). */
	public static final ResourceLocation METEOR_BOOM = new ResourceLocation(DefyingTheHeavens.MOD_ID, "meteor_boom");
	/** S2C: a talk with a cultivator opens (see {@link TalkSystem}): who they are, what they think of you, the intents on offer. */
	public static final ResourceLocation TALK_OPEN = new ResourceLocation(DefyingTheHeavens.MOD_ID, "talk_open");
	/** S2C: the dialogue box: what they say, and the replies on offer. */
	public static final ResourceLocation TALK_DIALOGUE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "talk_dialogue");
	/** C2S: the player chose an intent (entity id, intent ordinal). */
	public static final ResourceLocation TALK_INTENT = new ResourceLocation(DefyingTheHeavens.MOD_ID, "talk_intent");
	/** C2S: the player picked a reply in the dialogue box (entity id, topic). */
	public static final ResourceLocation TALK_TOPIC = new ResourceLocation(DefyingTheHeavens.MOD_ID, "talk_topic");
	/** C2S: the player closed the talk (entity id). */
	public static final ResourceLocation TALK_END = new ResourceLocation(DefyingTheHeavens.MOD_ID, "talk_end");
	/** S2C: the cultivator broke off the talk (entity id, why): the talk window closes and the reason shows over the hotbar. */
	public static final ResourceLocation TALK_CLOSE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "talk_close");
	/** C2S: a right-click on a cultivator past arm's length, within TalkSystem#REACH (entity id). */
	public static final ResourceLocation TALK_REQUEST = new ResourceLocation(DefyingTheHeavens.MOD_ID, "talk_request");
	/** S2C: someone in sight makes a gesture (entity id, Gesture ordinal), drawn for Gesture#TICKS. */
	public static final ResourceLocation GESTURE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "gesture");
	/** S2C: a cultivator nearby says something of its own accord (entity id, the line, ticks it stays up), over its head (NpcSpeech). */
	public static final ResourceLocation NPC_SAY = new ResourceLocation(DefyingTheHeavens.MOD_ID, "npc_say");
	/** S2C: the player's journal (see Journal), whole. C2S JOURNAL_REQUEST asks for it (the menu's Journal tab opening). */
	public static final ResourceLocation JOURNAL = new ResourceLocation(DefyingTheHeavens.MOD_ID, "journal");
	public static final ResourceLocation JOURNAL_REQUEST = new ResourceLocation(DefyingTheHeavens.MOD_ID, "journal_request");
	/**
	 * S2C: the player's own Netherworld state (see NetherworldManager#sync): the ghostly qi pool and its maximum, the density of
	 * ghostly qi here, yin corruption (-1 where it doesn't apply), which way it is going (1 rising, -1 ebbing, 0 held), and how
	 * many seconds a Yang Preservation Pill still holds it. Kept apart from SYNC, so that one doesn't grow.
	 */
	public static final ResourceLocation NETHERWORLD_SYNC = new ResourceLocation(DefyingTheHeavens.MOD_ID, "netherworld_sync");
	/** S2C: the player is on the Soul River (route ordinal; NONE: off it) and this many ticks into the journey (see SoulRiver). */
	public static final ResourceLocation SOUL_RIVER = new ResourceLocation(DefyingTheHeavens.MOD_ID, "soul_river");
	/** S2C: how far the Chief Judge's gaze has taken the player, 0 to 1 (see JudgeEye): the client warps and whispers by it. */
	public static final ResourceLocation JUDGE_GAZE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "judge_gaze");
	/** S2C: player X (entity id) is or isn't a ghost (see GhostSystem): to the ghost and everyone tracking it, as soon as they do. */
	public static final ResourceLocation GHOST = new ResourceLocation(DefyingTheHeavens.MOD_ID, "ghost");
	/** S2C: the screen fades to a colour (0xRRGGBB) over so many ticks, holds, and fades back (a ghost dispersing, the Wheel turning). */
	public static final ResourceLocation GHOST_FADE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "ghost_fade");
	/** S2C: entity X (id) is bound by a Ghost-Quelling Talisman for so many ticks (QuelledEffect): the talisman shows on its brow. */
	public static final ResourceLocation QUELLED = new ResourceLocation(DefyingTheHeavens.MOD_ID, "quelled");
	/**
	 * S2C: the player's ghost methods and spells (see GhostMethods, Spells): each method's layer and progress, the active one,
	 * the readied spell, the spell wheel's slots, each spell's cooldown left (ticks), and whether they are a ghost.
	 */
	public static final ResourceLocation METHODS = new ResourceLocation(DefyingTheHeavens.MOD_ID, "methods");
	/** C2S: practise this method (its id): meditation cultivates it. */
	public static final ResourceLocation PRACTISE_METHOD = new ResourceLocation(DefyingTheHeavens.MOD_ID, "practise_method");
	/** C2S: practise this heaven method (its id): the one meditation cultivates (HeavenMethods#practise). */
	public static final ResourceLocation PRACTISE_HEAVEN_METHOD = new ResourceLocation(DefyingTheHeavens.MOD_ID, "practise_heaven_method");
	/** C2S: the Cultivation tab's switch: draw in the other qi, cultivating the other path (TwoPaths#switchPath). */
	public static final ResourceLocation SWITCH_PATH = new ResourceLocation(DefyingTheHeavens.MOD_ID, "switch_path");
	/** C2S: ready this spell for the cast key (its ordinal plus one; 0 clears it). */
	public static final ResourceLocation READY_SPELL = new ResourceLocation(DefyingTheHeavens.MOD_ID, "ready_spell");
	/** C2S: cast this spell now (its ordinal plus one; 0 the readied one). */
	public static final ResourceLocation CAST_SPELL = new ResourceLocation(DefyingTheHeavens.MOD_ID, "cast_spell");
	/** C2S: put a spell in a slot of the spell wheel (the slot, then the spell's ordinal plus one; 0 empties it). */
	public static final ResourceLocation SET_SPELL_SLOT = new ResourceLocation(DefyingTheHeavens.MOD_ID, "set_spell_slot");
	/** S2C: a being's spell auras (SpellAuras): its entity id, then each aura's ordinal and the ticks it has left (none: all gone). */
	public static final ResourceLocation SPELL_AURAS = new ResourceLocation(DefyingTheHeavens.MOD_ID, "spell_auras");
	/** S2C: a player's Law domain (LawDomains): its entity id, the Law's ordinal plus one (0: folded away), and its reach. */
	public static final ResourceLocation LAW_DOMAIN = new ResourceLocation(DefyingTheHeavens.MOD_ID, "law_domain");
	/** S2C: a spell's passing moment to draw (SpellFx): its kind, where, a way, and two numbers of its own. */
	public static final ResourceLocation SPELL_FX = new ResourceLocation(DefyingTheHeavens.MOD_ID, "spell_fx");
	/**
	 * S2C: a sound under a spell's voice (SpellSounds#play): its voice, its handle, the sound, where, the thing it follows (0: none), its
	 * volume and pitch, and whether it loops.
	 */
	public static final ResourceLocation SPELL_SOUND = new ResourceLocation(DefyingTheHeavens.MOD_ID, "spell_sound");
	/** S2C: a spell's voice hushed (SpellSounds#hush): the voice, the handle (0: all its sounds), and how many ticks they fade over. */
	public static final ResourceLocation SPELL_SOUND_HUSH = new ResourceLocation(DefyingTheHeavens.MOD_ID, "spell_sound_hush");
	/** C2S: what Life Force Drain and Gain move (WoodSpells.LifeMode, by ordinal), from the Spells tab. */
	public static final ResourceLocation LIFE_MODE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "life_mode");
	/** C2S: the Spells tab's switch for whether spells change the land for good (the gamerule spellGriefing): on or off. */
	public static final ResourceLocation SPELL_GRIEFING = new ResourceLocation(DefyingTheHeavens.MOD_ID, "spell_griefing");
	/**
	 * S2C: an open Storage Ring's space (StorageRings#send): the screen's id, the ring's grade, whether this begins the space afresh,
	 * then its entries (each the item and how many, a flag before each), in as many packets as it takes.
	 */
	public static final ResourceLocation STORAGE_RING = new ResourceLocation(DefyingTheHeavens.MOD_ID, "storage_ring");
	/** C2S: a click on the open ring's space (StorageRings#act): what to do, and the kind of thing clicked (empty: none). */
	public static final ResourceLocation STORAGE_RING_ACTION = new ResourceLocation(DefyingTheHeavens.MOD_ID, "storage_ring_action");
	/** C2S: the Storage Ring key: open the ring worn (or else the first one on the player). */
	public static final ResourceLocation STORAGE_RING_OPEN = new ResourceLocation(DefyingTheHeavens.MOD_ID, "storage_ring_open");
	/** S2C: the Astral Realm's seed (AstralRealm#sendSeed), from which the client works out the stars and planets. */
	public static final ResourceLocation ASTRAL_SEED = new ResourceLocation(DefyingTheHeavens.MOD_ID, "astral_seed");
	/** S2C: "Name Your Obsession" (Obsession), before the tribulation into Heavenly Being; C2S: the one named (and whose path's tribulation). */
	public static final ResourceLocation OBSESSION_ASK = new ResourceLocation(DefyingTheHeavens.MOD_ID, "obsession_ask");
	public static final ResourceLocation OBSESSION_NAME = new ResourceLocation(DefyingTheHeavens.MOD_ID, "obsession_name");
	/** S2C: choose your bloodline (Bloodlines: the race picker); C2S: the one chosen and its colouring; S2C: players' bloodlines. */
	public static final ResourceLocation BLOODLINE_ASK = new ResourceLocation(DefyingTheHeavens.MOD_ID, "bloodline_ask");
	public static final ResourceLocation BLOODLINE_CHOOSE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "bloodline_choose");
	public static final ResourceLocation BLOODLINES = new ResourceLocation(DefyingTheHeavens.MOD_ID, "bloodlines");
	/** C2S: the title one takes (PlayerTitles, from Void Shattering on); S2C: players' plates over their heads, their titles and sect titles. */
	public static final ResourceLocation SET_TITLE = new ResourceLocation(DefyingTheHeavens.MOD_ID, "set_title");
	public static final ResourceLocation PLAYER_PLATES = new ResourceLocation(DefyingTheHeavens.MOD_ID, "player_plates");
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
		ServerPlayNetworking.registerGlobalReceiver(TALK_INTENT, (server, player, handler, buf, responseSender) -> {
			int entity = buf.readVarInt();
			int intent = buf.readVarInt();
			server.execute(() -> TalkSystem.intent(player, entity, intent));
		});
		ServerPlayNetworking.registerGlobalReceiver(TALK_TOPIC, (server, player, handler, buf, responseSender) -> {
			int entity = buf.readVarInt();
			int topic = buf.readVarInt();
			server.execute(() -> TalkSystem.topic(player, entity, topic));
		});
		ServerPlayNetworking.registerGlobalReceiver(TALK_END, (server, player, handler, buf, responseSender) -> {
			int entity = buf.readVarInt();
			server.execute(() -> TalkSystem.end(player, entity));
		});
		ServerPlayNetworking.registerGlobalReceiver(TALK_REQUEST, (server, player, handler, buf, responseSender) -> {
			int entity = buf.readVarInt();
			server.execute(() -> TalkSystem.request(player, entity));
		});
		ServerPlayNetworking.registerGlobalReceiver(JOURNAL_REQUEST,
				(server, player, handler, buf, responseSender) -> server.execute(() -> sendJournal(player)));
		ServerPlayNetworking.registerGlobalReceiver(SECT_INFO_REQUEST,
				(server, player, handler, buf, responseSender) -> server.execute(() -> SectMembership.sendInfo(player)));
		ServerPlayNetworking.registerGlobalReceiver(INSCRIBE, (server, player, handler, buf, responseSender) -> {
			int kind = buf.readVarInt();
			int index = buf.readVarInt();
			server.execute(() -> TechniqueManualItem.inscribe(player, kind, index));
		});
		ServerPlayNetworking.registerGlobalReceiver(SECT_ACT, (server, player, handler, buf, responseSender) -> {
			int act = buf.readVarInt();
			UUID target = act == 1 ? buf.readUUID() : null;
			server.execute(() -> {
				if (act == 0 && SectMembership.leaveAsBody(player)) SectMembership.sendInfo(player);
				else if (act == 1) SectMembership.banish(player, target);
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(SPEND_STAT_POINTS, (server, player, handler, buf, responseSender) -> {
			int stat = buf.readVarInt();
			int count = buf.readVarInt();
			server.execute(() -> StatPoints.spend(player, StatPoints.Stat.byIndex(stat), Math.min(count, 1000)));
		});
		ServerPlayNetworking.registerGlobalReceiver(LAW_FOCUS, (server, player, handler, buf, responseSender) -> {
			Law law = Law.byIndex(buf.readVarInt());
			server.execute(() -> Laws.focus(player, law));
		});
		ServerPlayNetworking.registerGlobalReceiver(LAW_CHOOSE, (server, player, handler, buf, responseSender) -> {
			Law law = Law.byIndex(buf.readVarInt());
			server.execute(() -> Laws.choose(player, law));
		});
		ServerPlayNetworking.registerGlobalReceiver(LIFE_MODE, (server, player, handler, buf, responseSender) -> {
			WoodSpells.LifeMode mode = WoodSpells.LifeMode.byIndex(buf.readByte());
			server.execute(() -> WoodSpells.setLifeMode(player, mode));
		});
		ServerPlayNetworking.registerGlobalReceiver(SPELL_GRIEFING, (server, player, handler, buf, responseSender) -> {
			boolean on = buf.readBoolean();
			server.execute(() -> ModGameRules.setSpellGriefing(player, on));
		});
		ServerPlayNetworking.registerGlobalReceiver(TOGGLE_MEDITATION,
				(server, player, handler, buf, responseSender) -> server.execute(() -> MeditationManager.toggle(player)));
		ServerPlayNetworking.registerGlobalReceiver(BREAKTHROUGH,
				(server, player, handler, buf, responseSender) -> server.execute(() -> CultivationManager.tryBreakthrough(player)));
		ServerPlayNetworking.registerGlobalReceiver(OBSESSION_NAME, (server, player, handler, buf, responseSender) -> {
			FourAxis.Axis obsession = FourAxis.Axis.byIndex(buf.readVarInt());
			boolean ghost = buf.readBoolean();
			server.execute(() -> Obsession.name(player, obsession, ghost));
		});
		ServerPlayNetworking.registerGlobalReceiver(BLOODLINE_CHOOSE, (server, player, handler, buf, responseSender) -> {
			Bloodline bloodline = Bloodline.byIndex(buf.readVarInt());
			int variant = buf.readVarInt();
			server.execute(() -> Bloodlines.choose(player, bloodline, variant));
		});
		ServerPlayNetworking.registerGlobalReceiver(SET_TITLE, (server, player, handler, buf, responseSender) -> {
			String title = buf.readUtf(PlayerTitles.MAX_LENGTH * 4);
			server.execute(() -> PlayerTitles.set(player, title));
		});
		ServerPlayNetworking.registerGlobalReceiver(CIRCULATION, (server, player, handler, buf, responseSender) -> {
			int outcome = buf.readVarInt();
			server.execute(() -> MeditationManager.onCirculation(player, outcome));
		});
		ServerPlayNetworking.registerGlobalReceiver(TOGGLE_ABILITY, (server, player, handler, buf, responseSender) -> {
			int ability = buf.readVarInt();
			server.execute(() -> CultivationManager.toggleAbility(player, ability));
		});
		ServerPlayNetworking.registerGlobalReceiver(SELECT_ABILITY, (server, player, handler, buf, responseSender) -> {
			int ability = buf.readVarInt();
			server.execute(() -> CultivationManager.selectAbility(player, ability));
		});
		ServerPlayNetworking.registerGlobalReceiver(SET_WHEEL_SLOT, (server, player, handler, buf, responseSender) -> {
			int slot = buf.readVarInt();
			int ability = buf.readVarInt();
			server.execute(() -> CultivationManager.setWheelSlot(player, slot, ability));
		});
		ServerPlayNetworking.registerGlobalReceiver(PRACTISE_METHOD, (server, player, handler, buf, responseSender) -> {
			GhostMethod method = GhostMethod.byId(buf.readUtf(64));
			server.execute(() -> Spells.practise(player, method));
		});
		ServerPlayNetworking.registerGlobalReceiver(SWITCH_PATH, (server, player, handler, buf, responseSender) ->
				server.execute(() -> TwoPaths.switchPath(player)));
		ServerPlayNetworking.registerGlobalReceiver(MEDITATION_SPEED, (server, player, handler, buf, responseSender) ->
				server.execute(() -> Lifespan.cycleMeditationSpeed(player)));
		ServerPlayNetworking.registerGlobalReceiver(PRACTISE_HEAVEN_METHOD, (server, player, handler, buf, responseSender) -> {
			HeavenMethod method = HeavenMethod.byId(buf.readUtf(64));
			server.execute(() -> HeavenMethods.practise(player, method));
		});
		ServerPlayNetworking.registerGlobalReceiver(READY_SPELL, (server, player, handler, buf, responseSender) -> {
			Spell spell = Spell.byIndex(buf.readVarInt() - 1);
			server.execute(() -> Spells.ready(player, spell));
		});
		ServerPlayNetworking.registerGlobalReceiver(CAST_SPELL, (server, player, handler, buf, responseSender) -> {
			Spell spell = Spell.byIndex(buf.readVarInt() - 1);
			server.execute(() -> Spells.cast(player, spell));
		});
		ServerPlayNetworking.registerGlobalReceiver(SET_SPELL_SLOT, (server, player, handler, buf, responseSender) -> {
			int slot = buf.readVarInt();
			Spell spell = Spell.byIndex(buf.readVarInt() - 1);
			server.execute(() -> Spells.setSlot(player, slot, spell));
		});
		ServerPlayNetworking.registerGlobalReceiver(SOUL_TETHER_STRIKE, (server, player, handler, buf, responseSender) -> {
			int soul = buf.readVarInt();
			server.execute(() -> SoulProjection.strikeTether(player, soul));
		});
		ServerPlayNetworking.registerGlobalReceiver(METEOR_STREAK, (server, player, handler, buf, responseSender) -> {
			boolean on = buf.readBoolean(), flatOut = buf.readBoolean();
			server.execute(() -> MeteorFlight.setStreaking(player, on, flatOut));
		});
		ServerPlayNetworking.registerGlobalReceiver(METEOR_DOZE, (server, player, handler, buf, responseSender) -> {
			int sequence = buf.readVarInt();
			int count = Math.max(0, Math.min(buf.readVarInt(), buf.readableBytes() / Long.BYTES));
			List<BlockPos> cleared = new ArrayList<>(Math.min(count, Bulldozer.MOST_A_TICK));
			for (int i = 0; i < count; i++) {
				long pos = buf.readLong();
				if (cleared.size() < Bulldozer.MOST_A_TICK) cleared.add(BlockPos.of(pos));
			}
			server.execute(() -> MeteorFlight.bulldoze(player, sequence, cleared));
		});
		ServerPlayNetworking.registerGlobalReceiver(STORAGE_RING_ACTION, (server, player, handler, buf, responseSender) -> {
			int action = buf.readVarInt();
			ItemStack like = buf.readItem();
			server.execute(() -> StorageRings.act(player, action, like));
		});
		ServerPlayNetworking.registerGlobalReceiver(STORAGE_RING_OPEN,
				(server, player, handler, buf, responseSender) -> server.execute(() -> StorageRings.openFirst(player)));
	}

	/**
	 * An open ring's space to {@code player} ({@link #STORAGE_RING}): the grade, then the entries, a new packet whenever one passes
	 * {@code maxBytes}.
	 */
	public static void sendStorageRing(ServerPlayer player, int containerId, StorageRings.Grade grade, java.util.List<StorageRings.Entry> entries, int maxBytes) {
		FriendlyByteBuf buf = storageRingHeader(containerId, grade, true);
		for (StorageRings.Entry entry : entries) {
			buf.writeBoolean(true);
			buf.writeItem(entry.kind());
			buf.writeVarLong(entry.count());
			if (buf.writerIndex() > maxBytes) {
				buf.writeBoolean(false);
				ServerPlayNetworking.send(player, STORAGE_RING, buf);
				buf = storageRingHeader(containerId, grade, false);
			}
		}
		buf.writeBoolean(false);
		ServerPlayNetworking.send(player, STORAGE_RING, buf);
	}

	private static FriendlyByteBuf storageRingHeader(int containerId, StorageRings.Grade grade, boolean fresh) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(containerId);
		buf.writeVarInt(grade.ordinal());
		buf.writeBoolean(fresh);
		return buf;
	}

	public static void sendSoulSelf(ServerPlayer player, SoulProjection.State state, int ticks, Vec3 body, int radius, float bodyHealth,
									float bodyMax, int hostId, float tether, float ownQi, float ownMax, boolean ghostPool, boolean bodyHere,
									boolean soulTears) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(state.ordinal());
		buf.writeVarInt(Math.max(0, ticks));
		buf.writeDouble(body.x);
		buf.writeDouble(body.y);
		buf.writeDouble(body.z);
		buf.writeVarInt(radius);
		buf.writeFloat(bodyHealth);
		buf.writeFloat(bodyMax);
		buf.writeVarInt(hostId + 1);
		buf.writeFloat(tether);
		buf.writeFloat(ownQi);
		buf.writeFloat(ownMax);
		buf.writeBoolean(ghostPool);
		buf.writeBoolean(bodyHere);
		buf.writeBoolean(soulTears);
		ServerPlayNetworking.send(player, SOUL_SELF, buf);
	}

	/**
	 * Whose look {@code soul} wears now (Possession), to everyone in its world: another player's ({@code player}: their skin, cape
	 * and {@code name}; their profile's {@code textures}, for clients that don't have them in the list, while they're away), an NPC
	 * cultivator's (by its entity, {@code npcId}: its skin and nameplate), or ({@code player} null and {@code npcId} -1) its own
	 * again. Told again each second while it lasts, and forgotten by clients that stop hearing it.
	 */
	public static void broadcastBodyLook(ServerPlayer soul, java.util.UUID player, Component name, com.mojang.authlib.properties.Property textures,
										 int npcId) {
		for (ServerPlayer other : soul.serverLevel().players()) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeVarInt(soul.getId());
			buf.writeByte(player != null ? 1 : npcId >= 0 ? 2 : 0);
			if (player != null) {
				buf.writeUUID(player);
				buf.writeComponent(name == null ? Component.empty() : name);
				buf.writeBoolean(textures != null);
				if (textures != null) {
					buf.writeUtf(textures.getValue());
					buf.writeNullable(textures.getSignature(), FriendlyByteBuf::writeUtf);
				}
			} else if (npcId >= 0) {
				buf.writeVarInt(npcId);
			}
			ServerPlayNetworking.send(other, BODY_LOOK, buf);
		}
	}

	/**
	 * A soul {@code to} perceives: its body's place (for the tether; {@code tether} -1: none to draw, its body in another realm), the
	 * beast it rides, and ({@code inside}) whether it is in a person's body (Possession: drawn as a violet veil over that body).
	 */
	public static void sendSoulSeen(ServerPlayer to, ServerPlayer soul, Vec3 body, int hostId, float tether, boolean inside) {
		ServerPlayNetworking.send(to, SOUL_SEEN, soulSeenBuf(soul, true, body, hostId, tether, inside));
	}

	/** The soul is home (or gone): everyone who might have been drawing it forgets it. */
	public static void broadcastSoulGone(ServerPlayer soul) {
		for (ServerPlayer other : soul.serverLevel().players()) {
			if (other != soul) ServerPlayNetworking.send(other, SOUL_SEEN, soulSeenBuf(soul, false, Vec3.ZERO, -1, 0, false));
		}
	}

	private static FriendlyByteBuf soulSeenBuf(ServerPlayer soul, boolean out, Vec3 body, int hostId, float tether, boolean inside) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(soul.getId());
		buf.writeBoolean(out);
		buf.writeDouble(body.x);
		buf.writeDouble(body.y);
		buf.writeDouble(body.z);
		buf.writeVarInt(hostId + 1);
		buf.writeFloat(tether);
		buf.writeBoolean(inside);
		return buf;
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
		// The ability wheel, slot by slot: the ability's ordinal plus one, 0 for an empty slot.
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) {
			Ability ability = c.getWheelSlot(i);
			buf.writeVarInt(ability == null ? 0 : ability.ordinal() + 1);
		}
		buf.writeVarInt(c.getSoulState().ordinal());
		buf.writeBoolean(c.hasHeavensLeave());
		buf.writeVarInt(c.getFavour());
		// The body they were born with (Physique): its ordinal (-1 not yet rolled), the roots' elements as a bit mask, body refining,
		// and a Myriad Form Body's forms as a bit mask over their ordinals.
		buf.writeVarInt(c.getPhysique() == null ? -1 : c.getPhysique().ordinal());
		int roots = 0;
		for (QiElement element : c.getRootElements()) roots |= 1 << element.ordinal();
		buf.writeVarInt(roots);
		buf.writeVarInt(c.getBodyRefining());
		long forms = 0;
		for (Physique form : c.getForms()) forms |= 1L << form.ordinal();
		buf.writeVarLong(forms);
		// The points put into each stat (StatPoints), by the stat's place.
		for (StatPoints.Stat stat : StatPoints.Stat.values()) buf.writeVarInt(c.getStatPoints(stat));
		// The ability selected on the wheel, by its place (+1; 0 for none).
		buf.writeVarInt(c.getSelectedAbility() == null ? 0 : c.getSelectedAbility().ordinal() + 1);
		// How dense qi is where they stand (the Astral Realm's places: AstralRealm).
		buf.writeDouble(c.getPlaceQi());
		// What the nascent soul still has to mend (SoulProjection#mendSouls), for Soul Projection's word of its hearts.
		buf.writeFloat(c.getSoulWounds());
		// Their own years (Lifespan; a soul in another's body goes by its own): those used, the span, and whether they are dead.
		PlayerCultivation own = CultivationManager.own(player);
		buf.writeDouble(Lifespan.used(own));
		buf.writeDouble(Lifespan.span(own));
		buf.writeBoolean(own.netherworld().ghost());
		buf.writeVarInt(c.longevityPills()); // the Longevity Pills taken this life, for the next one's tooltip (PillItem)
		// The four axes of virtue (FourAxis), their own: how far each is filled, in its measure, by its place.
		for (FourAxis.Axis axis : FourAxis.Axis.values()) buf.writeVarInt(own.axisProgress(axis));
		// The obsession named (Obsession), the record's: -1 none.
		buf.writeVarInt(c.obsession() == null ? -1 : c.obsession().ordinal());
		buf.writeVarInt(own.meditationSpeed()); // the pace its meditation is set to (the Cultivation tab's button), their own
		// The techniques learned from manuals (a bit mask over their ordinals), the spells learned from them (their ordinals), and
		// whether they play their clone now (Clones: its way back stays on the wheel).
		int techniques = 0;
		for (Technique technique : c.techniques()) techniques |= 1 << technique.ordinal();
		buf.writeVarInt(techniques);
		buf.writeVarInt(c.manualSpells().size());
		for (Spell spell : c.manualSpells()) buf.writeVarInt(spell.ordinal());
		buf.writeBoolean(Clones.inClone(player));
		// The Demon Path (DemonPath): its rank (-1 none), its stage's cultivation, whether a meditation draws it in, the record's blood.
		buf.writeVarInt(c.demon().rank());
		buf.writeDouble(c.demon().cultivation());
		buf.writeBoolean(c.demon().cultivating());
		buf.writeVarInt(c.demon().bloodline().ordinal());
		// What of a race's blood it has drunk (DemonBlood): the realm its blood is awake to (-1 none), a half-blood's race (-1 none), its purity.
		buf.writeVarInt(c.demon().bloodRealm());
		buf.writeVarInt(c.demon().halfBlood() == null ? -1 : c.demon().halfBlood().ordinal());
		buf.writeDouble(c.demon().purity());
		// A True Immortal's immortal essence (ImmortalEssence): what it holds, and its pool (0 short of True Immortal).
		buf.writeDouble(c.essence());
		buf.writeDouble(ImmortalEssence.max(c));
		// The Laws (Laws, the record's): each one's level and insight by its place, the Law in focus (-1 none), the levels owed to choose;
		// and the Five Decays (FiveDecays): the sign under way and the game time the next is due (0: not reckoned).
		LawState laws = c.laws();
		for (Law law : Law.values()) {
			buf.writeVarInt(laws.level(law));
			buf.writeDouble(laws.insight(law));
		}
		buf.writeVarInt(laws.focus() == null ? -1 : laws.focus().ordinal());
		buf.writeVarInt(laws.choices());
		buf.writeVarInt(laws.decaySign());
		buf.writeLong(laws.decayDue());
		// Whether they stand in the Immortal Realm: a breakthrough into True Immortal and past it is made only there (PlayerCultivation#isImmortalRealmLocked).
		buf.writeBoolean(c.isInImmortalRealm());
		// Whether they have a small world of their own (SmallWorlds): its door, the Abilities tab's.
		buf.writeBoolean(c.hasSmallWorld());
		// The title they have taken (PlayerTitles), their own record's: the Cultivation tab's field.
		buf.writeUtf(own.epithet(), PlayerTitles.MAX_LENGTH * 4);
		ServerPlayNetworking.send(player, SYNC, buf);
	}

	/** S2C clone_state: in their clone now or not, and their main body's health and its whole where it sits (Clones). */
	public static void sendCloneState(ServerPlayer player, boolean in, float mainHealth, float mainMax) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(in);
		buf.writeFloat(mainHealth);
		buf.writeFloat(mainMax);
		ServerPlayNetworking.send(player, CLONE_STATE, buf);
	}

	/** S2C inscribe_options: what {@code player} may write out ({@code options}: kind, then ordinal), opening the inscription screen. */
	public static void sendInscribeOptions(ServerPlayer player, List<int[]> options) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(options.size());
		for (int[] option : options) {
			buf.writeVarInt(option[0]);
			buf.writeVarInt(option[1]);
		}
		ServerPlayNetworking.send(player, INSCRIBE_OPTIONS, buf);
	}

	/**
	 * S2C space_years: the HUD's line in space (AstralRealm#tickSpace), or {@code inSpace} false to take it away. Whole doubles: the
	 * realms after Four Axis have billions of years left, past what a float (or an int) holds to the year.
	 */
	public static void sendSpaceYears(ServerPlayer player, boolean inSpace, double trip, double left, double home) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(inSpace);
		buf.writeDouble(trip);
		buf.writeDouble(left);
		buf.writeDouble(home);
		ServerPlayNetworking.send(player, SPACE_YEARS, buf);
	}

	/** S2C obsession_ask: "Name Your Obsession" comes up, before the tribulation (the ghost path's, if {@code ghost}). */
	public static void sendObsessionAsk(ServerPlayer player, boolean ghost) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(ghost);
		ServerPlayNetworking.send(player, OBSESSION_ASK, buf);
	}

	/** Opens the talk wheel on {@code npc}: its name, title, sect, the rank it shows (-1 a mortal), regard and the intents on offer. */
	public static void sendTalkOpen(ServerPlayer player, CultivatorNpc npc, int regard, int intents) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(npc.getId());
		buf.writeUtf(npc.getPersonalName());
		buf.writeVarInt(npc.getTitle().ordinal());
		Sect sect = npc.sect();
		buf.writeUtf(sect == null ? "" : sect.name);
		buf.writeInt(npc.isMortal() ? CultivatorNpc.MORTAL
				: npc.getDisplayedRealm().ordinal() * Stage.values().length + npc.getDisplayedStage().ordinal());
		buf.writeVarInt(npc.getFaction().ordinal());
		buf.writeInt(regard);
		buf.writeVarInt(intents);
		ServerPlayNetworking.send(player, TALK_OPEN, buf);
	}

	/** Shows the dialogue box: what {@code npc} says, under {@code intent}, and the replies the player can make. */
	public static void sendTalkDialogue(ServerPlayer player, CultivatorNpc npc, int regard, TalkIntent intent, Component line,
			List<TalkSystem.Reply> replies) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(npc.getId());
		buf.writeInt(regard);
		buf.writeVarInt(intent.ordinal());
		buf.writeComponent(line);
		buf.writeVarInt(replies.size());
		for (TalkSystem.Reply reply : replies) {
			buf.writeVarInt(reply.id());
			buf.writeComponent(reply.label());
			buf.writeBoolean(reply.enabled());
			buf.writeBoolean(reply.note() != null);
			if (reply.note() != null) buf.writeComponent(reply.note());
		}
		ServerPlayNetworking.send(player, TALK_DIALOGUE, buf);
	}

	/** {@code entity} makes {@code gesture}: everyone who can see it is told (the player making it too, for third person). */
	public static void sendGesture(net.minecraft.world.entity.Entity entity, Gesture gesture) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entity.getId());
		buf.writeVarInt(gesture.ordinal());
		for (ServerPlayer watcher : PlayerLookup.tracking(entity)) ServerPlayNetworking.send(watcher, GESTURE, PacketByteBufs.copy(buf));
		if (entity instanceof ServerPlayer self) ServerPlayNetworking.send(self, GESTURE, buf);
	}

	/** The cultivator {@code entityId} says {@code line}, shown over its head for {@code ticks}. */
	public static void sendNpcSay(ServerPlayer player, int entityId, Component line, int ticks) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entityId);
		buf.writeComponent(line);
		buf.writeVarInt(ticks);
		ServerPlayNetworking.send(player, NPC_SAY, buf);
	}

	/** The player's whole journal (Journal), for the menu's Journal tab. */
	public static void sendJournal(ServerPlayer player) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		Journal.write(buf, CultivationManager.get(player).journal());
		ServerPlayNetworking.send(player, JOURNAL, buf);
	}

	/** Closes {@code player}'s talk with the cultivator {@code entityId}, saying why over the hotbar. */
	public static void sendTalkClose(ServerPlayer player, int entityId, Component reason) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entityId);
		buf.writeComponent(reason);
		ServerPlayNetworking.send(player, TALK_CLOSE, buf);
	}

	public static void sendNetherworld(ServerPlayer player, double ghostQi, double ghostQiMax, double density, float corruption,
									   int trend, int heldSeconds) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeDouble(ghostQi);
		buf.writeDouble(ghostQiMax);
		buf.writeDouble(density);
		buf.writeFloat(corruption);
		buf.writeByte(trend);
		buf.writeVarInt(Math.max(0, heldSeconds));
		ServerPlayNetworking.send(player, NETHERWORLD_SYNC, buf);
	}

	public static void sendMethods(ServerPlayer player, NetherworldState s, java.util.function.ToIntFunction<Spell> cooldownLeft) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(s.knownMethods().size());
		for (GhostMethod method : s.knownMethods()) {
			buf.writeUtf(method.getId());
			buf.writeVarInt(s.layer(method));
			buf.writeDouble(s.progress(method));
		}
		buf.writeUtf(s.activeMethod() == null ? "" : s.activeMethod().getId());
		buf.writeVarInt(s.readiedSpell() == null ? 0 : s.readiedSpell().ordinal() + 1);
		for (int i = 0; i < Spells.WHEEL_SLOTS; i++) {
			Spell spell = s.spellSlot(i);
			buf.writeVarInt(spell == null ? 0 : spell.ordinal() + 1);
		}
		for (Spell spell : Spell.values()) buf.writeVarInt(cooldownLeft.applyAsInt(spell));
		buf.writeBoolean(s.ghost()); // what a ghost can cast and switch on (Spell#isKnown, Ability#isUnlocked)
		buf.writeBoolean(s.reignedKing()); // a Ghost King's form kept, born again (TwoPaths#hasKingForm: Form Shift)
		// The heaven methods (HeavenMethods): each known, its layer and progress, and the one practised.
		HeavenState heaven = CultivationManager.get(player).heaven();
		buf.writeVarInt(heaven.known().size());
		for (HeavenMethod method : heaven.known()) {
			buf.writeUtf(method.getId());
			buf.writeVarInt(heaven.layer(method));
			buf.writeDouble(heaven.progress(method));
		}
		buf.writeUtf(heaven.activeMethod() == null ? "" : heaven.activeMethod().getId());
		// The ghost path's own bar (GhostMethods), which qi a meditation draws in, and the Netherworld's tribulation's mark.
		buf.writeVarInt(s.pathRank());
		buf.writeDouble(s.pathCultivation());
		buf.writeBoolean(s.cultivatingGhost());
		// The Spells tab's switch for the land (ModGameRules#SPELL_GRIEFING): how it stands, and whether this player may turn it.
		buf.writeBoolean(HeavenSpells.griefs(player.serverLevel()));
		buf.writeBoolean(ModGameRules.mayChange(player));
		buf.writeByte(s.lifeMode().ordinal());
		ServerPlayNetworking.send(player, METHODS, buf);
	}

	/**
	 * Tells {@code viewer} whether {@code subject} is a ghost, whether a Ghost King (GhostSystem#isKing), and how it is drawn
	 * (TwoPaths#look: a form is only a look, so the living may be drawn as a Ghost King, and the dead as a man).
	 */
	public static void sendGhost(ServerPlayer viewer, ServerPlayer subject) {
		boolean ghost = GhostSystem.isGhost(subject);
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(subject.getId());
		buf.writeBoolean(ghost);
		buf.writeBoolean(ghost && GhostSystem.isKing(subject));
		buf.writeByte(TwoPaths.look(CultivationManager.get(subject)).ordinal());
		ServerPlayNetworking.send(viewer, GHOST, buf);
	}

	/** Tells the player and everyone tracking them whether they are a ghost, and how they are drawn. */
	public static void broadcastGhost(ServerPlayer player) {
		sendGhost(player, player);
		for (ServerPlayer other : PlayerLookup.tracking(player)) sendGhost(other, player);
	}

	/** Tells everyone tracking {@code bound} (and a bound player themselves) that a talisman binds it for {@code ticks}. */
	public static void broadcastQuelled(net.minecraft.world.entity.Entity bound, int ticks) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(bound.getId());
		buf.writeVarInt(ticks);
		for (ServerPlayer viewer : PlayerLookup.tracking(bound)) ServerPlayNetworking.send(viewer, QUELLED, PacketByteBufs.copy(buf));
		if (bound instanceof ServerPlayer player) ServerPlayNetworking.send(player, QUELLED, buf);
	}

	public static void sendGhostFade(ServerPlayer player, int rgb, int closeTicks, int holdTicks, int openTicks) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeInt(rgb);
		buf.writeVarInt(closeTicks);
		buf.writeVarInt(holdTicks);
		buf.writeVarInt(openTicks);
		ServerPlayNetworking.send(player, GHOST_FADE, buf);
	}

	public static void sendJudgeGaze(ServerPlayer player, float gaze) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeFloat(gaze);
		ServerPlayNetworking.send(player, JUDGE_GAZE, buf);
	}

	public static void sendSoulRiver(ServerPlayer player, SoulRiver.Route route, int tick) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(route.ordinal());
		buf.writeVarInt(tick);
		ServerPlayNetworking.send(player, SOUL_RIVER, buf);
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
			buf.writeBoolean(formation.getSect() != null); // its sect: a guest pass of it carried lets one through (SectPassItem)
			if (formation.getSect() != null) buf.writeUUID(formation.getSect());
			// A sect's barrier lets its player disciples and its guests through (SectMembership): on the client they are among the trusted.
			java.util.Set<UUID> trusted = new java.util.LinkedHashSet<>(formation.getTrusted());
			Sect sect = formation.getSect() == null ? null : SectManager.get(to.server, formation.getSect());
			if (sect != null) {
				trusted.addAll(sect.disciples.keySet());
				trusted.addAll(SectMembership.guestsOf(sect, to.level().getGameTime()));
			}
			buf.writeVarInt(trusted.size());
			for (UUID player : trusted) buf.writeUUID(player);
		}
		ServerPlayNetworking.send(to, FORMATIONS, buf);
	}

	/** To {@code to}: the barrier of {@code formation} was struck where {@code way} (a unit vector from its centre) meets it. */
	public static void sendBarrierRipple(ServerPlayer to, Formation formation, Vec3 way, float strength) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(formation.center);
		buf.writeVarInt(formation.getRadius());
		buf.writeEnum(formation.kind);
		buf.writeFloat((float) way.x);
		buf.writeFloat((float) way.y);
		buf.writeFloat((float) way.z);
		buf.writeFloat(strength);
		ServerPlayNetworking.send(to, BARRIER_RIPPLE, buf);
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
			buf.writeBoolean(domain.own());
		}
		ServerPlayNetworking.send(to, CONSCIOUSNESS_DOMAIN, buf);
	}

	/** Tells {@code to} that {@code other}'s domain touches theirs: who, and the realm they show. */
	public static void sendConsciousnessAlert(ServerPlayer to, ServerPlayer other, PlayerCultivation otherCultivation) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(other.getId());
		buf.writeUtf(other.getDisplayName().getString()); // a soul in another's body goes by that body's name (Possession)
		buf.writeVarInt(ConsciousnessDomainHandler.sensedRealm(otherCultivation).ordinal()); // the body's, either path, or their clone's (TwoPaths#sensedRank)
		buf.writeVarInt(ConsciousnessDomainHandler.sensedStage(otherCultivation).ordinal());
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

	/** Tells the flier and everyone tracking them whether they fly fast in Meteor Flight, how hot they are, and if the way may be cleared. */
	public static void broadcastMeteorState(ServerPlayer player, boolean streaking, float heat, boolean smashes) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(player.getId());
		buf.writeBoolean(streaking);
		buf.writeByte(Math.round(Math.max(0, Math.min(1, heat)) * 255));
		buf.writeBoolean(smashes);
		ServerPlayNetworking.send(player, METEOR_STATE, new FriendlyByteBuf(buf.copy()));
		for (ServerPlayer other : PlayerLookup.tracking(player)) ServerPlayNetworking.send(other, METEOR_STATE, new FriendlyByteBuf(buf.copy()));
	}

	/** Tells the flier and everyone near that they broke the sound barrier at {@code at}, going {@code dir}. */
	public static void broadcastMeteorBoom(ServerPlayer player, Vec3 at, Vec3 dir) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(player.getId());
		buf.writeDouble(at.x);
		buf.writeDouble(at.y);
		buf.writeDouble(at.z);
		buf.writeFloat((float) dir.x);
		buf.writeFloat((float) dir.y);
		buf.writeFloat((float) dir.z);
		ServerPlayNetworking.send(player, METEOR_BOOM, new FriendlyByteBuf(buf.copy()));
		for (ServerPlayer other : PlayerLookup.around(player.serverLevel(), at, 160)) {
			if (other != player) ServerPlayNetworking.send(other, METEOR_BOOM, new FriendlyByteBuf(buf.copy()));
		}
	}

	/** Tells the rider and everyone tracking them whether they're riding their sword. */
	public static void broadcastSwordFlight(ServerPlayer player, boolean riding) {
		ServerPlayNetworking.send(player, SWORD_FLIGHT, swordFlightBuf(player, riding));
		for (ServerPlayer other : PlayerLookup.tracking(player)) {
			ServerPlayNetworking.send(other, SWORD_FLIGHT, swordFlightBuf(player, riding));
		}
	}

	private static FriendlyByteBuf swordFlightBuf(ServerPlayer player, boolean riding) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(player.getId());
		buf.writeBoolean(riding);
		return buf;
	}

	/** S2C lifespan_tribulation: its HUD line, and the client's record told of it (no Longevity Pill under it: PillItem). */
	public static void sendLifespanTribulation(ServerPlayer player, boolean active, float power, boolean paused, boolean ghost) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(active);
		buf.writeFloat(power);
		buf.writeBoolean(paused);
		buf.writeBoolean(ghost);
		ServerPlayNetworking.send(player, LIFESPAN_TRIBULATION, buf);
	}

	public static void sendTribulation(ServerPlayer player, boolean active, int strikesLeft, Realm targetRealm, Stage targetStage, boolean ghost) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(active);
		buf.writeVarInt(strikesLeft);
		buf.writeVarInt(targetRealm.ordinal());
		buf.writeVarInt(targetStage.ordinal());
		buf.writeBoolean(ghost); // the Netherworld's tribulation (the ghost path's), for the HUD's words
		ServerPlayNetworking.send(player, TRIBULATION_STATE, buf);
	}

	private static FriendlyByteBuf meditationBuf(ServerPlayer player, boolean meditating) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUUID(player.getUUID());
		buf.writeBoolean(meditating);
		buf.writeBoolean(meditating && MeditationManager.floats(player)); // sits on the air, not the ground
		return buf;
	}

	/** {@code e}'s spell auras ({@code auras}: each with the game time it ends; null or empty: none), to {@code to}. */
	public static void sendSpellAuras(ServerPlayer to, net.minecraft.world.entity.LivingEntity e, java.util.EnumMap<SpellAura, Long> auras) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(e.getId());
		long now = e.level().getGameTime();
		buf.writeVarInt(auras == null ? 0 : auras.size());
		if (auras != null) {
			for (Map.Entry<SpellAura, Long> aura : auras.entrySet()) {
				buf.writeVarInt(aura.getKey().ordinal());
				buf.writeVarInt((int) Math.max(1, aura.getValue() - now));
			}
		}
		ServerPlayNetworking.send(to, SPELL_AURAS, buf);
	}

	/** {@code owner}'s Law domain ({@code law} null: folded away) reaching {@code radius}, to {@code to}. */
	public static void sendLawDomain(ServerPlayer to, net.minecraft.world.entity.Entity owner, Law law, float radius) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(owner.getId());
		buf.writeVarInt(law == null ? 0 : law.ordinal() + 1);
		buf.writeFloat(radius);
		ServerPlayNetworking.send(to, LAW_DOMAIN, buf);
	}

	/** A spell's passing moment (SpellFx) for {@code to} to draw. */
	public static void sendSpellFx(ServerPlayer to, SpellFx.Kind kind, Vec3 at, Vec3 way, float a, float b) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(kind.ordinal());
		buf.writeDouble(at.x);
		buf.writeDouble(at.y);
		buf.writeDouble(at.z);
		buf.writeFloat((float) way.x);
		buf.writeFloat((float) way.y);
		buf.writeFloat((float) way.z);
		buf.writeFloat(a);
		buf.writeFloat(b);
		ServerPlayNetworking.send(to, SPELL_FX, buf);
	}

	/** A sound under a spell's voice (SpellSounds#play) for {@code to} to hear. */
	public static void sendSpellSound(ServerPlayer to, long voice, int handle, net.minecraft.sounds.SoundEvent sound, Vec3 at, int follow, float volume, float pitch,
			boolean loop) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarLong(voice);
		buf.writeVarInt(handle);
		buf.writeResourceLocation(sound.getLocation());
		buf.writeDouble(at.x);
		buf.writeDouble(at.y);
		buf.writeDouble(at.z);
		buf.writeVarInt(follow);
		buf.writeFloat(volume);
		buf.writeFloat(pitch);
		buf.writeBoolean(loop);
		ServerPlayNetworking.send(to, SPELL_SOUND, buf);
	}

	/** A spell's voice hushed (SpellSounds#hush) for {@code to}. */
	public static void sendSpellSoundHush(ServerPlayer to, long voice, int handle, int fade) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarLong(voice);
		buf.writeVarInt(handle);
		buf.writeVarInt(fade);
		ServerPlayNetworking.send(to, SPELL_SOUND_HUSH, buf);
	}

	private ModPackets() {}
}
