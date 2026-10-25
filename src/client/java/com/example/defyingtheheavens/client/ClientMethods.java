package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.GhostMethod;
import com.example.defyingtheheavens.HeavenMethod;
import com.example.defyingtheheavens.HeavenState;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.NetherworldState;
import com.example.defyingtheheavens.Spell;
import com.example.defyingtheheavens.Spells;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * The local player's ghost methods and spells, as the server last sent them (ModPackets#METHODS): written into the client's
 * cultivation mirror (ClientCultivationData), so what a ghost method unlocks shows on the Abilities tab and wheels as it does on
 * the server; plus each spell's cooldown, counted down here between syncs. The spell wheel, the Spells and Methods tabs and the
 * readied spell's HUD line read it; it sends their choices back.
 */
public final class ClientMethods {
	/** Each spell's cooldown left when it was sent (ticks), and the game time it was sent at. */
	private static final int[] COOLDOWNS = new int[Spell.values().length];
	private static long receivedAt;
	private static final Map<Spell, ResourceLocation> ICONS = new EnumMap<>(Spell.class);

	static NetherworldState state() {
		return ClientCultivationData.get().netherworld();
	}

	/** Life Force Drain and Gain's next mode (WoodSpells.LifeMode), asked of the server (the Spells tab's switch). */
	static void nextLifeMode() {
		net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
		buf.writeByte(state().lifeMode().next().ordinal());
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(com.example.defyingtheheavens.ModPackets.LIFE_MODE, buf);
	}

	/** Whether spells change the land for good in this world (ModGameRules#SPELL_GRIEFING), and whether this player may turn it. */
	static boolean griefing = true, mayChangeGriefing;

	/** Reads a METHODS packet (on the network thread) into an action for the client thread. */
	static Runnable read(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		Map<GhostMethod, double[]> methods = new EnumMap<>(GhostMethod.class);
		for (int i = 0; i < count; i++) {
			GhostMethod method = GhostMethod.byId(buf.readUtf(64));
			int layer = buf.readVarInt();
			double progress = buf.readDouble();
			if (method != null) methods.put(method, new double[]{layer, progress});
		}
		GhostMethod active = GhostMethod.byId(buf.readUtf(64));
		Spell readied = Spell.byIndex(buf.readVarInt() - 1);
		Spell[] wheel = new Spell[Spells.WHEEL_SLOTS];
		for (int i = 0; i < wheel.length; i++) wheel[i] = Spell.byIndex(buf.readVarInt() - 1);
		int[] cooldowns = new int[Spell.values().length];
		for (int i = 0; i < cooldowns.length; i++) cooldowns[i] = buf.readVarInt();
		boolean ghost = buf.readBoolean();
		boolean reignedKing = buf.readBoolean();
		int heavenCount = buf.readVarInt();
		Map<HeavenMethod, double[]> heaven = new EnumMap<>(HeavenMethod.class);
		for (int i = 0; i < heavenCount; i++) {
			HeavenMethod method = HeavenMethod.byId(buf.readUtf(64));
			int layer = buf.readVarInt();
			double progress = buf.readDouble();
			if (method != null) heaven.put(method, new double[]{layer, progress});
		}
		HeavenMethod heavenActive = HeavenMethod.byId(buf.readUtf(64));
		int pathRank = buf.readVarInt();
		double pathCultivation = buf.readDouble();
		boolean cultivatingGhost = buf.readBoolean();
		boolean griefs = buf.readBoolean(), mayGrief = buf.readBoolean();
		com.example.defyingtheheavens.WoodSpells.LifeMode lifeMode = com.example.defyingtheheavens.WoodSpells.LifeMode.byIndex(buf.readByte());
		return () -> {
			state().setLifeMode(lifeMode);
			griefing = griefs;
			mayChangeGriefing = mayGrief;
			HeavenState h = ClientCultivationData.get().heaven();
			for (HeavenMethod method : HeavenMethod.values()) {
				double[] entry = heaven.get(method);
				h.setMethod(method, entry == null ? 0 : (int) entry[0], entry == null ? 0 : entry[1]);
			}
			h.setActiveMethod(heavenActive);
			NetherworldState s = state();
			for (GhostMethod method : GhostMethod.values()) {
				double[] entry = methods.get(method);
				s.setMethod(method, entry == null ? 0 : (int) entry[0], entry == null ? 0 : entry[1]);
			}
			s.setActiveMethod(active);
			s.setReadiedSpell(readied);
			for (int i = 0; i < wheel.length; i++) s.setSpellSlot(i, null);
			for (int i = 0; i < wheel.length; i++) if (wheel[i] != null) s.setSpellSlot(i, wheel[i]);
			s.setGhost(ghost);
			s.setReignedKing(reignedKing);
			s.setPath(pathRank, pathCultivation);
			s.setCultivatingGhost(cultivatingGhost);
			System.arraycopy(cooldowns, 0, COOLDOWNS, 0, cooldowns.length);
			receivedAt = now();
		};
	}

	private static long now() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? 0 : mc.level.getGameTime();
	}

	/** Ticks until {@code spell} can be cast again. */
	public static int cooldownLeft(Spell spell) {
		return (int) Math.max(0, COOLDOWNS[spell.ordinal()] - (now() - receivedAt));
	}

	public static boolean known(Spell spell) {
		return spell.isKnown(ClientCultivationData.get());
	}

	/** True once any spell can be cast: the spell wheel opens. */
	public static boolean anyKnown() {
		for (Spell spell : Spell.values()) {
			if (known(spell)) return true;
		}
		return false;
	}

	/** The spell in a slot of the wheel, if it can be cast (one placed there that can't, a living mortal's, shows empty). */
	static Spell shown(int slot) {
		Spell spell = state().spellSlot(slot);
		return spell != null && known(spell) ? spell : null;
	}

	/** Each spell's icon, textures/gui/spell/<id>.png: 32x32 art, drawn whole at whatever size is asked ({@link SpellWheelScreen#drawIcon}). */
	static ResourceLocation icon(Spell spell) {
		return ICONS.computeIfAbsent(spell, s -> new ResourceLocation(DefyingTheHeavens.MOD_ID, "textures/gui/spell/" + s.getId() + ".png"));
	}

	// --- Sent back ---

	static void ready(Spell spell) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(spell == null ? 0 : spell.ordinal() + 1);
		ClientPlayNetworking.send(ModPackets.READY_SPELL, buf);
	}

	/** Casts {@code spell} (null: the readied one). */
	static void cast(Spell spell) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(spell == null ? 0 : spell.ordinal() + 1);
		ClientPlayNetworking.send(ModPackets.CAST_SPELL, buf);
	}

	static void setSlot(int slot, Spell spell) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(slot);
		buf.writeVarInt(spell == null ? 0 : spell.ordinal() + 1);
		ClientPlayNetworking.send(ModPackets.SET_SPELL_SLOT, buf);
	}

	static void practise(GhostMethod method) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUtf(method.getId());
		ClientPlayNetworking.send(ModPackets.PRACTISE_METHOD, buf);
	}

	static void practise(HeavenMethod method) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUtf(method.getId());
		ClientPlayNetworking.send(ModPackets.PRACTISE_HEAVEN_METHOD, buf);
	}

	/** The Cultivation tab's switch: draw in the other qi (TwoPaths#switchPath). */
	static void switchPath() {
		ClientPlayNetworking.send(ModPackets.SWITCH_PATH, PacketByteBufs.create());
	}

	/** Leaving a world: forget its methods and spells. */
	static void clear() {
		NetherworldState s = state();
		for (GhostMethod method : GhostMethod.values()) s.setMethod(method, 0, 0);
		ClientCultivationData.get().heaven().clear();
		s.setReadiedSpell(null);
		for (int i = 0; i < Spells.WHEEL_SLOTS; i++) s.setSpellSlot(i, null);
		s.setGhost(false);
		s.setReignedKing(false);
		s.setPath(-1, 0);
		s.setCultivatingGhost(false);
		java.util.Arrays.fill(COOLDOWNS, 0);
		receivedAt = 0;
	}

	private ClientMethods() {}
}
