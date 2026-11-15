package com.example.defyingtheheavens.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The chunks a world holds now: Natal Recall looks through the containers in its loaded land for one's natal treasure (NatalTreasures#recall). */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
	@Invoker("getChunks")
	Iterable<ChunkHolder> dth$chunks();
}
