package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A puppet's frame (Puppetry; the user's "begin phase 1", 2026-11-17), forged at the Artifact Forge as any artifact is (ArtifactRecipes):
 * a humanoid's or a bee's body of its grade's matter, its joints in the grade's metal. Its grade sets the realms its puppet may stand at
 * (Puppetry#lowest, #highest) and its circuit board's size (CircuitBoard#sizeFor); how well it was forged sets the board's flaws (3 in a
 * Normal frame, 1 in a Fine one, none in a Perfect one), placed by a seed it takes the first time it lies on a Puppet Workbench.
 * <p>
 * The circuits painted on it at the bench are kept on the frame itself ({@link #board}, {@link #keep}), so it can be taken off the bench
 * and carried half drawn; a puppet dismantled gives its frame back with its drawing. One to a stack: no two drawings are alike.
 */
public class PuppetFrameItem extends Item {
	private static final String SEED = "FrameSeed", STROKES = "FrameCircuit";

	private final ArtifactKind kind;
	private final ArtifactGrade grade;

	public PuppetFrameItem(ArtifactKind kind, ArtifactGrade grade, Properties properties) {
		super(properties);
		this.kind = kind;
		this.grade = grade;
	}

	public ArtifactKind kind() { return kind; }
	public ArtifactGrade grade() { return grade; }
	public PuppetBlueprint blueprint() { return kind.blueprint(); }

	/** {@code frame}'s flaws: by its forging's quality. */
	public static int flaws(ItemStack frame) {
		return CircuitBoard.flawsFor(Artifacts.quality(frame).ordinal());
	}

	/** Whether {@code frame} has taken its seed yet (the first time it lay on a bench). */
	public static boolean seeded(ItemStack frame) {
		return frame.getTag() != null && frame.getTag().contains(SEED);
	}

	public static long seed(ItemStack frame) {
		return frame.getTag() == null ? 0 : frame.getTag().getLong(SEED);
	}

	/** Gives {@code frame} its seed (where its flaws lie) if it has none yet. */
	public static void ensureSeed(ItemStack frame, RandomSource random) {
		if (!seeded(frame)) frame.getOrCreateTag().putLong(SEED, random.nextLong());
	}

	/** {@code frame}'s board as drawn so far (a body's with its meridians: PuppetBodies). */
	public static CircuitBoard board(ItemStack frame) {
		PuppetFrameItem item = (PuppetFrameItem) frame.getItem();
		CircuitBoard board = new CircuitBoard(item.blueprint(), item.grade.ordinal(), flaws(frame), seed(frame));
		board.setMeridians(PuppetBodies.meridians(frame));
		CompoundTag tag = frame.getTag();
		if (tag != null && tag.contains(STROKES)) Puppetry.lay(board, tag.getByteArray(STROKES));
		return board;
	}

	/** Keeps {@code board}'s drawing on {@code frame}. */
	public static void keep(ItemStack frame, CircuitBoard board) {
		byte[] strokes = Puppetry.strokes(board);
		boolean any = false;
		for (byte b : strokes) any |= b != 0;
		if (any) frame.getOrCreateTag().putByteArray(STROKES, strokes);
		else if (frame.getTag() != null) frame.getTag().remove(STROKES);
	}

	/** {@code frame}'s drawing as kept (null: nothing drawn yet). */
	public static byte @Nullable [] strokes(ItemStack frame) {
		CompoundTag tag = frame.getTag();
		return tag != null && tag.contains(STROKES) ? tag.getByteArray(STROKES) : null;
	}

	/** A fresh frame of {@code blueprint} at {@code grade}, forged {@code quality}, carrying {@code seed} and {@code strokes} (a puppet taken apart). */
	public static ItemStack of(PuppetBlueprint blueprint, ArtifactGrade grade, Artifacts.Quality quality, long seed, byte @Nullable [] strokes) {
		ItemStack frame = Artifacts.create(ArtifactKind.frameOf(blueprint), grade, quality, null);
		frame.getOrCreateTag().putLong(SEED, seed);
		if (strokes != null) frame.getOrCreateTag().putByteArray(STROKES, strokes);
		return frame;
	}

	@Override
	public Component getName(ItemStack stack) {
		// A body of flesh (phase 2b): the dead's.
		PuppetBodies.Dead dead = PuppetBodies.deadOf(stack);
		if (dead != null) return Component.translatable(ModLang.BODY_NAME, dead.name());
		return Artifacts.name(super.getName(stack), stack, grade);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return !PuppetBodies.isBody(stack) && (grade == ArtifactGrade.IMMORTAL || Artifacts.quality(stack) == Artifacts.Quality.PERFECT);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		PuppetBodies.Dead dead = PuppetBodies.deadOf(stack);
		if (dead != null) {
			// A body of flesh: who it was, its board of meridians, how far it has refined, and what heart it wants.
			int rank = Math.max(0, dead.rank());
			Component realm = PlayerCultivation.rankName(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
			Tooltips.add(lines, Component.translatable(ModLang.BODY_WHO, realm).withStyle(ChatFormatting.GRAY));
			int size = CircuitBoard.sizeFor(grade.ordinal());
			Tooltips.add(lines, Component.translatable(ModLang.BODY_BOARD, size, size).withStyle(ChatFormatting.GRAY));
			int refined = PuppetBodies.refined(stack, level == null ? 0 : level.getGameTime());
			Tooltips.add(lines, Component.translatable(ModLang.BODY_REFINED, 100 * refined / PuppetBodies.REFINE_TICKS).withStyle(ChatFormatting.DARK_AQUA));
			Tooltips.add(lines, Component.translatable(dead.demon() ? ModLang.BODY_OWN_CORE : ModLang.BODY_WANTS_CORE, CultivatorNpc.realmOf(rank).getDisplayName())
					.withStyle(ChatFormatting.GRAY));
			Tooltips.add(lines, Component.translatable(ModLang.FRAME_USE).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		Tooltips.add(lines, Component.translatable(ModLang.FRAME_GRADE_LINE, grade.getDisplayName()).withStyle(ChatFormatting.GRAY));
		Tooltips.add(lines, Component.translatable(blueprint() == PuppetBlueprint.BEE ? ModLang.FRAME_BEE_ROLE : ModLang.FRAME_HUMANOID_ROLE)
				.withStyle(ChatFormatting.BLUE));
		Tooltips.add(lines, Puppetry.bandLine(grade).copy().withStyle(ChatFormatting.GRAY));
		int size = CircuitBoard.sizeFor(grade.ordinal());
		Tooltips.add(lines, Component.translatable(ModLang.FRAME_BOARD, size, size, flaws(stack), Artifacts.quality(stack).getDisplayName())
				.withStyle(ChatFormatting.GRAY));
		byte[] strokes = strokes(stack);
		int drawn = 0;
		if (strokes != null) for (byte b : strokes) if (b != 0) drawn++;
		Tooltips.add(lines, (drawn > 0 ? Component.translatable(ModLang.FRAME_DRAWN, drawn) : Component.translatable(ModLang.FRAME_UNDRAWN))
				.withStyle(ChatFormatting.DARK_AQUA));
		// A spring poured into its puppet once (phase 3), kept through its taking apart.
		if (stack.getTag() != null && stack.getTag().getBoolean(PuppetEntity.SPRING_FED)) {
			Tooltips.add(lines, Component.translatable(ModLang.FRAME_SPRING).withStyle(ChatFormatting.AQUA));
		}
		Tooltips.add(lines, Component.translatable(ModLang.FRAME_USE).withStyle(ChatFormatting.DARK_GRAY));
	}
}
