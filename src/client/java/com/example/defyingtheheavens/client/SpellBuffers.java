package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Buffers for drawing a spell: a buffer of its own for every render type asked of it, so a spell's renderer may hold several
 * VertexConsumers at once and draw into them in turn. (The game's own buffer source has one buffer shared by all but a few block
 * types: asking it for another type draws the last, and whatever is still drawn into the old consumer after that lands in the new
 * type, in its texture: a bramble's stems drawn in its leaves' texture, a stake's wood in its glow's.)
 *
 * <p>{@link #begin} at the top of a renderer, {@link #end} (in a finally) at its foot; nested calls share one batch. Everything is
 * drawn at the outermost {@link #end}: the solid types first, then the see-through and glowing ones in the order they were first
 * asked for, so a glow round a solid thing is not drawn over by it.
 */
public final class SpellBuffers implements MultiBufferSource {
	private static final SpellBuffers INSTANCE = new SpellBuffers();
	private static final List<BufferBuilder> POOL = new ArrayList<>();
	private static final Map<RenderType, Boolean> SOLID = new HashMap<>();
	private final LinkedHashMap<RenderType, BufferBuilder> open = new LinkedHashMap<>();
	private int depth;

	private SpellBuffers() {}

	/**
	 * The buffers to draw a spell into (the one batch already open, if this is inside another spell's drawing). What the game's buffers
	 * still hold undrawn is drawn first, as the game itself would on being asked for another type: so a ward's skin over a body is
	 * drawn after the body under it.
	 */
	public static MultiBufferSource begin(MultiBufferSource parent) {
		if (INSTANCE.depth++ == 0 && parent instanceof MultiBufferSource.BufferSource source) source.endLastBatch();
		return INSTANCE;
	}

	/** Ends a {@link #begin}: the outermost draws everything drawn into the buffers since. */
	public static void end() {
		if (INSTANCE.depth <= 0) return;
		if (--INSTANCE.depth == 0) INSTANCE.draw();
	}

	@Override
	public VertexConsumer getBuffer(RenderType type) {
		BufferBuilder builder = open.get(type);
		if (builder == null) {
			builder = POOL.isEmpty() ? new BufferBuilder(1536) : POOL.remove(POOL.size() - 1);
			if (builder.building()) {
				// Left building by a draw that failed: what was in it is dropped (a fresh buffer if it can't be).
				try {
					builder.end();
					builder.discard();
				} catch (RuntimeException e) {
					builder = new BufferBuilder(1536);
				}
			}
			builder.begin(type.mode(), type.format());
			open.put(type, builder);
		}
		return builder;
	}

	private void draw() {
		if (open.isEmpty()) return;
		List<Map.Entry<RenderType, BufferBuilder>> entries = new ArrayList<>(open.entrySet());
		open.clear();
		for (int pass = 0; pass < 2; pass++) {
			for (Map.Entry<RenderType, BufferBuilder> entry : entries) {
				if (solid(entry.getKey()) != (pass == 0)) continue;
				try {
					entry.getKey().end(entry.getValue(), RenderSystem.getVertexSorting());
				} finally {
					POOL.add(entry.getValue());
				}
			}
		}
	}

	/** Whether {@code type} is drawn solid (cut out or opaque, writing depth), as the game names its entity types. */
	private static boolean solid(RenderType type) {
		return SOLID.computeIfAbsent(type, t -> {
			String name = t.toString();
			return name.startsWith("RenderType[entity_cutout") || name.startsWith("RenderType[entity_solid") || name.startsWith("RenderType[solid")
					|| name.startsWith("RenderType[cutout");
		});
	}
}
