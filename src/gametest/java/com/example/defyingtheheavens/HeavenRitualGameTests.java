package com.example.defyingtheheavens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Ritual to the Heavens (HeavenRitual, HeavenAltar): Qi Refining Mid waits on the heavens' leave, how offerings and
 * altars are weighed, Heaven's Favour, a player's ritual (the offering taken, the answer, the wait until dawn, moving breaks
 * it), a sect member kneeling at its sect's altar on the sect's store, and the words for it all.
 */
public class HeavenRitualGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void qiRefiningMidWaitsOnTheHeavens(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		c.addCultivation(1_000_000);
		helper.assertTrue(c.getStage() == Stage.MID && c.isAtRitualGate() && !c.isAtBottleneck() && !c.canBreakthrough(),
				"Early runs on into Mid, and Mid stops full: no tribulation, the heavens' leave");
		c.setMedicinalQi(50);
		helper.assertTrue(c.refine(30) == 0 && c.isStalled(), "Nothing is refined (or lost) at the gate");
		c.setHeavensLeave(true);
		c.addCultivation(0);
		helper.assertTrue(c.getStage() == Stage.LATE, "With the heavens' leave a full Mid steps into Late");
		c.addCultivation(1_000_000);
		helper.assertTrue(c.getRealm() == Realm.QI_REFINING && c.getStage() == Stage.GRAND_PERFECTION && c.isAtBottleneck(),
				"...and runs on to Grand Perfection's tribulation");
		// Anyone already past Qi Refining Mid had the leave; saves keep it, Heaven's Favour and the day of a failed ritual.
		PlayerCultivation past = new PlayerCultivation();
		past.setState(Realm.QI_REFINING, Stage.LATE, 0);
		helper.assertTrue(past.hasHeavensLeave(), "Already past Mid: the leave was had");
		PlayerCultivation mid = new PlayerCultivation();
		mid.setState(Realm.QI_REFINING, Stage.MID, 0);
		mid.setFavour(2);
		mid.setRitualFailedDay(7);
		PlayerCultivation loaded = PlayerCultivation.load(mid.save());
		helper.assertTrue(!loaded.hasHeavensLeave() && loaded.getFavour() == 2 && loaded.getRitualFailedDay() == 7, "Saved and loaded");
		CompoundTag old = mid.save();
		old.remove("HeavensLeave");
		old.remove("RitualFailedDay");
		helper.assertTrue(!PlayerCultivation.load(old).hasHeavensLeave() && PlayerCultivation.load(old).getRitualFailedDay() == -1,
				"A save from before the ritual at Qi Refining Mid still needs it");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void offeringsAndAltarsAreWeighed(GameTestHelper helper) {
		helper.assertTrue(HeavenAltar.Offering.of(0) == HeavenAltar.Offering.UNWORTHY && HeavenAltar.Offering.of(8.9) == HeavenAltar.Offering.UNWORTHY,
				"Under a Mid stone's worth is no offering at all");
		helper.assertTrue(HeavenAltar.Offering.of(9) == HeavenAltar.Offering.PROPER && HeavenAltar.Offering.of(81) == HeavenAltar.Offering.GENEROUS
				&& HeavenAltar.Offering.of(729) == HeavenAltar.Offering.LAVISH && HeavenAltar.Offering.of(6561) == HeavenAltar.Offering.HEAVENLY, "The tiers");
		helper.assertTrue(HeavenAltar.worth(new ItemStack(SpiritStoneItem.of(SpiritStoneItem.Grade.LOW), 9)) == 9
				&& HeavenAltar.worth(new ItemStack(SpiritStoneItem.of(SpiritStoneItem.Grade.MID))) == 9 && HeavenAltar.worth(new ItemStack(Blocks.DIRT, 64)) == 0,
				"Nine Low stones are worth a Mid one; dirt is worth nothing to the heavens");
		helper.assertTrue(Math.abs(HeavenAltar.chance(HeavenAltar.Offering.PROPER, HeavenAltar.Grandeur.HUMBLE, 0) - 0.40) < 1e-9, "Proper at a humble altar: 40%");
		helper.assertTrue(Math.abs(HeavenAltar.chance(HeavenAltar.Offering.PROPER, HeavenAltar.Grandeur.GRAND, 8) - 0.68) < 1e-9, "+20% Grand, +8% height");
		helper.assertTrue(HeavenAltar.chance(HeavenAltar.Offering.HEAVENLY, HeavenAltar.Grandeur.MAGNIFICENT, 15) == HeavenAltar.MAX_CHANCE, "Never above 95%");
		helper.assertTrue(HeavenAltar.chance(HeavenAltar.Offering.UNWORTHY, HeavenAltar.Grandeur.HUMBLE, 0) == HeavenAltar.UNWORTHY_CHANCE
				&& Math.abs(HeavenAltar.chance(HeavenAltar.Offering.UNWORTHY, HeavenAltar.Grandeur.MAGNIFICENT, 15) - HeavenAltar.UNWORTHY_MAX_CHANCE) < 1e-9,
				"With nothing worth offering: half a percent, two at the very most");
		helper.assertTrue(HeavenAltar.Grandeur.of(9) == HeavenAltar.Grandeur.HUMBLE && HeavenAltar.Grandeur.of(10) == HeavenAltar.Grandeur.MODEST
				&& HeavenAltar.Grandeur.of(29) == HeavenAltar.Grandeur.GRAND && HeavenAltar.Grandeur.of(38) == HeavenAltar.Grandeur.MAGNIFICENT, "The grandeurs");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theAltarIsJudgedByWhatIsBuiltRoundIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// On a bare post, nothing round it: humble.
		for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(4, y, 4), Blocks.STONE);
		BlockPos altar = new BlockPos(4, 5, 4);
		helper.setBlock(altar, ModBlocks.HEAVEN_ALTAR);
		HeavenAltar.Judgement bare = HeavenAltar.judge(level, helper.absolutePos(altar));
		helper.assertTrue(bare.sky() && bare.platform() == 0 && bare.grandeur() == HeavenAltar.Grandeur.HUMBLE, "On a bare post it's humble (" + bare + ")");
		helper.setBlock(altar, Blocks.AIR);
		for (int y = 2; y <= 4; y++) helper.setBlock(new BlockPos(4, y, 4), Blocks.AIR);
		floor(helper);
		// A raised 5x5 platform of polished stone on a wider floor, three lanterns, one pillar.
		for (int x = 2; x <= 6; x++) for (int z = 2; z <= 6; z++) helper.setBlock(new BlockPos(x, 2, z), Blocks.POLISHED_ANDESITE);
		altar = new BlockPos(4, 3, 4);
		helper.setBlock(altar, ModBlocks.HEAVEN_ALTAR);
		for (BlockPos l : new BlockPos[] {new BlockPos(2, 3, 2), new BlockPos(6, 3, 2), new BlockPos(2, 3, 6)}) helper.setBlock(l, Blocks.LANTERN);
		for (int y = 3; y <= 5; y++) helper.setBlock(new BlockPos(6, y, 6), Blocks.STONE_BRICKS);
		HeavenAltar.Judgement built = HeavenAltar.judge(level, helper.absolutePos(altar));
		helper.assertTrue(built.platform() == 5 && built.noble() == 6 && built.lights() == 3 && built.pillars() == 1 && built.terraces() == 0,
				"Platform, noble stone, lanterns, a pillar (" + built + ")");
		helper.assertTrue(built.grandeur() == HeavenAltar.Grandeur.MODEST, "15 points: Modest");
		// Roofed over, the heavens can't see it.
		helper.setBlock(new BlockPos(4, 7, 4), Blocks.STONE);
		helper.assertTrue(!HeavenAltar.judge(level, helper.absolutePos(altar)).sky(), "A roof hides it from the heavens");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void favourRaisesEveryBonus(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		double qi = c.maxQi(), gather = c.qiGatherPerSecond(), speed = c.meditationCultivationPerSecond(0);
		c.setFavour(2);
		helper.assertTrue(Math.abs(c.maxQi() / qi - 1.10) < 1e-9 && Math.abs(c.qiGatherPerSecond() / gather - 1.10) < 1e-9
				&& Math.abs(c.meditationCultivationPerSecond(0) / speed - 1.10) < 1e-9, "Favour II: +10% qi, gathering and cultivation");
		c.setFavour(9);
		helper.assertTrue(c.getFavour() == 3, "Favour III at the most");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aPlayersRitualTakesTheOfferingAndTheHeavensAnswer(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			floor(helper);
			BlockPos altarAt = new BlockPos(4, 2, 4);
			helper.setBlock(altarAt, ModBlocks.HEAVEN_ALTAR);
			BlockPos altar = helper.absolutePos(altarAt);
			HeavenAltarBlockEntity tray = (HeavenAltarBlockEntity) helper.getLevel().getBlockEntity(altar);
			ServerPlayer p = player(helper, made, "rite-granted", Stage.MID);
			PlayerCultivation c = CultivationManager.get(p);
			c.addCultivation(1_000_000);
			tray.setItem(0, new ItemStack(SpiritStoneItem.of(SpiritStoneItem.Grade.HIGH))); // Generous
			tray.setItem(1, new ItemStack(Blocks.DIRT, 5)); // worthless, but taken all the same
			HeavenRitual.begin(p, altar);
			helper.assertTrue(HeavenRitual.isActive(p.getUUID()) && tray.isEmpty(), "Begun: the offering is the heavens' now");
			helper.assertTrue(HeavenRitual.isAltarInUse(helper.getLevel(), altar), "Nobody else prays here meanwhile");
			HeavenRitual.finishForTest(p, true);
			helper.assertTrue(c.hasHeavensLeave() && c.getFavour() == 1 && c.getStage() == Stage.LATE,
					"Granted: the leave, Heaven's Favour I for a Generous offering, and on into Late (" + c.getStage() + ")");
			helper.assertTrue(!HeavenRitual.isActive(p.getUUID()), "Over");

			ServerPlayer q = player(helper, made, "rite-denied", Stage.MID);
			PlayerCultivation d = CultivationManager.get(q);
			HeavenRitual.begin(q, altar);
			HeavenRitual.finishForTest(q, false);
			helper.assertTrue(!d.hasHeavensLeave() && d.getRitualFailedDay() == HeavenRitual.today(q.server), "Denied: no leave");
			HeavenRitual.begin(q, altar);
			helper.assertTrue(!HeavenRitual.isActive(q.getUUID()), "...and no new ritual until dawn");

			ServerPlayer r = player(helper, made, "rite-moved", Stage.MID);
			HeavenRitual.begin(r, altar);
			helper.assertTrue(HeavenRitual.isActive(r.getUUID()), "Begun again by another");
			r.teleportTo(r.getX() + 1, r.getY(), r.getZ());
			HeavenRitual.tick(helper.getLevel().getServer());
			helper.assertTrue(!HeavenRitual.isActive(r.getUUID()) && !CultivationManager.get(r).hasHeavensLeave(), "Moving breaks it");

			ServerPlayer s = player(helper, made, "rite-late", Stage.LATE);
			HeavenRitual.begin(s, altar);
			helper.assertTrue(!HeavenRitual.isActive(s.getUUID()), "Nothing to ask of the heavens past Qi Refining Mid");
			helper.succeed();
		} finally {
			for (ServerPlayer p : made) HeavenRitual.forget(p.getUUID());
			for (ServerPlayer p : made) p.server.getPlayerList().remove(p);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sectMembersKneelAtTheAltarOnTheSectsStore(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		Sect sect = new Sect(UUID.randomUUID());
		sect.core = helper.absolutePos(new BlockPos(4, 2, 4));
		sect.barrierRadius = 48;
		sect.alignment = 100;
		sect.capacity = 1;
		sect.posts(Sect.Post.DWELLING).add(helper.absolutePos(new BlockPos(1, 2, 1)));
		SectData.get(level.getServer()).put(sect);
		try {
			CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID));
			Sect.Member m = new Sect.Member(npc.getUUID(), npc.getRank(), 0);
			m.title = NpcTitle.OUTER_DISCIPLE;
			sect.members.put(m.id, m);
			npc.joinSect(sect, 0);
			npc.setTitle(NpcTitle.OUTER_DISCIPLE);
			npc.advance(1_000_000);
			helper.assertTrue(npc.getRank() == PlayerCultivation.RITUAL_RANK && npc.awaitsHeavensLeave(), "It waits at Qi Refining Mid's gate too");
			long now = level.getGameTime();
			NpcLife.live(level, sect, List.of(npc), NpcLife.Phase.DAY, now);
			NpcLife.Activity a = npc.activity();
			helper.assertTrue(a != null && a.kind == NpcLife.Kind.RITUAL && a.spot != null, "It goes to kneel (by the core, this sect having no altar)");
			NpcLife.arrived(npc, a);
			helper.assertTrue(a.ritualStart > 0 && sect.offerings == 0 && Math.abs(a.chance - 0.40) < 0.2,
					"Kneeling: the sect's store pays a Proper offering (" + sect.offerings + " left, chance " + a.chance + ")");
			helper.assertTrue(HeavenRitual.isAltarInUse(level, sect.core), "One at a time");
			NpcLife.answerRitualForTest(npc, 1.0);
			helper.assertTrue(npc.getRank() > PlayerCultivation.RITUAL_RANK && !npc.awaitsHeavensLeave(), "Granted: on into Qi Refining Late");
			// The store: one Proper offering for each member it holds, and one more each day, up to twice that.
			long today = HeavenRitual.today(level.getServer());
			helper.assertTrue(sect.takeOffering(today) == 0, "Spent for today");
			helper.assertTrue(sect.takeOffering(today + 1) == 9 && sect.takeOffering(today + 5) == 9 && sect.offerings == 9,
					"It refills a Proper offering a day, up to twice its stock");
			helper.succeed();
		} finally {
			sect.extinct = true;
			NpcLife.forget(sect.id);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theAltarsScreenRingsItsOfferings(GameTestHelper helper) {
		// Nine offering slots round a ring, none overlapping; the offering weighed from what lies in them; shift-click both ways.
		net.minecraft.world.entity.player.Player player = helper.makeMockPlayer();
		net.minecraft.world.SimpleContainer altar = new net.minecraft.world.SimpleContainer(HeavenAltarBlockEntity.SLOTS);
		HeavenAltarMenu menu = new HeavenAltarMenu(0, player.getInventory(), altar,
				new net.minecraft.world.inventory.SimpleContainerData(HeavenAltarMenu.COUNT));
		for (int i = 0; i < HeavenAltarBlockEntity.SLOTS; i++) {
			int[] a = HeavenAltarMenu.ringSlot(i);
			double r = Math.hypot(a[0] + 8 - HeavenAltarMenu.RING_X, a[1] + 8 - HeavenAltarMenu.RING_Y);
			helper.assertTrue(Math.abs(r - HeavenAltarMenu.RING_RADIUS) < 1.5, "Slot " + i + " sits on the ring");
			for (int j = i + 1; j < HeavenAltarBlockEntity.SLOTS; j++) {
				int[] b = HeavenAltarMenu.ringSlot(j);
				helper.assertTrue(Math.abs(a[0] - b[0]) >= 18 || Math.abs(a[1] - b[1]) >= 18, "Slots " + i + " and " + j + " don't overlap");
			}
		}
		helper.assertTrue(menu.slots.size() == HeavenAltarBlockEntity.SLOTS + 36, "The ring and the player's inventory");
		altar.setItem(3, new ItemStack(SpiritStoneItem.of(SpiritStoneItem.Grade.MID), 2));
		helper.assertTrue(menu.offeringWorth() == 18, "Two Mid stones on the altar: worth 18");
		player.getInventory().setItem(0, new ItemStack(SpiritStoneItem.of(SpiritStoneItem.Grade.HIGH)));
		int hotbar0 = HeavenAltarBlockEntity.SLOTS + 27;
		menu.quickMoveStack(player, hotbar0);
		helper.assertTrue(player.getInventory().getItem(0).isEmpty() && menu.offeringWorth() == 99, "Shift-click lays it on the altar");
		menu.quickMoveStack(player, 3);
		helper.assertTrue(altar.getItem(3).isEmpty() && menu.offeringWorth() == 81, "...and takes it back");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRitualsWordsAreInTheLangFile(GameTestHelper helper) {
		JsonObject lang;
		try (InputStream in = HeavenRitualGameTests.class.getResourceAsStream("/assets/defying-the-heavens/lang/en_us.json")) {
			helper.assertTrue(in != null, "The English lang file is on the classpath");
			lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
		List<String> keys = new ArrayList<>(List.of(ModLang.ALTAR_TOOLTIP, ModLang.ALTAR_TOOLTIP_BEGIN, ModLang.ALTAR_TRAY, ModLang.ALTAR_CHANCE, ModLang.ALTAR_OFFERING, ModLang.ALTAR_WORTH, ModLang.ALTAR_WORTH_NEXT,
				ModLang.ALTAR_ALTAR, ModLang.ALTAR_HEIGHT, ModLang.ALTAR_HEIGHT_Y, ModLang.ALTAR_REWARD, ModLang.ALTAR_REWARD_LEAVE, ModLang.ALTAR_REWARD_FAVOUR,
				ModLang.ALTAR_REWARD_HAVE,
				ModLang.ALTAR_NO_SKY, ModLang.FAVOUR_LINE, ModLang.RITUAL_AHEAD, ModLang.RITUAL_GATE, ModLang.RITUAL_HOW, ModLang.MSG_RITUAL_GATE,
				ModLang.MSG_RITUAL_BEGIN, ModLang.MSG_RITUAL_NOT_NOW, ModLang.MSG_RITUAL_DAWN, ModLang.MSG_RITUAL_ALTAR_BUSY, ModLang.MSG_RITUAL_BUSY,
				ModLang.MSG_RITUAL_MOVED, ModLang.MSG_RITUAL_BROKEN, ModLang.MSG_RITUAL_SUCCESS, ModLang.MSG_RITUAL_FAVOUR, ModLang.MSG_RITUAL_FAIL,
				ModLang.RITUAL_TITLE_SUCCESS, ModLang.RITUAL_TITLE_FAIL, ModLang.HUD_RITUAL, ModBlocks.HEAVEN_ALTAR.getDescriptionId()));
		for (HeavenAltar.Offering o : HeavenAltar.Offering.values()) keys.add(ModLang.ritualOffering(o.id));
		for (HeavenAltar.Grandeur g : HeavenAltar.Grandeur.values()) keys.add(ModLang.ritualGrandeur(g.id));
		for (String part : List.of("platform", "terraces", "noble", "pillars", "lights", "treasures")) keys.add(ModLang.altarPart(part));
		for (String state : List.of("ready", "granted", "not_yet", "dawn", "busy", "no_sky")) keys.add(ModLang.altarState(state));
		for (String key : keys) helper.assertTrue(lang.has(key), "en_us.json has " + key);
		helper.succeed();
	}

	/** A stone floor over the test's ground. */
	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
	}

	/** A survival player at Qi Refining {@code stage}, standing by the altar. */
	private static ServerPlayer player(GameTestHelper helper, List<ServerPlayer> made, String name, Stage stage) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() {
				return false;
			}

			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		made.add(p);
		BlockPos at = helper.absolutePos(new BlockPos(4, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.QI_REFINING, stage, 0);
		CultivationStats.apply(p, c);
		return p;
	}
}
