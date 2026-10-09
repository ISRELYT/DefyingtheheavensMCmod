package com.example.defyingtheheavens;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Temporary operator tools until fruit-bearing tree cultivation is designed. */
public final class CultivationFruitCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
            Commands.literal("cultivationfruit").requires(source -> source.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("years", IntegerArgumentType.integer(1, FruitAge.MAX_YEARS))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        ItemStack fruit = CultivationFruitItem.create(IntegerArgumentType.getInteger(ctx, "years"));
                        if (!player.getInventory().add(fruit)) player.drop(fruit, false);
                        return 1;
                    })))
                .then(Commands.literal("attach").then(Commands.argument("years", IntegerArgumentType.integer(1, FruitAge.MAX_YEARS))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        HitResult hit = player.pick(8, 0, false);
                        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                                || !player.serverLevel().getBlockState(blockHit.getBlockPos()).is(BlockTags.LEAVES)) {
                            ctx.getSource().sendFailure(Component.literal("Look at leaves within 8 blocks."));
                            return 0;
                        }
                        BlockPos pos = blockHit.getBlockPos().below();
                        if (!player.serverLevel().isEmptyBlock(pos)) {
                            ctx.getSource().sendFailure(Component.literal("The space directly beneath those leaves must be empty."));
                            return 0;
                        }
                        if (!player.serverLevel().setBlock(pos, ModBlocks.CULTIVATION_FRUIT.defaultBlockState(), 3)
                                || !(player.serverLevel().getBlockEntity(pos) instanceof CultivationFruitBlockEntity fruit)) return 0;
                        fruit.setAge(IntegerArgumentType.getInteger(ctx, "years"));
                        ctx.getSource().sendSuccess(() -> Component.literal("Fruit attached. It ages until harvested."), false);
                        return 1;
                    })))));
    }
    private CultivationFruitCommand() {}
}
