package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.BaublePanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the bauble panel (client/BaublePanel) to the survival inventory: the toggle button, the panel itself, clicks on
 * the button, and treating the panel as part of the window so clicking in it doesn't drop the held item. The panel
 * closes while the recipe book is open, since the book takes that side of the screen.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
	@Shadow @Final private RecipeBookComponent recipeBookComponent;

	protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "renderBg", at = @At("TAIL"))
	private void dth$drawBaublePanel(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
		if (recipeBookComponent.isVisible()) {
			BaublePanel.close();
			return;
		}
		BaublePanel.renderButton(graphics, leftPos, topPos, mouseX, mouseY);
		if (BaublePanel.isOpen()) BaublePanel.renderPanel(graphics, leftPos, topPos);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void dth$buttonTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!recipeBookComponent.isVisible() && BaublePanel.isOverButton(leftPos, topPos, mouseX, mouseY)) {
			graphics.renderTooltip(font, BaublePanel.TITLE, mouseX, mouseY);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void dth$toggleBaublePanel(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (button == 0 && !recipeBookComponent.isVisible() && BaublePanel.isOverButton(leftPos, topPos, mouseX, mouseY)) {
			BaublePanel.toggle();
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void dth$panelIsInside(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
		if (BaublePanel.isOpen() && BaublePanel.isOverPanel(left, top, mouseX, mouseY)) cir.setReturnValue(false);
	}
}
