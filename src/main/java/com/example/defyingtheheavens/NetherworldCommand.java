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
 * And for the ghost methods: /method learn <id> [layer] (whether or not it could be taken up yet), /method progress <seconds> (on
 * the practised method; a layer's cost is its minutes x 60, GhostMethods#layerCost), /method forget <id>; for the heaven methods
 * the same under /heavenmethod (their progress in cultivation, HeavenMethods#layerCost).
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
										.executes(ctx -> learn(ctx.getSource().getPlayerOrException(), method(ctx), 0))
										.then(Commands.argument("layer", IntegerArgumentType.integer(1, MethodLayers.layers(Realm.FOUR_AXIS)))
												.executes(ctx -> learn(ctx.getSource().getPlayerOrException(), method(ctx),
														IntegerArgumentType.getInteger(ctx, "layer"))))))
						.then(Commands.literal("progress")
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> progress(ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "amount")))))
						.then(Commands.literal("forget")
								.then(Commands.argument("id", StringArgumentType.word()).suggests(METHOD_IDS)
										.executes(ctx -> forget(ctx.getSource().getPlayerOrException(), method(ctx)))))));
	}

	private static final SuggestionProvider<CommandSourceStack> METHOD_IDS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(java.util.Arrays.stream(GhostMethod.values()).map(GhostMethod::getId), builder);
	private static final SuggestionProvider<CommandSourceStack> HEAVEN_IDS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(java.util.Arrays.stream(HeavenMethod.values()).map(HeavenMethod::getId), builder);

	/** /heavenmethod learn <id> [layer], progress <amount>, forget <id>: the heaven methods (HeavenMethods), as /method the ghost ones. */
	static void registerHeaven() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("heavenmethod")
						.requires(src -> src.hasPermission(2))
						.then(Commands.literal("learn")
								.then(Commands.argument("id", StringArgumentType.word()).suggests(HEAVEN_IDS)
										.executes(ctx -> learnHeaven(ctx.getSource().getPlayerOrException(), heavenMethod(ctx), 0))
										.then(Commands.argument("layer", IntegerArgumentType.integer(1, MethodLayers.layers(Realm.FOUR_AXIS)))
												.executes(ctx -> learnHeaven(ctx.getSource().getPlayerOrException(), heavenMethod(ctx),
														IntegerArgumentType.getInteger(ctx, "layer"))))))
						.then(Commands.literal("progress")
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> progressHeaven(ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "amount")))))
						.then(Commands.literal("forget")
								.then(Commands.argument("id", StringArgumentType.word()).suggests(HEAVEN_IDS)
										.executes(ctx -> forgetHeaven(ctx.getSource().getPlayerOrException(), heavenMethod(ctx)))))));
	}

	private static HeavenMethod heavenMethod(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		HeavenMethod method = HeavenMethod.byId(StringArgumentType.getString(ctx, "id"));
		if (method == null) throw new SimpleCommandExceptionType(Component.literal("No such heaven method.")).create();
		return method;
	}

	/**
	 * /heavenmethod learn <id> [layer]: that layer understood (no further than its last, deeper than the realm too: its spells and
	 * passive still wait on the realm), nothing settling; without one, as a stele teaches it.
	 */
	private static int learnHeaven(ServerPlayer p, HeavenMethod method, int layer) {
		HeavenState s = CultivationManager.get(p).heaven();
		if (!s.knows(method)) HeavenMethods.give(p, method); // an op's word: whatever stands in the way
		if (layer > 0) s.setMethod(method, layer, 0);
		HeavenMethods.afterLayer(p);
		p.sendSystemMessage(Component.literal(method.getId() + " at layer " + s.layer(method) + " of " + method.layers() + "."));
		return 1;
	}

	/**
	 * /heavenmethod progress <amount>: that much settling (HeavenMethods#settleCost) on the practised heaven method, no further than
	 * the realm has come (HeavenMethods#understoodTo).
	 */
	private static int progressHeaven(ServerPlayer p, double amount) {
		PlayerCultivation c = CultivationManager.get(p);
		HeavenState s = c.heaven();
		HeavenMethod active = s.activeMethod();
		if (active == null) {
			p.sendSystemMessage(Component.literal("No heaven method is being practised."));
			return 0;
		}
		int upTo = HeavenMethods.understoodTo(c, active);
		HeavenMethods.settle(s, active, amount, upTo);
		HeavenMethods.afterLayer(p);
		p.sendSystemMessage(Component.literal(active.getId() + ": layer " + s.layer(active) + (s.layer(active) >= upTo ? ", understood as far as the realm."
				: ", " + Math.round(s.progress(active)) + " / " + Math.round(HeavenMethods.settleCost(s.layer(active) + 1)) + " settling.")));
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
	 * /method learn <id> [layer]: the method at that layer (no further than its last), its progress through it reset; with no
	 * layer, learned as a stele teaches it (GhostMethods#startLayer), or left as it is if known.
	 */
	private static int learn(ServerPlayer p, GhostMethod method, int layer) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		if (!s.knows(method)) GhostMethods.give(p, method); // an op's word: whatever stands in the way
		if (layer > 0) {
			int ghostPathBefore = GhostMethods.ghostRank(s);
			s.setMethod(method, layer, 0);
			GhostMethods.afterLayer(p, s, ghostPathBefore);
		}
		changed(p);
		p.sendSystemMessage(Component.literal(method.getId() + " at layer " + s.layer(method) + " of " + method.layers() + "."));
		return 1;
	}

	/** /method progress <amount>: that many seconds of baseline meditation on the active method, the ghost path's gate ignored. */
	private static int progress(ServerPlayer p, double amount) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		GhostMethod active = s.activeMethod();
		if (active == null) {
			p.sendSystemMessage(Component.literal("No method is being practised."));
			return 0;
		}
		int ghostPathBefore = GhostMethods.ghostRank(s);
		int gained = GhostMethods.cultivate(s, active, amount);
		if (gained > 0) GhostMethods.afterLayer(p, s, ghostPathBefore);
		changed(p);
		p.sendSystemMessage(Component.literal(active.getId() + ": layer " + s.layer(active) + ", " + Math.round(s.progress(active)) + " / "
				+ Math.round(GhostMethods.layerCost(active, s.layer(active))) + " s."));
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
