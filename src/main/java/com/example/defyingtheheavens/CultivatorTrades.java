package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a cultivator trades (TalkSystem's Trade, on the vanilla trading screen; see CultivatorNpc's Merchant side). Prices
 * are in spirit stones (SpiritStoneItem), worked out in Low stones and paid in the grades that make them up.
 * <ul>
 *   <li><b>Its goods</b> come from its station and path: Outer Disciples (and commoners) sell seeds and young herbs, Inner
 *   Disciples rarer seeds and Qi Gathering Pills, Elders pills and old herbs, the Sect Master the best of both; rogues carry
 *   odd things from far away. The stock is the same all day (it changes with the day), each line selling only so many. One of a
 *   sect's (or a clan's) people sells a guest pass of it first (SectPassItem), one a day, to one it isn't resentful toward, made out to
 *   them alone.</li>
 *   <li><b>Regard</b> opens better offers (one more at Polite, Courteous, Warm and Trusted) and lowers prices, from
 *   x{@code 1.3} when Resentful to x{@code 0.75} when Sworn. Those not yet open are shown all the same, crossed out, the
 *   regard they want written on them ({@link #locked}).</li>
 *   <li><b>It buys</b> what you carry that a cultivator has use for (herbs, pills, Cultivation Fruit), up to
 *   {@link #MAX_BUYS} kinds, paying {@link #buyShare} of what it is worth.</li>
 * </ul>
 */
public final class CultivatorTrades {
	/** How many different things of yours it offers to buy at once. */
	public static final int MAX_BUYS = 5;
	/** Its markup on what it sells (of an item's worth, TalkSystem#worth). */
	private static final double MARKUP = 1.5;

	/** One thing it sells: the item, its price (Low stones) before regard, how many it sells a day, and the regard it wants. */
	record Goods(ItemStack item, int price, int maxUses, int regard) {
		Goods withRegard(int wanted) {
			return new Goods(item, price, maxUses, wanted);
		}
	}

	/** Price multiplier for what it sells, by its regard for you. */
	public static double priceFactor(int regard) {
		if (regard < 0) return 1.3;
		return 1.0 - 0.05 * Regard.pips(regard);
	}

	/** Share of an item's worth it pays when it buys from you, by its regard: 0.5 when Resentful, up to 0.8 when Sworn. */
	public static double buyShare(int regard) {
		if (regard < 0) return 0.5;
		return 0.55 + 0.05 * Regard.pips(regard);
	}

	/**
	 * Today's offers for {@code player}: its goods (with {@code uses} already made of each today, keyed in {@code keys}), then what
	 * it would buy from the player's inventory.
	 */
	public static MerchantOffers offersFor(CultivatorNpc npc, ServerPlayer player, Map<String, Integer> uses, Map<MerchantOffer, String> keys) {
		// The Celestial Court's Quartermaster sells for merit, by the Court's ranks (CelestialCourt#offers).
		if (npc.getTitle() == NpcTitle.COURT_QUARTERMASTER) return CelestialCourt.offers(npc, player, uses, keys);
		MerchantOffers offers = new MerchantOffers();
		keys.clear();
		int regard = npc.regard(player);
		long day = Regard.day(npc.level().getGameTime());
		RandomSource random = RandomSource.create(npc.getUUID().getMostSignificantBits() ^ (day * 0x9E3779B97F4A7C15L));
		List<Goods> goods = goodsFor(npc, random);
		// x0.8 more for one obsessed with Fortune (Obsession), on top of what regard does.
		double factor = priceFactor(regard) * Obsession.priceFactor(CultivationManager.get(player));
		for (int i = 0; i < goods.size(); i++) {
			Goods g = goods.get(i);
			String key = "sell:" + i;
			int price = Math.max(1, (int) Math.ceil(g.price() * factor));
			ItemStack[] costs = costsFor(price);
			if (regard < g.regard()) {
				// Not yet opened to you (the user's ask, 2026-10-19): shown, crossed out (sold out to the screen, so nothing can be
				// taken), the regard it wants and yours written on it.
				MerchantOffer locked = new MerchantOffer(costs[0], costs[1], locked(g.item(), g.regard(), regard), g.maxUses(), g.maxUses(), 0, 0);
				offers.add(locked);
				keys.put(locked, "locked:" + i);
				continue;
			}
			// A guest pass made out to the one it is offered to (SectPassItem#boundTo: it lets no one else through).
			ItemStack sold = g.item().is(ModItems.SECT_PASS) ? SectPassItem.boundTo(g.item(), player) : g.item().copy();
			MerchantOffer offer = new MerchantOffer(costs[0], costs[1], sold, uses.getOrDefault(key, 0), g.maxUses(), 0, 0);
			offers.add(offer);
			keys.put(offer, key);
		}
		// What it would buy from you: each kind of useful thing you carry, once each.
		Set<String> seen = new HashSet<>();
		double share = buyShare(regard);
		for (ItemStack stack : player.getInventory().items) {
			if (offers.size() >= goods.size() + MAX_BUYS) break;
			if (stack.isEmpty() || stack.getItem() instanceof SpiritStoneItem) continue;
			ItemStack one = stack.copyWithCount(1);
			double worth = TalkSystem.worth(one);
			if (worth <= 0) continue;
			String key = "buy:" + BuiltInRegistries.ITEM.getKey(one.getItem()) + (one.getTag() == null ? "" : one.getTag().toString());
			if (!seen.add(key)) continue;
			int pay = Math.max(1, (int) Math.round(worth * share));
			MerchantOffer offer = new MerchantOffer(one, ItemStack.EMPTY, payment(pay), uses.getOrDefault(key, 0), 16, 0, 0);
			offers.add(offer);
			keys.put(offer, key);
		}
		return offers;
	}

	/** {@code item} as a line not yet open to one it regards at {@code have}: a line under its name, the regard it wants ({@code wants}) and theirs. */
	static ItemStack locked(ItemStack item, int wants, int have) {
		ItemStack shown = item.copy();
		CompoundTag display = shown.getOrCreateTagElement("display");
		ListTag lore = display.getList("Lore", Tag.TAG_STRING);
		Component line = Component.translatable(ModLang.TRADE_LOCKED, Component.translatable(ModLang.talkRegardLevel(Regard.levelId(wants))),
				Component.translatable(ModLang.talkRegardLevel(Regard.levelId(have)))).withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false));
		lore.add(0, StringTag.valueOf(Component.Serializer.toJson(line)));
		display.put("Lore", lore);
		return shown;
	}

	/** Whether {@code offer} is a line shown but not yet open (see {@link #locked}): sold out to the screen from the start. */
	public static boolean isLocked(MerchantOffer offer) {
		CompoundTag display = offer.getResult().getTagElement("display");
		if (display == null) return false;
		ListTag lore = display.getList("Lore", Tag.TAG_STRING);
		return !lore.isEmpty() && lore.getString(0).contains(ModLang.TRADE_LOCKED);
	}

	/** What one of {@code npc}'s station and path has to sell today, the regard-gated lines last. */
	static List<Goods> goodsFor(CultivatorNpc npc, RandomSource random) {
		List<Goods> goods = new ArrayList<>();
		// First, one of its sect's people (a clan's too): a guest pass of it, good a day, to one it isn't resentful toward (SectPassItem: the
		// user's ask, 2026-10-24, a way in for one too strong to be let in as a guest).
		Sect own = npc.getSectId() == null || npc.getServer() == null ? null : SectManager.get(npc.getServer(), npc.getSectId());
		if (own != null && !own.extinct) goods.add(new Goods(SectPassItem.of(own, npc.level().getGameTime() + SectPassItem.LASTS), SectPassItem.PRICE, 1, 0));
		if (npc.isMortal()) {
			goods.add(new Goods(new ItemStack(Items.BREAD, 4), 1, 8, 0));
			goods.add(new Goods(new ItemStack(pick(random, ModItems.GINSENG_SEEDS, ModItems.LINGZHI_SPORES, ModItems.HUANGJING_SEEDS)), 3, 4, 0));
			goods.add(herb(random, 10, 40, 0, ModItems.GINSENG, ModItems.LINGZHI, ModItems.HUANGJING));
			goods.add(new Goods(new ItemStack(ModBlocks.MEDITATION_MAT), 4, 2, Regard.PER_PIP));
			return goods;
		}
		ClothingStyle style = ClothingStyle.of(npc.getFaction());
		switch (npc.getTitle()) {
			case OUTER_DISCIPLE -> {
				goods.add(new Goods(new ItemStack(pick(random, ModItems.GINSENG_SEEDS, ModItems.LINGZHI_SPORES, ModItems.HUANGJING_SEEDS), 2), 5, 4, 0));
				goods.add(herb(random, 20, 60, 0, ModItems.GINSENG, ModItems.LINGZHI, ModItems.HUANGJING));
				goods.add(new Goods(new ItemStack(ModItems.SPIRIT_DEW, 2), 4, 4, 0));
				goods.add(new Goods(new ItemStack(ModBlocks.MEDITATION_MAT), 4, 2, Regard.PER_PIP));
				goods.add(pill(ModItems.QI_GATHERING_PILL, PillGrade.LOW, 2, Regard.COURTEOUS));
				goods.add(herb(random, 80, 150, Regard.WARM, ModItems.GINSENG, ModItems.LINGZHI, ModItems.HUANGJING));
			}
			case INNER_DISCIPLE -> {
				goods.add(new Goods(new ItemStack(pick(random, ModItems.SPIRIT_GINSENG_SEEDS, ModItems.PURPLE_LINGZHI_SPORES,
						ModItems.OCHRE_HUANGJING_SEEDS, ModItems.SPIRIT_LOTUS_SEEDS)), 10, 3, 0));
				goods.add(herb(random, 60, 150, 0, ModItems.GINSENG, ModItems.LINGZHI, ModItems.HUANGJING));
				goods.add(pill(ModItems.QI_GATHERING_PILL, PillGrade.LOW, 3, 0));
				goods.add(new Goods(new ItemStack(ModBlocks.MEDITATION_MAT), 4, 2, Regard.PER_PIP));
				goods.add(pill(ModItems.CULTIVATION_PILL, PillGrade.LOW, 2, Regard.COURTEOUS));
				goods.add(herb(random, 150, 300, Regard.WARM, ModItems.SPIRIT_GINSENG, ModItems.PURPLE_LINGZHI, ModItems.OCHRE_HUANGJING));
			}
			case ELDER, GRAND_ELDER -> {
				goods.add(pill(pick(random, ModItems.CULTIVATION_PILL, ModItems.MARROW_CLEANSING_ELIXIR), PillGrade.MID, 2, 0));
				goods.add(pill(ModItems.FOUNDATION_PILL, PillGrade.LOW, 2, 0));
				goods.add(herb(random, 200, 500, 0, ModItems.SPIRIT_GINSENG, ModItems.PURPLE_LINGZHI, ModItems.OCHRE_HUANGJING));
				goods.add(new Goods(new ItemStack(ModBlocks.RED_MEDITATION_MAT), 9, 2, Regard.PER_PIP));
				goods.add(pill(ModItems.CORE_PILL, PillGrade.LOW, 1, Regard.COURTEOUS));
				goods.add(artifact(random, ArtifactGrade.SPIRIT, npc, Regard.COURTEOUS));
				goods.add(new Goods(ClothingItem.create(ModItems.clothing(ClothingTier.SPIRIT, pickPiece(random)), style), 40, 1, Regard.WARM));
				goods.add(herb(random, 600, 1000, Regard.TRUSTED, ModItems.SPIRIT_GINSENG, ModItems.PURPLE_LINGZHI, ModItems.SPIRIT_LOTUS));
			}
			case SECT_MASTER -> {
				goods.add(pill(ModItems.CORE_PILL, PillGrade.MID, 1, 0));
				goods.add(pill(ModItems.CULTIVATION_PILL, PillGrade.HIGH, 2, 0));
				goods.add(herb(random, 800, 1500, 0, ModItems.SPIRIT_GINSENG, ModItems.PURPLE_LINGZHI, ModItems.SPIRIT_LOTUS));
				goods.add(new Goods(ClothingItem.create(ModItems.clothing(ClothingTier.EARTH, pickPiece(random)), style), 150, 1, Regard.PER_PIP));
				goods.add(pill(ModItems.FOUNDATION_PILL, PillGrade.HIGH, 1, Regard.COURTEOUS));
				goods.add(fruit(random, 400, 900, Regard.WARM));
				goods.add(artifact(random, ArtifactGrade.EARTH, npc, Regard.WARM));
				goods.add(new Goods(new ItemStack(ModItems.RING_OF_POWER), 400, 1, Regard.TRUSTED));
			}
			case ROGUE, WANDERER -> {
				// Odd things from far away: three of these, different each day, and a herb from its travels.
				List<Goods> odd = new ArrayList<>(List.of(
						new Goods(new ItemStack(ModItems.JADE, 2), 6, 4, 0),
						new Goods(new ItemStack(ModItems.PEACH_PIT), 14, 1, 0),
						new Goods(new ItemStack(ModItems.SPIRIT_LOTUS_SEEDS), 10, 2, 0),
						new Goods(new ItemStack(ModBlocks.QI_VEIN), 30, 1, 0),
						new Goods(new ItemStack(Items.ENDER_PEARL), 6, 3, 0),
						new Goods(new ItemStack(Items.NAUTILUS_SHELL), 8, 2, 0),
						new Goods(new ItemStack(Items.AMETHYST_SHARD, 4), 3, 4, 0),
						new Goods(new ItemStack(Items.BLAZE_ROD), 6, 2, 0),
						new Goods(new ItemStack(Items.GHAST_TEAR), 15, 1, 0),
						new Goods(new ItemStack(Items.ECHO_SHARD), 20, 1, 0),
						artifact(random, ArtifactGrade.MORTAL, npc, 0)));
				for (int i = 0; i < 3; i++) goods.add(odd.remove(random.nextInt(odd.size())));
				goods.add(herb(random, 100, 400, 0, ModItems.GINSENG, ModItems.LINGZHI, ModItems.HUANGJING, ModItems.SPIRIT_GINSENG));
				goods.add(odd.remove(random.nextInt(odd.size())).withRegard(Regard.PER_PIP));
				goods.add(pill(ModItems.QI_GATHERING_PILL, PillGrade.MID, 2, Regard.COURTEOUS));
				goods.add(herb(random, 400, 900, Regard.WARM, ModItems.SPIRIT_GINSENG, ModItems.PURPLE_LINGZHI, ModItems.OCHRE_HUANGJING));
			}
			case WANDERING_GHOST, GHOST_ELDER, GATE_WARDEN, BRIDGE_KEEPER, GHOST_KING, GHOST_RETAINER -> ghostGoods(npc, random, goods);
			case IMMORTAL_MERCHANT -> immortalGoods(npc, random, goods);
		}
		// In the Immortal Realm a sect's elders and its Sect Lord sell a demon race's True Immortal blood too, to one they are Warm to.
		if (ModDimensions.isImmortalRealm(npc.level().dimension()) && (npc.getTitle().isElderOrAbove() && npc.isSectMember())) {
			goods.addAll(immortalBlood(random, Regard.WARM));
		}
		// Last, the slip of its heaven method (CultivatorNpc#heavenMethod): an elder's (its sect's own) only to one it is Warm to, a
		// rogue's to the courteous; a disciple keeps its own. Never a True Immortal's (Realm#taughtInTheWorld).
		HeavenMethod heaven = npc.heavenMethod();
		if (heaven != null && heaven.realm().taughtInTheWorld() && (npc.getTitle().isElderOrAbove() || npc.getTitle() == NpcTitle.ROGUE)) {
			goods.add(new Goods(JadeSlipItem.of(heaven), heavenSlipPrice(heaven), 1, npc.getTitle().isElderOrAbove() ? Regard.WARM : Regard.COURTEOUS));
		}
		return goods;
	}

	/**
	 * What the Netherworld's dead sell (to the living and the dead alike: TalkSystem): the Netherworld's herbs and Yin Jade, and
	 * talismans against its yin; a Ghost Elder (a Ghost King, a king's retainer) its own method's Ghost Jade Slip (a king its
	 * realm's foundation only to one it is Warm to, at its price); the Gate's wardens the remedies for the yin
	 * they warn the living of, and the means to keep the restless dead in order (the Peach Wood Sword, Ghost-Quelling Talismans).
	 */
	private static void ghostGoods(CultivatorNpc npc, RandomSource random, List<Goods> goods) {
		goods.add(new Goods(new ItemStack(ModItems.YIN_JADE, 2), 8, 4, 0));
		goods.add(herb(random, 40, 120, 0, ModItems.OTHER_SHORE_FLOWER));
		goods.add(new Goods(new ItemStack(ModItems.YANG_FIRE_TALISMAN, 2), 6, 3, 0));
		switch (npc.getTitle()) {
			case GHOST_ELDER, GHOST_KING, GHOST_RETAINER -> {
				GhostMethod method = npc instanceof GhostCultivatorNpc ghost ? ghost.method() : null;
				// The Breathing Art, the fire, the steps and the Marrow Art to the courteous; a king's own canon only to one it is Warm to.
				if (method != null && method.taughtInTheWorld()) goods.add(new Goods(GhostJadeSlipItem.of(method), slipPrice(method), 1, method.tier() <= 2 ? Regard.COURTEOUS : Regard.WARM));
				goods.add(herb(random, 100, 300, Regard.PER_PIP, ModItems.NETHERWORLD_LOTUS));
				goods.add(pill(ModItems.YIN_GATHERING_PILL, PillGrade.MID, 2, Regard.WARM));
			}
			case GATE_WARDEN -> {
				goods.add(pill(ModItems.YANG_PRESERVATION_PILL, PillGrade.MID, 2, 0));
				goods.add(new Goods(new ItemStack(ModItems.PEACH_WOOD_SWORD), 20, 1, Regard.COURTEOUS));
				goods.add(new Goods(new ItemStack(ModItems.GHOST_QUELLING_TALISMAN, 2), 12, 2, Regard.COURTEOUS));
			}
			case BRIDGE_KEEPER -> goods.add(herb(random, 200, 500, 0, ModItems.NETHERWORLD_LOTUS));
			default -> goods.add(new Goods(new ItemStack(ModItems.OTHER_SHORE_FLOWER_SEEDS), 12, 2, Regard.PER_PIP));
		}
	}

	/** Immortal Jade's worth in Low stones, the markets' unit. */
	private static final int JADE = SpiritStoneItem.Grade.IMMORTAL_JADE.value();
	/** An Immortal stone's. */
	private static final int IMMORTAL_STONE = SpiritStoneItem.Grade.IMMORTAL.value();

	/**
	 * What an Immortal Realm merchant sells at its stall in a city's market (CityMarkets; phase 2 of the realm plan: "markets sell pills,
	 * treasures and True Immortal slips for jade"), priced for immortals: Immortal-grade pills, herbs ten thousand years old and
	 * Cultivation Fruit; a Sacred Vessel of its own element to one it is Warm to; and the slip of its element's True Immortal heaven
	 * method (its own method's element, CultivatorNpc#heavenMethod) to the courteous, sold nowhere in the worlds below (Realm#taughtInTheWorld), at
	 * {@link #heavenSlipPrice}; one merchant in five instead the ghost path's last foundation, the Netherworld Immortal Canon.
	 */
	private static void immortalGoods(CultivatorNpc npc, RandomSource random, List<Goods> goods) {
		goods.add(new Goods(PillItem.create(ModItems.CULTIVATION_PILL, PillGrade.IMMORTAL, 100), 2 * IMMORTAL_STONE, 2, 0));
		goods.add(new Goods(PillItem.create(ModItems.QI_GATHERING_PILL, PillGrade.IMMORTAL, 100), IMMORTAL_STONE, 3, 0));
		ItemStack herb = GinsengItem.create(pick(random, ModItems.SPIRIT_GINSENG, ModItems.PURPLE_LINGZHI, ModItems.SPIRIT_LOTUS), Mth.nextInt(random, 5000, 10000));
		goods.add(new Goods(herb, 3 * IMMORTAL_STONE, 2, 0));
		// Its fruit as old as its herbs (the bug sweep of 2026-10-27: an ageless stack, a fruit of one year, sold at an Immortal Jade).
		goods.add(new Goods(CultivationFruitItem.create(Mth.nextInt(random, 5000, 10000)), JADE, 1, Regard.PER_PIP));
		HeavenMethod own = npc.heavenMethod();
		QiElement element = own != null ? own.element() : QiElement.five()[(int) Math.floorMod(npc.getUUID().getLeastSignificantBits() >>> 3, 5L)];
		goods.add(artifact(random, ArtifactGrade.HEAVEN, npc, 0));
		goods.add(new Goods(new ItemStack(ModItems.sacredVessel(element)), 30 * JADE, 1, Regard.WARM));
		goods.add(artifact(random, ArtifactGrade.IMMORTAL, npc, Regard.WARM));
		if (Math.floorMod(npc.getUUID().getMostSignificantBits(), 5L) == 0) {
			goods.add(new Goods(GhostJadeSlipItem.of(GhostMethod.NETHERWORLD_IMMORTAL_CANON), slipPrice(GhostMethod.NETHERWORLD_IMMORTAL_CANON), 1, Regard.COURTEOUS));
		} else {
			HeavenMethod method = HeavenMethod.of(element, Realm.TRUE_IMMORTAL);
			goods.add(new Goods(JadeSlipItem.of(method), heavenSlipPrice(method), 1, Regard.COURTEOUS));
		}
		goods.addAll(immortalBlood(random, Regard.COURTEOUS));
	}

	/**
	 * A demon race's blood (the user's ask, 2026-10-26: "a way to purchase true immortal demon race blood and above at sects or cities or from
	 * the capital"): one race's today, a phial of each realm's from True Immortal up (the ladder past it too since phase 3, though its blood
	 * alone climbs no realm: DemonPath wants the Laws besides), each at its price ({@link #bloodPrice}), to one regarded at {@code regard} or
	 * better. Sold nowhere in the worlds below.
	 */
	static List<Goods> immortalBlood(RandomSource random, int regard) {
		List<Bloodline> races = new ArrayList<>();
		for (Bloodline race : Bloodline.values()) if (race.isBeast()) races.add(race);
		Bloodline race = races.get(random.nextInt(races.size()));
		List<Goods> goods = new ArrayList<>();
		for (Realm realm : Realm.values()) {
			if (realm.ordinal() >= Realm.TRUE_IMMORTAL.ordinal()) goods.add(new Goods(DemonBloodItem.of(race, realm), bloodPrice(realm), 1, regard));
		}
		return goods;
	}

	/**
	 * A demon race's blood's price (Low stones): its realm's slip's ({@link #heavenSlipPrice}), a True Immortal's sixty Immortal Jade; past
	 * it, which no slip teaches (phase 3), ten jade more a realm, a Heavenly Lord's 120 (within the two stacks of jade a trade can ask:
	 * {@link #costsFor}).
	 */
	public static int bloodPrice(Realm realm) {
		if (realm.isPastTrueImmortal()) return bloodPrice(Realm.TRUE_IMMORTAL) + 10 * realm.pastTrueImmortal() * JADE;
		long price = GHOST_SLIP_PRICE;
		for (int t = 0; t < realm.ordinal(); t++) price = Math.min(Integer.MAX_VALUE, price * 3);
		return (int) price;
	}

	/** A Ghost Jade Slip's price (Low stones): a whole method's first layer, and the way to the rest. */
	public static final int GHOST_SLIP_PRICE = 60;

	/**
	 * What a slip of {@code method} is worth (Low stones): {@link #GHOST_SLIP_PRICE} for the Breathing Art, the fire and the steps,
	 * three times as much again for each foundation deeper (the Yellow Springs Marrow Art 180, the Ghost Core Canon 540, on to the
	 * Six Paths Reincarnation Canon's 14,580).
	 */
	public static int slipPrice(GhostMethod method) {
		int price = GHOST_SLIP_PRICE;
		if (method != null) for (int t = 1; t < method.tier(); t++) price *= 3;
		return price;
	}

	/** A Jade Slip's price (Low stones): {@link #GHOST_SLIP_PRICE} for a Qi Refining method, three times again each realm deeper. */
	public static int heavenSlipPrice(HeavenMethod method) {
		int price = GHOST_SLIP_PRICE;
		if (method != null) for (int t = 1; t < method.tier(); t++) price *= 3;
		return price;
	}

	private static Goods herb(RandomSource random, int minYears, int maxYears, int regard, Item... kinds) {
		ItemStack herb = GinsengItem.create(pick(random, kinds), Mth.nextInt(random, minYears, maxYears));
		return new Goods(herb, (int) Math.ceil(TalkSystem.worth(herb) * MARKUP), 2, regard);
	}

	/**
	 * A Cultivation Fruit {@code minYears} to {@code maxYears} old, priced by its worth as a herb is (the bug sweep of 2026-10-27: the Sect
	 * Master's was an ageless stack, a fruit of one year, at the price of one some six hundred years old).
	 */
	private static Goods fruit(RandomSource random, int minYears, int maxYears, int regard) {
		ItemStack fruit = CultivationFruitItem.create(Mth.nextInt(random, minYears, maxYears));
		return new Goods(fruit, (int) Math.ceil(TalkSystem.worth(fruit) * MARKUP), 1, regard);
	}

	private static Goods pill(Item kind, PillGrade grade, int maxUses, int regard) {
		ItemStack pill = PillItem.create(kind, grade, 100);
		return new Goods(pill, (int) Math.ceil(TalkSystem.worth(pill) * MARKUP), maxUses, regard);
	}

	/**
	 * An artifact of {@code grade} from the Artifact Forge, of any kind (a banner of {@code npc}'s own element, and from Spirit grade up the
	 * weapon made of it), at its price.
	 */
	private static Goods artifact(RandomSource random, ArtifactGrade grade, CultivatorNpc npc, int regard) {
		HeavenMethod own = npc.heavenMethod();
		ItemStack artifact = Artifacts.any(random, grade, own != null ? own.element() : QiElement.five()[random.nextInt(5)]);
		return new Goods(artifact, Artifacts.priceOf(artifact), 1, regard);
	}

	@SafeVarargs
	private static <T> T pick(RandomSource random, T... options) {
		return options[random.nextInt(options.length)];
	}

	private static ArmorItem.Type pickPiece(RandomSource random) {
		return ArmorItem.Type.values()[random.nextInt(ArmorItem.Type.values().length)];
	}

	/**
	 * {@code price} Low stones as the trading screen's two costs: as many of the largest grade that fits, then the rest in the
	 * grade below (rounded up; nine of those make one more of the larger). More than a stack of the largest grade (Immortal Jade:
	 * the ladder's blood, phase 3), the second cost is more of it (rounded up), two stacks at most.
	 */
	static ItemStack[] costsFor(int price) {
		SpiritStoneItem.Grade[] grades = SpiritStoneItem.Grade.values();
		int top = 0;
		for (int i = grades.length - 1; i >= 0; i--) {
			if (grades[i].value() <= price) {
				top = i;
				break;
			}
		}
		int count = price / grades[top].value();
		int rest = price - count * grades[top].value();
		if (count > 64 || count == 64 && rest > 0) {
			int more = Math.min(64, count - 64 + (rest > 0 ? 1 : 0));
			return new ItemStack[] {new ItemStack(SpiritStoneItem.of(grades[top]), 64), more > 0 ? new ItemStack(SpiritStoneItem.of(grades[top]), more) : ItemStack.EMPTY};
		}
		ItemStack second = ItemStack.EMPTY;
		if (rest > 0 && top > 0) {
			int below = (rest + grades[top - 1].value() - 1) / grades[top - 1].value();
			if (below >= 9) count++;
			else second = new ItemStack(SpiritStoneItem.of(grades[top - 1]), below);
		}
		return new ItemStack[] {new ItemStack(SpiritStoneItem.of(grades[top]), Math.min(64, count)), second};
	}

	/** {@code value} Low stones as one payment: in the finest grade that still pays it to within a stone, at most a stack. */
	static ItemStack payment(int value) {
		SpiritStoneItem.Grade[] grades = SpiritStoneItem.Grade.values();
		for (int i = grades.length - 1; i > 0; i--) {
			if (value >= grades[i].value() && value / grades[i - 1].value() > 64) {
				return new ItemStack(SpiritStoneItem.of(grades[i]), Math.min(64, value / grades[i].value()));
			}
		}
		for (int i = grades.length - 1; i >= 0; i--) {
			if (value >= grades[i].value() && value % grades[i].value() == 0) return new ItemStack(SpiritStoneItem.of(grades[i]), Math.min(64, value / grades[i].value()));
		}
		return new ItemStack(ModItems.LOW_SPIRIT_STONE, Math.min(64, Math.max(1, value)));
	}

	private CultivatorTrades() {}
}
