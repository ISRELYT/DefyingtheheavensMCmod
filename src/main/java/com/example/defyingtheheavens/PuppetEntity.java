package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A puppet (Puppetry; the user's "begin phase 1", 2026-11-17): a frame (PuppetFrameItem) with a demon core for its heart and its fuel, and
 * the circuits its puppeteer painted (CircuitBoard), sealed at a Puppet Workbench (PuppetBoards). The humanoid fights and guards, its art a
 * beam from its mouth; the bee carries between two chests and runs messages, its art from its stinger. It never cultivates, never tires and
 * never mends by itself.
 * <ul>
 *   <li><b>Its might</b>: a cultivator's of its realm (its core's, within its frame's: Puppetry#puppetRank) times {@link Puppetry#MIGHT};
 *   its blows and its art by its Flow, its pace, aim and dodging by its Harmony (Puppetry#flowFactor, #harmonyFactor).</li>
 *   <li><b>Its veins</b> (the deeper circuits, phase A: CircuitBoard): each part the stronger the more veins feed it (Puppetry's steps):
 *   the arms its blows, the legs its pace, the wings its flight, a bee's legs its load, the head its sight and how quickly it takes up a
 *   foe, the art's node its art (and a bee's sting), the heart how far it hears its puppeteer and how hard it is to seize.</li>
 *   <li><b>Its nodes</b>: a limb nothing feeds hangs dead (a dead leg or wing slows it, a dead arm weakens its blows); with its head dead it
 *   sees only what is close; with its art's node dead (or a Mortal frame) it has no art; with its heart of command dead it stands
 *   <i>inert</i>, deaf to its puppeteer, until mended.</li>
 *   <li><b>Its art</b> (from a Spirit frame): its core's element as a beam, charges that come back by themselves (two with a gathering
 *   knot). Every blow and every art heats its lines (an art the more), and it sheds the heat as it goes; past its Stability's bound
 *   (Puppetry#overheatAfter) it overheats, stalled and its art locked a while. Clarity: how slow it is to take up a foe, how often its art
 *   goes astray, how far it hears its puppeteer.</li>
 *   <li><b>Its orders</b> ({@link Order}, the Command Seal): follow, guard a place, hold where it stands, work (a bee: carry from one marked
 *   chest to another; Heaven grade and above go between them in a step), or carry a message (PuppetCouriers).</li>
 *   <li><b>Its arrays</b> (the deeper circuits, phase C: CircuitBoard.Array): an Eye of Discernment sees the unseen, takes up a foe at once
 *   and sees farther; a Guard Coil turns a blow aside every so often; a Gale Stair dashes it at a foe out of reach; a Gathering Spiral
 *   brings its art's charges back twice as fast; an Iron Lock keeps any seal from seizing it and halves a freezing. A Twin Seal halves the
 *   coil's or the stair's wait.</li>
 *   <li><b>Its soul</b> (phase 2a: PuppetSouls): a beast's in its Soul-Binding Ring makes it sharper, quicker and swifter; a cultivator's
 *   gives it the cultivator's name, a charge more of its art and the cultivator's own arts. Copied while its puppeteer is with it, whole
 *   after an hour, the real soul gone on.</li>
 *   <li><b>Its circuits cut</b> ({@link #cut}): each fifth of its health lost cuts a line of its board, and every blow of Metal qi cuts one
 *   more (and strikes half again as hard); its owner paints them again with the brush (PuppetBoards). Wrecked, it leaves its core and some of
 *   its body's matter; mended with that matter, a quarter of its health a piece.</li>
 * </ul>
 * Its lines glow on its body for anyone with Qi Sense on (the client's PuppetCircuitLayer).
 */
public class PuppetEntity extends PathfinderMob implements GeoEntity, OwnableEntity {
	/** What its puppeteer has told it. */
	public enum Order {
		FOLLOW("follow"), GUARD("guard"), HOLD("hold"), WORK("work");

		private final String id;

		Order(String id) {
			this.id = id;
		}

		public String getId() { return id; }

		public Component getDisplayName() { return Component.translatable(ModLang.puppetOrder(id)); }

		public static Order byIndex(int index) {
			return values()[Math.max(0, Math.min(values().length - 1, index))];
		}
	}

	/** Its art's harm against its blow's. */
	public static final double ART_POWER = 2.5;
	/** How far its art reaches. */
	public static final double ART_RANGE = 16;
	/** Ticks a charge of its art takes to come back; heat a point of it sheds in. */
	public static final int RECHARGE = 200, COOL = Puppetry.COOL_TICKS;
	/** Overheated: stalled this long, its art locked this long. */
	public static final int STALL = 60, ART_LOCK = 400;
	/** How far a guard keeps from its post, and how far round it it watches. */
	public static final double GUARD_KEEP = 8, GUARD_WATCH = 14;
	/** A bee's load (stacks) by its frame's grade. */
	public static final int[] CARGO = {1, 2, 4, 9, 27};
	/** Ticks a cultivator's puppet outlasts its maker's absence before it falls apart (NpcPuppets). */
	public static final int OWNER_GONE = 20 * 60;
	/** A frame's tag: a Spring of the Ancients was poured into its puppet (phase 3), kept through its taking apart. */
	public static final String SPRING_FED = "SpringFed";

	private static final EntityDataAccessor<Byte> GRADE = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> ELEMENT = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<CompoundTag> CIRCUIT = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.COMPOUND_TAG);
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	/** Its soul as its watchers know it: none (-1), a beast's (0), a cultivator's (1); and the soul's name (phase 2a). */
	private static final EntityDataAccessor<Byte> SOUL_KIND = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<String> SOUL_NAME = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.STRING);
	/** The bent of the mind its soul has grown of its own (phase 2c: PuppetSoul.Trait), -1 for none. */
	private static final EntityDataAccessor<Byte> SOUL_MIND = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.BYTE);
	/** The player whose look it wears (phase 2d: their soul copied whole, the copy its own: "wearing your name and look"); none else. */
	private static final EntityDataAccessor<Optional<UUID>> SOUL_LOOK = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	/** Who its body was, if it is made of one (phase 2b): its likeness for its watchers (empty: a frame of matter). */
	private static final EntityDataAccessor<CompoundTag> BODY = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.COMPOUND_TAG);
	private static final byte INERT = 1, FROZEN = 2, OVERHEATED = 4;

	private static final UUID BLOW_ID = UUID.fromString("5d0f3b1e-31a6-4f6c-9a39-7c1b5e2f9a01");
	private static final UUID PACE_ID = UUID.fromString("5d0f3b1e-31a6-4f6c-9a39-7c1b5e2f9a02");
	private static final UUID WING_ID = UUID.fromString("5d0f3b1e-31a6-4f6c-9a39-7c1b5e2f9a03");
	private static final UUID EARTH_HEALTH_ID = UUID.fromString("5d0f3b1e-31a6-4f6c-9a39-7c1b5e2f9a04");
	private static final UUID EARTH_STEADY_ID = UUID.fromString("5d0f3b1e-31a6-4f6c-9a39-7c1b5e2f9a05");
	private static final UUID METAL_BITE_ID = UUID.fromString("5d0f3b1e-31a6-4f6c-9a39-7c1b5e2f9a06");
	private static final UUID MIND_HEALTH_ID = UUID.fromString("5d0f3b1e-31a6-4f6c-9a39-7c1b5e2f9a07");
	private static final UUID LINK_ID = UUID.fromString("5d0f3b1e-31a6-4f6c-9a39-7c1b5e2f9a08");
	private static final UUID HELD_DOWN_HEALTH_ID = UUID.fromString("5d0f3b1e-31a6-4f6c-9a39-7c1b5e2f9a09");
	private static final UUID HELD_DOWN_HARM_ID = UUID.fromString("5d0f3b1e-31a6-4f6c-9a39-7c1b5e2f9a0a");

	// What it is.
	private String ownerName = "";
	private boolean ownerIsPlayer = true;
	private ArtifactGrade grade = ArtifactGrade.MORTAL;
	private Artifacts.Quality quality = Artifacts.Quality.NORMAL;
	private long seed;
	private byte[] strokes = new byte[0];
	private final IntArrayList cuts = new IntArrayList();
	private ItemStack core = ItemStack.EMPTY;
	// What it was told.
	private Order order = Order.FOLLOW;
	private @Nullable BlockPos post, carryFrom, carryTo;
	private final SimpleContainer cargo = new SimpleContainer(27);
	// How it stands.
	private int heat, charges, rechargeTimer, coolTimer, stallTicks, artLock, frozenTicks, attackCooldown, reaction, carryWait, ownerGone;
	private float lostSinceCut;
	private @Nullable LivingEntity lastFoe;
	/** A messenger bee lingering by its addressee: its errand (PuppetCouriers), and the ticks before it goes back. */
	private @Nullable CompoundTag courier;
	private int courierLeave;
	// What its board makes of it (rebuilt from the above): its blows, its art, its heart's reach, how slow its head is, a bee's load.
	private @Nullable CircuitBoard board;
	private @Nullable CircuitBoard.Scores scores;
	private int rank, heartVeins = 1;
	private double blowsFactor = 1, artFactor = 1, reachFactor = 1, reactionFactor = 1, cargoFactor = 1;
	// Its phases (the deeper circuits, phase B): what its blows and its art carry (bits, CircuitBoard.Phase#bit), and its body's veins of
	// each phase (legs, wings, head and heart).
	private int blowPhases, artPhases;
	private final int[] bodyPhaseVeins = new int[CircuitBoard.Phase.values().length];
	/** Its cut Wood lines growing back: the cell, the stroke it held, the tick it grows back at. */
	private final List<int[]> regrowing = new ArrayList<>();
	// Its arrays (the deeper circuits, phase C) that work, a Twin Seal's halving the coil's or the stair's wait; the coil's and the stair's
	// waits, and a dash under way (its step a tick).
	private boolean eye, guard, guardTwin, stair, stairTwin, spiral, locked;
	private int guardWait, dashWait, dashTicks;
	private Vec3 dashStep = Vec3.ZERO;
	/** The soul in its Soul-Binding Ring (phase 2a: PuppetSouls); null, hollow. A cultivator's arts' wait in a fight. */
	private @Nullable PuppetSoul soul;
	private int soulArtWait;
	/** The cultivator whose body it is, and its board's meridians (phase 2b: PuppetBodies); null for a frame of matter. */
	private PuppetBodies.@Nullable Dead dead;
	private boolean @Nullable [] meridians;
	// Its ranks of command (phase 2c): the grade of the seal it is bound to (null: as its puppeteer's kind has it, sealGrade()), the holder
	// of a Sub-Seal it answers (null: none) and their name, and its Sub-Seals' mark (revoking moves it on, voiding every one written before).
	private @Nullable ArtifactGrade sealGrade;
	private @Nullable UUID commander;
	private String commanderName = "";
	private int subNonce;
	/** A General (phase 2c): the puppets in its ranks, each as it was when it went in. */
	private boolean general;
	private final List<CompoundTag> ranks = new ArrayList<>();
	/** Where it stood a second ago: the ground a growing mind covers (phase 2c). */
	private @Nullable Vec3 lastSpot;
	/** A player's soul in it (phase 2d: PuppetPlayers): released to play it by its puppeteer; struggling from within, and how far into the next cut. */
	private boolean released, struggling;
	private int struggleTicks;
	/** The stroke each of its cut cells held before it was cut (phase 3: self-repair mends it so; its design is as it was drawn). */
	private final it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap cutWas = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();
	// The linked army (phase 3): linked by its heart's link knot; its linked fellows in reach a second ago (-1: not yet reckoned); its place
	// in a ring round its foe (null: none); held down where qi is suppressed (-1: not yet reckoned); a spring poured into it and the ticks to
	// its next mending; a fuse lit (-1: none); its puppeteer heard through its fellows till this tick.
	private boolean linked, springFed, relayed;
	private int linkFellows = -1, suppressedNow = -1, mendTimer, fuse = -1;
	private long relayedUntil;
	private @Nullable Vec3 formationSpot;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public PuppetEntity(EntityType<? extends PuppetEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		if (isBee()) {
			moveControl = new FlyingMoveControl(this, 20, true);
			setNoGravity(true);
		}
	}

	/** A player's puppet stays where it is left; a cultivator's comes and goes with its maker (NpcPuppets). */
	@Override
	public boolean requiresCustomPersistence() {
		return ownerIsPlayer || super.requiresCustomPersistence();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.ATTACK_DAMAGE, 2.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.5)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		if (!isBee()) return super.createNavigation(level);
		FlyingPathNavigation flying = new FlyingPathNavigation(this, level);
		flying.setCanOpenDoors(false);
		flying.setCanFloat(true);
		flying.setCanPassDoors(true);
		return flying;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(GRADE, (byte) 0);
		entityData.define(ELEMENT, (byte) -1);
		entityData.define(FLAGS, (byte) 0);
		entityData.define(CIRCUIT, new CompoundTag());
		entityData.define(OWNER, Optional.empty());
		entityData.define(SOUL_KIND, (byte) -1);
		entityData.define(SOUL_NAME, "");
		entityData.define(SOUL_MIND, (byte) -1);
		entityData.define(SOUL_LOOK, Optional.empty());
		entityData.define(BODY, new CompoundTag());
	}

	// --- Its body of flesh (phase 2b: PuppetBodies) ---

	/** {@code dead}'s body this puppet is made of, its board on {@code meridians}: set before its board is weighed (seal, load). */
	public void setBody(PuppetBodies.@Nullable Dead dead, boolean @Nullable [] meridians) {
		this.dead = dead;
		this.meridians = meridians == null ? null : meridians.clone();
		entityData.set(BODY, dead == null ? new CompoundTag() : dead.save());
	}

	/** Who its body was (the server's); null for a frame of matter. */
	public PuppetBodies.@Nullable Dead dead() {
		return dead;
	}

	/** Its body's meridians (the server's); null for a frame of matter. */
	public boolean @Nullable [] meridians() {
		return meridians == null ? null : meridians.clone();
	}

	/** Made of a cultivator's body (the client's too). */
	public boolean isBody() {
		return !entityData.get(BODY).isEmpty();
	}

	/** Who its body was as its watchers know it (its likeness: PuppetRenderer); null for a frame of matter. */
	public PuppetBodies.@Nullable Dead bodyLook() {
		CompoundTag tag = entityData.get(BODY);
		return tag.isEmpty() ? null : PuppetBodies.Dead.load(tag);
	}

	// --- Its soul (phase 2a: PuppetSouls) ---

	/** The soul in its ring (the server's); null, hollow. */
	public @Nullable PuppetSoul soul() {
		return soul;
	}

	/** {@code soul} bound into its ring (null: the ring emptied), its strength weighed again with it. */
	public void bindSoul(@Nullable PuppetSoul soul) {
		this.soul = soul;
		soulArtWait = PuppetSouls.ART_EVERY / 2;
		showSoul();
		if (!level().isClientSide && board != null) {
			applyStats();
			if (charges > maxCharges()) charges = maxCharges();
			remember();
		}
	}

	/** Its soul's kind and name for its watchers (the name a grown mind took), and its mind's bent. */
	private void showSoul() {
		entityData.set(SOUL_KIND, (byte) (soul == null ? -1 : soul.kind.ordinal()));
		entityData.set(SOUL_NAME, soul == null ? "" : soul.goesBy());
		entityData.set(SOUL_MIND, (byte) (soul == null || soul.trait() == null ? -1 : soul.trait().ordinal()));
		entityData.set(SOUL_LOOK, Optional.ofNullable(soul != null && soul.kind == PuppetSoul.Kind.PLAYER && soul.whole() ? soul.player() : null));
	}

	/** The player whose look it wears (the client's too): its copy of their soul whole; null for none. */
	public @Nullable UUID soulLook() {
		return entityData.get(SOUL_LOOK).orElse(null);
	}

	/** Its soul changed in itself (its copy come whole): shown anew to its watchers. */
	public void soulChanged() {
		showSoul();
	}

	/** The bent of the mind its soul has grown of its own (the client's too); null for none. */
	public PuppetSoul.@Nullable Trait mind() {
		return PuppetSoul.Trait.byIndex(entityData.get(SOUL_MIND));
	}

	/** The bent of its grown mind, as the server weighs it. */
	private boolean minded(PuppetSoul.Trait trait) {
		return soul != null && soul.trait() == trait;
	}

	/** Its soul's kind as its watchers know it (the client's too); null, hollow. */
	public PuppetSoul.@Nullable Kind soulKind() {
		int k = entityData.get(SOUL_KIND);
		return k < 0 || k >= PuppetSoul.Kind.values().length ? null : PuppetSoul.Kind.values()[k];
	}

	/** Its soul's name (the client's too); empty, hollow. */
	public String soulName() {
		return entityData.get(SOUL_NAME);
	}

	/** The charges its art holds at most: its board's, and one more with a cultivator's soul (it uses its art the more cannily). */
	public int maxCharges() {
		if (scores == null) rebuild(false);
		// (A player's soul is a mind as canny: phase 2d.)
		boolean mind = soul != null && (soul.kind == PuppetSoul.Kind.CULTIVATOR || soul.kind == PuppetSoul.Kind.PLAYER);
		return scores.artCharges() + (mind && grade.ordinal() >= ArtifactGrade.SPIRIT.ordinal() ? 1 : 0);
	}

	/**
	 * Under its puppeteer's control (its soul's copy grows): its puppeteer in the world with it, its heart fed, not frozen; a player's soul
	 * held in it, watching, not playing it (phase 2d: PuppetPlayers#heldIn).
	 */
	public boolean underControl() {
		if (!(getOwner() instanceof ServerPlayer owner) || owner.level() != level() || isInert() || isFrozen()) return false;
		return soul == null || soul.kind != PuppetSoul.Kind.PLAYER || PuppetPlayers.heldIn(this);
	}

	// --- Making one ---

	/** Seals the frame and core on a bench into a puppet of {@code owner}'s at {@code at} (PuppetBoards#seal). */
	public static PuppetEntity seal(ServerLevel level, ItemStack frame, ItemStack core, ServerPlayer owner, Vec3 at) {
		PuppetFrameItem item = (PuppetFrameItem) frame.getItem();
		PuppetEntity puppet = new PuppetEntity(item.blueprint() == PuppetBlueprint.BEE ? ModEntities.PUPPET_BEE : ModEntities.PUPPET_HUMANOID, level);
		byte[] drawn = PuppetFrameItem.strokes(frame);
		// A body of flesh (phase 2b): who it was and its meridians, before its board is weighed.
		puppet.setBody(PuppetBodies.deadOf(frame), PuppetBodies.meridians(frame));
		puppet.setup(item.grade(), Artifacts.quality(frame), PuppetFrameItem.seed(frame), drawn == null ? new byte[0] : drawn, core.copyWithCount(1));
		puppet.setOwner(owner.getUUID(), owner.getGameProfile().getName(), true);
		// Bound at the best Command Seal its puppeteer carries as it stands up (phase 2c), Mortal with none.
		puppet.sealGrade = CommandSealItem.best(owner);
		// A spring poured into it once, kept in its frame (phase 3).
		puppet.springFed = frame.getTag() != null && frame.getTag().getBoolean(SPRING_FED);
		puppet.moveTo(at.x, at.y, at.z, owner.getYRot() + 180, 0);
		puppet.findRoom();
		puppet.setHealth(puppet.getMaxHealth());
		level.addFreshEntity(puppet);
		puppet.remember();
		return puppet;
	}

	/** Sets up what it is made of, and weighs its board. */
	public void setup(ArtifactGrade grade, Artifacts.Quality quality, long seed, byte[] strokes, ItemStack core) {
		this.grade = grade;
		this.quality = quality;
		this.seed = seed;
		this.strokes = strokes.clone();
		this.core = core;
		entityData.set(GRADE, (byte) grade.ordinal());
		// Its art's element: its core's, or a body's own (the dead's) where they had one.
		QiElement element = dead != null && dead.element() != null ? dead.element() : DemonCoreItem.element(core);
		entityData.set(ELEMENT, (byte) (element == null ? -1 : element.ordinal()));
		rebuild(false);
		charges = scores == null ? 0 : maxCharges();
	}

	public void setOwner(@Nullable UUID owner, String name, boolean player) {
		entityData.set(OWNER, Optional.ofNullable(owner));
		ownerName = name;
		ownerIsPlayer = player;
	}

	/** Off the bench into the open: above it if there is room there, else beside it. */
	private void findRoom() {
		if (level().noCollision(this)) return;
		BlockPos base = blockPosition();
		for (BlockPos at : new BlockPos[] {base.north(), base.south(), base.east(), base.west(), base.below().north(), base.below().south(),
				base.below().east(), base.below().west()}) {
			moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, getYRot(), 0);
			if (level().noCollision(this)) return;
		}
		moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, getYRot(), 0);
	}

	// --- What it is ---

	public boolean isBee() {
		return getType() == ModEntities.PUPPET_BEE;
	}

	public PuppetBlueprint blueprint() {
		return isBee() ? PuppetBlueprint.BEE : PuppetBlueprint.HUMANOID;
	}

	/** Its frame's grade (the client's too). */
	public ArtifactGrade frameGrade() {
		return level().isClientSide ? ArtifactGrade.byIndex(entityData.get(GRADE)) : grade;
	}

	public Artifacts.Quality frameQuality() { return quality; }
	public long seed() { return seed; }
	public ItemStack core() { return core; }
	public Order order() { return order; }
	public @Nullable BlockPos post() { return post; }
	public @Nullable BlockPos carryFrom() { return carryFrom; }
	public @Nullable BlockPos carryTo() { return carryTo; }

	/** The element its core gives its art (the client's too); null for none. */
	public @Nullable QiElement element() {
		int e = entityData.get(ELEMENT);
		return e < 0 || e >= QiElement.values().length ? null : QiElement.values()[e];
	}

	/** Its realm and stage as one rank (PlayerCultivation#rank). */
	public int rank() { return rank; }

	public boolean isInert() { return (entityData.get(FLAGS) & INERT) != 0; }
	public boolean isFrozen() { return (entityData.get(FLAGS) & FROZEN) != 0; }
	public boolean isOverheated() { return (entityData.get(FLAGS) & OVERHEATED) != 0; }

	/** Still: inert, frozen by a talisman or stalled by its heat. */
	public boolean isStill() { return entityData.get(FLAGS) != 0; }

	private void flag(byte flag, boolean on) {
		byte flags = entityData.get(FLAGS);
		byte next = (byte) (on ? flags | flag : flags & ~flag);
		if (next != flags) entityData.set(FLAGS, next);
	}

	/** Its board's drawing as its client copy knows it (for its lines on its body, with Qi Sense on). */
	public byte[] shownStrokes() {
		CompoundTag tag = entityData.get(CIRCUIT);
		return tag.getByteArray("S");
	}

	@Override
	public @Nullable UUID getOwnerUUID() {
		return entityData.get(OWNER).orElse(null);
	}

	/** Its puppeteer, while in this world: a player, or a cultivator (an NPC puppeteer's). */
	@Override
	public @Nullable LivingEntity getOwner() {
		UUID owner = getOwnerUUID();
		if (owner == null) return null;
		if (level() instanceof ServerLevel level) return level.getEntity(owner) instanceof LivingEntity living ? living : null;
		Player player = level().getPlayerByUUID(owner);
		return player;
	}

	public String ownerName() { return ownerName; }
	public boolean ownedByPlayer() { return ownerIsPlayer; }

	public boolean isOwnedBy(LivingEntity entity) {
		return entity.getUUID().equals(getOwnerUUID());
	}

	/** "Spirit Humanoid Puppet" (a name tag's name over it, if it has one); with a cultivator's soul in its ring, the cultivator's name. */
	@Override
	protected Component getTypeName() {
		// A mind grown of its own goes by the name it took (phase 2c), a cultivator's soul and a player's (phase 2d) by their own.
		if ((mind() != null || soulKind() == PuppetSoul.Kind.CULTIVATOR || soulKind() == PuppetSoul.Kind.PLAYER) && !soulName().isEmpty()) {
			return Component.translatable(ModLang.PUPPET_SOUL_NAME, soulName());
		}
		// A body of flesh (phase 2b): the dead's name.
		PuppetBodies.Dead look = bodyLook();
		if (look != null) return Component.translatable(ModLang.PUPPET_SOUL_NAME, look.name());
		return Component.translatable(ModLang.PUPPET_NAME, frameGrade().getDisplayName(), super.getTypeName());
	}

	// --- Its board ---

	/** Its board as it stands, cuts and all. */
	public CircuitBoard board() {
		if (board == null) rebuild(false);
		return board.copy();
	}

	public CircuitBoard.Scores scores() {
		if (scores == null) rebuild(false);
		return scores;
	}

	public int[] cuts() {
		return cuts.toIntArray();
	}

	/** Its owner painted {@code cell} again: whether it was a cut (taken off the cuts). */
	public boolean mend(int cell) {
		cutWas.remove(cell);
		return cuts.rem(cell);
	}

	/**
	 * Its drawing as it was drawn (phase 3): its board's strokes with every cut cell holding what it held before it was cut (a plain line
	 * where that wasn't kept). What its design is reckoned by, and what a diagram taken from its wreck shows.
	 */
	public byte[] designStrokes() {
		byte[] out = Puppetry.strokes(board());
		for (int k = 0; k < cuts.size(); k++) {
			int cell = cuts.getInt(k);
			if (cell >= 0 && cell < out.length && out[cell] == 0) out[cell] = (byte) cutWas.getOrDefault(cell, CircuitBoard.Stroke.LINE.ordinal());
		}
		return out;
	}

	/** Its design (phase 3: Puppetry#design): its make, grade and drawing as drawn. Every puppet built to one diagram bears the same. */
	public long design() {
		return Puppetry.design(blueprint(), grade, designStrokes());
	}

	/** Its board painted (PuppetBoards): kept, weighed again, and what it makes of it put on. */
	public void boardChanged(CircuitBoard painted) {
		strokes = Puppetry.strokes(painted);
		rebuild(true);
	}

	/** Its cut Wood lines whose time has come grow back (a cell painted over since is left as it is), and off the cuts. */
	private void regrow() {
		CircuitBoard b = board();
		boolean grew = false;
		CircuitBoard.Stroke[] all = CircuitBoard.Stroke.values();
		for (java.util.Iterator<int[]> it = regrowing.iterator(); it.hasNext(); ) {
			int[] r = it.next();
			if (tickCount < r[2] && r[2] - tickCount <= Puppetry.REGROW_TICKS) continue;
			it.remove();
			int x = r[0] % b.size, y = r[0] / b.size;
			if (!b.inside(x, y) || b.stroke(x, y) != CircuitBoard.Stroke.NONE || r[1] <= 0 || r[1] >= all.length) continue;
			b.set(x, y, all[r[1]]);
			cuts.rem(r[0]);
			cutWas.remove(r[0]);
			grew = true;
		}
		if (!grew) return;
		strokes = Puppetry.strokes(b);
		rebuild(true);
		if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(0.6), getZ(), 6, 0.25, 0.3, 0.25, 0.02);
	}

	/** Its board rebuilt from its drawing, weighed, and its strength, pace and state set by it. */
	private void rebuild(boolean tell) {
		board = new CircuitBoard(blueprint(), grade.ordinal(), CircuitBoard.flawsFor(quality.ordinal()), seed);
		board.setMeridians(meridians); // a body's own (phase 2b)
		Puppetry.lay(board, strokes);
		// Its core's qi (and phase): a drawing that wants more than it gives (made before the veins drew qi) feeds its nearest veins first.
		scores = board.score(Puppetry.budget(grade, core), Puppetry.phaseOf(core));
		int stands = Puppetry.puppetRank(grade, core.isEmpty() ? 0 : DemonCoreItem.rank(core));
		rank = stands >= 0 ? stands : PlayerCultivation.rank(Puppetry.lowest(grade), Stage.EARLY);
		// A body of flesh stands at the dead's own rank, whatever its core (phase 2b).
		if (dead != null) rank = Math.max(0, dead.rank());
		CompoundTag shown = new CompoundTag();
		shown.putByteArray("S", Puppetry.strokes(board));
		entityData.set(CIRCUIT, shown);
		if (level().isClientSide) return;
		applyStats();
		boolean wasInert = isInert();
		flag(INERT, !scores.sealFed());
		if (tell && wasInert != isInert() && getOwner() instanceof ServerPlayer owner) {
			owner.displayClientMessage(Component.translatable(isInert() ? ModLang.PUPPET_WENT_INERT : ModLang.PUPPET_HEARS_AGAIN, getDisplayName())
					.withStyle(isInert() ? ChatFormatting.RED : ChatFormatting.AQUA), false);
		}
		if (charges > maxCharges()) charges = maxCharges();
		remember();
	}

	/** Index of its node of {@code kind} named {@code id} ("left_leg"), or -1. */
	private int node(String id) {
		List<PuppetBlueprint.Node> nodes = blueprint().nodes(CircuitBoard.sizeFor(grade.ordinal()));
		for (int n = 0; n < nodes.size(); n++) if (nodes.get(n).id().equals(id)) return n;
		return -1;
	}

	private boolean dead(String id) {
		int n = node(id);
		return n >= 0 && scores != null && scores.dead()[n];
	}

	/** The veins its core feeds node {@code id} ("left_arm"); none for a dead one or none such. */
	private int veins(String id) {
		int n = node(id);
		return n < 0 || scores == null ? 0 : scores.veins()[n];
	}

	/** The mean of {@code steps} over a pair's halves that are fed (1 if neither is). */
	private double pairStep(double[] steps, String left, String right) {
		double sum = 0;
		int fed = 0;
		for (String id : new String[] {left, right}) {
			if (dead(id)) continue;
			sum += Puppetry.step(steps, veins(id));
			fed++;
		}
		return fed == 0 ? 1 : sum / fed;
	}

	/** Its strength as its realm and its board give it: each part by its veins, the whole by its scores. */
	private void applyStats() {
		float before = getMaxHealth(), health = getHealth();
		// A frame's seven tenths of a cultivator of its realm; a body of flesh a cultivator's whole (phase 2b).
		CultivationStats.applyNpc(this, CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank), false, dead != null ? PuppetBodies.MIGHT : Puppetry.MIGHT);
		double flow = Puppetry.flowFactor(scores.flow()), harmony = Puppetry.harmonyFactor(scores.harmony());
		// The art's node draws no qi on a frame with no art (a Mortal one), and so gives nothing for its veins either.
		String art = isBee() ? "stinger" : "mouth";
		artFactor = dead(art) || grade == ArtifactGrade.MORTAL ? 1 : Puppetry.step(Puppetry.ART_STEP, veins(art));
		double blows, pace = harmony, wings = harmony;
		if (isBee()) {
			// Its sting by its stinger's veins; its flight by its wings', its load by its legs'.
			blows = flow * (dead("stinger") ? 0.6 : artFactor);
			wings *= pairStep(Puppetry.WING_STEP, "left_wing", "right_wing");
			if (dead("left_wing")) wings *= 0.5;
			if (dead("right_wing")) wings *= 0.5;
			if (dead("left_wing") && dead("right_wing")) wings = 0.3 * harmony;
			cargoFactor = pairStep(Puppetry.CARGO_STEP, "left_leg", "right_leg");
		} else {
			blows = flow * pairStep(Puppetry.ARM_STEP, "left_arm", "right_arm");
			pace *= pairStep(Puppetry.LEG_STEP, "left_leg", "right_leg");
			if (dead("left_arm")) blows *= 0.6;
			if (dead("right_arm")) blows *= 0.6;
			if (dead("left_leg")) pace *= 0.5;
			if (dead("right_leg")) pace *= 0.5;
			cargoFactor = 1;
		}
		// A grown mind's bent (phase 2c): Fierce blows harder (its blows, not its art), Swift moves and flies quicker.
		if (minded(PuppetSoul.Trait.FIERCE)) blows *= 1 + PuppetSouls.FIERCE_BLOWS;
		if (minded(PuppetSoul.Trait.SWIFT)) {
			pace *= 1 + PuppetSouls.SWIFT_PACE;
			wings *= 1 + PuppetSouls.SWIFT_PACE;
		}
		blowsFactor = blows;
		heartVeins = Math.max(1, veins("heart"));
		reachFactor = dead("heart") ? 1 : Puppetry.step(Puppetry.REACH_STEP, heartVeins);
		reactionFactor = dead("head") ? 2 : Puppetry.step(Puppetry.REACTION_STEP, veins("head"));
		// Its phases: its blows the arms' (a bee's sting its stinger's), its art its art node's, its body the rest's veins of each.
		blowPhases = isBee() ? phasesOf("stinger") : phasesOf("left_arm") | phasesOf("right_arm");
		artPhases = phasesOf(art);
		java.util.Arrays.fill(bodyPhaseVeins, 0);
		for (String id : isBee() ? new String[] {"left_wing", "right_wing", "left_leg", "right_leg", "head", "heart"}
				: new String[] {"left_leg", "right_leg", "head", "heart"}) {
			for (CircuitBoard.Phase phase : CircuitBoard.Phase.of(phasesOf(id))) bodyPhaseVeins[phase.ordinal()] += veins(id);
		}
		if (bodyVeins(CircuitBoard.Phase.FIRE) > 0) pace *= 1 + Puppetry.FIRE_PACE;
		// A beast's soul in its ring: a beast's swiftness (phase 2a).
		boolean beast = soul != null && soul.kind == PuppetSoul.Kind.BEAST;
		if (beast) {
			pace *= 1 + PuppetSouls.BEAST_PACE;
			wings *= 1 + PuppetSouls.BEAST_PACE;
		}
		int earth = Math.min(Puppetry.EARTH_MOST, bodyVeins(CircuitBoard.Phase.EARTH));
		modifier(Attributes.MAX_HEALTH, EARTH_HEALTH_ID, "Puppet circuits' earth", Puppetry.EARTH_HEALTH * earth);
		modifier(Attributes.MAX_HEALTH, MIND_HEALTH_ID, "Puppet's steadfast mind", minded(PuppetSoul.Trait.STEADFAST) ? PuppetSouls.STEADFAST_HEALTH : 0);
		AttributeInstance steady = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (steady != null) {
			steady.removeModifier(EARTH_STEADY_ID);
			if (earth > 0) steady.addPermanentModifier(new AttributeModifier(EARTH_STEADY_ID, "Puppet circuits' earth", Math.min(1.0, Puppetry.EARTH_STEADY * earth),
					AttributeModifier.Operation.ADDITION));
		}
		modifier(Attributes.ATTACK_DAMAGE, BLOW_ID, "Puppet circuits' blows", blows - 1);
		modifier(Attributes.MOVEMENT_SPEED, PACE_ID, "Puppet circuits' pace", pace - 1);
		// A bee flies swifter the higher its frame (and as its circuits let it).
		modifier(Attributes.FLYING_SPEED, WING_ID, "Puppet circuits' wings", wings * (1 + 0.15 * grade.ordinal()) - 1);
		// Its arrays: each works while its part lives, its strokes are reached and its qi is drawn (CircuitBoard's weighing).
		eye = scores.has(CircuitBoard.Array.EYE);
		guard = scores.has(CircuitBoard.Array.GUARD_COIL);
		guardTwin = scores.twin(CircuitBoard.Array.GUARD_COIL);
		stair = scores.has(CircuitBoard.Array.GALE_STAIR);
		stairTwin = scores.twin(CircuitBoard.Array.GALE_STAIR);
		spiral = scores.has(CircuitBoard.Array.GATHERING_SPIRAL);
		locked = scores.has(CircuitBoard.Array.IRON_LOCK);
		// Its link knot (phase 3): one of its puppeteer's linked army while a fed vein of its heart comes in through it.
		linked = scores.linked();
		AttributeInstance sight = getAttribute(Attributes.FOLLOW_RANGE);
		if (sight != null) sight.setBaseValue(dead("head") ? 6 : Puppetry.sight(veins("head")) + (eye ? Puppetry.EYE_SIGHT : 0) + (beast ? PuppetSouls.BEAST_SIGHT : 0)
				+ (minded(PuppetSoul.Trait.WATCHFUL) ? PuppetSouls.WATCHFUL_SIGHT : 0));
		if (before > 0 && getMaxHealth() != before) setHealth(Math.min(getMaxHealth(), health * getMaxHealth() / before));
	}

	/** Whether an array of {@code kind} works on it (and, for the coil and the stair, as a Twin Seal: {@code twin}). */
	public boolean hasArray(CircuitBoard.Array kind, boolean twin) {
		if (scores == null) rebuild(false);
		return switch (kind) {
			case EYE -> eye;
			case GUARD_COIL -> guard && (!twin || guardTwin);
			case GALE_STAIR -> stair && (!twin || stairTwin);
			case GATHERING_SPIRAL -> spiral;
			case IRON_LOCK -> locked;
		};
	}

	/** Its Iron Lock works: no Command Seal can seize it (PuppetCommands#seizeRefusal), a freezing holds it half as long. */
	public boolean ironLocked() {
		return hasArray(CircuitBoard.Array.IRON_LOCK, false);
	}

	// --- The linked army (phase 3) ---

	/** Whether its heart's link knot links it to its puppeteer's other linked puppets (CircuitBoard.Scores#linked). */
	public boolean linked() {
		if (scores == null) rebuild(false);
		return linked;
	}

	/** Its linked fellows within reach a second ago (none if it isn't linked). */
	public int linkFellows() {
		return Math.max(0, linkFellows);
	}

	/** Its place in a ring round its foe (PuppetFormations), or null. */
	public @Nullable Vec3 formationSpot() {
		return formationSpot;
	}

	/** Each second: its linked fellows within reach, and the strength they lend it (+3% each to its blows and art, +30% at most). */
	private void linkUp() {
		int fellows = 0;
		UUID owner = getOwnerUUID();
		if (linked && owner != null) {
			fellows = level().getEntitiesOfClass(PuppetEntity.class, getBoundingBox().inflate(Puppetry.LINK_REACH), p -> p != this && p.isAlive()
					&& owner.equals(p.getOwnerUUID()) && p.linked() && p.distanceTo(this) <= Puppetry.LINK_REACH).size();
		}
		if (fellows == linkFellows) return;
		linkFellows = fellows;
		modifier(Attributes.ATTACK_DAMAGE, LINK_ID, "Puppet's linked fellows", Puppetry.linkBonus(fellows));
	}

	/**
	 * Whether it is held down where it stands (the design: "Where qi is suppressed, puppets drop to half strength"): above the lower realms'
	 * cap where a lower realm's law holds (a secret realm's, its world's), unless its core is shielded (DemonCoreItem#shielded).
	 */
	public boolean suppressed() {
		if (DemonCoreItem.shielded(core) || !(level() instanceof ServerLevel level)) return false;
		return rank > PlayerCultivation.rank(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE)
				&& ModDimensions.isLowerRealm(SecretRealms.lawOf(level, blockPosition()));
	}

	/** Each second: held down or not, its health and harm halved or whole again (the share of its health it had kept). */
	private void holdDown() {
		int now = suppressed() ? 1 : 0;
		if (now == suppressedNow) return;
		boolean tell = suppressedNow >= 0;
		suppressedNow = now;
		float before = getMaxHealth(), health = getHealth();
		modifier(Attributes.MAX_HEALTH, HELD_DOWN_HEALTH_ID, "Puppet held down", now == 1 ? Puppetry.SUPPRESSED - 1 : 0);
		modifier(Attributes.ATTACK_DAMAGE, HELD_DOWN_HARM_ID, "Puppet held down", now == 1 ? Puppetry.SUPPRESSED - 1 : 0);
		if (before > 0 && getMaxHealth() != before) setHealth(Math.min(getMaxHealth(), health * getMaxHealth() / before));
		if (tell && getOwner() instanceof ServerPlayer owner && owner.distanceTo(this) < 64) {
			owner.displayClientMessage(Component.translatable(now == 1 ? ModLang.PUPPET_HELD_DOWN : ModLang.PUPPET_WHOLE_AGAIN, getDisplayName())
					.withStyle(now == 1 ? ChatFormatting.GOLD : ChatFormatting.AQUA), true);
		}
	}

	/** Whether a Spring of the Ancients was poured into it (self-repair: it mends its own cut lines). */
	public boolean springFed() {
		return springFed;
	}

	/**
	 * One of its cut lines mended (by itself, a spring poured into it; or by a Puppet Factory beside it): the oldest still bare, as it was
	 * drawn (a plain line where that wasn't kept). Whether one was.
	 */
	public boolean mendOne() {
		CircuitBoard b = board();
		CircuitBoard.Stroke[] all = CircuitBoard.Stroke.values();
		for (int k = 0; k < cuts.size(); k++) {
			int cell = cuts.getInt(k), x = cell % b.size, y = cell / b.size;
			if (!b.inside(x, y) || !b.isFree(x, y) || b.stroke(x, y) != CircuitBoard.Stroke.NONE) {
				// Painted over since: no cut any more.
				cuts.removeInt(k--);
				cutWas.remove(cell);
				continue;
			}
			int was = cutWas.getOrDefault(cell, CircuitBoard.Stroke.LINE.ordinal());
			b.set(x, y, was > 0 && was < all.length ? all[was] : CircuitBoard.Stroke.LINE);
			cuts.removeInt(k);
			cutWas.remove(cell);
			regrowing.removeIf(r -> r[0] == cell);
			strokes = Puppetry.strokes(b);
			rebuild(true);
			if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.6), getZ(), 4, 0.2, 0.3, 0.2, 0.01);
			return true;
		}
		return false;
	}

	/** Self-destruct (the Command Seal's page): its fuse lit, it bursts {@link Puppetry#FUSE_TICKS} later. */
	public void lightFuse() {
		if (fuse >= 0) return;
		fuse = Puppetry.FUSE_TICKS;
		playSound(SoundEvents.TNT_PRIMED, 1.0f, 0.8f);
	}

	public boolean fuseLit() {
		return fuse >= 0;
	}

	/**
	 * Its burst: {@link Puppetry#BURST_POWER} times its blow to everything within {@link Puppetry#burstReach} blocks, falling to
	 * {@link Puppetry#BURST_EDGE} of that at the edge; never its puppeteer, their allies nor their puppets; no block broken. Its core goes
	 * with it: nothing is left but what it carried (a General's ranks set down first); a player's soul in it dies as any death goes.
	 */
	private void burst() {
		if (!(level() instanceof ServerLevel level)) return;
		LivingEntity owner = getOwner();
		double reach = Puppetry.burstReach(grade), harm = Puppetry.BURST_POWER * getAttributeValue(Attributes.ATTACK_DAMAGE);
		deploy();
		int caught = 0;
		DamageSource source = damageSources().explosion(this, owner != null ? owner : this);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach), e -> e != this && e.isAlive())) {
			double d = e.distanceTo(this);
			if (d > reach || spared(e, owner)) continue;
			if (e.hurt(source, (float) (harm * (1 - (1 - Puppetry.BURST_EDGE) * d / reach)))) caught++;
		}
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(0.5), getZ(), 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY(0.5), getZ(), 40, reach / 3, reach / 4, reach / 3, 0.05);
		level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 2.0f, 0.9f);
		for (int i = 0; i < cargo.getContainerSize(); i++) if (!cargo.getItem(i).isEmpty()) spawnAtLocation(cargo.removeItemNoUpdate(i));
		core = ItemStack.EMPTY;
		PuppetPlayers.lostWith(this, ModLang.PUPPET_SOUL_WRECKED);
		forget();
		ServerPlayer told = getOwnerUUID() == null ? null : level.getServer().getPlayerList().getPlayer(getOwnerUUID());
		if (told != null) told.sendSystemMessage(Component.translatable(ModLang.PUPPET_BURST, getDisplayName(), caught).withStyle(ChatFormatting.GOLD));
		discard();
	}

	/** Whether its puppeteer's word reaches it through its linked fellows (PuppetCommands#relayed), weighed once a second while it is asked. */
	private boolean relayedFrom(ServerLevel level) {
		if (!linked || getOwnerUUID() == null) return false;
		if (level.getGameTime() >= relayedUntil) {
			relayedUntil = level.getGameTime() + 20;
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(getOwnerUUID());
			relayed = owner != null && owner.level() == level && PuppetCommands.relayed(owner, this);
		}
		return relayed;
	}

	/** Who knows its circuits among those who strike it: a player, or a player's puppet's puppeteer (never its own). */
	private boolean knownTo(@Nullable Entity striker) {
		UUID knower = striker instanceof Player p ? p.getUUID() : striker instanceof PuppetEntity puppet && puppet.ownedByPlayer() ? puppet.getOwnerUUID() : null;
		return knower != null && !knower.equals(getOwnerUUID()) && level() instanceof ServerLevel level && Puppetry.knows(level.getServer(), knower, design());
	}

	/** Whether it can see {@code e}: the unseen only with an Eye of Discernment. */
	private boolean perceives(LivingEntity e) {
		return eye || !e.isInvisible();
	}

	/** Node {@code id}'s phases (bits), none for a dead one or none such. */
	private int phasesOf(String id) {
		int n = node(id);
		return n < 0 || scores == null || scores.dead()[n] ? 0 : scores.phases()[n];
	}

	/** Its body's veins of {@code phase} (legs, wings, head, heart). */
	public int bodyVeins(CircuitBoard.Phase phase) {
		return bodyPhaseVeins[phase.ordinal()];
	}

	/** Whether its blows (a bee's sting) carry {@code phase}. */
	public boolean blowsCarry(CircuitBoard.Phase phase) {
		return (blowPhases & phase.bit()) != 0;
	}

	/** Whether its art carries {@code phase}. */
	public boolean artCarries(CircuitBoard.Phase phase) {
		return (artPhases & phase.bit()) != 0;
	}

	/** How much farther than one fed vein's its heart hears its puppeteer (CommandSealItem, its following). */
	public double reachFactor() {
		if (scores == null) rebuild(false);
		return reachFactor;
	}

	/** Its heart's veins: each past the first wants a seizer a rank more (PuppetCommands#seizeRefusal). */
	public int heartVeins() {
		if (scores == null) rebuild(false);
		return heartVeins;
	}

	/** The ticks it takes to take up a new foe or order: its Clarity's, by its head's veins (twice as slow with its head dead); none with an Eye. */
	private int reactionTicks() {
		if (scores == null) rebuild(false);
		if (eye) return 0;
		return (int) Math.round(Puppetry.reactionTicks(scores().clarity()) * reactionFactor * (minded(PuppetSoul.Trait.WATCHFUL) ? PuppetSouls.WATCHFUL_EYE : 1));
	}

	private void modifier(net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, String name, double amount) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(id);
		if (Math.abs(amount) > 1.0e-6) instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	/**
	 * A line of its board cut (a fifth of its health lost, a blow of Metal qi): a painted cell no ward keeps, picked at random. Metal in its
	 * body turns some cuts aside (Puppetry#glanceChance); a cut Wood line grows back in a while (Puppetry#REGROW_TICKS).
	 */
	public void cut(boolean aimed) {
		if (random.nextDouble() < Puppetry.glanceChance(bodyVeins(CircuitBoard.Phase.METAL))) {
			playSound(SoundEvents.ANVIL_LAND, 0.4f, 1.8f);
			return;
		}
		CircuitBoard b = board();
		boolean[] warded = b.wardedCells();
		List<Integer> lines = new ArrayList<>();
		for (int y = 0; y < b.size; y++) {
			for (int x = 0; x < b.size; x++) {
				int i = b.at(x, y);
				if (b.isFree(x, y) && b.stroke(x, y) != CircuitBoard.Stroke.NONE && !warded[i]) lines.add(i);
			}
		}
		if (lines.isEmpty()) return;
		// Struck by one who knows its circuits (phase 3): its weakest lines, those a single cut leaves a part unfed by, where there are any.
		if (aimed) {
			boolean[] critical = b.critical();
			List<Integer> weakest = new ArrayList<>();
			for (int i : lines) if (critical[i]) weakest.add(i);
			if (!weakest.isEmpty()) lines = weakest;
		}
		int cell = lines.get(random.nextInt(lines.size()));
		if (b.phases()[cell] == CircuitBoard.Phase.WOOD) regrowing.add(new int[] {cell, b.stroke(cell % b.size, cell / b.size).ordinal(), tickCount + Puppetry.REGROW_TICKS});
		cutWas.put(cell, b.stroke(cell % b.size, cell / b.size).ordinal()); // what it held (phase 3)
		b.set(cell % b.size, cell / b.size, CircuitBoard.Stroke.NONE);
		if (!cuts.contains(cell)) cuts.add(cell);
		strokes = Puppetry.strokes(b);
		rebuild(true);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.6), getZ(), 12, 0.25, 0.3, 0.25, 0.15);
			level.playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.NEUTRAL, 0.8f, 1.4f);
			// Its puppeteer's record of where its design's lines are cut in fights (phase 3: the Overseer Core's wear map).
			if (ownerIsPlayer && getOwnerUUID() != null) Puppetry.Ledger.get(level.getServer()).recordCut(getOwnerUUID(), design(), b.size * b.size, cell);
		}
	}

	// --- Orders ---

	/**
	 * Told {@code order}: a guard's post at {@code from} (its puppeteer's place: "guard here"; where it stands without one), holding where it
	 * stands, a bee's work between the chests {@code from} and {@code to}; the ledger keeps it.
	 */
	public void command(Order order, @Nullable BlockPos from, @Nullable BlockPos to) {
		this.order = order;
		post = order == Order.GUARD ? (from != null ? from.immutable() : blockPosition()) : order == Order.HOLD ? blockPosition() : null;
		if (order == Order.WORK) {
			carryFrom = from;
			carryTo = to;
			carryWait = 0;
		}
		setTarget(null);
		getNavigation().stop();
		reaction = reactionTicks();
		remember();
	}

	/** Frozen by a Circuit-Freeze Talisman for {@code ticks} (half as long with an Iron Lock): its circuits locked, it stands still. */
	public void freeze(int ticks) {
		if (ironLocked()) ticks = (int) Math.round(ticks * Puppetry.LOCK_FREEZE);
		frozenTicks = Math.max(frozenTicks, ticks);
		flag(FROZEN, true);
		getNavigation().stop();
		setTarget(null);
	}

	/** Come down by its addressee with {@code errand} (PuppetCouriers): it lingers a while, then goes back. */
	public void lingerAsCourier(CompoundTag errand) {
		courier = errand;
		courierLeave = PuppetCouriers.LINGER;
		getNavigation().stop();
		setTarget(null);
	}

	/** Its errand done with (rising on its way: PuppetCouriers#depart). */
	public void clearCourier() {
		courier = null;
		courierLeave = 0;
	}

	/**
	 * Taken into {@code owner}'s hands (seized by a stronger puppeteer: CommandSealItem), bound to their seal of {@code seal}: a General's ranks
	 * set down first (still their old puppeteer's), every Sub-Seal of it void.
	 */
	public void seizedBy(ServerPlayer owner, ArtifactGrade seal) {
		deploy();
		forget();
		setOwner(owner.getUUID(), owner.getGameProfile().getName(), true);
		sealGrade = seal;
		revokeSubSeals();
		released = false; // a player's soul in it held by its new puppeteer, its struggle begun anew (phase 2d)
		stopStruggle();
		order = Order.FOLLOW;
		post = null;
		carryFrom = carryTo = null;
		setTarget(null);
		remember();
	}

	// --- Its ranks of command (phase 2c) ---

	/**
	 * The grade of the seal it is bound to: the best its puppeteer carried as it was sealed, raised by an order given with a better one;
	 * a puppet sealed before seals had grades Mortal, a cultivator's puppet Spirit (its maker counts as an Adept).
	 */
	public ArtifactGrade sealGrade() {
		return sealGrade != null ? sealGrade : ownerIsPlayer ? ArtifactGrade.MORTAL : ArtifactGrade.SPIRIT;
	}

	/** Bound to a seal of {@code grade}, if that is above the one it is bound to; whether it rose. */
	public boolean bindSeal(ArtifactGrade grade) {
		if (grade.ordinal() <= sealGrade().ordinal()) return false;
		sealGrade = grade;
		return true;
	}

	/** The holder of a Sub-Seal it answers (null: its puppeteer alone), and their name. */
	public @Nullable UUID commander() { return commander; }
	public String commanderName() { return commanderName; }
	/** The mark its Sub-Seals carry: one written before a revoking is void. */
	public int subNonce() { return subNonce; }

	/** The one it follows and fights for: the holder of its Sub-Seal while they are in its world, else its puppeteer. */
	public @Nullable LivingEntity leader() {
		if (commander != null && level() instanceof ServerLevel level && level.getEntity(commander) instanceof ServerPlayer holder && holder.isAlive()
				&& !holder.isSpectator()) {
			return holder;
		}
		return getOwner();
	}

	/**
	 * {@code holder}'s Sub-Seal pressed on it: taken into their hands, following them; pressed again, on to its next order (guarding where
	 * they stand, then holding, then following again). Its puppeteer is told when it changes hands. The order it is under now.
	 */
	public Order answerSubSeal(ServerPlayer holder) {
		boolean own = holder.getUUID().equals(getOwnerUUID());
		boolean theirs = own ? commander == null : holder.getUUID().equals(commander);
		Order next = !theirs ? Order.FOLLOW : switch (order) {
			case FOLLOW -> Order.GUARD;
			case GUARD -> Order.HOLD;
			default -> Order.FOLLOW;
		};
		if (!theirs) {
			commander = own ? null : holder.getUUID();
			commanderName = own ? "" : holder.getGameProfile().getName();
			ServerPlayer owner = own || !(level() instanceof ServerLevel level) || getOwnerUUID() == null ? null
					: level.getServer().getPlayerList().getPlayer(getOwnerUUID());
			if (owner != null) {
				owner.displayClientMessage(Component.translatable(ModLang.SUB_SEAL_LENT, getDisplayName(), holder.getName()).withStyle(ChatFormatting.GRAY), false);
			}
		}
		command(next, next == Order.GUARD ? holder.blockPosition() : null, null);
		return next;
	}

	/** Its puppeteer's own order: whoever held a Sub-Seal of it holds it no longer (the seal still good for another time). */
	public void reclaim() {
		commander = null;
		commanderName = "";
	}

	/** Every Sub-Seal written for it so far void, and whoever held one let go. */
	public void revokeSubSeals() {
		subNonce++;
		reclaim();
	}

	/** A General (phase 2c: made on the Command Seal's page, PuppetCommands). */
	public boolean isGeneral() { return general; }

	public void makeGeneral() {
		general = true;
	}

	/** The puppets in its ranks. */
	public int ranksHeld() { return ranks.size(); }

	/**
	 * {@code puppets} (its puppeteer's, near it) taken into its ranks, the nearest first, until they are full: out of the world, each kept as
	 * it was, still on its puppeteer's list and in their slots. How many went in.
	 */
	public int muster(List<PuppetEntity> puppets) {
		if (!(level() instanceof ServerLevel level) || getOwnerUUID() == null) return 0;
		Puppetry.Ledger ledger = Puppetry.Ledger.get(level.getServer());
		String name = getDisplayName().getString();
		int took = 0;
		for (PuppetEntity p : puppets) {
			if (ranks.size() >= Puppetry.GENERAL_RANKS) break;
			if (p == this || p.isGeneral() || !p.isAlive() || p.courier != null || !getOwnerUUID().equals(p.getOwnerUUID())) continue;
			if (PuppetPlayers.holdsPlayer(p)) continue; // a player's soul in it: a person, never packed away (phase 2d)
			CompoundTag body = new CompoundTag();
			p.setTarget(null);
			if (!p.saveAsPassenger(body)) continue;
			ranks.add(body);
			ledger.inRanks(getOwnerUUID(), p.getUUID(), name);
			level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(0.5), p.getZ(), 10, 0.3, 0.4, 0.3, 0.03);
			level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY(0.5), p.getZ(), 16, 0.3, 0.5, 0.3, 0.4);
			p.discard();
			took++;
		}
		if (took > 0) playSound(SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 0.8f);
		return took;
	}

	/** Its ranks set down round it, each as it went in (a guard's or a holder's post moved to where it lands). How many came out. */
	public int deploy() {
		if (!(level() instanceof ServerLevel level) || ranks.isEmpty()) return 0;
		Puppetry.Ledger ledger = Puppetry.Ledger.get(level.getServer());
		List<CompoundTag> out = new ArrayList<>(ranks);
		ranks.clear();
		int n = 0;
		for (CompoundTag body : out) {
			double angle = Math.PI * 2 * n++ / out.size();
			Vec3 at = position().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2);
			net.minecraft.world.entity.Entity e = net.minecraft.world.entity.EntityType.loadEntityRecursive(body, level, x -> {
				x.moveTo(at.x, at.y, at.z, getYRot(), 0);
				return x;
			});
			if (!(e instanceof PuppetEntity p)) continue;
			UUID was = p.getUUID();
			if (level.getEntity(was) != null) p.setUUID(UUID.randomUUID()); // a twin of it already about: this one anew
			if (p.getOwnerUUID() != null) {
				ledger.inRanks(p.getOwnerUUID(), was, null);
				if (!was.equals(p.getUUID())) ledger.remove(p.getOwnerUUID(), was);
			}
			p.findRoom();
			if (p.order == Order.GUARD || p.order == Order.HOLD) p.post = p.blockPosition();
			level.addFreshEntity(p);
			p.remember();
			level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(0.5), p.getZ(), 10, 0.3, 0.4, 0.3, 0.03);
		}
		playSound(SoundEvents.ARMOR_EQUIP_CHAIN, 1.0f, 0.7f);
		return out.size();
	}

	// --- A player's soul in it (phase 2d: PuppetPlayers) ---

	/** Released by its puppeteer: the player's soul in it plays it. */
	public boolean released() { return released; }

	public void setReleased(boolean released) {
		this.released = released;
	}

	/** The player playing it now (both sides), or null. */
	public @Nullable Player piloted() {
		return PuppetPlayers.pilotOf(this);
	}

	/** Frozen by a talisman or stalled by its heat (both sides): it moves for no one, its own soul in it either. */
	public boolean stalled() {
		return isFrozen() || isOverheated();
	}

	/** Its held soul struggling from within. */
	public boolean struggling() { return struggling; }

	public void struggle() {
		struggling = true;
	}

	public void stopStruggle() {
		struggling = false;
		struggleTicks = 0;
	}

	/**
	 * {@code ticks} more of its held soul's struggle, each cell's time halved {@code halvings} times: when a cell is due, the line to its heart
	 * cut there from within (PuppetPlayers#STRUGGLE_TICKS a cell, PuppetPlayers#WARDED_TIMES as long where a ward keeps it), its puppeteer
	 * told. Whether its heart is cut off now (the soul free).
	 */
	public boolean struggleTick(int ticks, int halvings) {
		int cell = heartLineCell();
		if (cell < 0) return true;
		if ((struggleTicks += ticks) < struggleDue(cell, halvings)) return false;
		struggleTicks = 0;
		cutCell(cell);
		if (getOwner() instanceof ServerPlayer owner) {
			owner.displayClientMessage(Component.translatable(ModLang.PUPPET_SOUL_CUT_FROM_WITHIN, getDisplayName()).withStyle(ChatFormatting.RED), false);
		}
		return heartLineCell() < 0;
	}

	/**
	 * How long a struggle takes to cut {@code cell} from within (ticks), each cell's time halved {@code halvings} times: PuppetPlayers#STRUGGLE_TICKS,
	 * PuppetPlayers#WARDED_TIMES as long where a ward keeps it.
	 */
	public int struggleDue(int cell, int halvings) {
		CircuitBoard b = board();
		boolean warded = b.wardedCells()[cell] || b.stroke(cell % b.size, cell / b.size) == CircuitBoard.Stroke.WARD;
		return Math.max(20, (PuppetPlayers.STRUGGLE_TICKS >> Math.min(8, halvings)) * (warded ? PuppetPlayers.WARDED_TIMES : 1));
	}

	/** How far its held soul's struggle has come on the cell it cuts next, of 100 (0: not struggling). */
	public int struggleProgress(int halvings) {
		int cell = struggling ? heartLineCell() : -1;
		return cell < 0 ? 0 : Math.min(100, struggleTicks * 100 / struggleDue(cell, halvings));
	}

	/**
	 * The cell of the command line a struggle takes next (the design: "take over the command line one cell at a time, working from the core
	 * toward the Seal node"): on the heart's way from the core, the painted cell nearest the core; -1 if its heart is fed by nothing.
	 */
	public int heartLineCell() {
		int[] line = commandLine();
		return line.length == 0 ? -1 : line[0];
	}

	/**
	 * Its command line as a soul held in it sees it (the user, 2026-11-18: "a non-free puppet should see circuits too so they can
	 * takeover/struggle free"): the painted cells on the heart's way from the core, the nearest the core first; none if its heart is fed by
	 * nothing.
	 */
	public int[] commandLine() {
		if (scores == null) rebuild(false);
		int heart = node("heart");
		if (heart < 0 || dead("heart")) return new int[0];
		CircuitBoard b = board();
		it.unimi.dsi.fastutil.ints.IntArrayList line = new it.unimi.dsi.fastutil.ints.IntArrayList();
		for (int i : b.routes()[heart]) {
			int x = i % b.size, y = i / b.size;
			if (b.isFree(x, y) && b.stroke(x, y) != CircuitBoard.Stroke.NONE) line.add(i);
		}
		return line.toIntArray();
	}

	/** Its line at {@code cell} cut (a ward keeps nothing from a struggle within). */
	private void cutCell(int cell) {
		CircuitBoard b = board();
		cutWas.put(cell, b.stroke(cell % b.size, cell / b.size).ordinal()); // what it held (phase 3)
		b.set(cell % b.size, cell / b.size, CircuitBoard.Stroke.NONE);
		if (!cuts.contains(cell)) cuts.add(cell);
		strokes = Puppetry.strokes(b);
		rebuild(true);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL, getX(), getY(0.6), getZ(), 8, 0.2, 0.3, 0.2, 0.03);
			level.playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.NEUTRAL, 0.8f, 0.9f);
		}
	}

	/** Its blow at {@code target}, as the soul playing it strikes (within its reach, its wait between blows as in a fight). */
	public void pilotStrike(Entity target) {
		if (attackCooldown > 0 || stalled() || !target.isAlive() || target == this) return;
		if (distanceTo(target) > getBbWidth() * 2 + target.getBbWidth() + 1.5) return;
		attackCooldown = (int) Math.round(20 / Puppetry.harmonyFactor(scores().harmony()));
		doHurtTarget(target);
	}

	/** Its art, loosed by {@code pilot} along {@code look}: at the first being in its line within its art's reach; told if it can't. */
	public void pilotArt(ServerPlayer pilot, Vec3 look) {
		if (stalled()) return;
		if (!artReady()) {
			pilot.displayClientMessage(Component.translatable(ModLang.PUPPET_SOUL_NO_ART).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		Vec3 eye = getEyePosition(), end = eye.add(look.scale(ART_RANGE));
		net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level(), this, eye, end,
				getBoundingBox().expandTowards(look.scale(ART_RANGE)).inflate(1), e -> e instanceof LivingEntity living && living.isAlive() && e != pilot
						&& !(e instanceof Player p && (p.isSpectator() || PuppetPlayers.isPilot(p))));
		if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
			pilot.displayClientMessage(Component.translatable(ModLang.PUPPET_SOUL_NO_TARGET).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		fireArt(target);
	}

	// --- Ticking ---

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new Still());
		if (!isBee()) goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new Fight());
		goalSelector.addGoal(3, new Carry());
		goalSelector.addGoal(4, new Keep());
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			if (isOverheated() && random.nextInt(3) == 0) {
				level().addParticle(ParticleTypes.SMOKE, getRandomX(0.4), getY(0.8), getRandomZ(0.4), 0, 0.05, 0);
			}
			return;
		}
		if (board == null) rebuild(false);
		// A fuse lit (phase 3: self-destruct): smoke, then the burst.
		if (fuse >= 0) {
			if (fuse-- <= 0) {
				burst();
				return;
			}
			if (level() instanceof ServerLevel level && fuse % 3 == 0) level.sendParticles(ParticleTypes.SMOKE, getX(), getY(0.9), getZ(), 3, 0.2, 0.2, 0.2, 0.02);
		}
		if (isBee()) setNoGravity(!isStill()); // a still bee sinks to the ground; awake, it hovers
		// A cultivator's puppet with its maker gone (slain, or away a minute) falls apart where it stands (NpcPuppets).
		if (!ownerIsPlayer) {
			if (getOwner() != null) ownerGone = 0;
			else if (++ownerGone >= OWNER_GONE) {
				wreck();
				discard();
				return;
			}
		}
		if (courier != null && --courierLeave <= 0) {
			PuppetCouriers.depart(this, courier, 1, courier.getLong("Travel"));
			return;
		}
		if (attackCooldown > 0) attackCooldown--;
		if (reaction > 0) reaction--;
		if (frozenTicks > 0 && --frozenTicks == 0) flag(FROZEN, false);
		if (stallTicks > 0 && --stallTicks == 0) flag(OVERHEATED, false);
		if (artLock > 0) artLock--;
		// Its art's charges come back by themselves (twice as fast with a Gathering Spiral, half again with a Canny mind).
		int recharge = minded(PuppetSoul.Trait.CANNY) ? (int) Math.round(RECHARGE / PuppetSouls.CANNY_RECHARGE) : RECHARGE;
		if (scores != null && charges < maxCharges() && (rechargeTimer += spiral ? Puppetry.SPIRAL_RECHARGE : 1) >= recharge) {
			charges++;
			rechargeTimer = 0;
		}
		if (soulArtWait > 0) soulArtWait--;
		// Its soul copied while it is under its puppeteer's control; whole, the real soul goes on (phase 2a).
		if (soul != null && soul.real() && tickCount % 20 == 0 && underControl() && soul.copy(20)) {
			if (getOwner() instanceof ServerPlayer owner) {
				owner.sendSystemMessage(Component.translatable(ModLang.SOUL_COPY_WHOLE, getDisplayName(), soul.name).withStyle(ChatFormatting.LIGHT_PURPLE));
			}
			if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.SOUL, getX(), getY(0.8), getZ(), 12, 0.2, 0.5, 0.2, 0.05);
			showSoul(); // a player's copy whole wears their look from now on (phase 2d)
			remember();
			// A player's: its real soul pushed out, on (phase 2d).
			if (soul.kind == PuppetSoul.Kind.PLAYER) PuppetPlayers.copiedWhole(this, soul);
		}
		if (soul != null && soul.growing() && tickCount % 20 == 0) growMind();
		if (guardWait > 0) guardWait--;
		if (dashWait > 0) dashWait--;
		if (dashTicks > 0) dash();
		// Heat shed a point every COOL ticks (twice as fast with a Canny mind).
		if (heat > 0 && ++coolTimer >= (minded(PuppetSoul.Trait.CANNY) ? COOL / (int) PuppetSouls.CANNY_COOL : COOL)) {
			heat--;
			coolTimer = 0;
		}
		// Its cut Wood lines growing back (each where nothing has been painted over it since).
		if (!regrowing.isEmpty() && tickCount % 20 == 0) regrow();
		// Water in its body: it moves in water as freely as on land.
		if (isInWater() && bodyVeins(CircuitBoard.Phase.WATER) > 0 && tickCount % 20 == 0) {
			addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE, 40, 0, false, false));
		}
		// (Played by a soul in it, phase 2d: its foes are that soul's to choose.)
		if (tickCount % 10 == 0 && !isStill() && piloted() == null) chooseFoe();
		// The linked army (phase 3): each second its linked fellows near and the strength they lend, and whether it is held down where it
		// stands; each half second its place in a ring round its foe.
		if ((tickCount + getId()) % 20 == 0) {
			linkUp();
			holdDown();
		}
		if ((tickCount + getId()) % 10 == 0) formationSpot = piloted() == null ? PuppetFormations.update(this) : null;
		// Self-repair: a spring poured into it mends its cut lines one by one.
		if (springFed && !cuts.isEmpty() && ++mendTimer >= Puppetry.SPRING_MEND_TICKS) {
			mendTimer = 0;
			mendOne();
		}
		if (tickCount % 100 == 0) remember();
	}

	/**
	 * A second of its soul's copy growing into a mind of its own (phase 2c): what it does weighs toward its bent (a second on watch, the
	 * ground it covers; its blows, arts and harm borne are weighed as they come), and under its Saint puppeteer's control it grows; grown,
	 * it takes a name of its own and its bent, and its puppeteer is told.
	 */
	private void growMind() {
		if (order == Order.GUARD || order == Order.HOLD) soul.mark(PuppetSoul.Trait.WATCHFUL, PuppetSouls.MARK_WATCH);
		Vec3 here = position();
		if (lastSpot != null) soul.mark(PuppetSoul.Trait.SWIFT, Math.min(40, here.distanceTo(lastSpot)) * PuppetSouls.MARK_BLOCK);
		lastSpot = here;
		if (!underControl() || !(getOwner() instanceof ServerPlayer owner) || Puppetry.rank(CultivationManager.own(owner)) != CraftMastery.Rank.SAINT) return;
		if (!soul.grow(20)) return;
		awakenMind(owner);
	}

	/** Its grown mind come awake: a name of its own and the bent its deeds leaned to. */
	public void awakenMind(@Nullable ServerPlayer owner) {
		if (soul == null || !soul.growing()) return;
		soul.awaken(soul.leaning(random), NpcNames.of(getUUID()));
		showSoul();
		applyStats();
		remember();
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(0.9), getZ(), 16, 0.25, 0.3, 0.25, 0.02);
			level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.0f, 1.2f);
		}
		if (owner != null) {
			owner.sendSystemMessage(Component.translatable(ModLang.PUPPET_GREW_A_MIND, soul.goesBy(), soul.trait().getDisplayName(),
					Component.translatable(ModLang.puppetTraitAbout(soul.trait().getId()))).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	/** Its foe: as its order has it, never its puppeteer nor theirs, nor one it can't see (the unseen, without an Eye). */
	private void chooseFoe() {
		LivingEntity current = getTarget();
		// Following, it fights for the one it follows (the holder of its Sub-Seal, phase 2c); never its puppeteer nor them.
		LivingEntity owner = leader();
		if (current != null && (!current.isAlive() || spared(current, owner) || distanceToSqr(current) > 32 * 32 || !perceives(current))) {
			setTarget(null);
			current = null;
		}
		if (current != null) return;
		LivingEntity foe = null;
		LivingEntity struck = getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < 200 ? getLastHurtByMob() : null;
		double sight = getAttributeValue(Attributes.FOLLOW_RANGE);
		switch (order) {
			case FOLLOW -> {
				if (owner != null && owner.level() == level()) {
					if (owner.getLastHurtByMob() != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200) foe = owner.getLastHurtByMob();
					else if (owner.getLastHurtMob() != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 200) foe = owner.getLastHurtMob();
				}
				if (foe == null) foe = struck;
				if (foe == null) foe = nearestHunting(owner, Math.min(sight, 16));
			}
			case GUARD -> {
				foe = struck;
				if (foe == null && post != null) {
					double best = GUARD_WATCH * GUARD_WATCH;
					Vec3 centre = Vec3.atCenterOf(post);
					for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(GUARD_WATCH + GUARD_KEEP),
							e -> e instanceof Enemy && e.isAlive() && perceives(e))) {
						double d = e.position().distanceToSqr(centre);
						if (d < best && distanceToSqr(e) < sight * sight) {
							best = d;
							foe = e;
						}
					}
				}
			}
			case HOLD, WORK -> foe = struck;
		}
		if (foe != null && (spared(foe, owner) || distanceToSqr(foe) > sight * sight * 1.5 || !perceives(foe))) foe = null;
		if (foe != null && foe != lastFoe) reaction = reactionTicks();
		lastFoe = foe;
		setTarget(foe);
	}

	/** The nearest monster hunting its puppeteer or it. */
	private @Nullable LivingEntity nearestHunting(@Nullable LivingEntity owner, double radius) {
		LivingEntity foe = null;
		double best = radius * radius;
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
				e -> e instanceof Enemy && e instanceof Mob mob && (mob.getTarget() == this || owner != null && mob.getTarget() == owner) && perceives(e))) {
			double d = e.distanceToSqr(this);
			if (d < best) {
				best = d;
				foe = e;
			}
		}
		return foe;
	}

	/** Never its puppeteer, the holder of its Sub-Seal, its puppeteer's other puppets or {@code owner}'s allies, nor itself. */
	private boolean spared(LivingEntity e, @Nullable LivingEntity owner) {
		if (e == this || owner != null && e == owner) return true;
		if (e.getUUID().equals(getOwnerUUID()) || commander != null && e.getUUID().equals(commander)) return true;
		if (e instanceof PuppetEntity other && getOwnerUUID() != null && getOwnerUUID().equals(other.getOwnerUUID())) return true;
		return owner != null && HeavenSpells.ally(owner, e);
	}

	@Override
	public boolean isAlliedTo(Entity other) {
		if (other.getUUID().equals(getOwnerUUID()) || commander != null && other.getUUID().equals(commander)) return true;
		if (other instanceof PuppetEntity puppet && getOwnerUUID() != null && getOwnerUUID().equals(puppet.getOwnerUUID())) return true;
		return super.isAlliedTo(other);
	}

	// --- Fighting ---

	/** Whether its art is ready: a Spirit frame or above, its art's node fed, a charge, not locked by heat. */
	public boolean artReady() {
		return grade.ordinal() >= ArtifactGrade.SPIRIT.ordinal() && charges > 0 && artLock <= 0 && !dead(isBee() ? "stinger" : "mouth");
	}

	/** Its art at {@code target}: its core's element as a beam (astray now and then by its Clarity), heating it. */
	private void fireArt(LivingEntity target) {
		if (!(level() instanceof ServerLevel level)) return;
		charges--;
		rechargeTimer = 0;
		heat += Puppetry.ART_HEAT;
		coolTimer = 0;
		triggerAnim("main", "beam");
		if (soul != null) soul.mark(PuppetSoul.Trait.CANNY, PuppetSouls.MARK_ART);
		Vec3 from = isBee() ? position().add(0, getBbHeight() * 0.35, 0) : getEyePosition().add(0, -0.12, 0);
		Vec3 to = target.position().add(0, target.getBbHeight() * 0.55, 0);
		boolean astray = random.nextDouble() < Puppetry.misfireChance(scores().clarity());
		if (astray) to = to.add((random.nextDouble() - 0.5) * 6, random.nextDouble() * 3, (random.nextDouble() - 0.5) * 6);
		QiElement element = element();
		SpellFx.send(level, SpellFx.Kind.STREAK, from, to, 1, glow(element));
		level.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.7f, 1.8f);
		if (!astray) {
			// Its art by its art's node's veins and its Flow (not its arms': its blows are taken back out).
			double flow = Puppetry.flowFactor(scores().flow());
			float harm = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) / Math.max(0.05, blowsFactor) * flow * artFactor * ART_POWER);
			DamageSource source = element != null && element != QiElement.YIN
					? ModDamageTypes.qi(level, element, CultivatorNpc.realmOf(rank), this, getOwner() != null ? getOwner() : this)
					: damageSources().mobAttack(this);
			if (target.hurt(source, harm)) strikeWith(target, artPhases, true, harm);
			// Metal in its art carries it on, half as hard, to the foe nearest the first (never its puppeteer's side).
			if (artCarries(CircuitBoard.Phase.METAL)) {
				LivingEntity owner = getOwner(), next = null;
				double best = 16;
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4),
						e -> e != target && e.isAlive() && !spared(e, owner) && (e instanceof net.minecraft.world.entity.monster.Enemy || e == target.getLastHurtByMob()))) {
					double d = e.distanceToSqr(target);
					if (d < best) {
						best = d;
						next = e;
					}
				}
				if (next != null) {
					SpellFx.send(level, SpellFx.Kind.STREAK, target.position().add(0, target.getBbHeight() * 0.55, 0), next.position().add(0, next.getBbHeight() * 0.55, 0), 1, glow(element));
					next.hurt(source, harm / 2);
				}
			}
		}
		if (heat > Puppetry.overheatAfter(scores().stability())) overheat();
	}

	/**
	 * Its Gale Stair: a dash at {@code target}, out of its {@code reach}, to within it (at most {@link Puppetry#STAIR_REACH} blocks), over
	 * {@link Puppetry#DASH_TICKS} ticks; a bee straight at it, a humanoid along the ground.
	 */
	public void startDash(LivingEntity target, double reach) {
		Vec3 to = target.position().subtract(position());
		double cover = Math.min(Puppetry.STAIR_REACH, to.length() - reach + 0.4);
		Vec3 way = isBee() ? to : new Vec3(to.x, 0, to.z);
		if (cover < 1 || way.lengthSqr() < 1.0e-6) return;
		dashStep = way.normalize().scale(cover / Puppetry.DASH_TICKS);
		dashTicks = Puppetry.DASH_TICKS;
		dashWait = Puppetry.cooldown(Puppetry.STAIR_TICKS, stairTwin);
		getNavigation().stop();
		playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.6f, 1.8f);
	}

	/** A tick of its dash: a step through the air, stopped by what stands in its way, a gust behind it (none once it is still). */
	private void dash() {
		if (isStill()) {
			dashTicks = 0;
			return;
		}
		dashTicks--;
		move(net.minecraft.world.entity.MoverType.SELF, dashStep);
		resetFallDistance();
		if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.CLOUD, getX(), getY(0.3), getZ(), 2, 0.1, 0.1, 0.1, 0.01);
	}

	/** Its Gale Stair's wait, in ticks (0: ready). */
	public int dashWait() {
		return dashWait;
	}

	/**
	 * One of its cultivator's soul's own arts at {@code target} (phase 2a), cast as its own at its own realm (Spells#harm), facing it first so
	 * the art goes where it means; it heats it as its core's art does.
	 */
	public void castSoulArt(LivingEntity target) {
		if (soul == null || soul.arts.isEmpty() || !(level() instanceof ServerLevel)) return;
		Spell spell = soul.arts.get(random.nextInt(soul.arts.size()));
		soulArtWait = PuppetSouls.ART_EVERY;
		Vec3 to = HeavenSpells.middle(target).subtract(getEyePosition());
		float yaw = (float) (net.minecraft.util.Mth.atan2(to.z, to.x) * net.minecraft.util.Mth.RAD_TO_DEG) - 90f;
		float pitch = (float) -(net.minecraft.util.Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * net.minecraft.util.Mth.RAD_TO_DEG);
		setYRot(yaw);
		setYHeadRot(yaw);
		yBodyRot = yaw;
		setXRot(pitch);
		triggerAnim("main", "beam");
		HeavenSpells.cast(this, spell);
		soul.mark(PuppetSoul.Trait.CANNY, PuppetSouls.MARK_ART);
		heat += Puppetry.ART_HEAT;
		coolTimer = 0;
		if (heat > Puppetry.overheatAfter(scores().stability())) overheat();
	}

	/** Its cultivator's soul's arts' wait, in ticks (0: ready). */
	public int soulArtWait() {
		return soulArtWait;
	}

	/** Too many arts in a row for its Stability: it stalls, smoking, and its art is locked a while. */
	private void overheat() {
		heat = 0;
		stallTicks = STALL;
		artLock = ART_LOCK;
		flag(OVERHEATED, true);
		getNavigation().stop();
		playSound(SoundEvents.FIRE_EXTINGUISH, 1.0f, 0.8f);
		// Its puppeteer's record of where its design's lines run hot in fights (phase 3: the Overseer Core's wear map).
		if (ownerIsPlayer && getOwnerUUID() != null && level() instanceof ServerLevel level) {
			Puppetry.Ledger.get(level.getServer()).recordOverheat(getOwnerUUID(), design(), board().heat(Puppetry.budget(grade, core), Puppetry.phaseOf(core)));
		}
		if (getOwner() instanceof ServerPlayer owner && owner.distanceTo(this) < 64) {
			owner.displayClientMessage(Component.translatable(ModLang.PUPPET_OVERHEATED, getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		}
	}

	/** Its art's colour: its element's (as a core glows), white for none. */
	private static int glow(@Nullable QiElement element) {
		if (element == null) return 0xE8F4FF;
		return switch (element) {
			case WOOD -> 0x3CE65A;
			case WATER -> 0x3C8CFF;
			case FIRE -> 0xFF5A20;
			case EARTH -> 0xD99A2B;
			case METAL -> 0xFFD050;
			case YIN -> 0x7AF0F0;
		};
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		triggerAnim("main", isBee() ? "sting" : "strike");
		swing(InteractionHand.MAIN_HAND); // a body of flesh's arm swings with the blow (phase 2b: drawn as a cultivator is)
		// Every blow heats its lines (on every frame, the user's pick of 2026-11-17): past its Stability's bound it stalls. Fire in its
		// body runs hotter.
		heat += Puppetry.BLOW_HEAT + (bodyVeins(CircuitBoard.Phase.FIRE) > 0 ? 1 : 0);
		coolTimer = 0;
		if (heat > Puppetry.overheatAfter(scores().stability())) overheat();
		// Its aim, by its Harmony: below even it misses now and then.
		double harmony = Puppetry.harmonyFactor(scores().harmony());
		if (harmony < 1 && random.nextDouble() < 1 - harmony) {
			playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.8f, 1.2f);
			return false;
		}
		// Metal in its blows bites through a third of what the foe's armour would take off them.
		AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null && blowsCarry(CircuitBoard.Phase.METAL) && target instanceof LivingEntity living && living.getArmorValue() > 0) {
			float raw = (float) damage.getValue();
			float after = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(raw, living.getArmorValue(),
					(float) living.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
			if (after > 0 && after < raw) {
				damage.addTransientModifier(new AttributeModifier(METAL_BITE_ID, "Puppet circuits' metal", (after + (raw - after) / 3) / after - 1,
						AttributeModifier.Operation.MULTIPLY_TOTAL));
			}
		}
		boolean hit;
		try {
			hit = super.doHurtTarget(target);
		} finally {
			if (damage != null) damage.removeModifier(METAL_BITE_ID);
		}
		if (hit && target instanceof LivingEntity living) strikeWith(living, blowPhases, false, damage == null ? 0 : damage.getValue());
		if (hit && soul != null) soul.mark(PuppetSoul.Trait.FIERCE, PuppetSouls.MARK_BLOW);
		return hit;
	}

	/**
	 * What its phases do to {@code foe} it has struck ({@code art}: with its art; else a blow of {@code harm}): Wood heals it (a tenth of the
	 * blow) or roots the foe, Water slows the foe, Fire sets it alight, Earth knocks it back (or staggers it). Metal works on the blow itself
	 * ({@link #doHurtTarget}) or carries the art on to a second foe ({@link #fireArt}).
	 */
	private void strikeWith(LivingEntity foe, int phases, boolean art, double harm) {
		if ((phases & CircuitBoard.Phase.WOOD.bit()) != 0) {
			if (art) foe.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 20, 9), this);
			else heal((float) (harm * 0.1));
		}
		if ((phases & CircuitBoard.Phase.WATER.bit()) != 0) {
			foe.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, art ? 60 : 40, 1), this);
		}
		if ((phases & CircuitBoard.Phase.FIRE.bit()) != 0) foe.setSecondsOnFire(art ? 5 : 3);
		if ((phases & CircuitBoard.Phase.EARTH.bit()) != 0) {
			double dx = foe.getX() - getX(), dz = foe.getZ() - getZ();
			foe.knockback(art ? 1.0 : 0.6, -dx, -dz);
			if (art) foe.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 10, 2), this);
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide) return super.hurt(source, amount);
		// Its puppeteer's own blows and spells pass through it, and its Sub-Seal's holder's (phase 2c).
		if (source.getEntity() != null && (source.getEntity().getUUID().equals(getOwnerUUID())
				|| commander != null && source.getEntity().getUUID().equals(commander))) return false;
		// Its Guard Coil: a foe's blow turned aside every so often (awake; never what no shield could stop).
		if (guard && guardWait <= 0 && !isStill() && source.getEntity() != null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) {
			guardWait = Puppetry.cooldown(Puppetry.GUARD_TICKS, guardTwin);
			playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 1.3f);
			if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY(0.6), getZ(), 12, 0.3, 0.3, 0.3, 0.1);
			return false;
		}
		// A beast's soul in its ring: a beast's start, slipping a blow now and then (phase 2a).
		if (soul != null && soul.kind == PuppetSoul.Kind.BEAST && !isStill() && source.getEntity() != null && random.nextDouble() < PuppetSouls.BEAST_DODGE) {
			playSound(SoundEvents.PLAYER_ATTACK_WEAK, 0.6f, 1.8f);
			return false;
		}
		boolean metal = source instanceof QiDamageSource qi ? qi.element() == QiElement.METAL : source.is(ModDamageTypes.qiType(QiElement.METAL));
		if (metal) amount *= 1.5f;
		// Its circuits known to whoever strikes it (phase 3: a wreck of its design taken apart): harder, and its weakest lines cut.
		boolean known = knownTo(source.getEntity());
		if (known) amount *= (float) Puppetry.KNOWN_HARM;
		// Its dodging, by its Harmony: above even it slips a blow now and then.
		double harmony = Puppetry.harmonyFactor(scores().harmony());
		if (harmony > 1 && !isStill() && source.getEntity() != null && random.nextDouble() < harmony - 1) {
			playSound(SoundEvents.PLAYER_ATTACK_WEAK, 0.6f, 1.6f);
			return false;
		}
		float before = getHealth();
		boolean hurt = super.hurt(source, amount);
		if (!hurt || !isAlive()) return hurt;
		if (soul != null) soul.mark(PuppetSoul.Trait.STEADFAST, (before - getHealth()) * PuppetSouls.MARK_HARM);
		lostSinceCut += before - getHealth();
		int toCut = metal ? 1 : 0;
		float fifth = getMaxHealth() / 5;
		while (fifth > 0 && lostSinceCut >= fifth) {
			lostSinceCut -= fifth;
			toCut++;
		}
		for (int i = 0; i < toCut; i++) cut(known);
		return true;
	}

	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
		super.dropCustomDeathLoot(source, looting, recentlyHit);
		wreck();
	}

	/** Its wreck: its core, some of its body's matter, and whatever it carried, where it fell. */
	private void wreck() {
		if (!core.isEmpty()) spawnAtLocation(core.copy());
		core = ItemStack.EMPTY;
		// A frame leaves some of its matter; a body of flesh its bones (phase 2b).
		spawnAtLocation(dead != null ? new ItemStack(net.minecraft.world.item.Items.BONE, Corpses.BONES_DROPPED)
				: new ItemStack(ArtifactRecipes.frameBody(grade), isBee() ? 1 : 2));
		for (int i = 0; i < cargo.getContainerSize(); i++) if (!cargo.getItem(i).isEmpty()) spawnAtLocation(cargo.removeItemNoUpdate(i));
		if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.POOF, getX(), getY(0.5), getZ(), 12, 0.3, 0.4, 0.3, 0.02);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level().isClientSide) return;
		// A General's ranks spill out where it fell (phase 2c); a player's soul in it goes on (phase 2d).
		int spilled = deploy();
		PuppetPlayers.lostWith(this, ModLang.PUPPET_SOUL_WRECKED);
		// Wrecked by another than its puppeteer (phase 3: knowing the enemy's circuits): its design theirs to know, its diagram in the wreck.
		Entity slayer = source.getEntity();
		ServerPlayer learner = null;
		if (slayer instanceof ServerPlayer player) learner = player;
		else if (slayer instanceof PuppetEntity puppet && puppet.getOwner() instanceof ServerPlayer puppeteer) learner = puppeteer;
		if (learner != null && !learner.getUUID().equals(getOwnerUUID())) Puppetry.learn(learner, this);
		forget();
		if (getOwner() instanceof ServerPlayer owner) {
			owner.sendSystemMessage(Component.translatable(ModLang.PUPPET_WRECKED, getDisplayName(), blockPosition().getX(), blockPosition().getY(),
					blockPosition().getZ()).withStyle(ChatFormatting.RED));
			if (spilled > 0) owner.sendSystemMessage(Component.translatable(ModLang.GENERAL_FELL, getDisplayName(), spilled).withStyle(ChatFormatting.GOLD));
		}
	}

	/** Taken apart by its puppeteer: its frame (its drawing kept) and its core given back where it stands, whatever it carried with them. */
	public void dismantle() {
		if (!(level() instanceof ServerLevel)) return;
		deploy(); // a General's ranks set down first (phase 2c)
		// A soul is never an item: taken apart, the puppet lets its soul go (phase 2a); a player's goes on (phase 2d).
		PuppetPlayers.lostWith(this, ModLang.PUPPET_SOUL_DISMANTLED);
		if (soul != null && getOwner() instanceof ServerPlayer owner) {
			owner.sendSystemMessage(Component.translatable(ModLang.SOUL_FREED_DISMANTLED, getDisplayName(), soul.name).withStyle(ChatFormatting.GRAY));
		}
		ItemStack frame = PuppetFrameItem.of(blueprint(), grade, quality, seed, strokes);
		// A spring poured into it stays in its frame (phase 3): sealed again, it mends itself again.
		if (springFed) frame.getOrCreateTag().putBoolean(SPRING_FED, true);
		// A body of flesh comes back as its body (phase 2b): who it was, its meridians, refined.
		if (dead != null) {
			frame.getOrCreateTag().put(PuppetBodies.BODY, dead.save());
			if (meridians != null) frame.getOrCreateTag().putByteArray(PuppetBodies.MERIDIANS, PuppetBodies.bytes(meridians));
			frame.getOrCreateTag().putInt(PuppetBodies.REFINED, PuppetBodies.REFINE_TICKS);
		}
		spawnAtLocation(frame);
		if (!core.isEmpty()) spawnAtLocation(core.copy());
		for (int i = 0; i < cargo.getContainerSize(); i++) if (!cargo.getItem(i).isEmpty()) spawnAtLocation(cargo.removeItemNoUpdate(i));
		playSound(SoundEvents.ARMOR_EQUIP_CHAIN, 1.0f, 0.8f);
		forget();
		discard();
	}

	// --- Its puppeteer's hand on it ---

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!isOwnedBy(player)) return InteractionResult.PASS;
		// A Spring of the Ancients poured into it (phase 3, self-repair): it mends its own cut lines from now on, a line every 20 seconds.
		if (held.getItem() instanceof SpringOfTheAncientsItem) {
			if (level().isClientSide) return InteractionResult.SUCCESS;
			if (springFed) {
				((ServerPlayer) player).displayClientMessage(Component.translatable(ModLang.PUPPET_SPRING_ALREADY, getDisplayName()).withStyle(ChatFormatting.GRAY), true);
				return InteractionResult.CONSUME;
			}
			springFed = true;
			mendTimer = 0;
			if (!player.getAbilities().instabuild) {
				held.shrink(1);
				// The bottle back: in the hand it was poured from if that is empty now, else in the pack.
				ItemStack bottle = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
				if (held.isEmpty()) player.setItemInHand(hand, bottle);
				else if (!player.getInventory().add(bottle)) player.drop(bottle, false);
			}
			playSound(SoundEvents.BOTTLE_EMPTY, 1.0f, 0.9f);
			if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.SPLASH, getX(), getY(0.7), getZ(), 20, 0.25, 0.3, 0.25, 0.05);
			((ServerPlayer) player).sendSystemMessage(Component.translatable(ModLang.PUPPET_SPRING_POURED, getDisplayName(), Puppetry.SPRING_MEND_TICKS / 20)
					.withStyle(ChatFormatting.AQUA));
			return InteractionResult.CONSUME;
		}
		boolean brush = held.is(ModItems.INSCRIPTION_BRUSH), matter = held.is(ArtifactRecipes.frameBody(frameGrade()));
		if (!brush && !matter) return InteractionResult.PASS;
		if (level().isClientSide) return InteractionResult.SUCCESS;
		if (brush) {
			PuppetBoards.openPuppet((ServerPlayer) player, this);
			return InteractionResult.CONSUME;
		}
		if (getHealth() >= getMaxHealth()) return InteractionResult.PASS;
		heal(getMaxHealth() * 0.25f);
		if (!player.getAbilities().instabuild) held.shrink(1);
		playSound(isBee() ? SoundEvents.COPPER_PLACE : grade == ArtifactGrade.MORTAL ? SoundEvents.WOOD_PLACE : SoundEvents.ANVIL_USE, 0.8f, 1.2f);
		remember();
		return InteractionResult.CONSUME;
	}

	// --- The ledger ---

	/** Its puppeteer's ledger told how it stands now (a player's puppet). */
	public void remember() {
		if (!(level() instanceof ServerLevel level) || !ownerIsPlayer || getOwnerUUID() == null || !isAlive()) return;
		Puppetry.Ledger.get(level.getServer()).put(getOwnerUUID(), new Puppetry.Entry(getUUID(), getDisplayName().getString(), blueprint(), grade,
				rank, order.ordinal(), getHealth(), getMaxHealth(), level.dimension(), blockPosition(), isInert(), false));
	}

	/** Taken off its puppeteer's ledger: wrecked, dismantled, or seized. */
	private void forget() {
		if (level() instanceof ServerLevel level && getOwnerUUID() != null) Puppetry.Ledger.get(level.getServer()).remove(getOwnerUUID(), getUUID());
	}

	// --- Saving ---

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (getOwnerUUID() != null) tag.putUUID("PuppetOwner", getOwnerUUID());
		tag.putString("PuppetOwnerName", ownerName);
		tag.putBoolean("PuppetOwnerPlayer", ownerIsPlayer);
		tag.putByte("FrameGrade", (byte) grade.ordinal());
		tag.putByte("FrameQuality", (byte) quality.ordinal());
		tag.putLong("FrameSeed", seed);
		tag.putByteArray("Circuit", strokes);
		tag.putIntArray("Cuts", cuts.toIntArray());
		if (!core.isEmpty()) tag.put("Core", core.save(new CompoundTag()));
		tag.putByte("Order", (byte) order.ordinal());
		if (post != null) tag.putLong("Post", post.asLong());
		if (carryFrom != null) tag.putLong("CarryFrom", carryFrom.asLong());
		if (carryTo != null) tag.putLong("CarryTo", carryTo.asLong());
		ListTag carried = new ListTag();
		for (int i = 0; i < cargo.getContainerSize(); i++) if (!cargo.getItem(i).isEmpty()) carried.add(cargo.getItem(i).save(new CompoundTag()));
		tag.put("Cargo", carried);
		tag.putInt("Heat", heat);
		tag.putInt("Charges", charges);
		tag.putFloat("LostSinceCut", lostSinceCut);
		// Its cut Wood lines growing back: cell, stroke, ticks left, three to one.
		IntArrayList growing = new IntArrayList();
		for (int[] r : regrowing) growing.addAll(it.unimi.dsi.fastutil.ints.IntList.of(r[0], r[1], Math.max(0, r[2] - tickCount)));
		tag.putIntArray("Regrowing", growing.toIntArray());
		if (courier != null) {
			tag.put("Courier", courier);
			tag.putInt("CourierLeave", courierLeave);
		}
		if (soul != null) tag.put("Soul", soul.save());
		if (dead != null) tag.put("Body", dead.save());
		if (meridians != null) tag.putByteArray("Meridians", PuppetBodies.bytes(meridians));
		// Its ranks of command (phase 2c).
		if (sealGrade != null) tag.putByte("SealGrade", (byte) sealGrade.ordinal());
		if (commander != null) {
			tag.putUUID("Commander", commander);
			tag.putString("CommanderName", commanderName);
		}
		tag.putInt("SubNonce", subNonce);
		if (general) {
			tag.putBoolean("General", true);
			ListTag held = new ListTag();
			held.addAll(ranks);
			tag.put("Ranks", held);
		}
		// A player's soul in it (phase 2d).
		tag.putBoolean("Released", released);
		tag.putBoolean("Struggling", struggling);
		tag.putInt("StruggleTicks", struggleTicks);
		// What its cut cells held (phase 3): cell, stroke, two to one; a spring in it, its fuse.
		IntArrayList was = new IntArrayList();
		cutWas.int2IntEntrySet().forEach(e -> was.addAll(it.unimi.dsi.fastutil.ints.IntList.of(e.getIntKey(), e.getIntValue())));
		tag.putIntArray("CutWas", was.toIntArray());
		tag.putBoolean(SPRING_FED, springFed);
		tag.putInt("MendTimer", mendTimer);
		tag.putInt("Fuse", fuse);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setOwner(tag.hasUUID("PuppetOwner") ? tag.getUUID("PuppetOwner") : null, tag.getString("PuppetOwnerName"),
				!tag.contains("PuppetOwnerPlayer") || tag.getBoolean("PuppetOwnerPlayer"));
		cuts.clear();
		for (int cell : tag.getIntArray("Cuts")) cuts.add(cell);
		order = Order.byIndex(tag.getByte("Order"));
		post = tag.contains("Post") ? BlockPos.of(tag.getLong("Post")) : null;
		carryFrom = tag.contains("CarryFrom") ? BlockPos.of(tag.getLong("CarryFrom")) : null;
		carryTo = tag.contains("CarryTo") ? BlockPos.of(tag.getLong("CarryTo")) : null;
		cargo.clearContent();
		int slot = 0;
		for (Tag t : tag.getList("Cargo", Tag.TAG_COMPOUND)) if (slot < cargo.getContainerSize()) cargo.setItem(slot++, ItemStack.of((CompoundTag) t));
		heat = tag.getInt("Heat");
		lostSinceCut = tag.getFloat("LostSinceCut");
		regrowing.clear();
		int[] growing = tag.getIntArray("Regrowing");
		for (int k = 0; k + 2 < growing.length; k += 3) regrowing.add(new int[] {growing[k], growing[k + 1], tickCount + growing[k + 2]});
		courier = tag.contains("Courier") ? tag.getCompound("Courier") : null;
		courierLeave = tag.getInt("CourierLeave");
		// Its soul before its board is weighed: what the soul gives goes into its strength.
		soul = tag.contains("Soul", Tag.TAG_COMPOUND) ? PuppetSoul.load(tag.getCompound("Soul")) : null;
		showSoul();
		// A body of flesh: who it was and its meridians (phase 2b), before its board is weighed.
		setBody(tag.contains("Body", Tag.TAG_COMPOUND) ? PuppetBodies.Dead.load(tag.getCompound("Body")) : null,
				tag.contains("Meridians") ? PuppetBodies.mask(tag.getByteArray("Meridians")) : null);
		// Its ranks of command (phase 2c), before its board is weighed (a grown mind's bent among what it gives).
		sealGrade = tag.contains("SealGrade") ? ArtifactGrade.byIndex(tag.getByte("SealGrade")) : null;
		commander = tag.hasUUID("Commander") ? tag.getUUID("Commander") : null;
		commanderName = commander == null ? "" : tag.getString("CommanderName");
		subNonce = tag.getInt("SubNonce");
		general = tag.getBoolean("General");
		ranks.clear();
		for (Tag t : tag.getList("Ranks", Tag.TAG_COMPOUND)) ranks.add((CompoundTag) t);
		released = tag.getBoolean("Released");
		struggling = tag.getBoolean("Struggling");
		struggleTicks = tag.getInt("StruggleTicks");
		cutWas.clear();
		int[] was = tag.getIntArray("CutWas");
		for (int k = 0; k + 1 < was.length; k += 2) cutWas.put(was[k], was[k + 1]);
		springFed = tag.getBoolean(SPRING_FED);
		mendTimer = tag.getInt("MendTimer");
		fuse = tag.contains("Fuse") ? tag.getInt("Fuse") : -1;
		setup(ArtifactGrade.byIndex(tag.getByte("FrameGrade")), Artifacts.Quality.byIndex(tag.getByte("FrameQuality")), tag.getLong("FrameSeed"),
				tag.getByteArray("Circuit"), tag.contains("Core") ? ItemStack.of(tag.getCompound("Core")) : ItemStack.EMPTY);
		charges = Math.min(tag.getInt("Charges"), maxCharges());
	}

	@Override
	public boolean removeWhenFarAway(double distance) { return !ownerIsPlayer; }

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
		// A bee never falls hard; Water in a puppet's body lands it as softly as a fall into water.
		return !isBee() && bodyVeins(CircuitBoard.Phase.WATER) == 0 && super.causeFallDamage(distance, multiplier, source);
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
		if (!isBee()) super.checkFallDamage(y, onGround, state, pos);
	}

	@Override
	public boolean canBeLeashed(Player player) { return false; }

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return grade == ArtifactGrade.MORTAL ? SoundEvents.WOOD_HIT : SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return grade == ArtifactGrade.MORTAL ? SoundEvents.WOOD_BREAK : SoundEvents.IRON_GOLEM_DEATH;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return isBee() && !isStill() ? SoundEvents.BEE_LOOP : null;
	}

	@Override
	protected float getSoundVolume() {
		return isBee() ? 0.35f : 1.0f;
	}

	// --- Its animations ---

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), WALK = RawAnimation.begin().thenLoop("walk"),
			FLY = RawAnimation.begin().thenLoop("fly"), SLUMP = RawAnimation.begin().thenLoop("inert");

	/**
	 * Its animations (geo/puppet: tools/spell-art, Puppets): the humanoid's idle and walk, the bee's flight; slumped while inert, frozen or
	 * stalled; its blow (the humanoid's "strike", the bee's "sting") and its art ("beam") called as they happen.
	 */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		AnimationController<PuppetEntity> main = new AnimationController<>(this, "main", 4, state -> {
			if (isStill()) return state.setAndContinue(SLUMP);
			if (isBee()) return state.setAndContinue(FLY);
			return state.setAndContinue(state.isMoving() ? WALK : IDLE);
		});
		main.triggerableAnim("strike", RawAnimation.begin().thenPlay("strike"));
		main.triggerableAnim("sting", RawAnimation.begin().thenPlay("sting"));
		main.triggerableAnim("beam", RawAnimation.begin().thenPlay("beam"));
		controllers.add(main);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

	// --- Its goals ---

	/** Inert, frozen or stalled: it does nothing at all. */
	private class Still extends Goal {
		Still() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP, Flag.TARGET));
		}

		@Override
		public boolean canUse() { return isStill(); }

		@Override
		public void start() {
			getNavigation().stop();
		}
	}

	/** At its foe: its art from afar while it is ready, its blows close by; a holding puppet never leaves its place. */
	private class Fight extends Goal {
		private int repath;

		Fight() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return target != null && target.isAlive() && reaction <= 0;
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (target == null) return;
			getLookControl().setLookAt(target, 30, 30);
			double distance = distanceTo(target);
			double reach = getBbWidth() * 0.9 + target.getBbWidth() + 1.0;
			if (artReady() && distance > 3 && distance <= ART_RANGE && hasLineOfSight(target)) {
				fireArt(target);
				return;
			}
			// A cultivator's soul: one of its own arts now and then (phase 2a).
			if (soul != null && !soul.arts.isEmpty() && soulArtWait <= 0 && distance <= PuppetSouls.ART_REACH && hasLineOfSight(target)) {
				castSoulArt(target);
				return;
			}
			boolean holding = order == Order.HOLD;
			// Its place in a ring round its foe (phase 3: PuppetFormations): there, not straight at it, striking as it can reach.
			Vec3 spot = formationSpot;
			if (spot != null && !holding) {
				if (position().distanceToSqr(spot) > 0.36) {
					if (--repath <= 0) {
						repath = 5;
						getNavigation().moveTo(spot.x, isBee() ? target.getY(0.5) : spot.y, spot.z, 1.2);
					}
				} else {
					getNavigation().stop();
				}
				if (distance <= reach && attackCooldown <= 0) {
					doHurtTarget(target);
					attackCooldown = (int) Math.round(20 / Puppetry.harmonyFactor(scores().harmony()));
				}
				return;
			}
			// Its Gale Stair: a foe out of its reach but within a dash, while the stair is ready (never one holding its place).
			if (stair && dashWait <= 0 && dashTicks <= 0 && !holding && distance > reach + 1.5 && distance <= reach + Puppetry.STAIR_REACH
					&& hasLineOfSight(target)) {
				startDash(target, reach);
				return;
			}
			if (distance > reach) {
				if (!holding && --repath <= 0) {
					repath = 8;
					getNavigation().moveTo(isBee() ? target.getX() : target.getX(), isBee() ? target.getY(0.5) : target.getY(), target.getZ(), 1.1);
				}
			} else {
				getNavigation().stop();
				if (attackCooldown <= 0) {
					doHurtTarget(target);
					attackCooldown = (int) Math.round(20 / Puppetry.harmonyFactor(scores().harmony()));
				}
			}
		}
	}

	/** A bee at work: from the chest marked first to the other, a load at a time; Heaven grade and above stepping between them. */
	private class Carry extends Goal {
		Carry() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return isBee() && order == Order.WORK && carryFrom != null && carryTo != null && getTarget() == null;
		}

		@Override
		public void tick() {
			if (carryWait > 0) {
				carryWait--;
				return;
			}
			boolean loaded = !cargo.isEmpty();
			BlockPos chest = loaded ? carryTo : carryFrom;
			if (chest == null || !level().isLoaded(chest)) return;
			Vec3 above = Vec3.atCenterOf(chest).add(0, 1.0, 0);
			if (position().distanceToSqr(above) > 2.25) {
				if (grade.ordinal() >= ArtifactGrade.HEAVEN.ordinal()) {
					step(above);
					carryWait = 10;
				} else {
					getNavigation().moveTo(above.x, above.y, above.z, 1.0);
				}
				return;
			}
			getNavigation().stop();
			Container container = HopperBlockEntity.getContainerAt(level(), chest);
			if (container == null) {
				if (getOwner() instanceof ServerPlayer owner) {
					owner.displayClientMessage(Component.translatable(ModLang.PUPPET_CHEST_GONE, getDisplayName()).withStyle(ChatFormatting.GOLD), false);
				}
				command(Order.HOLD, null, null);
				return;
			}
			if (loaded) {
				for (int i = 0; i < cargo.getContainerSize(); i++) {
					ItemStack stack = cargo.getItem(i);
					if (stack.isEmpty()) continue;
					cargo.setItem(i, HopperBlockEntity.addItem(null, container, stack, null));
				}
				if (!cargo.isEmpty()) carryWait = 40; // the chest is full: it waits
			} else {
				int most = Math.max(1, (int) Math.round(CARGO[grade.ordinal()] * cargoFactor));
				if (dead("left_leg")) most = Math.max(1, most / 2);
				if (dead("right_leg")) most = Math.max(1, most / 2);
				int taken = 0;
				for (int i = 0; i < container.getContainerSize() && taken < most; i++) {
					ItemStack stack = container.getItem(i);
					if (stack.isEmpty()) continue;
					cargo.addItem(container.removeItem(i, stack.getCount()));
					taken++;
				}
				container.setChanged();
				if (taken == 0) carryWait = 100; // nothing to carry: it waits by the chest
			}
			playSound(SoundEvents.BUNDLE_INSERT, 0.6f, 1.2f);
		}

		/** A step through space, to {@code at}. */
		private void step(Vec3 at) {
			if (level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.PORTAL, getX(), getY(0.5), getZ(), 16, 0.3, 0.3, 0.3, 0.2);
				teleportTo(at.x, at.y, at.z);
				level.sendParticles(ParticleTypes.PORTAL, at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.2);
				playSound(SoundEvents.ENDERMAN_TELEPORT, 0.4f, 1.6f);
			}
		}
	}

	/** Ticks a following puppet far behind its puppeteer finds no way to them before it steps beside them. */
	private static final int STUCK_TICKS = 60;

	/** With nothing to fight or carry: after its puppeteer (running, stepping to them only when stuck far behind), back to its post, or where it was told to hold. */
	private class Keep extends Goal {
		private int repath, stuck;

		Keep() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return getTarget() == null && !(isBee() && order == Order.WORK && carryFrom != null && carryTo != null);
		}

		@Override
		public void tick() {
			switch (order) {
				case FOLLOW -> follow();
				case GUARD, HOLD, WORK -> {
					if (post == null) return;
					// A guarding bee hovers a block over its post (its puppeteer's feet when told); a holding one stays where it was.
					Vec3 at = Vec3.atBottomCenterOf(post).add(0, isBee() && order == Order.GUARD ? 1 : 0, 0);
					double keep = order == Order.GUARD ? 2.5 : 1.0;
					if (position().distanceToSqr(at) > keep * keep && --repath <= 0) {
						repath = 20;
						getNavigation().moveTo(at.x, at.y, at.z, 1.0);
					}
				}
			}
		}

		private void follow() {
			// After its puppeteer, or the holder of its Sub-Seal (phase 2c) while they are in its world.
			LivingEntity owner = leader();
			if (owner == null || owner.level() != level() || owner.isSpectator()) return;
			double distance = distanceTo(owner);
			// Out of its puppeteer's reach (by their rank and its Clarity; a Sub-Seal lends their reach too), it hears no more and stands
			// where it is; unless its linked fellows pass their word on to it (phase 3: PuppetCommands#relayed, weighed once a second).
			if (owner instanceof ServerPlayer && ownerIsPlayer && getOwnerUUID() != null && level() instanceof ServerLevel server
					&& distance > Puppetry.reach(CultivationManager.own(server.getServer(), getOwnerUUID())) * Puppetry.clarityReach(scores().clarity()) * reachFactor
					&& !relayedFrom(server)) {
				getNavigation().stop();
				return;
			}
			// It runs to its puppeteer (the user, 2026-11-17: not a step through space when told to follow), the farther the swifter; only
			// stuck far behind with no way to them for a while does it step beside them. Where it looks is its looking goals' (a second
			// look here each tick fought them, and its head flicked between the two).
			if (distance > 5) {
				if (--repath <= 0) {
					repath = 10;
					boolean found = getNavigation().moveTo(owner.getX(), isBee() ? owner.getY() + 1.5 : owner.getY(), owner.getZ(), distance > 12 ? 1.5 : 1.2);
					stuck = found ? 0 : stuck + repath;
				}
				if (distance > 16 && getNavigation().isDone()) stuck++;
				if (stuck >= STUCK_TICKS) {
					stuck = 0;
					stepTo(owner);
				}
			} else {
				stuck = 0;
				getNavigation().stop();
			}
		}

		/** Steps beside its puppeteer, on ground that holds it (a bee anywhere there is room). */
		private void stepTo(LivingEntity owner) {
			BlockPos base = owner.blockPosition();
			for (int tries = 0; tries < 12; tries++) {
				int dx = random.nextInt(7) - 3, dz = random.nextInt(7) - 3, dy = isBee() ? 1 + random.nextInt(2) : random.nextInt(3) - 1;
				if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
				BlockPos at = base.offset(dx, dy, dz);
				if (!isBee() && !level().getBlockState(at.below()).isFaceSturdy(level(), at.below(), net.minecraft.core.Direction.UP)) continue;
				Vec3 was = position();
				moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, getYRot(), getXRot());
				if (level().noCollision(PuppetEntity.this)) {
					getNavigation().stop();
					return;
				}
				moveTo(was.x, was.y, was.z, getYRot(), getXRot());
			}
		}
	}
}
