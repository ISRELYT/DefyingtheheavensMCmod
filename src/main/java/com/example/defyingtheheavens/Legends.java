package com.example.defyingtheheavens;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The record of the legendary weapons (Legend; phase 3 of the Artifact Forge): one of each in the whole world (the user's pick), where it
 * is and who has it, kept once for the server.
 * <ul>
 *   <li><b>One true stack.</b> Each time a legend is made (first laid in its resting place, reformed, or given by an op: {@link #issue})
 *   its serial rises, and the stack made carries it (LegendaryWeaponItem#SERIAL); any other stack of it, an older one or one taken from
 *   the creative menu, is a hollow echo with no power ({@link #isTrue}).</li>
 *   <li><b>Where it is</b> ({@link State}): resting at home (in its stone), held by a named immortal, abroad (a player's, or lying where
 *   it fell), or lost.</li>
 *   <li><b>Lost</b>: a stack of it that can't burn or be blown up gone all the same (out of the world, a dropped stack left to vanish):
 *   it reforms at home a day later ({@link #REFORM_AFTER}), its trial or holder there again.</li>
 *   <li><b>Resting places</b>, chosen once per world by its seed ({@link #site}) and laid the first time someone comes near
 *   ({@link #LAY_NEAR}; LegendSites): Ganjiang's sword tomb on a mountain of the Overworld, Moye's yin pool among the Other Shore's
 *   flowers in the Netherworld. The Tribulation Spear has none: mad Lei Zhen carries it about the Immortal Realm under his storm, its
 *   site where he is and its way where he goes (LeiZhen). Phase 3b's: the Xuanyuan Sword's jade shrine on the Jade Firmament's highest
 *   peak (drawn by the righteous alone, Chiyou's shade rising against whoever draws it: {@link #chiyou}); the dead sun's crater on the
 *   barren world nearest the Overworld, the Sun-Shooting Bow in its cinder and the Golden Crow keeping it ({@link #crowCrater}); the
 *   Sea-Fixing Needle standing as a pillar over the Immortal Realm's sea of clouds, pulled free by one of Entering Nirvana, the sea
 *   heaving and Ao Guang rising for it ({@link #pull}). The guards a legend calls up hunt their one prey (LegendGuards). Phase 3c's:
 *   Pangu's Axe in the heart of the Primordial Chaos, falling out of it when it is slain ({@link #fromTheChaos}); the five Venerables'
 *   weapons in their Venerables' hands at the Celestial Court ({@link #venerables}), won in Dao Duels ({@link #duelFor}, {@link #handOver}).</li>
 *   <li>The Sea-Fixing Needle is kept behind its owner's ear: never dropped at their death, it waits in the record ({@link Entry#kept})
 *   and is theirs again as they live again ({@link #keepBehindTheEar}, {@link #giveBack}).</li>
 *   <li>Every two seconds ({@link #tick}): the tomb's trial (two waves of sword spirits, then the sword may be drawn), Moye's ghost rising
 *   to keep her sword, Lei Zhen's roaming and madness, the Tribulation Spear's taming (three bolts on its new owner, {@link #tame}), and
 *   the lost reformed.</li>
 *   <li>The bolts the Tribulation Spear has drunk in its owners' tribulations ({@link Entry#drunk}): kept here, so a reformed spear keeps
 *   the strength they gave it, and shown on its stack (LegendaryWeaponItem#DRUNK).</li>
 *   <li>Rumours of each (Rumours.Kind#LEGEND), noted whenever it moves.</li>
 * </ul>
 * Bonding and awakening, and each legend's powers: LegendaryWeaponItem.
 */
public final class Legends extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_legends";
	/** A resting place is laid when someone first comes this near it (sideways). */
	static final double LAY_NEAR = 96;
	/** The tomb's spirits wake for a player this near its stone, and leave if nobody is within {@link #TRIAL_LEAVE}. */
	static final double TRIAL_REACH = 12, TRIAL_LEAVE = 40;
	/** Moye's ghost rises for a player this near her pool's stone. */
	static final double WAKE_REACH = 14;
	/** A lost legend reforms at home this long after (ticks: a day). */
	public static final long REFORM_AFTER = 24_000;
	/** The tomb's two waves of sword spirits. */
	static final int[] WAVES = {3, 4};
	/** The strength Moye's ghost stands at: Core Formation Grand Perfection, at the top of the swords' own lowest realm. */
	static final int MOYE_RANK = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION);
	/** Moye's name (a proper name: the same in every language). */
	public static final String MOYE_NAME = "Moye";
	/** The Golden Crow rises out of the dead sun's cinder for a player this near its stone. */
	static final double CROW_WAKE = 24;
	/** Chiyou's shade's strength: Nascent Soul Grand Perfection, at the top of the Xuanyuan Sword's own lowest realm; it rises this far from the stone. */
	static final int CHIYOU_RANK = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION);
	static final double CHIYOU_RISES = 4;
	/** Ao Guang's strength (True Immortal Early), his path, his name; he rises out of the clouds this far from where the pillar stood. */
	static final int AO_GUANG_RANK = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY);
	static final int AO_GUANG_ALIGNMENT = 80;
	public static final String AO_GUANG_NAME = "Ao Guang";
	static final double AO_GUANG_RISES = 10;
	/** The sea's surge as the Needle comes out of it: everyone within this is thrown back. */
	static final double SURGE_REACH = 24;
	/**
	 * The tag a cultivator made to keep a legend carries (Moye's ghost, Lei Zhen): one the record no longer names its keeper goes
	 * ({@link #stray}).
	 */
	public static final String HOLDER_TAG = DefyingTheHeavens.MOD_ID + "_legend_holder";

	/** Where a legend is. Saved by name: new ones go at the end. */
	public enum State { RESTING, HELD, ABROAD, LOST }

	public static final class Entry {
		public final Legend legend;
		int serial;
		State state = State.RESTING;
		/** Its resting place's floor, in its home world (null until chosen). */
		BlockPos site;
		boolean laid;
		/** HELD: the named immortal carrying it; ABROAD: the player who last held it (null: lying where it fell). */
		UUID holder;
		String holderName = "";
		long lostAt;
		/** The Tribulation Spear's: the next point of Lei Zhen's way (LeiZhen), its site being where he is. */
		BlockPos toward;
		/** The Tribulation Spear's: the bolts it has drunk in its owners' tribulations, all told (LegendaryWeaponItem#drink). */
		int drunk;
		/** The Sea-Fixing Needle's: kept behind its owner's ear while they are dead, their {@link #holder} (#keepBehindTheEar), or empty. */
		ItemStack kept = ItemStack.EMPTY;

		Entry(Legend legend) {
			this.legend = legend;
		}

		public State state() { return state; }
		public int serial() { return serial; }
		public @Nullable BlockPos site() { return site; }
		public boolean laid() { return laid; }
		public @Nullable UUID holder() { return holder; }
		public String holderName() { return holderName; }
		public @Nullable BlockPos toward() { return toward; }
		public int drunk() { return drunk; }

		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putInt("Serial", serial);
			tag.putString("State", state.name());
			if (site != null) tag.putLong("Site", site.asLong());
			tag.putBoolean("Laid", laid);
			if (holder != null) tag.putUUID("Holder", holder);
			tag.putString("HolderName", holderName);
			tag.putLong("LostAt", lostAt);
			if (toward != null) tag.putLong("Toward", toward.asLong());
			if (drunk > 0) tag.putInt("Drunk", drunk);
			if (!kept.isEmpty()) tag.put("Kept", kept.save(new CompoundTag()));
			return tag;
		}

		void load(CompoundTag tag) {
			serial = tag.getInt("Serial");
			try {
				state = State.valueOf(tag.getString("State"));
			} catch (IllegalArgumentException unknown) {
				state = State.RESTING;
			}
			site = tag.contains("Site") ? BlockPos.of(tag.getLong("Site")) : null;
			laid = tag.getBoolean("Laid");
			holder = tag.hasUUID("Holder") ? tag.getUUID("Holder") : null;
			holderName = tag.getString("HolderName");
			lostAt = tag.getLong("LostAt");
			toward = tag.contains("Toward") ? BlockPos.of(tag.getLong("Toward")) : null;
			drunk = Math.max(0, tag.getInt("Drunk"));
			kept = tag.contains("Kept") ? ItemStack.of(tag.getCompound("Kept")) : ItemStack.EMPTY;
		}
	}

	private final EnumMap<Legend, Entry> entries = new EnumMap<>(Legend.class);
	/** Who is bonding with which legend, and how long they have meditated with it so far (not saved: a restart begins it again). */
	private static final Map<UUID, Map<Legend, Integer>> BONDING = new HashMap<>();
	/** The Tribulation Spear's new owners being tamed: bolts left to survive (LegendaryWeaponItem#bond on the last). */
	private static final Map<UUID, Integer> TAMING = new HashMap<>();

	public Legends() {
		for (Legend legend : Legend.values()) entries.put(legend, new Entry(legend));
	}

	public static Legends get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Legends::load, Legends::new, NAME);
	}

	static Legends load(CompoundTag tag) {
		Legends data = new Legends();
		for (Legend legend : Legend.values()) if (tag.contains(legend.getId())) data.entries.get(legend).load(tag.getCompound(legend.getId()));
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		for (Entry entry : entries.values()) tag.put(entry.legend.getId(), entry.save());
		return tag;
	}

	public Entry entry(Legend legend) {
		return entries.get(legend);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Legends::tick);
		// A stack of a legend gone for good (fallen out of the world, a dropped stack left to vanish): lost, to reform at home.
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof ItemEntity item && entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()
					&& !item.getItem().isEmpty() && item.getItem().getItem() instanceof LegendaryWeaponItem) {
				lost(level.getServer(), item.getItem());
			}
		});
	}

	// --- The one true stack ---

	/** A new true stack of {@code legend}: its serial raised, so every older stack of it is a hollow echo from now on. */
	public static ItemStack issue(MinecraftServer server, Legend legend) {
		Legends data = get(server);
		Entry entry = data.entry(legend);
		entry.serial++;
		data.setDirty();
		return restack(entry);
	}

	/**
	 * {@code entry}'s true stack made again as it stands, its serial as it is (Lei Zhen come back into the world: the one he carried out of it
	 * went with him), with the bolts it has drunk.
	 */
	static ItemStack restack(Entry entry) {
		return LegendaryWeaponItem.create(entry.legend, entry.serial, entry.drunk);
	}

	/** Whether {@code stack} is its legend's one true stack (not a hollow echo). */
	public static boolean isTrue(@Nullable MinecraftServer server, ItemStack stack) {
		if (server == null || !(stack.getItem() instanceof LegendaryWeaponItem weapon)) return false;
		int serial = LegendaryWeaponItem.serial(stack);
		return serial > 0 && serial == get(server).entry(weapon.legend()).serial;
	}

	// --- Where it is ---

	/** {@code player} has {@code stack} on them (LegendaryWeaponItem#inventoryTick, now and then): abroad, theirs. */
	static void seen(ServerPlayer player, Legend legend) {
		Milestones.award(player, Milestones.legendHeard(legend)); // its Codex page opens for one who has it in hand, heard of or not
		Legends data = get(player.server);
		Entry entry = data.entry(legend);
		if (entry.state == State.ABROAD && player.getUUID().equals(entry.holder)) return;
		entry.state = State.ABROAD;
		entry.holder = player.getUUID();
		entry.holderName = player.getGameProfile().getName();
		data.setDirty();
		noteRumour(player.server, entry);
	}

	/** {@code npc}, a legend's holder, is dead: the legend dropped where it fell (its keeper's hand lets go of it: equipment drop). */
	public static void onHolderDeath(CultivatorNpc npc) {
		if (!(npc.level() instanceof ServerLevel level)) return;
		Legends data = get(level.getServer());
		for (Entry entry : data.entries.values()) {
			if (entry.state == State.HELD && npc.getUUID().equals(entry.holder)) {
				entry.state = State.ABROAD;
				entry.holder = null;
				entry.holderName = "";
				data.setDirty();
				noteRumour(level.getServer(), entry);
			}
		}
	}

	/** Whether {@code npc} carries a legend for the world (Moye's ghost, Lei Zhen), as the record says. */
	public static boolean isHolder(CultivatorNpc npc) {
		if (!(npc.level() instanceof ServerLevel level)) return npc.getTags().contains(HOLDER_TAG);
		for (Entry entry : get(level.getServer()).entries.values()) {
			if (entry.state == State.HELD && npc.getUUID().equals(entry.holder)) return true;
		}
		return false;
	}

	/**
	 * Whether {@code npc}, made to keep a legend, is a keeper the record no longer names (an old keeper of a legend since reformed, a Lei
	 * Zhen unloaded with his ground and come again elsewhere): it goes (CultivatorNpc#tick), and what it held with it.
	 */
	public static boolean stray(CultivatorNpc npc) {
		return npc.getTags().contains(HOLDER_TAG) && npc.level() instanceof ServerLevel && !isHolder(npc);
	}

	/** Its owner's tribulation's bolt drunk by the Tribulation Spear (LegendaryWeaponItem#drink): one more all told. */
	static int drank(MinecraftServer server) {
		Legends data = get(server);
		Entry entry = data.entry(Legend.TRIBULATION_SPEAR);
		entry.drunk++;
		data.setDirty();
		return entry.drunk;
	}

	/** A true stack of a legend gone for good: lost, to reform at home a day later. */
	static void lost(MinecraftServer server, ItemStack stack) {
		if (server == null || !isTrue(server, stack) || !(stack.getItem() instanceof LegendaryWeaponItem weapon)) return;
		Legends data = get(server);
		Entry entry = data.entry(weapon.legend());
		entry.state = State.LOST;
		entry.lostAt = server.overworld().getGameTime();
		entry.holder = null;
		entry.holderName = "";
		data.setDirty();
		noteRumour(server, entry);
		DefyingTheHeavens.LOGGER.info("{} lost; it reforms at home in a day", weapon.legend().getId());
	}

	/**
	 * {@code legend} made anew and put back at home (a lost one a day on, or at an op's word: /legend reform): its old stack, wherever it
	 * is, a hollow echo; in its stone again (the tomb's trial begun anew), or Lei Zhen's again (he walks the realm with it once more).
	 */
	public static void reform(MinecraftServer server, Legend legend) {
		Legends data = get(server);
		Entry entry = data.entry(legend);
		entry.state = State.RESTING;
		entry.holder = null;
		entry.holderName = "";
		entry.kept = ItemStack.EMPTY;
		data.setDirty();
		ServerLevel level = server.getLevel(legend.home());
		if (entry.laid && level != null && legend.restsInStone()) {
			putInStone(level, entry, issue(server, legend));
		} else {
			entry.serial++; // the old stack an echo now; the new one is made when it is laid, or with Lei Zhen
			if (legend == Legend.SEA_NEEDLE) entry.laid = false; // its pillar raised over the sea again when someone next comes near (#lay)
		}
		noteRumour(server, entry);
	}

	// --- Its resting place ---

	/**
	 * Where {@code legend}'s resting place goes in {@code level}, by the world's seed: some way out from the world's middle (the
	 * Overworld's spawn, the Netherworld's heart) the nearest land of the kind it lies in: a mountain, the Other Shore's fields. Where none
	 * is found, the spot out there as it is. The Tribulation Spear's, where Lei Zhen first walks (LeiZhen#start).
	 */
	static BlockPos site(ServerLevel level, Legend legend) {
		if (legend == Legend.TRIBULATION_SPEAR) return LeiZhen.start(level);
		if (legend == Legend.XUANYUAN) return LegendSites.peak(level);
		if (legend == Legend.SUN_BOW) return LegendSites.deadStar(level);
		if (legend == Legend.SEA_NEEDLE) return LegendSites.openSea(level);
		// Pangu's Axe in the heart of the Primordial Chaos, at its lair; a Venerable's weapon at the Celestial Court, in its Venerable's hand.
		if (legend == Legend.PANGU_AXE) {
			BlockPos lair = PrimordialBeasts.findLair(level, BeastKind.PRIMORDIAL_CHAOS);
			return new BlockPos(lair.getX(), level.getMinBuildHeight(), lair.getZ());
		}
		if (legend.venerable() != null) return new BlockPos(ModNoiseSettings.COURT_X, level.getMinBuildHeight(), ModNoiseSettings.COURT_Z);
		RandomSource random = RandomSource.create(level.getSeed() ^ 0x5EED1E6E4DL * (legend.ordinal() + 1));
		double angle = random.nextDouble() * Math.PI * 2;
		BlockPos centre;
		double out;
		Predicate<Holder<Biome>> fits;
		if (legend == Legend.GANJIANG) {
			centre = level.getSharedSpawnPos();
			out = 1000 + random.nextInt(1200);
			fits = biome -> biome.is(BiomeTags.IS_MOUNTAIN);
		} else {
			centre = BlockPos.ZERO;
			out = 400 + random.nextInt(400);
			fits = biome -> biome.is(ModBiomes.FIELDS_OF_THE_OTHER_SHORE);
		}
		BlockPos from = new BlockPos(centre.getX() + (int) Math.round(Math.cos(angle) * out), 100, centre.getZ() + (int) Math.round(Math.sin(angle) * out));
		BlockPos at = from;
		try {
			Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(fits, from, 640, 32, 64);
			if (found != null) at = found.getFirst();
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.warn("No land found for {}'s resting place; it lies where it was to", legend.getId(), e);
		}
		return new BlockPos(at.getX(), level.getMinBuildHeight(), at.getZ());
	}

	/** {@code stack} into the stone at {@code entry}'s resting place: resting at home, the tomb's trial begun anew. */
	static void putInStone(ServerLevel level, Entry entry, ItemStack stack) {
		BlockPos stonePos = entry.site.above();
		LegendStoneBlockEntity stone = level.getBlockEntity(stonePos) instanceof LegendStoneBlockEntity found ? found : null;
		if (stone == null) {
			level.setBlock(stonePos, ModBlocks.LEGEND_STONE.defaultBlockState(), 3);
			stone = level.getBlockEntity(stonePos) instanceof LegendStoneBlockEntity made ? made : null;
			if (stone == null) return;
		}
		stone.resetTrial(level);
		stone.setHeld(stack);
	}

	/** Whether anyone (not a spectator) is within {@code reach} of {@code at} sideways. */
	static boolean someoneNear(ServerLevel level, BlockPos at, double reach) {
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			double dx = player.getX() - at.getX() - 0.5, dz = player.getZ() - at.getZ() - 0.5;
			if (dx * dx + dz * dz <= reach * reach) return true;
		}
		return false;
	}

	/** The nearest player (not in creative or spectating) within {@code reach} of {@code at}, or null. */
	static @Nullable ServerPlayer nearest(ServerLevel level, BlockPos at, double reach) {
		ServerPlayer best = null;
		double bestDistance = reach * reach;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || player.isCreative()) continue;
			double d = player.distanceToSqr(Vec3.atCenterOf(at));
			if (d <= bestDistance) {
				best = player;
				bestDistance = d;
			}
		}
		return best;
	}

	/** Whether the ground within {@code reach} of {@code at} is loaded and ticking. */
	static boolean loaded(ServerLevel level, BlockPos at, int reach) {
		for (int dx = -reach; dx <= reach; dx += 2 * reach) {
			for (int dz = -reach; dz <= reach; dz += 2 * reach) {
				if (!level.isPositionEntityTicking(at.offset(dx, 0, dz).atY(level.getMinBuildHeight() + 1))) return false;
			}
		}
		return true;
	}

	// --- Every two seconds ---

	static void tick(MinecraftServer server) {
		try {
			LeiZhen.steer(server);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Lei Zhen's flight failed", e);
		}
		try {
			LegendGuards.tick(server);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("The legends' guards' hunt failed", e);
		}
		if (server.getTickCount() % 40 != 7) return;
		try {
			Legends data = get(server);
			long now = server.overworld().getGameTime();
			// The Needle kept behind a dead owner's ear: theirs again as soon as they live again.
			Entry needle = data.entry(Legend.SEA_NEEDLE);
			if (!needle.kept.isEmpty() && needle.holder != null) {
				ServerPlayer owner = server.getPlayerList().getPlayer(needle.holder);
				if (owner != null) giveBack(owner);
			}
			for (Entry entry : data.entries.values()) {
				ServerLevel level = server.getLevel(entry.legend.home());
				if (level == null) continue;
				if (entry.site == null) {
					entry.site = site(level, entry.legend);
					data.setDirty();
					noteRumour(server, entry);
					DefyingTheHeavens.LOGGER.info("{} rests near {} in {}", entry.legend.getId(), entry.site.toShortString(), level.dimension().location());
				}
				if (entry.state == State.LOST && now - entry.lostAt >= REFORM_AFTER) reform(server, entry.legend);
				if (entry.legend == Legend.TRIBULATION_SPEAR) {
					LeiZhen.tick(level, data, entry); // he roams unseen with nobody in the realm too
					continue;
				}
				// No resting place to lay: Pangu's Axe is in the Primordial Chaos (it falls when the Chaos is slain: #fromTheChaos), a Venerable's
				// weapon in its Venerable's hand (#venerables).
				if (entry.legend == Legend.PANGU_AXE || entry.legend.venerable() != null) continue;
				if (level.players().isEmpty()) continue;
				if (!entry.laid) {
					if (!someoneNear(level, entry.site, LAY_NEAR) || !loaded(level, entry.site, 8)) continue;
					lay(level, data, entry);
				}
				switch (entry.legend) {
					case GANJIANG -> tombTrial(level, entry);
					case MOYE -> moyePool(level, data, entry);
					case SUN_BOW -> crowCrater(level, entry);
					default -> {} // the Xuanyuan Sword's shade rises at its drawing (#draw), Ao Guang at the Needle's pulling (#pull)
				}
			}
			venerables(server, data);
			taming(server);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Legends tick failed", e);
		}
	}

	/** {@code entry}'s resting place laid on the land there (LegendSites), its sword into its stone if it rests at home. */
	static void lay(ServerLevel level, Legends data, Entry entry) {
		int x = entry.site.getX(), z = entry.site.getZ();
		entry.site = switch (entry.legend) {
			case GANJIANG -> LegendSites.tomb(level, x, z);
			case XUANYUAN -> LegendSites.shrine(level, x, z);
			case SUN_BOW -> LegendSites.crater(level, x, z);
			case SEA_NEEDLE -> LegendSites.pillar(level, x, z);
			default -> LegendSites.pool(level, x, z);
		};
		entry.laid = true;
		data.setDirty();
		if (entry.state == State.RESTING && entry.legend.restsInStone()) putInStone(level, entry, issue(level.getServer(), entry.legend));
		noteRumour(level.getServer(), entry);
		DefyingTheHeavens.LOGGER.info("{}'s resting place laid at {}", entry.legend.getId(), entry.site.toShortString());
	}

	/**
	 * Ganjiang's sword tomb: a player come near the stone while the sword is in it wakes its sword spirits, a wave of {@link #WAVES}[0]
	 * and, when those are beaten, a second of [1]; beat that, and the sword may be drawn. Nobody near, the spirits fade and it waits, its
	 * trial to be won again (drawn or not).
	 */
	static void tombTrial(ServerLevel level, Entry entry) {
		BlockPos stonePos = entry.site.above();
		if (!level.isLoaded(stonePos) || !(level.getBlockEntity(stonePos) instanceof LegendStoneBlockEntity stone) || trueHeld(level, stone).isEmpty()) return;
		if (!someoneNear(level, stonePos, TRIAL_LEAVE)) {
			if (stone.wave() > 0 || stone.unlocked()) stone.resetTrial(level);
			return;
		}
		if (stone.unlocked()) return;
		List<SwordSpirit> alive = stone.spirits(level);
		if (stone.wave() == 0) {
			ServerPlayer near = nearest(level, stonePos, TRIAL_REACH);
			if (near != null) wave(level, stone, 1);
			return;
		}
		if (!alive.isEmpty()) return;
		if (stone.wave() < WAVES.length) {
			wave(level, stone, stone.wave() + 1);
		} else {
			stone.unlock();
			level.playSound(null, stonePos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.3f);
			level.sendParticles(ParticleTypes.END_ROD, stonePos.getX() + 0.5, stonePos.getY() + 1.5, stonePos.getZ() + 0.5, 30, 0.3, 0.6, 0.3, 0.05);
			tellNear(level, stonePos, Component.translatable(ModLang.LEGEND_TOMB_QUIET, entry.legend.getDisplayName()).withStyle(ChatFormatting.GOLD));
		}
	}

	/** The tomb's wave {@code wave} of sword spirits rising round its stone. */
	static void wave(ServerLevel level, LegendStoneBlockEntity stone, int wave) {
		BlockPos at = stone.getBlockPos();
		stone.setWave(wave);
		int count = WAVES[wave - 1];
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2 * i / count;
			SwordSpirit spirit = SwordSpirit.guardian(level, new Vec3(at.getX() + 0.5 + Math.cos(angle) * 3, at.getY() + 2.5, at.getZ() + 0.5 + Math.sin(angle) * 3), at);
			if (spirit != null) stone.addSpirit(spirit);
		}
		level.playSound(null, at, SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.0f, 1.4f);
		tellNear(level, at, Component.translatable(wave == 1 ? ModLang.LEGEND_TOMB_WAKES : ModLang.LEGEND_TOMB_SECOND).withStyle(ChatFormatting.RED));
	}

	/**
	 * Moye's pool: a player come near the stone while her sword rests in it, her ghost rises from the water with it (a keeper of the
	 * pool, SacredRelics' rule: she goes for any player near it), and keeps it until she is struck down.
	 */
	static void moyePool(ServerLevel level, Legends data, Entry entry) {
		if (entry.state != State.RESTING) return;
		BlockPos stonePos = entry.site.above();
		if (!level.isLoaded(stonePos) || !(level.getBlockEntity(stonePos) instanceof LegendStoneBlockEntity stone) || trueHeld(level, stone).isEmpty()) return;
		if (nearest(level, stonePos, WAKE_REACH) == null) return;
		CultivatorNpc ghost = CultivatorNpc.createFor(level);
		if (ghost == null) return;
		ghost.moveTo(stonePos.getX() + 0.5, stonePos.getY(), stonePos.getZ() + 2.5, 180, 0);
		ghost.becomeRogue(MOYE_RANK, 0, level.getRandom());
		ghost.setTitle(NpcTitle.RELIC_KEEPER);
		ghost.setSettledAt(stonePos);
		ghost.setCustomName(Component.literal(MOYE_NAME));
		hold(ghost, stone.take());
		level.addFreshEntity(ghost);
		entry.state = State.HELD;
		entry.holder = ghost.getUUID();
		entry.holderName = MOYE_NAME;
		data.setDirty();
		noteRumour(level.getServer(), entry);
		level.playSound(null, stonePos, SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 2.0f, 0.6f);
		level.sendParticles(ParticleTypes.SOUL, stonePos.getX() + 0.5, stonePos.getY() + 0.5, stonePos.getZ() + 0.5, 40, 0.8, 1.0, 0.8, 0.03);
		tellNear(level, stonePos, Component.translatable(ModLang.LEGEND_MOYE_RISES).withStyle(ChatFormatting.DARK_AQUA));
	}

	/**
	 * {@code npc} given {@code stack} to fight with, a keeper of it for the world ({@link #HOLDER_TAG}): it never leaves its hand but at its
	 * death, and then always (whole).
	 */
	static void hold(CultivatorNpc npc, ItemStack stack) {
		npc.setItemSlot(EquipmentSlot.MAINHAND, stack);
		npc.setDropChance(EquipmentSlot.MAINHAND, 2.0f);
		npc.addTag(HOLDER_TAG);
	}

	/** {@code stone}'s sword if it is still its legend's true one; a hollow echo left in it (its legend given or reformed elsewhere) crumbles away. */
	static ItemStack trueHeld(ServerLevel level, LegendStoneBlockEntity stone) {
		ItemStack held = stone.held();
		if (held.isEmpty() || isTrue(level.getServer(), held)) return held;
		stone.take();
		return ItemStack.EMPTY;
	}

	/** {@code message} to everyone within 48 blocks of {@code at}. */
	static void tellNear(ServerLevel level, BlockPos at, Component message) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(Vec3.atCenterOf(at)) <= 48 * 48) player.sendSystemMessage(message);
		}
	}

	// --- Drawing a sword from its stone ---

	/**
	 * {@code player} lays an empty hand on {@code stone}: its sword drawn if it may be (the tomb's trial passed, the player of the sword's
	 * lowest realm at least), or they are told why not. Moye's stone never gives her sword up while she keeps it: her ghost rises for it.
	 */
	public static void draw(ServerLevel level, Player player, LegendStoneBlockEntity stone) {
		ItemStack held = trueHeld(level, stone);
		if (held.isEmpty() || !(held.getItem() instanceof LegendaryWeaponItem weapon)) {
			player.displayClientMessage(Component.translatable(ModLang.LEGEND_STONE_EMPTY), true);
			return;
		}
		Legend legend = weapon.legend();
		if (legend == Legend.GANJIANG && !stone.unlocked()) {
			player.displayClientMessage(Component.translatable(ModLang.LEGEND_TOMB_GUARDED), true);
			return;
		}
		if (legend == Legend.MOYE) {
			player.displayClientMessage(Component.translatable(ModLang.LEGEND_MOYE_GUARDED), true);
			return;
		}
		if (legend == Legend.SUN_BOW && !stone.unlocked()) {
			player.displayClientMessage(Component.translatable(ModLang.LEGEND_CROW_GUARDED), true);
			return;
		}
		// The Xuanyuan Sword stirs for the righteous alone.
		if (legend == Legend.XUANYUAN && !LegendaryWeaponItem.righteous(player)) {
			player.displayClientMessage(Component.translatable(ModLang.LEGEND_XUANYUAN_UNWORTHY, legend.getDisplayName()), true);
			return;
		}
		if (player instanceof ServerPlayer sp && !LegendaryWeaponItem.mayWield(sp, legend)) {
			player.displayClientMessage(Component.translatable(ModLang.LEGEND_TOO_HEAVY, legend.getDisplayName(), legend.lowest().getDisplayName()), true);
			return;
		}
		ItemStack taken = stone.take();
		if (!player.getInventory().add(taken)) player.drop(taken, false);
		BlockPos at = stone.getBlockPos();
		level.playSound(null, at, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.6f);
		level.playSound(null, at, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.0f);
		level.sendParticles(ParticleTypes.FLAME, at.getX() + 0.5, at.getY() + 1.2, at.getZ() + 0.5, 30, 0.2, 0.8, 0.2, 0.04);
		if (player instanceof ServerPlayer sp) seen(sp, legend);
		tellNear(level, at, Component.translatable(ModLang.LEGEND_DRAWN, player.getDisplayName(), legend.getDisplayName()).withStyle(ChatFormatting.GOLD));
		// Drawing the Yellow Emperor's sword wakes the war god he slew with it.
		if (legend == Legend.XUANYUAN && player instanceof ServerPlayer sp) chiyou(level, sp, at);
	}

	// --- Phase 3b's guards ---

	/**
	 * Chiyou's shade risen on the Xuanyuan Sword's shrine against {@code drawer}, who drew the sword from its stone at {@code stonePos}: on the
	 * terrace's far side from them, made flesh by its hatred (GhostCultivatorNpc#hurt), it hunts them until one of them falls or they get
	 * away (LegendGuards).
	 */
	static @Nullable GhostCultivatorNpc chiyou(ServerLevel level, ServerPlayer drawer, BlockPos stonePos) {
		GhostCultivatorNpc shade = ModEntities.GHOST_CULTIVATOR.create(level);
		if (shade == null) return null;
		Vec3 away = new Vec3(stonePos.getX() + 0.5 - drawer.getX(), 0, stonePos.getZ() + 0.5 - drawer.getZ());
		away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
		double x = stonePos.getX() + 0.5 + away.x * CHIYOU_RISES, z = stonePos.getZ() + 0.5 + away.z * CHIYOU_RISES;
		shade.moveTo(x, stonePos.getY() - 1, z, yawToward(x, z, drawer), 0);
		shade.becomeChiyou(CHIYOU_RANK, level.getRandom());
		level.addFreshEntity(shade);
		LegendGuards.hunt(shade, drawer, ModLang.CHIYOU_GONE);
		fx(level, ParticleTypes.SOUL_FIRE_FLAME, x, stonePos.getY() + 1, z, 80, 0.6, 1.6, 0.6, 0.06);
		fx(level, ParticleTypes.LARGE_SMOKE, x, stonePos.getY(), z, 40, 0.8, 0.4, 0.8, 0.04);
		level.playSound(null, x, stonePos.getY(), z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 0.7f);
		level.playSound(null, x, stonePos.getY(), z, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.6f);
		tellNear(level, stonePos, Component.translatable(ModLang.LEGEND_CHIYOU_RISES).withStyle(ChatFormatting.DARK_RED));
		return shade;
	}

	/**
	 * The dead sun's crater: a player come near the stone while the bow rests in it wakes the Golden Crow out of the cinder; slay it, and the
	 * bow may be drawn. Nobody near, the crow goes back into the cinder and it waits, to rise for the next who comes (drawn or not).
	 */
	static void crowCrater(ServerLevel level, Entry entry) {
		BlockPos stonePos = entry.site.above();
		if (!level.isLoaded(stonePos) || !(level.getBlockEntity(stonePos) instanceof LegendStoneBlockEntity stone) || trueHeld(level, stone).isEmpty()) return;
		if (!someoneNear(level, stonePos, TRIAL_LEAVE)) {
			if (stone.wave() > 0 || stone.unlocked()) stone.resetTrial(level);
			return;
		}
		if (stone.unlocked()) return;
		if (stone.wave() == 0) {
			if (nearest(level, stonePos, CROW_WAKE) != null) crowRises(level, stone);
			return;
		}
		if (!stone.guards(level).isEmpty()) return;
		stone.unlock();
		level.playSound(null, stonePos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.9f);
		level.sendParticles(ParticleTypes.FLAME, stonePos.getX() + 0.5, stonePos.getY() + 1.5, stonePos.getZ() + 0.5, 40, 0.3, 0.8, 0.3, 0.04);
		tellNear(level, stonePos, Component.translatable(ModLang.LEGEND_CROW_FALLS, entry.legend.getDisplayName()).withStyle(ChatFormatting.GOLD));
	}

	/** The Golden Crow risen out of the dead sun's cinder over {@code stone}, kept to the crater, the trial's one guard. */
	static @Nullable DemonBeast crowRises(ServerLevel level, LegendStoneBlockEntity stone) {
		BlockPos at = stone.getBlockPos();
		DemonBeast crow = ModEntities.beast(BeastKind.GOLDEN_CROW).create(level);
		if (crow == null) return null;
		crow.moveTo(at.getX() + 0.5, at.getY() + 4, at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		crow.setRank(BeastKind.GOLDEN_CROW.minRank());
		crow.guard(at);
		level.addFreshEntity(crow);
		stone.setWave(1);
		stone.addSpirit(crow);
		fx(level, ParticleTypes.FLAME, at.getX() + 0.5, at.getY() + 2, at.getZ() + 0.5, 120, 1.0, 2.0, 1.0, 0.12);
		fx(level, ParticleTypes.LAVA, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 30, 1.0, 0.5, 1.0, 0.2);
		level.playSound(null, at, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0f, 0.5f);
		level.playSound(null, at, SoundEvents.PARROT_IMITATE_BLAZE, SoundSource.HOSTILE, 2.0f, 0.6f);
		tellNear(level, at, Component.translatable(ModLang.LEGEND_CROW_RISES).withStyle(ChatFormatting.GOLD));
		return crow;
	}

	// --- The Sea-Fixing Needle ---

	/**
	 * {@code player} lays a hand on the Sea-Fixing Needle's pillar at {@code at}: one of its lowest realm at least pulls it free (else they are
	 * told it won't move). The pillar shrinks into their hand, the sea of clouds heaves ({@link #surge}), and Ao Guang, the East Sea's Dragon
	 * King, rises to take it back ({@link #aoGuang}).
	 */
	public static void pull(ServerLevel level, Player player, BlockPos at) {
		if (!(player instanceof ServerPlayer sp)) return;
		Legends data = get(level.getServer());
		Entry entry = data.entry(Legend.SEA_NEEDLE);
		if (level.dimension() != Legend.SEA_NEEDLE.home() || entry.state != State.RESTING || !entry.laid || entry.site == null) {
			sp.displayClientMessage(Component.translatable(ModLang.LEGEND_PILLAR_STILL), true); // a pillar the record doesn't know of
			return;
		}
		if (!LegendaryWeaponItem.mayWield(sp, Legend.SEA_NEEDLE)) {
			sp.displayClientMessage(Component.translatable(ModLang.LEGEND_PILLAR_UNMOVED, Legend.SEA_NEEDLE.lowest().getDisplayName()), true);
			return;
		}
		BlockPos foot = entry.site;
		LegendSites.unbuildPillar(level, foot);
		ItemStack needle = issue(level.getServer(), Legend.SEA_NEEDLE);
		if (!sp.getInventory().add(needle)) sp.drop(needle, false);
		seen(sp, Legend.SEA_NEEDLE);
		surge(level, at);
		aoGuang(level, sp, foot);
		tellNear(level, at, Component.translatable(ModLang.LEGEND_NEEDLE_PULLED, sp.getDisplayName(), Legend.SEA_NEEDLE.getDisplayName())
				.withStyle(ChatFormatting.GOLD));
	}

	/** The sea of clouds heaving as the Needle comes out of it at {@code at}: everyone within {@link #SURGE_REACH} thrown back, the clouds churning. */
	static void surge(ServerLevel level, BlockPos at) {
		Vec3 middle = Vec3.atCenterOf(at);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at).inflate(SURGE_REACH), e -> e.isAlive() && !e.isSpectator())) {
			Vec3 away = e.position().subtract(middle).multiply(1, 0, 1);
			away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
			e.push(away.x * 2.0, 0.6, away.z * 2.0);
			e.hurtMarked = true;
		}
		double cloud = ModDimensions.IMMORTAL_CLOUD_Y;
		fx(level, ParticleTypes.CLOUD, middle.x, cloud + 1, middle.z, 400, 12, 1.5, 12, 0.35);
		fx(level, ParticleTypes.POOF, middle.x, cloud + 2, middle.z, 160, 8, 3, 8, 0.25);
		fx(level, ParticleTypes.SPLASH, middle.x, cloud + 1, middle.z, 300, 10, 1, 10, 0.5);
		level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0f, 0.5f);
		level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.5f, 0.6f);
		level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.HOSTILE, 3.0f, 0.5f);
	}

	/**
	 * Ao Guang, Dragon King of the East Sea, risen out of the sea of clouds by where the Needle's pillar stood ({@code foot}) on the far side
	 * from {@code puller}, who took it: a True Immortal of the dragon race, aloft, who hunts them until one of them falls or they get away
	 * (LegendGuards).
	 */
	static @Nullable CultivatorNpc aoGuang(ServerLevel level, ServerPlayer puller, BlockPos foot) {
		CultivatorNpc king = ModEntities.CULTIVATOR.create(level);
		if (king == null) return null;
		RandomSource random = level.getRandom();
		Vec3 away = new Vec3(foot.getX() + 0.5 - puller.getX(), 0, foot.getZ() + 0.5 - puller.getZ());
		away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
		double x = foot.getX() + 0.5 + away.x * AO_GUANG_RISES, z = foot.getZ() + 0.5 + away.z * AO_GUANG_RISES, y = ModDimensions.IMMORTAL_CLOUD_Y + 2;
		king.moveTo(x, y, z, yawToward(x, z, puller), 0);
		king.becomeRogue(AO_GUANG_RANK, AO_GUANG_ALIGNMENT, random);
		king.setRank(AO_GUANG_RANK, true); // the sea's own king, whatever room the realm has for True Immortals (HighRealms), as Lei Zhen
		king.setRace(Bloodline.DRAGON, 0); // the azure dragon of the East Sea
		king.equip(random);
		king.setTitle(NpcTitle.DRAGON_KING);
		king.setCustomName(Component.literal(AO_GUANG_NAME));
		level.addFreshEntity(king);
		king.setQiFlying(true);
		LegendGuards.hunt(king, puller, ModLang.AO_GUANG_GONE);
		fx(level, ParticleTypes.SPLASH, x, y, z, 200, 1.5, 3, 1.5, 0.4);
		fx(level, ParticleTypes.CLOUD, x, y, z, 120, 2, 2, 2, 0.2);
		level.playSound(null, x, y, z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0f, 0.8f);
		tellNear(level, BlockPos.containing(x, y, z), Component.translatable(ModLang.LEGEND_AO_GUANG_RISES).withStyle(ChatFormatting.AQUA));
		return king;
	}

	/**
	 * {@code player}, dying, keeps the Sea-Fixing Needle behind their ear (Player#dropEquipment, PlayerMixin; a ghost born again, GhostSystem):
	 * out of their inventory into the record, never dropped, to be theirs again as they live again ({@link #giveBack}).
	 */
	public static void keepBehindTheEar(Player player) {
		if (!(player instanceof ServerPlayer sp)) return;
		Inventory inventory = sp.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!(stack.getItem() instanceof LegendaryWeaponItem weapon) || weapon.legend() != Legend.SEA_NEEDLE || !isTrue(sp.server, stack)) continue;
			inventory.setItem(i, ItemStack.EMPTY);
			Legends data = get(sp.server);
			Entry entry = data.entry(Legend.SEA_NEEDLE);
			entry.kept = stack.copy();
			entry.state = State.ABROAD;
			entry.holder = sp.getUUID();
			entry.holderName = sp.getGameProfile().getName();
			data.setDirty();
		}
	}

	/** {@code player}'s Needle, kept behind their ear while they were dead, theirs again if they live (Legends#tick, a respawn). */
	public static void giveBack(ServerPlayer player) {
		Legends data = get(player.server);
		Entry entry = data.entry(Legend.SEA_NEEDLE);
		if (entry.kept.isEmpty() || !player.getUUID().equals(entry.holder) || !player.isAlive() || player.isRemoved()) return;
		ItemStack stack = entry.kept;
		entry.kept = ItemStack.EMPTY;
		data.setDirty();
		if (!isTrue(player.server, stack)) return; // made anew meanwhile (an op's word): this one an echo, gone
		if (!player.getInventory().add(stack)) player.drop(stack, false);
		player.sendSystemMessage(Component.translatable(ModLang.LEGEND_NEEDLE_KEPT_MSG, Legend.SEA_NEEDLE.getDisplayName()).withStyle(ChatFormatting.GOLD));
	}

	// --- Phase 3c: Pangu's Axe, the Venerables' weapons ---

	/**
	 * The Primordial Chaos slain (PrimordialBeast#dropCustomDeathLoot): Pangu's Axe, at home in its heart, falls out of it (a new true stack,
	 * loose where it fell, nobody's yet); else nothing.
	 */
	public static ItemStack fromTheChaos(MinecraftServer server) {
		Legends data = get(server);
		Entry entry = data.entry(Legend.PANGU_AXE);
		if (entry.state != State.RESTING) return ItemStack.EMPTY;
		ItemStack axe = issue(server, Legend.PANGU_AXE);
		entry.state = State.ABROAD;
		entry.holder = null;
		entry.holderName = "";
		data.setDirty();
		noteRumour(server, entry);
		return axe;
	}

	/**
	 * Every two seconds, the Celestial Court's five Venerables: each at its post given its element's weapon while that is at home ({@link #arm});
	 * and one fighting with its weapon in hand casts its art now and then (LegendaryWeaponItem#symbolArt).
	 */
	static void venerables(MinecraftServer server, Legends data) {
		ServerLevel level = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (level == null || level.players().isEmpty()) return;
		boolean home = false;
		for (Legend legend : Legend.values()) home |= legend.venerable() != null && data.entry(legend).state == State.RESTING;
		boolean near = false;
		for (ServerPlayer player : level.players()) {
			double dx = player.getX() - ModNoiseSettings.COURT_X, dz = player.getZ() - ModNoiseSettings.COURT_Z;
			near |= dx * dx + dz * dz <= (double) CelestialCourt.NEAR * CelestialCourt.NEAR;
		}
		if (home && near && level.isLoaded(new BlockPos(ModNoiseSettings.COURT_X, 0, ModNoiseSettings.COURT_Z))) {
			for (CultivatorNpc npc : CelestialCourt.people(level, CelestialCourt.middle(level))) {
				Legend legend = npc.getTitle() == NpcTitle.HEAVENLY_VENERABLE ? Legend.ofVenerable(npc.courtElement()) : null;
				if (legend == null) continue;
				Entry entry = data.entry(legend);
				if (entry.state != State.RESTING) continue;
				arm(npc, issue(server, legend)); // made anew in its hand: any older stack of it (an op's gift, say) an echo
				entry.state = State.HELD;
				entry.holder = npc.getUUID();
				entry.holderName = npc.getPersonalName();
				data.setDirty();
				noteRumour(server, entry);
			}
		}
		for (Legend legend : Legend.values()) {
			if (legend.venerable() == null || level.getRandom().nextInt(4) != 0) continue;
			CultivatorNpc keeper = keeper(level, legend);
			// Not in a bout (a Dao Duel): its art could carry its partner off past the bout's ground (SparSystem#WALK_AWAY), a forfeit not theirs.
			if (keeper == null || keeper.getTarget() == null || !keeper.getTarget().isAlive() || SparSystem.isSparring(keeper)) continue;
			if (!(keeper.getMainHandItem().getItem() instanceof LegendaryWeaponItem weapon) || weapon.legend() != legend) continue;
			if (HeavenSpells.canCast(keeper, legend.symbolSpell())) LegendaryWeaponItem.symbolArt(keeper, legend);
		}
	}

	/**
	 * {@code npc}, a Venerable of the Court, given its element's weapon to fight with, dropped whole at its death: no keeper made for it (no
	 * {@link #HOLDER_TAG}), so it never strays (#stray) when its weapon is won from it or made anew elsewhere.
	 */
	static void arm(CultivatorNpc npc, ItemStack stack) {
		npc.setItemSlot(EquipmentSlot.MAINHAND, stack);
		npc.setDropChance(EquipmentSlot.MAINHAND, 2.0f);
	}

	/**
	 * The weapon {@code npc} would stake on a Dao Duel with {@code player} (SparSystem.Terms#DUEL): its own, if it is a Venerable at its post with
	 * its element's true weapon in hand and the player a General of the Celestial Court; else null.
	 */
	public static @Nullable Legend duelFor(ServerPlayer player, CultivatorNpc npc) {
		if (npc.getTitle() != NpcTitle.HEAVENLY_VENERABLE || !CelestialCourt.isCourtier(npc)) return null;
		Legend legend = Legend.ofVenerable(npc.courtElement());
		ItemStack held = npc.getMainHandItem();
		if (legend == null || !(held.getItem() instanceof LegendaryWeaponItem weapon) || weapon.legend() != legend || !isTrue(player.server, held)) return null;
		return CelestialCourt.rankOf(player.server, player.getUUID()) == CelestialCourt.Rank.GENERAL ? legend : null;
	}

	/**
	 * {@code npc}, a Venerable beaten in a Dao Duel (SparSystem), hands {@code player} the weapon it staked: theirs now, to bond with; the
	 * Venerable keeps its post, its hand empty.
	 */
	public static void handOver(CultivatorNpc npc, ServerPlayer player, Legend legend) {
		ItemStack held = npc.getMainHandItem();
		if (!(held.getItem() instanceof LegendaryWeaponItem weapon) || weapon.legend() != legend || !isTrue(player.server, held)) return;
		npc.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		npc.setDropChance(EquipmentSlot.MAINHAND, 0.085f);
		if (!player.getInventory().add(held)) player.drop(held, false);
		seen(player, legend);
		Milestones.award(player, Milestones.DUEL_WON);
		player.serverLevel().playSound(null, npc.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.0f);
		player.sendSystemMessage(Component.translatable(ModLang.LEGEND_DUEL_HANDED, Component.literal(npc.getPersonalName()), legend.getDisplayName())
				.withStyle(ChatFormatting.GOLD));
	}

	// --- The Xuanyuan Sword won't stay with the devilish ---

	/**
	 * The Xuanyuan Sword won't stay with the devilish (LegendaryWeaponItem#inventoryTick): out of {@code player}'s hands it flies home to its
	 * stone, a streak of gold into the sky, made anew there (its old stack gone).
	 */
	static void leaves(ServerPlayer player, ItemStack stack) {
		stack.setCount(0);
		ServerLevel level = player.serverLevel();
		Vec3 from = player.getEyePosition();
		SpellFx.send(level, SpellFx.Kind.STREAK, from, from.add(0, 48, 0), 0, 0xFFD860);
		level.sendParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 30, 0.3, 0.6, 0.3, 0.1);
		level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0f, 1.4f);
		player.sendSystemMessage(Component.translatable(ModLang.LEGEND_XUANYUAN_LEAVES, Legend.XUANYUAN.getDisplayName()).withStyle(ChatFormatting.GOLD));
		reform(player.server, Legend.XUANYUAN);
	}

	/** Yaw to face {@code target} from (x, z). */
	private static float yawToward(double x, double z, Entity target) {
		return (float) (Math.atan2(target.getZ() - z, target.getX() - x) * 180 / Math.PI) - 90;
	}

	/** Particles as sendParticles makes them, seen by whoever is within 128 of them however far off (vanilla drops them past 32). */
	static void fx(ServerLevel level, ParticleOptions options, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(x, y, z) <= 128 * 128) level.sendParticles(player, options, true, x, y, z, count, dx, dy, dz, speed);
		}
	}

	// --- Bonding, and the spear's taming ---

	/** {@code player} has meditated another tick with {@code legend} in hand: how long all told. */
	static int bonding(ServerPlayer player, Legend legend) {
		Map<Legend, Integer> theirs = BONDING.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(Legend.class));
		return theirs.merge(legend, 1, Integer::sum);
	}

	static void bonded(ServerPlayer player, Legend legend) {
		Map<Legend, Integer> theirs = BONDING.get(player.getUUID());
		if (theirs != null) theirs.remove(legend);
	}

	/** Whether {@code player} is being tamed by the Tribulation Spear now. */
	public static boolean taming(ServerPlayer player) {
		return TAMING.containsKey(player.getUUID());
	}

	/** The Tribulation Spear turns its lightning on {@code player}, bonding with it: three bolts, two seconds apart, to be survived. */
	static void tame(ServerPlayer player) {
		TAMING.put(player.getUUID(), 3);
		player.sendSystemMessage(Component.translatable(ModLang.LEGEND_TAMING).withStyle(ChatFormatting.LIGHT_PURPLE));
		player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.6f);
	}

	/** The taming's bolts: one each two seconds at each one being tamed; the last survived, the spear theirs. Dead or gone, it fails. */
	static void taming(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Integer>> it = TAMING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Integer> taming = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(taming.getKey());
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			ItemStack spear = LegendaryWeaponItem.carried(player, Legend.TRIBULATION_SPEAR);
			if (spear.isEmpty()) {
				it.remove();
				continue;
			}
			ServerLevel level = player.serverLevel();
			LegendaryWeaponItem.bolt(level, player.position(), player.getY() + 20, 2.0f, false);
			player.invulnerableTime = 0;
			player.hurt(level.damageSources().magic(), player.getMaxHealth() * LegendaryWeaponItem.TAMING_BOLT);
			int left = taming.getValue() - 1;
			if (left > 0) {
				taming.setValue(left);
				continue;
			}
			it.remove();
			if (player.isAlive()) LegendaryWeaponItem.bond(spear, player);
		}
	}

	/** A server stopping: nobody is bonding or being tamed any more, no bow gathering its sun, and Lei Zhen's storm is gone with the world. */
	public static void clear() {
		BONDING.clear();
		TAMING.clear();
		LegendaryWeaponItem.clear();
		LeiZhen.clear();
		LegendGuards.clear();
	}

	// --- Rumours ---

	/** What is told of {@code entry} now, as it stands (Rumours.Kind#LEGEND). */
	static void noteRumour(MinecraftServer server, Entry entry) {
		if (entry.site == null) return;
		Rumours.noteLegend(entry.legend, entry.legend.home(), entry.site, entry.state, entry.state == State.ABROAD ? entry.holderName : "",
				server.overworld().getGameTime());
	}

	/** Every legend's entry as it is now (the op's /legend list, the tests). */
	public static List<Entry> all(MinecraftServer server) {
		return new ArrayList<>(get(server).entries.values());
	}

	/** The one of {@code legend}'s keepers loaded now: Moye's ghost or Lei Zhen, or null. */
	public static @Nullable CultivatorNpc keeper(ServerLevel level, Legend legend) {
		Entry entry = get(level.getServer()).entry(legend);
		if (entry.state != State.HELD || entry.holder == null) return null;
		Entity found = level.getEntity(entry.holder);
		return found instanceof CultivatorNpc npc && npc.isAlive() ? npc : null;
	}

	/** Everything about {@code at} within {@code reach} that the tomb's spirits are (for the tests). */
	static List<SwordSpirit> spiritsNear(ServerLevel level, BlockPos at, double reach) {
		return level.getEntitiesOfClass(SwordSpirit.class, new AABB(at).inflate(reach), Entity::isAlive);
	}
}
