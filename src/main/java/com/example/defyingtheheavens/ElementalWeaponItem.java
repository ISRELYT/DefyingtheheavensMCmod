package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A weapon made of its element (ArtifactKind#isElemental; phase 2 of the Artifact Forge, the user's ask of 2026-10-28: "some made of their
 * element"): forged from Spirit grade up with a demon core of its element (ArtifactRecipes), the element itself given a weapon's shape.
 * Each blow adds a share of the wielder's strike as its element's qi, which passes armour as the element spells' does (ModDamageTypes#qi),
 * and does what its element does, once a full swing at most:
 * <ul>
 *   <li>the Flame Blade (fire) sets what it strikes burning;</li>
 *   <li>the Tide Spear (water) slows it, and puts out its fire;</li>
 *   <li>the Vine Whip (wood) roots it where it stands (WoodSpells#bind), a moment, and never again until a while after ({@link #ROOT_RESPITE});</li>
 *   <li>the Mountain Hammer (earth), the heaviest and slowest, staggers it: thrown back and dazed;</li>
 *   <li>the Golden Light Sword (metal) cuts: its share of qi the greatest, all of it through armour.</li>
 * </ul>
 * Right-click in the main hand: its art, for {@link #ART_COST} of its wielder's qi and a while before the next (the item's cooldown):
 * Flame Arc, Surging Tide, Binding Lash, Mountain Quake, Golden Crescent ({@link #art}). While held, its element drifts off it.
 */
public class ElementalWeaponItem extends ArtifactWeaponItem {
	// Each table by kind (Flame Blade, Tide Spear, Vine Whip, Mountain Hammer, Golden Light Sword), then by grade (Spirit to Immortal).
	/** The blow each deals, as its tooltip shows it (the wielder's own 1 with it). */
	private static final int[][] BLOW = {{8, 9, 10, 12}, {7, 8, 9, 11}, {6, 7, 8, 10}, {10, 12, 14, 17}, {8, 9, 11, 13}};
	/** Swings a second. */
	private static final double[] SPEED = {1.6, 1.3, 1.8, 0.8, 1.5};
	/** What each adds to all its wielder's blows (a share of their whole attack), as a metal blade does. */
	private static final double[][] MIGHT = {{0.06, 0.12, 0.18, 0.28}, {0.06, 0.12, 0.18, 0.28}, {0.05, 0.10, 0.15, 0.24},
			{0.08, 0.16, 0.24, 0.36}, {0.06, 0.12, 0.18, 0.28}};
	/** The share of its wielder's strike each blow adds as its element's qi. */
	private static final double[][] SHARE = {{0.20, 0.25, 0.30, 0.40}, {0.15, 0.20, 0.25, 0.30}, {0.15, 0.20, 0.25, 0.30},
			{0.15, 0.20, 0.25, 0.30}, {0.35, 0.45, 0.55, 0.70}};
	/** The Flame Blade's burning, seconds; the Tide Spear's slowing, ticks, and how strong; the Vine Whip's rooting, ticks. */
	private static final int[] BURN_SECONDS = {4, 6, 8, 10}, SLOW_TICKS = {40, 60, 80, 100}, SLOW_AMP = {1, 1, 1, 2}, ROOT_TICKS = {15, 20, 25, 30};
	/** The Mountain Hammer's stagger: how hard it throws, and how long the daze. */
	private static final double[] STAGGER_PUSH = {0.8, 1.0, 1.2, 1.5};
	private static final int[] STAGGER_TICKS = {20, 30, 40, 50};
	/** Ticks after a rooting ends before the same one can be rooted again: never held for good. */
	public static final int ROOT_RESPITE = 40;

	/** An art's cost: this share of its wielder's whole pool of qi. */
	public static final double ART_COST = 0.08;
	/** Ticks before the art is ready again, by kind. */
	private static final int[] ART_COOLDOWN = {50, 60, 40, 80, 50};
	/** How far each art reaches (the flame's sweep, the wave's ride, the lash, the quake's round, the crescent's flight), by grade. */
	private static final double[][] ART_REACH = {{4, 5, 6, 8}, {5, 6, 8, 10}, {8, 10, 12, 14}, {3, 4, 5, 6}, {10, 13, 16, 20}};
	/** How hard each art strikes, a share of its wielder's strike, by grade. */
	private static final double[][] ART_POWER = {{0.8, 0.9, 1.0, 1.2}, {0.7, 0.8, 0.9, 1.0}, {0.9, 1.0, 1.1, 1.3}, {0.6, 0.7, 0.8, 1.0},
			{1.0, 1.15, 1.3, 1.6}};
	/** Half the Flame Arc's width, degrees either side of the look. */
	private static final double ARC_HALF = 55;

	/** Until when each one rooted by a Vine Whip is free of another rooting (game time). */
	private static final Map<LivingEntity, Long> ROOTED_UNTIL = new WeakHashMap<>();

	public ElementalWeaponItem(ArtifactKind kind, ArtifactGrade grade, Properties properties) {
		super(kind, grade, BLOW[index(kind)][grade.ordinal() - 1], SPEED[index(kind)], MIGHT[index(kind)][grade.ordinal() - 1], properties);
	}

	private static int index(ArtifactKind kind) {
		return kind.ordinal() - ArtifactKind.FLAME_BLADE.ordinal();
	}

	private int k() { return index(kind()); }
	private int g() { return grade().ordinal() - 1; }
	public QiElement element() { return kind().element(); }

	/** The share of its wielder's strike each blow of {@code stack} adds as its element's qi (its work's worth with it). */
	public double share(ItemStack stack) {
		return SHARE[k()][g()] * Artifacts.power(stack);
	}

	/** How far {@code stack}'s art reaches. */
	public double artReach() {
		return ART_REACH[k()][g()];
	}

	/** Ticks before the art is ready again. */
	public int artCooldown() {
		return ART_COOLDOWN[k()];
	}

	/** What {@code attacker} strikes with now: its attack, its main hand's weapon and its realm's strength in it. */
	static double attackOf(LivingEntity attacker) {
		return attacker.getAttribute(Attributes.ATTACK_DAMAGE) != null ? attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) : 1;
	}

	/**
	 * {@code amount} of {@code element}'s qi from {@code attacker} into {@code target}, passing armour; a second blow in the same moment as
	 * the first, so the first's moment of grace is set aside for it.
	 */
	static boolean qiHit(ServerLevel level, LivingEntity attacker, LivingEntity target, QiElement element, double amount) {
		if (amount <= 0 || !target.isAlive()) return false;
		target.invulnerableTime = 0;
		return target.hurt(ModDamageTypes.qi(level, element, attacker), (float) amount);
	}

	// --- Its blows ---

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker.level() instanceof ServerLevel level && mayReach(level, attacker)) strike(level, stack, target, attacker);
		return super.hurtEnemy(stack, target, attacker);
	}

	/** Its element on {@code target}, struck by {@code attacker}: its share of qi, then what its element does. */
	void strike(ServerLevel level, ItemStack stack, LivingEntity target, LivingEntity attacker) {
		int g = g();
		double power = Artifacts.power(stack);
		Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
		qiHit(level, attacker, target, element(), attackOf(attacker) * share(stack));
		switch (kind()) {
			case FLAME_BLADE -> {
				target.setSecondsOnFire((int) Math.round(BURN_SECONDS[g] * power));
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, at, Vec3.ZERO, 0.6f, 0);
				sound(level, at, SoundEvents.FIRECHARGE_USE, 0.6f, 1.2f);
			}
			case TIDE_SPEAR -> {
				slow(attacker, target, (int) Math.round(SLOW_TICKS[g] * power), SLOW_AMP[g]);
				target.clearFire();
				SpellFx.send(level, SpellFx.Kind.SPLASH, at, Vec3.ZERO, 0.6f, 0);
				sound(level, at, SoundEvents.PLAYER_SPLASH, 0.5f, 1.3f);
			}
			case VINE_WHIP -> {
				if (root(attacker, target, (int) Math.round(ROOT_TICKS[g] * power))) {
					SpellFx.send(level, SpellFx.Kind.LEAVES, target.position().add(0, 0.3, 0), Vec3.ZERO, 0.6f, 0);
					sound(level, at, SoundEvents.ROOTED_DIRT_PLACE, 0.9f, 0.8f);
				}
			}
			case MOUNTAIN_HAMMER -> {
				stagger(attacker, target, STAGGER_PUSH[g] * power, (int) Math.round(STAGGER_TICKS[g] * power));
				SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, at, Vec3.ZERO, 0.7f, 0);
				sound(level, at, SoundEvents.ANVIL_LAND, 0.35f, 0.6f);
			}
			case GOLDEN_SWORD -> {
				SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, at, attacker.getLookAngle(), 0.6f, 0);
				sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
			}
			default -> {}
		}
	}

	private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	static void slow(LivingEntity attacker, LivingEntity target, int ticks, int amplifier) {
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier), attacker);
	}

	/** Roots {@code target} where it stands for {@code ticks}, unless rooted too lately ({@link #ROOT_RESPITE}); whether it took. */
	static boolean root(LivingEntity attacker, LivingEntity target, int ticks) {
		long now = target.level().getGameTime();
		Long free = ROOTED_UNTIL.get(target);
		if (free != null && now < free) return false;
		ROOTED_UNTIL.put(target, now + ticks + ROOT_RESPITE);
		WoodSpells.bind(attacker, target, ticks);
		return true;
	}

	/** Throws {@code target} back from {@code attacker} ({@code push}) and dazes it for {@code ticks}: slowed hard, its blows weakened. */
	static void stagger(LivingEntity attacker, LivingEntity target, double push, int ticks) {
		if (target instanceof Mob mob) mob.getNavigation().stop();
		target.knockback(push, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
		target.hurtMarked = true;
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2), attacker);
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0), attacker);
	}

	// --- Its art ---

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.consume(stack);
		PlayerCultivation c = CultivationManager.forPlayer(sp);
		double cost = Math.max(1, c.maxQi() * ART_COST);
		if (c.isMortal() || c.getQi() < cost) {
			sp.displayClientMessage(Component.translatable(ModLang.ART_NO_QI, Math.round(cost)), true);
			return InteractionResultHolder.fail(stack);
		}
		c.consumeQi(cost);
		art(sp.serverLevel(), sp, stack);
		sp.getCooldowns().addCooldown(this, artCooldown());
		sp.swing(hand, true);
		CultivationManager.sync(sp);
		return InteractionResultHolder.consume(stack);
	}

	/** Its art, by {@code p}: each strikes for a share of their strike as its element's qi, its reach and might by its grade and work. */
	void art(ServerLevel level, ServerPlayer p, ItemStack stack) {
		int g = g();
		double power = Artifacts.power(stack), reach = artReach(), amount = attackOf(p) * ART_POWER[k()][g] * power;
		switch (kind()) {
			case FLAME_BLADE -> flameArc(level, p, reach, amount, (int) Math.round(BURN_SECONDS[g] * power));
			case TIDE_SPEAR -> surgingTide(level, p, reach, amount, (int) Math.round(SLOW_TICKS[g] * power), SLOW_AMP[g]);
			case VINE_WHIP -> bindingLash(level, p, reach, amount, (int) Math.round(ROOT_TICKS[g] * 2 * power));
			case MOUNTAIN_HAMMER -> mountainQuake(level, p, reach, amount, STAGGER_PUSH[g] * power, (int) Math.round(STAGGER_TICKS[g] * power));
			case GOLDEN_SWORD -> goldenCrescent(level, p, reach, amount);
			default -> {}
		}
	}

	/** The Flame Arc: a sweep of fire before {@code p}, everyone within its reach and its width ({@link #ARC_HALF}) burnt and set burning. */
	private static void flameArc(ServerLevel level, ServerPlayer p, double reach, double amount, int burn) {
		Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
		double cos = Math.cos(Math.toRadians(ARC_HALF));
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(reach + 1), e -> Artifacts.mayStrike(p, e))) {
			Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
			double d = to.length();
			if (d < 1.0e-3 || d > reach + e.getBbWidth() * 0.5 || to.scale(1 / d).dot(look) < cos) continue;
			if (qiHit(level, p, e, QiElement.FIRE, amount)) e.setSecondsOnFire(burn);
		}
		// Fire along the arc, near and far, sweeping from one side to the other; bursts at its edge.
		Vec3 flat = flat(look, p);
		for (double angle = -ARC_HALF; angle <= ARC_HALF + 1.0e-6; angle += 11) {
			Vec3 way = flat.yRot((float) Math.toRadians(angle));
			for (double r = 1.2; r <= reach; r += 0.9) {
				Vec3 at = eye.add(way.scale(r)).add(0, -0.55, 0);
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2, 0.12, 0.12, 0.12, 0.02);
			}
		}
		for (int i = -2; i <= 2; i++) {
			Vec3 way = flat.yRot((float) Math.toRadians(i * ARC_HALF / 2));
			SpellFx.send(level, SpellFx.Kind.FLAME_BURST, eye.add(way.scale(reach * 0.8)).add(0, -0.5, 0), way, 1.0f, 0);
		}
		sound(level, eye, SoundEvents.BLAZE_SHOOT, 1.0f, 0.8f);
	}

	/** Where a weapon in {@code p}'s right hand is as they see it: a little before them, to the right and below. */
	private static Vec3 hand(LivingEntity p) {
		Vec3 look = p.getLookAngle(), flat = flat(look, p);
		return p.getEyePosition().add(look.scale(0.6)).add(-flat.z * 0.45, -0.45, flat.x * 0.45);
	}

	/** {@code particle} strewn along the line from {@code a} to {@code b}, {@code each} every {@code step} blocks. */
	private static void line(ServerLevel level, ParticleOptions particle, Vec3 a, Vec3 b, double step, int each, double spread) {
		Vec3 ab = b.subtract(a);
		double length = ab.length();
		for (double d = 0; d <= length; d += step) {
			Vec3 at = a.add(ab.scale(d / Math.max(1.0e-6, length)));
			level.sendParticles(particle, at.x, at.y, at.z, each, spread, spread, spread, 0.0);
		}
	}

	/**
	 * The Surging Tide: {@code p} rides a wave up to {@code reach} forward along the ground (to the first wall), everyone in the way struck,
	 * slowed and put out; no fall harm after.
	 */
	private static void surgingTide(ServerLevel level, ServerPlayer p, double reach, double amount, int slowTicks, int slowAmp) {
		Vec3 way = flat(p.getLookAngle(), p), start = p.position(), end = start;
		for (double d = 0.5; d <= reach + 1.0e-6; d += 0.5) {
			Vec3 next = start.add(way.scale(d));
			if (!level.noCollision(p, p.getBoundingBox().move(next.subtract(start)))) break;
			end = next;
		}
		AABB path = p.getBoundingBox().expandTowards(end.subtract(start)).inflate(0.8);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, path, e -> Artifacts.mayStrike(p, e))) {
			if (qiHit(level, p, e, QiElement.WATER, amount)) {
				slow(p, e, slowTicks, slowAmp);
				e.clearFire();
			}
		}
		double length = start.distanceTo(end);
		for (double d = 0; d <= length; d += 1.5) SpellFx.send(level, SpellFx.Kind.SPLASH, start.add(way.scale(d)).add(0, 0.3, 0), way, 1.0f, 0);
		HeavenSpells.teleport(p, end);
		HeavenSpells.safeFall(p, 40);
		p.clearFire();
		// The wave breaking before its rider where it sets them down, its spray thrown up ahead of them.
		Vec3 crest = end.add(way.scale(2.0)).add(0, 0.9, 0);
		SpellFx.send(level, SpellFx.Kind.RIPPLE, end.add(0, 0.05, 0), Vec3.ZERO, 2.5f, 0);
		SpellFx.send(level, SpellFx.Kind.SPLASH, crest, way, 1.8f, 0);
		level.sendParticles(ParticleTypes.SPLASH, crest.x, crest.y, crest.z, 90, 1.2, 0.5, 1.2, 0.3);
		level.sendParticles(ParticleTypes.FALLING_WATER, crest.x, crest.y + 0.6, crest.z, 30, 1.0, 0.4, 1.0, 0.0);
		level.sendParticles(ParticleTypes.BUBBLE_POP, end.x, end.y + 0.8, end.z, 20, 0.8, 0.5, 0.8, 0.05);
		sound(level, end, SoundEvents.TRIDENT_RIPTIDE_1, 1.0f, 1.0f);
		sound(level, end, SoundEvents.GENERIC_SPLASH, 0.8f, 1.0f);
	}

	/** The Binding Lash: the vine lashes out up to {@code reach} (to the first wall) at the first foe in the way, and roots it there. */
	private static void bindingLash(ServerLevel level, ServerPlayer p, double reach, double amount, int ticks) {
		Vec3 eye = p.getEyePosition(), end = wallOr(level, p, eye, eye.add(p.getLookAngle().scale(reach)));
		LivingEntity target = firstOnLine(level, p, eye, end);
		Vec3 to = target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0) : end, from = hand(p);
		SpellFx.send(level, SpellFx.Kind.STREAK, from, to, 0, 0x5FCB5A);
		line(level, ParticleTypes.HAPPY_VILLAGER, from, to, 0.6, 1, 0.04);
		line(level, ParticleTypes.COMPOSTER, from, to, 0.4, 2, 0.08);
		sound(level, eye, SoundEvents.CAVE_VINES_PLACE, 1.0f, 0.7f);
		if (target == null) return;
		if (qiHit(level, p, target, QiElement.WOOD, amount)) {
			ROOTED_UNTIL.remove(target); // a lash binds even one just loosed
			root(p, target, ticks);
		}
		SpellFx.send(level, SpellFx.Kind.LEAVES, target.position().add(0, 0.3, 0), Vec3.ZERO, 1.2f, 0);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 0.3, target.getZ(), 12, 0.4, 0.2, 0.4, 0.0);
		sound(level, to, SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 0.8f);
	}

	/** The Mountain Quake: {@code p} brings the hammer down on the ground; everyone within {@code reach} struck and staggered. */
	private static void mountainQuake(ServerLevel level, ServerPlayer p, double reach, double amount, double push, int ticks) {
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(reach, 2, reach),
				e -> Artifacts.mayStrike(p, e) && e.distanceToSqr(p) <= reach * reach)) {
			if (qiHit(level, p, e, QiElement.EARTH, amount)) stagger(p, e, push, ticks);
		}
		BlockState under = level.getBlockState(p.blockPosition().below());
		if (under.isAir()) under = Blocks.STONE.defaultBlockState();
		Vec3 ground = p.position().add(0, 0.1, 0);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) reach, 0);
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground.add(flat(p.getLookAngle(), p).scale(1.2)), new Vec3(0, 1, 0), (float) (reach * 0.4),
				Block.getId(under));
		// The ground's own earth and stone flung up in rings round the blow, and its dust rising.
		BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, under);
		for (double r = 1.2; r <= reach; r += Math.max(1.2, reach / 3)) {
			for (int i = 0; i < 16; i++) {
				double angle = 2 * Math.PI * i / 16;
				level.sendParticles(debris, ground.x + Math.cos(angle) * r, ground.y + 0.3, ground.z + Math.sin(angle) * r, 10, 0.25, 0.3, 0.25, 0.35);
			}
		}
		SpellFx.send(level, SpellFx.Kind.DUST, ground.add(flat(p.getLookAngle(), p).scale(2.0)), Vec3.ZERO, (float) (reach * 0.6), 0);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, ground.x, ground.y + 0.2, ground.z, 12, reach * 0.4, 0.1, reach * 0.4, 0.01);
		sound(level, ground, SoundEvents.GENERIC_EXPLODE, 0.6f, 0.6f);
		sound(level, ground, SoundEvents.ROOTED_DIRT_BREAK, 1.2f, 0.5f);
	}

	/** The Golden Crescent: a crescent of metal qi flies {@code reach} from {@code p}'s eye (to the first wall), cutting through all in its way. */
	private static void goldenCrescent(ServerLevel level, ServerPlayer p, double reach, double amount) {
		Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), end = wallOr(level, p, eye, eye.add(look.scale(reach)));
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.5), e -> Artifacts.mayStrike(p, e))) {
			Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
			if (fromSegment(c, eye, end) > 1.0 + e.getBbWidth() * 0.5) continue;
			if (qiHit(level, p, e, QiElement.METAL, amount)) SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, c, look, 0.6f, 0);
		}
		SpellFx.send(level, SpellFx.Kind.STREAK, hand(p), end, 0, 0xFFD866);
		SpellFx.send(level, SpellFx.Kind.FLASH, hand(p).add(look.scale(0.6)), Vec3.ZERO, 0.6f, 0xFFE08A);
		// The crescent as it flew: a curve of golden light across the line every few blocks, bowed forward.
		Vec3 flat = flat(look, p), right = new Vec3(-flat.z, 0, flat.x);
		DustParticleOptions gold = new DustParticleOptions(new Vector3f(1.0f, 0.86f, 0.4f), 1.6f);
		double length = eye.distanceTo(end);
		for (double d = 2; d <= length; d += 2.5) {
			Vec3 centre = eye.add(look.scale(d)).add(0, -0.3, 0);
			for (double s = -1; s <= 1.0e-6 + 1; s += 0.25) {
				Vec3 at = centre.add(right.scale(s * 1.3)).add(look.scale(0.5 * (1 - s * s)));
				level.sendParticles(gold, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
			}
		}
		sound(level, eye, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);
		sound(level, eye, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
	}

	/** The way {@code look} goes along the ground (looking straight up or down: the way {@code e} faces). */
	private static Vec3 flat(Vec3 look, Entity e) {
		Vec3 flat = new Vec3(look.x, 0, look.z);
		if (flat.lengthSqr() < 1.0e-4) flat = Vec3.directionFromRotation(0, e.getYRot());
		return flat.normalize();
	}

	/** {@code end}, or where the line from {@code from} first meets a wall. */
	private static Vec3 wallOr(ServerLevel level, Entity e, Vec3 from, Vec3 end) {
		BlockHitResult wall = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
		return wall.getType() == HitResult.Type.MISS ? end : wall.getLocation();
	}

	/** The first living thing {@code p} may strike on the line from {@code from} to {@code to}, or null. */
	private static @Nullable LivingEntity firstOnLine(ServerLevel level, ServerPlayer p, Vec3 from, Vec3 to) {
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, p, from, to, new AABB(from, to).inflate(1.0),
				e -> e instanceof LivingEntity living && Artifacts.mayStrike(p, living));
		return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
	}

	/** How far {@code point} lies from the segment {@code a} to {@code b}. */
	private static double fromSegment(Vec3 point, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double t = ab.lengthSqr() < 1.0e-9 ? 0 : Math.max(0, Math.min(1, point.subtract(a).dot(ab) / ab.lengthSqr()));
		return point.distanceTo(a.add(ab.scale(t)));
	}

	// --- Held ---

	/** While held in the main hand, its element drifts off it now and then, for all to see. */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, level, entity, slot, selected);
		// The main hand by the stack itself (vanilla ticks the offhand as "selected" too while the hotbar's first slot is chosen).
		if (!(level instanceof ServerLevel server) || !(entity instanceof LivingEntity holder) || holder.getMainHandItem() != stack || server.getGameTime() % 6 != 0) return;
		Vec3 look = flat(holder.getLookAngle(), holder);
		Vec3 right = new Vec3(-look.z, 0, look.x);
		Vec3 at = holder.position().add(0, holder.getBbHeight() * 0.5, 0).add(look.scale(0.45)).add(right.scale(0.4));
		server.sendParticles(drift(), at.x, at.y, at.z, 1, 0.08, 0.12, 0.08, 0.005);
	}

	/** What drifts off it: flames, drops, spores, dust, golden motes. */
	private ParticleOptions drift() {
		return switch (kind()) {
			case FLAME_BLADE -> ParticleTypes.SMALL_FLAME;
			case TIDE_SPEAR -> ParticleTypes.DRIPPING_WATER;
			case VINE_WHIP -> ParticleTypes.COMPOSTER;
			case MOUNTAIN_HAMMER -> new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.COARSE_DIRT.defaultBlockState());
			default -> new DustParticleOptions(new Vector3f(1.0f, 0.84f, 0.35f), 0.7f);
		};
	}

	/** Mended with its own element (a blaze rod, a prismarine crystal, a vine, obsidian, a gold ingot), not its grade's metal. */
	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
		return repair.is(switch (kind()) {
			case FLAME_BLADE -> Items.BLAZE_ROD;
			case TIDE_SPEAR -> Items.PRISMARINE_CRYSTALS;
			case VINE_WHIP -> Items.VINE;
			case MOUNTAIN_HAMMER -> Items.OBSIDIAN;
			default -> Items.GOLD_INGOT;
		});
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		Artifacts.describe(stack, grade(), lines);
		double power = Artifacts.power(stack);
		int g = g();
		Tooltips.add(lines, Component.translatable(ModLang.ELEMENTAL_SHARE, Artifacts.percent(share(stack)), element().getDisplayName())
				.withStyle(ChatFormatting.BLUE));
		Component perk = switch (kind()) {
			case FLAME_BLADE -> Component.translatable(ModLang.FLAME_BLADE_PERK, Math.round(BURN_SECONDS[g] * power));
			case TIDE_SPEAR -> Component.translatable(ModLang.TIDE_SPEAR_PERK, seconds(SLOW_TICKS[g] * power));
			case VINE_WHIP -> Component.translatable(ModLang.VINE_WHIP_PERK, seconds(ROOT_TICKS[g] * power));
			case MOUNTAIN_HAMMER -> Component.translatable(ModLang.MOUNTAIN_HAMMER_PERK, seconds(STAGGER_TICKS[g] * power));
			default -> Component.translatable(ModLang.GOLDEN_SWORD_PERK);
		};
		Tooltips.add(lines, perk.copy().withStyle(ChatFormatting.BLUE));
		String art = switch (kind()) {
			case FLAME_BLADE -> ModLang.ART_FLAME_ARC;
			case TIDE_SPEAR -> ModLang.ART_SURGING_TIDE;
			case VINE_WHIP -> ModLang.ART_BINDING_LASH;
			case MOUNTAIN_HAMMER -> ModLang.ART_MOUNTAIN_QUAKE;
			default -> ModLang.ART_GOLDEN_CRESCENT;
		};
		Tooltips.add(lines, Component.translatable(art, Math.round(artReach())).withStyle(ChatFormatting.GOLD));
		Tooltips.add(lines, Component.translatable(ModLang.ART_COST, Artifacts.percent(ART_COST), seconds(artCooldown())).withStyle(ChatFormatting.GRAY));
	}

	/** "1.5" seconds of {@code ticks}. */
	private static String seconds(double ticks) {
		double s = ticks / 20.0;
		return s == Math.rint(s) ? String.valueOf((long) s) : String.format(java.util.Locale.ROOT, "%.1f", s);
	}
}
