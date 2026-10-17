package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
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
	public static final double BAMBOO_RANGE = 48, BAMBOO_RADIUS = 24, BAMBOO_BREAK = 2.5;
	public static final int BAMBOO_TARGETS = 6;
	public static final float BAMBOO_DAMAGE = 70;
	public static final double SPRING_RADIUS = 32;
	public static final float SPRING_BURST = 40, SPRING_HEAL = 4;
	public static final int SPRING_TICKS = 10 * 20;
	public static final double VINES_RANGE = 64, VINES_RADIUS = 40;
	public static final int VINES_TARGETS = 12, VINES_TICKS = 5 * 20;
	public static final float VINES_DAMAGE = 22;
	public static final double TREE_RANGE = 24, TREE_REACH = 32;
	public static final int TREE_TICKS = 12 * 20;
	public static final float TREE_DRAIN = 10;

	private static final UUID BOUND_ID = UUID.fromString("2b8f4e17-91c3-4d6a-b5e0-7a3c1f9d2e64");

	static boolean canCast(ServerPlayer p, Spell spell) {
		return switch (spell) {
			case LIFE_FORCE_DRAIN, LIFE_FORCE_GAIN -> {
				LifeTarget target = lifeTarget(p, spell == Spell.LIFE_FORCE_DRAIN, true);
				yield target != LifeTarget.TOLD && (target != null || HeavenSpells.refuse(p));
			}
			case MYSTERIOUS_BRISTLE_PRISON -> HeavenSpells.aimed(p, PRISON_RANGE) != null || HeavenSpells.refuse(p);
			case VERDANT_GROWTH -> p.onGround() || HeavenSpells.refuse(p);
			default -> true;
		};
	}

	static void cast(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
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

		Bound(ServerPlayer caster, LivingEntity target, Vec3 at, int ticks) {
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
	static Bound bind(ServerPlayer p, LivingEntity target, int ticks) {
		for (Bound bound : HeavenSpells.lingering(p.getUUID(), Bound.class)) {
			if (bound.target == target) HeavenSpells.stop(bound);
		}
		Bound bound = new Bound(p, target, target.position(), ticks);
		HeavenSpells.linger(bound);
		return bound;
	}

	// --- Qi Refining ---

	private static void bullet(ServerPlayer p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.WOOD_STAKE, 1.0f, 6, new Vec3(0.35, -0.25, 1.1), BULLET_SPEED, BULLET_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						HeavenSpells.strike(p, target, BULLET_DAMAGE, spell, missile);
						HeavenSpells.push(missile.position().subtract(missile.getDeltaMovement()), target, 0.4);
						SpellFx.send(p.serverLevel(), SpellFx.Kind.SPLINTERS, missile.position(), missile.getDeltaMovement(), 0.6f, 0);
						HeavenSpells.sound(p.serverLevel(), target.position(), SoundEvents.WOOD_HIT, 1.0f, 1.2f);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						SpellFx.send(p.serverLevel(), SpellFx.Kind.SPLINTERS, hit.getLocation(), missile.getDeltaMovement(), 0.6f, 0);
						HeavenSpells.sound(p.serverLevel(), hit.getLocation(), SoundEvents.WOOD_BREAK, 0.8f, 1.2f);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(p.serverLevel(), missile.position(), SoundEvents.ARROW_SHOOT, 0.8f, 0.6f);
					}
				});
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.AZALEA_PLACE, 1.0f, 1.3f);
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

		Growth(ServerPlayer caster) {
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
	static BlockPos plantLookedAt(ServerPlayer p) {
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

	static LifeTarget lifeTarget(ServerPlayer p, boolean drain, boolean tell) {
		LivingEntity being = drain ? HeavenSpells.aimed(p, LIFE_RANGE) : HeavenSpells.aimedAny(p, LIFE_RANGE);
		if (being != null) return new LifeTarget(being, null, null);
		BlockPos plant = plantLookedAt(p);
		if (plant != null && drain && p.level().getBlockEntity(plant) instanceof CultivationFruitBlockEntity) plant = null;
		if (plant != null && sealedTreasury(p.serverLevel(), plant)) {
			if (tell) p.displayClientMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_SEALED), true);
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
			if (tell) p.displayClientMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_HOLD_ONE), true);
			return LifeTarget.TOLD;
		}
		return null;
	}

	/** Where a held fruit or herb is: in front of the caster's chest, the stream running from it. */
	private static Vec3 heldPoint(ServerPlayer p) {
		return p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.45, 0);
	}

	private static void life(ServerPlayer p, Spell spell, boolean drain) {
		HeavenSpells.endLingering(p.getUUID(), Life.class);
		LifeTarget target = lifeTarget(p, drain, false);
		if (target == null || target == LifeTarget.TOLD) return;
		HeavenSpells.linger(new Life(p, spell, drain, target.being(), target.plant(), target.hand()));
		HeavenSpells.sound(p.serverLevel(), p.position(), drain ? SoundEvents.SCULK_CATALYST_BLOOM : SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);
	}

	/**
	 * Life Force Drain or Gain on one being, one herb growing, or one fruit or herb held: for {@link #LIFE_TICKS}, a stream of
	 * emerald motes runs between it and its caster (SpellField LIFE_STREAM). Drawing: the being's life ({@link #DRAIN_PER_SECOND} of
	 * wood a second) mends the caster, a herb's or fruit's years ({@link #DRAIN_YEARS} a second) are lost and mend the caster a
	 * little. Giving: the caster's own life runs out ({@link #GAIN_COST} a second, never below {@link #GAIN_FLOOR}) to heal a being or
	 * age a herb. It breaks if they stray apart, or the one held leaves the hand.
	 */
	public static final class Life extends HeavenSpells.Timed {
		private final Spell spell;
		private final boolean drain;
		public final LivingEntity being;
		public final BlockPos plant;
		/** The hand holding the fruit or herb drawn on (or given to), or null. */
		public final net.minecraft.world.InteractionHand hand;
		private final SpellField field;

		Life(ServerPlayer caster, Spell spell, boolean drain, LivingEntity being, BlockPos plant, net.minecraft.world.InteractionHand hand) {
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
				} else {
					int years = plant != null ? plantAge(level, plant) : heldAge(held());
					if (years <= 1) return false;
					if (plant != null) setPlantAge(level, plant, Math.max(1, years - DRAIN_YEARS / 2));
					else setHeldAge(held(), Math.max(1, years - DRAIN_YEARS / 2));
					HeavenSpells.mend(caster, PLANT_DRAIN_HEAL / 2);
				}
			} else {
				if (caster.getHealth() <= GAIN_FLOOR) return false;
				caster.setHealth(Math.max(GAIN_FLOOR, caster.getHealth() - GAIN_COST / 2));
				if (being != null) HeavenSpells.mend(being, GAIN_HEAL / 2);
				else if (plant != null) setPlantAge(level, plant, Math.min(FruitAge.MAX_YEARS, plantAge(level, plant) + GAIN_YEARS / 2));
				else setHeldAge(held(), Math.min(FruitAge.MAX_YEARS, heldAge(held()) + GAIN_YEARS / 2));
			}
			if (age % 20 == 19) HeavenSpells.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, drain ? 0.7f : 1.5f);
			return true;
		}

		@Override
		public void end() {
			field.discard();
			if (being != null) SpellAuras.remove(being, SpellAura.LIFE_FLOW);
		}
	}

	private static void prison(ServerPlayer p, Spell spell) {
		LivingEntity target = HeavenSpells.aimed(p, PRISON_RANGE);
		if (target == null) return;
		HeavenSpells.linger(new Prison(p, spell, target));
		HeavenSpells.sound(p.serverLevel(), target.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.6f);
		HeavenSpells.sound(p.serverLevel(), target.position(), SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.5f, 0.7f);
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

		Prison(ServerPlayer caster, Spell spell, LivingEntity target) {
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

		LeafStorm(ServerPlayer caster, Spell spell) {
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

	private static void spears(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
		LivingEntity aimed = HeavenSpells.aimed(p, SPEARS_RANGE);
		Vec3 point = aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SPEARS_RANGE);
		Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 1, 0), 3, 16);
		Vec3 at = ground != null ? ground : point;
		SpellField.create(level, SpellField.Kind.THORN_SPEARS, at, (float) SPEARS_RADIUS, 4, 50, p).data(level.random.nextInt()).spawn();
		HeavenSpells.sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, 2.0f, 0.5f);
		HeavenSpells.linger(new FireFromBelow(p, 6, () -> {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, SPEARS_RADIUS + 1, e -> !HeavenSpells.friendly(p, e) && !HeavenSpells.spared(p, e))) {
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

		FireFromBelow(ServerPlayer caster, int ticks, Runnable then) {
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

		Lotus(ServerPlayer caster) {
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

	/** Up to {@code count} foes of {@code p}'s within {@code radius} of {@code centre}, nearest it first. */
	static List<LivingEntity> foesNear(ServerPlayer p, Vec3 centre, double radius, int count) {
		List<LivingEntity> found = new ArrayList<>(HeavenSpells.aroundPoint(p.serverLevel(), centre, radius,
				e -> !HeavenSpells.friendly(p, e) && !HeavenSpells.spared(p, e)));
		found.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(centre)));
		return found.size() > count ? found.subList(0, count) : found;
	}

	private static void bamboo(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
		Vec3 centre = HeavenSpells.lookPoint(p, BAMBOO_RANGE);
		List<LivingEntity> foes = foesNear(p, centre, BAMBOO_RADIUS, BAMBOO_TARGETS);
		if (foes.isEmpty()) {
			Vec3 ground = HeavenSpells.groundBelow(level, centre.add(0, 1, 0), 3, 16);
			SpellField.create(level, SpellField.Kind.BAMBOO, ground != null ? ground : centre, 0.9f, 18, 50, p).data(level.random.nextInt()).spawn();
		}
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

		Spring(ServerPlayer caster) {
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

	private static void vines(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
		Vec3 centre = HeavenSpells.lookPoint(p, VINES_RANGE);
		List<LivingEntity> foes = foesNear(p, centre, VINES_RADIUS, VINES_TARGETS);
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

		Vines(ServerPlayer caster, Spell spell, LivingEntity target) {
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

		WorldTree(ServerPlayer caster, Spell spell) {
			super(caster, TREE_TICKS);
			this.spell = spell;
			Vec3 point = HeavenSpells.lookPoint(caster, TREE_RANGE);
			if (point.distanceToSqr(caster.position()) < 36) point = caster.position().add(Vec3.directionFromRotation(0, caster.getYRot()).scale(8));
			Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.trunk = ground != null ? ground : point;
			this.field = SpellField.create(level, SpellField.Kind.WORLD_TREE, trunk, (float) TREE_REACH, 40, TREE_TICKS, caster)
					.data(caster.getRandom().nextInt()).spawn();
			// Its roots heave the land open as it grows.
			Devastation.fissures(caster, level, trunk, TREE_REACH * 0.55, 8, 3, 4);
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

	static void tick(MinecraftServer server) {}

	static void forget(UUID id) {}

	static void clear() {}

	private WoodSpells() {}
}
