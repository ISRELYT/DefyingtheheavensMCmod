package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The weapons made of their element (phase 2 of the Artifact Forge, 2026-10-28; ElementalWeaponItem): forged from Spirit grade up, each
 * with a core of its own element; each blow's element (burning, slowing, rooting and never twice running, staggering, cutting through
 * armour); each art, for its share of qi and a cooldown, striking what it should and nothing else; mended with its element; priced and
 * sold.
 */
public class ElementalWeaponGameTests implements FabricGameTest {
	/** A player who isn't creative (a mock player says it is), of {@code realm}, with a full pool, facing +x. */
	private static ServerPlayer survivor(GameTestHelper helper, String name, Realm realm, double x, double z) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		stand(helper, p, x, z);
		return p;
	}

	private static void stand(GameTestHelper helper, ServerPlayer p, double x, double z) {
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		p.teleportTo(helper.getLevel(), origin.getX() + x, origin.getY() + 2, origin.getZ() + z, -90, 0);
		p.setYRot(-90);
		p.setYHeadRot(-90);
		p.setXRot(0);
	}

	private static void leave(ServerPlayer... players) {
		for (ServerPlayer p : players) {
			if (p != null && p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
		}
	}

	/** A still husk at ({@code x}, 2, {@code z}) in the test's frame. */
	private static Husk husk(GameTestHelper helper, double x, double z, List<Husk> spawned) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		Husk husk = EntityType.HUSK.create(level);
		husk.moveTo(origin.getX() + x, origin.getY() + 2, origin.getZ() + z, 0, 0);
		husk.setNoAi(true);
		husk.setPersistenceRequired();
		level.addFreshEntity(husk);
		spawned.add(husk);
		return husk;
	}

	private static ElementalWeaponItem weapon(ArtifactKind kind) {
		return (ElementalWeaponItem) ModItems.artifact(kind, ArtifactGrade.SPIRIT);
	}

	private static boolean hurt(Husk husk) {
		return husk.getHealth() < husk.getMaxHealth();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void madeOfTheirElementFromSpiritUp(GameTestHelper helper) {
		for (QiElement element : QiElement.five()) {
			ArtifactKind kind = ArtifactKind.madeOf(element);
			helper.assertTrue(kind.isElemental() && kind.element() == element, kind + " is made of " + element);
			helper.assertTrue(!kind.has(ArtifactGrade.MORTAL) && ModItems.artifact(kind, ArtifactGrade.MORTAL) == null, "No Mortal " + kind);
			for (ArtifactGrade grade : ArtifactGrade.values()) {
				if (grade == ArtifactGrade.MORTAL) continue;
				helper.assertTrue(ModItems.artifact(kind, grade) instanceof ElementalWeaponItem, kind + " at " + grade + " is an elemental weapon");
				ArtifactRecipes.Recipe recipe = ArtifactRecipes.forOutput(ModItems.artifact(kind, grade));
				helper.assertTrue(recipe != null && recipe.core() && !recipe.token(), kind + " at " + grade + " is forged with a core");
			}
		}
		// A Spirit Flame Blade: two blaze rods, magma cream, two Mid stones and a fire core; a water core won't do.
		List<ItemStack> in = new ArrayList<>(List.of(new ItemStack(Items.BLAZE_ROD), new ItemStack(Items.BLAZE_ROD), new ItemStack(Items.MAGMA_CREAM),
				new ItemStack(ModItems.MID_SPIRIT_STONE), new ItemStack(ModItems.MID_SPIRIT_STONE)));
		int qr = PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY);
		helper.assertFalse(ArtifactRecipes.canAdd(in, DemonCoreItem.of(qr, QiElement.WATER, null)), "A water core fits no Flame Blade");
		helper.assertFalse(ArtifactRecipes.canAdd(in, DemonCoreItem.of(qr, null, null)), "Nor a core of no element");
		in.add(DemonCoreItem.of(qr, QiElement.FIRE, null));
		ArtifactRecipes.Recipe flame = ArtifactRecipes.match(in);
		helper.assertTrue(flame != null && flame.kind() == ArtifactKind.FLAME_BLADE && flame.grade() == ArtifactGrade.SPIRIT, "A fire core: a Spirit Flame Blade");
		helper.assertTrue(ModItems.artifact(ArtifactKind.FLAME_BLADE, ArtifactGrade.SPIRIT).isFireResistant(), "Flame doesn't burn");
		helper.assertTrue(weapon(ArtifactKind.TIDE_SPEAR).isValidRepairItem(ItemStack.EMPTY, new ItemStack(Items.PRISMARINE_CRYSTALS))
				&& !weapon(ArtifactKind.TIDE_SPEAR).isValidRepairItem(ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT)), "Mended with its own element");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachBlowDoesWhatItsElementDoes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper, "test-element-blows", Realm.FOUNDATION_BUILDING, 0.5, 1.5);
		List<Husk> husks = new ArrayList<>();
		try {
			Husk burnt = husk(helper, 2.5, 0.5, husks), slowed = husk(helper, 2.5, 2.5, husks), rooted = husk(helper, 2.5, 4.5, husks),
					staggered = husk(helper, 4.5, 0.5, husks), cut = husk(helper, 4.5, 2.5, husks);
			cut.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
			cut.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));

			weapon(ArtifactKind.FLAME_BLADE).strike(level, new ItemStack(weapon(ArtifactKind.FLAME_BLADE)), burnt, p);
			helper.assertTrue(burnt.getRemainingFireTicks() > 0 && hurt(burnt), "The Flame Blade burns, and its fire qi lands");
			burnt.clearFire();

			weapon(ArtifactKind.TIDE_SPEAR).strike(level, new ItemStack(weapon(ArtifactKind.TIDE_SPEAR)), slowed, p);
			helper.assertTrue(slowed.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && hurt(slowed), "The Tide Spear slows");

			weapon(ArtifactKind.VINE_WHIP).strike(level, new ItemStack(weapon(ArtifactKind.VINE_WHIP)), rooted, p);
			helper.assertTrue(HeavenSpells.lingering(p.getUUID(), WoodSpells.Bound.class).stream().anyMatch(b -> b.target == rooted),
					"The Vine Whip roots");
			helper.assertFalse(ElementalWeaponItem.root(p, rooted, 20), "...and never twice running");

			weapon(ArtifactKind.MOUNTAIN_HAMMER).strike(level, new ItemStack(weapon(ArtifactKind.MOUNTAIN_HAMMER)), staggered, p);
			helper.assertTrue(staggered.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && staggered.hasEffect(MobEffects.WEAKNESS)
					&& staggered.getDeltaMovement().x > 0, "The Mountain Hammer staggers: dazed, thrown back");

			ItemStack golden = new ItemStack(weapon(ArtifactKind.GOLDEN_SWORD));
			float before = cut.getHealth();
			weapon(ArtifactKind.GOLDEN_SWORD).strike(level, golden, cut, p);
			double expected = ElementalWeaponItem.attackOf(p) * weapon(ArtifactKind.GOLDEN_SWORD).share(golden);
			helper.assertTrue(Math.abs((before - cut.getHealth()) - expected) < 0.05,
					"The Golden Light Sword cuts through diamond armour whole: " + (before - cut.getHealth()) + " of " + expected);
			helper.assertTrue(weapon(ArtifactKind.GOLDEN_SWORD).share(golden) > weapon(ArtifactKind.FLAME_BLADE).share(golden),
					"Its share the greatest");
		} finally {
			husks.forEach(Husk::discard);
			leave(p);
		}
		helper.succeed();
	}

	/** Each art costs its share of the pool and starts the item's cooldown; none with too little qi. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void artsCostQiAndWait(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper, "test-element-arts", Realm.FOUNDATION_BUILDING, 0.5, 1.5);
		List<Husk> husks = new ArrayList<>();
		try {
			PlayerCultivation c = CultivationManager.forPlayer(p);
			ItemStack hammer = new ItemStack(weapon(ArtifactKind.MOUNTAIN_HAMMER));
			p.setItemInHand(InteractionHand.MAIN_HAND, hammer);
			Husk near = husk(helper, 2.5, 1.5, husks), far = husk(helper, 0.5, 7.5, husks);
			double pool = c.getQi();
			hammer.getItem().use(level, p, InteractionHand.MAIN_HAND);
			helper.assertTrue(Math.abs(pool - c.getQi() - c.maxQi() * ElementalWeaponItem.ART_COST) < 1.0e-6, "The quake took 8% of the pool");
			helper.assertTrue(p.getCooldowns().isOnCooldown(hammer.getItem()), "...and waits");
			helper.assertTrue(near.hasEffect(MobEffects.WEAKNESS) && hurt(near), "Mountain Quake staggers the one beside");
			helper.assertFalse(hurt(far), "...not one beyond its reach");
			double left = c.getQi();
			p.gameMode.useItem(p, level, hammer, InteractionHand.MAIN_HAND);
			helper.assertTrue(c.getQi() == left, "Used again at once (as the game does it), it waits: no qi spent");
			husks.forEach(Husk::discard);
			husks.clear();

			ItemStack lash = new ItemStack(weapon(ArtifactKind.VINE_WHIP));
			p.setItemInHand(InteractionHand.MAIN_HAND, lash);
			c.setQi(0);
			helper.assertFalse(lash.getItem().use(level, p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "No qi, no art");
			helper.assertFalse(p.getCooldowns().isOnCooldown(lash.getItem()), "...and no wait");
			c.setQi(c.maxQi());
			Husk lashed = husk(helper, 6.5, 1.5, husks);
			lash.getItem().use(level, p, InteractionHand.MAIN_HAND);
			helper.assertTrue(HeavenSpells.lingering(p.getUUID(), WoodSpells.Bound.class).stream().anyMatch(b -> b.target == lashed) && hurt(lashed),
					"Binding Lash roots a foe six blocks off");
		} finally {
			husks.forEach(Husk::discard);
			leave(p);
		}
		helper.succeed();
	}

	/** The fire sweeps before its wielder only; the crescent cuts along its line only. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void flameAndCrescentStrikeWhereAimed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper, "test-element-aim", Realm.FOUNDATION_BUILDING, 0.5, 2.5);
		List<Husk> husks = new ArrayList<>();
		try {
			ItemStack flame = new ItemStack(weapon(ArtifactKind.FLAME_BLADE));
			p.setItemInHand(InteractionHand.MAIN_HAND, flame);
			Husk before = husk(helper, 3.0, 2.5, husks), behind = husk(helper, -2.0, 2.5, husks);
			flame.getItem().use(level, p, InteractionHand.MAIN_HAND);
			helper.assertTrue(hurt(before) && before.getRemainingFireTicks() > 0, "Flame Arc burns the one before");
			helper.assertFalse(hurt(behind), "...not the one behind");
			husks.forEach(Husk::discard);
			husks.clear();

			// A wall past the second, so the crescent goes no further than this test's ground.
			helper.setBlock(WALL, Blocks.STONE);
			ItemStack golden = new ItemStack(weapon(ArtifactKind.GOLDEN_SWORD));
			p.setItemInHand(InteractionHand.MAIN_HAND, golden);
			Husk first = husk(helper, 3.5, 2.5, husks), second = husk(helper, 7.5, 2.5, husks), aside = husk(helper, 4.5, 6.5, husks);
			golden.getItem().use(level, p, InteractionHand.MAIN_HAND);
			helper.assertTrue(hurt(first) && hurt(second), "Golden Crescent cuts through all on its line");
			helper.assertFalse(hurt(aside), "...not one aside");
		} finally {
			helper.setBlock(WALL, Blocks.AIR);
			husks.forEach(Husk::discard);
			leave(p);
		}
		helper.succeed();
	}

	/** Where a wall stands to keep a test's arts on its own ground: at eye height, eight blocks on (along +x). */
	private static final BlockPos WALL = new BlockPos(9, 3, 2);

	/** The tide carries its rider forward, slowing who stands in the way. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theTideCarriesItsRider(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper, "test-element-tide", Realm.FOUNDATION_BUILDING, 0.5, 1.5);
		List<Husk> husks = new ArrayList<>();
		BlockPos wall = new BlockPos(7, 2, 1);
		try {
			helper.setBlock(wall, Blocks.STONE); // the wave stops at it, on this test's own ground
			ItemStack tide = new ItemStack(weapon(ArtifactKind.TIDE_SPEAR));
			p.setItemInHand(InteractionHand.MAIN_HAND, tide);
			Husk inTheWay = husk(helper, 3.0, 1.5, husks);
			double from = p.getX();
			tide.getItem().use(level, p, InteractionHand.MAIN_HAND);
			helper.assertTrue(p.getX() - from >= 3.0 && p.getX() < helper.absolutePos(wall).getX(), "Surging Tide carried Tester forward " + (p.getX() - from)
					+ ", short of the wall");
			helper.assertTrue(inTheWay.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && hurt(inTheWay), "...slowing the one in the way");
		} finally {
			helper.setBlock(wall, Blocks.AIR);
			husks.forEach(Husk::discard);
			leave(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void pricedAndSoldByElement(GameTestHelper helper) {
		ItemStack flame = new ItemStack(ModItems.artifact(ArtifactKind.FLAME_BLADE, ArtifactGrade.SPIRIT));
		helper.assertTrue(Artifacts.priceOf(flame) == Artifacts.price(ArtifactGrade.SPIRIT) * 3 / 2, "Half again its grade's price");
		boolean sawFlame = false;
		for (int i = 0; i < 300; i++) {
			ItemStack any = Artifacts.any(helper.getLevel().random, ArtifactGrade.SPIRIT, QiElement.FIRE);
			if (any.getItem() instanceof ElementalWeaponItem weapon) {
				helper.assertTrue(weapon.element() == QiElement.FIRE, "A fire cultivator sells only the fire weapon");
				sawFlame = true;
			}
			helper.assertFalse(Artifacts.any(helper.getLevel().random, ArtifactGrade.MORTAL, QiElement.FIRE).getItem() instanceof ElementalWeaponItem,
					"No elemental weapon at Mortal grade");
		}
		helper.assertTrue(sawFlame, "...and does sell it");
		helper.succeed();
	}
}
