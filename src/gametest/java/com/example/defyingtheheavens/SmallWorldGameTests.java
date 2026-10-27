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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Phase 3d (2026-10-26): the small worlds (SmallWorlds). The World Seed (its recipe, its refining by an immortal alone, one each); the
 * small worlds' realm (the Immortal Realm's qi, not its breakthroughs; nobody born there; each world in its slot; its reach by its owner's
 * Laws); the Small World ability (last of the abilities, an immortal's with a world); who may come in (its owner, its guests); its records
 * kept and read back; its land (a coast always further out for a larger land, grass over earth over stone hanging beneath, its new edge laid
 * as it grows and nothing standing on it touched) and its gate; its door opened and closed, gone into and out of, a stranger in it sent out
 * by its tick (the tests' server has the mod's dimensions since 2026-10-27). Each test with players in a batch of its own.
 */
public class SmallWorldGameTests implements FabricGameTest {
	/** A survival player in earnest, of {@code realm} Early, at the test's corner. */
	private static ServerPlayer player(GameTestHelper helper, Realm realm, String name) {
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
		CultivationManager.get(p).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		return p;
	}

	private static void leave(ServerPlayer p) {
		SmallWorlds.onDisconnect(p);
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

	// --- The seed, the realm, the ability, the records ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "small_worlds_1")
	public void theSeedAndTheWorlds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// The seed: five Chaos Origin Stones and four Immortal Jade; refined over ten seconds of holding it.
		var recipe = level.getServer().getRecipeManager().byKey(DefyingTheHeavens.id("world_seed"));
		helper.assertTrue(recipe.isPresent() && recipe.get().getResultItem(level.registryAccess()).is(ModItems.WORLD_SEED), "The World Seed's recipe");
		long stones = 0, jade = 0;
		for (Ingredient part : recipe.get().getIngredients()) {
			if (part.test(new ItemStack(ModItems.CHAOS_ORIGIN_STONE))) stones++;
			if (part.test(new ItemStack(ModItems.IMMORTAL_JADE))) jade++;
		}
		helper.assertTrue(stones == 5 && jade == 4, "Five Chaos Origin Stones and four Immortal Jade (" + stones + ", " + jade + ")");
		helper.assertTrue(new ItemStack(ModItems.WORLD_SEED).getUseDuration() == SmallWorlds.REFINE_TICKS && SmallWorlds.REFINE_TICKS == 200,
				"Refined over ten seconds of holding it");
		// Its realm: the Immortal Realm's qi, not its breakthroughs; nobody born there of its own.
		helper.assertTrue(ModDimensions.hasImmortalQi(ModDimensions.SMALL_WORLD) && !ModDimensions.isImmortalRealm(ModDimensions.SMALL_WORLD)
				&& RealmSuppressionHandler.realmQi(ModDimensions.SMALL_WORLD) == PlayerCultivation.IMMORTAL_QI_MULTIPLIER,
				"The Immortal Realm's qi, a thousandfold; not the realm itself");
		helper.assertTrue(!NpcSpawner.spawnsLiving(ModDimensions.SMALL_WORLD) && !ModDimensions.isLowerRealm(ModDimensions.SMALL_WORLD),
				"No wanderer or beast born there, and no suppression");
		// Each world in its own slot, far apart; its reach by its owner's Laws.
		helper.assertTrue(SmallWorlds.centre(0).equals(new BlockPos(0, SmallWorlds.ISLAND_Y, 0))
				&& SmallWorlds.centre(65).equals(new BlockPos(SmallWorlds.SLOT_SPACING, SmallWorlds.ISLAND_Y, SmallWorlds.SLOT_SPACING))
				&& SmallWorlds.slotAt(SmallWorlds.SLOT_SPACING + 300, SmallWorlds.SLOT_SPACING - 200) == 65 && SmallWorlds.slotAt(-3000, 0) == -1,
				"Each world in a slot of its own, 4,096 apart");
		helper.assertTrue(SmallWorlds.radius(0) == 16 && SmallWorlds.radius(3) == 22 && SmallWorlds.radius(35) == 86 && SmallWorlds.radius(72) == 160
				&& SmallWorlds.radius(500) == 160, "Its land 16 blocks out, two more for every level of the Laws, up to 160");
		// The ability: the last (the switches are synced as a mask over the ordinals), an immortal's with a world.
		// (Universe in a Sleeve's after it since 2026-10-27: the switches are kept by ordinal, so each new one goes at the end.)
		helper.assertTrue(Ability.SMALL_WORLD.ordinal() == Ability.SLEEVE.ordinal() - 1 && Ability.values().length <= 32 && !Ability.SMALL_WORLD.isGhostly(),
				"The Small World ability, last of them all");
		ServerPlayer mock = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(mock);
		try {
			c.setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
			helper.assertTrue(!Ability.SMALL_WORLD.isUnlocked(c), "Not without a world");
			c.setSmallWorld(true);
			helper.assertTrue(Ability.SMALL_WORLD.isUnlocked(c), "An immortal's, once it has one");
			c.setState(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION, 0);
			helper.assertTrue(!Ability.SMALL_WORLD.isUnlocked(c), "Not fallen below True Immortal");
		} finally {
			c.setSmallWorld(false);
			leave(mock);
		}
		// The records, kept and read back.
		SmallWorlds.Data data = new SmallWorlds.Data();
		UUID owner = UUID.randomUUID(), guest = UUID.randomUUID();
		SmallWorlds.World w = data.make(owner, "test-owner", 7);
		w.guests.put(guest, "test-guest");
		w.laid = 30;
		SmallWorlds.World next = data.make(UUID.randomUUID(), "test-other", 0);
		SmallWorlds.Data back = SmallWorlds.Data.load(data.save(new CompoundTag()));
		SmallWorlds.World again = back.of(owner);
		helper.assertTrue(again != null && again.slot == w.slot && again.laid() == 30 && again.target() == SmallWorlds.radius(7)
				&& "test-owner".equals(again.ownerName()) && "test-guest".equals(again.guests().get(guest)) && back.atSlot(w.slot) == again
				&& next.slot != w.slot && back.atSlot(next.slot) != null, "Its records kept and read back");
		helper.succeed();
	}

	// --- Refined, and who may come in ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "small_worlds_refine")
	public void refinedByAnImmortalAlone(GameTestHelper helper) {
		ServerPlayer owner = player(helper, Realm.CORE_FORMATION, "test-world-a"), guest = player(helper, Realm.CORE_FORMATION, "test-world-b"),
				stranger = player(helper, Realm.TRUE_IMMORTAL, "test-world-c");
		SmallWorlds.Data data = SmallWorlds.Data.get(helper.getLevel().getServer());
		try {
			helper.assertTrue(key(SmallWorlds.whyNotRefine(owner)).equals(ModLang.MSG_SEED_NOT_IMMORTAL) && !SmallWorlds.refine(owner) && data.of(owner.getUUID()) == null,
					"Below True Immortal, no world comes of the seed");
			CultivationManager.get(owner).setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
			helper.assertTrue(SmallWorlds.whyNotRefine(owner) == null && SmallWorlds.refine(owner), "A True Immortal refines it");
			SmallWorlds.World w = data.of(owner.getUUID());
			helper.assertTrue(w != null && CultivationManager.get(owner).hasSmallWorld() && w.laid() == 0 && w.target() == SmallWorlds.BASE_RADIUS
					&& Ability.SMALL_WORLD.isUnlocked(CultivationManager.get(owner)), "A world of their own (its land laid when first entered), its door theirs");
			helper.assertTrue(!SmallWorlds.refine(owner) && key(SmallWorlds.whyNotRefine(owner)).equals(ModLang.MSG_SEED_HAVE_ONE), "One each");
			// Who may come in: its owner, and those on its guest list.
			helper.assertTrue(SmallWorlds.mayEnter(w, owner) && !SmallWorlds.mayEnter(w, guest) && !SmallWorlds.mayEnter(w, stranger), "Its owner alone, at first");
			helper.assertTrue(key(SmallWorlds.addGuest(owner, guest)).equals(ModLang.CMD_GUEST_ADDED) && SmallWorlds.mayEnter(w, guest)
					&& !SmallWorlds.mayEnter(w, stranger), "A guest put on its list may come in (its realm no matter); others not");
			helper.assertTrue(key(SmallWorlds.addGuest(owner, guest)).equals(ModLang.CMD_GUEST_ALREADY)
					&& key(SmallWorlds.addGuest(owner, owner)).equals(ModLang.CMD_GUEST_SELF), "Not twice, nor its owner");
			helper.assertTrue(key(SmallWorlds.removeGuest(owner, "TEST-WORLD-B")).equals(ModLang.CMD_GUEST_REMOVED) && !SmallWorlds.mayEnter(w, guest)
					&& key(SmallWorlds.removeGuest(owner, "nobody")).equals(ModLang.CMD_GUEST_UNKNOWN), "Taken off it again (its name in any case)");
			// Its door (the tests' server has the small worlds since 2026-10-27): opened before its owner by its switch, and closed by it.
			helper.assertTrue(SmallWorlds.whyNotOpen(owner) == null, "Its door may open here");
			SmallWorlds.toggle(owner);
			helper.assertTrue(SmallWorlds.isOpen(owner.getUUID()) && CultivationManager.get(owner).isAbilityEnabled(Ability.SMALL_WORLD), "Opened, its switch on");
			SmallWorlds.toggle(owner);
			helper.assertTrue(!SmallWorlds.isOpen(owner.getUUID()) && !CultivationManager.get(owner).isAbilityEnabled(Ability.SMALL_WORLD), "...and closed again");
			// Another's world, in a slot of its own.
			helper.assertTrue(SmallWorlds.refine(stranger) && data.of(stranger.getUUID()).slot != w.slot, "Another immortal's world in a slot of its own");
			helper.succeed();
		} finally {
			data.forget(owner.getUUID());
			data.forget(stranger.getUUID());
			leave(owner);
			leave(guest);
			leave(stranger);
		}
	}

	// --- In and out (the tests' server has the small worlds since 2026-10-27) ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "small_worlds_enter", timeoutTicks = 400)
	public void inAndOutAndStraysSentOut(GameTestHelper helper) {
		ServerPlayer owner = player(helper, Realm.TRUE_IMMORTAL, "test-world-d"), stranger = player(helper, Realm.TRUE_IMMORTAL, "test-world-e");
		SmallWorlds.Data data = SmallWorlds.Data.get(helper.getLevel().getServer());
		ServerLevel sw = helper.getLevel().getServer().getLevel(ModDimensions.SMALL_WORLD);
		try {
			helper.assertTrue(sw != null, "The tests' server has the small worlds");
			helper.assertTrue(SmallWorlds.refine(owner), "Refined");
			SmallWorlds.World w = data.of(owner.getUUID());
			net.minecraft.world.phys.Vec3 from = owner.position();
			helper.assertTrue(SmallWorlds.enter(owner, w) && owner.level() == sw && SmallWorlds.worldAt(owner) == w, "In it, at its heart");
			helper.assertTrue(w.laid() >= SmallWorlds.BASE_RADIUS && sw.getBlockState(w.centre()).is(Blocks.GRASS_BLOCK), "Its heart laid as it was first come into");
			helper.assertTrue(sw.getBlockState(w.centre().offset(0, 1, -6)).is(ModBlocks.SMALL_WORLD_GATE), "Its gate behind the way in");
			// A stranger in it (on no guest list) sent out by its tick, the server going on: going through the level's own list of players
			// as it sent one out broke the server's tick off with a ConcurrentModificationException (bug sweep 4).
			stranger.teleportTo(sw, w.centre().getX() + 2.5, SmallWorlds.ISLAND_Y + 1, w.centre().getZ() + 0.5, 0, 0);
			SmallWorlds.tick(helper.getLevel().getServer());
			helper.assertTrue(!ModDimensions.isSmallWorld(stranger.level().dimension()), "A stranger sent out by its tick");
			helper.assertTrue(owner.level() == sw, "...its owner left in it");
			SmallWorlds.leave(owner);
			helper.assertTrue(owner.level() == helper.getLevel() && owner.position().distanceTo(from) < 1, "Out where they came in from");
			helper.succeed();
		} finally {
			data.forget(owner.getUUID());
			leave(owner);
			leave(stranger);
		}
	}

	// --- Its land and its gate ---

	/** The lowest of the land under {@code top} (the last solid going down from it). */
	private static int lowest(ServerLevel level, BlockPos top) {
		BlockPos.MutableBlockPos pos = top.mutable();
		while (!level.getBlockState(pos.below()).isAir() && pos.getY() > top.getY() - 64) pos.move(0, -1, 0);
		return pos.getY();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "small_worlds_land", timeoutTicks = 400)
	public void itsLandAndItsGate(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SmallWorlds.World w = new SmallWorlds.Data().make(UUID.randomUUID(), "test-land", 0); // a world never kept, its shape its own
		// Its coast: lobed, always the further out for a larger land; it hangs the deeper too.
		for (int i = 0; i < 24; i++) {
			double a = i * Math.PI / 12, dx = Math.cos(a), dz = Math.sin(a);
			double e5 = SmallWorlds.edge(w, dx, dz, 5), e8 = SmallWorlds.edge(w, dx, dz, 8), e16 = SmallWorlds.edge(w, dx, dz, 16), e160 = SmallWorlds.edge(w, dx, dz, 160);
			helper.assertTrue(e5 < e8 && e8 < e16 && e16 < e160 && e5 > 2, "Its coast always the further out for a larger land (" + e5 + ", " + e8 + ", " + e16 + ")");
			int x = (int) Math.round(dx * 3), z = (int) Math.round(dz * 3);
			helper.assertTrue(SmallWorlds.depth(w, x, z, 3, e5) <= SmallWorlds.depth(w, x, z, 3, e8), "...and hanging the deeper");
		}
		BlockPos centre = helper.absolutePos(new BlockPos(4, 40, 4)), base = helper.absolutePos(new BlockPos(4, 72, 4));
		try {
			SmallWorlds.Job heart = new SmallWorlds.Job(w, centre, 0, 5, false);
			while (!heart.done()) heart.step(level, Integer.MAX_VALUE);
			helper.assertTrue(level.getBlockState(centre).is(Blocks.GRASS_BLOCK) && level.getBlockState(centre.below()).is(Blocks.DIRT)
					&& level.getBlockState(centre.below(3)).is(Blocks.DIRT), "Grass over earth at its heart");
			BlockState rock = level.getBlockState(centre.below(4));
			helper.assertTrue(rock.is(Blocks.STONE) || rock.is(Blocks.ANDESITE), "...stone under the earth (" + rock + ")");
			helper.assertTrue(level.getBlockState(centre.above()).isAir() && level.getBlockState(centre.offset(12, 0, 0)).isAir()
					&& level.getBlockState(centre.offset(0, 0, -12)).isAir(), "Nothing grown where one comes in; nothing past its edge");
			int bottom = lowest(level, centre);
			helper.assertTrue(centre.getY() - bottom >= 4, "Hanging beneath its heart (" + (centre.getY() - bottom) + " deep)");
			// Grown: its new edge laid, its heart deepened beneath, what stood on it untouched.
			level.setBlock(centre.above(), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
			level.setBlock(centre.offset(1, 0, 0), Blocks.AIR.defaultBlockState(), 2); // a hole dug in it stays dug
			SmallWorlds.Job grow = new SmallWorlds.Job(w, centre, 5, 9, true);
			while (!grow.done()) grow.step(level, Integer.MAX_VALUE);
			helper.assertTrue(level.getBlockState(centre.above()).is(Blocks.GOLD_BLOCK) && level.getBlockState(centre.offset(1, 0, 0)).isAir(),
					"What stood on it, and what was dug out of it, left as they were");
			helper.assertTrue(lowest(level, centre) <= bottom, "Its heart hanging no shallower for the growing");
			int land = 0;
			for (int i = 0; i < 16; i++) {
				double a = i * Math.PI / 8;
				BlockPos at = centre.offset((int) Math.round(Math.cos(a) * 7), 0, (int) Math.round(Math.sin(a) * 7));
				for (int dy = -3; dy <= 3; dy++) if (level.getBlockState(at.above(dy)).is(Blocks.GRASS_BLOCK)) land++;
			}
			helper.assertTrue(land >= 12, "Its new edge laid, seven out (" + land + " of 16 bearings)");
			// The gate: three wide and four high of starlight between pillars of jade, under jade and white jade, on a step of white jade.
			SmallWorlds.gate(level, base);
			boolean gate = true;
			for (int dx = -1; dx <= 1; dx++) for (int dy = 1; dy <= 4; dy++) gate &= level.getBlockState(base.offset(dx, dy, 0)).is(ModBlocks.SMALL_WORLD_GATE);
			helper.assertTrue(gate && level.getBlockState(base.offset(-2, 1, 0)).is(ModBlocks.JADE_STONE) && level.getBlockState(base.offset(2, 4, 0)).is(ModBlocks.JADE_STONE)
					&& level.getBlockState(base.offset(0, 5, 0)).is(ModBlocks.JADE_STONE) && level.getBlockState(base.offset(3, 6, 0)).is(Blocks.CALCITE)
					&& level.getBlockState(base).is(Blocks.CALCITE), "Its gate in its frame of jade");
			BlockState light = ModBlocks.SMALL_WORLD_GATE.defaultBlockState();
			helper.assertTrue(light.getCollisionShape(level, base).isEmpty() && light.getDestroySpeed(level, base) < 0 && light.getLightEmission() > 0,
					"Its starlight walked into, taken by no one, lit");
			helper.succeed();
		} finally {
			for (int dx = -14; dx <= 14; dx++) {
				for (int dz = -14; dz <= 14; dz++) {
					for (int y = centre.getY() - 40; y <= centre.getY() + 4; y++) level.setBlock(new BlockPos(centre.getX() + dx, y, centre.getZ() + dz), Blocks.AIR.defaultBlockState(), 2);
					for (int y = base.getY(); y <= base.getY() + 6; y++) level.setBlock(new BlockPos(base.getX() + dx, y, base.getZ() + dz), Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}
}
