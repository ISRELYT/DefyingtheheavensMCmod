package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Burn Lifespan (Ability#BURN_LIFESPAN; the user's ask, 2026-10-21): one's own years set alight for strength. While it burns it costs
 * {@link #SPAN_PER_SECOND} of one's whole span a second (Lifespan#age: a share, not a fixed number, the user's ask the same day, so it
 * costs every realm alike); one's blows and spells strike {@link #HARM} times as hard ({@link #outgoing}) and one moves {@link #SPEED}
 * times as fast, wreathed in a crimson flame. It goes out when switched off, by itself before it would leave less than
 * Lifespan#WARN_YEARS (it never burns the last years away, nor lights with no more than those), at a death, and at a logout.
 * Creative players burn nothing (they don't age).
 */
public final class BurnLifespan {
	/** Half a percent of the whole span a second: ten seconds of it are a twentieth of a life. */
	public static final double SPAN_PER_SECOND = 0.005;
	public static final float HARM = 1.5f;
	public static final double SPEED = 1.25;
	private static final UUID SPEED_ID = UUID.fromString("9e1d3c55-0b7a-4c2f-8e61-4a7d2b9c0f15");
	private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(0.86f, 0.08f, 0.1f), 1.3f);

	/**
	 * What a second of it costs {@code c} (its own record): {@link #SPAN_PER_SECOND} of its span, in years; nothing for a True
	 * Immortal, whose years have no end (Lifespan#endless: the user's call, 2026-10-22, to leave it burning for them for now).
	 */
	public static double yearsPerSecond(PlayerCultivation c) {
		return Lifespan.endless(c) ? 0 : Lifespan.span(c) * SPAN_PER_SECOND;
	}

	/** Whether a second more of it would leave {@code c} (its own record) fewer than Lifespan#WARN_YEARS (never a True Immortal). */
	private static boolean tooFew(PlayerCultivation c) {
		return !Lifespan.endless(c) && Lifespan.left(c) - yearsPerSecond(c) < Lifespan.WARN_YEARS;
	}

	/** Whether {@code p}'s lifespan is burning this moment. */
	public static boolean isBurning(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return c.isAbilityEnabled(Ability.BURN_LIFESPAN) && Ability.BURN_LIFESPAN.isUnlocked(c);
	}

	/** Every tick: each burning player's flame, and once a second its years (or out it goes). */
	public static void tick(MinecraftServer server) {
		for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
			boolean burning = isBurning(p);
			if (burning && (!p.isAlive() || p.isSpectator())) { // a death puts it out (the respawned don't burn on)
				putOut(p, null);
				burning = false;
			}
			speed(p, burning);
			if (!burning) continue;
			if (p.tickCount % 3 == 0) {
				p.serverLevel().sendParticles(EMBER, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.35, 0.6, 0.35, 0.0);
				p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.4, p.getZ(), 1, 0.3, 0.3, 0.3, 0.01);
			}
			if (p.tickCount % 20 != 0 || p.isCreative()) continue;
			second(p);
		}
	}

	/** A second of it burnt on {@code p} (package-private for the game tests): its share of the span, or out it goes, too few left. */
	static void second(ServerPlayer p) {
		PlayerCultivation own = CultivationManager.own(p);
		if (tooFew(own)) {
			putOut(p, ModLang.MSG_BURN_OUT);
			return;
		}
		Lifespan.age(p, yearsPerSecond(own));
	}

	/**
	 * Its switch thrown (CultivationManager#toggleAbility, after the switch): lit (refused with too few years left), or put out; told,
	 * heard.
	 */
	static void toggled(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		boolean lit = c.isAbilityEnabled(Ability.BURN_LIFESPAN);
		PlayerCultivation own = CultivationManager.own(p);
		if (lit && !p.isCreative() && tooFew(own)) {
			c.setAbilityEnabled(Ability.BURN_LIFESPAN, false);
			CultivationManager.sync(p);
			p.displayClientMessage(Component.translatable(ModLang.MSG_BURN_TOO_FEW).withStyle(ChatFormatting.RED), true);
			return;
		}
		speed(p, lit);
		if (lit) {
			p.displayClientMessage((Lifespan.endless(own) ? Component.translatable(ModLang.MSG_BURN_ON_ENDLESS)
					: Component.translatable(ModLang.MSG_BURN_ON, String.format("%.1f", yearsPerSecond(own)), percent())).withStyle(ChatFormatting.RED), true);
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9f, 0.6f);
		} else {
			p.displayClientMessage(Component.translatable(ModLang.MSG_BURN_OFF), true);
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 0.8f);
		}
	}

	/** Put out (told why, if {@code why}): the switch off, the speed gone. */
	static void putOut(ServerPlayer p, String why) {
		PlayerCultivation c = CultivationManager.get(p);
		if (c.isAbilityEnabled(Ability.BURN_LIFESPAN)) {
			c.setAbilityEnabled(Ability.BURN_LIFESPAN, false);
			CultivationManager.markDirty(p.server);
			CultivationManager.sync(p);
		}
		speed(p, false);
		if (why != null) {
			p.sendSystemMessage(Component.translatable(why).withStyle(ChatFormatting.RED));
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 0.8f);
		}
	}

	/** {@link #SPAN_PER_SECOND} as said: "0.5" (percent). */
	public static String percent() {
		return String.format("%.1f", SPAN_PER_SECOND * 100);
	}

	/** A blow or a spell of {@code attacker}'s (Physiques#outgoing): {@link #HARM} times as hard while its lifespan burns. */
	public static float outgoing(ServerPlayer attacker, float amount) {
		return isBurning(attacker) ? amount * HARM : amount;
	}

	/** Its speed on {@code p} while it burns, or none. */
	private static void speed(ServerPlayer p, boolean burning) {
		AttributeInstance instance = p.getAttribute(Attributes.MOVEMENT_SPEED);
		if (instance == null) return;
		boolean has = instance.getModifier(SPEED_ID) != null;
		if (burning && !has) {
			instance.addTransientModifier(new AttributeModifier(SPEED_ID, "Burn Lifespan", SPEED - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
		} else if (!burning && has) {
			instance.removeModifier(SPEED_ID);
		}
	}

	/** A logout: the flame put out (it isn't lit again by logging in). */
	public static void onDisconnect(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (c.isAbilityEnabled(Ability.BURN_LIFESPAN)) {
			c.setAbilityEnabled(Ability.BURN_LIFESPAN, false);
			CultivationManager.markDirty(p.server);
		}
	}

	private BurnLifespan() {}
}
