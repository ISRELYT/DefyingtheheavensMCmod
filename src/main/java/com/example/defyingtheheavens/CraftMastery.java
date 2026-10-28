package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Mastery of the crafts (the user's pick, 2026-10-28: "the more you do it the better results you get"): Alchemy, Forging and Formations,
 * each its own, grown by practice and kept for life (a reincarnation carries it over: PlayerCultivation#carryOver). Its ranks, Apprentice
 * to Saint ({@link Rank}), by the practice gathered ({@link Rank#from}).
 * <p>
 * What a rank does in alchemy (AlchemyCauldronBlockEntity): a brew harder than one's rank can fail ({@link #failChance}: the herbs
 * ruined, half the practice still learned), a pill can come out a grade better than the herbs alone would give ({@link #betterGrade}: never
 * past the best grade herbs of that age can give at all), and a Grandmaster or a Saint now and then draws two pills from one brew
 * ({@link #secondPill}). Forging and Formations are practised at the Artifact Forge and on the formations themselves.
 */
public final class CraftMastery {
	public enum Craft {
		ALCHEMY("alchemy"), FORGING("forging"), FORMATIONS("formations");

		private final String id;

		Craft(String id) {
			this.id = id;
		}

		public String getId() { return id; }

		public Component getDisplayName() { return Component.translatable(ModLang.craftName(id)); }
	}

	/** A craft's ranks, and the practice each begins at: a handful of brews to Adept, a hundred or so to Saint. */
	public enum Rank {
		APPRENTICE("apprentice", 0, ChatFormatting.GRAY),
		ADEPT("adept", 20, ChatFormatting.GREEN),
		MASTER("master", 80, ChatFormatting.AQUA),
		GRANDMASTER("grandmaster", 250, ChatFormatting.LIGHT_PURPLE),
		SAINT("saint", 600, ChatFormatting.GOLD);

		private final String id;
		public final int from;
		public final ChatFormatting color;

		Rank(String id, int from, ChatFormatting color) {
			this.id = id;
			this.from = from;
			this.color = color;
		}

		public Component getDisplayName() { return Component.translatable(ModLang.craftRank(id)).withStyle(color); }

		/** The rank this much practice has reached. */
		public static Rank of(int practice) {
			Rank reached = APPRENTICE;
			for (Rank rank : values()) if (practice >= rank.from) reached = rank;
			return reached;
		}

		/** The next rank, or null at Saint. */
		public Rank next() {
			return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
		}
	}

	/** Each rank short of a brew's difficulty: this much more chance it fails. */
	public static final double FAIL_PER_RANK_SHORT = 0.12;
	/** Each rank past Apprentice: this much chance a pill comes out a grade better. */
	public static final double BETTER_GRADE_PER_RANK = 0.10;
	/** A Grandmaster's chance of a second pill from one brew, and a Saint's. */
	public static final double SECOND_PILL_GRANDMASTER = 0.10, SECOND_PILL_SAINT = 0.25;

	public static Rank rank(PlayerCultivation c, Craft craft) {
		return Rank.of(c.getMastery(craft));
	}

	/** The chance a brew of {@code difficulty} (AlchemyRecipes) fails in the hands of {@code rank}: none once the rank reaches it. */
	public static double failChance(Rank rank, int difficulty) {
		return Math.max(0, difficulty - rank.ordinal()) * FAIL_PER_RANK_SHORT;
	}

	/** The chance a pill brewed by {@code rank} comes out a grade better than its herbs rolled. */
	public static double betterGrade(Rank rank) {
		return rank.ordinal() * BETTER_GRADE_PER_RANK;
	}

	/** The chance {@code rank} draws a second pill from one brew. */
	public static double secondPill(Rank rank) {
		return rank == Rank.SAINT ? SECOND_PILL_SAINT : rank == Rank.GRANDMASTER ? SECOND_PILL_GRANDMASTER : 0;
	}

	/**
	 * {@code player} practised {@code craft} for {@code practice}: kept on their own record (a soul in another's body learns for itself),
	 * and told of a new rank reached, with a chime.
	 */
	public static void practise(ServerPlayer player, Craft craft, int practice) {
		if (practice <= 0) return;
		PlayerCultivation own = CultivationManager.own(player);
		Rank before = Rank.of(own.getMastery(craft));
		own.setMastery(craft, own.getMastery(craft) + practice);
		Rank after = Rank.of(own.getMastery(craft));
		if (after != before) {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_CRAFT_RANK, craft.getDisplayName(), after.getDisplayName()).withStyle(ChatFormatting.GOLD));
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
		}
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
	}

	private CraftMastery() {}
}
