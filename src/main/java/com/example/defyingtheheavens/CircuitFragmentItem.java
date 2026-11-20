package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Circuit Fragment (Puppetry; the design's "Bronze Puppet circuit fragments = quarters of an ancient diagram"): a torn quarter of the circuit
 * diagram an ancient puppeteer drew for a frame of one make and grade, carried by the Bronze Puppets of the secret realms (RealmGuardian: now
 * and then; the Cave Abode's master's puppet always). Right-clicked with all four quarters of one diagram in the pack, they are joined into
 * the Ancient Circuit Diagram (CircuitDiagramItem#ancient).
 */
public class CircuitFragmentItem extends Item {
	private static final String BLUEPRINT = "FragmentBlueprint", GRADE = "FragmentGrade", QUARTER = "FragmentQuarter";

	public CircuitFragmentItem(Properties properties) {
		super(properties);
	}

	public static ItemStack of(PuppetBlueprint blueprint, ArtifactGrade grade, int quarter) {
		ItemStack stack = new ItemStack(ModItems.CIRCUIT_FRAGMENT);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putString(BLUEPRINT, blueprint.getId());
		tag.putByte(GRADE, (byte) grade.ordinal());
		tag.putByte(QUARTER, (byte) Math.max(0, Math.min(3, quarter)));
		return stack;
	}

	/** A quarter of some ancient diagram: either make, Spirit to Heaven grade, now and then an Immortal one. */
	public static ItemStack random(RandomSource random) {
		PuppetBlueprint blueprint = PuppetBlueprint.values()[random.nextInt(PuppetBlueprint.values().length)];
		ArtifactGrade grade = random.nextFloat() < 0.1f ? ArtifactGrade.IMMORTAL : ArtifactGrade.byIndex(1 + random.nextInt(3));
		return of(blueprint, grade, random.nextInt(4));
	}

	public static @Nullable PuppetBlueprint blueprint(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? null : PuppetBlueprint.byId(tag.getString(BLUEPRINT));
	}

	public static ArtifactGrade grade(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return ArtifactGrade.byIndex(tag == null ? 0 : tag.getByte(GRADE));
	}

	public static int quarter(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getByte(QUARTER);
	}

	/** Right-clicked: the four quarters of its diagram joined, if all four are in the pack; else which are. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
		PuppetBlueprint blueprint = blueprint(held);
		ArtifactGrade grade = grade(held);
		if (blueprint == null) return InteractionResultHolder.fail(held);
		Inventory inv = player.getInventory();
		int[] slot = {-1, -1, -1, -1};
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.getItem() instanceof CircuitFragmentItem && blueprint(stack) == blueprint && grade(stack) == grade && slot[quarter(stack)] < 0) {
				slot[quarter(stack)] = i;
			}
		}
		StringBuilder have = new StringBuilder();
		for (int q = 0; q < 4; q++) if (slot[q] >= 0) have.append(have.isEmpty() ? "" : ", ").append(q + 1);
		for (int s : slot) {
			if (s < 0) {
				sp.displayClientMessage(Component.translatable(ModLang.FRAGMENT_MISSING, have.toString()).withStyle(ChatFormatting.GRAY), true);
				return InteractionResultHolder.consume(held);
			}
		}
		for (int s : slot) inv.getItem(s).shrink(1);
		ItemStack diagram = CircuitDiagramItem.ancient(blueprint, grade);
		if (!inv.add(diagram)) player.drop(diagram, false);
		level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
		sp.displayClientMessage(Component.translatable(ModLang.FRAGMENT_JOINED, diagram.getHoverName()).withStyle(ChatFormatting.GOLD), false);
		return InteractionResultHolder.consume(held);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		PuppetBlueprint blueprint = blueprint(stack);
		if (blueprint == null) return;
		Tooltips.add(lines, Component.translatable(ModLang.FRAGMENT_OF, quarter(stack) + 1,
				ModItems.artifact(ArtifactKind.frameOf(blueprint), grade(stack)).getDescription()).withStyle(ChatFormatting.GRAY));
		Tooltips.add(lines, Component.translatable(ModLang.FRAGMENT_USE).withStyle(ChatFormatting.DARK_GRAY));
	}
}
