package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 3c (2026-10-26): the Chaos Frontier (ChaosFrontier). Where it lies (out past some four thousand blocks from the main island's heart,
 * its own past its edge); its land (the realm's broken into floating shards that join further out into one wasteland, the First Flame's
 * island level among them, no city or sect reaching toward it, its biome picked by the terrain's own "erosion"); its storm (a hundredth of
 * one's health a second through armour and
 * wards, no mending of itself, shelter in a domain not a foe's); its fiends (their ranks by depth, a realm above them left alone, their
 * spoils); the Primordial Chaos (a Golden Immortal's might, its lair, its Unmaking folding domains, its spoils, the Court's pay); the Dao
 * Fragments; the First Flame's basin and its light. The Immortal Realm's land as its generator makes it (ImmortalCitiesGameTests#land:
 * written when the test server had no Immortal Realm), its rules by their own parts. Each test with a player in a batch of its own.
 */
public class ChaosFrontierGameTests implements FabricGameTest {
	private static final int TI_EARLY = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY);

	/** A survival player in earnest (a mock player answers that it is creative), of {@code realm} Early, no body's gifts, at the test's corner. */
	private static ServerPlayer survivor(GameTestHelper helper, Realm realm, String name) {
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
		PlayerCultivation c = CultivationManager.get(p);
		c.setPhysique(null);
		c.setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		p.setHealth(p.getMaxHealth());
		// A player just come into the world is spared all harm three seconds (never ticked here, it would stay so).
		try {
			java.lang.reflect.Field spared = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			spared.setAccessible(true);
			spared.setInt(p, 0);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		return p;
	}

	private static void leave(ServerPlayer p) {
		ChaosFrontier.onDisconnect(p);
		LawDomains.onDisconnect(p);
		ImmortalEssence.onDisconnect(p);
		FiveDecays.forget(p.getUUID());
		Lifespan.forget(p.getUUID());
		LifespanTribulation.forget(p.getUUID());
		QiFlight.forget(p.getUUID());
		if (p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
	}

	private static boolean near(double a, double b, double by) {
		return Math.abs(a - b) <= by;
	}

	/** A column {@code out} blocks from the main island's heart, the way (0, 1) east. */
	private static double[] east(double out) {
		return new double[] {ModNoiseSettings.MAIN_X + out, ModNoiseSettings.MAIN_Z};
	}

	/**
	 * The Frontier's land about column {@code c}, on 16 x 16 columns 16 apart: the share of them with land, its share of the sky between
	 * Y 140 and 270, any of it below Y 120 or over Y 290, the thinnest land, and the share of the land with open air under it over the sea
	 * of clouds (dig down through it, and there they are).
	 */
	private record Sky(double landed, double solid, int low, int high, int thinnest, double overClouds) {
		@Override
		public String toString() {
			return Math.round(landed * 100) + "% of columns, " + Math.round(solid * 1000) / 10.0 + "% of the sky, thinnest " + thinnest + ", "
					+ Math.round(overClouds * 100) + "% over the clouds";
		}
	}

	private static Sky sky(DensityFunction density, double[] c) {
		int columns = 0, landed = 0, solid = 0, cells = 0, low = 0, high = 0, thinnest = Integer.MAX_VALUE, overClouds = 0;
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				int x = (int) c[0] - 120 + i * 16, z = (int) c[1] - 120 + j * 16, top = Integer.MIN_VALUE, bottom = Integer.MAX_VALUE;
				columns++;
				for (int y = 100; y <= 320; y += 2) {
					boolean s = density.compute(new DensityFunction.SinglePointContext(x, y, z)) > 0;
					if (y < 120 && s) low++;
					else if (y > 290 && s) high++;
					if (y >= 140 && y <= 270 && y % 4 == 0) {
						cells++;
						if (s) solid++;
					}
					if (s) {
						top = Math.max(top, y);
						bottom = Math.min(bottom, y);
					}
				}
				if (top == Integer.MIN_VALUE) continue;
				landed++;
				thinnest = Math.min(thinnest, top - bottom);
				if (bottom > ModDimensions.IMMORTAL_CLOUD_Y + 2) overClouds++;
			}
		}
		return new Sky(landed / (double) columns, solid / (double) cells, low, high, thinnest, landed == 0 ? 0 : overClouds / (double) landed);
	}

	// --- Where it lies ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "chaos_frontier_1")
	public void whereItLies(GameTestHelper helper) {
		double[] inside = east(3900), from = east(ModNoiseSettings.FRONTIER_FROM), edge = east(ChaosFrontier.EDGE), past = east(4400);
		helper.assertTrue(ModNoiseSettings.frontier(inside[0], inside[1]) == 0 && near(ModNoiseSettings.frontier(edge[0], edge[1]), 0.5, 1e-6)
				&& ModNoiseSettings.frontier(past[0], past[1]) == 1, "The land breaks up from 4,000 blocks out, half way by its edge, all past 4,300");
		helper.assertTrue(ChaosFrontier.EDGE == 4150 && near(ChaosFrontier.depth(edge[0], edge[1]), 0, 1e-6) && ChaosFrontier.depth(inside[0], inside[1]) < 0,
				"Its edge some 4,150 blocks out");
		helper.assertTrue(ChaosFrontier.pastSettled(from[0], from[1]) && !ChaosFrontier.pastSettled(inside[0], inside[1]), "None of the realm's people past where it breaks up");
		// The fiends by depth: True Immortal Mid to Grand Perfection, then Earth Immortals, then Heaven Immortals.
		int[] shallow = ChaosFrontier.fiendRanks(10), middle = ChaosFrontier.fiendRanks(1500), deep = ChaosFrontier.fiendRanks(2500);
		helper.assertTrue(shallow[0] == PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.MID) && shallow[1] == PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION)
				&& middle[0] == PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.EARLY) && middle[1] == PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.GRAND_PERFECTION)
				&& deep[0] == PlayerCultivation.rank(Realm.HEAVEN_IMMORTAL, Stage.EARLY) && deep[1] == PlayerCultivation.rank(Realm.HEAVEN_IMMORTAL, Stage.GRAND_PERFECTION),
				"Fiends of True Immortal Mid..GP within 1,000 of the edge, Earth Immortals to 2,000, Heaven Immortals beyond");
		helper.assertTrue(BeastKind.Tier.CHAOS.minRank == shallow[0] && BeastKind.Tier.CHAOS.maxRank == deep[1], "Their tier spans them all");
		// The First Flame's spring 4,600 blocks south of the heart, in the Frontier's shallows; its light sixteen blocks about the flame.
		Vec3 heart = FirstFlame.heart();
		helper.assertTrue(ModNoiseSettings.SPRING_Z - ModNoiseSettings.MAIN_Z == 4600 && ModNoiseSettings.SPRING_X == ModNoiseSettings.MAIN_X
				&& ChaosFrontier.depth(heart.x, heart.z) > 0 && ChaosFrontier.depth(heart.x, heart.z) < ChaosFrontier.SHALLOW, "The spring 4,600 south, in the Frontier's shallows");
		helper.assertTrue(FirstFlame.inLight(ModDimensions.IMMORTAL_REALM, heart) && FirstFlame.inLight(ModDimensions.IMMORTAL_REALM, heart.add(15.9, 0, 0))
				&& !FirstFlame.inLight(ModDimensions.IMMORTAL_REALM, heart.add(0, 16.2, 0)) && !FirstFlame.inLight(helper.getLevel().dimension(), heart),
				"Its light sixteen blocks about the flame, and only in the Immortal Realm");
		// The Primordial Chaos's lair 5,600 out, among the shards, Earth Immortals' depth.
		BlockPos lair = PrimordialBeasts.chaosLair(helper.getLevel().getSeed());
		helper.assertTrue(near(ChaosFrontier.out(lair.getX(), lair.getZ()), PrimordialBeasts.CHAOS_LAIR_OUT, 1.5) && lair.getY() == PrimordialBeasts.CHAOS_LAIR_Y
				&& ChaosFrontier.depth(lair.getX(), lair.getZ()) >= ChaosFrontier.SHALLOW, "Its lair 5,600 out (" + lair.toShortString() + ")");
		helper.assertTrue(PrimordialBeasts.chaosLair(helper.getLevel().getSeed()).equals(lair) && !PrimordialBeasts.chaosLair(helper.getLevel().getSeed() + 1).equals(lair),
				"Its lair the world's own, from its seed");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "chaos_frontier_1")
	public void itsOwnLandAndOre(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
		Holder<Biome> chaos = biomes.getHolderOrThrow(ModBiomes.CHAOS_FRONTIER);
		Holder<PlacedFeature> ore = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(ModPlacedFeatures.ORE_CHAOS_ORIGIN);
		boolean has = chaos.value().getGenerationSettings().features().stream().anyMatch(step -> step.contains(ore));
		helper.assertTrue(has, "Chaos Origin Ore in its shards");
		helper.assertTrue(!chaos.is(ModTags.IS_IMMORTAL_REALM), "None of the Jade Firmament's herbs or ores, nor the realm's sects (not of its tag)");
		helper.assertTrue(chaos.value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).isEmpty(), "Nothing born of it: its fiends are ChaosFrontier's");
		// The realm's biome source picks it where the Frontier's share passes a half.
		var resource = level.getServer().getResourceManager().getResource(new ResourceLocation(DefyingTheHeavens.MOD_ID, "dimension/immortal_realm.json"));
		helper.assertTrue(resource.isPresent(), "The realm's dimension");
		try (var reader = resource.get().openAsReader()) {
			String text = reader.lines().reduce("", String::concat);
			helper.assertTrue(text.contains("defying-the-heavens:chaos_frontier") && text.contains("\"erosion\": 1.0"), "Its biome in the realm's, at the erosion of the whole Frontier");
		} catch (java.io.IOException e) {
			helper.fail("The dimension unread: " + e);
		}
		// The ore: a diamond pickaxe's; leaves Chaos Origin Stones; lit a little.
		BlockState oreBlock = ModBlocks.CHAOS_ORIGIN_ORE.defaultBlockState();
		helper.assertTrue(oreBlock.is(BlockTags.MINEABLE_WITH_PICKAXE) && oreBlock.is(BlockTags.NEEDS_DIAMOND_TOOL) && oreBlock.getLightEmission() > 0,
				"Chaos Origin Ore: a diamond pickaxe's, glowing faintly");
		LootTable table = level.getServer().getLootData().getLootTable(ModBlocks.CHAOS_ORIGIN_ORE.getLootTable());
		helper.assertTrue(table != LootTable.EMPTY, "It leaves its stones");
		helper.assertTrue(Blocks.TUFF.defaultBlockState().is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) && Blocks.DEEPSLATE.defaultBlockState().is(BlockTags.DEEPSLATE_ORE_REPLACEABLES),
				"The ore sets in the shards' tuff and deepslate");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "chaos_frontier_land", timeoutTicks = 1200)
	public void theLandBreaksUp(GameTestHelper helper) {
		ImmortalCitiesGameTests.Land land = ImmortalCitiesGameTests.land(helper);
		RandomState state = land.state();
		DensityFunction density = state.router().finalDensity(), erosion = state.router().erosion();
		// The Frontier's share, which picks its biome.
		for (double[] at : new double[][] {{3900, 0}, {4150, 0.5}, {4400, 1}}) {
			double[] c = east(at[0]);
			double share = erosion.compute(new DensityFunction.SinglePointContext((int) c[0], 200, (int) c[1]));
			helper.assertTrue(near(share, at[1], 0.01), "The Frontier's share " + at[1] + " at " + at[0] + " out (" + share + ")");
		}
		// The First Flame's island: level at its top, solid under it, open over it.
		int sx = ModNoiseSettings.SPRING_X, sz = ModNoiseSettings.SPRING_Z, sy = ModNoiseSettings.SPRING_Y;
		helper.assertTrue(density.compute(new DensityFunction.SinglePointContext(sx, sy - 3, sz)) > 0 && density.compute(new DensityFunction.SinglePointContext(sx, sy + 3, sz)) < 0,
				"The spring's island solid under its top, open over it");
		int top = land.generator().getBaseHeight(sx, sz, Heightmap.Types.WORLD_SURFACE_WG, land.heights(), state);
		helper.assertTrue(Math.abs(top - sy) <= 2, "Its top level about Y " + sy + " (" + top + ")");
		int ringSolid = 0;
		for (int k = 0; k < 48; k++) {
			double a = Math.PI * 2 * k / 48;
			for (double r : new double[] {50, 58, 66}) {
				for (int y = ModDimensions.IMMORTAL_CLOUD_Y - 40; y <= 300; y += 4) {
					if (density.compute(new DensityFunction.SinglePointContext((int) Math.round(sx + Math.cos(a) * r), y, (int) Math.round(sz + Math.sin(a) * r))) > 0) ringSolid++;
				}
			}
		}
		helper.assertTrue(ringSolid == 0, "Open sky about the island: no shard near it (" + ringSolid + " solid)");
		// Out in the Frontier: near its edge shards floating between the sea of clouds and some Y 260, open air between them; further out
		// they join (the user's word, 2026-10-26), until past some 5,000 blocks it is one land, a wasteland floating over the sea of clouds.
		// None of the realm's massifs or their roots below, nothing above Y 290.
		double[] joins = east(ModNoiseSettings.FRONTIER_TO), half = east((ModNoiseSettings.WASTE_FROM + ModNoiseSettings.WASTE_TO) / 2), whole = east(5200);
		helper.assertTrue(ModNoiseSettings.waste(joins[0], joins[1]) == 0 && near(ModNoiseSettings.waste(half[0], half[1]), 0.5, 1e-6)
				&& ModNoiseSettings.waste(whole[0], whole[1]) == 1, "The shards join from where the Frontier's land is all its own, whole by 5,100 blocks out");
		Sky shards = sky(density, east(4450)), joining = sky(density, east(4750)), wasteland = sky(density, east(5400));
		helper.assertTrue(shards.solid() > 0.01 && shards.solid() < 0.3 && shards.landed() < 0.8, "Shards here and there near its edge, open air between (" + shards + ")");
		helper.assertTrue(joining.landed() > shards.landed() && joining.landed() < 1, "Further out more of them, joining, gaps still between (" + joining + ")");
		helper.assertTrue(wasteland.landed() == 1 && wasteland.thinnest() >= 20 && wasteland.overClouds() == 1,
				"Past 5,000 blocks one land, some twenty blocks thick at its thinnest, open air under it over the sea of clouds (" + wasteland + ")");
		for (Sky sky : new Sky[] {shards, joining, wasteland}) {
			helper.assertTrue(sky.low() == 0 && sky.high() == 0, "Nothing of the realm's massifs below it, nothing above Y 290 (" + sky.low() + ", " + sky.high() + ")");
		}
		// The Primordial Chaos's lair: its body clear of the shards about it.
		BlockPos lair = PrimordialBeasts.findLair(state, land.generator(), land.heights(), helper.getLevel().getSeed(), BeastKind.PRIMORDIAL_CHAOS);
		int inLair = 0;
		for (int dx = -8; dx <= 8; dx += 4) {
			for (int dz = -8; dz <= 8; dz += 4) {
				for (int dy = 0; dy <= 16; dy += 4) if (density.compute(new DensityFunction.SinglePointContext(lair.getX() + dx, lair.getY() + dy, lair.getZ() + dz)) > 0) inLair++;
			}
		}
		helper.assertTrue(lair.getY() >= PrimordialBeasts.CHAOS_LAIR_Y && inLair == 0, "Its lair clear of the shards (" + lair.toShortString() + ", " + inLair + " solid)");
		// No city or immortal sect reaches toward it.
		for (int cx = Math.floorDiv(3200, ModNoiseSettings.CITY_CELL); cx <= Math.floorDiv(4800, ModNoiseSettings.CITY_CELL); cx++) {
			for (int cz = -2; cz <= 2; cz++) {
				ModNoiseSettings.CitySite city = ModNoiseSettings.citySite(state, cx, cz);
				helper.assertTrue(city == null || ChaosFrontier.out(city.x(), city.z()) + ModNoiseSettings.CITY_DISTRICT_OUTER < ModNoiseSettings.FRONTIER_FROM,
						"No city's district reaching into the Frontier (" + (city == null ? "" : city.x() + ", " + city.z()) + ")");
			}
		}
		for (int cx = Math.floorDiv(3200, ModNoiseSettings.SECT_CELL); cx <= Math.floorDiv(4800, ModNoiseSettings.SECT_CELL); cx++) {
			for (int cz = -2; cz <= 2; cz++) {
				ModNoiseSettings.CitySite sect = ModNoiseSettings.sectSite(state, cx, cz);
				helper.assertTrue(sect == null || ChaosFrontier.out(sect.x(), sect.z()) + ModNoiseSettings.SECT_CLEARING_OUTER < ModNoiseSettings.FRONTIER_FROM,
						"No sect's clearing reaching into it (" + (sect == null ? "" : sect.x() + ", " + sect.z()) + ")");
			}
		}
		helper.succeed();
	}

	// --- Its storm ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "chaos_frontier_storm")
	public void theStormUnmakesTheUnsheltered(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL, "test-chaos-a"), q = survivor(helper, Realm.TRUE_IMMORTAL, "test-chaos-b");
		ServerPlayer mock = helper.makeMockServerPlayerInLevel();
		try {
			DamageSource storm = ModDamageTypes.chaosStorm(helper.getLevel());
			helper.assertTrue(storm.is(DamageTypeTags.BYPASSES_ARMOR) && storm.is(DamageTypeTags.BYPASSES_EFFECTS) && storm.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)
					&& HeavenPassives.untouchedByWards(storm) && GhostSystem.reaches(storm), "Through armour, effects, enchantments and every ward; the dead too");
			// A hundredth of one's health each second, armour or none.
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				p.setItemSlot(slot, new ItemStack(switch (slot) {
					case HEAD -> Items.NETHERITE_HELMET;
					case CHEST -> Items.NETHERITE_CHESTPLATE;
					case LEGS -> Items.NETHERITE_LEGGINGS;
					default -> Items.NETHERITE_BOOTS;
				}));
			}
			float max = p.getMaxHealth(), before = p.getHealth();
			ChaosFrontier.weather(p, true);
			helper.assertTrue(near(before - p.getHealth(), max * ChaosFrontier.STORM_SHARE, max * 0.001) && ChaosFrontier.exposed(p),
					"A hundredth of its health through netherite (" + (before - p.getHealth()) + " of " + max + "), unsheltered");
			// Nor does its body mend of itself, nor spend its food on it, while unsheltered.
			p.setHealth(max * 0.5f);
			p.getFoodData().setFoodLevel(20);
			p.getFoodData().setSaturation(20);
			for (int i = 0; i < 30; i++) p.getFoodData().tick(p);
			helper.assertTrue(p.getHealth() == max * 0.5f && p.getFoodData().getSaturationLevel() == 20, "No mending of itself in the storm (" + p.getHealth() + ")");
			ChaosFrontier.weather(p, false);
			for (int i = 0; i < 30; i++) p.getFoodData().tick(p);
			helper.assertTrue(!ChaosFrontier.exposed(p) && p.getHealth() > max * 0.5f, "Out of it, it mends again");
			// Sheltered within its own domain, or one whose owner is no foe; not within a foe's.
			Ability fire = Ability.FIRE_DOMAIN;
			for (ServerPlayer owner : List.of(p, q)) {
				PlayerCultivation c = CultivationManager.get(owner);
				c.laws().setLevel(Law.FIRE, 1);
				c.setEssence(ImmortalEssence.max(c));
			}
			CultivationManager.toggleAbility(p, fire.ordinal());
			LawDomains.tick(server);
			p.invulnerableTime = 0;
			before = p.getHealth();
			ChaosFrontier.weather(p, true);
			helper.assertTrue(p.getHealth() == before && !ChaosFrontier.exposed(p), "Within its own domain, sheltered");
			CultivationManager.toggleAbility(p, fire.ordinal());
			CultivationManager.toggleAbility(q, fire.ordinal());
			LawDomains.tick(server);
			ChaosFrontier.weather(p, true);
			helper.assertTrue(p.getHealth() == before && !ChaosFrontier.exposed(p), "Within a friend's domain, sheltered");
			q.setLastHurtByMob(p); // blows have passed between them
			LawDomains.tick(server);
			p.invulnerableTime = 0;
			ChaosFrontier.weather(p, true);
			helper.assertTrue(p.getHealth() < before && ChaosFrontier.exposed(p), "Within a foe's, the storm finds it");
			// Creative and spectating: none of it.
			ChaosFrontier.weather(mock, true);
			helper.assertTrue(!ChaosFrontier.exposed(mock), "A creative player untouched");
			helper.succeed();
		} finally {
			leave(p);
			leave(q);
			ChaosFrontier.onDisconnect(mock);
			server.setPvpAllowed(pvp);
		}
	}

	// --- Its fiends and its remnant ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "chaos_frontier_fiends")
	public void theFiends(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL, "test-chaos-c");
		try {
			BlockPos at = helper.absolutePos(new BlockPos(2, 3, 2));
			ChaosFiend fiend = ChaosFrontier.fiend(level, at, PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.MID));
			helper.assertTrue(fiend != null && fiend.kind() == BeastKind.CHAOS_FIEND && fiend.kind().isChaos() && fiend.kind().moves == BeastKind.Moves.FLYING,
					"A Chaos Fiend, come out of the air, flying");
			fiend.setNoAi(true);
			helper.assertTrue(fiend.getRank() == PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.MID), "Of the rank it was given");
			fiend.setRank(0);
			helper.assertTrue(fiend.getRank() == BeastKind.Tier.CHAOS.minRank, "Held to its tier (" + fiend.getRank() + ")");
			fiend.setRank(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.MID));
			helper.assertTrue(fiend.hunts(p), "It hunts a True Immortal");
			CultivationManager.get(p).setState(Realm.EARTH_IMMORTAL, Stage.MID, 0);
			helper.assertTrue(!fiend.hunts(p), "...and leaves one a realm above it alone");
			CultivationManager.get(p).setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
			helper.assertTrue(fiend.isInvulnerableTo(ModDamageTypes.chaosStorm(level)) && !fiend.isInvulnerableTo(level.damageSources().generic()), "The storm its own; struck, harmed");
			BeastShot bolt = new BeastShot(level, fiend, BeastShot.Kind.CHAOS, 10);
			helper.assertTrue(bolt.kind() == BeastShot.Kind.CHAOS && bolt.getGravity() == 0, "Its bolts fly straight");
			bolt.discard();
			// Its spoils: a Chaos Origin Stone four times in ten, a Dao Fragment one in twenty, floating where it fell.
			int stones = 0, fragments = 0, other = 0;
			boolean floats = true;
			for (int i = 0; i < 200; i++) fiend.dropCustomDeathLoot(level.damageSources().generic(), 0, true);
			for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3))) {
				ItemStack stack = item.getItem();
				floats &= item.isNoGravity();
				if (stack.is(ModItems.CHAOS_ORIGIN_STONE)) stones += stack.getCount();
				else if (stack.is(ModItems.DAO_FRAGMENT) && DaoFragmentItem.law(stack) != null) fragments++;
				else other++;
				item.discard();
			}
			helper.assertTrue(stones >= 55 && stones <= 105 && fragments >= 2 && fragments <= 24 && other == 0 && floats,
					"Stones about 40%, fragments about 5%, no core or hide, floating (" + stones + ", " + fragments + ", " + other + ")");
			// In the First Flame's light it burns away.
			FirstFlame.burnAway(level, fiend);
			helper.assertTrue(fiend.isRemoved(), "Burnt away");
			helper.succeed();
		} finally {
			leave(p);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "chaos_frontier_remnant")
	public void thePrimordialChaos(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL, "test-chaos-d"), far = survivor(helper, Realm.TRUE_IMMORTAL, "test-chaos-e");
		try {
			helper.assertTrue(PrimordialBeasts.KINDS.length == 4 && PrimordialBeasts.index(BeastKind.PRIMORDIAL_CHAOS) == 3, "The fourth primordial beast");
			PrimordialBeast chaos = (PrimordialBeast) ModEntities.beast(BeastKind.PRIMORDIAL_CHAOS).create(level);
			chaos.setRank(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION));
			helper.assertTrue(chaos.getRank() == BeastKind.CHAOS_RANK && chaos.getCultivationRealm() == Realm.GOLDEN_IMMORTAL && chaos.body() == BeastKind.Body.CHAOS,
					"A Golden Immortal of Grand Perfection, whatever it is given (" + chaos.getRank() + ")");
			helper.assertTrue(chaos.scale() == 1 && chaos.getBbWidth() >= 18, "Its size its kind's, vast");
			// Its spoils: six Dao Fragments of six Laws, twelve Chaos Origin Stones, twelve Immortal Jade, hanging where it fell.
			BlockPos at = helper.absolutePos(new BlockPos(2, 3, 2));
			chaos.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			chaos.dropCustomDeathLoot(level.damageSources().generic(), 0, true);
			Set<Law> laws = EnumSet.noneOf(Law.class);
			int fragments = 0, stones = 0, jade = 0, other = 0;
			for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3))) {
				ItemStack stack = item.getItem();
				if (stack.is(ModItems.DAO_FRAGMENT)) {
					fragments += stack.getCount();
					laws.add(DaoFragmentItem.law(stack));
				} else if (stack.is(ModItems.CHAOS_ORIGIN_STONE)) stones += stack.getCount();
				else if (stack.is(ModItems.IMMORTAL_JADE)) jade += stack.getCount();
				else other++;
				item.discard();
			}
			helper.assertTrue(fragments == PrimordialBeast.CHAOS_FRAGMENTS && laws.size() == PrimordialBeast.CHAOS_FRAGMENTS && stones == PrimordialBeast.CHAOS_STONES
					&& jade == PrimordialBeast.CHAOS_JADE && other == 0, "Six fragments of six Laws, twelve stones, twelve jade (" + fragments + ", " + laws.size()
					+ ", " + stones + ", " + jade + ", " + other + ")");
			// The Court pays twice a primordial beast's for it.
			CelestialCourt.join(p);
			int before = CelestialCourt.meritOf(server, p.getUUID());
			chaos.settle(BlockPos.ZERO);
			CelestialCourt.onPrimordialSlain(chaos, Set.of(p.getUUID()));
			helper.assertTrue(CelestialCourt.meritOf(server, p.getUUID()) == before + CelestialCourt.SLAY_CHAOS && CelestialCourt.SLAY_CHAOS == 2 * CelestialCourt.SLAY_PRIMORDIAL,
					"Forty merit for the Primordial Chaos fought");
			chaos.discard();
			// Its Unmaking folds away every domain whose owner it catches, none beyond its reach.
			for (ServerPlayer owner : List.of(p, far)) {
				PlayerCultivation c = CultivationManager.get(owner);
				c.laws().setLevel(Law.WATER, 1);
				c.setEssence(ImmortalEssence.max(c));
				CultivationManager.toggleAbility(owner, Ability.WATER_DOMAIN.ordinal());
			}
			far.teleportTo(level, p.getX() + PrimordialBeast.UNMAKING_REACH + 8, p.getY(), p.getZ(), 0, 0);
			LawDomains.tick(server);
			int folded = LawDomains.unmake(level, LawDomains.centre(p), PrimordialBeast.UNMAKING_REACH);
			helper.assertTrue(folded == 1 && LawDomains.lit(CultivationManager.get(p)) == null && LawDomains.lit(CultivationManager.get(far)) == Law.WATER,
					"The domain it catches folded away, the one beyond it standing (" + folded + ")");
			helper.succeed();
		} finally {
			leave(p);
			leave(far);
		}
	}

	// --- Dao Fragments ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "chaos_frontier_fragments")
	public void daoFragments(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL, "test-chaos-f");
		try {
			for (Law law : Law.values()) {
				ItemStack stack = DaoFragmentItem.of(law);
				helper.assertTrue(DaoFragmentItem.law(stack) == law && DaoFragmentItem.tint(stack) == law.color()
						&& stack.getHoverName().getContents() instanceof TranslatableContents t && t.getKey().equals(ModLang.ITEM_DAO_FRAGMENT_OF),
						law.getId() + ": its fragment, named for it, its colour");
			}
			PlayerCultivation c = CultivationManager.get(p);
			double half = Laws.FRAGMENT_SHARE * Laws.insightFor(1);
			helper.assertTrue(Laws.absorb(p, Law.FIRE) && near(c.laws().insight(Law.FIRE), half, 1e-6) && c.laws().level(Law.FIRE) == 0,
					"Half a first level's insight into Fire (" + c.laws().insight(Law.FIRE) + ")");
			helper.assertTrue(Laws.absorb(p, Law.FIRE) && c.laws().level(Law.FIRE) == 1, "Two of them, its first level");
			helper.assertTrue(!Laws.absorb(p, Law.SPACE) && c.laws().insight(Law.SPACE) == 0, "A greater Law's refused short of Golden Immortal");
			c.setState(Realm.GOLDEN_IMMORTAL, Stage.EARLY, 0);
			helper.assertTrue(Laws.absorb(p, Law.SPACE) && near(c.laws().insight(Law.SPACE), half, 1e-6), "...and taken in from it");
			c.laws().setLevel(Law.WATER, Laws.MAX_LEVEL);
			helper.assertTrue(!Laws.absorb(p, Law.WATER), "Nothing more for a Law comprehended whole");
			// Used, it is spent; refused, kept.
			p.setItemInHand(InteractionHand.MAIN_HAND, DaoFragmentItem.of(Law.EARTH).copyWithCount(2));
			p.getMainHandItem().use(p.level(), p, InteractionHand.MAIN_HAND);
			helper.assertTrue(p.getMainHandItem().getCount() == 1 && c.laws().insight(Law.EARTH) > 0, "Used: one spent");
			p.setItemInHand(InteractionHand.MAIN_HAND, DaoFragmentItem.of(Law.WATER));
			p.getMainHandItem().use(p.level(), p, InteractionHand.MAIN_HAND);
			helper.assertTrue(p.getMainHandItem().getCount() == 1, "Refused: kept");
			helper.succeed();
		} finally {
			leave(p);
		}
	}

	// --- The First Flame ---

	@GameTest(template = EMPTY_STRUCTURE, batch = "chaos_frontier_flame")
	public void theFirstFlamesBasin(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockState flame = ModBlocks.FIRST_FLAME.defaultBlockState();
		helper.assertTrue(flame.getLightEmission() == 15 && flame.getCollisionShape(level, BlockPos.ZERO).isEmpty() && flame.getDestroySpeed(level, BlockPos.ZERO) < 0,
				"The First Flame: the brightest light, nothing to stand on, taken by no one");
		BlockPos corner = helper.absolutePos(new BlockPos(4, 1, 4));
		int x = corner.getX(), z = corner.getZ(), floorY = corner.getY() + 4;
		try {
			BlockPos floor = FirstFlame.lay(level, x, z, floorY);
			helper.assertTrue(floor.getY() == floorY && level.getBlockState(floor).is(Blocks.SMOOTH_QUARTZ), "Its floor of smooth quartz, on a floor of its own where no land is");
			helper.assertTrue(level.getBlockState(floor.above()).is(ModBlocks.FIRST_FLAME) && level.getBlockState(floor.above(2)).is(ModBlocks.FIRST_FLAME)
					&& level.getBlockState(floor.offset(2, 1, 0)).is(ModBlocks.FIRST_FLAME) && level.getBlockState(floor.offset(2, 1, 2)).isAir(),
					"The flame on its bed, the higher in its middle, the floor clear about it");
			for (BlockPos quarter : List.of(floor.offset(4, 0, 0), floor.offset(-4, 0, 0), floor.offset(0, 0, 4), floor.offset(0, 0, -4))) {
				helper.assertTrue(level.getBlockState(quarter.above()).is(Blocks.QUARTZ_PILLAR) && level.getBlockState(quarter.above(3)).is(Blocks.GOLD_BLOCK),
						"A post at each quarter, gold on it (" + quarter.toShortString() + ")");
			}
			helper.assertTrue(level.getBlockState(floor.offset(3, 1, 3)).is(Blocks.QUARTZ_BRICKS) && level.getBlockState(floor.offset(4, 1, 1)).is(Blocks.QUARTZ_BRICKS),
					"A ring of quartz bricks round it");
			helper.succeed();
		} finally {
			for (int dx = -7; dx <= 7; dx++) {
				for (int dz = -7; dz <= 7; dz++) {
					for (int y = floorY - 3; y <= floorY + 8; y++) level.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.AIR.defaultBlockState(), 2);
				}
			}
			for (Entity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(corner).inflate(10))) e.discard();
		}
	}
}
