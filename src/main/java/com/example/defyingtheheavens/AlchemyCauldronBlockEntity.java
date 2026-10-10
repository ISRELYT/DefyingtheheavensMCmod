package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The Alchemy Cauldron's contents and brew. Ingredients are kept one item per entry (so each keeps its own age), at most
 * {@link AlchemyRecipes#MAX_INGREDIENTS}. When they make a recipe, the brew runs for the recipe's time while the cauldron is
 * heated (it simply pauses while the fire is out), then the pill pops out with a grade rolled from the ingredients' average
 * age, and the water is used up.
 * <p>
 * Right-click: water bucket fills it; an ingredient goes in if it still fits a recipe; an empty hand shows what's inside (or
 * the brew's progress); sneaking with an empty hand tips the ingredients back out; an empty bucket takes the water back while
 * nothing is in it.
 */
public class AlchemyCauldronBlockEntity extends BlockEntity {
	private final List<ItemStack> contents = new ArrayList<>();
	/** What's brewing (null: nothing), how long it has cooked and how long it needs, in ticks. */
	private Item brewing;
	private int brewTicks;
	private int brewTotal;

	public AlchemyCauldronBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ALCHEMY_CAULDRON, pos, state);
	}

	// --- Interaction (server) ---

	public InteractionResult interact(BlockState state, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		boolean filled = state.getValue(AlchemyCauldronBlock.FILLED);

		if (!filled) {
			if (held.is(Items.WATER_BUCKET)) {
				setState(state.setValue(AlchemyCauldronBlock.FILLED, true));
				player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.BUCKET)));
				level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
				level.gameEvent(null, GameEvent.FLUID_PLACE, worldPosition);
				return InteractionResult.SUCCESS;
			}
			tell(player, Component.translatable(ModLang.CAULDRON_NEEDS_WATER));
			return InteractionResult.CONSUME;
		}

		if (brewing != null) {
			tell(player, AlchemyCauldronBlock.isHeated(level, worldPosition)
					? Component.translatable(ModLang.CAULDRON_BREWING, brewing.getDescription(), brewTicks * 100 / Math.max(1, brewTotal))
					: Component.translatable(ModLang.CAULDRON_NEEDS_HEAT, brewing.getDescription()));
			return InteractionResult.CONSUME;
		}

		if (held.isEmpty()) {
			if (player.isShiftKeyDown() && !contents.isEmpty()) {
				dropContents();
				level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 0.8f, 1.2f);
				tell(player, Component.translatable(ModLang.CAULDRON_EMPTIED));
			} else {
				tell(player, contents.isEmpty() ? Component.translatable(ModLang.CAULDRON_EMPTY)
						: Component.translatable(ModLang.CAULDRON_CONTENTS, describeContents()));
			}
			return InteractionResult.CONSUME;
		}

		if (held.is(Items.BUCKET) && contents.isEmpty()) {
			setState(state.setValue(AlchemyCauldronBlock.FILLED, false));
			player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.WATER_BUCKET)));
			level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
			level.gameEvent(null, GameEvent.FLUID_PICKUP, worldPosition);
			return InteractionResult.SUCCESS;
		}

		if (contents.size() >= AlchemyRecipes.MAX_INGREDIENTS) {
			tell(player, Component.translatable(ModLang.CAULDRON_FULL));
			return InteractionResult.CONSUME;
		}
		if (!AlchemyRecipes.canAdd(contents, held)) {
			tell(player, Component.translatable(ModLang.CAULDRON_REJECTED, held.getHoverName()));
			return InteractionResult.CONSUME;
		}

		ItemStack added = held.copyWithCount(1);
		if (!player.getAbilities().instabuild) held.shrink(1);
		contents.add(added);
		level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5f, 1.4f);

		AlchemyRecipes.Recipe recipe = AlchemyRecipes.match(contents);
		if (recipe != null) {
			brewing = recipe.output();
			brewTicks = 0;
			brewTotal = recipe.brewTicks();
			setState(state.setValue(AlchemyCauldronBlock.BREWING, true));
			level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 0.8f);
			tell(player, AlchemyCauldronBlock.isHeated(level, worldPosition)
					? Component.translatable(ModLang.CAULDRON_BREWING, brewing.getDescription(), 0)
					: Component.translatable(ModLang.CAULDRON_NEEDS_HEAT, brewing.getDescription()));
		} else {
			tell(player, Component.translatable(ModLang.CAULDRON_ADDED, added.getHoverName(), describeContents()));
		}
		setChanged();
		return InteractionResult.SUCCESS;
	}

	private static void tell(Player player, Component message) {
		player.displayClientMessage(message, true);
	}

	/** "Ginseng (120 years), Bone Meal" */
	private Component describeContents() {
		MutableComponent text = Component.empty();
		for (int i = 0; i < contents.size(); i++) {
			if (i > 0) text.append(", ");
			ItemStack stack = contents.get(i);
			text.append(stack.getItem().getDescription());
			int age = AlchemyRecipes.ageOf(stack);
			if (age > 0) text.append(Component.translatable(ModLang.PILL_AGE_SHORT, age));
		}
		return text;
	}

	private void setState(BlockState state) {
		level.setBlock(worldPosition, state, 3);
		setChanged();
	}

	// --- Brewing (server tick) ---

	public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyCauldronBlockEntity cauldron) {
		if (cauldron.brewing == null) {
			if (state.getValue(AlchemyCauldronBlock.BREWING)) cauldron.setState(state.setValue(AlchemyCauldronBlock.BREWING, false));
			return;
		}
		if (!state.getValue(AlchemyCauldronBlock.FILLED)) { // the water went somehow: the brew is spoiled, the ingredients kept
			cauldron.brewing = null;
			cauldron.setState(state.setValue(AlchemyCauldronBlock.BREWING, false));
			return;
		}
		if (!AlchemyCauldronBlock.isHeated(level, pos)) return; // waits for the fire
		if (++cauldron.brewTicks % 20 == 0) cauldron.setChanged();
		if (cauldron.brewTicks >= cauldron.brewTotal) cauldron.finish((ServerLevel) level, state);
	}

	private void finish(ServerLevel level, BlockState state) {
		int age = AlchemyRecipes.averageAge(contents);
		PillGrade grade = PillGrade.roll(age, level.random);
		ItemStack pill = PillItem.create(brewing, grade, age);
		brewing = null;
		brewTicks = brewTotal = 0;
		contents.clear();
		setState(state.setValue(AlchemyCauldronBlock.FILLED, false).setValue(AlchemyCauldronBlock.BREWING, false));

		double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.0, z = worldPosition.getZ() + 0.5;
		ItemEntity item = new ItemEntity(level, x, y, z, pill, 0, 0.25, 0);
		item.setDefaultPickUpDelay();
		level.addFreshEntity(item);
		level.sendParticles(ParticleTypes.CLOUD, x, y - 0.1, z, 12, 0.25, 0.1, 0.25, 0.02);
		level.sendParticles(ParticleTypes.END_ROD, x, y, z, 20 + grade.ordinal() * 15, 0.3, 0.4, 0.3, 0.05 + grade.ordinal() * 0.03);
		level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.2f);
		if (grade.ordinal() >= PillGrade.SUPREME.ordinal()) {
			level.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.6f, 1.0f);
		}
		Component done = Component.translatable(ModLang.CAULDRON_DONE, pill.getHoverName());
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(16))) {
			player.displayClientMessage(done, true);
		}
	}

	/** Tips the ingredients back out (sneaking with an empty hand, or the cauldron broken). */
	public void dropContents() {
		if (level == null || contents.isEmpty()) return;
		for (ItemStack stack : contents) {
			Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack);
		}
		contents.clear();
		brewing = null;
		brewTicks = brewTotal = 0;
		setChanged();
	}

	// --- Saving ---

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		ListTag list = new ListTag();
		for (ItemStack stack : contents) list.add(stack.save(new CompoundTag()));
		tag.put("Contents", list);
		if (brewing != null) {
			tag.putString("Brewing", BuiltInRegistries.ITEM.getKey(brewing).toString());
			tag.putInt("BrewTicks", brewTicks);
			tag.putInt("BrewTotal", brewTotal);
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
		brewing = null;
		if (tag.contains("Brewing")) {
			ResourceLocation id = ResourceLocation.tryParse(tag.getString("Brewing"));
			Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
			if (item != Items.AIR) brewing = item;
			brewTicks = tag.getInt("BrewTicks");
			brewTotal = tag.getInt("BrewTotal");
		}
	}
}
