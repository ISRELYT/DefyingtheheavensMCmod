package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

import java.util.ArrayList;

/**
 * Immortal essence (phase 2 of the realm plan, 2026-10-25; the user's pick for a True Immortal below the Gate: "Keep power, lose
 * essence"): a True Immortal's pool beside its qi, {@link #POOL} by its stage on the higher of its paths. It fills only in the Immortal
 * Realm, from empty in {@link #FILL_SECONDS} ({@link #MEDITATING} times as fast meditating); anywhere else it leaks away, a full pool in
 * {@link #LEAK_SECONDS} (an in-game day), so an immortal keeps its realm's might below the Gate but not its essence. Its switch
 * (Ability#IMMORTAL_ESSENCE, off to begin with): while it is on one's blows and spells strike {@link #HARM} times as hard
 * ({@link #outgoing}) and it burns a {@link #BURN_SECONDS}th of the pool a second, wreathed in golden light; it won't light on less
 * than {@link #LIGHT_SECONDS} of it, and goes out when the pool runs dry, at a death and at a logout. Creative players burn none of it.
 */
public final class ImmortalEssence {
	/** The pool, True Immortal Early to Grand Perfection. */
	public static final double[] POOL = {100, 150, 220, 320};
	/** Seconds the Immortal Realm takes to fill an empty pool. */
	public static final int FILL_SECONDS = 600;
	/** So many times as fast meditating there. */
	public static final int MEDITATING = 4;
	/** Seconds a full pool takes to leak away anywhere else (an in-game day). */
	public static final int LEAK_SECONDS = 1200;
	public static final float HARM = 1.5f;
	/** A full pool lasts so many seconds switched on. */
	public static final int BURN_SECONDS = 90;
	/** It won't light on less than so many seconds of it. */
	public static final int LIGHT_SECONDS = 5;
	private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.84f, 0.32f), 1.1f);

	/**
	 * {@code c}'s pool when full: {@link #POOL} by its stage once a True Immortal on any path (TwoPaths#higherRank), twice as much again
	 * for each realm past True Immortal (the stages' as there); else none.
	 */
	public static double max(PlayerCultivation c) {
		if (c.isMortal()) return 0;
		int stage = TwoPaths.higherRank(c, false) - PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY);
		if (stage < 0) return 0;
		int stages = Stage.values().length;
		return POOL[stage % stages] * Math.pow(2, stage / stages);
	}

	/** Whether {@code p}'s essence is lit this moment. */
	public static boolean isLit(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return c.isAbilityEnabled(Ability.IMMORTAL_ESSENCE) && Ability.IMMORTAL_ESSENCE.isUnlocked(c);
	}

	/** What a second does to {@code c}'s pool: filled in the Immortal Realm (the faster {@code meditating}), else leaked. */
	public static double perSecond(PlayerCultivation c, boolean immortalRealm, boolean meditating) {
		double max = max(c);
		return immortalRealm ? max / FILL_SECONDS * (meditating ? MEDITATING : 1) : -max / LEAK_SECONDS;
	}

	/** Every tick: each lit player's golden light, and once a second each True Immortal's pool (or out it goes). */
	public static void tick(MinecraftServer server) {
		for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
			boolean lit = isLit(p);
			if (lit && (!p.isAlive() || p.isSpectator())) { // a death puts it out (the respawned don't burn on)
				putOut(p, null);
				lit = false;
			}
			if (lit && p.tickCount % 3 == 0) {
				p.serverLevel().sendParticles(GOLD, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.35, 0.6, 0.35, 0.0);
				p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 0.8, p.getZ(), 1, 0.3, 0.5, 0.3, 0.01);
			}
			if (p.tickCount % 20 == 0) second(p);
		}
	}

	/**
	 * A second of it on {@code p} (package-private for the game tests): the pool filled or leaked where they are, and burnt while lit and
	 * while a Law's domain is unfurled (LawDomains#BURN_SECONDS; not a creative player's); out they go when it runs dry.
	 */
	static void second(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		double max = max(c);
		if (max <= 0) { // none short of True Immortal: a pool left from a fall emptied, its switch off
			c.setEssence(0);
			if (c.isAbilityEnabled(Ability.IMMORTAL_ESSENCE)) putOut(p, null);
			return;
		}
		boolean lit = isLit(p), domain = LawDomains.lit(c) != null;
		double before = c.essence();
		double flow = perSecond(c, ModDimensions.hasImmortalQi(p.level().dimension()), MeditationManager.isMeditating(p.getUUID())); // a small world's too
		if (FiveDecays.drains(c)) flow = -max / FiveDecays.ESSENCE_DRAIN_SECONDS; // the qi faltering under the Five Decays: it drains wherever it is
		double burnt = p.isCreative() ? 0 : (lit ? max / BURN_SECONDS : 0) + (domain ? max / LawDomains.BURN_SECONDS : 0);
		double next = Math.max(0, Math.min(max, before + flow - burnt));
		if (next != before) {
			c.setEssence(next);
			CultivationManager.markDirty(p.server);
			CultivationManager.sync(p);
		}
		if (lit && next <= 0) putOut(p, ModLang.MSG_ESSENCE_SPENT);
		if (domain && next <= 0) LawDomains.putOut(p, ModLang.MSG_DOMAIN_SPENT);
	}

	/** Its switch thrown (CultivationManager#toggleAbility, after the switch): lit (refused on too little), or put out; told, heard. */
	static void toggled(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		boolean lit = c.isAbilityEnabled(Ability.IMMORTAL_ESSENCE);
		if (lit && !p.isCreative() && c.essence() < max(c) / BURN_SECONDS * LIGHT_SECONDS) {
			c.setAbilityEnabled(Ability.IMMORTAL_ESSENCE, false);
			CultivationManager.sync(p);
			p.displayClientMessage(Component.translatable(ModLang.MSG_ESSENCE_TOO_LITTLE).withStyle(ChatFormatting.GOLD), true);
			return;
		}
		if (lit) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_ESSENCE_ON, Math.round((HARM - 1) * 100),
					Math.round(c.essence() / (max(c) / BURN_SECONDS))).withStyle(ChatFormatting.GOLD), true);
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
		} else {
			p.displayClientMessage(Component.translatable(ModLang.MSG_ESSENCE_OFF), true);
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 1.4f);
		}
	}

	/** Put out (told why, if {@code why}). */
	static void putOut(ServerPlayer p, String why) {
		PlayerCultivation c = CultivationManager.get(p);
		if (c.isAbilityEnabled(Ability.IMMORTAL_ESSENCE)) {
			c.setAbilityEnabled(Ability.IMMORTAL_ESSENCE, false);
			CultivationManager.markDirty(p.server);
			CultivationManager.sync(p);
		}
		if (why != null) {
			p.sendSystemMessage(Component.translatable(why).withStyle(ChatFormatting.GOLD));
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 1.4f);
		}
	}

	/** A blow or a spell of {@code attacker}'s (Physiques#outgoing): {@link #HARM} times as hard while its essence is lit. */
	public static float outgoing(ServerPlayer attacker, float amount) {
		return isLit(attacker) ? amount * HARM : amount;
	}

	/** A logout: it goes out (it isn't lit again by logging in). */
	public static void onDisconnect(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (c.isAbilityEnabled(Ability.IMMORTAL_ESSENCE)) {
			c.setAbilityEnabled(Ability.IMMORTAL_ESSENCE, false);
			CultivationManager.markDirty(p.server);
		}
	}

	private ImmortalEssence() {}
}
