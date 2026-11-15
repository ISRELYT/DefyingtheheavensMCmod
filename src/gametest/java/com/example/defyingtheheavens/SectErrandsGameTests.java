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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sending one's disciples out (the user's "Build as designed", 2026-11-13; SectErrands): a player's sect's founder sends its people on
 * errands, a third at a time, never one already out, resting, or someone's own disciple; an errand comes out by their strength against the
 * task's, its spoils waiting at the core to be collected with the tribute, the wounded resting, the lost struck from the roll; and out of
 * sight they go on unseen, to be met on the road. With the player sects' tests (any sect near the ground set aside while they run); the
 * road's, which waits a little, in a batch of its own.
 */
public class SectErrandsGameTests implements FabricGameTest {
	private static final BlockPos CORE = new BlockPos(4, 2, 4);

	@GameTest(template = EMPTY_STRUCTURE)
	public void anErrandComesOutByStrengthAgainstTheTask(GameTestHelper helper) {
		int qr = PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), cf = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID);
		helper.assertTrue(SectErrands.outcome(SectErrands.Task.HUNT, qr, cf, 0.2, 0.9) == SectErrands.Outcome.LOST,
				"A Qi Refining disciple hunting a Core Formation beast: lost one time in five or more");
		helper.assertTrue(SectErrands.outcome(SectErrands.Task.HUNT, cf, cf, 0.1, 0.9) == SectErrands.Outcome.WOUNDED
				&& SectErrands.outcome(SectErrands.Task.HUNT, cf, cf, 0.3, 0.9) == SectErrands.Outcome.DONE
				&& SectErrands.outcome(SectErrands.Task.HUNT, cf, cf, 0.3, 0.01) == SectErrands.Outcome.BREAKTHROUGH,
				"Its match: wounded now and then, done mostly, a breakthrough at times");
		helper.assertTrue(SectErrands.outcome(SectErrands.Task.LETTER, qr, 0, 0.01, 0.9) != SectErrands.Outcome.LOST, "A letter is hardly ever lost");
		helper.assertTrue(SectErrands.above(qr, cf) && !SectErrands.above(cf, cf), "Warned of a task above them, not one at their strength");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "player_sects", timeoutTicks = 200)
	public void theFounderSendsAThirdOfTheSectOutAtOnce(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		List<Sect> aside = setAside(level, helper.absolutePos(CORE));
		FormationCoreBlockEntity core = ground(helper);
		Sect sect = null;
		try {
			ServerPlayer p = survivor(helper, made, "errand-founder", Realm.NASCENT_SOUL);
			ServerPlayer other = survivor(helper, made, "errand-guest", Realm.NASCENT_SOUL);
			sect = founded(helper, core, p);
			helper.assertTrue(sect != null, "Founded");
			for (int i = 0; i < 4; i++) PlayerSects.arrive(level, sect);
			helper.assertTrue(sect.members.size() == 3 && SectErrands.mostOut(sect) == 1, "Three people, one of them out at a time: " + sect.members.size());
			List<Sect.Member> roll = SectErrands.roll(sect);
			Sect.Member first = roll.get(0), second = roll.get(1), third = roll.get(2);
			helper.assertTrue(key(SectErrands.send(other, sect, 0, SectErrands.check(first.id), SectErrands.Task.GATHER, 0)).equals(ModLang.ERRAND_NOT_FOUNDER),
					"Only its founder sends its people out");
			helper.assertTrue(key(SectErrands.send(p, sect, 0, SectErrands.check(second.id), SectErrands.Task.GATHER, 0)).equals(ModLang.ERRAND_NO_ONE),
					"A row whose mark doesn't match sends nobody");
			if (SectErrands.openRealm(server, sect) == null) {
				helper.assertTrue(key(SectErrands.send(p, sect, 0, SectErrands.check(first.id), SectErrands.Task.REALM, 0)).equals(ModLang.ERRAND_NO_REALM),
						"No secret realm open: none sent into one");
			}
			Component sent = SectErrands.send(p, sect, 0, SectErrands.check(first.id), SectErrands.Task.HUNT, Realm.CORE_FORMATION.ordinal());
			helper.assertTrue(key(sent).equals(ModLang.ERRAND_SENT), "Sent on a hunt: " + sent.getString());
			SectErrands.Errand e = sect.errands.get(first.id);
			long now = level.getGameTime();
			helper.assertTrue(e != null && e.task == SectErrands.Task.HUNT && e.taskRank == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID)
					&& e.due == now + 2 * 24_000L && Math.sqrt(e.to.distSqr(sect.core)) >= SectErrands.HUNT_FROM - 1, "A hunt of Core Formation, two days out, far off");
			helper.assertTrue(SectErrands.state(server, sect, first.id, now) == SectErrands.State.OUT && SectErrands.out(sect) == 1, "Out");
			helper.assertTrue(key(SectErrands.send(p, sect, 0, SectErrands.check(first.id), SectErrands.Task.GATHER, 0)).equals(ModLang.ERRAND_AWAY),
					"One out isn't sent again");
			helper.assertTrue(key(SectErrands.send(p, sect, 1, SectErrands.check(second.id), SectErrands.Task.GATHER, 0)).equals(ModLang.ERRAND_TOO_MANY),
					"A third of three is one: no more go");
			if (level.getEntity(first.id) instanceof CultivatorNpc out) {
				helper.assertTrue(!out.isLeashed(), "Out on an errand, it is held to no grounds (it fights back on the road)");
			}
			// The third one, someone's own disciple, keeps to their side.
			sect.errands.clear();
			CultivatorNpc own = level.getEntity(third.id) instanceof CultivatorNpc npc ? npc : null;
			helper.assertTrue(own != null, "The third in the world");
			own.changeRegard(p, 200);
			PersonalDisciples.take(p, own);
			helper.assertTrue(SectErrands.state(server, sect, third.id, now) == SectErrands.State.PERSONAL
					&& key(SectErrands.send(p, sect, 2, SectErrands.check(third.id), SectErrands.Task.GATHER, 0)).equals(ModLang.ERRAND_PERSONAL),
					"A personal disciple isn't sent out");
			PersonalDisciples.act(p, own, PersonalDisciples.A_RELEASE);
			helper.succeed();
		} finally {
			if (sect != null && !sect.extinct) PlayerSects.disband(server, sect, ModLang.MSG_PSECT_DISBANDED);
			clear(helper, core);
			leave(made);
			restore(aside);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "player_sects", timeoutTicks = 200)
	public void spoilsWaitAtTheCoreTheWoundedRestAndTheLostAreMourned(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		List<Sect> aside = setAside(level, helper.absolutePos(CORE));
		FormationCoreBlockEntity core = ground(helper);
		Sect sect = null;
		try {
			ServerPlayer p = survivor(helper, made, "errand-collector", Realm.NASCENT_SOUL);
			sect = founded(helper, core, p);
			for (int i = 0; i < 3; i++) PlayerSects.arrive(level, sect);
			List<Sect.Member> roll = SectErrands.roll(sect);
			helper.assertTrue(roll.size() == 3, "Three people");
			long now = level.getGameTime();

			// Done: herbs brought back, waiting at the core till its founder collects them with the tribute.
			Sect.Member gatherer = roll.get(0);
			helper.assertTrue(key(SectErrands.send(p, sect, 0, SectErrands.check(gatherer.id), SectErrands.Task.GATHER, 0)).equals(ModLang.ERRAND_SENT), "Sent for herbs");
			SectErrands.Errand e = sect.errands.get(gatherer.id);
			int rank = level.getEntity(gatherer.id) instanceof CultivatorNpc g ? g.getRank() : gatherer.rank;
			SectErrands.comeBack(server, sect, gatherer.id, e, seeded(SectErrands.Task.GATHER, rank, e.taskRank, SectErrands.Outcome.DONE));
			helper.assertTrue(e.outcome == SectErrands.Outcome.DONE && !sect.spoils.isEmpty()
					&& sect.spoils.stream().allMatch(s -> s.getItem() instanceof GinsengItem), "Back with herbs, at the core: " + sect.spoils);
			helper.assertTrue(SectErrands.state(server, sect, gatherer.id, now) == SectErrands.State.RETURNING && SectErrands.out(sect) == 0,
					"Its time up, it is on the way back, and no longer counted out");
			int herbs = sect.spoils.size();
			int before = herbsCarried(p);
			PlayerSects.collect(p, sect);
			helper.assertTrue(sect.spoils.isEmpty() && herbsCarried(p) - before >= herbs, "Collected at the core with the tribute");
			sect.errands.clear();

			// Wounded: half the spoils, and rest before it goes again.
			Sect.Member hunter = roll.get(1);
			helper.assertTrue(key(SectErrands.send(p, sect, 1, SectErrands.check(hunter.id), SectErrands.Task.HUNT, Realm.QI_REFINING.ordinal())).equals(ModLang.ERRAND_SENT),
					"Sent on a hunt");
			e = sect.errands.get(hunter.id);
			rank = level.getEntity(hunter.id) instanceof CultivatorNpc h ? h.getRank() : hunter.rank;
			SectErrands.comeBack(server, sect, hunter.id, e, seeded(SectErrands.Task.HUNT, rank, e.taskRank, SectErrands.Outcome.WOUNDED));
			helper.assertTrue(e.outcome == SectErrands.Outcome.WOUNDED && sect.resting.containsKey(hunter.id), "Back wounded, resting");
			e.settled = true;
			helper.assertTrue(SectErrands.state(server, sect, hunter.id, now) == SectErrands.State.RESTING
					&& key(SectErrands.send(p, sect, 1, SectErrands.check(hunter.id), SectErrands.Task.GATHER, 0)).equals(ModLang.ERRAND_RESTING),
					"Resting, it isn't sent again yet");
			sect.errands.clear();
			sect.spoils.clear();

			// Lost on the road, unseen: struck from the roll, its body gone with it.
			Sect.Member lost = roll.get(2);
			helper.assertTrue(key(SectErrands.send(p, sect, 2, SectErrands.check(lost.id), SectErrands.Task.HUNT, Realm.CORE_FORMATION.ordinal())).equals(ModLang.ERRAND_SENT),
					"Sent on a hunt above it");
			e = sect.errands.get(lost.id);
			CultivatorNpc walker = level.getEntity(lost.id) instanceof CultivatorNpc l ? l : null;
			helper.assertTrue(walker != null, "In the world");
			rank = walker.getRank();
			SectErrands.takeOut(walker, e, now);
			helper.assertTrue(walker.isRemoved() && e.body != null && level.getEntity(lost.id) == null && sect.members.containsKey(lost.id),
					"Out of sight: out of the world, its body with the errand, still of the sect");
			SectErrands.comeBack(server, sect, lost.id, e, seeded(SectErrands.Task.HUNT, rank, e.taskRank, SectErrands.Outcome.LOST));
			helper.assertTrue(e.outcome == SectErrands.Outcome.LOST && e.settled && e.body == null && !sect.members.containsKey(lost.id) && sect.spoils.isEmpty(),
					"Lost: off the roll, its body gone, nothing brought back");
			helper.succeed();
		} finally {
			if (sect != null && !sect.extinct) PlayerSects.disband(server, sect, ModLang.MSG_PSECT_DISBANDED);
			clear(helper, core);
			leave(made);
			restore(aside);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "player_sects", timeoutTicks = 200)
	public void oneWhoseLandUnloadsUnderItGoesOnUnseenAndIsNotSavedTwice(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		List<Sect> aside = setAside(level, helper.absolutePos(CORE));
		FormationCoreBlockEntity core = ground(helper);
		Sect sect = null;
		try {
			ServerPlayer p = survivor(helper, made, "errand-teleporter", Realm.NASCENT_SOUL);
			sect = founded(helper, core, p);
			PlayerSects.arrive(level, sect);
			Sect.Member member = SectErrands.roll(sect).get(0);
			CultivatorNpc npc = level.getEntity(member.id) instanceof CultivatorNpc found ? found : null;
			helper.assertTrue(npc != null && npc.shouldBeSaved(), "In the world, saved with its land as any");
			helper.assertTrue(!SectErrands.unloaded(npc), "At home, its land unloading leaves it standing there, as ever");
			helper.assertTrue(key(SectErrands.send(p, sect, 0, SectErrands.check(member.id), SectErrands.Task.GATHER, 0)).equals(ModLang.ERRAND_SENT), "Sent for herbs");
			SectErrands.Errand e = sect.errands.get(member.id);
			helper.assertTrue(SectErrands.unloaded(npc) && e.body != null && !npc.shouldBeSaved() && e.legFrom.equals(npc.blockPosition()),
					"Out, its land unloading at once under it (its founder teleported off): it goes on unseen from there, and isn't saved with the land");
			npc.travelling = true;
			npc.discard();
			helper.assertTrue(sect.members.containsKey(member.id), "Still of its sect");
			helper.succeed();
		} finally {
			if (sect != null && !sect.extinct) PlayerSects.disband(server, sect, ModLang.MSG_PSECT_DISBANDED);
			if (sect != null) sect.errands.clear();
			clear(helper, core);
			leave(made);
			restore(aside);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "sect_errands_road", timeoutTicks = 200)
	public void outOfSightTheyGoOnUnseenAndAreMetOnTheRoad(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		List<Sect> aside = List.of();
		FormationCoreBlockEntity core = ground(helper);
		Sect[] held = new Sect[1];
		boolean waiting = false;
		try {
			ServerPlayer p = survivor(helper, made, "errand-wayfarer", Realm.NASCENT_SOUL);
			// Any sect near set aside only while it is founded (their people, ticking, would leave a sect fallen for long).
			List<Sect> near = setAside(level, helper.absolutePos(CORE));
			Sect sect = held[0] = founded(helper, core, p);
			restore(near);
			helper.assertTrue(sect != null, "Founded");
			PlayerSects.arrive(level, sect);
			Sect.Member member = SectErrands.roll(sect).get(0);
			helper.assertTrue(key(SectErrands.send(p, sect, 0, SectErrands.check(member.id), SectErrands.Task.HUNT, Realm.QI_REFINING.ordinal())).equals(ModLang.ERRAND_SENT),
					"Sent on a hunt");
			SectErrands.Errand e = sect.errands.get(member.id);
			CultivatorNpc npc = level.getEntity(member.id) instanceof CultivatorNpc found ? found : null;
			helper.assertTrue(npc != null, "In the world");
			long now = level.getGameTime();
			SectErrands.takeOut(npc, e, now);
			helper.assertTrue(npc.isRemoved() && e.body != null, "Taken out of the world");
			Vec3 from = Vec3.atBottomCenterOf(e.legFrom), later = e.at(sect, now + 100);
			helper.assertTrue(Math.abs(later.distanceTo(from) - 100 * NpcTravel.SPEED) < 1 && later.distanceTo(Vec3.atBottomCenterOf(e.to)) < from.distanceTo(Vec3.atBottomCenterOf(e.to)),
					"Unseen, it goes on toward its hunt at a flier's pace");
			waiting = true;
			helper.runAfterDelay(40, () -> {
				try {
					helper.assertTrue(level.getEntity(member.id) instanceof CultivatorNpc back && back.isAlive() && e.body == null && !e.settled,
							"Its founder near where it has got to: it is back in the world, on its way");
					helper.succeed();
				} finally {
					cleanUp(helper, server, held[0], core, made, aside);
				}
			});
		} finally {
			if (!waiting) cleanUp(helper, server, held[0], core, made, aside);
		}
	}

	// --- Helpers ---

	/** A random whose first two doubles give {@code wanted} for {@code task} at {@code taskRank} against one of {@code rank} (SectErrands#comeBack draws them first). */
	private static RandomSource seeded(SectErrands.Task task, int rank, int taskRank, SectErrands.Outcome wanted) {
		for (long seed = 1; seed < 100_000; seed++) {
			RandomSource probe = RandomSource.create(seed);
			if (SectErrands.outcome(task, rank, taskRank, probe.nextDouble(), probe.nextDouble()) == wanted) return RandomSource.create(seed);
		}
		throw new IllegalStateException("No seed gives " + wanted);
	}

	private static int herbsCarried(ServerPlayer p) {
		int n = 0;
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			ItemStack stack = p.getInventory().getItem(i);
			if (stack.getItem() instanceof GinsengItem) n += stack.getCount();
		}
		return n;
	}

	private static void cleanUp(GameTestHelper helper, MinecraftServer server, Sect sect, FormationCoreBlockEntity core, List<ServerPlayer> made, List<Sect> aside) {
		if (sect != null && !sect.extinct) PlayerSects.disband(server, sect, ModLang.MSG_PSECT_DISBANDED);
		if (sect != null) sect.errands.clear();
		clear(helper, core);
		leave(made);
		restore(aside);
	}

	/** A floor, the core with four veins about it, ready to be claimed. */
	private static FormationCoreBlockEntity ground(GameTestHelper helper) {
		for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) helper.setBlock(CORE.offset(dx, -1, dz), Blocks.STONE);
		helper.setBlock(CORE, ModBlocks.FORMATION_CORE);
		for (Direction side : Direction.Plane.HORIZONTAL) helper.setBlock(CORE.relative(side), ModBlocks.QI_VEIN);
		FormationCoreBlockEntity core = (FormationCoreBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CORE));
		core.scanNetwork(helper.getLevel());
		return core;
	}

	/** {@code p} claims the core, raises its barrier, lays three mats and founds a sect. */
	private static Sect founded(GameTestHelper helper, FormationCoreBlockEntity core, ServerPlayer p) {
		core.canControl(p);
		core.configure(p, Formations.MIN_RADIUS, true);
		for (BlockPos mat : new BlockPos[] {CORE.offset(2, 0, 2), CORE.offset(-2, 0, 2), CORE.offset(2, 0, -2)}) helper.setBlock(mat, ModBlocks.MEDITATION_MAT);
		return PlayerSects.found(p, core, "Errand Sect");
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
