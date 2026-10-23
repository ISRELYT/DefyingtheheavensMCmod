package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The user's asks of 2026-10-23: the great spells' wounds in the land sized by their realm as a core's blast is (Devastation#scale),
 * Heaven-Severing's slit half as tall; a soul's upkeep the same at every realm and its tether endless from Void Shattering; a Sect
 * Master's or an elder's body may cast out those below it (SectMembership#mayBanish); technique manuals (TechniqueManualItem), learned
 * and wielded only at their realm and written out again on a blank; and Clone (Clones): split off with half the qi, played at one's
 * own realm with half one's strength, the main body sitting where it was, drawn back in, and slain.
 */
public class TechniquesCloneGameTests implements FabricGameTest {
	// --- The land's wounds ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGreatSpellsWoundTheLandByTheirRealm(GameTestHelper helper) {
		helper.assertTrue(Math.abs(Devastation.scale(Realm.FOUR_AXIS) - 1) < 1e-9, "Four Axis's spells are the yardstick");
		double last = 0;
		for (Realm realm : Realm.values()) {
			if (realm.ordinal() < Realm.CORE_FORMATION.ordinal()) continue;
			double scale = Devastation.scale(realm);
			helper.assertTrue(scale > last, "Greater with every realm, as a core's blast is: " + realm.getId() + " (" + scale + ")");
			last = scale;
		}
		helper.assertTrue(Devastation.scale(Realm.NASCENT_SOUL) < 0.6 && Devastation.scale(Realm.HEAVENLY_BEING) > 0.7
				&& Devastation.scale(Realm.HEAVENLY_BEING) < 0.85, "Nascent Soul's a little over half Four Axis's, Heavenly Being's some four fifths");
		helper.assertTrue(Math.abs(Devastation.sized(Realm.HEAVENLY_BEING, 40) - 40 * Devastation.scale(Realm.HEAVENLY_BEING)) < 1e-9, "Sized by the scale");
		helper.assertTrue(MetalSpells.SEVER_THICK == 5, "Heaven-Severing's slit half as tall as it was (" + MetalSpells.SEVER_THICK + ")");
		helper.assertTrue(EarthSpells.TOMB_FISSURES == 40 && WoodSpells.TREE_FISSURES == 40 && FireSpells.SUNS_CRATER == 9,
				"Four Axis's own spells cut as they did");
		helper.assertTrue(FireSpells.METEOR_CRATER < FireSpells.SUNS_CRATER * 2 && FireSpells.METEOR_CRATER == Math.round(23 * Devastation.scale(Realm.NASCENT_SOUL)),
				"A Nascent Soul's meteor craters by its realm (" + FireSpells.METEOR_CRATER + ")");
		helper.succeed();
	}

	// --- The soul's upkeep ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSoulCostsAlikeAtEveryRealmAndGoesFreeFromVoidShattering(GameTestHelper helper) {
		PlayerCultivation nascent = new PlayerCultivation();
		nascent.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		PlayerCultivation immortal = new PlayerCultivation();
		immortal.setState(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, 0);
		for (double reach : new double[] {0, 0.5, 1}) {
			for (boolean possessing : new boolean[] {false, true}) {
				double low = nascent.soulCostPerSecond(reach, possessing), high = immortal.soulCostPerSecond(reach, possessing);
				helper.assertTrue(Math.abs(low - high) < 1e-9, "The same upkeep at every realm (" + reach + ", " + possessing + "): " + low + " / " + high);
			}
		}
		helper.assertTrue(Math.abs(nascent.soulCostPerSecond(0, false) - SoulProjection.SOUL_QI_BASE * 0.5) < 1e-9
				&& Math.abs(nascent.soulCostPerSecond(1, true) - SoulProjection.SOUL_QI_BASE * 3) < 1e-9, "Half the base beside the body, three times it possessing at the end");
		// Against a True Immortal's gathering it is nothing; against a Nascent Soul's, its whole gathering and more.
		helper.assertTrue(immortal.soulCostPerSecond(1, true) < CultivationStats.qiGather(Realm.TRUE_IMMORTAL, Stage.EARLY) * 0.1, "A True Immortal never runs dry");
		helper.assertTrue(nascent.soulCostPerSecond(1, true) > CultivationStats.qiGather(Realm.NASCENT_SOUL, Stage.EARLY), "A Nascent Soul still does");
		PlayerCultivation axis = new PlayerCultivation();
		axis.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		PlayerCultivation shatterer = new PlayerCultivation();
		shatterer.setState(Realm.VOID_SHATTERING, Stage.EARLY, 0);
		helper.assertTrue(!axis.soulTetherEndless() && axis.soulTetherRadius() < SoulProjection.ENDLESS_TETHER, "Four Axis's tether still has an end");
		helper.assertTrue(shatterer.soulTetherEndless() && shatterer.soulTetherRadius() == SoulProjection.ENDLESS_TETHER
				&& immortal.soulTetherEndless(), "From Void Shattering it has none");
		helper.succeed();
	}

	// --- Banishing ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void aMasterOrAnElderCastsOutThoseBelow(GameTestHelper helper) {
		UUID master = UUID.randomUUID(), elder = UUID.randomUUID(), disciple = UUID.randomUUID();
		SectMembership.Seat asMaster = new SectMembership.Seat(null, master, NpcTitle.SECT_MASTER, null, true);
		SectMembership.Seat asElder = new SectMembership.Seat(null, elder, NpcTitle.ELDER, null, true);
		SectMembership.Seat asDisciple = new SectMembership.Seat(null, disciple, NpcTitle.INNER_DISCIPLE, null, true);
		helper.assertTrue(SectMembership.mayBanish(asMaster, elder, NpcTitle.ELDER) && SectMembership.mayBanish(asMaster, disciple, NpcTitle.OUTER_DISCIPLE)
				&& SectMembership.mayBanish(asMaster, UUID.randomUUID(), NpcTitle.GRAND_ELDER), "A Sect Master casts out anyone");
		helper.assertTrue(SectMembership.mayBanish(asElder, disciple, NpcTitle.INNER_DISCIPLE), "An elder those below");
		helper.assertTrue(!SectMembership.mayBanish(asElder, UUID.randomUUID(), NpcTitle.ELDER) && !SectMembership.mayBanish(asElder, master, NpcTitle.SECT_MASTER),
				"Not their equals nor those above");
		helper.assertTrue(!SectMembership.mayBanish(asDisciple, UUID.randomUUID(), NpcTitle.OUTER_DISCIPLE), "A disciple no one");
		helper.assertTrue(!SectMembership.mayBanish(asMaster, master, NpcTitle.SECT_MASTER) && !SectMembership.mayBanish(null, disciple, NpcTitle.OUTER_DISCIPLE),
				"Not oneself; not with no seat");
		// A player who is an Elder or Grand Elder of the sect themselves, in no one's body: no one (the user's pick).
		SectMembership.Seat playerElder = new SectMembership.Seat(null, UUID.randomUUID(), NpcTitle.GRAND_ELDER, null, false);
		helper.assertTrue(!SectMembership.mayBanish(playerElder, disciple, NpcTitle.OUTER_DISCIPLE), "A player's own title casts out no one");
		helper.succeed();
	}

	// --- A treasury no hopper drains ---

	/** Its own batch: its hopper takes up whatever lies loose above it, a neighbouring test's drops too. */
	@GameTest(template = EMPTY_STRUCTURE, batch = "treasury_hopper")
	public void noHopperDrawsFromATreasury(GameTestHelper helper) {
		net.minecraft.server.level.ServerLevel level = helper.getLevel();
		BlockPos vault = helper.absolutePos(new BlockPos(2, 2, 2));
		Sect sect = new Sect(UUID.randomUUID());
		sect.dimension = level.dimension();
		sect.treasury = new net.minecraft.world.level.levelgen.structure.BoundingBox(vault.getX() - 1, vault.getY() - 1, vault.getZ() - 1,
				vault.getX() + 1, vault.getY() + 1, vault.getZ() + 1);
		sect.bounds = sect.treasury.inflatedBy(8);
		SectData.get(level.getServer()).put(sect);
		try {
			level.setBlock(vault, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
			level.setBlock(vault.below(), net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState(), 3);
			helper.assertTrue(SectManager.treasuryAt(level, vault) && !SectManager.treasuryAt(level, vault.offset(3, 0, 0)), "The vault, and only it, is a treasury");
			var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(vault);
			chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
			var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(vault.below());
			// (The chest's own count: refused the chest, a hopper takes up what lies loose above it, as vanilla's does, and the kept
			// game-test world may have things lying there from earlier runs.)
			net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, hopper);
			helper.assertTrue(chest.getItem(0).getCount() == 3, "A hopper under a treasury chest draws nothing out of it (" + chest.getItem(0) + ")");
			sect.extinct = true; // no sect's treasury now: the same hopper works as ever
			net.minecraft.world.level.block.entity.HopperBlockEntity.suckInItems(level, hopper);
			helper.assertTrue(chest.getItem(0).getCount() == 2, "Under any other chest it draws as ever (" + chest.getItem(0) + ")");
		} finally {
			sect.extinct = true;
			level.setBlock(vault.below(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(vault, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
		}
		helper.succeed();
	}

	// --- Technique manuals ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTechniqueIsLearnedAndWieldedOnlyAtItsRealm(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer low = survivor(helper, made, "manual_low", Realm.CORE_FORMATION, Stage.GRAND_PERFECTION);
			ServerPlayer high = survivor(helper, made, "manual_high", Realm.NASCENT_SOUL, Stage.EARLY);
			PlayerCultivation l = CultivationManager.get(low), h = CultivationManager.get(high);
			helper.assertTrue(!TechniqueManualItem.learn(low, Technique.CLONE) && !l.knowsTechnique(Technique.CLONE) && !Ability.CLONE.isUnlocked(l),
					"Below Nascent Soul the Clone manual teaches nothing");
			// Read from the manual itself: learned, and the manual kept.
			ItemStack manual = TechniqueManualItem.of(Technique.CLONE);
			high.setItemInHand(InteractionHand.MAIN_HAND, manual);
			manual.use(high.level(), high, InteractionHand.MAIN_HAND);
			helper.assertTrue(h.knowsTechnique(Technique.CLONE) && Ability.CLONE.isUnlocked(h), "At Nascent Soul it is learned, and Clone is theirs");
			helper.assertTrue(high.getMainHandItem().is(ModItems.TECHNIQUE_MANUAL) && high.getMainHandItem().getCount() == 1, "The manual isn't used up");
			helper.assertTrue(!TechniqueManualItem.learn(high, Technique.CLONE), "Not learned twice");
			// Fallen below its realm (a spent life, a broken foundation), it is still known but not wielded.
			h.setState(Realm.CORE_FORMATION, Stage.LATE, 0);
			helper.assertTrue(h.knowsTechnique(Technique.CLONE) && !Ability.CLONE.isUnlocked(h), "Known for good, wielded only at its realm");
			h.setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
			helper.assertTrue(Ability.CLONE.isUnlocked(h), "And at every realm above it");
			// Saved and loaded with the rest.
			PlayerCultivation loaded = PlayerCultivation.load(h.save());
			helper.assertTrue(loaded.knowsTechnique(Technique.CLONE), "Kept in the record");
			helper.assertTrue(TechniqueManualItem.technique(TechniqueManualItem.of(Technique.CLONE)) == Technique.CLONE, "A manual says what it holds");
		} finally {
			leave(made);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSpellFromAManualIsCastOnlyAtItsRealm(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			Spell spell = spellAbove(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION));
			helper.assertTrue(spell != null, "A spell of an element above Core Formation");
			int stages = Stage.values().length;
			Realm realm = Realm.byIndex(spell.realmNeeded() / stages);
			Stage stage = Stage.byIndex(spell.realmNeeded() % stages);
			ServerPlayer low = survivor(helper, made, "spell_low", Realm.CORE_FORMATION, Stage.GRAND_PERFECTION);
			ServerPlayer reader = survivor(helper, made, "spell_reader", realm, stage);
			PlayerCultivation l = CultivationManager.get(low), r = CultivationManager.get(reader);
			helper.assertTrue(!spell.isKnown(r), "Not known without its method");
			helper.assertTrue(!TechniqueManualItem.learn(low, spell) && !l.knowsByManual(spell), "Below its realm the manual teaches nothing");
			helper.assertTrue(TechniqueManualItem.learn(reader, spell) && spell.isKnown(r), "At its realm it is learned and cast, no method needed");
			helper.assertTrue(PlayerCultivation.load(r.save()).knowsByManual(spell), "Kept in the record");
			// Fallen a stage below it, known still but not cast.
			int below = spell.realmNeeded() - 1;
			r.setState(Realm.byIndex(below / stages), Stage.byIndex(below % stages), 0);
			helper.assertTrue(r.knowsByManual(spell) && !spell.isKnown(r), "Cast only at its realm");
			// Written out again on a blank: what one knows at one's realm, by brush.
			r.setState(realm, stage, 0);
			r.learnTechnique(Technique.CLONE);
			reader.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BLANK_MANUAL, 2));
			reader.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.INSCRIPTION_BRUSH));
			boolean technique = false, theSpell = false;
			for (int[] option : TechniqueManualItem.writable(reader)) {
				technique |= option[0] == 0 && option[1] == Technique.CLONE.ordinal();
				theSpell |= option[0] == 1 && option[1] == spell.ordinal();
			}
			helper.assertTrue(technique && theSpell, "Both may be written out");
			Spell unknown = null;
			for (Spell s : Spell.values()) if (!s.isDual() && !s.isKnown(r)) { unknown = s; break; }
			if (unknown != null) TechniqueManualItem.inscribe(reader, 1, unknown.ordinal());
			helper.assertTrue(reader.getMainHandItem().getCount() == 2, "Not what one doesn't know");
			TechniqueManualItem.inscribe(reader, 0, Technique.CLONE.ordinal());
			ItemStack written = find(reader, ModItems.TECHNIQUE_MANUAL);
			helper.assertTrue(reader.getMainHandItem().is(ModItems.BLANK_MANUAL) && reader.getMainHandItem().getCount() == 1 && written != null
					&& TechniqueManualItem.technique(written) == Technique.CLONE
					&& "spell_reader".equals(written.getTag().getString("Author")), "One blank becomes Clone's manual, signed");
			ItemStack brush = reader.getOffhandItem();
			helper.assertTrue(!brush.isDamageableItem() || brush.getDamageValue() == 1, "The brush a use more worn");
			TechniqueManualItem.inscribe(reader, 1, spell.ordinal());
			helper.assertTrue(reader.getMainHandItem().is(ModItems.TECHNIQUE_MANUAL) && TechniqueManualItem.spell(reader.getMainHandItem()) == spell,
					"The last blank becomes the spell's manual in hand");
			// A Core Formation reader learns nothing from it still.
			helper.assertTrue(!TechniqueManualItem.learn(low, spell), "Written out or not, its realm stands");
		} finally {
			leave(made);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSectsKeepTheManualsInTheirHallsAndLibraries(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		LootTable hall = server.getLootData().getLootTable(SectPiece.MASTER_HALL_LOOT);
		LootTable library = server.getLootData().getLootTable(SectPiece.LIBRARY_LOOT);
		Vec3 at = helper.absoluteVec(new Vec3(1, 2, 1));
		int inHall = 0, inLibrary = 0, other = 0;
		for (int i = 0; i < 1000; i++) {
			for (ItemStack stack : hall.getRandomItems(new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, at).create(LootContextParamSets.CHEST))) {
				if (!stack.is(ModItems.TECHNIQUE_MANUAL)) continue;
				if (TechniqueManualItem.technique(stack) == Technique.CLONE) inHall++;
				else other++;
			}
			for (ItemStack stack : library.getRandomItems(new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, at).create(LootContextParamSets.CHEST))) {
				if (!stack.is(ModItems.TECHNIQUE_MANUAL)) continue;
				if (TechniqueManualItem.technique(stack) == Technique.CLONE) inLibrary++;
				else other++;
			}
		}
		helper.assertTrue(other == 0, "Every manual found holds a technique (" + other + " didn't)");
		helper.assertTrue(inHall >= 40 && inHall <= 130, "A master's hall chest keeps one now and then (" + inHall + " in 1000)");
		helper.assertTrue(inLibrary >= 20 && inLibrary <= 90, "A library chest, rarer (" + inLibrary + " in 1000)");
		helper.succeed();
	}

	// --- Clone ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void aCloneIsSplitOffPlayedAndDrawnBackIn(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "clone_maker", Realm.HEAVENLY_BEING, Stage.MID);
			PlayerCultivation own = CultivationManager.get(p);
			own.learnTechnique(Technique.CLONE);
			own.setQi(own.maxQi());
			p.setHealth(p.getMaxHealth());
			double qi = own.getQi(), pool = own.maxQi();
			float health = p.getMaxHealth();
			p.getInventory().add(new ItemStack(Items.DIAMOND, 3));
			Vec3 mainAt = p.position();

			Clones.act(p);
			CloneBodyEntity standing = body(helper, p, false);
			helper.assertTrue(Clones.hasClone(p) && !Clones.inClone(p) && standing != null, "A clone split off, standing beside them");
			helper.assertTrue(Math.abs(own.getQi() - qi * Clones.SUMMON_SHARE) < 1e-6, "Splitting it off took half their qi (" + own.getQi() + " of " + qi + ")");
			helper.assertTrue(Math.abs(standing.getMaxHealth() - health * Clones.STAT_SHARE) < 0.5, "Half their health (" + standing.getMaxHealth() + ")");
			helper.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 3, "Nothing they carry split off with it");

			Clones.act(p);
			PlayerCultivation clone = CultivationManager.get(p);
			helper.assertTrue(Clones.inClone(p) && clone == own && Clones.isCloneRecord(own),
					"Into the clone, played on their own record: what it does and learns is theirs");
			helper.assertTrue(clone.getRealm() == Realm.HEAVENLY_BEING && clone.getStage() == Stage.MID, "At their own realm and stage");
			helper.assertTrue(Math.abs(clone.maxQi() - pool * Clones.STAT_SHARE) < 1e-6 && Math.abs(clone.getQi() - qi * Clones.SUMMON_SHARE) < 1e-6,
					"Half the pool, holding the qi it took");
			helper.assertTrue(Math.abs(p.getMaxHealth() - health * Clones.STAT_SHARE) < 0.5, "Half their health, played (" + p.getMaxHealth() + ")");
			helper.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 0, "The clone carries nothing of the main body's");
			CloneBodyEntity sitting = body(helper, p, true);
			helper.assertTrue(sitting != null && sitting.position().distanceTo(mainAt) < 0.5 && Math.abs(sitting.getMaxHealth() - health) < 0.5,
					"The main body sits where it was, whole");
			helper.assertTrue(body(helper, p, false) == null, "The clone's stand-in gone: it is played");
			helper.assertTrue(Ability.CLONE.isUnlocked(clone), "The way back");
			SoulProjection.toggle(p);
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()), "A clone projects no soul");
			// What it spends is made up from the main body's pool; nothing is paid for its being out.
			double mainBefore = Clones.mainQi(p), spent = clone.maxQi() * 0.5;
			helper.assertTrue(Math.abs(mainBefore - qi * (1 - Clones.SUMMON_SHARE)) < 1e-6, "The main body's pool kept apart (" + mainBefore + ")");
			clone.setQi(clone.getQi() - spent);
			Clones.feed(p, clone, 0);
			helper.assertTrue(Math.abs(clone.getQi() - clone.maxQi()) < 1e-6 && Clones.mainQi(p) < mainBefore - spent * 0.5,
					"Made up from the main body (" + Clones.mainQi(p) + " of " + mainBefore + ")");
			double still = clone.getQi();
			Clones.feed(p, clone, 0);
			helper.assertTrue(clone.getQi() == still, "Nothing paid for being out");
			p.getInventory().add(new ItemStack(Items.EMERALD, 2));
			Vec3 cloneAt = p.position();
			double mainNow = Clones.mainQi(p), cloneNow = own.getQi();
			own.setAlignment(own.getAlignment() + 7); // a deed done in the clone (a kill, a word)
			int alignment = own.getAlignment();

			Clones.act(p);
			helper.assertTrue(!Clones.inClone(p) && Clones.hasClone(p) && !Clones.isCloneRecord(own), "Back in the main body, the clone out");
			helper.assertTrue(Math.abs(own.getQi() - mainNow) < 1e-6 && Math.abs(own.getClone().getDouble("Qi") - cloneNow) < 1e-6,
					"The main body's pool again, the clone's kept with it (" + own.getQi() + ")");
			helper.assertTrue(own.getAlignment() == alignment, "What was done in the clone stays done");
			helper.assertTrue(p.position().distanceTo(mainAt) < 0.5 && Math.abs(p.getMaxHealth() - health) < 0.5, "Where it sat, its own strength again");
			helper.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 3 && p.getInventory().countItem(Items.EMERALD) == 0, "What the main body carried, back");
			standing = body(helper, p, false);
			helper.assertTrue(standing != null && standing.position().distanceTo(cloneAt) < 0.5 && body(helper, p, true) == null,
					"The clone left standing where it was");

			Clones.act(p);
			helper.assertTrue(Clones.inClone(p) && p.getInventory().countItem(Items.EMERALD) == 2, "Into it again, its things as it left them");
			Clones.act(p);

			// Drawn back in: sneaking, the clone near.
			double before = own.getQi();
			p.setShiftKeyDown(true);
			Clones.act(p);
			p.setShiftKeyDown(false);
			helper.assertTrue(!Clones.hasClone(p) && !Clones.inClone(p) && body(helper, p, false) == null, "The clone drawn back in");
			helper.assertTrue(own.getQi() > before && p.getInventory().countItem(Items.EMERALD) == 2, "Its qi and its things theirs");
		} finally {
			leave(made);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aCloneSlainDissolvesTheMainBodySlainIsDeath(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "clone_slain", Realm.NASCENT_SOUL, Stage.LATE);
			PlayerCultivation own = CultivationManager.get(p);
			own.learnTechnique(Technique.CLONE);
			own.setQi(own.maxQi());
			Vec3 mainAt = p.position();
			// Killed in it, they wake in the main body; what the clone carried falls where it fell.
			Clones.act(p);
			Clones.act(p);
			p.getInventory().add(new ItemStack(Items.EMERALD, 2));
			Vec3 fell = p.position();
			int emeralds = lying(helper, fell, Items.EMERALD); // (counted from: the kept game-test world may have some lying there already)
			helper.assertTrue(!Clones.allowDeath(p, p.damageSources().generic()), "Killed in the clone, they don't die");
			helper.assertTrue(!Clones.inClone(p) && !Clones.hasClone(p) && p.position().distanceTo(mainAt) < 0.5, "They wake in the main body; the clone is gone");
			helper.assertTrue(lying(helper, fell, Items.EMERALD) - emeralds == 2 && p.getInventory().countItem(Items.EMERALD) == 0, "Its things fall where it fell");
			// A clone lost, another can't be split off until the cooldown has run (the user's pick).
			long now = p.server.overworld().getGameTime();
			helper.assertTrue(own.cloneReadyAt() >= now + Clones.REFORM_TICKS - 20, "A cooldown set (" + (own.cloneReadyAt() - now) + " ticks)");
			Clones.act(p);
			helper.assertTrue(!Clones.hasClone(p), "No new clone while it runs");
			own.setCloneReadyAt(now - 1); // its time run out
			// Overtaken by the Netherworld's yin in it (it plays their record), the clone's body gives way, not theirs.
			Clones.act(p);
			helper.assertTrue(Clones.hasClone(p), "Once it has run, a new clone splits off");
			Clones.act(p);
			own.netherworld().setYinCorruption(1.0);
			GhostSystem.overtakenByYin(p);
			helper.assertTrue(!Clones.inClone(p) && !Clones.hasClone(p) && !GhostSystem.isGhost(p) && own.netherworld().yinCorruption() == 0,
					"The yin overtakes the clone: it dissolves, its maker alive in the main body, the yin gone with it");
			helper.assertTrue(own.cloneReadyAt() > p.server.overworld().getGameTime(), "...and that is a clone lost too");
			own.setCloneReadyAt(0);
			// Slain where it stands, it dissolves, its things falling there.
			Clones.act(p);
			Clones.act(p);
			p.getInventory().add(new ItemStack(Items.GOLD_INGOT, 4));
			Clones.act(p);
			CloneBodyEntity standing = body(helper, p, false);
			helper.assertTrue(standing != null, "The clone stands");
			Vec3 stood = standing.position();
			int gold = lying(helper, stood, Items.GOLD_INGOT);
			standing.hurt(p.damageSources().generic(), 100000);
			helper.assertTrue(!Clones.hasClone(p) && lying(helper, stood, Items.GOLD_INGOT) - gold == 4, "Slain standing, it dissolves, its things falling there");
			helper.assertTrue(own.cloneReadyAt() > p.server.overworld().getGameTime(), "...a clone lost: the cooldown again");
			own.setCloneReadyAt(0);
			// The main body slain where it sits, its player dies, and the clone with them.
			p.getInventory().add(new ItemStack(Items.DIAMOND, 1));
			Clones.act(p);
			Clones.act(p);
			CloneBodyEntity sitting = body(helper, p, true);
			helper.assertTrue(Clones.inClone(p) && sitting != null, "In the clone, the main body sitting");
			sitting.hurt(p.damageSources().generic(), 100000);
			helper.assertTrue(p.isDeadOrDying() && !Clones.inClone(p) && !Clones.hasClone(p), "The main body slain: they die, and the clone dissolves");
		} finally {
			leave(made);
		}
		helper.succeed();
	}

	// --- Helpers ---

	/** A survival player in the player list (as FourAxisGameTests'), of {@code realm} {@code stage}, wielding it whole. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm, Stage stage) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		made.add(p);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, stage, 0);
		c.setLowerRealmBound(false);
		CultivationManager.refresh(p);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			if (Clones.inClone(p)) Clones.onDisconnect(p);
			Clones.dissolve(p.server, CultivationManager.own(p));
			CultivationManager.own(p).setClone(null);
			p.server.getPlayerList().remove(p);
		}
	}

	/** A heaven spell (of one element, not both paths) first cast past {@code rank}. */
	private static Spell spellAbove(int rank) {
		for (Spell spell : Spell.values()) if (spell.isHeaven() && !spell.isDual() && spell.realmNeeded() > rank) return spell;
		return null;
	}

	/** {@code owner}'s one body near the test's ground: their sitting main body ({@code sitting}) or their standing clone; null if none. */
	private static CloneBodyEntity body(GameTestHelper helper, ServerPlayer owner, boolean sitting) {
		AABB ground = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
		List<CloneBodyEntity> found = helper.getLevel().getEntitiesOfClass(CloneBodyEntity.class, ground,
				b -> b.isAlive() && b.isSitting() == sitting && owner.getUUID().equals(b.getOwnerId()));
		return found.isEmpty() ? null : found.get(0);
	}

	/** How many of {@code item} lie on the ground within two blocks of {@code at}. */
	private static int lying(GameTestHelper helper, Vec3 at, Item item) {
		int count = 0;
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(2))) {
			if (entity.getItem().is(item)) count += entity.getItem().getCount();
		}
		return count;
	}

	private static ItemStack find(ServerPlayer p, Item item) {
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) return p.getInventory().getItem(i);
		return null;
	}
}
