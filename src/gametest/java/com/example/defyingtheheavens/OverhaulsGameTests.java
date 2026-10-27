package com.example.defyingtheheavens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The overhauls of 2026-10-27 (the user's batch). The spells' blocks a crash left standing are taken up when the server next starts
 * (HeavenSpells.Land). Cultivators call a player by the rung they stand on before it (NpcAddress: a Fellow Daoist, a Senior, an Esteemed
 * or Revered Senior, an Ancestor, a Lord Immortal; a junior in its own voice; never a Venerable nor a Patriarch), kneel from four realms
 * beneath them, and call one who has taken a title by it (PlayerTitles: from Void Shattering on, cleaned, at most fifty long, kept with
 * them, on their plate); what they say is written for every rung and voice, and of what they see. A crater blasted into the Great Void's
 * battlefields keeps within its feature's reach; their ghostly qi is ten times the Netherworld's. The Great Void's currents keep clear of
 * its sects' grounds.
 */
public class OverhaulsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void theSpellsLandIsTakenUpAfterACrash(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 3, 1));
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		helper.assertTrue(HeavenSpells.placeTemporary(level, pos, Blocks.PACKED_ICE.defaultBlockState(), 1200), "A spell's ice is set");
		helper.assertTrue(HeavenSpells.Land.of(level).holds(pos), "...and kept in the level's own record");
		// Its record as written out and read back, as a server starting after a crash reads it.
		HeavenSpells.Land read = HeavenSpells.Land.load(HeavenSpells.Land.of(level).save(new CompoundTag()));
		helper.assertTrue(read.holds(pos), "The record is read back as it was written");
		// The crash: the server's hold on it lost, the ice still standing.
		HeavenSpells.forgetForTests(level, pos);
		helper.assertTrue(!HeavenSpells.isTemporary(level, pos) && level.getBlockState(pos).is(Blocks.PACKED_ICE), "A crash leaves the ice standing, forgotten");
		helper.assertTrue(HeavenSpells.restoreLeftLand(level.getServer()) >= 1, "Starting again, what was left standing is taken up");
		helper.assertTrue(level.getBlockState(pos).isAir(), "...the air it stood in given back");
		helper.assertTrue(!HeavenSpells.Land.of(level).holds(pos), "...and struck off the record");
		// One the server still holds is left to its own time.
		helper.assertTrue(HeavenSpells.placeTemporary(level, pos, Blocks.PACKED_ICE.defaultBlockState(), 1200), "Another is set");
		HeavenSpells.restoreLeftLand(level.getServer());
		helper.assertTrue(level.getBlockState(pos).is(Blocks.PACKED_ICE) && HeavenSpells.isTemporary(level, pos), "One still held is left to its time");
		HeavenSpells.removeTemporary(level, pos);
		helper.assertTrue(level.getBlockState(pos).isAir() && !HeavenSpells.Land.of(level).holds(pos), "Taken up in its time, it is struck off too");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void cultivatorsCallYouByHowFarAboveThemYouStand(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			CultivatorNpc refiner = npc(helper, new BlockPos(3, 2, 2), Realm.QI_REFINING, 100);
			Object[][] ladder = {
					{Realm.QI_REFINING, NpcAddress.Rung.PEER, "peer"},
					{Realm.FOUNDATION_BUILDING, NpcAddress.Rung.SENIOR, "senior"},
					{Realm.CORE_FORMATION, NpcAddress.Rung.ESTEEMED, "esteemed"},
					{Realm.NASCENT_SOUL, NpcAddress.Rung.ESTEEMED, "esteemed"},
					{Realm.HEAVENLY_BEING, NpcAddress.Rung.REVERED, "revered"},
					{Realm.FOUR_AXIS, NpcAddress.Rung.REVERED, "revered"},
					// Held to Heavenly Being in the lower realms (PlayerCultivation#LOWER_REALM_CAP_REALM), and sensed so; from Void
					// Shattering on none is held.
					{Realm.DAO_INTEGRATION, NpcAddress.Rung.REVERED, "revered"},
					{Realm.VOID_SHATTERING, NpcAddress.Rung.ANCESTOR, "ancestor"},
					{Realm.SACRED_VESSEL, NpcAddress.Rung.ANCESTOR, "ancestor"},
					{Realm.TRUE_IMMORTAL, NpcAddress.Rung.ANCESTOR, "lord_immortal"}};
			for (Object[] rung : ladder) {
				ServerPlayer p = cultivator(helper, made, "climber", (Realm) rung[0], Stage.EARLY);
				helper.assertTrue(NpcAddress.rung(p, refiner) == rung[1], rung[0] + " before Qi Refining stands on the rung " + rung[1]);
				helper.assertTrue(ModLang.address((String) rung[2]).equals(key(NpcAddress.address(p, refiner))), "...and is called " + rung[2]);
				boolean kneels = NpcAddress.gap(p, refiner) >= NpcAddress.KNEEL_GAP;
				helper.assertTrue(TalkSystem.kneelsTo(p, refiner) == kneels, "It kneels to " + rung[0] + " only from four realms beneath them");
				leave(made);
				made.clear();
			}
			// Passing close, it kneels to one four realms above it, and not to one three.
			ServerPlayer revered = cultivator(helper, made, "revered", Realm.HEAVENLY_BEING, Stage.EARLY);
			helper.assertTrue(TalkSystem.kneelsBefore(refiner, null) == revered, "It kneels before one four realms above it as they pass");
			// Let up (Release pressure), it stands before them for the five minutes, four realms or no (the user, 2026-10-27).
			RealmSuppressSystem.spare(revered, refiner);
			helper.assertTrue(!TalkSystem.kneelsTo(revered, refiner) && TalkSystem.kneelsBefore(refiner, null) == null,
					"Released, it gets up and stays up");
			leave(made);
			made.clear();
			ServerPlayer esteemed = cultivator(helper, made, "esteemed", Realm.NASCENT_SOUL, Stage.EARLY);
			helper.assertTrue(TalkSystem.kneelsBefore(refiner, null) == null, "...and only bows to one three above");
			// Above the player, it calls them a junior in its own voice; a mortal player, a mortal. (Below Void Shattering it, too, is held to
			// Heavenly Being in the lower realms, and shows itself so: CultivatorNpc#sustainedRank.)
			ServerPlayer refining = cultivator(helper, made, "refining", Realm.QI_REFINING, Stage.EARLY);
			CultivatorNpc devil = npc(helper, new BlockPos(4, 2, 4), Realm.CORE_FORMATION, -120);
			helper.assertTrue(NpcAddress.rung(refining, devil) == NpcAddress.Rung.FAR_ABOVE
					&& ModLang.address("far_above.devilish").equals(key(NpcAddress.address(refining, devil))), "A devil two realms above calls one an ant");
			CultivatorNpc mentor = npc(helper, new BlockPos(1, 2, 4), Realm.FOUNDATION_BUILDING, 100);
			helper.assertTrue(NpcAddress.rung(refining, mentor) == NpcAddress.Rung.ABOVE
					&& ModLang.address("above.righteous").equals(key(NpcAddress.address(refining, mentor))), "A righteous one a realm above, a young friend");
			ServerPlayer mortal = player(helper, made, "mortal");
			helper.assertTrue(CultivationManager.get(mortal).isMortal() && NpcAddress.rung(mortal, mentor) == NpcAddress.Rung.MORTAL
					&& ModLang.address("mortal.righteous").equals(key(NpcAddress.address(mortal, mentor))), "A mortal is a young one to it");
			// A mortal calls any cultivator Immortal, by name; another mortal, friend.
			CultivatorNpc farmer = npc(helper, new BlockPos(4, 2, 1), null, 0);
			Component immortal = NpcAddress.address(esteemed, farmer);
			helper.assertTrue(ModLang.address("immortal").equals(key(immortal)) && immortal.getContents() instanceof TranslatableContents t
					&& t.getArgs().length == 1 && "esteemed".equals(String.valueOf(t.getArgs()[0])), "A mortal calls a cultivator Immortal, by name");
			helper.assertTrue(ModLang.address("friend").equals(key(NpcAddress.address(mortal, farmer))), "...and another mortal, friend");
			// Every word of address is in the lang file, and none is a rank no player holds.
			JsonObject lang = lang();
			String prefix = ModLang.address("");
			int words = 0;
			for (Map.Entry<String, com.google.gson.JsonElement> entry : lang.entrySet()) {
				if (!entry.getKey().startsWith(prefix)) continue;
				words++;
				String said = entry.getValue().getAsString().toLowerCase();
				helper.assertTrue(!said.contains("venerable") && !said.contains("patriarch"), "No one is called a Venerable or a Patriarch: " + entry.getKey());
			}
			helper.assertTrue(words >= 24, "Every word of address is in the lang file (" + words + ")");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTitleIsTakenFromVoidShattering(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			helper.assertTrue(PlayerTitles.clean("  §6Sword \u0007  Saint  of\tthe North ").equals("Sword Saint of the North"), "A title is cleaned of codes and spaces");
			String long60 = "Sixty".repeat(12);
			helper.assertTrue(PlayerTitles.clean(long60).length() == PlayerTitles.MAX_LENGTH && PlayerTitles.MAX_LENGTH == 50, "A title is at most fifty long");
			ServerPlayer below = cultivator(helper, made, "below", Realm.DAO_INTEGRATION, Stage.LATE);
			PlayerTitles.set(below, "Too Soon");
			helper.assertTrue(CultivationManager.get(below).epithet().isEmpty(), "Below Void Shattering, no title is taken");
			CultivationManager.get(below).setEpithet("Kept Unseen");
			helper.assertTrue(PlayerTitles.shown(below).isEmpty() && PlayerTitles.plateOf(below).title().isEmpty(), "...nor one kept shown");
			ServerPlayer shatterer = cultivator(helper, made, "shatterer", Realm.VOID_SHATTERING, Stage.EARLY);
			PlayerTitles.set(shatterer, "  Sword   Saint ");
			PlayerCultivation c = CultivationManager.get(shatterer);
			helper.assertTrue(c.epithet().equals("Sword Saint") && PlayerTitles.shown(shatterer).equals("Sword Saint"), "From Void Shattering on, one is taken");
			helper.assertTrue(PlayerCultivation.load(c.save()).epithet().equals("Sword Saint"), "...and kept with them");
			PlayerTitles.Plate plate = PlayerTitles.plateOf(shatterer);
			helper.assertTrue(plate.title().equals("Sword Saint") && plate.sectTitle() == -1, "It is on their plate, with no sect's title under it");
			// Cultivators call them by it, bare: no Senior, no Ancestor.
			CultivatorNpc refiner = npc(helper, new BlockPos(3, 2, 2), Realm.QI_REFINING, 100);
			helper.assertTrue(NpcAddress.address(shatterer, refiner).getString().equals("Sword Saint"), "A junior calls them by their title");
			helper.assertTrue(NpcAddress.name(shatterer).equals("Sword Saint"), "...and names them by it");
			CultivatorNpc elder = npc(helper, new BlockPos(1, 2, 4), Realm.ENTERING_NIRVANA, -120);
			Component junior = NpcAddress.address(shatterer, elder);
			helper.assertTrue(ModLang.address("titled_junior.devilish").equals(key(junior)) && junior.getContents() instanceof TranslatableContents t
					&& "Sword Saint".equals(String.valueOf(t.getArgs()[0])), "One far above them calls them by it too, as a junior");
			PlayerTitles.set(shatterer, "   ");
			helper.assertTrue(c.epithet().isEmpty() && key(NpcAddress.address(shatterer, refiner)).equals(ModLang.address("ancestor")),
					"Emptied, it is cleared, and they are an Ancestor to a Qi Refiner again");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void whatTheySayIsByRungAndTitle(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			// Every rung's lines in every voice that can meet it: in passing, and in talk.
			Map<String, String> english = NpcLines.english();
			String[] voices = {"righteous", "neutral", "devilish", "ghost"};
			for (NpcAddress.Rung rung : NpcAddress.Rung.values()) {
				List<String> them = new ArrayList<>(List.of(voices));
				if (rung.above() || rung == NpcAddress.Rung.PEER) them.add("commoner");
				for (String voice : them) {
					if (rung != NpcAddress.Rung.PEER && rung != NpcAddress.Rung.MORTAL) {
						helper.assertTrue(english.containsKey(ModLang.npcSay("pass." + rung.id() + "." + voice + ".1")), "A passing line for " + rung + " in " + voice);
					}
					for (String kind : List.of("talk.greet.", "talk.greet.liked.", "talk.ask.")) {
						if (rung == NpcAddress.Rung.MORTAL && voice.equals("commoner")) continue;
						helper.assertTrue(english.containsKey(ModLang.npcSay(kind + rung.id() + "." + voice + ".1")), kind + rung.id() + " in " + voice);
					}
				}
			}
			// A titled one passing a Qi Refiner: it puts its forehead to the ground, calling them by their title.
			ServerPlayer shatterer = cultivator(helper, made, "shatterer", Realm.VOID_SHATTERING, Stage.EARLY);
			CultivationManager.get(shatterer).setEpithet("Azure Dragon Sovereign");
			CultivatorNpc refiner = npc(helper, new BlockPos(3, 2, 2), Realm.QI_REFINING, 100);
			Component passing = NpcLife.remarkOn(refiner, shatterer, false);
			helper.assertTrue(key(passing).startsWith(ModLang.npcSay("pass.ancestor.righteous.")), "Seven realms beneath them it bows its head to the ground");
			helper.assertTrue(argSaid(passing, 0).equals("Azure Dragon Sovereign"), "...calling them by their title");
			Component greeting = TalkSystem.lineFor("greet", refiner, shatterer, TalkSystem.standing(shatterer, refiner));
			helper.assertTrue(key(greeting).startsWith(ModLang.npcSay("talk.greet.ancestor.righteous.")) && argSaid(greeting, 0).equals("Azure Dragon Sovereign"),
					"Its answer to a greeting calls them by it too");
			Component asked = TalkSystem.lineFor("ask", refiner, shatterer, TalkSystem.standing(shatterer, refiner));
			helper.assertTrue(key(asked).startsWith(ModLang.npcSay("talk.ask.ancestor.righteous.")), "...and its answer when asked");
			// It speaks of what it sees only on one it needn't bow low to: a peer's wounds.
			ServerPlayer hurt = cultivator(helper, made, "hurt", Realm.QI_REFINING, Stage.LATE);
			hurt.setHealth(hurt.getMaxHealth() * 0.2f);
			int wounds = 0;
			for (int i = 0; i < 40; i++) {
				String said = key(NpcLife.remarkOn(refiner, hurt, false));
				if (said != null && said.startsWith(ModLang.npcSay("pass.hurt."))) wounds++;
			}
			helper.assertTrue(wounds > 0, "A peer's wounds are remarked on");
			shatterer.setHealth(shatterer.getMaxHealth() * 0.2f);
			for (int i = 0; i < 20; i++) {
				String said = key(NpcLife.remarkOn(refiner, shatterer, false));
				helper.assertTrue(said == null || !said.startsWith(ModLang.npcSay("pass.hurt.")), "...but not those of one it kneels to");
			}
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ghostlyQiTeemsOnTheBattlefields(GameTestHelper helper) {
		Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		Holder<Biome> field = biomes.getHolderOrThrow(ModBiomes.ANCIENT_BATTLEFIELD), shore = biomes.getHolderOrThrow(ModBiomes.SHORE_OF_NIRVANA);
		helper.assertTrue(GhostQi.density(ModDimensions.GREAT_VOID, field) == 100 && GhostQi.ANCIENT_BATTLEFIELD == GhostQi.NETHERWORLD * 10,
				"The Great Void's battlefields: x100, ten times the Netherworld's");
		helper.assertTrue(GhostQi.density(ModDimensions.GREAT_VOID, shore) == 0, "...and none elsewhere in the Great Void");
		helper.assertTrue(GhostQi.fillPerSecond(GhostQi.ANCIENT_BATTLEFIELD) == 20 && GhostQi.word(GhostQi.ANCIENT_BATTLEFIELD).equals("teeming"),
				"The pool fills at 20 a second there; it is teeming");
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		double there = GhostMethods.ratePerSecond(c, GhostQi.ANCIENT_BATTLEFIELD), dead = GhostMethods.ratePerSecond(c, GhostQi.NETHERWORLD);
		helper.assertTrue(dead > 0 && Math.abs(there / dead - 10) < 1e-9, "Ghost methods cultivate ten times the Netherworld's pace there");
		helper.assertTrue(Math.abs(Lifespan.ghostPlaceSaving(GhostQi.ANCIENT_BATTLEFIELD) - 10) < 1e-9, "...and ten times the years are saved");
		// In the Great Void itself, where a battlefield lies.
		ServerLevel greatVoid = helper.getLevel().getServer().getLevel(ModDimensions.GREAT_VOID);
		helper.assertTrue(greatVoid != null, "The Great Void is there to ask");
		com.mojang.datafixers.util.Pair<BlockPos, Holder<Biome>> found = greatVoid.findClosestBiome3d(b -> b.is(ModBiomes.ANCIENT_BATTLEFIELD),
				new BlockPos(NirvanaCamp.X, 200, NirvanaCamp.Z), 6400, 32, 64);
		helper.assertTrue(found != null && GhostQi.densityAt(greatVoid, found.getFirst()) == 100, "A battlefield in the Great Void is teeming with it");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void voidCurrentsKeepClearOfSects(GameTestHelper helper) {
		Sect holy = new Sect(UUID.randomUUID());
		holy.core = new BlockPos(1000, 200, 0);
		holy.barrierRadius = 136;
		List<Sect> sects = List.of(holy);
		double reach = holy.barrierRadius + VoidCurrents.CLEAR_OF_SECTS;
		helper.assertTrue(VoidCurrents.nearSect(sects, 1000.5 + reach - 2, 0.5) && !VoidCurrents.nearSect(sects, 1000.5 + reach + 2, 0.5),
				"No current within a Holy Land's barrier, nor a little beyond it");
		// Born a little way west of that, flowing east into it (yaw -90) it would come near; flowing west, away, it never does; born far
		// enough off, it never reaches it in its life.
		Vec3 near = new Vec3(1000.5 - reach - 60, 210, 0.5), far = new Vec3(1000.5 - reach - 200, 210, 0.5);
		helper.assertTrue(VoidCurrents.pathNearSect(sects, near, -90f) && !VoidCurrents.pathNearSect(sects, near, 90f)
				&& !VoidCurrents.pathNearSect(sects, far, -90f), "A current is born only where its way over its life keeps clear of a sect");
		// One come up against a sect all the same shuts there.
		SpatialTearEntity current = SpatialTearEntity.create(helper.getLevel(), near, -90f, SpatialTearEntity.Leads.VOID_CURRENT);
		current.holdOpen(VoidCurrents.LIFE);
		current.closeSoon();
		helper.assertTrue(current.life() == current.tickCount + SpatialTearEntity.CLOSE_TICKS, "...it closes at once (" + current.life() + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aCraterIsBlastedIntoTheGround(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		// Well away from the other tests' plots, on a block of stone laid for it.
		int x = origin.getX() + 3000, z = origin.getZ() + 3000, top = 150, side = 20, deep = 10;
		BlockState stone = Blocks.STONE.defaultBlockState();
		for (int dx = -side; dx <= side; dx++) {
			for (int dz = -side; dz <= side; dz++) {
				for (int y = top - deep; y <= top; y++) level.setBlock(new BlockPos(x + dx, y, z + dz), stone, 2);
			}
		}
		try {
			// A great one, its rim and its cracks reaching for the edge of what a feature may touch.
			BattlefieldRuinFeature.craterForTests(level, new BlockPos(x, top + 1, z), 12, RandomSource.create(7));
			BlockState heart = level.getBlockState(new BlockPos(x, top, z));
			helper.assertTrue(heart.isAir() || heart.is(Blocks.WATER), "Its heart is blasted out of the ground");
			int burnt = 0, rim = 0;
			for (int dx = -side; dx <= side; dx++) {
				for (int dz = -side; dz <= side; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					for (int y = top - deep; y <= top + 4; y++) {
						BlockState state = level.getBlockState(new BlockPos(x + dx, y, z + dz));
						if (d < 12 && (state.is(Blocks.BLACKSTONE) || state.is(Blocks.BASALT) || state.is(Blocks.OBSIDIAN) || state.is(Blocks.MAGMA_BLOCK))) burnt++;
						if (d >= 11 && d <= 17 && y > top && !state.isAir()) rim++;
					}
					// Nothing laid past the feature's reach (its 3x3 chunks).
					if (Math.max(Math.abs(dx), Math.abs(dz)) > BattlefieldRuinFeature.REACH) {
						helper.assertTrue(level.getBlockState(new BlockPos(x + dx, top, z + dz)).is(Blocks.STONE), "Nothing is dug past its reach");
						for (int y = top + 1; y <= top + 6; y++) {
							helper.assertTrue(level.getBlockState(new BlockPos(x + dx, y, z + dz)).isAir(), "...nor thrown past it");
						}
					}
				}
			}
			helper.assertTrue(burnt > 40, "Its floor is burnt (" + burnt + ")");
			helper.assertTrue(rim > 20, "A rim is thrown up round it (" + rim + ")");
			helper.succeed();
		} finally {
			BlockState air = Blocks.AIR.defaultBlockState();
			for (int dx = -side; dx <= side; dx++) {
				for (int dz = -side; dz <= side; dz++) {
					for (int y = top - deep - 1; y <= top + 12; y++) level.setBlock(new BlockPos(x + dx, y, z + dz), air, 2);
				}
			}
		}
	}

	// --- Helpers ---

	/** The English words, as the game's own resources hold them. */
	private static JsonObject lang() {
		try (InputStream in = DefyingTheHeavens.class.getResourceAsStream("/assets/" + DefyingTheHeavens.MOD_ID + "/lang/en_us.json")) {
			if (in == null) return new JsonObject();
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			return new JsonObject();
		}
	}

	private static String key(Component line) {
		return line != null && line.getContents() instanceof TranslatableContents t ? t.getKey() : null;
	}

	/** What a line's {@code n}th argument says ("" if there is none). */
	private static String argSaid(Component line, int n) {
		if (line == null || !(line.getContents() instanceof TranslatableContents t) || t.getArgs().length <= n) return "";
		Object arg = t.getArgs()[n];
		return arg instanceof Component c ? c.getString() : String.valueOf(arg);
	}

	/** A cultivator NPC of {@code realm} (Early; null for a commoner; past the census's room) and {@code alignment}, with no will of its own. */
	private static CultivatorNpc npc(GameTestHelper helper, BlockPos pos, Realm realm, int alignment) {
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, pos);
		npc.setRank(realm == null ? CultivatorNpc.MORTAL : PlayerCultivation.rank(realm, Stage.EARLY), true);
		npc.setAlignment(alignment);
		npc.setHealth(npc.getMaxHealth());
		return npc;
	}

	/** A player (a mortal, as all begin) beside the test's NPCs. */
	private static ServerPlayer player(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() {
				return false;
			}

			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		made.add(p);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		return p;
	}

	private static ServerPlayer cultivator(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm, Stage stage) {
		ServerPlayer p = player(helper, made, name);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, stage, 0);
		CultivationStats.apply(p, c);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) p.server.getPlayerList().remove(p);
	}
}
