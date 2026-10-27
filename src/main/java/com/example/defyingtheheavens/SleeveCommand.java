package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Universe in a Sleeve's words (Sleeves), anyone's, sent by the chat's own buttons: /sleeve accept <bearer> and /sleeve refuse <bearer>
 * answer an offer of a place in their sleeve; /sleeve leave steps out of the sleeve one is in (only while its bearer is out in a world).
 */
public final class SleeveCommand {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("sleeve")
						.then(Commands.literal("accept").then(Commands.argument("bearer", StringArgumentType.word())
								.executes(ctx -> tell(ctx.getSource().getPlayerOrException(),
										Sleeves.accept(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "bearer"))))))
						.then(Commands.literal("refuse").then(Commands.argument("bearer", StringArgumentType.word())
								.executes(ctx -> tell(ctx.getSource().getPlayerOrException(),
										Sleeves.refuse(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "bearer"))))))
						.then(Commands.literal("leave").executes(ctx -> tell(ctx.getSource().getPlayerOrException(),
								Sleeves.leave(ctx.getSource().getPlayerOrException()))))));
	}

	/** {@code line} to {@code p} (nothing more to say if null). */
	private static int tell(ServerPlayer p, Component line) {
		if (line != null) p.sendSystemMessage(line.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
		return 1;
	}

	private SleeveCommand() {}
}
