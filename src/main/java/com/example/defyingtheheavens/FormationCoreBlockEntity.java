package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The heart of a protective formation. It holds a small qi battery ({@link #BATTERY}), filled by the Qi Veins its seals reach
 * ({@link #scanNetwork}: lines of {@link SealBlock}s from the core, at most {@link #LINK_RADIUS} blocks out and only through
 * loaded chunks, each vein touching the line or the core feeding {@link QiVeinBlock#QI_PER_SECOND}), and spends it on the
 * barrier's upkeep ({@link #upkeep}, growing with the radius).
 * <p>
 * Its owner switches the barrier on and sets its radius from the core's screen. A raised barrier stays up while the veins and
 * battery can carry it; when the upkeep outruns the veins and the battery runs dry, it comes down. Every barrier block broken
 * costs {@link #BREACH_COST}, every one mended {@link #REPAIR_COST}: break enough of it fast enough and the battery empties
 * and the whole barrier shatters, and can't rise again for {@link #SHATTER_COOLDOWN} ticks.
 * <p>
 * Cores a sect's founders set at the heart of their grounds start with a {@link #blueprint} of the sect (see SectStructure);
 * on their first tick they found the sect and raise its barrier, and from then on answer only to that sect.
 * <p>
 * Since 2026-10-28 (the user's ask: "the formation core now acts as a core for all types of formations except the storage one") the
 * barrier is one of its functions; the other is concealment (Concealments), with its own radius and switch, its upkeep ({@link #veilUpkeep})
 * from the same veins and battery. Short of qi, the concealment goes first. Its owner's mastery of formations (CraftMastery) takes 5% off
 * both upkeeps a rank past Apprentice, and keeping them up teaches it.
 */
public class FormationCoreBlockEntity extends BlockEntity {
	public static final double BATTERY = 2000;
	public static final double BREACH_COST = 40;
	public static final double REPAIR_COST = 5;
	public static final int LINK_RADIUS = 64;
	private static final int MAX_NODES = 4096;
	public static final int SHATTER_COOLDOWN = 2400;
	public static final int DEFAULT_RADIUS = 12;
	/** A lowered barrier rises again once the battery is back to this share, if the veins alone can't carry it. */
	private static final double RAISE_FRACTION = 0.5;

	private UUID formationId;
	private UUID owner;
	private String ownerName = "";
	private UUID sect;
	private String sectName = "";
	private int radius = DEFAULT_RADIUS;
	private boolean wanted;
	private double battery;
	private int veins;
	private long shatteredUntil;
	private int rank = -1;
	/** Set at worldgen: the sect to found on the first tick (see SectManager#found). */
	private CompoundTag blueprint;
	/** Players the owner trusts to pass the barrier as they do, with the names they had when added. */
	private final Map<UUID, String> trusted = new LinkedHashMap<>();
	/** The sect this core served before it fell (its last member dead), for the first to claim it. Empty otherwise. */
	private String formerSect = "";
	/** Its concealment (Concealments: its veil's id the formation's): the radius to keep it at, and whether it should be up. */
	private int veilRadius = DEFAULT_RADIUS;
	private boolean veilWanted;
	/** Its owner's rank in formations (CraftMastery#Rank ordinal) when they last set it: each past Apprentice takes 5% off the upkeeps. */
	private int masteryRank;
	/** Ticks its functions have stood since its owner last learned from them. */
	private int practiceTicks;
	/** A function standing this long teaches its owner a point of practice in formations (ten minutes). */
	private static final int PRACTICE_TICKS = 12_000;

	public FormationCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FORMATION_CORE, pos, state);
	}

	/** Qi per second a barrier of this radius costs to keep up: 1.3 at radius 8, 13 at 32, 52 at 64. */
	public static double upkeep(int radius) {
		return 0.5 + radius * radius / 80.0;
	}

	/** Qi per second a concealment of this radius costs: three quarters of a barrier's (1 at radius 8, 10 at 32, 39 at 64). */
	public static double veilUpkeep(int radius) {
		return upkeep(radius) * 0.75;
	}

	/** What its owner's mastery of formations leaves of each upkeep: 5% off for every rank past Apprentice (a Saint's, 80%). */
	public double upkeepShare() {
		return 1 - 0.05 * masteryRank;
	}

	public int getVeilRadius() { return veilRadius; }
	public boolean isVeilWanted() { return veilWanted; }

	public boolean isVeilRaised() {
		if (!(level instanceof ServerLevel server) || formationId == null) return false;
		Concealments.Veil veil = Concealments.get(server, formationId);
		return veil != null && veil.isRaised();
	}

	/**
	 * The qi a second flowing in: the veins', and for a sect's core its own people's making up what its barrier's upkeep asks beyond what its
	 * plaza's veins ({@link #SECT_VEINS}) give, with {@link #SECT_SURPLUS} to spare for mending it. A sect's barrier is no wider than those
	 * carry, but a Holy Land's, a Demon Palace's or an immortal sect's (a radius up to 136) asks some 232 a second of their 64: it drained
	 * its battery in seconds, fell all at once (some 230,000 blocks taken down in a tick) and rose again over and over while anyone was near
	 * (the user, 2026-10-27, "massive lag spikes"). The veins mined, it is short as before.
	 */
	public double supply() {
		return supply(veins, sect != null, radius);
	}

	/** {@link #supply()} of a core with {@code veins} linked, a sect's or not, keeping a barrier of {@code radius}. */
	public static double supply(int veins, boolean sect, int radius) {
		double veinsGive = veins * QiVeinBlock.QI_PER_SECOND;
		if (!sect) return veinsGive;
		return veinsGive + Math.max(0, upkeep(radius) + SECT_SURPLUS - SECT_VEINS * QiVeinBlock.QI_PER_SECOND);
	}

	/** The veins a sect's plaza lays round its core (SectPiece's array: one at each of its eight spokes' ends). */
	public static final int SECT_VEINS = 8;
	/** What a sect's people leave over a second, beyond its barrier's upkeep, to mend it (about what a lesser sect's veins leave). */
	public static final double SECT_SURPLUS = 32;
	public int getRadius() { return radius; }
	public boolean isWanted() { return wanted; }
	public double getBattery() { return battery; }
	public int getVeins() { return veins; }
	public int getRank() { return rank; }
	public UUID getSect() { return sect; }
	public UUID getFormationId() { return formationId; }
	public String getOwnerName() { return ownerName; }
	public String getSectName() { return sectName; }
	public Map<UUID, String> getTrusted() { return Collections.unmodifiableMap(trusted); }

	public boolean isRaised() {
		if (!(level instanceof ServerLevel server) || formationId == null) return false;
		Formation formation = Formations.get(server, formationId);
		return formation != null && formation.isRaised();
	}

	/** Seconds until a shattered barrier can rise again (0 if it can). */
	public int cooldownSeconds() {
		return level == null ? 0 : (int) Math.max(0, (shatteredUntil - level.getGameTime() + 19) / 20);
	}

	public void setOwner(ServerPlayer player) {
		owner = player.getUUID();
		ownerName = player.getGameProfile().getName();
		rank = Formations.rankOf(player);
		setChanged();
	}

	public void setBlueprint(CompoundTag blueprint) {
		this.blueprint = blueprint;
	}

	/** SectManager: this core now belongs to {@code sect}, keeps its barrier up at {@code radius}, and is as strong as its master. */
	public void bindToSect(UUID sect, String name, int radius, int masterRank) {
		this.sect = sect;
		this.sectName = name;
		this.radius = Math.max(Formations.MIN_RADIUS, Math.min(Formations.maxRadius(level), radius));
		this.wanted = true;
		this.owner = null;
		this.ownerName = "";
		this.rank = masterRank;
		this.battery = BATTERY;
		trusted.clear();
		this.veilWanted = false; // a sect's core keeps no concealment
		setChanged();
		if (level instanceof ServerLevel server) {
			Formation formation = formation(server);
			Formations.resize(server, formation, this.radius);
			Formations.update(server, formation, rank, null, sect);
			Concealments.Veil veil = Concealments.get(server, formationId);
			if (veil != null && veil.isRaised()) Concealments.set(server, veil, false, veil.radius());
		}
	}

	/**
	 * The sect it served is gone (its last member dead): the core answers to no one, and the first to lay hands on it claims
	 * it (see {@link #canControl}). The barrier stands meanwhile, as strong as the last Sect Master.
	 */
	private void releaseFromSect(ServerLevel level) {
		formerSect = sectName;
		sect = null;
		sectName = "";
		owner = null;
		ownerName = "";
		trusted.clear();
		setChanged();
		Formations.update(level, formation(level), rank, null, null);
	}

	/** The sect's master changed: the barrier is as strong as the new one. */
	public void setSectRank(int masterRank) {
		if (rank == masterRank) return;
		rank = masterRank;
		setChanged();
		if (level instanceof ServerLevel server) Formations.update(server, formation(server), rank, owner, sect);
	}

	private Formation formation(ServerLevel level) {
		if (formationId == null) {
			formationId = UUID.randomUUID();
			setChanged();
		}
		Formation formation = Formations.create(level, formationId, Formation.Kind.CORE, worldPosition, radius);
		Formations.update(level, formation, rank, owner, sect);
		Formations.setTrusted(level, formation, trusted.keySet());
		return formation;
	}

	/** Its concealment's veil (made, lowered, the first time it is wanted), answering to its owner and those they trust. */
	private Concealments.Veil veil(ServerLevel level) {
		formation(level);
		Concealments.Veil veil = Concealments.veil(level, formationId, worldPosition, veilRadius);
		Concealments.update(level, veil, rank, owner, trusted.keySet());
		return veil;
	}

	// --- Ticking ---

	public static void serverTick(Level level, BlockPos pos, BlockState state, FormationCoreBlockEntity core) {
		if (!(level instanceof ServerLevel server)) return;
		if (core.blueprint != null) {
			CompoundTag blueprint = core.blueprint;
			core.blueprint = null;
			core.setChanged();
			SectManager.found(server, core, blueprint);
		}
		long time = level.getGameTime() + pos.asLong();
		if (time % 100 == 0) core.scanNetwork(server);
		if (time % 20 == 0) core.economy(server);
	}

	/**
	 * Once a second: the veins fill the battery, the barrier and the concealment draw their upkeep, and each rises or falls accordingly;
	 * short of qi, the concealment goes first, the barrier only if it still can't be carried.
	 */
	private void economy(ServerLevel level) {
		if (sect != null) {
			Sect served = SectManager.get(level.getServer(), sect);
			if (served == null || served.extinct) releaseFromSect(level);
		}
		Formation formation = formation(level);
		Concealments.Veil veil = veilWanted && sect == null ? veil(level) : formationId == null ? null : Concealments.get(level, formationId);
		double share = upkeepShare();
		double upkeep = formation.isRaised() ? upkeep(formation.getRadius()) * share : 0;
		double veilCost = veil != null && veil.isRaised() ? veilUpkeep(veil.radius()) * share : 0;
		double before = battery;
		battery = Math.max(0, Math.min(BATTERY, battery + supply() - upkeep - veilCost));
		if (battery != before) setChanged();
		if (veil != null) {
			if (veil.isRaised() && (!veilWanted || sect != null)) {
				Concealments.set(level, veil, false, veil.radius());
				veilCost = 0;
				level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 0.6f);
			} else if (veil.isRaised() && battery <= 0 && supply() < upkeep + veilCost) {
				Concealments.set(level, veil, false, veil.radius());
				veilCost = 0;
				level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 0.6f);
			} else if (!veil.isRaised() && veilWanted && sect == null
					&& (supply() >= upkeep + veilUpkeep(veilRadius) * share || battery >= BATTERY * RAISE_FRACTION)) {
				Concealments.set(level, veil, true, veilRadius);
				level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 1.4f);
			} else if (veil.isRaised() && veil.radius() != veilRadius) {
				Concealments.set(level, veil, true, veilRadius);
			}
		}
		if (formation.isRaised() && !wanted) {
			Formations.lower(level, formation);
			level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
			SectManager.onBarrierChanged(level, sect, false, false);
		} else if (formation.isRaised() && battery <= 0 && supply() < upkeep + veilCost) {
			collapse(level, false);
		} else if (!formation.isRaised() && wanted && level.getGameTime() >= shatteredUntil
				&& (supply() >= upkeep(radius) * share + veilCost || battery >= BATTERY * RAISE_FRACTION)) {
			if (formation.getRadius() != radius) Formations.resize(level, formation, radius);
			Formations.raise(level, formation);
			level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.2f);
			SectManager.onBarrierChanged(level, sect, true, false);
		}
		practise(level, formation.isRaised(), veil != null && veil.isRaised());
	}

	/** Every ten minutes a function of a player's core stands, its owner (if about) learns a point of formations for each (CraftMastery). */
	private void practise(ServerLevel level, boolean barrier, boolean veiled) {
		if (owner == null || sect != null || !barrier && !veiled) return;
		practiceTicks += 20;
		if (practiceTicks < PRACTICE_TICKS) return;
		practiceTicks = 0;
		setChanged();
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
		if (player != null) CraftMastery.practise(player, CraftMastery.Craft.FORMATIONS, (barrier ? 1 : 0) + (veiled ? 1 : 0));
	}

	/** The barrier comes down: out of qi, or {@code shattered} by a breach the battery couldn't mend. */
	private void collapse(ServerLevel level, boolean shattered) {
		Formation formation = formation(level);
		if (!formation.isRaised()) return;
		Formations.lower(level, formation);
		shatteredUntil = level.getGameTime() + (shattered ? SHATTER_COOLDOWN : 0);
		setChanged();
		double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.0, z = worldPosition.getZ() + 0.5;
		if (shattered) {
			level.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.0f, 0.5f);
			level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2.0f, 0.6f);
			level.sendParticles(ParticleTypes.END_ROD, x, y, z, 60, 1.5, 1.5, 1.5, 0.3);
		} else {
			level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 0.8f);
		}
		SectManager.onBarrierChanged(level, sect, false, true);
	}

	/** A barrier block was broken: mending the breach strains the battery, and an empty battery shatters the barrier. */
	public void onBreach() {
		batter(BREACH_COST);
	}

	/**
	 * The barrier struck by one strong enough to break it (Formations#batter): its stored qi, its health, takes {@code cost}, and empty, the
	 * whole barrier shatters ({@link #SHATTER_COOLDOWN} before it can rise again).
	 */
	public void batter(double cost) {
		if (!(level instanceof ServerLevel server)) return;
		battery = Math.max(0, battery - cost);
		setChanged();
		if (battery <= 0) collapse(server, true);
	}

	/** Pays for mending one barrier block. @return false if the battery can't */
	public boolean payRepair() {
		if (battery < REPAIR_COST) return false;
		battery -= REPAIR_COST;
		setChanged();
		return true;
	}

	/**
	 * Follows the seals out from the core and counts the Qi Veins they reach: seals join seals within a block (diagonals too),
	 * veins count when they touch a seal of the line or the core itself. Never further than {@link #LINK_RADIUS} from the
	 * core and never into an unloaded chunk, so a core can't load or lag the world chasing a long line.
	 */
	public void scanNetwork(ServerLevel level) {
		Set<BlockPos> seals = new HashSet<>();
		Set<BlockPos> veinBlocks = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		visit(level, worldPosition, seals, veinBlocks, queue);
		while (!queue.isEmpty() && seals.size() < MAX_NODES) visit(level, queue.poll(), seals, veinBlocks, queue);
		if (veins != veinBlocks.size()) {
			veins = veinBlocks.size();
			setChanged();
		}
	}

	private void visit(ServerLevel level, BlockPos from, Set<BlockPos> seals, Set<BlockPos> veinBlocks, ArrayDeque<BlockPos> queue) {
		long reach = (long) LINK_RADIUS * LINK_RADIUS;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) continue;
					BlockPos next = from.offset(dx, dy, dz);
					if (next.distSqr(worldPosition) > reach || !level.isLoaded(next)) continue;
					BlockState state = level.getBlockState(next);
					if (state.getBlock() instanceof SealBlock) {
						if (seals.add(next)) queue.add(next);
					} else if (state.is(ModTags.QI_VEIN_BLOCKS)) {
						veinBlocks.add(next);
					}
				}
			}
		}
	}

	// --- The owner's screen ---

	/** Who may open the screen: the owner (a core nobody owns yet is claimed by the first to open it), or an operator in creative. */
	public boolean canControl(ServerPlayer player) {
		if (player.isCreative() && player.hasPermissions(2)) return true;
		if (sect != null && level instanceof ServerLevel server) {
			Sect served = SectManager.get(server.getServer(), sect);
			if (served != null && !served.extinct) return false;
			releaseFromSect(server); // its sect fell since the last second's check
		}
		if (owner == null) {
			setOwner(player);
			if (!formerSect.isEmpty()) {
				player.displayClientMessage(Component.translatable(ModLang.MSG_CORE_CLAIMED, formerSect), false);
				formerSect = "";
			}
			if (level instanceof ServerLevel server) Formations.update(server, formation(server), rank, owner, null);
			return true;
		}
		return owner.equals(player.getUUID());
	}

	// --- Trusted cultivators ---

	/** Most players a core can be told to trust. */
	public static final int MAX_TRUSTED = 16;

	/** From the screen: trust the player called {@code name} (online now, or seen on this server before). */
	public void trustByName(ServerPlayer by, String name) {
		if (!(level instanceof ServerLevel server) || !canControl(by) || sect != null || name == null || name.isBlank()) return;
		MinecraftServer mc = server.getServer();
		ServerPlayer online = mc.getPlayerList().getPlayerByName(name.trim());
		GameProfile profile = online != null ? online.getGameProfile() : mc.getProfileCache() == null ? null
				: mc.getProfileCache().get(name.trim()).orElse(null);
		if (profile == null || profile.getId() == null) {
			by.displayClientMessage(Component.translatable(ModLang.MSG_TRUST_UNKNOWN, name.trim()), true);
			return;
		}
		if (profile.getId().equals(owner)) {
			by.displayClientMessage(Component.translatable(ModLang.MSG_TRUST_SELF), true);
			return;
		}
		if (!trusted.containsKey(profile.getId()) && trusted.size() >= MAX_TRUSTED) {
			by.displayClientMessage(Component.translatable(ModLang.MSG_TRUST_FULL, MAX_TRUSTED), true);
			return;
		}
		trust(profile.getId(), profile.getName());
		by.displayClientMessage(Component.translatable(ModLang.MSG_TRUST_ADDED, profile.getName()), true);
	}

	/** Trusts {@code player} (the tests, and {@link #trustByName}): they pass its barrier and see through its concealment. */
	public void trust(UUID player, String name) {
		trusted.put(player, name);
		setChanged();
		if (level instanceof ServerLevel server) {
			Formations.setTrusted(server, formation(server), trusted.keySet());
			answering(server);
		}
	}

	/** Its concealment's veil, if it has one, told again whom it answers to (its owner's rank, its owner, those they trust). */
	private void answering(ServerLevel level) {
		Concealments.Veil veil = formationId == null ? null : Concealments.get(level, formationId);
		if (veil != null) Concealments.update(level, veil, rank, owner, trusted.keySet());
	}

	/** From the screen: no longer trusts {@code player}. */
	public void distrust(ServerPlayer by, UUID player) {
		if (!(level instanceof ServerLevel server) || !canControl(by) || player == null) return;
		String name = trusted.remove(player);
		if (name == null) return;
		setChanged();
		Formations.setTrusted(server, formation(server), trusted.keySet());
		answering(server);
		by.displayClientMessage(Component.translatable(ModLang.MSG_TRUST_REMOVED, name), true);
	}

	/** From the screen: the radius to keep the barrier at, and whether it should be up (its concealment left as it is). */
	public void configure(ServerPlayer player, int newRadius, boolean newWanted) {
		configure(player, newRadius, newWanted, veilRadius, veilWanted);
	}

	/** From the screen: each function's radius, and whether it should be up. */
	public void configure(ServerPlayer player, int newRadius, boolean newWanted, int newVeilRadius, boolean newVeilWanted) {
		if (!(level instanceof ServerLevel server) || !canControl(player)) return;
		radius = Math.max(Formations.MIN_RADIUS, Math.min(Formations.maxRadius(level), newRadius));
		wanted = newWanted;
		veilRadius = Math.max(Formations.MIN_RADIUS, Math.min(Formations.maxRadius(level), newVeilRadius));
		veilWanted = newVeilWanted && sect == null;
		if (sect == null && owner != null && owner.equals(player.getUUID())) {
			rank = Formations.rankOf(player); // as strong as its maker now
			masteryRank = CraftMastery.rank(CultivationManager.own(player), CraftMastery.Craft.FORMATIONS).ordinal(); // and as skilled
		}
		setChanged();
		Formation formation = formation(server);
		if (formation.isRaised() && formation.getRadius() != radius) Formations.resize(server, formation, radius);
		answering(server);
		economy(server); // switch on or off at once rather than within the second
	}

	/** The core's block was broken or replaced: its barrier and its concealment come down for good. */
	public void onRemovedFromWorld(ServerLevel level) {
		if (formationId != null) {
			Formations.dissolve(level, formationId);
			Concealments.dissolve(level, formationId);
		}
		SectManager.onCoreDestroyed(level, sect);
	}

	// --- Saving ---

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (formationId != null) tag.putUUID("Formation", formationId);
		if (owner != null) tag.putUUID("Owner", owner);
		tag.putString("OwnerName", ownerName);
		if (sect != null) tag.putUUID("Sect", sect);
		tag.putString("SectName", sectName);
		tag.putInt("Radius", radius);
		tag.putBoolean("Wanted", wanted);
		tag.putDouble("Battery", battery);
		tag.putInt("Veins", veins);
		tag.putLong("ShatteredUntil", shatteredUntil);
		tag.putInt("Rank", rank);
		if (blueprint != null) tag.put("Blueprint", blueprint);
		ListTag list = new ListTag();
		for (Map.Entry<UUID, String> player : trusted.entrySet()) {
			CompoundTag entry = new CompoundTag();
			entry.putUUID("Id", player.getKey());
			entry.putString("Name", player.getValue());
			list.add(entry);
		}
		tag.put("Trusted", list);
		tag.putString("FormerSect", formerSect);
		tag.putInt("VeilRadius", veilRadius);
		tag.putBoolean("VeilWanted", veilWanted);
		tag.putInt("Mastery", masteryRank);
		tag.putInt("PracticeTicks", practiceTicks);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		formationId = tag.hasUUID("Formation") ? tag.getUUID("Formation") : null;
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		ownerName = tag.getString("OwnerName");
		sect = tag.hasUUID("Sect") ? tag.getUUID("Sect") : null;
		sectName = tag.getString("SectName");
		radius = tag.contains("Radius") ? tag.getInt("Radius") : DEFAULT_RADIUS;
		wanted = tag.getBoolean("Wanted");
		battery = tag.getDouble("Battery");
		veins = tag.getInt("Veins");
		shatteredUntil = tag.getLong("ShatteredUntil");
		rank = tag.contains("Rank") ? tag.getInt("Rank") : -1;
		blueprint = tag.contains("Blueprint") ? tag.getCompound("Blueprint") : null;
		trusted.clear();
		for (Tag entry : tag.getList("Trusted", Tag.TAG_COMPOUND)) {
			CompoundTag player = (CompoundTag) entry;
			if (player.hasUUID("Id")) trusted.put(player.getUUID("Id"), player.getString("Name"));
		}
		formerSect = tag.getString("FormerSect");
		veilRadius = tag.contains("VeilRadius") ? tag.getInt("VeilRadius") : DEFAULT_RADIUS;
		veilWanted = tag.getBoolean("VeilWanted");
		masteryRank = tag.getInt("Mastery");
		practiceTicks = tag.getInt("PracticeTicks");
	}

	/** For the screen's status lines. */
	public Component statusLine() {
		if (cooldownSeconds() > 0) return Component.translatable(ModLang.CORE_SHATTERED, cooldownSeconds());
		return Component.translatable(isRaised() ? ModLang.CORE_RAISED : ModLang.CORE_LOWERED);
	}
}
