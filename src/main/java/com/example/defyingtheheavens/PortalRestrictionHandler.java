package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * No shortcuts from the high realms: Nether portals won't light or teleport and End portal frames won't take eyes inside
 * the Upper Realm and the Spatial Gap. Enforced by the mixins in the mixin package (PortalShape, NetherPortalBlock,
 * EndPortalBlock, EnderEyeItem); vanilla already refuses to light Nether portals outside the Overworld and Nether, the
 * PortalShape mixin makes that explicit for any other ignition path.
 */
public final class PortalRestrictionHandler {
	private static final int MESSAGE_COOLDOWN_TICKS = 60;
	private static final Map<UUID, Long> LAST_MESSAGE = new HashMap<>();

	public static boolean isRestricted(Level level) {
		return level != null && (ModDimensions.isUpperRealm(level.dimension()) || ModDimensions.isSpatialGap(level.dimension()));
	}

	/** Also handles the world-generation views of a level (WorldGenRegion). */
	public static boolean isRestricted(LevelAccessor level) {
		if (level instanceof Level real) return isRestricted(real);
		if (level instanceof ServerLevelAccessor serverAccessor) {
			ServerLevel server = serverAccessor.getLevel();
			return isRestricted(server);
		}
		return false;
	}

	/** Tells a player (at most every few seconds) why the portal did nothing. */
	public static void notifyBlocked(Entity entity) {
		if (!(entity instanceof ServerPlayer player)) return;
		long now = player.level().getGameTime();
		Long last = LAST_MESSAGE.get(player.getUUID());
		if (last != null && now - last < MESSAGE_COOLDOWN_TICKS && now >= last) return;
		LAST_MESSAGE.put(player.getUUID(), now);
		player.displayClientMessage(Component.translatable(ModLang.MSG_PORTAL_BLOCKED), true);
	}

	public static void forget(UUID id) {
		LAST_MESSAGE.remove(id);
	}

	private PortalRestrictionHandler() {}
}
