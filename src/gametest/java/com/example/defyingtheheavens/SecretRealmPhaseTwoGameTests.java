package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The secret realms' second phase (2026-10-31): the Sunken Sword Tomb and the Ancient Battlefield, who gets into each and who flies freely;
 * their lands from the world's seed (the Sword Path and the ways up walkable, the ways in clear of the trenches); each realm its own biome;
 * the Seals apart; the Sleeping Sword's grade following its bearer; the War General's Banner calling the old army, strengthening its side
 * and resting; Sword Intent's second cut after a blade's blow alone; Blood Battle's guard against many; the tomb's cuts by the realm's
 * measure; where those who come for a realm are raised; and a whole opening of each, laid and shut ({@link SecretRealms#inTests}).
 */
public class SecretRealmPhaseTwoGameTests implements FabricGameTest {
	private static final SecretRealm TOMB = SecretRealm.SUNKEN_SWORD_TOMB, FIELD = SecretRealm.ANCIENT_BATTLEFIELD;

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachAdmitsItsLowestAndFreesTheVastlyStronger(GameTestHelper helper) {
		helper.assertTrue(!TOMB.mayEnter(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.MID)) && TOMB.mayEnter(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.LATE)),
				"The Tomb from Foundation Building Late");
		helper.assertTrue(!FIELD.mayEnter(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID)) && FIELD.mayEnter(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.LATE)),
				"The Battlefield from Core Formation Late");
		helper.assertTrue(TOMB.mayEnter(PlayerCultivation.rank(Realm.HEAVENLY_LORD, Stage.GRAND_PERFECTION)) && FIELD.mayEnter(PlayerCultivation.rank(Realm.HEAVENLY_LORD,
				Stage.GRAND_PERFECTION)), "No upper limit");
		helper.assertTrue(!TOMB.fliesFreely(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION)) && TOMB.fliesFreely(PlayerCultivation.rank(Realm.HEAVENLY_BEING,
				Stage.EARLY)), "Over the Tomb (meant for Core Formation), Heavenly Beings fly freely");
		helper.assertTrue(!FIELD.fliesFreely(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION)) && FIELD.fliesFreely(PlayerCultivation.rank(Realm.FOUR_AXIS,
				Stage.EARLY)), "Over the Battlefield (meant for Nascent Soul), Four Axis");
		helper.assertTrue(TOMB.centre().getX() != FIELD.centre().getX() && SecretRealm.at(TOMB.centre().getX(), 0) == TOMB && SecretRealm.at(FIELD.centre().getX(), 0) == FIELD,
				"Each in a slot of its own");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theTombIsLaidFromItsSeedAndItsPathCanBeWalked(GameTestHelper helper) {
		SwordTomb.Shape s = SwordTomb.shape(helper.getLevel().getSeed());
		SwordTomb.Column heart = SwordTomb.column(s, 0, 0);
		helper.assertTrue(heart.kind() == SwordTomb.Kind.SUMMIT && heart.ground() == SwordTomb.SUMMIT_Y, "The spire's summit at its heart: " + heart);
		SwordTomb.Column floor = SwordTomb.column(s, 0, (SwordTomb.RING_IN + SwordTomb.RING_OUT) / 2);
		helper.assertTrue(floor.kind() == SwordTomb.Kind.FLOOR && Math.abs(floor.ground() - SwordTomb.FLOOR_Y) <= 3, "The gorge's floor about it: " + floor);
		helper.assertTrue(SwordTomb.column(s, 400, 0).kind() == SwordTomb.Kind.NONE, "Nothing past its edge");
		helper.assertTrue(SwordTomb.arrivals(s).size() == SwordTomb.BRANCHES, "An arrival ledge at each branch's head");
		// The Sword Path: on its ledge all the way, never climbing more than a block from one block to the next.
		int last = Integer.MIN_VALUE, lx = Integer.MIN_VALUE, lz = Integer.MIN_VALUE;
		for (double turn = 0; turn <= SwordTomb.PATH_TURN; turn += 0.5) {
			BlockPos p = SwordTomb.pathPoint(s, turn);
			if (p.getX() == lx && p.getZ() == lz) continue;
			SwordTomb.Column c = SwordTomb.column(s, p.getX(), p.getZ());
			helper.assertTrue(c.kind() == SwordTomb.Kind.PATH, "On the path's ledge at " + turn + ": " + c);
			if (last != Integer.MIN_VALUE) helper.assertTrue(Math.abs(c.ground() - last) <= 1, "The path climbs a step at a time at " + turn + ": " + last + " to " + c.ground());
			last = c.ground();
			lx = p.getX();
			lz = p.getZ();
		}
		helper.assertTrue(last >= SwordTomb.SUMMIT_Y - 1, "...up to the summit: " + last);
		// The main branch canyon down from its ledge to the floor: a step at a time too.
		double b = s.branch(0);
		last = Integer.MIN_VALUE;
		for (int r = SwordTomb.ARRIVAL_R - 6; r >= SwordTomb.RING_IN + 4; r--) {
			SwordTomb.Column c = SwordTomb.column(s, (int) Math.round(Math.cos(b) * r), (int) Math.round(Math.sin(b) * r));
			if (last != Integer.MIN_VALUE) helper.assertTrue(Math.abs(c.ground() - last) <= 1, "The way down steps a block at most, at " + r + ": " + last + " to " + c.ground());
			last = c.ground();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBattlefieldIsLaidFromItsSeedAndItsWaysAreOpen(GameTestHelper helper) {
		Battlefield.Shape s = Battlefield.shape(helper.getLevel().getSeed());
		Battlefield.Column heart = Battlefield.column(s, 0, 0);
		helper.assertTrue(heart.kind() == Battlefield.Kind.HILLTOP && heart.ground() == Battlefield.HILL_TOP_Y, "The General's hill at its heart: " + heart);
		helper.assertTrue(Battlefield.column(s, 400, 0).kind() == Battlefield.Kind.NONE, "Nothing past its edge");
		helper.assertTrue(Battlefield.arrivals(s).size() == 4, "A camp on each way in");
		for (int k = 0; k < 4; k++) {
			double b = s.way(k);
			for (int r = Battlefield.HILL_R + 1; r <= Battlefield.ARRIVAL_R; r++) {
				Battlefield.Column c = Battlefield.column(s, (int) Math.round(Math.cos(b) * r), (int) Math.round(Math.sin(b) * r));
				helper.assertTrue(c.kind() != Battlefield.Kind.TRENCH && c.kind() != Battlefield.Kind.BERM, "Way " + k + " crosses no trench, at " + r + ": " + c);
			}
		}
		// The ramp up the hill's front, from the main way: a step at a time.
		int last = Integer.MIN_VALUE;
		for (int r = Battlefield.HILL_R - 1; r >= Battlefield.HILL_FLAT_R; r--) {
			Battlefield.Column c = Battlefield.column(s, (int) Math.round(Math.cos(s.mainTurn) * r), (int) Math.round(Math.sin(s.mainTurn) * r));
			helper.assertTrue(c.kind() == Battlefield.Kind.RAMP || c.kind() == Battlefield.Kind.HILLTOP, "On the ramp at " + r + ": " + c);
			if (last != Integer.MIN_VALUE) helper.assertTrue(c.ground() - last <= 1 && c.ground() >= last, "The ramp climbs a step at a time at " + r + ": " + last + " to " + c.ground());
			last = c.ground();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachRealmHasItsOwnBiome(GameTestHelper helper) {
		ServerLevel realms = helper.getLevel().getServer().getLevel(ModDimensions.SECRET_REALM);
		helper.assertTrue(realms != null && realms.getChunkSource().getGenerator().getBiomeSource() instanceof SecretRealmBiomeSource, "The realms' own biome source");
		var sampler = realms.getChunkSource().randomState().sampler();
		var source = realms.getChunkSource().getGenerator().getBiomeSource();
		for (SecretRealm realm : SecretRealm.values()) {
			BlockPos c = realm.centre();
			Holder<Biome> biome = source.getNoiseBiome(c.getX() >> 2, c.getY() >> 2, c.getZ() >> 2, sampler);
			helper.assertTrue(biome.is(realm.biome()), realm.getId() + "'s own biome over its land: " + biome.unwrapKey());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSealsLieApart(GameTestHelper helper) {
		SecretRealms.Data apart = new SecretRealms.Data();
		List<SecretRealm> placed = new ArrayList<>();
		for (SecretRealm realm : SecretRealm.values()) {
			// Each in its own world (the Yellow Springs Hollow's in the Netherworld), apart from the others there.
			ServerLevel home = helper.getLevel().getServer().getLevel(realm.home());
			BlockPos site = SecretRealms.site(home, realm, apart);
			apart.of(realm).seal = site;
			for (SecretRealm other : placed) {
				if (other.home() != realm.home()) continue;
				BlockPos at = apart.of(other).seal;
				helper.assertTrue(Math.hypot(at.getX() - site.getX(), at.getZ() - site.getZ()) >= SecretRealms.SITE_APART, "Seals " + SecretRealms.SITE_APART
						+ " apart: " + at + " and " + site);
			}
			placed.add(realm);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSleepingSwordGrowsWithItsBearer(GameTestHelper helper) {
		helper.assertTrue(SleepingSwordItem.gradeFor(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION)) == ArtifactGrade.SPIRIT, "Spirit to Core Formation");
		helper.assertTrue(SleepingSwordItem.gradeFor(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY)) == ArtifactGrade.EARTH, "Earth from Nascent Soul");
		helper.assertTrue(SleepingSwordItem.gradeFor(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY)) == ArtifactGrade.HEAVEN, "Heaven from Four Axis");
		helper.assertTrue(SleepingSwordItem.gradeFor(PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY)) == ArtifactGrade.IMMORTAL, "Immortal from Void Shattering");
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		try {
			CultivationManager.get(p).setState(Realm.NASCENT_SOUL, Stage.MID, 0);
			ItemStack sword = SleepingSwordItem.create();
			helper.assertTrue(Artifacts.quality(sword) == Artifacts.Quality.PERFECT, "Of a master's Perfect work");
			SleepingSwordItem item = (SleepingSwordItem) ModItems.SLEEPING_SWORD;
			double spirit = damage(item, sword);
			p.tickCount = 20;
			item.inventoryTick(sword, p.level(), p, 0, true);
			helper.assertTrue(item.grade(sword) == ArtifactGrade.EARTH, "Borne by a Nascent Soul, Earth grade: " + item.grade(sword));
			helper.assertTrue(damage(item, sword) > spirit, "...and its blow an Earth blade's: " + spirit + " to " + damage(item, sword));
			helper.succeed();
		} finally {
			p.discard();
		}
	}

	private static double damage(SleepingSwordItem item, ItemStack stack) {
		double sum = 0;
		for (AttributeModifier m : item.getAttributeModifiers(stack, EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) sum += m.getAmount();
		return sum;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBannerCallsTheOldArmyAndRests(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		List<ArmySpirit> called = new ArrayList<>();
		try {
			// In the test's middle, so all four stand within it (a mock player keeps no land about it for entities to be found in).
			BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
			p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			PlayerCultivation c = CultivationManager.get(p);
			c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
			c.setQi(c.maxQi());
			ItemStack banner = new ItemStack(ModItems.WAR_GENERALS_BANNER);
			p.setItemInHand(InteractionHand.MAIN_HAND, banner);
			p.getAbilities().instabuild = false; // (a mock player builds as in creative: in creative the banner costs nothing)
			double full = c.getQi();
			ModItems.WAR_GENERALS_BANNER.use(p.level(), p, InteractionHand.MAIN_HAND);
			for (var e : helper.getLevel().getAllEntities()) if (e instanceof ArmySpirit s && p.getUUID().equals(s.owner())) called.add(s);
			helper.assertTrue(called.size() == WarGeneralsBannerItem.CALLED, "Four of the old army answer: " + called.size());
			helper.assertTrue(Math.abs(c.getQi() - full * (1 - WarGeneralsBannerItem.COST_SHARE)) < 1.0e-6 * full + 1.0e-9, "For a fifth of its bearer's qi: " + c.getQi() + " of " + full);
			helper.assertTrue(called.get(0).fightsFor(p) && !called.get(0).canAttack(p), "Theirs: never against their bearer");
			// Held, its side is strengthened, the old army with it.
			p.tickCount = 20;
			ModItems.WAR_GENERALS_BANNER.inventoryTick(banner, p.level(), p, 0, true);
			helper.assertTrue(p.hasEffect(MobEffects.DAMAGE_BOOST) && p.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Its bearer strikes harder and is harder to harm");
			helper.assertTrue(called.stream().anyMatch(s -> s.hasEffect(MobEffects.DAMAGE_BOOST)), "...and so does the army it called");
			// Raised again at once: it rests.
			c.setQi(c.maxQi());
			ModItems.WAR_GENERALS_BANNER.use(p.level(), p, InteractionHand.MAIN_HAND);
			int now = 0;
			for (var e : helper.getLevel().getAllEntities()) if (e instanceof ArmySpirit s && p.getUUID().equals(s.owner())) now++;
			helper.assertTrue(now == WarGeneralsBannerItem.CALLED && c.getQi() == c.maxQi(), "Resting, it calls no one more and costs nothing: " + now);
			helper.succeed();
		} finally {
			List<ArmySpirit> all = new ArrayList<>(called);
			for (var e : helper.getLevel().getAllEntities()) if (e instanceof ArmySpirit s && p.getUUID().equals(s.owner()) && !all.contains(s)) all.add(s);
			for (ArmySpirit s : all) s.discard();
			p.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void swordIntentFollowsABladesBlowWithASecondCut(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		Pig cut = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
		Pig spared = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 2, 1));
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		c.learnTechnique(Technique.SWORD_INTENT);
		c.setAbilityEnabled(Ability.SWORD_INTENT, true);
		c.setQi(c.maxQi());
		helper.assertTrue(WarArts.lit(p, Ability.SWORD_INTENT), "Sword Intent lit");
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		WarArts.landed(cut, p.damageSources().playerAttack(p), 8);
		p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		WarArts.landed(spared, p.damageSources().playerAttack(p), 8); // a fist's blow: no blade, no second cut
		float before = cut.getHealth(), whole = spared.getHealth();
		helper.runAfterDelay(WarArts.INTENT_DELAY + 4, () -> {
			try {
				helper.assertTrue(cut.getHealth() <= before - 8 * WarArts.INTENT_SHARE + 0.01, "A second cut, half the blow: " + before + " to " + cut.getHealth());
				helper.assertTrue(spared.getHealth() == whole, "None after a fist's blow");
				helper.succeed();
			} finally {
				p.discard();
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bloodBattleGuardsAgainstMany(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		List<Husk> foes = new ArrayList<>();
		try {
			BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
			p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			PlayerCultivation c = CultivationManager.get(p);
			c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
			c.learnTechnique(Technique.BLOOD_BATTLE);
			c.setAbilityEnabled(Ability.BLOOD_BATTLE, true);
			var blow = p.damageSources().generic();
			foes.add(helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(1, 2, 1)));
			foes.get(0).setTarget(p);
			helper.assertTrue(WarArts.guard(p, blow, 10) == 10, "One foe: no guard");
			foes.add(helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(3, 2, 1)));
			foes.add(helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(1, 2, 3)));
			for (Husk h : foes) h.setTarget(p);
			helper.assertTrue(Math.abs(WarArts.guard(p, blow, 10) - 10 * (1 - 2 * WarArts.BLOOD_GUARD)) < 1.0e-4, "Three: a tenth off for each past the first");
			for (int i = 0; i < 6; i++) {
				Husk h = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(3, 2, 3));
				h.setTarget(p);
				foes.add(h);
			}
			helper.assertTrue(Math.abs(WarArts.guard(p, blow, 10) - 10 * (1 - WarArts.BLOOD_GUARD_MOST)) < 1.0e-4, "Many: at most half");
			c.setAbilityEnabled(Ability.BLOOD_BATTLE, false);
			helper.assertTrue(WarArts.guard(p, blow, 10) == 10, "Switched off, nothing");
			helper.succeed();
		} finally {
			for (Husk h : foes) h.discard();
			p.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theTombCutsByTheRealmsMeasure(GameTestHelper helper) {
		ServerPlayer weak = helper.makeMockServerPlayerInLevel(), meant = helper.makeMockServerPlayerInLevel(), strong = helper.makeMockServerPlayerInLevel();
		try {
			CultivationManager.get(weak).setState(Realm.FOUNDATION_BUILDING, Stage.LATE, 0);
			CultivationManager.get(meant).setState(Realm.CORE_FORMATION, Stage.MID, 0);
			CultivationManager.get(strong).setState(Realm.HEAVENLY_BEING, Stage.MID, 0);
			double w = SwordTombWorks.weakness(TOMB, weak), m = SwordTombWorks.weakness(TOMB, meant), s = SwordTombWorks.weakness(TOMB, strong);
			helper.assertTrue(w > m && m > s, "Harder the weaker one is: " + w + ", " + m + ", " + s);
			helper.assertTrue(Math.abs(m - 1) < 1.0e-9 && s == 0.25, "Its own measure at the realm it is meant for, a quarter far above it");
			helper.succeed();
		} finally {
			weak.discard();
			meant.discard();
			strong.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSwitchesOfEveryAbilityReachTheClient(GameTestHelper helper) {
		helper.assertTrue(Ability.values().length > 32 && Ability.values().length <= 64, "More abilities than an int's bits, fewer than a long's: " + Ability.values().length);
		PlayerCultivation c = new PlayerCultivation();
		for (Ability a : Ability.values()) c.setAbilityEnabled(a, true);
		c.setAbilityEnabled(Ability.BLOOD_BATTLE, false);
		PlayerCultivation client = new PlayerCultivation();
		client.setDisabledAbilityMask(c.getDisabledAbilityMask());
		helper.assertTrue(!client.isAbilityEnabled(Ability.BLOOD_BATTLE) && client.isAbilityEnabled(Ability.QI_SUSTENANCE),
				"The last ability's switch comes through as itself, not onto the first's");
		for (Ability a : Ability.values()) helper.assertTrue(client.isAbilityEnabled(a) == c.isAbilityEnabled(a), "Every switch as it was: " + a);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void thoseWhoComeAreRaisedOnItsLand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var random = level.getRandom();
		for (SecretRealm realm : SecretRealm.values()) {
			BlockPos c = realm.centre();
			for (double along : new double[] {0, 0.5, 1}) {
				BlockPos at = realm.works().rivalStart(level, level.getSeed(), c, along, random);
				double d = realm.distance(at.getX(), at.getZ());
				helper.assertTrue(d <= SecretRealm.REACH && SecretRealm.at(at.getX(), at.getZ()) == realm, realm.getId() + ": raised within it, " + along + " along: " + d);
			}
		}
		helper.succeed();
	}

	/** A whole opening of each, out of turn: its land laid and readied, its heart's things in place; then shut; the Seals apart. */
	@GameTest(template = EMPTY_STRUCTURE, batch = "secret_realm_phase_two_cycle", timeoutTicks = 600)
	public void theTombAndTheBattlefieldOpenAndClose(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		SecretRealms.inTests = true;
		try {
			SecretRealms.Data data = SecretRealms.Data.get(server);
			ServerLevel realms = server.getLevel(ModDimensions.SECRET_REALM);
			for (SecretRealm realm : List.of(TOMB, FIELD)) {
				SecretRealms.Record r = data.of(realm);
				if (r.openedAt >= 0) SecretRealms.closeNow(server, realm);
				SecretRealms.openNow(server, realm);
				helper.assertTrue(r.openedAt >= 0 && r.laid && r.seal != null && r.sealBuilt, realm.getId() + ": open, its land laid and its Seal built");
			}
			SwordTomb.Shape ts = SwordTomb.shape(realms.getSeed());
			BlockState sword = realms.getBlockState(SwordTombWorks.shrineSword(ts, TOMB.centre()));
			helper.assertTrue(sword.is(ModBlocks.BURIED_SWORD), "The Sleeping Sword in its stone on the summit: " + sword);
			Battlefield.Shape bs = Battlefield.shape(realms.getSeed());
			helper.assertTrue(realms.getBlockState(BattlefieldWorks.bier(bs, FIELD.centre()).above()).is(ModBlocks.CORPSE), "The General's bones on his bier");
			helper.assertTrue(realms.getBlockState(BattlefieldWorks.ringMiddle(FIELD.centre()).above()).is(net.minecraft.world.level.block.Blocks.BARREL),
					"The war drum on the hill");
			List<SecretRealm> sealed = new ArrayList<>();
			for (SecretRealm realm : SecretRealm.values()) if (data.of(realm).seal != null) sealed.add(realm);
			for (int i = 0; i < sealed.size(); i++) {
				for (int j = i + 1; j < sealed.size(); j++) {
					if (sealed.get(i).home() != sealed.get(j).home()) continue; // in different worlds
					BlockPos a = data.of(sealed.get(i)).seal, b = data.of(sealed.get(j)).seal;
					helper.assertTrue(Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ()) >= SecretRealms.SITE_APART, "The Seals lie apart: " + a + ", " + b);
				}
			}
			for (SecretRealm realm : List.of(TOMB, FIELD)) {
				SecretRealms.closeNow(server, realm);
				helper.assertTrue(data.of(realm).openedAt < 0, realm.getId() + " shut again");
			}
			helper.succeed();
		} finally {
			SecretRealms.inTests = false;
		}
	}
}
