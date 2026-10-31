package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Universe in a Sleeve (Technique#SLEEVE, the user's ask of 2026-10-26: "i want to add ascending while carrying other players (like lower
 * realm players and stuff) as another technique like how clone is one"; its rules the user's of that evening and of 2026-10-27): other
 * players carried in one's sleeve, wherever one goes.
 * <ul>
 *   <li><b>Taken in.</b> Ability#SLEEVE used on a player one looks at within {@link #REACH}, both out in a world (not on the way through
 *   one of the passages between: ModDimensions#isPassage), offers them a place ({@link #offer}); they accept or refuse in the chat within
 *   {@link #OFFER_TICKS} (SleeveCommand). Accepted, they vanish into the sleeve: watching through their bearer's eyes (a spectator held
 *   there, as Possession holds a body's owner: SoulServerPlayerMixin, ServerGamePacketListenerImplMixin), touched by nothing. At most
 *   {@link #MOST} at once.</li>
 *   <li><b>Carried.</b> Wherever their bearer goes they go too, up through the Spatial Gap, the Void Breach and the Immortal Gate, whatever
 *   their realm (the ways' floors and the Immortal Realm's are their bearer's to meet, not theirs), and the moment their bearer comes out
 *   into a world they are let out beside them.</li>
 *   <li><b>The cost.</b> The bearer pays: every spatial pressure on them is {@link #pressureFactor} times, twice for each one carried (one
 *   x2, two x4, three x8, four x16: the user's rule); a True Immortal, untouched at the Immortal Gate on its own, takes
 *   {@link #GATE_BASE_BODIES} of its body over the climb there while it carries anyone, doubled likewise (ImmortalGate). Those in one's
 *   small world count as those in one's sleeve do ({@link #borne}; SmallWorlds carries them up).</li>
 *   <li><b>Let out.</b> Sneaking, the ability lets everyone out beside the bearer, anywhere but partway through the Breach's or the Gate's
 *   climb (in the gap, out into it). One carried may step out themselves while their bearer is out in a world. Their bearer slain, they
 *   are cast out where it fell (the user's pick): in the gap, into its pressure; partway through a climb, back to its foot; anywhere
 *   else, where it lay. Their bearer gone from the game, they are set down where they were taken in.</li>
 * </ul>
 * Kept with the Overworld's data ({@link Data}): who is in whose sleeve, how each played before (their game mode), where each was
 * taken in; so a logout or a crash on either side is set right.
 */
public final class Sleeves {
	/** How near one must stand to be offered a place, and how many a sleeve holds. */
	public static final double REACH = 6;
	public static final int MOST = 4;
	/** An offer stands this long (ticks). */
	static final int OFFER_TICKS = 30 * 20;
	/**
	 * A True Immortal carrying anyone through the Immortal Gate's passage, which on its own presses on it not at all: this much of its body
	 * over the climb (ImmortalGate#CLIMB_SECONDS), doubled for each one carried as every pressure is (the user's pick: a quarter).
	 */
	public static final double GATE_BASE_BODIES = 0.25;

	private record Offer(UUID bearer, long until) {}

	/** Offers standing: the one offered, whose sleeve, until when (game time). */
	private static final Map<UUID, Offer> OFFERS = new HashMap<>();
	/** Each bearer's world last stood in (not a passage), and where: the foot of the way it is on now. */
	private static final Map<UUID, Place> LAST_WORLD = new HashMap<>();
	/** Each bearer's world at the last tick: coming out into another, those carried are let out. */
	private static final Map<UUID, ResourceKey<Level>> LAST_DIMENSION = new HashMap<>();
	/** Bearers told of their load's cost on this way through (forgotten once out in a world). */
	private static final Set<UUID> TOLD_COST = new HashSet<>();
	/** Those carried come back into the game, to be set right once they stand in it (moving them while they join breaks the login). */
	private static final Set<UUID> REJOINED = new HashSet<>();
	/** The view held or handed back by this class itself (SoulServerPlayerMixin lets it). */
	private static boolean moving;

	/** A place in a world: which, and where. */
	record Place(ResourceKey<Level> level, Vec3 at) {
		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Level", level.location().toString());
			tag.putDouble("X", at.x);
			tag.putDouble("Y", at.y);
			tag.putDouble("Z", at.z);
			return tag;
		}

		static Place load(CompoundTag tag) {
			ResourceLocation id = ResourceLocation.tryParse(tag.getString("Level"));
			if (id == null) return null;
			return new Place(ResourceKey.create(Registries.DIMENSION, id), new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")));
		}

		static Place of(Entity e) {
			return new Place(e.level().dimension(), e.position());
		}
	}

	/** How far beside its bearer one let out stands, and how far round it the next one is (degrees). */
	private static final double BESIDE = 1.3, ROUND = 60;

	/**
	 * A step beside {@code bearer} for the {@code i}-th one set down there (at its right hand first, then on round it), where a body fits;
	 * else where it stands (not inside it, as the in-game check of 2026-10-27 found them: the view in its robe).
	 */
	static Place beside(ServerPlayer bearer, int i) {
		ServerLevel level = bearer.serverLevel();
		for (int k = 0; k < 6; k++) {
			Vec3 step = Vec3.directionFromRotation(0, bearer.getYRot() + 90 + (float) (ROUND * (i + k))).scale(BESIDE);
			if (level.noCollision(bearer.getBoundingBox().move(step))) return new Place(level.dimension(), bearer.position().add(step));
		}
		return Place.of(bearer);
	}

	/** One in a sleeve: whose, their game mode before, where taken in; and, let out while they were away, where they come out. */
	static final class Carried {
		final UUID bearer;
		final String bearerName;
		final int mode;
		final Place from;
		Place outAt;
		boolean castIntoGap, gapFromUpper;

		Carried(UUID bearer, String bearerName, int mode, Place from) {
			this.bearer = bearer;
			this.bearerName = bearerName;
			this.mode = mode;
			this.from = from;
		}
	}

	// --- Who is where ---

	/** In someone's sleeve now (not let out). */
	public static boolean isCarried(UUID id) {
		Data data = Data.current();
		Carried c = data == null ? null : data.carried.get(id);
		return c != null && c.outAt == null;
	}

	/** How many {@code bearer} carries now (those away from the game too: they are in the sleeve still). */
	public static int carrying(UUID bearer) {
		Data data = Data.current();
		if (data == null) return 0;
		int n = 0;
		for (Carried c : data.carried.values()) if (c.outAt == null && c.bearer.equals(bearer)) n++;
		return n;
	}

	/** Those {@code bearer} carries who are in the game now. */
	static List<ServerPlayer> carried(ServerPlayer bearer) {
		List<ServerPlayer> out = new ArrayList<>();
		Data data = Data.get(bearer.server);
		for (Map.Entry<UUID, Carried> e : data.carried.entrySet()) {
			if (e.getValue().outAt != null || !e.getValue().bearer.equals(bearer.getUUID())) continue;
			ServerPlayer p = bearer.server.getPlayerList().getPlayer(e.getKey());
			if (p != null) out.add(p);
		}
		return out;
	}

	/**
	 * How many {@code p} bears up the ways now: those in its sleeve, and those in its small world (SmallWorlds#inside: anyone with a world
	 * carries those in it as a sleeve does, the user's ask of 2026-10-27).
	 */
	public static int borne(ServerPlayer p) {
		return carrying(p.getUUID()) + SmallWorlds.inside(p);
	}

	/** What every spatial pressure on {@code p} is multiplied by: two for each one it bears ({@link #borne}; none, once). */
	public static float pressureFactor(ServerPlayer p) {
		return 1 << Math.min(30, borne(p)); // (a world holds any number: past thirty, the shift would overflow; long dead by then)
	}

	/** Whether {@code viewer}'s view may be set to {@code camera} (SoulServerPlayerMixin): one in a sleeve sees through its bearer's eyes alone. */
	public static boolean mayView(ServerPlayer viewer, Entity camera) {
		if (moving || !isCarried(viewer.getUUID())) return true;
		Carried c = Data.get(viewer.server).carried.get(viewer.getUUID());
		return camera != null && camera.getUUID().equals(c.bearer);
	}

	// --- Taking in ---

	/**
	 * Ability#SLEEVE used: sneaking, everyone carried let out beside {@code bearer}; else a place offered to the player looked at within
	 * {@link #REACH}, or what the sleeve holds told.
	 */
	public static void act(ServerPlayer bearer) {
		if (!bearer.isAlive() || bearer.isSpectator()) return;
		if (Clones.inClone(bearer)) {
			bearer.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_CANNOT), true);
			return;
		}
		// A secret realm's space is sealed: no sleeve opens in it (SecretRealms).
		if (ModDimensions.isSecretRealm(bearer.level().dimension())) {
			bearer.displayClientMessage(Component.translatable(ModLang.MSG_SECRET_SEALED_SPACE), true);
			return;
		}
		if (bearer.isShiftKeyDown()) {
			if (carrying(bearer.getUUID()) == 0) {
				bearer.displayClientMessage(Component.translatable(ModLang.MSG_SLEEVE_EMPTY), true);
				return;
			}
			if (midClimb(bearer)) {
				bearer.displayClientMessage(Component.translatable(ModLang.MSG_SLEEVE_MID_CLIMB), true);
				return;
			}
			letAllOut(bearer, ModLang.MSG_SLEEVE_OUT);
			bearer.displayClientMessage(Component.translatable(ModLang.MSG_SLEEVE_LET_OUT).withStyle(ChatFormatting.LIGHT_PURPLE), true);
			return;
		}
		ServerPlayer target = lookedAt(bearer);
		if (target == null) {
			bearer.displayClientMessage(Component.translatable(ModLang.MSG_SLEEVE_HOW, Math.round(REACH), carrying(bearer.getUUID()), MOST), true);
			return;
		}
		offer(bearer, target);
	}

	/** The player {@code bearer} looks at within {@link #REACH}, nearest first, or null. */
	private static ServerPlayer lookedAt(ServerPlayer bearer) {
		Vec3 eye = bearer.getEyePosition(), look = bearer.getLookAngle();
		ServerPlayer best = null;
		double bestDistance = Double.MAX_VALUE;
		for (ServerPlayer other : bearer.serverLevel().players()) {
			if (other == bearer || other.isSpectator() || !other.isAlive()) continue;
			Vec3 to = other.position().add(0, other.getBbHeight() * 0.5, 0).subtract(eye);
			double d = to.length();
			if (d > REACH + 0.5 || d < 1.0e-3 || to.normalize().dot(look) < 0.94) continue;
			if (d < bestDistance) {
				best = other;
				bestDistance = d;
			}
		}
		return best;
	}

	/** Why {@code target} can't go into {@code bearer}'s sleeve now, or null if they can. */
	private static String whyNot(ServerPlayer bearer, ServerPlayer target) {
		if (ModDimensions.isPassage(bearer.level().dimension())) return ModLang.MSG_SLEEVE_NOT_ON_THE_WAY;
		if (carrying(bearer.getUUID()) >= MOST) return ModLang.MSG_SLEEVE_FULL;
		if (target.level() != bearer.level() || target.distanceTo(bearer) > REACH * 2 || !target.isAlive() || target.isSpectator()
				|| isCarried(target.getUUID()) || carrying(target.getUUID()) > 0 || ModDimensions.isPassage(target.level().dimension())
				|| SoulProjection.isAwayFromBody(target.getUUID()) || Possession.isHeld(target.getUUID()) || Possession.inhabits(target.getUUID())
				|| Clones.inClone(target) || TribulationManager.isActive(target.getUUID()) || InnerRealm.isInside(target)) {
			return ModLang.MSG_SLEEVE_CANT_TAKE;
		}
		return null;
	}

	/** {@code bearer} offers {@code target} a place in their sleeve: told, with a word to accept or refuse it in the chat. */
	static void offer(ServerPlayer bearer, ServerPlayer target) {
		String why = whyNot(bearer, target);
		if (why != null) {
			bearer.displayClientMessage(Component.translatable(why, target.getDisplayName(), MOST), true);
			return;
		}
		OFFERS.put(target.getUUID(), new Offer(bearer.getUUID(), bearer.level().getGameTime() + OFFER_TICKS));
		String name = bearer.getGameProfile().getName();
		MutableComponent line = Component.translatable(ModLang.MSG_SLEEVE_OFFERED, bearer.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(" ").append(button(ModLang.SLEEVE_ACCEPT, "/sleeve accept " + name, ChatFormatting.GREEN))
				.append(" ").append(button(ModLang.SLEEVE_REFUSE, "/sleeve refuse " + name, ChatFormatting.RED));
		target.sendSystemMessage(line);
		bearer.displayClientMessage(Component.translatable(ModLang.MSG_SLEEVE_OFFER, target.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
	}

	private static Component button(String key, String command, ChatFormatting colour) {
		return Component.translatable(key).withStyle(style -> style.withColor(colour).withUnderlined(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
	}

	/** {@code target} accepts the place {@code bearerName} offered (SleeveCommand). @return what they are told, or null (taken in: told so) */
	public static Component accept(ServerPlayer target, String bearerName) {
		Offer offer = OFFERS.get(target.getUUID());
		ServerPlayer bearer = target.server.getPlayerList().getPlayerByName(bearerName);
		if (offer == null || bearer == null || !offer.bearer().equals(bearer.getUUID()) || target.level().getGameTime() > offer.until()) {
			return Component.translatable(ModLang.MSG_SLEEVE_NO_OFFER);
		}
		OFFERS.remove(target.getUUID());
		String why = whyNot(bearer, target);
		if (why != null) return Component.translatable(why, target.getDisplayName(), MOST);
		takeIn(bearer, target);
		return null;
	}

	/** {@code target} refuses the place {@code bearerName} offered (SleeveCommand). @return what they are told */
	public static Component refuse(ServerPlayer target, String bearerName) {
		Offer offer = OFFERS.get(target.getUUID());
		ServerPlayer bearer = target.server.getPlayerList().getPlayerByName(bearerName);
		if (offer == null || bearer == null || !offer.bearer().equals(bearer.getUUID())) return Component.translatable(ModLang.MSG_SLEEVE_NO_OFFER);
		OFFERS.remove(target.getUUID());
		bearer.displayClientMessage(Component.translatable(ModLang.MSG_SLEEVE_REFUSED, target.getDisplayName()), true);
		return Component.translatable(ModLang.MSG_SLEEVE_YOU_REFUSED, bearer.getDisplayName());
	}

	/** Into {@code bearer}'s sleeve: {@code target}'s game mode and place kept, a spectator watching through its bearer's eyes. */
	static void takeIn(ServerPlayer bearer, ServerPlayer target) {
		Data data = Data.get(bearer.server);
		TribulationManager.abandon(target);
		MeditationManager.stop(target, false);
		data.carried.put(target.getUUID(), new Carried(bearer.getUUID(), bearer.getGameProfile().getName(),
				target.gameMode.getGameModeForPlayer().getId(), Place.of(target)));
		data.setDirty();
		ServerLevel level = target.serverLevel();
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1, target.getZ(), 60, 0.4, 0.8, 0.4, 0.15);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.6f);
		moving = true;
		try {
			target.setGameMode(GameType.SPECTATOR);
			target.setCamera(bearer);
		} finally {
			moving = false;
		}
		LAST_DIMENSION.put(bearer.getUUID(), bearer.level().dimension());
		LAST_WORLD.put(bearer.getUUID(), Place.of(bearer));
		switchOn(bearer);
		bearer.displayClientMessage(Component.translatable(ModLang.MSG_SLEEVE_TAKEN, target.getDisplayName(), carrying(bearer.getUUID()), MOST)
				.withStyle(ChatFormatting.LIGHT_PURPLE), true);
		target.sendSystemMessage(Component.translatable(ModLang.MSG_SLEEVE_INSIDE, bearer.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(" ").append(button(ModLang.SLEEVE_LEAVE, "/sleeve leave", ChatFormatting.GOLD)));
	}

	/** The ability's switch "on" while the sleeve holds anyone, as an act's is (CultivationManager#toggleAbility). */
	private static void switchOn(ServerPlayer bearer) {
		PlayerCultivation c = CultivationManager.get(bearer);
		boolean on = carrying(bearer.getUUID()) > 0;
		if (c.isAbilityEnabled(Ability.SLEEVE) == on) return;
		c.setAbilityEnabled(Ability.SLEEVE, on);
		CultivationManager.markDirty(bearer.server);
		CultivationManager.sync(bearer);
	}

	/** Partway through the Breach's or the Gate's climb (nobody steps out of a sleeve there, nor is let out). */
	private static boolean midClimb(ServerPlayer p) {
		return ModDimensions.isVoidBreach(p.level().dimension()) || ModDimensions.isImmortalPassage(p.level().dimension());
	}

	// --- Letting out ---

	/** {@code p} steps out of the sleeve they are in (SleeveCommand): only while its bearer is out in a world. @return what they are told */
	public static Component leave(ServerPlayer p) {
		Data data = Data.get(p.server);
		Carried c = data.carried.get(p.getUUID());
		if (c == null || c.outAt != null) return Component.translatable(ModLang.MSG_SLEEVE_NOT_IN);
		ServerPlayer bearer = p.server.getPlayerList().getPlayer(c.bearer);
		if (bearer != null && ModDimensions.isPassage(bearer.level().dimension())) return Component.translatable(ModLang.MSG_SLEEVE_CANT_LEAVE);
		Place at = bearer != null ? beside(bearer, 0) : c.from;
		letOut(p, at);
		if (bearer != null) {
			switchOn(bearer);
			bearer.displayClientMessage(Component.translatable(ModLang.MSG_SLEEVE_STEPPED_OUT, p.getDisplayName()), true);
		}
		return Component.translatable(ModLang.MSG_SLEEVE_YOU_STEPPED_OUT, c.bearerName);
	}

	/** Everyone {@code bearer} carries let out beside it (in the gap, out into it), each told {@code why}. */
	static void letAllOut(ServerPlayer bearer, String why) {
		boolean inGap = ModDimensions.isSpatialGap(bearer.level().dimension());
		boolean fromUpper = CultivationManager.get(bearer).isGapFromUpper();
		Data data = Data.get(bearer.server);
		int i = 0;
		for (Map.Entry<UUID, Carried> e : new ArrayList<>(data.carried.entrySet())) {
			Carried c = e.getValue();
			if (c.outAt != null || !c.bearer.equals(bearer.getUUID())) continue;
			ServerPlayer p = bearer.server.getPlayerList().getPlayer(e.getKey());
			Place here = beside(bearer, i++);
			if (p == null) {
				// Away from the game: they come out here when they come back (into the gap's, to its foot instead: Place#from).
				c.outAt = inGap ? c.from : here;
				data.setDirty();
				continue;
			}
			if (inGap) letOutIntoGap(p, here.at(), fromUpper);
			else letOut(p, here);
			p.sendSystemMessage(Component.translatable(why, bearer.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		TOLD_COST.remove(bearer.getUUID());
		switchOn(bearer);
	}

	/** {@code p} out of the sleeve at {@code at}: as they played before, their own view, set down gently. */
	static void letOut(ServerPlayer p, Place at) {
		Data data = Data.get(p.server);
		Carried c = data.carried.remove(p.getUUID());
		data.setDirty();
		ServerLevel level = at == null ? null : p.server.getLevel(at.level());
		moving = true;
		try {
			if (p.getCamera() != p) p.setCamera(p);
			if (level != null) p.teleportTo(level, at.at().x, at.at().y, at.at().z, p.getYRot(), p.getXRot());
			p.setGameMode(c == null ? GameType.SURVIVAL : GameType.byId(c.mode));
		} finally {
			moving = false;
		}
		p.fallDistance = 0;
		p.setDeltaMovement(Vec3.ZERO);
		CultivationManager.get(p).setFallProtected(true); // set down, not dropped: the first fall after is harmless
		CultivationManager.markDirty(p.server);
		RealmSuppressionHandler.update(p);
		CultivationManager.sync(p);
		p.serverLevel().sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 50, 0.4, 0.8, 0.4, 0.4);
		p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.2f);
	}

	/** {@code p} out of the sleeve into the Spatial Gap at {@code at}, there as any who came into it ({@code fromUpper}: from the Jade Firmament). */
	private static void letOutIntoGap(ServerPlayer p, Vec3 at, boolean fromUpper) {
		Data data = Data.get(p.server);
		Carried c = data.carried.remove(p.getUUID());
		data.setDirty();
		moving = true;
		try {
			if (p.getCamera() != p) p.setCamera(p);
			p.setGameMode(c == null ? GameType.SURVIVAL : GameType.byId(c.mode));
		} finally {
			moving = false;
		}
		SpatialTrialHandler.castIn(p, at, fromUpper);
	}

	// --- Each tick ---

	public static void tick(MinecraftServer server) {
		if (!OFFERS.isEmpty() && server.getTickCount() % 20 == 0) {
			long now = server.overworld().getGameTime();
			OFFERS.values().removeIf(o -> now > o.until());
		}
		Data data = Data.current();
		if (data == null || data.carried.isEmpty() && REJOINED.isEmpty()) return;
		for (UUID id : new ArrayList<>(REJOINED)) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			REJOINED.remove(id);
			if (p != null) rejoined(p);
		}
		// Each bearer first: come out into a world since the last tick, those it carries come out beside it.
		Set<UUID> bearers = new HashSet<>();
		for (Carried c : data.carried.values()) if (c.outAt == null) bearers.add(c.bearer);
		for (UUID id : bearers) {
			ServerPlayer bearer = server.getPlayerList().getPlayer(id);
			if (bearer == null || !bearer.isAlive()) continue;
			ResourceKey<Level> now = bearer.level().dimension(), was = LAST_DIMENSION.put(id, now);
			boolean passage = ModDimensions.isPassage(now);
			if (!passage) LAST_WORLD.put(id, Place.of(bearer));
			// (Not into a small world, though: those carried aren't its guests, and stay in the sleeve there till it comes out again.)
			if (was != null && was != now && !passage && !ModDimensions.isSmallWorld(now)) {
				letAllOut(bearer, ModLang.MSG_SLEEVE_ARRIVED);
				continue;
			}
			if (passage && TOLD_COST.add(id)) {
				bearer.sendSystemMessage(Component.translatable(ModLang.MSG_SLEEVE_COST, carrying(id), Math.round(pressureFactor(bearer))).withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
		// Then each one carried: a spectator still, watching through its bearer's eyes, carried into whatever world it is in.
		for (Map.Entry<UUID, Carried> e : new ArrayList<>(data.carried.entrySet())) {
			Carried c = e.getValue();
			if (c.outAt != null) continue;
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			if (p == null) continue;
			ServerPlayer bearer = server.getPlayerList().getPlayer(c.bearer);
			if (bearer == null) {
				// Its bearer gone from the game (a crash, or a server killed mid-way: a logout lets them out first): where taken in.
				letOut(p, c.from);
				p.sendSystemMessage(Component.translatable(ModLang.MSG_SLEEVE_BEARER_GONE, c.bearerName).withStyle(ChatFormatting.LIGHT_PURPLE));
				continue;
			}
			if (!bearer.isAlive()) continue; // dying: onDeath casts them out
			moving = true;
			try {
				if (p.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) p.setGameMode(GameType.SPECTATOR);
				if (p.level() != bearer.level()) p.teleportTo(bearer.serverLevel(), bearer.getX(), bearer.getY(), bearer.getZ(), p.getYRot(), p.getXRot());
				if (p.getCamera() != bearer) p.setCamera(bearer);
			} finally {
				moving = false;
			}
		}
	}

	// --- Deaths, leaving the game, coming back ---

	/**
	 * {@code bearer} slain with others in its sleeve (CultivationEvents, before the gap's or a climb's own reckoning of the death): they are
	 * cast out where it fell (the user's pick): in the Spatial Gap, into its pressure; partway through the Breach's climb, back to its foot
	 * (where the bearer stood before it); through the Gate's, back down onto the Shore of Nirvana; anywhere else, where it lay.
	 */
	public static void onDeath(ServerPlayer bearer) {
		if (carrying(bearer.getUUID()) == 0) {
			if (isCarried(bearer.getUUID())) endFor(bearer); // one carried killed (by a command): out of the sleeve, as they were
			return;
		}
		ResourceKey<Level> dim = bearer.level().dimension();
		Place where = null;
		boolean climb = true;
		if (ModDimensions.isImmortalPassage(dim)) {
			ServerLevel greatVoid = bearer.server.getLevel(ModDimensions.GREAT_VOID);
			BlockPos camp = greatVoid == null ? BlockPos.ZERO : NirvanaCamp.middle(greatVoid);
			where = new Place(ModDimensions.GREAT_VOID, new Vec3(camp.getX() + 0.5, camp.getY() + 1, camp.getZ() + 4.5));
		} else if (ModDimensions.isVoidBreach(dim)) {
			where = LAST_WORLD.get(bearer.getUUID()); // (none kept: where each was taken in)
		} else {
			climb = false;
		}
		boolean inGap = ModDimensions.isSpatialGap(dim), fromUpper = CultivationManager.get(bearer).isGapFromUpper();
		Data data = Data.get(bearer.server);
		int i = 0;
		for (Map.Entry<UUID, Carried> e : new ArrayList<>(data.carried.entrySet())) {
			Carried c = e.getValue();
			if (c.outAt != null || !c.bearer.equals(bearer.getUUID())) continue;
			ServerPlayer p = bearer.server.getPlayerList().getPlayer(e.getKey());
			Place to = !climb ? beside(bearer, i++) : where != null ? where : c.from;
			if (p == null) {
				// Away from the game: cast out so when they come back (into the gap at its bearer's last place there, if it fell in it).
				c.outAt = inGap ? Place.of(bearer) : to;
				c.castIntoGap = inGap;
				c.gapFromUpper = fromUpper;
				data.setDirty();
				continue;
			}
			if (inGap) letOutIntoGap(p, bearer.position(), fromUpper);
			else letOut(p, to);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_SLEEVE_CAST_OUT, bearer.getDisplayName()).withStyle(ChatFormatting.RED));
		}
		forget(bearer.getUUID());
	}

	/** One carried who died (a command reaches even them) or came back into a new body: out of the sleeve, playing as before. */
	private static void endFor(ServerPlayer p) {
		Data data = Data.get(p.server);
		Carried c = data.carried.remove(p.getUUID());
		data.setDirty();
		if (c == null) return;
		moving = true;
		try {
			if (p.getCamera() != p) p.setCamera(p);
			p.setGameMode(GameType.byId(c.mode));
		} finally {
			moving = false;
		}
	}

	/** A player respawned (CultivationEvents): one who died in a sleeve plays as before. */
	public static void onRespawn(ServerPlayer p) {
		Data data = Data.get(p.server);
		if (data.carried.containsKey(p.getUUID())) endFor(p);
	}

	/**
	 * {@code p} leaving the game (CultivationEvents#leaving): a bearer sets down those it carries where each was taken in (none dropped into
	 * a way's pressure for a logout); one carried stays in the sleeve, saved as they are, and coming back watches through it again (or, let
	 * out meanwhile, comes out where they were let out).
	 */
	public static void onDisconnect(ServerPlayer p) {
		OFFERS.remove(p.getUUID());
		if (carrying(p.getUUID()) == 0) return;
		boolean passage = ModDimensions.isPassage(p.level().dimension());
		Data data = Data.get(p.server);
		int i = 0;
		for (Map.Entry<UUID, Carried> e : new ArrayList<>(data.carried.entrySet())) {
			Carried c = e.getValue();
			if (c.outAt != null || !c.bearer.equals(p.getUUID())) continue;
			Place to = passage ? c.from : beside(p, i++);
			ServerPlayer carried = p.server.getPlayerList().getPlayer(e.getKey());
			if (carried == null) {
				c.outAt = to;
				data.setDirty();
				continue;
			}
			letOut(carried, to);
			carried.sendSystemMessage(Component.translatable(ModLang.MSG_SLEEVE_BEARER_GONE, p.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		forget(p.getUUID());
		CultivationManager.get(p).setAbilityEnabled(Ability.SLEEVE, false);
	}

	/** Joining with a sleeve on record: set right once they stand in the world, a tick on ({@link #rejoined}). */
	public static void onJoin(ServerPlayer p) {
		if (Data.get(p.server).carried.containsKey(p.getUUID())) REJOINED.add(p.getUUID());
	}

	/** One carried back in the game: let out meanwhile, out where they were let out; else (its bearer away) where taken in; else in it again. */
	private static void rejoined(ServerPlayer p) {
		Carried c = Data.get(p.server).carried.get(p.getUUID());
		if (c == null) return;
		if (c.outAt != null) {
			if (c.castIntoGap) letOutIntoGap(p, c.outAt.at(), c.gapFromUpper);
			else letOut(p, c.outAt);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_SLEEVE_OUT_AWAY, c.bearerName).withStyle(ChatFormatting.LIGHT_PURPLE));
			return;
		}
		if (p.server.getPlayerList().getPlayer(c.bearer) == null) {
			letOut(p, c.from);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_SLEEVE_BEARER_GONE, c.bearerName).withStyle(ChatFormatting.LIGHT_PURPLE));
			return;
		}
		p.sendSystemMessage(Component.translatable(ModLang.MSG_SLEEVE_INSIDE, c.bearerName).withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(" ").append(button(ModLang.SLEEVE_LEAVE, "/sleeve leave", ChatFormatting.GOLD)));
	}

	private static void forget(UUID bearer) {
		LAST_WORLD.remove(bearer);
		LAST_DIMENSION.remove(bearer);
		TOLD_COST.remove(bearer);
	}

	/** The server stopping, before anyone is saved: every bearer sets down those it carries, as leaving the game does ({@link #onDisconnect}). */
	public static void releaseAll(MinecraftServer server) {
		for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) onDisconnect(p);
	}

	/** Server stopped (not saved): the offers and each bearer's ways forgotten; the record stays with the world. */
	public static void clear() {
		OFFERS.clear();
		LAST_WORLD.clear();
		LAST_DIMENSION.clear();
		TOLD_COST.clear();
		REJOINED.clear();
		Data.server = null;
	}

	/** Who is in whose sleeve, kept with the Overworld's data. */
	static final class Data extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_sleeves";
		/** The server whose record {@link #current} answers (set by {@link #get}; cleared when it stops). */
		private static MinecraftServer server;
		final Map<UUID, Carried> carried = new LinkedHashMap<>();

		static Data get(MinecraftServer srv) {
			server = srv;
			return srv.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
		}

		/** The running server's record, or null before anything has asked for it (nobody carried then). */
		static Data current() {
			return server == null ? null : get(server);
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			for (Tag t : tag.getList("Carried", Tag.TAG_COMPOUND)) {
				CompoundTag e = (CompoundTag) t;
				Place from = Place.load(e.getCompound("From"));
				if (!e.hasUUID("Id") || !e.hasUUID("Bearer") || from == null) continue;
				Carried c = new Carried(e.getUUID("Bearer"), e.getString("BearerName"), e.getInt("Mode"), from);
				if (e.contains("OutAt")) c.outAt = Place.load(e.getCompound("OutAt"));
				c.castIntoGap = e.getBoolean("IntoGap");
				c.gapFromUpper = e.getBoolean("GapFromUpper");
				data.carried.put(e.getUUID("Id"), c);
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			for (Map.Entry<UUID, Carried> entry : carried.entrySet()) {
				Carried c = entry.getValue();
				CompoundTag e = new CompoundTag();
				e.putUUID("Id", entry.getKey());
				e.putUUID("Bearer", c.bearer);
				e.putString("BearerName", c.bearerName);
				e.putInt("Mode", c.mode);
				e.put("From", c.from.save());
				if (c.outAt != null) e.put("OutAt", c.outAt.save());
				e.putBoolean("IntoGap", c.castIntoGap);
				e.putBoolean("GapFromUpper", c.gapFromUpper);
				list.add(e);
			}
			tag.put("Carried", list);
			return tag;
		}
	}

	private Sleeves() {}
}
