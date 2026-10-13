package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Cultivating the ghost methods (see GhostMethod): how fast, how far, and what each layer brings.
 * <p>
 * <b>The rate.</b> Meditating where ghostly qi flows, the active method gains progress at {@link #BASE_RATE} (a second a
 * second, for a living Nascent Soul Early in the Netherworld) x the density / {@link GhostQi#NETHERWORLD} x the realm's factor
 * ({@link #realmFactor}: x0.5 for a mortal's ghost; -10% a realm below Nascent Soul, +10% above) x Qi Harmony x a Yin
 * Gathering Pill x {@link #GHOST_FACTOR} for the dead (made of yin) x {@link #STELE_BONUS} by a stele of that method.
 * <p>
 * <b>The layers.</b> Progress is counted in seconds of that baseline meditation, so a layer's cost is its minutes x 60:
 * {@link #MINUTES} = 5, 8, 12, 20, 30, 45, 60, 90, 120 minutes for layers one to nine, about six and a half hours in all
 * (a ghost goes 1.5 times as fast; in a Soul Sand Valley, density 1, ten times slower).
 * <p>
 * <b>The cap.</b> Where the density is below the Netherworld's, progress stops at the end of layer {@link #OUTSIDE_LAYER_CAP}:
 * past it, only the Netherworld's ghostly qi can carry a method (the ghost track's own lower-realm cap).
 * <p>
 * <b>Learning.</b> Meditating within {@link #STELE_LEARN_RADIUS} of a Method Stele for {@link #STELE_LEARN_SECONDS} teaches
 * the first layer of its method; a Ghost Jade Slip, read, teaches it at once.
 */
public final class GhostMethods {
	public static final int LAYERS = 9;
	public static final int[] MINUTES = {5, 8, 12, 20, 30, 45, 60, 90, 120};
	public static final int OUTSIDE_LAYER_CAP = 3;
	public static final double BASE_RATE = 1.0;
	public static final double GHOST_FACTOR = 1.5;
	public static final double MORTAL_FACTOR = 0.5;
	public static final double REALM_STEP = 0.10;
	public static final double STELE_BONUS = 1.25;
	public static final double STELE_LEARN_RADIUS = 4, STELE_BONUS_RADIUS = 8;
	public static final double STELE_LEARN_SECONDS = 60;
	/** Each layer of Wandering Soul Steps quickens a ghost this much; each of Ghost Fire Scripture strengthens its blows this much. */
	public static final double STEPS_SPEED_PER_LAYER = 0.03, FIRE_DAMAGE_PER_LAYER = 0.10;
	/** Yin Wind Steps: this much swifter (and no harm from falls). */
	public static final double YIN_WIND_SPEED = 0.20;
	/** The abilities' upkeep from the ghostly qi pool, a second. */
	public static final double GHOST_EYE_COST = 0.5, VEIL_COST = 1.0, YIN_WIND_COST = 1.0, GHOST_FLIGHT_COST = 2.0;

	private static final UUID SPEED_ID = UUID.fromString("0f6d9a52-2c1e-4a37-8f0b-3b2e5d9c7a11");
	private static final UUID DAMAGE_ID = UUID.fromString("7a3c1e58-9d4b-4f62-b0a7-5c8e2d1f6b93");

	/** Seconds of baseline meditation to cultivate through layer {@code layer} (1 to 9). */
	public static double layerCost(int layer) {
		return MINUTES[Math.max(1, Math.min(LAYERS, layer)) - 1] * 60.0;
	}

	/** The realm's factor on the rate: x0.5 for a mortal; otherwise -10% a realm below Nascent Soul, +10% a realm above it. */
	public static double realmFactor(PlayerCultivation c) {
		if (c.isMortal()) return MORTAL_FACTOR;
		return 1 + REALM_STEP * (c.getRealm().ordinal() - Realm.NASCENT_SOUL.ordinal());
	}

	/** Progress a second at {@code density}, every factor but harmony, pill and stele (which the caller multiplies in). */
	public static double ratePerSecond(PlayerCultivation c, double density) {
		return BASE_RATE * density / GhostQi.NETHERWORLD * realmFactor(c) * (c.netherworld().ghost() ? GHOST_FACTOR : 1.0);
	}

	/**
	 * Adds {@code amount} of progress to {@code method}, layer by layer; stops at the end of layer three where the density is
	 * below the Netherworld's, and at the end of layer nine. Returns how many layers were gained (and whether it hit the cap,
	 * as a negative: -1 when held at the outside cap with nothing gained).
	 */
	public static int cultivate(NetherworldState s, GhostMethod method, double amount, double density) {
		int layer = s.layer(method);
		if (layer <= 0 || amount <= 0) return 0;
		double progress = s.progress(method) + amount;
		int gained = 0;
		boolean capped = false;
		while (progress >= layerCost(layer)) {
			if (layer >= LAYERS) {
				progress = layerCost(LAYERS); // perfected
				break;
			}
			if (layer >= OUTSIDE_LAYER_CAP && density < GhostQi.FULL_DENSITY) {
				progress = layerCost(layer); // held at the end of the third
				capped = true;
				break;
			}
			progress -= layerCost(layer);
			layer++;
			gained++;
		}
		s.setMethod(method, layer, progress);
		return gained > 0 ? gained : capped ? -1 : 0;
	}

	/** Whether {@code method} is held at the outside cap right now (end of layer three, density too thin). */
	public static boolean heldAtCap(NetherworldState s, GhostMethod method, double density) {
		int layer = s.layer(method);
		return layer == OUTSIDE_LAYER_CAP && density < GhostQi.FULL_DENSITY && s.progress(method) >= layerCost(layer);
	}

	/**
	 * Teaches {@code player} the first layer of {@code method} (nothing if they know it): the first method learned becomes the
	 * active one, and its spells go on the wheel as they come.
	 */
	public static boolean learn(ServerPlayer player, GhostMethod method) {
		NetherworldState s = CultivationManager.get(player).netherworld();
		if (s.knows(method)) return false;
		s.setMethod(method, 1, 0);
		if (s.activeMethod() == null) s.setActiveMethod(method);
		afterLayer(player, s);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_METHOD_LEARNED, method.getDisplayName()).withStyle(ChatFormatting.AQUA));
		player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.7f);
		Journal.note(player, "method:" + method.getId(), ModLang.JOURNAL_METHOD, Component.translatable(ModLang.JOURNAL_METHOD_TEXT,
				method.getDisplayName(), method.getGains()));
		CultivationManager.markDirty(player.server);
		Spells.sync(player);
		NetherworldManager.refresh(player);
		return true;
	}

	/** After layers change: spells newly in reach go on the wheel. */
	static void afterLayer(ServerPlayer player, NetherworldState s) {
		PlayerCultivation c = CultivationManager.get(player);
		for (Spell spell : Spell.values()) {
			if (spell.isKnown(c)) s.addToWheel(spell);
		}
	}

	// --- What the layers bring ---

	public static int breathingLayers(PlayerCultivation c) {
		return c.netherworld().layer(GhostMethod.NETHERWORLD_BREATHING);
	}

	/**
	 * A ghost's swiftness (Wandering Soul Steps' layers) and blows (Ghost Fire Scripture's), and Yin Wind Steps' speed while it is
	 * on (for the living too): transient modifiers, refreshed every second.
	 */
	static void applyModifiers(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		NetherworldState s = c.netherworld();
		boolean ghost = s.ghost();
		double speed = (ghost ? STEPS_SPEED_PER_LAYER * s.layer(GhostMethod.WANDERING_SOUL_STEPS) : 0)
				+ (c.isAbilityActive(Ability.YIN_WIND_STEPS) ? YIN_WIND_SPEED : 0);
		set(p, Attributes.MOVEMENT_SPEED, SPEED_ID, "Wandering Soul Steps", speed, AttributeModifier.Operation.MULTIPLY_BASE);
		double damage = ghost ? FIRE_DAMAGE_PER_LAYER * s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) : 0;
		set(p, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "Ghost Fire Scripture", damage, AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	private static void set(ServerPlayer p, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, String name, double amount,
							AttributeModifier.Operation operation) {
		AttributeInstance instance = p.getAttribute(attribute);
		if (instance == null) return;
		AttributeModifier current = instance.getModifier(id);
		if (amount == 0) {
			if (current != null) instance.removeModifier(id);
			return;
		}
		if (current != null && Math.abs(current.getAmount() - amount) < 1.0e-9) return;
		instance.removeModifier(id);
		instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
	}

	/** The ghostly qi the switched-on ghost abilities cost a second (see Ability). */
	static double upkeep(PlayerCultivation c) {
		double cost = 0;
		if (c.isAbilityActive(Ability.GHOST_EYE)) cost += GHOST_EYE_COST;
		if (c.isAbilityActive(Ability.NETHERWORLD_VEIL)) cost += VEIL_COST;
		if (c.isAbilityActive(Ability.YIN_WIND_STEPS)) cost += YIN_WIND_COST;
		if (c.isAbilityActive(Ability.GHOST_FLIGHT)) cost += GHOST_FLIGHT_COST;
		return cost;
	}

	/** The pool ran dry: every ghost ability switches off. */
	static void exhaust(ServerPlayer p, PlayerCultivation c) {
		boolean any = false;
		for (Ability ability : new Ability[]{Ability.GHOST_EYE, Ability.NETHERWORLD_VEIL, Ability.YIN_WIND_STEPS, Ability.GHOST_FLIGHT}) {
			if (c.isAbilityEnabled(ability)) {
				c.setAbilityEnabled(ability, false);
				any = true;
			}
		}
		if (any) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_QI_SPENT), true);
			CultivationManager.markDirty(p.server);
			CultivationManager.sync(p);
		}
	}

	private GhostMethods() {}
}
