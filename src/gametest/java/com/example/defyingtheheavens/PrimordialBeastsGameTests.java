package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The primordial beasts (phase 2, 2026-10-26): their strength, a little more than a True Immortal of Grand Perfection's (the user's word);
 * the Kun broken from the clouds as the Peng and the Kun again when left alone; the True Phoenix burnt to an ember and reborn once; what
 * they leave (their cores, Immortal Jade, the Phoenix's and the Dragon's True Immortal blood, the Kun-Peng's slip of its form's element),
 * hanging where they fell; their lairs about the Southern Heavenly Gate, each its own kind of place, clear of people; one of each, the
 * next five days after one is slain; the Venerables' word of them; and with them the user's two asks of the same day: the Court's two demon
 * Venerables and True Immortal blood sold in the realm's markets.
 */
public class PrimordialBeastsGameTests implements FabricGameTest {
	private static final int TOP = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);

	private static PrimordialBeast beast(ServerLevel level, BeastKind kind) {
		PrimordialBeast beast = (PrimordialBeast) ModEntities.beast(kind).create(level);
		beast.setRank(TOP);
		return beast;
	}

	/** Each beast's lair in the Immortal Realm's land as the test server makes it (it has no such world: ImmortalCitiesGameTests#land). */
	private static BlockPos[] lairs(GameTestHelper helper) {
		ImmortalCitiesGameTests.Land land = ImmortalCitiesGameTests.land(helper);
		BlockPos[] lairs = new BlockPos[PrimordialBeasts.KINDS.length];
		for (int k = 0; k < lairs.length; k++) {
			lairs[k] = PrimordialBeasts.findLair(land.state(), land.generator(), land.heights(), helper.getLevel().getSeed(), PrimordialBeasts.KINDS[k]);
		}
		return lairs;
	}

	private static String key(Component component) {
		return component.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theirStrength(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CultivatorNpc plain = ModEntities.CULTIVATOR.create(level), golden = ModEntities.CULTIVATOR.create(level);
		plain.setRank(TOP, true);
		golden.setRank(BeastKind.CHAOS_RANK, true);
		for (BeastKind kind : PrimordialBeasts.KINDS) {
			PrimordialBeast beast = beast(level, kind);
			// The Primordial Chaos (phase 3c) a Golden Immortal of Grand Perfection, the more by the same might.
			boolean chaos = kind == BeastKind.PRIMORDIAL_CHAOS;
			CultivatorNpc like = chaos ? golden : plain;
			double body = beast.getMaxHealth() / like.getMaxHealth();
			double blows = beast.getAttributeValue(Attributes.ATTACK_DAMAGE) / like.getAttributeValue(Attributes.ATTACK_DAMAGE);
			helper.assertTrue(Math.abs(body - PrimordialBeast.MIGHT) < 0.01 && Math.abs(blows - PrimordialBeast.MIGHT) < 0.01,
					kind + ": a little stronger than its realm's Grand Perfection (" + body + ", " + blows + ")");
			helper.assertTrue(kind.tier == BeastKind.Tier.PRIMORDIAL && beast.getRank() == (chaos ? BeastKind.CHAOS_RANK : TOP)
					&& beast.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.999, kind + ": of its realm's Grand Perfection, moved by nothing");
			helper.assertTrue(beast.isInvulnerableTo(level.damageSources().inWall()) && beast.isInvulnerableTo(level.damageSources().fall())
					&& !beast.isInvulnerableTo(level.damageSources().generic()) && beast.requiresCustomPersistence(),
					kind + ": no suffocating in the land it brushes nor falling, but harmed; never gone for want of anyone near");
		}
		helper.assertTrue(PrimordialBeast.MIGHT > 1 && PrimordialBeast.MIGHT < Realm.EARTH_IMMORTAL.getMaxHealth() / Realm.TRUE_IMMORTAL.getMaxHealth(),
				"Slightly stronger: not the next realm's strength (an Earth Immortal's, twice a True Immortal's)");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theKunBecomesThePeng(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		PrimordialBeast kun = beast(level, BeastKind.KUN_PENG);
		BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
		kun.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		helper.assertTrue(kun.isKun() && kun.body() == BeastKind.Body.FISH && kun.element() == QiElement.WATER
				&& Math.abs(kun.getBbWidth() - BeastKind.KUN_PENG.width) < 0.01, "The Kun: a fish, of water");
		kun.breach(level);
		helper.assertTrue(kun.isPeng() && kun.body() == BeastKind.Body.BIRD && kun.element() == QiElement.METAL
				&& Math.abs(kun.getBbWidth() - PrimordialBeast.PENG_WIDTH) < 0.01, "Broken from the clouds, the Peng: a bird, of metal, its own size");
		kun.setHealth(kun.getMaxHealth() * 0.3f);
		kun.calm(level, level.getGameTime());
		helper.assertTrue(kun.isKun() && kun.getHealth() >= kun.getMaxHealth() - 1, "Left alone, the Kun again and whole");
		helper.succeed();
	}

	/**
	 * The user, 2026-10-26: "Make the ku's first form look like the one from the spell that summons it... overhaul the dragon model, and
	 * make all of them larger": drawn vaster, their bodies and reaches grown with them; the Kun's model the spell's and the Dragon's
	 * tools/spell-art's, each moved by its own swimming loop (BeastModels), the Peng's and the Phoenix's in code; what they do seen from afar.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theirSizesAndLooks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(BeastKind.KUN_PENG.modelScale() >= 6 && BeastKind.TRUE_PHOENIX.modelScale() >= 5 && BeastKind.TRUE_DRAGON.modelScale() >= 5,
				"Drawn half again larger than first made");
		PrimordialBeast kun = beast(level, BeastKind.KUN_PENG), phoenix = beast(level, BeastKind.TRUE_PHOENIX), dragon = beast(level, BeastKind.TRUE_DRAGON);
		helper.assertTrue(kun.getBbWidth() >= 16 && phoenix.getBbWidth() > 9.5 && dragon.getBbWidth() > 11 && PrimordialBeast.PENG_WIDTH > 12,
				"Their bodies grown with them (" + kun.getBbWidth() + ", " + phoenix.getBbWidth() + ", " + dragon.getBbWidth() + ")");
		helper.assertTrue(kun.swims() && dragon.swims() && !phoenix.swims(), "The Kun and the Dragon swim by their own loops, the Phoenix is moved in code");
		kun.breach(level);
		helper.assertTrue(!kun.swims(), "The Peng is moved in code");
		helper.assertTrue(PrimordialBeast.NOTICE >= 64 && PrimordialBeast.BAR_REACH > PrimordialBeast.NOTICE && PrimordialBeast.FX_REACH > PrimordialBeast.BAR_REACH,
				"Its reaches grown with it, what it does seen past where it shows its bar");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void thePhoenixIsRebornOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		PrimordialBeast phoenix = beast(level, BeastKind.TRUE_PHOENIX);
		BlockPos at = helper.absolutePos(new BlockPos(1, 3, 1));
		phoenix.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		phoenix.die(level.damageSources().generic());
		helper.assertTrue(phoenix.isEmber() && phoenix.isAlive() && phoenix.getHealth() > 0 && !phoenix.reborn(), "Slain the first time, an ember, alive");
		helper.assertTrue(phoenix.isInvulnerableTo(level.damageSources().generic()) && !phoenix.isInvulnerableTo(level.damageSources().genericKill()),
				"An ember untouchable, but by what nothing withstands");
		for (int i = 0; i < PrimordialBeast.EMBER_TICKS; i++) phoenix.burn(level);
		helper.assertTrue(!phoenix.isEmber() && phoenix.reborn() && phoenix.getHealth() >= phoenix.getMaxHealth() - 1, "Reborn from its fire, whole");
		phoenix.calm(level, level.getGameTime());
		helper.assertTrue(!phoenix.reborn(), "Left alone, its rebirth its own again");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theirSpoils(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(2, 3, 2));
		Object[][] cases = {{BeastKind.KUN_PENG, false}, {BeastKind.KUN_PENG, true}, {BeastKind.TRUE_PHOENIX, false}, {BeastKind.TRUE_DRAGON, false}};
		for (Object[] c : cases) {
			BeastKind kind = (BeastKind) c[0];
			boolean peng = (Boolean) c[1];
			PrimordialBeast beast = beast(level, kind);
			beast.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			if (peng) beast.breach(level);
			beast.dropCustomDeathLoot(level.damageSources().generic(), 0, true);
			List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3));
			int jade = 0;
			boolean core = false, floats = !items.isEmpty();
			Bloodline blood = null;
			Realm bloodRealm = null;
			HeavenMethod slip = null;
			for (ItemEntity item : items) {
				ItemStack stack = item.getItem();
				floats &= item.isNoGravity() && item.hasGlowingTag();
				if (stack.is(ModItems.IMMORTAL_JADE)) jade += stack.getCount();
				if (stack.is(ModItems.DEMON_CORE)) core = true;
				if (stack.is(ModItems.DEMON_BLOOD)) {
					blood = DemonBloodItem.race(stack);
					bloodRealm = DemonBloodItem.realm(stack);
				}
				if (stack.is(ModItems.JADE_SLIP)) slip = JadeSlipItem.method(stack);
			}
			items.forEach(Entity::discard);
			String what = kind + (peng ? " (the Peng)" : "");
			helper.assertTrue(core && jade >= PrimordialBeast.JADE && jade <= PrimordialBeast.JADE + PrimordialBeast.JADE_MORE, what + ": its core and Immortal Jade (" + jade + ")");
			helper.assertTrue(floats, what + ": what it leaves hangs where it fell, glowing");
			Bloodline kin = kind == BeastKind.TRUE_PHOENIX ? Bloodline.FOX : kind == BeastKind.TRUE_DRAGON ? Bloodline.DRAGON : null;
			helper.assertTrue(blood == kin && (kin == null || bloodRealm == Realm.TRUE_IMMORTAL), what + ": its race's True Immortal blood (" + blood + ", " + bloodRealm + ")");
			if (kind == BeastKind.KUN_PENG) {
				QiElement element = peng ? QiElement.METAL : QiElement.WATER;
				helper.assertTrue(slip != null && slip.realm() == Realm.TRUE_IMMORTAL && slip.element() == element, what + ": a True Immortal slip of its form's element (" + slip + ")");
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
	public void theirLairs(GameTestHelper helper) {
		RandomState state = ImmortalCitiesGameTests.land(helper).state();
		BlockPos[] lairs = lairs(helper);
		for (int k = 0; k < lairs.length; k++) {
			BeastKind kind = PrimordialBeasts.KINDS[k];
			BlockPos lair = lairs[k];
			if (kind == BeastKind.PRIMORDIAL_CHAOS) continue; // out in the Chaos Frontier (ChaosFrontierGameTests)
			double d = Math.hypot(lair.getX() - ImmortalGate.HEAVEN_X, lair.getZ() - ImmortalGate.HEAVEN_Z);
			helper.assertTrue(d >= PrimordialBeasts.LAIR_MIN - 1 && d <= PrimordialBeasts.LAIR_MAX + 1, kind + "'s lair " + Math.round(d) + " from the Gate");
			helper.assertTrue(PrimordialBeasts.clearOfPeople(state, lair.getX(), lair.getZ()), kind + "'s lair clear of the cities and the sects (" + lair.toShortString() + ")");
			// Its kind's place, or at the least (the second, relaxed pass: PrimordialBeasts#findLair) one somewhat like it.
			switch (kind) {
				case KUN_PENG -> helper.assertTrue(ModNoiseSettings.massifMask(state, lair.getX(), lair.getZ()) <= -0.1 && lair.getY() == ModDimensions.IMMORTAL_CLOUD_Y - PrimordialBeasts.KUN_UNDER,
						"The Kun's in the open sea of clouds (" + lair.toShortString() + ")");
				case TRUE_PHOENIX -> helper.assertTrue(lair.getY() >= ModDimensions.IMMORTAL_CLOUD_Y + PrimordialBeasts.PEAK_OVER_CLOUDS / 2 + PrimordialBeasts.PHOENIX_OVER,
						"The Phoenix's over a peak out of the clouds (" + lair.toShortString() + ")");
				default -> helper.assertTrue(lair.getY() >= ModDimensions.IMMORTAL_CLOUD_Y + 20 + PrimordialBeasts.DRAGON_OVER,
						"The Dragon's high over what stands out of the clouds (" + lair.toShortString() + ")");
			}
		}
		for (int a = 0; a < lairs.length; a++) {
			for (int b = a + 1; b < lairs.length; b++) {
				helper.assertTrue(Math.hypot(lairs[a].getX() - lairs[b].getX(), lairs[a].getZ() - lairs[b].getZ()) > 400, "The three lairs far apart (their reaches never meet)");
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCourtsDemonsAndItsPay(GameTestHelper helper) {
		helper.assertTrue(CelestialCourt.venerableRace(QiElement.METAL) == Bloodline.TIGER && CelestialCourt.venerableRace(QiElement.WOOD) == Bloodline.DRAGON
				&& CelestialCourt.venerableRace(QiElement.FIRE) == null && CelestialCourt.venerableRace(QiElement.EARTH) == null && CelestialCourt.venerableRace(QiElement.WATER) == null,
				"The Metal Venerable of the tiger race, the Wood Venerable of the dragon's");
		ServerLevel level = helper.getLevel();
		CultivatorNpc metal = CelestialCourt.arrive(level, new CelestialCourt.Post(helper.absolutePos(new BlockPos(1, 2, 1)), NpcTitle.HEAVENLY_VENERABLE, QiElement.METAL, 0, true), level.getRandom());
		CultivatorNpc wood = CelestialCourt.arrive(level, new CelestialCourt.Post(helper.absolutePos(new BlockPos(3, 2, 1)), NpcTitle.HEAVENLY_VENERABLE, QiElement.WOOD, 0, true), level.getRandom());
		try {
			helper.assertTrue(metal != null && wood != null && metal.getBloodline() == Bloodline.TIGER && wood.getBloodline() == Bloodline.DRAGON && metal.isDemon() && wood.isDemon(),
					"Come to their halls, demons of their races");
			// A Taiyi Golden Immortal of Grand Perfection (phase 3), its race's or not.
			helper.assertTrue(metal.getTitle() == NpcTitle.HEAVENLY_VENERABLE && metal.getRank() == PlayerCultivation.rank(Realm.TAIYI_GOLDEN_IMMORTAL, Stage.GRAND_PERFECTION)
					&& Math.abs(metal.getMaxHealth() - (CultivationStats.maxHealth(Realm.TAIYI_GOLDEN_IMMORTAL, Stage.GRAND_PERFECTION) + 20)) < 2,
					"A Venerable's rank and strength still (" + metal.getMaxHealth() + ")");
		} finally {
			if (metal != null) metal.discard();
			if (wood != null) wood.discard();
		}
		// The Court pays each of its officers who fought one of the realm's primordial beasts slain; nothing for one called up otherwise.
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		CelestialCourt.join(player);
		int before = CelestialCourt.meritOf(player.server, player.getUUID());
		PrimordialBeast called = beast(level, BeastKind.TRUE_DRAGON), realms = beast(level, BeastKind.TRUE_DRAGON);
		realms.settle(BlockPos.ZERO);
		CelestialCourt.onPrimordialSlain(called, Set.of(player.getUUID()));
		helper.assertTrue(CelestialCourt.meritOf(player.server, player.getUUID()) == before, "Nothing for a beast called up by an egg or a command");
		CelestialCourt.onPrimordialSlain(realms, Set.of(player.getUUID()));
		helper.assertTrue(CelestialCourt.meritOf(player.server, player.getUUID()) == before + CelestialCourt.SLAY_PRIMORDIAL, "Twenty merit for the realm's own fought");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void immortalBloodForSale(GameTestHelper helper) {
		int jade = SpiritStoneItem.Grade.IMMORTAL_JADE.value();
		helper.assertTrue(CultivatorTrades.bloodPrice(Realm.TRUE_IMMORTAL) == 60 * jade
				&& CultivatorTrades.bloodPrice(Realm.TRUE_IMMORTAL) == CultivatorTrades.heavenSlipPrice(HeavenMethod.of(QiElement.FIRE, Realm.TRUE_IMMORTAL)),
				"Sixty jade a True Immortal's blood, as its slip");
		// The ladder's past it (phase 3): ten jade more a realm, a Heavenly Lord's 120, two stacks of jade on the trading screen.
		helper.assertTrue(CultivatorTrades.bloodPrice(Realm.EARTH_IMMORTAL) == 70 * jade && CultivatorTrades.bloodPrice(Realm.HEAVENLY_LORD) == 120 * jade,
				"Seventy jade an Earth Immortal's blood, 120 a Heavenly Lord's");
		ItemStack[] dearest = CultivatorTrades.costsFor(CultivatorTrades.bloodPrice(Realm.HEAVENLY_LORD));
		helper.assertTrue(dearest[0].is(ModItems.IMMORTAL_JADE) && dearest[0].getCount() == 64 && dearest[1].is(ModItems.IMMORTAL_JADE) && dearest[1].getCount() == 56,
				"Paid in two stacks of jade (" + dearest[0] + ", " + dearest[1] + ")");
		CultivatorNpc npc = ModEntities.CULTIVATOR.create(helper.getLevel());
		int rank = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY);
		npc.becomeRogue(rank, 0, helper.getLevel().getRandom());
		npc.setRank(rank, true);
		npc.setTitle(NpcTitle.IMMORTAL_MERCHANT);
		Set<Bloodline> races = EnumSet.noneOf(Bloodline.class);
		for (int n = 0; n < 24; n++) {
			boolean blood = false;
			for (CultivatorTrades.Goods g : CultivatorTrades.goodsFor(npc, RandomSource.create(n))) {
				if (!g.item().is(ModItems.DEMON_BLOOD)) continue;
				blood |= DemonBloodItem.realm(g.item()) == Realm.TRUE_IMMORTAL && g.price() == CultivatorTrades.bloodPrice(Realm.TRUE_IMMORTAL) && g.regard() == Regard.COURTEOUS;
				races.add(DemonBloodItem.race(g.item()));
			}
			helper.assertTrue(blood, "An immortal merchant sells a race's True Immortal blood every day, to the courteous");
		}
		helper.assertTrue(races.size() == 4, "Every demon race's in its turn (" + races + ")");
		List<CultivatorTrades.Goods> sects = CultivatorTrades.immortalBlood(RandomSource.create(3), Regard.WARM);
		helper.assertTrue(!sects.isEmpty() && sects.stream().allMatch(g -> g.regard() == Regard.WARM && DemonBloodItem.realm(g.item()).ordinal() >= Realm.TRUE_IMMORTAL.ordinal()),
				"A sect's elders' the same, to one they are Warm to");
		helper.assertTrue(DemonBlood.carried(Realm.TRUE_IMMORTAL, true) == Realm.TRUE_IMMORTAL && DemonBlood.carried(Realm.TRUE_IMMORTAL, false) == Realm.ENTERING_NIRVANA,
				"A demon immortal carries a True Immortal's blood in the Immortal Realm, none past Entering Nirvana's elsewhere");
		helper.assertTrue(DemonBloodItem.look(DemonBloodItem.of(Bloodline.FOX, Realm.TRUE_IMMORTAL)) == DemonBloodItem.LOOKS - 1
				&& DemonBloodItem.look(DemonBloodItem.of(Bloodline.FOX, Realm.ENTERING_NIRVANA)) == 4 && DemonBloodItem.look(DemonBloodItem.of(Bloodline.FOX, Realm.NASCENT_SOUL)) == 3,
				"A True Immortal's blood in its own phial");
		npc.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
	public void oneOfEachAndTheVenerablesWord(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		PrimordialBeasts.Data data = PrimordialBeasts.Data.get(level.getServer());
		BlockPos[] lairs = lairs(helper);
		List<Component> lines = PrimordialBeasts.directions(data, lairs, level.getGameTime());
		helper.assertTrue(lines.size() == PrimordialBeasts.KINDS.length && lines.size() == 4, "A Venerable's word of each of the four");
		for (int k = 0; k < lines.size(); k++) {
			String id = PrimordialBeasts.KINDS[k].getId(), said = key(lines.get(k));
			helper.assertTrue(said.equals(ModLang.primordialWhere(id)) || said.equals(ModLang.primordialGone(id)), "Where the " + id + " keeps, or when the next comes (" + said + ")");
		}
		int k = PrimordialBeasts.index(BeastKind.KUN_PENG);
		UUID was = data.alive[k];
		long returns = data.returnsAt[k];
		BlockPos stored = data.lairs[k];
		try {
			BlockPos lair = lairs[k];
			data.lairs[k] = lair;
			PrimordialBeast kun = beast(level, BeastKind.KUN_PENG), copy = beast(level, BeastKind.KUN_PENG);
			kun.settle(lair);
			copy.settle(lair);
			data.alive[k] = kun.getUUID();
			helper.assertTrue(PrimordialBeasts.claims(kun) && !PrimordialBeasts.claims(copy), "The realm's own is its one; a copy left behind is not");
			PrimordialBeasts.slain(kun);
			helper.assertTrue(data.alive[k] == null && data.returnsAt[k] == level.getGameTime() + PrimordialBeasts.RETURN_TICKS, "Slain: the next of its kind in five days");
			helper.assertTrue(key(PrimordialBeasts.directions(data, lairs, level.getGameTime()).get(k)).equals(ModLang.primordialGone(BeastKind.KUN_PENG.getId())),
					"A Venerable then tells when the next comes");
			PrimordialBeasts.Data loaded = PrimordialBeasts.Data.load(data.save(new net.minecraft.nbt.CompoundTag()));
			helper.assertTrue(lair.equals(loaded.lairs[k]) && loaded.returnsAt[k] == data.returnsAt[k], "Its lair and its return kept as saved");
		} finally {
			data.alive[k] = was;
			data.returnsAt[k] = returns;
			data.lairs[k] = stored;
			data.setDirty();
		}
		helper.succeed();
	}
}
