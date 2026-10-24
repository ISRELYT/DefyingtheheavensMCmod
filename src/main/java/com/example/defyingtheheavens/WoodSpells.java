package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The wood element's spells (Spell, HeavenSpells): wood is weak to metal and strong against earth, and a wooden missile burns up
 * against a ward of fire (SpellAuras). Every number is the design's; every look its own (the client's spell renderers).
 * <ul>
 *   <li>Qi Refining: Wood Bullet (a stake grown in the air and shot), Emerald Healing Art (healing whatever the hunger, a green
 *       glow), Verdant Growth (a tree grows under your feet and lifts you high).</li>
 *   <li>Foundation Building: Life Force Drain and Life Force Gain (years of life drawn out of a being or a herb into you, or poured
 *       from you into it), Mysterious Bristle Prison (thorns rise and hold a being; it can strike its way out, at a price).</li>
 *   <li>Core Formation: Thousand Leaf Storm, Ancient Bark Skin.</li>
 *   <li>Nascent Soul: Thorn Spear Forest, Lotus of Rebirth.</li>
 *   <li>Heavenly Being: Heaven-Piercing Bamboo, Spring of Myriad Life.</li>
 *   <li>Four Axis: Myriad Vine Binding, World Tree (its vines seize all but the caster and drink their life into them).</li>
 *   <li>Dao Integration: Azure Dragon (the dragon of the east bursts from the land), Myriad Woods Return to the Dao (a spirit tree).</li>
 *   <li>Void Shattering: Spring Thunder of the Azure Emperor (a jade storm cloud's thunder: in the old lore thunder is wood's, the east's
 *       and spring's), Void Withering (the void-rooted tree's roots hang down out of a rift and drink the land's life).</li>
 * </ul>
 */
public final class WoodSpells {
	public static final double BULLET_SPEED = 2.0, BULLET_RANGE = 40;
	public static final float BULLET_DAMAGE = 5;
	public static final int EMERALD_TICKS = 10 * 20;
	public static final int GROWTH_HEIGHT = 14, GROWTH_TICKS = 40, GROWTH_LASTS = 60 * 20;
	public static final double LIFE_RANGE = 12, LIFE_LEASH = 16;
	public static final int LIFE_TICKS = 6 * 20;
	public static final float DRAIN_PER_SECOND = 1.5f, PLANT_DRAIN_HEAL = 1.0f, GAIN_COST = 1.0f, GAIN_HEAL = 2.0f, GAIN_FLOOR = 4.0f;
	public static final int DRAIN_YEARS = 60, GAIN_YEARS = 120;
	/**
	 * Life Force's lifespan (the user's ask, 2026-10-21): Gain gives a herb or a fruit the caster's own years, this many a second (each
	 * second's {@link #GAIN_YEARS} of growth), never the last Lifespan#WARN_YEARS of them; Drain draws this many a second out of another
	 * player's years while it lands on them, the drawer keeping Lifespan#DRAWN_KEPT of them (Lifespan#draw).
	 */
	public static final double GAIN_LIFE_YEARS = 10, DRAIN_LIFE_YEARS = 10;
	public static final double PRISON_RANGE = 20;
	public static final int PRISON_TICKS = 4 * 20, PRISON_STRIKES = 3;
	public static final float PRISON_DAMAGE = 1.5f, PRISON_THORNS = 2;
	public static final int LEAVES = 16;
	public static final double LEAF_SPEED = 1.6, LEAF_RANGE = 36;
	public static final float LEAF_DAMAGE = 2.5f;
	public static final int BARK_TICKS = 20 * 20;
	public static final double SPEARS_RANGE = 32, SPEARS_RADIUS = 6;
	public static final float SPEARS_DAMAGE = 35;
	public static final int SPEARS_ROOT = 2 * 20;
	public static final double LOTUS_RADIUS = 10;
	public static final int LOTUS_TICKS = 10 * 20;
	public static final float LOTUS_HEAL = 3;
	/** The ground each bamboo bursts out of, broken {@link #BAMBOO_BREAK} round (4, by its realm: Devastation#sized). */
	public static final double BAMBOO_RANGE = 48, BAMBOO_RADIUS = 24, BAMBOO_BREAK = Devastation.sized(Realm.HEAVENLY_BEING, 5);
	public static final int BAMBOO_TARGETS = 6;
	public static final float BAMBOO_DAMAGE = 70;
	public static final double SPRING_RADIUS = 32;
	public static final float SPRING_BURST = 40, SPRING_HEAL = 4;
	public static final int SPRING_TICKS = 10 * 20;
	public static final double VINES_RANGE = 64, VINES_RADIUS = 40;
	public static final int VINES_TARGETS = 12, VINES_TICKS = 5 * 20;
	public static final float VINES_DAMAGE = 22;
	/** The tree's vines reach {@link #TREE_REACH}; the land splits round its roots out to {@link #TREE_FISSURES} (40, by its realm). */
	public static final double TREE_RANGE = 24, TREE_REACH = 32, TREE_FISSURES = Devastation.sized(Realm.FOUR_AXIS, 40);
	public static final int TREE_TICKS = 12 * 20;
	public static final float TREE_DRAIN = 10;
	/**
	 * Azure Dragon: how far off it rises (82), whom it catches (within {@link #DRAGON_RADIUS}, 18) and how hard, its climb (round a coil
	 * {@link #DRAGON_COIL} across, {@link #DRAGON_TURNS} times, {@link #DRAGON_HEIGHT} up, 56, over {@link #DRAGON_RISE} ticks), its roar (its
	 * model's roar takes 32 ticks, then it fades), how long it holds what it caught, and the fissures it tears (out to 51 and 8 deep). Its
	 * reach and size by its realm (HeavenSpells#span), its wounds too (Devastation#sized).
	 */
	public static final double DRAGON_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 64), DRAGON_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 14),
			DRAGON_COIL = HeavenSpells.span(Realm.DAO_INTEGRATION, 14), DRAGON_TURNS = 1, DRAGON_HEIGHT = HeavenSpells.span(Realm.DAO_INTEGRATION, 44);
	public static final float DRAGON_DAMAGE = 480;
	public static final int DRAGON_RISE = 50, DRAGON_ROAR = 40, DRAGON_HOLD = 3 * 20, DRAGON_FISSURES = 14;
	public static final double DRAGON_REACH = Devastation.sized(Realm.DAO_INTEGRATION, 40), DRAGON_FISSURE_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 6);
	/** Myriad Woods Return to the Dao: the woods' reach round the spirit tree (41, by its realm), how long they hold, what each foe is drained a second. */
	public static final double WOODS_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 32);
	public static final int WOODS_TICKS = 15 * 20;
	public static final float WOODS_DRAIN = 40;
	/**
	 * Spring Thunder of the Azure Emperor: where its cloud gathers (up to {@link #THUNDER_RANGE}, 118 by its realm), how high over the land
	 * (40), how far round it reaches ({@link #THUNDER_RADIUS}, 26); each bolt's harm and how long it roots; how long the cloud takes to
	 * gather, its volleys and how far apart, the bolts loosed at the land besides in each, and the little craters those leave (by its realm).
	 */
	public static final double THUNDER_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), THUNDER_HIGH = HeavenSpells.span(Realm.VOID_SHATTERING, 22),
			THUNDER_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 14), THUNDER_CRATER = Devastation.sized(Realm.VOID_SHATTERING, 1.5),
			THUNDER_CRATER_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 1);
	public static final float THUNDER_DAMAGE = 220;
	public static final int THUNDER_GATHER = 30, THUNDER_VOLLEYS = 6, THUNDER_EVERY = 20, THUNDER_ROOT = 20, THUNDER_STRAYS = 2;
	/** Spring thunder's jade (its bolts: TribulationLightning#tint). */
	public static final int THUNDER_JADE = 0x5CF0A0;
	/**
	 * Void Withering: where (up to {@link #WITHER_RANGE}, 118), how high over the land its rift opens (33) and how long it is, how far round
	 * the void tree's roots reach and drink (37), how high they haul what they seize (9, by its realm) and how many they hold at once; what each
	 * foe is drained a second; when the roots come out of the rift (it open), when they begin to seize and drink, how long they drink, and how
	 * long a seizing root takes to lash out to its catch (TendrilRenderer#LASH: then it hauls).
	 */
	public static final double WITHER_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), WITHER_HIGH = HeavenSpells.span(Realm.VOID_SHATTERING, 18),
			WITHER_RIFT = HeavenSpells.span(Realm.VOID_SHATTERING, 13), WITHER_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 20),
			WITHER_LIFT = HeavenSpells.span(Realm.VOID_SHATTERING, 5);
	public static final float WITHER_DRAIN = 200;
	public static final int WITHER_GRASPS = 8, WITHER_ROOTS = 10, WITHER_OPENS = 20, WITHER_TICKS = 8 * 20, WITHER_LASH = 7;

	private static final UUID BOUND_ID = UUID.fromString("2b8f4e17-91c3-4d6a-b5e0-7a3c1f9d2e64");

	static boolean canCast(LivingEntity p, Spell spell) {
		return switch (spell) {
			case LIFE_FORCE_DRAIN, LIFE_FORCE_GAIN -> {
				LifeTarget target = lifeTarget(p, spell == Spell.LIFE_FORCE_DRAIN, true);
				yield target != LifeTarget.TOLD && (target != null || HeavenSpells.refuse(p));
			}
			case MYSTERIOUS_BRISTLE_PRISON -> HeavenSpells.aimed(p, PRISON_RANGE) != null || HeavenSpells.refuse(p);
			case VERDANT_GROWTH -> p.onGround() || HeavenSpells.refuse(p);
			// Nothing to seize where you look: refused, and nothing paid (it was 500 and 1200 qi spent on nothing).
			case HEAVEN_PIERCING_BAMBOO -> !marksNear(p, BAMBOO_RANGE, BAMBOO_RADIUS, 1).isEmpty() || HeavenSpells.refuse(p);
			case MYRIAD_VINE_BINDING -> !marksNear(p, VINES_RANGE, VINES_RADIUS, 1).isEmpty() || HeavenSpells.refuse(p);
			default -> true;
		};
	}

	static void cast(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		switch (spell) {
			case WOOD_BULLET -> bullet(p, spell);
			case EMERALD_HEALING_ART -> {
				SpellAuras.put(p, SpellAura.EMERALD_HEALING, EMERALD_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
				HeavenSpells.sound(level, p.position(), SoundEvents.AZALEA_LEAVES_PLACE, 1.0f, 1.0f);
			}
			case VERDANT_GROWTH -> HeavenSpells.linger(new Growth(p));
			case LIFE_FORCE_DRAIN -> life(p, spell, true);
			case LIFE_FORCE_GAIN -> life(p, spell, false);
			case MYSTERIOUS_BRISTLE_PRISON -> prison(p, spell);
			case THOUSAND_LEAF_STORM -> HeavenSpells.linger(new LeafStorm(p, spell));
			case ANCIENT_BARK_SKIN -> {
				SpellAuras.put(p, SpellAura.ANCIENT_BARK_SKIN, BARK_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.WOOD_PLACE, 1.5f, 0.5f);
				HeavenSpells.sound(level, p.position(), SoundEvents.ROOTED_DIRT_PLACE, 1.0f, 0.6f);
				SpellFx.send(level, SpellFx.Kind.SPLINTERS, HeavenSpells.middle(p), Vec3.ZERO, 1.2f, 0);
			}
			case THORN_SPEAR_FOREST -> spears(p, spell);
			case LOTUS_OF_REBIRTH -> HeavenSpells.linger(new Lotus(p));
			case HEAVEN_PIERCING_BAMBOO -> bamboo(p, spell);
			case SPRING_OF_MYRIAD_LIFE -> HeavenSpells.linger(new Spring(p));
			case MYRIAD_VINE_BINDING -> vines(p, spell);
			case WORLD_TREE -> {
				HeavenSpells.endLingering(p.getUUID(), WorldTree.class);
				HeavenSpells.linger(new WorldTree(p, spell));
			}
			case AZURE_DRAGON -> {
				LivingEntity aimed = HeavenSpells.aimed(p, DRAGON_RANGE);
				HeavenSpells.linger(new Dragon(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, DRAGON_RANGE)));
			}
			case MYRIAD_WOODS_RETURN -> {
				HeavenSpells.endLingering(p.getUUID(), Woods.class);
				HeavenSpells.linger(new Woods(p, spell));
			}
			case SPRING_THUNDER_OF_THE_AZURE_EMPEROR -> {
				LivingEntity aimed = HeavenSpells.aimed(p, THUNDER_RANGE);
				HeavenSpells.linger(new Thunder(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, THUNDER_RANGE)));
			}
			case VOID_WITHERING -> {
				HeavenSpells.endLingering(p.getUUID(), Withering.class);
				LivingEntity aimed = HeavenSpells.aimed(p, WITHER_RANGE);
				HeavenSpells.linger(new Withering(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, WITHER_RANGE)));
			}
			default -> {}
		}
	}

	// --- Binding a being where it stands ---

	/**
	 * Holds {@code e} at {@code at} for {@code ticks}: it can't walk away (pulled back if it strays), its walking speed gone. A great
	 * beast (HeavenSpells#greatBeast) is only slowed.
	 */
	public static final class Bound extends HeavenSpells.Timed {
		public final LivingEntity target;
		private final Vec3 at;

		Bound(LivingEntity caster, LivingEntity target, Vec3 at, int ticks) {
			super(caster, ticks);
			this.target = target;
			this.at = at;
			AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.removeModifier(BOUND_ID);
				speed.addTransientModifier(new AttributeModifier(BOUND_ID, "Bound by wood", HeavenSpells.greatBeast(target) ? -0.6 : -1.0,
						AttributeModifier.Operation.MULTIPLY_TOTAL));
			}
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			if (!HeavenSpells.greatBeast(target) && target.position().distanceToSqr(at) > 0.09) {
				HeavenSpells.teleport(target, new Vec3(at.x, Math.min(target.getY(), at.y + 0.5), at.z));
			}
			HeavenSpells.still(target);
			return true;
		}

		@Override
		public void end() {
			AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) speed.removeModifier(BOUND_ID);
		}
	}

	/** Binds {@code target} where it stands for {@code ticks} (an earlier binding of this caster's on it ends first). */
	static Bound bind(LivingEntity p, LivingEntity target, int ticks) {
		for (Bound bound : HeavenSpells.lingering(p.getUUID(), Bound.class)) {
			if (bound.target == target) HeavenSpells.stop(bound);
		}
		Bound bound = new Bound(p, target, target.position(), ticks);
		HeavenSpells.linger(bound);
		return bound;
	}

	// --- Qi Refining ---

	private static void bullet(LivingEntity p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.WOOD_STAKE, 1.0f, 6, new Vec3(0.35, -0.25, 1.1), BULLET_SPEED, BULLET_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						HeavenSpells.strike(p, target, BULLET_DAMAGE, spell, missile);
						HeavenSpells.push(missile.position().subtract(missile.getDeltaMovement()), target, 0.4);
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.SPLINTERS, missile.position(), missile.getDeltaMovement(), 0.6f, 0);
						HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.WOOD_HIT, 1.0f, 1.2f);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.SPLINTERS, hit.getLocation(), missile.getDeltaMovement(), 0.6f, 0);
						HeavenSpells.sound(HeavenSpells.level(p), hit.getLocation(), SoundEvents.WOOD_BREAK, 0.8f, 1.2f);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.ARROW_SHOOT, 0.8f, 0.6f);
					}
				});
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.AZALEA_PLACE, 1.0f, 1.3f);
	}

	/**
	 * Verdant Growth: a tree pushes up out of the ground under its caster, lifting them {@link #GROWTH_HEIGHT} blocks over
	 * {@link #GROWTH_TICKS}, and spreads a crown round them at the top. It stands {@link #GROWTH_LASTS}, then withers away (its
	 * blocks are the spell's: they drop nothing).
	 */
	public static final class Growth extends HeavenSpells.Timed {
		private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
		private static final BlockState LEAVES = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		private final BlockPos base;
		private int placed;
		private boolean crowned;

		Growth(LivingEntity caster) {
			super(caster, GROWTH_TICKS + 20);
			this.base = caster.blockPosition();
			HeavenSpells.sound(level, caster.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.2f, 0.7f);
			SpellFx.send(level, SpellFx.Kind.LEAVES, caster.position(), Vec3.ZERO, 1.0f, 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age > GROWTH_TICKS) {
				if (!crowned) crown();
				return age < GROWTH_TICKS + 20;
			}
			// The height the trunk has reached by now, eased out at the top.
			double t = age / (double) GROWTH_TICKS;
			double height = GROWTH_HEIGHT * (1 - Math.pow(1 - t, 2));
			int want = (int) Math.floor(height);
			while (placed < want) {
				BlockPos pos = base.above(placed);
				if (!level.getBlockState(pos).canBeReplaced() && !level.getBlockState(pos).is(LOG.getBlock())) break;
				if (!HeavenSpells.placeTemporary(level, pos, LOG, GROWTH_LASTS) && !level.getBlockState(pos).is(LOG.getBlock())) break;
				SpellBlock.show(level, LOG, Vec3.atBottomCenterOf(pos).subtract(0, 1, 0), new Vec3(0, 1, 0), 0, 3, SpellBlock.Motion.SLIDE, 0);
				placed++;
				if (placed % 3 == 0) HeavenSpells.sound(level, Vec3.atCenterOf(pos), SoundEvents.WOOD_PLACE, 0.8f, 0.6f + placed * 0.04f);
			}
			// The caster rides the top of the trunk: lifted to just over it.
			double top = base.getY() + placed;
			if (caster.getY() < top + 0.05) {
				HeavenSpells.move(caster, new Vec3(caster.getDeltaMovement().x * 0.3, Math.min(0.6, top + 0.1 - caster.getY() + 0.08), caster.getDeltaMovement().z * 0.3));
				caster.resetFallDistance();
			}
			if (placed < want) return age < GROWTH_TICKS; // blocked overhead: it stops growing there
			return true;
		}

		/** The crown: a round head of blossoming leaves about the trunk's top, the caster standing on it. */
		private void crown() {
			crowned = true;
			BlockPos top = base.above(Math.max(1, placed));
			int r = 3;
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = -2; dy <= 1; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						double d = dx * dx + dz * dz + (dy + 0.5) * (dy + 0.5) * 2.2;
						if (d > r * r + 0.5 || level.random.nextInt(10) == 0 && d > 4) continue;
						BlockPos pos = top.offset(dx, dy, dz);
						if (dx == 0 && dz == 0 && dy < 0) continue; // the trunk
						if (HeavenSpells.placeTemporary(level, pos, LEAVES, GROWTH_LASTS)) {
							SpellBlock.show(level, LEAVES, Vec3.atBottomCenterOf(pos), Vec3.ZERO, level.random.nextInt(6), 6, SpellBlock.Motion.GROW, 0);
						}
					}
				}
			}
			if (HeavenSpells.placeTemporary(level, top.below(), LOG, GROWTH_LASTS)) placed++;
			HeavenSpells.sound(level, Vec3.atCenterOf(top), SoundEvents.AZALEA_LEAVES_PLACE, 1.5f, 0.8f);
			SpellFx.send(level, SpellFx.Kind.LEAVES, Vec3.atCenterOf(top), Vec3.ZERO, 2.0f, 0);
			Spells.safeFall(caster, 60);
		}
	}

	// --- Foundation Building ---

	/**
	 * The aging herb or fruit {@code p} looks at within reach (a Cultivation Fruit, a ginseng), or null. Herbs and fruit can be
	 * walked through (no collision), so the look is traced against the blocks' outlines, as the crosshair picks them.
	 */
	static BlockPos plantLookedAt(LivingEntity p) {
		Vec3 from = p.getEyePosition();
		Vec3 to = from.add(p.getViewVector(1.0f).scale(LIFE_RANGE));
		BlockHitResult hit = p.level().clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.OUTLINE,
				net.minecraft.world.level.ClipContext.Fluid.NONE, p));
		if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return null;
		BlockEntity be = p.level().getBlockEntity(hit.getBlockPos());
		return be instanceof CultivationFruitBlockEntity || be instanceof GinsengBlockEntity ? hit.getBlockPos() : null;
	}

	private static int plantAge(ServerLevel level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof CultivationFruitBlockEntity fruit) return fruit.age();
		if (be instanceof GinsengBlockEntity ginseng) return ginseng.age();
		return -1;
	}

	private static void setPlantAge(ServerLevel level, BlockPos pos, int years) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof CultivationFruitBlockEntity fruit) fruit.setAge(years);
		else if (be instanceof GinsengBlockEntity ginseng) ginseng.setAge(years);
	}

	/** The age of an aging fruit or herb held ({@code stack}: a Cultivation Fruit, a ginseng and its kind), or -1 for anything else. */
	static int heldAge(net.minecraft.world.item.ItemStack stack) {
		if (stack.getItem() instanceof CultivationFruitItem) return CultivationFruitItem.age(stack);
		if (stack.getItem() instanceof GinsengItem) return GinsengItem.age(stack);
		return -1;
	}

	private static void setHeldAge(net.minecraft.world.item.ItemStack stack, int years) {
		if (stack.getItem() instanceof CultivationFruitItem) stack.getOrCreateTag().putInt("FruitAge", FruitAge.clamp(years));
		else if (stack.getItem() instanceof GinsengItem) stack.getOrCreateTag().putInt("HerbAge", FruitAge.clamp(years));
	}

	/** Whether {@code pos} lies in a treasury whose seal holds (a sect's, or a Ghost King's palace's): nothing in it is to be touched. */
	private static boolean sealedTreasury(ServerLevel level, BlockPos pos) {
		return SectManager.sealedTreasuryAt(level, pos) != null || PalaceTreasury.isTreasury(level, pos) && PalaceTreasury.sealed(level, pos);
	}

	/**
	 * What Life Force Drain or Gain takes hold of: a being looked at, an aging herb growing there (or, giving, a fruit on its tree),
	 * or else a single aging fruit or herb held. Drawing life is harm (a player only where one may harm another); giving reaches
	 * anyone. A fruit is drawn on only once picked and held, never on its tree; nothing in a sealed treasury is touched until its seal
	 * is broken. {@link #TOLD}: refused, and the caster told why ({@code tell}); null: nothing there.
	 */
	record LifeTarget(LivingEntity being, BlockPos plant, net.minecraft.world.InteractionHand hand) {
		static final LifeTarget TOLD = new LifeTarget(null, null, null);
	}

	static LifeTarget lifeTarget(LivingEntity p, boolean drain, boolean tell) {
		LivingEntity being = drain ? HeavenSpells.aimed(p, LIFE_RANGE) : HeavenSpells.aimedAny(p, LIFE_RANGE);
		if (being != null) return new LifeTarget(being, null, null);
		BlockPos plant = plantLookedAt(p);
		if (plant != null && drain && p.level().getBlockEntity(plant) instanceof CultivationFruitBlockEntity) plant = null;
		if (plant != null && sealedTreasury(HeavenSpells.level(p), plant)) {
			if (tell) HeavenSpells.tell(p, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_SEALED));
			return LifeTarget.TOLD;
		}
		if (plant != null) return new LifeTarget(null, plant, null);
		boolean many = false;
		for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
			net.minecraft.world.item.ItemStack held = p.getItemInHand(hand);
			if (heldAge(held) < 0) continue;
			if (held.getCount() == 1) return new LifeTarget(null, null, hand);
			many = true; // a stack would be drawn on (or aged) all at once: one at a time
		}
		if (many) {
			if (tell) HeavenSpells.tell(p, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_HOLD_ONE));
			return LifeTarget.TOLD;
		}
		return null;
	}

	/** Where a held fruit or herb is: in front of the caster's chest, the stream running from it. */
	private static Vec3 heldPoint(LivingEntity p) {
		return p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.45, 0);
	}

	private static void life(LivingEntity p, Spell spell, boolean drain) {
		HeavenSpells.endLingering(p.getUUID(), Life.class);
		LifeTarget target = lifeTarget(p, drain, false);
		if (target == null || target == LifeTarget.TOLD) return;
		HeavenSpells.linger(new Life(p, spell, drain, target.being(), target.plant(), target.hand()));
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), drain ? SoundEvents.SCULK_CATALYST_BLOOM : SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);
	}

	/**
	 * Life Force Drain or Gain on one being, one herb growing, or one fruit or herb held: for {@link #LIFE_TICKS}, a stream of
	 * emerald motes runs between it and its caster (SpellField LIFE_STREAM). Drawing: the being's life ({@link #DRAIN_PER_SECOND} of
	 * wood a second) mends the caster, and another player's years go with it ({@link #DRAIN_LIFE_YEARS} a second, half of them the
	 * caster's: Lifespan#draw); a herb's or fruit's years ({@link #DRAIN_YEARS} a second) are lost and mend the caster a little.
	 * Giving: the caster's own life runs out ({@link #GAIN_COST} a second, never below {@link #GAIN_FLOOR}) to heal a being; a herb or
	 * fruit is given the caster's own years ({@link #GAIN_LIFE_YEARS} a second for {@link #GAIN_YEARS} of growth, never the last ten
	 * of them). It breaks if they stray apart, or the one held leaves the hand; the years it moved are told as it ends.
	 */
	public static final class Life extends HeavenSpells.Timed {
		private final Spell spell;
		private final boolean drain;
		public final LivingEntity being;
		public final BlockPos plant;
		/** The hand holding the fruit or herb drawn on (or given to), or null. */
		public final net.minecraft.world.InteractionHand hand;
		private final SpellField field;
		/** The years of a lifespan it has moved (drawn out of another, or given a herb), and the growth it gave, for the word as it ends. */
		private double yearsMoved;
		private int growthGiven;

		Life(LivingEntity caster, Spell spell, boolean drain, LivingEntity being, BlockPos plant, net.minecraft.world.InteractionHand hand) {
			super(caster, LIFE_TICKS);
			this.spell = spell;
			this.drain = drain;
			this.being = being;
			this.plant = plant;
			this.hand = hand;
			Vec3 at = being != null ? HeavenSpells.middle(being) : plant != null ? Vec3.atCenterOf(plant) : heldPoint(caster);
			this.field = SpellField.create(level, SpellField.Kind.LIFE_STREAM, at, 0.5f, 1, LIFE_TICKS, caster).target(being).data(drain ? 1 : 0).spawn();
			if (being != null) SpellAuras.put(being, SpellAura.LIFE_FLOW, LIFE_TICKS);
		}

		/** The fruit or herb still held, alone, in its hand; or null. */
		private net.minecraft.world.item.ItemStack held() {
			net.minecraft.world.item.ItemStack stack = caster.getItemInHand(hand);
			return heldAge(stack) >= 0 && stack.getCount() == 1 ? stack : null;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			Vec3 at;
			if (being != null) {
				if (!HeavenSpells.present(being, level)) return false;
				at = HeavenSpells.middle(being);
			} else if (plant != null) {
				if (plantAge(level, plant) < 0) return false;
				at = Vec3.atCenterOf(plant);
			} else {
				if (held() == null) return false;
				at = heldPoint(caster);
			}
			if (at.distanceToSqr(caster.position()) > LIFE_LEASH * LIFE_LEASH) return false;
			field.setPos(at.x, at.y, at.z);
			if (age % 10 != 9) return true;
			if (drain) {
				if (being != null) {
					float taken = HeavenSpells.strikeAgain(caster, being, DRAIN_PER_SECOND / 2, spell);
					HeavenSpells.mend(caster, taken);
					// Another player's years go with the life it draws (the user's ask, 2026-10-21); a beast's or an NPC's have no lifespan.
					if (taken > 0 && being instanceof ServerPlayer victim && caster instanceof ServerPlayer drawer) {
						yearsMoved += Lifespan.draw(drawer, victim, DRAIN_LIFE_YEARS / 2);
					}
				} else {
					int years = plant != null ? plantAge(level, plant) : heldAge(held());
					if (years <= 1) return false;
					if (plant != null) setPlantAge(level, plant, Math.max(1, years - DRAIN_YEARS / 2));
					else setHeldAge(held(), Math.max(1, years - DRAIN_YEARS / 2));
					HeavenSpells.mend(caster, PLANT_DRAIN_HEAL / 2);
				}
			} else if (being != null) {
				if (caster.getHealth() <= GAIN_FLOOR) return false;
				caster.setHealth(Math.max(GAIN_FLOOR, caster.getHealth() - GAIN_COST / 2));
				HeavenSpells.mend(being, GAIN_HEAL / 2);
			} else {
				// A herb or a fruit grows on the caster's own years (the user's ask, 2026-10-21), never the last of them.
				int years = plant != null ? plantAge(level, plant) : heldAge(held());
				if (years >= FruitAge.MAX_YEARS) return false;
				if (!(caster instanceof ServerPlayer giver)) return false; // a herb grown on a player's own years
				if (!giver.isCreative()) {
					if (Lifespan.left(CultivationManager.own(giver)) - GAIN_LIFE_YEARS / 2 < Lifespan.WARN_YEARS) {
						HeavenSpells.tell(giver, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_TOO_FEW));
						return false;
					}
					Lifespan.age(giver, GAIN_LIFE_YEARS / 2);
					yearsMoved += GAIN_LIFE_YEARS / 2;
				}
				int grown = Math.min(FruitAge.MAX_YEARS, years + GAIN_YEARS / 2);
				growthGiven += grown - years;
				if (plant != null) setPlantAge(level, plant, grown);
				else setHeldAge(held(), grown);
			}
			if (age % 20 == 19) HeavenSpells.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, drain ? 0.7f : 1.5f);
			return true;
		}

		@Override
		public void end() {
			field.discard();
			if (being != null) SpellAuras.remove(being, SpellAura.LIFE_FLOW);
			// The years it moved, told: drawn out of another (to them both), or given a herb or fruit.
			if (yearsMoved <= 0) return;
			long years = Math.round(yearsMoved);
			if (drain && being instanceof ServerPlayer victim) {
				caster.sendSystemMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_DRAWN, years, victim.getDisplayName(),
						Math.round(yearsMoved * Lifespan.DRAWN_KEPT)).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
				victim.sendSystemMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_LOST, caster.getDisplayName(), years)
						.withStyle(net.minecraft.ChatFormatting.RED));
			} else if (!drain) {
				HeavenSpells.tell(caster, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_GIVEN, years, growthGiven));
			}
		}
	}

	private static void prison(LivingEntity p, Spell spell) {
		LivingEntity target = HeavenSpells.aimed(p, PRISON_RANGE);
		if (target == null) return;
		HeavenSpells.linger(new Prison(p, spell, target));
		HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.6f);
		HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.5f, 0.7f);
	}

	/**
	 * Mysterious Bristle Prison: thorny bristles burst up from the ground and close round a being, holding it where it stands for
	 * {@link #PRISON_TICKS} and tearing it ({@link #PRISON_DAMAGE} of wood a second). The cage can be struck (SpellField#strikeable):
	 * {@link #PRISON_STRIKES} blows from anyone break it, but each the caged one strikes from inside tears it for
	 * {@link #PRISON_THORNS} more.
	 */
	public static final class Prison extends HeavenSpells.Timed {
		private final Spell spell;
		public final LivingEntity target;
		private final SpellField field;
		private final Bound bound;
		private int strikes;

		Prison(LivingEntity caster, Spell spell, LivingEntity target) {
			super(caster, PRISON_TICKS);
			this.spell = spell;
			this.target = target;
			float radius = target.getBbWidth() / 2 + 0.65f;
			this.field = SpellField.create(level, SpellField.Kind.BRISTLE_PRISON, target.position(), radius, target.getBbHeight() + 0.5f, PRISON_TICKS,
					caster).target(target).data(caster.getRandom().nextInt()).strikeable(this::struck).spawn();
			this.bound = bind(caster, target, PRISON_TICKS);
		}

		private void struck(SpellField cage, DamageSource source, float amount) {
			if (cage.isRemoved()) return;
			strikes++;
			cage.value(1 - strikes / (float) PRISON_STRIKES);
			HeavenSpells.sound(level, cage.position(), SoundEvents.SWEET_BERRY_BUSH_BREAK, 1.2f, 0.8f);
			if (source.getEntity() == target) HeavenSpells.strikeAgain(caster, target, PRISON_THORNS, spell);
			if (strikes >= PRISON_STRIKES) end = level.getGameTime(); // broken: it ends this tick
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level) || field.isRemoved()) return false;
			if (age % 10 == 9) HeavenSpells.strikeAgain(caster, target, PRISON_DAMAGE / 2, spell);
			return true;
		}

		@Override
		public void end() {
			if (strikes >= PRISON_STRIKES) {
				SpellFx.send(level, SpellFx.Kind.THORNS_BREAK, field.position().add(0, field.height() / 2, 0), Vec3.ZERO, field.radius(), 0);
				HeavenSpells.sound(level, field.position(), SoundEvents.WOOD_BREAK, 1.5f, 0.6f);
			} else {
				SpellFx.send(level, SpellFx.Kind.SPLINTERS, field.position().add(0, field.height() / 2, 0), Vec3.ZERO, 1.0f, 0);
			}
			field.discard();
			HeavenSpells.stop(bound);
		}
	}

	// --- Core Formation ---

	/** Thousand Leaf Storm: {@link #LEAVES} razor leaves stream from the palm, one a tick, each bending a little toward its mark. */
	public static final class LeafStorm extends HeavenSpells.Timed {
		private final Spell spell;
		private int thrown;

		LeafStorm(LivingEntity caster, Spell spell) {
			super(caster, LEAVES + 2);
			this.spell = spell;
			HeavenSpells.sound(level, caster.position(), SoundEvents.AZALEA_LEAVES_BREAK, 1.5f, 1.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere() || thrown >= LEAVES) return thrown < LEAVES;
			double spread = 0.5;
			Vec3 offset = new Vec3(0.3 + (level.random.nextDouble() - 0.5) * spread, -0.2 + (level.random.nextDouble() - 0.5) * spread, 1.0);
			SpellProjectile leaf = SpellProjectile.cast(caster, spell, SpellProjectile.Shape.RAZOR_LEAF, 0.8f + level.random.nextFloat() * 0.4f, 0,
					offset, LEAF_SPEED, LEAF_RANGE, new SpellProjectile.Behaviour() {
						@Override
						public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
							HeavenSpells.strike(caster, target, LEAF_DAMAGE, spell, missile);
							target.invulnerableTime = 0; // a storm of leaves, each its own cut
							SpellFx.send(level, SpellFx.Kind.LEAVES, missile.position(), Vec3.ZERO, 0.3f, 0);
							return false;
						}

						@Override
						public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
							SpellFx.send(level, SpellFx.Kind.LEAVES, hit.getLocation(), Vec3.ZERO, 0.3f, 0);
						}
					}).homing(null, 2.5).data(level.random.nextInt());
			// Each its own way a little, fanning out of the palm.
			leaf.setDeltaMovement(leaf.getDeltaMovement().add((level.random.nextDouble() - 0.5) * 0.25, (level.random.nextDouble() - 0.5) * 0.15,
					(level.random.nextDouble() - 0.5) * 0.25));
			thrown++;
			if (thrown % 4 == 0) HeavenSpells.sound(level, caster.position(), SoundEvents.AZALEA_LEAVES_STEP, 1.0f, 1.6f);
			return true;
		}
	}

	// --- Nascent Soul ---

	private static void spears(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		LivingEntity aimed = HeavenSpells.aimed(p, SPEARS_RANGE);
		Vec3 point = aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SPEARS_RANGE);
		Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 1, 0), 3, 16);
		Vec3 at = ground != null ? ground : point;
		SpellField.create(level, SpellField.Kind.THORN_SPEARS, at, (float) SPEARS_RADIUS, 4, 50, p).data(level.random.nextInt()).spawn();
		HeavenSpells.sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, 2.0f, 0.5f);
		HeavenSpells.linger(new FireFromBelow(p, 6, () -> {
			// Whoever stands in it that the spells don't spare: foes, and people (players, cultivators), the one aimed at too.
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, SPEARS_RADIUS + 1, e -> e == aimed || HeavenSpells.mark(p, e))) {
				if (Math.abs(e.getY() - at.y) > 3) continue;
				HeavenSpells.strike(p, e, SPEARS_DAMAGE, spell);
				bind(p, e, SPEARS_ROOT);
			}
			HeavenSpells.sound(level, at, SoundEvents.SWEET_BERRY_BUSH_BREAK, 2.0f, 0.5f);
			HeavenSpells.sound(level, at, SoundEvents.TRIDENT_HIT, 2.0f, 0.6f);
		}));
	}

	/** A spell's blow a moment after its sign (spears breaking the surface where the ground first stirred). */
	static final class FireFromBelow extends HeavenSpells.Timed {
		private final Runnable then;

		FireFromBelow(LivingEntity caster, int ticks, Runnable then) {
			super(caster, ticks);
			this.then = then;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age >= end - start) {
				then.run();
				return false;
			}
			return true;
		}
	}

	/** Lotus of Rebirth: a great lotus opens at its caster's feet; all friendly within {@link #LOTUS_RADIUS} heal and are cleansed. */
	public static final class Lotus extends HeavenSpells.Timed {
		private final Vec3 at;
		private final SpellField field;

		Lotus(LivingEntity caster) {
			super(caster, LOTUS_TICKS);
			this.at = caster.position();
			this.field = SpellField.create(level, SpellField.Kind.LOTUS, at, (float) LOTUS_RADIUS, 2, LOTUS_TICKS, caster).spawn();
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, LOTUS_RADIUS, e -> HeavenSpells.friendly(caster, e))) {
				for (MobEffectInstance effect : List.copyOf(e.getActiveEffects())) {
					if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) e.removeEffect(effect.getEffect());
				}
				e.clearFire();
			}
			HeavenSpells.sound(level, at, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.4f);
			HeavenSpells.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0f, 0.8f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 20 == 10) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, at, LOTUS_RADIUS, e -> HeavenSpells.friendly(caster, e))) HeavenSpells.mend(e, LOTUS_HEAL);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Heavenly Being ---

	/** Where a spell that seeks beings out lands: the being aimed at (within {@code range}), or else the point looked at. */
	static Vec3 seekCentre(LivingEntity p, LivingEntity aimed, double range) {
		return aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, range);
	}

	/**
	 * Up to {@code count} beings for a spell that seeks them out where its caster looks (Heaven-Piercing Bamboo, Myriad Vine
	 * Binding): the one aimed at first, then the marks ({@link HeavenSpells#mark}: foes, and players and cultivators the caster's
	 * spells don't spare) within {@code radius} of it, nearest first. With a being aimed at, the search is round that being, so
	 * one standing against the open sky (the look running on past it to the end of the range) is still found.
	 */
	static List<LivingEntity> marksNear(LivingEntity p, double range, double radius, int count) {
		LivingEntity aimed = HeavenSpells.aimed(p, range);
		Vec3 centre = seekCentre(p, aimed, range);
		List<LivingEntity> found = new ArrayList<>(HeavenSpells.aroundPoint(HeavenSpells.level(p), centre, radius, e -> e != aimed && HeavenSpells.mark(p, e)));
		found.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(centre)));
		if (aimed != null) found.add(0, aimed);
		return found.size() > count ? new ArrayList<>(found.subList(0, count)) : found;
	}

	private static void bamboo(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 centre = seekCentre(p, HeavenSpells.aimed(p, BAMBOO_RANGE), BAMBOO_RANGE);
		List<LivingEntity> foes = marksNear(p, BAMBOO_RANGE, BAMBOO_RADIUS, BAMBOO_TARGETS);
		for (LivingEntity foe : foes) {
			Vec3 ground = HeavenSpells.groundBelow(level, foe.position().add(0, 0.5, 0), 2, 24);
			Vec3 at = ground != null ? ground : foe.position();
			SpellField.create(level, SpellField.Kind.BAMBOO, at, 0.9f, 18, 50, p).target(foe).data(level.random.nextInt()).spawn();
			Devastation.crater(p, level, at, BAMBOO_BREAK, BAMBOO_BREAK, Devastation.Floor.RUBBLE); // the ground it burst out of, broken
			HeavenSpells.strike(p, foe, BAMBOO_DAMAGE, spell);
			HeavenSpells.move(foe, new Vec3(foe.getDeltaMovement().x, 1.6, foe.getDeltaMovement().z));
		}
		HeavenSpells.sound(level, centre, SoundEvents.BAMBOO_PLACE, 3.0f, 0.5f);
		HeavenSpells.sound(level, centre, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.4f);
	}

	/** Spring of Myriad Life: a green rain of life over all friendly within {@link #SPRING_RADIUS}: a great mending, then more each second. */
	public static final class Spring extends HeavenSpells.Timed {
		private final SpellField field;

		Spring(LivingEntity caster) {
			super(caster, SPRING_TICKS);
			this.field = SpellField.create(level, SpellField.Kind.SPRING_RAIN, caster.position(), (float) SPRING_RADIUS, 18, SPRING_TICKS, caster).spawn();
			for (LivingEntity e : HeavenSpells.around(caster, SPRING_RADIUS, e -> HeavenSpells.friendly(caster, e))) {
				HeavenSpells.mend(e, SPRING_BURST);
				SpellFx.send(level, SpellFx.Kind.LEAVES, HeavenSpells.middle(e), Vec3.ZERO, 0.6f, 0);
			}
			HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_ACTIVATE, 2.0f, 1.6f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.WEATHER_RAIN_ABOVE, 2.0f, 1.2f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			field.setPos(caster.getX(), caster.getY(), caster.getZ());
			if (age % 20 == 10) {
				for (LivingEntity e : HeavenSpells.around(caster, SPRING_RADIUS, e -> HeavenSpells.friendly(caster, e))) HeavenSpells.mend(e, SPRING_HEAL);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Four Axis ---

	private static void vines(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 centre = seekCentre(p, HeavenSpells.aimed(p, VINES_RANGE), VINES_RANGE);
		List<LivingEntity> foes = marksNear(p, VINES_RANGE, VINES_RADIUS, VINES_TARGETS);
		for (LivingEntity foe : foes) HeavenSpells.linger(new Vines(p, spell, foe));
		HeavenSpells.sound(level, centre, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.4f);
		HeavenSpells.sound(level, p.position(), SoundEvents.EVOKER_CAST_SPELL, 2.0f, 0.5f);
		if (foes.isEmpty()) SpellFx.send(level, SpellFx.Kind.LEAVES, centre, Vec3.ZERO, 2.0f, 0);
	}

	/** One being seized by Myriad Vine Binding: held fast {@link #VINES_TICKS}, {@link #VINES_DAMAGE} of wood a second. */
	public static final class Vines extends HeavenSpells.Timed {
		private final Spell spell;
		public final LivingEntity target;
		private final SpellField field;
		private final Bound bound;

		Vines(LivingEntity caster, Spell spell, LivingEntity target) {
			super(caster, VINES_TICKS);
			this.spell = spell;
			this.target = target;
			this.field = SpellField.create(level, SpellField.Kind.VINE_BINDING, target.position(), target.getBbWidth() / 2 + 0.5f, target.getBbHeight() + 0.3f,
					VINES_TICKS, caster).target(target).data(caster.getRandom().nextInt()).spawn();
			this.bound = new Bound(caster, target, target.position(), VINES_TICKS);
			HeavenSpells.linger(bound);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			field.setPos(target.getX(), target.getY(), target.getZ());
			if (age % 10 == 9) HeavenSpells.strikeAgain(caster, target, VINES_DAMAGE / 2, spell);
			return true;
		}

		@Override
		public void end() {
			field.discard();
			HeavenSpells.stop(bound);
		}
	}

	/**
	 * The World Tree: it grows out of the ground where its caster looks (or before them), towering over all; for {@link #TREE_TICKS}
	 * its vines reach from its boughs for every being but the caster within {@link #TREE_REACH} of its trunk (their pets spared),
	 * lift them into its branches and hold them there, drinking {@link #TREE_DRAIN} of their life a second into its caster.
	 */
	public static final class WorldTree extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 trunk;
		private final SpellField field;
		/** Who hangs in its branches, and where each is held. */
		private final java.util.Map<LivingEntity, Vec3> seized = new java.util.HashMap<>();
		/** The vine that holds each (SpellField TREE_VINE: from a bough to them). */
		private final java.util.Map<LivingEntity, SpellField> vines = new java.util.HashMap<>();

		WorldTree(LivingEntity caster, Spell spell) {
			super(caster, TREE_TICKS);
			this.spell = spell;
			Vec3 point = HeavenSpells.lookPoint(caster, TREE_RANGE);
			if (point.distanceToSqr(caster.position()) < 36) point = caster.position().add(Vec3.directionFromRotation(0, caster.getYRot()).scale(8));
			Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.trunk = ground != null ? ground : point;
			this.field = SpellField.create(level, SpellField.Kind.WORLD_TREE, trunk, (float) TREE_REACH, 40, TREE_TICKS, caster)
					.data(caster.getRandom().nextInt()).spawn();
			// Its roots heave the land open as it grows.
			Devastation.fissures(caster, level, trunk, TREE_FISSURES, 12, 4, 4);
			HeavenSpells.sound(level, trunk, SoundEvents.ROOTED_DIRT_BREAK, 4.0f, 0.3f);
			HeavenSpells.sound(level, trunk, SoundEvents.WARDEN_EMERGE, 2.0f, 0.6f);
		}

		@Override
		protected boolean run(long age, long now) {
			// Its vines reach once it has grown (two seconds), and keep reaching for whoever comes.
			if (age >= 40 && age % 10 == 0) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, trunk, TREE_REACH, e -> e != caster && !HeavenSpells.spared(caster, e) && !seized.containsKey(e))) {
					double a = level.random.nextDouble() * Math.PI * 2;
					double r = 5 + level.random.nextDouble() * 9;
					seized.put(e, trunk.add(Math.cos(a) * r, 14 + level.random.nextDouble() * 14, Math.sin(a) * r));
					vines.put(e, SpellField.create(level, SpellField.Kind.TREE_VINE, trunk, (float) a, 24, (int) (end - level.getGameTime()), caster)
							.target(e).data(level.random.nextInt()).spawn());
					HeavenSpells.sound(level, e.position(), SoundEvents.ROOTED_DIRT_PLACE, 1.5f, 0.5f);
				}
			}
			for (java.util.Iterator<java.util.Map.Entry<LivingEntity, Vec3>> it = seized.entrySet().iterator(); it.hasNext(); ) {
				java.util.Map.Entry<LivingEntity, Vec3> entry = it.next();
				LivingEntity e = entry.getKey();
				if (!HeavenSpells.present(e, level)) {
					SpellField vine = vines.remove(e);
					if (vine != null) vine.discard();
					it.remove();
					continue;
				}
				// Drawn up to its bough over a second, then held.
				Vec3 hold = entry.getValue();
				Vec3 to = hold.subtract(e.position());
				if (to.lengthSqr() > 0.25) HeavenSpells.move(e, to.scale(0.2));
				else HeavenSpells.move(e, Vec3.ZERO);
				e.resetFallDistance();
				if (age % 10 == 5) {
					float taken = HeavenSpells.strikeAgain(caster, e, TREE_DRAIN / 2, spell);
					if (casterHere() && caster.position().distanceToSqr(trunk) < 96 * 96) HeavenSpells.mend(caster, taken);
				}
			}
			field.value(seized.size());
			return true;
		}

		/** Whom it holds now (tests). */
		public java.util.Set<LivingEntity> seized() { return seized.keySet(); }

		@Override
		public void end() {
			for (LivingEntity e : seized.keySet()) HeavenSpells.safeFall(e, 100);
			for (SpellField vine : vines.values()) vine.discard();
			field.discard();
		}
	}

	// --- Dao Integration ---

	/**
	 * Azure Dragon: the Azure Dragon of the east (SpellConstruct AZURE_DRAGON, its own GeckoLib model) bursts out of the ground where its
	 * caster looks and coils up into the sky. Whatever stands within {@link #DRAGON_RADIUS} of where it rises takes {@link #DRAGON_DAMAGE} and
	 * is caught in its coils, carried up with it and held there {@link #DRAGON_HOLD} ticks (a great beast only struck); the land it bursts
	 * from is torn open in {@link #DRAGON_FISSURES} fissures out to {@link #DRAGON_REACH}. At the top of its climb it roars.
	 */
	public static final class Dragon extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct dragon;
		private final java.util.Map<LivingEntity, Vec3> caught = new java.util.HashMap<>();

		Dragon(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, DRAGON_RISE + DRAGON_ROAR);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.ground = below != null ? below : point;
			this.dragon = SpellConstruct.create(level, SpellConstruct.Kind.AZURE_DRAGON, coil(0), 0, 0, DRAGON_RISE + DRAGON_ROAR, caster).sizedFor(spell).spawn();
			face(0);
			// It bursts up out of the land, whatever stands round it caught and struck.
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, DRAGON_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
				HeavenSpells.strike(caster, e, DRAGON_DAMAGE, spell);
				if (!HeavenSpells.greatBeast(e)) caught.put(e, e.position().subtract(ground).multiply(1, 0, 1));
				SpellFx.send(level, SpellFx.Kind.LEAVES, HeavenSpells.middle(e), Vec3.ZERO, 1.4f, 0);
			}
			Devastation.fissures(caster, level, ground, DRAGON_REACH, DRAGON_FISSURES, (int) Math.round(DRAGON_FISSURE_DEPTH), 4);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, (float) DRAGON_RADIUS * 0.6f,
					net.minecraft.world.level.block.Block.getId(Blocks.ROOTED_DIRT.defaultBlockState()));
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) DRAGON_RADIUS, 0, 192);
			HeavenSpells.sound(level, ground, SoundEvents.ENDER_DRAGON_GROWL, 3.0f, 1.5f);
			HeavenSpells.sound(level, ground, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.5f);
		}

		/** Where its head is {@code t} ticks into its climb: once round a coil {@link #DRAGON_COIL} round its rising, up {@link #DRAGON_HEIGHT}. */
		private Vec3 coil(double t) {
			double f = Math.min(1, t / DRAGON_RISE), angle = f * Math.PI * 2 * DRAGON_TURNS;
			return ground.add(Math.cos(angle) * DRAGON_COIL, -6 + (DRAGON_HEIGHT + 6) * (1 - (1 - f) * (1 - f)), Math.sin(angle) * DRAGON_COIL);
		}

		/** Turns its head along its climb. */
		private void face(double t) {
			Vec3 at = coil(t), way = coil(t + 1).subtract(at);
			if (way.lengthSqr() < 1.0e-6) way = new Vec3(0, 1, 0);
			float yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z));
			float pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(way.normalize().y, -1, 1)));
			dragon.place(at, yaw, pitch);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= DRAGON_RISE) face(age);
			if (age == DRAGON_RISE) {
				dragon.act("roar");
				HeavenSpells.sound(level, dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 4.0f, 1.2f);
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, dragon.position(), Vec3.ZERO, 12, 0, 192);
			}
			// Those in its coils go up with it, round its rising, and hang there till it lets go.
			if (age <= DRAGON_HOLD && !caught.isEmpty()) {
				double up = Math.min(dragon.getY() - 4, ground.y + DRAGON_HEIGHT * 0.7);
				for (java.util.Iterator<java.util.Map.Entry<LivingEntity, Vec3>> it = caught.entrySet().iterator(); it.hasNext(); ) {
					java.util.Map.Entry<LivingEntity, Vec3> held = it.next();
					LivingEntity e = held.getKey();
					if (!HeavenSpells.present(e, level)) {
						it.remove();
						continue;
					}
					Vec3 want = ground.add(held.getValue().scale(0.5)).add(0, Math.max(0, up - ground.y), 0);
					HeavenSpells.move(e, want.subtract(e.position()).scale(0.3));
					e.resetFallDistance();
				}
			}
			if (age == DRAGON_HOLD) for (LivingEntity e : caught.keySet()) HeavenSpells.safeFall(e, 60); // let go: what falls is a fall
			return true;
		}

		@Override
		public void end() {
			dragon.dissolve();
		}
	}

	/**
	 * Myriad Woods Return to the Dao: a spirit tree of jade light (SpellConstruct DAO_TREE) grows where its caster stands and holds the land
	 * {@link #WOODS_RADIUS} round for {@link #WOODS_TICKS}: every second each foe in it is rooted and drained of {@link #WOODS_DRAIN}, all of it
	 * shared out as healing to the caster and its own within the woods. It withers away at the end; the land is left as it was.
	 */
	public static final class Woods extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final SpellConstruct tree;

		Woods(LivingEntity caster, Spell spell) {
			super(caster, WOODS_TICKS);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, caster.position().add(0, 1, 0), 2, 16);
			this.at = below != null ? below : caster.position();
			this.tree = SpellConstruct.create(level, SpellConstruct.Kind.DAO_TREE, at, caster.getYRot(), 0, WOODS_TICKS, caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, at, SoundEvents.AZALEA_LEAVES_PLACE, 3.0f, 0.5f);
			HeavenSpells.sound(level, at, SoundEvents.BEACON_ACTIVATE, 2.0f, 1.4f);
			SpellFx.send(level, SpellFx.Kind.LEAVES, at.add(0, 2, 0), Vec3.ZERO, 4, 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age == WOODS_TICKS - 30) tree.act("wither");
			if (age % 20 != 10) return true;
			float drawn = 0;
			Vec3 crown = at.add(0, 10, 0);
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, WOODS_RADIUS, e -> !HeavenSpells.friendly(caster, e) && !HeavenSpells.spared(caster, e))) {
				drawn += HeavenSpells.strikeAgain(caster, e, WOODS_DRAIN, spell);
				e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 3)); // rooted
				SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, HeavenSpells.middle(e), crown.subtract(HeavenSpells.middle(e)), 1, 0); // its life, drawn to the tree
			}
			if (drawn > 0) {
				List<LivingEntity> own = HeavenSpells.aroundPoint(level, at, WOODS_RADIUS, e -> HeavenSpells.friendly(caster, e));
				if (!own.contains(caster)) own.add(caster);
				for (LivingEntity e : own) {
					HeavenSpells.mend(e, drawn / own.size());
					SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, crown, HeavenSpells.middle(e).subtract(crown), 0, 0); // and given to its own
				}
			}
			if (age % 60 == 10) HeavenSpells.sound(level, at, SoundEvents.AZALEA_LEAVES_BREAK, 2.0f, 0.7f);
			return true;
		}

		@Override
		public void end() {
			tree.dissolve();
		}
	}

	// --- Void Shattering ---

	/**
	 * Spring Thunder of the Azure Emperor (the Green Emperor of the east and the spring: in the old lore thunder is wood's): a storm cloud of
	 * jade (SpellConstruct AZURE_THUNDER_CLOUD, its own GeckoLib model: the thunder trigram ☳ burning under it) gathers
	 * {@link #THUNDER_HIGH} over where its caster looks ({@link #THUNDER_GATHER} ticks) and calls spring thunder down in {@link #THUNDER_VOLLEYS}
	 * volleys, {@link #THUNDER_EVERY} ticks apart: in each, every foe under it (within {@link #THUNDER_RADIUS}) is struck by a bolt of jade
	 * lightning for {@link #THUNDER_DAMAGE} and rooted, and {@link #THUNDER_STRAYS} more bolts strike the land beneath, each leaving a little
	 * crater (a player's).
	 */
	public static final class Thunder extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct cloud;
		private int volleys;

		Thunder(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, THUNDER_GATHER + THUNDER_VOLLEYS * THUNDER_EVERY);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.cloud = SpellConstruct.create(level, SpellConstruct.Kind.AZURE_THUNDER_CLOUD, ground.add(0, THUNDER_HIGH, 0), caster.getYRot(), 0, (int) (end - start), caster)
					.sizedFor(spell).spawn();
			HeavenSpells.sound(level, ground.add(0, THUNDER_HIGH, 0), SoundEvents.LIGHTNING_BOLT_THUNDER, 3.0f, 0.5f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_ACTIVATE, 2.0f, 1.2f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < THUNDER_GATHER || (age - THUNDER_GATHER) % THUNDER_EVERY != 0 || volleys >= THUNDER_VOLLEYS) {
				if (age % 30 == 15) HeavenSpells.sound(level, ground.add(0, THUNDER_HIGH, 0), SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0f, 0.4f);
				return true;
			}
			volleys++;
			cloud.act("flash");
			boolean first = true;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, THUNDER_RADIUS + THUNDER_HIGH, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > THUNDER_RADIUS || e.getY() > ground.y + THUNDER_HIGH - 2 || e.getY() < ground.y - 16) continue;
				bolt(e.position(), first, false);
				first = false;
				HeavenSpells.strikeAgain(caster, e, THUNDER_DAMAGE, spell);
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, THUNDER_ROOT, 6)); // rooted where it stands
				SpellFx.send(level, SpellFx.Kind.LEAVES, HeavenSpells.middle(e), Vec3.ZERO, 1.2f, 0);
			}
			// And at the land beneath, the storm's own.
			for (int i = 0; i < THUNDER_STRAYS; i++) {
				double angle = level.random.nextDouble() * Math.PI * 2, out = Math.sqrt(level.random.nextDouble()) * THUNDER_RADIUS;
				Vec3 at = ground.add(Math.cos(angle) * out, 0, Math.sin(angle) * out);
				Vec3 land = HeavenSpells.groundBelow(level, at.add(0, 16, 0), 0, 48);
				bolt(land != null ? land : at, first, true);
				first = false;
			}
			return true;
		}

		/** A bolt of jade spring thunder from the cloud down to {@code at} (the volley's first lights the sky); one at the land leaves a little crater. */
		private void bolt(Vec3 at, boolean flash, boolean land) {
			TribulationLightning bolt = ModEntities.TRIBULATION_LIGHTNING.create(level);
			if (bolt != null) {
				bolt.moveTo(at.x, at.y, at.z);
				bolt.setCloudBaseY(ground.y + THUNDER_HIGH);
				bolt.setScale(2.5f);
				bolt.setTint(THUNDER_JADE);
				bolt.setVisualOnly(true); // its harm is the spell's own
				bolt.setDim(!flash);
				level.addFreshEntity(bolt);
			}
			if (land) Devastation.crater(caster, level, at, THUNDER_CRATER, THUNDER_CRATER_DEPTH, Devastation.Floor.NONE);
		}

		@Override
		public void end() {
			cloud.dissolve();
		}
	}

	/**
	 * Void Withering (the Void-Rooted Divine Tree's, whose roots hang in the void and drink from nothing): space tears open in a slash across the
	 * sky {@link #WITHER_HIGH} over where its caster looks (SpellConstruct VOID_RIFT lying across), and once it is open the void tree's roots
	 * spill down out of it (VOID_ROOTS, its own GeckoLib model). For {@link #WITHER_TICKS} they reach out and drink (the user's ask,
	 * 2026-10-24: roots that grab things, out of a rift): a root lashes down to each foe within {@link #WITHER_RADIUS} (VOID_TENDRIL, drawn by
	 * hand: TendrilRenderer), {@link #WITHER_GRASPS} at most at once, coils round it and hauls it up into the air ({@link #WITHER_LIFT}; a great
	 * beast only held where it stands); every second each foe within reach is drained of {@link #WITHER_DRAIN}, weakened and slowed, all of it
	 * shared out as healing to the caster and its own within reach; the land under them withers outward from their heart (Devastation#wither: a
	 * player's). Then they let go (what they held falls, unharmed by its fall) and draw back up, and the rift closes. Its caster gone, it goes.
	 */
	public static final class Withering extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, mouth;
		private final float yaw;
		private final SpellConstruct rift;
		private SpellConstruct roots;
		/** What its roots hold, oldest first: each held one's root, where it is hauled to, and when it was seized (its age then). */
		private final java.util.Map<LivingEntity, Held> held = new java.util.LinkedHashMap<>();
		/** How far out the land has withered. */
		private double withered;

		private record Held(SpellConstruct root, Vec3 to, long since) {}

		Withering(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, WITHER_OPENS + WITHER_TICKS + SpellConstruct.FADE);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.mouth = ground.add(0, WITHER_HIGH, 0);
			this.yaw = caster.getYRot();
			// A slash lying across the sky (RiftRenderer: a tear's yaw turns it on its watchers' screens), the roots to come out of its underside.
			this.rift = SpellConstruct.create(level, SpellConstruct.Kind.VOID_RIFT, mouth, 90, 0, (int) (end - start), caster).size((float) WITHER_RIFT).spawn();
			HeavenSpells.sound(level, mouth, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.6f);
			SpellFx.send(level, SpellFx.Kind.FLASH, mouth, Vec3.ZERO, 6, SpellConstruct.VOID, 192);
		}

		/** Whether {@code e} is one its roots may seize and drink: a foe within reach of the land beneath the rift. */
		private boolean within(LivingEntity e) {
			return Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) <= WITHER_RADIUS && e.getY() <= mouth.y && e.getY() >= ground.y - 16;
		}

		/** Foes within reach not yet held, nearest first, seized by a root each, as many as it may hold. */
		private void seize(long age) {
			if (held.size() >= WITHER_GRASPS) return;
			List<LivingEntity> near = HeavenSpells.aroundPoint(level, ground, WITHER_RADIUS + WITHER_HIGH,
					e -> !held.containsKey(e) && within(e) && !HeavenSpells.friendly(caster, e) && !HeavenSpells.spared(caster, e));
			near.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(ground)));
			for (LivingEntity e : near) {
				if (held.size() >= WITHER_GRASPS) break;
				// Each out of the rift's underside a little apart, lashing down to its catch.
				Vec3 from = mouth.add((level.random.nextDouble() - 0.5) * WITHER_RIFT * 0.5, -1.2, (level.random.nextDouble() - 0.5) * WITHER_RIFT * 0.5);
				SpellConstruct root = SpellConstruct.create(level, SpellConstruct.Kind.VOID_TENDRIL, from, 0, 0, (int) (end - level.getGameTime()), caster)
						.size((float) HeavenSpells.greatness(spell.realm())).target(e).spawn();
				// Hauled up off the land, drawn in a little under the rift.
				Vec3 to = new Vec3(ground.x + (e.getX() - ground.x) * 0.6, ground.y + WITHER_LIFT, ground.z + (e.getZ() - ground.z) * 0.6);
				held.put(e, new Held(root, to, age));
				HeavenSpells.sound(level, from, SoundEvents.ROOTED_DIRT_BREAK, 2.5f, 0.5f);
			}
		}

		/** Each held one hauled toward where it is to hang once its root has reached it (a great beast only held where it stands); the gone let go. */
		private void haul(long age) {
			for (java.util.Iterator<java.util.Map.Entry<LivingEntity, Held>> it = held.entrySet().iterator(); it.hasNext(); ) {
				java.util.Map.Entry<LivingEntity, Held> entry = it.next();
				LivingEntity e = entry.getKey();
				Held h = entry.getValue();
				if (!HeavenSpells.present(e, level) || HeavenSpells.spared(caster, e) || Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > WITHER_RADIUS + 8) {
					h.root().dissolve();
					it.remove();
					continue;
				}
				if (age - h.since() < WITHER_LASH) continue; // its root still reaching it
				if (age - h.since() == WITHER_LASH) HeavenSpells.sound(level, e.position(), SoundEvents.ROOTED_DIRT_PLACE, 2.0f, 0.6f);
				// Nothing flies out of its grip (QiFlight#weighDown: a held player's flight given up while it holds; a cultivator's too).
				if (e instanceof net.minecraft.server.level.ServerPlayer player) QiFlight.weighDown(player, 10);
				if (e instanceof CultivatorNpc npc && npc.isQiFlying()) npc.setQiFlying(false);
				if (HeavenSpells.greatBeast(e)) {
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));
					HeavenSpells.still(e);
				} else {
					HeavenSpells.move(e, h.to().subtract(e.position()).scale(0.22));
					e.resetFallDistance();
				}
			}
		}

		/** Its roots let go of all they hold: what they held falls, unharmed by the fall, and the roots draw back up. */
		private void release() {
			for (java.util.Map.Entry<LivingEntity, Held> entry : held.entrySet()) {
				entry.getValue().root().dissolve();
				if (HeavenSpells.present(entry.getKey(), level)) HeavenSpells.safeFall(entry.getKey(), 60);
			}
			held.clear();
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (roots == null && age >= WITHER_ROOTS) {
				// Out of the rift once it is open: the knot swelling out of it, the roots growing down out of the knot (its model's descend).
				roots = SpellConstruct.create(level, SpellConstruct.Kind.VOID_ROOTS, mouth.add(0, -2, 0), yaw, 0, (int) (end - level.getGameTime()), caster).sizedFor(spell)
						.spawn();
				HeavenSpells.sound(level, mouth, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.4f);
			}
			long drinking = age - WITHER_OPENS;
			if (drinking == WITHER_TICKS - 20) {
				roots.act("withdraw");
				release();
				HeavenSpells.sound(level, mouth, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.6f);
			}
			if (drinking >= 0 && drinking < WITHER_TICKS - 20) {
				if (drinking % 5 == 0) seize(age);
				haul(age);
			}
			if (drinking < 0 || drinking > WITHER_TICKS || drinking % 20 != 10) return true;
			// The land withers outward from beneath the rift, all of it by the last draught.
			double reach = WITHER_RADIUS * Math.min(1, (drinking + 10) / (double) WITHER_TICKS);
			Devastation.wither(caster, level, ground, withered, reach);
			withered = reach;
			float drawn = 0;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, WITHER_RADIUS + WITHER_HIGH, e -> !HeavenSpells.friendly(caster, e) && !HeavenSpells.spared(caster, e))) {
				if (!within(e) && !held.containsKey(e)) continue; // one hauled up still drinks
				drawn += HeavenSpells.strikeAgain(caster, e, WITHER_DRAIN, spell);
				e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1));
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
				SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, HeavenSpells.middle(e), mouth.subtract(HeavenSpells.middle(e)), 1, 0); // its life, drunk up into the void
			}
			if (drawn > 0) {
				List<LivingEntity> own = HeavenSpells.aroundPoint(level, ground, WITHER_RADIUS, e -> HeavenSpells.friendly(caster, e));
				if (!own.contains(caster)) own.add(caster);
				for (LivingEntity e : own) {
					HeavenSpells.mend(e, drawn / own.size());
					SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, mouth, HeavenSpells.middle(e).subtract(mouth), 0, 0); // and given to its own
				}
			}
			if (drinking % 60 == 10) HeavenSpells.sound(level, mouth, SoundEvents.ROOTED_DIRT_PLACE, 3.0f, 0.4f);
			return true;
		}

		@Override
		public void end() {
			release();
			rift.dissolve();
			if (roots != null) roots.dissolve();
		}
	}

	static void tick(MinecraftServer server) {}

	static void forget(UUID id) {}

	static void clear() {}

	private WoodSpells() {}
}
