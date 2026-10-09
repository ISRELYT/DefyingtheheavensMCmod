package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.RingContainer;
import com.example.defyingtheheavens.RingHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives every player a ring container, saves it with the player and drops it on death like the rest of the inventory.
 * Also keeps Qi Flight from cushioning falls.
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements RingHolder {
	@Unique
	private RingContainer dth$rings;

	@Override
	public RingContainer dth$getRings() {
		// Lazy: the Player constructor builds its InventoryMenu (which asks for the rings) partway through.
		if (dth$rings == null) dth$rings = new RingContainer();
		return dth$rings;
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void dth$saveRings(CompoundTag tag, CallbackInfo ci) {
		tag.put(RingContainer.NBT_KEY, dth$getRings().save());
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void dth$loadRings(CompoundTag tag, CallbackInfo ci) {
		dth$getRings().load(tag.getList(RingContainer.NBT_KEY, Tag.TAG_COMPOUND));
	}

	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void dth$dropRings(CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (self.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
		RingContainer rings = dth$getRings();
		for (int i = 0; i < rings.getContainerSize(); i++) {
			ItemStack stack = rings.removeItemNoUpdate(i);
			if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)) {
				self.drop(stack, true, false);
			}
		}
	}

	/**
	 * Vanilla spares anyone allowed to fly from fall damage. In survival and adventure that permission only ever comes from
	 * Qi Flight (it owns the flag there), and a cultivator who stops flying and drops should land as hard as anyone.
	 * Flying itself keeps resetting the fall distance, so flying down to land stays safe. Creative and spectator (the
	 * invulnerable modes) are unchanged.
	 */
	@Redirect(method = "causeFallDamage", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z", opcode = Opcodes.GETFIELD))
	private boolean dth$qiFlightTakesFallDamage(Abilities abilities) {
		return abilities.mayfly && abilities.invulnerable;
	}
}
