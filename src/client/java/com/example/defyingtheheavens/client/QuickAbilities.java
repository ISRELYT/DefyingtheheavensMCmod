package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PlayerCultivation;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Switching an ability by key, without pointing at the wheel. An ability is selected on the wheel (hold Z, click it; saved
 * with the cultivation, shown under the qi bar) and the Use Selected Ability key (X) switches it on or off, or carries out an act
 * such as tearing space; the eight Ability Wheel Slot keys (unbound until bound in Controls) each switch their slot's, and the
 * number keys do the same while the wheel is held. The server flips it, as from the wheel or the menu; once its answer comes back,
 * "Qi Flight: On" shows above the hotbar (an act, which switches nothing, says its own word instead).
 */
public final class QuickAbilities {
	/** The ability last switched by key and how it stood then, watched until the server's answer shows it changed. */
	private static Ability pending;
	private static boolean pendingWas;
	private static int pendingTicks;

	/** The selected ability, if it is one the cultivator can use now (awakened, not held back by a lower realm's suppression). */
	static Ability selected(PlayerCultivation c) {
		Ability ability = c.getSelectedAbility();
		return ability != null && ability.isUnlocked(c) ? ability : null;
	}

	/** Selects {@code ability} for the Use Selected Ability key (the server keeps it). */
	static void select(Ability ability) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(ability == null ? -1 : ability.ordinal());
		ClientPlayNetworking.send(ModPackets.SELECT_ABILITY, buf);
		ClientCultivationData.get().setSelectedAbility(ability); // shown at once; the server's sync confirms it
	}

	/** The Use Selected Ability key: switches the selected ability, or says how to select one. */
	static void useSelected() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		PlayerCultivation c = ClientCultivationData.get();
		if (!AbilityWheelScreen.canOpen(c)) {
			mc.gui.setOverlayMessage(Component.translatable(ModLang.ABILITIES_EMPTY), false);
			return;
		}
		Ability ability = selected(c);
		if (ability == null) {
			mc.gui.setOverlayMessage(c.getSelectedAbility() == null ? Component.translatable(ModLang.MSG_SELECTED_ABILITY_NONE,
					ModKeybinds.ABILITY_WHEEL.getTranslatedKeyMessage()) : Component.translatable(ModLang.MSG_SELECTED_ABILITY_ASLEEP,
					c.getSelectedAbility().getDisplayName()), false);
			return;
		}
		switchNow(ability);
	}

	/** Switches the ability in wheel slot {@code slot} (0: the top, clockwise), or says the slot is empty. */
	static void toggle(int slot) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		PlayerCultivation c = ClientCultivationData.get();
		if (!AbilityWheelScreen.canOpen(c)) {
			mc.gui.setOverlayMessage(Component.translatable(ModLang.ABILITIES_EMPTY), false);
			return;
		}
		Ability ability = AbilityWheel.shown(c, slot);
		if (ability == null) {
			mc.gui.setOverlayMessage(Component.translatable(ModLang.MSG_QUICK_ABILITY_EMPTY, slot + 1), false);
			return;
		}
		switchNow(ability);
	}

	/** Asks the server to switch {@code ability}, and watches for its answer. */
	static void switchNow(Ability ability) {
		Minecraft mc = Minecraft.getInstance();
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(ability.ordinal());
		ClientPlayNetworking.send(ModPackets.TOGGLE_ABILITY, buf);
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		pending = ability;
		pendingWas = ClientCultivationData.get().isAbilityEnabled(ability);
		pendingTicks = 40;
	}

	/** Each client tick: the keys pressed, and the switched ability's new state named once the server has answered. */
	static void tick(Minecraft client) {
		while (ModKeybinds.QUICK_ABILITY.consumeClick()) {
			if (client.screen == null) useSelected();
		}
		for (int slot = 0; slot < ModKeybinds.ABILITY_SLOTS.length; slot++) {
			while (ModKeybinds.ABILITY_SLOTS[slot].consumeClick()) {
				if (client.screen == null) toggle(slot);
			}
		}
		if (pending == null) return;
		boolean now = ClientCultivationData.get().isAbilityEnabled(pending);
		if (now != pendingWas) {
			client.gui.setOverlayMessage(Component.translatable(ModLang.MSG_QUICK_ABILITY_STATE, pending.getDisplayName(),
					Component.translatable(now ? ModLang.ABILITY_ON : ModLang.ABILITY_OFF)), false);
			pending = null;
		} else if (--pendingTicks <= 0) {
			pending = null; // refused, or an act that switches nothing (it has its own word)
		}
	}

	/** Disconnected: nothing left waiting. */
	static void clear() {
		pending = null;
	}

	private QuickAbilities() {}
}
