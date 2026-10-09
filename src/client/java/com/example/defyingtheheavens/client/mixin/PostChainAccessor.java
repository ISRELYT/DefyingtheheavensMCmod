package com.example.defyingtheheavens.client.mixin;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Qi Sense eases its monochrome in and out by setting the post effect passes' uniforms every frame. */
@Mixin(PostChain.class)
public interface PostChainAccessor {
	@Accessor("passes")
	List<PostPass> dth$getPasses();
}
