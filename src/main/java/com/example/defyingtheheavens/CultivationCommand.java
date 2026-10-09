package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Op-only testing helpers: /cultivation set <realm 1-6> <stage 1-4>, /cultivation addcultivation <amount>,
 * /cultivation setqi <amount> (the qi pool, capped at its maximum).
 */
public final class CultivationCommand {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("cultivation")
						.requires(src -> src.hasPermission(2))
						.then(Commands.literal("set")
								.then(Commands.argument("realm", IntegerArgumentType.integer(1, Realm.values().length))
										.then(Commands.argument("stage", IntegerArgumentType.integer(1, Stage.values().length))
												.executes(ctx -> {
													ServerPlayer p = ctx.getSource().getPlayerOrException();
													Realm r = Realm.byIndex(IntegerArgumentType.getInteger(ctx, "realm") - 1);
													Stage s = Stage.byIndex(IntegerArgumentType.getInteger(ctx, "stage") - 1);
													CultivationManager.get(p).setState(r, s, 0);
													CultivationManager.refresh(p);
													ctx.getSource().sendSuccess(() -> Component.literal("Cultivation set.").append(" ")
															.append(r.getDisplayName()).append(" - ").append(s.getDisplayName()), false);
													return 1;
												}))))
						.then(Commands.literal("addcultivation")
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> {
											ServerPlayer p = ctx.getSource().getPlayerOrException();
											CultivationManager.get(p).addCultivation(DoubleArgumentType.getDouble(ctx, "amount"));
											CultivationManager.refresh(p);
											return 1;
										})))
						.then(Commands.literal("setqi")
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> {
											ServerPlayer p = ctx.getSource().getPlayerOrException();
											PlayerCultivation c = CultivationManager.get(p);
											c.setQi(Math.min(c.maxQi(), DoubleArgumentType.getDouble(ctx, "amount")));
											CultivationManager.markDirty(p.server);
											CultivationManager.sync(p);
											return 1;
										})))));
	}

	private CultivationCommand() {}
}
