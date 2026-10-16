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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * The body each player is born with (Physique), server side: rolled the first time they join ({@link #onJoin}), told in chat
 * and written in their journal; a Jade Poison Body's waiting pill qi refined as soon as it fits ({@link #tick}); and an op's
 * /physique get|set|reroll [player].
 */
public final class Physiques {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("physique")
						.requires(src -> src.hasPermission(2))
						.then(Commands.literal("get")
								.executes(ctx -> get(ctx.getSource(), ctx.getSource().getPlayerOrException()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
						.then(Commands.literal("set")
								.then(Commands.argument("id", StringArgumentType.word()).suggests(IDS)
										.executes(ctx -> set(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id")))
										.then(Commands.argument("player", EntityArgument.player())
												.executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
														StringArgumentType.getString(ctx, "id"))))))
						.then(Commands.literal("reroll")
								.executes(ctx -> reroll(ctx.getSource(), ctx.getSource().getPlayerOrException()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> reroll(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))));
	}

	private static final SuggestionProvider<CommandSourceStack> IDS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(Physique.values()).map(Physique::getId), builder);

	/** Whether joining rolls a physique: the game tests turn it off, so their mock players are born plain (a Mortal Body, no roots). */
	public static boolean rollOnJoin = true;

	/** On joining: one born before physiques were (or never yet joined) is given one now, and told. */
	public static void onJoin(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		if (c.getPhysique() != null || !rollOnJoin) return;
		Physique physique = Physique.roll(player.getRandom());
		// One already cultivating (a world from before physiques) can't have been born with meridians that never held qi.
		while (physique == Physique.SHATTERED_MERIDIANS && !c.isMortal()) physique = Physique.roll(player.getRandom());
		give(player, physique);
	}

	/** {@code physique} becomes {@code player}'s, its roots drawn (the elements of methods known first), and they are told. */
	public static void give(ServerPlayer player, Physique physique) {
		PlayerCultivation c = CultivationManager.get(player);
		c.setPhysique(physique);
		Set<QiElement> known = EnumSet.noneOf(QiElement.class);
		for (HeavenMethod method : c.heaven().known()) known.add(method.element());
		c.setRootElements(Physique.rollRoots(physique, known, player.getRandom()));
		if (!c.hasShatteredMeridians()) c.setBodyRefining(0);
		CultivationManager.refresh(player); // the qi pool, the body (and the client told)
		tell(player, c);
	}

	private static void tell(ServerPlayer player, PlayerCultivation c) {
		Physique physique = c.physique();
		player.sendSystemMessage(Component.translatable(ModLang.MSG_PHYSIQUE_BORN, physique.getDisplayName(), physique.grade().getDisplayName(),
				physique.getDescription()).withStyle(ChatFormatting.GOLD));
		if (!c.getRootElements().isEmpty()) {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_PHYSIQUE_ROOTS, rootsLine(c.getRootElements())).withStyle(ChatFormatting.GOLD));
		}
		if (physique.grade().ordinal() >= Physique.Grade.SUPREME.ordinal()) {
			player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.0f);
		}
		Component roots = c.getRootElements().isEmpty() ? Component.empty()
				: Component.translatable(ModLang.MSG_PHYSIQUE_ROOTS, rootsLine(c.getRootElements()));
		Journal.note(player, "physique", ModLang.JOURNAL_PHYSIQUE, Component.translatable(ModLang.JOURNAL_PHYSIQUE_TEXT, physique.getDisplayName(),
				physique.grade().getDisplayName(), physique.getDescription()).append(" ").append(roots));
	}

	/** "Fire, Water, Wood": the roots' elements, each in its colour. */
	public static Component rootsLine(Collection<QiElement> roots) {
		MutableComponent line = Component.empty();
		boolean first = true;
		for (QiElement element : roots) {
			if (!first) line.append(", ");
			line.append(element.getDisplayName());
			first = false;
		}
		return line;
	}

	/** Once a second: a Jade Poison Body's pill qi waiting behind a bottleneck refined the moment it fits. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerCultivation c = CultivationManager.get(player);
			if (c.physique() == Physique.JADE_POISON_BODY && c.getMedicinalQi() > 0 && c.refineAtOnce()) CultivationManager.refresh(player);
		}
	}

	private static int get(CommandSourceStack source, ServerPlayer player) {
		Physique physique = CultivationManager.get(player).physique();
		source.sendSuccess(() -> Component.translatable(ModLang.CMD_PHYSIQUE_GET, player.getDisplayName(), physique.getDisplayName()), false);
		return 1;
	}

	private static int set(CommandSourceStack source, ServerPlayer player, String id) {
		Physique physique = Physique.byId(id);
		if (physique == null) {
			source.sendFailure(Component.translatable(ModLang.CMD_PHYSIQUE_UNKNOWN, id));
			return 0;
		}
		give(player, physique);
		source.sendSuccess(() -> Component.translatable(ModLang.CMD_PHYSIQUE_SET, player.getDisplayName(), physique.getDisplayName()), true);
		return 1;
	}

	private static int reroll(CommandSourceStack source, ServerPlayer player) {
		give(player, Physique.roll(player.getRandom()));
		Physique physique = CultivationManager.get(player).physique();
		source.sendSuccess(() -> Component.translatable(ModLang.CMD_PHYSIQUE_SET, player.getDisplayName(), physique.getDisplayName()), true);
		return 1;
	}

	private Physiques() {}
}
