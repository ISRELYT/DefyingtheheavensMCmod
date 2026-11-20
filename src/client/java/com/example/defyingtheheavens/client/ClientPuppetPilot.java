package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PuppetPlayers;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A player's soul in a puppet, as its own client has it (PuppetPlayers, puppet phase 2d).
 * <ul>
 *   <li>Playing it (S2C puppet_pilot): it steers what it rides as a soul steers a body it possesses (SoulProjection#possessorOf asks
 *   PuppetPlayers#isPilot), and the cast key looses the puppet's art where it looks (C2S puppet_pilot_art), taken before the cast key's own
 *   use. Everything else as the living do (the user, 2026-11-18: "as a free puppet you should be able to live like normal ... moving the art
 *   button to what it normally is (c?) and having right click just be interact"): the use key uses, the inventory key opens its pack.</li>
 *   <li>Its soul's screen (S2C puppet_soul_page; the user's word, 2026-11-18, "rather than commands they should be buttons or things in
 *   menus"): opened when the server says (as the soul comes into its puppet, an offer comes), once no other screen is up; then, while it is
 *   held (watching, a spectator with no pack to open), by the inventory key. The cultivation menu's first tab stands in the Cultivation
 *   tab's place (a puppet never cultivates: the user's ask, "having the circuit board menu replace the cultivation tab"): the puppet's board
 *   itself, a free soul's own body's to paint, a held one's captor's to look at and struggle against (the user, 2026-11-18: "a non-free
 *   puppet should see circuits too so they can takeover/struggle free"). A hint of them over the hotbar meanwhile.</li>
 * </ul>
 */
public final class ClientPuppetPilot {
	/** Whether this client's player plays a puppet now. */
	private static boolean piloting;
	/** How its soul stands, as last told (null: never told, or back in its own body with no offer). */
	private static PuppetSoulScreen.Page page;
	/** Its screen to be opened once nothing else is up (the server asked for it: a respawn's screens, a menu, may be up). */
	private static boolean pendingOpen;
	private static long pendingSince;

	/** Whether this client's player plays a puppet now (its hands are the puppet's: ItemInHandRendererMixin). */
	public static boolean piloting() {
		return piloting;
	}

	/** Whether this client's player's soul is in a puppet (held or playing it). */
	public static boolean inPuppet() {
		return page != null && page.bound();
	}

	/** Whether the puppet its soul is in is its own body (a free soul). */
	public static boolean free() {
		return page != null && page.free();
	}

	/**
	 * The name of the cultivation menu's first tab while its soul is in a puppet (CultivationScreen): "Circuits", its own body's or its
	 * captor's (the user, 2026-11-18: "a non-free puppet should see circuits too so they can takeover/struggle free"); null otherwise (the
	 * Cultivation tab).
	 */
	public static @Nullable Component firstTab() {
		return inPuppet() ? Component.translatable(ModLang.TAB_CIRCUITS) : null;
	}

	/**
	 * The cultivation menu's first tab chosen while its soul is in a puppet: the puppet's board (asked of the server, opened over the menu and
	 * closing back to it: PuppetBoardScreen#receive), a free soul's to paint, a held one's to look at and struggle against.
	 */
	public static void openFirstTab(Screen menu) {
		if (inPuppet()) PuppetSoulScreen.send(PuppetPlayers.BOARD);
	}

	/** Whether {@code entity} is the puppet this client's player plays, looked out of in first person: not drawn (it would wall in the view). */
	public static boolean seenFromWithin(net.minecraft.world.entity.Entity entity) {
		Minecraft mc = Minecraft.getInstance();
		return piloting && mc.player != null && entity == mc.player.getVehicle() && mc.options.getCameraType().isFirstPerson();
	}

	public static void register() {
		PuppetPlayers.clientPilot = player -> piloting && player == Minecraft.getInstance().player;
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.PUPPET_PILOT, (client, handler, buf, sender) -> {
			boolean on = buf.readBoolean();
			client.execute(() -> piloting = on);
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.PUPPET_SOUL_PAGE, (client, handler, buf, sender) -> {
			boolean open = buf.readBoolean();
			PuppetSoulScreen.Page next = PuppetSoulScreen.read(buf);
			client.execute(() -> {
				page = next.bound() || next.offer() ? next : null;
				if (client.screen instanceof PuppetSoulScreen screen) {
					if (page == null) screen.onClose();
					else screen.page(page);
				} else if (open && page != null) {
					pendingOpen = true;
					pendingSince = System.currentTimeMillis();
				}
			});
		});
		ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			if (mc.player == null) return;
			if (pendingOpen && page != null && mc.screen == null && mc.player.isAlive()) {
				pendingOpen = false;
				if (System.currentTimeMillis() - pendingSince < 60_000) mc.setScreen(new PuppetSoulScreen(page, null));
			}
			if (mc.screen != null) return;
			// Held in a puppet, watching (a spectator, no pack to open): the inventory key opens its soul's screen (asking the server for it
			// up to date). Playing it, the key opens its pack as ever.
			if (page != null && page.bound() && !piloting) {
				boolean asked = false;
				while (mc.options.keyInventory.consumeClick()) asked = true;
				if (asked) {
					mc.setScreen(new PuppetSoulScreen(page, null));
					PuppetSoulScreen.send(PuppetPlayers.OPEN);
				}
			}
			if (!piloting) return;
			// Playing it: the cast key looses its art (its own cultivation lies dormant: no art of its own to cast meanwhile).
			boolean art = false;
			while (ModKeybinds.CAST_SPELL.consumeClick()) art = true;
			if (art) ClientPlayNetworking.send(ModPackets.PUPPET_PILOT_ART, PacketByteBufs.create());
		});
		// A hint over the hotbar while its soul is in a puppet (never over the boss bars): held, its inventory key opens the soul's screen;
		// playing it, its health, its art's key and the menu that holds its circuits (or its soul's screen).
		HudRenderCallback.EVENT.register((graphics, delta) -> {
			Minecraft mc = Minecraft.getInstance();
			if (page == null || !page.bound() || mc.screen != null || mc.options.hideGui || mc.player == null) return;
			Component hint;
			if (piloting && mc.player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity body) {
				// Its own health first (the player's hearts are a soul's, untouched in it).
				hint = Component.translatable(ModLang.SOUL_SCREEN_HEALTH, (int) Math.ceil(body.getHealth()), (int) Math.ceil(body.getMaxHealth()))
						.append("  ").append(Component.translatable(page.free() ? ModLang.SOUL_HINT_FREE : ModLang.SOUL_HINT_PLAYING,
								ModKeybinds.CAST_SPELL.getTranslatedKeyMessage(), ModKeybinds.OPEN_MENU.getTranslatedKeyMessage()));
			} else {
				hint = Component.translatable(ModLang.SOUL_SCREEN_HINT, mc.options.keyInventory.getTranslatedKeyMessage(),
						ModKeybinds.OPEN_MENU.getTranslatedKeyMessage());
			}
			int w = mc.font.width(hint);
			graphics.drawString(mc.font, hint, (graphics.guiWidth() - w) / 2, graphics.guiHeight() - 92, 0xE0A0FF, true);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			piloting = false;
			page = null;
			pendingOpen = false;
		});
	}

	private ClientPuppetPilot() {}
}
