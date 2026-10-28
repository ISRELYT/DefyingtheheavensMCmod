package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Those a legend calls up against whoever takes it (phase 3b of the Artifact Forge): Chiyou's shade, risen as the Xuanyuan Sword is drawn
 * (Legends#draw), and Ao Guang, the East Sea's Dragon King, risen as the Sea-Fixing Needle is pulled from the sea (Legends#pull). Each hunts
 * its one prey, and whoever strikes it ({@link #goesFor}); its prey gone (dead, out of its world, fled past {@link #GIVES_UP}), it goes too.
 * The hunts aren't saved: a guard found after a restart, its hunt forgotten, goes ({@link #stray}).
 */
public final class LegendGuards {
	/** The tag a legend's guard carries. */
	public static final String TAG = DefyingTheHeavens.MOD_ID + "_legend_guard";
	/** How far its prey may get from a guard before it gives the hunt up. */
	public static final double GIVES_UP = 128;

	/** A guard's hunt: its prey, the world it hunts in, and what is told of it when it goes. */
	private record Hunt(UUID prey, ResourceKey<Level> world, String gone) {}

	private static final Map<UUID, Hunt> HUNTS = new HashMap<>();

	private LegendGuards() {}

	/** {@code guard} set on {@code prey}: it hunts them till one of them falls or they get away, {@code gone} told when it goes. */
	static void hunt(CultivatorNpc guard, ServerPlayer prey, String gone) {
		guard.addTag(TAG);
		HUNTS.put(guard.getUUID(), new Hunt(prey.getUUID(), guard.level().dimension(), gone));
		guard.setTarget(prey);
		guard.markAggressed(prey); // it fell on them: striking it down is no murder
	}

	public static boolean is(Entity entity) {
		return entity.getTags().contains(TAG);
	}

	/** Whether {@code guard} goes for {@code target}: its prey, or whoever has struck it. */
	public static boolean goesFor(CultivatorNpc guard, LivingEntity target) {
		Hunt hunt = HUNTS.get(guard.getUUID());
		return hunt != null && target.getUUID().equals(hunt.prey()) || guard.getLastHurtByMob() == target;
	}

	/** {@code guard}'s prey, or null (none, or its hunt forgotten). */
	public static @Nullable UUID preyOf(Entity guard) {
		Hunt hunt = HUNTS.get(guard.getUUID());
		return hunt == null ? null : hunt.prey();
	}

	/** Whether {@code npc} is a guard whose hunt is forgotten (a restart): it goes (CultivatorNpc#tick). */
	public static boolean stray(CultivatorNpc npc) {
		return is(npc) && !npc.level().isClientSide && !HUNTS.containsKey(npc.getUUID());
	}

	/** Every half second (Legends#tick): each guard kept on its prey; its prey gone, it goes. */
	static void tick(MinecraftServer server) {
		if (HUNTS.isEmpty() || server.getTickCount() % 10 != 3) return;
		for (Iterator<Map.Entry<UUID, Hunt>> it = HUNTS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Hunt> entry = it.next();
			Hunt hunt = entry.getValue();
			ServerLevel level = server.getLevel(hunt.world());
			if (level == null || !(level.getEntity(entry.getKey()) instanceof CultivatorNpc guard) || !guard.isAlive()) {
				it.remove(); // slain, or out of the world with its ground (found again, it goes: #stray)
				continue;
			}
			ServerPlayer prey = server.getPlayerList().getPlayer(hunt.prey());
			if (prey == null || !prey.isAlive() || prey.isSpectator() || prey.level() != level || prey.distanceTo(guard) > GIVES_UP) {
				it.remove();
				leave(level, guard, hunt.gone());
				continue;
			}
			LivingEntity target = guard.getTarget();
			if (target == null || !target.isAlive()) guard.setTarget(prey);
		}
	}

	/** {@code guard} goes, its prey got away or gone: told to those near. */
	private static void leave(ServerLevel level, CultivatorNpc guard, String gone) {
		Legends.tellNear(level, guard.blockPosition(), Component.translatable(gone).withStyle(ChatFormatting.GRAY));
		level.sendParticles(guard.isGhost() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.CLOUD, guard.getX(), guard.getY() + guard.getBbHeight() * 0.5,
				guard.getZ(), 50, 0.6, guard.getBbHeight() * 0.4, 0.6, 0.06);
		level.playSound(null, guard.getX(), guard.getY(), guard.getZ(), guard.isGhost() ? SoundEvents.SOUL_ESCAPE : SoundEvents.ELDER_GUARDIAN_AMBIENT,
				SoundSource.HOSTILE, 1.5f, 0.6f);
		guard.discard();
	}

	/** A server stopping: no guard hunts any more (any found again goes). */
	static void clear() {
		HUNTS.clear();
	}
}
