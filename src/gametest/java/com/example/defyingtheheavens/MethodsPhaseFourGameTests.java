package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The methods overhaul, phase 4 (the user's design of 2026-10-31, built on their go-ahead): the ways of one realm and of a few. The heart
 * demon tempered; the places' meditations (a Dao field, the primeval dragon's bones, a primordial beast, immortal essence burnt); rifts
 * walked and vessels nurtured; the Shore of Nirvana risen from; the Yellow Springs, the Wheel's errand, a rift's souls, the soul furnace,
 * ghosts slain and taken in; the blood burnt and remade; whose chests keep which manuals; devilish cultivators draining the dead.
 */
public class MethodsPhaseFourGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6 * Math.max(1, Math.abs(b));
	}

	private static String key(Component message) {
		return message != null && message.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theHeartDemonIsTemperedAgainAndAgain(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "heart-tempering");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			TestMethods.setAt(c, Realm.NASCENT_SOUL, Stage.EARLY);
			helper.assertTrue(!HeartDemonTempering.tempers(p), "Qi Circulation: no tempering");
			CultivationMethods.learn(p, CultivationMethod.HEART_DEMON_TEMPERING, Realm.NASCENT_SOUL);
			CultivationMethods.practise(p, CultivationMethod.HEART_DEMON_TEMPERING);
			helper.assertTrue(HeartDemonTempering.tempers(p), "Practised at Nascent Soul: the heart demon called up again and again");
			helper.assertTrue(key(CultivationMethods.whyNoGain(p, c, TwoPaths.Path.HEAVEN)).equals(ModLang.MSG_METHOD_NOT_BY_MEDITATION), "Meditation itself gathers nothing");
			HeartDemonTempering.won(p);
			helper.assertTrue(near(c.getCultivation(), HeartDemonTempering.WIN * c.cultivationRequired()), "A heart demon slain: a tenth of the stage");
			HeartDemonTempering.won(p);
			HeartDemonTempering.lost(p);
			helper.assertTrue(near(c.getCultivation(), (2 * HeartDemonTempering.WIN - HeartDemonTempering.LOSS) * c.cultivationRequired()), "A defeat: a tenth lost");
			for (int i = 0; i < 3; i++) HeartDemonTempering.lost(p);
			helper.assertTrue(c.getCultivation() == 0 && c.getRealm() == Realm.NASCENT_SOUL && c.getStage() == Stage.EARLY, "Never below the stage's start");
			TestMethods.setAt(c, Realm.HEAVENLY_BEING, Stage.EARLY);
			helper.assertTrue(!HeartDemonTempering.tempers(p), "Nascent Soul's alone: at Heavenly Being it holds no volume");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void thePlacesWaysGatherOnlyInTheirPlaces(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		ServerLevel secret = level.getServer().getLevel(ModDimensions.SECRET_REALM);
		BlockPos bones = new BlockPos(SecretRealm.SLOT_SPACING * 3 + 150, 200, 150);
		try {
			helper.assertTrue(MethodGrounds.fieldFits(Stage.EARLY, QiElement.WOOD) && !MethodGrounds.fieldFits(Stage.EARLY, QiElement.FIRE)
					&& MethodGrounds.fieldFits(Stage.MID, QiElement.FIRE) && MethodGrounds.fieldFits(Stage.LATE, QiElement.EARTH)
					&& MethodGrounds.fieldFits(Stage.GRAND_PERFECTION, QiElement.METAL) && MethodGrounds.fieldFits(Stage.GRAND_PERFECTION, QiElement.WATER)
					&& !MethodGrounds.fieldFits(Stage.LATE, null), "Each stage's Dao field: wood, fire, earth, then metal or water");
			ServerPlayer p = survivor(helper, made, "dao-field");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			TestMethods.setAt(c, Realm.DAO_INTEGRATION, Stage.EARLY);
			CultivationMethods.learn(p, CultivationMethod.DAO_FIELD_ATTUNEMENT, Realm.DAO_INTEGRATION);
			CultivationMethods.practise(p, CultivationMethod.DAO_FIELD_ATTUNEMENT);
			helper.assertTrue(key(CultivationMethods.whyNoGain(p, c, TwoPaths.Path.HEAVEN)).equals(ModLang.MSG_DAO_FIELD_OUTSIDE), "Out of the Great Void's fields: nothing");
			ServerLevel greatVoid = level.getServer().getLevel(ModDimensions.GREAT_VOID);
			Pair<BlockPos, Holder<Biome>> wood = greatVoid.findClosestBiome3d(b -> b.is(ModBiomes.DAO_FIELD_WOOD), new BlockPos(0, 200, 0), 4096, 64, 64);
			helper.assertTrue(wood != null && MethodGrounds.inField(greatVoid, wood.getFirst(), Stage.EARLY) && !MethodGrounds.inField(greatVoid, wood.getFirst(), Stage.MID),
					"The wood field is Early's, not Mid's");
			// Ancestral Communion: among the primeval dragon's bones, in the graveyard's own land.
			secret.getChunk(bones);
			for (int i = 0; i < MethodGrounds.BONES - 1; i++) secret.setBlockAndUpdate(bones.offset(i - 1, -2, 2), Blocks.BONE_BLOCK.defaultBlockState());
			helper.assertTrue(!MethodGrounds.amongBones(secret, bones), "Three bones: not enough");
			secret.setBlockAndUpdate(bones.offset(3, -2, 2), Blocks.BONE_BLOCK.defaultBlockState());
			helper.assertTrue(MethodGrounds.amongBones(secret, bones) && !MethodGrounds.amongBones(level, helper.absolutePos(new BlockPos(1, 2, 1))),
					"Four bones in the Primeval Beast Graveyard: communion; anywhere else, none");
			ServerPlayer demon = survivor(helper, made, "ancestral-communion");
			PlayerCultivation d = CultivationManager.own(demon);
			d.awaken(0);
			d.demon().setBloodline(Bloodline.TIGER);
			d.demon().set(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), 0);
			d.demon().setBloodRealm(Realm.NASCENT_SOUL.ordinal());
			CultivationMethods.learn(demon, CultivationMethod.ANCESTRAL_COMMUNION, Realm.NASCENT_SOUL);
			CultivationMethods.practise(demon, CultivationMethod.ANCESTRAL_COMMUNION);
			helper.assertTrue(key(CultivationMethods.whyNoGain(demon, d, TwoPaths.Path.DEMON)).equals(ModLang.MSG_COMMUNION_OUTSIDE), "Away from the bones: nothing");
			// Primordial Return: a living primordial beast near.
			BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
			helper.assertTrue(!MethodGrounds.nearPrimordial(level, origin), "No primordial beast near");
			DemonBeast phoenix = ModEntities.beast(BeastKind.TRUE_PHOENIX).create(level);
			phoenix.moveTo(origin.getX() + 20, origin.getY() + 10, origin.getZ(), 0, 0);
			phoenix.setNoAi(true);
			level.addFreshEntity(phoenix);
			spawned.add(phoenix);
			helper.assertTrue(phoenix instanceof PrimordialBeast && MethodGrounds.nearPrimordial(level, origin), "A True Phoenix within 48: Primordial Return gathers");
			// Immortal Essence Refining: its essence burnt for the gain.
			ServerPlayer immortal = survivor(helper, made, "essence-refining");
			PlayerCultivation i = CultivationManager.own(immortal);
			i.awaken(0);
			TestMethods.setAt(i, Realm.TRUE_IMMORTAL, Stage.EARLY);
			CultivationMethods.learn(immortal, CultivationMethod.IMMORTAL_ESSENCE_REFINING, Realm.TRUE_IMMORTAL);
			CultivationMethods.practise(immortal, CultivationMethod.IMMORTAL_ESSENCE_REFINING);
			helper.assertTrue(key(CultivationMethods.whyNoGain(immortal, i, TwoPaths.Path.HEAVEN)).equals(ModLang.MSG_ESSENCE_REFINING_OUTSIDE), "Out of the Immortal Realm: nothing");
			double max = ImmortalEssence.max(i);
			i.setEssence(max);
			helper.assertTrue(near(MethodGrounds.burn(immortal, i), MethodGrounds.ESSENCE_PACE) && near(i.essence(), max - max / MethodGrounds.ESSENCE_BURN * 0.5),
					"Half a second burns a hundred-and-twentieth of the pool for twice the gain");
			i.setEssence(max / MethodGrounds.ESSENCE_BURN * 0.25);
			helper.assertTrue(near(MethodGrounds.burn(immortal, i), MethodGrounds.ESSENCE_PACE / 2) && i.essence() == 0 && MethodGrounds.burn(immortal, i) == 0,
					"Half of what it wants: half the gain; none left: nothing");
			helper.succeed();
		} finally {
			for (int k = -1; k <= 3; k++) secret.setBlockAndUpdate(bones.offset(k, -2, 2), Blocks.AIR.defaultBlockState());
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void riftsAreWalkedAndVesselsNurtured(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "rift-walker");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			TestMethods.setAt(c, Realm.VOID_SHATTERING, Stage.EARLY);
			RiftWalking.climbed(p);
			helper.assertTrue(c.getCultivation() == 0, "Not practising it: a climb is only a climb");
			CultivationMethods.learn(p, CultivationMethod.RIFT_WALKING, Realm.VOID_SHATTERING);
			CultivationMethods.practise(p, CultivationMethod.RIFT_WALKING);
			RiftWalking.climbed(p);
			RiftWalking.struck(p);
			helper.assertTrue(near(c.getCultivation(), (RiftWalking.CLIMB + RiftWalking.BOLT) * c.cultivationRequired()), "A breach climbed and a bolt borne: 0.15 of the stage");
			TestMethods.setAt(c, Realm.SACRED_VESSEL, Stage.EARLY);
			CultivationMethods.learn(p, CultivationMethod.VESSEL_NURTURING, Realm.SACRED_VESSEL);
			CultivationMethods.practise(p, CultivationMethod.VESSEL_NURTURING);
			// Two of it: a pour may crack one (VesselNurturing#CRACK_ODDS), which left an empty stack, no item to rest, a flaky test (2026-11-10).
			ItemStack vessel = new ItemStack(ModItems.NINE_PROVINCES_DING, 2);
			c.setQi(c.maxQi() * 0.4);
			helper.assertTrue(VesselNurturing.pour(p, vessel) && c.getCultivation() == 0, "Less than half its qi: refused");
			c.setQi(c.maxQi());
			helper.assertTrue(VesselNurturing.pour(p, vessel) && near(c.getCultivation(), VesselNurturing.SHARE * c.cultivationRequired())
					&& near(c.getQi(), c.maxQi() * (1 - VesselNurturing.POUR)) && p.getCooldowns().isOnCooldown(vessel.getItem()),
					"Half its qi poured in: a tenth of the stage drawn back, and the vessel rests");
			helper.assertTrue(!VesselNurturing.pour(p, vessel), "Resting: only held");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void nirvanaRebirthRisesFromTheShore(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "nirvana-rebirth");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			TestMethods.setAt(c, Realm.ENTERING_NIRVANA, Stage.EARLY);
			CultivationMethods.learn(p, CultivationMethod.NIRVANA_REBIRTH, Realm.ENTERING_NIRVANA);
			CultivationMethods.practise(p, CultivationMethod.NIRVANA_REBIRTH);
			helper.assertTrue(NirvanaRebirth.practises(p) && NirvanaRebirth.allowDeath(p, p.damageSources().generic()), "Off the shore: the death goes ahead");
			ServerLevel greatVoid = helper.getLevel().getServer().getLevel(ModDimensions.GREAT_VOID);
			Pair<BlockPos, Holder<Biome>> shore = greatVoid.findClosestBiome3d(b -> b.is(ModBiomes.SHORE_OF_NIRVANA), new BlockPos(0, 200, 0), 2048, 32, 64);
			helper.assertTrue(shore != null && NirvanaRebirth.shore(greatVoid, shore.getFirst()), "The Shore of Nirvana lies about the Great Void's middle");
			int risen = 0, fell = 0;
			for (int k = 0; k < 40; k++) {
				c.setState(Realm.ENTERING_NIRVANA, Stage.EARLY, 0);
				p.setHealth(1);
				if (NirvanaRebirth.rises(p)) {
					risen++;
					helper.assertTrue(p.getHealth() == p.getMaxHealth() && near(c.getCultivation(), NirvanaRebirth.SHARE * c.cultivationRequired()),
							"Risen from its ashes: whole, half a stage won");
				} else {
					fell++;
					helper.assertTrue(c.getCultivation() == 0, "Not risen: the death is real, nothing won");
				}
			}
			helper.assertTrue(risen > fell && fell > 0, "Three times in four it rises (" + risen + " of 40)");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSpringsAreSoakedInAndTheLostLedToTheWheel(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		BlockPos wheelAt = new BlockPos(6, 2, 6);
		try {
			helper.assertTrue(near(YellowSpringsImmersion.share(Realm.NASCENT_SOUL), YellowSpringsImmersion.SHARE)
					&& near(YellowSpringsImmersion.share(Realm.HEAVENLY_BEING), YellowSpringsImmersion.SHARE / GhostMethods.REALM_TIME_STEP),
					"A second in the springs: 0.0025 of the stage at Nascent Soul, a realm's time step less a realm past it");
			ServerPlayer p = survivor(helper, made, "springs-and-wheel");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			GhostMethods.give(p, GhostMethod.NETHERWORLD_BREATHING);
			NetherworldState s = c.netherworld();
			int hb = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY);
			GhostMethods.setPath(s, hb);
			CultivationMethods.learn(p, CultivationMethod.YELLOW_SPRINGS_IMMERSION, Realm.HEAVENLY_BEING);
			CultivationMethods.practise(p, CultivationMethod.YELLOW_SPRINGS_IMMERSION);
			helper.assertTrue(YellowSpringsImmersion.soaks(p) && !YellowSpringsImmersion.inTheSprings(p), "Practised, but dry and far from the springs");
			CultivationMethods.learn(p, CultivationMethod.JUDGEMENT_HALL_SERVICE, Realm.DAO_INTEGRATION);
			CultivationMethods.practise(p, CultivationMethod.JUDGEMENT_HALL_SERVICE);
			helper.assertTrue(JudgementHallService.serves(p) && !YellowSpringsImmersion.soaks(p), "One way at a time: now the Judgement Hall's errand");
			GhostCultivatorNpc soul = ModEntities.GHOST_CULTIVATOR.create(level);
			BlockPos soulAt = helper.absolutePos(new BlockPos(3, 2, 3));
			soul.moveTo(soulAt.getX() + 0.5, soulAt.getY(), soulAt.getZ() + 0.5, 0, 0);
			soul.becomeGhost(0, 0, level.getRandom());
			soul.setNoAi(true);
			level.addFreshEntity(soul);
			spawned.add(soul);
			helper.assertTrue(soul.isWanderingRogue() && JudgementHallService.lead(p, soul) && JudgementHallService.isLed(soul), "A wandering soul, led");
			JudgementHallService.lead(p, soul);
			helper.assertTrue(!JudgementHallService.isLed(soul), "Again: let go");
			JudgementHallService.lead(p, soul);
			helper.assertTrue(!JudgementHallService.nearWheel(level, soul.blockPosition()), "No Wheel near yet");
			helper.setBlock(wheelAt, ModBlocks.WHEEL_OF_REINCARNATION);
			helper.assertTrue(JudgementHallService.nearWheel(level, soul.blockPosition()), "Brought within eight of the Wheel");
			double before = s.pathCultivation();
			JudgementHallService.passOn(p, soul);
			helper.assertTrue(soul.isRemoved() && !JudgementHallService.isLed(soul) && near(s.pathCultivation() - before, JudgementHallService.SHARE * GhostMethods.required(hb)),
					"It passes on into the Wheel: 0.08 of the stage");
			helper.succeed();
		} finally {
			helper.setBlock(wheelAt, Blocks.AIR);
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void riftSoulsAreSeizedAndTheFurnaceBurns(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		BlockPos riftAt = new BlockPos(6, 4, 6);
		try {
			ServerPlayer p = survivor(helper, made, "rift-haunter");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			GhostMethods.give(p, GhostMethod.NETHERWORLD_BREATHING);
			NetherworldState s = c.netherworld();
			int vs = PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY);
			GhostMethods.setPath(s, vs);
			CultivationMethods.learn(p, CultivationMethod.RIFT_HAUNTING, Realm.VOID_SHATTERING);
			CultivationMethods.practise(p, CultivationMethod.RIFT_HAUNTING);
			helper.assertTrue(RiftHaunting.haunts(p) && RiftHaunting.riftNear(p) == null, "Practised, but no rift near");
			helper.setBlock(riftAt, ModBlocks.SPATIAL_RIFT);
			helper.assertTrue(RiftHaunting.riftNear(p) != null, "A spatial rift within eight: the lost souls slip out to it");
			int below = Realm.VOID_SHATTERING.ordinal() - 1;
			helper.assertTrue(SoulRiverFeast.take(p, below, p.getEyePosition(), CultivationMethod.RIFT_HAUNTING, RiftHaunting.WORTH)
					&& near(s.pathCultivation(), SoulRiverFeast.share(below, below + 1) * RiftHaunting.WORTH * GhostMethods.required(vs)),
					"A soul a realm below seized: half what the river's would give (" + s.pathCultivation() + ")");
			// The soul furnace, on one of the dead (the living die when it breaks).
			ServerPlayer g = survivor(helper, made, "soul-furnace");
			PlayerCultivation d = CultivationManager.own(g);
			d.awaken(0);
			GhostSystem.makeGhost(g);
			NetherworldState t = d.netherworld();
			int en = PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY);
			GhostMethods.setPath(t, en);
			CultivationMethods.learn(g, CultivationMethod.SOUL_FURNACE, Realm.ENTERING_NIRVANA);
			CultivationMethods.practise(g, CultivationMethod.SOUL_FURNACE);
			helper.assertTrue(SoulFurnace.burns(d) && near(SoulFurnace.harm(0), SoulFurnace.HARM) && near(SoulFurnace.harm(1000), SoulFurnace.MOST_HARM)
					&& near(SoulFurnace.gain(SoulFurnace.RAMP), 2 * SoulFurnace.GAIN), "Hotter every second: the gain twice at thirty, the harm a fifth at most");
			g.setHealth(g.getMaxHealth());
			float whole = g.getHealth();
			double won = SoulFurnace.burn(g, d, 600, false);
			helper.assertTrue(near(won, 0.5 * SoulFurnace.gain(30) * GhostMethods.required(en)) && Math.abs(whole - g.getHealth() - 0.5 * SoulFurnace.harm(30) * g.getMaxHealth()) < 1e-3,
					"Thirty seconds in: its half second's gain, and its harm off the soul's health");
			t.setPath(en, GhostMethods.required(en) * 0.4);
			g.setHealth(1);
			helper.assertTrue(SoulFurnace.burn(g, d, 600, false) == 0 && t.pathCultivation() == 0 && GhostMethods.ghostRank(t) == en,
					"Out of health, the furnace breaks: the stage's cultivation burnt away, never the stage");
			helper.succeed();
		} finally {
			helper.setBlock(riftAt, Blocks.AIR);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tenThousandGhostsTakesInTheSlain(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		try {
			int ti = Realm.TRUE_IMMORTAL.ordinal();
			helper.assertTrue(near(TenThousandGhosts.cultivatorShare(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY), ti), TenThousandGhosts.CULTIVATOR)
					&& near(TenThousandGhosts.cultivatorShare(PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.LATE), ti), 2 * TenThousandGhosts.CULTIVATOR)
					&& near(TenThousandGhosts.cultivatorShare(0, ti), TenThousandGhosts.CULTIVATOR / 32)
					&& near(TenThousandGhosts.cultivatorShare(PlayerCultivation.LAST_RANK, ti), TenThousandGhosts.MOST),
					"A ghost cultivator's worth: 0.02 at Four Axis, twice a realm above, at most half a stage");
			ServerPlayer p = survivor(helper, made, "ten-thousand-ghosts");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			GhostMethods.give(p, GhostMethod.NETHERWORLD_BREATHING);
			NetherworldState s = c.netherworld();
			int rank = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY);
			GhostMethods.setPath(s, rank);
			CultivationMethods.learn(p, CultivationMethod.TEN_THOUSAND_GHOSTS, Realm.TRUE_IMMORTAL);
			CultivationMethods.practise(p, CultivationMethod.TEN_THOUSAND_GHOSTS);
			HungryGhost hungry = helper.spawnWithNoFreeWill(ModEntities.HUNGRY_GHOST, new BlockPos(3, 2, 3));
			spawned.add(hungry);
			int alignment = c.getAlignment();
			TenThousandGhosts.onDeath(hungry, level.damageSources().magic());
			helper.assertTrue(s.pathCultivation() == 0, "Not by its hand: nothing taken in");
			TenThousandGhosts.onDeath(hungry, level.damageSources().playerAttack(p));
			helper.assertTrue(near(s.pathCultivation(), TenThousandGhosts.HUNGRY * GhostMethods.required(rank)) && c.getAlignment() == alignment - TenThousandGhosts.SHIFT,
					"A Hungry Ghost slain by its hand: taken in, and its path darker");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bloodlineNirvanaRemakesOrBurnsTheBlood(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "bloodline-nirvana");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			c.demon().setBloodline(Bloodline.FOX);
			int en = PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY);
			c.demon().set(en, 0);
			c.demon().setBloodRealm(Realm.ENTERING_NIRVANA.ordinal());
			helper.assertTrue(DemonBlood.judge(c, Bloodline.FOX, Realm.ENTERING_NIRVANA, false, false, false).verdict() == DemonBlood.Verdict.REFUSE,
					"Blood it holds already: refused");
			CultivationMethods.learn(p, CultivationMethod.BLOODLINE_NIRVANA, Realm.ENTERING_NIRVANA);
			CultivationMethods.practise(p, CultivationMethod.BLOODLINE_NIRVANA);
			helper.assertTrue(BloodlineNirvana.takes(c, Realm.ENTERING_NIRVANA) && !BloodlineNirvana.takes(c, Realm.SACRED_VESSEL)
					&& DemonBlood.judge(c, Bloodline.FOX, Realm.ENTERING_NIRVANA, false, false, false).verdict() == DemonBlood.Verdict.DRINK,
					"Practising it: its own blood of Entering Nirvana is drunk, held or not");
			int risen = 0, burnt = 0;
			for (int k = 0; k < 60; k++) {
				c.demon().set(en, DemonPath.required(en) * 0.3);
				c.demon().setTaint(0.2);
				p.setHealth(p.getMaxHealth());
				helper.assertTrue(DemonBlood.drink(p, Bloodline.FOX, Realm.ENTERING_NIRVANA), "Drunk");
				if (c.demon().taint() == 0) {
					risen++;
					helper.assertTrue(near(c.demon().cultivation(), DemonPath.required(en) * (0.3 + BloodlineNirvana.SHARE)), "Reborn: the taint burnt out, half a stage won");
				} else {
					burnt++;
					helper.assertTrue(c.demon().cultivation() == 0 && DemonPath.rank(c) == en, "Burnt: the stage's cultivation lost, never the stage");
				}
			}
			// (Sixty draws: at seven in ten, fewer than thirty risen is a chance of some three in ten thousand.)
			helper.assertTrue(risen >= 30 && burnt > 0, "Seven times in ten it rises (" + risen + " of 60)");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachWorldKeepsItsOwnRealmsWays(GameTestHelper helper) {
		RandomSource random = RandomSource.create(20261101L);
		Alignment.Faction[] factions = Alignment.Faction.values();
		for (int k = 0; k < 900; k++) {
			boolean upper = k % 2 == 0;
			Alignment.Faction faction = factions[k % factions.length];
			ItemStack[] stacks = {MethodManualItem.kept(faction, null, upper, true, random), MethodManualItem.kept(faction, Bloodline.TIGER, upper, true, random)};
			for (ItemStack stack : stacks) {
				CultivationMethod method = MethodManualItem.method(stack);
				if (method == null) continue;
				helper.assertTrue(method.first().ordinal() <= (upper ? Realm.SACRED_VESSEL : Realm.CORE_FORMATION).ordinal(),
						"No sect keeps a way beyond its world's realms: " + method.getId());
			}
		}
		for (int k = 0; k < 40; k++) {
			ItemStack en = MethodManualItem.ancient(Realm.ENTERING_NIRVANA, random), ti = MethodManualItem.ancient(Realm.TRUE_IMMORTAL, random);
			helper.assertTrue(MethodManualItem.method(en).first() == Realm.ENTERING_NIRVANA && MethodManualItem.realm(en) == Realm.ENTERING_NIRVANA
					&& MethodManualItem.method(ti).first() == Realm.TRUE_IMMORTAL, "The ancients' vaults: Entering Nirvana's ways, True Immortal's");
		}
		helper.assertTrue(MethodManualItem.ancient(Realm.FOUR_AXIS, random).isEmpty(), "No way begins at Four Axis");
		// Every way of phase 4 is somewhere to be found: a sect's kind, or the ancients' vaults.
		for (CultivationMethod method : CultivationMethod.values()) {
			if (!method.fromManual()) continue;
			boolean kept = method.first().ordinal() >= Realm.ENTERING_NIRVANA.ordinal();
			for (Alignment.Faction faction : factions) kept |= MethodManualItem.kinds(faction, null).contains(method) || MethodManualItem.kinds(faction, Bloodline.FOX).contains(method);
			helper.assertTrue(kept, "Somewhere to be found: " + method.getId());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void devilishCultivatorsDrainTheDead(GameTestHelper helper) {
		// The goal itself, worked by hand (the shared test world's other people would turn a free cultivator's head: the tour sees it live).
		ServerLevel level = helper.getLevel();
		BlockPos bodyAt = new BlockPos(5, 2, 5);
		List<Entity> spawned = new ArrayList<>();
		try {
			helper.setBlock(bodyAt, ModBlocks.CORPSE);
			CorpseBlockEntity body = (CorpseBlockEntity) level.getBlockEntity(helper.absolutePos(bodyAt));
			CultivatorNpc devilish = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(4, 2, 5));
			devilish.becomeRogue(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), -150, level.getRandom());
			CultivatorNpc righteous = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(6, 2, 5));
			righteous.becomeRogue(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), 150, level.getRandom());
			spawned.add(devilish);
			spawned.add(righteous);
			devilish.tickCount = righteous.tickCount = 40;
			CorpseYin.DrainGoal drains = new CorpseYin.DrainGoal(devilish), wont = new CorpseYin.DrainGoal(righteous);
			helper.assertTrue(body != null && !body.yinDrawn() && drains.canUse() && !wont.canUse(), "A devilish cultivator sees a fresh body to drain; a righteous one doesn't");
			drains.start();
			for (int k = 0; k < CorpseYin.NPC_DRAWS + 5 && !body.yinDrawn(); k++) drains.tick();
			helper.assertTrue(body.yinDrawn(), "Beside the body, it draws its yin out");
			devilish.tickCount = 80;
			helper.assertTrue(!drains.canUse(), "Drained, the body is nothing to it");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			helper.setBlock(bodyAt, Blocks.AIR);
		}
	}

	// --- Helpers ---

	/** A survival player in the player list (the helper's mock player is creative, and pays no years), remembered in {@code made}. */
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
			MeditationManager.forget(p.getUUID());
			Spells.forget(p.getUUID());
			HeavenMethods.forget(p.getUUID());
			GhostSystem.forget(p.getUUID());
			TwoPaths.forget(p.getUUID());
			YellowSpringsImmersion.forget(p.getUUID());
			RiftHaunting.forget(p.getUUID());
			JudgementHallService.onDisconnect(p);
			p.server.getPlayerList().remove(p);
		}
	}
}
