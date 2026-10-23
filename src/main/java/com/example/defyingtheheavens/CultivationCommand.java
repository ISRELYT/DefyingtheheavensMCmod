package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Op-only testing helpers: /cultivation set <realm 1-6> <stage 1-4>, /cultivation addcultivation <amount>, /cultivation setqi
 * <amount> (the qi pool, capped at its maximum), /cultivation lifespan [set <years>] (Lifespan), /cultivation obsession [set <one|none>]
 * (Obsession), /cultivation bloodline [set <race> [colouring] | choose] (Bloodlines). Setting a cultivator back to or below Qi Refining Mid (or mortal) takes back the
 * heavens' leave and Favour, so the Ritual to the Heavens is asked again. Set leaves every heaven method known understood exactly as
 * far as the stage set (HeavenMethods#alignTo), nothing left to settle.
 */
public final class CultivationCommand {
	/**
	 * /cultivation set: the realm and stage, the methods understood as far as it, the ritual undone at or below its gate, and a
	 * breakthrough pill taken for a realm already reached spent.
	 */
	public static void set(PlayerCultivation c, Realm realm, Stage stage) {
		c.setState(realm, stage, 0);
		if (c.getPreparedRealm() != null && c.getPreparedRealm().ordinal() <= realm.ordinal()) c.prepareBreakthrough(null, 0);
		HeavenMethods.alignTo(c.heaven(), PlayerCultivation.rank(realm, stage));
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
													HeavenMethods.afterLayer(p); // the methods' layers as the stage's
													ctx.getSource().sendSuccess(() -> Component.literal("Cultivation set.").append(" ")
															.append(r.getDisplayName()).append(" - ").append(s.getDisplayName()), false);
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
										})))
						// Every stat point given back to spend again (StatPoints).
						.then(Commands.literal("resetstats")
								.executes(ctx -> {
									ServerPlayer p = ctx.getSource().getPlayerOrException();
									CultivationManager.get(p).clearStatPoints();
									CultivationManager.refresh(p);
									ctx.getSource().sendSuccess(() -> Component.literal("Stat points given back: "
											+ StatPoints.unspent(CultivationManager.get(p)) + " to spend."), false);
									return 1;
								}))
						// Lifespan (Lifespan): how many years are used of how many; set the years used (the age, alive or dead), which
						// ends the life at once if they reach the span.
						.then(Commands.literal("lifespan")
								.executes(ctx -> {
									ServerPlayer p = ctx.getSource().getPlayerOrException();
									PlayerCultivation c = CultivationManager.own(p);
									ctx.getSource().sendSuccess(() -> Lifespan.endless(c)
											? Component.translatable(ModLang.CMD_LIFESPAN_ENDLESS, p.getDisplayName(), Math.round(Lifespan.used(c)))
											: Component.translatable(ModLang.CMD_LIFESPAN, p.getDisplayName(),
											Math.round(Lifespan.used(c)), Math.round(Lifespan.span(c)), Math.round(Lifespan.left(c))), false);
									return 1;
								})
								.then(Commands.literal("set")
										.then(Commands.argument("years", DoubleArgumentType.doubleArg(0))
												.executes(ctx -> {
													ServerPlayer p = ctx.getSource().getPlayerOrException();
													PlayerCultivation c = CultivationManager.own(p);
													c.setAge(DoubleArgumentType.getDouble(ctx, "years"));
													CultivationManager.markDirty(p.server);
													CultivationManager.sync(p);
													Lifespan.check(p);
													return 1;
												}))))
						.then(obsession())
						.then(bloodline())));
	}

	/**
	 * /cultivation bloodline [set <race> [colouring]] | choose (Bloodlines): whose blood one is of; make it a race (its first colouring
	 * unless one is given); or undo the choice, the race picker brought up again.
	 */
	private static LiteralArgumentBuilder<CommandSourceStack> bloodline() {
		LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
		for (Bloodline b : Bloodline.values()) {
			set.then(Commands.literal(b.getId()).executes(ctx -> setBloodline(ctx.getSource(), b, 0))
					.then(Commands.argument("colouring", IntegerArgumentType.integer(1, Math.max(1, b.variants())))
							.executes(ctx -> setBloodline(ctx.getSource(), b, IntegerArgumentType.getInteger(ctx, "colouring") - 1))));
		}
		return Commands.literal("bloodline").executes(ctx -> reportBloodline(ctx.getSource())).then(set)
				.then(Commands.literal("choose").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					Bloodlines.forget(p.server, p.getUUID());
					ctx.getSource().sendSuccess(() -> Component.translatable(ModLang.CMD_BLOODLINE_ASKED, p.getDisplayName()), false);
					return 1;
				}));
	}

	private static int setBloodline(CommandSourceStack source, Bloodline bloodline, int variant) throws CommandSyntaxException {
		ServerPlayer p = source.getPlayerOrException();
		Bloodlines.set(p.server, p.getUUID(), bloodline, variant);
		return reportBloodline(source);
	}

	private static int reportBloodline(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer p = source.getPlayerOrException();
		Bloodlines.Entry entry = Bloodlines.entry(p.server, p.getUUID());
		source.sendSuccess(() -> entry == null ? Component.translatable(ModLang.CMD_BLOODLINE_NONE, p.getDisplayName())
				: entry.bloodline().isBeast() ? Component.translatable(ModLang.CMD_BLOODLINE, p.getDisplayName(), entry.bloodline().getDisplayName(),
				entry.bloodline().variantName(entry.variant()))
				: Component.translatable(ModLang.CMD_BLOODLINE_HUMAN, p.getDisplayName()), false);
		return 1;
	}

	/** /cultivation obsession [set <longevity|fortune|merit|enlightenment|none>] (Obsession): which is named, or name one (none: asked again). */
	private static LiteralArgumentBuilder<CommandSourceStack> obsession() {
		LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
		for (FourAxis.Axis axis : FourAxis.Axis.values()) set.then(Commands.literal(axis.getId()).executes(ctx -> setObsession(ctx.getSource(), axis)));
		set.then(Commands.literal("none").executes(ctx -> setObsession(ctx.getSource(), null)));
		return Commands.literal("obsession").executes(ctx -> reportObsession(ctx.getSource())).then(set);
	}

	private static int setObsession(CommandSourceStack source, FourAxis.Axis obsession) throws CommandSyntaxException {
		ServerPlayer p = source.getPlayerOrException();
		CultivationManager.get(p).setObsession(obsession);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		return reportObsession(source);
	}

	private static int reportObsession(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer p = source.getPlayerOrException();
		FourAxis.Axis obsession = CultivationManager.get(p).obsession();
		source.sendSuccess(() -> obsession == null ? Component.translatable(ModLang.CMD_OBSESSION_NONE, p.getDisplayName())
				: Component.translatable(ModLang.CMD_OBSESSION, p.getDisplayName(), Obsession.displayName(obsession), Obsession.boon(obsession)), false);
		return 1;
	}

	private CultivationCommand() {}
}
