package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Body Path's techniques, phase 2 (2026-11-01: six of them, vigor, their unlocks by realm; then five for the realms above, the pool
 * growing with the body): awakened by the body's realm alone, a mortal's too, never the dead's; vigor spent, refused short, refilled by food
 * and rest, its pool by the realm; Iron Skin's and Golden Body's guard; the stomp's reach; the fist's blow and Blood Frenzy's burn; the leap
 * thrown ahead, its fall safe till it lands; Diamond Will, Rebounding Bones, Flesh Reborn, the Heaven-Shattering Fist's cone and Comet Charge;
 * a mortal on the Body Path weighed by its body.
 */
public class BodyTechniquesGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6 * Math.max(1, Math.abs(b));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theTechniquesAwakenByTheBodysRealmAloneAMortalsToo(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setMortal(true);
		helper.assertTrue(!Ability.IRON_SKIN.isUnlocked(c), "No Body Path: no technique");
		c.body().set(0, 0);
		helper.assertTrue(Ability.IRON_SKIN.isUnlocked(c) && !Ability.MOUNTAIN_STOMP.isUnlocked(c), "Copper Skin: Iron Skin alone, a mortal's too");
		c.body().set(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY), 0);
		for (Ability technique : BodyTechniques.TECHNIQUES) {
			boolean awake = BodyTechniques.awakensAt(technique).ordinal() <= Realm.FOUR_AXIS.ordinal();
			helper.assertTrue(technique.isUnlocked(c) == awake, "Golden Blood: " + technique.getId() + (awake ? " awake" : " still asleep"));
		}
		helper.assertTrue(!Ability.QI_SENSE.isUnlocked(c) && !Ability.CONSCIOUSNESS_DOMAIN.isUnlocked(c), "...but none of the qi realms' abilities");
		c.netherworld().setGhost(true);
		helper.assertTrue(!Ability.IRON_SKIN.isUnlocked(c), "The dead have no flesh to wield them");
		c.netherworld().setGhost(false);
		c.body().set(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), 0);
		for (Ability technique : BodyTechniques.TECHNIQUES) {
			boolean awake = BodyTechniques.awakensAt(technique).ordinal() <= Realm.TRUE_IMMORTAL.ordinal(); // the ladder's own wait (BodyHeavenTechniquesGameTests)
			helper.assertTrue(technique.isUnlocked(c) == awake, "The Indestructible Body: " + technique.getId() + (awake ? " awake" : " still asleep"));
		}
		helper.assertTrue(near(BodyTechniques.ironSkinCut(c), BodyTechniques.IRON_SKIN_CUT_MOST) && BodyTechniques.stompRadius(c) > BodyTechniques.STOMP_RADIUS
				&& BodyTechniques.fistFactor(c) > BodyTechniques.FIST_FACTOR && BodyTechniques.goldenTicks(c) > BodyTechniques.GOLDEN_TICKS
				&& BodyTechniques.diamondTicks(c) > BodyTechniques.DIAMOND_TICKS && BodyTechniques.cometBlocks(c) > BodyTechniques.COMET_BLOCKS
				&& BodyTechniques.reboundShare(c) > BodyTechniques.REBOUND_SHARE && BodyTechniques.rebornNow(c) > BodyTechniques.REBORN_NOW,
				"They grow with the body's realm");
		helper.assertTrue(near(BodyTechniques.maxVigor(0), 100) && near(BodyTechniques.maxVigor(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY)), 150)
				&& near(BodyTechniques.maxVigor(c), 200), "The pool grows with the body: 100 at Copper Skin, 150 at Golden Blood, 200 at the top");
		helper.assertTrue(near(c.body().vigor(), BodyTechniques.VIGOR), "Vigor as it was (no more for the realm reached: rest fills the rest)");
		c.body().setVigor(180);
		helper.assertTrue(near(c.body().vigor(), 180), "The Indestructible Body's pool holds 180");
		c.body().setVigor(37.5);
		helper.assertTrue(near(PlayerCultivation.load(c.save()).body().vigor(), 37.5), "Vigor saved and loaded");
		c.body().setVigor(180);
		c.body().set(0, 0);
		helper.assertTrue(near(c.body().vigor(), 100), "Fallen to Copper Skin: the pool holds no more than its own");
		PlayerCultivation plain = new PlayerCultivation();
		plain.setMortal(true);
		helper.assertTrue(TwoPaths.weighedRank(plain) == -1 && TwoPaths.weighedRank(c) >= 0, "Others weigh a mortal on the Body Path by its body, one off it as a mortal");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ironSkinSpendsVigorAndTakesTheStingOutOfBlows(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-iron", Realm.QI_REFINING);
			PlayerCultivation c = CultivationManager.own(p);
			Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
			DamageSource blow = p.damageSources().mobAttack(zombie);
			helper.assertTrue(near(HeavenSpells.incoming(p, blow, 10), 10), "No technique: the blow whole");
			CultivationManager.toggleAbility(p, Ability.IRON_SKIN.ordinal());
			helper.assertTrue(near(c.body().vigor(), BodyTechniques.VIGOR - BodyTechniques.IRON_SKIN_COST) && c.isAbilityEnabled(Ability.IRON_SKIN),
					"Iron Skin: its vigor spent, on");
			helper.assertTrue(near(HeavenSpells.incoming(p, blow, 10), 10 * (1 - BodyTechniques.IRON_SKIN_CUT)), "...a blow harms 40% less");
			helper.assertTrue(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1, "...and nothing knocks it back");
			helper.assertTrue(near(HeavenSpells.incoming(p, p.damageSources().fall(), 10), 10), "...a fall is no blow");
			CultivationManager.toggleAbility(p, Ability.IRON_SKIN.ordinal());
			helper.assertTrue(near(c.body().vigor(), BodyTechniques.VIGOR - BodyTechniques.IRON_SKIN_COST), "Again at once: refused, cooling down");
			BodyTechniques.readyAgain(p.getUUID());
			c.body().setVigor(10);
			CultivationManager.toggleAbility(p, Ability.IRON_SKIN.ordinal());
			helper.assertTrue(near(c.body().vigor(), 10), "Short of vigor: refused");
			BodyTechniques.ate(p, 8);
			helper.assertTrue(near(c.body().vigor(), 10 + 8 * BodyTechniques.maxVigor(c) * BodyTechniques.FOOD_SHARE), "A steak eaten: 24 vigor");
			p.tickCount = 1000; // no blow given or taken this long: resting
			BodyTechniques.second(p);
			double rested = 34 + BodyTechniques.maxVigor(c) * BodyTechniques.REST_SHARE;
			helper.assertTrue(near(c.body().vigor(), rested), "Rest: a hundredth of the pool a second");
			p.setSprinting(true);
			BodyTechniques.second(p);
			helper.assertTrue(near(c.body().vigor(), rested), "...not while sprinting");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void goldenBodyTurnsEveryBlowAsideAndTheStompStrikesAllRound(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-gold", Realm.FOUR_AXIS);
			PlayerCultivation c = CultivationManager.own(p);
			Zombie close = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
			Zombie distant = helper.spawn(EntityType.ZOMBIE, new BlockPos(16, 2, 1));
			CultivationManager.toggleAbility(p, Ability.GOLDEN_BODY.ordinal());
			helper.assertTrue(HeavenSpells.incoming(p, p.damageSources().mobAttack(close), 50) == 0 && HeavenSpells.wardedOff(p),
					"Golden Body: the blow turned aside, refused outright");
			float closeHealth = close.getHealth(), distantHealth = distant.getHealth();
			CultivationManager.toggleAbility(p, Ability.MOUNTAIN_STOMP.ordinal());
			helper.assertTrue(close.getHealth() < closeHealth && close.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The stomp strikes and slows a foe near");
			helper.assertTrue(distant.getHealth() == distantHealth, "...not one beyond its reach (" + BodyTechniques.stompRadius(c) + " blocks)");
			helper.assertTrue(near(c.body().vigor(), BodyTechniques.VIGOR - BodyTechniques.GOLDEN_COST - BodyTechniques.STOMP_COST), "Both spent their vigor");
			p.setOnGround(false);
			BodyTechniques.readyAgain(p.getUUID());
			double vigor = c.body().vigor();
			CultivationManager.toggleAbility(p, Ability.MOUNTAIN_STOMP.ordinal());
			helper.assertTrue(near(c.body().vigor(), vigor), "No stomp without ground underfoot");
			distant.discard(); // outside the test's bounds: not swept away with it
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theFistAndBloodFrenzyStrikeHarder(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-frenzy", Realm.HEAVENLY_BEING);
			PlayerCultivation c = CultivationManager.own(p);
			Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
			DamageSource hand = p.damageSources().playerAttack(p);
			helper.assertTrue(near(BodyTechniques.outgoing(zombie, hand, 10), 10), "No technique: the blow as it is");
			CultivationManager.toggleAbility(p, Ability.THOUSAND_POUND_FIST.ordinal());
			double factor = BodyTechniques.fistFactor(c);
			helper.assertTrue(c.isAbilityEnabled(Ability.THOUSAND_POUND_FIST) && near(BodyTechniques.outgoing(zombie, hand, 10), 10 * factor),
					"The fist primed: its blow x" + factor);
			helper.assertTrue(near(BodyTechniques.outgoing(zombie, hand, 10), 10) && !c.isAbilityEnabled(Ability.THOUSAND_POUND_FIST), "...once");
			double speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
			CultivationManager.toggleAbility(p, Ability.BLOOD_FRENZY.ordinal());
			helper.assertTrue(c.isAbilityEnabled(Ability.BLOOD_FRENZY) && near(BodyTechniques.outgoing(zombie, hand, 10), 10 * (1 + BodyTechniques.FRENZY_HARM))
					&& p.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed * 1.2, "Blood Frenzy: blows 30% harder, and swifter");
			helper.assertTrue(near(BodyTechniques.outgoing(zombie, ModDamageTypes.bodyArt(p.level(), p), 10), 10 * (1 + BodyTechniques.FRENZY_HARM))
					&& near(BodyTechniques.outgoing(zombie, p.damageSources().magic(), 10), 10), "...its techniques too, not what isn't the body's");
			double vigor = c.body().vigor();
			p.tickCount = 1000;
			BodyTechniques.second(p);
			helper.assertTrue(near(c.body().vigor(), vigor - BodyTechniques.FRENZY_PER_SECOND), "It burns its vigor every second, none refilled");
			c.body().setVigor(1);
			BodyTechniques.second(p);
			helper.assertTrue(!c.isAbilityEnabled(Ability.BLOOD_FRENZY) && near(p.getAttributeValue(Attributes.MOVEMENT_SPEED), speed),
					"Its vigor run dry: it goes out, its swiftness with it");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theLeapThrowsOneAheadAndItsFallIsSafeTillItLands(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-leap", Realm.CORE_FORMATION);
			PlayerCultivation c = CultivationManager.own(p);
			p.setYRot(0);
			p.setXRot(0);
			CultivationManager.toggleAbility(p, Ability.GROUND_SPLITTING_LEAP.ordinal());
			Vec3 v = p.getDeltaMovement();
			helper.assertTrue(v.y > 0.5 && v.z > 1 && Math.abs(v.x) < 1.0e-6, "Thrown ahead where it looks, and up (" + v + ")");
			helper.assertTrue(BodyTechniques.fallIsSafe(p) && c.isAbilityEnabled(Ability.GROUND_SPLITTING_LEAP), "On the leap");
			((com.example.defyingtheheavens.mixin.ServerPlayerAccessor) p).dth$setSpawnInvulnerableTime(0); // just joined: vanilla shields it 3 s
			float health = p.getHealth();
			p.hurt(p.damageSources().fall(), 5);
			helper.assertTrue(p.getHealth() == health, "Its fall does no harm");
			BodyTechniques.tick(p.server);
			helper.assertTrue(BodyTechniques.fallIsSafe(p), "Still on the ground (the client not yet off it): not landed");
			p.setOnGround(false);
			BodyTechniques.tick(p.server);
			p.setOnGround(true);
			BodyTechniques.tick(p.server);
			helper.assertTrue(!BodyTechniques.fallIsSafe(p) && !c.isAbilityEnabled(Ability.GROUND_SPLITTING_LEAP), "Off the ground, then down: landed");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void diamondWillClearsHarmAndHoldsItOff(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-diamond", Realm.DAO_INTEGRATION);
			PlayerCultivation c = CultivationManager.own(p);
			p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
			helper.assertTrue(p.hasEffect(MobEffects.POISON) && !BodyTechniques.unshaken(p), "Poisoned, the heart not yet diamond");
			CultivationManager.toggleAbility(p, Ability.DIAMOND_WILL.ordinal());
			helper.assertTrue(!p.hasEffect(MobEffects.POISON) && BodyTechniques.unshaken(p) && c.isAbilityEnabled(Ability.DIAMOND_WILL),
					"Diamond Will: the poison gone, no one's pressure on it");
			p.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
			p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
			helper.assertTrue(!p.hasEffect(MobEffects.WITHER) && p.hasEffect(MobEffects.MOVEMENT_SPEED), "...no harm takes hold, a boon still does");
			helper.assertTrue(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1, "...and nothing knocks it back");
			BodyTechniques.end(p);
			helper.assertTrue(!BodyTechniques.unshaken(p) && p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1, "Ended: as it was");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void reboundingBonesStrikeABlowBackOnItsDealer(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-bones", Realm.SACRED_VESSEL);
			Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
			float before = zombie.getHealth();
			HeavenSpells.incoming(p, p.damageSources().mobAttack(zombie), 10);
			helper.assertTrue(zombie.getHealth() == before, "No technique: nothing struck back");
			CultivationManager.toggleAbility(p, Ability.REBOUNDING_BONES.ordinal());
			float taken = HeavenSpells.incoming(p, p.damageSources().mobAttack(zombie), 10);
			helper.assertTrue(near(taken, 10), "Rebounding Bones: the blow still taken whole");
			helper.assertTrue(zombie.getHealth() < before, "...and half of it struck back on its dealer (" + (before - zombie.getHealth()) + ")");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void fleshRebornHealsAtOnceAndOverTime(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-reborn", Realm.ENTERING_NIRVANA);
			PlayerCultivation c = CultivationManager.own(p);
			float max = p.getMaxHealth();
			p.setHealth(max * 0.1f);
			p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
			p.setSecondsOnFire(5);
			CultivationManager.toggleAbility(p, Ability.FLESH_REBORN.ordinal());
			helper.assertTrue(Math.abs(p.getHealth() - max * (0.1 + BodyTechniques.REBORN_NOW)) < max * 0.01 && !p.hasEffect(MobEffects.POISON) && !p.isOnFire()
					&& c.isAbilityEnabled(Ability.FLESH_REBORN), "Flesh Reborn: 30% back at once, poison and fire gone (" + p.getHealth() / max + ")");
			for (int i = 0; i < 20; i++) BodyTechniques.tick(p.server);
			helper.assertTrue(p.getHealth() > max * (0.1 + BodyTechniques.REBORN_NOW + 0.04), "...and more growing back (" + p.getHealth() / max + ")");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theShatteringFistStrikesTheConeBeforeIt(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-shatter", Realm.TRUE_IMMORTAL);
			p.setYRot(0);
			p.setXRot(0);
			Zombie ahead = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 8));
			Zombie behind = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, -3));
			Zombie aside = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, 2, 1));
			float a = ahead.getHealth(), b = behind.getHealth(), s = aside.getHealth();
			CultivationManager.toggleAbility(p, Ability.HEAVEN_SHATTERING_FIST.ordinal());
			helper.assertTrue(ahead.getHealth() < a, "The fist strikes the foe seven blocks before it");
			helper.assertTrue(behind.getHealth() == b && aside.getHealth() == s, "...not the one behind, nor the one aside");
			behind.discard(); // outside the test's bounds: not swept away with it
			aside.discard();
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void cometChargeThrowsOneAheadStrikingWhatItPasses(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-comet", Realm.VOID_SHATTERING);
			p.setYRot(0);
			p.setXRot(0);
			Zombie inWay = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
			float before = inWay.getHealth();
			CultivationManager.toggleAbility(p, Ability.COMET_CHARGE.ordinal());
			Vec3 v = p.getDeltaMovement();
			helper.assertTrue(v.z > 2 && v.y > 0 && Math.abs(v.x) < 1.0e-6 && BodyTechniques.fallIsSafe(p), "Comet Charge: hurled along the ground ahead (" + v + ")");
			BodyTechniques.tick(p.server);
			helper.assertTrue(inWay.getHealth() < before, "...striking the foe in its way");
			float once = inWay.getHealth();
			inWay.invulnerableTime = 0;
			BodyTechniques.tick(p.server);
			helper.assertTrue(inWay.getHealth() == once, "...once only");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	// --- Helpers ---

	/** A survival mortal at Peak in the player list, begun on the Body Path and set at {@code realm}'s Early stage on it. */
	private static ServerPlayer bodyCultivator(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm) {
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
		PlayerCultivation c = CultivationManager.own(p);
		c.setMortal(true);
		c.setMortalState(MortalStage.PEAK, 0);
		BodyPath.begin(p);
		c.body().set(PlayerCultivation.rank(realm, Stage.EARLY), 0);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			BodyTechniques.end(p);
			BodyTechniques.forget(p.getUUID());
			BodyPath.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
