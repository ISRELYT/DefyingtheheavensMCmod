package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A Method Manual (the methods overhaul's phase 2): one of the methods learned apart from a path's own teaching (CultivationMethod#fromManual),
 * written to a realm. Read (right-click; the dead may read one too: Incorporeal#mayUse), its volumes from its first realm up to that one
 * are learned (CultivationMethods#learn); a demon method's whole, its blood being its volume. Not used up, so it may be passed on. Sect
 * libraries and masters' halls keep them, each the methods of its own kind ({@link #kept}, {@link #kinds}); the lower realms' to Core
 * Formation, the Upper Realm's from Core Formation on; the ways of Entering Nirvana and True Immortal only the ancients' vaults
 * ({@link #ancient}).
 */
public class MethodManualItem extends Item {
	private static final String METHOD = "Method", REALM = "Realm";
	/** How often a library's chest keeps one, and a master's hall's. */
	public static final float LIBRARY_CHANCE = 0.2f, HALL_CHANCE = 0.35f;

	public MethodManualItem(Properties properties) {
		super(properties);
	}

	/** A manual of {@code method}, written to {@code realm} (within its own realms). */
	public static ItemStack of(CultivationMethod method, Realm realm) {
		ItemStack stack = new ItemStack(ModItems.METHOD_MANUAL);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putString(METHOD, method.getId());
		tag.putInt(REALM, Math.max(method.first().ordinal(), Math.min(method.last().ordinal(), realm.ordinal())));
		return stack;
	}

	public static @Nullable CultivationMethod method(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		CultivationMethod method = tag == null ? null : CultivationMethod.byId(tag.getString(METHOD));
		return method != null && method.fromManual() ? method : null;
	}

	public static Realm realm(ItemStack stack) {
		CultivationMethod method = method(stack);
		CompoundTag tag = stack.getTag();
		if (method == null || tag == null) return Realm.QI_REFINING;
		return Realm.byIndex(Math.max(method.first().ordinal(), Math.min(method.last().ordinal(), tag.getInt(REALM))));
	}

	@Override
	public Component getName(ItemStack stack) {
		CultivationMethod method = method(stack);
		return method == null ? super.getName(stack) : Component.translatable(ModLang.ITEM_METHOD_MANUAL_OF, method.getDisplayName());
	}

	@Override
	public Rarity getRarity(ItemStack stack) {
		int realm = realm(stack).ordinal();
		return realm >= Realm.NASCENT_SOUL.ordinal() ? Rarity.EPIC : realm >= Realm.FOUNDATION_BUILDING.ordinal() ? Rarity.RARE : Rarity.UNCOMMON;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		CultivationMethod method = method(stack);
		if (method == null) return;
		tooltip.add(Component.translatable(ModLang.pathName(method.path())).withStyle(ChatFormatting.GOLD));
		tooltip.add(method.path() == TwoPaths.Path.DEMON ? Component.translatable(ModLang.TOOLTIP_MANUAL_BLOOD).withStyle(ChatFormatting.GRAY)
				: method.first() == method.last() ? Component.translatable(ModLang.TOOLTIP_MANUAL_VOLUME, method.first().getDisplayName()).withStyle(ChatFormatting.GRAY)
				: Component.translatable(ModLang.TOOLTIP_MANUAL_VOLUMES, method.first().getDisplayName(), realm(stack).getDisplayName()).withStyle(ChatFormatting.GRAY));
		tooltip.add(method.getDescription().copy().withStyle(ChatFormatting.DARK_GRAY));
		tooltip.add(Component.translatable(ModLang.TOOLTIP_MANUAL_READ).withStyle(ChatFormatting.DARK_AQUA));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		CultivationMethod method = method(stack);
		if (method == null) return InteractionResultHolder.pass(stack);
		if (player instanceof ServerPlayer sp) CultivationMethods.learn(sp, method, realm(stack));
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	/**
	 * Now and then ({@link #LIBRARY_CHANCE}, {@link #HALL_CHANCE} for a master's hall) a manual for a chest of a sect's of {@code faction}
	 * (a demon clan's den, {@code race} a beast's), of the {@code upper} realm's or the lower realms'; or nothing. Only methods with volumes
	 * of the realms that world's manuals are written to (the lower realms' to Core Formation, the Upper Realm's from it to Sacred Vessel):
	 * the ways of Entering Nirvana and True Immortal no sect keeps ({@link #ancient}).
	 */
	public static ItemStack kept(Alignment.Faction faction, Bloodline race, boolean upper, boolean hall, RandomSource random) {
		if (random.nextFloat() >= (hall ? HALL_CHANCE : LIBRARY_CHANCE)) return ItemStack.EMPTY;
		int top = (upper ? Realm.SACRED_VESSEL : Realm.CORE_FORMATION).ordinal();
		List<CultivationMethod> kept = kinds(faction, race).stream().filter(m -> m.first().ordinal() <= top).toList();
		return volume(kept.get(random.nextInt(kept.size())), upper, random);
	}

	/**
	 * A body sect's chest's manuals (Sect#body; the user's "Go, with body sects", 2026-11-01): the Body Path's ways its faction keeps ({@link #kinds}:
	 * the bath, the weights and the spars a righteous one's, all four a neutral one's, beasts' blood and the weights a devilish one's), its
	 * library's one or two always, in place of the heaven path's slips (SectPiece#BODY_LIBRARY_LOOT), its master's hall's one now and then
	 * ({@link #HALL_CHANCE}); written to a realm as {@link #kept}'s are.
	 */
	public static List<ItemStack> keptBody(Alignment.Faction faction, boolean upper, boolean hall, RandomSource random) {
		List<ItemStack> manuals = new ArrayList<>();
		if (hall && random.nextFloat() >= HALL_CHANCE) return manuals;
		List<CultivationMethod> ways = kinds(faction, Bloodline.HUMAN).stream().filter(m -> m.path() == TwoPaths.Path.BODY).toList();
		if (ways.isEmpty()) return manuals;
		int count = hall ? 1 : 1 + random.nextInt(2);
		for (int i = 0; i < count; i++) manuals.add(volume(ways.get(random.nextInt(ways.size())), upper, random));
		return manuals;
	}

	/** A manual of {@code method} for a sect's chest of the {@code upper} realm's or the lower realms': written to one of the realms that world's are. */
	private static ItemStack volume(CultivationMethod method, boolean upper, RandomSource random) {
		int low = upper ? Math.max(method.first().ordinal(), Realm.CORE_FORMATION.ordinal()) : method.first().ordinal();
		int high = upper ? Math.min(method.last().ordinal(), Realm.SACRED_VESSEL.ordinal()) : Math.min(method.last().ordinal(), Realm.CORE_FORMATION.ordinal());
		if (high < low) high = low;
		int realm = low + random.nextInt(high - low + 1);
		if (realm == Realm.FOUR_AXIS.ordinal()) realm--; // no method has a volume of Four Axis
		return of(method, Realm.byIndex(realm));
	}

	/**
	 * The methods a sect of {@code faction} keeps (a demon clan's den, {@code race} a beast's): a righteous sect the heaven path's own ways
	 * (the rites, Turtle Breath, Five Elements Cycling, Incense and Faith, Spirit Beast Bond, the heart demon's tempering, the Dao fields', the
	 * vessels', the herbs', the clone's seclusion), and the Judgement Hall's errand; a neutral one the same but Incense and Faith and the
	 * vessels', Rift Walking, the ancestral tablets' incense and the Yellow Springs; a devilish one the ghost path's darker ways (Corpse Yin
	 * Refining, Haunting, the Soul River Feast, the Court of the Dead, the tablets' incense, the springs, Rift Haunting, a life borrowed) and
	 * Turtle Breath; a clan its blood's (kin blood, cores,
	 * territory, beast worship, the ancestors' bones). The Body Path's ways (2026-11-01): the bath, the weights and the spars a righteous sect's;
	 * all four a neutral one's; beasts' blood and the weights a devilish one's. (The ways of Entering Nirvana and True Immortal are listed with
	 * their kind's, but no sect's chest holds one: {@link #kept}.)
	 */
	public static List<CultivationMethod> kinds(Alignment.Faction faction, Bloodline race) {
		if (race != null && race.isBeast()) return List.of(CultivationMethod.KIN_BLOOD, CultivationMethod.CORE_DEVOURING,
				CultivationMethod.TERRITORIAL_DOMINION, CultivationMethod.BEAST_DEITY, CultivationMethod.ANCESTRAL_COMMUNION,
				CultivationMethod.BLOODLINE_NIRVANA, CultivationMethod.PRIMORDIAL_RETURN);
		return switch (faction) {
			case DEVILISH -> List.of(CultivationMethod.CORPSE_YIN, CultivationMethod.TURTLE_BREATH, CultivationMethod.HAUNTING,
					CultivationMethod.SOUL_RIVER_FEAST, CultivationMethod.COURT_OF_THE_DEAD, CultivationMethod.ANCESTRAL_INCENSE,
					CultivationMethod.YELLOW_SPRINGS_IMMERSION, CultivationMethod.RIFT_HAUNTING, CultivationMethod.SOUL_FURNACE, CultivationMethod.TEN_THOUSAND_GHOSTS,
					CultivationMethod.BORROWED_LIFE, CultivationMethod.BEAST_BLOOD_BATHING, CultivationMethod.WEIGHTED_TRAINING);
			case NEUTRAL -> List.of(CultivationMethod.HEAVEN_RITES, CultivationMethod.TURTLE_BREATH, CultivationMethod.FIVE_ELEMENTS,
					CultivationMethod.SPIRIT_BEAST_BOND, CultivationMethod.ANCESTRAL_INCENSE, CultivationMethod.HEART_DEMON_TEMPERING,
					CultivationMethod.DAO_FIELD_ATTUNEMENT, CultivationMethod.RIFT_WALKING, CultivationMethod.YELLOW_SPRINGS_IMMERSION,
					CultivationMethod.JUDGEMENT_HALL_SERVICE, CultivationMethod.SPIRIT_HERB_REFINING, CultivationMethod.CLONE_SECLUSION,
					CultivationMethod.MEDICINAL_BATH, CultivationMethod.BEAST_BLOOD_BATHING, CultivationMethod.WEIGHTED_TRAINING,
					CultivationMethod.HUNDRED_BATTLES);
			case RIGHTEOUS -> List.of(CultivationMethod.HEAVEN_RITES, CultivationMethod.TURTLE_BREATH, CultivationMethod.FIVE_ELEMENTS,
					CultivationMethod.INCENSE_FAITH, CultivationMethod.SPIRIT_BEAST_BOND, CultivationMethod.HEART_DEMON_TEMPERING,
					CultivationMethod.DAO_FIELD_ATTUNEMENT, CultivationMethod.VESSEL_NURTURING, CultivationMethod.JUDGEMENT_HALL_SERVICE,
					CultivationMethod.NIRVANA_REBIRTH, CultivationMethod.IMMORTAL_ESSENCE_REFINING, CultivationMethod.SPIRIT_HERB_REFINING,
					CultivationMethod.CLONE_SECLUSION, CultivationMethod.MEDICINAL_BATH, CultivationMethod.WEIGHTED_TRAINING,
					CultivationMethod.HUNDRED_BATTLES);
		};
	}

	/**
	 * A manual the ancients left in a secret realm's vault (the methods overhaul's phase 4): one of the ways whose volumes begin at
	 * {@code realm}, written to it; the Fallen Holy Land's vaults keep Entering Nirvana's, the Fallen Immortal's Tomb's halls True
	 * Immortal's, which no sect keeps. Nothing if none begins there.
	 */
	public static ItemStack ancient(Realm realm, RandomSource random) {
		List<CultivationMethod> ways = java.util.Arrays.stream(CultivationMethod.values()).filter(m -> m.fromManual() && m.first() == realm).toList();
		return ways.isEmpty() ? ItemStack.EMPTY : of(ways.get(random.nextInt(ways.size())), realm);
	}
}
