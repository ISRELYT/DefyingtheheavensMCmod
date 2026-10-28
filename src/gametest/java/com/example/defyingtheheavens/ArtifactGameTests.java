package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Artifact Forge (2026-10-28): its workings, one for every kind at every grade and none alike; what it takes and refuses; a working
 * begun by hand, fed its forger's qi, standing still while they are away, and teaching them; Fine and Perfect work by rank; each
 * artifact's power (a flying sword's cheaper flight, a spear's pierce, a glaive's sweep, a banner's element, a gourd's qi, a jade's
 * turned harm); its saving; and its price.
 */
public class ArtifactGameTests implements FabricGameTest {
	private static final BlockPos FORGE = new BlockPos(2, 2, 2);

	/** A player who isn't creative (a mock player says it is), of {@code realm}, standing by the forge with a full pool. */
	private static ServerPlayer survivor(GameTestHelper helper, String name, Realm realm) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(FORGE);
		p.teleportTo(helper.getLevel(), at.getX() + 1.5, at.getY(), at.getZ() + 0.5, 0, 0);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		return p;
	}

	private static void leave(ServerPlayer... players) {
		for (ServerPlayer p : players) {
			if (p != null && p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
		}
	}

	private static ItemStack[] mortalSword() {
		return new ItemStack[] {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.STICK), new ItemStack(ModItems.LOW_SPIRIT_STONE),
				new ItemStack(Items.IRON_INGOT), new ItemStack(ModItems.LOW_SPIRIT_STONE)};
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyKindAtEveryGradeHasOneWorking(GameTestHelper helper) {
		List<ArtifactRecipes.Recipe> all = ArtifactRecipes.all();
		int forged = 0;
		for (ArtifactKind kind : ArtifactKind.values()) for (ArtifactGrade grade : ArtifactGrade.values()) if (kind.has(grade)) forged++;
		helper.assertTrue(all.size() == forged && forged == 7 * 5 + 5 * 4, "A working for every kind at every grade it is forged at");
		Set<Item> outputs = new HashSet<>();
		for (ArtifactRecipes.Recipe recipe : all) {
			helper.assertTrue(outputs.add(recipe.output()), recipe.output() + " has one working");
			helper.assertTrue(ArtifactRecipes.forOutput(recipe.output()) == recipe, "Found by what it forges");
			int count = recipe.materials().values().stream().mapToInt(Integer::intValue).sum() + (recipe.core() ? 1 : 0) + (recipe.token() ? 1 : 0);
			helper.assertTrue(count <= ArtifactRecipes.MAX_MATERIALS, recipe.output() + " fits in the forge");
			helper.assertTrue(recipe.materials().getOrDefault(recipe.grade().stone(), 0) == 2, recipe.output() + " takes two of its grade's stones");
			helper.assertTrue(recipe.core() == (recipe.grade() != ArtifactGrade.MORTAL), "A demon core from Spirit grade up");
			helper.assertTrue(recipe.token() == (recipe.kind() == ArtifactKind.BANNER && recipe.grade() == ArtifactGrade.MORTAL),
					"Only a Mortal banner takes a token");
			for (ArtifactRecipes.Recipe other : all) {
				helper.assertTrue(other == recipe || !(other.materials().equals(recipe.materials()) && other.core() == recipe.core()
						&& other.token() == recipe.token()), recipe.output() + " and " + other.output() + " are made alike");
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theForgeTakesOnlyWhatFits(GameTestHelper helper) {
		List<ItemStack> in = new ArrayList<>();
		helper.assertFalse(ArtifactRecipes.canAdd(in, new ItemStack(Items.DIRT)), "Dirt fits nothing");
		for (ItemStack stack : mortalSword()) {
			helper.assertTrue(ArtifactRecipes.canAdd(in, stack), stack + " fits");
			in.add(stack.copyWithCount(1));
		}
		ArtifactRecipes.Recipe sword = ArtifactRecipes.match(in);
		helper.assertTrue(sword != null && sword.kind() == ArtifactKind.FLYING_SWORD && sword.grade() == ArtifactGrade.MORTAL, "A Mortal flying sword's materials");
		helper.assertTrue(ArtifactRecipes.canAdd(in, new ItemStack(Items.STICK)), "A second stick still fits: a glaive's");
		in.add(new ItemStack(Items.STICK));
		ArtifactRecipes.Recipe glaive = ArtifactRecipes.match(in);
		helper.assertTrue(glaive != null && glaive.kind() == ArtifactKind.GLAIVE, "...and makes it a glaive");
		helper.assertFalse(ArtifactRecipes.canAdd(in, new ItemStack(Items.STICK)), "A third doesn't");
		helper.assertFalse(ArtifactRecipes.canAdd(in, new ItemStack(Items.GOLD_INGOT)), "Nor another grade's metal");

		// A Spirit sword wants a core of Qi Refining or above; an Earth one Core Formation's.
		List<ItemStack> spirit = new ArrayList<>(List.of(new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.STICK),
				new ItemStack(ModItems.MID_SPIRIT_STONE), new ItemStack(ModItems.MID_SPIRIT_STONE)));
		ItemStack qrCore = DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), QiElement.FIRE, null);
		helper.assertTrue(ArtifactRecipes.match(spirit) == null, "Without its core, no working");
		spirit.add(qrCore);
		helper.assertTrue(ArtifactRecipes.match(spirit) != null && ArtifactRecipes.match(spirit).grade() == ArtifactGrade.SPIRIT, "With it, a Spirit sword");
		List<ItemStack> earth = new ArrayList<>(List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND), new ItemStack(Items.STICK),
				new ItemStack(ModItems.HIGH_SPIRIT_STONE), new ItemStack(ModItems.HIGH_SPIRIT_STONE)));
		helper.assertFalse(ArtifactRecipes.canAdd(earth, qrCore), "A Qi Refining core is too weak for Earth grade");
		helper.assertTrue(ArtifactRecipes.canAdd(earth, DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null)),
				"A Core Formation core is not");

		// A Mortal banner takes its element from a token, a Spirit one from its core.
		List<ItemStack> banner = new ArrayList<>(List.of(new ItemStack(Items.WHITE_WOOL), new ItemStack(Items.WHITE_WOOL), new ItemStack(Items.STICK),
				new ItemStack(ModItems.LOW_SPIRIT_STONE), new ItemStack(ModItems.LOW_SPIRIT_STONE), new ItemStack(Items.PRISMARINE_SHARD)));
		helper.assertTrue(ArtifactRecipes.match(banner) != null && ArtifactRecipes.element(banner) == QiElement.WATER, "A prismarine shard: a water banner");
		helper.assertTrue(ArtifactRecipes.element(spirit) == QiElement.FIRE, "A fire beast's core: fire");
		helper.succeed();
	}

	/**
	 * A forging from first to last: the materials in, begun by an empty hand over magma (half again as fast), its forger's qi drawn,
	 * standing still while they are away, the sword out and its forger taught.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aForgingIsFedAndTeaches(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(FORGE.below(), Blocks.MAGMA_BLOCK);
		helper.setBlock(FORGE, ModBlocks.ARTIFACT_FORGE);
		BlockPos pos = helper.absolutePos(FORGE);
		ArtifactForgeBlockEntity forge = (ArtifactForgeBlockEntity) level.getBlockEntity(pos);
		ServerPlayer smith = survivor(helper, "test-forge-smith", Realm.FOUNDATION_BUILDING);
		try {
			PlayerCultivation c = CultivationManager.forPlayer(smith);
			helper.assertTrue(ArtifactForgeBlockEntity.heat(level, pos) == 1.5, "Magma beneath: the hotter heat");
			smith.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
			forge.interact(level.getBlockState(pos), smith, InteractionHand.MAIN_HAND);
			helper.assertTrue(forge.contents().isEmpty() && smith.getMainHandItem().getCount() == 1, "Dirt is refused, and kept");
			for (ItemStack stack : mortalSword()) {
				smith.setItemInHand(InteractionHand.MAIN_HAND, stack);
				forge.interact(level.getBlockState(pos), smith, InteractionHand.MAIN_HAND);
				helper.assertTrue(smith.getMainHandItem().isEmpty(), stack + " goes in");
			}
			helper.assertFalse(forge.isWorking(), "All in, it waits to be begun");
			forge.interact(level.getBlockState(pos), smith, InteractionHand.MAIN_HAND);
			helper.assertTrue(forge.isWorking() && smith.getUUID().equals(forge.forger()) && level.getBlockState(pos).getValue(ArtifactForgeBlock.WORKING),
					"An empty hand begins it: its forger the one who did");
			int practice = CultivationManager.own(smith).getMastery(CraftMastery.Craft.FORGING);
			double qi = c.getQi();
			for (int i = 0; i < 60; i++) ArtifactForgeBlockEntity.serverTick(level, pos, level.getBlockState(pos), forge);
			int done = forge.progressPercent();
			helper.assertTrue(done > 0 && c.getQi() < qi, "It goes on, fed its forger's qi");

			smith.teleportTo(level, pos.getX() + 20.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
			for (int i = 0; i < 40; i++) ArtifactForgeBlockEntity.serverTick(level, pos, level.getBlockState(pos), forge);
			helper.assertTrue(forge.progressPercent() == done, "Its forger away, it stands still");
			smith.teleportTo(level, pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5, 0, 0);

			int ticks = (int) Math.ceil(ArtifactGrade.MORTAL.seconds() * 20 / 1.5) + 5;
			for (int i = 0; i < ticks && forge.isWorking(); i++) ArtifactForgeBlockEntity.serverTick(level, pos, level.getBlockState(pos), forge);
			helper.assertFalse(forge.isWorking(), "Done in its grade's time over the hotter heat");
			List<ItemEntity> out = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2),
					e -> e.getItem().is(ModItems.artifact(ArtifactKind.FLYING_SWORD, ArtifactGrade.MORTAL)));
			helper.assertTrue(out.size() == 1 && Artifacts.quality(out.get(0).getItem()) == Artifacts.Quality.NORMAL,
					"A Mortal flying sword comes out (an apprentice's: Normal work)");
			helper.assertTrue(forge.contents().isEmpty(), "Its materials spent");
			helper.assertTrue(CultivationManager.own(smith).getMastery(CraftMastery.Craft.FORGING) == practice + ArtifactGrade.MORTAL.practice(),
					"The forging taught its forger");
			double spent = qi - c.getQi();
			double expected = ArtifactGrade.MORTAL.seconds() / 1.5 * ArtifactGrade.MORTAL.qiPerSecond();
			helper.assertTrue(Math.abs(spent - expected) < 1.0, "Its grade's qi a second, " + expected + " in all, not " + spent);
			out.forEach(ItemEntity::discard);
		} finally {
			leave(smith);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aWorkingIsSavedAndEmptied(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(FORGE.below(), Blocks.CAMPFIRE);
		helper.setBlock(FORGE, ModBlocks.ARTIFACT_FORGE);
		BlockPos pos = helper.absolutePos(FORGE);
		ArtifactForgeBlockEntity forge = (ArtifactForgeBlockEntity) level.getBlockEntity(pos);
		ServerPlayer smith = survivor(helper, "test-forge-saver", Realm.QI_REFINING);
		try {
			helper.assertTrue(ArtifactForgeBlockEntity.heat(level, pos) == 1.0, "A campfire: plain heat");
			for (ItemStack stack : mortalSword()) {
				smith.setItemInHand(InteractionHand.MAIN_HAND, stack);
				forge.interact(level.getBlockState(pos), smith, InteractionHand.MAIN_HAND);
			}
			forge.interact(level.getBlockState(pos), smith, InteractionHand.MAIN_HAND);
			for (int i = 0; i < 30; i++) ArtifactForgeBlockEntity.serverTick(level, pos, level.getBlockState(pos), forge);
			CompoundTag saved = forge.saveWithoutMetadata();
			ArtifactForgeBlockEntity copy = new ArtifactForgeBlockEntity(pos, level.getBlockState(pos));
			copy.load(saved);
			helper.assertTrue(copy.isWorking() && copy.working().kind() == ArtifactKind.FLYING_SWORD && copy.contents().size() == 5
					&& copy.progressPercent() == forge.progressPercent() && smith.getUUID().equals(copy.forger()), "Its working is saved");

			smith.setShiftKeyDown(true);
			forge.interact(level.getBlockState(pos), smith, InteractionHand.MAIN_HAND);
			smith.setShiftKeyDown(false);
			helper.assertTrue(!forge.isWorking() && forge.contents().isEmpty(), "Sneaking with an empty hand takes it all back, stopping the working");
			List<ItemEntity> back = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
			helper.assertTrue(back.stream().mapToInt(e -> e.getItem().getCount()).sum() == 5, "...every material");
			back.forEach(ItemEntity::discard);
		} finally {
			leave(smith);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void finerWorkComesWithRank(GameTestHelper helper) {
		helper.assertTrue(ArtifactForgeBlockEntity.rollQuality(CraftMastery.Rank.APPRENTICE, 0.0) == Artifacts.Quality.NORMAL, "An apprentice: Normal work");
		helper.assertTrue(ArtifactForgeBlockEntity.rollQuality(CraftMastery.Rank.ADEPT, 0.05) == Artifacts.Quality.FINE
				&& ArtifactForgeBlockEntity.rollQuality(CraftMastery.Rank.ADEPT, 0.11) == Artifacts.Quality.NORMAL, "An adept: Fine work one time in ten");
		helper.assertTrue(ArtifactForgeBlockEntity.rollQuality(CraftMastery.Rank.MASTER, 0.04) == Artifacts.Quality.PERFECT
				&& ArtifactForgeBlockEntity.rollQuality(CraftMastery.Rank.MASTER, 0.2) == Artifacts.Quality.FINE
				&& ArtifactForgeBlockEntity.rollQuality(CraftMastery.Rank.MASTER, 0.26) == Artifacts.Quality.NORMAL, "A master: Perfect from now on");
		helper.assertTrue(CraftMastery.failChance(CraftMastery.Rank.APPRENTICE, ArtifactGrade.MORTAL.difficulty()) == 0
				&& CraftMastery.failChance(CraftMastery.Rank.SAINT, ArtifactGrade.IMMORTAL.difficulty()) == 0
				&& CraftMastery.failChance(CraftMastery.Rank.GRANDMASTER, ArtifactGrade.IMMORTAL.difficulty()) > 0, "Mortal never fails; Immortal fails all but a Saint");

		// A Perfect blade's blow is written onto it, a quarter more.
		ItemStack perfect = Artifacts.create(ArtifactKind.FLYING_SWORD, ArtifactGrade.SPIRIT, Artifacts.Quality.PERFECT, null);
		ItemStack normal = Artifacts.create(ArtifactKind.FLYING_SWORD, ArtifactGrade.SPIRIT, Artifacts.Quality.NORMAL, null);
		helper.assertTrue(Artifacts.quality(perfect) == Artifacts.Quality.PERFECT && Artifacts.quality(normal) == Artifacts.Quality.NORMAL, "Its work is kept");
		helper.assertTrue(blow(perfect) > blow(normal) * 1.2 && blow(normal) > 0, "A Perfect blade strikes a quarter harder: " + blow(perfect) + " to " + blow(normal));
		helper.succeed();
	}

	/** The added blow and might of {@code stack} in a main hand. */
	private static double blow(ItemStack stack) {
		double total = 0;
		for (Map.Entry<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> entry : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).entries()) {
			if (entry.getKey() == Attributes.ATTACK_DAMAGE && entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION) total += entry.getValue().getAmount();
		}
		return total;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aFlyingSwordSavesFlightAndBannersStrengthen(GameTestHelper helper) {
		helper.assertTrue(Math.abs(ArtifactWeaponItem.flightShare(Artifacts.create(ArtifactKind.FLYING_SWORD, ArtifactGrade.MORTAL, Artifacts.Quality.NORMAL, null)) - 0.9) < 1e-9,
				"A Mortal flying sword: a tenth off Sword Flight");
		helper.assertTrue(Math.abs(ArtifactWeaponItem.flightShare(Artifacts.create(ArtifactKind.FLYING_SWORD, ArtifactGrade.IMMORTAL, Artifacts.Quality.PERFECT, null)) - 0.375) < 1e-9,
				"A Perfect Immortal one: 62.5% off");
		helper.assertTrue(ArtifactWeaponItem.flightShare(new ItemStack(Items.IRON_SWORD)) == 1
				&& ArtifactWeaponItem.flightShare(Artifacts.create(ArtifactKind.SABER, ArtifactGrade.HEAVEN, Artifacts.Quality.NORMAL, null)) == 1, "Any other blade: none");

		ServerPlayer holder = survivor(helper, "test-banner-holder", Realm.QI_REFINING);
		try {
			holder.setItemInHand(InteractionHand.OFF_HAND, Artifacts.create(ArtifactKind.BANNER, ArtifactGrade.EARTH, Artifacts.Quality.NORMAL, QiElement.FIRE));
			helper.assertTrue(Math.abs(ElementBannerItem.strength(holder, QiElement.FIRE) - 1.12f) < 1e-5, "An Earth fire banner in the off hand: fire 12% harder");
			helper.assertTrue(ElementBannerItem.strength(holder, QiElement.WATER) == 1f && ElementBannerItem.strength(holder, null) == 1f, "Water: as before");
			holder.setItemInHand(InteractionHand.MAIN_HAND, Artifacts.create(ArtifactKind.BANNER, ArtifactGrade.IMMORTAL, Artifacts.Quality.NORMAL, QiElement.FIRE));
			helper.assertTrue(Math.abs(ElementBannerItem.strength(holder, QiElement.FIRE) - 1.2f) < 1e-5, "Two banners: the better holds, not both");
		} finally {
			leave(holder);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGourdKeepsQi(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, "test-gourd", Realm.CORE_FORMATION);
		try {
			PlayerCultivation c = CultivationManager.forPlayer(p);
			ItemStack gourd = Artifacts.create(ArtifactKind.GOURD, ArtifactGrade.MORTAL, Artifacts.Quality.NORMAL, null);
			double capacity = QiGourdItem.capacity(gourd), pool = c.getQi();
			helper.assertTrue(capacity == 100 && pool > capacity, "A Mortal gourd holds 100");
			QiGourdItem.pour(p, gourd);
			helper.assertTrue(QiGourdItem.held(gourd) == capacity && Math.abs(c.getQi() - (pool - capacity)) < 1e-6, "Poured full, from the pool");
			QiGourdItem.drink(p, gourd);
			helper.assertTrue(QiGourdItem.held(gourd) == 0 && Math.abs(c.getQi() - pool) < 1e-6, "Drunk back, every drop");
			helper.assertTrue(QiGourdItem.capacity(Artifacts.create(ArtifactKind.GOURD, ArtifactGrade.MORTAL, Artifacts.Quality.FINE, null)) > capacity
					&& QiGourdItem.capacity(Artifacts.create(ArtifactKind.GOURD, ArtifactGrade.IMMORTAL, Artifacts.Quality.NORMAL, null)) == 60000,
					"Fine work holds more, an Immortal gourd 60,000");
		} finally {
			leave(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aJadeTurnsHarmIntoQi(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, "test-jade", Realm.FOUNDATION_BUILDING);
		try {
			PlayerCultivation c = CultivationManager.forPlayer(p);
			helper.assertTrue(ProtectiveJadeItem.incoming(p, p.damageSources().generic(), 10) == 10, "No jade worn: the harm whole");
			RingContainer.of(p).setItem(0, Artifacts.create(ArtifactKind.JADE, ArtifactGrade.HEAVEN, Artifacts.Quality.NORMAL, null));
			double pool = c.getQi();
			float taken = ProtectiveJadeItem.incoming(p, p.damageSources().generic(), 10);
			helper.assertTrue(Math.abs(taken - 7.5f) < 1e-4, "A Heaven jade turns a quarter: " + taken);
			double perPoint = c.maxQi() / p.getMaxHealth() * ProtectiveJadeItem.QI_PER_HEALTH;
			helper.assertTrue(Math.abs(pool - c.getQi() - 2.5 * perPoint) < 1e-6, "...paid from the pool");
			helper.assertTrue(ProtectiveJadeItem.incoming(p, p.damageSources().fellOutOfWorld(), 10) == 10, "The void it never turns");
			c.setQi(0);
			helper.assertTrue(ProtectiveJadeItem.incoming(p, p.damageSources().generic(), 10) == 10, "No qi, no turning");
			RingContainer.of(p).setItem(0, ItemStack.EMPTY);
		} finally {
			leave(p);
		}
		helper.succeed();
	}

	/** {@code who} stood at {@code at} (in the test's frame), facing +x, head and all (a look is the head's). */
	private static void stand(GameTestHelper helper, ServerPlayer who, double x, double y, double z) {
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		who.teleportTo(helper.getLevel(), origin.getX() + x, origin.getY() + y, origin.getZ() + z, -90, 0);
		who.setYRot(-90);
		who.setYHeadRot(-90);
		who.setXRot(0);
	}

	/** A spear's blow runs on into the foe behind its target, never one aside. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aSpearPierces(GameTestHelper helper) {
		ServerPlayer lancer = survivor(helper, "test-lancer", Realm.QI_REFINING);
		List<Husk> husks = new ArrayList<>();
		try {
			stand(helper, lancer, 0.5, 2, 1.5);
			ItemStack spear = Artifacts.create(ArtifactKind.SPEAR, ArtifactGrade.MORTAL, Artifacts.Quality.NORMAL, null);
			lancer.setItemInHand(InteractionHand.MAIN_HAND, spear);
			Husk target = husk(helper, 2.0, 2, 1.5, husks);
			Husk behind = husk(helper, 3.5, 2, 1.5, husks);
			Husk aside = husk(helper, 0.5, 2, 4.5, husks);
			spear.getItem().hurtEnemy(spear, target, lancer);
			helper.assertTrue(behind.getHealth() < behind.getMaxHealth(), "The spear pierces into the one behind");
			helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not one aside");
			helper.assertFalse(Artifacts.mayStrike(lancer, lancer), "Never its wielder");
		} finally {
			husks.forEach(Husk::discard);
			leave(lancer);
		}
		helper.succeed();
	}

	/** A glaive's blow takes in everyone round its wielder, behind them too, but none beyond its reach. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aGlaiveSweeps(GameTestHelper helper) {
		ServerPlayer reaper = survivor(helper, "test-reaper", Realm.QI_REFINING);
		List<Husk> husks = new ArrayList<>();
		try {
			stand(helper, reaper, 2.5, 2, 1.5);
			ItemStack glaive = Artifacts.create(ArtifactKind.GLAIVE, ArtifactGrade.MORTAL, Artifacts.Quality.NORMAL, null);
			reaper.setItemInHand(InteractionHand.MAIN_HAND, glaive);
			Husk front = husk(helper, 4.0, 2, 1.5, husks);
			Husk back = husk(helper, 1.0, 2, 1.5, husks);
			Husk out = husk(helper, 2.5, 2, 6.0, husks);
			glaive.getItem().hurtEnemy(glaive, front, reaper);
			helper.assertTrue(back.getHealth() < back.getMaxHealth(), "The glaive sweeps the one behind its wielder");
			helper.assertTrue(out.getHealth() == out.getMaxHealth(), "...not one beyond its reach");
		} finally {
			husks.forEach(Husk::discard);
			leave(reaper);
		}
		helper.succeed();
	}

	/** A still husk at ({@code x}, {@code y}, {@code z}) in the test's frame. */
	private static Husk husk(GameTestHelper helper, double x, double y, double z, List<Husk> spawned) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		Husk husk = EntityType.HUSK.create(level);
		husk.moveTo(origin.getX() + x, origin.getY() + y, origin.getZ() + z, 0, 0);
		husk.setNoAi(true);
		husk.setPersistenceRequired();
		level.addFreshEntity(husk);
		spawned.add(husk);
		return husk;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void artifactsArePricedAndSold(GameTestHelper helper) {
		ItemStack saber = Artifacts.create(ArtifactKind.SABER, ArtifactGrade.EARTH, Artifacts.Quality.NORMAL, null);
		helper.assertTrue(Math.abs(TalkSystem.worth(saber) - Artifacts.price(ArtifactGrade.EARTH) / 1.5) < 1e-6, "A cultivator gives two thirds of its price");
		helper.assertTrue(TalkSystem.worth(Artifacts.create(ArtifactKind.SABER, ArtifactGrade.EARTH, Artifacts.Quality.PERFECT, null)) > TalkSystem.worth(saber),
				"Perfect work is worth more");
		for (ArtifactGrade grade : ArtifactGrade.values()) {
			helper.assertTrue(grade == ArtifactGrade.MORTAL || Artifacts.price(grade) > Artifacts.price(ArtifactGrade.values()[grade.ordinal() - 1]), "Each grade dearer");
		}
		boolean court = CelestialCourt.wares(3).stream().anyMatch(w -> Artifacts.gradeOf(w.item()) == ArtifactGrade.IMMORTAL);
		helper.assertTrue(court, "The Court's Quartermaster has the day's Immortal artifact");
		for (int i = 0; i < 20; i++) {
			helper.assertTrue(Artifacts.gradeOf(Artifacts.any(helper.getLevel().random, ArtifactGrade.SPIRIT, QiElement.WOOD)) == ArtifactGrade.SPIRIT,
					"Any Spirit artifact is Spirit grade");
		}
		helper.assertTrue(ModItems.artifact(ArtifactKind.SABER, ArtifactGrade.HEAVEN).isFireResistant()
				&& !ModItems.artifact(ArtifactKind.SABER, ArtifactGrade.EARTH).isFireResistant(), "Heaven grade up don't burn");
		helper.succeed();
	}
}
