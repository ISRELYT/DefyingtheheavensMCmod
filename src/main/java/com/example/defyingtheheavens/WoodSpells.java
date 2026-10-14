package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The wood methods' twelve spells (see HeavenSpells for what they share): roots that bind, brambles that tear the striker,
 * a vine to climb by, vines that bind two beings together, thorn pods lying in wait, a heartwood that roots and fortifies, a
 * pollen of sleep, a thicket that tears whatever moves, a fig that steals its host's healing, a rite whose blessed heal from
 * every hurt, a canopy that catches missiles, and the World Tree itself.
 */
public final class WoodSpells {
	static final int LEAF = 0x3CE65A, DEEP_LEAF = 0x14B43C, ROOT = 0x6B4A2B, THORN = 0x7A4A22, VINE = 0x1F7A2E, WISTERIA = 0x9B6BD6,
			HEARTWOOD = 0xC89B5A, POLLEN = 0xF2D64B, PALE_GOLD = 0xFFF2B0, JADE = 0x9CF5A8, SEED = 0x5A3E1E, BARK = 0x8B5A2B;

	/** Grasping Roots: reach, harm, how far below a being in the air the ground may be, the haul, the bind (a great beast's). */
	static final double ROOTS_RANGE = 16, ROOTS_FLOOR = 12;
	static final float ROOTS_HARM = 5;
	static final int ROOTS_HAUL_TICKS = 15, ROOTS_BIND_TICKS = 50, ROOTS_BIND_BEAST_TICKS = 20;
	/** Bramble Mantle: how long, the share of a melee blow it leaves, the tearing (base, share of the blow, most), the snag. */
	static final int MANTLE_TICKS = 240, MANTLE_SNAG_TICKS = 20, MANTLE_TEAR_EVERY = 10;
	static final float MANTLE_KEEPS = 0.85f, MANTLE_TEAR = 2, MANTLE_TEAR_SHARE = 0.5f, MANTLE_TEAR_MAX = 8;
	/** Wisteria Ascent: reach, the haul's longest, the cling's longest, and the safe fall after. */
	static final double WISTERIA_RANGE = 28;
	static final int WISTERIA_PULL_TICKS = 40, WISTERIA_CLING_TICKS = 80, WISTERIA_SAFE_TICKS = 60;
	/** Twin Serpent Vines: reach, how near the second being, harm, how far apart they may stray, the haul's harm, how long. */
	static final double TWIN_RANGE = 12, TWIN_PARTNER = 10, TWIN_SLACK = 4;
	static final float TWIN_HARM = 8, TWIN_HAUL_HARM = 4;
	static final int TWIN_TICKS = 160;
	/** Bursting Thorn Pods: reach, how many and how far round, how long they wait, the trigger, the burst, at most how many lie. */
	static final double PODS_RANGE = 20, PODS_SPREAD = 2.5, PODS_TRIGGER = 1.5, PODS_BURST = 2.5;
	static final int PODS_COUNT = 5, PODS_TICKS = 400, PODS_MAX = 10, PODS_SLOW_TICKS = 40;
	static final float PODS_HARM = 18, PODS_SPLASH = 8;
	/** Heartwood of Nine Rings: how long rooted, the rings, what each takes off and mends, the release. */
	static final int HEARTWOOD_TICKS = 180, HEARTWOOD_RINGS = 9, HEARTWOOD_RELEASE_TICKS = 160;
	static final float HEARTWOOD_WARD_PER_RING = 0.05f, HEARTWOOD_MEND_PER_RING = 0.5f, HEARTWOOD_BLOW_PER_RING = 3;
	/** Pollen of Long Dreams: reach, the cloud and how long it drifts, the sleep, the wakened's grace, a great beast's slow. */
	static final double POLLEN_RANGE = 24, POLLEN_RADIUS = 4.5;
	static final int POLLEN_TICKS = 80, SLEEP_TICKS = 120, WOKEN_TICKS = 200, POLLEN_BEAST_TICKS = 60;
	static final float WAKING_BLOW = 1.5f;
	/** Myriad Thorn Thicket: reach, size, how long, the tearing per block moved and its most each half second. */
	static final double THICKET_RANGE = 24, THICKET_RADIUS = 10;
	static final int THICKET_TICKS = 200;
	static final float THICKET_PER_BLOCK = 3, THICKET_MAX = 8;
	/** Strangler Fig Seed: reach, harm, how long it feeds, and how far the stolen healing can flow. */
	static final double FIG_RANGE = 32, FIG_REACH = 64;
	static final float FIG_HARM = 65;
	static final int FIG_TICKS = 200;
	/** Rite of Returning Spring: reach, the mending at once, how long the blessing, the share of each hurt that grows back, over how long. */
	static final double RITE_RADIUS = 24;
	static final float RITE_MEND = 30, RITE_SHARE = 0.6f;
	static final int RITE_TICKS = 400, RITE_STREAM_TICKS = 60;
	/** Heaven-Shading Canopy: reach, how long, the qi each missile caught gives back. */
	static final double CANOPY_RADIUS = 32;
	static final int CANOPY_TICKS = 160;
	static final double CANOPY_QI = 20;
	/** The World Tree Rises: reach, size, how long, growth, the mending, the roots' strike (every, how many, harm). */
	static final double TREE_RANGE = 32, TREE_RADIUS = 28, TREE_HEIGHT = 48, TREE_LEASH = 96;
	static final int TREE_TICKS = 600, TREE_GROWTH = 40, TREE_STRIKE_EVERY = 60, TREE_STRIKES = 8;
	static final float TREE_MEND = 12, TREE_ROOT_HARM = 60;

	private static final UUID HEARTWOOD_STILL_ID = UUID.fromString("a3f1c7e2-5b84-4e19-9d06-7c2b8e4f1a35");
	private static final UUID HEARTWOOD_ROOTED_ID = UUID.fromString("e9b24d61-3c7f-4a8e-b512-6d0f9a3c2e74");

	/** Whether {@code spell} can take hold for {@code p} now; refused, nothing is paid. */
	static boolean canCast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case GRASPING_ROOTS -> {
				return HeavenSpells.aimed(p, ROOTS_RANGE) != null || HeavenSpells.refuse(p);
			}
			case WISTERIA_ASCENT -> {
				return HeavenSpells.lookedBlock(p, WISTERIA_RANGE) != null || HeavenSpells.refuse(p);
			}
			case TWIN_SERPENT_VINES -> {
				return HeavenSpells.aimed(p, TWIN_RANGE) != null || HeavenSpells.refuse(p);
			}
			case BURSTING_THORN_PODS -> {
				return HeavenSpells.lookedBlock(p, PODS_RANGE) != null || HeavenSpells.refuse(p);
			}
			case HEARTWOOD_OF_NINE_RINGS -> {
				// Cast again while rooted: uproot, and nothing is paid.
				if (!HeavenSpells.lingering(p.getUUID(), Heartwood.class).isEmpty()) {
					HeavenSpells.endLingering(p.getUUID(), Heartwood.class);
					return false;
				}
				return p.onGround() || p.isInWater() || HeavenSpells.refuse(p);
			}
			case STRANGLER_FIG_SEED -> {
				return HeavenSpells.aimed(p, FIG_RANGE) != null || HeavenSpells.refuse(p);
			}
			default -> {
				return true;
			}
		}
	}

	/** Casts one of the wood spells (paid for). */
	static void cast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case GRASPING_ROOTS -> graspingRoots(p);
			case BRAMBLE_MANTLE -> brambleMantle(p);
			case WISTERIA_ASCENT -> wisteriaAscent(p);
			case TWIN_SERPENT_VINES -> twinVines(p);
			case BURSTING_THORN_PODS -> thornPods(p);
			case HEARTWOOD_OF_NINE_RINGS -> heartwood(p, spell);
			case POLLEN_OF_LONG_DREAMS -> pollen(p);
			case MYRIAD_THORN_THICKET -> thicket(p);
			case STRANGLER_FIG_SEED -> strangler(p);
			case RITE_OF_RETURNING_SPRING -> rite(p);
			case HEAVEN_SHADING_CANOPY -> canopy(p);
			case WORLD_TREE_RISES -> worldTree(p);
			default -> { }
		}
	}

	/** Every server tick (what lingers is ticked by HeavenSpells). */
	static void tick(MinecraftServer server) {
	}

	/** A player left. */
	static void forget(UUID id) {
	}

	/** The server stopping. */
	static void clear() {
		Woken.UNTIL.clear();
	}

	// --- Grasping Roots ---

	/** Grasping Roots: roots burst from the earth under the first being looked at, harm it, haul it down from the air, and bind it. */
	private static void graspingRoots(ServerPlayer p) {
		LivingEntity target = HeavenSpells.aimed(p, ROOTS_RANGE);
		if (target == null) return;
		ServerLevel level = p.serverLevel();
		HeavenSpells.strike(p, target, ROOTS_HARM, QiElement.WOOD);
		Vec3 floor = HeavenSpells.groundBelow(level, target.position(), 0, (int) ROOTS_FLOOR);
		if (floor != null) HeavenSpells.particles(level, HeavenSpells.block(Blocks.ROOTED_DIRT), floor.add(0, 0.1, 0), 30, 0.4, 0.1);
		HeavenSpells.sound(level, target.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 0.8f);
		// The Ender Dragon and the Wither are past binding: the harm alone.
		if (!target.isAlive() || target instanceof EnderDragon || target instanceof WitherBoss) return;
		for (Bind old : HeavenSpells.lingering(p.getUUID(), Bind.class)) if (old.target == target) old.end = 0;
		HeavenSpells.linger(new Bind(p, target, floor, HeavenSpells.greatBeast(target) ? ROOTS_BIND_BEAST_TICKS : ROOTS_BIND_TICKS));
	}

	/** The roots' hold: hauling a being in the air down to the floor beneath it, then binding it where it stands. */
	private static final class Bind extends HeavenSpells.Timed {
		final LivingEntity target;
		final Vec3 floor;
		final int bindTicks;
		Vec3 point;
		long boundAt = -1;

		Bind(ServerPlayer caster, LivingEntity target, Vec3 floor, int bindTicks) {
			super(caster, ROOTS_HAUL_TICKS + bindTicks + 2);
			this.target = target;
			this.floor = floor;
			this.bindTicks = bindTicks;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			if (point == null) {
				// Hauled down while in the air above a floor, at most so long; then bound where it lands.
				if (!target.onGround() && floor != null && age < ROOTS_HAUL_TICKS && !target.isInWater()) {
					HeavenSpells.move(target, new Vec3(0, -1.2, 0));
					target.resetFallDistance();
					HeavenSpells.line(level, floor, target.position(), HeavenSpells.dust(ROOT, 1.2f), 0.5);
					return true;
				}
				target.resetFallDistance();
				point = target.position();
				boundAt = age;
				end = start + age + bindTicks;
				HeavenSpells.sound(level, point, SoundEvents.HANGING_ROOTS_PLACE, 1.0f, 0.7f);
			}
			if (target.position().distanceToSqr(point) > 0.4 * 0.4) {
				if (target.position().distanceToSqr(point) > 8 * 8) return false; // taken off by something else
				HeavenSpells.teleport(target, point);
			}
			HeavenSpells.still(target);
			if ((age - boundAt) % 10 == 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (end - now) + 2, 5, false, false));
			if (age % 2 == 0) {
				double turn = age * 0.4;
				for (int i = 0; i < 8; i++) {
					double h = i * 0.15, a = turn + i * 0.8, r = 0.6 - i * 0.04;
					level.sendParticles(HeavenSpells.dust(i % 3 == 0 ? LEAF : ROOT, 0.9f), point.x + Math.cos(a) * r, point.y + h, point.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
				}
			}
			if (age % 10 == 0) HeavenSpells.particles(level, ParticleTypes.HAPPY_VILLAGER, point, 2, 0.3, 0);
			return true;
		}

		@Override
		public void end() {
			if (point != null && target.isAlive()) {
				target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
				HeavenSpells.sound(level, target.position(), SoundEvents.HANGING_ROOTS_BREAK, 0.6f, 1.0f);
			}
		}
	}

	// --- Bramble Mantle ---

	/** Bramble Mantle: a mantle of brambles that softens melee blows and tears their strikers. */
	private static void brambleMantle(ServerPlayer p) {
		HeavenSpells.endLingering(p.getUUID(), Mantle.class);
		HeavenSpells.linger(new Mantle(p));
		ServerLevel level = p.serverLevel();
		for (int i = 0; i < 20; i++) {
			double a = i * 0.6, h = i * 0.1;
			level.sendParticles(HeavenSpells.dust(LEAF, 1.0f), p.getX() + Math.cos(a) * 0.8, p.getY() + h, p.getZ() + Math.sin(a) * 0.8, 1, 0, 0, 0, 0);
			level.sendParticles(HeavenSpells.block(Blocks.SWEET_BERRY_BUSH), p.getX() + Math.cos(a + 3) * 0.8, p.getY() + h, p.getZ() + Math.sin(a + 3) * 0.8, 1, 0, 0, 0, 0);
		}
		HeavenSpells.sound(level, p.position(), SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.0f, 0.9f);
		HeavenSpells.sound(level, p.position(), SoundEvents.AZALEA_LEAVES_PLACE, 1.0f, 0.8f);
	}

	/** The mantle: each melee blow on its wearer softened, its striker torn the next tick (once in half a second each). */
	private static final class Mantle extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		final Map<LivingEntity, Long> lastTorn = new HashMap<>();
		final List<Object[]> tears = new ArrayList<>();

		Mantle(ServerPlayer caster) {
			super(caster, MANTLE_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			for (Object[] tear : tears) {
				LivingEntity striker = (LivingEntity) tear[0];
				if (!striker.isAlive()) continue;
				HeavenSpells.strikeAgain(caster, striker, (Float) tear[1], QiElement.WOOD);
				striker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MANTLE_SNAG_TICKS, 0));
				HeavenSpells.particles(level, HeavenSpells.block(Blocks.SWEET_BERRY_BUSH), HeavenSpells.middle(striker), 12, 0.3, 0);
				HeavenSpells.particles(level, ParticleTypes.CRIT, HeavenSpells.middle(striker), 6, 0.3, 0.1);
				HeavenSpells.line(level, HeavenSpells.middle(caster), HeavenSpells.middle(striker), HeavenSpells.dust(THORN, 0.8f), 0.4);
				HeavenSpells.sound(level, striker.position(), SoundEvents.THORNS_HIT, 1.0f, 1.0f);
			}
			tears.clear();
			if (age % 4 == 0) {
				for (int i = 0; i < 10; i++) {
					double a = age * 0.1 + i * Math.PI / 5;
					level.sendParticles(HeavenSpells.dust(i % 2 == 0 ? 0x2E7D32 : THORN, 0.8f), caster.getX() + Math.cos(a) * 0.7,
							caster.getY() + 1.1, caster.getZ() + Math.sin(a) * 0.7, 1, 0, 0, 0, 0);
				}
			}
			return true;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			if (target != caster || !(source.getEntity() instanceof LivingEntity striker) || source.getDirectEntity() != striker) return amount;
			if (HeavenSpells.isQi(source) || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.THORNS)
					|| source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || striker.distanceTo(caster) > 6) return amount;
			float kept = amount * MANTLE_KEEPS;
			long now = level.getGameTime();
			Long last = lastTorn.get(striker);
			if (!HeavenSpells.inHurtTime(caster) && (last == null || now - last >= MANTLE_TEAR_EVERY)) {
				lastTorn.put(striker, now);
				tears.add(new Object[]{striker, Math.min(MANTLE_TEAR_MAX, MANTLE_TEAR + MANTLE_TEAR_SHARE * kept)});
			}
			return kept;
		}

		@Override
		public void end() {
			if (caster.isAlive()) HeavenSpells.sound(level, caster.position(), SoundEvents.AZALEA_LEAVES_BREAK, 0.5f, 1.0f);
		}
	}

	// --- Wisteria Ascent ---

	/** Wisteria Ascent: a flowering vine cast onto the block looked at hauls the caster to it, where it clings. */
	private static void wisteriaAscent(ServerPlayer p) {
		BlockHitResult hit = HeavenSpells.lookedBlock(p, WISTERIA_RANGE);
		if (hit == null) return;
		ServerLevel level = p.serverLevel();
		p.stopRiding();
		HeavenSpells.endLingering(p.getUUID(), Ascent.class);
		Vec3 anchor = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.6));
		HeavenSpells.particles(level, HeavenSpells.block(level.getBlockState(hit.getBlockPos()).getBlock()), hit.getLocation(), 10, 0.2, 0.05);
		HeavenSpells.sound(level, p.position(), SoundEvents.FISHING_BOBBER_THROW, 1.0f, 1.0f);
		HeavenSpells.sound(level, anchor, SoundEvents.CAVE_VINES_PLACE, 1.0f, 1.3f);
		HeavenSpells.linger(new Ascent(p, hit.getBlockPos(), anchor));
	}

	/** The vine: hauling the caster to its anchor, then holding it there until it lets go (sneaks), its falls forgiven a while after. */
	private static final class Ascent extends HeavenSpells.Timed {
		final BlockPos block;
		final Vec3 anchor;
		long arrivedAt = -1;
		double lastDistance = Double.MAX_VALUE;
		int stuck;

		Ascent(ServerPlayer caster, BlockPos block, Vec3 anchor) {
			super(caster, WISTERIA_PULL_TICKS + WISTERIA_CLING_TICKS);
			this.block = block;
			this.anchor = anchor;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere() || level.getBlockState(block).isAir()) return false;
			caster.resetFallDistance();
			Vec3 body = caster.position().add(0, 0.9, 0);
			if (arrivedAt < 0) {
				Vec3 d = anchor.subtract(body);
				double distance = d.length();
				if (distance < 1.3) {
					arrivedAt = age;
					end = start + age + WISTERIA_CLING_TICKS;
					HeavenSpells.sound(level, anchor, SoundEvents.MOSS_PLACE, 1.0f, 1.0f);
				} else {
					if (age >= WISTERIA_PULL_TICKS) return false;
					stuck = lastDistance - distance < 0.05 ? stuck + 1 : 0;
					if (stuck >= 4) return false; // blocked on the way
					lastDistance = distance;
					HeavenSpells.move(caster, d.normalize().scale(Math.min(1.6, distance * 0.45 + 0.3)));
					drawVine();
					if (age % 6 == 0) HeavenSpells.sound(level, caster.position(), SoundEvents.FISHING_BOBBER_RETRIEVE, 0.6f, 1.0f);
					return true;
				}
			}
			// Clinging: held still at the anchor (a slight drift under slow falling), until it lets go.
			if (caster.isShiftKeyDown()) return false;
			HeavenSpells.move(caster, Vec3.ZERO);
			caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false));
			if (age % 6 == 0) {
				level.sendParticles(HeavenSpells.dust(WISTERIA, 0.9f), caster.getX(), caster.getY() + 1.2, caster.getZ(), 2, 0.3, 0.3, 0.3, 0);
				level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, caster.getX(), caster.getY() + 1.5, caster.getZ(), 1, 0.3, 0.2, 0.3, 0);
			}
			if (age % 4 == 0) drawVine();
			return true;
		}

		private void drawVine() {
			Vec3 hand = HeavenSpells.fromHands(caster);
			Vec3 along = anchor.subtract(hand);
			double length = along.length();
			if (length < 0.1) return;
			Vec3 dir = along.scale(1 / length);
			int i = 0;
			for (double d = 0; d < length; d += 0.35, i++) {
				Vec3 at = hand.add(dir.scale(d));
				level.sendParticles(HeavenSpells.dust(i % 2 == 0 ? LEAF : WISTERIA, i % 2 == 0 ? 0.9f : 1.1f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
				if (i % 6 == 0) level.sendParticles(HeavenSpells.dust(0xC9A6F0, 0.8f), at.x, at.y - 0.2, at.z, 3, 0.05, 0.1, 0.05, 0);
			}
		}

		@Override
		public void end() {
			if (caster.isAlive()) {
				HeavenSpells.safeFall(caster, WISTERIA_SAFE_TICKS);
				HeavenSpells.sound(level, caster.position(), SoundEvents.CAVE_VINES_BREAK, 0.6f, 1.0f);
			}
		}
	}

	// --- Twin Serpent Vines ---

	/** Twin Serpent Vines: two vines bind the being looked at to the nearest other being, hauling them together when they stray. */
	private static void twinVines(ServerPlayer p) {
		LivingEntity first = HeavenSpells.aimed(p, TWIN_RANGE);
		if (first == null) return;
		ServerLevel level = p.serverLevel();
		LivingEntity second = null;
		double best = TWIN_PARTNER * TWIN_PARTNER;
		for (LivingEntity e : HeavenSpells.aroundPoint(level, first.position(), TWIN_PARTNER, e -> e != first && !HeavenSpells.spared(p, e))) {
			double d = e.distanceToSqr(first);
			if (d < best) {
				best = d;
				second = e;
			}
		}
		HeavenSpells.strike(p, first, TWIN_HARM, QiElement.WOOD);
		if (second != null) HeavenSpells.strike(p, second, TWIN_HARM, QiElement.WOOD);
		HeavenSpells.line(level, HeavenSpells.fromHands(p), HeavenSpells.middle(first), HeavenSpells.dust(VINE, 1.3f), 0.3);
		HeavenSpells.particles(level, ParticleTypes.SWEEP_ATTACK, HeavenSpells.middle(first), 1, 0, 0);
		HeavenSpells.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
		HeavenSpells.sound(level, first.position(), SoundEvents.CAVE_VINES_BREAK, 1.0f, 0.7f);
		if (first.isAlive()) HeavenSpells.linger(new Twined(p, first, second != null && second.isAlive() ? second : null));
	}

	/** Two beings bound by the vines (or one, to the ground where it stood), hauled back together when they stray too far. */
	private static final class Twined extends HeavenSpells.Timed {
		final LivingEntity a, b;
		final Vec3 anchor;
		final Map<LivingEntity, Long> lastHauled = new HashMap<>();

		Twined(ServerPlayer caster, LivingEntity a, LivingEntity b) {
			super(caster, TWIN_TICKS);
			this.a = a;
			this.b = b;
			this.anchor = b == null ? a.position() : null;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(a, level) || b != null && !HeavenSpells.present(b, level)) return false;
			Vec3 other = b == null ? anchor : b.position();
			double apart = a.position().distanceTo(other);
			if (apart > TWIN_SLACK) {
				Vec3 mid = b == null ? anchor : a.position().add(other).scale(0.5);
				haul(a, mid, apart, now);
				if (b != null) haul(b, mid, apart, now);
			}
			if (age % 2 == 0) {
				Vec3 from = HeavenSpells.middle(a), to = b == null ? anchor.add(0, 0.1, 0) : HeavenSpells.middle(b);
				Vec3 along = to.subtract(from);
				double length = along.length();
				for (double d = 0; d < length; d += 0.4) {
					Vec3 at = from.add(along.scale(d / length)).add(0, Math.sin(d * 2 + age * 0.3) * 0.15, 0);
					level.sendParticles(HeavenSpells.dust(VINE, 1.0f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
				}
			}
			return true;
		}

		private void haul(LivingEntity e, Vec3 to, double apart, long now) {
			if (HeavenSpells.greatBeast(e)) return;
			Vec3 d = to.subtract(e.position()).multiply(1, 0, 1);
			if (d.lengthSqr() < 1.0e-4) return;
			HeavenSpells.move(e, d.normalize().scale(Math.min(1.0, (apart - TWIN_SLACK) * 0.4 + 0.2)).add(0, 0.1, 0));
			Long last = lastHauled.get(e);
			if (last == null || now - last >= 20) {
				lastHauled.put(e, now);
				HeavenSpells.strikeAgain(caster, e, TWIN_HAUL_HARM, QiElement.WOOD);
				HeavenSpells.particles(level, HeavenSpells.block(Blocks.OAK_LEAVES), HeavenSpells.middle(e), 8, 0.3, 0);
				HeavenSpells.sound(level, e.position(), SoundEvents.FISHING_BOBBER_RETRIEVE, 0.8f, 0.7f);
			}
		}
	}

	// --- Bursting Thorn Pods ---

	/** Bursting Thorn Pods: five pods scattered round the spot looked at, lying in wait for a foe. */
	private static void thornPods(ServerPlayer p) {
		BlockHitResult hit = HeavenSpells.lookedBlock(p, PODS_RANGE);
		if (hit == null) return;
		ServerLevel level = p.serverLevel();
		Vec3 centre = hit.getLocation();
		Pods pods = HeavenSpells.lingering(p.getUUID(), Pods.class).stream().findFirst().orElse(null);
		if (pods == null) {
			pods = new Pods(p);
			HeavenSpells.linger(pods);
		}
		long now = level.getGameTime();
		for (int i = 0; i < PODS_COUNT; i++) {
			double a = Math.PI * 2 * i / (PODS_COUNT - 1) + p.getRandom().nextDouble() * 0.6;
			Vec3 at = i == 0 ? centre : centre.add(Math.cos(a) * PODS_SPREAD, 0, Math.sin(a) * PODS_SPREAD);
			Vec3 ground = HeavenSpells.groundBelow(level, at, 2, 4);
			if (ground == null) continue;
			pods.lying.add(new double[]{ground.x, ground.y, ground.z, now + PODS_TICKS});
			while (pods.lying.size() > PODS_MAX) pods.lying.remove(0);
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.MOSS_BLOCK), ground.add(0, 0.1, 0), 6, 0.15, 0.02);
		}
		pods.end = now + PODS_TICKS + 1;
		HeavenSpells.sound(level, centre, SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.0f, 0.7f);
		HeavenSpells.sound(level, p.position(), SoundEvents.SNIFFER_DROP_SEED, 0.8f, 1.2f);
	}

	/** A caster's thorn pods, each lying in wait for a foe (x, y, z, until when). */
	static final class Pods extends HeavenSpells.Timed {
		final List<double[]> lying = new ArrayList<>();

		Pods(ServerPlayer caster) {
			super(caster, PODS_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			lying.removeIf(pod -> now > pod[3]);
			for (java.util.Iterator<double[]> it = lying.iterator(); it.hasNext(); ) {
				double[] pod = it.next();
				Vec3 at = new Vec3(pod[0], pod[1], pod[2]);
				// Hidden: only now and then a glint at the soil.
				if ((age + (long) Math.abs(pod[0] * 7)) % 30 == 0) level.sendParticles(HeavenSpells.dust(DEEP_LEAF, 0.6f), at.x, at.y + 0.1, at.z, 1, 0.05, 0, 0.05, 0);
				List<LivingEntity> near = HeavenSpells.aroundPoint(level, at, PODS_TRIGGER, e -> HeavenSpells.foe(caster, e));
				if (near.isEmpty()) continue;
				it.remove();
				burst(at, near.get(0));
			}
			return !lying.isEmpty();
		}

		private void burst(Vec3 at, LivingEntity trigger) {
			HeavenSpells.strike(caster, trigger, PODS_HARM, QiElement.WOOD);
			trigger.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PODS_SLOW_TICKS, 2));
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, PODS_BURST, e -> e != trigger && !HeavenSpells.spared(caster, e))) {
				HeavenSpells.strikeAgain(caster, e, PODS_SPLASH, QiElement.WOOD);
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PODS_SLOW_TICKS, 2));
			}
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.SWEET_BERRY_BUSH), at.add(0, 0.5, 0), 30, 0.8, 0.1);
			HeavenSpells.particles(level, ParticleTypes.CRIT, at.add(0, 0.8, 0), 12, 0.8, 0.3);
			HeavenSpells.particles(level, HeavenSpells.dust(LEAF, 1.2f), at.add(0, 0.6, 0), 16, 1.0, 0);
			HeavenSpells.sound(level, at, SoundEvents.SWEET_BERRY_BUSH_BREAK, 1.2f, 0.7f);
			HeavenSpells.sound(level, at, SoundEvents.PUFFER_FISH_BLOW_UP, 0.8f, 1.2f);
		}
	}

	// --- Heartwood of Nine Rings ---

	/** Heartwood of Nine Rings: rooted where it stands, the caster grows ring on ring, each warding and mending it, until it uproots. */
	private static void heartwood(ServerPlayer p, Spell spell) {
		HeavenSpells.linger(new Heartwood(p, spell));
		// Its cooldown runs from the uprooting, and a second cast uproots (canCast): none until then.
		Spells.setCooldown(p, spell, 0);
		ServerLevel level = p.serverLevel();
		BlockPos below = p.blockPosition().below();
		HeavenSpells.particles(level, HeavenSpells.block(level.getBlockState(below).getBlock()), p.position(), 20, 0.4, 0.1);
		for (int r = 0; r < 8; r++) {
			double a = Math.PI * 2 * r / 8;
			for (double d = 0.3; d <= 2.5; d += 0.3) {
				level.sendParticles(HeavenSpells.dust(ROOT, 1.1f), p.getX() + Math.cos(a) * d, p.getY() + 0.05, p.getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0);
			}
		}
		HeavenSpells.sound(level, p.position(), SoundEvents.ROOTED_DIRT_PLACE, 1.0f, 0.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.WOOD_PLACE, 1.0f, 0.8f);
	}

	/** The rooted heartwood, then (uprooted) the blow it lends the next strike. */
	static final class Heartwood extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		final Spell spell;
		final Vec3 root;
		int rings;

		Heartwood(ServerPlayer caster, Spell spell) {
			super(caster, HEARTWOOD_TICKS);
			this.spell = spell;
			this.root = caster.position();
			set(caster, Attributes.KNOCKBACK_RESISTANCE, HEARTWOOD_ROOTED_ID, 1.0, AttributeModifier.Operation.ADDITION);
			set(caster, Attributes.MOVEMENT_SPEED, HEARTWOOD_STILL_ID, -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			BlockPos under = BlockPos.containing(root).below();
			if (level.getBlockState(under).getCollisionShape(level, under).isEmpty() && !caster.isInWater()) return false;
			if (caster.position().distanceToSqr(root) > 0.3 * 0.3) HeavenSpells.teleport(caster, root);
			Vec3 v = caster.getDeltaMovement();
			if (v.x != 0 || v.z != 0 || v.y > 0) HeavenSpells.move(caster, new Vec3(0, Math.min(v.y, 0), 0));
			if (age > 0 && age % 20 == 0) {
				if (rings < HEARTWOOD_RINGS) {
					rings++;
					HeavenSpells.particles(level, ParticleTypes.HAPPY_VILLAGER, caster.position().add(0, 2.2, 0), 1, 0.1, 0);
					HeavenSpells.sound(level, root, SoundEvents.WOOD_HIT, 1.0f, 0.6f + 0.08f * rings);
				}
				HeavenSpells.mend(caster, HEARTWOOD_MEND_PER_RING * rings);
			}
			if (age % 10 == 0) {
				for (int n = 1; n <= rings; n++) HeavenSpells.ring(level, root.add(0, 0.9, 0), 0.5 + 0.15 * n, HeavenSpells.dust(HEARTWOOD, 1.0f), 16);
			}
			return true;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			return target == caster ? amount * (1 - HEARTWOOD_WARD_PER_RING * rings) : amount;
		}

		@Override
		public void end() {
			unset(caster, Attributes.KNOCKBACK_RESISTANCE, HEARTWOOD_ROOTED_ID);
			unset(caster, Attributes.MOVEMENT_SPEED, HEARTWOOD_STILL_ID);
			if (!caster.isAlive() || caster.isRemoved()) return;
			Spells.setCooldown(caster, spell, spell.cooldownTicks());
			Spells.sync(caster);
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.ROOTED_DIRT), caster.position(), 15, 0.4, 0.1);
			HeavenSpells.sound(level, caster.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 1.0f);
			if (rings > 0) HeavenSpells.linger(new Released(caster, rings));
		}
	}

	/** The heartwood's strength, lent to the next melee blow after the uprooting. */
	private static final class Released extends HeavenSpells.Timed implements HeavenSpells.MeleeListener {
		final int rings;
		boolean spent;

		Released(ServerPlayer caster, int rings) {
			super(caster, HEARTWOOD_RELEASE_TICKS);
			this.rings = rings;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 10 == 0) level.sendParticles(HeavenSpells.dust(HEARTWOOD, 0.8f), caster.getX(), caster.getY() + 1, caster.getZ(), 2, 0.3, 0.3, 0.3, 0);
			return !spent && casterHere();
		}

		@Override
		public void onMelee(ServerPlayer attacker, LivingEntity target) {
			if (spent || attacker != caster) return;
			spent = true;
			HeavenSpells.strikeAgain(caster, target, HEARTWOOD_BLOW_PER_RING * rings, QiElement.WOOD);
			HeavenSpells.push(caster.position(), target, 1.5);
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.OAK_LOG), HeavenSpells.middle(target), 20, 0.3, 0.1);
			HeavenSpells.particles(level, ParticleTypes.CRIT, HeavenSpells.middle(target), 8, 0.3, 0.2);
			HeavenSpells.sound(level, target.position(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 0.9f);
			HeavenSpells.sound(level, target.position(), SoundEvents.WOOD_BREAK, 1.0f, 0.8f);
		}
	}

	private static void set(LivingEntity e, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, double amount, AttributeModifier.Operation op) {
		AttributeInstance instance = e.getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(id);
		instance.addTransientModifier(new AttributeModifier(id, "Heartwood of Nine Rings", amount, op));
	}

	private static void unset(LivingEntity e, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id) {
		AttributeInstance instance = e.getAttribute(attribute);
		if (instance != null) instance.removeModifier(id);
	}

	// --- Pollen of Long Dreams ---

	/** Pollen of Long Dreams: a drifting cloud of golden pollen at the spot looked at, that puts foes to sleep. */
	private static void pollen(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		BlockHitResult hit = HeavenSpells.lookedBlock(p, POLLEN_RANGE);
		Vec3 centre = hit != null ? Vec3.atCenterOf(hit.getBlockPos()).add(0, 1, 0) : p.getEyePosition().add(p.getViewVector(1.0f).scale(POLLEN_RANGE));
		HeavenSpells.linger(new Pollen(p, centre));
		HeavenSpells.sound(level, centre, SoundEvents.BEE_POLLINATE, 1.2f, 0.8f);
		HeavenSpells.sound(level, centre, SoundEvents.FLOWERING_AZALEA_PLACE, 1.0f, 0.8f);
	}

	/** The pollen cloud, and the foes it has put to sleep (each until when). */
	static final class Pollen extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		final Vec3 centre;
		final Map<LivingEntity, Long> sleeping = new HashMap<>();
		final Set<LivingEntity> slept = new HashSet<>();
		final Set<LivingEntity> slowed = new HashSet<>();

		Pollen(ServerPlayer caster, Vec3 centre) {
			super(caster, POLLEN_TICKS + SLEEP_TICKS);
			this.centre = centre;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < POLLEN_TICKS) {
				if (age % 2 == 0) {
					for (int i = 0; i < 18; i++) {
						Vec3 at = centre.add(randomIn(POLLEN_RADIUS));
						level.sendParticles(HeavenSpells.dust(i < 14 ? POLLEN : PALE_GOLD, i < 14 ? 1.2f : 1.0f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
					}
					level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, centre.x, centre.y, centre.z, 2, POLLEN_RADIUS / 2, 1.5, POLLEN_RADIUS / 2, 0);
				}
				if (age % 20 == 0) HeavenSpells.sound(level, centre, SoundEvents.FLOWERING_AZALEA_STEP, 0.5f, 1.0f);
				for (LivingEntity e : HeavenSpells.aroundPoint(level, centre, POLLEN_RADIUS, e -> HeavenSpells.foe(caster, e))) {
					if (slept.contains(e) || Woken.of(e, now)) continue;
					if (HeavenSpells.greatBeast(e)) {
						if (slowed.add(e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, POLLEN_BEAST_TICKS, 1));
						continue;
					}
					slept.add(e);
					sleeping.put(e, now + SLEEP_TICKS);
					HeavenSpells.sound(level, e.position(), SoundEvents.FOX_SLEEP, 1.0f, 1.0f);
				}
			}
			for (java.util.Iterator<Map.Entry<LivingEntity, Long>> it = sleeping.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<LivingEntity, Long> entry = it.next();
				LivingEntity e = entry.getKey();
				if (!HeavenSpells.present(e, level) || now > entry.getValue()) {
					wake(e, false);
					it.remove();
					continue;
				}
				hold(e);
				if (age % 10 == 0) {
					level.sendParticles(ParticleTypes.FALLING_NECTAR, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 1, 0.1, 0, 0.1, 0);
					level.sendParticles(HeavenSpells.dust(POLLEN, 0.7f), e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 2, 0.2, 0.1, 0.2, 0);
				}
			}
			return age < POLLEN_TICKS || !sleeping.isEmpty();
		}

		/** Whether {@code e} sleeps under this cloud now (tests). */
		boolean asleep(LivingEntity e) {
			return sleeping.containsKey(e);
		}

		/** Asleep: no target, no path, no stepping, no jumping, a creeper's fuse put out. */
		private static void hold(LivingEntity e) {
			if (e instanceof Mob mob) {
				mob.setTarget(null);
				mob.setAggressive(false);
			}
			if (e instanceof Creeper creeper) creeper.setSwellDir(-1);
			HeavenSpells.still(e);
			e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 4, false, false));
			e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 4, false, false));
		}

		private void wake(LivingEntity e, boolean struck) {
			Woken.mark(e, level.getGameTime() + WOKEN_TICKS);
			if (struck && e.isAlive()) {
				HeavenSpells.particles(level, ParticleTypes.CRIT, HeavenSpells.middle(e), 8, 0.3, 0.2);
				HeavenSpells.particles(level, HeavenSpells.dust(PALE_GOLD, 1.0f), HeavenSpells.middle(e), 6, 0.3, 0);
				HeavenSpells.sound(level, e.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
			}
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			if (!sleeping.containsKey(target)) return amount;
			sleeping.remove(target);
			wake(target, true);
			return amount * WAKING_BLOW;
		}

		@Override
		public void end() {
			for (LivingEntity e : sleeping.keySet()) Woken.mark(e, level.getGameTime() + WOKEN_TICKS);
			sleeping.clear();
		}
	}

	/** The beings Pollen of Long Dreams can't put to sleep again yet, each until when (shared by every cloud). */
	private static final class Woken {
		static final Map<LivingEntity, Long> UNTIL = new java.util.WeakHashMap<>();

		static void mark(LivingEntity e, long until) {
			UNTIL.put(e, until);
		}

		static boolean of(LivingEntity e, long now) {
			Long until = UNTIL.get(e);
			return until != null && now < until;
		}
	}

	private static Vec3 randomIn(double radius) {
		java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
		double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * radius, h = (r.nextDouble() - 0.5) * radius;
		return new Vec3(Math.cos(a) * d, h, Math.sin(a) * d);
	}

	private static Vec3 randomFlat(double radius) {
		java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
		double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * radius;
		return new Vec3(Math.cos(a) * d, 0, Math.sin(a) * d);
	}

	// --- Myriad Thorn Thicket ---

	/** Myriad Thorn Thicket: a thicket of thorns round the spot looked at that tears whatever moves within it. */
	private static void thicket(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		BlockHitResult hit = HeavenSpells.lookedBlock(p, THICKET_RANGE);
		Vec3 centre = hit != null ? Vec3.atBottomCenterOf(hit.getBlockPos().above()) : p.position();
		HeavenSpells.linger(new Thicket(p, centre));
		for (float pitch : new float[]{0.6f, 0.8f, 1.0f}) HeavenSpells.sound(level, centre, SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.2f, pitch);
		HeavenSpells.sound(level, centre, SoundEvents.BONE_MEAL_USE, 1.0f, 0.8f);
	}

	/** The thicket: each being's way through it measured, and torn for it every half second. */
	private static final class Thicket extends HeavenSpells.Timed {
		final Vec3 centre;
		final Map<LivingEntity, Vec3> last = new HashMap<>();
		final Map<LivingEntity, Double> travelled = new HashMap<>();

		Thicket(ServerPlayer caster, Vec3 centre) {
			super(caster, THICKET_TICKS);
			this.centre = centre;
		}

		private boolean inside(LivingEntity e) {
			double dx = e.getX() - centre.x, dz = e.getZ() - centre.z;
			return dx * dx + dz * dz <= THICKET_RADIUS * THICKET_RADIUS && e.getY() >= centre.y - 2 && e.getY() <= centre.y + 4;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			if (age < 10) HeavenSpells.ring(level, centre.add(0, 0.2, 0), THICKET_RADIUS * (age + 1) / 10.0, HeavenSpells.dust(LEAF, 1.2f), 60);
			List<LivingEntity> in = level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(THICKET_RADIUS, 6, THICKET_RADIUS),
					e -> !HeavenSpells.spared(caster, e) && inside(e));
			Set<LivingEntity> present = new HashSet<>(in);
			last.keySet().removeIf(e -> !present.contains(e));
			travelled.keySet().removeIf(e -> !present.contains(e));
			for (LivingEntity e : in) {
				Vec3 was = last.put(e, e.position());
				if (was != null) {
					double d = was.distanceTo(e.position());
					if (d <= 4) travelled.merge(e, d, Double::sum);
				}
			}
			if (age % 10 == 0) {
				for (LivingEntity e : in) {
					double moved = travelled.getOrDefault(e, 0.0);
					travelled.put(e, 0.0);
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 1));
					float harm = (float) (Math.floor(Math.min(THICKET_MAX, THICKET_PER_BLOCK * moved) * 2) / 2);
					if (harm < 0.5f) continue;
					Vec3 v = e.getDeltaMovement();
					HeavenSpells.strikeAgain(caster, e, harm, QiElement.WOOD);
					e.setDeltaMovement(v); // the thorns tear; they don't throw
					e.hurtMarked = true;
					HeavenSpells.particles(level, HeavenSpells.block(Blocks.SWEET_BERRY_BUSH), e.position().add(0, 0.5, 0), 6, 0.3, 0);
					HeavenSpells.particles(level, ParticleTypes.CRIT, e.position().add(0, 0.5, 0), 3, 0.3, 0.1);
					HeavenSpells.sound(level, e.position(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 0.8f, 1.0f);
				}
				HeavenSpells.ring(level, centre.add(0, 0.2, 0), THICKET_RADIUS, HeavenSpells.dust(0x1F5E2A, 1.3f), 48);
			}
			if (age % 5 == 0) {
				for (int i = 0; i < 25; i++) {
					Vec3 at = centre.add(randomFlat(THICKET_RADIUS));
					for (int h = 0; h < 3; h++) level.sendParticles(HeavenSpells.dust(0x5A3A1A, 0.9f), at.x, at.y + h * 0.27, at.z, 1, 0, 0, 0, 0);
					if (i % 5 == 0) level.sendParticles(HeavenSpells.dust(0x1F5E2A, 0.9f), at.x, at.y + 0.9, at.z, 1, 0, 0, 0, 0);
				}
			}
			if (age % 40 == 0) HeavenSpells.sound(level, centre.add(randomFlat(THICKET_RADIUS)), SoundEvents.SWEET_BERRY_BUSH_BREAK, 0.4f, 1.0f);
			return true;
		}

		@Override
		public void end() {
			HeavenSpells.sound(level, centre, SoundEvents.AZALEA_LEAVES_BREAK, 1.0f, 0.8f);
		}
	}

	// --- Strangler Fig Seed ---

	/** Strangler Fig Seed: a seed shot into the being looked at, that weakens it and turns its healing to the caster. */
	private static void strangler(ServerPlayer p) {
		LivingEntity host = HeavenSpells.aimed(p, FIG_RANGE);
		if (host == null) return;
		ServerLevel level = p.serverLevel();
		HeavenSpells.line(level, HeavenSpells.fromHands(p), HeavenSpells.middle(host), HeavenSpells.dust(SEED, 1.0f), 0.5);
		HeavenSpells.strike(p, host, FIG_HARM, QiElement.WOOD);
		HeavenSpells.sound(level, p.position(), SoundEvents.SNIFFER_DROP_SEED, 1.0f, 1.2f);
		HeavenSpells.sound(level, host.position(), SoundEvents.HANGING_ROOTS_BREAK, 1.0f, 0.8f);
		HeavenSpells.sound(level, host.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
		if (!host.isAlive()) return;
		for (Fig old : HeavenSpells.lingering(null, Fig.class)) if (old.host == host) old.end = 0;
		HeavenSpells.linger(new Fig(p, host));
	}

	/** The fig, feeding on its host: weakening it, and every heal it would take flowing to the caster instead. */
	static final class Fig extends HeavenSpells.Timed implements HeavenSpells.HealListener {
		final LivingEntity host;

		Fig(ServerPlayer caster, LivingEntity host) {
			super(caster, FIG_TICKS);
			this.host = host;
			host.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FIG_TICKS, 0));
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(host, level)) return false;
			if (age % 4 == 0) {
				double h = (age % 40) / 40.0 * host.getBbHeight(), a = age * 0.5;
				level.sendParticles(HeavenSpells.dust(ROOT, 0.9f), host.getX() + Math.cos(a) * 0.55, host.getY() + h, host.getZ() + Math.sin(a) * 0.55, 1, 0, 0, 0, 0);
				level.sendParticles(HeavenSpells.dust(LEAF, 0.9f), host.getX() - Math.cos(a) * 0.55, host.getY() + h, host.getZ() - Math.sin(a) * 0.55, 1, 0, 0, 0, 0);
			}
			return true;
		}

		@Override
		public boolean allowHeal(LivingEntity entity, float amount) {
			if (entity != host) return true;
			// The fig takes it: the caster is healed in the host's stead, if it is near enough to be fed.
			if (casterHere() && caster.distanceToSqr(host) <= FIG_REACH * FIG_REACH && caster.getHealth() < caster.getMaxHealth()) {
				caster.heal(amount);
				HeavenSpells.line(level, HeavenSpells.middle(host), HeavenSpells.middle(caster), HeavenSpells.dust(0xE05A5A, 0.8f), 1.0);
				if (amount >= 1) HeavenSpells.sound(level, host.position(), SoundEvents.ROOTED_DIRT_HIT, 0.5f, 1.0f);
			}
			return false;
		}
	}

	// --- Rite of Returning Spring ---

	/** Rite of Returning Spring: the caster and every friendly being near are mended, and blessed so that their hurts grow back. */
	private static void rite(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		for (Rite old : HeavenSpells.lingering(p.getUUID(), Rite.class)) old.blessed.clear(); // its streams run on; this one blesses now
		Rite rite = new Rite(p);
		for (LivingEntity e : HeavenSpells.around(p, RITE_RADIUS, e -> HeavenSpells.friendly(p, e))) {
			rite.blessed.add(e);
			HeavenSpells.mend(e, RITE_MEND);
			HeavenSpells.particles(level, ParticleTypes.HAPPY_VILLAGER, HeavenSpells.middle(e), 10, 0.4, 0);
			HeavenSpells.sound(level, e.position(), SoundEvents.CHERRY_SAPLING_PLACE, 0.5f, 1.0f);
		}
		rite.blessed.add(p);
		HeavenSpells.mend(p, RITE_MEND);
		HeavenSpells.linger(rite);
		HeavenSpells.particles(level, ParticleTypes.CHERRY_LEAVES, p.position().add(0, 1, 0), 40, 1.0, 0.1);
		HeavenSpells.sound(level, p.position(), SoundEvents.CHERRY_LEAVES_PLACE, 1.2f, 1.0f);
		HeavenSpells.sound(level, p.position(), SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0f, 1.2f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BONE_MEAL_USE, 1.0f, 1.0f);
	}

	/** The blessing: those it holds, and the healing growing back from each hurt they took (who, how much left, ticks left). */
	static final class Rite extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		final Set<LivingEntity> blessed = new HashSet<>();
		final List<Object[]> streams = new ArrayList<>();
		final Vec3 from;

		Rite(ServerPlayer caster) {
			super(caster, RITE_TICKS);
			this.from = caster.position();
		}

		@Override
		public boolean tick(long now) {
			// Its streams outlast the blessing: it ends only when both are done.
			long at = level.getGameTime();
			if (at > end) blessed.clear();
			return run(at - start, at) && (at <= end || !streams.isEmpty());
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < 15) HeavenSpells.ring(level, from.add(0, 0.2, 0), RITE_RADIUS * (age + 1) / 15.0, HeavenSpells.dust(LEAF, 1.5f), 64);
			if (age % 10 == 0) {
				for (LivingEntity e : blessed) {
					if (e.isAlive()) level.sendParticles(ParticleTypes.CHERRY_LEAVES, e.getX(), e.getY() + 2, e.getZ(), 1, 0.3, 0, 0.3, 0);
				}
			}
			for (java.util.Iterator<Object[]> it = streams.iterator(); it.hasNext(); ) {
				Object[] stream = it.next();
				LivingEntity e = (LivingEntity) stream[0];
				float left = (Float) stream[1];
				int ticks = (Integer) stream[2];
				if (!e.isAlive() || ticks <= 0) {
					it.remove();
					continue;
				}
				float piece = left / ticks;
				HeavenSpells.mend(e, piece);
				stream[1] = left - piece;
				stream[2] = ticks - 1;
				if (ticks % 5 == 0) level.sendParticles(HeavenSpells.dust(JADE, 0.8f), e.getX(), e.getY() + 0.5, e.getZ(), 2, 0.2, 0.3, 0.2, 0);
			}
			return true;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			if (amount > 0 && blessed.contains(target) && !HeavenSpells.inHurtTime(target)) {
				streams.add(new Object[]{target, amount * RITE_SHARE, RITE_STREAM_TICKS});
				HeavenSpells.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.3f, 1.5f);
			}
			return amount;
		}
	}

	// --- Heaven-Shading Canopy ---

	/** Heaven-Shading Canopy: the World Tree's canopy spreads round the caster and catches every hostile missile in its leaves. */
	private static void canopy(ServerPlayer p) {
		HeavenSpells.endLingering(p.getUUID(), Canopy.class);
		HeavenSpells.linger(new Canopy(p));
		ServerLevel level = p.serverLevel();
		HeavenSpells.sound(level, p.position(), SoundEvents.AZALEA_LEAVES_PLACE, 1.5f, 0.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.4f);
	}

	/** The canopy over the caster, catching the missiles that enter it (each once). */
	static final class Canopy extends HeavenSpells.Timed {
		final Set<Integer> caught = new HashSet<>();

		Canopy(ServerPlayer caster) {
			super(caster, CANOPY_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			Vec3 at = caster.position();
			boolean fed = false;
			for (Projectile missile : level.getEntitiesOfClass(Projectile.class, caster.getBoundingBox().inflate(CANOPY_RADIUS),
					m -> m.distanceToSqr(caster) <= CANOPY_RADIUS * CANOPY_RADIUS && !caught.contains(m.getId()))) {
				if (missile.getOwner() == caster || missile.getOwner() instanceof LivingEntity owner && HeavenSpells.ally(caster, owner)) continue;
				if (missile.getDeltaMovement().lengthSqr() < 0.01) continue; // lying still: nothing to catch
				caught.add(missile.getId());
				Vec3 where = missile.position();
				if (missile instanceof AbstractArrow arrow) {
					arrow.setDeltaMovement(0, -0.05, 0); // dropped, its force spent in the leaves
					arrow.hurtMarked = true;
				} else {
					missile.discard();
				}
				PlayerCultivation c = CultivationManager.get(caster);
				c.setQi(Math.min(c.maxQi(), c.getQi() + CANOPY_QI));
				fed = true;
				HeavenSpells.particles(level, HeavenSpells.block(Blocks.AZALEA_LEAVES), where, 12, 0.3, 0.05);
				HeavenSpells.sound(level, where, SoundEvents.AZALEA_LEAVES_HIT, 1.0f, 1.0f);
			}
			if (fed) CultivationManager.sync(caster);
			if (age % 5 == 0) {
				for (int i = 0; i < 40; i++) {
					Vec3 leaf = at.add(randomFlat(7)).add(0, 7, 0);
					level.sendParticles(HeavenSpells.dust(i % 2 == 0 ? DEEP_LEAF : LEAF, 1.6f), leaf.x, leaf.y, leaf.z, 1, 0, 0, 0, 0);
				}
				level.sendParticles(ParticleTypes.COMPOSTER, at.x, at.y + 7, at.z, 6, 5, 0.2, 5, 0);
			}
			if (age % 20 == 0) HeavenSpells.ring(level, at.add(0, 0.2, 0), CANOPY_RADIUS, HeavenSpells.dust(LEAF, 1.2f), 96);
			if (age % 40 == 0) HeavenSpells.sound(level, at, SoundEvents.AZALEA_LEAVES_STEP, 0.4f, 1.0f);
			return true;
		}

		@Override
		public void end() {
			if (caster.isAlive()) HeavenSpells.sound(level, caster.position(), SoundEvents.AZALEA_LEAVES_BREAK, 1.0f, 0.8f);
		}
	}

	// --- The World Tree Rises ---

	/** The World Tree Rises: the World Tree planted where the caster looks, mending allies beneath it and striking foes with its roots. */
	private static void worldTree(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		BlockHitResult hit = HeavenSpells.lookedBlock(p, TREE_RANGE);
		Vec3 root = hit != null ? Vec3.atBottomCenterOf(hit.getBlockPos().above()) : p.position();
		HeavenSpells.endLingering(p.getUUID(), WorldTree.class); // one tree at a time: the old one withers
		HeavenSpells.linger(new WorldTree(p, root));
		HeavenSpells.sound(level, root, SoundEvents.BONE_MEAL_USE, 1.5f, 0.5f);
		HeavenSpells.sound(level, root, SoundEvents.ROOTED_DIRT_PLACE, 2.0f, 0.5f);
		HeavenSpells.sound(level, root, SoundEvents.WARDEN_DIG, 0.8f, 1.0f);
	}

	/** The World Tree: growing, then mending its allies and striking its foes, withering when its time is out. */
	static final class WorldTree extends HeavenSpells.Timed implements HeavenSpells.HealListener {
		final Vec3 root;
		final List<LivingEntity> telegraphed = new ArrayList<>();

		WorldTree(ServerPlayer caster, Vec3 root) {
			super(caster, TREE_TICKS);
			this.root = root;
		}

		private boolean under(LivingEntity e) {
			double dx = e.getX() - root.x, dz = e.getZ() - root.z;
			return dx * dx + dz * dz <= TREE_RADIUS * TREE_RADIUS && e.getY() >= root.y - 8 && e.getY() <= root.y + TREE_HEIGHT;
		}

		private List<LivingEntity> beneath(java.util.function.Predicate<LivingEntity> which) {
			return level.getEntitiesOfClass(LivingEntity.class, new AABB(root, root).inflate(TREE_RADIUS, TREE_HEIGHT, TREE_RADIUS),
					e -> under(e) && which.test(e));
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere() || caster.position().distanceToSqr(root) > TREE_LEASH * TREE_LEASH) return false;
			double height = Math.min(TREE_HEIGHT, age * 1.2);
			if (age % 4 == 0) {
				for (double h = 0; h < height; h += 0.25) {
					double a = h * 1.3 + age * 0.05;
					level.sendParticles(HeavenSpells.dust(((int) (h * 4)) % 2 == 0 ? SEED : BARK, 2.0f), root.x + Math.cos(a) * 1.5, root.y + h,
							root.z + Math.sin(a) * 1.5, 1, 0, 0, 0, 0);
				}
			}
			if (age >= TREE_GROWTH && age % 5 == 0) {
				for (int i = 0; i < 80; i++) {
					Vec3 leaf = root.add(randomFlat(TREE_RADIUS)).add(0, TREE_HEIGHT, 0);
					level.sendParticles(HeavenSpells.dust(i % 2 == 0 ? DEEP_LEAF : LEAF, 2.0f), leaf.x, leaf.y, leaf.z, 1, 0, 0, 0, 0);
				}
				level.sendParticles(ParticleTypes.COMPOSTER, root.x, root.y + TREE_HEIGHT - 1, root.z, 20, TREE_RADIUS / 2, 0.5, TREE_RADIUS / 2, 0);
			}
			if (age == TREE_GROWTH) HeavenSpells.sound(level, root, SoundEvents.BEACON_ACTIVATE, 2.0f, 0.6f);
			if (age % 20 == 0) HeavenSpells.ring(level, root.add(0, 0.2, 0), TREE_RADIUS, HeavenSpells.dust(LEAF, 1.2f), 90);
			if (age % 60 == 0) HeavenSpells.sound(level, root, SoundEvents.AZALEA_LEAVES_STEP, 0.6f, 1.0f);
			if (age < TREE_GROWTH) return true;
			if ((age - TREE_GROWTH) % 20 == 0) {
				for (LivingEntity ally : beneath(e -> HeavenSpells.friendly(caster, e))) {
					HeavenSpells.mend(ally, TREE_MEND);
					HeavenSpells.particles(level, ParticleTypes.HAPPY_VILLAGER, HeavenSpells.middle(ally), 3, 0.3, 0);
				}
			}
			// The roots: telegraphed under each foe half a second before they erupt.
			long beat = (age - TREE_GROWTH) % TREE_STRIKE_EVERY;
			if (beat == TREE_STRIKE_EVERY - 10) {
				telegraphed.clear();
				List<LivingEntity> foes = beneath(e -> HeavenSpells.enemy(caster, e));
				foes.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(root)));
				for (LivingEntity foe : foes.subList(0, Math.min(TREE_STRIKES, foes.size()))) {
					telegraphed.add(foe);
					HeavenSpells.ring(level, foe.position().add(0, 0.1, 0), 1.0, HeavenSpells.block(Blocks.ROOTED_DIRT), 12);
				}
			}
			if (beat == 0 && age > TREE_GROWTH) {
				for (LivingEntity foe : telegraphed) {
					if (!HeavenSpells.present(foe, level) || !under(foe)) continue;
					HeavenSpells.strikeAgain(caster, foe, TREE_ROOT_HARM, QiElement.WOOD);
					Vec3 v = foe.getDeltaMovement();
					if (!HeavenSpells.greatBeast(foe)) HeavenSpells.move(foe, new Vec3(v.x, 0.6, v.z));
					for (double h = 0; h < 2; h += 0.2) level.sendParticles(HeavenSpells.dust(ROOT, 1.8f), foe.getX(), foe.getY() + h, foe.getZ(), 1, 0.15, 0, 0.15, 0);
					HeavenSpells.particles(level, HeavenSpells.block(Blocks.ROOTED_DIRT), foe.position().add(0, 0.5, 0), 20, 0.4, 0.2);
					HeavenSpells.sound(level, foe.position(), SoundEvents.EVOKER_FANGS_ATTACK, 1.0f, 0.6f);
					HeavenSpells.sound(level, foe.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 0.8f);
				}
				telegraphed.clear();
			}
			return true;
		}

		@Override
		public boolean allowHeal(LivingEntity entity, float amount) {
			// Foes beneath the tree can't heal (once it has grown).
			return level.getGameTime() - start < TREE_GROWTH || entity.level() != level || !under(entity) || !HeavenSpells.enemy(caster, entity);
		}

		@Override
		public void end() {
			for (int i = 0; i < 60; i++) {
				Vec3 leaf = root.add(randomFlat(TREE_RADIUS)).add(0, TREE_HEIGHT * java.util.concurrent.ThreadLocalRandom.current().nextDouble(), 0);
				level.sendParticles(HeavenSpells.dust(0x8C7A3A, 1.6f), leaf.x, leaf.y, leaf.z, 1, 0, 0, 0, 0);
			}
			for (int i = 0; i < 4; i++) HeavenSpells.sound(level, root.add(randomFlat(TREE_RADIUS / 2)), SoundEvents.AZALEA_LEAVES_BREAK, 1.0f, 0.8f);
			HeavenSpells.sound(level, root, SoundEvents.WOOD_BREAK, 1.5f, 0.5f);
		}
	}

	private WoodSpells() {}
}
