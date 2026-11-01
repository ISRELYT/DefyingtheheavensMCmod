package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * The Body Path's techniques of the ladder past the Indestructible Body (the user's "Go, all three parts", 2026-11-01, part three): one
 * awakened at each of its realms; Earthshaker's rings running out and striking what they pass under; the Sky-Pillar Strike's throw, its turn
 * down onto where one looked and its slam; Imperishable Gold turning every blow and harmful effect aside, then bursting; Primordial Roar
 * stripping wards and the body's guards and stunning; Myriad Fists' flurry; the Dharma Body of Heaven and Earth standing behind one, its
 * fist coming down where one's blow lands and its foot stamping as one stomps. The far-reaching ones on floors of their own, in batches of
 * their own (HeavenSpellTesting#floor).
 */
public class BodyHeavenTechniquesGameTests implements FabricGameTest {
	private static final Ability[] LADDER = {Ability.EARTHSHAKER, Ability.SKY_PILLAR_STRIKE, Ability.IMPERISHABLE_GOLD, Ability.PRIMORDIAL_ROAR,
			Ability.MYRIAD_FISTS, Ability.DHARMA_BODY};
	private static final Realm[] REALMS = {Realm.EARTH_IMMORTAL, Realm.HEAVEN_IMMORTAL, Realm.GOLDEN_IMMORTAL, Realm.TAIYI_GOLDEN_IMMORTAL,
			Realm.PROFOUND_SOVEREIGN, Realm.HEAVENLY_LORD};

	@GameTest(template = EMPTY_STRUCTURE)
	public void theLadderTechniquesAwakenRealmByRealm(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setMortal(true);
		c.body().set(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION), 0);
		for (Ability technique : LADDER) helper.assertTrue(!technique.isUnlocked(c), "The Indestructible Body: not yet " + technique.getId());
		for (int i = 0; i < REALMS.length; i++) {
			c.body().set(PlayerCultivation.rank(REALMS[i], Stage.EARLY), 0);
			for (int j = 0; j < LADDER.length; j++) {
				helper.assertTrue(LADDER[j].isUnlocked(c) == j <= i, REALMS[i].getId() + ": " + LADDER[j].getId() + (j <= i ? " awake" : " asleep"));
			}
			helper.assertTrue(BodyTechniques.awakensAt(LADDER[i]) == REALMS[i] && BodyTechniques.cost(LADDER[i]) > 0 && BodyTechniques.cooldown(LADDER[i]) > 0,
					LADDER[i].getId() + " awakens at its own realm, spends vigor and waits");
		}
		helper.assertTrue(BodyTechniques.maxVigor(c) >= BodyTechniques.DHARMA_COST && BodyTechniques.earthshakerReach(c) > BodyTechniques.EARTHSHAKER_REACH
				&& BodyTechniques.pillarRadius(c) > BodyTechniques.PILLAR_RADIUS && BodyTechniques.imperishableTicks(c) > BodyTechniques.GOLD_TICKS
				&& BodyTechniques.roarReach(c) > BodyTechniques.ROAR_REACH, "At the Body of Heaven and Earth: the pool for them all, and each grown");
		c.netherworld().setGhost(true);
		helper.assertTrue(!Ability.DHARMA_BODY.isUnlocked(c), "The dead have no body to tower");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "body_earthshaker", timeoutTicks = 200)
	public void earthshakersRingsRunOutAndTossWhatTheyPassUnder(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 20, 8);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = bodyCultivator(helper, "body-shaker", Realm.EARTH_IMMORTAL, floor.centre());
		Vec3 at = p.position();
		Husk near = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(3, 0, 0), 1.0e6, spawned);
		Husk middle = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(0, 0, 8), 1.0e6, spawned);
		Husk outer = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(-13, 0, 0), 1.0e6, spawned);
		Husk beyond = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(0, 0, -18.5), 1.0e6, spawned);
		PlayerCultivation c = CultivationManager.own(p);
		CultivationManager.toggleAbility(p, Ability.EARTHSHAKER.ordinal());
		helper.assertTrue(BodyTechniques.holding(p, Ability.EARTHSHAKER) && near(c.body().vigor(), BodyTechniques.maxVigor(c) - BodyTechniques.EARTHSHAKER_COST),
				"Earthshaker: its vigor spent, its rings under way");
		helper.runAfterDelay(BodyTechniques.EARTHSHAKER_EVERY * BodyTechniques.EARTHSHAKER_RINGS + 4, () -> {
			try {
				helper.assertTrue(hurt(near) && hurt(middle) && hurt(outer), "Its three rings struck what each passed under (" + near.getHealth() + ", "
						+ middle.getHealth() + ", " + outer.getHealth() + ")");
				helper.assertTrue(!hurt(beyond), "...not what stood beyond the last");
				helper.assertTrue(!BodyTechniques.holding(p, Ability.EARTHSHAKER), "...and they have all run out");
				helper.succeed();
			} finally {
				leave(p, spawned, floor);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "body_sky_pillar", timeoutTicks = 200)
	public void theSkyPillarThrowsOneUpAndSlamsItDown(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 14, 8);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = bodyCultivator(helper, "body-pillar", Realm.HEAVEN_IMMORTAL, floor.centre());
		Vec3 at = p.position();
		HeavenSpellTesting.lookAt(p, at.add(0, 0, 10));
		Husk close = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(2, 0, 0), 1.0e6, spawned);
		Husk apart = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(-12, 0, 0), 1.0e6, spawned);
		CultivationManager.toggleAbility(p, Ability.SKY_PILLAR_STRIKE.ordinal());
		helper.assertTrue(BodyTechniques.holding(p, Ability.SKY_PILLAR_STRIKE) && near(p.getDeltaMovement().y, BodyTechniques.PILLAR_UP)
				&& p.getDeltaMovement().z > 0 && BodyTechniques.fallIsSafe(p), "Sky-Pillar Strike: thrown up and toward where one looks, safe from the fall");
		helper.runAfterDelay(2, () -> p.setOnGround(false)); // off the ground (a mock player isn't moved: its throw is the server's to read)
		helper.runAfterDelay(BodyTechniques.PILLAR_APEX + 3, () -> {
			Vec3 v = p.getDeltaMovement();
			if (!(v.z > 0 && near(v.length(), BodyTechniques.PILLAR_SLAM))) {
				leave(p, spawned, floor);
				helper.fail("Near the top of its throw, hurled at where it looked (" + v + ")");
				return;
			}
			p.setOnGround(true); // down again
			helper.runAfterDelay(3, () -> {
				try {
					helper.assertTrue(!BodyTechniques.holding(p, Ability.SKY_PILLAR_STRIKE) && hurt(close), "Down: the slam strikes all round where it lands");
					helper.assertTrue(!hurt(apart), "...not beyond its reach");
					helper.succeed();
				} finally {
					leave(p, spawned, floor);
				}
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "body_imperishable", timeoutTicks = 400)
	public void imperishableGoldTurnsEverythingAsideThenBurstsWithIt(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 8);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = bodyCultivator(helper, "body-gold", Realm.GOLDEN_IMMORTAL, floor.centre());
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, p.position().add(3, 0, 0), 1.0e6, spawned);
		CultivationManager.toggleAbility(p, Ability.IMPERISHABLE_GOLD.ordinal());
		DamageSource blow = p.damageSources().mobAttack(husk);
		helper.assertTrue(HeavenSpells.incoming(p, blow, 40) == 0 && HeavenSpells.wardedOff(p) && near(BodyTechniques.turned(p), 40),
				"Imperishable Gold: a blow turned aside whole, and kept for its burst");
		helper.assertTrue(near(HeavenSpells.incoming(p, p.damageSources().fall(), 5), 5), "...a fall is no blow");
		helper.assertTrue(!p.addEffect(new MobEffectInstance(MobEffects.POISON, 100)) && !p.hasEffect(MobEffects.POISON), "...and no harmful effect takes hold");
		helper.assertTrue(goldHold(p), "...nothing knocks one back (its own hold, beside the realm's)");
		float before = husk.getHealth();
		helper.runAfterDelay(BodyTechniques.imperishableTicks(CultivationManager.own(p)) + 3, () -> {
			try {
				helper.assertTrue(!BodyTechniques.holding(p, Ability.IMPERISHABLE_GOLD) && before - husk.getHealth() >= 30,
						"Run out, what it turned aside burst out on those near (" + (before - husk.getHealth()) + ")");
				helper.assertTrue(!goldHold(p), "...and its hold against knockback is gone");
				helper.succeed();
			} finally {
				leave(p, spawned, floor);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "body_roar", timeoutTicks = 100)
	public void primordialRoarStripsWardsAndStuns(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 20, 8);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = bodyCultivator(helper, "body-roar", Realm.TAIYI_GOLDEN_IMMORTAL, floor.centre());
		Vec3 at = p.position();
		Husk warded = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(5, 0, 0), 1.0e6, spawned);
		SpellAuras.put(warded, SpellAura.VAJRA_BODY, 400);
		SpellAuras.put(warded, SpellAura.BLACK_TORTOISE, 400);
		Husk far = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(-19, 0, 0), 1.0e6, spawned);
		SpellAuras.put(far, SpellAura.VAJRA_BODY, 400);
		CultivatorNpc guarded = helper.spawn(ModEntities.CULTIVATOR, helper.relativePos(BlockPos.containing(at.add(0, 0, 6))));
		guarded.setNoAi(true);
		guarded.setRank(PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY), true);
		guarded.setBodyCultivator(true);
		spawned.add(guarded);
		BodyTechniques.npcAct(guarded, Ability.IRON_SKIN, null);
		helper.assertTrue(BodyTechniques.holding(guarded, Ability.IRON_SKIN), "A body cultivator under its Iron Skin");
		CultivationManager.toggleAbility(p, Ability.PRIMORDIAL_ROAR.ordinal());
		try {
			helper.assertTrue(!SpellAuras.has(warded, SpellAura.VAJRA_BODY) && !SpellAuras.has(warded, SpellAura.BLACK_TORTOISE),
					"Primordial Roar: the wards stripped from what it reached");
			MobEffectInstance slowed = warded.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
			helper.assertTrue(slowed != null && slowed.getAmplifier() >= 9 && warded.hasEffect(MobEffects.WEAKNESS), "...and it stands stunned");
			helper.assertTrue(!BodyTechniques.holding(guarded, Ability.IRON_SKIN) && guarded.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
					"...the body's own guard stripped too, and it stunned");
			helper.assertTrue(SpellAuras.has(far, SpellAura.VAJRA_BODY) && !far.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "...not what stood past its reach");
			helper.succeed();
		} finally {
			leave(p, spawned, floor);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "body_myriad", timeoutTicks = 200)
	public void myriadFistsFallTenTimes(GameTestHelper helper) {
		// Its own floor and batch: its blows strike a cone five blocks out and its last hurls (in the shared grid it would reach a neighbour).
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 6);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = bodyCultivator(helper, "body-myriad", Realm.PROFOUND_SOVEREIGN, floor.centre());
		// A Chaos-Tempered Body's blow is some hundreds of thousands: a husk of the most health a body may have (BodyCaps#MAX) outlasts ten.
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, p.position().add(0, 0, 2.5), BodyCaps.MAX, spawned);
		HeavenSpellTesting.lookAt(p, husk.position().add(0, 1, 0));
		float before = husk.getHealth();
		double each = p.getAttributeValue(Attributes.ATTACK_DAMAGE) * BodyTechniques.myriadShare(CultivationManager.own(p));
		CultivationManager.toggleAbility(p, Ability.MYRIAD_FISTS.ordinal());
		helper.assertTrue(BodyTechniques.holding(p, Ability.MYRIAD_FISTS), "Myriad Fists: its flurry under way");
		helper.runAfterDelay(BodyTechniques.MYRIAD_BLOWS * BodyTechniques.MYRIAD_EVERY + 4, () -> {
			try {
				double taken = before - husk.getHealth();
				helper.assertTrue(taken >= BodyTechniques.MYRIAD_BLOWS * each * 0.8, "Ten blows landed, each its own (" + taken + " of " + BodyTechniques.MYRIAD_BLOWS * each + ")");
				helper.assertTrue(!BodyTechniques.holding(p, Ability.MYRIAD_FISTS), "...and the flurry is done");
				helper.succeed();
			} finally {
				leave(p, spawned, floor);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "body_dharma", timeoutTicks = 300)
	public void theDharmaBodyStandsBehindAndStrikesAsOneDoes(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 22, 8);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = bodyCultivator(helper, "body-dharma", Realm.HEAVENLY_LORD, floor.centre());
		Vec3 at = p.position();
		CultivationManager.toggleAbility(p, Ability.DHARMA_BODY.ordinal());
		SpellConstruct body = BodyTechniques.dharmaOf(p);
		helper.assertTrue(body != null && body.kind() == SpellConstruct.Kind.DHARMA_BODY && BodyTechniques.holding(p, Ability.DHARMA_BODY),
				"Dharma Body of Heaven and Earth: one's body towering");
		helper.assertTrue(body.position().distanceTo(at.add(0, 0, -BodyTechniques.DHARMA_BEHIND)) < 1.5, "...behind one (" + body.position() + ")");
		Husk struck = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(0, 0, 3), 1.0e6, spawned);
		Husk behind = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(0, 0, -BodyTechniques.DHARMA_BEHIND - 13), 1.0e6, spawned);
		float before = struck.getHealth();
		struck.hurt(p.damageSources().playerAttack(p), 10); // a blow of the hand
		float afterBlow = struck.getHealth();
		List<SpellConstruct> hands = level.getEntitiesOfClass(SpellConstruct.class, new AABB(struck.blockPosition()).inflate(4),
				e -> e.kind() == SpellConstruct.Kind.AVATAR_HAND);
		helper.assertTrue(before - afterBlow > 0 && hands.size() == 1, "A blow of the hand: its fist sent down out of the sky on what it struck");
		helper.runAfterDelay(HeavenDemonSpells.AVATAR_FALL + 3, () -> {
			if (!(afterBlow - struck.getHealth() >= 15)) {
				leave(p, spawned, floor);
				helper.fail("...and come down on it (" + (afterBlow - struck.getHealth()) + ")");
				return;
			}
			float behindBefore = behind.getHealth();
			p.setOnGround(true);
			CultivationManager.toggleAbility(p, Ability.MOUNTAIN_STOMP.ordinal());
			helper.runAfterDelay(BodyTechniques.DHARMA_STOMP_DELAY + 3, () -> {
				try {
					helper.assertTrue(behind.getHealth() < behindBefore, "One stomps: it stamps its foot, striking what stands round its feet ("
							+ (behindBefore - behind.getHealth()) + ")");
					BodyTechniques.end(p);
					helper.assertTrue(BodyTechniques.dharmaOf(p) == null, "...and it is gone with one's end");
					helper.succeed();
				} finally {
					leave(p, spawned, floor);
				}
			});
		});
	}

	// --- Helpers ---

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-3 * Math.max(1, Math.abs(b));
	}

	private static boolean hurt(LivingEntity e) {
		return e.getHealth() < e.getMaxHealth();
	}

	/** Whether Imperishable Gold's own hold against knockback is on {@code p} (the realm's body may resist knockback whole by itself). */
	private static boolean goldHold(ServerPlayer p) {
		var knock = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		return knock != null && knock.getModifiers().stream().anyMatch(m -> m.getName().equals("Imperishable Gold"));
	}

	/** A mortal on the Body Path at {@code realm}'s Early stage, its vigor full, standing on the ground at {@code at} (absolute), looking along +z. */
	private static ServerPlayer bodyCultivator(GameTestHelper helper, String name, Realm realm, BlockPos at) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setYHeadRot(0);
		p.setOnGround(true);
		PlayerCultivation c = CultivationManager.own(p);
		c.setMortal(true);
		c.setMortalState(MortalStage.PEAK, 0);
		BodyPath.begin(p);
		c.body().set(PlayerCultivation.rank(realm, Stage.EARLY), 0);
		CultivationManager.refresh(p);
		c.body().setVigor(BodyTechniques.maxVigor(c));
		return p;
	}

	private static void leave(ServerPlayer p) {
		BodyTechniques.end(p);
		BodyTechniques.forget(p.getUUID());
		BodyPath.forget(p.getUUID());
		QiFlight.forget(p.getUUID());
		p.server.getPlayerList().remove(p);
	}

	private static void leave(ServerPlayer p, List<LivingEntity> spawned, HeavenSpellTesting.Floor floor) {
		for (LivingEntity e : spawned) e.discard();
		leave(p);
		floor.remove();
	}
}
