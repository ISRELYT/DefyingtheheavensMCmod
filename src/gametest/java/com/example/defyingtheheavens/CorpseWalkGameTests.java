package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Walking in the dead (CorpseWalk, the user's picks of 2026-11-13): a projected soul, or one of the dead as it is, rises in a body of the dead
 * as what the dead was, and lays it down again where it stands, holding what it carried; the body rots on in its walker's keeping, the slower
 * the higher the walker, its realm whole for all its rot; it gives off its yin; the righteous fall on it, mortals run, its own sect turns on
 * its walker; meditating in it refines Corpse Yin Refining for the walker's own record. Bodies are laid by hand here (CorpseBlockEntity#of, as a
 * death lays them) and taken up after, so the kept test world holds none.
 */
public class CorpseWalkGameTests implements FabricGameTest {
	/** Where the body lies (relative): beside where the test players stand. */
	private static final BlockPos BODY = new BlockPos(3, 2, 2);

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSoulRisesInTheDeadAndLaysItBack(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		BlockPos coreAt = helper.absolutePos(new BlockPos(6, 2, 6));
		Corpses.inTests = true;
		try {
			int rank = PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION);
			BlockPos at = lay(helper, cultivator(helper, null, BODY, rank, 0), BODY, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.LEATHER_CHESTPLATE));
			CorpseBlockEntity body = body(level, at);
			UUID id = body.id();
			helper.assertTrue(body.record() != null && body.dead() != null, "The body keeps what the dead was, and whose it was");
			ServerPlayer soul = projected(helper, made, "walk-soul", Realm.NASCENT_SOUL);
			soul.getInventory().setItem(3, new ItemStack(Items.GOLD_INGOT, 2)); // the soul's own things (it carries them, touching nothing)

			helper.assertTrue(SoulProjection.tryPossessCorpse(soul, at) == InteractionResult.SUCCESS, "The soul rises in the body");
			helper.assertTrue(Possession.inCorpse(soul.getUUID()) && CorpseWalk.walking(soul) && SoulProjection.state(soul) == SoulProjection.State.INHABITING
					&& !level.getBlockState(at).is(ModBlocks.CORPSE), "It walks in it, lifted whole from where it lay");
			helper.assertTrue(soul.getItemBySlot(EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE) && soul.getInventory().countItem(Items.IRON_SWORD) == 1
					&& soul.getInventory().countItem(Items.GOLD_INGOT) == 0, "What the body held is the soul's to use, its robe on it; the soul's own things put by");
			PlayerCultivation as = CultivationManager.get(soul);
			helper.assertTrue(as != CultivationManager.own(soul) && as.getRealm() == Realm.QI_REFINING && as.getStage() == Stage.GRAND_PERFECTION
					&& !GhostSystem.isGhost(soul), "It is what the dead was: " + as.getRealm() + " " + as.getStage());
			helper.assertTrue(soul.getMobType() == MobType.UNDEAD && id.equals(Possession.identity(soul)) && soul.getHealth() == soul.getMaxHealth(),
					"Undead to the world, known to it as the body, its health whole (" + soul.getHealth() + " of " + soul.getMaxHealth() + ")");

			// The dead eat nothing, and found no sects.
			soul.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BREAD));
			helper.assertTrue(UseItemCallback.EVENT.invoker().interact(soul, level, InteractionHand.OFF_HAND).getResult() == InteractionResult.FAIL,
					"The dead eat nothing");
			soul.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			level.setBlock(coreAt, ModBlocks.FORMATION_CORE.defaultBlockState(), Block.UPDATE_ALL);
			FormationCoreBlockEntity core = (FormationCoreBlockEntity) level.getBlockEntity(coreAt);
			core.setOwner(soul);
			helper.assertTrue(ModLang.MSG_PSECT_DEAD.equals(key(PlayerSects.refusal(soul, core))), "The dead found no sects: " + key(PlayerSects.refusal(soul, core)));

			// A step on, and out of it again.
			soul.moveTo(soul.getX() + 1, soul.getY(), soul.getZ(), 90, 0);
			SoulProjection.toggle(soul);
			helper.assertTrue(!Possession.inCorpse(soul.getUUID()) && !CorpseWalk.walking(soul) && SoulProjection.isSoul(soul)
					&& soul.getInventory().getItem(3).is(Items.GOLD_INGOT) && soul.getInventory().countItem(Items.IRON_SWORD) == 0,
					"Out of it, a soul again, with its own things");
			BlockPos laid = bodyNear(level, soul.blockPosition());
			helper.assertTrue(laid != null, "The body lies down again where it stood");
			CorpseBlockEntity again = body(level, laid);
			helper.assertTrue(id.equals(again.id()) && again.rank() == rank && again.record() != null
					&& count(again.held(), Items.IRON_SWORD) == 1 && count(again.held(), Items.LEATHER_CHESTPLATE) == 1,
					"...the same body, holding what it carried: " + again.held());
			helper.assertTrue(level.getBlockState(laid).getValue(CorpseBlock.STAGE) == CorpseBlock.Rot.FRESH, "...as fresh as it was");
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			level.setBlock(coreAt, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			leave(helper, made);
		}
	}

	/** A soul sent out that walks in the dead is tethered to its own body still, as in a living one (the user, 2026-11-13). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aSoulInTheDeadIsTetheredStill(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		BlockPos far = null;
		Corpses.inTests = true;
		try {
			BlockPos at = lay(helper, cultivator(helper, null, BODY, PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), 0), BODY);
			ServerPlayer soul = projected(helper, made, "tethered-walker", Realm.NASCENT_SOUL);
			helper.assertTrue(SoulProjection.tryPossessCorpse(soul, at) == InteractionResult.SUCCESS, "Risen");
			int radius = CultivationManager.own(soul).soulTetherRadius();
			double x = soul.getX(), y = soul.getY(), z = soul.getZ();
			soul.moveTo(x, y + radius - 6, z, 0, 0); // up, over its own ground (its body far off would lie on another test's)
			SoulProjection.tick(soul, level.getGameTime());
			helper.assertTrue(CorpseWalk.walking(soul), "Within its tether (" + radius + " blocks) it walks on");
			soul.moveTo(x, y + radius + 6, z, 0, 0);
			SoulProjection.tick(soul, level.getGameTime());
			far = bodyBelow(level, soul.blockPosition());
			helper.assertTrue(!CorpseWalk.walking(soul) && SoulProjection.isSoul(soul) && far != null,
					"Past its tether's end the soul is pulled out of the body, as out of a living one's; the body lies down: " + far);
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			takeUp(level, far);
			leave(helper, made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void itRotsInItsWalkersKeepingTheSlowerTheHigher(GameTestHelper helper) {
		helper.assertTrue(CorpseWalk.rotSlowed(Realm.QI_REFINING.ordinal()) == 1 && CorpseWalk.rotSlowed(Realm.NASCENT_SOUL.ordinal()) == 2.5,
				"A Qi Refining walker's body rots as it would lying; a Nascent Soul's 2.5 times as slowly");
		helper.assertTrue(CorpseWalk.rot(0) == CorpseBlock.Rot.FRESH && CorpseWalk.rot(Corpses.FRESH_TICKS) == CorpseBlock.Rot.ROTTING
				&& CorpseWalk.rot(Corpses.BONES_AT) == CorpseBlock.Rot.BONES, "Fresh, rotting, bones: as a body lying");
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		Corpses.inTests = true;
		try {
			BlockPos at = lay(helper, cultivator(helper, null, BODY, PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), 0), BODY);
			ServerPlayer soul = projected(helper, made, "rot-soul", Realm.NASCENT_SOUL);
			helper.assertTrue(SoulProjection.tryPossessCorpse(soul, at) == InteractionResult.SUCCESS, "Risen");
			float health = soul.getMaxHealth();
			double age = Possession.corpseAge(soul);
			soul.getFoodData().setFoodLevel(3);
			CorpseWalk.tick(level.getServer());
			helper.assertTrue(Math.abs(Possession.corpseAge(soul) - age - 1 / 2.5) < 1e-9, "A tick rots it 1/2.5 of a tick lying: " + (Possession.corpseAge(soul) - age));
			helper.assertTrue(soul.getFoodData().getFoodLevel() == 20, "It hungers for nothing");
			Possession.setCorpseAge(soul, Corpses.FRESH_TICKS - 0.1);
			CorpseWalk.tick(level.getServer());
			helper.assertTrue(CorpseWalk.rot(Possession.corpseAge(soul)) == CorpseBlock.Rot.ROTTING, "Past two minutes' rot, rotting");
			helper.assertTrue(CultivationManager.get(soul).getRealm() == Realm.FOUNDATION_BUILDING && soul.getMaxHealth() == health,
					"...its realm and strength whole for all its rot");
			Possession.setCorpseAge(soul, Corpses.BONES_AT + 10);
			SoulProjection.toggle(soul);
			BlockPos laid = bodyNear(level, soul.blockPosition());
			helper.assertTrue(laid != null && level.getBlockState(laid).getValue(CorpseBlock.STAGE) == CorpseBlock.Rot.BONES, "Left as bones, it lies as bones");
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			leave(helper, made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyTheWeakerDeadRiseUnderASoulAndOnesOwnAlways(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		Corpses.inTests = true;
		try {
			ServerPlayer p = survivor(helper, made, "refused-walker");
			PlayerCultivation c = CultivationManager.own(p);
			c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
			CultivationStats.apply(p, c);
			BlockPos weak = lay(helper, cultivator(helper, null, BODY, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION), 0), BODY);
			BlockPos strong = lay(helper, cultivator(helper, null, new BlockPos(3, 2, 4), PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), 0),
					new BlockPos(3, 2, 4));
			helper.assertTrue(CorpseWalk.refusal(p, body(level, weak)) == null, "A realm below one's own: it may be walked in");
			helper.assertTrue(ModLang.MSG_POSSESS_TOO_STRONG.equals(CorpseWalk.refusal(p, body(level, strong))), "One's own realm's dead: too strong a body");
			BlockPos own = lay(helper, p, new BlockPos(5, 2, 2));
			helper.assertTrue(body(level, own).rank() == PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY) && CorpseWalk.refusal(p, body(level, own)) == null,
					"One's own body, whatever its realm");
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			leave(helper, made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGhostWalksInItsOwnBodyAsItIs(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		Corpses.inTests = true;
		try {
			ServerPlayer p = survivor(helper, made, "ghost-walker");
			PlayerCultivation c = CultivationManager.own(p);
			c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
			CultivationStats.apply(p, c);
			BlockPos at = lay(helper, p, BODY);
			GhostSystem.makeGhost(p);
			helper.assertTrue(GhostSystem.isGhost(p), "Dead, a ghost");
			NetherworldState s = c.netherworld();
			s.setGhostQi(TwoPaths.poolMax(c, TwoPaths.Path.GHOST));
			double full = s.ghostQi();
			int alignment = c.getAlignment();

			helper.assertTrue(GhostPossession.enterCorpse(p, at) == InteractionResult.SUCCESS, "The ghost steps into its own body, no soul sent out");
			helper.assertTrue(GhostPossession.isTenant(p) && !GhostSystem.isGhost(p) && CultivationManager.get(p).getRealm() == Realm.CORE_FORMATION
					&& !level.getBlockState(at).is(ModBlocks.CORPSE), "It walks in it as what it was, no ghost to the world");
			helper.assertTrue(s.ghostQi() < full && c.getAlignment() == alignment - Corpses.BLEED_SHIFT,
					"Its ghostly qi paid the way in, its path drawn toward the devilish (" + alignment + " to " + c.getAlignment() + ")");
			PlayerCultivation walked = CultivationManager.get(p);
			helper.assertTrue(onWheel(walked, Ability.GHOST_POSSESSION) && Ability.GHOST_POSSESSION.isUnlocked(walked) && !Ability.SOUL_PROJECTION.isUnlocked(walked),
					"Its own possession on the body's wheel, the way out (not Soul Projection)");
			// No soul sent out: nothing tethers it (the user, 2026-11-13), however far it walks.
			double x = p.getX(), y = p.getY(), z = p.getZ();
			p.moveTo(x, y + 100, z, 0, 0);
			SoulProjection.tick(p, level.getGameTime());
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()) && GhostPossession.isTenant(p), "Untethered: a hundred blocks off, it walks on");
			p.moveTo(x, y, z, 0, 0);
			double before = s.ghostQi();
			GhostPossession.second(p);
			helper.assertTrue(Math.abs(before - s.ghostQi() - GhostPossession.UPKEEP) < 1e-6, "A second's upkeep, the same anywhere: " + (before - s.ghostQi()));
			s.setGhostQi(0);
			GhostPossession.second(p);
			helper.assertTrue(!Possession.inCorpse(p.getUUID()) && !GhostPossession.isTenant(p) && GhostSystem.isGhost(p),
					"Its ghostly qi run dry, it is out of it, a ghost again");
			helper.assertTrue(bodyNear(level, p.blockPosition()) != null, "...and the body lies again");
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			leave(helper, made);
		}
	}

	/**
	 * The dead's own possession takes the living too (GhostPossession; the user's picks of 2026-11-13: the living and the dead, every ghost): a
	 * cultivator's body and a player's, at least a realm below the ghost, no soul sent out and nothing tethering it; out again by the ability.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aGhostTakesTheLivingUntethered(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<CultivatorNpc> npcs = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		try {
			ServerPlayer p = survivor(helper, made, "ghost-taker");
			PlayerCultivation c = CultivationManager.own(p);
			c.setState(Realm.FOUNDATION_BUILDING, Stage.EARLY, 0);
			CultivationStats.apply(p, c);
			GhostSystem.makeGhost(p);
			NetherworldState s = c.netherworld();
			s.setGhostQi(TwoPaths.poolMax(c, TwoPaths.Path.GHOST));
			helper.assertTrue(Ability.GHOST_POSSESSION.isUnlocked(c) && !Ability.SOUL_PROJECTION.isUnlocked(c),
					"A Foundation Building ghost has its own possession, no Nascent Soul to send out");
			CultivatorNpc strong = cultivator(helper, npcs, new BlockPos(3, 2, 4), PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), 0);
			helper.assertTrue(GhostPossession.enter(p, strong) == InteractionResult.FAIL && !GhostPossession.isTenant(p), "Its own realm's body: too strong");
			CultivatorNpc npc = cultivator(helper, npcs, BODY, PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 0);
			helper.assertTrue(GhostPossession.enter(p, npc) == InteractionResult.SUCCESS, "The ghost takes a Qi Refining cultivator's body");
			PlayerCultivation body = CultivationManager.get(p);
			helper.assertTrue(GhostPossession.isTenant(p) && npc.isVacant() && body == npc.cultivation() && !GhostSystem.isGhost(p)
					&& !SoulProjection.isActive(p.getUUID()), "It is the body now: no ghost to the world, no soul out, no tether");
			helper.assertTrue(onWheel(body, Ability.GHOST_POSSESSION) && Ability.GHOST_POSSESSION.isUnlocked(body) && !Ability.SOUL_PROJECTION.isUnlocked(body),
					"Its own possession on the body's wheel, the way out");
			double x = p.getX(), y = p.getY(), z = p.getZ();
			p.moveTo(x, y + 80, z, 0, 0);
			SoulProjection.tick(p, level.getGameTime());
			GhostPossession.second(p);
			helper.assertTrue(GhostPossession.isTenant(p), "Far off, nothing draws it back");
			p.moveTo(x, y, z, 0, 0);
			GhostPossession.act(p);
			helper.assertTrue(!GhostPossession.isTenant(p) && !npc.isVacant() && GhostSystem.isGhost(p) && !Possession.inhabits(p.getUUID()),
					"Used again, it leaves: the cultivator its own again, the ghost a ghost");

			// A living player's body too, mortal, the PvP rules as for any possession.
			ServerPlayer victim = survivor(helper, made, "ghost-victim");
			victim.moveTo(p.getX() + 1, p.getY(), p.getZ(), 90, 0);
			CultivationManager.own(victim).setMortal(true);
			CultivationStats.apply(victim, CultivationManager.own(victim));
			s.setGhostQi(TwoPaths.poolMax(c, TwoPaths.Path.GHOST));
			helper.assertTrue(GhostPossession.enter(p, victim) == InteractionResult.SUCCESS && victim.gameMode.getGameModeForPlayer() == GameType.SPECTATOR
					&& victim.getCamera() == p && Possession.isHeld(victim.getUUID()), "It takes a player's body, the player watching through it");
			GhostPossession.act(p);
			helper.assertTrue(victim.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && victim.getCamera() == victim && GhostSystem.isGhost(p),
					"Let go of, the body is its owner's again");
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			leave(helper, made);
			for (CultivatorNpc npc : npcs) npc.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void meditatingInTheDeadRefinesForTheWalkersOwnGhostPath(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		Corpses.inTests = true;
		try {
			int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
			BlockPos at = lay(helper, cultivator(helper, null, BODY, rank, 0), BODY);
			ServerPlayer soul = survivor(helper, made, "yin-walker");
			PlayerCultivation own = CultivationManager.own(soul);
			own.awaken(0);
			own.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
			GhostMethods.give(soul, GhostMethod.NETHERWORLD_BREATHING);
			CultivationMethods.learn(soul, CultivationMethod.CORPSE_YIN, Realm.HEAVENLY_BEING);
			CultivationMethods.practise(soul, CultivationMethod.CORPSE_YIN);
			NetherworldState s = own.netherworld();
			helper.assertTrue(GhostMethods.ghostRank(s) == 0 && CultivationMethods.practised(own, TwoPaths.Path.GHOST) == CultivationMethod.CORPSE_YIN,
					"Its own ghost path begun, practising Corpse Yin Refining");
			project(helper, soul);
			helper.assertTrue(SoulProjection.tryPossessCorpse(soul, at) == InteractionResult.SUCCESS, "Risen in a Core Formation body");

			double was = s.pathCultivation();
			CorpseWalk.meditate(soul, CorpseWalk.MEDITATE_SECONDS / 2);
			double share = CorpseYin.share(rank, 0, CorpseBlock.Rot.FRESH) / 2;
			helper.assertTrue(share > 0 && Math.abs(Possession.corpseYin(soul) - share) < 1e-9 && s.pathCultivation() == was,
					"A minute's meditation owes half the body's share, none of it paid yet (" + Possession.corpseYin(soul) + " of " + share + ")");
			SoulProjection.toggle(soul);
			helper.assertTrue(Math.abs(s.pathCultivation() - was - share * GhostMethods.required(0)) < 1e-6,
					"Out of it, the walker's own ghost path has it: " + (s.pathCultivation() - was) + " of " + share * GhostMethods.required(0));
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			leave(helper, made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRighteousFallOnItMortalsRunAndItsSectTurns(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<CultivatorNpc> npcs = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		Sect sect = new Sect(UUID.randomUUID());
		Corpses.inTests = true;
		try {
			sect.name = "Walking Dead Test Sect";
			sect.dimension = level.dimension();
			sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
			SectData.get(level.getServer()).put(sect);
			int qr = PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID);
			CultivatorNpc dead = cultivator(helper, null, BODY, qr, 0);
			join(dead, sect, 0);
			BlockPos at = lay(helper, dead, BODY);
			helper.assertTrue(sect.name.equals(body(level, at).sect()), "The body knows its sect");
			CultivatorNpc mate = cultivator(helper, npcs, new BlockPos(6, 2, 2), qr, 0);
			join(mate, sect, 1);
			CultivatorNpc righteous = cultivator(helper, npcs, new BlockPos(3, 2, 6), qr, 150);
			CultivatorNpc devilish = cultivator(helper, npcs, new BlockPos(6, 2, 6), qr, -150);
			CultivatorNpc mortal = cultivator(helper, npcs, new BlockPos(0, 2, 6), CultivatorNpc.MORTAL, 0);
			ServerPlayer soul = projected(helper, made, "seen-walker", Realm.NASCENT_SOUL);
			helper.assertTrue(SoulProjection.tryPossessCorpse(soul, at) == InteractionResult.SUCCESS, "Risen");
			soul.setHealth(soul.getMaxHealth() / 2);
			float hurt = soul.getHealth();

			CorpseWalk.second(soul, CultivationManager.own(soul));
			helper.assertTrue(soul.getHealth() > hurt, "It mends a little by the yin about it (" + hurt + " to " + soul.getHealth() + ")");
			helper.assertTrue(sect.standing(soul.getUUID()) <= Regard.HOSTILE && mate.getTarget() == soul,
					"Its own sect sees its dead walk, and turns on its walker for good (" + sect.standing(soul.getUUID()) + ")");
			helper.assertTrue(righteous.getTarget() == soul && righteous.regardEntry(soul).value <= Regard.HOSTILE, "The righteous fall on it");
			helper.assertTrue(mortal.shouldFlee(), "Mortals run from it");
			helper.assertTrue(devilish.getTarget() == null, "The devilish let it be");

			// Its yin about it as it walks, as it gave lying; none past its reach, and its own feeds it nothing.
			double near = GhostQi.densityAt(level, soul.blockPosition().above());
			double far = GhostQi.densityAt(level, soul.blockPosition().offset(Corpses.yinRadius(qr) + 3, 0, 0));
			helper.assertTrue(Math.abs(near - (GhostQi.OVERWORLD + Corpses.yin(qr, false))) < 1e-9 && Math.abs(far - GhostQi.OVERWORLD) < 1e-9,
					"It gives off its yin as it walks (" + near + ", " + far + ")");
			helper.assertTrue(Math.abs(CorpseWalk.densityAt(level, soul.blockPosition(), soul.getUUID()) - GhostQi.OVERWORLD) < 1e-9,
					"...none of it its own to feed on");

			// Its health spent, it falls back down where it stands; the soul is thrown out unharmed.
			soul.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
			helper.assertTrue(soul.isAlive() && soul.getHealth() > 0 && SoulProjection.isSoul(soul) && !Possession.inCorpse(soul.getUUID()),
					"Its last health spent, the body falls and the soul is thrown out, unharmed");
			helper.assertTrue(bodyNear(level, soul.blockPosition()) != null, "...the body lying again");
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			sect.extinct = true;
			for (CultivatorNpc npc : npcs) npc.discard();
			leave(helper, made);
		}
	}

	// --- Helpers ---

	/** A cultivator of {@code rank} and {@code alignment} at {@code rel}, its mind silent; kept in {@code npcs} (if given) to take away after. */
	private static CultivatorNpc cultivator(GameTestHelper helper, List<CultivatorNpc> npcs, BlockPos rel, int rank, int alignment) {
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, rel);
		npc.setRank(rank);
		npc.setAlignment(alignment);
		if (npcs != null) npcs.add(npc);
		return npc;
	}

	/** {@code npc} one of {@code sect}'s, at its place {@code home}. */
	private static void join(CultivatorNpc npc, Sect sect, int home) {
		npc.joinSect(sect, home);
		sect.members.put(npc.getUUID(), new Sect.Member(npc.getUUID(), npc.getRank(), home));
	}

	/**
	 * A body of {@code dead} laid at {@code rel}, as its death would lay it (CorpseBlockEntity#of: a player's, or a cultivator's, which is taken
	 * away), holding {@code items}. @return where it lies
	 */
	private static BlockPos lay(GameTestHelper helper, LivingEntity dead, BlockPos rel, ItemStack... items) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(rel);
		level.setBlock(at, ModBlocks.CORPSE.defaultBlockState(), Block.UPDATE_ALL);
		CorpseBlockEntity body = body(level, at);
		body.of(dead, level.getGameTime());
		for (ItemStack stack : items) body.hold(stack);
		if (dead instanceof CultivatorNpc npc) npc.discard();
		return at;
	}

	private static CorpseBlockEntity body(ServerLevel level, BlockPos at) {
		return (CorpseBlockEntity) level.getBlockEntity(at);
	}

	/** The body nearest {@code pos} within a few blocks (it lies on the ground under where it was let go), or null. */
	private static BlockPos bodyNear(ServerLevel level, BlockPos pos) {
		for (BlockPos p : BlockPos.withinManhattan(pos, 3, 4, 3)) if (level.getBlockState(p).is(ModBlocks.CORPSE)) return p.immutable();
		return null;
	}

	/** A body under {@code pos}, as far down as the ground (let go in the air, it lies a few blocks down: Corpses#placeFor), or null. */
	private static BlockPos bodyBelow(ServerLevel level, BlockPos pos) {
		for (int down = -2; down <= 64; down++) {
			BlockPos p = bodyNear(level, pos.below(down));
			if (p != null) return p;
		}
		return null;
	}

	/** A body left by a test taken up whole: what it held taken out first, so nothing spills into the kept world. */
	private static void takeUp(ServerLevel level, BlockPos at) {
		if (at == null || !level.getBlockState(at).is(ModBlocks.CORPSE)) return;
		if (level.getBlockEntity(at) instanceof CorpseBlockEntity body) body.takeAll();
		level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(4)).forEach(ItemEntity::discard);
	}

	/** Whether {@code ability} is on {@code c}'s ability wheel. */
	private static boolean onWheel(PlayerCultivation c, Ability ability) {
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) if (c.getWheelSlot(i) == ability) return true;
		return false;
	}

	private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
		return stacks.stream().mapToInt(s -> s.is(item) ? s.getCount() : 0).sum();
	}

	private static String key(Component line) {
		return line != null && line.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(line);
	}

	/** A survival player in the player list (the helper's mock player is creative), remembered in {@code made} to take out after. */
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

	/** A cultivator of {@code realm} Early with its soul already out, full of qi. */
	private static ServerPlayer projected(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm) {
		ServerPlayer p = survivor(helper, made, name);
		CultivationManager.get(p).setState(realm, Stage.EARLY, 0);
		project(helper, p);
		return p;
	}

	/** {@code p}'s soul sent out, its strength reckoned and its qi full first. */
	private static void project(GameTestHelper helper, ServerPlayer p) {
		PlayerCultivation c = CultivationManager.own(p);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		c.setQi(c.maxQi());
		p.setOnGround(true);
		long now = helper.getLevel().getGameTime();
		SoulProjection.toggle(p);
		for (int i = 0; i < SoulProjection.CHANNEL_TICKS; i++) SoulProjection.tick(p, now);
		helper.assertTrue(SoulProjection.isSoul(p), "The soul is out");
	}

	/** Everyone out of the bodies they walk in (each lying down again), the players gone, and every body about the ground taken up. */
	private static void leave(GameTestHelper helper, List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			if (GhostPossession.isTenant(p)) GhostPossession.leave(p, null);
			if (Possession.inhabits(p.getUUID())) SoulProjection.letGo(p, null);
		}
		for (ServerPlayer p : made) {
			SoulProjection.onDeath(p); // nothing of a journey is left behind (its body discards itself)
			CultivationManager.own(p).setPossession(null);
			GhostSystem.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
		ServerLevel level = helper.getLevel();
		List<BlockPos> found = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(-3, 0, -3)), helper.absolutePos(new BlockPos(10, 6, 10)))) {
			if (level.getBlockState(pos).is(ModBlocks.CORPSE)) found.add(pos.immutable());
		}
		for (BlockPos at : found) takeUp(level, at);
	}
}
