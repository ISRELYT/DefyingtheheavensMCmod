package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thunder Tempering (CultivationMethod#THUNDER_TEMPERING; the methods overhaul's phase 2): the Heavenly Thunder Body's own way, known from
 * birth and chosen only if it will (the user, 2026-10-31: "a unique method that only the heavenly thunder body can utilise if they wish
 * to"). Meditating under the open sky, it calls the lightning down on itself: a strike every {@link #INTERVAL} ticks, every
 * {@link #STORM_INTERVAL} in a thunderstorm. Each strike is real, {@link #HARM} of its health (a strike that would end it, ends it), and
 * wins what {@link #STRIKE_SECONDS} of plain meditation would (PlayerCultivation#cultivationPerSecond), half again in a storm
 * (CultivationMethods#grant: its years by the method's half weight).
 */
public final class ThunderTempering {
	public static final int INTERVAL = 30 * 20, STORM_INTERVAL = 10 * 20;
	public static final double STRIKE_SECONDS = 60, STORM = 1.5;
	public static final float HARM = 0.15f;

	/** When each meditator's next strike falls (game time); not saved. */
	private static final Map<UUID, Long> NEXT = new HashMap<>();

	/**
	 * Half a second of {@code p} meditating by Thunder Tempering (MeditationManager): its body under the open sky (where it sits, its soul
	 * turned inward or not: InnerRealm#bodyPos), the strike when its time is up.
	 */
	static void meditating(ServerPlayer p, PlayerCultivation c) {
		ServerLevel level = p.server.getLevel(InnerRealm.bodyDimension(p));
		if (level == null) level = p.serverLevel();
		BlockPos body = InnerRealm.bodyPos(p);
		if (!level.canSeeSky(body.above())) {
			NEXT.remove(p.getUUID());
			return;
		}
		long now = level.getGameTime();
		boolean storm = level.isThundering();
		Long next = NEXT.get(p.getUUID());
		if (next == null) {
			NEXT.put(p.getUUID(), now + (storm ? STORM_INTERVAL : INTERVAL));
			p.displayClientMessage(Component.translatable(ModLang.MSG_THUNDER_CALLED).withStyle(ChatFormatting.AQUA), true);
			return;
		}
		if (now < next) return;
		NEXT.put(p.getUUID(), now + (storm ? STORM_INTERVAL : INTERVAL));
		strike(p, c, storm, level, body);
	}

	/** The lightning called down on {@code p} where it stands. */
	static void strike(ServerPlayer p, PlayerCultivation c, boolean storm) {
		strike(p, c, storm, p.serverLevel(), p.blockPosition());
	}

	/** The lightning called down on {@code p}'s body at {@code body} in {@code level}: its harm, then its share of the stage. */
	static void strike(ServerPlayer p, PlayerCultivation c, boolean storm, ServerLevel level, BlockPos body) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt != null) {
			bolt.moveTo(body.getX() + 0.5, body.getY(), body.getZ() + 0.5);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
		// Real harm, though no blow to break the meditation (CultivationEvents ends one at a blow): its health taken, a death if that is all.
		float harm = p.getMaxHealth() * HARM;
		if (!p.isCreative()) {
			if (p.getHealth() <= harm) {
				p.hurt(p.serverLevel().damageSources().lightningBolt(), Float.MAX_VALUE);
				return;
			}
			p.setHealth(p.getHealth() - harm);
		}
		double fraction = c.cultivationPerSecond() * STRIKE_SECONDS * (storm ? STORM : 1.0) / Math.max(1.0, c.cultivationRequired());
		CultivationMethods.grant(p, TwoPaths.Path.HEAVEN, CultivationMethod.THUNDER_TEMPERING, fraction);
	}

	/** A player left, or stopped. */
	public static void forget(UUID id) {
		NEXT.remove(id);
	}

	private ThunderTempering() {}
}
