package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Where the meditation ways of the methods overhaul's phase 4 gather (CultivationMethods#whyNoGain, MeditationManager), each by where the
 * meditator's body sits (InnerRealm#bodyPos: turned inward, it sits on there):
 * <ul>
 *   <li><b>Dao Field Attunement</b> (Dao Integration): in the Great Void's Dao field of its stage ({@link #fieldFits}: Early wood, Mid fire,
 *   Late earth, Grand Perfection metal or water), {@link #FIELD_PACE} times as fast; anywhere else nothing.</li>
 *   <li><b>Ancestral Communion</b> (the Demon Path, Nascent Soul to Heavenly Being): among the primeval dragon's bones in the Primeval Beast
 *   Graveyard ({@link #BONES} bone blocks within {@link #BONES_REACH}), {@link #BONES_PACE} times as fast (the user: "ancestral communion
 *   should still need a place"); the graveyard stands open twenty minutes in six years, so the faster.</li>
 *   <li><b>Primordial Return</b> (the Demon Path, True Immortal): within {@link #PRIMORDIAL_REACH} of a living primordial beast,
 *   {@link #PRIMORDIAL_PACE} times as fast.</li>
 *   <li><b>Immortal Essence Refining</b> (True Immortal): in the Immortal Realm (or a small world's immortal qi), burning immortal essence, a
 *   {@link #ESSENCE_BURN}th of the pool a second, for {@link #ESSENCE_PACE} times the gain ({@link #burn}); with none left, nothing.</li>
 * </ul>
 */
public final class MethodGrounds {
	public static final double FIELD_PACE = 2, BONES_PACE = 3, PRIMORDIAL_PACE = 2, ESSENCE_PACE = 2;
	public static final int BONES = 4, BONES_REACH = 8, PRIMORDIAL_REACH = 48, ESSENCE_BURN = 60;

	/** Why {@code method}, practised by {@code p}, gathers nothing where its body sits, or null if it gathers (or isn't a place's way). */
	static Component whyNot(ServerPlayer p, PlayerCultivation c, CultivationMethod method) {
		return switch (method) {
			case DAO_FIELD_ATTUNEMENT -> inField(p, c.getStage()) ? null : Component.translatable(ModLang.MSG_DAO_FIELD_OUTSIDE, fieldName(c.getStage()));
			case ANCESTRAL_COMMUNION -> amongBones(p) ? null : Component.translatable(ModLang.MSG_COMMUNION_OUTSIDE);
			case PRIMORDIAL_RETURN -> nearPrimordial(p) ? null : Component.translatable(ModLang.MSG_PRIMORDIAL_FAR, PRIMORDIAL_REACH);
			case IMMORTAL_ESSENCE_REFINING -> !ModDimensions.hasImmortalQi(InnerRealm.bodyDimension(p)) ? Component.translatable(ModLang.MSG_ESSENCE_REFINING_OUTSIDE)
					: c.essence() <= 0 ? Component.translatable(ModLang.MSG_ESSENCE_REFINING_EMPTY) : null;
			default -> null;
		};
	}

	/** The level {@code p}'s body is in (its soul's, unless turned inward). */
	private static ServerLevel bodyLevel(ServerPlayer p) {
		return p.server.getLevel(InnerRealm.bodyDimension(p));
	}

	// --- Dao Field Attunement ---

	/** Whether {@code field} (a Dao field's element: QiElement#daoField) is the one Dao Field Attunement gathers in at {@code stage}. */
	public static boolean fieldFits(Stage stage, QiElement field) {
		if (field == null) return false;
		return switch (stage) {
			case EARLY -> field == QiElement.WOOD;
			case MID -> field == QiElement.FIRE;
			case LATE -> field == QiElement.EARTH;
			case GRAND_PERFECTION -> field == QiElement.METAL || field == QiElement.WATER;
		};
	}

	/** "the wood Dao field": the field (or fields) of {@code stage}, named. */
	public static Component fieldName(Stage stage) {
		return switch (stage) {
			case EARLY -> Component.translatable(ModLang.DAO_FIELD_OF, QiElement.WOOD.getDisplayName());
			case MID -> Component.translatable(ModLang.DAO_FIELD_OF, QiElement.FIRE.getDisplayName());
			case LATE -> Component.translatable(ModLang.DAO_FIELD_OF, QiElement.EARTH.getDisplayName());
			case GRAND_PERFECTION -> Component.translatable(ModLang.DAO_FIELDS_OF, QiElement.METAL.getDisplayName(), QiElement.WATER.getDisplayName());
		};
	}

	/** Whether {@code p}'s body sits in the Great Void's Dao field of {@code stage}. */
	static boolean inField(ServerPlayer p, Stage stage) {
		ServerLevel level = bodyLevel(p);
		return level != null && inField(level, InnerRealm.bodyPos(p), stage);
	}

	/** Whether {@code at} in {@code level} lies in the Great Void's Dao field of {@code stage}. */
	static boolean inField(ServerLevel level, BlockPos at, Stage stage) {
		return ModDimensions.isGreatVoid(level.dimension()) && fieldFits(stage, QiElement.daoField(level.getBiome(at)));
	}

	// --- Ancestral Communion ---

	/** Whether {@code p}'s body sits among the primeval dragon's bones ({@link #amongBones(ServerLevel, BlockPos)}). */
	static boolean amongBones(ServerPlayer p) {
		ServerLevel level = bodyLevel(p);
		return level != null && amongBones(level, InnerRealm.bodyPos(p));
	}

	/** Whether {@code at} in {@code level} lies among the primeval dragon's bones: in the Primeval Beast Graveyard, {@link #BONES} bone blocks near. */
	static boolean amongBones(ServerLevel level, BlockPos at) {
		if (!ModDimensions.isSecretRealm(level.dimension()) || SecretRealm.at(at.getX(), at.getZ()) != SecretRealm.PRIMEVAL_BEAST_GRAVEYARD) return false;
		int bones = 0;
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-BONES_REACH, -BONES_REACH, -BONES_REACH), at.offset(BONES_REACH, BONES_REACH, BONES_REACH))) {
			if (level.getBlockState(pos).is(Blocks.BONE_BLOCK) && ++bones >= BONES) return true;
		}
		return false;
	}

	// --- Primordial Return ---

	/** Whether {@code p}'s body sits within {@link #PRIMORDIAL_REACH} of a living primordial beast. */
	static boolean nearPrimordial(ServerPlayer p) {
		ServerLevel level = bodyLevel(p);
		return level != null && nearPrimordial(level, InnerRealm.bodyPos(p));
	}

	/** Whether {@code at} in {@code level} lies within {@link #PRIMORDIAL_REACH} of a living primordial beast. */
	static boolean nearPrimordial(ServerLevel level, BlockPos at) {
		return !level.getEntitiesOfClass(PrimordialBeast.class, new AABB(at).inflate(PRIMORDIAL_REACH), beast -> beast.isAlive()).isEmpty();
	}

	// --- Immortal Essence Refining ---

	/**
	 * Half a second of Immortal Essence Refining's burning: a {@link #ESSENCE_BURN}th of {@code c}'s pool a second taken (what there is of it),
	 * and what its meditation gathers is multiplied by the return: {@link #ESSENCE_PACE} with the whole of it burnt, less with less.
	 */
	static double burn(ServerPlayer p, PlayerCultivation c) {
		double want = ImmortalEssence.max(c) / ESSENCE_BURN * 0.5, had = c.essence();
		if (want <= 0 || had <= 0) return 0;
		double burnt = Math.min(had, want);
		if (!p.isCreative()) c.setEssence(had - burnt);
		return ESSENCE_PACE * burnt / want;
	}

	private MethodGrounds() {}
}
