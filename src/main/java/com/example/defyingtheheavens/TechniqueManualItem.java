package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A Technique Manual (the user's ask, 2026-10-23): a bound manual holding one technique (its "Technique" tag: Technique) or one spell
 * (its "Spell" tag), and who wrote it out ("Author"), if anyone did. Read (used), it teaches what it holds to one who stands at its
 * realm, and isn't used up; one below it is told what realm it wants. A spell learned from one is the reader's apart from any method
 * (Spell#byManual), wielded only at its realm (and, an element's, by roots that take its element).
 * <p>
 * The sects keep the techniques' manuals in their masters' halls and their libraries (loot_tables/chests/sect_master_hall.json,
 * sect_library.json). Anyone who knows a technique, or can cast a spell of an element or a ghost method, may write it out again on a
 * Blank Manual ({@link #offerInscription}: the Inscription Brush in one hand, the blank in the other), for whoever has lost theirs.
 */
public class TechniqueManualItem extends Item {
	private static final String TECHNIQUE = "Technique", SPELL = "Spell", AUTHOR = "Author";

	public TechniqueManualItem(Properties properties) {
		super(properties);
	}

	/** A manual of {@code technique}. */
	public static ItemStack of(Technique technique) {
		ItemStack stack = new ItemStack(ModItems.TECHNIQUE_MANUAL);
		stack.getOrCreateTag().putString(TECHNIQUE, technique.getId());
		return stack;
	}

	/** A manual of {@code spell}. */
	public static ItemStack of(Spell spell) {
		ItemStack stack = new ItemStack(ModItems.TECHNIQUE_MANUAL);
		stack.getOrCreateTag().putString(SPELL, spell.getId());
		return stack;
	}

	public static Technique technique(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(TECHNIQUE) ? Technique.byId(stack.getTag().getString(TECHNIQUE)) : null;
	}

	public static Spell spell(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(SPELL) ? Spell.byId(stack.getTag().getString(SPELL)) : null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Technique technique = technique(stack);
		Spell spell = spell(stack);
		if (technique == null && spell == null) return InteractionResultHolder.pass(stack);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
		if (!(technique != null ? learn(sp, technique) : learn(sp, spell))) return InteractionResultHolder.fail(stack);
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.2f);
		return InteractionResultHolder.consume(stack);
	}

	/**
	 * {@code p} reads a manual of {@code technique}: learned, at its realm (a clone learns it for its maker: it plays their record,
	 * Clones). @return whether it was (else they are told why not)
	 */
	public static boolean learn(ServerPlayer p, Technique technique) {
		PlayerCultivation c = CultivationManager.get(p);
		if (c.knowsTechnique(technique)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_MANUAL_KNOWN, technique.getDisplayName()), true);
			return false;
		}
		if (!technique.wieldable(c)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_MANUAL_TOO_LOW, technique.getDisplayName(),
					PlayerCultivation.rankName(technique.realm(), Stage.EARLY)), true);
			return false;
		}
		c.learnTechnique(technique);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_MANUAL_LEARNED, technique.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		return true;
	}

	/** {@code p} reads a manual of {@code spell}: learned apart from any method, at its realm (Spell#manualReach). @return whether it was */
	public static boolean learn(ServerPlayer p, Spell spell) {
		PlayerCultivation c = CultivationManager.get(p);
		if (spell.isKnown(c) || c.knowsByManual(spell)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_MANUAL_KNOWN, spell.getDisplayName()), true);
			return false;
		}
		if (!spell.manualReach(c)) {
			String why = spell.isHeaven() && c.netherworld().ghost() ? ModLang.MSG_MANUAL_DEAD
					: spell.isHeaven() && !c.rootsAllow(spell.element()) ? ModLang.MSG_MANUAL_ROOTS : ModLang.MSG_MANUAL_TOO_LOW;
			p.displayClientMessage(Component.translatable(why, spell.getDisplayName(), needs(spell)), true);
			return false;
		}
		c.learnFromManual(spell);
		c.netherworld().addToWheel(spell);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		Spells.sync(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_MANUAL_LEARNED, spell.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		return true;
	}

	/** The realm and stage {@code spell}'s manual wants, as named ("Nascent Soul - Mid"; a ghost method's on the ghost path). */
	private static Component needs(Spell spell) {
		int rank = Math.max(0, spell.realmNeeded()), stages = Stage.values().length;
		Component name = PlayerCultivation.rankName(Realm.byIndex(rank / stages), Stage.byIndex(rank % stages));
		return spell.isHeaven() ? name : Component.translatable(ModLang.TOOLTIP_MANUAL_GHOST, name);
	}

	// --- Writing one out ---

	/** What {@code p} may write out now: their techniques at their realm (kind 0), and every spell they can cast, but those of both paths (1). */
	public static List<int[]> writable(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		List<int[]> options = new ArrayList<>();
		for (Technique technique : c.techniques()) if (technique.wieldable(c)) options.add(new int[] {0, technique.ordinal()});
		for (Spell spell : Spell.values()) if (!spell.isDual() && spell.isKnown(c)) options.add(new int[] {1, spell.ordinal()});
		return options;
	}

	/**
	 * The Inscription Brush used with a Blank Manual in the other hand (InscriptionBrushItem): what {@code p} may write out is shown to
	 * pick from (S2C inscribe_options), or they are told there is nothing.
	 */
	public static void offerInscription(ServerPlayer p) {
		List<int[]> options = writable(p);
		if (options.isEmpty()) {
			p.displayClientMessage(Component.translatable(ModLang.INSCRIBE_NONE), true);
			return;
		}
		ModPackets.sendInscribeOptions(p, options);
	}

	/**
	 * C2S inscribe: {@code p} writes out what they picked ({@code kind} 0 a technique, 1 a spell; {@code index} its ordinal) on the Blank
	 * Manual in one hand with the brush in the other: the blank (one of them) becomes its manual, signed with their name, and the brush
	 * a use more worn. Nothing happens unless they hold both and may write it ({@link #writable}).
	 */
	public static void inscribe(ServerPlayer p, int kind, int index) {
		InteractionHand blankHand = p.getMainHandItem().is(ModItems.BLANK_MANUAL) ? InteractionHand.MAIN_HAND
				: p.getOffhandItem().is(ModItems.BLANK_MANUAL) ? InteractionHand.OFF_HAND : null;
		if (blankHand == null) return;
		InteractionHand brushHand = blankHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		ItemStack brush = p.getItemInHand(brushHand);
		if (!brush.is(ModItems.INSCRIPTION_BRUSH)) return;
		boolean allowed = false;
		for (int[] option : writable(p)) allowed |= option[0] == kind && option[1] == index;
		if (!allowed) return;
		Technique technique = kind == 0 ? Technique.byIndex(index) : null;
		Spell spell = kind == 1 ? Spell.byIndex(index) : null;
		if (technique == null && spell == null) return;
		ItemStack written = technique != null ? of(technique) : of(spell);
		written.getOrCreateTag().putString(AUTHOR, p.getGameProfile().getName());
		ItemStack blank = p.getItemInHand(blankHand);
		blank.shrink(1);
		if (blank.isEmpty()) p.setItemInHand(blankHand, written);
		else if (!p.getInventory().add(written)) p.drop(written, false);
		brush.hurtAndBreak(1, p, who -> who.broadcastBreakEvent(brushHand));
		p.level().playSound(null, p.blockPosition(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.8f, 1.0f);
		p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.0f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_INSCRIBED, technique != null ? technique.getDisplayName() : spell.getDisplayName())
				.withStyle(ChatFormatting.GOLD), true);
	}

	// --- Its face ---

	@Override
	public Component getName(ItemStack stack) {
		Technique technique = technique(stack);
		if (technique != null) return Component.translatable(ModLang.ITEM_TECHNIQUE_MANUAL_OF, technique.getDisplayName());
		Spell spell = spell(stack);
		return spell == null ? super.getName(stack) : Component.translatable(ModLang.ITEM_SPELL_MANUAL_OF, spell.getDisplayName());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		Technique technique = technique(stack);
		Spell spell = spell(stack);
		if (technique != null) {
			lines.add(Component.translatable(ModLang.TOOLTIP_MANUAL_REALM, PlayerCultivation.rankName(technique.realm(), Stage.EARLY)).withStyle(ChatFormatting.GOLD));
			Tooltips.add(lines, technique.getDescription().copy().withStyle(ChatFormatting.GRAY));
		} else if (spell != null) {
			lines.add(Component.translatable(ModLang.TOOLTIP_MANUAL_REALM, needs(spell)).withStyle(ChatFormatting.GOLD));
		}
		if (stack.hasTag() && stack.getTag().contains(AUTHOR)) {
			lines.add(Component.translatable(ModLang.TOOLTIP_MANUAL_AUTHOR, stack.getTag().getString(AUTHOR)).withStyle(ChatFormatting.DARK_GRAY));
		}
		lines.add(Component.translatable(ModLang.TOOLTIP_MANUAL).withStyle(ChatFormatting.GRAY));
	}
}
