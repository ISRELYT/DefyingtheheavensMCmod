package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/**
 * Who keeps a blood pool (BloodPoolFeature; the realm plan, 2026-10-25: "Void Shattering to Entering Nirvana blood, guarded by elder
 * beasts"): the first time anyone comes within {@link #WAKE_REACH} of a pool whose phial still lies on its altar (in its frame), the elder
 * of the blood's race rises out of it (DemonBlood#elder: the Elder White Tiger, the Nine-Tailed Fox, the Mountain-Bearing Lizard King, the
 * Azure Dragon), of the blood's realm, any stage. It keeps to its pool (DemonBeast#guard) and goes for whoever comes near (its kind is
 * territorial), unless they stand a realm or more above it. Beaten, the phial is theirs to take; no other elder rises there (its frame
 * is marked, {@link #KEPT}).
 */
public final class BloodPools {
	/** A pool's elder rises when someone comes this near its altar; those this near hear it. */
	static final double WAKE_REACH = 28, HEARD = 64;
	/** The mark on a pool's frame once its elder has risen. */
	static final String KEPT = DefyingTheHeavens.MOD_ID + ".pool_kept";

	/** Every two seconds: round each player in the Great Void, a pool not yet kept gets its elder. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 40 != 31) return;
		ServerLevel level = server.getLevel(ModDimensions.GREAT_VOID);
		if (level == null) return;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			for (GlowItemFrame frame : level.getEntitiesOfClass(GlowItemFrame.class, player.getBoundingBox().inflate(WAKE_REACH),
					f -> f.getItem().is(ModItems.DEMON_BLOOD) && !f.getTags().contains(KEPT))) {
				frame.addTag(KEPT);
				rise(level, frame.blockPosition().below(), frame.getItem(), level.getRandom());
			}
		}
	}

	/**
	 * The elder of the race whose {@code blood} lies on the altar at {@code altar} rising out of its pool beside it (the dragon over it):
	 * of the blood's realm, any stage, set to guard the pool. @return it, or null (blood of no race, or no room for it)
	 */
	static DemonBeast rise(ServerLevel level, BlockPos altar, ItemStack blood, RandomSource random) {
		Bloodline race = DemonBloodItem.race(blood);
		BeastKind kind = race == null ? null : DemonBlood.elder(race);
		if (kind == null) return null;
		DemonBeast elder = ModEntities.beast(kind).create(level);
		if (elder == null) return null;
		int stage = random.nextInt(Stage.values().length);
		elder.setRank(Mth.clamp(PlayerCultivation.rank(DemonBloodItem.realm(blood), Stage.byIndex(stage)), kind.tier.minRank, kind.tier.maxRank));
		elder.finalizeSpawn(level, level.getCurrentDifficultyAt(altar), MobSpawnType.EVENT, null, null);
		// Out of the blood beside the island (clear of it by half its own width: it is huge), the way there's room for it; the dragon
		// rising into the air over it.
		boolean flies = kind.moves == BeastKind.Moves.FLYING;
		boolean placed = false;
		double start = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < 8 && !placed; i++) {
			double angle = start + i * Math.PI / 4, out = flies ? 0 : 2.5 + elder.getBbWidth() / 2;
			double x = altar.getX() + 0.5 + Math.cos(angle) * out, z = altar.getZ() + 0.5 + Math.sin(angle) * out;
			double y = flies ? altar.getY() + 6 : altar.getY() - 1.5;
			elder.moveTo(x, y, z, (float) Math.toDegrees(angle) + 90, 0);
			placed = level.noCollision(elder);
		}
		if (!placed) elder.moveTo(altar.getX() + 0.5, altar.getY() + 3, altar.getZ() + 0.5, random.nextFloat() * 360, 0);
		elder.guard(altar);
		level.addFreshEntity(elder);
		level.sendParticles(new DustParticleOptions(new Vector3f(0.55f, 0.02f, 0.04f), 2.0f), elder.getX(), elder.getY() + 0.5, elder.getZ(),
				80, elder.getBbWidth() * 0.6, 0.8, elder.getBbWidth() * 0.6, 0.05);
		level.sendParticles(ParticleTypes.SPLASH, elder.getX(), altar.getY(), elder.getZ(), 60, elder.getBbWidth() * 0.7, 0.2, elder.getBbWidth() * 0.7, 0.4);
		level.playSound(null, elder.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 0.6f);
		for (ServerPlayer near : level.players()) {
			if (near.distanceToSqr(elder) > HEARD * HEARD) continue;
			near.displayClientMessage(Component.translatable(ModLang.MSG_ELDER_RISES, kind.getDisplayName()).withStyle(ChatFormatting.DARK_RED), true);
		}
		return elder;
	}

	private BloodPools() {}
}
