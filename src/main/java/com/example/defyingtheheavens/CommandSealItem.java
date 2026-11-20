package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Command Seal (Puppetry; the user's pick, 2026-11-17: "Seal + list screen"): a puppeteer's seal of jade, through which their will
 * reaches their puppets.
 * <ul>
 *   <li>Right-clicked at the air: the list of all one's puppets, each with its order, its health and where it is (PuppetCommands: the
 *   client's CommandSealScreen), where any of them in reach is given its orders.</li>
 *   <li>Right-clicked on one's own puppet: the same, that puppet chosen.</li>
 *   <li>Sneak-right-clicked on a chest (any container; without sneaking the chest opens, as vanilla has it): marked for a bee's work, the
 *   first the one it carries from, the next the one it carries to; a third begins again.</li>
 *   <li>Held on another's puppet ({@link #SEIZE_TICKS} and more by its Clarity): seized, if one's Puppetry stands above its puppeteer's
 *   (two ranks above for a Fine puppet, three for a Perfect one: PuppetCommands#maySeize), the seal is of a grade above the one the puppet
 *   is bound to, and a slot is free.</li>
 *   <li>Its grade (puppet phase 2c), Mortal to Immortal: sneak-right-clicked on a Puppet Workbench, raised one grade for
 *   Puppetry#SEAL_RAISE_STONES of the new grade's spirit stones, as high as its holder's Puppetry allows (Puppetry#highestSeal). A puppet
 *   is bound to the best seal its puppeteer carries as it is sealed, and to a better one it is ordered with.</li>
 * </ul>
 */
public class CommandSealItem extends Item {
	private static final String FROM = "MarkFrom", TO = "MarkTo", DIMENSION = "MarkDimension", GRADE = "SealGrade";
	/** The least a seizing takes; a clear puppet's Clarity draws it out (PuppetCommands#seizeTicks). */
	public static final int SEIZE_TICKS = 60;
	/** Whom each player is seizing (the puppet's id), while they hold the seal on it. */
	private static final Map<UUID, UUID> SEIZING = new HashMap<>();

	public CommandSealItem(Properties properties) {
		super(properties);
	}

	/** The chest marked to carry from, if one is (in {@code level}). */
	public static @Nullable BlockPos markFrom(ItemStack seal, Level level) {
		return mark(seal, level, FROM);
	}

	public static @Nullable BlockPos markTo(ItemStack seal, Level level) {
		return mark(seal, level, TO);
	}

	private static @Nullable BlockPos mark(ItemStack seal, Level level, String key) {
		CompoundTag tag = seal.getTag();
		if (tag == null || !tag.contains(key) || !level.dimension().location().toString().equals(tag.getString(DIMENSION))) return null;
		return BlockPos.of(tag.getLong(key));
	}

	// --- Its grade (phase 2c) ---

	/** {@code seal}'s grade (a new one Mortal). */
	public static ArtifactGrade grade(ItemStack seal) {
		CompoundTag tag = seal.getTag();
		return tag == null ? ArtifactGrade.MORTAL : ArtifactGrade.byIndex(tag.getByte(GRADE));
	}

	public static void setGrade(ItemStack seal, ArtifactGrade grade) {
		seal.getOrCreateTag().putByte(GRADE, (byte) grade.ordinal());
	}

	/** A Command Seal of {@code grade}. */
	public static ItemStack of(ArtifactGrade grade) {
		ItemStack seal = new ItemStack(ModItems.COMMAND_SEAL);
		if (grade != ArtifactGrade.MORTAL) setGrade(seal, grade);
		return seal;
	}

	/** The grade of the best Command Seal {@code player} carries (in hand or pack); Mortal with none. */
	public static ArtifactGrade best(Player player) {
		ArtifactGrade best = ArtifactGrade.MORTAL;
		net.minecraft.world.entity.player.Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.getItem() instanceof CommandSealItem && grade(stack).ordinal() > best.ordinal()) best = grade(stack);
		}
		return best;
	}

	/**
	 * Sneak-right-clicked on a Puppet Workbench: {@code seal} raised a grade, for {@link Puppetry#SEAL_RAISE_STONES} of that grade's spirit
	 * stones, if {@code player}'s Puppetry holds a seal that high. What to tell them.
	 */
	public static Component raise(ServerPlayer player, ItemStack seal) {
		ArtifactGrade now = grade(seal);
		if (now == ArtifactGrade.IMMORTAL) return Component.translatable(ModLang.SEAL_AT_TOP).withStyle(ChatFormatting.GRAY);
		ArtifactGrade next = ArtifactGrade.byIndex(now.ordinal() + 1);
		CraftMastery.Rank rank = Puppetry.rank(CultivationManager.own(player));
		if (next.ordinal() > Puppetry.highestSeal(rank).ordinal()) {
			return Component.translatable(ModLang.SEAL_RANK_HOLDS, rank.getDisplayName(), Puppetry.highestSeal(rank).getDisplayName()).withStyle(ChatFormatting.RED);
		}
		if (!player.getAbilities().instabuild && !PuppetBoards.take(player, next.stone(), Puppetry.SEAL_RAISE_STONES)) {
			return Component.translatable(ModLang.SEAL_RAISE_STONES, next.getDisplayName(), Puppetry.SEAL_RAISE_STONES, new ItemStack(next.stone()).getHoverName())
					.withStyle(ChatFormatting.RED);
		}
		setGrade(seal, next);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.1f);
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.9f);
		return Component.translatable(ModLang.SEAL_RAISED, next.getDisplayName()).withStyle(ChatFormatting.AQUA);
	}

	@Override
	public Component getName(ItemStack stack) {
		ArtifactGrade grade = grade(stack);
		return grade == ArtifactGrade.MORTAL ? super.getName(stack) : Component.translatable(ModLang.COMMAND_SEAL_GRADED, grade.getDisplayName());
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return grade(stack) == ArtifactGrade.IMMORTAL;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack seal = player.getItemInHand(hand);
		if (player instanceof ServerPlayer sp) PuppetCommands.open(sp, null);
		return InteractionResultHolder.sidedSuccess(seal, level.isClientSide);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		// Sneaking on a Puppet Workbench (unsneaking, the bench opens its board): the seal raised a grade (phase 2c).
		if (level.getBlockEntity(pos) instanceof PuppetWorkbenchBlockEntity) {
			if (context.getPlayer() instanceof ServerPlayer player) player.displayClientMessage(raise(player, context.getItemInHand()), true);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if (HopperBlockEntity.getContainerAt(level, pos) == null) return InteractionResult.PASS;
		if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
		ItemStack seal = context.getItemInHand();
		CompoundTag tag = seal.getOrCreateTag();
		boolean sameWorld = level.dimension().location().toString().equals(tag.getString(DIMENSION));
		boolean second = sameWorld && tag.contains(FROM) && !tag.contains(TO) && tag.getLong(FROM) != pos.asLong();
		if (!second) {
			tag.remove(TO);
			tag.putLong(FROM, pos.asLong());
			tag.putString(DIMENSION, level.dimension().location().toString());
		} else {
			tag.putLong(TO, pos.asLong());
		}
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, second ? 1.3f : 1.0f);
		player.displayClientMessage(Component.translatable(second ? ModLang.SEAL_MARKED_TO : ModLang.SEAL_MARKED_FROM, pos.getX(), pos.getY(), pos.getZ())
				.withStyle(ChatFormatting.AQUA), true);
		return InteractionResult.CONSUME;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack seal, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof PuppetEntity puppet)) return InteractionResult.PASS;
		if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
		if (puppet.isOwnedBy(player)) {
			// Bound to this seal, if it is of a better grade than the one it was bound to (phase 2c).
			if (puppet.bindSeal(grade(seal))) {
				sp.displayClientMessage(Component.translatable(ModLang.SEAL_BOUND, puppet.getDisplayName(), grade(seal).getDisplayName()).withStyle(ChatFormatting.AQUA), true);
			}
			PuppetCommands.open(sp, puppet.getUUID());
			return InteractionResult.CONSUME;
		}
		Component refusal = PuppetCommands.seizeRefusal(sp, puppet);
		if (refusal != null) {
			sp.displayClientMessage(refusal.copy().withStyle(ChatFormatting.RED), true);
			return InteractionResult.CONSUME;
		}
		SEIZING.put(player.getUUID(), puppet.getUUID());
		player.startUsingItem(hand);
		sp.displayClientMessage(Component.translatable(ModLang.SEIZE_BEGUN, puppet.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	/** Held on another's puppet: the seizing goes on while it stays near, and is done once long enough (PuppetCommands#seizeTicks). */
	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack seal, int left) {
		if (!(user instanceof ServerPlayer player)) return;
		UUID id = SEIZING.get(player.getUUID());
		if (id == null) {
			player.stopUsingItem();
			return;
		}
		if (!(player.serverLevel().getEntity(id) instanceof PuppetEntity puppet) || !puppet.isAlive() || puppet.distanceTo(player) > 8) {
			SEIZING.remove(player.getUUID());
			player.stopUsingItem();
			player.displayClientMessage(Component.translatable(ModLang.SEIZE_BROKEN).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		int held = getUseDuration(seal) - left;
		if (held % 10 == 0) level.playSound(null, puppet.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.6f, 1.6f);
		if (held >= PuppetCommands.seizeTicks(puppet)) {
			SEIZING.remove(player.getUUID());
			player.stopUsingItem();
			PuppetCommands.seize(player, puppet);
		}
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int left) {
		if (user instanceof ServerPlayer player && SEIZING.remove(player.getUUID()) != null) {
			player.displayClientMessage(Component.translatable(ModLang.SEIZE_BROKEN).withStyle(ChatFormatting.GRAY), true);
		}
	}

	public static void forget(UUID player) {
		SEIZING.remove(player);
	}

	/** The server stopped: no seizing carries over. */
	public static void clear() {
		SEIZING.clear();
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.COMMAND_SEAL_TOOLTIP).withStyle(ChatFormatting.GRAY));
		Tooltips.add(lines, Component.translatable(ModLang.COMMAND_SEAL_MARKS).withStyle(ChatFormatting.DARK_GRAY));
		ArtifactGrade grade = grade(stack);
		if (grade == ArtifactGrade.IMMORTAL) {
			Tooltips.add(lines, Component.translatable(ModLang.COMMAND_SEAL_GRADE_TOP, grade.getDisplayName()).withStyle(ChatFormatting.DARK_AQUA));
		} else {
			ArtifactGrade next = ArtifactGrade.byIndex(grade.ordinal() + 1);
			Tooltips.add(lines, Component.translatable(ModLang.COMMAND_SEAL_GRADE, grade.getDisplayName(), Puppetry.SEAL_RAISE_STONES,
					new ItemStack(next.stone()).getHoverName()).withStyle(ChatFormatting.DARK_AQUA));
		}
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains(FROM)) {
			BlockPos from = BlockPos.of(tag.getLong(FROM));
			Tooltips.add(lines, Component.translatable(ModLang.COMMAND_SEAL_FROM, from.getX(), from.getY(), from.getZ()).withStyle(ChatFormatting.DARK_AQUA));
		}
		if (tag != null && tag.contains(TO)) {
			BlockPos to = BlockPos.of(tag.getLong(TO));
			Tooltips.add(lines, Component.translatable(ModLang.COMMAND_SEAL_TO, to.getX(), to.getY(), to.getZ()).withStyle(ChatFormatting.DARK_AQUA));
		}
	}
}
