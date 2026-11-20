package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
 * libraries and masters' halls keep them, each the methods of its own kind ({@link #kept}, {@link #kinds}); the lower realms' to Heavenly
 * Being, the Upper Realm's from Core Formation to Sacred Vessel, the Great Void's from Heavenly Being to Entering Nirvana, the Immortal
 * Realm's from Entering Nirvana to True Immortal ({@link World}); the ways of Entering Nirvana the Great Void's and the Immortal Realm's
 * sects and the ancients' vaults, those of True Immortal the Immortal Realm's sects and the ancients' ({@link #ancient}). The Celestial
 * Court sells True Immortal's volume of a way that runs on to it, one a day (CelestialCourt#wares).
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
		tooltip.add(volumes(method, realm(stack)).copy().withStyle(ChatFormatting.GRAY));
		tooltip.add(method.getDescription().copy().withStyle(ChatFormatting.DARK_GRAY));
		tooltip.add(Component.translatable(ModLang.TOOLTIP_MANUAL_READ).withStyle(ChatFormatting.DARK_AQUA));
	}

	/**
	 * What a manual (or a stele: SteleTeachings) of {@code method} written to {@code realm} holds: a Demon Path method whole (the blood is its volume);
	 * one of a single realm, that realm's volume; else its volumes from its first realm to {@code realm}'s (CultivationMethod#volumeFor).
	 */
	public static Component volumes(CultivationMethod method, Realm realm) {
		if (method.path() == TwoPaths.Path.DEMON) return Component.translatable(ModLang.TOOLTIP_MANUAL_BLOOD);
		if (method.first() == method.last()) return Component.translatable(ModLang.TOOLTIP_MANUAL_VOLUME, method.first().getDisplayName());
		return Component.translatable(ModLang.TOOLTIP_MANUAL_VOLUMES, method.first().getDisplayName(), method.volumeFor(realm).getDisplayName());
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
	 * The worlds whose sects keep Method Manuals (SectPiece#chest), each writing them to its own realms: the lower realms' Qi Refining to
	 * Heavenly Being, their last (Core Formation until the user's word of 2026-11-16, so no lower library held a Nascent Soul or Heavenly
	 * Being volume); the Upper Realm's Core Formation to Sacred Vessel; the Great Void's, its Holy Lands' and Demon Palaces', Heavenly Being
	 * to Entering Nirvana (the user, the same evening: "make it so up to entering nirvana method manuals spawn in the great void"), the realms
	 * of its libraries' scriptures; the Immortal Realm's island sects (ImmortalSects) Entering Nirvana to True Immortal (the user's word of
	 * 2026-11-17, "Also the Court sales" with it: they kept the Upper Realm's before, so the ways that run on to True Immortal had that volume
	 * nowhere). True Immortal's volumes are the Immortal Realm's alone (Realm#taughtInTheWorld: the worlds below none).
	 */
	public enum World {
		LOWER(Realm.QI_REFINING, Realm.HEAVENLY_BEING),
		UPPER(Realm.CORE_FORMATION, Realm.SACRED_VESSEL),
		GREAT_VOID(Realm.HEAVENLY_BEING, Realm.ENTERING_NIRVANA),
		IMMORTAL(Realm.ENTERING_NIRVANA, Realm.TRUE_IMMORTAL);

		/** The shallowest and the deepest realm its chests write a manual to. */
		public final Realm low, deepest;

		World(Realm low, Realm deepest) {
			this.low = low;
			this.deepest = deepest;
		}

		/** The world of a sect of the {@code upper} realm's or the lower realms' standing in {@code dimension}. */
		public static World of(boolean upper, ResourceKey<Level> dimension) {
			return !upper ? LOWER : ModDimensions.isGreatVoid(dimension) ? GREAT_VOID : ModDimensions.isImmortalRealm(dimension) ? IMMORTAL : UPPER;
		}
	}

	/**
	 * Now and then ({@link #LIBRARY_CHANCE}, {@link #HALL_CHANCE} for a master's hall) a manual for a chest of a sect's of {@code faction}
	 * (a demon clan's den, {@code race} a beast's) in {@code world}; or nothing. Only methods with volumes of the realms that world's manuals
	 * are written to ({@link World}), each written within its own realms ({@link #volume}): the ways of Entering Nirvana the Great Void's and
	 * the Immortal Realm's sects keep, those of True Immortal the Immortal Realm's alone ({@link #ancient}); and the ways that end before a
	 * world's realms begin it keeps not (the user, 2026-11-16: "not all methods go all the way to true immortal or whatever, some are only for
	 * certain realms"), which leaves out of the Great Void's, from Heavenly Being, those ending at Nascent Soul or before (Heart Demon
	 * Tempering, Haunting, the weights, beasts' blood, Territorial Dominion), and out of the Immortal Realm's, from Entering Nirvana, all but
	 * the ways that reach it; the lower realms' and the Upper Realm's keep all of their kind's that reach them, as before. Nothing if a kind
	 * keeps none there.
	 */
	public static ItemStack kept(Alignment.Faction faction, Bloodline race, World world, boolean hall, RandomSource random) {
		if (random.nextFloat() >= (hall ? HALL_CHANCE : LIBRARY_CHANCE)) return ItemStack.EMPTY;
		int low = world.low.ordinal(), top = world.deepest.ordinal();
		List<CultivationMethod> kept = kinds(faction, race).stream().filter(m -> m.first().ordinal() <= top && m.last().ordinal() >= low).toList();
		if (kept.isEmpty()) return ItemStack.EMPTY;
		return volume(kept.get(random.nextInt(kept.size())), world, random);
	}

	/**
	 * A body sect's chest's manuals (Sect#body; the user's "Go, with body sects", 2026-11-01): the Body Path's ways its faction keeps ({@link #kinds}:
	 * the bath, the weights and the spars a righteous one's, all four a neutral one's, beasts' blood and the weights a devilish one's), its
	 * library's one or two always, in place of the scriptures' Art Manuals (SectPiece#BODY_LIBRARY_LOOT), its master's hall's one now and then
	 * ({@link #HALL_CHANCE}); written to a realm as {@link #kept}'s are.
	 */
	public static List<ItemStack> keptBody(Alignment.Faction faction, World world, boolean hall, RandomSource random) {
		List<ItemStack> manuals = new ArrayList<>();
		if (hall && random.nextFloat() >= HALL_CHANCE) return manuals;
		List<CultivationMethod> ways = kinds(faction, Bloodline.HUMAN).stream().filter(m -> m.path() == TwoPaths.Path.BODY).toList();
		if (ways.isEmpty()) return manuals;
		int count = hall ? 1 : 1 + random.nextInt(2);
		for (int i = 0; i < count; i++) manuals.add(volume(ways.get(random.nextInt(ways.size())), world, random));
		return manuals;
	}

	/**
	 * A Qi Circulation manual for a sect library in {@code world} (SectPiece#chest: every one keeps one since 2026-11-16, the arts carrying
	 * none of its volumes): written to one of the realms that world's are ({@link World}: the lower realms' Qi Refining to Heavenly Being,
	 * the Upper Realm's Core Formation to Sacred Vessel, the Great Void's Heavenly Being to Entering Nirvana, the Immortal Realm's Entering
	 * Nirvana to True Immortal), the lower ones the likelier.
	 */
	public static ItemStack circulation(World world, RandomSource random) {
		int low = world.low.ordinal(), high = world.deepest.ordinal();
		int realm = low + Math.min(random.nextInt(high - low + 1), random.nextInt(high - low + 1)); // the lower of two
		if (realm == Realm.FOUR_AXIS.ordinal()) realm--; // no method has a volume of Four Axis
		return of(CultivationMethod.QI_CIRCULATION, Realm.byIndex(realm));
	}

	/**
	 * A manual of {@code method} for a sect's chest in {@code world}: written to one of the realms that world's are, or, a method whose volumes
	 * end before them, to its last.
	 */
	private static ItemStack volume(CultivationMethod method, World world, RandomSource random) {
		int low = Math.max(method.first().ordinal(), world.low.ordinal());
		int high = Math.min(method.last().ordinal(), world.deepest.ordinal());
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
	 * their kind's: the Great Void's and the Immortal Realm's sects keep the first, the Immortal Realm's alone the second: {@link #kept}.)
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
	 * Immortal's (both kept besides by the Immortal Realm's sects, the first by the Great Void's too: {@link #kept}). Nothing if none begins
	 * there.
	 */
	public static ItemStack ancient(Realm realm, RandomSource random) {
		List<CultivationMethod> ways = java.util.Arrays.stream(CultivationMethod.values()).filter(m -> m.fromManual() && m.first() == realm).toList();
		return ways.isEmpty() ? ItemStack.EMPTY : of(ways.get(random.nextInt(ways.size())), realm);
	}
}
