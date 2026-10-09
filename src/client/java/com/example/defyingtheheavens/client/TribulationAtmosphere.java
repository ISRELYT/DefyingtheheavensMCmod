package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.TribulationCloud;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Local overcast colouring for every observer near a tracked tribulation, without changing server weather. */
public final class TribulationAtmosphere {
	private static ClientLevel lastLevel;
	private static float previous;
	private static float current;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != lastLevel) {
				lastLevel = client.level;
				previous = current = 0;
			}
			previous = current;
			float target = 0;
			if (client.level != null && client.getCameraEntity() != null) {
				Vec3 camera = client.gameRenderer.getMainCamera().getPosition();
				for (Entity entity : client.level.entitiesForRendering()) {
					if (!(entity instanceof TribulationCloud cloud) || entity.isRemoved()) continue;
					// The cloud layer has a fixed altitude; proximity is measured beneath it.
					float distance = (float) Math.hypot(camera.x - entity.getX(), camera.z - entity.getZ());
					float proximity = Mth.clamp((80.0f * cloud.stormScale() - distance) / (56.0f * cloud.stormScale()), 0, 1);
					target = Math.max(target, proximity * proximity * (3 - 2 * proximity));
				}
			}
			// About two seconds to gather or clear; overlapping trials use the strongest effect.
			current += Mth.clamp(target - current, -0.025f, 0.025f);
		});
	}

	public static float strength(float partialTick) {
		return Mth.lerp(partialTick, previous, current);
	}

	public static Vec3 tint(Vec3 color, float partialTick) {
		double grey = (color.x * 0.3 + color.y * 0.59 + color.z * 0.11) * 0.85;
		return color.lerp(new Vec3(grey, grey, grey), strength(partialTick));
	}

	private TribulationAtmosphere() {}
}
