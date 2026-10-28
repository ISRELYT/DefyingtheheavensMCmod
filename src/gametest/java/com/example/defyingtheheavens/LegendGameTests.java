package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * The legendary weapons (phase 3a of the Artifact Forge, 2026-10-28; Legend, Legends, LegendaryWeaponItem, LeiZhen, SwordSpirit): one true
 * stack of each, hollow echoes powerless; too heavy below its realm; bonded and woken by realm; Ganjiang's fire, Moye's cold, both at once;
 * Heaven's Brand's bolts on a player's tribulations (theirs to lay alone, capped), the spear drinking its owner's bolts and the strength it
 * keeps for them; a stone's sword drawn only when it may be; mad Lei Zhen at the fourteenth realm, his way kept clear of the realm's people;
 * the tomb's spirits and Twin Dragons; the rumours told of them. Phase 3b's: the Xuanyuan Sword drawn by the righteous alone, Chiyou's shade
 * risen against its drawer, the sword leaving the devilish; the Golden Crow keeping the Sun-Shooting Bow, the bow's arrows of sun qi and its
 * Falling Sun; the Sea-Fixing Needle's pillar, its weight and its keeping behind the ear, Ao Guang; the vast arts landing at their moment;
 * where the three rest.
 */
public class LegendGameTests implements FabricGameTest {
	/** A player who isn't creative (a mock player says it is), of {@code realm}, with a full pool. */
	private static ServerPlayer survivor(GameTestHelper helper, String name, Realm realm, double x, double z) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		p.teleportTo(helper.getLevel(), origin.getX() + x, origin.getY() + 2, origin.getZ() + z, -90, 0);
		return p;
	}

	private static void leave(ServerPlayer... players) {
		for (ServerPlayer p : players) {
			if (p != null && p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
		}
	}

	private static Husk husk(GameTestHelper helper, double x, double z) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		Husk husk = EntityType.HUSK.create(level);
		husk.moveTo(origin.getX() + x, origin.getY() + 2, origin.getZ() + z, 0, 0);
		husk.setNoAi(true);
		husk.setPersistenceRequired();
		level.addFreshEntity(husk);
		return husk;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void oneTrueStackTheRestEchoes(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		ItemStack first = Legends.issue(server, Legend.GANJIANG);
		helper.assertTrue(Legends.isTrue(server, first), "A legend just made is the true one");
		ItemStack second = Legends.issue(server, Legend.GANJIANG);
		helper.assertTrue(Legends.isTrue(server, second) && !Legends.isTrue(server, first), "Made again, the older stack is a hollow echo");
		helper.assertFalse(Legends.isTrue(server, new ItemStack(ModItems.legend(Legend.GANJIANG))), "One from the creative menu is an echo");
		helper.assertTrue(ModItems.legend(Legend.TRIBULATION_SPEAR).isFireResistant() && !ModItems.legend(Legend.MOYE).canBeDepleted(),
				"A legend neither burns nor wears out");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tooHeavyBelowItsRealmWokenAbove(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer weak = survivor(helper, "test-legend-weak", Realm.QI_REFINING, 1.5, 1.5);
		ServerPlayer strong = survivor(helper, "test-legend-strong", Realm.HEAVENLY_BEING, 2.5, 1.5);
		try {
			ItemStack sword = Legends.issue(level.getServer(), Legend.MOYE);
			weak.setItemSlot(EquipmentSlot.MAINHAND, sword);
			sword.inventoryTick(level, weak, weak.getInventory().selected, true);
			helper.assertTrue(weak.getMainHandItem().isEmpty() && !LegendaryWeaponItem.carried(weak, Legend.MOYE).isEmpty(),
					"Below Core Formation, Moye slips from the hand into the pack");
			// Its stages: none below its realm, the first at it and a realm on, the second two on, the third from four on.
			helper.assertTrue(Legend.MOYE.stage(Realm.QI_REFINING) == 0 && Legend.MOYE.stage(Realm.CORE_FORMATION) == 1
					&& Legend.MOYE.stage(Realm.NASCENT_SOUL) == 1 && Legend.MOYE.stage(Realm.HEAVENLY_BEING) == 2
					&& Legend.MOYE.stage(Realm.DAO_INTEGRATION) == 3 && Legend.TRIBULATION_SPEAR.stage(Realm.ENTERING_NIRVANA) == 0,
					"Stages by realm");
			ItemStack held = Legends.issue(level.getServer(), Legend.GANJIANG);
			strong.setItemSlot(EquipmentSlot.MAINHAND, held);
			helper.assertTrue(LegendaryWeaponItem.powers(held, strong) == 0, "Unbonded, its powers don't answer");
			LegendaryWeaponItem.bond(held, strong);
			helper.assertTrue(LegendaryWeaponItem.bondedTo(held, strong) && LegendaryWeaponItem.stage(held) == 2 && LegendaryWeaponItem.powers(held, strong) == 2,
					"Bonded by a Heavenly Being, Ganjiang wakes to its second stage");
			helper.assertTrue(LegendaryWeaponItem.powers(held, weak) == 0, "Its powers answer nobody else");
			double woken = held.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream()
					.filter(m -> m.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL).mapToDouble(AttributeModifier::getAmount).sum();
			helper.assertTrue(Math.abs(woken - LegendaryWeaponItem.MIGHT[2]) < 1.0e-6, "Its might by its stage (" + woken + ")");
		} finally {
			leave(weak, strong);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void yangBurnsYinChillsBothStrikeTwice(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = survivor(helper, "test-legend-pair", Realm.CORE_FORMATION, 0.5, 1.5);
		Husk burnt = husk(helper, 2.5, 1.5), chilled = husk(helper, 2.5, 3.5), both = husk(helper, 2.5, 5.5);
		try {
			ItemStack ganjiang = Legends.issue(level.getServer(), Legend.GANJIANG), moye = Legends.issue(level.getServer(), Legend.MOYE);
			LegendaryWeaponItem.bond(ganjiang, p);
			LegendaryWeaponItem.bond(moye, p);
			LegendaryWeaponItem g = (LegendaryWeaponItem) ganjiang.getItem(), m = (LegendaryWeaponItem) moye.getItem();
			p.setItemSlot(EquipmentSlot.MAINHAND, ganjiang);
			g.strike(level, ganjiang, burnt, p, 1);
			helper.assertTrue(burnt.isOnFire() && burnt.getHealth() < burnt.getMaxHealth(), "Ganjiang's fire qi burns its foe");
			p.setItemSlot(EquipmentSlot.MAINHAND, moye);
			m.strike(level, moye, chilled, p, 1);
			helper.assertTrue(chilled.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !chilled.isOnFire(), "Moye's yin chills, and burns nothing");
			p.setItemSlot(EquipmentSlot.MAINHAND, ganjiang);
			p.setItemSlot(EquipmentSlot.OFFHAND, moye);
			g.strike(level, ganjiang, both, p, 1);
			helper.assertTrue(both.isOnFire() && both.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "One in each hand: every blow strikes twice, fire and cold");
			helper.assertTrue(LegendaryWeaponItem.reachesTheDead(level.damageSources().playerAttack(p)) == false,
					"Ganjiang in hand: the blow is no yin to reach the dead");
			p.setItemSlot(EquipmentSlot.MAINHAND, moye);
			helper.assertTrue(LegendaryWeaponItem.reachesTheDead(level.damageSources().playerAttack(p)), "Moye's blows reach the dead");
		} finally {
			burnt.discard();
			chilled.discard();
			both.discard();
			leave(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void heavensBrandOnPlayersAloneCapped(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(TribulationManager.branded(2, 10) == 1 && TribulationManager.branded(3200, 20) == 640 && TribulationManager.branded(18, 100) == 18
				&& TribulationManager.branded(0, 50) == 0 && TribulationManager.branded(50, 0) == 0, "A brand's bolts: its share, one at the least");
		ServerPlayer caster = survivor(helper, "test-brand-caster", Realm.TRUE_IMMORTAL, 0.5, 1.5);
		ServerPlayer branded = survivor(helper, "test-brand-target", Realm.TRUE_IMMORTAL, 4.5, 1.5); // strong enough to stand the bolts
		Husk husk = husk(helper, 4.5, 4.5);
		try {
			PlayerCultivation c = CultivationManager.get(branded);
			c.setTribulationBrand(0);
			LegendaryWeaponItem.heavensBrand(level, caster, branded, 3);
			helper.assertTrue(c.getTribulationBrand() == LegendaryWeaponItem.BRAND[3], "Struck by the art, a player is branded (" + c.getTribulationBrand() + ")");
			for (int i = 0; i < 10; i++) LegendaryWeaponItem.heavensBrand(level, caster, branded, 3);
			helper.assertTrue(c.getTribulationBrand() == LegendaryWeaponItem.BRAND_MOST, "Never past twice the bolts");
			LegendaryWeaponItem.heavensBrand(level, caster, husk, 3);
			helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "Anyone else takes the bolt alone");
			c.setTribulationBrand(0);
			// Lei Zhen's bolt is the art's harm alone: it lays no brand.
			LegendaryWeaponItem.heavensBolt(level, caster, branded.position(), 3);
			helper.assertTrue(c.getTribulationBrand() == 0, "The bolt alone brands nobody");
		} finally {
			husk.discard();
			leave(caster, branded);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spearDrinksForItsOwnerAndKeepsTheStrength(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = survivor(helper, "test-spear-owner", Realm.TRUE_IMMORTAL, 0.5, 1.5);
		try {
			ItemStack spear = Legends.issue(level.getServer(), Legend.TRIBULATION_SPEAR);
			owner.setItemSlot(EquipmentSlot.MAINHAND, spear);
			helper.assertFalse(LegendaryWeaponItem.drinks(owner), "Not bonded to its owner yet, it drinks nothing");
			LegendaryWeaponItem.bond(spear, owner);
			helper.assertTrue(LegendaryWeaponItem.drinks(owner), "Held out by its owner, it drinks");
			owner.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			owner.getInventory().setItem(9, spear);
			helper.assertFalse(LegendaryWeaponItem.drinks(owner), "In the pack, it drinks nothing");
			owner.getInventory().setItem(9, ItemStack.EMPTY);
			owner.setItemSlot(EquipmentSlot.OFFHAND, spear);
			helper.assertTrue(LegendaryWeaponItem.drinks(owner), "In the other hand, it drinks as well");
			owner.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			owner.setItemSlot(EquipmentSlot.MAINHAND, spear);
			int before = Legends.get(level.getServer()).entry(Legend.TRIBULATION_SPEAR).drunk();
			for (int i = 0; i < 3; i++) LegendaryWeaponItem.drink(owner, i == 0);
			helper.assertTrue(Legends.get(level.getServer()).entry(Legend.TRIBULATION_SPEAR).drunk() == before + 3, "Every bolt it drinks is kept in the record");
			// What it keeps: a reformed spear is made with it; its might grows a little for each.
			ItemStack again = Legends.restack(Legends.get(level.getServer()).entry(Legend.TRIBULATION_SPEAR));
			helper.assertTrue(LegendaryWeaponItem.drunk(again) == before + 3, "Made again, it keeps what it drank");
			ItemStack heavy = LegendaryWeaponItem.create(Legend.TRIBULATION_SPEAR, 1, 5000);
			heavy.getOrCreateTag().putInt(LegendaryWeaponItem.STAGE, 1);
			double might = heavy.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream()
					.filter(m -> m.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL).mapToDouble(AttributeModifier::getAmount).sum();
			helper.assertTrue(Math.abs(might - (LegendaryWeaponItem.MIGHT[1] + 0.1)) < 1.0e-6, "Five thousand bolts: a tenth more might (" + might + ")");
			ItemStack echo = spear.copy();
			Legends.issue(level.getServer(), Legend.TRIBULATION_SPEAR);
			owner.setItemSlot(EquipmentSlot.MAINHAND, echo);
			helper.assertFalse(LegendaryWeaponItem.drinks(owner), "A hollow echo drinks nothing");
			echo.inventoryTick(level, owner, owner.getInventory().selected, true);
			helper.assertTrue(LegendaryWeaponItem.serial(echo) == 0 && LegendaryWeaponItem.bondOf(echo) == null && LegendaryWeaponItem.stage(echo) == 0,
					"An echo in hand is shown for what it is, nothing of a woken one's kept");
		} finally {
			leave(owner);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aStonesSwordIsDrawnOnlyWhenItMayBe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
		level.setBlock(at, ModBlocks.LEGEND_STONE.defaultBlockState(), 3);
		ServerPlayer weak = survivor(helper, "test-stone-weak", Realm.QI_REFINING, 0.5, 0.5);
		ServerPlayer worthy = survivor(helper, "test-stone-worthy", Realm.CORE_FORMATION, 0.5, 2.5);
		try {
			helper.assertTrue(level.getBlockEntity(at) instanceof LegendStoneBlockEntity, "The stone keeps its sword");
			LegendStoneBlockEntity stone = (LegendStoneBlockEntity) level.getBlockEntity(at);
			stone.setHeld(Legends.issue(level.getServer(), Legend.GANJIANG));
			Legends.draw(level, worthy, stone);
			helper.assertFalse(stone.held().isEmpty(), "Its trial not won, the sword holds fast");
			stone.unlock();
			Legends.draw(level, weak, stone);
			helper.assertFalse(stone.held().isEmpty(), "Too heavy for one below Core Formation");
			Legends.draw(level, worthy, stone);
			helper.assertTrue(stone.held().isEmpty() && !LegendaryWeaponItem.carried(worthy, Legend.GANJIANG).isEmpty(), "Won, the worthy draw it");
			Legends.Entry entry = Legends.get(level.getServer()).entry(Legend.GANJIANG);
			helper.assertTrue(entry.state() == Legends.State.ABROAD && worthy.getUUID().equals(entry.holder()), "The record has it in their hands");
			// A stale sword left in a stone (its legend made anew elsewhere) crumbles away.
			stone.setHeld(LegendaryWeaponItem.create(Legend.GANJIANG, 1, 0));
			helper.assertTrue(Legends.trueHeld(level, stone).isEmpty() && stone.held().isEmpty(), "An echo in a stone is gone");
		} finally {
			leave(weak, worthy);
			level.removeBlock(at, false);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void leiZhenMadAtTheFourteenthRealm(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(LeiZhen.RANK == PlayerCultivation.rank(Realm.GOLDEN_IMMORTAL, Stage.GRAND_PERFECTION) && Realm.GOLDEN_IMMORTAL.ordinal() == 13,
				"Lei Zhen stands at Golden Immortal Grand Perfection, the fourteenth realm");
		// His way: each point within his ring about the Southern Heavenly Gate, and clear of the realm's people.
		ServerLevel immortal = level.getServer().getLevel(ModDimensions.IMMORTAL_REALM);
		if (immortal != null) {
			RandomState state = immortal.getChunkSource().randomState();
			BlockPos from = LeiZhen.start(immortal);
			var random = net.minecraft.util.RandomSource.create(7);
			int clear = 0;
			for (int i = 0; i < 12; i++) {
				BlockPos next = LeiZhen.nextPoint(immortal, from, random);
				double out = Math.hypot(next.getX() - ImmortalGate.HEAVEN_X, next.getZ() - ImmortalGate.HEAVEN_Z);
				helper.assertTrue(out >= LeiZhen.RING_MIN - 1 && out <= LeiZhen.RING_MAX + 1, "His way keeps to his ring (" + Math.round(out) + ")");
				if (PrimordialBeasts.clearOfPeople(state, next.getX(), next.getZ())) clear++;
				from = next;
			}
			helper.assertTrue(clear >= 10, "His way keeps clear of the realm's cities and sects (" + clear + " of 12)");
		}
		BlockPos step = LeiZhen.step(BlockPos.ZERO, new BlockPos(100, 0, 0), 6, 0);
		helper.assertTrue(step.getX() == 6 && LeiZhen.step(BlockPos.ZERO, new BlockPos(3, 0, 0), 6, 0).getX() == 3, "He walks on toward his way's point, not past it");
		// Come into the world: his realm whatever room the census has, the true spear in hand, aloft, and mad.
		ServerPlayer near = survivor(helper, "test-lei-zhen-near", Realm.FOUNDATION_BUILDING, 0.5, 0.5);
		Legends data = Legends.get(level.getServer());
		Legends.Entry entry = new Legends.Entry(Legend.TRIBULATION_SPEAR);
		entry.site = helper.absolutePos(new BlockPos(1, 0, 1));
		CultivatorNpc lei = LeiZhen.come(level, data, entry);
		try {
			helper.assertTrue(lei != null && lei.getRank() == LeiZhen.RANK && LeiZhen.is(lei) && Legends.isTrue(level.getServer(), lei.getMainHandItem()),
					"Lei Zhen comes at his realm with the true spear");
			helper.assertTrue(lei.getCustomName() != null && lei.getCustomName().getString().equals(LeiZhen.NAME), "Named Lei Zhen");
			helper.assertTrue(LeiZhen.goesFor(lei, near) && !LeiZhen.goesFor(lei, lei), "Mad: he goes for anyone but himself");
			helper.assertFalse(lei.isWanderingRogue(), "No wandering rogue of the spawner's");
		} finally {
			if (lei != null) lei.discard();
			leave(near);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void tombSpiritsAndTwinDragons(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos home = helper.absolutePos(new BlockPos(1, 1, 1));
		ServerPlayer owner = survivor(helper, "test-twin-dragons", Realm.CORE_FORMATION, 0.5, 3.5);
		SwordSpirit guardian = SwordSpirit.guardian(level, Vec3.atCenterOf(home).add(0, 2, 0), home);
		SwordSpirit dragon = SwordSpirit.called(level, owner, Legend.GANJIANG, 6, 30);
		helper.assertTrue(guardian != null && guardian.kind() == SwordSpirit.TOMB && dragon != null && dragon.kind() == SwordSpirit.GANJIANG,
				"A tomb's blade and a spirit of Ganjiang");
		helper.assertTrue(dragon.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6, "A called spirit strikes with its share of its owner's strike");
		helper.assertFalse(dragon.hurt(level.damageSources().playerAttack(owner), 5), "Its owner's blows pass through it");
		helper.succeedWhen(() -> {
			helper.assertTrue(guardian.getTarget() instanceof net.minecraft.world.entity.player.Player, "The tomb's blade goes for a player near its stone");
			helper.assertTrue(dragon.isRemoved(), "Its time up, the called spirit goes back into its sword");
			guardian.discard();
			leave(owner);
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theWorldTalksOfItsLegends(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Rumours.noteLegend(Legend.MOYE, ModDimensions.NETHERWORLD, new BlockPos(500, 0, 500), Legends.State.RESTING, "", level.getGameTime());
		Rumours.noteLegend(Legend.GANJIANG, level.dimension(), helper.absolutePos(new BlockPos(1, 0, 1)).offset(800, 0, 0), Legends.State.ABROAD, "Someone",
				level.getGameTime());
		Rumours.tick(level.getServer());
		CultivatorNpc teller = CultivatorNpc.createFor(level);
		try {
			BlockPos origin = helper.absolutePos(BlockPos.ZERO);
			teller.moveTo(origin.getX() + 0.5, origin.getY() + 2, origin.getZ() + 0.5, 0, 0);
			teller.becomeRogue(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0, level.getRandom());
			level.addFreshEntity(teller);
			List<Rumours.Rumour> first = Rumours.untold(teller, new HashSet<>(), 2);
			helper.assertTrue(first.stream().anyMatch(r -> r.kind == Rumours.Kind.LEGEND), "Untold legends come first of what a teller says");
			// Everything else the test world knows of told already: the legends left.
			HashSet<String> heard = new HashSet<>();
			Rumours.get(level.getServer()).all().stream().filter(r -> r.kind != Rumours.Kind.LEGEND).map(r -> r.id).forEach(heard::add);
			List<Rumours.Rumour> told = Rumours.untold(teller, heard, 50);
			Rumours.Rumour moye = told.stream().filter(r -> r.kind == Rumours.Kind.LEGEND && r.subject.equals("moye")).findFirst().orElse(null);
			Rumours.Rumour ganjiang = told.stream().filter(r -> r.kind == Rumours.Kind.LEGEND && r.subject.equals("ganjiang")).findFirst().orElse(null);
			String known = String.join(", ", Rumours.all(level.getServer(), Rumours.Kind.LEGEND).stream().map(r -> r.id).toList());
			String picked = String.join(", ", told.stream().filter(r -> r.kind == Rumours.Kind.LEGEND).map(r -> r.id).toList());
			helper.assertTrue(moye != null && ganjiang != null, "Legends are talked of in every world, however far off (known: " + known + "; told: "
					+ picked + "; " + told.size() + " told in all)");
			helper.assertTrue(key(Rumours.tell(moye, teller)).equals(ModLang.rumour("legend.moye.far")), "Of another world's legend, only the world it lies in");
			helper.assertTrue(key(Rumours.tell(ganjiang, teller)).equals(ModLang.rumour("legend.held")), "In someone's hands: whose");
		} finally {
			teller.discard();
		}
		helper.succeed();
	}

	// --- Phase 3b ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void xuanyuanAnswersTheRighteousAndChiyouRises(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
		level.setBlock(at, ModBlocks.LEGEND_STONE.defaultBlockState(), 3);
		ServerPlayer righteous = survivor(helper, "test-xuanyuan-righteous", Realm.NASCENT_SOUL, 0.5, 3.5);
		ServerPlayer devilish = survivor(helper, "test-xuanyuan-devilish", Realm.NASCENT_SOUL, 3.5, 3.5);
		Husk husk = husk(helper, 4.5, 0.5);
		GhostCultivatorNpc shade = null;
		try {
			CultivationManager.get(righteous).setAlignment(150);
			CultivationManager.get(devilish).setAlignment(-150);
			helper.assertTrue(LegendaryWeaponItem.evil(devilish) && !LegendaryWeaponItem.evil(righteous) && !LegendaryWeaponItem.evil(husk),
					"The sword's bane: the devilish, not the righteous nor a mere husk");
			LegendStoneBlockEntity stone = (LegendStoneBlockEntity) level.getBlockEntity(at);
			stone.setHeld(Legends.issue(level.getServer(), Legend.XUANYUAN));
			Legends.draw(level, devilish, stone);
			helper.assertFalse(stone.held().isEmpty(), "It won't stir for the devilish");
			Legends.draw(level, righteous, stone);
			ItemStack sword = LegendaryWeaponItem.carried(righteous, Legend.XUANYUAN);
			helper.assertTrue(stone.held().isEmpty() && !sword.isEmpty(), "The righteous draw it");
			shade = level.getEntitiesOfClass(GhostCultivatorNpc.class, new AABB(at).inflate(8), g -> g.getRole() == GhostCultivatorNpc.Role.CHIYOU)
					.stream().findFirst().orElse(null);
			helper.assertTrue(shade != null && shade.getRank() == Legends.CHIYOU_RANK && righteous.getUUID().equals(LegendGuards.preyOf(shade))
					&& LegendGuards.goesFor(shade, righteous) && !LegendGuards.goesFor(shade, devilish), "Chiyou's shade rises and hunts the drawer alone");
			helper.assertTrue(shade.getTitle() == NpcTitle.FALLEN_WAR_GOD && shade.getScale() == GhostCultivatorNpc.CHIYOU_SCALE && LegendaryWeaponItem.evil(shade),
					"The Fallen War God, twice a man's height, one of the dead");
			float before = shade.getHealth();
			helper.assertTrue(shade.hurt(level.damageSources().playerAttack(righteous), 4) && shade.getHealth() < before,
					"Made flesh by its hatred, the living's blows land on it");
			LegendaryWeaponItem.bond(sword, righteous);
			helper.assertTrue(LegendaryWeaponItem.powers(sword, righteous) == 1 && LegendaryWeaponItem.reachesTheDead(level.damageSources().playerAttack(righteous)),
					"Bonded by the righteous: its first stage at Nascent Soul, its blows reaching the dead");
			CultivationManager.get(righteous).setAlignment(0);
			helper.assertTrue(LegendaryWeaponItem.powers(sword, righteous) == 0, "Its owner no longer righteous, it answers no more");
			CultivationManager.get(righteous).setAlignment(-150);
			sword.inventoryTick(level, righteous, righteous.getInventory().selected, true);
			helper.assertTrue(sword.isEmpty() && LegendaryWeaponItem.carried(righteous, Legend.XUANYUAN).isEmpty()
					&& Legends.get(level.getServer()).entry(Legend.XUANYUAN).state() == Legends.State.RESTING, "It won't stay with the devilish: home again");
		} finally {
			if (shade != null) shade.discard();
			husk.discard();
			leave(righteous, devilish);
			level.removeBlock(at, false);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sunBowNeedsNoArrowsAndTheCrowKeepsIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
		level.setBlock(at, ModBlocks.LEGEND_STONE.defaultBlockState(), 3);
		ServerPlayer archer = survivor(helper, "test-sun-archer", Realm.HEAVENLY_BEING, 0.5, 4.5);
		Husk husk = husk(helper, 4.5, 4.5);
		DemonBeast crow = null;
		try {
			LegendaryWeaponItem bow = (LegendaryWeaponItem) ModItems.legend(Legend.SUN_BOW);
			ItemStack stack = Legends.issue(level.getServer(), Legend.SUN_BOW);
			helper.assertTrue(bow.getUseDuration(stack) == 72000 && bow.getUseAnimation(stack) == UseAnim.BOW, "Drawn as a bow is");
			LegendStoneBlockEntity stone = (LegendStoneBlockEntity) level.getBlockEntity(at);
			stone.setHeld(stack);
			Legends.Entry entry = new Legends.Entry(Legend.SUN_BOW);
			entry.site = at.below();
			Legends.crowCrater(level, entry);
			List<net.minecraft.world.entity.LivingEntity> guards = stone.guards(level);
			helper.assertTrue(guards.size() == 1 && guards.get(0) instanceof DemonBeast beast && beast.kind() == BeastKind.GOLDEN_CROW
					&& beast.getRank() == BeastKind.GOLDEN_CROW.minRank() && BeastKind.GOLDEN_CROW.minRank() == PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION),
					"The Golden Crow rises for one near, at Heavenly Being Grand Perfection");
			crow = (DemonBeast) guards.get(0);
			Legends.draw(level, archer, stone);
			helper.assertFalse(stone.held().isEmpty(), "The bow won't move while the crow lives");
			crow.discard();
			Legends.crowCrater(level, entry);
			helper.assertTrue(stone.unlocked(), "The crow gone, the bow may be drawn");
			Legends.draw(level, archer, stone);
			ItemStack drawn = archer.getMainHandItem();
			helper.assertTrue(drawn.getItem() == bow && Legends.isTrue(level.getServer(), drawn), "Drawn by a Heavenly Being");
			LegendaryWeaponItem.bond(drawn, archer);
			bow.releaseUsing(drawn, level, archer, bow.getUseDuration(drawn) - 25);
			List<AbstractArrow> arrows = level.getEntitiesOfClass(AbstractArrow.class, archer.getBoundingBox().inflate(8), a -> a.getOwner() == archer);
			helper.assertTrue(arrows.size() == 1 && arrows.get(0).pickup == AbstractArrow.Pickup.DISALLOWED && arrows.get(0).isOnFire(),
					"Let go, an arrow of sun qi flies, burning, none to pick up (" + arrows.size() + ")");
			arrows.forEach(net.minecraft.world.entity.Entity::discard);
			LegendaryWeaponItem.fallingSun(level, archer, husk.position(), 1);
			helper.assertTrue(husk.getHealth() < husk.getMaxHealth() && husk.isOnFire(), "Falling Sun bursts into sunfire where it falls");
		} finally {
			if (crow != null) crow.discard();
			husk.discard();
			leave(archer);
			level.removeBlock(at, false);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void seaNeedleStandsAndStaysBehindTheEar(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 0, 2));
		ServerPlayer strong = survivor(helper, "test-needle-strong", Realm.TRUE_IMMORTAL, 0.5, 0.5);
		ServerPlayer weak = survivor(helper, "test-needle-weak", Realm.HEAVENLY_BEING, 4.5, 0.5);
		Husk husk = husk(helper, 1.5, 4.5);
		CultivatorNpc king = null;
		BlockPos foot = null;
		try {
			foot = LegendSites.pillar(level, origin.getX(), origin.getZ());
			int top = ModDimensions.IMMORTAL_CLOUD_Y + LegendSites.PILLAR_OVER;
			helper.assertTrue(level.getBlockState(origin.atY(top)).getValue(SeaPillarBlock.BANDED) && !level.getBlockState(origin.atY(top - 10)).getValue(SeaPillarBlock.BANDED)
					&& level.getBlockState(origin.atY(ModDimensions.IMMORTAL_CLOUD_Y + 1)).getValue(SeaPillarBlock.BANDED),
					"Black iron, banded with gold at its top and just out of the clouds");
			LegendSites.unbuildPillar(level, foot);
			helper.assertTrue(level.getBlockState(origin.atY(top)).isAir() && level.getBlockState(origin.atY(foot.getY())).isAir(), "Pulled, the pillar is gone");
			helper.assertTrue(LegendaryWeaponItem.mayWield(strong, Legend.SEA_NEEDLE) && !LegendaryWeaponItem.mayWield(weak, Legend.SEA_NEEDLE),
					"Only one of Entering Nirvana or above can pull it free");
			ItemStack needle = Legends.issue(level.getServer(), Legend.SEA_NEEDLE);
			strong.getInventory().setItem(5, needle);
			Legends.keepBehindTheEar(strong);
			Legends.Entry entry = Legends.get(level.getServer()).entry(Legend.SEA_NEEDLE);
			helper.assertTrue(LegendaryWeaponItem.carried(strong, Legend.SEA_NEEDLE).isEmpty() && !entry.kept.isEmpty() && strong.getUUID().equals(entry.holder()),
					"Dying, its holder keeps it behind their ear: out of the pack, kept in the record");
			Legends.giveBack(strong);
			ItemStack back = LegendaryWeaponItem.carried(strong, Legend.SEA_NEEDLE);
			helper.assertTrue(!back.isEmpty() && entry.kept.isEmpty(), "Living again, it is theirs again");
			weak.getInventory().setItem(5, LegendaryWeaponItem.create(Legend.SEA_NEEDLE, 0, 0));
			Legends.keepBehindTheEar(weak);
			helper.assertFalse(weak.getInventory().getItem(5).isEmpty(), "A hollow echo isn't kept: it falls as anything else");
			((LegendaryWeaponItem) back.getItem()).strike(level, back, husk, strong, 1);
			helper.assertTrue(husk.getDeltaMovement().horizontalDistance() > 0.3 && husk.getHealth() < husk.getMaxHealth(), "Its blow lands its weight and throws");
			king = Legends.aoGuang(level, strong, origin);
			helper.assertTrue(king != null && king.getRank() == Legends.AO_GUANG_RANK && king.getBloodline() == Bloodline.DRAGON && king.getTitle() == NpcTitle.DRAGON_KING
					&& strong.getUUID().equals(LegendGuards.preyOf(king)) && LegendGuards.is(king) && king.holdsGround(),
					"Ao Guang, a dragon of True Immortal, rises and hunts the one who took it");
		} finally {
			if (foot != null) LegendSites.unbuildPillar(level, foot);
			if (king != null) king.discard();
			husk.discard();
			leave(strong, weak);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void theVastArtsLandAtTheirMoment(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer caster = survivor(helper, "test-legend-arts", Realm.ENTERING_NIRVANA, 0.5, 0.5);
		Husk judged = husk(helper, 4.5, 4.5), crushed = husk(helper, 0.5, 5.5);
		ItemStack sword = Legends.issue(level.getServer(), Legend.XUANYUAN), needle = Legends.issue(level.getServer(), Legend.SEA_NEEDLE);
		boolean[] landed = {false};
		Colossus.make(level, sword, Colossus.FALL, 10, judged.position().add(0, 48, 0), judged.position(), () -> landed[0] = true);
		LegendaryWeaponItem.judgement(level, caster, sword, judged.position(), 1);
		LegendaryWeaponItem.mountain(level, caster, needle, crushed.position(), 1);
		helper.assertTrue(!landed[0] && judged.getHealth() == judged.getMaxHealth() && crushed.getHealth() == crushed.getMaxHealth(),
				"Nothing lands before its moment");
		helper.succeedWhen(() -> {
			helper.assertTrue(landed[0], "Its harm lands at its moment");
			helper.assertTrue(judged.getHealth() < judged.getMaxHealth(), "The Judgement struck what it fell on");
			helper.assertTrue(crushed.getHealth() < crushed.getMaxHealth(), "The mountain came down on what stood there");
			helper.assertTrue(level.getEntitiesOfClass(Colossus.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(64)).isEmpty(),
					"Gone once its moment is past");
			judged.discard();
			crushed.discard();
			leave(caster);
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void phase3bRestingPlacesAreWhereTheyShouldBe(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		ServerLevel surfaces = server.getLevel(ModDimensions.ASTRAL_SURFACES), immortal = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (surfaces != null) {
			BlockPos star = LegendSites.deadStar(surfaces);
			AstralPlanets.Planet world = AstralPlanets.get() == null ? null : AstralPlanets.get().planetAt(star.getX(), star.getZ());
			helper.assertTrue(world != null && (world.kind() == AstralPlanets.Kind.BARREN || world.kind() == AstralPlanets.Kind.ASHEN)
					&& star.getX() == world.landX() && star.getZ() == world.landZ(), "The dead sun fell on a barren or ashen world, on its east-facing side");
		}
		if (immortal != null) {
			BlockPos sea = LegendSites.openSea(immortal);
			double out = Math.hypot(sea.getX() - ImmortalGate.HEAVEN_X, sea.getZ() - ImmortalGate.HEAVEN_Z);
			RandomState state = immortal.getChunkSource().randomState();
			helper.assertTrue(out >= LegendSites.SEA_MIN - 1 && out <= LegendSites.SEA_MAX + 1 && PrimordialBeasts.clearOfPeople(state, sea.getX(), sea.getZ()),
					"The Needle stands out over the sea, clear of the realm's people (" + Math.round(out) + ")");
		}
		helper.succeed();
	}

	// --- Phase 3c ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void panguAxeCleavesWardsBarriersAndTheLand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var server = level.getServer();
		ServerPlayer wielder = survivor(helper, "test-pangu-wielder", Realm.EARTH_IMMORTAL, 0.5, 0.5);
		ServerPlayer other = survivor(helper, "test-pangu-other", Realm.TRUE_IMMORTAL, 0.5, 6.5);
		Husk struck = husk(helper, 4.5, 0.5), spared = husk(helper, 4.5, 6.5);
		var griefing = level.getGameRules().getRule(ModGameRules.SPELL_GRIEFING);
		boolean wasGriefing = griefing.get();
		try {
			ItemStack axe = Legends.issue(server, Legend.PANGU_AXE);
			wielder.setItemSlot(EquipmentSlot.MAINHAND, axe);
			helper.assertFalse(LegendaryWeaponItem.cleaves(wielder), "Unbonded, the axe cleaves nothing");
			LegendaryWeaponItem.bond(axe, wielder);
			helper.assertTrue(LegendaryWeaponItem.cleaves(wielder) && LegendaryWeaponItem.stage(axe) == 1 && Legend.PANGU_AXE.lowest() == Realm.EARTH_IMMORTAL,
					"Bonded by an Earth Immortal (past True Immortal), it cleaves");
			helper.assertTrue(LegendaryWeaponItem.cutsThroughWards(level.damageSources().playerAttack(wielder))
					&& !LegendaryWeaponItem.cutsThroughWards(level.damageSources().playerAttack(other)), "Its blows pass every ward; another's don't");
			// A barrier far above them both: the axe's wielder breaks it, at once; the other can't.
			Formation formation = new Formation(UUID.randomUUID(), Formation.Kind.CORE, helper.absolutePos(new BlockPos(1, 1, 1)), 8);
			formation.setRank(PlayerCultivation.rank(Realm.TAIYI_GOLDEN_IMMORTAL, Stage.GRAND_PERFECTION));
			helper.assertTrue(Formations.canBreak(wielder, formation) && Formations.breakTicks(wielder, formation) == 4 && !Formations.canBreak(other, formation),
					"No barrier holds against Pangu's Axe");
			// Split Heaven and Earth: everyone on its line struck, none off it (the land left whole here: the gamerule off).
			griefing.set(false, server);
			LegendaryWeaponItem.splitHeavenAndEarth(level, wielder, 1);
			helper.assertTrue(struck.getHealth() < struck.getMaxHealth() && spared.getHealth() == spared.getMaxHealth(),
					"Split Heaven and Earth strikes along its line alone");
			// It falls out of the Primordial Chaos only while it is in its heart.
			Legends.reform(server, Legend.PANGU_AXE);
			ItemStack fallen = Legends.fromTheChaos(server);
			helper.assertTrue(Legends.isTrue(server, fallen) && Legends.get(server).entry(Legend.PANGU_AXE).state() == Legends.State.ABROAD,
					"The Chaos slain, the axe falls out of its heart");
			helper.assertTrue(Legends.fromTheChaos(server).isEmpty(), "Out of its heart already, the next Chaos has none to drop");
		} finally {
			griefing.set(wasGriefing, server);
			struck.discard();
			spared.discard();
			leave(wielder, other);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void venerablesWeaponsAndTheDaoDuel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var server = level.getServer();
		ServerLevel immortal = server.getLevel(ModDimensions.IMMORTAL_REALM);
		ServerPlayer general = survivor(helper, "test-duel-general", Realm.TRUE_IMMORTAL, 0.5, 0.5);
		ServerPlayer stranger = survivor(helper, "test-duel-stranger", Realm.TRUE_IMMORTAL, 2.5, 0.5);
		Husk husk = husk(helper, 4.5, 4.5);
		try {
			helper.assertTrue(immortal != null, "The Immortal Realm is loaded for the game tests");
			// A Venerable at its post in the Immortal Realm, its weapon in hand (not set in the world: only asked).
			CultivatorNpc venerable = ModEntities.CULTIVATOR.create(immortal);
			venerable.moveTo(ModNoiseSettings.COURT_X + 0.5, 300, ModNoiseSettings.COURT_Z + 0.5, 0, 0);
			venerable.becomeRogue(PlayerCultivation.rank(Realm.TAIYI_GOLDEN_IMMORTAL, Stage.GRAND_PERFECTION), 90, immortal.getRandom());
			venerable.setRank(PlayerCultivation.rank(Realm.TAIYI_GOLDEN_IMMORTAL, Stage.GRAND_PERFECTION), true);
			venerable.setCourtElement(QiElement.WOOD);
			venerable.setTitle(NpcTitle.HEAVENLY_VENERABLE);
			venerable.setSettledAt(BlockPos.containing(venerable.position()));
			venerable.setHealth(venerable.getMaxHealth());
			helper.assertTrue(Legend.ofVenerable(QiElement.WOOD) == Legend.AZURE_GLAIVE && Legend.AZURE_GLAIVE.symbolSpell() == Spell.AZURE_DRAGON,
					"The Wood Venerable's is the Azure Dragon Glaive, its art the Azure Dragon");
			Legends.arm(venerable, Legends.issue(server, Legend.AZURE_GLAIVE));
			// Only a General of the Court may challenge it, however far above them it stands.
			CelestialCourt.join(general);
			CelestialCourt.award(general, CelestialCourt.Rank.GENERAL.merit(), ModLang.talkLine("court.join"));
			helper.assertTrue(Legends.duelFor(general, venerable) == Legend.AZURE_GLAIVE && Legends.duelFor(stranger, venerable) == null,
					"A General may duel the Venerable for its glaive; a stranger may not");
			helper.assertTrue((TalkSystem.intents(general, venerable, TalkSystem.Standing.FAR_ABOVE) & TalkIntent.SPAR.bit()) != 0
					&& (TalkSystem.intents(stranger, venerable, TalkSystem.Standing.FAR_ABOVE) & TalkIntent.SPAR.bit()) == 0,
					"It takes up a General's challenge, far above them though it stands");
			general.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			general.setHealth(general.getMaxHealth()); // both fit to spar (SparSystem#READY_SHARE)
			venerable.setHealth(venerable.getMaxHealth());
			String refusal = SparSystem.refusal(general, venerable, SparSystem.Terms.DUEL);
			helper.assertTrue(ModLang.SPAR_DUEL_NO_STAKE.equals(refusal), "No stake, no duel (" + refusal + ")");
			general.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.COURT_MERIT_TOKEN, SparSystem.DUEL_TOKENS + 4));
			helper.assertTrue(SparSystem.refusal(general, venerable, SparSystem.Terms.DUEL) == null, "Its stake in hand, the General may duel");
			SparSystem.start(general, venerable, SparSystem.Terms.DUEL);
			helper.assertTrue(general.getMainHandItem().getCount() == 4 && venerable.isHoldingBack() && SparSystem.isSparring(venerable),
					"The tokens staked, the Venerable holds back to the General's realm");
			SparSystem.abandon(general);
			// Won: it hands its weapon over and keeps its post.
			Legends.handOver(venerable, general, Legend.AZURE_GLAIVE);
			ItemStack won = LegendaryWeaponItem.carried(general, Legend.AZURE_GLAIVE);
			Legends.Entry entry = Legends.get(server).entry(Legend.AZURE_GLAIVE);
			helper.assertTrue(!won.isEmpty() && venerable.getMainHandItem().isEmpty() && entry.state() == Legends.State.ABROAD && general.getUUID().equals(entry.holder())
					&& venerable.getTitle() == NpcTitle.HEAVENLY_VENERABLE, "Its glaive handed over, the Venerable keeps its post");
			helper.assertTrue(Legends.duelFor(general, venerable) == null, "Its weapon gone, it has nothing more to stake");
			// The glaive's blow: wood qi, and its roots.
			LegendaryWeaponItem.bond(won, general);
			((LegendaryWeaponItem) won.getItem()).strike(level, won, husk, general, 1);
			helper.assertTrue(husk.getHealth() < husk.getMaxHealth() && husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Its blow lands wood qi, its roots slowing");
			// Where they are told of: Pangu's Axe at the Primordial Chaos's lair, the Venerables' weapons at the Court.
			BlockPos axeSite = Legends.site(immortal, Legend.PANGU_AXE), lair = PrimordialBeasts.findLair(immortal, BeastKind.PRIMORDIAL_CHAOS);
			BlockPos glaiveSite = Legends.site(immortal, Legend.AZURE_GLAIVE);
			helper.assertTrue(axeSite.getX() == lair.getX() && axeSite.getZ() == lair.getZ() && glaiveSite.getX() == ModNoiseSettings.COURT_X
					&& glaiveSite.getZ() == ModNoiseSettings.COURT_Z, "The axe told of at the Chaos's lair, the glaive at the Court");
		} finally {
			husk.discard();
			leave(general, stranger);
		}
		helper.succeed();
	}

	private static String key(Component told) {
		return told.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
	}
}
