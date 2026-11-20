package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A Sub-Seal (puppet phase 2c; the design's command-authority ladder: "control can be handed over by presenting it"): a sheet of paper
 * its puppeteer writes for one of their puppets on the Command Seal's page (PuppetCommands), bound to that puppet. Whoever holds it
 * right-clicks the puppet: it follows them and fights for them; again, it guards where they stand; again, it holds; again, it follows
 * (PuppetEntity#answerSubSeal). Its puppeteer's own order takes it back; their Revoke voids every Sub-Seal written for it before, as
 * a seizing does (the mark the seal carries no longer the puppet's).
 */
public class SubSealItem extends Item {
	private static final String PUPPET = "Puppet", NAME = "PuppetName", OWNER = "Owner", OWNER_NAME = "OwnerName", MARK = "Mark";

	public SubSealItem(Properties properties) {
		super(properties);
	}

	/** A Sub-Seal for {@code puppet}, written by its puppeteer {@code owner}. */
	public static ItemStack of(PuppetEntity puppet, ServerPlayer owner) {
		ItemStack seal = new ItemStack(ModItems.SUB_SEAL);
		CompoundTag tag = seal.getOrCreateTag();
		tag.putUUID(PUPPET, puppet.getUUID());
		tag.putString(NAME, puppet.getDisplayName().getString());
		tag.putUUID(OWNER, owner.getUUID());
		tag.putString(OWNER_NAME, owner.getGameProfile().getName());
		tag.putInt(MARK, puppet.subNonce());
		return seal;
	}

	/** The puppet {@code seal} is bound to, or null for a blank one. */
	public static @Nullable UUID puppet(ItemStack seal) {
		CompoundTag tag = seal.getTag();
		return tag != null && tag.hasUUID(PUPPET) ? tag.getUUID(PUPPET) : null;
	}

	/** Whether {@code seal} still holds {@code puppet}: written by its puppeteer as it is now, since it was last revoked. */
	public static boolean holds(ItemStack seal, PuppetEntity puppet) {
		CompoundTag tag = seal.getTag();
		return tag != null && tag.hasUUID(OWNER) && tag.getUUID(OWNER).equals(puppet.getOwnerUUID()) && tag.getInt(MARK) == puppet.subNonce();
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack seal, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof PuppetEntity puppet)) return InteractionResult.PASS;
		if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
		UUID bound = puppet(seal);
		Component said;
		if (bound == null || !bound.equals(puppet.getUUID())) {
			CompoundTag tag = seal.getTag();
			said = Component.translatable(ModLang.SUB_SEAL_OTHER, tag == null ? "?" : tag.getString(NAME)).withStyle(ChatFormatting.GRAY);
		} else if (!holds(seal, puppet)) {
			said = Component.translatable(ModLang.SUB_SEAL_VOID).withStyle(ChatFormatting.RED);
		} else if (puppet.isInert()) {
			said = Component.translatable(ModLang.COMMAND_INERT).withStyle(ChatFormatting.RED);
		} else {
			PuppetEntity.Order now = puppet.answerSubSeal(sp);
			said = Component.translatable(switch (now) {
				case GUARD -> ModLang.SUB_SEAL_GUARD;
				case HOLD -> ModLang.SUB_SEAL_HOLD;
				default -> ModLang.SUB_SEAL_FOLLOW;
			}, puppet.getDisplayName()).withStyle(ChatFormatting.AQUA);
			sp.level().playSound(null, puppet.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.2f);
		}
		sp.displayClientMessage(said, true);
		return InteractionResult.CONSUME;
	}

	@Override
	public Component getName(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(NAME) ? Component.translatable(ModLang.SUB_SEAL_NAME, tag.getString(NAME)) : super.getName(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.hasUUID(PUPPET)) {
			Tooltips.add(lines, Component.translatable(ModLang.SUB_SEAL_BLANK).withStyle(ChatFormatting.GRAY));
			return;
		}
		Tooltips.add(lines, Component.translatable(ModLang.SUB_SEAL_BOUND, tag.getString(NAME), tag.getString(OWNER_NAME)).withStyle(ChatFormatting.DARK_AQUA));
		Tooltips.add(lines, Component.translatable(ModLang.SUB_SEAL_USE).withStyle(ChatFormatting.GRAY));
	}
}
