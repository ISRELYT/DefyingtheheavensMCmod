package com.example.defyingtheheavens;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * A blade forged at the Artifact Forge (ArtifactKind: the four weapons; the user's asks, 2026-10-28), every one a sword a cultivator can
 * ride (SwordFlight): a flat blow by kind and grade, and from Spirit grade up its grade's might added to all its wielder's blows
 * (a share of their whole attack: what keeps a blade worth holding as the realms' own strength grows). Each kind has its way:
 * <ul>
 *   <li>the flying sword (jian) is made to be ridden: Sword Flight on it costs less qi ({@link #flightShare});</li>
 *   <li>the saber (dao) is the heaviest and the slowest, its might the greatest;</li>
 *   <li>the spear (qiang) pierces: its blow runs on into the foes behind its target ({@link #pierce});</li>
 *   <li>the glaive (guandao) sweeps everyone round its wielder ({@link #sweep}).</li>
 * </ul>
 * A Fine or Perfect piece's work (Artifacts#Quality) raises all of it by a tenth or a quarter: the stack's blow by its work
 * ({@link #getAttributeModifiers(ItemStack, EquipmentSlot)}), the base blow's own ids kept so its tooltip reads as a sword's does.
 */
public class ArtifactWeaponItem extends SwordItem {
	/** The blow each kind deals at each grade, as its tooltip shows it (the wielder's own 1 with it). */
	private static final int[][] BLOW = {
			{6, 7, 8, 9, 10},   // flying sword
			{7, 8, 9, 11, 12},  // saber
			{6, 7, 8, 9, 10},   // spear
			{7, 8, 9, 10, 12}}; // glaive
	/** What each kind adds at each grade to all its wielder's blows (a share of their whole attack). */
	private static final double[][] MIGHT = {
			{0, 0.05, 0.10, 0.15, 0.25},
			{0, 0.07, 0.14, 0.21, 0.32},
			{0, 0.05, 0.10, 0.15, 0.25},
			{0, 0.05, 0.10, 0.15, 0.25}};
	/** Swings a second, by kind. */
	private static final double[] SPEED = {1.6, 1.2, 1.3, 1.0};
	/** The share of Sword Flight's qi a flying sword saves its rider, by grade. */
	private static final double[] FLIGHT_SAVING = {0.10, 0.20, 0.30, 0.40, 0.50};
	/** A spear's pierce, by grade: how many foes behind its target, and their share of the blow. */
	private static final int[] PIERCE = {1, 1, 2, 2, 3};
	private static final double[] PIERCE_SHARE = {0.50, 0.55, 0.60, 0.65, 0.75};
	/** A glaive's sweep, by grade: how far round its wielder, and the share of the blow. */
	private static final double[] SWEEP_REACH = {2.5, 2.75, 3.0, 3.25, 3.5};
	private static final double[] SWEEP_SHARE = {0.40, 0.45, 0.50, 0.55, 0.65};
	private static final UUID MIGHT_ID = UUID.fromString("5b3e7c1a-9d24-4f61-8a0e-2c71d4b9e6f3");
	/** When each wielder's spear last pierced or glaive last swept (game time): once a swing at most, not with every quick tap. */
	private static final Map<LivingEntity, Long> LAST_REACH = new WeakHashMap<>();

	private final ArtifactKind kind;
	private final ArtifactGrade grade;
	/** Its blow (as its tooltip shows it), its swings a second and its might (a share of its wielder's whole attack). */
	private final double blow, speed, might;
	/** Its blow, swing and might in a main hand, by its work (Artifacts#Quality's order). */
	private final List<Multimap<Attribute, AttributeModifier>> modifiers = new java.util.ArrayList<>();

	public ArtifactWeaponItem(ArtifactKind kind, ArtifactGrade grade, Properties properties) {
		this(kind, grade, BLOW[kind.ordinal()][grade.ordinal()], SPEED[kind.ordinal()], MIGHT[kind.ordinal()][grade.ordinal()], properties);
	}

	/** A blade of {@code kind} at {@code grade} with a blow, swing and might of its own (ElementalWeaponItem's). */
	protected ArtifactWeaponItem(ArtifactKind kind, ArtifactGrade grade, double blow, double speed, double might, Properties properties) {
		super(Material.values()[grade.ordinal()], (int) Math.round(blow) - 1, (float) speed - 4, properties);
		this.kind = kind;
		this.grade = grade;
		this.blow = blow;
		this.speed = speed;
		this.might = might;
		for (Artifacts.Quality quality : Artifacts.Quality.values()) modifiers.add(modifiers(quality.power()));
	}

	public ArtifactKind kind() { return kind; }
	public ArtifactGrade grade() { return grade; }
	/** Swings a second. */
	public double speed() { return speed; }

	/** Its blow, its swing and its might, all times {@code power} (a Fine or Perfect piece's work). */
	private Multimap<Attribute, AttributeModifier> modifiers(double power) {
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", blow * power - 1,
				AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", speed - 4,
				AttributeModifier.Operation.ADDITION));
		if (might * power > 0) {
			builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(MIGHT_ID, "Artifact might", might * power, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
		return builder.build();
	}

	/**
	 * Whether {@code attacker}'s blade may reach out this swing (a spear's pierce, a glaive's sweep, an elemental weapon's element): once
	 * a full swing at most, not with every quick tap.
	 */
	protected boolean mayReach(ServerLevel level, LivingEntity attacker) {
		Long last = LAST_REACH.get(attacker);
		long now = level.getGameTime();
		if (last != null && now - last < Math.round(20 / speed) - 1) return false;
		LAST_REACH.put(attacker, now);
		return true;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND ? modifiers.get(Artifacts.Quality.NORMAL.ordinal()) : super.getDefaultAttributeModifiers(slot);
	}

	/** A Fine or Perfect piece's blow by its work (Fabric's stack-aware modifiers: a stack's own NBT modifiers would still win). */
	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND ? modifiers.get(Artifacts.quality(stack).ordinal()) : getDefaultAttributeModifiers(slot);
	}

	/** What Sword Flight costs its rider on {@code held}: a flying sword's saving off (its work's worth in it); any other blade, all of it. */
	public static double flightShare(ItemStack held) {
		if (!(held.getItem() instanceof ArtifactWeaponItem weapon) || weapon.kind != ArtifactKind.FLYING_SWORD) return 1;
		return Math.max(0, 1 - FLIGHT_SAVING[weapon.grade.ordinal()] * Artifacts.power(held));
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker.level() instanceof ServerLevel level && (kind == ArtifactKind.SPEAR || kind == ArtifactKind.GLAIVE) && mayReach(level, attacker)) {
			if (kind == ArtifactKind.SPEAR) pierce(level, stack, target, attacker);
			else sweep(level, stack, target, attacker);
		}
		return super.hurtEnemy(stack, target, attacker);
	}

	protected static DamageSource blowOf(LivingEntity attacker) {
		return attacker instanceof Player player ? attacker.damageSources().playerAttack(player) : attacker.damageSources().mobAttack(attacker);
	}

	/** A spear's blow runs on through its target into the nearest foes behind it, along the way the wielder faces. */
	private void pierce(ServerLevel level, ItemStack stack, LivingEntity target, LivingEntity attacker) {
		int g = grade.ordinal();
		Vec3 way = attacker.getLookAngle().multiply(1, 0, 1);
		if (way.lengthSqr() < 1.0e-6) return;
		way = way.normalize();
		Vec3 from = target.position();
		Vec3 along = way;
		AABB reach = target.getBoundingBox().expandTowards(way.scale(3.5)).inflate(0.75, 0.5, 0.75);
		List<LivingEntity> behind = level.getEntitiesOfClass(LivingEntity.class, reach,
				e -> e != target && Artifacts.mayStrike(attacker, e) && e.position().subtract(from).dot(along) > 0.1);
		if (behind.isEmpty()) return;
		behind.sort(Comparator.comparingDouble(e -> e.distanceToSqr(target)));
		float blow = (float) (attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * PIERCE_SHARE[g] * Artifacts.power(stack));
		DamageSource source = blowOf(attacker);
		for (int i = 0; i < Math.min(PIERCE[g], behind.size()); i++) behind.get(i).hurt(source, blow);
		for (int i = 1; i <= 7; i++) {
			Vec3 at = from.add(way.scale(i * 0.5)).add(0, target.getBbHeight() * 0.6, 0);
			level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.02);
		}
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.8f, 1.3f);
	}

	/** A glaive's swing takes in everyone round its wielder, each knocked back a little. */
	private void sweep(ServerLevel level, ItemStack stack, LivingEntity target, LivingEntity attacker) {
		int g = grade.ordinal();
		double reach = SWEEP_REACH[g];
		List<LivingEntity> round = level.getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(reach, 1.0, reach),
				e -> e != target && Artifacts.mayStrike(attacker, e) && e.distanceToSqr(attacker) <= reach * reach);
		float blow = (float) (attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * SWEEP_SHARE[g] * Artifacts.power(stack));
		DamageSource source = blowOf(attacker);
		for (LivingEntity e : round) {
			e.knockback(0.4, attacker.getX() - e.getX(), attacker.getZ() - e.getZ());
			e.hurt(source, blow);
		}
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, attacker.getX() + Math.cos(angle) * reach * 0.6, attacker.getY() + 1.0,
					attacker.getZ() + Math.sin(angle) * reach * 0.6, 1, 0, 0, 0, 0);
		}
		level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
	}

	@Override
	public Component getName(ItemStack stack) {
		return Artifacts.name(super.getName(stack), stack, grade);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return grade == ArtifactGrade.IMMORTAL || Artifacts.quality(stack) == Artifacts.Quality.PERFECT;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		Artifacts.describe(stack, grade, lines);
		int g = grade.ordinal();
		double power = Artifacts.power(stack);
		Component perk = switch (kind) {
			case FLYING_SWORD -> Component.translatable(ModLang.FLYING_SWORD_PERK, Artifacts.percent(FLIGHT_SAVING[g] * power));
			case SABER -> Component.translatable(ModLang.SABER_PERK);
			case SPEAR -> Component.translatable(ModLang.SPEAR_PERK, PIERCE[g], Artifacts.percent(PIERCE_SHARE[g] * power));
			case GLAIVE -> Component.translatable(ModLang.GLAIVE_PERK, String.format(java.util.Locale.ROOT, "%.1f", SWEEP_REACH[g]),
					Artifacts.percent(SWEEP_SHARE[g] * power));
			default -> null;
		};
		if (perk != null) Tooltips.add(lines, perk.copy().withStyle(ChatFormatting.BLUE));
	}

	/** The metal of each grade's blades: iron's wear to netherite's and beyond; an Immortal blade never wears. */
	private enum Material implements Tier {
		//       uses  mining  level  enchant
		MORTAL(   400,   6.0f,   2,    14),
		SPIRIT(   900,   8.0f,   2,    22),
		EARTH(   1800,   8.0f,   3,    10),
		HEAVEN(  3000,   9.0f,   4,    15),
		IMMORTAL(   0,  12.0f,   4,    25);

		private final int uses;
		private final float speed;
		private final int level;
		private final int enchantment;

		Material(int uses, float speed, int level, int enchantment) {
			this.uses = uses;
			this.speed = speed;
			this.level = level;
			this.enchantment = enchantment;
		}

		@Override public int getUses() { return uses; }
		@Override public float getSpeed() { return speed; }
		@Override public float getAttackDamageBonus() { return 0; }
		@Override public int getLevel() { return level; }
		@Override public int getEnchantmentValue() { return enchantment; }
		@Override public Ingredient getRepairIngredient() { return Ingredient.of(ArtifactGrade.values()[ordinal()].metal()); }
	}
}
