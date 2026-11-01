package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Five Elements Cycling (CultivationMethod#FIVE_ELEMENTS; the methods overhaul's phase 3, made over on 2026-11-01: the user's "it feels like
 * qi circulation but with a minigame", and their pick of the five elements' offerings): the heaven path's way of the five elements taken
 * into oneself, one offering at a time, in the order each gives rise to the next ({@link #ORDER}: wood feeds fire, fire leaves earth, earth
 * bears metal, metal gathers water, water nourishes wood). Meditation gathers nothing on it.
 * <ul>
 *   <li>An offering ({@link #element}: mortal things, herbs, the five elements' demon cores) is sneak-used into the air ({@link #use}); the
 *   cycle may begin with any element.</li>
 *   <li>The fifth closes it: each offering wins {@link #LINK} of the stage at the realm it stands at ({@link #grade}: a mortal thing Qi
 *   Refining's, a herb by its age, a core its own), twice as much a realm above (at most {@link #MOST_ABOVE}), half a realm below
 *   ({@link #worth}); then the elements circulate {@link #REST} ticks, drawn about the body, before another may begin.</li>
 *   <li>An element out of its order is a qi deviation ({@link #deviate}): the cycle lost, the qi shaken as meditation's is, and
 *   {@link #DEVIATION_LOSS} of the stage with it. The same element twice is only refused.</li>
 * </ul>
 * Half talent weight and a twelfth of the years: a resource way of its own, as kin blood and cores are the Demon Path's.
 */
public final class FiveElementsCycling {
	public static final double LINK = 0.1, DEVIATION_LOSS = 0.05;
	public static final int LINKS = 5, MOST_ABOVE = 2, REST = 2400;
	/** The five in the order each gives rise to the next. */
	private static final QiElement[] ORDER = {QiElement.WOOD, QiElement.FIRE, QiElement.EARTH, QiElement.METAL, QiElement.WATER};

	/** Each one's cycle; not saved (a logout, or the server stopping, and it is gone). */
	private static final Map<UUID, Cycle> CYCLES = new HashMap<>();

	private static final class Cycle {
		/** The last element taken in, or null before the first. */
		QiElement last;
		int links;
		/** What the offerings taken in so far will win of the stage, the cycle closed. */
		double worth;
		/** The server tick the elements settle at, once a cycle has closed (0: not circulating). */
		long settles;
	}

	/** The element {@code element} gives rise to (yin: none). */
	public static @Nullable QiElement next(QiElement element) {
		for (int i = 0; i < ORDER.length; i++) if (ORDER[i] == element) return ORDER[(i + 1) % ORDER.length];
		return null;
	}

	/** Whether {@code c} takes the elements in: one of the living, its meridians open, its heaven path practising Five Elements Cycling. */
	public static boolean practises(PlayerCultivation c) {
		return HeavenMethods.canLearn(c) && CultivationMethods.practised(c, TwoPaths.Path.HEAVEN) == CultivationMethod.FIVE_ELEMENTS;
	}

	/**
	 * The element {@code stack} is an offering of, or null if it is none: wood saplings, logs, sticks and the living world's herbs; fire blaze
	 * powder and rods, coal, charcoal, fire charges and magma cream; earth clay, bricks and dirt; metal iron, gold and copper ingots and
	 * nuggets; water a water bucket, prismarine and snowballs; and a demon core of one of the five, its own.
	 */
	public static @Nullable QiElement element(ItemStack stack) {
		if (stack.isEmpty()) return null;
		if (stack.is(ModItems.DEMON_CORE)) {
			QiElement element = DemonCoreItem.element(stack);
			return element == QiElement.YIN ? null : element;
		}
		if (stack.getItem() instanceof GinsengItem && !stack.is(ModItems.OTHER_SHORE_FLOWER) && !stack.is(ModItems.NETHERWORLD_LOTUS)
				|| stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.LOGS) || stack.is(Items.STICK)) return QiElement.WOOD;
		if (stack.is(Items.BLAZE_POWDER) || stack.is(Items.BLAZE_ROD) || stack.is(Items.COAL) || stack.is(Items.CHARCOAL)
				|| stack.is(Items.FIRE_CHARGE) || stack.is(Items.MAGMA_CREAM)) return QiElement.FIRE;
		if (stack.is(Items.CLAY_BALL) || stack.is(Items.BRICK) || stack.is(Items.DIRT)) return QiElement.EARTH;
		if (stack.is(Items.IRON_INGOT) || stack.is(Items.GOLD_INGOT) || stack.is(Items.COPPER_INGOT) || stack.is(Items.IRON_NUGGET)
				|| stack.is(Items.GOLD_NUGGET)) return QiElement.METAL;
		if (stack.is(Items.WATER_BUCKET) || stack.is(Items.PRISMARINE_SHARD) || stack.is(Items.PRISMARINE_CRYSTALS) || stack.is(Items.SNOWBALL))
			return QiElement.WATER;
		return null;
	}

	/**
	 * The realm an offering stands at, or null for none: a demon core its own; a herb by its age and kind (GinsengItem#grade: Foundation
	 * Building under a hundred years, up to Dao Integration from five thousand, a rarer kind a realm higher); a mortal thing Qi Refining's.
	 */
	public static @Nullable Realm grade(ItemStack stack) {
		if (element(stack) == null) return null;
		if (stack.is(ModItems.DEMON_CORE)) return DemonCoreItem.realm(stack);
		if (stack.getItem() instanceof GinsengItem) return GinsengItem.grade(stack);
		return Realm.QI_REFINING;
	}

	/** What an offering of {@code grade} wins of the stage of a heaven path standing in {@code realm}, its cycle closed. */
	public static double worth(Realm grade, Realm realm) {
		return LINK * Math.pow(2, Math.min(MOST_ABOVE, grade.ordinal() - realm.ordinal()));
	}

	/** The colour an element's words are told in. */
	public static ChatFormatting colour(QiElement element) {
		return switch (element) {
			case WOOD -> ChatFormatting.GREEN;
			case FIRE -> ChatFormatting.RED;
			case EARTH -> ChatFormatting.GOLD;
			case WATER -> ChatFormatting.AQUA;
			default -> ChatFormatting.WHITE;
		};
	}

	/** How many of the five {@code player}'s cycle has taken in (0 to 4). */
	public static int links(UUID player) {
		Cycle cycle = CYCLES.get(player);
		return cycle == null ? 0 : cycle.links;
	}

	/**
	 * An item used (UseItemCallback, on both sides): sneaking, an offering in hand, practising it, the offering taken in ({@link #offer});
	 * else it goes on to its own use. The client only sends it on: the server judges.
	 */
	static InteractionResultHolder<ItemStack> use(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!player.isShiftKeyDown() || player.isSpectator()) return InteractionResultHolder.pass(stack);
		QiElement element = element(stack);
		if (element == null || !practises(CultivationManager.forPlayer(player))) return InteractionResultHolder.pass(stack);
		if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);
		if (!offer(p, stack, element)) return InteractionResultHolder.fail(stack);
		ItemStack left = taken(p, stack);
		p.setItemInHand(hand, left);
		return InteractionResultHolder.consume(left);
	}

	/**
	 * {@code p} takes in {@code stack}, an offering of {@code element}: refused (told) with no volume of the realm, its stage full, the
	 * elements still circulating, or the same element again; one out of its order a deviation ({@link #deviate}); else the cycle's next,
	 * the fifth closing it ({@link #close}). @return whether the offering was taken in (and so is gone)
	 */
	static boolean offer(ServerPlayer p, ItemStack stack, QiElement element) {
		PlayerCultivation c = CultivationManager.get(p);
		Realm realm = c.getRealm();
		if (!CultivationMethods.holdsVolume(c, TwoPaths.Path.HEAVEN)) {
			return refuse(p, Component.translatable(ModLang.MSG_METHOD_NO_VOLUME, CultivationMethod.FIVE_ELEMENTS.getDisplayName(), realm.getDisplayName()));
		}
		if (c.isStalled()) return refuse(p, Component.translatable(ModLang.MSG_CYCLE_STALLED));
		Cycle cycle = CYCLES.computeIfAbsent(p.getUUID(), id -> new Cycle());
		long now = p.server.getTickCount();
		if (cycle.settles > now) return refuse(p, Component.translatable(ModLang.MSG_CYCLE_CIRCULATING, (cycle.settles - now + 19) / 20));
		QiElement wanted = cycle.last == null ? null : next(cycle.last);
		if (cycle.last == element) {
			return refuse(p, Component.translatable(ModLang.MSG_CYCLE_SAME, element.getDisplayName(), wanted.getDisplayName()));
		}
		if (wanted != null && element != wanted) {
			deviate(p, c, cycle, element);
			return true;
		}
		cycle.last = element;
		cycle.links++;
		cycle.worth += worth(grade(stack), realm);
		takenIn(p, element);
		if (cycle.links < LINKS) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_CYCLE_LINK, element.getDisplayName(), cycle.links, LINKS, next(element).getDisplayName())
					.withStyle(colour(element)), true);
		} else close(p, cycle, now);
		return true;
	}

	/** The fifth taken in: what the cycle's offerings are worth won (CultivationMethods#grant, its years paid), and the elements circulating. */
	private static void close(ServerPlayer p, Cycle cycle, long now) {
		double share = cycle.worth;
		cycle.last = null;
		cycle.links = 0;
		cycle.worth = 0;
		cycle.settles = now + REST;
		CultivationMethods.grant(p, TwoPaths.Path.HEAVEN, CultivationMethod.FIVE_ELEMENTS, share, false);
		for (QiElement element : ORDER) ring(p, dust(element, 1.1f), 8, 1.1, 0);
		ring(p, ParticleTypes.END_ROD, 12, 1.2, 0.04);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.2f);
		// A share under a hundredth told to a tenth of it, as CultivationMethods#grant tells it.
		double percent = share * 100;
		Object shown = percent < 0.95 ? String.format(Locale.ROOT, "%.1f", percent) : (Object) Math.round(percent);
		p.displayClientMessage(Component.translatable(ModLang.MSG_CYCLE_COMPLETE, shown, REST / 20).withStyle(ChatFormatting.GREEN), true);
	}

	/**
	 * {@code element} forced out of its order: the cycle lost, the qi shaken as a meditation's deviation shakes it (MeditationManager#onCirculation:
	 * a quarter of the pool, dizziness, a blow), and {@link #DEVIATION_LOSS} of the stage with it.
	 */
	private static void deviate(ServerPlayer p, PlayerCultivation c, Cycle cycle, QiElement element) {
		QiElement after = cycle.last;
		cycle.last = null;
		cycle.links = 0;
		cycle.worth = 0;
		CultivationMethods.lose(p, TwoPaths.Path.HEAVEN, DEVIATION_LOSS);
		c.setQi(c.getQi() - c.maxQi() * MeditationManager.DEVIATION_QI_LOSS);
		p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
		ring(p, ParticleTypes.SMOKE, 16, 0.9, 0.01);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0f, 0.6f);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_CYCLE_DEVIATION, element.getDisplayName(), after.getDisplayName()).withStyle(ChatFormatting.RED));
		p.displayClientMessage(Component.translatable(ModLang.MSG_DEVIATION).withStyle(ChatFormatting.RED), true); // over the last link's line
		CultivationManager.sync(p);
		p.hurt(p.damageSources().magic(), MeditationManager.DEVIATION_DAMAGE);
	}

	/**
	 * An element drawn in at the chest from before the body, in its colour and with its sound: at chest height and never within half a block
	 * of the body, so it never fills its taker's own view (dust before the eyes is drawn as a great square).
	 */
	private static void takenIn(ServerPlayer p, QiElement element) {
		ServerLevel level = p.serverLevel();
		DustParticleOptions dust = dust(element, 0.9f);
		double yaw = Math.toRadians(p.getYRot()), ahead = 1.4, y = p.getY() + 1.0;
		double fx = -Math.sin(yaw), fz = Math.cos(yaw);
		for (int i = 0; i <= 6; i++) {
			double d = ahead - (ahead - 0.5) * i / 6.0;
			level.sendParticles(dust, p.getX() + fx * d, y, p.getZ() + fz * d, 2, 0.05, 0.05, 0.05, 0);
		}
		level.playSound(null, p.blockPosition(), sound(element), SoundSource.PLAYERS, 0.9f, 1.2f);
	}

	/** {@code count} particles in a ring about {@code p}'s body at chest height and below (none before its eyes). */
	private static void ring(ServerPlayer p, ParticleOptions particle, int count, double radius, double speed) {
		ServerLevel level = p.serverLevel();
		for (int i = 0; i < count; i++) {
			double a = Math.PI * 2 * i / count;
			level.sendParticles(particle, p.getX() + Math.cos(a) * radius, p.getY() + 0.3 + p.getRandom().nextDouble() * 0.8, p.getZ() + Math.sin(a) * radius,
					1, 0, 0.02, 0, speed);
		}
	}

	private static SoundEvent sound(QiElement element) {
		return switch (element) {
			case WOOD -> SoundEvents.AZALEA_LEAVES_PLACE;
			case FIRE -> SoundEvents.FIRECHARGE_USE;
			case EARTH -> SoundEvents.ROOTED_DIRT_PLACE;
			case METAL -> SoundEvents.CHAIN_PLACE;
			default -> SoundEvents.BUCKET_EMPTY;
		};
	}

	private static DustParticleOptions dust(QiElement element, float size) {
		int rgb = element == QiElement.METAL ? 0xE8E8F0 : element.getCoreColor();
		return new DustParticleOptions(new Vector3f((rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f), size);
	}

	/** What is left in hand once {@code stack} is taken in: one fewer, a water bucket's empty bucket; a creative hand's as it was. */
	private static ItemStack taken(ServerPlayer p, ItemStack stack) {
		if (p.getAbilities().instabuild) return stack;
		if (stack.is(Items.WATER_BUCKET)) return new ItemStack(Items.BUCKET);
		stack.shrink(1);
		return stack;
	}

	private static boolean refuse(ServerPlayer p, Component why) {
		p.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
		return false;
	}

	/** The server's tick: the five drawn circling those whose cycle has closed, and told when they settle. */
	static void tick(MinecraftServer server) {
		if (CYCLES.isEmpty()) return;
		long now = server.getTickCount();
		for (Iterator<Map.Entry<UUID, Cycle>> it = CYCLES.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Cycle> entry = it.next();
			Cycle cycle = entry.getValue();
			if (cycle.settles <= 0) continue;
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			if (p == null || !practises(CultivationManager.get(p))) {
				it.remove();
				continue;
			}
			if (now >= cycle.settles) {
				cycle.settles = 0;
				p.displayClientMessage(Component.translatable(ModLang.MSG_CYCLE_SETTLED).withStyle(ChatFormatting.AQUA), true);
				p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.4f);
				continue;
			}
			if (now % 4 != 0 || !p.isAlive()) continue;
			// The five circling the body a fifth of a turn apart, in their order.
			ServerLevel level = p.serverLevel();
			double turn = now * 0.12;
			for (int i = 0; i < ORDER.length; i++) {
				double a = turn + i * Math.PI * 2 / ORDER.length;
				level.sendParticles(dust(ORDER[i], 0.9f), p.getX() + Math.cos(a) * 1.0, p.getY() + 0.9, p.getZ() + Math.sin(a) * 1.0, 1, 0, 0, 0, 0);
			}
		}
	}

	/** A player left: its cycle goes. */
	public static void forget(UUID id) {
		CYCLES.remove(id);
	}

	/** The server stopped. */
	public static void clear() {
		CYCLES.clear();
	}

	private FiveElementsCycling() {}
}
