package com.example.defyingtheheavens;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/**
 * A puppet's circuit board (Puppetry; the user's asks of 2026-11-17: "circuits ... actually freely drawn by players", "drawn using qi like
 * how the formation lines are drawn", "a layout system that decides the grade of circuits ... pretty in depth"). A square of cells, odd-sized
 * by the frame's grade ({@link #sizeFor}: 9 x 9 at Mortal to 17 x 17 at Immortal), its demon core's socket at the centre with an outlet on
 * each side, the blueprint's nodes (PuppetBlueprint#nodes) and joints, and a frame's flaws (random blocked cells: 3 in a Normal frame, 1 in
 * a Fine, none in a Perfect). On the rest its puppeteer paints lines of qi with the Inscription Brush, a cell a stroke, as seals are painted
 * for formations; what they paint decides the puppet, and nothing else does: no dice.
 * <p>
 * <b>Conduction.</b> Painted cells that touch side to side join (a diagonal stroke also joins its diagonal neighbours that are diagonal
 * strokes); everything one outlet feeds is one <i>channel</i>. A node (a part) is fed through its four sides, and passes the qi on to
 * whatever else touches it (the user's ask, 2026-11-17: the head fed straight through the mouth); a node nothing reaches is dead. A bridge
 * carries two lines across each other without joining them.
 * <p>
 * <b>The core's qi and the veins</b> (the deeper circuits, phase A: the user's "Yes, build Phase A", 2026-11-17). The core gives
 * {@link #BUDGET} qi by the frame's grade, more by its own stage (Puppetry#budget) and {@link #RING_QI} more with the resonance ring closed.
 * Each side of a part that a line reaches it through is a <i>vein</i> (another part beside it feeds it too, the one of the two nearer the
 * core the other). Every vein draws its part's load ({@link #load}: a limb, the head and the heart 1, the art's node 2, and on a frame with no
 * art, a Mortal one, the mouth or stinger nothing) and makes the part stronger (Puppetry's steps). A drawing that wants more than its core
 * gives can't be sealed; a puppet's already made feeds every part's nearest vein, then the rest nearest first while the qi lasts.
 * <p>
 * <b>Phases</b> (the deeper circuits, phase B). A phase seal ({@link Stroke#SEAL_METAL} and its four fellows) painted on an outlet tunes
 * the line-channel it stands in (lines joined, never through a part) to its {@link Phase}; a channel holding seals of two phases clashes
 * and has none. A vein takes its channel's phase, a part every phase its fed veins bring (one fed through another part, that part's).
 * Channels of two phases close to each other (where Clarity reads a leak) don't leak if the one feeds the other, and give the core
 * {@link #FEEDING_QI} more a pair; if the one overcomes the other they leak twice over and heat. A seal of the core's own phase gives
 * {@link #RESONANCE_QI} more qi, one of the phase that overcomes the core's as much less.
 * <p>
 * <b>Arrays</b> (the deeper circuits, phase C). A small shape of strokes drawn touching a part ({@link Array}: a relay knot by the head,
 * a block of thick lines by an arm, a slant of diagonals by a leg or wing, a gathering knot by the art, a ward knot by the heart) is
 * recognised as the board stands and gives the puppet a technique, while its part lives, its strokes are reached by the core's qi and
 * the {@link Array#qi} it draws lasts (after every vein). The same array drawn as its mirror on the other half of a pair is a Twin Seal:
 * its cooldown halved, for {@link #TWIN_QI} more. Whoever can paint the strokes can draw them (the user's pick).
 * <p>
 * <b>The linked army</b> (puppet phase 3). A link knot ({@link Stroke#LINK}) drawn beside the heart of command, a fed vein of the heart
 * coming in through it, links the puppet ({@link Scores#linked}) to its puppeteer's other linked puppets near it: they strengthen one
 * another, pass its puppeteer's orders on and close formations round a foe (PuppetEntity, PuppetCommands, PuppetFormations).
 * <p>
 * <b>The five scores</b> ({@link #score}), each 0 to 100:
 * <ul>
 *   <li><b>Flow</b>: how directly each part is fed: the best route possible on this board over its nearest vein's, a bend counting as a
 *   cell; each relay knot on the way takes {@link #RELAY_SAVING} cells off; weighed by the parts' loads, a dead part nothing.</li>
 *   <li><b>Stability</b>: each fed vein carries its part's load along its own route; a plain line holds 2, a thick line or the ring 4, a
 *   part passing the qi on {@link #NODE_HOLD}; what's over is heat, which cooling vents on the board's edge draw off nearby;
 *   {@link #HEAT_COST} a point of heat left.</li>
 *   <li><b>Harmony</b>: the two of a pair (two arms, two legs, two wings) fed alike: the same veins, the same length and bends, and the one
 *   route the other's mirror.</li>
 *   <li><b>Clarity</b>: different channels close to each other leak (touching at a corner, or one empty cell apart in a straight line),
 *   dead-end stubs waste, and lines fed by nothing are dead weight.</li>
 *   <li><b>Resilience</b>: the share of the load whose part still has a way to the core whichever single cell of line is cut (two veins on
 *   separate routes; cells by a ward knot can't be cut).</li>
 * </ul>
 * The grade ({@link Grade}) is their average, and at most one grade above what the weakest of them alone would be. Pure logic: no game
 * classes, so it can be weighed outside the game; saving and the screen are elsewhere (Puppetry, the workbench).
 */
public final class CircuitBoard {
	/** What a painted cell holds, the Puppetry rank that may paint it (CraftMastery.Rank's order), and the brush's wear for it. */
	public enum Stroke {
		NONE(0, 0),
		/** A plain line of qi: carries a load of 2. */
		LINE(0, 1),
		/** A line painted twice over: carries 4 (Master). */
		THICK(2, 2),
		/** Two lines crossing without joining: the one up and down, the other across (Master). */
		BRIDGE(2, 2),
		/** A relay knot: takes {@link #RELAY_SAVING} cells off a route through it (Adept; at most {@link #MAX_RELAYS} a board). */
		RELAY(1, 2),
		/** A cooling vent, on the board's edge: draws {@link #VENT_COOLING} heat off the lines near it (Grandmaster; with a piece of metal). */
		VENT(3, 2),
		/** A gathering knot against the art's node: a vein of it that gathers a second charge of the art (Grandmaster; with a spirit stone). */
		GATHER(3, 2),
		/** A ward knot: the cells round it can't be cut (Saint; at most {@link #MAX_WARDS} a board). */
		WARD(4, 3),
		/** A cell of the resonance ring, the square two cells out from the core: closed, the core gives {@link #RING_QI} more (Saint). */
		RING(4, 1),
		/** A diagonal stroke: a line that also joins the diagonal strokes at its corners (Master). */
		DIAGONAL(2, 1),
		/**
		 * A phase seal on an outlet (phase B): a line that tunes its channel to its phase (Master; with its element's token). Kept after
		 * the others, so a drawing's saved strokes keep their order.
		 */
		SEAL_METAL(2, 2), SEAL_WOOD(2, 2), SEAL_WATER(2, 2), SEAL_FIRE(2, 2), SEAL_EARTH(2, 2),
		/**
		 * A link knot against the heart of command (puppet phase 3, the linked army): a line that ties its puppet to its puppeteer's other
		 * linked puppets while a fed vein of the heart comes in through it (Scores#linked; Grandmaster). Kept last, so a drawing's saved
		 * strokes keep their order.
		 */
		LINK(3, 2);

		public final int rank, wear;

		Stroke(int rank, int wear) {
			this.rank = rank;
			this.wear = wear;
		}

		/** Whether qi runs along it. */
		public boolean conducts() {
			return this != NONE && this != VENT && this != WARD;
		}

		/** A seal's phase; null for any other stroke. */
		public Phase phase() {
			return switch (this) {
				case SEAL_METAL -> Phase.METAL;
				case SEAL_WOOD -> Phase.WOOD;
				case SEAL_WATER -> Phase.WATER;
				case SEAL_FIRE -> Phase.FIRE;
				case SEAL_EARTH -> Phase.EARTH;
				default -> null;
			};
		}

		/** The seal of {@code phase}. */
		public static Stroke seal(Phase phase) {
			return switch (phase) {
				case METAL -> SEAL_METAL;
				case WOOD -> SEAL_WOOD;
				case WATER -> SEAL_WATER;
				case FIRE -> SEAL_FIRE;
				case EARTH -> SEAL_EARTH;
			};
		}
	}

	/** The five phases a channel can be tuned to (phase B): each feeds one and overcomes another. */
	public enum Phase {
		METAL, WOOD, WATER, FIRE, EARTH;

		/** The phase this one gives birth to: wood feeds fire, fire makes earth, earth bears metal, metal carries water, water feeds wood. */
		public Phase feeds() {
			return switch (this) {
				case WOOD -> FIRE;
				case FIRE -> EARTH;
				case EARTH -> METAL;
				case METAL -> WATER;
				case WATER -> WOOD;
			};
		}

		/** The phase this one overcomes: metal fells wood, wood breaks earth, earth dams water, water quenches fire, fire melts metal. */
		public Phase overcomes() {
			return switch (this) {
				case METAL -> WOOD;
				case WOOD -> EARTH;
				case EARTH -> WATER;
				case WATER -> FIRE;
				case FIRE -> METAL;
			};
		}

		/** The phase that overcomes this one. */
		public Phase overcomer() {
			for (Phase p : values()) if (p.overcomes() == this) return p;
			return this;
		}

		public int bit() {
			return 1 << ordinal();
		}

		/** The phases in {@code mask} (bits by {@link #bit}). */
		public static List<Phase> of(int mask) {
			List<Phase> out = new ArrayList<>();
			for (Phase p : values()) if ((mask & p.bit()) != 0) out.add(p);
			return out;
		}
	}

	/** The grade a board's scores give: Crude below 40, Normal to 64, Fine to 84, Perfect from 85. */
	public enum Grade {
		CRUDE(0), NORMAL(40), FINE(65), PERFECT(85);

		public final int from;

		Grade(int from) {
			this.from = from;
		}

		public static Grade of(double score) {
			Grade reached = CRUDE;
			for (Grade grade : values()) if (score >= grade.from) reached = grade;
			return reached;
		}
	}

	// The parts a Circuit Path's raises name: the head, the heart, the art's node, the left half of a pair (0: the arms or wings, 1: the legs).
	private static final int HEAD = 0, HEART = 1, ART = 2, PAIR0 = 3, PAIR1 = 4;

	/**
	 * A Circuit Path's build (the deeper circuits, phase A): which parts the core's qi feeds harder, in order, each as far as the qi goes,
	 * then the rest as a balanced path would. The humanoid's are Balanced, Brawler (arms), Scout (legs and head), Sentinel (head and
	 * heart) and, on a frame with an art, Caster (its mouth); the bee's Balanced, Courier (wings and legs), Sentinel and Caster (its stinger).
	 * Balanced raises each part a vein at a time, the art's node (twice as dear) last of each round.
	 */
	public enum Build {
		BALANCED("balanced", HEART, 2, HEAD, 2, PAIR0, 2, PAIR1, 2, ART, 2, HEART, 3, HEAD, 3, PAIR0, 3, PAIR1, 3, ART, 3, HEART, 4, HEAD, 4,
				PAIR0, 4, PAIR1, 4, ART, 4),
		BRAWLER("brawler", PAIR0, 2, PAIR0, 3, PAIR0, 4, HEART, 2, HEAD, 2, PAIR1, 2, ART, 2),
		SCOUT("scout", PAIR1, 2, PAIR1, 3, PAIR1, 4, HEAD, 2, HEAD, 3, HEART, 2, PAIR0, 2, ART, 2),
		COURIER("courier", PAIR0, 2, PAIR0, 3, PAIR0, 4, PAIR1, 2, PAIR1, 3, HEART, 2, HEAD, 2, ART, 2),
		SENTINEL("sentinel", HEAD, 2, HEART, 2, HEAD, 3, HEART, 3, HEAD, 4, HEART, 4, PAIR0, 2, PAIR1, 2, ART, 2),
		CASTER("caster", ART, 2, ART, 3, ART, 4, HEAD, 2, HEAD, 3, HEART, 2, PAIR0, 2, PAIR1, 2);

		private final String id;
		/** Its raises: a part and the veins it is fed to, pairs of numbers in order. */
		private final int[] raises;

		Build(String id, int... raises) {
			this.id = id;
			this.raises = raises;
		}

		public String getId() { return id; }

		/** Whether a {@code blueprint} frame offers it ({@code art}: the frame has an art, Spirit and up). */
		public boolean offered(PuppetBlueprint blueprint, boolean art) {
			return switch (this) {
				case BRAWLER, SCOUT -> blueprint == PuppetBlueprint.HUMANOID;
				case COURIER -> blueprint == PuppetBlueprint.BEE;
				case CASTER -> art;
				default -> true;
			};
		}

		/** The builds a {@code blueprint} frame offers, in the order the board's Path button runs through them. */
		public static List<Build> of(PuppetBlueprint blueprint, boolean art) {
			List<Build> builds = new ArrayList<>();
			for (Build build : values()) if (build.offered(blueprint, art)) builds.add(build);
			return builds;
		}

		/** The arrays its Circuit Path draws, each from the rank that paints its strokes (Array#rank; its Twin Seal at Saint). */
		public Array[] arrays() {
			return switch (this) {
				case BALANCED -> new Array[] {Array.IRON_LOCK};
				case BRAWLER -> new Array[] {Array.GUARD_COIL};
				case SCOUT -> new Array[] {Array.EYE, Array.GALE_STAIR};
				case COURIER -> new Array[] {Array.GALE_STAIR};
				case SENTINEL -> new Array[] {Array.EYE, Array.IRON_LOCK};
				case CASTER -> new Array[] {Array.GATHERING_SPIRAL};
			};
		}
	}

	/**
	 * An array (the deeper circuits, phase C): a small shape of strokes drawn touching a part, that gives the puppet a technique. Its knot
	 * or block touches the part by one side; a knot's other sides that are free cells all hold its strokes (lines round a relay or a
	 * gathering knot, bridges round a ward), two at least for lines, one for bridges (a side on the frame's matter or past the edge is
	 * left as it is).
	 */
	public enum Array {
		/** A relay knot touching the head, lines on its other sides: sees the unseen, takes up a foe at once, farther sight. */
		EYE("eye", 2, 1),
		/** A two-by-two block of thick lines touching an arm: turns a blow aside every so often. */
		GUARD_COIL("guard_coil", 2, 2),
		/** Three diagonal strokes in a slant, one touching a leg or a wing: a dash at its foe every so often. */
		GALE_STAIR("gale_stair", 2, 2),
		/** A gathering knot touching the art's node, lines on its other sides: the art's charges come back twice as fast. */
		GATHERING_SPIRAL("gathering_spiral", 2, 3),
		/** A ward knot touching the heart, bridges on its other sides: no seal can seize it, a freezing holds it half as long. */
		IRON_LOCK("iron_lock", 3, 4);

		private final String id;
		/** The qi it draws. */
		public final int qi;
		/** The rank (CraftMastery.Rank's order) whose Circuit Path draws it first: the rank that paints its strokes. */
		public final int rank;

		Array(String id, int qi, int rank) {
			this.id = id;
			this.qi = qi;
			this.rank = rank;
		}

		public String getId() { return id; }

		/** Whether it is drawn by {@code node}: the head's, an arm's, a leg's or a wing's, the art's, the heart's. */
		public boolean fits(PuppetBlueprint.Node node) {
			return switch (this) {
				case EYE -> node.kind() == PuppetBlueprint.NodeKind.SENSE;
				case GUARD_COIL -> node.kind() == PuppetBlueprint.NodeKind.LIMB && node.id().endsWith("arm");
				case GALE_STAIR -> node.kind() == PuppetBlueprint.NodeKind.LIMB && (node.id().endsWith("leg") || node.id().endsWith("wing"));
				case GATHERING_SPIRAL -> node.kind() == PuppetBlueprint.NodeKind.ART;
				case IRON_LOCK -> node.kind() == PuppetBlueprint.NodeKind.SEAL;
			};
		}

		/** Whether it turns with a cooldown (a Twin Seal halves it): the coil and the stair. */
		public boolean twinnable() {
			return this == GUARD_COIL || this == GALE_STAIR;
		}
	}

	/**
	 * An array drawn on the board: its kind, its part, its cells (in board order), whether it is one of a Twin Seal (its mirror drawn on
	 * the other half of the pair: the right half's draws {@link #TWIN_QI} more for the two), and whether it works ({@code fed}: its part
	 * alive, its strokes reached by the core's qi, its qi drawn; as {@link #arrays} finds them, before weighing, false).
	 */
	public record Formed(Array array, int node, int[] cells, boolean twin, boolean fed) {
		Formed fedAs(boolean works) {
			return new Formed(array, node, cells, twin, works);
		}
	}

	/**
	 * A board's scores, its grade, and what it feeds: which parts are dead (by index), whether the heart of command is fed, the art's
	 * charges; each part's veins the core feeds ({@code veins}) and has drawn ({@code drawn}: more only where its qi runs short), whether
	 * one cut can't cut it off ({@code sure}); the qi its fed veins draw, what all its veins would, and what its core gives (the ring's
	 * and the phases' shares in); each part's phases (bits, Phase#bit), and what the phases add to the core's qi or take from it; the
	 * arrays drawn (phase C), each with whether it works (their qi in {@code draw} and {@code wanted}).
	 */
	public record Scores(int flow, int stability, int harmony, int clarity, int resilience, Grade grade, boolean[] dead, boolean sealFed,
			int artCharges, int[] veins, int[] drawn, boolean[] sure, int draw, int wanted, int budget, int[] phases, int phaseQi,
			List<Formed> arrays, boolean linked) {
		public double average() {
			return (flow + stability + harmony + clarity + resilience) / 5.0;
		}

		/** Its veins want more qi than its core gives: it can't be sealed so. */
		public boolean over() {
			return wanted > budget;
		}

		/** Whether an array of {@code kind} works on it. */
		public boolean has(Array kind) {
			for (Formed f : arrays) if (f.array() == kind && f.fed()) return true;
			return false;
		}

		/** Whether {@code kind} works as a Twin Seal: both halves of its pair drawn as each other's mirror, both working. */
		public boolean twin(Array kind) {
			int working = 0;
			for (Formed f : arrays) if (f.array() == kind && f.twin() && f.fed()) working++;
			return working >= 2;
		}

		/** The array drawn by node {@code n}, or null. */
		public Formed arrayOf(int n) {
			for (Formed f : arrays) if (f.node() == n) return f;
			return null;
		}
	}

	public static final int MAX_RELAYS = 2, MAX_WARDS = 2;
	/** The load a node passing qi on to another holds (a thick line's): it is a solid part, not a stroke to thicken. */
	public static final int NODE_HOLD = 4;
	public static final int RELAY_SAVING = 2, VENT_COOLING = 2, VENT_REACH = 2;
	public static final double HEAT_COST = 15;
	/** The qi a frame's core gives by the frame's grade (Mortal to Immortal), before its core's own stage and the ring. */
	public static final int[] BUDGET = {14, 18, 22, 24, 26};
	/** The qi the closed resonance ring adds. */
	public static final int RING_QI = 3;
	/** Phases (phase B): what a seal of the core's own phase adds (as much taken for the phase overcoming it), and a feeding pair close by. */
	public static final int RESONANCE_QI = 2, FEEDING_QI = 1;
	/** What a Twin Seal draws besides its two arrays (phase C). */
	public static final int TWIN_QI = 1;
	/** Clarity's costs: channels touching at a corner, one empty cell apart, a dead-end stub, a cell of dead weight. */
	public static final double CORNER_LEAK = 5, GAP_LEAK = 1.5, STUB = 4, DEAD_WEIGHT = 2;
	/**
	 * Harmony's costs: a vein's difference between a pair's halves, a cell of length between their routes, a bend, the routes wholly unlike
	 * their mirror, one fed and not the other.
	 */
	public static final double PAIR_VEIN = 25, PAIR_LENGTH = 8, PAIR_BEND = 6, PAIR_MIRROR = 30, PAIR_HALF = 50, PAIR_NONE = 25;
	/** The most veins each rank's Circuit Path feeds a part (Apprentice to Saint), and the vents it lays at most. */
	private static final int[] PATH_VEINS = {2, 3, 4, 4, 4};
	private static final int PATH_VENTS = 4;

	/** The size of a frame's board by its grade (0 Mortal to 4 Immortal): 9, 11, 13, 15, 17. */
	public static int sizeFor(int grade) {
		return 9 + 2 * Math.max(0, Math.min(4, grade));
	}

	/** Flaws a frame of a forged quality has (0 Normal, 1 Fine, 2 Perfect): 3, 1, none. */
	public static int flawsFor(int quality) {
		return quality <= 0 ? 3 : quality == 1 ? 1 : 0;
	}

	// The four ways out of a cell, then the four diagonals: x and y steps.
	private static final int[] DX = {0, 1, 0, -1, 1, 1, -1, -1};
	private static final int[] DY = {-1, 0, 1, 0, -1, 1, 1, -1};
	private static final int UNREACHED = Integer.MAX_VALUE;

	public final PuppetBlueprint blueprint;
	public final int size;
	private final int centre;
	private final List<PuppetBlueprint.Node> nodes;
	/** The board's own: 0 free, 1 joint, 2 flaw, 3 the socket, 4 + n node n. */
	private final int[] fixed;
	private final Stroke[] cells;
	/**
	 * A body's own meridians (Puppetry phase 2b, PuppetBodies): cells whose line is the body's, never wiped nor painted over; empty, a break,
	 * mended with a plain line. Null for a frame of matter.
	 */
	private boolean[] meridians;

	/** A blank board for {@code blueprint} at frame grade {@code grade}, with {@code flaws} flaws placed by {@code seed}. */
	public CircuitBoard(PuppetBlueprint blueprint, int grade, int flaws, long seed) {
		this.blueprint = blueprint;
		this.size = sizeFor(grade);
		this.centre = size / 2;
		this.nodes = blueprint.nodes(size);
		this.fixed = new int[size * size];
		this.cells = new Stroke[size * size];
		Arrays.fill(cells, Stroke.NONE);
		fixed[at(centre, centre)] = 3;
		for (int n = 0; n < nodes.size(); n++) fixed[at(nodes.get(n).x(), nodes.get(n).y())] = 4 + n;
		for (int[] joint : blueprint.blocked(size)) fixed[at(joint[0], joint[1])] = 1;
		placeFlaws(flaws, seed);
	}

	/** A copy, its strokes and all. */
	public CircuitBoard copy() {
		CircuitBoard copy = new CircuitBoard(blueprint, gradeIndex(), 0, 0);
		System.arraycopy(fixed, 0, copy.fixed, 0, fixed.length);
		System.arraycopy(cells, 0, copy.cells, 0, cells.length);
		if (meridians != null) copy.meridians = meridians.clone();
		return copy;
	}

	/** A body's meridians (phase 2b: PuppetBodies#meridians), a cell each; null: a frame of matter. */
	public void setMeridians(boolean @org.jetbrains.annotations.Nullable [] meridians) {
		this.meridians = meridians == null || meridians.length != cells.length ? null : meridians.clone();
	}

	/** Whether (x, y) is one of a body's meridians (painted, or broken). */
	public boolean isMeridian(int x, int y) {
		return meridians != null && inside(x, y) && meridians[at(x, y)];
	}

	/** Whether the board is a body's (its meridians laid on it). */
	public boolean isBody() {
		return meridians != null;
	}

	/** The frame's grade the board is of (0 Mortal to 4 Immortal). */
	public int gradeIndex() {
		return (size - 9) / 2;
	}

	/** Whether the frame has an art (Spirit and up; a Mortal frame's mouth or stinger only passes the qi on). */
	public boolean hasArt() {
		return gradeIndex() >= 1;
	}

	/** The qi a vein of node {@code n} draws: its kind's load, nothing for the art's node of a frame with no art. */
	public int load(int n) {
		PuppetBlueprint.NodeKind kind = nodes.get(n).kind();
		return kind == PuppetBlueprint.NodeKind.ART && !hasArt() ? 0 : kind.load;
	}

	/**
	 * Flaws on free cells chosen by {@code seed}, never an outlet nor a cell beside a node or two from the core, and never one that would
	 * leave a node with no way to the core at all (tried again elsewhere).
	 */
	private void placeFlaws(int count, long seed) {
		Random random = new Random(seed);
		int placed = 0;
		for (int tries = 0; placed < count && tries < 500; tries++) {
			int x = random.nextInt(size), y = random.nextInt(size);
			int i = at(x, y);
			if (fixed[i] != 0 || isOutlet(x, y) || Math.max(Math.abs(x - centre), Math.abs(y - centre)) <= 2 || besideNode(x, y)) continue;
			fixed[i] = 2;
			if (allReachable()) placed++;
			else fixed[i] = 0;
		}
	}

	private boolean besideNode(int x, int y) {
		for (int d = 0; d < 4; d++) {
			int nx = x + DX[d], ny = y + DY[d];
			if (inside(nx, ny) && fixed[at(nx, ny)] >= 4) return true;
		}
		return false;
	}

	/** Every node could still be reached from the core if every free cell were a line. */
	private boolean allReachable() {
		Stroke[] full = fullBoard();
		for (int n = 0; n < nodes.size(); n++) if (route(full, n) == null) return false;
		return true;
	}

	// --- Cells ---

	public int at(int x, int y) {
		return y * size + x;
	}

	public boolean inside(int x, int y) {
		return x >= 0 && y >= 0 && x < size && y < size;
	}

	public Stroke stroke(int x, int y) {
		return cells[at(x, y)];
	}

	public List<PuppetBlueprint.Node> nodes() {
		return nodes;
	}

	/** The node on (x, y), or -1. */
	public int nodeAt(int x, int y) {
		int f = fixed[at(x, y)];
		return f >= 4 ? f - 4 : -1;
	}

	public boolean isJoint(int x, int y) { return fixed[at(x, y)] == 1; }
	public boolean isFlaw(int x, int y) { return fixed[at(x, y)] == 2; }
	public boolean isSocket(int x, int y) { return fixed[at(x, y)] == 3; }

	/** A free cell: no socket, node, joint or flaw on it, so a stroke may go there. */
	public boolean isFree(int x, int y) {
		return inside(x, y) && fixed[at(x, y)] == 0;
	}

	/** One of the four cells beside the socket, where a channel begins. */
	public boolean isOutlet(int x, int y) {
		return Math.abs(x - centre) + Math.abs(y - centre) == 1;
	}

	public boolean onEdge(int x, int y) {
		return x == 0 || y == 0 || x == size - 1 || y == size - 1;
	}

	/** On the resonance ring: the square of cells two out from the core. */
	public boolean onRing(int x, int y) {
		return Math.max(Math.abs(x - centre), Math.abs(y - centre)) == 2;
	}

	/** How many cells hold {@code stroke}. */
	public int count(Stroke stroke) {
		int n = 0;
		for (Stroke s : cells) if (s == stroke) n++;
		return n;
	}

	/**
	 * Why {@code stroke} can't go on (x, y), or null if it can: only on a free cell; a thick line over a line; a vent on the edge; a gathering
	 * knot against the art node; a bridge never on an outlet; a ring cell only on the ring; relays and wards only so many a board. A body's
	 * meridian is never wiped nor painted over; a broken one is mended with a plain line only.
	 */
	public String refusal(int x, int y, Stroke stroke) {
		if (!isFree(x, y)) return "occupied";
		Stroke here = stroke(x, y);
		if (isMeridian(x, y)) {
			if (here != Stroke.NONE) return here == stroke ? "same" : "meridian";
			return stroke == Stroke.LINE ? null : "meridian_break";
		}
		// A phase seal goes on an outlet only, over a line or another seal as well as an empty cell.
		if (stroke.phase() != null) {
			if (!isOutlet(x, y)) return "seal_outlet";
			if (here == stroke) return "same";
			return here == Stroke.NONE || here == Stroke.LINE || here == Stroke.THICK || here.phase() != null ? null : "occupied";
		}
		switch (stroke) {
			case NONE -> {
				return here == Stroke.NONE ? "empty" : null;
			}
			case THICK -> {
				if (here != Stroke.LINE && here != Stroke.NONE) return "occupied";
			}
			case VENT -> {
				if (!onEdge(x, y)) return "edge";
			}
			case GATHER -> {
				if (!besideKind(x, y, PuppetBlueprint.NodeKind.ART)) return "art";
			}
			case LINK -> {
				if (!besideKind(x, y, PuppetBlueprint.NodeKind.SEAL)) return "heart";
			}
			case BRIDGE -> {
				if (isOutlet(x, y)) return "outlet";
			}
			case RING -> {
				if (!onRing(x, y)) return "ring";
			}
			case RELAY -> {
				if (here != Stroke.RELAY && count(Stroke.RELAY) >= MAX_RELAYS) return "relays";
			}
			case WARD -> {
				if (here != Stroke.WARD && count(Stroke.WARD) >= MAX_WARDS) return "wards";
			}
			default -> {
			}
		}
		if (stroke != Stroke.THICK && here != Stroke.NONE && here != stroke) return "occupied";
		return here == stroke ? "same" : null;
	}

	private boolean besideKind(int x, int y, PuppetBlueprint.NodeKind kind) {
		for (int d = 0; d < 4; d++) {
			int nx = x + DX[d], ny = y + DY[d];
			if (!inside(nx, ny)) continue;
			int n = nodeAt(nx, ny);
			if (n >= 0 && nodes.get(n).kind() == kind) return true;
		}
		return false;
	}

	/** Paints {@code stroke} on (x, y) if it may go there (NONE wipes); whether it went. */
	public boolean paint(int x, int y, Stroke stroke) {
		if (refusal(x, y, stroke) != null) return false;
		cells[at(x, y)] = stroke;
		return true;
	}

	/** Sets (x, y) without the painting rules: loading a saved board, a diagram laid down. */
	public void set(int x, int y, Stroke stroke) {
		if (isFree(x, y)) cells[at(x, y)] = stroke;
	}

	public Stroke[] strokes() {
		return cells.clone();
	}

	// --- Routes ---

	/** The board as it would be with every free cell a plain line: what the best routes are measured on. */
	private Stroke[] fullBoard() {
		Stroke[] full = new Stroke[cells.length];
		for (int i = 0; i < full.length; i++) full[i] = fixed[i] == 0 ? Stroke.LINE : Stroke.NONE;
		return full;
	}

	/** A route to a node: its cells in order from the outlet, its cost (cells and bends), its length, its bends and the relays on it. */
	public record Route(int[] cells, int cost, int length, int bends, int relays) {
		/** The outlet it leaves the core by. */
		public int outlet() {
			return cells[0];
		}
	}

	/** One of a part's veins: the part, the cell beside it the qi comes in by (a line's, or another part's), and the route there. */
	public record Vein(int node, int side, Route route) {}

	/** A search from the outlets toward node {@code n}: each state's cost and how it was reached, and the cheapest way onto the node. */
	private record Search(int n, int[] cost, int[] from, int bestState, int best) {}

	/** The ways tried in order, and their mirror (left for right) for a node on the right half: a pair's routes tie alike. */
	private static final int[] WAYS = {0, 1, 2, 3, 4, 5, 6, 7}, MIRRORED_WAYS = {0, 3, 2, 1, 7, 6, 5, 4};

	/**
	 * The cheapest route from any outlet to node {@code n} over {@code board}'s conducting cells (a cell entered costs 1, a turn 1 more;
	 * stepping onto the node itself, through one of its four sides, 1, no turn counted), or null if nothing reaches it. Another node on the
	 * way passes the qi on (the user's ask, 2026-11-17: the head fed straight through the mouth, not round it), a cell of the route like any
	 * other, but never cut and holding more ({@link #NODE_HOLD}).
	 */
	private Route route(Stroke[] board, int n) {
		return route(board, n, -1);
	}

	private Route route(Stroke[] board, int n, int cut) {
		Search s = search(board, n, cut, 0b1111, null, true, false);
		return s.bestState() < 0 ? null : routeFrom(board, s, s.bestState(), s.best());
	}

	/**
	 * The search from the outlets in {@code outlets} (a bit a way: 1 up, 2 right, 4 down, 8 left) toward node {@code n}, never through
	 * {@code cut} or {@code barred}; other nodes pass the qi on if {@code throughNodes}. {@code whole}: on to every cell it reaches (each
	 * side's way in: the veins); else only as far as the cheapest way onto the node. A node on the right half is sought with every way
	 * mirrored, so where several routes cost the same the one taken is its left partner's mirror on a board drawn alike (Harmony reads them).
	 */
	private Search search(Stroke[] board, int n, int cut, int outlets, boolean[] barred, boolean throughNodes, boolean whole) {
		return search(board, n, cut, outlets, barred, throughNodes, whole, null);
	}

	/** {@link #search}, a cell entered costing {@code toll} more besides (null: nothing; a Circuit Path's way round busy lines). */
	private Search search(Stroke[] board, int n, int cut, int outlets, boolean[] barred, boolean throughNodes, boolean whole, int[] toll) {
		int states = size * size * 16;
		// A state: a cell, its pass (0: the cell, or a bridge's up-and-down line; 1: a bridge's across line) and the way it was entered.
		int[] cost = new int[states];
		int[] from = new int[states];
		Arrays.fill(cost, UNREACHED);
		Arrays.fill(from, -1);
		// Equal costs taken in the order they were found (a heap alone would break ties anyhow): a mirrored board gives mirrored routes.
		PriorityQueue<int[]> queue = new PriorityQueue<>((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[2], b[2]));
		int found = 0;
		PuppetBlueprint.Node target = nodes.get(n);
		int[] ways = target.x() > centre ? MIRRORED_WAYS : WAYS;
		for (int k = 0; k < 4; k++) {
			int d = ways[k];
			int x = centre + DX[d], y = centre + DY[d], i = at(x, y);
			if ((outlets & (1 << d)) == 0 || i == cut || (barred != null && barred[i]) || fixed[i] != 0 || !board[i].conducts()
					|| board[i] == Stroke.BRIDGE) continue;
			int s = state(i, 0, d);
			cost[s] = 1;
			queue.add(new int[] {1, s, found++});
		}
		int best = UNREACHED, bestState = -1;
		while (!queue.isEmpty()) {
			int[] top = queue.poll();
			int c = top[0], s = top[1];
			if (c > cost[s] || (!whole && c >= best)) continue;
			int i = s / 16, pass = (s / 8) % 2, dir = s % 8;
			int x = i % size, y = i / size;
			// Onto the node from here, through one of its sides?
			for (int d = 0; d < 4; d++) {
				if (!passAllows(board[i], pass, d)) continue;
				if (x + DX[d] == target.x() && y + DY[d] == target.y() && c + 1 < best) {
					best = c + 1;
					bestState = s;
				}
			}
			for (int k = 0; k < 8; k++) {
				if (k >= 4 && board[i] != Stroke.DIAGONAL) break;
				int d = ways[k];
				if (!passAllows(board[i], pass, d)) continue;
				int nx = x + DX[d], ny = y + DY[d];
				if (!inside(nx, ny)) continue;
				int j = at(nx, ny);
				// A painted cell, or another node passing the qi on (never the target: it is reached from beside it).
				boolean through = throughNodes && fixed[j] >= 4 && fixed[j] != 4 + n;
				if (j == cut || !(through || fixed[j] == 0 && board[j].conducts()) || (barred != null && barred[j])) continue;
				if (d >= 4 && board[j] != Stroke.DIAGONAL) continue;
				int nextPass = board[j] == Stroke.BRIDGE ? (d % 2 == 0 ? 0 : 1) : 0;
				int nc = c + 1 + (d == dir ? 0 : 1) + (toll == null ? 0 : toll[j]);
				int ns = state(j, nextPass, d);
				if (nc < cost[ns]) {
					cost[ns] = nc;
					from[ns] = s;
					queue.add(new int[] {nc, ns, found++});
				}
			}
		}
		return new Search(n, cost, from, bestState, best);
	}

	/** The route a search reached state {@code end} by, at {@code cost} in all. */
	private static Route routeFrom(Stroke[] board, Search s, int end, int cost) {
		List<Integer> path = new ArrayList<>();
		int bends = 0, relays = 0;
		for (int st = end; st >= 0; st = s.from()[st]) {
			int i = st / 16;
			path.add(0, i);
			if (board[i] == Stroke.RELAY) relays++;
			int prev = s.from()[st];
			if (prev >= 0 && prev % 8 != st % 8) bends++;
		}
		int[] route = path.stream().mapToInt(Integer::intValue).toArray();
		return new Route(route, cost, route.length + 1, bends, relays);
	}

	private static int state(int cell, int pass, int dir) {
		return cell * 16 + pass * 8 + dir;
	}

	/** Whether a cell's line in pass {@code pass} runs the way {@code d}: a bridge's first line up and down, its second across. */
	private static boolean passAllows(Stroke stroke, int pass, int d) {
		if (stroke != Stroke.BRIDGE) return true;
		return pass == 0 ? d == 0 || d == 2 : d == 1 || d == 3;
	}

	/** The sides tried in order (up, right, down, left), and their mirror for a node on the right half. */
	private static final int[] SIDES = {0, 1, 2, 3}, MIRRORED_SIDES = {0, 3, 2, 1};

	/**
	 * Node {@code n}'s way in through each of its four sides by a whole search (null where none comes). Where a side can be reached as
	 * cheaply more than one way, its qi takes the way that shares fewest cells with the part's cheaper veins' (a line drawn apart carries
	 * its own vein, not the trunk beside it).
	 */
	private Route[] sideRoutes(Stroke[] board, Search s) {
		PuppetBlueprint.Node node = nodes.get(s.n());
		boolean mirrored = node.x() > centre;
		int[] ways = mirrored ? MIRRORED_WAYS : WAYS;
		Route[] out = new Route[4];
		int[] best = new int[4];
		Arrays.fill(best, UNREACHED);
		for (int d = 0; d < 4; d++) {
			int sx = node.x() + DX[d], sy = node.y() + DY[d];
			if (!inside(sx, sy)) continue;
			int bestState = cheapestState(s, at(sx, sy), mirrored, board[at(sx, sy)], (d + 2) % 4);
			if (bestState >= 0) best[d] = s.cost()[bestState];
		}
		Set<Integer> used = new HashSet<>();
		Integer[] order = {0, 1, 2, 3};
		if (mirrored) order = new Integer[] {0, 3, 2, 1};
		Arrays.sort(order, Comparator.comparingInt(d -> best[d]));
		for (int d : order) {
			if (best[d] == UNREACHED) continue;
			int side = at(node.x() + DX[d], node.y() + DY[d]), toward = (d + 2) % 4;
			Route chosen = null;
			int shared = Integer.MAX_VALUE;
			for (int pass = 0; pass < 2; pass++) {
				for (int k = 0; k < 8; k++) {
					int st = state(side, pass, ways[k]);
					if (s.cost()[st] != best[d] || !passAllows(board[side], pass, toward)) continue;
					Route r = routeFrom(board, s, st, best[d] + 1);
					int overlap = 0;
					for (int i : r.cells()) if (used.contains(i)) overlap++;
					if (overlap < shared) {
						shared = overlap;
						chosen = r;
					}
				}
			}
			out[d] = chosen;
			for (int i : chosen.cells()) used.add(i);
		}
		return out;
	}

	/**
	 * The cheapest of a search's states at {@code cell} (that may step the way {@code toward}, -1 for any), the ways it was entered weighed
	 * in mirrored order for a right-half node ({@code mirrored}) so ties fall alike on both halves of a pair; -1 if none was reached.
	 */
	private static int cheapestState(Search s, int cell, boolean mirrored, Stroke stroke, int toward) {
		int[] ways = mirrored ? MIRRORED_WAYS : WAYS;
		int best = UNREACHED, bestState = -1;
		for (int pass = 0; pass < 2; pass++) {
			for (int k = 0; k < 8; k++) {
				int st = state(cell, pass, ways[k]);
				if (s.cost()[st] == UNREACHED || toward >= 0 && !passAllows(stroke, pass, toward)) continue;
				if (s.cost()[st] < best) {
					best = s.cost()[st];
					bestState = st;
				}
			}
		}
		return bestState;
	}

	/**
	 * Every part's veins on {@code board}, each part's cheapest first: a way in through each of its sides that one reaches, and through
	 * another part beside it only if that part is the nearer the core of the two (the nearer feeds the farther, never both ways).
	 */
	private List<List<Vein>> veins(Stroke[] board) {
		int count = nodes.size();
		Route[][] sides = new Route[count][];
		int[] nearest = new int[count];
		for (int n = 0; n < count; n++) {
			sides[n] = sideRoutes(board, search(board, n, -1, 0b1111, null, true, true));
			nearest[n] = UNREACHED;
			for (Route r : sides[n]) if (r != null) nearest[n] = Math.min(nearest[n], r.cost());
		}
		List<List<Vein>> out = new ArrayList<>();
		for (int n = 0; n < count; n++) {
			PuppetBlueprint.Node node = nodes.get(n);
			List<Vein> list = new ArrayList<>();
			for (int d : node.x() > centre ? MIRRORED_SIDES : SIDES) {
				Route r = sides[n][d];
				if (r == null) continue;
				int side = at(node.x() + DX[d], node.y() + DY[d]);
				int m = fixed[side] >= 4 ? fixed[side] - 4 : -1;
				if (m >= 0 && !(nearest[m] < nearest[n] || nearest[m] == nearest[n] && m < n)) continue;
				list.add(new Vein(n, side, r));
			}
			list.sort(Comparator.comparingInt(v -> v.route().cost()));
			out.add(list);
		}
		return out;
	}

	/**
	 * Which of {@code veins} the core feeds with {@code budget} qi: all, if they want no more; else every part's nearest, then the rest
	 * nearest the core first while the qi lasts.
	 */
	private boolean[][] feed(List<List<Vein>> veins, int budget) {
		boolean[][] fed = new boolean[veins.size()][];
		int draw = 0;
		List<int[]> extra = new ArrayList<>();
		for (int n = 0; n < veins.size(); n++) {
			fed[n] = new boolean[veins.get(n).size()];
			if (fed[n].length == 0) continue;
			fed[n][0] = true;
			draw += load(n);
			for (int k = 1; k < fed[n].length; k++) extra.add(new int[] {n, k, veins.get(n).get(k).route().cost()});
		}
		extra.sort(Comparator.comparingInt(e -> e[2]));
		for (int[] e : extra) {
			int load = load(e[0]);
			if (draw + load > budget) continue;
			fed[e[0]][e[1]] = true;
			draw += load;
		}
		return fed;
	}

	/** The qi all of {@code veins} would draw. */
	private int wanted(List<List<Vein>> veins) {
		int wanted = 0;
		for (int n = 0; n < veins.size(); n++) wanted += load(n) * veins.get(n).size();
		return wanted;
	}

	// --- The scores ---

	/** The board weighed with the qi its own frame's core gives at least (its stage and phase unknown): its five scores and its grade. */
	public Scores score() {
		return score(BUDGET[gradeIndex()], null);
	}

	/** {@link #score(int, Phase)} for a core of no phase. */
	public Scores score(int budget) {
		return score(budget, null);
	}

	/**
	 * The board weighed with its core giving {@code budget} qi (Puppetry#budget) in {@code core}'s phase (null: none of the five); the
	 * ring's share and the phases' are added here.
	 */
	public Scores score(int budget, Phase core) {
		Stroke[] full = fullBoard();
		int count = nodes.size();
		List<List<Vein>> veins = veins(cells);
		// The phases first: each line-channel's, the leaks between channels close by (a feeding pair none and qi besides, an overcoming one
		// twice over and heat), and the core's resonance with seals of its own phase.
		Phase[] phaseAt = cellPhases(lineChannels());
		Leaks leaks = leaks(phaseAt);
		int phaseQi = leaks.feedingPairs() * FEEDING_QI + resonance(core, phaseAt);
		int total = budget + (ringClosed() ? RING_QI : 0) + phaseQi;
		boolean[][] fed = feed(veins, total);
		boolean[] dead = new boolean[count];
		int[] loads = new int[count], fedVeins = new int[count], drawn = new int[count];
		int draw = 0;
		for (int n = 0; n < count; n++) {
			dead[n] = veins.get(n).isEmpty();
			loads[n] = load(n);
			drawn[n] = veins.get(n).size();
			for (boolean f : fed[n]) if (f) fedVeins[n]++;
			draw += loads[n] * fedVeins[n];
		}
		int totalLoad = Math.max(1, Arrays.stream(loads).sum());
		// The arrays (phase C): each works while its part lives and its strokes are reached, its qi drawn after every vein's while it lasts.
		List<Formed> arrays = new ArrayList<>();
		int arrayWant = 0;
		boolean[] reachedCells = fedCells();
		for (Formed f : arrays()) {
			boolean reached = !dead[f.node()] && reached(f, reachedCells);
			int qi = qiOf(f);
			if (reached) arrayWant += qi;
			boolean works = reached && draw + qi <= total;
			if (works) draw += qi;
			arrays.add(f.fedAs(works));
		}

		// Flow: the best route possible on this board over each part's nearest vein, relays taking cells off.
		double flowSum = 0;
		for (int n = 0; n < count; n++) {
			if (dead[n] || loads[n] == 0) continue;
			Route taken = veins.get(n).get(0).route();
			Route ideal = route(full, n);
			int best = ideal == null ? taken.cost() : ideal.cost();
			double cost = Math.max(best, taken.cost() - RELAY_SAVING * taken.relays());
			flowSum += loads[n] * Math.min(1.0, best / cost);
		}
		int flow = (int) Math.round(100 * flowSum / totalLoad);

		// Stability: what each cell carries against what it holds; the heat over, less what vents draw off; channels of overcoming phases
		// touching heat besides.
		int leftHeat = Arrays.stream(heatOf(veins, fed)).sum() + leaks.overcomeHeat();
		int stability = clamp(100 - HEAT_COST * leftHeat);

		// Harmony: each pair fed alike, with as many veins, the one route the other's mirror.
		double discord = 0;
		Set<Integer> pairs = new HashSet<>();
		for (PuppetBlueprint.Node node : nodes) if (node.pair() >= 0) pairs.add(node.pair());
		for (int pair : pairs) {
			int a = -1, b = -1;
			for (int n = 0; n < count; n++) {
				if (nodes.get(n).pair() != pair) continue;
				if (a < 0) a = n;
				else b = n;
			}
			if (dead[a] && dead[b]) discord += PAIR_NONE;
			else if (dead[a] || dead[b]) discord += PAIR_HALF;
			else {
				Route ra = veins.get(a).get(0).route(), rb = veins.get(b).get(0).route();
				discord += PAIR_VEIN * Math.abs(fedVeins[a] - fedVeins[b]);
				discord += PAIR_LENGTH * Math.abs(ra.length() - rb.length());
				discord += PAIR_BEND * Math.abs(ra.bends() - rb.bends());
				discord += PAIR_MIRROR * (1 - mirrorShare(ra, rb));
			}
		}
		int harmony = clamp(100 - discord);

		// Clarity: channels leaking into each other (by their phases), stubs, dead weight.
		int clarity = clamp(100 - leaks.murk());

		// Resilience: the load whose part no single cut can sever.
		int sureLoad = 0;
		boolean[] sure = new boolean[count];
		boolean[] warded = wardedCells();
		for (int n = 0; n < count; n++) {
			if (dead[n]) continue;
			boolean holds = true;
			for (int i : veins.get(n).get(0).route().cells()) {
				if (warded[i] || fixed[i] >= 4) continue; // a ward's cells, and a node passing the qi on, are never cut
				if (route(cells, n, i) == null) {
					holds = false;
					break;
				}
			}
			sure[n] = holds;
			if (holds) sureLoad += loads[n];
		}
		int resilience = (int) Math.round(100.0 * sureLoad / totalLoad);

		double average = (flow + stability + harmony + clarity + resilience) / 5.0;
		int weakest = Math.min(Math.min(flow, stability), Math.min(Math.min(harmony, clarity), resilience));
		Grade grade = Grade.of(average);
		Grade cap = Grade.values()[Math.min(Grade.values().length - 1, Grade.of(weakest).ordinal() + 1)];
		if (grade.ordinal() > cap.ordinal()) grade = cap;
		boolean sealFed = false, linked = false;
		int charges = 1;
		for (int n = 0; n < count; n++) {
			PuppetBlueprint.NodeKind kind = nodes.get(n).kind();
			if (kind == PuppetBlueprint.NodeKind.SEAL && !dead[n]) {
				sealFed = true;
				// A link knot that is a fed vein of the heart (phase 3): the puppet is one of its puppeteer's linked army.
				for (int k = 0; k < veins.get(n).size(); k++) if (fed[n][k] && cells[veins.get(n).get(k).side()] == Stroke.LINK) linked = true;
			}
			// A gathering knot that is a fed vein of the art: a second charge.
			if (kind == PuppetBlueprint.NodeKind.ART && hasArt()) {
				for (int k = 0; k < veins.get(n).size(); k++) if (fed[n][k] && cells[veins.get(n).get(k).side()] == Stroke.GATHER) charges = 2;
			}
		}
		return new Scores(flow, stability, harmony, clarity, resilience, grade, dead, sealFed, charges, fedVeins, drawn, sure, draw,
				wanted(veins) + arrayWant, total, partPhases(veins, fed, phaseAt), phaseQi, List.copyOf(arrays), linked);
	}

	// --- The arrays (phase C) ---

	/**
	 * Every array drawn on the board as it stands, a part's first found (its sides in order, mirrored on the right half), in the parts'
	 * order; a pair's two of a kind drawn as each other's mirror a Twin Seal. Whether each works is the weighing's ({@link #score}).
	 */
	public List<Formed> arrays() {
		List<Formed> found = new ArrayList<>();
		int count = nodes.size();
		int[][] first = new int[count][];
		Array[] kindOf = new Array[count];
		List<List<int[]>> all = new ArrayList<>();
		for (int n = 0; n < count; n++) {
			List<int[]> shapes = List.of();
			for (Array a : Array.values()) {
				if (!a.fits(nodes.get(n)) || a == Array.GATHERING_SPIRAL && !hasArt()) continue;
				shapes = shapes(a, n);
				if (!shapes.isEmpty()) {
					kindOf[n] = a;
					first[n] = shapes.get(0);
					break;
				}
			}
			all.add(shapes);
		}
		boolean[] twin = new boolean[count];
		for (int n = 0; n < count; n++) {
			PuppetBlueprint.Node node = nodes.get(n);
			if (kindOf[n] == null || !kindOf[n].twinnable() || node.pair() < 0 || node.x() > centre) continue;
			int m = partner(n);
			if (kindOf[m] != kindOf[n]) continue;
			int[] mirror = mirrored(first[n]);
			for (int[] shape : all.get(m)) {
				if (Arrays.equals(shape, mirror)) {
					first[m] = shape;
					twin[n] = twin[m] = true;
					break;
				}
			}
		}
		for (int n = 0; n < count; n++) if (kindOf[n] != null) found.add(new Formed(kindOf[n], n, first[n], twin[n], false));
		return found;
	}

	/** The qi {@code f} draws: its kind's, and on a Twin Seal's right half the seal's own besides. */
	private int qiOf(Formed f) {
		return f.array().qi + (f.twin() && nodes.get(f.node()).x() > centre ? TWIN_QI : 0);
	}

	/** Whether the core's qi reaches any of {@code f}'s strokes ({@code reached}: {@link #fedCells}). */
	private static boolean reached(Formed f, boolean[] reached) {
		for (int i : f.cells()) if (reached[i]) return true;
		return false;
	}

	/** The cells of {@code cells} mirrored left for right, in board order. */
	private int[] mirrored(int[] cells) {
		int[] out = new int[cells.length];
		for (int k = 0; k < cells.length; k++) out[k] = at(size - 1 - cells[k] % size, cells[k] / size);
		Arrays.sort(out);
		return out;
	}

	/** A stroke that is a line for an array's knot: a plain, thick or diagonal line, a phase seal, the ring. */
	private static boolean isLine(Stroke s) {
		return s == Stroke.LINE || s == Stroke.THICK || s == Stroke.DIAGONAL || s == Stroke.RING || s.phase() != null;
	}

	/**
	 * Every way {@code a} stands drawn by node {@code n} as the board is, its cells in board order: its sides in order (mirrored on the right
	 * half). Never in the core's square: the eight cells round the core are its own (they carry whatever it gives), no array's.
	 */
	private List<int[]> shapes(Array a, int n) {
		List<int[]> out = new ArrayList<>();
		for (int[] shape : shapesAnywhere(a, n)) {
			boolean core = false;
			for (int i : shape) core |= inCoreSquare(i % size, i / size);
			if (!core) out.add(shape);
		}
		return out;
	}

	private List<int[]> shapesAnywhere(Array a, int n) {
		PuppetBlueprint.Node node = nodes.get(n);
		boolean mirrored = node.x() > centre;
		List<int[]> out = new ArrayList<>();
		for (int d : mirrored ? MIRRORED_SIDES : SIDES) {
			int sx = node.x() + DX[d], sy = node.y() + DY[d];
			if (!isFree(sx, sy)) continue;
			int s = at(sx, sy);
			switch (a) {
				case EYE -> addKnot(out, s, d, Stroke.RELAY, false);
				case GATHERING_SPIRAL -> addKnot(out, s, d, Stroke.GATHER, false);
				case IRON_LOCK -> addKnot(out, s, d, Stroke.WARD, true);
				case GUARD_COIL -> {
					if (cells[s] != Stroke.THICK) continue;
					for (int[] block : blocks(sx, sy, mirrored)) {
						boolean whole = true;
						for (int i : block) whole &= fixed[i] == 0 && cells[i] == Stroke.THICK;
						if (whole) out.add(sorted(block));
					}
				}
				case GALE_STAIR -> {
					if (cells[s] != Stroke.DIAGONAL) continue;
					for (int[] slant : slants(sx, sy, mirrored)) {
						boolean whole = true;
						for (int i : slant) whole &= fixed[i] == 0 && cells[i] == Stroke.DIAGONAL && !besideNodeBut(i, n, s);
						if (whole) out.add(sorted(slant));
					}
				}
			}
		}
		return out;
	}

	/**
	 * A knot array at a part's side cell {@code s} (the way {@code d} out of the part): the knot {@code knot} on {@code s}, its other sides
	 * that are free cells all lines (all bridges round a ward: {@code bridges}), two lines at least or one bridge.
	 */
	private void addKnot(List<int[]> out, int s, int d, Stroke knot, boolean bridges) {
		if (cells[s] != knot) return;
		int sx = s % size, sy = s / size, back = (d + 2) % 4;
		List<Integer> shape = new ArrayList<>();
		shape.add(s);
		for (int e = 0; e < 4; e++) {
			if (e == back) continue;
			int tx = sx + DX[e], ty = sy + DY[e];
			if (!isFree(tx, ty) || inCoreSquare(tx, ty)) continue;
			Stroke there = cells[at(tx, ty)];
			if (bridges ? there != Stroke.BRIDGE : !isLine(there)) return;
			shape.add(at(tx, ty));
		}
		if (shape.size() - 1 < (bridges ? 1 : 2)) return;
		out.add(sorted(shape.stream().mapToInt(Integer::intValue).toArray()));
	}

	/** The two-by-two blocks holding cell (x, y) that keep off the part beside it, in order (mirrored on the right half). */
	private List<int[]> blocks(int x, int y, boolean mirrored) {
		List<int[]> out = new ArrayList<>();
		int[] dxs = mirrored ? new int[] {0, -1} : new int[] {-1, 0};
		for (int oy : new int[] {-1, 0}) {
			for (int ox : dxs) {
				int x0 = x + ox, y0 = y + oy;
				if (!inside(x0, y0) || !inside(x0 + 1, y0 + 1)) continue;
				int[] block = {at(x0, y0), at(x0 + 1, y0), at(x0, y0 + 1), at(x0 + 1, y0 + 1)};
				boolean onPart = false;
				for (int i : block) onPart |= fixed[i] >= 4;
				if (!onPart) out.add(block);
			}
		}
		return out;
	}

	/** The slants of three cells through (x, y), in order (mirrored on the right half): each diagonal, (x, y) at its end, middle or start. */
	private List<int[]> slants(int x, int y, boolean mirrored) {
		List<int[]> out = new ArrayList<>();
		int[][] dirs = mirrored ? new int[][] {{-1, 1}, {1, 1}} : new int[][] {{1, 1}, {-1, 1}};
		for (int[] dir : dirs) {
			for (int start = 0; start >= -2; start--) {
				int[] slant = new int[3];
				boolean ok = true;
				for (int k = 0; k < 3 && ok; k++) {
					int cx = x + (start + k) * dir[0], cy = y + (start + k) * dir[1];
					ok = inside(cx, cy);
					if (ok) slant[k] = at(cx, cy);
				}
				if (ok) out.add(slant);
			}
		}
		return out;
	}

	/** Whether cell {@code i} touches node {@code n} by a side, other than as its side cell {@code side}. */
	private boolean besideNodeBut(int i, int n, int side) {
		if (i == side) return false;
		PuppetBlueprint.Node node = nodes.get(n);
		return Math.abs(i % size - node.x()) + Math.abs(i / size - node.y()) == 1;
	}

	private static int[] sorted(int[] cells) {
		int[] out = cells.clone();
		Arrays.sort(out);
		return out;
	}

	/** What the Clarity scan finds: its murk, the distinct pairs of feeding phases close by, and the heat of overcoming ones touching. */
	private record Leaks(double murk, int feedingPairs, int overcomeHeat) {}

	/**
	 * Channels too close leak (touching at a corner, or one empty cell apart in a straight line; the core's square aside), stubs waste,
	 * unfed lines are dead weight. Between two channels of phases, a leak is none if they are of one phase or the one feeds the other (that
	 * pair noted once, for the core's qi), twice over if the one overcomes the other (and a point of heat).
	 */
	private Leaks leaks(Phase[] phaseAt) {
		int[] channel = channels();
		boolean[] fedChannel = fedChannels(channel);
		double murk = 0;
		Set<Integer> feeding = new HashSet<>();
		int[] heat = {0};
		for (int i = 0; i < cells.length; i++) {
			if (!cells[i].conducts() || cells[i] == Stroke.BRIDGE) continue;
			int x = i % size, y = i / size;
			if (!fedChannel[channel[i]]) {
				murk += DEAD_WEIGHT;
				continue;
			}
			if (inCoreSquare(x, y)) continue;
			// Corners (each pair once: the ones to the right of this cell) and straight gaps (right and down).
			for (int d : new int[] {4, 5}) {
				int nx = x + DX[d], ny = y + DY[d];
				if (!inside(nx, ny) || inCoreSquare(nx, ny)) continue;
				int j = at(nx, ny);
				if (cells[j].conducts() && cells[j] != Stroke.BRIDGE && channel[j] != channel[i] && fedChannel[channel[j]] && !joinedDiagonally(i, j)) {
					murk += CORNER_LEAK * contact(phaseAt[i], phaseAt[j], feeding, heat);
				}
			}
			for (int d : new int[] {1, 2}) {
				int mx = x + DX[d], my = y + DY[d], nx = x + 2 * DX[d], ny = y + 2 * DY[d];
				if (!inside(nx, ny) || inCoreSquare(nx, ny)) continue;
				int m = at(mx, my), j = at(nx, ny);
				if (cells[m] == Stroke.NONE && fixed[m] == 0 && cells[j].conducts() && cells[j] != Stroke.BRIDGE && channel[j] != channel[i]
						&& fedChannel[channel[j]]) murk += GAP_LEAK * contact(phaseAt[i], phaseAt[j], feeding, heat);
			}
			if (isStub(x, y)) murk += STUB;
		}
		return new Leaks(murk, feeding.size(), heat[0]);
	}

	/** A leak's weight between cells of phases {@code a} and {@code b}: 1 without both, 0 alike or feeding (pair noted), 2 overcoming (heat). */
	private static double contact(Phase a, Phase b, Set<Integer> feeding, int[] heat) {
		if (a == null || b == null) return 1;
		if (a == b) return 0;
		if (a.feeds() == b || b.feeds() == a) {
			feeding.add(Math.min(a.ordinal(), b.ordinal()) * 8 + Math.max(a.ordinal(), b.ordinal()));
			return 0;
		}
		heat[0]++;
		return 2;
	}

	/** The core's resonance: more qi for a seal of its own phase on the board, as much less for one of the phase overcoming it (clashing seals aside). */
	private int resonance(Phase core, Phase[] phaseAt) {
		if (core == null) return 0;
		boolean own = false, against = false;
		for (int i = 0; i < cells.length; i++) {
			if (cells[i].phase() == null || phaseAt[i] == null) continue;
			if (phaseAt[i] == core) own = true;
			else if (phaseAt[i].overcomes() == core) against = true;
		}
		return (own ? RESONANCE_QI : 0) - (against ? RESONANCE_QI : 0);
	}

	/** Each part's phases (bits): what its fed veins bring, a vein through another part that part's (the nearer parts weighed first). */
	private int[] partPhases(List<List<Vein>> veins, boolean[][] fed, Phase[] phaseAt) {
		int count = nodes.size();
		int[] mask = new int[count];
		Integer[] order = new Integer[count];
		for (int n = 0; n < count; n++) order[n] = n;
		Arrays.sort(order, Comparator.comparingInt(n -> veins.get(n).isEmpty() ? UNREACHED : veins.get(n).get(0).route().cost()));
		for (int n : order) {
			for (int k = 0; k < veins.get(n).size(); k++) {
				if (!fed[n][k]) continue;
				int side = veins.get(n).get(k).side();
				if (fixed[side] >= 4) mask[n] |= mask[fixed[side] - 4];
				else if (phaseAt[side] != null) mask[n] |= phaseAt[side].bit();
			}
		}
		return mask;
	}

	/** Each cell's phase: its line-channel's seals', where they are all of one phase (null for none, or where two clash). */
	private Phase[] cellPhases(int[] lineChannel) {
		int most = Arrays.stream(lineChannel).max().orElse(-1) + 1;
		int[] seals = new int[Math.max(1, most)];
		for (int i = 0; i < cells.length; i++) if (lineChannel[i] >= 0 && cells[i].phase() != null) seals[lineChannel[i]] |= cells[i].phase().bit();
		Phase[] out = new Phase[cells.length];
		for (int i = 0; i < cells.length; i++) {
			if (lineChannel[i] < 0) continue;
			int m = seals[lineChannel[i]];
			if (Integer.bitCount(m) == 1) out[i] = Phase.values()[Integer.numberOfTrailingZeros(m)];
		}
		return out;
	}

	/** Each cell's phase as the board stands (null for none): the screen tints the lines by it. */
	public Phase[] phases() {
		return cellPhases(lineChannels());
	}

	/** The cells of line-channels holding seals of two phases or more: they clash and have none (the screen marks them). */
	public boolean[] clashes() {
		int[] lineChannel = lineChannels();
		int most = Arrays.stream(lineChannel).max().orElse(-1) + 1;
		int[] seals = new int[Math.max(1, most)];
		for (int i = 0; i < cells.length; i++) if (lineChannel[i] >= 0 && cells[i].phase() != null) seals[lineChannel[i]] |= cells[i].phase().bit();
		boolean[] out = new boolean[cells.length];
		for (int i = 0; i < cells.length; i++) out[i] = lineChannel[i] >= 0 && Integer.bitCount(seals[lineChannel[i]]) >= 2;
		return out;
	}

	private static int clamp(double score) {
		return (int) Math.round(Math.max(0, Math.min(100, score)));
	}

	/** What each cell carries: every fed vein its part's load, along its own route. */
	private int[] carried(List<List<Vein>> veins, boolean[][] fed) {
		int[] carried = new int[size * size];
		for (int n = 0; n < veins.size(); n++) {
			int load = load(n);
			if (load == 0) continue;
			for (int k = 0; k < veins.get(n).size(); k++) {
				if (fed == null || fed[n][k]) for (int i : veins.get(n).get(k).route().cells()) carried[i] += load;
			}
		}
		return carried;
	}

	/** What a plain line holds on this frame, by its matter: a Mortal or Spirit frame's 2, an Earth or Heaven frame's 3, an Immortal one's 4. */
	public int lineHold() {
		return 2 + gradeIndex() / 2;
	}

	/** What cell {@code i} holds: a thick line, the ring and a part passing the qi on twice a plain line's. */
	private int hold(int i) {
		return fixed[i] >= 4 || cells[i] == Stroke.THICK || cells[i] == Stroke.RING ? 2 * lineHold() : lineHold();
	}

	/**
	 * The heat each cell is left with: what it carries over what it holds ({@link #hold}; the eight cells round the core are its own and
	 * carry whatever it gives), less what the vents draw off.
	 */
	private int[] heatOf(List<List<Vein>> veins, boolean[][] fed) {
		int[] carried = carried(veins, fed);
		int[] heat = new int[size * size];
		for (int i = 0; i < heat.length; i++) {
			if (inCoreSquare(i % size, i / size)) continue;
			heat[i] = Math.max(0, carried[i] - hold(i));
		}
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] != Stroke.VENT) continue;
			int cooling = VENT_COOLING, vx = i % size, vy = i / size;
			for (int reach = 0; reach <= VENT_REACH && cooling > 0; reach++) {
				for (int j = 0; j < heat.length && cooling > 0; j++) {
					if (heat[j] <= 0 || Math.max(Math.abs(j % size - vx), Math.abs(j / size - vy)) != reach) continue;
					int drawn = Math.min(cooling, heat[j]);
					heat[j] -= drawn;
					cooling -= drawn;
				}
			}
		}
		return heat;
	}

	/**
	 * The heat left in each cell as the board stands with its core giving {@code budget} in {@code core}'s phase (the screen's heat map,
	 * Grandmaster and above).
	 */
	public int[] heat(int budget, Phase core) {
		List<List<Vein>> veins = veins(cells);
		Phase[] phaseAt = cellPhases(lineChannels());
		int total = budget + (ringClosed() ? RING_QI : 0) + leaks(phaseAt).feedingPairs() * FEEDING_QI + resonance(core, phaseAt);
		return heatOf(veins, feed(veins, total));
	}

	/** The cells a single cut of would leave some node unfed (a ward's cells never): the screen's weak spots, a Saint's. */
	public boolean[] critical() {
		boolean[] critical = new boolean[cells.length];
		boolean[] warded = wardedCells();
		for (int n = 0; n < nodes.size(); n++) {
			Route route = route(cells, n);
			if (route == null) continue;
			for (int i : route.cells()) if (!warded[i] && fixed[i] < 4 && !critical[i] && route(cells, n, i) == null) critical[i] = true;
		}
		return critical;
	}

	/** Which cells' lines an outlet feeds (a bridge's if either of its lines is fed): the screen draws the rest as dead weight. */
	public boolean[] fedCells() {
		int[] channel = channels();
		boolean[] fed = fedChannels(channel);
		boolean[] out = new boolean[cells.length];
		for (int i = 0; i < cells.length; i++) {
			if (channel[i] < 0) continue;
			if (cells[i] != Stroke.BRIDGE) {
				out[i] = fed[channel[i]];
				continue;
			}
			int x = i % size, y = i / size;
			for (int d = 0; d < 4 && !out[i]; d++) {
				int nx = x + DX[d], ny = y + DY[d];
				if (inside(nx, ny) && channel[at(nx, ny)] >= 0 && cells[at(nx, ny)] != Stroke.BRIDGE && fed[channel[at(nx, ny)]]) out[i] = true;
			}
		}
		return out;
	}

	/** Node {@code n}'s veins as the board stands (the sides the qi comes in by, cheapest first): the screen's lit sides. */
	public List<Vein> veinsOf(int n) {
		return veinsOfNode(cells, n);
	}

	private boolean inCoreSquare(int x, int y) {
		return Math.abs(x - centre) <= 1 && Math.abs(y - centre) <= 1;
	}

	/** How much of route {@code a} mirrored (left to right) lies on route {@code b}. */
	private double mirrorShare(Route a, Route b) {
		Set<Integer> theirs = new HashSet<>();
		for (int i : b.cells()) theirs.add(i);
		int matched = 0;
		for (int i : a.cells()) {
			int x = i % size, y = i / size;
			if (theirs.contains(at(size - 1 - x, y))) matched++;
		}
		return (double) matched / Math.max(a.cells().length, b.cells().length);
	}

	/** Every free cell of the resonance ring painted as a ring cell (a node or a joint on the ring is part of it as it stands). */
	public boolean ringClosed() {
		for (int y = centre - 2; y <= centre + 2; y++) {
			for (int x = centre - 2; x <= centre + 2; x++) {
				if (onRing(x, y) && isFree(x, y) && cells[at(x, y)] != Stroke.RING) return false;
			}
		}
		return true;
	}

	// --- The Circuit Paths ---

	/** The Balanced Circuit Path for {@code rank} with the qi its frame's core gives at least: an ancient diagram, a quick look. */
	public Stroke[] circuitPath(int rank) {
		return circuitPath(rank, Build.BALANCED, BUDGET[gradeIndex()]);
	}

	/**
	 * The Circuit Path a puppeteer of {@code rank} (CraftMastery.Rank's order) is shown for this board's blueprint and size, for
	 * {@code build}, with its core giving {@code budget} qi (before the ring): drawn for a flawless frame (this board's own flaws aren't
	 * heeded: the puppeteer routes round them). Every part one vein, the heart first, then the build's raises as far as the qi goes, a
	 * pair's two halves alike; each rank's better than the last:
	 * <ul>
	 *   <li>Apprentice: every line off the up outlet, a part's veins sharing their way; at most two veins a part;</li>
	 *   <li>Adept: each vein from the nearest outlet on a way of its own, touching no part it isn't for; at most three;</li>
	 *   <li>Master: and lines thickened wherever they carry more than a plain line holds; at most four;</li>
	 *   <li>Grandmaster: and cooling vents where heat is left, a gathering knot by the art;</li>
	 *   <li>Saint: and the resonance ring closed round the core if the qi it gives covers what it feeds, ward knots at the narrowest places.</li>
	 * </ul>
	 * Once every part has its first vein, the build's arrays (Build#arrays, phase C) its rank paints the strokes of, as far as the qi goes: a
	 * pair's on its left half, and from Saint its mirror on the right too, a Twin Seal. Strokes in board order (NONE where nothing is drawn).
	 */
	public Stroke[] circuitPath(int rank, Build build, int budget) {
		return circuitPath(rank, build, budget, null);
	}

	/**
	 * {@link #circuitPath(int, Build, int)} for a core of {@code core}'s phase: from Master the outlets the lines leave by are sealed in the
	 * core's own phase (phase B: its resonance's qi counted as the path is drawn).
	 */
	public Stroke[] circuitPath(int rank, Build build, int budget, Phase core) {
		CircuitBoard plan = new CircuitBoard(blueprint, gradeIndex(), 0, 0);
		int r = Math.max(0, Math.min(4, rank));
		boolean seals = r >= 2 && core != null;
		plan.draw(r, build, budget + (seals ? RESONANCE_QI : 0));
		if (seals) {
			// (Never an array's own line: a seal on a Guard Coil's corner would undo it.)
			Set<Integer> arrayCells = new HashSet<>();
			for (Formed f : plan.arrays()) for (int i : f.cells()) arrayCells.add(i);
			for (int d = 0; d < 4; d++) {
				int i = plan.at(centre + DX[d], centre + DY[d]);
				if ((plan.cells[i] == Stroke.LINE || plan.cells[i] == Stroke.THICK) && !arrayCells.contains(i)) plan.cells[i] = Stroke.seal(core);
			}
		}
		return plan.cells.clone();
	}

	/** Draws the Circuit Path on this (blank, flawless) board. */
	private void draw(int rank, Build build, int budget) {
		boolean trunk = rank == 0;
		int cap = PATH_VEINS[rank];
		List<int[]> raises = new ArrayList<>();
		for (int part : new int[] {HEART, ART, HEAD, PAIR0, PAIR1}) raises.add(new int[] {part, 1});
		for (Build b : build == Build.BALANCED ? new Build[] {build} : new Build[] {build, Build.BALANCED}) {
			for (int k = 0; k + 1 < b.raises.length; k += 2) raises.add(new int[] {b.raises[k], b.raises[k + 1]});
		}
		for (int k = 0; k < raises.size(); k++) {
			// The build's arrays: an Iron Lock first, while the heart's sides are clear (the veins route round it); the rest once every part
			// has its first vein, before the raises spend their qi. A Saint's ring closes then too, so the raises are drawn with its qi and
			// the veins it gives the parts on it.
			if (k == 0) placeArrays(build, rank, budget, true);
			if (k == BASE_RAISES) {
				if (rank >= 4) closeRing(budget, rank);
				placeArrays(build, rank, withRing(budget), false);
			}
			int[] raise = raises.get(k);
			int level = raise[1], n = part(raise[0]);
			if (n < 0 || level > cap || raise[0] == ART && !hasArt() && level > 1) continue;
			int partner = nodes.get(n).pair() >= 0 ? partner(n) : -1;
			while (veinCount(n) < level) {
				Stroke[] before = cells.clone();
				int hot = overflow(rank);
				if (!addVein(n, partner, rank, level == 1)) break;
				// The other half kept even, on its own way if the mirror didn't reach it.
				while (partner >= 0 && veinCount(partner) < veinCount(n)) if (!addVein(partner, -1, rank, level == 1)) break;
				// A part's first vein stays whatever it costs; one more only within the qi, the pairs even, and no hotter than the rank can bear.
				if (level > 1 && (wantedNow() > withRing(budget) || !pairsEven() || overflow(rank) > hot)) {
					System.arraycopy(before, 0, cells, 0, cells.length);
					break;
				}
			}
		}
		if (rank >= 2) thicken();
		if (rank >= 4 && !ringClosed()) closeRing(budget, rank);
		if (rank >= 3) {
			gather(withRing(budget));
			link();
			vents();
		}
		if (rank >= 4) ward();
	}

	/**
	 * A link knot (puppet phase 3) on the side of the heart whose vein carries least, where a plain line's worth is all it carries (a knot
	 * holds no more) or the core's square holds it; never on an outlet (its seal's place), a closed ring nor an array's line, and none where
	 * one stands by the heart already. It carries the vein as the line did: only the puppet's linking changes.
	 */
	private void link() {
		int heart = part(HEART);
		if (heart < 0) return;
		PuppetBlueprint.Node node = nodes.get(heart);
		for (int d = 0; d < 4; d++) {
			int x = node.x() + DX[d], y = node.y() + DY[d];
			if (inside(x, y) && cells[at(x, y)] == Stroke.LINK) return;
		}
		List<List<Vein>> veins = veins(cells);
		int[] carried = carried(veins, null);
		boolean ring = ringClosed();
		Set<Integer> arrayCells = new HashSet<>();
		for (Formed f : arrays()) for (int i : f.cells()) arrayCells.add(i);
		int best = -1;
		for (Vein v : veins.get(heart)) {
			int side = v.side(), x = side % size, y = side / size;
			if (fixed[side] != 0 || cells[side] != Stroke.LINE && cells[side] != Stroke.THICK || isOutlet(x, y) || ring && onRing(x, y)
					|| arrayCells.contains(side)) continue;
			if (!inCoreSquare(x, y) && carried[side] > lineHold()) continue;
			if (best < 0 || carried[side] < carried[best]) best = side;
		}
		if (best >= 0) cells[best] = Stroke.LINK;
	}

	/** The qi a core giving {@code budget} gives this board as it stands: the closed ring's besides. */
	private int withRing(int budget) {
		return budget + (ringClosed() ? RING_QI : 0);
	}

	/** The raises that give every part its first vein (heart, art, head and the pairs: draw's first). */
	private static final int BASE_RAISES = 5;

	/**
	 * The build's arrays (phase C) its rank paints the strokes of, each by the part it is drawn by: a pair's on its left half, and from Saint
	 * its mirror on the right as well, a Twin Seal (alone where the two won't stand). {@code early}: the Iron Lock alone (on a blank board),
	 * else the rest.
	 */
	private void placeArrays(Build build, int rank, int budget, boolean early) {
		for (Array a : build.arrays()) {
			if (rank < a.rank || a == Array.GATHERING_SPIRAL && !hasArt() || (a == Array.IRON_LOCK) != early) continue;
			int n = part(switch (a) {
				case EYE -> HEAD;
				case GATHERING_SPIRAL -> ART;
				case IRON_LOCK -> HEART;
				case GUARD_COIL -> PAIR0;
				case GALE_STAIR -> blueprint == PuppetBlueprint.BEE ? PAIR0 : PAIR1;
			});
			if (n < 0 || !a.fits(nodes.get(n))) continue;
			boolean twin = rank >= 4 && a.twinnable() && nodes.get(n).pair() >= 0;
			if (!(twin && placeArray(a, n, rank, budget, true))) placeArray(a, n, rank, budget, false);
		}
	}

	/**
	 * Array {@code a} drawn by node {@code n} ({@code twin}: and its mirror by the other half of the pair). Tried by each of its sides, those
	 * its veins come in by already first and those clear of the resonance ring before the rest; its strokes on free cells (a line painted
	 * over where it must be, never a new stroke beside another part, nor beside this one but by its side), joined to the core's qi by a
	 * line of its own if nothing reaches it. Kept if it stands and works: every part still fed, within the qi, the pairs even, no hotter
	 * than the rank can bear, and a pair's half drawn alone adding it no vein nor changing its nearest way (Harmony reads the two).
	 */
	private boolean placeArray(Array a, int n, int rank, int budget, boolean twin) {
		Stroke[] before = cells.clone();
		int hot = overflow(rank), deadBefore = deadCount(), veinsBefore = veinCount(n);
		int[] countsBefore = veinCounts();
		boolean pair = nodes.get(n).pair() >= 0;
		int partner = pair ? partner(n) : -1;
		List<Vein> was = veinsOfNode(cells, n);
		int[] nearestBefore = was.isEmpty() ? new int[0] : was.get(0).route().cells();
		for (int[][] shape : candidates(a, n, rank)) {
			for (int[] p : shape) cells[p[0]] = Stroke.values()[p[1]];
			boolean ok = touchesQi(shape) || connect(n, shape, rank);
			if (ok && twin) ok = mirrorDrawn(before, n);
			if (ok) {
				Formed f = null;
				for (Formed g : arrays()) if (g.node() == n && g.array() == a) f = g;
				boolean[] reached = fedCells();
				ok = f != null && reached(f, reached) && (!twin || f.twin()) && deadCount() <= deadBefore && wantedNow() <= budget && pairsEven()
						&& overflow(rank) <= hot;
				// No other part fed anew by it (nor starved): the array is its own part's.
				int[] counts = veinCounts();
				for (int m = 0; m < counts.length && ok; m++) if (m != n && m != partner) ok = counts[m] == countsBefore[m];
				if (ok && pair && !twin) {
					List<Vein> now = veinsOfNode(cells, n);
					ok = veinCount(n) == veinsBefore && !now.isEmpty() && Arrays.equals(now.get(0).route().cells(), nearestBefore);
				}
			}
			if (ok) return true;
			System.arraycopy(before, 0, cells, 0, cells.length);
		}
		return false;
	}

	/**
	 * The ways {@code a} could be drawn by node {@code n} on the board as it stands, each its cells and strokes ({cell, stroke's order}) to
	 * paint, in the order they are tried (see {@link #placeArray}).
	 */
	private List<int[][]> candidates(Array a, int n, int rank) {
		PuppetBlueprint.Node node = nodes.get(n);
		List<int[][]> first = new ArrayList<>(), later = new ArrayList<>();
		boolean[] reached = fedCells();
		for (int d : SIDES) {
			int sx = node.x() + DX[d], sy = node.y() + DY[d];
			if (!isFree(sx, sy)) continue;
			int s = at(sx, sy);
			List<int[][]> shapes = new ArrayList<>();
			switch (a) {
				case EYE, GATHERING_SPIRAL, IRON_LOCK -> {
					Stroke knot = a == Array.EYE ? Stroke.RELAY : a == Array.GATHERING_SPIRAL ? Stroke.GATHER : Stroke.WARD;
					int[][] shape = knotShape(n, s, d, knot);
					if (shape != null) shapes.add(shape);
				}
				case GUARD_COIL -> {
					for (int[] block : blocks(sx, sy, false)) {
						int[][] shape = cellsShape(n, s, block, Stroke.THICK);
						if (shape != null) shapes.add(shape);
					}
				}
				case GALE_STAIR -> {
					for (int[] slant : slants(sx, sy, false)) {
						int[][] shape = cellsShape(n, s, slant, Stroke.DIAGONAL);
						if (shape != null) shapes.add(shape);
					}
				}
			}
			for (int[][] shape : shapes) {
				boolean ring = false, core = false;
				for (int[] p : shape) {
					ring |= onRing(p[0] % size, p[0] / size);
					core |= inCoreSquare(p[0] % size, p[0] / size);
				}
				// Never in the core's square (no array stands there), nor on the ring for a Saint (whose ring closes round it); a side a vein
				// of the part comes in by first (no new vein; a ward, which cuts what it stands on, by an empty side), off the ring.
				if (core || ring && rank >= 4) continue;
				boolean vein = reached[s] && cells[s].conducts();
				(vein != (a == Array.IRON_LOCK) && !ring ? first : later).add(shape);
			}
		}
		first.addAll(later);
		return first;
	}

	/**
	 * A knot array's strokes at side cell {@code s} (the way {@code d} out of node {@code n}): the knot, and on each other side that is a
	 * free cell a line (bridges round a ward); null where it can't go: a knot over anything but an empty cell or a line, past a board's
	 * relays or wards, a new stroke beside another part, a bridge on an outlet, too few lines or bridges.
	 */
	private int[][] knotShape(int n, int s, int d, Stroke knot) {
		Stroke here = cells[s];
		if (here != Stroke.NONE && here != Stroke.LINE && here != Stroke.THICK && here != knot) return null;
		if (knot == Stroke.RELAY && here != knot && count(Stroke.RELAY) >= MAX_RELAYS) return null;
		if (knot == Stroke.WARD && here != knot && count(Stroke.WARD) >= MAX_WARDS) return null;
		if (knot == Stroke.GATHER && !besideKind(s % size, s / size, PuppetBlueprint.NodeKind.ART)) return null;
		boolean bridges = knot == Stroke.WARD;
		Stroke round = bridges ? Stroke.BRIDGE : Stroke.LINE;
		List<int[]> shape = new ArrayList<>();
		shape.add(new int[] {s, knot.ordinal()});
		int sx = s % size, sy = s / size, back = (d + 2) % 4, sides = 0;
		for (int e = 0; e < 4; e++) {
			if (e == back) continue;
			int tx = sx + DX[e], ty = sy + DY[e];
			if (!isFree(tx, ty) || inCoreSquare(tx, ty)) continue;
			int t = at(tx, ty);
			Stroke there = cells[t];
			if (bridges) {
				// (A bridge may stand by another part: its line toward the ward carries nothing; placeArray weighs the rest.)
				if (isOutlet(tx, ty) || there != Stroke.NONE && there != Stroke.BRIDGE) return null;
			} else if (!isLine(there) && (there != Stroke.NONE || besideOther(t, n, false))) {
				return null;
			}
			if (there == Stroke.NONE) shape.add(new int[] {t, round.ordinal()});
			sides++;
		}
		return sides < (bridges ? 1 : 2) ? null : shape.toArray(new int[0][]);
	}

	/**
	 * A block or slant array's strokes ({@code stroke} on each of {@code group}'s cells, {@code s}, the one by node {@code n}, first); null
	 * where it can't go: a cell not free, holding other than an empty cell, a line or the stroke itself, a new stroke beside another part,
	 * or a cell beside this part but by {@code s}.
	 */
	private int[][] cellsShape(int n, int s, int[] group, Stroke stroke) {
		List<int[]> shape = new ArrayList<>();
		shape.add(new int[] {s, stroke.ordinal()});
		for (int i : group) {
			int x = i % size, y = i / size;
			if (!isFree(x, y) || besideNodeBut(i, n, s)) return null;
			Stroke there = cells[i];
			if (there != Stroke.NONE && there != Stroke.LINE && there != stroke) return null;
			if (there == Stroke.NONE && besideOther(i, n, false)) return null;
			if (i != s) shape.add(new int[] {i, stroke.ordinal()});
		}
		return shape.toArray(new int[0][]);
	}

	/** Whether the core's qi reaches any of a shape's cells as the board stands (painted: a shape over a fed line joins it). */
	private boolean touchesQi(int[][] shape) {
		boolean[] reached = fedCells();
		for (int[] p : shape) if (reached[p[0]]) return true;
		return false;
	}

	/**
	 * A line of its own from the core's outlets to the nearest of a shape's conducting cells (the cheapest way round the busy lines, by no
	 * empty cell beside any part; the shape's first cell is the one by its part); whether one was found and drawn.
	 */
	private boolean connect(int n, int[][] shape, int rank) {
		int[] toll = tolls(n, rank);
		int side = shape[0][0];
		boolean[] barred = barred(n, side, null, TOUCH_NONE);
		Route best = null;
		for (int[] p : shape) {
			if (!Stroke.values()[p[1]].conducts()) continue;
			Route r = toCell(n, p[0], 0b1111, barred, toll);
			if (r != null && (best == null || r.cost() < best.cost())) best = r;
		}
		if (best == null) return false;
		for (int i : best.cells()) if (fixed[i] == 0 && cells[i] == Stroke.NONE) cells[i] = Stroke.LINE;
		return true;
	}

	/**
	 * Every cell changed since {@code before} painted on its mirror too (a pair's other half drawn alike: a Twin Seal); false if a mirror
	 * cell isn't free or holds a stroke of its own.
	 */
	private boolean mirrorDrawn(Stroke[] before, int n) {
		List<Integer> changed = new ArrayList<>();
		for (int i = 0; i < cells.length; i++) if (cells[i] != before[i]) changed.add(i);
		for (int i : changed) {
			int x = i % size, y = i / size, j = at(size - 1 - x, y);
			if (j == i) continue;
			if (!isFree(size - 1 - x, y)) return false;
			Stroke there = cells[j];
			if (there != Stroke.NONE && there != before[i] && there != cells[i]) return false;
			cells[j] = cells[i];
		}
		return true;
	}

	/** The qi the board as it stands would draw: all its veins, and the arrays its core's qi reaches by a living part. */
	private int wantedNow() {
		List<List<Vein>> veins = veins(cells);
		int want = wanted(veins);
		boolean[] reached = fedCells();
		for (Formed f : arrays()) if (!veins.get(f.node()).isEmpty() && reached(f, reached)) want += qiOf(f);
		return want;
	}

	/** How many parts nothing feeds. */
	private int deadCount() {
		int dead = 0;
		for (List<Vein> v : veins(cells)) if (v.isEmpty()) dead++;
		return dead;
	}

	/** Each part's veins as the board stands. */
	private int[] veinCounts() {
		List<List<Vein>> veins = veins(cells);
		int[] counts = new int[veins.size()];
		for (int n = 0; n < counts.length; n++) counts[n] = veins.get(n).size();
		return counts;
	}

	/**
	 * Each cell's toll for a new way to node {@code n}: half what it already carries, and dear where {@code n}'s load would carry it past what
	 * a puppeteer of {@code rank} can make it hold (the core's square none).
	 */
	private int[] tolls(int n, int rank) {
		int[] toll = carried(veins(cells), null);
		for (int i = 0; i < toll.length; i++) {
			if (inCoreSquare(i % size, i / size)) toll[i] = 0;
			else toll[i] = toll[i] / 2 + (toll[i] + load(n) > capacity(i, rank) ? 8 : 0);
		}
		return toll;
	}

	/** The node a raise names (a pair's left half), or -1. */
	private int part(int which) {
		for (int n = 0; n < nodes.size(); n++) {
			PuppetBlueprint.Node node = nodes.get(n);
			boolean match = switch (which) {
				case HEAD -> node.kind() == PuppetBlueprint.NodeKind.SENSE;
				case HEART -> node.kind() == PuppetBlueprint.NodeKind.SEAL;
				case ART -> node.kind() == PuppetBlueprint.NodeKind.ART;
				case PAIR0 -> node.pair() == 0 && node.x() < centre;
				default -> node.pair() == 1 && node.x() < centre;
			};
			if (match) return n;
		}
		return -1;
	}

	private int veinCount(int n) {
		return veinsOfNode(cells, n).size();
	}

	/** Node {@code n}'s veins alone (as {@link #veins} weighs them, another part beside it asked how near it is by its own route). */
	private List<Vein> veinsOfNode(Stroke[] board, int n) {
		PuppetBlueprint.Node node = nodes.get(n);
		Route[] sides = sideRoutes(board, search(board, n, -1, 0b1111, null, true, true));
		int nearest = UNREACHED;
		for (Route r : sides) if (r != null) nearest = Math.min(nearest, r.cost());
		List<Vein> list = new ArrayList<>();
		for (int d : node.x() > centre ? MIRRORED_SIDES : SIDES) {
			Route r = sides[d];
			if (r == null) continue;
			int side = at(node.x() + DX[d], node.y() + DY[d]);
			int m = fixed[side] >= 4 ? fixed[side] - 4 : -1;
			if (m >= 0) {
				Route theirs = route(board, m);
				int near = theirs == null ? UNREACHED : theirs.cost();
				if (!(near < nearest || near == nearest && m < n)) continue;
			}
			list.add(new Vein(n, side, r));
		}
		list.sort(Comparator.comparingInt(v -> v.route().cost()));
		return list;
	}

	/**
	 * What cell {@code i} can be made to carry by a puppeteer of {@code rank}: an Adept's plain line, a Master's and above thickened (the
	 * ring and a part passing the qi on hold as much); an Apprentice draws without weighing it.
	 */
	private int capacity(int i, int rank) {
		if (rank == 0) return Integer.MAX_VALUE / 4;
		// A knot, a bridge, a diagonal or a seal can't be thickened: a plain line's worth.
		Stroke s = cells[i];
		if (fixed[i] == 0 && s != Stroke.NONE && s != Stroke.LINE && s != Stroke.THICK && s != Stroke.RING) return lineHold();
		return rank >= 2 || fixed[i] >= 4 || cells[i] == Stroke.RING ? 2 * lineHold() : lineHold();
	}

	/** How far the drawing's lines carry past what a puppeteer of {@code rank} can make them hold, all told (the core's square aside). */
	private int overflow(int rank) {
		if (rank == 0) return 0;
		int[] carried = carried(veins(cells), null);
		int over = 0;
		for (int i = 0; i < cells.length; i++) {
			if (inCoreSquare(i % size, i / size) || !carries(i)) continue;
			over += Math.max(0, carried[i] - capacity(i, rank));
		}
		return over;
	}

	/** Whether every pair's two halves have as many veins. */
	private boolean pairsEven() {
		for (int n = 0; n < nodes.size(); n++) {
			PuppetBlueprint.Node node = nodes.get(n);
			if (node.pair() >= 0 && node.x() < centre && veinCount(n) != veinCount(partner(n))) return false;
		}
		return true;
	}

	// Which parts' sides a new vein's way keeps clear of: every other part's, only the head's, mouth's and heart's (a pair's passed on both
	// halves alike), or none.
	private static final int TOUCH_NONE = 0, TOUCH_PAIRS = 1, TOUCH_ANY = 2;

	/**
	 * One vein more for node {@code n} (and its mirror for {@code partner}, a pair's other half): the cheapest way to one of its empty sides,
	 * round the lines that already carry most. Tried strictly first (on a way apart from its other veins', an Apprentice's off the up outlet
	 * and shared; touching no other part, nor this one but by that side), then sharing ways, then passing a pair's sides (drawn on both
	 * halves alike, so the pair stays even), then (only for a part's first vein, {@code base}) touching anything.
	 */
	private boolean addVein(int n, int partner, int rank, boolean base) {
		boolean trunk = rank == 0;
		PuppetBlueprint.Node node = nodes.get(n);
		boolean[] mine = new boolean[cells.length];
		for (Vein v : veinsOfNode(cells, n)) for (int i : v.route().cells()) mine[i] = true;
		// The toll of a cell: half what it already carries, and dear where this vein's load would carry it past what the rank can make it hold.
		int[] toll = tolls(n, rank);
		// A part on the centre line takes its veins from above and below first: one from a side is drawn on both sides alike (two veins).
		boolean centred = node.pair() < 0;
		int[][] tries = centred ? new int[][] {{0, 2}, {1, 3}} : new int[][] {node.x() > centre ? MIRRORED_SIDES : SIDES};
		for (int[] sides : tries) {
			// Every way of drawing it weighed together: the strictest wins unless a looser one is shorter by more than it gives up (a way of
			// its own over a shared one; touching no other part over passing a pair's side, or, for a first vein, touching anything).
			Route best = null;
			int bestScore = Integer.MAX_VALUE, bestTouch = TOUCH_NONE;
			for (int strict = 2; strict >= (base ? -1 : 0); strict--) {
				int touch = strict >= 1 ? TOUCH_NONE : strict == 0 ? TOUCH_PAIRS : TOUCH_ANY;
				for (int d : sides) {
					int sx = node.x() + DX[d], sy = node.y() + DY[d];
					if (!isFree(sx, sy) || cells[at(sx, sy)] != Stroke.NONE) continue;
					int side = at(sx, sy);
					if (touch == TOUCH_NONE && besideOther(side, n, false) || touch == TOUCH_PAIRS && besideOther(side, n, true)) continue;
					Route r = toCell(n, side, trunk && strict >= 1 ? 1 : 0b1111, barred(n, side, strict >= 2 && !trunk ? mine : null, touch), toll);
					if (r == null) continue;
					int score = r.cost() + LOOSENESS[strict + 1];
					if (score < bestScore) {
						bestScore = score;
						best = r;
						bestTouch = touch;
					}
				}
			}
			if (best == null) continue;
			for (int i : best.cells()) if (fixed[i] == 0 && cells[i] == Stroke.NONE) cells[i] = Stroke.LINE;
			// A pair's other half, and a centre part's other side, drawn alike: the board stays its own mirror (Harmony reads the pairs' ways).
			if (partner >= 0 || bestTouch == TOUCH_PAIRS || node.pair() < 0) {
				for (int i : best.cells()) {
					int j = at(size - 1 - i % size, i / size);
					if (fixed[j] == 0 && cells[j] == Stroke.NONE) cells[j] = Stroke.LINE;
				}
			}
			return true;
		}
		return false;
	}

	/** What a looser way of drawing a vein must save to be taken (cells), by strictness: anyhow, by a pair's side, sharing ways, apart. */
	private static final int[] LOOSENESS = {6, 4, 4, 0};

	/** Whether cell {@code i} touches a node other than {@code n} (with {@code centreOnly}, one not of a pair). */
	private boolean besideOther(int i, int n, boolean centreOnly) {
		int x = i % size, y = i / size;
		for (int d = 0; d < 4; d++) {
			int nx = x + DX[d], ny = y + DY[d];
			if (!inside(nx, ny)) continue;
			int f = fixed[at(nx, ny)];
			if (f >= 4 && f != 4 + n && (!centreOnly || nodes.get(f - 4).pair() < 0)) return true;
		}
		return false;
	}

	/**
	 * The cells a new vein to {@code side} of node {@code n} keeps off: {@code mine} (its other veins' ways), the empty cells beside {@code n}
	 * but {@code side} (another vein of its own by the way), and by {@code touch} the empty cells beside the other nodes.
	 */
	private boolean[] barred(int n, int side, boolean[] mine, int touch) {
		boolean[] barred = mine == null ? new boolean[cells.length] : mine.clone();
		for (int m = 0; m < nodes.size(); m++) {
			if (m != n && (touch == TOUCH_ANY || touch == TOUCH_PAIRS && nodes.get(m).pair() >= 0)) continue;
			PuppetBlueprint.Node node = nodes.get(m);
			for (int d = 0; d < 4; d++) {
				int x = node.x() + DX[d], y = node.y() + DY[d];
				if (!inside(x, y)) continue;
				int i = at(x, y);
				if (i != side && cells[i] == Stroke.NONE) barred[i] = true;
			}
		}
		barred[side] = false;
		return barred;
	}

	/**
	 * The cheapest way from the outlets in {@code outlets} to cell {@code target} over the free cells, lines drawn or not, off {@code barred},
	 * each cell costing its {@code toll} more; through another part where the cells beside it may be passed (the head fed straight through
	 * the mouth, as the user asked of the board: no lines round it, so the mouth isn't fed by them over again).
	 */
	private Route toCell(int n, int target, int outlets, boolean[] barred, int[] toll) {
		Stroke[] potential = cells.clone();
		for (int i = 0; i < potential.length; i++) if (fixed[i] == 0 && potential[i] == Stroke.NONE) potential[i] = Stroke.LINE;
		Search s = search(potential, n, -1, outlets, barred, true, true, toll);
		int bestState = cheapestState(s, target, nodes.get(n).x() > centre, potential[target], -1);
		return bestState < 0 ? null : routeFrom(potential, s, bestState, s.cost()[bestState]);
	}

	/** Every plain line that carries more than it holds thickened. */
	private void thicken() {
		int[] carried = carried(veins(cells), null);
		for (int i = 0; i < cells.length; i++) if (carried[i] > lineHold() && cells[i] == Stroke.LINE) cells[i] = Stroke.THICK;
	}

	/** The resonance ring closed round the core, if no knot stands on it, the qi it gives covers what it feeds besides, and it adds no heat. */
	private void closeRing(int budget, int rank) {
		Stroke[] before = cells.clone();
		int hot = overflow(rank);
		for (int y = centre - 2; y <= centre + 2; y++) {
			for (int x = centre - 2; x <= centre + 2; x++) {
				if (!onRing(x, y) || !isFree(x, y)) continue;
				Stroke s = cells[at(x, y)];
				if (s != Stroke.NONE && s != Stroke.LINE && s != Stroke.THICK) {
					System.arraycopy(before, 0, cells, 0, cells.length);
					return;
				}
				cells[at(x, y)] = Stroke.RING;
			}
		}
		List<List<Vein>> veins = veins(cells);
		int[] counts = new int[nodes.size()];
		for (int n = 0; n < counts.length; n++) counts[n] = veins.get(n).size();
		boolean even = true;
		for (int n = 0; n < counts.length; n++) if (nodes.get(n).pair() >= 0 && counts[n] != counts[partner(n)]) even = false;
		if (!even || wantedNow() > budget + RING_QI || overflow(rank) > hot) System.arraycopy(before, 0, cells, 0, cells.length);
	}

	/**
	 * A gathering knot on the side of the art's that carries least, if a plain line's worth is all it carries (a knot holds no more), never
	 * on a closed ring; a frame with an art only, and none where a gathering knot stands by the art already (a Gathering Spiral's). Where
	 * its lines happen to make it a Gathering Spiral, only if the qi covers the spiral (the next side tried else).
	 */
	private void gather(int budget) {
		int art = part(ART);
		if (!hasArt() || art < 0) return;
		PuppetBlueprint.Node node = nodes.get(art);
		for (int d = 0; d < 4; d++) {
			int x = node.x() + DX[d], y = node.y() + DY[d];
			if (inside(x, y) && cells[at(x, y)] == Stroke.GATHER) return;
		}
		List<List<Vein>> veins = veins(cells);
		int[] carried = carried(veins, null);
		boolean ring = ringClosed();
		List<Integer> sides = new ArrayList<>();
		for (Vein v : veins.get(art)) {
			int side = v.side(), x = side % size, y = side / size;
			if (fixed[side] != 0 || cells[side] != Stroke.LINE && cells[side] != Stroke.THICK || ring && onRing(x, y)) continue;
			if (!inCoreSquare(x, y) && carried[side] > lineHold()) continue;
			sides.add(side);
		}
		sides.sort(Comparator.comparingInt(side -> carried[side]));
		// (Never over another array's line: an Eye's relay beside the mouth would lose it.)
		Set<Integer> arrayCells = new HashSet<>();
		for (Formed f : arrays()) for (int i : f.cells()) arrayCells.add(i);
		for (int side : sides) {
			if (arrayCells.contains(side)) continue;
			Stroke was = cells[side];
			cells[side] = Stroke.GATHER;
			if (wantedNow() <= budget) return;
			cells[side] = was;
		}
	}

	/** Cooling vents on the edge where they draw off the most heat left, while there is heat and room. */
	private void vents() {
		for (int k = 0; k < PATH_VENTS; k++) {
			List<List<Vein>> veins = veins(cells);
			int[] heat = heatOf(veins, null);
			int bestCell = -1, bestCover = 0;
			for (int i = 0; i < cells.length; i++) {
				int x = i % size, y = i / size;
				if (!onEdge(x, y) || fixed[i] != 0 || cells[i] != Stroke.NONE) continue;
				int cover = 0;
				for (int j = 0; j < heat.length; j++) {
					if (heat[j] > 0 && Math.max(Math.abs(j % size - x), Math.abs(j / size - y)) <= VENT_REACH) cover += heat[j];
				}
				cover = Math.min(cover, VENT_COOLING);
				if (cover > bestCover) {
					bestCover = cover;
					bestCell = i;
				}
			}
			if (bestCell < 0) return;
			cells[bestCell] = Stroke.VENT;
		}
	}

	/** Ward knots, as many as a board allows (an Iron Lock's among them), on empty free cells beside the lines a single cut of would sever a part at. */
	private void ward() {
		int[] crossings = new int[cells.length];
		for (int n = 0; n < nodes.size(); n++) {
			Route r = route(cells, n);
			if (r == null) continue;
			for (int i : r.cells()) if (fixed[i] < 4 && route(cells, n, i) == null) crossings[i] += Math.max(1, load(n));
		}
		for (int k = count(Stroke.WARD); k < MAX_WARDS; k++) {
			int bestCell = -1, bestCover = 0;
			for (int i = 0; i < cells.length; i++) {
				int x = i % size, y = i / size;
				if (!isFree(x, y) || cells[i] != Stroke.NONE) continue;
				int cover = 0;
				for (int dy = -1; dy <= 1; dy++) {
					for (int dx = -1; dx <= 1; dx++) if (inside(x + dx, y + dy)) cover += crossings[at(x + dx, y + dy)];
				}
				if (cover > bestCover) {
					bestCover = cover;
					bestCell = i;
				}
			}
			if (bestCell < 0) break;
			cells[bestCell] = Stroke.WARD;
			int x = bestCell % size, y = bestCell / size;
			for (int dy = -1; dy <= 1; dy++) {
				for (int dx = -1; dx <= 1; dx++) if (inside(x + dx, y + dy)) crossings[at(x + dx, y + dy)] = 0;
			}
		}
	}

	/** The other node of node {@code n}'s pair. */
	private int partner(int n) {
		int pair = nodes.get(n).pair();
		for (int m = 0; m < nodes.size(); m++) if (m != n && nodes.get(m).pair() == pair) return m;
		return n;
	}

	/** Lays {@code path} on this board (its own flaws heeded: what falls on a flaw is left out). */
	public void lay(Stroke[] path) {
		for (int i = 0; i < path.length && i < cells.length; i++) {
			int x = i % size, y = i / size;
			// (A body's meridian standing keeps its line; a broken one takes the path's line.)
			if (isFree(x, y) && !(isMeridian(x, y) && cells[i] != Stroke.NONE)) cells[i] = path[i];
		}
	}

	/**
	 * The channel each conducting cell is in (a number; -1 for none), cells joined as conduction joins them. A bridge's two lines are its
	 * own: the cell is given its up-and-down line's channel (enough to weigh leaks, which pass bridges over).
	 */
	private int[] channels() {
		return channels(true);
	}

	/** The line-channel of each conducting cell (phase B): {@link #channels} that never join through a part (-1 for none, the parts too). */
	private int[] lineChannels() {
		return channels(false);
	}

	private int[] channels(boolean throughParts) {
		int[] channel = new int[cells.length];
		Arrays.fill(channel, -1);
		int next = 0;
		for (int start = 0; start < cells.length; start++) {
			if (!carries(start, throughParts) || channel[start] >= 0) continue;
			ArrayDeque<Integer> queue = new ArrayDeque<>();
			queue.add(start);
			channel[start] = next;
			while (!queue.isEmpty()) {
				int i = queue.poll(), x = i % size, y = i / size;
				for (int d = 0; d < 8; d++) {
					if (d >= 4 && cells[i] != Stroke.DIAGONAL) break;
					int nx = x + DX[d], ny = y + DY[d];
					if (!inside(nx, ny)) continue;
					int j = at(nx, ny);
					if (!carries(j, throughParts) || channel[j] >= 0) continue;
					if (d >= 4 && cells[j] != Stroke.DIAGONAL) continue;
					if (cells[i] == Stroke.BRIDGE || cells[j] == Stroke.BRIDGE) continue; // a bridge keeps its lines apart
					channel[j] = next;
					queue.add(j);
				}
			}
			next++;
		}
		return channel;
	}

	/** Whether qi runs through cell {@code i}: a conducting stroke, or a node (which passes it on to whatever touches it). */
	private boolean carries(int i) {
		return carries(i, true);
	}

	private boolean carries(int i, boolean throughParts) {
		return throughParts && fixed[i] >= 4 || fixed[i] == 0 && cells[i].conducts();
	}

	/** Which channels an outlet feeds. */
	private boolean[] fedChannels(int[] channel) {
		int most = Arrays.stream(channel).max().orElse(-1) + 1;
		boolean[] fed = new boolean[Math.max(1, most)];
		for (int d = 0; d < 4; d++) {
			int i = at(centre + DX[d], centre + DY[d]);
			if (channel[i] >= 0) fed[channel[i]] = true;
		}
		// What only a bridge carries on from an outlet's channel is fed too.
		boolean grew = true;
		while (grew) {
			grew = false;
			for (int i = 0; i < cells.length; i++) {
				if (cells[i] != Stroke.BRIDGE) continue;
				int x = i % size, y = i / size;
				for (int axis = 0; axis < 2; axis++) {
					int a = axis == 0 ? 0 : 1, b = axis == 0 ? 2 : 3;
					int ja = inside(x + DX[a], y + DY[a]) ? at(x + DX[a], y + DY[a]) : -1;
					int jb = inside(x + DX[b], y + DY[b]) ? at(x + DX[b], y + DY[b]) : -1;
					if (ja < 0 || jb < 0 || channel[ja] < 0 || channel[jb] < 0) continue;
					if (fed[channel[ja]] != fed[channel[jb]]) {
						fed[channel[ja]] = fed[channel[jb]] = true;
						grew = true;
					}
				}
			}
		}
		return fed;
	}

	private boolean joinedDiagonally(int i, int j) {
		return cells[i] == Stroke.DIAGONAL && cells[j] == Stroke.DIAGONAL;
	}

	/** A dead-end stub: a line with one neighbour it joins, beside no node, outlet or knot. */
	private boolean isStub(int x, int y) {
		Stroke here = cells[at(x, y)];
		if (here != Stroke.LINE && here != Stroke.THICK && here != Stroke.DIAGONAL) return false;
		if (isOutlet(x, y)) return false;
		int joined = 0;
		for (int d = 0; d < 8; d++) {
			if (d >= 4 && here != Stroke.DIAGONAL) break;
			int nx = x + DX[d], ny = y + DY[d];
			if (!inside(nx, ny)) continue;
			int j = at(nx, ny);
			if (fixed[j] >= 4) return false;
			Stroke there = cells[j];
			if (there == Stroke.VENT || there == Stroke.WARD || there == Stroke.GATHER || there == Stroke.RELAY || there == Stroke.LINK) return false;
			if (there.conducts() && (d < 4 || there == Stroke.DIAGONAL)) joined++;
		}
		return joined <= 1;
	}

	/** The cells a ward knot keeps from being cut: its eight neighbours. */
	public boolean[] wardedCells() {
		boolean[] warded = new boolean[cells.length];
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] != Stroke.WARD) continue;
			int x = i % size, y = i / size;
			for (int dy = -1; dy <= 1; dy++) {
				for (int dx = -1; dx <= 1; dx++) if (inside(x + dx, y + dy)) warded[at(x + dx, y + dy)] = true;
			}
		}
		return warded;
	}

	/** The cells every node's route runs through now, for the screen to show (and for cuts: CircuitBoards). */
	public int[][] routes() {
		int[][] all = new int[nodes.size()][];
		for (int n = 0; n < nodes.size(); n++) {
			Route route = route(cells, n);
			all[n] = route == null ? new int[0] : route.cells();
		}
		return all;
	}
}
