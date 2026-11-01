package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Borrowed Life (CultivationMethod#BORROWED_LIFE; the user's pick of 2026-11-01): the ghost path's way of a living cultivator's body
 * possessed and its life drawn off. Practising it, a soul in a living cultivator's body (Possession: an NPC cultivator's, or a player's)
 * draws off every {@link #EVERY} ticks {@link #SHARE} of its ghost path's stage, twice as much for each realm the host stands above that
 * path, half for each below (at most {@link #MOST}: {@link #share}); the host loses as much of its own stage, never a stage ({@link #drain}).
 * What is drawn settles in the soul as it leaves the body, however it leaves ({@link #left}, from Possession#leave: CultivationMethods#grant).
 * The host's soul fights back: it is held {@link #HOLD} ticks, half as long for each realm it stands above the ghost path, twice as long
 * for each below ({@link #holdFor}: {@link #LEAST_HOLD} to {@link #MOST_HOLD}); then it throws the soul out ({@link #thrownOut}): stunned,
 * and an NPC host remembers it (its regard at its lowest: hostile).
 */
public final class BorrowedLife {
	public static final double SHARE = 0.012, MOST = 0.05;
	public static final int EVERY = 100, HOLD = 1200, LEAST_HOLD = 400, MOST_HOLD = 3600;

	/** A soul's draw on the body it is in: whose, how long it has held it (ticks), what it has drawn off (a share of its own stage). */
	private static final class Draw {
		final UUID host;
		int held;
		double drawn;

		Draw(UUID host) { this.host = host; }
	}

	/** Each soul's draw; not saved (a logout gives the body back first, and what was drawn settles then: {@link #left}). */
	private static final Map<UUID, Draw> DRAWS = new HashMap<>();

	/** Whether {@code own} (a soul's own record) draws on the bodies it takes: its ghost path practising Borrowed Life. */
	public static boolean practises(PlayerCultivation own) {
		return CultivationMethods.practised(own, TwoPaths.Path.GHOST) == CultivationMethod.BORROWED_LIFE;
	}

	/** What a draw on a host standing in {@code host} wins of the stage of a ghost path standing in {@code ghost}. */
	public static double share(Realm host, Realm ghost) {
		return Math.min(MOST, SHARE * Math.pow(2, host.ordinal() - ghost.ordinal()));
	}

	/** How long a host standing in {@code host} is held by a ghost path standing in {@code ghost} before it throws the soul out (ticks). */
	public static int holdFor(Realm host, Realm ghost) {
		return (int) Math.max(LEAST_HOLD, Math.min(MOST_HOLD, HOLD * Math.pow(2, ghost.ordinal() - host.ordinal())));
	}

	/** What {@code soul} has drawn off the body it is in so far (0 with none). */
	public static double drawn(UUID soul) {
		Draw d = DRAWS.get(soul);
		return d == null ? 0 : d.drawn;
	}

	/** The server's tick: every soul in a living body, drawing on it. */
	static void tick(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (Possession.inhabits(p.getUUID())) tick(p);
		}
	}

	/** A tick of {@code p}'s soul in a body: its draw, when its time comes; thrown out when its hold is up. */
	private static void tick(ServerPlayer p) {
		PlayerCultivation own = CultivationManager.own(p);
		if (!practises(own) || !CultivationMethods.holdsVolume(own, TwoPaths.Path.GHOST)) return;
		Realm ghost = CultivationMethods.realmOf(own, TwoPaths.Path.GHOST);
		LivingEntity body = Possession.bodyEntity(p);
		PlayerCultivation host = Possession.borrowed(p);
		if (ghost == null || body == null || host == null) return;
		int hostRank = TwoPaths.higherRank(host, false);
		if (hostRank < 0) return; // a mortal's body: no cultivation in it to draw on
		Realm hostRealm = Realm.byIndex(Math.min(Realm.values().length - 1, hostRank / Stage.values().length));
		Draw d = DRAWS.get(p.getUUID());
		if (d == null || !d.host.equals(body.getUUID())) {
			d = new Draw(body.getUUID());
			DRAWS.put(p.getUUID(), d);
		}
		int hold = holdFor(hostRealm, ghost);
		if (++d.held >= hold) {
			thrownOut(p);
			return;
		}
		if (d.held % EVERY != 0) return;
		double share = share(hostRealm, ghost);
		d.drawn += share;
		drain(host, share);
		CultivationManager.markDirty(p.server);
		// The life drawn off, rising about the body (at chest height and below: never before its eyes).
		ServerLevel level = p.serverLevel();
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10;
			level.sendParticles(ParticleTypes.SOUL, p.getX() + Math.cos(a) * 0.8, p.getY() + 0.2 + p.getRandom().nextDouble() * 0.8,
					p.getZ() + Math.sin(a) * 0.8, 1, 0, 0.04, 0, 0.01);
		}
		level.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.6f, 0.8f);
		double percent = d.drawn * 100;
		Object shown = percent < 0.95 ? String.format(Locale.ROOT, "%.1f", percent) : (Object) Math.round(percent);
		Component name = Possession.displayName(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_BORROWED_DRAW, name == null ? body.getDisplayName() : name, shown,
				Math.round(100.0 * d.held / hold)).withStyle(ChatFormatting.DARK_AQUA), true);
	}

	/**
	 * {@code host} (a body's record) loses {@code share} of its own stage, on its mightier path (the heaven path's, or a demon's blood's),
	 * never below the stage's start: the life drawn off it.
	 */
	static void drain(PlayerCultivation host, double share) {
		int demon = DemonPath.rank(host), heaven = host.isMortal() ? -1 : PlayerCultivation.rank(host.getRealm(), host.getStage());
		if (demon > heaven) {
			host.demon().set(demon, Math.max(0, host.demon().cultivation() - share * DemonPath.required(demon)));
		} else if (heaven >= 0) {
			host.setState(host.getRealm(), host.getStage(), Math.max(0, host.getCultivation() - share * host.cultivationRequired()));
		}
	}

	/**
	 * The host's soul throws {@code p}'s out (its hold up): the body given back (SoulProjection#letGo; what was drawn settles in the soul:
	 * {@link #left}), the soul reeling, and an NPC host remembering it (its regard at its lowest).
	 */
	private static void thrownOut(ServerPlayer p) {
		CultivatorNpc npc = Possession.npcBody(p);
		Component name = Possession.displayName(p);
		SoulProjection.letGo(p, null);
		p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
		p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
		p.sendSystemMessage(Component.translatable(ModLang.MSG_BORROWED_THROWN_OUT, name == null ? Component.literal("?") : name).withStyle(ChatFormatting.RED));
		if (npc != null) npc.changeRegard(p, Regard.MIN);
	}

	/** {@code soul} left the body it was in (Possession#leave, however it went): what it drew off settles in it now (told). */
	static void left(ServerPlayer soul) {
		Draw d = DRAWS.remove(soul.getUUID());
		if (d != null && d.drawn > 0) CultivationMethods.grant(soul, TwoPaths.Path.GHOST, CultivationMethod.BORROWED_LIFE, d.drawn);
	}

	/** A player left (its body given back first: {@link #left}). */
	public static void forget(UUID id) {
		DRAWS.remove(id);
	}

	/** The server stopped. */
	public static void clear() {
		DRAWS.clear();
	}

	private BorrowedLife() {}
}
