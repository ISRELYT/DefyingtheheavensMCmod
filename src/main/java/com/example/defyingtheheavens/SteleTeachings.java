package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a stele teaches (the user's asks of 2026-11-16: "Steles should instead now tell you what method they give you upon right clicking them
 * (also what path that method belongs to and the realms it cultivates and stuff) and provide you with a method to cultivate from one of the
 * method manuals"; "the method that steles have should be random"; "make it so you dont need to be drawing in from that path for steles to work.
 * change the meditation time from 15 seconds to 7 seconds"). A Dao Stele and a Method Stele alike: the art carved on it (as the land's odds chose
 * it: DaoSteleFeature, MethodSteleFeature) gives its realm; what it teaches is one method a manual holds (CultivationMethod#fromManual), of any
 * path, whose volumes span that realm, drawn by the stele's own place and the world's seed (so it never changes), its volumes from its first realm
 * up to that one. A touch tells it ({@link #tell}); meditating within {@link #LEARN_RADIUS} of it for {@link #LEARN_SECONDS} teaches it, whatever
 * the meditation draws in (MeditationManager#readSteles); within {@link #BONUS_RADIUS}, a path whose practised method it is gathers {@link #BONUS}
 * times as fast.
 */
public final class SteleTeachings {
	public static final double LEARN_RADIUS = 4, BONUS_RADIUS = 8, LEARN_SECONDS = 7, BONUS = 1.25;

	/** A stele's teaching: {@code method}'s volumes up to {@code realm}'s. */
	public record Teaching(CultivationMethod method, Realm realm) {
		/** The realm a reader's volumes reach once taught it (a Demon Path method's whole: its blood is its volume). */
		public Realm reach() {
			return method.path() == TwoPaths.Path.DEMON ? method.last() : method.volumeFor(realm);
		}
	}

	/**
	 * The realm {@code state}'s carving stands for (a Dao Stele's heaven art's, a Method Stele's ghost art's: a foundation's own realm, the fire's
	 * and the steps', which have none, Qi Refining's); null for any other block.
	 */
	public static Realm carved(BlockState state) {
		if (state.is(ModBlocks.DAO_STELE)) return state.getValue(DaoSteleBlock.METHOD).realm();
		if (state.is(ModBlocks.METHOD_STELE)) return carved(state.getValue(MethodSteleBlock.METHOD));
		return null;
	}

	/** The realm a Method Stele carved with {@code art} stands for: a foundation's own; the fire's and the steps' (which have none) Qi Refining's. */
	public static Realm carved(GhostMethod art) {
		return art.realm() == null ? Realm.QI_REFINING : art.realm();
	}

	/** The middle stone of the stele {@code pos} (of {@code state}) is a part of. */
	public static BlockPos middle(BlockState state, BlockPos pos) {
		MethodSteleBlock.Part part = state.getValue(MethodSteleBlock.PART);
		return part == MethodSteleBlock.Part.LOWER ? pos.above() : part == MethodSteleBlock.Part.UPPER ? pos.below() : pos;
	}

	/**
	 * What the stele of {@code state} whose middle stone is {@code middle} teaches in {@code level}: null for another block, or one carved too deep
	 * for the worlds below (True Immortal's: Realm#taughtInTheWorld). A Four Axis carving teaches Heavenly Being's volumes (no method has one of
	 * Four Axis).
	 */
	public static Teaching of(Level level, BlockPos middle, BlockState state) {
		return of(level, middle, carved(state));
	}

	/** {@link #of(Level, BlockPos, BlockState)} of a stele carved for {@code carved} (null: no stele), its block unread (a rumour of it: Rumours). */
	public static Teaching of(Level level, BlockPos middle, Realm carved) {
		if (carved == null || !carved.taughtInTheWorld()) return null;
		Realm realm = carved == Realm.FOUR_AXIS ? Realm.HEAVENLY_BEING : carved;
		List<CultivationMethod> methods = new ArrayList<>();
		for (CultivationMethod method : CultivationMethod.values()) if (method.fromManual() && method.spans(realm)) methods.add(method);
		if (methods.isEmpty()) return null;
		long seed = middle.asLong() * 0x9E3779B97F4A7C15L ^ (level instanceof ServerLevel server ? server.getSeed() : 0L);
		return new Teaching(methods.get(RandomSource.create(seed).nextInt(methods.size())), realm);
	}

	/** Whether {@code c} holds {@code teaching} already, as far as the stele would take it. */
	public static boolean holds(PlayerCultivation c, Teaching teaching) {
		return CultivationMethods.knows(c, teaching.method()) && c.methodReach(teaching.method()) >= teaching.reach().ordinal();
	}

	/** The teachings of the steles within {@code radius} of {@code p} (by their middle stones). */
	public static Set<Teaching> within(ServerPlayer p, double radius) {
		Set<Teaching> found = new HashSet<>();
		int r = (int) Math.ceil(radius);
		BlockPos at = p.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -2, -r), at.offset(r, 3, r))) {
			BlockState state = p.level().getBlockState(pos);
			if (!state.is(ModBlocks.DAO_STELE) && !state.is(ModBlocks.METHOD_STELE) || state.getValue(MethodSteleBlock.PART) != MethodSteleBlock.Part.MIDDLE) continue;
			if (pos.distToCenterSqr(p.getX(), p.getY() + 1, p.getZ()) > radius * radius) continue;
			Teaching teaching = of(p.level(), pos.immutable(), state);
			if (teaching != null) found.add(teaching);
		}
		return found;
	}

	/**
	 * A touch (a Dao Stele's or a Method Stele's use): the method carved, its path, the volumes it holds and what it is, and where {@code p}
	 * stands with it: known that far, barred (a mortal's meridians, a body born against it, a path not begun: CultivationMethods#refusal), or
	 * {@link #LEARN_SECONDS} of meditation beside it away.
	 */
	public static void tell(ServerPlayer p, BlockState state, BlockPos pos) {
		Realm carved = carved(state);
		if (carved == null) return;
		if (!carved.taughtInTheWorld()) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_STELE_SEALED, carved.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		Teaching teaching = of(p.level(), middle(state, pos), state);
		if (teaching == null) return;
		CultivationMethod method = teaching.method();
		PlayerCultivation c = CultivationManager.own(p);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_STELE_HOLDS, method.getDisplayName(), Component.translatable(ModLang.pathName(method.path())))
				.withStyle(ChatFormatting.GOLD));
		p.sendSystemMessage(MethodManualItem.volumes(method, teaching.realm()).copy().withStyle(ChatFormatting.GRAY));
		p.sendSystemMessage(method.getDescription().copy().withStyle(ChatFormatting.DARK_GRAY));
		Component refusal = CultivationMethods.refusal(c, method);
		p.sendSystemMessage(refusal != null ? refusal.copy().withStyle(ChatFormatting.RED)
				: holds(c, teaching) ? Component.translatable(ModLang.MSG_STELE_HELD).withStyle(ChatFormatting.GRAY)
				: Component.translatable(ModLang.MSG_STELE_MEDITATE, (int) LEARN_SECONDS).withStyle(ChatFormatting.DARK_AQUA));
	}

	private SteleTeachings() {}
}
