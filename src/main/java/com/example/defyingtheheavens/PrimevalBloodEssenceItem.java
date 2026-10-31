package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Primeval Blood Essence, the Primeval Beast Graveyard's Great Inheritance (BeastGraveyardWorks, 2026-10-31): the blood of the primeval
 * dragon whose bones lie there, from which every beast's blood came. Drunk (DemonBlood does the rest):
 * <ul>
 *   <li>by a demon of any race: its blood woken a realm further than it was ({@link #AWAKEN_TO} at most);</li>
 *   <li>by a half-blood: its own race's blood the purer (as blood a realm above its other paths', DemonBlood#gain), turning it wholly that
 *   race's if it comes to the whole (warned first, as any blood);</li>
 *   <li>by a human: a dragon's blood woken in it: a Dragon half-blood, its purity begun as by blood a realm above its paths'. Warned first:
 *   drunk only sneaking, for there is no way back.</li>
 * </ul>
 * Never by a mortal, one of the dead, or a soul in another's body.
 */
public class PrimevalBloodEssenceItem extends Item {
	/** A demon's blood is woken no further than this realm's by it. */
	static final Realm AWAKEN_TO = Realm.ENTERING_NIRVANA;

	public PrimevalBloodEssenceItem(Properties properties) {
		super(properties);
	}

	/** Why {@code c} may not drink it now (null: it may), {@code sneaking} as one who means it. The same on both sides. */
	static @Nullable Component why(PlayerCultivation c, boolean sneaking, boolean elsewhere) {
		if (elsewhere) return Component.translatable(ModLang.MSG_BLOOD_ELSEWHERE);
		if (c.netherworld().ghost()) return Component.translatable(ModLang.MSG_BLOOD_DEAD);
		if (c.isMortal()) return Component.translatable(ModLang.MSG_BLOOD_MORTAL);
		if (DemonPath.isDemon(c)) return c.demon().bloodRealm() >= AWAKEN_TO.ordinal() ? Component.translatable(ModLang.MSG_ESSENCE_NOTHING) : null;
		if (c.demon().halfBlood() == null && !sneaking) return Component.translatable(ModLang.MSG_ESSENCE_WARN);
		return null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Component why = why(CultivationManager.forPlayer(player), player.isShiftKeyDown(), false);
		if (why != null) {
			if (!level.isClientSide) player.displayClientMessage(why.copy().withStyle(ChatFormatting.RED), true);
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 40;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) return stack;
		if (!drink(player)) return stack;
		player.awardStat(Stats.ITEM_USED.get(this));
		if (!player.getAbilities().instabuild) stack.shrink(1);
		return stack;
	}

	/** {@code player} drinks it, judged again (things may have changed while it was drunk). @return whether it took */
	static boolean drink(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.own(player);
		Component why = why(c, player.isShiftKeyDown(), CultivationManager.get(player) != c);
		if (why != null) {
			player.displayClientMessage(why.copy().withStyle(ChatFormatting.RED), true);
			return false;
		}
		if (DemonPath.isDemon(c)) {
			Realm woken = Realm.byIndex(c.demon().bloodRealm() + 1);
			c.demon().setBloodRealm(woken.ordinal());
			player.sendSystemMessage(Component.translatable(ModLang.MSG_ESSENCE_AWAKENS, woken.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.7f, 1.3f);
			CultivationManager.markDirty(player.server);
			CultivationManager.refresh(player);
			return true;
		}
		// A human's or a half-blood's: blood a realm above its other paths', of its own race, or a dragon's if it has none yet.
		Bloodline race = c.demon().halfBlood() != null ? c.demon().halfBlood() : Bloodline.DRAGON;
		Realm realm = Realm.byIndex(Math.min(AWAKEN_TO.ordinal(), DemonBlood.otherPaths(c) + 1));
		return DemonBlood.drink(player, race, realm);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.TOOLTIP_PRIMEVAL_ESSENCE).withStyle(ChatFormatting.GRAY));
		Tooltips.add(lines, Component.translatable(ModLang.TOOLTIP_PRIMEVAL_ESSENCE_WARN).withStyle(ChatFormatting.DARK_RED));
	}
}
