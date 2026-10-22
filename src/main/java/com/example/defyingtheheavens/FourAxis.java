package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

/**
 * The four axes of virtue (the user's ask, 2026-10-19): Four Axis is not climbed on qi alone. Each of its stages has its axis, and a
 * breakthrough from the stage (on either path) waits on that axis being complete: Early the Axis of Longevity, Mid of Fortune, Late
 * of Merit, Grand Perfection of Enlightenment (the last completes the realm). Each axis is filled by its own measure ({@link #need};
 * the user's ask, 2026-10-20), all of it won through the wishes of the lower realms' people, mortals and lesser cultivators, in the
 * Astral Realm's settlements (Wishes): each axis by wishes of its own kind, and only the axis one stands at, in order. The axes are
 * the cultivator's, not one path's: saved on the record (PlayerCultivation#axisProgress), and lost with a spent life (Lifespan#wipe).
 */
public final class FourAxis {
	/** The four axes, in order: each a Four Axis stage's, with what completes it in its own measure. Saved by place: new ones never. */
	public enum Axis {
		/** Years of life taken from the sick one heals (Wishes#takeYears: the user's 500), each onto one's own lifespan as well. */
		LONGEVITY("longevity", 500),
		/** Low Spirit Stones' worth absorbed of the fortune settlements share for what one brings them (Wishes#give, SpiritStoneItem#fortuneOf). */
		FORTUNE("fortune", 200),
		/** Hearts (max health, halved) of the threats slain by one's own hand (Wishes.Foe): a beast, raiders, the restless dead... */
		MERIT("merit", 200),
		/** Stages lesser cultivators are guided through, beside them or from within their bodies (Wishes#guide, #guideWithin). */
		ENLIGHTENMENT("enlightenment", 6);

		private final String id;
		private final int need;

		Axis(String id, int need) {
			this.id = id;
			this.need = need;
		}

		public String getId() { return id; }

		/** What completes it, its obsession aside ({@link FourAxis#need}). */
		public int baseNeed() { return need; }

		public Component getDisplayName() { return Component.translatable(ModLang.AXIS_PREFIX + id); }

		/** What it is measured in, said after a count: "years", "hearts". */
		public Component unit() { return Component.translatable(ModLang.AXIS_UNIT_PREFIX + id); }

		/** The Four Axis stage it belongs to. */
		public Stage stage() { return Stage.byIndex(ordinal()); }

		public static Axis byIndex(int i) {
			Axis[] all = values();
			return i >= 0 && i < all.length ? all[i] : null;
		}

		/** The axis of this id, or null (none, or unknown). */
		public static Axis byId(String id) {
			for (Axis axis : values()) if (axis.id.equals(id)) return axis;
			return null;
		}
	}

	/** The axis a breakthrough from {@code rank} (PlayerCultivation#rank, either path's) waits on, or null outside Four Axis. */
	public static Axis wanted(int rank) {
		if (rank < 0 || rank / Stage.values().length != Realm.FOUR_AXIS.ordinal()) return null;
		return Axis.byIndex(rank % Stage.values().length);
	}

	/** What completes {@code axis} for {@code c}: its own measure, or half of it for the axis of {@code c}'s obsession (Obsession#need). */
	public static int need(PlayerCultivation c, Axis axis) {
		return Obsession.need(c, axis, axis.baseNeed());
	}

	/** How far {@code c} has filled {@code axis}, no further than it needs. */
	public static int progress(PlayerCultivation c, Axis axis) {
		return Math.min(need(c, axis), c.axisProgress(axis));
	}

	public static boolean complete(PlayerCultivation c, Axis axis) {
		return c.axisProgress(axis) >= need(c, axis);
	}

	/** Whether a breakthrough from {@code rank} waits on its axis, not yet complete. */
	public static boolean holds(PlayerCultivation c, int rank) {
		Axis axis = wanted(rank);
		return axis != null && !complete(c, axis);
	}

	/**
	 * The axis {@code c} works at now: the first not yet complete, among those of the Four Axis stages its higher path has reached;
	 * null below Four Axis or past it (the axes are its stages' own: no path passes it with one wanting), or with all it has reached
	 * complete.
	 */
	public static Axis current(PlayerCultivation c) {
		int rank = TwoPaths.higherRank(c, false);
		if (rank < 0 || rank / Stage.values().length != Realm.FOUR_AXIS.ordinal()) return null;
		int reached = rank % Stage.values().length;
		for (Axis axis : Axis.values()) {
			if (axis.ordinal() > reached) return null;
			if (!complete(c, axis)) return axis;
		}
		return null;
	}

	/** Whether all four are complete (Four Axis itself completed). */
	public static boolean allComplete(PlayerCultivation c) {
		for (Axis axis : Axis.values()) if (!complete(c, axis)) return false;
		return true;
	}

	/**
	 * Why a breakthrough from {@code rank} waits ({@link #holds}): its axis to complete, how far it stands, and how it is filled (the
	 * Cultivation tab shows a meter instead).
	 */
	public static Component gateLine(PlayerCultivation c, int rank) {
		Axis axis = wanted(rank);
		return Component.translatable(ModLang.AXIS_GATE, axis.getDisplayName(), progress(c, axis), need(c, axis), axis.unit())
				.append(" ").append(howLine(c, axis));
	}

	/** How {@code axis} is filled, and how much of it {@code c} needs in all. */
	public static Component howLine(PlayerCultivation c, Axis axis) {
		return Component.translatable(ModLang.AXIS_HOW_PREFIX + axis.getId(), need(c, axis));
	}

	/**
	 * {@code player} wins {@code amount} toward {@code axis}, in its measure (years taken, a stone's worth absorbed, a threat's hearts,
	 * a stage guided: Wishes, SpiritStoneItem), if it is the axis they stand at ({@link #current}); told, and told again when it is
	 * complete. @return how much of it was taken in (none past what completes it)
	 */
	public static int add(ServerPlayer player, Axis axis, int amount) {
		return add(player.server, player.getUUID(), axis, amount);
	}

	/** {@link #add} by the player's id, online or not (a threat slain while they were away: told only if they are here). */
	public static int add(MinecraftServer server, UUID id, Axis axis, int amount) {
		PlayerCultivation c = CultivationManager.own(server, id);
		if (amount <= 0 || current(c) != axis) return 0;
		int need = need(c, axis);
		int before = c.axisProgress(axis);
		int after = Math.min(need, before + amount);
		if (after <= before) return 0;
		c.setAxisProgress(axis, after);
		CultivationManager.markDirty(server);
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player == null) return after - before;
		CultivationManager.sync(player);
		if (after >= need) {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_AXIS_COMPLETE, axis.getDisplayName()).withStyle(ChatFormatting.GOLD));
			player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.0f);
		} else {
			player.displayClientMessage(Component.translatable(ModLang.MSG_AXIS_PROGRESS, after - before, axis.getDisplayName(), after, need, axis.unit())
					.withStyle(ChatFormatting.YELLOW), true);
		}
		return after - before;
	}

	/**
	 * What {@code c} had of each axis in virtue (saves from before the measures: 100 completed one, the obsession's counted twice),
	 * in each axis's measure now: as far along, one complete still complete.
	 */
	static int fromVirtue(PlayerCultivation c, Axis axis, int virtue) {
		int need = need(c, axis);
		return virtue >= OLD_VIRTUE ? need : (int) Math.round(Math.max(0, virtue) / (double) OLD_VIRTUE * need);
	}

	/** Virtue that completed an axis before the measures (PlayerCultivation's "AxisVirtue" saves). */
	private static final int OLD_VIRTUE = 100;

	private FourAxis() {}
}
