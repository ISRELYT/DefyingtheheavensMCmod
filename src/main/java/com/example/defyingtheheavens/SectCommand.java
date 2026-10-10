package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Op-only helpers for sects, NPC cultivators and alignment:
 * <ul>
 *   <li>/sect info: the nearest sect (within 512 blocks): its path, its barrier, its members with their titles and realms.</li>
 *   <li>/cultivator spawn &lt;realm 0-6&gt; &lt;stage 1-4&gt; &lt;alignment&gt;: a rogue where you stand (realm 0: a mortal).</li>
 *   <li>/alignment get [player], /alignment set &lt;player&gt; &lt;value&gt;.</li>
 * </ul>
 * Sects themselves can be placed with vanilla's {@code /place structure defying-the-heavens:sect} and found with
 * {@code /locate structure defying-the-heavens:sect} ({@code sect_upper_realm} in the Upper Realm).
 */
public final class SectCommand {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			dispatcher.register(Commands.literal("sect")
					.requires(src -> src.hasPermission(2))
					.then(Commands.literal("info").executes(ctx -> info(ctx.getSource()))));

			dispatcher.register(Commands.literal("cultivator")
					.requires(src -> src.hasPermission(2))
					.then(Commands.literal("spawn")
							.then(Commands.argument("realm", IntegerArgumentType.integer(0, Realm.values().length))
									.then(Commands.argument("stage", IntegerArgumentType.integer(1, Stage.values().length))
											.then(Commands.argument("alignment", IntegerArgumentType.integer(Alignment.MIN, Alignment.MAX))
													.executes(ctx -> {
														CommandSourceStack source = ctx.getSource();
														ServerLevel level = source.getLevel();
														int realm = IntegerArgumentType.getInteger(ctx, "realm");
														int stage = IntegerArgumentType.getInteger(ctx, "stage");
														int rank = realm == 0 ? CultivatorNpc.MORTAL : PlayerCultivation.rank(Realm.byIndex(realm - 1), Stage.byIndex(stage - 1));
														CultivatorNpc npc = ModEntities.CULTIVATOR.create(level);
														if (npc == null) return 0;
														Vec3 at = source.getPosition();
														npc.moveTo(at.x, at.y, at.z, source.getRotation().y, 0);
														npc.becomeRogue(rank, IntegerArgumentType.getInteger(ctx, "alignment"), level.getRandom());
														level.addFreshEntity(npc);
														source.sendSuccess(() -> Component.literal("Spawned a ").append(npc.describe()), false);
														return 1;
													}))))));

			dispatcher.register(Commands.literal("alignment")
					.then(Commands.literal("get")
							.executes(ctx -> get(ctx.getSource(), ctx.getSource().getPlayerOrException()))
							.then(Commands.argument("player", EntityArgument.player())
									.requires(src -> src.hasPermission(2))
									.executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
					.then(Commands.literal("set")
							.requires(src -> src.hasPermission(2))
							.then(Commands.argument("player", EntityArgument.player())
									.then(Commands.argument("value", IntegerArgumentType.integer(Alignment.MIN, Alignment.MAX))
											.executes(ctx -> {
												ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
												PlayerCultivation c = CultivationManager.get(player);
												c.setAlignment(IntegerArgumentType.getInteger(ctx, "value"));
												CultivationManager.markDirty(player.server);
												CultivationManager.sync(player);
												return get(ctx.getSource(), player);
											})))));
		});
	}

	private static int get(CommandSourceStack source, ServerPlayer player) {
		int alignment = CultivationManager.get(player).getAlignment();
		source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + ": ").append(Alignment.describe(alignment)), false);
		return 1;
	}

	private static int info(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		Vec3 at = source.getPosition();
		Sect nearest = null;
		double best = 512 * 512;
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (sect.dimension != level.dimension()) continue;
			double d = sect.core.distToCenterSqr(at);
			if (d < best) {
				best = d;
				nearest = sect;
			}
		}
		if (nearest == null) {
			source.sendFailure(Component.literal("No sect within 512 blocks."));
			return 0;
		}
		Sect sect = nearest;
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal(sect.name).withStyle(ChatFormatting.GOLD).append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
				.append(Alignment.describe(sect.alignment)).append(Component.literal(")").withStyle(ChatFormatting.GRAY)));
		lines.add(Component.literal("Core " + sect.core.toShortString() + ", barrier radius " + sect.barrierRadius
				+ (sect.isBarrierDown() ? " (DOWN)" : "") + ", members " + sect.members.size() + "/" + sect.capacity
				+ (sect.extinct ? ", EXTINCT" : "") + ", replacements due " + sect.replacements.size()).withStyle(ChatFormatting.GRAY));
		List<Sect.Member> members = new ArrayList<>(sect.members.values());
		members.sort(Sect.SENIORITY);
		for (Sect.Member member : members) {
			MutableComponent line = Component.literal("  ").append(Component.translatable(ModLang.NPC_TITLE_BRACKETS, member.title.getDisplayName())
					.withStyle(style -> style.withColor(member.title.getColor())));
			line.append(" ").append(PlayerCultivation.rankName(CultivatorNpc.realmOf(member.rank), CultivatorNpc.stageOf(member.rank)));
			lines.add(line);
		}
		for (Component line : lines) source.sendSuccess(() -> line, false);
		return 1;
	}

	private SectCommand() {}
}
