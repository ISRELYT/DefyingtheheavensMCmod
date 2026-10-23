package com.example.defyingtheheavens;

import com.example.defyingtheheavens.mixin.ServerPlayerAccessor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The user's ask of 2026-10-20: the eleven new physiques and the Yin Soul's and Jade Poison's changes, the grades evened out, a
 * Myriad Form Body's forms (Physique, Physiques and the systems each touches); Longevity and Rebirth Pills (PillItem,
 * RebirthPillItem); body refining's years (Lifespan); settlements kept alive (Settlements).
 */
public class PhysiquesPillsGameTests implements FabricGameTest {
	private static ServerPlayer survivor(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		return player;
	}

	/** A survival player in the player list (the helper's mock player always counts as creative, and the void bears the creative). */
	private static ServerPlayer trueSurvivor(GameTestHelper helper) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "starborn")) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		return p;
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1e-6 * Math.max(1, Math.abs(b));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGradesAreEvenedOut(GameTestHelper helper) {
		Map<Physique.Grade, Double> share = new EnumMap<>(Physique.Grade.class);
		for (Physique p : Physique.values()) share.merge(p.grade(), p.weight(), Double::sum);
		helper.assertTrue(near(share.get(Physique.Grade.LOW), 56) && near(share.get(Physique.Grade.MID), 24) && near(share.get(Physique.Grade.HIGH), 12)
				&& near(share.get(Physique.Grade.SUPREME), 5) && near(share.get(Physique.Grade.IMMORTAL), 3), "Of every hundred: Low 56, Mid 24, High 12, Supreme 5, Immortal 3 (" + share + ")");
		for (Physique p : Physique.values()) {
			if (p.grade() == Physique.Grade.SUPREME) helper.assertTrue(near(p.weight(), 1.25), p.getId() + ": 1.25 of a hundred, as each Supreme");
			if (p != Physique.ETERNAL_REINCARNATION_BODY && p != Physique.MYRIAD_FORM_BODY) {
				helper.assertTrue(p.weight() > Physique.ETERNAL_REINCARNATION_BODY.weight(), p.getId() + " is commoner than the rarest two");
			}
		}
		helper.assertTrue(near(Physique.ETERNAL_REINCARNATION_BODY.weight(), 0.1) && near(Physique.MYRIAD_FORM_BODY.weight(), 0.1),
				"The Eternal Reincarnation and Myriad Form Bodies: one in a thousand each");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void myriadFormsNeverClash(GameTestHelper helper) {
		RandomSource random = RandomSource.create(7);
		Set<Physique> seen = EnumSet.noneOf(Physique.class);
		boolean rooted = false;
		for (int i = 0; i < 4000; i++) {
			EnumSet<Physique> forms = Physique.rollForms(random);
			List<Physique> two = new ArrayList<>(forms);
			helper.assertTrue(two.size() == Physique.FORMS && two.stream().allMatch(Physique::isForm) && !Physique.clash(two.get(0), two.get(1)),
					"Two forms of Mid to Supreme grade, never a pair that clashes: " + forms);
			seen.addAll(forms);
			rooted |= two.stream().anyMatch(f -> f.rootCount() > 0);
		}
		for (Physique p : Physique.values()) {
			if (p.isForm()) helper.assertTrue(seen.contains(p), p.getId() + " is a form sometimes");
			else helper.assertTrue(!seen.contains(p), p.getId() + " never is");
		}
		helper.assertTrue(rooted, "A form of roots now and then");
		helper.assertTrue(Physique.clash(Physique.THREE_ELEMENT_ROOTS, Physique.TRUE_ELEMENT_ROOT) && Physique.clash(Physique.NINE_YANG_BODY, Physique.YIN_SOUL_BODY)
				&& !Physique.clash(Physique.NINE_YANG_BODY, Physique.STARBORN_BODY), "Three roots and one clash, the yang and the yin; the rest don't");

		// Both forms' gifts at once, a form's roots the body's.
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		double pace = c.meditationCultivationPerSecond(0);
		c.setPhysique(Physique.MYRIAD_FORM_BODY);
		c.setForms(List.of(Physique.NINE_YANG_BODY, Physique.HEAVEN_SACRED_BODY));
		helper.assertTrue(c.hasPhysique(Physique.MYRIAD_FORM_BODY) && c.hasPhysique(Physique.NINE_YANG_BODY) && c.hasPhysique(Physique.HEAVEN_SACRED_BODY)
				&& !c.hasPhysique(Physique.BLESSED_MERIDIANS), "A Myriad Form Body is its forms too");
		helper.assertTrue(near(c.meditationCultivationPerSecond(0), pace * 1.3 * 1.2 * Physique.Grade.IMMORTAL.speed()),
				"Both forms' pace together (and its own grade's edge)");
		c.setForms(List.of(Physique.THREE_ELEMENT_ROOTS, Physique.BLESSED_MERIDIANS));
		c.setRootElements(List.of(QiElement.FIRE, QiElement.WATER, QiElement.WOOD));
		helper.assertTrue(c.rootCount() == 3 && c.rootsAllow(QiElement.FIRE) && !c.rootsAllow(QiElement.METAL), "A form of three roots: the body's roots");
		PlayerCultivation saved = PlayerCultivation.load(c.save());
		helper.assertTrue(saved.getForms().equals(EnumSet.of(Physique.THREE_ELEMENT_ROOTS, Physique.BLESSED_MERIDIANS)), "The forms are saved");
		CompoundTag other = c.save();
		other.putString("Physique", Physique.HEAVEN_SACRED_BODY.getId());
		helper.assertTrue(PlayerCultivation.load(other).getForms().isEmpty(), "No forms but a Myriad Form Body's");

		// Born one: its forms rolled, and roots to match a form of roots.
		ServerPlayer player = survivor(helper);
		PlayerCultivation pc = CultivationManager.get(player);
		Physiques.give(player, Physique.MYRIAD_FORM_BODY);
		helper.assertTrue(pc.getForms().size() == Physique.FORMS && pc.getRootElements().size() == pc.rootCount(), "Born a Myriad Form Body: two forms, roots to match");
		Physiques.give(player, Physique.FIVE_ELEMENT_ROOTS);
		helper.assertTrue(pc.getForms().isEmpty() && pc.getRootElements().isEmpty(), "Another body: no forms");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void newBodiesShapeThePace(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		double pace = c.meditationCultivationPerSecond(0), ghost = GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY);
		// Each with its grade's small edge on top (Physique.Grade#speed: the user's pick, 2026-10-22).
		double high = Physique.Grade.HIGH.speed(), midGrade = Physique.Grade.MID.speed(), immortal = Physique.Grade.IMMORTAL.speed();
		c.setPhysique(Physique.NINE_YANG_BODY);
		helper.assertTrue(near(c.meditationCultivationPerSecond(0), pace * 1.3 * high) && near(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY), ghost * 0.5 * high),
				"Nine Yang: the heaven path x1.3, the ghost path x0.5");
		c.setPhysique(Physique.YIN_SOUL_BODY);
		helper.assertTrue(near(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY), ghost * 1.2 * midGrade) && near(c.meditationCultivationPerSecond(0), pace * midGrade),
				"Yin Soul: the ghost path x1.2");
		c.setPhysique(Physique.HEAVEN_DEFYING_BODY);
		helper.assertTrue(near(c.meditationCultivationPerSecond(0), pace * 0.75 * immortal), "Heaven-Defying: the heaven path x0.75");
		PlayerCultivation mid = new PlayerCultivation();
		mid.setState(Realm.QI_REFINING, Stage.MID, PlayerCultivation.required(Realm.QI_REFINING, Stage.MID));
		mid.setPhysique(Physique.HEAVEN_DEFYING_BODY);
		helper.assertTrue(!mid.needsHeavensLeave() && !mid.isAtRitualGate(), "...and it asks no leave of the heavens");

		// Hundred Refinements: a point more at every breakthrough.
		PlayerCultivation refined = new PlayerCultivation();
		refined.setState(Realm.FOUNDATION_BUILDING, Stage.MID, 0);
		int plain = StatPoints.earned(refined);
		refined.setPhysique(Physique.HUNDRED_REFINEMENTS_BODY);
		helper.assertTrue(StatPoints.earned(refined) == plain + PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.MID),
				"Hundred Refinements: a point more for each of its breakthroughs (" + plain + " -> " + StatPoints.earned(refined) + ")");

		// Body refining lives as long as the cultivator it is as strong as.
		PlayerCultivation body = new PlayerCultivation();
		body.setPhysique(Physique.SHATTERED_MERIDIANS);
		body.setMortalState(MortalStage.PEAK, 0);
		helper.assertTrue(Lifespan.span(body) == 100, "A mortal's Peak: a mortal's hundred years");
		body.setBodyRefining(4);
		helper.assertTrue(Lifespan.span(body) == 300, "Jade Bone: Foundation Building's years (" + Lifespan.span(body) + ")");
		body.setBodyRefining(BodyRefining.LAST);
		helper.assertTrue(Lifespan.span(body) == 700, "The Indestructible Body: Core Formation's (" + Lifespan.span(body) + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void holyStarbornAndSpiritEye(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		PlayerCultivation c = CultivationManager.get(player);
		c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		CultivationManager.refresh(player);
		float plain = player.getMaxHealth();
		double strength = player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
		c.setPhysique(Physique.ANCIENT_HOLY_BODY);
		CultivationManager.refresh(player);
		helper.assertTrue(near(player.getMaxHealth(), plain * 1.3) && player.getAttribute(Attributes.ATTACK_DAMAGE).getValue() > strength,
				"Ancient Holy: health 1.3 times, and strength (" + plain + " -> " + player.getMaxHealth() + ")");
		ServerPlayer mortal = survivor(helper);
		PlayerCultivation m = CultivationManager.get(mortal);
		Tempering.gain(mortal, 10);
		double once = m.getTempering();
		m.setPhysique(Physique.ANCIENT_HOLY_BODY);
		Tempering.gain(mortal, 10);
		helper.assertTrue(near(once, 10) && near(m.getTempering() - once, 20), "...and tempering twice as fast");

		// Starborn: the void bears it from Core Formation, space's qi whole to it, space flown 1.5 times as fast.
		ServerPlayer flier = trueSurvivor(helper);
		try {
			PlayerCultivation f = CultivationManager.get(flier);
			f.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
			helper.assertTrue(!AstralRealm.canBearTheVoid(flier) && AstralRealm.spaceQi(f) == AstralRealm.SPACE_QI, "Core Formation can't bear the void");
			float speed = AstralRealm.spaceSpeed(f);
			f.setPhysique(Physique.STARBORN_BODY);
			helper.assertTrue(AstralRealm.canBearTheVoid(flier) && AstralRealm.spaceQi(f) == 1.0 && AstralRealm.spaceSpeed(f) == AstralRealm.starborn(speed),
					"...a Starborn Body can, its qi whole there, faster through it");
		} finally {
			flier.server.getPlayerList().remove(flier);
		}

		// Spirit-Eye: souls and the dead seen from a mortal's first day.
		ServerPlayer viewer = survivor(helper);
		helper.assertTrue(!SoulProjection.perceives(viewer), "A mortal sees no soul");
		CultivationManager.get(viewer).setPhysique(Physique.SPIRIT_EYE_BODY);
		helper.assertTrue(SoulProjection.perceives(viewer), "...a Spirit-Eye Body does");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void fireJadeAndBlood(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		((ServerPlayerAccessor) player).dth$setSpawnInvulnerableTime(0);
		PlayerCultivation c = CultivationManager.get(player);
		Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 2));
		try {
			c.setPhysique(Physique.NINE_YANG_BODY);
			float full = player.getHealth();
			player.invulnerableTime = 0;
			boolean burnt = player.hurt(player.damageSources().lava(), 4);
			player.setSecondsOnFire(5);
			player.invulnerableTime = 0;
			burnt |= player.hurt(player.damageSources().onFire(), 1);
			helper.assertTrue(!burnt && player.getHealth() == full && !player.isOnFire(), "Nine Yang: no lava nor fire burns it, the flames go out");
			c.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
			player.invulnerableTime = 0;
			helper.assertTrue(player.hurt(player.damageSources().lava(), 4) && player.getHealth() < full, "An ordinary body burns");
			player.setHealth(player.getMaxHealth());

			// Jade Poison: no poison nor wither takes hold; its blows poison.
			c.setPhysique(Physique.JADE_POISON_BODY);
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
			player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100));
			helper.assertTrue(!player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.WITHER), "Jade Poison: no poison nor wither takes hold");
			pig.invulnerableTime = 0;
			pig.hurt(player.damageSources().playerAttack(player), 1);
			helper.assertTrue(pig.hasEffect(MobEffects.POISON), "...and its blow poisons");

			// Blood Demon: healed by a share of what its blows take.
			c.setPhysique(Physique.BLOOD_DEMON_BODY);
			pig.removeAllEffects();
			pig.setHealth(pig.getMaxHealth());
			player.setHealth(10);
			float pigBefore = pig.getHealth();
			pig.invulnerableTime = 0;
			pig.hurt(player.damageSources().playerAttack(player), 6);
			float taken = pigBefore - pig.getHealth();
			helper.assertTrue(taken > 0 && Math.abs(player.getHealth() - (10 + taken * Physiques.BLOOD_SHARE)) < 1.0e-4,
					"Blood Demon: healed by 15% of the " + taken + " its blow took (" + player.getHealth() + ")");
			helper.succeed();
		} finally {
			pig.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void chaosStrikesHarderAndIsHated(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer caster = survivor(helper);
		PlayerCultivation c = CultivationManager.get(caster);
		DamageSource qi = ModDamageTypes.qi(level, QiElement.FIRE, caster);
		helper.assertTrue(Physiques.outgoing(qi, 10) == 10, "An ordinary body's spell, as cast");
		c.setPhysique(Physique.PRIMORDIAL_CHAOS_BODY);
		helper.assertTrue(near(Physiques.outgoing(qi, 10), 12.5) && near(Physiques.outgoing(ModDamageTypes.ghostly(level, caster), 8), 10)
				&& Physiques.outgoing(caster.damageSources().playerAttack(caster), 10) == 10, "Primordial Chaos: its spells a quarter harder, its own blows not");
		TribulationManager.Strikes plain = TribulationManager.strikes(Realm.CORE_FORMATION, Stage.EARLY);
		TribulationManager.Strikes hated = TribulationManager.strikes(Realm.CORE_FORMATION, Stage.EARLY, true);
		helper.assertTrue(hated.count() == 9 && hated.bolt() == plain.bolt() && TribulationManager.strikes(Realm.FOUR_AXIS, Stage.EARLY, true).count() == 150,
				"...and the heavens hate it: half as many strikes again (" + hated.count() + ")");
		c.awaken(0);
		helper.assertTrue(MeditationManager.drawsBoth(caster, c), "Its meditation feeds both paths");
		c.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
		helper.assertTrue(!MeditationManager.drawsBoth(caster, c), "...another's, one");
		helper.succeed();
	}

	/** One at Foundation Building Grand Perfection, its bar full, its pill taken, every way open: ready for Core Formation's tribulation. */
	private static ServerPlayer readyForCore(GameTestHelper helper, Physique physique) {
		ServerPlayer player = survivor(helper);
		net.minecraft.world.phys.Vec3 at = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		player.teleportTo(helper.getLevel(), at.x, at.y, at.z, 0, 0);
		((ServerPlayerAccessor) player).dth$setSpawnInvulnerableTime(0);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		player.setHealth(100);
		PlayerCultivation c = CultivationManager.get(player);
		TestMethods.carryAll(c);
		c.setState(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION, 0);
		c.addCultivation(c.cultivationRequired());
		c.prepareBreakthrough(Realm.CORE_FORMATION, 0);
		c.setPhysique(physique);
		return player;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "physique_tribulation")
	public void heavenDefyingOutlivesItsTribulation(GameTestHelper helper) {
		ServerPlayer player = readyForCore(helper, Physique.HEAVEN_DEFYING_BODY);
		PlayerCultivation c = CultivationManager.get(player);
		try {
			TribulationManager.start(player);
			helper.assertTrue(TribulationManager.isActive(player.getUUID()), "It is struck as anyone is");
			for (int tick = 0; tick < 59; tick++) TribulationManager.tick(helper.getLevel().getServer());
			player.setHealth(3); // the strike would kill it
			TribulationManager.tick(helper.getLevel().getServer());
			helper.assertTrue(player.isAlive() && player.getHealth() == TribulationManager.DEFIED_HEALTH, "The killing strike leaves it one heart (" + player.getHealth() + ")");
			helper.assertTrue(!TribulationManager.isActive(player.getUUID()) && !c.isInTribulation() && c.getRealm() == Realm.FOUNDATION_BUILDING
					&& c.getStage() == Stage.GRAND_PERFECTION && c.getCultivation() == c.cultivationRequired() && c.getPreparedRealm() == Realm.CORE_FORMATION,
					"...and the tribulation fails, costing it nothing: no stage down, its cultivation and its pill kept");
			TribulationManager.start(player);
			helper.assertTrue(TribulationManager.isActive(player.getUUID()) && c.isInTribulation(), "It may face the heavens again at once");
			TribulationManager.forget(player.getUUID()); // as a logout mid-way would
			helper.assertTrue(!TribulationManager.onJoin(player) && !c.isInTribulation() && c.getStage() == Stage.GRAND_PERFECTION
					&& c.getCultivation() == c.cultivationRequired(), "Leaving mid-way and coming back: failed, and still nothing lost");
			helper.succeed();
		} finally {
			TribulationManager.forget(player.getUUID());
			helper.getLevel().getServer().getPlayerList().remove(player);
			player.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "physique_tribulation")
	public void anAncientHolyBodyTakesTheStrikesLightly(GameTestHelper helper) {
		ServerPlayer player = readyForCore(helper, Physique.ANCIENT_HOLY_BODY);
		try {
			TribulationManager.start(player);
			for (int tick = 0; tick < 60; tick++) TribulationManager.tick(helper.getLevel().getServer());
			TribulationManager.Strikes core = TribulationManager.strikes(Realm.CORE_FORMATION, Stage.EARLY);
			helper.assertTrue(Math.abs(player.getHealth() - (100 - (core.bolt() + core.magic()) * TribulationManager.HOLY_STRIKES)) < 0.01f,
					"Core Formation's strike at three quarters (" + player.getHealth() + ")");
			helper.succeed();
		} finally {
			TribulationManager.forget(player.getUUID());
			helper.getLevel().getServer().getPlayerList().remove(player);
			player.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void soulDevouringFeedsOnTheSlain(GameTestHelper helper) {
		GameRules.BooleanValue loot = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
		boolean wasLoot = loot.get();
		loot.set(false, helper.getLevel().getServer());
		ServerPlayer player = survivor(helper);
		PlayerCultivation c = CultivationManager.get(player);
		c.setState(Realm.HEAVENLY_BEING, Stage.EARLY, 0);
		c.setPhysique(Physique.SOUL_DEVOURING_BODY);
		List<CultivatorNpc> made = new ArrayList<>();
		try {
			CultivatorNpc victim = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
			made.add(victim);
			victim.becomeRogue(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 0, helper.getLevel().getRandom());
			int alignment = c.getAlignment();
			victim.hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
			helper.assertTrue(!victim.isAlive() && near(c.getCultivation(), Physiques.DEVOUR_SHARE * PlayerCultivation.required(Realm.QI_REFINING, Stage.EARLY)),
					"A cultivator slain: a tenth of what its stage takes, devoured (" + c.getCultivation() + ")");
			helper.assertTrue(c.getAlignment() <= alignment - Physiques.DEVOUR_SHIFT, "A neutral one drags its path toward the devilish (" + alignment + " -> " + c.getAlignment() + ")");
			double was = c.getCultivation();
			CultivatorNpc mortal = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(2, 2, 1));
			made.add(mortal);
			mortal.becomeRogue(CultivatorNpc.MORTAL, 0, helper.getLevel().getRandom());
			mortal.hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
			helper.assertTrue(!mortal.isAlive() && c.getCultivation() == was, "A mortal has nothing to devour");
			helper.succeed();
		} finally {
			for (CultivatorNpc npc : made) npc.discard();
			loot.set(wasLoot, helper.getLevel().getServer());
		}
	}

	/**
	 * A player slain by a Soul-Devouring Body loses a stage (the user's ask, 2026-10-20): of the higher of its paths, its cultivation
	 * there with it; a mortal has none to lose.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aPlayerDevouredFallsAStage(GameTestHelper helper) {
		ServerPlayer killer = survivor(helper);
		CultivationManager.get(killer).setState(Realm.HEAVENLY_BEING, Stage.EARLY, 0);
		CultivationManager.get(killer).setPhysique(Physique.SOUL_DEVOURING_BODY);
		ServerPlayer slain = survivor(helper);
		PlayerCultivation v = CultivationManager.get(slain);
		v.setState(Realm.FOUNDATION_BUILDING, Stage.MID, PlayerCultivation.required(Realm.FOUNDATION_BUILDING, Stage.MID) / 2);
		Physiques.devoured(slain, v, killer);
		helper.assertTrue(v.getRealm() == Realm.FOUNDATION_BUILDING && v.getStage() == Stage.EARLY && v.getCultivation() == 0,
				"Foundation Building Mid devoured: Early, its cultivation gone (" + v.getRealm() + " " + v.getStage() + ", " + v.getCultivation() + ")");
		v.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		Physiques.devoured(slain, v, killer);
		helper.assertTrue(v.getRealm() == Realm.FOUNDATION_BUILDING && v.getStage() == Stage.GRAND_PERFECTION, "An Early stage falls to the realm before's Grand Perfection");
		// The ghost path standing higher: it falls instead.
		v.setState(Realm.QI_REFINING, Stage.MID, 0);
		v.netherworld().setMethod(GhostMethod.SIX_PATHS_CANON, 1, 0);
		int ghost = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID);
		GhostMethods.setPath(v.netherworld(), ghost);
		Physiques.devoured(slain, v, killer);
		helper.assertTrue(GhostMethods.ghostRank(v.netherworld()) == ghost - 1 && v.getStage() == Stage.MID && v.getRealm() == Realm.QI_REFINING,
				"The ghost path higher: its stage lost, the heaven path's kept");
		PlayerCultivation mortal = new PlayerCultivation();
		Physiques.devoured(slain, mortal, killer);
		helper.assertTrue(mortal.isMortal(), "A mortal: nothing to lose");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anEternalReincarnationBodyKeepsEverything(GameTestHelper helper) {
		ServerPlayer p = survivor(helper);
		PlayerCultivation c = CultivationManager.own(p);
		c.setState(Realm.NASCENT_SOUL, Stage.MID, 1234);
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.NASCENT_SOUL);
		c.heaven().setMethod(fire, 5, 0);
		NetherworldState s = c.netherworld();
		s.setMethod(GhostMethod.SIX_PATHS_CANON, 1, 0);
		int ghostRank = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY);
		GhostMethods.setPath(s, ghostRank);
		s.setPath(ghostRank, 77);
		c.setStatPoints(StatPoints.Stat.values()[0], 2);
		c.setStatPoints(StatPoints.Stat.values()[2], 1);
		c.setObsession(FourAxis.Axis.LONGEVITY);
		c.setFavour(2);
		c.setPhysique(Physique.ETERNAL_REINCARNATION_BODY);
		c.setAge(300);
		int[] order = c.statPointOrder();
		int layer = c.heaven().layer(fire);
		Lifespan.wipe(p, false);
		PlayerCultivation after = CultivationManager.own(p);
		helper.assertTrue(after != c && after.getPhysique() == Physique.ETERNAL_REINCARNATION_BODY && after.age() == Lifespan.START_AGE && p.isAlive(),
				"Born again as itself, a life begun anew");
		helper.assertTrue(!after.isMortal() && after.getRealm() == Realm.NASCENT_SOUL && after.getStage() == Stage.MID && after.getCultivation() == 1234,
				"Its realm, stage and cultivation whole (the user's ask, 2026-10-22: it loses nothing)");
		helper.assertTrue(after.heaven().knows(fire) && after.heaven().layer(fire) == layer, "Its method as deep as it was (" + after.heaven().layer(fire) + " of " + layer + ")");
		NetherworldState ghost = after.netherworld();
		helper.assertTrue(ghost.knows(GhostMethod.SIX_PATHS_CANON) && GhostMethods.ghostRank(ghost) == ghostRank && ghost.pathCultivation() == 77,
				"The ghost path too, whole (" + GhostMethods.ghostRank(ghost) + ", " + ghost.pathCultivation() + ")");
		helper.assertTrue(java.util.Arrays.equals(after.statPointOrder(), order) && after.obsession() == FourAxis.Axis.LONGEVITY && after.getFavour() == 2,
				"Its stat points as they were put in, its obsession, Heaven's Favour");
		helper.assertTrue(Physique.ETERNAL_REINCARNATION_BODY.grade() == Physique.Grade.IMMORTAL && Lifespan.talent(after) == Physique.Grade.LOW.talent()
				&& Physique.ETERNAL_REINCARNATION_BODY.speed() == Physique.Grade.LOW.speed(), "For all its grade, a Low one's talent and pace");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void pillsOfLongevityAndRebirth(GameTestHelper helper) {
		ServerPlayer p = survivor(helper);
		PlayerCultivation c = CultivationManager.get(p);
		double span = Lifespan.span(c);
		ItemStack low = PillItem.create(ModItems.LONGEVITY_PILL, PillGrade.LOW, 1);
		helper.assertTrue(near(PillItem.longevityYears(low), PillItem.YEARS_PER_POTENCY), "A Low-Grade Longevity Pill: 20 years");
		helper.assertTrue(PillItem.longevityYears(PillItem.create(ModItems.LONGEVITY_PILL, PillGrade.HIGH, 1)) > PillItem.longevityYears(low)
				&& PillItem.longevityYears(PillItem.create(ModItems.LONGEVITY_PILL, PillGrade.IMMORTAL, 1)) > PillItem.longevityYears(PillItem.create(ModItems.LONGEVITY_PILL, PillGrade.HIGH, 1)),
				"The higher its grade, the more years");
		low.finishUsingItem(p.level(), p);
		helper.assertTrue(near(Lifespan.span(c), span + 20) && c.longevityPills() == 1 && low.isEmpty(), "Eaten: 20 years more (" + Lifespan.span(c) + ")");
		PillItem.create(ModItems.LONGEVITY_PILL, PillGrade.LOW, 1).finishUsingItem(p.level(), p);
		helper.assertTrue(near(Lifespan.span(c), span + 20 + 20 * PillItem.LONGEVITY_FADE) && PillItem.LONGEVITY_FADE == 0.5,
				"The next one this life, half as much (" + Lifespan.span(c) + ")");
		helper.assertTrue(PlayerCultivation.load(c.save()).longevityPills() == 2, "Their count is saved");

		// The Rebirth Pill: eaten only sneaking; then the life's cultivation ends whole, alive, a body born anew.
		TestMethods.setAt(c, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		c.setPhysique(Physique.CURSED_MERIDIANS);
		ItemStack rebirth = new ItemStack(ModItems.REBIRTH_PILL, 2);
		p.setItemInHand(InteractionHand.MAIN_HAND, rebirth);
		helper.assertTrue(rebirth.use(p.level(), p, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL && !p.isUsingItem(), "Not sneaking: not eaten");
		p.setShiftKeyDown(true);
		try {
			helper.assertTrue(rebirth.use(p.level(), p, InteractionHand.MAIN_HAND).getResult().consumesAction() && p.isUsingItem(), "Sneaking: eaten");
			p.stopUsingItem();
			rebirth.finishUsingItem(p.level(), p);
		} finally {
			p.setShiftKeyDown(false);
		}
		PlayerCultivation after = CultivationManager.own(p);
		helper.assertTrue(after != c && after.isMortal() && after.age() == Lifespan.START_AGE && after.heaven().known().isEmpty() && after.longevityPills() == 0
				&& after.extraYears() == 0 && p.isAlive() && rebirth.getCount() == 1, "Born anew: a mortal of 16, its years and methods gone, alive, one pill fewer");
		// It has no hold on an Eternal Reincarnation Body (the user's call, 2026-10-22): not eaten, sneaking or not, nothing changed.
		TestMethods.setAt(after, Realm.FOUNDATION_BUILDING, Stage.MID);
		after.setPhysique(Physique.ETERNAL_REINCARNATION_BODY);
		after.setAge(200);
		p.setShiftKeyDown(true);
		try {
			helper.assertTrue(rebirth.use(p.level(), p, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL && !p.isUsingItem(),
					"An Eternal Reincarnation Body won't eat it, sneaking or not");
			rebirth.finishUsingItem(p.level(), p);
		} finally {
			p.setShiftKeyDown(false);
		}
		helper.assertTrue(CultivationManager.own(p) == after && after.getRealm() == Realm.FOUNDATION_BUILDING && after.getStage() == Stage.MID
				&& after.age() == 200 && rebirth.getCount() == 1, "...and nothing comes of it: the same life, the pill kept");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void rebirthPillsLieOnlyInTheUpperRealm(GameTestHelper helper) {
		var table = helper.getLevel().getServer().getLootData().getLootTable(SectPiece.TREASURY_LOOT);
		int here = 0;
		for (int i = 0; i < 400; i++) here += pills(table, helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
		helper.assertTrue(here == 0, "No sect treasury of the lower realms keeps one (" + here + ")");
		ServerLevel upper = helper.getLevel().getServer().getLevel(ModDimensions.UPPER_REALM);
		if (upper != null) {
			int there = 0;
			for (int i = 0; i < 2000; i++) there += pills(table, upper, new net.minecraft.world.phys.Vec3(0, 120, 0));
			helper.assertTrue(there >= 10 && there <= 90, "One Upper Realm treasury chest in fifty keeps one (" + there + " in 2000)");
		}
		helper.succeed();
	}

	private static int pills(net.minecraft.world.level.storage.loot.LootTable table, ServerLevel level, net.minecraft.world.phys.Vec3 at) {
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, at).create(LootContextParamSets.CHEST);
		int count = 0;
		for (ItemStack stack : table.getRandomItems(params)) if (stack.is(ModItems.REBIRTH_PILL)) count += stack.getCount();
		return count;
	}

	/** The settlers of {@code well} standing within its home ground. */
	private static List<CultivatorNpc> settlersOf(ServerLevel level, BlockPos well) {
		int reach = Settlements.HOME_REACH;
		return level.getEntitiesOfClass(CultivatorNpc.class, new AABB(well).inflate(reach, 32, reach), npc -> npc.isAlive() && well.equals(npc.settledAt()));
	}

	/** Whether all of {@code well}'s home ground ticks its beings (Settlements#replenish counts its people only then). */
	private static boolean homeTicks(ServerLevel level, BlockPos well) {
		int reach = Settlements.HOME_REACH;
		for (int dx = -reach; dx <= reach; dx += reach) {
			for (int dz = -reach; dz <= reach; dz += reach) if (!level.isPositionEntityTicking(well.offset(dx, 0, dz))) return false;
		}
		return true;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "physique_settlement", timeoutTicks = 400)
	public void settlementsAreKeptAlive(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Off to the side, on a stone floor of its own (the test world under the tests may be sea), everything put back after.
		BlockPos centre = helper.absolutePos(new BlockPos(24, 1, 24));
		int ground = centre.getY();
		Map<BlockPos, BlockState> before = new HashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-20, 0, -20).atY(ground - 10), centre.offset(20, 0, 20).atY(ground + 10))) {
			before.put(pos.immutable(), level.getBlockState(pos));
		}
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-20, 0, -20).atY(ground - 1), centre.offset(20, 0, 20).atY(ground + 10))) {
			level.setBlock(pos, pos.getY() == ground - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
		}
		Settlements.Built built = Settlements.build(level, centre.getX(), centre.getZ(), ground + 10, Settlements.OAK, RandomSource.create(42));
		helper.assertTrue(built != null, "Built");
		BlockPos well = built.well();
		// All its home ground in the world's reach, so its people are counted.
		List<long[]> forced = new ArrayList<>();
		int reach = Settlements.HOME_REACH + 16;
		for (int cx = (well.getX() - reach) >> 4; cx <= (well.getX() + reach) >> 4; cx++) {
			for (int cz = (well.getZ() - reach) >> 4; cz <= (well.getZ() + reach) >> 4; cz++) {
				if (level.setChunkForced(cx, cz, true)) forced.add(new long[] {cx, cz});
			}
		}
		Runnable cleanUp = () -> {
			for (CultivatorNpc npc : settlersOf(level, well)) npc.discard();
			for (CultivatorNpc npc : built.settlers()) npc.discard();
			before.forEach((pos, state) -> {
				if (level.getBlockState(pos) != state) level.setBlock(pos, state, 2);
			});
			for (long[] chunk : forced) level.setChunkForced((int) chunk[0], (int) chunk[1], false);
		};
		// Waits for the forced ground to come into reach (it may first have to be made), where one strays too, then checks once.
		BlockPos strayAt = well.offset(Settlements.HOME_REACH + 8, 0, 0);
		int[] waited = {0};
		boolean[] ran = {false};
		String[] failure = {null};
		helper.succeedWhen(() -> {
			if (failure[0] != null) throw new net.minecraft.gametest.framework.GameTestAssertException(failure[0]);
			if (ran[0]) return;
			if (!homeTicks(level, well) || !level.isPositionEntityTicking(strayAt)) {
				if (++waited[0] >= 300) {
					ran[0] = true;
					failure[0] = "Its home ground never came into reach";
					cleanUp.run();
				}
				throw new net.minecraft.gametest.framework.GameTestAssertException("Its home ground is still coming into reach");
			}
			ran[0] = true;
			try {
				Settlements.Settlement s = Settlements.Settlement.of(built);
				int people = s.people();
				// No monster born near it.
				Settlements.SettlementData data = new Settlements.SettlementData();
				data.put(Settlements.cellKey(Mth.floor(well.getX() / (double) Settlements.CELL), Mth.floor(well.getZ() / (double) Settlements.CELL)), s);
				helper.assertTrue(Settlements.sheltered(data, well.getX() + 30.5, well.getZ() + 0.5) && !Settlements.sheltered(data, well.getX() + 100.5, well.getZ() + 0.5),
						"No monster born within 64 blocks of its well; past that, as anywhere");
				helper.assertTrue(!Settlements.shelters(level, well), "(Only on the Astral Realm's worlds)");

				long now = level.getGameTime();
				RandomSource random = RandomSource.create(5);
				helper.assertTrue(Settlements.replenish(level, s, now, random).equals(s) && settlersOf(level, well).size() == people, "Whole: nothing to do");
				// One strayed past its home ground a while (off after what struck it, say) is still one of its people: no newcomer for it.
				CultivatorNpc stray = built.settlers().get(2);
				stray.moveTo(strayAt.getX() + 0.5, ground, strayAt.getZ() + 0.5);
				helper.assertTrue(Settlements.replenish(level, s, now, random).equals(s), "A settler strayed past its home ground still counts ("
						+ level.getEntitiesOfClass(CultivatorNpc.class, new AABB(well).inflate(Settlements.CENSUS, 64, Settlements.CENSUS),
								npc -> npc.isAlive() && well.equals(npc.settledAt())).size() + " of " + people + ", the stray "
						+ (stray.isAlive() ? "alive" : "gone") + (stray.isRemoved() ? ", removed: " + stray.getRemovalReason() : "") + ")");
				stray.moveTo(well.getX() + 2.5, ground, well.getZ() + 0.5);
				built.settlers().get(0).discard();
				built.settlers().get(1).discard();
				Settlements.Settlement shortNow = Settlements.replenish(level, s, now, random);
				helper.assertTrue(shortNow.shortSince() == now && settlersOf(level, well).size() == people - 2, "Two lost: short from now, none come yet");
				Settlements.Settlement later = Settlements.replenish(level, shortNow, now + Settlements.ARRIVAL_TICKS, random);
				List<CultivatorNpc> living = settlersOf(level, well);
				helper.assertTrue(living.size() == people - 1 && later.shortSince() == now + Settlements.ARRIVAL_TICKS, "Two days on, a newcomer");
				helper.assertTrue(living.stream().allMatch(npc -> npc.wish() != null && npc.isPersistenceRequired()), "...with a wish, kept, as its people are");
				Settlements.Settlement whole = Settlements.replenish(level, later, now + 10 * Settlements.ARRIVAL_TICKS, random);
				helper.assertTrue(settlersOf(level, well).size() == people && whole.shortSince() == -1, "Long after: the rest at once, whole again");
			} catch (net.minecraft.gametest.framework.GameTestAssertException e) {
				failure[0] = e.getMessage();
				throw e;
			} finally {
				cleanUp.run();
			}
		});
	}
}
