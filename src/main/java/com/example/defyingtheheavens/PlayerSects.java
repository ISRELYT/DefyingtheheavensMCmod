package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Sects founded by players (the user's "Build as designed", 2026-11-10).
 * <ul>
 *   <li><b>Founding.</b> At their own Formation Core, on its screen's Sect page: one of Nascent Soul or above (by either path, as a sect weighs
 *   strength: TwoPaths#weighedRank), of no sect, its barrier up with at least {@link #MIN_MATS} meditation mats inside it, in a world with
 *   sects of its own ({@link #sectWorld}), on no other sect's land. They name it; its path is theirs (righteous, neutral or devilish), a demon
 *   founds a clan of their race, and one who tempers the body alone a body sect. On the core of a fallen sect they claimed, that sect lives
 *   again under them: its grounds, and its name unless they give another.</li>
 *   <li><b>Its people.</b> Its places are its meditation mats inside the barrier, at most {@link #cap} by its founder's realm (Nascent Soul 9,
 *   Heavenly Being 12, Four Axis 16, Dao Integration 20, Void Shattering and up 24). About one aspirant a day comes while there is room
 *   ({@link #arrive}): most at the world's floor (SectManager#floorRank), one in {@link #STRONGER_ONE_IN} stronger, never as strong as the
 *   founder. The founder is its Sect Master; its cultivators are titled under them, a Grand Elder at most (Sect#recalculateTitles). Each lives
 *   at a mat; Sect Plaques (SectPlaqueBlock) hung by its gates are where aspirants come in and its gate guards stand, and a Heaven Altar
 *   inside is its Altar of Heaven. Players join it only when its founder invites them ({@link #invite}, then {@link #accept}).</li>
 *   <li><b>Tribute.</b> Each day each of its cultivators brings 2 to the power of their realm in Low stones (Qi Refining 1, Foundation
 *   Building 2, Core Formation 4...), kept up to {@link #TRIBUTE_DAYS_KEPT} days for the founder to collect at the core.</li>
 *   <li><b>Its end.</b> The founder disbands it at its core, or its core is broken: its cultivators set off as rogues, its players are of no
 *   sect, and the core is the founder's own again.</li>
 * </ul>
 * No other sect raids it. Rumours tell of its founding round its world.
 */
public final class PlayerSects {
	/** The weakest who may found one: Nascent Soul Early. */
	public static final int FOUNDER_RANK = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
	/** Meditation mats a core's barrier must hold for a sect to be founded on it. */
	public static final int MIN_MATS = 3;
	/** Its places at most by its founder's realm, from Nascent Soul up (Void Shattering and above all 24). */
	private static final int[] CAPS = {9, 12, 16, 20, 24};
	/** How long an invitation stands (ticks: five minutes). */
	public static final long INVITE_TICKS = 6000;
	/** One aspirant in this many comes stronger than the world's floor. */
	public static final int STRONGER_ONE_IN = 6;
	/** Tribute is kept for this many days at most: what is left longer uncollected the sect spends. */
	public static final int TRIBUTE_DAYS_KEPT = 30;
	/** How often a sect's grounds are looked over again while its core is loaded (its mats, plaques and altar), ticks. */
	private static final long SURVEY_EVERY = 1200;
	/** How far below and above its core a sect's grounds are looked over. */
	private static final int SURVEY_DOWN = 16, SURVEY_UP = 24;
	/** The longest name a sect may be given. */
	public static final int NAME_LENGTH = 32;

	/** What the core's Sect page asks for (C2S player_sect_act). */
	public enum Act { QUERY, FOUND, RENAME, INVITE, EXPEL, COLLECT, DISBAND }

	/** Its places at most for a founder of {@code founderRank}: 0 below Nascent Soul. */
	public static int cap(int founderRank) {
		int above = CultivatorNpc.realmOf(Math.max(0, founderRank)).ordinal() - Realm.NASCENT_SOUL.ordinal();
		return founderRank < FOUNDER_RANK || above < 0 ? 0 : CAPS[Math.min(above, CAPS.length - 1)];
	}

	/** A founder's strength as their sect weighs it: either path's, the body's too (TwoPaths#weighedRank). */
	static int founderRank(PlayerCultivation c) {
		return TwoPaths.weighedRank(c);
	}

	static int founderRank(MinecraftServer server, Sect sect) {
		return founderRank(CultivationManager.own(server, sect.founder));
	}

	/** A world with sects of its own (where aspirants come from): the Overworld, the Jade Firmament, the Great Void, the Immortal Realm. */
	public static boolean sectWorld(ResourceKey<Level> dimension) {
		return dimension == Level.OVERWORLD || ModDimensions.isUpperRealm(dimension) || HolyLands.isHolyLand(dimension) || ImmortalSects.isImmortalSect(dimension);
	}

	// --- Founding ---

	/** Why {@code player} can't found a sect on {@code core} now (a line to show them), or null if they can. */
	public static Component refusal(ServerPlayer player, FormationCoreBlockEntity core) {
		if (!player.getUUID().equals(core.getOwner())) return Component.translatable(ModLang.MSG_PSECT_NOT_OWNER);
		if (GhostSystem.isGhost(player) || Possession.inhabits(player.getUUID())) return Component.translatable(ModLang.MSG_PSECT_DEAD);
		// (A sect fallen with them on its rolls holds nobody: SectMembership#liveSectOf.)
		Sect own = SectMembership.liveSectOf(player.server, player.getUUID());
		if (own != null) return Component.translatable(ModLang.MSG_PSECT_IN_SECT, own.name);
		if (founderRank(CultivationManager.get(player)) < FOUNDER_RANK) return Component.translatable(ModLang.MSG_PSECT_WEAK);
		ServerLevel level = player.serverLevel();
		if (!sectWorld(level.dimension())) return Component.translatable(ModLang.MSG_PSECT_NO_SECTS);
		Sect land = landOf(level, core.getBlockPos(), core.getRadius());
		if (land != null) return Component.translatable(ModLang.MSG_PSECT_TERRITORY, land.name);
		if (!core.isRaised()) return Component.translatable(ModLang.MSG_PSECT_BARRIER);
		int mats = survey(level, core.getBlockPos(), core.getRadius()).mats().size();
		if (mats < MIN_MATS) return Component.translatable(ModLang.MSG_PSECT_MATS, MIN_MATS, mats);
		return null;
	}

	/** Another sect standing whose land {@code core} lies on, or whose core lies within its barrier ({@code radius}) and the margin past it. */
	static Sect landOf(ServerLevel level, BlockPos core, int radius) {
		double reach = radius + CultivatorNpc.TERRITORY_MARGIN;
		for (Sect other : SectData.get(level.getServer()).all()) {
			if (other.extinct || other.dimension != level.dimension() || other.core.equals(core)) continue;
			double dx = other.core.getX() - core.getX(), dz = other.core.getZ() - core.getZ();
			if (other.inTerritory(core) || dx * dx + dz * dz <= reach * reach) return other;
		}
		return null;
	}

	/** The fallen sect {@code core} served, if founding on it would revive it (its record kept, of this world and this core), or null. */
	static Sect fallen(ServerLevel level, FormationCoreBlockEntity core) {
		Sect fallen = core.getFallenSect() == null ? null : SectManager.get(level.getServer(), core.getFallenSect());
		return fallen != null && fallen.extinct && !fallen.isPlayers() && fallen.dimension == level.dimension() && fallen.core.equals(core.getBlockPos())
				? fallen : null;
	}

	/**
	 * {@code founder} founds a sect on their {@code core}, called {@code typed} (one made up for it if they gave none; a fallen sect's own when
	 * it is revived). @return the sect, or null if they can't ({@link #refusal}: told why)
	 */
	public static Sect found(ServerPlayer founder, FormationCoreBlockEntity core, String typed) {
		Component why = refusal(founder, core);
		if (why != null) {
			founder.displayClientMessage(why.copy().withStyle(ChatFormatting.RED), true);
			return null;
		}
		ServerLevel level = founder.serverLevel();
		MinecraftServer server = level.getServer();
		PlayerCultivation c = CultivationManager.get(founder);
		RandomSource random = level.getRandom();
		long now = level.getGameTime();
		Sect fallen = fallen(level, core);
		Sect sect = fallen != null ? fallen : new Sect(UUID.randomUUID());
		sect.extinct = false;
		sect.replacements.clear();
		sect.reserved.clear();
		sect.letGo.clear();
		sect.invited.clear();
		if (fallen != null) {
			// Its rolls and grudges were the fallen sect's, not its new master's (the bug sweep of 2026-11-10): players left on its rolls when it fell
			// aren't taken into it unasked, and its standing with everyone starts again (with its founder above all, often the one who brought it
			// down: its new people would have met their master as the slayer of its old).
			sect.disciples.clear();
			sect.standing.clear();
			sect.barredUntil.clear();
		}
		// Off any fallen sect's rolls they were left on (#refusal: they are on no standing one), which SectMembership#sectOf could find first.
		for (Sect other : SectData.get(server).all()) {
			if (other != sect) other.disciples.remove(founder.getUUID());
		}
		sect.dimension = level.dimension();
		sect.core = core.getBlockPos().immutable();
		sect.alignment = Alignment.clamp(c.getAlignment());
		sect.race = DemonPath.isDemon(c) ? DemonPath.bloodline(c) : Bloodline.HUMAN;
		sect.body = !sect.isClan() && c.isMortal() && BodyPath.rank(c) >= 0;
		sect.founder = founder.getUUID();
		sect.founderName = founder.getGameProfile().getName();
		String name = clean(typed);
		if (name.isEmpty()) {
			Alignment.Faction faction = Alignment.factionOf(sect.alignment);
			name = fallen != null ? fallen.name : sect.isClan() ? SectNames.clan(random, sect.race)
					: sect.body ? SectNames.body(random, faction) : SectNames.generate(random, faction);
		}
		sect.name = name;
		sect.barrierDownSince = -1;
		sect.elderFellAt = -1;
		sect.tribute = 0;
		sect.tributeDay = Regard.day(now);
		sect.nextArrival = now + HeavenRitual.DAY / 4; // the first comes soon: word of a new sect travels
		// A new sect has no treasury of its own (the founder keeps their own stores): its box lies under the world, where nothing is.
		if (fallen == null) sect.treasury = new BoundingBox(sect.core.getX(), level.getMinBuildHeight() - 64, sect.core.getZ(),
				sect.core.getX(), level.getMinBuildHeight() - 64, sect.core.getZ());
		sect.disciples.put(founder.getUUID(), new Sect.Disciple(founder.getUUID(), sect.founderName, now));
		sect.barredUntil.remove(founder.getUUID());
		SectData.get(server).put(sect);
		core.foundPlayersSect(level, sect);
		lay(level, sect, core, survey(level, sect.core, core.getRadius()), fallen == null);
		if (fallen != null) Rumours.forgetCore(sect); // no unclaimed core there now
		Rumours.noteFounding(sect, sect.founderName, now);
		SectManager.dirty(server);
		Formations.syncSoon(); // its barrier knows its people now
		SectMembership.sendStatus(founder);
		founder.sendSystemMessage(Component.translatable(fallen != null ? ModLang.MSG_PSECT_REVIVED : ModLang.MSG_PSECT_FOUNDED, sect.name)
				.withStyle(ChatFormatting.GOLD));
		level.playSound(null, sect.core, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0f, 1.0f);
		level.playSound(null, founder.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.8f);
		DefyingTheHeavens.LOGGER.info("{} founded the {} at {}", sect.founderName, sect.name, sect.core.toShortString());
		return sect;
	}

	/** A name as given: no formatting codes or control characters, its spaces single, at most {@link #NAME_LENGTH} long. */
	static String clean(String typed) {
		if (typed == null) return "";
		// A formatting code goes with the character after it (a colour, a style); then control characters, and runs of spaces.
		String name = typed.replaceAll("§.?", "").replaceAll("\\p{Cntrl}", "").trim().replaceAll("\\s+", " ");
		return name.length() > NAME_LENGTH ? name.substring(0, NAME_LENGTH).trim() : name;
	}

	/** Its founder gives {@code sect} a new name. */
	public static void rename(ServerPlayer founder, Sect sect, FormationCoreBlockEntity core, String typed) {
		String name = clean(typed);
		if (name.isEmpty() || name.equals(sect.name)) return;
		sect.name = name;
		core.renamePlayersSect(name);
		SectManager.dirty(founder.server);
		for (Sect.Disciple d : sect.disciples.values()) {
			ServerPlayer online = founder.server.getPlayerList().getPlayer(d.id);
			if (online != null) SectMembership.sendStatus(online);
		}
		founder.displayClientMessage(Component.translatable(ModLang.MSG_PSECT_RENAMED, name).withStyle(ChatFormatting.GOLD), true);
	}

	// --- Its grounds ---

	/** What lies inside a core's barrier: its meditation mats (nearest the core first), the Sect Plaques hung there, and a Heaven Altar. */
	public record Grounds(List<BlockPos> mats, List<BlockPos> plaques, BlockPos altar) {}

	/**
	 * Looks over the land inside a barrier of {@code radius} round {@code core} (its loaded part, {@link #SURVEY_DOWN} below to
	 * {@link #SURVEY_UP} above it): only the sections holding one of the blocks that count are looked through block by block.
	 */
	public static Grounds survey(ServerLevel level, BlockPos core, int radius) {
		List<BlockPos> mats = new ArrayList<>(), plaques = new ArrayList<>();
		BlockPos altar = null;
		int minY = Math.max(level.getMinBuildHeight(), core.getY() - SURVEY_DOWN);
		int maxY = Math.min(level.getMaxBuildHeight() - 1, core.getY() + SURVEY_UP);
		long r2 = (long) radius * radius;
		for (int cx = (core.getX() - radius) >> 4; cx <= (core.getX() + radius) >> 4; cx++) {
			for (int cz = (core.getZ() - radius) >> 4; cz <= (core.getZ() + radius) >> 4; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;
				for (int index = chunk.getSectionIndex(minY); index <= chunk.getSectionIndex(maxY); index++) {
					LevelChunkSection section = chunk.getSection(index);
					if (section.hasOnlyAir() || !section.getStates().maybeHas(PlayerSects::marksGrounds)) continue;
					int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
					for (int y = 0; y < 16; y++) {
						int wy = baseY + y;
						if (wy < minY || wy > maxY) continue;
						for (int z = 0; z < 16; z++) {
							for (int x = 0; x < 16; x++) {
								BlockState state = section.getBlockState(x, y, z);
								if (!marksGrounds(state)) continue;
								int wx = (cx << 4) + x, wz = (cz << 4) + z;
								long dx = wx - core.getX(), dz = wz - core.getZ();
								if (dx * dx + dz * dz > r2) continue;
								BlockPos pos = new BlockPos(wx, wy, wz);
								if (state.is(ModBlocks.SECT_PLAQUE)) plaques.add(pos);
								else if (state.is(ModBlocks.HEAVEN_ALTAR)) {
									if (altar == null || pos.distSqr(core) < altar.distSqr(core)) altar = pos;
								} else mats.add(pos);
							}
						}
					}
				}
			}
		}
		mats.sort(Comparator.comparingDouble(p -> p.distSqr(core)));
		return new Grounds(mats, plaques, altar);
	}

	private static boolean marksGrounds(BlockState state) {
		return state.is(ModBlocks.MEDITATION_MAT) || state.is(ModBlocks.RED_MEDITATION_MAT) || state.is(ModBlocks.SECT_PLAQUE)
				|| state.is(ModBlocks.HEAVEN_ALTAR);
	}

	/**
	 * Lays {@code sect}'s posts out by its {@code grounds}: a home and a seat for meditation at each mat; a gate before each plaque, a gate guard
	 * either side of it; its Altar of Heaven at the altar. A new sect's ({@code fresh}) other posts are spots round its core: the elders' about
	 * it, a courtyard further out, a training ground further still (a revived sect keeps its own). Its places: its mats, at most its founder's cap.
	 */
	static void lay(ServerLevel level, Sect sect, FormationCoreBlockEntity core, Grounds grounds, boolean fresh) {
		BlockPos c = sect.core;
		sect.barrierRadius = core.getRadius();
		int r = sect.barrierRadius;
		sect.bounds = new BoundingBox(c.getX() - r, c.getY() - SURVEY_DOWN, c.getZ() - r, c.getX() + r, c.getY() + SURVEY_UP, c.getZ() + r);
		set(sect, Sect.Post.DWELLING, grounds.mats());
		set(sect, Sect.Post.MEDITATION, grounds.mats());
		if (!grounds.plaques().isEmpty()) {
			List<BlockPos> gates = new ArrayList<>(), guards = new ArrayList<>();
			for (BlockPos plaque : grounds.plaques()) {
				BlockState state = level.getBlockState(plaque);
				Direction out = state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
				BlockPos gate = ground(level, plaque.relative(out));
				if (gate == null) continue;
				gates.add(gate);
				for (int side : new int[] {2, -2}) {
					BlockPos guard = ground(level, gate.relative(out.getClockWise(), side));
					if (guard != null) guards.add(guard);
				}
			}
			set(sect, Sect.Post.GATE, gates);
			set(sect, Sect.Post.GATE_GUARD, guards);
		} else if (fresh) {
			set(sect, Sect.Post.GATE, List.of());
			set(sect, Sect.Post.GATE_GUARD, List.of());
		}
		if (grounds.altar() != null) {
			Direction toCore = Direction.getNearest(c.getX() - grounds.altar().getX(), 0, c.getZ() - grounds.altar().getZ());
			BlockPos kneel = ground(level, grounds.altar().relative(toCore.getAxis().isHorizontal() ? toCore : Direction.SOUTH));
			set(sect, Sect.Post.ALTAR, kneel == null ? List.of(grounds.altar()) : List.of(grounds.altar(), kneel));
		} else if (fresh) {
			set(sect, Sect.Post.ALTAR, List.of());
		}
		if (fresh || sect.posts(Sect.Post.CORE).isEmpty()) {
			set(sect, Sect.Post.MASTER_SEAT, List.of(c.above()));
			set(sect, Sect.Post.CORE, ring(level, c, 3, 4, 0));
			set(sect, Sect.Post.TREASURY, ring(level, c, 3, 4, Math.PI / 4));
			set(sect, Sect.Post.COURTYARD, ring(level, c, Math.min(6, Math.max(4, r / 3)), 6, 0));
			set(sect, Sect.Post.TRAINING, ring(level, c, Math.min(10, Math.max(5, r / 2)), 5, Math.PI / 5));
		}
		sect.capacity = Math.min(grounds.mats().size(), cap(founderRank(level.getServer(), sect)));
	}

	private static void set(Sect sect, Sect.Post post, List<BlockPos> spots) {
		List<BlockPos> list = sect.posts(post);
		list.clear();
		for (BlockPos spot : spots) list.add(spot.immutable());
	}

	/** {@code count} spots {@code distance} round {@code centre} (from {@code turn} radians), each on the ground there; the centre's top if none. */
	private static List<BlockPos> ring(ServerLevel level, BlockPos centre, int distance, int count, double turn) {
		List<BlockPos> spots = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			double angle = turn + Math.PI * 2 * i / count;
			BlockPos spot = ground(level, centre.offset((int) Math.round(Math.cos(angle) * distance), 0, (int) Math.round(Math.sin(angle) * distance)));
			if (spot != null) spots.add(spot);
		}
		if (spots.isEmpty()) spots.add(centre.above());
		return spots;
	}

	/** Where one could stand at {@code pos}'s column near its height (a few blocks up or down): firm footing, room for a body. Null if none. */
	static BlockPos ground(ServerLevel level, BlockPos pos) {
		if (!level.isLoaded(pos)) return null;
		for (int dy : new int[] {0, -1, 1, -2, 2, -3, 3, -4, -5, -6}) {
			BlockPos at = pos.offset(0, dy, 0), below = at.below();
			if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
					&& level.getBlockState(at).getCollisionShape(level, at).isEmpty()
					&& level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
				return at;
			}
		}
		return null;
	}

	// --- Its people ---

	/** Every 5 s: invitations lapse, tribute is reckoned; a loaded sect's grounds are looked over now and then, and its aspirants come. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 100 != 53) return;
		for (Sect sect : SectData.get(server).all()) {
			if (!sect.isPlayers() || sect.extinct) continue;
			ServerLevel level = server.getLevel(sect.dimension);
			if (level == null) continue;
			long now = level.getGameTime();
			if (!sect.invited.isEmpty() && sect.invited.values().removeIf(until -> until <= now)) SectManager.dirty(server);
			reckonTribute(server, sect, now);
			if (!level.isPositionEntityTicking(sect.core) || !(level.getBlockEntity(sect.core) instanceof FormationCoreBlockEntity core)
					|| !sect.id.equals(core.getSect())) continue;
			if (now % SURVEY_EVERY < 100) {
				lay(level, sect, core, survey(level, sect.core, core.getRadius()), false);
				SectManager.dirty(server);
			}
			if (sect.nextArrival < 0) sect.nextArrival = now + HeavenRitual.DAY / 2;
			if (now >= sect.nextArrival) arrive(level, sect);
		}
	}

	/**
	 * An aspirant comes to {@code sect} if it has a place: at the world's floor, or (one in {@link #STRONGER_ONE_IN}) stronger, up to halfway
	 * to its founder, never their equal; in at a gate (its plaque's), to its mat. The next comes in about a day (18 to 30 hours).
	 */
	static void arrive(ServerLevel level, Sect sect) {
		RandomSource random = level.getRandom();
		long now = level.getGameTime();
		sect.nextArrival = now + HeavenRitual.DAY * 3 / 4 + random.nextInt((int) (HeavenRitual.DAY / 2));
		SectManager.dirty(level.getServer());
		if (sect.members.size() >= sect.capacity) return;
		int floor = SectManager.floorRank(sect.dimension);
		int top = Math.max(floor, founderRank(level.getServer(), sect) - 1);
		int rank = floor;
		if (top > floor && random.nextInt(STRONGER_ONE_IN) == 0) rank = Math.min(top, floor + 1 + random.nextInt(Math.max(1, (top - floor + 1) / 2)));
		CultivatorNpc npc = SectManager.spawnMember(level, sect, rank, sect.freeHome(), random);
		if (npc == null) return;
		List<BlockPos> gates = sect.posts(Sect.Post.GATE);
		BlockPos gate = gates.isEmpty() ? null : gates.get(random.nextInt(gates.size()));
		if (gate != null && level.isLoaded(gate)) npc.moveTo(gate.getX() + 0.5, gate.getY() + 0.1, gate.getZ() + 0.5, npc.getYRot(), 0);
		sect.recalculateTitles();
		npc.setTitle(sect.members.get(npc.getUUID()).title);
		npc.equip(random);
		level.addFreshEntity(npc);
		SectManager.retitle(level, sect);
		DefyingTheHeavens.LOGGER.info("An aspirant joins the {} (a player's sect)", sect.name);
		ServerPlayer founder = level.getServer().getPlayerList().getPlayer(sect.founder);
		if (founder != null) {
			NpcTitle title = sect.members.get(npc.getUUID()).title;
			founder.sendSystemMessage(Component.translatable(ModLang.MSG_PSECT_ARRIVED, npc.getPersonalName(),
					PlayerCultivation.rankName(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank)), sect.name,
					title.getDisplayName(sect.isClan(), sect.isHoly(), sect.isImmortal())).withStyle(ChatFormatting.GOLD));
		}
	}

	// --- Players ---

	/** Its founder invites the player called {@code name} (online): they have {@link #INVITE_TICKS} to accept (a link in their chat). */
	public static void invite(ServerPlayer founder, Sect sect, String name) {
		String wanted = name == null ? "" : name.trim();
		if (wanted.isEmpty()) return;
		ServerPlayer target = founder.server.getPlayerList().getPlayerByName(wanted);
		if (target == null) {
			founder.displayClientMessage(Component.translatable(ModLang.MSG_PSECT_INVITE_UNKNOWN, wanted).withStyle(ChatFormatting.RED), true);
			return;
		}
		if (target == founder) return;
		if (sect.disciples.containsKey(target.getUUID())) {
			founder.displayClientMessage(Component.translatable(ModLang.MSG_PSECT_INVITE_MEMBER, target.getGameProfile().getName(), sect.name), true);
			return;
		}
		// One invitation at a time: sent again while it stands, it rang in their chat each time (the bug sweep of 2026-11-10).
		Long standing = sect.invited.get(target.getUUID());
		if (standing != null && standing > target.level().getGameTime()) {
			founder.displayClientMessage(Component.translatable(ModLang.MSG_PSECT_INVITE_PENDING, target.getGameProfile().getName()).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		sect.invited.put(target.getUUID(), target.level().getGameTime() + INVITE_TICKS);
		SectManager.dirty(founder.server);
		target.sendSystemMessage(Component.translatable(ModLang.MSG_PSECT_INVITED, sect.founderName, sect.name).withStyle(ChatFormatting.GOLD)
				.append(" ")
				.append(Component.translatable(ModLang.MSG_PSECT_ACCEPT).withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sectaccept"))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(ModLang.MSG_PSECT_ACCEPT_HOVER, sect.name))))));
		target.level().playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
		founder.displayClientMessage(Component.translatable(ModLang.MSG_PSECT_INVITE_SENT, target.getGameProfile().getName(), sect.name)
				.withStyle(ChatFormatting.GOLD), true);
	}

	/** The sect whose invitation stands for {@code player} (the latest), or null. */
	static Sect invitation(ServerPlayer player) {
		long now = player.level().getGameTime();
		Sect sect = null;
		long latest = Long.MIN_VALUE;
		for (Sect s : SectData.get(player.server).all()) {
			Long until = s.invited.get(player.getUUID());
			if (until == null || until <= now || !s.isPlayers() || s.extinct || until <= latest) continue;
			sect = s;
			latest = until;
		}
		return sect;
	}

	/** The name of the sect inviting {@code player} (for their Sect tab, where they may accept it), or "". */
	public static String invitationFor(ServerPlayer player) {
		Sect sect = invitation(player);
		return sect == null ? "" : sect.name;
	}

	/**
	 * {@code player} takes up the invitation standing for them (the latest), if nothing keeps them out: from their Sect tab, or /sectaccept (the
	 * invitation's link). @return 1 if they joined
	 */
	public static int accept(ServerPlayer player) {
		Sect sect = invitation(player);
		if (sect == null) {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_PSECT_NO_INVITE).withStyle(ChatFormatting.GRAY));
			return 0;
		}
		Component why = joinRefusal(player, sect);
		if (why != null) {
			player.sendSystemMessage(why.copy().withStyle(ChatFormatting.RED));
			return 0;
		}
		sect.invited.remove(player.getUUID());
		SectMembership.join(player, sect);
		ServerPlayer founder = player.server.getPlayerList().getPlayer(sect.founder);
		if (founder != null) {
			founder.sendSystemMessage(Component.translatable(ModLang.MSG_PSECT_ACCEPTED, player.getGameProfile().getName(), sect.name)
					.withStyle(ChatFormatting.GOLD));
		}
		return 1;
	}

	/** Why {@code player} can't join {@code sect} on its founder's invitation now, or null: as a sect's elders would see it, but for the trial. */
	static Component joinRefusal(ServerPlayer player, Sect sect) {
		PlayerCultivation c = CultivationManager.get(player);
		if (Possession.inhabits(player.getUUID())) return Component.translatable(ModLang.MSG_PSECT_JOIN_POSSESSED);
		if (GhostSystem.isGhost(player)) return Component.translatable(ModLang.MSG_PSECT_JOIN_DEAD);
		if (sect.isClan() && !DemonPath.isDemon(c)) return Component.translatable(ModLang.MSG_PSECT_JOIN_HUMAN, sect.name);
		if (sect.body && BodyPath.rank(c) < 0) return Component.translatable(ModLang.MSG_PSECT_JOIN_NOT_BODY, sect.name);
		if (c.isMortal() && !sect.body) return Component.translatable(ModLang.MSG_PSECT_JOIN_MORTAL);
		Sect other = SectMembership.liveSectOf(player.server, player.getUUID());
		if (other != null) return Component.translatable(ModLang.MSG_PSECT_JOIN_OTHER, other.name);
		return null;
	}

	// --- Tribute ---

	/** What its cultivators bring in a day: 2 to the power of each one's realm, in Low stones. */
	public static long dailyTribute(Sect sect) {
		long sum = 0;
		for (Sect.Member member : sect.members.values()) sum += 1L << Math.min(20, CultivatorNpc.realmOf(Math.max(0, member.rank)).ordinal());
		return sum;
	}

	/** The days since its tribute was last reckoned are added to it (as its people are now), up to {@link #TRIBUTE_DAYS_KEPT} days' worth. */
	static void reckonTribute(MinecraftServer server, Sect sect, long now) {
		long today = Regard.day(now);
		if (sect.tributeDay < 0 || today < sect.tributeDay) {
			sect.tributeDay = today;
			SectManager.dirty(server);
			return;
		}
		if (today == sect.tributeDay) return;
		long daily = dailyTribute(sect);
		long days = Math.min(TRIBUTE_DAYS_KEPT, today - sect.tributeDay);
		sect.tribute = Math.min(daily * TRIBUTE_DAYS_KEPT, sect.tribute + daily * days);
		sect.tributeDay = today;
		SectManager.dirty(server);
	}

	/** Its founder takes the tribute waiting, as spirit stones (the fewest that make it up). */
	public static void collect(ServerPlayer founder, Sect sect) {
		reckonTribute(founder.server, sect, founder.level().getGameTime());
		if (sect.tribute <= 0) {
			founder.displayClientMessage(Component.translatable(ModLang.MSG_PSECT_NO_TRIBUTE).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		int value = (int) Math.min(Integer.MAX_VALUE, sect.tribute);
		for (ItemStack stones : SpiritStoneItem.stonesFor(value)) {
			if (!founder.getInventory().add(stones)) founder.drop(stones, false);
		}
		sect.tribute -= value;
		SectManager.dirty(founder.server);
		founder.displayClientMessage(Component.translatable(ModLang.MSG_PSECT_TRIBUTE, value, sect.name).withStyle(ChatFormatting.GOLD), true);
		founder.level().playSound(null, founder.blockPosition(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
	}

	// --- Its end ---

	/**
	 * {@code sect} breaks up: its cultivators set off as rogues (SectManager#banish), its players are of no sect (each told, with
	 * {@code messageKey}), and its core is its founder's own again.
	 */
	public static void disband(MinecraftServer server, Sect sect, String messageKey) {
		for (UUID id : new ArrayList<>(sect.members.keySet())) SectManager.banish(server, sect, id);
		for (Sect.Disciple d : new ArrayList<>(sect.disciples.values())) {
			sect.disciples.remove(d.id);
			ServerPlayer online = server.getPlayerList().getPlayer(d.id);
			if (online != null) {
				online.sendSystemMessage(Component.translatable(messageKey, sect.name).withStyle(ChatFormatting.GRAY));
				SectMembership.sendStatus(online);
			}
		}
		sect.extinct = true;
		sect.invited.clear();
		sect.replacements.clear();
		sect.reserved.clear();
		SectManager.dirty(server);
		Formations.syncSoon();
		ServerLevel home = server.getLevel(sect.dimension);
		if (home != null && home.isLoaded(sect.core) && home.getBlockEntity(sect.core) instanceof FormationCoreBlockEntity core && sect.id.equals(core.getSect())) {
			core.unbindPlayersSect(home);
		}
		DefyingTheHeavens.LOGGER.info("The {} (a player's sect) is disbanded", sect.name);
	}

	/** Its core broken (SectManager#onCoreDestroyed): it breaks up. */
	static void coreLost(ServerLevel level, Sect sect) {
		if (sect.extinct) return;
		disband(level.getServer(), sect, ModLang.MSG_PSECT_CORE_LOST);
	}

	// --- The core's Sect page ---

	/** C2S player_sect_act: what the Sect page of {@code pos}'s core asks for, done if {@code player} may; then the page told how it stands. */
	public static void act(ServerPlayer player, BlockPos pos, Act act, String text, UUID target) {
		if (!(player.level().getBlockEntity(pos) instanceof FormationCoreBlockEntity core) || !core.canControl(player)) return;
		Sect sect = core.isPlayersSect() ? SectManager.get(player.server, core.getSect()) : null;
		if (sect != null && sect.extinct) sect = null;
		boolean founder = sect != null && SectMembership.isFounder(sect, player.getUUID());
		switch (act) {
			case FOUND -> {
				if (core.getSect() == null) found(player, core, text);
			}
			case RENAME -> {
				if (founder) rename(player, sect, core, text);
			}
			case INVITE -> {
				if (founder) invite(player, sect, text);
			}
			case EXPEL -> {
				if (founder && target != null) SectMembership.banish(player, target);
			}
			case COLLECT -> {
				if (founder) collect(player, sect);
			}
			case DISBAND -> {
				if (founder) {
					String name = sect.name;
					disband(player.server, sect, ModLang.MSG_PSECT_DISBANDED);
					player.displayClientMessage(Component.translatable(ModLang.MSG_PSECT_DISBANDED, name).withStyle(ChatFormatting.GRAY), true);
					player.level().playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 0.5f);
				}
			}
			case QUERY -> { }
		}
		sendInfo(player, core);
	}

	/**
	 * S2C player_sect, the core's Sect page: its sect if it is the player's own (its name, places, tribute, people, when the next aspirant is
	 * due), else whether they could found one here and why not, the mats inside, their cap, and the fallen sect it would revive.
	 */
	public static void sendInfo(ServerPlayer player, FormationCoreBlockEntity core) {
		ServerLevel level = player.serverLevel();
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(core.getBlockPos());
		Sect sect = core.isPlayersSect() ? SectManager.get(player.server, core.getSect()) : null;
		Grounds grounds = survey(level, core.getBlockPos(), core.getRadius());
		long now = level.getGameTime();
		boolean own = sect != null && !sect.extinct;
		buf.writeBoolean(own);
		if (own) {
			reckonTribute(player.server, sect, now);
			buf.writeUtf(sect.name);
			buf.writeVarLong(sect.tribute);
			buf.writeVarLong(dailyTribute(sect));
			buf.writeVarInt(sect.capacity);
			buf.writeVarInt(grounds.mats().size());
			buf.writeVarInt(cap(founderRank(player.server, sect)));
			buf.writeVarInt(sect.posts(Sect.Post.GATE).size());
			buf.writeBoolean(sect.altar() != null);
			buf.writeVarLong(sect.members.size() >= sect.capacity || sect.nextArrival < 0 ? -1 : Math.max(0, sect.nextArrival - now));
			buf.writeBoolean(sect.isClan());
			buf.writeBoolean(sect.isHoly());
			buf.writeBoolean(sect.isImmortal());
			buf.writeBoolean(sect.body);
			List<Sect.Member> members = new ArrayList<>(sect.members.values());
			members.sort(Sect.SENIORITY);
			List<Sect.Disciple> players = sect.disciples.values().stream().filter(d -> !d.id.equals(sect.founder)).toList();
			buf.writeVarInt(members.size() + players.size());
			for (Sect.Member member : members) {
				buf.writeUUID(member.id);
				buf.writeUtf(NpcNames.of(member.id));
				buf.writeVarInt(member.title.ordinal());
				buf.writeVarInt(member.rank);
				buf.writeBoolean(false);
			}
			for (Sect.Disciple d : players) {
				ServerPlayer online = player.server.getPlayerList().getPlayer(d.id);
				buf.writeUUID(d.id);
				buf.writeUtf(d.name);
				buf.writeVarInt(SectMembership.discipleTitle(player.server, sect, d.id).ordinal());
				buf.writeVarInt(online != null ? SectMembership.strength(sect, online) : -1);
				buf.writeBoolean(true);
			}
		} else {
			Component why = core.getSect() != null ? Component.translatable(ModLang.MSG_PSECT_NOT_OWNER) : refusal(player, core);
			buf.writeBoolean(why == null);
			buf.writeComponent(why == null ? Component.empty() : why);
			buf.writeVarInt(grounds.mats().size());
			buf.writeVarInt(cap(founderRank(CultivationManager.get(player))));
			Sect fallen = fallen(level, core);
			buf.writeUtf(fallen == null ? "" : fallen.name);
		}
		ServerPlayNetworking.send(player, ModPackets.PLAYER_SECT, buf);
	}

	private PlayerSects() {}
}
