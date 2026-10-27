package com.example.defyingtheheavens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The mod's advancements (Milestones, ModAdvancements, 2026-10-27): all of them loaded, in five tabs, each with its words; a milestone
 * reached grants what asks for it; the sweep grants a player what they already stand at (a world from before them catching up); one asking
 * for several (the Five Phases) waits for them all.
 */
public class AdvancementsGameTests implements FabricGameTest {
	private static final String NS = DefyingTheHeavens.MOD_ID;

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyAdvancementIsThereWithItsWords(GameTestHelper helper) {
		JsonObject lang = lang();
		int ours = 0, roots = 0;
		List<String> missing = new ArrayList<>();
		for (Advancement advancement : helper.getLevel().getServer().getAdvancements().getAllAdvancements()) {
			if (!advancement.getId().getNamespace().equals(NS)) continue;
			ours++;
			DisplayInfo display = advancement.getDisplay();
			if (display == null) {
				missing.add(advancement.getId() + " (no display)");
				continue;
			}
			if (advancement.getParent() == null) {
				roots++;
				if (display.getBackground() == null) missing.add(advancement.getId() + " (a root with no background)");
			}
			for (var text : List.of(display.getTitle(), display.getDescription())) {
				String key = text.getContents() instanceof TranslatableContents t ? t.getKey() : null;
				if (key == null || !lang.has(key)) missing.add(advancement.getId() + " " + key);
			}
		}
		helper.assertTrue(ours >= 80, "The mod's advancements are loaded (" + ours + ")");
		helper.assertTrue(roots == 5, "In five tabs (" + roots + " roots)");
		helper.assertTrue(missing.isEmpty(), "Every one shown, with its title and description in English: " + missing);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void milestonesGrantTheirAdvancements(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = player(helper, made, "climber");
			helper.assertTrue(!done(helper, p, "cultivation/root"), "Nothing yet for a mortal");
			Milestones.award(p, Milestones.realm(Realm.QI_REFINING));
			helper.assertTrue(done(helper, p, "cultivation/root"), "Reaching Qi Refining opens Defying the Heavens");
			// The Five Phases asks for all five elements.
			QiElement[] five = QiElement.five();
			for (int i = 0; i < five.length - 1; i++) Milestones.award(p, Milestones.spell(five[i]));
			helper.assertTrue(!done(helper, p, "arts/five_phases"), "Four of the five are not enough");
			Milestones.award(p, Milestones.spell(five[five.length - 1]));
			helper.assertTrue(done(helper, p, "arts/five_phases"), "...the fifth completes it");
			// A spell cast by a player is counted by its kind (Milestones#onSpell): one of fire.
			ServerPlayer caster = player(helper, made, "caster");
			Milestones.onSpell(caster, firstHeavenSpellOf(QiElement.FIRE));
			helper.assertTrue(done(helper, caster, "arts/first_spell") && !done(helper, caster, "arts/heaven_demon"), "A fire spell is a first spell, not a dual one");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSweepCatchesUpWhatIsAlreadyDone(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = player(helper, made, "elder");
			PlayerCultivation c = CultivationManager.get(p);
			c.setState(Realm.NASCENT_SOUL, Stage.MID, 0);
			Milestones.sweep(p);
			helper.assertTrue(done(helper, p, "cultivation/root") && done(helper, p, "cultivation/foundation_building")
					&& done(helper, p, "cultivation/core_formation") && done(helper, p, "cultivation/nascent_soul"), "A Nascent Soul has every realm up to its own");
			helper.assertTrue(!done(helper, p, "cultivation/heavenly_being"), "...and none past it");
			c.setEpithet("Unseen Before Void Shattering");
			Milestones.sweep(p);
			helper.assertTrue(!done(helper, p, "sects/title"), "A title kept below Void Shattering is no title taken");
			c.setState(Realm.VOID_SHATTERING, Stage.EARLY, 0);
			Milestones.sweep(p);
			helper.assertTrue(done(helper, p, "cultivation/void_shattering") && done(helper, p, "sects/title"), "At Void Shattering both are granted");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	// --- Helpers ---

	private static Spell firstHeavenSpellOf(QiElement element) {
		for (Spell spell : Spell.values()) if (spell.isHeaven() && !spell.isDual() && spell.element() == element) return spell;
		throw new IllegalStateException("No heaven spell of " + element);
	}

	private static boolean done(GameTestHelper helper, ServerPlayer p, String id) {
		Advancement advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(new ResourceLocation(NS, id));
		helper.assertTrue(advancement != null, "The advancement " + id + " is there");
		return p.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static JsonObject lang() {
		try (InputStream in = DefyingTheHeavens.class.getResourceAsStream("/assets/" + NS + "/lang/en_us.json")) {
			if (in == null) return new JsonObject();
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			return new JsonObject();
		}
	}

	private static ServerPlayer player(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() {
				return false;
			}

			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		made.add(p);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) p.server.getPlayerList().remove(p);
	}
}
