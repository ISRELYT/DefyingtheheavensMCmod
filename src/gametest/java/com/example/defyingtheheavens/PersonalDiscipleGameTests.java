package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A personal disciple (the user's "Build as designed", 2026-11-13; PersonalDisciples): taken through talk, weaker than its master and admiring
 * them; cultivating the faster beside them, their method and their war arts; asking for its pill at a gate; leaving a master who does devilish
 * things; mourned a week when it dies.
 */
public class PersonalDiscipleGameTests implements FabricGameTest {
	private static String key(Component message) {
		return message != null && message.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aWandererWhoAdmiresYouBecomesYourDisciple(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "disciple-master", Realm.CORE_FORMATION);
			CultivatorNpc wanderer = cultivator(helper, spawned, new BlockPos(3, 2, 3), Realm.FOUNDATION_BUILDING);
			CultivatorNpc strong = cultivator(helper, spawned, new BlockPos(5, 2, 3), Realm.NASCENT_SOUL);
			helper.assertTrue(key(PersonalDisciples.refusal(p, strong)).equals(ModLang.DISCIPLE_TOO_STRONG), "One no weaker than you won't be your disciple");
			if (wanderer.regard(p) >= PersonalDisciples.ADMIRE) wanderer.changeRegard(p, -200);
			helper.assertTrue(key(PersonalDisciples.refusal(p, wanderer)).equals(ModLang.DISCIPLE_NOT_ADMIRED), "A wanderer that doesn't admire you won't follow you");
			wanderer.changeRegard(p, 200);
			helper.assertTrue(PersonalDisciples.refusal(p, wanderer) == null, "...one that does will");
			PersonalDisciples.take(p, wanderer);
			helper.assertTrue(PersonalDisciples.isDiscipleOf(wanderer, p) && wanderer.getUUID().equals(CultivationManager.own(p).personalDisciple())
					&& wanderer.discipleOrder() == PersonalDisciples.Order.FOLLOW, "Taken: its master's, following them");
			CultivatorNpc another = cultivator(helper, spawned, new BlockPos(3, 2, 6), Realm.FOUNDATION_BUILDING);
			another.changeRegard(p, 200);
			helper.assertTrue(!PersonalDisciples.offered(p, another), "One at a time: no other is asked while it is yours");
			helper.assertTrue(wanderer.isAlly(p), "It stands with its master");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDiscipleCultivatesFasterBesideItsMasterTheirMethodAndArts(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "disciple-teacher", Realm.NASCENT_SOUL);
			CultivatorNpc npc = cultivator(helper, spawned, new BlockPos(3, 2, 3), Realm.CORE_FORMATION);
			npc.changeRegard(p, 200);
			PersonalDisciples.take(p, npc);
			helper.assertTrue(PersonalDisciples.cultivationFactor(npc) == PersonalDisciples.FASTER, "Beside its master, half again as fast");
			p.teleportTo(p.getX() + 60, p.getY(), p.getZ());
			helper.assertTrue(PersonalDisciples.cultivationFactor(npc) == 1, "Far from them, as ever");
			p.teleportTo(p.getX() - 60, p.getY(), p.getZ());
			PlayerCultivation own = CultivationManager.own(p);
			HeavenMethod method = HeavenMethod.of(QiElement.WATER, Realm.CORE_FORMATION);
			own.heaven().setMethod(method, 1, 0);
			own.learnTechnique(Technique.SWORD_INTENT);
			List<HeavenMethod> methods = PersonalDisciples.teachable(p, npc);
			int index = methods.indexOf(method);
			helper.assertTrue(index >= 0, "Its master's method is offered it");
			PersonalDisciples.act(p, npc, PersonalDisciples.A_METHOD + index);
			helper.assertTrue(npc.heavenMethod() == method, "...and set: " + npc.heavenMethod());
			PersonalDisciples.act(p, npc, PersonalDisciples.A_TEACH + Technique.SWORD_INTENT.ordinal());
			helper.assertTrue(npc.cultivation().techniques().contains(Technique.SWORD_INTENT), "Sword Intent taught it");
			PersonalDisciples.act(p, npc, PersonalDisciples.A_STAY);
			helper.assertTrue(npc.discipleOrder() == PersonalDisciples.Order.STAY && npc.discipleSpot() != null, "Told to stay, it stays where it is");
			PersonalDisciples.act(p, npc, PersonalDisciples.A_RELEASE);
			helper.assertTrue(npc.master() == null && own.personalDisciple() == null && own.discipleMournUntil() == 0, "Let go: its own again, no wait for another");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDiscipleAtAGateAsksItsMasterForThePill(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "disciple-pill-giver", Realm.CORE_FORMATION);
			CultivatorNpc npc = cultivator(helper, spawned, new BlockPos(3, 2, 3), Realm.QI_REFINING);
			npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION));
			npc.grantHeavensLeave();
			npc.changeRegard(p, 200);
			PersonalDisciples.take(p, npc);
			int gate = npc.getRank();
			npc.advance(npc.cultivation().cultivationRequired() * 3);
			helper.assertTrue(npc.getRank() == gate && npc.pillGate() == Realm.FOUNDATION_BUILDING.ordinal(), "At Foundation Building's gate it waits, asking for its pill");
			helper.assertTrue(!PersonalDisciples.givePill(p, npc, new ItemStack(ModItems.CORE_PILL)), "Not just any pill");
			helper.assertTrue(PersonalDisciples.givePill(p, npc, new ItemStack(ModItems.FOUNDATION_PILL))
					&& npc.getRank() == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), "Given the Foundation Pill, it breaks through at once");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aRighteousDiscipleLeavesAMasterWhoDoesDevilishThings(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "disciple-devil", Realm.CORE_FORMATION);
			CultivatorNpc npc = cultivator(helper, spawned, new BlockPos(3, 2, 3), Realm.FOUNDATION_BUILDING);
			npc.setAlignment(Alignment.MAX);
			npc.changeRegard(p, 200);
			PersonalDisciples.take(p, npc);
			PersonalDisciples.witnessed(p, -10);
			helper.assertTrue(PersonalDisciples.isDiscipleOf(npc, p), "Troubled by one deed, it stays");
			for (int i = 0; i < 20 && PersonalDisciples.isDiscipleOf(npc, p); i++) PersonalDisciples.witnessed(p, -10);
			helper.assertTrue(!PersonalDisciples.isDiscipleOf(npc, p) && CultivationManager.own(p).personalDisciple() == null, "Seeing more, it leaves its master");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDiscipleThatDiesIsMournedAWeek(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "disciple-mourner", Realm.CORE_FORMATION);
			CultivatorNpc npc = cultivator(helper, spawned, new BlockPos(3, 2, 3), Realm.FOUNDATION_BUILDING);
			npc.changeRegard(p, 200);
			PersonalDisciples.take(p, npc);
			npc.kill();
			PlayerCultivation own = CultivationManager.own(p);
			helper.assertTrue(own.personalDisciple() == null && own.discipleMournUntil() >= helper.getLevel().getGameTime() + PersonalDisciples.MOURN_TICKS - 20,
					"Its death mourned: no disciple, a week's wait");
			CultivatorNpc next = cultivator(helper, spawned, new BlockPos(5, 2, 5), Realm.FOUNDATION_BUILDING);
			next.changeRegard(p, 200);
			helper.assertTrue(key(PersonalDisciples.refusal(p, next)).equals(ModLang.DISCIPLE_MOURNING), "No other taken while it is mourned");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	// --- Helpers ---

	private static CultivatorNpc cultivator(GameTestHelper helper, List<Entity> spawned, BlockPos at, Realm realm) {
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, at);
		npc.setRank(PlayerCultivation.rank(realm, Stage.EARLY));
		spawned.add(npc);
		return npc;
	}

	/** A survival player in the player list (the helper's mock player is creative), at {@code realm} Early, remembered in {@code made}. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm) {
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
		CultivationManager.own(p).awaken(0);
		CultivationManager.get(p).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		made.add(p);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			QiFlight.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
