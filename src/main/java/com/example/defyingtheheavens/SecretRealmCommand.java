package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * The secret realms' commands (SecretRealms): /secretrealm info [realm] (anyone: when it opens, whom it admits, who took its inheritance);
 * and an operator's: /secretrealm stir|open|close [realm] (its cycle moved on now), /secretrealm go [realm] (into it, as through its gate),
 * /secretrealm seal [realm] (to its Seal). The realm left out, the Verdant Herb Valley.
 */
public final class SecretRealmCommand {
	private static final SuggestionProvider<CommandSourceStack> REALMS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(SecretRealm.values()).map(SecretRealm::getId), builder);

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("secretrealm");
			root.then(withRealm(Commands.literal("info"), (ctx, realm) -> info(ctx.getSource(), realm)));
			root.then(withRealm(Commands.literal("stir").requires(s -> s.hasPermission(2)), (ctx, realm) -> {
				SecretRealms.stirNow(ctx.getSource().getServer(), realm);
				return done(ctx.getSource(), Component.translatable(ModLang.CMD_SECRET_STIRRED, realm.displayName()));
			}));
			root.then(withRealm(Commands.literal("open").requires(s -> s.hasPermission(2)), (ctx, realm) -> {
				SecretRealms.openNow(ctx.getSource().getServer(), realm);
				return done(ctx.getSource(), Component.translatable(ModLang.CMD_SECRET_OPENED, realm.displayName()));
			}));
			root.then(withRealm(Commands.literal("close").requires(s -> s.hasPermission(2)), (ctx, realm) -> {
				SecretRealms.closeNow(ctx.getSource().getServer(), realm);
				return done(ctx.getSource(), Component.translatable(ModLang.CMD_SECRET_CLOSED, realm.displayName()));
			}));
			root.then(withRealm(Commands.literal("go").requires(s -> s.hasPermission(2)), (ctx, realm) -> go(ctx.getSource().getPlayerOrException(), realm)));
			root.then(withRealm(Commands.literal("seal").requires(s -> s.hasPermission(2)), (ctx, realm) -> seal(ctx.getSource().getPlayerOrException(), realm)));
			dispatcher.register(root);
		});
	}

	private interface Run {
		int run(CommandContext<CommandSourceStack> ctx, SecretRealm realm) throws com.mojang.brigadier.exceptions.CommandSyntaxException;
	}

	/** {@code literal} run for the Verdant Herb Valley, or for the realm named after it. */
	private static LiteralArgumentBuilder<CommandSourceStack> withRealm(LiteralArgumentBuilder<CommandSourceStack> literal, Run run) {
		return literal.executes(ctx -> run.run(ctx, SecretRealm.VERDANT_HERB_VALLEY))
				.then(Commands.argument("realm", StringArgumentType.word()).suggests(REALMS).executes(ctx -> {
					SecretRealm realm = SecretRealm.byId(StringArgumentType.getString(ctx, "realm"));
					if (realm == null) {
						ctx.getSource().sendFailure(Component.translatable(ModLang.CMD_SECRET_UNKNOWN, StringArgumentType.getString(ctx, "realm")));
						return 0;
					}
					return run.run(ctx, realm);
				}));
	}

	private static int done(CommandSourceStack source, Component line) {
		source.sendSuccess(() -> line.copy().withStyle(ChatFormatting.GOLD), true);
		return 1;
	}

	/** When it opens (or how it stands), whom it admits, where its Seal is (an operator is told), who took its inheritance. */
	private static int info(CommandSourceStack source, SecretRealm realm) {
		MinecraftServer server = source.getServer();
		SecretRealms.Record r = SecretRealms.Data.get(server).of(realm);
		long now = server.overworld().getGameTime();
		Component state = r.openedAt >= 0 ? Component.translatable(ModLang.CMD_SECRET_OPEN, SecretRealms.clock(SecretRealms.untilClose(r, now)), r.inside.size())
				: r.stirred ? Component.translatable(ModLang.CMD_SECRET_STIRRING, SecretRealms.clock(SecretRealms.untilOpen(r, now)))
				: Component.translatable(ModLang.CMD_SECRET_SLEEPING, SecretRealms.years(SecretRealms.untilOpen(r, now)));
		source.sendSuccess(() -> Component.translatable(ModLang.MSG_SECRET_INFO, realm.displayName(), realm.lowestName(), realm.meantFor().getDisplayName())
				.withStyle(ChatFormatting.GOLD), false);
		source.sendSuccess(() -> state.copy().withStyle(ChatFormatting.YELLOW), false);
		if (source.hasPermission(2) && r.seal != null) {
			source.sendSuccess(() -> Component.translatable(ModLang.CMD_SECRET_SEAL_AT, r.seal.getX(), r.seal.getY(), r.seal.getZ(), r.openings).withStyle(ChatFormatting.GRAY), false);
		}
		source.sendSuccess(() -> (r.inheritor.isEmpty() ? Component.translatable(ModLang.MSG_SECRET_INFO_HEIR_WAITS)
				: Component.translatable(ModLang.MSG_SECRET_INFO_HEIR, r.inheritor)).withStyle(ChatFormatting.GRAY), false);
		return 1;
	}

	/** Into the realm as through its gate (opened now if it isn't). */
	private static int go(ServerPlayer p, SecretRealm realm) {
		SecretRealms.Record r = SecretRealms.Data.get(p.server).of(realm);
		if (r.openedAt < 0) SecretRealms.openNow(p.server, realm);
		SecretRealms.enter(p, r);
		return 1;
	}

	/** To the realm's Seal (built now if it isn't). */
	private static int seal(ServerPlayer p, SecretRealm realm) {
		SecretRealms.Data data = SecretRealms.Data.get(p.server);
		SecretRealms.Record r = data.of(realm);
		SecretRealms.ensureSeal(p.server, data, r);
		ServerLevel home = p.server.getLevel(realm.home());
		if (home == null || r.seal == null) return 0;
		p.teleportTo(home, r.seal.getX() + 0.5, r.seal.getY() + 1, r.seal.getZ() + 4.5, 180, 0);
		return 1;
	}

	private SecretRealmCommand() {}
}
