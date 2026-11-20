package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Puppetry (the fifth craft, CraftMastery; the user's "begin phase 1" of the puppet design, 2026-11-17): its rules in one place.
 * <ul>
 *   <li>A puppet is a frame (PuppetFrameItem, forged at the Artifact Forge), a demon core for its heart and its fuel for ever, and the
 *   circuits its puppeteer paints on the frame's board with the Inscription Brush (CircuitBoard) at a Puppet Workbench, then seals there
 *   (PuppetBoards) for {@link #SEAL_STONES} of the frame's grade's spirit stones.</li>
 *   <li>The frame's grade sets the realms its puppet may stand at ({@link #lowest}, {@link #highest}): Mortal Qi Refining to Foundation
 *   Building, Spirit Core Formation, Earth Nascent Soul to Heavenly Being, Heaven Four Axis to Void Shattering, Immortal Sacred Vessel to True
 *   Immortal. The core sets its realm and stage within that ({@link #puppetRank}); a core too weak for the frame won't go in, one too
 *   strong runs at the frame's top. Nothing depends on the puppeteer's own realm (the user's word).</li>
 *   <li>A puppet is {@link #MIGHT} of a cultivator of its realm, as its circuits allow (PuppetEntity), and never cultivates.</li>
 *   <li>Puppetry's rank gives the puppeteer more strokes and readings on the board and a better Circuit Path (the board's screen),
 *   more puppets kept at once ({@link #slots}: a puppet weighs its frame's grade, {@link #cost}) and a longer reach ({@link #reach}).</li>
 * </ul>
 * Every player's puppets are kept in a ledger ({@link Ledger}: where each is, its order, its health), so the Command Seal's list shows
 * them wherever they stand and a puppet in an unloaded place still counts against its puppeteer's slots.
 */
public final class Puppetry {
	/** A puppet's might against a cultivator of its realm (the design: about seventy percent). */
	public static final double MIGHT = 0.7;
	/** The puppets' weight a puppeteer may keep, by Puppetry rank (Apprentice to Saint), before Spirit's share. */
	public static final int[] SLOTS = {2, 4, 7, 11, 16};
	/** Points of Spirit (StatPoints) for each slot more. */
	public static final int SPIRIT_PER_SLOT = 20;
	/** How far a puppeteer's will reaches, by rank (blocks), unless their Consciousness Domain reaches farther. */
	public static final double[] REACH = {32, 48, 64, 96, 128};
	/** The spirit stones of its frame's grade sealing a puppet burns. */
	public static final int SEAL_STONES = 4;

	// --- The frame's grade and the core ---

	/** The least realm a puppet of a frame of {@code grade} stands at (its core must be of it or above). */
	public static Realm lowest(ArtifactGrade grade) {
		return switch (grade) {
			case MORTAL -> Realm.QI_REFINING;
			case SPIRIT -> Realm.CORE_FORMATION;
			case EARTH -> Realm.NASCENT_SOUL;
			case HEAVEN -> Realm.FOUR_AXIS;
			case IMMORTAL -> Realm.SACRED_VESSEL;
		};
	}

	/** The highest realm a puppet of a frame of {@code grade} can stand at, whatever its core. */
	public static Realm highest(ArtifactGrade grade) {
		return switch (grade) {
			case MORTAL -> Realm.FOUNDATION_BUILDING;
			case SPIRIT -> Realm.CORE_FORMATION;
			case EARTH -> Realm.HEAVENLY_BEING;
			case HEAVEN -> Realm.VOID_SHATTERING;
			case IMMORTAL -> Realm.TRUE_IMMORTAL;
		};
	}

	/** "Its puppet stands from Qi Refining to Foundation Building, as its core." */
	public static Component bandLine(ArtifactGrade grade) {
		Realm low = lowest(grade), high = highest(grade);
		return low == high ? Component.translatable(ModLang.FRAME_BAND_ONE, low.getDisplayName())
				: Component.translatable(ModLang.FRAME_BAND, low.getDisplayName(), high.getDisplayName());
	}

	/**
	 * The rank (PlayerCultivation#rank) a puppet of a frame of {@code grade} with a core of {@code coreRank} stands at: the core's, the
	 * frame's top at most; -1 if the core is weaker than the frame's least realm.
	 */
	public static int puppetRank(ArtifactGrade grade, int coreRank) {
		int least = PlayerCultivation.rank(lowest(grade), Stage.EARLY), most = PlayerCultivation.rank(highest(grade), Stage.GRAND_PERFECTION);
		if (coreRank < least) return -1;
		return Math.min(coreRank, most);
	}

	/** The slots a puppet of a frame of {@code grade} takes: one Mortal, two Spirit ... five Immortal. */
	public static int cost(ArtifactGrade grade) {
		return grade.ordinal() + 1;
	}

	public static CraftMastery.Rank rank(PlayerCultivation c) {
		return CraftMastery.rank(c, CraftMastery.Craft.PUPPETRY);
	}

	/** The puppets' weight {@code c} may keep: their rank's, and a slot more for every {@link #SPIRIT_PER_SLOT} points of Spirit. */
	public static int slots(PlayerCultivation c) {
		return SLOTS[rank(c).ordinal()] + c.getStatPoints(StatPoints.Stat.SPIRIT) / SPIRIT_PER_SLOT;
	}

	/** How far {@code c}'s will reaches a puppet of perfect Clarity: their rank's reach, or their Consciousness Domain if it is wider. */
	public static double reach(PlayerCultivation c) {
		return Math.max(REACH[rank(c).ordinal()], ConsciousnessDomainHandler.radius(c));
	}

	/** The practice sealing a puppet of a frame of {@code grade} teaches, by how well its circuits came out: more for bigger frames and better work. */
	public static int sealPractice(ArtifactGrade grade, CircuitBoard.Grade circuits) {
		double worth = switch (circuits) {
			case CRUDE -> 0.5;
			case NORMAL -> 1.0;
			case FINE -> 1.5;
			case PERFECT -> 2.0;
		};
		return Math.max(1, (int) Math.round((grade.ordinal() + 1) * 2 * worth));
	}

	// --- The core's qi and the veins (the deeper circuits, phase A: CircuitBoard) ---

	/**
	 * The qi a puppet's core gives its lines: its frame's (CircuitBoard#BUDGET), and a point more for each stage past Early its core stands
	 * at in the frame (Mid 1, Late 2, Grand Perfection 3). With no core yet, the frame's own.
	 */
	public static int budget(ArtifactGrade grade, ItemStack core) {
		int qi = CircuitBoard.BUDGET[grade.ordinal()];
		if (core.isEmpty()) return qi;
		int rank = puppetRank(grade, DemonCoreItem.rank(core));
		return rank < 0 ? qi : qi + CultivatorNpc.stageOf(rank).ordinal();
	}

	/**
	 * What a part's veins make of it, one vein to four (a part with none is dead): the arms' blows, the legs' pace, the wings' flight, a bee's
	 * legs' load, the art, the heart's command reach, and how slow the head is to take up a foe; the head's sight in blocks.
	 */
	public static final double[] ARM_STEP = {1.0, 1.2, 1.35, 1.5}, LEG_STEP = {1.0, 1.1, 1.2, 1.3}, WING_STEP = {1.0, 1.15, 1.3, 1.45},
			CARGO_STEP = {1.0, 1.5, 2.0, 2.5}, ART_STEP = {1.0, 1.25, 1.5, 1.75}, REACH_STEP = {1.0, 1.25, 1.5, 1.75},
			REACTION_STEP = {1.0, 0.75, 0.5, 0.25};
	public static final int[] SIGHT = {32, 40, 52, 64};

	/** {@code steps}' step for a part of {@code veins} veins (one at least, four at most). */
	public static double step(double[] steps, int veins) {
		return steps[Math.max(1, Math.min(steps.length, veins)) - 1];
	}

	/** The head's sight in blocks for {@code veins} veins. */
	public static int sight(int veins) {
		return SIGHT[Math.max(1, Math.min(SIGHT.length, veins)) - 1];
	}

	// --- Phases (the deeper circuits, phase B: CircuitBoard.Phase) ---

	/** The board's phase of {@code element} (null for yin or none: such a core resonates with no seal). */
	public static @Nullable CircuitBoard.Phase phaseOf(@Nullable QiElement element) {
		if (element == null) return null;
		return switch (element) {
			case METAL -> CircuitBoard.Phase.METAL;
			case WOOD -> CircuitBoard.Phase.WOOD;
			case WATER -> CircuitBoard.Phase.WATER;
			case FIRE -> CircuitBoard.Phase.FIRE;
			case EARTH -> CircuitBoard.Phase.EARTH;
			default -> null;
		};
	}

	/** The phase a demon core's element gives it (none for yin or no core). */
	public static @Nullable CircuitBoard.Phase phaseOf(ItemStack core) {
		return core.isEmpty() ? null : phaseOf(DemonCoreItem.element(core));
	}

	public static QiElement elementOf(CircuitBoard.Phase phase) {
		return switch (phase) {
			case METAL -> QiElement.METAL;
			case WOOD -> QiElement.WOOD;
			case WATER -> QiElement.WATER;
			case FIRE -> QiElement.FIRE;
			case EARTH -> QiElement.EARTH;
		};
	}

	/** Whether {@code stack} is the token a phase seal of {@code phase} takes: the forge's element token (ArtifactRecipes#tokenElement). */
	public static boolean sealToken(ItemStack stack, CircuitBoard.Phase phase) {
		return ArtifactRecipes.tokenElement(stack) == elementOf(phase);
	}

	/** A token of {@code phase}'s to name (any sapling serves for Wood). */
	public static net.minecraft.world.item.Item tokenItem(CircuitBoard.Phase phase) {
		return switch (phase) {
			case METAL -> net.minecraft.world.item.Items.IRON_NUGGET;
			case WOOD -> net.minecraft.world.item.Items.OAK_SAPLING;
			case WATER -> net.minecraft.world.item.Items.PRISMARINE_SHARD;
			case FIRE -> net.minecraft.world.item.Items.BLAZE_POWDER;
			case EARTH -> net.minecraft.world.item.Items.CLAY_BALL;
		};
	}

	/** Metal on a puppet's body: each Metal vein there a 15% chance a cut glances off (at most 60%). */
	public static double glanceChance(int metalVeins) {
		return Math.min(0.6, 0.15 * metalVeins);
	}

	/** Wood on a puppet's body: a cut Wood line grows back after this many ticks. */
	public static final int REGROW_TICKS = 600;
	/** Earth on a puppet's body: each Earth vein there this much more health, and this much knockback resistance (five veins at most). */
	public static final double EARTH_HEALTH = 0.10, EARTH_STEADY = 0.10;
	public static final int EARTH_MOST = 5;
	/** Fire on a puppet's body: this much swifter, and a point more heat a blow. */
	public static final double FIRE_PACE = 0.10;

	// --- Its arrays (the deeper circuits, phase C: CircuitBoard.Array) ---

	/** An Eye of Discernment: this much farther sight (and it sees the unseen and takes up a foe at once). */
	public static final int EYE_SIGHT = 16;
	/** A Guard Coil turns a blow aside every this many ticks; a Gale Stair dashes every this many, this far at most. */
	public static final int GUARD_TICKS = 160, STAIR_TICKS = 120;
	public static final double STAIR_REACH = 8;
	/** A Gale Stair's dash takes this many ticks. */
	public static final int DASH_TICKS = 4;
	/** A Gathering Spiral: its art's charges come back this many times as fast. */
	public static final int SPIRAL_RECHARGE = 2;
	/** An Iron Lock: a Circuit-Freeze Talisman holds it this share of the time. */
	public static final double LOCK_FREEZE = 0.5;

	/** A cooldown of {@code ticks}, halved by a Twin Seal. */
	public static int cooldown(int ticks, boolean twin) {
		return twin ? ticks / 2 : ticks;
	}

	// --- Ranks of command (puppet phase 2c: CommandSealItem, SubSealItem, PuppetCommands) ---

	/** Raising a Command Seal a grade burns this many spirit stones of the grade it rises to. */
	public static final int SEAL_RAISE_STONES = 4;

	/** The highest grade a Command Seal of a puppeteer of {@code rank} goes to: Apprentice Mortal, Adept Spirit ... Saint Immortal. */
	public static ArtifactGrade highestSeal(CraftMastery.Rank rank) {
		return ArtifactGrade.byIndex(rank.ordinal());
	}

	/**
	 * A General: a humanoid of a frame of {@link #GENERAL_FRAME} or better, made by a puppeteer of {@link #GENERAL_RANK} or above for
	 * {@link #GENERAL_STONES} of its frame's grade's spirit stones; it takes up to {@link #GENERAL_RANKS} of its puppeteer's other puppets
	 * into its ranks, from within {@link #MUSTER_REACH} blocks of it.
	 */
	public static final ArtifactGrade GENERAL_FRAME = ArtifactGrade.EARTH;
	public static final CraftMastery.Rank GENERAL_RANK = CraftMastery.Rank.GRANDMASTER;
	public static final int GENERAL_STONES = 8, GENERAL_RANKS = 8;
	public static final double MUSTER_REACH = 16;

	/** Why {@code puppet} of a puppeteer of {@code rank} can't be made a General (null: it can). */
	public static @Nullable String generalRefusal(PuppetEntity puppet, CraftMastery.Rank rank) {
		if (rank.ordinal() < GENERAL_RANK.ordinal()) return ModLang.GENERAL_RANK;
		if (puppet.isBee() || puppet.frameGrade().ordinal() < GENERAL_FRAME.ordinal()) return ModLang.GENERAL_FRAME;
		return null;
	}

	// --- The linked army (puppet phase 3) ---

	/**
	 * Link Knots (CircuitBoard.Stroke#LINK): one puppeteer's linked puppets within {@link #LINK_REACH} blocks of each other strengthen one
	 * another, each linked fellow in reach {@link #LINK_STEP} harder blows and art, {@link #LINK_MOST} at most, and pass their puppeteer's
	 * orders on beyond their Seal's reach (PuppetCommands#inReach).
	 */
	public static final double LINK_REACH = 16, LINK_STEP = 0.03, LINK_MOST = 0.30;

	/** What {@code fellows} linked puppets in reach give one: +3% each, +30% at most. */
	public static double linkBonus(int fellows) {
		return Math.min(LINK_MOST, LINK_STEP * Math.max(0, fellows));
	}

	/**
	 * Formations (PuppetFormations): {@link #FORMATION_SIZE} or more of one puppeteer's linked puppets at one foe spread round it; each within
	 * {@link #FORMATION_REACH} blocks of it, no gap between neighbours wider than {@link #FORMATION_GAP} degrees, it is locked: Bound, and
	 * crushed every second for {@link #FORMATION_CRUSH} of their blows together.
	 */
	public static final int FORMATION_SIZE = 4;
	public static final double FORMATION_REACH = 4, FORMATION_GAP = 135, FORMATION_CRUSH = 0.25;

	/** Knowing an enemy's circuits (their design learned from a wreck: {@link #learn}): blows at a puppet of that design land this much harder. */
	public static final double KNOWN_HARM = 1.25;

	/** Self-repair: a puppet given a Spring of the Ancients mends one of its own cut lines every this many ticks. */
	public static final int SPRING_MEND_TICKS = 400;

	/**
	 * Shielded core: a puppet above the lower realms' cap, where a lower realm's law holds (or held down by another's Realm Suppress), stands
	 * at this share of its health and harm; a shielded core (DemonCoreItem#shielded) keeps it whole.
	 */
	public static final double SUPPRESSED = 0.5;

	/** Self-destruct: its fuse (ticks), its blast this many times its blow, a quarter of that at the blast's edge. */
	public static final int FUSE_TICKS = 30;
	public static final double BURST_POWER = 6, BURST_EDGE = 0.25;

	/** How far a puppet of a frame of {@code grade} bursts: 3 blocks at Mortal, 1.5 more each grade. */
	public static double burstReach(ArtifactGrade grade) {
		return 3 + 1.5 * grade.ordinal();
	}

	/**
	 * The Puppet Factory (PuppetFactoryBlockEntity): a copy takes {@link #FACTORY_TICKS} a grade from Mortal's one; within
	 * {@link #FACTORY_REACH} blocks it mends one cut line, and gives one piece of matter's health, every {@link #FACTORY_MEND_TICKS}.
	 */
	public static final int FACTORY_TICKS = 600, FACTORY_MEND_TICKS = 100;
	public static final double FACTORY_REACH = 8;

	public static int factoryTicks(ArtifactGrade grade) {
		return FACTORY_TICKS * (grade.ordinal() + 1);
	}

	/** The Overseer Core's wear map: each puppeteer's designs kept at most. */
	public static final int WEAR_DESIGNS = 32;

	/** A drawing's design: its make, its grade and its strokes as one number (FNV-1a over them). Every puppet built to it bears it. */
	public static long design(PuppetBlueprint blueprint, ArtifactGrade grade, byte[] strokes) {
		long h = 0xcbf29ce484222325L;
		h = (h ^ blueprint.ordinal()) * 0x100000001b3L;
		h = (h ^ grade.ordinal()) * 0x100000001b3L;
		for (byte b : strokes) h = (h ^ (b & 0xFF)) * 0x100000001b3L;
		return h;
	}

	/**
	 * Knowing the enemy's circuits: {@code player} has wrecked {@code puppet}, another's: its design is theirs to know from now on (harder
	 * blows, aimed cuts, easier seizing), and its Circuit Diagram lies in its wreck.
	 */
	public static void learn(ServerPlayer player, PuppetEntity puppet) {
		boolean fresh = Ledger.get(player.server).learn(player.getUUID(), puppet.design());
		puppet.spawnAtLocation(CircuitDiagramItem.of(puppet.blueprint(), puppet.frameGrade(), puppet.designStrokes(), puppet.ownerName(), false));
		if (fresh) {
			player.sendSystemMessage(Component.translatable(ModLang.DESIGN_LEARNED, puppet.getDisplayName(), (int) Math.round((KNOWN_HARM - 1) * 100))
					.withStyle(ChatFormatting.GOLD));
		}
	}

	/** Whether {@code player} knows {@code design} (they wrecked a puppet built to it). */
	public static boolean knows(MinecraftServer server, @Nullable UUID player, long design) {
		return player != null && Ledger.get(server).knows(player, design);
	}

	// --- What a board's scores do (PuppetEntity, and the board's screen tells them) ---

	/** Flow: its blows and its art, x0.8 at nothing to x1.1 at a hundred. */
	public static double flowFactor(int flow) {
		return 0.8 + 0.3 * flow / 100.0;
	}

	/** Harmony: its pace, its aim and its dodging, x0.8 at nothing to x1.1 at a hundred (a miss below 1, a dodge above it). */
	public static double harmonyFactor(int harmony) {
		return 0.8 + 0.3 * harmony / 100.0;
	}

	/** The heat a blow and an art put into a puppet's lines (on every frame), and the ticks it takes to shed a point of it. */
	public static final int BLOW_HEAT = 1, ART_HEAT = 3, COOL_TICKS = 40;

	/** Stability: how much heat a puppet bears before it overheats and stalls (Integer.MAX_VALUE: never, from 85). */
	public static int overheatAfter(int stability) {
		return stability >= 85 ? Integer.MAX_VALUE : stability >= 65 ? 24 : stability >= 40 ? 12 : 6;
	}

	/** Clarity: the ticks it is slow to take up a new foe or order (a second at nothing). */
	public static int reactionTicks(int clarity) {
		return Math.max(0, (100 - clarity) / 5);
	}

	/** Clarity: the chance its art misfires, a quarter at nothing. */
	public static double misfireChance(int clarity) {
		return Math.max(0, 100 - clarity) / 400.0;
	}

	/** Clarity: the share of its puppeteer's reach it hears them at, half at nothing. */
	public static double clarityReach(int clarity) {
		return 0.5 + clarity / 200.0;
	}

	// --- A board's drawing, kept as bytes (a stroke's order a cell) ---

	public static byte[] strokes(CircuitBoard board) {
		byte[] out = new byte[board.size * board.size];
		for (int y = 0; y < board.size; y++) {
			for (int x = 0; x < board.size; x++) out[board.at(x, y)] = (byte) board.stroke(x, y).ordinal();
		}
		return out;
	}

	/** Lays {@code strokes} on {@code board} (what falls on a flaw or a fixed cell is left out). */
	public static void lay(CircuitBoard board, byte[] strokes) {
		CircuitBoard.Stroke[] all = CircuitBoard.Stroke.values();
		for (int y = 0; y < board.size; y++) {
			for (int x = 0; x < board.size; x++) {
				int i = board.at(x, y);
				if (i >= strokes.length) continue;
				int s = strokes[i];
				board.set(x, y, s > 0 && s < all.length ? all[s] : CircuitBoard.Stroke.NONE);
			}
		}
	}

	// --- The ledger ---

	/** One puppet as its puppeteer's ledger knows it: what it is, what it was told, how it stands and where. */
	public record Entry(UUID id, String name, PuppetBlueprint blueprint, ArtifactGrade grade, int rank, int order, float health, float maxHealth,
			ResourceKey<Level> dimension, BlockPos pos, boolean inert, boolean away) {
		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("Id", id);
			tag.putString("Name", name);
			tag.putString("Blueprint", blueprint.getId());
			tag.putByte("Grade", (byte) grade.ordinal());
			tag.putInt("Rank", rank);
			tag.putByte("Order", (byte) order);
			tag.putFloat("Health", health);
			tag.putFloat("MaxHealth", maxHealth);
			tag.putString("Dimension", dimension.location().toString());
			tag.putLong("Pos", pos.asLong());
			tag.putBoolean("Inert", inert);
			tag.putBoolean("Away", away);
			return tag;
		}

		static @Nullable Entry load(CompoundTag tag) {
			PuppetBlueprint blueprint = PuppetBlueprint.byId(tag.getString("Blueprint"));
			ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
			if (blueprint == null || dimension == null || !tag.hasUUID("Id")) return null;
			return new Entry(tag.getUUID("Id"), tag.getString("Name"), blueprint, ArtifactGrade.byIndex(tag.getByte("Grade")), tag.getInt("Rank"),
					tag.getByte("Order"), tag.getFloat("Health"), tag.getFloat("MaxHealth"), ResourceKey.create(Registries.DIMENSION, dimension),
					BlockPos.of(tag.getLong("Pos")), tag.getBoolean("Inert"), tag.getBoolean("Away"));
		}
	}

	/** Every player's puppets (kept with the overworld's data), and the messenger bees in flight (PuppetCouriers). */
	public static final class Ledger extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_puppets";
		private final Map<UUID, Map<UUID, Entry>> byOwner = new HashMap<>();
		/** Messenger bees on the wing, out of the world while they fly (PuppetCouriers). */
		final List<CompoundTag> flights = new ArrayList<>();
		/** Puppets in a General's ranks (phase 2c), out of the world in it: each one's General's name. */
		private final Map<UUID, String> inRanks = new HashMap<>();
		/** Players' souls held in puppets (phase 2d: PuppetPlayers), each player's: its puppet, how they stood before, what waits for them. */
		final Map<UUID, CompoundTag> boundSouls = new HashMap<>();
		/** The designs each player knows (phase 3: Puppetry#learn, a wreck of each taken). */
		private final Map<UUID, java.util.Set<Long>> known = new HashMap<>();
		/** Each player's puppets' wear in fights by design (phase 3: the Overseer Core's wear map), the latest last. */
		private final Map<UUID, LinkedHashMap<Long, Wear>> wear = new HashMap<>();

		public static Ledger get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(Ledger::load, Ledger::new, NAME);
		}

		private static Ledger load(CompoundTag tag) {
			Ledger ledger = new Ledger();
			for (Tag t : tag.getList("Owners", Tag.TAG_COMPOUND)) {
				CompoundTag owner = (CompoundTag) t;
				if (!owner.hasUUID("Owner")) continue;
				Map<UUID, Entry> puppets = new LinkedHashMap<>();
				for (Tag p : owner.getList("Puppets", Tag.TAG_COMPOUND)) {
					Entry entry = Entry.load((CompoundTag) p);
					if (entry != null) puppets.put(entry.id(), entry);
				}
				if (!puppets.isEmpty()) ledger.byOwner.put(owner.getUUID("Owner"), puppets);
			}
			for (Tag t : tag.getList("Flights", Tag.TAG_COMPOUND)) ledger.flights.add((CompoundTag) t);
			for (Tag t : tag.getList("InRanks", Tag.TAG_COMPOUND)) {
				CompoundTag r = (CompoundTag) t;
				if (r.hasUUID("Id")) ledger.inRanks.put(r.getUUID("Id"), r.getString("General"));
			}
			for (Tag t : tag.getList("BoundSouls", Tag.TAG_COMPOUND)) {
				CompoundTag b = (CompoundTag) t;
				if (b.hasUUID("Player")) ledger.boundSouls.put(b.getUUID("Player"), b.getCompound("Binding"));
			}
			for (Tag t : tag.getList("Known", Tag.TAG_COMPOUND)) {
				CompoundTag k = (CompoundTag) t;
				if (!k.hasUUID("Player")) continue;
				java.util.Set<Long> designs = new java.util.HashSet<>();
				for (long d : k.getLongArray("Designs")) designs.add(d);
				ledger.known.put(k.getUUID("Player"), designs);
			}
			for (Tag t : tag.getList("Wear", Tag.TAG_COMPOUND)) {
				CompoundTag o = (CompoundTag) t;
				if (!o.hasUUID("Owner")) continue;
				LinkedHashMap<Long, Wear> designs = new LinkedHashMap<>();
				for (Tag d : o.getList("Designs", Tag.TAG_COMPOUND)) {
					CompoundTag w = (CompoundTag) d;
					designs.put(w.getLong("Design"), Wear.load(w));
				}
				ledger.wear.put(o.getUUID("Owner"), designs);
			}
			return ledger;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag owners = new ListTag();
			byOwner.forEach((owner, puppets) -> {
				CompoundTag o = new CompoundTag();
				o.putUUID("Owner", owner);
				ListTag list = new ListTag();
				for (Entry entry : puppets.values()) list.add(entry.save());
				o.put("Puppets", list);
				owners.add(o);
			});
			tag.put("Owners", owners);
			ListTag flying = new ListTag();
			flying.addAll(flights);
			tag.put("Flights", flying);
			ListTag ranks = new ListTag();
			inRanks.forEach((id, general) -> {
				CompoundTag r = new CompoundTag();
				r.putUUID("Id", id);
				r.putString("General", general);
				ranks.add(r);
			});
			tag.put("InRanks", ranks);
			ListTag souls = new ListTag();
			boundSouls.forEach((player, binding) -> {
				CompoundTag b = new CompoundTag();
				b.putUUID("Player", player);
				b.put("Binding", binding);
				souls.add(b);
			});
			tag.put("BoundSouls", souls);
			ListTag knownList = new ListTag();
			known.forEach((player, designs) -> {
				CompoundTag k = new CompoundTag();
				k.putUUID("Player", player);
				k.putLongArray("Designs", designs.stream().mapToLong(Long::longValue).toArray());
				knownList.add(k);
			});
			tag.put("Known", knownList);
			ListTag wearList = new ListTag();
			wear.forEach((owner, designs) -> {
				CompoundTag o = new CompoundTag();
				o.putUUID("Owner", owner);
				ListTag list = new ListTag();
				designs.forEach((design, w) -> list.add(w.save(design)));
				o.put("Designs", list);
				wearList.add(o);
			});
			tag.put("Wear", wearList);
			return tag;
		}

		// --- Known designs and wear (phase 3) ---

		/** {@code player} knows {@code design} from now on; whether they hadn't before. */
		public boolean learn(UUID player, long design) {
			boolean fresh = known.computeIfAbsent(player, p -> new java.util.HashSet<>()).add(design);
			if (fresh) setDirty();
			return fresh;
		}

		public boolean knows(UUID player, long design) {
			java.util.Set<Long> designs = known.get(player);
			return designs != null && designs.contains(design);
		}

		/** {@code owner}'s wear of {@code design}, or null if none of its puppets has been cut or overheated yet. */
		public @Nullable Wear wearOf(UUID owner, long design) {
			Map<Long, Wear> designs = wear.get(owner);
			return designs == null ? null : designs.get(design);
		}

		/** {@code owner}'s record of {@code design} (a board of {@code cells} cells), kept the latest (the eldest let go past WEAR_DESIGNS). */
		private Wear wearFor(UUID owner, long design, int cells) {
			LinkedHashMap<Long, Wear> designs = wear.computeIfAbsent(owner, o -> new LinkedHashMap<>());
			Wear w = designs.remove(design);
			if (w == null || w.cuts.length != cells) w = new Wear(cells);
			designs.put(design, w);
			while (designs.size() > WEAR_DESIGNS) designs.remove(designs.keySet().iterator().next());
			setDirty();
			return w;
		}

		/** A line of {@code cell} cut in a fight on {@code owner}'s puppet of {@code design}. */
		public void recordCut(UUID owner, long design, int cells, int cell) {
			Wear w = wearFor(owner, design, cells);
			if (cell >= 0 && cell < cells) w.cuts[cell]++;
			w.cutsTotal++;
		}

		/** {@code owner}'s puppet of {@code design} overheated, the cells of {@code heat} running hot. */
		public void recordOverheat(UUID owner, long design, int[] heat) {
			Wear w = wearFor(owner, design, heat.length);
			for (int i = 0; i < heat.length; i++) if (heat[i] > 0) w.heat[i]++;
			w.overheats++;
		}

		/** {@code puppet} gone into the ranks of the General named {@code general} (out of the world, still its puppeteer's), or back out. */
		public void inRanks(UUID owner, UUID puppet, @Nullable String general) {
			if (general == null) inRanks.remove(puppet);
			else inRanks.put(puppet, general);
			markAway(owner, puppet, general != null);
			setDirty();
		}

		/** The name of the General whose ranks {@code puppet} is in, or null. */
		public @Nullable String generalOf(UUID puppet) {
			return inRanks.get(puppet);
		}

		/** {@code owner}'s puppets, in the order they were made. */
		public List<Entry> of(UUID owner) {
			Map<UUID, Entry> puppets = byOwner.get(owner);
			return puppets == null ? List.of() : List.copyOf(puppets.values());
		}

		public @Nullable Entry get(UUID owner, UUID puppet) {
			Map<UUID, Entry> puppets = byOwner.get(owner);
			return puppets == null ? null : puppets.get(puppet);
		}

		/** The slots {@code owner}'s puppets take. */
		public int used(UUID owner) {
			int used = 0;
			for (Entry entry : of(owner)) used += cost(entry.grade());
			return used;
		}

		public void put(UUID owner, Entry entry) {
			Entry before = get(owner, entry.id());
			if (entry.equals(before)) return;
			byOwner.computeIfAbsent(owner, o -> new LinkedHashMap<>()).put(entry.id(), entry);
			setDirty();
		}

		/** {@code owner}'s puppet {@code puppet} out of the world on an errand (PuppetCouriers), or back. */
		public void markAway(UUID owner, UUID puppet, boolean away) {
			Entry e = get(owner, puppet);
			if (e == null || e.away() == away) return;
			put(owner, new Entry(e.id(), e.name(), e.blueprint(), e.grade(), e.rank(), e.order(), e.health(), e.maxHealth(), e.dimension(), e.pos(),
					e.inert(), away));
		}

		public void remove(UUID owner, UUID puppet) {
			if (inRanks.remove(puppet) != null) setDirty();
			Map<UUID, Entry> puppets = byOwner.get(owner);
			if (puppets == null || puppets.remove(puppet) == null) return;
			if (puppets.isEmpty()) byOwner.remove(owner);
			setDirty();
		}
	}

	/**
	 * A design's wear (puppet phase 3, the Overseer Core): how often each cell's line was cut in fights, how often each cell ran hot when a
	 * puppet of it overheated, and how many times that was in all.
	 */
	public static final class Wear {
		public final int[] cuts, heat;
		public int cutsTotal, overheats;

		Wear(int cells) {
			cuts = new int[cells];
			heat = new int[cells];
		}

		private Wear(int[] cuts, int[] heat, int cutsTotal, int overheats) {
			this.cuts = cuts;
			this.heat = heat;
			this.cutsTotal = cutsTotal;
			this.overheats = overheats;
		}

		CompoundTag save(long design) {
			CompoundTag tag = new CompoundTag();
			tag.putLong("Design", design);
			tag.putIntArray("Cuts", cuts);
			tag.putIntArray("Heat", heat);
			tag.putInt("CutsTotal", cutsTotal);
			tag.putInt("Overheats", overheats);
			return tag;
		}

		/** Kept on a diagram in an Overseer Core (CircuitDiagramItem#wear), or in the ledger. */
		public static Wear load(CompoundTag tag) {
			int[] cuts = tag.getIntArray("Cuts"), heat = tag.getIntArray("Heat");
			if (heat.length != cuts.length) heat = new int[cuts.length];
			return new Wear(cuts, heat, tag.getInt("CutsTotal"), tag.getInt("Overheats"));
		}

		/** As kept on a diagram (its design aside). */
		public CompoundTag tag() {
			return save(0);
		}

		/** The most a single cell was cut. */
		public int mostCuts() {
			int most = 0;
			for (int c : cuts) most = Math.max(most, c);
			return most;
		}
	}

	/**
	 * Its hooks: the messenger bees' flights every second; a board or a seizing let go of as its player leaves; and a left-click on a Puppet
	 * Workbench with a frame or core on it takes them back instead of breaking the bench (PuppetWorkbenchBlockEntity#takeBack: on both
	 * sides, so the client sends it and neither starts to break the bench, a creative player's instant break too). An empty bench mines as
	 * any other.
	 */
	public static void register() {
		net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (player.isSpectator() || !(level.getBlockEntity(pos) instanceof PuppetWorkbenchBlockEntity bench)) return net.minecraft.world.InteractionResult.PASS;
			return bench.takeBack(player) ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.PASS;
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) PuppetCouriers.tick(server);
		});
		PuppetPlayers.register(); // players' souls held in puppets (phase 2d): their keeping each tick
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			server.execute(() -> {
				PuppetBoards.forget(id);
				CommandSealItem.forget(id);
				PuppetBodies.forget(id);
			});
		});
		// A world closing never runs the tasks its leaving players queue (a single-player world, quit): nothing carries over to the next
		// (bug sweep 10, 2026-11-18).
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PuppetBoards.clear();
			CommandSealItem.clear();
			PuppetBodies.clear();
		});
	}

	/** {@code player}'s slots in use, and how many they have: "3 of 4". */
	public static Component slotsLine(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.own(player);
		int used = Ledger.get(player.server).used(player.getUUID()), most = slots(c);
		return Component.translatable(ModLang.PUPPET_SLOTS, used, most).withStyle(used >= most ? ChatFormatting.GOLD : ChatFormatting.GRAY);
	}

	private Puppetry() {}
}
