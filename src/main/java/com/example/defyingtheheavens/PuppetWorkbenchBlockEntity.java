package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What lies on a Puppet Workbench (PuppetWorkbenchBlock): a frame (PuppetFrameItem, its circuits drawn on it) and the demon core set in it as
 * its heart. A frame goes on with a right-click, then its core (one of the frame's realms or stronger: Puppetry#puppetRank); the Inscription
 * Brush, an empty hand or anything else opens its board to paint (PuppetBoards; the brush wanted in the pack to paint); a left-click takes
 * the frame and its core back ({@link #takeBack}). Sealing (on the board's screen) takes both and makes the puppet. Its client copy knows both, to draw them on the bench (and the board's lines to one
 * with Qi Sense on).
 */
public class PuppetWorkbenchBlockEntity extends BlockEntity {
	private ItemStack frame = ItemStack.EMPTY, core = ItemStack.EMPTY;

	public PuppetWorkbenchBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PUPPET_WORKBENCH, pos, state);
	}

	public ItemStack frame() { return frame; }
	public ItemStack core() { return core; }

	/** The frame's grade, or null with none on the bench. */
	public ArtifactGrade grade() {
		return frame.getItem() instanceof PuppetFrameItem item ? item.grade() : null;
	}

	public void setFrame(ItemStack frame) {
		this.frame = frame;
		changed();
	}

	public void setCore(ItemStack core) {
		this.core = core;
		changed();
	}

	/** The frame's drawing changed (PuppetBoards): kept, and shown. */
	public void boardChanged() {
		changed();
	}

	/** Sealed into a puppet: both gone into it. */
	public void clear() {
		frame = ItemStack.EMPTY;
		core = ItemStack.EMPTY;
		changed();
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/** A right-click on the bench: a frame laid on it, a core set in it, the board opened, or (sneaking, empty-handed) the core or frame taken back. */
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer sp) || level == null) return InteractionResult.CONSUME;
		ItemStack held = player.getItemInHand(hand);
		if (held.getItem() instanceof PuppetFrameItem) {
			if (!frame.isEmpty()) {
				sp.displayClientMessage(Component.translatable(ModLang.BENCH_HAS_FRAME).withStyle(ChatFormatting.GRAY), true);
				return InteractionResult.CONSUME;
			}
			ItemStack one = player.getAbilities().instabuild ? held.copyWithCount(1) : held.split(1);
			PuppetFrameItem.ensureSeed(one, level.random);
			// A body of flesh (phase 2b: PuppetBodies) refines while it lies here; a demon's comes with its own core.
			PuppetBodies.laid(one, level.getGameTime());
			PuppetBodies.Dead dead = PuppetBodies.deadOf(one);
			if (dead != null && dead.demon() && core.isEmpty()) core = PuppetBodies.ownCore(dead);
			setFrame(one);
			level.playSound(null, worldPosition, dead != null ? SoundEvents.SLIME_BLOCK_PLACE : SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.9f, 0.9f);
			sp.displayClientMessage(Component.translatable(dead != null ? ModLang.BENCH_BODY_LAID : ModLang.BENCH_FRAME_LAID).withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.CONSUME;
		}
		if (held.is(ModItems.DEMON_CORE)) {
			ArtifactGrade grade = grade();
			PuppetBodies.Dead dead = PuppetBodies.deadOf(frame);
			if (grade == null) {
				sp.displayClientMessage(Component.translatable(ModLang.BENCH_FRAME_FIRST).withStyle(ChatFormatting.GRAY), true);
			} else if (!core.isEmpty()) {
				sp.displayClientMessage(Component.translatable(ModLang.BENCH_HAS_CORE).withStyle(ChatFormatting.GRAY), true);
			} else if (dead != null) {
				// A body's heart: a core of at least the dead's realm; its puppet stands at the dead's own rank, whatever the core's.
				if (!PuppetBodies.coreFits(dead, held)) {
					sp.displayClientMessage(Component.translatable(ModLang.BODY_CORE_TOO_WEAK, CultivatorNpc.realmOf(Math.max(0, dead.rank())).getDisplayName())
							.withStyle(ChatFormatting.RED), true);
				} else {
					setCore(player.getAbilities().instabuild ? held.copyWithCount(1) : held.split(1));
					level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);
					int rank = Math.max(0, dead.rank());
					sp.displayClientMessage(Component.translatable(ModLang.BENCH_CORE_SET, PlayerCultivation.rankName(CultivatorNpc.realmOf(rank),
							CultivatorNpc.stageOf(rank))).withStyle(ChatFormatting.AQUA), true);
				}
			} else {
				int rank = Puppetry.puppetRank(grade, DemonCoreItem.rank(held));
				if (rank < 0) {
					sp.displayClientMessage(Component.translatable(ModLang.BENCH_CORE_TOO_WEAK, Puppetry.lowest(grade).getDisplayName())
							.withStyle(ChatFormatting.RED), true);
				} else {
					setCore(player.getAbilities().instabuild ? held.copyWithCount(1) : held.split(1));
					level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);
					Component stands = PlayerCultivation.rankName(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
					sp.displayClientMessage(Component.translatable(rank < DemonCoreItem.rank(core) ? ModLang.BENCH_CORE_SET_TOP : ModLang.BENCH_CORE_SET, stands)
							.withStyle(ChatFormatting.AQUA), true);
				}
			}
			return InteractionResult.CONSUME;
		}
		// Anything else in hand (an empty one, the brush, a Circuit Diagram to lay over the board) opens the board; the brush is wanted in
		// the pack only to paint. Left-click takes the frame and its core back (takeBack: Puppetry's AttackBlockCallback).
		if (frame.isEmpty()) {
			sp.displayClientMessage(Component.translatable(ModLang.BENCH_EMPTY).withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.CONSUME;
		}
		PuppetBoards.openBench(sp, this);
		return InteractionResult.CONSUME;
	}

	/**
	 * A left-click on the bench (Puppetry's AttackBlockCallback; the user's ask, 2026-11-17, "left click ... to remove the frame/core on it
	 * without having to break the bench"): the frame (its drawing kept on it) and its core back into {@code player}'s pack, together.
	 * Whether anything lay there (with nothing on it, the click mines the bench as any other).
	 */
	public boolean takeBack(Player player) {
		if (frame.isEmpty() && core.isEmpty()) return false;
		if (level == null || level.isClientSide) return true;
		// Told before they are handed over: the pack empties a stack it takes in.
		boolean hadFrame = !frame.isEmpty(), hadCore = !core.isEmpty();
		Component took = Component.translatable(hadFrame && hadCore ? ModLang.BENCH_TOOK_BOTH : hadFrame ? ModLang.BENCH_TOOK_FRAME : ModLang.BENCH_TOOK_CORE);
		PuppetBodies.lifted(frame, level.getGameTime()); // a body's refining kept, stopped off the bench
		if (hadFrame) give(player, frame);
		if (hadCore) give(player, core);
		clear();
		if (player instanceof ServerPlayer sp) sp.displayClientMessage(took.copy().withStyle(ChatFormatting.GRAY), true);
		level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
		return true;
	}

	private static void give(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack)) player.drop(stack, false);
	}

	/** Broken: what lay on it falls out, the frame with its drawing. */
	public void dropContents() {
		if (level == null) return;
		PuppetBodies.lifted(frame, level.getGameTime());
		if (!frame.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, frame);
		if (!core.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, core);
		frame = ItemStack.EMPTY;
		core = ItemStack.EMPTY;
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		frame = tag.contains("Frame") ? ItemStack.of(tag.getCompound("Frame")) : ItemStack.EMPTY;
		core = tag.contains("Core") ? ItemStack.of(tag.getCompound("Core")) : ItemStack.EMPTY;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (!frame.isEmpty()) tag.put("Frame", frame.save(new CompoundTag()));
		if (!core.isEmpty()) tag.put("Core", core.save(new CompoundTag()));
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = new CompoundTag();
		saveAdditional(tag);
		// The update packet drops an empty tag instead of sending it, so an emptied bench (sealed, or its frame taken) never reached the
		// client, which kept drawing the frame and core (the user's video, 2026-11-17; SpiritPedestalBlockEntity has the same marker).
		if (tag.isEmpty()) tag.putBoolean("Empty", true);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
