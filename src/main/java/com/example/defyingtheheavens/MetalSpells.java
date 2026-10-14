package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The metal methods' twelve spells (see HeavenSpells for what they share): a thread of sword qi that goes out and comes home, a
 * bell of iron qi, a lodestone brand that draws every missile, a pull that makes weapons kneel, a finger that splits armour,
 * golden lightning leaping from foe to foe, nine circling blades, a toll whose hearers share their wounds, an edge that severs
 * fate's blessings, ten thousand swords from the sky, a golden body that will not break, and the four gates' immortal-slaying array.
 */
public final class MetalSpells {
	static final int GOLD = 0xFFC83C, WHITE = 0xFFFFFF, SEVERED = 0x6E6E6E, TARNISH = 0x8C6A2A;

	/** Swallow-Return Thread: reach, speed out and home, harm each way, how long at most. */
	static final double THREAD_REACH = 14, THREAD_OUT = 1.0, THREAD_HOME = 1.2;
	static final float THREAD_HARM = 4;
	static final int THREAD_TICKS = 60;
	/** Iron Bell Cover: how long, the harm it takes off each blow. */
	static final int BELL_TICKS = 240;
	static final float BELL_STOPS = 2;
	/** Lodestone Brand: reach, harm, how long, how near a missile must pass. */
	static final double BRAND_RANGE = 24, BRAND_PULL = 8;
	static final float BRAND_HARM = 10;
	static final int BRAND_TICKS = 200;
	/** Hundred Weapons Kneel: reach, harm, the numbing, the creatures' empty hands, a player's seized items. */
	static final double KNEEL_RADIUS = 10;
	static final float KNEEL_HARM = 8, NUMB_SHARE = 0.5f;
	static final int NUMB_TICKS = 100, NO_PICKUP_TICKS = 160, SEIZED_TICKS = 100;
	/** Armour-Splitting Finger: reach, harm, how long a crack lasts, its deepest, the harm each stage adds, how often a blow widens it. */
	static final double FINGER_RANGE = 24;
	static final float FINGER_HARM = 20, CRACK_HARM = 0.05f;
	static final int CRACK_TICKS = 200, CRACK_STAGES = 5, CRACK_GROWS_EVERY = 10;
	/** Leaping Golden Lightning: reach, the leap (from water), the harm of each strike in turn, the ticks between. */
	static final double LIGHTNING_RANGE = 28, LEAP = 7, LEAP_WATER = 14;
	static final float[] LIGHTNING_HARM = {26, 18, 16, 15, 13, 12};
	static final int LEAP_TICKS = 2;
	/** Nine Circling Blades: how many, how long, the ring, how near a foe draws one, speed, harm, the knockback. */
	static final int BLADES = 9, BLADES_TICKS = 800, BLADE_FLIGHT = 12;
	static final double BLADE_RING = 2.2, BLADE_REACH = 5, BLADE_SPEED = 1.5, BLADE_LEASH = 8;
	static final float BLADE_HARM = 26;
	/** Thousand-Echo Toll: reach, harm, how long they resonate, the echo's share and reach. */
	static final double TOLL_RADIUS = 16, ECHO_REACH = 24;
	static final float TOLL_HARM = 22, ECHO_SHARE = 0.4f;
	static final int TOLL_TICKS = 200;
	/** Fate-Severing Edge: reach, harm, how long no blessing takes hold. */
	static final double EDGE_RANGE = 32;
	static final float EDGE_HARM = 60;
	static final int EDGE_TICKS = 200;
	/** Ten Thousand Swords Descend: reach, size, the gathering, the waves (how many, each how long, swords in each), harm, the cut's reach. */
	static final double SWORDS_RANGE = 40, SWORDS_RADIUS = 10, SWORD_CUT = 1.5, SWORD_HEIGHT = 20, SWORD_FALL = 3;
	static final int SWORDS_GATHER = 20, SWORD_WAVES = 6, SWORD_WAVE_TICKS = 20, SWORD_GRACE = 20;
	static final int[] SWORD_LAUNCHES = {0, 2, 3, 5, 7, 8, 10, 12, 13, 15, 17, 18};
	static final float SWORD_HARM = 8;
	/** Indestructible Golden Body: how long, the most of its greatest health one blow may take. */
	static final int GOLDEN_TICKS = 200;
	static final float GOLDEN_CAP = 0.1f;
	/** Four Gates Immortal-Slaying Array: reach, size (each side of the middle), the descent, how long it stands, the cut, the volleys. */
	static final double ARRAY_RANGE = 48, ARRAY_HALF = 14;
	static final int ARRAY_DESCENT = 10, ARRAY_TICKS = 300, ARRAY_VOLLEY_EVERY = 60, ARRAY_CUT_EVERY = 20;
	static final float ARRAY_CUT = 60, ARRAY_VOLLEY = 16;

	private static final UUID CRACK_ARMOUR_ID = UUID.fromString("5d8e1f2a-7b3c-4e6d-9a0f-1c2b3d4e5f61");
	private static final UUID CRACK_TOUGHNESS_ID = UUID.fromString("8b2c4d6e-1f3a-4b5c-8d7e-9f0a1b2c3d4e");
	private static final UUID GOLDEN_STAND_ID = UUID.fromString("c4a7e9b1-2d3f-4a5b-9c6d-7e8f9a0b1c2d");

	/** Whether {@code spell} can take hold for {@code p} now; refused, nothing is paid. */
	static boolean canCast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case LODESTONE_BRAND -> {
				return HeavenSpells.aimed(p, BRAND_RANGE) != null || HeavenSpells.refuse(p);
			}
			case ARMOUR_SPLITTING_FINGER -> {
				return HeavenSpells.aimed(p, FINGER_RANGE) != null || HeavenSpells.refuse(p);
			}
			case LEAPING_GOLDEN_LIGHTNING -> {
				return HeavenSpells.aimed(p, LIGHTNING_RANGE) != null || HeavenSpells.refuse(p);
			}
			case FATE_SEVERING_EDGE -> {
				return HeavenSpells.aimed(p, EDGE_RANGE) != null || HeavenSpells.refuse(p);
			}
			case TEN_THOUSAND_SWORDS_DESCEND -> {
				return HeavenSpells.lookedBlock(p, SWORDS_RANGE) != null || HeavenSpells.refuse(p);
			}
			default -> {
				return true;
			}
		}
	}

	/** Casts one of the metal spells (paid for). */
	static void cast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case SWALLOW_RETURN_THREAD -> thread(p);
			case IRON_BELL_COVER -> bell(p);
			case LODESTONE_BRAND -> brand(p);
			case HUNDRED_WEAPONS_KNEEL -> kneel(p);
			case ARMOUR_SPLITTING_FINGER -> finger(p);
			case LEAPING_GOLDEN_LIGHTNING -> lightning(p);
			case NINE_CIRCLING_BLADES -> blades(p);
			case THOUSAND_ECHO_TOLL -> toll(p);
			case FATE_SEVERING_EDGE -> edge(p);
			case TEN_THOUSAND_SWORDS_DESCEND -> swords(p);
			case INDESTRUCTIBLE_GOLDEN_BODY -> golden(p);
			case FOUR_GATES_IMMORTAL_SLAYING_ARRAY -> array(p);
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
	}

	// --- Shared ---

	/** A jagged bolt of gold and white from {@code a} to {@code b}. */
	private static void bolt(ServerLevel level, Vec3 a, Vec3 b) {
		java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
		int segments = (int) Math.max(4, a.distanceTo(b) / 1.2);
		Vec3 from = a;
		for (int i = 1; i <= segments; i++) {
			Vec3 to = i == segments ? b : a.add(b.subtract(a).scale(i / (double) segments)).add(r.nextDouble(-0.35, 0.35), r.nextDouble(-0.35, 0.35), r.nextDouble(-0.35, 0.35));
			HeavenSpells.line(level, from, to, HeavenSpells.dust(WHITE, 0.7f), 0.2);
			HeavenSpells.line(level, from, to, HeavenSpells.dust(GOLD, 1.3f), 0.5);
			from = to;
		}
	}

	private static void set(LivingEntity e, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, String name, double amount, AttributeModifier.Operation op) {
		AttributeInstance instance = e.getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(id);
		instance.addTransientModifier(new AttributeModifier(id, name, amount, op));
	}

	private static void unset(LivingEntity e, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id) {
		AttributeInstance instance = e.getAttribute(attribute);
		if (instance != null) instance.removeModifier(id);
	}

	/** Whether {@code target} can be seen from {@code from} (no block between). */
	private static boolean clear(ServerLevel level, Vec3 from, Vec3 to, LivingEntity viewer) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer)).getType() == HitResult.Type.MISS;
	}

	// --- Swallow-Return Thread ---

	/** Swallow-Return Thread: a thread of sword qi flicked along the look, piercing all in its way, that turns and flies home. */
	private static void thread(ServerPlayer p) {
		HeavenSpells.linger(new ReturnThread(p));
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.TRIDENT_THROW, 0.6f, 1.7f);
	}

	/** The thread: going out until a wall or its reach, then home to the caster, each being pierced once each way. */
	static final class ReturnThread extends HeavenSpells.Timed {
		final Vec3 out;
		final Set<LivingEntity> outward = new HashSet<>(), homeward = new HashSet<>();
		Vec3 at;
		double flown;
		boolean home;

		ReturnThread(ServerPlayer caster) {
			super(caster, THREAD_TICKS);
			this.at = caster.getEyePosition().subtract(0, 0.3, 0);
			this.out = caster.getViewVector(1.0f).normalize().scale(THREAD_OUT);
		}

		/** Whether it has turned for home (tests). */
		boolean homeward() {
			return home;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			Vec3 hand = caster.getEyePosition().subtract(0, 0.3, 0);
			Vec3 next;
			if (!home) {
				next = at.add(out);
				BlockHitResult wall = level.clip(new ClipContext(at, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
				boolean turn = false;
				if (wall.getType() != HitResult.Type.MISS) {
					next = wall.getLocation();
					turn = true;
					HeavenSpells.sound(level, next, SoundEvents.TRIDENT_HIT_GROUND, 0.5f, 1.8f);
				}
				flown += THREAD_OUT;
				if (flown >= THREAD_REACH) turn = true;
				pierce(at, next, outward);
				if (turn) {
					home = true;
					HeavenSpells.ring(level, next, 0.4, HeavenSpells.dust(GOLD, 0.8f), 8);
				}
			} else {
				Vec3 d = hand.subtract(at);
				if (d.length() <= 1.0 + THREAD_HOME) {
					pierce(at, hand, homeward);
					HeavenSpells.particles(level, ParticleTypes.ENCHANTED_HIT, hand, 4, 0.1, 0.05);
					HeavenSpells.sound(level, caster.position(), SoundEvents.TRIDENT_RETURN, 0.7f, 1.5f);
					return false;
				}
				next = at.add(d.normalize().scale(THREAD_HOME));
				pierce(at, next, homeward);
			}
			HeavenSpells.line(level, at, next, HeavenSpells.dust(WHITE, 0.6f), 0.25);
			level.sendParticles(HeavenSpells.dust(GOLD, 1.0f), next.x, next.y, next.z, 1, 0, 0, 0, 0);
			at = next;
			return level.isLoaded(BlockPos.containing(at));
		}

		private void pierce(Vec3 from, Vec3 to, Set<LivingEntity> struck) {
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1), e -> !HeavenSpells.spared(caster, e) && !(e instanceof ArmorStand))) {
				if (struck.contains(e)) continue;
				AABB body = e.getBoundingBox().inflate(0.3);
				if (!body.contains(from) && body.clip(from, to).isEmpty()) continue;
				struck.add(e);
				HeavenSpells.strikeAgain(caster, e, THREAD_HARM, QiElement.METAL);
				HeavenSpells.particles(level, ParticleTypes.CRIT, HeavenSpells.middle(e), 6, 0.2, 0.1);
				HeavenSpells.particles(level, ParticleTypes.ENCHANTED_HIT, HeavenSpells.middle(e), 4, 0.2, 0.1);
				HeavenSpells.sound(level, e.position(), SoundEvents.TRIDENT_HIT, 0.6f, 1.6f);
			}
		}
	}

	// --- Iron Bell Cover ---

	/** Iron Bell Cover: a bell of iron qi hung over the caster, taking a little off every blow a being deals it. */
	private static void bell(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		List<Bell> hung = HeavenSpells.lingering(p.getUUID(), Bell.class);
		if (!hung.isEmpty()) hung.get(0).end = level.getGameTime() + BELL_TICKS;
		else HeavenSpells.linger(new Bell(p));
		for (int i = 0; i < 12; i++) {
			double h = i * 2.6 / 11, r = 1.2 - (1.2 - 0.4) * i / 11;
			HeavenSpells.ring(level, p.position().add(0, h, 0), r, HeavenSpells.dust(GOLD, 1.0f), 16);
		}
		HeavenSpells.particles(level, HeavenSpells.dust(WHITE, 1.2f), p.position().add(0, 2.7, 0), 3, 0.1, 0);
		HeavenSpells.sound(level, p.position(), SoundEvents.BELL_BLOCK, 0.8f, 0.9f);
	}

	/** The bell over its caster: each blow by a being (or its missile) two the less. */
	static final class Bell extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		Bell(ServerPlayer caster) {
			super(caster, BELL_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age % 10 == 0) HeavenSpells.ring(level, caster.position().add(0, 0.1, 0), 1.1, HeavenSpells.dust(GOLD, 0.7f), 12);
			return true;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			if (target != caster || source.getEntity() == null || HeavenPassives.untouchedByWards(source)) return amount;
			float left = Math.max(0, amount - BELL_STOPS);
			HeavenSpells.particles(level, ParticleTypes.ENCHANTED_HIT, HeavenSpells.middle(caster), 6, 0.3, 0.05);
			HeavenSpells.sound(level, caster.position(), left == 0 ? SoundEvents.BELL_BLOCK : SoundEvents.BELL_RESONATE, left == 0 ? 0.6f : 0.5f, left == 0 ? 1.7f : 1.6f);
			return left;
		}

		@Override
		public void end() {
			if (caster.isAlive()) HeavenSpells.sound(level, caster.position(), SoundEvents.ARMOR_EQUIP_IRON, 0.4f, 0.8f);
		}
	}

	// --- Lodestone Brand ---

	/** Lodestone Brand: the first being looked at branded with lodestone qi, every missile near bending into it. */
	private static void brand(ServerPlayer p) {
		LivingEntity target = HeavenSpells.aimed(p, BRAND_RANGE);
		if (target == null) return;
		ServerLevel level = p.serverLevel();
		HeavenSpells.line(level, HeavenSpells.fromHands(p), HeavenSpells.middle(target), HeavenSpells.dust(WHITE, 0.5f), 0.5);
		HeavenSpells.strike(p, target, BRAND_HARM, QiElement.METAL);
		HeavenSpells.particles(level, ParticleTypes.ELECTRIC_SPARK, HeavenSpells.middle(target), 10, 0.3, 0.1);
		HeavenSpells.sound(level, target.position(), SoundEvents.CHAIN_PLACE, 1.0f, 1.5f);
		HeavenSpells.sound(level, target.position(), SoundEvents.TRIDENT_HIT, 1.0f, 1.0f);
		if (!target.isAlive()) return;
		for (Brand old : HeavenSpells.lingering(null, Brand.class)) if (old.target == target) old.end = 0;
		HeavenSpells.linger(new Brand(p, target));
	}

	/** The brand: each missile flying near its bearer turned to strike it. */
	static final class Brand extends HeavenSpells.Timed {
		final LivingEntity target;

		Brand(ServerPlayer caster, LivingEntity target) {
			super(caster, BRAND_TICKS);
			this.target = target;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			Vec3 heart = HeavenSpells.middle(target);
			for (Projectile missile : level.getEntitiesOfClass(Projectile.class, target.getBoundingBox().inflate(BRAND_PULL),
					m -> m.getOwner() != target && m.distanceToSqr(heart) <= BRAND_PULL * BRAND_PULL)) {
				Vec3 v = missile.getDeltaMovement();
				double speed = v.length();
				if (speed < 0.2) continue; // lying still, or stuck fast
				Vec3 toward = heart.subtract(missile.position());
				if (toward.lengthSqr() < 1.0e-4) continue;
				missile.setDeltaMovement(toward.normalize().scale(Math.max(speed, 0.8)));
				missile.hurtMarked = true;
				if (age % 2 == 0) level.sendParticles(HeavenSpells.dust(GOLD, 0.6f), missile.getX(), missile.getY(), missile.getZ(), 1, 0, 0, 0, 0);
			}
			if (age % 6 == 0) {
				for (int i = 0; i < 6; i++) {
					double a = Math.PI * 2 * i / 6 + age * 0.1;
					level.sendParticles(HeavenSpells.dust(GOLD, 0.8f), heart.x + Math.cos(a) * 0.6, heart.y, heart.z + Math.sin(a) * 0.6, 1, 0, 0, 0, 0);
				}
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, heart.x, heart.y, heart.z, 1, 0.3, 0.3, 0.3, 0);
			}
			return true;
		}
	}

	// --- Hundred Weapons Kneel ---

	/** Hundred Weapons Kneel: a wrench of lodestone qi at every enemy near, harming and numbing them, tearing creatures' weapons away. */
	private static void kneel(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Numbed numbed = new Numbed(p);
		for (LivingEntity e : HeavenSpells.around(p, KNEEL_RADIUS, e -> HeavenSpells.enemy(p, e) && !(e instanceof ArmorStand)
				&& !(e instanceof Player other && other.isCreative()))) {
			HeavenSpells.strike(p, e, KNEEL_HARM, QiElement.METAL);
			if (!e.isAlive()) continue;
			numbed.held.add(e);
			for (int t = 0; t < 3; t++) HeavenSpells.line(level, e.position().add(0, e.getBbHeight() * 0.6, 0), p.position().add(0, 1.2, 0), HeavenSpells.dust(GOLD, 0.7f), 0.5);
			HeavenSpells.particles(level, ParticleTypes.ELECTRIC_SPARK, e.position().add(0, e.getBbHeight() * 0.6, 0), 6, 0.2, 0.05);
			if (e instanceof ServerPlayer other) {
				// A player's things are never taken: they seize up a while instead.
				for (ItemStack held : new ItemStack[]{other.getMainHandItem(), other.getOffhandItem()}) {
					if (!held.isEmpty()) other.getCooldowns().addCooldown(held.getItem(), SEIZED_TICKS);
				}
				boolean shield = other.isUsingItem() && other.getUseItem().is(Items.SHIELD);
				if (other.isUsingItem()) other.stopUsingItem();
				HeavenSpells.sound(level, other.position(), shield ? SoundEvents.SHIELD_BREAK : SoundEvents.ARMOR_EQUIP_CHAIN, 1.0f, shield ? 1.2f : 0.6f);
			} else if (e instanceof Mob mob && !(e instanceof EnderDragon) && !(e instanceof WitherBoss)) {
				disarm(p, mob, level, numbed);
			}
		}
		HeavenSpells.linger(numbed);
		HeavenSpells.sound(level, p.position(), SoundEvents.ANVIL_PLACE, 0.6f, 1.5f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 0.7f);
	}

	/** A creature's held weapons torn away: what it picked up falls to the ground; what it was born with shatters (so none is farmed). */
	private static void disarm(ServerPlayer p, Mob mob, ServerLevel level, Numbed numbed) {
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
			ItemStack held = mob.getItemBySlot(slot);
			if (held.isEmpty()) continue;
			Vec3 hand = mob.position().add(0, mob.getBbHeight() * 0.6, 0);
			// A creature keeps what it picks up for good (it grows persistent then); what it spawned holding is its own.
			if (mob.isPersistenceRequired()) {
				ItemEntity dropped = new ItemEntity(level, hand.x, hand.y, hand.z, held.copy());
				dropped.setPickUpDelay(40);
				Vec3 toward = p.position().subtract(hand).multiply(1, 0, 1);
				dropped.setDeltaMovement((toward.lengthSqr() < 1.0e-4 ? Vec3.ZERO : toward.normalize().scale(0.25)).add(0, 0.2, 0));
				level.addFreshEntity(dropped);
				HeavenSpells.sound(level, hand, SoundEvents.ARMOR_EQUIP_CHAIN, 1.0f, 0.8f);
			} else {
				HeavenSpells.particles(level, new ItemParticleOption(ParticleTypes.ITEM, held), hand, 12, 0.2, 0.1);
				HeavenSpells.sound(level, hand, SoundEvents.ITEM_BREAK, 1.0f, 0.9f);
			}
			mob.setItemSlot(slot, ItemStack.EMPTY);
		}
		if (mob.canPickUpLoot()) {
			mob.setCanPickUpLoot(false);
			numbed.lootless.add(mob);
		}
	}

	/** The numbed (their own blows halved), and the creatures kept from picking anything up a while longer. */
	static final class Numbed extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		final Set<LivingEntity> held = new HashSet<>();
		final Set<Mob> lootless = new HashSet<>();

		Numbed(ServerPlayer caster) {
			super(caster, NO_PICKUP_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == NUMB_TICKS) held.clear();
			if (age % 10 == 0) {
				for (LivingEntity e : held) if (e.isAlive()) level.sendParticles(HeavenSpells.dust(WHITE, 0.5f), e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 2, 0.3, 0.1, 0.3, 0);
			}
			return !held.isEmpty() || !lootless.isEmpty() && age < NO_PICKUP_TICKS;
		}

		/** Whether {@code e}'s blows are numbed now (tests). */
		boolean numbs(LivingEntity e) {
			return held.contains(e);
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			return source.getEntity() instanceof LivingEntity striker && source.getDirectEntity() == striker && held.contains(striker) ? amount * NUMB_SHARE : amount;
		}

		@Override
		public void end() {
			for (Mob mob : lootless) if (mob.isAlive()) mob.setCanPickUpLoot(true);
			lootless.clear();
			held.clear();
		}
	}

	// --- Armour-Splitting Finger ---

	/** Armour-Splitting Finger: a finger of golden qi that harms the first being looked at and cracks its armour, wider with every blow. */
	private static void finger(ServerPlayer p) {
		LivingEntity target = HeavenSpells.aimed(p, FINGER_RANGE);
		if (target == null) return;
		ServerLevel level = p.serverLevel();
		Vec3 tip = p.getEyePosition().subtract(0, 0.3, 0).add(new Vec3(-p.getViewVector(1.0f).z, 0, p.getViewVector(1.0f).x).normalize().scale(0.35));
		HeavenSpells.line(level, tip, HeavenSpells.middle(target), HeavenSpells.dust(WHITE, 0.5f), 0.3);
		HeavenSpells.line(level, tip, HeavenSpells.middle(target), HeavenSpells.dust(GOLD, 0.9f), 1.5);
		HeavenSpells.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_CRIT, 0.8f, 1.5f);
		Crack crack = HeavenSpells.lingering(null, Crack.class).stream().filter(c -> c.target == target).findFirst().orElse(null);
		if (crack != null) crack.striking = true;
		HeavenSpells.strike(p, target, FINGER_HARM, QiElement.METAL);
		if (crack != null) crack.striking = false;
		HeavenSpells.particles(level, ParticleTypes.CRIT, HeavenSpells.middle(target), 12, 0.3, 0.1);
		HeavenSpells.particles(level, ParticleTypes.ENCHANTED_HIT, HeavenSpells.middle(target), 8, 0.3, 0.1);
		HeavenSpells.particles(level, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_CHESTPLATE)), HeavenSpells.middle(target), 6, 0.3, 0.1);
		HeavenSpells.sound(level, target.position(), SoundEvents.ANVIL_BREAK, 0.6f, 1.4f);
		if (!target.isAlive()) return;
		if (crack == null) {
			HeavenSpells.linger(new Crack(p, target));
		} else {
			crack.end = level.getGameTime() + CRACK_TICKS;
			crack.widen();
		}
	}

	/** A being's cracked armour: its stage (each stripping a fifth of its armour and adding to its harm), widened by blows. */
	static final class Crack extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		final LivingEntity target;
		int stage;
		long lastWidened;
		boolean striking;

		Crack(ServerPlayer caster, LivingEntity target) {
			super(caster, CRACK_TICKS);
			this.target = target;
			widen();
		}

		/** The crack's stage now (tests). */
		int stage() {
			return stage;
		}

		void widen() {
			if (stage >= CRACK_STAGES) return;
			stage++;
			lastWidened = level.getGameTime();
			set(target, Attributes.ARMOR, CRACK_ARMOUR_ID, "Armour-Splitting Finger", -0.2 * stage, AttributeModifier.Operation.MULTIPLY_TOTAL);
			set(target, Attributes.ARMOR_TOUGHNESS, CRACK_TOUGHNESS_ID, "Armour-Splitting Finger", -0.2 * stage, AttributeModifier.Operation.MULTIPLY_TOTAL);
			HeavenSpells.particles(level, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_CHESTPLATE)), HeavenSpells.middle(target), 4, 0.3, 0.1);
			HeavenSpells.particles(level, ParticleTypes.CRIT, HeavenSpells.middle(target), 4, 0.3, 0.1);
			HeavenSpells.sound(level, target.position(), stage == CRACK_STAGES ? SoundEvents.ANVIL_DESTROY : SoundEvents.ITEM_BREAK, 0.5f, 1.2f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			if (age % 10 == 0) {
				AABB box = target.getBoundingBox();
				java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
				for (int i = 0; i < stage; i++) {
					level.sendParticles(HeavenSpells.dust(GOLD, 0.6f), r.nextDouble(box.minX, box.maxX), r.nextDouble(box.minY, box.maxY), r.nextDouble(box.minZ, box.maxZ), 1, 0, 0, 0, 0);
				}
			}
			return true;
		}

		@Override
		public float onIncoming(LivingEntity struck, DamageSource source, float amount) {
			if (struck != target || amount <= 0) return amount;
			float more = amount * (1 + CRACK_HARM * stage);
			if (!striking && !HeavenSpells.inHurtTime(target) && level.getGameTime() - lastWidened >= CRACK_GROWS_EVERY) widen();
			return more;
		}

		@Override
		public void end() {
			unset(target, Attributes.ARMOR, CRACK_ARMOUR_ID);
			unset(target, Attributes.ARMOR_TOUGHNESS, CRACK_TOUGHNESS_ID);
		}
	}

	// --- Leaping Golden Lightning ---

	/** Leaping Golden Lightning: golden lightning from the hand to the first being looked at, leaping on from each to the next. */
	private static void lightning(ServerPlayer p) {
		LivingEntity first = HeavenSpells.aimed(p, LIGHTNING_RANGE);
		if (first == null) return;
		ServerLevel level = p.serverLevel();
		Chain chain = new Chain(p);
		chain.strike(HeavenSpells.fromHands(p), first);
		HeavenSpells.linger(chain);
		HeavenSpells.sound(level, p.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f, 1.9f);
	}

	/** The lightning's chain: whom it has struck, the last, and its leaps to come. */
	static final class Chain extends HeavenSpells.Timed {
		final Set<LivingEntity> struck = new LinkedHashSet<>();
		LivingEntity last;
		Vec3 lastAt;
		int strikes;

		Chain(ServerPlayer caster) {
			super(caster, LEAP_TICKS * LIGHTNING_HARM.length + 2);
		}

		void strike(Vec3 from, LivingEntity target) {
			bolt(level, from, HeavenSpells.middle(target));
			HeavenSpells.strikeAgain(caster, target, LIGHTNING_HARM[strikes], QiElement.METAL);
			Vec3 v = target.getDeltaMovement();
			HeavenSpells.move(target, new Vec3(0, v.y, 0));
			HeavenSpells.particles(level, ParticleTypes.ELECTRIC_SPARK, HeavenSpells.middle(target), 10, 0.3, 0.1);
			HeavenSpells.particles(level, ParticleTypes.FLASH, HeavenSpells.middle(target), 1, 0, 0);
			HeavenSpells.sound(level, target.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8f, 1.3f);
			struck.add(target);
			last = target;
			lastAt = HeavenSpells.middle(target);
			strikes++;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere() || strikes >= LIGHTNING_HARM.length) return false;
			if (age == 0 || age % LEAP_TICKS != 0) return true;
			Vec3 from = last != null && last.isAlive() ? HeavenSpells.middle(last) : lastAt;
			double reach = last != null && last.isInWater() ? LEAP_WATER : LEAP;
			LivingEntity next = null;
			double best = reach * reach;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, from, reach + 2, e -> !HeavenSpells.spared(caster, e) && !struck.contains(e) && !(e instanceof ArmorStand))) {
				double d = HeavenSpells.middle(e).distanceToSqr(from);
				if (d <= best && clear(level, from, HeavenSpells.middle(e), caster)) {
					best = d;
					next = e;
				}
			}
			if (next == null) return false;
			if (last != null && last.isInWater()) HeavenSpells.particles(level, ParticleTypes.BUBBLE_POP, from, 6, 0.3, 0.05);
			strike(from, next);
			return true;
		}
	}

	// --- Nine Circling Blades ---

	/** Nine Circling Blades: nine flying blades circling the caster, each darting at a foe that comes near. */
	private static void blades(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		List<Blades> circling = HeavenSpells.lingering(p.getUUID(), Blades.class);
		if (!circling.isEmpty()) {
			circling.get(0).left = BLADES;
			circling.get(0).end = level.getGameTime() + BLADES_TICKS;
		} else {
			HeavenSpells.linger(new Blades(p));
		}
		HeavenSpells.sound(level, p.position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 1.3f);
		HeavenSpells.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
	}

	/** The blades: how many are left, and those darting out (where, at whom, how long). */
	static final class Blades extends HeavenSpells.Timed {
		final List<Object[]> darting = new ArrayList<>();
		final Map<LivingEntity, Long> lastStruck = new HashMap<>();
		int left = BLADES;
		long lastLaunch = -10;

		Blades(ServerPlayer caster) {
			super(caster, BLADES_TICKS);
		}

		/** How many blades are left (tests). */
		int left() {
			return left;
		}

		private boolean seen(LivingEntity e) {
			return clear(level, caster.getEyePosition(), HeavenSpells.middle(e), caster);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere() || left <= 0) return false;
			// Darting blades: home on their quarry; strike it, or come back unspent.
			for (java.util.Iterator<Object[]> it = darting.iterator(); it.hasNext(); ) {
				Object[] dart = it.next();
				Vec3 at = (Vec3) dart[0];
				LivingEntity quarry = (LivingEntity) dart[1];
				int flown = (Integer) dart[2];
				if (!HeavenSpells.present(quarry, level) || quarry.distanceTo(caster) > BLADE_LEASH || !seen(quarry)) {
					it.remove();
					continue;
				}
				Vec3 heart = HeavenSpells.middle(quarry);
				Vec3 next = heart.subtract(at).length() <= BLADE_SPEED ? heart : at.add(heart.subtract(at).normalize().scale(BLADE_SPEED));
				HeavenSpells.line(level, at, next, HeavenSpells.dust(GOLD, 0.6f), 0.3);
				if (quarry.getBoundingBox().inflate(0.6).contains(next) || flown >= BLADE_FLIGHT) {
					it.remove();
					left--;
					lastStruck.put(quarry, now);
					HeavenSpells.strikeAgain(caster, quarry, BLADE_HARM, QiElement.METAL);
					HeavenSpells.push(caster.position(), quarry, 0.4);
					HeavenSpells.particles(level, ParticleTypes.CRIT, heart, 8, 0.3, 0.1);
					HeavenSpells.particles(level, ParticleTypes.ENCHANTED_HIT, heart, 4, 0.3, 0.1);
					HeavenSpells.sound(level, quarry.position(), SoundEvents.TRIDENT_HIT, 0.7f, 1.4f);
					if (left == 0) HeavenSpells.sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_BREAK, 0.6f, 1.4f);
					continue;
				}
				dart[0] = next;
				dart[2] = flown + 1;
			}
			int ringed = left - darting.size();
			// A foe near: the nearest blade darts out at it.
			if (ringed > 0 && now - lastLaunch >= 2) {
				LivingEntity nearest = null;
				double best = BLADE_REACH * BLADE_REACH;
				for (LivingEntity e : HeavenSpells.around(caster, BLADE_REACH, e -> HeavenSpells.enemy(caster, e) && !(e instanceof ArmorStand))) {
					Long struck = lastStruck.get(e);
					if (struck != null && now - struck < 10 || darting.stream().anyMatch(d -> d[1] == e)) continue;
					double d = e.distanceToSqr(caster);
					if (d < best && seen(e)) {
						best = d;
						nearest = e;
					}
				}
				if (nearest != null) {
					darting.add(new Object[]{bladeAt(age, 0, ringed), nearest, 0});
					lastLaunch = now;
					ringed--;
					HeavenSpells.sound(level, caster.position(), SoundEvents.TRIDENT_THROW, 0.5f, 1.9f);
				}
			}
			if (age % 2 == 0) {
				for (int i = 0; i < ringed; i++) {
					Vec3 at = bladeAt(age, i, ringed);
					double a = Math.PI * age / 20 + i * Math.PI * 2 / Math.max(1, ringed) + Math.PI / 2;
					Vec3 tip = at.add(Math.cos(a) * 0.25, 0, Math.sin(a) * 0.25);
					level.sendParticles(HeavenSpells.dust(WHITE, 0.6f), tip.x, tip.y, tip.z, 2, 0.02, 0.02, 0.02, 0);
					level.sendParticles(HeavenSpells.dust(GOLD, 0.8f), at.x - Math.cos(a) * 0.25, at.y, at.z - Math.sin(a) * 0.25, 2, 0.02, 0.02, 0.02, 0);
				}
			}
			if (age % 40 == 0) HeavenSpells.sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.2f, 1.8f);
			return true;
		}

		private Vec3 bladeAt(long age, int i, int of) {
			double a = Math.PI * age / 20 + i * Math.PI * 2 / Math.max(1, of);
			return caster.position().add(Math.cos(a) * BLADE_RING, 1.1, Math.sin(a) * BLADE_RING);
		}
	}

	// --- Thousand-Echo Toll ---

	/** Thousand-Echo Toll: the soul tolls like a bell, harming every enemy near and setting them resonating, sharing one another's wounds. */
	private static void toll(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Toll toll = new Toll(p);
		for (LivingEntity e : HeavenSpells.around(p, TOLL_RADIUS, e -> HeavenSpells.enemy(p, e) && !(e instanceof ArmorStand)
				&& !(e instanceof Player other && other.isCreative()))) {
			HeavenSpells.strike(p, e, TOLL_HARM, QiElement.METAL);
			if (!e.isAlive()) continue;
			for (Toll other : HeavenSpells.lingering(null, Toll.class)) other.health.remove(e); // a newer toll takes it
			toll.health.put(e, e.getHealth());
		}
		HeavenSpells.linger(toll);
		for (int i = 0; i < 10; i++) {
			double h = i * 5 / 9.0, r = 3 - 2 * i / 9.0;
			HeavenSpells.ring(level, p.position().add(0, h, 0), r, HeavenSpells.dust(GOLD, 1.6f), 20);
		}
		HeavenSpells.sound(level, p.position(), SoundEvents.BELL_BLOCK, 2.5f, 0.5f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BELL_RESONATE, 1.5f, 0.6f);
	}

	/** The resonating: each one's health as last seen; whatever it loses echoes on the others near. */
	static final class Toll extends HeavenSpells.Timed {
		final Map<LivingEntity, Float> health = new HashMap<>();
		final Vec3 centre;

		Toll(ServerPlayer caster) {
			super(caster, TOLL_TICKS);
			this.centre = caster.position();
		}

		/** Whether {@code e} resonates now (tests). */
		boolean resonates(LivingEntity e) {
			return health.containsKey(e);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age < 10) {
				HeavenSpells.ring(level, centre.add(0, 0.3, 0), 1 + 15 * (age + 1) / 10.0, HeavenSpells.dust(WHITE, 1.4f), 64);
				level.sendParticles(ParticleTypes.ENCHANT, centre.x, centre.y + 1, centre.z, 2, 1, 1, 1, 0.5);
			}
			health.keySet().removeIf(e -> !HeavenSpells.present(e, level));
			Map<LivingEntity, Float> echoes = new HashMap<>();
			for (Map.Entry<LivingEntity, Float> entry : health.entrySet()) {
				LivingEntity x = entry.getKey();
				float lost = entry.getValue() - x.getHealth();
				if (lost <= 0) continue;
				for (LivingEntity other : health.keySet()) {
					if (other == x || other.distanceToSqr(x) > ECHO_REACH * ECHO_REACH) continue;
					echoes.merge(other, lost * ECHO_SHARE, Float::sum);
					HeavenSpells.line(level, HeavenSpells.middle(x), HeavenSpells.middle(other), HeavenSpells.dust(WHITE, 0.5f), 0.6);
				}
			}
			for (Map.Entry<LivingEntity, Float> echo : echoes.entrySet()) {
				LivingEntity e = echo.getKey();
				HeavenSpells.strikeAgain(caster, e, echo.getValue(), QiElement.METAL);
				HeavenSpells.particles(level, ParticleTypes.ENCHANTED_HIT, HeavenSpells.middle(e), 5, 0.3, 0.1);
				HeavenSpells.sound(level, e.position(), SoundEvents.BELL_BLOCK, 0.35f, 1.9f);
			}
			// What the echoes took is no echo of its own: every health is seen afresh after them.
			for (LivingEntity e : health.keySet()) health.put(e, e.getHealth());
			if (age % 10 == 0) {
				for (LivingEntity e : health.keySet()) HeavenSpells.ring(level, e.position().add(0, e.getBbHeight() + 0.2, 0), 0.7, HeavenSpells.dust(GOLD, 0.6f), 10);
			}
			return health.size() > 0;
		}
	}

	// --- Fate-Severing Edge ---

	/** Fate-Severing Edge: an edge of sword light severs every blessing on the first being looked at, cuts it, and bars new blessings. */
	private static void edge(ServerPlayer p) {
		LivingEntity target = HeavenSpells.aimed(p, EDGE_RANGE);
		if (target == null) return;
		ServerLevel level = p.serverLevel();
		HeavenSpells.line(level, HeavenSpells.fromHands(p), HeavenSpells.middle(target), HeavenSpells.dust(WHITE, 0.4f), 0.25);
		boolean severed = target.getAbsorptionAmount() > 0;
		for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
			if (effect.getEffect().getCategory() != MobEffectCategory.BENEFICIAL || effect.isInfiniteDuration()) continue;
			target.removeEffect(effect.getEffect());
			severed = true;
		}
		target.setAbsorptionAmount(0);
		HeavenSpells.strike(p, target, EDGE_HARM, QiElement.METAL);
		Vec3 at = HeavenSpells.middle(target);
		Vec3 across = new Vec3(-p.getViewVector(1.0f).z, 0, p.getViewVector(1.0f).x).normalize();
		HeavenSpells.line(level, at.add(across.scale(1.2)).add(0, 0.8, 0), at.subtract(across.scale(1.2)).add(0, -0.8, 0), HeavenSpells.dust(WHITE, 0.8f), 0.15);
		HeavenSpells.line(level, at.add(across.scale(1.2)).add(0, 0.8, 0), at.subtract(across.scale(1.2)).add(0, -0.8, 0), HeavenSpells.dust(GOLD, 1.0f), 0.3);
		HeavenSpells.particles(level, ParticleTypes.SWEEP_ATTACK, at, 1, 0, 0);
		if (severed) {
			for (int i = 0; i < 8; i++) {
				double a = Math.PI * 2 * i / 8;
				HeavenSpells.line(level, at.add(Math.cos(a) * 0.4, 0, Math.sin(a) * 0.4), at.add(Math.cos(a) * 1.2, 0, Math.sin(a) * 1.2), HeavenSpells.dust(GOLD, 0.6f), 0.25);
			}
			HeavenSpells.particles(level, ParticleTypes.ENCHANTED_HIT, at, 8, 0.3, 0.1);
			HeavenSpells.sound(level, target.position(), SoundEvents.BEACON_DEACTIVATE, 0.6f, 1.5f);
		}
		HeavenSpells.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.7f);
		HeavenSpells.sound(level, target.position(), SoundEvents.SHEEP_SHEAR, 1.0f, 0.6f);
		HeavenSpells.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
		if (!target.isAlive()) return;
		for (Severed old : HeavenSpells.lingering(null, Severed.class)) {
			if (old.target == target) {
				old.end = level.getGameTime() + EDGE_TICKS;
				return;
			}
		}
		HeavenSpells.linger(new Severed(p, target));
	}

	/** A fate severed: no blessing takes hold of its bearer a while. */
	static final class Severed extends HeavenSpells.Timed implements HeavenSpells.EffectListener {
		final LivingEntity target;

		Severed(ServerPlayer caster, LivingEntity target) {
			super(caster, EDGE_TICKS);
			this.target = target;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			if (age % 10 == 0) HeavenSpells.particles(level, HeavenSpells.dust(SEVERED, 0.7f), HeavenSpells.middle(target), 3, 0.3, 0);
			return true;
		}

		@Override
		public boolean allowEffect(LivingEntity entity, MobEffectInstance effect) {
			return entity != target || effect.getEffect().getCategory() != MobEffectCategory.BENEFICIAL || level.getGameTime() > end;
		}
	}

	// --- Ten Thousand Swords Descend ---

	/** Ten Thousand Swords Descend: a sky of swords gathered over the ground looked at, raining down on all beneath it. */
	private static void swords(ServerPlayer p) {
		BlockHitResult hit = HeavenSpells.lookedBlock(p, SWORDS_RANGE);
		if (hit == null) return;
		ServerLevel level = p.serverLevel();
		HeavenSpells.linger(new Swords(p, hit.getLocation()));
		HeavenSpells.sound(level, hit.getLocation(), SoundEvents.BEACON_ACTIVATE, 1.2f, 1.5f);
		HeavenSpells.sound(level, hit.getLocation(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.7f);
	}

	/** The sky of swords: gathering, then wave on wave falling, each sword homing on whom it was loosed at, roofs sheltering. */
	static final class Swords extends HeavenSpells.Timed {
		final Vec3 centre;
		final List<Object[]> falling = new ArrayList<>();
		final Map<LivingEntity, Long> lastStruck = new HashMap<>();
		final List<Object[]> buried = new ArrayList<>();
		final Set<LivingEntity> aimedThisWave = new HashSet<>();

		Swords(ServerPlayer caster, Vec3 centre) {
			super(caster, SWORDS_GATHER + SWORD_WAVES * SWORD_WAVE_TICKS + 12);
			this.centre = centre;
		}

		private boolean inZone(LivingEntity e) {
			double dx = e.getX() - centre.x, dz = e.getZ() - centre.z;
			return dx * dx + dz * dz <= SWORDS_RADIUS * SWORDS_RADIUS && e.getY() >= centre.y - 4 && e.getY() <= centre.y + 12;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
			if (age % 5 == 0) HeavenSpells.ring(level, centre.add(0, 0.1, 0), SWORDS_RADIUS, HeavenSpells.dust(WHITE, 1.0f), 40);
			if (age < SWORDS_GATHER) {
				if (age % 2 == 0) {
					for (int i = 0; i < 80; i++) {
						double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * SWORDS_RADIUS;
						level.sendParticles(HeavenSpells.dust(GOLD, 1.5f), centre.x + Math.cos(a) * d, centre.y + SWORD_HEIGHT, centre.z + Math.sin(a) * d, 1, 0, 0, 0, 0);
					}
					level.sendParticles(ParticleTypes.END_ROD, centre.x, centre.y + SWORD_HEIGHT, centre.z, 6, SWORDS_RADIUS / 2, 0.3, SWORDS_RADIUS / 2, 0);
				}
				return true;
			}
			long rain = age - SWORDS_GATHER;
			int wave = (int) (rain / SWORD_WAVE_TICKS), inWave = (int) (rain % SWORD_WAVE_TICKS);
			if (wave < SWORD_WAVES) {
				if (inWave == 0) {
					aimedThisWave.clear();
					HeavenSpells.sound(level, centre, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
				}
				for (int launch : SWORD_LAUNCHES) if (launch == inWave) launch(now, r);
			}
			for (java.util.Iterator<Object[]> it = falling.iterator(); it.hasNext(); ) {
				Object[] sword = it.next();
				Vec3 at = (Vec3) sword[0];
				LivingEntity aim = (LivingEntity) sword[1];
				double tx = aim != null && aim.isAlive() ? aim.getX() : at.x, tz = aim != null && aim.isAlive() ? aim.getZ() : at.z;
				double sx = Math.max(-0.6, Math.min(0.6, tx - at.x)), sz = Math.max(-0.6, Math.min(0.6, tz - at.z));
				Vec3 next = at.add(sx, -SWORD_FALL, sz);
				BlockHitResult ground = level.clip(new ClipContext(at, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
				boolean intoAim = aim != null && aim.isAlive() && aim.getBoundingBox().clip(at, next).isPresent();
				if (intoAim || ground.getType() != HitResult.Type.MISS) {
					Vec3 impact = intoAim ? aim.getBoundingBox().clip(at, next).get() : ground.getLocation();
					impact(impact, now, !intoAim);
					it.remove();
					continue;
				}
				for (int k = 0; k < 6; k++) {
					Vec3 point = next.add(0, k * 0.25, 0);
					level.sendParticles(HeavenSpells.dust(k < 4 ? WHITE : GOLD, k < 4 ? 0.7f : 1.0f), point.x, point.y, point.z, 1, 0, 0, 0, 0);
				}
				level.sendParticles(ParticleTypes.END_ROD, next.x, next.y, next.z, 1, 0, 0, 0, 0);
				sword[0] = next;
				if (next.y < centre.y - 8) it.remove();
			}
			for (java.util.Iterator<Object[]> it = buried.iterator(); it.hasNext(); ) {
				Object[] b = it.next();
				Vec3 at = (Vec3) b[0];
				if (now > (Long) b[1]) {
					it.remove();
					continue;
				}
				if (now % 2 == 0) for (int k = 0; k < 3; k++) level.sendParticles(HeavenSpells.dust(k < 2 ? WHITE : GOLD, 0.7f), at.x, at.y + k * 0.25, at.z, 1, 0, 0, 0, 0);
			}
			return wave < SWORD_WAVES || !falling.isEmpty();
		}

		private void launch(long now, java.util.concurrent.ThreadLocalRandom r) {
			List<LivingEntity> beneath = HeavenSpells.aroundPoint(level, centre, SWORDS_RADIUS + 12, e -> !HeavenSpells.spared(caster, e) && inZone(e)
					&& !aimedThisWave.contains(e) && (lastStruck.get(e) == null || now - lastStruck.get(e) >= SWORD_GRACE));
			LivingEntity aim = beneath.isEmpty() ? null : beneath.get(r.nextInt(beneath.size()));
			Vec3 from;
			if (aim != null) {
				aimedThisWave.add(aim);
				from = new Vec3(aim.getX(), centre.y + SWORD_HEIGHT, aim.getZ());
			} else {
				double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * SWORDS_RADIUS;
				from = new Vec3(centre.x + Math.cos(a) * d, centre.y + SWORD_HEIGHT, centre.z + Math.sin(a) * d);
			}
			falling.add(new Object[]{from, aim});
		}

		private void impact(Vec3 at, long now, boolean ground) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, SWORD_CUT + 2, e -> !HeavenSpells.spared(caster, e)
					&& e.getBoundingBox().inflate(SWORD_CUT).contains(at) && (lastStruck.get(e) == null || now - lastStruck.get(e) >= SWORD_GRACE))) {
				lastStruck.put(e, now);
				HeavenSpells.strikeAgain(caster, e, SWORD_HARM, QiElement.METAL);
				HeavenSpells.particles(level, ParticleTypes.CRIT, HeavenSpells.middle(e), 8, 0.3, 0.1);
				HeavenSpells.particles(level, ParticleTypes.SWEEP_ATTACK, HeavenSpells.middle(e), 1, 0, 0);
				HeavenSpells.sound(level, e.position(), SoundEvents.TRIDENT_HIT, 0.6f, 1.3f);
			}
			if (ground) {
				BlockPos under = BlockPos.containing(at.x, at.y - 0.1, at.z);
				HeavenSpells.particles(level, HeavenSpells.block(level.getBlockState(under).getBlock()), at, 8, 0.2, 0.05);
				buried.add(new Object[]{at, now + 10});
				if (now % 2 == 0) HeavenSpells.sound(level, at, SoundEvents.TRIDENT_HIT_GROUND, 0.5f, 1.1f + java.util.concurrent.ThreadLocalRandom.current().nextFloat() * 0.8f);
			}
		}

		@Override
		public void end() {
			if (caster.isAlive()) HeavenSpells.sound(level, centre, SoundEvents.TRIDENT_RETURN, 1.0f, 0.7f);
		}
	}

	// --- Indestructible Golden Body ---

	/** Indestructible Golden Body: the caster's body gold that will not break: no blow takes more than a tenth of its health, nothing moves it. */
	private static void golden(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		HeavenSpells.endLingering(p.getUUID(), Golden.class);
		HeavenSpells.linger(new Golden(p));
		for (int i = 0; i < 24; i++) {
			double a = i * 0.7, h = i * 0.08;
			level.sendParticles(HeavenSpells.dust(GOLD, 1.2f), p.getX() + Math.cos(a) * 0.6, p.getY() + h, p.getZ() + Math.sin(a) * 0.6, 1, 0, 0, 0, 0);
		}
		HeavenSpells.particles(level, ParticleTypes.FLASH, HeavenSpells.middle(p), 1, 0, 0);
		HeavenSpells.sound(level, p.position(), SoundEvents.ARMOR_EQUIP_GOLD, 1.0f, 0.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.3f);
	}

	/** The golden body: each blow capped at a tenth of the caster's greatest health; no knockback. */
	static final class Golden extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		Golden(ServerPlayer caster) {
			super(caster, GOLDEN_TICKS);
			set(caster, Attributes.KNOCKBACK_RESISTANCE, GOLDEN_STAND_ID, "Indestructible Golden Body", 1.0, AttributeModifier.Operation.ADDITION);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age % 4 == 0) {
				AABB box = caster.getBoundingBox();
				java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
				for (int i = 0; i < 4; i++) {
					level.sendParticles(HeavenSpells.dust(i == 0 ? WHITE : GOLD, i == 0 ? 0.6f : 0.9f), r.nextDouble(box.minX, box.maxX), r.nextDouble(box.minY, box.maxY),
							r.nextDouble(box.minZ, box.maxZ), 1, 0, 0, 0, 0);
				}
			}
			return true;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			if (target != caster || HeavenPassives.untouchedByWards(source)) return amount;
			float cap = caster.getMaxHealth() * GOLDEN_CAP;
			if (amount <= cap) return amount;
			HeavenSpells.particles(level, ParticleTypes.ENCHANTED_HIT, HeavenSpells.middle(caster), 6, 0.3, 0.05);
			HeavenSpells.particles(level, ParticleTypes.WAX_ON, HeavenSpells.middle(caster), 4, 0.3, 0);
			HeavenSpells.sound(level, caster.position(), SoundEvents.ANVIL_LAND, 0.25f, 1.9f);
			return cap;
		}

		@Override
		public void end() {
			unset(caster, Attributes.KNOCKBACK_RESISTANCE, GOLDEN_STAND_ID);
			if (caster.isAlive()) HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_DEACTIVATE, 0.5f, 1.2f);
		}
	}

	// --- Four Gates Immortal-Slaying Array ---

	/** Four Gates Immortal-Slaying Array: four swords fall at the gates of a great square, sealing every enemy within and striking them. */
	private static void array(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		BlockHitResult hit = HeavenSpells.lookedBlock(p, ARRAY_RANGE);
		Vec3 centre = hit != null ? Vec3.atBottomCenterOf(hit.getBlockPos().above()) : p.position();
		HeavenSpells.endLingering(p.getUUID(), Gates.class);
		HeavenSpells.linger(new Gates(p, centre));
		HeavenSpells.sound(level, centre, SoundEvents.BEACON_ACTIVATE, 2.0f, 0.5f);
		HeavenSpells.sound(level, centre, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 0.6f);
	}

	/** The array: its four swords, the sealed (each with where it last stood inside, and its last cut), and the volleys. */
	static final class Gates extends HeavenSpells.Timed {
		final Vec3 centre;
		final Vec3[] gates;
		final Map<LivingEntity, Object[]> sealed = new HashMap<>();

		Gates(ServerPlayer caster, Vec3 centre) {
			super(caster, ARRAY_DESCENT + ARRAY_TICKS);
			this.centre = centre;
			this.gates = new Vec3[]{centre.add(0, 0, -ARRAY_HALF), centre.add(ARRAY_HALF, 0, 0), centre.add(0, 0, ARRAY_HALF), centre.add(-ARRAY_HALF, 0, 0)};
		}

		/** Whether {@code e} is sealed in now (tests). */
		boolean seals(LivingEntity e) {
			return sealed.containsKey(e);
		}

		private boolean inside(LivingEntity e) {
			return Math.abs(e.getX() - centre.x) <= ARRAY_HALF && Math.abs(e.getZ() - centre.z) <= ARRAY_HALF && e.getY() >= centre.y - 6 && e.getY() <= centre.y + 20;
		}

		private void drawSword(Vec3 base) {
			for (double h = 0; h < 12; h += 1) {
				for (int w = -1; w <= 1; w++) level.sendParticles(HeavenSpells.dust(WHITE, 1.5f), base.x + w * 0.25, base.y + h, base.z, 1, 0, 0, 0, 0);
			}
			for (int w = -2; w <= 1; w++) level.sendParticles(HeavenSpells.dust(GOLD, 1.8f), base.x + w * 0.4 + 0.2, base.y + 12, base.z, 1, 0, 0, 0, 0);
			for (int h = 0; h < 2; h++) level.sendParticles(HeavenSpells.dust(GOLD, 1.5f), base.x, base.y + 12.5 + h * 0.5, base.z, 1, 0, 0, 0, 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age < ARRAY_DESCENT) {
				double drop = 30 * (1 - (age + 1) / (double) ARRAY_DESCENT);
				for (Vec3 gate : gates) drawSword(gate.add(0, drop, 0));
				if (age == ARRAY_DESCENT - 1) {
					for (Vec3 gate : gates) {
						HeavenSpells.particles(level, ParticleTypes.EXPLOSION, gate, 1, 0, 0);
						HeavenSpells.particles(level, ParticleTypes.FLASH, gate.add(0, 1, 0), 1, 0, 0);
						HeavenSpells.sound(level, gate, SoundEvents.ANVIL_LAND, 1.5f, 0.5f);
					}
				}
				return true;
			}
			long standing = age - ARRAY_DESCENT;
			// Seal every enemy found inside: the gates admit, never release.
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(ARRAY_HALF, 21, ARRAY_HALF),
					e -> !sealed.containsKey(e) && inside(e) && HeavenSpells.enemy(caster, e) && !HeavenSpells.ally(caster, e) && !(e instanceof ArmorStand)
							&& !(e instanceof Player other && other.isCreative()))) {
				sealed.put(e, new Object[]{e.position(), -ARRAY_CUT_EVERY * 2L});
			}
			for (java.util.Iterator<Map.Entry<LivingEntity, Object[]>> it = sealed.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<LivingEntity, Object[]> entry = it.next();
				LivingEntity e = entry.getKey();
				Object[] seal = entry.getValue();
				if (!HeavenSpells.present(e, level)) {
					it.remove();
					continue;
				}
				if (inside(e)) {
					seal[0] = e.position();
				} else if (!(e instanceof EnderDragon) && !(e instanceof WitherBoss)) {
					Vec3 crossing = e.position();
					e.stopRiding();
					HeavenSpells.teleport(e, (Vec3) seal[0]);
					Vec3 in = centre.subtract(e.position()).multiply(1, 0, 1);
					HeavenSpells.move(e, (in.lengthSqr() < 1.0e-4 ? Vec3.ZERO : in.normalize().scale(0.8)).add(0, 0.3, 0));
					if (now - (Long) seal[1] >= ARRAY_CUT_EVERY) {
						seal[1] = now;
						HeavenSpells.strikeAgain(caster, e, ARRAY_CUT, QiElement.METAL);
						for (int k = 0; k < 10; k++) level.sendParticles(HeavenSpells.dust(WHITE, 1.2f), crossing.x, crossing.y + k * 0.25, crossing.z, 1, 0, 0, 0, 0);
						HeavenSpells.particles(level, ParticleTypes.CRIT, crossing.add(0, 1, 0), 12, 0.3, 0.1);
						HeavenSpells.sound(level, crossing, SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.7f);
						HeavenSpells.sound(level, crossing, SoundEvents.CHAIN_BREAK, 1.0f, 0.8f);
					}
				}
			}
			if (standing > 0 && standing % ARRAY_VOLLEY_EVERY == 0) {
				for (LivingEntity e : sealed.keySet()) {
					if (!inside(e)) continue;
					Vec3 gate = gates[0];
					for (Vec3 g : gates) if (g.distanceToSqr(e.position()) < gate.distanceToSqr(e.position())) gate = g;
					HeavenSpells.line(level, gate.add(0, 6, 0), HeavenSpells.middle(e), HeavenSpells.dust(WHITE, 1.0f), 0.5);
					HeavenSpells.strikeAgain(caster, e, ARRAY_VOLLEY, QiElement.METAL);
					HeavenSpells.particles(level, ParticleTypes.SWEEP_ATTACK, HeavenSpells.middle(e), 1, 0, 0);
					HeavenSpells.particles(level, ParticleTypes.CRIT, HeavenSpells.middle(e), 10, 0.3, 0.1);
				}
				HeavenSpells.sound(level, centre, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
				HeavenSpells.sound(level, centre, SoundEvents.TRIDENT_RIPTIDE_3, 0.8f, 1.5f);
			}
			if (standing % 5 == 0) for (Vec3 gate : gates) drawSword(gate);
			if (standing % 10 == 0) {
				for (int side = 0; side < 4; side++) {
					Vec3 a = gates[side].add(side % 2 == 0 ? -ARRAY_HALF : 0, 0, side % 2 == 0 ? 0 : -ARRAY_HALF);
					Vec3 along = side % 2 == 0 ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
					for (int k = 0; k <= ARRAY_HALF * 2; k++) {
						Vec3 point = a.add(along.scale(k));
						for (double h : new double[]{0.5, 2, 3.5}) level.sendParticles(HeavenSpells.dust(GOLD, 0.6f), point.x, point.y + h, point.z, 1, 0, 0, 0, 0);
						if (k % 4 == 0) level.sendParticles(ParticleTypes.END_ROD, point.x, point.y + 2, point.z, 1, 0, 0, 0, 0);
					}
				}
				for (LivingEntity e : sealed.keySet()) HeavenSpells.ring(level, e.position().add(0, 0.1, 0), 0.6, HeavenSpells.dust(GOLD, 0.6f), 8);
			}
			return true;
		}

		@Override
		public void end() {
			sealed.clear();
			HeavenSpells.sound(level, centre, SoundEvents.BEACON_DEACTIVATE, 1.0f, 0.6f);
			HeavenSpells.sound(level, centre, SoundEvents.TRIDENT_RETURN, 1.0f, 0.5f);
		}
	}

	private MetalSpells() {}
}
