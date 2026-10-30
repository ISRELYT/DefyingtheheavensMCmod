package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.Spell;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The readied spell and the selected ability, beside the hotbar where the hand acts, as the mockups' "G" draws them (in the artifact's
 * pixels, HallArt; they were rows of the top-left stack before): a slot each, 66 across in a dark-gold frame over a see-through ink, its
 * icon 48 across, its word over it ("Spell", "Ability"), and the key that uses it in its corner as an item's count is. A spell cooling
 * shows dim, the seconds left across it; an ability that is on has its frame in jade and a green diamond in its corner. They stand 17
 * past the hotbar's right end (past the offhand slot and the attack indicator on that side when they are there), their foot 2 above
 * the screen's: the hotbar keeps the GUI scale's size, the slots the artifact's.
 */
public final class HotbarSlots {
	private static final int SIZE = 66, BORDER = 3, ICON = 48, STEP = 72;
	private static final int ON_FRAME = 0xFF3E8C72, ON_LAMP = 0xFF69F0AE, INK = 0x8C0B0E12, WORD = 0xE6D3A3;

	public static void register() {
		HudRenderCallback.EVENT.register((g, tickDelta) -> render(g));
	}

	private static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
		Spell spell = ClientMethods.state().readiedSpell();
		if (spell != null && !ClientMethods.known(spell)) spell = null;
		Ability ability = QuickAbilities.selected(ClientCultivationData.get());
		if (spell == null && ability == null) return;
		// Right of the hotbar (in the GUI's pixels, then the artifact's), past the offhand slot (a left-handed player's) and the attack
		// indicator (drawn there in Hotbar mode).
		float gui = g.guiWidth() / 2f + 91;
		boolean leftHanded = mc.player.getMainArm() == HumanoidArm.LEFT;
		if (leftHanded && !mc.player.getOffhandItem().isEmpty()) gui += 29;
		if (!leftHanded && mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) gui += 22;
		HallArt.beginHud(g);
		try {
			float x = gui / HallArt.hudScale() + 17, y = HallArt.height() - 2 - SIZE;
			if (spell != null) {
				int cooldown = ClientMethods.cooldownLeft(spell);
				slot(g, x, y, Component.translatable(ModLang.HUD_SLOT_SPELL).getString(), HallArt.FRAME);
				SpellWheelScreen.drawIcon(g, spell, Math.round(x + BORDER + 6), Math.round(y + BORDER + 6), ICON, cooldown <= 0);
				if (cooldown > 0) HallArt.textCentred(g, String.valueOf((cooldown + 19) / 20), x + SIZE / 2f, y + 22, 20, 22, 0xD0D0D0, true);
				key(g, x, y, ModKeybinds.CAST_SPELL);
				x += STEP;
			}
			if (ability != null) {
				boolean on = ClientCultivationData.get().isAbilityEnabled(ability);
				slot(g, x, y, Component.translatable(ModLang.HUD_SLOT_ABILITY).getString(), on ? ON_FRAME : HallArt.FRAME);
				AbilityWheel.drawIcon(g, ability, Math.round(x + BORDER + 6), Math.round(y + BORDER + 6), ICON, on);
				if (on) HallArt.diamond(g, x + BORDER + 47, y + BORDER + 5, 9, ON_LAMP);
				key(g, x, y, ModKeybinds.QUICK_ABILITY);
			}
		} finally {
			HallArt.end(g);
		}
	}

	/** A slot's frame and ink, and its word over it. */
	private static void slot(GuiGraphics g, float x, float y, String word, int frame) {
		HallArt.box(g, x, y, SIZE, SIZE, BORDER, frame, INK);
		HallArt.textCentred(g, word, x + SIZE / 2f, y - 24, 15, 22, WORD, true);
	}

	/** The key that uses a slot, in its lower right (a long key's name left out). */
	private static void key(GuiGraphics g, float x, float y, KeyMapping key) {
		String name = key.getTranslatedKeyMessage().getString();
		if (name.isEmpty() || name.length() > 3) return;
		HallArt.textCentred(g, name, x + BORDER + 48, y + BORDER + 36, 20, 22, HallArt.WHITE, true);
	}

	private HotbarSlots() {}
}
