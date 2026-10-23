package com.example.defyingtheheavens;

/**
 * A beast's head worn on a man's body, drawn in place of the human head and its hat layer (the client's BeastHeadModels: the
 * shape, the texture and the animations of each, GeckoLib models). The Ghost Gate's wardens, Ox-Head and Horse-Face
 * (GhostCultivatorNpc.Role#beastHead), and the demon races (Bloodline#head), whose heads come with their tails. Saved and synced by
 * id; add new heads at the end all the same.
 */
public enum BeastHead {
	/** A bull's head: broad and dark, a wide muzzle with a gold ring through it, great horns sweeping out and up. */
	OX("ox"),
	/** A horse's head: long-faced and chestnut, a white blaze down it, ears pricked, a black mane down the neck. */
	HORSE("horse"),
	/** A tiger's: striped, round ears, whiskers and fangs; a long striped tail. */
	TIGER("tiger"),
	/** A lizard's: scaled, slit eyes, a forked tongue that flicks; a long scaled tail. */
	LIZARD("lizard"),
	/** A fox's: tall ears, a pointed white muzzle; a bushy tail, or as many as its realm gives (Bloodline#tails). */
	FOX("fox"),
	/** A dragon's: antlers, whiskers, a mane down the neck; a long scaled tail tufted at its end. */
	DRAGON("dragon");

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
