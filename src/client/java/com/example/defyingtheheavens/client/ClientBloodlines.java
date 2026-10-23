package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Bloodline;
import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.InnerBodyEntity;
import com.example.defyingtheheavens.CloneBodyEntity;
import com.example.defyingtheheavens.HeartDemonEntity;
import com.example.defyingtheheavens.SoulBodyEntity;
import com.example.defyingtheheavens.SoulVesselEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every player's bloodline as the server tells it (Bloodlines; S2C bloodlines), and how a demon is drawn from it: its race's own skin
 * in place of the player's ({@link #skin}: AbstractClientPlayerMixin, and the bodies drawn as their owners, InnerBodyRenderer#skinOf),
 * broad-armed, and its beast's head and tail (BeastLayer, SoulRenderer). A human, or one who hasn't chosen yet, looks as they are.
 * <p>
 * While the race picker is open, the race shown on it is worn by this client's own player ({@link #preview}), so the figure on it
 * shows what will be chosen.
 */
public final class ClientBloodlines {
	/** A bloodline as worn: the race, its colouring, and (a fox spirit's) how many tails. */
	public record Look(Bloodline bloodline, int variant, int tails) {
		public boolean isBeast() { return bloodline.isBeast(); }
	}

	private static final Map<UUID, Look> LOOKS = new HashMap<>();
	/** The race picker's choice, worn by this client's own player while it is open; null otherwise. */
	public static Look preview;

	/** S2C bloodlines: some players' bloodlines (-1: none, a human's look). */
	public static void read(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		for (int i = 0; i < count; i++) {
			UUID id = buf.readUUID();
			Bloodline bloodline = Bloodline.byIndex(buf.readVarInt());
			int variant = buf.readVarInt();
			int tails = buf.readVarInt();
			if (bloodline == null) LOOKS.remove(id);
			else LOOKS.put(id, new Look(bloodline, variant, tails));
		}
	}

	/** {@code id}'s bloodline as worn, or null: a human's look (none chosen, or human). */
	public static Look of(UUID id) {
		if (id == null) return null;
		Minecraft mc = Minecraft.getInstance();
		Look look = preview != null && mc.player != null && id.equals(mc.player.getUUID()) ? preview : LOOKS.get(id);
		return look != null && look.isBeast() ? look : null;
	}

	/**
	 * Whose look {@code entity} wears: a player's own (or, in another player's body, that body's: Possession; in an NPC's, none here),
	 * a body's its owner's (one sitting while its soul is out, a clone's, a meditator's, a heart demon's), a soul vessel's its
	 * likeness's; null for any other.
	 */
	public static UUID owner(Entity entity) {
		if (entity instanceof AbstractClientPlayer player) {
			if (!ClientBodies.disguised(player)) return player.getUUID();
			return ClientBodies.bodyPlayer(player);
		}
		if (entity instanceof SoulBodyEntity body) return body.getOwnerId();
		if (entity instanceof CloneBodyEntity body) return body.getOwnerId();
		if (entity instanceof InnerBodyEntity body) return body.getOwnerId();
		if (entity instanceof HeartDemonEntity demon) return demon.getOwnerId();
		if (entity instanceof SoulVesselEntity vessel) return vessel.getLikeness();
		return null;
	}

	/**
	 * The bloodline {@code entity} is drawn in (see {@link #owner}), or null for a human's look: a demon cultivator's its own race
	 * ({@link #npcLook}), and so a player's soul in one's body.
	 */
	public static Look look(Entity entity) {
		if (entity instanceof CultivatorNpc npc) return npcLook(npc);
		if (entity instanceof AbstractClientPlayer player && ClientBodies.disguised(player)) {
			CultivatorNpc npc = ClientBodies.npc(player);
			if (npc != null) return npcLook(npc);
		}
		return of(owner(entity));
	}

	/** A demon cultivator's race as worn (CultivatorNpc#getBloodline), a fox's tails by the realm it shows; null for a man. */
	public static Look npcLook(CultivatorNpc npc) {
		Bloodline race = npc.getBloodline();
		return race.isBeast() ? new Look(race, npc.getRaceVariant(), race.tails(npc.getDisplayedRealm())) : null;
	}

	/** {@code id}'s race skin (in place of their own), or null: their own. */
	public static ResourceLocation skin(UUID id) {
		Look look = of(id);
		return look == null ? null : skinOf(look);
	}

	/** A race's skin in a colouring: {@code textures/entity/bloodline/<race>_<colouring>.png}, the shape of a broad-armed player's. */
	public static ResourceLocation skinOf(Look look) {
		return DefyingTheHeavens.id("textures/entity/bloodline/" + look.bloodline().getId() + "_" + look.bloodline().variantId(look.variant()) + ".png");
	}

	/** The texture of a race's head and tail in a colouring: {@code textures/entity/bloodline/<race>_<colouring>_beast.png}. */
	public static ResourceLocation beastTexture(Look look) {
		return DefyingTheHeavens.id("textures/entity/bloodline/" + look.bloodline().getId() + "_" + look.bloodline().variantId(look.variant()) + "_beast.png");
	}

	/** Disconnect: nothing carries over. */
	public static void clear() {
		LOOKS.clear();
		preview = null;
	}

	private ClientBloodlines() {}
}
