package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Weighted Training (CultivationMethod#WEIGHTED_TRAINING, the user's "Go, all four", 2026-11-01): the body worked under the weight of heavy
 * armour. One drawn in by this way, on its own feet (not riding, flying or gliding; in water, only swimming), tempers by every block it covers
 * walking, running, jumping or swimming, and climbing ({@link #tick}): {@link #PER_BLOCK} seconds of the Body Path's own pace a block, times its armour's
 * weight ({@link #load}: its pieces' defence and toughness over {@link #LOAD_UNIT}, at most {@link #LOAD_MOST}; under {@link #LOAD_LEAST},
 * none: no armour, no training). A step past {@link #MOST_STEP} a tick isn't its own (a leap, a fling, a teleport). Meditation tempers
 * nothing on it.
 */
public final class WeightedTraining {
	public static final double PER_BLOCK = 0.4, LOAD_UNIT = 15, LOAD_MOST = 2.5, LOAD_LEAST = 0.2;
	/** A tick's step past this is no step of one's own: not counted. */
	static final double MOST_STEP = 0.8;
	/** How often (ticks) one in training is told what it has won meanwhile, or that it wears no weight. */
	static final int TELL_EVERY = 200;

	/** Where each was last tick, what of its steps under weight this second holds (blocks times load), what it has won since last told. */
	private static final Map<UUID, Vec3> LAST = new HashMap<>();
	private static final Map<UUID, double[]> TRAINING = new HashMap<>();
	/** When each was last told (server ticks). */
	private static final Map<UUID, Integer> TOLD = new HashMap<>();

	/** The weight {@code p}'s armour puts on it: its pieces' defence and toughness over {@link #LOAD_UNIT} (full iron 1, netherite 2.1). */
	public static double load(Player p) {
		double weight = 0;
		for (ItemStack piece : p.getArmorSlots()) {
			if (piece.getItem() instanceof ArmorItem armor) weight += armor.getDefense() + armor.getToughness();
		}
		return Math.min(LOAD_MOST, weight / LOAD_UNIT);
	}

	/** Every tick: the steps of those in training counted under their weight; once a second, what they come to tempered. */
	public static void tick(MinecraftServer server) {
		int now = server.getTickCount();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			UUID id = p.getUUID();
			if (!p.isAlive() || p.isSpectator() || !BodyPath.practises(CultivationManager.get(p), CultivationMethod.WEIGHTED_TRAINING)) {
				LAST.remove(id);
				TRAINING.remove(id);
				continue;
			}
			Vec3 at = p.position(), was = LAST.put(id, at);
			if (was == null || p.isPassenger() || p.getAbilities().flying || p.isFallFlying()) continue;
			// In water, only its own strokes (swimming): a current carries one for nothing (a stream left to carry one round trained it
			// unattended before the sweep of 2026-11-01).
			if (p.isInWater() && !p.isSwimming()) continue;
			double dx = at.x - was.x, dz = at.z - was.z;
			double step = Math.sqrt(dx * dx + dz * dz) + (p.onClimbable() ? Math.abs(at.y - was.y) : 0);
			if (step <= 1.0e-3 || step > MOST_STEP) continue;
			double load = load(p);
			if (load < LOAD_LEAST) {
				if (now - TOLD.getOrDefault(id, -TELL_EVERY) >= TELL_EVERY) {
					TOLD.put(id, now);
					p.displayClientMessage(Component.translatable(ModLang.MSG_WEIGHTS_NONE).withStyle(ChatFormatting.GRAY), true);
				}
				continue;
			}
			TRAINING.computeIfAbsent(id, k -> new double[2])[0] += step * load;
		}
		if (now % 20 != 0 || TRAINING.isEmpty()) return;
		for (UUID id : new java.util.ArrayList<>(TRAINING.keySet())) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p != null) settle(p);
		}
	}

	/** A second of {@code p}'s training settled: what its steps under weight came to, tempered; told of what it has won every {@link #TELL_EVERY}. */
	static void settle(ServerPlayer p) {
		UUID id = p.getUUID();
		double[] held = TRAINING.get(id);
		if (held == null || held[0] <= 0) return;
		held[1] += BodyPath.temperBy(p, CultivationMethod.WEIGHTED_TRAINING, held[0] * PER_BLOCK);
		held[0] = 0;
		int now = p.server.getTickCount();
		if (held[1] > 0 && now - TOLD.getOrDefault(id, -TELL_EVERY) >= TELL_EVERY) {
			TOLD.put(id, now);
			p.displayClientMessage(Component.translatable(ModLang.MSG_WEIGHTS_TRAINING, BodyTechniques.number(Math.round(load(p) * 10) / 10.0),
					BodyPath.percent(held[1])).withStyle(ChatFormatting.GRAY), true);
			held[1] = 0;
		}
	}

	public static void forget(UUID id) {
		LAST.remove(id);
		TRAINING.remove(id);
		TOLD.remove(id);
	}

	public static void clear() {
		LAST.clear();
		TRAINING.clear();
		TOLD.clear();
	}

	private WeightedTraining() {}
}
