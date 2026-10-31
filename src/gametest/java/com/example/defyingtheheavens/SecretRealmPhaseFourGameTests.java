package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The secret realms' fourth phase (2026-10-31): the Jade Lake Mirage, the Ruins of a Fallen Holy Land and the Tomb of a Fallen Immortal:
 * who gets into each, from which world, and over which the sky's hold counts from the land; their lands from the world's seed (the lake's
 * isles joined, the landing's bridge never false; the shards over the void with their currents; the base and its eight halls, a Law to
 * each); a mirage gone at a touch; the reflection a copy of its owner; the Mirror turning a blow back; the formation's eyes; the Law seals;
 * the spreading decays; and a whole opening of each, laid, its formation broken, and shut.
 */
public class SecretRealmPhaseFourGameTests implements FabricGameTest {
	private static final SecretRealm MIRAGE = SecretRealm.JADE_LAKE_MIRAGE, RUINS = SecretRealm.FALLEN_HOLY_LAND, TOMB = SecretRealm.FALLEN_IMMORTAL_TOMB;

	/** A player in survival (a mock player answers that it is creative), out of the player list. */
	private static ServerPlayer survivor(GameTestHelper helper, String name) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
		p.setGameMode(GameType.SURVIVAL);
		return p;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachAdmitsItsLowestFromItsOwnWorld(GameTestHelper helper) {
		helper.assertTrue(!MIRAGE.mayEnter(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.MID)) && MIRAGE.mayEnter(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.LATE)),
				"The Mirage from Four Axis Late");
		helper.assertTrue(!RUINS.mayEnter(PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.MID)) && RUINS.mayEnter(PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.LATE)),
				"The Ruins from Sacred Vessel Late");
		helper.assertTrue(!TOMB.mayEnter(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.MID)) && TOMB.mayEnter(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.LATE)),
				"The Tomb from Entering Nirvana Late");
		helper.assertTrue(!MIRAGE.fliesFreely(PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.GRAND_PERFECTION))
				&& MIRAGE.fliesFreely(PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.EARLY)), "Over the Mirage, Sacred Vessel flies freely");
		helper.assertTrue(TOMB.fliesFreely(PlayerCultivation.rank(Realm.HEAVEN_IMMORTAL, Stage.EARLY)) && !TOMB.fliesFreely(PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.LATE)),
				"Over the Tomb, Heaven Immortal");
		helper.assertTrue(MIRAGE.home() == ModDimensions.UPPER_REALM && RUINS.home() == ModDimensions.GREAT_VOID && TOMB.home() == ModDimensions.IMMORTAL_REALM,
				"Their Seals in the Jade Firmament, the Great Void, the Immortal Realm");
		helper.assertTrue(!MIRAGE.floats() && RUINS.floats() && TOMB.floats() && MIRAGE.holdFloor() < 0 && RUINS.holdFloor() == FallenHolyLand.LEVEL_Y
				&& TOMB.holdFloor() == ImmortalTomb.BASE_Y, "The Ruins and the Tomb float, the sky's hold over their void from their land");
		// Over the void, the hold's ground is the land's level, never lower; elsewhere the land's top.
		BlockPos here = helper.absolutePos(new BlockPos(1, 1, 1));
		int ground = SecretRealms.groundUnder(helper.getLevel(), RUINS, here.getX(), here.getZ());
		helper.assertTrue(ground >= FallenHolyLand.LEVEL_Y, "Over the Ruins' void, held from its level: " + ground);
		helper.assertTrue(SecretRealms.groundUnder(helper.getLevel(), MIRAGE, here.getX(), here.getZ()) < FallenHolyLand.LEVEL_Y, "Over the Mirage, from the ground");
		Set<Integer> slots = new HashSet<>();
		for (SecretRealm realm : SecretRealm.values()) slots.add(realm.centre().getX());
		helper.assertTrue(slots.size() == SecretRealm.values().length, "No two in one slot");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theMirageIsLaidFromItsSeed(GameTestHelper helper) {
		long seed = helper.getLevel().getSeed();
		MirageLake.Shape s = MirageLake.shape(seed);
		helper.assertTrue(MirageLake.column(s, 0, 0).kind() == MirageLake.Kind.HEART, "The heart isle at its middle");
		helper.assertTrue(MirageLake.column(s, 300, 0).kind() == MirageLake.Kind.NONE, "Nothing past its edge");
		List<BlockPos> arrivals = MirageLake.arrivals(s);
		helper.assertTrue(arrivals.size() == 4, "Four arrivals");
		for (BlockPos at : arrivals) helper.assertTrue(MirageLake.column(s, at.getX(), at.getZ()).kind() == MirageLake.Kind.ISLE, "Each on an isle: " + at);
		List<int[]> bridges = MirageLakeWorks.bridges(s);
		helper.assertTrue(bridges.size() == s.isles.size(), "Every isle joined, to the heart in the end: " + bridges.size() + " of " + s.isles.size());
		for (int[] b : bridges) if (b[0] <= 1 && b[1] <= 1) helper.assertTrue(!MirageLakeWorks.isFalse(s, b), "The landing's bridge to the heart is never false");
		helper.assertTrue(MirageLakeWorks.falseSpan(0.5) && !MirageLakeWorks.falseSpan(0.1) && !MirageLakeWorks.falseSpan(0.9), "A false bridge's middle span");
		List<RealmWorks.Slot> guards = MIRAGE.works().guardianSlots(seed, MIRAGE.centre(), MIRAGE);
		helper.assertTrue(guards.size() >= 10 && guards.stream().allMatch(slot -> slot.type() == ModEntities.MIRROR_SHADE), "Mirror Shades keep it: " + guards.size());
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRuinsFloatOverTheVoid(GameTestHelper helper) {
		long seed = helper.getLevel().getSeed();
		FallenHolyLand.Shape s = FallenHolyLand.shape(seed);
		FallenHolyLand.Column heart = FallenHolyLand.column(s, 0, 0);
		helper.assertTrue(heart.kind() == FallenHolyLand.Kind.HEART && Math.abs(heart.ground() - FallenHolyLand.HEART_TOP) <= 1 && heart.bottom() < heart.ground() - 30,
				"The heart shard at its middle, hung deep: " + heart);
		// Somewhere between the heart and the eyes: the void.
		boolean voidSeen = false;
		for (int d = FallenHolyLand.HEART_R; d < FallenHolyLand.EYES_OUT && !voidSeen; d++) voidSeen = FallenHolyLand.column(s, d, 0).kind() == FallenHolyLand.Kind.NONE;
		helper.assertTrue(voidSeen, "The void between the shards");
		helper.assertTrue(s.eyes().size() == FallenHolyLandWorks.EYES, "Four eye shards");
		Set<BlockPos> eyes = new HashSet<>();
		for (int i = 0; i < FallenHolyLandWorks.EYES; i++) {
			BlockPos eye = FallenHolyLandWorks.eye(s, RUINS.centre(), i);
			eyes.add(eye);
			FallenHolyLand.Column col = FallenHolyLand.column(s, eye.getX() - RUINS.centre().getX(), eye.getZ() - RUINS.centre().getZ());
			helper.assertTrue(col.kind() == FallenHolyLand.Kind.EYE, "An eye on its shard: " + col);
			helper.assertTrue(FallenHolyLandWorks.eyeAt(s, RUINS.centre(), eye) == i, "Each eye known by its place");
		}
		helper.assertTrue(eyes.size() == FallenHolyLandWorks.EYES, "Each its own");
		List<FallenHolyLandWorks.Current> currents = FallenHolyLandWorks.currents(s, RUINS.centre());
		helper.assertTrue(currents.size() == 2 * (s.shards.size() - 1), "Two currents to each pair joined: " + currents.size());
		for (BlockPos at : FallenHolyLand.arrivals(s)) helper.assertTrue(FallenHolyLand.column(s, at.getX(), at.getZ()).kind() == FallenHolyLand.Kind.ARRIVAL, "An arrival's shard");
		long eyeGuards = RUINS.works().guardianSlots(seed, RUINS.centre(), RUINS).stream().filter(slot -> slot.id().startsWith("eye")).count();
		helper.assertTrue(eyeGuards == FallenHolyLandWorks.EYES, "A guard by each eye");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theTombHasAHallForEachLaw(GameTestHelper helper) {
		long seed = helper.getLevel().getSeed();
		ImmortalTomb.Shape s = ImmortalTomb.shape(seed);
		BlockPos c = TOMB.centre();
		helper.assertTrue(ImmortalTomb.column(s, 0, 0).kind() == ImmortalTomb.Kind.BASE && ImmortalTomb.column(s, 0, 0).ground() == ImmortalTomb.BASE_Y, "The court on the base");
		Set<Law> laws = new HashSet<>();
		for (int k = 0; k < ImmortalTombWorks.HALLS; k++) {
			laws.add(ImmortalTombWorks.lawOf(k));
			RealmWorks.Frame h = ImmortalTombWorks.lawHall(s, c, k);
			for (BlockPos at : ImmortalTombWorks.hallDoor(h)) {
				helper.assertTrue(ImmortalTomb.column(s, at.getX() - c.getX(), at.getZ() - c.getZ()).kind() == ImmortalTomb.Kind.BASE, "Hall " + k + "'s door on the base");
			}
		}
		helper.assertTrue(laws.size() == Law.values().length, "A hall for each Law: " + laws);
		RealmWorks.Frame t = ImmortalTombWorks.tomb(s, c);
		for (BlockPos at : ImmortalTombWorks.sanctumDoor(t)) {
			helper.assertTrue(ImmortalTomb.column(s, at.getX() - c.getX(), at.getZ() - c.getZ()).kind() == ImmortalTomb.Kind.BASE, "The sanctum on the base");
		}
		for (BlockPos at : ImmortalTomb.arrivals(s)) helper.assertTrue(ImmortalTomb.column(s, at.getX(), at.getZ()).kind() == ImmortalTomb.Kind.BASE, "An arrival on the base: " + at);
		long keepers = TOMB.works().guardianSlots(seed, c, TOMB).stream().filter(RealmWorks.Slot::gatekeeper).count();
		helper.assertTrue(keepers == 1, "One trial guardian, the immortal's spirit");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aMirageIsNothingAndGoesAtATouch(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos lotus = helper.absolutePos(new BlockPos(1, 2, 1)), span = helper.absolutePos(new BlockPos(2, 2, 1));
		level.setBlockAndUpdate(lotus, ModBlocks.MIRAGE_LOTUS.defaultBlockState());
		level.setBlockAndUpdate(span, ModBlocks.MIRAGE_JADE.defaultBlockState());
		helper.assertTrue(level.getBlockState(span).getCollisionShape(level, span, CollisionContext.empty()).isEmpty(), "A false span stops no foot");
		helper.assertTrue(!ModBlocks.JADE_STONE.defaultBlockState().getCollisionShape(level, span, CollisionContext.empty()).isEmpty(), "...the real one does");
		MirageBlock.dissolve(level, lotus, null);
		helper.assertTrue(level.getBlockState(lotus).isAir(), "A mirage lotus gone into mist at a touch");
		MirageBlock.dissolve(level, span, null);
		helper.assertTrue(level.getBlockState(span).isAir(), "...and anything a mirage is");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theReflectionIsItsOwnersCopy(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = survivor(helper, "owner"), other = survivor(helper, "other");
		CultivationManager.get(owner).setState(Realm.NASCENT_SOUL, Stage.MID, 0);
		owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		MirrorReflection whole = MirrorReflection.raise(level, owner, at, 0, false, -1);
		MirrorReflection half = MirrorReflection.raise(level, owner, at.add(2, 0, 0), 0, true, level.getGameTime() + 100);
		try {
			helper.assertTrue(whole != null && half != null, "Both raised");
			int rank = TwoPaths.higherRank(CultivationManager.get(owner), true);
			helper.assertTrue(whole.getRank() == rank && half.getRank() == rank - 2, "Its owner's rank (the waking mirror's two stages under): " + whole.getRank() + ", "
					+ half.getRank() + " of " + rank);
			helper.assertTrue(owner.getUUID().equals(whole.getOwnerId()) && whole.isTrial() && !half.isTrial(), "Its owner's, the mirror's trial");
			helper.assertTrue(whole.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "In its owner's armour");
			helper.assertTrue(half.getMaxHealth() < whole.getMaxHealth() * 0.6, "Half its strength: " + half.getMaxHealth() + " of " + whole.getMaxHealth());
			helper.assertTrue(!whole.livesItsLife() && !whole.shouldBeSaved() && !whole.shouldFlee(), "No life of its own, never saved, never fleeing");
			float before = whole.getHealth();
			helper.assertTrue(!whole.hurt(level.damageSources().playerAttack(other), 5) && whole.getHealth() == before, "Another's blow doesn't touch it");
			helper.assertTrue(whole.hurt(level.damageSources().playerAttack(owner), 5) && whole.getHealth() < before, "Its owner's does");
			helper.succeed();
		} finally {
			if (whole != null) whole.discard();
			if (half != null) half.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theMirrorTurnsBlowsBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer bearer = survivor(helper, "bearer");
		Zombie striker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		bearer.addEffect(new MobEffectInstance(ModEffects.MIRROR_RAISED, JadeLakeMirrorItem.RAISED_TICKS, JadeLakeMirrorItem.TURNS - 1));
		float strikerBefore = striker.getHealth();
		helper.assertTrue(!JadeLakeMirrorItem.allowDamage(bearer, level.damageSources().mobAttack(striker), 6), "The blow refused its bearer");
		helper.assertTrue(striker.getHealth() <= strikerBefore - 5.9f, "...and turned back on its striker: " + striker.getHealth());
		helper.assertTrue(bearer.getEffect(ModEffects.MIRROR_RAISED).getAmplifier() == JadeLakeMirrorItem.TURNS - 2, "A turn spent");
		for (int k = 1; k < JadeLakeMirrorItem.TURNS; k++) JadeLakeMirrorItem.allowDamage(bearer, level.damageSources().mobAttack(striker), 0.1f);
		helper.assertTrue(!bearer.hasEffect(ModEffects.MIRROR_RAISED), "Its turns spent, it rests");
		helper.assertTrue(JadeLakeMirrorItem.allowDamage(bearer, level.damageSources().mobAttack(striker), 1), "At rest, the blow lands");
		striker.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theFormationFallsWithItsFourEyes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SecretRealms.Record r = new SecretRealms.Record(RUINS);
		UUID anyone = UUID.randomUUID();
		helper.assertTrue(!FallenHolyLandWorks.fallen(r) && !RealmWorks.barrierLets(r, anyone), "Standing, its hall's door bars everyone");
		r.works.putInt("Broken", FallenHolyLandWorks.EYES);
		helper.assertTrue(FallenHolyLandWorks.fallen(r) && RealmWorks.barrierLets(r, anyone), "Its four eyes broken, the door lets anyone by");
		// An eye is guarded while a remnant stands near it.
		BlockPos eye = helper.absolutePos(new BlockPos(1, 2, 1));
		helper.assertTrue(!FallenHolyLandWorks.eyeGuarded(level, eye), "No guard near: unguarded");
		RealmGuardian guard = helper.spawn(ModEntities.HOLY_REMNANT, new BlockPos(2, 2, 2));
		helper.assertTrue(FallenHolyLandWorks.eyeGuarded(level, eye), "Its guard near: guarded");
		guard.discard();
		helper.assertTrue(!FallenHolyLandWorks.eyeGuarded(level, eye), "Its guard gone: open to a hand");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aLawsSealLetsItsHoldersBy(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper, "sealed");
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		level.setBlockAndUpdate(at, LawSealBlock.of(Law.FIRE));
		LawState laws = CultivationManager.get(p).laws();
		try {
			helper.assertTrue(!LawSealBlock.holds(p, Law.FIRE) && !level.getBlockState(at).getCollisionShape(level, at, CollisionContext.of(p)).isEmpty(), "Fire's seal bars one without it");
			laws.setLevel(Law.FIRE, 1);
			helper.assertTrue(LawSealBlock.holds(p, Law.FIRE) && level.getBlockState(at).getCollisionShape(level, at, CollisionContext.of(p)).isEmpty(), "...and lets one who holds it by");
			helper.assertTrue(!LawSealBlock.holds(p, Law.WATER), "Water's would not");
			laws.setLevel(Law.WATER, 1);
			laws.setLevel(Law.WOOD, 2);
			helper.assertTrue(ImmortalTombWorks.lawsHeld(p) == 3, "Three Laws held: " + ImmortalTombWorks.lawsHeld(p));
			SecretRealms.Record r = new SecretRealms.Record(TOMB);
			r.openedAt = level.getGameTime(); // (what one passes is kept for the opening under way)
			helper.assertTrue(!RealmWorks.barrierLets(r, p.getUUID()), "The sanctum bars one it doesn't know");
			RealmWorks.pass(r, "Laws", p.getUUID());
			helper.assertTrue(RealmWorks.barrierLets(r, p.getUUID()), "...and lets by one whose Laws it has felt");
			helper.succeed();
		} finally {
			for (Law law : Law.values()) laws.setLevel(law, 0);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSpreadingDecaysWeaken(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, "decaying");
		p.getAttribute(Attributes.ARMOR).setBaseValue(10); // (a worn piece counts only from the next tick)
		double armour = p.getAttributeValue(Attributes.ARMOR);
		helper.assertTrue(FiveDecays.outgoing(p, 10) == 10, "Whole, a blow whole");
		p.addEffect(new MobEffectInstance(ModEffects.DECAYING, 200));
		helper.assertTrue(Math.abs(FiveDecays.outgoing(p, 10) - 10 * FiveDecays.HARM_KEPT) < 1.0e-4, "Decaying, weaker: " + FiveDecays.outgoing(p, 10));
		helper.assertTrue(p.getAttributeValue(Attributes.ARMOR) < armour, "...and its armour: " + p.getAttributeValue(Attributes.ARMOR) + " of " + armour);
		p.removeEffect(ModEffects.DECAYING);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "secret_realm_phase_four_cycle", timeoutTicks = 1200)
	public void theFourthPhasesRealmsOpenAndClose(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		SecretRealms.inTests = true;
		try {
			SecretRealms.Data data = SecretRealms.Data.get(server);
			ServerLevel realms = server.getLevel(ModDimensions.SECRET_REALM);
			for (SecretRealm realm : List.of(MIRAGE, RUINS, TOMB)) {
				SecretRealms.Record r = data.of(realm);
				if (r.openedAt >= 0) SecretRealms.closeNow(server, realm);
				SecretRealms.openNow(server, realm);
				helper.assertTrue(r.openedAt >= 0 && r.laid && r.seal != null && r.sealBuilt, realm.getId() + ": open, its land laid and its Seal built");
				ServerLevel home = server.getLevel(realm.home());
				helper.assertTrue(r.seal.getY() > home.getMinBuildHeight() + 8, realm.getId() + "'s Seal on land, never the void's floor: " + r.seal);
			}
			// The Mirage: its great mirror.
			RealmWorks.Frame p = MirageLakeWorks.pavilion(MirageLake.shape(realms.getSeed()), MIRAGE.centre());
			for (BlockPos at : MirageLakeWorks.mirror(p)) helper.assertTrue(realms.getBlockState(at).is(ModBlocks.JADE_MIRROR), "The great mirror");
			helper.assertTrue(realms.getBlockState(p.at(0, 3, 1)).is(Blocks.GOLD_BLOCK), "...in its frame of gold");
			// The Ruins: its four eyes whole and the hall barred; broken, the formation falls and the door is open.
			FallenHolyLand.Shape rs = FallenHolyLand.shape(realms.getSeed());
			RealmWorks.Frame h = FallenHolyLandWorks.hall(rs, RUINS.centre());
			for (BlockPos at : FallenHolyLandWorks.doorway(h)) helper.assertTrue(realms.getBlockState(at).is(ModBlocks.TRIAL_BARRIER), "The hall's door barred");
			SecretRealms.Record rr = data.of(RUINS);
			for (int i = 0; i < FallenHolyLandWorks.EYES; i++) {
				BlockPos eye = FallenHolyLandWorks.eye(rs, RUINS.centre(), i);
				helper.assertTrue(realms.getBlockState(eye).is(ModBlocks.FORMATION_EYE), "Eye " + i + " whole on its dais");
				realms.setBlockAndUpdate(eye, Blocks.AIR.defaultBlockState());
				FallenHolyLandWorks.eyeBroken(realms, eye, null);
			}
			helper.assertTrue(FallenHolyLandWorks.fallen(rr), "All four broken: the formation fallen");
			for (BlockPos at : FallenHolyLandWorks.doorway(h)) helper.assertTrue(realms.getBlockState(at).isAir(), "...the hall's door open");
			// The Tomb: each hall sealed by its Law, the sanctum barred.
			ImmortalTomb.Shape ts = ImmortalTomb.shape(realms.getSeed());
			for (int k = 0; k < ImmortalTombWorks.HALLS; k++) {
				for (BlockPos at : ImmortalTombWorks.hallDoor(ImmortalTombWorks.lawHall(ts, TOMB.centre(), k))) {
					helper.assertTrue(realms.getBlockState(at).is(ModBlocks.LAW_SEAL) && LawSealBlock.law(realms.getBlockState(at)) == ImmortalTombWorks.lawOf(k),
							"Hall " + k + " sealed by its Law");
				}
			}
			for (BlockPos at : ImmortalTombWorks.sanctumDoor(ImmortalTombWorks.tomb(ts, TOMB.centre()))) {
				helper.assertTrue(realms.getBlockState(at).is(ModBlocks.TRIAL_BARRIER), "The sanctum's door barred");
			}
			for (SecretRealm realm : List.of(MIRAGE, RUINS, TOMB)) {
				SecretRealms.closeNow(server, realm);
				helper.assertTrue(data.of(realm).openedAt < 0, realm.getId() + " shut again");
			}
			helper.succeed();
		} finally {
			SecretRealms.inTests = false;
		}
	}
}
