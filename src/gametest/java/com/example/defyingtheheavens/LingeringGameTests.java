package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The soul after death (Lingering, the user's ask of 2026-11-16): one whose qi path stands at Nascent Soul or beyond (or who walks the ghost
 * path alone) rises where its body lies and lingers among the living until the Netherworld's pull takes it down; a body holds the pull back, never
 * for good; anyone's blow lands on it, and struck down or logging out it goes down. And the two bugs fixed with it: the dead spoke to the dead
 * as to the warm ("Still warm?"), and a right-click in a body one possessed tried to talk to that body (the client's side, TalkReach, is checked
 * in game). Driven by hand on survival test players in the player list, as CorpseWalkGameTests' are; one real death.
 */
public class LingeringGameTests implements FabricGameTest {
	private static final int NASCENT = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
	private static final int CORE = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);

	@GameTest(template = EMPTY_STRUCTURE)
	public void whoLingersAfterDeath(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		try {
			DamageSource common = level.damageSources().generic();
			ServerPlayer heaven = cultivator(helper, made, "linger-heaven", Realm.NASCENT_SOUL, -1);
			helper.assertTrue(Lingering.mayLinger(heaven, common, false) && !Lingering.pureGhost(CultivationManager.own(heaven)),
					"A Nascent Soul of the heaven path lingers after a common death, as its nascent soul");
			ServerPlayer core = cultivator(helper, made, "linger-core", Realm.CORE_FORMATION, -1);
			helper.assertTrue(!Lingering.mayLinger(core, common, false), "A Core Formation doesn't: no nascent soul to flee its body");
			ServerPlayer ghostly = cultivator(helper, made, "linger-ghostly", Realm.CORE_FORMATION, NASCENT);
			helper.assertTrue(Lingering.mayLinger(ghostly, common, false) && !Lingering.pureGhost(CultivationManager.own(ghostly)),
					"...but a Nascent Soul ghost path does, the highest path weighing (a soul, not a ghost: it walks the heaven path too)");
			ServerPlayer pure = pureGhost(helper, made, "linger-pure", 0);
			helper.assertTrue(Lingering.pureGhost(CultivationManager.own(pure)) && Lingering.mayLinger(pure, common, false),
					"One of the ghost path alone lingers at any realm (Qi Refining here)");
			ServerPlayer mortal = survivor(helper, made, "linger-mortal");
			CultivationManager.own(mortal).setMortal(true);
			helper.assertTrue(!Lingering.mayLinger(mortal, common, false), "A mortal goes straight down");

			helper.assertTrue(!Lingering.mayLinger(heaven, common, true), "Struck down by the heavens (a tribulation, the Five Decays): nothing lingers");
			for (DamageSource source : List.of(ModDamageTypes.judgeGaze(level), ModDamageTypes.oldAge(level), ModDamageTypes.soulScatter(level),
					ModDamageTypes.soulSevered(level), ModDamageTypes.soulSeized(level, null), ModDamageTypes.chaosStorm(level))) {
				helper.assertTrue(Lingering.destroysTheSoul(source, false) && !Lingering.mayLinger(heaven, source, false),
						"Nothing lingers after what takes the soul with the body: " + source.getMsgId());
			}
			helper.assertTrue(!Lingering.destroysTheSoul(ModDamageTypes.coreDetonation(level), false) && !Lingering.destroysTheSoul(common, false),
					"A core detonated, a common death: the nascent soul flees the body");

			GhostSystem.toGate(heaven);
			helper.assertTrue(ModDimensions.isNetherworld(heaven.level().dimension()) && !Lingering.mayLinger(heaven, common, false),
					"Dying in the Netherworld: already where the dead go");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void thePullHoldsLongerUpTheGhostPath(GameTestHelper helper) {
		PlayerCultivation heaven = record(Realm.NASCENT_SOUL, -1, true);
		helper.assertTrue(Lingering.seconds(heaven) == 90 && Lingering.BASE_SECONDS == 90, "The heaven path alone: a minute and a half");
		PlayerCultivation ghostly = record(Realm.NASCENT_SOUL, NASCENT, true);
		helper.assertTrue(Lingering.seconds(ghostly) == 90 + 6 * (NASCENT + 1),
				"Six seconds more for each stage of the ghost path (Nascent Soul Early: " + Lingering.seconds(ghostly) + " s)");
		PlayerCultivation lord = record(Realm.NASCENT_SOUL, PlayerCultivation.rank(Realm.HEAVENLY_LORD, Stage.GRAND_PERFECTION), true);
		helper.assertTrue(Lingering.seconds(lord) > Lingering.seconds(ghostly) && Lingering.seconds(lord) < 15 * 60,
				"...the higher the longer, never without end (Heavenly Lord Grand Perfection: " + Lingering.seconds(lord) + " s)");
		PlayerCultivation pure = record(Realm.QI_REFINING, NASCENT, false);
		helper.assertTrue(Lingering.pureGhost(pure) && Lingering.seconds(pure) == 2 * Lingering.seconds(ghostly),
				"One of the ghost path alone resists it twice as long");
		PlayerCultivation manual = record(Realm.QI_REFINING, NASCENT, false);
		manual.setMethodReach(heavenManual(), 0);
		helper.assertTrue(!Lingering.pureGhost(manual), "A heaven method from a manual is the heaven path walked too: not the ghost path alone");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aNascentSoulRisesWhereItsBodyLies(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = cultivator(helper, made, "linger-rise", Realm.NASCENT_SOUL, CORE);
			PlayerCultivation c = CultivationManager.own(p);
			NetherworldState s = c.netherworld();
			BlockPos died = p.blockPosition();
			p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE); // a real death: the death's own reckoning (AFTER_DEATH) binds it
			helper.assertTrue(p.isDeadOrDying() && s.lingering() && !s.boundBelow() && !s.ghost(), "Dead, bound to linger: not for the Ghost Gate");
			helper.assertTrue(p.getRespawnDimension() == helper.getLevel().dimension() && p.getRespawnPosition() != null && p.isRespawnForced()
					&& p.getRespawnPosition().distManhattan(died) <= 2, "Its respawn where its body lies (" + p.getRespawnPosition() + ", died at " + died + ")");
			GhostSystem.onRespawn(p, false); // as its respawn does
			helper.assertTrue(GhostSystem.isGhost(p) && Lingering.lingers(p) && Lingering.isSoul(p) && s.pull() == 0,
					"Risen as its nascent soul: one of the dead, lingering among the living");
			helper.assertTrue(p.getRespawnPosition() == null, "Its own respawn point given back (it had none)");
			helper.assertTrue(TwoPaths.ghostRank(c, false) >= GhostSystem.KING_RANK && !GhostSystem.isKing(p),
					"A Core Formation ghost path, but no Ghost King's crown while it lingers");

			double seconds = Lingering.seconds(c);
			Lingering.second(p);
			helper.assertTrue(Math.abs(s.pull() - 1 / seconds) < 1e-6 && Lingering.secondsLeft(p) == (int) Math.ceil((1 - s.pull()) * seconds),
					"A second out of a body: 1/" + seconds + " of the pull (" + s.pull() + ")");
			s.setPull(0.9999f);
			Lingering.second(p);
			helper.assertTrue(!s.lingering() && s.ghost() && ModDimensions.isNetherworld(p.level().dimension()) && !Lingering.isSoul(p),
					"The pull full, it is taken down to the Ghost Gate, one of the dead");
			helper.assertTrue(GhostSystem.isKing(p), "...and there a Ghost King, its crown its own now");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void oneOfTheGhostPathAloneRisesAsAGhost(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = pureGhost(helper, made, "linger-pure-king", CORE);
			ServerPlayer striker = survivor(helper, made, "linger-pure-striker");
			linger(helper, p);
			PlayerCultivation c = CultivationManager.own(p);
			helper.assertTrue(Lingering.lingers(p) && !Lingering.isSoul(p) && GhostSystem.isGhost(p) && !Lingering.asSoul(c),
					"It lingers as one of the dead, not as a soul");
			helper.assertTrue(GhostSystem.isKing(p), "A Ghost King keeps its crown and its hands");
			helper.assertTrue(!GhostSystem.allowDamage(p, p.damageSources().playerAttack(striker)), "...and the dead's protection: a living fist passes through");
			SpellProjectile bolt = new SpellProjectile(ModEntities.SPELL_PROJECTILE, helper.getLevel());
			bolt.setOwner(striker);
			boolean pvp = helper.getLevel().getServer().isPvpAllowed();
			helper.getLevel().getServer().setPvpAllowed(true); // so only the dead's protection can turn it
			boolean hits = bolt.canHitEntity(p);
			helper.getLevel().getServer().setPvpAllowed(pvp);
			helper.assertTrue(!hits, "...and a spell's bolt passes through");
			helper.assertTrue(Lingering.seconds(c) == 2 * (90 + 6 * (CORE + 1)), "Its pull twice as long (" + Lingering.seconds(c) + " s)");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyHoldsThePullBackButNotForGood(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<CultivatorNpc> npcs = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		try {
			ServerPlayer p = cultivator(helper, made, "linger-taker", Realm.NASCENT_SOUL, -1);
			linger(helper, p);
			PlayerCultivation c = CultivationManager.own(p);
			NetherworldState s = c.netherworld();
			s.setGhostQi(0); // nothing to pay with: the pull is the price
			CultivatorNpc strong = npc(helper, npcs, new BlockPos(2, 2, 4), NASCENT);
			helper.assertTrue(GhostPossession.enter(p, strong) == InteractionResult.FAIL && !strong.isVacant(), "A body of its own realm: too strong");
			CultivatorNpc npc = npc(helper, npcs, new BlockPos(3, 2, 2), PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID));
			InteractionResult click = UseEntityCallback.EVENT.invoker().interact(p, level, InteractionHand.MAIN_HAND, npc, null);
			helper.assertTrue(click == InteractionResult.SUCCESS && GhostPossession.isTenant(p) && npc.isVacant() && s.ghostQi() == 0,
					"A right-click takes a Qi Refining cultivator's body, free of ghostly qi (" + click + ")");
			helper.assertTrue(!Lingering.isSoul(p) && Lingering.lingers(p) && !GhostSystem.isGhost(p) && !npc.isPickable(),
					"In it, it is the body (no soul to the world), still lingering");

			double seconds = Lingering.seconds(c), was = s.pull();
			Lingering.second(p);
			helper.assertTrue(Math.abs(s.pull() - was - Lingering.IN_A_BODY / seconds) < 1e-6, "In a body the pull fills a third as fast (" + (s.pull() - was) + ")");
			helper.assertTrue(Math.abs(Lingering.secondsLeft(p) - (1 - s.pull()) * seconds * 3) <= 1, "...so what is left of it lasts three times as long ("
					+ Lingering.secondsLeft(p) + " s)");
			GhostPossession.second(p);
			helper.assertTrue(GhostPossession.isTenant(p), "No upkeep: its ghostly qi empty, it stays in");

			s.setPull(0.9999f);
			Lingering.second(p);
			helper.assertTrue(!GhostPossession.isTenant(p) && !npc.isVacant() && !s.lingering() && s.ghost() && ModDimensions.isNetherworld(p.level().dimension()),
					"Never for good: the pull full, it is torn out of the body (given back) and taken down");
			helper.succeed();
		} finally {
			leave(made);
			for (CultivatorNpc npc : npcs) npc.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anyoneStrikesItAndStruckDownItGoesDown(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<CultivatorNpc> npcs = new ArrayList<>();
		try {
			ServerPlayer p = cultivator(helper, made, "linger-struck", Realm.NASCENT_SOUL, -1);
			linger(helper, p);
			ServerPlayer mortal = survivor(helper, made, "linger-striker");
			CultivationManager.own(mortal).setMortal(true);
			DamageSource fist = p.damageSources().playerAttack(mortal);
			helper.assertTrue(GhostSystem.allowDamage(p, fist) && GhostSystem.scaleDamage(p, fist, 10) == 10, "A mortal's fist lands on it, whole");
			helper.assertTrue(!GhostSystem.allowDamage(p, p.damageSources().fall()), "The world's own (a fall) passes through it, as through the dead");
			SpellProjectile bolt = new SpellProjectile(ModEntities.SPELL_PROJECTILE, helper.getLevel());
			bolt.setOwner(mortal);
			boolean pvp = helper.getLevel().getServer().isPvpAllowed();
			helper.getLevel().getServer().setPvpAllowed(true); // a player's spell spares players with PvP off (HeavenSpells#spared)
			boolean hits = bolt.canHitEntity(p);
			helper.getLevel().getServer().setPvpAllowed(pvp);
			helper.assertTrue(hits, "A spell's bolt finds it too, as any blow does (it passes through the dead)");

			CultivatorNpc seer = npc(helper, npcs, new BlockPos(4, 2, 2), NASCENT), blind = npc(helper, npcs, new BlockPos(4, 2, 4), CORE);
			boolean peaceful = helper.getLevel().getDifficulty() == Difficulty.PEACEFUL;
			helper.assertTrue(seer.canAttack(p) == !peaceful && !blind.canAttack(p),
					"A Nascent Soul cultivator may hunt it; one below, who doesn't see souls, may not (peaceful: " + peaceful + ")");

			SoulProjection.toggle(p);
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()), "Out of its body already: no soul to send out");

			GhostSystem.disperse(p);
			NetherworldState s = CultivationManager.own(p).netherworld();
			helper.assertTrue(!s.lingering() && s.reforming() && s.ghost() && !Lingering.isSoul(p), "Struck down, it is torn away to reform at the Gate");

			ServerPlayer q = cultivator(helper, made, "linger-logout", Realm.NASCENT_SOUL, -1);
			linger(helper, q);
			Lingering.onDisconnect(q);
			NetherworldState t = CultivationManager.own(q).netherworld();
			helper.assertTrue(!t.lingering() && t.reforming() && t.ghost(), "Logging out, the pull takes it: it reforms at the Gate on its return");
			helper.succeed();
		} finally {
			leave(made);
			for (CultivatorNpc npc : npcs) npc.discard();
		}
	}

	/** The user's report of 2026-11-16: the Gate's keepers and Meng Po called one long dead "still warm". */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadSpeakToTheDeadAsTheDead(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		GhostCultivatorNpc warden = helper.spawnWithNoFreeWill(ModEntities.GHOST_CULTIVATOR, new BlockPos(3, 2, 2));
		try {
			warden.setRank(CORE);
			ServerPlayer living = survivor(helper, made, "warm-mortal"), dead = survivor(helper, made, "dead-mortal");
			CultivationManager.own(living).setMortal(true);
			CultivationManager.own(dead).setMortal(true);
			GhostSystem.makeGhost(dead);
			helper.assertTrue(NpcAddress.rung(dead, warden) == NpcAddress.Rung.MORTAL && NpcAddress.rung(living, warden) == NpcAddress.Rung.MORTAL,
					"A mortal's ghost is a mortal to it still, as a living mortal is");
			String deadAddress = ModLang.address("mortal." + NpcLines.DEAD_TO_DEAD);
			helper.assertTrue(key(NpcAddress.address(dead, warden)).equals(deadAddress) && !key(NpcAddress.address(living, warden)).equals(deadAddress),
					"The dead call one of their own a little shade; the warm, as ever");
			for (int i = 0; i < 8; i++) {
				for (String kind : List.of("greet", "ask")) {
					String toDead = key(TalkSystem.lineFor(kind, warden, dead, null)), toLiving = key(TalkSystem.lineFor(kind, warden, living, null));
					helper.assertTrue(toDead.startsWith(ModLang.npcSay("talk." + kind + ".mortal." + NpcLines.DEAD_TO_DEAD + ".")),
							"To the dead, the dead's own words: " + toDead);
					helper.assertTrue(toLiving.startsWith(ModLang.npcSay("talk." + kind + ".mortal.ghost.")), "To the living, as ever: " + toLiving);
				}
				String passing = key(NpcLines.lineTo("pass.mortal", warden, dead, "x", "y"));
				helper.assertTrue(passing.startsWith(ModLang.npcSay("pass.mortal." + NpcLines.DEAD_TO_DEAD + ".")), "Passing by, too: " + passing);
			}
			helper.succeed();
		} finally {
			warden.discard();
			leave(made);
		}
	}

	// --- Helpers ---

	/** A record of the heaven path at {@code realm} Early ({@code scripture}: a heaven scripture known), its ghost path at {@code ghostRank} (-1 none). */
	private static PlayerCultivation record(Realm realm, int ghostRank, boolean scripture) {
		PlayerCultivation c = new PlayerCultivation();
		shape(c, realm, ghostRank, scripture);
		return c;
	}

	private static void shape(PlayerCultivation c, Realm realm, int ghostRank, boolean scripture) {
		c.setMortal(false);
		c.setState(realm, Stage.EARLY, 0);
		if (scripture) c.heaven().setMethod(HeavenMethod.WOOD_QI_REFINING, 1, 0);
		if (ghostRank >= 0) c.netherworld().setPath(ghostRank, 0);
	}

	/** A cultivation method of the heaven path's from a manual. */
	private static CultivationMethod heavenManual() {
		for (CultivationMethod method : CultivationMethod.values()) if (method.path() == TwoPaths.Path.HEAVEN) return method;
		throw new IllegalStateException("No heaven method from a manual");
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

	/** A cultivator of the heaven path at {@code realm} Early (a scripture known, as one must to have come so far), its ghost path at {@code ghostRank}. */
	private static ServerPlayer cultivator(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm, int ghostRank) {
		ServerPlayer p = survivor(helper, made, name);
		PlayerCultivation c = CultivationManager.own(p);
		shape(c, realm, ghostRank, true);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		return p;
	}

	/** One of the ghost path alone: meridians open, no heaven scripture, its ghost path at {@code ghostRank}. */
	private static ServerPlayer pureGhost(GameTestHelper helper, List<ServerPlayer> made, String name, int ghostRank) {
		ServerPlayer p = survivor(helper, made, name);
		PlayerCultivation c = CultivationManager.own(p);
		shape(c, Realm.QI_REFINING, ghostRank, false);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		return p;
	}

	/** {@code p} dead of a common death and risen, lingering (GhostSystem#onDeath, then its respawn: #onRespawn), where it stood. */
	private static void linger(GameTestHelper helper, ServerPlayer p) {
		GhostSystem.onDeath(p, p.damageSources().generic(), false);
		GhostSystem.onRespawn(p, false);
		helper.assertTrue(Lingering.lingers(p), "Risen, lingering");
	}

	/** A cultivator of {@code rank} at {@code rel}, its mind silent, kept in {@code npcs} to take away after. */
	private static CultivatorNpc npc(GameTestHelper helper, List<CultivatorNpc> npcs, BlockPos rel, int rank) {
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, rel);
		npc.setRank(rank);
		npcs.add(npc);
		return npc;
	}

	private static String key(Component line) {
		return line != null && line.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(line);
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) if (GhostPossession.isTenant(p)) GhostPossession.leave(p, null);
		for (ServerPlayer p : made) {
			SoulProjection.onDeath(p); // nothing of a journey is left behind
			CultivationManager.own(p).setPossession(null);
			GhostSystem.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
