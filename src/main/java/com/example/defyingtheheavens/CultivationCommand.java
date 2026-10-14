package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Op-only testing helpers: /cultivation set <realm 1-6> <stage 1-4>, /cultivation addcultivation <amount>,
 * /cultivation setqi <amount> (the qi pool, capped at its maximum). Setting a cultivator back to or below Qi Refining Mid (or
 * mortal) takes back the heavens' leave and Favour, so the Ritual to the Heavens is asked again; setting one past their heaven
 * methods' reach says so (their cultivation waits there for the methods: HeavenMethods#reach).
 */
public final class CultivationCommand {
	/** /cultivation set: the realm and stage, the ritual undone at or below its gate. */
	public static void set(PlayerCultivation c, Realm realm, Stage stage) {
		c.setState(realm, stage, 0);
		if (PlayerCultivation.rank(realm, stage) <= PlayerCultivation.RITUAL_RANK) c.resetRitual();
	}

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
													PlayerCultivation c = CultivationManager.get(p);
													set(c, r, s);
													CultivationManager.refresh(p);
													ctx.getSource().sendSuccess(() -> Component.literal("Cultivation set.").append(" ")
															.append(r.getDisplayName()).append(" - ").append(s.getDisplayName()), false);
													int reach = HeavenMethods.reach(c);
													if (PlayerCultivation.rank(r, s) > reach) {
														ctx.getSource().sendSuccess(() -> Component.literal("Your heaven methods only carry you to ")
																.append(GhostMethods.rankName(reach)).append(": your cultivation waits there until they catch up"
																+ " (/heavenmethod learn <id> <layer>)."), false);
													}
													return 1;
												}))))
						.then(Commands.literal("mortal")
								.executes(ctx -> {
									// Back to the start of the path (for testing the Marrow Cleansing Elixir).
									ServerPlayer p = ctx.getSource().getPlayerOrException();
									PlayerCultivation c = CultivationManager.get(p);
									set(c, Realm.QI_REFINING, Stage.EARLY);
									c.setMortal(true);
									c.setMortalState(MortalStage.LOW, 0);
									c.setQi(0);
									c.prepareBreakthrough(null, 0);
									CultivationManager.refresh(p);
									RealmSuppressSystem.refreshNow();
									ctx.getSource().sendSuccess(() -> Component.literal("You are mortal again."), false);
									return 1;
								}))
						.then(Commands.literal("addtempering")
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> {
											ServerPlayer p = ctx.getSource().getPlayerOrException();
											Tempering.gain(p, DoubleArgumentType.getDouble(ctx, "amount"));
											CultivationManager.refresh(p);
											ctx.getSource().sendSuccess(() -> Component.literal("Tempering added."), false);
											return 1;
										})))
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
