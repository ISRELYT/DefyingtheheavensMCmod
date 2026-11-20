package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Players' souls in puppets (puppet phase 2d; the user, 2026-11-17: "I also want to be able to turn other players (or even myself) into a
 * puppet ... soul in the puppet, can't control it, can only watch ... grows a fake soul and pushes the real one out ... fight back if your
 * understanding of puppets is good enough or your cultivation is high enough ... puppeteer can 'release' or 'control' any of its puppets at
 * any time"; the merged design's rules, 2026-11-17).
 * <ul>
 *   <li><b>In</b>: slain by a puppeteer or one of their puppets within PuppetSouls#CATCH_RADIUS of one of the puppeteer's puppets with an
 *   empty ring ({@link #onDeath}: the gamerule puppetSoulBinding; a devilish deed); or willingly, one's own hollow puppet entered from its
 *   Command Seal page ({@link #enter}), or a puppeteer's offer from theirs ({@link #offer}) accepted on the soul screen ({@link #accept});
 *   or a nascent soul out of its body going into a puppet ({@link #projectInto}: its own free, another's held, a weaker soul in it driven
 *   out). The body dies as any body does, and at the respawn the soul is in the puppet.</li>
 *   <li><b>Its own screen</b> (the user's word, 2026-11-18: "rather than commands they should be buttons or things in menus"): opened by
 *   itself as the soul comes into its puppet, then by its inventory key while it is held, and from the menu's first tab while it plays the
 *   puppet ({@link #sendPage}, {@link #act}): let go, struggle, leave, its board, and an offer's accept or decline. The menu's first tab is
 *   the puppet's board itself: a free soul's to paint, with Leave it and its puppets there; a held one's to look at, its captor's command
 *   line lit, with Struggle and Let go there (PuppetBoards; the user, 2026-11-18: "a non-free puppet should see circuits too so they can
 *   takeover/struggle free").</li>
 *   <li><b>Held</b>: under its puppeteer's control the player watches through the puppet's eyes, a spectator held to it ({@link #mayView}),
 *   chat their only voice; the puppet's circuits copy the soul (PuppetEntity#underControl, {@link #heldIn}: its puppeteer in its world, the
 *   player held, logged out or not): whole after PuppetSouls#COPY_TICKS, the real soul pushed out straight to the Netherworld
 *   ({@link #goOn}), the copy the puppet's own with their name and look (PuppetEntity#soulLook). Released, the copy fades at {@link #FADE}
 *   of its pace.</li>
 *   <li><b>Playing it</b> ({@link #plays}): released by its puppeteer (the Command Seal's page), its puppeteer away, or free. The player rides
 *   inside it and steers it as a soul steers a body it possesses (SoulProjection#possessorOf), strikes with its blows ({@link #strike}) and
 *   looses its art by the cast key ({@link #art}); otherwise it lives as the living do (the user, 2026-11-18: "as a free puppet you should
 *   be able to live like normal"): uses blocks, items and beings, digs, takes up and drops things (Incorporeal#touchesNothing), but
 *   rides nothing else and never sleeps. It is seen by no one (SoulProjection#broadcastsTo), nothing strikes it but through its puppet
 *   (Incorporeal#untouchable), and it neither hungers nor ages; its own cultivation lies dormant (CultivationManager#get answers as
 *   Possession#sealed).</li>
 *   <li><b>Struggle</b> ({@link #struggle}: Puppetry at least the puppeteer's, or one's own realm above the puppet's): the line to its heart
 *   cut from within, a cell at a time (PuppetEntity#struggleTick); the heart cut off, free for good.</li>
 *   <li><b>Free</b> ({@link #sever}: severed by its puppeteer, struggled loose, its puppeteer slain): the puppet the player's own.</li>
 *   <li><b>Out</b> ({@link #goOn}): let go, pushed out by the copy, the puppet wrecked, taken apart or its soul let go at the bench, or left
 *   ({@link #leave}): the soul goes on; the death is its body's already (ModDamageTypes#SOUL_WENT_ON: no body, no lingering).</li>
 * </ul>
 */
public final class PuppetPlayers {
	/** A struggle cuts a cell of the line to the heart every this many ticks, halved for each rank of Puppetry above its puppeteer's or realm
	 * above the puppet's; a warded cell (a ward knot's or beside one) takes this many times as long. */
	public static final int STRUGGLE_TICKS = 600, WARDED_TIMES = 4;
	/** Seizing a free soul-puppet: the seal held on it this long. */
	public static final int FREE_SEIZE_TICKS = 600;
	/** How near one stands to one's own hollow puppet to enter it, or to offer it. */
	public static final double NEAR = 8;
	/** An offer stands this many ticks. */
	public static final int OFFER_TICKS = 20 * 60;
	/** Released, the copy fades at this share of its pace. */
	public static final double FADE = 0.5;
	/** A puppet nowhere to be found for this many seconds (and none to say where it went): the soul goes on. */
	private static final int LOST_SECONDS = 60;

	private static final String PUPPET = "Puppet", MODE = "Mode", OWNER = "Owner", PASSING = "Passing", DUE = "Due", WHY = "Why", SEVER = "Sever";

	/** The acts of C2S puppet_soul_act, from the soul's own screen (the client's PuppetSoulScreen; the user's word, 2026-11-18: "rather than
	 * commands they should be buttons or things in menus"). */
	public static final int OPEN = 0, LET_GO = 1, STRUGGLE = 2, LEAVE = 3, BOARD = 4, ACCEPT = 5, DECLINE = 6, PUPPETS = 7;
	/** A bound one's screen sent again this often while it is bound (ticks): how far its copy has come, its struggle. */
	private static final int PAGE_EVERY = 100;

	/** Who plays which puppet now (player, puppet). */
	private static final Map<UUID, UUID> PILOTS = new HashMap<>();
	/** Offers standing (the one offered: whose puppet, which, until when). */
	private static final Map<UUID, Offer> OFFERS = new HashMap<>();
	/** Seconds each bound one's puppet has been nowhere to be found. */
	private static final Map<UUID, Integer> MISSING = new HashMap<>();
	/** Bound ones shown their soul's screen since they came into the world (it opens once by itself, then with their inventory key). */
	private static final java.util.Set<UUID> SHOWN = new java.util.HashSet<>();
	/** Set while this moves a view or a body itself (mayView lets it). */
	private static boolean moving;
	/** Set by the client: whether a player (its own) plays a puppet now (ModPackets#PUPPET_PILOT). */
	public static Predicate<Player> clientPilot = player -> false;

	private record Offer(UUID master, UUID puppet, long until) {}

	private static Puppetry.Ledger ledger(MinecraftServer server) {
		return Puppetry.Ledger.get(server);
	}

	private static @Nullable CompoundTag binding(MinecraftServer server, UUID player) {
		return ledger(server).boundSouls.get(player);
	}

	// --- Who is where ---

	/** Whether {@code player}'s soul is in a puppet (held or playing it), not on its way out. */
	public static boolean isBound(ServerPlayer player) {
		CompoundTag b = binding(player.server, player.getUUID());
		return b != null && !b.getBoolean(PASSING);
	}

	/** Whether {@code player} watches through a puppet holding their soul, its puppeteer's to steer (not playing it). */
	public static boolean isHeld(ServerPlayer player) {
		return isBound(player) && !PILOTS.containsKey(player.getUUID());
	}

	/** Whether {@code player} plays a puppet holding their soul now (both sides: the client is told). */
	public static boolean isPilot(@Nullable Player player) {
		if (player == null) return false;
		if (player.level().isClientSide) return clientPilot.test(player);
		return PILOTS.containsKey(player.getUUID());
	}

	/** The one playing {@code entity} (a puppet), both sides; null if none does. */
	public static @Nullable Player pilotOf(@Nullable Entity entity) {
		return entity instanceof PuppetEntity && entity.getFirstPassenger() instanceof Player player && isPilot(player) ? player : null;
	}

	/** The puppet {@code player} plays now, or null. */
	public static @Nullable PuppetEntity played(ServerPlayer player) {
		return PILOTS.containsKey(player.getUUID()) && player.getVehicle() instanceof PuppetEntity puppet ? puppet : null;
	}

	/**
	 * Whether the soul in {@code puppet} is a player's held in it now, not playing it: its copy grows then (PuppetEntity#underControl). Logged
	 * out, it is held all the same (the design: "Logging out doesn't end it ... The bar keeps filling while the puppet is controlled"), unless
	 * its puppeteer released it.
	 */
	public static boolean heldIn(PuppetEntity puppet) {
		PuppetSoul soul = puppet.soul();
		UUID id = soul == null || !soul.real() ? null : soul.player();
		if (id == null || !(puppet.level() instanceof ServerLevel level) || id.equals(puppet.getOwnerUUID())) return false;
		ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
		if (p == null) return !puppet.released();
		return p.isAlive() && isBound(p) && !PILOTS.containsKey(id);
	}

	/** Whether {@code player} paints {@code puppet}'s lines as their own body's, with their own qi: no brush (PuppetBoards#paint). */
	public static boolean paintsOwnBody(ServerPlayer player, PuppetEntity puppet) {
		return played(player) == puppet && puppet.isOwnedBy(player);
	}

	/**
	 * Whether {@code player}'s own soul is held in {@code puppet}, another's (watching through it or playing it): its board theirs to look at,
	 * never to paint (PuppetBoards; the user, 2026-11-18: "a non-free puppet should see circuits too so they can takeover/struggle free").
	 */
	public static boolean heldWithin(ServerPlayer player, PuppetEntity puppet) {
		if (puppet.isOwnedBy(player) || !isBound(player)) return false;
		CompoundTag b = binding(player.server, player.getUUID());
		PuppetSoul soul = puppet.soul();
		return b != null && b.hasUUID(PUPPET) && puppet.getUUID().equals(b.getUUID(PUPPET)) && soul != null && player.getUUID().equals(soul.player())
				&& soul.real();
	}

	/**
	 * A held soul's look at its puppet's board (PuppetBoards#send, after the page): its captor, how far the copy of it has come, whether it may
	 * struggle (else the puppet's realm, for why not), whether it struggles, the command line it struggles against (each cell's seconds from
	 * within, the nearest the core first) and how far the next cell is cut, and whether it plays the puppet.
	 */
	static void writeCaptive(FriendlyByteBuf buf, ServerPlayer p, PuppetEntity puppet) {
		PuppetSoul soul = puppet.soul();
		int halvings = halvings(p, puppet);
		buf.writeUtf(puppet.ownerName());
		buf.writeVarInt(soul == null ? 0 : soul.copiedPercent());
		buf.writeBoolean(mayStruggle(p, puppet));
		buf.writeBoolean(puppet.struggling());
		buf.writeComponent(CultivatorNpc.realmOf(puppet.rank()).getDisplayName());
		int[] line = puppet.commandLine(), seconds = new int[line.length];
		for (int k = 0; k < line.length; k++) seconds[k] = Math.max(1, puppet.struggleDue(line[k], halvings) / 20);
		buf.writeVarIntArray(line);
		buf.writeVarIntArray(seconds);
		buf.writeVarInt(puppet.struggleProgress(halvings));
		buf.writeBoolean(PILOTS.containsKey(p.getUUID()));
	}

	/**
	 * Whether {@code player} may look out through {@code camera} (SoulServerPlayerMixin): one held in a puppet through it alone, until let
	 * go (this class moving the view itself is let through).
	 */
	public static boolean mayView(ServerPlayer player, Entity camera) {
		if (moving || PILOTS.containsKey(player.getUUID()) || !isBound(player)) return true;
		CompoundTag b = binding(player.server, player.getUUID());
		return b != null && camera.getUUID().equals(b.getUUID(PUPPET));
	}

	/** The puppet {@code id}, in whatever world it is loaded; null if none. */
	static @Nullable PuppetEntity find(MinecraftServer server, @Nullable UUID id) {
		if (id == null) return null;
		for (ServerLevel level : server.getAllLevels()) if (level.getEntity(id) instanceof PuppetEntity puppet && puppet.isAlive()) return puppet;
		return null;
	}

	// --- In ---

	/** {@code player}'s soul into {@code puppet}'s empty ring (their body dies now, or has just died). */
	static void bind(ServerPlayer player, PuppetEntity puppet) {
		puppet.bindSoul(new PuppetSoul(PuppetSoul.Kind.PLAYER, player.getGameProfile().getName(), player.getUUID().toString(), null,
				Math.max(0, TwoPaths.higherRank(CultivationManager.own(player), false)), List.of()));
		CompoundTag b = new CompoundTag();
		b.putUUID(PUPPET, puppet.getUUID());
		GameType mode = player.gameMode.getGameModeForPlayer();
		b.putInt(MODE, (mode == GameType.SPECTATOR ? GameType.SURVIVAL : mode).getId());
		if (puppet.getOwnerUUID() != null) b.putUUID(OWNER, puppet.getOwnerUUID());
		Puppetry.Ledger ledger = ledger(player.server);
		ledger.boundSouls.put(player.getUUID(), b);
		ledger.setDirty();
		if (puppet.level() instanceof ServerLevel level) {
			Vec3 from = player.position().add(0, 1.2, 0), step = puppet.position().add(0, puppet.getBbHeight() * 0.6, 0).subtract(from);
			for (int k = 0; k <= 8; k++) {
				Vec3 at = from.add(step.scale(k / 8.0));
				level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.01);
			}
			level.playSound(null, puppet.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.7f);
		}
	}

	/**
	 * AFTER_DEATH (PuppetSouls#onDeath): {@code victim} slain by a puppeteer or their puppet near one of the puppeteer's puppets with an empty
	 * ring: their soul drawn into it. Not with the gamerule off, never one held already (a willing body's death), a ghost, a soul out of its
	 * body, one in a clone or in another's body, nor a death that destroys the soul.
	 */
	static void onDeath(ServerPlayer victim, DamageSource source) {
		if (binding(victim.server, victim.getUUID()) != null) return;
		if (!victim.serverLevel().getGameRules().getBoolean(ModGameRules.PUPPET_SOUL_BINDING)) return;
		if (victim.isSpectator() || GhostSystem.isGhost(victim) || Clones.inClone(victim) || SoulProjection.isSoul(victim)
				|| Possession.inhabits(victim.getUUID()) || Possession.isHeld(victim.getUUID())) return;
		boolean byHeaven = TribulationManager.isActive(victim.getUUID()) || FiveDecays.isStriking(victim);
		if (Lingering.destroysTheSoul(source, byHeaven)) return;
		ServerPlayer slayer = PuppetSouls.slayer(source);
		if (slayer == null || slayer == victim) return;
		PuppetEntity ring = PuppetSouls.nearestEmpty(victim.serverLevel(), slayer, victim.position());
		if (ring == null) return;
		bind(victim, ring);
		PlayerCultivation c = CultivationManager.own(slayer);
		c.setAlignment(c.getAlignment() - PuppetSouls.BIND_SHIFT);
		CultivationManager.markDirty(slayer.server);
		slayer.sendSystemMessage(Component.translatable(ModLang.SOUL_BOUND_PLAYER, victim.getName(), ring.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
	}

	/** AFTER_RESPAWN (first of all there): a soul gone on has its game back (the dead's way after); one bound at its death wakes in its puppet. */
	public static void onRespawn(ServerPlayer p, boolean alive) {
		if (alive) return;
		CompoundTag b = binding(p.server, p.getUUID());
		if (b == null) return;
		if (b.getBoolean(PASSING)) {
			done(p, b);
			return;
		}
		// In its puppet from now on: held to it on the next tick (or playing it), its screen opened then.
		p.setGameMode(GameType.SPECTATOR);
		SHOWN.remove(p.getUUID());
	}

	/** A player back in the world (CultivationEvents#arrived): one whose soul went on as the server stopped has its game back; one bound is shown its screen. */
	public static void arrived(ServerPlayer p) {
		CompoundTag b = binding(p.server, p.getUUID());
		if (b == null) return;
		if (b.getBoolean(PASSING)) {
			if (p.isAlive()) done(p, b);
			return;
		}
		SHOWN.remove(p.getUUID());
	}

	/** The binding ended: {@code p}'s game as it was before, their view and their body their own. */
	private static void done(ServerPlayer p, CompoundTag b) {
		Puppetry.Ledger ledger = ledger(p.server);
		ledger.boundSouls.remove(p.getUUID());
		ledger.setDirty();
		PILOTS.remove(p.getUUID());
		moving = true;
		try {
			if (p.getCamera() != p) p.setCamera(p);
			p.setGameMode(GameType.byId(b.getInt(MODE)));
			p.setInvisible(false);
		} finally {
			moving = false;
		}
		sendPilot(p, false);
		SHOWN.remove(p.getUUID());
		sendPage(p, false); // its screen and its hint gone
		CultivationManager.refresh(p);
	}

	// --- Its own screen (the client's PuppetSoulScreen) ---

	/**
	 * S2C puppet_soul_page: how {@code p}'s soul stands (none, held, playing it released, playing it with its puppeteer away, free), its
	 * puppet, its puppeteer, its copy, its struggle; and an offer standing for it. {@code open}: the screen opened with it (else only its HUD
	 * hint and an open screen are brought up to date).
	 */
	static void sendPage(ServerPlayer p, boolean open) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(open);
		CompoundTag b = binding(p.server, p.getUUID());
		PuppetEntity puppet = b == null || b.getBoolean(PASSING) ? null : find(p.server, b.getUUID(PUPPET));
		PuppetSoul soul = puppet == null ? null : puppet.soul();
		boolean theirs = soul != null && p.getUUID().equals(soul.player()) && soul.real();
		int state = !theirs ? 0 : puppet.isOwnedBy(p) ? 4 : PILOTS.containsKey(p.getUUID()) ? (puppet.released() ? 2 : 3) : 1;
		buf.writeVarInt(state);
		if (state > 0) {
			buf.writeComponent(puppet.getDisplayName());
			buf.writeUtf(puppet.ownerName());
			buf.writeVarInt(soul.copiedPercent());
			buf.writeBoolean(state != 4 && mayStruggle(p, puppet));
			buf.writeBoolean(puppet.struggling());
			buf.writeVarInt(Math.max(1, (STRUGGLE_TICKS >> Math.min(8, halvings(p, puppet))) / 20));
			buf.writeComponent(CultivatorNpc.realmOf(puppet.rank()).getDisplayName());
		}
		Offer o = OFFERS.get(p.getUUID());
		long now = p.server.overworld().getGameTime();
		PuppetEntity offered = o == null || o.until() < now ? null : find(p.server, o.puppet());
		buf.writeBoolean(offered != null);
		if (offered != null) {
			buf.writeUtf(offered.ownerName());
			buf.writeComponent(offered.getDisplayName());
			buf.writeVarInt((int) Math.max(1, (o.until() - now) / 20));
		}
		ServerPlayNetworking.send(p, ModPackets.PUPPET_SOUL_PAGE, buf);
	}

	/** C2S puppet_soul_act: {@code act} pressed on {@code p}'s soul screen (OPEN: their inventory key, the screen asked for); the screen sent again. */
	public static void act(ServerPlayer p, int act) {
		switch (act) {
			case LET_GO -> {
				if (boundPuppet(p) != null) goOn(p, ModLang.PUPPET_SOUL_LET_GO);
			}
			case STRUGGLE -> struggle(p);
			case LEAVE -> leave(p);
			case BOARD -> board(p);
			case ACCEPT -> accept(p);
			case DECLINE -> decline(p);
			case PUPPETS -> {
				// A free soul keeps its Puppetry and runs its puppets (the design): their list, though it holds no Command Seal in its puppet.
				PuppetEntity own = played(p);
				if (own != null && own.isOwnedBy(p)) PuppetCommands.open(p, null);
			}
			default -> {}
		}
		if (act != BOARD && act != PUPPETS && p.isAlive()) sendPage(p, act == OPEN);
		// Struggle pressed on its board (a held soul's Circuits): the board shows it begun.
		if (act == STRUGGLE && p.isAlive()) PuppetBoards.refresh(p);
	}

	// --- Every tick ---

	/** Each tick: every bound one in the world kept by its puppet, held to its view or riding it as it plays it; each second, the rest. */
	public static void tick(MinecraftServer server) {
		Puppetry.Ledger ledger = ledger(server);
		if (ledger.boundSouls.isEmpty()) {
			PILOTS.clear();
			return;
		}
		boolean second = server.getTickCount() % 20 == 0;
		for (Map.Entry<UUID, CompoundTag> e : new ArrayList<>(ledger.boundSouls.entrySet())) {
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			CompoundTag b = e.getValue();
			if (p == null || !p.isAlive() || b.getBoolean(PASSING)) continue;
			if (b.getBoolean(DUE)) {
				goOn(p, b.contains(WHY) ? b.getString(WHY) : null);
				continue;
			}
			PuppetEntity puppet = find(server, b.getUUID(PUPPET));
			if (puppet == null) {
				if (second) seek(p, b);
				continue;
			}
			MISSING.remove(p.getUUID());
			PuppetSoul soul = puppet.soul();
			// Its copy whole, by whatever way it came whole: pushed out, the puppet wearing its name and look.
			if (soul != null && p.getUUID().equals(soul.player()) && soul.whole()) {
				copiedWhole(puppet, soul);
				continue;
			}
			if (soul == null || !p.getUUID().equals(soul.player())) {
				goOn(p, ModLang.PUPPET_SOUL_GONE_OUT);
				continue;
			}
			if (puppet.getOwnerUUID() != null && (!b.hasUUID(OWNER) || !puppet.getOwnerUUID().equals(b.getUUID(OWNER)))) {
				b.putUUID(OWNER, puppet.getOwnerUUID());
				ledger.setDirty();
			}
			// Its puppeteer slain while the puppet lay out of reach: free now it is found.
			if (b.getBoolean(SEVER)) {
				b.remove(SEVER);
				ledger.setDirty();
				if (!puppet.isOwnedBy(p)) sever(puppet, p, ModLang.PUPPET_SOUL_MASTER_FELL);
			}
			if (plays(puppet, p)) keepPlaying(p, puppet);
			else keepHeld(p, puppet);
			// Its screen opened once by itself as it comes into its puppet (then its inventory key opens it), kept up to date after.
			if (SHOWN.add(p.getUUID())) sendPage(p, true);
			else if (server.getTickCount() % PAGE_EVERY == 0) sendPage(p, false);
			if (second) second(p, puppet, soul);
		}
	}

	/** Whether {@code p} plays {@code puppet} now: it is theirs (free), its puppeteer released it, or its puppeteer is away (or in another world). */
	static boolean plays(PuppetEntity puppet, ServerPlayer p) {
		if (puppet.isOwnedBy(p) || puppet.released()) return true;
		ServerPlayer owner = puppet.getOwnerUUID() == null ? null : p.server.getPlayerList().getPlayer(puppet.getOwnerUUID());
		return owner == null || owner.level() != puppet.level();
	}

	/** Held: a spectator looking out through it, carried into its world. */
	private static void keepHeld(ServerPlayer p, PuppetEntity puppet) {
		if (PILOTS.containsKey(p.getUUID())) stopPlaying(p);
		moving = true;
		try {
			if (p.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) p.setGameMode(GameType.SPECTATOR);
			if (p.level() != puppet.level()) p.teleportTo((ServerLevel) puppet.level(), puppet.getX(), puppet.getY(), puppet.getZ(), p.getYRot(), p.getXRot());
			if (p.getCamera() != puppet) p.setCamera(puppet);
		} finally {
			moving = false;
		}
	}

	/** Playing it: riding inside it, unseen, its own client steering it. */
	private static void keepPlaying(ServerPlayer p, PuppetEntity puppet) {
		if (p.getVehicle() == puppet && PILOTS.containsKey(p.getUUID())) return;
		boolean first = !PILOTS.containsKey(p.getUUID());
		moving = true;
		try {
			if (p.getCamera() != p) p.setCamera(p);
			if (p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) p.setGameMode(GameType.SURVIVAL);
			p.stopRiding();
			if (p.level() != puppet.level()) p.teleportTo((ServerLevel) puppet.level(), puppet.getX(), puppet.getY(), puppet.getZ(), puppet.getYRot(), 0);
			else p.connection.teleport(puppet.getX(), puppet.getY(), puppet.getZ(), puppet.getYRot(), 0);
			p.setInvisible(true);
			PILOTS.put(p.getUUID(), puppet.getUUID());
			sendPilot(p, true); // first, so its client takes the reins as it sits in
			p.startRiding(puppet, true);
			SoulProjection.refreshTracking(p);
		} finally {
			moving = false;
		}
		if (first) {
			// Its art by the cast key, everything else as ever, its circuits (or its soul's screen) in the menu's first tab: the keys as its
			// own client binds them.
			p.sendSystemMessage(Component.translatable(puppet.isOwnedBy(p) ? ModLang.PUPPET_SOUL_PLAYS_OWN : ModLang.PUPPET_SOUL_PLAYS, puppet.getDisplayName(),
					Component.keybind(ModLang.KEY_CAST_SPELL), Component.keybind(ModLang.KEY_MENU)).withStyle(ChatFormatting.AQUA));
			if (SHOWN.contains(p.getUUID())) sendPage(p, false);
			// Its board open before it (struggled free while it looked on): its own body's now, painted with its own qi.
			PuppetBoards.refresh(p);
		}
	}

	/** No longer playing it: off it, seen again (by those who may). */
	private static void stopPlaying(ServerPlayer p) {
		if (PILOTS.remove(p.getUUID()) == null) return;
		moving = true;
		try {
			p.stopRiding();
			p.setInvisible(false);
		} finally {
			moving = false;
		}
		sendPilot(p, false);
		SoulProjection.refreshTracking(p);
		if (SHOWN.contains(p.getUUID()) && isBound(p)) sendPage(p, false);
	}

	private static void sendPilot(ServerPlayer p, boolean on) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(on);
		ServerPlayNetworking.send(p, ModPackets.PUPPET_PILOT, buf);
	}

	/** Its puppet not loaded anywhere: taken where its puppeteer's ledger last saw it (loading it); nowhere to be found long, the soul goes on. */
	private static void seek(ServerPlayer p, CompoundTag b) {
		int missing = MISSING.merge(p.getUUID(), 1, Integer::sum);
		Puppetry.Entry entry = b.hasUUID(OWNER) ? ledger(p.server).get(b.getUUID(OWNER), b.getUUID(PUPPET)) : null;
		if (entry != null && !entry.away()) {
			ServerLevel level = p.server.getLevel(entry.dimension());
			if (level != null && (p.level() != level || p.blockPosition().distSqr(entry.pos()) > 16 * 16)) {
				moving = true;
				try {
					if (PILOTS.containsKey(p.getUUID())) stopPlaying(p);
					if (p.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) p.setGameMode(GameType.SPECTATOR);
					p.teleportTo(level, entry.pos().getX() + 0.5, entry.pos().getY() + 1, entry.pos().getZ() + 0.5, p.getYRot(), p.getXRot());
				} finally {
					moving = false;
				}
			}
			if (missing < LOST_SECONDS) return;
		}
		if (entry != null && entry.away()) return; // out of the world a while (on an errand): it comes back
		if (missing >= LOST_SECONDS / 2) goOn(p, ModLang.PUPPET_SOUL_GONE_OUT);
	}

	/** A second of a bound one: playing, it hungers not and a released one's copy fades; its struggle goes on; held, reminded now and then. */
	private static void second(ServerPlayer p, PuppetEntity puppet, PuppetSoul soul) {
		boolean playing = PILOTS.containsKey(p.getUUID());
		if (playing) {
			p.getFoodData().setFoodLevel(20);
			if (!puppet.isOwnedBy(p)) soul.uncopy((int) Math.round(20 * FADE));
		}
		boolean captive = !puppet.isOwnedBy(p);
		if (puppet.struggling() && captive && puppet.struggleTick(20, halvings(p, puppet))) {
			sever(puppet, p, ModLang.PUPPET_SOUL_STRUGGLED_FREE);
			PuppetBoards.refresh(p); // its board, open before it, its own body's now
			return;
		}
		// Its board open before it (its Circuits): the command line as it stands, the struggle cutting its way, each second.
		if (captive) PuppetBoards.refresh(p);
	}

	// --- Out ---

	/**
	 * {@code p}'s soul out of its puppet and on (let go, pushed out, the puppet wrecked or left): the ring let go of it (a whole copy stays,
	 * the puppet's own), and it dies, its body dead already: a spectator's death, no body left. Pushed out by its copy it goes straight down
	 * to the Netherworld (ModDamageTypes#SOUL_WENT_ON); any other way, as any death goes (SOUL_LEFT_PUPPET: a nascent soul lingers there to
	 * take another body by the ghosts' rules, Lingering; the rest down to the Ghost Gate, the gamerule netherworldAfterDeath on). Its game
	 * back at the respawn. {@code why} told first (null: nothing).
	 */
	public static void goOn(ServerPlayer p, @Nullable String why) {
		CompoundTag b = binding(p.server, p.getUUID());
		if (b == null || b.getBoolean(PASSING)) return;
		PuppetEntity puppet = find(p.server, b.getUUID(PUPPET));
		PuppetSoul soul = puppet == null ? null : puppet.soul();
		if (soul != null && p.getUUID().equals(soul.player()) && soul.real()) puppet.bindSoul(null);
		stopPlaying(p);
		b.putBoolean(PASSING, true);
		b.remove(DUE);
		ledger(p.server).setDirty();
		MISSING.remove(p.getUUID());
		moving = true;
		try {
			if (p.getCamera() != p) p.setCamera(p);
			p.setGameMode(GameType.SPECTATOR); // its body died already: nothing to leave
		} finally {
			moving = false;
		}
		if (why != null) p.sendSystemMessage(Component.translatable(why, puppet == null ? Component.translatable(ModLang.PUPPET_SOUL_A_PUPPET)
				: puppet.getDisplayName()).withStyle(ChatFormatting.GRAY));
		// Pushed out by its copy: down to the Netherworld at once (the design); any other way out: a death as any goes, a nascent soul
		// lingering where it was to take another body by the ghosts' rules.
		p.hurt(ModDamageTypes.soulWentOn(p.level(), ModLang.PUPPET_SOUL_PUSHED_OUT.equals(why)), Float.MAX_VALUE);
		if (p.isAlive()) done(p, b); // nothing let it die: its game back as it was
	}

	/** {@code player}'s soul to go on: now if they are in the world, else as they come back. */
	public static void goOn(MinecraftServer server, UUID player, @Nullable String why) {
		ServerPlayer p = server.getPlayerList().getPlayer(player);
		if (p != null && p.isAlive()) {
			goOn(p, why);
			return;
		}
		CompoundTag b = binding(server, player);
		if (b == null || b.getBoolean(PASSING)) return;
		b.putBoolean(DUE, true);
		if (why != null) b.putString(WHY, why);
		ledger(server).setDirty();
	}

	/** A player's soul in {@code puppet} lost with it (wrecked, taken apart, its soul let go at the bench): it goes on. */
	public static void lostWith(PuppetEntity puppet, String why) {
		PuppetSoul soul = puppet.soul();
		UUID id = soul == null || !soul.real() ? null : soul.player();
		if (id != null && puppet.level() instanceof ServerLevel level) goOn(level.getServer(), id, why);
	}

	/** The copy of a player's soul in {@code puppet} come whole: the real soul pushed out, on. */
	public static void copiedWhole(PuppetEntity puppet, PuppetSoul soul) {
		puppet.soulChanged(); // it wears their name and look from now on
		UUID id = soul.player();
		if (id != null && puppet.level() instanceof ServerLevel level) goOn(level.getServer(), id, ModLang.PUPPET_SOUL_PUSHED_OUT);
	}

	/** {@code puppet}'s soul moved into {@code into}'s ring at a bench (PuppetSouls#move): a player's binding follows it. */
	public static void movedTo(PuppetEntity into, PuppetSoul soul) {
		UUID id = soul.player();
		if (id == null || !(into.level() instanceof ServerLevel level)) return;
		CompoundTag b = binding(level.getServer(), id);
		if (b == null) return;
		b.putUUID(PUPPET, into.getUUID());
		ledger(level.getServer()).setDirty();
		ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
		if (p != null) stopPlaying(p);
	}

	/** A puppeteer slain: every soul they held free for good (now, or as its puppet is found). */
	public static void masterDied(ServerPlayer master) {
		Puppetry.Ledger ledger = ledger(master.server);
		for (Map.Entry<UUID, CompoundTag> e : new ArrayList<>(ledger.boundSouls.entrySet())) {
			CompoundTag b = e.getValue();
			// (One's own puppet entered: its body's death is its own coming in, no puppeteer slain.)
			if (e.getKey().equals(master.getUUID())) continue;
			if (b.getBoolean(PASSING) || !b.hasUUID(OWNER) || !b.getUUID(OWNER).equals(master.getUUID())) continue;
			PuppetEntity puppet = find(master.server, b.getUUID(PUPPET));
			ServerPlayer p = master.server.getPlayerList().getPlayer(e.getKey());
			if (puppet != null && p != null) sever(puppet, p, ModLang.PUPPET_SOUL_MASTER_FELL);
			else {
				b.putBoolean(SEVER, true);
				ledger.setDirty();
			}
		}
	}

	/** {@code puppet} {@code p}'s own for good: free of its puppeteer. Its puppeteer told {@code why} (null: the plain word). */
	static void sever(PuppetEntity puppet, ServerPlayer p, @Nullable String why) {
		ServerPlayer was = puppet.getOwnerUUID() == null ? null : p.server.getPlayerList().getPlayer(puppet.getOwnerUUID());
		Component name = puppet.getDisplayName();
		puppet.seizedBy(p, ArtifactGrade.MORTAL);
		puppet.setReleased(false);
		puppet.stopStruggle();
		p.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_NOW_FREE, name).withStyle(ChatFormatting.GOLD));
		if (SHOWN.contains(p.getUUID())) sendPage(p, false);
		if (was != null && was != p) {
			was.sendSystemMessage(Component.translatable(why != null ? why : ModLang.PUPPET_SOUL_SEVERED, name, p.getName()).withStyle(ChatFormatting.GOLD));
		}
		if (puppet.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, puppet.getX(), puppet.getY(0.8), puppet.getZ(), 20, 0.3, 0.4, 0.3, 0.03);
			level.playSound(null, puppet.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
		}
	}

	/** Its held soul's struggles halved this many times: its Puppetry's ranks above its puppeteer's, or its own realms above the puppet's. */
	static int halvings(ServerPlayer p, PuppetEntity puppet) {
		PlayerCultivation own = CultivationManager.own(p);
		int master = puppet.getOwnerUUID() == null ? CraftMastery.Rank.ADEPT.ordinal()
				: Puppetry.rank(CultivationManager.own(p.server, puppet.getOwnerUUID())).ordinal();
		int ranks = Puppetry.rank(own).ordinal() - master;
		int mine = TwoPaths.higherRank(own, false);
		int realms = mine < 0 ? Integer.MIN_VALUE : CultivatorNpc.realmOf(mine).ordinal() - CultivatorNpc.realmOf(puppet.rank()).ordinal();
		return Math.max(0, Math.max(ranks, realms));
	}

	/** Whether {@code p} may struggle in {@code puppet}: Puppetry at least its puppeteer's, or their own realm above the puppet's. */
	static boolean mayStruggle(ServerPlayer p, PuppetEntity puppet) {
		PlayerCultivation own = CultivationManager.own(p);
		int master = puppet.getOwnerUUID() == null ? CraftMastery.Rank.ADEPT.ordinal()
				: Puppetry.rank(CultivationManager.own(p.server, puppet.getOwnerUUID())).ordinal();
		if (Puppetry.rank(own).ordinal() >= master) return true;
		int mine = TwoPaths.higherRank(own, false);
		return mine >= 0 && CultivatorNpc.realmOf(mine).ordinal() > CultivatorNpc.realmOf(puppet.rank()).ordinal();
	}

	// --- Playing it ---

	/** {@code p}, playing a puppet, strikes {@code target} (its blow, as it can: its reach, its wait). */
	public static void strike(ServerPlayer p, Entity target) {
		PuppetEntity puppet = played(p);
		if (puppet != null && target != puppet) puppet.pilotStrike(target);
	}

	/** {@code p}, playing a puppet, looses its art where they look (C2S puppet_pilot_art). */
	public static void art(ServerPlayer p) {
		PuppetEntity puppet = played(p);
		if (puppet != null) puppet.pilotArt(p, p.getLookAngle());
	}

	/** Whether {@code puppet} of its puppeteer may be released, or controlled again (the Command Seal's page): it holds a player's soul. */
	public static boolean holdsPlayer(PuppetEntity puppet) {
		PuppetSoul soul = puppet.soul();
		return soul != null && soul.kind == PuppetSoul.Kind.PLAYER && soul.real();
	}

	// --- Its puppeteer's word (the Command Seal's page: PuppetCommands) ---

	/** Released to play it, or controlled again; what to tell its puppeteer. */
	static Component release(ServerPlayer owner, PuppetEntity puppet, boolean released) {
		if (!holdsPlayer(puppet)) return Component.translatable(ModLang.PUPPET_SOUL_NONE).withStyle(ChatFormatting.GOLD);
		puppet.setReleased(released);
		ServerPlayer held = owner.server.getPlayerList().getPlayer(puppet.soul().player());
		if (held != null) held.sendSystemMessage(Component.translatable(released ? ModLang.PUPPET_SOUL_RELEASED_TOLD : ModLang.PUPPET_SOUL_CONTROLLED_TOLD,
				owner.getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		return Component.translatable(released ? ModLang.PUPPET_SOUL_RELEASED : ModLang.PUPPET_SOUL_CONTROLLED, puppet.getDisplayName(), puppet.soul().name);
	}

	/** Severed by its puppeteer: free for good; what to tell them. */
	static Component severByOwner(ServerPlayer owner, PuppetEntity puppet) {
		if (!holdsPlayer(puppet)) return Component.translatable(ModLang.PUPPET_SOUL_NONE).withStyle(ChatFormatting.GOLD);
		ServerPlayer held = owner.server.getPlayerList().getPlayer(puppet.soul().player());
		String who = puppet.soul().name;
		if (held == null) return Component.translatable(ModLang.PUPPET_SOUL_AWAY, who).withStyle(ChatFormatting.GOLD);
		sever(puppet, held, ModLang.PUPPET_SOUL_SEVERED);
		return Component.translatable(ModLang.PUPPET_SOUL_SEVERED_DONE, who);
	}

	// --- Willingly in ---

	/** Whether {@code puppet} is {@code p}'s own, hollow, beside them (within {@link #NEAR}): one they may enter, or offer. */
	private static boolean ownHollowBeside(ServerPlayer p, PuppetEntity puppet) {
		return puppet.isAlive() && puppet.isOwnedBy(p) && puppet.soul() == null && puppet.level() == p.level() && puppet.distanceTo(p) <= NEAR;
	}

	/** Whether {@code p} may give up their body to a puppet: alive in it, their own, not out of it or in another's. */
	private static @Nullable String mayGiveBody(ServerPlayer p) {
		if (binding(p.server, p.getUUID()) != null) return ModLang.PUPPET_SOUL_ALREADY;
		if (!p.isAlive() || p.isSpectator() || GhostSystem.isGhost(p) || Clones.inClone(p) || SoulProjection.isActive(p.getUUID())
				|| Possession.inhabits(p.getUUID()) || Possession.isHeld(p.getUUID())) return ModLang.PUPPET_SOUL_NOT_NOW;
		return null;
	}

	/**
	 * Enter it (its Command Seal page, pressed twice): {@code p}'s own hollow puppet beside them entered: their body dies (what they carry
	 * falls with it), their soul in it, free. What to tell them (null: done).
	 */
	static @Nullable Component enter(ServerPlayer p, PuppetEntity puppet) {
		String not = mayGiveBody(p);
		if (not != null) return Component.translatable(not).withStyle(ChatFormatting.RED);
		if (!ownHollowBeside(p, puppet)) return Component.translatable(ModLang.PUPPET_SOUL_ENTER_NONE, (int) NEAR).withStyle(ChatFormatting.RED);
		bind(p, puppet);
		p.hurt(ModDamageTypes.soulIntoPuppet(p.level()), Float.MAX_VALUE);
		return null;
	}

	/**
	 * Offer to <the player beside one> (its Command Seal page): {@code master}'s hollow puppet beside them offered to {@code target}, beside
	 * them too, who may accept it a minute (their soul screen opens on it). What to tell the puppeteer.
	 */
	static Component offer(ServerPlayer master, PuppetEntity puppet, @Nullable ServerPlayer target) {
		if (!ownHollowBeside(master, puppet)) return Component.translatable(ModLang.PUPPET_SOUL_ENTER_NONE, (int) NEAR).withStyle(ChatFormatting.RED);
		if (target == null || target == master || target.level() != master.level() || target.distanceTo(master) > NEAR) {
			return Component.translatable(ModLang.PUPPET_SOUL_OFFER_NONE, (int) NEAR).withStyle(ChatFormatting.RED);
		}
		if (binding(target.server, target.getUUID()) != null) return Component.translatable(ModLang.PUPPET_SOUL_TAKEN, target.getName()).withStyle(ChatFormatting.RED);
		OFFERS.put(target.getUUID(), new Offer(master.getUUID(), puppet.getUUID(), master.server.overworld().getGameTime() + OFFER_TICKS));
		target.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_OFFERED, master.getName(), puppet.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		sendPage(target, true);
		return Component.translatable(ModLang.PUPPET_SOUL_OFFER_SENT, target.getName(), puppet.getDisplayName());
	}

	/**
	 * A nascent soul out of its body goes into {@code puppet} for good (SoulProjection#tryPossess, the soul right-clicking it; the user,
	 * 2026-11-18: "can someone posses a puppet with nascent soul projection? if its hollow they should be able to (if its their own puppet they
	 * enter it like how they would when pressing the button, if its not, they become a slave puppet to the master and would have to struggle
	 * free, if there is a soul already in there, they can push it out if its weaker than them and if the soul is stronger than them they cant
	 * posses it)"). A soul in it already is driven out if it stands below the highest of {@code p}'s paths (a player's own soul only where
	 * players may fight: it goes on, as from a wrecked puppet); one as strong or stronger holds it. Then {@code p}'s soul goes home at once
	 * and their body dies there (what they carry falls with it), their soul in the puppet: free if it is theirs (as its Command Seal's Enter
	 * it), else held, its puppeteer's to steer until they struggle free. Only a player's puppet: an NPC puppeteer's seal holds its own shut.
	 * Null if it went in, else why not (a lang key; its one argument the puppet's name).
	 */
	public static @Nullable String projectInto(ServerPlayer p, PuppetEntity puppet) {
		if (binding(p.server, p.getUUID()) != null) return ModLang.PUPPET_SOUL_ALREADY;
		if (!puppet.isAlive() || GhostSystem.isGhost(p) || Clones.inClone(p) || Possession.inhabits(p.getUUID())) return ModLang.MSG_POSSESS_CANNOT;
		if (puppet.getOwnerUUID() == null || !puppet.ownedByPlayer()) return ModLang.MSG_POSSESS_PUPPET_NPC;
		// (Someone riding it who doesn't play it: no room in it.)
		if (puppet.isVehicle() && pilotOf(puppet) == null) return ModLang.MSG_POSSESS_CANNOT;
		PuppetSoul soul = puppet.soul();
		int own = Math.max(0, TwoPaths.higherRank(CultivationManager.own(p), true));
		UUID player = soul != null && soul.kind == PuppetSoul.Kind.PLAYER && soul.real() ? soul.player() : null;
		if (soul != null) {
			if (soul.rank >= own) return ModLang.MSG_POSSESS_PUPPET_STRONGER;
			if (player != null && !p.server.isPvpAllowed()) return ModLang.MSG_POSSESS_PVP;
		}
		ServerPlayer owner = puppet.isOwnedBy(p) ? null : p.server.getPlayerList().getPlayer(puppet.getOwnerUUID());
		Component name = puppet.getDisplayName();
		// Whoever was in it driven out first: a player's soul goes on (now, or as they come back), any other is gone.
		if (soul != null) {
			String was = soul.goesBy();
			puppet.bindSoul(null);
			if (player != null) goOn(p.server, player, ModLang.PUPPET_SOUL_DRIVEN_OUT);
			if (puppet.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.SOUL, puppet.getX(), puppet.getY(0.7), puppet.getZ(), 16, 0.3, 0.4, 0.3, 0.05);
				level.playSound(null, puppet.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2f, 0.5f);
			}
			p.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_DROVE_OUT, was, name).withStyle(ChatFormatting.DARK_PURPLE));
			if (owner != null) owner.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_DROVE_OUT_TOLD, p.getName(), was, name).withStyle(ChatFormatting.RED));
		}
		// Home at once (where its body sits: there it dies), then into the ring.
		SoulProjection.bringHome(p);
		bind(p, puppet);
		boolean free = puppet.isOwnedBy(p);
		p.sendSystemMessage(free ? Component.translatable(ModLang.PUPPET_SOUL_PROJECTED_OWN, name).withStyle(ChatFormatting.AQUA)
				: Component.translatable(ModLang.PUPPET_SOUL_PROJECTED_HELD, name, puppet.ownerName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		if (owner != null) owner.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_ENTERED_TOLD, p.getName(), name).withStyle(ChatFormatting.DARK_PURPLE));
		p.hurt(ModDamageTypes.soulIntoPuppet(p.level()), Float.MAX_VALUE);
		return null;
	}

	/** Decline (the soul screen): the offer standing for {@code p} turned down, its puppeteer told. */
	static void decline(ServerPlayer p) {
		Offer o = OFFERS.remove(p.getUUID());
		ServerPlayer master = o == null ? null : p.server.getPlayerList().getPlayer(o.master());
		if (master != null) master.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_DECLINED, p.getName()).withStyle(ChatFormatting.GRAY));
	}

	/** Accept (the soul screen): the offer standing taken: {@code p}'s body dies, their soul held in the puppet offered. */
	static int accept(ServerPlayer p) {
		Offer o = OFFERS.remove(p.getUUID());
		PuppetEntity puppet = o == null || o.until() < p.server.overworld().getGameTime() ? null : find(p.server, o.puppet());
		if (puppet == null || puppet.soul() != null || puppet.getOwnerUUID() == null || !puppet.getOwnerUUID().equals(o.master())) {
			p.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_NO_OFFER).withStyle(ChatFormatting.RED));
			return 0;
		}
		String not = mayGiveBody(p);
		if (not != null) {
			p.sendSystemMessage(Component.translatable(not).withStyle(ChatFormatting.RED));
			return 0;
		}
		ServerPlayer master = p.server.getPlayerList().getPlayer(o.master());
		if (master != null) master.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_ACCEPTED, p.getName(), puppet.getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE));
		bind(p, puppet);
		p.hurt(ModDamageTypes.soulIntoPuppet(p.level()), Float.MAX_VALUE);
		return 1;
	}

	// --- Its own ways out ---

	private static @Nullable PuppetEntity boundPuppet(ServerPlayer p) {
		CompoundTag b = binding(p.server, p.getUUID());
		return b == null || b.getBoolean(PASSING) ? null : find(p.server, b.getUUID(PUPPET));
	}

	/** Struggle (the soul screen): a held soul fights its puppeteer's hold, if it may. */
	static int struggle(ServerPlayer p) {
		PuppetEntity puppet = boundPuppet(p);
		if (puppet == null) {
			p.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_NOT_BOUND).withStyle(ChatFormatting.RED));
			return 0;
		}
		if (puppet.isOwnedBy(p)) {
			p.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_FREE_ALREADY).withStyle(ChatFormatting.GRAY));
			return 0;
		}
		if (!mayStruggle(p, puppet)) {
			p.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_CANT_STRUGGLE, CultivatorNpc.realmOf(puppet.rank()).getDisplayName()).withStyle(ChatFormatting.RED));
			return 0;
		}
		puppet.struggle();
		int seconds = Math.max(1, (STRUGGLE_TICKS >> Math.min(8, halvings(p, puppet))) / 20);
		p.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_STRUGGLING, seconds).withStyle(ChatFormatting.GOLD));
		ServerPlayer owner = puppet.getOwnerUUID() == null ? null : p.server.getPlayerList().getPlayer(puppet.getOwnerUUID());
		if (owner != null) owner.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_STRUGGLE_TOLD, p.getName(), puppet.getDisplayName()).withStyle(ChatFormatting.RED));
		return 1;
	}

	/** Leave it (the soul screen): a free soul out of its puppet (hollow, still its own) and on. */
	static int leave(ServerPlayer p) {
		PuppetEntity puppet = boundPuppet(p);
		if (puppet == null || !puppet.isOwnedBy(p)) {
			p.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_NOT_FREE).withStyle(ChatFormatting.RED));
			return 0;
		}
		goOn(p, ModLang.PUPPET_SOUL_LEFT);
		return 1;
	}

	/** Its board (the soul screen): a free soul's own puppet's board, to paint with its own qi. */
	static int board(ServerPlayer p) {
		// Its own body's board, a free soul's to paint; another's, a held soul's to look at and struggle against (the user, 2026-11-18: "a
		// non-free puppet should see circuits too so they can takeover/struggle free").
		PuppetEntity puppet = boundPuppet(p);
		if (puppet == null || !puppet.isOwnedBy(p) && !heldWithin(p, puppet)) {
			p.sendSystemMessage(Component.translatable(ModLang.PUPPET_SOUL_NOT_BOUND).withStyle(ChatFormatting.RED));
			return 0;
		}
		PuppetBoards.openPuppet(p, puppet);
		return 1;
	}

	// --- Hooks ---

	/** Its hooks: each tick's keeping; a player gone from the server let go of here (their binding waits for them). */
	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(PuppetPlayers::tick);
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			server.execute(() -> {
				PILOTS.remove(id);
				OFFERS.remove(id);
				MISSING.remove(id);
				SHOWN.remove(id);
			});
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PILOTS.clear();
			OFFERS.clear();
			MISSING.clear();
			SHOWN.clear();
		});
	}

	private PuppetPlayers() {}
}
