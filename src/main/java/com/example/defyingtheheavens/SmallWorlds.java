package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The small worlds (phase 3d of the realm plan, 2026-10-26; the user's pick of who comes in: "You + who you allow"): a True Immortal, or
 * one past it, refines a World Seed (WorldSeedItem: Chaos Origin Stones and Immortal Jade) into a world of their own, one each, kept with
 * the world's save ({@link Data}): a floating island under the stars in the small worlds' dimension (ModDimensions#SMALL_WORLD), every one
 * in its own slot ({@link #SLOT_SPACING} apart), its qi the Immortal Realm's (ModDimensions#hasImmortalQi: an immortal's essence fills
 * there and the Laws are comprehended there; a breakthrough into True Immortal or past it is still the realm's alone).
 * <ul>
 *   <li><b>Its land</b> ({@link #radius}): {@link #BASE_RADIUS} blocks out, {@link #PER_LAW_LEVEL} more for every level of the Laws its
 *   owner holds, up to {@link #MAX_RADIUS}. Its heart is laid the first time it is entered, a pool and trees on it and its gate
 *   (SmallWorldGateBlock, the way out) in a frame of jade; the rest is laid out from there a little each tick ({@link #BUDGET}), and its
 *   new edge as the owner's Laws grow, never over anything that stands there. Grass and earth over stone hanging into the void, rooted
 *   here and there beneath.</li>
 *   <li><b>Its door</b> (Ability#SMALL_WORLD, an act, {@link #toggle}): opened before its owner wherever they stand ({@link #whyNotOpen}:
 *   not in a passage, the Inner Realm or a small world, nor with the soul out of the body, nor in a tribulation), a door of starlight
 *   (SmallWorldDoor) for {@link #DOOR_TICKS}. Its owner and those on its guest list walk through it into the world; anyone else is turned
 *   away. The switch again inside, or its gate, for anyone: out, where they came in. The switch is "on" while its door stands open or its
 *   owner is in it.</li>
 *   <li><b>Its guests</b> (SmallWorldCommand): put on its list and taken off it by its owner; one taken off it while inside is sent out,
 *   and one in a world not theirs to be in (taken off while away) is sent out as they come back.</li>
 *   <li>Fallen off its island, or flown {@link #ROAM} past its edge into the nothing round it: set on it again, unharmed.</li>
 * </ul>
 */
public final class SmallWorlds {
	/** The level of each island's top about its middle. */
	public static final int ISLAND_Y = 100;
	/** Each world has its own spot, this far apart, {@link #SLOTS_PER_ROW} to a row (as the Inner Realm's islands). */
	public static final int SLOT_SPACING = 4096;
	private static final int SLOTS_PER_ROW = 64;
	/** A world's land reaches so far out, so much further for every level of its owner's Laws (LawState#total), up to the last. */
	public static final int BASE_RADIUS = 16, PER_LAW_LEVEL = 2, MAX_RADIUS = 160;
	/** A door stands open so long (ticks), this far before the one who opened it. */
	public static final int DOOR_TICKS = 600;
	public static final double DOOR_AHEAD = 2.5;
	/** Holding a World Seed so long (ticks) refines it (WorldSeedItem). */
	public static final int REFINE_TICKS = 200;
	/** Blocks laid (and columns looked at) a tick while a world's land is laid out. */
	public static final int BUDGET = 4000;
	/** Fallen this far below its top, or flown this far past its edge: set on the island again. */
	public static final int FALL_CATCH = 48, ROAM = 64;
	/** How deep the land hangs at most within its rim, and its roots below that. */
	private static final int DEPTH = 20, ROOTS = 14;
	/** Level within this of the middle (the way in, the gate); the gate this far behind the way in. */
	private static final int FLAT = 10, GATE_BACK = 6;
	/** Nothing grown within this of the middle, where one comes in. */
	private static final int CLEAR = 5;

	// --- Where ---

	/** The middle of the world in {@code slot}: its top there. */
	public static BlockPos centre(int slot) {
		return new BlockPos((slot % SLOTS_PER_ROW) * SLOT_SPACING, ISLAND_Y, (slot / SLOTS_PER_ROW) * SLOT_SPACING);
	}

	/** The slot whose middle column (x, z) of the small worlds is nearest, or -1 if it lies off the grid. */
	public static int slotAt(double x, double z) {
		long col = Math.round(x / SLOT_SPACING), row = Math.round(z / SLOT_SPACING);
		if (col < 0 || col >= SLOTS_PER_ROW || row < 0 || row > Integer.MAX_VALUE / SLOTS_PER_ROW) return -1;
		return (int) (row * SLOTS_PER_ROW + col);
	}

	/** How far out a world's land reaches for {@code lawLevels} of its owner's Laws. */
	public static int radius(int lawLevels) {
		return Math.min(MAX_RADIUS, BASE_RADIUS + PER_LAW_LEVEL * Math.max(0, lawLevels));
	}

	/** The world {@code p} stands in (the small worlds' slot they are in), or null if none. */
	public static World worldAt(ServerPlayer p) {
		if (!ModDimensions.isSmallWorld(p.level().dimension())) return null;
		int slot = slotAt(p.getX(), p.getZ());
		return slot < 0 ? null : Data.get(p.server).atSlot(slot);
	}

	/** Whether {@code p} may come into {@code w}: its owner, or one on its guest list (not one looking on as a spectator). */
	public static boolean mayEnter(World w, ServerPlayer p) {
		return w != null && !p.isSpectator() && (w.owner.equals(p.getUUID()) || w.guests.containsKey(p.getUUID()));
	}

	// --- A seed refined ---

	/** Why {@code p} can't refine a World Seed, or null if they can: a True Immortal or past it, with no world yet. */
	public static Component whyNotRefine(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (!Laws.isImmortal(c)) return Component.translatable(ModLang.MSG_SEED_NOT_IMMORTAL);
		if (Data.get(p.server).of(p.getUUID()) != null) return Component.translatable(ModLang.MSG_SEED_HAVE_ONE);
		return null;
	}

	/** {@code p}'s seed refined into their world (WorldSeedItem, its holding done): told, heard. @return whether it was */
	public static boolean refine(ServerPlayer p) {
		Component why = whyNotRefine(p);
		if (why != null) {
			p.displayClientMessage(why.copy().withStyle(ChatFormatting.LIGHT_PURPLE), true);
			return false;
		}
		PlayerCultivation c = CultivationManager.get(p);
		World w = Data.get(p.server).make(p.getUUID(), p.getScoreboardName(), c.laws().total());
		c.setSmallWorld(true);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_SEED_REFINED, w.target()).withStyle(ChatFormatting.LIGHT_PURPLE));
		ServerLevel level = p.serverLevel();
		level.playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 1.0f, 0.7f);
		level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.5f);
		level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.2, p.getZ(), 60, 0.5, 0.8, 0.5, 0.08);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 80, 0.6, 0.8, 0.6, 0.05);
		return true;
	}

	// --- Its door ---

	/** Each owner's door while it stands. */
	private static final Map<UUID, SmallWorldDoor> DOORS = new HashMap<>();

	/**
	 * Its switch thrown (CultivationManager#toggleAbility): in a small world, out where one came in; else its door, open, closed, or
	 * opened before them.
	 */
	public static void toggle(ServerPlayer p) {
		if (ModDimensions.isSmallWorld(p.level().dimension())) {
			leave(p);
			return;
		}
		if (isOpen(p.getUUID())) {
			close(p.server, p.getUUID(), true);
			return;
		}
		Component why = whyNotOpen(p);
		if (why != null) {
			p.displayClientMessage(why.copy().withStyle(ChatFormatting.LIGHT_PURPLE), true);
			return;
		}
		open(p);
	}

	/** Why {@code p} can't open their world's door here, or null if they can. */
	public static Component whyNotOpen(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (Data.get(p.server).of(p.getUUID()) == null) return Component.translatable(ModLang.CMD_WORLD_NONE);
		if (!Laws.isImmortal(c)) return Component.translatable(ModLang.MSG_DOOR_NOT_IMMORTAL);
		ResourceKey<Level> dimension = p.level().dimension();
		boolean place = ModDimensions.isPassage(dimension) || ModDimensions.isInnerRealm(dimension) || ModDimensions.isSmallWorld(dimension);
		boolean busy = !p.isAlive() || p.isSpectator() || Incorporeal.isGhost(p) || SoulProjection.isActive(p.getUUID()) || Clones.inClone(p)
				|| TribulationManager.isActive(p.getUUID()) || CoreDetonation.isCharging(p.getUUID()) || p.server.getLevel(ModDimensions.SMALL_WORLD) == null;
		return place || busy ? Component.translatable(ModLang.MSG_DOOR_NOT_HERE) : null;
	}

	/** Whether {@code owner}'s door stands open. */
	public static boolean isOpen(UUID owner) {
		SmallWorldDoor door = DOORS.get(owner);
		return door != null && !door.isRemoved();
	}

	/** The door before {@code p}, facing them, for {@link #DOOR_TICKS}; the switch on. */
	static void open(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		SmallWorldDoor door = ModEntities.SMALL_WORLD_DOOR.create(level);
		if (door == null) return;
		Vec3 ahead = Vec3.directionFromRotation(0, p.getYRot()).scale(DOOR_AHEAD);
		door.setUp(p.getUUID(), DOOR_TICKS);
		door.moveTo(p.getX() + ahead.x, p.getY(), p.getZ() + ahead.z, p.getYRot() + 180, 0);
		level.addFreshEntity(door);
		DOORS.put(p.getUUID(), door);
		switchTo(p, true);
		p.displayClientMessage(Component.translatable(ModLang.MSG_DOOR_OPEN, DOOR_TICKS / 20).withStyle(ChatFormatting.LIGHT_PURPLE), true);
		level.playSound(null, door.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
		level.playSound(null, door.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.7f);
	}

	/** {@code owner}'s door closed (told, if {@code told} and they're about); their switch off unless they are in their world. */
	public static void close(MinecraftServer server, UUID owner, boolean told) {
		SmallWorldDoor door = DOORS.remove(owner);
		if (door != null && !door.isRemoved()) {
			door.level().playSound(null, door.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.5f);
			door.discard();
		}
		ServerPlayer p = server.getPlayerList().getPlayer(owner);
		if (p == null) return;
		World own = Data.get(server).of(owner);
		if (worldAt(p) != own || own == null) switchTo(p, false);
		if (told) p.displayClientMessage(Component.translatable(ModLang.MSG_DOOR_CLOSED), true);
	}

	/** The door's time run out or it is gone (SmallWorldDoor): forgotten, its owner's switch as it now stands. */
	static void doorGone(SmallWorldDoor door) {
		if (door.owner() == null || DOORS.get(door.owner()) != door) return;
		if (door.level() instanceof ServerLevel level) close(level.getServer(), door.owner(), false);
	}

	/** The time since each was last turned away at a door (that they're told once a while, not each tick). */
	private static final Map<UUID, Long> TURNED_AWAY = new HashMap<>();

	/**
	 * {@code p} at {@code door} (SmallWorldDoor, its tick): carried into its world if it's theirs to come into; else turned away, a step
	 * back from it.
	 */
	static void atDoor(SmallWorldDoor door, ServerPlayer p) {
		World w = Data.get(p.server).of(door.owner());
		if (w == null || CARRYING.contains(p.getUUID())) return;
		if (!mayEnter(w, p) || Incorporeal.isGhost(p) || SoulProjection.isActive(p.getUUID())) {
			long now = p.server.getTickCount();
			Long last = TURNED_AWAY.get(p.getUUID());
			if (last == null || now - last > 60) {
				TURNED_AWAY.put(p.getUUID(), now);
				p.displayClientMessage(Component.translatable(ModLang.MSG_DOOR_TURNED_AWAY, w.ownerName), true);
			}
			Vec3 away = p.position().subtract(door.position()).multiply(1, 0, 1);
			if (away.lengthSqr() < 1.0e-4) away = Vec3.directionFromRotation(0, door.getYRot());
			p.setDeltaMovement(away.normalize().scale(0.6).add(0, 0.15, 0));
			p.hurtMarked = true;
			return;
		}
		Level from = p.level();
		carry(p, () -> {
			if (p.level() == from) enter(p, w);
		});
	}

	// --- Coming in and going out ---

	/** Players a door or a gate has hold of, to be carried as soon as the server's next task runs (as SpatialRiftBlock carries them). */
	private static final Set<UUID> CARRYING = new HashSet<>();

	/**
	 * {@code then} for {@code p} at the server's next task, not at once: a walker meets a gate inside the server's handling of their own
	 * step, and carried to another dimension right there they'd be snapped back as for a wrong move (SpatialRiftBlock#entityInside).
	 */
	private static void carry(ServerPlayer p, Runnable then) {
		if (!CARRYING.add(p.getUUID())) return;
		MinecraftServer server = p.server;
		server.tell(new TickTask(server.getTickCount(), () -> {
			CARRYING.remove(p.getUUID());
			if (p.isRemoved() || !p.isAlive()) return;
			try {
				then.run();
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("A small world failed to carry {}", p.getScoreboardName(), e);
			}
		}));
	}

	/**
	 * {@code p} into {@code w}: where they came in from kept (for the way out), the world's heart laid if it isn't yet, set at its way in
	 * facing out over the island, told whose world it is. @return whether they came in
	 */
	public static boolean enter(ServerPlayer p, World w) {
		ServerLevel sw = p.server.getLevel(ModDimensions.SMALL_WORLD);
		if (sw == null || w == null) return false;
		Data data = Data.get(p.server);
		if (!ModDimensions.isSmallWorld(p.level().dimension())) data.keepReturn(p);
		p.stopRiding();
		lay(sw, w);
		BlockPos c = w.centre();
		ServerLevel was = p.serverLevel();
		was.playSound(null, p.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.6f);
		p.teleportTo(sw, c.getX() + 0.5, ISLAND_Y + 1, c.getZ() + 0.5, 0, 10);
		p.setDeltaMovement(Vec3.ZERO);
		p.fallDistance = 0;
		boolean own = w.owner.equals(p.getUUID());
		if (own) switchTo(p, true);
		p.displayClientMessage(own ? Component.translatable(ModLang.MSG_WORLD_OWN, (int) PlayerCultivation.IMMORTAL_QI_MULTIPLIER)
				: Component.translatable(ModLang.MSG_WORLD_GUEST, w.ownerName), true);
		sw.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.8f);
		sw.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
		return true;
	}

	/** {@code p} out of the small worlds, where they came in (or, that lost, at their spawn); their switch off if it's their world they left. */
	public static void leave(ServerPlayer p) {
		Data data = Data.get(p.server);
		CompoundTag back = data.takeReturn(p.getUUID());
		ServerLevel level = back == null ? null : p.server.getLevel(dimension(back));
		p.stopRiding();
		if (level == null) {
			ServerLevel spawn = p.server.getLevel(p.getRespawnDimension());
			if (spawn == null || ModDimensions.isSmallWorld(spawn.dimension())) spawn = p.server.overworld();
			BlockPos at = p.getRespawnPosition() != null && spawn.dimension() == p.getRespawnDimension() ? p.getRespawnPosition() : spawn.getSharedSpawnPos();
			p.teleportTo(spawn, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, p.getYRot(), p.getXRot());
		} else {
			p.teleportTo(level, back.getDouble("X"), back.getDouble("Y"), back.getDouble("Z"), back.getFloat("YRot"), back.getFloat("XRot"));
		}
		p.setDeltaMovement(Vec3.ZERO);
		p.fallDistance = 0;
		if (data.of(p.getUUID()) != null && !isOpen(p.getUUID())) switchTo(p, false);
		p.displayClientMessage(Component.translatable(ModLang.MSG_WORLD_LEFT), true);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.8f);
	}

	/** {@code p} walked into a world's gate (SmallWorldGateBlock): out, at the server's next task. */
	static void atGate(ServerPlayer p) {
		if (!ModDimensions.isSmallWorld(p.level().dimension())) return;
		Level from = p.level();
		carry(p, () -> {
			if (p.level() == from) leave(p);
		});
	}

	/** {@code p}'s switch set (the Abilities tab shows it) and the client told. */
	private static void switchTo(ServerPlayer p, boolean on) {
		PlayerCultivation c = CultivationManager.get(p);
		if (c.isAbilityEnabled(Ability.SMALL_WORLD) == on) return;
		c.setAbilityEnabled(Ability.SMALL_WORLD, on);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
	}

	// --- The guest list (SmallWorldCommand) ---

	/** {@code guest} put on {@code owner}'s list (told both). @return the command's line for the owner */
	public static Component addGuest(ServerPlayer owner, ServerPlayer guest) {
		Data data = Data.get(owner.server);
		World w = data.of(owner.getUUID());
		if (w == null) return Component.translatable(ModLang.CMD_WORLD_NONE);
		if (guest.getUUID().equals(owner.getUUID())) return Component.translatable(ModLang.CMD_GUEST_SELF);
		if (w.guests.containsKey(guest.getUUID())) return Component.translatable(ModLang.CMD_GUEST_ALREADY, guest.getScoreboardName());
		w.guests.put(guest.getUUID(), guest.getScoreboardName());
		data.setDirty();
		guest.sendSystemMessage(Component.translatable(ModLang.MSG_GUEST_INVITED, owner.getScoreboardName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		return Component.translatable(ModLang.CMD_GUEST_ADDED, guest.getScoreboardName());
	}

	/** The guest named {@code name} taken off {@code owner}'s list; sent out if they are in the world now. @return the owner's line */
	public static Component removeGuest(ServerPlayer owner, String name) {
		Data data = Data.get(owner.server);
		World w = data.of(owner.getUUID());
		if (w == null) return Component.translatable(ModLang.CMD_WORLD_NONE);
		UUID id = null;
		for (Map.Entry<UUID, String> guest : w.guests.entrySet()) if (guest.getValue().equalsIgnoreCase(name)) id = guest.getKey();
		if (id == null) return Component.translatable(ModLang.CMD_GUEST_UNKNOWN, name);
		String was = w.guests.remove(id);
		data.setDirty();
		ServerPlayer guest = owner.server.getPlayerList().getPlayer(id);
		if (guest != null && worldAt(guest) == w) sendOut(guest);
		return Component.translatable(ModLang.CMD_GUEST_REMOVED, was);
	}

	/** {@code p} sent out of a world not theirs to be in: told, and out where they came in. */
	static void sendOut(ServerPlayer p) {
		leave(p);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_WORLD_SENT_OUT).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	// --- Each tick ---

	/** Each owner's land being laid out (from its heart, or its new edge as their Laws grow), a little each tick. */
	private static final Map<UUID, Job> JOBS = new HashMap<>();

	/**
	 * Each tick: the land being laid in the worlds someone is in; each second the owners' Laws (their worlds' reach) and the land due
	 * laid; and those in the small worlds who have fallen off their island, flown out into its nothing, or are in one not theirs to be in.
	 */
	public static void tick(MinecraftServer server) {
		ServerLevel sw = server.getLevel(ModDimensions.SMALL_WORLD);
		if (sw == null) return;
		Data data = Data.get(server);
		Set<Integer> occupied = new HashSet<>();
		for (ServerPlayer p : sw.players()) {
			int slot = slotAt(p.getX(), p.getZ());
			if (slot >= 0) occupied.add(slot);
		}
		int budget = BUDGET;
		for (java.util.Iterator<Job> it = JOBS.values().iterator(); it.hasNext() && budget > 0; ) {
			Job job = it.next();
			if (!occupied.contains(job.world.slot)) continue;
			budget = job.step(sw, budget);
			if (job.done()) {
				it.remove();
				job.world.laid = job.to;
				data.setDirty();
				ServerPlayer owner = server.getPlayerList().getPlayer(job.world.owner);
				if (job.grew && owner != null && worldAt(owner) == job.world) {
					owner.displayClientMessage(Component.translatable(ModLang.MSG_WORLD_GROWN, job.to).withStyle(ChatFormatting.LIGHT_PURPLE), true);
				}
			}
		}
		if (server.getTickCount() % 20 == 0) {
			// A door gone with its land unloaded, say, forgotten; each owner's switch as things stand (a death in the world, a respawn out
			// of it), and their Laws, their world's reach.
			DOORS.values().removeIf(SmallWorldDoor::isRemoved);
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				World own = data.of(p.getUUID());
				if (own == null) continue;
				switchTo(p, isOpen(p.getUUID()) || worldAt(p) == own);
				int levels = CultivationManager.get(p).laws().total();
				if (levels != own.lawLevels) {
					own.lawLevels = levels;
					data.setDirty();
				}
			}
			for (int slot : occupied) {
				World w = data.atSlot(slot);
				if (w != null && w.laid > 0 && w.target() > w.laid && !JOBS.containsKey(w.owner)) JOBS.put(w.owner, new Job(w, w.laid, w.target(), true));
			}
		}
		// A copy: one sent out leaves the level's own list as they go (the bug sweep of 2026-10-27: going through that list itself, the
		// server's tick broke off with a ConcurrentModificationException the moment anyone was sent out).
		for (ServerPlayer p : new ArrayList<>(sw.players())) {
			// (Not one watching their body held by another's soul (Possession), nor a soul out of its body (SoulProjection): their places
			// are their bodies'.)
			if (!p.isAlive() || CARRYING.contains(p.getUUID()) || Possession.isHeld(p.getUUID()) || SoulProjection.isAwayFromBody(p.getUUID())) continue;
			World w = worldAt(p);
			boolean looking = p.isSpectator() && p.hasPermissions(2); // an operator's look about
			if (!looking && !mayEnter(w, p)) {
				sendOut(p);
				continue;
			}
			if (w == null) continue;
			BlockPos c = w.centre();
			double out = Math.hypot(p.getX() - c.getX() - 0.5, p.getZ() - c.getZ() - 0.5);
			if (!looking && (p.getY() < ISLAND_Y - FALL_CATCH || out > Math.max(w.laid, BASE_RADIUS) + ROAM)) {
				p.teleportTo(sw, c.getX() + 0.5, ISLAND_Y + 1, c.getZ() + 0.5, p.getYRot(), p.getXRot());
				p.setDeltaMovement(Vec3.ZERO);
				p.fallDistance = 0;
				p.displayClientMessage(Component.translatable(ModLang.MSG_WORLD_CAUGHT), true);
			}
		}
	}

	/** Coming back into the world: their switch as it stands (a door doesn't outlast a logout), and out of a world not theirs to be in. */
	public static void onJoin(ServerPlayer p) {
		Data data = Data.get(p.server);
		World own = data.of(p.getUUID());
		PlayerCultivation c = CultivationManager.get(p);
		c.setSmallWorld(own != null);
		if (own != null && !own.ownerName.equals(p.getScoreboardName())) {
			own.ownerName = p.getScoreboardName();
			data.setDirty();
		}
		World here = worldAt(p);
		c.setAbilityEnabled(Ability.SMALL_WORLD, own != null && here == own);
		if (ModDimensions.isSmallWorld(p.level().dimension()) && !mayEnter(here, p)) sendOut(p);
	}

	/** Leaving: their door closes. */
	public static void onDisconnect(ServerPlayer p) {
		close(p.server, p.getUUID(), false);
		CARRYING.remove(p.getUUID());
		TURNED_AWAY.remove(p.getUUID());
	}

	/** The server stopped: nothing of this world's kept (the doors went with it; the land's laying goes on next time). */
	public static void clear() {
		DOORS.clear();
		JOBS.clear();
		SHAPES.clear();
		CARRYING.clear();
		TURNED_AWAY.clear();
	}

	// --- Its land ---

	/** A world's own shape: its hills', its underside's and its roots' noises, and the lobes of its coast. */
	private record Shape(SimplexNoise hills, SimplexNoise under, double p1, double p2, double p3) {}

	private static final Map<UUID, Shape> SHAPES = new HashMap<>();

	private static Shape shape(World w) {
		return SHAPES.computeIfAbsent(w.owner, id -> {
			RandomSource random = RandomSource.create(id.getMostSignificantBits() ^ id.getLeastSignificantBits() * 31);
			return new Shape(new SimplexNoise(random), new SimplexNoise(random), random.nextDouble() * Math.PI * 2,
					random.nextDouble() * Math.PI * 2, random.nextDouble() * Math.PI * 2);
		});
	}

	/** The island's edge on column (dx, dz)'s bearing from its middle, its land {@code radius} out: lobed, always the further for a larger. */
	static double edge(World w, double dx, double dz, int radius) {
		Shape s = shape(w);
		double a = Math.atan2(dz, dx);
		double lobes = 0.5 * Math.sin(3 * a + s.p1) + 0.3 * Math.sin(5 * a + s.p2) + 0.2 * Math.sin(8 * a + s.p3);
		return radius + (1.5 + radius * 0.07) * lobes;
	}

	/** How far the land's top on column (dx, dz) stands above (or below) its level in the middle: level about the middle, rolling further out. */
	static int rise(World w, int dx, int dz) {
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < FLAT) return 0;
		double swell = Math.min(3.0, (d - FLAT) / 6.0);
		return (int) Math.round(swell * shape(w).hills.getValue(dx * 0.035, dz * 0.035));
	}

	/** How deep the land hangs below its top on column (dx, dz), {@code d} out, its edge {@code edge} out on that bearing (deeper, the larger it is). */
	static int depth(World w, int dx, int dz, double d, double edge) {
		Shape s = shape(w);
		double in = edge - d;
		double hang = Math.min(DEPTH, 2 + in * 1.3);
		double roots = Math.max(0, s.under.getValue(dx * 0.09, dz * 0.09) - 0.35) / 0.65 * ROOTS * Math.min(1, in / 10);
		double rough = s.under.getValue(dx * 0.21 + 100, dz * 0.21) * 2 * Math.min(1, Math.max(0, in) / 4);
		return Math.max(1, (int) Math.round(hang + roots + rough));
	}

	/** The block at height {@code y} of a column topped at {@code top}: grass, earth under it, stone below, banded with andesite. */
	private static BlockState ground(World w, int dx, int y, int dz, int top) {
		if (y == top) return Blocks.GRASS_BLOCK.defaultBlockState();
		if (y >= top - 3) return Blocks.DIRT.defaultBlockState();
		return shape(w).hills.getValue(dx * 0.08 + y * 0.05, dz * 0.08 - y * 0.03) > 0.45 ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
	}

	/**
	 * {@code w}'s land as far as it is due: its heart laid now if none is (with its gate, pool and trees), the rest out to its reach
	 * queued to be laid a little each tick.
	 */
	static void lay(ServerLevel level, World w) {
		if (w.laid <= 0) {
			Job heart = new Job(w, 0, Math.min(BASE_RADIUS, w.target()), false);
			while (!heart.done()) heart.step(level, Integer.MAX_VALUE);
			furnish(level, w);
			w.laid = heart.to;
			Data.get(level.getServer()).setDirty();
		}
		if (w.target() > w.laid && !JOBS.containsKey(w.owner)) JOBS.put(w.owner, new Job(w, w.laid, w.target(), w.laid > BASE_RADIUS));
	}

	/**
	 * A world's land laid out from {@code from} to {@code to} (0: none yet), column by column across its square: a new column whole, one
	 * laid before (within its old rim's taper) only deepened below its old underside; nothing but air ever set over.
	 */
	static final class Job {
		final World world;
		final BlockPos centre;
		final int from, to;
		final boolean grew;
		private final int reach;
		private int index;

		Job(World world, int from, int to, boolean grew) {
			this(world, world.centre(), from, to, grew);
		}

		/** Laid about {@code centre} (its world's middle; elsewhere for the game tests). */
		Job(World world, BlockPos centre, int from, int to, boolean grew) {
			this.world = world;
			this.centre = centre;
			this.from = from;
			this.to = to;
			this.grew = grew;
			this.reach = to + (int) Math.ceil(1.5 + to * 0.07) + 1;
		}

		boolean done() {
			int side = reach * 2 + 1;
			return index >= side * side;
		}

		/** As many columns as {@code budget} allows (a block laid, or a column looked at, one of it). @return what is left of it */
		int step(ServerLevel level, int budget) {
			int side = reach * 2 + 1;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			while (budget > 0 && index < side * side) {
				int dx = index % side - reach, dz = index / side - reach;
				index++;
				budget--;
				double d = Math.sqrt(dx * dx + dz * dz);
				double edgeNew = edge(world, dx, dz, to);
				if (d > edgeNew) continue;
				double edgeOld = from > 0 ? edge(world, dx, dz, from) : -1;
				if (edgeOld > 0 && d <= edgeOld - DEPTH) continue; // its depth there no deeper for the growing
				boolean wasLand = edgeOld > 0 && d <= edgeOld;
				int top = centre.getY() + rise(world, dx, dz);
				int lowest = top - depth(world, dx, dz, d, edgeNew) + 1;
				int highest = wasLand ? top - depth(world, dx, dz, d, edgeOld) : top;
				for (int y = lowest; y <= highest; y++) {
					pos.set(centre.getX() + dx, y, centre.getZ() + dz);
					if (!level.getBlockState(pos).isAir()) continue;
					level.setBlock(pos, ground(world, dx, y, dz, top), Block.UPDATE_CLIENTS);
					budget--;
				}
				if (!wasLand && d > CLEAR) grow(level, world, centre.getX() + dx, top + 1, centre.getZ() + dz);
			}
			return budget;
		}
	}

	/** On new grass: now and then short grass, more seldom a flower (the column's own hash, so the same each time it is laid). */
	private static void grow(ServerLevel level, World w, int x, int y, int z) {
		long h = (x * 3129871L) ^ (z * 116129781L) ^ (w.slot * 0x9E3779B9L);
		h = h * h * 42317861L + h * 11L;
		int roll = (int) ((h >>> 16) & 1023);
		BlockState plant = roll < 90 ? Blocks.GRASS.defaultBlockState() : roll < 104 ? FLOWERS[(int) ((h >>> 28) & 0xFFFF) % FLOWERS.length].defaultBlockState() : null;
		BlockPos pos = new BlockPos(x, y, z);
		if (plant != null && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) level.setBlock(pos, plant, Block.UPDATE_CLIENTS);
	}

	private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.ALLIUM,
			Blocks.LILY_OF_THE_VALLEY};

	/** Its heart's furnishings, laid once with it: its gate {@link #GATE_BACK} behind the way in ({@link #gate}), a pool, and three cherry trees. */
	static void furnish(ServerLevel level, World w) {
		BlockPos c = w.centre();
		gate(level, c.offset(0, 0, -GATE_BACK));
		// The pool, south-east of the way in: two deep, clay under it.
		int px = c.getX() + 7, pz = c.getZ() + 5;
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				double r = Math.sqrt(dx * dx + dz * dz);
				if (r > 2.6) continue;
				int top = c.getY() + rise(w, px + dx - c.getX(), pz + dz - c.getZ());
				BlockPos at = new BlockPos(px + dx, top, pz + dz);
				level.setBlock(at.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				level.setBlock(at, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
				level.setBlock(at.below(), r < 1.6 ? Blocks.WATER.defaultBlockState() : Blocks.CLAY.defaultBlockState(), Block.UPDATE_CLIENTS);
				if (r < 1.6) level.setBlock(at.below(2), Blocks.CLAY.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		// Three cherry trees about the heart.
		var trees = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(TreeFeatures.CHERRY);
		RandomSource random = RandomSource.create(w.slot * 0x51ED27L + 7);
		for (int[] at : new int[][] {{-8, 4}, {9, -4}, {-4, 11}}) {
			int top = c.getY() + rise(w, at[0], at[1]);
			BlockPos pos = new BlockPos(c.getX() + at[0], top + 1, c.getZ() + at[1]);
			if (trees.isPresent() && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
				trees.get().value().place(level, level.getChunkSource().getGenerator(), random, pos);
			}
		}
	}

	/**
	 * A world's gate, its middle at {@code base} (the top of the land under it): three wide and four high of starlight (SmallWorldGateBlock)
	 * across X between pillars of jade, under a lintel of jade and a cap of white jade, on a step of white jade.
	 */
	static void gate(ServerLevel level, BlockPos base) {
		BlockState jade = ModBlocks.JADE_STONE.defaultBlockState(), white = Blocks.CALCITE.defaultBlockState(), gate = ModBlocks.SMALL_WORLD_GATE.defaultBlockState();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -1; dz <= 1; dz++) level.setBlock(base.offset(dx, 0, dz), white, Block.UPDATE_CLIENTS);
		}
		for (int dy = 1; dy <= 4; dy++) {
			level.setBlock(base.offset(-2, dy, 0), jade, Block.UPDATE_CLIENTS);
			level.setBlock(base.offset(2, dy, 0), jade, Block.UPDATE_CLIENTS);
			for (int dx = -1; dx <= 1; dx++) level.setBlock(base.offset(dx, dy, 0), gate, Block.UPDATE_CLIENTS);
		}
		for (int dx = -2; dx <= 2; dx++) level.setBlock(base.offset(dx, 5, 0), jade, Block.UPDATE_CLIENTS);
		for (int dx = -3; dx <= 3; dx++) level.setBlock(base.offset(dx, 6, 0), white, Block.UPDATE_CLIENTS);
	}

	private static ResourceKey<Level> dimension(CompoundTag back) {
		ResourceLocation id = ResourceLocation.tryParse(back.getString("Dimension"));
		return id == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, id);
	}

	// --- Kept with the world ---

	/** One world: whose it is, its slot, how far out its land is laid, its owner's Law levels as last seen, and its guests (named as last seen). */
	public static final class World {
		public final UUID owner;
		public final int slot;
		String ownerName = "";
		int laid;
		int lawLevels;
		final Map<UUID, String> guests = new LinkedHashMap<>();

		World(UUID owner, int slot) {
			this.owner = owner;
			this.slot = slot;
		}

		public BlockPos centre() { return SmallWorlds.centre(slot); }

		/** How far out its land reaches by its owner's Laws. */
		public int target() { return radius(lawLevels); }

		/** How far out its land is laid now (0: none yet). */
		public int laid() { return laid; }

		public String ownerName() { return ownerName; }

		public Map<UUID, String> guests() { return Collections.unmodifiableMap(guests); }
	}

	/** Every world, and where each one in a small world came in from (to go out there), kept with the world (in the Overworld's data). */
	public static final class Data extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_small_worlds";
		private final Map<UUID, World> worlds = new HashMap<>();
		private final Map<Integer, World> bySlot = new HashMap<>();
		private final Map<UUID, CompoundTag> returns = new HashMap<>();
		private int next;

		public static Data get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
		}

		public World of(UUID owner) { return worlds.get(owner); }

		public World atSlot(int slot) { return bySlot.get(slot); }

		/** A new world for {@code owner}, in the next slot. */
		World make(UUID owner, String name, int lawLevels) {
			World w = new World(owner, next++);
			w.ownerName = name;
			w.lawLevels = lawLevels;
			worlds.put(owner, w);
			bySlot.put(w.slot, w);
			setDirty();
			return w;
		}

		/** {@code owner}'s world struck from the records (the game tests' own; its slot not handed out again). */
		void forget(UUID owner) {
			World w = worlds.remove(owner);
			if (w != null) bySlot.remove(w.slot);
			setDirty();
		}

		/** Where {@code p} stands now, kept as where they came in from. */
		void keepReturn(ServerPlayer p) {
			CompoundTag back = new CompoundTag();
			back.putString("Dimension", p.level().dimension().location().toString());
			back.putDouble("X", p.getX());
			back.putDouble("Y", p.getY());
			back.putDouble("Z", p.getZ());
			back.putFloat("YRot", p.getYRot());
			back.putFloat("XRot", p.getXRot());
			returns.put(p.getUUID(), back);
			setDirty();
		}

		/** Where {@code player} came in from, forgotten as it is told, or null. */
		CompoundTag takeReturn(UUID player) {
			CompoundTag back = returns.remove(player);
			if (back != null) setDirty();
			return back;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			for (World w : worlds.values()) {
				CompoundTag one = new CompoundTag();
				one.putUUID("Owner", w.owner);
				one.putInt("Slot", w.slot);
				one.putString("Name", w.ownerName);
				one.putInt("Laid", w.laid);
				one.putInt("Laws", w.lawLevels);
				ListTag guests = new ListTag();
				for (Map.Entry<UUID, String> guest : w.guests.entrySet()) {
					CompoundTag g = new CompoundTag();
					g.putUUID("Id", guest.getKey());
					g.putString("Name", guest.getValue());
					guests.add(g);
				}
				one.put("Guests", guests);
				list.add(one);
			}
			tag.put("Worlds", list);
			ListTag backs = new ListTag();
			for (Map.Entry<UUID, CompoundTag> back : returns.entrySet()) {
				CompoundTag one = back.getValue().copy();
				one.putUUID("Player", back.getKey());
				backs.add(one);
			}
			tag.put("Returns", backs);
			tag.putInt("Next", next);
			return tag;
		}

		public static Data load(CompoundTag tag) {
			Data data = new Data();
			for (Tag entry : tag.getList("Worlds", Tag.TAG_COMPOUND)) {
				CompoundTag one = (CompoundTag) entry;
				if (!one.hasUUID("Owner")) continue;
				World w = new World(one.getUUID("Owner"), one.getInt("Slot"));
				w.ownerName = one.getString("Name");
				w.laid = one.getInt("Laid");
				w.lawLevels = one.getInt("Laws");
				for (Tag g : one.getList("Guests", Tag.TAG_COMPOUND)) {
					CompoundTag guest = (CompoundTag) g;
					if (guest.hasUUID("Id")) w.guests.put(guest.getUUID("Id"), guest.getString("Name"));
				}
				data.worlds.put(w.owner, w);
				data.bySlot.put(w.slot, w);
			}
			for (Tag entry : tag.getList("Returns", Tag.TAG_COMPOUND)) {
				CompoundTag one = ((CompoundTag) entry).copy();
				if (!one.hasUUID("Player")) continue;
				UUID player = one.getUUID("Player");
				one.remove("Player");
				data.returns.put(player, one);
			}
			data.next = Math.max(tag.getInt("Next"), data.bySlot.keySet().stream().mapToInt(s -> s + 1).max().orElse(0));
			return data;
		}
	}

	private SmallWorlds() {}
}
