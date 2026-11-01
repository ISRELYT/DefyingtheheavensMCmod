package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Body Path's other ways (2026-11-01, the user's "Go, all four"): a body manual read by a mortal on the Body Path (and refused one off it),
 * the way practised the only one that tempers (Body Tempering's deeds and meditation nothing under another); a cauldron steeped and soaked in;
 * a beast's blood spilled and bathed in; steps under armour, and none without; blows traded in a spar, and one won.
 */
public class BodyMethodsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void aMortalOnTheBodyPathReadsABodyManualAndItsWayAloneTempers(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-reader", Realm.QI_REFINING);
			PlayerCultivation c = CultivationManager.own(p);
			helper.assertTrue(CultivationMethods.learn(p, CultivationMethod.MEDICINAL_BATH, Realm.HEAVENLY_BEING)
					&& c.methodReach(CultivationMethod.MEDICINAL_BATH) == Realm.HEAVENLY_BEING.ordinal(), "A mortal on the Body Path reads the bath's manual");
			helper.assertTrue(!CultivationMethods.learn(p, CultivationMethod.SPIRIT_HERB_REFINING, Realm.QI_REFINING), "...but no heaven path's: its meridians are sealed");
			ServerPlayer plain = survivor(helper, made, "body-plain");
			CultivationManager.own(plain).setMortal(true);
			helper.assertTrue(!CultivationMethods.learn(plain, CultivationMethod.WEIGHTED_TRAINING, Realm.NASCENT_SOUL), "A mortal off the Body Path reads none of its ways");
			helper.assertTrue(MethodManualItem.kinds(Alignment.Faction.NEUTRAL, null).containsAll(List.of(CultivationMethod.MEDICINAL_BATH,
					CultivationMethod.BEAST_BLOOD_BATHING, CultivationMethod.WEIGHTED_TRAINING, CultivationMethod.HUNDRED_BATTLES)), "Neutral libraries keep all four");
			double before = c.body().cultivation();
			BodyPath.deed(p, 10);
			helper.assertTrue(c.body().cultivation() > before, "Under Body Tempering, a deed tempers");
			helper.assertTrue(CultivationMethods.practise(p, CultivationMethod.MEDICINAL_BATH)
					&& CultivationMethods.practised(c, TwoPaths.Path.BODY) == CultivationMethod.MEDICINAL_BATH, "The bath practised");
			before = c.body().cultivation();
			BodyPath.deed(p, 10);
			helper.assertTrue(c.body().cultivation() == before, "...and a deed tempers nothing now: the deeds are Body Tempering's way");
			helper.assertTrue(CultivationMethods.whyNoGain(p, c, TwoPaths.Path.BODY) != null, "...nor does meditation");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSteepedBathTempersTheBodySoakedInIt(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-bath", Realm.QI_REFINING);
			PlayerCultivation c = CultivationManager.own(p);
			CultivationMethods.learn(p, CultivationMethod.MEDICINAL_BATH, Realm.HEAVENLY_BEING);
			CultivationMethods.practise(p, CultivationMethod.MEDICINAL_BATH);
			BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
			helper.getLevel().setBlockAndUpdate(at, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
			ItemStack herbs = new ItemStack(ModItems.GINSENG, 2);
			helper.assertTrue(MedicinalBath.isHerb(herbs) && !MedicinalBath.isHerb(new ItemStack(ModItems.OTHER_SHORE_FLOWER)), "Ginseng steeps a bath, the Other Shore Flower not");
			helper.assertTrue(MedicinalBath.steep(p, helper.getLevel(), at, herbs) && herbs.getCount() == 1
					&& MedicinalBath.secondsLeft(helper.getLevel(), at) == MedicinalBath.SECONDS_PER_HERB, "A herb steeps it: a minute of soaking");
			double before = c.body().cultivation();
			MedicinalBath.soak(p);
			helper.assertTrue(c.body().cultivation() > before && MedicinalBath.secondsLeft(helper.getLevel(), at) == MedicinalBath.SECONDS_PER_HERB - 1,
					"Standing in it, the body soaks: tempered, a second of the bath spent");
			helper.assertTrue(MedicinalBath.worth(Realm.NASCENT_SOUL, Realm.QI_REFINING) == 4 && MedicinalBath.worth(Realm.QI_REFINING, Realm.NASCENT_SOUL) == 0.125,
					"A bath above the body twice a realm (at most four times), below half a realm (at least an eighth)");
			CultivationMethods.practise(p, CultivationMethod.BODY_TEMPERING);
			before = c.body().cultivation();
			MedicinalBath.soak(p);
			helper.assertTrue(c.body().cultivation() == before, "Under another way, soaking tempers nothing");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	/** The sweep of 2026-11-01: one old herb made the whole bath as strong as itself (an old ginseng and nine weeds, ten minutes of the ginseng). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void eachHerbInABathSoaksAtItsOwnStrength(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			// A Jade Bone body (Nascent Soul's): a young herb (Foundation Building's) a quarter as strong as an old one (Nascent Soul's).
			ServerPlayer p = bodyCultivator(helper, made, "body-bath-herbs", Realm.NASCENT_SOUL);
			PlayerCultivation c = CultivationManager.own(p);
			CultivationMethods.learn(p, CultivationMethod.MEDICINAL_BATH, Realm.HEAVENLY_BEING);
			CultivationMethods.practise(p, CultivationMethod.MEDICINAL_BATH);
			BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
			helper.getLevel().setBlockAndUpdate(at, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
			MedicinalBath.steep(p, helper.getLevel(), at, new ItemStack(ModItems.GINSENG));
			MedicinalBath.steep(p, helper.getLevel(), at, GinsengItem.create(ModItems.GINSENG, 500));
			double before = c.body().cultivation();
			MedicinalBath.soak(p);
			double old = c.body().cultivation() - before;
			for (int i = 1; i < MedicinalBath.SECONDS_PER_HERB; i++) MedicinalBath.soak(p);
			before = c.body().cultivation();
			MedicinalBath.soak(p);
			double young = c.body().cultivation() - before;
			helper.assertTrue(old > 0 && Math.abs(young * 4 - old) < old * 1.0e-6,
					"The old herb's minute soaks first at its own strength, then the young one's at its own (" + old + ", " + young + ")");
			helper.assertTrue(MedicinalBath.secondsLeft(helper.getLevel(), at) == MedicinalBath.SECONDS_PER_HERB - 1, "A second of the young herb's minute spent");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBeastsBloodSpillsAndTheBodyBathesInIt(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-blood", Realm.QI_REFINING);
			PlayerCultivation c = CultivationManager.own(p);
			CultivationMethods.learn(p, CultivationMethod.BEAST_BLOOD_BATHING, Realm.NASCENT_SOUL);
			CultivationMethods.practise(p, CultivationMethod.BEAST_BLOOD_BATHING);
			helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE); // ground under them both, for the blood to lie on
			LivingEntity beast = helper.spawn(ModEntities.beast(BeastKind.IRON_HIDE_BOAR), new BlockPos(1, 2, 1));
			int pools = BeastBlood.pools();
			beast.kill();
			helper.assertTrue(BeastBlood.pools() == pools + 1, "A demon beast's death spills a pool of its blood");
			double before = c.body().cultivation();
			BeastBlood.bathe(p);
			helper.assertTrue(c.body().cultivation() > before, "Standing in it, the body bathes: tempered");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void stepsUnderArmourTemperTheBodyAndStepsWithoutNone(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-weights", Realm.QI_REFINING);
			PlayerCultivation c = CultivationManager.own(p);
			CultivationMethods.learn(p, CultivationMethod.WEIGHTED_TRAINING, Realm.NASCENT_SOUL);
			CultivationMethods.practise(p, CultivationMethod.WEIGHTED_TRAINING);
			helper.assertTrue(WeightedTraining.load(p) == 0, "No armour, no weight");
			double before = c.body().cultivation();
			walk(p, 5);
			helper.assertTrue(c.body().cultivation() == before, "Steps without armour temper nothing");
			p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
			p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
			helper.assertTrue(Math.abs(WeightedTraining.load(p) - 1) < 1.0e-9, "Full iron: a weight of 1 (" + WeightedTraining.load(p) + ")");
			walk(p, 5);
			helper.assertTrue(c.body().cultivation() > before, "Steps under it temper the body");
			before = c.body().cultivation();
			p.setPos(p.getX() + 5, p.getY(), p.getZ());
			WeightedTraining.tick(p.server);
			WeightedTraining.settle(p);
			helper.assertTrue(c.body().cultivation() == before, "...not a leap of five blocks in a tick: no step of its own");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	/** The sweep of 2026-11-01: a stream left to carry one round trained it unattended. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void inWaterOnlySwimmingIsWeightedTraining(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-weights-water", Realm.QI_REFINING);
			PlayerCultivation c = CultivationManager.own(p);
			CultivationMethods.learn(p, CultivationMethod.WEIGHTED_TRAINING, Realm.NASCENT_SOUL);
			CultivationMethods.practise(p, CultivationMethod.WEIGHTED_TRAINING);
			p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
			p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
			helper.setBlock(new BlockPos(1, 2, 1), Blocks.WATER);
			p.baseTick(); // its feet found in the water
			helper.assertTrue(p.isInWater(), "In the water");
			double before = c.body().cultivation();
			walk(p, 5);
			helper.assertTrue(c.body().cultivation() == before, "Carried along in water, not swimming: nothing");
			p.setSwimming(true);
			walk(p, 5);
			helper.assertTrue(c.body().cultivation() > before, "Swimming: tempered");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void blowsTradedInASparTemperTheBodyAndAWinMore(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 1));
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-battles", Realm.FOUNDATION_BUILDING);
			PlayerCultivation c = CultivationManager.own(p);
			CultivationMethods.learn(p, CultivationMethod.HUNDRED_BATTLES, Realm.HEAVENLY_BEING);
			CultivationMethods.practise(p, CultivationMethod.HUNDRED_BATTLES);
			npc.setNoAi(true);
			npc.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			npc.setHealth(npc.getMaxHealth());
			double before = c.body().cultivation();
			HundredBattles.onHurt(npc, p.damageSources().playerAttack(p), 2);
			helper.assertTrue(c.body().cultivation() == before, "Out of a spar, a blow tempers nothing");
			SparSystem.start(p, npc, SparSystem.Terms.FULL);
			helper.assertTrue(SparSystem.areSparring(p, npc), "A spar begins");
			HundredBattles.onHurt(npc, p.damageSources().playerAttack(p), 2);
			double given = c.body().cultivation() - before;
			helper.assertTrue(given > 0, "A blow given in the spar tempers the body");
			HundredBattles.onHurt(p, p.damageSources().mobAttack(npc), 2);
			helper.assertTrue(c.body().cultivation() - before > given, "...and a blow taken");
			before = c.body().cultivation();
			HundredBattles.won(p, npc.fightingRank());
			helper.assertTrue(c.body().cultivation() - before > given * 10, "A spar won: a minute's worth more");
			SparSystem.abandon(p);
			helper.succeed();
		} finally {
			leave(made);
			npc.discard();
		}
	}

	// --- Helpers ---

	/** {@code p} walks {@code steps} ticks a quarter block each, a tick of Weighted Training counted after each; then the second settled. */
	private static void walk(ServerPlayer p, int steps) {
		WeightedTraining.tick(p.server);
		for (int i = 0; i < steps; i++) {
			p.setPos(p.getX() + 0.25, p.getY(), p.getZ());
			WeightedTraining.tick(p.server);
		}
		WeightedTraining.settle(p);
	}

	/** A survival mortal at Peak in the player list, begun on the Body Path and set at {@code realm}'s Early stage on it. */
	private static ServerPlayer bodyCultivator(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm) {
		ServerPlayer p = survivor(helper, made, name);
		PlayerCultivation c = CultivationManager.own(p);
		c.setMortal(true);
		c.setMortalState(MortalStage.PEAK, 0);
		BodyPath.begin(p);
		c.body().set(PlayerCultivation.rank(realm, Stage.EARLY), 0);
		return p;
	}

	/** A survival player in the player list (the helper's mock player is creative), remembered in {@code made}. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		made.add(p);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			SparSystem.abandon(p);
			WeightedTraining.forget(p.getUUID());
			BodyTechniques.forget(p.getUUID());
			BodyPath.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
