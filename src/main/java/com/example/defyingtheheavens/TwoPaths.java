package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The two paths (see the README's "The two paths"): the <b>heaven path</b>, ordinary cultivation, and the <b>ghost path</b>, the
 * ghost methods' own (GhostMethods#ghostRank). Neither builds on the other, but one who walks both is the stronger for it:
 * <ul>
 *   <li><b>The body</b> takes its strength from the higher of the two ({@link #body}: health, harm, speed, armour, toughness and
 *       knockback resistance by that path's realm and stage, as CultivationStats has them), and the two paths' <b>harmony</b>
 *       adds to it, the more the closer they stand: {@link #HARMONY} for realms the same, one apart, two apart. What that body
 *       comes to is named as one path's rank would be ({@link #effective}, "effective cultivation"): a stage it equals, or
 *       "Quasi-" the stage or realm it is on its way to.</li>
 *   <li><b>Spells of both paths</b> (Spell#isDual), one for each realm from Foundation Building, need both paths at that realm,
 *       and draw on both pools, qi and ghostly qi.</li>
 *   <li><b>The true form</b>, the living's only. Once the ghost path reaches Core Formation (a Ghost King's might), or once
 *       they have reigned as a Ghost King among the dead (kept when born again: {@link #hasKingForm}), a living
 *       cultivator has a second form beside the human one: the Ghost King's, crowned and drawn in the dead's deeper light. The
 *       true form is the higher path's ({@link #trueForm}; the paths level, the human); Form Shift (Ability#FORM_SHIFT) takes the
 *       other. In a Ghost King's form one is seen as the dead are: outside the Netherworld only by those who perceive souls
 *       ({@link #inGhostForm}, SoulProjection#seenOnlyBySeers); in all else one is still among the living. Below that the living
 *       look human.</li>
 *   <li><b>The dead</b> are the dead, whatever their paths ({@link #look}): a ghost, or a Ghost King (GhostSystem#isKing). A
 *       Ghost King has two forms whatever its ghost path: its true one, the King's, and (Form Shift) its human form, in which it is
 *       seen as the living are ({@link #inHumanForm}); the rules it keeps are still the dead's.</li>
 * </ul>
 * A mortal's meridians are closed (until a Marrow Cleansing Elixir opens them), so a mortal has no ghost path, alive or dead:
 * its methods can't be taken up or cultivated (GhostMethods#refusal), and any it knows lie dormant ({@link #ghostRank}).
 */
public final class TwoPaths {
	/** The harmony of the two paths, added to the body: both in the same realm, one realm apart, two apart (and none further). */
	public static final double[] HARMONY = {0.30, 0.20, 0.10};

	// --- The paths ---

	/** The heaven path's rank (PlayerCultivation#rank), or -1 for a mortal. {@code wielded}: as Realm Suppression and pressure leave it. */
	public static int heavenRank(PlayerCultivation c, boolean wielded) {
		if (c.isMortal()) return -1;
		return wielded ? c.effectiveRank() : PlayerCultivation.rank(c.getRealm(), c.getStage());
	}

	/**
	 * The ghost path's rank, or -1: not begun, or a mortal's (alive or dead: the meridians closed, its methods lie dormant).
	 * {@code wielded}: as Realm Suppression and pressure leave it.
	 */
	public static int ghostRank(PlayerCultivation c, boolean wielded) {
		if (c.isMortal()) return -1;
		int rank = GhostMethods.ghostRank(c.netherworld());
		return wielded ? c.wielded(rank) : rank;
	}

	/** On both paths: a cultivator (not a mortal) whose ghost path has begun. */
	public static boolean walksBoth(PlayerCultivation c) {
		return heavenRank(c, false) >= 0 && ghostRank(c, false) >= 0;
	}

	/** How many realms apart the two paths stand (true cultivation), or -1 if not on both. */
	public static int realmGap(PlayerCultivation c) {
		if (!walksBoth(c)) return -1;
		int stages = Stage.values().length;
		return Math.abs(heavenRank(c, false) / stages - ghostRank(c, false) / stages);
	}

	/** The harmony of the two paths: a share added to the body ({@link #HARMONY} by {@link #realmGap}); 0 on one path. */
	public static double harmony(PlayerCultivation c) {
		int gap = realmGap(c);
		return gap < 0 || gap >= HARMONY.length ? 0 : HARMONY[gap];
	}

	/** Whether both paths stand at {@code realm} or beyond (true cultivation): a spell of both paths of that realm is in reach. */
	public static boolean bothReach(PlayerCultivation c, Realm realm) {
		int needed = PlayerCultivation.rank(realm, Stage.EARLY);
		return heavenRank(c, false) >= needed && ghostRank(c, false) >= needed;
	}

	// --- The body ---

	/**
	 * Where the body's strength comes from: the realm and stage it takes its bonuses at, the scale on them (Heaven's Favour and
	 * the harmony; a mortal's tempering share), whether the ghost path gives them, and the rank (for comparing the wielded body
	 * with the true one).
	 */
	public record Body(Realm realm, Stage stage, double scale, boolean ghostPath, int rank) {}

	/**
	 * The body (see the class notes): the higher path's realm, the harmony on top. A mortal on no path: a tempered mortal body
	 * growing toward Qi Refining Early's bonuses. {@code wielded}: as Realm Suppression and pressure leave the paths.
	 */
	public static Body body(PlayerCultivation c, boolean wielded) {
		int heaven = heavenRank(c, wielded), ghost = ghostRank(c, wielded);
		double scale = c.favourFactor() * (1 + harmony(c));
		int stages = Stage.values().length;
		// A Ghost Devil Body in its Ghost King's form is held to its ghost path's might, as Realm Suppression holds a realm (Physique).
		if (c.physique() == Physique.GHOST_DEVIL_BODY && inGhostForm(c)) {
			int rank = Math.max(0, ghost);
			return new Body(Realm.byIndex(rank / stages), Stage.byIndex(rank % stages), ghost < 0 ? 0.5 : scale, true, rank);
		}
		if (ghost > heaven) return new Body(Realm.byIndex(ghost / stages), Stage.byIndex(ghost % stages), scale, true, ghost);
		if (heaven < 0 && c.getBodyRefining() > 0) { // shattered meridians' body refining: a cultivator's body, a stage higher for each
			int rank = BodyRefining.rank(c.getBodyRefining());
			return new Body(Realm.byIndex(rank / stages), Stage.byIndex(rank % stages), 1.0, false, -1);
		}
		if (heaven < 0) return new Body(Realm.QI_REFINING, Stage.EARLY, c.getMortalStage().bodyShare(), false, -1);
		return new Body(Realm.byIndex(heaven / stages), Stage.byIndex(heaven % stages), scale, false, heaven);
	}

	// --- Effective cultivation: what the body's strength comes to on one path ---

	/** Within this share of a rank's might, a body counts as that rank, not past it (rounding). */
	public static final double EFFECTIVE_TOLERANCE = 0.005;

	/**
	 * A rank's might, by which bodies are weighed against each other: the geometric mean of its health and harm bonuses
	 * (CultivationStats), the two every realm has. Within a realm every bonus grows alike with the stage (Stage#getPowerMultiplier),
	 * so between two stages of one realm any bonus gives the same answer; between realms these two set it.
	 */
	public static double might(int rank) {
		int stages = Stage.values().length;
		Realm realm = Realm.byIndex(rank / stages);
		Stage stage = Stage.byIndex(rank % stages);
		return Math.sqrt(CultivationStats.maxHealth(realm, stage) * CultivationStats.attackDamage(realm, stage));
	}

	/**
	 * Effective cultivation: the highest rank (PlayerCultivation#rank) a cultivator on one path, with no Heaven's Favour, would need
	 * for a body as mighty as this one; whether this one is past it ({@code quasi}: "Quasi-" the rank after it); and how far toward
	 * that next rank, 0 to 1.
	 */
	public record Effective(int rank, boolean quasi, double toward) {}

	/**
	 * {@code c}'s effective cultivation ({@link Effective}): the body's might (the higher path's rank, x Heaven's Favour and the
	 * harmony, {@link #body}) set against the single path's ranks. Null for a mortal on no path. {@code wielded}: the body as
	 * Realm Suppression and pressure leave it (the stats it has now).
	 */
	public static Effective effective(PlayerCultivation c, boolean wielded) {
		Body body = body(c, wielded);
		if (body.rank() < 0) return null;
		double value = might(body.rank()) * body.scale();
		int last = PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
		int rank = body.rank();
		while (rank < last && might(rank + 1) <= value * (1 + EFFECTIVE_TOLERANCE)) rank++;
		boolean quasi = value > might(rank) * (1 + EFFECTIVE_TOLERANCE);
		double toward = rank >= last ? 0 : Math.max(0, Math.min(1, (value - might(rank)) / (might(rank + 1) - might(rank))));
		return new Effective(rank, quasi, toward);
	}

	/** Whether {@code c}'s body is mightier than its own rank alone (the harmony, Heaven's Favour): its effective cultivation is worth saying. */
	public static boolean strongerThanRank(PlayerCultivation c) {
		Effective effective = effective(c, true);
		return effective != null && (effective.quasi() || effective.rank() > body(c, true).rank());
	}

	/**
	 * An effective cultivation's name: the rank it equals ("Foundation Building - Late"); past a stage, "Quasi-" the next
	 * ("Foundation Building - Quasi-Late"); past Grand Perfection, "Quasi-" the next realm ("Quasi-Core Formation"); past the
	 * very last, beyond it.
	 */
	public static Component effectiveName(Effective effective) {
		int stages = Stage.values().length;
		Realm realm = Realm.byIndex(effective.rank() / stages);
		Stage stage = Stage.byIndex(effective.rank() % stages);
		if (!effective.quasi()) return PlayerCultivation.rankName(realm, stage);
		if (effective.rank() >= PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION)) {
			return Component.translatable(ModLang.EFFECTIVE_BEYOND, realm.getDisplayName(), stage.getDisplayName());
		}
		if (stage.isLast()) return Component.translatable(ModLang.EFFECTIVE_QUASI_REALM, realm.next().getDisplayName());
		return Component.translatable(ModLang.EFFECTIVE_QUASI_STAGE, realm.getDisplayName(), stage.next().getDisplayName());
	}

	/**
	 * {@link #effectiveName}, and for a Quasi- one how far toward the rank it names, in whole per cent (1 to 99): past a realm's Grand
	 * Perfection the next realm is two to four times as mighty, so one barely past it is "Quasi-Core Formation (1%)".
	 */
	public static Component effectiveLabel(Effective effective) {
		Component name = effectiveName(effective);
		if (!effective.quasi() || effective.rank() >= PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION)) return name;
		long percent = Math.max(1, Math.min(99, Math.round(effective.toward() * 100)));
		return Component.translatable(ModLang.EFFECTIVE_TOWARD, name, percent);
	}

	// --- The forms ---

	/** A cultivator's forms: the human, and (from a ghost path of Core Formation) the Ghost King's. */
	public enum Form {
		HUMAN("human"), GHOST_KING("ghost_king");

		private final String id;

		Form(String id) { this.id = id; }

		public Form other() { return this == HUMAN ? GHOST_KING : HUMAN; }

		public Component getDisplayName() { return Component.translatable(ModLang.FORM_PREFIX + id); }
	}

	/** How a player is drawn: as a man, as the dead's light, or as a Ghost King's (crowned). Synced (ModPackets#GHOST) by ordinal. */
	public enum Look { HUMAN, DEAD, KING }

	/**
	 * Whether {@code c} has the Ghost King's form (dormant while it is dead): its ghost path at Core Formation, a Ghost King's might;
	 * or, whatever its ghost path, having reigned as a Ghost King among the dead (NetherworldState#reignedKing), kept when born again.
	 */
	public static boolean hasKingForm(PlayerCultivation c) {
		if (c.physique() == Physique.GHOST_DEVIL_BODY && !c.isMortal()) return true; // from birth, as strong as its ghost path (Physique)
		if (c.netherworld().reignedKing() && !c.isMortal()) return true;
		return ghostRank(c, false) >= GhostSystem.KING_RANK;
	}

	/**
	 * Whether {@code c} has two forms to choose between now: the living with the Ghost King's form ({@link #hasKingForm}: their ghost
	 * path at Core Formation, or once a Ghost King among the dead); the dead, as Ghost Kings (GhostSystem#isKing), whatever their
	 * ghost path, a king by its heaven path having its human form to take as well. Common ghosts have none.
	 */
	public static boolean formsOpen(PlayerCultivation c) {
		return c.netherworld().ghost() ? GhostSystem.isKing(c) : hasKingForm(c);
	}

	/**
	 * The true form, with forms open: for the living the higher path's (true cultivation), the paths level the human (it lives);
	 * for a Ghost King, the King's (it is dead).
	 */
	public static Form trueForm(PlayerCultivation c) {
		if (c.netherworld().ghost()) return Form.GHOST_KING;
		return ghostRank(c, false) > heavenRank(c, false) ? Form.GHOST_KING : Form.HUMAN;
	}

	/** The form shown, with forms open: the true one, or the other while Form Shift is on. */
	public static Form shownForm(PlayerCultivation c) {
		Form form = trueForm(c);
		return c.isAbilityActive(Ability.FORM_SHIFT) ? form.other() : form;
	}

	/**
	 * How {@code c} is drawn. The dead as the dead, whatever their paths: a Ghost King crowned (or as a man while it shows its human
	 * form, Form Shift on), the rest as common ghosts. The living by their shown form if they have two, otherwise as men.
	 */
	public static Look look(PlayerCultivation c) {
		if (c.netherworld().ghost()) {
			if (!GhostSystem.isKing(c)) return Look.DEAD;
			return shownForm(c) == Form.HUMAN ? Look.HUMAN : Look.KING;
		}
		if (formsOpen(c)) return shownForm(c) == Form.GHOST_KING ? Look.KING : Look.HUMAN;
		return Look.HUMAN;
	}

	/** One of the living in a Ghost King's form: seen as the dead are (SoulProjection#seenOnlyBySeers). */
	public static boolean inGhostForm(PlayerCultivation c) {
		return !c.netherworld().ghost() && look(c) == Look.KING;
	}

	/** {@link #inGhostForm} for an online player (the server's record). */
	public static boolean inGhostForm(ServerPlayer p) {
		return inGhostForm(CultivationManager.get(p));
	}

	/**
	 * A Ghost King showing its human form: seen as the living are (by everyone, SoulProjection#seenOnlyBySeers), the mirror of
	 * the living in a Ghost King's form; in all else still one of the dead (a form is only a look and who sees it).
	 */
	public static boolean inHumanForm(PlayerCultivation c) {
		return c.netherworld().ghost() && look(c) == Look.HUMAN;
	}

	/** {@link #inHumanForm} for an online player (the server's record). */
	public static boolean inHumanForm(ServerPlayer p) {
		return inHumanForm(CultivationManager.get(p));
	}

	// --- Watching the paths (server) ---

	/**
	 * What each online player was last seen with: how they were drawn, whether they were dead, whether the living had the Ghost
	 * King's form ({@link #hasKingForm}; the dead are told theirs as Ghost Kings, GhostSystem#watchKing), their true form (null: one
	 * form), the spells of both paths in reach.
	 */
	private record Seen(Look look, boolean dead, boolean kingForm, Form form, EnumSet<Spell> both) {}

	private static final Map<UUID, Seen> SEEN = new HashMap<>();

	private static Seen see(PlayerCultivation c) {
		EnumSet<Spell> both = EnumSet.noneOf(Spell.class);
		for (Spell spell : Spell.values()) if (spell.isDual() && spell.isKnown(c)) both.add(spell);
		boolean dead = c.netherworld().ghost();
		return new Seen(look(c), dead, !dead && hasKingForm(c), formsOpen(c) ? trueForm(c) : null, both);
	}

	/** On joining: what they have now is simply theirs (nothing to tell them). */
	public static void onJoin(ServerPlayer p) {
		SEEN.put(p.getUUID(), see(CultivationManager.get(p)));
	}

	/**
	 * Once a second (GhostSystem#tick): the Ghost King's form come to them, or a new true form, told; a spell of both paths newly
	 * in reach told and put on the wheel; and if how they are drawn has changed, everyone who sees them told, and who that is asked
	 * again (a Ghost King's form is seen only by those who perceive souls, outside the Netherworld).
	 */
	public static void watch(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		Seen before = SEEN.get(p.getUUID());
		Seen now = see(c);
		SEEN.put(p.getUUID(), now);
		if (before == null) return;
		if (now.kingForm() && !before.kingForm()) {
			// The ghost path come to Core Formation, or a Ghost King born again: the form is theirs, to take while they live.
			p.sendSystemMessage((before.dead() ? Component.translatable(ModLang.MSG_FORMS_KEPT)
					: Component.translatable(ModLang.MSG_FORMS_OPEN, trueForm(c).getDisplayName())).withStyle(ChatFormatting.LIGHT_PURPLE));
			p.playNotifySound(SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 0.8f);
			if (!before.dead()) Journal.note(p, "two_forms", ModLang.JOURNAL_FORMS, Component.translatable(ModLang.JOURNAL_FORMS_TEXT));
		} else if (now.form() != null && before.form() != null && now.form() != before.form()) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_TRUE_FORM, now.form().getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		boolean spells = false;
		for (Spell spell : now.both()) {
			if (before.both().contains(spell)) continue;
			c.netherworld().addToWheel(spell);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_SPELL_BOTH_PATHS, spell.dualRealm().getDisplayName(), spell.getDisplayName())
					.withStyle(ChatFormatting.GOLD));
			p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
			spells = true;
		}
		if (spells) {
			CultivationManager.markDirty(p.server);
			Spells.sync(p);
		}
		if (now.look() != before.look()) {
			GhostSystem.broadcast(p);
			SoulProjection.refreshTracking(p);
		}
	}

	/**
	 * Form Shift switched (CultivationManager#toggleAbility): the form taken, at once, for all to see; and in a Ghost King's form,
	 * at once unseen by all who don't perceive souls (out of the Netherworld), seen again by them when it is put off.
	 */
	public static void shifted(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (!formsOpen(c)) return;
		Form form = shownForm(c);
		p.displayClientMessage(Component.translatable(ModLang.MSG_FORM_TAKEN, form.getDisplayName()), true);
		if (p.level() instanceof ServerLevel level) {
			boolean king = form == Form.GHOST_KING;
			level.sendParticles(king ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.06);
			level.playSound(null, p.getX(), p.getY(), p.getZ(), king ? SoundEvents.SOUL_ESCAPE : SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f,
					king ? 0.6f : 1.2f);
		}
		SEEN.put(p.getUUID(), see(c));
		GhostSystem.broadcast(p);
		SoulProjection.refreshTracking(p);
		// The body follows the form at once (a Ghost Devil Body's Ghost King form is held to its ghost path: TwoPaths#body).
		CultivationManager.refresh(p);
		if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
	}

	public static void forget(UUID id) {
		SEEN.remove(id);
	}

	public static void clear() {
		SEEN.clear();
	}

	private TwoPaths() {}
}
