package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The small worlds' commands (SmallWorlds, phase 3d), anyone's, about their own world: /smallworld (its reach, how far it is laid, its
 * guests), /smallworld guests (the list), /smallworld guest add <player> (one online now), /smallworld guest remove <name> (sent out if
 * they're in it).
 */
public final class SmallWorldCommand {
	private static final SuggestionProvider<CommandSourceStack> GUESTS = (ctx, builder) -> {
		ServerPlayer p = ctx.getSource().getPlayer();
		SmallWorlds.World w = p == null ? null : SmallWorlds.Data.get(p.server).of(p.getUUID());
		return SharedSuggestionProvider.suggest(w == null ? java.util.List.of() : w.guests().values(), builder);
	};

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("smallworld")
						.executes(ctx -> info(ctx.getSource().getPlayerOrException()))
						.then(Commands.literal("guests").executes(ctx -> guests(ctx.getSource().getPlayerOrException())))
						.then(Commands.literal("guest")
								.then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> tell(ctx.getSource().getPlayerOrException(),
												SmallWorlds.addGuest(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"))))))
								.then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word()).suggests(GUESTS)
										.executes(ctx -> tell(ctx.getSource().getPlayerOrException(),
												SmallWorlds.removeGuest(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name")))))))));
	}

	private static int tell(ServerPlayer p, Component line) {
		p.sendSystemMessage(line.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
		return 1;
	}

	private static int info(ServerPlayer p) {
		SmallWorlds.World w = SmallWorlds.Data.get(p.server).of(p.getUUID());
		if (w == null) return tell(p, Component.translatable(ModLang.CMD_WORLD_NONE));
		return tell(p, Component.translatable(ModLang.CMD_WORLD_INFO, w.target(), Math.max(w.laid(), 0), w.guests().size()));
	}

	private static int guests(ServerPlayer p) {
		SmallWorlds.World w = SmallWorlds.Data.get(p.server).of(p.getUUID());
		if (w == null) return tell(p, Component.translatable(ModLang.CMD_WORLD_NONE));
		if (w.guests().isEmpty()) return tell(p, Component.translatable(ModLang.CMD_WORLD_NO_GUESTS));
		return tell(p, Component.translatable(ModLang.CMD_WORLD_GUESTS, String.join(", ", w.guests().values())));
	}

	private SmallWorldCommand() {}
}
