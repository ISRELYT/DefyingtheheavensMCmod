package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;
import java.util.Locale;

/**
 * The op's commands for the legendary weapons (Legends): /legend list (where each is and who has it), /legend give <legend> (a new true
 * stack into your pack, every other stack of it a hollow echo from then on), /legend reform <legend> (made anew at home now, as a lost one
 * is a day on), /legend goto <legend> (to its resting place, or near Lei Zhen), /legend brand <player> <percent> (heaven's brand on a player
 * set, 0 to lift it: LegendaryWeaponItem#heavensBrand).
 */
public final class LegendCommand {
	private static final SuggestionProvider<CommandSourceStack> LEGENDS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(Legend.values()).map(Legend::getId), builder);

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("legend").requires(src -> src.hasPermission(2))
						.then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
						.then(Commands.literal("give").then(Commands.argument("legend", StringArgumentType.word()).suggests(LEGENDS)
								.executes(ctx -> give(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "legend")))))
						.then(Commands.literal("reform").then(Commands.argument("legend", StringArgumentType.word()).suggests(LEGENDS)
								.executes(ctx -> reform(ctx.getSource(), StringArgumentType.getString(ctx, "legend")))))
						.then(Commands.literal("goto").then(Commands.argument("legend", StringArgumentType.word()).suggests(LEGENDS)
								.executes(ctx -> go(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "legend")))))
						.then(Commands.literal("brand").then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("percent", IntegerArgumentType.integer(0, LegendaryWeaponItem.BRAND_MOST))
										.executes(ctx -> brand(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
												IntegerArgumentType.getInteger(ctx, "percent"))))))));
	}

	private static int list(CommandSourceStack source) {
		for (Legends.Entry entry : Legends.all(source.getServer())) {
			BlockPos site = entry.site();
			String where = site == null ? "its place not chosen yet" : "at " + site.getX() + ", " + site.getZ() + " in " + entry.legend.home().location()
					+ (entry.laid() || entry.legend == Legend.TRIBULATION_SPEAR ? "" : " (not laid yet)");
			String holder = entry.holderName().isEmpty() ? "" : " (" + entry.holderName() + ")";
			String drunk = entry.legend == Legend.TRIBULATION_SPEAR ? ", " + entry.drunk() + " bolts drunk" : "";
			String line = entry.legend.getId() + ": " + entry.state().name().toLowerCase(Locale.ROOT) + holder + ", serial " + entry.serial() + ", " + where + drunk;
			source.sendSuccess(() -> Component.literal(line), false);
		}
		return 1;
	}

	private static int give(ServerPlayer player, String id) {
		Legend legend = Legend.byId(id);
		if (legend == null) {
			player.sendSystemMessage(Component.literal("No legend called " + id + "."));
			return 0;
		}
		ItemStack stack = Legends.issue(player.server, legend);
		int serial = LegendaryWeaponItem.serial(stack);
		if (!player.getInventory().add(stack)) player.drop(stack, false);
		Legends.seen(player, legend);
		player.sendSystemMessage(Component.literal("The true " + legend.getId() + " (serial " + serial + ") is yours: every other stack of it is a hollow echo now."));
		return 1;
	}

	private static int reform(CommandSourceStack source, String id) {
		Legend legend = Legend.byId(id);
		if (legend == null) {
			source.sendFailure(Component.literal("No legend called " + id + "."));
			return 0;
		}
		Legends.reform(source.getServer(), legend);
		source.sendSuccess(() -> Component.literal(legend.getId() + " is made anew at home."), true);
		return 1;
	}

	/**
	 * To {@code id}'s resting place, a little way off it (the Xuanyuan Sword's onto its shrine's terrace, its peak standing alone); high over the
	 * sea of clouds by the Sea-Fixing Needle's pillar; or high up near Lei Zhen, within his consciousness (he goes for anyone not in creative).
	 */
	private static int go(ServerPlayer player, String id) {
		Legend legend = Legend.byId(id);
		ServerLevel level = legend == null ? null : player.server.getLevel(legend.home());
		BlockPos site = legend == null ? null : Legends.get(player.server).entry(legend).site();
		if (level == null || site == null) {
			player.sendSystemMessage(Component.literal(legend == null ? "No legend called " + id + "." : "Its place isn't chosen yet: wait a few seconds."));
			return 0;
		}
		// Pangu's Axe: beside the Primordial Chaos's lair, aloft over the Chaos Frontier; a Venerable's weapon: at the Celestial Court.
		if (legend == Legend.PANGU_AXE) {
			BlockPos lair = PrimordialBeasts.findLair(level, BeastKind.PRIMORDIAL_CHAOS);
			level.getChunk(lair.getX() >> 4, (lair.getZ() + 60) >> 4);
			player.teleportTo(level, lair.getX() + 0.5, lair.getY() + 20, lair.getZ() + 60.5, 180, 15);
			return 1;
		}
		if (legend.venerable() != null) {
			BlockPos court = CelestialCourt.middle(level);
			level.getChunk(court.getX() >> 4, court.getZ() >> 4);
			player.teleportTo(level, court.getX() + 0.5, court.getY() + 2, court.getZ() + 0.5, 180, 10);
			return 1;
		}
		boolean aloft = legend == Legend.TRIBULATION_SPEAR || legend == Legend.SEA_NEEDLE;
		int x = site.getX(), z = site.getZ() + switch (legend) {
			case TRIBULATION_SPEAR -> 100;
			case SEA_NEEDLE -> 12;
			case XUANYUAN -> 4;
			default -> 16;
		};
		level.getChunk(x >> 4, z >> 4);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (aloft) y = Math.max(y, ModDimensions.IMMORTAL_CLOUD_Y) + 20;
		else if (y <= level.getMinBuildHeight() + 1 && site.getY() > level.getMinBuildHeight()) y = site.getY() + 1; // nothing under it: onto its own floor
		player.teleportTo(level, x + 0.5, y, z + 0.5, 180, 15);
		return 1;
	}

	private static int brand(CommandSourceStack source, ServerPlayer target, int percent) {
		CultivationManager.get(target).setTribulationBrand(percent);
		CultivationManager.markDirty(source.getServer());
		source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " now bears heaven's brand of " + percent + " percent more tribulation bolts."), true);
		return 1;
	}

	private LegendCommand() {}
}
