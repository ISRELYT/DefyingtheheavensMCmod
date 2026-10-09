package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pouring qi into something that ages (a Spirit Peach Tree, growing ginseng): only a cultivator with a Golden Core (Core
 * Formation or higher) can, and qi buys whole years at the rising cost of SpiritPeachHeartBlockEntity#qiCost.
 */
public final class QiFeeding {
    /** What a feeding bought. */
    public record Fed(int years, double qi) {}

    /** The messages shown when feeding fails, so each thing fed is named in them. */
    public record Messages(String needCore, String notEnough, String max) {}

    /**
     * Spends as much of the player's qi as buys whole years for something {@code age} years old, or explains why not.
     * @return the years bought and the qi spent, or null if nothing was fed (the player has already been told why)
     */
    public static Fed pour(ServerPlayer player, int age, Messages messages) {
        PlayerCultivation c = CultivationManager.get(player);
        if (c.getEffectiveRealm().ordinal() < Realm.CORE_FORMATION.ordinal()) {
            player.displayClientMessage(Component.translatable(messages.needCore()), true);
            return null;
        }
        if (age >= FruitAge.MAX_YEARS) {
            player.displayClientMessage(Component.translatable(messages.max()), true);
            return null;
        }
        int years = SpiritPeachHeartBlockEntity.yearsFor(age, c.getQi());
        if (years < 1) {
            player.displayClientMessage(Component.translatable(messages.notEnough(),
                    (int) Math.ceil(SpiritPeachHeartBlockEntity.qiCost(age, 1))), true);
            return null;
        }
        double cost = Math.min(c.getQi(), SpiritPeachHeartBlockEntity.qiCost(age, years));
        return QiManager.trySpend(player, cost) ? new Fed(years, cost) : null;
    }

    private QiFeeding() {}
}
