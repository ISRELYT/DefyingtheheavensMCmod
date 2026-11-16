package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Op-only testing helpers for the Netherworld, acting on yourself: /netherworld enter (arrive alive at the Ghost Gate, skipping
 * the river), /netherworld leave (down the Soul River to the Nether), /netherworld gate (to the Ghost Gate), /netherworld ghost
 * (become a ghost where you stand), /netherworld revive (return to life, as at the Wheel),
 * /netherworld up (onto the Soul River from its start, going up), /netherworld corruption <percent> (set yin corruption).
 * And for the ghost path and its arts: /method learn <id> (known whether or not it could be taken up yet, and understood as far as
 * the ghost path, as every art is), /method progress <seconds> (on the ghost path's bar, GhostMethods#required), /method path
 * <realm> <stage> (the ghost path set there: GhostMethods#setPath; /cultivation set ghost and addcultivation ghost do the same,
 * CultivationCommand), /method forget <id>; for the heaven arts /heavenmethod learn <id> and forget <id> (understood as far as the
 * realm). Since 2026-11-16 nothing settles and no layer is set by hand: an art's layer is the cultivator's own stage's.
 */
public final class NetherworldCommand {
	public static void register() {
		registerHeaven();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("netherworld")
						.requires(src -> src.hasPermission(2))
						.then(Commands.literal("enter").executes(ctx -> toGate(ctx.getSource().getPlayerOrException())))
						.then(Commands.literal("gate").executes(ctx -> toGate(ctx.getSource().getPlayerOrException())))
						.then(Commands.literal("leave").executes(ctx -> {
							SoulRiver.beginDescent(ctx.getSource().getPlayerOrException(), false);
							return 1;
						}))
						.then(Commands.literal("ghost").executes(ctx -> {
							GhostSystem.makeGhost(ctx.getSource().getPlayerOrException());
							return 1;
						}))
						.then(Commands.literal("revive").executes(ctx -> {
							GhostSystem.revive(ctx.getSource().getPlayerOrException());
							return 1;
						}))
						.then(Commands.literal("up").executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							CultivationManager.get(p).netherworld().setRiverCooldownUntil(0);
							SoulRiver.beginAscent(p);
							return 1;
						}))
						.then(Commands.literal("corruption")
								.then(Commands.argument("percent", DoubleArgumentType.doubleArg(0, 100))
										.executes(ctx -> {
											ServerPlayer p = ctx.getSource().getPlayerOrException();
											double percent = DoubleArgumentType.getDouble(ctx, "percent");
											CultivationManager.get(p).netherworld().setYinCorruption(percent / 100);
											CultivationManager.markDirty(p.server);
											NetherworldManager.refresh(p);
											ctx.getSource().sendSuccess(() -> Component.literal("Yin corruption set to " + percent + "%."), false);
											return 1;
										})))));
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("method")
						.requires(src -> src.hasPermission(2))
						.then(Commands.literal("learn")
								.then(Commands.argument("id", StringArgumentType.word()).suggests(METHOD_IDS)
										.executes(ctx -> learn(ctx.getSource().getPlayerOrException(), method(ctx)))))
						.then(Commands.literal("progress")
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> progress(ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "amount")))))
						.then(Commands.literal("path")
								.then(Commands.argument("realm", IntegerArgumentType.integer(1, Realm.values().length))
										.then(Commands.argument("stage", IntegerArgumentType.integer(1, Stage.values().length))
												.executes(ctx -> path(ctx.getSource().getPlayerOrException(),
														PlayerCultivation.rank(Realm.byIndex(IntegerArgumentType.getInteger(ctx, "realm") - 1),
																Stage.byIndex(IntegerArgumentType.getInteger(ctx, "stage") - 1)))))))
						.then(Commands.literal("forget")
								.then(Commands.argument("id", StringArgumentType.word()).suggests(METHOD_IDS)
										.executes(ctx -> forget(ctx.getSource().getPlayerOrException(), method(ctx)))))));
	}

	/**
	 * /method path <realm> <stage> (and /cultivation set ghost, CultivationCommand): the ghost path set there (begun, if it wasn't), its
	 * stage's cultivation none, its methods understood as far.
	 */
	static int path(ServerPlayer p, int rank) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		int before = GhostMethods.ghostRank(s);
		GhostMethods.setPath(s, rank);
		GhostMethods.afterLayer(p, s);
		GhostMethods.afterRank(p, before);
		changed(p);
		p.sendSystemMessage(Component.literal("Ghost path set: ").append(GhostMethods.rankName(GhostMethods.ghostRank(s))));
		return 1;
	}

	private static final SuggestionProvider<CommandSourceStack> METHOD_IDS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(java.util.Arrays.stream(GhostMethod.values()).map(GhostMethod::getId), builder);
	private static final SuggestionProvider<CommandSourceStack> HEAVEN_IDS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(java.util.Arrays.stream(HeavenMethod.values()).map(HeavenMethod::getId), builder);

	/**
	 * /heavenmethod learn <id>, forget <id>: the heaven arts (HeavenMethods), as /method the ghost ones. (Its progress <amount> went with
	 * the settling, 2026-11-16: an art is understood as far as the realm at once.)
	 */
	static void registerHeaven() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("heavenmethod")
						.requires(src -> src.hasPermission(2))
						.then(Commands.literal("learn")
								.then(Commands.argument("id", StringArgumentType.word()).suggests(HEAVEN_IDS)
										.executes(ctx -> learnHeaven(ctx.getSource().getPlayerOrException(), heavenMethod(ctx)))))
						.then(Commands.literal("forget")
								.then(Commands.argument("id", StringArgumentType.word()).suggests(HEAVEN_IDS)
										.executes(ctx -> forgetHeaven(ctx.getSource().getPlayerOrException(), heavenMethod(ctx)))))));
	}

	private static HeavenMethod heavenMethod(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		HeavenMethod method = HeavenMethod.byId(StringArgumentType.getString(ctx, "id"));
		if (method == null) throw new SimpleCommandExceptionType(Component.literal("No such heaven method.")).create();
		return method;
	}

	/** /heavenmethod learn <id>: the art known (an op's word: whatever stands in the way), understood as far as the realm as every art is. */
	private static int learnHeaven(ServerPlayer p, HeavenMethod method) {
		boolean known = CultivationManager.get(p).heaven().knows(method);
		if (!known) HeavenMethods.give(p, method); // an op's word: whatever stands in the way
		HeavenMethods.afterLayer(p);
		p.sendSystemMessage(Component.literal(method.getId() + (known ? " was already known." : " learned: its arts open as the realm reaches them.")));
		return 1;
	}

	/** /heavenmethod forget <id>: as if it had never been learned. */
	private static int forgetHeaven(ServerPlayer p, HeavenMethod method) {
		HeavenState s = CultivationManager.get(p).heaven();
		s.setMethod(method, 0, 0);
		if (s.activeMethod() == null && s.knowsAny()) s.setActiveMethod(s.known().iterator().next());
		HeavenMethods.afterLayer(p);
		p.sendSystemMessage(Component.literal(method.getId() + " forgotten."));
		return 1;
	}

	private static GhostMethod method(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		GhostMethod method = GhostMethod.byId(StringArgumentType.getString(ctx, "id"));
		if (method == null) throw new SimpleCommandExceptionType(Component.literal("No such method.")).create();
		return method;
	}

	/**
	 * /method learn <id>: the art known (an op's word: whatever stands in the way), understood as far as the ghost path as every art is;
	 * left as it is if known.
	 */
	private static int learn(ServerPlayer p, GhostMethod method) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		boolean known = s.knows(method);
		if (!known) GhostMethods.give(p, method); // an op's word: whatever stands in the way
		changed(p);
		p.sendSystemMessage(Component.literal(method.getId() + (known ? " was already known." : " learned: its arts open as the ghost path reaches them.")));
		return 1;
	}

	/**
	 * /method progress <amount> (and /cultivation addcultivation ghost, CultivationCommand): that many seconds of baseline meditation
	 * on the ghost path's bar (to its next bottleneck); the arts are understood as far as the path then goes (nothing settles).
	 */
	static int progress(ServerPlayer p, double amount) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		if (GhostMethods.ghostRank(s) < 0) {
			p.sendSystemMessage(Component.literal("No ghost path begun: learn a foundation first (or /cultivation set ghost <realm> <stage>)."));
			return 0;
		}
		int before = GhostMethods.ghostRank(s);
		GhostMethods.addCultivation(s, amount); // the arts' layers go with the path (NetherworldState#layer: nothing settles since 2026-11-16)
		GhostMethods.afterRank(p, before);
		changed(p);
		int rank = GhostMethods.ghostRank(s);
		p.sendSystemMessage(Component.literal("Ghost path: ").append(GhostMethods.rankName(rank)).append(", " + Math.round(s.pathCultivation())
				+ " / " + Math.round(GhostMethods.required(rank)) + " s."));
		return 1;
	}

	/** /method forget <id>: as if it had never been learned. */
	private static int forget(ServerPlayer p, GhostMethod method) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		s.setMethod(method, 0, 0);
		if (s.activeMethod() == null && s.knowsAnyMethod()) s.setActiveMethod(s.knownMethods().iterator().next());
		changed(p);
		p.sendSystemMessage(Component.literal(method.getId() + " forgotten."));
		return 1;
	}

	private static void changed(ServerPlayer p) {
		CultivationManager.markDirty(p.server);
		Spells.sync(p);
		NetherworldManager.refresh(p);
		CultivationManager.sync(p);
	}

	/** To the Ghost Gate's arrival spot, alive, as the river would set them down. */
	private static int toGate(ServerPlayer p) {
		ServerLevel netherworld = p.server.getLevel(ModDimensions.NETHERWORLD);
		if (netherworld == null) return 0;
		BlockPos at = GhostGate.at(netherworld, GhostGate.ARRIVAL);
		p.teleportTo(netherworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
		SoulRiver.settle(p);
		return 1;
	}

	private NetherworldCommand() {}
}
