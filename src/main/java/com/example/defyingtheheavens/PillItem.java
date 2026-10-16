package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Everything the Alchemy Cauldron brews. Each pill carries the grade it was brewed at and the average age of its ingredients
 * ({@link #GRADE_TAG}, {@link #AGE_TAG}); together they give its {@link #potency}, which scales what it does:
 * <ul>
 *   <li>Marrow Cleansing Elixir: makes a mortal a Qi Refining cultivator, starting part of the way into Early.</li>
 *   <li>Foundation Pill / Core Pill: taken at Grand Perfection, they open the way into Foundation Building / Core Formation
 *   (the tribulation still has to be survived), and the new realm starts part of the way in.</li>
 *   <li>Qi Gathering Pill: more qi gathered per second for a while.</li>
 *   <li>Cultivation Pill: a share of the current stage's cultivation at once, less the pill resistance built up by earlier
 *   ones.</li>
 *   <li>Yang Preservation Pill: holds the Netherworld's yin still in the body for a while (YinCorruption).</li>
 *   <li>Yin Gathering Pill: the ghost method practised is cultivated faster for a while (GhostMethods).</li>
 * </ul>
 * A pill that can't do anything right now isn't eaten: the player is told why instead.
 */
public class PillItem extends Item {
	public enum Kind { MARROW_CLEANSING, FOUNDATION, CORE, QI_GATHERING, CULTIVATION, YANG_PRESERVATION, YIN_GATHERING }

	public static final String GRADE_TAG = "PillGrade";
	public static final String AGE_TAG = "PillAge";

	/** Qi Gathering Pill: extra qi gathering per point of potency (Low grade, fresh ingredients: +25%; at best +250%). */
	public static final double QI_BOOST_PER_POTENCY = 0.25;
	public static final int QI_BOOST_TICKS = 5 * 60 * 20;
	/** Cultivation Pill: share of the current stage's requirement per point of potency (15% to 150%). */
	public static final double CULTIVATION_PER_POTENCY = 0.15;
	/** Breakthrough pills: head start into the new realm per point of potency above 1, up to {@link #MAX_HEAD_START}. */
	public static final double HEAD_START_PER_POTENCY = 0.05;
	public static final double MAX_HEAD_START = 0.5;
	/** Yang Preservation Pill: the yin held still this long per point of potency (3 minutes), at most {@link #MAX_PRESERVATION_TICKS}. */
	public static final int PRESERVATION_TICKS_PER_POTENCY = 3 * 60 * 20;
	public static final int MAX_PRESERVATION_TICKS = 15 * 60 * 20;
	/** Yin Gathering Pill: the ghost method cultivated faster by this much per point of potency, for {@link #YIN_BOOST_TICKS}. */
	public static final double YIN_BOOST_PER_POTENCY = 0.25;
	public static final int YIN_BOOST_TICKS = 5 * 60 * 20;
	/** The colour of the impurities the Marrow Cleansing Elixir drives out: a dull, earthy grey. */
	private static final org.joml.Vector3f IMPURITY = new org.joml.Vector3f(0.45f, 0.41f, 0.34f);

	private final Kind kind;

	public PillItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind getKind() { return kind; }

	// --- Grade and age ---

	public static PillGrade grade(ItemStack stack) {
		return PillGrade.byIndex(stack.hasTag() ? stack.getTag().getInt(GRADE_TAG) : 0);
	}

	/** Average age of the ingredients the pill was brewed from. */
	public static int age(ItemStack stack) {
		return FruitAge.clamp(stack.hasTag() ? stack.getTag().getInt(AGE_TAG) : 1);
	}

	public static ItemStack create(Item item, PillGrade grade, int age) {
		ItemStack stack = new ItemStack(item);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putInt(GRADE_TAG, grade.ordinal());
		tag.putInt(AGE_TAG, FruitAge.clamp(age));
		return stack;
	}

	/**
	 * 1 (Low grade, fresh ingredients) to 10 (Immortal grade, 10,000-year ingredients); twice that brewed by an Innate Alchemy Body
	 * ({@link #ALCHEMY_BODY_TAG}).
	 */
	public static double potency(ItemStack stack) {
		return grade(stack).potency(age(stack)) * (byAlchemyBody(stack) ? ALCHEMY_BODY_FACTOR : 1.0);
	}

	/** Set on a pill brewed by one born with an Innate Alchemy Body (Physique): it works {@link #ALCHEMY_BODY_FACTOR} times as strongly. */
	public static final String ALCHEMY_BODY_TAG = "AlchemyBody";
	public static final double ALCHEMY_BODY_FACTOR = 2.0;

	public static boolean byAlchemyBody(ItemStack stack) {
		return stack.hasTag() && stack.getTag().getBoolean(ALCHEMY_BODY_TAG);
	}

	public static double headStart(ItemStack stack) {
		return Math.min(MAX_HEAD_START, HEAD_START_PER_POTENCY * (potency(stack) - 1));
	}

	public static double qiBoost(ItemStack stack) {
		return QI_BOOST_PER_POTENCY * potency(stack);
	}

	public static double cultivationShare(ItemStack stack) {
		return CULTIVATION_PER_POTENCY * potency(stack);
	}

	/** How long a Yang Preservation Pill holds the yin still (ticks). */
	public static int preservationTicks(ItemStack stack) {
		return (int) Math.min(MAX_PRESERVATION_TICKS, Math.round(PRESERVATION_TICKS_PER_POTENCY * potency(stack)));
	}

	public static double yinBoost(ItemStack stack) {
		return YIN_BOOST_PER_POTENCY * potency(stack);
	}

	/** The pill a breakthrough into {@code target} needs, or null if it needs none. */
	public static Item breakthroughPillFor(Realm target) {
		if (target == Realm.FOUNDATION_BUILDING) return ModItems.FOUNDATION_PILL;
		if (target == Realm.CORE_FORMATION) return ModItems.CORE_PILL;
		return null;
	}

	/** The realm a breakthrough pill opens, or null for the other kinds. */
	private Realm target() {
		return switch (kind) {
			case FOUNDATION -> Realm.FOUNDATION_BUILDING;
			case CORE -> Realm.CORE_FORMATION;
			default -> null;
		};
	}

	// --- Eating ---

	/** Why this pill would be wasted right now, or null if it can be taken. */
	private Component refusal(PlayerCultivation c) {
		switch (kind) {
			case MARROW_CLEANSING:
				if (!c.isMortal()) return Component.translatable(ModLang.MSG_ALREADY_AWAKENED);
				if (c.hasShatteredMeridians()) return Component.translatable(ModLang.MSG_PHYSIQUE_SHATTERED); // nothing to open
				return c.isMortalPeak() ? null : Component.translatable(ModLang.MSG_ELIXIR_TOO_WEAK);
			case FOUNDATION:
			case CORE: {
				Realm target = target();
				if (c.isMortal()) return Component.translatable(ModLang.MSG_PILL_MORTAL);
				if (c.getPreparedRealm() == target) return Component.translatable(ModLang.MSG_PILL_ALREADY, target.getDisplayName());
				if (!c.canPrepare(target)) {
					return Component.translatable(ModLang.MSG_PILL_WRONG_TIME,
							PlayerCultivation.rankName(target.previous(), Stage.GRAND_PERFECTION));
				}
				return null;
			}
			case QI_GATHERING:
				return c.isMortal() ? Component.translatable(ModLang.MSG_PILL_MORTAL) : null;
			case CULTIVATION:
				if (c.isMortal()) return Component.translatable(ModLang.MSG_PILL_MORTAL);
				if (c.isStalled() || c.isSuppressed() || c.isMaxed()) return Component.translatable(ModLang.MSG_PILL_BOTTLENECK);
				return null;
			case YIN_GATHERING:
				if (c.isMortal()) return Component.translatable(ModLang.MSG_PILL_MORTAL); // its ghost methods lie dormant (GhostMethods#refusal)
				return c.netherworld().knowsAnyMethod() ? null : Component.translatable(ModLang.MSG_PILL_NO_METHOD);
			default:
				return null;
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Component refusal = refusal(CultivationManager.forPlayer(player));
		if (refusal != null) {
			if (!level.isClientSide) player.displayClientMessage(refusal, true);
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return kind == Kind.MARROW_CLEANSING ? 32 : 24;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return kind == Kind.MARROW_CLEANSING ? UseAnim.DRINK : UseAnim.EAT;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) return stack;
		PlayerCultivation c = CultivationManager.get(player);
		Component refusal = refusal(c); // things may have changed while it was being eaten
		if (refusal != null) {
			player.displayClientMessage(refusal, true);
			return stack;
		}
		takeEffect(player, c, stack);
		player.awardStat(Stats.ITEM_USED.get(this));
		if (!player.getAbilities().instabuild) stack.shrink(1);
		if (kind == Kind.MARROW_CLEANSING && !player.getAbilities().instabuild) {
			ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
			if (stack.isEmpty()) return bottle;
			if (!player.getInventory().add(bottle)) player.drop(bottle, false);
		}
		return stack;
	}

	private void takeEffect(ServerPlayer player, PlayerCultivation c, ItemStack stack) {
		ServerLevel level = player.serverLevel();
		switch (kind) {
			case MARROW_CLEANSING -> {
				c.awaken(headStart(stack));
				CultivationManager.refresh(player);
				RealmSuppressSystem.refreshNow(); // no longer weighed as a mortal
				// The impurities the elixir drives out of the body, a dusky steam off the skin that's gone at once, then the first
				// breath of qi.
				level.sendParticles(new DustParticleOptions(IMPURITY, 0.9f), player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.35, 0.55, 0.35, 0.0);
				level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
				level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.8, 0.5, 0.08);
				level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
				// The dead may drink it too (Incorporeal#mayUse): the ghost path is theirs now, the heaven path once they live again.
				player.sendSystemMessage(GhostSystem.isGhost(player) ? Component.translatable(ModLang.MSG_AWAKENED_DEAD)
						: Component.translatable(ModLang.MSG_AWAKENED, Realm.QI_REFINING.getDisplayName()));
			}
			case FOUNDATION, CORE -> {
				c.prepareBreakthrough(target(), headStart(stack));
				CultivationManager.refresh(player);
				level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 40, 0.5, 0.6, 0.5, 0.6);
				level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.8f);
				player.sendSystemMessage(Component.translatable(ModLang.MSG_PILL_PREPARED, getDescription(), target().getDisplayName(),
						Math.round(headStart(stack) * 100)));
			}
			case QI_GATHERING -> {
				// A new pill replaces whatever Qi Gathering Pill was still working.
				c.setQiBoost(qiBoost(stack));
				player.addEffect(new MobEffectInstance(ModEffects.QI_GATHERING, QI_BOOST_TICKS, 0, false, false, true));
				CultivationManager.refresh(player);
				level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
				player.displayClientMessage(Component.translatable(ModLang.MSG_PILL_QI, Math.round(qiBoost(stack) * 100),
						QI_BOOST_TICKS / 1200), true);
			}
			case CULTIVATION -> {
				// Its qi goes into the unrefined pool, refined into cultivation by meditating.
				double effectiveness = 1 - c.getPillResistance();
				double amount = c.takeMedicine(c.cultivationRequired() * cultivationShare(stack));
				CultivationManager.refresh(player);
				level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.1f);
				player.displayClientMessage(Component.translatable(ModLang.MSG_PILL_CULTIVATION, Math.round(amount),
						Math.round(effectiveness * 100)), true);
			}
			case YANG_PRESERVATION -> {
				// Its yang warmth holds the Netherworld's yin where it is (YinCorruption), however much of the hold was left.
				int ticks = preservationTicks(stack);
				NetherworldState s = c.netherworld();
				s.setYinPausedUntil(Math.max(s.yinPausedUntil(), level.getGameTime() + ticks));
				player.addEffect(new MobEffectInstance(ModEffects.YANG_PRESERVATION, ticks, 0, false, false, true));
				CultivationManager.markDirty(player.server);
				NetherworldManager.refresh(player);
				level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.35, 0.5, 0.35, 0.01);
				level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.3f);
				player.displayClientMessage(Component.translatable(ModLang.MSG_PILL_YANG, Math.round(ticks / 1200.0)), true);
			}
			case YIN_GATHERING -> {
				c.netherworld().setMethodBoost(yinBoost(stack));
				player.addEffect(new MobEffectInstance(ModEffects.YIN_GATHERING, YIN_BOOST_TICKS, 0, false, false, true));
				CultivationManager.markDirty(player.server);
				level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.35, 0.5, 0.35, 0.02);
				level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.9f);
				player.displayClientMessage(Component.translatable(ModLang.MSG_PILL_YIN, Math.round(yinBoost(stack) * 100),
						YIN_BOOST_TICKS / 1200), true);
			}
		}
	}

	// --- Name and tooltip ---

	@Override
	public Component getName(ItemStack stack) {
		return grade(stack).name(super.getName(stack));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return grade(stack) == PillGrade.IMMORTAL;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.PILL_AGE, age(stack)).withStyle(ChatFormatting.GOLD));
		if (byAlchemyBody(stack)) Tooltips.add(lines, Component.translatable(ModLang.PILL_ALCHEMY_BODY).withStyle(ChatFormatting.LIGHT_PURPLE));
		switch (kind) {
			case MARROW_CLEANSING -> {
				Tooltips.add(lines, Component.translatable(ModLang.PILL_EFFECT_AWAKEN).withStyle(ChatFormatting.GREEN));
				headStartLine(stack, lines);
			}
			case FOUNDATION, CORE -> {
				Tooltips.add(lines, Component.translatable(ModLang.PILL_EFFECT_BREAKTHROUGH, target().getDisplayName()).withStyle(ChatFormatting.GREEN));
				Tooltips.add(lines, Component.translatable(ModLang.PILL_WHEN, PlayerCultivation.rankName(target().previous(), Stage.GRAND_PERFECTION))
						.withStyle(ChatFormatting.GRAY));
				headStartLine(stack, lines);
			}
			case QI_GATHERING -> Tooltips.add(lines, Component.translatable(ModLang.PILL_EFFECT_QI, Math.round(qiBoost(stack) * 100),
					QI_BOOST_TICKS / 1200).withStyle(ChatFormatting.AQUA));
			case CULTIVATION -> {
				Tooltips.add(lines, Component.translatable(ModLang.PILL_EFFECT_CULTIVATION, Math.round(cultivationShare(stack) * 100))
						.withStyle(ChatFormatting.GREEN));
				double resistance = CultivationManager.clientMirror.get().getPillResistance();
				if (resistance > 0.005) {
					Tooltips.add(lines, Component.translatable(ModLang.PILL_EFFECT_RESISTANCE, Math.round((1 - resistance) * 100))
							.withStyle(ChatFormatting.RED));
				}
			}
			case YANG_PRESERVATION -> Tooltips.add(lines, Component.translatable(ModLang.PILL_EFFECT_YANG,
					Math.round(preservationTicks(stack) / 1200.0)).withStyle(ChatFormatting.GOLD));
			case YIN_GATHERING -> Tooltips.add(lines, Component.translatable(ModLang.PILL_EFFECT_YIN, Math.round(yinBoost(stack) * 100),
					YIN_BOOST_TICKS / 1200).withStyle(ChatFormatting.AQUA));
		}
		Tooltips.add(lines, Component.translatable(ModLang.PILL_RECIPE, AlchemyRecipes.describe(this)).withStyle(ChatFormatting.DARK_GRAY));
	}

	private static void headStartLine(ItemStack stack, List<Component> lines) {
		long percent = Math.round(headStart(stack) * 100);
		if (percent > 0) Tooltips.add(lines, Component.translatable(ModLang.PILL_EFFECT_START, percent).withStyle(ChatFormatting.AQUA));
	}
}
