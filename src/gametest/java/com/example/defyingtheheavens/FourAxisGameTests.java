package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The four axes of virtue (the user's ask, 2026-10-19; FourAxis, Wishes, Settlements): each Four Axis stage's breakthrough waits on its
 * axis, on either path; wishes of each kind fill their axis, only the one stood at; settlements built with their settlers and wishes;
 * the Astral Realm's way home to the Upper Realm remembered. Then (the user's asks, same day): the dead of Four Axis on the ghost path
 * grant wishes too (a Ghost King in its human form); a Merit wish's threats are many kinds, all to be slain, by one's own hand among
 * others; a settler possessed and cultivated through its wall from within is enlightened.
 */
public class FourAxisGameTests implements FabricGameTest {
	private static ServerPlayer fourAxis(GameTestHelper helper, Stage stage) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.own(player);
		c.setState(Realm.FOUR_AXIS, stage, PlayerCultivation.required(Realm.FOUR_AXIS, stage));
		c.setQi(c.maxQi());
		return player;
	}

	private static CompoundTag wish(FourAxis.Axis axis) {
		CompoundTag wish = new CompoundTag();
		wish.putInt("Axis", axis.ordinal());
		return wish;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theAxesHoldFourAxisInOrder(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION, PlayerCultivation.required(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION));
		helper.assertTrue(!c.isHeldByAxis() && FourAxis.current(c) == null, "Heavenly Being: no axis to hold it");
		c.setState(Realm.FOUR_AXIS, Stage.EARLY, PlayerCultivation.required(Realm.FOUR_AXIS, Stage.EARLY));
		helper.assertTrue(c.isAtBottleneck() && c.isHeldByAxis() && !c.canBreakthrough() && FourAxis.current(c) == FourAxis.Axis.LONGEVITY,
				"Four Axis Early, its bar full: held by the Axis of Longevity");
		c.setAxisProgress(FourAxis.Axis.LONGEVITY, FourAxis.need(c, FourAxis.Axis.LONGEVITY) - 1);
		helper.assertTrue(c.isHeldByAxis(), "A year short: still held");
		c.setAxisProgress(FourAxis.Axis.LONGEVITY, FourAxis.need(c, FourAxis.Axis.LONGEVITY));
		helper.assertTrue(!c.isHeldByAxis() && FourAxis.current(c) == null, "Longevity complete: free to break through, no axis ahead yet");
		c.setState(Realm.FOUR_AXIS, Stage.MID, PlayerCultivation.required(Realm.FOUR_AXIS, Stage.MID));
		helper.assertTrue(c.isHeldByAxis() && FourAxis.current(c) == FourAxis.Axis.FORTUNE, "Mid: the Axis of Fortune");
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, PlayerCultivation.required(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION));
		helper.assertTrue(FourAxis.current(c) == FourAxis.Axis.FORTUNE, "In order: Fortune before any other, even at Grand Perfection");
		for (FourAxis.Axis axis : FourAxis.Axis.values()) c.setAxisProgress(axis, FourAxis.need(c, axis) + 50);
		helper.assertTrue(FourAxis.allComplete(c) && FourAxis.current(c) == null && FourAxis.progress(c, FourAxis.Axis.MERIT) == FourAxis.need(c, FourAxis.Axis.MERIT),
				"All four complete (shown no further than what completes them)");
		helper.assertTrue(PlayerCultivation.load(c.save()).axisProgress(FourAxis.Axis.ENLIGHTENMENT) == FourAxis.need(c, FourAxis.Axis.ENLIGHTENMENT) + 50, "Saved");
		// The ghost path is held by the same axes.
		PlayerCultivation ghostly = new PlayerCultivation();
		ghostly.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		ghostly.netherworld().setMethod(GhostMethod.SIX_PATHS_CANON, 1, 0);
		int rank = PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY);
		GhostMethods.setPath(ghostly.netherworld(), rank);
		ghostly.netherworld().setPath(rank, GhostMethods.required(rank));
		helper.assertTrue(GhostMethods.atBottleneck(ghostly) && GhostMethods.heldByAxis(ghostly) && !GhostMethods.canBreakthrough(ghostly)
				&& FourAxis.current(ghostly) == FourAxis.Axis.LONGEVITY, "The ghost path's Four Axis Early: held by Longevity too");
		ghostly.setAxisProgress(FourAxis.Axis.LONGEVITY, FourAxis.need(ghostly, FourAxis.Axis.LONGEVITY));
		helper.assertTrue(!GhostMethods.heldByAxis(ghostly), "...and freed by it");
		helper.succeed();
	}

	/**
	 * Each axis in its own measure (the user's ask, 2026-10-20): Longevity 500 years, Fortune 200 Low stones' worth, Merit 200 hearts,
	 * Enlightenment 6 stages; the obsession's axis half (rounded up). A save from before, in virtue (100 an axis), comes over as far
	 * along in each measure, one complete still complete.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void eachAxisHasItsMeasure(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		helper.assertTrue(FourAxis.need(c, FourAxis.Axis.LONGEVITY) == 500 && FourAxis.need(c, FourAxis.Axis.FORTUNE) == 200
				&& FourAxis.need(c, FourAxis.Axis.MERIT) == 200 && FourAxis.need(c, FourAxis.Axis.ENLIGHTENMENT) == 6, "500 years, 200 worth, 200 hearts, 6 stages");
		c.setObsession(FourAxis.Axis.ENLIGHTENMENT);
		helper.assertTrue(FourAxis.need(c, FourAxis.Axis.ENLIGHTENMENT) == 3 && FourAxis.need(c, FourAxis.Axis.LONGEVITY) == 500,
				"Its obsession's axis needs half; the others as ever");
		helper.assertTrue(Obsession.need(c, FourAxis.Axis.ENLIGHTENMENT, 5) == 3, "Half rounded up");
		// From before the measures: virtue.
		CompoundTag old = new PlayerCultivation().save();
		old.remove("AxisProgress");
		old.putIntArray("AxisVirtue", new int[] {100, 50, 25, 0});
		PlayerCultivation was = PlayerCultivation.load(old);
		helper.assertTrue(was.axisProgress(FourAxis.Axis.LONGEVITY) == 500 && was.axisProgress(FourAxis.Axis.FORTUNE) == 100
				&& was.axisProgress(FourAxis.Axis.MERIT) == 50 && was.axisProgress(FourAxis.Axis.ENLIGHTENMENT) == 0,
				"Virtue in each measure now: complete, half, a quarter, none");
		old.putString("Obsession", FourAxis.Axis.FORTUNE.getId());
		old.putIntArray("AxisVirtue", new int[] {100, 100, 0, 0});
		was = PlayerCultivation.load(old);
		helper.assertTrue(FourAxis.complete(was, FourAxis.Axis.FORTUNE) && was.axisProgress(FourAxis.Axis.FORTUNE) == 100,
				"The obsession's complete axis: complete at its half");
		helper.assertTrue(PlayerCultivation.load(was.save()).axisProgress(FourAxis.Axis.LONGEVITY) == 500, "Saved in the new measure");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "four_axis_wishes")
	public void wishesFillTheirAxes(GameTestHelper helper) {
		ServerPlayer player = fourAxis(helper, Stage.EARLY);
		PlayerCultivation c = CultivationManager.own(player);
		CultivatorNpc settler = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		settler.becomeRogue(CultivatorNpc.MORTAL, 100, helper.getLevel().getRandom());
		CultivatorNpc student = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 1));
		student.becomeRogue(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 100, helper.getLevel().getRandom());
		player.teleportTo(student.getX() + 1, student.getY(), student.getZ());
		Entity threat = null;
		try {
			// One below Four Axis can't grant it, and is told so.
			ServerPlayer lesser = helper.makeMockServerPlayerInLevel();
			CultivationManager.own(lesser).setState(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION, 0);
			CompoundTag longevity = wish(FourAxis.Axis.LONGEVITY);
			longevity.putInt("Spare", 80);
			settler.setWish(longevity);
			TalkSystem.Reply line = Wishes.reply(lesser, settler, TalkSystem.Topic.WISH.ordinal());
			helper.assertTrue(line != null && line.note() != null && Wishes.actions(lesser, settler).isEmpty(), "Below Four Axis: told why, no grant");

			// Longevity: healed with a quarter of one's qi, then a share of the years taken.
			helper.assertTrue(Wishes.reply(player, settler, TalkSystem.Topic.WISH.ordinal()).note() == null
					&& Wishes.actions(player, settler).size() == 1, "At Longevity: the grant is offered");
			double qi = c.getQi();
			Wishes.act(player, settler, 0);
			helper.assertTrue(settler.wish().getBoolean("Healed") && c.getQi() < qi - c.maxQi() * 0.2, "Healed, at a quarter of the pool");
			helper.assertTrue(Wishes.actions(player, settler).size() == 4, "Then: how many years to take");
			Wishes.act(player, settler, 3);
			helper.assertTrue(c.extraYears() == 40 && c.axisProgress(FourAxis.Axis.LONGEVITY) == 40,
					"Half the 80 years taken: onto the lifespan, and 40 years toward the axis");
			helper.assertTrue(Wishes.open(settler) == null, "The wish granted: none until its time comes round again");

			// A wish of another axis gives nothing.
			CompoundTag fortune = wish(FourAxis.Axis.FORTUNE);
			fortune.putString("Item", "minecraft:bread");
			fortune.putInt("Count", 4);
			settler.setWish(fortune);
			helper.assertTrue(ModLang.WISH_OTHER_AXIS.equals(Wishes.refusal(player, fortune)) && Wishes.actions(player, settler).isEmpty(),
					"Fortune's wish while at Longevity: not theirs to grant");

			// Fortune: what they lack brought, a share of their fortune in turn, marked as shared with the giver.
			c.setAxisProgress(FourAxis.Axis.LONGEVITY, FourAxis.need(c, FourAxis.Axis.LONGEVITY));
			c.setState(Realm.FOUR_AXIS, Stage.MID, 0);
			Wishes.act(player, settler, 0);
			helper.assertTrue(!settler.wish().contains("Done"), "Without the bread: nothing");
			player.getInventory().add(new ItemStack(Items.BREAD, 6));
			Wishes.act(player, settler, 0);
			ItemStack share = ItemStack.EMPTY;
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				if (player.getInventory().getItem(i).is(ModItems.MID_SPIRIT_STONE)) share = player.getInventory().getItem(i);
			}
			helper.assertTrue(player.getInventory().countItem(Items.BREAD) == 2 && share.getCount() >= Wishes.SHARE_STONES
					&& share.getCount() <= Wishes.SHARE_STONES + Wishes.SHARE_MORE && c.axisProgress(FourAxis.Axis.FORTUNE) == 0,
					"Four bread given, Mid stones back; nothing toward the axis until they are absorbed");
			helper.assertTrue(player.getUUID().equals(SpiritStoneItem.fortuneOf(share)) && SpiritStoneItem.fillsFortune(player, share),
					"The share marked as theirs, and it fills their axis");
			// Absorbed (even with the pool full), each counts its worth; stones of no wish, or shared with another, don't.
			player.getInventory().clearContent();
			player.setItemInHand(InteractionHand.MAIN_HAND, share);
			c.setQi(c.maxQi());
			helper.assertTrue(share.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Absorbed with the pool full");
			share.finishUsingItem(helper.getLevel(), player);
			player.stopUsingItem();
			int worth = SpiritStoneItem.Grade.MID.value();
			helper.assertTrue(c.axisProgress(FourAxis.Axis.FORTUNE) == worth, "One Mid stone absorbed: its worth (" + c.axisProgress(FourAxis.Axis.FORTUNE) + ")");
			ItemStack plain = new ItemStack(ModItems.MID_SPIRIT_STONE, 2);
			ItemStack another = SpiritStoneItem.fortune(new ItemStack(ModItems.MID_SPIRIT_STONE, 2), helper.makeMockServerPlayerInLevel());
			c.setQi(0);
			plain.finishUsingItem(helper.getLevel(), player);
			another.finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(c.axisProgress(FourAxis.Axis.FORTUNE) == worth && !SpiritStoneItem.fillsFortune(player, another),
					"Plain stones and another's share: qi only");
			// Spent in meditation, a share counts too.
			ItemStack held = SpiritStoneItem.fortune(new ItemStack(ModItems.MID_SPIRIT_STONE, 1), player);
			player.setItemInHand(InteractionHand.MAIN_HAND, held);
			for (int s = 0; s < SpiritStoneItem.secondsPerStone(SpiritStoneItem.Grade.MID); s++) SpiritStoneItem.drainWhileMeditating(player);
			helper.assertTrue(c.axisProgress(FourAxis.Axis.FORTUNE) == 2 * worth, "Its qi spent meditating: its worth too");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

			// Merit: the threat comes out, and slain by their hand, its hearts fill the axis, and the wish is granted.
			c.setAxisProgress(FourAxis.Axis.FORTUNE, FourAxis.need(c, FourAxis.Axis.FORTUNE));
			c.setState(Realm.FOUR_AXIS, Stage.LATE, 0);
			CompoundTag merit = wish(FourAxis.Axis.MERIT);
			merit.putString("Foe", Wishes.Foe.SORCERER.getId());
			settler.setWish(merit);
			Wishes.act(player, settler, 0);
			ListTag foes = settler.wish().getList("Foes", Tag.TAG_INT_ARRAY);
			threat = foes.size() == 1 ? helper.getLevel().getEntity(NbtUtils.loadUUID(foes.get(0))) : null;
			helper.assertTrue(threat instanceof Evoker && threat.isCurrentlyGlowing(), "A sorcerer comes out of the land, glowing");
			threat.hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
			helper.assertTrue(c.axisProgress(FourAxis.Axis.MERIT) == 12 && Wishes.hearts((LivingEntity) threat) == 12 && settler.wish().contains("Done"),
					"Slain by their hand: an evoker's 12 hearts (" + c.axisProgress(FourAxis.Axis.MERIT) + ")");

			// Enlightenment: a minute in meditation beside the one guided, and it breaks through.
			c.setAxisProgress(FourAxis.Axis.MERIT, FourAxis.need(c, FourAxis.Axis.MERIT));
			c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
			student.setWish(wish(FourAxis.Axis.ENLIGHTENMENT));
			int before = student.getRank();
			Wishes.act(player, student, 0);
			helper.assertTrue(!Wishes.guide(player, student), "Not meditating: no second counts");
			player.setOnGround(true);
			MeditationManager.start(player);
			for (int s = 0; s < Wishes.GUIDE_SECONDS; s++) Wishes.guide(player, student);
			helper.assertTrue(student.getRank() == before + 1 && c.axisProgress(FourAxis.Axis.ENLIGHTENMENT) == 1,
					"Guided a minute: a stage up, one stage toward the axis (meditating " + MeditationManager.isMeditating(player.getUUID()) + ", rank "
							+ before + " -> " + student.getRank() + ", axis " + c.axisProgress(FourAxis.Axis.ENLIGHTENMENT) + ", wish " + student.wish()
							+ ", apart " + player.distanceTo(student) + ")");
			helper.assertTrue(FourAxis.add(player, FourAxis.Axis.MERIT, 50) == 0, "Toward an axis behind: nothing");
		} finally {
			MeditationManager.stop(player, false);
			if (threat != null) threat.discard();
			Wishes.forget(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aWishTakenOnAndLeftLapses(GameTestHelper helper) {
		ServerPlayer player = fourAxis(helper, Stage.GRAND_PERFECTION);
		PlayerCultivation c = CultivationManager.own(player);
		for (FourAxis.Axis axis : new FourAxis.Axis[] {FourAxis.Axis.LONGEVITY, FourAxis.Axis.FORTUNE, FourAxis.Axis.MERIT}) {
			c.setAxisProgress(axis, FourAxis.need(c, axis));
		}
		CultivatorNpc student = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		student.becomeRogue(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 100, helper.getLevel().getRandom());
		student.setWish(wish(FourAxis.Axis.ENLIGHTENMENT));
		Wishes.act(player, student, 0);
		helper.assertTrue(Wishes.actions(player, student).isEmpty(), "Taken on: no grant offered again");
		Wishes.forget(player.getUUID()); // as a restart would
		helper.assertTrue(Wishes.actions(player, student).size() == 1 && !student.wish().hasUUID("Accepted"), "The guidance forgotten: offered afresh");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "four_axis_settlement", timeoutTicks = 100)
	public void aSettlementIsBuilt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Off to the side, so nothing it builds or digs touches the test's own structure block (at its corner); on a stone floor of its
		// own, since the test world under the tests may be sea.
		BlockPos centre = helper.absolutePos(new BlockPos(24, 1, 24));
		int ground = centre.getY();
		// What was there, to put back after (the game test world is kept).
		Map<BlockPos, BlockState> before = new HashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-20, 0, -20).atY(ground - 10), centre.offset(20, 0, 20).atY(ground + 10))) {
			before.put(pos.immutable(), level.getBlockState(pos));
		}
		List<CultivatorNpc> settlers = List.of();
		try {
			for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-20, 0, -20).atY(ground - 1), centre.offset(20, 0, 20).atY(ground + 10))) {
				level.setBlock(pos, pos.getY() == ground - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
			}
			// Over water: no settlement.
			BlockPos wet = new BlockPos(centre.getX() + 15, ground - 1, centre.getZ() + 15);
			level.setBlock(wet, Blocks.WATER.defaultBlockState(), 2);
			helper.assertTrue(Settlements.build(level, wet.getX(), wet.getZ(), ground + 10, Settlements.OAK, RandomSource.create(1)) == null, "Water: none");
			level.setBlock(wet, Blocks.STONE.defaultBlockState(), 2);

			Settlements.Built built = Settlements.build(level, centre.getX(), centre.getZ(), ground + 10, Settlements.OAK, RandomSource.create(42));
			helper.assertTrue(built != null && !built.name().isEmpty() && built.well().getY() == ground, "Built on the floor, and named");
			settlers = built.settlers();
			BlockPos well = built.well();
			helper.assertTrue(level.getBlockState(well.below()).is(Blocks.WATER) && level.getBlockState(well.east()).is(Blocks.STONE_BRICKS),
					"A well of water in a stone ring");
			int logs = 0;
			for (BlockPos pos : before.keySet()) if (level.getBlockState(pos).is(Blocks.OAK_LOG)) logs++;
			helper.assertTrue(logs >= Settlements.HUTS * 4 * 3, "Four huts' log corners (" + logs + ")");
			helper.assertTrue(settlers.size() >= Settlements.MIN_SETTLERS && settlers.size() <= Settlements.MAX_SETTLERS, "Five to eight settlers");
			List<BlockPos> huts = Settlements.huts(level, well);
			helper.assertTrue(huts.size() == Settlements.HUTS, "Its four huts found by their lanterns (" + huts + ")");
			for (CultivatorNpc npc : settlers) {
				helper.assertTrue(well.equals(npc.settledAt()) && npc.wish() != null && npc.isPersistenceRequired() && npc.getAlignment() >= 0
						&& npc.getRank() <= PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION),
						"Each settled at the well, with a wish, kept, of the righteous path or none, no stronger than Core Formation");
				FourAxis.Axis axis = Wishes.axisOf(npc.wish());
				helper.assertTrue(axis != null && (!npc.isMortal() || axis != FourAxis.Axis.ENLIGHTENMENT), "A mortal wishes for no guidance");
				BlockPos shelter = Settlements.shelterFor(npc);
				helper.assertTrue(huts.contains(shelter) && level.getBlockState(shelter).isAir() && level.getBlockState(shelter.below()).isSolid(),
						"Each shelters inside one of the huts, on its floor");
			}
		} finally {
			for (CultivatorNpc npc : settlers) npc.discard();
			before.forEach((pos, state) -> {
				if (level.getBlockState(pos) != state) level.setBlock(pos, state, 2);
			});
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sitesLieOnTheWorldsLand(GameTestHelper helper) {
		AstralPlanets planets = AstralPlanets.get();
		helper.assertTrue(planets != null, "The planets are worked out");
		AstralPlanets.Planet world = planets.planets().stream().filter(p -> !p.isOverworld() && Settlements.habitable(p.kind())).findFirst().orElseThrow();
		int cx0 = Math.floorDiv(world.landX(), Settlements.CELL), cz0 = Math.floorDiv(world.landZ(), Settlements.CELL), sites = 0;
		for (int cx = cx0 - 10; cx < cx0 + 10; cx++) {
			for (int cz = cz0 - 10; cz < cz0 + 10; cz++) {
				Settlements.Site site = Settlements.site(planets, cx, cz);
				if (site == null) continue;
				sites++;
				helper.assertTrue(site.planet().equals(world) && Math.floorDiv(site.x(), Settlements.CELL) == cx
						&& Math.floorDiv(site.z(), Settlements.CELL) == cz, "Each in its own square, on the world");
				Settlements.Site again = Settlements.site(planets, cx, cz);
				helper.assertTrue(again != null && again.x() == site.x() && again.z() == site.z(), "The same every time");
			}
		}
		helper.assertTrue(sites > 100 && sites < 300, "About every other square of a living world (" + sites + " of 400)");
		AstralPlanets.Planet dead = planets.planets().stream().filter(p -> p.kind() == AstralPlanets.Kind.BARREN).findFirst().orElse(null);
		if (dead != null) {
			int dx = Math.floorDiv(dead.landX(), Settlements.CELL), dz = Math.floorDiv(dead.landZ(), Settlements.CELL);
			for (int cx = dx - 3; cx < dx + 3; cx++) {
				for (int cz = dz - 3; cz < dz + 3; cz++) helper.assertTrue(Settlements.site(planets, cx, cz) == null, "None on a barren world");
			}
		}
		// Built sites saved and loaded.
		Settlements.SettlementData data = new Settlements.SettlementData();
		data.put(7L, new Settlements.Settlement(new BlockPos(5, 70, 9), "Jade Spring Hamlet", 6, 1234L));
		data.put(8L, new Settlements.Settlement(new BlockPos(900, 0, 9), "", 0, -1));
		Settlements.SettlementData loaded = Settlements.SettlementData.load(data.save(new CompoundTag()));
		helper.assertTrue(loaded.get(7L).built() && loaded.get(7L).name().equals("Jade Spring Hamlet") && !loaded.get(8L).built()
				&& loaded.nearest(0, 0).size() == 1, "Saved: the built one and the empty one");
		helper.assertTrue(loaded.get(7L).people() == 6 && loaded.get(7L).shortSince() == 1234L, "With its people, and since when it is short of them");
		// The Astral Realm remembers a departure from the Upper Realm.
		AstralRealm.AstralData astral = new AstralRealm.AstralData();
		UUID id = UUID.randomUUID();
		astral.setDeparture(id, 12, -40, true);
		double[] back = AstralRealm.AstralData.load(astral.save(new CompoundTag())).departure(id);
		helper.assertTrue(back != null && back[0] == 12 && back[1] == -40 && back[2] == 1, "Rose out of the Upper Realm: comes home to it");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadOfFourAxisGrantWishes(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc settler = null;
		try {
			// A Ghost King of Four Axis on the ghost path (its heaven path at Qi Refining), and one of the common dead whose Four Axis is
			// the heaven path's alone.
			ServerPlayer king = survivor(helper, made, "dead-king");
			PlayerCultivation c = CultivationManager.own(king);
			c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
			c.netherworld().setMethod(GhostMethod.SIX_PATHS_CANON, 1, 0);
			GhostMethods.setPath(c.netherworld(), PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY));
			ServerPlayer shade = survivor(helper, made, "dead-shade");
			CultivationManager.own(shade).setState(Realm.FOUR_AXIS, Stage.EARLY, 0);
			GhostSystem.makeGhost(king);
			GhostSystem.makeGhost(shade);
			helper.assertTrue(GhostSystem.isKing(king) && AstralRealm.canBearTheVoid(king), "A Ghost King of Four Axis: the void bears it");
			helper.assertTrue(!GhostSystem.isKing(shade) && !AstralRealm.canBearTheVoid(shade), "One of the common dead: not");

			settler = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
			settler.becomeRogue(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 100, helper.getLevel().getRandom());
			CompoundTag longevity = wish(FourAxis.Axis.LONGEVITY);
			longevity.putInt("Spare", 60);
			settler.setWish(longevity);
			helper.assertTrue(ModLang.WISH_NOT_FOUR_AXIS_DEAD.equals(Wishes.refusal(shade, longevity)),
					"The dead grant by the ghost path: a heaven path of Four Axis alone won't do");
			helper.assertTrue(!settler.senses(king) && ModLang.TALK_UNSEEN_KING.equals(TalkSystem.refusal(king, settler)),
					"In its true form the living don't see it, and it is told how they would");
			c.setAbilityEnabled(Ability.FORM_SHIFT, true);
			helper.assertTrue(TwoPaths.inHumanForm(king) && settler.senses(king) && TalkSystem.refusal(king, settler) == null,
					"In its human form the settler sees it and talks");
			TalkSystem.Reply line = Wishes.reply(king, settler, TalkSystem.Topic.WISH.ordinal());
			helper.assertTrue(line != null && line.note() == null && Wishes.actions(king, settler).size() == 1, "At its Axis of Longevity: the grant is offered");
			c.netherworld().setGhostQi(GhostQi.poolMax(c));
			c.setQi(c.maxQi());
			double heavenQi = c.getQi(), ghostQi = c.netherworld().ghostQi();
			Wishes.act(king, settler, 0);
			helper.assertTrue(settler.wish().getBoolean("Healed") && c.netherworld().ghostQi() < ghostQi && c.getQi() == heavenQi,
					"Healed out of the ghost path's pool (" + ghostQi + " -> " + c.netherworld().ghostQi() + ")");
			Wishes.act(king, settler, 4);
			helper.assertTrue(c.extraYears() == 60 && c.axisProgress(FourAxis.Axis.LONGEVITY) == 60, "All 60 years onto its lifespan, and toward its axis");
		} finally {
			for (ServerPlayer p : made) GhostSystem.forget(p.getUUID());
			leave(made);
			if (settler != null) settler.discard();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "four_axis_foes")
	public void aThreatMustAllFallAndByTheirHand(GameTestHelper helper) {
		ServerPlayer player = fourAxis(helper, Stage.LATE);
		PlayerCultivation c = CultivationManager.own(player);
		c.setAxisProgress(FourAxis.Axis.LONGEVITY, FourAxis.need(c, FourAxis.Axis.LONGEVITY));
		c.setAxisProgress(FourAxis.Axis.FORTUNE, FourAxis.need(c, FourAxis.Axis.FORTUNE));
		CultivatorNpc settler = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		settler.becomeRogue(CultivatorNpc.MORTAL, 100, helper.getLevel().getRandom());
		GameRules.BooleanValue loot = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
		boolean looting = loot.get();
		loot.set(false, helper.getLevel().getServer()); // nothing the slain drop left about the kept world
		List<Entity> made = new ArrayList<>();
		try {
			CompoundTag raid = wish(FourAxis.Axis.MERIT);
			raid.putString("Foe", Wishes.Foe.RAIDERS.getId());
			settler.setWish(raid);
			Wishes.act(player, settler, 0);
			List<Entity> raiders = foes(helper, settler);
			made.addAll(raiders);
			helper.assertTrue(raiders.size() == 4 && raiders.stream().allMatch(Entity::isCurrentlyGlowing), "Two pillagers and two vindicators, glowing");
			// Spoils only to the one who took it on: slain by anything else, nothing (its wish open again, an evoker's totem endless).
			LivingEntity raider = (LivingEntity) raiders.get(0);
			helper.assertTrue(!Wishes.spoilsDrop(raider, raider.damageSources().genericKill())
					&& Wishes.spoilsDrop(raider, player.damageSources().playerAttack(player))
					&& Wishes.spoilsDrop(settler, settler.damageSources().genericKill()), "A threat's spoils are its taker's alone; anything else's as ever");
			raiders.get(0).kill();
			helper.assertTrue(settler.wish().getList("Foes", Tag.TAG_INT_ARRAY).size() == 3 && !settler.wish().getBoolean("Slain")
					&& c.axisProgress(FourAxis.Axis.MERIT) == 0, "One falls, not by their hand: three left, no hearts to their axis");
			int hearts = 0;
			for (int i = 1; i < raiders.size(); i++) {
				hearts += Wishes.hearts((LivingEntity) raiders.get(i));
				raiders.get(i).hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
			}
			helper.assertTrue(settler.wish().contains("Done") && c.axisProgress(FourAxis.Axis.MERIT) == hearts && hearts == 36,
					"All fallen, their own hand among the slayers: granted, the hearts of the three they slew (" + c.axisProgress(FourAxis.Axis.MERIT) + ")");

			// A brood that dies by other hands alone: the wish stands, to be taken on again.
			CompoundTag brood = wish(FourAxis.Axis.MERIT);
			brood.putString("Foe", Wishes.Foe.BROOD.getId());
			settler.setWish(brood);
			Wishes.act(player, settler, 0);
			List<Entity> spiders = foes(helper, settler);
			made.addAll(spiders);
			helper.assertTrue(spiders.size() == 5, "Three spiders and two cave spiders");
			for (Entity spider : spiders) spider.kill();
			helper.assertTrue(!settler.wish().contains("Done") && !settler.wish().hasUUID("Accepted") && c.axisProgress(FourAxis.Axis.MERIT) == hearts,
					"All dead, none by their hand: no hearts, and the wish open again");

			// Left a day: it lapses, and what of the threat is still about goes with it.
			CompoundTag pack = wish(FourAxis.Axis.MERIT);
			pack.putString("Foe", Wishes.Foe.PACK.getId());
			settler.setWish(pack);
			Wishes.act(player, settler, 0);
			List<Entity> wolves = foes(helper, settler);
			made.addAll(wolves);
			helper.assertTrue(wolves.size() == 4 && wolves.get(0) instanceof Wolf wolf && player.getUUID().equals(wolf.getPersistentAngerTarget()),
					"Four wolves, angry at the one who took them on");
			settler.wish().putLong("AcceptedAt", helper.getLevel().getGameTime() - Wishes.ACCEPT_TICKS - 1);
			helper.assertTrue(Wishes.open(settler) != null && !settler.wish().hasUUID("Accepted") && wolves.stream().allMatch(Entity::isRemoved),
					"A day gone: lapsed, and the wolves gone with it");
		} finally {
			for (Entity e : made) gone(e);
			settler.discard();
			loot.set(looting, helper.getLevel().getServer());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "four_axis_foes")
	public void aThreatsSpoilsAreItsTakersAlone(GameTestHelper helper) {
		ServerPlayer player = fourAxis(helper, Stage.LATE);
		PlayerCultivation c = CultivationManager.own(player);
		c.setAxisProgress(FourAxis.Axis.LONGEVITY, FourAxis.need(c, FourAxis.Axis.LONGEVITY));
		c.setAxisProgress(FourAxis.Axis.FORTUNE, FourAxis.need(c, FourAxis.Axis.FORTUNE));
		CultivatorNpc settler = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		settler.becomeRogue(CultivatorNpc.MORTAL, 100, helper.getLevel().getRandom());
		GameRules.BooleanValue loot = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
		boolean looting = loot.get();
		loot.set(true, helper.getLevel().getServer()); // the spoils are the question here (picked up again after)
		List<Entity> made = new ArrayList<>();
		List<BlockPos> fell = new ArrayList<>();
		try {
			CompoundTag sorcerer = wish(FourAxis.Axis.MERIT);
			sorcerer.putString("Foe", Wishes.Foe.SORCERER.getId());
			settler.setWish(sorcerer);
			Wishes.act(player, settler, 0);
			Entity first = foes(helper, settler).get(0);
			made.add(first);
			fell.add(first.blockPosition());
			int before = totems(helper, first.blockPosition());
			first.kill(); // by nothing of the taker's
			helper.assertTrue(totems(helper, fell.get(0)) == before && !settler.wish().contains("Done"),
					"An evoker of the threat slain by something else: no totem, and the wish open again");
			Wishes.act(player, settler, 0);
			Entity second = foes(helper, settler).get(0);
			made.add(second);
			fell.add(second.blockPosition());
			before = totems(helper, second.blockPosition());
			second.hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
			helper.assertTrue(totems(helper, fell.get(1)) == before + 1 && settler.wish().contains("Done"), "Slain by their hand: its totem, and granted");
		} finally {
			for (Entity e : made) gone(e);
			for (BlockPos at : fell) {
				for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(4))) item.discard();
			}
			settler.discard();
			loot.set(looting, helper.getLevel().getServer());
		}
		helper.succeed();
	}

	/**
	 * A Merit threat out keeps its settlement's people home (the user's ask, 2026-10-20): the alarm raised with it, till it lapses; each
	 * settler's shelter its hut; all of it slain, the alarm ends. (Their going home is a goal of the Astral Realm's settlers alone:
	 * CultivatorGoals.ShelterGoal.)
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "four_axis_foes")
	public void aThreatOutKeepsItsPeopleHome(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = fourAxis(helper, Stage.LATE);
		PlayerCultivation c = CultivationManager.own(player);
		c.setAxisProgress(FourAxis.Axis.LONGEVITY, FourAxis.need(c, FourAxis.Axis.LONGEVITY));
		c.setAxisProgress(FourAxis.Axis.FORTUNE, FourAxis.need(c, FourAxis.Axis.FORTUNE));
		BlockPos well = helper.absolutePos(new BlockPos(1, 2, 1));
		// A hut: its floor's middle open, a lantern hung two above it.
		BlockPos hut = helper.absolutePos(new BlockPos(4, 2, 4));
		Map<BlockPos, BlockState> before = new HashMap<>();
		for (BlockPos pos : List.of(hut.below(), hut, hut.above(), hut.above(2))) before.put(pos.immutable(), level.getBlockState(pos));
		GameRules.BooleanValue loot = level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
		boolean looting = loot.get();
		loot.set(false, level.getServer());
		List<Entity> made = new ArrayList<>();
		try {
			level.setBlock(hut.below(), Blocks.STONE.defaultBlockState(), 2);
			level.setBlock(hut, Blocks.AIR.defaultBlockState(), 2);
			level.setBlock(hut.above(), Blocks.AIR.defaultBlockState(), 2);
			level.setBlock(hut.above(2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
			CultivatorNpc settler = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 2));
			CultivatorNpc neighbour = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(2, 2, 1));
			made.add(settler);
			made.add(neighbour);
			for (CultivatorNpc npc : List.of(settler, neighbour)) {
				npc.becomeRogue(CultivatorNpc.MORTAL, 100, level.getRandom());
				npc.setSettledAt(well);
			}
			long now = level.getGameTime();
			helper.assertTrue(!Settlements.alarmed(well, now), "Nothing out: no alarm");
			helper.assertTrue(Settlements.huts(level, well).contains(hut) && hut.equals(Settlements.shelterFor(neighbour)), "Its hut found: its people's shelter");
			CompoundTag pack = wish(FourAxis.Axis.MERIT);
			pack.putString("Foe", Wishes.Foe.PACK.getId());
			settler.setWish(pack);
			Wishes.act(player, settler, 0);
			List<Entity> wolves = foes(helper, settler);
			made.addAll(wolves);
			helper.assertTrue(wolves.size() == 4 && Settlements.alarmed(well, now) && Wishes.threatUntil(settler) == now + Wishes.ACCEPT_TICKS,
					"The pack out: its settlement alarmed until the wish lapses");
			helper.assertTrue(!Settlements.alarmed(well, now + Wishes.ACCEPT_TICKS + 1), "...and no longer once it has");
			Settlements.raiseAlarm(settler, Wishes.threatUntil(settler)); // as its settler raises it again (a restart forgets it)
			Settlements.raiseAlarm(neighbour, now - 1);
			helper.assertTrue(Settlements.alarmed(well, now), "Raised again by the one whose threat it is (a lapsed one beside it counts for nothing)");
			for (Entity wolf : wolves) wolf.hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
			helper.assertTrue(settler.wish().contains("Done") && !Settlements.alarmed(well, now), "All of it slain: its people come out again");
		} finally {
			for (Entity e : made) gone(e);
			before.forEach((pos, state) -> level.setBlock(pos, state, 2));
			loot.set(looting, level.getServer());
		}
		helper.succeed();
	}

	/** Totems of Undying lying within 4 blocks of {@code at}. */
	private static int totems(GameTestHelper helper, BlockPos at) {
		int count = 0;
		for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(4))) {
			if (item.getItem().is(Items.TOTEM_OF_UNDYING)) count += item.getItem().getCount();
		}
		return count;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "four_axis_foes")
	public void everyFoeComesOutOfTheLand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		List<Entity> made = new ArrayList<>();
		try {
			for (Wishes.Foe foe : Wishes.Foe.values()) {
				List<LivingEntity> foes = Wishes.summon(level, foe, at, RandomSource.create(foe.ordinal()));
				made.addAll(foes);
				helper.assertTrue(!foes.isEmpty() && foes.stream().allMatch(e -> e.isAlive() && e.level() == level) && Wishes.Foe.byId(foe.getId()) == foe,
						foe.getId() + ": it comes, in the world");
				switch (foe) {
					case RIDER -> helper.assertTrue(foes.size() == 2 && foes.get(1).getVehicle() == foes.get(0), "A pillager riding the ravager");
					case HORSEMEN -> helper.assertTrue(foes.size() == 3
							&& foes.stream().allMatch(e -> e instanceof Skeleton && e.getVehicle() instanceof SkeletonHorse), "Three skeletons on skeleton horses");
					case UNDEAD -> helper.assertTrue(foes.size() == 6 && foes.stream().allMatch(e -> e.getItemBySlot(EquipmentSlot.HEAD).hasTag()
							&& e.getItemBySlot(EquipmentSlot.HEAD).getTag().getBoolean("Unbreakable")) && foes.stream().anyMatch(e -> e instanceof Zombie),
							"Six of the dead, helmed against the sun");
					case DEMONIC -> helper.assertTrue(foes.size() == 1 && foes.get(0) instanceof CultivatorNpc demon && demon.getAlignment() < Alignment.DEMONIC_BELOW
							&& demon.getRank() >= Wishes.DEMONIC_LOW && demon.getRank() <= Wishes.DEMONIC_HIGH, "A cultivator of the demonic path, Nascent Soul to Heavenly Being");
					default -> { }
				}
			}
		} finally {
			for (Entity e : made) gone(e);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aPossessedSettlerIsEnlightenedFromWithin(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc npc = null;
		try {
			ServerPlayer soul = projected(helper, made, "within-soul", Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
			PlayerCultivation own = CultivationManager.own(soul);
			for (FourAxis.Axis axis : new FourAxis.Axis[] {FourAxis.Axis.LONGEVITY, FourAxis.Axis.FORTUNE, FourAxis.Axis.MERIT}) {
				own.setAxisProgress(axis, FourAxis.need(own, axis));
			}
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.becomeRogue(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 100, helper.getLevel().getRandom());
			npc.moveTo(soul.getX() + 1, soul.getY(), soul.getZ(), 0, 0);
			npc.setWish(wish(FourAxis.Axis.ENLIGHTENMENT));
			helper.assertTrue(SoulProjection.tryPossess(soul, npc) == InteractionResult.SUCCESS, "The soul takes the settler's body");
			helper.assertTrue(!Wishes.guideWithin(soul, npc), "Nothing yet: it hasn't broken through");
			PlayerCultivation body = CultivationManager.get(soul);
			body.addCultivation(body.cultivationRequired() + 3);
			helper.assertTrue(body.getStage() == Stage.MID && Wishes.guideWithin(soul, npc), "Cultivated through its wall from within: granted");
			helper.assertTrue(own.axisProgress(FourAxis.Axis.ENLIGHTENMENT) == 1 && npc.wish().contains("Done"), "A stage guided, to the soul's own axis");
			SoulProjection.toggle(soul);
			helper.assertTrue(npc.getRank() == PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), "Given back, the settler keeps the stage it came to");
		} finally {
			leave(made);
			if (npc != null) npc.discard();
		}
		helper.succeed();
	}

	// --- Helpers ---

	/** The creatures of {@code settler}'s threat still in the world. */
	private static List<Entity> foes(GameTestHelper helper, CultivatorNpc settler) {
		List<Entity> found = new ArrayList<>();
		for (Tag id : settler.wish().getList("Foes", Tag.TAG_INT_ARRAY)) {
			Entity e = helper.getLevel().getEntity(NbtUtils.loadUUID(id));
			if (e != null) found.add(e);
		}
		return found;
	}

	/** {@code e} out of the kept world, with whatever it rides and whatever rides it (a mount, a jockey). */
	private static void gone(Entity e) {
		Entity root = e.getRootVehicle();
		for (Entity passenger : root.getIndirectPassengers()) if (!passenger.isRemoved()) passenger.discard();
		if (!root.isRemoved()) root.discard();
	}

	/** A survival player in the player list (the helper's mock player is creative, and creative ghosts keep no rule of the dead). */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		made.add(p);
		return p;
	}

	/** A cultivator of {@code realm} and {@code stage} with its soul already out, full of qi (as PossessionGameTests'). */
	private static ServerPlayer projected(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm, Stage stage) {
		ServerPlayer p = survivor(helper, made, name);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, stage, 0);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		c.setQi(c.maxQi());
		long now = helper.getLevel().getGameTime();
		SoulProjection.toggle(p);
		for (int i = 0; i < SoulProjection.CHANNEL_TICKS; i++) SoulProjection.tick(p, now);
		helper.assertTrue(SoulProjection.isSoul(p), "The soul is out");
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) if (Possession.inhabits(p.getUUID())) SoulProjection.letGo(p, null);
		for (ServerPlayer p : made) {
			SoulProjection.onDeath(p);
			CultivationManager.own(p).setPossession(null);
			p.server.getPlayerList().remove(p);
		}
	}
}
