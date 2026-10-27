package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The titles players take (the user's ask, 2026-10-27: "From void shattering onwards, I want players to be able to name themselves or give
 * themselves a title. It would look like how the current sect titles look (you can have both at once without them overlapping). NPCs should
 * henceforth refer to them by their title"), and the plates over players' heads.
 * <ul>
 *   <li><b>Taking one.</b> From Void Shattering on (the higher of one's paths, {@link #FROM_RANK}), on the Cultivation tab: up to
 *   {@link #MAX_LENGTH} characters (the user's "like 50"), Enter to set it, emptied to clear it ({@link #set}). Kept with one's own record
 *   (PlayerCultivation#epithet); below Void Shattering it is kept but neither shown nor used.</li>
 *   <li><b>Shown.</b> Over a player's head, above their name, in gold, and their sect's title above that in the sect titles' own way
 *   ([Elder], in its colour), so both may stand at once: each player's plate sent to everyone as it changes ({@link #tick}).</li>
 *   <li><b>Used.</b> Cultivators call one by it (NpcAddress), never by a rank one doesn't hold.</li>
 * </ul>
 */
public final class PlayerTitles {
	/** The longest a title may be (characters). */
	public static final int MAX_LENGTH = 50;
	/** From Void Shattering on one may take a title. */
	public static final int FROM_RANK = PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY);

	/** {@code raw} as a title may be: no colour codes nor control characters, its spaces single, trimmed, at most {@link #MAX_LENGTH} long. */
	public static String clean(String raw) {
		if (raw == null || raw.isEmpty()) return "";
		StringBuilder out = new StringBuilder();
		boolean space = false;
		for (int i = 0; i < raw.length(); ) {
			int cp = raw.codePointAt(i);
			i += Character.charCount(cp);
			if (cp == '§') { // a formatting code: it and the character after it
				if (i < raw.length()) i += Character.charCount(raw.codePointAt(i));
				continue;
			}
			// A tab or a line break is a space; the other control characters are dropped.
			if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
				space = out.length() > 0;
				continue;
			}
			if (Character.isISOControl(cp)) continue;
			if (space) out.append(' ');
			space = false;
			out.appendCodePoint(cp);
		}
		String clean = out.toString();
		if (clean.codePointCount(0, clean.length()) > MAX_LENGTH) clean = clean.substring(0, clean.offsetByCodePoints(0, MAX_LENGTH)).trim();
		return clean;
	}

	/** Whether {@code c} may take (and go by) a title: one of the living's paths at Void Shattering or past it. */
	public static boolean mayTake(PlayerCultivation c) {
		return c != null && !c.isMortal() && TwoPaths.higherRank(c, false) >= FROM_RANK;
	}

	/** The title {@code p} goes by now (their own, from Void Shattering on), or "" for none. */
	public static String shown(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.own(p);
		return mayTake(c) ? c.epithet() : "";
	}

	/** C2S set_title: {@code p}'s title, cleaned, taken (or, empty, cleared); they are told, and everyone sees it. */
	public static void set(ServerPlayer p, String raw) {
		PlayerCultivation c = CultivationManager.own(p);
		if (!mayTake(c)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_TITLE_LOCKED).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		String title = clean(raw);
		if (title.equals(c.epithet())) return;
		c.setEpithet(title);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		if (title.isEmpty()) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_TITLE_CLEARED).withStyle(ChatFormatting.GRAY));
		} else {
			announce(p, c, title);
			p.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.2f);
		}
		tell(p.server, p.getUUID(), plateOf(p));
	}

	/** A title taken is told to everyone at most this often for each player (ticks: thirty seconds); sooner, only they are told. */
	static final long ANNOUNCE_EVERY = 600;
	/** When each player's title was last told to everyone (game time). */
	private static final Map<UUID, Long> ANNOUNCED = new HashMap<>();

	/**
	 * A title taken, told to everyone on the server as an advancement is (the user's ask, 2026-10-27: "when a player gives themselves a title
	 * it's announced server-wide so everyone can see it. Like an advancement would"): "Name has taken the title [title]", the title in gold
	 * and, over it, the realm it was taken at. Taken again within {@link #ANNOUNCE_EVERY} (a few tries at a name), only they are told.
	 */
	private static void announce(ServerPlayer p, PlayerCultivation c, String title) {
		int rank = TwoPaths.higherRank(c, false);
		Component realm = PlayerCultivation.rankName(Realm.byIndex(rank / Stage.values().length), Stage.byIndex(rank % Stage.values().length));
		Component titled = ComponentUtils.wrapInSquareBrackets(Component.literal(title))
				.withStyle(s -> s.withColor(ChatFormatting.GOLD).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, realm)));
		long now = p.server.overworld().getGameTime();
		Long last = ANNOUNCED.get(p.getUUID());
		if (last != null && now - last >= 0 && now - last < ANNOUNCE_EVERY) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_TITLE_SET, titled).withStyle(ChatFormatting.YELLOW));
			return;
		}
		ANNOUNCED.put(p.getUUID(), now);
		p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_TITLE_ANNOUNCED, p.getDisplayName(), titled), false);
	}

	// --- The plates over players' heads ---

	/** A player's plate: their title ("" none) and their sect's title over them (an NpcTitle's ordinal, -1 none; how its sect names it). */
	public record Plate(String title, int sectTitle, boolean clan, boolean holy, boolean immortal) {
		static final Plate NONE = new Plate("", -1, false, false, false);
	}

	/** What everyone was last told of each player's plate. */
	private static final Map<UUID, Plate> TOLD = new HashMap<>();

	/** {@code p}'s plate as it stands: their title, and their own sect's title for them (by strength among its members). */
	public static Plate plateOf(ServerPlayer p) {
		Sect sect = SectMembership.sectOf(p.server, p.getUUID());
		NpcTitle title = sect == null || sect.extinct ? null : SectMembership.titleOf(sect, p);
		return new Plate(shown(p), title == null ? -1 : title.ordinal(), sect != null && sect.isClan(), sect != null && sect.isHoly(),
				sect != null && sect.isImmortal());
	}

	/** Each second: everyone told of each plate that has changed (a title taken, a realm reached, a sect joined or a title won in it). */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 3) return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Plate now = plateOf(p);
			if (!now.equals(TOLD.get(p.getUUID()))) tell(server, p.getUUID(), now);
		}
	}

	/** Joining: told every plate there is (theirs is told to everyone at the next second). */
	public static void onJoin(ServerPlayer p) {
		TOLD.remove(p.getUUID());
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(TOLD.size());
		TOLD.forEach((id, plate) -> write(buf, id, plate));
		ServerPlayNetworking.send(p, ModPackets.PLAYER_PLATES, buf);
	}

	/** Leaving: their plate gone from everyone's view. */
	public static void onDisconnect(ServerPlayer p) {
		if (TOLD.remove(p.getUUID()) == null) return;
		for (ServerPlayer other : p.server.getPlayerList().getPlayers()) {
			if (other == p) continue;
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeVarInt(1);
			write(buf, p.getUUID(), Plate.NONE);
			ServerPlayNetworking.send(other, ModPackets.PLAYER_PLATES, buf);
		}
	}

	/** The server stopped: nothing kept. */
	public static void clear() {
		TOLD.clear();
		ANNOUNCED.clear();
	}

	private static void tell(MinecraftServer server, UUID id, Plate plate) {
		TOLD.put(id, plate);
		for (ServerPlayer other : server.getPlayerList().getPlayers()) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeVarInt(1);
			write(buf, id, plate);
			ServerPlayNetworking.send(other, ModPackets.PLAYER_PLATES, buf);
		}
	}

	private static void write(FriendlyByteBuf buf, UUID id, Plate plate) {
		buf.writeUUID(id);
		buf.writeUtf(plate.title(), MAX_LENGTH * 4);
		buf.writeVarInt(plate.sectTitle());
		buf.writeBoolean(plate.clan());
		buf.writeBoolean(plate.holy());
		buf.writeBoolean(plate.immortal());
	}

	private PlayerTitles() {}
}
