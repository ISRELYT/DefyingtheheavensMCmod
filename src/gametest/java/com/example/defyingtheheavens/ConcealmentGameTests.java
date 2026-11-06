package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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

	/** A veil of {@code owner}'s (and {@code friends}') about {@code center}, raised, strong enough to hide from the weak. */
	private static Concealments.Veil raise(ServerLevel level, UUID id, BlockPos center, ServerPlayer owner, UUID... friends) {
		Concealments.Veil veil = Concealments.veil(level, id, center, 6);
		Concealments.update(level, veil, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), owner.getUUID(), Set.of(friends));
		Concealments.set(level, veil, true, 6);
		return veil;
	}

	/** {@code pos} set to {@code state} as a player's work (what stands there is what stood before them). */
	private static void work(ServerLevel level, BlockPos pos, BlockState state) {
		LandLedger.begin();
		try {
			level.setBlock(pos, state, 3);
		} finally {
			LandLedger.end();
		}
	}

	/** The user's report, 2026-11-06: walking through a hidden wall hurt, at its edge: the eyes in the air beside it, the square about them in it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void noOneIsSmotheredInOrBesideWhatIsHiddenFromThem(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos center = helper.absolutePos(new BlockPos(4, 2, 4)), wall = center.offset(1, 1, 0), real = wall.west();
		ServerPlayer owner = player(helper, "breath-owner", Realm.CORE_FORMATION, Stage.EARLY);
		ServerPlayer weak = player(helper, "breath-weak", Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
		UUID id = UUID.randomUUID();
		forgetAll(helper);
		level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(real, Blocks.AIR.defaultBlockState(), 3);
		work(level, wall, Blocks.STONE.defaultBlockState());
		raise(level, id, center, owner);
		try {
			double feet = wall.getY() + 0.5 - weak.getEyeHeight();
			weak.moveTo(wall.getX() - 0.1, feet, wall.getZ() + 0.5);
			owner.moveTo(wall.getX() - 0.1, feet, wall.getZ() + 0.5);
			helper.assertTrue(BlockPos.containing(weak.getEyePosition()).equals(real), "The eyes in the air beside the wall");
			helper.assertTrue(!weak.isInWall(), "One it hides from isn't smothered at the edge of a hidden wall");
			helper.assertTrue(owner.isInWall(), "...its owner, who sees it, is");
			weak.moveTo(wall.getX() + 0.5, feet, wall.getZ() + 0.5);
			owner.moveTo(wall.getX() + 0.5, feet, wall.getZ() + 0.5);
			helper.assertTrue(!weak.isInWall() && owner.isInWall(), "Eyes right in it, the same");
			weak.moveTo(wall.getX() - 0.1, feet, wall.getZ() + 0.5);
			level.setBlock(real, Blocks.STONE.defaultBlockState(), 3);
			forget(helper, real); // no player's work: a wall of the land's own, shown as it stands
			helper.assertTrue(weak.isInWall(), "A block they can see still smothers them, a hidden one beside it or not");
		} finally {
			Concealments.dissolve(level, id);
			level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(real, Blocks.AIR.defaultBlockState(), 3);
			forgetAll(helper);
		}
		helper.succeed();
	}

	/**
	 * The user's asks, 2026-11-06: what a stranger destroys under a veil "should look destroyed for the stranger", "the owner shouldnt see the changed
	 * ground, it should look like a spell never happened, and if the concealment falls, the ground should look like it does for the owner, not
	 * destroyed", "although area outside the barrier that gets destroyed should still have terrain destruction". Mined, blasted or taken by a spell,
	 * even beside the owner's hidden work and even a block of it; what the owner and those they trust do stays hidden.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aStrangersDestructionIsForStrangersEyesAlone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos center = helper.absolutePos(new BlockPos(4, 2, 4));
		BlockPos mined = center.offset(2, 0, 0), carved = center.offset(0, 0, 2), hut = carved.above(), roof = hut.above(), outside = center.offset(-4, 4, -4);
		BlockPos ownMined = center.offset(-2, 0, 0), friendMined = center.offset(-2, 0, 2), ownCarved = center.offset(-2, 0, -2), blasted = center.offset(2, 0, -2);
		BlockState stone = Blocks.STONE.defaultBlockState(), planks = Blocks.OAK_PLANKS.defaultBlockState();
		ServerPlayer owner = player(helper, "grief-owner", Realm.CORE_FORMATION, Stage.EARLY);
		ServerPlayer friend = player(helper, "grief-friend", Realm.QI_REFINING, Stage.EARLY);
		ServerPlayer stranger = player(helper, "grief-stranger", Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
		ServerPlayer other = player(helper, "grief-other", Realm.FOUNDATION_BUILDING, Stage.EARLY);
		UUID id = UUID.randomUUID();
		forgetAll(helper);
		List<BlockPos> ground = List.of(mined, carved, ownMined, friendMined, ownCarved, blasted, outside);
		for (BlockPos pos : ground) level.setBlock(pos, stone, 3);
		for (BlockPos pos : List.of(hut, roof)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		work(level, hut, planks); // the owner's hut, beside the ground a stranger's spell will take
		work(level, roof, planks);
		Concealments.Veil veil = raise(level, id, center, owner, friend.getUUID());
		try {
			stranger.moveTo(mined.getX() + 0.5, mined.getY() + 1, mined.getZ() + 0.5);
			helper.assertTrue(stranger.gameMode.destroyBlock(mined), "A stranger mines ground they can see");
			helper.assertTrue(level.getBlockState(mined) == stone && LandLedger.original(level, mined) == null, "The ground itself stands");
			helper.assertTrue(Concealments.shown(level, stranger, mined, stone).isAir() && Concealments.shown(level, other, mined, stone).isAir(),
					"It looks gone to them and everyone else");
			helper.assertTrue(Concealments.shown(level, owner, mined, stone) == stone && Concealments.shown(level, friend, mined, stone) == stone,
					"...and never happened for its owner and those they trust");
			net.minecraft.world.phys.AABB in = new net.minecraft.world.phys.AABB(mined).deflate(0.1);
			helper.assertTrue(level.noCollision(stranger, in) && !level.noCollision(owner, in), "A stranger goes by what they see, its owner stands on the land");
			Packet<?> update = new ClientboundBlockUpdatePacket(mined, stone);
			helper.assertTrue(Concealments.shownPacket(stranger, update) instanceof ClientboundBlockUpdatePacket rewritten && rewritten.getBlockState().isAir()
					&& Concealments.shownPacket(owner, update) == update, "Its change goes out so: air to the stranger, nothing new to its owner");
			helper.assertTrue(HeavenSpells.carvableAnyway(level, carved, stranger), "A stranger's spell may take the ground");
			level.removeBlock(carved, false);
			helper.assertTrue(level.getBlockState(carved) == stone && Concealments.shown(level, other, carved, stone).isAir() && Concealments.shown(level, owner, carved, stone) == stone,
					"Taken by a stranger's spell, right beside hidden work: the same");
			helper.assertTrue(HeavenSpells.carvableAnyway(level, roof, stranger), "...a block of the hidden work too");
			level.removeBlock(roof, false);
			helper.assertTrue(level.getBlockState(roof) == planks && LandLedger.original(level, roof) != null && Concealments.shown(level, owner, roof, planks) == planks,
					"...which stands, still its owner's");
			helper.assertTrue(Concealments.shown(level, other, hut, planks).isAir(), "The rest of the hut stays hidden");
			LandLedger.begin(stranger);
			try {
				level.removeBlock(blasted, false); // as a blast of theirs takes it (ExplosionMixin)
				level.setBlock(blasted, Blocks.FIRE.defaultBlockState(), 3);
			} finally {
				LandLedger.end();
			}
			helper.assertTrue(level.getBlockState(blasted) == stone && Concealments.shown(level, other, blasted, stone).isAir(), "A stranger's blast the same, its fire setting nothing in the hole's air");
			helper.assertTrue(HeavenSpells.carvableAnyway(level, outside, stranger), "Outside the veil a stranger's spell takes the ground too");
			level.removeBlock(outside, false);
			helper.assertTrue(level.getBlockState(outside).isAir() && Concealments.shown(level, owner, outside, level.getBlockState(outside)).isAir(), "...for good, for everyone");

			owner.moveTo(ownMined.getX() + 0.5, ownMined.getY() + 1, ownMined.getZ() + 0.5);
			helper.assertTrue(owner.gameMode.destroyBlock(ownMined), "Its owner mines");
			helper.assertTrue(LandLedger.original(level, ownMined) == stone && Concealments.shown(level, other, ownMined, level.getBlockState(ownMined)) == stone,
					"Mined by its owner: hidden, as it ever was");
			friend.moveTo(friendMined.getX() + 0.5, friendMined.getY() + 1, friendMined.getZ() + 0.5);
			helper.assertTrue(friend.gameMode.destroyBlock(friendMined) && LandLedger.original(level, friendMined) == stone, "...by one they trust, the same");
			helper.assertTrue(HeavenSpells.carvableAnyway(level, ownCarved, owner), "Its owner's spell may take the ground");
			level.removeBlock(ownCarved, false);
			helper.assertTrue(LandLedger.original(level, ownCarved) == stone, "...and that stays hidden too");

			Concealments.set(level, veil, false, 6);
			helper.assertTrue(veil.wrecked.isEmpty() && Concealments.shown(level, other, mined, stone) == stone && Concealments.shown(level, stranger, carved, stone) == stone,
					"The veil down, the land is as its owner knows it, for everyone");
			Concealments.set(level, veil, true, 6);
			helper.assertTrue(Concealments.shown(level, other, mined, stone) == stone, "...and it stays so, raised again");
		} finally {
			Concealments.dissolve(level, id);
			for (BlockPos pos : ground) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			for (BlockPos pos : List.of(hut, roof)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			forgetAll(helper);
			sweepDrops(helper);
		}
		helper.assertTrue(!LandLedger.recording(), "Every act's begin had its end");
		helper.succeed();
	}

	/** {@code pos} set to {@code state} by {@code player}'s own act. */
	private static void placed(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
		LandLedger.begin(player);
		try {
			level.setBlock(pos, state, 3);
		} finally {
			LandLedger.end();
		}
	}

	/** The user's rule, 2026-11-06: a block a stranger places is there for them (and everyone else), and doesn't exist for the owner or those they trust. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aStrangersBlockIsTheirsAndNotTheOwners(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos center = helper.absolutePos(new BlockPos(4, 2, 4)), block = center.offset(2, 1, 0), overhead = block.above(), gone = center.offset(-2, 1, 0), built = center.offset(0, 1, 2),
				meadow = center.offset(0, 1, -2);
		BlockState stone = Blocks.STONE.defaultBlockState(), air = Blocks.AIR.defaultBlockState(), planks = Blocks.OAK_PLANKS.defaultBlockState();
		ServerPlayer owner = player(helper, "placed-owner", Realm.CORE_FORMATION, Stage.EARLY);
		ServerPlayer friend = player(helper, "placed-friend", Realm.QI_REFINING, Stage.EARLY);
		ServerPlayer stranger = player(helper, "placed-stranger", Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
		ServerPlayer other = player(helper, "placed-other", Realm.FOUNDATION_BUILDING, Stage.EARLY);
		ServerPlayer strong = player(helper, "placed-strong", Realm.CORE_FORMATION, Stage.MID);
		UUID id = UUID.randomUUID();
		forgetAll(helper);
		List<BlockPos> spots = List.of(block, overhead, gone, built, meadow);
		for (BlockPos pos : spots) level.setBlock(pos, air, 3);
		Concealments.Veil veil = raise(level, id, center, owner, friend.getUUID());
		try {
			placed(level, stranger, block, stone);
			placed(level, strong, overhead, stone);
			helper.assertTrue(level.getBlockState(block) == stone && LandLedger.original(level, block) == null, "It is a real block, and not hidden as the owner's work is");
			helper.assertTrue(Concealments.shown(level, stranger, block, stone) == stone && Concealments.shown(level, other, block, stone) == stone
					&& Concealments.shown(level, strong, block, stone) == stone, "There for the stranger who set it, another, and one stronger");
			helper.assertTrue(Concealments.shown(level, owner, block, stone).isAir() && Concealments.shown(level, friend, block, stone).isAir(),
					"Not there for its owner or one they trust");
			helper.assertTrue(Concealments.shown(level, owner, overhead, stone).isAir() && Concealments.shown(level, strong, overhead, stone) == stone,
					"One who sees through it is a stranger still: theirs is there for them, not for its owner");
			net.minecraft.world.phys.AABB in = new net.minecraft.world.phys.AABB(block).deflate(0.1);
			helper.assertTrue(level.noCollision(owner, in) && !level.noCollision(stranger, in), "Its owner walks through it; a stranger meets it");
			double feet = block.getY() + 0.5 - owner.getEyeHeight();
			owner.moveTo(block.getX() + 0.5, feet, block.getZ() + 0.5);
			stranger.moveTo(block.getX() + 0.5, feet, block.getZ() + 0.5);
			helper.assertTrue(!owner.isInWall() && stranger.isInWall(), "It doesn't smother its owner");
			helper.assertTrue(Concealments.reachesHidden(owner, block) && !Concealments.reachesHidden(stranger, block), "Its owner can't lay hands on it; its stranger can");
			Packet<?> update = new ClientboundBlockUpdatePacket(block, stone);
			Packet<?> shown = Concealments.shownPacket(owner, update);
			helper.assertTrue(shown instanceof ClientboundBlockUpdatePacket rewritten && rewritten.getBlockState().isAir(), "Its change goes out to its owner as air");
			helper.assertTrue(Concealments.shownPacket(owner, shown) == shown && Concealments.shownPacket(stranger, update) == update,
					"...once, and to the stranger as it is");
			CompoundTag saved = veil.save();
			helper.assertTrue(Concealments.Veil.load(saved).strangers.size() == 2, "It is remembered with the veil");

			placed(level, stranger, gone, stone);
			stranger.moveTo(gone.getX() + 0.5, gone.getY() + 2, gone.getZ() + 0.5);
			helper.assertTrue(stranger.gameMode.destroyBlock(gone) && level.getBlockState(gone).isAir() && Concealments.shown(level, owner, gone, air).isAir()
					&& !veil.strangers.containsKey(gone.asLong()), "Mined by the stranger, it is gone and forgotten");
			placed(level, stranger, built, stone);
			LandLedger.begin(owner);
			try {
				Concealments.clearFor(owner, built);
				level.setBlock(built, planks, 3);
			} finally {
				LandLedger.end();
			}
			helper.assertTrue(level.getBlockState(built) == planks && !veil.strangers.containsKey(built.asLong()), "Its owner building there: it gives way");
			helper.assertTrue(Concealments.shown(level, owner, built, planks) == planks && Concealments.shown(level, stranger, built, planks).isAir(),
					"...and what they build is theirs, hidden as ever, over what stood before the stranger's block");
			level.setBlock(meadow, Blocks.GRASS.defaultBlockState(), 2);
			placed(level, stranger, meadow, stone);
			helper.assertTrue(Concealments.shown(level, owner, meadow, stone).is(Blocks.GRASS), "Set down over grass, the grass is what its owner goes on seeing");
			owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
			UseBlockCallback.EVENT.invoker().interact(owner, level, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(meadow), Direction.UP, meadow, false));
			helper.assertTrue(level.getBlockState(meadow).isAir() && !veil.strangers.containsKey(meadow.asLong()), "...and it gives way when they build on that grass");

			Concealments.set(level, veil, false, 6);
			helper.assertTrue(Concealments.shown(level, owner, block, stone) == stone && veil.strangers.isEmpty(), "The veil down, it is simply there for all");
			Concealments.set(level, veil, true, 6);
			helper.assertTrue(Concealments.shown(level, owner, block, stone) == stone, "...and stays so raised again");
		} finally {
			Concealments.dissolve(level, id);
			for (BlockPos pos : spots) level.setBlock(pos, air, 3);
			forgetAll(helper);
			sweepDrops(helper);
		}
		helper.succeed();
	}

	/**
	 * The user's rule, 2026-11-06: what a stranger makes of ground that stands already looks so to strangers alone: "the owner shouldnt see the changed
	 * ground, it should look like a spell never happened". A door they swing is the same block still, there for all. The owner's hidden work that
	 * shifts by a stranger's act stays hidden; what they rework of their own stays theirs. Water and fire run and burn on nothing.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void whatAStrangerMakesOfStandingGroundIsForStrangersEyes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos center = helper.absolutePos(new BlockPos(4, 2, 4)), ground = center.offset(2, 1, 0), door = center.offset(-2, 1, 0), fence = center.offset(0, 1, 2),
				own = center.offset(0, 1, -2), snow = center.offset(2, 1, 2), rock = center.offset(-2, 1, 2), burnt = center.offset(-2, 1, -2), iced = center.offset(2, 1, -2),
				pool = center.offset(0, 1, 0), flame = center.offset(0, 3, 0);
		BlockState air = Blocks.AIR.defaultBlockState(), closed = Blocks.OAK_DOOR.defaultBlockState(), open = closed.setValue(DoorBlock.OPEN, true),
				alone = Blocks.OAK_FENCE.defaultBlockState(), joined = alone.setValue(FenceBlock.NORTH, true), layer = Blocks.SNOW.defaultBlockState(),
				layers = layer.setValue(SnowLayerBlock.LAYERS, 2), blackstone = Blocks.BLACKSTONE.defaultBlockState(), farmland = Blocks.FARMLAND.defaultBlockState(),
				grass = Blocks.GRASS_BLOCK.defaultBlockState(), stone = Blocks.STONE.defaultBlockState(), water = Blocks.WATER.defaultBlockState();
		ServerPlayer owner = player(helper, "worked-owner", Realm.CORE_FORMATION, Stage.EARLY);
		ServerPlayer stranger = player(helper, "worked-stranger", Realm.FOUNDATION_BUILDING, Stage.EARLY);
		UUID id = UUID.randomUUID();
		forgetAll(helper);
		List<BlockPos> spots = List.of(ground, door, fence, own, snow, rock, burnt, iced, pool, flame);
		for (BlockPos pos : spots) level.setBlock(pos, air, 3);
		level.setBlock(ground, grass, 3);
		level.setBlock(door, closed, 2);
		level.setBlock(snow, layer, 2);
		level.setBlock(rock, stone, 3);
		level.setBlock(burnt, stone, 3);
		level.setBlock(iced, stone, 3);
		work(level, fence, alone);
		Concealments.Veil veil = raise(level, id, center, owner);
		try {
			stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
			stranger.gameMode.useItemOn(stranger, level, stranger.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false));
			helper.assertTrue(level.getBlockState(ground) == grass && veil.strangers.isEmpty() && LandLedger.original(level, ground) == null, "A stranger's hoe leaves the ground as it was");
			helper.assertTrue(Concealments.shown(level, stranger, ground, grass) == farmland && Concealments.shown(level, owner, ground, grass) == grass, "...a furrow to strangers, grass to its owner");
			helper.assertTrue(!Concealments.reachesHidden(owner, ground) && !level.noCollision(owner, new net.minecraft.world.phys.AABB(ground).deflate(0.1)), "...who can build on it, and stands on it");
			helper.assertTrue(!Concealments.reachesHidden(stranger, ground), "The stranger goes on working what they made of it");

			placed(level, stranger, door, open);
			helper.assertTrue(veil.strangers.isEmpty() && Concealments.shown(level, owner, door, open) == open && !Concealments.reachesHidden(owner, door), "A door they swing is swung for its owner too, and theirs to swing back");

			LandLedger.begin(stranger);
			try {
				helper.assertTrue(HeavenSpells.carvableAnyway(level, rock, stranger), "A stranger's spell may change the ground, from a weapon's art inside their act");
				level.setBlock(rock, blackstone, 3);
			} finally {
				LandLedger.end();
			}
			helper.assertTrue(HeavenSpells.carvableAnyway(level, burnt, stranger), "...or cast with its key");
			level.setBlock(burnt, blackstone, 3);
			LandLedger.markSpell(level, iced, stranger);
			level.setBlock(iced, Blocks.ICE.defaultBlockState(), 3);
			for (BlockPos pos : List.of(rock, burnt, iced)) {
				helper.assertTrue(level.getBlockState(pos) == stone && Concealments.shown(level, owner, pos, stone) == stone && !Concealments.reachesHidden(owner, pos), "Ground it changes stands for its owner, to stand and build on");
			}
			helper.assertTrue(Concealments.shown(level, stranger, rock, stone) == blackstone && Concealments.shown(level, stranger, burnt, stone) == blackstone && Concealments.shown(level, stranger, iced, stone).is(Blocks.ICE),
					"...and looks changed to the stranger");
			helper.assertTrue(veil.strangers.isEmpty(), "None of it is a block of theirs");

			placed(level, stranger, pool, water);
			placed(level, stranger, flame, Blocks.FIRE.defaultBlockState());
			helper.assertTrue(level.getBlockState(pool).isAir() && level.getBlockState(flame).isAir() && Concealments.shown(level, stranger, pool, air) == water
					&& Concealments.shown(level, stranger, flame, air).is(Blocks.FIRE) && Concealments.shown(level, owner, pool, air).isAir() && Concealments.shown(level, owner, flame, air).isAir(),
					"Water and fire they set run and burn on nothing: strangers see them, the land has none");
			Concealments.Veil kept = Concealments.Veil.load(veil.save());
			helper.assertTrue(kept.wreckAt(ground.asLong()) == farmland && kept.wreckAt(pool.asLong()) == water && kept.wreckAt(flame.asLong()) == null, "What they did is remembered with the veil, its fires not");
			BlockPos marked = center.offset(-1, 1, 0);
			level.setBlock(marked, stone, 3);
			helper.assertTrue(HeavenSpells.stainTemporary(level, marked, blackstone, 40, stranger), "A stranger's passing char is laid");
			helper.assertTrue(level.getBlockState(marked) == stone && !HeavenSpells.isTemporary(level, marked) && Concealments.shown(level, stranger, marked, stone) == blackstone
					&& Concealments.shown(level, owner, marked, stone) == stone, "...for strangers' eyes alone");
			level.setBlock(marked, air, 3);
			BlockPos tall = center.offset(1, 1, 0);
			level.setBlock(tall, Blocks.TALL_GRASS.defaultBlockState(), 2 | 16); // its neighbours not told: it has no earth under it to stand on
			level.setBlock(tall.above(), Blocks.TALL_GRASS.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 2 | 16);
			LandLedger.begin(stranger);
			try {
				level.setBlock(tall, air, 3);
			} finally {
				LandLedger.end();
			}
			helper.assertTrue(level.getBlockState(tall).is(Blocks.TALL_GRASS) && Concealments.shown(level, stranger, tall, level.getBlockState(tall)).isAir()
					&& Concealments.shown(level, stranger, tall.above(), level.getBlockState(tall.above())).isAir(), "What stands in two goes whole");
			level.setBlock(tall.above(), air, 2 | 16);
			level.setBlock(tall, air, 2 | 16);

			placed(level, stranger, fence, joined);
			helper.assertTrue(veil.strangers.isEmpty() && !Concealments.reachesHidden(owner, fence), "Its owner's fence joining a stranger's block stays its owner's to touch");
			helper.assertTrue(LandLedger.original(level, fence).isAir() && Concealments.shown(level, stranger, fence, joined).isAir(), "...and hidden from the stranger");

			placed(level, stranger, snow, layers);
			helper.assertTrue(veil.strangers.isEmpty() && Concealments.shown(level, owner, snow, layers) == layers, "More of the same is no new block");

			placed(level, stranger, own, Blocks.DIRT.defaultBlockState());
			placed(level, stranger, own, farmland);
			helper.assertTrue(veil.strangers.containsKey(own.asLong()) && Concealments.shown(level, owner, own, farmland).isAir() && Concealments.shown(level, stranger, own, farmland) == farmland,
					"A block of their own they rework stays theirs, and doesn't exist for its owner");
		} finally {
			Concealments.dissolve(level, id);
			for (BlockPos pos : spots) level.setBlock(pos, air, 3);
			forgetAll(helper);
			sweepDrops(helper);
		}
		helper.succeed();
	}

	/** The user's asks, 2026-11-06: a core follows its owner's realm by itself, and "its power shouldnt fall with the owners in case of core detonation or something". */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aCoreRisesWithItsOwnerAndNeverFalls(GameTestHelper helper) throws ReflectiveOperationException {
		ServerLevel level = helper.getLevel();
		BlockPos rel = new BlockPos(4, 2, 4);
		helper.setBlock(rel.below(), Blocks.STONE);
		helper.setBlock(rel, ModBlocks.FORMATION_CORE);
		for (BlockPos vein : new BlockPos[] {rel.east(), rel.west(), rel.north(), rel.south()}) helper.setBlock(vein, ModBlocks.QI_VEIN);
		FormationCoreBlockEntity core = (FormationCoreBlockEntity) level.getBlockEntity(helper.absolutePos(rel));
		ServerPlayer owner = player(helper, "risen-owner", Realm.CORE_FORMATION, Stage.EARLY);
		Method economy = FormationCoreBlockEntity.class.getDeclaredMethod("economy", ServerLevel.class);
		economy.setAccessible(true);
		int low = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), high = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.MID);
		try {
			core.scanNetwork(level);
			helper.assertTrue(core.canControl(owner) && core.getRank() == low, "The first hand claims it, as strong as they are");
			core.configure(owner, 8, false, 8, true);
			Concealments.Veil veil = Concealments.get(level, core.getFormationId());
			helper.assertTrue(veil != null && veil.rank() == low, "Its concealment as strong");

			CultivationManager.get(owner).setState(Realm.NASCENT_SOUL, Stage.MID, 0);
			economy.invoke(core, level);
			helper.assertTrue(core.getRank() == high && veil.rank() == high && Formations.get(level, core.getFormationId()).getRank() == high,
					"Its owner breaks through: barrier and concealment rise by themselves, its screen untouched");

			CultivationManager.get(owner).setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
			economy.invoke(core, level);
			helper.assertTrue(core.getRank() == high && veil.rank() == high, "Its owner's realm falls (a burst golden core): it doesn't");
			core.configure(owner, 8, false, 8, true);
			helper.assertTrue(core.getRank() == high && veil.rank() == high, "...nor when they next set it");
			helper.succeed();
		} finally {
			core.onRemovedFromWorld(level);
			helper.setBlock(rel, Blocks.AIR);
			for (BlockPos vein : new BlockPos[] {rel.east(), rel.west(), rel.north(), rel.south(), rel.below()}) helper.setBlock(vein, Blocks.AIR);
		}
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
