package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Medicinal Bath (CultivationMethod#MEDICINAL_BATH, the user's "Go, all four", 2026-11-01): the body soaked in a water cauldron steeped with
 * spirit herbs. Any spirit herb (a GinsengItem, not the Netherworld's flowers) used on a water cauldron steeps it ({@link #steep}): each herb
 * gives it {@link #SECONDS_PER_HERB} seconds of soaking at its own strength (GinsengItem#grade: by its age and kind), at most {@link
 * #MOST_SECONDS} in all. One drawn in by this way standing in a steeped cauldron soaks ({@link #soak}): every second {@link #PACE} seconds of
 * the Body Path's own pace, twice for each realm the herb soaked stands above the body (at most {@link #MOST_ABOVE}), half for each below (at
 * least an eighth), a second of the strongest herb's soaking spent. (One strong herb made the whole bath as strong before the sweep of
 * 2026-11-01: an old ginseng and nine weeds were ten minutes of the ginseng.) Anyone may steep one; only this way tempers by it. Meditation
 * tempers nothing on it. Kept in memory: a world closed lets the steep go, and a cauldron emptied or broken loses it.
 */
public final class MedicinalBath {
	public static final int SECONDS_PER_HERB = 60, MOST_SECONDS = 600, MOST_ABOVE = 2;
	public static final double PACE = 2.0;
	/** A bath below the body is worth half for each realm, an eighth at the least. */
	static final int MOST_BELOW = 3;

	private static final DustParticleOptions HERBAL = new DustParticleOptions(new Vector3f(0.45f, 0.72f, 0.28f), 0.9f);

	/** A steeped cauldron: the seconds of soaking left in it at each strength its herbs gave. */
	private static final class Bath {
		final EnumMap<Realm, Integer> seconds = new EnumMap<>(Realm.class);

		int total() {
			int total = 0;
			for (int s : seconds.values()) total += s;
			return total;
		}

		/** The strongest herbs' strength with soaking left, soaked first (null: none left). */
		Realm strongest() {
			Realm strongest = null;
			for (Realm grade : seconds.keySet()) strongest = grade; // (in the realms' order)
			return strongest;
		}
	}

	private static final Map<GlobalPos, Bath> BATHS = new HashMap<>();

	/** Whether {@code stack} steeps a bath: a spirit herb, not the Netherworld's flowers (their yin is no bath for the flesh). */
	public static boolean isHerb(ItemStack stack) {
		return stack.getItem() instanceof GinsengItem && !stack.is(ModItems.OTHER_SHORE_FLOWER) && !stack.is(ModItems.NETHERWORLD_LOTUS);
	}

	/** What a second's soak in a bath of {@code grade} is worth to a body standing in {@code realm}: twice a realm above, half a realm below. */
	public static double worth(Realm grade, Realm realm) {
		return Math.pow(2, Math.max(-MOST_BELOW, Math.min(MOST_ABOVE, grade.ordinal() - realm.ordinal())));
	}

	/** UseBlockCallback: a spirit herb used on a water cauldron steeps it (and isn't eaten). */
	public static InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSpectator() || !isHerb(stack) || !level.getBlockState(hit.getBlockPos()).is(Blocks.WATER_CAULDRON)) return InteractionResult.PASS;
		if (level.isClientSide) return InteractionResult.SUCCESS;
		steep((ServerPlayer) player, (ServerLevel) level, hit.getBlockPos(), stack);
		return InteractionResult.SUCCESS;
	}

	/**
	 * {@code herb} steeps the water cauldron at {@code pos}: its seconds added at its own strength; refused, told, with no room left in it. One
	 * herb used (not in creative). @return whether it steeped
	 */
	public static boolean steep(ServerPlayer p, ServerLevel level, BlockPos pos, ItemStack herb) {
		GlobalPos at = GlobalPos.of(level.dimension(), pos.immutable());
		Bath bath = BATHS.get(at);
		if (bath != null && bath.total() + SECONDS_PER_HERB > MOST_SECONDS) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_BATH_FULL, MOST_SECONDS).withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		Realm grade = GinsengItem.grade(herb);
		if (bath == null) BATHS.put(at, bath = new Bath());
		bath.seconds.merge(grade, SECONDS_PER_HERB, Integer::sum);
		if (!p.getAbilities().instabuild) herb.shrink(1);
		double x = pos.getX() + 0.5, y = pos.getY() + 0.95, z = pos.getZ() + 0.5;
		level.sendParticles(HERBAL, x, y, z, 14, 0.2, 0.02, 0.2, 0);
		level.sendParticles(ParticleTypes.SPLASH, x, y, z, 10, 0.2, 0.05, 0.2, 0.1);
		level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.6f, 1.4f);
		level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.6f, 1.2f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_BATH_STEEPED, bath.total(), grade.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
		return true;
	}

	/** Every tick: the steeped cauldrons seen (their green and bubbles), those whose water is gone forgotten; once a second, the soaks. */
	public static void tick(MinecraftServer server) {
		if (BATHS.isEmpty()) return;
		int tick = server.getTickCount();
		if (tick % 10 == 0) {
			for (Iterator<Map.Entry<GlobalPos, Bath>> it = BATHS.entrySet().iterator(); it.hasNext(); ) {
				GlobalPos at = it.next().getKey();
				ServerLevel level = server.getLevel(at.dimension());
				if (level == null || !level.isLoaded(at.pos())) continue;
				if (!level.getBlockState(at.pos()).is(Blocks.WATER_CAULDRON)) {
					it.remove(); // the water gone, the steep with it
					continue;
				}
				double x = at.pos().getX() + 0.5, y = at.pos().getY() + 0.95, z = at.pos().getZ() + 0.5;
				level.sendParticles(HERBAL, x, y, z, 3, 0.22, 0.02, 0.22, 0);
				level.sendParticles(ParticleTypes.BUBBLE_POP, x, y, z, 2, 0.22, 0.02, 0.22, 0);
			}
		}
		if (tick % 20 != 0) return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) soak(p);
	}

	/**
	 * A second in the bath, for {@code p} drawn in by this way standing in a steeped cauldron: tempered by its strongest herbs, a second of
	 * theirs spent (none while the body is held at a bottleneck or has no volume of its realm), told.
	 */
	static void soak(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (!p.isAlive() || !BodyPath.practises(c, CultivationMethod.MEDICINAL_BATH)) return;
		ServerLevel level = p.serverLevel();
		BlockPos pos = p.blockPosition();
		GlobalPos at = GlobalPos.of(level.dimension(), pos);
		Bath bath = BATHS.get(at);
		if (bath == null || !level.getBlockState(pos).is(Blocks.WATER_CAULDRON)) return;
		Realm grade = bath.strongest();
		if (grade == null) {
			BATHS.remove(at);
			return;
		}
		double fraction = BodyPath.temperBy(p, CultivationMethod.MEDICINAL_BATH, PACE * worth(grade, BodyPath.realm(BodyPath.rank(c))));
		if (fraction <= 0) return;
		if (bath.seconds.merge(grade, -1, Integer::sum) <= 0) bath.seconds.remove(grade);
		if (bath.seconds.isEmpty()) {
			BATHS.remove(at);
			p.displayClientMessage(Component.translatable(ModLang.MSG_BATH_SPENT).withStyle(ChatFormatting.GRAY), true);
		} else {
			p.displayClientMessage(Component.translatable(ModLang.MSG_BATH_SOAKING, BodyPath.percent(fraction), bath.total()).withStyle(ChatFormatting.GREEN), true);
		}
		level.sendParticles(ParticleTypes.SPLASH, p.getX(), pos.getY() + 0.95, p.getZ(), 6, 0.3, 0.02, 0.3, 0.05);
	}

	/** The seconds of soaking left in the bath at {@code pos} (0: not steeped): the game tests. */
	static int secondsLeft(ServerLevel level, BlockPos pos) {
		Bath bath = BATHS.get(GlobalPos.of(level.dimension(), pos));
		return bath == null ? 0 : bath.total();
	}

	/** The server stopped: the steeps go with it. */
	public static void clear() {
		BATHS.clear();
	}

	private MedicinalBath() {}
}
