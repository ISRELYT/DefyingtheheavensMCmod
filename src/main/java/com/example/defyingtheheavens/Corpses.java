package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The bodies of the dead (the user's idea, 2026-10-26; built 2026-10-31 with the user's changes: "bodies rot to bones over a few minutes,
 * bones don't disappear unless mined, and loot stays in the body rather than spilling out like normal. If a body is mined the loot should
 * spill out"). A player, or a cultivator (CultivatorNpc; not the Netherworld's dead, nor a body set aside while a soul uses it), lies where
 * it fell, on its back in its own likeness (CorpseBlock, CorpseBlockEntity), holding everything its death would have dropped: what would
 * have scattered is caught as it falls ({@link #drops}, the corpse mixins) and laid in the body. It is fresh for {@link #FRESH_TICKS},
 * rots till {@link #BONES_AT}, then lies as bones until someone breaks it; broken, at any stage, it spills what it holds (bones some bones
 * besides). Experience drops as ever; with keepInventory a player's body holds nothing.
 * <p>
 * A fresh cultivator's body may be bled once with a glass bottle for Cultivator's Blood of its realm (CultivatorsBloodItem), which draws
 * the one who bleeds it toward the devilish path; and every body gives off yin, ghostly qi for the ghost path, round it ({@link #yinAt}:
 * GhostQi#densityAt). A player always leaves a body; cultivators' are kept few in one place ({@link #NPC_CROWD}). The gamerule
 * leaveCorpses turns them off.
 */
public final class Corpses {
	/** Fresh for two minutes, rotting three more, then bones (the user's "a few minutes"). */
	public static final int FRESH_TICKS = 2 * 60 * 20, BONES_AT = 5 * 60 * 20;
	/** No cultivator's body is left where this many already lie within {@link #CROWD_RADIUS}: a battle's dead don't pile up into lag. */
	public static final int NPC_CROWD = 6, CROWD_RADIUS = 16;
	/** How far a body that fell is looked down for ground to lie on, and up out of a wall. */
	private static final int FALL = 48, RISE = 3;
	/** What breaking a body's bones gives besides what it held. */
	public static final int BONES_DROPPED = 3;
	/** Bleeding a body draws the one who bleeds it this far toward the devilish path (Alignment). */
	public static final int BLEED_SHIFT = 5;
	/** The farthest any body's yin reaches ({@link #yinRadius} at the very top). */
	private static final int MAX_REACH = yinRadius(PlayerCultivation.LAST_RANK);
	/** The game tests leave no bodies unless one asks for them: the test world is kept between runs, and its dead would pile up in it. */
	public static boolean inTests;

	/** A death being caught: whose, in which level, and what it has dropped so far. */
	private record Catch(LivingEntity dying, ServerLevel level, List<ItemEntity> items) {}

	/** Where someone's body was laid, and when (for what their death drops after it: {@link #leave}). */
	private record Laid(ResourceKey<Level> dimension, BlockPos pos, long at) {}

	private static Catch catching;
	private static final Map<UUID, Laid> LAID = new HashMap<>();
	/** The bodies lying in loaded land: each level's, by chunk, with the dead's rank (CorpseBlockEntity tells, as it loads and goes). */
	private static final Map<ResourceKey<Level>, Long2ObjectMap<Map<BlockPos, Integer>>> LYING = new HashMap<>();

	// --- The death ---

	/**
	 * LivingEntity#die and ServerPlayer#die's drops ({@code drop}, the corpse mixins): for one who leaves a body, every item they drop is
	 * caught and laid in it; if no body can lie anywhere near (the void, a lava lake), they fall as they would have. Anyone else's, as ever.
	 */
	public static void drops(LivingEntity dying, Runnable drop) {
		if (catching != null || !(dying.level() instanceof ServerLevel level) || !leavesBody(dying, level)) {
			drop.run();
			return;
		}
		Catch caught = new Catch(dying, level, new ArrayList<>());
		catching = caught;
		try {
			drop.run();
		} finally {
			catching = null;
		}
		if (lay(dying, level, caught.items()) == null) {
			for (ItemEntity item : caught.items()) level.addFreshEntity(item);
		}
	}

	/** ServerLevel#addFreshEntity (the corpse mixins): an item a death being caught drops goes to its body. True if it was taken. */
	public static boolean take(ServerLevel level, Entity entity) {
		Catch caught = catching;
		if (caught == null || caught.level() != level || !(entity instanceof ItemEntity item)) return false;
		caught.items().add(item);
		return true;
	}

	/**
	 * Whether {@code dying} leaves a body: a player (not a spectator, nor one of the dead), or a living cultivator in its own body where
	 * fewer than {@link #NPC_CROWD} already lie; never with leaveCorpses off, nor in the game tests unless asked.
	 */
	static boolean leavesBody(LivingEntity dying, ServerLevel level) {
		if (!level.getGameRules().getBoolean(ModGameRules.LEAVE_CORPSES)) return false;
		if (level.getServer() instanceof GameTestServer && !inTests) return false;
		if (dying instanceof ServerPlayer player) return !player.isSpectator() && !GhostSystem.isGhost(player);
		// A reflection (MirrorReflection, never truly alive) shatters: no body.
		if (dying instanceof CultivatorNpc npc) return !npc.isGhost() && !npc.isVacant() && npc.livesItsLife() && near(level, npc.blockPosition(), CROWD_RADIUS) < NPC_CROWD;
		return false;
	}

	/**
	 * {@code dying}'s body laid down, holding {@code items}: where it fell, or on the ground under it, or just above a wall it fell into;
	 * fresh, its rot to come (CorpseBlock#tick). Null if it has nowhere to lie.
	 */
	static BlockPos lay(LivingEntity dying, ServerLevel level, List<ItemEntity> items) {
		BlockPos at = placeFor(level, dying.blockPosition());
		if (at == null) return null;
		boolean water = level.getFluidState(at).is(Fluids.WATER) && level.getFluidState(at).isSource();
		level.setBlock(at, ModBlocks.CORPSE.defaultBlockState().setValue(CorpseBlock.WATERLOGGED, water), Block.UPDATE_ALL);
		if (!(level.getBlockEntity(at) instanceof CorpseBlockEntity body)) {
			level.removeBlock(at, false);
			return null;
		}
		body.of(dying, level.getGameTime());
		for (ItemEntity item : items) body.hold(item.getItem());
		level.scheduleTick(at, ModBlocks.CORPSE, FRESH_TICKS);
		LAID.put(dying.getUUID(), new Laid(level.dimension(), at.immutable(), level.getGameTime()));
		return at;
	}

	/**
	 * Where a body that fell at {@code from} lies: up out of a wall it fell into (a little way), then down to the first ground under it
	 * (one slain in the air falls). Null in the void, in lava, or with no ground within {@link #FALL}.
	 */
	static BlockPos placeFor(ServerLevel level, BlockPos from) {
		if (from.getY() < level.getMinBuildHeight()) return null;
		BlockPos.MutableBlockPos pos = from.mutable();
		for (int i = 0; i < RISE && !fits(level, pos); i++) pos.move(Direction.UP);
		if (!fits(level, pos)) return null;
		for (int i = 0; i < FALL && fits(level, pos.below()); i++) pos.move(Direction.DOWN);
		return fits(level, pos.below()) ? null : pos.immutable();
	}

	/** Whether a body may lie at {@code pos}: what is there gives way (air, water, a plant, snow), and isn't lava or another body. */
	private static boolean fits(ServerLevel level, BlockPos pos) {
		if (!level.isInWorldBounds(pos)) return false;
		BlockState state = level.getBlockState(pos);
		return state.canBeReplaced() && !state.getFluidState().is(FluidTags.LAVA) && !state.is(ModBlocks.SEAL) && !state.is(ModBlocks.CORPSE);
	}

	/**
	 * {@code stack}, dropped by {@code dead}'s death after its body was laid (a demon's core: DemonCoreItem), into that body; anything
	 * else's, or with no body laid this tick, where it fell.
	 */
	public static void leave(LivingEntity dead, ItemStack stack) {
		Laid laid = LAID.get(dead.getUUID());
		if (laid != null && dead.level() instanceof ServerLevel level && laid.dimension() == level.dimension() && laid.at() == level.getGameTime()
				&& level.getBlockEntity(laid.pos()) instanceof CorpseBlockEntity body) {
			body.hold(stack);
			return;
		}
		dead.spawnAtLocation(stack);
	}

	// --- The body ---

	/**
	 * A body right-clicked by {@code player}: with a glass bottle, a fresh cultivator's body bled once for Cultivator's Blood of its realm
	 * (and the one who bleeds it drawn toward the devilish path, {@link #BLEED_SHIFT}); with anything else, whose body it was.
	 */
	static InteractionResult use(ServerPlayer player, InteractionHand hand, CorpseBlockEntity body, BlockState state) {
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(Items.GLASS_BOTTLE)) {
			player.displayClientMessage(body.rank() < 0 ? Component.translatable(ModLang.MSG_CORPSE_WHO_MORTAL, body.name())
					: Component.translatable(ModLang.MSG_CORPSE_WHO, body.name(), PlayerCultivation.rankName(
					Realm.byIndex(body.rank() / Stage.values().length), Stage.byIndex(body.rank() % Stage.values().length))), true);
			return InteractionResult.SUCCESS;
		}
		String refusal = body.rank() < 0 ? ModLang.MSG_CORPSE_MORTAL_BLOOD : body.bled() ? ModLang.MSG_CORPSE_BLED_ALREADY
				: state.getValue(CorpseBlock.STAGE) != CorpseBlock.Rot.FRESH ? ModLang.MSG_CORPSE_BLOOD_SPOILT : null;
		if (refusal != null) {
			player.displayClientMessage(Component.translatable(refusal), true);
			return InteractionResult.SUCCESS;
		}
		body.markBled();
		if (!player.getAbilities().instabuild) held.shrink(1);
		ItemStack blood = CultivatorsBloodItem.of(body.rank(), body.name());
		if (!player.getInventory().add(blood)) player.drop(blood, false);
		PlayerCultivation c = CultivationManager.own(player); // their own path, whatever body they are in
		c.setAlignment(c.getAlignment() - BLEED_SHIFT);
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 0.6f);
		player.displayClientMessage(Component.translatable(ModLang.MSG_CORPSE_BLED, body.name()).withStyle(ChatFormatting.DARK_RED), true);
		return InteractionResult.SUCCESS;
	}

	// --- Yin ---

	/** How much yin a body gives off (a Soul Sand Valley being 1, GhostQi): a mortal's a half, Qi Refining's 1, a half more each realm; bones half that. */
	public static double yin(int rank, boolean bones) {
		double yin = rank < 0 ? 0.5 : 1 + 0.5 * (rank / Stage.values().length);
		return bones ? yin / 2 : yin;
	}

	/** How far round it a body's yin reaches: 8 blocks, and a block more for each realm of the dead's. */
	public static int yinRadius(int rank) {
		return 8 + Math.max(0, rank) / Stage.values().length;
	}

	/** The yin the bodies round {@code pos} give off together (GhostQi#densityAt adds it, never past the Netherworld's). */
	public static double yinAt(ServerLevel level, BlockPos pos) {
		Long2ObjectMap<Map<BlockPos, Integer>> chunks = LYING.get(level.dimension());
		if (chunks == null || chunks.isEmpty()) return 0;
		int reach = (MAX_REACH >> 4) + 1, cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
		double yin = 0;
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				Map<BlockPos, Integer> here = chunks.get(ChunkPos.asLong(cx + dx, cz + dz));
				if (here == null) continue;
				for (Map.Entry<BlockPos, Integer> body : here.entrySet()) {
					int radius = yinRadius(body.getValue());
					if (body.getKey().distSqr(pos) > (double) radius * radius) continue;
					BlockState state = level.getBlockState(body.getKey());
					yin += yin(body.getValue(), state.is(ModBlocks.CORPSE) && state.getValue(CorpseBlock.STAGE) == CorpseBlock.Rot.BONES);
				}
			}
		}
		return yin;
	}

	/** How many bodies lie within {@code radius} of {@code pos}. */
	static int near(ServerLevel level, BlockPos pos, int radius) {
		Long2ObjectMap<Map<BlockPos, Integer>> chunks = LYING.get(level.dimension());
		if (chunks == null || chunks.isEmpty()) return 0;
		int reach = (radius >> 4) + 1, cx = pos.getX() >> 4, cz = pos.getZ() >> 4, count = 0;
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				Map<BlockPos, Integer> here = chunks.get(ChunkPos.asLong(cx + dx, cz + dz));
				if (here == null) continue;
				for (BlockPos body : here.keySet()) if (body.distSqr(pos) <= (double) radius * radius) count++;
			}
		}
		return count;
	}

	/** A body lies in loaded land (CorpseBlockEntity#setLevel). */
	static void lies(ServerLevel level, BlockPos pos, int rank) {
		LYING.computeIfAbsent(level.dimension(), key -> new Long2ObjectOpenHashMap<>())
				.computeIfAbsent(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), key -> new HashMap<>()).put(pos.immutable(), rank);
	}

	/** A body is gone from loaded land: broken, or its land unloaded (CorpseBlockEntity#setRemoved). */
	static void gone(ServerLevel level, BlockPos pos) {
		Long2ObjectMap<Map<BlockPos, Integer>> chunks = LYING.get(level.dimension());
		if (chunks == null) return;
		long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
		Map<BlockPos, Integer> here = chunks.get(key);
		if (here == null) return;
		here.remove(pos);
		if (here.isEmpty()) chunks.remove(key);
	}

	/** The server stopped: nothing carries over. */
	public static void clear() {
		LYING.clear();
		LAID.clear();
		catching = null;
	}

	private Corpses() {}
}
