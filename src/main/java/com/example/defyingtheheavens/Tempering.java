package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A mortal tempers their body (see {@link MortalStage}) by fighting, mining, sprinting and eating. Every amount here is
 * tempering points; a stage takes {@link MortalStage#getTemperingToNext()}.
 */
public final class Tempering {
	public static final double CHARGED_HIT = 0.5;
	public static final double HOSTILE_KILL = 4;
	public static final double OTHER_KILL = 1;
	public static final double STONE_MINED = 0.2;
	public static final double ORE_MINED = 3;
	public static final double SPRINT_PER_SECOND = 0.1;
	public static final double PER_FOOD_POINT = 0.5;
	public static final double SPIRIT_DEW = 12;
	/** A herb eaten raw: this much, more for older herbs (up to twice at 10,000 years), twice again for rare ones. */
	public static final double HERB = 10;

	/** Small gains reach the client at most this often (ticks); a new stage syncs at once. */
	private static final int SYNC_INTERVAL = 20;
	private static final Map<UUID, Long> lastSync = new HashMap<>();

	public static void register() {
		AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
			if (player instanceof ServerPlayer sp && target instanceof LivingEntity living && living.isAlive()
					&& player.getAttackStrengthScale(0.5f) > 0.9f) {
				gain(sp, CHARGED_HIT);
			}
			return InteractionResult.PASS;
		});
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, killer, killed) -> {
			if (killer instanceof ServerPlayer sp) gain(sp, killed instanceof Enemy ? HOSTILE_KILL : OTHER_KILL);
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!(player instanceof ServerPlayer sp)) return;
			if (isOre(state)) gain(sp, ORE_MINED);
			else if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) gain(sp, STONE_MINED);
		});
	}

	private static boolean isOre(BlockState state) {
		return state.is(ConventionalBlockTags.ORES) || state.getBlock() instanceof DropExperienceBlock
				|| state.getBlock() instanceof RedStoneOreBlock;
	}

	/** Every tick: sprinting on the ground tempers too. */
	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSprinting() && player.onGround()) gain(player, SPRINT_PER_SECOND / 20);
		}
	}

	/** Food eaten (see PlayerMixin): half a point of tempering per food point. */
	public static void onEat(ServerPlayer player, ItemStack stack) {
		FoodProperties food = stack.getItem().getFoodProperties();
		if (food != null) gain(player, food.getNutrition() * PER_FOOD_POINT);
	}

	/** Tempering from a herb eaten raw. */
	public static double herb(ItemStack stack) {
		double rare = stack.getRarity() == net.minecraft.world.item.Rarity.COMMON ? 1 : 2;
		return HERB * PillGrade.ageFactor(GinsengItem.age(stack)) * rare;
	}

	/** Adds tempering to a mortal below Peak (anyone else: nothing). Returns true if it did anything. */
	public static boolean gain(ServerPlayer player, double amount) {
		PlayerCultivation c = CultivationManager.get(player);
		if (!c.isMortal() || c.getMortalStage().isLast() || amount <= 0) return false;
		if (c.addTempering(amount)) {
			MortalStage stage = c.getMortalStage();
			player.displayClientMessage(stage.isLast() ? Component.translatable(ModLang.MSG_TEMPERED_PEAK)
					: Component.translatable(ModLang.MSG_TEMPERED, stage.getDisplayName()), true);
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 0.8f);
			CultivationManager.refresh(player); // the tempered body's stats
			lastSync.put(player.getUUID(), player.level().getGameTime());
			return true;
		}
		CultivationManager.markDirty(player.server);
		long now = player.level().getGameTime();
		Long last = lastSync.get(player.getUUID());
		if (last == null || now - last >= SYNC_INTERVAL) {
			CultivationManager.sync(player);
			lastSync.put(player.getUUID(), now);
		}
		return true;
	}

	public static void forget(UUID player) { lastSync.remove(player); }

	private Tempering() {}
}
