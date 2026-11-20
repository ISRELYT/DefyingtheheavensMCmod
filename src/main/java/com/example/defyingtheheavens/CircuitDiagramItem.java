package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Circuit Diagram (Puppetry; the design's "save, trade or loot a layout"): a board's drawing copied onto paper with the Inscription Brush
 * (the board's screen's Copy: PuppetBoards#COPY), for a frame of one make and grade. Held while painting a board of that make and grade, it
 * lies over the board as a ghost (the screen's Path button, past the rank's own Circuit Path), the strokes above one's Puppetry greyed: one
 * paints it over by hand. An ancient one is joined from the four Circuit Fragments Bronze Puppets carry (CircuitFragmentItem): the best
 * layout there is for its frame, a Saint's.
 */
public class CircuitDiagramItem extends Item {
	private static final String BLUEPRINT = "DiagramBlueprint", GRADE = "DiagramGrade", STROKES = "DiagramStrokes", AUTHOR = "DiagramAuthor",
			ANCIENT = "DiagramAncient", WEAR = "DiagramWear";

	public CircuitDiagramItem(Properties properties) {
		super(properties);
	}

	/** A diagram of {@code strokes} for a {@code blueprint} frame of {@code grade}, drawn by {@code author} (or ancient). */
	public static ItemStack of(PuppetBlueprint blueprint, ArtifactGrade grade, byte[] strokes, String author, boolean ancient) {
		ItemStack stack = new ItemStack(ModItems.CIRCUIT_DIAGRAM);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putString(BLUEPRINT, blueprint.getId());
		tag.putByte(GRADE, (byte) grade.ordinal());
		tag.putByteArray(STROKES, strokes);
		if (!author.isEmpty()) tag.putString(AUTHOR, author);
		if (ancient) tag.putBoolean(ANCIENT, true);
		return stack;
	}

	/** The ancient diagram for a {@code blueprint} frame of {@code grade}: a Saint's Circuit Path on a flawless board. */
	public static ItemStack ancient(PuppetBlueprint blueprint, ArtifactGrade grade) {
		CircuitBoard board = new CircuitBoard(blueprint, grade.ordinal(), 0, 0);
		board.lay(board.circuitPath(CraftMastery.Rank.SAINT.ordinal()));
		return of(blueprint, grade, Puppetry.strokes(board), "", true);
	}

	public static @Nullable PuppetBlueprint blueprint(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? null : PuppetBlueprint.byId(tag.getString(BLUEPRINT));
	}

	public static ArtifactGrade grade(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return ArtifactGrade.byIndex(tag == null ? 0 : tag.getByte(GRADE));
	}

	public static byte[] strokes(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? new byte[0] : tag.getByteArray(STROKES);
	}

	public static boolean isAncient(ItemStack stack) {
		return stack.getTag() != null && stack.getTag().getBoolean(ANCIENT);
	}

	/** Whether {@code stack} is a diagram for a {@code blueprint} frame of {@code grade}. */
	public static boolean fits(ItemStack stack, PuppetBlueprint blueprint, ArtifactGrade grade) {
		return stack.getItem() instanceof CircuitDiagramItem && blueprint(stack) == blueprint && grade(stack) == grade;
	}

	/** The Puppetry rank (CraftMastery.Rank's order) its whole drawing wants: a Puppet Factory builds it only for a puppeteer of it (phase 3). */
	public static int rankWanted(ItemStack stack) {
		return measure(stack)[1];
	}

	/**
	 * Its design's wear in its keeper's puppets' fights (puppet phase 3: written on it by the Overseer Core it lies in), or null if none is
	 * written.
	 */
	public static Puppetry.@Nullable Wear wear(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null || !tag.contains(WEAR, net.minecraft.nbt.Tag.TAG_COMPOUND) ? null : Puppetry.Wear.load(tag.getCompound(WEAR));
	}

	/** Its wear written on it ({@code wear} null: wiped); whether it changed. */
	public static boolean setWear(ItemStack stack, Puppetry.@Nullable Wear wear) {
		CompoundTag tag = stack.getOrCreateTag();
		CompoundTag next = wear == null ? null : wear.tag();
		CompoundTag now = tag.contains(WEAR, net.minecraft.nbt.Tag.TAG_COMPOUND) ? tag.getCompound(WEAR) : null;
		if (next == null ? now == null : next.equals(now)) return false;
		if (next == null) tag.remove(WEAR);
		else tag.put(WEAR, next);
		return true;
	}

	/** Its design (Puppetry#design): every puppet built to it bears the same. */
	public static long design(ItemStack stack) {
		PuppetBlueprint blueprint = blueprint(stack);
		return blueprint == null ? 0 : Puppetry.design(blueprint, grade(stack), strokes(stack));
	}

	/** How many cells it draws, and the Puppetry rank its whole drawing wants. */
	private static int[] measure(ItemStack stack) {
		CircuitBoard.Stroke[] all = CircuitBoard.Stroke.values();
		int cells = 0, rank = 0;
		for (byte b : strokes(stack)) {
			if (b <= 0 || b >= all.length) continue;
			cells++;
			rank = Math.max(rank, all[b].rank);
		}
		return new int[] {cells, rank};
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(isAncient(stack) ? ModLang.DIAGRAM_ANCIENT_NAME : ModLang.DIAGRAM_NAME)
				.withStyle(isAncient(stack) ? ChatFormatting.GOLD : ChatFormatting.WHITE);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return isAncient(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		PuppetBlueprint blueprint = blueprint(stack);
		if (blueprint == null) return;
		ArtifactGrade grade = grade(stack);
		int size = CircuitBoard.sizeFor(grade.ordinal());
		Tooltips.add(lines, Component.translatable(ModLang.DIAGRAM_FOR, ModItems.artifact(ArtifactKind.frameOf(blueprint), grade).getDescription(), size, size)
				.withStyle(ChatFormatting.GRAY));
		CompoundTag tag = stack.getTag();
		if (isAncient(stack)) Tooltips.add(lines, Component.translatable(ModLang.DIAGRAM_ANCIENT).withStyle(ChatFormatting.GOLD));
		else if (tag != null && tag.contains(AUTHOR)) Tooltips.add(lines, Component.translatable(ModLang.DIAGRAM_AUTHOR, tag.getString(AUTHOR)).withStyle(ChatFormatting.GRAY));
		int[] m = measure(stack);
		Tooltips.add(lines, Component.translatable(ModLang.DIAGRAM_CELLS, m[0], CraftMastery.Rank.values()[m[1]].getDisplayName()).withStyle(ChatFormatting.DARK_AQUA));
		// Its wear, as an Overseer Core wrote it (phase 3).
		Puppetry.Wear wear = wear(stack);
		if (wear != null) Tooltips.add(lines, Component.translatable(ModLang.DIAGRAM_WEAR, wear.cutsTotal, wear.overheats).withStyle(ChatFormatting.GOLD));
		Tooltips.add(lines, Component.translatable(ModLang.DIAGRAM_USE).withStyle(ChatFormatting.DARK_GRAY));
	}
}
