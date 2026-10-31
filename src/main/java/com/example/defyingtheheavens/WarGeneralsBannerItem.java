package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The War General's Banner, half of the Ancient Battlefield's Great Inheritance (BattlefieldWorks, 2026-10-31): the old general's own.
 * <ul>
 *   <li><b>Held</b> (either hand), its side fights the harder: its bearer and everyone fighting with them within {@link #REACH} blocks
 *   ({@link #ally}: the old army it calls up, their sect's fellows, players of their sect or team) strike harder and are harder to harm
 *   (Strength and Resistance, renewed each second while it is held).</li>
 *   <li><b>Raised</b> (use), it calls up {@link #CALLED} soldiers of the old army (ArmySpirit) to fight beside its bearer for
 *   {@link ArmySpirit#LIFE} ticks, for a fifth of their whole qi; then it rests {@link #REST} ticks (kept on the banner, so a rest outlasts
 *   leaving the world).</li>
 * </ul>
 */
public class WarGeneralsBannerItem extends Item {
	/** How far its side is strengthened (blocks). */
	public static final int REACH = 16;
	/** How many it calls up, and how long it rests after (ticks: five minutes). */
	public static final int CALLED = 4, REST = 6000;
	/** What raising it costs: this share of its bearer's whole qi. */
	public static final double COST_SHARE = 0.2;
	private static final String RESTS_UNTIL = "BannerRestsUntil";

	public WarGeneralsBannerItem(Properties properties) {
		super(properties);
	}

	/** Held: its side strengthened, once a second. */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer p) || p.tickCount % 20 != 0 || p.isSpectator()) return;
		if (!selected && p.getOffhandItem() != stack) return;
		for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(REACH), e -> e.isAlive() && ally(p, e))) {
			e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50, 0, true, true, true));
			e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0, true, true, true));
		}
	}

	/** Whether {@code e} fights on {@code bearer}'s side: themself, the old army they called, their sect's fellows, their sect or team's players. */
	public static boolean ally(ServerPlayer bearer, LivingEntity e) {
		if (e == bearer) return true;
		if (e instanceof ArmySpirit spirit) return bearer.getUUID().equals(spirit.owner());
		if (e instanceof CultivatorNpc npc) return SectMembership.areFellows(npc, bearer);
		if (!(e instanceof ServerPlayer other) || other.isSpectator()) return false;
		if (bearer.isAlliedTo(other)) return true;
		Sect sect = SectMembership.sectOf(bearer.server, bearer.getUUID());
		return sect != null && SectMembership.isDisciple(bearer.server, sect.id, other.getUUID());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
		long now = sp.server.overworld().getGameTime();
		long until = stack.getTag() == null ? 0 : stack.getTag().getLong(RESTS_UNTIL);
		if (now < until) {
			int left = (int) (until - now);
			sp.getCooldowns().addCooldown(this, left);
			sp.displayClientMessage(Component.translatable(ModLang.MSG_BANNER_RESTING, SecretRealms.clock(left)).withStyle(ChatFormatting.GRAY), true);
			return InteractionResultHolder.fail(stack);
		}
		PlayerCultivation c = CultivationManager.get(sp);
		double cost = c.maxQi() * COST_SHARE;
		if (!sp.getAbilities().instabuild && c.getQi() < cost) {
			sp.displayClientMessage(Component.translatable(ModLang.MSG_BANNER_NO_QI).withStyle(ChatFormatting.RED), true);
			return InteractionResultHolder.fail(stack);
		}
		if (!sp.getAbilities().instabuild) {
			c.setQi(c.getQi() - cost);
			CultivationManager.sync(sp);
		}
		for (int i = 0; i < CALLED; i++) {
			float bearing = sp.getYRot() + 45 + i * 90;
			double x = sp.getX() - Mth.sin(bearing * Mth.DEG_TO_RAD) * 2.5, z = sp.getZ() + Mth.cos(bearing * Mth.DEG_TO_RAD) * 2.5;
			ArmySpirit spirit = ModEntities.ARMY_SPIRIT.create(server);
			if (spirit == null) continue;
			spirit.moveTo(x, standing(server, x, sp.getY(), z), z, sp.getYRot(), 0);
			spirit.serve(sp);
			server.addFreshEntity(spirit);
			server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, spirit.getY() + 1, z, 16, 0.3, 0.8, 0.3, 0.03);
		}
		server.playSound(null, sp.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 2.0f, 1.0f);
		server.playSound(null, sp.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 1.5f, 0.6f);
		stack.getOrCreateTag().putLong(RESTS_UNTIL, now + REST);
		sp.getCooldowns().addCooldown(this, REST);
		sp.displayClientMessage(Component.translatable(ModLang.MSG_BANNER_CALLED, ArmySpirit.LIFE / 20).withStyle(ChatFormatting.GOLD), true);
		return InteractionResultHolder.consume(stack);
	}

	/** Where one called up beside {@code y} stands at {@code x, z}: on the ground within a few blocks of it, else at {@code y}. */
	private static double standing(ServerLevel level, double x, double y, double z) {
		BlockPos at = BlockPos.containing(x, y, z);
		for (int dy = 2; dy >= -4; dy--) {
			BlockPos feet = at.above(dy);
			if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
					&& !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) return feet.getY();
		}
		return y;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.TOOLTIP_WAR_BANNER, REACH, CALLED, ArmySpirit.LIFE / 20, REST / 1200).withStyle(ChatFormatting.GRAY));
	}
}
