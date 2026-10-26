package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A demon race's blood in a phial (the user's ask, 2026-10-24: "blood lines should be a physical item like slips"): its race ("BloodRace")
 * and the realm it was of ("BloodRealm"), drunk as a potion is (DemonBlood: what it does, and when it won't take: refused before it's
 * drunk, on both sides). It looks its realm as a demon core does ({@link #look}: six looks, Qi Refining to Heavenly Being and beyond, and a
 * True Immortal's its own), its blood glowing its race's hue ({@link #tint}).
 */
public class DemonBloodItem extends Item {
	private static final String RACE = "BloodRace", REALM = "BloodRealm";
	/** Its looks, by realm: Qi Refining, Foundation Building, Core Formation, Nascent Soul, Heavenly Being to Entering Nirvana, True Immortal. */
	public static final int LOOKS = 6;
	/** Heavenly Being's look, worn up to Entering Nirvana; a True Immortal's blood wears the last. */
	private static final int HIGH_LOOK = 4;

	public DemonBloodItem(Properties properties) {
		super(properties);
	}

	/** {@code race}'s blood of {@code realm}. */
	public static ItemStack of(Bloodline race, Realm realm) {
		ItemStack stack = new ItemStack(ModItems.DEMON_BLOOD);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putString(RACE, race.getId());
		tag.putInt(REALM, realm.ordinal());
		return stack;
	}

	/** Its race, or null: none known (a bare stack). */
	public static @Nullable Bloodline race(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(RACE)) return null;
		Bloodline race = Bloodline.byId(tag.getString(RACE));
		return race != null && race.isBeast() ? race : null;
	}

	public static Realm realm(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return Realm.byIndex(tag == null ? 0 : Math.max(0, Math.min(Realm.values().length - 1, tag.getInt(REALM))));
	}

	/**
	 * Which of its {@link #LOOKS} it wears (the client's "defying-the-heavens:realm" model property): its realm's, Heavenly Being's from there
	 * up to Entering Nirvana, a True Immortal's its own (the golden phial, tools/beast-art's Bloods), and every immortal realm's past it too.
	 */
	public static int look(ItemStack stack) {
		Realm realm = realm(stack);
		return realm.isImmortal() ? LOOKS - 1 : Math.min(HIGH_LOOK, realm.ordinal());
	}

	/** The hue its blood glows (the client's tint of its second layer): its race's; a dull red where none is known. */
	public static int tint(ItemStack stack) {
		Bloodline race = race(stack);
		if (race == null) return 0x8A1A1A;
		return switch (race) {
			case TIGER -> 0xE08A2A; // the White Tiger's metal: amber gold
			case LIZARD -> 0x4FB848; // the earth's green-gold venom
			case FOX -> 0xF0402A; // fox fire
			case DRAGON -> 0x4A86F0; // the Azure Dragon's
			case HUMAN -> 0x8A1A1A;
		};
	}

	@Override
	public Rarity getRarity(ItemStack stack) {
		return switch (look(stack)) {
			case 0 -> Rarity.COMMON;
			case 1 -> Rarity.UNCOMMON;
			case 2 -> Rarity.RARE;
			default -> Rarity.EPIC;
		};
	}

	/** "Core Formation Tiger Blood". */
	@Override
	public Component getName(ItemStack stack) {
		Bloodline race = race(stack);
		return race == null ? Component.translatable(getDescriptionId()) : Component.translatable(ModLang.BLOOD_NAME, realm(stack).getDisplayName(), race.getDisplayName());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		Bloodline race = race(stack);
		if (race == null) {
			tooltip.add(Component.translatable(ModLang.BLOOD_UNKNOWN).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		tooltip.add(Component.translatable(ModLang.BLOOD_USE, race.getDisplayName(), realm(stack).getDisplayName()).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable(ModLang.BLOOD_TURN_TIP, race.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		PlayerCultivation c = CultivationManager.forPlayer(player);
		// The body's record on the client (its own unless held: Possession tells it), judged as the server will judge it.
		DemonBlood.Outcome outcome = DemonBlood.judge(c, race(stack), realm(stack), player.isShiftKeyDown(), c.netherworld().ghost(), false);
		if (outcome.verdict() != DemonBlood.Verdict.DRINK) {
			if (!level.isClientSide) {
				player.displayClientMessage(outcome.why().copy().withStyle(outcome.verdict() == DemonBlood.Verdict.WARN ? ChatFormatting.RED : ChatFormatting.GRAY), true);
			}
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 32;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) return stack;
		Bloodline race = race(stack);
		if (!DemonBlood.drink(player, race, realm(stack))) return stack; // judged again: things may have changed while it was drunk
		player.awardStat(Stats.ITEM_USED.get(this));
		if (!player.getAbilities().instabuild) stack.shrink(1);
		return stack;
	}
}
