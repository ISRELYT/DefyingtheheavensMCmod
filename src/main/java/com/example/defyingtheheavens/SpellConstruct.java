package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;

/**
 * A great spell's living shape (the realms past Four Axis, 2026-10-24: the user's "use geckolib for animations and also use unique and
 * well-crafted detailed models"): a spirit beast of an element's qi, a tree, a seal, a whirlpool, a vortex of swords, a procession and a
 * gate of the dead, an avatar of heaven and demon and its hands, each its own GeckoLib model, animations and glow (tools/spell-art; drawn
 * translucent and full bright: SpellConstructRenderer). Its spell's lingering
 * effect moves and turns it and calls its moves ({@link #act}); the construct is only the body, as a SpellField is. Never saved.
 * <p>
 * From Void Shattering on, space itself is torn open too (the user's ask, 2026-10-24: "make sure ... the rifts and tears/cracks in space
 * looks cool"): a construct of a {@link Form} other than a model's is a tear in space, drawn by the client's own hand (RiftRenderer: its
 * jagged rim burning, the void and its stars within, cracks running out from it like cracks in glass, shards of space breaking away),
 * opening as it forms and closing as it goes, glowing the colour of what comes through it ({@link #tint}).
 */
public class SpellConstruct extends Entity implements GeoEntity {
	/**
	 * How it is drawn: a GeckoLib model ({@link #SHAPE}); or space torn open, drawn by the client's own hand (RiftRenderer), its size the
	 * tear's length in blocks and its glow its {@link #tint}: a jagged {@link #TEAR} standing in the air (turned to whoever looks at it,
	 * upright, or turned on their screen by its yaw: 90, a slash lying across the sky), an
	 * {@link #ABYSS} lying open in the ground, a {@link #LINE} cut along its facing from where it stands, or space {@link #SHATTER}ed like
	 * glass round a point; or a {@link #TENDRIL}, a root drawn by hand too (TendrilRenderer), lashing from where it stands to the being it
	 * holds ({@link #target}) and coiled round it, its size its girth.
	 */
	public enum Form { SHAPE, TEAR, ABYSS, LINE, SHATTER, TENDRIL }

	/**
	 * What it is: its model's id (geo/spell/&lt;id&gt;.geo.json, its texture and animations alike), the move it enters with (or null),
	 * the one it loops, the moves its spell may call, and how many times its model's size it is drawn; or a tear in space ({@link Form}),
	 * which has none of these. Synced by ordinal: add at the end.
	 */
	public enum Kind {
		AZURE_DRAGON("azure_dragon", null, "swim", 4.0f, "roar"),
		DAO_TREE("dao_tree", "grow", "sway", 4.0f, "wither"),
		VERMILION_BIRD("vermilion_bird", null, "glide", 4.0f, "dive"),
		ONE_FLAME_SEED("one_flame_seed", null, "spin", 2.0f, "bloom"),
		ONE_FLAME_PILLAR("one_flame_pillar", "erupt", "burn", 4.0f),
		QILIN("qilin", null, "gallop", 4.0f, "rear"),
		MOUNTAIN_SEAL("mountain_seal", null, "fall", 4.0f, "stamp"),
		BLACK_TORTOISE("black_tortoise", "rise", "idle", 4.0f, "strike"),
		HUNDRED_RIVERS("hundred_rivers", null, "spin", 4.0f, "burst"),
		WHITE_TIGER("white_tiger", null, "prowl", 4.0f, "pounce", "claw"),
		MYRIAD_SWORDS("myriad_swords", null, "orbit", 4.0f, "merge"),
		COLOSSAL_SWORD("colossal_sword", null, "idle", 4.0f),
		// Dao Integration's ghost foundation's (GhostSpells) and its spell of both paths (HeavenDemonSpells).
		PROCESSION("procession", "pour", "march", 4.0f),
		GHOST_GATE("ghost_gate", "open", "draw", 4.0f, "slam"),
		HEAVEN_DEMON_AVATAR("heaven_demon_avatar", "rise", "loom", 4.0f, "strike_palm", "strike_claw"),
		AVATAR_PALM("avatar_palm", "fall", "press", 4.0f),
		AVATAR_CLAW("avatar_claw", "fall", "press", 4.0f),
		// Void Shattering's (2026-10-24): the elements' (their kits), the ghost foundation's (GhostSpells) and both paths' (HeavenDemonSpells)...
		AZURE_THUNDER_CLOUD("azure_thunder_cloud", "gather", "churn", 4.0f, "flash"),
		VOID_ROOTS("void_roots", "descend", "drink", 4.0f, "withdraw"),
		GOLDEN_CROW("golden_crow", "emerge", "dive", 4.0f, "strike"),
		VOID_SUN("void_sun", "emerge", "blaze", 4.0f, "collapse"),
		BUZHOU_PILLAR("buzhou_pillar", null, "fall", 4.0f, "impact"),
		EARTH_VEIN_LOCK("earth_vein_lock", "rise", "hold", 4.0f, "release"),
		WEAK_WATER_FALL("weak_water_fall", "pour", "fall", 4.0f, "dry"),
		KUN("kun", null, "swim", 4.0f),
		SEVERING_LIGHT("severing_light", null, "thrust", 4.0f),
		SWORD_PRISON("sword_prison", "descend", "hold", 4.0f, "converge"),
		NINE_NETHERS("nine_nethers", "open", "turn", 4.0f, "seal"),
		NAIHE_BRIDGE("naihe_bridge", "form", "stand", 4.0f),
		HEAVEN_DEMON_FIST("heaven_demon_fist", null, "fly", 4.0f, "impact"),
		// ...and the tears in space they come through (Form).
		VOID_RIFT("void_rift", Form.TEAR),
		ABYSS_RIFT("abyss_rift", Form.ABYSS),
		RIFT_LINE("rift_line", Form.LINE),
		SHATTERED_SPACE("shattered_space", Form.SHATTER),
		// The void tree's grasping roots (Void Withering, 2026-10-24: the user's ask, roots that reach out and seize).
		VOID_TENDRIL("void_tendril", Form.TENDRIL);

		public final String id, enter, loop;
		public final float scale;
		public final String[] moves;
		public final Form form;

		Kind(String id, String enter, String loop, float scale, String... moves) {
			this.id = id;
			this.enter = enter;
			this.loop = loop;
			this.scale = scale;
			this.moves = moves;
			this.form = Form.SHAPE;
		}

		/** A tear in space, or a tendril: no model, no moves; its size a tear's length in blocks, a tendril's girth. */
		Kind(String id, Form form) {
			this.id = id;
			this.enter = null;
			this.loop = null;
			this.scale = 1.0f;
			this.moves = new String[0];
			this.form = form;
		}

		/** One of GeckoLib's models (not a tear in space, which the client draws by its own hand). */
		public boolean modelled() { return form == Form.SHAPE; }

		public static Kind byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : AZURE_DRAGON;
		}
	}

	/** Ticks it takes to fade in as it forms and out as it goes (SpellConstructRenderer). */
	public static final int FADE = 10;
	/** Ticks its animations ease from one into the next. */
	private static final int EASE = 3;

	private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.BYTE);
	/** Drawn this much larger (or smaller) than its kind's own size. */
	private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.FLOAT);
	/** How long it lasts in all, in ticks (it fades in over its first ticks and out over its last: SpellConstructRenderer). */
	private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	/** Its caster's entity id (0: none). */
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	/** A tear in space's glow (0xRRGGBB: what comes through it; 0, the void's own violet). */
	private static final EntityDataAccessor<Integer> TINT = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);
	/** The entity id of the being a tendril holds (0: none). */
	private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(SpellConstruct.class, EntityDataSerializers.INT);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	/** Whether it is fading away (its spell done with it): it goes when its life is out. */
	private boolean dissolving;

	public SpellConstruct(EntityType<? extends SpellConstruct> type, Level level) {
		super(type, level);
		noPhysics = true;
		noCulling = true;
	}

	/**
	 * A construct of {@code kind} at {@code at}, facing {@code yaw} and {@code pitch} (degrees), lasting {@code life} ticks: its spell's
	 * whole time, so it has faded out by the time its spell {@link #dissolve dissolves} it.
	 */
	public static SpellConstruct create(Level level, Kind kind, Vec3 at, float yaw, float pitch, int life, Entity owner) {
		SpellConstruct construct = new SpellConstruct(ModEntities.SPELL_CONSTRUCT, level);
		construct.entityData.set(KIND, (byte) kind.ordinal());
		construct.entityData.set(LIFE, life);
		construct.entityData.set(OWNER, owner == null ? 0 : owner.getId());
		construct.moveTo(at.x, at.y, at.z, yaw, pitch);
		return construct;
	}

	/** Adds it to its level. */
	public SpellConstruct spawn() {
		level().addFreshEntity(this);
		return this;
	}

	public SpellConstruct size(float size) { entityData.set(SIZE, size); return this; }

	/**
	 * Drawn as great as {@code spell}'s realm (Spell#realm: an element's, a ghost foundation's, both paths') makes a spell beside one of Four
	 * Axis (HeavenSpells#greatness), as its reach and areas are.
	 */
	public SpellConstruct sizedFor(Spell spell) { return size((float) HeavenSpells.greatness(spell.realm())); }
	public SpellConstruct life(int life) { entityData.set(LIFE, life); return this; }
	/** A tear in space glowing {@code rgb} (0xRRGGBB): the colour of what comes through it. */
	public SpellConstruct tint(int rgb) { entityData.set(TINT, rgb & 0xFFFFFF); return this; }

	/** A tendril holding {@code held} (its watchers draw it reaching to it, wherever it goes). */
	public SpellConstruct target(Entity held) { entityData.set(TARGET, held == null ? 0 : held.getId()); return this; }

	public Kind kind() { return Kind.byIndex(entityData.get(KIND)); }
	public float size() { return entityData.get(SIZE); }
	public int life() { return entityData.get(LIFE); }
	public int tint() { return entityData.get(TINT); }

	/** The being a tendril holds, or null. */
	public Entity target() {
		int id = entityData.get(TARGET);
		return id <= 0 ? null : level().getEntity(id);
	}

	public Entity owner() {
		int id = entityData.get(OWNER);
		return id <= 0 ? null : level().getEntity(id);
	}

	/** Moves it to {@code at}, facing {@code yaw} and {@code pitch} (its spell does, tick by tick; its watchers' copies follow). */
	public void place(Vec3 at, float yaw, float pitch) {
		setPos(at.x, at.y, at.z);
		setYRot(yaw);
		setXRot(pitch);
	}

	/** Plays one of its kind's moves once ({@link Kind#moves}: a roar, a dive, a strike), on every watcher's screen. */
	public void act(String move) {
		triggerAnim("main", move);
	}

	/**
	 * Lets it go as its spell ends with it: it fades out over what is left of its last {@link #FADE} ticks and is gone (taken away at once,
	 * a shape still bright blinked out; one whose life its spell's own time spent is already faded, and goes now).
	 */
	public void dissolve() {
		if (isRemoved()) return;
		dissolving = true;
		entityData.set(LIFE, Math.min(life(), tickCount + FADE));
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(KIND, (byte) 0);
		entityData.define(SIZE, 1.0f);
		entityData.define(LIFE, 20);
		entityData.define(OWNER, 0);
		entityData.define(TINT, 0);
		entityData.define(TARGET, 0);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			if (!kind().modelled()) motes();
			return;
		}
		// Dissolved, it goes once faded (a tick or two after, its watchers' clocks a little behind); else its spell's end dissolves it, and
		// this is only lest one be left behind (its spell gone with the server or its caster).
		if (tickCount > life() + (dissolving ? 2 : 100)) discard();
	}

	/** The void's own violet: a tear's glow where its spell gives it none ({@link #tint}). */
	public static final int VOID = 0xB070FF;

	/**
	 * A tear in space's motes, on its watchers' screens while it stands open: sparks drawn in toward its heart (along its cut, out of its
	 * abyss, off its shattering) and wisps of its glow; more the greater it is.
	 */
	private void motes() {
		if (kind().form == Form.TENDRIL || tickCount < 6 || tickCount > life() - FADE) return; // a tendril's light is its own (TendrilRenderer)
		float length = size() * kind().scale;
		int rgb = tint() != 0 ? tint() : VOID;
		double r = (rgb >> 16 & 255) / 255.0, g = (rgb >> 8 & 255) / 255.0, b = (rgb & 255) / 255.0;
		net.minecraft.util.RandomSource random = level().random;
		Vec3 at = position();
		int count = Math.min(6, 1 + (int) (length / 14));
		for (int i = 0; i < count; i++) {
			Vec3 from, way;
			switch (kind().form) {
				case LINE -> {
					Vec3 along = Vec3.directionFromRotation(getXRot(), getYRot());
					Vec3 on = at.add(along.scale(random.nextDouble() * length));
					from = on.add(random.nextGaussian() * 1.2, random.nextGaussian() * 1.2, random.nextGaussian() * 1.2);
					way = on.subtract(from).scale(0.06);
				}
				case ABYSS -> {
					double angle = random.nextDouble() * Math.PI * 2, out = Math.sqrt(random.nextDouble()) * length * 0.4;
					from = at.add(Math.cos(angle) * out, 0.2, Math.sin(angle) * out);
					way = new Vec3(-Math.cos(angle) * 0.04, 0.12 + random.nextDouble() * 0.1, -Math.sin(angle) * 0.04);
				}
				case SHATTER -> {
					Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
					from = at.add(dir.scale(length * 0.1 + random.nextDouble() * length * 0.35));
					way = dir.scale(0.05 + random.nextDouble() * 0.08);
				}
				default -> {
					// A tear stands upright, its heart at its middle: sparks drawn into it from round about.
					Vec3 dir = new Vec3(random.nextGaussian() * 0.6, random.nextGaussian(), random.nextGaussian() * 0.6).normalize();
					from = at.add(dir.scale(length * (0.35 + random.nextDouble() * 0.3)));
					way = at.subtract(from).scale(0.045);
				}
			}
			level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, from.x, from.y, from.z, way.x, way.y, way.z);
			if (random.nextInt(2) == 0) level().addParticle(net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, from.x, from.y, from.z, r, g, b);
		}
	}

	/** Whether a move of its has played (on its watchers' screens; after one, only its loop is asked for). */
	private boolean entered;

	/**
	 * Its one controller: its enter move then its loop, and its moves when its spell calls them. It eases {@link #EASE} ticks into a move
	 * called (from wherever its loop had got to), and goes straight into all else: its first pose (easing into it from its model's rest
	 * showed a tree shrinking into its seedling and a seal's imprint into nothing as they formed), and its loop after its enter or a move
	 * (each ends where its loop begins: tools/spell-art, SpellArt#carryTheLoop; easing there only held it still a moment, its flames
	 * stopped). Once a move has played only the loop is asked for: GeckoLib reloads what is asked for whenever a called move ends, so
	 * asking for the enter and the loop still played the enter again after every move (a tortoise rising afresh after each strike of its
	 * serpent; all seen checking them in game, 2026-10-24).
	 */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		Kind kind = kind();
		if (!kind.modelled()) return; // a tear in space: drawn by the client's own hand, nothing of GeckoLib's to move
		RawAnimation loop = RawAnimation.begin().thenLoop(kind.loop), going = kind.enter == null ? loop : RawAnimation.begin().thenPlay(kind.enter).thenLoop(kind.loop);
		AnimationController<SpellConstruct> main = new AnimationController<>(this, "main", 0, state -> {
			AnimationProcessor.QueuedAnimation playing = state.getController().getCurrentAnimation();
			if (!entered && playing != null && !playing.animation().name().equals(kind.enter) && !playing.animation().name().equals(kind.loop)) entered = true;
			return state.setAndContinue(entered ? loop : going);
		}) {
			private boolean posed;

			@Override
			public void process(CoreGeoModel<SpellConstruct> model, AnimationState<SpellConstruct> state, Map<String, CoreGeoBone> bones, Map<String, BoneSnapshot> snapshots,
					double seekTime, boolean crashWhenCantFindBone) {
				// Chosen between transitions (one under way keeps its own: changed midway, GeckoLib eases from where it began).
				if (getAnimationState() != State.TRANSITIONING) transitionLength = posed && triggeredAnimation != null && !hasAnimationFinished() ? EASE : 0;
				super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
				if (getCurrentAnimation() != null) posed = true;
			}
		};
		for (String move : kind.moves) main.triggerableAnim(move, RawAnimation.begin().thenPlay(move));
		controllers.add(main);
		// Its spins on a controller of their own, from its first moment, turning on through its enter and every move (tools/spell-art
		// SpellArt#splitSpins: carried into a move, a wheel was eased back to its loop's start, whole turns backwards in a moment).
		RawAnimation spins = RawAnimation.begin().thenLoop(kind.loop + "_spin");
		controllers.add(new AnimationController<>(this, "spin", 0, state -> state.setAndContinue(spins)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) { return distance < 384 * 384; }

	@Override
	public boolean isPickable() { return false; }

	@Override
	public boolean isPushable() { return false; }

	@Override
	public boolean canBeCollidedWith() { return false; }

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
