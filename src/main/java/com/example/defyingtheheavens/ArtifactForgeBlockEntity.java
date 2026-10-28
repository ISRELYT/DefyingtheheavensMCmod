package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Artifact Forge's materials and working (the user's picks, 2026-10-28: "like the cauldron"; mastery "fail less, finer work").
 * Materials go in one at a time while they still fit a working (ArtifactRecipes); with all of one in, an empty hand on the forge begins
 * it, over heat (a fire, a lit campfire, lava or magma beneath: lava, soul fire and magma, the hotter, half again as fast). The one who
 * began it is its forger: they must stay by it ({@link #REACH}) and feed it qi, its grade's each second (ArtifactGrade#qiPerSecond),
 * or it stands still. Done, by the forger's rank in forging (CraftMastery, as it was when they began) it may fail (the materials burnt
 * away, a demon core given back, half the lesson still learned); else the artifact comes out Normal, Fine (a tenth better: a chance of
 * 10% for each rank past Apprentice) or Perfect (a quarter: 5% for each rank from Master on).
 * <p>
 * Right-click: a material goes in if it still fits a working; an empty hand begins the working (all its materials in) or shows what's in;
 * sneaking with an empty hand takes the materials back (stopping a working: its qi spent).
 */
public class ArtifactForgeBlockEntity extends BlockEntity {
	/** How near its forger must keep. */
	public static final double REACH = 5;
	/** The chances of a Fine and a Perfect piece, a rank (past Apprentice; from Master). */
	public static final double FINE_PER_RANK = 0.10, PERFECT_PER_RANK = 0.05;

	private final List<ItemStack> contents = new ArrayList<>();
	/** The working (null: none): what it forges, how far it has got and how far it goes, in ticks of an ordinary fire. */
	private ArtifactRecipes.Recipe working;
	private double progress;
	private int total;
	/** Who began it (they feed it qi and learn from it), and their rank in forging then. */
	private UUID forger;
	private CraftMastery.Rank forgerRank = CraftMastery.Rank.APPRENTICE;
	/** When the forger was last told it stands still (game time): once in a while, not every tick. */
	private long stallTold;

	public ArtifactForgeBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ARTIFACT_FORGE, pos, state);
	}

	public boolean isWorking() { return working != null; }
	public ArtifactRecipes.Recipe working() { return working; }
	public UUID forger() { return forger; }
	public int progressPercent() { return total <= 0 ? 0 : (int) Math.min(100, progress * 100 / total); }
	public List<ItemStack> contents() { return contents; }

	/** Hot enough beneath to forge over; a hotter heat, lava, soul fire or magma, half again as fast. */
	public static double heat(Level level, BlockPos pos) {
		if (!AlchemyCauldronBlock.isHeated(level, pos)) return 0;
		BlockState below = level.getBlockState(pos.below());
		return below.is(Blocks.LAVA) || below.is(Blocks.SOUL_FIRE) || below.is(Blocks.MAGMA_BLOCK) ? 1.5 : 1.0;
	}

	// --- Interaction (server) ---

	public InteractionResult interact(BlockState state, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.isEmpty()) {
			if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			if (player.isShiftKeyDown()) {
				if (contents.isEmpty()) return InteractionResult.CONSUME;
				dropContents();
				level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 0.8f, 0.9f);
				tell(player, Component.translatable(ModLang.FORGE_EMPTIED));
				return InteractionResult.SUCCESS;
			}
			if (working != null) {
				tell(player, Component.translatable(ModLang.FORGE_WORKING, working.output().getDescription(), progressPercent()));
				return InteractionResult.CONSUME;
			}
			ArtifactRecipes.Recipe recipe = ArtifactRecipes.match(contents);
			if (recipe == null) {
				tell(player, contents.isEmpty() ? Component.translatable(ModLang.FORGE_EMPTY) : Component.translatable(ModLang.FORGE_CONTENTS, describeContents()));
				return InteractionResult.CONSUME;
			}
			begin(state, player, recipe);
			return InteractionResult.SUCCESS;
		}
		if (working != null) {
			tell(player, Component.translatable(ModLang.FORGE_WORKING, working.output().getDescription(), progressPercent()));
			return InteractionResult.CONSUME;
		}
		if (contents.size() >= ArtifactRecipes.MAX_MATERIALS) {
			tell(player, Component.translatable(ModLang.FORGE_FULL));
			return InteractionResult.CONSUME;
		}
		if (!ArtifactRecipes.canAdd(contents, held)) {
			tell(player, Component.translatable(ModLang.FORGE_REJECTED, held.getHoverName()));
			return InteractionResult.CONSUME;
		}
		ItemStack added = held.copyWithCount(1);
		if (!player.getAbilities().instabuild) held.shrink(1);
		contents.add(added);
		level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6f, 1.3f);
		ArtifactRecipes.Recipe recipe = ArtifactRecipes.match(contents);
		tell(player, recipe != null ? Component.translatable(ModLang.FORGE_READY, recipe.output().getDescription())
				: Component.translatable(ModLang.FORGE_ADDED, added.getHoverName(), describeContents()));
		setChanged();
		return InteractionResult.SUCCESS;
	}

	/** {@code player} begins {@code recipe}: its forger, over heat. */
	private void begin(BlockState state, Player player, ArtifactRecipes.Recipe recipe) {
		if (heat(level, worldPosition) <= 0) {
			tell(player, Component.translatable(ModLang.FORGE_NEEDS_HEAT));
			return;
		}
		working = recipe;
		progress = 0;
		total = recipe.grade().seconds() * 20;
		forger = player.getUUID();
		PlayerCultivation own = player instanceof ServerPlayer sp ? CultivationManager.own(sp) : CultivationManager.forPlayer(player);
		forgerRank = CraftMastery.rank(own, CraftMastery.Craft.FORGING);
		setState(state.setValue(ArtifactForgeBlock.WORKING, true));
		level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.7f, 0.6f);
		tell(player, Component.translatable(ModLang.FORGE_BEGUN, recipe.output().getDescription(), Math.round(recipe.grade().qiPerSecond())));
	}

	private static void tell(Player player, Component message) {
		player.displayClientMessage(message, true);
	}

	/** "Iron Ingot, Iron Ingot, Stick" */
	private Component describeContents() {
		MutableComponent text = Component.empty();
		for (int i = 0; i < contents.size(); i++) {
			if (i > 0) text.append(", ");
			text.append(contents.get(i).getHoverName());
		}
		return text;
	}

	private void setState(BlockState state) {
		level.setBlock(worldPosition, state, 3);
		setChanged();
	}

	// --- The working (server tick) ---

	public static void serverTick(Level level, BlockPos pos, BlockState state, ArtifactForgeBlockEntity forge) {
		if (forge.working == null) {
			if (state.getValue(ArtifactForgeBlock.WORKING)) forge.setState(state.setValue(ArtifactForgeBlock.WORKING, false));
			return;
		}
		if (!(level instanceof ServerLevel server)) return;
		double heat = heat(level, pos);
		ServerPlayer forger = forge.forger == null ? null : server.getServer().getPlayerList().getPlayer(forge.forger);
		boolean near = forger != null && forger.isAlive() && forger.level() == level && forger.distanceToSqr(Vec3.atCenterOf(pos)) <= REACH * REACH;
		double draw = forge.working.grade().qiPerSecond() / 20.0;
		PlayerCultivation c = near ? CultivationManager.forPlayer(forger) : null;
		if (heat <= 0 || !near || c.isMortal() || !c.consumeQi(draw)) {
			// It stands still: told now and then why (no heat, no forger by it, no qi to spare).
			if (forger != null && server.getGameTime() - forge.stallTold > 60) {
				forge.stallTold = server.getGameTime();
				tell(forger, Component.translatable(heat <= 0 ? ModLang.FORGE_NEEDS_HEAT : ModLang.FORGE_STALLED));
			}
			return;
		}
		forge.progress += heat;
		if (server.getGameTime() % 20 == 0) forge.setChanged();
		if (server.getGameTime() % 10 == 0) {
			// Out of its chimney: a flame or two and the sparks of the work.
			server.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 2, 0.06, 0.02, 0.06, 0.01);
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.12, 0.08, 0.12, 0.05);
		}
		if (server.getGameTime() % 30 == 0) server.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.25f, 1.4f);
		if (forge.progress >= forge.total) forge.finish(server, state, forger);
	}

	/**
	 * The working done: by its forger's rank in forging it may fail (the materials burnt away, a demon core given back, half the lesson
	 * learned); else the artifact comes out, Normal, Fine or Perfect by that rank too, and the forger learns from it.
	 */
	private void finish(ServerLevel level, BlockState state, ServerPlayer forgerNow) {
		ArtifactRecipes.Recipe recipe = working;
		CraftMastery.Rank rank = forgerRank;
		QiElement element = ArtifactRecipes.element(contents);
		List<ItemStack> cores = new ArrayList<>();
		for (ItemStack stack : contents) if (stack.is(ModItems.DEMON_CORE)) cores.add(stack.copy());
		working = null;
		forger = null;
		progress = 0;
		total = 0;
		contents.clear();
		setState(state.setValue(ArtifactForgeBlock.WORKING, false));

		double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.1, z = worldPosition.getZ() + 0.5;
		boolean failed = level.random.nextDouble() < CraftMastery.failChance(rank, recipe.grade().difficulty());
		if (failed) {
			for (ItemStack core : cores) Containers.dropItemStack(level, x, y, z, core);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 24, 0.3, 0.3, 0.3, 0.02);
			level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.7f);
			Component lost = Component.translatable(ModLang.FORGE_FAILED, recipe.output().getDescription());
			for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(16))) player.displayClientMessage(lost, true);
			if (forgerNow != null) CraftMastery.practise(forgerNow, CraftMastery.Craft.FORGING, recipe.grade().practice() / 2);
			return;
		}
		Artifacts.Quality quality = rollQuality(rank, level.random.nextDouble());
		ItemStack artifact = Artifacts.create(recipe.kind(), recipe.grade(), quality, element);
		ItemEntity item = new ItemEntity(level, x, y, z, artifact, 0, 0.25, 0);
		item.setDefaultPickUpDelay();
		level.addFreshEntity(item);
		if (forgerNow != null) CraftMastery.practise(forgerNow, CraftMastery.Craft.FORGING, recipe.grade().practice());
		level.sendParticles(ParticleTypes.LAVA, x, y - 0.2, z, 6, 0.2, 0.1, 0.2, 0.02);
		level.sendParticles(ParticleTypes.END_ROD, x, y, z, 20 + recipe.grade().ordinal() * 12 + quality.ordinal() * 15, 0.3, 0.4, 0.3, 0.06);
		level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.7f, 1.2f);
		if (quality == Artifacts.Quality.PERFECT || recipe.grade() == ArtifactGrade.IMMORTAL) {
			level.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.6f, 1.0f);
		}
		Component done = Component.translatable(ModLang.FORGE_DONE, artifact.getHoverName());
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(16))) player.displayClientMessage(done, true);
	}

	/** How a piece comes out of {@code rank}'s hands for a {@code roll} of 0..1: Perfect first (from Master), then Fine, else Normal. */
	public static Artifacts.Quality rollQuality(CraftMastery.Rank rank, double roll) {
		double perfect = Math.max(0, rank.ordinal() - 1) * PERFECT_PER_RANK;
		double fine = rank.ordinal() * FINE_PER_RANK;
		if (roll < perfect) return Artifacts.Quality.PERFECT;
		if (roll < perfect + fine) return Artifacts.Quality.FINE;
		return Artifacts.Quality.NORMAL;
	}

	/** The materials back out (sneaking with an empty hand, or the forge broken): a working stops, its qi spent. */
	public void dropContents() {
		if (level == null || contents.isEmpty()) return;
		for (ItemStack stack : contents) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack);
		contents.clear();
		working = null;
		forger = null;
		progress = 0;
		total = 0;
		setChanged();
	}

	// --- Saving ---

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		ListTag list = new ListTag();
		for (ItemStack stack : contents) list.add(stack.save(new CompoundTag()));
		tag.put("Contents", list);
		if (working != null) {
			tag.putString("Kind", working.kind().name());
			tag.putString("Grade", working.grade().name());
			tag.putDouble("Progress", progress);
			tag.putInt("Total", total);
			if (forger != null) tag.putUUID("Forger", forger);
			tag.putInt("ForgerRank", forgerRank.ordinal());
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		contents.clear();
		ListTag list = tag.getList("Contents", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			ItemStack stack = ItemStack.of(list.getCompound(i));
			if (!stack.isEmpty()) contents.add(stack);
		}
		working = null;
		if (tag.contains("Kind")) {
			try {
				ArtifactKind kind = ArtifactKind.valueOf(tag.getString("Kind"));
				ArtifactGrade grade = ArtifactGrade.valueOf(tag.getString("Grade"));
				for (ArtifactRecipes.Recipe recipe : ArtifactRecipes.all()) {
					if (recipe.kind() == kind && recipe.grade() == grade) working = recipe;
				}
			} catch (IllegalArgumentException unknown) {
				working = null;
			}
			progress = tag.getDouble("Progress");
			total = tag.getInt("Total");
		}
		forger = working != null && tag.hasUUID("Forger") ? tag.getUUID("Forger") : null;
		forgerRank = CraftMastery.Rank.values()[Math.max(0, Math.min(CraftMastery.Rank.values().length - 1, tag.getInt("ForgerRank")))];
	}
}
