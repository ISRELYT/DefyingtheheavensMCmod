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
		// Each deed tempers a mortal's body below Peak (gain), and the Body Path while it is drawn in (BodyPath#deed): the same deeds.
		AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
			if (player instanceof ServerPlayer sp && target instanceof LivingEntity living && living.isAlive()
					&& player.getAttackStrengthScale(0.5f) > 0.9f) {
				gain(sp, CHARGED_HIT);
				BodyPath.deed(sp, BodyPath.CHARGED_HIT);
			}
			return InteractionResult.PASS;
		});
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, killer, killed) -> {
			if (!(killer instanceof ServerPlayer sp)) return;
			gain(sp, killed instanceof Enemy ? HOSTILE_KILL : OTHER_KILL);
			BodyPath.deed(sp, (killed instanceof Enemy ? BodyPath.HOSTILE_KILL : BodyPath.OTHER_KILL)
					* BodyPath.killFactor(slainRank(killed), BodyPath.rank(CultivationManager.get(sp))));
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!(player instanceof ServerPlayer sp)) return;
			if (isOre(state)) {
				gain(sp, ORE_MINED);
				BodyPath.deed(sp, BodyPath.ORE_MINED);
			} else if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) {
				gain(sp, STONE_MINED);
				BodyPath.deed(sp, BodyPath.STONE_MINED);
			}
		});
	}

	/** The rank the slain stood at, for what its death tempers (BodyPath#killFactor): a cultivator's, a demon beast's, a player's mightier path; -1 else. */
	private static int slainRank(net.minecraft.world.entity.Entity killed) {
		if (killed instanceof CultivatorNpc npc) return npc.getRank();
		if (killed instanceof DemonBeast beast) return beast.getRank();
		if (killed instanceof ServerPlayer player) {
			PlayerCultivation c = CultivationManager.get(player);
			return Math.max(TwoPaths.higherRank(c, false), BodyPath.rank(c));
		}
		return -1;
	}

	/** Each player's health as it was the tick before: what it has lost since is harm taken, which tempers the Body Path (BodyPath#deed). */
	private static final Map<UUID, Float> lastHealth = new HashMap<>();

	private static boolean isOre(BlockState state) {
		return state.is(ConventionalBlockTags.ORES) || state.getBlock() instanceof DropExperienceBlock
				|| state.getBlock() instanceof RedStoneOreBlock;
	}

	/** Every tick: sprinting on the ground tempers too; and harm taken, the Body Path (the whole health {@link BodyPath#WHOLE_HEALTH_TAKEN} s). */
	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSprinting() && player.onGround()) {
				gain(player, SPRINT_PER_SECOND / 20);
				BodyPath.deed(player, BodyPath.SPRINT_PER_SECOND / 20);
			}
			float health = player.getHealth();
			Float was = lastHealth.put(player.getUUID(), health);
			if (was != null && health < was && player.isAlive() && player.getMaxHealth() > 0) {
				BodyPath.deed(player, BodyPath.WHOLE_HEALTH_TAKEN * (was - health) / player.getMaxHealth());
			}
		}
	}

	/** Food eaten (see PlayerMixin): half a point of tempering per food point; and the Body Path's vigor refilled (BodyTechniques#ate). */
	public static void onEat(ServerPlayer player, ItemStack stack) {
		FoodProperties food = stack.getItem().getFoodProperties();
		if (food == null) return;
		gain(player, food.getNutrition() * PER_FOOD_POINT);
		BodyTechniques.ate(player, food.getNutrition());
	}

	/** Tempering from a herb eaten raw. */
	public static double herb(ItemStack stack) {
		double rare = stack.getRarity() == net.minecraft.world.item.Rarity.COMMON ? 1 : 2;
		return HERB * PillGrade.ageFactor(GinsengItem.age(stack)) * rare;
	}

	/** An Ancient Holy Body (Physique) tempers this many times as fast. */
	public static final double HOLY_TEMPERING = 2.0;

	/**
	 * Adds tempering to a mortal below Peak, or one with shattered meridians below the last body refining (BodyRefining); anyone
	 * else: nothing. Returns true if it did anything.
	 */
	public static boolean gain(ServerPlayer player, double amount) {
		PlayerCultivation c = CultivationManager.get(player);
		if (!c.canTemper() || amount <= 0) return false;
		if (c.hasPhysique(Physique.ANCIENT_HOLY_BODY)) amount *= HOLY_TEMPERING;
		boolean refining = c.getMortalStage().isLast();
		if (c.addTempering(amount)) {
			MortalStage stage = c.getMortalStage();
			player.displayClientMessage(refining || c.getBodyRefining() > 0
					? Component.translatable(ModLang.MSG_BODY_REFINED, BodyRefining.name(c.getBodyRefining())).withStyle(net.minecraft.ChatFormatting.GOLD)
					: stage.isLast() ? Component.translatable(ModLang.MSG_TEMPERED_PEAK)
					: Component.translatable(ModLang.MSG_TEMPERED, stage.getDisplayName()), true);
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 0.8f);
			CultivationManager.refresh(player); // the tempered body's stats
			lastSync.put(player.getUUID(), player.level().getGameTime());
			BodyPath.shatteredBegin(player); // Shattered Meridians at Peak: the Body Path theirs from here, as their body refining was
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

	public static void forget(UUID player) {
		lastSync.remove(player);
		lastHealth.remove(player);
		BodyPath.forget(player);
	}

	private Tempering() {}
}
