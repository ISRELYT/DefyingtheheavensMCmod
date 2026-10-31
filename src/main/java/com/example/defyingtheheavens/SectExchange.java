package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The sect's exchange, its mission board's other half (SectMissions; the plan the user approved, 2026-10-28): its disciples spend their
 * Contribution there. Its wares are its world's (the lower realms', the Jade Firmament's, the Great Void's, the Immortal Realm's: each a
 * grade up on the one before, and dearer: {@link #TIER_PRICE}), each kept for a title and above (SectMembership#titleOf: Outer Disciple,
 * Inner Disciple, Elder), so many of each a day:
 * <ul>
 *   <li>Outer Disciples: Qi Gathering and Cultivation Pills, Ginseng and Spirit Ginseng, Yang Fire Talismans, and a few spirit stones at a
 *   poor rate.</li>
 *   <li>Inner Disciples: Foundation and Core Pills, the day's two manuals of the sect's element's spells (to their realm), the day's artifact,
 *   a storage formation.</li>
 *   <li>Elders: the weapon made of the sect's element, the day's method of another element (at their realm), the techniques' manuals they
 *   stand at the realm of, a Longevity Pill.</li>
 * </ul>
 */
public final class SectExchange {
	/** One of its wares: the key its day's count is kept under, the thing, its price (Contribution), how many a day, the least title it is kept for. */
	public record Ware(String key, ItemStack item, int price, int perDay, NpcTitle title) {}

	/** What its prices are multiplied by, by its world (#tier): the lower realms', the Jade Firmament's, the Great Void's, the Immortal Realm's. */
	static final int[] TIER_PRICE = {1, 2, 4, 6};

	/** {@code sect}'s world, as its exchange keeps it: 0 the lower realms, 1 the Jade Firmament, 2 the Great Void, 3 the Immortal Realm. */
	static int tier(Sect sect) {
		if (ModDimensions.isImmortalRealm(sect.dimension)) return 3;
		if (ModDimensions.isGreatVoid(sect.dimension)) return 2;
		if (ModDimensions.isUpperRealm(sect.dimension)) return 1;
		return 0;
	}

	/** What {@code sect}'s exchange holds for {@code player} today, in the order its page lists it (the day's choices by the game day). */
	static List<Ware> wares(Sect sect, ServerPlayer player) {
		int tier = tier(sect), p = TIER_PRICE[tier];
		long day = Regard.day(player.level().getGameTime());
		PillGrade pills = PillGrade.values()[Math.min(PillGrade.values().length - 1, tier + 1)];
		int years = new int[] {20, 60, 200, 600}[tier];
		ArtifactGrade forged = new ArtifactGrade[] {ArtifactGrade.SPIRIT, ArtifactGrade.EARTH, ArtifactGrade.HEAVEN, ArtifactGrade.IMMORTAL}[tier];
		QiElement element = sect.element();
		Realm realm = CultivatorNpc.realmOf(SectMissions.rankOf(player));
		List<Ware> wares = new ArrayList<>();
		// Outer Disciples and up
		wares.add(new Ware("qi_pill", PillItem.create(ModItems.QI_GATHERING_PILL, pills, years), 8 * p, 4, NpcTitle.OUTER_DISCIPLE));
		wares.add(new Ware("cultivation_pill", PillItem.create(ModItems.CULTIVATION_PILL, pills, years), 12 * p, 3, NpcTitle.OUTER_DISCIPLE));
		ItemStack ginseng = GinsengItem.create(ModItems.GINSENG, years);
		ginseng.setCount(2);
		wares.add(new Ware("ginseng", ginseng, 6 * p, 4, NpcTitle.OUTER_DISCIPLE));
		wares.add(new Ware("spirit_ginseng", GinsengItem.create(ModItems.SPIRIT_GINSENG, years), 10 * p, 2, NpcTitle.OUTER_DISCIPLE));
		wares.add(new Ware("talisman", new ItemStack(ModItems.YANG_FIRE_TALISMAN, 2), 6 * p, 2, NpcTitle.OUTER_DISCIPLE));
		ItemStack stones = switch (tier) {
			case 0 -> new ItemStack(ModItems.LOW_SPIRIT_STONE, 4);
			case 1 -> new ItemStack(ModItems.MID_SPIRIT_STONE, 2);
			case 2 -> new ItemStack(ModItems.HIGH_SPIRIT_STONE, 2);
			default -> new ItemStack(ModItems.IMMORTAL_JADE, 1);
		};
		wares.add(new Ware("stones", stones, new int[] {10, 20, 40, 60}[tier], 2, NpcTitle.OUTER_DISCIPLE));
		// Inner Disciples and up
		wares.add(new Ware("foundation_pill", PillItem.create(ModItems.FOUNDATION_PILL, pills, years), 40 * p, 1, NpcTitle.INNER_DISCIPLE));
		wares.add(new Ware("core_pill", PillItem.create(ModItems.CORE_PILL, pills, years), 80 * p, 1, NpcTitle.INNER_DISCIPLE));
		// The way out of a secret realm before it closes (SecretRealms).
		wares.add(new Ware("departure_jade", new ItemStack(ModItems.DEPARTURE_JADE), 30 * p, 1, NpcTitle.INNER_DISCIPLE));
		List<Spell> spells = new ArrayList<>();
		for (Spell spell : Spell.values()) {
			if (spell.isHeaven() && spell.element() == element && spell.heavenRealm().ordinal() <= realm.ordinal()) spells.add(spell);
		}
		// The day's two of them (two different ones while there are two to have).
		if (!spells.isEmpty()) {
			int first = (int) Math.floorMod(day * 7 + sect.id.hashCode(), (long) spells.size());
			List<Spell> todays = new ArrayList<>(List.of(spells.get(first)));
			if (spells.size() > 1) todays.add(spells.get((first + 1 + (int) Math.floorMod(day, (long) spells.size() - 1)) % spells.size()));
			for (Spell spell : todays) {
				wares.add(new Ware("spell:" + spell.getId(), TechniqueManualItem.of(spell), 30 + 30 * spell.heavenRealm().ordinal(), 1, NpcTitle.INNER_DISCIPLE));
			}
		}
		ArtifactKind kind = SectMissions.PLAIN[(int) Math.floorMod(day, (long) SectMissions.PLAIN.length)];
		wares.add(new Ware("artifact", Artifacts.create(kind, forged, Artifacts.Quality.NORMAL, kind == ArtifactKind.BANNER ? element : null), 60 * p, 1,
				NpcTitle.INNER_DISCIPLE));
		wares.add(new Ware("storage", StorageFormationItem.of(StorageFormations.Tier.values()[tier]), 50 * p, 1, NpcTitle.INNER_DISCIPLE));
		// Elders and up
		wares.add(new Ware("weapon", Artifacts.create(ArtifactKind.madeOf(element), forged, Artifacts.Quality.NORMAL, null), 150 * p, 1, NpcTitle.ELDER));
		HeavenMethod method = otherMethod(sect, element, realm, day);
		if (method != null) wares.add(new Ware("method", JadeSlipItem.of(method), 120 * p, 1, NpcTitle.ELDER));
		for (Technique technique : Technique.values()) {
			if (realm.ordinal() >= technique.realm().ordinal()) {
				wares.add(new Ware("technique:" + technique.getId(), TechniqueManualItem.of(technique), 300 + 200 * technique.ordinal(), 1, NpcTitle.ELDER));
			}
		}
		wares.add(new Ware("longevity_pill", PillItem.create(ModItems.LONGEVITY_PILL, pills, years), 200 * p, 1, NpcTitle.ELDER));
		return wares;
	}

	/**
	 * The day's method of an element not the sect's own (each of the other four in turn), at {@code realm} or the deepest below it there is
	 * one of; never past what the worlds teach outside the Immortal Realm (Realm#taughtInTheWorld). Null if there is none.
	 */
	static @Nullable HeavenMethod otherMethod(Sect sect, QiElement own, Realm realm, long day) {
		List<QiElement> others = new ArrayList<>();
		for (QiElement element : QiElement.five()) if (element != own) others.add(element);
		QiElement element = others.get((int) Math.floorMod(day, (long) others.size()));
		Realm top = ModDimensions.isImmortalRealm(sect.dimension) ? realm : realm.atMostTaught();
		for (int r = top.ordinal(); r >= 0; r--) {
			HeavenMethod method = HeavenMethod.of(element, Realm.values()[r]);
			if (method != null) return method;
		}
		return null;
	}

	/** How many of {@code key} {@code d} has had today. */
	static int boughtToday(ServerPlayer player, Sect.Disciple d, String key) {
		return d.boughtDay == Regard.day(player.level().getGameTime()) ? d.bought.getOrDefault(key, 0) : 0;
	}

	/** One ware on the board's page (S2C mission_board): its place in the list, the thing, its price, how many left today of how many, whether it is open to them (else the title it is kept for). */
	static void write(FriendlyByteBuf buf, ServerPlayer player, Sect sect, Sect.Disciple d, Ware ware, int index) {
		buf.writeVarInt(index);
		buf.writeItem(ware.item());
		buf.writeVarInt(ware.price());
		buf.writeVarInt(Math.max(0, ware.perDay() - boughtToday(player, d, ware.key())));
		buf.writeVarInt(ware.perDay());
		boolean open = SectMembership.titleOf(sect, player).ordinal() <= ware.title().ordinal();
		buf.writeBoolean(open);
		buf.writeComponent(open ? Component.empty() : Component.translatable(ModLang.EXCHANGE_LOCKED, keptFor(sect, ware)));
	}

	private static Component keptFor(Sect sect, Ware ware) {
		return ware.title().getDisplayName(sect.isClan(), sect.isHoly(), sect.isImmortal());
	}

	/** {@code d} takes ware {@code index} for its Contribution: theirs, if their title opens it, the day's are not gone, and they have enough. */
	static @Nullable Component buy(ServerPlayer player, Sect sect, Sect.Disciple d, int index) {
		List<Ware> wares = wares(sect, player);
		if (index < 0 || index >= wares.size()) return null;
		Ware ware = wares.get(index);
		if (SectMembership.titleOf(sect, player).ordinal() > ware.title().ordinal()) {
			return Component.translatable(ModLang.MSG_EXCHANGE_LOCKED, keptFor(sect, ware)).withStyle(ChatFormatting.RED);
		}
		long today = Regard.day(player.level().getGameTime());
		if (d.boughtDay != today) {
			d.boughtDay = today;
			d.bought.clear();
		}
		int had = d.bought.getOrDefault(ware.key(), 0);
		if (had >= ware.perDay()) return Component.translatable(ModLang.MSG_EXCHANGE_SOLD_OUT).withStyle(ChatFormatting.GRAY);
		if (d.contribution < ware.price()) return Component.translatable(ModLang.MSG_EXCHANGE_POOR, ware.price(), d.contribution).withStyle(ChatFormatting.RED);
		d.contribution -= ware.price();
		d.bought.put(ware.key(), had + 1);
		ItemStack item = ware.item().copy();
		Component said = Component.translatable(ModLang.MSG_EXCHANGE_BOUGHT, ware.price(), item.getCount(), item.getHoverName()).withStyle(ChatFormatting.GOLD);
		if (!player.getInventory().add(item)) player.drop(item, false);
		SectManager.dirty(player.server);
		player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1.2f);
		return said;
	}

	private SectExchange() {}
}
