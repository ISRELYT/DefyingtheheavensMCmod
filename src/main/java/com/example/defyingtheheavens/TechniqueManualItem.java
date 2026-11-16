package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * An Art Manual (the user's asks of 2026-11-16: "arts can still be found from art manuals"; the heaven scriptures and the ghost foundations made
 * Art Manuals; "i want you to learn spells immediately upon using the arts manual"): a bound manual holding one art, of three kinds:
 * <ul>
 *   <li>a heaven art (its "HeavenMethod" tag: a scripture, HeavenMethod): its element's arts of every realm it spans and the element's passive,
 *   and the element Qi Circulation runs by while it follows it (HeavenMethods#cultivationFactor); no method's volumes (Method Manuals hold those);</li>
 *   <li>a ghost art (its "GhostMethod" tag: GhostMethod): its spells, abilities and gains on the ghost path;</li>
 *   <li>one spell (its "Spell" tag), the reader's apart from any art (Spell#byManual), wielded only at its realm.</li>
 * </ul>
 * Read (used), everything in it is learned at once, each spell wielded as the realm (the ghost path, a ghost art's) reaches it (Spell#isKnown), and
 * it isn't used up. Who wrote one out ("Author"), if anyone did. Its id is the old Technique Manual's: one of a technique from before turns into
 * that technique's jade slip as it is carried or read (TechniqueSlipItem, {@link #of(Technique)}), as the old Jade Slips and Ghost Jade Slips turn
 * into Art Manuals (JadeSlipItem, GhostJadeSlipItem).
 * <p>
 * The sects keep them in their libraries and masters' halls; cultivators sell and drop their own art's. Anyone who knows a technique, or can cast a
 * spell of an element or a ghost art, may write it out again on a Blank Manual ({@link #offerInscription}: the Inscription Brush in one hand, the
 * blank in the other), for whoever has lost theirs: a spell as its Art Manual, a technique as its slip.
 */
public class TechniqueManualItem extends Item {
	private static final String TECHNIQUE = TechniqueSlipItem.TECHNIQUE, SPELL = "Spell", HEAVEN = "HeavenMethod", GHOST = "GhostMethod",
			AUTHOR = TechniqueSlipItem.AUTHOR;

	public TechniqueManualItem(Properties properties) {
		super(properties);
	}

	/** A slip of {@code technique} (the techniques are slips since 2026-11-16: TechniqueSlipItem). */
	public static ItemStack of(Technique technique) {
		return TechniqueSlipItem.of(technique);
	}

	/** An Art Manual of {@code spell} alone. */
	public static ItemStack of(Spell spell) {
		ItemStack stack = new ItemStack(ModItems.TECHNIQUE_MANUAL);
		stack.getOrCreateTag().putString(SPELL, spell.getId());
		return stack;
	}

	/** An Art Manual of the heaven art {@code method} (a scripture). */
	public static ItemStack of(HeavenMethod method) {
		ItemStack stack = new ItemStack(ModItems.TECHNIQUE_MANUAL);
		stack.getOrCreateTag().putString(HEAVEN, method.getId());
		return stack;
	}

	/** An Art Manual of the ghost art {@code method}. */
	public static ItemStack of(GhostMethod method) {
		ItemStack stack = new ItemStack(ModItems.TECHNIQUE_MANUAL);
		stack.getOrCreateTag().putString(GHOST, method.getId());
		return stack;
	}

	/** The technique a technique's slip holds, or a manual of one from before. */
	public static Technique technique(ItemStack stack) {
		if (stack.is(ModItems.TECHNIQUE_JADE_SLIP)) return TechniqueSlipItem.technique(stack);
		return stack.hasTag() && stack.getTag().contains(TECHNIQUE) ? Technique.byId(stack.getTag().getString(TECHNIQUE)) : null;
	}

	public static Spell spell(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(SPELL) ? Spell.byId(stack.getTag().getString(SPELL)) : null;
	}

	public static HeavenMethod heavenArt(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(HEAVEN) ? HeavenMethod.byId(stack.getTag().getString(HEAVEN)) : null;
	}

	public static GhostMethod ghostArt(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(GHOST) ? GhostMethod.byId(stack.getTag().getString(GHOST)) : null;
	}

	/** A manual of a technique from before as its slip now (its author kept); null for any other stack. */
	static ItemStack converted(ItemStack stack) {
		if (!stack.is(ModItems.TECHNIQUE_MANUAL)) return null;
		Technique technique = technique(stack);
		if (technique == null) return null;
		ItemStack slip = TechniqueSlipItem.of(technique);
		if (stack.getTag().contains(AUTHOR)) slip.getOrCreateTag().putString(AUTHOR, stack.getTag().getString(AUTHOR));
		return slip;
	}

	/** {@code stack}, carried by {@code player}, swapped where it lies for {@code into}. */
	static void swap(Player player, ItemStack stack, ItemStack into) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i) == stack) {
				inventory.setItem(i, into);
				return;
			}
		}
	}

	/** A technique's manual from before turns into its slip as it is carried. */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide || !(entity instanceof Player player)) return;
		ItemStack slip = converted(stack);
		if (slip != null) swap(player, stack, slip);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Technique technique = technique(stack);
		Spell spell = spell(stack);
		HeavenMethod heaven = heavenArt(stack);
		GhostMethod ghost = ghostArt(stack);
		if (technique == null && spell == null && heaven == null && ghost == null) return InteractionResultHolder.pass(stack);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
		if (technique != null) { // one from before: its slip now, read
			ItemStack slip = converted(stack);
			player.setItemInHand(hand, slip);
			if (!learn(sp, technique)) return InteractionResultHolder.fail(slip);
			read(sp, 1.0f);
			return InteractionResultHolder.consume(slip);
		}
		boolean learned;
		if (heaven != null) {
			learned = !known(sp, CultivationManager.get(sp).heaven().knows(heaven), heaven.getDisplayName()) && HeavenMethods.learn(sp, heaven);
		} else if (ghost != null) {
			learned = !known(sp, CultivationManager.get(sp).netherworld().knows(ghost), ghost.getDisplayName()) && GhostMethods.learn(sp, ghost);
		} else {
			learned = learn(sp, spell);
		}
		if (!learned) return InteractionResultHolder.fail(stack);
		read(sp, ghost != null ? 0.6f : 1.2f);
		return InteractionResultHolder.consume(stack);
	}

	/** Whether the art is {@code known} already (told so). */
	private static boolean known(ServerPlayer p, boolean known, Component name) {
		if (known) p.displayClientMessage(Component.translatable(ModLang.MSG_SLIP_KNOWN, name), true);
		return known;
	}

	private static void read(ServerPlayer p, float pitch) {
		p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
		p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, pitch);
	}

	/**
	 * {@code p} reads a slip (or a manual from before) of {@code technique}: learned, at its realm (a clone learns it for its maker: it plays
	 * their record, Clones). @return whether it was (else they are told why not)
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

	/** {@code p} reads a manual of {@code spell}: learned apart from any art, at its realm (Spell#manualReach). @return whether it was */
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

	/** The realm and stage {@code spell}'s manual wants, as named ("Nascent Soul - Mid"; a ghost art's on the ghost path). */
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
	 * Manual in one hand with the brush in the other: the blank (one of them) becomes its Art Manual (a technique's slip), signed with their
	 * name, and the brush a use more worn. Nothing happens unless they hold both and may write it ({@link #writable}).
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
		if (technique != null) return Component.translatable(ModLang.ITEM_TECHNIQUE_SLIP_OF, technique.getDisplayName());
		HeavenMethod heaven = heavenArt(stack);
		if (heaven != null) return Component.translatable(ModLang.ITEM_SPELL_MANUAL_OF, heaven.getDisplayName());
		GhostMethod ghost = ghostArt(stack);
		if (ghost != null) return Component.translatable(ModLang.ITEM_SPELL_MANUAL_OF, ghost.getDisplayName());
		Spell spell = spell(stack);
		return spell == null ? super.getName(stack) : Component.translatable(ModLang.ITEM_SPELL_MANUAL_OF, spell.getDisplayName());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		Technique technique = technique(stack);
		HeavenMethod heaven = heavenArt(stack);
		GhostMethod ghost = ghostArt(stack);
		Spell spell = spell(stack);
		if (technique != null) {
			lines.add(Component.translatable(ModLang.TOOLTIP_MANUAL_REALM, PlayerCultivation.rankName(technique.realm(), Stage.EARLY)).withStyle(ChatFormatting.GOLD));
			Tooltips.add(lines, technique.getDescription().copy().withStyle(ChatFormatting.GRAY));
		} else if (heaven != null) {
			lines.add(Component.translatable(ModLang.TOOLTIP_ART_HEAVEN, heaven.element().getDisplayName(), HeavenMethods.artsOf(heaven), heaven.realm().getDisplayName())
					.withStyle(ChatFormatting.GOLD));
			lines.add(Component.translatable(ModLang.TOOLTIP_ART_READ).withStyle(ChatFormatting.GRAY));
		} else if (ghost != null) {
			lines.add((ghost.realm() == null ? Component.translatable(ModLang.TOOLTIP_ART_GHOST_OWN)
					: Component.translatable(ModLang.TOOLTIP_ART_GHOST, ghost.realm().getDisplayName())).withStyle(ChatFormatting.AQUA));
			lines.add(Component.translatable(ModLang.TOOLTIP_ART_READ_GHOST).withStyle(ChatFormatting.GRAY));
		} else if (spell != null) {
			lines.add(Component.translatable(ModLang.TOOLTIP_MANUAL_REALM, needs(spell)).withStyle(ChatFormatting.GOLD));
		}
		if (stack.hasTag() && stack.getTag().contains(AUTHOR)) {
			lines.add(Component.translatable(ModLang.TOOLTIP_MANUAL_AUTHOR, stack.getTag().getString(AUTHOR)).withStyle(ChatFormatting.DARK_GRAY));
		}
		if (heaven == null && ghost == null) lines.add(Component.translatable(ModLang.TOOLTIP_MANUAL).withStyle(ChatFormatting.GRAY));
	}
}
