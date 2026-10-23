package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The demon clans (Sect#race, DenStructure, DenPieces): every race's den laid out with the posts its people need, drawn with nothing
 * a spring opens in; its people of its race (never mortals, no Ritual to the Heavens, its element); wary of men; and ascended
 * demons going to clans, their own race's first.
 */
public class ClanGameTests implements FabricGameTest {
	private static final Bloodline[] RACES = {Bloodline.TIGER, Bloodline.LIZARD, Bloodline.FOX, Bloodline.DRAGON};

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyRacesDenHoldsItsClan(GameTestHelper helper) {
		for (Bloodline race : RACES) {
			for (int size = 0; size <= 1; size++) {
				for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST}) {
					List<StructurePiece> pieces = DenStructure.layOut(race.ordinal() * 7919L + size * 31 + facing.ordinal(), size, facing, false, race);
					String what = race.getId() + " size " + size + " " + facing;
					CompoundTag blueprint = null;
					BoundingBox grounds = null;
					int halls = 0, crags = 0, hoards = 0, burrows = 0, elders = 0, rows = 0;
					List<BoundingBox> buildings = new ArrayList<>();
					for (StructurePiece piece : pieces) {
						SectPiece sect = (SectPiece) piece;
						switch (sect.kind()) {
							case PLAZA -> blueprint = sect.extra().getCompound("Blueprint");
							case TERRACE -> grounds = sect.getBoundingBox();
							case DEN_HALL -> halls++;
							case DEN_CRAG -> crags++;
							case DEN_HOARD -> hoards++;
							case DEN_BURROW -> burrows++;
							case DEN_ELDER -> elders++;
							case DEN_FEATURE -> rows += "torii_row".equals(sect.extra().getString("Feature")) ? 1 : 0;
							default -> {
							}
						}
						if (sect.kind() != SectPiece.Kind.TERRACE && sect.kind() != SectPiece.Kind.PLAZA) {
							for (BoundingBox other : buildings) {
								helper.assertTrue(!other.intersects(sect.getBoundingBox()), "Nothing of a den stands on another (" + what + ", " + sect.kind() + ")");
							}
							buildings.add(sect.getBoundingBox());
						}
					}
					helper.assertTrue(blueprint != null && race.getId().equals(blueprint.getString("Race")), "The core's blueprint names the race (" + what + ")");
					if (race == Bloodline.DRAGON) {
						helper.assertTrue(crags == 1 && halls == 0 && hoards == 0 && burrows == 0, "A dragon clan's all under its crag (" + what + ")");
					} else {
						helper.assertTrue(halls == 1 && hoards == 1 && burrows == 1 && elders >= 2, "A head's den, a hoard, a burrow and elders' dens (" + what
								+ ": " + halls + " " + hoards + " " + burrows + " " + elders + ")");
					}
					helper.assertTrue(race != Bloodline.FOX || rows == 1, "A fox clan's tunnel of torii up its way (" + what + ")");
					CompoundTag posts = blueprint.getCompound("Posts");
					int homes = posts.getLongArray("DWELLING").length;
					helper.assertTrue(homes >= SectStructure.maxMembers(size) - 1, "A home for every member but the head (" + homes + ", " + what + ")");
					helper.assertTrue(posts.getLongArray("MASTER_SEAT").length == 1 && posts.getLongArray("TREASURY").length == 3
							&& posts.getLongArray("CORE").length == 4 && posts.getLongArray("GATE").length == 1 && posts.getLongArray("GATE_GUARD").length == 6,
							"Its seat, hoard, core, gate and guards' posts (" + what + ")");
					for (String post : posts.getAllKeys()) {
						for (long at : posts.getLongArray(post)) {
							BlockPos pos = BlockPos.of(at);
							helper.assertTrue(pos.getX() >= grounds.minX() - 4 && pos.getX() <= grounds.maxX() + 4 && pos.getZ() >= grounds.minZ() - 4
									&& pos.getZ() <= grounds.maxZ() + 4, "Every post on its grounds (" + post + ", " + what + ")");
						}
					}
					helper.assertTrue(blueprint.getInt("Radius") <= Formations.MAX_RADIUS, "Its barrier no wider than a barrier can be (" + what + ")");
				}
			}
		}
		helper.succeed();
	}

	/** What a spring opens in (vanilla's spring_water and spring_lava): none of it may be in what a den raises. */
	private static boolean springsOpenIn(BlockState state) {
		return state.is(Blocks.STONE) || state.is(Blocks.DIRT) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF) || state.is(Blocks.ANDESITE)
				|| state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || state.is(Blocks.CALCITE);
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void eachRacesHeadDenIsBuiltWhole(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int i = 0; i < RACES.length; i++) {
			Bloodline race = RACES[i];
			SectPiece head = null;
			for (StructurePiece piece : DenStructure.layOut(4242L + i, 0, Direction.SOUTH, false, race)) {
				SectPiece sect = (SectPiece) piece;
				if (sect.kind() == SectPiece.Kind.DEN_HALL || sect.kind() == SectPiece.Kind.DEN_CRAG) head = sect;
			}
			helper.assertTrue(head != null, "A head's den (" + race.getId() + ")");
			// Into the air high over the test, out of every other test's way, and drawn whole.
			BlockPos to = helper.absolutePos(new BlockPos(0, 70, 0)).offset(i * 48, 0, 0);
			BoundingBox box = head.getBoundingBox();
			head.move(to.getX() - box.minX(), to.getY() - box.minY(), to.getZ() - box.minZ());
			box = head.getBoundingBox();
			for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
				for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
					BoundingBox chunk = new BoundingBox(cx << 4, level.getMinBuildHeight(), cz << 4, (cx << 4) + 15, level.getMaxBuildHeight() - 1, (cz << 4) + 15);
					head.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), chunk, new ChunkPos(cx, cz), to);
				}
			}
			int[] seat = DenPieces.seat(head);
			helper.assertTrue(level.getBlockState(head.toWorld(seat[0], seat[1], seat[2])).is(ModBlocks.RED_MEDITATION_MAT)
					&& level.getBlockState(head.toWorld(seat[0], seat[1] + 1, seat[2])).isAir(), "The head's mat, room over it (" + race.getId() + ")");
			int cx = head.width() / 2;
			helper.assertTrue(level.getBlockState(head.toWorld(cx, 1, 0)).isAir() || level.getBlockState(head.toWorld(cx, 1, 0)).getBlock() instanceof net.minecraft.world.level.block.StairBlock,
					"Its way in open (" + race.getId() + ")");
			int springy = 0;
			for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY() + 1, box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
				if (springsOpenIn(level.getBlockState(pos))) springy++;
			}
			helper.assertTrue(springy == 0, "Nothing a spring opens in raised (" + springy + " blocks, " + race.getId() + ")");
			for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY() - 3, box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
				if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
			}
		}
		helper.succeed();
	}

	private static Sect clan(GameTestHelper helper, Bloodline race, int dx) {
		ServerLevel level = helper.getLevel();
		Sect sect = new Sect(UUID.randomUUID());
		sect.race = race;
		sect.name = SectNames.clan(level.getRandom(), race);
		sect.dimension = level.dimension();
		sect.core = helper.absolutePos(new BlockPos(1 + dx, 1, 1));
		sect.barrierRadius = 20;
		sect.capacity = 5;
		SectData.get(level.getServer()).put(sect);
		return sect;
	}

	private static ServerPlayer player(GameTestHelper helper, Bloodline race) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "test-clan")) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		Bloodlines.set(p.server, p.getUUID(), race, 0);
		PlayerCultivation c = CultivationManager.get(p);
		c.setMortal(false);
		c.setState(Realm.QI_REFINING, Stage.MID, 0);
		DemonPath.born(p);
		CultivationManager.refresh(p);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		return p;
	}

	private static void gone(GameTestHelper helper, ServerPlayer p, Sect... sects) {
		for (Sect sect : sects) {
			sect.extinct = true;
			sect.disciples.remove(p == null ? null : p.getUUID());
		}
		if (p != null) {
			Bloodlines.forget(p.server, p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aClansPeopleAreOfItsRace(GameTestHelper helper) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		npc.setNoAi(true);
		npc.setRank(CultivatorNpc.MORTAL);
		npc.setRace(Bloodline.TIGER, 2);
		helper.assertTrue(npc.isDemon() && !npc.isMortal() && npc.getBloodline() == Bloodline.TIGER && npc.getRaceVariant() == 2,
				"A demon is born with its core: never a mortal");
		npc.setRank(PlayerCultivation.RITUAL_RANK);
		npc.cultivation().setState(Realm.QI_REFINING, Stage.MID, npc.cultivation().cultivationRequired());
		helper.assertTrue(!npc.awaitsHeavensLeave(), "No Ritual to the Heavens on the Demon Path");
		CompoundTag saved = new CompoundTag();
		npc.addAdditionalSaveData(saved);
		CultivatorNpc again = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 1));
		again.setNoAi(true);
		again.readAdditionalSaveData(saved);
		helper.assertTrue(again.getBloodline() == Bloodline.TIGER && again.getRaceVariant() == 2, "Its race kept");
		Sect sect = clan(helper, Bloodline.FOX, 0);
		helper.assertTrue(sect.isClan() && sect.element() == QiElement.FIRE && sect.name.endsWith(" Fox Clan"), "A fox clan's Fire, its name (" + sect.name + ")");
		Sect loaded = Sect.load(sect.save());
		helper.assertTrue(loaded.race == Bloodline.FOX, "Its race kept with it");
		helper.assertTrue(NpcTitle.SECT_MASTER.getDisplayName(true).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
				&& t.getKey().equals(ModLang.titleKey("clan_head")), "A clan's master its Clan Head");
		npc.discard();
		again.discard();
		gone(helper, null, sect);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theClansAreWaryOfMen(GameTestHelper helper) {
		Sect sect = clan(helper, Bloodline.TIGER, 0);
		CultivatorNpc elder = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		elder.setNoAi(true);
		elder.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
		elder.setRace(Bloodline.TIGER, 0);
		elder.joinSect(sect, 0);
		Sect.Member m = new Sect.Member(elder.getUUID(), elder.getRank(), 0);
		m.title = NpcTitle.SECT_MASTER;
		sect.members.put(m.id, m);
		elder.setTitle(NpcTitle.SECT_MASTER);
		ServerPlayer man = player(helper, Bloodline.HUMAN), tiger = player(helper, Bloodline.TIGER), fox = player(helper, Bloodline.FOX);
		try {
			helper.assertTrue(elder.isEnemy(man) && !elder.isEnemy(tiger) && !elder.isEnemy(fox), "A man on its grounds is set upon; demons are let be");
			int toMan = elder.regard(man), toTiger = elder.regard(tiger);
			helper.assertTrue(toMan < toTiger - 30, "Cold to men, warm to its own (" + toMan + ", " + toTiger + ")");
			helper.assertTrue(Regard.bloodShift(Bloodline.TIGER, Bloodline.HUMAN) < 0 && Regard.bloodShift(Bloodline.TIGER, Bloodline.FOX) > 0
					&& Regard.bloodShift(Bloodline.TIGER, Bloodline.TIGER) > Regard.bloodShift(Bloodline.TIGER, Bloodline.FOX)
					&& Regard.bloodShift(Bloodline.HUMAN, Bloodline.TIGER) == 0, "A demon minds blood, its own most; a man none");
			Object[] arg = new Object[1];
			helper.assertTrue("join.human".equals(SectMembership.refusal(man, elder, sect, arg)), "A man isn't taken in");
			helper.assertTrue(SectMembership.refusal(tiger, elder, sect, arg) == null && SectMembership.refusal(fox, elder, sect, arg) == null,
					"Any demon may join");
			helper.assertTrue(SectMembership.kin(tiger, sect) && !SectMembership.kin(fox, sect) && SectMembership.excused(tiger, elder),
					"Its own blood needs no trial");
			// Off its grounds, a man is let be.
			BlockPos far = sect.core.offset(200, 0, 0);
			man.teleportTo(helper.getLevel(), far.getX() + 0.5, far.getY() + 1, far.getZ() + 0.5, 0, 0);
			helper.assertTrue(!elder.isEnemy(man), "Not off its grounds");
		} finally {
			elder.discard();
			gone(helper, man, sect);
			gone(helper, tiger);
			gone(helper, fox);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ascendedDemonsGoToClansTheirOwnFirst(GameTestHelper helper) {
		Sect men = clan(helper, Bloodline.HUMAN, 0), tigers = clan(helper, Bloodline.TIGER, 1), foxes = clan(helper, Bloodline.FOX, 6);
		CultivatorNpc tiger = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1)), fox = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 2)),
				man = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 3));
		for (CultivatorNpc npc : new CultivatorNpc[] {tiger, fox, man}) {
			npc.setNoAi(true);
			npc.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			npc.setAlignment(0);
		}
		tiger.setRace(Bloodline.TIGER, 0);
		fox.setRace(Bloodline.FOX, 0);
		try {
			helper.assertTrue(SectManager.findOpening(helper.getLevel(), tiger) == tigers, "A tiger to its own clan");
			helper.assertTrue(SectManager.findOpening(helper.getLevel(), fox) == foxes, "A fox to its own, though another clan is nearer");
			helper.assertTrue(SectManager.findOpening(helper.getLevel(), man) == men, "A man to a sect of men");
			foxes.extinct = true;
			helper.assertTrue(SectManager.findOpening(helper.getLevel(), fox) == tigers, "With none of its own, any clan");
		} finally {
			tiger.discard();
			fox.discard();
			man.discard();
			gone(helper, null, men, tigers, foxes);
		}
		helper.succeed();
	}
}
