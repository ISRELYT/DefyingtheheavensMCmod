package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Spirit stones: heaven and earth's qi, condensed over ages into small, smooth oval stones with a pearl's sheen. Cultivators
 * trade in them (see CultivatorTrades), and five grades each hold nine times the one below: Low, Mid, High, Supreme and
 * Immortal (nine of one grade craft into one of the next, and back). Mined from spirit stone veins (the richer grades mostly
 * in the Upper Realm), kept in sect treasuries, carried by cultivators. They glow, the more the higher the grade (the
 * client's SpiritStoneGlow).
 * <ul>
 *   <li><b>Absorbing</b>: hold right-click for {@link #ABSORB_TICKS} to draw a stone's qi into your pool, {@link #QI_PER_VALUE}
 *   for every Low stone's worth it holds; the stone is used up.</li>
 *   <li><b>Meditating</b> with one in either hand cultivates {@link #MEDITATION_BOOST} faster while its qi lasts:
 *   {@link #SECONDS_PER_LOW} seconds for every Low stone's worth, then the stone is spent (how much is left shows on it).</li>
 * </ul>
 */
public class SpiritStoneItem extends Item {
	/** The five grades, each worth nine of the one below ({@link #value}, in Low stones). */
	public enum Grade {
		LOW("low", 1, 0xF2EEE6, Rarity.COMMON),
		MID("mid", 9, 0x7FD8A8, Rarity.UNCOMMON),
		HIGH("high", 81, 0x7FB8F2, Rarity.RARE),
		SUPREME("supreme", 729, 0xC9A0F2, Rarity.EPIC),
		IMMORTAL("immortal", 6561, 0xF2D27F, Rarity.EPIC);

		private final String id;
		private final int value;
		private final int color;
		private final Rarity rarity;

		Grade(String id, int value, int color, Rarity rarity) {
			this.id = id;
			this.value = value;
			this.color = color;
			this.rarity = rarity;
		}

		public String getId() { return id; }
		/** What it is worth, in Low stones. */
		public int value() { return value; }
		public int color() { return color; }
		public Rarity rarity() { return rarity; }
		public Grade next() { return this == IMMORTAL ? null : values()[ordinal() + 1]; }
	}

	/** Qi drawn from a Low stone (a quarter of a Qi Refining Early pool); each grade holds nine times as much. */
	public static final double QI_PER_VALUE = 25;
	/** How long absorbing one takes (ticks). */
	public static final int ABSORB_TICKS = 30;
	/** Meditating with one in hand: this much faster. */
	public static final double MEDITATION_BOOST = 0.5;
	/** Seconds of that boost one Low stone's worth feeds. */
	public static final int SECONDS_PER_LOW = 30;
	/** Seconds of meditation already drawn from the stone on top of a stack. */
	private static final String DRAWN_TAG = "QiDrawn";

	private final Grade grade;

	public SpiritStoneItem(Grade grade, Properties properties) {
		super(properties.rarity(grade.rarity()));
		this.grade = grade;
	}

	public Grade grade() { return grade; }

	public static Item of(Grade grade) {
		return switch (grade) {
			case LOW -> ModItems.LOW_SPIRIT_STONE;
			case MID -> ModItems.MID_SPIRIT_STONE;
			case HIGH -> ModItems.HIGH_SPIRIT_STONE;
			case SUPREME -> ModItems.SUPREME_SPIRIT_STONE;
			case IMMORTAL -> ModItems.IMMORTAL_SPIRIT_STONE;
		};
	}

	/** Worth in Low stones of {@code stack} if it is spirit stones (the whole stack), else 0. */
	public static int valueOf(ItemStack stack) {
		return stack.getItem() instanceof SpiritStoneItem stone ? stone.grade.value() * stack.getCount() : 0;
	}

	/**
	 * {@code value} Low stones' worth as stones, the fewest that make it up (largest grades first), each stack no larger than
	 * the item allows.
	 */
	public static List<ItemStack> stonesFor(int value) {
		List<ItemStack> stacks = new java.util.ArrayList<>();
		Grade[] grades = Grade.values();
		for (int i = grades.length - 1; i >= 0 && value > 0; i--) {
			int count = value / grades[i].value();
			value -= count * grades[i].value();
			while (count > 0) {
				int n = Math.min(count, 64);
				stacks.add(new ItemStack(of(grades[i]), n));
				count -= n;
			}
		}
		return stacks;
	}

	// --- Absorbing ---

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		PlayerCultivation c = CultivationManager.forPlayer(player);
		if (c.isMortal() || c.getQi() >= c.maxQi() - 0.5) {
			if (!level.isClientSide) {
				player.displayClientMessage(Component.translatable(c.isMortal() ? ModLang.MSG_STONE_MORTAL : ModLang.MSG_STONE_QI_FULL), true);
			}
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return ABSORB_TICKS;
	}

	/** No pose (BLOCK would raise it like a shield, and block blows with it): the qi streaming in shows the draw. */
	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.NONE;
	}

	/** While drawing it in, its qi streams from the stone into the chest. */
	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
		if (!(level instanceof ServerLevel server) || remaining % 3 != 0) return;
		Vec3 look = user.getLookAngle();
		Vec3 hand = user.getEyePosition().add(look.scale(0.6)).add(0, -0.4, 0);
		Vec3 chest = user.position().add(0, user.getBbHeight() * 0.6, 0);
		Vec3 step = chest.subtract(hand);
		DustParticleOptions dust = new DustParticleOptions(colour(grade), 0.8f);
		for (int i = 0; i < 3; i++) {
			double t = user.getRandom().nextDouble();
			Vec3 at = hand.add(step.scale(t));
			server.sendParticles(dust, at.x, at.y, at.z, 1, 0.04, 0.04, 0.04, 0);
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		if (!(user instanceof ServerPlayer player)) return stack;
		PlayerCultivation c = CultivationManager.get(player);
		double before = c.getQi();
		c.setQi(Math.min(c.maxQi(), before + QI_PER_VALUE * grade.value()));
		int gained = (int) Math.round(c.getQi() - before);
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.3f);
		player.displayClientMessage(Component.translatable(ModLang.MSG_STONE_ABSORBED, gained), true);
		if (!player.getAbilities().instabuild) stack.shrink(1);
		return stack;
	}

	// --- Meditating ---

	/** The stone a meditator holds (main hand first), or an empty stack. */
	public static ItemStack heldStone(Player player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof SpiritStoneItem) return stack;
		}
		return ItemStack.EMPTY;
	}

	/** Meditation's speed with what {@code player} holds: x1.5 with a spirit stone in hand. */
	public static double meditationMultiplier(Player player) {
		return heldStone(player).isEmpty() ? 1.0 : 1.0 + MEDITATION_BOOST;
	}

	/** Seconds of meditation one stone of {@code grade} feeds. */
	public static int secondsPerStone(Grade grade) {
		return SECONDS_PER_LOW * grade.value();
	}

	/** A second of meditation drawn from the stone in hand; when its qi is spent, it is used up. Qi streams from it as it goes. */
	public static void drainWhileMeditating(ServerPlayer player) {
		ItemStack stack = heldStone(player);
		if (!(stack.getItem() instanceof SpiritStoneItem stone)) return;
		int drawn = stack.getOrCreateTag().getInt(DRAWN_TAG) + 1;
		if (drawn >= secondsPerStone(stone.grade)) {
			stack.removeTagKey(DRAWN_TAG);
			if (!player.getAbilities().instabuild) stack.shrink(1);
			player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.6f, 1.4f);
		} else {
			stack.getTag().putInt(DRAWN_TAG, drawn);
		}
		ServerLevel level = player.serverLevel();
		DustParticleOptions dust = new DustParticleOptions(colour(stone.grade), 0.7f);
		level.sendParticles(dust, player.getX(), player.getY() + 0.9, player.getZ(), 4, 0.35, 0.3, 0.35, 0);
	}

	/** How much of the top stone's qi is left, 0-1. */
	public static double qiLeft(ItemStack stack) {
		if (!(stack.getItem() instanceof SpiritStoneItem stone) || stack.getTag() == null) return 1.0;
		return 1.0 - stack.getTag().getInt(DRAWN_TAG) / (double) secondsPerStone(stone.grade);
	}

	private static Vector3f colour(Grade grade) {
		int c = grade.color();
		return new Vector3f(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return grade.ordinal() >= Grade.SUPREME.ordinal();
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		if (grade != Grade.LOW) tooltip.add(Component.translatable(ModLang.STONE_WORTH, grade.value()).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable(ModLang.STONE_ABSORB, (int) (QI_PER_VALUE * grade.value())).withStyle(ChatFormatting.DARK_AQUA));
		tooltip.add(Component.translatable(ModLang.STONE_MEDITATE, (int) Math.round(MEDITATION_BOOST * 100)).withStyle(ChatFormatting.DARK_AQUA));
		double left = qiLeft(stack);
		if (left < 1.0) tooltip.add(Component.translatable(ModLang.STONE_QI_LEFT, (int) Math.round(left * 100)).withStyle(ChatFormatting.YELLOW));
	}
}
