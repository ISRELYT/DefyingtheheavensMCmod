package com.example.defyingtheheavens.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * What cultivators nearby are saying (S2C npc_say, see NpcSpeech): a line each, until when (client game time).
 * CultivatorNpcRenderer draws it over the speaker's name, fading in and out.
 */
public final class ClientNpcSpeech {
	/** A line said at {@code from}, up until {@code until}. */
	public record Said(Component line, long from, long until) {
		/** 0-1: how visible it is now, fading in over its first few ticks and out over its last ten. */
		public float alpha(long now, float partialTick) {
			float t = now + partialTick;
			return Math.max(0, Math.min(1, Math.min((t - from) / 4f, (until - t) / 10f)));
		}
	}

	private static final Map<Integer, Said> SAID = new HashMap<>();

	public static void say(int entityId, Component line, int ticks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		long now = mc.level.getGameTime();
		SAID.put(entityId, new Said(line, now, now + ticks));
	}

	/** What {@code entity} is saying now, or null. */
	public static Said of(Entity entity) {
		Said said = SAID.get(entity.getId());
		if (said == null) return null;
		if (entity.level().getGameTime() >= said.until()) {
			SAID.remove(entity.getId());
			return null;
		}
		return said;
	}

	public static void clear() {
		SAID.clear();
	}

	private ClientNpcSpeech() {}
}
