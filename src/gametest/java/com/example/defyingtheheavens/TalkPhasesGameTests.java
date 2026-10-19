package com.example.defyingtheheavens;

import com.example.defyingtheheavens.mixin.ServerPlayerAccessor;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The talk system's later phases (see TalkSystem): what the wheel offers, being struck mid-talk, seclusion, sparring, rumours
 * and the journal, guidance, trading, tribute and release, mercy, a Sworn friend's aid, and spirit stones. Driven by hand on
 * survival test players (as in SoulProjectionGameTests); each test takes its players out of the world again.
 */
public class TalkPhasesGameTests implements FabricGameTest {
	private static int bits(TalkIntent... intents) {
		int mask = 0;
		for (TalkIntent intent : intents) mask |= intent.bit();
		return mask;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theWheelOffersWhatStandingAllows(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer player = cultivator(helper, made, "standing", Realm.CORE_FORMATION, Stage.EARLY);
			CultivatorNpc peer = npc(helper, new BlockPos(3, 2, 2), Realm.CORE_FORMATION, 0);
			int all = bits(TalkIntent.GREET, TalkIntent.ASK, TalkIntent.SPAR, TalkIntent.TRADE, TalkIntent.GIFT, TalkIntent.PROVOKE);
			helper.assertTrue(TalkSystem.intents(player, peer, TalkSystem.standing(player, peer)) == all, "An equal entertains all six");
			CultivatorNpc senior = npc(helper, new BlockPos(1, 2, 2), Realm.NASCENT_SOUL, 0);
			int above = TalkSystem.intents(player, senior, TalkSystem.Standing.ABOVE);
			helper.assertTrue((above & TalkIntent.SPAR.bit()) == 0 && (above & TalkIntent.TRADE.bit()) != 0, "A senior won't spar before Courteous");
			senior.changeRegard(player, Regard.COURTEOUS);
			helper.assertTrue((TalkSystem.intents(player, senior, TalkSystem.Standing.ABOVE) & TalkIntent.SPAR.bit()) != 0, "...and will after");
			CultivatorNpc far = npc(helper, new BlockPos(2, 2, 4), Realm.HEAVENLY_BEING, 0);
			helper.assertTrue(TalkSystem.intents(player, far, TalkSystem.standing(player, far)) == bits(TalkIntent.GREET, TalkIntent.ASK, TalkIntent.GIFT, TalkIntent.TRADE),
					"Two realms above: Greet, Ask, Gift and Trade (everyone trades, since 2026-10-19), no bouts or insults");
			CultivatorNpc commoner = npc(helper, new BlockPos(4, 2, 4), null, 0);
			helper.assertTrue((TalkSystem.intents(player, commoner, TalkSystem.standing(player, commoner)) & TalkIntent.SPAR.bit()) == 0,
					"No sparring with a commoner");
			// An enemy of its path: Provoke or Leave; two realms beneath you, it begs: Spare or Leave.
			CultivationManager.get(player).setAlignment(150);
			CultivatorNpc enemy = npc(helper, new BlockPos(2, 2, 0), Realm.CORE_FORMATION, -150);
			helper.assertTrue(TalkSystem.intents(player, enemy, TalkSystem.standing(player, enemy)) == bits(TalkIntent.PROVOKE, TalkIntent.LEAVE),
					"An enemy of its path only takes Provoke or Leave");
			CultivatorNpc weakEnemy = npc(helper, new BlockPos(0, 2, 2), null, -150);
			weakEnemy.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY));
			helper.assertTrue(TalkSystem.intents(player, weakEnemy, TalkSystem.standing(player, weakEnemy)) == bits(TalkIntent.SPARE, TalkIntent.LEAVE),
					"Two realms beneath you, it begs to be spared");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void beingStruckBreaksOffATalk(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer player = cultivator(helper, made, "struck", Realm.QI_REFINING, Stage.EARLY);
			CultivatorNpc npc = npc(helper, new BlockPos(3, 2, 2), Realm.QI_REFINING, 0);
			TalkSystem.open(player, npc);
			helper.assertTrue(npc.talkPartner() == player, "They talk");
			((ServerPlayerAccessor) player).dth$setSpawnInvulnerableTime(0); // a new player's grace would turn the blow aside
			player.hurt(helper.getLevel().damageSources().generic(), 1);
			helper.assertTrue(npc.talkPartner() == null, "A blow to the player breaks the talk off");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void disturbingSeclusionCostsRegardOnceADay(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc hermit = null;
		try {
			ServerPlayer player = cultivator(helper, made, "intruder", Realm.QI_REFINING, Stage.EARLY);
			hermit = npc(helper, new BlockPos(3, 2, 3), Realm.HEAVENLY_BEING, 0);
			hermit.enterSeclusion(helper.getLevel(), hermit.blockPosition());
			TalkSystem.open(player, hermit);
			int once = hermit.regard(player);
			TalkSystem.open(player, hermit);
			helper.assertTrue(once == Regard.SECLUSION_DISTURBED && hermit.regard(player) == once, "-5 the first time, nothing more that day");
			helper.assertTrue(hermit.talkPartner() == null, "And no talk");
			helper.succeed();
		} finally {
			leave(made);
			// Its Concealment Barrier down: left raised in the kept test world, it dispersed the monsters of whatever test came to
			// stand there in a later run (Formations#purge), a Hungry Ghost's own room among them.
			if (hermit != null) hermit.lowerConcealment(helper.getLevel());
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSparHoldsBackEndsAtTheLineAndRewards(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			MinecraftServer server = helper.getLevel().getServer();
			ServerPlayer player = cultivator(helper, made, "duellist", Realm.CORE_FORMATION, Stage.EARLY);
			CultivatorNpc senior = npc(helper, new BlockPos(3, 2, 2), Realm.NASCENT_SOUL, 0);
			float fullHealth = senior.getMaxHealth();
			player.setItemInHand(InteractionHand.MAIN_HAND, GinsengItem.create(ModItems.GINSENG, 600));
			TalkSystem.open(player, senior);
			SparSystem.start(player, senior, SparSystem.Terms.WAGER);
			helper.assertTrue(SparSystem.areSparring(player, senior) && player.getMainHandItem().isEmpty(), "The spar is on, the ginseng staked");
			helper.assertTrue(senior.getMaxHealth() < fullHealth && senior.isHoldingBack(), "The senior holds back to a Core Formation's strength");
			for (int i = 0; i < SparSystem.COUNTDOWN_TICKS; i++) SparSystem.tick(server);
			helper.assertTrue(senior.getTarget() == player, "Three breaths, and it comes at the player");
			// Nobody dies: a killing blow from the other stops at the line.
			helper.assertTrue(!SparSystem.allowDeath(player, helper.getLevel().damageSources().mobAttack(senior)) && player.getHealth() > 0,
					"A killing blow stops at the line");
			SparSystem.abandon(player); // that blow lost the spar; start again for the win
			senior.setHealth(senior.getMaxHealth());
			player.setHealth(player.getMaxHealth());
			player.setItemInHand(InteractionHand.MAIN_HAND, GinsengItem.create(ModItems.GINSENG, 600));
			helper.assertTrue(ModLang.SPAR_WAGERED.equals(SparSystem.refusal(player, senior, SparSystem.Terms.WAGER)), "One wager a day");
			SparSystem.start(player, senior, SparSystem.Terms.FRIENDLY);
			for (int i = 0; i < SparSystem.COUNTDOWN_TICKS; i++) SparSystem.tick(server);
			int regard = senior.regard(player);
			senior.setHealth(senior.getMaxHealth() * 0.5f);
			SparSystem.tick(server);
			helper.assertTrue(!SparSystem.isSparring(player) && !senior.isHoldingBack() && senior.getMaxHealth() == fullHealth,
					"Past its line, the spar ends and the senior is itself again");
			helper.assertTrue(senior.regard(player) == regard, "A second spar that day moves no regard (the forfeit already did)");
			helper.assertTrue(senior.getTarget() != player, "...and it bears no grudge");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	/**
	 * A weak cultivator sparring a far stronger player doesn't run from them (the user's Core Formation gate guard against their
	 * Heavenly Being: it ran the moment the bout began, the spar counted it as the player walking off, and it kept on running).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aWeakerSparPartnerStandsItsGround(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			MinecraftServer server = helper.getLevel().getServer();
			ServerPlayer player = cultivator(helper, made, "towering", Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);
			CultivatorNpc junior = npc(helper, new BlockPos(3, 2, 2), Realm.CORE_FORMATION, 0);
			// Out of a spar, a fight with one so far above it sends it running.
			junior.setTarget(player);
			junior.senseDomains(helper.getLevel());
			helper.assertTrue(junior.shouldFlee(), "Fighting a Heavenly Being for real, a Core Formation cultivator runs");
			// A spar it agreed to: it stops running, and doesn't start again once the bout is on.
			SparSystem.start(player, junior, SparSystem.Terms.FRIENDLY);
			helper.assertTrue(!junior.shouldFlee(), "The spar begins: it no longer runs from its partner");
			for (int i = 0; i < SparSystem.COUNTDOWN_TICKS; i++) SparSystem.tick(server);
			helper.assertTrue(junior.getTarget() == player, "...and comes at them");
			junior.senseDomains(helper.getLevel());
			helper.assertTrue(!junior.shouldFlee() && SparSystem.areSparring(player, junior), "...feeling their domain, it stands its ground");
			int regard = junior.regard(player);
			junior.setHealth(1);
			SparSystem.tick(server);
			helper.assertTrue(!SparSystem.isSparring(player) && junior.regard(player) > regard, "The spar is won, not forfeited");
			junior.senseDomains(helper.getLevel());
			helper.assertTrue(!junior.shouldFlee(), "...and over, it bears no fear of them either");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	/**
	 * One far stronger ends a spar with its first blow (half the partner's health in a stroke), and the next swing a
	 * moment later landed as an attack in earnest: the whole sect turned out and the barrier set its guest outside. Blows
	 * between the two just after the end are pulled (SparSystem#AFTERMATH_TICKS); afterwards, blows are blows again.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = SparSystem.AFTERMATH_TICKS + 40)
	public void theSwingJustAfterASparIsPulled(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer player = cultivator(helper, made, "eager", Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);
		CultivatorNpc junior = npc(helper, new BlockPos(3, 2, 2), Realm.CORE_FORMATION, 0);
		try {
			SparSystem.start(player, junior, SparSystem.Terms.FRIENDLY);
			for (int i = 0; i < SparSystem.COUNTDOWN_TICKS; i++) SparSystem.tick(server);
			junior.setHealth(1); // the winning blow
			SparSystem.tick(server);
			helper.assertTrue(!SparSystem.isSparring(player) && SparSystem.justSparred(player, junior), "The spar is won, a moment ago");
			int regard = junior.regard(player);
			float health = junior.getHealth();
			helper.assertTrue(!SparSystem.allowDamage(junior, player.damageSources().playerAttack(player))
					&& !junior.hurt(player.damageSources().playerAttack(player), 4) && junior.getHealth() == health, "The next swing is pulled");
			helper.assertTrue(junior.regard(player) == regard && junior.getTarget() != player, "...with no grudge and no fight for it");
			helper.assertTrue(!SparSystem.allowDamage(player, junior.damageSources().mobAttack(junior)), "...nor does its own last blow land");
			helper.assertTrue(SparSystem.allowDamage(junior, junior.damageSources().generic()), "Anything else still hurts");
		} catch (RuntimeException e) {
			leave(made);
			throw e;
		}
		helper.runAfterDelay(SparSystem.AFTERMATH_TICKS + 5, () -> {
			try {
				helper.assertTrue(!SparSystem.justSparred(player, junior) && SparSystem.allowDamage(junior, player.damageSources().playerAttack(player)),
						"Once the moment has passed, a blow is a blow");
				helper.succeed();
			} finally {
				leave(made);
			}
		});
	}

	/**
	 * One a realm or more beneath you won't wager (it can't win your stake, so its own was a thing a day for nothing): friendly
	 * bouts only. And in a spar with a sect's member, your stray blows on its fellows (a sweep, a spell's spread) are pulled, in
	 * the bout and the moment after: a friendly bout raises no alarm.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theWeakSparFriendlyOnlyAndStrayBlowsArePulled(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer player = cultivator(helper, made, "towering", Realm.NASCENT_SOUL, Stage.EARLY);
			player.setItemInHand(InteractionHand.MAIN_HAND, GinsengItem.create(ModItems.GINSENG, 600));
			CultivatorNpc junior = npc(helper, new BlockPos(3, 2, 2), Realm.CORE_FORMATION, 0);
			CultivatorNpc peer = npc(helper, new BlockPos(1, 2, 4), Realm.NASCENT_SOUL, 0);
			helper.assertTrue(ModLang.SPAR_BENEATH.equals(SparSystem.refusal(player, junior, SparSystem.Terms.WAGER)), "A realm beneath you: no wager");
			helper.assertTrue(SparSystem.refusal(player, junior, SparSystem.Terms.FRIENDLY) == null, "...a friendly bout, gladly");
			helper.assertTrue(SparSystem.refusal(player, peer, SparSystem.Terms.WAGER) == null, "An equal takes the wager");
			// A spar with a sect's member (all in this tick: the test's sect is on no rolls, and they'd leave it on their next second).
			Sect sect = new Sect(UUID.randomUUID());
			sect.dimension = helper.getLevel().dimension();
			sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
			junior.joinSect(sect, 0);
			CultivatorNpc fellow = npc(helper, new BlockPos(4, 2, 4), Realm.CORE_FORMATION, 0);
			fellow.joinSect(sect, 1);
			SparSystem.start(player, junior, SparSystem.Terms.FRIENDLY);
			net.minecraft.world.damagesource.DamageSource blow = player.damageSources().playerAttack(player);
			helper.assertTrue(SparSystem.allowDamage(junior, blow), "The partner takes the bout's blows");
			helper.assertTrue(!SparSystem.allowDamage(fellow, blow) && !fellow.hurt(blow, 4), "A stray blow on its fellow is pulled");
			helper.assertTrue(SparSystem.allowDamage(peer, blow), "...one on an outsider is not");
			SparSystem.abandon(player);
			helper.assertTrue(!SparSystem.allowDamage(fellow, blow), "...nor in the moment after the bout");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	/**
	 * At a quarter's line, one blow from an equal ended a spar (up to Nascent Soul a blow takes more than that). Now nobody starts
	 * a bout below three quarters of their health, its blows land at half strength, and it ends at half the health lost.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aSparLastsSeveralBlows(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			MinecraftServer server = helper.getLevel().getServer();
			ServerPlayer player = cultivator(helper, made, "duellist", Realm.NASCENT_SOUL, Stage.EARLY);
			CultivatorNpc partner = npc(helper, new BlockPos(3, 2, 2), Realm.NASCENT_SOUL, 0);
			CultivatorNpc bystander = npc(helper, new BlockPos(1, 2, 4), Realm.NASCENT_SOUL, 0);
			partner.setHealth(partner.getMaxHealth() * 0.7f);
			helper.assertTrue(ModLang.SPAR_HURT.equals(SparSystem.refusal(player, partner, SparSystem.Terms.FRIENDLY)), "Under three quarters of its health, no bout");
			partner.setHealth(partner.getMaxHealth());
			SparSystem.start(player, partner, SparSystem.Terms.FRIENDLY);
			for (int i = 0; i < SparSystem.COUNTDOWN_TICKS; i++) SparSystem.tick(server);
			helper.assertTrue(SparSystem.pulled(partner, player.damageSources().playerAttack(player), 10) == 10 * SparSystem.BLOW_SHARE
					&& SparSystem.pulled(bystander, player.damageSources().playerAttack(player), 10) == 10, "A blow on the partner is pulled; on anyone else, not");
			float before = partner.getHealth(), otherBefore = bystander.getHealth();
			partner.hurt(player.damageSources().playerAttack(player), 10);
			bystander.hurt(player.damageSources().playerAttack(player), 10);
			float pulled = before - partner.getHealth(), full = otherBefore - bystander.getHealth();
			helper.assertTrue(pulled > 0 && pulled < full, "...and lands softer (" + pulled + " against " + full + ")");
			SparSystem.tick(server);
			helper.assertTrue(SparSystem.isSparring(player), "One blow doesn't end it");
			float line = partner.getMaxHealth() * (1 - SparSystem.LINE_SHARE);
			partner.setHealth(line + 1);
			SparSystem.tick(server);
			helper.assertTrue(SparSystem.isSparring(player), "Short of half its health lost, the bout goes on");
			partner.setHealth(line - 0.5f);
			SparSystem.tick(server);
			helper.assertTrue(!SparSystem.isSparring(player) && partner.getHealth() >= line - 0.01f, "Past half, it is over, and its health stops at the line");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void winningAWagerTakesBothStakes(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			MinecraftServer server = helper.getLevel().getServer();
			ServerPlayer player = cultivator(helper, made, "gambler", Realm.CORE_FORMATION, Stage.EARLY);
			CultivatorNpc senior = npc(helper, new BlockPos(3, 2, 2), Realm.NASCENT_SOUL, 0);
			player.setItemInHand(InteractionHand.MAIN_HAND, GinsengItem.create(ModItems.GINSENG, 600));
			SparSystem.start(player, senior, SparSystem.Terms.WAGER);
			for (int i = 0; i < SparSystem.COUNTDOWN_TICKS; i++) SparSystem.tick(server);
			int regard = senior.regard(player);
			senior.setHealth(1);
			SparSystem.tick(server);
			helper.assertTrue(senior.regard(player) == regard + Regard.SPAR_WIN_SENIOR, "Beating a higher realm: +15");
			boolean ginseng = false;
			int things = 0;
			for (ItemStack stack : player.getInventory().items) {
				if (stack.isEmpty()) continue;
				things++;
				ginseng |= stack.is(ModItems.GINSENG);
			}
			helper.assertTrue(ginseng && things >= 2, "The stake comes back, with theirs");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void rumoursAreRealToldOnceAndNoted(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			MinecraftServer server = helper.getLevel().getServer();
			ServerPlayer player = cultivator(helper, made, "listener", Realm.QI_REFINING, Stage.EARLY);
			// A realm above (two would be too far above to share rumours).
			CultivatorNpc teller = npc(helper, new BlockPos(3, 2, 2), Realm.FOUNDATION_BUILDING, 0);
			BlockPos herbAt = new BlockPos(5, 2, 5);
			helper.setBlock(herbAt.below(), Blocks.GRASS_BLOCK);
			helper.setBlock(herbAt, ModBlocks.GINSENG);
			BlockPos absolute = helper.absolutePos(herbAt);
			((GinsengBlockEntity) helper.getLevel().getBlockEntity(absolute)).setAge(500);
			Rumours.tick(server);
			String id = "herb:" + helper.getLevel().dimension().location() + ":" + absolute.asLong();
			helper.assertTrue(Rumours.get(server).all().stream().anyMatch(r -> r.id.equals(id)), "The old ginseng is known of");
			teller.changeRegard(player, Regard.COURTEOUS);
			// Other tests' herbs and trees are in the index too (it is the world's): as if the teller had told of those already.
			java.util.Set<String> told = teller.regardEntry(player).toldRumours;
			Rumours.get(server).all().stream().map(r -> r.id).filter(other -> !other.equals(id)).forEach(told::add);
			helper.assertTrue(Rumours.untold(teller, told, 2).stream().anyMatch(r -> r.id.equals(id)), "The teller has heard of it");
			TalkSystem.open(player, teller);
			helper.assertTrue(teller.talkPartner() == player, "They talk");
			TalkSystem.topic(player, teller.getId(), TalkSystem.Topic.RUMOURS.ordinal());
			List<Journal.Entry> journal = CultivationManager.get(player).journal();
			helper.assertTrue(journal.stream().anyMatch(e -> e.id().equals(id)), "Told, and noted in the journal");
			helper.assertTrue(teller.regardEntry(player).toldRumours.contains(id), "...and not to be told again");
			PlayerCultivation loaded = PlayerCultivation.load(CultivationManager.get(player).save());
			helper.assertTrue(loaded.journal().size() == journal.size(), "The journal is saved");
			helper.destroyBlock(herbAt);
			Rumours.tick(server);
			helper.assertTrue(Rumours.get(server).all().stream().noneMatch(r -> r.id.equals(id)), "Dug up, it is forgotten");
			helper.assertTrue(Rumours.ageGuess(600, 0).getContents() instanceof TranslatableContents weak && weak.getKey().equals(ModLang.rumour("between"))
					&& weak.getArgs()[0].equals(350) && weak.getArgs()[1].equals(850), "A weak teller guesses wide: 350 to 850");
			helper.assertTrue(Rumours.ageGuess(600, 5).getContents() instanceof TranslatableContents strong && strong.getArgs()[0].equals(550)
					&& strong.getArgs()[1].equals(650), "A strong one close: 550 to 650");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aWarmSeniorGivesInsightOnceADay(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer player = cultivator(helper, made, "student", Realm.QI_REFINING, Stage.EARLY);
			CultivatorNpc teacher = npc(helper, new BlockPos(3, 2, 2), Realm.FOUNDATION_BUILDING, 0);
			teacher.changeRegard(player, Regard.WARM);
			TalkSystem.open(player, teacher);
			TalkSystem.topic(player, teacher.getId(), TalkSystem.Topic.GUIDANCE.ordinal());
			helper.assertTrue(player.hasEffect(ModEffects.INSIGHT), "Guidance: Insight");
			player.removeEffect(ModEffects.INSIGHT);
			TalkSystem.topic(player, teacher.getId(), TalkSystem.Topic.GUIDANCE.ordinal());
			helper.assertTrue(!player.hasEffect(ModEffects.INSIGHT), "Once a day from each teacher");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void cultivatorsTradeInSpiritStones(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer player = cultivator(helper, made, "trader", Realm.QI_REFINING, Stage.EARLY);
			CultivatorNpc elder = npc(helper, new BlockPos(3, 2, 2), Realm.CORE_FORMATION, 0);
			elder.setTitle(NpcTitle.ELDER);
			player.getInventory().add(GinsengItem.create(ModItems.GINSENG, 400));
			Map<MerchantOffer, String> keys = new IdentityHashMap<>();
			MerchantOffers cold = CultivatorTrades.offersFor(elder, player, new HashMap<>(), keys);
			boolean stonesOnly = cold.stream().filter(o -> keys.get(o).startsWith("sell")).allMatch(o -> o.getBaseCostA().getItem() instanceof SpiritStoneItem);
			boolean buys = cold.stream().anyMatch(o -> o.getBaseCostA().is(ModItems.GINSENG) && o.getResult().getItem() instanceof SpiritStoneItem);
			helper.assertTrue(stonesOnly && buys, "It sells for spirit stones and buys your ginseng");
			elder.changeRegard(player, Regard.TRUSTED);
			long lockedCold = cold.stream().filter(CultivatorTrades::isLocked).count();
			MerchantOffers warm = CultivatorTrades.offersFor(elder, player, new HashMap<>(), keys);
			long lockedWarm = warm.stream().filter(CultivatorTrades::isLocked).count();
			helper.assertTrue(lockedCold > 0 && lockedWarm < lockedCold && warm.size() - lockedWarm > cold.size() - lockedCold,
					"Regard opens better offers (those not yet open shown all along, locked)");
			ItemStack[] costs = CultivatorTrades.costsFor(15);
			helper.assertTrue(costs[0].is(ModItems.MID_SPIRIT_STONE) && costs[0].getCount() == 1 && costs[1].is(ModItems.LOW_SPIRIT_STONE)
					&& costs[1].getCount() == 6, "15 Low stones: one Mid and six Low");
			elder.openTrade(player);
			helper.assertTrue(player.containerMenu instanceof MerchantMenu && elder.getTradingPlayer() == player, "Trade opens the trading screen");
			player.closeContainer();
			helper.assertTrue(elder.getTradingPlayer() == null, "...and closing it ends the trade");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void pressingOneDownLetsYouDemandOrRelease(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			MinecraftServer server = helper.getLevel().getServer();
			ServerPlayer player = cultivator(helper, made, "oppressor", Realm.HEAVENLY_BEING, Stage.EARLY);
			PlayerCultivation c = CultivationManager.get(player);
			c.setAbilityEnabled(Ability.REALM_SUPPRESS, true);
			CultivatorNpc victim = npc(helper, new BlockPos(3, 2, 2), Realm.FOUNDATION_BUILDING, 0);
			RealmSuppressSystem.refreshNow();
			RealmSuppressSystem.tick(server);
			helper.assertTrue(TalkSystem.kneelsTo(player, victim), "Pressed two realms down, it kneels");
			TalkSystem.open(player, victim);
			helper.assertTrue(victim.isKneeling(), "...on one knee");
			int alignment = c.getAlignment();
			int regard = victim.regard(player);
			TalkSystem.intent(player, victim.getId(), TalkIntent.DEMAND.ordinal());
			helper.assertTrue(victim.regard(player) == regard + Regard.TRIBUTE && c.getAlignment() == alignment - 5, "Tribute: -20 regard, -5 alignment");
			helper.assertTrue(player.getInventory().items.stream().anyMatch(s -> !s.isEmpty()), "...and something of theirs");
			TalkSystem.open(player, victim);
			TalkSystem.intent(player, victim.getId(), TalkIntent.DEMAND.ordinal());
			helper.assertTrue(victim.regard(player) == regard + Regard.TRIBUTE, "Once a day");
			TalkSystem.open(player, victim);
			TalkSystem.intent(player, victim.getId(), TalkIntent.RELEASE.ordinal());
			RealmSuppressSystem.tick(server);
			helper.assertTrue(!RealmSuppressSystem.presses(player, victim) && !victim.isKneeling(), "Released, it stands");
			// The release is a mercy for that pressing, not a lasting pass: switched off and on again, it bears down on it anew.
			CultivationManager.toggleAbility(player, Ability.REALM_SUPPRESS.ordinal());
			RealmSuppressSystem.tick(server);
			CultivationManager.toggleAbility(player, Ability.REALM_SUPPRESS.ordinal());
			RealmSuppressSystem.tick(server);
			helper.assertTrue(c.isAbilityEnabled(Ability.REALM_SUPPRESS) && RealmSuppressSystem.presses(player, victim),
					"Switched off and on again, Realm Suppress presses the one it let up off");
			c.setAbilityEnabled(Ability.REALM_SUPPRESS, false);
			RealmSuppressSystem.refreshNow();
			RealmSuppressSystem.tick(server);
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBeatenBegAndTheSparedRemember(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer player = cultivator(helper, made, "victor", Realm.NASCENT_SOUL, Stage.EARLY);
			CultivationManager.get(player).setAlignment(150);
			CultivatorNpc foe = npc(helper, new BlockPos(3, 2, 2), Realm.FOUNDATION_BUILDING, -150);
			helper.assertTrue(TalkSystem.wouldBeg(foe, player), "Two realms beneath, run down, it would beg");
			TalkSystem.open(player, foe);
			helper.assertTrue(foe.isBeggingTo(player) && foe.isKneeling(), "An enemy two realms beneath begs on its knees");
			int regard = foe.regard(player);
			TalkSystem.intent(player, foe.getId(), TalkIntent.SPARE.ordinal());
			helper.assertTrue(foe.owesLife(player) && !foe.isEnemy(player) && foe.regard(player) == regard + Regard.SPARED, "Spared, it owes you its life");
			helper.assertTrue(CultivationManager.get(player).getAlignment() == 152, "...and your path brightens");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSwornFriendComesToYourAid(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer player = cultivator(helper, made, "befriended", Realm.QI_REFINING, Stage.EARLY);
			CultivatorNpc friend = npc(helper, new BlockPos(4, 2, 4), Realm.CORE_FORMATION, 0);
			friend.changeRegard(player, Regard.MAX);
			Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
			TalkSystem.onPlayerHurt(player, helper.getLevel().damageSources().mobAttack(zombie));
			helper.assertTrue(friend.getTarget() == zombie, "The friend turns on what struck you");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spiritStonesHoldQiAndValue(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer player = cultivator(helper, made, "absorber", Realm.QI_REFINING, Stage.EARLY);
			PlayerCultivation c = CultivationManager.get(player);
			c.setQi(0);
			ItemStack stones = new ItemStack(ModItems.LOW_SPIRIT_STONE, 3);
			player.setItemInHand(InteractionHand.MAIN_HAND, stones);
			ModItems.LOW_SPIRIT_STONE.finishUsingItem(stones, helper.getLevel(), player);
			helper.assertTrue(Math.abs(c.getQi() - SpiritStoneItem.QI_PER_VALUE) < 0.01 && stones.getCount() == 2, "A Low stone gives 25 qi, and is used up");
			helper.assertTrue(SpiritStoneItem.meditationMultiplier(player) == 1.0 + SpiritStoneItem.MEDITATION_BOOST, "Held, it speeds meditation");
			for (int i = 0; i < SpiritStoneItem.secondsPerStone(SpiritStoneItem.Grade.LOW); i++) SpiritStoneItem.drainWhileMeditating(player);
			helper.assertTrue(stones.getCount() == 1, "...until its qi is spent");
			List<ItemStack> hundred = SpiritStoneItem.stonesFor(100);
			helper.assertTrue(hundred.size() == 3 && hundred.get(0).is(ModItems.HIGH_SPIRIT_STONE) && hundred.get(1).getCount() == 2,
					"100 Low stones' worth: a High, two Mid and a Low");
			helper.assertTrue(helper.getLevel().getServer().getRecipeManager()
					.byKey(new ResourceLocation(DefyingTheHeavens.MOD_ID, "mid_spirit_stone_from_low")).isPresent(), "Nine Low craft into a Mid");
			helper.assertTrue(!CultivatorNpc.carriedStones(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), helper.getLevel().getRandom()).isEmpty(),
					"Cultivators carry spirit stones");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	// --- Helpers (as in SoulProjectionGameTests) ---

	/** A cultivator NPC of {@code realm} (Early; null for a commoner) and {@code alignment}, with no will of its own. */
	private static CultivatorNpc npc(GameTestHelper helper, BlockPos pos, Realm realm, int alignment) {
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, pos);
		npc.setRank(realm == null ? CultivatorNpc.MORTAL : PlayerCultivation.rank(realm, Stage.EARLY));
		npc.setAlignment(alignment);
		npc.setHealth(npc.getMaxHealth());
		return npc;
	}

	private static ServerPlayer cultivator(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm, Stage stage) {
		ServerPlayer p = survivor(helper, made, name);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, stage, 0);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		c.setQi(c.maxQi());
		return p;
	}

	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
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
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			SparSystem.abandon(p);
			p.server.getPlayerList().remove(p);
		}
	}
}
