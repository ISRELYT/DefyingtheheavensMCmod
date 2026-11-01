package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Territorial Dominion's territories (CultivationMethod#TERRITORIAL_DOMINION; the methods overhaul's phase 2, the user's pick of
 * 2026-10-31 in Return to the Wild's place): a demon marks one with a phial of its own race's blood, {@link #RADIUS} blocks round where
 * it stands (the Methods tab's Mark Territory: {@link #mark}), and meditating within it gathers by its qi (CultivationMethods#TERRITORY_PACE,
 * a third of the years), outside it nothing. While its holder meditates in it, a challenger comes every {@link #CHALLENGE} ticks: a wild
 * beast of the holder's realm at its edge, hunting it. Dying within it, or leaving it while a challenger lives, loses it; marking another
 * gives up the first. One a cultivator, saved on its record (DemonState#territory).
 */
public final class Territories {
	public static final int RADIUS = 24;
	public static final int CHALLENGE = 4 * 60 * 20;

	/** When each holder's next challenger comes (game time), and the challengers alive; not saved (a challenger left behind is just a beast). */
	private static final Map<UUID, Long> NEXT = new HashMap<>();
	private static final Map<UUID, Set<UUID>> CHALLENGERS = new HashMap<>();

	/** Whether {@code p} is within its own territory: its body (where it sits, its soul turned inward or not: InnerRealm#bodyPos). */
	public static boolean inside(ServerPlayer p) {
		DemonState d = CultivationManager.get(p).demon();
		if (!d.hasTerritory() || !InnerRealm.bodyDimension(p).location().toString().equals(d.territoryDimension())) return false;
		BlockPos centre = d.territory(), body = InnerRealm.bodyPos(p);
		double dx = body.getX() - centre.getX(), dz = body.getZ() - centre.getZ();
		return dx * dx + dz * dz <= RADIUS * RADIUS && Math.abs(body.getY() - centre.getY()) <= RADIUS * 2;
	}

	/**
	 * {@code p} marks its territory where it stands (C2S mark_territory): a demon knowing Territorial Dominion, a phial of its own race's blood
	 * poured out (taken from its inventory). Told why not, or that it is marked.
	 */
	public static void mark(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (!CultivationMethods.knows(c, CultivationMethod.TERRITORIAL_DOMINION)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_METHOD_UNKNOWN, CultivationMethod.TERRITORIAL_DOMINION.getDisplayName())
					.withStyle(ChatFormatting.GRAY), true);
			return;
		}
		Bloodline race = DemonPath.race(c);
		int phial = -1;
		for (int i = 0; i < p.getInventory().getContainerSize() && phial < 0; i++) {
			ItemStack stack = p.getInventory().getItem(i);
			if (stack.is(ModItems.DEMON_BLOOD) && DemonBloodItem.race(stack) == race) phial = i;
		}
		if (phial < 0 && !p.isCreative()) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_TERRITORY_NEEDS_BLOOD, race.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (phial >= 0 && !p.isCreative()) p.getInventory().getItem(phial).shrink(1);
		// Where its body is (the soul may be turned inward: InnerRealm#bodyPos).
		ServerLevel level = p.server.getLevel(InnerRealm.bodyDimension(p));
		if (level == null) level = p.serverLevel();
		BlockPos at = InnerRealm.bodyPos(p);
		c.demon().setTerritory(level.dimension().location().toString(), at);
		NEXT.put(p.getUUID(), level.getGameTime() + CHALLENGE);
		CHALLENGERS.remove(p.getUUID());
		DustParticleOptions blood = new DustParticleOptions(new Vector3f(0.6f, 0.05f, 0.08f), 1.6f);
		for (int i = 0; i < 72; i++) {
			double a = i * Math.PI * 2 / 72;
			BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					BlockPos.containing(at.getX() + 0.5 + Math.cos(a) * RADIUS, at.getY(), at.getZ() + 0.5 + Math.sin(a) * RADIUS));
			level.sendParticles(blood, ground.getX() + 0.5, ground.getY() + 0.3, ground.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0);
		}
		level.playSound(null, at, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.4f, 0.7f);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_TERRITORY_MARKED, RADIUS).withStyle(ChatFormatting.DARK_RED));
		CultivationManager.markDirty(p.server);
		Spells.sync(p);
	}

	/** Half a second of {@code p} meditating in its territory by Territorial Dominion (MeditationManager): a challenger, when its time comes. */
	static void meditating(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		long now = level.getGameTime();
		long next = NEXT.computeIfAbsent(p.getUUID(), id -> now + CHALLENGE);
		if (now < next) return;
		NEXT.put(p.getUUID(), now + CHALLENGE);
		challenge(p);
	}

	/**
	 * A challenger comes for {@code p}'s territory: a wild beast of its Demon Path's realm at the territory's edge, hunting it (its body,
	 * sitting there, if its soul is turned inward: a blow on it calls the soul back, InnerRealm#onBodyHurt).
	 */
	static void challenge(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		ServerLevel level = p.server.getLevel(InnerRealm.bodyDimension(p));
		if (level == null) return;
		int rank = Math.max(0, DemonPath.rank(c));
		List<BeastKind> fit = new ArrayList<>();
		for (BeastKind kind : BeastKind.values()) {
			if (kind.isElder() || kind.isPrimordial() || kind == BeastKind.CHAOS_FIEND) continue;
			if (kind.minRank() <= rank && kind.maxRank() >= rank) fit.add(kind);
		}
		if (fit.isEmpty()) return;
		BeastKind kind = fit.get(level.random.nextInt(fit.size()));
		BlockPos centre = c.demon().territory();
		double a = level.random.nextDouble() * Math.PI * 2;
		BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				BlockPos.containing(centre.getX() + Math.cos(a) * (RADIUS - 4), centre.getY(), centre.getZ() + Math.sin(a) * (RADIUS - 4)));
		DemonBeast beast = ModEntities.beast(kind).create(level);
		if (beast == null) return;
		beast.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
		beast.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		beast.setRank(rank);
		level.addFreshEntity(beast);
		net.minecraft.nbt.CompoundTag inward = c.getInnerReturn();
		beast.setTarget(inward != null && inward.hasUUID("Body") && level.getEntity(inward.getUUID("Body")) instanceof InnerBodyEntity body ? body : p);
		CHALLENGERS.computeIfAbsent(p.getUUID(), id -> new HashSet<>()).add(beast.getUUID());
		p.sendSystemMessage(Component.translatable(ModLang.MSG_TERRITORY_CHALLENGED, beast.getDisplayName()).withStyle(ChatFormatting.RED));
		level.playSound(null, at, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0f, 0.8f);
	}

	/** Each second of every holder (NetherworldManager#second): out of its territory while a challenger lives, the territory is lost. */
	static void second(ServerPlayer p) {
		Set<UUID> challengers = CHALLENGERS.get(p.getUUID());
		if (challengers == null || challengers.isEmpty()) return;
		ServerLevel level = p.server.getLevel(InnerRealm.bodyDimension(p)); // the challengers are where its body is
		if (level == null) return;
		challengers.removeIf(id -> !(level.getEntity(id) instanceof DemonBeast beast) || !beast.isAlive());
		if (challengers.isEmpty()) {
			CHALLENGERS.remove(p.getUUID());
			p.displayClientMessage(Component.translatable(ModLang.MSG_TERRITORY_HELD).withStyle(ChatFormatting.DARK_RED), true);
			return;
		}
		if (!inside(p)) lose(p);
	}

	/** {@code p} died: in its territory with a challenger alive (or any death within it), it is lost. */
	public static void died(ServerPlayer p) {
		if (CultivationManager.get(p).demon().hasTerritory() && inside(p)) lose(p);
	}

	/** {@code p}'s territory lost: told; the challengers let be. */
	static void lose(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (!c.demon().hasTerritory()) return;
		c.demon().setTerritory("", null);
		CHALLENGERS.remove(p.getUUID());
		NEXT.remove(p.getUUID());
		p.sendSystemMessage(Component.translatable(ModLang.MSG_TERRITORY_LOST).withStyle(ChatFormatting.RED));
		CultivationManager.markDirty(p.server);
		Spells.sync(p);
	}

	/** A player left. */
	public static void forget(UUID id) {
		NEXT.remove(id);
		CHALLENGERS.remove(id);
	}

	private Territories() {}
}
