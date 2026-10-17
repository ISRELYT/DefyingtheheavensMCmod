package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The metal element's spells (Spell, HeavenSpells): metal is weak to fire and strong against wood (SpellAuras). Every number is the
 * design's; every look its own (the client's spell renderers).
 * <ul>
 *   <li>Qi Refining: Flying Sword (a sword of metal formed in the air and shot), Blade Polish (melee x1.2, a white wisp on the
 *       blade), Steel Hurricane (a dash with the held weapon, spinning, as a trident's riptide).</li>
 *   <li>Foundation Building: Boulder Cleave (a crescent of sword qi that cuts through the ground for 15 blocks), Mineral
 *       Purification (every ore within the reach of one's consciousness drawn out of its rock), Advanced Flying Sword Technique (six
 *       swords formed round you one by one, loosed one by one).</li>
 *   <li>Core Formation: Sword Rain, Golden Bell Shield.</li>
 *   <li>Nascent Soul: Piercing Sword Light, Vajra Body.</li>
 *   <li>Heavenly Being: Mountain Cleave (a slash that cuts through mountains), Sword Domain.</li>
 *   <li>Four Axis: Heaven-Severing Sword, Four Swords Array.</li>
 * </ul>
 */
public final class MetalSpells {
	public static final double SWORD_SPEED = 2.6, SWORD_RANGE = 48;
	public static final float SWORD_DAMAGE = 6;
	public static final int POLISH_TICKS = 30 * 20, HURRICANE_TICKS = 32;
	public static final double HURRICANE_SPEED = 2.6;
	public static final float HURRICANE_DAMAGE = 3;
	public static final double CLEAVE_SPEED = 1.5, CLEAVE_RANGE = 15;
	public static final float CLEAVE_DAMAGE = 14;
	public static final int CLEAVE_WIDTH = 3, CLEAVE_HEIGHT = 2;
	public static final int ORES_MAX = 256;
	public static final int SWORDS = 6, SWORD_FORM_EVERY = 5, SWORD_LOOSE_EVERY = 6;
	public static final float ADVANCED_DAMAGE = 9;
	public static final double RAIN_RANGE = 32, RAIN_RADIUS = 5;
	public static final int RAIN_SWORDS = 12, RAIN_FORM = 15;
	public static final float RAIN_DAMAGE = 6;
	public static final float BELL_HOLD = 40;
	public static final int BELL_TICKS = 10 * 20;
	public static final double LIGHT_RANGE = 64, LIGHT_BORE = 1.6;
	public static final float LIGHT_DAMAGE = 55;
	public static final int VAJRA_TICKS = 12 * 20;
	public static final double MOUNTAIN_SPEED = 4.0, MOUNTAIN_RANGE = 140;
	public static final float MOUNTAIN_DAMAGE = 150;
	public static final int MOUNTAIN_WIDTH = 4, MOUNTAIN_HEIGHT = 16;
	public static final double DOMAIN_RADIUS = 10;
	public static final int DOMAIN_TICKS = 12 * 20;
	public static final float DOMAIN_DAMAGE = 20;
	/**
	 * Heaven-Severing Sword: a level crescent of {@link #SEVER_RADIUS} round {@link #SEVER_SPAN} radians (its tips about 110 blocks
	 * apart, trailing 0.59 of the radius behind its bow), {@link #SEVER_THICK} blocks thick at its middle and a quarter of that at its
	 * tips, its middle {@link #SEVER_LIFT} over the ground it set out from; it flies {@link #SEVER_RANGE} at {@link #SEVER_SPEED} a
	 * tick and cuts only what its blade passes through. The client draws it to the same numbers (SpellProjectileRenderer).
	 */
	public static final double SEVER_RANGE = 240, SEVER_SPEED = 6;
	public static final float SEVER_DAMAGE = 300;
	public static final float SEVER_RADIUS = 60, SEVER_SPAN = 2.3f, SEVER_THICK = 10, SEVER_LIFT = 1.3f;

	/**
	 * The severing slash's way, asked to load (out to its tips) and tick (round its middle) the moment it is cast, and again just
	 * ahead of it as it flies: its 240 blocks run past the usual simulation distance, where it would otherwise stop dead and its cut
	 * with it (land never made before is made meanwhile; the slash waits at its edge until it is). Each lapses on its own.
	 */
	private static final TicketType<ChunkPos> SEVER_TICKET = TicketType.create("dth_sever", java.util.Comparator.comparingLong(ChunkPos::toLong), 300);
	private static final int SEVER_TICKET_RADIUS = 4;

	private static void loadSeverWay(ServerLevel level, Vec3 at) {
		ChunkPos chunk = new ChunkPos(BlockPos.containing(at));
		level.getChunkSource().addRegionTicket(SEVER_TICKET, chunk, SEVER_TICKET_RADIUS, chunk);
	}

	/** How thick the severing blade is (top to bottom) at {@code across} blocks out to the side of its middle (0 past its tips). */
	public static float severThickness(double across) {
		double s = across / SEVER_RADIUS;
		if (Math.abs(s) > Math.sin(SEVER_SPAN / 2)) return 0;
		double a = Math.asin(s);
		return (float) (SEVER_THICK * (0.25 + 0.75 * Math.sin(Math.PI * (a / SEVER_SPAN + 0.5))));
	}

	/** How far behind its bow the severing blade is at {@code across} blocks out to the side (its crescent's curve). */
	public static double severLag(double across) {
		double s = Mth.clamp(across / SEVER_RADIUS, -1, 1);
		return SEVER_RADIUS * (1 - Math.cos(Math.asin(s)));
	}
	public static final double ARRAY_RANGE = 48, ARRAY_HALF = 16;
	public static final int ARRAY_TICKS = 8 * 20;
	public static final float ARRAY_DAMAGE = 80;
	/** Where each of the Four Swords is driven in: a crater this round and deep. */
	public static final double ARRAY_CRATER = 4, ARRAY_CRATER_DEPTH = 7;

	static boolean canCast(ServerPlayer p, Spell spell) {
		return true;
	}

	static void cast(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
		switch (spell) {
			case FLYING_SWORD -> sword(p, spell, new Vec3(0.45, 0.15, 1.2), 8, SWORD_DAMAGE, 0);
			case BLADE_POLISH -> {
				SpellAuras.put(p, SpellAura.BLADE_POLISH, POLISH_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.GRINDSTONE_USE, 1.0f, 1.4f);
				HeavenSpells.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
			}
			case STEEL_HURRICANE -> hurricane(p, spell);
			case BOULDER_CLEAVE -> cleave(p, spell, SpellProjectile.Shape.CLEAVE_SLASH, CLEAVE_SPEED, CLEAVE_RANGE, CLEAVE_DAMAGE, CLEAVE_WIDTH, CLEAVE_HEIGHT, 1.0f);
			case MINERAL_PURIFICATION -> purify(p);
			case ADVANCED_FLYING_SWORD_TECHNIQUE -> advanced(p, spell);
			case SWORD_RAIN -> rain(p, spell);
			case GOLDEN_BELL_SHIELD -> {
				SpellAuras.bell(p, BELL_HOLD, BELL_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.BELL_BLOCK, 2.0f, 0.5f);
				HeavenSpells.sound(level, p.position(), SoundEvents.BELL_RESONATE, 1.5f, 0.8f);
			}
			case PIERCING_SWORD_LIGHT -> light(p, spell);
			case VAJRA_BODY -> {
				SpellAuras.put(p, SpellAura.VAJRA_BODY, VAJRA_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.ARMOR_EQUIP_GOLD, 1.5f, 0.6f);
				HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.2f);
			}
			case MOUNTAIN_CLEAVE -> cleave(p, spell, SpellProjectile.Shape.MOUNTAIN_SLASH, MOUNTAIN_SPEED, MOUNTAIN_RANGE, MOUNTAIN_DAMAGE, MOUNTAIN_WIDTH,
					MOUNTAIN_HEIGHT, 4.0f);
			case SWORD_DOMAIN -> {
				HeavenSpells.endLingering(p.getUUID(), Domain.class);
				HeavenSpells.linger(new Domain(p, spell));
			}
			case HEAVEN_SEVERING_SWORD -> sever(p, spell);
			case FOUR_SWORDS_ARRAY -> HeavenSpells.linger(new Array(p, spell));
			default -> {}
		}
	}

	// --- Qi Refining ---

	/** A sword of metal formed at {@code at} (beside the caster) over {@code form} ticks and shot: {@code damage}, through {@code pierce} more. */
	private static SpellProjectile sword(ServerPlayer p, Spell spell, Vec3 at, int form, float damage, int pierce) {
		return SpellProjectile.cast(p, spell, SpellProjectile.Shape.FLYING_SWORD, 1.0f, form, at, SWORD_SPEED, SWORD_RANGE, new SpellProjectile.Behaviour() {
			@Override
			public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
				HeavenSpells.strike(p, target, damage, spell, missile);
				target.invulnerableTime = 0;
				SpellFx.send(p.serverLevel(), SpellFx.Kind.METAL_SPARKS, missile.position(), missile.getDeltaMovement(), 0.7f, 0);
				HeavenSpells.sound(p.serverLevel(), target.position(), SoundEvents.TRIDENT_HIT, 1.0f, 1.4f);
				return true;
			}

			@Override
			public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
				SpellFx.send(p.serverLevel(), SpellFx.Kind.METAL_SPARKS, hit.getLocation(), missile.getDeltaMovement(), 0.8f, 0);
				HeavenSpells.sound(p.serverLevel(), hit.getLocation(), SoundEvents.TRIDENT_HIT_GROUND, 1.0f, 1.3f);
			}

			@Override
			public void launched(SpellProjectile missile) {
				HeavenSpells.sound(p.serverLevel(), missile.position(), SoundEvents.TRIDENT_THROW, 0.9f, 1.5f);
			}
		}).pierce(pierce);
	}

	private static void hurricane(ServerPlayer p, Spell spell) {
		Vec3 look = p.getViewVector(1.0f);
		p.startAutoSpinAttack(HURRICANE_TICKS);
		HeavenSpells.move(p, look.scale(HURRICANE_SPEED).add(0, p.onGround() ? 0.25 : 0, 0));
		Spells.safeFall(p, HURRICANE_TICKS + 40);
		SpellAuras.put(p, SpellAura.STEEL_HURRICANE, HURRICANE_TICKS);
		HeavenSpells.endLingering(p.getUUID(), Hurricane.class);
		HeavenSpells.linger(new Hurricane(p, spell));
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.TRIDENT_RIPTIDE_3, 1.2f, 1.2f);
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
	}

	/** Steel Hurricane's edge: each being its spinning blade meets takes {@link #HURRICANE_DAMAGE} of metal on top of the weapon's blow. */
	public static final class Hurricane extends HeavenSpells.Timed implements HeavenSpells.MeleeListener {
		private final Spell spell;
		private final Set<LivingEntity> cut = new HashSet<>();

		Hurricane(ServerPlayer caster, Spell spell) {
			super(caster, HURRICANE_TICKS + 4);
			this.spell = spell;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 4 == 0) SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(caster), caster.getDeltaMovement(), 0.4f, 0);
			return casterHere();
		}

		@Override
		public void onMelee(ServerPlayer attacker, LivingEntity target) {
			if (attacker != caster || !cut.add(target)) return;
			HeavenSpells.strikeAgain(caster, target, HURRICANE_DAMAGE, spell);
			SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(target), caster.getDeltaMovement(), 0.8f, 0);
		}
	}

	// --- Foundation Building ---

	/**
	 * A crescent of sword qi flung from the caster (Boulder Cleave, Mountain Cleave): it flies {@code speed} a tick for {@code range},
	 * through the ground as through air, cutting a slot {@code width} wide and {@code height} tall wherever it passes (what it cuts is
	 * gone: nothing drops), and cleaving every being in its way for {@code damage}.
	 */
	private static void cleave(ServerPlayer p, Spell spell, SpellProjectile.Shape shape, double speed, double range, float damage, int width, int height, float scale) {
		ServerLevel level = p.serverLevel();
		Vec3 look = p.getViewVector(1.0f);
		Vec3 flat = look.multiply(1, 0, 1).lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, p.getYRot()) : look.multiply(1, 0, 1).normalize();
		Vec3 side = new Vec3(-flat.z, 0, flat.x);
		Set<BlockPos> cut = new HashSet<>();
		SpellProjectile.cast(p, spell, shape, scale, 0, new Vec3(0, -0.3, 1.0), speed, range, new SpellProjectile.Behaviour() {
			@Override
			public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
				HeavenSpells.strike(p, target, damage, spell, missile);
				SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(target), missile.getDeltaMovement(), 1.2f, 0);
				return true;
			}

			@Override
			public void hitBlock(SpellProjectile missile, BlockHitResult hit) {}

			@Override
			public void flying(SpellProjectile missile) {
				// The slot it cuts: its width across its way, its height from a little under it up.
				Vec3 at = missile.position();
				Vec3 step = missile.getDeltaMovement();
				int steps = (int) Math.ceil(step.length());
				for (int s = 0; s <= steps; s++) {
					Vec3 point = at.add(step.scale(s / (double) Math.max(1, steps)));
					for (int w = -(width / 2); w <= width / 2; w++) {
						for (int h = 0; h < height; h++) {
							BlockPos pos = BlockPos.containing(point.add(side.scale(w)).add(0, h - (height > 4 ? height / 3 : 0), 0));
							if (!cut.add(pos)) continue;
							BlockState state = level.getBlockState(pos);
							if (state.isAir() || !HeavenSpells.carvable(level, pos, p)) continue;
							level.removeBlock(pos, false);
							if (level.random.nextInt(height > 4 ? 12 : 4) == 0) {
								SpellBlock.show(level, state, Vec3.atBottomCenterOf(pos), Vec3.ZERO, 0, 10, SpellBlock.Motion.CRUMBLE, 0);
							}
						}
					}
				}
				if (missile.tickCount % 3 == 0) HeavenSpells.sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
			}
		}).phasing().pierce(1000).hitSize(scale * 1.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, scale > 2 ? 0.4f : 0.8f);
		HeavenSpells.sound(level, p.position(), SoundEvents.TRIDENT_RIPTIDE_2, 1.0f, scale > 2 ? 0.5f : 1.4f);
	}

	/** Every ore block (any block named for one, of any mod, and ancient debris), found once. */
	private static java.util.Set<Block> oreBlocks;

	/** Whether {@code state} is an ore (any block named for one, or ancient debris). */
	static boolean ore(BlockState state) {
		if (oreBlocks == null) {
			java.util.Set<Block> found = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
			for (Block block : BuiltInRegistries.BLOCK) if (BuiltInRegistries.BLOCK.getKey(block).getPath().endsWith("_ore")) found.add(block);
			found.add(Blocks.ANCIENT_DEBRIS);
			oreBlocks = found;
		}
		return oreBlocks.contains(state.getBlock());
	}

	/** The plain rock an ore lies in: deepslate's, the Nether's, the End's or stone. */
	static BlockState rockOf(BlockState ore) {
		String path = BuiltInRegistries.BLOCK.getKey(ore.getBlock()).getPath();
		if (path.startsWith("deepslate_")) return Blocks.DEEPSLATE.defaultBlockState();
		if (path.startsWith("nether_") || ore.is(Blocks.ANCIENT_DEBRIS)) return Blocks.NETHERRACK.defaultBlockState();
		if (ore.is(Blocks.GILDED_BLACKSTONE)) return Blocks.BLACKSTONE.defaultBlockState();
		return Blocks.STONE.defaultBlockState();
	}

	/** The colour an ore's streak of light takes, by its name. */
	static int oreColour(BlockState ore) {
		String path = BuiltInRegistries.BLOCK.getKey(ore.getBlock()).getPath();
		if (path.contains("diamond")) return 0x6FF2E6;
		if (path.contains("emerald")) return 0x3FE07A;
		if (path.contains("gold")) return 0xFFD23A;
		if (path.contains("redstone")) return 0xFF3020;
		if (path.contains("lapis")) return 0x3A5CFF;
		if (path.contains("copper")) return 0xE07A45;
		if (path.contains("iron")) return 0xE8C8B0;
		if (path.contains("coal")) return 0x707070;
		if (path.contains("quartz")) return 0xF4EEE6;
		if (path.contains("jade")) return 0x5CE0A8;
		if (ore.is(Blocks.ANCIENT_DEBRIS)) return 0x9A6A4E;
		return 0xE0E0F0;
	}

	/**
	 * Mineral Purification: every ore (as many as {@link #ORES_MAX}, nearest first) within the reach of the caster's consciousness
	 * (ConsciousnessDomainHandler#radius) gives up its ore, which streams to the caster as light and is theirs (as mined, unsmelted),
	 * the rock it lay in left behind.
	 */
	private static void purify(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		double radius = ConsciousnessDomainHandler.radius(CultivationManager.get(p));
		BlockPos centre = p.blockPosition();
		List<BlockPos> ores = oresWithin(level, p, centre, radius);
		ores.sort(java.util.Comparator.comparingDouble(pos -> pos.distSqr(centre)));
		if (ores.size() > ORES_MAX) ores = ores.subList(0, ORES_MAX);
		if (ores.isEmpty()) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_NO_ORES), true);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, p.position(), Vec3.ZERO, (float) Math.min(radius, 24), 0);
			return;
		}
		int count = 0;
		Vec3 to = HeavenSpells.middle(p);
		for (BlockPos pos : ores) {
			BlockState state = level.getBlockState(pos);
			List<ItemStack> drops = Block.getDrops(state, level, pos, null, p, ItemStack.EMPTY);
			level.setBlock(pos, rockOf(state), 3);
			for (ItemStack drop : drops) {
				if (!p.getInventory().add(drop)) p.drop(drop, false);
			}
			if (count++ < 64 || count % 4 == 0) SpellFx.send(level, SpellFx.Kind.STREAK, Vec3.atCenterOf(pos), to, 1, oreColour(state));
		}
		p.containerMenu.broadcastChanges();
		p.displayClientMessage(Component.translatable(ModLang.MSG_ORES_FOUND, ores.size()), true);
		HeavenSpells.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.8f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.5f);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, p.position(), Vec3.ZERO, (float) Math.min(radius, 32), 0);
	}

	/**
	 * Every ore within {@code radius} of {@code centre}, in the loaded world, outside shelters and ground {@code p} may not dig. A
	 * consciousness reaches far (160 blocks at Four Axis), so the world is read a chunk section at a time, and a section whose
	 * palette holds no ore (or only air) is passed over whole.
	 */
	static List<BlockPos> oresWithin(ServerLevel level, ServerPlayer p, BlockPos centre, double radius) {
		List<BlockPos> ores = new ArrayList<>();
		int r = (int) Math.ceil(radius);
		double r2 = radius * radius;
		for (int cx = (centre.getX() - r) >> 4; cx <= (centre.getX() + r) >> 4; cx++) {
			for (int cz = (centre.getZ() - r) >> 4; cz <= (centre.getZ() + r) >> 4; cz++) {
				net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;
				net.minecraft.world.level.chunk.LevelChunkSection[] sections = chunk.getSections();
				for (int i = 0; i < sections.length; i++) {
					net.minecraft.world.level.chunk.LevelChunkSection section = sections[i];
					if (section == null || section.hasOnlyAir() || !section.getStates().maybeHas(MetalSpells::ore)) continue;
					int baseY = level.getSectionYFromSectionIndex(i) << 4;
					if (baseY + 15 < centre.getY() - r || baseY > centre.getY() + r) continue;
					for (int x = 0; x < 16; x++) {
						for (int y = 0; y < 16; y++) {
							for (int z = 0; z < 16; z++) {
								BlockState state = section.getBlockState(x, y, z);
								if (!ore(state)) continue;
								BlockPos pos = new BlockPos((cx << 4) + x, baseY + y, (cz << 4) + z);
								if (pos.distSqr(centre) > r2 || Formations.shelters(level, pos) || Formations.protectsGround(p, pos)) continue;
								ores.add(pos);
							}
						}
					}
				}
			}
		}
		return ores;
	}

	/**
	 * Advanced Flying Sword Technique: {@link #SWORDS} swords form in the air round the caster one by one, then fly one by one at
	 * where they look (each its {@link #ADVANCED_DAMAGE}, through one being).
	 */
	private static void advanced(ServerPlayer p, Spell spell) {
		for (int i = 0; i < SWORDS; i++) {
			int index = i;
			double a = Math.PI * (0.15 + 0.7 * i / (SWORDS - 1));
			Vec3 at = new Vec3(Math.cos(a) * 1.6, 0.4 + Math.sin(a) * 0.9, -0.2);
			int formed = i * SWORD_FORM_EVERY;
			int loosed = SWORDS * SWORD_FORM_EVERY + 4 + i * SWORD_LOOSE_EVERY;
			Runnable make = () -> {
				SpellProjectile sword = sword(p, spell, at, loosed - formed, ADVANCED_DAMAGE, 1);
				sword.data(index);
				HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.ANVIL_PLACE, 0.3f, 1.8f);
			};
			if (formed <= 0) make.run();
			else HeavenSpells.linger(new FireSpells.Delayed(p, formed, make));
		}
	}

	// --- Core Formation ---

	private static void rain(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
		LivingEntity aimed = HeavenSpells.aimed(p, RAIN_RANGE);
		Vec3 point = aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, RAIN_RANGE);
		Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 1, 0), 3, 24);
		Vec3 target = ground != null ? ground : point;
		Vec3 sky = target.add(0, 9, 0);
		SpellField.create(level, SpellField.Kind.SWORD_RING, sky, (float) RAIN_RADIUS, 1, RAIN_FORM, p).data(RAIN_SWORDS).spawn();
		HeavenSpells.sound(level, sky, SoundEvents.ANVIL_PLACE, 1.0f, 1.6f);
		HeavenSpells.linger(new FireSpells.Delayed(p, RAIN_FORM, () -> {
			for (int i = 0; i < RAIN_SWORDS; i++) {
				double a = Math.PI * 2 * i / RAIN_SWORDS;
				Vec3 from = sky.add(Math.cos(a) * RAIN_RADIUS * 0.8, 0, Math.sin(a) * RAIN_RADIUS * 0.8);
				Vec3 down = target.add(Math.cos(a) * RAIN_RADIUS * 0.5 * level.random.nextDouble(), 0, Math.sin(a) * RAIN_RADIUS * 0.5 * level.random.nextDouble());
				SpellProjectile.thrown(level, p, spell, SpellProjectile.Shape.PLUNGING_SWORD, 1.0f, from, down.subtract(from).normalize().scale(1.8), 16,
						new SpellProjectile.Behaviour() {
							@Override
							public boolean hitEntity(SpellProjectile missile, LivingEntity hit) {
								HeavenSpells.strike(p, hit, RAIN_DAMAGE, spell, missile);
								hit.invulnerableTime = 0;
								SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, missile.position(), missile.getDeltaMovement(), 0.6f, 0);
								return false;
							}

							@Override
							public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
								SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, hit.getLocation(), missile.getDeltaMovement(), 0.6f, 0);
								HeavenSpells.sound(level, hit.getLocation(), SoundEvents.TRIDENT_HIT_GROUND, 0.6f, 1.5f);
							}
						}).data(i);
			}
			HeavenSpells.sound(level, sky, SoundEvents.TRIDENT_RIPTIDE_3, 1.5f, 1.5f);
		}));
	}

	// --- Nascent Soul ---

	/**
	 * Piercing Sword Light: a line of sword light along the look for {@link #LIGHT_RANGE}, through whatever stands in its way: it
	 * bores a tunnel {@link #LIGHT_BORE} round through hill and wall (Devastation#bore) and cuts every being along it.
	 */
	private static void light(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
		Vec3 from = HeavenSpells.fromHands(p);
		Vec3 look = p.getViewVector(1.0f);
		Vec3 to = p.getEyePosition().add(look.scale(LIGHT_RANGE));
		SpellField.create(level, SpellField.Kind.SWORD_LIGHT, from, 0.6f, (float) from.distanceTo(to), 14, p).facing(p.getYRot(), p.getXRot()).spawn();
		Vec3 eye = p.getEyePosition();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(eye, to).inflate(2), e -> !HeavenSpells.spared(p, e))) {
			Vec3 mid = HeavenSpells.middle(e).subtract(eye);
			double along = mid.dot(look);
			if (along < 0 || along > LIGHT_RANGE || mid.subtract(look.scale(along)).length() > 1.2 + e.getBbWidth() / 2) continue;
			HeavenSpells.strike(p, e, LIGHT_DAMAGE, spell);
			e.invulnerableTime = 0;
			SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), look, 1.2f, 0);
		}
		Devastation.bore(p, level, eye.add(look.scale(1.5)), to, LIGHT_BORE);
		SpellFx.send(level, SpellFx.Kind.FLASH, to, Vec3.ZERO, 2.0f, 0xFFF2C0);
		HeavenSpells.sound(level, p.position(), SoundEvents.TRIDENT_THUNDER, 1.0f, 1.8f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 2.0f);
	}

	// --- Heavenly Being ---

	/** Sword Domain: swords circle the caster {@link #DOMAIN_RADIUS} round for {@link #DOMAIN_TICKS}; each foe inside is cut every half second. */
	public static final class Domain extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellField field;

		Domain(ServerPlayer caster, Spell spell) {
			super(caster, DOMAIN_TICKS);
			this.spell = spell;
			this.field = SpellField.create(level, SpellField.Kind.SWORD_DOMAIN, caster.position(), (float) DOMAIN_RADIUS, 3, DOMAIN_TICKS, caster).data(36).spawn();
			HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_ACTIVATE, 1.5f, 1.8f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.ANVIL_USE, 1.0f, 1.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			field.setPos(caster.getX(), caster.getY(), caster.getZ());
			if (age % 10 == 5) {
				for (LivingEntity e : HeavenSpells.around(caster, DOMAIN_RADIUS, e -> !HeavenSpells.friendly(caster, e) && !HeavenSpells.spared(caster, e))) {
					HeavenSpells.strikeAgain(caster, e, DOMAIN_DAMAGE, spell);
					SpellFx.send(level, SpellFx.Kind.STREAK, HeavenSpells.middle(caster).add(0, 1, 0), HeavenSpells.middle(e), 1, 0xF4F0D8);
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 0.6f, 0);
				}
				if (age % 20 == 5) HeavenSpells.sound(level, caster.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Four Axis ---

	/**
	 * Heaven-Severing Sword: one stroke, and a crescent of sword light about 110 blocks across flies out level from the caster's
	 * chest the way they face, {@link #SEVER_SPEED} a tick for {@link #SEVER_RANGE} blocks. Nothing stops it, and it cuts only what
	 * its blade passes through: a slit {@link #SEVER_THICK} blocks tall at its middle, thinning toward its tips, through hills,
	 * mountains, trees and whatever is built in its way (the tops stand on above the cut), the land's far end of it cut in its
	 * crescent's curve. Every being its blade passes through takes {@link #SEVER_DAMAGE} and is hurled ahead of it.
	 */
	private static void sever(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
		Vec3 look = p.getViewVector(1.0f);
		Vec3 flat = look.multiply(1, 0, 1).lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, p.getYRot()) : look.multiply(1, 0, 1).normalize();
		Vec3 side = new Vec3(-flat.z, 0, flat.x);
		// It runs level, its middle at a caster's chest over the ground they stand over (in the air, the ground below them, if near).
		Vec3 ground = HeavenSpells.groundBelow(level, p.position().add(0, 1, 0), 2, 24);
		int base = (int) Math.floor((ground != null ? ground : p.position()).y);
		double bladeY = base + SEVER_LIFT;
		double half = SEVER_RADIUS * Math.sin(SEVER_SPAN / 2);
		java.util.Set<Long> columns = new java.util.HashSet<>();
		java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
		Vec3 from = new Vec3(p.getX(), base + 1, p.getZ()).add(flat.scale(2.5));
		for (int d = 0; d <= SEVER_RANGE + 16; d += 32) loadSeverWay(level, from.add(flat.scale(d)));
		long[] ticketed = {Long.MIN_VALUE};
		SpellProjectile.thrown(level, p, spell, SpellProjectile.Shape.HEAVEN_SLASH, 1.0f, from, flat.scale(SEVER_SPEED), SEVER_RANGE, new SpellProjectile.Behaviour() {
			@Override
			public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
				return true; // its blade cuts what it passes (flying), and goes on
			}

			@Override
			public void hitBlock(SpellProjectile missile, BlockHitResult hit) {}

			@Override
			public void flying(SpellProjectile missile) {
				Vec3 at = missile.position();
				// The land ahead of it asked for again as it goes (the first asking may have lapsed if it waited at unmade land).
				ChunkPos ahead = new ChunkPos(BlockPos.containing(at.add(flat.scale(32))));
				if (ahead.toLong() != ticketed[0]) {
					ticketed[0] = ahead.toLong();
					loadSeverWay(level, at.add(flat.scale(32)));
				}
				// How far out its bow is now and was a tick ago, along its way from where it set out (its caster stands just behind).
				double bowNow = at.subtract(from).dot(flat), bowBefore = bowNow - SEVER_SPEED;
				// What its blade passed through this tick: at each step out to the side, the blade lags its bow by the crescent's curve,
				// and is as thick there as the crescent is.
				List<int[]> cut = new ArrayList<>();
				for (int w = (int) -Math.floor(half); w <= (int) Math.floor(half); w++) {
					double lag = severLag(w), thick = severThickness(w);
					double fFrom = Math.max(0, bowBefore - lag), fTo = bowNow - lag;
					if (fTo < 0 || thick <= 0) continue;
					int yLo = (int) Math.floor(bladeY - thick / 2), yHi = (int) Math.floor(bladeY + thick / 2);
					for (double f = Math.ceil(fFrom); f <= fTo; f++) {
						BlockPos column = BlockPos.containing(from.add(flat.scale(f)).add(side.scale(w)));
						if (columns.add(BlockPos.asLong(column.getX(), 0, column.getZ()))) cut.add(new int[]{column.getX(), column.getZ(), yLo, yHi});
					}
				}
				Devastation.shear(p, level, cut, at);
				// Every being its blade passes through this tick.
				AABB band = new AABB(at.subtract(flat.scale(SEVER_SPEED + SEVER_RADIUS)), at).inflate(half + 1, 0, half + 1)
						.setMinY(bladeY - SEVER_THICK / 2 - 2).setMaxY(bladeY + SEVER_THICK / 2 + 1);
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, band, e -> !HeavenSpells.spared(p, e))) {
					Vec3 off = e.position().subtract(from);
					double across = off.dot(side), thick = severThickness(across);
					double blade = bowNow - severLag(across);
					double feet = e.getY(), head = e.getY() + e.getBbHeight();
					if (thick <= 0 || off.dot(flat) > blade + 1 || off.dot(flat) < Math.max(-1, blade - SEVER_SPEED - 2)) continue;
					if (head < bladeY - thick / 2 || feet > bladeY + thick / 2 || !struck.add(e)) continue;
					HeavenSpells.strike(p, e, SEVER_DAMAGE, spell, missile);
					HeavenSpells.move(e, flat.scale(1.8).add(0, 0.7, 0));
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), flat, 2.0f, 0);
				}
				if (missile.tickCount % 2 == 0) {
					HeavenSpells.sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 3.0f, 0.4f);
					SpellFx.send(level, SpellFx.Kind.EARTH_BURST, new Vec3(at.x, base + 0.5, at.z), Vec3.ZERO, 3, Block.getId(Blocks.STONE.defaultBlockState()), 160);
				}
				if (missile.tickCount % 6 == 0) HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.4f);
			}
		}).phasing().pierce(100000).hitSize(4.0f).data(Math.round(p.getYRot()));
		SpellFx.send(level, SpellFx.Kind.FLASH, p.position().add(flat.scale(3)).add(0, 2, 0), Vec3.ZERO, 8, 0xFFF4D0, 256);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, p.position(), Vec3.ZERO, 14, 0, 256);
		HeavenSpells.sound(level, p.position(), SoundEvents.TRIDENT_THUNDER, 4.0f, 0.5f);
		HeavenSpells.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 4.0f, 0.3f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.4f);
	}

	/**
	 * Four Swords Array: four giant swords plant at the corners of a square {@link #ARRAY_HALF} each way of the point looked at;
	 * for {@link #ARRAY_TICKS} every foe inside is held in (thrown back from its edge) and cut ({@link #ARRAY_DAMAGE} a second).
	 */
	public static final class Array extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final SpellField field;

		Array(ServerPlayer caster, Spell spell) {
			super(caster, ARRAY_TICKS);
			this.spell = spell;
			LivingEntity aimed = HeavenSpells.aimed(caster, ARRAY_RANGE);
			Vec3 point = aimed != null ? aimed.position() : HeavenSpells.lookPoint(caster, ARRAY_RANGE);
			Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.at = ground != null ? ground : point;
			this.field = SpellField.create(level, SpellField.Kind.SWORD_ARRAY, at, (float) ARRAY_HALF, 24, ARRAY_TICKS, caster).spawn();
			HeavenSpells.sound(level, at, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
			HeavenSpells.sound(level, at, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.5f);
		}

		private boolean inside(Vec3 pos, double margin) {
			return Math.abs(pos.x - at.x) <= ARRAY_HALF + margin && Math.abs(pos.z - at.z) <= ARRAY_HALF + margin && pos.y > at.y - 8 && pos.y < at.y + 32;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < 10) return true; // the swords are still falling into place
			if (age == 10) {
				// Each sword driven deep into the ground where it lands, the earth thrown up round it.
				for (int c = 0; c < 4; c++) {
					Vec3 corner = at.add((c == 0 || c == 3 ? -1 : 1) * ARRAY_HALF, 0, (c < 2 ? -1 : 1) * ARRAY_HALF);
					Devastation.crater(caster, level, corner, ARRAY_CRATER, ARRAY_CRATER_DEPTH, Devastation.Floor.RUBBLE);
					SpellFx.send(level, SpellFx.Kind.EARTH_BURST, corner, Vec3.ZERO, 4, Block.getId(Blocks.STONE.defaultBlockState()), 192);
				}
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, (float) ARRAY_HALF * 2.5f, 0, 256);
				HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.4f);
			}
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(ARRAY_HALF + 3, 32, ARRAY_HALF + 3),
					e -> !HeavenSpells.friendly(caster, e) && !HeavenSpells.spared(caster, e))) {
				if (!inside(e.position(), 2.5)) continue;
				// Held in: at its edge, thrown back toward its middle.
				if (!inside(e.position(), -1.0)) {
					Vec3 in = at.subtract(e.position()).multiply(1, 0, 1).normalize();
					HeavenSpells.move(e, in.scale(0.8).add(0, 0.2, 0));
				}
				if (age % 10 == 0) HeavenSpells.strikeAgain(caster, e, ARRAY_DAMAGE / 2, spell);
			}
			if (age % 20 == 0) HeavenSpells.sound(level, at, SoundEvents.BEACON_AMBIENT, 3.0f, 1.6f);
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	static void tick(MinecraftServer server) {}

	static void forget(UUID id) {}

	static void clear() {}

	private MetalSpells() {}
}
