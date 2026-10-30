package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The storage formations (StorageFormations, 2026-10-28, in the Storage Rings' place): a tier's room in whole stacks and the Immortal's
 * without end; set down by a player it is theirs; a stranger below its seal is turned away, one of the seal's realm forces it; its owner
 * mends it, refines it (realm, mastery, cost) and takes it up only empty (one packed before still sets down with all it held); an old
 * ring unfolds into its formation with its space;
 * a treasury's formation holds its riches; nothing breaks one.
 */
public class StorageFormationGameTests implements FabricGameTest {
	private static final BlockPos AT = new BlockPos(2, 2, 2);

	/** A survival player in earnest (a mock player answers that it is creative), of {@code realm} Early (or untouched), by the formation. */
	private static ServerPlayer survivor(GameTestHelper helper, String name, Realm realm) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(AT);
		p.teleportTo(helper.getLevel(), at.getX() + 1.5, at.getY(), at.getZ() + 0.5, 0, 0);
		if (realm != null) CultivationManager.get(p).setState(realm, Stage.EARLY, 0);
		return p;
	}

	private static void leave(ServerPlayer... players) {
		for (ServerPlayer p : players) {
			if (p != null && p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
		}
	}

	private static StorageFormationBlockEntity place(GameTestHelper helper, StorageFormations.Tier tier, ServerPlayer owner, ItemStack from) {
		helper.setBlock(AT, tier.block());
		StorageFormationBlockEntity formation = (StorageFormationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(AT));
		formation.placedBy(owner, from);
		return formation;
	}

	/** {@code player} right-clicks the formation's block with an empty hand, sneaking (taking it up) or not. */
	private static void click(GameTestHelper helper, ServerPlayer player, boolean sneaking) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(AT);
		player.setShiftKeyDown(sneaking);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tiersHoldStacksAndTheImmortalWithoutEnd(GameTestHelper helper) {
		StorageSpaces.Space space = new StorageSpaces.Space();
		int moved = 0;
		for (int i = 0; i < 30; i++) moved += space.insert(new ItemStack(Items.COBBLESTONE, 64), StorageFormations.Tier.LOW.stacks());
		helper.assertTrue(moved == 27 * 64 && space.stacksUsed() == 27, "A Low formation holds 27 stacks: " + moved);
		helper.assertTrue(space.insert(new ItemStack(Items.DIRT), StorageFormations.Tier.LOW.stacks()) == 0, "...and nothing more of another kind");
		helper.assertTrue(StorageFormations.Tier.MID.stacks() == 54 && StorageFormations.Tier.HIGH.stacks() == 81
				&& StorageFormations.Tier.SUPREME.stacks() == 108 && StorageFormations.Tier.IMMORTAL.boundless(), "27, 54, 81, 108 and without end");

		StorageSpaces.Space kinds = new StorageSpaces.Space();
		kinds.insert(new ItemStack(Items.DIRT, 1), StorageFormations.Tier.LOW.stacks());
		kinds.insert(new ItemStack(Items.ENDER_PEARL, 17), StorageFormations.Tier.LOW.stacks());
		helper.assertTrue(kinds.stacksUsed() == 3, "Each kind takes whole stacks of its own size (16 pearls a stack)");
		ItemStack pearls = kinds.extract(new ItemStack(Items.ENDER_PEARL), 16);
		helper.assertTrue(pearls.is(Items.ENDER_PEARL) && pearls.getCount() == 16 && kinds.count(new ItemStack(Items.ENDER_PEARL)) == 1, "Taken out");

		StorageSpaces.Space boundless = new StorageSpaces.Space();
		for (int i = 0; i < 2000; i++) boundless.insert(new ItemStack(Items.COBBLESTONE, 64), StorageFormations.Tier.IMMORTAL.stacks());
		int kindsIn = 0;
		for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (kindsIn >= 400 || item == Items.AIR || item == Items.COBBLESTONE) continue;
			if (boundless.insert(new ItemStack(item), StorageFormations.Tier.IMMORTAL.stacks()) > 0) kindsIn++;
		}
		helper.assertTrue(boundless.count(new ItemStack(Items.COBBLESTONE)) == 2000L * 64 && boundless.entries().size() == 401,
				"The Immortal formation holds any number of any number of kinds");

		StorageSpaces data = new StorageSpaces();
		UUID id = UUID.randomUUID();
		data.space(id).insert(new ItemStack(Items.DIAMOND, 40), StorageFormations.Tier.MID.stacks());
		helper.assertTrue(StorageSpaces.load(data.save(new CompoundTag())).space(id).count(new ItemStack(Items.DIAMOND)) == 40,
				"A space is saved with the world, by its id");
		helper.assertTrue(StorageFormations.Tier.LOW.seal() == Realm.FOUNDATION_BUILDING && StorageFormations.Tier.IMMORTAL.seal() == Realm.TRUE_IMMORTAL,
				"Seals from Foundation Building to True Immortal");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSealTurnsTheWeakAwayAndGivesToTheForcing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = survivor(helper, "test-seal-owner", Realm.CORE_FORMATION);
		ServerPlayer weak = survivor(helper, "test-seal-weak", Realm.QI_REFINING);
		ServerPlayer strong = survivor(helper, "test-seal-strong", Realm.FOUNDATION_BUILDING);
		try {
			StorageFormationBlockEntity formation = place(helper, StorageFormations.Tier.LOW, owner, ItemStack.EMPTY);
			helper.assertTrue(owner.getUUID().equals(formation.getOwner()), "Set down, it is its setter's");
			StorageFormations.use(owner, formation);
			helper.assertTrue(owner.containerMenu instanceof StorageFormationMenu && formation.spaceId() != null, "Its owner opens it: its space made");
			owner.closeContainer();

			StorageFormations.use(weak, formation);
			helper.assertTrue(!(weak.containerMenu instanceof StorageFormationMenu) && formation.forcer() == null, "A Qi Refining stranger is turned away");

			StorageFormations.use(strong, formation);
			helper.assertTrue(strong.getUUID().equals(formation.forcer()) && !(strong.containerMenu instanceof StorageFormationMenu),
					"One of the seal's realm sets to forcing it");
			BlockPos pos = helper.absolutePos(AT);
			for (int i = 0; i < StorageFormations.Tier.LOW.forceSeconds() * 20 - 1; i++) {
				StorageFormationBlockEntity.serverTick(level, pos, level.getBlockState(pos), formation);
			}
			helper.assertTrue(!formation.isBroken() && formation.forcePercent() >= 99, "Not a tick early");
			StorageFormationBlockEntity.serverTick(level, pos, level.getBlockState(pos), formation);
			helper.assertTrue(formation.isBroken() && formation.forcer() == null, "Stayed by long enough, the seal breaks");
			StorageFormations.use(strong, formation);
			helper.assertTrue(strong.containerMenu instanceof StorageFormationMenu, "A broken seal opens to them");
			strong.closeContainer();
			StorageFormations.use(owner, formation);
			helper.assertTrue(!formation.isBroken(), "Its owner opening it mends the seal");
			owner.closeContainer();

			StorageFormations.use(strong, formation);
			strong.teleportTo(level, pos.getX() + 20.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
			StorageFormationBlockEntity.serverTick(level, pos, level.getBlockState(pos), formation);
			helper.assertTrue(formation.forcer() == null && !formation.isBroken(), "Gone from it, the forcing is given up");

			formation.trust(weak.getUUID(), "test-seal-weak");
			StorageFormations.use(weak, formation);
			helper.assertTrue(weak.containerMenu instanceof StorageFormationMenu, "One it trusts opens it, whatever their realm");
			weak.closeContainer();

			helper.assertTrue(level.getBlockState(pos).getDestroySpeed(level, pos) < 0 && level.getBlockState(pos).getBlock().getExplosionResistance() > 1_000_000,
					"Nothing breaks it: no pickaxe, no blast");
			helper.succeed();
		} finally {
			leave(owner, weak, strong);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void takenUpOnlyEmptyNeverCarryingAnything(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = survivor(helper, "test-take-owner", Realm.CORE_FORMATION);
		ServerPlayer stranger = survivor(helper, "test-take-stranger", Realm.TRUE_IMMORTAL);
		try {
			StorageFormationBlockEntity formation = place(helper, StorageFormations.Tier.MID, owner, ItemStack.EMPTY);
			formation.ensureSpace(level);
			StorageSpaces.Space space = StorageSpaces.forServer(level.getServer()).space(formation.spaceId());
			space.insert(new ItemStack(Items.EMERALD, 33), StorageFormations.Tier.MID.stacks());

			BlockPos pos = helper.absolutePos(AT);
			click(helper, stranger, true);
			helper.assertTrue(level.getBlockState(pos).getBlock() instanceof StorageFormationBlock, "No one else takes it up, however strong");
			click(helper, owner, true);
			helper.assertTrue(level.getBlockState(pos).getBlock() instanceof StorageFormationBlock && owner.getMainHandItem().isEmpty(),
					"Holding anything, it stays where it is: not even its owner carries it off");
			space.extract(new ItemStack(Items.EMERALD), 33);
			click(helper, owner, true);
			ItemStack taken = owner.getMainHandItem();
			helper.assertTrue(level.getBlockState(pos).isAir() && taken.getItem() instanceof StorageFormationItem item && item.tier() == StorageFormations.Tier.MID
					&& StorageFormationItem.spaceOf(taken) == null, "Empty, its owner takes it up: a plain formation of its tier");

			// Treasures the world laid up in one (a treasury's, never opened) are something held too.
			StorageFormationBlockEntity treasury = place(helper, StorageFormations.Tier.MID, owner, ItemStack.EMPTY);
			treasury.layUp(new net.minecraft.resources.ResourceLocation("minecraft", "chests/simple_dungeon"), 1L);
			helper.assertFalse(treasury.holdsNothing(level.getServer()), "Treasures laid up in it, it isn't empty");
			level.removeBlock(pos, false);

			// One packed up with its things before formations stayed put still sets down with all it held.
			UUID old = UUID.randomUUID();
			StorageSpaces.forServer(level.getServer()).space(old).insert(new ItemStack(Items.DIAMOND, 5), StorageFormations.Tier.MID.stacks());
			ItemStack packed = StorageFormationItem.of(StorageFormations.Tier.MID);
			StorageFormationItem.setSpace(packed, old);
			helper.assertTrue(!StorageSpaces.mayHold(packed), "A packed formation goes into no space");
			StorageFormationBlockEntity again = place(helper, StorageFormations.Tier.MID, owner, packed);
			helper.assertTrue(old.equals(again.spaceId())
					&& StorageSpaces.forServer(level.getServer()).space(again.spaceId()).count(new ItemStack(Items.DIAMOND)) == 5, "Set down, all it held is there");
			click(helper, owner, true);
			helper.assertTrue(level.getBlockState(pos).getBlock() instanceof StorageFormationBlock, "...and there it stays while it holds them");
			helper.succeed();
		} finally {
			leave(owner, stranger);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void refiningWantsTheRealmTheMasteryAndTheCost(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = survivor(helper, "test-refine-owner", Realm.FOUNDATION_BUILDING);
		ServerPlayer other = survivor(helper, "test-refine-other", Realm.TRUE_IMMORTAL);
		try {
			StorageFormationBlockEntity formation = place(helper, StorageFormations.Tier.LOW, owner, ItemStack.EMPTY);
			formation.ensureSpace(level);
			UUID space = formation.spaceId();
			helper.assertTrue(StorageFormations.refineRefusal(owner, formation) != null, "A Mid seal wants Core Formation");
			CultivationManager.get(owner).setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
			helper.assertTrue(StorageFormations.refineRefusal(owner, formation) != null, "...and its cost");
			for (ItemStack cost : StorageFormations.Tier.MID.cost()) owner.getInventory().add(cost.copy());
			helper.assertTrue(StorageFormations.refineRefusal(other, formation) != null, "Only its owner refines it");
			helper.assertTrue(StorageFormations.refineRefusal(owner, formation) == null && StorageFormations.refine(owner, formation), "Refined");
			StorageFormationBlockEntity refined = (StorageFormationBlockEntity) level.getBlockEntity(helper.absolutePos(AT));
			helper.assertTrue(refined.tier() == StorageFormations.Tier.MID && space.equals(refined.spaceId()) && owner.getUUID().equals(refined.getOwner()),
					"In place: the next tier, its space and owner kept");
			helper.assertTrue(owner.getInventory().countItem(ModItems.MID_SPIRIT_STONE) == 0 && owner.getInventory().countItem(ModItems.JADE) == 0,
					"The cost is taken");
			helper.assertTrue(CultivationManager.own(owner).getMastery(CraftMastery.Craft.FORMATIONS) == StorageFormations.Tier.MID.practice(),
					"Refining teaches formations");
			helper.assertTrue(StorageFormations.Tier.SUPREME.mastery() == CraftMastery.Rank.ADEPT && StorageFormations.Tier.IMMORTAL.mastery() == CraftMastery.Rank.MASTER,
					"Supreme and Immortal want mastery of formations");
			helper.succeed();
		} finally {
			leave(owner, other);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anOldRingUnfoldsIntoItsFormation(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		UUID id = UUID.randomUUID();
		StorageSpaces.forServer(level.getServer()).space(id).insert(new ItemStack(Items.GOLD_INGOT, 70), 0);
		ItemStack ring = new ItemStack(ModItems.STORAGE_RING);
		ring.getOrCreateTag().putUUID("StorageRing", id);
		ring.getOrCreateTag().putInt("StorageGrade", 2); // an Earth ring: 108 stacks
		ItemStack unfolded = StorageFormations.unfold(ring);
		helper.assertTrue(unfolded.getItem() instanceof StorageFormationItem item && item.tier() == StorageFormations.Tier.SUPREME
				&& id.equals(StorageFormationItem.spaceOf(unfolded)), "An Earth ring becomes a Supreme formation holding its very space");
		helper.assertTrue(StorageFormations.tierForRing(0) == StorageFormations.Tier.LOW && StorageFormations.tierForRing(1) == StorageFormations.Tier.MID
				&& StorageFormations.tierForRing(3) == StorageFormations.Tier.IMMORTAL && StorageFormations.tierForRing(4) == StorageFormations.Tier.IMMORTAL,
				"Each ring's room kept or bettered");
		ServerPlayer player = survivor(helper, "test-ring-keeper", null);
		try {
			player.getInventory().setItem(5, ring.copy());
			ItemStack worn = ring.copy(); // a ring of its own (one space between two rings would stack their formations in one slot)
			worn.getOrCreateTag().putUUID("StorageRing", UUID.randomUUID());
			RingContainer.of(player).setItem(0, worn);
			player.getEnderChestInventory().setItem(3, new ItemStack(ModItems.STORAGE_RING));
			helper.assertTrue(StorageFormations.convertRings(player) == 3, "Carried, in the pack, on a finger or in the ender chest, each unfolds");
			int formations = 0;
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				if (player.getInventory().getItem(i).getItem() instanceof StorageFormationItem) formations++;
			}
			helper.assertTrue(formations == 2 && RingContainer.of(player).getItem(0).isEmpty()
					&& player.getEnderChestInventory().getItem(3).getItem() == StorageFormations.Tier.LOW.block().asItem(),
					"The one on a finger went to the pack: a formation isn't worn");
			helper.succeed();
		} finally {
			leave(player);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTreasuryKeepsItsRichesInAFormation(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(AT);
		StorageFormations.treasury(level, pos, SectPiece.TREASURY_LOOT, 42L, new ItemStack(Items.NETHER_STAR));
		helper.assertTrue(level.getBlockEntity(pos) instanceof StorageFormationBlockEntity raised && raised.getOwner() == null
				&& raised.tier() == StorageFormations.treasuryTier(level.dimension()), "A treasury's formation of the place's tier, no one's");
		StorageFormationBlockEntity formation = (StorageFormationBlockEntity) level.getBlockEntity(pos);
		formation.ensureSpace(level);
		StorageSpaces.Space space = StorageSpaces.forServer(level.getServer()).space(formation.spaceId());
		helper.assertTrue(space.count(new ItemStack(Items.NETHER_STAR)) == 1 && space.entries().size() > 1,
				"Opened, its riches are rolled in, what was laid up with them");
		int kinds = space.entries().size();
		formation.ensureSpace(level);
		helper.assertTrue(space.entries().size() == kinds, "...once");
		helper.assertTrue(StorageFormations.treasuryTier(ModDimensions.IMMORTAL_REALM) == StorageFormations.Tier.IMMORTAL
				&& StorageFormations.treasuryTier(net.minecraft.world.level.Level.OVERWORLD) == StorageFormations.Tier.MID,
				"The richer the realm, the stronger its treasuries' seals");
		helper.succeed();
	}
}
