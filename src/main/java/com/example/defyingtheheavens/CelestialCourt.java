package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Celestial Court (phase 2 of the realm plan, 2026-10-25: the user's "Yes, build the Celestial Court as you described"), on its
 * pinnacle over the Jade Capital (ImmortalCapital#court).
 * <ul>
 *   <li><b>Its people.</b> The Heavenly King on the palace's throne, a True Immortal of Grand Perfection {@link #KING_MIGHT} times as strong
 *   as his realm, named a Daluo Golden Immortal; the five Heavenly Venerables, one of each element seated in the Court's five halls,
 *   {@link #VENERABLE_MIGHT} times, named Taiyi Golden Immortals (the plan: True Immortals of their rank's strength until phase 3, shown
 *   as so; their strength the ladder's past True Immortal, {@link #pastTrueImmortal}); four Immortal Generals keeping the Court's gate;
 *   its Quartermaster on the palace's first terrace. All of the righteous path.
 *   Peopled the first time a player comes within {@link #NEAR} of the Court with all its posts loaded, all at once; one struck down is
 *   made up {@link #REPLACE_EVERY} later. Each keeps to its post (as the markets' merchants do, CityMarkets), the King and the Venerables
 *   seated at theirs. The Southern Heavenly Gate's four Immortal Officers are the Court's too (ImmortalGate).</li>
 *   <li><b>Joining.</b> A Heavenly Venerable takes in any of the realm who asks (TalkSystem's Celestial Court topic): an Officer of the
 *   Court. Only those who could climb the Gate ({@link ImmortalGate#mayPass}).</li>
 *   <li><b>Merit</b>, paid as Court Merit Tokens (ModItems#COURT_MERIT_TOKEN): one for each minute ({@link #WATCH_TICKS}) on the Southern
 *   Heavenly Gate's plaza while the Gate stands open (three an opening); {@link #SLAY_NIRVANA} for each devilish Entering Nirvana slain in
 *   the realm, {@link #SLAY_IMMORTAL} for each devilish True Immortal; {@link #SLAY_PRIMORDIAL} to each who fought a primordial beast
 *   slain.</li>
 *   <li><b>Two of the Venerables demons</b> ({@link #venerableRace}): the Metal Venerable of the tiger race, the Wood Venerable of the
 *   dragon's.</li>
 *   <li><b>Ranks</b> by the merit earned all told ({@link Rank}): Officer, Captain at 30, General at 100.</li>
 *   <li><b>What merit buys</b>: the Quartermaster's wares for tokens ({@link #offers}): Immortal Jade and any element's True Immortal method
 *   to all its officers, more to Captains and Generals. A Venerable teaches its own element's True Immortal method to Captains and above
 *   (TalkSystem).</li>
 * </ul>
 */
public final class CelestialCourt {
	/**
	 * How many times a True Immortal's strength one has who stands {@code realms} realms past it: each realm twice the one before (the
	 * user's rule for the ladder past True Immortal, 2026-10-26: Earth Immortal 2, Heaven Immortal 4, Golden Immortal 8, Taiyi Golden
	 * Immortal 16, Daluo Golden Immortal 32, Dao Ancestor 64). The realms up to True Immortal keep their own leaps (Realm).
	 */
	public static double pastTrueImmortal(int realms) { return Math.pow(2, realms); }

	/** How many times their realm's strength the King (a Daluo Golden Immortal, five realms on) and the Venerables (Taiyi, four on) have: their bodies and their blows. */
	public static final double KING_MIGHT = pastTrueImmortal(5), VENERABLE_MIGHT = pastTrueImmortal(4);
	/** A player this near the Court's middle (sideways) has it peopled. */
	public static final int NEAR = 120;
	/** One struck down is made up this long after (ticks: an in-game hour). */
	static final long REPLACE_EVERY = 1000;
	/** Drawn this far off its post one walks back; this far, it is set back on it. */
	static final double STRAY = 1.5, FAR = 16;
	/** A minute's watch at the open Gate earns a token. */
	public static final int WATCH_TICKS = 60 * 20;
	/** Merit for a devilish immortal slain in the realm: an Entering Nirvana, a True Immortal. */
	public static final int SLAY_NIRVANA = 2, SLAY_IMMORTAL = 5;
	/** The five halls' Venerables, in the order the Court's plan lays the halls (ImmortalCapital#court): west, west, east, east, behind. */
	static final QiElement[] HALL_ELEMENTS = {QiElement.METAL, QiElement.EARTH, QiElement.FIRE, QiElement.WOOD, QiElement.WATER};
	/** The Court's merit for each of its officers who fought one of the primordial beasts it slain (PrimordialBeast). */
	public static final int SLAY_PRIMORDIAL = 20;

	/**
	 * The demon race of the Venerable of {@code element}, or null for a man's: the Metal Venerable a tiger's and the Wood Venerable a
	 * dragon's, the White Tiger of the West and the Azure Dragon of the East (the user's ask, 2026-10-26: "at least 2 demon race members
	 * among the heavenly venerables").
	 */
	public static Bloodline venerableRace(QiElement element) {
		if (element == QiElement.METAL) return Bloodline.TIGER;
		if (element == QiElement.WOOD) return Bloodline.DRAGON;
		return null;
	}

	/** An officer of the Court's rank, by the merit earned all told. */
	public enum Rank {
		OFFICER("officer", 0), CAPTAIN("captain", 30), GENERAL("general", 100);

		private final String id;
		private final int merit;

		Rank(String id, int merit) {
			this.id = id;
			this.merit = merit;
		}

		public String getId() { return id; }
		public int merit() { return merit; }

		/** "Captain". */
		public Component displayName() { return Component.translatable(ModLang.courtRank(id)); }

		/** The rank above, or null at the top. */
		public Rank next() { return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null; }

		public static Rank of(int merit) {
			Rank rank = OFFICER;
			for (Rank r : values()) if (merit >= r.merit) rank = r;
			return rank;
		}

		public static Rank byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : null;
		}
	}

	/** How many times its realm's strength one of {@code title} has: the King and the Venerables more, anyone else its own. */
	public static double might(NpcTitle title) {
		return title == NpcTitle.HEAVENLY_KING ? KING_MIGHT : title == NpcTitle.HEAVENLY_VENERABLE ? VENERABLE_MIGHT : 1;
	}

	/** Whether {@code npc} is one of the Court's people at their post in the Immortal Realm. */
	public static boolean isCourtier(CultivatorNpc npc) {
		return npc.getTitle().isCourt() && npc.settledAt() != null && ModDimensions.isImmortalRealm(npc.level().dimension());
	}

	// --- Its people ---

	/** A post of the Court's: where one of its people keeps it, of what title, its element (a Venerable's), which way it looks, seated or not. */
	public record Post(BlockPos at, NpcTitle title, QiElement element, float yaw, boolean seated) {}

	/** The Court's middle on its pinnacle, at the Court's level. */
	static BlockPos middle(ServerLevel level) {
		return new BlockPos(ModNoiseSettings.COURT_X, plan(level).courtY(), ModNoiseSettings.COURT_Z);
	}

	private static ImmortalCapital.Plan plan(ServerLevel level) {
		return ImmortalCapital.plan(level.getChunkSource().randomState(), level.getChunkSource().getGenerator(), level);
	}

	/**
	 * The Court's posts as its plan lays them (ImmortalCapital#court), in the world: the King's on the palace's throne and the Quartermaster's
	 * on its first terrace beside the stairs, a General either side of the gate's way before it and behind it, a Venerable before each
	 * hall's altar. Every one, loaded or not.
	 */
	public static List<Post> posts(ImmortalCapital.Plan plan) {
		List<Post> posts = new ArrayList<>();
		List<SectPiece> halls = new ArrayList<>();
		for (SectPiece piece : plan.pieces()) {
			BlockPos centre = piece.getBoundingBox().getCenter();
			if (Math.hypot(centre.getX() - ModNoiseSettings.COURT_X, centre.getZ() - ModNoiseSettings.COURT_Z) > ImmortalCapital.COURT_REACH + 8) continue;
			float out = piece.getOrientation() == null ? 0 : piece.getOrientation().getOpposite().toYRot();
			switch (piece.kind()) {
				case HOLY_PALACE -> {
					int[] seat = HolyLandPieces.throne(piece);
					posts.add(new Post(piece.toWorld(seat[0], seat[1], seat[2]), NpcTitle.HEAVENLY_KING, null, out, true));
					posts.add(new Post(piece.toWorld(piece.width() / 2 + 12, 4, 8), NpcTitle.COURT_QUARTERMASTER, null, out, false));
				}
				case HOLY_GATE -> {
					int cx = piece.width() / 2;
					for (int[] at : new int[][] {{cx - 5, 0}, {cx + 5, 0}, {cx - 5, piece.depth() - 1}, {cx + 5, piece.depth() - 1}}) {
						posts.add(new Post(piece.toWorld(at[0], 1, at[1]), NpcTitle.IMMORTAL_GENERAL, null, out, false));
					}
				}
				case MEDITATION_HALL -> halls.add(piece);
				default -> { }
			}
		}
		for (int i = 0; i < halls.size() && i < HALL_ELEMENTS.length; i++) {
			SectPiece hall = halls.get(i);
			int[] seat = hall.hallSeat();
			float out = hall.getOrientation() == null ? 0 : hall.getOrientation().getOpposite().toYRot();
			posts.add(new Post(hall.toWorld(seat[0], seat[1], seat[2]), NpcTitle.HEAVENLY_VENERABLE, HALL_ELEMENTS[i], out, true));
		}
		return posts;
	}

	/** Every two seconds: the Court near a player kept peopled, and its people kept to their posts. */
	static void keep(MinecraftServer server) {
		if (server.getTickCount() % 40 != 27) return;
		ServerLevel level = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (level == null || level.players().isEmpty()) return;
		boolean near = false;
		for (ServerPlayer player : level.players()) {
			double dx = player.getX() - ModNoiseSettings.COURT_X, dz = player.getZ() - ModNoiseSettings.COURT_Z;
			near |= dx * dx + dz * dz <= (double) NEAR * NEAR;
		}
		if (!near || !level.isLoaded(new BlockPos(ModNoiseSettings.COURT_X, 0, ModNoiseSettings.COURT_Z))) return;
		BlockPos middle = middle(level);
		if (!level.isPositionEntityTicking(middle)) return;
		Data data = Data.get(server);
		List<Post> posts = posts(plan(level));
		List<CultivatorNpc> people = people(level, middle);
		Map<BlockPos, Post> byPlace = new HashMap<>();
		for (Post post : posts) byPlace.put(post.at(), post);
		for (CultivatorNpc npc : people) {
			Post post = byPlace.get(npc.settledAt());
			if (post == null) continue;
			// A Venerable of a demon's hall come before the Court had its demons (2026-10-26) is of that race now.
			Bloodline race = post.title() == NpcTitle.HEAVENLY_VENERABLE ? venerableRace(post.element()) : null;
			if (race != null && npc.getBloodline() != race) {
				npc.setRace(race, npc.getRandom().nextInt(3));
				npc.setRank(npc.getRank(), true);
			}
			toPost(npc, post);
		}
		if (people.size() >= posts.size()) return;
		long now = level.getGameTime();
		List<Post> loaded = posts.stream().filter(post -> level.isPositionEntityTicking(post.at())).toList();
		if (!data.peopled && loaded.size() < posts.size()) return; // peopled all at once: every post loaded first
		if (data.peopled && now - data.lastArrival < REPLACE_EVERY) return;
		Set<BlockPos> manned = new HashSet<>();
		for (CultivatorNpc npc : people) manned.add(npc.settledAt());
		RandomSource random = level.getRandom();
		for (Post post : loaded) {
			if (manned.contains(post.at())) continue;
			arrive(level, post, random);
			if (data.peopled) break; // one an hour once the Court has been peopled
		}
		data.peopled = true;
		data.lastArrival = now;
		data.setDirty();
	}

	/** The Court's people loaded now, at the Court. */
	public static List<CultivatorNpc> people(ServerLevel level, BlockPos middle) {
		int reach = ImmortalCapital.COURT_REACH + 16;
		return level.getEntitiesOfClass(CultivatorNpc.class, new AABB(middle).inflate(reach, 64, reach), npc -> npc.isAlive() && isCourtier(npc));
	}

	/** Back to its post, if drawn off it and not fighting: walking, or set back on it from far; seated there (the King, the Venerables). */
	private static void toPost(CultivatorNpc npc, Post post) {
		if (npc.getTarget() != null || npc.isBegging() || npc.isInCombat() || npc.talkPartner() != null) return;
		Vec3 at = Vec3.atBottomCenterOf(post.at());
		double off = npc.position().distanceToSqr(at);
		if (off > FAR * FAR) {
			npc.getNavigation().stop();
			npc.moveTo(at.x, at.y, at.z, post.yaw(), 0);
		} else if (off > STRAY * STRAY) {
			npc.setMeditating(false);
			if (npc.getNavigation().isDone()) npc.getNavigation().moveTo(at.x, at.y, at.z, 0.7);
		} else if (post.seated() && !npc.isMeditating()) {
			CultivatorGoals.sit(npc, post.at());
			npc.setYRot(post.yaw());
			npc.setYHeadRot(post.yaw());
			npc.yBodyRot = post.yaw();
		}
	}

	/** One of the Court's people for {@code post}: of its title and strength, of the righteous path, looking the way its post looks. */
	static CultivatorNpc arrive(ServerLevel level, Post post, RandomSource random) {
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		if (npc == null) return null;
		int rank = switch (post.title()) {
			case HEAVENLY_KING, HEAVENLY_VENERABLE -> PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
			case IMMORTAL_GENERAL -> PlayerCultivation.rank(Realm.TRUE_IMMORTAL, random.nextBoolean() ? Stage.LATE : Stage.GRAND_PERFECTION);
			default -> PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY);
		};
		npc.moveTo(post.at().getX() + 0.5, post.at().getY(), post.at().getZ() + 0.5, post.yaw(), 0);
		npc.becomeRogue(rank, Alignment.RIGHTEOUS_ABOVE + 20 + random.nextInt(30), random);
		Bloodline race = post.title() == NpcTitle.HEAVENLY_VENERABLE ? venerableRace(post.element()) : null;
		if (race != null) npc.setRace(race, random.nextInt(3)); // the Metal and Wood Venerables demons of their races
		npc.setRank(rank, true); // the Court's own, whatever room the realm has for its True Immortals (HighRealms)
		npc.setCourtElement(post.element());
		npc.setTitle(post.title());
		npc.setSettledAt(post.at());
		npc.setYHeadRot(post.yaw());
		npc.setHealth(npc.getMaxHealth());
		level.addFreshEntity(npc);
		return npc;
	}

	// --- Its officers (players) ---

	/** Whether the player {@code id} is an officer of the Court. */
	public static boolean isMember(MinecraftServer server, UUID id) {
		return Data.get(server).members.containsKey(id);
	}

	/** The player {@code id}'s rank in the Court, or null if they aren't of it. */
	public static Rank rankOf(MinecraftServer server, UUID id) {
		Data.Member member = Data.get(server).members.get(id);
		return member == null ? null : Rank.of(member.merit);
	}

	/** The merit the player {@code id} has earned in the Court all told (0 if they aren't of it). */
	public static int meritOf(MinecraftServer server, UUID id) {
		Data.Member member = Data.get(server).members.get(id);
		return member == null ? 0 : member.merit;
	}

	/** {@code player} takes service with the Court: an Officer. @return false if they were of it already */
	public static boolean join(ServerPlayer player) {
		Data data = Data.get(player.server);
		if (data.members.containsKey(player.getUUID())) return false;
		data.members.put(player.getUUID(), new Data.Member());
		data.setDirty();
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
		return true;
	}

	/** A Heavenly Venerable answers {@code player}'s asking of the Court: takes them into it, or tells them where they stand in it. */
	static Component ask(ServerPlayer player, CultivatorNpc npc) {
		Component name = Component.literal(npc.getPersonalName());
		if (!isMember(player.server, player.getUUID())) {
			if (!ImmortalGate.mayPass(player)) return Component.translatable(ModLang.talkLine("court.too_weak"));
			join(player);
			return Component.translatable(ModLang.talkLine("court.join"), name);
		}
		int merit = meritOf(player.server, player.getUUID());
		Rank rank = Rank.of(merit), next = rank.next();
		return next == null ? Component.translatable(ModLang.talkLine("court.member.top"), rank.displayName(), merit)
				: Component.translatable(ModLang.talkLine("court.member"), rank.displayName(), merit, next.merit() - merit, next.displayName());
	}

	/**
	 * Merit for {@code player}'s service: {@code tokens} Court Merit Tokens (into their pack, or at their feet if it is full), as much on the
	 * merit they have earned all told, told over the hotbar ({@code line}, with its {@code args} before the count in all), and their rank
	 * risen if it has.
	 */
	static void award(ServerPlayer player, int tokens, String line, Object... args) {
		Data data = Data.get(player.server);
		Data.Member member = data.members.get(player.getUUID());
		if (member == null || tokens <= 0) return;
		Rank before = Rank.of(member.merit);
		member.merit += tokens;
		data.setDirty();
		ItemStack pay = new ItemStack(ModItems.COURT_MERIT_TOKEN, tokens);
		if (!player.getInventory().add(pay)) player.drop(pay, false);
		Object[] told = new Object[args.length + 1];
		System.arraycopy(args, 0, told, 0, args.length);
		told[args.length] = member.merit;
		player.displayClientMessage(Component.translatable(line, told).withStyle(ChatFormatting.GOLD), true);
		Rank after = Rank.of(member.merit);
		if (after != before) {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_COURT_RANK_UP, after.displayName()).withStyle(ChatFormatting.GOLD));
			player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
		}
	}

	/** Every second: those of the Court standing watch on the Southern Heavenly Gate's plaza while it stands open, a token a minute. */
	static void watch(MinecraftServer server) {
		if (server.getTickCount() % 20 != 11) return;
		Data data = Data.get(server);
		ServerLevel level = server.getLevel(ModDimensions.IMMORTAL_REALM);
		int y = ImmortalGate.heavenY(server);
		if (level == null || y == Integer.MIN_VALUE || !ImmortalGate.isOpen(server)) {
			// Shut: no watch is kept, and a minute half kept is begun again at the next opening.
			boolean any = false;
			for (Data.Member member : data.members.values()) {
				any |= member.watch != 0;
				member.watch = 0;
			}
			if (any) data.setDirty();
			return;
		}
		for (ServerPlayer player : level.players()) {
			Data.Member member = data.members.get(player.getUUID());
			if (member == null || !onWatch(player, y)) continue;
			member.watch += 20;
			data.setDirty();
			if (member.watch < WATCH_TICKS) continue;
			member.watch -= WATCH_TICKS;
			award(player, 1, ModLang.MSG_COURT_MERIT_WATCH);
		}
	}

	/** Whether {@code player} stands on the Southern Heavenly Gate's plaza (its paving's height {@code y}), there in body. */
	static boolean onWatch(ServerPlayer player, int y) {
		double dx = player.getX() - (ImmortalGate.HEAVEN_X + 0.5), dz = player.getZ() - (ImmortalGate.HEAVEN_Z + 0.5);
		return player.isAlive() && !player.isSpectator() && dx * dx + dz * dz <= (double) ImmortalGate.PLAZA_RADIUS * ImmortalGate.PLAZA_RADIUS
				&& player.getY() >= y - 1 && player.getY() <= y + 12 && !SoulProjection.isAwayFromBody(player.getUUID());
	}

	/** {@code npc} slain by {@code killer}: one of the realm's devilish immortals, merit for an officer of the Court ({@link #bounty}). */
	static void onSlain(CultivatorNpc npc, ServerPlayer killer) {
		if (killer == null || !ModDimensions.isImmortalRealm(npc.level().dimension()) || !isMember(killer.server, killer.getUUID())) return;
		int bounty = bounty(npc);
		if (bounty > 0) award(killer, bounty, ModLang.MSG_COURT_MERIT_SLAIN, Component.literal(npc.getPersonalName()), bounty);
	}

	/**
	 * One of the realm's primordial beasts slain (PrimordialBeast#isOfTheRealm: not one called up by an egg or a command): {@link #SLAY_PRIMORDIAL}
	 * merit for each officer of the Court who fought it and is in the world now (its tokens in their pack).
	 */
	public static void onPrimordialSlain(PrimordialBeast beast, Collection<UUID> fought) {
		if (!(beast.level() instanceof ServerLevel level) || !beast.isOfTheRealm()) return;
		for (UUID id : fought) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player == null || !isMember(level.getServer(), id)) continue;
			award(player, SLAY_PRIMORDIAL, ModLang.MSG_COURT_MERIT_PRIMORDIAL, beast.getDisplayName(), SLAY_PRIMORDIAL);
		}
	}

	/** The merit the Court pays for {@code npc} slain: a devilish True Immortal {@link #SLAY_IMMORTAL}, a devilish Entering Nirvana {@link #SLAY_NIRVANA}; else none. */
	static int bounty(CultivatorNpc npc) {
		if (npc.getFaction() != Alignment.Faction.DEVILISH || npc.isMortal() || npc.getRank() < ImmortalSects.FLOOR || isCourtier(npc)) return 0;
		return CultivatorNpc.realmOf(npc.getRank()) == Realm.TRUE_IMMORTAL ? SLAY_IMMORTAL : SLAY_NIRVANA;
	}

	/** Each server tick: the Court kept peopled, the Gate's watch counted. */
	public static void tick(MinecraftServer server) {
		try {
			keep(server);
			watch(server);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Celestial Court tick failed", e);
		}
	}

	/** The Sect tab's line of the Court (SectMembership#sendInfo): {@code player}'s rank (its index and one more; 0 for none) and merit. */
	public static void writeInfo(ServerPlayer player, FriendlyByteBuf buf) {
		Rank rank = rankOf(player.server, player.getUUID());
		buf.writeVarInt(rank == null ? 0 : rank.ordinal() + 1);
		buf.writeVarInt(meritOf(player.server, player.getUUID()));
	}

	// --- The Quartermaster ---

	/** One of the Quartermaster's wares: the thing, its price in tokens, how many a day, the rank it wants. */
	record Ware(ItemStack item, int tokens, int maxUses, Rank rank) {}

	/**
	 * What the Court's Quartermaster has for tokens today: three Immortal Jade for a token, and each element's True Immortal method's slip,
	 * to every officer; to Captains, Immortal-grade Cultivation Pills and a Sacred Vessel (the day's element); to Generals, the ghost path's
	 * last foundation (the Netherworld Immortal Canon) and a Ring of Transcendence.
	 */
	static List<Ware> wares(long day) {
		List<Ware> wares = new ArrayList<>();
		wares.add(new Ware(new ItemStack(ModItems.IMMORTAL_JADE, 3), 1, 16, Rank.OFFICER));
		for (QiElement element : QiElement.five()) wares.add(new Ware(JadeSlipItem.of(HeavenMethod.of(element, Realm.TRUE_IMMORTAL)), 20, 1, Rank.OFFICER));
		wares.add(new Ware(PillItem.create(ModItems.CULTIVATION_PILL, PillGrade.IMMORTAL, 100), 3, 2, Rank.CAPTAIN));
		QiElement todays = QiElement.five()[(int) Math.floorMod(day, 5L)];
		wares.add(new Ware(new ItemStack(ModItems.sacredVessel(todays)), 15, 1, Rank.CAPTAIN));
		wares.add(new Ware(GhostJadeSlipItem.of(GhostMethod.NETHERWORLD_IMMORTAL_CANON), 30, 1, Rank.GENERAL));
		wares.add(new Ware(new ItemStack(ModItems.RING_OF_TRANSCENDENCE), 25, 1, Rank.GENERAL));
		return wares;
	}

	/**
	 * The Quartermaster's offers for {@code player} today (CultivatorTrades#offersFor): each ware for its tokens ({@code uses} already made of
	 * each today, keyed in {@code keys}); those their rank doesn't open yet, or all to one not of the Court, shown crossed out with the rank
	 * they want.
	 */
	public static MerchantOffers offers(CultivatorNpc npc, ServerPlayer player, Map<String, Integer> uses, Map<MerchantOffer, String> keys) {
		MerchantOffers offers = new MerchantOffers();
		keys.clear();
		Rank rank = rankOf(player.server, player.getUUID());
		List<Ware> wares = wares(Regard.day(npc.level().getGameTime()));
		for (int i = 0; i < wares.size(); i++) {
			Ware ware = wares.get(i);
			ItemStack cost = new ItemStack(ModItems.COURT_MERIT_TOKEN, ware.tokens());
			if (rank == null || rank.ordinal() < ware.rank().ordinal()) {
				MerchantOffer locked = new MerchantOffer(cost, ItemStack.EMPTY, locked(ware.item(), ware.rank(), rank), ware.maxUses(), ware.maxUses(), 0, 0);
				offers.add(locked);
				keys.put(locked, "locked:" + i);
				continue;
			}
			String key = "court:" + i;
			MerchantOffer offer = new MerchantOffer(cost, ItemStack.EMPTY, ware.item().copy(), uses.getOrDefault(key, 0), ware.maxUses(), 0, 0);
			offers.add(offer);
			keys.put(offer, key);
		}
		return offers;
	}

	/** {@code item} as a line not yet open: under its name, the rank it wants and theirs (or that they aren't of the Court). */
	static ItemStack locked(ItemStack item, Rank wants, Rank have) {
		ItemStack shown = item.copy();
		CompoundTag display = shown.getOrCreateTagElement("display");
		ListTag lore = display.getList("Lore", Tag.TAG_STRING);
		Component theirs = have == null ? Component.translatable(ModLang.TRADE_NOT_OF_COURT) : have.displayName();
		Component line = Component.translatable(ModLang.TRADE_LOCKED_RANK, wants.displayName(), theirs)
				.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false));
		lore.add(0, StringTag.valueOf(Component.Serializer.toJson(line)));
		display.put("Lore", lore);
		return shown;
	}

	// --- Bookkeeping ---

	/**
	 * Whether the Court has been peopled and when its last arrival came; and its officers (players), each with the merit they have earned all
	 * told and the ticks of the Gate's watch they have kept toward their next token. Kept with the Overworld's data (the Court is the
	 * server's).
	 */
	static final class Data extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_celestial_court";
		boolean peopled;
		long lastArrival;
		final Map<UUID, Member> members = new HashMap<>();

		static final class Member {
			int merit;
			int watch;
		}

		static Data get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			data.peopled = tag.getBoolean("Peopled");
			data.lastArrival = tag.getLong("LastArrival");
			ListTag list = tag.getList("Members", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag m = list.getCompound(i);
				if (!m.hasUUID("Id")) continue;
				Member member = new Member();
				member.merit = m.getInt("Merit");
				member.watch = m.getInt("Watch");
				data.members.put(m.getUUID("Id"), member);
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			tag.putBoolean("Peopled", peopled);
			tag.putLong("LastArrival", lastArrival);
			ListTag list = new ListTag();
			for (Map.Entry<UUID, Member> e : members.entrySet()) {
				CompoundTag m = new CompoundTag();
				m.putUUID("Id", e.getKey());
				m.putInt("Merit", e.getValue().merit);
				m.putInt("Watch", e.getValue().watch);
				list.add(m);
			}
			tag.put("Members", list);
			return tag;
		}
	}

	private CelestialCourt() {}
}
