package com.example.defyingtheheavens.client.compat;

import com.example.defyingtheheavens.ModDimensions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientLevel;

import java.lang.reflect.Method;

/**
 * Iris and its shader packs, when they are installed (looked up by name: the mod never needs them). A shader pack draws the
 * world in its own passes and keeps only what comes through the render types it knows, so some of the mod's own drawing is done
 * otherwise while one is on (SoulRenderer#drawGhostArm), and the realms' own skies are laid over its (SkyLayer); it picks its
 * programs by dimension, the Overworld's for any it doesn't know, so the Netherworld is named the Nether to it
 * (IrisDimensionMixin): its dark, starless, cloudless sky, lit by the fog. And where the pack's sky stays (the Upper Realm, sun,
 * clouds and day), it is painted from the sky's colour alone (Complementary Reimagined: lib/colors/skyColors.glsl), which is
 * given the tint the realm's own sky has (#skyColour).
 */
public final class ShaderPacks {
	private static final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");
	private static Object api;
	private static Method inUse;
	private static Object nether;
	private static boolean looked;

	/** Whether a shader pack is drawing the world right now. */
	public static boolean inUse() {
		if (!IRIS) return false;
		try {
			if (!looked) {
				looked = true;
				Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
				api = type.getMethod("getInstance").invoke(null);
				inUse = type.getMethod("isShaderPackInUse");
			}
			return api != null && (Boolean) inUse.invoke(api);
		} catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
			api = null;
			return false;
		}
	}

	/**
	 * The sky's colour as the game gives it (ClientLevelSkyMixin), seen by a shader pack. The Upper Realm's own sky
	 * (UpperRealmSkyRenderer) blends its azure into a gold horizon and reads jade green; a pack, painting only from this colour,
	 * drew it pale blue-grey, so while one is on the colour is turned toward jade. The mod's own sky, drawn without a pack, is
	 * untouched.
	 */
	public static net.minecraft.world.phys.Vec3 skyColour(ClientLevel level, net.minecraft.world.phys.Vec3 colour) {
		if (!ModDimensions.isUpperRealm(level.dimension()) || !inUse()) return colour;
		return colour.multiply(0.72, 1.05, 0.68);
	}

	/**
	 * The dimension a shader pack is told {@code level} is (Iris's own name for it), or null to leave it be: the Netherworld is
	 * the Nether.
	 */
	public static Object dimensionFor(ClientLevel level) {
		if (level == null || !ModDimensions.isNetherworld(level.dimension())) return null;
		try {
			if (nether == null) nether = Class.forName("net.irisshaders.iris.shaderpack.DimensionId").getField("NETHER").get(null);
			return nether;
		} catch (ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	private ShaderPacks() {}
}
