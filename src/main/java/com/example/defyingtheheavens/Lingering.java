package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * The soul after death (the user's ask, 2026-11-16: "if u are past nascent soul and you die not in the netherworld you should be able to fly around
 * as a nascent soul and be able to possess someone to not get sent to the netherworld as fast, obviously people at nascent soul and above can see you
 * and everyone can hit you. you should feel the 'pull of the netherworld' (a timer). possessing someone or something just extends it (make sure it cant
 * be infinitely extended). the higher your ghost cultivation path the longer you can resist the pull. if someone purely on the ghost path dies then
 * you decide what happens").
 * <p>
 * While the gamerule netherworldAfterDeath is on, one whose qi path (heaven, ghost or the Demon Path) stands at Nascent Soul or beyond, dying
 * anywhere but the Netherworld, its passages and the Inner Realm, and of nothing that destroys the soul with the body ({@link #destroysTheSoul}),
 * rises at the death's respawn where its body lies (the corpse keeps its things: Corpses) as its nascent soul, and lingers among the living until
 * the Netherworld's pull takes it down to the Ghost Gate, one of the dead, as every death goes ({@link #pulledDown}). Meanwhile it is one of the
 * dead (GhostSystem: it flies freely, touches nothing, is seen only by those who perceive souls, SoulProjection#perceives: a Nascent Soul with
 * Qi Sense open, a Heavenly Being or above, a Ghost Eye), but:
 * <ul>
 *   <li>it is a soul's violet, not the dead's cyan, and wears no Ghost King's crown and has no king's hands while it lingers ({@link #asSoul},
 *   GhostSystem#isKing);</li>
 *   <li>anyone strikes it: every blow that finds it lands whole (GhostSystem#allowDamage, #scaleDamage), and the cultivators who perceive souls
 *   may hunt it (CultivatorNpc#canAttack); struck down, it is torn away to the Gate (GhostSystem#disperse, {@link #struckDown});</li>
 *   <li>a right-click on a person or a cultivator at least a realm below it, or on a body of the dead (its own too), takes that body as Ghostly
 *   Possession does (GhostPossession), free of ghostly qi: the pull is the price;</li>
 *   <li>the pull ({@link #seconds}: {@link #BASE_SECONDS}, and {@link #PER_GHOST_STAGE} more for each stage of its ghost path) fills at a third of
 *   its pace inside a body ({@link #IN_A_BODY}): a body holds it longer, never for good. It shows on the HUD. Logging out, the pull takes it.</li>
 * </ul>
 * My call for one who walks the ghost path alone ({@link #pureGhost}: its ghost path begun, no heaven scripture or manual, no Demon Path): it is half the
 * Netherworld's already, so at ANY realm it rises as a ghost, with the dead's own protection (only what touches the dead harms it) and a Ghost
 * King's crown and hands if it is one, and the pull takes twice as long ({@link #PURE_GHOST}).
 */
public final class Lingering {
	/** The pull's length out of a body (seconds): this, and this much more for each stage of the ghost path (none: the base). */
	public static final double BASE_SECONDS = 90, PER_GHOST_STAGE = 6;
	/** Inside a body the pull fills at this share of its pace. */
	public static final double IN_A_BODY = 1.0 / 3;
	/** One of the ghost path alone resists the pull this many times as long. */
	public static final double PURE_GHOST = 2;
	/** The pull is told once as it passes this share: the last quarter. */
	public static final double WARN_AT = 0.75;
	/** The rank a nascent soul is formed at: Nascent Soul Early. */
	public static final int NASCENT = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
	/** Set by the client: whether it has been told a player lingers as a nascent soul (ModPackets#GHOST). */
	public static Predicate<Player> clientSoul = player -> false;

	// --- Who ---

	/**
	 * Whether {@code c} walks the ghost path alone: its ghost path begun, no Demon Path (no demon core), and the heaven path never taken up: no
	 * heaven scripture known (HeavenMethods: without one it gathers nothing) and no heaven method from a manual (CultivationMethod).
	 */
	public static boolean pureGhost(PlayerCultivation c) {
		if (TwoPaths.ghostRank(c, false) < 0 || TwoPaths.demonRank(c, false) >= 0 || c.heaven().knowsAny()) return false;
		for (CultivationMethod method : c.methodsLearned()) if (method.path() == TwoPaths.Path.HEAVEN) return false;
		return true;
	}

	/** Whether {@code c} lingers among the living now, as a soul or as a ghost: dead, and not yet taken down. */
	public static boolean lingers(PlayerCultivation c) {
		return c.netherworld().lingering() && c.netherworld().ghost();
	}

	/** Whether {@code c} lingers as its nascent soul ({@link #lingers}, not one of the ghost path alone, who lingers as a ghost). */
	public static boolean asSoul(PlayerCultivation c) {
		return lingers(c) && !pureGhost(c);
	}

	/** Whether {@code p} lingers (its own record: its soul's, whatever body it is in). Server. */
	public static boolean lingers(ServerPlayer p) {
		return lingers(CultivationManager.own(p));
	}

	/**
	 * Whether {@code player} lingers as a nascent soul and stands as one now (in no body: in one it is that body). Server: its own record; a
	 * client: what it was told (ModPackets#GHOST).
	 */
	public static boolean isSoul(Player player) {
		if (player == null) return false;
		if (player.level().isClientSide) return clientSoul.test(player);
		return player instanceof ServerPlayer sp && asSoul(CultivationManager.own(sp)) && !Possession.inhabits(sp.getUUID());
	}

	/** How long the pull takes {@code c} out of a body (seconds): {@link #BASE_SECONDS} and {@link #PER_GHOST_STAGE} a stage of its ghost path. */
	public static double seconds(PlayerCultivation c) {
		int ghost = TwoPaths.ghostRank(c, false);
		double seconds = BASE_SECONDS + PER_GHOST_STAGE * Math.max(0, ghost + 1);
		return pureGhost(c) ? seconds * PURE_GHOST : seconds;
	}

	/** Seconds left before the pull takes {@code p}, at its pace now (in a body or not). */
	public static int secondsLeft(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.own(p);
		double pace = Possession.inhabits(p.getUUID()) ? IN_A_BODY : 1;
		return (int) Math.ceil((1 - c.netherworld().pull()) * seconds(c) / pace);
	}

	// --- Dying ---

	/**
	 * Whether a death by {@code source} destroys the soul with the body, leaving nothing to linger: the heavens' own strike (a tribulation's, the
	 * Five Decays' judgement: {@code byHeaven}), the Chief Judge's gaze, a life run out (old age), a soul scattered, severed or overcome by
	 * another's seizing its body, the Chaos storm that unmakes souls.
	 */
	public static boolean destroysTheSoul(DamageSource source, boolean byHeaven) {
		return byHeaven || source.is(ModDamageTypes.JUDGE_GAZE) || source.is(ModDamageTypes.OLD_AGE) || source.is(ModDamageTypes.SOUL_SCATTER)
				|| source.is(ModDamageTypes.SOUL_SEVERED) || source.is(ModDamageTypes.SOUL_SEIZED) || source.is(ModDamageTypes.CHAOS_STORM)
				|| source.is(ModDamageTypes.SOUL_WENT_ON); // a soul gone on out of a puppet (PuppetPlayers): it lingered there already
	}

	/**
	 * Whether {@code p}, dying now of {@code source}, lingers (AFTER_DEATH, GhostSystem#onDeath; the gamerule already asked): of the living, not
	 * in a clone, dying in no Netherworld, passage or Inner Realm and of nothing that destroys the soul; a nascent soul formed on a qi path, or one
	 * of the ghost path alone at any realm.
	 */
	static boolean mayLinger(ServerPlayer p, DamageSource source, boolean byHeaven) {
		PlayerCultivation c = CultivationManager.own(p);
		if (c.netherworld().ghost() || Clones.inClone(p)) return false;
		ResourceKey<Level> where = p.level().dimension();
		if (ModDimensions.isNetherworld(where) || ModDimensions.isPassage(where) || ModDimensions.isInnerRealm(where)) return false;
		if (destroysTheSoul(source, byHeaven)) return false;
		return pureGhost(c) || TwoPaths.higherRank(c, false) >= NASCENT;
	}

	/**
	 * Bound to linger (GhostSystem#onDeath, the player's own respawn point set aside as for the Gate's): the respawn set where its body lies, on
	 * the nearest spot a body could stand in above it.
	 */
	static void bind(ServerPlayer p, NetherworldState s) {
		s.setLingering(true);
		s.setPull(0);
		ServerLevel level = p.serverLevel();
		p.setRespawnPosition(level.dimension(), standingNear(level, p.blockPosition()), p.getYRot(), true, false);
	}

	/** The nearest spot at or above {@code pos} (within the world) where a body could stand: two blocks one could respawn in; else {@code pos}. */
	static BlockPos standingNear(ServerLevel level, BlockPos pos) {
		int y = Mth.clamp(pos.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 3);
		BlockPos base = new BlockPos(pos.getX(), y, pos.getZ());
		for (int up = 0; up <= 24 && base.getY() + up < level.getMaxBuildHeight() - 2; up++) {
			BlockPos at = base.above(up);
			if (free(level, at) && free(level, at.above())) return at;
		}
		return base;
	}

	private static boolean free(ServerLevel level, BlockPos at) {
		BlockState state = level.getBlockState(at);
		return state.getBlock().isPossibleToRespawnInThis(state);
	}

	/**
	 * The death's respawn (GhostSystem#onRespawn): it rises where its body lies (taken there now if the respawn couldn't have the spot), one of
	 * the dead, its soul held up among the living; the pull begins.
	 */
	static void rise(ServerPlayer p, NetherworldState s) {
		ServerLevel died = diedIn(p, s);
		if (died != null && (p.level() != died || p.blockPosition().distSqr(s.deathPos()) > 16 * 16)) {
			BlockPos at = standingNear(died, s.deathPos());
			p.teleportTo(died, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), 0);
		}
		s.setPull(0);
		GhostSystem.become(p);
		PlayerCultivation c = CultivationManager.own(p);
		boolean soul = !pureGhost(c);
		ServerLevel level = p.serverLevel();
		level.sendParticles(soul ? ParticleTypes.END_ROD : ParticleTypes.SOUL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.3, 0.7, 0.3, 0.03);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, soul ? 1.2f : 0.6f);
		p.sendSystemMessage(Component.translatable(soul ? ModLang.MSG_LINGER_RISE_SOUL : ModLang.MSG_LINGER_RISE_GHOST, (int) Math.round(seconds(c)))
				.withStyle(soul ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
		NetherworldManager.refresh(p);
		CultivationManager.markDirty(p.server);
	}

	/** The world {@code p} died in (its death place: NetherworldState#deathDimension), or null. */
	private static ServerLevel diedIn(ServerPlayer p, NetherworldState s) {
		if (!s.hasDeathPlace()) return null;
		ResourceLocation id = ResourceLocation.tryParse(s.deathDimension());
		return id == null ? null : p.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	// --- The pull ---

	/**
	 * Each second of a player (GhostSystem#tick, its own record, whatever body it is in): a lingering one's pull fills, at {@link #IN_A_BODY} of its
	 * pace inside a body; told once in its last quarter; full, it is taken down ({@link #pulledDown}).
	 */
	static void second(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.own(p);
		NetherworldState s = c.netherworld();
		if (!lingers(c) || GhostSystem.dispersed(p.getUUID())) return;
		double was = s.pull();
		double pace = Possession.inhabits(p.getUUID()) ? IN_A_BODY : 1;
		s.setPull(was + pace / seconds(c));
		if (s.pull() >= 1) {
			pulledDown(p);
			return;
		}
		if (was < WARN_AT && s.pull() >= WARN_AT) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_LINGER_WANING, secondsLeft(p)).withStyle(ChatFormatting.DARK_AQUA), false);
			p.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8f, 0.5f);
		}
		NetherworldManager.sync(p);
	}

	/**
	 * The pull has it: out of any body it is in (given back where it stands), down to the Ghost Gate, one of the dead now as any death leaves one
	 * (a Ghost King crowned, GhostSystem#watchKing).
	 */
	public static void pulledDown(ServerPlayer p) {
		NetherworldState s = CultivationManager.own(p).netherworld();
		if (GhostPossession.isTenant(p)) GhostPossession.leave(p, null);
		ServerLevel from = p.serverLevel();
		from.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1, p.getZ(), 30, 0.3, 0.7, 0.3, 0.05);
		from.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.4f);
		s.setLingering(false);
		s.setPull(0);
		GhostSystem.toGate(p);
		GhostSystem.broadcast(p); // one of the dead's light now, the Netherworld's cyan
		NetherworldManager.refresh(p);
		CultivationManager.markDirty(p.server);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_LINGER_PULLED).withStyle(ChatFormatting.DARK_AQUA));
		p.playNotifySound(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundSource.PLAYERS, 1.0f, 0.6f);
	}

	/** Struck down while it lingered (GhostSystem#disperse): no longer held up among the living; it reforms at the Gate as the dead do. */
	static void struckDown(ServerPlayer p) {
		NetherworldState s = CultivationManager.own(p).netherworld();
		if (!s.lingering()) return;
		s.setLingering(false);
		s.setPull(0);
		CultivationManager.markDirty(p.server);
		GhostSystem.broadcast(p); // no soul's violet now: one of the dead (its client too: no soul's right-click)
		NetherworldManager.sync(p); // the pull's bar gone
		p.sendSystemMessage(Component.translatable(ModLang.MSG_LINGER_STRUCK).withStyle(ChatFormatting.DARK_AQUA));
	}

	/** Logging out (the body it was in given back first: GhostPossession#onDisconnect): the pull takes it; it reforms at the Gate on its return. */
	public static void onDisconnect(ServerPlayer p) {
		NetherworldState s = CultivationManager.own(p).netherworld();
		if (!s.lingering() || !s.ghost()) return;
		s.setLingering(false);
		s.setPull(0);
		s.setReforming(true); // GhostSystem#arrived: together again at the Gate
		CultivationManager.markDirty(p.server);
	}

	/** Logging in still lingering (a crash took the server down while it lingered): taken down as on a logout ({@link #onDisconnect}). */
	public static void onJoin(ServerPlayer p) {
		onDisconnect(p);
	}

	private Lingering() {}
}
