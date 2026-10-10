package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
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

	public FormationCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FORMATION_CORE, pos, state);
	}

	/** Qi per second a barrier of this radius costs to keep up: 1.3 at radius 8, 13 at 32, 52 at 64. */
	public static double upkeep(int radius) {
		return 0.5 + radius * radius / 80.0;
	}

	public double supply() { return veins * QiVeinBlock.QI_PER_SECOND; }
	public int getRadius() { return radius; }
	public boolean isWanted() { return wanted; }
	public double getBattery() { return battery; }
	public int getVeins() { return veins; }
	public int getRank() { return rank; }
	public UUID getSect() { return sect; }
	public UUID getFormationId() { return formationId; }
	public String getOwnerName() { return ownerName; }
	public String getSectName() { return sectName; }

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
		this.radius = Math.max(Formations.MIN_RADIUS, Math.min(Formations.MAX_RADIUS, radius));
		this.wanted = true;
		this.owner = null;
		this.rank = masterRank;
		this.battery = BATTERY;
		setChanged();
		if (level instanceof ServerLevel server) {
			Formation formation = formation(server);
			Formations.resize(server, formation, this.radius);
			Formations.update(server, formation, rank, null, sect);
		}
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
		return formation;
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

	/** Once a second: the veins fill the battery, the barrier draws its upkeep, and it rises or falls accordingly. */
	private void economy(ServerLevel level) {
		Formation formation = formation(level);
		double upkeep = formation.isRaised() ? upkeep(formation.getRadius()) : 0;
		double before = battery;
		battery = Math.max(0, Math.min(BATTERY, battery + supply() - upkeep));
		if (battery != before) setChanged();
		if (formation.isRaised() && !wanted) {
			Formations.lower(level, formation);
			level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
			SectManager.onBarrierChanged(level, sect, false, false);
		} else if (formation.isRaised() && battery <= 0 && supply() < upkeep) {
			collapse(level, false);
		} else if (!formation.isRaised() && wanted && level.getGameTime() >= shatteredUntil
				&& (supply() >= upkeep(radius) || battery >= BATTERY * RAISE_FRACTION)) {
			if (formation.getRadius() != radius) Formations.resize(level, formation, radius);
			Formations.raise(level, formation);
			level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.2f);
			SectManager.onBarrierChanged(level, sect, true, false);
		}
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
		if (!(level instanceof ServerLevel server)) return;
		battery = Math.max(0, battery - BREACH_COST);
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
		if (sect != null) return false;
		if (owner == null) {
			setOwner(player);
			return true;
		}
		return owner.equals(player.getUUID());
	}

	/** From the screen: the radius to keep the barrier at, and whether it should be up. */
	public void configure(ServerPlayer player, int newRadius, boolean newWanted) {
		if (!(level instanceof ServerLevel server) || !canControl(player)) return;
		radius = Math.max(Formations.MIN_RADIUS, Math.min(Formations.MAX_RADIUS, newRadius));
		wanted = newWanted;
		if (sect == null && owner != null && owner.equals(player.getUUID())) rank = Formations.rankOf(player); // as strong as its maker now
		setChanged();
		Formation formation = formation(server);
		if (formation.isRaised() && formation.getRadius() != radius) Formations.resize(server, formation, radius);
		economy(server); // switch on or off at once rather than within the second
	}

	/** The core's block was broken or replaced: its barrier comes down for good. */
	public void onRemovedFromWorld(ServerLevel level) {
		if (formationId != null) Formations.dissolve(level, formationId);
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
	}

	/** For the screen's status lines. */
	public Component statusLine() {
		if (cooldownSeconds() > 0) return Component.translatable(ModLang.CORE_SHATTERED, cooldownSeconds());
		return Component.translatable(isRaised() ? ModLang.CORE_RAISED : ModLang.CORE_LOWERED);
	}
}
