package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PuppetPlayers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A player's own soul in a puppet, as they see it (PuppetPlayers, puppet phase 2d; the user's word, 2026-11-18: "rather than commands they
 * should be buttons or things in menus"): where their soul is and how it stands (held, released, its puppeteer away, free), how far the
 * puppet's copy of them has come, their struggle; its buttons: Let go, Struggle and Its board (to look at, the command line they struggle
 * against lit) while another holds it, Leave it, Its board and their puppets once it is their own; and an offer standing for them, Accept or
 * Decline. Opened by itself as the soul comes into its puppet or an offer comes, then by the inventory key while held (ClientPuppetPilot);
 * its buttons sent as C2S puppet_soul_act, the page coming back up to date.
 */
public class PuppetSoulScreen extends Screen {
	private static final int W = 300, H = 150, BUTTON_H = 14, PAD = 10;

	/**
	 * How a soul stands (S2C puppet_soul_page): {@code state} 0 in its own body, 1 held, 2 released, 3 its puppeteer away, 4 free; its puppet,
	 * puppeteer, copy, struggle; an offer standing for it.
	 */
	public record Page(int state, @Nullable Component puppet, String master, int copied, boolean mayStruggle, boolean struggling, int struggleSeconds,
			@Nullable Component realm, boolean offer, String offerFrom, @Nullable Component offerPuppet, int offerSeconds) {
		boolean bound() { return state > 0; }
		boolean free() { return state == 4; }
		boolean playing() { return state >= 2; }
	}

	public static Page read(FriendlyByteBuf buf) {
		int state = buf.readVarInt();
		Component puppet = null, realm = null;
		String master = "";
		int copied = 0, seconds = 0;
		boolean may = false, struggling = false;
		if (state > 0) {
			puppet = buf.readComponent();
			master = buf.readUtf();
			copied = buf.readVarInt();
			may = buf.readBoolean();
			struggling = buf.readBoolean();
			seconds = buf.readVarInt();
			realm = buf.readComponent();
		}
		boolean offer = buf.readBoolean();
		String from = "";
		Component offered = null;
		int offerSeconds = 0;
		if (offer) {
			from = buf.readUtf();
			offered = buf.readComponent();
			offerSeconds = buf.readVarInt();
		}
		return new Page(state, puppet, master, copied, may, struggling, seconds, realm, offer, from, offered, offerSeconds);
	}

	private Page page;
	private int left, top, sureAct = -1, sureTicks;
	/** The screen to go back to as it closes (the cultivation menu, whose first tab opened it while the soul plays a puppet), or none. */
	private final @Nullable Screen parent;

	public PuppetSoulScreen(Page page, @Nullable Screen parent) {
		super(Component.translatable(ModLang.SOUL_SCREEN_TITLE));
		this.page = page;
		this.parent = parent;
	}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(parent);
	}

	/** The page come again (up to date). */
	public void page(Page page) {
		this.page = page;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		if (sureTicks > 0 && --sureTicks == 0) sureAct = -1;
		// Nothing left to show: its soul back in its own body and no offer standing.
		if (!page.bound() && !page.offer()) onClose();
	}

	private record Button(int act, Component label, boolean enabled, int x, int y, int w, @Nullable Component tip) {}

	private boolean sure(int act) {
		return sureAct == act && sureTicks > 0;
	}

	/** Pressed twice: letting go, leaving it, accepting an offer (each the end of something). */
	private static boolean twice(int act) {
		return act == PuppetPlayers.LET_GO || act == PuppetPlayers.LEAVE || act == PuppetPlayers.ACCEPT;
	}

	private Component label(int act, String key) {
		return Component.translatable(sure(act) ? ModLang.BOARD_SURE : key);
	}

	private List<Button> buttons() {
		List<Button> buttons = new ArrayList<>();
		int y = top + H - PAD - BUTTON_H, half = (W - 2 * PAD - 6) / 2, x = left + PAD;
		if (page.offer() && !page.bound()) {
			buttons.add(new Button(PuppetPlayers.ACCEPT, label(PuppetPlayers.ACCEPT, ModLang.PUPPET_SOUL_ACCEPT_BUTTON), true, x, y, half,
					Component.translatable(ModLang.PUPPET_SOUL_ACCEPT_TIP)));
			buttons.add(new Button(PuppetPlayers.DECLINE, Component.translatable(ModLang.SOUL_SCREEN_DECLINE), true, x + half + 6, y, half, null));
		} else if (page.free()) {
			int third = (W - 2 * PAD - 12) / 3;
			buttons.add(new Button(PuppetPlayers.LEAVE, label(PuppetPlayers.LEAVE, ModLang.PUPPET_SOUL_LEAVE_BUTTON), true, x, y, third,
					Component.translatable(ModLang.PUPPET_SOUL_LEAVE_TIP)));
			buttons.add(new Button(PuppetPlayers.BOARD, Component.translatable(ModLang.PUPPET_SOUL_BOARD_BUTTON), true, x + third + 6, y, third,
					Component.translatable(ModLang.PUPPET_SOUL_BOARD_TIP)));
			buttons.add(new Button(PuppetPlayers.PUPPETS, Component.translatable(ModLang.SOUL_SCREEN_PUPPETS), true, x + 2 * (third + 6), y, third,
					Component.translatable(ModLang.SOUL_SCREEN_PUPPETS_TIP)));
		} else if (page.bound()) {
			// Held in another's: let go, struggle, and its board to look at (the command line struggled against; the user, 2026-11-18: "a
			// non-free puppet should see circuits too so they can takeover/struggle free").
			int third = (W - 2 * PAD - 12) / 3;
			buttons.add(new Button(PuppetPlayers.LET_GO, label(PuppetPlayers.LET_GO, ModLang.PUPPET_SOUL_LET_GO_BUTTON), true, x, y, third,
					Component.translatable(ModLang.PUPPET_SOUL_LET_GO_TIP)));
			Component why = page.mayStruggle() ? Component.translatable(ModLang.PUPPET_SOUL_STRUGGLE_TIP)
					: Component.translatable(ModLang.PUPPET_SOUL_CANT_STRUGGLE, page.realm() == null ? Component.empty() : page.realm());
			buttons.add(new Button(PuppetPlayers.STRUGGLE, Component.translatable(page.struggling() ? ModLang.BOARD_STRUGGLE_ON : ModLang.PUPPET_SOUL_STRUGGLE_BUTTON),
					page.mayStruggle() && !page.struggling(), x + third + 6, y, third, why));
			buttons.add(new Button(PuppetPlayers.BOARD, Component.translatable(ModLang.PUPPET_SOUL_BOARD_BUTTON), true, x + 2 * (third + 6), y, third,
					Component.translatable(ModLang.PUPPET_SOUL_BOARD_CAPTIVE_TIP)));
		}
		return buttons;
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0) {
			for (Button b : buttons()) {
				if (!in(mx, my, b.x(), b.y(), b.w(), BUTTON_H) || !b.enabled()) continue;
				if (twice(b.act()) && !sure(b.act())) {
					sureAct = b.act();
					sureTicks = 60;
				} else {
					sureAct = -1;
					sureTicks = 0;
					send(b.act());
					if (b.act() == PuppetPlayers.BOARD || b.act() == PuppetPlayers.DECLINE || b.act() == PuppetPlayers.PUPPETS) onClose();
				}
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	/** {@code act} sent (C2S puppet_soul_act). */
	static void send(int act) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(act);
		ClientPlayNetworking.send(ModPackets.PUPPET_SOUL_ACT, buf);
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	/** What the screen tells: where the soul is, its copy, its struggle, how it plays the puppet; an offer. */
	private List<Component> lines() {
		List<Component> lines = new ArrayList<>();
		Component puppet = page.puppet() == null ? Component.translatable(ModLang.PUPPET_SOUL_A_PUPPET) : page.puppet();
		switch (page.state()) {
			case 1 -> lines.add(Component.translatable(ModLang.PUPPET_SOUL_HELD_IN, puppet).withStyle(s -> s.withColor(0xE0A0FF)));
			case 2 -> lines.add(Component.translatable(ModLang.SOUL_SCREEN_RELEASED, puppet, page.master()).withStyle(s -> s.withColor(0xE0A0FF)));
			case 3 -> lines.add(Component.translatable(ModLang.SOUL_SCREEN_AWAY, puppet, page.master()).withStyle(s -> s.withColor(0xE0A0FF)));
			case 4 -> lines.add(Component.translatable(ModLang.PUPPET_SOUL_FREE_IN, puppet).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
			default -> {
				if (!page.offer()) lines.add(Component.translatable(ModLang.SOUL_SCREEN_NONE).withStyle(s -> s.withColor(TalkUi.MUTED)));
			}
		}
		if (page.bound() && !page.free()) {
			lines.add(Component.translatable(ModLang.SOUL_SCREEN_COPIED, page.copied()).withStyle(s -> s.withColor(TalkUi.CREAM)));
			if (page.struggling()) lines.add(Component.translatable(ModLang.PUPPET_SOUL_STRUGGLING, page.struggleSeconds()).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
		}
		if (page.playing()) lines.add(Component.translatable(ModLang.SOUL_SCREEN_PLAYING).withStyle(s -> s.withColor(TalkUi.MUTED)));
		if (page.offer() && !page.bound()) {
			Component offered = page.offerPuppet() == null ? Component.translatable(ModLang.PUPPET_SOUL_A_PUPPET) : page.offerPuppet();
			lines.add(Component.translatable(ModLang.SOUL_SCREEN_OFFER, page.offerFrom(), offered, page.offerSeconds()).withStyle(s -> s.withColor(0xE0A0FF)));
		}
		return lines;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partialTick) {
		renderBackground(g);
		TalkUi.panel(g, left, top, left + W, top + H, TalkUi.GOLD);
		HallHud.heading(g, font, title, left + PAD, top + 6, W - 2 * PAD);
		int y = top + 24, bottom = top + H - PAD - BUTTON_H - 4;
		for (Component line : lines()) {
			for (FormattedCharSequence part : font.split(line, W - 2 * PAD)) {
				if (y + 9 > bottom) break;
				g.drawString(font, part, left + PAD, y, TalkUi.CREAM, false);
				y += 10;
			}
			y += 3;
		}
		Component tip = null;
		for (Button b : buttons()) {
			boolean hover = in(mx, my, b.x(), b.y(), b.w(), BUTTON_H);
			HallHud.button(g, font, b.x(), b.y(), b.w(), BUTTON_H, b.label(), b.enabled(), hover && b.enabled());
			if (hover && b.tip() != null) tip = b.tip();
		}
		super.render(g, mx, my, partialTick);
		if (tip != null) g.renderTooltip(font, font.split(tip, 220), mx, my);
	}
}
