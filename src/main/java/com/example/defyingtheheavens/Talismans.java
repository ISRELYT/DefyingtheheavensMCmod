package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Talismans, the fourth craft (the user's "Build as designed", 2026-11-10). The Inscription Brush in one hand and Talisman Paper in the other:
 * used, a pattern is picked (the inscription screen, kind 2: {@link #offer}) and kept on the brush; used again and held for
 * {@link #DRAW_TICKS}, it draws it ({@link #draw}): one paper, one redstone (the cinnabar), its share of one's qi pool (TalismanPattern#qiShare),
 * the pattern's own extras, a point of the brush's wear. CraftMastery's Talismans: a pattern harder than one's rank fails 12% a rank short
 * (the paper and cinnabar spoiled, half the practice learned); from Adept a talisman can come out Fine, from Master Perfect (the forge's
 * odds and worth: Artifacts.Quality). A talisman of power is as strong as its drawer's realm, written on it ({@link #RANK}); burnt or thrown by
 * one weaker, it works at most {@link #ABOVE_BURNER} stages (a realm) above them ({@link #effectiveRank}). One found with no hand on it
 * (loot, a merchant's) is as strong as whoever uses it. Each pattern has its own short wait (TalismanItem: the hotbar's).
 */
public final class Talismans {
	/** On a drawn talisman: its drawer's rank (PlayerCultivation#rank) and name, and a Thousand-Li Escape's reach. Its work's worth is the artifacts' (Artifacts#quality). */
	public static final String RANK = "TalismanRank", DRAWER = "TalismanDrawer", REACH = "TalismanReach";
	/** On the brush: the pattern it is set to draw. */
	public static final String CHOSEN = "TalismanPattern";
	/** A drawing takes this long, the use held. */
	public static final int DRAW_TICKS = 40;
	/** Burnt or thrown by one weaker than its drawer, a talisman works at most this many stages (a realm) above them. */
	public static final int ABOVE_BURNER = 4;
	/** How hard one is thrown (a snowball's 1.5). */
	public static final float THROW_SPEED = 1.5f;
	/** A Thousand-Li Escape's reach, by its drawer's Talismans rank (Apprentice to Saint); it lands no nearer than a quarter of it. */
	public static final int[] ESCAPE_REACH = {100, 150, 200, 300, 500};
	/** Fire: its burst's reach, its harm (times its realm's blow: CultivationStats#attackDamage), and how long it sets one alight. */
	public static final double FIRE_RADIUS = 2.5, FIRE_HARM = 1.5;
	public static final int FIRE_SECONDS = 5;
	/** Five Thunder: its bolt's harm, and how near where it lands it strikes. */
	public static final double THUNDER_HARM = 2.5, THUNDER_REACH = 1.5;
	/** Frost: its burst's reach and harm, and how long it slows (Slowness II). */
	public static final double FROST_RADIUS = 3.0, FROST_HARM = 1.0;
	public static final int FROST_TICKS = 100;
	/** Binding: seconds held, and one more for each realm the talisman stands above the bound (at most {@link #BIND_MORE} more). */
	public static final int BIND_SECONDS = 3, BIND_MORE = 3;
	/** Golden Light: its shell, a share of its realm's whole health (CultivationStats#maxHealth); how long it lasts. */
	public static final double GOLDEN_SHARE = 0.3;
	public static final int GOLDEN_TICKS = 600;
	/** Divine Speed and Concealment: how long each lasts. */
	public static final int SPEED_TICKS = 1200, CONCEAL_TICKS = 1200;
	/** A concealed one is noticed anyway by a cultivator or beast this many stages (two realms) or more above the talisman. */
	public static final int SEES_THROUGH = 8;

	// --- Drawing ---

	/** The pattern {@code brush} is set to draw, or null. */
	public static TalismanPattern chosen(ItemStack brush) {
		CompoundTag tag = brush.getTag();
		return tag == null || !tag.contains(CHOSEN) ? null : TalismanPattern.byIndex(tag.getInt(CHOSEN));
	}

	/** Talisman Paper in the hand other than {@code hand}. */
	public static boolean paperIn(Player player, InteractionHand hand) {
		return player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND).is(ModItems.TALISMAN_PAPER);
	}

	/** Every pattern, as the inscription screen lists its options (kind 2: TechniqueManualItem's manuals are 0 and 1). */
	public static List<int[]> patterns() {
		List<int[]> options = new ArrayList<>();
		for (TalismanPattern pattern : TalismanPattern.values()) options.add(new int[] {2, pattern.ordinal()});
		return options;
	}

	/** The brush used with paper in the other hand and no pattern set (or sneaking): the patterns shown to pick from. */
	public static void offer(ServerPlayer p) {
		ModPackets.sendInscribeOptions(p, patterns());
	}

	/** C2S inscribe, kind 2: the brush in a hand with paper in the other is set to draw pattern {@code index}. */
	public static void choose(ServerPlayer p, int index) {
		TalismanPattern pattern = TalismanPattern.byIndex(index);
		InteractionHand hand = p.getMainHandItem().is(ModItems.INSCRIPTION_BRUSH) ? InteractionHand.MAIN_HAND
				: p.getOffhandItem().is(ModItems.INSCRIPTION_BRUSH) ? InteractionHand.OFF_HAND : null;
		if (pattern == null || hand == null || !paperIn(p, hand)) return;
		p.getItemInHand(hand).getOrCreateTag().putInt(CHOSEN, pattern.ordinal());
		p.displayClientMessage(Component.translatable(ModLang.MSG_TALISMAN_CHOSEN, pattern.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
	}

	/** What a drawing of {@code pattern} costs {@code c} in qi. */
	public static double qiCost(PlayerCultivation c, TalismanPattern pattern) {
		return c.maxQi() * pattern.qiShare;
	}

	/** Why {@code p} can't draw {@code pattern} now (paper, qi, cinnabar, extras), or null. */
	public static Component refusal(ServerPlayer p, TalismanPattern pattern) {
		PlayerCultivation c = CultivationManager.get(p);
		if (c.maxQi() <= 0) return Component.translatable(ModLang.MSG_TALISMAN_NO_QI_POOL);
		double cost = qiCost(c, pattern);
		if (c.getQi() < cost) return Component.translatable(ModLang.MSG_TALISMAN_LOW_QI, pattern.getDisplayName(), (int) Math.ceil(cost), (int) c.getQi());
		if (p.getAbilities().instabuild) return null;
		if (!has(p, Items.REDSTONE)) return Component.translatable(ModLang.MSG_TALISMAN_NEEDS, pattern.getDisplayName(), Items.REDSTONE.getDescription());
		for (Item extra : pattern.extras()) {
			if (!has(p, extra)) return Component.translatable(ModLang.MSG_TALISMAN_NEEDS, pattern.getDisplayName(), extra.getDescription());
		}
		return null;
	}

	private static boolean has(ServerPlayer p, Item item) {
		return p.getInventory().contains(new ItemStack(item));
	}

	private static void take(ServerPlayer p, Item item) {
		int slot = p.getInventory().findSlotMatchingItem(new ItemStack(item));
		if (slot >= 0) p.getInventory().removeItem(slot, 1);
		else if (p.getOffhandItem().is(item)) p.getOffhandItem().shrink(1);
	}

	/**
	 * The drawing held to its end (InscriptionBrushItem#finishUsingItem): the pattern set on the brush in {@code brushHand} drawn on the paper in
	 * the other, if {@code p} can ({@link #refusal}). Fails at the mastery's odds; else a talisman (or two: TalismanPattern#count) of the drawer's
	 * realm and the work's worth.
	 */
	public static void draw(ServerPlayer p, InteractionHand brushHand) {
		ItemStack brush = p.getItemInHand(brushHand);
		TalismanPattern pattern = chosen(brush);
		if (pattern == null || !brush.is(ModItems.INSCRIPTION_BRUSH)) return;
		InteractionHand paperHand = brushHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		ItemStack paper = p.getItemInHand(paperHand);
		if (!paper.is(ModItems.TALISMAN_PAPER)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_TALISMAN_NO_PAPER).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		Component why = refusal(p, pattern);
		if (why != null) {
			p.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
			return;
		}
		PlayerCultivation c = CultivationManager.get(p);
		c.setQi(c.getQi() - qiCost(c, pattern));
		CultivationManager.sync(p);
		if (!p.getAbilities().instabuild) {
			paper.shrink(1);
			take(p, Items.REDSTONE);
			for (Item extra : pattern.extras()) take(p, extra);
		}
		brush.hurtAndBreak(1, p, who -> who.broadcastBreakEvent(brushHand));
		ServerLevel level = p.serverLevel();
		CraftMastery.Rank rank = CraftMastery.rank(CultivationManager.own(p), CraftMastery.Craft.TALISMANS);
		if (p.getRandom().nextDouble() < CraftMastery.failChance(rank, pattern.difficulty)) {
			CraftMastery.practise(p, CraftMastery.Craft.TALISMANS, (pattern.practice() + 1) / 2);
			level.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.4f, 1.6f);
			aroundThem(level, ParticleTypes.SMOKE, p, 8, 0.9, 0.01);
			p.displayClientMessage(Component.translatable(ModLang.MSG_TALISMAN_SPOILED, pattern.getDisplayName()).withStyle(ChatFormatting.RED), true);
			return;
		}
		ItemStack made = new ItemStack(pattern.item(), pattern.count);
		Artifacts.Quality quality = Artifacts.Quality.NORMAL;
		if (pattern.hasPower()) {
			CompoundTag tag = made.getOrCreateTag();
			tag.putInt(RANK, Math.max(0, TwoPaths.higherRank(c, false)));
			tag.putString(DRAWER, p.getGameProfile().getName());
			if (pattern == TalismanPattern.THOUSAND_LI_ESCAPE) tag.putInt(REACH, ESCAPE_REACH[rank.ordinal()]);
			quality = ArtifactForgeBlockEntity.rollQuality(rank, p.getRandom().nextDouble());
			Artifacts.withQuality(made, quality);
		}
		CraftMastery.practise(p, CraftMastery.Craft.TALISMANS, pattern.practice());
		if (!p.getInventory().add(made)) p.drop(made, false);
		level.playSound(null, p.blockPosition(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.8f, 1.0f);
		level.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
		aroundThem(level, ParticleTypes.ENCHANT, p, 16, 0.9, 0.2);
		p.displayClientMessage((quality == Artifacts.Quality.NORMAL ? Component.translatable(ModLang.MSG_TALISMAN_DRAWN, pattern.getDisplayName())
				: Component.translatable(ModLang.MSG_TALISMAN_DRAWN_FINE, pattern.getDisplayName(), quality.getDisplayName())).withStyle(ChatFormatting.GOLD), true);
		Milestones.talismanDrawn(p);
	}

	/** Talismans found (a secret realm's vault, a merchant's wares): {@code count} of the patterns of power at random, of no known hand. */
	public static List<ItemStack> found(net.minecraft.util.RandomSource random, int count) {
		List<TalismanPattern> powered = new ArrayList<>();
		for (TalismanPattern pattern : TalismanPattern.values()) if (pattern.hasPower()) powered.add(pattern);
		List<ItemStack> out = new ArrayList<>();
		for (int i = 0; i < count; i++) out.add(new ItemStack(powered.get(random.nextInt(powered.size())).item()));
		return out;
	}

	/** {@code count} talismans of {@code pattern} drawn by {@code drawer} at {@code rank} (a cultivator's own, for its wares: CultivatorTrades). */
	public static ItemStack drawnBy(TalismanPattern pattern, int rank, String drawer, int count) {
		ItemStack stack = new ItemStack(pattern.item(), count);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putInt(RANK, Math.max(0, rank));
		tag.putString(DRAWER, drawer);
		if (pattern == TalismanPattern.THOUSAND_LI_ESCAPE) tag.putInt(REACH, ESCAPE_REACH[0]);
		return stack;
	}

	// --- Its strength ---

	/** {@code e}'s rank (PlayerCultivation#rank): a player's higher path's, a cultivator's or beast's own; -1 for anything else. */
	public static int rankOf(LivingEntity e) {
		if (e instanceof ServerPlayer p) return TwoPaths.higherRank(CultivationManager.get(p), false);
		if (e instanceof CultivatorEntity cultivator) return PlayerCultivation.rank(cultivator.getCultivationRealm(), cultivator.getCultivationStage());
		return -1;
	}

	/** Its drawer's rank as written on it, or -1: none (one found with no hand on it). */
	public static int drawnAt(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(RANK) ? tag.getInt(RANK) : -1;
	}

	/** How strong {@code stack} is in {@code user}'s hands: its drawer's realm, at most a realm above theirs (theirs, if none is written on it). */
	public static int effectiveRank(ItemStack stack, LivingEntity user) {
		int own = Math.max(0, rankOf(user));
		int drawn = drawnAt(stack);
		return drawn < 0 ? own : Math.max(0, Math.min(drawn, own + ABOVE_BURNER));
	}

	/** The blow of a cultivator of {@code rank} (CultivationStats#attackDamage), the measure of a talisman's harm. */
	public static double blow(int rank) {
		return CultivationStats.attackDamage(CultivatorNpc.realmOf(Math.max(0, rank)), CultivatorNpc.stageOf(Math.max(0, rank)));
	}

	// --- Thrown ---

	/** {@code p} throws {@code stack} (one of it), as strong as it is in their hands. */
	static void toss(ServerPlayer p, ItemStack stack) {
		ServerLevel level = p.serverLevel();
		TalismanEntity thrown = new TalismanEntity(level, p);
		thrown.setItem(stack.copyWithCount(1));
		thrown.setStrength(effectiveRank(stack, p), (float) Artifacts.power(stack));
		thrown.shootFromRotation(p, p.getXRot(), p.getYRot(), 0.0f, THROW_SPEED, 1.0f);
		level.addFreshEntity(thrown);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f, 0.6f + level.getRandom().nextFloat() * 0.2f);
	}

	/** A thrown talisman lands ({@code hit}): its pattern's work where it struck. */
	static void land(TalismanEntity talisman, HitResult hit) {
		if (!(talisman.level() instanceof ServerLevel level)) return;
		TalismanPattern pattern = TalismanPattern.of(talisman.getItem().getItem());
		if (pattern == null) return;
		LivingEntity thrower = talisman.getOwner() instanceof LivingEntity owner ? owner : null;
		LivingEntity struck = hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
		if (struck != null && thrower != null && HeavenSpells.spared(thrower, struck)) struck = null;
		Vec3 at = hit.getLocation();
		int rank = talisman.rank();
		Realm realm = CultivatorNpc.realmOf(rank);
		float power = talisman.power();
		switch (pattern) {
			case FIRE -> {
				float harm = (float) (blow(rank) * FIRE_HARM * power);
				for (LivingEntity e : caught(level, thrower, at, FIRE_RADIUS, struck)) {
					e.hurt(ModDamageTypes.qi(level, QiElement.FIRE, realm, talisman, thrower), harm);
					e.setSecondsOnFire(FIRE_SECONDS);
				}
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.3, at.z, 40, FIRE_RADIUS * 0.4, 0.4, FIRE_RADIUS * 0.4, 0.05);
				level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 6, 0.6, 0.2, 0.6, 0.0);
				level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
			}
			case FIVE_THUNDER -> {
				Vec3 strike = struck != null ? struck.position() : at;
				LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
				if (bolt != null) {
					bolt.moveTo(strike.x, strike.y, strike.z);
					bolt.setVisualOnly(true);
					level.addFreshEntity(bolt);
				}
				float harm = (float) (blow(rank) * THUNDER_HARM * power);
				for (LivingEntity e : caught(level, thrower, strike, THUNDER_REACH, struck)) {
					e.hurt(ModDamageTypes.qi(level, QiElement.METAL, realm, talisman, thrower), harm);
				}
			}
			case FROST -> {
				float harm = (float) (blow(rank) * FROST_HARM * power);
				for (LivingEntity e : caught(level, thrower, at, FROST_RADIUS, struck)) {
					e.hurt(ModDamageTypes.qi(level, QiElement.WATER, realm, talisman, thrower), harm);
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (FROST_TICKS * power), 1), thrower);
					e.setTicksFrozen(Math.max(e.getTicksFrozen(), 160));
					e.clearFire();
				}
				level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.4, at.z, 50, FROST_RADIUS * 0.4, 0.5, FROST_RADIUS * 0.4, 0.05);
				level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.6f);
				level.playSound(null, at.x, at.y, at.z, SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
			}
			case BINDING -> {
				if (struck != null) bind(struck, rank, power, thrower);
			}
			default -> {
			}
		}
	}

	/** Who a burst at {@code at} of {@code radius} catches: the one struck (unless spared) and every being its thrower's spells would mark (HeavenSpells#mark). */
	private static List<LivingEntity> caught(ServerLevel level, LivingEntity thrower, Vec3 at, double radius, LivingEntity struck) {
		List<LivingEntity> caught = new ArrayList<>();
		if (struck != null && struck.isAlive()) caught.add(struck);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius), e -> e.isAlive() && e.distanceToSqr(at) <= radius * radius)) {
			if (e == struck || e == thrower) continue;
			if (thrower != null ? HeavenSpells.mark(thrower, e) : !(e instanceof Player)) caught.add(e);
		}
		return caught;
	}

	/**
	 * A Binding Talisman strikes {@code target}: held where it stands ({@link ModEffects#BOUND}) for {@link #BIND_SECONDS}, a second more for each
	 * realm the talisman ({@code rank}) stands above it, at most {@link #BIND_MORE} more; nothing a realm or more above the talisman. A bound
	 * player is weighed down besides (no flight while it lasts). Returns whether it took hold.
	 */
	public static boolean bind(LivingEntity target, int rank, float power, LivingEntity thrower) {
		int theirs = rankOf(target);
		if (theirs >= rank + ABOVE_BURNER) {
			if (thrower instanceof ServerPlayer p) {
				p.displayClientMessage(Component.translatable(ModLang.MSG_TALISMAN_UNBOUND, target.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
			}
			return false;
		}
		int seconds = BIND_SECONDS + Math.min(BIND_MORE, Math.max(0, (rank - Math.max(0, theirs)) / ABOVE_BURNER));
		int ticks = Math.round(seconds * 20 * power);
		target.addEffect(new MobEffectInstance(ModEffects.BOUND, ticks, 0, false, false, true), thrower);
		if (target instanceof ServerPlayer bound) {
			QiFlight.weighDown(bound, ticks);
			bound.displayClientMessage(Component.translatable(ModLang.MSG_TALISMAN_BOUND).withStyle(ChatFormatting.GOLD), true);
		}
		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.WAX_OFF, target.getX(), target.getEyeY(), target.getZ(), 16, 0.3, 0.3, 0.3, 0.6);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.6f);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.2f);
		}
		return true;
	}

	// --- Burnt ---

	/**
	 * {@code p} burns {@code stack} on themselves: its pattern's work, as strong as it is in their hands. Returns whether it was spent (a Thousand-Li
	 * Escape with nowhere to go isn't).
	 */
	static boolean burn(ServerPlayer p, ItemStack stack, TalismanPattern pattern) {
		ServerLevel level = p.serverLevel();
		int rank = effectiveRank(stack, p);
		double power = Artifacts.power(stack);
		switch (pattern) {
			case GOLDEN_LIGHT -> {
				float shell = (float) (CultivationStats.maxHealth(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank)) * GOLDEN_SHARE * power);
				p.addEffect(new MobEffectInstance(ModEffects.GOLDEN_LIGHT, GOLDEN_TICKS, 0, false, false, true));
				p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), shell));
				aroundThem(level, ParticleTypes.WAX_ON, p, 24, 0.8, 0.05);
				level.playSound(null, p.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.8f, 1.6f);
			}
			case DIVINE_SPEED -> {
				int amplifier = Math.min(4, 1 + rank / 16);
				p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) (SPEED_TICKS * power), amplifier, false, true, true));
				level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 12, 0.3, 0.1, 0.3, 0.05);
				level.playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.3f, 1.8f);
			}
			case THOUSAND_LI_ESCAPE -> {
				return escape(p, stack, power);
			}
			case CONCEALMENT -> conceal(p, rank, power);
			default -> {
				return false;
			}
		}
		return true;
	}

	/**
	 * A Thousand-Li Escape: {@code p} flashes its reach (by its drawer's mastery, {@link #ESCAPE_REACH}, a tenth or a quarter more for its work)
	 * the way they face, onto safe ground there or as near short of it as there is. Not where space is sealed: a Space domain or the Earth's lock
	 * on it, the ways between the worlds, the secret realms, the Inner Realm.
	 */
	static boolean escape(ServerPlayer p, ItemStack stack, double power) {
		ServerLevel level = p.serverLevel();
		if (sealed(p)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_TALISMAN_ESCAPE_SEALED).withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		CompoundTag tag = stack.getTag();
		int reach = (int) Math.round((tag != null && tag.contains(REACH) ? tag.getInt(REACH) : ESCAPE_REACH[0]) * power);
		float yaw = p.getYRot() * Mth.DEG_TO_RAD;
		Vec3 way = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
		BlockPos landing = landing(level, p.position(), way, reach, p);
		if (landing == null) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_TALISMAN_ESCAPE_NO_GROUND).withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		aroundThem(level, ParticleTypes.REVERSE_PORTAL, p, 32, 0.8, 0.05);
		level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
		p.teleportTo(level, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, p.getYRot(), p.getXRot());
		p.connection.resetPosition();
		p.resetFallDistance();
		aroundThem(level, ParticleTypes.REVERSE_PORTAL, p, 32, 0.8, 0.05);
		level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.4f);
		return true;
	}

	/**
	 * {@code count} of {@code particle} in a ring a block out round {@code p}, {@code height} above their feet: about them for whoever looks on,
	 * and out of their own eyes (one spawned by the eyes is drawn as a great square over a first-person view).
	 */
	static void aroundThem(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle, Entity p, int count, double height, double speed) {
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2 * i / count;
			level.sendParticles(particle, p.getX() + Math.cos(angle) * 1.1, p.getY() + height, p.getZ() + Math.sin(angle) * 1.1, 1, 0.05, 0.15, 0.05, speed);
		}
	}

	/** Space sealed where {@code p} is: a Space domain's or the Earth's lock, the ways between the worlds, a secret realm, the Inner Realm. */
	static boolean sealed(ServerPlayer p) {
		var dimension = p.level().dimension();
		return LawDomains.locksSpace(p) || EarthSpells.spaceLocked(p) || ModDimensions.isPassage(dimension) || ModDimensions.isSecretRealm(dimension)
				|| ModDimensions.isInnerRealm(dimension) || ModDimensions.isSpace(dimension);
	}

	/** At most about this many spots are looked at for a Thousand-Li Escape's landing (each one's land loaded, or made, there and then). */
	static final int LANDING_TRIES = 12;

	/** {@link #landing} for anyone: no barrier asked about. */
	static BlockPos landing(ServerLevel level, Vec3 from, Vec3 way, int reach) {
		return landing(level, from, way, reach, null);
	}

	/**
	 * Safe ground {@code reach} blocks along {@code way} from {@code from}, or as near short of it as there is (no nearer than a quarter of the
	 * reach, about {@link #LANDING_TRIES} spots looked at: each one's land is loaded to look, made if it never was, and steps of 8 over a wide sea
	 * made dozens of them a flash, the 2026-11-10 sweep): room to stand, something solid underfoot, no lava; never on a barrier's dome (its ground
	 * is under it: Formations#surface), nor past a barrier {@code crossing} couldn't break or be let through (Formations#walls: what flashes past
	 * it lands no further in than they could walk; before, they landed in a sect's grounds and were only set out again at its next sweep). Under
	 * a roof (the Nether's) looked for about one's own height rather than at the top of the world.
	 */
	static BlockPos landing(ServerLevel level, Vec3 from, Vec3 way, int reach, LivingEntity crossing) {
		boolean roofed = level.dimensionType().hasCeiling();
		BlockPos start = BlockPos.containing(from);
		int step = Math.max(8, reach / LANDING_TRIES);
		for (int d = reach; d >= reach / 4; d -= step) {
			int x = Mth.floor(from.x + way.x * d), z = Mth.floor(from.z + way.z * d);
			if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) continue;
			level.getChunk(x >> 4, z >> 4);
			if (roofed) {
				for (int y = Mth.floor(from.y) + 8; y >= Mth.floor(from.y) - 24; y--) {
					BlockPos feet = new BlockPos(x, y, z);
					if (standable(level, feet) && open(level, start, feet, crossing)) return feet;
				}
				continue;
			}
			int top = Formations.surface(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (top <= level.getMinBuildHeight()) continue;
			BlockPos feet = new BlockPos(x, top, z);
			if (standable(level, feet) && open(level, start, feet, crossing)) return feet;
		}
		return null;
	}

	/** No raised barrier between {@code from} and {@code to} that {@code crossing} couldn't break or be let through (none asked about for null). */
	private static boolean open(ServerLevel level, BlockPos from, BlockPos to, LivingEntity crossing) {
		return crossing == null || !Formations.walls(level, from, to, crossing);
	}

	private static boolean standable(ServerLevel level, BlockPos feet) {
		BlockState below = level.getBlockState(feet.below());
		if (!below.isFaceSturdy(level, feet.below(), Direction.UP) || below.getFluidState().is(FluidTags.LAVA)) return false;
		for (BlockPos at : new BlockPos[] {feet, feet.above()}) {
			BlockState state = level.getBlockState(at);
			if (!state.getCollisionShape(level, at).isEmpty() || !state.getFluidState().isEmpty()) return false;
		}
		return true;
	}

	/**
	 * Concealment: {@code p} fades from notice ({@link ModEffects#CONCEALED}, its amplifier the talisman's {@code rank}): whoever was after them
	 * without being struck by them loses them, and no beast or cultivator takes them up (MobConcealmentMixin), but those two realms above the
	 * talisman. Striking anything ends it ({@link #struck}).
	 */
	static void conceal(ServerPlayer p, int rank, double power) {
		p.addEffect(new MobEffectInstance(ModEffects.CONCEALED, (int) (CONCEAL_TICKS * power), Math.min(127, rank), false, false, true));
		ServerLevel level = p.serverLevel();
		for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64), mob -> mob.getTarget() == p)) {
			if (unseenBy(mob, p)) mob.setTarget(null);
		}
		aroundThem(level, ParticleTypes.SMOKE, p, 20, 0.8, 0.02);
		level.playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.6f, 1.2f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_TALISMAN_CONCEALED).withStyle(ChatFormatting.GRAY), true);
	}

	/** Whether {@code mob} fails to notice {@code target}: concealed, the mob not two realms above the talisman, and not struck by it. */
	public static boolean unseenBy(Mob mob, LivingEntity target) {
		MobEffectInstance concealed = target.getEffect(ModEffects.CONCEALED);
		if (concealed == null) return false;
		if (mob.getLastHurtByMob() == target) return false;
		return rankOf(mob) < concealed.getAmplifier() + SEES_THROUGH;
	}

	/** A blow landed ({@code source}): one concealed who dealt it is concealed no more. */
	public static void struck(DamageSource source) {
		if (source.getEntity() instanceof LivingEntity striker && striker.hasEffect(ModEffects.CONCEALED)) {
			striker.removeEffect(ModEffects.CONCEALED);
			if (striker instanceof ServerPlayer p) p.displayClientMessage(Component.translatable(ModLang.MSG_TALISMAN_REVEALED).withStyle(ChatFormatting.GRAY), true);
		}
	}

	private Talismans() {}
}
