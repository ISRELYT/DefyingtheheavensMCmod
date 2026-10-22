package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The soul and the forms (the user's asks, 2026-10-19): a soul's hearts are the ghost path's, a body left sitting keeps the form its
 * owner set out in, a soul in an NPC's body is drawn as that NPC, and the dead send their souls out too (taking no person's body).
 * Driven by hand on survival test players, as SoulProjectionGameTests' are.
 */
public class SoulFormsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void aSoulsHeartsAreTheGhostPaths(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(c.soulHearts() == 1 && c.soulTetherRadius() == 96,
				"The heaven path at Nascent Soul Grand Perfection: its tether reaches 96 blocks, but the soul has 1 heart (" + c.soulHearts() + ")");
		ghostPath(c, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION));
		helper.assertTrue(c.soulHearts() == 10, "A ghost path there as well: 10 hearts (" + c.soulHearts() + ")");
		PlayerCultivation ghostly = new PlayerCultivation();
		ghostly.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		ghostPath(ghostly, PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION));
		helper.assertTrue(ghostly.soulHearts() == 34 && ghostly.soulPath() == TwoPaths.Path.GHOST,
				"A ghost path of Four Axis Grand Perfection: 34 hearts (" + ghostly.soulHearts() + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyLeftInAGhostKingsFormSitsOnInIt(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = kingForm(helper, made, "king-form-soul");
			PlayerCultivation c = CultivationManager.get(p);
			helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.KING, "In a Ghost King's form");
			project(helper, p);
			SoulBodyEntity body = bodyNear(helper, p);
			helper.assertTrue(body != null && body.look() == TwoPaths.Look.KING, "Its body sits on in that form (" + (body == null ? null : body.look()) + ")");
			recall(helper, p);
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()) && c.isAbilityEnabled(Ability.FORM_SHIFT) && TwoPaths.look(c) == TwoPaths.Look.KING,
					"Home again, still in it");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSoulInAnNpcsBodyIsDrawnAsTheNpc(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc npc = null;
		try {
			ServerPlayer soul = kingForm(helper, made, "king-form-possessor");
			project(helper, soul);
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION));
			helper.assertTrue(SoulProjection.tryPossess(soul, npc) == InteractionResult.SUCCESS, "The soul takes the cultivator's body");
			PlayerCultivation body = CultivationManager.get(soul);
			helper.assertTrue(body == npc.cultivation() && TwoPaths.look(body) == TwoPaths.Look.HUMAN,
					"In its body the soul is drawn as the cultivator is, not in its own Ghost King's form (" + TwoPaths.look(body) + ")");
			// Possession forgets what the forms last saw of it (Possession#rebase): the next look tells everyone how it is drawn now.
			helper.assertTrue(TwoPaths.watchWouldTell(soul), "...and its clients are told so");
			helper.succeed();
		} finally {
			leave(made);
			if (npc != null) npc.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadSendTheirSoulsOutToo(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc npc = null;
		ServerPlayer p = null;
		try {
			p = survivor(helper, made, "dead-soul");
			PlayerCultivation c = CultivationManager.own(p);
			c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
			ghostPath(c, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY));
			GhostSystem.makeGhost(p);
			CultivationManager.refresh(p);
			c.netherworld().setGhostQi(GhostQi.poolMax(c));
			helper.assertTrue(GhostSystem.isKing(p) && Ability.SOUL_PROJECTION.isUnlocked(c), "A Ghost King of Nascent Soul has Soul Projection");
			project(helper, p);
			SoulBodyEntity body = bodyNear(helper, p);
			helper.assertTrue(body != null && body.look() == TwoPaths.Look.KING && GhostSystem.isGhost(p),
					"Its soul is out, its form sitting on, crowned (" + (body == null ? null : body.look()) + ")");
			helper.assertTrue(Math.abs(p.getMaxHealth() - c.soulHearts() * 2) < 1.0e-3, "The soul holds its own hearts (" + p.getMaxHealth() + ")");
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY));
			helper.assertTrue(SoulProjection.tryPossess(p, npc) == InteractionResult.SUCCESS && Possession.inhabits(p.getUUID()),
					"A soul of the dead takes a person's body too");
			helper.assertTrue(!GhostSystem.isGhost(p) && !Incorporeal.is(p) && CultivationManager.get(p) == npc.cultivation(),
					"In it, it is the living body: it touches and is touched, the dead's rules its own no longer");
			SoulProjection.toggle(p); // out of the body, still out of its form
			helper.assertTrue(!Possession.inhabits(p.getUUID()) && SoulProjection.isSoul(p) && GhostSystem.isGhost(p) && !npc.isVacant(),
					"Out of it again, one of the dead, the cultivator itself again");
			recall(helper, p);
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()) && GhostSystem.isGhost(p) && body.isRemoved(), "Home again in its form");
			// Struck down out there (the death about to go ahead): back in its form, its nascent soul lost, and scattered as the dead are.
			project(helper, p);
			boolean died = net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.invoker()
					.allowDeath(p, p.damageSources().magic(), 1000);
			helper.assertTrue(!died && !SoulProjection.isActive(p.getUUID()) && GhostSystem.isGhost(p)
							&& TwoPaths.ghostRank(c, false) == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION),
					"Struck down, the soul is lost and the ghost path falls to Core Formation (" + TwoPaths.ghostRank(c, false) + ")");
			helper.succeed();
		} finally {
			if (p != null) GhostSystem.forget(p.getUUID());
			leave(made);
			if (npc != null) npc.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aCommonGhostTakesTheTribulationAsALifeInIronWould(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerPlayer p = null;
		try {
			p = survivor(helper, made, "common-ghost-trial");
			((com.example.defyingtheheavens.mixin.ServerPlayerAccessor) p).dth$setSpawnInvulnerableTime(0);
			PlayerCultivation c = CultivationManager.own(p);
			c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
			NetherworldState s = c.netherworld();
			for (GhostMethod method : new GhostMethod[]{GhostMethod.NETHERWORLD_BREATHING, GhostMethod.YELLOW_SPRINGS_MARROW, GhostMethod.GHOST_CORE_CANON}) {
				s.setMethod(method, 1, 0);
			}
			int rank = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
			GhostMethods.setPath(s, rank);
			s.setPath(rank, GhostMethods.required(rank));
			GhostSystem.makeGhost(p);
			CultivationManager.refresh(p);
			p.setHealth(p.getMaxHealth());
			float full = p.getHealth();
			helper.assertTrue(Incorporeal.isRuledGhost(p) && p.getArmorValue() < TribulationManager.GHOST_FORM_ARMOR,
					"One of the common dead, its armour its realm's alone (" + p.getArmorValue() + ")");
			CultivationManager.tryBreakthrough(p);
			helper.assertTrue(TribulationManager.isGhostTrial(p.getUUID()), "It faces the Netherworld's tribulation");
			for (int tick = 0; tick < 60; tick++) TribulationManager.tick(helper.getLevel().getServer());
			float share = (float) GhostSystem.livingShare(p);
			TribulationManager.Strikes core = TribulationManager.strikes(Realm.CORE_FORMATION, Stage.EARLY);
			float bolt = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(core.bolt(), TribulationManager.GHOST_FORM_ARMOR,
					(float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS));
			helper.assertTrue(Math.abs(full - p.getHealth() - (core.magic() + bolt) * share) < 0.01f,
					"Its form turns the bolt as iron would, and the strike takes the share of its hearts it would of a life's ("
							+ (full - p.getHealth()) + " of " + p.getMaxHealth() + ", x" + share + ")");
			helper.succeed();
		} finally {
			if (p != null) {
				TribulationManager.forget(p.getUUID());
				GhostSystem.forget(p.getUUID());
			}
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGhostPathIsHalfAliveAndWholeDead(GameTestHelper helper) {
		int nascent = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
		PlayerCultivation heaven = new PlayerCultivation();
		heaven.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		double life = CultivationStats.bodyHealth(TwoPaths.body(heaven, false));
		helper.assertTrue(Math.abs(life - 65) < 1.0e-6, "A living Nascent Soul of the heaven path: 65 health (" + life + ")");

		PlayerCultivation ghostly = new PlayerCultivation();
		ghostly.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		ghostPath(ghostly, nascent);
		TwoPaths.Body alive = TwoPaths.body(ghostly, false);
		helper.assertTrue(alive.ghostPath() && alive.share() == TwoPaths.HALF_PATH && Math.abs(CultivationStats.bodyHealth(alive) - life / 2) < 1.0e-6,
				"The ghost path alone, alive: half of it (" + CultivationStats.bodyHealth(alive) + ")");
		ghostly.netherworld().setGhost(true);
		TwoPaths.Body dead = TwoPaths.body(ghostly, false);
		helper.assertTrue(dead.share() == 1.0 && Math.abs(CultivationStats.bodyHealth(dead) - life) < 1.0e-6,
				"...dead, the whole of it: as a life's heaven path (" + CultivationStats.bodyHealth(dead) + ")");

		heaven.netherworld().setGhost(true);
		helper.assertTrue(Math.abs(CultivationStats.bodyHealth(TwoPaths.body(heaven, false)) - life / 2) < 1.0e-6,
				"The heaven path alone, dead: half of it");
		heaven.netherworld().setGhost(false);
		// Both paths at Nascent Soul: nothing lost, alive or dead (the harmony on top).
		ghostPath(heaven, nascent);
		double harmony = 1 + TwoPaths.HARMONY[0];
		for (boolean isDead : new boolean[] {false, true}) {
			heaven.netherworld().setGhost(isDead);
			TwoPaths.Body both = TwoPaths.body(heaven, false);
			helper.assertTrue(both.share() == 1.0 && Math.abs(CultivationStats.bodyHealth(both) - (20 + 45 * harmony)) < 1.0e-6,
					"Both paths, " + (isDead ? "dead" : "alive") + ": the whole (" + CultivationStats.bodyHealth(both) + ")");
		}
		// The heaven path high enough overtakes half the ghost path's: Qi Refining's whole is less than half a Nascent Soul's, Core
		// Formation's isn't.
		PlayerCultivation rising = new PlayerCultivation();
		rising.setState(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION, 0);
		ghostPath(rising, nascent);
		helper.assertTrue(!TwoPaths.body(rising, false).ghostPath(), "A heaven path of Core Formation Grand Perfection outweighs half a Nascent Soul's ghost path");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSectLibraryKeepsJadeSlips(GameTestHelper helper) {
		var table = helper.getLevel().getServer().getLootData().getLootTable(SectPiece.LIBRARY_LOOT);
		int slips = 0;
		for (int i = 0; i < 20; i++) {
			var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(helper.getLevel())
					.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)))
					.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
			int here = 0;
			for (net.minecraft.world.item.ItemStack stack : table.getRandomItems(params)) {
				if (!stack.is(ModItems.JADE_SLIP)) continue;
				HeavenMethod method = JadeSlipItem.method(stack);
				helper.assertTrue(method != null && method.realm().ordinal() <= Realm.CORE_FORMATION.ordinal(),
						"A slip of a heaven method a sect's library keeps (" + method + ")");
				here += stack.getCount();
			}
			helper.assertTrue(here >= 1, "Every library chest keeps a slip or two (" + here + ")");
			slips += here;
		}
		helper.assertTrue(slips >= 20 && slips <= 40, "One or two a chest (" + slips + " in 20)");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadTurnInwardToo(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerPlayer p = null;
		try {
			p = survivor(helper, made, "dead-inward");
			PlayerCultivation c = CultivationManager.own(p);
			c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
			ghostPath(c, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			GhostSystem.makeGhost(p);
			helper.assertTrue(GhostSystem.isKing(p) && InnerRealm.mayTurnInward(p), "One of the dead may turn inward");
			// (The game tests' server has no Inner Realm to go to: the way in is a world's that has one.)
			if (InnerRealm.enter(p)) {
				net.minecraft.nbt.CompoundTag back = c.getInnerReturn();
				var level = helper.getLevel().getServer().getLevel(InnerRealm.bodyDimension(p));
				helper.assertTrue(InnerRealm.isInside(p) && level != null && level.getEntity(back.getUUID("Body")) instanceof InnerBodyEntity body
						&& body.look() == TwoPaths.Look.KING, "Its form sits on where it meditated, crowned");
				InnerRealm.leave(p);
			}
			helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.KING, "...as it is drawn (InnerRealm#enter gives its body that look)");
			HeartDemonEntity demon = new HeartDemonEntity(ModEntities.HEART_DEMON, p.serverLevel());
			helper.assertTrue(GhostSystem.reaches(p.damageSources().mobAttack(demon)) && GhostSystem.attack(p, demon),
					"Its heart demon reaches it, and it its heart demon");
			demon.discard();
			helper.succeed();
		} finally {
			if (p != null) {
				InnerRealm.leave(p);
				GhostSystem.forget(p.getUUID());
			}
			leave(made);
		}
	}

	// --- Helpers ---

	/** The ghost path set at {@code rank}, with the foundations that carry it there. */
	private static void ghostPath(PlayerCultivation c, int rank) {
		NetherworldState s = c.netherworld();
		for (GhostMethod method : GhostMethod.values()) if (method.isFoundation()) s.setMethod(method, 1, 0);
		GhostMethods.setPath(s, rank);
	}

	/** A living Nascent Soul cultivator in a Ghost King's form (it once reigned among the dead; Form Shift shows that form). */
	private static ServerPlayer kingForm(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = survivor(helper, made, name);
		PlayerCultivation c = CultivationManager.own(p);
		c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		c.netherworld().setReignedKing(true);
		c.setAbilityEnabled(Ability.FORM_SHIFT, true);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		c.setQi(c.maxQi());
		return p;
	}

	private static void project(GameTestHelper helper, ServerPlayer p) {
		long now = helper.getLevel().getGameTime();
		p.setOnGround(true);
		SoulProjection.toggle(p);
		for (int i = 0; i < SoulProjection.CHANNEL_TICKS; i++) SoulProjection.tick(p, now);
		helper.assertTrue(SoulProjection.isSoul(p), "The soul is out");
	}

	private static void recall(GameTestHelper helper, ServerPlayer p) {
		long now = helper.getLevel().getGameTime();
		SoulProjection.toggle(p);
		for (int i = 0; i < SoulProjection.CHANNEL_TICKS; i++) SoulProjection.tick(p, now);
	}

	private static SoulBodyEntity bodyNear(GameTestHelper helper, ServerPlayer owner) {
		for (SoulBodyEntity body : helper.getLevel().getEntitiesOfClass(SoulBodyEntity.class, owner.getBoundingBox().inflate(40))) {
			if (owner.getUUID().equals(body.getOwnerId())) return body;
		}
		return null;
	}

	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}

			@Override
			public boolean isSpectator() {
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

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) if (Possession.inhabits(p.getUUID())) SoulProjection.letGo(p, null);
		for (ServerPlayer p : made) {
			SoulProjection.onDeath(p);
			CultivationManager.own(p).setPossession(null);
			p.server.getPlayerList().remove(p);
		}
	}
}
