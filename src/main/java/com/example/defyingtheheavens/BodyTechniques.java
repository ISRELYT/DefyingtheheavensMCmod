package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Body Path's techniques (phase 2, 2026-11-01; the user's "Go" to six of them, a pool of their own and their unlocks by realm; and to five
 * for the realms above, the pool growing with the body): the flesh's own arts, wielded from the ability wheel as the realm's abilities are,
 * each awakened as the body reaches its realm on the Body Path ({@link #awakensAt}), the living's alone (the dead have no flesh), a mortal's
 * too (Ability#isUnlocked). They spend <b>vigor</b>, the body's own pool ({@link BodyState#vigor}, {@link #maxVigor}: {@link #VIGOR} at Copper
 * Skin and {@link #VIGOR_PER_REALM} more for each realm past it; no qi), which eating fills ({@link #ate}: {@link #FOOD_SHARE} of the pool a
 * food point) and rest refills ({@link #resting}: {@link #REST_SHARE} of it a second, {@link #MEDITATING} times as fast in meditation), and
 * each leaves one the hungrier ({@link #HUNGER}). Every one grows with the body's realm past the one it awakens at ({@link #realmsPast}):
 * <ul>
 *   <li><b>Iron Skin</b> (Copper Skin): for {@link #IRON_SKIN_TICKS}, blows harm one {@link #ironSkinCut} less and nothing knocks one back.</li>
 *   <li><b>Mountain Stomp</b> (Iron Flesh): on the ground, every foe within {@link #stompRadius} struck for {@link #STOMP_SHARE} of one's blow,
 *   flung back and slowed.</li>
 *   <li><b>Ground-Splitting Leap</b> (Steel Sinew): a great leap where one looks, no fall's harm till one lands, and a shockwave where one does
 *   ({@link #landingRadius}, {@link #LANDING_SHARE} of one's blow).</li>
 *   <li><b>Thousand-Pound Fist</b> (Jade Bone): the next blow of the hand within {@link #FIST_TICKS} lands {@link #fistFactor} times as hard and
 *   hurls its foe away.</li>
 *   <li><b>Blood Frenzy</b> (Silver Marrow): a standing switch, {@link #FRENZY_PER_SECOND} vigor a second (none refilled meanwhile): blows of the
 *   hand and the body's techniques {@link #frenzyHarm} harder, and one {@link #FRENZY_SPEED} swifter.</li>
 *   <li><b>Golden Body</b> (Golden Blood): for {@link #goldenTicks}, every blow turned aside, unharmed and unmoved; not the heavens' own strikes
 *   (HeavenSpells#incoming lets a tribulation's through before any ward).</li>
 *   <li><b>Diamond Will</b> (Diamond Heart): for {@link #diamondTicks}, every harmful effect cleared and none taking hold ({@link #allowEffect}),
 *   nothing knocking one back, no one's Realm Suppress pressing one ({@link #unshaken}).</li>
 *   <li><b>Comet Charge</b> (the Star-Forged Body): on the ground, hurled some {@link #cometBlocks} along it where one looks, every foe one passes
 *   through struck for {@link #COMET_SHARE} of one's blow and thrown aside, no fall's harm till it is done.</li>
 *   <li><b>Rebounding Bones</b> (the Heaven-Bone Body): for {@link #reboundTicks}, {@link #reboundShare} of every blow one takes struck back on its
 *   dealer within {@link #REBOUND_REACH} (one still takes it).</li>
 *   <li><b>Flesh Reborn</b> (the Undying Body): {@link #rebornNow} of one's health back at once, fire and every harmful effect gone, and
 *   {@link #REBORN_OVER} more over {@link #REBORN_TICKS}.</li>
 *   <li><b>Heaven-Shattering Fist</b> (the Indestructible Body): every foe within {@link #SHATTER_REACH} in a cone before one struck for
 *   {@link #SHATTER_SHARE} of one's blow and hurled away.</li>
 *   <li>Up the ladder (the user's "Go, all three parts", 2026-11-01): <b>Earthshaker</b> (the Earth-Bearing Body), {@link #EARTHSHAKER_RINGS}
 *   rings of heaving earth out to {@link #earthshakerReach}, each foe in them struck and tossed up; <b>Sky-Pillar Strike</b> (the Sky-Pillar
 *   Body), thrown up high and slammed down where one looks (within {@link #PILLAR_REACH}), the land struck round where one lands; <b>Imperishable
 *   Gold</b> (the Imperishable Gold Body), for {@link #imperishableTicks} every blow and harmful effect turned aside, then all it turned aside
 *   burst out on those near; <b>Primordial Roar</b> (the Primordial Saint Body), every foe within {@link #roarReach} stripped of its wards and
 *   stunned; <b>Myriad Fists</b> (the Chaos-Tempered Body), ten blows in two seconds before one, the last hurling; <b>Dharma Body of Heaven and
 *   Earth</b> (the Body of Heaven and Earth), one's own body as tall as heaven and earth behind one, its fist coming down where one's lands, its
 *   foot where one stomps. Players' alone: no body cultivator of the world's climbs past the Indestructible Body (BodyCultivators#LAST_RANK).</li>
 * </ul>
 * The acts show "on" on the wheel while they last (the skin, the fist primed, the gold, the leap or the charge under way, the will, the bones,
 * the flesh growing back), and each has a cooldown that outlasts a logout. Blows here are harm something deals ({@link #isBlow}): not a fall,
 * fire, hunger or drowning.
 * <p>
 * Body cultivators wield them too (CultivatorNpc#isBodyCultivator, the user's "Go, with body sects", 2026-11-01: {@link #npcAct}, chosen in a
 * fight by BodyCultivators#TechniqueGoal): the same arts of their realm, at the realm they fight at, on cooldowns of their own and with no
 * vigor to spend; their Blood Frenzy burns a while ({@link #NPC_FRENZY_TICKS}) rather than standing as a switch. Every hold below is kept by
 * whoever's it is, a player's or a body cultivator's ({@link #user}).
 */
public final class BodyTechniques {
	/** The vigor pool at Copper Skin, and how much more it holds for each body realm past it (200 at the Indestructible Body). */
	public static final double VIGOR = 100, VIGOR_PER_REALM = 10;
	/** Rest refills this share of the pool a second ({@link #MEDITATING} times as fast meditating); eating, this share a food point. */
	public static final double REST_SHARE = 0.01, MEDITATING = 3, FOOD_SHARE = 0.03;
	/** One rests after this long (ticks) with no blow given or taken, not sprinting. */
	static final int REST_AFTER = 100;
	/** Each technique leaves one this much of vanilla's exhaustion (4 is a food point) for every vigor it spends: the body burns its food. */
	static final double HUNGER = 0.1;

	/** What each act spends, and how long till it may be used again (ticks). */
	public static final double IRON_SKIN_COST = 25, STOMP_COST = 30, LEAP_COST = 15, FIST_COST = 20, GOLDEN_COST = 40,
			DIAMOND_COST = 35, COMET_COST = 30, REBOUND_COST = 35, REBORN_COST = 50, SHATTER_COST = 60;
	public static final int IRON_SKIN_COOLDOWN = 400, STOMP_COOLDOWN = 160, LEAP_COOLDOWN = 60, FIST_COOLDOWN = 120, GOLDEN_COOLDOWN = 900,
			DIAMOND_COOLDOWN = 800, COMET_COOLDOWN = 200, REBOUND_COOLDOWN = 600, REBORN_COOLDOWN = 1200, SHATTER_COOLDOWN = 600;

	/** Iron Skin: how long it holds (ticks), the share of a blow it takes away at Copper Skin and more for each realm past it, at most. */
	public static final int IRON_SKIN_TICKS = 160;
	public static final double IRON_SKIN_CUT = 0.40, IRON_SKIN_CUT_PER_REALM = 0.03, IRON_SKIN_CUT_MOST = 0.70;
	/** Mountain Stomp: its reach at Iron Flesh and further for each realm past it, the share of one's blow it strikes with, its slowness (ticks). */
	public static final double STOMP_RADIUS = 5, STOMP_RADIUS_PER_REALM = 0.5, STOMP_SHARE = 1.0;
	public static final int STOMP_SLOW_TICKS = 60;
	/** Ground-Splitting Leap: how hard it throws one forward and up at Steel Sinew and harder for each realm past it; its landing's reach and share. */
	static final double LEAP_FORWARD = 1.4, LEAP_FORWARD_PER_REALM = 0.12, LEAP_UP = 0.8, LEAP_UP_PER_REALM = 0.04;
	public static final double LANDING_RADIUS = 3, LANDING_RADIUS_PER_REALM = 0.3, LANDING_SHARE = 0.6;
	/** A leap not yet off the ground this long (ticks), or in the air this long, is let go (blocked by a roof; flown, swum, fallen far). */
	static final int LEAP_TAKEOFF = 40, LEAP_MOST = 200;
	/**
	 * About how many blocks a body cultivator's leap carries it for each unit it is thrown forward, so it leaps no further than its foe
	 * ({@link #npcAct}): measured in game, a throw of 1.6 carried one 8.9 blocks (its first tick still on the ground, near half its speed lost
	 * to the ground's friction, then some twenty-five ticks aloft losing a tenth a tick).
	 */
	static final double LEAP_BLOCKS_PER_FORWARD = 5.5;
	/** Thousand-Pound Fist: how long it waits for its blow (ticks); how many times as hard it lands at Jade Bone and more for each realm past; its hurl. */
	public static final int FIST_TICKS = 100;
	public static final double FIST_FACTOR = 3, FIST_FACTOR_PER_REALM = 0.25, FIST_HURL = 2.0, FIST_HURL_PER_REALM = 0.2;
	/** Blood Frenzy: its vigor a second, the harm it adds at Silver Marrow and more for each realm past it, its swiftness. */
	public static final double FRENZY_PER_SECOND = 6, FRENZY_HARM = 0.30, FRENZY_HARM_PER_REALM = 0.05, FRENZY_SPEED = 0.30;
	/** A body cultivator's Blood Frenzy burns this long (ticks): it has no vigor to keep it lit. */
	public static final int NPC_FRENZY_TICKS = 300;
	/** Golden Body: how long it holds at Golden Blood (ticks), and how much longer for each realm past it. */
	public static final int GOLDEN_TICKS = 60, GOLDEN_TICKS_PER_REALM = 10;
	/** Diamond Will: how long it holds at Diamond Heart (ticks), and how much longer for each realm past it. */
	public static final int DIAMOND_TICKS = 200, DIAMOND_TICKS_PER_REALM = 20;
	/**
	 * Comet Charge: about how far it carries one at the Star-Forged Body (a block more for each realm past it), how hard it throws one to go so
	 * far, and its lift off the ground; the share of one's blow it strikes each foe it passes with, how near they must come.
	 */
	public static final int COMET_BLOCKS = 12;
	static final double COMET_SPEED = 2.9, COMET_LIFT = 0.25; // measured in game: 2.93 a tick carried one 12.4 blocks to its landing
	public static final double COMET_SHARE = 1.5, COMET_REACH = 1.5;
	/** A charge still under way this long (ticks) is over. */
	static final int COMET_MOST = 30;
	/** Rebounding Bones: how long it holds at the Heaven-Bone Body (ticks) and longer for each realm past; the share struck back, more each realm, at most. */
	public static final int REBOUND_TICKS = 120, REBOUND_TICKS_PER_REALM = 10;
	public static final double REBOUND_SHARE = 0.5, REBOUND_SHARE_PER_REALM = 0.05, REBOUND_SHARE_MOST = 0.8;
	/** How far off a blow's dealer may stand to have it struck back on them. */
	public static final int REBOUND_REACH = 32;
	/** Flesh Reborn: the share of one's health back at once at the Undying Body and more for each realm past it; the share more over its regrowth (ticks). */
	public static final double REBORN_NOW = 0.30, REBORN_NOW_PER_REALM = 0.05, REBORN_OVER = 0.50;
	public static final int REBORN_TICKS = 200;
	/** Heaven-Shattering Fist: its reach, the half-width of its cone (degrees), the share of one's blow it strikes with, its hurl. */
	public static final double SHATTER_REACH = 16, SHATTER_HALF_ANGLE = 45, SHATTER_SHARE = 3.0, SHATTER_HURL = 2.5;
	/**
	 * Cloud-Treading Steps (Silver Marrow, the body's way up through the worlds: the user's "Go, all three parts", 2026-11-01): its vigor a
	 * second in the air, and its pace, two thirds of Qi Flight's (QiFlight flies it where no qi or sword carries one). At that pace one climbs
	 * 5 blocks a second, so the Spatial Gap's 232 (the rift's way in to its crossing) take some 46 s: Silver Marrow's 140 vigor carries one
	 * 70 s aloft.
	 */
	public static final double TREAD_PER_SECOND = 2;
	public static final float TREAD_SPEED = SwordFlight.QI_FLIGHT_SPEED * 2 / 3f;

	// --- The ladder's techniques (the user's "Go, all three parts", 2026-11-01, part three): what each spends, how long till it may be used again ---
	public static final double EARTHSHAKER_COST = 70, PILLAR_COST = 60, GOLD_COST = 90, ROAR_COST = 70, MYRIAD_COST = 80, DHARMA_COST = 150;
	public static final int EARTHSHAKER_COOLDOWN = 400, PILLAR_COOLDOWN = 240, GOLD_COOLDOWN = 1200, ROAR_COOLDOWN = 800, MYRIAD_COOLDOWN = 500,
			DHARMA_COOLDOWN = 2400;
	/**
	 * Earthshaker (the Earth-Bearing Body): its rings of heaving earth, the ticks between one and the next, how far the last reaches at the
	 * Earth-Bearing Body and further for each realm past it, the share of one's blow each strikes with, how high it tosses what it strikes.
	 */
	public static final int EARTHSHAKER_RINGS = 3, EARTHSHAKER_EVERY = 8;
	public static final double EARTHSHAKER_REACH = 15, EARTHSHAKER_REACH_PER_REALM = 1, EARTHSHAKER_SHARE = 1.5, EARTHSHAKER_TOSS = 1.1;
	/** How far above or below its centre a ring still strikes what it passes under. */
	static final double EARTHSHAKER_HEIGHT = 3;
	/**
	 * Sky-Pillar Strike (the Sky-Pillar Body): how far off where one looks it may land, how hard it throws one up, the share of the way there
	 * the throw covers a tick, its slam's speed down onto it; the slam's reach at the Sky-Pillar Body and further each realm past, its share of
	 * one's blow and its toss. A strike still under way this long (ticks) is let go.
	 */
	public static final double PILLAR_REACH = 24, PILLAR_UP = 1.7, PILLAR_CARRY = 0.055, PILLAR_SLAM = 3.2;
	public static final double PILLAR_RADIUS = 6, PILLAR_RADIUS_PER_REALM = 0.5, PILLAR_SHARE = 2.5, PILLAR_TOSS = 0.9;
	static final int PILLAR_MOST = 100;
	/**
	 * Imperishable Gold (the Imperishable Gold Body): how long it holds (ticks) and longer each realm past; then all the harm it turned aside
	 * bursts out on every foe within its reach, at most so many times one's blow.
	 */
	public static final int GOLD_TICKS = 160, GOLD_TICKS_PER_REALM = 10;
	public static final double GOLD_BURST_RADIUS = 8, GOLD_BURST_MOST = 10;
	/** Primordial Roar (the Primordial Saint Body): its reach and further each realm past; how long those it reaches stand stunned (ticks). */
	public static final double ROAR_REACH = 16, ROAR_REACH_PER_REALM = 1;
	public static final int ROAR_STUN_TICKS = 60;
	/**
	 * Myriad Fists (the Chaos-Tempered Body): its blows, the ticks between them, their reach and the half-width of their cone (degrees), each's
	 * share of one's blow and more each realm past, and how far the last hurls.
	 */
	public static final int MYRIAD_BLOWS = 10, MYRIAD_EVERY = 4;
	public static final double MYRIAD_REACH = 5, MYRIAD_HALF_ANGLE = 50, MYRIAD_SHARE = 0.6, MYRIAD_SHARE_PER_REALM = 0.05, MYRIAD_HURL = 4;
	/**
	 * Dharma Body of Heaven and Earth (the Body of Heaven and Earth): how long it towers (ticks), its height and how far behind one it stands
	 * (blocks); its fist, coming down out of the sky where one's own blow lands (HeavenDemonSpells#AVATAR_FALL ticks after, at most one every
	 * {@link #DHARMA_FIST_EVERY}), its reach round there and its share of one's blow; its stomp, along with one's own (Mountain Stomp,
	 * Earthshaker), its reach round its feet, its share and the ticks its foot takes to come down.
	 */
	public static final int DHARMA_TICKS = 400, DHARMA_FIST_EVERY = 14, DHARMA_STOMP_DELAY = 12;
	public static final double DHARMA_HEIGHT = 30, DHARMA_BEHIND = 6, DHARMA_FIST_RADIUS = 4.5, DHARMA_FIST_SHARE = 2.0, DHARMA_STOMP_RADIUS = 14,
			DHARMA_STOMP_SHARE = 1.5;
	/** How fast it strides after one (blocks a tick), and turns to where one looks (degrees a tick); further off than this, it is there at once. */
	static final double DHARMA_STRIDE = 1.2, DHARMA_TURN = 6, DHARMA_SNAP = 40;

	/** The techniques, in the order they awaken. */
	public static final Ability[] TECHNIQUES = {Ability.IRON_SKIN, Ability.MOUNTAIN_STOMP, Ability.GROUND_SPLITTING_LEAP, Ability.THOUSAND_POUND_FIST,
			Ability.BLOOD_FRENZY, Ability.CLOUD_TREADING, Ability.GOLDEN_BODY, Ability.DIAMOND_WILL, Ability.COMET_CHARGE, Ability.REBOUNDING_BONES,
			Ability.FLESH_REBORN, Ability.HEAVEN_SHATTERING_FIST, Ability.EARTHSHAKER, Ability.SKY_PILLAR_STRIKE, Ability.IMPERISHABLE_GOLD,
			Ability.PRIMORDIAL_ROAR, Ability.MYRIAD_FISTS, Ability.DHARMA_BODY};

	private static final UUID IRON_SKIN_ID = UUID.fromString("6b1f3c2e-9a4d-4e57-b3c1-2f8d7a0e5b91");
	private static final UUID DIAMOND_ID = UUID.fromString("2e8c5a71-4f3b-4d9a-8c16-7b0e9d3f2a58");
	private static final UUID IMPERISHABLE_ID = UUID.fromString("8d3f6b20-1c7e-4a95-b4d2-5e9a0c7f3b16");
	private static final UUID FRENZY_ID = UUID.fromString("c47e2a19-5d3b-4f86-a0e2-8b9c1d6f3e74");
	private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.72f, 0.04f, 0.04f), 1.5f);
	private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.25f), 1.1f);
	private static final DustParticleOptions DIAMOND = new DustParticleOptions(new Vector3f(0.70f, 0.95f, 1.0f), 1.0f);
	private static final DustParticleOptions FLESH = new DustParticleOptions(new Vector3f(0.95f, 0.35f, 0.40f), 1.0f);

	/** When each of a player's acts may be used again (game time): outlasting a logout, gone with the server. */
	private static final Map<UUID, Map<Ability, Long>> READY = new HashMap<>();
	/** Until when Iron Skin holds, a fist waits primed, Golden Body, Diamond Will and Rebounding Bones hold, the flesh grows back (game time). */
	private static final Map<UUID, Long> IRON_SKIN = new HashMap<>(), FIST = new HashMap<>(), GOLDEN = new HashMap<>(), DIAMOND_WILL = new HashMap<>(),
			REBOUND = new HashMap<>(), REBORN = new HashMap<>();
	/** Until when a body cultivator's Blood Frenzy burns (game time; a player's is a switch, PlayerCultivation#isAbilityEnabled). */
	private static final Map<UUID, Long> FRENZY = new HashMap<>();
	/** Those on a leap: when they leapt, and whether they have left the ground yet (a landing counts only after). */
	private static final Map<UUID, Leap> LEAPS = new HashMap<>();
	/** Those on a Comet Charge: when, which way, whom it has struck, whether they have left the ground yet. */
	private static final Map<UUID, Comet> COMETS = new HashMap<>();
	/** Foes a fist has struck, hurled at the tick's end (after the blow's own knockback, which would halve the hurl). */
	private static final List<Hurl> HURLS = new ArrayList<>();
	/** The whole vigor each client was last told of: a sync only when it changes. */
	private static final Map<UUID, Integer> TOLD = new HashMap<>();
	/** Body cultivators who have used a technique, by their UUID: their holds are found through it, as players' are through the player list. */
	private static final Map<UUID, LivingEntity> NPCS = new HashMap<>();
	/** While a blow is being struck back (Rebounding Bones): it isn't struck back again by another's bones. */
	private static boolean rebounding;

	private static final class Leap {
		final long at;
		boolean aloft;

		Leap(long at) {
			this.at = at;
		}
	}

	private static final class Comet {
		final long at;
		final double x, z;
		final Set<UUID> struck = new HashSet<>();
		boolean aloft;

		Comet(long at, double x, double z) {
			this.at = at;
			this.x = x;
			this.z = z;
		}
	}

	private record Hurl(LivingEntity target, double x, double z, double strength) {}

	/** Earthshakers under way: whose, where and when the ground was struck, how far they reach, how many rings have run out, how many they struck. */
	private static final Map<UUID, Shake> SHAKES = new HashMap<>();
	/** Those up on a Sky-Pillar Strike: when, where they come down, whether they have left the ground and turned down to it. */
	private static final Map<UUID, Pillar> PILLARS = new HashMap<>();
	/** Until when Imperishable Gold holds (game time), and the harm it has turned aside meanwhile (its burst's). */
	private static final Map<UUID, Long> IMPERISHABLE = new HashMap<>();
	private static final Map<UUID, Float> TURNED = new HashMap<>();
	/** Myriad Fists under way: when the flurry began, how many blows have fallen. */
	private static final Map<UUID, Myriad> MYRIADS = new HashMap<>();
	/** Dharma Bodies towering: the body, till when, its next fist's time, which arm, its fists falling and its stomps coming down. */
	private static final Map<UUID, Dharma> DHARMAS = new HashMap<>();

	private static final class Shake {
		final long at;
		final Vec3 centre;
		final double reach;
		int rings, struck;

		Shake(long at, Vec3 centre, double reach) {
			this.at = at;
			this.centre = centre;
			this.reach = reach;
		}
	}

	private static final class Pillar {
		final long at;
		final Vec3 target;
		boolean aloft, slamming;

		Pillar(long at, Vec3 target) {
			this.at = at;
			this.target = target;
		}
	}

	private static final class Myriad {
		final long at;
		int blows;

		Myriad(long at) {
			this.at = at;
		}
	}

	private static final class Dharma {
		final SpellConstruct body;
		final long until;
		long nextFist, holdYawUntil;
		boolean right = true;
		final List<Fist> fists = new ArrayList<>();
		final List<Long> stomps = new ArrayList<>();
		/** Fists landed, pressing into what they struck till their life is out (then dissolved). */
		final List<SpellConstruct> sent = new ArrayList<>();

		Dharma(SpellConstruct body, long until) {
			this.body = body;
			this.until = until;
		}
	}

	/** A Dharma Body's fist falling out of the sky (an AVATAR_HAND of gold): where it lands, when, with how much, and its hand. */
	private record Fist(Vec3 ground, long lands, float harm, SpellConstruct hand) {}

	// --- Where they stand ---

	/** The body realm {@code technique} awakens at on the Body Path, or null for an ability that isn't one of these. */
	public static Realm awakensAt(Ability technique) {
		return switch (technique) {
			case IRON_SKIN -> Realm.QI_REFINING;
			case MOUNTAIN_STOMP -> Realm.FOUNDATION_BUILDING;
			case GROUND_SPLITTING_LEAP -> Realm.CORE_FORMATION;
			case THOUSAND_POUND_FIST -> Realm.NASCENT_SOUL;
			case BLOOD_FRENZY, CLOUD_TREADING -> Realm.HEAVENLY_BEING;
			case GOLDEN_BODY -> Realm.FOUR_AXIS;
			case DIAMOND_WILL -> Realm.DAO_INTEGRATION;
			case COMET_CHARGE -> Realm.VOID_SHATTERING;
			case REBOUNDING_BONES -> Realm.SACRED_VESSEL;
			case FLESH_REBORN -> Realm.ENTERING_NIRVANA;
			case HEAVEN_SHATTERING_FIST -> Realm.TRUE_IMMORTAL;
			case EARTHSHAKER -> Realm.EARTH_IMMORTAL;
			case SKY_PILLAR_STRIKE -> Realm.HEAVEN_IMMORTAL;
			case IMPERISHABLE_GOLD -> Realm.GOLDEN_IMMORTAL;
			case PRIMORDIAL_ROAR -> Realm.TAIYI_GOLDEN_IMMORTAL;
			case MYRIAD_FISTS -> Realm.PROFOUND_SOVEREIGN;
			case DHARMA_BODY -> Realm.HEAVENLY_LORD;
			default -> null;
		};
	}

	public static boolean isTechnique(Ability ability) {
		return awakensAt(ability) != null;
	}

	/** Whether {@code c}'s body has reached {@code realm} on the Body Path, as one of the living. */
	static boolean has(PlayerCultivation c, Realm realm) {
		int rank = BodyPath.rank(c);
		return rank >= 0 && !c.netherworld().ghost() && BodyPath.realm(rank).ordinal() >= realm.ordinal();
	}

	/** Whether a body at {@code rank} on the Body Path has awakened {@code technique}. */
	public static boolean knows(int rank, Ability technique) {
		Realm at = awakensAt(technique);
		return at != null && rank >= 0 && BodyPath.realm(rank).ordinal() >= at.ordinal();
	}

	/**
	 * The rank {@code user}'s body stands at on the Body Path, which its techniques grow by: a player's own; a body cultivator's as it fights
	 * (CultivatorNpc#fightingRank: in a spar, the realm it holds back to). -1 for anyone else.
	 */
	public static int rankOf(LivingEntity user) {
		if (user instanceof ServerPlayer p) return BodyPath.rank(CultivationManager.get(p));
		if (user instanceof CultivatorNpc npc && npc.isBodyCultivator() && !npc.isMortal()) return Math.min(BodyCultivators.LAST_RANK, npc.fightingRank());
		return -1;
	}

	/** How many realms a body at {@code rank} stands past {@code realm} (0 at it or short of it). */
	static int realmsPast(int rank, Realm realm) {
		return rank < 0 ? 0 : Math.max(0, BodyPath.realm(rank).ordinal() - realm.ordinal());
	}

	/** How many realms {@code c}'s body stands past {@code realm} (0 at it or short of it). */
	static int realmsPast(PlayerCultivation c, Realm realm) {
		return realmsPast(BodyPath.rank(c), realm);
	}

	/** The vigor pool of a body at {@code rank} on the Body Path: {@link #VIGOR} at Copper Skin (and short of the path), more for each realm past. */
	public static double maxVigor(int rank) {
		return rank < 0 ? VIGOR : VIGOR + VIGOR_PER_REALM * BodyPath.realm(rank).ordinal();
	}

	public static double maxVigor(PlayerCultivation c) {
		return maxVigor(BodyPath.rank(c));
	}

	public static double ironSkinCut(int rank) {
		return Math.min(IRON_SKIN_CUT_MOST, IRON_SKIN_CUT + IRON_SKIN_CUT_PER_REALM * realmsPast(rank, Realm.QI_REFINING));
	}

	public static double ironSkinCut(PlayerCultivation c) {
		return ironSkinCut(BodyPath.rank(c));
	}

	public static double stompRadius(int rank) {
		return STOMP_RADIUS + STOMP_RADIUS_PER_REALM * realmsPast(rank, Realm.FOUNDATION_BUILDING);
	}

	public static double stompRadius(PlayerCultivation c) {
		return stompRadius(BodyPath.rank(c));
	}

	public static double landingRadius(int rank) {
		return LANDING_RADIUS + LANDING_RADIUS_PER_REALM * realmsPast(rank, Realm.CORE_FORMATION);
	}

	public static double landingRadius(PlayerCultivation c) {
		return landingRadius(BodyPath.rank(c));
	}

	/** How hard a leap throws a body at {@code rank} forward. */
	static double leapForward(int rank) {
		return LEAP_FORWARD + LEAP_FORWARD_PER_REALM * realmsPast(rank, Realm.CORE_FORMATION);
	}

	public static double fistFactor(int rank) {
		return FIST_FACTOR + FIST_FACTOR_PER_REALM * realmsPast(rank, Realm.NASCENT_SOUL);
	}

	public static double fistFactor(PlayerCultivation c) {
		return fistFactor(BodyPath.rank(c));
	}

	public static double frenzyHarm(int rank) {
		return FRENZY_HARM + FRENZY_HARM_PER_REALM * realmsPast(rank, Realm.HEAVENLY_BEING);
	}

	public static double frenzyHarm(PlayerCultivation c) {
		return frenzyHarm(BodyPath.rank(c));
	}

	public static int goldenTicks(int rank) {
		return GOLDEN_TICKS + GOLDEN_TICKS_PER_REALM * realmsPast(rank, Realm.FOUR_AXIS);
	}

	public static int goldenTicks(PlayerCultivation c) {
		return goldenTicks(BodyPath.rank(c));
	}

	public static int diamondTicks(int rank) {
		return DIAMOND_TICKS + DIAMOND_TICKS_PER_REALM * realmsPast(rank, Realm.DAO_INTEGRATION);
	}

	public static int diamondTicks(PlayerCultivation c) {
		return diamondTicks(BodyPath.rank(c));
	}

	public static int cometBlocks(int rank) {
		return COMET_BLOCKS + realmsPast(rank, Realm.VOID_SHATTERING);
	}

	public static int cometBlocks(PlayerCultivation c) {
		return cometBlocks(BodyPath.rank(c));
	}

	public static int reboundTicks(int rank) {
		return REBOUND_TICKS + REBOUND_TICKS_PER_REALM * realmsPast(rank, Realm.SACRED_VESSEL);
	}

	public static int reboundTicks(PlayerCultivation c) {
		return reboundTicks(BodyPath.rank(c));
	}

	public static double reboundShare(int rank) {
		return Math.min(REBOUND_SHARE_MOST, REBOUND_SHARE + REBOUND_SHARE_PER_REALM * realmsPast(rank, Realm.SACRED_VESSEL));
	}

	public static double reboundShare(PlayerCultivation c) {
		return reboundShare(BodyPath.rank(c));
	}

	public static double rebornNow(int rank) {
		return REBORN_NOW + REBORN_NOW_PER_REALM * realmsPast(rank, Realm.ENTERING_NIRVANA);
	}

	public static double rebornNow(PlayerCultivation c) {
		return rebornNow(BodyPath.rank(c));
	}

	public static double earthshakerReach(int rank) {
		return EARTHSHAKER_REACH + EARTHSHAKER_REACH_PER_REALM * realmsPast(rank, Realm.EARTH_IMMORTAL);
	}

	public static double earthshakerReach(PlayerCultivation c) {
		return earthshakerReach(BodyPath.rank(c));
	}

	public static double pillarRadius(int rank) {
		return PILLAR_RADIUS + PILLAR_RADIUS_PER_REALM * realmsPast(rank, Realm.HEAVEN_IMMORTAL);
	}

	public static double pillarRadius(PlayerCultivation c) {
		return pillarRadius(BodyPath.rank(c));
	}

	public static int imperishableTicks(int rank) {
		return GOLD_TICKS + GOLD_TICKS_PER_REALM * realmsPast(rank, Realm.GOLDEN_IMMORTAL);
	}

	public static int imperishableTicks(PlayerCultivation c) {
		return imperishableTicks(BodyPath.rank(c));
	}

	public static double roarReach(int rank) {
		return ROAR_REACH + ROAR_REACH_PER_REALM * realmsPast(rank, Realm.TAIYI_GOLDEN_IMMORTAL);
	}

	public static double roarReach(PlayerCultivation c) {
		return roarReach(BodyPath.rank(c));
	}

	public static double myriadShare(int rank) {
		return MYRIAD_SHARE + MYRIAD_SHARE_PER_REALM * realmsPast(rank, Realm.PROFOUND_SOVEREIGN);
	}

	public static double myriadShare(PlayerCultivation c) {
		return myriadShare(BodyPath.rank(c));
	}

	/** What {@code technique} spends: an act's vigor, Blood Frenzy's a second. */
	public static double cost(Ability technique) {
		return switch (technique) {
			case IRON_SKIN -> IRON_SKIN_COST;
			case MOUNTAIN_STOMP -> STOMP_COST;
			case GROUND_SPLITTING_LEAP -> LEAP_COST;
			case THOUSAND_POUND_FIST -> FIST_COST;
			case BLOOD_FRENZY -> FRENZY_PER_SECOND;
			case CLOUD_TREADING -> TREAD_PER_SECOND;
			case GOLDEN_BODY -> GOLDEN_COST;
			case DIAMOND_WILL -> DIAMOND_COST;
			case COMET_CHARGE -> COMET_COST;
			case REBOUNDING_BONES -> REBOUND_COST;
			case FLESH_REBORN -> REBORN_COST;
			case HEAVEN_SHATTERING_FIST -> SHATTER_COST;
			case EARTHSHAKER -> EARTHSHAKER_COST;
			case SKY_PILLAR_STRIKE -> PILLAR_COST;
			case IMPERISHABLE_GOLD -> GOLD_COST;
			case PRIMORDIAL_ROAR -> ROAR_COST;
			case MYRIAD_FISTS -> MYRIAD_COST;
			case DHARMA_BODY -> DHARMA_COST;
			default -> 0;
		};
	}

	/** How long after {@code technique} it may be used again (ticks): Blood Frenzy, a switch, has none. */
	public static int cooldown(Ability technique) {
		return switch (technique) {
			case IRON_SKIN -> IRON_SKIN_COOLDOWN;
			case MOUNTAIN_STOMP -> STOMP_COOLDOWN;
			case GROUND_SPLITTING_LEAP -> LEAP_COOLDOWN;
			case THOUSAND_POUND_FIST -> FIST_COOLDOWN;
			case GOLDEN_BODY -> GOLDEN_COOLDOWN;
			case DIAMOND_WILL -> DIAMOND_COOLDOWN;
			case COMET_CHARGE -> COMET_COOLDOWN;
			case REBOUNDING_BONES -> REBOUND_COOLDOWN;
			case FLESH_REBORN -> REBORN_COOLDOWN;
			case HEAVEN_SHATTERING_FIST -> SHATTER_COOLDOWN;
			case EARTHSHAKER -> EARTHSHAKER_COOLDOWN;
			case SKY_PILLAR_STRIKE -> PILLAR_COOLDOWN;
			case IMPERISHABLE_GOLD -> GOLD_COOLDOWN;
			case PRIMORDIAL_ROAR -> ROAR_COOLDOWN;
			case MYRIAD_FISTS -> MYRIAD_COOLDOWN;
			case DHARMA_BODY -> DHARMA_COOLDOWN;
			default -> 0;
		};
	}

	/**
	 * {@link #cooldown} for one whose Laws are {@code c}'s: the Law of Time makes it as much shorter a level as it makes a spell's (Laws#cooldown,
	 * Laws#COOLDOWN_PER_LEVEL; the user's "Go, all three parts", 2026-11-01).
	 */
	public static int cooldown(PlayerCultivation c, Ability technique) {
		return (int) Math.round(cooldown(technique) * (1 - Laws.COOLDOWN_PER_LEVEL * c.laws().level(Law.TIME)));
	}

	/** {@code value} as the descriptions say it: a whole number bare ("5"), else to two places at most ("5.5", "3.25"). */
	public static String number(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(Math.round(value * 100) / 100.0);
	}

	/** {@code ticks} as seconds, the same way. */
	public static String seconds(int ticks) {
		return number(ticks / 20.0);
	}

	// --- Vigor ---

	/** Takes {@code cost} vigor from {@code p}'s body, and leaves it the hungrier; refused, told, on too little. */
	static boolean spend(ServerPlayer p, PlayerCultivation c, double cost) {
		if (c.body().vigor() < cost) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_VIGOR_SHORT, number(cost), (long) Math.floor(c.body().vigor()))
					.withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		c.body().setVigor(c.body().vigor() - cost);
		p.causeFoodExhaustion((float) (cost * HUNGER));
		return true;
	}

	/** {@code p} has eaten {@code nutrition} food points (Tempering#onEat): its body's vigor refilled by them, a share of its pool each. */
	public static void ate(ServerPlayer p, int nutrition) {
		PlayerCultivation c = CultivationManager.get(p);
		if (BodyPath.rank(c) < 0 || nutrition <= 0) return;
		c.body().setVigor(c.body().vigor() + nutrition * maxVigor(c) * FOOD_SHARE);
		CultivationManager.markDirty(p.server);
		tell(p, c);
	}

	/** Resting: not sprinting, with no blow given or taken for {@link #REST_AFTER} ticks. */
	static boolean resting(ServerPlayer p) {
		int since = Math.min(p.tickCount - p.getLastHurtByMobTimestamp(), p.tickCount - p.getLastHurtMobTimestamp());
		return !p.isSprinting() && since >= REST_AFTER;
	}

	/** The client told of {@code c}'s vigor, if its whole number has changed since it last was. */
	private static void tell(ServerPlayer p, PlayerCultivation c) {
		int whole = (int) Math.floor(c.body().vigor());
		Integer told = TOLD.put(p.getUUID(), whole);
		if (told == null || told != whole) CultivationManager.sync(p);
	}

	// --- Using them ---

	/**
	 * {@code p} uses {@code technique} (CultivationManager#toggleAbility: the wheel, the Abilities tab): Blood Frenzy switched on or off, any
	 * other done if its cooldown has passed and the vigor is there for it (Mountain Stomp, the leap and the charge from the ground alone).
	 */
	public static void act(ServerPlayer p, Ability technique) {
		PlayerCultivation c = CultivationManager.get(p);
		if (technique == Ability.BLOOD_FRENZY) {
			if (c.isAbilityEnabled(technique)) frenzyOff(p, c, false);
			else if (mayAct(p)) frenzyOn(p, c);
			return;
		}
		if (technique == Ability.CLOUD_TREADING) {
			if (c.isAbilityEnabled(technique)) treadOff(p, c, false);
			else if (mayAct(p)) treadOn(p, c);
			return;
		}
		if (!mayAct(p)) return;
		// High in the Jade Firmament's sky a Star-Forged Body's charge shatters space itself: a Void Breach torn there (VoidBreach#bodyTears).
		boolean tears = technique == Ability.COMET_CHARGE && VoidBreach.bodyTears(p, c);
		if (needsGround(technique) && !p.onGround() && !tears) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_BODY_NEEDS_GROUND).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		long now = p.serverLevel().getGameTime();
		Map<Ability, Long> ready = READY.computeIfAbsent(p.getUUID(), id -> new EnumMap<>(Ability.class));
		long at = ready.getOrDefault(technique, 0L);
		if (at > now) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_GIFT_COOLDOWN, (at - now + 19) / 20), true);
			return;
		}
		if (!spend(p, c, cost(technique))) return;
		ready.put(technique, now + cooldown(c, technique));
		int rank = BodyPath.rank(c);
		switch (technique) {
			case IRON_SKIN -> ironSkin(p, now);
			case MOUNTAIN_STOMP -> stomp(p, rank);
			case GROUND_SPLITTING_LEAP -> leap(p, rank, now, flatLook(p), leapForward(rank));
			case THOUSAND_POUND_FIST -> primeFist(p, now);
			case GOLDEN_BODY -> goldenBody(p, rank, now);
			case DIAMOND_WILL -> diamondWill(p, rank, now);
			case COMET_CHARGE -> {
				if (tears) tearSpace(p);
				else comet(p, now, flatLook(p), COMET_SPEED * cometBlocks(rank) / COMET_BLOCKS, cometBlocks(rank));
			}
			case REBOUNDING_BONES -> reboundingBones(p, rank, now);
			case FLESH_REBORN -> fleshReborn(p, rank, now);
			case HEAVEN_SHATTERING_FIST -> shatter(p, p.getEyePosition(), p.getLookAngle());
			case EARTHSHAKER -> earthshaker(p, rank, now);
			case SKY_PILLAR_STRIKE -> skyPillar(p, now);
			case IMPERISHABLE_GOLD -> imperishableGold(p, rank, now);
			case PRIMORDIAL_ROAR -> roar(p, rank);
			case MYRIAD_FISTS -> myriadFists(p, now);
			case DHARMA_BODY -> dharmaBody(p, now);
			default -> {
			}
		}
		TOLD.put(p.getUUID(), (int) Math.floor(c.body().vigor()));
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
	}

	/** Mountain Stomp, the leap, the charge, Earthshaker and the Sky-Pillar Strike: done from the ground alone. */
	static boolean needsGround(Ability technique) {
		return technique == Ability.MOUNTAIN_STOMP || technique == Ability.GROUND_SPLITTING_LEAP || technique == Ability.COMET_CHARGE
				|| technique == Ability.EARTHSHAKER || technique == Ability.SKY_PILLAR_STRIKE;
	}

	/** Whether {@code p} may use a technique now: alive and in the world, in its own flesh (no soul out, none of the dead). */
	private static boolean mayAct(ServerPlayer p) {
		if (!p.isAlive() || p.isSpectator()) return false;
		if (SoulProjection.isAwayFromBody(p.getUUID()) || SoulProjection.isSoul(p)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return false;
		}
		if (Incorporeal.is(p)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_CANNOT), true);
			return false;
		}
		return true;
	}

	/**
	 * {@code npc}, a body cultivator, uses {@code technique} in a fight with {@code foe} (BodyCultivators#TechniqueGoal, which keeps its
	 * cooldowns): as a player of its realm would, with no vigor spent; its leap and its charge go at {@code foe} and no further than it, its
	 * Heaven-Shattering Fist where it stands. @return whether it was done (a technique of a realm past its own, or one of the ground's while
	 * it is off the ground, isn't)
	 */
	public static boolean npcAct(CultivatorNpc npc, Ability technique, LivingEntity foe) {
		if (!(npc.level() instanceof ServerLevel level) || !npc.isAlive() || !npc.isBodyCultivator()) return false;
		int rank = rankOf(npc);
		if (!knows(rank, technique) || needsGround(technique) && !npc.onGround()) return false;
		long now = level.getGameTime();
		NPCS.put(npc.getUUID(), npc);
		Vec3 to = foe == null ? flatLook(npc) : flatToward(npc, foe);
		double distance = foe == null ? 0 : Math.sqrt(Math.pow(foe.getX() - npc.getX(), 2) + Math.pow(foe.getZ() - npc.getZ(), 2));
		switch (technique) {
			case IRON_SKIN -> ironSkin(npc, now);
			case MOUNTAIN_STOMP -> stomp(npc, rank);
			case GROUND_SPLITTING_LEAP -> {
				// Thrown only as far as its foe stands (a leap's full throw carries some LEAP_BLOCKS_PER_FORWARD blocks for each unit of it).
				double full = leapForward(rank);
				double scale = foe == null ? 1 : Math.max(0.35, Math.min(1, (distance - 1.0) / (full * LEAP_BLOCKS_PER_FORWARD)));
				leap(npc, rank, now, to, full * scale);
			}
			case THOUSAND_POUND_FIST -> primeFist(npc, now);
			case BLOOD_FRENZY -> npcFrenzy(npc, now);
			case GOLDEN_BODY -> goldenBody(npc, rank, now);
			case DIAMOND_WILL -> diamondWill(npc, rank, now);
			case COMET_CHARGE -> {
				// On through its foe, a little past it, but no further than the charge carries one of its realm: thrown as a player is for
				// so far (a mob's weight the same as a player's, measured in game: 1.85 carried one 7.3 blocks, 2.93 a player 12.4).
				double blocks = foe == null ? cometBlocks(rank) : Math.min(cometBlocks(rank), distance + 2);
				comet(npc, now, to, COMET_SPEED * blocks / COMET_BLOCKS, (int) Math.ceil(blocks));
			}
			case REBOUNDING_BONES -> reboundingBones(npc, rank, now);
			case FLESH_REBORN -> fleshReborn(npc, rank, now);
			case HEAVEN_SHATTERING_FIST -> {
				Vec3 eye = npc.getEyePosition();
				Vec3 look = foe == null ? npc.getLookAngle() : HeavenSpells.middle(foe).subtract(eye);
				if (foe != null) CultivatorSpells.face(npc, foe);
				shatter(npc, eye, look.lengthSqr() < 1.0e-4 ? npc.getLookAngle() : look.normalize());
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	/** Iron Skin: the skin turned to iron, no knockback while it holds. */
	private static void ironSkin(LivingEntity user, long now) {
		IRON_SKIN.put(user.getUUID(), now + IRON_SKIN_TICKS);
		unmoved(user, IRON_SKIN_ID, "Iron Skin", true);
		on(user, Ability.IRON_SKIN);
		ServerLevel level = level(user);
		ring(user, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_INGOT)), 18, 1.1, 0.7, 0.08);
		ring(user, ParticleTypes.WAX_OFF, 10, 1.0, 0.3, 0.5);
		level.playSound(null, user.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, user.getSoundSource(), 1.0f, 0.7f);
		level.playSound(null, user.blockPosition(), SoundEvents.ANVIL_PLACE, user.getSoundSource(), 0.35f, 1.6f);
		tell(user, Component.translatable(ModLang.MSG_IRON_SKIN, seconds(IRON_SKIN_TICKS)).withStyle(ChatFormatting.GRAY));
	}

	/** Mountain Stomp: the ground struck, and every foe near struck with it. */
	private static void stomp(LivingEntity user, int rank) {
		double radius = stompRadius(rank);
		int struck = shockwave(user, radius, STOMP_SHARE, 1.0, STOMP_SLOW_TICKS);
		ServerLevel level = level(user);
		BlockState ground = groundUnder(user);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, user.position().add(0, 0.05, 0), Vec3.ZERO, (float) radius, 0);
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, user.position(), Vec3.ZERO, 1.4f, Block.getId(ground));
		// The ground's own chips thrown out in a ring round the foot.
		BlockParticleOption chips = new BlockParticleOption(ParticleTypes.BLOCK, ground);
		for (int i = 0; i < 24; i++) {
			double a = Math.PI * 2 * i / 24;
			level.sendParticles(chips, user.getX() + Math.cos(a) * 1.3, user.getY() + 0.1, user.getZ() + Math.sin(a) * 1.3, 3, 0.2, 0.05, 0.2, 0.15);
		}
		level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, user.getSoundSource(), 0.9f, 0.6f);
		level.playSound(null, user.blockPosition(), SoundEvents.ANVIL_LAND, user.getSoundSource(), 0.5f, 0.5f);
		tell(user, Component.translatable(ModLang.MSG_STOMPED, struck));
		dharmaStomps(user, level.getGameTime()); // one's Dharma Body, if it towers, stamps with one
	}

	/** Where {@code e} looks, flat on the ground (its facing, looking straight up or down). */
	private static Vec3 flatLook(LivingEntity e) {
		Vec3 look = e.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, e.getYRot()) : flat.normalize();
	}

	/** The way from {@code from} to {@code to}, flat on the ground (where {@code from} looks, if they stand one over the other). */
	private static Vec3 flatToward(LivingEntity from, LivingEntity to) {
		Vec3 way = new Vec3(to.getX() - from.getX(), 0, to.getZ() - from.getZ());
		return way.lengthSqr() < 1.0e-4 ? flatLook(from) : way.normalize();
	}

	/** Ground-Splitting Leap: thrown {@code forward} along {@code flat} and up, safe from the fall till one lands. */
	private static void leap(LivingEntity user, int rank, long now, Vec3 flat, double forward) {
		double up = LEAP_UP + LEAP_UP_PER_REALM * realmsPast(rank, Realm.CORE_FORMATION);
		user.setDeltaMovement(flat.x * forward, up, flat.z * forward);
		user.hurtMarked = true;
		user.fallDistance = 0;
		LEAPS.put(user.getUUID(), new Leap(now));
		on(user, Ability.GROUND_SPLITTING_LEAP);
		ServerLevel level = level(user);
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, user.position(), Vec3.ZERO, 1.0f, Block.getId(groundUnder(user)));
		level.playSound(null, user.blockPosition(), SoundEvents.GOAT_LONG_JUMP, user.getSoundSource(), 1.0f, 0.7f);
		level.playSound(null, user.blockPosition(), SoundEvents.RAVAGER_STEP, user.getSoundSource(), 1.0f, 0.6f);
	}

	/** The leap come down: a shockwave where one lands. */
	private static void land(LivingEntity user) {
		off(user, Ability.GROUND_SPLITTING_LEAP);
		double radius = landingRadius(rankOf(user));
		shockwave(user, radius, LANDING_SHARE, 0.8, 0);
		ServerLevel level = level(user);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, user.position().add(0, 0.05, 0), Vec3.ZERO, (float) radius, 0);
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, user.position(), Vec3.ZERO, 1.2f, Block.getId(groundUnder(user)));
		level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, user.getSoundSource(), 0.7f, 0.8f);
		user.fallDistance = 0;
	}

	/** Thousand-Pound Fist: the next blow of the hand waits primed. */
	private static void primeFist(LivingEntity user, long now) {
		FIST.put(user.getUUID(), now + FIST_TICKS);
		on(user, Ability.THOUSAND_POUND_FIST);
		ServerLevel level = level(user);
		level.playSound(null, user.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE, user.getSoundSource(), 1.0f, 0.6f);
		level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, user.getSoundSource(), 0.6f, 0.5f);
		tell(user, Component.translatable(ModLang.MSG_FIST_PRIMED, seconds(FIST_TICKS)));
	}

	/** Golden Body: every blow turned aside while it holds. */
	private static void goldenBody(LivingEntity user, int rank, long now) {
		int ticks = goldenTicks(rank);
		GOLDEN.put(user.getUUID(), now + ticks);
		on(user, Ability.GOLDEN_BODY);
		ServerLevel level = level(user);
		ring(user, GOLD, 28, 1.1, 0.6, 0);
		ring(user, ParticleTypes.END_ROD, 12, 1.2, 0.2, 0.04);
		level.playSound(null, user.blockPosition(), SoundEvents.BELL_BLOCK, user.getSoundSource(), 1.0f, 1.3f);
		level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, user.getSoundSource(), 0.6f, 1.6f);
		tell(user, Component.translatable(ModLang.MSG_GOLDEN_BODY, seconds(ticks)).withStyle(ChatFormatting.GOLD));
	}

	/** Diamond Will: what harms one cleared away, nothing taking hold or moving one while it holds, no one's pressure on one. */
	private static void diamondWill(LivingEntity user, int rank, long now) {
		int ticks = diamondTicks(rank);
		DIAMOND_WILL.put(user.getUUID(), now + ticks);
		clearHarm(user);
		unmoved(user, DIAMOND_ID, "Diamond Will", true);
		on(user, Ability.DIAMOND_WILL);
		RealmSuppressSystem.refreshNow(); // the pressure on one lifts at once
		ServerLevel level = level(user);
		ring(user, DIAMOND, 24, 1.1, 0.6, 0);
		ring(user, ParticleTypes.END_ROD, 10, 1.2, 0.3, 0.03);
		level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, user.getSoundSource(), 1.5f, 0.7f);
		level.playSound(null, user.blockPosition(), SoundEvents.BEACON_POWER_SELECT, user.getSoundSource(), 0.6f, 1.5f);
		tell(user, Component.translatable(ModLang.MSG_DIAMOND_WILL, seconds(ticks)).withStyle(ChatFormatting.AQUA));
	}

	/**
	 * Comet Charge: hurled along the ground {@code way} at {@code speed}, striking every foe one passes through (#comets), safe from the fall
	 * meanwhile; its streak runs on {@code blocks} ahead.
	 */
	private static void comet(LivingEntity user, long now, Vec3 way, double speed, int blocks) {
		user.setDeltaMovement(way.x * speed, COMET_LIFT, way.z * speed);
		user.hurtMarked = true;
		user.fallDistance = 0;
		COMETS.put(user.getUUID(), new Comet(now, way.x, way.z));
		on(user, Ability.COMET_CHARGE);
		ServerLevel level = level(user);
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, user.position(), Vec3.ZERO, 0.9f, Block.getId(groundUnder(user)));
		// The falling star's streak racing on ahead along the way it goes, as far as it carries.
		Vec3 from = user.position().add(0, 0.9, 0);
		SpellFx.send(level, SpellFx.Kind.STREAK, from, from.add(way.scale(blocks)), 0, 0xFFB648);
		level.playSound(null, user.blockPosition(), SoundEvents.FIRECHARGE_USE, user.getSoundSource(), 1.0f, 0.6f);
		level.playSound(null, user.blockPosition(), SoundEvents.PHANTOM_SWOOP, user.getSoundSource(), 1.0f, 0.7f);
	}

	/** Rebounding Bones: blows struck back on their dealers while it holds (#guard). */
	private static void reboundingBones(LivingEntity user, int rank, long now) {
		int ticks = reboundTicks(rank);
		REBOUND.put(user.getUUID(), now + ticks);
		on(user, Ability.REBOUNDING_BONES);
		ServerLevel level = level(user);
		ring(user, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BONE)), 18, 1.1, 0.7, 0.08);
		ring(user, ParticleTypes.WHITE_ASH, 16, 1.0, 0.4, 0.02);
		level.playSound(null, user.blockPosition(), SoundEvents.BONE_BLOCK_BREAK, user.getSoundSource(), 1.2f, 0.5f);
		level.playSound(null, user.blockPosition(), SoundEvents.SKELETON_AMBIENT, user.getSoundSource(), 0.5f, 0.6f);
		tell(user, Component.translatable(ModLang.MSG_REBOUNDING_BONES, seconds(ticks)).withStyle(ChatFormatting.GRAY));
	}

	/** Flesh Reborn: a share of one's health back at once, fire and harm gone, and the rest growing back (#tick). */
	private static void fleshReborn(LivingEntity user, int rank, long now) {
		user.heal((float) (user.getMaxHealth() * rebornNow(rank)));
		user.clearFire();
		clearHarm(user);
		REBORN.put(user.getUUID(), now + REBORN_TICKS);
		on(user, Ability.FLESH_REBORN);
		ServerLevel level = level(user);
		ring(user, ParticleTypes.HAPPY_VILLAGER, 16, 1.1, 0.4, 0);
		ring(user, ParticleTypes.HEART, 5, 1.0, 1.0, 0);
		ring(user, FLESH, 20, 1.0, 0.7, 0);
		level.playSound(null, user.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, user.getSoundSource(), 0.5f, 1.5f);
		tell(user, Component.translatable(ModLang.MSG_FLESH_REBORN).withStyle(ChatFormatting.GREEN));
	}

	/**
	 * Heaven-Shattering Fist: every foe in the cone along {@code look} from {@code eye} struck and hurled, sonic rings running out along it, the
	 * ground bursting under them.
	 */
	private static void shatter(LivingEntity user, Vec3 eye, Vec3 look) {
		ServerLevel level = level(user);
		float harm = (float) (user.getAttributeValue(Attributes.ATTACK_DAMAGE) * SHATTER_SHARE);
		DamageSource source = ModDamageTypes.bodyArt(level, user);
		double cos = Math.cos(Math.toRadians(SHATTER_HALF_ANGLE));
		int struck = 0;
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(SHATTER_REACH), e -> reaches(user, e))) {
			Vec3 to = other.position().add(0, other.getBbHeight() / 2, 0).subtract(eye);
			double d = to.length();
			if (d > SHATTER_REACH || d < 1.0e-3) continue;
			double facing = to.scale(1 / d).dot(look);
			// Within the cone; or, close by, anywhere before one (a foe at one's shoulder is out of a narrow cone but not of the blow).
			if (facing < cos && (d > 2.5 || facing < 0)) continue;
			other.hurt(source, harm);
			Vec3 push = new Vec3(to.x, 0, to.z);
			push = push.lengthSqr() < 1.0e-4 ? new Vec3(look.x, 0, look.z) : push.normalize();
			other.knockback(SHATTER_HURL, -push.x, -push.z);
			other.hurtMarked = true;
			struck++;
		}
		for (double along = 2.5; along <= SHATTER_REACH; along += 1.75) {
			Vec3 at = eye.add(look.scale(along));
			level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		}
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0e-4 ? flatLook(user) : flat.normalize();
		for (double along = 4; along <= SHATTER_REACH; along += 4) {
			Vec3 at = user.position().add(flat.scale(along));
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, 1.2f, Block.getId(groundUnder(user)));
		}
		level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, user.getSoundSource(), 1.6f, 0.8f);
		level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, user.getSoundSource(), 1.2f, 0.5f);
		tell(user, Component.translatable(ModLang.MSG_SHATTERED, struck));
	}

	// --- The ladder's techniques (the user's "Go, all three parts", 2026-11-01, part three) ---

	/** Earthshaker: the ground struck so the earth heaves out round one in rings ({@link #shakes}); one's Dharma Body stomps with one. */
	private static void earthshaker(LivingEntity user, int rank, long now) {
		SHAKES.put(user.getUUID(), new Shake(now, user.position(), earthshakerReach(rank)));
		on(user, Ability.EARTHSHAKER);
		ServerLevel level = level(user);
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, user.position(), Vec3.ZERO, 2.0f, Block.getId(groundUnder(user)));
		level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_EMERGE, user.getSoundSource(), 1.2f, 0.6f);
		level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, user.getSoundSource(), 1.0f, 0.5f);
		dharmaStomps(user, now);
	}

	/**
	 * One of Earthshaker's rings: every foe between {@code inner} and {@code outer} of {@code centre} (and no more than {@link #EARTHSHAKER_HEIGHT}
	 * above or below it) struck for {@link #EARTHSHAKER_SHARE} of one's blow and tossed up, the earth heaving along the ring. @return how many
	 */
	private static int earthRing(LivingEntity user, Vec3 centre, double inner, double outer, int ring) {
		ServerLevel level = level(user);
		float harm = (float) (user.getAttributeValue(Attributes.ATTACK_DAMAGE) * EARTHSHAKER_SHARE);
		DamageSource source = ModDamageTypes.bodyArt(level, user);
		int struck = 0;
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(outer, EARTHSHAKER_HEIGHT, outer),
				o -> reaches(user, o))) {
			double dx = other.getX() - centre.x, dz = other.getZ() - centre.z, d = Math.sqrt(dx * dx + dz * dz);
			if (inner > 0 && d <= inner || d > outer || Math.abs(other.getY() - centre.y) > EARTHSHAKER_HEIGHT) continue;
			other.invulnerableTime = 0; // a ring's blow is its own, not the last ring's again
			other.hurt(source, harm);
			Vec3 out = d < 1.0e-3 ? Vec3.ZERO : new Vec3(dx / d, 0, dz / d);
			other.setDeltaMovement(other.getDeltaMovement().multiply(0.3, 0, 0.3).add(out.scale(0.35)).add(0, EARTHSHAKER_TOSS, 0));
			other.hurtMarked = true;
			struck++;
		}
		// The earth heaving up along the ring: its shockwave, and the ground bursting at points round it.
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, centre.add(0, 0.05, 0), Vec3.ZERO, (float) outer, 0);
		int bursts = Math.max(6, (int) Math.round(outer * 1.1));
		for (int i = 0; i < bursts; i++) {
			double a = Math.PI * 2 * (i + 0.5 * ring) / bursts;
			Vec3 at = centre.add(Math.cos(a) * outer, 0, Math.sin(a) * outer);
			Vec3 ground = HeavenSpells.groundBelow(level, at.add(0, 3, 0), 3, 6);
			Vec3 on = ground != null ? ground : at;
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, on, Vec3.ZERO, 1.3f, Block.getId(level.getBlockState(BlockPos.containing(on).below())));
		}
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE, user.getSoundSource(), 1.4f, 0.45f + 0.1f * ring);
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.ANVIL_LAND, user.getSoundSource(), 0.7f, 0.4f + 0.1f * ring);
		return struck;
	}

	/**
	 * Sky-Pillar Strike: thrown up high toward where one looks (no further than {@link #PILLAR_REACH}), a pillar of light where one rose; near
	 * the top of the throw one comes slamming down onto that place ({@link #pillars}), safe from the fall, and the land there takes the slam
	 * ({@link #pillarSlam}).
	 */
	private static void skyPillar(LivingEntity user, long now) {
		ServerLevel level = level(user);
		Vec3 look = HeavenSpells.lookPoint(user, PILLAR_REACH);
		Vec3 below = HeavenSpells.groundBelow(level, look.add(0, 2, 0), 4, 32);
		Vec3 target = below != null ? below : look;
		Vec3 flat = new Vec3(target.x - user.getX(), 0, target.z - user.getZ());
		double d = flat.length();
		Vec3 way = d < 0.5 ? Vec3.ZERO : flat.scale(1 / d);
		double carry = Math.min(1.8, d * PILLAR_CARRY);
		user.setDeltaMovement(way.x * carry, PILLAR_UP, way.z * carry);
		user.hurtMarked = true;
		user.fallDistance = 0;
		PILLARS.put(user.getUUID(), new Pillar(now, target));
		on(user, Ability.SKY_PILLAR_STRIKE);
		// A pillar of light where one rose, gold motes all up it, and the ground thrown up under one's feet.
		Vec3 foot = user.position();
		for (double y = 0; y < 16; y += 0.5) level.sendParticles(ParticleTypes.END_ROD, foot.x, foot.y + y, foot.z, 2, 0.12, 0.1, 0.12, 0.01);
		level.sendParticles(GOLD, foot.x, foot.y + 7, foot.z, 48, 0.35, 7, 0.35, 0);
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, foot, Vec3.ZERO, 1.5f, Block.getId(groundUnder(user)));
		level.playSound(null, user.blockPosition(), SoundEvents.FIRECHARGE_USE, user.getSoundSource(), 1.0f, 0.5f);
		level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, user.getSoundSource(), 1.4f, 0.6f);
	}

	/** The Sky-Pillar Strike come down: every foe within {@link #pillarRadius} of where one lands struck, flung out and tossed, the land burst. */
	private static void pillarSlam(LivingEntity user) {
		off(user, Ability.SKY_PILLAR_STRIKE);
		user.fallDistance = 0;
		double radius = pillarRadius(rankOf(user));
		ServerLevel level = level(user);
		float harm = (float) (user.getAttributeValue(Attributes.ATTACK_DAMAGE) * PILLAR_SHARE);
		DamageSource source = ModDamageTypes.bodyArt(level, user);
		int struck = 0;
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(radius, 3, radius),
				o -> reaches(user, o) && o.distanceToSqr(user) <= radius * radius)) {
			other.invulnerableTime = 0;
			other.hurt(source, harm);
			other.knockback(1.6, user.getX() - other.getX(), user.getZ() - other.getZ());
			other.setDeltaMovement(other.getDeltaMovement().add(0, PILLAR_TOSS, 0));
			other.hurtMarked = true;
			struck++;
		}
		Vec3 at = user.position();
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at.add(0, 0.05, 0), Vec3.ZERO, (float) radius, 0);
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, 2.6f, Block.getId(groundUnder(user)));
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8;
			Vec3 ring = at.add(Math.cos(a) * radius * 0.6, 0, Math.sin(a) * radius * 0.6);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ring, Vec3.ZERO, 1.4f, Block.getId(groundUnder(user)));
		}
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
		// The pillar of light again, standing up out of the blow.
		for (double y = 0; y < 18; y += 0.6) level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y, at.z, 2, 0.2, 0.1, 0.2, 0.02);
		level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, user.getSoundSource(), 1.6f, 0.5f);
		level.playSound(null, user.blockPosition(), SoundEvents.ANVIL_LAND, user.getSoundSource(), 1.0f, 0.5f);
		tell(user, Component.translatable(ModLang.MSG_SKY_PILLAR, struck));
	}

	/** Imperishable Gold: every blow and harmful effect turned aside while it holds, nothing moving one; then all it turned aside bursts out ({@link #goldBurst}). */
	private static void imperishableGold(LivingEntity user, int rank, long now) {
		int ticks = imperishableTicks(rank);
		IMPERISHABLE.put(user.getUUID(), now + ticks);
		TURNED.put(user.getUUID(), 0f);
		clearHarm(user);
		unmoved(user, IMPERISHABLE_ID, "Imperishable Gold", true);
		on(user, Ability.IMPERISHABLE_GOLD);
		ServerLevel level = level(user);
		ring(user, GOLD, 36, 1.2, 0.4, 0);
		ring(user, GOLD, 28, 1.1, 1.3, 0);
		ring(user, ParticleTypes.END_ROD, 16, 1.3, 0.9, 0.05);
		SpellFx.send(level, SpellFx.Kind.FLASH, user.position().add(0, 1, 0), Vec3.ZERO, 1.2f, 0xFFD24A);
		level.playSound(null, user.blockPosition(), SoundEvents.BELL_BLOCK, user.getSoundSource(), 1.2f, 0.8f);
		level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, user.getSoundSource(), 0.8f, 1.2f);
		level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, user.getSoundSource(), 1.0f, 0.6f);
		tell(user, Component.translatable(ModLang.MSG_IMPERISHABLE, seconds(ticks)).withStyle(ChatFormatting.GOLD));
	}

	/**
	 * Imperishable Gold run out on {@code user}: the harm it turned aside ({@code turned}, at most {@link #GOLD_BURST_MOST} times one's blow) burst
	 * out on every foe within {@link #GOLD_BURST_RADIUS}, thrown back; with nothing turned aside, the gold only fades.
	 */
	private static void goldBurst(LivingEntity user, float turned) {
		unmoved(user, IMPERISHABLE_ID, "Imperishable Gold", false);
		off(user, Ability.IMPERISHABLE_GOLD);
		ServerLevel level = level(user);
		float harm = (float) Math.min(turned, user.getAttributeValue(Attributes.ATTACK_DAMAGE) * GOLD_BURST_MOST);
		if (harm < 1) {
			ring(user, GOLD, 16, 1.0, 0.8, 0);
			level.playSound(null, user.blockPosition(), SoundEvents.BEACON_DEACTIVATE, user.getSoundSource(), 0.6f, 1.3f);
			return;
		}
		DamageSource source = ModDamageTypes.bodyArt(level, user);
		int struck = 0;
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(GOLD_BURST_RADIUS),
				o -> reaches(user, o) && o.distanceToSqr(user) <= GOLD_BURST_RADIUS * GOLD_BURST_RADIUS)) {
			other.invulnerableTime = 0;
			other.hurt(source, harm);
			other.knockback(1.8, user.getX() - other.getX(), user.getZ() - other.getZ());
			other.hurtMarked = true;
			struck++;
		}
		Vec3 at = user.position().add(0, 1, 0);
		SpellFx.send(level, SpellFx.Kind.FLASH, at, Vec3.ZERO, 3.0f, 0xFFD24A);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, user.position().add(0, 0.05, 0), Vec3.ZERO, (float) GOLD_BURST_RADIUS, 0);
		level.sendParticles(GOLD, at.x, at.y, at.z, 120, GOLD_BURST_RADIUS * 0.4, 0.8, GOLD_BURST_RADIUS * 0.4, 0);
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 40, 0.3, 0.3, 0.3, 0.5);
		level.playSound(null, user.blockPosition(), SoundEvents.BELL_BLOCK, user.getSoundSource(), 1.6f, 0.5f);
		level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, user.getSoundSource(), 1.2f, 0.8f);
		tell(user, Component.translatable(ModLang.MSG_GOLD_BURST, Math.round(harm), struck).withStyle(ChatFormatting.GOLD));
	}

	/**
	 * Primordial Roar: every foe within {@link #roarReach} stripped of its wards and its body's guards ({@link #stripWards}), stunned
	 * ({@link #stun}) and thrown back a little; rings of the roar running out from one's mouth.
	 */
	private static void roar(LivingEntity user, int rank) {
		double reach = roarReach(rank);
		ServerLevel level = level(user);
		int stunned = 0;
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(reach),
				o -> reaches(user, o) && o.distanceToSqr(user) <= reach * reach)) {
			stripWards(other);
			stun(other, user);
			other.knockback(0.6, user.getX() - other.getX(), user.getZ() - other.getZ());
			other.hurtMarked = true;
			stunned++;
		}
		Vec3 mouth = user.getEyePosition().subtract(0, 0.2, 0);
		for (double r = 2.5; r <= reach; r += 2.5) {
			int points = (int) Math.max(6, r * 1.2);
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2 * i / points;
				level.sendParticles(ParticleTypes.SONIC_BOOM, mouth.x + Math.cos(a) * r, mouth.y, mouth.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
			}
		}
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, user.position().add(0, 0.05, 0), Vec3.ZERO, (float) reach, 0);
		level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, user.getSoundSource(), 2.0f, 0.5f);
		level.playSound(null, user.blockPosition(), SoundEvents.RAVAGER_ROAR, user.getSoundSource(), 2.0f, 0.6f);
		level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_ROAR, user.getSoundSource(), 1.5f, 0.8f);
		tell(user, Component.translatable(ModLang.MSG_PRIMORDIAL_ROAR, stunned));
	}

	/**
	 * Every ward on {@code e} stripped from it: its element wards (SpellAuras: a Golden Bell, Vajra Body, a Black Tortoise's shell...) and its
	 * body's guards (Iron Skin, Golden Body, Diamond Will, Imperishable Gold, Rebounding Bones). @return how many
	 */
	static int stripWards(LivingEntity e) {
		int stripped = 0;
		for (SpellAura aura : SpellAura.values()) {
			if (aura.ward() == null || !SpellAuras.has(e, aura)) continue;
			SpellAuras.remove(e, aura);
			stripped++;
		}
		UUID id = e.getUUID();
		if (IRON_SKIN.remove(id) != null) {
			unmoved(e, IRON_SKIN_ID, "Iron Skin", false);
			off(e, Ability.IRON_SKIN);
			stripped++;
		}
		if (GOLDEN.remove(id) != null) {
			off(e, Ability.GOLDEN_BODY);
			stripped++;
		}
		if (DIAMOND_WILL.remove(id) != null) {
			unmoved(e, DIAMOND_ID, "Diamond Will", false);
			off(e, Ability.DIAMOND_WILL);
			RealmSuppressSystem.refreshNow();
			stripped++;
		}
		if (IMPERISHABLE.remove(id) != null) {
			TURNED.remove(id);
			unmoved(e, IMPERISHABLE_ID, "Imperishable Gold", false);
			off(e, Ability.IMPERISHABLE_GOLD);
			stripped++;
		}
		if (REBOUND.remove(id) != null) {
			off(e, Ability.REBOUNDING_BONES);
			stripped++;
		}
		return stripped;
	}

	/** {@code e} stunned by {@code by}'s roar for {@link #ROAR_STUN_TICKS}: rooted where it stands, its blows weak and slow; a mob's course broken. */
	private static void stun(LivingEntity e, LivingEntity by) {
		e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ROAR_STUN_TICKS, 9), by);
		e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ROAR_STUN_TICKS, 2), by);
		e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ROAR_STUN_TICKS, 2), by);
		if (e instanceof Mob mob) mob.getNavigation().stop();
	}

	/** Myriad Fists: a flurry of {@link #MYRIAD_BLOWS} blows before one, one every {@link #MYRIAD_EVERY} ticks ({@link #myriads}), the last hurling. */
	private static void myriadFists(LivingEntity user, long now) {
		MYRIADS.put(user.getUUID(), new Myriad(now));
		on(user, Ability.MYRIAD_FISTS);
		level(user).playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, user.getSoundSource(), 1.0f, 0.6f);
		tell(user, Component.translatable(ModLang.MSG_MYRIAD).withStyle(ChatFormatting.GOLD));
	}

	/**
	 * One of Myriad Fists' blows (the {@code n}th): every foe within {@link #MYRIAD_REACH} in the cone before one (or close by anywhere before
	 * one) struck for {@link #myriadShare} of one's blow, nudged back; the {@code last} hurled {@link #MYRIAD_HURL} at the tick's end.
	 */
	private static void flurryBlow(LivingEntity user, int n, boolean last) {
		ServerLevel level = level(user);
		float harm = (float) (user.getAttributeValue(Attributes.ATTACK_DAMAGE) * myriadShare(rankOf(user)));
		DamageSource source = ModDamageTypes.bodyArt(level, user);
		Vec3 eye = user.getEyePosition(), look = user.getLookAngle();
		double cos = Math.cos(Math.toRadians(MYRIAD_HALF_ANGLE));
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(MYRIAD_REACH), o -> reaches(user, o))) {
			Vec3 to = other.position().add(0, other.getBbHeight() / 2, 0).subtract(eye);
			double d = to.length();
			if (d > MYRIAD_REACH || d < 1.0e-3) continue;
			double facing = to.scale(1 / d).dot(look);
			if (facing < cos && (d > 1.8 || facing < 0)) continue;
			other.invulnerableTime = 0; // ten blows in two seconds: each its own
			other.hurt(source, harm);
			// Where it lands on the foe, seen by all, its user too (the fist's own flash is by one's eyes: others alone see it, below).
			Vec3 on = other.position().add(0, other.getBbHeight() * 0.6, 0);
			level.sendParticles(ParticleTypes.CRIT, on.x, on.y, on.z, 10, 0.25, 0.3, 0.25, 0.35);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, on.x, on.y, on.z, 1, 0, 0, 0, 0);
			Vec3 away = new Vec3(to.x, 0, to.z);
			away = away.lengthSqr() < 1.0e-4 ? flatLook(user) : away.normalize();
			if (last) HURLS.add(new Hurl(other, away.x, away.z, MYRIAD_HURL));
			else other.knockback(0.12, -away.x, -away.z);
			other.hurtMarked = true;
		}
		// Each fist's flash a little before one, left and right in turn (shown to others: it is by one's own eyes).
		Vec3 flat = flatLook(user), side = new Vec3(-flat.z, 0, flat.x);
		Vec3 at = eye.add(look.scale(1.7)).add(side.scale(n % 2 == 0 ? 0.4 : -0.4)).subtract(0, 0.25, 0);
		toOthers(user, ParticleTypes.CRIT, at, 8, 0.15, 0.35);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, user.getSoundSource(), 0.9f, 0.75f + (n % 3) * 0.15f);
		if (last) {
			level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, user.getSoundSource(), 0.9f, 1.1f);
			level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, user.getSoundSource(), 1.2f, 0.6f);
		}
	}

	/**
	 * Dharma Body of Heaven and Earth: one's own body grown as tall as heaven and earth (SpellConstruct DHARMA_BODY, of gold, {@link #DHARMA_HEIGHT}
	 * blocks), standing behind one and striding after one for {@link #DHARMA_TICKS} ({@link #dharmas}); where one's blow of the hand lands its fist
	 * comes down out of the sky ({@link #dharmaFist}), and as one stomps it stamps its foot ({@link #dharmaStomps}).
	 */
	private static void dharmaBody(LivingEntity user, long now) {
		endDharma(user.getUUID());
		ServerLevel level = level(user);
		Vec3 at = dharmaSpot(user);
		SpellConstruct body = SpellConstruct.create(level, SpellConstruct.Kind.DHARMA_BODY, at, user.getYRot(), 0, DHARMA_TICKS, user)
				.size((float) (DHARMA_HEIGHT / 1.8)).target(user).spawn();
		DHARMAS.put(user.getUUID(), new Dharma(body, now + DHARMA_TICKS));
		on(user, Ability.DHARMA_BODY);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at.add(0, 0.05, 0), Vec3.ZERO, 12f, 0);
		SpellFx.send(level, SpellFx.Kind.FLASH, at.add(0, DHARMA_HEIGHT * 0.5, 0), Vec3.ZERO, 6.0f, 0xFFD24A);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, user.getSoundSource(), 3.0f, 0.5f);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDER_DRAGON_GROWL, user.getSoundSource(), 2.0f, 0.5f);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.BELL_BLOCK, user.getSoundSource(), 2.0f, 0.5f);
		tell(user, Component.translatable(ModLang.MSG_DHARMA, seconds(DHARMA_TICKS)).withStyle(ChatFormatting.GOLD));
	}

	/** Where one's Dharma Body stands: {@link #DHARMA_BEHIND} blocks behind one, on the ground there (aloft, at one's own height). */
	private static Vec3 dharmaSpot(LivingEntity user) {
		Vec3 behind = user.position().add(Vec3.directionFromRotation(0, user.getYRot()).scale(-DHARMA_BEHIND));
		Vec3 below = HeavenSpells.groundBelow(level(user), behind.add(0, 3, 0), 6, 12);
		return below != null ? below : behind;
	}

	/**
	 * One's blow of the hand landed on {@code target} for {@code harm} under the Dharma Body ({@link #outgoing}): it turns to it and raises an arm,
	 * and its fist (an AVATAR_HAND of gold, as great as it) comes down out of the sky there, landing HeavenDemonSpells#AVATAR_FALL ticks on
	 * ({@link #dharmaFistLands}); no more than one every {@link #DHARMA_FIST_EVERY} ticks.
	 */
	private static void dharmaFist(LivingEntity user, Dharma d, LivingEntity target, float harm, long now) {
		if (now < d.nextFist) return;
		d.nextFist = now + DHARMA_FIST_EVERY;
		ServerLevel level = level(user);
		Vec3 below = HeavenSpells.groundBelow(level, target.position().add(0, 2, 0), 4, 16);
		Vec3 ground = below != null ? below : target.position();
		float yaw = HeavenSpells.facing(ground.subtract(d.body.position()).multiply(1, 0, 1))[0];
		d.body.place(d.body.position(), yaw, 0);
		d.holdYawUntil = now + HeavenDemonSpells.AVATAR_FALL + 10;
		d.body.act(d.right ? "strike_right" : "strike_left");
		d.right = !d.right;
		SpellConstruct hand = SpellConstruct.create(level, SpellConstruct.Kind.AVATAR_HAND, ground, yaw, 0, HeavenDemonSpells.AVATAR_FALL + HeavenDemonSpells.AVATAR_PRESS, user)
				.size(d.body.size()).value(1).target(user).spawn();
		d.fists.add(new Fist(ground, now + HeavenDemonSpells.AVATAR_FALL, (float) (harm * DHARMA_FIST_SHARE), hand));
		level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.PHANTOM_SWOOP, user.getSoundSource(), 2.0f, 0.5f);
	}

	/** The Dharma Body's fist come down: every foe within {@link #DHARMA_FIST_RADIUS} of where it lands struck and thrown back, the land burst. */
	private static void dharmaFistLands(LivingEntity user, Fist fist) {
		fist.hand().act("press");
		ServerLevel level = level(user);
		DamageSource source = ModDamageTypes.bodyArt(level, user);
		Vec3 at = fist.ground();
		for (LivingEntity e : HeavenSpells.aroundPoint(level, at, DHARMA_FIST_RADIUS, o -> reaches(user, o))) {
			e.invulnerableTime = 0; // one's own blow struck it a moment ago: the fist's is its own
			e.hurt(source, fist.harm());
			e.knockback(1.2, at.x - e.getX(), at.z - e.getZ());
			e.hurtMarked = true;
		}
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at.add(0, 0.05, 0), Vec3.ZERO, (float) DHARMA_FIST_RADIUS, 0);
		SpellFx.send(level, SpellFx.Kind.FLASH, at.add(0, 1.5, 0), Vec3.ZERO, 3.0f, 0xFFD24A);
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, 2.0f, Block.getId(level.getBlockState(BlockPos.containing(at).below())));
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, user.getSoundSource(), 2.0f, 0.7f);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, user.getSoundSource(), 1.5f, 0.5f);
	}

	/** {@code user} stomps (Mountain Stomp, Earthshaker): its Dharma Body, if it towers, lifts its foot and stamps with it ({@link #dharmaStompLands}). */
	private static void dharmaStomps(LivingEntity user, long now) {
		Dharma d = DHARMAS.get(user.getUUID());
		if (d == null) return;
		d.body.act("stomp");
		d.stomps.add(now + DHARMA_STOMP_DELAY);
	}

	/** The Dharma Body's foot come down: every foe within {@link #DHARMA_STOMP_RADIUS} of its feet struck, flung back and tossed, the land burst round. */
	private static void dharmaStompLands(LivingEntity user, Dharma d) {
		ServerLevel level = level(user);
		Vec3 foot = d.body.position();
		float harm = (float) (user.getAttributeValue(Attributes.ATTACK_DAMAGE) * DHARMA_STOMP_SHARE);
		DamageSource source = ModDamageTypes.bodyArt(level, user);
		for (LivingEntity e : HeavenSpells.aroundPoint(level, foot, DHARMA_STOMP_RADIUS, o -> reaches(user, o))) {
			e.invulnerableTime = 0;
			e.hurt(source, harm);
			e.knockback(1.4, foot.x - e.getX(), foot.z - e.getZ());
			e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
			e.hurtMarked = true;
		}
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, foot.add(0, 0.05, 0), Vec3.ZERO, (float) DHARMA_STOMP_RADIUS, 0);
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, foot, Vec3.ZERO, 3.0f, Block.getId(level.getBlockState(BlockPos.containing(foot).below())));
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10;
			Vec3 at = foot.add(Math.cos(a) * DHARMA_STOMP_RADIUS * 0.5, 0, Math.sin(a) * DHARMA_STOMP_RADIUS * 0.5);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, 1.6f, Block.getId(level.getBlockState(BlockPos.containing(at).below())));
		}
		level.playSound(null, foot.x, foot.y, foot.z, SoundEvents.GENERIC_EXPLODE, user.getSoundSource(), 3.0f, 0.4f);
		level.playSound(null, foot.x, foot.y, foot.z, SoundEvents.WARDEN_EMERGE, user.getSoundSource(), 2.0f, 0.5f);
	}

	/** {@code id}'s Dharma Body gone: it and the fists it sent fade away. */
	private static void endDharma(UUID id) {
		Dharma d = DHARMAS.remove(id);
		if (d == null) return;
		d.body.dissolve();
		for (Fist fist : d.fists) fist.hand().dissolve();
		for (SpellConstruct hand : d.sent) hand.dissolve();
	}

	/** A Star-Forged Body's charge high in the Jade Firmament's sky: the streak of it, and space shattered before it (VoidBreach#open). */
	private static void tearSpace(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Vec3 from = p.position().add(0, 0.9, 0), look = p.getLookAngle();
		SpellFx.send(level, SpellFx.Kind.STREAK, from, from.add(look.scale(4)), 0, 0xFFB648);
		level.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, p.getSoundSource(), 1.0f, 0.5f);
		VoidBreach.open(p);
	}

	/** Cloud-Treading Steps switched on: refused on too little vigor for a second of it in the air. */
	private static void treadOn(ServerPlayer p, PlayerCultivation c) {
		if (c.body().vigor() < TREAD_PER_SECOND) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_VIGOR_SHORT, number(TREAD_PER_SECOND), (long) Math.floor(c.body().vigor()))
					.withStyle(ChatFormatting.GRAY), true);
			return;
		}
		c.setAbilityEnabled(Ability.CLOUD_TREADING, true);
		ring(p, ParticleTypes.CLOUD, 14, 0.9, 0.05, 0.02);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WOOL_PLACE, p.getSoundSource(), 1.0f, 0.6f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_TREAD_ON, number(TREAD_PER_SECOND)).withStyle(ChatFormatting.AQUA), true);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
	}

	/** Cloud-Treading Steps switched off: by its switch, or {@code spent} (its vigor run dry in the air: told; one falls). */
	private static void treadOff(ServerPlayer p, PlayerCultivation c, boolean spent) {
		c.setAbilityEnabled(Ability.CLOUD_TREADING, false);
		if (spent) p.displayClientMessage(Component.translatable(ModLang.MSG_TREAD_SPENT).withStyle(ChatFormatting.GRAY), true);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
	}

	/**
	 * Whether {@code c}'s body may stride on the air now (QiFlight flies it where no qi or sword carries one): Cloud-Treading Steps switched on,
	 * the body's (one of the living, Silver Marrow or past it), and a second of it in its vigor.
	 */
	public static boolean treads(PlayerCultivation c) {
		return c.isAbilityEnabled(Ability.CLOUD_TREADING) && Ability.CLOUD_TREADING.isUnlocked(c) && c.body().vigor() >= TREAD_PER_SECOND;
	}

	/** Blood Frenzy lit: refused on too little vigor for a second of it. */
	private static void frenzyOn(ServerPlayer p, PlayerCultivation c) {
		if (c.body().vigor() < FRENZY_PER_SECOND) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_VIGOR_SHORT, number(FRENZY_PER_SECOND), (long) Math.floor(c.body().vigor()))
					.withStyle(ChatFormatting.GRAY), true);
			return;
		}
		c.setAbilityEnabled(Ability.BLOOD_FRENZY, true);
		frenzyLit(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_FRENZY_ON).withStyle(ChatFormatting.RED), true);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
	}

	/** A body cultivator's Blood Frenzy lit, burning {@link #NPC_FRENZY_TICKS}. */
	private static void npcFrenzy(LivingEntity npc, long now) {
		FRENZY.put(npc.getUUID(), now + NPC_FRENZY_TICKS);
		frenzyLit(npc);
	}

	/** Blood Frenzy's swiftness, and the blood boiling up about one as it is lit. */
	private static void frenzyLit(LivingEntity user) {
		frenzySpeed(user, true);
		ring(user, BLOOD, 20, 1.0, 0.6, 0);
		level(user).playSound(null, user.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, user.getSoundSource(), 1.2f, 0.9f);
		level(user).playSound(null, user.blockPosition(), SoundEvents.RAVAGER_ROAR, user.getSoundSource(), 0.4f, 1.5f);
	}

	/** Blood Frenzy put out: by its switch, or {@code spent} (its vigor run dry: told). */
	private static void frenzyOff(ServerPlayer p, PlayerCultivation c, boolean spent) {
		c.setAbilityEnabled(Ability.BLOOD_FRENZY, false);
		frenzySpeed(p, false);
		if (spent) p.displayClientMessage(Component.translatable(ModLang.MSG_FRENZY_SPENT).withStyle(ChatFormatting.GRAY), true);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
	}

	private static void frenzySpeed(LivingEntity e, boolean on) {
		AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) return;
		speed.removeModifier(FRENZY_ID);
		if (on) speed.addTransientModifier(new AttributeModifier(FRENZY_ID, "Blood Frenzy", FRENZY_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	/** Whether {@code e}'s Blood Frenzy burns: a player's switch, a body cultivator's while it lasts. */
	private static boolean frenzied(LivingEntity e) {
		if (e instanceof ServerPlayer p) {
			PlayerCultivation c = CultivationManager.get(p);
			return c.isAbilityEnabled(Ability.BLOOD_FRENZY) && Ability.BLOOD_FRENZY.isUnlocked(c);
		}
		return holds(FRENZY, e);
	}

	/** Nothing knocks {@code e} back while {@code on}, by the hold {@code id} (Iron Skin's, Diamond Will's), or that hold let go. */
	private static void unmoved(LivingEntity e, UUID id, String name, boolean on) {
		AttributeInstance knock = e.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (knock == null) return;
		knock.removeModifier(id);
		if (on) knock.addTransientModifier(new AttributeModifier(id, name, 1.0, AttributeModifier.Operation.ADDITION));
	}

	/** Every harmful effect on {@code e} taken away. */
	private static void clearHarm(LivingEntity e) {
		for (MobEffectInstance effect : new ArrayList<>(e.getActiveEffects())) {
			if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) e.removeEffect(effect.getEffect());
		}
	}

	/** {@code act} shown "on" on {@code user}'s wheel (a player's: a body cultivator has none). */
	private static void on(LivingEntity user, Ability act) {
		if (user instanceof ServerPlayer p) CultivationManager.get(p).setAbilityEnabled(act, true);
	}

	/** {@code act}'s "on" shown no longer. */
	private static void off(LivingEntity user, Ability act) {
		if (!(user instanceof ServerPlayer p)) return;
		PlayerCultivation c = CultivationManager.get(p);
		if (!c.isAbilityEnabled(act)) return;
		c.setAbilityEnabled(act, false);
		CultivationManager.sync(p);
	}

	/** {@code message} over the hotbar, to a player using a technique (a body cultivator's go unsaid). */
	private static void tell(LivingEntity user, Component message) {
		if (user instanceof ServerPlayer p) p.displayClientMessage(message, true);
	}

	private static ServerLevel level(LivingEntity e) {
		return (ServerLevel) e.level();
	}

	/**
	 * Every foe within {@code radius} of {@code user} struck for {@code share} of its blow (its attack's strength, the body's own) as a body
	 * technique, knocked back {@code knock} and slowed {@code slowTicks} (0: not). @return how many it struck
	 */
	private static int shockwave(LivingEntity user, double radius, double share, double knock, int slowTicks) {
		float harm = (float) (user.getAttributeValue(Attributes.ATTACK_DAMAGE) * share);
		DamageSource source = ModDamageTypes.bodyArt(user.level(), user);
		int struck = 0;
		for (LivingEntity other : level(user).getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(radius, 2, radius),
				e -> reaches(user, e) && e.distanceToSqr(user) <= radius * radius)) {
			other.hurt(source, harm);
			other.knockback(knock, user.getX() - other.getX(), user.getZ() - other.getZ());
			other.hurtMarked = true;
			if (slowTicks > 0) other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1), user);
			struck++;
		}
		return struck;
	}

	/**
	 * Who a technique's blow reaches: not its own, the spared (HeavenSpells#spared: a player's allies, pets, players they may not harm; a
	 * cultivator's, whoever it isn't fighting), an armour stand, nor a soul or one of the dead out of the living's reach.
	 */
	private static boolean reaches(LivingEntity user, LivingEntity other) {
		return other != user && other.isAlive() && !(other instanceof ArmorStand) && !HeavenSpells.spared(user, other)
				&& !(other instanceof Player player && Incorporeal.is(player));
	}

	/** The block {@code e} stands on, for the dust and chips it throws up (dirt over nothing). */
	private static BlockState groundUnder(LivingEntity e) {
		BlockState ground = e.level().getBlockState(e.getOnPos());
		return ground.isAir() ? Blocks.DIRT.defaultBlockState() : ground;
	}

	/**
	 * {@code count} of {@code particle} in a ring {@code radius} round {@code e}, {@code height} over its feet: below its eyes and out from them
	 * (in first person what is drawn close by the eyes shows as great squares).
	 */
	private static void ring(LivingEntity e, ParticleOptions particle, int count, double radius, double height, double speed) {
		ServerLevel level = level(e);
		for (int i = 0; i < count; i++) {
			double a = Math.PI * 2 * i / count;
			level.sendParticles(particle, e.getX() + Math.cos(a) * radius, e.getY() + height, e.getZ() + Math.sin(a) * radius, 1, 0.05, 0.1, 0.05, speed);
		}
	}

	/** {@code count} of {@code particle} at a point, shown to everyone near but {@code e} (a player's own fist is by its eyes). */
	private static void toOthers(LivingEntity e, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
		ServerLevel level = level(e);
		for (ServerPlayer other : level.players()) {
			if (other != e) level.sendParticles(other, particle, false, at.x, at.y, at.z, count, spread, spread, spread, speed);
		}
	}

	// --- Blows ---

	/** A blow: harm something deals (an attacker, an arrow, a blast), not a fall, fire, hunger, drowning, nor what nothing stops (the void). */
	static boolean isBlow(DamageSource source) {
		return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
				&& (source.getEntity() != null || source.getDirectEntity() != null || source.is(DamageTypeTags.IS_EXPLOSION));
	}

	private static boolean holds(Map<UUID, Long> until, LivingEntity e) {
		if (until.isEmpty()) return false;
		Long end = until.get(e.getUUID());
		return end != null && e.level().getGameTime() < end;
	}

	/** Whether {@code e}'s heart is diamond now (Diamond Will): no one's Realm Suppress presses it (RealmSuppressSystem#canBePressed). */
	public static boolean unshaken(LivingEntity e) {
		return holds(DIAMOND_WILL, e);
	}

	/** Whether {@code effect} may take hold of {@code entity} (LivingEntity#canBeAffected): nothing harmful of one under Diamond Will or Imperishable Gold. */
	public static boolean allowEffect(LivingEntity entity, MobEffectInstance effect) {
		return effect.getEffect().getCategory() != MobEffectCategory.HARMFUL || !holds(DIAMOND_WILL, entity) && !holds(IMPERISHABLE, entity);
	}

	/**
	 * The harm {@code target} takes from {@code source} under its body's techniques (HeavenSpells#incoming, the heavens' own strikes already gone
	 * through untouched): a blow turned aside whole under Imperishable Gold (and counted for its burst) or Golden Body (0: refused outright, no
	 * knockback); under Rebounding Bones, {@link #reboundShare} of it struck back on its dealer; under Iron Skin, {@link #ironSkinCut} of it taken away.
	 */
	public static float guard(LivingEntity target, DamageSource source, float amount) {
		if (GOLDEN.isEmpty() && REBOUND.isEmpty() && IRON_SKIN.isEmpty() && IMPERISHABLE.isEmpty() || amount <= 0 || !isBlow(source)) return amount;
		if (holds(IMPERISHABLE, target)) {
			TURNED.merge(target.getUUID(), amount, Float::sum);
			struck(target, source, true);
			return 0;
		}
		if (holds(GOLDEN, target)) {
			struck(target, source, true);
			return 0;
		}
		if (holds(REBOUND, target) && !rebounding && source.getEntity() instanceof LivingEntity dealer && dealer != target && dealer.isAlive()
				&& dealer.level() == target.level() && dealer.distanceToSqr(target) <= REBOUND_REACH * REBOUND_REACH && reaches(target, dealer)) {
			strikeBack(target, dealer, (float) (amount * reboundShare(rankOf(target))));
		}
		if (holds(IRON_SKIN, target)) {
			struck(target, source, false);
			return (float) (amount * (1 - ironSkinCut(rankOf(target))));
		}
		return amount;
	}

	/** A blow meeting gold or iron skin: sparks where it struck, and a ring of the metal. */
	private static void struck(LivingEntity target, DamageSource source, boolean gold) {
		Vec3 from = source.getSourcePosition();
		Vec3 side = from == null ? target.getLookAngle() : from.subtract(target.position());
		side = new Vec3(side.x, 0, side.z);
		side = side.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 at = target.position().add(side.scale(0.9)).add(0, 1.0, 0);
		ServerLevel level = level(target);
		SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, at, side, gold ? 1.0f : 0.6f, 0);
		if (gold) level.playSound(null, target.blockPosition(), SoundEvents.BELL_BLOCK, target.getSoundSource(), 0.5f, 1.9f);
		else level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_HIT, target.getSoundSource(), 1.0f, 0.7f);
	}

	/** Rebounding Bones: {@code harm} struck back on {@code dealer} as {@code user}'s body technique (never struck back again by its own bones). */
	private static void strikeBack(LivingEntity user, LivingEntity dealer, float harm) {
		rebounding = true;
		try {
			dealer.hurt(ModDamageTypes.bodyArt(user.level(), user), harm);
		} finally {
			rebounding = false;
		}
		ServerLevel level = level(user);
		Vec3 at = dealer.position().add(0, dealer.getBbHeight() * 0.6, 0);
		SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, at, user.position().subtract(dealer.position()), 0.8f, 0);
		level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.3);
		level.playSound(null, dealer.blockPosition(), SoundEvents.BONE_BLOCK_HIT, user.getSoundSource(), 1.2f, 0.7f);
	}

	/**
	 * What {@code source}'s blow on {@code target} comes to under its dealer's body techniques (SoulLivingEntityMixin, as it is dealt): under Blood
	 * Frenzy, a blow of the hand or a body technique {@link #frenzyHarm} harder; a primed Thousand-Pound Fist's blow of the hand {@link #fistFactor}
	 * times as hard, its foe hurled at the tick's end. A body cultivator's blow of the hand is its own attack (a mob's), a player's theirs.
	 */
	public static float outgoing(LivingEntity target, DamageSource source, float amount) {
		if (!(source.getEntity() instanceof LivingEntity dealer) || dealer == target || amount <= 0) return amount;
		if (!(dealer instanceof ServerPlayer) && FRENZY.isEmpty() && FIST.isEmpty()) return amount;
		boolean hand = source.getDirectEntity() == dealer
				&& (dealer instanceof ServerPlayer ? source.is(DamageTypes.PLAYER_ATTACK) : source.is(DamageTypes.MOB_ATTACK));
		if (!hand && !source.is(ModDamageTypes.BODY_ART)) return amount;
		int rank = rankOf(dealer);
		if (frenzied(dealer)) amount *= (float) (1 + frenzyHarm(rank));
		if (hand && holds(FIST, dealer)) {
			FIST.remove(dealer.getUUID());
			amount *= (float) fistFactor(rank);
			fistLands(dealer, rank, target);
		}
		// Under the Dharma Body of Heaven and Earth, its fist comes down out of the sky where one's own lands (a blow of the hand only: not its own).
		if (hand && !DHARMAS.isEmpty()) {
			Dharma d = DHARMAS.get(dealer.getUUID());
			if (d != null) dharmaFist(dealer, d, target, amount, dealer.level().getGameTime());
		}
		return amount;
	}

	/** The fist landed: the foe hurled away at the tick's end, the blow's burst and boom. */
	private static void fistLands(LivingEntity dealer, int rank, LivingEntity target) {
		off(dealer, Ability.THOUSAND_POUND_FIST);
		Vec3 away = target.position().subtract(dealer.position());
		away = new Vec3(away.x, 0, away.z);
		away = away.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, dealer.getYRot()) : away.normalize();
		HURLS.add(new Hurl(target, away.x, away.z, FIST_HURL + FIST_HURL_PER_REALM * realmsPast(rank, Realm.NASCENT_SOUL)));
		ServerLevel level = level(dealer);
		Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 24, 0.3, 0.3, 0.3, 0.6);
		level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE, dealer.getSoundSource(), 0.6f, 1.5f);
		level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, dealer.getSoundSource(), 1.2f, 0.6f);
	}

	/** Whether a fall should do {@code e} no harm: on a leap, a charge or a Sky-Pillar Strike, till it is done (CultivationEvents' ALLOW_DAMAGE). */
	public static boolean fallIsSafe(LivingEntity e) {
		return LEAPS.containsKey(e.getUUID()) || COMETS.containsKey(e.getUUID()) || PILLARS.containsKey(e.getUUID());
	}

	/** Whether {@code e} holds Iron Skin, Golden Body, Diamond Will or Rebounding Bones now, or has a fist primed (the game tests, the tour). */
	static boolean holding(LivingEntity e, Ability act) {
		return switch (act) {
			case IRON_SKIN -> holds(IRON_SKIN, e);
			case THOUSAND_POUND_FIST -> holds(FIST, e);
			case BLOOD_FRENZY -> frenzied(e);
			case GOLDEN_BODY -> holds(GOLDEN, e);
			case DIAMOND_WILL -> holds(DIAMOND_WILL, e);
			case REBOUNDING_BONES -> holds(REBOUND, e);
			case FLESH_REBORN -> holds(REBORN, e);
			case GROUND_SPLITTING_LEAP -> LEAPS.containsKey(e.getUUID());
			case COMET_CHARGE -> COMETS.containsKey(e.getUUID());
			case EARTHSHAKER -> SHAKES.containsKey(e.getUUID());
			case SKY_PILLAR_STRIKE -> PILLARS.containsKey(e.getUUID());
			case IMPERISHABLE_GOLD -> holds(IMPERISHABLE, e);
			case MYRIAD_FISTS -> MYRIADS.containsKey(e.getUUID());
			case DHARMA_BODY -> DHARMAS.containsKey(e.getUUID());
			default -> false;
		};
	}

	/** The harm Imperishable Gold has turned aside for {@code e} so far (the game tests). */
	static float turned(LivingEntity e) {
		return TURNED.getOrDefault(e.getUUID(), 0f);
	}

	/** {@code e}'s Dharma Body's construct, or null (the game tests, the tour). */
	static SpellConstruct dharmaOf(LivingEntity e) {
		Dharma d = DHARMAS.get(e.getUUID());
		return d == null ? null : d.body;
	}

	// --- Every tick ---

	/** The one whose holds {@code id} names, if they are in the world: a player, or a body cultivator who has used a technique. */
	private static LivingEntity user(MinecraftServer server, UUID id) {
		ServerPlayer p = server.getPlayerList().getPlayer(id);
		if (p != null) return p;
		LivingEntity npc = NPCS.get(id);
		return npc == null || npc.isRemoved() ? null : npc;
	}

	public static void tick(MinecraftServer server) {
		// The fists' foes hurled, after their blows' own knockback.
		if (!HURLS.isEmpty()) {
			for (Hurl h : HURLS) {
				if (!h.target.isAlive() || h.target.isRemoved()) continue;
				h.target.knockback(h.strength, -h.x, -h.z);
				h.target.hurtMarked = true;
			}
			HURLS.clear();
		}
		long now = server.overworld().getGameTime();
		expire(server, IRON_SKIN, Ability.IRON_SKIN, now);
		expire(server, FIST, Ability.THOUSAND_POUND_FIST, now);
		expire(server, GOLDEN, Ability.GOLDEN_BODY, now);
		expire(server, DIAMOND_WILL, Ability.DIAMOND_WILL, now);
		expire(server, REBOUND, Ability.REBOUNDING_BONES, now);
		expire(server, REBORN, Ability.FLESH_REBORN, now);
		expire(server, FRENZY, Ability.BLOOD_FRENZY, now);
		leaps(server, now);
		comets(server, now);
		shakes(server, now);
		pillars(server, now);
		imperishables(server, now);
		myriads(server, now);
		dharmas(server, now);
		// The flesh growing back, a share of the health every tick.
		for (UUID id : REBORN.keySet()) {
			LivingEntity user = user(server, id);
			if (user == null || !user.isAlive()) continue;
			user.heal((float) (user.getMaxHealth() * REBORN_OVER / REBORN_TICKS));
			if (now % 10 == 0) ring(user, ParticleTypes.HAPPY_VILLAGER, 4, 0.9, 0.3 + (now / 10 % 3) * 0.3, 0);
		}
		// What holds, seen: blood boiling about one in a frenzy, gold about one turned to it, diamond's glints, bones' dust, iron's glints, a
		// fist primed (by others: it is at one's eyes).
		// Cloud-Treading Steps: a puff of cloud under each stride on the air.
		if (now % 3 == 0) {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (!QiFlight.treading(p)) continue;
				level(p).sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() - 0.15, p.getZ(), 4, 0.35, 0.04, 0.35, 0.01);
				if (now % 18 == 0 && p.getDeltaMovement().horizontalDistanceSqr() > 1.0e-3) {
					level(p).playSound(null, p.blockPosition(), SoundEvents.WOOL_STEP, p.getSoundSource(), 0.6f, 0.7f);
				}
			}
		}
		if (now % 5 == 0) {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (CultivationManager.get(p).isAbilityEnabled(Ability.BLOOD_FRENZY)) ring(p, BLOOD, 8, 0.9, 0.3 + (now / 5 % 3) * 0.3, 0);
			}
			for (UUID id : FRENZY.keySet()) {
				LivingEntity user = user(server, id);
				if (user == null) continue;
				ring(user, BLOOD, 8, 0.9, 0.3 + (now / 5 % 3) * 0.3, 0);
				if (now % 20 == 0) ring(user, ParticleTypes.CRIMSON_SPORE, 6, 1.0, 0.8, 0.02);
			}
		}
		if (now % 4 == 0) {
			for (UUID id : GOLDEN.keySet()) {
				LivingEntity user = user(server, id);
				if (user != null) ring(user, GOLD, 10, 1.0, 0.3 + (now / 4 % 3) * 0.35, 0);
			}
			// Imperishable Gold: a body of gold, brighter, light rising off it.
			for (UUID id : IMPERISHABLE.keySet()) {
				LivingEntity user = user(server, id);
				if (user == null) continue;
				ring(user, GOLD, 14, 1.05, 0.2 + (now / 4 % 4) * 0.45, 0);
				ring(user, ParticleTypes.END_ROD, 3, 0.8, 1.9, 0.02);
			}
			for (UUID id : FIST.keySet()) {
				LivingEntity user = user(server, id);
				if (user == null) continue;
				Vec3 look = Vec3.directionFromRotation(0, user.getYRot());
				toOthers(user, ParticleTypes.CRIT, user.position().add(-look.z * 0.4 + look.x * 0.3, 1.0, look.x * 0.4 + look.z * 0.3), 3, 0.08, 0.05);
			}
		}
		if (now % 6 == 0) {
			for (UUID id : DIAMOND_WILL.keySet()) {
				LivingEntity user = user(server, id);
				if (user != null) ring(user, DIAMOND, 6, 1.0, 0.3 + (now / 6 % 3) * 0.3, 0);
			}
		}
		if (now % 8 == 0) {
			for (UUID id : REBOUND.keySet()) {
				LivingEntity user = user(server, id);
				if (user != null) ring(user, ParticleTypes.WHITE_ASH, 6, 0.9, 0.3 + (now / 8 % 3) * 0.3, 0.02);
			}
		}
		if (now % 10 == 0) {
			for (UUID id : IRON_SKIN.keySet()) {
				LivingEntity user = user(server, id);
				if (user != null) ring(user, ParticleTypes.WAX_OFF, 4, 0.9, 0.3 + (now / 10 % 3) * 0.3, 0.3);
			}
		}
		if (server.getTickCount() % 20 == 0) {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) second(p);
			forgetGone();
		}
	}

	/** Body cultivators dead or gone from the world (their land unloaded, carried to another): what they held goes with them. */
	private static void forgetGone() {
		if (NPCS.isEmpty()) return;
		for (Iterator<Map.Entry<UUID, LivingEntity>> it = NPCS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, LivingEntity> e = it.next();
			LivingEntity npc = e.getValue();
			UUID id = e.getKey();
			if (npc.isRemoved() || !npc.isAlive()) {
				it.remove();
				endHolds(id);
			} else if (!holdsAny(id)) {
				it.remove(); // nothing under way: found again when it next acts
			}
		}
	}

	/** Whether anything of {@code id}'s holds or is under way. */
	private static boolean holdsAny(UUID id) {
		return IRON_SKIN.containsKey(id) || FIST.containsKey(id) || GOLDEN.containsKey(id) || DIAMOND_WILL.containsKey(id) || REBOUND.containsKey(id)
				|| REBORN.containsKey(id) || FRENZY.containsKey(id) || LEAPS.containsKey(id) || COMETS.containsKey(id) || SHAKES.containsKey(id)
				|| PILLARS.containsKey(id) || IMPERISHABLE.containsKey(id) || MYRIADS.containsKey(id) || DHARMAS.containsKey(id);
	}

	/** Acts in {@code until} run out: their "on" gone from the wheel, a hold against knockback or a frenzy's swiftness with it, Diamond Will's pressure back. */
	private static void expire(MinecraftServer server, Map<UUID, Long> until, Ability act, long now) {
		if (until.isEmpty()) return;
		boolean pressure = false;
		for (Iterator<Map.Entry<UUID, Long>> it = until.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Long> e = it.next();
			if (now < e.getValue()) continue;
			it.remove();
			LivingEntity user = user(server, e.getKey());
			if (user == null) continue;
			if (act == Ability.IRON_SKIN) unmoved(user, IRON_SKIN_ID, "Iron Skin", false);
			if (act == Ability.DIAMOND_WILL) {
				unmoved(user, DIAMOND_ID, "Diamond Will", false);
				pressure = true;
			}
			if (act == Ability.BLOOD_FRENZY) frenzySpeed(user, false); // a body cultivator's, burnt out
			off(user, act);
		}
		if (pressure) RealmSuppressSystem.refreshNow();
	}

	/** Those on a leap: off the ground, then down again (a shockwave), or let go (flown, swum, blocked, too long in the air). */
	private static void leaps(MinecraftServer server, long now) {
		if (LEAPS.isEmpty()) return;
		// Gone through a copy, as every one of these below: a blow dealt on the way may kill a player with one of their own under way, and
		// a death ends it (#end) while the rest are gone through (it threw, before the sweep of 2026-11-01).
		for (UUID id : List.copyOf(LEAPS.keySet())) {
			Leap leap = LEAPS.get(id);
			if (leap == null) continue; // ended by a death on the way
			LivingEntity user = user(server, id);
			if (user == null || !user.isAlive()) {
				LEAPS.remove(id);
				continue;
			}
			long since = now - leap.at;
			if (!leap.aloft && !user.onGround()) leap.aloft = true;
			if (leap.aloft && user.onGround()) {
				LEAPS.remove(id);
				land(user);
			} else if (flying(user) || leap.aloft && user.isInWater() || since > LEAP_MOST || !leap.aloft && since > LEAP_TAKEOFF) {
				LEAPS.remove(id);
				user.fallDistance = 0;
				off(user, Ability.GROUND_SPLITTING_LEAP);
			}
		}
	}

	/** In the air by one's own power: a player flying or gliding (a body cultivator never flies). */
	private static boolean flying(LivingEntity user) {
		return user instanceof Player p && p.getAbilities().flying || user.isFallFlying();
	}

	/**
	 * Those on a Comet Charge: every foe they pass through struck once and thrown aside (away from the way it goes, and on along it), a blazing
	 * trail behind them; done once they are down again, or too long under way.
	 */
	private static void comets(MinecraftServer server, long now) {
		if (COMETS.isEmpty()) return;
		for (UUID id : List.copyOf(COMETS.keySet())) {
			Comet comet = COMETS.get(id);
			if (comet == null) continue; // ended by a death on the way
			LivingEntity user = user(server, id);
			if (user == null || !user.isAlive()) {
				COMETS.remove(id);
				continue;
			}
			ServerLevel level = level(user);
			float harm = (float) (user.getAttributeValue(Attributes.ATTACK_DAMAGE) * COMET_SHARE);
			for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(COMET_REACH),
					o -> reaches(user, o) && !comet.struck.contains(o.getUUID()))) {
				if (!user.isAlive()) break; // fallen to what came back off a blow (thorns, say)
				comet.struck.add(other.getUUID());
				other.hurt(ModDamageTypes.bodyArt(level, user), harm);
				// Aside: away from the charge's line, and a little on along it.
				double side = (other.getX() - user.getX()) * -comet.z + (other.getZ() - user.getZ()) * comet.x;
				double sx = -comet.z * Math.signum(side == 0 ? 1 : side), sz = comet.x * Math.signum(side == 0 ? 1 : side);
				Vec3 throwWay = new Vec3(sx + comet.x * 0.5, 0, sz + comet.z * 0.5).normalize();
				other.knockback(1.4, -throwWay.x, -throwWay.z);
				other.hurtMarked = true;
				Vec3 at = other.position().add(0, other.getBbHeight() * 0.6, 0);
				level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.4);
				level.playSound(null, other.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, user.getSoundSource(), 1.2f, 0.7f);
			}
			// The blazing trail, a block behind (out of its own sight ahead).
			Vec3 tail = user.position().add(-comet.x, 0.5, -comet.z);
			level.sendParticles(ParticleTypes.FLAME, tail.x, tail.y, tail.z, 8, 0.25, 0.25, 0.25, 0.02);
			level.sendParticles(ParticleTypes.END_ROD, tail.x, tail.y, tail.z, 3, 0.2, 0.2, 0.2, 0.02);
			if (COMETS.get(id) != comet) continue; // ended by its user's death on the way
			long since = now - comet.at;
			if (!comet.aloft && !user.onGround()) comet.aloft = true;
			if (comet.aloft && user.onGround() && since >= 3 || since > COMET_MOST || flying(user) || user.isInWater() && comet.aloft) {
				COMETS.remove(id);
				user.fallDistance = 0;
				off(user, Ability.COMET_CHARGE);
			}
		}
	}

	/** Earthshakers under way: each ring as it comes due runs out from where the ground was struck ({@link #earthRing}); all run out, told. */
	private static void shakes(MinecraftServer server, long now) {
		if (SHAKES.isEmpty()) return;
		for (UUID id : List.copyOf(SHAKES.keySet())) {
			Shake shake = SHAKES.get(id);
			if (shake == null) continue; // ended by a death on the way
			LivingEntity user = user(server, id);
			if (user == null || !user.isAlive()) {
				SHAKES.remove(id);
				continue;
			}
			while (shake.rings < EARTHSHAKER_RINGS && now - shake.at >= (long) shake.rings * EARTHSHAKER_EVERY && SHAKES.get(id) == shake) {
				double inner = shake.reach * shake.rings / EARTHSHAKER_RINGS, outer = shake.reach * (shake.rings + 1) / EARTHSHAKER_RINGS;
				shake.struck += earthRing(user, shake.centre, inner, outer, shake.rings);
				shake.rings++;
			}
			if (SHAKES.get(id) != shake) continue; // ended by its user's death on the way
			if (shake.rings >= EARTHSHAKER_RINGS) {
				SHAKES.remove(id);
				off(user, Ability.EARTHSHAKER);
				tell(user, Component.translatable(ModLang.MSG_EARTHSHAKER, shake.struck));
			}
		}
	}

	/** How long (ticks) a Sky-Pillar Strike rises before it turns down onto where one looked: near the top of its throw. */
	static final int PILLAR_APEX = 16;

	/**
	 * Those up on a Sky-Pillar Strike: off the ground, then near the top of the throw ({@link #PILLAR_APEX}; a player's own motion isn't the
	 * server's to read) hurled down at {@link #PILLAR_SLAM} onto where they looked, a trail of light behind; down again, the slam. Let go flown,
	 * swum, never off the ground, or too long under way.
	 */
	private static void pillars(MinecraftServer server, long now) {
		if (PILLARS.isEmpty()) return;
		for (UUID id : List.copyOf(PILLARS.keySet())) {
			Pillar pillar = PILLARS.get(id);
			if (pillar == null) continue; // ended by a death on the way
			LivingEntity user = user(server, id);
			if (user == null || !user.isAlive()) {
				PILLARS.remove(id);
				continue;
			}
			long since = now - pillar.at;
			if (!pillar.aloft && !user.onGround()) pillar.aloft = true;
			ServerLevel level = level(user);
			if (pillar.aloft && !pillar.slamming && since >= PILLAR_APEX) {
				pillar.slamming = true;
				Vec3 down = pillar.target.subtract(user.position());
				down = down.lengthSqr() < 1.0e-4 ? new Vec3(0, -1, 0) : down.normalize();
				user.setDeltaMovement(down.scale(PILLAR_SLAM));
				user.hurtMarked = true;
				user.fallDistance = 0;
				level.playSound(null, user.blockPosition(), SoundEvents.PHANTOM_SWOOP, user.getSoundSource(), 1.4f, 0.5f);
			}
			if (pillar.slamming) {
				Vec3 tail = user.position().add(0, 1.2, 0);
				level.sendParticles(ParticleTypes.END_ROD, tail.x, tail.y, tail.z, 4, 0.2, 0.3, 0.2, 0.01);
				level.sendParticles(GOLD, tail.x, tail.y, tail.z, 6, 0.25, 0.4, 0.25, 0);
			}
			if (pillar.aloft && user.onGround()) {
				PILLARS.remove(id);
				pillarSlam(user);
			} else if (flying(user) || pillar.aloft && user.isInWater() || since > PILLAR_MOST || !pillar.aloft && since > LEAP_TAKEOFF) {
				PILLARS.remove(id);
				user.fallDistance = 0;
				off(user, Ability.SKY_PILLAR_STRIKE);
			}
		}
	}

	/** Imperishable Gold run out: its burst ({@link #goldBurst}); the one it held gone from the world, only its hold against knockback taken off. */
	private static void imperishables(MinecraftServer server, long now) {
		if (IMPERISHABLE.isEmpty()) return;
		for (UUID id : List.copyOf(IMPERISHABLE.keySet())) {
			Long until = IMPERISHABLE.get(id);
			if (until == null || now < until) continue; // (ended by a death on the way, or still holding)
			IMPERISHABLE.remove(id);
			Float turned = TURNED.remove(id);
			LivingEntity user = user(server, id);
			if (user == null) continue;
			if (user.isAlive()) goldBurst(user, turned == null ? 0 : turned);
			else unmoved(user, IMPERISHABLE_ID, "Imperishable Gold", false);
		}
	}

	/** Myriad Fists under way: each blow as it comes due ({@link #flurryBlow}), the last hurling; all fallen, done. */
	private static void myriads(MinecraftServer server, long now) {
		if (MYRIADS.isEmpty()) return;
		for (UUID id : List.copyOf(MYRIADS.keySet())) {
			Myriad myriad = MYRIADS.get(id);
			if (myriad == null) continue; // ended by a death on the way
			LivingEntity user = user(server, id);
			if (user == null || !user.isAlive()) {
				MYRIADS.remove(id);
				continue;
			}
			while (myriad.blows < MYRIAD_BLOWS && now - myriad.at >= (long) myriad.blows * MYRIAD_EVERY && MYRIADS.get(id) == myriad) {
				flurryBlow(user, myriad.blows, myriad.blows == MYRIAD_BLOWS - 1);
				myriad.blows++;
			}
			if (MYRIADS.get(id) != myriad) continue; // ended by its user's death on the way
			if (myriad.blows >= MYRIAD_BLOWS) {
				MYRIADS.remove(id);
				off(user, Ability.MYRIAD_FISTS);
			}
		}
	}

	/**
	 * Dharma Bodies towering: each strides after its user to stand behind them (there at once from far off) and turns to where they look (held
	 * toward a fist's mark while it strikes); its fists land and its stomps come down as they come due; gone when its time is out, or its user is.
	 */
	private static void dharmas(MinecraftServer server, long now) {
		if (DHARMAS.isEmpty()) return;
		for (UUID id : List.copyOf(DHARMAS.keySet())) {
			Dharma d = DHARMAS.get(id);
			if (d == null) continue; // ended by a death on the way
			LivingEntity user = user(server, id);
			if (user == null || !user.isAlive() || d.body.isRemoved() || now >= d.until || user.level() != d.body.level()) {
				endDharma(id);
				if (user != null) off(user, Ability.DHARMA_BODY);
				continue;
			}
			Vec3 spot = dharmaSpot(user), at = d.body.position(), step = spot.subtract(at);
			double length = step.length();
			Vec3 next = length > DHARMA_SNAP || length <= DHARMA_STRIDE ? spot : at.add(step.scale(DHARMA_STRIDE / length));
			float yaw = d.body.getYRot();
			if (now >= d.holdYawUntil) yaw += (float) net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(user.getYRot() - yaw), -DHARMA_TURN, DHARMA_TURN);
			d.body.place(next, yaw, 0);
			// Each blow's hand among those sent before it lands: its user fallen to what came back off it (thorns, say), the Body is ended
			// (#end) with every hand it sent, this one too.
			for (Iterator<Fist> f = d.fists.iterator(); f.hasNext() && DHARMAS.get(id) == d; ) {
				Fist fist = f.next();
				if (now < fist.lands()) continue;
				f.remove();
				d.sent.add(fist.hand());
				dharmaFistLands(user, fist);
			}
			for (Iterator<Long> s = d.stomps.iterator(); s.hasNext() && DHARMAS.get(id) == d; ) {
				if (now < s.next()) continue;
				s.remove();
				dharmaStompLands(user, d);
			}
			if (DHARMAS.get(id) != d) continue; // ended by its user's death on the way
			d.sent.removeIf(hand -> {
				if (hand.tickCount < hand.life()) return false;
				hand.dissolve();
				return true;
			});
		}
	}

	/**
	 * Once a second: Blood Frenzy burns its vigor (going out when it runs dry, or once it is no longer one's), else rest refills it; and an act
	 * left "on" with nothing under way (a logout, a crash: their times are the server's) shown off again.
	 */
	static void second(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		UUID id = p.getUUID();
		for (Ability act : TECHNIQUES) {
			if (act == Ability.BLOOD_FRENZY || act == Ability.CLOUD_TREADING || !c.isAbilityEnabled(act)) continue;
			boolean under = switch (act) {
				case IRON_SKIN -> IRON_SKIN.containsKey(id);
				case GROUND_SPLITTING_LEAP -> LEAPS.containsKey(id);
				case THOUSAND_POUND_FIST -> FIST.containsKey(id);
				case GOLDEN_BODY -> GOLDEN.containsKey(id);
				case DIAMOND_WILL -> DIAMOND_WILL.containsKey(id);
				case COMET_CHARGE -> COMETS.containsKey(id);
				case REBOUNDING_BONES -> REBOUND.containsKey(id);
				case FLESH_REBORN -> REBORN.containsKey(id);
				case EARTHSHAKER -> SHAKES.containsKey(id);
				case SKY_PILLAR_STRIKE -> PILLARS.containsKey(id);
				case IMPERISHABLE_GOLD -> IMPERISHABLE.containsKey(id);
				case MYRIAD_FISTS -> MYRIADS.containsKey(id);
				case DHARMA_BODY -> DHARMAS.containsKey(id);
				default -> false; // the stomp, the shattering fist and the roar are done at once
			};
			if (!under) off(p, act);
		}
		// Cloud-Treading Steps burns its vigor only in the air on it (QiFlight#treading), and goes out when it runs dry there (one falls).
		boolean aloft = QiFlight.treading(p);
		if (c.isAbilityEnabled(Ability.CLOUD_TREADING)) {
			if (!p.isAlive() || !Ability.CLOUD_TREADING.isUnlocked(c)) {
				treadOff(p, c, false);
			} else if (aloft) {
				c.body().setVigor(Math.max(0, c.body().vigor() - TREAD_PER_SECOND));
				p.causeFoodExhaustion((float) (TREAD_PER_SECOND * HUNGER));
				CultivationManager.markDirty(p.server);
				if (c.body().vigor() < TREAD_PER_SECOND) treadOff(p, c, true);
			}
		}
		if (c.isAbilityEnabled(Ability.BLOOD_FRENZY)) {
			if (!p.isAlive() || !Ability.BLOOD_FRENZY.isUnlocked(c)) {
				frenzyOff(p, c, false);
			} else if (c.body().vigor() < FRENZY_PER_SECOND) {
				c.body().setVigor(0);
				frenzyOff(p, c, true);
			} else {
				c.body().setVigor(c.body().vigor() - FRENZY_PER_SECOND);
				p.causeFoodExhaustion((float) (FRENZY_PER_SECOND * HUNGER));
				ring(p, ParticleTypes.CRIMSON_SPORE, 6, 1.0, 0.8, 0.02);
				if (p.server.getTickCount() % 60 == 0) p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, p.getSoundSource(), 0.5f, 1.0f);
				CultivationManager.markDirty(p.server);
			}
		} else if (BodyPath.rank(c) >= 0 && p.isAlive() && c.body().vigor() < maxVigor(c) && resting(p) && !aloft) { // no rest striding on the air
			c.body().setVigor(c.body().vigor() + maxVigor(c) * REST_SHARE * (MeditationManager.isMeditating(id) ? MEDITATING : 1));
			CultivationManager.markDirty(p.server);
		}
		if (BodyPath.rank(c) >= 0) tell(p, c);
	}

	// --- Ends ---

	/** {@code p} has died or is leaving: what holds ends and Blood Frenzy goes out (not lit again by coming back); the cooldowns outlast it. */
	public static void end(ServerPlayer p) {
		boolean diamond = endHolds(p.getUUID());
		unmoved(p, IRON_SKIN_ID, "Iron Skin", false);
		unmoved(p, DIAMOND_ID, "Diamond Will", false);
		unmoved(p, IMPERISHABLE_ID, "Imperishable Gold", false);
		frenzySpeed(p, false);
		PlayerCultivation c = CultivationManager.get(p);
		for (Ability technique : TECHNIQUES) if (c.isAbilityEnabled(technique)) c.setAbilityEnabled(technique, false);
		TOLD.remove(p.getUUID());
		if (diamond) RealmSuppressSystem.refreshNow();
	}

	/** Everything that holds or is under way of {@code id}'s ended (its cooldowns kept). @return whether Diamond Will was one */
	private static boolean endHolds(UUID id) {
		boolean diamond = DIAMOND_WILL.remove(id) != null;
		IRON_SKIN.remove(id);
		FIST.remove(id);
		GOLDEN.remove(id);
		REBOUND.remove(id);
		REBORN.remove(id);
		FRENZY.remove(id);
		LEAPS.remove(id);
		COMETS.remove(id);
		SHAKES.remove(id);
		PILLARS.remove(id);
		IMPERISHABLE.remove(id);
		TURNED.remove(id);
		MYRIADS.remove(id);
		endDharma(id);
		return diamond;
	}

	/** Everything of {@code id}'s (the game tests). */
	public static void forget(UUID id) {
		READY.remove(id);
		endHolds(id);
		TOLD.remove(id);
		NPCS.remove(id);
	}

	/** The server stopped: game times mean nothing in the next world opened. */
	public static void clear() {
		READY.clear();
		IRON_SKIN.clear();
		FIST.clear();
		GOLDEN.clear();
		DIAMOND_WILL.clear();
		REBOUND.clear();
		REBORN.clear();
		FRENZY.clear();
		LEAPS.clear();
		COMETS.clear();
		SHAKES.clear();
		PILLARS.clear();
		IMPERISHABLE.clear();
		TURNED.clear();
		MYRIADS.clear();
		DHARMAS.clear(); // their bodies went with the server's worlds (never saved)
		HURLS.clear();
		TOLD.clear();
		NPCS.clear();
		rebounding = false;
	}

	/** For the game tests: every cooldown of {@code id}'s passed. */
	static void readyAgain(UUID id) {
		READY.remove(id);
	}

	private BodyTechniques() {}
}
