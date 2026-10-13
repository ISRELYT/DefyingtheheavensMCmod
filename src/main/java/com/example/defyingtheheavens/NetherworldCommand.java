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
 * And for the ghost methods: /method learn <id> [layer], /method progress <seconds> (on the practised method; a layer's cost is
 * its minutes x 60, GhostMethods#MINUTES), /method forget <id>.
 */
public final class NetherworldCommand {
	public static void register() {
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
										.executes(ctx -> learn(ctx.getSource().getPlayerOrException(), method(ctx), 1))
										.then(Commands.argument("layer", IntegerArgumentType.integer(1, GhostMethods.LAYERS))
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

	private static GhostMethod method(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		GhostMethod method = GhostMethod.byId(StringArgumentType.getString(ctx, "id"));
		if (method == null) throw new SimpleCommandExceptionType(Component.literal("No such method.")).create();
		return method;
	}

	/** /method learn <id> [layer]: the method at that layer (the first by default), its progress through it reset. */
	private static int learn(ServerPlayer p, GhostMethod method, int layer) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		if (!s.knows(method)) GhostMethods.learn(p, method);
		s.setMethod(method, layer, 0);
		GhostMethods.afterLayer(p, s);
		changed(p);
		p.sendSystemMessage(Component.literal(method.getId() + " set to layer " + layer + "."));
		return 1;
	}

	/** /method progress <amount>: that many seconds of baseline meditation on the active method, the outside cap ignored. */
	private static int progress(ServerPlayer p, double amount) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		GhostMethod active = s.activeMethod();
		if (active == null) {
			p.sendSystemMessage(Component.literal("No method is being practised."));
			return 0;
		}
		int gained = GhostMethods.cultivate(s, active, amount, GhostQi.FULL_DENSITY);
		if (gained > 0) GhostMethods.afterLayer(p, s);
		changed(p);
		p.sendSystemMessage(Component.literal(active.getId() + ": layer " + s.layer(active) + ", " + Math.round(s.progress(active)) + " / "
				+ Math.round(GhostMethods.layerCost(s.layer(active))) + " s."));
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
