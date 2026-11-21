package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The effects work's dev-only tools (DevTools, 2026-11-21) asleep in every game not started with the dev flag: the jar carries them,
 * nothing registers them. The game tests run without -Ddth.dev=true, as every player's game does.
 */
public class DevToolsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void theCaptureCommandExistsOnlyWithTheDevFlag(GameTestHelper helper) {
		boolean command = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("vfxcapture") != null;
		helper.assertTrue(command == DevTools.ENABLED, "/vfxcapture is there exactly when the dev flag is on (flag: " + DevTools.ENABLED + ")");
		helper.assertTrue(!VfxCapture.busy(), "No capture is running in a game that never started one");
		helper.succeed();
	}
}
