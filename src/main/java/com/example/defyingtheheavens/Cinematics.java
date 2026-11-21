package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The breakthrough cinematics' server side (the VFX plan, step B1, 2026-11-21). A survived tribulation into a realm that has a
 * cinematic (CinematicTimes) plays it from TribulationManager#succeed and its paths' twins, once the rank-up is saved: after the last
 * strike, so nothing of it ever plays during a tribulation's strikes.
 *
 * While it plays, the player is held: kept where they sat (their client holds them too, and draws them in the lotus pose), harmed
 * by nothing that can be warded off, and acting on nothing: their blows, uses and block breaks are refused here, the mod's acts they
 * send are dropped (ModPackets#unlessHeld), and their client takes their keys and mouse besides (CinematicClient#swallowKeys,
 * CinematicMouseMixin). At the blast the hostile mobs near are thrown back, unharmed. No block is ever touched: all the
 * cinematic shows is drawn by the clients. It ends when its length is up, on a skip, if the player leaves the game or the world they
 * sat in, or dies; every end lets the hold go and tells those watching.
 *
 * Who sees it: the player (the whole of it: their camera taken, the letterbox, the title) and everyone tracking them (the shapes in
 * the world only, from where they stand); anyone who starts tracking them mid-way joins at the right moment. A concealed player is
 * already out of outsiders' tracking (Concealments), so nothing here gives them away.
 *
 * Until step B2 makes the hero look right, a real breakthrough plays its cinematic only in a game started with the dev flag
 * (DevTools): everyone else keeps the old burst. `/breakthroughfx` (dev flag only) plays any cinematic without a tribulation.
 */
public final class Cinematics {
	/** FULL and SHORT: the player's camera is taken. OBSERVER: their own client shows only what others see (a dev preview). */
	public enum Version { FULL, SHORT, OBSERVER }

	/** The player's own setting (their client's CinematicSettings, sent as they join): which version they get, or none. */
	public enum Pref { FULL, SHORT, OFF }

	/**
	 * Whether the short versions are made (the plan's step B4). Until they are, every breakthrough plays its full version (the record of
	 * those seen is kept all the same, so the short ones begin where they should), and none can be asked for short.
	 */
	public static final boolean SHORT_READY = false;

	/** How far round the player hostile mobs are thrown back at the blast, and how hard. */
	private static final double BLAST_REACH = 12, BLAST_PUSH = 1.6;
	/** Ticks the server holds on past a cinematic's length (see tick). */
	static final int END_GRACE = 2;

	/** A capture's choice for a player's next breakthrough (VfxCapture): every view of a run plays the same version with the same seed. */
	private record Forced(Version version, long seed) {}

	private static final class Hold {
		final UUID player;
		final Realm realm;
		final Stage stage;
		final TwoPaths.Path path;
		final Version version;
		final long seed;
		final Vec3 at;
		final float facing;
		final long start;
		final ResourceKey<Level> world;
		final int length, blast;
		boolean blasted;
		/** Everyone told it began (the player and those who saw them), so all of them are told it ended, wherever they are by then. */
		final Set<UUID> told = new HashSet<>();

		Hold(ServerPlayer p, Realm realm, Stage stage, TwoPaths.Path path, Version version, long seed, CinematicTimes.Times times) {
			this.player = p.getUUID();
			this.realm = realm;
			this.stage = stage;
			this.path = path;
			this.version = version;
			this.seed = seed;
			this.at = p.position();
			this.facing = p.getYRot();
			this.start = p.serverLevel().getGameTime();
			this.world = p.level().dimension();
			this.length = times.length();
			this.blast = times.blast();
		}
	}

	private static final Map<UUID, Hold> HOLDS = new HashMap<>();
	private static final Map<UUID, Pref> PREFS = new HashMap<>();
	private static final Map<UUID, Forced> FORCED = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Cinematics::tick);
		// Held: harmed by nothing that can be warded off (the void and /kill still can: they are not the cinematic's to stop).
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(entity instanceof ServerPlayer p && HOLDS.containsKey(p.getUUID())) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY));
		// Held, they act on nothing either: no blow struck, nothing used, no block broken or placed (their client takes their controls, but
		// isn't trusted with it). The mod's own acts (casts, abilities, strikes) are refused where they arrive (ModPackets#unlessHeld).
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> held(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> held(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> held(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> held(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseItemCallback.EVENT.register((player, world, hand) ->
				held(player) ? InteractionResultHolder.fail(player.getItemInHand(hand)) : InteractionResultHolder.pass(ItemStack.EMPTY));
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> !held(player));
		// Whoever starts seeing a held player joins the cinematic where it is.
		EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
			Hold h = tracked instanceof ServerPlayer ? HOLDS.get(tracked.getUUID()) : null;
			if (h != null) sendStart(viewer, (ServerPlayer) tracked, h);
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
				end(player.server, player.getUUID(), "left the world it sat in"));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer p) end(p.server, p.getUUID(), "died");
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			if (server.isSameThread()) leave(server, id);
			else server.execute(() -> leave(server, id));
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			HOLDS.clear();
			PREFS.clear();
			FORCED.clear();
		});
		ServerPlayNetworking.registerGlobalReceiver(ModPackets.CINEMATIC_SKIP, (server, player, handler, buf, sender) ->
				server.execute(() -> end(server, player.getUUID(), "skipped")));
		ServerPlayNetworking.registerGlobalReceiver(ModPackets.CINEMATIC_PREF, (server, player, handler, buf, sender) -> {
			int i = buf.readVarInt();
			server.execute(() -> PREFS.put(player.getUUID(), Pref.values()[Math.max(0, Math.min(Pref.values().length - 1, i))]));
		});
		if (DevTools.ENABLED) CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(Commands
				.literal("breakthroughfx")
				.requires(src -> src.hasPermission(2))
				.then(Commands.literal("stop").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					if (!HOLDS.containsKey(p.getUUID())) {
						ctx.getSource().sendFailure(Component.literal("No cinematic is playing for you."));
						return 0;
					}
					end(p.server, p.getUUID(), "stopped by /breakthroughfx stop");
					return 1;
				}))
				.then(Commands.argument("spec", StringArgumentType.greedyString()).executes(ctx ->
						play(ctx.getSource(), StringArgumentType.getString(ctx, "spec"))))));
	}

	/**
	 * A tribulation survived: its cinematic, where there is one. @return true when one plays (the caller then skips the old burst of
	 * sounds and particles; the chat line, the healing and the milestones stay as they are)
	 */
	public static boolean onBreakthrough(ServerPlayer p, Realm realm, Stage stage, TwoPaths.Path path) {
		if (!DevTools.ENABLED) return false; // step B1: the grey box plays only in a game started with the dev flag (see the class note)
		Forced forced = FORCED.remove(p.getUUID()); // a capture's choice is for this breakthrough alone, whether it plays or not
		if (versionFor(realm, stage, Pref.FULL, false) == null) return false;
		if (forced != null) {
			start(p, realm, stage, path, forced.version(), forced.seed());
			return true;
		}
		Version version = versionFor(realm, stage, PREFS.getOrDefault(p.getUUID(), Pref.FULL), CultivationManager.own(p).hasSeenCinematic(key(path, realm)));
		if (version == null) return false;
		start(p, realm, stage, path, version, p.serverLevel().getRandom().nextLong());
		return true;
	}

	/**
	 * Which version a breakthrough into {@code realm}'s {@code stage} plays, or null for none (the old burst): a realm's first stage only
	 * (a new stage within one is a stage flourish, step B4), a realm with a cinematic, a player who hasn't turned them off. The first time
	 * into a realm by a path the full version; after that, or when the player asked for it, the short one (once made: SHORT_READY).
	 */
	static Version versionFor(Realm realm, Stage stage, Pref pref, boolean seen) {
		if (stage != Stage.EARLY || CinematicTimes.of(realm) == null || pref == Pref.OFF) return null;
		return !SHORT_READY || pref == Pref.FULL && !seen ? Version.FULL : Version.SHORT;
	}

	/**
	 * {@code player}'s next breakthrough into a realm with a cinematic plays {@code version} with {@code seed}, whatever their setting
	 * and whatever they have seen (VfxCapture: every view of a run the same); a null version forgets it.
	 */
	public static void forceNext(UUID player, Version version, long seed) {
		if (version == null) FORCED.remove(player);
		else FORCED.put(player, new Forced(version, seed));
	}

	/** When {@code p}'s cinematic began (game time), or Long.MIN_VALUE when none plays. */
	public static long startedAt(ServerPlayer p) {
		Hold h = HOLDS.get(p.getUUID());
		return h == null ? Long.MIN_VALUE : h.start;
	}

	/** The cinematic into {@code realm} begins for {@code p} (any earlier one of theirs ends first). */
	public static void start(ServerPlayer p, Realm realm, Stage stage, TwoPaths.Path path, Version version, long seed) {
		CinematicTimes.Entry entry = CinematicTimes.of(realm);
		if (entry == null) return;
		if (HOLDS.containsKey(p.getUUID())) end(p.server, p.getUUID(), "a new one began");
		Hold h = new Hold(p, realm, stage, path, version, seed, entry.of(version != Version.SHORT));
		HOLDS.put(p.getUUID(), h);
		p.stopUsingItem(); // a bow drawn, food half eaten: let go of, not loosed (nothing is done while held)
		if (version == Version.FULL) {
			CultivationManager.own(p).markCinematicSeen(key(path, realm));
			CultivationManager.markDirty(p.server);
		}
		p.setDeltaMovement(Vec3.ZERO);
		sendStart(p, p, h);
		for (ServerPlayer viewer : PlayerLookup.tracking(p)) if (viewer != p) sendStart(viewer, p, h);
		DefyingTheHeavens.LOGGER.info("[DTH-CINEMATIC] {} into {} ({}, {}, seed {}) begins for {}", key(path, realm), realm.getId(), version.name().toLowerCase(Locale.ROOT),
				h.length + " ticks", seed, p.getGameProfile().getName());
	}

	/** Whether {@code p} is held in a cinematic now. */
	public static boolean isPlaying(ServerPlayer p) {
		return HOLDS.containsKey(p.getUUID());
	}

	/** Whether {@code player} is a server's player held in a cinematic (a client's own copy of the player is its own client's to guard). */
	public static boolean held(Player player) {
		return player instanceof ServerPlayer && HOLDS.containsKey(player.getUUID());
	}

	/** The key a cinematic is remembered as seen by: the path and the realm reached ("heaven:heavenly_being"). */
	static String key(TwoPaths.Path path, Realm realm) {
		return path.name().toLowerCase(Locale.ROOT) + ":" + realm.getId();
	}

	private static void tick(MinecraftServer server) {
		if (HOLDS.isEmpty()) return;
		for (Hold h : new ArrayList<>(HOLDS.values())) {
			ServerPlayer p = server.getPlayerList().getPlayer(h.player);
			if (p == null) {
				leave(server, h.player);
				continue;
			}
			ServerLevel level = p.serverLevel();
			if (level.dimension() != h.world) {
				end(server, h.player, "left the world it sat in");
				continue;
			}
			long age = level.getGameTime() - h.start;
			if (!h.blasted && age >= h.blast) {
				h.blasted = true;
				throwBack(level, h.at);
			}
			// The server's own clock ends it, whatever the client did or didn't (no client can keep it held): END_GRACE ticks after its length,
			// so the player's client, a tick or so behind, has finished handing back by itself before the server's word arrives.
			if (age >= h.length + END_GRACE) {
				end(server, h.player, null);
				continue;
			}
			// Held where they sat: anything that moved them (a push, a blast's knockback the client didn't undo) is undone.
			if (p.position().distanceToSqr(h.at) > 0.36) p.teleportTo(level, h.at.x, h.at.y, h.at.z, p.getYRot(), p.getXRot());
			p.setDeltaMovement(Vec3.ZERO);
			p.fallDistance = 0;
		}
	}

	/** The blast: the hostile mobs within reach of {@code at} thrown back from it, unharmed (no block is touched). @return those thrown */
	static List<Mob> throwBack(ServerLevel level, Vec3 at) {
		List<Mob> mobs = level.getEntitiesOfClass(Mob.class, new AABB(at, at).inflate(BLAST_REACH),
				m -> m instanceof Enemy && m.isAlive() && m.distanceToSqr(at) <= BLAST_REACH * BLAST_REACH);
		for (Mob m : mobs) {
			Vec3 away = m.position().subtract(at).multiply(1, 0, 1);
			away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
			m.setDeltaMovement(away.x * BLAST_PUSH, 0.5, away.z * BLAST_PUSH);
			m.hurtMarked = true;
		}
		return mobs;
	}

	/** A held player who left the game: the hold let go, and those who saw them told; their setting is sent again as they join. */
	private static void leave(MinecraftServer server, UUID id) {
		PREFS.remove(id);
		end(server, id, "left the game");
	}

	/** The cinematic of player {@code id} ends ({@code why}: why it ended early, or null when its length was up). */
	public static void end(MinecraftServer server, UUID id, String why) {
		Hold h = HOLDS.remove(id);
		if (h == null) return;
		// Everyone told it began, and anyone seeing the player now: a player taken to another world is seen there by others, and those
		// who watched them sit would otherwise go on drawing it where they sat until its length was up.
		Set<UUID> tell = new HashSet<>(h.told);
		ServerPlayer p = server.getPlayerList().getPlayer(id);
		if (p != null) {
			tell.add(id);
			for (ServerPlayer viewer : PlayerLookup.tracking(p)) tell.add(viewer.getUUID());
		}
		for (UUID each : tell) {
			ServerPlayer to = server.getPlayerList().getPlayer(each);
			if (to != null) sendEnd(to, id);
		}
		DefyingTheHeavens.LOGGER.info("[DTH-CINEMATIC] {} into {} for {} ends{}", key(h.path, h.realm), h.realm.getId(), id, why == null ? "" : ": " + why);
	}

	private static void sendStart(ServerPlayer to, ServerPlayer subject, Hold h) {
		h.told.add(to.getUUID());
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(subject.getId());
		buf.writeUUID(h.player);
		buf.writeVarInt(h.realm.ordinal());
		buf.writeVarInt(h.stage.ordinal());
		buf.writeVarInt(h.path.ordinal());
		buf.writeVarInt(h.version.ordinal());
		buf.writeLong(h.seed);
		buf.writeDouble(h.at.x);
		buf.writeDouble(h.at.y);
		buf.writeDouble(h.at.z);
		buf.writeFloat(h.facing);
		buf.writeLong(h.start);
		buf.writeVarInt(h.length);
		ServerPlayNetworking.send(to, ModPackets.CINEMATIC_START, buf);
	}

	private static void sendEnd(ServerPlayer to, UUID id) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUUID(id);
		ServerPlayNetworking.send(to, ModPackets.CINEMATIC_END, buf);
	}

	/** `/breakthroughfx <realm> [stage] [path] [seed] [full|short|observer]`: a cinematic without a tribulation (dev flag only). */
	private static int play(CommandSourceStack source, String text) {
		try {
			ServerPlayer p = source.getPlayerOrException();
			String[] t = text.trim().toLowerCase(Locale.ROOT).split("\\s+");
			Realm realm = VfxCapture.realm(t[0]);
			Stage stage = Stage.EARLY;
			TwoPaths.Path path = TwoPaths.Path.HEAVEN;
			Version version = Version.FULL;
			long seed = p.serverLevel().getRandom().nextLong();
			for (int i = 1; i < t.length; i++) {
				String w = t[i];
				switch (w) {
					case "full" -> version = Version.FULL;
					case "short" -> {
						if (!SHORT_READY) throw new IllegalArgumentException("the short versions come in step B4");
						version = Version.SHORT;
					}
					case "observer" -> version = Version.OBSERVER;
					case "early", "mid", "late", "gp" -> stage = VfxCapture.stage(w);
					case "heaven", "ghost", "demon", "body" -> path = VfxCapture.path(w);
					default -> {
						if (!w.matches("-?\\d+")) throw new IllegalArgumentException("unknown word '" + w + "'");
						seed = Long.parseLong(w);
					}
				}
			}
			if (CinematicTimes.of(realm) == null) throw new IllegalArgumentException("no cinematic for " + realm.getId() + " yet (only heavenly_being)");
			start(p, realm, stage, path, version, seed);
			Version shown = version;
			source.sendSuccess(() -> Component.literal("Playing the cinematic into " + realm.getId() + " (" + shown.name().toLowerCase(Locale.ROOT) + ")."), false);
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal("breakthroughfx: " + e.getMessage()));
			return 0;
		}
	}

	private Cinematics() {}
}
