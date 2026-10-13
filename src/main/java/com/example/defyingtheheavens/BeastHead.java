package com.example.defyingtheheavens;

/**
 * A beast's head worn on a man's body, drawn in place of the human head and its hat layer (the client's BeastHeadModels: the
 * shape and the texture of each). Now the Ghost Gate's wardens, Ox-Head and Horse-Face (GhostCultivatorNpc.Role#beastHead);
 * made to be shared, so whatever else comes to wear one (a demon race, say: a tiger's head, a snake's) names it here and the
 * client draws it the same way. Saved and synced by id; add new heads at the end all the same.
 */
public enum BeastHead {
	/** A bull's head: broad and dark, a wide muzzle with a gold ring through it, great horns sweeping out and up. */
	OX("ox"),
	/** A horse's head: long-faced and chestnut, a white blaze down it, ears pricked, a black mane down the neck. */
	HORSE("horse");

	private final String id;

	BeastHead(String id) {
		this.id = id;
	}

	public String getId() { return id; }

	public static BeastHead byId(String id) {
		for (BeastHead head : values()) {
			if (head.id.equals(id)) return head;
		}
		return null;
	}
}
