package com.example.defyingtheheavens;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * A legendary weapon (Legend; phase 3a of the Artifact Forge: Ganjiang, Moye and Heaven's Tribulation Spear), the one true stack of it in the
 * world or a hollow echo (Legends#isTrue).
 * <ul>
 *   <li><b>Too heavy</b> for one below its lowest realm: it slips from their hand.</li>
 *   <li><b>Bonding.</b> Its powers answer only its bonded owner: one who meditates {@link #BOND_TICKS} with it in their main hand (the
 *   Tribulation Spear then turns its lightning on them: three bolts to survive, Legends#tame). Dropped at its owner's death, it is theirs
 *   no longer for whoever takes it: they must bond with it anew (the user's pick).</li>
 *   <li><b>Awakening</b> by its owner's realm (Legend#stage): might {@link #MIGHT} of their whole strike, its blow two more each stage,
 *   its powers stronger. It never wears out, burns, or is blown up; lost all the same, it reforms at home (Legends).</li>
 *   <li><b>Ganjiang</b> (yang): each blow adds fire qi and sets its foe burning. <b>Moye</b> (yin): each blow adds yin (the dead's own harm,
 *   passing armour) and chills; her blows reach the dead in full (GhostSystem#scaled). <b>Held together</b>, one in each hand, both
 *   bonded to you, every blow strikes twice, yang fire and yin cold. Their art, <b>Twin Dragons</b>: a spirit of each sword held flies out
 *   and fights for you a while (SwordSpirit).</li>
 *   <li><b>Heaven's Tribulation Spear</b>, a bolt of heavenly tribulation itself (drawn so: the client's TribulationSpearRenderer): each
 *   blow's lightning runs on into the foes nearest its target. Its art, <b>Heaven's Brand</b> ({@link #heavensBrand}): a tribulation bolt
 *   onto the foe its wielder looks at, and a player it strikes branded, their heavenly tribulations the heavier for good. Held out in its
 *   owner's own tribulations, it drinks every third of the heavens' bolts, none of them reaching its owner, and each drunk makes it the
 *   stronger for good ({@link #drink}, {@link #GROWTH}; the user's asks, 2026-10-28).</li>
 *   <li>Phase 3b's. The <b>Xuanyuan Sword</b>, the bane of evil, answers the righteous alone and leaves the devilish (flying home to its
 *   stone: Legends#leaves): each blow adds golden qi, twice over on the evil ({@link #evil}), and reaches the dead in full. Its art, <b>Yellow
 *   Emperor's Judgement</b> ({@link #judgement}): the sword falls vast out of the sky where its wielder looks (Colossus). The
 *   <b>Sun-Shooting Bow</b>, needing no arrows, looses arrows of sun qi as it is let go ({@link SunArrow}); drawn fully and let go sneaking,
 *   its art, <b>Falling Sun</b> ({@link #fallingSun}). The <b>Sea-Fixing Needle</b>, kept behind its owner's ear (never dropped at their death:
 *   Legends#keepBehindTheEar), throws what it strikes; its art, <b>As You Will</b>: it stretches out along where its wielder looks
 *   ({@link #stretch}), or, sneaking, grows to a mountain and comes down there ({@link #mountain}).</li>
 *   <li>Phase 3c's. <b>Pangu's Axe</b>: its blows pass every ward ({@link #cutsThroughWards}), no barrier holds against it nor a foe's Law
 *   domain on its wielder ({@link #cleaves}), and a domain whose owner it strikes folds; its art, <b>Split Heaven and Earth</b>
 *   ({@link #splitHeavenAndEarth}). The Celestial Court's <b>Venerables' five weapons</b>: each blow its element's qi and touch
 *   ({@link #symbolBlow}); each art its symbol's own Dao Integration spell ({@link #symbolArt}); won from their Venerables in Dao Duels
 *   (SparSystem.Terms#DUEL).</li>
 * </ul>
 * Its holders (Moye's ghost, Lei Zhen) fight with all of it (CultivatorNpc#doHurtTarget, LeiZhen), but for the brand: a player's alone.
 */
public class LegendaryWeaponItem extends SwordItem {
	public static final String SERIAL = "LegendSerial", BOND = "LegendBond", BOND_NAME = "LegendBondName", STAGE = "LegendStage", DRUNK = "LegendDrunk";
	/** Ticks of meditation with it in the main hand to bond with it (two minutes). */
	public static final int BOND_TICKS = 2400;
	/** What it adds to all its wielder's blows (a share of their whole strike), by stage: none woken, then its three awakenings. */
	static final double[] MIGHT = {0.20, 0.40, 0.55, 0.70};
	/** The share of its wielder's strike each blow adds as its own (Ganjiang's fire, Moye's yin, the spear's lightning on its target). */
	static final double[] SHARE = {0, 0.30, 0.40, 0.55};
	/** Ganjiang's burning (seconds) and Moye's chill (ticks), by stage. */
	static final int[] BURN = {0, 3, 4, 6}, CHILL = {0, 40, 60, 80};
	/** The spear's chain: how many foes past its target, how far it leaps, and their share of the strike. */
	static final int[] CHAIN = {0, 1, 2, 3};
	static final double[] CHAIN_REACH = {0, 4, 5, 6}, CHAIN_SHARE = {0, 0.35, 0.45, 0.60};
	/** Twin Dragons: its share of the pool, how long before it may be called again, how long its spirits fight (ticks), their strike's share. */
	public static final double DRAGONS_COST = 0.08;
	public static final int DRAGONS_COOLDOWN = 300;
	static final int[] DRAGON_LIFE = {0, 120, 180, 240};
	static final double[] DRAGON_SHARE = {0, 0.45, 0.55, 0.70};
	/**
	 * Heaven's Brand, the Tribulation Spear's art: its share of the pool, its wait, how far it reaches; its bolt's breadth and harm (a share
	 * of the strike); and the brand it lays on a player it strikes, by stage: that many percent more bolts in every heavenly tribulation they
	 * face, for good, {@link #BRAND_MOST} at most (twice the bolts).
	 */
	public static final double BRAND_COST = 0.10;
	public static final int BRAND_COOLDOWN = 120;
	static final double[] BRAND_REACH = {0, 24, 32, 40}, BOLT_RADIUS = {0, 2.5, 3.0, 3.5}, BOLT_POWER = {0, 1.2, 1.5, 2.0};
	public static final int[] BRAND = {0, 10, 15, 20};
	public static final int BRAND_MOST = 100;
	/**
	 * What each bolt the Tribulation Spear drinks in its owner's tribulations adds to its might, for good (the user's ask: "this should
	 * permanently increase its power slightly"): a hundredth for every five hundred, so a True Immortal's tribulation (3,200 bolts, a third of
	 * them drunk) leaves it some two hundredths the stronger.
	 */
	public static final double GROWTH = 0.00002;
	/** A taming bolt's share of its target's whole health. */
	public static final float TAMING_BOLT = 0.25f;
	/** The share of the pool each of phase 3b's arts asks (Judgement, Falling Sun, As You Will). */
	public static final double ART_COST = 0.10;
	/**
	 * The Xuanyuan Sword: how many times over its golden qi lands on the evil (#evil); its art, Yellow Emperor's Judgement: its wait, how far
	 * it reaches, its breadth and harm (a share of the strike), and how vast the sword that falls (times a held one), by stage.
	 */
	static final double EVIL_TIMES = 2;
	public static final int JUDGEMENT_COOLDOWN = 160;
	static final double[] JUDGEMENT_REACH = {0, 24, 32, 40}, JUDGEMENT_RADIUS = {0, 4, 5, 6}, JUDGEMENT_POWER = {0, 1.5, 2.0, 2.6};
	static final float[] JUDGEMENT_SIZE = {0, 10, 13, 16};
	/**
	 * The Sun-Shooting Bow: a full-drawn arrow's harm (a share of the strike) and the fire qi it adds; how long a full draw takes, and an
	 * arrow's speed; its art, Falling Sun: its wait, how far it reaches, its breadth and harm, by stage.
	 */
	static final double[] ARROW_POWER = {0, 0.9, 1.1, 1.4}, SUNFIRE_SHARE = {0, 0.30, 0.40, 0.55};
	static final int FULL_DRAW = 20;
	static final float ARROW_SPEED = 3.0f;
	public static final int FALLING_SUN_COOLDOWN = 160;
	static final double[] SUN_REACH = {0, 32, 40, 48}, SUN_RADIUS = {0, 4, 5, 6}, SUN_POWER = {0, 1.4, 1.8, 2.4};
	/**
	 * The Sea-Fixing Needle: how hard its blows throw; its art, As You Will: stretched out (its wait, reach, breadth and harm), or grown to a
	 * mountain (its wait, reach, breadth, harm, and how vast), by stage.
	 */
	static final double[] NEEDLE_KNOCK = {0, 0.8, 1.1, 1.5};
	public static final int STRETCH_COOLDOWN = 120, MOUNTAIN_COOLDOWN = 200;
	static final double[] STRETCH_REACH = {0, 24, 32, 48}, STRETCH_POWER = {0, 1.2, 1.5, 2.0};
	static final double STRETCH_BREADTH = 1.5;
	static final double[] MOUNTAIN_REACH = {0, 24, 32, 40}, MOUNTAIN_RADIUS = {0, 5, 6, 8}, MOUNTAIN_POWER = {0, 1.6, 2.1, 2.8};
	static final float[] MOUNTAIN_SIZE = {0, 18, 24, 32};
	/**
	 * Pangu's Axe's art, Split Heaven and Earth: its wait, how far its cleave runs along the land and how hard it strikes (a share of the
	 * strike), by stage; how near the line it strikes; and how wide and deep it splits the land (a player's: HeavenSpells#carvesLand).
	 */
	public static final int SPLIT_COOLDOWN = 240;
	static final double[] SPLIT_REACH = {0, 32, 48, 64}, SPLIT_POWER = {0, 2.0, 2.6, 3.4};
	static final double SPLIT_BREADTH = 2.5, SPLIT_WIDTH = 2.5, SPLIT_DEPTH = 8;
	/**
	 * The Venerables' weapons: their arts' wait (each its symbol's own Dao Integration spell: Legend#symbolSpell); their blows' touch of
	 * their element, by stage: wood's roots (slowness, ticks), fire's burning (seconds), water's chill (ticks) and throw, earth's stagger
	 * (ticks); metal's edge cutting the deeper (its share the more).
	 */
	public static final int SYMBOL_COOLDOWN = 400;
	static final int[] ROOTS = {0, 40, 60, 80}, SCORCH = {0, 4, 5, 7}, CHILL_TIDE = {0, 40, 60, 80}, STAGGER = {0, 30, 40, 60};
	static final double METAL_EDGE = 1.3, TIDE_THROW = 0.9;
	private static final UUID MIGHT_ID = UUID.fromString("8c5a1f2e-3d47-4b9a-9e61-0f2a7c4d8b13");
	/** When each wielder last struck with it (game time): its powers once a full swing at most. */
	private static final Map<LivingEntity, Long> LAST_STRIKE = new WeakHashMap<>();
	/** When each archer's Falling Sun may fall again (game time; not saved: a restart's wait begins anew). */
	private static final Map<UUID, Long> SUN_READY = new HashMap<>();

	private final Legend legend;
	/** Its blow, swing and might in a main hand, by stage. */
	private final List<Multimap<Attribute, AttributeModifier>> modifiers = new ArrayList<>();

	public LegendaryWeaponItem(Legend legend, Properties properties) {
		super(Unbreakable.TIER, legend.blow() - 1, (float) legend.speed() - 4, properties.fireResistant().rarity(Rarity.EPIC));
		this.legend = legend;
		for (int stage = 0; stage <= 3; stage++) modifiers.add(modifiers(stage));
	}

	public Legend legend() { return legend; }

	private Multimap<Attribute, AttributeModifier> modifiers(int stage) {
		return modifiers(stage, 0);
	}

	/** Its blow, swing and might at {@code stage}, the might grown by {@code drunk} bolts (the Tribulation Spear's, {@link #GROWTH}). */
	private Multimap<Attribute, AttributeModifier> modifiers(int stage, int drunk) {
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", legend.blow() + 2 * Math.max(0, stage - 1) - 1,
				AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", legend.speed() - 4, AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(MIGHT_ID, "Legendary might", MIGHT[stage] + drunk * GROWTH,
				AttributeModifier.Operation.MULTIPLY_TOTAL));
		return builder.build();
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND ? modifiers.get(0) : super.getDefaultAttributeModifiers(slot);
	}

	/** Its blow by how far it has woken in its owner's hands (the stage kept on the stack, #inventoryTick), and the bolts it has drunk. */
	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, EquipmentSlot slot) {
		if (slot != EquipmentSlot.MAINHAND) return getDefaultAttributeModifiers(slot);
		int drunk = drunk(stack);
		return drunk > 0 ? modifiers(stage(stack), drunk) : modifiers.get(stage(stack));
	}

	// --- The stack ---

	/** The true stack of {@code legend} made with {@code serial} (Legends#issue), with the bolts it has drunk (the Tribulation Spear's). */
	static ItemStack create(Legend legend, int serial, int drunk) {
		ItemStack stack = new ItemStack(ModItems.legend(legend));
		stack.getOrCreateTag().putInt(SERIAL, serial);
		if (drunk > 0) stack.getOrCreateTag().putInt(DRUNK, drunk);
		return stack;
	}

	public static int serial(ItemStack stack) {
		return stack.hasTag() ? stack.getTag().getInt(SERIAL) : 0;
	}

	public static int stage(ItemStack stack) {
		return stack.hasTag() ? Math.max(0, Math.min(3, stack.getTag().getInt(STAGE))) : 0;
	}

	/** The bolts the Tribulation Spear has drunk, as its stack shows them (the record's own: Legends.Entry#drunk). */
	public static int drunk(ItemStack stack) {
		return stack.hasTag() ? Math.max(0, stack.getTag().getInt(DRUNK)) : 0;
	}

	public static @Nullable UUID bondOf(ItemStack stack) {
		return stack.hasTag() && stack.getTag().hasUUID(BOND) ? stack.getTag().getUUID(BOND) : null;
	}

	public static boolean bondedTo(ItemStack stack, Entity who) {
		return who.getUUID().equals(bondOf(stack));
	}

	/** {@code player}'s realm for wielding: the highest of their paths (null: a mortal on none). */
	static @Nullable Realm realmOf(Player player) {
		int rank = TwoPaths.higherRank(CultivationManager.forPlayer(player), false);
		return rank < 0 ? null : Realm.byIndex(rank / Stage.values().length);
	}

	/** Whether {@code player} is strong enough to hold {@code legend}. */
	public static boolean mayWield(ServerPlayer player, Legend legend) {
		Realm realm = realmOf(player);
		return player.isCreative() || realm != null && realm.ordinal() >= legend.lowest().ordinal();
	}

	/** {@code stack} bonded to {@code player}: theirs, woken as far as their realm allows, the record told. */
	static void bond(ItemStack stack, ServerPlayer player) {
		if (!(stack.getItem() instanceof LegendaryWeaponItem weapon)) return;
		CompoundTag tag = stack.getOrCreateTag();
		tag.putUUID(BOND, player.getUUID());
		tag.putString(BOND_NAME, player.getGameProfile().getName());
		Realm realm = realmOf(player);
		tag.putInt(STAGE, realm == null ? 0 : weapon.legend.stage(realm));
		Legends.bonded(player, weapon.legend);
		Legends.seen(player, weapon.legend);
		Milestones.award(player, Milestones.LEGEND_BONDED);
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.2f);
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
		player.sendSystemMessage(Component.translatable(ModLang.LEGEND_BONDED, weapon.legend.getDisplayName()).withStyle(ChatFormatting.GOLD));
	}

	/** The true stack of {@code legend} on {@code player} (either hand, or anywhere in their inventory), or empty. */
	static ItemStack carried(ServerPlayer player, Legend legend) {
		for (ItemStack stack : player.getInventory().items) if (isTrueOf(player, stack, legend)) return stack;
		for (ItemStack stack : player.getInventory().offhand) if (isTrueOf(player, stack, legend)) return stack;
		return ItemStack.EMPTY;
	}

	private static boolean isTrueOf(ServerPlayer player, ItemStack stack, Legend legend) {
		return stack.getItem() instanceof LegendaryWeaponItem weapon && weapon.legend == legend && Legends.isTrue(player.server, stack);
	}

	/**
	 * How far {@code stack}'s powers answer {@code holder}: a holder of the world's (Moye's ghost, Lei Zhen) all of them; its bonded owner by
	 * how far it has woken; anyone else, or a hollow echo, none.
	 */
	static int powers(ItemStack stack, LivingEntity holder) {
		if (!Legends.isTrue(holder.level().getServer(), stack)) return 0;
		if (holder instanceof CultivatorNpc) return 3;
		if (!bondedTo(stack, holder)) return 0;
		// The Xuanyuan Sword answers only while its owner walks the righteous path.
		if (stack.getItem() instanceof LegendaryWeaponItem weapon && weapon.legend == Legend.XUANYUAN && holder instanceof Player player && !righteous(player)) return 0;
		return stage(stack);
	}

	// --- Held ---

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, level, entity, slot, selected);
		if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player) || player.isSpectator()) return;
		if (!Legends.isTrue(server.getServer(), stack)) {
			// A hollow echo (a copy, or one left behind by its legend reforming): shown so, and nothing of a woken one's might or owner kept.
			if (serial(stack) > 0) {
				CompoundTag tag = stack.getOrCreateTag();
				tag.putInt(SERIAL, 0);
				for (String key : new String[] {STAGE, DRUNK, BOND, BOND_NAME}) tag.remove(key);
			}
			return;
		}
		long now = server.getGameTime();
		if (now % 100 == 0) Legends.seen(player, legend);
		// The Xuanyuan Sword will not stay with the devilish: it leaves them, flying home to its stone.
		if (legend == Legend.XUANYUAN && devilish(player)) {
			Legends.leaves(player, stack);
			return;
		}
		// The bolts it has drunk, as the record keeps them (the Tribulation Spear's: drunk in its owner's tribulations, #drink).
		if (now % 20 == 0 && legend == Legend.TRIBULATION_SPEAR) {
			int drunk = Legends.get(server.getServer()).entry(legend).drunk();
			if (drunk != drunk(stack)) stack.getOrCreateTag().putInt(DRUNK, drunk);
		}
		// Which hand by the stack itself: vanilla ticks the offhand as "selected" too while the hotbar's first slot is chosen.
		boolean main = player.getMainHandItem() == stack, inHand = main || player.getOffhandItem() == stack;
		// Too heavy for one below its lowest realm: it slips from their hand.
		if (inHand && !mayWield(player, legend)) {
			slip(player, stack, main);
			if (now % 40 == 0) {
				player.displayClientMessage(Component.translatable(ModLang.LEGEND_TOO_HEAVY, legend.getDisplayName(), legend.lowest().getDisplayName()), true);
			}
			return;
		}
		// Woken as far as its owner's realm allows.
		if (now % 20 == 0 && bondedTo(stack, player)) {
			Realm realm = realmOf(player);
			int stage = realm == null ? 0 : legend.stage(realm);
			if (stage != stage(stack)) {
				boolean rose = stage > stage(stack);
				stack.getOrCreateTag().putInt(STAGE, stage);
				if (rose) {
					player.sendSystemMessage(Component.translatable(ModLang.LEGEND_AWAKENS, legend.getDisplayName(), stage).withStyle(ChatFormatting.GOLD));
					server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.0f);
				}
			}
		}
		// Bonding: meditating with it in the main hand.
		if (main && !bondedTo(stack, player) && MeditationManager.isMeditating(player.getUUID()) && !Legends.taming(player)) {
			// The Xuanyuan Sword answers only the righteous.
			if (legend == Legend.XUANYUAN && !righteous(player)) {
				if (now % 40 == 0) player.displayClientMessage(Component.translatable(ModLang.LEGEND_XUANYUAN_REFUSES, legend.getDisplayName()), true);
				return;
			}
			int ticks = Legends.bonding(player, legend);
			if (ticks % 40 == 0) {
				player.displayClientMessage(Component.translatable(ModLang.LEGEND_BONDING, legend.getDisplayName(), Math.min(100, ticks * 100 / BOND_TICKS)), true);
				server.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.4, 0.4, 0.4, 0.4);
			}
			if (ticks >= BOND_TICKS) {
				Legends.bonded(player, legend);
				if (legend == Legend.TRIBULATION_SPEAR) Legends.tame(player);
				else bond(stack, player);
			}
		}
	}

	/** A server stopping: the bow's wait forgotten (game times mean nothing in the next world opened). */
	static void clear() {
		SUN_READY.clear();
	}

	/** {@code stack} slipping from {@code player}'s hand into their pack, or to the ground if it is full. */
	private static void slip(ServerPlayer player, ItemStack stack, boolean main) {
		ItemStack moved = stack.copy();
		if (main) player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
		else player.getInventory().offhand.set(0, ItemStack.EMPTY);
		for (int i = 9; i < 36; i++) {
			if (player.getInventory().getItem(i).isEmpty()) {
				player.getInventory().setItem(i, moved);
				return;
			}
		}
		player.drop(moved, false);
	}

	/** Never stacked with another (each is one of a kind), and the stage's change never makes it bob in the hand. */
	@Override
	public boolean allowNbtUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
		return false;
	}

	/** Nothing harms it lying on the ground: no fire, lava, cactus or blast (lost all the same, it reforms: Legends). */
	@Override
	public boolean canBeHurtBy(DamageSource source) {
		return false;
	}

	@Override
	public void onDestroyed(ItemEntity item) {
		if (item.level() instanceof ServerLevel level) Legends.lost(level.getServer(), item.getItem());
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return false;
	}

	// --- Its blows ---

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker.level() instanceof ServerLevel level && !(attacker instanceof CultivatorNpc)) {
			int stage = powers(stack, attacker);
			if (stage > 0 && mayStrike(level, attacker)) strike(level, stack, target, attacker, stage);
		}
		return super.hurtEnemy(stack, target, attacker);
	}

	/** A holder of the world's blow landed (CultivatorNpc#doHurtTarget): its legend's powers with it, as a bonded owner's. */
	public static void npcStrike(CultivatorNpc npc, LivingEntity target) {
		ItemStack stack = npc.getMainHandItem();
		if (!(stack.getItem() instanceof LegendaryWeaponItem weapon) || !(npc.level() instanceof ServerLevel level)) return;
		int stage = powers(stack, npc);
		if (stage > 0 && weapon.mayStrike(level, npc)) weapon.strike(level, stack, target, npc, stage);
	}

	private boolean mayStrike(ServerLevel level, LivingEntity attacker) {
		Long last = LAST_STRIKE.get(attacker);
		long now = level.getGameTime();
		if (last != null && now - last < Math.round(20 / legend.speed()) - 1) return false;
		LAST_STRIKE.put(attacker, now);
		return true;
	}

	/** Its powers on {@code target}, struck by {@code attacker}, woken to {@code stage}. */
	void strike(ServerLevel level, ItemStack stack, LivingEntity target, LivingEntity attacker, int stage) {
		double strike = ElementalWeaponItem.attackOf(attacker);
		Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
		switch (legend) {
			case GANJIANG, MOYE -> {
				yangOrYin(level, legend, target, attacker, strike * SHARE[stage], stage, at);
				// Held together, both bonded to the one who holds them (or a holder of the world's): every blow strikes twice.
				ItemStack other = attacker.getOffhandItem();
				if (other.getItem() instanceof LegendaryWeaponItem pair && pair.legend == legend.pair() && powers(other, attacker) > 0) {
					yangOrYin(level, pair.legend, target, attacker, strike * SHARE[Math.max(stage, powers(other, attacker))], stage, at);
				}
			}
			case TRIBULATION_SPEAR -> chain(level, target, attacker, strike, stage);
			case XUANYUAN -> {
				// Golden qi, twice over on what it is the bane of (#evil).
				boolean bane = evil(target);
				ElementalWeaponItem.qiHit(level, attacker, target, QiElement.METAL, strike * SHARE[stage] * (bane ? EVIL_TIMES : 1));
				SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, at, Vec3.ZERO, bane ? 0.9f : 0.5f, 0);
				if (bane) level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 12, 0.3, 0.4, 0.3, 0.06);
			}
			case SUN_BOW -> {
				// Struck with the bow itself: its sun's fire, a little.
				if (ElementalWeaponItem.qiHit(level, attacker, target, QiElement.FIRE, strike * SUNFIRE_SHARE[stage] * 0.5)) target.setSecondsOnFire(3);
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, at, Vec3.ZERO, 0.4f, 0);
			}
			case SEA_NEEDLE -> {
				// Its weight: earth qi, and a blow that throws.
				ElementalWeaponItem.qiHit(level, attacker, target, QiElement.EARTH, strike * SHARE[stage]);
				Vec3 away = target.position().subtract(attacker.position()).multiply(1, 0, 1);
				away = away.lengthSqr() < 1.0e-4 ? attacker.getLookAngle().multiply(1, 0, 1) : away;
				away = away.normalize().scale(NEEDLE_KNOCK[stage]);
				target.push(away.x, 0.25 * NEEDLE_KNOCK[stage], away.z);
				target.hurtMarked = true;
				SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, at, Vec3.ZERO, 0.6f, 0);
				level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35f, 0.7f);
			}
			case PANGU_AXE -> {
				// Its edge: metal qi past every ward (#cutsThroughWards); a Law domain whose owner it lands on folds away.
				ElementalWeaponItem.qiHit(level, attacker, target, QiElement.METAL, strike * SHARE[stage]);
				if (target instanceof ServerPlayer struck) LawDomains.unmake(level, HeavenSpells.middle(struck), 0.5);
				SpellFx.send(level, SpellFx.Kind.RIFT_TEAR, at, Vec3.ZERO, 0.6f, 0);
				level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 0.5f);
			}
			case AZURE_GLAIVE, WHITE_TIGER_SABER, VERMILION_FAN, TORTOISE_MACE, YELLOW_DRAGON_SEAL -> symbolBlow(level, target, attacker, strike, stage, at);
			default -> {}
		}
	}

	/** A Venerable's weapon's blow: its element's qi (metal's the deeper) and that element's touch on its foe. */
	private void symbolBlow(ServerLevel level, LivingEntity target, LivingEntity attacker, double strike, int stage, Vec3 at) {
		QiElement element = legend.venerable();
		boolean landed = ElementalWeaponItem.qiHit(level, attacker, target, element, strike * SHARE[stage] * (element == QiElement.METAL ? METAL_EDGE : 1));
		switch (element) {
			case WOOD -> {
				if (landed) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ROOTS[stage], 1), attacker);
				SpellFx.send(level, SpellFx.Kind.LEAVES, at, Vec3.ZERO, 0.6f, 0);
			}
			case FIRE -> {
				if (landed) target.setSecondsOnFire(SCORCH[stage]);
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, at, Vec3.ZERO, 0.6f, 0);
			}
			case WATER -> {
				if (landed) {
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHILL_TIDE[stage], 0), attacker);
					Vec3 away = target.position().subtract(attacker.position()).multiply(1, 0, 1);
					away = away.lengthSqr() < 1.0e-4 ? attacker.getLookAngle().multiply(1, 0, 1) : away;
					away = away.normalize().scale(TIDE_THROW);
					target.push(away.x, 0.25, away.z);
					target.hurtMarked = true;
				}
				SpellFx.send(level, SpellFx.Kind.SPLASH, at, Vec3.ZERO, 0.6f, 0);
			}
			case EARTH -> {
				if (landed) {
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STAGGER[stage], 2), attacker);
					// A flier brought down out of the air.
					if (!target.onGround()) {
						target.setDeltaMovement(target.getDeltaMovement().x, -1.2, target.getDeltaMovement().z);
						target.hurtMarked = true;
					}
				}
				SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, at, Vec3.ZERO, 0.6f, 0);
			}
			default -> SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, at, Vec3.ZERO, 0.8f, 0);
		}
	}

	/**
	 * A Venerable's weapon's art: its symbol's own Dao Integration spell cast through it (Legend#symbolSpell), as its wielder's own realm
	 * casts it (its reach and size by realm: HeavenSpells#span). The caller asks HeavenSpells#canCast first.
	 */
	static void symbolArt(LivingEntity caster, Legend legend) {
		Spell spell = legend.symbolSpell();
		if (spell == null) return;
		HeavenSpells.castSeal(caster, spell);
		HeavenSpells.cast(caster, spell);
	}

	/**
	 * Whether {@code being} wields Pangu's Axe (its true one in hand, answering it): no barrier holds against it, nor a foe's Law domain. A
	 * client, which can't ask the record, goes by the stack's own bond and waking (a barrier's breaking is foreseen there:
	 * SectBarrierBlock#getDestroyProgress).
	 */
	public static boolean cleaves(LivingEntity being) {
		if (being == null) return false;
		ItemStack held = being.getMainHandItem();
		if (!(held.getItem() instanceof LegendaryWeaponItem weapon) || weapon.legend != Legend.PANGU_AXE) return false;
		if (being.level().isClientSide) return bondedTo(held, being) && stage(held) > 0 && serial(held) > 0;
		return powers(held, being) > 0;
	}

	/** Whether a blow of {@code source} is Pangu's Axe's (its wielder's own hand, the axe in it, answering them): no ward turns any of it. */
	public static boolean cutsThroughWards(DamageSource source) {
		Entity attacker = source.getEntity();
		return attacker instanceof LivingEntity living && source.getDirectEntity() == attacker && cleaves(living);
	}

	/**
	 * Whether {@code e} is of what the Xuanyuan Sword is the bane of: the devilish path, the demons (the Demon Path, the demon races, the demon
	 * beasts, a heart demon), and the dead.
	 */
	static boolean evil(LivingEntity e) {
		if (GhostSystem.isDead(e) || e instanceof DemonBeast || e instanceof HeartDemonEntity) return true;
		if (e instanceof CultivatorNpc npc) return npc.isDemon() || Alignment.factionOf(npc.getAlignment()) == Alignment.Faction.DEVILISH;
		if (e instanceof ServerPlayer player) {
			PlayerCultivation c = CultivationManager.get(player);
			return TwoPaths.cultivatesDemon(c) || Alignment.factionOf(c.getAlignment()) == Alignment.Faction.DEVILISH;
		}
		return false;
	}

	/** Whether {@code player} walks the righteous path (the only ones the Xuanyuan Sword answers), and whether the devilish (it leaves them). */
	static boolean righteous(Player player) {
		return Alignment.factionOf(CultivationManager.forPlayer(player).getAlignment()) == Alignment.Faction.RIGHTEOUS;
	}

	static boolean devilish(Player player) {
		return Alignment.factionOf(CultivationManager.forPlayer(player).getAlignment()) == Alignment.Faction.DEVILISH;
	}

	/** Ganjiang's yang (fire qi, burning) or Moye's yin (the dead's own harm, a chill) on {@code target}. */
	static void yangOrYin(ServerLevel level, Legend legend, LivingEntity target, LivingEntity attacker, double amount, int stage, Vec3 at) {
		if (legend == Legend.GANJIANG) {
			if (ElementalWeaponItem.qiHit(level, attacker, target, QiElement.FIRE, amount)) target.setSecondsOnFire(BURN[stage]);
			SpellFx.send(level, SpellFx.Kind.FLAME_BURST, at, Vec3.ZERO, 0.6f, 0);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.3f);
		} else {
			target.invulnerableTime = 0;
			if (target.hurt(ModDamageTypes.ghostly(level, attacker), (float) amount)) {
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHILL[stage], 0), attacker);
			}
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 10, 0.25, 0.3, 0.25, 0.02);
			level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.03);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.7f, 0.6f);
		}
	}

	/** The spear's lightning: its share into the target, then leaping on to the nearest foes about it, a lesser share each. */
	static void chain(ServerLevel level, LivingEntity target, LivingEntity attacker, double strike, int stage) {
		lightning(level, attacker, target, strike * SHARE[stage]);
		Vec3 from = target.position().add(0, target.getBbHeight() * 0.5, 0);
		double reach = CHAIN_REACH[stage];
		List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(reach),
				e -> e != target && Artifacts.mayStrike(attacker, e) && e.distanceToSqr(target) <= reach * reach);
		near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(target)));
		for (int i = 0; i < Math.min(CHAIN[stage], near.size()); i++) {
			LivingEntity next = near.get(i);
			Vec3 to = next.position().add(0, next.getBbHeight() * 0.5, 0);
			SpellFx.send(level, SpellFx.Kind.STREAK, from, to, 0, 0xCFE4FF);
			lightning(level, attacker, next, strike * CHAIN_SHARE[stage]);
			from = to;
		}
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 1.6f);
	}

	/** {@code amount} of tribulation lightning from {@code attacker} into {@code target}: heaven's own, passing armour. */
	static void lightning(ServerLevel level, LivingEntity attacker, LivingEntity target, double amount) {
		if (amount <= 0 || !target.isAlive()) return;
		target.invulnerableTime = 0;
		target.hurt(level.damageSources().indirectMagic(attacker, attacker), (float) amount);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 14, 0.3, 0.5, 0.3, 0.2);
	}

	// --- Its art ---

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
		if (!(player instanceof ServerPlayer sp)) {
			// The bow drawn on its owner's own side too, as the server will (else their letting go never reaches it).
			if (legend == Legend.SUN_BOW && bondedTo(stack, player) && stage(stack) > 0 && serial(stack) > 0) player.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}
		if (!Legends.isTrue(sp.server, stack)) {
			sp.displayClientMessage(Component.translatable(ModLang.LEGEND_HOLLOW, legend.getDisplayName()), true);
			return InteractionResultHolder.fail(stack);
		}
		int stage = powers(stack, sp);
		if (stage <= 0) {
			sp.displayClientMessage(Component.translatable(ModLang.LEGEND_NOT_BONDED, legend.getDisplayName()), true);
			return InteractionResultHolder.fail(stack);
		}
		ServerLevel server = sp.serverLevel();
		// The bow is drawn (its arrows and its art loosed as it is let go: #releaseUsing).
		if (legend == Legend.SUN_BOW) {
			sp.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}
		// Heaven's Brand falls on someone, or on nobody at all: with none where its wielder looks, it waits (nothing spent).
		LivingEntity branded = legend == Legend.TRIBULATION_SPEAR ? aimed(server, sp, BRAND_REACH[stage]) : null;
		if (legend == Legend.TRIBULATION_SPEAR && branded == null) {
			sp.displayClientMessage(Component.translatable(ModLang.LEGEND_BRAND_NONE, Math.round(BRAND_REACH[stage])), true);
			return InteractionResultHolder.fail(stack);
		}
		// A Venerable's weapon's spell takes hold of something first (HeavenSpells#canCast says why not), or nothing is spent.
		if (legend.venerable() != null && !HeavenSpells.canCast(sp, legend.symbolSpell())) return InteractionResultHolder.fail(stack);
		if (!spend(sp, legend == Legend.GANJIANG || legend == Legend.MOYE ? DRAGONS_COST : ART_COST)) return InteractionResultHolder.fail(stack);
		switch (legend) {
			case PANGU_AXE -> {
				splitHeavenAndEarth(server, sp, stage);
				sp.getCooldowns().addCooldown(this, SPLIT_COOLDOWN);
			}
			case AZURE_GLAIVE, WHITE_TIGER_SABER, VERMILION_FAN, TORTOISE_MACE, YELLOW_DRAGON_SEAL -> {
				symbolArt(sp, legend);
				sp.getCooldowns().addCooldown(this, SYMBOL_COOLDOWN);
			}
			case TRIBULATION_SPEAR -> {
				heavensBrand(server, sp, branded, stage);
				sp.getCooldowns().addCooldown(this, BRAND_COOLDOWN);
			}
			case XUANYUAN -> {
				judgement(server, sp, stack, aimPoint(server, sp, JUDGEMENT_REACH[stage]), stage);
				sp.getCooldowns().addCooldown(this, JUDGEMENT_COOLDOWN);
			}
			case SEA_NEEDLE -> {
				if (sp.isShiftKeyDown()) {
					mountain(server, sp, stack, aimPoint(server, sp, MOUNTAIN_REACH[stage]), stage);
					sp.getCooldowns().addCooldown(this, MOUNTAIN_COOLDOWN);
				} else {
					stretch(server, sp, stack, stage);
					sp.getCooldowns().addCooldown(this, STRETCH_COOLDOWN);
				}
			}
			case GANJIANG, MOYE -> {
				twinDragons(server, sp, stack, stage);
				sp.getCooldowns().addCooldown(ModItems.legend(Legend.GANJIANG), DRAGONS_COOLDOWN);
				sp.getCooldowns().addCooldown(ModItems.legend(Legend.MOYE), DRAGONS_COOLDOWN);
			}
			default -> {}
		}
		sp.swing(hand, true);
		CultivationManager.sync(sp);
		return InteractionResultHolder.consume(stack);
	}

	/** {@code share} of {@code player}'s pool spent on an art, or they are told they haven't it (false). */
	private static boolean spend(ServerPlayer player, double share) {
		PlayerCultivation c = CultivationManager.get(player);
		double cost = Math.max(1, c.maxQi() * share);
		if (c.getQi() < cost) {
			player.displayClientMessage(Component.translatable(ModLang.ART_NO_QI, Math.round(cost)), true);
			return false;
		}
		c.consumeQi(cost);
		return true;
	}

	// --- The Sun-Shooting Bow: drawn, and let go ---

	@Override
	public int getUseDuration(ItemStack stack) {
		return legend == Legend.SUN_BOW ? 72000 : super.getUseDuration(stack);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return legend == Legend.SUN_BOW ? UseAnim.BOW : super.getUseAnimation(stack);
	}

	/**
	 * The bow let go: an arrow of sun qi (no arrow needed), as hard as it was drawn; drawn fully and let go sneaking, its art, Falling Sun, for
	 * {@link #ART_COST} of the pool.
	 */
	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
		if (legend != Legend.SUN_BOW || !(user instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) return;
		int drawn = getUseDuration(stack) - timeLeft;
		float pull = BowItem.getPowerForTime(drawn);
		int stage = powers(stack, player);
		if (pull < 0.1f || stage <= 0) return;
		if (player.isShiftKeyDown() && drawn >= FULL_DRAW) {
			// Its own wait, apart from the bow's (an item's cooldown would hold its arrows up too): still gathering, an arrow flies instead.
			long now = server.getGameTime();
			Long ready = SUN_READY.get(player.getUUID());
			if (ready != null && now < ready) {
				player.displayClientMessage(Component.translatable(ModLang.LEGEND_SUN_GATHERING, (ready - now + 19) / 20), true);
			} else {
				if (!spend(player, ART_COST)) return;
				fallingSun(server, player, aimPoint(server, player, SUN_REACH[stage]), stage);
				SUN_READY.put(player.getUUID(), now + FALLING_SUN_COOLDOWN);
				CultivationManager.sync(player);
				return;
			}
		}
		SunArrow arrow = new SunArrow(server, player, stage);
		arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, pull * ARROW_SPEED, 0.5f);
		// An arrow's harm is its speed times its base: set so a full draw lands its share of its archer's strike.
		arrow.setBaseDamage(ElementalWeaponItem.attackOf(player) * ARROW_POWER[stage] / ARROW_SPEED);
		arrow.setCritArrow(pull >= 1.0f);
		arrow.setSecondsOnFire(100);
		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		server.addFreshEntity(arrow);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.3f);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.4f, 1.6f);
	}

	/**
	 * An arrow of sun qi (vanilla's spectral arrow, golden, burning): on what it strikes its sun's fire besides, a share of its archer's
	 * strike as fire qi (#SUNFIRE_SHARE) and burning. Its stage and strike are the moment's (not saved: an arrow's flight is short).
	 */
	static final class SunArrow extends SpectralArrow {
		private final int stage;
		private final double strike;

		SunArrow(Level level, LivingEntity archer, int stage) {
			super(level, archer);
			this.stage = stage;
			this.strike = ElementalWeaponItem.attackOf(archer);
		}

		/** Its sun's fire on what it strikes (not a spectral arrow's glow). */
		@Override
		protected void doPostHurtEffects(LivingEntity target) {
			if (level() instanceof ServerLevel level && getOwner() instanceof LivingEntity archer) {
				if (ElementalWeaponItem.qiHit(level, archer, target, QiElement.FIRE, strike * SUNFIRE_SHARE[stage])) target.setSecondsOnFire(5);
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, target.position().add(0, target.getBbHeight() * 0.6, 0), Vec3.ZERO, 0.7f, 0);
			}
		}
	}

	// --- The arts made vast (Colossus) ---

	/**
	 * Yellow Emperor's Judgement, the Xuanyuan Sword's art: the sword itself, made vast, falls point first out of the sky onto {@code point};
	 * where it lands, everyone within its breadth struck with its golden qi (twice over on the evil, #evil), thrown back, and the land shaken.
	 */
	static void judgement(ServerLevel level, LivingEntity caster, ItemStack sword, Vec3 point, int stage) {
		double strike = ElementalWeaponItem.attackOf(caster), radius = JUDGEMENT_RADIUS[stage];
		level.playSound(null, point.x, point.y, point.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.6f);
		Colossus.make(level, sword, Colossus.FALL, JUDGEMENT_SIZE[stage], point.add(0, 48, 0), point, () -> {
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius, radius + 2, radius),
					e -> Artifacts.mayStrike(caster, e) && e.position().distanceToSqr(point) <= radius * radius + 4)) {
				ElementalWeaponItem.qiHit(level, caster, e, QiElement.METAL, strike * JUDGEMENT_POWER[stage] * (evil(e) ? EVIL_TIMES : 1));
				throwFrom(e, point, 1.2);
			}
			SpellFx.send(level, SpellFx.Kind.FLASH, point.add(0, 1, 0), Vec3.ZERO, 1.6f, 0xFFD860);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, point.add(0, 0.2, 0), Vec3.ZERO, (float) radius, 0);
			SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, point.add(0, 0.5, 0), Vec3.ZERO, 2.5f, 0);
			level.playSound(null, point.x, point.y, point.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.0f, 0.5f);
			level.playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 0.8f);
		});
	}

	/**
	 * As You Will, reaching: the Sea-Fixing Needle stretches out from its wielder's hand along where they look ({@link #STRETCH_REACH}, to the
	 * first wall), striking everyone on its line with its weight's earth qi and driving them along it, then draws back.
	 */
	static void stretch(ServerLevel level, ServerPlayer caster, ItemStack needle, int stage) {
		double strike = ElementalWeaponItem.attackOf(caster);
		Vec3 look = caster.getLookAngle(), hand = caster.getEyePosition().add(look.scale(0.6)).add(0, -0.35, 0);
		Vec3 end = hand.add(look.scale(STRETCH_REACH[stage]));
		BlockHitResult wall = level.clip(new ClipContext(hand, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		Vec3 tip = wall.getType() == HitResult.Type.MISS ? end : wall.getLocation();
		level.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0f, 0.7f);
		Colossus.make(level, needle, Colossus.STRETCH, 1.0f, hand, tip, () -> {
			Vec3 along = tip.subtract(hand);
			double length = along.length();
			Vec3 way = length < 1.0e-4 ? look : along.scale(1 / length);
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(hand, tip).inflate(STRETCH_BREADTH),
					e -> Artifacts.mayStrike(caster, e) && nearLine(e, hand, way, length, STRETCH_BREADTH))) {
				ElementalWeaponItem.qiHit(level, caster, e, QiElement.EARTH, strike * STRETCH_POWER[stage]);
				e.push(way.x * 1.2, 0.3, way.z * 1.2);
				e.hurtMarked = true;
			}
			SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, tip, Vec3.ZERO, 1.2f, 0);
			level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 0.8f);
		});
	}

	/**
	 * As You Will, as a mountain: the Sea-Fixing Needle grows to a mountain's size and comes down upright on {@code point}; everyone within its
	 * breadth struck with its earth qi and thrown out and up, the land about it shaken.
	 */
	static void mountain(ServerLevel level, ServerPlayer caster, ItemStack needle, Vec3 point, int stage) {
		double strike = ElementalWeaponItem.attackOf(caster), radius = MOUNTAIN_RADIUS[stage];
		level.playSound(null, point.x, point.y, point.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.5f, 0.5f);
		Colossus.make(level, needle, Colossus.SLAM, MOUNTAIN_SIZE[stage], point.add(0, 70, 0), point, () -> {
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius, radius + 3, radius),
					e -> Artifacts.mayStrike(caster, e) && e.position().distanceToSqr(point) <= radius * radius + 9)) {
				ElementalWeaponItem.qiHit(level, caster, e, QiElement.EARTH, strike * MOUNTAIN_POWER[stage]);
				throwFrom(e, point, 1.6);
			}
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, point.add(0, 0.2, 0), Vec3.ZERO, (float) radius * 1.4f, 0);
			SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, point.add(0, 0.5, 0), Vec3.ZERO, 3.0f, 0);
			SpellFx.send(level, SpellFx.Kind.DUST, point.add(0, 0.5, 0), Vec3.ZERO, (float) radius, 0);
			level.playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5f, 0.5f);
			level.playSound(null, point.x, point.y, point.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.0f, 0.4f);
		});
	}

	/**
	 * Falling Sun, the Sun-Shooting Bow's art: a sun falls on {@code point} out of the sky and bursts into sunfire, everyone within its
	 * breadth struck with its fire qi and set burning.
	 */
	static void fallingSun(ServerLevel level, LivingEntity caster, Vec3 point, int stage) {
		double strike = ElementalWeaponItem.attackOf(caster), radius = SUN_RADIUS[stage];
		SpellFx.send(level, SpellFx.Kind.STREAK, point.add(0, 40, 0), point, 0, 0xFFB030);
		SpellFx.send(level, SpellFx.Kind.FLASH, point.add(0, 1, 0), Vec3.ZERO, 1.8f, 0xFFC040);
		SpellFx.send(level, SpellFx.Kind.FLAME_BURST, point.add(0, 1, 0), Vec3.ZERO, (float) radius * 0.6f, 0);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, point.add(0, 0.2, 0), Vec3.ZERO, (float) radius, 0);
		level.sendParticles(ParticleTypes.FLAME, point.x, point.y + 1, point.z, 80, radius * 0.5, 1.0, radius * 0.5, 0.08);
		level.sendParticles(ParticleTypes.LAVA, point.x, point.y + 0.5, point.z, 20, radius * 0.4, 0.3, radius * 0.4, 0.1);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius, radius + 2, radius),
				e -> Artifacts.mayStrike(caster, e) && e.position().distanceToSqr(point) <= radius * radius + 4)) {
			if (ElementalWeaponItem.qiHit(level, caster, e, QiElement.FIRE, strike * SUN_POWER[stage])) e.setSecondsOnFire(6);
		}
		level.playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.7f);
		level.playSound(null, point.x, point.y, point.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f);
	}

	/**
	 * Split Heaven and Earth, Pangu's Axe's art: a cleave down into the land the way its wielder looks (level), {@link #SPLIT_REACH} along it.
	 * A tear in space runs the length of it; everyone within {@link #SPLIT_BREADTH} of the line is struck with the axe's edge (past every
	 * ward: #cutsThroughWards) and thrown aside; every barrier it crosses shatters, every Law domain on it but its wielder's own folds; and a
	 * player's (HeavenSpells#carvesLand) splits the land along it.
	 */
	static void splitHeavenAndEarth(ServerLevel level, ServerPlayer caster, int stage) {
		double strike = ElementalWeaponItem.attackOf(caster), reach = SPLIT_REACH[stage];
		Vec3 flat = caster.getLookAngle().multiply(1, 0, 1);
		Vec3 way = flat.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, caster.getYRot()) : flat.normalize();
		Vec3 from = caster.position().add(way.scale(1.5)), to = from.add(way.scale(reach));
		// The tear along the land, its whole length.
		for (double d = 0; d <= reach; d += 3) {
			Vec3 point = from.add(way.scale(d));
			int floor = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(point.x), (int) Math.floor(point.z));
			double y = floor > level.getMinBuildHeight() + 1 ? floor : caster.getY();
			SpellFx.send(level, SpellFx.Kind.RIFT_TEAR, new Vec3(point.x, y + 1.5, point.z), way, 2.2f, 0);
			if (d % 9 == 0) SpellFx.send(level, SpellFx.Kind.DUST, new Vec3(point.x, y + 0.2, point.z), Vec3.ZERO, 2.0f, 0);
		}
		// Everyone near the line: struck, and thrown off it to its side.
		Vec3 across = new Vec3(-way.z, 0, way.x);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(SPLIT_BREADTH, 8, SPLIT_BREADTH),
				e -> Artifacts.mayStrike(caster, e))) {
			Vec3 to2 = e.position().subtract(from).multiply(1, 0, 1);
			double t = to2.dot(way), side = to2.dot(across);
			if (t < -1 || t > reach + 1 || Math.abs(side) > SPLIT_BREADTH) continue;
			ElementalWeaponItem.qiHit(level, caster, e, QiElement.METAL, strike * SPLIT_POWER[stage]);
			double push = side >= 0 ? 1.4 : -1.4;
			e.push(across.x * push, 0.5, across.z * push);
			e.hurtMarked = true;
		}
		// The barriers it crosses shatter (Formations#batter: no barrier holds against Pangu's Axe).
		for (Formation formation : List.copyOf(FormationData.get(level).all())) {
			if (!formation.isRaised()) continue;
			Vec3 heart = Vec3.atCenterOf(formation.center), off = heart.subtract(from).multiply(1, 0, 1);
			double t = Math.max(0, Math.min(reach, off.dot(way)));
			Vec3 nearest = from.add(way.scale(t));
			if (nearest.multiply(1, 0, 1).distanceTo(heart.multiply(1, 0, 1)) > formation.getRadius()) continue;
			Formations.batter(level, formation, caster, 2, BlockPos.containing(nearest.x, heart.y, nearest.z));
		}
		// The Law domains on it fold, but its wielder's own.
		for (LawDomains.Domain domain : List.copyOf(LawDomains.lit())) {
			if (domain.owner() == caster || domain.owner().level() != level) continue;
			Vec3 off = domain.centre().subtract(from).multiply(1, 0, 1);
			double t = Math.max(0, Math.min(reach, off.dot(way)));
			if (from.add(way.scale(t)).multiply(1, 0, 1).distanceTo(domain.centre().multiply(1, 0, 1)) <= domain.radius()) {
				LawDomains.unmake(level, domain.centre(), 0.5);
			}
		}
		// The land split along it (a player's alone: HeavenSpells#carvesLand).
		Devastation.trench(caster, level, from, to, SPLIT_WIDTH, SPLIT_DEPTH);
		level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 0.5f);
		level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.4f);
		level.playSound(null, to.x, to.y, to.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2f, 0.6f);
	}

	/** {@code e} thrown out from {@code from} and up, {@code strength} by its weight. */
	private static void throwFrom(LivingEntity e, Vec3 from, double strength) {
		Vec3 away = e.position().subtract(from).multiply(1, 0, 1);
		away = away.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 0) : away.normalize();
		e.push(away.x * strength, 0.4 * strength, away.z * strength);
		e.hurtMarked = true;
	}

	/** Whether {@code e}'s middle lies within {@code breadth} of the line from {@code from} along {@code way}, {@code length} long. */
	private static boolean nearLine(LivingEntity e, Vec3 from, Vec3 way, double length, double breadth) {
		Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
		double t = to.dot(way);
		return t >= 0 && t <= length && to.subtract(way.scale(t)).lengthSqr() <= breadth * breadth;
	}

	/** Where {@code player} looks, up to {@code reach}: the first foe on the line, else the first ground or wall, else the land under its end. */
	static Vec3 aimPoint(ServerLevel level, ServerPlayer player, double reach) {
		LivingEntity foe = aimed(level, player, reach);
		if (foe != null) return foe.position();
		Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(reach));
		BlockHitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (wall.getType() != HitResult.Type.MISS) return wall.getLocation();
		BlockHitResult ground = level.clip(new ClipContext(end, end.add(0, -64, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
		return ground.getType() != HitResult.Type.MISS ? ground.getLocation() : end;
	}

	/** Twin Dragons: a spirit of the sword in hand flies out and fights for its owner a while, and one of its other half if they hold it too. */
	static void twinDragons(ServerLevel level, ServerPlayer player, ItemStack stack, int stage) {
		double strike = ElementalWeaponItem.attackOf(player);
		LegendaryWeaponItem weapon = (LegendaryWeaponItem) stack.getItem();
		SwordSpirit.called(level, player, weapon.legend, strike * DRAGON_SHARE[stage], DRAGON_LIFE[stage]);
		ItemStack other = player.getOffhandItem();
		if (other.getItem() instanceof LegendaryWeaponItem pair && pair.legend == weapon.legend.pair() && powers(other, player) > 0) {
			SwordSpirit.called(level, player, pair.legend, strike * DRAGON_SHARE[Math.max(stage, powers(other, player))], DRAGON_LIFE[stage]);
		}
		level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 1.6f);
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.4f);
	}

	/** The first foe on the line where {@code player} looks, up to {@code reach} and not through walls, or null. */
	static @Nullable LivingEntity aimed(ServerLevel level, ServerPlayer player, double reach) {
		Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(reach));
		BlockHitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, new AABB(eye, end).inflate(1.0),
				e -> e instanceof LivingEntity living && Artifacts.mayStrike(player, living));
		return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
	}

	/**
	 * Heaven's Brand, the Tribulation Spear's art (the user's word, 2026-10-28: "it permanently adds bolts to someone's heavenly tribulation
	 * when used on them"): its tribulation bolt onto {@code target} ({@link #heavensBolt}), and a player struck branded: every heavenly
	 * tribulation they face from now on brings {@link #BRAND} percent more bolts (TribulationManager), for good, up to {@link #BRAND_MOST}
	 * (PlayerCultivation#getTribulationBrand). None but a player has a tribulation: anyone else takes the bolt alone.
	 */
	static void heavensBrand(ServerLevel level, ServerPlayer caster, LivingEntity target, int stage) {
		heavensBolt(level, caster, target.position(), stage);
		if (!(target instanceof ServerPlayer branded) || branded.isCreative() || branded.isSpectator()) return;
		PlayerCultivation c = CultivationManager.get(branded);
		c.setTribulationBrand(c.getTribulationBrand() + BRAND[stage]);
		CultivationManager.markDirty(level.getServer());
		int brand = c.getTribulationBrand();
		level.sendParticles(ParticleTypes.WITCH, branded.getX(), branded.getY() + 1.2, branded.getZ(), 40, 0.4, 0.7, 0.4, 0.1);
		branded.playNotifySound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.6f, 1.4f);
		branded.sendSystemMessage(Component.translatable(ModLang.LEGEND_BRANDED_YOU, caster.getDisplayName(), brand).withStyle(ChatFormatting.LIGHT_PURPLE));
		caster.displayClientMessage(Component.translatable(ModLang.LEGEND_BRANDED, branded.getDisplayName(), brand), true);
	}

	/**
	 * A tribulation bolt from the sky onto {@code point}: everyone within its stage's breadth struck with heaven's lightning (a share of
	 * {@code caster}'s strike) and jolted; {@code caster} never, nor those it may not strike. Heaven's Brand's, and Lei Zhen's at whoever he
	 * fights (the bolt alone: LeiZhen).
	 */
	public static void heavensBolt(ServerLevel level, LivingEntity caster, Vec3 point, int stage) {
		double strike = ElementalWeaponItem.attackOf(caster), radius = BOLT_RADIUS[stage];
		bolt(level, point, point.y + 30, 2.0f + 0.4f * stage, false);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius, radius + 1, radius),
				e -> Artifacts.mayStrike(caster, e) && e.position().distanceToSqr(point) <= (radius + 0.5) * (radius + 0.5))) {
			lightning(level, caster, e, strike * BOLT_POWER[stage]);
			e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3), caster);
		}
		SpellFx.send(level, SpellFx.Kind.FLASH, point.add(0, 1, 0), Vec3.ZERO, 1.2f, 0xCFE4FF);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y + 0.5, point.z, 60, radius * 0.5, 0.5, radius * 0.5, 0.3);
	}

	/** A tribulation bolt drawn onto {@code at} from a cloud at {@code cloudY} (visual: its harm is whoever sends it's own). */
	static void bolt(ServerLevel level, Vec3 at, double cloudY, float scale, boolean dim) {
		TribulationLightning bolt = ModEntities.TRIBULATION_LIGHTNING.create(level);
		if (bolt == null) return;
		bolt.moveTo(at.x, at.y, at.z);
		bolt.setCloudBaseY(cloudY);
		bolt.setScale(scale);
		bolt.setVisualOnly(true);
		bolt.setDim(dim);
		level.addFreshEntity(bolt);
	}

	/**
	 * Whether {@code player} holds out the Tribulation Spear they are bonded to, in either hand: in their tribulations it drinks every third
	 * of the heavens' bolts (TribulationManager#strike; the user's ask, 2026-10-28: "if the owner holds it out, it should absorb 1/3 of the
	 * bolts that come").
	 */
	public static boolean drinks(ServerPlayer player) {
		return ownSpear(player, player.getMainHandItem()) || ownSpear(player, player.getOffhandItem());
	}

	private static boolean ownSpear(ServerPlayer player, ItemStack stack) {
		return stack.getItem() instanceof LegendaryWeaponItem weapon && weapon.legend == Legend.TRIBULATION_SPEAR && bondedTo(stack, player)
				&& Legends.isTrue(player.server, stack);
	}

	/**
	 * A bolt of {@code player}'s tribulation drunk by the spear they hold out: none of it reaches them, and the spear is the stronger for it,
	 * for good ({@link #GROWTH}; counted in the record, Legends#drank, so a reformed spear keeps it). {@code drawn}: the bolt drawn of those
	 * landing together, which it is seen drinking.
	 */
	public static void drink(ServerPlayer player, boolean drawn) {
		int drunk = Legends.drank(player.server);
		if (!drawn) return;
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.6, player.getZ(), 40, 0.3, 0.6, 0.3, 0.25);
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.6, player.getZ(), 8, 0.2, 0.4, 0.2, 0.02);
		level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.8f, 1.5f);
		player.displayClientMessage(Component.translatable(ModLang.LEGEND_DRINKS_BOLT, Legend.TRIBULATION_SPEAR.getDisplayName(), drunk), true);
	}

	/**
	 * Whether a blow of {@code source} is Moye's (the attacker's own hand, the true sword in it, answering them): on the dead it lands, and in
	 * full (GhostSystem#reaches, #scaled).
	 */
	public static boolean reachesTheDead(DamageSource source) {
		Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity living) || source.getDirectEntity() != attacker) return false;
		ItemStack held = living.getMainHandItem();
		return held.getItem() instanceof LegendaryWeaponItem weapon && (weapon.legend == Legend.MOYE || weapon.legend == Legend.XUANYUAN)
				&& powers(held, living) > 0;
	}

	// --- Its tooltip ---

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.legendLore(legend.getId())).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		if (serial(stack) <= 0) {
			Tooltips.add(lines, Component.translatable(ModLang.LEGEND_HOLLOW_LINE).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		Tooltips.add(lines, Component.translatable(ModLang.LEGEND_WIELDED_FROM, legend.lowest().getDisplayName()).withStyle(ChatFormatting.GRAY));
		String owner = stack.hasTag() ? stack.getTag().getString(BOND_NAME) : "";
		Tooltips.add(lines, (owner.isEmpty() ? Component.translatable(ModLang.LEGEND_UNBONDED) : Component.translatable(ModLang.LEGEND_BONDED_TO, owner))
				.withStyle(ChatFormatting.GRAY));
		int stage = stage(stack);
		if (stage > 0) Tooltips.add(lines, Component.translatable(ModLang.LEGEND_STAGE, stage).withStyle(ChatFormatting.GOLD));
		int shown = Math.max(1, stage);
		switch (legend) {
			case GANJIANG -> {
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_GANJIANG_POWER, Artifacts.percent(SHARE[shown]), BURN[shown]).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_PAIR_POWER, Legend.MOYE.getDisplayName()).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_DRAGONS, DRAGON_LIFE[shown] / 20).withStyle(ChatFormatting.GOLD));
			}
			case MOYE -> {
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_MOYE_POWER, Artifacts.percent(SHARE[shown])).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_PAIR_POWER, Legend.GANJIANG.getDisplayName()).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_DRAGONS, DRAGON_LIFE[shown] / 20).withStyle(ChatFormatting.GOLD));
			}
			case TRIBULATION_SPEAR -> {
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_SPEAR_POWER, Artifacts.percent(SHARE[shown]), CHAIN[shown]).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_SPEAR_DRINKS).withStyle(ChatFormatting.BLUE));
				int drunk = drunk(stack);
				if (drunk > 0) {
					Tooltips.add(lines, Component.translatable(ModLang.LEGEND_SPEAR_DRUNK, String.format(Locale.ROOT, "%,d", drunk),
							String.format(Locale.ROOT, "%.1f", drunk * GROWTH * 100)).withStyle(ChatFormatting.AQUA));
				}
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_BRAND, Math.round(BRAND_REACH[shown]), BRAND[shown], BRAND_MOST).withStyle(ChatFormatting.GOLD));
			}
			case XUANYUAN -> {
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_XUANYUAN_POWER, Artifacts.percent(SHARE[shown])).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_XUANYUAN_RIGHTEOUS).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_JUDGEMENT, Math.round(JUDGEMENT_REACH[shown])).withStyle(ChatFormatting.GOLD));
			}
			case SUN_BOW -> {
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_BOW_POWER, Artifacts.percent(SUNFIRE_SHARE[shown])).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_FALLING_SUN, Math.round(SUN_REACH[shown])).withStyle(ChatFormatting.GOLD));
			}
			case SEA_NEEDLE -> {
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_NEEDLE_POWER, Artifacts.percent(SHARE[shown])).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_NEEDLE_KEPT).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_AS_YOU_WILL, Math.round(STRETCH_REACH[shown])).withStyle(ChatFormatting.GOLD));
			}
			case PANGU_AXE -> {
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_AXE_POWER, Artifacts.percent(SHARE[shown])).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_AXE_CLEAVES).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_SPLIT, Math.round(SPLIT_REACH[shown])).withStyle(ChatFormatting.GOLD));
			}
			case AZURE_GLAIVE, WHITE_TIGER_SABER, VERMILION_FAN, TORTOISE_MACE, YELLOW_DRAGON_SEAL -> {
				double share = SHARE[shown] * (legend.venerable() == QiElement.METAL ? METAL_EDGE : 1);
				Tooltips.add(lines, Component.translatable(ModLang.legendSymbolPower(legend.getId()), Artifacts.percent(share)).withStyle(ChatFormatting.BLUE));
				Tooltips.add(lines, Component.translatable(ModLang.LEGEND_SYMBOL_ART, legend.symbolSpell().getDisplayName()).withStyle(ChatFormatting.GOLD));
			}
			default -> {}
		}
		if (owner.isEmpty()) {
			Tooltips.add(lines, Component.translatable(legend == Legend.TRIBULATION_SPEAR ? ModLang.LEGEND_HOW_TO_TAME : ModLang.LEGEND_HOW_TO_BOND)
					.withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	/** A legend's tier: it never wears out, takes enchantments well, and is mended by nothing (it needs none). */
	private enum Unbreakable implements Tier {
		TIER;

		@Override public int getUses() { return 0; }
		@Override public float getSpeed() { return 12.0f; }
		@Override public float getAttackDamageBonus() { return 0; }
		@Override public int getLevel() { return 4; }
		@Override public int getEnchantmentValue() { return 22; }
		@Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
	}
}
