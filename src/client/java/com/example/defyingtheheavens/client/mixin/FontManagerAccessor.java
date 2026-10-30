package com.example.defyingtheheavens.client.mixin;

import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** The loaded fonts by name, for a font of the framed screens' own (HallFonts#screenFont). */
@Mixin(FontManager.class)
public interface FontManagerAccessor {
	@Accessor("fontSets")
	Map<ResourceLocation, FontSet> dth$fontSets();

	@Accessor("missingFontSet")
	FontSet dth$missingFontSet();
}
