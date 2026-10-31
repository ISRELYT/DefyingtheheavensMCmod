package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Universe in a Sleeve (Sleeves, Technique#SLEEVE; the user's ask of 2026-10-26 and picks of 2026-10-27) and the small worlds carried up
 * the ways with their owners (SmallWorlds#inside, #cameOut; the user's ask of 2026-10-27). The technique (after Clone, from Heavenly
 * Being, its ability the last); its records kept and read back; one offered a place, taken in (a spectator seeing through its bearer's
 * eyes), carried onto a way and through it, kept in on the way, let out beside its bearer in a world; cast out at the foot of the Breach
 * with its bearer slain on the climb; four at most; let out by a sneak and by its bearer leaving the game; the pressure x2 for each, and
 * the Gate's quarter body on a True Immortal; and a small world's guests borne the same way (one in a guest's sleeve counted too, and
 * neither sent out of the world nor let out in it), its door shut as its owner takes a way, its gate leading out beside its owner where
 * they come out, and left as it was with its owner slain on the way. The tests' server has the mod's dimensions. Each test with players in
 * a batch of its own.
 */
public class SleeveGameTests implements FabricGameTest {
	/** A player in earnest (their game modes real: a spectator in a sleeve is one), of {@code realm} Early, at the test's corner. */
	private static ServerPlayer player(GameTestHelper helper, Realm realm, String name) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer p = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() { return false; }
		};
		server.getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		home(helper, p);
		CultivationManager.get(p).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		return p;
	}

	/** {@code p} back at the test's corner. */
	private static void home(GameTestHelper helper, ServerPlayer p) {
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
	}

	private static void leave(ServerPlayer p) {
		Sleeves.onDisconnect(p);
		Sleeves.Data data = Sleeves.Data.get(p.server);
		if (data.carried.remove(p.getUUID()) != null) data.setDirty();
		SmallWorlds.onDisconnect(p);
		SmallWorlds.Data.get(p.server).takeReturn(p.getUUID());
		ImmortalEssence.onDisconnect(p);
		FiveDecays.forget(p.getUUID());
		Lifespan.forget(p.getUUID());
		LifespanTribulation.forget(p.getUUID());
		QiFlight.forget(p.getUUID());
		if (p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
	}

	private static String key(Component line) {
		return line != null && line.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-9;
	}

	/** {@code target} offered a place in {@code bearer}'s sleeve and accepting it. @return whether they are in it */
	private static boolean takeIn(ServerPlayer bearer, ServerPlayer target) {
		Sleeves.offer(bearer, target);
		return Sleeves.accept(target, bearer.getGameProfile().getName()) == null && Sleeves.isCarried(target.getUUID());
	}

	// --- The technique, its ability, its records ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "sleeves_1")
	public void theTechniqueAndItsRecords(GameTestHelper helper) {
		helper.assertTrue(Technique.SLEEVE.realm() == Realm.HEAVENLY_BEING && Technique.byId("universe_in_a_sleeve") == Technique.SLEEVE
				&& Technique.SLEEVE.ordinal() == Technique.CLONE.ordinal() + 1, "Universe in a Sleeve, after Clone (saved by id), from Heavenly Being");
		// (The switches are synced as a mask over the ordinals, a long's 64 since 2026-10-31: each new ability goes at the end, the secret
		// realms' war arts after this one.)
		helper.assertTrue(Ability.SLEEVE.ordinal() == Ability.SMALL_WORLD.ordinal() + 1 && Ability.values().length <= 64 && !Ability.SLEEVE.isGhostly(),
				"Its ability after the Small World's, each new one at the end (the switches are a mask over the ordinals)");
		helper.assertTrue(Sleeves.REACH == 6 && Sleeves.MOST == 4 && Sleeves.GATE_BASE_BODIES == 0.25, "Six blocks, four at most, a quarter body (the user's pick)");
		int ti = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), en = PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION);
		helper.assertTrue(ImmortalGate.pressureShare(ti) == 0 && ImmortalGate.pressureShare(en) > 0, "The Gate presses on an Entering Nirvana, not a True Immortal");
		ServerPlayer mock = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(mock);
		try {
			c.setState(Realm.HEAVENLY_BEING, Stage.EARLY, 0);
			helper.assertTrue(!Ability.SLEEVE.isUnlocked(c), "Not without its manual read");
			c.learnTechnique(Technique.SLEEVE);
			helper.assertTrue(Ability.SLEEVE.isUnlocked(c), "Read, a Heavenly Being wields it");
			c.setState(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION, 0);
			helper.assertTrue(!Ability.SLEEVE.isUnlocked(c), "Below its realm, known but not wielded");
		} finally {
			c.setTechniques(EnumSet.noneOf(Technique.class));
			leave(mock);
		}
		// The records, kept and read back.
		Sleeves.Data data = new Sleeves.Data();
		UUID carried = UUID.randomUUID(), bearer = UUID.randomUUID();
		Sleeves.Carried one = new Sleeves.Carried(bearer, "test-bearer", GameType.ADVENTURE.getId(), new Sleeves.Place(Level.NETHER, new Vec3(1.5, 64, -3.5)));
		one.outAt = new Sleeves.Place(ModDimensions.GREAT_VOID, new Vec3(10, 70, 10));
		one.castIntoGap = true;
		data.carried.put(carried, one);
		Sleeves.Carried again = Sleeves.Data.load(data.save(new CompoundTag())).carried.get(carried);
		helper.assertTrue(again != null && again.bearer.equals(bearer) && "test-bearer".equals(again.bearerName) && again.mode == GameType.ADVENTURE.getId()
				&& again.from.equals(one.from) && again.outAt.equals(one.outAt) && again.castIntoGap && !again.gapFromUpper, "Its records kept and read back");
		helper.succeed();
	}

	// --- Carried, kept in on the way, let out ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "sleeves_carry", timeoutTicks = 400)
	public void takenInCarriedAndLetOut(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerLevel breach = server.getLevel(ModDimensions.VOID_BREACH), passage = server.getLevel(ModDimensions.IMMORTAL_PASSAGE), nether = server.getLevel(Level.NETHER);
		ServerPlayer bearer = player(helper, Realm.HEAVENLY_BEING, "test-sleeve-a"), target = player(helper, Realm.QI_REFINING, "test-sleeve-b");
		ServerPlayer[] more = {player(helper, Realm.QI_REFINING, "test-sleeve-c"), player(helper, Realm.QI_REFINING, "test-sleeve-d"),
				player(helper, Realm.QI_REFINING, "test-sleeve-e"), player(helper, Realm.QI_REFINING, "test-sleeve-f")};
		try {
			helper.assertTrue(breach != null && passage != null && nether != null, "The tests' server has the ways and the Nether");
			// Offered a place, and taken in: a spectator seeing through its bearer's eyes, and nothing else.
			Sleeves.offer(bearer, target);
			helper.assertTrue(key(Sleeves.accept(target, "nobody")).equals(ModLang.MSG_SLEEVE_NO_OFFER), "Accepted from the one who offered alone");
			helper.assertTrue(Sleeves.accept(target, bearer.getGameProfile().getName()) == null && Sleeves.isCarried(target.getUUID())
					&& Sleeves.carrying(bearer.getUUID()) == 1, "Accepted: in the sleeve");
			helper.assertTrue(target.isSpectator() && target.getCamera() == bearer && !Sleeves.mayView(target, target) && Sleeves.mayView(target, bearer),
					"A spectator seeing through its bearer's eyes alone");
			helper.assertTrue(Sleeves.pressureFactor(bearer) == 2 && Sleeves.pressureFactor(target) == 1, "Twice the pressure on its bearer");
			// Onto the Gate's way: carried there, kept in.
			bearer.teleportTo(passage, 0.5, 100, 0.5, 0, 0);
			Sleeves.tick(server);
			helper.assertTrue(target.level() == passage && Sleeves.isCarried(target.getUUID()), "Carried onto the way with its bearer");
			helper.assertTrue(key(Sleeves.leave(target)).equals(ModLang.MSG_SLEEVE_CANT_LEAVE), "Nobody steps out on the way");
			bearer.setShiftKeyDown(true);
			Sleeves.act(bearer);
			bearer.setShiftKeyDown(false);
			helper.assertTrue(Sleeves.isCarried(target.getUUID()), "...nor is let out partway through the climb");
			// Out into a world: let out beside its bearer, as it played before.
			bearer.teleportTo(nether, 8.5, 70, 8.5, 0, 0);
			Sleeves.tick(server);
			helper.assertTrue(!Sleeves.isCarried(target.getUUID()) && target.level() == nether && target.distanceTo(bearer) < 2 && !target.isSpectator()
					&& target.getCamera() == target && Sleeves.carrying(bearer.getUUID()) == 0, "Out beside its bearer in the world it came out into, playing as before");
			// Slain partway through the Breach's climb: cast out at its foot.
			helper.assertTrue(takeIn(bearer, target), "Taken in again");
			Vec3 foot = bearer.position();
			bearer.teleportTo(breach, 0.5, 100, 0.5, 0, 0);
			Sleeves.tick(server);
			helper.assertTrue(target.level() == breach, "Carried into the Breach");
			Sleeves.onDeath(bearer);
			helper.assertTrue(!Sleeves.isCarried(target.getUUID()) && target.level() == nether && target.position().distanceTo(foot) < 1 && !target.isSpectator(),
					"Its bearer slain on the climb: cast out at the foot of the way");
			// Four at most; a sneak lets them all out.
			home(helper, bearer);
			home(helper, target);
			Sleeves.tick(server);
			helper.assertTrue(takeIn(bearer, target) && takeIn(bearer, more[0]) && takeIn(bearer, more[1]) && takeIn(bearer, more[2]), "Four taken in");
			helper.assertTrue(Sleeves.carrying(bearer.getUUID()) == 4 && Sleeves.pressureFactor(bearer) == 16, "Four: sixteen times the pressure");
			helper.assertTrue(!takeIn(bearer, more[3]) && !Sleeves.isCarried(more[3].getUUID()), "...and no fifth");
			bearer.setShiftKeyDown(true);
			Sleeves.act(bearer);
			bearer.setShiftKeyDown(false);
			boolean allOut = Sleeves.carrying(bearer.getUUID()) == 0;
			for (ServerPlayer p : new ServerPlayer[] {target, more[0], more[1], more[2]}) allOut &= !Sleeves.isCarried(p.getUUID()) && !p.isSpectator();
			helper.assertTrue(allOut, "A sneak lets them all out, out in a world");
			// Its bearer leaving the game: set down beside it.
			helper.assertTrue(takeIn(bearer, target), "Taken in once more");
			Sleeves.onDisconnect(bearer);
			helper.assertTrue(!Sleeves.isCarried(target.getUUID()) && !target.isSpectator() && target.distanceTo(bearer) < 2, "Its bearer leaving the game: set down beside it");
			// (Beside, not in it: a step to its right where a body fits, the next round it; where it stands if nothing fits.)
			Sleeves.Place step = Sleeves.beside(bearer, 0), next = Sleeves.beside(bearer, 1);
			helper.assertTrue(step.level() == bearer.level().dimension() && step.at().distanceTo(bearer.position()) > 1.2 && step.at().distanceTo(bearer.position()) < 1.4
					&& next.at().distanceTo(bearer.position()) < 1.4 && next.at().distanceTo(step.at()) > 1.0,
					"Set down a step beside its bearer (" + step.at().distanceTo(bearer.position()) + ", " + next.at().distanceTo(step.at()) + " apart)");
			// The Gate: nothing on a True Immortal alone; carrying one, half a body over the climb; an Entering Nirvana's own share doubled.
			CultivationManager.get(bearer).setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
			helper.assertTrue(ImmortalGate.carriedShare(bearer) == 0, "A True Immortal alone: untouched at the Gate");
			helper.assertTrue(takeIn(bearer, target), "Taken in by an immortal");
			helper.assertTrue(near(ImmortalGate.carriedShare(bearer), Sleeves.GATE_BASE_BODIES / ImmortalGate.CLIMB_SECONDS * 2),
					"...carrying one: a quarter body doubled, over the climb");
			CultivationManager.get(bearer).setState(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION, 0);
			helper.assertTrue(near(ImmortalGate.carriedShare(bearer), ImmortalGate.pressureShare(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION)) * 2),
					"An Entering Nirvana carrying one: its own share doubled");
			helper.succeed();
		} finally {
			leave(bearer);
			leave(target);
			for (ServerPlayer p : more) leave(p);
		}
	}

	// --- A small world carried up with its owner ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "sleeves_world", timeoutTicks = 400)
	public void aSmallWorldCarriesThoseInIt(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerLevel sw = server.getLevel(ModDimensions.SMALL_WORLD), passage = server.getLevel(ModDimensions.IMMORTAL_PASSAGE), nether = server.getLevel(Level.NETHER);
		ServerPlayer owner = player(helper, Realm.TRUE_IMMORTAL, "test-sleeve-w"), guest = player(helper, Realm.QI_REFINING, "test-sleeve-x"),
				held = player(helper, Realm.QI_REFINING, "test-sleeve-y");
		SmallWorlds.Data data = SmallWorlds.Data.get(server);
		try {
			helper.assertTrue(sw != null && passage != null && nether != null, "The tests' server has the small worlds, the Gate's way and the Nether");
			helper.assertTrue(SmallWorlds.refine(owner), "Refined");
			SmallWorlds.World w = data.of(owner.getUUID());
			SmallWorlds.addGuest(owner, guest);
			helper.assertTrue(SmallWorlds.inside(owner) == 0 && Sleeves.pressureFactor(owner) == 1 && ImmortalGate.carriedShare(owner) == 0,
					"Nobody in it: nothing more on its owner");
			// A guest with one in their own sleeve: both borne; the one in the sleeve neither sent out of the world nor let out in it.
			helper.assertTrue(takeIn(guest, held), "The guest takes one into their sleeve");
			helper.assertTrue(SmallWorlds.enter(guest, w) && guest.level() == sw, "The guest in the world");
			Sleeves.tick(server);
			SmallWorlds.tick(server);
			helper.assertTrue(held.level() == sw && Sleeves.isCarried(held.getUUID()), "The one in their sleeve carried in with them, kept in it there");
			helper.assertTrue(SmallWorlds.inside(owner) == 2 && Sleeves.borne(owner) == 2 && Sleeves.pressureFactor(owner) == 4
					&& near(ImmortalGate.carriedShare(owner), Sleeves.GATE_BASE_BODIES / ImmortalGate.CLIMB_SECONDS * 4), "Both borne by its owner: four times the pressure");
			SmallWorlds.leave(guest);
			Sleeves.tick(server);
			helper.assertTrue(!Sleeves.isCarried(held.getUUID()) && held.level() == guest.level() && held.distanceTo(guest) < 2,
					"The guest out of the world: the one in their sleeve let out beside them");
			// The guest alone in it: borne as one in a sleeve.
			helper.assertTrue(SmallWorlds.enter(guest, w), "The guest in again");
			helper.assertTrue(SmallWorlds.inside(owner) == 1 && Sleeves.pressureFactor(owner) == 2
					&& near(ImmortalGate.carriedShare(owner), Sleeves.GATE_BASE_BODIES / ImmortalGate.CLIMB_SECONDS * 2), "One borne: twice the pressure, half a body at the Gate");
			// Its owner onto a way: its door shut behind them.
			SmallWorlds.tick(server);
			SmallWorlds.toggle(owner);
			helper.assertTrue(SmallWorlds.isOpen(owner.getUUID()), "Its door open");
			owner.teleportTo(passage, 0.5, 100, 0.5, 0, 0);
			SmallWorlds.tick(server);
			helper.assertTrue(!SmallWorlds.isOpen(owner.getUUID()), "Shut behind its owner as they took the way");
			// Out into a world: the guest's way out beside them there.
			owner.teleportTo(nether, 8.5, 70, 8.5, 0, 0);
			SmallWorlds.tick(server);
			CompoundTag back = data.peekReturn(guest.getUUID());
			helper.assertTrue(back != null && back.getString("Dimension").equals(Level.NETHER.location().toString())
					&& Math.hypot(back.getDouble("X") - 8.5, back.getDouble("Z") - 8.5) < 2, "Its gate leads out beside its owner where they came out");
			SmallWorlds.leave(guest);
			helper.assertTrue(guest.level() == nether && guest.distanceTo(owner) < 2, "...and the guest comes out there");
			// Slain on the way: those in it left as they were.
			helper.assertTrue(SmallWorlds.enter(guest, w), "The guest in again, from the Nether");
			SmallWorlds.tick(server);
			owner.teleportTo(passage, 0.5, 100, 0.5, 0, 0);
			SmallWorlds.tick(server);
			owner.setHealth(0);
			SmallWorlds.tick(server);
			owner.setHealth(owner.getMaxHealth());
			home(helper, owner);
			SmallWorlds.tick(server);
			back = data.peekReturn(guest.getUUID());
			helper.assertTrue(back != null && back.getString("Dimension").equals(Level.NETHER.location().toString())
					&& Math.hypot(back.getDouble("X") - 8.5, back.getDouble("Z") - 8.5) < 2, "Its owner slain on the way: the guest's way out where it was");
			helper.succeed();
		} finally {
			data.forget(owner.getUUID());
			leave(guest);
			leave(held);
			leave(owner);
		}
	}
}
