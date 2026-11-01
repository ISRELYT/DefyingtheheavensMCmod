package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Body cultivators and body sects (2026-11-01, the user's "Go, with body sects"): a body cultivator's mightier body and no qi arts, its realm
 * named the Body Path's, its flag saved; its techniques (Iron Skin's and Golden Body's guard, the fist, the frenzy) and what it chooses in a
 * fight; a Body Path player known for their body (address, standing, regard, weighing); the rogues' roll; body sects planned whole (every
 * piece, the blueprint), saved, joined only by those on the Body Path, newcomers sent to their own kind; their chests' and their people's
 * manuals; their names.
 */
public class BodyCultivatorsGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-4 * Math.max(1, Math.abs(b));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyCultivatorIsMightierAndWieldsNoQi(GameTestHelper helper) {
		int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
		CultivatorNpc qi = npc(helper, rank, 1);
		CultivatorNpc body = npc(helper, rank, 3);
		body.setBodyCultivator(true);
		helper.assertTrue(body.isBodyCultivator() && !qi.isBodyCultivator(), "One tempers the body, one is of qi");
		helper.assertTrue(near(body.getMaxHealth() / qi.getMaxHealth(), BodyPath.BODY),
				"Its body half again as strong (" + body.getMaxHealth() + " to " + qi.getMaxHealth() + ")");
		helper.assertTrue(body.getAttributeValue(Attributes.ATTACK_DAMAGE) > qi.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3
				&& body.getAttributeValue(Attributes.ARMOR) > qi.getAttributeValue(Attributes.ARMOR), "...and its blows and its armour, as a player's on the Body Path");
		helper.assertTrue(!body.canFlySustainably() && !body.canSuppressSustainably() && body.domainRadius() == 0, "No flight, no Realm Suppress, no domain");
		helper.assertTrue(body.heavenMethod() == null && CultivatorSpells.repertoire(body).isEmpty() && qi.heavenMethod() != null,
				"No heaven method, so no spell (the qi one has its own)");
		helper.assertTrue(body.rankLabel().getString().equals(BodyPath.rankName(rank).getString()) && !qi.rankLabel().getString().equals(body.rankLabel().getString()),
				"Its realm the Body Path's: " + body.rankLabel().getString());
		helper.assertTrue(body.cultivation().hasHeavensLeave(), "No Ritual to the Heavens on its way");
		ItemStack manual = body.bodyManual();
		helper.assertTrue(!manual.isEmpty() && MethodManualItem.method(manual).path() == TwoPaths.Path.BODY && qi.bodyManual().isEmpty(),
				"It carries a manual of the body's ways; the qi one none");
		// Saved and loaded.
		CompoundTag tag = new CompoundTag();
		body.addAdditionalSaveData(tag);
		CultivatorNpc again = ModEntities.CULTIVATOR.create(helper.getLevel());
		again.readAdditionalSaveData(tag);
		helper.assertTrue(again.isBodyCultivator() && near(again.getMaxHealth(), body.getMaxHealth()), "Saved and loaded a body cultivator");
		again.discard();
		// Never a demon, a mortal or one of the dead.
		CultivatorNpc demon = npc(helper, rank, 5);
		demon.setRace(Bloodline.TIGER, 0);
		demon.setBodyCultivator(true);
		CultivatorNpc mortal = npc(helper, CultivatorNpc.MORTAL, 7);
		mortal.setBodyCultivator(true);
		CultivatorNpc ghost = helper.spawn(ModEntities.GHOST_CULTIVATOR, new BlockPos(9, 2, 1));
		ghost.setBodyCultivator(true);
		helper.assertTrue(!demon.isBodyCultivator() && !mortal.isBodyCultivator() && !ghost.isBodyCultivator(), "Never a demon, a mortal or one of the dead");
		body.setRace(Bloodline.FOX, 0);
		helper.assertTrue(!body.isBodyCultivator(), "Made a demon, it walks the Demon Path instead");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSoulInABodyCultivatorLivesTheBodyPath(GameTestHelper helper) {
		int rank = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.MID);
		CultivatorNpc npc = npc(helper, rank, 1);
		npc.setBodyCultivator(true);
		PlayerCultivation record = npc.cultivation();
		record.setState(record.getRealm(), record.getStage(), 123);
		BodyCultivators.asBody(npc, record);
		helper.assertTrue(record.isMortal() && BodyPath.rank(record) == rank && record.body().cultivation() == 123 && record.body().cultivating(),
				"A soul in its body lives it as a mortal on the Body Path at its rank, its progress carried (Possession)");
		helper.assertTrue(TwoPaths.weighedRank(record) == rank && TwoPaths.higherRank(record, false) < 0, "Weighed by its body, with no qi path");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyCultivatorsTechniquesGuardAndStrike(GameTestHelper helper) {
		// Void Shattering: past the lower realms' hold (a Four Axis body here is held to their cap, as a player is).
		CultivatorNpc npc = npc(helper, PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY), 1);
		npc.setBodyCultivator(true);
		int rank = BodyTechniques.rankOf(npc);
		Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(3, 2, 1));
		husk.setNoAi(true);
		try {
			DamageSource blow = npc.damageSources().mobAttack(husk);
			helper.assertTrue(near(BodyTechniques.guard(npc, blow, 10), 10), "Nothing held: the blow as it is");
			helper.assertTrue(BodyTechniques.npcAct(npc, Ability.IRON_SKIN, husk) && BodyTechniques.holding(npc, Ability.IRON_SKIN), "Iron Skin");
			helper.assertTrue(near(BodyTechniques.guard(npc, blow, 10), 10 * (1 - BodyTechniques.ironSkinCut(rank)))
					&& npc.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1, "...takes the sting out of a blow, nothing moves it");
			helper.assertTrue(BodyTechniques.npcAct(npc, Ability.GOLDEN_BODY, husk) && BodyTechniques.guard(npc, blow, 10) == 0, "Golden Body turns it aside");
			helper.assertTrue(rank == PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY), "It fights at its own realm (" + rank + ")");
			helper.assertTrue(!BodyTechniques.npcAct(npc, Ability.HEAVEN_SHATTERING_FIST, husk), "The Heaven-Shattering Fist is the Indestructible Body's: not yet its");
			DamageSource hand = npc.damageSources().mobAttack(npc);
			helper.assertTrue(near(BodyTechniques.outgoing(husk, hand, 10), 10), "Its blow of the hand as it is");
			helper.assertTrue(BodyTechniques.npcAct(npc, Ability.THOUSAND_POUND_FIST, husk)
					&& near(BodyTechniques.outgoing(husk, hand, 10), 10 * BodyTechniques.fistFactor(rank)), "The fist primed: its next blow x" + BodyTechniques.fistFactor(rank));
			helper.assertTrue(near(BodyTechniques.outgoing(husk, hand, 10), 10), "...once");
			double speed = npc.getAttributeValue(Attributes.MOVEMENT_SPEED);
			helper.assertTrue(BodyTechniques.npcAct(npc, Ability.BLOOD_FRENZY, husk)
					&& near(BodyTechniques.outgoing(husk, hand, 10), 10 * (1 + BodyTechniques.frenzyHarm(rank)))
					&& npc.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed * 1.2, "Blood Frenzy: harder and swifter, burning a while");
			helper.assertTrue(near(BodyTechniques.outgoing(husk, husk.damageSources().mobAttack(husk), 10), 10), "Another's blow untouched");
			helper.succeed();
		} finally {
			BodyTechniques.forget(npc.getUUID());
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void inAFightItChoosesByWhatBefallsIt(GameTestHelper helper) {
		CultivatorNpc npc = npc(helper, PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY), 1);
		npc.setBodyCultivator(true);
		Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(4, 2, 1));
		husk.setNoAi(true);
		BodyCultivators.TechniqueGoal goal = new BodyCultivators.TechniqueGoal(npc);
		long now = helper.getLevel().getGameTime();
		npc.setHealth(npc.getMaxHealth() * 0.3f);
		helper.assertTrue(goal.choose(null, now) == Ability.FLESH_REBORN, "Low: its flesh reborn");
		npc.setHealth(npc.getMaxHealth() * 0.45f);
		npc.setLastHurtByMob(husk);
		helper.assertTrue(goal.choose(null, now) == Ability.GOLDEN_BODY, "Hurt and under blows: Golden Body");
		npc.setHealth(npc.getMaxHealth());
		npc.setLastHurtByMob(null);
		npc.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
		helper.assertTrue(goal.choose(null, now) == Ability.DIAMOND_WILL, "Harried by poison: Diamond Will");
		npc.removeAllEffects();
		npc.setLastHurtByMob(husk);
		helper.assertTrue(goal.choose(null, now) == Ability.IRON_SKIN, "Under blows, whole: Iron Skin");
		npc.setLastHurtByMob(null);
		helper.assertTrue(goal.choose(null, now) == null, "Nothing befalling it, no foe: nothing");
		// Holding back in a spar to Qi Refining, it has Copper Skin's alone.
		npc.beginSpar(husk, PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE));
		npc.setHealth(npc.getMaxHealth() * 0.3f);
		npc.setLastHurtByMob(husk);
		helper.assertTrue(goal.choose(null, now) == Ability.IRON_SKIN, "Held back: no art past the realm it holds back to");
		npc.endSpar(husk);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyCultivatorKnowsABodyForWhatItIs(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
		CultivatorNpc qi = npc(helper, rank, 1);
		CultivatorNpc body = npc(helper, rank, 3);
		body.setBodyCultivator(true);
		try {
			ServerPlayer p = bodyCultivator(helper, made, "body-known", Realm.CORE_FORMATION);
			helper.assertTrue(NpcAddress.rung(p, qi) == NpcAddress.Rung.MORTAL && NpcAddress.rung(p, body) == NpcAddress.Rung.PEER,
					"A cultivator of qi sees a mortal; a body cultivator, a peer (" + NpcAddress.rung(p, body) + ")");
			helper.assertTrue(TalkSystem.standing(p, qi) == TalkSystem.Standing.MORTAL && TalkSystem.standing(p, body) == TalkSystem.Standing.PEER,
					"...and stands with them as one (so they may spar)");
			helper.assertTrue(qi.weighs(p) == CultivatorNpc.MORTAL && body.weighs(p) == rank, "Weighed as its body by its own kind");
			helper.assertTrue(body.regard(p) - qi.regard(p) == Regard.BODY_KIN, "Warmer to one of its own way (" + body.regard(p) + " to " + qi.regard(p) + ")");
			ServerPlayer plain = survivor(helper, made, "body-unknown");
			CultivationManager.own(plain).setMortal(true);
			helper.assertTrue(NpcAddress.rung(plain, body) == NpcAddress.Rung.MORTAL && body.regard(plain) == qi.regard(plain),
					"A mortal off the Body Path is a mortal to it too");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void roguesAreBodyCultivatorsNowAndThen(GameTestHelper helper) {
		CultivatorNpc man = npc(helper, PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 1);
		CultivatorNpc demon = npc(helper, PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 3);
		demon.setRace(Bloodline.LIZARD, 0);
		CultivatorNpc mortal = npc(helper, CultivatorNpc.MORTAL, 5);
		RandomSource random = RandomSource.create(42);
		int bodies = 0;
		for (int i = 0; i < 500; i++) if (NpcSpawner.rollsBody(man, random)) bodies++;
		helper.assertTrue(bodies > 60 && bodies < 140, "About one man in " + NpcSpawner.BODY_IN + " (" + bodies + " of 500)");
		boolean any = false;
		for (int i = 0; i < 100; i++) any |= NpcSpawner.rollsBody(demon, random) || NpcSpawner.rollsBody(mortal, random);
		helper.assertTrue(!any, "Never a demon or a mortal");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bodySectsArePlannedWholeAndKeepTheBodysWays(GameTestHelper helper) {
		int bodySects = 0, others = 0;
		for (long seed = 1; seed <= 48; seed++) {
			List<StructurePiece> pieces = SectStructure.layOut(seed, 0, Direction.NORTH, false);
			boolean blueprint = false;
			for (StructurePiece piece : pieces) {
				if (piece instanceof SectPiece sp && sp.kind() == SectPiece.Kind.PLAZA) blueprint = sp.extra().getCompound("Blueprint").getBoolean("Body");
			}
			for (StructurePiece piece : pieces) {
				if (piece instanceof SectPiece sp && sp.body != blueprint) {
					helper.fail("Every building of a sect knows whether it is a body sect (seed " + seed + ")");
				}
			}
			if (blueprint) bodySects++;
			else others++;
		}
		helper.assertTrue(bodySects > 0 && others > bodySects, "Now and then a body sect (" + bodySects + " of 48)");
		// Its library's and master's hall's manuals: the body's ways, of its path's.
		RandomSource random = RandomSource.create(7);
		for (int i = 0; i < 40; i++) {
			List<ItemStack> library = MethodManualItem.keptBody(Alignment.Faction.RIGHTEOUS, false, false, random);
			helper.assertTrue(!library.isEmpty() && library.size() <= 2, "A body sect's library always keeps one or two");
			for (ItemStack manual : library) {
				CultivationMethod way = MethodManualItem.method(manual);
				helper.assertTrue(way != null && way.path() == TwoPaths.Path.BODY && way != CultivationMethod.BEAST_BLOOD_BATHING,
						"...of the body's ways a righteous sect keeps: " + way);
			}
			for (ItemStack manual : MethodManualItem.keptBody(Alignment.Faction.DEVILISH, true, true, random)) {
				CultivationMethod way = MethodManualItem.method(manual);
				helper.assertTrue(way == CultivationMethod.BEAST_BLOOD_BATHING || way == CultivationMethod.WEIGHTED_TRAINING, "A devilish one's: blood and weights");
			}
		}
		for (int i = 0; i < 20; i++) {
			String name = SectNames.body(random, Alignment.Faction.values()[i % 3]);
			helper.assertTrue(name.split(" ").length >= 3, "A body sect's name: " + name);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodySectTakesItsOwnKind(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		Sect body = sect(helper, "Iron Bone Hall", true);
		Sect qi = sect(helper, "Azure Cloud Sect", false);
		CultivatorNpc elder = npc(helper, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), 1);
		elder.setBodyCultivator(true);
		try {
			// Saved and loaded a body sect.
			helper.assertTrue(Sect.load(body.save()).body && !Sect.load(qi.save()).body, "A body sect saved and loaded as one");
			ServerPlayer tempered = bodyCultivator(helper, made, "body-joiner", Realm.FOUNDATION_BUILDING);
			ServerPlayer plain = survivor(helper, made, "qi-joiner");
			PlayerCultivation c = CultivationManager.own(plain);
			c.setMortal(false);
			c.setState(Realm.FOUNDATION_BUILDING, Stage.EARLY, 0);
			helper.assertTrue(SectMembership.refusal(tempered, elder, body, new Object[1]) == null, "A body sect takes one on the Body Path, a mortal too");
			helper.assertTrue("join.not_body".equals(SectMembership.refusal(plain, elder, body, new Object[1])), "...but no one off it");
			helper.assertTrue("join.mortal".equals(SectMembership.refusal(tempered, elder, qi, new Object[1])), "A sect of qi still sees a mortal in a body with no qi");
			helper.assertTrue(SectMembership.strength(body, tempered) == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY)
					&& SectMembership.strength(qi, tempered) == CultivatorNpc.MORTAL, "A body sect weighs one by their body");
			// A newcomer goes to its own kind.
			CultivatorNpc newcomer = npc(helper, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 5);
			newcomer.setAlignment(0);
			newcomer.moveTo(body.core.getX() + 0.5, body.core.getY() + 1, body.core.getZ() + 0.5);
			helper.assertTrue(SectManager.findOpening(level, newcomer) == qi, "One of qi to a sect of qi");
			newcomer.setBodyCultivator(true);
			helper.assertTrue(SectManager.findOpening(level, newcomer) == body, "A body cultivator to a body sect");
			helper.succeed();
		} finally {
			leave(made);
			body.extinct = true; // gone from every newcomer's search after
			qi.extinct = true;
		}
	}

	// --- Helpers ---

	/** A still cultivator of {@code rank} at x, of no path's quarrel (alignment 0), past any quota. */
	private static CultivatorNpc npc(GameTestHelper helper, int rank, int x) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(x, 2, 1));
		npc.setNoAi(true);
		npc.setRank(rank, true);
		npc.setAlignment(0);
		npc.setHealth(npc.getMaxHealth());
		return npc;
	}

	/** A sect of {@code name} at the test's corner, room for ten, neutral, a body sect or not. */
	private static Sect sect(GameTestHelper helper, String name, boolean body) {
		ServerLevel level = helper.getLevel();
		Sect sect = new Sect(UUID.randomUUID());
		sect.name = name;
		sect.dimension = level.dimension();
		sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
		sect.alignment = 0;
		sect.capacity = 10;
		sect.body = body;
		SectData.get(level.getServer()).put(sect);
		return sect;
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
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 3));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		made.add(p);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			SparSystem.abandon(p);
			BodyTechniques.forget(p.getUUID());
			BodyPath.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
