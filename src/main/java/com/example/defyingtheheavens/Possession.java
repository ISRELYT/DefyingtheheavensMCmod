package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A nascent soul in another's body (SoulProjection#tryPossess; the user's rules, 2026-10-18): a player's, or an NPC cultivator's.
 * Not a puppet steered from outside: the soul's own player takes the body's place and plays it as its own, with everything a
 * player can do. It has what the body carries, wears and holds, its health, hunger, experience and effects, its look and name to
 * everyone (the client's ClientBodies; {@link #displayName}), and its own cultivation: its realm, qi, abilities and spells, and
 * meditating in it cultivates the body, not the soul ({@link CultivationManager#get} answers with the body's record while the soul
 * is in it). The soul's own things wait for it in its own record ({@link PlayerCultivation#getPossession}), and its upkeep still
 * comes out of its own pool (SoulProjection#upkeep).
 * <ul>
 *   <li><b>A player</b> isn't killed: they watch through their body's eyes, a spectator held to it (no other view, no leaving it,
 *   nothing touched), until the soul lets go (Soul Projection, put on the body's wheel for it), runs dry, is pulled out by its
 *   tether or called home, or the body dies. Then the body is theirs again where it stands, with whatever was done with it. A body
 *   that dies, dies as theirs ({@link #kill}). Either of the two logging out, or the server stopping, gives it back first; a crash
 *   is put right as each comes back ({@link #onJoin}).</li>
 *   <li><b>An NPC cultivator</b> is set aside, unseen and still ({@link CultivatorNpc#isVacant}), and goes where its body goes;
 *   given back, it stands there again with what its body carries (what it wears and holds; the rest among its winnings), its
 *   health, and any stage its cultivation came to. Its sect takes its body for one of its own.</li>
 * </ul>
 * Their ender chest stays each one's own, as does a player's spawn point: the body is borrowed, not who they are.
 */
public final class Possession {
	/** A soul in a body. */
	private static final class Tenancy {
		final UUID soul, body;
		/** The soul's player (as last ticked: leaving the End by its portal makes it anew). */
		ServerPlayer soulPlayer;
		/** Whether the body is an NPC's (else a player's). */
		final boolean npcBody;
		/** The player whose body it is while they're here (null while they're away: logged out, the body still held), or null for an NPC's. */
		ServerPlayer heldPlayer;
		/** A player's body: their name and profile (their skin, for clients that no longer have them in the list), kept for while they're away. */
		final Component bodyName;
		final com.mojang.authlib.GameProfile bodyProfile;
		/** The NPC whose body it is (as last found: crossing into another world makes it anew), or null for a player's. */
		CultivatorNpc npc;
		/** The body's cultivation: its player's own record, or the NPC's. */
		final PlayerCultivation record;
		/** The sect the body belongs to (its player as a disciple, or the NPC), or null: its grounds and barrier take the soul for one of its own. */
		final UUID sect;
		/** Where Soul Projection was put on the body's ability wheel, to leave by (-1: it was there already, or the wheel was full). */
		int wheelSlot = -1;

		Tenancy(ServerPlayer soulPlayer, ServerPlayer heldPlayer, CultivatorNpc npc, PlayerCultivation record, UUID sect) {
			this.soul = soulPlayer.getUUID();
			this.soulPlayer = soulPlayer;
			this.npcBody = heldPlayer == null;
			this.heldPlayer = heldPlayer;
			this.bodyName = heldPlayer == null ? null : heldPlayer.getDisplayName();
			this.bodyProfile = heldPlayer == null ? null : heldPlayer.getGameProfile();
			this.npc = npc;
			this.body = heldPlayer != null ? heldPlayer.getUUID() : npc.getUUID();
			this.record = record;
			this.sect = sect;
		}

		boolean isNpc() { return npcBody; }
	}

	/**
	 * A soul of this rank or above (its own path's, as it truly stands) forces a mortal body's meridians open as it enters: no
	 * Marrow Cleansing Elixir, no tempering to Peak first (Heavenly Being Grand Perfection; the user's ask, 2026-10-19, my pick of
	 * the two they named). Shattered meridians stay shut, as they do to the elixir.
	 */
	public static final int OPENS_MERIDIANS = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);

	private static final Map<UUID, Tenancy> BY_SOUL = new HashMap<>();
	private static final Map<UUID, Tenancy> BY_BODY = new HashMap<>();
	/** While a soul is being moved in or out here, a held player's view may be set (anything else trying it is refused: {@link #mayView}). */
	private static boolean moving;

	/**
	 * Client hooks (set by the client): whether this record is the local player's while their soul is in a body (so Soul Projection,
	 * to leave it, is open), and the name a player goes by on this client.
	 */
	public static Predicate<PlayerCultivation> clientBorrowed = c -> false;
	public static Function<Player, Component> clientName = player -> null;

	// --- Queries ---

	/** The record of the body {@code soul} is in, or null (in none). */
	public static PlayerCultivation borrowed(ServerPlayer soul) {
		if (BY_SOUL.isEmpty()) return null;
		Tenancy t = BY_SOUL.get(soul.getUUID());
		return t == null ? null : t.record;
	}

	/** Whether {@code soul}'s soul is in a body. */
	public static boolean inhabits(UUID soul) {
		return !BY_SOUL.isEmpty() && BY_SOUL.containsKey(soul);
	}

	/** Whether {@code player}'s body is held by another's soul (they are watching it, a spectator). */
	public static boolean isHeld(UUID player) {
		if (BY_BODY.isEmpty()) return false;
		Tenancy t = BY_BODY.get(player);
		return t != null && !t.isNpc();
	}

	/** Whether a soul is in the body of {@code body} (a player or an NPC). */
	public static boolean holds(UUID body) {
		return !BY_BODY.isEmpty() && BY_BODY.containsKey(body);
	}

	/** Whether {@code c} is the record of a body some soul is in (server), or the local player's while theirs is (client). */
	public static boolean isBorrowed(PlayerCultivation c) {
		for (Tenancy t : BY_SOUL.values()) if (t.record == c) return true;
		return clientBorrowed.test(c);
	}

	/** Client hook (set by the client): whether this record is the local player's body while their own soul can tear space. */
	public static Predicate<PlayerCultivation> clientSoulTears = c -> false;

	/**
	 * Whether the soul in the body whose record is {@code c} can tear space with its own tear (Ability#SPATIAL_TEAR on its own record:
	 * a Heavenly Being's): in a body without one, it tears with its own, for its own pool (SpatialTrialHandler#tear).
	 */
	public static boolean soulTears(PlayerCultivation c) {
		for (Tenancy t : BY_SOUL.values()) {
			if (t.record == c) return Ability.SPATIAL_TEAR.isUnlocked(CultivationManager.own(t.soulPlayer.server, t.soul));
		}
		return clientSoulTears.test(c);
	}

	/**
	 * How many times as fast meditating cultivates the body {@code p}'s soul is in (MeditationManager, either path): once more for
	 * each realm the soul stands above the body, counted by the stage, a quarter a stage (a mortal's body a stage below Qi Refining
	 * Early). 1 when it is in none. The user's ask, 2026-10-19: the higher the one possessing, the faster the body cultivates.
	 */
	public static double guidance(ServerPlayer p) {
		Tenancy t = BY_SOUL.isEmpty() ? null : BY_SOUL.get(p.getUUID());
		return t == null ? 1 : guidance(CultivationManager.own(p), t.record);
	}

	/** {@link #guidance} of a soul whose own record is {@code soul} in the body whose record is {@code body}. */
	public static double guidance(PlayerCultivation soul, PlayerCultivation body) {
		int gap = soulRank(soul) - TwoPaths.higherRank(body, false);
		return 1 + Math.max(0, gap) / (double) Stage.values().length;
	}

	/** Where a soul stands, for what it brings a body (#guidance, #OPENS_MERIDIANS): its own path's true rank (PlayerCultivation#soulPath). */
	private static int soulRank(PlayerCultivation soul) {
		return Math.max(0, TwoPaths.rank(soul, soul.soulPath(), false));
	}

	/** Who {@code player} is to the world's people: the one whose body they are in, or themselves (for regard, sects and the like). */
	public static UUID identity(Player player) {
		if (BY_SOUL.isEmpty()) return player.getUUID();
		Tenancy t = BY_SOUL.get(player.getUUID());
		return t == null ? player.getUUID() : t.body;
	}

	/** The sect of the body {@code soul} is in, or null. */
	public static UUID sectOfBody(UUID soul) {
		if (BY_SOUL.isEmpty()) return null;
		Tenancy t = BY_SOUL.get(soul);
		return t == null ? null : t.sect;
	}

	/** The souls in bodies of {@code sect} (its barrier lets them through as its own: SectMembership#guestsOf). */
	public static List<UUID> tenantsOf(UUID sect) {
		List<UUID> found = new ArrayList<>();
		for (Tenancy t : BY_SOUL.values()) if (sect.equals(t.sect)) found.add(t.soul);
		return found;
	}

	/** The NPC whose body {@code soul} is in, or null. */
	public static CultivatorNpc npcBody(ServerPlayer soul) {
		Tenancy t = BY_SOUL.isEmpty() ? null : BY_SOUL.get(soul.getUUID());
		return t == null || !t.isNpc() ? null : findNpc(soul.server, t);
	}

	/** The player whose body {@code soul} is in, or null (an NPC's, or its player away: see {@link #awayOwner}). */
	public static ServerPlayer playerBody(ServerPlayer soul) {
		Tenancy t = BY_SOUL.isEmpty() ? null : BY_SOUL.get(soul.getUUID());
		return t == null ? null : t.heldPlayer;
	}

	/** The player whose body {@code soul} is in while they are away (logged out, the body still held), or null. */
	public static UUID awayOwner(ServerPlayer soul) {
		Tenancy t = BY_SOUL.isEmpty() ? null : BY_SOUL.get(soul.getUUID());
		return t == null || t.isNpc() || t.heldPlayer != null ? null : t.body;
	}

	/** The body {@code soul} is in, as itself (the player watching it, or the NPC set aside), or null. */
	public static LivingEntity bodyEntity(ServerPlayer soul) {
		ServerPlayer player = playerBody(soul);
		return player != null ? player : npcBody(soul);
	}

	/** The soul in {@code body}'s body (a player or an NPC), or null. */
	public static ServerPlayer tenantOf(UUID body) {
		Tenancy t = BY_BODY.isEmpty() ? null : BY_BODY.get(body);
		return t == null ? null : t.soulPlayer;
	}

	/** The name {@code player} goes by: the body's, while their soul is in one (chat, death messages, the name over its head); else null. */
	public static Component displayName(Player player) {
		if (player.level().isClientSide) return clientName.apply(player);
		Tenancy t = BY_SOUL.isEmpty() ? null : BY_SOUL.get(player.getUUID());
		if (t == null || !(player instanceof ServerPlayer soul)) return null;
		if (!t.isNpc()) return t.heldPlayer != null ? t.heldPlayer.getDisplayName() : t.bodyName;
		CultivatorNpc npc = findNpc(soul.server, t);
		return npc == null ? null : nameOf(npc);
	}

	/** An NPC cultivator's name, as its nameplate gives it: what a name tag called it, or the one it was born with. */
	public static Component nameOf(CultivatorNpc npc) {
		return npc.hasCustomName() ? npc.getCustomName() : Component.literal(npc.getPersonalName());
	}

	/**
	 * Whether {@code viewer}'s view may be set to {@code camera} (the ServerPlayer mixin): always, unless their body is held, when it
	 * stays on the one holding it (sneaking, clicking another, a command: all refused) until it is given back.
	 */
	public static boolean mayView(ServerPlayer viewer, Entity camera) {
		if (moving || !isHeld(viewer.getUUID())) return true;
		return camera == BY_BODY.get(viewer.getUUID()).soulPlayer;
	}

	/**
	 * What a held player's own entity answers to the rest of the mod while their body is lent: a mortal with nothing, made anew each
	 * time, so nothing of theirs is reckoned twice (the soul plays it) and nothing can build up on it (no ghostly corruption, no
	 * qi) while they only watch.
	 */
	public static PlayerCultivation sealed() {
		PlayerCultivation c = new PlayerCultivation();
		c.setMortal(true);
		return c;
	}

	// --- Taking a body, and giving it back ---

	/**
	 * {@code soul}'s soul takes {@code target}'s body (a living player's, or a living NPC cultivator's): checked already by
	 * SoulProjection#tryPossess. The soul's own things are put by, the body's are the soul's, the soul stands where the body stood;
	 * the player watches, the NPC is set aside. False (nothing changed) if it can't.
	 */
	static boolean enter(ServerPlayer soul, LivingEntity target) {
		if (inhabits(soul.getUUID()) || holds(target.getUUID()) || !target.isAlive()) return false;
		ServerPlayer victim = target instanceof ServerPlayer p ? p : null;
		CultivatorNpc npc = target instanceof CultivatorNpc n && !n.isGhost() ? n : null;
		if (victim == null && npc == null) return false;
		MinecraftServer server = soul.server;
		PlayerCultivation own = CultivationManager.own(soul);

		CompoundTag body;
		PlayerCultivation record;
		UUID sect;
		if (victim != null) {
			victim.stopRiding();
			if (victim.isSleeping()) victim.stopSleepInBed(true, true);
			if (MeditationManager.isMeditating(victim.getUUID())) MeditationManager.stop(victim, true);
			victim.stopUsingItem();
			endDealings(victim); // what was on their cursor or crafting grid goes back into their things first, to go with the body
			body = takeFrom(victim);
			record = CultivationManager.own(victim);
			Sect disciple = SectMembership.sectOf(server, victim.getUUID());
			sect = disciple == null ? null : disciple.id;
			CompoundTag held = new CompoundTag();
			held.putString("Role", "Held");
			held.putUUID("By", soul.getUUID());
			held.putInt("Mode", victim.gameMode.getGameModeForPlayer().getId());
			record.setPossession(held);
		} else {
			body = takeFrom(npc);
			record = npc.cultivation();
			record.setAlignment(npc.getAlignment()); // its path is its own: what it is to the people it meets
			knowOwnMethod(npc, record);
			sect = npc.getSectId();
		}
		// A soul mighty enough forces a mortal body's meridians open as it goes in: theirs to keep (OPENS_MERIDIANS).
		boolean opened = record.isMortal() && !record.hasShatteredMeridians() && soulRank(own) >= OPENS_MERIDIANS;
		if (opened) {
			record.awaken(0);
			if (npc != null) npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY)); // its record is the one awoken
		}
		CompoundTag mine = new CompoundTag();
		mine.putString("Role", "Tenant");
		mine.putUUID("Body", target.getUUID());
		mine.putBoolean("Npc", npc != null);
		mine.put("Stash", takeFrom(soul));
		own.setPossession(mine);

		Tenancy t = new Tenancy(soul, victim, npc, record, sect);
		BY_SOUL.put(t.soul, t);
		BY_BODY.put(t.body, t);
		moving = true;
		try {
			// The soul stands where the body stood, as it stood (its view too).
			soul.stopRiding();
			ServerLevel level = (ServerLevel) target.level();
			if (soul.level() != level) {
				soul.teleportTo(level, target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
			} else {
				soul.connection.teleport(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
			}
			soul.connection.resetPosition();
			soul.setYHeadRot(target.getYHeadRot());
			soul.setDeltaMovement(target.getDeltaMovement());
			soul.fallDistance = target.fallDistance;
			soul.noPhysics = false;
			rebase(soul);
			CultivationManager.refresh(soul); // the body's strength: its record answers now
			giveTo(soul, body);
			if (victim != null) {
				victim.setGameMode(GameType.SPECTATOR);
				victim.setCamera(soul);
				rebase(victim);
			} else {
				npc.setVacant(true);
				npc.moveTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), soul.getXRot());
			}
		} finally {
			moving = false;
		}
		putLeaveOnWheel(t);
		CultivationManager.markDirty(server);
		CultivationManager.sync(soul);
		Spells.sync(soul);
		NetherworldManager.refresh(soul);
		RealmSuppressSystem.refreshNow();
		lookAll(soul, t);
		Component name = victim != null ? victim.getDisplayName() : nameOf(npc);
		soul.sendSystemMessage(Component.translatable(ModLang.MSG_POSSESS_INHABIT, name).withStyle(ChatFormatting.LIGHT_PURPLE));
		if (victim != null) {
			victim.sendSystemMessage(Component.translatable(ModLang.MSG_POSSESS_TAKEN, soul.getGameProfile().getName()).withStyle(ChatFormatting.DARK_PURPLE));
		}
		if (opened) { // as the elixir's opening shows: the first breath of qi through the body
			ServerLevel at = soul.serverLevel();
			at.sendParticles(ParticleTypes.END_ROD, soul.getX(), soul.getY() + 1.0, soul.getZ(), 40, 0.5, 0.8, 0.5, 0.08);
			at.playSound(null, soul.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
			soul.sendSystemMessage(Component.translatable(ModLang.MSG_POSSESS_MERIDIANS, name).withStyle(ChatFormatting.GOLD));
			if (victim != null) victim.sendSystemMessage(Component.translatable(ModLang.MSG_POSSESS_MERIDIANS_TAKEN).withStyle(ChatFormatting.GOLD));
		}
		double speed = guidance(own, record);
		if (!record.isMortal() && speed > 1) {
			String times = java.math.BigDecimal.valueOf(speed).stripTrailingZeros().toPlainString();
			soul.sendSystemMessage(Component.translatable(ModLang.MSG_POSSESS_GUIDANCE, times).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		return true;
	}

	/**
	 * {@code soul}'s soul leaves the body it is in: everything that is the body's goes back to it where the soul stands (a player
	 * gets their view, their game mode and their body back; an NPC stands there again), and the soul's own things come back to it.
	 * The soul is left there out of the body (SoulProjection makes it a soul again).
	 */
	static void leave(ServerPlayer soul) {
		Tenancy t = BY_SOUL.get(soul.getUUID());
		if (t == null) return;
		MinecraftServer server = soul.server;
		if (MeditationManager.isMeditating(soul.getUUID())) MeditationManager.stop(soul, false);
		soul.stopUsingItem();
		endDealings(soul); // whatever it had open, the talk or the bout it was in, were the body's
		takeLeaveOffWheel(t);
		CompoundTag body = takeFrom(soul);
		BY_SOUL.remove(t.soul);
		BY_BODY.remove(t.body);
		ServerLevel level = soul.serverLevel();
		moving = true;
		try {
			if (!t.isNpc()) {
				ServerPlayer victim = t.heldPlayer;
				PlayerCultivation theirs = CultivationManager.own(server, t.body);
				CompoundTag held = theirs.getPossession();
				GameType mode = held != null && held.contains("Mode") ? GameType.byId(held.getInt("Mode")) : GameType.SURVIVAL;
				if (victim != null && !victim.isRemoved() && !victim.hasDisconnected()) {
					theirs.setPossession(null);
					victim.setCamera(victim);
					victim.setGameMode(mode);
					if (victim.level() != level) {
						victim.teleportTo(level, soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), soul.getXRot());
					} else {
						victim.connection.teleport(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), soul.getXRot());
					}
					victim.connection.resetPosition();
					victim.setDeltaMovement(soul.getDeltaMovement());
					victim.fallDistance = soul.fallDistance;
					rebase(victim);
					CultivationManager.refresh(victim); // their own strength again
					giveTo(victim, body);
					Spells.sync(victim);
					NetherworldManager.refresh(victim);
					victim.sendSystemMessage(Component.translatable(ModLang.MSG_POSSESS_FREED).withStyle(ChatFormatting.LIGHT_PURPLE));
				} else {
					// Away (logged out, their body held on): it waits in their record for when they are back, and so does where it
					// was left, for them to wake there (onJoin).
					CompoundTag waiting = held == null ? new CompoundTag() : held;
					waiting.putString("Role", "Held");
					waiting.put("Returned", body);
					CompoundTag at = new CompoundTag();
					at.putString("Dimension", level.dimension().location().toString());
					at.putDouble("X", soul.getX());
					at.putDouble("Y", soul.getY());
					at.putDouble("Z", soul.getZ());
					at.putFloat("YRot", soul.getYRot());
					waiting.put("At", at);
					theirs.setPossession(waiting);
				}
			} else {
				CultivatorNpc npc = findNpc(server, t);
				if (npc != null && npc.level() != level) npc = carryNpc(npc, level, soul);
				if (npc != null) {
					npc.moveTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), 0);
					npc.setYHeadRot(soul.getYHeadRot());
					npc.setVacant(false);
					// A stage its cultivation came to while borrowed is its own now, and its way into the next (setRank starts a new
					// rank's afresh); and so is the path its deeds turned it to.
					int rank = t.record.isMortal() ? CultivatorNpc.MORTAL : PlayerCultivation.rank(t.record.getRealm(), t.record.getStage());
					double progress = t.record.getCultivation();
					if (rank != npc.getRank()) npc.setRank(rank);
					if (rank >= 0) npc.cultivation().setState(npc.cultivation().getRealm(), npc.cultivation().getStage(), progress);
					npc.setAlignment(t.record.getAlignment());
					giveTo(npc, body);
				} else {
					dropAll(soul, body); // it was taken from the world meanwhile: what it carried falls where its body stood
				}
			}
			PlayerCultivation own = CultivationManager.own(soul);
			CompoundTag mine = own.getPossession();
			own.setPossession(null);
			rebase(soul);
			CultivationManager.refresh(soul);
			if (mine != null && mine.contains("Stash")) giveTo(soul, mine.getCompound("Stash"));
		} finally {
			moving = false;
		}
		CultivationManager.markDirty(server);
		Spells.sync(soul);
		NetherworldManager.refresh(soul);
		RealmSuppressSystem.refreshNow();
		lookAll(soul, null);
	}

	/**
	 * The body dies under the soul: given back first (SoulProjection#onBodyDying), it dies now as its owner's own death, by the same
	 * blow: whatever struck it has the kill, and it falls with everything it carries (as their keepInventory says, for a player).
	 */
	static void kill(LivingEntity body, DamageSource source, ServerPlayer soul) {
		// Not "is it alive": given back at the blow's end, it has no health left already; only its death is still to happen.
		if (body.isRemoved()) return;
		if (source.getEntity() instanceof Player killer && killer != soul && killer != body) body.setLastHurtByPlayer(killer);
		body.getCombatTracker().recordDamage(source, Math.max(1, body.getHealth()));
		body.setHealth(0);
		body.die(source);
	}

	/**
	 * The body of {@code owner}, away while it was held, has died under {@code soul} (given back already: its things wait in their
	 * record, {@link #leave}): what it carried falls where it died (as their keepInventory says; kept, it waits for them), and they
	 * die as they come back ({@link #onJoin}).
	 */
	static void diedAway(ServerPlayer soul, UUID owner) {
		PlayerCultivation theirs = CultivationManager.own(soul.server, owner);
		CompoundTag waiting = theirs.getPossession();
		if (waiting == null) return;
		boolean keep = soul.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
		if (!keep && waiting.contains("Returned")) {
			CompoundTag body = waiting.getCompound("Returned");
			dropAll(soul, body);
			int experience = Math.min(100, body.getInt("XpLevel") * 7); // as a player's death lets it go
			if (experience > 0) ExperienceOrb.award(soul.serverLevel(), soul.position(), experience);
			waiting.remove("Returned");
		}
		waiting.putBoolean("Died", true);
		theirs.setPossession(waiting);
		CultivationManager.markDirty(soul.server);
	}

	/**
	 * Each tick of a soul in a body (SoulProjection): its owner kept watching it, an NPC kept by it, everyone told whose look it
	 * wears. False if the body is lost to it (its player gone, its NPC taken from the world).
	 */
	static boolean tick(ServerPlayer soul) {
		Tenancy t = BY_SOUL.get(soul.getUUID());
		if (t == null) return false;
		t.soulPlayer = soul;
		if (!t.isNpc()) {
			ServerPlayer victim = t.heldPlayer;
			if (victim != null && (victim.isRemoved() || victim.hasDisconnected())) {
				// Gone without a word; or killed (a command reaches even a spectator) and back as a new body of theirs: held all the same.
				ServerPlayer again = victim.hasDisconnected() ? null : soul.server.getPlayerList().getPlayer(t.body);
				t.heldPlayer = victim = again != null && again != victim && !again.isRemoved() ? again : null;
			}
			if (victim != null) {
				moving = true;
				try {
					// Nothing hands their view or their game back while the soul is in their body (a command, a respawn screen): put
					// back. Their body carried into another world, their view goes with it.
					if (victim.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) victim.setGameMode(GameType.SPECTATOR);
					watch(victim, soul);
				} finally {
					moving = false;
				}
			}
		} else {
			CultivatorNpc npc = findNpc(soul.server, t);
			if (npc == null || !npc.isAlive()) return false;
			if (!npc.isVacant()) npc.setVacant(true);
			// Its body carried into another world (a portal, a tear), it goes along, set aside still.
			if (npc.level() != soul.level()) npc = carryNpc(npc, soul.serverLevel(), soul);
			if (npc == null) return false;
			t.npc = npc;
			npc.moveTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), soul.getXRot());
		}
		if (soul.tickCount % 20 == 0) lookAll(soul, t);
		return true;
	}

	/**
	 * A held player's view put in their body's eyes, carried first into the world it is in: vanilla's own crossing hands a view back
	 * to its owner on the way, so watching across worlds at once would end on themselves. Called with {@link #moving} set.
	 */
	private static void watch(ServerPlayer victim, ServerPlayer soul) {
		if (victim.level() != soul.level()) victim.teleportTo(soul.serverLevel(), soul.getX(), soul.getY(), soul.getZ(), victim.getYRot(), victim.getXRot());
		if (victim.getCamera() != soul) victim.setCamera(soul);
	}

	// --- Logging out, crashes, stopping ---

	/**
	 * A player logs out: one whose body is held does NOT take it with them (no escaping by leaving: the user's ask, 2026-10-18). Their
	 * body plays on under the soul; they are saved as they are (a spectator with nothing), and coming back they watch it again
	 * ({@link #onJoin}), or, if it was given back while they were away, they have it back where it was left. A soul in a body is
	 * SoulProjection's (it goes home, leaving the body first).
	 */
	public static void onDisconnect(ServerPlayer player) {
		Tenancy t = BY_BODY.isEmpty() ? null : BY_BODY.get(player.getUUID());
		if (t == null || t.isNpc()) return;
		t.heldPlayer = null;
		t.soulPlayer.displayClientMessage(Component.translatable(ModLang.MSG_POSSESS_OWNER_AWAY, t.bodyName), true);
	}

	/**
	 * Joining with a possession still on record (a crash, or a server killed mid-way: a logout or a stop gives everything back first).
	 * A soul that was in a body has its own things back, and what it held of the body's goes to the body's owner (waiting in their
	 * record if they're away) or, an NPC's, falls where the body stood. A player whose body was held has their game mode back, and
	 * their body's things if they're waiting, a tick on ({@link #rejoined}). Called before SoulProjection#onJoin brings the soul
	 * home, from Fabric's join event: before the player is in the world, so nothing here may move them or their view.
	 */
	public static void onJoin(ServerPlayer player) {
		// Back while their body is still held: watching it again, as they left off. Their view is put back on the soul's next tick
		// (#tick): this is called before the world has taken them in, and moving their view now would break their login.
		Tenancy held = BY_BODY.isEmpty() ? null : BY_BODY.get(player.getUUID());
		if (held != null && !held.isNpc()) {
			held.heldPlayer = player;
			player.sendSystemMessage(Component.translatable(ModLang.MSG_POSSESS_STILL_HELD, held.soulPlayer.getGameProfile().getName())
					.withStyle(ChatFormatting.DARK_PURPLE));
			held.soulPlayer.displayClientMessage(Component.translatable(ModLang.MSG_POSSESS_OWNER_BACK, player.getDisplayName()), true);
			return;
		}
		PlayerCultivation own = CultivationManager.own(player);
		CompoundTag tag = own.getPossession();
		if (tag == null || inhabits(player.getUUID())) return;
		if ("Tenant".equals(tag.getString("Role"))) {
			own.setPossession(null);
			CultivationManager.markDirty(player.server);
			CompoundTag body = takeFrom(player);
			if (tag.getBoolean("Npc") || !tag.hasUUID("Body")) dropAll(player, body);
			else returnTo(player.server, tag.getUUID("Body"), body);
			if (tag.contains("Stash")) giveTo(player, tag.getCompound("Stash"));
			return;
		}
		// One whose body was held, given back meanwhile: set right on the next tick, once they stand in the world (moving them, or
		// their death, before it has taken them in breaks their login).
		REJOINED.add(player.getUUID());
	}

	/** Players back whose body was given back while they were away (or held when a crash struck), for {@link #settle} to set right. */
	private static final Set<UUID> REJOINED = new HashSet<>();

	/** A player whose body was held, back in the world: their game mode, their view, and their body as it was given back. */
	private static void rejoined(ServerPlayer player) {
		PlayerCultivation own = CultivationManager.own(player);
		CompoundTag tag = own.getPossession();
		if (tag == null || isHeld(player.getUUID()) || inhabits(player.getUUID()) || !"Held".equals(tag.getString("Role"))) return;
		own.setPossession(null);
		CultivationManager.markDirty(player.server);
		if (tag.contains("Mode")) player.setGameMode(GameType.byId(tag.getInt("Mode")));
		if (player.getCamera() != player) player.setCamera(player);
		// Soul Projection put on their wheel to leave by, if it was left there.
		int slot = tag.contains("WheelSlot") ? tag.getInt("WheelSlot") : -1;
		if (slot >= 0 && slot < PlayerCultivation.WHEEL_SLOTS && own.getWheelSlot(slot) == Ability.SOUL_PROJECTION) own.setWheelSlot(slot, null);
		// Given back while they were away: they wake where it was left.
		if (tag.contains("At")) {
			CompoundTag at = tag.getCompound("At");
			net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(at.getString("Dimension"));
			ServerLevel level = id == null ? null : player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
			if (level != null) player.teleportTo(level, at.getDouble("X"), at.getDouble("Y"), at.getDouble("Z"), at.getFloat("YRot"), 0);
		}
		CultivationManager.refresh(player); // their own strength, before their body's health goes back on
		if (tag.contains("Returned")) {
			// What their body was, all of it, if they come back empty-handed as they left; else its things alongside theirs.
			if (player.getInventory().isEmpty()) giveTo(player, tag.getCompound("Returned"));
			else addAll(player, tag.getCompound("Returned"));
		}
		Spells.sync(player);
		NetherworldManager.refresh(player);
		if (tag.getBoolean("Died")) {
			// Their body died while another's soul had it, and they away: they come back to that death.
			player.sendSystemMessage(Component.translatable(ModLang.MSG_POSSESS_DIED_AWAY).withStyle(ChatFormatting.DARK_PURPLE));
			player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
			return;
		}
		if (tag.contains("Returned")) player.sendSystemMessage(Component.translatable(ModLang.MSG_POSSESS_FREED).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** The server stopped: nothing carries over (every soul left its body as the server stopped: SoulProjection#returnAll). */
	public static void clear() {
		BY_SOUL.clear();
		BY_BODY.clear();
		SETTLING.clear();
		REJOINED.clear();
	}

	// --- What goes with a body ---

	/**
	 * Everything that goes with {@code p}'s body as one tag, taken off them (left with nothing): what they carry, wear and hold (and
	 * their rings), their health, hunger, experience and effects, their air, fire and frost.
	 */
	static CompoundTag takeFrom(ServerPlayer p) {
		CompoundTag tag = new CompoundTag();
		Inventory inventory = p.getInventory();
		tag.put("Inventory", inventory.save(new ListTag()));
		tag.putInt("Selected", inventory.selected);
		inventory.clearContent();
		RingContainer rings = RingContainer.of(p);
		tag.put("Rings", rings.save());
		rings.clearContent();
		tag.putFloat("Health", p.getHealth());
		tag.putFloat("MaxHealth", p.getMaxHealth());
		tag.putFloat("Absorption", p.getAbsorptionAmount());
		p.setAbsorptionAmount(0);
		CompoundTag food = new CompoundTag();
		p.getFoodData().addAdditionalSaveData(food);
		tag.put("Food", food);
		tag.putInt("XpLevel", p.experienceLevel);
		tag.putFloat("XpProgress", p.experienceProgress);
		tag.putInt("XpTotal", p.totalExperience);
		p.experienceProgress = 0;
		p.totalExperience = 0;
		p.setExperienceLevels(0);
		ListTag effects = new ListTag();
		for (MobEffectInstance effect : p.getActiveEffects()) effects.add(effect.save(new CompoundTag()));
		tag.put("Effects", effects);
		p.removeAllEffects();
		tag.putInt("Air", p.getAirSupply());
		tag.putInt("Fire", p.getRemainingFireTicks());
		tag.putInt("Frozen", p.getTicksFrozen());
		p.clearFire();
		p.setTicksFrozen(0);
		return tag;
	}

	/** An NPC's body as one tag (as {@link #takeFrom(ServerPlayer)}'s): what it wears and holds, what it won, its health and effects. */
	static CompoundTag takeFrom(CultivatorNpc npc) {
		CompoundTag tag = new CompoundTag();
		ListTag items = new ListTag();
		put(items, npc.getItemBySlot(EquipmentSlot.MAINHAND), 0);
		put(items, npc.getItemBySlot(EquipmentSlot.FEET), 100);
		put(items, npc.getItemBySlot(EquipmentSlot.LEGS), 101);
		put(items, npc.getItemBySlot(EquipmentSlot.CHEST), 102);
		put(items, npc.getItemBySlot(EquipmentSlot.HEAD), 103);
		put(items, npc.getItemBySlot(EquipmentSlot.OFFHAND), 150);
		for (EquipmentSlot slot : EquipmentSlot.values()) npc.setItemSlot(slot, ItemStack.EMPTY);
		// What it won in spars: in the pack (slots 9-35), then the hotbar beside its weapon (1-8); any more falls at its feet.
		int next = 9;
		for (ItemStack won : npc.takeWinnings()) {
			if (next < 0) {
				npc.spawnAtLocation(won);
				continue;
			}
			put(items, won, next);
			next = next == 35 ? 1 : next == 8 ? -1 : next + 1;
		}
		tag.put("Inventory", items);
		tag.putInt("Selected", 0);
		tag.putFloat("Health", npc.getHealth());
		tag.putFloat("MaxHealth", npc.getMaxHealth());
		tag.putBoolean("Scaled", true); // an NPC's strength is reckoned apart from a player's: its health goes over as its share of the whole
		CompoundTag food = new CompoundTag();
		new FoodData().addAdditionalSaveData(food); // it hungers for nothing: a fed body
		tag.put("Food", food);
		ListTag effects = new ListTag();
		for (MobEffectInstance effect : npc.getActiveEffects()) effects.add(effect.save(new CompoundTag()));
		tag.put("Effects", effects);
		npc.removeAllEffects();
		tag.putInt("Air", npc.getAirSupply());
		tag.putInt("Fire", npc.getRemainingFireTicks());
		return tag;
	}

	private static void put(ListTag items, ItemStack stack, int slot) {
		if (stack == null || stack.isEmpty()) return;
		CompoundTag one = stack.save(new CompoundTag());
		one.putByte("Slot", (byte) slot);
		items.add(one);
	}

	/**
	 * {@code body} (from {@link #takeFrom}) onto {@code p}, in place of whatever they had: a player's health as it was (held to while
	 * what it can hold settles: {@link #settle}), an NPC's by its share of the whole.
	 */
	static void giveTo(ServerPlayer p, CompoundTag body) {
		Inventory inventory = p.getInventory();
		inventory.clearContent();
		inventory.load(body.getList("Inventory", Tag.TAG_COMPOUND));
		inventory.selected = Mth.clamp(body.getInt("Selected"), 0, Inventory.getSelectionSize() - 1);
		// The hand's slot is the client's to say (it tells the server when it changes): it is told, or it would show one slot and use another.
		if (p.connection != null) p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(inventory.selected));
		RingContainer rings = RingContainer.of(p);
		rings.clearContent();
		rings.load(body.getList("Rings", Tag.TAG_COMPOUND));
		float health = healthFor(body, p.getMaxHealth());
		p.setHealth(health);
		if (!body.getBoolean("Scaled") && health > 0) {
			SETTLING.put(p.getUUID(), new Settling(body.getFloat("Health"), p.getHealth(), p.server.getTickCount() + SETTLE_TICKS));
		}
		p.setAbsorptionAmount(body.getFloat("Absorption"));
		if (body.contains("Food")) p.getFoodData().readAdditionalSaveData(body.getCompound("Food"));
		p.experienceProgress = body.getFloat("XpProgress");
		p.totalExperience = body.getInt("XpTotal");
		p.setExperienceLevels(body.getInt("XpLevel")); // and its client told
		p.removeAllEffects();
		for (Tag entry : body.getList("Effects", Tag.TAG_COMPOUND)) {
			MobEffectInstance effect = MobEffectInstance.load((CompoundTag) entry);
			if (effect != null) p.addEffect(effect);
		}
		p.setAirSupply(body.contains("Air") ? body.getInt("Air") : p.getMaxAirSupply());
		p.setRemainingFireTicks(body.getInt("Fire"));
		p.setTicksFrozen(body.getInt("Frozen"));
		p.inventoryMenu.broadcastChanges();
	}

	/**
	 * {@code body} back onto an NPC: what was in the hand and what is worn, in its hand and on it; the rest among what it carries (its
	 * winnings, dropped when it falls); experience it can't keep falls as orbs; its health by its share.
	 */
	static void giveTo(CultivatorNpc npc, CompoundTag body) {
		int selected = body.getInt("Selected");
		for (Tag entry : body.getList("Inventory", Tag.TAG_COMPOUND)) {
			CompoundTag one = (CompoundTag) entry;
			ItemStack stack = ItemStack.of(one);
			if (stack.isEmpty()) continue;
			int slot = one.getByte("Slot") & 255;
			EquipmentSlot worn = slot == selected ? EquipmentSlot.MAINHAND : switch (slot) {
				case 100 -> EquipmentSlot.FEET;
				case 101 -> EquipmentSlot.LEGS;
				case 102 -> EquipmentSlot.CHEST;
				case 103 -> EquipmentSlot.HEAD;
				case 150 -> EquipmentSlot.OFFHAND;
				default -> null;
			};
			if (worn != null && npc.getItemBySlot(worn).isEmpty()) npc.setItemSlot(worn, stack);
			else npc.addWinnings(stack);
		}
		npc.setHealth(healthFor(body, npc.getMaxHealth()));
		npc.removeAllEffects();
		for (Tag entry : body.getList("Effects", Tag.TAG_COMPOUND)) {
			MobEffectInstance effect = MobEffectInstance.load((CompoundTag) entry);
			if (effect != null) npc.addEffect(effect);
		}
		npc.setRemainingFireTicks(body.getInt("Fire"));
		int experience = body.getInt("XpTotal");
		if (experience > 0 && npc.level() instanceof ServerLevel level) ExperienceOrb.award(level, npc.position(), experience);
	}

	/**
	 * The health a body comes to on {@code max}: a player's own (as far as {@code max} holds it for now), an NPC's the same share of
	 * the whole as it had (none if none).
	 */
	private static float healthFor(CompoundTag body, float max) {
		float health = body.getFloat("Health");
		float was = body.getFloat("MaxHealth");
		if (health <= 0) return 0;
		if (!body.getBoolean("Scaled") || was <= 0 || Math.abs(was - max) < 1.0e-3f) return Math.min(health, max);
		return Math.max(0.01f, Math.min(max, health / was * max));
	}

	/**
	 * A player's health given with their body, held to for a moment: what a body can hold is reckoned by several of the mod's systems,
	 * each on its own tick, so just after a body changes hands its whole may stand a little off (and the health be cut to it). Until it
	 * settles, health untouched since is set again to what was given; any blow or healing meanwhile leaves it be.
	 */
	private record Settling(float target, float given, int until) {}

	private static final Map<UUID, Settling> SETTLING = new HashMap<>();
	private static final int SETTLE_TICKS = 40;

	/**
	 * Each server tick: players back whose body was given back while they were away set right ({@link #rejoined}), and health given
	 * with a body held to while its whole settles ({@link Settling}).
	 */
	public static void settle(MinecraftServer server) {
		if (!REJOINED.isEmpty()) {
			for (UUID id : new ArrayList<>(REJOINED)) {
				REJOINED.remove(id);
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p != null && !p.hasDisconnected()) rejoined(p);
			}
		}
		if (SETTLING.isEmpty()) return;
		SETTLING.entrySet().removeIf(entry -> {
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			Settling s = entry.getValue();
			if (p == null || !p.isAlive() || server.getTickCount() > s.until() || p.getHealth() != s.given()) return true;
			float want = Math.min(s.target(), p.getMaxHealth());
			if (want != p.getHealth()) {
				p.setHealth(want);
				entry.setValue(new Settling(s.target(), p.getHealth(), s.until()));
			}
			return false;
		});
	}

	/** Everything {@code body} carries, into {@code p}'s things alongside their own (what won't fit falls at their feet). */
	private static void addAll(ServerPlayer p, CompoundTag body) {
		Inventory loaded = new Inventory(p);
		loaded.load(body.getList("Inventory", Tag.TAG_COMPOUND));
		for (int i = 0; i < loaded.getContainerSize(); i++) {
			ItemStack stack = loaded.getItem(i);
			if (!stack.isEmpty()) p.getInventory().placeItemBackInInventory(stack);
		}
		RingContainer rings = new RingContainer();
		rings.load(body.getList("Rings", Tag.TAG_COMPOUND));
		for (int i = 0; i < rings.getContainerSize(); i++) {
			ItemStack stack = rings.getItem(i);
			if (!stack.isEmpty()) p.getInventory().placeItemBackInInventory(stack);
		}
	}

	/** Everything {@code body} carries falls where {@code at} stands. */
	private static void dropAll(ServerPlayer at, CompoundTag body) {
		Inventory loaded = new Inventory(at);
		loaded.load(body.getList("Inventory", Tag.TAG_COMPOUND));
		for (int i = 0; i < loaded.getContainerSize(); i++) {
			ItemStack stack = loaded.getItem(i);
			if (!stack.isEmpty()) at.drop(stack, true, false);
		}
		for (Tag entry : body.getList("Rings", Tag.TAG_COMPOUND)) {
			ItemStack stack = ItemStack.of((CompoundTag) entry);
			if (!stack.isEmpty()) at.drop(stack, true, false);
		}
	}

	/** A crashed possession's body things back to their owner: at once if they're here and free, else waiting in their record. */
	private static void returnTo(MinecraftServer server, UUID owner, CompoundTag body) {
		ServerPlayer online = server.getPlayerList().getPlayer(owner);
		if (online != null && !isHeld(owner)) {
			addAll(online, body);
			return;
		}
		PlayerCultivation theirs = CultivationManager.own(server, owner);
		CompoundTag held = theirs.getPossession();
		if (held == null) held = new CompoundTag();
		held.putString("Role", "Held");
		held.put("Returned", body);
		theirs.setPossession(held);
		CultivationManager.markDirty(server);
	}

	// --- Helpers ---

	/**
	 * Whatever {@code p} was in the middle of with others as the body changes hands: a screen open (its cursor and crafting grid back
	 * into their things), a talk with an NPC cultivator, a bout (forfeited).
	 */
	private static void endDealings(ServerPlayer p) {
		p.closeContainer();
		for (CultivatorNpc npc : CultivatorNpc.talkingWith(p)) {
			npc.endTalk(p);
			TalkSystem.breakOff(p, npc, ModLang.TALK_INTERRUPTED);
		}
		SparSystem.abandon(p);
	}

	/** The NPC of {@code t}, wherever it is now (a crossing into another world makes it anew, under the same id). */
	private static CultivatorNpc findNpc(MinecraftServer server, Tenancy t) {
		if (t.npc != null && !t.npc.isRemoved()) return t.npc;
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getEntity(t.body) instanceof CultivatorNpc npc && !npc.isRemoved()) {
				t.npc = npc;
				return npc;
			}
		}
		return null;
	}

	/**
	 * An NPC brought into {@code level} where {@code soul} stands (its body went through a portal or a tear): made anew there under
	 * the same id, as vanilla's own crossing does, and kept by hand, since the land it comes to may not be loaded yet (looked up by
	 * its id, it isn't there until it is).
	 */
	private static CultivatorNpc carryNpc(CultivatorNpc npc, ServerLevel level, ServerPlayer soul) {
		if (!(npc.getType().create(level) instanceof CultivatorNpc moved)) return null;
		npc.unRide();
		moved.restoreFrom(npc);
		moved.moveTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), 0);
		moved.setYHeadRot(soul.getYRot());
		npc.setRemoved(net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION);
		level.addDuringTeleport(moved);
		return moved;
	}

	/** An NPC's body knows its own heaven method (CultivatorNpc#heavenMethod), as far as its cultivation goes, its spells on the wheel. */
	private static void knowOwnMethod(CultivatorNpc npc, PlayerCultivation record) {
		HeavenMethod method = npc.heavenMethod();
		if (method == null || record.isMortal() || record.heaven().knows(method)) return;
		record.heaven().setMethod(method, Math.max(1, HeavenMethods.understoodTo(record, method)), 0);
		record.heaven().setActiveMethod(method);
		for (Spell spell : Spell.values()) if (spell.isHeaven() && spell.isKnown(record)) record.netherworld().addToWheel(spell);
	}

	/**
	 * What each player's systems last noted of them, forgotten as they change bodies (or get theirs back): the next look takes what
	 * they are as it finds it, with no word of flight, sense, forms or spells newly "gained".
	 */
	private static void rebase(ServerPlayer p) {
		UUID id = p.getUUID();
		QiFlight.forget(id);
		QiSense.forget(id);
		TwoPaths.forget(id);
		HeavenMethods.forget(id);
		NetherworldManager.forget(id);
		SectMembership.forget(id);
		ConsciousnessDomainHandler.forget(id);
		Tempering.forget(id);
	}

	/** Soul Projection on the body's ability wheel, to leave it by, if it isn't there and a slot is free. */
	private static void putLeaveOnWheel(Tenancy t) {
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) if (t.record.getWheelSlot(i) == Ability.SOUL_PROJECTION) return;
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) {
			if (t.record.getWheelSlot(i) == null) {
				t.record.setWheelSlot(i, Ability.SOUL_PROJECTION);
				t.wheelSlot = i;
				// Noted with their possession, so a crash leaves their own wheel as it was (onJoin takes it off again).
				if (!t.isNpc() && t.record.getPossession() != null) t.record.getPossession().putInt("WheelSlot", i);
				return;
			}
		}
	}

	private static void takeLeaveOffWheel(Tenancy t) {
		if (t.wheelSlot >= 0 && t.record.getWheelSlot(t.wheelSlot) == Ability.SOUL_PROJECTION) t.record.setWheelSlot(t.wheelSlot, null);
		t.wheelSlot = -1;
	}

	/** Everyone in {@code soul}'s world told whose look it wears ({@code t} null: its own again). */
	private static void lookAll(ServerPlayer soul, Tenancy t) {
		if (t != null && !t.isNpc()) {
			// Their name and skin go with it, for clients that won't have them in the list while they're away.
			com.mojang.authlib.properties.Property textures = t.bodyProfile.getProperties().get("textures").stream().findFirst().orElse(null);
			Component name = t.heldPlayer != null ? t.heldPlayer.getDisplayName() : t.bodyName;
			ModPackets.broadcastBodyLook(soul, t.body, name, textures, -1);
			return;
		}
		CultivatorNpc npc = t == null ? null : findNpc(soul.server, t);
		ModPackets.broadcastBodyLook(soul, null, null, null, npc == null ? -1 : npc.getId());
	}

	private Possession() {}
}
