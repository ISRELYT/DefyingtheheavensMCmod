package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.List;

/**
 * The Netherworld's inhabitants and content (phase 5): ghost cultivators and the Gate's keepers, Hungry Ghosts, the Peach Wood
 * Sword, the dead's talk and teaching, Meng Po's soup, the herbs, pills and Yin Jade, stele rumours, and NPC ascension from
 * the Nether. What needs the Netherworld's land (spawning there, the Terrace, the keepers' return) is checked in game (written when
 * the test server had no Netherworld).
 */
public class NetherworldInhabitantsGameTests implements FabricGameTest {
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(player).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(player);
		player.setHealth(player.getMaxHealth());
		return player;
	}

	/** A new player can't be hurt for its first three seconds (vanilla's spawn protection): this one can, at once. */
	private static void vulnerable(ServerPlayer player) {
		try {
			java.lang.reflect.Field field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			field.setAccessible(true);
			field.setInt(player, 0);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static GhostCultivatorNpc ghost(GameTestHelper helper, Realm realm) {
		GhostCultivatorNpc ghost = ModEntities.GHOST_CULTIVATOR.create(helper.getLevel());
		ghost.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
		ghost.becomeGhost(PlayerCultivation.rank(realm, Stage.EARLY), 0, helper.getLevel().getRandom());
		helper.getLevel().addFreshEntity(ghost);
		return ghost;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ghostCultivatorsAreTheDeadWithTheirOwnStories(GameTestHelper helper) {
		GhostCultivatorNpc young = ghost(helper, Realm.QI_REFINING);
		GhostCultivatorNpc teacher = ghost(helper, Realm.FOUNDATION_BUILDING);
		GhostCultivatorNpc elder = ghost(helper, Realm.NASCENT_SOUL);
		helper.assertTrue(young.isGhost() && young instanceof Ghostly && NpcLines.voice(young).equals("ghost"), "One of the dead, speaking as the dead");
		helper.assertTrue(young.getTitle() == NpcTitle.WANDERING_GHOST && teacher.getTitle() == NpcTitle.GHOST_ELDER && elder.getTitle() == NpcTitle.GHOST_KING,
				"A Wandering Ghost; from Foundation Building a Ghost Elder; from Core Formation a Ghost King");
		helper.assertTrue(young.method() == null && teacher.method() != null && teacher.method() == teacher.method(),
				"Below Foundation Building no method; from it one, always the same");
		helper.assertTrue(young.deathKey().equals(young.deathKey())
				&& NpcLines.english().containsKey(ModLang.npcSay("death." + young.deathKey() + ".1")), "How it died: always the same tale, and told in English");
		for (String death : GhostCultivatorNpc.DEATHS) {
			helper.assertTrue(NpcLines.english().containsKey(ModLang.npcSay("death." + death + ".1")), "A tale for " + death);
		}
		helper.assertTrue(young.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty(), "The dead are light, not cloth");
		helper.assertTrue(young.cultivation().hasHeavensLeave(), "The dead aren't held at Qi Refining Mid's gate");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadAreStruckByTheRulesForTheDead(GameTestHelper helper) {
		GhostCultivatorNpc ghost = ghost(helper, Realm.CORE_FORMATION);
		HungryGhost hungry = helper.spawn(ModEntities.HUNGRY_GHOST, new BlockPos(4, 2, 4));
		DamageSources sources = helper.getLevel().damageSources();
		ServerPlayer mortal = cultivator(helper, Realm.QI_REFINING);
		CultivationManager.get(mortal).setMortal(true);
		ServerPlayer deadPlayer = cultivator(helper, Realm.NASCENT_SOUL);
		try {
			for (net.minecraft.world.entity.LivingEntity dead : List.of(ghost, hungry)) {
				float before = dead.getHealth();
				dead.hurt(sources.lava(), 5);
				dead.hurt(sources.fall(), 5);
				Arrow arrow = new Arrow(helper.getLevel(), mortal);
				dead.hurt(sources.arrow(arrow, mortal), 5);
				dead.hurt(sources.playerAttack(mortal), 5);
				helper.assertTrue(dead.getHealth() == before, "Lava, falls, arrows and a mortal's fist pass through " + dead.getType().getDescriptionId());
			}
			helper.assertTrue(GhostSystem.reaches(sources.mobAttack(hungry)), "The dead's own blows reach the dead");
			mortal.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.PEACH_WOOD_SWORD));
			helper.assertTrue(GhostSystem.reaches(sources.playerAttack(mortal))
					&& GhostSystem.scaled(sources.playerAttack(mortal), 4) == 4 * PeachWoodSwordItem.ON_THE_DEAD,
					"A Peach Wood Sword lands in full on the dead, whoever swings it, and twice as hard");
			float before = hungry.getHealth();
			hungry.hurt(sources.playerAttack(mortal), 3);
			helper.assertTrue(hungry.getHealth() < before, "...even a mortal's");
			// A Hungry Ghost's hunger: heavy on the dead, light on the living but full of yin.
			vulnerable(mortal);
			vulnerable(deadPlayer);
			NetherworldState living = CultivationManager.get(mortal).netherworld();
			float mortalBefore = mortal.getHealth();
			hungry.doHurtTarget(mortal);
			// (The 5% yin it drives in is checked in game: mock players count as creative, and the yin passes the creative by.)
			helper.assertTrue(Math.abs(mortalBefore - mortal.getHealth() - HungryGhost.ON_THE_LIVING) < 0.01 && living.yinCorruption() >= 0,
					"A living one takes 1 (took " + (mortalBefore - mortal.getHealth()) + ")");
			GhostSystem.makeGhost(deadPlayer);
			mortal.invulnerableTime = 0;
			float ghostBefore = deadPlayer.getHealth();
			hungry.doHurtTarget(deadPlayer);
			helper.assertTrue(Math.abs(ghostBefore - deadPlayer.getHealth() - HungryGhost.ON_THE_DEAD) < 0.01, "A ghost takes 3");
			// A ghost who disperses one takes its ghostly qi.
			NetherworldState s = CultivationManager.get(deadPlayer).netherworld();
			s.setGhostQi(0);
			hungry.die(sources.playerAttack(deadPlayer));
			helper.assertTrue(Math.abs(s.ghostQi() - HungryGhost.QI_REWARD) < 1.0e-9, "The ghost that dispersed it takes in 5 ghostly qi");
		} finally {
			GhostSystem.forget(deadPlayer.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadTalkOnlyOfWhatTheDeadMayDo(GameTestHelper helper) {
		GhostCultivatorNpc teacher = ghost(helper, Realm.CORE_FORMATION);
		ServerPlayer living = cultivator(helper, Realm.CORE_FORMATION);
		ServerPlayer dead = cultivator(helper, Realm.CORE_FORMATION);
		try {
			GhostSystem.makeGhost(dead);
			int ghostMask = TalkSystem.intents(dead, teacher, TalkSystem.standing(dead, teacher));
			helper.assertTrue((ghostMask & ~TalkSystem.GHOST_INTENTS) == 0 && (ghostMask & TalkIntent.TRADE.bit()) != 0 && (ghostMask & TalkIntent.GIFT.bit()) != 0
					&& (TalkSystem.GHOST_INTENTS & TalkIntent.DEMAND.bit()) == 0,
					"A ghost may greet, ask, trade, give, spar, provoke or leave, with the spirit stones and slips it carries: never demand");
			// The mock players are creative, and a creative ghost keeps no rule (Incorporeal): the rules are asked of a survival one.
			dead.setGameMode(GameType.SURVIVAL);
			helper.assertTrue(Incorporeal.mayCarry(dead, new ItemStack(ModItems.LOW_SPIRIT_STONE)) && Incorporeal.mayCarry(dead, new ItemStack(ModItems.YIN_JADE))
					&& Incorporeal.mayUse(dead, GhostJadeSlipItem.of(GhostMethod.GHOST_FIRE_SCRIPTURE)) && Incorporeal.mayUse(dead, new ItemStack(ModItems.JADE_SLIP))
					&& !Incorporeal.mayCarry(dead, new ItemStack(net.minecraft.world.item.Items.DIAMOND)) && !Incorporeal.mayUse(dead, new ItemStack(ModItems.YIN_JADE))
					&& !Incorporeal.mayUse(living, GhostJadeSlipItem.of(GhostMethod.GHOST_FIRE_SCRIPTURE)),
					"The dead take up their money and writings, and read the slips; nothing else of the living world");
			helper.assertTrue((TalkSystem.intents(living, teacher, TalkSystem.standing(living, teacher)) & TalkIntent.TRADE.bit()) != 0,
					"The living trade with the dead");
			// Its teaching: its method, then (another day) ten minutes on the ghost path's bar.
			GhostMethod method = teacher.method();
			NetherworldState s = CultivationManager.get(living).netherworld();
			// Any foundation is taught to one with no ghost path at all: each carries it from Qi Refining (and begins the path).
			TalkSystem.teach(living, teacher);
			int learnedAt = s.layer(method);
			helper.assertTrue(s.knows(method) && learnedAt == GhostMethods.understoodTo(s, method),
					"It teaches its method, understood as far as the ghost path (" + method.getId() + ": " + learnedAt + ")");
			TalkSystem.teach(living, teacher);
			helper.assertTrue(s.layer(method) == learnedAt && s.pathCultivation() == 0, "...once a day");
			teacher.regardEntry(living).guidanceDay = -1;
			int before = GhostMethods.ghostRank(s);
			TalkSystem.teach(living, teacher);
			if (method.isFoundation()) {
				// Ten minutes: part way through Qi Refining Early (since 2026-10-19 its 1,047).
				helper.assertTrue(GhostMethods.ghostRank(s) == before && Math.abs(s.pathCultivation() - TalkSystem.TEACHING_PROGRESS) < 1.0e-9,
						"Known already: ten minutes on the ghost path's bar (" + GhostMethods.ghostRank(s) + ", " + s.pathCultivation() + ")");
			} else {
				helper.assertTrue(GhostMethods.ghostRank(s) == before, "Known already, with no ghost path begun: nothing to fill (" + method.getId() + ")");
			}
			// Meng Po's soup: for the dead.
			GhostGate.drinkSoup(living);
			helper.assertTrue(!s.memoryCleansed(), "The living are turned away");
			GhostGate.drinkSoup(dead);
			helper.assertTrue(CultivationManager.get(dead).netherworld().memoryCleansed(), "The dead drink, and will wake clean");
		} finally {
			GhostSystem.forget(dead.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGatesKeepersKeepTheirPlaces(GameTestHelper helper) {
		RandomSource random = helper.getLevel().getRandom();
		GhostCultivatorNpc ox = ModEntities.GHOST_CULTIVATOR.create(helper.getLevel());
		ox.becomeOfficial(GhostCultivatorNpc.Role.OX_HEAD, helper.absolutePos(new BlockPos(1, 2, 1)), random);
		GhostCultivatorNpc mengPo = ModEntities.GHOST_CULTIVATOR.create(helper.getLevel());
		mengPo.becomeOfficial(GhostCultivatorNpc.Role.MENG_PO, helper.absolutePos(new BlockPos(3, 2, 1)), random);
		helper.assertTrue(ox.getRank() == PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION) && ox.getTitle() == NpcTitle.GATE_WARDEN
				&& ox.hasCustomName(), "Ox-Head: Four Axis Grand Perfection, a Gate Warden, by name");
		helper.assertTrue(mengPo.getTitle() == NpcTitle.BRIDGE_KEEPER && mengPo.isInvulnerable() && !mengPo.isHostileTo(cultivator(helper, Realm.QI_REFINING)),
				"Meng Po keeps the bridge, fights nobody, and nobody harms her");
		// No one's path makes a foe of them: a devilish Heavenly Being once drew Meng Po off her post to cut it down (HostilityGoal).
		ServerPlayer devil = cultivator(helper, Realm.HEAVENLY_BEING);
		try {
			CultivationManager.get(devil).setAlignment(Alignment.MIN);
			helper.assertTrue(!mengPo.isEnemy(devil) && !ox.isEnemy(devil), "A devilish cultivator passing is no foe of the keepers");
			ox.setLastHurtByMob(devil);
			mengPo.setLastHurtByMob(devil);
			helper.assertTrue(ox.isEnemy(devil) && !mengPo.isEnemy(devil), "Ox-Head fights whoever strikes it; Meng Po not even then");
			helper.assertTrue(ox.holdsGround() && mengPo.holdsGround() && ox.makesLastStand() && !ghost(helper, Realm.QI_REFINING).holdsGround(),
					"The keepers never run from their posts; a wandering ghost may");
		} finally {
			helper.getLevel().getServer().getPlayerList().remove(devil);
		}
		helper.assertTrue(NpcLines.english().containsKey(ModLang.npcSay("warden.greet.1")) && NpcLines.english().containsKey(ModLang.npcSay("warden.warn.1"))
				&& NpcLines.english().containsKey(ModLang.npcSay("meng_po.call.1")), "Their words, in English");
		// Saved and loaded: still who they were.
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		ox.saveWithoutId(tag);
		GhostCultivatorNpc loaded = ModEntities.GHOST_CULTIVATOR.create(helper.getLevel());
		loaded.load(tag);
		helper.assertTrue(loaded.getRole() == GhostCultivatorNpc.Role.OX_HEAD, "Its role survives a save");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadSellTheirOwnAndTellOfSteles(GameTestHelper helper) {
		GhostCultivatorNpc elder = ghost(helper, Realm.FOUNDATION_BUILDING); // a Ghost Elder (from Core Formation, a king: its canon isn't sold)
		GhostCultivatorNpc young = ghost(helper, Realm.QI_REFINING);
		RandomSource random = RandomSource.create(1);
		boolean slip = CultivatorTrades.goodsFor(elder, random).stream().anyMatch(g -> g.item().is(ModItems.GHOST_JADE_SLIP));
		boolean youngSlip = CultivatorTrades.goodsFor(young, random).stream().anyMatch(g -> g.item().is(ModItems.GHOST_JADE_SLIP));
		helper.assertTrue(slip && !youngSlip, "A Ghost Elder sells its method's slip; a Wandering Ghost doesn't");
		helper.assertTrue(CultivatorTrades.goodsFor(young, random).stream().anyMatch(g -> g.item().is(ModItems.YIN_JADE)), "The dead sell Yin Jade");
		helper.assertTrue(TalkSystem.worth(new ItemStack(ModItems.YIN_JADE)) > 0 && TalkSystem.worth(GhostJadeSlipItem.of(GhostMethod.NETHERWORLD_BREATHING)) > 0
				&& TalkSystem.worth(GinsengItem.create(ModItems.OTHER_SHORE_FLOWER, 50)) > 0, "Yin Jade, slips and the herbs are worth something");
		// A stele the world placed: only the dead tell of it.
		CultivatorNpc living = ModEntities.CULTIVATOR.create(helper.getLevel());
		living.moveTo(elder.position());
		living.becomeRogue(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), 0, random);
		BlockPos foot = helper.absolutePos(new BlockPos(5, 1, 5));
		MethodSteleBlock.place(helper.getLevel(), foot, GhostMethod.GHOST_FIRE_SCRIPTURE, net.minecraft.core.Direction.NORTH);
		BlockPos stele = foot.above();
		Rumours.noteStele(helper.getLevel().dimension(), stele, GhostMethod.GHOST_FIRE_SCRIPTURE);
		Rumours.tick(helper.getLevel().getServer());
		boolean deadTell = Rumours.untold(elder, new HashSet<>(), 20).stream().anyMatch(r -> r.kind == Rumours.Kind.STELE && r.pos.equals(stele));
		boolean livingTell = Rumours.untold(living, new HashSet<>(), 20).stream().anyMatch(r -> r.kind == Rumours.Kind.STELE);
		helper.assertTrue(deadTell && !livingTell, "The dead tell of the steles; the living know nothing of them");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theNetherworldsHerbsAndPillsWork(GameTestHelper helper) {
		BlockPos soil = helper.absolutePos(new BlockPos(1, 1, 1));
		helper.getLevel().setBlock(soil, Blocks.SOUL_SOIL.defaultBlockState(), 3);
		helper.assertTrue(ModBlocks.OTHER_SHORE_FLOWER.defaultBlockState().canSurvive(helper.getLevel(), soil.above()), "The Other-Shore Flower roots in soul soil");
		helper.getLevel().setBlock(soil, Blocks.SOUL_SAND.defaultBlockState(), 3);
		helper.assertTrue(ModBlocks.OTHER_SHORE_FLOWER.defaultBlockState().canSurvive(helper.getLevel(), soil.above()), "...and soul sand");
		helper.getLevel().setBlock(soil, Blocks.STONE.defaultBlockState(), 3);
		helper.assertTrue(!ModBlocks.OTHER_SHORE_FLOWER.defaultBlockState().canSurvive(helper.getLevel(), soil.above()), "...not bare stone");
		helper.assertTrue(GinsengBlock.plantFor(ModItems.OTHER_SHORE_FLOWER) == ModBlocks.OTHER_SHORE_FLOWER
				&& GinsengBlock.plantFor(ModItems.NETHERWORLD_LOTUS) == ModBlocks.NETHERWORLD_LOTUS, "Both age like the other herbs, and sit on pedestals");
		helper.assertTrue(ModBlockEntities.GINSENG.isValid(ModBlocks.OTHER_SHORE_FLOWER.defaultBlockState())
				&& ModBlockEntities.GINSENG.isValid(ModBlocks.NETHERWORLD_LOTUS.defaultBlockState()), "...with the herbs' age clock");
		helper.assertTrue(AlchemyRecipes.forOutput(ModItems.YANG_PRESERVATION_PILL) != null && AlchemyRecipes.forOutput(ModItems.YIN_GATHERING_PILL) != null,
				"Both pills can be brewed");
		ServerPlayer player = cultivator(helper, Realm.NASCENT_SOUL);
		NetherworldState s = CultivationManager.get(player).netherworld();
		long now = helper.getLevel().getGameTime();
		ItemStack yang = PillItem.create(ModItems.YANG_PRESERVATION_PILL, PillGrade.LOW, 1);
		yang.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(s.yinPausedUntil() >= now + PillItem.preservationTicks(yang) - 1 && player.hasEffect(ModEffects.YANG_PRESERVATION),
				"The Yang Preservation Pill holds the yin still (3 minutes a point of potency)");
		ItemStack yin = PillItem.create(ModItems.YIN_GATHERING_PILL, PillGrade.LOW, 1);
		yin.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(!player.hasEffect(ModEffects.YIN_GATHERING), "No ghost path begun: the Yin Gathering Pill is refused");
		s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 1, 0);
		s.setPath(0, 0);
		yin.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(player.hasEffect(ModEffects.YIN_GATHERING) && Math.abs(s.methodBoost() - PillItem.yinBoost(yin)) < 1.0e-9,
				"Begun: +25% a point of potency to the ghost path's bar");
		// The Netherworld's worldgen is all there (the registries are read even without its dimension).
		var placed = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
		for (var key : List.of(ModPlacedFeatures.NW_ORE_YIN_JADE, ModPlacedFeatures.NW_ORE_YIN_JADE_PEAKS, ModPlacedFeatures.NW_OTHER_SHORE_FLOWER,
				ModPlacedFeatures.NW_OTHER_SHORE_FLOWER_FIELDS, ModPlacedFeatures.NW_NETHERWORLD_LOTUS, ModPlacedFeatures.OTHER_SHORE_FLOWER_NETHER,
				ModPlacedFeatures.ORE_YIN_JADE_NETHER)) {
			helper.assertTrue(placed.containsKey(key), "Placed feature " + key.location());
		}
		var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		helper.assertTrue(biomes.getOrThrow(ModBiomes.BONE_WASTES).getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap().stream()
				.anyMatch(data -> data.type == ModEntities.HUNGRY_GHOST && data.minCount == 2 && data.maxCount == 4), "Hungry Ghosts in packs of 2-4 in the Bone Wastes");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anNpcAscendingFromTheNetherBecomesOneOfTheDead(GameTestHelper helper) {
		helper.assertTrue(NpcTravel.ascentRift(Level.NETHER) == Level.NETHER && NpcTravel.ascentDestination(Level.NETHER) == ModDimensions.NETHERWORLD,
				"From the Nether, to its own rift, and up the Soul River to the Netherworld");
		helper.assertTrue(NpcTravel.ascentRift(Level.OVERWORLD) == Level.OVERWORLD && NpcTravel.ascentDestination(Level.OVERWORLD) == ModDimensions.UPPER_REALM,
				"From the Overworld, as ever, to the Upper Realm");
		helper.assertTrue(NpcTravel.ascentRift(Level.END) == Level.OVERWORLD, "From the End, by the Overworld's rift");
		CultivatorNpc living = ModEntities.CULTIVATOR.create(helper.getLevel());
		living.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), -150, helper.getLevel().getRandom());
		living.setSkin(3);
		GhostCultivatorNpc ghost = GhostCultivatorNpc.shedBody(living, helper.getLevel());
		helper.assertTrue(ghost.getUUID().equals(living.getUUID()) && ghost.getPersonalName().equals(living.getPersonalName()),
				"The same soul: its name carries over");
		helper.assertTrue(ghost.getRank() == living.getRank() && ghost.getAlignment() == living.getAlignment() && ghost.getSkin() == 3,
				"...its strength, its path, its face");
		helper.assertTrue(ghost.isGhost() && ghost.getTitle() == NpcTitle.GHOST_KING && ghost.getRole() == GhostCultivatorNpc.Role.GHOST_KING,
				"...one of the dead now, and so strong a one a Ghost King");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void theSpellsHarmTheDeadsKindAsTheyHarmGhosts(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.NASCENT_SOUL);
		NetherworldState s = CultivationManager.get(player).netherworld();
		HungryGhost wailed = helper.spawn(ModEntities.HUNGRY_GHOST, new BlockPos(3, 2, 3));
		HungryGhost burning = helper.spawn(ModEntities.HUNGRY_GHOST, new BlockPos(12, 2, 12));
		wailed.setNoAi(true);
		burning.setNoAi(true);
		float whole;
		try {
			s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 5, 0);
			s.setGhostQi(100);
			player.teleportTo(wailed.getX() + 2, wailed.getY(), wailed.getZ());
			whole = wailed.getHealth();
			Spells.cast(player, Spell.GHOSTLY_WAIL);
		} catch (RuntimeException e) {
			Spells.forget(player.getUUID());
			throw e;
		}
		// Its cry breaks out once its ghost has risen over its caster.
		helper.runAfterDelay(Spells.WAIL_RISES + 2, () -> {
			Spells.forget(player.getUUID());
			helper.assertTrue(Math.abs(whole - wailed.getHealth() - Spells.WAIL_DEAD_DAMAGE) < 0.01,
					"The wail harms a Hungry Ghost as it harms a ghost (took " + (whole - wailed.getHealth()) + ")");
		});
		burning.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.GHOST_FIRE, 40));
		float before = burning.getHealth();
		helper.runAfterDelay(25, () -> {
			helper.assertTrue(before - burning.getHealth() >= Spells.GHOST_FIRE_DAMAGE * Spells.GHOST_FIRE_DEAD_FACTOR - 0.01,
					"Ghost fire burns the dead's kind three times over (took " + (before - burning.getHealth()) + ")");
			helper.succeed();
		});
	}

	/** Entity#getMovementEmission, protected: what a walker's steps give off (sounds, game events, both, neither). */
	private static net.minecraft.world.entity.Entity.MovementEmission emission(net.minecraft.world.entity.Entity entity) {
		try {
			java.lang.reflect.Method method = net.minecraft.world.entity.Entity.class.getDeclaredMethod("getMovementEmission");
			method.setAccessible(true);
			return (net.minecraft.world.entity.Entity.MovementEmission) method.invoke(entity);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadTreadWithoutASound(GameTestHelper helper) {
		ServerPlayer living = cultivator(helper, Realm.QI_REFINING);
		ServerPlayer dead = cultivator(helper, Realm.QI_REFINING);
		try {
			living.setOnGround(true);
			dead.setOnGround(true);
			GhostSystem.makeGhost(dead);
			helper.assertTrue(emission(living).emitsSounds(), "The living's steps are heard");
			helper.assertTrue(!emission(dead).emitsSounds() && emission(dead).emitsAnything(),
					"A ghost's aren't (still counted as walked, for the view's bobbing)");
			helper.assertTrue(!dead.causeFallDamage(10, 1, helper.getLevel().damageSources().fall()), "Nor does it land with a thud");
			HungryGhost hungry = helper.spawn(ModEntities.HUNGRY_GHOST, new BlockPos(4, 2, 4));
			GhostCultivatorNpc ghost = ghost(helper, Realm.CORE_FORMATION);
			helper.assertTrue(!emission(hungry).emitsAnything() && !emission(ghost).emitsAnything(), "The dead's own kind tread silently too");
		} finally {
			GhostSystem.forget(dead.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGhostQuellingTalismanStrikesAndBindsOnlyTheDead(GameTestHelper helper) {
		ServerPlayer thrower = cultivator(helper, Realm.FOUNDATION_BUILDING);
		ServerPlayer bystander = cultivator(helper, Realm.QI_REFINING);
		HungryGhost hungry = helper.spawn(ModEntities.HUNGRY_GHOST, new BlockPos(4, 2, 4));
		hungry.setNoAi(true);
		GhostQuellingTalismanEntity talisman = new GhostQuellingTalismanEntity(helper.getLevel(), thrower);
		try {
			java.lang.reflect.Method canHit = GhostQuellingTalismanEntity.class.getDeclaredMethod("canHitEntity", net.minecraft.world.entity.Entity.class);
			canHit.setAccessible(true);
			helper.assertTrue((Boolean) canHit.invoke(talisman, hungry) && !(Boolean) canHit.invoke(talisman, bystander),
					"It flies at the dead and past the living");
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		vulnerable(bystander);
		float living = bystander.getHealth();
		helper.assertTrue(!GhostQuellingTalismanItem.strike(bystander, talisman, thrower) && bystander.getHealth() == living
				&& !bystander.hasEffect(ModEffects.QUELLED), "The living are nothing to it");
		float before = hungry.getHealth();
		helper.assertTrue(GhostQuellingTalismanItem.strike(hungry, talisman, thrower), "It strikes a Hungry Ghost");
		helper.assertTrue(Math.abs(before - hungry.getHealth() - GhostQuellingTalismanItem.DAMAGE) < 0.01,
				"6 of the dead's harm, in full (took " + (before - hungry.getHealth()) + ")");
		helper.assertTrue(hungry.hasEffect(ModEffects.QUELLED) && hungry.getEffect(ModEffects.QUELLED).getDuration() == GhostQuellingTalismanItem.BIND_TICKS
				&& hungry.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0, "...and binds it: 5 s, no speed");
		// A bound ghost can't dash its way free.
		ServerPlayer ghost = cultivator(helper, Realm.NASCENT_SOUL);
		try {
			GhostSystem.makeGhost(ghost);
			NetherworldState s = CultivationManager.get(ghost).netherworld();
			s.setMethod(GhostMethod.WANDERING_SOUL_STEPS, 2, 0);
			s.setGhostQi(50);
			GhostQuellingTalismanItem.quell(ghost);
			Spells.cast(ghost, Spell.SOUL_DASH);
			helper.assertTrue(s.ghostQi() == 50, "Bound, a ghost can't Soul Dash (nothing spent)");
		} finally {
			Spells.forget(ghost.getUUID());
			GhostSystem.forget(ghost.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGatesKeepersStandThreeTimesAMansHeight(GameTestHelper helper) {
		for (GhostCultivatorNpc.Role role : new GhostCultivatorNpc.Role[] {GhostCultivatorNpc.Role.OX_HEAD, GhostCultivatorNpc.Role.HORSE_FACE,
				GhostCultivatorNpc.Role.MENG_PO}) {
			GhostCultivatorNpc keeper = ModEntities.GHOST_CULTIVATOR.create(helper.getLevel());
			keeper.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
			keeper.becomeOfficial(role, helper.absolutePos(new BlockPos(2, 2, 2)), helper.getLevel().getRandom());
			helper.assertTrue(keeper.getScale() == GhostCultivatorNpc.KEEPER_SCALE && Math.abs(keeper.getBbHeight() - 5.4f) < 0.01f
					&& Math.abs(keeper.getBbWidth() - 1.8f) < 0.01f && keeper.getEyeHeight() > 4.5f,
					role.getId() + " is three times a man (" + keeper.getBbWidth() + " x " + keeper.getBbHeight() + ")");
		}
		GhostCultivatorNpc plain = ghost(helper, Realm.FOUNDATION_BUILDING);
		helper.assertTrue(plain.getScale() == 1.0f && Math.abs(plain.getBbHeight() - 1.8f) < 0.01f, "Any other ghost a man's size");
		GhostCultivatorNpc king = ghost(helper, Realm.NASCENT_SOUL);
		helper.assertTrue(king.getScale() == GhostCultivatorNpc.KING_SCALE && Math.abs(king.getBbHeight() - 2.7f) < 0.01f, "A Ghost King half again as tall");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyRecipeOfTheModIsInTheRecipeBook(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.QI_REFINING);
		ModRecipeBook.unlock(player);
		int ours = 0;
		for (net.minecraft.world.item.crafting.Recipe<?> recipe : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
			if (!recipe.getId().getNamespace().equals(DefyingTheHeavens.MOD_ID) || recipe.isSpecial()) continue;
			ours++;
			helper.assertTrue(player.getRecipeBook().contains(recipe), "Known: " + recipe.getId());
		}
		helper.assertTrue(ours >= 26, "All of them (" + ours + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theWardensWearTheirBeastsHeads(GameTestHelper helper) {
		helper.assertTrue(GhostCultivatorNpc.Role.OX_HEAD.beastHead() == BeastHead.OX && GhostCultivatorNpc.Role.HORSE_FACE.beastHead() == BeastHead.HORSE,
				"Ox-Head an ox's, Horse-Face a horse's");
		helper.assertTrue(GhostCultivatorNpc.Role.MENG_PO.beastHead() == null && GhostCultivatorNpc.Role.NONE.beastHead() == null, "Everyone else their own");
		for (BeastHead head : BeastHead.values()) helper.assertTrue(BeastHead.byId(head.getId()) == head, "Known by its id: " + head.getId());
		helper.succeed();
	}
}
