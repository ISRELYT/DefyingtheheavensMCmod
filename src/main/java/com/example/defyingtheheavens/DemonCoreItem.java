package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A demon core (the user's ask, 2026-10-23): a demon's whole cultivation condensed, left when it dies. A wild demon beast leaves its
 * own the more surely the higher it stands (DemonBeast#CORE_CHANCE); a demon cultivator (CultivatorNpc#isDemon) always does; a demon
 * player only when slain by another player ({@link #onPlayerDeath}), and loses nothing else by it. Each core keeps the rank it was
 * formed at (PlayerCultivation#rank: its realm and stage), its element (the beast's, the race's) and what it was taken from. It looks
 * its realm ({@link #look}: five looks, Qi Refining to Heavenly Being and beyond), glowing in its element ({@link #glow}).
 * <p>
 * No use yet: it is to be essential to crafting and artifact forging, later (the user's word).
 */
public class DemonCoreItem extends Item {
	private static final String RANK = "CoreRank", ELEMENT = "CoreElement", FROM = "CoreFrom";
	/** Its looks, by realm: Qi Refining, Foundation Building, Core Formation, Nascent Soul, Heavenly Being and above. */
	public static final int LOOKS = 5;

	public DemonCoreItem(Properties properties) {
		super(properties);
	}

	/** A core formed at {@code rank}, of {@code element} (null: none known), taken from {@code from}. */
	public static ItemStack of(int rank, @Nullable QiElement element, @Nullable Component from) {
		ItemStack stack = new ItemStack(ModItems.DEMON_CORE);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putInt(RANK, Math.max(0, rank));
		if (element != null) tag.putString(ELEMENT, element.name());
		if (from != null) tag.putString(FROM, Component.Serializer.toJson(from));
		return stack;
	}

	public static int rank(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : Math.max(0, tag.getInt(RANK));
	}

	public static Realm realm(ItemStack stack) { return DemonPath.realm(rank(stack)); }

	public static Stage stage(ItemStack stack) { return DemonPath.stage(rank(stack)); }

	public static @Nullable QiElement element(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(ELEMENT)) return null;
		try {
			return QiElement.valueOf(tag.getString(ELEMENT));
		} catch (IllegalArgumentException unknown) {
			return null;
		}
	}

	public static @Nullable Component from(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(FROM)) return null;
		try {
			return Component.Serializer.fromJson(tag.getString(FROM));
		} catch (RuntimeException unreadable) {
			return null;
		}
	}

	/** Which of its {@link #LOOKS} it wears (the client's "defying-the-heavens:realm" model property): its realm's, Heavenly Being's from there up. */
	public static int look(ItemStack stack) {
		return Math.min(LOOKS - 1, realm(stack).ordinal());
	}

	/** The colour its glow takes (the client's tint of its second layer): its element's; a dull red where none is known. */
	public static int glow(ItemStack stack) {
		QiElement element = element(stack);
		if (element == null) return 0xC83A2A;
		return switch (element) {
			case WOOD -> 0x3CE65A;
			case WATER -> 0x3C8CFF;
			case FIRE -> 0xFF5A20;
			case EARTH -> 0xD99A2B;
			case METAL -> 0xFFD050;
			case YIN -> 0x7AF0F0;
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

	/** "Foundation Building Demon Core". */
	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(ModLang.CORE_NAME, realm(stack).getDisplayName());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.CORE_RANK, DemonPath.rankName(rank(stack))).withStyle(ChatFormatting.GRAY));
		QiElement element = element(stack);
		if (element != null) tooltip.add(Component.translatable(ModLang.CORE_ELEMENT, element.getDisplayName()).withStyle(ChatFormatting.GRAY));
		Component from = from(stack);
		if (from != null) tooltip.add(Component.translatable(ModLang.CORE_FROM, from).withStyle(ChatFormatting.DARK_GRAY));
		tooltip.add(Component.translatable(ModLang.CORE_USE).withStyle(ChatFormatting.DARK_AQUA));
	}

	// --- A demon player's ---

	/**
	 * A player has died (CultivationEvents' AFTER_DEATH, before anything else there): a demon (their own record's blood, whatever body
	 * their soul was in) slain by another player leaves their demon core, of their Demon Path's rank, where they fell. Their cultivation
	 * stays as it was: the core is only the victor's spoils (the user's pick). Slain by another player is the killing blow's, or the kill's
	 * credit (struck by them, then finished by a fall or the lava: no core for it before the sweep of 2026-10-23); never a machine's
	 * player (another mod's FakePlayer).
	 */
	static void onPlayerDeath(ServerPlayer player, DamageSource source) {
		Player killer = source.getEntity() instanceof Player blow ? blow : player.getKillCredit() instanceof Player credit ? credit : null;
		if (killer == null || killer instanceof net.fabricmc.fabric.api.entity.FakePlayer || killer == player || killer.getUUID().equals(player.getUUID())) return;
		PlayerCultivation c = CultivationManager.own(player);
		int rank = DemonPath.rank(c);
		if (rank < 0) return;
		// A half-blood's core too (DemonPath#race), and its race's blood as far as its blood is awake (DemonBlood), none beyond what the worlds
		// hold (Realm#atMostTaught: a True Immortal's is the Immortal Realm's).
		player.spawnAtLocation(of(rank, DemonPath.element(DemonPath.race(c)), player.getName()));
		if (c.demon().bloodRealm() >= 0) player.spawnAtLocation(DemonBloodItem.of(DemonPath.race(c), Realm.byIndex(c.demon().bloodRealm()).atMostTaught()));
	}
}
