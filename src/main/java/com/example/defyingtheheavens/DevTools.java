package com.example.defyingtheheavens;

/**
 * The developer's switch: {@code -Ddth.dev=true} on the game's command line (the test harness's runs set it). Without it no dev command
 * exists (VfxCapture's /vfxcapture, and later /breakthroughfx) and no dev hook is applied (the dev mixins, DevMixinPlugin), so a
 * player's game never has them: they are in the jar, asleep.
 */
public final class DevTools {
	/** The flag's name, read once at start-up. */
	public static final String FLAG = "dth.dev";
	public static final boolean ENABLED = Boolean.getBoolean(FLAG);

	private DevTools() {}
}
