package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sects founded by players (the user's "Build as designed", 2026-11-10; PlayerSects): who may found one and where, its places and aspirants
 * titled under its founder, its tribute, its plaques' gates, invitations, casting out, its founder never cast out nor walking out, disbanding,
 * and a fallen sect's core claimed and its sect revived. Their own batch; any sect left standing near the test ground by other tests is set
 * aside while they run (its land would refuse the founding).
 */
public class PlayerSectGameTests implements FabricGameTest {
	private static final BlockPos CORE = new BlockPos(4, 2, 4);

	@GameTest(template = EMPTY_STRUCTURE, batch = "player_sects", timeoutTicks = 200)
	public void onlyTheStrongFoundASectWithMatsBehindTheirBarrier(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		List<Sect> aside = setAside(level, helper.absolutePos(CORE));
		FormationCoreBlockEntity core = ground(helper);
		Sect sect = null;
		try {
			ServerPlayer p = survivor(helper, made, "sect-founder", Realm.FOUNDATION_BUILDING);
			helper.assertTrue(core.canControl(p), "The first hand claims the core");
			helper.assertTrue(key(PlayerSects.refusal(p, core)).equals(ModLang.MSG_PSECT_WEAK), "Foundation Building: too weak to found a sect");
			TestMethods.setAt(CultivationManager.own(p), Realm.NASCENT_SOUL, Stage.EARLY);
			helper.assertTrue(key(PlayerSects.refusal(p, core)).equals(ModLang.MSG_PSECT_BARRIER), "Nascent Soul, but the barrier down");
			core.configure(p, Formations.MIN_RADIUS, true);
			helper.assertTrue(core.isRaised(), "The barrier raised");
			helper.assertTrue(key(PlayerSects.refusal(p, core)).equals(ModLang.MSG_PSECT_MATS), "No mats inside: no places");
			BlockPos[] mats = {CORE.offset(2, 0, 2), CORE.offset(-2, 0, 2), CORE.offset(2, 0, -2)};
			for (BlockPos mat : mats) helper.setBlock(mat, ModBlocks.MEDITATION_MAT);
			// A wall to the west with a plaque on it, facing east: its gate before it, a guard either side.
			helper.setBlock(CORE.offset(-3, 0, -1), Blocks.STONE_BRICKS);
			helper.setBlock(CORE.offset(-3, 1, -1), Blocks.STONE_BRICKS);
			helper.setBlock(CORE.offset(-2, 1, -1), ModBlocks.SECT_PLAQUE.defaultBlockState().setValue(SectPlaqueBlock.FACING, Direction.EAST));
			helper.assertTrue(PlayerSects.refusal(p, core) == null, "Three mats inside a raised barrier: it may be founded");

			sect = PlayerSects.found(p, core, "  Testing §cPeak   Sect ");
			helper.assertTrue(sect != null && sect.isPlayers() && p.getUUID().equals(sect.founder) && "Testing Peak Sect".equals(sect.name),
					"Founded, its name cleaned of formatting and spaces: " + (sect == null ? null : sect.name));
			helper.assertTrue(core.isPlayersSect() && sect.id.equals(core.getSect()) && p.getUUID().equals(core.getOwner()),
					"The core is the sect's and still its founder's");
			helper.assertTrue(SectMembership.sectOf(server, p.getUUID()) == sect && SectMembership.titleOf(sect, p) == NpcTitle.SECT_MASTER,
					"Its founder is its Sect Master");
			helper.assertTrue(sect.capacity == 3 && sect.posts(Sect.Post.DWELLING).size() == 3, "Three mats, three places: " + sect.capacity);
			helper.assertTrue(sect.posts(Sect.Post.GATE).size() == 1 && sect.posts(Sect.Post.GATE).get(0).equals(helper.absolutePos(CORE.offset(-1, 0, -1)))
					&& sect.posts(Sect.Post.GATE_GUARD).size() == 2, "The plaque marks a gate before it, a guard either side: " + sect.posts(Sect.Post.GATE));
			helper.assertTrue(key(PlayerSects.refusal(p, core)).equals(ModLang.MSG_PSECT_IN_SECT), "One sect is all one founds");

			for (int i = 0; i < 4; i++) PlayerSects.arrive(level, sect);
			helper.assertTrue(sect.members.size() == 3, "Aspirants come while there are places, no more: " + sect.members.size());
			int founderRank = PlayerSects.founderRank(CultivationManager.own(p));
			helper.assertTrue(sect.members.values().stream().allMatch(m -> m.title != NpcTitle.SECT_MASTER && m.rank < founderRank),
					"None of them the master, none as strong as the founder");
			Sect.Member strongest = sect.members.values().stream().sorted(Sect.SENIORITY).findFirst().orElseThrow();
			helper.assertTrue(strongest.title == NpcTitle.ELDER, "Under the founder, the strongest of three an Elder: " + strongest.title);
			UUID sectId = sect.id;
			helper.assertTrue(sect.members.values().stream().allMatch(m -> level.getEntity(m.id) instanceof CultivatorNpc npc && sectId.equals(npc.getSectId())),
					"Each a cultivator of it in the world");

			// Two days on (the test world may be younger than two days: its days can't be set back before its first).
			long now = level.getGameTime();
			long daily = PlayerSects.dailyTribute(sect);
			sect.tributeDay = Regard.day(now);
			sect.tribute = 0;
			PlayerSects.reckonTribute(server, sect, now + 2 * HeavenRitual.DAY);
			helper.assertTrue(daily > 0 && sect.tribute == 2 * daily, "Two days' tribute waits: " + sect.tribute + " of " + daily + " a day");
			int before = stones(p);
			PlayerSects.collect(p, sect);
			helper.assertTrue(sect.tribute == 0 && stones(p) - before == 2 * daily, "Collected as spirit stones: " + (stones(p) - before));

			PlayerSects.disband(server, sect, ModLang.MSG_PSECT_DISBANDED);
			helper.assertTrue(sect.extinct && sect.members.isEmpty() && sect.disciples.isEmpty() && SectMembership.sectOf(server, p.getUUID()) == null,
					"Disbanded: its people gone their ways, its founder of no sect");
			helper.assertTrue(!core.isPlayersSect() && core.getSect() == null && p.getUUID().equals(core.getOwner()), "The core is its owner's own again");
			helper.succeed();
		} finally {
			if (sect != null && !sect.extinct) PlayerSects.disband(server, sect, ModLang.MSG_PSECT_DISBANDED);
			clear(helper, core);
			leave(made);
			restore(aside);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "player_sects", timeoutTicks = 200)
	public void playersJoinOnlyByInvitationAndTheFounderStays(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		List<Sect> aside = setAside(level, helper.absolutePos(CORE));
		FormationCoreBlockEntity core = ground(helper);
		Sect sect = null;
		try {
			ServerPlayer p = survivor(helper, made, "sect-host", Realm.HEAVENLY_BEING);
			sect = founded(helper, core, p);
			helper.assertTrue(sect != null && sect.capacity == 3, "Founded (Heavenly Being: up to 12 places, its three mats the limit)");
			ServerPlayer q = survivor(helper, made, "sect-guest", Realm.FOUNDATION_BUILDING);
			ServerPlayer r = survivor(helper, made, "sect-asker", Realm.FOUNDATION_BUILDING);
			PlayerSects.arrive(level, sect);
			UUID elderId = sect.members.keySet().iterator().next();
			CultivatorNpc npc = (CultivatorNpc) level.getEntity(elderId);
			Object[] arg = new Object[1];
			helper.assertTrue(npc != null && "join.founder_only".equals(SectMembership.refusal(r, npc, sect, arg)) && "sect-host".equals(arg[0]),
					"Its cultivators take no one in themselves: only those its founder invites");
			helper.assertTrue(PlayerSects.accept(q) == 0, "No invitation, nothing to accept");
			PlayerSects.invite(p, sect, "sect-guest");
			helper.assertTrue(sect.invited.containsKey(q.getUUID()), "Invited");
			helper.assertTrue(PlayerSects.accept(q) == 1 && SectMembership.sectOf(server, q.getUUID()) == sect && !sect.invited.containsKey(q.getUUID()),
					"Accepted: one of it");
			helper.assertTrue(SectMembership.titleOf(sect, q) != NpcTitle.SECT_MASTER, "...under its founder");
			SectMembership.leave(p, sect);
			SectMembership.expel(p, sect, ModLang.MSG_SECT_EXPELLED_VIOLENCE);
			helper.assertTrue(SectMembership.sectOf(server, p.getUUID()) == sect, "Its founder neither walks out nor is cast out");
			SectMembership.banish(p, q.getUUID());
			helper.assertTrue(SectMembership.sectOf(server, q.getUUID()) == null, "Its founder casts a player out");
			SectMembership.banish(p, elderId);
			helper.assertTrue(!sect.members.containsKey(elderId), "...and a cultivator");
			helper.succeed();
		} finally {
			if (sect != null && !sect.extinct) PlayerSects.disband(server, sect, ModLang.MSG_PSECT_DISBANDED);
			clear(helper, core);
			leave(made);
			restore(aside);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "player_sects", timeoutTicks = 200)
	public void aFallenSectsCoreClaimedRevivesItAndItsBreakingEndsTheSect(GameTestHelper helper) throws ReflectiveOperationException {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		List<Sect> aside = setAside(level, helper.absolutePos(CORE));
		FormationCoreBlockEntity core = ground(helper);
		Sect fallen = new Sect(UUID.randomUUID());
		Sect sect = null;
		try {
			fallen.name = "Fallen Pine Sect";
			fallen.dimension = level.dimension();
			fallen.core = helper.absolutePos(CORE);
			fallen.extinct = true;
			SectData.get(server).put(fallen);
			Field field = FormationCoreBlockEntity.class.getDeclaredField("fallenSect");
			field.setAccessible(true);
			field.set(core, fallen.id);
			ServerPlayer p = survivor(helper, made, "sect-reviver", Realm.NASCENT_SOUL);
			sect = founded(helper, core, p, "");
			helper.assertTrue(sect == fallen && !sect.extinct && "Fallen Pine Sect".equals(sect.name) && sect.isPlayers(),
					"The fallen sect lives again under its claimer, its name kept");
			BlockPos at = sect.core;
			level.removeBlock(at, false); // its core broken
			core.onRemovedFromWorld(level);
			helper.assertTrue(sect.extinct && SectMembership.sectOf(server, p.getUUID()) == null, "Its core broken, the sect breaks up");
			helper.succeed();
		} finally {
			if (sect != null && !sect.extinct) PlayerSects.disband(server, sect, ModLang.MSG_PSECT_DISBANDED);
			fallen.extinct = true;
			clear(helper, core);
			leave(made);
			restore(aside);
		}
	}

	/**
	 * The bug sweep of 2026-11-10: a sect fallen with players left on its rolls holds none of them (nobody can walk out of one: leaving is asked of
	 * its elders), so they may still found a sect; one revived starts afresh (its old disciples aren't taken in, its grudge against its new
	 * master, often its destroyer, is gone); an invitation stands one at a time; and a Thousand-Li Escape lands no further past a barrier than its
	 * burner could walk.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "player_sects", timeoutTicks = 200)
	public void aFallenSectHoldsNobodyAndARevivalStartsAfresh(GameTestHelper helper) throws ReflectiveOperationException {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		List<Sect> aside = setAside(level, helper.absolutePos(CORE));
		FormationCoreBlockEntity core = ground(helper);
		Sect fallen = new Sect(UUID.randomUUID()), older = new Sect(UUID.randomUUID());
		Sect sect = null;
		try {
			ServerPlayer p = survivor(helper, made, "sect-heir", Realm.NASCENT_SOUL);
			ServerPlayer old = survivor(helper, made, "sect-remnant", Realm.FOUNDATION_BUILDING);
			// Two fallen sects: this core's (its last disciple and its grudge against p still on its rolls), and another p was left on.
			for (Sect s : new Sect[] {fallen, older}) {
				s.dimension = level.dimension();
				s.extinct = true;
				SectData.get(server).put(s);
			}
			fallen.name = "Fallen Cedar Sect";
			fallen.core = helper.absolutePos(CORE);
			fallen.disciples.put(old.getUUID(), new Sect.Disciple(old.getUUID(), "sect-remnant", 0));
			fallen.changeStanding(p.getUUID(), -50);
			fallen.barredUntil.put(p.getUUID(), Long.MAX_VALUE);
			older.name = "Fallen Willow Sect";
			older.core = helper.absolutePos(CORE).offset(1000, 0, 1000);
			older.disciples.put(p.getUUID(), new Sect.Disciple(p.getUUID(), "sect-heir", 0));
			Field field = FormationCoreBlockEntity.class.getDeclaredField("fallenSect");
			field.setAccessible(true);
			field.set(core, fallen.id);

			helper.assertTrue(SectMembership.sectOf(server, p.getUUID()) == older && SectMembership.liveSectOf(server, p.getUUID()) == null,
					"Left on a fallen sect's rolls, one is of no standing sect");
			core.canControl(p);
			core.configure(p, Formations.MIN_RADIUS, true);
			for (BlockPos mat : new BlockPos[] {CORE.offset(2, 0, 2), CORE.offset(-2, 0, 2), CORE.offset(2, 0, -2)}) helper.setBlock(mat, ModBlocks.MEDITATION_MAT);
			helper.assertTrue(PlayerSects.refusal(p, core) == null, "...and may found a sect: " + key(PlayerSects.refusal(p, core)));
			sect = PlayerSects.found(p, core, "");
			helper.assertTrue(sect == fallen && !sect.extinct && "Fallen Cedar Sect".equals(sect.name), "The fallen sect lives again");
			helper.assertTrue(sect.disciples.size() == 1 && sect.disciples.containsKey(p.getUUID()) && SectMembership.sectOf(server, old.getUUID()) == null,
					"Its old disciple isn't taken in unasked: " + sect.disciples.keySet());
			helper.assertTrue(sect.standing(p.getUUID()) == 0 && sect.barredUntil.isEmpty(), "Its grudge against its new master is gone: " + sect.standing(p.getUUID()));
			helper.assertTrue(!older.disciples.containsKey(p.getUUID()) && SectMembership.sectOf(server, p.getUUID()) == sect,
					"Off the other fallen sect's rolls: its own sect is the one found for its founder");

			// An invitation stands one at a time: asked again while it stands, it isn't sent again.
			PlayerSects.invite(p, sect, "sect-remnant");
			Long first = sect.invited.get(old.getUUID());
			helper.assertTrue(first != null, "Invited");
			sect.invited.put(old.getUUID(), first - 100);
			PlayerSects.invite(p, sect, "sect-remnant");
			helper.assertTrue(sect.invited.get(old.getUUID()) == first - 100, "Asked again while it stands: not sent again");

			// A Thousand-Li Escape from the ground's corner, outside the barrier, toward a free spot inside it.
			ServerPlayer w = survivor(helper, made, "sect-stranger", Realm.FOUNDATION_BUILDING);
			net.minecraft.world.phys.Vec3 corner = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(0, 2, 0)));
			net.minecraft.world.phys.Vec3 toCore = new net.minecraft.world.phys.Vec3(1, 0, 1).normalize();
			BlockPos inside = helper.absolutePos(new BlockPos(5, 2, 5));
			helper.assertTrue(inside.equals(Talismans.landing(level, corner, toCore, 7)), "Asked for nobody, it lands inside: " + Talismans.landing(level, corner, toCore, 7));
			helper.assertTrue(Talismans.landing(level, corner, toCore, 7, w) == null, "A weak stranger's lands nowhere past the barrier");
			helper.assertTrue(inside.equals(Talismans.landing(level, corner, toCore, 7, p)), "Its founder's lands inside it");
			helper.succeed();
		} finally {
			if (sect != null && !sect.extinct) PlayerSects.disband(server, sect, ModLang.MSG_PSECT_DISBANDED);
			fallen.extinct = true;
			older.extinct = true;
			clear(helper, core);
			leave(made);
			restore(aside);
		}
	}

	// --- The ground ---

	/** A floor, the core with four veins about it, ready to be claimed. */
	private static FormationCoreBlockEntity ground(GameTestHelper helper) {
		for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) helper.setBlock(CORE.offset(dx, -1, dz), Blocks.STONE);
		helper.setBlock(CORE, ModBlocks.FORMATION_CORE);
		for (Direction side : Direction.Plane.HORIZONTAL) helper.setBlock(CORE.relative(side), ModBlocks.QI_VEIN);
		FormationCoreBlockEntity core = (FormationCoreBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CORE));
		core.scanNetwork(helper.getLevel());
		return core;
	}

	private static Sect founded(GameTestHelper helper, FormationCoreBlockEntity core, ServerPlayer p) {
		return founded(helper, core, p, "Host Sect");
	}

	/** {@code p} claims the core, raises its barrier, lays three mats and founds a sect called {@code name}. */
	private static Sect founded(GameTestHelper helper, FormationCoreBlockEntity core, ServerPlayer p, String name) {
		core.canControl(p);
		core.configure(p, Formations.MIN_RADIUS, true);
		for (BlockPos mat : new BlockPos[] {CORE.offset(2, 0, 2), CORE.offset(-2, 0, 2), CORE.offset(2, 0, -2)}) helper.setBlock(mat, ModBlocks.MEDITATION_MAT);
		return PlayerSects.found(p, core, name);
	}

	private static void clear(GameTestHelper helper, FormationCoreBlockEntity core) {
		core.onRemovedFromWorld(helper.getLevel());
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = -1; dy <= 2; dy++) helper.setBlock(CORE.offset(dx, dy, dz), Blocks.AIR);
			}
		}
		for (Entity e : helper.getLevel().getEntitiesOfClass(CultivatorNpc.class, new net.minecraft.world.phys.AABB(helper.absolutePos(CORE)).inflate(48))) {
			if (e instanceof CultivatorNpc npc && npc.getSectId() == null) npc.discard(); // the aspirants, set off as rogues
		}
	}

	/** Every sect standing near {@code at} (another test's, or the world's own), set aside (as fallen) while these run: restored after. */
	private static List<Sect> setAside(ServerLevel level, BlockPos at) {
		List<Sect> aside = new ArrayList<>();
		for (Sect other : SectData.get(level.getServer()).all()) {
			if (other.extinct || other.dimension != level.dimension()) continue;
			if (other.core.distSqr(at) < 400.0 * 400.0) {
				other.extinct = true;
				aside.add(other);
			}
		}
		return aside;
	}

	private static void restore(List<Sect> aside) {
		for (Sect other : aside) other.extinct = false;
	}

	private static String key(Component line) {
		return line != null && line.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(line);
	}

	/** Low stones' worth of spirit stones {@code p} carries. */
	private static int stones(ServerPlayer p) {
		int sum = 0;
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) sum += SpiritStoneItem.valueOf(p.getInventory().getItem(i));
		return sum;
	}

	/** A survival player in the player list at {@code realm} Early (the helper's mock player is creative), remembered in {@code made}. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(CORE.offset(1, 0, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		CultivationManager.own(p).awaken(0);
		TestMethods.setAt(CultivationManager.own(p), realm, Stage.EARLY);
		made.add(p);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) p.server.getPlayerList().remove(p);
	}
}
