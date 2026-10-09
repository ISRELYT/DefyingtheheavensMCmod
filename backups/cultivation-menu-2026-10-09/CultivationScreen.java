package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CultivationStats;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.RingOfPowerItem;
import com.example.defyingtheheavens.Stage;
import com.example.defyingtheheavens.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

public class CultivationScreen extends Screen {
	private static final int WIDTH = 260;
	private static final int HEIGHT = 250;

	private int left;
	private int top;
	private Button meditateButton;
	private Button breakthroughButton;

	public CultivationScreen() {
		super(Component.translatable(ModLang.TITLE));
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;

		meditateButton = addRenderableWidget(Button.builder(Component.empty(),
						b -> ClientPlayNetworking.send(ModPackets.TOGGLE_MEDITATION, PacketByteBufs.create()))
				.bounds(left + 12, top + HEIGHT - 32, 112, 20).build());

		breakthroughButton = addRenderableWidget(Button.builder(Component.translatable(ModLang.BTN_BREAKTHROUGH),
						b -> ClientPlayNetworking.send(ModPackets.BREAKTHROUGH, PacketByteBufs.create()))
				.bounds(left + WIDTH - 124, top + HEIGHT - 32, 112, 20).build());

		updateButtons();
	}

	@Override
	public void tick() {
		updateButtons();
	}

	private void updateButtons() {
		if (meditateButton == null) return;
		meditateButton.setMessage(Component.translatable(
				ClientCultivationData.isMeditating() ? ModLang.BTN_STOP : ModLang.BTN_MEDITATE));
		breakthroughButton.active = ClientCultivationData.get().canBreakthrough() && !ClientTribulationData.isActive();
	}

	@Override
	public boolean isPauseScreen() {
		return false; // meditation keeps running while the menu is open
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (ModKeybinds.OPEN_MENU.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);

		g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, 0xFFB8860B);
		g.fill(left, top, left + WIDTH, top + HEIGHT, 0xF0101018);

		PlayerCultivation c = ClientCultivationData.get();
		Player player = minecraft == null ? null : minecraft.player;
		Realm realm = c.getRealm();
		Stage stage = c.getStage();
		Realm statRealm = c.getEffectiveRealm(); // stats follow the (possibly suppressed) effective stage
		Stage statStage = c.getEffectiveStage();
		int cx = left + WIDTH / 2;
		int x = left + 14;
		int y = top + 10;

		g.drawCenteredString(font, title, cx, y, 0xFFD700);
		y += 16;
		g.drawCenteredString(font, Component.translatable(ModLang.REALM, realm.getDisplayName()), cx, y, 0x7FDBFF);
		y += 12;
		g.drawCenteredString(font, Component.translatable(ModLang.STAGE, stage.getDisplayName()), cx, y, 0xB39DDB);
		y += 16;

		// Qi bar
		int barW = WIDTH - 28;
		double required = c.cultivationRequired();
		double ratio = required <= 0 ? 0 : Math.min(1.0, c.getCultivation() / required);
		boolean bottleneck = c.isAtBottleneck();
		g.fill(x - 1, y - 1, x + barW + 1, y + 11, 0xFF000000);
		g.fill(x, y, x + barW, y + 10, 0xFF2A2A3A);
		g.fill(x, y, x + (int) (barW * ratio), y + 10, bottleneck ? 0xFFFFC107 : 0xFF4DD0E1);
		y += 14;

		Component qiLine = c.isMaxed()
				? Component.translatable(ModLang.CULTIVATION_MAX, num(c.getCultivation()))
				: Component.translatable(ModLang.CULTIVATION, num(c.getCultivation()), num(required));
		g.drawCenteredString(font, qiLine, cx, y, 0xFFFFFF);
		y += 11;
		double qiRate = c.meditationCultivationPerSecond(RingOfPowerItem.cultivationBonus(player)); // 0 while suppressed
		g.drawCenteredString(font, c.isInUpperRealm()
				? Component.translatable(ModLang.CULTIVATION_RATE_UPPER, one(qiRate), (int) PlayerCultivation.UPPER_REALM_QI_MULTIPLIER)
				: Component.translatable(ModLang.CULTIVATION_RATE, one(qiRate)), cx, y, 0xA0A0A0);
		y += 14;

		if (c.isSuppressed()) {
			g.drawCenteredString(font, Component.translatable(ModLang.SUPPRESSED), cx, y, 0xFF6E6E);
			g.drawCenteredString(font, Component.translatable(ModLang.SUPPRESSED_TO,
					PlayerCultivation.rankName(c.getEffectiveRealm(), c.getEffectiveStage())), cx, y + 11, 0xFF6E6E);
		} else if (c.isMaxed()) {
			g.drawCenteredString(font, Component.translatable(ModLang.PINNACLE), cx, y, 0xFFD700);
		} else if (c.isAtBottleneck() && c.isBreakthroughLocked()) {
			g.drawCenteredString(font, Component.translatable(ModLang.BREAKTHROUGH_SEALED), cx, y, 0xFF6E6E);
		} else if (c.canBreakthrough()) {
			g.drawCenteredString(font, Component.translatable(ModLang.BOTTLENECK), cx, y, 0xFFC107);
			g.drawCenteredString(font, Component.translatable(ModLang.BOTTLENECK_NEXT,
					PlayerCultivation.rankName(c.breakthroughRealm(), c.breakthroughStage())), cx, y + 11, 0xFFC107);
		}
		y += 25;

		// Stats
		g.drawString(font, Component.translatable(ModLang.STATS_HEADER), x, y, 0xFFD700);
		y += 12;
		double health = CultivationStats.maxHealth(statRealm, statStage);
		stat(g, ModLang.STAT_HEALTH, x, y, one(attr(player, Attributes.MAX_HEALTH, health)), "+" + one(health));
		y += 11;
		double damage = CultivationStats.attackDamage(statRealm, statStage);
		stat(g, ModLang.STAT_DAMAGE, x, y, one(attr(player, Attributes.ATTACK_DAMAGE, damage)), "+" + one(damage));
		y += 11;
		// Movement speed is always synced, so it never needs the bonus estimate.
		stat(g, ModLang.STAT_SPEED, x, y, one(attr(player, Attributes.MOVEMENT_SPEED, 0) * 1000) + "%", "+" + one(CultivationStats.moveSpeed(statRealm, statStage) * 100) + "%");
		y += 11;
		double armor = CultivationStats.armor(statRealm, statStage);
		stat(g, ModLang.STAT_ARMOR, x, y, one(attr(player, Attributes.ARMOR, armor)), "+" + one(armor));
		y += 11;
		double toughness = CultivationStats.toughness(statRealm, statStage);
		stat(g, ModLang.STAT_TOUGHNESS, x, y, one(attr(player, Attributes.ARMOR_TOUGHNESS, toughness)), "+" + one(toughness));
		y += 11;
		double knockback = CultivationStats.knockbackResistance(statRealm, statStage);
		stat(g, ModLang.STAT_KNOCKBACK, x, y, one(attr(player, Attributes.KNOCKBACK_RESISTANCE, knockback) * 100) + "%", "+" + one(knockback * 100) + "%");
		y += 14;

		boolean meditating = ClientCultivationData.isMeditating();
		g.drawString(font, Component.translatable(meditating ? ModLang.STATUS_MEDITATING : ModLang.STATUS_IDLE), x, y,
				meditating ? 0x69F0AE : 0x9E9E9E);

		super.render(g, mouseX, mouseY, partialTick);
	}

	private void stat(GuiGraphics g, String key, int x, int y, String total, String bonus) {
		g.drawString(font, Component.translatable(key, total, bonus), x, y, 0xE0E0E0);
	}

	/**
	 * The player's current value of an attribute. The server only sends some attributes to clients (max health, speed,
	 * armor, toughness); the client's copy of the others (attack damage, knockback resistance) never changes, so for those
	 * show the base value plus the cultivation bonus (bare-handed, without gear or effects).
	 */
	private static double attr(Player player, Attribute attribute, double cultivationBonus) {
		if (player == null) return 0;
		if (attribute.isClientSyncable()) return player.getAttributeValue(attribute);
		return attribute.sanitizeValue(player.getAttributeBaseValue(attribute) + cultivationBonus);
	}

	private static String one(double v) {
		return String.format(Locale.ROOT, "%.1f", v);
	}

	private static String num(double v) {
		return String.format(Locale.ROOT, "%,.0f", v);
	}
}
