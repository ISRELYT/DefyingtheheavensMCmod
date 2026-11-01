package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Body Path, phase 1 (the user's ask of 2026-11-01: a fourth path, its realms and the body's strength, its realms their own names): begun
 * at a mortal's Peak (refused short of it), drawn in by the switch; its deeds and its meditation tempering it; the body it gives against the
 * qi paths'; its bottleneck and tribulation; its years; Shattered Meridians' old body refining carried onto it.
 */
public class BodyPathGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6 * Math.max(1, Math.abs(b));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aMortalAtPeakBeginsTheBodyPathAndItsDeedsTemperIt(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "body-mortal");
			PlayerCultivation c = CultivationManager.own(p);
			c.setMortal(true);
			c.setMortalState(MortalStage.HIGH, 0);
			helper.assertTrue(!BodyPath.available(c) && !BodyPath.begin(p) && BodyPath.rank(c) < 0, "Short of Peak: no Body Path");
			c.setMortalState(MortalStage.PEAK, 0);
			helper.assertTrue(BodyPath.available(c) && BodyPath.begin(p) && BodyPath.rank(c) == 0, "At Peak: begun, Copper Skin Early");
			helper.assertTrue(TwoPaths.cultivatesBody(c) && TwoPaths.drawnIn(c) == TwoPaths.Path.BODY
					&& CultivationMethods.practised(c, TwoPaths.Path.BODY) == CultivationMethod.BODY_TEMPERING && BodyPath.canGain(c),
					"Drawn in, by Body Tempering, the body its own volume");
			helper.assertTrue(ModLang.bodyRefiningKey("copper_skin").equals(((net.minecraft.network.chat.contents.TranslatableContents)
					BodyPath.realmName(Realm.QI_REFINING).getContents()).getKey())
					&& ((net.minecraft.network.chat.contents.TranslatableContents) BodyPath.realmName(Realm.TRUE_IMMORTAL).getContents()).getKey()
					.equals(ModLang.bodyRefiningKey("indestructible_body")), "Its realms' own names");
			double perSecond = BodyPath.perSecond(c, 0);
			BodyPath.deed(p, 10);
			helper.assertTrue(perSecond > 0 && near(c.body().cultivation(), perSecond * 10), "A deed of ten seconds tempers ten seconds' worth");
			helper.assertTrue(near(BodyPath.killFactor(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY), 0), BodyPath.MOST_KILL_FACTOR)
					&& near(BodyPath.killFactor(0, PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY)), 1 / BodyPath.MOST_KILL_FACTOR)
					&& near(BodyPath.killFactor(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.LATE), 0), 2)
					&& BodyPath.killFactor(-1, 0) == 1, "The slain worth more the stronger, within the bounds");
			// Meditation: a mortal on the Body Path sits down to it.
			p.setOnGround(true);
			MeditationManager.start(p);
			helper.assertTrue(MeditationManager.isMeditating(p.getUUID()), "A mortal on the Body Path meditates");
			MeditationManager.stop(p, false);
			helper.assertTrue(Lifespan.spanRank(c) == BodyPath.rank(c), "Its realm gives the mortal its years");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBodyPathGivesTheMightierBody(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setMortal(false);
		c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		c.body().set(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0);
		TwoPaths.Body body = TwoPaths.body(c, false);
		helper.assertTrue(body.rank() == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY) && near(body.share(), BodyPath.BODY),
				"Steel Sinew beside Core Formation: the body is the Body Path's, half again as strong (" + body.share() + ")");
		c.setState(Realm.NASCENT_SOUL, Stage.LATE, 0);
		body = TwoPaths.body(c, false);
		helper.assertTrue(body.rank() == PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.LATE) && near(body.share(), 1.0),
				"A heaven path far above it gives the body instead");
		helper.assertTrue(TwoPaths.higherRank(c, false) == PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.LATE),
				"The Body Path is no qi path: the highest qi path's rank leaves it out");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void itsBottleneckCallsTheHeavensAndTheSwitchTurnsToIt(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "body-cultivator");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			TestMethods.setAt(c, Realm.CORE_FORMATION, Stage.EARLY);
			helper.assertTrue(BodyPath.begin(p) && TwoPaths.drawnIn(c) == TwoPaths.Path.BODY, "A cultivator begins it beside the heaven path");
			TwoPaths.switchPath(p);
			helper.assertTrue(TwoPaths.drawnIn(c) == TwoPaths.Path.HEAVEN, "The switch turns back to the heaven path");
			TwoPaths.switchPath(p);
			helper.assertTrue(TwoPaths.drawnIn(c) == TwoPaths.Path.BODY, "...and round to the body again");
			int gp = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION);
			c.body().set(gp - 1, 0);
			BodyPath.addCultivation(c, BodyPath.required(gp - 1) + BodyPath.required(gp) * 2);
			helper.assertTrue(BodyPath.rank(c) == gp && BodyPath.atBottleneck(c) && near(c.body().cultivation(), BodyPath.required(gp)),
					"A stage stepped through by itself, then held full at Grand Perfection");
			BodyPath.deed(p, 100);
			helper.assertTrue(near(c.body().cultivation(), BodyPath.required(gp)), "Held: deeds temper nothing more");
			helper.assertTrue(BodyPath.canBreakthrough(c), "Ready for its tribulation (Nascent Soul is below the lower realms' cap)");
			CultivationManager.tryBreakthrough(p);
			helper.assertTrue(TribulationManager.isActive(p.getUUID()) && c.body().inTribulation(), "The heavens' lightning comes for the flesh");
			TribulationManager.abandon(p);
			helper.assertTrue(!TribulationManager.isActive(p.getUUID()) && !c.body().inTribulation(), "Abandoned, nothing lost");
			helper.assertTrue(BodyPath.breakthrough(c) && BodyPath.rank(c) == PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY)
					&& BodyPath.needsTribulation(BodyPath.rank(c)), "Through: Jade Bone, where every stage wants the heavens");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shatteredMeridiansBodyRefiningIsCarriedOntoTheBodyPath(GameTestHelper helper) {
		PlayerCultivation old = new PlayerCultivation();
		old.setPhysique(Physique.SHATTERED_MERIDIANS);
		old.setMortal(true);
		CompoundTag tag = old.save();
		tag.putInt("MortalStage", MortalStage.PEAK.ordinal());
		tag.putInt("BodyRefining", 3);
		tag.putDouble("Tempering", BodyRefining.toNext(3) / 2);
		PlayerCultivation c = PlayerCultivation.load(tag);
		helper.assertTrue(BodyPath.rank(c) == BodyRefining.rank(3) && c.getBodyRefining() == 0 && c.body().cultivating()
				&& near(c.body().cultivation(), BodyPath.required(BodyRefining.rank(3)) / 2),
				"Its third refining is the Body Path's rank it was as strong as, half way on, drawn in (" + BodyPath.rank(c) + ")");
		PlayerCultivation again = PlayerCultivation.load(c.save());
		helper.assertTrue(BodyPath.rank(again) == BodyPath.rank(c) && near(again.body().cultivation(), c.body().cultivation()), "Saved and loaded as it is");
		helper.succeed();
	}

	// --- Helpers ---

	/** A survival player in the player list (the helper's mock player is creative), remembered in {@code made}. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		made.add(p);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			MeditationManager.forget(p.getUUID());
			TribulationManager.forget(p.getUUID());
			BodyPath.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
