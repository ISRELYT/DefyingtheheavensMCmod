package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Each player's bloodline (Bloodline) and its colouring, kept for the world with the Overworld rather than in a life's record, so it
 * stays theirs through every life and is known while they are away (a body or a ghost of theirs left in the world is drawn in it).
 * <p>
 * The first time a player joins (or the first time since bloodlines came, a world from before: their realm and all stay as they
 * were) they are asked to choose ({@link #onJoin}: the client's race picker), and asked again each time they join until they do;
 * until then they look as a human. Chosen once, it is theirs: only {@code /cultivation bloodline} changes it, or asks again. Every
 * client is told every bloodline ({@link #send}), and a fox spirit's tails as its realm grows them ({@link #tick}).
 */
public class Bloodlines extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_bloodlines";

	/** A player's bloodline and which of its colourings. */
	public record Entry(Bloodline bloodline, int variant) {}

	private final Map<UUID, Entry> entries = new HashMap<>();
	/** The tails each fox spirit online was last told to have (by everyone), so they are told again when it grows another. */
	private static final Map<UUID, Integer> TOLD_TAILS = new HashMap<>();

	public static Bloodlines get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Bloodlines::load, Bloodlines::new, NAME);
	}

	/** {@code id}'s bloodline and colouring, or null: not chosen yet. */
	public static Entry entry(MinecraftServer server, UUID id) {
		return get(server).entries.get(id);
	}

	/** {@code id}'s bloodline: a human's until chosen. */
	public static Bloodline of(MinecraftServer server, UUID id) {
		Entry entry = entry(server, id);
		return entry == null ? Bloodline.HUMAN : entry.bloodline();
	}

	/** On joining: told every bloodline there is; asked to choose theirs while they haven't (not while their body is held: Possession). */
	public static void onJoin(ServerPlayer player) {
		Bloodlines data = get(player.server);
		send(player, data.entries);
		// The others are told this one's tails as it stands now (their bloodline they were told when it was chosen).
		if (data.entries.containsKey(player.getUUID())) broadcast(player.server, player.getUUID());
		if (!data.entries.containsKey(player.getUUID()) && !Possession.isHeld(player.getUUID())) ask(player);
	}

	/** S2C bloodline_ask: the race picker comes up. */
	public static void ask(ServerPlayer player) {
		ServerPlayNetworking.send(player, ModPackets.BLOODLINE_ASK, PacketByteBufs.create());
	}

	/** C2S bloodline_choose: {@code player} chose; theirs if they had none, and everyone is told. Once chosen, the choice is ignored. */
	public static void choose(ServerPlayer player, Bloodline bloodline, int variant) {
		if (bloodline == null || get(player.server).entries.containsKey(player.getUUID())) return;
		set(player.server, player.getUUID(), bloodline, variant);
		player.sendSystemMessage(Component.translatable(bloodline.isBeast() ? ModLang.MSG_BLOODLINE_BEAST : ModLang.MSG_BLOODLINE_HUMAN,
				bloodline.isBeast() ? bloodline.variantName(variant) : bloodline.getDisplayName(), bloodline.getDisplayName()).withStyle(ChatFormatting.GOLD));
		player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, bloodline.isBeast() ? 0.8f : 1.0f);
		Journal.note(player, "bloodline", ModLang.JOURNAL_BLOODLINE, Component.translatable(ModLang.bloodlineLoreKey(bloodline.getId())));
	}

	/** {@code id}'s bloodline is {@code bloodline} (colouring {@code variant}), and everyone is told. */
	public static void set(MinecraftServer server, UUID id, Bloodline bloodline, int variant) {
		Bloodlines data = get(server);
		int clamped = bloodline.variants() == 0 ? 0 : Math.floorMod(variant, bloodline.variants());
		data.entries.put(id, new Entry(bloodline, clamped));
		data.setDirty();
		broadcast(server, id);
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player != null) DemonPath.born(player); // a demon's demon core, its blood on its record
	}

	/** {@code id}'s choice undone: they look human again, and are asked anew (now, if they're here, else when they next join). */
	public static void forget(MinecraftServer server, UUID id) {
		Bloodlines data = get(server);
		if (data.entries.remove(id) != null) data.setDirty();
		TOLD_TAILS.remove(id);
		broadcastGone(server, id);
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player != null) {
			DemonPath.born(player); // a human's look on the record again (its Demon Path, if any, at rest until its blood is chosen)
			ask(player);
		}
	}

	/** How many tails {@code id} shows: its realm's for a fox spirit (the higher of its paths), one for any other. */
	public static int tails(MinecraftServer server, UUID id) {
		if (of(server, id) != Bloodline.FOX) return 1;
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player == null) return TOLD_TAILS.getOrDefault(id, 1);
		PlayerCultivation c = CultivationManager.get(player);
		if (c.isMortal()) return 1;
		return Bloodline.tails(Realm.byIndex(TwoPaths.higherRank(c, false) / Stage.values().length));
	}

	/** Each second: a fox spirit that has grown a tail (or lost one) shows it to everyone. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		Bloodlines data = get(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Entry entry = data.entries.get(player.getUUID());
			if (entry == null || entry.bloodline() != Bloodline.FOX) continue;
			int tails = tails(server, player.getUUID());
			Integer told = TOLD_TAILS.get(player.getUUID());
			if (told == null || told != tails) broadcast(server, player.getUUID());
		}
	}

	/** S2C bloodlines to everyone: {@code id}'s, as it stands. */
	private static void broadcast(MinecraftServer server, UUID id) {
		Bloodlines data = get(server);
		Map<UUID, Entry> one = new HashMap<>();
		Entry entry = data.entries.get(id);
		if (entry != null) one.put(id, entry);
		for (ServerPlayer other : server.getPlayerList().getPlayers()) send(other, one);
	}

	/** S2C bloodlines to everyone: {@code id} has none (a human's look again). */
	private static void broadcastGone(MinecraftServer server, UUID id) {
		for (ServerPlayer other : server.getPlayerList().getPlayers()) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeVarInt(1);
			buf.writeUUID(id);
			buf.writeVarInt(-1);
			buf.writeVarInt(0);
			buf.writeVarInt(1);
			ServerPlayNetworking.send(other, ModPackets.BLOODLINES, buf);
		}
	}

	/** S2C bloodlines: these players' bloodlines (by ordinal, -1 none), colourings and tails. */
	private static void send(ServerPlayer to, Map<UUID, Entry> entries) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entries.size());
		entries.forEach((id, entry) -> {
			int tails = tails(to.server, id);
			if (entry.bloodline() == Bloodline.FOX && to.server.getPlayerList().getPlayer(id) != null) TOLD_TAILS.put(id, tails);
			buf.writeUUID(id);
			buf.writeVarInt(entry.bloodline().ordinal());
			buf.writeVarInt(entry.variant());
			buf.writeVarInt(tails);
		});
		ServerPlayNetworking.send(to, ModPackets.BLOODLINES, buf);
	}

	/** Every bloodline, for the game tests. */
	static List<UUID> chosen(MinecraftServer server) {
		return List.copyOf(get(server).entries.keySet());
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		entries.forEach((id, e) -> {
			CompoundTag entry = new CompoundTag();
			entry.putUUID("Id", id);
			entry.putString("Bloodline", e.bloodline().getId());
			entry.putInt("Variant", e.variant());
			list.add(entry);
		});
		tag.put("Entries", list);
		return tag;
	}

	static Bloodlines load(CompoundTag tag) {
		Bloodlines data = new Bloodlines();
		ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			Bloodline bloodline = Bloodline.byId(entry.getString("Bloodline"));
			if (bloodline != null) data.entries.put(entry.getUUID("Id"), new Entry(bloodline, entry.getInt("Variant")));
		}
		return data;
	}
}
