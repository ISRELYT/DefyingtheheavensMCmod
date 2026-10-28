package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Celestial Court (phase 2, 2026-10-25): its ranks by merit; its King a Profound Sovereign and its Venerables Taiyi Golden Immortals
 * (the ladder's own realms since phase 3), a Venerable's own element's method; its posts on its pinnacle as the capital's plan lays them (the King on the throne, a Venerable
 * of each element in each hall, four Generals at the gate, the Quartermaster on the palace's terrace); a Venerable taking one into the
 * Court; an officer's merit paid in tokens, their rank risen, the Quartermaster's wares opened by it; the bounty on the realm's devilish
 * immortals; the Gate's watch kept on its plaza; the Court's officers kept as saved.
 */
public class CelestialCourtGameTests implements FabricGameTest {
	private static final int TOP = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCourtsRanksAndMight(GameTestHelper helper) {
		CelestialCourt.Rank officer = CelestialCourt.Rank.OFFICER, captain = CelestialCourt.Rank.CAPTAIN, general = CelestialCourt.Rank.GENERAL;
		helper.assertTrue(CelestialCourt.Rank.of(0) == officer && CelestialCourt.Rank.of(29) == officer && CelestialCourt.Rank.of(30) == captain
				&& CelestialCourt.Rank.of(99) == captain && CelestialCourt.Rank.of(100) == general && CelestialCourt.Rank.of(5000) == general, "Officer, Captain at 30, General at 100");
		helper.assertTrue(officer.next() == captain && captain.next() == general && general.next() == null, "Each rank's next");
		// Its King the one Profound Sovereign, its Venerables Taiyi Golden Immortals (phase 3: the ladder's own realms), its Generals True
		// Immortals of Late or Grand Perfection, the rest True Immortals.
		RandomSource random = RandomSource.create(7);
		int kingRank = CelestialCourt.rankOf(NpcTitle.HEAVENLY_KING, random), venerableRank = CelestialCourt.rankOf(NpcTitle.HEAVENLY_VENERABLE, random);
		helper.assertTrue(kingRank == PlayerCultivation.rank(Realm.PROFOUND_SOVEREIGN, Stage.GRAND_PERFECTION)
				&& venerableRank == PlayerCultivation.rank(Realm.TAIYI_GOLDEN_IMMORTAL, Stage.GRAND_PERFECTION), "The King a Profound Sovereign, a Venerable a Taiyi Golden Immortal");
		for (int i = 0; i < 20; i++) {
			int generalRank = CelestialCourt.rankOf(NpcTitle.IMMORTAL_GENERAL, random);
			helper.assertTrue(generalRank == PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.LATE) || generalRank == TOP, "A General a True Immortal of Late or Grand Perfection");
		}
		helper.assertTrue(CelestialCourt.rankOf(NpcTitle.IMMORTAL_OFFICER, random) == PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), "An officer a True Immortal");
		CultivatorNpc plain = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1)), king = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 1)),
				venerable = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 1));
		plain.setRank(TOP, true);
		king.setRank(kingRank, true);
		venerable.setRank(venerableRank, true);
		king.setTitle(NpcTitle.HEAVENLY_KING);
		venerable.setCourtElement(QiElement.WOOD);
		venerable.setTitle(NpcTitle.HEAVENLY_VENERABLE);
		// Each realm past True Immortal twice the last (the user's rule): a Taiyi sixteen times a True Immortal, a Profound Sovereign thirty-two.
		helper.assertTrue(Realm.TAIYI_GOLDEN_IMMORTAL.getMaxHealth() == 16 * Realm.TRUE_IMMORTAL.getMaxHealth()
				&& Realm.PROFOUND_SOVEREIGN.getMaxHealth() == 32 * Realm.TRUE_IMMORTAL.getMaxHealth()
				&& Realm.PROFOUND_SOVEREIGN.getAttackDamage() == 32 * Realm.TRUE_IMMORTAL.getAttackDamage(), "Each realm past True Immortal twice the last");
		double health = king.getMaxHealth() / plain.getMaxHealth();
		helper.assertTrue(Math.abs(king.getMaxHealth() - (CultivationStats.maxHealth(Realm.PROFOUND_SOVEREIGN, Stage.GRAND_PERFECTION) + 20)) < 2
				&& Math.abs(venerable.getMaxHealth() - (CultivationStats.maxHealth(Realm.TAIYI_GOLDEN_IMMORTAL, Stage.GRAND_PERFECTION) + 20)) < 2,
				"The King's body a Profound Sovereign's of Grand Perfection, a Venerable's a Taiyi's (" + health + " times a True Immortal's)");
		double blows = king.getAttributeValue(Attributes.ATTACK_DAMAGE) / plain.getAttributeValue(Attributes.ATTACK_DAMAGE);
		helper.assertTrue(health > 31.9 && blows > 31.9, "The King's body and blows some thirty-two times a True Immortal's (" + health + ", " + blows + ")");
		helper.assertTrue(HeavenMethod.of(QiElement.WOOD, Realm.TRUE_IMMORTAL).equals(venerable.heavenMethod()), "A Venerable's method its own element's True Immortal one");
		helper.assertTrue(king.heavenMethod() != null && king.heavenMethod().realm() == Realm.TRUE_IMMORTAL, "No method teaches past True Immortal: the King's is a True Immortal one");
		helper.assertTrue(king.getDisplayedRealm() == Realm.PROFOUND_SOVEREIGN && venerable.getDisplayedRealm() == Realm.TAIYI_GOLDEN_IMMORTAL,
				"Named by their realms (" + king.getDisplayedRealm() + ", " + venerable.getDisplayedRealm() + ")");
		// Stripped of its station, a King keeps its realm: a Profound Sovereign still.
		king.setTitle(NpcTitle.ROGUE);
		helper.assertTrue(king.getRank() == kingRank && Math.abs(king.getMaxHealth() / plain.getMaxHealth() - health) < 0.01, "No King, still a Profound Sovereign");
		helper.succeed();
	}

	private static String key(Component component) {
		return component.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCourtsPosts(GameTestHelper helper) {
		ImmortalCitiesGameTests.Land land = ImmortalCitiesGameTests.land(helper);
		ImmortalCapital.Plan plan = ImmortalCapital.plan(land.state(), land.generator(), land.heights());
		SectPiece palace = null, gate = null;
		List<SectPiece> halls = new ArrayList<>();
		for (SectPiece piece : plan.pieces()) {
			BlockPos centre = piece.getBoundingBox().getCenter();
			if (Math.hypot(centre.getX() - ModNoiseSettings.COURT_X, centre.getZ() - ModNoiseSettings.COURT_Z) > ImmortalCapital.COURT_REACH + 8) continue;
			if (piece.kind() == SectPiece.Kind.HOLY_PALACE) palace = piece;
			if (piece.kind() == SectPiece.Kind.HOLY_GATE) gate = piece;
			if (piece.kind() == SectPiece.Kind.MEDITATION_HALL) halls.add(piece);
		}
		helper.assertTrue(palace != null && gate != null && halls.size() == 5, "The Court's palace, gate and five halls");
		Map<NpcTitle, Integer> counts = new EnumMap<>(NpcTitle.class);
		Set<QiElement> elements = EnumSet.noneOf(QiElement.class);
		for (CelestialCourt.Post post : CelestialCourt.posts(plan)) {
			counts.merge(post.title(), 1, Integer::sum);
			BlockPos at = post.at();
			// On the pinnacle's level top: the Generals before the gate stand on its front row, just past the buildings' reach.
			helper.assertTrue(Math.hypot(at.getX() - ModNoiseSettings.COURT_X, at.getZ() - ModNoiseSettings.COURT_Z) <= ModNoiseSettings.COURT_INNER,
					"Every post on the Court's pinnacle (" + post.title() + " at " + at.toShortString() + ")");
			switch (post.title()) {
				case HEAVENLY_KING, COURT_QUARTERMASTER -> helper.assertTrue(palace.getBoundingBox().isInside(at), "The " + post.title() + " at the palace");
				case IMMORTAL_GENERAL -> helper.assertTrue(gate.getBoundingBox().inflatedBy(1).isInside(at), "A General at the gate");
				case HEAVENLY_VENERABLE -> {
					boolean inHall = false;
					for (SectPiece hall : halls) inHall |= hall.getBoundingBox().isInside(at);
					helper.assertTrue(inHall && post.seated(), "A Venerable seated in a hall");
					elements.add(post.element());
				}
				default -> helper.fail("A post of no Court title: " + post.title());
			}
		}
		helper.assertTrue(counts.getOrDefault(NpcTitle.HEAVENLY_KING, 0) == 1 && counts.getOrDefault(NpcTitle.HEAVENLY_VENERABLE, 0) == 5
				&& counts.getOrDefault(NpcTitle.IMMORTAL_GENERAL, 0) == 4 && counts.getOrDefault(NpcTitle.COURT_QUARTERMASTER, 0) == 1,
				"One King, five Venerables, four Generals, one Quartermaster (" + counts + ")");
		helper.assertTrue(elements.size() == 5 && !elements.contains(QiElement.YIN), "A Venerable of each of the five elements");
		BoundingBox box = palace.getBoundingBox();
		helper.assertTrue(box.minY() == plan.courtY(), "The palace on the Court's level");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anOfficersMerit(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(!CelestialCourt.isMember(player.server, player.getUUID()) && CelestialCourt.rankOf(player.server, player.getUUID()) == null,
				"Not of the Court until taken in");
		CultivatorNpc quartermaster = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1)), venerable = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 1));
		quartermaster.setRank(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), true);
		quartermaster.setTitle(NpcTitle.COURT_QUARTERMASTER);
		venerable.setRank(TOP, true);
		venerable.setCourtElement(QiElement.FIRE);
		venerable.setTitle(NpcTitle.HEAVENLY_VENERABLE);
		Map<String, Integer> uses = new HashMap<>();
		Map<MerchantOffer, String> keys = new HashMap<>();
		helper.assertTrue(open(CelestialCourt.offers(quartermaster, player, uses, keys)) == 0, "To one not of the Court, the Quartermaster sells nothing");
		// A Venerable takes them in (they could climb the Gate), then tells them where they stand.
		helper.assertTrue(key(CelestialCourt.ask(player, venerable)).equals(ModLang.talkLine("court.join")) && CelestialCourt.isMember(player.server, player.getUUID()),
				"A Venerable takes one into the Court");
		helper.assertTrue(key(CelestialCourt.ask(player, venerable)).equals(ModLang.talkLine("court.member")), "Asked again, it tells them their rank");
		helper.assertTrue(CelestialCourt.rankOf(player.server, player.getUUID()) == CelestialCourt.Rank.OFFICER, "An Officer to begin with");
		MerchantOffers asOfficer = CelestialCourt.offers(quartermaster, player, uses, keys);
		helper.assertTrue(open(asOfficer) == 6 && asOfficer.size() == 11, "Jade and the five True Immortal methods to an Officer (" + open(asOfficer) + " open)");
		helper.assertTrue(asOfficer.get(1).getCostA().is(ModItems.COURT_MERIT_TOKEN) && asOfficer.get(1).getCostA().getCount() == 20, "A method's slip for twenty tokens");
		// Merit paid in tokens, the rank risen with it.
		CelestialCourt.award(player, 30, ModLang.MSG_COURT_MERIT_WATCH);
		helper.assertTrue(CelestialCourt.rankOf(player.server, player.getUUID()) == CelestialCourt.Rank.CAPTAIN && CelestialCourt.meritOf(player.server, player.getUUID()) == 30
				&& player.getInventory().countItem(ModItems.COURT_MERIT_TOKEN) == 30, "Thirty merit, thirty tokens: a Captain");
		helper.assertTrue(open(CelestialCourt.offers(quartermaster, player, uses, keys)) == 9, "A Captain's wares too (the day's Immortal artifact among them)");
		CelestialCourt.award(player, 70, ModLang.MSG_COURT_MERIT_WATCH);
		helper.assertTrue(CelestialCourt.rankOf(player.server, player.getUUID()) == CelestialCourt.Rank.GENERAL && open(CelestialCourt.offers(quartermaster, player, uses, keys)) == 11,
				"A hundred: a General, every ware open");
		// The bounty on the realm's devilish immortals: a True Immortal more, none on the righteous, none below Entering Nirvana.
		CultivatorNpc nirvana = devil(helper, PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY), -90, 3),
				immortal = devil(helper, PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), -90, 4),
				righteous = devil(helper, PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), 90, 5),
				lesser = devil(helper, PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.GRAND_PERFECTION), -90, 6);
		helper.assertTrue(CelestialCourt.bounty(nirvana) == CelestialCourt.SLAY_NIRVANA && CelestialCourt.bounty(immortal) == CelestialCourt.SLAY_IMMORTAL
				&& CelestialCourt.bounty(righteous) == 0 && CelestialCourt.bounty(lesser) == 0, "The bounty: 2 for a devilish Entering Nirvana, 5 for a True Immortal");
		// The Gate's watch: kept on its plaza, not beyond it nor high over it.
		int y = 240;
		player.moveTo(ImmortalGate.HEAVEN_X + 0.5, y + 1, ImmortalGate.HEAVEN_Z + 10.5);
		boolean on = CelestialCourt.onWatch(player, y);
		player.moveTo(ImmortalGate.HEAVEN_X + ImmortalGate.PLAZA_RADIUS + 6.5, y + 1, ImmortalGate.HEAVEN_Z + 0.5);
		boolean beyond = CelestialCourt.onWatch(player, y);
		player.moveTo(ImmortalGate.HEAVEN_X + 0.5, y + 40, ImmortalGate.HEAVEN_Z + 0.5);
		boolean over = CelestialCourt.onWatch(player, y);
		helper.assertTrue(on && !beyond && !over, "On watch on the plaza only (" + on + ", " + beyond + ", " + over + ")");
		// Its officers kept as saved.
		CelestialCourt.Data loaded = CelestialCourt.Data.load(CelestialCourt.Data.get(player.server).save(new CompoundTag()));
		CelestialCourt.Data.Member member = loaded.members.get(player.getUUID());
		helper.assertTrue(member != null && member.merit == 100, "Their merit kept");
		helper.succeed();
	}

	private static CultivatorNpc devil(GameTestHelper helper, int rank, int alignment, int x) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(x, 2, 2));
		npc.setRank(rank, true);
		npc.setAlignment(alignment);
		return npc;
	}

	/** How many of {@code offers} are open (not crossed out). */
	private static int open(MerchantOffers offers) {
		int open = 0;
		for (MerchantOffer offer : offers) if (!CultivatorTrades.isLocked(offer)) open++;
		return open;
	}
}
