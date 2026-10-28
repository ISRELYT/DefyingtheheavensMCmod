package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The land ledger and concealment (LandLedger, Concealments, 2026-10-28): what a player's act changes is noted with what stood there first,
 * struck off when set back, kept in the chunk's file; a player's blast too. A raised veil shows those it hides from the land as it was,
 * its barrier's cells as the air they stand in, and not the players inside; its owner, those they trust and the stronger see it all; its
 * packets are rewritten once and pass the second time as they are. A Formation Core keeps it as its second function, paid from the same
 * battery, the first to go when the qi runs short, its owner's mastery taking off its upkeep.
 */
public class ConcealmentGameTests implements FabricGameTest {
	private static ServerPlayer player(GameTestHelper helper, String name, Realm realm, Stage stage) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
		p.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(p).setState(realm, stage, 0);
		CultivationManager.refresh(p);
		return p;
	}

	private static LevelChunk chunk(GameTestHelper helper, BlockPos abs) {
		return helper.getLevel().getChunkAt(abs);
	}

	/** {@code abs} struck off its chunk's ledger (the test world is kept between runs). */
	private static void forget(GameTestHelper helper, BlockPos abs) {
		var ledger = ((LedgerHolder) chunk(helper, abs)).dth$ledger();
		if (ledger != null) ledger.remove(abs.asLong());
	}

	/**
	 * The test's whole ground struck off the ledger, before and after: a block noted beside one it sets would have the world's doing there
	 * noted too (LandLedger#spreading).
	 */
	private static void forgetAll(GameTestHelper helper) {
		for (BlockPos rel : BlockPos.betweenClosed(new BlockPos(-1, -1, -1), new BlockPos(9, 9, 9))) forget(helper, helper.absolutePos(rel));
	}

	/** What a broken or blasted block dropped, gone (a stray item fails a neighbour's test: PrimordialBeastsGameTests#theirSpoils). */
	private static void sweepDrops(GameTestHelper helper) {
		BlockPos low = helper.absolutePos(new BlockPos(-1, -1, -1)), high = helper.absolutePos(new BlockPos(9, 9, 9));
		for (net.minecraft.world.entity.item.ItemEntity item : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(low, high))) item.discard();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aPlayersActsAreNotedAndSetBackStruckOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2)), elsewhere = helper.absolutePos(new BlockPos(5, 2, 2));
		forgetAll(helper);
		level.setBlock(at, Blocks.STONE.defaultBlockState(), 3);
		level.setBlock(elsewhere, Blocks.DIRT.defaultBlockState(), 3);
		helper.assertTrue(LandLedger.original(level, at) == null && LandLedger.original(level, elsewhere) == null, "The world's own doings aren't noted");
		LandLedger.begin();
		try {
			level.setBlock(at, Blocks.OAK_PLANKS.defaultBlockState(), 3);
			level.setBlock(at, Blocks.GLASS.defaultBlockState(), 3);
		} finally {
			LandLedger.end();
		}
		helper.assertTrue(LandLedger.original(level, at) == Blocks.STONE.defaultBlockState(), "A player's act: what stood there first, kept through more changes");
		CompoundTag file = ChunkSerializer.write(level, chunk(helper, at));
		helper.assertTrue(file.contains("dth_ledger"), "...written into the chunk's file");
		LevelChunk chunk = chunk(helper, at);
		var kept = ((LedgerHolder) chunk).dth$ledger();
		((LedgerHolder) chunk).dth$setLedger(null);
		LandLedger.read(chunk, file);
		helper.assertTrue(LandLedger.original(level, at) == Blocks.STONE.defaultBlockState(), "...and read back from it");
		((LedgerHolder) chunk).dth$setLedger(kept);
		LandLedger.begin();
		try {
			level.setBlock(at, Blocks.STONE.defaultBlockState(), 3);
		} finally {
			LandLedger.end();
		}
		helper.assertTrue(LandLedger.original(level, at) == null, "Set back to what stood there first: struck off");

		ServerPlayer miner = player(helper, "ledger-miner", Realm.QI_REFINING, Stage.EARLY);
		miner.moveTo(elsewhere.getX() + 1.5, elsewhere.getY(), elsewhere.getZ() + 0.5);
		miner.gameMode.destroyBlock(elsewhere);
		helper.assertTrue(level.getBlockState(elsewhere).isAir() && LandLedger.original(level, elsewhere) == Blocks.DIRT.defaultBlockState(),
				"A block a player breaks: noted by their hands' own act");
		forgetAll(helper);
		sweepDrops(helper);
		helper.assertTrue(!LandLedger.recording(), "Every act's begin had its end");
		helper.succeed();
	}

	/** The user's catch, 2026-10-28: the grass under a hut's walls died to dirt, and showed the hut's outline under its concealment. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theWorldsDoingBesideAPlayersWorkIsNoted(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos ground = helper.absolutePos(new BlockPos(2, 1, 2)), wall = ground.above(), far = ground.offset(3, 0, 0);
		forgetAll(helper);
		level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		level.setBlock(far, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
		LandLedger.begin();
		try {
			level.setBlock(wall, Blocks.OAK_PLANKS.defaultBlockState(), 3);
		} finally {
			LandLedger.end();
		}
		level.setBlock(ground, Blocks.DIRT.defaultBlockState(), 3); // the grass dying under the wall: the world's doing, no player's act
		level.setBlock(far, Blocks.DIRT.defaultBlockState(), 3);
		helper.assertTrue(LandLedger.original(level, ground) == Blocks.GRASS_BLOCK.defaultBlockState(),
				"The grass that died under a player's wall is noted as grass");
		helper.assertTrue(LandLedger.original(level, far) == null, "...what changes away from any player's work isn't");
		BlockPos beside = wall.south();
		level.setBlock(beside, Blocks.SNOW.defaultBlockState(), 3);
		level.setBlock(wall.above(), Blocks.SNOW.defaultBlockState(), 3);
		helper.assertTrue(LandLedger.original(level, beside) == null,
				"The weather's snow on the ground beside the wall isn't noted (a bare strip would outline the wall)");
		helper.assertTrue(LandLedger.original(level, wall.above()) == Blocks.AIR.defaultBlockState(), "...snow lying on the wall itself is");
		for (BlockPos pos : List.of(wall.above(), beside, ground, wall, far)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		forgetAll(helper);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aPlayersBlastIsNoted(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<BlockPos> stones = new ArrayList<>();
		for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) stones.add(helper.absolutePos(new BlockPos(x, 1, z)));
		forgetAll(helper);
		for (BlockPos pos : stones) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
		ServerPlayer bomber = player(helper, "ledger-bomber", Realm.QI_REFINING, Stage.EARLY);
		BlockPos middle = helper.absolutePos(new BlockPos(2, 2, 2));
		bomber.moveTo(middle.getX() + 0.5, middle.getY() + 6, middle.getZ() + 0.5);
		level.explode(bomber, middle.getX() + 0.5, middle.getY(), middle.getZ() + 0.5, 3.0f, Level.ExplosionInteraction.TNT);
		int noted = 0;
		for (BlockPos pos : stones) {
			if (level.getBlockState(pos).isAir() && LandLedger.original(level, pos) == Blocks.STONE.defaultBlockState()) noted++;
		}
		forgetAll(helper);
		sweepDrops(helper);
		helper.assertTrue(noted > 0, "What a player's blast took is noted with what stood there (" + noted + ")");
		helper.assertTrue(!LandLedger.recording(), "...its act ended");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aVeilShowsTheLandAsItWas(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos center = helper.absolutePos(new BlockPos(4, 2, 4));
		BlockPos built = center.offset(1, 0, 0), cell = center.offset(-2, 0, 0), outside = center.offset(0, 0, 9), dug = center.offset(2, -1, 2);
		ServerPlayer owner = player(helper, "veil-owner", Realm.CORE_FORMATION, Stage.EARLY);
		ServerPlayer friend = player(helper, "veil-friend", Realm.QI_REFINING, Stage.EARLY);
		ServerPlayer weak = player(helper, "veil-weak", Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
		ServerPlayer strong = player(helper, "veil-strong", Realm.CORE_FORMATION, Stage.MID);
		UUID id = UUID.randomUUID();
		forgetAll(helper);
		forget(helper, outside);
		level.setBlock(built, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(outside, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(dug, Blocks.STONE.defaultBlockState(), 3);
		LandLedger.begin();
		try {
			level.setBlock(built, Blocks.OAK_PLANKS.defaultBlockState(), 3);
			level.setBlock(outside, Blocks.OAK_PLANKS.defaultBlockState(), 3);
			level.setBlock(dug, Blocks.AIR.defaultBlockState(), 3); // a pit dug
		} finally {
			LandLedger.end();
		}
		level.setBlock(cell, SectBarrierBlock.holding(SectBarrierBlock.Held.NONE), 3);
		Concealments.Veil veil = Concealments.veil(level, id, center, 6);
		try {
			Concealments.update(level, veil, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), owner.getUUID(), Set.of(friend.getUUID()));
			BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
			helper.assertTrue(Concealments.shown(level, weak, built, planks) == planks, "Lowered, it hides nothing");
			Concealments.set(level, veil, true, 6);
			helper.assertTrue(Concealments.raised(level).length > 0 && veil.isRaised(), "Raised");
			helper.assertTrue(Concealments.shown(level, weak, built, planks).isAir(), "One weaker than its owner sees the land as it was");
			helper.assertTrue(Concealments.shown(level, weak, cell, level.getBlockState(cell)).isAir(), "...the barrier's cell as the air it stands in");
			helper.assertTrue(Concealments.shown(level, weak, outside, planks) == planks, "...and what is outside it as it is");
			helper.assertTrue(Concealments.shown(level, owner, built, planks) == planks && Concealments.shown(level, friend, built, planks) == planks
					&& Concealments.shown(level, strong, built, planks) == planks, "Its owner, one they trust and one stronger see it all");
			helper.assertTrue(Concealments.reachesHidden(weak, built) && !Concealments.reachesHidden(owner, built), "Only the hidden-from reach for what they can't see");
			// It hides; it doesn't block (the user, 2026-10-28): it takes the barrier too to keep anyone out.
			net.minecraft.world.phys.AABB wall = new net.minecraft.world.phys.AABB(built).deflate(0.1), pit = new net.minecraft.world.phys.AABB(dug).deflate(0.1);
			helper.assertTrue(level.noCollision(weak, wall) && !level.noCollision(owner, wall), "One it hides from walks through hidden work; its owner meets it");
			helper.assertTrue(!level.noCollision(weak, pit) && level.noCollision(owner, pit), "...and stands on the ground they are shown over a hidden pit");
			helper.assertTrue(Concealments.untouchedBy(weak, built, planks) && !Concealments.untouchedBy(owner, built, planks), "...nothing hidden touches them");

			owner.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
			helper.assertTrue(Concealments.hidesPlayer(owner, weak) && !Concealments.hidesPlayer(owner, strong), "The players inside: hidden from the weaker only");
			owner.moveTo(outside.getX() + 0.5, outside.getY(), outside.getZ() + 0.5);
			helper.assertTrue(!Concealments.hidesPlayer(owner, weak), "...and only while inside");

			Packet<?> update = new ClientboundBlockUpdatePacket(built, planks);
			Packet<?> shown = Concealments.shownPacket(weak, update);
			helper.assertTrue(shown instanceof ClientboundBlockUpdatePacket rewritten && rewritten.getBlockState().isAir(), "A block's change goes out to them as it was");
			helper.assertTrue(Concealments.shownPacket(weak, shown) == shown && Concealments.shownPacket(owner, update) == update,
					"A rewritten packet passes the second time as it is; the owner's as it is");
			Packet<?> section = Concealments.sectionPacket(List.of(built, built.above()), List.of(planks, Blocks.TORCH.defaultBlockState()));
			Packet<?> sectionShown = Concealments.shownPacket(weak, section);
			List<BlockState> states = new ArrayList<>();
			((ClientboundSectionBlocksUpdatePacket) sectionShown).runUpdates((pos, state) -> states.add(state));
			helper.assertTrue(sectionShown != section && states.size() == 2 && states.get(0).isAir(), "A section's changes too: " + states);

			Concealments.set(level, veil, false, 6);
			helper.assertTrue(Concealments.shown(level, weak, built, planks) == planks, "Lowered again, all is shown");
			helper.assertTrue(!level.noCollision(weak, wall), "...and met");
		} finally {
			Concealments.dissolve(level, id);
			level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(dug, Blocks.AIR.defaultBlockState(), 3);
			forgetAll(helper);
			forget(helper, outside);
		}
		helper.assertTrue(Concealments.get(level, id) == null, "Dissolved: forgotten");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCoreKeepsItsConcealment(GameTestHelper helper) throws ReflectiveOperationException {
		ServerLevel level = helper.getLevel();
		BlockPos rel = new BlockPos(4, 2, 4);
		helper.setBlock(rel.below(), Blocks.STONE);
		helper.setBlock(rel, ModBlocks.FORMATION_CORE);
		for (BlockPos vein : new BlockPos[] {rel.east(), rel.west(), rel.north(), rel.south()}) helper.setBlock(vein, ModBlocks.QI_VEIN);
		FormationCoreBlockEntity core = (FormationCoreBlockEntity) level.getBlockEntity(helper.absolutePos(rel));
		ServerPlayer owner = player(helper, "core-owner", Realm.CORE_FORMATION, Stage.EARLY);
		Method economy = FormationCoreBlockEntity.class.getDeclaredMethod("economy", ServerLevel.class);
		economy.setAccessible(true);
		Field battery = FormationCoreBlockEntity.class.getDeclaredField("battery");
		battery.setAccessible(true);
		try {
			core.scanNetwork(level);
			helper.assertTrue(core.getVeins() == 4 && core.canControl(owner), "Four veins joined; the first hand claims it");
			helper.assertTrue(Math.abs(FormationCoreBlockEntity.veilUpkeep(8) - FormationCoreBlockEntity.upkeep(8) * 0.75) < 1.0e-9,
					"A concealment costs three quarters of a barrier's upkeep");
			core.configure(owner, 8, false, 8, true);
			Concealments.Veil veil = Concealments.get(level, core.getFormationId());
			helper.assertTrue(core.isVeilRaised() && veil != null && veil.radius() == 8 && owner.getUUID().equals(veil.owner())
					&& veil.rank() == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), "Switched on: raised, answering to its owner and as strong");
			helper.assertTrue(!core.isRaised(), "...the barrier left down");
			core.trust(UUID.randomUUID(), "friend");
			helper.assertTrue(veil.trusted().size() == 1, "Those it trusts see through it");
			core.configure(owner, Formations.MIN_RADIUS, true, 8, true);
			helper.assertTrue(core.isRaised() && core.isVeilRaised(), "Both at once, the veins carrying both: unseen, and no way in");
			core.configure(owner, Formations.MIN_RADIUS, false, 8, true);
			helper.assertTrue(!core.isRaised() && core.isVeilRaised(), "The barrier down, the concealment stands alone");

			core.configure(owner, 8, false, 64, true);
			battery.setDouble(core, 0);
			economy.invoke(core, level);
			helper.assertTrue(!core.isVeilRaised() && core.isVeilWanted(), "Wider than its veins carry, the store dry: it falls, still wanted");
			battery.setDouble(core, FormationCoreBlockEntity.BATTERY);
			economy.invoke(core, level);
			helper.assertTrue(core.isVeilRaised() && veil.radius() == 64, "A full store raises it again");
			core.configure(owner, 8, false, 64, false);
			helper.assertTrue(!core.isVeilRaised(), "Switched off: down");

			CultivationManager.own(owner).setMastery(CraftMastery.Craft.FORMATIONS, 80); // a Master's practice
			core.configure(owner, 8, false, 8, false);
			helper.assertTrue(Math.abs(core.upkeepShare() - (1 - 0.05 * CraftMastery.Rank.MASTER.ordinal())) < 1.0e-9,
					"Its owner's mastery takes 5% off a rank: " + core.upkeepShare());
			helper.succeed();
		} finally {
			core.onRemovedFromWorld(level);
			helper.setBlock(rel, Blocks.AIR);
			for (BlockPos vein : new BlockPos[] {rel.east(), rel.west(), rel.north(), rel.south(), rel.below()}) helper.setBlock(vein, Blocks.AIR);
		}
	}
}
