package com.example.defyingtheheavens;

import java.util.ArrayList;
import java.util.List;

/**
 * A puppet's shape (Puppetry, phase 1 of the user's puppet design, 2026-11-17; "Humanoid + bee first"): what its frame is forged as
 * (ArtifactKind#HUMANOID_FRAME, #BEE_FRAME) and where its circuit board's nodes and joints lie (CircuitBoard). The humanoid fights and
 * guards; the bee carries and runs messages. The hound, the spider and the beast one rides are to come.
 * <p>
 * A board is square and odd-sized, from 9 x 9 at a Mortal frame to 17 x 17 at an Immortal one (CircuitBoard#size); its core socket sits
 * at the centre. Every layout here is given from the centre and the edges, so it stands on any size, and the two halves of a pair of
 * limbs lie as each other's mirror (left to right), as do the joints: on a flawless frame a symmetric drawing is always possible. Pure
 * data: no game classes, so the circuit logic can be weighed outside the game.
 */
public enum PuppetBlueprint {
	HUMANOID("humanoid"),
	BEE("bee");

	/** What a node does on the puppet, and the load of qi it draws through the lines that feed it. */
	public enum NodeKind {
		/** An arm, a leg, a wing: movement and blows. */
		LIMB(1),
		/** The head: targeting and awareness. */
		SENSE(1),
		/** The mouth or the stinger: where the core's art fires. */
		ART(2),
		/** The heart of command: it takes its owner's orders, and must be fed for the board to be sealed. */
		SEAL(1);

		public final int load;

		NodeKind(int load) {
			this.load = load;
		}
	}

	/**
	 * One node: what it is, its name's key part ("left_arm"), where it sits, and its pair (-1: none; the two nodes of a pair have the same
	 * number, and lie as each other's mirror).
	 */
	public record Node(NodeKind kind, String id, int x, int y, int pair) {}

	private final String id;

	PuppetBlueprint(String id) {
		this.id = id;
	}

	public String getId() { return id; }

	public static PuppetBlueprint byId(String id) {
		for (PuppetBlueprint blueprint : values()) if (blueprint.id.equals(id)) return blueprint;
		return null;
	}

	/**
	 * Its nodes on a board of {@code size} (odd, 9 or more), the centre {@code c}:
	 * <ul>
	 *   <li>the humanoid: its head at the top (c, 1) and its mouth below it (c, c - 2), its heart under the core (c, c + 2); its arms at the
	 *   sides a row above the core (1, c - 1) and (size - 2, c - 1); its legs at the foot (c - 2, size - 2) and (c + 2, size - 2);</li>
	 *   <li>the bee: its head at the top (c, 1) and its heart between head and core (c, c - 2); its stinger at the foot (c, size - 2); its
	 *   wings at the sides (1, c - 1) and (size - 2, c - 1); its legs below the core to either side (c - 2, c + 2) and (c + 2, c + 2).</li>
	 * </ul>
	 */
	public List<Node> nodes(int size) {
		int c = size / 2, far = size - 2;
		List<Node> nodes = new ArrayList<>();
		switch (this) {
			case HUMANOID -> {
				nodes.add(new Node(NodeKind.SENSE, "head", c, 1, -1));
				nodes.add(new Node(NodeKind.ART, "mouth", c, c - 2, -1));
				nodes.add(new Node(NodeKind.SEAL, "heart", c, c + 2, -1));
				nodes.add(new Node(NodeKind.LIMB, "left_arm", 1, c - 1, 0));
				nodes.add(new Node(NodeKind.LIMB, "right_arm", far, c - 1, 0));
				nodes.add(new Node(NodeKind.LIMB, "left_leg", c - 2, far, 1));
				nodes.add(new Node(NodeKind.LIMB, "right_leg", c + 2, far, 1));
			}
			case BEE -> {
				nodes.add(new Node(NodeKind.SENSE, "head", c, 1, -1));
				nodes.add(new Node(NodeKind.SEAL, "heart", c, c - 2, -1));
				nodes.add(new Node(NodeKind.ART, "stinger", c, far, -1));
				nodes.add(new Node(NodeKind.LIMB, "left_wing", 1, c - 1, 0));
				nodes.add(new Node(NodeKind.LIMB, "right_wing", far, c - 1, 0));
				nodes.add(new Node(NodeKind.LIMB, "left_leg", c - 2, c + 2, 1));
				nodes.add(new Node(NodeKind.LIMB, "right_leg", c + 2, c + 2, 1));
			}
		}
		return nodes;
	}

	/**
	 * Its joints and plates on a board of {@code size}: cells no line may cross, each with its mirror. The humanoid's shoulders (c - 2,
	 * c - 2) and its hips (c - 1, size - 3); the bee's wing roots (c - 2, c - 2) and the plates of its abdomen (c - 1, size - 3). A
	 * bigger board adds a second plate of each, a step further out (from 13 x 13).
	 */
	public List<int[]> blocked(int size) {
		int c = size / 2;
		List<int[]> cells = new ArrayList<>();
		pair(cells, size, c - 2, c - 2);
		pair(cells, size, c - 1, size - 3);
		if (size >= 13) {
			pair(cells, size, c - 3, c - 3);
			pair(cells, size, c - 3, size - 4);
		}
		return cells;
	}

	private static void pair(List<int[]> cells, int size, int x, int y) {
		cells.add(new int[] {x, y});
		cells.add(new int[] {size - 1 - x, y});
	}
}
