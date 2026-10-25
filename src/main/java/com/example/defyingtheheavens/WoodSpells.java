package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The wood element's spells (Spell, HeavenSpells): wood is weak to metal and strong against earth, and a wooden missile burns up
 * against a ward of fire (SpellAuras). Every number is the design's; every look its own (the client's spell renderers).
 * <ul>
 *   <li>Qi Refining: Wood Bullet (a stake grown in the air and shot), Emerald Healing Art (healing whatever the hunger, a green
 *       glow), Verdant Growth (a tree grows under your feet and lifts you high).</li>
 *   <li>Foundation Building: Life Force Drain and Life Force Gain (years of life drawn out of a being or a herb into you, or poured
 *       from you into it), Mysterious Bristle Prison (thorns rise and hold a being; it can strike its way out, at a price).</li>
 *   <li>Core Formation: Thousand Leaf Storm, Ancient Bark Skin.</li>
 *   <li>Nascent Soul: Thorn Spear Forest, Lotus of Rebirth.</li>
 *   <li>Heavenly Being: Heaven-Piercing Bamboo, Spring of Myriad Life.</li>
 *   <li>Four Axis: Myriad Vine Binding, World Tree (its vines seize all but the caster and drink their life into them).</li>
 *   <li>Dao Integration: Azure Dragon (the dragon of the east bursts from the land), Myriad Woods Return to the Dao (a spirit tree).</li>
 *   <li>Void Shattering: Spring Thunder of the Azure Emperor (a jade storm cloud's thunder: in the old lore thunder is wood's, the east's
 *       and spring's), Void Withering (the void-rooted tree's roots hang down out of a rift and drink the land's life).</li>
 * </ul>
 */
public final class WoodSpells {
	public static final double BULLET_SPEED = 2.0, BULLET_RANGE = 40;
	public static final float BULLET_DAMAGE = 5;
	public static final int EMERALD_TICKS = 10 * 20;
	public static final int GROWTH_HEIGHT = 14, GROWTH_TICKS = 40, GROWTH_LASTS = 60 * 20;
	public static final double LIFE_RANGE = 12, LIFE_LEASH = 16;
	public static final int LIFE_TICKS = 6 * 20;
	public static final float DRAIN_PER_SECOND = 1.5f, PLANT_DRAIN_HEAL = 1.0f, GAIN_COST = 1.0f, GAIN_HEAL = 2.0f, GAIN_FLOOR = 4.0f;
	public static final int DRAIN_YEARS = 60, GAIN_YEARS = 120;
	/**
	 * Life Force's lifespan (the user's ask, 2026-10-21): Gain gives a herb or a fruit the caster's own years, this many a second (each
	 * second's {@link #GAIN_YEARS} of growth), never the last Lifespan#WARN_YEARS of them; Drain draws this many a second out of another
	 * player's years while it lands on them, the drawer keeping Lifespan#DRAWN_KEPT of them (Lifespan#draw).
	 */
	public static final double GAIN_LIFE_YEARS = 10, DRAIN_LIFE_YEARS = 10;
	/**
	 * Life Force's qi (its mode {@link LifeMode#QI}): drawn out of another player's pool this much a second, {@link #QI_KEPT} of it the drawer's;
	 * out of a being that keeps none, its life burnt into qi, {@link #QI_PER_LIFE} for each of its health taken; given, the caster's own qi this
	 * much a second, to another's pool, or to a being that keeps none as life ({@link #LIFE_PER_QI} of its health for each).
	 */
	public static final double DRAIN_QI = 40, QI_KEPT = 0.6, QI_PER_LIFE = 8, GAIN_QI = 40, LIFE_PER_QI = 0.05;

	/**
	 * What Life Force Drain and Gain move between a being and their caster (the user's ask, 2026-10-25: "Add the wood life force give and take
	 * ability modes, qi, hp and years of life"), chosen in the Spells tab and kept: its health (as they always did), its qi, or its years of
	 * life (only a player's: only a player keeps a lifespan). A herb or a fruit is given, or drawn on for, its years whatever the mode.
	 */
	public enum LifeMode {
		HEALTH(ModLang.LIFE_MODE_HEALTH, 0xFF8A80), QI(ModLang.LIFE_MODE_QI, 0x80D8FF), YEARS(ModLang.LIFE_MODE_YEARS, 0xFFE082);

		private final String key;
		private final int colour;

		LifeMode(String key, int colour) {
			this.key = key;
			this.colour = colour;
		}

		/** Its name's lang key, and its switch's colour (0xRRGGBB). */
		public String key() { return key; }
		public int colour() { return colour; }

		public LifeMode next() { return values()[(ordinal() + 1) % values().length]; }

		public static LifeMode byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : HEALTH;
		}

		public static LifeMode byName(String name) {
			for (LifeMode mode : values()) if (mode.name().equals(name)) return mode;
			return HEALTH;
		}
	}

	/** The Spells tab's switch (C2S life_mode): what {@code p}'s Life Force moves now, kept, and told. */
	public static void setLifeMode(ServerPlayer p, LifeMode mode) {
		CultivationManager.get(p).netherworld().setLifeMode(mode);
		CultivationManager.markDirty(p.server);
		Spells.sync(p);
		HeavenSpells.tell(p, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_MODE, net.minecraft.network.chat.Component.translatable(mode.key())));
	}

	/** What {@code caster}'s Life Force moves: their mode, a player's; a cultivator's always health. */
	static LifeMode lifeMode(LivingEntity caster) {
		return caster instanceof ServerPlayer p ? CultivationManager.get(p).netherworld().lifeMode() : LifeMode.HEALTH;
	}
	public static final double PRISON_RANGE = 20;
	public static final int PRISON_TICKS = 4 * 20, PRISON_STRIKES = 3;
	public static final float PRISON_DAMAGE = 1.5f, PRISON_THORNS = 2;
	public static final int LEAVES = 16;
	public static final double LEAF_SPEED = 1.6, LEAF_RANGE = 36;
	public static final float LEAF_DAMAGE = 2.5f;
	public static final int BARK_TICKS = 20 * 20;
	public static final double SPEARS_RANGE = 32, SPEARS_RADIUS = 6;
	public static final float SPEARS_DAMAGE = 35;
	public static final int SPEARS_ROOT = 2 * 20;
	public static final double LOTUS_RADIUS = 10;
	public static final int LOTUS_TICKS = 10 * 20;
	public static final float LOTUS_HEAL = 3;
	/** The ground each bamboo bursts out of, broken {@link #BAMBOO_BREAK} round (4, by its realm: Devastation#sized). */
	public static final double BAMBOO_RANGE = 48, BAMBOO_RADIUS = 24, BAMBOO_BREAK = Devastation.sized(Realm.HEAVENLY_BEING, 5);
	public static final int BAMBOO_TARGETS = 6;
	public static final float BAMBOO_DAMAGE = 70;
	public static final double SPRING_RADIUS = 32;
	public static final float SPRING_BURST = 40, SPRING_HEAL = 4;
	public static final int SPRING_TICKS = 10 * 20;
	public static final double VINES_RANGE = 64, VINES_RADIUS = 40;
	public static final int VINES_TARGETS = 12, VINES_TICKS = 5 * 20;
	public static final float VINES_DAMAGE = 22;
	/** The tree's vines reach {@link #TREE_REACH}; the land splits round its roots out to {@link #TREE_FISSURES} (40, by its realm). */
	public static final double TREE_RANGE = 24, TREE_REACH = 32, TREE_FISSURES = Devastation.sized(Realm.FOUR_AXIS, 40);
	public static final int TREE_TICKS = 12 * 20;
	public static final float TREE_DRAIN = 10;
	/**
	 * Azure Dragon: how far off it rises (82), whom it catches (within {@link #DRAGON_RADIUS}, 18) and how hard, how long it carries them
	 * and how many, and the fissures it tears (out to 51 and 8 deep); its flight is DragonFlight's. Its reach and size by its realm
	 * (HeavenSpells#span), its wounds too (Devastation#sized).
	 */
	public static final double DRAGON_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 64), DRAGON_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 14);
	public static final float DRAGON_DAMAGE = 480;
	public static final int DRAGON_HOLD = 40, DRAGON_FISSURES = 14, DRAGON_CARRIES = 12;
	/** Those it catches up are held on its body this far back from its head and on, this much further apart (blocks, for each of its size). */
	public static final double DRAGON_CARRY_FROM = 5, DRAGON_CARRY_EVERY = 2.2;
	public static final double DRAGON_REACH = Devastation.sized(Realm.DAO_INTEGRATION, 40), DRAGON_FISSURE_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 6);
	/** Myriad Woods Return to the Dao: the woods' reach round the spirit tree (41, by its realm), how long they hold, what each foe is drained a second. */
	public static final double WOODS_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 32);
	public static final int WOODS_TICKS = 15 * 20;
	public static final float WOODS_DRAIN = 40;
	/**
	 * Spring Thunder of the Azure Emperor: where its cloud gathers (up to {@link #THUNDER_RANGE}, 118 by its realm), how high over the land
	 * (40), how far round it reaches ({@link #THUNDER_RADIUS}, 26); each bolt's harm and how long it roots; how long the cloud takes to
	 * gather, its volleys and how far apart, the bolts loosed at the land besides in each, and the little craters those leave (by its realm).
	 */
	public static final double THUNDER_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), THUNDER_HIGH = HeavenSpells.span(Realm.VOID_SHATTERING, 22),
			THUNDER_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 14), THUNDER_CRATER = Devastation.sized(Realm.VOID_SHATTERING, 1.5),
			THUNDER_CRATER_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 1);
	public static final float THUNDER_DAMAGE = 220;
	public static final int THUNDER_GATHER = 30, THUNDER_VOLLEYS = 6, THUNDER_EVERY = 20, THUNDER_ROOT = 20, THUNDER_STRAYS = 2;
	/** Spring thunder's jade (its bolts: TribulationLightning#tint). */
	public static final int THUNDER_JADE = 0x5CF0A0;
	/**
	 * Void Withering: where (up to {@link #WITHER_RANGE}, 118), how high over the land its rift opens (33) and how long it is, how far round
	 * the void tree's roots reach and drink (52; 37 before 2026-10-24, the user's ask for longer roots), how far over the rift and down into
	 * the land under it ({@link #WITHER_OVER}, {@link #WITHER_BELOW}), how high they haul what they seize (9, by its realm) and how many they
	 * hold at once; what each foe is drained a second; when the roots come out of the rift (it open), when they begin to seize and drink, how
	 * long they drink, and how long a seizing root takes to lash out to its catch (TendrilRenderer#LASH: then it hauls).
	 */
	public static final double WITHER_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), WITHER_HIGH = HeavenSpells.span(Realm.VOID_SHATTERING, 18),
			WITHER_RIFT = HeavenSpells.span(Realm.VOID_SHATTERING, 13), WITHER_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 28),
			WITHER_LIFT = HeavenSpells.span(Realm.VOID_SHATTERING, 5), WITHER_OVER = 8, WITHER_BELOW = HeavenSpells.span(Realm.VOID_SHATTERING, 22);
	public static final float WITHER_DRAIN = 200;
	public static final int WITHER_GRASPS = 8, WITHER_ROOTS = 10, WITHER_OPENS = 20, WITHER_TICKS = 8 * 20, WITHER_LASH = 7;
	/**
	 * Heaven-Swallowing Gourd: where (up to {@link #GOURD_RANGE}, 136 by its realm), how high its mouth hangs over the land (19), how far round
	 * it draws (30), how near its mouth what it draws is swallowed (5); what the swallowed are drained a second, and the harm they are spat out
	 * with; the land it tears up into itself (a bowl 19 round and 4 deep, by its realm); how long it takes to come down, how long it draws, and
	 * how long it has after it spits (ticks).
	 */
	public static final double GOURD_RANGE = HeavenSpells.span(Realm.SACRED_VESSEL, 64), GOURD_HIGH = HeavenSpells.span(Realm.SACRED_VESSEL, 9),
			GOURD_RADIUS = HeavenSpells.span(Realm.SACRED_VESSEL, 14), GOURD_MOUTH = HeavenSpells.span(Realm.SACRED_VESSEL, 2.5),
			GOURD_STRIP = Devastation.sized(Realm.SACRED_VESSEL, 9), GOURD_STRIP_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 2);
	public static final float GOURD_DRAIN = 450, GOURD_SPIT = 900;
	public static final int GOURD_DESCENDS = 20, GOURD_TICKS = 5 * 20, GOURD_AFTER = 16;
	/**
	 * Seven-Treasure Bodhi Branch: how far ahead its sweep reaches (85 by its realm), how wide an arc it sweeps across the caster's front
	 * (degrees), how near the caster it leaves the land (and them) be; its harm; how deep it scours the land (6 at the arc's middle, by its realm);
	 * how long it takes to form at the caster's side, how long its sweep takes, and how long it has after (its treasures' light).
	 */
	public static final double BRANCH_REACH = HeavenSpells.span(Realm.SACRED_VESSEL, 40), BRANCH_ARC = 150, BRANCH_SPARE = 6,
			BRANCH_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 3);
	public static final float BRANCH_DAMAGE = 3500;
	public static final int BRANCH_FORMS = 22, BRANCH_SWEEP = 20, BRANCH_AFTER = 24;
	/** The seven treasures' colours (gold, silver, lapis, crystal, pearl, agate, coral): the light they burst into as the branch has swept. */
	public static final int[] TREASURES = {0xFFD54A, 0xE6EEF4, 0x2F5BD9, 0xBFF4FF, 0xFFF6E8, 0xD8502E, 0xFF8A9A};
	/**
	 * Seven-Treasure Bodhi Branch, the tree it grows into (Branch, 2026-10-25): its height (32 by its realm), how far round its roots seize (30),
	 * how wide a treasure bursts where it falls; a treasure's harm; how long the tree takes to grow, how long it stands before its treasures
	 * fall, how far apart they fall, and how long it takes to shed its leaves and go (ticks).
	 */
	public static final double BODHI_HEIGHT = HeavenSpells.span(Realm.SACRED_VESSEL, 15), BODHI_RADIUS = HeavenSpells.span(Realm.SACRED_VESSEL, 14),
			BODHI_BURST = HeavenSpells.span(Realm.SACRED_VESSEL, 2.6);
	public static final float BODHI_TREASURE = 650;
	public static final int BODHI_GROWS = 34, BODHI_HOLDS = 14, BODHI_EVERY = 7, BODHI_SHEDS = 26;

	/**
	 * Where the bodhi tree's treasure {@code i} (of the seven) hangs from its foot, the tree {@code height} tall: under the end of one of its
	 * boughs, round its crown (BlockMasses lays its boughs out to them, and draws them hanging there; its spell lets them fall from there).
	 */
	public static Vec3 treasureAt(int i, double height) {
		double a = Math.PI * 2 * i / 7 + 0.3, r = height * (i % 2 == 0 ? 0.34 : 0.27), y = height * (0.52 + 0.05 * (i % 3));
		return new Vec3(Math.cos(a) * r, y, Math.sin(a) * r);
	}
	/**
	 * Seed of Withering Rebirth: how far it flies for the being looked at (154 by its realm); what it drinks a second out of what it roots in,
	 * and the share of it mending its caster; its bloom's harm to what it rooted in, and to every other foe within its burst (12 round); how
	 * far round the land flourishes where it blooms (29, by its realm); how long it flies, how long it roots, how long it holds in bloom, and
	 * how long it withers (ticks).
	 */
	public static final double SEED_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), SEED_BURST = HeavenSpells.span(Realm.ENTERING_NIRVANA, 5),
			SEED_FLOURISH = Devastation.sized(Realm.ENTERING_NIRVANA, 12);
	public static final float SEED_DRAIN = 500, SEED_MENDS = 0.5f, SEED_BLOOM = 2500, SEED_BLOOM_AROUND = 1200;
	public static final int SEED_FLIGHT = 14, SEED_ROOTS = 5 * 20, SEED_HOLDS = 30, SEED_WITHERS = 26;
	/**
	 * The seed shot (Seed: the user's ask, 2026-10-25): how fast it flies (blocks a tick), how much it falls as it goes (a tick, each tick), how
	 * long it may fly before it withers in the air, how far round it its roots reach once planted (14 by its realm), what each of them drinks a
	 * second, and what a being struck in its flight takes as it strikes.
	 */
	public static final double SEED_SPEED = 2.0, SEED_FALL = 0.035, SEED_REACH = HeavenSpells.span(Realm.ENTERING_NIRVANA, 6);
	public static final int SEED_FLIES = 80;
	public static final float SEED_DRAIN_EACH = 300, SEED_STRUCK = 600;
	/** The flower of rebirth's blush: the light of its bloom. */
	public static final int REBIRTH_BLUSH = 0xFFC0DA;
	/**
	 * Withering and Flourishing: where (154 by its realm), how far round the wheel of the seasons reaches (34); what every foe within is drained
	 * of a second, and the share of all of it mending its caster and their own within; its last turn's harm, and its share mending its caster;
	 * how long it grows, how long it turns, and how long it has after (ticks).
	 */
	public static final double SEASONS_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), SEASONS_RADIUS = HeavenSpells.span(Realm.ENTERING_NIRVANA, 14);
	public static final float SEASONS_DRAIN = 450, SEASONS_MENDS = 0.3f, SEASONS_TURN = 3000, SEASONS_TURN_MENDS = 0.2f;
	public static final int SEASONS_GROWS = 24, SEASONS_TICKS = 8 * 20, SEASONS_AFTER = 24;
	/**
	 * Primal Wind of Creation: where it forms (218 by its realm); how far round it draws foes in (41), how far round its funnel holds and whirls
	 * them (17), how high it carries them (24), how fast it wanders on along its caster's look (blocks a tick); its harm each time it strikes, and
	 * as it flings them out at its end; the share of all of it mending its caster and their own near; how long it forms, how long it whirls, how
	 * often it strikes, and how long it dies away (ticks).
	 */
	public static final double WIND_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 64), WIND_PULL = HeavenSpells.span(Realm.TRUE_IMMORTAL, 12),
			WIND_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 5), WIND_HIGH = HeavenSpells.span(Realm.TRUE_IMMORTAL, 7), WIND_SPEED = 0.35;
	public static final float WIND_DAMAGE = 3000, WIND_FLING = 12000, WIND_MENDS = 0.05f;
	public static final int WIND_FORMS = 20, WIND_TICKS = 8 * 20, WIND_EVERY = 10, WIND_FADES = 16;
	/**
	 * First Seed of Creation: where it falls (218 by its realm), how far round its dome of life reaches (48: its model's); what it draws out of
	 * every foe within each time it drinks, and the share of all of it mending its caster and their own within; its bloom's harm to every foe
	 * within; how long it grows, how long it drinks, how often, how long it blooms, and how long it has after (ticks).
	 */
	public static final double CREATION_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 64), CREATION_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 14);
	public static final float CREATION_DRAIN = 2500, CREATION_MENDS = 0.3f, CREATION_BLOOM = 45000;
	public static final int CREATION_GROWS = 28, CREATION_TICKS = 6 * 20, CREATION_EVERY = 10, CREATION_BLOOMS = 24, CREATION_AFTER = 20;
	/** Creation's light: the green-gold of the first life. */
	public static final int CREATION_LIGHT = 0xB8FFB0;

	private static final UUID BOUND_ID = UUID.fromString("2b8f4e17-91c3-4d6a-b5e0-7a3c1f9d2e64");

	static boolean canCast(LivingEntity p, Spell spell) {
		return switch (spell) {
			case LIFE_FORCE_DRAIN, LIFE_FORCE_GAIN -> {
				LifeTarget target = lifeTarget(p, spell == Spell.LIFE_FORCE_DRAIN, true);
				yield target != LifeTarget.TOLD && (target != null || HeavenSpells.refuse(p));
			}
			case MYSTERIOUS_BRISTLE_PRISON -> HeavenSpells.aimed(p, PRISON_RANGE) != null || HeavenSpells.refuse(p);
			case RETURN_TO_THE_BOUGH -> boughTarget(p, true) != null;
			case VERDANT_GROWTH -> p.onGround() || HeavenSpells.refuse(p);
			// Nothing to seize where you look: refused, and nothing paid (it was 500 and 1200 qi spent on nothing).
			case HEAVEN_PIERCING_BAMBOO -> !marksNear(p, BAMBOO_RANGE, BAMBOO_RADIUS, 1).isEmpty() || HeavenSpells.refuse(p);
			case MYRIAD_VINE_BINDING -> !marksNear(p, VINES_RANGE, VINES_RADIUS, 1).isEmpty() || HeavenSpells.refuse(p);
			default -> true;
		};
	}

	static void cast(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		switch (spell) {
			case WOOD_BULLET -> bullet(p, spell);
			case EMERALD_HEALING_ART -> {
				SpellAuras.put(p, SpellAura.EMERALD_HEALING, EMERALD_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
				HeavenSpells.sound(level, p.position(), SoundEvents.AZALEA_LEAVES_PLACE, 1.0f, 1.0f);
			}
			case VERDANT_GROWTH -> HeavenSpells.linger(new Growth(p));
			case LIFE_FORCE_DRAIN -> life(p, spell, true);
			case LIFE_FORCE_GAIN -> life(p, spell, false);
			case MYSTERIOUS_BRISTLE_PRISON -> prison(p, spell);
			case RETURN_TO_THE_BOUGH -> {
				BoughTarget target = boughTarget(p, false);
				if (target != null) HeavenSpells.linger(new Bough(p, target));
			}
			case THOUSAND_LEAF_STORM -> HeavenSpells.linger(new LeafStorm(p, spell));
			case ANCIENT_BARK_SKIN -> {
				SpellAuras.put(p, SpellAura.ANCIENT_BARK_SKIN, BARK_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.WOOD_PLACE, 1.5f, 0.5f);
				HeavenSpells.sound(level, p.position(), SoundEvents.ROOTED_DIRT_PLACE, 1.0f, 0.6f);
				SpellFx.send(level, SpellFx.Kind.SPLINTERS, HeavenSpells.middle(p), Vec3.ZERO, 1.2f, 0);
			}
			case THORN_SPEAR_FOREST -> spears(p, spell);
			case LOTUS_OF_REBIRTH -> HeavenSpells.linger(new Lotus(p, spell));
			case HEAVEN_PIERCING_BAMBOO -> bamboo(p, spell);
			case SPRING_OF_MYRIAD_LIFE -> HeavenSpells.linger(new Spring(p));
			case MYRIAD_VINE_BINDING -> vines(p, spell);
			case WORLD_TREE -> {
				HeavenSpells.endLingering(p.getUUID(), WorldTree.class);
				HeavenSpells.linger(new WorldTree(p, spell));
			}
			case AZURE_DRAGON -> {
				LivingEntity aimed = HeavenSpells.aimed(p, DRAGON_RANGE);
				HeavenSpells.linger(new Dragon(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, DRAGON_RANGE)));
			}
			case MYRIAD_WOODS_RETURN -> {
				HeavenSpells.endLingering(p.getUUID(), Woods.class);
				HeavenSpells.linger(new Woods(p, spell));
			}
			case SPRING_THUNDER_OF_THE_AZURE_EMPEROR -> {
				LivingEntity aimed = HeavenSpells.aimed(p, THUNDER_RANGE);
				HeavenSpells.linger(new Thunder(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, THUNDER_RANGE)));
			}
			case VOID_WITHERING -> {
				HeavenSpells.endLingering(p.getUUID(), Withering.class);
				LivingEntity aimed = HeavenSpells.aimed(p, WITHER_RANGE);
				HeavenSpells.linger(new Withering(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, WITHER_RANGE)));
			}
			case HEAVEN_SWALLOWING_GOURD -> {
				LivingEntity aimed = HeavenSpells.aimed(p, GOURD_RANGE);
				HeavenSpells.linger(new Gourd(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, GOURD_RANGE)));
			}
			case SEVEN_TREASURE_BODHI_BRANCH -> {
				HeavenSpells.endLingering(p.getUUID(), Branch.class);
				HeavenSpells.linger(new Branch(p, spell));
			}
			case SEED_OF_WITHERING_REBIRTH -> HeavenSpells.linger(new Seed(p, spell)); // shot along the look, rooting in what it strikes
			case WITHERING_AND_FLOURISHING -> {
				HeavenSpells.endLingering(p.getUUID(), Seasons.class);
				LivingEntity aimed = HeavenSpells.aimed(p, SEASONS_RANGE);
				HeavenSpells.linger(new Seasons(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SEASONS_RANGE)));
			}
			case PRIMAL_WIND_OF_CREATION -> {
				LivingEntity aimed = HeavenSpells.aimed(p, WIND_RANGE);
				HeavenSpells.linger(new Wind(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, WIND_RANGE)));
			}
			case FIRST_SEED_OF_CREATION -> {
				HeavenSpells.endLingering(p.getUUID(), Creation.class);
				LivingEntity aimed = HeavenSpells.aimed(p, CREATION_RANGE);
				HeavenSpells.linger(new Creation(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, CREATION_RANGE)));
			}
			default -> {}
		}
	}

	// --- Binding a being where it stands ---

	/**
	 * Holds {@code e} at {@code at} for {@code ticks}: it can't walk away (pulled back if it strays), its walking speed gone. A great
	 * beast (HeavenSpells#greatBeast) is only slowed.
	 */
	public static final class Bound extends HeavenSpells.Timed {
		public final LivingEntity target;
		private final Vec3 at;

		Bound(LivingEntity caster, LivingEntity target, Vec3 at, int ticks) {
			super(caster, ticks);
			this.target = target;
			this.at = at;
			AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.removeModifier(BOUND_ID);
				speed.addTransientModifier(new AttributeModifier(BOUND_ID, "Bound by wood", HeavenSpells.greatBeast(caster, target) ? -0.6 : -1.0,
						AttributeModifier.Operation.MULTIPLY_TOTAL));
			}
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			if (!HeavenSpells.greatBeast(caster, target) && target.position().distanceToSqr(at) > 0.09) {
				HeavenSpells.teleport(target, new Vec3(at.x, Math.min(target.getY(), at.y + 0.5), at.z));
			}
			HeavenSpells.still(target);
			return true;
		}

		@Override
		public void end() {
			AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) speed.removeModifier(BOUND_ID);
		}
	}

	/** Binds {@code target} where it stands for {@code ticks} (an earlier binding of this caster's on it ends first). */
	static Bound bind(LivingEntity p, LivingEntity target, int ticks) {
		for (Bound bound : HeavenSpells.lingering(p.getUUID(), Bound.class)) {
			if (bound.target == target) HeavenSpells.stop(bound);
		}
		Bound bound = new Bound(p, target, target.position(), ticks);
		HeavenSpells.linger(bound);
		return bound;
	}

	// --- Qi Refining ---

	private static void bullet(LivingEntity p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.WOOD_STAKE, 1.0f, 6, new Vec3(0.35, -0.25, 1.1), BULLET_SPEED, BULLET_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						HeavenSpells.strike(p, target, BULLET_DAMAGE, spell, missile);
						HeavenSpells.push(missile.position().subtract(missile.getDeltaMovement()), target, 0.4);
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.SPLINTERS, missile.position(), missile.getDeltaMovement(), 0.6f, 0);
						HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.WOOD_HIT, 1.0f, 1.2f);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.SPLINTERS, hit.getLocation(), missile.getDeltaMovement(), 0.6f, 0);
						HeavenSpells.sound(HeavenSpells.level(p), hit.getLocation(), SoundEvents.WOOD_BREAK, 0.8f, 1.2f);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.ARROW_SHOOT, 0.8f, 0.6f);
					}
				});
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.AZALEA_PLACE, 1.0f, 1.3f);
	}

	/**
	 * Verdant Growth: a tree pushes up out of the ground under its caster, lifting them {@link #GROWTH_HEIGHT} blocks over
	 * {@link #GROWTH_TICKS}, and spreads a crown round them at the top. It stands {@link #GROWTH_LASTS}, then withers away (its
	 * blocks are the spell's: they drop nothing).
	 */
	public static final class Growth extends HeavenSpells.Timed {
		private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
		private static final BlockState LEAVES = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		private final BlockPos base;
		private int placed;
		private boolean crowned;

		Growth(LivingEntity caster) {
			super(caster, GROWTH_TICKS + 20);
			this.base = caster.blockPosition();
			sound(caster.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.2f, 0.7f);
			SpellFx.send(level, SpellFx.Kind.LEAVES, caster.position(), Vec3.ZERO, 1.0f, 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age > GROWTH_TICKS) {
				if (!crowned) crown();
				return age < GROWTH_TICKS + 20;
			}
			// The height the trunk has reached by now, eased out at the top.
			double t = age / (double) GROWTH_TICKS;
			double height = GROWTH_HEIGHT * (1 - Math.pow(1 - t, 2));
			int want = (int) Math.floor(height);
			while (placed < want) {
				BlockPos pos = base.above(placed);
				if (!level.getBlockState(pos).canBeReplaced() && !level.getBlockState(pos).is(LOG.getBlock())) break;
				if (!HeavenSpells.placeTemporary(level, pos, LOG, GROWTH_LASTS) && !level.getBlockState(pos).is(LOG.getBlock())) break;
				SpellBlock.show(level, LOG, Vec3.atBottomCenterOf(pos).subtract(0, 1, 0), new Vec3(0, 1, 0), 0, 3, SpellBlock.Motion.SLIDE, 0);
				placed++;
				if (placed % 3 == 0) sound(Vec3.atCenterOf(pos), SoundEvents.WOOD_PLACE, 0.8f, 0.6f + placed * 0.04f);
			}
			// The caster rides the top of the trunk: lifted to just over it.
			double top = base.getY() + placed;
			if (caster.getY() < top + 0.05) {
				HeavenSpells.move(caster, new Vec3(caster.getDeltaMovement().x * 0.3, Math.min(0.6, top + 0.1 - caster.getY() + 0.08), caster.getDeltaMovement().z * 0.3));
				caster.resetFallDistance();
			}
			if (placed < want) return age < GROWTH_TICKS; // blocked overhead: it stops growing there
			return true;
		}

		/** The crown: a round head of blossoming leaves about the trunk's top, the caster standing on it. */
		private void crown() {
			crowned = true;
			BlockPos top = base.above(Math.max(1, placed));
			int r = 3;
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = -2; dy <= 1; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						double d = dx * dx + dz * dz + (dy + 0.5) * (dy + 0.5) * 2.2;
						if (d > r * r + 0.5 || level.random.nextInt(10) == 0 && d > 4) continue;
						BlockPos pos = top.offset(dx, dy, dz);
						if (dx == 0 && dz == 0 && dy < 0) continue; // the trunk
						if (HeavenSpells.placeTemporary(level, pos, LEAVES, GROWTH_LASTS)) {
							SpellBlock.show(level, LEAVES, Vec3.atBottomCenterOf(pos), Vec3.ZERO, level.random.nextInt(6), 6, SpellBlock.Motion.GROW, 0);
						}
					}
				}
			}
			if (HeavenSpells.placeTemporary(level, top.below(), LOG, GROWTH_LASTS)) placed++;
			sound(Vec3.atCenterOf(top), SoundEvents.AZALEA_LEAVES_PLACE, 1.5f, 0.8f);
			SpellFx.send(level, SpellFx.Kind.LEAVES, Vec3.atCenterOf(top), Vec3.ZERO, 2.0f, 0);
			Spells.safeFall(caster, 60);
		}
	}

	// --- Foundation Building ---

	/**
	 * The aging herb or fruit {@code p} looks at within reach (a Cultivation Fruit, a ginseng), or null. Herbs and fruit can be
	 * walked through (no collision), so the look is traced against the blocks' outlines, as the crosshair picks them.
	 */
	static BlockPos plantLookedAt(LivingEntity p) {
		Vec3 from = p.getEyePosition();
		Vec3 to = from.add(p.getViewVector(1.0f).scale(LIFE_RANGE));
		BlockHitResult hit = p.level().clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.OUTLINE,
				net.minecraft.world.level.ClipContext.Fluid.NONE, p));
		if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return null;
		BlockEntity be = p.level().getBlockEntity(hit.getBlockPos());
		return be instanceof CultivationFruitBlockEntity || be instanceof GinsengBlockEntity ? hit.getBlockPos() : null;
	}

	private static int plantAge(ServerLevel level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof CultivationFruitBlockEntity fruit) return fruit.age();
		if (be instanceof GinsengBlockEntity ginseng) return ginseng.age();
		return -1;
	}

	private static void setPlantAge(ServerLevel level, BlockPos pos, int years) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof CultivationFruitBlockEntity fruit) fruit.setAge(years);
		else if (be instanceof GinsengBlockEntity ginseng) ginseng.setAge(years);
	}

	/** The age of an aging fruit or herb held ({@code stack}: a Cultivation Fruit, a ginseng and its kind), or -1 for anything else. */
	static int heldAge(net.minecraft.world.item.ItemStack stack) {
		if (stack.getItem() instanceof CultivationFruitItem) return CultivationFruitItem.age(stack);
		if (stack.getItem() instanceof GinsengItem) return GinsengItem.age(stack);
		return -1;
	}

	private static void setHeldAge(net.minecraft.world.item.ItemStack stack, int years) {
		if (stack.getItem() instanceof CultivationFruitItem) stack.getOrCreateTag().putInt("FruitAge", FruitAge.clamp(years));
		else if (stack.getItem() instanceof GinsengItem) stack.getOrCreateTag().putInt("HerbAge", FruitAge.clamp(years));
	}

	/** Whether {@code pos} lies in a treasury whose seal holds (a sect's, or a Ghost King's palace's): nothing in it is to be touched. */
	private static boolean sealedTreasury(ServerLevel level, BlockPos pos) {
		return SectManager.sealedTreasuryAt(level, pos) != null || PalaceTreasury.isTreasury(level, pos) && PalaceTreasury.sealed(level, pos);
	}

	/**
	 * What Life Force Drain or Gain takes hold of: a being looked at, an aging herb growing there (or, giving, a fruit on its tree),
	 * or else a single aging fruit or herb held. Drawing life is harm (a player only where one may harm another); giving reaches
	 * anyone. A fruit is drawn on only once picked and held, never on its tree; nothing in a sealed treasury is touched until its seal
	 * is broken. {@link #TOLD}: refused, and the caster told why ({@code tell}); null: nothing there.
	 */
	record LifeTarget(LivingEntity being, BlockPos plant, net.minecraft.world.InteractionHand hand) {
		static final LifeTarget TOLD = new LifeTarget(null, null, null);
	}

	static LifeTarget lifeTarget(LivingEntity p, boolean drain, boolean tell) {
		LivingEntity being = drain ? HeavenSpells.aimed(p, LIFE_RANGE) : HeavenSpells.aimedAny(p, LIFE_RANGE);
		if (being != null && lifeMode(p) == LifeMode.YEARS && !(being instanceof ServerPlayer)) {
			// Years only from (and to) one who keeps a lifespan.
			if (tell) HeavenSpells.tell(p, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_NO_YEARS));
			return LifeTarget.TOLD;
		}
		if (being != null) return new LifeTarget(being, null, null);
		BlockPos plant = plantLookedAt(p);
		if (plant != null && drain && p.level().getBlockEntity(plant) instanceof CultivationFruitBlockEntity) plant = null;
		if (plant != null && sealedTreasury(HeavenSpells.level(p), plant)) {
			if (tell) HeavenSpells.tell(p, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_SEALED));
			return LifeTarget.TOLD;
		}
		if (plant != null) return new LifeTarget(null, plant, null);
		boolean many = false;
		for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
			net.minecraft.world.item.ItemStack held = p.getItemInHand(hand);
			if (heldAge(held) < 0) continue;
			if (held.getCount() == 1) return new LifeTarget(null, null, hand);
			many = true; // a stack would be drawn on (or aged) all at once: one at a time
		}
		if (many) {
			if (tell) HeavenSpells.tell(p, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_HOLD_ONE));
			return LifeTarget.TOLD;
		}
		return null;
	}

	/** Where a held fruit or herb is: in front of the caster's chest, the stream running from it. */
	private static Vec3 heldPoint(LivingEntity p) {
		return p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.45, 0);
	}

	private static void life(LivingEntity p, Spell spell, boolean drain) {
		HeavenSpells.endLingering(p.getUUID(), Life.class);
		LifeTarget target = lifeTarget(p, drain, false);
		if (target == null || target == LifeTarget.TOLD) return;
		HeavenSpells.linger(new Life(p, spell, drain, target.being(), target.plant(), target.hand(), target.being() != null ? lifeMode(p) : LifeMode.YEARS));
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), drain ? SoundEvents.SCULK_CATALYST_BLOOM : SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);
	}

	/**
	 * Life Force Drain or Gain on one being, one herb growing, or one fruit or herb held: for {@link #LIFE_TICKS}, a stream of
	 * emerald motes runs between it and its caster (SpellField LIFE_STREAM). Drawing: the being's life ({@link #DRAIN_PER_SECOND} of
	 * wood a second) mends the caster, and another player's years go with it ({@link #DRAIN_LIFE_YEARS} a second, half of them the
	 * caster's: Lifespan#draw); a herb's or fruit's years ({@link #DRAIN_YEARS} a second) are lost and mend the caster a little.
	 * Giving: the caster's own life runs out ({@link #GAIN_COST} a second, never below {@link #GAIN_FLOOR}) to heal a being; a herb or
	 * fruit is given the caster's own years ({@link #GAIN_LIFE_YEARS} a second for {@link #GAIN_YEARS} of growth, never the last ten
	 * of them). It breaks if they stray apart, or the one held leaves the hand; the years it moved are told as it ends.
	 * <p>
	 * On a being, it moves what its caster's {@link LifeMode} says: its health (as above; a player's years no longer go with it), its qi
	 * ({@link #DRAIN_QI} out of another player's pool a second, or a being's life burnt into qi; given, {@link #GAIN_QI} of the caster's own),
	 * or its years ({@link #DRAIN_LIFE_YEARS} out of another player a second, or {@link #GAIN_LIFE_YEARS} of the caster's own given them).
	 * The stream runs green for health, blue for qi, gold for years.
	 */
	public static final class Life extends HeavenSpells.Timed {
		private final Spell spell;
		private final boolean drain;
		private final LifeMode mode;
		public final LivingEntity being;
		public final BlockPos plant;
		/** The hand holding the fruit or herb drawn on (or given to), or null. */
		public final net.minecraft.world.InteractionHand hand;
		private final SpellField field;
		/** The years of a lifespan it has moved (drawn out of another, or given a herb), and the growth it gave, for the word as it ends. */
		private double yearsMoved;
		private int growthGiven;

		Life(LivingEntity caster, Spell spell, boolean drain, LivingEntity being, BlockPos plant, net.minecraft.world.InteractionHand hand, LifeMode mode) {
			super(caster, LIFE_TICKS);
			this.spell = spell;
			this.drain = drain;
			this.mode = mode;
			this.being = being;
			this.plant = plant;
			this.hand = hand;
			Vec3 at = being != null ? HeavenSpells.middle(being) : plant != null ? Vec3.atCenterOf(plant) : heldPoint(caster);
			this.field = SpellField.create(level, SpellField.Kind.LIFE_STREAM, at, 0.5f, 1, LIFE_TICKS, caster).target(being).data((drain ? 1 : 0) | mode.ordinal() << 1).spawn();
			if (being != null) SpellAuras.put(being, SpellAura.LIFE_FLOW, LIFE_TICKS);
		}

		/** The fruit or herb still held, alone, in its hand; or null. */
		private net.minecraft.world.item.ItemStack held() {
			net.minecraft.world.item.ItemStack stack = caster.getItemInHand(hand);
			return heldAge(stack) >= 0 && stack.getCount() == 1 ? stack : null;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			Vec3 at;
			if (being != null) {
				if (!HeavenSpells.present(being, level)) return false;
				at = HeavenSpells.middle(being);
			} else if (plant != null) {
				if (plantAge(level, plant) < 0) return false;
				at = Vec3.atCenterOf(plant);
			} else {
				if (held() == null) return false;
				at = heldPoint(caster);
			}
			if (at.distanceToSqr(caster.position()) > LIFE_LEASH * LIFE_LEASH) return false;
			field.setPos(at.x, at.y, at.z);
			if (age % 10 != 9) return true;
			if (being != null && mode != LifeMode.HEALTH) {
				if (!flow()) return false;
			} else if (drain) {
				if (being != null) {
					float taken = HeavenSpells.strikeAgain(caster, being, DRAIN_PER_SECOND / 2, spell);
					HeavenSpells.mend(caster, taken);
				} else {
					int years = plant != null ? plantAge(level, plant) : heldAge(held());
					if (years <= 1) return false;
					if (plant != null) setPlantAge(level, plant, Math.max(1, years - DRAIN_YEARS / 2));
					else setHeldAge(held(), Math.max(1, years - DRAIN_YEARS / 2));
					HeavenSpells.mend(caster, PLANT_DRAIN_HEAL / 2);
				}
			} else if (being != null) {
				if (caster.getHealth() <= GAIN_FLOOR) return false;
				caster.setHealth(Math.max(GAIN_FLOOR, caster.getHealth() - GAIN_COST / 2));
				HeavenSpells.mend(being, GAIN_HEAL / 2);
			} else {
				// A herb or a fruit grows on the caster's own years (the user's ask, 2026-10-21), never the last of them.
				int years = plant != null ? plantAge(level, plant) : heldAge(held());
				if (years >= FruitAge.MAX_YEARS) return false;
				if (!(caster instanceof ServerPlayer giver)) return false; // a herb grown on a player's own years
				if (!giver.isCreative()) {
					if (Lifespan.left(CultivationManager.own(giver)) - GAIN_LIFE_YEARS / 2 < Lifespan.WARN_YEARS) {
						HeavenSpells.tell(giver, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_TOO_FEW));
						return false;
					}
					Lifespan.age(giver, GAIN_LIFE_YEARS / 2);
					yearsMoved += GAIN_LIFE_YEARS / 2;
				}
				int grown = Math.min(FruitAge.MAX_YEARS, years + GAIN_YEARS / 2);
				growthGiven += grown - years;
				if (plant != null) setPlantAge(level, plant, grown);
				else setHeldAge(held(), grown);
			}
			if (age % 20 == 19) sound(at, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, drain ? 0.7f : 1.5f);
			return true;
		}

		/** Half a second of qi or years moved between its being and its caster ({@link #mode}). @return whether it flows on */
		private boolean flow() {
			PlayerCultivation mine = caster instanceof ServerPlayer p ? CultivationManager.get(p) : null;
			if (mode == LifeMode.QI) {
				if (drain) {
					double gained;
					if (being instanceof ServerPlayer victim) {
						PlayerCultivation theirs = CultivationManager.get(victim);
						double drawn = Math.min(DRAIN_QI / 2, theirs.getQi());
						if (drawn <= 0) return false;
						theirs.setQi(theirs.getQi() - drawn);
						gained = drawn * QI_KEPT;
					} else {
						// A being that keeps no qi: its life burnt into the caster's.
						gained = HeavenSpells.strikeAgain(caster, being, DRAIN_PER_SECOND / 2, spell) * QI_PER_LIFE;
					}
					if (mine != null) mine.setQi(Math.min(mine.maxQi(), mine.getQi() + gained));
				} else {
					if (mine != null && !((ServerPlayer) caster).isCreative()) {
						if (mine.getQi() < GAIN_QI / 2) {
							HeavenSpells.tell(caster, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_NO_QI));
							return false;
						}
						mine.setQi(mine.getQi() - GAIN_QI / 2);
					}
					if (being instanceof ServerPlayer receiver) {
						PlayerCultivation theirs = CultivationManager.get(receiver);
						theirs.setQi(Math.min(theirs.maxQi(), theirs.getQi() + GAIN_QI / 2));
					} else {
						HeavenSpells.mend(being, (float) (GAIN_QI / 2 * LIFE_PER_QI));
					}
				}
				if (caster instanceof ServerPlayer p) CultivationManager.markDirty(p.server);
				return true;
			}
			// Years: only between players (lifeTarget turned the rest away).
			if (!(being instanceof ServerPlayer other) || !(caster instanceof ServerPlayer self)) return false;
			if (drain) {
				yearsMoved += Lifespan.draw(self, other, DRAIN_LIFE_YEARS / 2);
				return true;
			}
			if (!self.isCreative()) {
				if (Lifespan.left(CultivationManager.own(self)) - GAIN_LIFE_YEARS / 2 < Lifespan.WARN_YEARS) {
					HeavenSpells.tell(self, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_TOO_FEW));
					return false;
				}
				Lifespan.age(self, GAIN_LIFE_YEARS / 2);
			}
			Lifespan.give(other, GAIN_LIFE_YEARS / 2);
			yearsMoved += GAIN_LIFE_YEARS / 2;
			return true;
		}

		@Override
		public void end() {
			field.discard();
			if (being != null) SpellAuras.remove(being, SpellAura.LIFE_FLOW);
			// The years it moved, told: drawn out of another (to them both), or given a herb or fruit.
			if (yearsMoved <= 0) return;
			long years = Math.round(yearsMoved);
			if (drain && being instanceof ServerPlayer victim) {
				caster.sendSystemMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_DRAWN, years, victim.getDisplayName(),
						Math.round(yearsMoved * Lifespan.DRAWN_KEPT)).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
				victim.sendSystemMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_LOST, caster.getDisplayName(), years)
						.withStyle(net.minecraft.ChatFormatting.RED));
			} else if (!drain && being instanceof ServerPlayer receiver) {
				HeavenSpells.tell(caster, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_GIFTED, years, receiver.getDisplayName()));
				receiver.sendSystemMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_RECEIVED, caster.getDisplayName(), years)
						.withStyle(net.minecraft.ChatFormatting.GREEN));
			} else if (!drain) {
				HeavenSpells.tell(caster, net.minecraft.network.chat.Component.translatable(ModLang.MSG_LIFE_YEARS_GIVEN, years, growthGiven));
			}
		}
	}

	// --- Return to the Bough ---

	/**
	 * Return to the Bough: how far off a tree may be, how far round the leaves looked at it seeks room to hang the fruit, how many it hangs at
	 * once, and how long they take to fly to it (ticks).
	 */
	public static final double BOUGH_REACH = 24, BOUGH_ROUND = 5;
	public static final int BOUGH_MOST = 6, BOUGH_FLIES = 14;

	/** What Return to the Bough takes hold of: the hand holding the fruit, and the places under the leaves it will hang them, nearest first. */
	record BoughTarget(net.minecraft.world.InteractionHand hand, List<BlockPos> spots) {}

	/**
	 * The fruit held and the bough looked at, for Return to the Bough; or null (and the caster told why, if {@code tell}): no Cultivation Fruit
	 * held, no tree looked at, or no room under its leaves.
	 */
	static BoughTarget boughTarget(LivingEntity p, boolean tell) {
		net.minecraft.world.InteractionHand hand = null;
		for (net.minecraft.world.InteractionHand h : net.minecraft.world.InteractionHand.values()) {
			if (p.getItemInHand(h).getItem() instanceof CultivationFruitItem) {
				hand = h;
				break;
			}
		}
		if (hand == null) {
			if (tell) HeavenSpells.tell(p, net.minecraft.network.chat.Component.translatable(ModLang.MSG_BOUGH_NO_FRUIT));
			return null;
		}
		ServerLevel level = HeavenSpells.level(p);
		BlockHitResult hit = HeavenSpells.lookedBlock(p, BOUGH_REACH);
		BlockPos at = hit == null ? null : hit.getBlockPos();
		if (at == null || !level.getBlockState(at).is(net.minecraft.tags.BlockTags.LEAVES) && !level.getBlockState(at).is(net.minecraft.tags.BlockTags.LOGS)) {
			if (tell) HeavenSpells.tell(p, net.minecraft.network.chat.Component.translatable(ModLang.MSG_BOUGH_NO_TREE));
			return null;
		}
		List<BlockPos> spots = new ArrayList<>();
		int r = (int) Math.ceil(BOUGH_ROUND);
		BlockState fruit = ModBlocks.CULTIVATION_FRUIT.defaultBlockState();
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -r, -r), at.offset(r, r, r))) {
			if (pos.distSqr(at) > BOUGH_ROUND * BOUGH_ROUND || !level.isLoaded(pos)) continue;
			if (!level.getBlockState(pos).isAir() || !fruit.canSurvive(level, pos) || sealedTreasury(level, pos)) continue;
			if (p instanceof net.minecraft.world.entity.player.Player player && !level.mayInteract(player, pos)) continue;
			spots.add(pos.immutable());
		}
		if (spots.isEmpty()) {
			if (tell) HeavenSpells.tell(p, net.minecraft.network.chat.Component.translatable(ModLang.MSG_BOUGH_NO_TREE));
			return null;
		}
		spots.sort(java.util.Comparator.comparingDouble(pos -> pos.distSqr(at)));
		// Spread over the bough, not huddled: each a block or more from the last chosen.
		List<BlockPos> chosen = new ArrayList<>();
		for (BlockPos pos : spots) {
			if (chosen.size() >= BOUGH_MOST) break;
			boolean near = false;
			for (BlockPos c : chosen) near |= c.distSqr(pos) < 2.5;
			if (!near) chosen.add(pos);
		}
		return new BoughTarget(hand, chosen);
	}

	/**
	 * Return to the Bough (the user's ask, 2026-10-25: "a mid to late realm wood spell that lets u put cultivation fruits back onto trees"): the
	 * Cultivation Fruit its caster holds (as many as it finds room for, {@link #BOUGH_MOST} at most, of the one stack) fly from their hand on
	 * streams of green light up into the tree they look at ({@link #BOUGH_FLIES}), within {@link #BOUGH_REACH}, and hang there again under its
	 * leaves, each as old as it was picked: on the bough a fruit ripens on with the years again (a fruit picked never ages). Nothing hangs in a
	 * sealed treasury, nor where its caster may not build.
	 */
	public static final class Bough extends HeavenSpells.Timed {
		private final List<BlockPos> spots;
		private final int years;
		private final Vec3 from;

		Bough(LivingEntity caster, BoughTarget target) {
			super(caster, BOUGH_FLIES + 4);
			net.minecraft.world.item.ItemStack held = caster.getItemInHand(target.hand());
			int count = Math.min(held.getCount(), target.spots().size());
			this.years = CultivationFruitItem.age(held);
			this.spots = new ArrayList<>(target.spots().subList(0, count));
			if (!(caster instanceof net.minecraft.world.entity.player.Player player && player.isCreative())) held.shrink(count);
			this.from = HeavenSpells.fromHands(caster);
			for (BlockPos spot : spots) {
				Vec3 to = Vec3.atCenterOf(spot);
				SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, from, to.subtract(from), 1.4f, 0, 96);
			}
			sound(from, SoundEvents.AZALEA_LEAVES_PLACE, 1.5f, 1.2f);
			sound(from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < BOUGH_FLIES) {
				// The fruit borne up on their streams of light.
				if (age % 4 == 1) for (BlockPos spot : spots) {
					Vec3 to = Vec3.atCenterOf(spot);
					Vec3 here = from.lerp(to, (age + 1) / (double) BOUGH_FLIES).add(0, Math.sin(Math.PI * (age + 1) / BOUGH_FLIES) * 1.5, 0);
					level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, here.x, here.y, here.z, 2, 0.15, 0.15, 0.15, 0);
					level.sendParticles(net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES, here.x, here.y, here.z, 1, 0.2, 0.2, 0.2, 0);
				}
				return true;
			}
			if (age == BOUGH_FLIES) hang();
			return true;
		}

		/** They hang on the bough again, each at its age; any that finds no room is given back. */
		private void hang() {
			int hung = 0;
			BlockState fruit = ModBlocks.CULTIVATION_FRUIT.defaultBlockState();
			for (BlockPos spot : spots) {
				if (level.getBlockState(spot).isAir() && fruit.canSurvive(level, spot) && level.setBlock(spot, fruit, 3)
						&& level.getBlockEntity(spot) instanceof CultivationFruitBlockEntity grown) {
					grown.setAge(years);
					hung++;
					Vec3 at = Vec3.atCenterOf(spot);
					SpellFx.send(level, SpellFx.Kind.LEAVES, at, Vec3.ZERO, 1.2f, 0, 96);
					SpellFx.send(level, SpellFx.Kind.FLASH, at, Vec3.ZERO, 1.6f, 0xB8FFB0, 96);
					sound(at, SoundEvents.AZALEA_LEAVES_PLACE, 1.2f, 0.9f + level.random.nextFloat() * 0.3f);
				} else {
					net.minecraft.world.item.ItemStack back = CultivationFruitItem.create(years);
					if (!(caster instanceof net.minecraft.world.entity.player.Player player) || !player.getInventory().add(back)) caster.spawnAtLocation(back);
				}
			}
			if (hung > 0) {
				sound(Vec3.atCenterOf(spots.get(0)), SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.3f);
				HeavenSpells.tell(caster, net.minecraft.network.chat.Component.translatable(ModLang.MSG_BOUGH_HUNG, hung, years));
			}
		}
	}

	private static void prison(LivingEntity p, Spell spell) {
		LivingEntity target = HeavenSpells.aimed(p, PRISON_RANGE);
		if (target == null) return;
		HeavenSpells.linger(new Prison(p, spell, target));
		HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.6f);
		HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.5f, 0.7f);
	}

	/**
	 * Mysterious Bristle Prison: thorny bristles burst up from the ground and close round a being, holding it where it stands for
	 * {@link #PRISON_TICKS} and tearing it ({@link #PRISON_DAMAGE} of wood a second). The cage can be struck (SpellField#strikeable):
	 * {@link #PRISON_STRIKES} blows from anyone break it, but each the caged one strikes from inside tears it for
	 * {@link #PRISON_THORNS} more.
	 */
	public static final class Prison extends HeavenSpells.Timed {
		private final Spell spell;
		public final LivingEntity target;
		private final SpellField field;
		private final Bound bound;
		private int strikes;

		Prison(LivingEntity caster, Spell spell, LivingEntity target) {
			super(caster, PRISON_TICKS);
			this.spell = spell;
			this.target = target;
			float radius = target.getBbWidth() / 2 + 0.65f;
			this.field = SpellField.create(level, SpellField.Kind.BRISTLE_PRISON, target.position(), radius, target.getBbHeight() + 0.5f, PRISON_TICKS,
					caster).target(target).data(caster.getRandom().nextInt()).strikeable(this::struck).spawn();
			this.bound = bind(caster, target, PRISON_TICKS);
		}

		private void struck(SpellField cage, DamageSource source, float amount) {
			if (cage.isRemoved()) return;
			strikes++;
			cage.value(1 - strikes / (float) PRISON_STRIKES);
			sound(cage.position(), SoundEvents.SWEET_BERRY_BUSH_BREAK, 1.2f, 0.8f);
			if (source.getEntity() == target) HeavenSpells.strikeAgain(caster, target, PRISON_THORNS, spell);
			if (strikes >= PRISON_STRIKES) end = level.getGameTime(); // broken: it ends this tick
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level) || field.isRemoved()) return false;
			if (age % 10 == 9) HeavenSpells.strikeAgain(caster, target, PRISON_DAMAGE / 2, spell);
			return true;
		}

		@Override
		public void end() {
			if (strikes >= PRISON_STRIKES) {
				SpellFx.send(level, SpellFx.Kind.THORNS_BREAK, field.position().add(0, field.height() / 2, 0), Vec3.ZERO, field.radius(), 0);
				sound(field.position(), SoundEvents.WOOD_BREAK, 1.5f, 0.6f);
			} else {
				SpellFx.send(level, SpellFx.Kind.SPLINTERS, field.position().add(0, field.height() / 2, 0), Vec3.ZERO, 1.0f, 0);
			}
			field.discard();
			HeavenSpells.stop(bound);
		}
	}

	// --- Core Formation ---

	/** Thousand Leaf Storm: {@link #LEAVES} razor leaves stream from the palm, one a tick, each bending a little toward its mark. */
	public static final class LeafStorm extends HeavenSpells.Timed {
		private final Spell spell;
		private int thrown;

		LeafStorm(LivingEntity caster, Spell spell) {
			super(caster, LEAVES + 2);
			this.spell = spell;
			sound(caster.position(), SoundEvents.AZALEA_LEAVES_BREAK, 1.5f, 1.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere() || thrown >= LEAVES) return thrown < LEAVES;
			double spread = 0.5;
			Vec3 offset = new Vec3(0.3 + (level.random.nextDouble() - 0.5) * spread, -0.2 + (level.random.nextDouble() - 0.5) * spread, 1.0);
			SpellProjectile leaf = SpellProjectile.cast(caster, spell, SpellProjectile.Shape.RAZOR_LEAF, 0.8f + level.random.nextFloat() * 0.4f, 0,
					offset, LEAF_SPEED, LEAF_RANGE, new SpellProjectile.Behaviour() {
						@Override
						public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
							HeavenSpells.strike(caster, target, LEAF_DAMAGE, spell, missile);
							target.invulnerableTime = 0; // a storm of leaves, each its own cut
							SpellFx.send(level, SpellFx.Kind.LEAVES, missile.position(), Vec3.ZERO, 0.3f, 0);
							return false;
						}

						@Override
						public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
							SpellFx.send(level, SpellFx.Kind.LEAVES, hit.getLocation(), Vec3.ZERO, 0.3f, 0);
						}
					}).homing(null, 2.5).data(level.random.nextInt());
			// Each its own way a little, fanning out of the palm.
			leaf.setDeltaMovement(leaf.getDeltaMovement().add((level.random.nextDouble() - 0.5) * 0.25, (level.random.nextDouble() - 0.5) * 0.15,
					(level.random.nextDouble() - 0.5) * 0.25));
			thrown++;
			if (thrown % 4 == 0) sound(caster.position(), SoundEvents.AZALEA_LEAVES_STEP, 1.0f, 1.6f);
			return true;
		}
	}

	// --- Nascent Soul ---

	private static void spears(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		LivingEntity aimed = HeavenSpells.aimed(p, SPEARS_RANGE);
		Vec3 point = aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SPEARS_RANGE);
		Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 1, 0), 3, 16);
		Vec3 at = ground != null ? ground : point;
		// The ground stirs, and the spears of living wood (SpellConstruct THORN_FOREST) thrust up as its blow lands; they stand a moment.
		SpellConstruct forest = SpellConstruct.create(level, SpellConstruct.Kind.THORN_FOREST, at, level.random.nextFloat() * 360, 0, 50, p).sizedFor(spell).spawn();
		HeavenSpells.linger(new FireSpells.Delayed(p, 40, forest::dissolve));
		HeavenSpells.sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, 2.0f, 0.5f);
		HeavenSpells.linger(new FireFromBelow(p, 6, () -> {
			// Whoever stands in it that the spells don't spare: foes, and people (players, cultivators), the one aimed at too.
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, SPEARS_RADIUS + 1, e -> e == aimed || HeavenSpells.mark(p, e))) {
				if (Math.abs(e.getY() - at.y) > 3) continue;
				HeavenSpells.strike(p, e, SPEARS_DAMAGE, spell);
				bind(p, e, SPEARS_ROOT);
			}
			HeavenSpells.sound(level, at, SoundEvents.SWEET_BERRY_BUSH_BREAK, 2.0f, 0.5f);
			HeavenSpells.sound(level, at, SoundEvents.TRIDENT_HIT, 2.0f, 0.6f);
		}));
	}

	/** A spell's blow a moment after its sign (spears breaking the surface where the ground first stirred). */
	static final class FireFromBelow extends HeavenSpells.Timed {
		private final Runnable then;

		FireFromBelow(LivingEntity caster, int ticks, Runnable then) {
			super(caster, ticks);
			this.then = then;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age >= end - start) {
				then.run();
				return false;
			}
			return true;
		}
	}

	/**
	 * Lotus of Rebirth: a great lotus (SpellConstruct REBIRTH_LOTUS) opens at its caster's feet among its pads; all friendly within
	 * {@link #LOTUS_RADIUS} heal and are cleansed.
	 */
	public static final class Lotus extends HeavenSpells.Timed {
		private final Vec3 at;
		private final SpellConstruct lotus;

		Lotus(LivingEntity caster, Spell spell) {
			super(caster, LOTUS_TICKS);
			this.at = caster.position();
			this.lotus = SpellConstruct.create(level, SpellConstruct.Kind.REBIRTH_LOTUS, at, caster.getYRot(), 0, LOTUS_TICKS + 10, caster).sizedFor(spell).spawn();
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, LOTUS_RADIUS, e -> HeavenSpells.blessed(caster, e))) {
				for (MobEffectInstance effect : List.copyOf(e.getActiveEffects())) {
					if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) e.removeEffect(effect.getEffect());
				}
				e.clearFire();
			}
			sound(at, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.4f);
			sound(at, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0f, 0.8f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 20 == 10) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, at, LOTUS_RADIUS, e -> HeavenSpells.blessed(caster, e))) HeavenSpells.mend(e, LOTUS_HEAL);
			}
			return true;
		}

		@Override
		public void end() {
			lotus.dissolve();
		}
	}

	// --- Heavenly Being ---

	/** Where a spell that seeks beings out lands: the being aimed at (within {@code range}), or else the point looked at. */
	static Vec3 seekCentre(LivingEntity p, LivingEntity aimed, double range) {
		return aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, range);
	}

	/**
	 * Up to {@code count} beings for a spell that seeks them out where its caster looks (Heaven-Piercing Bamboo, Myriad Vine
	 * Binding): the one aimed at first, then the marks ({@link HeavenSpells#mark}: foes, and players and cultivators the caster's
	 * spells don't spare) within {@code radius} of it, nearest first. With a being aimed at, the search is round that being, so
	 * one standing against the open sky (the look running on past it to the end of the range) is still found.
	 */
	static List<LivingEntity> marksNear(LivingEntity p, double range, double radius, int count) {
		LivingEntity aimed = HeavenSpells.aimed(p, range);
		Vec3 centre = seekCentre(p, aimed, range);
		List<LivingEntity> found = new ArrayList<>(HeavenSpells.aroundPoint(HeavenSpells.level(p), centre, radius, e -> e != aimed && HeavenSpells.mark(p, e)));
		found.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(centre)));
		if (aimed != null) found.add(0, aimed);
		return found.size() > count ? new ArrayList<>(found.subList(0, count)) : found;
	}

	private static void bamboo(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 centre = seekCentre(p, HeavenSpells.aimed(p, BAMBOO_RANGE), BAMBOO_RANGE);
		List<LivingEntity> foes = marksNear(p, BAMBOO_RANGE, BAMBOO_RADIUS, BAMBOO_TARGETS);
		for (LivingEntity foe : foes) {
			Vec3 ground = HeavenSpells.groundBelow(level, foe.position().add(0, 0.5, 0), 2, 24);
			Vec3 at = ground != null ? ground : foe.position();
			// Spirit bamboo bursting up under it (SpellConstruct PIERCING_BAMBOO), standing a moment before it goes.
			SpellConstruct grove = SpellConstruct.create(level, SpellConstruct.Kind.PIERCING_BAMBOO, at, level.random.nextFloat() * 360, 0, 50, p).sizedFor(spell).spawn();
			HeavenSpells.linger(new FireSpells.Delayed(p, 40, grove::dissolve));
			Devastation.crater(p, level, at, BAMBOO_BREAK, BAMBOO_BREAK, Devastation.Floor.RUBBLE); // the ground it burst out of, broken
			HeavenSpells.strike(p, foe, BAMBOO_DAMAGE, spell);
			HeavenSpells.move(foe, new Vec3(foe.getDeltaMovement().x, 1.6, foe.getDeltaMovement().z));
		}
		HeavenSpells.sound(level, centre, SoundEvents.BAMBOO_PLACE, 3.0f, 0.5f);
		HeavenSpells.sound(level, centre, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.4f);
	}

	/** Spring of Myriad Life: a green rain of life over all friendly within {@link #SPRING_RADIUS}: a great mending, then more each second. */
	public static final class Spring extends HeavenSpells.Timed {
		private final SpellField field;

		Spring(LivingEntity caster) {
			super(caster, SPRING_TICKS);
			this.field = SpellField.create(level, SpellField.Kind.SPRING_RAIN, caster.position(), (float) SPRING_RADIUS, 18, SPRING_TICKS, caster)
					.anchor(caster, SpellAnchor.POSITION, Vec3.ZERO).spawn();
			for (LivingEntity e : HeavenSpells.around(caster, SPRING_RADIUS, e -> HeavenSpells.blessed(caster, e))) {
				HeavenSpells.mend(e, SPRING_BURST);
				SpellFx.send(level, SpellFx.Kind.LEAVES, HeavenSpells.middle(e), Vec3.ZERO, 0.6f, 0);
			}
			sound(caster.position(), SoundEvents.BEACON_ACTIVATE, 2.0f, 1.6f);
			sound(caster.position(), SoundEvents.WEATHER_RAIN_ABOVE, 2.0f, 1.2f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			field.setPos(caster.getX(), caster.getY(), caster.getZ());
			if (age % 20 == 10) {
				for (LivingEntity e : HeavenSpells.around(caster, SPRING_RADIUS, e -> HeavenSpells.blessed(caster, e))) HeavenSpells.mend(e, SPRING_HEAL);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Four Axis ---

	private static void vines(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 centre = seekCentre(p, HeavenSpells.aimed(p, VINES_RANGE), VINES_RANGE);
		List<LivingEntity> foes = marksNear(p, VINES_RANGE, VINES_RADIUS, VINES_TARGETS);
		for (LivingEntity foe : foes) HeavenSpells.linger(new Vines(p, spell, foe));
		HeavenSpells.sound(level, centre, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.4f);
		HeavenSpells.sound(level, p.position(), SoundEvents.EVOKER_CAST_SPELL, 2.0f, 0.5f);
		if (foes.isEmpty()) SpellFx.send(level, SpellFx.Kind.LEAVES, centre, Vec3.ZERO, 2.0f, 0);
	}

	/**
	 * One being seized by Myriad Vine Binding: held fast {@link #VINES_TICKS}, {@link #VINES_DAMAGE} of wood a second. Thorned vines of living wood
	 * (SpellConstruct BINDING_VINES) burst out of the ground round it and coil up it, crushing it at every second's harm.
	 */
	public static final class Vines extends HeavenSpells.Timed {
		private final Spell spell;
		public final LivingEntity target;
		private final SpellConstruct vines;
		private final Bound bound;

		Vines(LivingEntity caster, Spell spell, LivingEntity target) {
			super(caster, VINES_TICKS);
			this.spell = spell;
			this.target = target;
			this.vines = SpellConstruct.create(level, SpellConstruct.Kind.BINDING_VINES, target.position(), caster.getRandom().nextFloat() * 360, 0, VINES_TICKS, caster)
					.size(Math.max(target.getBbHeight() / 2, target.getBbWidth() / 0.75f)).spawn();
			this.bound = new Bound(caster, target, target.position(), VINES_TICKS);
			HeavenSpells.linger(bound);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			vines.place(target.position(), vines.getYRot(), 0);
			if (age % 10 == 9) {
				HeavenSpells.strikeAgain(caster, target, VINES_DAMAGE / 2, spell);
				if (age % 20 == 9) vines.act("crush");
			}
			return true;
		}

		@Override
		public void end() {
			vines.dissolve();
			HeavenSpells.stop(bound);
		}
	}

	/**
	 * The World Tree: it grows out of the ground where its caster looks (or before them), towering over all; for {@link #TREE_TICKS}
	 * its vines reach from its boughs for every being but the caster within {@link #TREE_REACH} of its trunk (their pets spared),
	 * lift them into its branches and hold them there, drinking {@link #TREE_DRAIN} of their life a second into its caster.
	 */
	public static final class WorldTree extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 trunk;
		private final SpellField field;
		/** Who hangs in its branches, and where each is held. */
		private final java.util.Map<LivingEntity, Vec3> seized = new java.util.HashMap<>();
		/** The vine that holds each (SpellField TREE_VINE: from a bough to them). */
		private final java.util.Map<LivingEntity, SpellField> vines = new java.util.HashMap<>();

		WorldTree(LivingEntity caster, Spell spell) {
			super(caster, TREE_TICKS);
			this.spell = spell;
			Vec3 point = HeavenSpells.lookPoint(caster, TREE_RANGE);
			if (point.distanceToSqr(caster.position()) < 36) point = caster.position().add(Vec3.directionFromRotation(0, caster.getYRot()).scale(8));
			Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.trunk = ground != null ? ground : point;
			this.field = SpellField.create(level, SpellField.Kind.WORLD_TREE, trunk, (float) TREE_REACH, 40, TREE_TICKS, caster)
					.data(caster.getRandom().nextInt()).spawn();
			// Its roots heave the land open as it grows.
			Devastation.fissures(caster, level, trunk, TREE_FISSURES, 12, 4, 4);
			sound(trunk, SoundEvents.ROOTED_DIRT_BREAK, 4.0f, 0.3f);
			sound(trunk, SoundEvents.WARDEN_EMERGE, 2.0f, 0.6f);
		}

		@Override
		protected boolean run(long age, long now) {
			// Its vines reach once it has grown (two seconds), and keep reaching for whoever comes.
			if (age >= 40 && age % 10 == 0) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, trunk, TREE_REACH, e -> e != caster && !HeavenSpells.spared(caster, e) && !seized.containsKey(e))) {
					double a = level.random.nextDouble() * Math.PI * 2;
					double r = 5 + level.random.nextDouble() * 9;
					seized.put(e, trunk.add(Math.cos(a) * r, 14 + level.random.nextDouble() * 14, Math.sin(a) * r));
					vines.put(e, SpellField.create(level, SpellField.Kind.TREE_VINE, trunk, (float) a, 24, (int) (end - level.getGameTime()), caster)
							.target(e).data(level.random.nextInt()).spawn());
					sound(e.position(), SoundEvents.ROOTED_DIRT_PLACE, 1.5f, 0.5f);
				}
			}
			for (java.util.Iterator<java.util.Map.Entry<LivingEntity, Vec3>> it = seized.entrySet().iterator(); it.hasNext(); ) {
				java.util.Map.Entry<LivingEntity, Vec3> entry = it.next();
				LivingEntity e = entry.getKey();
				if (!HeavenSpells.present(e, level)) {
					SpellField vine = vines.remove(e);
					if (vine != null) vine.discard();
					it.remove();
					continue;
				}
				// Drawn up to its bough over a second, then held.
				Vec3 hold = entry.getValue();
				Vec3 to = hold.subtract(e.position());
				if (to.lengthSqr() > 0.25) HeavenSpells.move(e, to.scale(0.2));
				else HeavenSpells.move(e, Vec3.ZERO);
				e.resetFallDistance();
				if (age % 10 == 5) {
					float taken = HeavenSpells.strikeAgain(caster, e, TREE_DRAIN / 2, spell);
					if (casterHere() && caster.position().distanceToSqr(trunk) < 96 * 96) HeavenSpells.mend(caster, taken);
				}
			}
			field.value(seized.size());
			return true;
		}

		/** Whom it holds now (tests). */
		public java.util.Set<LivingEntity> seized() { return seized.keySet(); }

		@Override
		public void end() {
			for (LivingEntity e : seized.keySet()) HeavenSpells.safeFall(e, 100);
			for (SpellField vine : vines.values()) vine.discard();
			field.discard();
		}
	}

	// --- Dao Integration ---

	/**
	 * Azure Dragon: the Azure Dragon of the east (SpellConstruct AZURE_DRAGON, its own GeckoLib model) bursts up out of the ground where its
	 * caster looks and flies off into the sky, away the way they looked (DragonFlight: its body laid along the path its head has flown, a
	 * serpent's flight; the user's ask, 2026-10-25, "don't make azure dragon like look up and roar, just make it fly off, also make it
	 * smoother ... looks like its spinning instead of flying": it coiled once round where it rose, turned about whole, and roared at the
	 * top). Whatever stands within {@link #DRAGON_RADIUS} of where it bursts out takes {@link #DRAGON_DAMAGE} and is caught up on its body,
	 * carried up and away with it {@link #DRAGON_HOLD} ticks and then flung off (a great beast only struck); the land it bursts from is torn
	 * open in {@link #DRAGON_FISSURES} fissures out to {@link #DRAGON_REACH}.
	 */
	public static final class Dragon extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, start;
		private final DragonFlight.Path path;
		private final SpellConstruct dragon;
		/** Those caught up on its body, and how far back along it from its head each is held (blocks). */
		private final java.util.Map<LivingEntity, Double> caught = new java.util.LinkedHashMap<>();
		private Vec3 lastHead;

		Dragon(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, DragonFlight.TICKS);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.ground = below != null ? below : point;
			// It flies off away from its caster, the way they looked at where it rises.
			Vec3 away = ground.subtract(caster.position()).multiply(1, 0, 1);
			float yaw = away.lengthSqr() < 4 ? caster.getYRot() : HeavenSpells.facing(away)[0];
			float size = (float) HeavenSpells.greatness(spell.realm());
			this.path = DragonFlight.of(yaw, size);
			this.start = ground.add(0, -DragonFlight.UNDER, 0);
			this.dragon = SpellConstruct.create(level, SpellConstruct.Kind.AZURE_DRAGON, start, yaw, -80, DragonFlight.TICKS, caster).size(size).value(yaw).spawn();
			this.lastHead = start;
			// It bursts up out of the land, whatever stands round it struck and caught up on its body, the nearest held nearest its head.
			List<LivingEntity> near = HeavenSpells.aroundPoint(level, ground, DRAGON_RADIUS, e -> !HeavenSpells.spared(caster, e));
			near.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(ground)));
			double behind = DRAGON_CARRY_FROM * size;
			for (LivingEntity e : near) {
				HeavenSpells.strike(caster, e, DRAGON_DAMAGE, spell);
				if (!HeavenSpells.greatBeast(caster, e) && caught.size() < DRAGON_CARRIES) {
					caught.put(e, behind);
					behind += DRAGON_CARRY_EVERY * size;
				}
				SpellFx.send(level, SpellFx.Kind.LEAVES, HeavenSpells.middle(e), Vec3.ZERO, 1.4f, 0);
			}
			Devastation.fissures(caster, level, ground, DRAGON_REACH, DRAGON_FISSURES, (int) Math.round(DRAGON_FISSURE_DEPTH), 4);
			int rooted = net.minecraft.world.level.block.Block.getId(Blocks.ROOTED_DIRT.defaultBlockState());
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, (float) DRAGON_RADIUS * 0.6f, rooted, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) DRAGON_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) DRAGON_RADIUS * 0.7f, 0, 192);
			sound(ground, SoundEvents.ENDER_DRAGON_GROWL, 3.5f, 1.45f);
			sound(ground, SoundEvents.ROOTED_DIRT_BREAK, 4.0f, 0.45f);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.6f);
			sound(ground, SoundEvents.AZALEA_LEAVES_BREAK, 3.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			Vec3 head = start.add(path.at(age));
			float[] face = HeavenSpells.facing(path.heading(age));
			dragon.place(head, face[0], face[1]);
			// Earth and leaves torn up after it as its body pours up out of the land, and leaves shed in its wake as it goes.
			if (age < 30 && age % 3 == 0) {
				SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, 3.0f,
						net.minecraft.world.level.block.Block.getId(Blocks.ROOTED_DIRT.defaultBlockState()), 192);
			}
			if (age % 4 == 0 && age < DragonFlight.TICKS - 20) SpellFx.send(level, SpellFx.Kind.LEAVES, head, Vec3.ZERO, 2.2f, 0, 256);
			// The rush of its flight, and the beat of its body through the air, fading as it goes.
			if (age % 13 == 6) {
				float off = (float) Math.max(0.3, 1 - age / (double) DragonFlight.TICKS);
				sound(head, SoundEvents.ENDER_DRAGON_FLAP, 4.0f * off, 0.55f + level.random.nextFloat() * 0.1f);
			}
			if (age == 8) sound(head, SoundEvents.ELYTRA_FLYING, 3.0f, 0.6f);
			if (age == 46) sound(head, SoundEvents.ENDER_DRAGON_GROWL, 4.0f, 1.25f);
			// Those caught up on it go with it, held on its body, until it lets them fall.
			if (age < DRAGON_HOLD && !caught.isEmpty()) {
				Vec3 way = head.subtract(lastHead);
				for (java.util.Iterator<java.util.Map.Entry<LivingEntity, Double>> it = caught.entrySet().iterator(); it.hasNext(); ) {
					java.util.Map.Entry<LivingEntity, Double> held = it.next();
					LivingEntity e = held.getKey();
					if (!HeavenSpells.present(e, level)) {
						it.remove();
						continue;
					}
					Vec3 on = start.add(path.behind(age, held.getValue())).add(0, 0.6, 0);
					HeavenSpells.move(e, on.subtract(e.position()).scale(0.55).add(way.scale(0.45)));
					e.resetFallDistance();
				}
			}
			if (age == DRAGON_HOLD) {
				// Flung off its body as it climbs on away.
				Vec3 heading = path.heading(age);
				for (LivingEntity e : caught.keySet()) {
					if (!HeavenSpells.present(e, level)) continue;
					Vec3 off = new Vec3(level.random.nextGaussian(), 0, level.random.nextGaussian()).normalize().scale(0.6);
					HeavenSpells.move(e, heading.scale(0.9).add(off).add(0, 0.3, 0));
					HeavenSpells.safeFall(e, 240);
					SpellFx.send(level, SpellFx.Kind.LEAVES, HeavenSpells.middle(e), Vec3.ZERO, 1.8f, 0, 192);
				}
				caught.clear();
			}
			lastHead = head;
			return true;
		}

		@Override
		public void end() {
			for (LivingEntity e : caught.keySet()) if (HeavenSpells.present(e, level)) HeavenSpells.safeFall(e, 240);
			caught.clear();
			dragon.dissolve();
		}
	}

	/**
	 * The living roots a wood spell drinks through, made seen (the user's ask, 2026-10-25: "a lot of the wood tree moves just summon a tree and
	 * people die, like theres no visuals as to why they are dying"): one reaching up out of the land from its tree's foot to each being it drinks
	 * from (SpellConstruct LIVING_ROOT: TendrilRenderer), coiled round it, the life drawn down it to the tree, kept on it as long as it is held
	 * and let go as it leaves the tree's reach or dies. At most {@link #MOST} at once.
	 */
	static final class Rooting {
		static final int MOST = 16;
		private final LivingEntity caster;
		private final ServerLevel level;
		private final Vec3 from;
		private final float girth;
		private final java.util.Map<LivingEntity, SpellConstruct> roots = new java.util.LinkedHashMap<>();

		Rooting(LivingEntity caster, ServerLevel level, Vec3 from, float girth) {
			this.caster = caster;
			this.level = level;
			this.from = from.add(0, 0.1, 0);
			this.girth = girth;
		}

		/** It holds {@code drinking} now, for at most {@code left} ticks more: a root to each newly held, those no longer held let go. */
		void hold(java.util.Collection<LivingEntity> drinking, int left) {
			roots.entrySet().removeIf(e -> {
				boolean gone = !drinking.contains(e.getKey()) || !HeavenSpells.present(e.getKey(), level);
				if (gone) e.getValue().dissolve();
				return gone;
			});
			for (LivingEntity e : drinking) {
				if (roots.containsKey(e) || roots.size() >= MOST) continue;
				roots.put(e, SpellConstruct.create(level, SpellConstruct.Kind.LIVING_ROOT, from, 0, 0, Math.max(10, left) + SpellConstruct.FADE, caster).size(girth).target(e).spawn());
				level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ROOTED_DIRT_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.6f);
			}
		}

		/** Every root let go. */
		void release() {
			for (SpellConstruct root : roots.values()) root.dissolve();
			roots.clear();
		}
	}

	/**
	 * Myriad Woods Return to the Dao: a spirit tree of jade light (SpellConstruct DAO_TREE) grows where its caster stands and holds the land
	 * {@link #WOODS_RADIUS} round for {@link #WOODS_TICKS}: every second each foe in it is rooted and drained of {@link #WOODS_DRAIN}, all of it
	 * shared out as healing to the caster and its own within the woods. It withers away at the end; the land is left as it was.
	 */
	public static final class Woods extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final SpellConstruct tree;
		/** Its roots to every foe it drinks from (Rooting). */
		private final Rooting rooting;

		Woods(LivingEntity caster, Spell spell) {
			super(caster, WOODS_TICKS);
			this.spell = spell;
			this.at = HeavenSpells.footing(caster);
			this.tree = SpellConstruct.create(level, SpellConstruct.Kind.DAO_TREE, at, caster.getYRot(), 0, WOODS_TICKS, caster).sizedFor(spell).spawn();
			this.rooting = new Rooting(caster, level, at, 1.5f);
			sound(at, SoundEvents.AZALEA_LEAVES_PLACE, 3.0f, 0.5f);
			sound(at, SoundEvents.BEACON_ACTIVATE, 2.0f, 1.4f);
			SpellFx.send(level, SpellFx.Kind.LEAVES, at.add(0, 2, 0), Vec3.ZERO, 4, 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age == WOODS_TICKS - 30) tree.act("wither");
			if (age % 20 != 10) return true;
			float drawn = 0;
			Vec3 crown = at.add(0, 10, 0);
			List<LivingEntity> drinking = HeavenSpells.aroundPoint(level, at, WOODS_RADIUS, e -> HeavenSpells.mark(caster, e));
			rooting.hold(drinking, (int) (end - level.getGameTime()));
			for (LivingEntity e : drinking) {
				drawn += HeavenSpells.strikeAgain(caster, e, WOODS_DRAIN, spell);
				e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 3)); // rooted
				SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, HeavenSpells.middle(e), crown.subtract(HeavenSpells.middle(e)), 1, 0); // its life, drawn to the tree
			}
			if (drawn > 0) {
				List<LivingEntity> own = HeavenSpells.aroundPoint(level, at, WOODS_RADIUS, e -> HeavenSpells.blessed(caster, e));
				if (!own.contains(caster)) own.add(caster);
				for (LivingEntity e : own) {
					HeavenSpells.mend(e, drawn / own.size());
					SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, crown, HeavenSpells.middle(e).subtract(crown), 0, 0); // and given to its own
				}
			}
			if (age % 60 == 10) sound(at, SoundEvents.AZALEA_LEAVES_BREAK, 2.0f, 0.7f);
			return true;
		}

		@Override
		public void end() {
			rooting.release();
			tree.dissolve();
		}
	}

	// --- Void Shattering ---

	/**
	 * Spring Thunder of the Azure Emperor (the Green Emperor of the east and the spring: in the old lore thunder is wood's): a storm cloud of
	 * jade (SpellConstruct AZURE_THUNDER_CLOUD, its own GeckoLib model: the thunder trigram ☳ burning under it) gathers
	 * {@link #THUNDER_HIGH} over where its caster looks ({@link #THUNDER_GATHER} ticks) and calls spring thunder down in {@link #THUNDER_VOLLEYS}
	 * volleys, {@link #THUNDER_EVERY} ticks apart: in each, every foe under it (within {@link #THUNDER_RADIUS}) is struck by a bolt of jade
	 * lightning for {@link #THUNDER_DAMAGE} and rooted, and {@link #THUNDER_STRAYS} more bolts strike the land beneath, each leaving a little
	 * crater (a player's).
	 */
	public static final class Thunder extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct cloud;
		private int volleys;

		Thunder(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, THUNDER_GATHER + THUNDER_VOLLEYS * THUNDER_EVERY);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.cloud = SpellConstruct.create(level, SpellConstruct.Kind.AZURE_THUNDER_CLOUD, ground.add(0, THUNDER_HIGH, 0), caster.getYRot(), 0, (int) (end - start), caster)
					.sizedFor(spell).spawn();
			sound(ground.add(0, THUNDER_HIGH, 0), SoundEvents.LIGHTNING_BOLT_THUNDER, 3.0f, 0.5f);
			sound(caster.position(), SoundEvents.BEACON_ACTIVATE, 2.0f, 1.2f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < THUNDER_GATHER || (age - THUNDER_GATHER) % THUNDER_EVERY != 0 || volleys >= THUNDER_VOLLEYS) {
				if (age % 30 == 15) sound(ground.add(0, THUNDER_HIGH, 0), SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0f, 0.4f);
				return true;
			}
			volleys++;
			cloud.act("flash");
			boolean first = true;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, THUNDER_RADIUS + THUNDER_HIGH, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > THUNDER_RADIUS || e.getY() > ground.y + THUNDER_HIGH - 2 || e.getY() < ground.y - 16) continue;
				bolt(e.position(), first, false);
				first = false;
				HeavenSpells.strikeAgain(caster, e, THUNDER_DAMAGE, spell);
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, THUNDER_ROOT, 6)); // rooted where it stands
				SpellFx.send(level, SpellFx.Kind.LEAVES, HeavenSpells.middle(e), Vec3.ZERO, 1.2f, 0);
			}
			// And at the land beneath, the storm's own.
			for (int i = 0; i < THUNDER_STRAYS; i++) {
				double angle = level.random.nextDouble() * Math.PI * 2, out = Math.sqrt(level.random.nextDouble()) * THUNDER_RADIUS;
				Vec3 at = ground.add(Math.cos(angle) * out, 0, Math.sin(angle) * out);
				Vec3 land = HeavenSpells.groundBelow(level, at.add(0, 16, 0), 0, 48);
				bolt(land != null ? land : at, first, true);
				first = false;
			}
			return true;
		}

		/** A bolt of jade spring thunder from the cloud down to {@code at} (the volley's first lights the sky); one at the land leaves a little crater. */
		private void bolt(Vec3 at, boolean flash, boolean land) {
			TribulationLightning bolt = ModEntities.TRIBULATION_LIGHTNING.create(level);
			if (bolt != null) {
				bolt.moveTo(at.x, at.y, at.z);
				bolt.setCloudBaseY(ground.y + THUNDER_HIGH);
				bolt.setScale(2.5f);
				bolt.setTint(THUNDER_JADE);
				bolt.setVisualOnly(true); // its harm is the spell's own
				bolt.setDim(!flash);
				level.addFreshEntity(bolt);
			}
			if (land) Devastation.crater(caster, level, at, THUNDER_CRATER, THUNDER_CRATER_DEPTH, Devastation.Floor.NONE);
		}

		@Override
		public void end() {
			cloud.dissolve();
		}
	}

	/**
	 * Void Withering (the Void-Rooted Divine Tree's, whose roots hang in the void and drink from nothing): space tears open in a slash across the
	 * sky {@link #WITHER_HIGH} over where its caster looks (SpellConstruct VOID_RIFT lying across), and once it is open the void tree's roots
	 * spill down out of it (VOID_ROOTS, its own GeckoLib model). For {@link #WITHER_TICKS} they reach out and drink (the user's ask,
	 * 2026-10-24: roots that grab things, out of a rift): a root lashes down to each foe within {@link #WITHER_RADIUS} (VOID_TENDRIL, drawn by
	 * hand: TendrilRenderer), {@link #WITHER_GRASPS} at most at once, coils round it and hauls it up into the air ({@link #WITHER_LIFT}; a great
	 * beast only held where it stands); every second each foe within reach is drained of {@link #WITHER_DRAIN}, weakened and slowed, all of it
	 * shared out as healing to the caster and its own within reach; the land under them withers outward from their heart (Devastation#wither: a
	 * player's). Then they let go (what they held falls, unharmed by its fall) and draw back up, and the rift closes. Its caster gone, it goes.
	 */
	public static final class Withering extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, mouth;
		private final float yaw;
		private final SpellConstruct rift;
		private SpellConstruct roots;
		/** What its roots hold, oldest first: each held one's root, where it is hauled to, and when it was seized (its age then). */
		private final java.util.Map<LivingEntity, Held> held = new java.util.LinkedHashMap<>();
		/** How far out the land has withered. */
		private double withered;

		private record Held(SpellConstruct root, Vec3 to, long since) {}

		Withering(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, WITHER_OPENS + WITHER_TICKS + SpellConstruct.FADE);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.mouth = ground.add(0, WITHER_HIGH, 0);
			this.yaw = caster.getYRot();
			// A slash lying across the sky (RiftRenderer: a tear's yaw turns it on its watchers' screens), the roots to come out of its underside.
			this.rift = SpellConstruct.create(level, SpellConstruct.Kind.VOID_RIFT, mouth, 90, 0, (int) (end - start), caster).size((float) WITHER_RIFT).spawn();
			sound(mouth, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.6f);
			SpellFx.send(level, SpellFx.Kind.FLASH, mouth, Vec3.ZERO, 6, SpellConstruct.VOID, 192);
		}

		/** Whether {@code e} is one its roots may seize and drink: a foe within reach of the land beneath the rift. */
		private boolean within(LivingEntity e) {
			// From a little over the rift down into the land far below it (37 round and 16 down before 2026-10-24: its roots often reached nothing).
			return Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) <= WITHER_RADIUS && e.getY() <= mouth.y + WITHER_OVER && e.getY() >= ground.y - WITHER_BELOW;
		}

		/** Foes within reach not yet held, nearest first, seized by a root each, as many as it may hold. */
		private void seize(long age) {
			if (held.size() >= WITHER_GRASPS) return;
			List<LivingEntity> near = HeavenSpells.aroundPoint(level, ground, WITHER_RADIUS + WITHER_HIGH,
					e -> !held.containsKey(e) && within(e) && HeavenSpells.mark(caster, e));
			near.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(ground)));
			for (LivingEntity e : near) {
				if (held.size() >= WITHER_GRASPS) break;
				// Each out of the rift's underside a little apart, lashing down to its catch.
				Vec3 from = mouth.add((level.random.nextDouble() - 0.5) * WITHER_RIFT * 0.5, -1.2, (level.random.nextDouble() - 0.5) * WITHER_RIFT * 0.5);
				SpellConstruct root = SpellConstruct.create(level, SpellConstruct.Kind.VOID_TENDRIL, from, 0, 0, (int) (end - level.getGameTime()), caster)
						.size((float) HeavenSpells.greatness(spell.realm())).target(e).spawn();
				// Hauled up off the land, drawn in a little under the rift.
				Vec3 to = new Vec3(ground.x + (e.getX() - ground.x) * 0.6, ground.y + WITHER_LIFT, ground.z + (e.getZ() - ground.z) * 0.6);
				held.put(e, new Held(root, to, age));
				sound(from, SoundEvents.ROOTED_DIRT_BREAK, 2.5f, 0.5f);
			}
		}

		/** Each held one hauled toward where it is to hang once its root has reached it (a great beast only held where it stands); the gone let go. */
		private void haul(long age) {
			for (java.util.Iterator<java.util.Map.Entry<LivingEntity, Held>> it = held.entrySet().iterator(); it.hasNext(); ) {
				java.util.Map.Entry<LivingEntity, Held> entry = it.next();
				LivingEntity e = entry.getKey();
				Held h = entry.getValue();
				if (!HeavenSpells.present(e, level) || HeavenSpells.spared(caster, e) || Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > WITHER_RADIUS + 8) {
					h.root().dissolve();
					it.remove();
					continue;
				}
				if (age - h.since() < WITHER_LASH) continue; // its root still reaching it
				if (age - h.since() == WITHER_LASH) sound(e.position(), SoundEvents.ROOTED_DIRT_PLACE, 2.0f, 0.6f);
				// Nothing flies out of its grip (QiFlight#weighDown: a held player's flight given up while it holds; a cultivator's too).
				if (e instanceof net.minecraft.server.level.ServerPlayer player) QiFlight.weighDown(player, 10);
				if (e instanceof CultivatorNpc npc && npc.isQiFlying()) npc.setQiFlying(false);
				if (HeavenSpells.greatBeast(caster, e)) {
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));
					HeavenSpells.still(e);
				} else {
					HeavenSpells.move(e, h.to().subtract(e.position()).scale(0.22));
					e.resetFallDistance();
				}
			}
		}

		/** Its roots let go of all they hold: what they held falls, unharmed by the fall, and the roots draw back up. */
		private void release() {
			for (java.util.Map.Entry<LivingEntity, Held> entry : held.entrySet()) {
				entry.getValue().root().dissolve();
				if (HeavenSpells.present(entry.getKey(), level)) HeavenSpells.safeFall(entry.getKey(), 60);
			}
			held.clear();
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (roots == null && age >= WITHER_ROOTS) {
				// Out of the rift once it is open: the knot swelling out of it, the roots growing down out of the knot (its model's descend).
				roots = SpellConstruct.create(level, SpellConstruct.Kind.VOID_ROOTS, mouth.add(0, -2, 0), yaw, 0, (int) (end - level.getGameTime()), caster).sizedFor(spell)
						.spawn();
				sound(mouth, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.4f);
			}
			long drinking = age - WITHER_OPENS;
			if (drinking == WITHER_TICKS - 20) {
				roots.act("withdraw");
				release();
				sound(mouth, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.6f);
			}
			if (drinking >= 0 && drinking < WITHER_TICKS - 20) {
				if (drinking % 5 == 0) seize(age);
				haul(age);
			}
			if (drinking < 0 || drinking > WITHER_TICKS || drinking % 20 != 10) return true;
			// The land withers outward from beneath the rift, all of it by the last draught.
			double reach = WITHER_RADIUS * Math.min(1, (drinking + 10) / (double) WITHER_TICKS);
			Devastation.wither(caster, level, ground, withered, reach);
			withered = reach;
			float drawn = 0;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, WITHER_RADIUS + WITHER_HIGH, e -> HeavenSpells.mark(caster, e))) {
				if (!within(e) && !held.containsKey(e)) continue; // one hauled up still drinks
				drawn += HeavenSpells.strikeAgain(caster, e, WITHER_DRAIN, spell);
				e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1));
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
				SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, HeavenSpells.middle(e), mouth.subtract(HeavenSpells.middle(e)), 1, 0); // its life, drunk up into the void
			}
			if (drawn > 0) {
				List<LivingEntity> own = HeavenSpells.aroundPoint(level, ground, WITHER_RADIUS, e -> HeavenSpells.blessed(caster, e));
				if (!own.contains(caster)) own.add(caster);
				for (LivingEntity e : own) {
					HeavenSpells.mend(e, drawn / own.size());
					SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, mouth, HeavenSpells.middle(e).subtract(mouth), 0, 0); // and given to its own
				}
			}
			if (drinking % 60 == 10) sound(mouth, SoundEvents.ROOTED_DIRT_PLACE, 3.0f, 0.4f);
			return true;
		}

		@Override
		public void end() {
			release();
			rift.dissolve();
			if (roots != null) roots.dissolve();
		}
	}

	// --- Sacred Vessel ---

	/**
	 * Heaven-Swallowing Gourd: a gourd of living jade wood on its vine (SpellConstruct SWALLOWING_GOURD, its own GeckoLib model: gold bands
	 * of charms round its waist, its mouth burning green) comes down out of the sky mouth first, to hang {@link #GOURD_HIGH} over where its
	 * caster looks ({@link #GOURD_DESCENDS}). Its stopper flies off and it draws for {@link #GOURD_TICKS}: the land under it is torn up into
	 * it, a bowl {@link #GOURD_STRIP} round, and every foe within {@link #GOURD_RADIUS} is dragged up toward its mouth; whatever comes within
	 * {@link #GOURD_MOUTH} of it is swallowed, held inside and drained of {@link #GOURD_DRAIN} a second, weakened (a great beast is only
	 * dragged and drained where it stands). Then it spits them out for {@link #GOURD_SPIT}, hurled far, and goes.
	 */
	public static final class Gourd extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, mouth;
		private final SpellConstruct gourd;
		private final java.util.Set<LivingEntity> swallowed = new java.util.LinkedHashSet<>();

		Gourd(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, GOURD_DESCENDS + GOURD_TICKS + GOURD_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.mouth = ground.add(0, GOURD_HIGH, 0);
			this.gourd = SpellConstruct.create(level, SpellConstruct.Kind.SWALLOWING_GOURD, mouth, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(mouth, SoundEvents.AZALEA_LEAVES_PLACE, 3.0f, 0.4f);
			sound(mouth, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.6f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < GOURD_DESCENDS) return true;
			long drawing = age - GOURD_DESCENDS;
			if (drawing == 0) {
				// The stopper flies, and the land under it is torn up into it.
				SpellFx.send(level, SpellFx.Kind.LEAVES, mouth, Vec3.ZERO, 4, 0, 192);
				Devastation.crater(caster, level, ground, GOURD_STRIP, GOURD_STRIP_DEPTH, Devastation.Floor.RUBBLE);
				sound(mouth, SoundEvents.BOTTLE_EMPTY, 4.0f, 0.3f);
			}
			if (drawing < GOURD_TICKS) {
				draw(drawing);
				return true;
			}
			if (drawing == GOURD_TICKS) spit();
			return true;
		}

		/** A tick of its drawing: foes dragged up to it, the swallowed held and drained, clods and leaves flying up into its mouth. */
		private void draw(long drawing) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, mouth, GOURD_RADIUS + GOURD_HIGH, e -> !HeavenSpells.spared(caster, e))) {
				if (swallowed.contains(e)) continue;
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > GOURD_RADIUS || e.getY() > mouth.y + 4 || e.getY() < ground.y - 12) continue;
				Vec3 to = mouth.subtract(HeavenSpells.middle(e));
				double d = to.length();
				if (d <= GOURD_MOUTH && !HeavenSpells.greatBeast(caster, e)) {
					swallowed.add(e);
					SpellFx.send(level, SpellFx.Kind.LEAVES, HeavenSpells.middle(e), Vec3.ZERO, 2, 0);
					sound(mouth, SoundEvents.GENERIC_DRINK, 3.0f, 0.4f);
					continue;
				}
				double pull = HeavenSpells.greatBeast(caster, e) ? 0.12 : 0.35 + 0.9 * (1 - Math.min(1, d / (GOURD_RADIUS + GOURD_HIGH)));
				HeavenSpells.move(e, to.normalize().scale(pull));
				HeavenSpells.safeFall(e, 100);
				if (HeavenSpells.greatBeast(caster, e) && drawing % 20 == 10) HeavenSpells.strikeAgain(caster, e, GOURD_DRAIN, spell);
			}
			// The swallowed held in it (just inside its mouth), drained.
			Vec3 inside = mouth.add(0, GOURD_MOUTH * 0.6, 0);
			for (java.util.Iterator<LivingEntity> it = swallowed.iterator(); it.hasNext(); ) {
				LivingEntity e = it.next();
				if (!HeavenSpells.present(e, level)) {
					it.remove();
					continue;
				}
				HeavenSpells.teleport(e, inside.subtract(0, e.getBbHeight() / 2, 0));
				HeavenSpells.safeFall(e, 100);
				if (drawing % 20 == 10) {
					HeavenSpells.strikeAgain(caster, e, GOURD_DRAIN, spell);
					e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2));
				}
			}
			if (drawing % 2 == 0) {
				// Clods of the land and its leaves flying up into it.
				for (int i = 0; i < 3; i++) {
					double angle = level.random.nextDouble() * Math.PI * 2, out = Math.sqrt(level.random.nextDouble()) * GOURD_RADIUS * 0.9;
					BlockPos top = Devastation.groundTop(level, Mth.floor(ground.x + Math.cos(angle) * out), Mth.floor(ground.z + Math.sin(angle) * out));
					if (top == null) continue;
					BlockState state = HeavenSpells.asEarth(level.getBlockState(top));
					if (state.isAir()) continue;
					Vec3 from = Vec3.atBottomCenterOf(top.above());
					SpellBlock.show(level, state, from, inside.subtract(from), 0, 14 + level.random.nextInt(8), SpellBlock.Motion.FLY, 0);
				}
			}
			if (drawing % 4 == 0) SpellFx.send(level, SpellFx.Kind.LEAVES, ground.add(0, 2, 0), mouth.subtract(ground), 3, 0, 160);
			if (drawing % 20 == 0) sound(mouth, SoundEvents.ELYTRA_FLYING, 3.0f, 0.5f);
		}

		/** It spits them out: the swallowed struck and hurled far; anything still being dragged flung away too. */
		private void spit() {
			gourd.act("spit");
			for (LivingEntity e : swallowed) {
				if (!HeavenSpells.present(e, level)) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, GOURD_SPIT, spell);
				double angle = level.random.nextDouble() * Math.PI * 2;
				HeavenSpells.move(e, new Vec3(Math.cos(angle) * 2.2, -0.6, Math.sin(angle) * 2.2));
				HeavenSpells.safeFall(e, 120);
			}
			swallowed.clear();
			SpellFx.send(level, SpellFx.Kind.LEAVES, mouth, Vec3.ZERO, 8, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) GOURD_RADIUS, 0, 192);
			sound(mouth, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.6f);
			sound(mouth, SoundEvents.SNIFFER_SNIFFING, 4.0f, 0.4f);
		}

		@Override
		public void end() {
			swallowed.clear();
			gourd.dissolve();
		}
	}

	/**
	 * Seven-Treasure Bodhi Branch (made over, 2026-10-25, the user's ask: "the move that looks the stupidest by far is Seven-Treasure Bodhi
	 * Branch, please either redo it completely or just swap it out for something else"; it was a stick swept across its caster's front): a
	 * branch of the bodhi tree, hung with the seven treasures, is planted where its caster looks (up to {@link #BRANCH_REACH}) and bursts up out
	 * of the land into a towering bodhi tree of the world's own wood and leaves (SpellConstruct BODHI_TREE: BlockMasses; {@link #BODHI_GROWS}),
	 * {@link #BODHI_HEIGHT} tall, the land round its foot heaving. As it grows its roots burst up out of the land round it and seize every foe
	 * within {@link #BODHI_RADIUS} (SpellConstruct LIVING_ROOT: Rooting), holding them where they stand. Then its seven treasures, hanging
	 * glowing from its boughs (gold, silver, lapis, crystal, the white of the giant clam, red coral, carnelian), fall one after another
	 * ({@link #BODHI_EVERY} apart), each on one of those it holds in turn: each bursts in its own light, {@link #BODHI_TREASURE} to everything
	 * within {@link #BODHI_BURST} of where it falls, thrown out. Then the tree sheds its leaves in a storm of them and sinks back into the land
	 * ({@link #BODHI_SHEDS}), letting them go.
	 */
	public static final class Branch extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct tree;
		private final Rooting rooting;
		private final List<LivingEntity> held = new ArrayList<>();
		private int fallen;

		Branch(LivingEntity caster, Spell spell) {
			super(caster, BODHI_GROWS + BODHI_HOLDS + 7 * BODHI_EVERY + BODHI_SHEDS);
			this.spell = spell;
			Vec3 point = HeavenSpells.lookPoint(caster, BRANCH_REACH);
			if (point.distanceToSqr(caster.position()) < 12 * 12) point = caster.position().add(Vec3.directionFromRotation(0, caster.getYRot()).scale(14));
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.tree = SpellConstruct.create(level, SpellConstruct.Kind.BODHI_TREE, ground, 0, 0, (int) (end - start) + SpellConstruct.FADE, caster)
					.size((float) BODHI_HEIGHT).tint(level.random.nextInt()).spawn();
			this.rooting = new Rooting(caster, level, ground, 2.2f);
			sound(ground, SoundEvents.ROOTED_DIRT_BREAK, 4.0f, 0.4f);
			sound(ground, SoundEvents.WARDEN_EMERGE, 3.0f, 1.3f);
			sound(ground, SoundEvents.AMETHYST_BLOCK_CHIME, 3.0f, 0.5f);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, 6, net.minecraft.world.level.block.Block.getId(Blocks.ROOTED_DIRT.defaultBlockState()), 192);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < BODHI_GROWS) {
				// It bursts up out of the land, groaning as it grows, its leaves unfurling.
				if (age % 5 == 0) {
					sound(ground.add(0, BODHI_HEIGHT * age / BODHI_GROWS, 0), SoundEvents.WOOD_BREAK, 4.0f, 0.35f + 0.25f * level.random.nextFloat());
					SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) BODHI_HEIGHT * 0.5f, 0, 192);
				}
				if (age % 7 == 3) sound(ground.add(0, BODHI_HEIGHT * 0.6, 0), SoundEvents.AZALEA_LEAVES_PLACE, 4.0f, 0.5f);
				if (age == BODHI_GROWS / 3) Devastation.fissures(caster, level, ground, BODHI_HEIGHT * 0.55, 8, 3, BODHI_HEIGHT * 0.12);
				if (age == BODHI_GROWS / 2) seize();
				return true;
			}
			// Its roots keep their hold (a root to each held foe: Rooting).
			held.removeIf(e -> !HeavenSpells.present(e, level));
			rooting.hold(held, (int) (end - level.getGameTime()));
			for (LivingEntity e : held) HeavenSpells.still(e);
			long falling = age - BODHI_GROWS - BODHI_HOLDS;
			if (falling >= 0 && falling % BODHI_EVERY == 0 && fallen < 7) fall(fallen++);
			if (fallen >= 7 && falling == 7L * BODHI_EVERY + 6) shed();
			if (age % 9 == 0) {
				Vec3 crown = ground.add((level.random.nextDouble() - 0.5) * BODHI_HEIGHT * 0.6, BODHI_HEIGHT * (0.5 + 0.3 * level.random.nextDouble()),
						(level.random.nextDouble() - 0.5) * BODHI_HEIGHT * 0.6);
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES, crown.x, crown.y, crown.z, 4, 2, 1, 2, 0.0);
			}
			return true;
		}

		/** Its roots burst up out of the land round it and seize every foe near, binding them where they stand. */
		private void seize() {
			held.addAll(HeavenSpells.aroundPoint(level, ground, BODHI_RADIUS, e -> HeavenSpells.mark(caster, e)));
			held.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(ground)));
			int holding = (int) (end - level.getGameTime());
			for (LivingEntity e : held) WoodSpells.bind(caster, e, holding);
			rooting.hold(held, holding);
			sound(ground, SoundEvents.ROOTED_DIRT_PLACE, 4.0f, 0.4f);
			sound(ground, SoundEvents.MANGROVE_ROOTS_BREAK, 4.0f, 0.45f);
		}

		/** Treasure {@code i} falls from its bough onto the next it holds (or round its foot, with none), and bursts in its own light. */
		private void fall(int i) {
			tree.value(i + 1);
			Vec3 from = ground.add(treasureAt(i, BODHI_HEIGHT));
			LivingEntity target = held.isEmpty() ? null : held.get(i % held.size());
			Vec3 to;
			if (target != null) to = HeavenSpells.middle(target);
			else {
				double a = level.random.nextDouble() * Math.PI * 2, r = BODHI_RADIUS * (0.3 + 0.5 * level.random.nextDouble());
				Vec3 spot = ground.add(Math.cos(a) * r, 4, Math.sin(a) * r), land = HeavenSpells.groundBelow(level, spot, 8, 24);
				to = land != null ? land : spot.subtract(0, 4, 0);
			}
			int colour = TREASURES[i];
			SpellFx.send(level, SpellFx.Kind.STREAK, from, to.subtract(from), 0, colour, 192);
			sound(from, SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0f, 0.6f + i * 0.12f);
			Vec3 strike = to;
			HeavenSpells.linger(new FireSpells.Delayed(caster, 4, () -> burst(strike, colour, i)));
		}

		/** A treasure bursts where it fell: all near struck and thrown out, in its own light. */
		private void burst(Vec3 at, int colour, int i) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, BODHI_BURST + 2, e -> !HeavenSpells.spared(caster, e))) {
				if (HeavenSpells.middle(e).distanceTo(at) > BODHI_BURST + e.getBbWidth()) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, BODHI_TREASURE, spell);
				if (!held.contains(e)) HeavenSpells.push(at, e, 1.2);
			}
			SpellFx.send(level, SpellFx.Kind.FLASH, at, Vec3.ZERO, 9, colour, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at.subtract(0, 0.8, 0), Vec3.ZERO, (float) BODHI_BURST * 1.6f, 0, 192);
			SpellFx.send(level, SpellFx.Kind.LEAVES, at, Vec3.ZERO, 2.5f, 0, 160);
			HeavenSpells.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 3.5f, 0.5f + i * 0.08f);
			HeavenSpells.sound(level, at, SoundEvents.BELL_BLOCK, 2.5f, 0.6f + i * 0.1f);
			HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 2.0f, 1.1f);
		}

		/** It sheds its leaves in a storm of them and sinks back into the land, its roots letting go. */
		private void shed() {
			tree.act("shed");
			rooting.release();
			Vec3 crown = ground.add(0, BODHI_HEIGHT * 0.65, 0);
			SpellFx.send(level, SpellFx.Kind.LEAVES, crown, Vec3.ZERO, (float) BODHI_HEIGHT * 0.5f, 0, 256);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES, crown.x, crown.y, crown.z, 120, BODHI_HEIGHT * 0.35, BODHI_HEIGHT * 0.25, BODHI_HEIGHT * 0.35, 0.02);
			sound(crown, SoundEvents.AZALEA_LEAVES_BREAK, 5.0f, 0.4f);
			sound(ground, SoundEvents.ROOTED_DIRT_BREAK, 4.0f, 0.3f);
		}

		@Override
		public void end() {
			rooting.release();
			for (LivingEntity e : held) if (HeavenSpells.present(e, level)) HeavenSpells.safeFall(e, 40);
			held.clear();
			tree.dissolve();
		}
	}

	// --- Entering Nirvana ---

	/**
	 * Seed of Withering Rebirth: a seed half withered and half alive (SpellConstruct REBIRTH_SEED, its own GeckoLib model) shot from its caster's
	 * hands along their look (the user's ask, 2026-10-25: "if I shot it and it could only activate when it hits the ground, so it flys through the
	 * air but will only plant in the ground, also make it so a player will get trapped in it if they are hit by the seed, and if no player it
	 * just does what it normally does"). It flies on, falling a little as it goes ({@link #SEED_SPEED}, {@link #SEED_FALL}), for as long as
	 * {@link #SEED_FLIES}:
	 * <ul>
	 *   <li>Striking a being on its way (one its caster's spells don't spare), it roots in it and grows about it: the being is sealed in a pod
	 *       of mangrove roots, flowering on one side and bare on the other (blocks set down for a while: HeavenSpells#placeTemporary), bound
	 *       where it stands, and for {@link #SEED_ROOTS} the seed drinks {@link #SEED_DRAIN} a second out of it, {@link #SEED_MENDS} of it
	 *       mending its caster. Then the flower of rebirth bursts the pod open out of it: {@link #SEED_BLOOM} to it,
	 *       {@link #SEED_BLOOM_AROUND} to every other foe within {@link #SEED_BURST}.</li>
	 *   <li>Striking the ground, it plants itself there: living roots reach up out of the land to every foe within {@link #SEED_REACH} of it
	 *       (SpellConstruct LIVING_ROOT), binding them where they stand and drinking {@link #SEED_DRAIN_EACH} a second out of each for
	 *       {@link #SEED_ROOTS}, as much mending its caster; then the flower blooms out of the seed: {@link #SEED_BLOOM} to every foe its roots
	 *       held, {@link #SEED_BLOOM_AROUND} to every other within {@link #SEED_BURST}.</li>
	 * </ul>
	 * Either way the land round it flourishes ({@link #SEED_FLOURISH}); the flower holds a while in bloom, then withers. Striking nothing (shot
	 * into the open sky), it withers in the air.
	 */
	public static final class Seed extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellConstruct seed;
		private Vec3 at, velocity;
		/** The being it struck and sealed in its pod (null: none). */
		private LivingEntity trapped;
		/** Where it planted itself (null: it hasn't, or it is in a being). */
		private Vec3 planted;
		/** When it rooted (in a being or the ground), ticks in; -1, still flying. */
		private long rooted = -1;
		private final java.util.Map<LivingEntity, SpellConstruct> roots = new java.util.LinkedHashMap<>();
		private final List<BlockPos> pod = new ArrayList<>();

		Seed(LivingEntity caster, Spell spell) {
			super(caster, SEED_FLIES + SEED_ROOTS + SEED_HOLDS + SEED_WITHERS);
			this.spell = spell;
			this.at = HeavenSpells.fromHands(caster);
			this.velocity = caster.getViewVector(1.0f).scale(SEED_SPEED);
			float[] face = HeavenSpells.facing(velocity);
			this.seed = SpellConstruct.create(level, SpellConstruct.Kind.REBIRTH_SEED, at, face[0], 90 + face[1], (int) (end - start) + SpellConstruct.FADE, caster)
					.sizedFor(spell).spawn();
			sound(at, SoundEvents.AZALEA_PLACE, 2.0f, 0.5f);
			sound(at, SoundEvents.TRIDENT_THROW, 1.6f, 0.6f);
			sound(at, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0f, 0.6f);
		}

		/** It has rooted, {@code age} ticks in: the rest of its life counted from now. */
		private void rootedNow(long age) {
			rooted = age;
			end = level.getGameTime() + SEED_ROOTS + SEED_HOLDS + SEED_WITHERS;
		}

		@Override
		protected boolean run(long age, long now) {
			if (rooted < 0) return fly(age);
			long rooting = age - rooted;
			if (rooting < SEED_ROOTS) drink(rooting);
			if (rooting == SEED_ROOTS) bloom();
			if (rooting == SEED_ROOTS + SEED_HOLDS) {
				seed.act("wither");
				sound(seed.position(), SoundEvents.AZALEA_LEAVES_BREAK, 2.0f, 0.4f);
			}
			return true;
		}

		/** A tick of its flight: on along its way, falling a little; a being struck, or the ground, and it roots. */
		private boolean fly(long age) {
			if (age > SEED_FLIES) {
				// Shot into the open: it withers in the air.
				seed.act("wither");
				rooted = age - SEED_ROOTS - SEED_HOLDS - 1;
				end = level.getGameTime() + SEED_WITHERS;
				return true;
			}
			Vec3 next = at.add(velocity);
			net.minecraft.world.phys.BlockHitResult block = level.clip(new net.minecraft.world.level.ClipContext(at, next,
					net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.ANY, caster));
			Vec3 reach = block.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? next : block.getLocation();
			net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, caster, at, reach,
					new net.minecraft.world.phys.AABB(at, reach).inflate(1.2), e -> e instanceof LivingEntity living && living.isAlive() && !HeavenSpells.spared(caster, living));
			if (hit != null && hit.getEntity() instanceof LivingEntity struck) {
				trap(struck, age);
				return true;
			}
			if (block.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
				plant(block.getLocation(), age);
				return true;
			}
			at = next;
			velocity = velocity.scale(0.995).add(0, -SEED_FALL, 0);
			float[] face = HeavenSpells.facing(velocity);
			seed.place(at, face[0], 90 + face[1]);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES, at.x, at.y, at.z, 2, 0.2, 0.2, 0.2, 0.0);
			if (age % 2 == 0) level.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0.0);
			return true;
		}

		/** It strikes {@code e} and grows about it: sealed in a pod of roots, flowering on one side and bare on the other, bound where it stands. */
		private void trap(LivingEntity e, long age) {
			trapped = e;
			rootedNow(age);
			seed.act("root");
			WoodSpells.bind(caster, e, SEED_ROOTS + 4);
			e.invulnerableTime = 0;
			HeavenSpells.strike(caster, e, SEED_STRUCK, spell);
			net.minecraft.world.phys.AABB box = e.getBoundingBox();
			int x0 = Mth.floor(box.minX) - 1, y0 = Mth.floor(box.minY) - 1, z0 = Mth.floor(box.minZ) - 1;
			int x1 = Mth.floor(box.maxX) + 1, y1 = Mth.floor(box.maxY) + 1, z1 = Mth.floor(box.maxZ) + 1;
			double faceX = caster.getX() - e.getX(), faceZ = caster.getZ() - e.getZ();
			for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
				boolean shell = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
				if (!shell) continue;
				BlockPos pos = new BlockPos(x, y, z);
				// Its living side toward its caster flowers; its withered side is bare roots.
				boolean alive = (x + 0.5 - e.getX()) * faceX + (z + 0.5 - e.getZ()) * faceZ > 0;
				BlockState part = y == y1 || alive && level.random.nextInt(3) == 0 ? Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState()
						.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true) : Blocks.MANGROVE_ROOTS.defaultBlockState();
				if (HeavenSpells.placeTemporary(level, pos, part, SEED_ROOTS + 6)) pod.add(pos);
			}
			SpellFx.send(level, SpellFx.Kind.LEAVES, HeavenSpells.middle(e), Vec3.ZERO, 3f, 0, 160);
			sound(e.position(), SoundEvents.ROOTED_DIRT_PLACE, 3.0f, 0.5f);
			sound(e.position(), SoundEvents.MANGROVE_ROOTS_PLACE, 3.0f, 0.6f);
			sound(e.position(), SoundEvents.AZALEA_LEAVES_PLACE, 3.0f, 0.5f);
		}

		/** It strikes the ground and plants itself: living roots reach up out of the land to every foe near, binding them. */
		private void plant(Vec3 where, long age) {
			planted = where;
			rootedNow(age);
			seed.place(where, seed.getYRot(), 0);
			seed.act("root");
			for (LivingEntity e : HeavenSpells.aroundPoint(level, where, SEED_REACH, e -> HeavenSpells.mark(caster, e))) {
				roots.put(e, SpellConstruct.create(level, SpellConstruct.Kind.LIVING_ROOT, where.add(0, 0.1, 0), 0, 0, SEED_ROOTS + SpellConstruct.FADE * 2, caster)
						.size(1.6f).target(e).spawn());
				WoodSpells.bind(caster, e, SEED_ROOTS);
			}
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, where, Vec3.ZERO, 3, net.minecraft.world.level.block.Block.getId(Blocks.ROOTED_DIRT.defaultBlockState()), 160);
			SpellFx.send(level, SpellFx.Kind.LEAVES, where, Vec3.ZERO, 3.5f, 0, 160);
			sound(where, SoundEvents.ROOTED_DIRT_PLACE, 3.0f, 0.5f);
			sound(where, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.4f);
			if (!roots.isEmpty()) sound(where, SoundEvents.MANGROVE_ROOTS_BREAK, 3.0f, 0.5f);
		}

		/** A tick of its drinking: once a second, out of what it holds, mending its caster. */
		private void drink(long rooting) {
			if (trapped != null) {
				if (HeavenSpells.present(trapped, level)) seed.place(HeavenSpells.middle(trapped), seed.getYRot(), 0);
				if (rooting % 20 == 10 && HeavenSpells.present(trapped, level)) {
					float took = HeavenSpells.strikeAgain(caster, trapped, SEED_DRAIN, spell);
					HeavenSpells.mend(caster, took * SEED_MENDS);
					SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, HeavenSpells.middle(trapped), HeavenSpells.middle(caster).subtract(HeavenSpells.middle(trapped)), 2, 0);
					sound(trapped.position(), SoundEvents.ROOTED_DIRT_BREAK, 2.0f, 0.6f);
				}
				return;
			}
			if (rooting % 20 != 10) return;
			float took = 0;
			for (LivingEntity e : roots.keySet()) {
				if (!HeavenSpells.present(e, level)) continue;
				took += HeavenSpells.strikeAgain(caster, e, SEED_DRAIN_EACH, spell);
			}
			if (took > 0) {
				HeavenSpells.mend(caster, took * SEED_MENDS);
				SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, planted.add(0, 1, 0), HeavenSpells.middle(caster).subtract(planted.add(0, 1, 0)), 2, 0);
				sound(planted, SoundEvents.ROOTED_DIRT_BREAK, 2.0f, 0.6f);
			}
		}

		/** It blooms: the flower of rebirth thrown open (bursting its pod, out of the being it held, or out of the ground), its burst round it. */
		private void bloom() {
			seed.act("bloom");
			Vec3 heart = trapped != null && HeavenSpells.present(trapped, level) ? HeavenSpells.middle(trapped) : planted != null ? planted : seed.position();
			java.util.Set<LivingEntity> held = new java.util.HashSet<>(roots.keySet());
			if (trapped != null) held.add(trapped);
			for (LivingEntity e : held) {
				if (!HeavenSpells.present(e, level)) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, SEED_BLOOM, spell);
			}
			for (LivingEntity e : HeavenSpells.aroundPoint(level, heart, SEED_BURST + 2, e -> !held.contains(e) && !HeavenSpells.spared(caster, e))) {
				if (HeavenSpells.middle(e).distanceTo(heart) > SEED_BURST) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, SEED_BLOOM_AROUND, spell);
				HeavenSpells.push(heart, e, 1.0);
			}
			// The pod burst open, its roots and blossom thrown off.
			for (int i = 0; i < pod.size(); i++) {
				BlockPos pos = pod.get(i);
				if (i % 3 == 0) level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(level.getBlockState(pos)));
				HeavenSpells.removeTemporary(level, pos);
			}
			pod.clear();
			for (SpellConstruct root : roots.values()) root.dissolve();
			Vec3 below = HeavenSpells.groundBelow(level, heart, 2, 24);
			Devastation.flourish(caster, level, below != null ? below : heart, 0, SEED_FLOURISH, 0.35);
			SpellFx.send(level, SpellFx.Kind.LEAVES, heart, Vec3.ZERO, 8, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, heart.add(0, 2, 0), Vec3.ZERO, 10, REBIRTH_BLUSH, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, below != null ? below : heart, Vec3.ZERO, (float) SEED_BURST, 0, 192);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES, heart.x, heart.y + 1, heart.z, 60, SEED_BURST * 0.3, 1.5, SEED_BURST * 0.3, 0.02);
			sound(heart, SoundEvents.AMETHYST_CLUSTER_BREAK, 4.0f, 0.5f);
			sound(heart, SoundEvents.BEACON_POWER_SELECT, 3.0f, 1.2f);
			sound(heart, SoundEvents.AZALEA_LEAVES_BREAK, 4.0f, 0.6f);
		}

		@Override
		public void end() {
			for (BlockPos pos : pod) HeavenSpells.removeTemporary(level, pos);
			pod.clear();
			for (SpellConstruct root : roots.values()) root.dissolve();
			seed.dissolve();
		}
	}

	/**
	 * Withering and Flourishing: the wheel of the seasons, a taiji of spring's green and autumn's dry earth round a tree half in blossom and half
	 * bare (SpellConstruct SEASON_WHEEL, its own GeckoLib model), grows out of the land where its caster looks, {@link #SEASONS_RADIUS} round
	 * ({@link #SEASONS_GROWS}), and turns for {@link #SEASONS_TICKS}: every foe within is drained of {@link #SEASONS_DRAIN} a second, slowed, and
	 * {@link #SEASONS_MENDS} of all it drains is shared out mending its caster and their own within; the land to their left flourishes, to their
	 * right withers. Then its last turn: {@link #SEASONS_TURN} to every foe within, {@link #SEASONS_TURN_MENDS} of it mending its caster.
	 */
	public static final class Seasons extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final float yaw;
		private final SpellConstruct wheel;
		/** Its roots, from the tree at its heart to every foe it drinks from (Rooting). */
		private final Rooting rooting;

		Seasons(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, SEASONS_GROWS + SEASONS_TICKS + SEASONS_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.yaw = caster.getYRot();
			this.wheel = SpellConstruct.create(level, SpellConstruct.Kind.SEASON_WHEEL, ground.add(0, 0.1, 0), yaw, 0, (int) (end - start), caster).sizedFor(spell).spawn();
			this.rooting = new Rooting(caster, level, ground, 1.8f);
			sound(ground, SoundEvents.AZALEA_LEAVES_PLACE, 3.0f, 0.4f);
			sound(ground, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.6f);
		}

		/** Whether {@code e} stands within the wheel. */
		private boolean within(LivingEntity e) {
			return Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) <= SEASONS_RADIUS && e.getY() < ground.y + 16 && e.getY() > ground.y - 10;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == SEASONS_GROWS) {
				// The land under it: flourishing on its caster's left half, withered on their right.
				Vec3 left = Vec3.directionFromRotation(0, yaw - 90).scale(SEASONS_RADIUS / 2);
				Devastation.flourish(caster, level, ground.add(left), 0, SEASONS_RADIUS / 2, 0.3);
				Devastation.wither(caster, level, ground.subtract(left), 0, SEASONS_RADIUS / 2);
			}
			if (age < SEASONS_GROWS) return true;
			long turning = age - SEASONS_GROWS;
			if (turning < SEASONS_TICKS && turning % 20 == 10) drink();
			if (turning % 4 == 0 && turning < SEASONS_TICKS) {
				double a = level.random.nextDouble() * Math.PI * 2, r = level.random.nextDouble() * SEASONS_RADIUS;
				SpellFx.send(level, SpellFx.Kind.LEAVES, ground.add(Math.cos(a) * r, 2 + level.random.nextDouble() * 4, Math.sin(a) * r), Vec3.ZERO, 2, 0, 160);
			}
			if (turning == SEASONS_TICKS) turn();
			return true;
		}

		/** A second of its turning: every foe within drained and slowed, a share of it mending its caster and their own within. */
		private void drink() {
			float drunk = 0;
			List<LivingEntity> drinking = HeavenSpells.aroundPoint(level, ground, SEASONS_RADIUS + 16, e -> !HeavenSpells.spared(caster, e) && within(e));
			rooting.hold(drinking, (int) (end - level.getGameTime()));
			for (LivingEntity e : drinking) {
				drunk += HeavenSpells.strikeAgain(caster, e, SEASONS_DRAIN, spell);
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
				SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, HeavenSpells.middle(e), ground.add(0, 3, 0).subtract(HeavenSpells.middle(e)), 1.5f, 0);
			}
			List<LivingEntity> own = new ArrayList<>(HeavenSpells.aroundPoint(level, ground, SEASONS_RADIUS + 16, e -> HeavenSpells.own(caster, e) && within(e)));
			if (within(caster) || own.isEmpty()) own.add(caster);
			for (LivingEntity e : own) HeavenSpells.mend(e, drunk * SEASONS_MENDS / own.size());
			sound(ground, SoundEvents.AZALEA_LEAVES_HIT, 3.0f, 0.5f);
		}

		/** Its last turn: the wheel spun round, every foe within struck, a share of it mending its caster. */
		private void turn() {
			wheel.act("flip");
			rooting.release();
			float dealt = 0;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, SEASONS_RADIUS + 16, e -> !HeavenSpells.spared(caster, e))) {
				if (!within(e)) continue;
				e.invulnerableTime = 0;
				dealt += HeavenSpells.strike(caster, e, SEASONS_TURN, spell);
				HeavenSpells.push(ground, e, 1.2);
			}
			HeavenSpells.mend(caster, dealt * SEASONS_TURN_MENDS);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) SEASONS_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.LEAVES, ground.add(0, 3, 0), Vec3.ZERO, 10, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 4, 0), Vec3.ZERO, 12, REBIRTH_BLUSH, 192);
			sound(ground, SoundEvents.BEACON_POWER_SELECT, 4.0f, 0.6f);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.8f);
		}

		@Override
		public void end() {
			rooting.release();
			wheel.dissolve();
		}
	}

	// --- True Immortal ---

	/** Everything within {@code radius} of {@code at} (level, by its feet) and between {@code below} under it and {@code above} over it that {@code caster}'s spells don't spare. */
	static List<LivingEntity> foesWithin(LivingEntity caster, ServerLevel level, Vec3 at, double radius, double below, double above) {
		return HeavenSpells.aroundPoint(level, at, radius + above + below, e -> !HeavenSpells.spared(caster, e)
				&& Math.hypot(e.getX() - at.x, e.getZ() - at.z) <= radius + e.getBbWidth() / 2 && e.getY() <= at.y + above && e.getY() >= at.y - below);
	}

	/** {@code amount} shared out mending {@code caster} and their own within {@code radius} of {@code at}. */
	static void shareMending(LivingEntity caster, ServerLevel level, Vec3 at, double radius, float amount) {
		if (amount <= 0) return;
		List<LivingEntity> own = new ArrayList<>(HeavenSpells.aroundPoint(level, at, radius, e -> HeavenSpells.own(caster, e)));
		own.add(caster);
		for (LivingEntity e : own) HeavenSpells.mend(e, amount / own.size());
	}

	/**
	 * Primal Wind of Creation: a cyclone of the primal wind, green-gold, the leaves and seeds of creation whirled up in it (SpellConstruct
	 * PRIMORDIAL_CYCLONE, its own GeckoLib model), forms where its caster looks ({@link #WIND_FORMS}) and wanders on along their look for
	 * {@link #WIND_TICKS}, {@link #WIND_SPEED} a tick: every foe within {@link #WIND_PULL} is drawn in to it, and those within {@link #WIND_RADIUS}
	 * whirled up round it as high as {@link #WIND_HIGH} (a great beast only drawn, slowly) and struck for {@link #WIND_DAMAGE} every
	 * {@link #WIND_EVERY} ticks; the land it passes over flourishes. At its end it flings them out, {@link #WIND_FLING} to each, and dies away;
	 * {@link #WIND_MENDS} of all its harm mends its caster and their own near it.
	 */
	public static final class Wind extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 way;
		private final float yaw;
		private final SpellConstruct cyclone;
		private Vec3 foot;
		private float dealt;
		private boolean flung;

		Wind(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, WIND_FORMS + WIND_TICKS + WIND_FADES);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.foot = below != null ? below : point;
			this.yaw = caster.getYRot();
			this.way = Vec3.directionFromRotation(0, yaw);
			this.cyclone = SpellConstruct.create(level, SpellConstruct.Kind.PRIMORDIAL_CYCLONE, foot, yaw, 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(foot, SoundEvents.ELYTRA_FLYING, 4.0f, 0.5f);
			sound(foot, SoundEvents.AZALEA_LEAVES_PLACE, 3.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < WIND_FORMS) return true;
			long whirling = age - WIND_FORMS;
			if (whirling < WIND_TICKS) {
				// It wanders on along its caster's look, its foot on the land.
				Vec3 next = foot.add(way.scale(WIND_SPEED)), below = HeavenSpells.groundBelow(level, next.add(0, 3, 0), 6, 12);
				foot = below != null ? below : next;
				cyclone.place(foot, yaw, 0);
				whirl(whirling % WIND_EVERY == 0);
				if (whirling % 10 == 0) Devastation.flourish(caster, level, foot, 0, WIND_RADIUS, 0.3);
				if (whirling % 3 == 0) {
					double a = level.random.nextDouble() * Math.PI * 2, r = WIND_RADIUS * (0.3 + 0.7 * level.random.nextDouble());
					SpellFx.send(level, SpellFx.Kind.LEAVES, foot.add(Math.cos(a) * r, 2 + level.random.nextDouble() * WIND_HIGH * 1.5, Math.sin(a) * r), Vec3.ZERO, 2.5f, 0, 192);
				}
				if (whirling % 24 == 0) sound(foot, SoundEvents.ELYTRA_FLYING, 4.0f, 0.4f + 0.02f * (whirling / 24));
			} else if (!flung) {
				flung = true;
				fling();
			}
			return true;
		}

		/** A tick of its whirling: foes near drawn in, those in its funnel whirled up round it; struck if {@code strike}. */
		private void whirl(boolean strike) {
			for (LivingEntity e : foesWithin(caster, level, foot, WIND_PULL, 8, WIND_HIGH + 12)) {
				double dx = e.getX() - foot.x, dz = e.getZ() - foot.z, d = Math.max(1.0e-3, Math.hypot(dx, dz));
				Vec3 in = new Vec3(-dx / d, 0, -dz / d), round = new Vec3(-in.z, 0, in.x), v = e.getDeltaMovement();
				boolean held = d <= WIND_RADIUS;
				if (!held || HeavenSpells.greatBeast(caster, e)) {
					double pull = HeavenSpells.greatBeast(caster, e) ? 0.12 : 0.5;
					HeavenSpells.move(e, new Vec3((in.x + round.x * 0.5) * pull, v.y, (in.z + round.z * 0.5) * pull));
				} else {
					// Whirled up round it, carried as high as its funnel is wide there.
					double lift = Mth.clamp((foot.y + WIND_HIGH * (0.4 + 0.6 * d / WIND_RADIUS) - e.getY()) * 0.12, -0.4, 0.6);
					HeavenSpells.move(e, round.scale(0.9).add(in.scale(0.25 * d / WIND_RADIUS)).add(0, lift, 0));
					HeavenSpells.safeFall(e, 60);
				}
				if (held && strike) {
					dealt += HeavenSpells.strikeAgain(caster, e, WIND_DAMAGE, spell);
					SpellFx.send(level, SpellFx.Kind.LEAVES, HeavenSpells.middle(e), Vec3.ZERO, 1.5f, 0);
				}
			}
		}

		/** Its end: everything in its funnel flung out and struck, a share of all its harm mending its caster and their own. */
		private void fling() {
			cyclone.act("fade");
			for (LivingEntity e : foesWithin(caster, level, foot, WIND_RADIUS + 2, 8, WIND_HIGH + 12)) {
				double dx = e.getX() - foot.x, dz = e.getZ() - foot.z, d = Math.max(1.0e-3, Math.hypot(dx, dz));
				e.invulnerableTime = 0;
				dealt += HeavenSpells.strike(caster, e, WIND_FLING, spell);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, new Vec3(dx / d * 2.2, 0.8, dz / d * 2.2));
				HeavenSpells.safeFall(e, 100);
			}
			shareMending(caster, level, foot, WIND_PULL + 16, dealt * WIND_MENDS);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, foot, Vec3.ZERO, (float) WIND_PULL, 0, 192);
			SpellFx.send(level, SpellFx.Kind.LEAVES, foot.add(0, WIND_HIGH * 0.5, 0), Vec3.ZERO, 14, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, foot.add(0, WIND_HIGH * 0.5, 0), Vec3.ZERO, 12, CREATION_LIGHT, 192);
			sound(foot, SoundEvents.PLAYER_ATTACK_SWEEP, 5.0f, 0.4f);
			sound(foot, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.8f);
		}

		@Override
		public void end() {
			cyclone.dissolve();
		}
	}

	/**
	 * First Seed of Creation: the seed of creation comes down out of the light where its caster looks, and the dome of life grows over it, great
	 * vines arching up out of the land to meet over it, {@link #CREATION_RADIUS} round (SpellConstruct CREATION_SEED, its own GeckoLib model;
	 * {@link #CREATION_GROWS}); the land within flourishes. For {@link #CREATION_TICKS} it drinks: every {@link #CREATION_EVERY} ticks every foe
	 * within is drawn on for {@link #CREATION_DRAIN} and slowed, held within its vines, and {@link #CREATION_MENDS} of all it draws shared out
	 * mending its caster and their own within. Then the tree of creation bursts up out of the seed through the dome: {@link #CREATION_BLOOM} to
	 * every foe within, thrown up, the same share of it mending its own.
	 */
	public static final class Creation extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct seed;
		/** Its roots, from the seed to every foe it drinks from (Rooting). */
		private final Rooting rooting;

		Creation(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, CREATION_GROWS + CREATION_TICKS + CREATION_BLOOMS + CREATION_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.seed = SpellConstruct.create(level, SpellConstruct.Kind.CREATION_SEED, ground.add(0, 0.1, 0), caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			this.rooting = new Rooting(caster, level, ground, 2.6f);
			sound(ground, SoundEvents.BEACON_ACTIVATE, 4.0f, 0.5f);
			sound(ground, SoundEvents.ROOTED_DIRT_PLACE, 4.0f, 0.4f);
		}

		private List<LivingEntity> within() {
			return foesWithin(caster, level, ground, CREATION_RADIUS, 10, CREATION_RADIUS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < CREATION_GROWS) return true;
			long drinking = age - CREATION_GROWS;
			if (drinking == 0) Devastation.flourish(caster, level, ground, 0, CREATION_RADIUS, 0.35);
			if (drinking < CREATION_TICKS) {
				hold();
				if (drinking % CREATION_EVERY == 0) drink();
				if (drinking % 4 == 0) {
					double a = level.random.nextDouble() * Math.PI * 2, r = CREATION_RADIUS * level.random.nextDouble();
					SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, ground.add(Math.cos(a) * r, 1 + level.random.nextDouble() * 6, Math.sin(a) * r), new Vec3(0, 3, 0), 2, 0, 192);
				}
			}
			if (drinking == CREATION_TICKS) {
				seed.act("bloom");
				sound(ground, SoundEvents.BEACON_POWER_SELECT, 5.0f, 0.5f);
			}
			if (drinking == CREATION_TICKS + 6) bloom();
			return true;
		}

		/** Its foes near its edge from within turned back in: none leaves the dome while it drinks. */
		private void hold() {
			for (LivingEntity e : foesWithin(caster, level, ground, CREATION_RADIUS + 1, 10, CREATION_RADIUS)) {
				double dx = e.getX() - ground.x, dz = e.getZ() - ground.z, d = Math.max(1.0e-3, Math.hypot(dx, dz));
				if (d < CREATION_RADIUS - 3) continue;
				Vec3 v = e.getDeltaMovement();
				if ((v.x * -dx + v.z * -dz) / d < 0.2) HeavenSpells.move(e, new Vec3(-dx / d * 0.5, v.y, -dz / d * 0.5));
			}
		}

		/** One draught: every foe within drawn on and slowed, a share of it mending its caster and their own within. */
		private void drink() {
			float drunk = 0;
			Vec3 heart = ground.add(0, HeavenSpells.span(Realm.TRUE_IMMORTAL, 3), 0);
			List<LivingEntity> drinking = within();
			rooting.hold(drinking, (int) (end - level.getGameTime()));
			for (LivingEntity e : drinking) {
				drunk += HeavenSpells.strikeAgain(caster, e, CREATION_DRAIN, spell);
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
				SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, HeavenSpells.middle(e), heart.subtract(HeavenSpells.middle(e)), 2, 0);
			}
			shareMending(caster, level, ground, CREATION_RADIUS, drunk * CREATION_MENDS);
			sound(ground, SoundEvents.AZALEA_LEAVES_HIT, 4.0f, 0.4f);
		}

		/** The tree of creation bursts up: every foe within struck and thrown up, a share of it mending its own, the land flourishing. */
		private void bloom() {
			rooting.release();
			float dealt = 0;
			for (LivingEntity e : within()) {
				e.invulnerableTime = 0;
				dealt += HeavenSpells.strike(caster, e, CREATION_BLOOM, spell);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, new Vec3(0, 1.4, 0));
				HeavenSpells.safeFall(e, 100);
			}
			shareMending(caster, level, ground, CREATION_RADIUS, dealt * CREATION_MENDS);
			Devastation.flourish(caster, level, ground, 0, CREATION_RADIUS, 0.6);
			Vec3 crown = ground.add(0, HeavenSpells.span(Realm.TRUE_IMMORTAL, 16), 0);
			SpellFx.send(level, SpellFx.Kind.FLASH, crown, Vec3.ZERO, 18, CREATION_LIGHT, 256);
			SpellFx.send(level, SpellFx.Kind.LEAVES, crown, Vec3.ZERO, 16, 0, 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) CREATION_RADIUS, 0, 256);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.6f);
			sound(ground, SoundEvents.AMETHYST_BLOCK_CHIME, 5.0f, 0.5f);
		}

		@Override
		public void end() {
			rooting.release();
			seed.dissolve();
		}
	}

	static void tick(MinecraftServer server) {}

	static void forget(UUID id) {}

	static void clear() {}

	private WoodSpells() {}
}
