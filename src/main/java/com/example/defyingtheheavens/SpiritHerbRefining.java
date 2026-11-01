package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Spirit Herb Refining (CultivationMethod#SPIRIT_HERB_REFINING; the user's pick of 2026-11-01: "absorb cultivation from any herb ... just a
 * method for getting more out of herbs really"): the heaven path's way of spirit herbs eaten for their essence. Practising it, any herb
 * (GinsengItem, the Netherworld's too) is eaten as food is (GinsengItem#use) and refined into the stage ({@link #refine}): {@link #SHARE}
 * of it at the herb's own realm (GinsengItem#grade: by its age, a rarer kind a realm higher), twice as much for each realm it stands above
 * the heaven path (at most {@link #MOST_ABOVE}), half for each below; then every herb rests {@link #SETTLE} ticks while the essence
 * settles. Meditation gathers nothing on it.
 */
public final class SpiritHerbRefining {
	public static final double SHARE = 0.1;
	public static final int MOST_ABOVE = 2, SETTLE = 600;

	/** Every herb item (a GinsengItem), to rest together. */
	private static List<Item> herbs;

	/** Whether {@code c} eats herbs for their essence: one of the living, its meridians open, its heaven path practising it. */
	public static boolean practises(PlayerCultivation c) {
		return HeavenMethods.canLearn(c) && CultivationMethods.practised(c, TwoPaths.Path.HEAVEN) == CultivationMethod.SPIRIT_HERB_REFINING;
	}

	/** What a herb of {@code grade} wins of the stage of a heaven path standing in {@code realm}. */
	public static double worth(Realm grade, Realm realm) {
		return SHARE * Math.pow(2, Math.min(MOST_ABOVE, grade.ordinal() - realm.ordinal()));
	}

	/**
	 * {@code p} has eaten {@code herb} (GinsengItem#finishUsingItem): its essence refined into the stage (CultivationMethods#grant, its years
	 * paid), and every herb resting; refused (told), nothing eaten, with no volume of the realm or the stage full. @return whether it was eaten
	 */
	static boolean refine(ServerPlayer p, ItemStack herb) {
		PlayerCultivation c = CultivationManager.get(p);
		if (!CultivationMethods.holdsVolume(c, TwoPaths.Path.HEAVEN)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_METHOD_NO_VOLUME, CultivationMethod.SPIRIT_HERB_REFINING.getDisplayName(),
					c.getRealm().getDisplayName()).withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		if (c.isStalled()) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_HERB_STALLED).withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		if (CultivationMethods.grant(p, TwoPaths.Path.HEAVEN, CultivationMethod.SPIRIT_HERB_REFINING, worth(GinsengItem.grade(herb), c.getRealm())) <= 0) return false;
		for (Item item : herbs()) p.getCooldowns().addCooldown(item, SETTLE);
		// The essence taking hold: a green ring about the body at chest height, never before the eyes.
		ServerLevel level = p.serverLevel();
		for (int i = 0; i < 14; i++) {
			double a = Math.PI * 2 * i / 14;
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + Math.cos(a) * 1.3, p.getY() + 0.2 + p.getRandom().nextDouble() * 0.6,
					p.getZ() + Math.sin(a) * 1.3, 1, 0, 0.05, 0, 0);
		}
		level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.3f);
		return true;
	}

	private static List<Item> herbs() {
		if (herbs == null) herbs = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof GinsengItem).toList();
		return herbs;
	}

	private SpiritHerbRefining() {}
}
