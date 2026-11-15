package com.example.defyingtheheavens;

import com.example.defyingtheheavens.mixin.ServerPlayerAccessor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

/**
 * The Spatial Gap's storms that matter, and pressure one out-heals (2026-11-13, the user: "Do the storms that matter change. Also,
 * people who can out heal the spatial pressure currently still have the damage tick happen which is annoying").
 */
public class GapStormsGameTests implements FabricGameTest {
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, stage, 0);
		c.setLowerRealmBound(false); // its own body in the lower realms (the tests' level is the Overworld)
		CultivationManager.refresh(p);
		((ServerPlayerAccessor) p).dth$setSpawnInvulnerableTime(0);
		p.setHealth(p.getMaxHealth());
		return p;
	}

	private static void fed(ServerPlayer p) {
		p.getFoodData().setFoodLevel(20);
		p.getFoodData().setSaturation(20);
	}

	private static void leave(ServerPlayer p) {
		if (p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyThatOutHealsThePressureDoesntFeelIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		DamageSource pressure = ModDamageTypes.spatialPressure(level);
		float layers = SpatialTrialHandler.LAYER_PRESSURE;
		ServerPlayer axis = cultivator(helper, Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
		ServerPlayer being = cultivator(helper, Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);
		try {
			// Fed, a body mends vanilla's quick two a second times its realm's pace.
			fed(axis);
			fed(being);
			double axisMends = CultivationStats.mendingNow(axis), beingMends = CultivationStats.mendingNow(being);
			helper.assertTrue(Math.abs(axisMends - 2 * CultivationStats.mendFactor(axis)) < 1e-6, "Fed, two a second at the realm's pace (" + axisMends + ")");
			helper.assertTrue(axisMends >= layers && beingMends < layers, "Four Axis Grand Perfection out-heals the stormy layers' 8.2 (" + axisMends
					+ "), a Heavenly Being Grand Perfection doesn't (" + beingMends + ")");
			float whole = axis.getHealth();
			helper.assertTrue(!SpatialTrialHandler.press(axis, pressure, layers) && axis.getHealth() == whole && axis.hurtTime == 0,
					"Out-healed: no hurt at all, not even the flinch (" + axis.getHealth() + " of " + whole + ")");
			float before = being.getHealth();
			helper.assertTrue(SpatialTrialHandler.press(being, pressure, layers) && Math.abs(before - being.getHealth() - layers) < 0.01,
					"Not out-healed: the second's pressure lands whole (" + before + " -> " + being.getHealth() + ")");
			// Hungry, with nothing keeping it fed, the body mends nothing, and the pressure is felt again.
			CultivationManager.get(axis).setAbilityEnabled(Ability.QI_SUSTENANCE, false);
			axis.getFoodData().setFoodLevel(10);
			axis.getFoodData().setSaturation(0);
			helper.assertTrue(CultivationStats.mendingNow(axis) == 0, "Hungry: no mending (" + CultivationStats.mendingNow(axis) + ")");
			axis.invulnerableTime = 0;
			helper.assertTrue(SpatialTrialHandler.press(axis, pressure, layers) && axis.getHealth() < whole, "Hungry, a Four Axis feels the pressure again");
			// Regeneration mends on top: one health every 50 >> amplifier ticks.
			axis.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
			helper.assertTrue(Math.abs(CultivationStats.mendingNow(axis) - 0.8) < 1e-6, "Regeneration II mends 0.8 a second (" + CultivationStats.mendingNow(axis) + ")");
			helper.assertTrue(!SpatialTrialHandler.press(axis, pressure, 0.5f), "A pressure Regeneration outpaces isn't felt either");
			helper.succeed();
		} finally {
			leave(axis);
			leave(being);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aStormsBoltStrikesTwelveThroughAnyArmour(GameTestHelper helper) {
		ServerPlayer p = cultivator(helper, Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);
		try {
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				ItemStack piece = new ItemStack(switch (slot) {
					case HEAD -> Items.NETHERITE_HELMET;
					case CHEST -> Items.NETHERITE_CHESTPLATE;
					case LEGS -> Items.NETHERITE_LEGGINGS;
					default -> Items.NETHERITE_BOOTS;
				});
				piece.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
				p.setItemSlot(slot, piece);
			}
			// The pieces' own armour counts once the body has ticked in them (a mock player doesn't); its realm's armour and the pieces'
			// Protection count at once.
			int protection = net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageProtection(p.getArmorSlots(), helper.getLevel().damageSources().generic());
			helper.assertTrue(p.getArmorValue() >= 15 && protection >= 16, "Armoured: its realm's armour (" + p.getArmorValue() + ") and Protection IV all over ("
					+ protection + ")");
			float before = p.getHealth();
			SpatialStorms.hurt(helper.getLevel(), p);
			helper.assertTrue(Math.abs(before - p.getHealth() - SpatialStorms.BOLT_DAMAGE) < 0.01,
					"A bolt strikes a flat 12, armour and its enchantments no help (" + before + " -> " + p.getHealth() + ")");
			DamageSource last = p.getLastDamageSource();
			helper.assertTrue(last != null && last.is(ModDamageTypes.SPATIAL_STORM) && last.is(DamageTypeTags.BYPASSES_ARMOR)
					&& last.is(DamageTypeTags.BYPASSES_ENCHANTMENTS) && last.is(DamageTypeTags.NO_IMPACT), "As a spatial storm's bolt");
			helper.assertTrue(SpatialStorms.boltDamage(p) == SpatialStorms.BOLT_DAMAGE, "Whatever the realm: no ring, the whole of it");
			helper.succeed();
		} finally {
			leave(p);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theirReachAndHowTheyTurn(GameTestHelper helper) {
		// Beneath the cloud within its lightning's 100, or inside the cloud (to 48 over its base); not past its edge.
		helper.assertTrue(SpatialStorms.inReach(0, 0, 300, 150, 0, 250, 0) && SpatialStorms.inReach(0, 0, 300, 150, 0, 200, 0)
				&& SpatialStorms.inReach(0, 0, 300, 150, 0, 348, 0) && SpatialStorms.inReach(0, 0, 300, 150, 149, 250, 0), "In its reach");
		helper.assertTrue(!SpatialStorms.inReach(0, 0, 300, 150, 0, 199, 0) && !SpatialStorms.inReach(0, 0, 300, 150, 0, 349, 0)
				&& !SpatialStorms.inReach(0, 0, 300, 150, 151, 250, 0), "Out of it");
		helper.assertTrue(!SpatialStorms.inForce(SpatialStorms.FORM_TICKS - 1, 2400) && SpatialStorms.inForce(SpatialStorms.FORM_TICKS, 2400)
				&& !SpatialStorms.inForce(2400 - SpatialStorms.FADE_TICKS + 1, 2400), "In force once formed, until it dies away");
		// Turning toward a climber: at most the turn a tick, its speed kept; in time, straight at them.
		double[] v = SpatialStorms.steer(0.2, 0, 0, 100, SpatialStorms.TURN_PER_TICK);
		helper.assertTrue(v[1] > 0 && Math.abs(Math.atan2(v[1], v[0]) - SpatialStorms.TURN_PER_TICK) < 1e-9 && Math.abs(Math.hypot(v[0], v[1]) - 0.2) < 1e-9,
				"One tick: turned a little toward them, as fast as before");
		for (int i = 0; i < 200; i++) v = SpatialStorms.steer(v[0], v[1], 0, 100, SpatialStorms.TURN_PER_TICK);
		helper.assertTrue(Math.abs(v[0]) < 1e-9 && Math.abs(v[1] - 0.2) < 1e-9, "In time, straight at them (" + v[0] + ", " + v[1] + ")");
		helper.assertTrue(SpatialStorms.DRAG == 0.5f && SwordFlight.QI_FLIGHT_SPEED * SpatialStorms.DRAG / SpatialStorms.DRAG == SwordFlight.QI_FLIGHT_SPEED,
				"Dragged to a half, exactly: a halved speed is still known for what it came of");
		// Closing in (a storm at 0, 0, its base at 300, 150 across, drifting north): toward a climber 400 off to the east at 260, as fast as before.
		java.util.function.Function<java.util.List<net.minecraft.world.phys.Vec3>, double[]> drift = climbers -> SpatialStorms.closeIn(0, 0, 300, 150, 0, 0.2, climbers);
		double[] toward = drift.apply(java.util.List.of(new net.minecraft.world.phys.Vec3(400, 260, 0)));
		helper.assertTrue(toward[0] > 0 && Math.abs(Math.hypot(toward[0], toward[1]) - 0.2) < 1e-9, "Turned toward a climber off to the side (" + toward[0] + ", " + toward[1] + ")");
		double[] nearer = drift.apply(java.util.List.of(new net.minecraft.world.phys.Vec3(600, 260, 0), new net.minecraft.world.phys.Vec3(-300, 260, 0)));
		helper.assertTrue(nearer[0] < 0, "Toward the nearer of two (" + nearer[0] + ")");
		double[] under = drift.apply(java.util.List.of(new net.minecraft.world.phys.Vec3(400, 260, 0), new net.minecraft.world.phys.Vec3(50, 250, 0)));
		helper.assertTrue(under[0] == 0 && under[1] == 0.2, "Someone under it: it holds its course, so they can fly out of it");
		double[] far = drift.apply(java.util.List.of(new net.minecraft.world.phys.Vec3(400, 130, 0), new net.minecraft.world.phys.Vec3(800, 260, 0)));
		helper.assertTrue(far[0] == 0 && far[1] == 0.2, "None near its layer (one far below, one too far off): it keeps its drift");
		helper.succeed();
	}

	/**
	 * In the gap itself: a storm over a climber drags its flight to half and holds its course. (A mock player is creative to the game, and a
	 * storm closes in on no creative player, so the one off to the side keeps its drift here: the turning is checked above, and in game.)
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void inTheGapTheirWindsDragAndTheUpperOnesCloseIn(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerLevel gap = server.getLevel(ModDimensions.SPATIAL_GAP);
		if (gap == null) { // only vanilla's dimensions on this server: no gap
			helper.succeed();
			return;
		}
		ServerPlayer p = cultivator(helper, Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);
		PlayerCultivation c = CultivationManager.get(p);
		int over = -1, aside = -1;
		try {
			SpatialTrialHandler.beginAscension(p);
			p.teleportTo(gap, p.getX(), 260, p.getZ(), 0, 0);
			over = SpatialStorms.place(p.getX(), 300, p.getZ(), 0.2, 0, 150);
			aside = SpatialStorms.place(p.getX() + 400, 300, p.getZ(), 0, 0.2, 150);
			SpatialStorms.tick(server);
			helper.assertTrue(SpatialStorms.drags(p), "Under a storm in force: its winds hold them");
			helper.assertTrue(QiFlight.dragged(p, SwordFlight.QI_FLIGHT_SPEED) == SwordFlight.QI_FLIGHT_SPEED / 2
					&& QiFlight.dragged(p, BodyTechniques.TREAD_SPEED) == BodyTechniques.TREAD_SPEED / 2, "Any flight at half speed");
			double[] held = SpatialStorms.drift(over), turned = SpatialStorms.drift(aside);
			helper.assertTrue(held != null && held[0] == 0.2 && held[1] == 0, "The storm they're under holds its course, so they can fly out of it");
			helper.assertTrue(p.isCreative() && turned != null && turned[0] == 0 && turned[1] == 0.2,
					"A creative player draws no storm (" + (turned == null ? "gone" : turned[0] + ", " + turned[1]) + ")");
			SpatialStorms.remove(over);
			SpatialStorms.remove(aside);
			over = aside = -1;
			SpatialStorms.tick(server);
			helper.assertTrue(QiFlight.dragged(p, SwordFlight.QI_FLIGHT_SPEED) == SwordFlight.QI_FLIGHT_SPEED || SpatialStorms.drags(p),
					"Out from under it, the whole speed again (unless another storm drifted over meanwhile)");
			helper.succeed();
		} finally {
			if (over >= 0) SpatialStorms.remove(over);
			if (aside >= 0) SpatialStorms.remove(aside);
			SpatialTrialHandler.forget(p.getUUID());
			c.setPendingTrial(SpatialTrialHandler.Route.NONE);
			c.setAbilityEnabled(Ability.SPATIAL_TEAR, false);
			c.setGapFromUpper(false);
			net.minecraft.core.BlockPos home = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
			p.teleportTo(helper.getLevel(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0, 0);
			leave(p);
		}
	}
}
