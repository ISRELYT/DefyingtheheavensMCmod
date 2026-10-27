package com.example.defyingtheheavens.client.mixin;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** The client's own record of its advancements' progress, as the server sends it: what the Dao Codex unlocks by (CodexState). */
@Mixin(ClientAdvancements.class)
public interface ClientAdvancementsAccessor {
	@Accessor("progress")
	Map<Advancement, AdvancementProgress> dth$progress();
}
