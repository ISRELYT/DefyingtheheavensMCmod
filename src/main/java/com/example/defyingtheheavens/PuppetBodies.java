package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Puppet bodies of flesh (Puppetry, phase 2b: the design's "Bodies from corpses", built on the user's "continue with all phases",
 * 2026-11-17). A slain cultivator's body, fresh or rotting, is taken up with the Inscription Brush (sneaking: {@link #take}) as a body: a
 * humanoid frame of the grade whose realms hold the dead's (PuppetFrameItem, tagged {@link #BODY} with who they were), devilish to take
 * ({@link #TAKE_SHIFT}).
 * <ul>
 *   <li><b>Its board is the body's own meridians</b>: a well-balanced layout already drawn ({@link #MERIDIANS}), never wiped nor painted
 *   over (CircuitBoard#refusal); rot has broken some of them ({@link #FRESH_BREAKS} fresh, up to {@link #ROT_BREAKS} more as it rots), and a
 *   break is mended with a plain line. Its puppeteer adds what else they will, as on a frame.</li>
 *   <li><b>Refined</b>: it lies on a Puppet Workbench a day of the world's ({@link #REFINE_TICKS}) before it can be sealed.</li>
 *   <li><b>Its heart</b>: a demon core of at least the dead's realm; a demon's body comes with its own.</li>
 *   <li><b>The puppet</b> (PuppetEntity): at the dead's own rank, at full strength ({@link #MIGHT}, a frame's seven tenths), in the dead's
 *   likeness, its art the dead's element. Wrecked, it leaves its core and bones; taken apart, its body.</li>
 *   <li><b>Seen</b>: righteous cultivators who see a puppeteer with one take them for an enemy ({@link #walksWithBody}).</li>
 * </ul>
 * Only cultivators' bodies of the world's people: never a player's (their ghost may walk in it), a mortal's, bones, or the long dead's.
 */
public final class PuppetBodies {
	/** The body's tag on its frame (who the dead was), its meridians' (a byte a cell), its refining's ticks and the time it was laid on a bench. */
	public static final String BODY = "PuppetBody", MERIDIANS = "Meridians", REFINED = "Refined", LAID_AT = "LaidAt";
	/** A day of the world's on the bench before it can be sealed. */
	public static final int REFINE_TICKS = 24000;
	/** Taking a body draws its taker this far toward the devilish path. */
	public static final int TAKE_SHIFT = 10;
	/** Breaks in its meridians: so many fresh, and up to so many more as it rots to bones. */
	public static final int FRESH_BREAKS = 2, ROT_BREAKS = 10;
	/** A body puppet's might: a cultivator's whole, where a frame's is Puppetry#MIGHT. */
	public static final double MIGHT = 1.0;
	/** Righteous cultivators see a puppeteer with a body puppet this near them as an enemy. */
	public static final double SEEN = 16;

	/** Who the dead was: their name, their look (a cultivator's skin, a demon's race, its colouring and tails), their rank, element and race. */
	public record Dead(String name, int skin, String race, int raceVariant, int tails, int rank, @Nullable QiElement element) {
		public boolean demon() {
			return !race.isEmpty();
		}

		public CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Name", name);
			tag.putInt("Skin", skin);
			tag.putString("Race", race);
			tag.putInt("RaceVariant", raceVariant);
			tag.putInt("Tails", tails);
			tag.putInt("Rank", rank);
			if (element != null) tag.putString("Element", element.name());
			return tag;
		}

		public static Dead load(CompoundTag tag) {
			QiElement element = null;
			if (tag.contains("Element", Tag.TAG_STRING)) {
				try {
					element = QiElement.valueOf(tag.getString("Element"));
				} catch (IllegalArgumentException ignored) {
				}
			}
			return new Dead(tag.getString("Name"), tag.getInt("Skin"), tag.getString("Race"), tag.getInt("RaceVariant"), Math.max(1, tag.getInt("Tails")),
					tag.getInt("Rank"), element);
		}
	}

	// --- Taking a body ---

	/**
	 * {@code player} sneaking with the Inscription Brush on {@code corpse}: a fresh or rotting cultivator's body taken up as a body to refine
	 * (what it held spilling where it lay), its taker drawn toward the devilish path. What to say, or null if it can't be taken (said why).
	 */
	public static InteractionResult take(ServerPlayer player, CorpseBlockEntity corpse, BlockState state, BlockPos pos) {
		ServerLevel level = player.serverLevel();
		String refusal = corpse.profile() != null ? ModLang.BODY_NOT_PLAYERS : corpse.rank() < 0 ? ModLang.BODY_MORTAL
				: corpse.record() == null ? ModLang.BODY_TOO_OLD : state.getValue(CorpseBlock.STAGE) == CorpseBlock.Rot.BONES ? ModLang.BODY_BONES : null;
		if (refusal != null) {
			player.displayClientMessage(Component.translatable(refusal).withStyle(ChatFormatting.RED), true);
			return InteractionResult.SUCCESS;
		}
		long age = Math.max(0, level.getGameTime() - corpse.diedAt());
		Dead dead = deadOf(corpse);
		ItemStack body = bodyItem(dead, level.random.nextLong(), breaks(age), level.random);
		// What it held spills where it lay, as it does when it is broken (CorpseBlock#onRemove); the body is lifted whole.
		level.setBlock(pos, state.getFluidState().isEmpty() ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
		if (!player.getInventory().add(body)) player.drop(body, false);
		PlayerCultivation c = CultivationManager.own(player);
		c.setAlignment(c.getAlignment() - TAKE_SHIFT);
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
		PersonalDisciples.witnessed(player, -TAKE_SHIFT);
		level.playSound(null, pos, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8f, 0.6f);
		level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 12, 0.4, 0.1, 0.4, 0.01);
		player.displayClientMessage(Component.translatable(ModLang.BODY_TAKEN, dead.name()).withStyle(ChatFormatting.DARK_RED), true);
		return InteractionResult.SUCCESS;
	}

	/** Who {@code corpse} was: its name and look, rank, and element (a demon's its race's; a cultivator's its heaven method's, as it died). */
	static Dead deadOf(CorpseBlockEntity corpse) {
		Bloodline race = corpse.race();
		QiElement element = null;
		if (race != null) element = DemonPath.element(race);
		else if (corpse.record() != null) {
			HeavenMethod method = HeavenMethods.practised(CorpseWalk.recordOf(corpse).heaven());
			element = method == null ? null : method.element();
		}
		return new Dead(corpse.name(), Math.max(0, corpse.skin()), race == null ? "" : race.getId(), corpse.raceVariant(), corpse.tails(), corpse.rank(),
				element);
	}

	/** How many of its meridians rot has broken, {@code age} ticks dead: a couple fresh, more as it rots, up to {@link #ROT_BREAKS} more. */
	static int breaks(long age) {
		double rotted = Math.max(0, Math.min(1, (double) age / Corpses.BONES_AT));
		return FRESH_BREAKS + (int) Math.round(ROT_BREAKS * rotted);
	}

	/** The frame grade whose realms hold {@code rank}'s (Puppetry#lowest .. #highest). */
	public static ArtifactGrade gradeFor(int rank) {
		Realm realm = CultivatorNpc.realmOf(Math.max(0, rank));
		ArtifactGrade best = ArtifactGrade.MORTAL;
		for (ArtifactGrade grade : ArtifactGrade.values()) if (realm.ordinal() >= Puppetry.lowest(grade).ordinal()) best = grade;
		return best;
	}

	/**
	 * A body of {@code dead}: a humanoid frame of its grade, flawless, its board drawn as the body's meridians (a Master's Balanced path for its
	 * size, every line of it) with {@code breaks} of them broken at random.
	 */
	public static ItemStack bodyItem(Dead dead, long seed, int breaks, RandomSource random) {
		ArtifactGrade grade = gradeFor(dead.rank());
		ItemStack body = PuppetFrameItem.of(PuppetBlueprint.HUMANOID, grade, Artifacts.Quality.PERFECT, seed, null);
		CircuitBoard board = PuppetFrameItem.board(body);
		board.lay(board.circuitPath(CraftMastery.Rank.MASTER.ordinal(), CircuitBoard.Build.BALANCED, CircuitBoard.BUDGET[grade.ordinal()]));
		byte[] meridians = new byte[board.size * board.size];
		List<int[]> lines = new ArrayList<>();
		for (int y = 0; y < board.size; y++) {
			for (int x = 0; x < board.size; x++) {
				if (board.stroke(x, y) == CircuitBoard.Stroke.NONE) continue;
				meridians[board.at(x, y)] = 1;
				lines.add(new int[] {x, y});
			}
		}
		for (int k = 0; k < breaks && !lines.isEmpty(); k++) {
			int[] at = lines.remove(random.nextInt(lines.size()));
			board.set(at[0], at[1], CircuitBoard.Stroke.NONE);
		}
		PuppetFrameItem.keep(body, board);
		CompoundTag tag = body.getOrCreateTag();
		tag.put(BODY, dead.save());
		tag.putByteArray(MERIDIANS, meridians);
		return body;
	}

	// --- The body as a frame ---

	public static boolean isBody(ItemStack frame) {
		return frame.getTag() != null && frame.getTag().contains(BODY, Tag.TAG_COMPOUND);
	}

	/** Who the body on {@code frame} was, or null for a frame of matter. */
	public static @Nullable Dead deadOf(ItemStack frame) {
		return isBody(frame) ? Dead.load(frame.getTag().getCompound(BODY)) : null;
	}

	/** Its meridians as a board's mask (a cell its own line), or null for a frame of matter. */
	public static boolean @Nullable [] meridians(ItemStack frame) {
		return frame.getTag() == null || !frame.getTag().contains(MERIDIANS) ? null : mask(frame.getTag().getByteArray(MERIDIANS));
	}

	public static boolean[] mask(byte[] bytes) {
		boolean[] out = new boolean[bytes.length];
		for (int i = 0; i < bytes.length; i++) out[i] = bytes[i] != 0;
		return out;
	}

	public static byte[] bytes(boolean[] mask) {
		byte[] out = new byte[mask.length];
		for (int i = 0; i < mask.length; i++) out[i] = (byte) (mask[i] ? 1 : 0);
		return out;
	}

	/** Its refining's ticks so far: what it had, and on a bench the time since it was laid there ({@code now}: the world's time). */
	public static int refined(ItemStack frame, long now) {
		CompoundTag tag = frame.getTag();
		if (tag == null) return 0;
		long ticks = tag.getInt(REFINED) + (tag.contains(LAID_AT) ? Math.max(0, now - tag.getLong(LAID_AT)) : 0);
		return (int) Math.min(REFINE_TICKS, ticks);
	}

	/** Refined enough to be sealed. */
	public static boolean ready(ItemStack frame, long now) {
		return !isBody(frame) || refined(frame, now) >= REFINE_TICKS;
	}

	/** Laid on a bench at {@code now}: its refining runs on. */
	public static void laid(ItemStack frame, long now) {
		if (isBody(frame)) frame.getOrCreateTag().putLong(LAID_AT, now);
	}

	/** Taken off a bench at {@code now}: what it refined kept, its refining stopped. */
	public static void lifted(ItemStack frame, long now) {
		if (!isBody(frame)) return;
		CompoundTag tag = frame.getOrCreateTag();
		tag.putInt(REFINED, refined(frame, now));
		tag.remove(LAID_AT);
	}

	/** Whether {@code core} may be {@code body}'s heart: a demon core of at least the dead's realm. */
	public static boolean coreFits(Dead body, ItemStack core) {
		return DemonCoreItem.rank(core) >= PlayerCultivation.rank(CultivatorNpc.realmOf(Math.max(0, body.rank())), Stage.EARLY);
	}

	/** A demon's own core, which its body comes with (its rank and its race's element). */
	public static ItemStack ownCore(Dead body) {
		return DemonCoreItem.of(Math.max(0, body.rank()), body.element(), Component.literal(body.name()));
	}

	// --- Seen with one ---

	/** Per puppeteer, the last answer of {@link #walksWithBody} and the tick it was asked: asked every few ticks by every cultivator about. */
	private static final Map<UUID, long[]> SEEN_WITH = new HashMap<>();

	/** Whether {@code player} has a body puppet of theirs within {@link #SEEN} (a righteous cultivator's enemy: CultivatorNpc#isEnemy). */
	public static boolean walksWithBody(ServerPlayer player) {
		long now = player.serverLevel().getGameTime();
		long[] last = SEEN_WITH.get(player.getUUID());
		if (last != null && now - last[0] < 20) return last[1] != 0;
		boolean with = !player.serverLevel().getEntitiesOfClass(PuppetEntity.class, player.getBoundingBox().inflate(SEEN),
				p -> p.isAlive() && p.isBody() && p.isOwnedBy(player)).isEmpty();
		SEEN_WITH.put(player.getUUID(), new long[] {now, with ? 1 : 0});
		return with;
	}

	/** The server stopped: all forgotten. */
	public static void clear() {
		SEEN_WITH.clear();
	}

	/** {@code player} has left: forgotten. */
	public static void forget(UUID player) {
		SEEN_WITH.remove(player);
	}

	private PuppetBodies() {}
}
