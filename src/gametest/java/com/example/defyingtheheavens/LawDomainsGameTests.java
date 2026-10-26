package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Phase 3b (2026-10-26): the Law domains (LawDomains): one ability for each Law comprehended, one lit at a time, burning immortal essence;
 * what each does to the foes in it (the five elements' harm and holds, Space's lock, Time's slowness and stretched cooldowns, Life and
 * Death's withheld healing and drained life), the Law's spells the harder in it; and whom it counts a foe. Each test in a batch of its
 * own: a domain reaches past its test's corner, onto the foes of the tests run beside it.
 */
public class LawDomainsGameTests implements FabricGameTest {

	/** A survival player in earnest (a mock player answers that it is creative), of {@code realm} Early, no body's gifts, at the test's corner. */
	private static ServerPlayer survivor(GameTestHelper helper, Realm realm, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		PlayerCultivation c = CultivationManager.get(p);
		c.setPhysique(null); // no Blood Demon Body's drain, no Chaos Body's spells, to muddy the sums
		c.setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		p.setHealth(p.getMaxHealth());
		return p;
	}

	private static void leave(ServerPlayer p) {
		LawDomains.onDisconnect(p);
		ImmortalEssence.onDisconnect(p);
		FiveDecays.forget(p.getUUID());
		Lifespan.forget(p.getUUID());
		LifespanTribulation.forget(p.getUUID());
		QiFlight.forget(p.getUUID());
		if (p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
	}

	/** A foe that stands still and takes a long while to fell: a husk (no sun burns it), no mind, a million health. */
	private static Husk standing(GameTestHelper helper, BlockPos at) {
		Husk husk = helper.spawn(EntityType.HUSK, at);
		still(husk);
		husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1.0e6);
		husk.setHealth(1.0e6f);
		return husk;
	}

	private static void still(Mob mob) {
		mob.setNoAi(true);
		mob.setPersistenceRequired();
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-3 * Math.max(1, Math.abs(b));
	}

	/** {@code p}'s pool filled and {@code ability} switched on (as its switch is thrown on the Abilities tab). */
	private static void unfurl(ServerPlayer p, Ability ability) {
		PlayerCultivation c = CultivationManager.get(p);
		c.setEssence(ImmortalEssence.max(c));
		if (!c.isAbilityEnabled(ability)) CultivationManager.toggleAbility(p, ability.ordinal());
	}

	private static int slowness(LivingEntity e) {
		MobEffectInstance slow = e.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
		return slow == null ? -1 : slow.getAmplifier();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "law_domains_1")
	public void whomADomainWorksOn(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL, "test-domain-a"), q = survivor(helper, Realm.TRUE_IMMORTAL, "test-domain-b");
		try {
			Husk husk = standing(helper, new BlockPos(4, 2, 2));
			Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 4));
			still(villager);
			Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(0, 2, 2));
			still(wolf);
			wolf.tame(p);
			CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 0));
			still(npc);
			helper.assertTrue(LawDomains.foeOf(p, husk) && !LawDomains.foeOf(p, villager) && !LawDomains.foeOf(p, wolf),
					"A monster a foe; village folk and one's own pet not");
			helper.assertTrue(!LawDomains.foeOf(p, npc) && !LawDomains.foeOf(p, q), "A cultivator and a player minding their own business: not foes");
			p.setLastHurtMob(npc);
			q.setLastHurtByMob(p);
			helper.assertTrue(LawDomains.foeOf(p, npc) && LawDomains.foeOf(p, q), "...once blows have passed between them, foes");
			server.setPvpAllowed(false);
			helper.assertTrue(!LawDomains.foeOf(p, q), "A player out of reach of harm (PvP off) never one");
			server.setPvpAllowed(true);
			// Within it by the body's middle, and a foe besides.
			LawDomains.Domain d = new LawDomains.Domain(p, Law.FIRE, 1, LawDomains.centre(p), 10);
			helper.assertTrue(d.holds(husk) && !d.holds(villager) && !d.holds(p), "The domain holds the husk, not the villager, not its owner");
			husk.teleportTo(p.getX() + 14, husk.getY(), p.getZ());
			helper.assertTrue(!d.holds(husk), "...nor the husk once beyond its reach");
			husk.discard();
			villager.discard();
			wolf.discard();
			npc.discard();
			helper.succeed();
		} finally {
			leave(p);
			leave(q);
			server.setPvpAllowed(pvp);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "law_domains_2")
	public void theDomainsAwakenOneAtATime(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL, "test-domain-c");
		try {
			PlayerCultivation c = CultivationManager.get(p);
			for (Law law : Law.values()) {
				Ability ability = Ability.domainOf(law);
				helper.assertTrue(ability != null && ability.law() == law && !ability.isUnlocked(c), law.getId() + ": its domain, not yet awakened");
			}
			c.laws().setLevel(Law.FIRE, 1);
			c.laws().setLevel(Law.WATER, 2);
			helper.assertTrue(Ability.FIRE_DOMAIN.isUnlocked(c) && Ability.WATER_DOMAIN.isUnlocked(c) && !Ability.METAL_DOMAIN.isUnlocked(c),
					"A level of a Law awakens its domain, and only its");
			PlayerCultivation below = new PlayerCultivation();
			below.awaken(0);
			below.setState(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION, 0);
			below.laws().setLevel(Law.FIRE, 3);
			helper.assertTrue(!Ability.FIRE_DOMAIN.isUnlocked(below), "No domain short of True Immortal");
			// Its reach: 8 and 2 a level, a tenth again a realm past True Immortal.
			helper.assertTrue(near(LawDomains.radius(c, Law.FIRE), 10) && near(LawDomains.radius(c, Law.WATER), 12), "Ten blocks at a level, twelve at two");
			c.setState(Realm.HEAVENLY_LORD, Stage.EARLY, 0);
			helper.assertTrue(near(LawDomains.radius(c, Law.WATER), 12 * 1.6), "A Heavenly Lord's six tenths farther");
			c.setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
			// No unfurling on too little essence.
			double max = ImmortalEssence.max(c);
			c.setEssence(max / LawDomains.BURN_SECONDS * LawDomains.LIGHT_SECONDS * 0.9);
			CultivationManager.toggleAbility(p, Ability.FIRE_DOMAIN.ordinal());
			helper.assertTrue(!c.isAbilityEnabled(Ability.FIRE_DOMAIN) && LawDomains.lit(c) == null, "Refused on less than five seconds of essence");
			c.setEssence(max);
			CultivationManager.toggleAbility(p, Ability.FIRE_DOMAIN.ordinal());
			helper.assertTrue(LawDomains.lit(c) == Law.FIRE, "Unfurled on a full pool");
			CultivationManager.toggleAbility(p, Ability.WATER_DOMAIN.ordinal());
			helper.assertTrue(LawDomains.lit(c) == Law.WATER && !c.isAbilityEnabled(Ability.FIRE_DOMAIN), "One at a time: Water's unfurled folds Fire's away");
			// It burns the pool beside the essence's own switch: a full pool in 120 s for it, 90 for the switch, and the leak outside the Immortal Realm.
			CultivationManager.toggleAbility(p, Ability.IMMORTAL_ESSENCE.ordinal());
			helper.assertTrue(ImmortalEssence.isLit(p) && LawDomains.lit(c) == Law.WATER, "Immortal Essence lit beside it");
			c.setEssence(max);
			ImmortalEssence.second(p);
			double burnt = max / ImmortalEssence.LEAK_SECONDS + max / ImmortalEssence.BURN_SECONDS + max / LawDomains.BURN_SECONDS;
			helper.assertTrue(near(c.essence(), max - burnt), "A second's burn of both and the leak (" + c.essence() + " of " + max + ")");
			CultivationManager.toggleAbility(p, Ability.IMMORTAL_ESSENCE.ordinal());
			// Run dry, it folds away.
			c.setEssence(max / LawDomains.BURN_SECONDS * 0.5);
			ImmortalEssence.second(p);
			helper.assertTrue(c.essence() == 0 && LawDomains.lit(c) == null && !c.isAbilityEnabled(Ability.WATER_DOMAIN), "The pool dry: it folds away");
			// Lit, the tick finds it, its Law, level and reach.
			unfurl(p, Ability.WATER_DOMAIN);
			LawDomains.tick(p.server);
			helper.assertTrue(LawDomains.lit().stream().anyMatch(d -> d.owner() == p && d.law() == Law.WATER && d.level() == 2 && near(d.radius(), 12)),
					"The tick finds it unfurled, its reach twelve");
			helper.assertTrue(key(Ability.WATER_DOMAIN.getDescription(c)).equals(ModLang.abilityKey("water_domain") + ".description"), "Its own words on the tab");
			// No immortal, no domain: its switch put back.
			c.setState(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION, 0);
			LawDomains.tick(p.server);
			helper.assertTrue(!c.isAbilityEnabled(Ability.WATER_DOMAIN) && LawDomains.lit().stream().noneMatch(d -> d.owner() == p), "Fallen below True Immortal: folded away");
			// A logout folds it away.
			c.setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
			unfurl(p, Ability.WATER_DOMAIN);
			LawDomains.onDisconnect(p);
			helper.assertTrue(LawDomains.lit(c) == null, "A logout folds it away");
			helper.succeed();
		} finally {
			leave(p);
		}
	}

	private static String key(net.minecraft.network.chat.Component c) {
		return c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "law_domains_3", timeoutTicks = 220)
	public void theElementsDomainsWorkTheirFoes(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL, "test-domain-d"), q = survivor(helper, Realm.TRUE_IMMORTAL, "test-domain-e");
		Husk husk = standing(helper, new BlockPos(4, 2, 2));
		Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 5));
		still(villager);
		// A cultivator of True Immortal Grand Perfection, to outlast the domains' harm.
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(0, 2, 2));
		still(npc);
		npc.setRank(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION), true);
		npc.setHealth(npc.getMaxHealth());
		PlayerCultivation c = CultivationManager.get(p);
		for (Law law : new Law[] {Law.METAL, Law.WOOD, Law.WATER, Law.FIRE, Law.EARTH}) c.laws().setLevel(law, 1);
		double armour = husk.getAttributeValue(Attributes.ARMOR);
		float full = husk.getHealth();
		Runnable done = () -> {
			leave(p);
			leave(q);
			server.setPvpAllowed(pvp);
		};
		unfurl(p, Ability.METAL_DOMAIN);
		helper.runAfterDelay(25, () -> {
			try {
				helper.assertTrue(husk.getHealth() < full, "Metal: the husk cut (" + husk.getHealth() + ")");
				helper.assertTrue(near(husk.getAttributeValue(Attributes.ARMOR), armour * (1 - LawDomains.ARMOUR_CUT_PER_LEVEL)), "...its armour cut 4% ("
						+ husk.getAttributeValue(Attributes.ARMOR) + " of " + armour + ")");
				helper.assertTrue(villager.getHealth() == villager.getMaxHealth() && npc.getLastHurtByMob() == null, "The villager and a cultivator at peace untouched");
				p.setLastHurtMob(npc); // now at odds
				unfurl(p, Ability.FIRE_DOMAIN);
			} catch (RuntimeException e) {
				done.run();
				throw e;
			}
		});
		helper.runAfterDelay(50, () -> {
			try {
				helper.assertTrue(npc.getLastHurtByMob() == p, "Fire: the cultivator at odds with its owner burnt"); // (it mends as fast as a TI)
				helper.assertTrue(husk.isOnFire(), "...the husk set alight");
				helper.assertTrue(near(husk.getAttributeValue(Attributes.ARMOR), armour), "Metal's folded away: the husk's armour whole again");
				float fire = LawDomains.outgoing(husk, ModDamageTypes.qi(level, QiElement.FIRE, p), 100);
				float water = LawDomains.outgoing(husk, ModDamageTypes.qi(level, QiElement.WATER, p), 100);
				float spared = LawDomains.outgoing(villager, ModDamageTypes.qi(level, QiElement.FIRE, p), 100);
				helper.assertTrue(near(fire, 125) && near(water, 100) && near(spared, 100), "Its own fire spells a quarter harder on a foe in it alone (" + fire + ", "
						+ water + ", " + spared + ")");
				unfurl(p, Ability.WATER_DOMAIN);
			} catch (RuntimeException e) {
				done.run();
				throw e;
			}
		});
		helper.runAfterDelay(75, () -> {
			try {
				helper.assertTrue(!husk.isOnFire() && slowness(husk) == 0, "Water: the fire on the husk put out, the husk slowed");
				float doused = LawDomains.outgoing(villager, ModDamageTypes.qi(level, QiElement.FIRE, husk), 100);
				helper.assertTrue(near(doused, 100 * (1 - LawDomains.FIRE_DOUSED_PER_LEVEL)), "...the husk's own fire doused (" + doused + ")");
				p.setHealth(p.getMaxHealth() / 2);
				unfurl(p, Ability.WOOD_DOMAIN);
			} catch (RuntimeException e) {
				done.run();
				throw e;
			}
		});
		helper.runAfterDelay(100, () -> {
			try {
				helper.assertTrue(p.getHealth() > p.getMaxHealth() / 2, "Wood: its roots drain the foes, its owner healed (" + p.getHealth() + ")");
				// Earth: a flier at odds with its owner, and a cultivator flying, brought down.
				q.teleportTo(level, p.getX(), p.getY() + 4, p.getZ() + 2, 0, 0);
				q.getAbilities().mayfly = true;
				q.getAbilities().flying = true;
				q.setLastHurtByMob(p);
				npc.setQiFlying(true);
				unfurl(p, Ability.EARTH_DOMAIN);
			} catch (RuntimeException e) {
				done.run();
				throw e;
			}
		});
		helper.runAfterDelay(125, () -> {
			try {
				helper.assertTrue(QiFlight.weighedDown(q) && !npc.isQiFlying(), "Earth: the player's flight weighed down, the cultivator's ended");
				helper.assertTrue(slowness(husk) == 0, "...the husk slowed");
				husk.discard();
				villager.discard();
				npc.discard();
				helper.succeed();
			} finally {
				done.run();
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "law_domains_4", timeoutTicks = 160)
	public void theGreaterDomains(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper, Realm.GOLDEN_IMMORTAL, "test-domain-f");
		Husk husk = standing(helper, new BlockPos(4, 2, 2));
		Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 5));
		still(villager);
		PlayerCultivation c = CultivationManager.get(p);
		c.laws().setLevel(Law.SPACE, 1);
		c.laws().setLevel(Law.TIME, 3);
		c.laws().setLevel(Law.LIFE_AND_DEATH, 2);
		double reach = LawDomains.radius(c, Law.SPACE);
		unfurl(p, Ability.SPACE_DOMAIN);
		helper.runAfterDelay(15, () -> {
			try {
				helper.assertTrue(LawDomains.locksSpace(husk) && !LawDomains.locksSpace(villager), "Space: locked on the husk, not on the villager");
				float rift = LawDomains.outgoing(husk, ModDamageTypes.qi(level, QiElement.WATER, Realm.VOID_SHATTERING, null, p), 100);
				helper.assertTrue(near(rift, 125), "...a spell of space a quarter harder on it (" + rift + ")");
				helper.assertTrue(slowness(husk) == 0, "...the husk slowed");
				// Thrown out of it, it is put back.
				husk.teleportTo(p.getX() + reach + 3, husk.getY(), p.getZ());
			} catch (RuntimeException e) {
				leave(p);
				throw e;
			}
		});
		helper.runAfterDelay(18, () -> {
			try {
				double from = HeavenSpells.middle(husk).distanceTo(LawDomains.centre(p));
				helper.assertTrue(from <= reach, "...what it caught can't leave it (" + from + " of " + reach + ")");
				unfurl(p, Ability.TIME_DOMAIN);
			} catch (RuntimeException e) {
				leave(p);
				throw e;
			}
		});
		helper.runAfterDelay(40, () -> {
			try {
				helper.assertTrue(!LawDomains.locksSpace(husk), "Space folded away: its lock lifted");
				helper.assertTrue(LawDomains.stretched(husk, 100) == 130 && LawDomains.stretched(villager, 100) == 100,
						"Time: a foe's cooldowns 30% longer at three levels, the villager's its own");
				MobEffectInstance fatigue = husk.getEffect(MobEffects.DIG_SLOWDOWN);
				helper.assertTrue(slowness(husk) == 2 && fatigue != null && fatigue.getAmplifier() == 1, "...the husk slowed, its blows too ("
						+ slowness(husk) + ")");
				unfurl(p, Ability.LIFE_AND_DEATH_DOMAIN);
			} catch (RuntimeException e) {
				leave(p);
				throw e;
			}
		});
		helper.runAfterDelay(55, () -> {
			try {
				husk.setHealth(1.0e5f);
				husk.heal(100);
				villager.setHealth(5);
				villager.heal(5);
				helper.assertTrue(husk.getHealth() == 1.0e5f && villager.getHealth() == 10, "Life and Death: the husk can't heal; the villager can");
				float ghostly = LawDomains.outgoing(husk, ModDamageTypes.ghostly(level, p), 100);
				helper.assertTrue(near(ghostly, 125), "...the ghost path's harm a quarter harder on it (" + ghostly + ")");
				p.setHealth(p.getMaxHealth() / 2);
				float before = p.getHealth(), health = husk.getHealth();
				husk.invulnerableTime = 0;
				husk.hurt(ModDamageTypes.qi(level, QiElement.FIRE, p), 1000);
				float lost = health - husk.getHealth();
				double back = lost * LawDomains.LIFE_DRAIN_PER_LEVEL * 2 * (1 + Laws.LIFE_PER_LEVEL * 2); // its Law's own healing on it
				helper.assertTrue(lost > 0 && near(p.getHealth() - before, back), "...6% of the harm it deals comes back to its owner ("
						+ (p.getHealth() - before) + " of " + back + ")");
				husk.discard();
				villager.discard();
				helper.succeed();
			} finally {
				leave(p);
			}
		});
	}

	/**
	 * Space locked on a foe: its Nine Nether Step refused (nothing paid, nowhere gone), then taken once the domain folds away. (The Spatial
	 * Tear's refusal is checked in game: the test server has no gap to tear into, so a tear is refused there before the lock is asked.)
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "law_domains_5", timeoutTicks = 60)
	public void spaceLockedRefusesTheStep(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		ServerPlayer p = survivor(helper, Realm.GOLDEN_IMMORTAL, "test-domain-g"), q = survivor(helper, Realm.HEAVENLY_BEING, "test-domain-h");
		CultivationManager.get(p).laws().setLevel(Law.SPACE, 1);
		NetherworldState s = CultivationManager.get(q).netherworld();
		s.setMethod(GhostMethod.NINE_NETHER_SOUL_SUTRA, GhostMethod.NINE_NETHER_SOUL_SUTRA.layers(), 0);
		s.setGhostQi(1000);
		q.setLastHurtByMob(p);
		unfurl(p, Ability.SPACE_DOMAIN);
		helper.runAfterDelay(12, () -> {
			try {
				helper.assertTrue(LawDomains.locksSpace(q) && !LawDomains.locksSpace(p), "Locked on its owner's foe, not on its owner");
				helper.assertTrue(Spell.NINE_NETHER_STEP.isKnown(CultivationManager.get(q)), "The foe knows the Nine Nether Step");
				// (Its ghost qi gathers as the ticks pass: read just before each cast.)
				net.minecraft.world.phys.Vec3 at = q.position();
				double ghost = s.ghostQi();
				Spells.cast(q, Spell.NINE_NETHER_STEP);
				helper.assertTrue(s.ghostQi() == ghost && q.position().distanceTo(at) < 1.0e-3, "The step refused in it: nothing paid, nowhere gone ("
						+ s.ghostQi() + " of " + ghost + ")");
				CultivationManager.toggleAbility(p, Ability.SPACE_DOMAIN.ordinal());
				LawDomains.tick(server);
				helper.assertTrue(!LawDomains.locksSpace(q), "Folded away: space free");
				ghost = s.ghostQi();
				Spells.cast(q, Spell.NINE_NETHER_STEP);
				helper.assertTrue(near(s.ghostQi(), ghost - Spell.NINE_NETHER_STEP.cost()), "...the step taken, its ghost qi paid (" + s.ghostQi() + " of "
						+ ghost + ")");
				helper.succeed();
			} finally {
				Spells.forget(q.getUUID());
				leave(p);
				leave(q);
				server.setPvpAllowed(pvp);
			}
		});
	}
}
