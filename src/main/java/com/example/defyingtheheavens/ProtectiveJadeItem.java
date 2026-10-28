package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A protective jade, forged at the Artifact Forge (the user's pick, 2026-10-28): a carved jade disc worn in a ring slot. A share of the
 * harm its wearer takes, by its grade (10% to 30%, its work's worth with it), comes out of their qi instead of their health, while they
 * have the qi: a whole health bar's worth of harm turned costs half their pool, at any realm ({@link #QI_PER_HEALTH}). It never turns a
 * tribulation's strike, the Five Decays' or a lifespan's (HeavenSpells#incoming lets those land whole, as the wards do), nor the void.
 * Two worn don't add up: the better holds.
 */
public class ProtectiveJadeItem extends RingItem {
	/** The share of harm a jade turns, by grade. */
	private static final double[] SHARE = {0.10, 0.15, 0.20, 0.25, 0.30};
	/** What a point of turned harm costs: this share of the wearer's pool for each point of their health. */
	public static final double QI_PER_HEALTH = 0.5;

	private final ArtifactGrade grade;

	public ProtectiveJadeItem(ArtifactGrade grade, Properties properties) {
		super(properties);
		this.grade = grade;
	}

	public ArtifactGrade grade() { return grade; }

	/** The share of harm {@code stack} turns (its grade's, its work's worth with it). */
	public static double share(ItemStack stack) {
		return stack.getItem() instanceof ProtectiveJadeItem jade ? SHARE[jade.grade.ordinal()] * Artifacts.power(stack) : 0;
	}

	/** The share the best jade {@code player} wears turns. */
	public static double worn(Player player) {
		RingContainer rings = RingContainer.of(player);
		double best = 0;
		for (int i = 0; i < rings.getContainerSize(); i++) best = Math.max(best, share(rings.getItem(i)));
		return best;
	}

	/**
	 * The harm {@code target} takes from {@code source} after its jade (the last of the mod's turnings, HeavenSpells#incoming, which lets a
	 * tribulation's strike pass whole before it ever comes here): its share paid in qi while the qi lasts.
	 */
	public static float incoming(LivingEntity target, DamageSource source, float amount) {
		if (amount <= 0 || !(target instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
		double share = worn(player);
		if (share <= 0) return amount;
		PlayerCultivation c = CultivationManager.forPlayer(player);
		double perPoint = c.maxQi() / Math.max(1, player.getMaxHealth()) * QI_PER_HEALTH;
		if (perPoint <= 0) return amount;
		double turned = Math.min(amount * share, c.getQi() / perPoint);
		if (turned < 0.05 || !c.consumeQi(turned * perPoint)) return amount;
		player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.3, 0.4, 0.3, 0.3);
		return (float) (amount - turned);
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
		Tooltips.add(lines, Component.translatable(ModLang.JADE_PERK, Artifacts.percent(share(stack))).withStyle(ChatFormatting.BLUE));
	}
}
