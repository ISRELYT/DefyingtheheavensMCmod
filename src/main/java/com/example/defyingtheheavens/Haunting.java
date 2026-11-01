package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Haunting (CultivationMethod#HAUNTING; the methods overhaul's phase 3): one of the dead whose ghost path practises it haunts the living's
 * villages by night, and their fear feeds it. It may go down the Soul River as a Ghost King may ({@link #crosses}), and comes out among the
 * living where it died ({@link #arrive}; else at the world's spawn), touching nothing still; dawn drives it back to the Ghost Gate
 * ({@link #second}). Every {@link #EVERY} ticks of night the villagers within {@link #REACH} it hasn't frightened in the last
 * {@link #FRIGHT_COOLDOWN} take fright (they flee in panic; it shows itself in cold fire): each is {@link #SHARE} of the ghost path's stage,
 * halved for each realm the path stands above Qi Refining (CultivationMethods#grant).
 * <p>
 * A village counts its frights ({@link Villages}: a 64-block square's, cooling by half every half day). Past {@link #EXORCIST_AT} it sends for
 * an exorcist, at most one a day: a cultivator with a Peach Wood Sword come to the villagers, whose rite burns any haunting ghost within
 * {@link #RITE_REACH} of it ({@link #RITE_HARM} of its health every two seconds, ghostly harm, which reaches the dead). Past
 * {@link #EMPTIES_AT} a fright drives a villager out of the village for good, one time in {@link #FLEE_ODDS}.
 */
public final class Haunting {
	public static final int EVERY = 200, FRIGHT_COOLDOWN = 1200, FLEE_ODDS = 4;
	public static final double REACH = 12, SHARE = 0.02, EXORCIST_AT = 20, EMPTIES_AT = 60, RITE_REACH = 24;
	public static final float RITE_HARM = 0.1f;
	/** The tag an exorcist sent for carries (CultivatorNpc). */
	public static final String EXORCIST_TAG = "dth_exorcist";

	/** When each villager was last frightened (game time); not saved. */
	private static final Map<UUID, Long> FRIGHTENED = new HashMap<>();

	/** Whether {@code player} crosses between the worlds as the dead's kings do: one of the common dead whose ghost path practises Haunting. */
	public static boolean crosses(Player player) {
		return player instanceof ServerPlayer sp && Incorporeal.isRuledGhost(sp)
				&& CultivationMethods.practised(CultivationManager.get(sp), TwoPaths.Path.GHOST) == CultivationMethod.HAUNTING;
	}

	/** Whether {@code p} haunts now: one of the dead in earnest, practising Haunting, holding its volume. */
	public static boolean haunts(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return Incorporeal.isGhost(p) && CultivationMethods.practised(c, TwoPaths.Path.GHOST) == CultivationMethod.HAUNTING
				&& CultivationMethods.holdsVolume(c, TwoPaths.Path.GHOST);
	}

	/** Down the Soul River to haunt the living (SoulRiver#arrive): out where it died, in a world of the living, else at the world's spawn. */
	static void arrive(MinecraftServer server, ServerPlayer p) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		ServerLevel level = null;
		BlockPos at = null;
		if (s.hasDeathPlace()) {
			ResourceLocation id = ResourceLocation.tryParse(s.deathDimension());
			ServerLevel died = id == null ? null : server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
			if (died != null && livingWorld(died)) {
				level = died;
				at = s.deathPos();
			}
		}
		if (level == null) {
			level = server.overworld();
			at = level.getSharedSpawnPos();
		}
		BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
		p.teleportTo(level, ground.getX() + 0.5, ground.getY() + 0.1, ground.getZ() + 0.5, p.getYRot(), 0);
		p.fallDistance = 0;
		p.sendSystemMessage(Component.translatable(ModLang.MSG_HAUNT_ARRIVE).withStyle(ChatFormatting.DARK_AQUA));
	}

	/** A world the living live in, with nights to haunt (not the Netherworld, nor a passage, nor one without a sky's day). */
	static boolean livingWorld(Level level) {
		return !ModDimensions.isNetherworld(level.dimension()) && !ModDimensions.isPassage(level.dimension()) && level.dimensionType().hasSkyLight()
				&& !level.dimensionType().hasFixedTime();
	}

	/**
	 * Each second of a player (NetherworldManager#second): a haunting ghost in a world of the living is driven back to the Ghost Gate by day;
	 * by night an exorcist's rite burns it, and every {@link #EVERY} ticks it frightens the villagers about it.
	 */
	static void second(ServerPlayer p) {
		if (!Incorporeal.isRuledGhost(p)) return;
		ServerLevel level = p.serverLevel();
		if (!livingWorld(level)) return;
		if (!level.isNight()) {
			// Dawn: the light drives the dead back down (none of the common dead stays among the living by day).
			ServerLevel netherworld = p.server.getLevel(ModDimensions.NETHERWORLD);
			if (netherworld == null) return;
			BlockPos gate = GhostGate.at(netherworld, GhostGate.ARRIVAL);
			p.teleportTo(netherworld, gate.getX() + 0.5, gate.getY(), gate.getZ() + 0.5, 0, 0);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_HAUNT_DAWN).withStyle(ChatFormatting.GRAY));
			return;
		}
		if (!haunts(p)) return;
		long now = level.getGameTime();
		if (now % 40 < 20) exorcised(p, level);
		if (now % EVERY < 20) frighten(p, level, now);
	}

	/** {@code p} frightens the villagers about it that haven't been in the last {@link #FRIGHT_COOLDOWN}, and is fed on their fear. */
	static int frighten(ServerPlayer p, ServerLevel level, long now) {
		List<Villager> villagers = level.getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(REACH),
				v -> v.isAlive() && now - FRIGHTENED.getOrDefault(v.getUUID(), Long.MIN_VALUE / 2) >= FRIGHT_COOLDOWN);
		if (villagers.isEmpty()) return 0;
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
		level.playSound(null, p.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.0f, 0.7f);
		Villages villages = Villages.get(level.getServer());
		Villager first = villagers.get(0);
		for (Villager villager : villagers) {
			FRIGHTENED.put(villager.getUUID(), now);
			villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HURT_BY, level.damageSources().generic(), 200); // it panics and flees
			level.sendParticles(ParticleTypes.SOUL, villager.getX(), villager.getY() + 1.6, villager.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
			double heat = villages.fright(level, villager.blockPosition(), now);
			if (heat >= EMPTIES_AT && villager != first && level.random.nextInt(FLEE_ODDS) == 0) {
				level.sendParticles(ParticleTypes.POOF, villager.getX(), villager.getY() + 1, villager.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
				villager.discard(); // gone from the haunted village for good
				p.displayClientMessage(Component.translatable(ModLang.MSG_HAUNT_FLED).withStyle(ChatFormatting.DARK_AQUA), true);
			}
		}
		Realm realm = GhostMethods.ghostRealm(CultivationManager.get(p).netherworld());
		CultivationMethods.grant(p, TwoPaths.Path.GHOST, CultivationMethod.HAUNTING, share(villagers.size(), realm == null ? 0 : realm.ordinal()));
		if (villages.heat(level, first.blockPosition(), now) >= EXORCIST_AT && villages.sendsForExorcist(level, first.blockPosition(), now)) {
			callExorcist(p, level, first);
		}
		return villagers.size();
	}

	/** The share of its ghost path's stage {@code frightened} villagers' fear is to a ghost whose path stands in {@code realm} (ordinal). */
	public static double share(int frightened, int realm) {
		return SHARE * frightened / Math.pow(2, realm);
	}

	/** An exorcist comes to the village (a cultivator a little above the ghost, a Peach Wood Sword in hand). */
	static CultivatorNpc callExorcist(ServerPlayer ghost, ServerLevel level, Villager near) {
		CultivatorNpc npc = ModEntities.CULTIVATOR.create(level);
		if (npc == null) return null;
		BlockPos at = near.blockPosition();
		npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
		int rank = Math.max(PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), GhostMethods.ghostRank(CultivationManager.get(ghost).netherworld()) + 2);
		npc.becomeRogue(rank, Alignment.RIGHTEOUS_ABOVE + 10, level.getRandom());
		npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PEACH_WOOD_SWORD));
		npc.addTag(EXORCIST_TAG);
		level.addFreshEntity(npc);
		ghost.sendSystemMessage(Component.translatable(ModLang.MSG_HAUNT_EXORCIST).withStyle(ChatFormatting.RED));
		return npc;
	}

	/** An exorcist's rite within {@link #RITE_REACH} burns {@code p}. */
	static boolean exorcised(ServerPlayer p, ServerLevel level) {
		List<CultivatorNpc> exorcists = level.getEntitiesOfClass(CultivatorNpc.class, p.getBoundingBox().inflate(RITE_REACH),
				npc -> npc.isAlive() && npc.getTags().contains(EXORCIST_TAG));
		if (exorcists.isEmpty()) return false;
		CultivatorNpc exorcist = exorcists.get(0);
		exorcist.getLookControl().setLookAt(p, 30, 30);
		level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
		p.hurt(ModDamageTypes.ghostly(level, exorcist), p.getMaxHealth() * RITE_HARM);
		p.displayClientMessage(Component.translatable(ModLang.MSG_HAUNT_RITE).withStyle(ChatFormatting.RED), true);
		return true;
	}

	/** A player left. */
	public static void forget(UUID id) {
		FRIGHTENED.remove(id);
	}

	/**
	 * The villages' frights, a 64-block square of a level each (saved with the Overworld's data): how haunted it is, cooling by half every
	 * {@link #HALF_LIFE} ticks, and when it last sent for an exorcist.
	 */
	public static final class Villages extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_haunted_villages";
		public static final long HALF_LIFE = 12_000, EXORCIST_EVERY = 24_000;
		private final Map<String, double[]> squares = new HashMap<>();

		public static Villages get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(Villages::load, Villages::new, NAME);
		}

		private static String key(Level level, BlockPos pos) {
			return level.dimension().location() + "|" + (pos.getX() >> 6) + "|" + (pos.getZ() >> 6);
		}

		/** [heat, when, exorcist's time] of the square, its heat cooled to {@code now}. */
		private double[] square(Level level, BlockPos pos, long now) {
			double[] square = squares.computeIfAbsent(key(level, pos), k -> new double[] {0, now, Long.MIN_VALUE / 2.0});
			square[0] *= Math.pow(0.5, Math.max(0, now - square[1]) / (double) HALF_LIFE);
			square[1] = now;
			return square;
		}

		/** How haunted the village at {@code pos} is now. */
		public double heat(Level level, BlockPos pos, long now) {
			return square(level, pos, now)[0];
		}

		/** A fright in the village at {@code pos}: @return how haunted it is now */
		public double fright(Level level, BlockPos pos, long now) {
			double[] square = square(level, pos, now);
			square[0] += 1;
			setDirty();
			return square[0];
		}

		/** The village at {@code pos} forgotten (the game tests': their squares out of the kept test world's record). */
		void clear(Level level, BlockPos pos) {
			if (squares.remove(key(level, pos)) != null) setDirty();
		}

		/** Whether the village at {@code pos} sends for an exorcist now (one a day at most), noted if so. */
		public boolean sendsForExorcist(Level level, BlockPos pos, long now) {
			double[] square = square(level, pos, now);
			if (now - square[2] < EXORCIST_EVERY) return false;
			square[2] = now;
			setDirty();
			return true;
		}

		public static Villages load(CompoundTag tag) {
			Villages villages = new Villages();
			CompoundTag all = tag.getCompound("Squares");
			for (String key : all.getAllKeys()) {
				long[] values = all.getLongArray(key);
				if (values.length == 3) villages.squares.put(key, new double[] {Double.longBitsToDouble(values[0]), values[1], values[2]});
			}
			return villages;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			CompoundTag all = new CompoundTag();
			for (Map.Entry<String, double[]> entry : squares.entrySet()) {
				double[] v = entry.getValue();
				all.putLongArray(entry.getKey(), new long[] {Double.doubleToLongBits(v[0]), (long) v[1], (long) v[2]});
			}
			tag.put("Squares", all);
			return tag;
		}
	}

	private Haunting() {}
}
