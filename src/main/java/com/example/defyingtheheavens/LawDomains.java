package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Law domains (phase 3b of the realm plan, 2026-10-26; the proposal the user took): a Law comprehended, an immortal may unfurl its
 * own ground round itself, a sphere {@link #radius} that goes where it goes (its abilities, Ability#METAL_DOMAIN..., one for each Law from
 * its first level; off to begin with, one lit at a time). Lit, it burns immortal essence (a full pool in {@link #BURN_SECONDS}, beside what
 * the Immortal Essence switch burns: ImmortalEssence#second), and it won't unfurl on less than {@link #LIGHT_SECONDS} of it; it folds away
 * when the pool runs dry, at a death, at a logout, and where it can't hold (a soul out of its body, a passage). What it does is done to its
 * owner's foes alone ({@link #foeOf}: monsters, whatever is set on it, the players and cultivators it is at odds with; never a pet, a sect
 * fellow, a player out of reach of harm, nor anyone behind a barrier its owner couldn't break):
 * <ul>
 *   <li>its Law's own spells, its owner's, strike them {@link #SPELL_HARM} times as hard ({@link #outgoing}: the element's qi; for
 *       Space a spell of space, for Life and Death the ghost path's);</li>
 *   <li>Metal cuts them every second ({@link #HARM_SHARE} of its owner's blow, its qi) and cuts their armour {@link #ARMOUR_CUT_PER_LEVEL}
 *       a level; Wood's roots slow them and drain them, {@link #WOOD_DRAIN} of what they lose its owner's life; Water crushes and slows
 *       them, puts out what burns on them, and douses their own fire ({@link #FIRE_DOUSED_PER_LEVEL} a level weaker); Fire burns them, the
 *       harder ({@link #FIRE_SHARE}: true fire, its qi past any ward against fire), and sets them alight; Earth's weight crushes and
 *       slows them and brings fliers down out of the air (sparing them the fall);</li>
 *   <li>Space locks space on them: no step or tear through it ({@link #locksSpace}), and nothing it has caught leaves it, held to its
 *       edge as it goes; Time slows them, their blows too, and their spells cool {@link #TIME_STRETCH_PER_LEVEL} a level the slower
 *       ({@link #stretched}); Life and Death lets nothing in it heal ({@link #withholdsHealing}), and {@link #LIFE_DRAIN_PER_LEVEL} a
 *       level of the harm its owner deals them comes back to its owner as life ({@link #onHurt}).</li>
 * </ul>
 * Players' alone: the realm's own immortals have no Laws. Everyone near sees it (LAW_DOMAIN to those who track its owner).
 */
public final class LawDomains {
	/** A full pool of immortal essence lasts so many seconds with a domain lit. */
	public static final int BURN_SECONDS = 120;
	/** It won't unfurl on less than so many seconds of it. */
	public static final int LIGHT_SECONDS = 5;
	/** Its reach: so many blocks and so many more a level of its Law, and a tenth again for each realm past True Immortal. */
	public static final double BASE_RADIUS = 8, RADIUS_PER_LEVEL = 2, RADIUS_PER_REALM = 0.10;
	/** Its Law's own spells strike foes in it so many times as hard. */
	public static final float SPELL_HARM = 1.25f;
	/** Every second each foe in an element's domain takes this share of its owner's blow (ATTACK_DAMAGE) as the element's qi; Fire's more. */
	public static final float HARM_SHARE = 0.15f, FIRE_SHARE = 0.25f;
	/** Metal: armour cut so much a level. */
	public static final double ARMOUR_CUT_PER_LEVEL = 0.04;
	/** Wood: the share of the harm its roots do that comes back to its owner as life. */
	public static final float WOOD_DRAIN = 0.5f;
	/** Water: the fire a foe in it deals, so much weaker a level. */
	public static final float FIRE_DOUSED_PER_LEVEL = 0.04f;
	/** Time: a foe's spells cool so much the slower a level. */
	public static final double TIME_STRETCH_PER_LEVEL = 0.10;
	/** Life and Death: so much a level of the harm its owner deals foes in it comes back to its owner as life. */
	public static final float LIFE_DRAIN_PER_LEVEL = 0.03f;
	/** How often its hold on foes is renewed (slowness, fire, the cut armour, the weight, a catch), and its harm (ticks). */
	private static final int PULSE = 10, HARM_EVERY = 20;
	/** A foe Space has caught that is found this much past its edge (a pearl thrown, a portal) is let go. */
	private static final double ESCAPED = 16;
	private static final UUID ARMOUR_ID = UUID.fromString("6b2d4f0e-8f43-4c61-9a37-1c5b7d2e3a91");

	/** The domain abilities, in the Laws' order. */
	private static final Ability[] ABILITIES = Arrays.stream(Ability.values()).filter(a -> a.law() != null).toArray(Ability[]::new);

	/** A domain unfurled this tick: whose, its Law and that Law's level, its middle and its reach. */
	public record Domain(ServerPlayer owner, Law law, int level, Vec3 centre, double radius) {
		/**
		 * Whether {@code e} is within it (its body's middle) and a foe of its owner's ({@link #foeOf}); never one who wields Pangu's Axe
		 * (LegendaryWeaponItem#cleaves), whom no Law domain holds.
		 */
		public boolean holds(LivingEntity e) {
			return e != owner && e.level() == owner.level() && HeavenSpells.middle(e).distanceToSqr(centre) <= radius * radius && foeOf(owner, e)
					&& !LegendaryWeaponItem.cleaves(e);
		}
	}

	/**
	 * One a domain works on: an enemy of its owner's (HeavenSpells#enemy: a monster, anything set on it, a player at odds with it), or a
	 * cultivator at odds with it (one it struck, by blow or spell, or that struck it, in the last HeavenSpells#QUARREL_TICKS); never one its
	 * owner's spells spare. Narrower than a spell's mark (HeavenSpells#mark takes every cultivator not of its sect): a domain stands for
	 * minutes, and one carried through a city must not fall on its merchants and passers-by.
	 */
	public static boolean foeOf(ServerPlayer owner, LivingEntity e) {
		if (HeavenSpells.enemy(owner, e)) return true;
		if (!(e instanceof CultivatorNpc) || HeavenSpells.spared(owner, e)) return false;
		int quarrel = HeavenSpells.QUARREL_TICKS;
		return owner.getLastHurtMob() == e && owner.tickCount - owner.getLastHurtMobTimestamp() < quarrel
				|| e.getLastHurtByMob() == owner && e.tickCount - e.getLastHurtByMobTimestamp() < quarrel
				|| owner.getLastHurtByMob() == e && owner.tickCount - owner.getLastHurtByMobTimestamp() < quarrel;
	}

	/** Every domain lit, as this tick found them (the hooks between ticks ask these). */
	private static final List<Domain> LIT = new ArrayList<>();
	/** What those who see each player were last told of its domain. */
	private static final Map<UUID, Shown> SHOWN = new HashMap<>();
	private record Shown(Law law, float radius) {}
	/** Those whose armour a Metal domain has cut, till when (game time). */
	private static final Map<LivingEntity, Long> CUT = new HashMap<>();
	/** Those a Space domain has caught, and whose: they can't leave it while it stands. */
	private static final Map<LivingEntity, UUID> CAUGHT = new HashMap<>();

	// --- Who holds one, and how far it reaches ---

	/** Whether {@code c} has awakened {@code law}'s domain: an immortal (Laws#isImmortal) with a level of it comprehended. */
	public static boolean unlocked(PlayerCultivation c, Law law) {
		return Laws.isImmortal(c) && c.laws().level(law) >= 1;
	}

	/** The domain switched on in {@code c}'s abilities (awakened or not), or null. */
	private static Ability switchedOn(PlayerCultivation c) {
		for (Ability ability : ABILITIES) if (c.isAbilityEnabled(ability)) return ability;
		return null;
	}

	/** The Law of the domain {@code c} has lit, or null. */
	public static Law lit(PlayerCultivation c) {
		Ability on = switchedOn(c);
		return on != null && on.isUnlocked(c) ? on.law() : null;
	}

	/** How far {@code law}'s domain reaches for {@code c}: {@link #BASE_RADIUS} and {@link #RADIUS_PER_LEVEL} a level, the greater past True Immortal. */
	public static double radius(PlayerCultivation c, Law law) {
		Realm realm = Realm.byIndex(Math.max(0, TwoPaths.higherRank(c, false)) / Stage.values().length);
		return (BASE_RADIUS + RADIUS_PER_LEVEL * c.laws().level(law)) * (1 + RADIUS_PER_REALM * realm.pastTrueImmortal());
	}

	/** Its middle: its owner's. */
	public static Vec3 centre(Entity owner) {
		return HeavenSpells.middle(owner);
	}

	/** Slowness's strength in a domain of {@code law} at {@code level} (an amplifier): Time's the heavier. */
	public static int slowness(Law law, int level) {
		return law == Law.TIME ? 1 + level / 3 : level / 4;
	}

	/** What the domain's Ability says, with {@code c}'s numbers ({@code key}: its description's). */
	public static Component description(String key, Law law, PlayerCultivation c) {
		int level = Math.max(1, c.laws().level(law));
		long radius = Math.round(radius(c, law));
		long harm = Math.round((law == Law.FIRE ? FIRE_SHARE : HARM_SHARE) * 100), bonus = Math.round((SPELL_HARM - 1) * 100);
		String slow = roman(slowness(law, level) + 1);
		return switch (law) {
			case METAL -> Component.translatable(key, radius, BURN_SECONDS, bonus, harm, Math.round(ARMOUR_CUT_PER_LEVEL * level * 100));
			case WOOD -> Component.translatable(key, radius, BURN_SECONDS, bonus, harm, slow, Math.round(WOOD_DRAIN * 100));
			case WATER -> Component.translatable(key, radius, BURN_SECONDS, bonus, harm, slow, Math.round(FIRE_DOUSED_PER_LEVEL * level * 100));
			case FIRE -> Component.translatable(key, radius, BURN_SECONDS, bonus, harm);
			case EARTH -> Component.translatable(key, radius, BURN_SECONDS, bonus, harm, slow);
			case SPACE -> Component.translatable(key, radius, BURN_SECONDS, bonus, slow);
			case TIME -> Component.translatable(key, radius, BURN_SECONDS, slow, Math.round(TIME_STRETCH_PER_LEVEL * level * 100));
			case LIFE_AND_DEATH -> Component.translatable(key, radius, BURN_SECONDS, bonus, Math.round(LIFE_DRAIN_PER_LEVEL * level * 100));
		};
	}

	private static String roman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> String.valueOf(n);
		};
	}

	// --- Unfurled, folded away ---

	/** Its switch thrown (CultivationManager#toggleAbility, after the switch): unfurled (refused on too little essence), or folded away. */
	static void toggled(ServerPlayer p, Ability ability) {
		PlayerCultivation c = CultivationManager.get(p);
		Law law = ability.law();
		if (!c.isAbilityEnabled(ability)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_DOMAIN_OFF, law.getDisplayName()), true);
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 0.8f);
			show(p, null, 0);
			return;
		}
		double max = ImmortalEssence.max(c);
		if (!p.isCreative() && c.essence() < max / BURN_SECONDS * LIGHT_SECONDS) {
			c.setAbilityEnabled(ability, false);
			CultivationManager.sync(p);
			p.displayClientMessage(Component.translatable(ModLang.MSG_DOMAIN_TOO_LITTLE).withStyle(ChatFormatting.GOLD), true);
			show(p, null, 0);
			return;
		}
		String why = cantHold(p);
		if (why != null) {
			c.setAbilityEnabled(ability, false);
			CultivationManager.sync(p);
			p.displayClientMessage(Component.translatable(ModLang.MSG_DOMAIN_LOST, law.getDisplayName()), true);
			return;
		}
		long seconds = p.isCreative() ? BURN_SECONDS : Math.round(c.essence() / (max / BURN_SECONDS));
		p.displayClientMessage(Component.translatable(ModLang.MSG_DOMAIN_ON, law.getDisplayName(), Math.round(radius(c, law)), seconds)
				.withStyle(ChatFormatting.GOLD), true);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.7f);
		p.serverLevel().playSound(null, p.blockPosition(), voice(law), SoundSource.PLAYERS, 1.2f, 0.8f);
		show(p, law, (float) radius(c, law));
	}

	/** The sound its Law makes as it unfurls. */
	private static SoundEvent voice(Law law) {
		return switch (law) {
			case METAL -> SoundEvents.ANVIL_USE;
			case WOOD -> SoundEvents.ROOTED_DIRT_PLACE;
			case WATER -> SoundEvents.GENERIC_SPLASH;
			case FIRE -> SoundEvents.BLAZE_SHOOT;
			case EARTH -> SoundEvents.WARDEN_EMERGE;
			case SPACE -> SoundEvents.ENDER_EYE_DEATH;
			case TIME -> SoundEvents.AMETHYST_BLOCK_RESONATE;
			case LIFE_AND_DEATH -> SoundEvents.SOUL_ESCAPE;
		};
	}

	/** Why {@code p}'s domain can't stand now: "" for no word of it (dead, a spectator), a message for a soul out of its body or a passage; null when it may. */
	private static String cantHold(ServerPlayer p) {
		if (!p.isAlive() || p.isSpectator()) return "";
		if (SoulProjection.isSoul(p) || ModDimensions.isPassage(p.level().dimension())) return ModLang.MSG_DOMAIN_LOST;
		return null;
	}

	/** Folded away, told why ({@code why}: a message taking the Law's name; null: no word of it). */
	static void putOut(ServerPlayer p, String why) {
		PlayerCultivation c = CultivationManager.get(p);
		Ability on = switchedOn(c);
		if (on == null) return;
		c.setAbilityEnabled(on, false);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		show(p, null, 0);
		if (why != null) {
			p.sendSystemMessage(Component.translatable(why, on.law().getDisplayName()).withStyle(ChatFormatting.GOLD));
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 0.8f);
		}
	}

	/** A logout: it folds away (it isn't unfurled again by logging in). */
	public static void onDisconnect(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		Ability on = switchedOn(c);
		if (on != null) {
			c.setAbilityEnabled(on, false);
			CultivationManager.markDirty(p.server);
		}
		show(p, null, 0);
		SHOWN.remove(p.getUUID());
		LIT.removeIf(d -> d.owner() == p);
	}

	// --- Each tick ---

	/**
	 * Every tick: each player's domain as it stands (folded away where it can't hold, or with the immortal gone out of {@code c}), what
	 * those who see them are told of it, and the work of each one lit on the foes in it; what it held let go once it is gone.
	 */
	public static void tick(MinecraftServer server) {
		LIT.clear();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			PlayerCultivation c = CultivationManager.get(p);
			Ability on = switchedOn(c);
			if (on != null && !on.isUnlocked(c)) { // no longer an immortal: its switch put back, unseen
				c.setAbilityEnabled(on, false);
				CultivationManager.markDirty(server);
				CultivationManager.sync(p);
				on = null;
			}
			if (on != null) {
				String why = cantHold(p);
				if (why != null) {
					putOut(p, why.isEmpty() ? null : why);
					on = null;
				}
			}
			Law law = on == null ? null : on.law();
			double radius = law == null ? 0 : radius(c, law);
			if (law != null) LIT.add(new Domain(p, law, c.laws().level(law), centre(p), radius));
			show(p, law, (float) radius);
		}
		for (Domain d : LIT) work(d);
		letGo(server);
	}

	/** {@code d}'s work this tick: its hold on its foes renewed every {@link #PULSE}, its harm every {@link #HARM_EVERY}; Space's edge kept every tick. */
	private static void work(Domain d) {
		ServerPlayer owner = d.owner();
		ServerLevel level = owner.serverLevel();
		long now = level.getGameTime();
		if (d.law() == Law.SPACE) keepIn(d);
		long beat = now + owner.getId();
		if (beat % PULSE != 0) return;
		boolean harm = beat % HARM_EVERY == 0;
		int amp = slowness(d.law(), d.level());
		float blow = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
		for (LivingEntity e : HeavenSpells.aroundPoint(level, d.centre(), d.radius() + 3, d::holds)) {
			boolean great = HeavenSpells.greatBeast(owner, e);
			switch (d.law()) {
				case METAL -> cut(e, d.level(), now);
				case WOOD, WATER, EARTH, SPACE -> slow(owner, e, MobEffects.MOVEMENT_SLOWDOWN, amp);
				case TIME -> {
					slow(owner, e, MobEffects.MOVEMENT_SLOWDOWN, amp);
					slow(owner, e, MobEffects.DIG_SLOWDOWN, d.level() / 3); // its blows the slower too
				}
				default -> {}
			}
			if (d.law() == Law.WATER) e.clearFire();
			if (d.law() == Law.FIRE && e.getRemainingFireTicks() < 40) e.setSecondsOnFire(3);
			if (d.law() == Law.EARTH && !great) ground(e);
			if (d.law() == Law.SPACE && !great) CAUGHT.put(e, owner.getUUID());
			if (harm && d.law().element() != null) {
				float taken = HeavenSpells.strikeAgain(owner, e, blow * (d.law() == Law.FIRE ? FIRE_SHARE : HARM_SHARE), d.law().element());
				if (d.law() == Law.WOOD && taken > 0) HeavenSpells.mend(owner, taken * WOOD_DRAIN);
			}
		}
	}

	/** {@code effect} on {@code e} at {@code amp}, renewed while it stays (no swirl of it; its icon shown). */
	private static void slow(ServerPlayer owner, LivingEntity e, MobEffect effect, int amp) {
		e.addEffect(new MobEffectInstance(effect, PULSE + 20, amp, false, false, true), owner);
	}

	/** Metal: {@code e}'s armour cut by its level ({@link #ARMOUR_CUT_PER_LEVEL}; the deepest cut on it holds), till a little after it leaves. */
	private static void cut(LivingEntity e, int level, long now) {
		AttributeInstance armour = e.getAttribute(Attributes.ARMOR);
		if (armour == null) return;
		double amount = -ARMOUR_CUT_PER_LEVEL * level;
		AttributeModifier had = armour.getModifier(ARMOUR_ID);
		if (had == null || had.getAmount() > amount) {
			armour.removeModifier(ARMOUR_ID);
			armour.addTransientModifier(new AttributeModifier(ARMOUR_ID, "Law of Metal's domain", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
		CUT.put(e, now + PULSE + 5);
	}

	/** Earth: {@code e} brought down out of the air (a player's flight given up while it stays, a cultivator's ended), and spared the fall. */
	private static void ground(LivingEntity e) {
		if (e instanceof ServerPlayer p) QiFlight.weighDown(p, PULSE + 10);
		if (e instanceof CultivatorNpc npc && npc.isQiFlying()) npc.setQiFlying(false);
		if (!e.onGround() && !e.isInWater()) {
			Vec3 v = e.getDeltaMovement();
			HeavenSpells.move(e, new Vec3(v.x * 0.6, Math.min(v.y, 0) - 0.6, v.z * 0.6));
		}
		HeavenSpells.safeFall(e, 60);
	}

	/** Space: what it has caught kept within it, drawn back from its edge (put back inside, if it got out by more than a step). */
	private static void keepIn(Domain d) {
		ServerLevel level = d.owner().serverLevel();
		for (Iterator<Map.Entry<LivingEntity, UUID>> it = CAUGHT.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<LivingEntity, UUID> entry = it.next();
			if (!entry.getValue().equals(d.owner().getUUID())) continue;
			LivingEntity e = entry.getKey();
			if (!HeavenSpells.present(e, level) || HeavenSpells.spared(d.owner(), e)) {
				it.remove();
				continue;
			}
			Vec3 at = HeavenSpells.middle(e);
			double distance = at.distanceTo(d.centre());
			if (distance <= d.radius()) continue;
			if (distance > d.radius() + ESCAPED) {
				it.remove();
				continue;
			}
			Vec3 in = d.centre().subtract(at).scale(1 / Math.max(1.0e-3, distance));
			if (distance <= d.radius() + 1.5) {
				HeavenSpells.move(e, in.scale(0.6).add(0, Math.max(0, e.getDeltaMovement().y), 0));
			} else {
				Vec3 back = d.centre().add(at.subtract(d.centre()).scale((d.radius() - 1) / distance)).subtract(0, e.getBbHeight() / 2, 0);
				if (level.noCollision(e, e.getBoundingBox().move(back.subtract(e.position())))) HeavenSpells.teleport(e, back);
				else HeavenSpells.move(e, in.scale(0.9));
			}
		}
	}

	/** Cut armour healed once its domain is gone from it; those caught let go once theirs is folded away. */
	private static void letGo(MinecraftServer server) {
		if (!CUT.isEmpty()) {
			long now = server.overworld().getGameTime();
			for (Iterator<Map.Entry<LivingEntity, Long>> it = CUT.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<LivingEntity, Long> entry = it.next();
				LivingEntity e = entry.getKey();
				if (!e.isRemoved() && e.isAlive() && now < entry.getValue()) continue;
				AttributeInstance armour = e.getAttribute(Attributes.ARMOR);
				if (armour != null) armour.removeModifier(ARMOUR_ID);
				it.remove();
			}
		}
		if (!CAUGHT.isEmpty()) CAUGHT.values().removeIf(owner -> LIT.stream().noneMatch(d -> d.law() == Law.SPACE && d.owner().getUUID().equals(owner)));
	}

	// --- What the rest of the mod asks of them ---

	/**
	 * A blow on {@code target} from {@code source} (SoulLivingEntityMixin, after Physiques#outgoing): {@link #SPELL_HARM} times as hard for
	 * its Law's own spell of a domain's owner, on a foe within it; a foe's fire within a Water domain doused, {@link #FIRE_DOUSED_PER_LEVEL}
	 * a level weaker.
	 */
	public static float outgoing(LivingEntity target, DamageSource source, float amount) {
		if (LIT.isEmpty()) return amount;
		Entity by = source.getEntity();
		for (Domain d : LIT) {
			if (d.owner() == by && ofTheLaw(d.law(), source, d.owner()) && d.holds(target)) {
				amount *= SPELL_HARM;
				break;
			}
		}
		if (by instanceof LivingEntity attacker && (source.is(DamageTypeTags.IS_FIRE) || source.is(ModDamageTypes.qiType(QiElement.FIRE)))) {
			float doused = 0;
			for (Domain d : LIT) if (d.law() == Law.WATER && d.holds(attacker)) doused = Math.max(doused, FIRE_DOUSED_PER_LEVEL * d.level());
			amount *= 1 - Math.min(0.9f, doused);
		}
		return amount;
	}

	/** Whether {@code source} is a spell of {@code law}'s, cast by {@code caster}: its element's qi; a spell of space; the ghost path's harm (not a blow of the dead's). */
	static boolean ofTheLaw(Law law, DamageSource source, LivingEntity caster) {
		if (law.element() != null) return source.is(ModDamageTypes.qiType(law.element()));
		return switch (law) {
			case SPACE -> source instanceof QiDamageSource qi && qi.realm() == Realm.VOID_SHATTERING || Laws.castingSpatial(caster);
			case LIFE_AND_DEATH -> source.is(ModDamageTypes.GHOSTLY) && !GhostSystem.isBlow();
			default -> false;
		};
	}

	/** {@code victim} has lost {@code lost} to {@code source} (Physiques#onHurt): to a Life and Death domain's owner, in it, a share back as life. */
	public static void onHurt(LivingEntity victim, DamageSource source, float lost) {
		if (LIT.isEmpty() || lost <= 0 || !(source.getEntity() instanceof ServerPlayer attacker) || attacker == victim) return;
		for (Domain d : LIT) {
			if (d.owner() == attacker && d.law() == Law.LIFE_AND_DEATH && d.holds(victim)) {
				HeavenSpells.mend(attacker, lost * LIFE_DRAIN_PER_LEVEL * d.level());
				return;
			}
		}
	}

	/** Whether {@code e} is held from healing now (Laws#healed): a foe within a Life and Death domain. */
	public static boolean withholdsHealing(LivingEntity e) {
		if (LIT.isEmpty()) return false;
		for (Domain d : LIT) if (d.law() == Law.LIFE_AND_DEATH && d.holds(e)) return true;
		return false;
	}

	/** Whether space is locked on {@code e} (a foe within a Space domain): no step through it (Spells#cast), no tear in it (SpatialTrialHandler#tear). */
	public static boolean locksSpace(LivingEntity e) {
		if (LIT.isEmpty()) return false;
		for (Domain d : LIT) if (d.law() == Law.SPACE && d.holds(e)) return true;
		return false;
	}

	/** {@code ticks} of a spell's cooldown for {@code caster} (Spells#cast, #castBy): the longer within a Time domain whose owner's foe it is. */
	public static long stretched(LivingEntity caster, long ticks) {
		if (LIT.isEmpty()) return ticks;
		double factor = 1;
		for (Domain d : LIT) if (d.law() == Law.TIME && d.holds(caster)) factor = Math.max(factor, 1 + TIME_STRETCH_PER_LEVEL * d.level());
		return Math.round(ticks * factor);
	}

	/** The domains lit this tick (game tests). */
	static List<Domain> lit() {
		return List.copyOf(LIT);
	}

	// --- The Chaos Frontier (phase 3c) ---

	/**
	 * Whether {@code p} stands within a lit domain that shelters them from the Chaos Frontier's storm (ChaosFrontier#sheltered): their own,
	 * or one whose owner is no foe of theirs ({@link #foeOf}).
	 */
	public static boolean shelters(ServerPlayer p) {
		if (LIT.isEmpty()) return false;
		Vec3 at = HeavenSpells.middle(p);
		for (Domain d : LIT) {
			if (d.owner().level() != p.level() || d.owner() != p && foeOf(d.owner(), p)) continue;
			if (at.distanceToSqr(d.centre()) <= d.radius() * d.radius()) return true;
		}
		return false;
	}

	/** The Primordial Chaos's Unmaking burst at {@code at} out to {@code reach} (PrimordialBeast): each domain whose owner it catches folds away. @return how many */
	public static int unmake(ServerLevel level, Vec3 at, double reach) {
		int folded = 0;
		for (Domain d : List.copyOf(LIT)) {
			if (d.owner().level() != level || d.centre().distanceToSqr(at) > reach * reach) continue;
			putOut(d.owner(), ModLang.MSG_DOMAIN_UNMADE);
			LIT.remove(d);
			folded++;
		}
		return folded;
	}

	// --- Seen ---

	/** What those who see {@code p} are told of its domain: sent when it changes (a domain unfurled or folded, its reach grown). */
	private static void show(ServerPlayer p, Law law, float radius) {
		Shown was = SHOWN.get(p.getUUID());
		Shown now = new Shown(law, law == null ? 0 : radius);
		if (was == null ? law == null : was.law() == law && Math.abs(was.radius() - now.radius()) < 0.05f) return;
		SHOWN.put(p.getUUID(), now);
		for (ServerPlayer watcher : PlayerLookup.tracking(p)) ModPackets.sendLawDomain(watcher, p, law, now.radius());
		ModPackets.sendLawDomain(p, p, law, now.radius());
	}

	/** {@code viewer} has just begun to see {@code e}: told of its domain, if one is lit. */
	public static void startSeeing(ServerPlayer viewer, Entity e) {
		Shown shown = e instanceof ServerPlayer p ? SHOWN.get(p.getUUID()) : null;
		if (shown != null && shown.law() != null) ModPackets.sendLawDomain(viewer, e, shown.law(), shown.radius());
	}

	/** {@code p} has changed worlds: told of its own domain again (its client's world is new). */
	public static void resend(ServerPlayer p) {
		Shown shown = SHOWN.get(p.getUUID());
		if (shown != null && shown.law() != null) ModPackets.sendLawDomain(p, p, shown.law(), shown.radius());
	}

	/** The server stopping. */
	public static void clear() {
		LIT.clear();
		SHOWN.clear();
		CUT.clear();
		CAUGHT.clear();
	}

	private LawDomains() {}
}
