package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The five elements' spells (Spell#isHeaven), cast by the living on qi alone (Spells#cast pays and checks): each element's fourteen
 * in a kit of its own ({@link WoodSpells}, {@link WaterSpells}, {@link FireSpells}, {@link EarthSpells}, {@link MetalSpells}),
 * and here what they share: whom a spell spares and whom it takes for a foe, the ways of finding a target, the element's qi harm
 * (of the spell's realm, for the wards: SpellAuras), effects that linger tick by tick ({@link Lingering}), blocks a spell sets down
 * for a while ({@link #placeTemporary}), always taken up again, and the ground a spell may carve for good ({@link #carvable}).
 * What a spell looks like is its own: its missiles (SpellProjectile), what it leaves standing (SpellField), blocks it moves before
 * your eyes (SpellBlock), its passing moments (SpellFx) and the auras it leaves on beings (SpellAuras).
 */
public final class HeavenSpells {
	/** Whether {@code spell} can take hold for {@code p} now (its element's say); refused, nothing is paid. */
	public static boolean canCast(ServerPlayer p, Spell spell) {
		return switch (spell.element()) {
			case WOOD -> WoodSpells.canCast(p, spell);
			case WATER -> WaterSpells.canCast(p, spell);
			case FIRE -> FireSpells.canCast(p, spell);
			case EARTH -> EarthSpells.canCast(p, spell);
			default -> MetalSpells.canCast(p, spell);
		};
	}

	/** Nothing for a spell to take hold of (no being, no ground looked at): said, and false, so nothing is paid. */
	public static boolean refuse(ServerPlayer p) {
		p.displayClientMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_SPELL_NO_HOLD), true);
		return false;
	}

	/** Casts {@code spell} (Spells#cast has paid for it). */
	public static void cast(ServerPlayer p, Spell spell) {
		if (!spell.isHeaven()) return;
		switch (spell.element()) {
			case WOOD -> WoodSpells.cast(p, spell);
			case WATER -> WaterSpells.cast(p, spell);
			case FIRE -> FireSpells.cast(p, spell);
			case EARTH -> EarthSpells.cast(p, spell);
			default -> MetalSpells.cast(p, spell);
		}
	}

	/** Every server tick: what lingers, the temporary blocks' time, each element's own. */
	public static void tick(MinecraftServer server) {
		WARDED_OFF.clear(); // a warded blow is refused in its own tick, or never reached the refusal (another check stopped it)
		if (!LINGERING.isEmpty()) {
			long now = server.overworld().getGameTime();
			// A copy: a lingering effect may set another lingering (a detonation's after-burn).
			List<Lingering> running = new ArrayList<>(LINGERING);
			for (Lingering l : running) {
				boolean on;
				try {
					on = l.level() != null && l.tick(now);
				} catch (Exception e) {
					DefyingTheHeavens.LOGGER.error("A lingering heaven spell failed", e);
					on = false;
				}
				if (!on) {
					LINGERING.remove(l);
					endQuietly(l);
				}
			}
		}
		if (!DUE.isEmpty()) {
			long now = server.overworld().getGameTime();
			while (!DUE.isEmpty() && DUE.peek().until() <= now) {
				Placed placed = DUE.poll();
				Map<BlockPos, Placed> here = TEMPORARY.get(placed.level());
				if (here == null || here.get(placed.pos()) != placed) continue; // taken up early, or set again since
				here.remove(placed.pos());
				if (here.isEmpty()) TEMPORARY.remove(placed.level());
				restore(placed);
			}
		}
		SpellAuras.tick(server);
		Devastation.tick(server);
		WoodSpells.tick(server);
		WaterSpells.tick(server);
		FireSpells.tick(server);
		EarthSpells.tick(server);
		MetalSpells.tick(server);
	}

	/** A player left: what their spells left lingering ends (its blocks taken up). */
	public static void forget(UUID id) {
		// Taken out first, then ended: an ending may set something else lingering.
		List<Lingering> theirs = new ArrayList<>();
		for (Lingering l : LINGERING) if (id.equals(l.caster())) theirs.add(l);
		LINGERING.removeAll(theirs);
		for (Lingering l : theirs) endQuietly(l);
		WoodSpells.forget(id);
		WaterSpells.forget(id);
		FireSpells.forget(id);
		EarthSpells.forget(id);
		MetalSpells.forget(id);
		HeavenPassives.forget(id);
		HeavenMethods.forget(id);
	}

	/** The server stopping: everything ends, every temporary block taken up. */
	public static void clear() {
		List<Lingering> all = new ArrayList<>(LINGERING);
		LINGERING.clear();
		for (Lingering l : all) endQuietly(l);
		LINGERING.clear(); // whatever an ending set lingering ends with the server too
		for (Map<BlockPos, Placed> here : TEMPORARY.values()) for (Placed placed : here.values()) restore(placed);
		TEMPORARY.clear();
		DUE.clear();
		Devastation.clear();
		SpellAuras.clear();
		WoodSpells.clear();
		WaterSpells.clear();
		FireSpells.clear();
		EarthSpells.clear();
		MetalSpells.clear();
	}

	// --- Lingering effects ---

	/**
	 * An effect a spell leaves running (a zone on the ground, a mark on a target, a ward round the caster, a detonation to come,
	 * blades circling): ticked every server tick until {@link #tick} says it is over, then {@link #end}ed.
	 */
	public interface Lingering {
		/** One tick at game time {@code now}; false once it is over. */
		boolean tick(long now);

		/** It is over (run out, or its caster gone, or the server stopping): take back what it set down. */
		default void end() {}

		/** Whose it is (forgotten with them), or null. */
		UUID caster();

		/** Where it runs. */
		ServerLevel level();
	}

	private static final List<Lingering> LINGERING = new ArrayList<>();

	/**
	 * A lingering effect of a caster's that runs for a while from now: the caster, the level it runs in, when it began and when it
	 * ends ({@link #end}, which an effect may move). Its {@link #tick} returns false past the end, or when {@link #run} does.
	 */
	public abstract static class Timed implements Lingering {
		protected final ServerPlayer caster;
		protected final ServerLevel level;
		protected final long start;
		protected long end;

		protected Timed(ServerPlayer caster, int ticks) {
			this.caster = caster;
			this.level = caster.serverLevel();
			this.start = level.getGameTime();
			this.end = start + ticks;
		}

		@Override
		public boolean tick(long now) {
			long at = level.getGameTime();
			return at <= end && run(at - start, at);
		}

		/** One tick, {@code age} ticks after it began; false once it is over. */
		protected abstract boolean run(long age, long now);

		@Override
		public UUID caster() { return caster.getUUID(); }

		@Override
		public ServerLevel level() { return level; }

		/** The caster still here: alive, not left, not gone to another level. */
		protected boolean casterHere() {
			return caster.isAlive() && !caster.isRemoved() && caster.level() == level;
		}
	}

	/** One the caster's blessings reach: itself, or any being not its enemy and not a monster (villagers, beasts, peaceful folk, allies). */
	public static boolean friendly(ServerPlayer p, LivingEntity e) {
		if (e == p) return true;
		if (!e.isAlive() || e instanceof Player other && other.isSpectator()) return false;
		return !(e instanceof Enemy) && !enemy(p, e);
	}

	/** Sets {@code effect} running. */
	public static void linger(Lingering effect) {
		LINGERING.add(effect);
	}

	/** The lingering effects of {@code type} {@code caster} has running (to refresh one, or to test). */
	public static <T extends Lingering> List<T> lingering(UUID caster, Class<T> type) {
		List<T> found = new ArrayList<>();
		for (Lingering l : LINGERING) if (type.isInstance(l) && (caster == null || caster.equals(l.caster()))) found.add(type.cast(l));
		return found;
	}

	/** Ends {@code effect} now, if it is still running. */
	public static void stop(Lingering effect) {
		if (LINGERING.remove(effect)) endQuietly(effect);
	}

	/** Ends every lingering effect of {@code type} of {@code caster}'s now. */
	public static void endLingering(UUID caster, Class<? extends Lingering> type) {
		// Taken out first, then ended: an ending may set something else lingering (an uprooting lends its blow).
		List<Lingering> ending = new ArrayList<>();
		for (Lingering l : LINGERING) if (type.isInstance(l) && caster.equals(l.caster())) ending.add(l);
		LINGERING.removeAll(ending);
		for (Lingering l : ending) endQuietly(l);
	}

	private static void endQuietly(Lingering l) {
		try {
			l.end();
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Ending a lingering heaven spell failed", e);
		}
	}

	// --- Hooks: harm coming in, blows going out, a death ---

	/**
	 * A lingering effect that has a say in the harm a being is about to take (a ward, a mirror, a burden shared). Asked at the
	 * start of LivingEntity#hurt on the server, after the dead's and souls' own scaling, in the order the effects were set
	 * running. A blow may still be refused after (invulnerability after a hit: a being still in it, its invulnerableTime above
	 * 10, takes only what exceeds the last blow), so a listener that spends something on a blow should check that first.
	 */
	public interface HarmListener {
		/** What {@code amount} of harm from {@code source} on {@code target} comes to; the same amount to leave it be. */
		float onIncoming(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float amount);
	}

	/** A lingering effect that hears of its caster's (or anyone's) melee blows, as they land (Spells#onMeleeHit). */
	public interface MeleeListener {
		void onMelee(ServerPlayer attacker, LivingEntity target);
	}

	/** A lingering effect that may hold off a death (ServerLivingEntityEvents.ALLOW_DEATH). */
	public interface DeathListener {
		/** False to stop {@code entity}'s death (it must then be left alive: health above 0). */
		boolean allowDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source);
	}

	/** A lingering effect that may refuse a being's healing (a seared wound) or take it for itself (a strangler fig). */
	public interface HealListener {
		/** False to refuse {@code amount} of healing to {@code entity}. */
		boolean allowHeal(LivingEntity entity, float amount);
	}

	/** A lingering effect that may refuse an effect taking hold of a being (a ward against harm, a severed fate). */
	public interface EffectListener {
		/** False to refuse {@code effect} to {@code entity}. */
		boolean allowEffect(LivingEntity entity, net.minecraft.world.effect.MobEffectInstance effect);
	}

	/** A lingering effect that hears of a player casting any spell (a mist that hides it, broken by the cast). */
	public interface CastListener {
		void onCast(ServerPlayer caster, Spell spell);
	}

	/** The element spells cast on oneself (wards, a body's arts, a field round one): their seal is under the caster only. */
	private static final java.util.Set<Spell> ON_ONESELF = java.util.EnumSet.of(Spell.FLAME_FIST, Spell.CRIMSON_TEMPERING, Spell.EMERALD_HEALING_ART,
			Spell.VERDANT_GROWTH, Spell.HARDENED_BODY, Spell.DIRT_TIDE, Spell.AZURE_COATING_TECHNIQUE, Spell.AZURE_BREATHING_TECHNIQUE, Spell.BLADE_POLISH,
			Spell.FLAME_ARMOUR, Spell.MINERAL_PURIFICATION, Spell.SOLAR_FIELD, Spell.ANCIENT_BARK_SKIN, Spell.MOUNTAIN_STANCE, Spell.MIST_VEIL,
			Spell.GOLDEN_BELL_SHIELD, Spell.LOTUS_OF_REBIRTH, Spell.EARTHQUAKE, Spell.VAJRA_BODY, Spell.SEA_OF_FLAMES, Spell.SPRING_OF_MYRIAD_LIFE,
			Spell.FROZEN_FIELD, Spell.SWORD_DOMAIN, Spell.PHOENIX_NIRVANA, Spell.WORLD_TREE, Spell.ABSOLUTE_ZERO, Spell.BOUNDLESS_SEA, Spell.FOUR_SWORDS_ARRAY);

	/**
	 * An element spell's casting seal (SpellFx CAST_SEAL), seen by all near: its element's seal turning under the caster, the larger
	 * and grander the deeper its realm, and for one cast at something a smaller one upright before them, facing the way.
	 */
	public static void castSeal(ServerPlayer caster, Spell spell) {
		if (!spell.isHeaven()) return;
		int realm = spell.heavenRealm().ordinal();
		float radius = 1.4f + 0.75f * realm;
		Vec3 way = ON_ONESELF.contains(spell) ? Vec3.ZERO : caster.getLookAngle();
		SpellFx.send(caster.serverLevel(), SpellFx.Kind.CAST_SEAL, caster.position(), way, radius, spell.element().ordinal() + 16 * realm);
	}

	/** {@code caster} is casting {@code spell} (paid for, about to take hold). */
	public static void onCast(ServerPlayer caster, Spell spell) {
		if (LINGERING.isEmpty()) return;
		for (Lingering l : new ArrayList<>(LINGERING)) {
			if (l instanceof CastListener listener) {
				try {
					listener.onCast(caster, spell);
				} catch (Exception e) {
					DefyingTheHeavens.LOGGER.error("A heaven spell's ear for casting failed", e);
				}
			}
		}
	}

	/** Beings whose harm a ward has brought to nothing this tick: the blow is refused outright (no knockback, no flinch). */
	private static final java.util.Set<LivingEntity> WARDED_OFF = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

	/**
	 * The harm {@code target} takes from {@code source}, after every ward lingering and the earth's passive (HeavenPassives). One a
	 * ward brings to nothing is refused outright ({@link #wardedOff}, ALLOW_DAMAGE).
	 */
	public static float incoming(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float amount) {
		// First the auras: a ward of the element that overcomes the blow's, a Golden Bell, a polished blade (SpellAuras).
		amount = SpellAuras.incoming(target, source, amount);
		if (amount <= 0) {
			WARDED_OFF.add(target);
			return 0;
		}
		if (!LINGERING.isEmpty()) {
			for (Lingering l : new ArrayList<>(LINGERING)) {
				if (l instanceof HarmListener listener) {
					try {
						amount = listener.onIncoming(target, source, amount);
					} catch (Exception e) {
						DefyingTheHeavens.LOGGER.error("A heaven spell's ward failed", e);
					}
					if (amount <= 0) {
						WARDED_OFF.add(target);
						return 0;
					}
				}
			}
		}
		return HeavenPassives.incoming(target, source, amount);
	}

	/** Whether a ward brought the blow {@code entity} is taking to nothing (ALLOW_DAMAGE refuses it then); asked once a blow. */
	public static boolean wardedOff(LivingEntity entity) {
		return !WARDED_OFF.isEmpty() && WARDED_OFF.remove(entity);
	}

	/** Whether {@code entity} may be healed {@code amount} (LivingEntity#heal). */
	public static boolean allowHeal(LivingEntity entity, float amount) {
		if (LINGERING.isEmpty()) return true;
		for (Lingering l : new ArrayList<>(LINGERING)) {
			if (l instanceof HealListener listener) {
				try {
					if (!listener.allowHeal(entity, amount)) return false;
				} catch (Exception e) {
					DefyingTheHeavens.LOGGER.error("A heaven spell's hold on healing failed", e);
				}
			}
		}
		return true;
	}

	/** Whether {@code effect} may take hold of {@code entity} (LivingEntity#canBeAffected). */
	public static boolean allowEffect(LivingEntity entity, net.minecraft.world.effect.MobEffectInstance effect) {
		if (LINGERING.isEmpty()) return true;
		for (Lingering l : new ArrayList<>(LINGERING)) {
			if (l instanceof EffectListener listener) {
				try {
					if (!listener.allowEffect(entity, effect)) return false;
				} catch (Exception e) {
					DefyingTheHeavens.LOGGER.error("A heaven spell's ward against effects failed", e);
				}
			}
		}
		return true;
	}

	/** A melee blow of {@code attacker}'s landed on {@code target}. */
	public static void onMelee(ServerPlayer attacker, LivingEntity target) {
		SpellAuras.onMelee(attacker, target); // Flame Fist
		if (LINGERING.isEmpty()) return;
		for (Lingering l : new ArrayList<>(LINGERING)) {
			if (l instanceof MeleeListener listener) {
				try {
					listener.onMelee(attacker, target);
				} catch (Exception e) {
					DefyingTheHeavens.LOGGER.error("A heaven spell's blow failed", e);
				}
			}
		}
	}

	/** Whether {@code entity}'s death goes ahead: false if a lingering effect holds it off. */
	public static boolean allowDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		if (LINGERING.isEmpty()) return true;
		for (Lingering l : new ArrayList<>(LINGERING)) {
			if (l instanceof DeathListener listener) {
				try {
					if (!listener.allowDeath(entity, source)) return false;
				} catch (Exception e) {
					DefyingTheHeavens.LOGGER.error("A heaven spell's rebirth failed", e);
				}
			}
		}
		return true;
	}

	/** Whether a blow on {@code target} now may be refused by its invulnerability after the last (a listener spending something checks). */
	public static boolean inHurtTime(LivingEntity target) {
		return target.invulnerableTime > 10;
	}

	// --- Temporary blocks ---

	private record Placed(ServerLevel level, BlockPos pos, BlockState was, BlockState placed, long until) {}

	/** Each block a spell has set down, by level and place (a Frozen Field sets down thousands: looked up, never searched). */
	private static final Map<ServerLevel, Map<BlockPos, Placed>> TEMPORARY = new HashMap<>();
	/** The same, soonest to be taken up first. */
	private static final java.util.PriorityQueue<Placed> DUE = new java.util.PriorityQueue<>(java.util.Comparator.comparingLong(Placed::until));

	/**
	 * Sets {@code state} at {@code pos} for {@code ticks}, then takes it up again: only into air or a replaceable block (grass,
	 * snow, water...), never inside a raised formation's barrier, and never where a spell's block already stands. Returns
	 * whether it was set. What it replaced comes back (a replaceable plant comes back as air, water as water).
	 */
	public static boolean placeTemporary(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
		BlockState here = level.getBlockState(pos);
		if (!here.canBeReplaced() || !level.isInWorldBounds(pos) || Formations.shelters(level, pos) || isTemporary(level, pos)) return false;
		BlockState was = here.getFluidState().isEmpty() ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : here.getFluidState().createLegacyBlock();
		if (!level.setBlock(pos, state, 3)) return false;
		Placed placed = new Placed(level, pos.immutable(), was, state, level.getGameTime() + Math.max(1, ticks));
		TEMPORARY.computeIfAbsent(level, k -> new HashMap<>()).put(placed.pos(), placed);
		DUE.add(placed);
		return true;
	}

	/** Whether a spell's block stands at {@code pos} now. */
	public static boolean isTemporary(ServerLevel level, BlockPos pos) {
		Map<BlockPos, Placed> here = TEMPORARY.get(level);
		return here != null && here.containsKey(pos);
	}

	/** Takes up a spell's block at {@code pos} at once (a wall shattered early). */
	public static void removeTemporary(ServerLevel level, BlockPos pos) {
		Map<BlockPos, Placed> here = TEMPORARY.get(level);
		Placed placed = here == null ? null : here.remove(pos);
		if (placed == null) return;
		if (here.isEmpty()) TEMPORARY.remove(level);
		restore(placed);
	}

	/**
	 * A player breaks a spell's block (PlayerBlockBreakEvents): it is taken up at once, what it replaced back, and nothing drops (a
	 * wall, a tree, a field's ice are never a quarry). Returns whether {@code pos} held one.
	 */
	public static boolean breakTemporary(ServerLevel level, BlockPos pos) {
		if (!isTemporary(level, pos)) return false;
		BlockState state = level.getBlockState(pos);
		level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state)); // the break's crumbs and sound
		removeTemporary(level, pos);
		return true;
	}

	/** A blast takes no spell's block as loot: those among {@code toBlow} are taken up at once instead, dropping nothing. */
	public static void spareTemporaryFromBlast(ServerLevel level, List<BlockPos> toBlow) {
		if (TEMPORARY.get(level) == null) return;
		toBlow.removeIf(pos -> {
			if (!isTemporary(level, pos)) return false;
			removeTemporary(level, pos);
			return true;
		});
	}

	/** Moves when a spell's block at {@code pos} is taken up to {@code ticks} from now (ice thawing a little sooner here, later there). */
	public static void retime(ServerLevel level, BlockPos pos, int ticks) {
		Map<BlockPos, Placed> here = TEMPORARY.get(level);
		Placed placed = here == null ? null : here.get(pos);
		if (placed == null) return;
		Placed moved = new Placed(level, placed.pos(), placed.was(), placed.placed(), level.getGameTime() + Math.max(1, ticks));
		here.put(moved.pos(), moved);
		DUE.add(moved);
	}

	/** How many of the spells' blocks stand now (tests). */
	public static int temporaryCount() {
		int count = 0;
		for (Map<BlockPos, Placed> here : TEMPORARY.values()) count += here.size();
		return count;
	}

	private static void restore(Placed placed) {
		ServerLevel level = placed.level();
		if (!level.isLoaded(placed.pos())) {
			level.getChunkAt(placed.pos()); // load it: a spell's block is never left behind
		}
		// Only what the spell set: if something else has taken the place since (a player's block), it stays.
		if (level.getBlockState(placed.pos()).is(placed.placed().getBlock())) level.setBlock(placed.pos(), placed.was(), 3);
	}

	// --- Carving the ground for good ---

	/**
	 * Whether a spell may take the block at {@code pos} away for good (a boulder torn from the ground, a cave carved, a slash cut
	 * through a hillside): something there, not a spell's own block, nothing kept in it (a chest, a furnace), no harder than
	 * obsidian's toughness and not unbreakable, not a barrier, and not under a formation's shelter or ground {@code p} may not dig
	 * (Adventure mode, spawn protection, a sect's ground).
	 */
	public static boolean carvable(ServerLevel level, BlockPos pos, ServerPlayer p) {
		return carvable(level, pos, p, false);
	}

	/**
	 * As {@link #carvable(ServerLevel, BlockPos, ServerPlayer)}, or, {@code utter}, as a burst golden core takes the land
	 * (CoreDetonation; the user's ask, 2026-10-23): water and lava too, and what holds or keeps something (a lectern, a chest, a
	 * Spirit Pedestal, any mod's block with a block entity), its contents thrown out as it goes (Devastation#obliterate). Still
	 * never what is unbreakable or harder than obsidian, a barrier, a spell's own block, or ground sheltered or not the caster's.
	 */
	public static boolean carvable(ServerLevel level, BlockPos pos, ServerPlayer p, boolean utter) {
		if (!level.isInWorldBounds(pos)) return false;
		BlockState state = level.getBlockState(pos);
		if (state.isAir()) return false;
		boolean fluid = !state.getFluidState().isEmpty() && state.canBeReplaced();
		if (fluid && !utter) return false;
		// A fluid's own block is as hard as can be (water and lava 100), though nothing breaks it by hand: it is let go by that.
		float hardness = fluid ? 0 : state.getDestroySpeed(level, pos);
		if (hardness < 0 || hardness >= 50 || state.hasBlockEntity() && !utter || state.getBlock() instanceof SectBarrierBlock) return false;
		if (isTemporary(level, pos) || Formations.shelters(level, pos)) return false;
		// Nor what the caster couldn't dig by hand: in Adventure mode, or under the server's spawn protection.
		return p == null || p.mayBuild() && level.mayInteract(p, pos) && !Formations.protectsGround(p, pos);
	}

	/** What a block of {@code state} looks like as moved earth: grassy and snowy tops come as their soil. */
	public static BlockState asEarth(BlockState state) {
		if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || state.is(net.minecraft.world.level.block.Blocks.MYCELIUM)
				|| state.is(net.minecraft.world.level.block.Blocks.PODZOL) || state.is(net.minecraft.world.level.block.Blocks.DIRT_PATH)) {
			return net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
		}
		return state;
	}

	// --- Whom, and where ---

	/**
	 * Spared by a caster's spells: the caster, the dead and fallen, spectators, its own tamed creatures, and any player it may not
	 * harm (PvP off, or a team that won't hurt its own), and the members of its own sect (SectMembership): no hold, push, drowning
	 * or burning reaches them either, not only the harm.
	 */
	public static boolean spared(ServerPlayer p, LivingEntity e) {
		return e == p || !e.isAlive() || e instanceof Player player && (player.isSpectator() || !p.canHarmPlayer(player))
				|| e instanceof OwnableEntity pet && p.getUUID().equals(pet.getOwnerUUID())
				|| e instanceof CultivatorNpc npc && SectMembership.areFellows(npc, p);
	}

	/**
	 * The first being along the look within {@code range} that a blessing may reach (Life Force Gain): any but the caster, the dead and
	 * spectators, a player too where the server keeps players from harming each other (which {@link #aimed} spares).
	 */
	public static LivingEntity aimedAny(ServerPlayer p, double range) {
		Vec3 from = p.getEyePosition();
		Vec3 to = beamEnd(p, range);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, from, to, new AABB(from, to).inflate(1.0),
				e -> e instanceof LivingEntity living && living != p && living.isAlive() && !(living instanceof Player other && other.isSpectator()));
		return hit != null && hit.getEntity() instanceof LivingEntity target ? target : null;
	}

	/**
	 * A mark for a spell that seeks beings out where its caster looks (Heaven-Piercing Bamboo, Myriad Vine Binding, Thorn Spear
	 * Forest): any foe or enemy (what {@link #friendly} isn't), and any person, a player or a cultivator, that the caster's spells
	 * don't spare (PvP off, a sect fellow: {@link #spared}). Beasts and village folk minding their own business are left be.
	 */
	public static boolean mark(ServerPlayer p, LivingEntity e) {
		if (spared(p, e)) return false;
		return e instanceof Player || e instanceof CultivatorNpc || !friendly(p, e);
	}

	/** A foe of the caster's: a monster, or anything set on it (a mob or a cultivator with it as its target). */
	public static boolean foe(ServerPlayer p, LivingEntity e) {
		if (spared(p, e)) return false;
		return e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == p;
	}

	/**
	 * An enemy of the caster's: a foe ({@link #foe}), or a player at odds with it, one that struck it or that it struck in the
	 * last {@link #QUARREL_TICKS} (so a duel's spells find their mark, and a passing player is never taken for a foe).
	 */
	public static boolean enemy(ServerPlayer p, LivingEntity e) {
		if (spared(p, e)) return false;
		if (foe(p, e)) return true;
		if (!(e instanceof Player other)) return false;
		return p.getLastHurtByMob() == other && p.tickCount - p.getLastHurtByMobTimestamp() < QUARREL_TICKS
				|| other.getLastHurtByMob() == p && other.tickCount - other.getLastHurtByMobTimestamp() < QUARREL_TICKS;
	}

	/** How long a blow between two players makes them enemies to each other's spells ({@link #enemy}). */
	public static final int QUARREL_TICKS = 200;

	/**
	 * One of the great beasts (the Ender Dragon, the Wither, the Warden, an Elder Guardian, or anything of 300 health or more):
	 * the spells that would hold, carry, swap or lull a being only slow or harm them.
	 */
	public static boolean greatBeast(LivingEntity e) {
		return e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss
				|| e instanceof net.minecraft.world.entity.monster.warden.Warden || e instanceof net.minecraft.world.entity.monster.ElderGuardian
				|| e.getMaxHealth() >= 300;
	}

	/** Whether {@code source} is one of the heaven spells' qi harms (never reflected or caught again by another spell). */
	public static boolean isQi(net.minecraft.world.damagesource.DamageSource source) {
		for (QiElement element : QiElement.five()) if (source.is(ModDamageTypes.qiType(element))) return true;
		return false;
	}

	/** Takes {@code e} to {@code to} at once, keeping its facing (a player through its connection), its motion and fall stilled. */
	public static void teleport(LivingEntity e, Vec3 to) {
		e.stopRiding();
		if (e instanceof ServerPlayer sp) sp.teleportTo(sp.serverLevel(), to.x, to.y, to.z, sp.getYRot(), sp.getXRot());
		else e.teleportTo(to.x, to.y, to.z);
		e.setDeltaMovement(Vec3.ZERO);
		e.hurtMarked = true;
		e.resetFallDistance();
	}

	/** No fall harm for {@code e} for {@code ticks}: a player through Spells' safe fall, anything else by forgetting its fall now. */
	public static void safeFall(LivingEntity e, int ticks) {
		if (e instanceof ServerPlayer sp) Spells.safeFall(sp, ticks);
		e.resetFallDistance();
	}

	/** Stops a mob where it stands: its path dropped, its sideways motion stilled (it may still fall). */
	public static void still(LivingEntity e) {
		if (e instanceof Mob mob) mob.getNavigation().stop();
		Vec3 v = e.getDeltaMovement();
		e.setDeltaMovement(0, Math.min(v.y, 0), 0);
		e.hurtMarked = true;
	}

	/** The top of the first block with a collision shape at or below {@code at}, searching from {@code up} above to {@code down} below; or null. */
	public static Vec3 groundBelow(ServerLevel level, Vec3 at, int up, int down) {
		BlockPos start = BlockPos.containing(at.x, at.y + up, at.z);
		for (int dy = 0; dy <= up + down; dy++) {
			BlockPos pos = start.below(dy);
			if (!level.isInWorldBounds(pos)) continue;
			net.minecraft.world.phys.shapes.VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
			if (!shape.isEmpty()) return new Vec3(at.x, pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y), at.z);
		}
		return null;
	}

	/** Plays {@code sound} at {@code at} for everyone near. */
	public static void sound(ServerLevel level, Vec3 at, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, net.minecraft.sounds.SoundSource.PLAYERS, volume, pitch);
	}

	/** {@code count} of {@code particle} at {@code at}, strewn {@code spread} each way, at {@code speed}. */
	public static void particles(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
		level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
	}

	/** A block's crumbs, as particles (BlockParticleOption). */
	public static ParticleOptions block(net.minecraft.world.level.block.Block block) {
		return new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, block.defaultBlockState());
	}

	/** Whether {@code e} is still a fit target in {@code level}: alive, not removed, not gone to another level. */
	public static boolean present(LivingEntity e, ServerLevel level) {
		return e != null && e.isAlive() && !e.isRemoved() && e.level() == level;
	}

	/** An ally of the caster's to ward or mend: another player (where they could not harm each other, too), or its tamed creature. */
	public static boolean ally(ServerPlayer p, LivingEntity e) {
		if (e == p || !e.isAlive()) return false;
		if (e instanceof OwnableEntity pet && p.getUUID().equals(pet.getOwnerUUID())) return true;
		return e instanceof Player other && !other.isSpectator() && !enemy(p, e);
	}

	/** Every being within {@code radius} of the caster that {@code which} takes. */
	public static List<LivingEntity> around(ServerPlayer p, double radius, Predicate<LivingEntity> which) {
		return aroundPoint(p.serverLevel(), p.position(), radius, which);
	}

	/** Every being within {@code radius} of {@code centre} (by its feet) that {@code which} takes. */
	public static List<LivingEntity> aroundPoint(ServerLevel level, Vec3 centre, double radius, Predicate<LivingEntity> which) {
		return level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(radius),
				e -> e.position().distanceToSqr(centre) <= radius * radius && which.test(e));
	}

	/** Where the look meets a wall within {@code range}, or the end of the range. */
	public static Vec3 beamEnd(ServerPlayer p, double range) {
		Vec3 from = p.getEyePosition();
		Vec3 to = from.add(p.getViewVector(1.0f).scale(range));
		BlockHitResult hit = p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
	}

	/** The block the look meets within {@code range} (its top face's centre if it was struck from above), or null. */
	public static BlockHitResult lookedBlock(ServerPlayer p, double range) {
		Vec3 from = p.getEyePosition();
		Vec3 to = from.add(p.getViewVector(1.0f).scale(range));
		BlockHitResult hit = p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		return hit.getType() == HitResult.Type.MISS ? null : hit;
	}

	/** The point looked at within {@code range}: where the look meets a wall or the ground, else the end of the range. */
	public static Vec3 lookPoint(ServerPlayer p, double range) {
		return beamEnd(p, range);
	}

	/** The first being along the look within {@code range}, short of a wall, that {@code which} takes; or null. */
	public static LivingEntity aimed(ServerPlayer p, double range, Predicate<LivingEntity> which) {
		Vec3 from = p.getEyePosition();
		Vec3 to = beamEnd(p, range);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, from, to, new AABB(from, to).inflate(1.0),
				e -> e instanceof LivingEntity living && !spared(p, living) && which.test(living));
		return hit != null && hit.getEntity() instanceof LivingEntity target ? target : null;
	}

	/** The first being along the look within {@code range} that the caster's spells don't spare; or null. */
	public static LivingEntity aimed(ServerPlayer p, double range) {
		return aimed(p, range, e -> true);
	}

	/** Every being along the look within {@code range} (a line {@code width} wide), short of a wall, nearest first. */
	public static List<LivingEntity> alongLook(ServerPlayer p, double range, double width) {
		Vec3 from = p.getEyePosition();
		Vec3 to = beamEnd(p, range);
		Vec3 dir = to.subtract(from);
		double length = dir.length();
		if (length < 1.0e-3) return List.of();
		Vec3 unit = dir.scale(1 / length);
		List<LivingEntity> found = new ArrayList<>(p.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(width + 1), e -> {
			if (spared(p, e)) return false;
			Vec3 mid = e.position().add(0, e.getBbHeight() / 2, 0).subtract(from);
			double along = mid.dot(unit);
			if (along < 0 || along > length) return false;
			return mid.subtract(unit.scale(along)).length() <= width + e.getBbWidth() / 2;
		}));
		found.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(p)));
		return found;
	}

	/** Where a cast from the hands sets out: before the chest, clear of the caster's own eyes. */
	public static Vec3 fromHands(ServerPlayer p) {
		return p.getEyePosition().subtract(0, 0.45, 0).add(p.getViewVector(1.0f).scale(1.2));
	}

	/** The middle of a being's body. */
	public static Vec3 middle(Entity e) {
		return e.position().add(0, e.getBbHeight() / 2, 0);
	}

	// --- Harm and force ---

	/**
	 * Strikes {@code target} for {@code amount} of {@code element}'s qi harm by {@code p}, and returns how much it took (health
	 * and absorption). The dead take it only from one who sees souls, as any living blow (GhostSystem#reaches).
	 */
	public static float strike(ServerPlayer p, LivingEntity target, float amount, QiElement element) {
		float before = target.getHealth() + target.getAbsorptionAmount();
		target.hurt(ModDamageTypes.qi(p.serverLevel(), element, p), amount);
		return Math.max(0, before - target.getHealth() - target.getAbsorptionAmount());
	}

	/**
	 * Strikes {@code target} for {@code amount} of {@code spell}'s element's qi harm (of its realm, for the wards: SpellAuras) by
	 * {@code p} ({@code via}: the thing that struck, or null for the caster's own hand), and returns how much it took.
	 */
	public static float strike(ServerPlayer p, LivingEntity target, float amount, Spell spell, net.minecraft.world.entity.Entity via) {
		float before = target.getHealth() + target.getAbsorptionAmount();
		ServerLevel level = (ServerLevel) target.level();
		target.hurt(ModDamageTypes.qi(level, spell.element(), spell.heavenRealm(), via, p), amount);
		return Math.max(0, before - target.getHealth() - target.getAbsorptionAmount());
	}

	/** {@link #strike(ServerPlayer, LivingEntity, float, Spell, net.minecraft.world.entity.Entity)} by the caster's own hand. */
	public static float strike(ServerPlayer p, LivingEntity target, float amount, Spell spell) {
		return strike(p, target, amount, spell, null);
	}

	/** Strikes without the struck flinching away from the next blow's harm (a zone's ticks, a chain's leaps). */
	public static float strikeAgain(ServerPlayer p, LivingEntity target, float amount, QiElement element) {
		target.invulnerableTime = 0;
		return strike(p, target, amount, element);
	}

	/** {@link #strike(ServerPlayer, LivingEntity, float, Spell)} without the struck flinching away from the next (a zone's ticks). */
	public static float strikeAgain(ServerPlayer p, LivingEntity target, float amount, Spell spell) {
		target.invulnerableTime = 0;
		return strike(p, target, amount, spell);
	}

	/** Throws {@code target} away from {@code from} along the ground, {@code strength} as a knockback's. */
	public static void push(Vec3 from, LivingEntity target, double strength) {
		Vec3 away = target.position().subtract(from).multiply(1, 0, 1);
		if (away.lengthSqr() > 1.0e-4) target.knockback(strength, -away.x, -away.z);
	}

	/** Sets {@code target} moving at {@code velocity} (a launch, a pull), and tells its client if it is a player. */
	public static void move(LivingEntity target, Vec3 velocity) {
		target.setDeltaMovement(velocity);
		target.hurtMarked = true;
	}

	/** Mends {@code target} by {@code amount}. */
	public static void mend(LivingEntity target, float amount) {
		if (amount > 0) target.heal(amount);
	}

	// --- Visuals ---

	/** A dust mote of {@code rgb} (0xRRGGBB), {@code size} across. */
	public static DustParticleOptions dust(int rgb, float size) {
		return new DustParticleOptions(new Vector3f((rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f), size);
	}

	/** The element's own dust (its core colour). */
	public static DustParticleOptions dust(QiElement element, float size) {
		return dust(element.getCoreColor(), size);
	}

	/** {@code particle} strewn along the line from {@code a} to {@code b}, one every {@code step} blocks. */
	public static void line(ServerLevel level, Vec3 a, Vec3 b, ParticleOptions particle, double step) {
		Vec3 along = b.subtract(a);
		double length = along.length();
		if (length < 1.0e-3) return;
		Vec3 dir = along.scale(1 / length);
		for (double d = 0; d <= length; d += step) {
			Vec3 at = a.add(dir.scale(d));
			level.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		}
	}

	/** {@code particle} in a flat ring of {@code radius} round {@code centre}, {@code count} of them. */
	public static void ring(ServerLevel level, Vec3 centre, double radius, ParticleOptions particle, int count) {
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2 * i / count;
			level.sendParticles(particle, centre.x + Math.cos(angle) * radius, centre.y, centre.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
		}
	}

	private HeavenSpells() {}
}
