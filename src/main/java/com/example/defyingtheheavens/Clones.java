package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Clone (Technique#CLONE, Ability#CLONE; the user's ask, 2026-10-23): a second body split off one's own, from Nascent Soul, by the
 * living.
 * <ul>
 *   <li><b>Splitting it off</b> takes {@link #SUMMON_SHARE} of the qi one has: that is the clone's to begin with. It stands beside
 *   its maker wearing and holding nothing (nothing is split off but the body).</li>
 *   <li><b>The clone</b> is its maker at their own realm, with all their abilities and spells, but half their strength ({@link
 *   #STAT_SHARE}: the user's call, 2026-10-23). It is played as oneself, a player like any other: no tether, nothing paid for it
 *   being out, anywhere in any realm. It is played on its maker's own record ({@link #isCloneRecord}): what it learns, does and
 *   earns is theirs (methods, techniques, pills, alignment, wishes granted; the bug sweep of 2026-10-23: a copy played in its place
 *   lost all of it on the way back). Its pool is its own, half theirs; the main body's is kept apart while it is played (its
 *   tag's "MainQi"), gathering where the main body sits (#feed), and what the clone spends is made up from it. It doesn't
 *   meditate, project its soul, break through, detonate a core nor split off clones of its own.</li>
 *   <li><b>Going into it</b> (the ability again) leaves the main body sitting where it was, in meditation (a CloneBodyEntity with
 *   its health, armour and gear; its land kept loaded); <b>going back</b> leaves the clone standing where it was. What each carries
 *   is its own, kept in its maker's record while the other is played (PlayerCultivation#getClone).</li>
 *   <li><b>Drawing it back in</b> (sneaking, the other body within {@link #ABSORB_RANGE}): its qi and its things come back to its
 *   maker, who is in their main body after.</li>
 *   <li>A clone slain dissolves, its things falling where it fell; played, its player wakes in the main body. The main body slain,
 *   its player dies (in their main body, where it sat) and the clone dissolves with them, as it does whenever they die, or their
 *   life ends whole (Lifespan#wipe). Logging out in the clone, or the server stopping, puts its player back in their main body
 *   first; a crash is put right as they come back ({@link #onJoin}).</li>
 * </ul>
 */
public final class Clones {
	/** The share of its maker's qi that splitting a clone off takes (the clone's own to begin with). */
	public static final double SUMMON_SHARE = 0.5;
	/** How near the other body must be for the clone to be drawn back in (blocks). */
	public static final double ABSORB_RANGE = 8;
	/**
	 * A clone lost (slain, played or standing; given way to the Netherworld's yin), another can't be split off for this long (5
	 * minutes; the user's ask after the bug sweep of 2026-10-23: a clone's death cost nearly nothing). Drawn back in, none is lost.
	 */
	public static final long REFORM_TICKS = 5 * 60 * 20;
	private static final int MAIN_TICKET_RADIUS = 2;
	private static final TicketType<ChunkPos> MAIN_TICKET = TicketType.create("defying-the-heavens:clone_main", Comparator.comparingLong(ChunkPos::toLong));

	/** The players in their clones now, and their own records, played at the clone's strength while they are ({@link #strength}). */
	private static final Map<UUID, PlayerCultivation> IN = new HashMap<>();
	/** Players back from a crash that left them in their clone (#onJoin), set right a tick on, once they stand in the world. */
	private static final Set<UUID> REJOINED = new HashSet<>();
	/** Client hook (set by the client): whether this record is the local player's while they play their clone. */
	public static Predicate<PlayerCultivation> clientCloneRecord = c -> false;

	// --- Queries ---

	public static boolean inClone(ServerPlayer p) {
		return !IN.isEmpty() && IN.containsKey(p.getUUID());
	}

	/**
	 * The qi in {@code p}'s main body's pool: kept apart while they play their clone (its tag's "MainQi", gathering: {@link #feed}),
	 * else their pool as it is.
	 */
	public static double mainQi(ServerPlayer p) {
		PlayerCultivation own = CultivationManager.own(p);
		CompoundTag tag = own.getClone();
		return inClone(p) && tag != null && tag.contains("MainQi") ? tag.getDouble("MainQi") : own.getQi();
	}

	/** Whether {@code c} is being played as a clone: its owner's record while they are in their clone (server), or the local player's (client). */
	public static boolean isCloneRecord(PlayerCultivation c) {
		for (PlayerCultivation played : IN.values()) if (played == c) return true;
		return clientCloneRecord.test(c);
	}

	/** Whether {@code p} has a clone out (played or standing). */
	public static boolean hasClone(ServerPlayer p) {
		return CultivationManager.own(p).getClone() != null;
	}

	/** Whether {@code owner}'s record still has {@code entity} as the body it isn't playing ({@code sitting}: the main body). */
	static boolean claims(MinecraftServer server, UUID owner, UUID entity, boolean sitting) {
		CompoundTag tag = CultivationManager.own(server, owner).getClone();
		return tag != null && tag.hasUUID("Entity") && tag.getUUID("Entity").equals(entity) && tag.getBoolean("In") == sitting;
	}

	/**
	 * What share of its maker's strength a clone has (the user's call, 2026-10-23: half their stats, not half their realm, or a True
	 * Immortal's clone would be a mere Four Axis cultivator): its health, the strength of its blows, its armour, toughness and footing,
	 * and the depth of its qi pool (CultivationStats#apply, PlayerCultivation#maxQi). Its realm, speed, flight, abilities and spells are
	 * its maker's own.
	 */
	public static final double STAT_SHARE = 0.5;

	/** The share of its strength {@code c} has: {@link #STAT_SHARE} while it is played as a clone, else the whole. */
	public static double strength(PlayerCultivation c) {
		return isCloneRecord(c) ? STAT_SHARE : 1.0;
	}

	/** The qi the clone not played now holds (its tag's "Qi"; one split off before the sweep of 2026-10-23 kept a whole record). */
	private static double cloneQi(CompoundTag tag) {
		return tag.contains("Qi") ? tag.getDouble("Qi") : tag.getCompound("Record").getDouble("Qi");
	}

	// --- The ability ---

	/**
	 * The ability (CultivationManager#toggleAbility): with no clone out, split one off; with one standing, go into it; in it, go back;
	 * sneaking with the other body near, draw the clone back in.
	 */
	public static void act(ServerPlayer p) {
		String busy = busy(p);
		if (busy != null) {
			p.displayClientMessage(Component.translatable(busy), true);
			return;
		}
		CompoundTag tag = CultivationManager.own(p).getClone();
		if (tag != null && p.isShiftKeyDown() && near(p, tag)) {
			absorb(p);
			return;
		}
		if (inClone(p)) back(p, true);
		else if (tag == null) split(p);
		else into(p);
	}

	/** Why {@code p} can't use the ability now (the line), or null: out of their body or in another's, mid-tribulation, a core breaking. */
	private static String busy(ServerPlayer p) {
		if (!p.isAlive() || p.isSpectator()) return ModLang.MSG_SOUL_BUSY;
		if (SoulProjection.isActive(p.getUUID()) || Possession.inhabits(p.getUUID()) || Possession.isHeld(p.getUUID()) || InnerRealm.isInside(p)
				|| TribulationManager.isActive(p.getUUID()) || CoreDetonation.isCharging(p.getUUID())) {
			return ModLang.MSG_CLONE_BUSY;
		}
		return null;
	}

	/** Whether the body {@code p} isn't playing stands within {@link #ABSORB_RANGE} of them, in their world. */
	private static boolean near(ServerPlayer p, CompoundTag tag) {
		CloneBodyEntity other = standIn(p.server, tag);
		Vec3 at = other != null ? other.position() : posOf(tag);
		return dimensionOf(tag) == p.level().dimension() && p.position().distanceTo(at) <= ABSORB_RANGE;
	}

	/** A clone split off {@code p}, standing beside them with half their qi; not while one lost is still being reformed ({@link #REFORM_TICKS}). */
	private static void split(ServerPlayer p) {
		PlayerCultivation own = CultivationManager.own(p);
		long wait = own.cloneReadyAt() - p.server.overworld().getGameTime();
		if (wait > 0) {
			long seconds = (wait + 19) / 20;
			p.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_REFORMING, String.format("%d:%02d", seconds / 60, seconds % 60))
					.withStyle(ChatFormatting.RED), true);
			return;
		}
		double taken = own.getQi() * SUMMON_SHARE;
		own.setQi(own.getQi() - taken);
		ServerLevel level = p.serverLevel();
		Vec3 at = beside(p);
		float health = (float) (p.getMaxHealth() * STAT_SHARE);
		CloneBodyEntity body = CloneBodyEntity.split(p, at.x, at.y, at.z, p.getYRot(), health, (float) (p.getArmorValue() * STAT_SHARE));
		level.addFreshEntity(body);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("In", false);
		tag.putUUID("Entity", body.getUUID());
		place(tag, level, at, p.getYRot());
		CompoundTag things = new CompoundTag();
		things.putBoolean("Fresh", true); // nothing on it, its health whole as it is first played
		tag.put("Body", things);
		tag.putDouble("Qi", Math.min(taken, own.maxQi() * STAT_SHARE)); // its pool, half its maker's
		own.setClone(tag);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.0, at.z, 40, 0.3, 0.8, 0.3, 0.05);
		level.playSound(null, BlockPos.containing(at), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 1.0f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_SPLIT).withStyle(ChatFormatting.LIGHT_PURPLE), true);
	}

	/** A spot for a new clone a step before {@code p}, if there is room there; else where they stand. */
	private static Vec3 beside(ServerPlayer p) {
		Vec3 ahead = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(1.5));
		BlockPos feet = BlockPos.containing(ahead);
		Level level = p.level();
		boolean room = level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
				&& level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
		return room ? new Vec3(ahead.x, p.getY(), ahead.z) : p.position();
	}

	/** {@code p} goes into their clone where it stands; their main body sits down in meditation where they were. */
	private static void into(ServerPlayer p) {
		MinecraftServer server = p.server;
		PlayerCultivation own = CultivationManager.own(p);
		CompoundTag tag = own.getClone();
		ServerLevel there = levelOf(server, tag);
		if (there == null) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_LOST), true);
			return;
		}
		Vec3 at = posOf(tag);
		float yRot = tag.getFloat("YRot");
		CompoundTag cloneBody = tag.getCompound("Body");
		CloneBodyEntity standing = standIn(server, tag);
		if (standing != null) {
			at = standing.position();
			yRot = standing.getYRot();
			// Its health as it stands now, by its share of the whole (the whole is the clone's as it is played: #derive).
			if (!cloneBody.getBoolean("Fresh") || standing.getHealth() < standing.getMaxHealth()) {
				cloneBody.remove("Fresh");
				cloneBody.putFloat("Health", standing.getHealth());
				cloneBody.putFloat("MaxHealth", standing.getMaxHealth());
				cloneBody.putBoolean("Scaled", true);
			}
			standing.discard();
		}
		Possession.endDealings(p);
		if (MeditationManager.isMeditating(p.getUUID())) MeditationManager.stop(p, false);
		ServerLevel mainLevel = p.serverLevel();
		Vec3 mainAt = p.position();
		float mainYaw = p.getYRot();
		CompoundTag mainBody = Possession.takeFrom(p);
		CloneBodyEntity sitting = CloneBodyEntity.of(p, p, true, mainBody);
		mainBody.putFloat("Health", sitting.getHealth()); // as the player left it (takeFrom left them with what they had)
		mainLevel.addFreshEntity(sitting);
		ticket(mainLevel, mainAt, true);
		// Their record is the clone's now (at its strength): the main body's pool kept apart, gathering where it sits (#feed).
		double cloneQi = cloneQi(tag);
		tag.putBoolean("In", true);
		tag.putUUID("Entity", sitting.getUUID());
		place(tag, mainLevel, mainAt, mainYaw);
		tag.put("Body", mainBody);
		tag.remove("Record");
		tag.remove("Qi");
		tag.putDouble("MainQi", own.getQi());
		tag.putDouble("MainPlace", own.placeFactor());
		own.setClone(tag);
		IN.put(p.getUUID(), own);
		own.setQi(Math.min(cloneQi, own.maxQi()));
		move(p, there, at, yRot);
		Possession.rebase(p);
		CultivationManager.refresh(p); // the clone's strength now
		if (cloneBody.getBoolean("Fresh")) {
			cloneBody.putFloat("Health", p.getMaxHealth());
			cloneBody.putFloat("MaxHealth", p.getMaxHealth());
		}
		Possession.giveTo(p, cloneBody);
		settled(p);
		there.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.0, at.z, 20, 0.3, 0.8, 0.3, 0.03);
		p.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_INTO).withStyle(ChatFormatting.LIGHT_PURPLE), true);
	}

	/**
	 * {@code p} goes back from their clone into their main body where it sits (its stand-in gone); the clone is left standing where
	 * it was ({@code leaveClone}), or isn't (drawn back in, or dissolved: then none is out after). @return what the clone carried
	 */
	private static CompoundTag back(ServerPlayer p, boolean leaveClone) {
		MinecraftServer server = p.server;
		PlayerCultivation own = CultivationManager.own(p);
		CompoundTag tag = own.getClone();
		double cloneQi = own.getQi();
		IN.remove(p.getUUID());
		if (tag != null && tag.contains("MainQi")) own.setQi(tag.getDouble("MainQi")); // the main body's pool again
		Possession.endDealings(p);
		if (MeditationManager.isMeditating(p.getUUID())) MeditationManager.stop(p, false);
		CompoundTag cloneBody = Possession.takeFrom(p);
		cloneBody.putBoolean("Scaled", true); // its health by its share of the whole, which may be another as it is next played
		if (tag == null) { // nothing on record (an op cleared it): theirs stays as it is, their strength their own again
			Possession.rebase(p);
			CultivationManager.refresh(p);
			Possession.giveTo(p, cloneBody);
			settled(p);
			return new CompoundTag();
		}
		ServerLevel mainLevel = levelOf(server, tag);
		Vec3 mainAt = posOf(tag);
		float mainYaw = tag.getFloat("YRot");
		CompoundTag mainBody = tag.getCompound("Body");
		CloneBodyEntity sitting = standIn(server, tag);
		if (sitting != null) {
			mainBody.putFloat("Health", sitting.getHealth());
			mainAt = sitting.position();
			sitting.discard();
		}
		if (mainLevel != null) ticket(mainLevel, mainAt, false);
		CompoundTag next = null;
		if (leaveClone) {
			CloneBodyEntity standing = CloneBodyEntity.of(p, p, false, cloneBody);
			p.serverLevel().addFreshEntity(standing);
			next = new CompoundTag();
			next.putBoolean("In", false);
			next.putUUID("Entity", standing.getUUID());
			place(next, p.serverLevel(), p.position(), p.getYRot());
			next.put("Body", cloneBody);
			next.putDouble("Qi", cloneQi);
		}
		own.setClone(next);
		if (mainLevel == null) mainLevel = server.overworld(); // its world gone (a mod removed): back at least
		move(p, mainLevel, mainAt, mainYaw);
		Possession.rebase(p);
		CultivationManager.refresh(p); // their own strength again
		Possession.giveTo(p, mainBody);
		settled(p);
		if (leaveClone) p.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_BACK).withStyle(ChatFormatting.LIGHT_PURPLE), true);
		return cloneBody;
	}

	/** {@code p}'s clone drawn back into them (the other body near): its qi and what it carried are theirs; they are in their main body after. */
	private static void absorb(ServerPlayer p) {
		PlayerCultivation own = CultivationManager.own(p);
		CompoundTag tag = own.getClone();
		CompoundTag things;
		double qi;
		if (inClone(p)) {
			qi = own.getQi(); // the clone's pool, as it is played
			things = back(p, false);
		} else {
			CloneBodyEntity standing = standIn(p.server, tag);
			if (standing != null) standing.discard();
			things = tag.getCompound("Body");
			qi = cloneQi(tag);
			own.setClone(null);
		}
		Possession.addAll(p, things);
		int experience = things.getInt("XpTotal");
		if (experience > 0) p.giveExperiencePoints(experience);
		own.setQi(Math.min(own.maxQi(), own.getQi() + qi));
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		ServerLevel level = p.serverLevel();
		level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
		level.playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 0.6f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_ABSORBED).withStyle(ChatFormatting.LIGHT_PURPLE), true);
	}

	// --- Qi ---

	/**
	 * Each tick of a player in their clone (QiManager, for its pool): the clone spends {@code spendPerSecond} (its flight, its pressing
	 * down) from its own qi, gathering none of itself, and what it has spent is made up from the main body's pool (its tag's
	 * "MainQi"), which gathers as the main body would where it sits (the place it sat down in: "MainPlace"). @return whether either
	 * pool changed
	 */
	public static boolean feed(ServerPlayer p, PlayerCultivation c, double spendPerSecond) {
		CompoundTag tag = c.getClone();
		if (tag == null) return c.gatherQi(1 / 20.0, spendPerSecond);
		double before = c.getQi(), mainBefore = tag.getDouble("MainQi");
		c.setQi(Math.max(0, before - spendPerSecond / 20.0));
		double whole = c.maxQi() / STAT_SHARE; // the main body's pool, its strength whole
		double place = tag.contains("MainPlace") ? tag.getDouble("MainPlace") : 1.0;
		double main = Math.max(0, Math.min(whole, mainBefore + c.qiGatherAt(place) / 20.0));
		double want = c.maxQi() - c.getQi();
		if (want > 0 && main > 0) {
			double take = Math.min(want, main);
			main -= take;
			c.setQi(c.getQi() + take);
		}
		tag.putDouble("MainQi", main);
		return main != mainBefore || c.getQi() != before;
	}

	// --- Harm and death ---

	/** The body not played was hurt: its player hears of it (now and then: the main body's; a clone's too). */
	static void onStandInHurt(CloneBodyEntity body, UUID owner) {
		MinecraftServer server = body.level().getServer();
		ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(owner);
		if (p == null || body.tickCount % 20 != 0 && body.hurtTime < 9) return;
		p.displayClientMessage(Component.translatable(body.isSitting() ? ModLang.MSG_CLONE_MAIN_HURT : ModLang.MSG_CLONE_HURT,
				Math.round(body.getHealth()), Math.round(body.getMaxHealth())).withStyle(ChatFormatting.RED), true);
	}

	/**
	 * The body not played was slain: the standing clone dissolves, what it carried falling where it fell; the main body slain, its
	 * player dies there (as their own death, by the same blow) and the clone they played dissolves with them.
	 */
	static void onStandInDied(CloneBodyEntity body, UUID owner, DamageSource source) {
		MinecraftServer server = body.level().getServer();
		if (server == null || !(body.level() instanceof ServerLevel level)) return;
		PlayerCultivation own = CultivationManager.own(server, owner);
		CompoundTag tag = own.getClone();
		if (tag == null || !tag.hasUUID("Entity") || !tag.getUUID("Entity").equals(body.getUUID())) return;
		ServerPlayer p = server.getPlayerList().getPlayer(owner);
		if (!body.isSitting()) {
			own.setClone(null);
			lost(server, own);
			CultivationManager.markDirty(server);
			drop(level, body.position(), tag.getCompound("Body"));
			level.sendParticles(ParticleTypes.POOF, body.getX(), body.getY() + 1.0, body.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
			if (p != null) p.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_SLAIN, REFORM_TICKS / 1200).withStyle(ChatFormatting.RED), true);
			return;
		}
		// The main body: its player dies, the clone with them.
		if (p == null || !inClone(p)) { // (no one plays it now: a stale record) what it carried falls where it sat
			own.setClone(null);
			CultivationManager.markDirty(server);
			drop(level, body.position(), tag.getCompound("Body"));
			return;
		}
		Vec3 cloneAt = p.position();
		ServerLevel cloneLevel = p.serverLevel();
		CompoundTag things = back(p, false);
		drop(cloneLevel, cloneAt, things);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_CLONE_MAIN_SLAIN).withStyle(ChatFormatting.DARK_RED));
		if (source.getEntity() instanceof net.minecraft.world.entity.player.Player killer && killer != p) p.setLastHurtByPlayer(killer);
		p.getCombatTracker().recordDamage(source, Math.max(1, p.getHealth()));
		p.setHealth(0);
		p.die(source);
	}

	/**
	 * ALLOW_DEATH: a player killed in their clone doesn't die: the clone does, dissolving, what it carried falling where it fell, and
	 * they wake in their main body where it sits. @return false to hold off the death
	 */
	public static boolean allowDeath(ServerPlayer p, DamageSource source) {
		if (!inClone(p)) return true;
		giveWay(p);
		return false;
	}

	/**
	 * The clone {@code p} plays gives way where it stands (slain: {@link #allowDeath}; overtaken by the Netherworld's yin:
	 * GhostSystem#overtakenByYin): it dissolves, what it carried falling there, and they wake in their main body.
	 */
	public static void giveWay(ServerPlayer p) {
		if (!inClone(p)) return;
		Vec3 at = p.position();
		ServerLevel level = p.serverLevel();
		CompoundTag things = back(p, false);
		drop(level, at, things);
		lost(p.server, CultivationManager.own(p));
		level.sendParticles(ParticleTypes.POOF, at.x, at.y + 1.0, at.z, 30, 0.3, 0.6, 0.3, 0.05);
		p.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_FELL, REFORM_TICKS / 1200).withStyle(ChatFormatting.RED), true);
	}

	/** {@code own}'s clone is lost: no other may be split off for {@link #REFORM_TICKS}. */
	private static void lost(MinecraftServer server, PlayerCultivation own) {
		own.setCloneReadyAt(server.overworld().getGameTime() + REFORM_TICKS);
		CultivationManager.markDirty(server);
	}

	/**
	 * A player died: a clone of theirs dissolves where it stands. One who died in their clone all the same (what nothing holds off: the
	 * lifespan tribulation's end) died there whole: their main body is gone too, what it carried falling where it sat.
	 */
	public static void onDeath(ServerPlayer p) {
		PlayerCultivation own = CultivationManager.own(p);
		if (IN.remove(p.getUUID()) != null) {
			CompoundTag tag = own.getClone();
			if (tag != null && tag.contains("MainQi")) own.setQi(tag.getDouble("MainQi")); // the main body's pool, as it died
			own.setClone(null);
			CultivationManager.markDirty(p.server);
			if (tag == null) return;
			ServerLevel level = levelOf(p.server, tag);
			if (level == null) return;
			CloneBodyEntity sitting = standIn(p.server, tag);
			Vec3 at = sitting != null ? sitting.position() : posOf(tag);
			if (sitting != null) sitting.discard();
			ticket(level, at, false);
			drop(level, at, tag.getCompound("Body"));
			return;
		}
		dissolve(p.server, own);
	}

	/**
	 * {@code p}'s life ends whole (Lifespan#wipe): out of the clone first, if they are in it (what it carried falling where it stood),
	 * and the clone dissolves.
	 */
	public static void lifeEnds(ServerPlayer p) {
		if (inClone(p)) {
			Vec3 at = p.position();
			ServerLevel level = p.serverLevel();
			drop(level, at, back(p, false));
			return;
		}
		dissolve(p.server, CultivationManager.own(p));
	}

	/**
	 * {@code own}'s clone (not being played) dissolves where it stands: what it carried falls there (its land loaded for it), and none is
	 * out after. Before a life ends whole (Lifespan#wipe), and at its maker's death.
	 */
	public static void dissolve(MinecraftServer server, PlayerCultivation own) {
		CompoundTag tag = own.getClone();
		if (tag == null || tag.getBoolean("In")) return;
		own.setClone(null);
		CultivationManager.markDirty(server);
		ServerLevel level = levelOf(server, tag);
		if (level == null) return;
		CloneBodyEntity standing = standIn(server, tag);
		Vec3 at = standing != null ? standing.position() : posOf(tag);
		if (standing != null) standing.discard();
		level.getChunk(BlockPos.containing(at)); // loaded, so what falls there stays
		drop(level, at, tag.getCompound("Body"));
	}

	/** Everything {@code body} (Possession#takeFrom's tag) carries falls at {@code at}: its things, its rings, its experience. */
	private static void drop(ServerLevel level, Vec3 at, CompoundTag body) {
		java.util.List<ItemStack> stacks = new ArrayList<>();
		for (Tag entry : body.getList("Inventory", Tag.TAG_COMPOUND)) stacks.add(ItemStack.of((CompoundTag) entry));
		for (Tag entry : body.getList("Rings", Tag.TAG_COMPOUND)) stacks.add(ItemStack.of((CompoundTag) entry));
		for (ItemStack stack : stacks) {
			if (stack.isEmpty()) continue;
			ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, stack);
			item.setDefaultPickUpDelay();
			level.addFreshEntity(item);
		}
		int experience = body.getInt("XpTotal");
		if (experience > 0) net.minecraft.world.entity.ExperienceOrb.award(level, at, Math.min(100, experience));
	}

	// --- Logging out, crashes, stopping ---

	/** A player logs out: in their clone, they go back to their main body first (the clone stands on where it was). */
	public static void onDisconnect(ServerPlayer p) {
		if (inClone(p)) back(p, true);
		REJOINED.remove(p.getUUID());
	}

	/**
	 * Joining with their clone on record as played (a crash left them in it): they are set right a tick on, once in the world (moving
	 * them before breaks their login): what they have on them is the clone's, left standing where they are, and they wake in their
	 * main body where it sat.
	 */
	public static void onJoin(ServerPlayer p) {
		CompoundTag tag = CultivationManager.own(p).getClone();
		if (tag != null && tag.getBoolean("In") && !inClone(p)) REJOINED.add(p.getUUID());
	}

	private static void rejoined(ServerPlayer p) {
		PlayerCultivation own = CultivationManager.own(p);
		CompoundTag tag = own.getClone();
		if (tag == null || !tag.getBoolean("In") || inClone(p)) return;
		// Their record holds the clone's pool, the main body's kept apart; a clone played before the sweep of 2026-10-23 kept the
		// main body's in the record and its own in a copy ("Record").
		if (!tag.contains("MainQi")) {
			tag.putDouble("MainQi", own.getQi());
			own.setQi(cloneQi(tag));
		}
		IN.put(p.getUUID(), own);
		back(p, true);
	}

	/** The server is stopping: everyone in their clone goes back to their main body (the clones stand on). */
	public static void returnAll(MinecraftServer server) {
		for (UUID id : new ArrayList<>(IN.keySet())) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p != null) back(p, true);
		}
		IN.clear();
		REJOINED.clear();
	}

	/** Each server tick: players back from a crash set right; those in their clones told how their main body fares. */
	public static void tick(MinecraftServer server) {
		if (!REJOINED.isEmpty()) {
			for (UUID id : new ArrayList<>(REJOINED)) {
				REJOINED.remove(id);
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p != null && !p.hasDisconnected()) rejoined(p);
			}
		}
		if (IN.isEmpty() || server.getTickCount() % 10 != 0) return;
		for (UUID id : IN.keySet()) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null) continue;
			CompoundTag tag = CultivationManager.own(p).getClone();
			CloneBodyEntity sitting = tag == null ? null : standIn(server, tag);
			ModPackets.sendCloneState(p, true, sitting == null ? 0 : sitting.getHealth(), sitting == null ? 0 : sitting.getMaxHealth());
		}
	}

	// --- Helpers ---

	/** After a body changes hands: the player's screens told what they are now. */
	private static void settled(ServerPlayer p) {
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		Spells.sync(p);
		NetherworldManager.refresh(p);
		ModPackets.sendCloneState(p, inClone(p), 0, 0);
	}

	/** {@code p} brought to {@code at} in {@code level}, still (a teleport across the worlds, if need be). */
	private static void move(ServerPlayer p, ServerLevel level, Vec3 at, float yRot) {
		p.stopRiding();
		if (p.level() != level) p.teleportTo(level, at.x, at.y, at.z, yRot, p.getXRot());
		else p.connection.teleport(at.x, at.y, at.z, yRot, p.getXRot());
		p.connection.resetPosition();
		p.setDeltaMovement(Vec3.ZERO);
		p.fallDistance = 0;
	}

	/** The main body's land kept loaded while it sits there ({@code on}), or let go. */
	private static void ticket(ServerLevel level, Vec3 at, boolean on) {
		ChunkPos chunk = new ChunkPos(BlockPos.containing(at));
		if (on) level.getChunkSource().addRegionTicket(MAIN_TICKET, chunk, MAIN_TICKET_RADIUS, chunk);
		else level.getChunkSource().removeRegionTicket(MAIN_TICKET, chunk, MAIN_TICKET_RADIUS, chunk);
	}

	/** The body {@code tag} has not played (its stand-in), if it is where it should be and loaded; else null. */
	private static CloneBodyEntity standIn(MinecraftServer server, CompoundTag tag) {
		ServerLevel level = levelOf(server, tag);
		if (level == null || !tag.hasUUID("Entity")) return null;
		return level.getEntity(tag.getUUID("Entity")) instanceof CloneBodyEntity body && body.isAlive() ? body : null;
	}

	private static void place(CompoundTag tag, ServerLevel level, Vec3 at, float yRot) {
		tag.putString("Dimension", level.dimension().location().toString());
		tag.putDouble("X", at.x);
		tag.putDouble("Y", at.y);
		tag.putDouble("Z", at.z);
		tag.putFloat("YRot", yRot);
	}

	private static ResourceKey<Level> dimensionOf(CompoundTag tag) {
		ResourceLocation id = ResourceLocation.tryParse(tag.getString("Dimension"));
		return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
	}

	private static ServerLevel levelOf(MinecraftServer server, CompoundTag tag) {
		ResourceKey<Level> key = dimensionOf(tag);
		return key == null ? null : server.getLevel(key);
	}

	private static Vec3 posOf(CompoundTag tag) {
		return new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
	}

	/** Server stopped: nothing held. */
	public static void clear() {
		IN.clear();
		REJOINED.clear();
	}

	private Clones() {}
}
