package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CelestialCourt;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.NpcTitle;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The local player's sect and their title in it (S2C sect_status, SectMembership#sendStatus), for the Stats tab; and the Sect
 * tab's page (S2C sect_info, SectMembership#sendInfo), asked for as the tab opens.
 */
public final class ClientSect {
	private static String name = "";
	private static NpcTitle title;
	/** The Sect tab's page, or null until it has come. */
	private static Info info;

	/**
	 * One of a sect's members as the Sect tab lists it: a title (null: an absent player's), a rank (-1: not known), a player or not; its
	 * id, and whether the one viewing may cast it out (seated as its master or a senior elder, in such a one's body: SectMembership#banish).
	 */
	public record Member(String name, NpcTitle title, int rank, boolean player, UUID id, boolean banishable) {}

	/**
	 * A disciple's own sect (the first fields), or the sect of the body their soul is in ({@code inBody}: it may be left from here), or,
	 * for one of no sect, the nearest one ({@code nearest...}; empty name: none). {@code clan}: a demon clan's, its master its Clan Head;
	 * {@code holy}: a Holy Land's (the Great Void's sects), its master its Patriarch; {@code immortal}: an immortal sect's (the Immortal
	 * Realm's), its master its Sect Lord. Whatever their sect, their rank in the Celestial Court ({@code court}: null if they aren't of it)
	 * and the merit they have earned there. A disciple's Contribution in their sect (its mission board: SectMissions), all they have earned of
	 * it, and the missions they hold ({@code missions}).
	 */
	public record Info(boolean member, String sectName, int alignment, NpcTitle title, int standing, boolean onGrounds, int distance,
					   String direction, boolean barrierUp, int treasuryTries, boolean elder, long ticksMember, boolean inBody, List<Member> members,
					   int nearestDistance, int barredDays, boolean clan, boolean holy, boolean immortal, CelestialCourt.Rank court, int merit,
					   int contribution, int earned, int missions) {}

	static void set(String sectName, int titleIndex) {
		name = sectName == null ? "" : sectName;
		title = titleIndex < 0 ? null : NpcTitle.byIndex(titleIndex);
	}

	/** Reads S2C sect_info (on the network thread; the result is set on the client's). */
	static Info read(FriendlyByteBuf buf) {
		if (buf.readBoolean()) {
			String sectName = buf.readUtf();
			int alignment = buf.readInt();
			NpcTitle mine = NpcTitle.byIndex(buf.readVarInt());
			int standing = buf.readInt();
			boolean onGrounds = buf.readBoolean();
			int distance = buf.readVarInt();
			String direction = buf.readUtf();
			boolean barrierUp = buf.readBoolean();
			int tries = buf.readVarInt();
			boolean elder = buf.readBoolean();
			long ticks = buf.readVarLong();
			boolean inBody = buf.readBoolean();
			boolean clan = buf.readBoolean();
			boolean holy = buf.readBoolean();
			boolean immortal = buf.readBoolean();
			int count = buf.readVarInt();
			List<Member> members = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				String memberName = buf.readUtf();
				int t = buf.readVarInt() - 1;
				int rank = buf.readVarInt() - 1;
				boolean player = buf.readBoolean();
				UUID id = buf.readUUID();
				boolean banishable = buf.readBoolean();
				members.add(new Member(memberName, t < 0 ? null : NpcTitle.byIndex(t), rank, player, id, banishable));
			}
			int contribution = buf.readVarInt(), earned = buf.readVarInt(), missions = buf.readVarInt();
			CelestialCourt.Rank court = CelestialCourt.Rank.byIndex(buf.readVarInt() - 1);
			int merit = buf.readVarInt();
			return new Info(true, sectName, alignment, mine, standing, onGrounds, distance, direction, barrierUp, tries, elder, ticks, inBody, members, 0, 0, clan, holy,
					immortal, court, merit, contribution, earned, missions);
		}
		if (!buf.readBoolean()) {
			CelestialCourt.Rank court = CelestialCourt.Rank.byIndex(buf.readVarInt() - 1);
			int merit = buf.readVarInt();
			return new Info(false, "", 0, null, 0, false, -1, "", false, 0, false, 0, false, List.of(), -1, 0, false, false, false, court, merit, 0, 0, 0);
		}
		String near = buf.readUtf();
		int alignment = buf.readInt();
		int distance = buf.readVarInt();
		String direction = buf.readUtf();
		int barred = buf.readVarInt();
		CelestialCourt.Rank court = CelestialCourt.Rank.byIndex(buf.readVarInt() - 1);
		int merit = buf.readVarInt();
		return new Info(false, near, alignment, null, 0, false, distance, direction, false, 0, false, 0, false, List.of(), distance, barred, false, false, false,
				court, merit, 0, 0, 0);
	}

	/** The Sect tab's Leave button (in a member's body): the body leaves its sect. */
	public static void leaveAsBody() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(0);
		ClientPlayNetworking.send(ModPackets.SECT_ACT, buf);
	}

	/** The Sect tab's Banish button by {@code id}'s row: cast them out. */
	public static void banish(UUID id) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(1);
		buf.writeUUID(id);
		ClientPlayNetworking.send(ModPackets.SECT_ACT, buf);
	}

	static void setInfo(Info page) { info = page; }

	/** The Sect tab's page as last sent, or null while it is on its way. */
	public static Info info() { return info; }

	/** The Sect tab is opening: its page is asked for afresh. */
	public static void request() {
		ClientPlayNetworking.send(ModPackets.SECT_INFO_REQUEST, PacketByteBufs.create());
	}

	/** The sect's name, or "" for none. */
	public static String name() { return name; }

	/** Their title in it, or null for none. */
	public static NpcTitle title() { return title; }

	public static boolean member() { return !name.isEmpty() && title != null; }

	static void clear() {
		name = "";
		title = null;
		info = null;
	}

	private ClientSect() {}
}
