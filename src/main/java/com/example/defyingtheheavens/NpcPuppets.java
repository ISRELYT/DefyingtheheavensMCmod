package com.example.defyingtheheavens;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Cultivators who are puppeteers (Puppetry; the design's "NPC rogues and sects field puppets"): one wandering cultivator in
 * {@link #PUPPETEER_IN} (NpcSpawner) comes with one or two puppets at its side, humanoids mostly, now and then a bee, of the frame its
 * realm makes (a Qi Refining or Foundation Building cultivator's Mortal, a Core Formation one's Spirit ... a Sacred Vessel one's
 * Immortal), their cores a little below its own, their circuits drawn as a puppeteer of its standing draws them (a Circuit Path of the rank
 * its realm gives it, of a build it favours, within its core's qi, on a frame of Normal work: the frame's flaws break a line here and
 * there). They follow it and fight what it fights;
 * with it gone (slain, or away for a minute) they fall apart where they stand, leaving their cores and some of their matter.
 */
public final class NpcPuppets {
	/** One wandering cultivator in this many is a puppeteer. */
	public static final int PUPPETEER_IN = 12;

	/** The frame a cultivator of {@code realm} makes: the grade whose realms it stands in, Mortal below, Immortal at most. */
	static ArtifactGrade gradeFor(Realm realm) {
		ArtifactGrade best = ArtifactGrade.MORTAL;
		for (ArtifactGrade grade : ArtifactGrade.values()) if (Puppetry.lowest(grade).ordinal() <= realm.ordinal()) best = grade;
		return best;
	}

	/** {@code npc} fields one or two puppets, beside it. */
	public static void field(ServerLevel level, CultivatorNpc npc, RandomSource random) {
		int rank = npc.getRank();
		Realm realm = CultivatorNpc.realmOf(rank);
		ArtifactGrade grade = gradeFor(realm);
		int least = PlayerCultivation.rank(Puppetry.lowest(grade), Stage.EARLY);
		// The rank of puppetry its standing gives it: a Mortal frame's maker Apprentice or Adept, an Immortal one's Grandmaster or Saint.
		int puppetry = Math.max(0, Math.min(4, grade.ordinal() - random.nextInt(2) + (grade.ordinal() >= 3 ? 1 : 0)));
		int count = 1 + (random.nextInt(3) == 0 ? 1 : 0);
		for (int k = 0; k < count; k++) {
			PuppetBlueprint blueprint = random.nextInt(5) == 0 ? PuppetBlueprint.BEE : PuppetBlueprint.HUMANOID;
			long seed = random.nextLong();
			CircuitBoard board = new CircuitBoard(blueprint, grade.ordinal(), CircuitBoard.flawsFor(Artifacts.Quality.NORMAL.ordinal()), seed);
			ItemStack core = DemonCoreItem.of(Math.max(least, rank - random.nextInt(4)), QiElement.five()[random.nextInt(5)], null);
			// A build its maker favours: a brawler's or (with an art) a caster's mostly, a bee a courier's, now and then an even one.
			CircuitBoard.Build build = random.nextInt(3) == 0 ? CircuitBoard.Build.BALANCED : blueprint == PuppetBlueprint.BEE ? CircuitBoard.Build.COURIER
					: board.hasArt() && random.nextBoolean() ? CircuitBoard.Build.CASTER : CircuitBoard.Build.BRAWLER;
			board.lay(board.circuitPath(puppetry, build, Puppetry.budget(grade, core), Puppetry.phaseOf(core)));
			PuppetEntity puppet = new PuppetEntity(blueprint == PuppetBlueprint.BEE ? ModEntities.PUPPET_BEE : ModEntities.PUPPET_HUMANOID, level);
			puppet.setup(grade, Artifacts.Quality.NORMAL, seed, Puppetry.strokes(board), core);
			puppet.setOwner(npc.getUUID(), npc.getName().getString(), false);
			double angle = random.nextDouble() * Math.PI * 2;
			puppet.moveTo(npc.getX() + Math.cos(angle) * 2, npc.getY() + (blueprint == PuppetBlueprint.BEE ? 1.5 : 0), npc.getZ() + Math.sin(angle) * 2,
					npc.getYRot(), 0);
			puppet.setHealth(puppet.getMaxHealth());
			level.addFreshEntity(puppet);
		}
	}

	private NpcPuppets() {}
}
