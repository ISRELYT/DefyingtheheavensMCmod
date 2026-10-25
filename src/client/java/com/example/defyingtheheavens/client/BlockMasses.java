package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.SpellConstruct;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The great spells made of the world's own blocks (SpellConstruct Form BLOCKS; the user's ask, 2026-10-25: "the earth move falling continent
 * looks dumb", "the Glacier of the first age ... should just carve through slowly", and "you don't need to use GeckoLib as much"): a land torn
 * up whole, its meadows and woods, its snow-capped peaks and its lake on it and its bedrock hanging jagged under it; a wall of the first age's
 * ice, ranks of seracs climbing toward its back, blue ice in its depths and snow on its shoulders. Each is laid out block by block (the same
 * every time for its seed, on every watcher's screen alike), meshed once as the game meshes its chunks (off the render thread, their faces
 * against one another culled, their shading and the land's colours as a plain's), and drawn every frame from the mesh, turned and moved as its
 * spell moves it. The glacier's face is a mesh of its own, that calves forward ({@code calve}); the land, once it strikes ({@code impact}),
 * is driven down into what it smashed.
 */
final class BlockMasses {
	/** Each construct's mass, by its entity id: its parts' meshes (built a while after it is first seen), what its spell adds to its drawing. */
	private static final Map<Integer, Mass> MASSES = new HashMap<>();

	private BlockMasses() {}

	/** One mass: its parts, each a mesh per render type (solid, cutout...); its lake's water, drawn apart. */
	static final class Mass {
		CompletableFuture<Built> building;
		Map<Integer, Map<RenderType, VertexBuffer>> parts;
		List<float[]> water = List.of();
		boolean failed;

		void close() {
			if (parts != null) for (Map<RenderType, VertexBuffer> part : parts.values()) for (VertexBuffer vb : part.values()) vb.close();
			parts = null;
		}
	}

	/** A mass laid out and meshed off the render thread, its meshes still to be put on the card: per part, per render type. */
	private record Built(Map<Integer, Map<RenderType, BufferBuilder.RenderedBuffer>> parts, List<float[]> water) {}

	/** Every client tick: the masses whose constructs are gone let go, their meshes freed. */
	static void tick(Minecraft mc) {
		if (MASSES.isEmpty()) return;
		MASSES.entrySet().removeIf(e -> {
			boolean gone = mc.level == null || !(mc.level.getEntity(e.getKey()) instanceof SpellConstruct c) || c.isRemoved();
			if (gone) e.getValue().close();
			return gone;
		});
	}

	static void clear() {
		for (Mass mass : MASSES.values()) mass.close();
		MASSES.clear();
	}

	/** Draws {@code c} (the pose at its place, its renderer's), once its mesh is made. */
	static void render(SpellConstruct c, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
		Mass mass = MASSES.computeIfAbsent(c.getId(), id -> {
			Mass m = new Mass();
			SpellConstruct.Kind kind = c.kind();
			float size = c.size(), value = c.value();
			long seed = c.tint();
			m.building = CompletableFuture.supplyAsync(() -> build(kind, size, value, seed), Util.backgroundExecutor());
			return m;
		});
		if (mass.failed) return;
		if (mass.parts == null) {
			if (mass.building == null || !mass.building.isDone()) return;
			try {
				Built built = mass.building.join();
				mass.parts = upload(built);
				mass.water = built.water();
			} catch (RuntimeException e) {
				DefyingTheHeavens.LOGGER.error("A spell's mass of blocks couldn't be made", e);
				mass.failed = true;
				return;
			}
			mass.building = null;
		}
		float age = c.tickCount + partial;
		float yaw = c.drawnYaw(partial), pitch = c.drawnPitch(partial);
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-yaw));
		switch (c.kind()) {
			case CONTINENT_MASS -> continent(c, mass, partial, age, pitch, pose, buffers);
			case GLACIER_MASS -> glacier(c, mass, partial, age, pose, buffers);
			case BODHI_TREE -> bodhi(c, mass, partial, age, pose, buffers);
			default -> EarthMasses.draw(c, mass, partial, age, pose, buffers);
		}
		pose.popPose();
	}

	/**
	 * The bodhi tree: bursting up out of the land as it grows (swelling up from its foot, past its full height and settling back), stirring a
	 * little in a wind of its own; its seven treasures hanging glowing on threads of light under its boughs, each gone as it falls (its value:
	 * how many have); shedding ({@code shed}), it sinks back into the land it rose from, shrinking as it goes.
	 */
	private static void bodhi(SpellConstruct c, Mass mass, float partial, float age, PoseStack pose, MultiBufferSource buffers) {
		float grow = Mth.clamp(age / com.example.defyingtheheavens.WoodSpells.BODHI_GROWS, 0, 1);
		// Eased out past its full height a little, then settling.
		float s = grow < 1 ? 1 + 2.70158f * (float) Math.pow(grow - 1, 3) + 1.70158f * (float) Math.pow(grow - 1, 2) : 1;
		boolean shedding = "shed".equals(c.phase());
		if (shedding) s *= 1 - SpellRender.ease(c.phaseAge(partial) / com.example.defyingtheheavens.WoodSpells.BODHI_SHEDS);
		if (s <= 0.01f) return;
		pose.pushPose();
		pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * 0.035f) * 0.9f));
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.029f + 1.7f) * 0.9f));
		pose.scale(s, s, s);
		draw(mass, 0, pose);
		// Its treasures, on their threads of light.
		MultiBufferSource spell = SpellBuffers.begin(buffers);
		try {
			Quaternionf facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
			VertexConsumer glow = spell.getBuffer(SpellRender.glow(SpellRender.GLOW));
			VertexConsumer star = spell.getBuffer(SpellRender.glow(SpellRender.STAR));
			int fallen = (int) c.value();
			double height = c.size();
			for (int i = fallen; i < 7; i++) {
				Vec3 at = com.example.defyingtheheavens.WoodSpells.treasureAt(i, height);
				float[] rgb = SpellRender.rgb(com.example.defyingtheheavens.WoodSpells.TREASURES[i]);
				float pulse = 0.85f + 0.15f * Mth.sin(age * 0.2f + i * 1.3f);
				float x = (float) at.x, y = (float) at.y + Mth.sin(age * 0.07f + i) * 0.25f, z = (float) at.z;
				SpellRender.billboard(glow, pose, facing, x, y, z, 4.2f * pulse, 0, rgb, 0.55f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				SpellRender.billboard(glow, pose, facing, x, y, z, 1.6f, 0, new float[] {1, 1, 1}, 0.9f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				SpellRender.billboard(star, pose, facing, x, y, z, 3.0f * pulse, age * 0.02f + i, rgb, 0.8f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				// Its thread up to the bough over it.
				for (int k = 0; k < 6; k++) {
					SpellRender.billboard(glow, pose, facing, x, y + 0.6f + k * 0.45f, z, 0.35f, 0, rgb, 0.5f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				}
			}
		} finally {
			SpellBuffers.end();
		}
		pose.popPose();
	}

	// --- Drawing ---

	/** The falling land: tumbling slowly as it comes down; struck, driven down into the land it smashed. */
	private static void continent(SpellConstruct c, Mass mass, float partial, float age, float tilt, PoseStack pose, MultiBufferSource buffers) {
		boolean struck = "impact".equals(c.phase());
		float sunk = struck ? c.phaseAge(partial) : 0;
		pose.pushPose();
		if (struck) pose.translate(0, -Math.min(sunk * 0.55f + sunk * sunk * 0.01f, c.size() * 1.2f), 0);
		// Its tumble: a slow roll and pitch about its heart as it comes, eased out as it strikes.
		float wobble = struck ? Math.max(0, 1 - sunk / 8) : 1;
		pose.mulPose(Axis.XP.rotationDegrees(tilt + Mth.sin(age * 0.045f) * 6 * wobble));
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.037f + 1.3f) * 5 * wobble));
		draw(mass, 0, pose);
		water(mass, pose, buffers, age);
		pose.popPose();
	}

	/**
	 * The glacier: its body as it grinds on; its face, once it calves ({@code calve}), tipping forward off its foot and falling flat before
	 * it. Mist pours off its foot and its face as it goes, the cold of the first age.
	 */
	private static void glacier(SpellConstruct c, Mass mass, float partial, float age, PoseStack pose, MultiBufferSource buffers) {
		float half = c.size(), depth = c.value();
		// It heaves up out of the land as it rises (its first second), then grinds on.
		float risen = SpellRender.ease(age / 20f);
		pose.pushPose();
		pose.translate(0, -(1 - risen) * 18, 0);
		draw(mass, 0, pose);
		boolean calving = "calve".equals(c.phase());
		float tip = calving ? SpellRender.ease(c.phaseAge(partial) / 16f) : 0;
		pose.pushPose();
		if (tip > 0) {
			// Tipped forward about its foot's front edge, and falling as it goes over.
			pose.translate(0, -4 * tip * tip, 0);
			pose.mulPose(Axis.XP.rotationDegrees(85 * tip * tip));
		}
		draw(mass, 1, pose);
		pose.popPose();
		pose.popPose();
		mist(c, pose, buffers, age, half, depth, risen, tip, c.drawnYaw(partial));
	}

	/** The cold mist rolling off the glacier's foot and face, low and slow, brighter in front. */
	private static void mist(SpellConstruct c, PoseStack pose, MultiBufferSource buffers, float age, float half, float depth, float risen, float tip, float yaw) {
		// Turned to face the eye within the glacier's own turned space: its turn undone, then the camera's.
		Quaternionf facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
		Quaternionf local = new Quaternionf(facing).premul(Axis.YP.rotationDegrees(yaw));
		MultiBufferSource spell = SpellBuffers.begin(buffers);
		try {
			VertexConsumer mist = spell.getBuffer(SpellRender.clear(SpellRender.MIST));
			VertexConsumer glow = spell.getBuffer(SpellRender.glow(SpellRender.MIST));
			long seed = c.getId();
			float fade = Mth.clamp(Math.min(age / 10f, (c.life() - age) / 10f), 0, 1) * risen;
			for (int i = 0; i < 46; i++) {
				float u = SpellRender.hash(seed * 31 + i), v = SpellRender.hash(seed * 31 + i + 500);
				float drift = (age * (0.012f + 0.01f * v) + u) % 1;
				float x, z;
				if (i < 30) {
					// Before its face, rolled forward along the ground.
					x = (u * 2 - 1) * (half + 2);
					z = 1.5f + drift * 9;
				} else {
					// Along its sides and foot.
					x = (i % 2 == 0 ? 1 : -1) * (half + 1.5f + drift * 4);
					z = -v * depth;
				}
				float y = 0.6f + drift * 2.2f + Mth.sin(age * 0.05f + i) * 0.3f;
				float size = 5.5f + 4 * v + drift * 5;
				float a = fade * (1 - drift) * (i < 30 ? 0.42f : 0.3f) * (1 - tip * 0.5f);
				SpellRender.billboard(mist, pose, local, x, y, z, size, age * 0.004f * (i % 2 == 0 ? 1 : -1) + u * 6, new float[] {0.86f, 0.94f, 1.0f}, a, 0, 0, 1, 1,
						SpellRender.FULL_BRIGHT);
				if (i % 3 == 0) SpellRender.billboard(glow, pose, local, x, y, z, size * 0.7f, u * 6, new float[] {0.55f, 0.82f, 1.0f}, a * 0.35f, 0, 0, 1, 1,
						SpellRender.FULL_BRIGHT);
			}
		} finally {
			SpellBuffers.end();
		}
	}

	/** Part {@code part}'s meshes drawn at the pose (each in its render type's own state, as the game draws its chunks). */
	static void draw(Mass mass, int part, PoseStack pose) {
		Map<RenderType, VertexBuffer> meshes = mass.parts.get(part);
		if (meshes == null) return;
		Matrix4f modelView = pose.last().pose();
		for (Map.Entry<RenderType, VertexBuffer> e : meshes.entrySet()) {
			RenderType type = e.getKey();
			type.setupRenderState();
			ShaderInstance shader = RenderSystem.getShader();
			if (shader != null) {
				if (shader.CHUNK_OFFSET != null) shader.CHUNK_OFFSET.set(0.0f, 0.0f, 0.0f);
				VertexBuffer vb = e.getValue();
				vb.bind();
				vb.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), shader);
				VertexBuffer.unbind();
			}
			type.clearRenderState();
		}
	}

	/** The land's lake: still water lying in its hollow (each cell a quad of the water's own picture, its colour a plain's). */
	private static void water(Mass mass, PoseStack pose, MultiBufferSource buffers, float age) {
		if (mass.water.isEmpty()) return;
		TextureAtlasSprite still = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(new ResourceLocation("minecraft", "block/water_still"));
		VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
		PoseStack.Pose p = pose.last();
		float[] rgb = SpellRender.rgb(0x3F76E4);
		for (float[] cell : mass.water) {
			float x = cell[0], y = cell[1], z = cell[2];
			SpellRender.vertex(vc, p, x, y, z, rgb[0], rgb[1], rgb[2], 0.78f, still.getU0(), still.getV0(), 0xF000F0, 0, 1, 0);
			SpellRender.vertex(vc, p, x, y, z + 1, rgb[0], rgb[1], rgb[2], 0.78f, still.getU0(), still.getV1(), 0xF000F0, 0, 1, 0);
			SpellRender.vertex(vc, p, x + 1, y, z + 1, rgb[0], rgb[1], rgb[2], 0.78f, still.getU1(), still.getV1(), 0xF000F0, 0, 1, 0);
			SpellRender.vertex(vc, p, x + 1, y, z, rgb[0], rgb[1], rgb[2], 0.78f, still.getU1(), still.getV0(), 0xF000F0, 0, 1, 0);
		}
	}

	private static Map<Integer, Map<RenderType, VertexBuffer>> upload(Built built) {
		Map<Integer, Map<RenderType, VertexBuffer>> parts = new HashMap<>();
		for (Map.Entry<Integer, Map<RenderType, BufferBuilder.RenderedBuffer>> part : built.parts().entrySet()) {
			Map<RenderType, VertexBuffer> meshes = new LinkedHashMap<>();
			for (Map.Entry<RenderType, BufferBuilder.RenderedBuffer> e : part.getValue().entrySet()) {
				VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
				vb.bind();
				vb.upload(e.getValue());
				VertexBuffer.unbind();
				meshes.put(e.getKey(), vb);
			}
			parts.put(part.getKey(), meshes);
		}
		return parts;
	}

	// --- Laying out and meshing ---

	/** A mass of blocks laid out: each block by its place (packed), and which part it belongs to (0 its body, 1 its face). */
	static final class Layout implements BlockAndTintGetter {
		final Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
		final Map<Long, Integer> part = new HashMap<>();
		final List<float[]> water = new ArrayList<>();

		void set(int x, int y, int z, BlockState state, int inPart) {
			long key = BlockPos.asLong(x, y, z);
			blocks.put(key, state);
			if (inPart != 0) part.put(key, inPart);
			else part.remove(key);
		}

		boolean has(int x, int y, int z) {
			return blocks.containsKey(BlockPos.asLong(x, y, z));
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			BlockState state = blocks.get(pos.asLong());
			return state == null ? Blocks.AIR.defaultBlockState() : state;
		}

		@Override
		public FluidState getFluidState(BlockPos pos) {
			return getBlockState(pos).getFluidState();
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos pos) {
			return null;
		}

		@Override
		public int getHeight() {
			return 1024;
		}

		@Override
		public int getMinBuildHeight() {
			return -512;
		}

		@Override
		public float getShade(Direction direction, boolean shade) {
			if (!shade) return 1.0f;
			return switch (direction) {
				case DOWN -> 0.5f;
				case UP -> 1.0f;
				case NORTH, SOUTH -> 0.8f;
				default -> 0.6f;
			};
		}

		@Override
		public LevelLightEngine getLightEngine() {
			return null; // never asked: its light is the open sky's (getBrightness)
		}

		@Override
		public int getBrightness(LightLayer layer, BlockPos pos) {
			return layer == LightLayer.SKY ? 15 : 0;
		}

		@Override
		public int getRawBrightness(BlockPos pos, int darkening) {
			return 15 - darkening;
		}

		@Override
		public boolean canSeeSky(BlockPos pos) {
			return true;
		}

		@Override
		public int getBlockTint(BlockPos pos, ColorResolver resolver) {
			if (resolver == BiomeColors.GRASS_COLOR_RESOLVER) return 0x79C05A;
			if (resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER) return 0x59AE30;
			return 0x3F76E4;
		}
	}

	/** {@code kind}'s mass, {@code size} and {@code value} its measures, {@code seed} its own, laid out and meshed. */
	private static Built build(SpellConstruct.Kind kind, float size, float value, long seed) {
		Layout layout = new Layout();
		switch (kind) {
			case CONTINENT_MASS -> continentLayout(layout, Math.max(8, size), seed);
			case GLACIER_MASS -> glacierLayout(layout, Math.max(6, size), Math.max(6, value), seed);
			case BODHI_TREE -> bodhiLayout(layout, Math.max(10, size), seed);
			default -> EarthMasses.layout(kind, layout, size, value, seed);
		}
		return mesh(layout, seed);
	}

	/** Only the blocks with a face open to the air (or a see-through neighbour) are meshed: the rest can never be seen. */
	private static boolean shows(Layout layout, int x, int y, int z) {
		for (Direction d : Direction.values()) {
			BlockState next = layout.blocks.get(BlockPos.asLong(x + d.getStepX(), y + d.getStepY(), z + d.getStepZ()));
			if (next == null || !next.canOcclude()) return true;
		}
		return false;
	}

	private static Built mesh(Layout layout, long seed) {
		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		Map<Integer, Map<RenderType, BufferBuilder>> builders = new HashMap<>();
		PoseStack pose = new PoseStack();
		RandomSource random = RandomSource.create(seed);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (Long2ObjectOpenHashMap.Entry<BlockState> e : layout.blocks.long2ObjectEntrySet()) {
			long key = e.getLongKey();
			int x = BlockPos.getX(key), y = BlockPos.getY(key), z = BlockPos.getZ(key);
			BlockState state = e.getValue();
			if (state.canOcclude() && !shows(layout, x, y, z)) continue;
			int part = layout.part.getOrDefault(key, 0);
			RenderType type = ItemBlockRenderTypes.getChunkRenderType(state);
			BufferBuilder builder = builders.computeIfAbsent(part, k -> new LinkedHashMap<>()).computeIfAbsent(type, t -> {
				BufferBuilder b = new BufferBuilder(t.bufferSize());
				b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
				return b;
			});
			at.set(x, y, z);
			pose.pushPose();
			pose.translate(x, y, z);
			random.setSeed(key ^ seed);
			dispatcher.renderBatched(state, at, layout, pose, builder, true, random);
			pose.popPose();
		}
		Map<Integer, Map<RenderType, BufferBuilder.RenderedBuffer>> parts = new HashMap<>();
		for (Map.Entry<Integer, Map<RenderType, BufferBuilder>> part : builders.entrySet()) {
			Map<RenderType, BufferBuilder.RenderedBuffer> meshes = new LinkedHashMap<>();
			for (Map.Entry<RenderType, BufferBuilder> e : part.getValue().entrySet()) {
				BufferBuilder.RenderedBuffer rendered = e.getValue().endOrDiscardIfEmpty();
				if (rendered != null) meshes.put(e.getKey(), rendered);
			}
			parts.put(part.getKey(), meshes);
		}
		return new Built(parts, layout.water);
	}

	/** A steady number in [0, 1) for a seed and three whole numbers. */
	static float noise(long seed, int a, int b, int c) {
		return SpellRender.hash(seed * 0x9E3779B97F4A7C15L + a * 73856093L + b * 19349663L + c * 83492791L);
	}

	/** Smooth value noise over a grid of {@code cell} blocks: in [0, 1), the same for the same seed and place. */
	static float smooth(long seed, float x, float z, float cell) {
		float gx = x / cell, gz = z / cell;
		int x0 = Mth.floor(gx), z0 = Mth.floor(gz);
		float fx = gx - x0, fz = gz - z0;
		fx = fx * fx * (3 - 2 * fx);
		fz = fz * fz * (3 - 2 * fz);
		float a = noise(seed, x0, 0, z0), b = noise(seed, x0 + 1, 0, z0), c = noise(seed, x0, 0, z0 + 1), d = noise(seed, x0 + 1, 0, z0 + 1);
		return Mth.lerp(fz, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
	}

	/**
	 * Falling Continent's land (CONTINENT_MASS: {@code radius} its round), its heart at the middle of its meadows' level: its outline ragged;
	 * meadows rolling over it, two or three mountains rising out of them (their tops bare stone and snow, their flanks stone where steep), a
	 * lake in a hollow, woods of oak and spruce on the grass; under it earth, then stone of every kind (andesite, granite, diorite, tuff, a
	 * seam of ore here and there, deepslate at the bottom), hanging in jagged teeth below, deepest under its heart, roots and dirt clinging to
	 * the ragged rim.
	 */
	private static void continentLayout(Layout layout, float radius, long seed) {
		int r = Mth.ceil(radius);
		// Its peaks: where, how wide, how high.
		int peaks = 2 + (int) (noise(seed, 1, 2, 3) * 2);
		float[][] peak = new float[peaks][4];
		for (int i = 0; i < peaks; i++) {
			double a = Math.PI * 2 * noise(seed, i, 7, 1), d = radius * (0.15 + 0.4 * noise(seed, i, 7, 2));
			peak[i] = new float[] {(float) (Math.cos(a) * d), (float) (Math.sin(a) * d), radius * (0.22f + 0.1f * noise(seed, i, 7, 3)),
					radius * (0.42f + 0.25f * noise(seed, i, 7, 4))};
		}
		// Its lake: away from the peaks.
		double la = Math.PI * 2 * noise(seed, 9, 9, 9);
		float lakeX = (float) (Math.cos(la) * radius * 0.45), lakeZ = (float) (Math.sin(la) * radius * 0.45), lakeR = radius * 0.16f;
		float tallest = 0;
		for (float[] p : peak) tallest = Math.max(tallest, p[3]);
		float snowLine = tallest * 0.62f;
		int[][] tops = new int[r * 2 + 1][r * 2 + 1];
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				double angle = Math.atan2(z, x);
				float rim = radius * (0.8f + 0.2f * smooth(seed + 11, (float) Math.cos(angle) * 8, (float) Math.sin(angle) * 8, 3));
				float d = Mth.sqrt(x * x + z * z);
				tops[x + r][z + r] = Integer.MIN_VALUE;
				if (d > rim) continue;
				float e = d / rim;
				// Its meadows' roll, and its peaks.
				float h = 1.5f + 3.5f * smooth(seed + 21, x, z, 9) + 1.5f * smooth(seed + 22, x, z, 4);
				for (float[] p : peak) {
					float pd = Mth.sqrt((x - p[0]) * (x - p[0]) + (z - p[1]) * (z - p[1]));
					float f = Math.max(0, 1 - pd / p[2]);
					h += p[3] * (float) Math.pow(f, 1.5) * (0.85f + 0.3f * smooth(seed + 23, x, z, 3));
				}
				h *= 1 - (float) Math.pow(e, 5);
				h -= 3 * (float) Math.pow(e, 6);
				// The lake's hollow.
				float ld = Mth.sqrt((x - lakeX) * (x - lakeX) + (z - lakeZ) * (z - lakeZ));
				boolean lake = ld < lakeR;
				int top = Mth.floor(h);
				if (lake) top = Math.min(top, 1) - (ld < lakeR * 0.6f ? 2 : 1);
				// Its hanging bedrock: deepest under its heart, in jagged teeth.
				float teeth = smooth(seed + 31, x, z, 3.5f), tooth = noise(seed, x >> 1, 5, z >> 1);
				int bottom = -Mth.floor(4 + radius * 0.5f * (float) Math.pow(1 - e, 0.8) + radius * 0.4f * (1 - e) * teeth * teeth + (tooth < 0.08f ? radius * 0.25f * (1 - e) : 0));
				tops[x + r][z + r] = top;
				for (int y = bottom; y <= top; y++) layout.set(x, y, z, rock(seed, x, y, z, bottom, top, tallest), 0);
				if (lake && top < 1) layout.water.add(new float[] {x, 0.88f, z}); // its still surface
			}
		}
		// Its surfaces: grass on the meadows, stone on the steeps, snow over the heights; earth under the grass; roots and dirt under its rim.
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				int top = tops[x + r][z + r];
				if (top == Integer.MIN_VALUE) continue;
				int steep = 0;
				for (int[] n : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
					int nx = x + n[0] + r, nz = z + n[1] + r;
					int other = nx < 0 || nz < 0 || nx > r * 2 || nz > r * 2 ? Integer.MIN_VALUE : tops[nx][nz];
					steep = Math.max(steep, other == Integer.MIN_VALUE ? 4 : Math.abs(other - top));
				}
				float ld = Mth.sqrt((x - lakeX) * (x - lakeX) + (z - lakeZ) * (z - lakeZ));
				if (ld < lakeR) {
					layout.set(x, top, z, (noise(seed, x, top, z) < 0.5f ? Blocks.SAND : Blocks.GRAVEL).defaultBlockState(), 0);
					continue;
				}
				if (top > snowLine) {
					layout.set(x, top, z, steep > 2 && noise(seed, x, 3, z) < 0.5f ? Blocks.STONE.defaultBlockState() : Blocks.SNOW_BLOCK.defaultBlockState(), 0);
					if (steep <= 1 && noise(seed, x, 4, z) < 0.6f) layout.set(x, top + 1, z, Blocks.SNOW.defaultBlockState(), 0);
				} else if (steep > 2) {
					layout.set(x, top, z, noise(seed, x, 5, z) < 0.3f ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState(), 0);
				} else {
					layout.set(x, top, z, Blocks.GRASS_BLOCK.defaultBlockState(), 0);
					for (int y = top - 1; y >= top - 3; y--) if (layout.has(x, y, z)) layout.set(x, y, z, Blocks.DIRT.defaultBlockState(), 0);
					float plant = noise(seed, x, 6, z);
					if (plant < 0.18f) layout.set(x, top + 1, z, Blocks.GRASS.defaultBlockState(), 0);
					else if (plant < 0.205f) layout.set(x, top + 1, z, (plant < 0.193f ? Blocks.POPPY : Blocks.DANDELION).defaultBlockState(), 0);
				}
			}
		}
		// Its woods.
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				int top = tops[x + r][z + r];
				if (top == Integer.MIN_VALUE || noise(seed, x, 8, z) > 0.022f) continue;
				if (!layout.getBlockState(new BlockPos(x, top, z)).is(Blocks.GRASS_BLOCK)) continue;
				boolean spruce = top > snowLine * 0.5f || noise(seed, x, 10, z) < 0.3f;
				tree(layout, seed, x, top + 1, z, spruce);
			}
		}
	}

	/** The stone of the land's depths at a place: earth near its top, stone of every kind below, a seam of ore here and there, deepslate low down. */
	static BlockState rock(long seed, int x, int y, int z, int bottom, int top, float tallest) {
		float n = noise(seed, x >> 1, y >> 1, z >> 1), m = smooth(seed + 41, x + y * 0.7f, z - y * 0.5f, 5);
		if (y > top - 4 && top < tallest * 0.3f) return Blocks.DIRT.defaultBlockState();
		if (y < bottom + (top - bottom) * 0.3f) return n < 0.15f ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
		if (n < 0.012f) return Blocks.COAL_ORE.defaultBlockState();
		if (n < 0.02f) return Blocks.IRON_ORE.defaultBlockState();
		if (n < 0.024f) return Blocks.COPPER_ORE.defaultBlockState();
		if (m < 0.2f) return Blocks.ANDESITE.defaultBlockState();
		if (m > 0.86f) return Blocks.GRANITE.defaultBlockState();
		if (m > 0.8f) return Blocks.DIORITE.defaultBlockState();
		if (n > 0.93f) return Blocks.TUFF.defaultBlockState();
		if (n > 0.88f) return Blocks.GRAVEL.defaultBlockState();
		return Blocks.STONE.defaultBlockState();
	}

	/** A tree at {@code (x, y, z)} (its trunk's foot): an oak's round crown, or a spruce's cone. */
	static void tree(Layout layout, long seed, int x, int y, int z, boolean spruce) {
		int height = (spruce ? 6 : 4) + (int) (noise(seed, x, 11, z) * 3);
		BlockState log = (spruce ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG).defaultBlockState();
		BlockState leaves = (spruce ? Blocks.SPRUCE_LEAVES : Blocks.OAK_LEAVES).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		for (int i = 0; i < height; i++) layout.set(x, y + i, z, log, 0);
		if (spruce) {
			for (int i = 2; i <= height + 1; i++) {
				int rr = Math.max(0, (height + 1 - i) / 2 + (i % 2));
				for (int dx = -rr; dx <= rr; dx++) for (int dz = -rr; dz <= rr; dz++) {
					if (Math.abs(dx) + Math.abs(dz) > rr + 1 || layout.has(x + dx, y + i, z + dz)) continue;
					layout.set(x + dx, y + i, z + dz, leaves, 0);
				}
			}
			layout.set(x, y + height + 1, z, leaves, 0);
		} else {
			for (int dx = -2; dx <= 2; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -2; dz <= 2; dz++) {
				if (dx * dx + dz * dz + dy * dy * 1.6f > 6.5f || layout.has(x + dx, y + height - 1 + dy, z + dz)) continue;
				if (noise(seed, x + dx, y + dy, z + dz) < 0.1f) continue;
				layout.set(x + dx, y + height - 1 + dy, z + dz, leaves, 0);
			}
		}
	}

	/**
	 * Glacier of the First Age's wall (GLACIER_MASS: {@code half} the half of its width, {@code depth} how far back it runs), its face toward +z
	 * at 0 and its foot at 0: a wall of packed ice banded with blue ice in strata, its face ragged and leaning, ranks of seracs (spires of blue
	 * ice) climbing taller toward its back, snow lying on its shoulders, moraine (gravel and stone) ground into its foot, and running on down
	 * under its foot, so it is never seen to float over a hollow. Its face (the first four blocks of it) is a part of its own (1): it calves.
	 */
	private static void glacierLayout(Layout layout, float half, float depth, long seed) {
		int w = Mth.ceil(half), d = Mth.ceil(depth);
		for (int x = -w; x <= w; x++) {
			// Its face, ragged: each column standing a little forward or back.
			float faceAt = -2.2f * smooth(seed + 3, x, 0, 3) - (noise(seed, x, 1, 1) < 0.2f ? 1 : 0);
			float side = Math.min(1, (w - Math.abs(x)) / 5.0f);
			for (int z = -d; z <= 0; z++) {
				if (z > faceAt) continue;
				float back = -z / (float) d;
				// Height: taller toward its back, seracs standing up out of it, tapering to its sides.
				float h = 9 + 13 * back + 4 * smooth(seed + 5, x, z, 6);
				float serac = smooth(seed + 7, x, z, 2.5f);
				if (serac > 0.62f) h += (serac - 0.62f) * 38 * (0.4f + back);
				h *= 0.35f + 0.65f * side;
				// Its face leans back as it rises, and its first rows stand lower (they calve).
				float fromFace = faceAt - z;
				if (fromFace < 3) h = Math.min(h, 8 + fromFace * 2.5f + 3 * serac);
				int top = Mth.floor(h);
				boolean face = fromFace < 4;
				for (int y = -10; y <= top; y++) {
					float band = smooth(seed + 9, x * 0.25f, y, 3.0f) + 0.2f * noise(seed, x, y, z);
					BlockState ice;
					if (y < 1 && noise(seed, x, y, z) < 0.35f) ice = (noise(seed, x, y + 50, z) < 0.5f ? Blocks.GRAVEL : Blocks.COBBLESTONE).defaultBlockState();
					else if (band > 0.72f || serac > 0.7f && y > top - 6) ice = Blocks.BLUE_ICE.defaultBlockState();
					else ice = Blocks.PACKED_ICE.defaultBlockState();
					layout.set(x, y, z, ice, face ? 1 : 0);
				}
				// Snow on its shoulders where it lies flat.
				if (serac < 0.55f && !face && noise(seed, x, top, z) < 0.7f) layout.set(x, top + 1, z, Blocks.SNOW_BLOCK.defaultBlockState(), 0);
				else if (!face && noise(seed, x, top, z) < 0.35f) layout.set(x, top + 1, z, Blocks.SNOW.defaultBlockState(), 0);
			}
		}
	}

	/** Every block within {@code r0} to {@code r1} (from {@code a}'s end to {@code b}'s) of the line from {@code a} to {@code b} made {@code state}. */
	static void sweep(Layout layout, double ax, double ay, double az, double bx, double by, double bz, double r0, double r1, BlockState state) {
		sweep(layout, ax, ay, az, bx, by, bz, r0, r1, (x, y, z) -> state, 0);
	}

	/** What a block of a mass is, by its place. */
	interface Stuff {
		BlockState at(int x, int y, int z);
	}

	/** {@link #sweep(Layout, double, double, double, double, double, double, double, double, BlockState)}, of {@code stuff}, in part {@code part}. */
	static void sweep(Layout layout, double ax, double ay, double az, double bx, double by, double bz, double r0, double r1, Stuff stuff, int part) {
		double length = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
		int steps = Math.max(1, (int) Math.ceil(length / 0.5));
		for (int i = 0; i <= steps; i++) {
			double f = i / (double) steps, r = Mth.lerp(f, r0, r1);
			double cx = Mth.lerp(f, ax, bx), cy = Mth.lerp(f, ay, by), cz = Mth.lerp(f, az, bz);
			int ri = Mth.ceil(r);
			for (int dx = -ri; dx <= ri; dx++) for (int dy = -ri; dy <= ri; dy++) for (int dz = -ri; dz <= ri; dz++) {
				int x = Mth.floor(cx) + dx, y = Mth.floor(cy) + dy, z = Mth.floor(cz) + dz;
				double ex = x + 0.5 - cx, ey = y + 0.5 - cy, ez = z + 0.5 - cz;
				if (ex * ex + ey * ey + ez * ez <= r * r) layout.set(x, y, z, stuff.at(x, y, z), part);
			}
		}
	}

	/** A leafy crown's mass at {@code (cx, cy, cz)}: its outer shell of leaves, flattened a little, some flowering, holed here and there. */
	private static void crown(Layout layout, long seed, double cx, double cy, double cz, double r) {
		int ri = Mth.ceil(r);
		BlockState jungle = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		BlockState azalea = Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		BlockState flowering = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		for (int dx = -ri; dx <= ri; dx++) for (int dy = -ri; dy <= ri; dy++) for (int dz = -ri; dz <= ri; dz++) {
			int x = Mth.floor(cx) + dx, y = Mth.floor(cy) + dy, z = Mth.floor(cz) + dz;
			double ex = x + 0.5 - cx, ey = (y + 0.5 - cy) * 1.45, ez = z + 0.5 - cz;
			double d = Math.sqrt(ex * ex + ey * ey + ez * ez);
			float n = noise(seed, x, y, z);
			if (d > r * (0.85 + 0.15 * smooth(seed + 51, x + y * 0.5f, z, 3)) || d < r - 2.2 || n < 0.1f || layout.has(x, y, z)) continue;
			layout.set(x, y, z, n < 0.62f ? jungle : n < 0.8f ? azalea : flowering, 0);
		}
	}

	/**
	 * Seven-Treasure Bodhi Branch's tree (BODHI_TREE: {@code height} tall, its foot at 0): a sacred fig, its trunk fluted and buttressed, roots
	 * clutching the land round its foot; seven great boughs spreading up and out, one toward each of its treasures' places
	 * (WoodSpells#treasureAt), twigged and crowned in leaves, flowering here and there; a crown over its top; hanging roots dropping from its
	 * boughs.
	 */
	private static void bodhiLayout(Layout layout, float height, long seed) {
		BlockState wood = Blocks.JUNGLE_WOOD.defaultBlockState(), log = Blocks.JUNGLE_LOG.defaultBlockState();
		int top = Math.round(height * 0.62f);
		float base = Math.max(2.2f, height * 0.085f), thin = Math.max(1.2f, height * 0.04f);
		// Its trunk: fluted, leaning a little as it rises.
		for (int y = -2; y <= top; y++) {
			float f = Mth.clamp(y / (float) top, 0, 1);
			float r = Mth.lerp((float) Math.pow(f, 0.8), base, thin);
			float cx = Mth.sin(f * 2.1f + noise(seed, 1, 1, 1) * 6) * 1.3f, cz = Mth.cos(f * 1.7f + noise(seed, 2, 2, 2) * 6) * 1.1f;
			int ri = Mth.ceil(r * 1.25f);
			for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
				double ex = dx + 0.5 - (cx - Mth.floor(cx)), ez = dz + 0.5 - (cz - Mth.floor(cz));
				double angle = Math.atan2(ez, ex);
				double rr = r * (1 + 0.18 * Math.sin(angle * 5 + f * 3));
				if (ex * ex + ez * ez <= rr * rr) layout.set(Mth.floor(cx) + dx, y, Mth.floor(cz) + dz, wood, 0);
			}
		}
		// Its buttress roots, clutching the land round its foot.
		for (int k = 0; k < 6; k++) {
			double a = Math.PI * 2 * k / 6 + noise(seed, k, 3, 3) * 0.6, out = base + 4 + 3 * noise(seed, k, 4, 4);
			sweep(layout, Math.cos(a) * base * 0.6, 2.5, Math.sin(a) * base * 0.6, Math.cos(a) * out, -1.5, Math.sin(a) * out, 1.4, 0.7, wood);
		}
		// Its seven boughs, each spreading toward a treasure's place, twigged, crowned in leaves; hanging roots dropped from them.
		for (int i = 0; i < 7; i++) {
			Vec3 hang = com.example.defyingtheheavens.WoodSpells.treasureAt(i, height);
			double from = height * (0.38 + 0.06 * (i % 3));
			double a = Math.atan2(hang.z, hang.x);
			double ex = hang.x * 1.12, ey = hang.y + 2.2, ez = hang.z * 1.12;
			sweep(layout, Math.cos(a) * thin, from, Math.sin(a) * thin, ex, ey, ez, 1.5, 0.75, wood);
			for (int t = 0; t < 2; t++) {
				double ta = a + (t == 0 ? 0.7 : -0.6) + (noise(seed, i, t, 9) - 0.5) * 0.4;
				double mx = Mth.lerp(0.6, Math.cos(a) * thin, ex), my = Mth.lerp(0.6, from, ey), mz = Mth.lerp(0.6, Math.sin(a) * thin, ez);
				double tx = mx + Math.cos(ta) * height * 0.15, ty = my + height * 0.08, tz = mz + Math.sin(ta) * height * 0.15;
				sweep(layout, mx, my, mz, tx, ty, tz, 0.8, 0.5, log);
				crown(layout, seed + i * 31 + t, tx, ty + 1.5, tz, height * 0.12);
			}
			crown(layout, seed + i * 17, ex, ey + 2, ez, height * 0.15);
			// Hanging roots, under the bough's length.
			for (int h = 0; h < 3; h++) {
				double f = 0.35 + 0.2 * h;
				int x = Mth.floor(Mth.lerp(f, Math.cos(a) * thin, ex)), z = Mth.floor(Mth.lerp(f, Math.sin(a) * thin, ez));
				int y = Mth.floor(Mth.lerp(f, from, ey)) - 1;
				int drop = 2 + (int) (noise(seed, i, h, 11) * 5);
				for (int d = 0; d < drop; d++) if (!layout.has(x, y - d, z)) layout.set(x, y - d, z, Blocks.HANGING_ROOTS.defaultBlockState(), 0);
			}
		}
		// Its crown over its top.
		crown(layout, seed + 99, 0, top + height * 0.12, 0, height * 0.2);
	}

	/** The place its land sits under {@code at} on this client (for what is drawn on the ground under it), or null. */
	static Vec3 groundUnder(Vec3 at, double down) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(at, at.subtract(0, down, 0), net.minecraft.world.level.ClipContext.Block.COLLIDER,
				net.minecraft.world.level.ClipContext.Fluid.ANY, (net.minecraft.world.entity.Entity) null));
		return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : hit.getLocation();
	}
}
