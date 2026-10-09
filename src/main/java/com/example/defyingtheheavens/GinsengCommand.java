package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Operator tools for ginseng (like /cultivationfruit):
 * <pre>
 * /ginseng give &lt;years&gt; [spirit]    a harvested root of that age
 * /ginseng plant &lt;years&gt; [spirit]   plants one, growing, on the soil you are looking at (a seedling under 4
 *                                    years, young under 10, full grown after)
 * </pre>
 */
public final class GinsengCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
            Commands.literal("ginseng").requires(source -> source.hasPermission(2))
                .then(Commands.literal("give").then(withKind(Commands.argument("years", IntegerArgumentType.integer(1, FruitAge.MAX_YEARS)),
                    GinsengCommand::give)))
                .then(Commands.literal("plant").then(withKind(Commands.argument("years", IntegerArgumentType.integer(1, FruitAge.MAX_YEARS)),
                    GinsengCommand::plant)))));
    }

    @FunctionalInterface
    private interface Action {
        int run(CommandContext<CommandSourceStack> ctx, boolean spirit) throws com.mojang.brigadier.exceptions.CommandSyntaxException;
    }

    /** "... <years>" gives ordinary ginseng, "... <years> spirit" Spirit Ginseng. */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withKind(T years, Action action) {
        return years.executes(ctx -> action.run(ctx, false))
                .then(Commands.literal("spirit").executes(ctx -> action.run(ctx, true)));
    }

    private static int give(CommandContext<CommandSourceStack> ctx, boolean spirit) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack root = GinsengItem.create(spirit ? ModItems.SPIRIT_GINSENG : ModItems.GINSENG, IntegerArgumentType.getInteger(ctx, "years"));
        if (!player.getInventory().add(root)) player.drop(root, false);
        return 1;
    }

    private static int plant(CommandContext<CommandSourceStack> ctx, boolean spirit) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        HitResult hit = player.pick(8, 0, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            ctx.getSource().sendFailure(Component.literal("Look at soil within 8 blocks."));
            return 0;
        }
        Block block = spirit ? ModBlocks.SPIRIT_GINSENG : ModBlocks.GINSENG;
        BlockPos pos = blockHit.getBlockPos().above();
        if (!level.isEmptyBlock(pos) || !block.defaultBlockState().canSurvive(level, pos)) {
            ctx.getSource().sendFailure(Component.literal("Ginseng needs soil (grass, dirt, podzol...) with open space above it."));
            return 0;
        }
        if (!level.setBlock(pos, block.defaultBlockState(), 3) || !(level.getBlockEntity(pos) instanceof GinsengBlockEntity ginseng)) return 0;
        ginseng.setAge(IntegerArgumentType.getInteger(ctx, "years"));
        ctx.getSource().sendSuccess(() -> Component.literal("Ginseng planted. It ages until it is dug up."), false);
        return 1;
    }

    private GinsengCommand() {}
}
