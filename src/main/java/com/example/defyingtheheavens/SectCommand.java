package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Op-only helpers for sects, NPC cultivators and alignment:
 * <ul>
 *   <li>/sect info: the nearest sect (within 512 blocks): its path, its barrier, its members with their titles and realms.</li>
 *   <li>/cultivator spawn &lt;realm 0-6&gt; &lt;stage 1-4&gt; &lt;alignment&gt;: a rogue where you stand (realm 0: a mortal).</li>
 *   <li>/cultivator secluded [count]: the secluded cultivators nearest you, and land where a hermit will be seated.</li>
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
					.then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
					// A player made a disciple of the nearest sect (no trial), or taken off their sect's rolls (SectMembership).
					.then(Commands.literal("join").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
						ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
						Sect sect = nearest(ctx.getSource());
						if (sect == null) {
							ctx.getSource().sendFailure(Component.literal("No sect within 512 blocks."));
							return 0;
						}
						SectMembership.join(player, sect);
						ctx.getSource().sendSuccess(() -> Component.literal(player.getGameProfile().getName() + " joined " + sect.name + "."), true);
						return 1;
					})))
					.then(Commands.literal("leave").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
						ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
						Sect sect = SectMembership.sectOf(player.server, player.getUUID());
						if (sect == null) {
							ctx.getSource().sendFailure(Component.literal(player.getGameProfile().getName() + " belongs to no sect."));
							return 0;
						}
						SectMembership.leave(player, sect);
						ctx.getSource().sendSuccess(() -> Component.literal(player.getGameProfile().getName() + " left " + sect.name + "."), true);
						return 1;
					}))));

			dispatcher.register(Commands.literal("cultivator")
					.requires(src -> src.hasPermission(2))
					// Where the secluded are: everyone known to sit in seclusion (or to be seeking a place for it), nearest first.
					.then(Commands.literal("secluded")
							.executes(ctx -> secluded(ctx.getSource(), 10))
							.then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
									.executes(ctx -> secluded(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))))
					// The Immortal Gate (ImmortalGate): opened or shut out of turn, or how it stands.
					.then(Commands.literal("gate")
							.then(Commands.literal("open").executes(ctx -> {
								ImmortalGate.openNow(ctx.getSource().getServer());
								ctx.getSource().sendSuccess(() -> Component.literal("The Immortal Gate opens for " + ImmortalGate.OPEN_TICKS / 20 + " s."), true);
								return 1;
							}))
							.then(Commands.literal("close").executes(ctx -> {
								ImmortalGate.shutNow(ctx.getSource().getServer());
								ctx.getSource().sendSuccess(() -> Component.literal("The Immortal Gate shuts till its next opening."), true);
								return 1;
							}))
							.then(Commands.literal("status").executes(ctx -> {
								MinecraftServer server = ctx.getSource().getServer();
								long until = ImmortalGate.untilOpen(server.overworld().getGameTime());
								ctx.getSource().sendSuccess(() -> Component.literal(ImmortalGate.isOpen(server) ? "The Immortal Gate stands open."
										: "The Immortal Gate is shut; it opens in " + until / 20 + " s (" + String.format(java.util.Locale.ROOT, "%.2f", until / 24000.0) + " days)."), false);
								return 1;
							})))
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
														CultivatorNpc npc = CultivatorNpc.createFor(level);
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

	/** One secluded cultivator found: who, where, and what is known of it (its realm if its land is loaded). */
	private record Secluded(String name, ResourceKey<Level> dimension, BlockPos pos, Component state) {}

	/**
	 * /cultivator secluded [count]: the secluded cultivators nearest the source (those in this dimension first), from the loaded
	 * ones (their realm, seated or still seeking a place) and the rumours of caves (those whose land is unloaded); each line
	 * teleports there when clicked. Then the nearest spots where a hermit will be seated once their land first loads (HermitSites).
	 */
	private static int secluded(CommandSourceStack source, int count) {
		MinecraftServer server = source.getServer();
		Vec3 at = source.getPosition();
		ResourceKey<Level> here = source.getLevel().dimension();
		List<Secluded> found = new ArrayList<>();
		java.util.Set<String> seen = new java.util.HashSet<>();
		for (ServerLevel level : server.getAllLevels()) {
			for (CultivatorNpc npc : CultivatorNpc.loaded(level)) {
				CultivatorNpc.Lifecycle life = npc.getLifecycle();
				if (life != CultivatorNpc.Lifecycle.SECLUDED && life != CultivatorNpc.Lifecycle.SEEKING_SECLUSION) continue;
				seen.add("cave:" + npc.getUUID());
				BlockPos pos = life == CultivatorNpc.Lifecycle.SECLUDED && npc.getSeclusionMat() != null ? npc.getSeclusionMat() : npc.blockPosition();
				Component state = PlayerCultivation.rankName(CultivatorNpc.realmOf(npc.getRank()), CultivatorNpc.stageOf(npc.getRank())).copy()
						.append(Component.literal(life == CultivatorNpc.Lifecycle.SECLUDED ? ", in seclusion" : ", seeking a place for seclusion"));
				found.add(new Secluded(npc.getPersonalName(), level.dimension(), pos, state));
			}
		}
		for (Rumours.Rumour cave : Rumours.all(server, Rumours.Kind.CAVE)) {
			if (seen.contains(cave.id)) continue;
			found.add(new Secluded(cave.subject, cave.dimension, cave.pos, Component.literal("in seclusion, its land unloaded")));
		}
		found.sort(java.util.Comparator.comparing((Secluded s) -> s.dimension() != here).thenComparingDouble(s -> s.pos().distToCenterSqr(at)));
		source.sendSuccess(() -> Component.literal(found.isEmpty() ? "No secluded cultivators are known yet."
				: found.size() + " secluded cultivator" + (found.size() == 1 ? "" : "s") + " known" + (found.size() > count ? "; the nearest " + count : "") + ":")
				.withStyle(ChatFormatting.GOLD), false);
		for (Secluded s : found.subList(0, Math.min(count, found.size()))) {
			BlockPos p = s.pos();
			String where = p.getX() + " " + p.getY() + " " + p.getZ();
			MutableComponent line = Component.literal("  " + s.name()).withStyle(ChatFormatting.AQUA)
					.append(Component.literal(" (").withStyle(ChatFormatting.GRAY)).append(s.state().copy().withStyle(ChatFormatting.GRAY))
					.append(Component.literal(") at ").withStyle(ChatFormatting.GRAY))
					.append(Component.literal(where).withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
							.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
									"/execute in " + s.dimension().location() + " run tp @s " + p.getX() + " " + (p.getY() + 1) + " " + p.getZ()))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Teleport there")))))
					.append(Component.literal(s.dimension() == here ? ", " + Math.round(Math.sqrt(p.distToCenterSqr(at))) + " blocks away"
							: " in " + s.dimension().location().getPath()).withStyle(ChatFormatting.GRAY));
			source.sendSuccess(() -> line, false);
		}
		// Hermits still to be seated: the land there hasn't been loaded yet.
		List<net.minecraft.world.level.ChunkPos> unseated = HermitSites.unseatedNear(source.getLevel(), BlockPos.containing(at), 3);
		unseated.sort(java.util.Comparator.comparingDouble(c -> c.getMiddleBlockPosition((int) at.y).distToCenterSqr(at)));
		if (!unseated.isEmpty()) {
			source.sendSuccess(() -> Component.literal("Land nobody has loaded yet where a hermit will be seated (if it holds a cave or a high peak):")
					.withStyle(ChatFormatting.GOLD), false);
			for (net.minecraft.world.level.ChunkPos chunk : unseated.subList(0, Math.min(5, unseated.size()))) {
				int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
				MutableComponent line = Component.literal("  around ").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(x + ", " + z).withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
								.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + x + " 200 " + z))
								.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Put a teleport there in the chat")))))
						.append(Component.literal(", " + Math.round(Math.sqrt(chunk.getMiddleBlockPosition((int) at.y).distToCenterSqr(at))) + " blocks away")
								.withStyle(ChatFormatting.GRAY));
				source.sendSuccess(() -> line, false);
			}
		}
		return found.size();
	}

	private static int get(CommandSourceStack source, ServerPlayer player) {
		int alignment = CultivationManager.get(player).getAlignment();
		source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + ": ").append(Alignment.describe(alignment)), false);
		return 1;
	}

	/** The sect whose core is nearest the command's source, within 512 blocks; or null. */
	private static Sect nearest(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		Vec3 at = source.getPosition();
		Sect nearest = null;
		double best = 512 * 512;
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (sect.dimension != level.dimension() || sect.extinct) continue;
			double d = sect.core.distToCenterSqr(at);
			if (d < best) {
				best = d;
				nearest = sect;
			}
		}
		return nearest;
	}

	private static int info(CommandSourceStack source) {
		Sect sect = nearest(source);
		if (sect == null) {
			source.sendFailure(Component.literal("No sect within 512 blocks."));
			return 0;
		}
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal(sect.name).withStyle(ChatFormatting.GOLD).append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
				.append(Alignment.describe(sect.alignment)).append(Component.literal(")").withStyle(ChatFormatting.GRAY)));
		lines.add(Component.literal("Core " + sect.core.toShortString() + ", barrier radius " + sect.barrierRadius
				+ (sect.isBarrierDown() ? " (DOWN)" : "") + ", members " + sect.members.size() + "/" + sect.capacity
				+ (sect.extinct ? ", EXTINCT" : "") + ", replacements due " + sect.replacements.size()).withStyle(ChatFormatting.GRAY));
		List<Sect.Member> members = new ArrayList<>(sect.members.values());
		members.sort(Sect.SENIORITY);
		for (Sect.Member member : members) {
			MutableComponent line = Component.literal("  ").append(Component.translatable(ModLang.NPC_TITLE_BRACKETS, member.title.getDisplayName(sect.isClan(), sect.isHoly(), sect.isImmortal()))
					.withStyle(style -> style.withColor(member.title.getColor())));
			line.append(" ").append(PlayerCultivation.rankName(CultivatorNpc.realmOf(member.rank), CultivatorNpc.stageOf(member.rank)));
			lines.add(line);
		}
		for (Sect.Disciple disciple : sect.disciples.values()) {
			ServerPlayer online = source.getServer().getPlayerList().getPlayer(disciple.id);
			NpcTitle title = online != null ? SectMembership.titleOf(sect, online) : null;
			lines.add(Component.literal("  player " + disciple.name + (title != null ? " " : " (offline)")).withStyle(ChatFormatting.AQUA)
					.append(title != null ? Component.translatable(ModLang.NPC_TITLE_BRACKETS, title.getDisplayName()) : Component.empty())
					.append(Component.literal(disciple.treasuryTries > 0 ? ", treasury tries " + disciple.treasuryTries : "").withStyle(ChatFormatting.GRAY)));
		}
		for (Component line : lines) source.sendSuccess(() -> line, false);
		return 1;
	}

	private SectCommand() {}
}
