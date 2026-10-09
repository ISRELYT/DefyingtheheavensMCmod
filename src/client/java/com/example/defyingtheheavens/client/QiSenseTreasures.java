package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.FruitAura;
import com.example.defyingtheheavens.QiElement;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Spirit treasures under Qi Sense: Cultivation Fruit, ginseng and what Spirit Pedestals hold are vessels of qi, so in the
 * grey plane of Qi they are what keeps its colour.
 * <ul>
 * <li>Their auras, and a pedestal's streams and orbits around a meditator, are drawn on {@link SenseOverlay} instead of
 * into the world while Qi Sense is on, so the post effect leaves them vivid. Renderers ask {@link #glow} for the buffer to
 * draw glow into; it is the world's additive glow normally, and a buffer laid over the frame under Qi Sense.</li>
 * <li>Each one breathes out wood qi: motes (see {@link QiSenseClientHandler#emit}) rising off it, more and brighter the
 * older it is, with white-gold metal qi mixed in from 5,000 years. Pedestals feeding a meditator send their qi into them
 * as motes too (see MeditationFormation).</li>
 * </ul>
 */
public final class QiSenseTreasures {
	private static final BufferBuilder BUFFER = new BufferBuilder(1 << 16);
	private static boolean building;

	/** A treasure seen this frame: where its centre is, how old it is, and the last tick it was drawn. */
	private record Source(Vec3 centre, int years, long seenAt) {}

	private static final Map<Long, Source> SOURCES = new HashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();
	/** Sources not drawn for this many ticks (out of sight, or gone) stop breathing qi. */
	private static final int FORGET_TICKS = 5;

	public static void register() {
		// Anything left from a frame whose Qi Sense layer was never drawn is dropped, never carried into the next.
		WorldRenderEvents.START.register(context -> discard());
	}

	/** The buffer for a treasure's glow (additive, position and colour, quads), for this frame. */
	public static VertexConsumer glow(MultiBufferSource buffers) {
		if (!QiSenseClientHandler.isActive()) return buffers.getBuffer(FruitAuraRenderType.GLOW);
		if (!building) {
			BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			building = true;
		}
		return BUFFER;
	}

	/** A treasure {@code years} old was drawn this frame with its centre at {@code centre} (for its breath of qi). */
	public static void seen(BlockPos pos, Vec3 centre, int years) {
		if (!QiSenseClientHandler.isActive() || Minecraft.getInstance().level == null) return;
		SOURCES.put(pos.asLong(), new Source(centre, years, Minecraft.getInstance().level.getGameTime()));
	}

	static boolean hasGlow() {
		return building;
	}

	/** Draws this frame's glow on the Qi Sense layer, with the layer's additive blending already set. */
	static void render() {
		if (!building) return;
		building = false;
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferUploader.drawWithShader(BUFFER.end());
	}

	static void discard() {
		if (!building) return;
		building = false;
		BUFFER.end().release();
	}

	/** Each client tick (after QiSenseClientHandler's): the treasures in sight breathe out their qi. */
	public static void tick(Minecraft mc) {
		if (mc.level == null || !QiSenseClientHandler.isActive()) {
			SOURCES.clear();
			return;
		}
		if (mc.isPaused()) return;
		long now = mc.level.getGameTime();
		for (Iterator<Source> it = SOURCES.values().iterator(); it.hasNext(); ) {
			Source source = it.next();
			if (now - source.seenAt() > FORGET_TICKS || now < source.seenAt()) {
				it.remove();
				continue;
			}
			breathe(source);
		}
	}

	private static void breathe(Source source) {
		FruitAura.Tier tier = FruitAura.of(source.years());
		int rank = tier.ordinal(); // 0 (under 100 years) to 5 (10,000)
		if (RANDOM.nextFloat() >= 0.04f + 0.06f * rank) return;
		QiElement element = rank >= FruitAura.Tier.HEAVENLY.ordinal() && RANDOM.nextInt(3) == 0 ? QiElement.METAL : QiElement.WOOD;
		Vec3 c = source.centre();
		QiSenseClientHandler.emit(element,
				c.x + (RANDOM.nextDouble() - 0.5) * 0.5, c.y + (RANDOM.nextDouble() - 0.5) * 0.3, c.z + (RANDOM.nextDouble() - 0.5) * 0.5,
				(RANDOM.nextDouble() - 0.5) * 0.012, 0.008 + RANDOM.nextDouble() * 0.01, (RANDOM.nextDouble() - 0.5) * 0.012,
				0.05f + 0.008f * rank + RANDOM.nextFloat() * 0.02f, 50 + RANDOM.nextInt(40), null);
	}

	/**
	 * A pedestal at {@code fruit}, its treasure {@code years} old, feeding {@code meditator}: its qi rises off it and is drawn
	 * into them. Called each tick for each such link; older treasures send more.
	 */
	public static void feed(Vec3 fruit, int years, java.util.UUID meditator, long time, int lane) {
		int rank = FruitAura.of(years).ordinal();
		if ((time + lane * 3) % Math.max(2, 7 - rank) != 0) return;
		QiElement element = rank >= FruitAura.Tier.HEAVENLY.ordinal() && RANDOM.nextInt(3) == 0 ? QiElement.METAL : QiElement.WOOD;
		QiSenseClientHandler.emit(element, fruit.x + (RANDOM.nextDouble() - 0.5) * 0.3, fruit.y, fruit.z + (RANDOM.nextDouble() - 0.5) * 0.3,
				0, 0.12, 0, 0.06f + 0.008f * rank, 140, meditator);
	}

	/** Disconnect. */
	public static void clear() {
		SOURCES.clear();
		discard();
	}

	private QiSenseTreasures() {}
}
