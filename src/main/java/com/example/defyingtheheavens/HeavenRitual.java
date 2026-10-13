package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The Ritual to the Heavens: the heavens' leave to cultivate past Qi Refining Mid (see PlayerCultivation#needsHeavensLeave).
 * <ol>
 *   <li>Lay offerings on a Heaven Altar's tray (anything at all; see {@link HeavenAltar} for how they are weighed).</li>
 *   <li>Sneak and right-click it, empty-handed, to begin: the offerings rise as smoke and are taken, whatever happens next,
 *   and the chance is set by the offering, the altar and its height.</li>
 *   <li>Kneel there, unmoving, for {@link #DURATION} ticks (three minutes): moving a third of a block (being knocked back
 *   too), leaving, dying, or losing the altar breaks the ritual, and the offering is gone.</li>
 *   <li>Then the heavens answer: their leave (and, for a Generous offering or better, Heaven's Favour), or silence, and no
 *   new ritual until the next dawn.</li>
 * </ol>
 * Every path performs the same ritual. Sect cultivators perform it too, at their sect's Altar of Heaven (see NpcLife).
 */
public final class HeavenRitual {
	/** Three minutes. */
	public static final int DURATION = 3 * 60 * 20;
	/** How far (blocks, horizontally) the kneeling cultivator may drift before the ritual counts as broken. */
	public static final double STILL = 1.0 / 3.0;
	public static final long DAY = 24_000L;

	/** One ritual under way: where, from where the cultivator kneels, until when, and how it will be judged. */
	private static final class Ritual {
		final ResourceKey<Level> dimension;
		final BlockPos altar;
		final double x, y, z;
		final long start, end;
		final double chance;
		final HeavenAltar.Offering offering;

		Ritual(ResourceKey<Level> dimension, BlockPos altar, double x, double y, double z, long start, double chance, HeavenAltar.Offering offering) {
			this.dimension = dimension;
			this.altar = altar;
			this.x = x;
			this.y = y;
			this.z = z;
			this.start = start;
			this.end = start + DURATION;
			this.chance = chance;
			this.offering = offering;
		}
	}

	private static final Map<UUID, Ritual> ACTIVE = new HashMap<>();

	public static boolean isActive(UUID player) { return ACTIVE.containsKey(player); }

	/** Someone is praying at the altar at {@code pos}. */
	public static boolean isAltarInUse(Level level, BlockPos pos) {
		for (Ritual r : ACTIVE.values()) if (r.dimension == level.dimension() && r.altar.equals(pos)) return true;
		return NpcLife.isAltarInUse(level, pos);
	}

	/** The game day (from dawn to dawn), by the Overworld's clock. */
	public static long today(MinecraftServer server) {
		return Math.floorDiv(server.overworld().getDayTime(), DAY);
	}

	/** Why {@code player} can't begin a ritual now (a message key), or null if they can. */
	static String refusal(ServerPlayer player, PlayerCultivation c) {
		if (ModDimensions.isUnseenByHeaven(player.level().dimension())) return ModLang.MSG_HEAVENS_BLIND;
		if (c.isMortal() || !c.needsHeavensLeave()) return ModLang.MSG_RITUAL_NOT_NOW;
		if (c.getRitualFailedDay() >= today(player.server)) return ModLang.MSG_RITUAL_DAWN;
		if (isActive(player.getUUID()) || TribulationManager.isActive(player.getUUID()) || SoulProjection.isActive(player.getUUID())) {
			return ModLang.MSG_RITUAL_BUSY;
		}
		return null;
	}

	/** Sneak-use of a Heaven Altar: begins the ritual if the heavens will hear it. */
	public static void begin(ServerPlayer player, BlockPos pos) {
		PlayerCultivation c = CultivationManager.get(player);
		String refusal = refusal(player, c);
		if (refusal != null) {
			player.displayClientMessage(Component.translatable(refusal), true);
			return;
		}
		ServerLevel level = player.serverLevel();
		if (!(level.getBlockEntity(pos) instanceof HeavenAltarBlockEntity altar)) return;
		if (isAltarInUse(level, pos)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_RITUAL_ALTAR_BUSY), true);
			return;
		}
		HeavenAltar.Judgement judgement = HeavenAltar.judge(level, pos);
		if (!judgement.sky()) {
			player.displayClientMessage(Component.translatable(ModLang.ALTAR_NO_SKY), true);
			return;
		}
		HeavenAltar.Offering offering = HeavenAltar.Offering.of(HeavenAltar.worth(altar));
		double chance = HeavenAltar.chance(offering, judgement.grandeur(), HeavenAltar.heightBonus(level, pos));
		// The offerings are the heavens' now, whatever comes of it.
		boolean offered = !altar.isEmpty();
		altar.clearContent();
		if (offered) {
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.2, 0.1, 0.2, 0.02);
			level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6f, 0.6f);
		}
		if (MeditationManager.isMeditating(player.getUUID())) MeditationManager.stop(player, false);
		player.closeContainer();
		ACTIVE.put(player.getUUID(), new Ritual(level.dimension(), pos.immutable(), player.getX(), player.getY(), player.getZ(),
				level.getGameTime(), chance, offering));
		ModPackets.broadcastMeditation(player, true); // kneeling before the altar
		level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 0.7f);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_RITUAL_BEGIN, offering.displayName(), HeavenAltar.percent(chance))
				.withStyle(ChatFormatting.GOLD));
	}

	public static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		Iterator<Map.Entry<UUID, Ritual>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Ritual> entry = it.next();
			Ritual r = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) { // left the game: the ritual is lost with the offering
				it.remove();
				continue;
			}
			ServerLevel level = player.serverLevel();
			double dx = player.getX() - r.x, dz = player.getZ() - r.z;
			boolean moved = dx * dx + dz * dz > STILL * STILL || Math.abs(player.getY() - r.y) > 0.5;
			boolean gone = level.dimension() != r.dimension || !level.getBlockState(r.altar).is(ModBlocks.HEAVEN_ALTAR);
			if (!player.isAlive() || moved || gone) {
				it.remove();
				ModPackets.broadcastMeditation(player, false);
				player.sendSystemMessage(Component.translatable(moved && !gone ? ModLang.MSG_RITUAL_MOVED : ModLang.MSG_RITUAL_BROKEN)
						.withStyle(ChatFormatting.RED));
				level.playSound(null, r.altar, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 0.8f);
				continue;
			}
			long now = level.getGameTime();
			double progress = Math.min(1.0, (now - r.start) / (double) DURATION);
			if (now % 20 == 0) {
				// The kneeling pose: clients drop a meditator's pose without this once-a-second heartbeat (as MeditationManager).
				ModPackets.broadcastMeditation(player, true);
				long left = Math.max(0, (r.end - now) / 20);
				player.displayClientMessage(Component.translatable(ModLang.HUD_RITUAL, left / 60, String.format("%02d", left % 60)), true);
				level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, r.altar.getX() + 0.5, r.altar.getY() + 1.0, r.altar.getZ() + 0.5,
						1, 0.05, 0, 0.05, 0.01);
			}
			// Light gathers over the altar, a little more as the minutes pass.
			if (level.random.nextDouble() < 0.15 + 0.6 * progress) {
				double h = 3 + level.random.nextDouble() * (4 + 10 * progress);
				level.sendParticles(ParticleTypes.END_ROD, r.altar.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6, r.altar.getY() + h,
						r.altar.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6, 1, 0, -0.05, 0, 0.02);
			}
			if (now < r.end) continue;
			it.remove();
			ModPackets.broadcastMeditation(player, false);
			answer(player, r, level.random.nextDouble() < r.chance);
		}
	}

	/** The heavens' answer, at the end of three still minutes. */
	private static void answer(ServerPlayer player, Ritual r, boolean granted) {
		ServerLevel level = player.serverLevel();
		PlayerCultivation c = CultivationManager.get(player);
		if (granted) {
			c.setHeavensLeave(true);
			if (r.offering.favour > c.getFavour()) c.setFavour(r.offering.favour);
			boolean advanced = c.addCultivation(0); // a full Qi Refining Mid steps straight into Late
			CultivationManager.refresh(player);
			for (int i = 0; i < 40; i++) {
				level.sendParticles(ParticleTypes.END_ROD, r.altar.getX() + 0.5, r.altar.getY() + 1 + i * 0.6, r.altar.getZ() + 0.5, 3, 0.15, 0.2, 0.15, 0.01);
			}
			level.playSound(null, r.altar, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 1.2f);
			level.playSound(null, r.altar, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.0f);
			title(player, Component.translatable(ModLang.RITUAL_TITLE_SUCCESS).withStyle(ChatFormatting.GOLD),
					r.offering.favour > 0 ? Component.translatable(ModLang.MSG_RITUAL_FAVOUR, Math.round((c.favourFactor() - 1) * 100)) : null);
			player.sendSystemMessage(Component.translatable(ModLang.MSG_RITUAL_SUCCESS).withStyle(ChatFormatting.GOLD));
			if (r.offering.favour > 0) {
				player.sendSystemMessage(Component.translatable(ModLang.MSG_RITUAL_FAVOUR, Math.round((c.favourFactor() - 1) * 100))
						.withStyle(ChatFormatting.YELLOW));
			}
			if (advanced) {
				player.displayClientMessage(Component.translatable(ModLang.MSG_STAGE_UP, c.getRealm().getDisplayName(), c.getStage().getDisplayName()), true);
				player.setHealth(player.getMaxHealth());
			}
		} else {
			c.setRitualFailedDay(today(player.server));
			CultivationManager.markDirty(player.server);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, r.altar.getX() + 0.5, r.altar.getY() + 1.2, r.altar.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.01);
			level.playSound(null, r.altar, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.6f, 0.7f);
			title(player, Component.translatable(ModLang.RITUAL_TITLE_FAIL).withStyle(ChatFormatting.GRAY), null);
			player.sendSystemMessage(Component.translatable(ModLang.MSG_RITUAL_FAIL).withStyle(ChatFormatting.GRAY));
		}
	}

	private static void title(ServerPlayer player, Component title, Component subtitle) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
		if (subtitle != null) player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	/** Disconnect: the ritual is lost (the offering already taken). */
	public static void forget(UUID player) { ACTIVE.remove(player); }

	public static void clear() { ACTIVE.clear(); }

	/** For the game tests: ends {@code player}'s ritual now, as granted or not. */
	static void finishForTest(ServerPlayer player, boolean granted) {
		Ritual r = ACTIVE.remove(player.getUUID());
		if (r != null) answer(player, r, granted);
	}

	private HeavenRitual() {}
}
