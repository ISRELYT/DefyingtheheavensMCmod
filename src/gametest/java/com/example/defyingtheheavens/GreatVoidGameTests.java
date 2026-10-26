package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import java.util.List;

/**
 * The Great Void Realm (phase 1 of the realm plan, 2026-10-25): its Holy Lands' twenty (a Sacred Vessel Patriarch, two of Void
 * Shattering, four of Dao Integration, Heavenly Being to Four Axis disciples), their names and their white jade and gold; its Dao
 * fields' laws (each field's element, its spells the stronger there and the element it overcomes the weaker); a Holy Land's or a
 * palace's barrier as wide as its grounds need there and only there; its realms' cultivation needs a hundred times their base, a Dao
 * Integration's ten.
 */
public class GreatVoidGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void aHolyLandsTwenty(GameTestHelper helper) {
		RandomSource random = RandomSource.create(17);
		for (int trial = 0; trial < 20; trial++) {
			int[] ranks = HolyLands.foundingRanks(random, HolyLands.MEMBERS);
			helper.assertTrue(ranks.length == HolyLands.MEMBERS, "Twenty founded");
			helper.assertTrue(realm(ranks[0]) == Realm.SACRED_VESSEL, "Its Patriarch of Sacred Vessel: " + realm(ranks[0]));
			for (int i = 1; i <= 2; i++) helper.assertTrue(realm(ranks[i]) == Realm.VOID_SHATTERING, "Two of Void Shattering: " + realm(ranks[i]));
			for (int i = 3; i <= 6; i++) helper.assertTrue(realm(ranks[i]) == Realm.DAO_INTEGRATION, "Four of Dao Integration: " + realm(ranks[i]));
			for (int i = 7; i < ranks.length; i++) {
				helper.assertTrue(ranks[i] >= HolyLands.FLOOR && ranks[i] <= PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION),
						"Its disciples of Heavenly Being to Four Axis: " + realm(ranks[i]));
			}
		}
		for (Alignment.Faction faction : Alignment.Faction.values()) {
			helper.assertTrue(SectNames.holy(random, faction).endsWith(" Holy Land"), "A Holy Land named as one");
			helper.assertTrue(SectPalette.holy(faction).accent == Blocks.GOLD_BLOCK, "A Holy Land's accents of gold");
		}
		// Its layout: the palace's throne for its master, the blueprint's twenty.
		List<StructurePiece> pieces = SectStructure.layOut(5L, 0, Direction.NORTH, true, true);
		CompoundTag blueprint = null;
		int palaces = 0;
		for (StructurePiece piece : pieces) {
			SectPiece sect = (SectPiece) piece;
			if (sect.kind() == SectPiece.Kind.PLAZA) blueprint = sect.extra().getCompound("Blueprint");
			if (sect.kind() == SectPiece.Kind.HOLY_PALACE) palaces++;
		}
		helper.assertTrue(palaces == 1 && blueprint != null && blueprint.getInt("Capacity") == HolyLands.MEMBERS
				&& blueprint.getCompound("Posts").getLongArray("MASTER_SEAT").length == 1, "A Holy Land's palace and room for its twenty");
		helper.succeed();
	}

	private static Realm realm(int rank) {
		return Realm.values()[rank / Stage.values().length];
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDaoFieldsLaws(GameTestHelper helper) {
		var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		Object[][] fields = {{ModBiomes.DAO_FIELD_WOOD, QiElement.WOOD}, {ModBiomes.DAO_FIELD_FIRE, QiElement.FIRE}, {ModBiomes.DAO_FIELD_EARTH, QiElement.EARTH},
				{ModBiomes.DAO_FIELD_METAL, QiElement.METAL}, {ModBiomes.DAO_FIELD_WATER, QiElement.WATER}};
		for (Object[] field : fields) {
			@SuppressWarnings("unchecked")
			Holder<Biome> biome = biomes.getHolderOrThrow((ResourceKey<Biome>) field[0]);
			helper.assertTrue(QiElement.daoField(biome) == field[1], "Each Dao field its element's: " + field[0]);
		}
		for (ResourceKey<Biome> other : List.of(ModBiomes.VOID_STEPPES, ModBiomes.ANCIENT_BATTLEFIELD, ModBiomes.SHORE_OF_NIRVANA)) {
			helper.assertTrue(QiElement.daoField(biomes.getHolderOrThrow(other)) == null, "No law runs strong elsewhere: " + other);
		}
		helper.assertTrue(HeavenSpells.DAO_FIELD_OWN > 1 && HeavenSpells.DAO_FIELD_AGAINST < 1, "Its own element's spells the stronger, the overcome's the weaker");
		int overcome = 0;
		for (QiElement a : QiElement.values()) {
			for (QiElement b : QiElement.values()) if (a != QiElement.YIN && b != QiElement.YIN && a.overcomes(b)) overcome++;
		}
		helper.assertTrue(overcome == 5, "Each of the five overcomes one (the five phases' cycle): " + overcome);
		helper.assertTrue(SacredVesselItem.STRENGTH > 1, "A Sacred Vessel strengthens its element's spells");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void itsBarriersAndItsNeeds(GameTestHelper helper) {
		// (The game-test server has no Great Void of its own to ask: the dimension's own check, and the Overworld's barrier a sect's.)
		ServerLevel overworld = helper.getLevel().getServer().getLevel(Level.OVERWORLD);
		helper.assertTrue(ModDimensions.isGreatVoid(ModDimensions.GREAT_VOID) && !ModDimensions.isGreatVoid(Level.OVERWORLD)
				&& Formations.maxRadius(overworld) == Formations.MAX_RADIUS && Formations.HOLY_MAX_RADIUS >= DemonPalacePlan.REACH + 2
				&& Formations.HOLY_MAX_RADIUS > Formations.MAX_RADIUS, "A barrier as wide as a Holy Land's or a palace's grounds need in the Great Void, a sect's elsewhere");
		for (Realm realm : Realm.values()) {
			double need = PlayerCultivation.qiNeed(realm);
			double want = realm.isImmortal() ? PlayerCultivation.IMMORTAL_QI_MULTIPLIER // True Immortal and the ladder past it (phase 3)
					: realm.ordinal() >= Realm.VOID_SHATTERING.ordinal() ? PlayerCultivation.GREAT_VOID_QI_MULTIPLIER
					: PlayerCultivation.isUpperRealmOnly(realm) ? PlayerCultivation.UPPER_REALM_QI_MULTIPLIER : 1;
			helper.assertTrue(need == want, "Its needs offset the qi of the place that sustains it: " + realm + " " + need);
		}
		helper.assertTrue(PlayerCultivation.qiNeed(Realm.DAO_INTEGRATION) == PlayerCultivation.UPPER_REALM_QI_MULTIPLIER
				&& PlayerCultivation.qiNeed(Realm.VOID_SHATTERING) == 10 * PlayerCultivation.qiNeed(Realm.DAO_INTEGRATION),
				"Void Shattering ten times again (a Dao Integration carried up races ahead)");
		helper.succeed();
	}
}
