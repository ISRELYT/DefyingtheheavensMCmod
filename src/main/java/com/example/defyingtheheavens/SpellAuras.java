package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The auras element spells leave on beings (SpellAura): who has which, until when, and what they do while they last. Every
 * client that sees a being is told its auras (on a change, and when it starts seeing it), to draw them. Here too the wards'
 * part in the harm a being takes ({@link #incoming}: the cycle of the five, the Golden Bell, Blade Polish's edge), the Flame
 * Fist's blows ({@link #onMelee}), and the auras that work by the tick (healing, breath, fire put out).
 */
public final class SpellAuras {
	/** Harm of the element that overcomes a ward strikes this many times as hard through it. */
	public static final double PIERCE = 1.25;
	/** Blade Polish: melee blows strike this many times as hard. */
	public static final double BLADE_POLISH_FACTOR = 1.2;
	/** Emerald Healing Art: health a second, fed or not. */
	public static final float EMERALD_HEALING_PER_SECOND = 1.0f;
	/** Flame Fist: how long a blow sets its mark burning, and the fire's own harm with it. */
	public static final int FLAME_FIST_BURN_SECONDS = 5;
	public static final float FLAME_FIST_DAMAGE = 1.0f;
	/** Vajra Body: harm a melee blow's striker takes from the golden skin. */
	public static final float VAJRA_THORNS = 4.0f;
	/** Mountain Stance: the share of walking speed it takes. */
	public static final double MOUNTAIN_STANCE_SLOW = 0.2;

	private static final UUID STANCE_SPEED_ID = UUID.fromString("7d0f2c41-3e8a-4b9d-a6c2-51e9f08b3d17");
	private static final UUID STANCE_KNOCKBACK_ID = UUID.fromString("7d0f2c41-3e8a-4b9d-a6c2-51e9f08b3d18");
	private static final UUID VAJRA_KNOCKBACK_ID = UUID.fromString("7d0f2c41-3e8a-4b9d-a6c2-51e9f08b3d19");

	/** Each being's auras, with the game time each ends. */
	private static final Map<LivingEntity, EnumMap<SpellAura, Long>> AURAS = new HashMap<>();
	/** What each Golden Bell can still take. */
	private static final Map<LivingEntity, Float> BELLS = new HashMap<>();
	/** Beings whose auras changed since their watchers were last told. */
	private static final List<LivingEntity> CHANGED = new ArrayList<>();

	// --- Who has what ---

	/** Puts {@code aura} on {@code e} for {@code ticks} (from now; a longer one already on stays). */
	public static void put(LivingEntity e, SpellAura aura, int ticks) {
		if (e.level().isClientSide) return;
		EnumMap<SpellAura, Long> mine = AURAS.computeIfAbsent(e, k -> new EnumMap<>(SpellAura.class));
		long until = e.level().getGameTime() + Math.max(1, ticks);
		Long was = mine.get(aura);
		if (was != null && was >= until) return;
		mine.put(aura, until);
		if (was == null) began(e, aura);
		changed(e);
	}

	/** Takes {@code aura} off {@code e} now. */
	public static void remove(LivingEntity e, SpellAura aura) {
		EnumMap<SpellAura, Long> mine = AURAS.get(e);
		if (mine == null || mine.remove(aura) == null) return;
		ended(e, aura);
		if (mine.isEmpty()) AURAS.remove(e);
		changed(e);
	}

	/** The client's view of the auras it was told of (set by the client: ClientSpellAuras); nothing on a server. */
	public static java.util.function.BiPredicate<LivingEntity, SpellAura> clientView = (e, aura) -> false;

	/** Whether {@code e} has {@code aura} now, on either side (on a client, as it was told). */
	public static boolean holds(LivingEntity e, SpellAura aura) {
		return e.level().isClientSide ? clientView.test(e, aura) : has(e, aura);
	}

	/** Whether {@code e} has {@code aura} now (server). */
	public static boolean has(LivingEntity e, SpellAura aura) {
		if (AURAS.isEmpty()) return false;
		EnumMap<SpellAura, Long> mine = AURAS.get(e);
		return mine != null && mine.containsKey(aura);
	}

	/** Ticks {@code aura} has left on {@code e} (0: none). */
	public static int left(LivingEntity e, SpellAura aura) {
		EnumMap<SpellAura, Long> mine = AURAS.get(e);
		Long until = mine == null ? null : mine.get(aura);
		return until == null ? 0 : (int) Math.max(0, until - e.level().getGameTime());
	}

	/** Whether {@code e} wears a ward of {@code element}. */
	public static boolean wardedBy(LivingEntity e, QiElement element) {
		EnumMap<SpellAura, Long> mine = AURAS.get(e);
		if (mine == null) return false;
		for (SpellAura aura : mine.keySet()) if (aura.ward() == element) return true;
		return false;
	}

	private static void changed(LivingEntity e) {
		if (!CHANGED.contains(e)) CHANGED.add(e);
	}

	/** An aura has just been put on: what it changes at once. */
	private static void began(LivingEntity e, SpellAura aura) {
		switch (aura) {
			case MOUNTAIN_STANCE -> {
				modifier(e, Attributes.MOVEMENT_SPEED, STANCE_SPEED_ID, "Mountain Stance", -MOUNTAIN_STANCE_SLOW, AttributeModifier.Operation.MULTIPLY_TOTAL);
				modifier(e, Attributes.KNOCKBACK_RESISTANCE, STANCE_KNOCKBACK_ID, "Mountain Stance", 1.0, AttributeModifier.Operation.ADDITION);
			}
			case VAJRA_BODY -> modifier(e, Attributes.KNOCKBACK_RESISTANCE, VAJRA_KNOCKBACK_ID, "Vajra Body", 1.0, AttributeModifier.Operation.ADDITION);
			case CRIMSON_TEMPERING, AZURE_COATING -> e.clearFire();
			default -> {}
		}
	}

	/** An aura has run out or been taken off: what it gives back. */
	private static void ended(LivingEntity e, SpellAura aura) {
		switch (aura) {
			case MOUNTAIN_STANCE -> {
				unmodify(e, Attributes.MOVEMENT_SPEED, STANCE_SPEED_ID);
				unmodify(e, Attributes.KNOCKBACK_RESISTANCE, STANCE_KNOCKBACK_ID);
			}
			case VAJRA_BODY -> unmodify(e, Attributes.KNOCKBACK_RESISTANCE, VAJRA_KNOCKBACK_ID);
			case GOLDEN_BELL -> BELLS.remove(e);
			default -> {}
		}
	}

	private static void modifier(LivingEntity e, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, String name, double amount,
								 AttributeModifier.Operation operation) {
		AttributeInstance instance = e.getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(id);
		instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
	}

	private static void unmodify(LivingEntity e, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id) {
		AttributeInstance instance = e.getAttribute(attribute);
		if (instance != null) instance.removeModifier(id);
	}

	// --- The Golden Bell ---

	/** Raises a Golden Bell over {@code e} that takes {@code hold} harm for it, for {@code ticks}. */
	public static void bell(LivingEntity e, float hold, int ticks) {
		BELLS.put(e, hold);
		put(e, SpellAura.GOLDEN_BELL, ticks);
	}

	/** What the Golden Bell over {@code e} can still take (tests). */
	public static float bellLeft(LivingEntity e) {
		return BELLS.getOrDefault(e, 0f);
	}

	// --- Harm ---

	/** Whether {@code stack} is a weapon of metal: a sword, axe or tool of iron, gold, diamond or netherite, or a trident. */
	public static boolean metalWeapon(ItemStack stack) {
		if (stack.getItem() instanceof TridentItem) return true;
		return stack.getItem() instanceof TieredItem tiered && tiered.getTier() != Tiers.WOOD && tiered.getTier() != Tiers.STONE
				&& !(stack.getItem() instanceof PeachWoodSwordItem);
	}

	/** The element a harm is of, for the wards: a spell's, fire's, drowning's, a metal weapon's blow; else null. */
	public static QiElement elementOf(DamageSource source) {
		if (source instanceof QiDamageSource qi) return qi.element();
		for (QiElement element : QiElement.five()) if (source.is(ModDamageTypes.qiType(element))) return element;
		if (source.is(DamageTypeTags.IS_FIRE)) return QiElement.FIRE;
		if (source.is(DamageTypeTags.IS_DROWNING)) return QiElement.WATER;
		if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
				&& (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)) && metalWeapon(attacker.getMainHandItem())) {
			return QiElement.METAL;
		}
		return null;
	}

	/**
	 * What {@code amount} of harm from {@code source} on {@code target} comes to after the spells' auras: the striker's Blade Polish,
	 * the target's wards (fire and lava against Crimson Tempering and Azure Coating, a wooden missile against a fire ward, the cycle
	 * of the five, each ward's share), and its Golden Bell. 0 is a blow warded off.
	 */
	public static float incoming(LivingEntity target, DamageSource source, float amount) {
		if (AURAS.isEmpty() || amount <= 0) return amount;
		// The striker's own: a polished blade bites deeper.
		if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && has(attacker, SpellAura.BLADE_POLISH)
				&& (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))) {
			amount *= (float) BLADE_POLISH_FACTOR;
		}
		EnumMap<SpellAura, Long> mine = AURAS.get(target);
		if (mine == null || HeavenPassives.untouchedByWards(source)) return amount;
		if (source.is(DamageTypeTags.IS_FIRE)) {
			if (mine.containsKey(SpellAura.CRIMSON_TEMPERING)) {
				target.clearFire();
				return 0;
			}
			if (mine.containsKey(SpellAura.AZURE_COATING)) {
				if (source.is(DamageTypes.LAVA)) { // lava boils the water off, and burns as lava does
					remove(target, SpellAura.AZURE_COATING);
					if (target.level() instanceof ServerLevel level) {
						SpellFx.send(level, SpellFx.Kind.STEAM, target.position().add(0, target.getBbHeight() / 2, 0), net.minecraft.world.phys.Vec3.ZERO, 1, 0);
						HeavenSpells.sound(level, target.position(), SoundEvents.FIRE_EXTINGUISH, 1.0f, 0.8f);
					}
				} else {
					target.clearFire();
					return 0;
				}
			}
		}
		// A wooden missile (a Wood Bullet, a razor leaf) burns up against a ward of fire.
		if (source.getDirectEntity() instanceof SpellProjectile missile && missile.isWooden() && wardedBy(target, QiElement.FIRE)) {
			if (target.level() instanceof ServerLevel level) SpellFx.send(level, SpellFx.Kind.ASH, missile.position(), net.minecraft.world.phys.Vec3.ZERO, 1, 0);
			return 0;
		}
		QiElement element = elementOf(source);
		Realm realm = source instanceof QiDamageSource qi ? qi.realm() : null;
		double factor = 1;
		for (SpellAura aura : mine.keySet()) {
			if (aura.ward() == null) continue;
			double reduction = aura.reduction();
			if (element != null && aura.ward().overcomes(element)) {
				double against = aura.overcomeReduction();
				if (realm != null && realm.ordinal() > aura.wardRealm().ordinal()) against /= 2; // a deeper realm's spell half breaks through
				reduction = Math.max(reduction, against);
			} else if (element != null && element.overcomes(aura.ward())) {
				factor *= PIERCE;
			}
			factor *= 1 - Math.min(1, reduction);
		}
		amount *= (float) factor;
		if (amount > 0 && mine.containsKey(SpellAura.GOLDEN_BELL)) amount = ring(target, element, amount);
		// Thorns answer a blow of the hand, never qi: two Vajra Bodies' thorns would answer each other's without end (a stack overflow
		// that took the server down, found in the sweep of 2026-10-23).
		if (amount > 0 && mine.containsKey(SpellAura.VAJRA_BODY) && source.getEntity() instanceof LivingEntity striker && source.getDirectEntity() == striker
				&& striker != target && !source.is(DamageTypeTags.IS_PROJECTILE) && !HeavenSpells.isQi(source) && target.level() instanceof ServerLevel level) {
			striker.hurt(ModDamageTypes.qi(level, QiElement.METAL, Realm.NASCENT_SOUL, null, target), VAJRA_THORNS);
		}
		return amount;
	}

	/** The Golden Bell takes what it can of {@code amount}: wood's at half its cost, fire's (which melts it) at twice. */
	private static float ring(LivingEntity target, QiElement element, float amount) {
		float hold = BELLS.getOrDefault(target, 0f);
		if (hold <= 0) return amount;
		double cost = element == QiElement.WOOD ? 0.5 : element == QiElement.FIRE ? 2.0 : 1.0;
		float taken = (float) Math.min(amount, hold / cost);
		hold -= (float) (taken * cost);
		if (target.level() instanceof ServerLevel level) {
			HeavenSpells.sound(level, target.position(), SoundEvents.BELL_BLOCK, 1.2f, 0.6f + level.random.nextFloat() * 0.2f);
			SpellFx.send(level, SpellFx.Kind.BELL_TOLL, target.position(), net.minecraft.world.phys.Vec3.ZERO, taken, 0);
		}
		if (hold <= 0.01f) {
			remove(target, SpellAura.GOLDEN_BELL);
			if (target.level() instanceof ServerLevel level) {
				HeavenSpells.sound(level, target.position(), SoundEvents.ANVIL_BREAK, 1.0f, 0.7f);
				SpellFx.send(level, SpellFx.Kind.BELL_SHATTER, target.position(), net.minecraft.world.phys.Vec3.ZERO, 1, 0);
			}
		} else {
			BELLS.put(target, hold);
		}
		return amount - taken;
	}

	/**
	 * A melee blow of {@code attacker}'s landed on {@code target}: with Flame Fist on its hands, the target burns (not one warded by
	 * water: the flames hiss out on it).
	 */
	public static void onMelee(LivingEntity attacker, LivingEntity target) {
		if (!has(attacker, SpellAura.FLAME_FIST) || !(attacker.level() instanceof ServerLevel level)) return;
		if (wardedBy(target, QiElement.WATER)) {
			HeavenSpells.sound(level, target.position(), SoundEvents.FIRE_EXTINGUISH, 0.8f, 1.3f);
			SpellFx.send(level, SpellFx.Kind.STEAM, target.position().add(0, target.getBbHeight() / 2, 0), net.minecraft.world.phys.Vec3.ZERO, 0.5f, 0);
			return;
		}
		target.setSecondsOnFire(FLAME_FIST_BURN_SECONDS);
		target.invulnerableTime = 0;
		target.hurt(ModDamageTypes.qi(level, QiElement.FIRE, Realm.QI_REFINING, null, attacker), FLAME_FIST_DAMAGE);
		SpellFx.send(level, SpellFx.Kind.FLAME_BURST, HeavenSpells.middle(target), net.minecraft.world.phys.Vec3.ZERO, 0.6f, 0);
		HeavenSpells.sound(level, target.position(), SoundEvents.FIRECHARGE_USE, 0.6f, 1.4f);
	}

	// --- By the tick ---

	/** Every server tick: auras run out, those that work by the tick do, and watchers hear of changes. */
	public static void tick(MinecraftServer server) {
		if (!AURAS.isEmpty()) {
			// Over copies: an aura's tick (a heal, a hold) can set others going on any being, and an EnumMap's entry is dead once removed.
			for (LivingEntity e : new ArrayList<>(AURAS.keySet())) {
				EnumMap<SpellAura, Long> mine = AURAS.get(e);
				if (mine == null) continue;
				if (e.isRemoved() || !e.isAlive()) {
					AURAS.remove(e);
					for (SpellAura aura : mine.keySet()) ended(e, aura);
					broadcast(e); // all gone: a player keeps its entity id through a respawn, and must not come back still wearing them
					continue;
				}
				long now = e.level().getGameTime();
				for (SpellAura aura : new ArrayList<>(mine.keySet())) {
					Long until = mine.get(aura);
					if (until == null) continue;
					if (now >= until) {
						mine.remove(aura);
						ended(e, aura);
						changed(e);
					} else {
						work(e, aura, now);
					}
				}
				if (mine.isEmpty()) AURAS.remove(e);
			}
		}
		if (!CHANGED.isEmpty()) {
			for (LivingEntity e : CHANGED) if (!e.isRemoved()) broadcast(e);
			CHANGED.clear();
		}
	}

	/** One tick of an aura that works by the tick. */
	private static void work(LivingEntity e, SpellAura aura, long now) {
		switch (aura) {
			case EMERALD_HEALING -> {
				if (now % 20 == 0 && e.getHealth() < e.getMaxHealth()) HeavenSpells.mend(e, EMERALD_HEALING_PER_SECOND);
			}
			case CRIMSON_TEMPERING, AZURE_COATING -> {
				if (e.isOnFire()) e.clearFire();
			}
			case AZURE_BREATHING -> e.setAirSupply(e.getMaxAirSupply());
			case FROZEN, PRESSED -> HeavenSpells.still(e);
			default -> {}
		}
	}

	// --- Watchers ---

	/** Tells everyone who sees {@code e} (and {@code e} itself, a player) its auras now. */
	private static void broadcast(LivingEntity e) {
		for (ServerPlayer watcher : PlayerLookup.tracking(e)) ModPackets.sendSpellAuras(watcher, e, AURAS.get(e));
		if (e instanceof ServerPlayer self) ModPackets.sendSpellAuras(self, e, AURAS.get(e));
	}

	/** {@code p} has changed worlds: told its own auras again (none, as often as not: whatever its client still drew goes). */
	public static void resend(ServerPlayer p) {
		ModPackets.sendSpellAuras(p, p, AURAS.get(p));
	}

	/** {@code viewer} has just begun to see {@code e}: told its auras, if any. */
	public static void startSeeing(ServerPlayer viewer, net.minecraft.world.entity.Entity e) {
		if (e instanceof LivingEntity living && AURAS.containsKey(living)) ModPackets.sendSpellAuras(viewer, living, AURAS.get(living));
	}

	/** A player left or changed worlds: what they wore ends (its effects given back). */
	public static void forget(LivingEntity e) {
		EnumMap<SpellAura, Long> mine = AURAS.remove(e);
		BELLS.remove(e);
		if (mine == null) return;
		for (SpellAura aura : mine.keySet()) ended(e, aura);
		broadcast(e); // those who saw them see them go
	}

	public static void clear() {
		for (Map.Entry<LivingEntity, EnumMap<SpellAura, Long>> entry : AURAS.entrySet()) {
			for (SpellAura aura : entry.getValue().keySet()) ended(entry.getKey(), aura);
		}
		AURAS.clear();
		BELLS.clear();
		CHANGED.clear();
	}

	private SpellAuras() {}
}
