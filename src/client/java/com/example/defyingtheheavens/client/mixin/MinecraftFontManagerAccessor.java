package com.example.defyingtheheavens.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The game's font manager (HallFonts#screenFont). */
@Mixin(Minecraft.class)
public interface MinecraftFontManagerAccessor {
	@Accessor("fontManager")
	FontManager dth$fontManager();
}
