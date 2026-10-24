package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pouring qi into something that ages (a Spirit Peach Tree, growing ginseng): only a cultivator with a Golden Core (Core
 * Formation or higher, on any path: TwoPaths#awakens, paid from that path's own pool) can, and qi buys whole years at the rising
 * cost of SpiritPeachHeartBlockEntity#qiCost. (The heaven path's alone before the sweep of 2026-10-23.)
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
        TwoPaths.Path path = TwoPaths.awakens(c, Realm.CORE_FORMATION, true);
        if (path == null) {
            player.displayClientMessage(Component.translatable(messages.needCore()), true);
            return null;
        }
        if (age >= FruitAge.MAX_YEARS) {
            player.displayClientMessage(Component.translatable(messages.max()), true);
            return null;
        }
        double pool = TwoPaths.pool(c, path);
        int years = SpiritPeachHeartBlockEntity.yearsFor(age, pool);
        if (years < 1) {
            player.displayClientMessage(Component.translatable(messages.notEnough(),
                    (int) Math.ceil(SpiritPeachHeartBlockEntity.qiCost(age, 1))), true);
            return null;
        }
        double cost = Math.min(pool, SpiritPeachHeartBlockEntity.qiCost(age, years));
        if (path != TwoPaths.Path.GHOST) return QiManager.trySpend(player, cost) ? new Fed(years, cost) : null;
        if (!TwoPaths.spend(c, path, cost)) return null;
        CultivationManager.markDirty(player.server);
        NetherworldManager.refresh(player); // the ghost qi's bar
        return new Fed(years, cost);
    }

    private QiFeeding() {}
}
