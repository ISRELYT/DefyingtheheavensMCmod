package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Alignment;
import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.NpcTitle;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.TalkIntent;
import com.example.defyingtheheavens.TalkSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * The client's side of a talk with a cultivator (see TalkSystem): who they are, as the server told us when the talk opened,
 * and what they think of us now. The talk wheel and the dialogue box both draw from it.
 */
public final class TalkSession {
	public final int entityId;
	public final String name;
	public final NpcTitle title;
	public final String sectName;
	/** The rank they show, {@link CultivatorNpc#MORTAL} for a mortal. */
	public final int rank;
	public final Alignment.Faction faction;
	public int regard;
	public final List<TalkIntent> intents = new ArrayList<>();

	/** One reply in the dialogue box. */
	public record Reply(int id, Component label, boolean enabled, Component note) {}

	/** What the dialogue box shows: under which intent, what they say, and the replies on offer. */
	public record Dialogue(TalkIntent intent, Component line, List<Reply> replies) {}

	public TalkSession(int entityId, String name, NpcTitle title, String sectName, int rank, Alignment.Faction faction, int regard, int intentMask) {
		this.entityId = entityId;
		this.name = name;
		this.title = title;
		this.sectName = sectName;
		this.rank = rank;
		this.faction = faction;
		this.regard = regard;
		for (TalkIntent intent : TalkIntent.values()) {
			if ((intentMask & intent.bit()) != 0) intents.add(intent);
		}
	}

	/** "Nascent Soul - Mid", or "Mortal". */
	public Component realmName() {
		return rank < 0 ? Component.translatable(ModLang.TALK_MORTAL)
				: PlayerCultivation.rankName(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
	}

	/** The cultivator, if still loaded and in reach. */
	public Entity entity() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return null;
		Entity entity = mc.level.getEntity(entityId);
		if (entity == null || !entity.isAlive() || entity.distanceToSqr(mc.player) > TalkSystem.KEEP * TalkSystem.KEEP) return null;
		return entity;
	}

	public void sendIntent(TalkIntent intent) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entityId);
		buf.writeVarInt(intent.ordinal());
		ClientPlayNetworking.send(ModPackets.TALK_INTENT, buf);
	}

	public void sendTopic(int topic) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entityId);
		buf.writeVarInt(topic);
		ClientPlayNetworking.send(ModPackets.TALK_TOPIC, buf);
	}

	public void sendEnd() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entityId);
		ClientPlayNetworking.send(ModPackets.TALK_END, buf);
	}

	// --- Packets ---

	/** The last talk opened, so a dialogue that arrives knows whose it is. */
	private static TalkSession current;

	/** TALK_OPEN: the wheel opens. */
	public static void open(TalkSession session) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null && !(mc.screen instanceof TalkWheelScreen) && !(mc.screen instanceof DialogueScreen)) {
			// Another screen is up (it opened as the talk did): let them go rather than leave them standing for nothing.
			session.sendEnd();
			return;
		}
		current = session;
		mc.setScreen(new TalkWheelScreen(session));
	}

	/**
	 * TALK_CLOSE: they broke the talk off. Its window closes (without the usual goodbye: the server has ended it already) and
	 * why is said over the hotbar, if it was still our talk with them, window open or just answered.
	 */
	public static void close(int entityId, Component reason) {
		if (current == null || current.entityId != entityId) return;
		TalkSession session = current;
		current = null;
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof TalkWheelScreen wheel && wheel.session == session
				|| mc.screen instanceof DialogueScreen box && box.session == session) {
			mc.setScreen(null);
		}
		mc.gui.setOverlayMessage(reason, false);
	}

	/** TALK_DIALOGUE: the dialogue box opens, or shows what they said next. */
	public static void dialogue(int entityId, int regard, Dialogue dialogue) {
		if (current == null || current.entityId != entityId) return;
		current.regard = regard;
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof DialogueScreen open && open.session == current) {
			open.show(dialogue);
		} else if (mc.screen == null || mc.screen instanceof TalkWheelScreen) {
			mc.setScreen(new DialogueScreen(current, dialogue));
		}
	}
}
