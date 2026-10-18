package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * What the Netherworld keeps on a cultivator: the ghostly qi pool, yin corruption, a Soul River journey under way. Saved under
 * one "Netherworld" compound in PlayerCultivation, and only when something in it differs from the defaults, so untouched players
 * and every NPC save as before; every key falls back to its default when missing.
 */
public final class NetherworldState {
	/** The ghostly qi pool (see {@link GhostQi#poolMax}). */
	private double ghostQi;
	/** Yin corruption, 0 (none) to 1 (the body gives way; see {@link YinCorruption}). */
	private double yinCorruption;
	/** Game time until which a Yang Preservation Pill holds the corruption still; 0 for none. */
	private long yinPausedUntil;
	/** The Soul River journey under way (saved, so a relog starts it over), and where a journey down set out from. */
	private SoulRiver.Route river = SoulRiver.Route.NONE;
	private int riverFromX, riverFromZ;
	/** Game time before which a rift turns the player away (after the river, and longer after their soul scattered on it). */
	private long riverCooldownUntil;
	/** Synced, never saved: the ghostly qi density where the body is (see {@link GhostQi#density}), refreshed every second. */
	private double density;

	// The dead (see GhostSystem): all saved, so a ghost logs back in a ghost and a death survives a crash before the respawn.
	/** One of the dead. */
	private boolean ghost;
	/** Died, and bound for the Netherworld at the respawn (the real respawn point set aside meanwhile, below). */
	private boolean boundBelow;
	/** Dispersed, and reforming at the Ghost Gate (a ghost who logs out meanwhile reforms on their return). */
	private boolean reforming;
	/** Where they died (or the yin took them): a dimension id and a position; empty when none. */
	private String deathDimension = "";
	private int deathX, deathY, deathZ;
	/** The player's own respawn point, set aside while the Gate's stands in for it at a death's respawn; empty when none. */
	private CompoundTag savedRespawn;
	/** Drank Meng Po's soup: medicinal toxicity is washed away at the next return to life. */
	private boolean memoryCleansed;
	/** Has been a Ghost King among the dead (GhostSystem#isKing): its form stays theirs, alive again too (TwoPaths#hasKingForm). */
	private boolean reignedKing;

	public boolean ghost() { return ghost; }
	public void setGhost(boolean ghost) { this.ghost = ghost; }
	public boolean boundBelow() { return boundBelow; }
	public void setBoundBelow(boolean bound) { this.boundBelow = bound; }
	public boolean reforming() { return reforming; }
	public void setReforming(boolean reforming) { this.reforming = reforming; }
	public boolean memoryCleansed() { return memoryCleansed; }
	public void setMemoryCleansed(boolean cleansed) { this.memoryCleansed = cleansed; }
	public boolean reignedKing() { return reignedKing; }
	public void setReignedKing(boolean reigned) { this.reignedKing = reigned; }

	public boolean hasDeathPlace() { return !deathDimension.isEmpty(); }
	public String deathDimension() { return deathDimension; }
	public net.minecraft.core.BlockPos deathPos() { return new net.minecraft.core.BlockPos(deathX, deathY, deathZ); }

	public void setDeathPlace(String dimension, net.minecraft.core.BlockPos pos) {
		this.deathDimension = dimension == null ? "" : dimension;
		this.deathX = pos.getX();
		this.deathY = pos.getY();
		this.deathZ = pos.getZ();
		this.sawHome = false; // a new death: the Terrace has something new to show
	}

	// The ghost methods (see GhostMethods) and spells (see Spells): saved by id, never by ordinal.
	/** Each method learned: the layer reached (1 to its last, GhostMethod#layers) and the progress through it. */
	private final java.util.Map<GhostMethod, double[]> methods = new java.util.EnumMap<>(GhostMethod.class);
	/** The method meditation cultivates (null: none chosen). */
	private GhostMethod activeMethod;
	/** The spell the cast key casts (null: none readied). */
	private Spell readiedSpell;
	/** The spell wheel's slots, clockwise from the top (null: empty). */
	private final Spell[] spellWheel = new Spell[Spells.WHEEL_SLOTS];

	/** The layer reached in {@code method}: 0 if it isn't known. */
	public int layer(GhostMethod method) {
		double[] entry = methods.get(method);
		return entry == null ? 0 : (int) entry[0];
	}

	public double progress(GhostMethod method) {
		double[] entry = methods.get(method);
		return entry == null ? 0 : entry[1];
	}

	public boolean knows(GhostMethod method) { return methods.containsKey(method); }
	public boolean knowsAnyMethod() { return !methods.isEmpty(); }

	/** Sets a method's layer (1 to its last, GhostMethod#layers; 0 forgets it) and progress through it. */
	public void setMethod(GhostMethod method, int layer, double progress) {
		if (layer <= 0) {
			methods.remove(method);
			if (activeMethod == method) activeMethod = null;
			return;
		}
		methods.put(method, new double[]{Math.min(method.layers(), layer), Math.max(0, progress)});
	}

	/**
	 * How the saved layers are laid out: 2 since each foundation spans Qi Refining up to its own realm (MethodLayers). Before, a
	 * foundation past the first had nine layers of its own realm alone, so its layer L then is its layer 9 x (tier - 1) + L now.
	 */
	private static final int METHOD_LAYOUT = 2;

	public java.util.Set<GhostMethod> knownMethods() { return java.util.Collections.unmodifiableSet(methods.keySet()); }

	public GhostMethod activeMethod() { return activeMethod; }
	public void setActiveMethod(GhostMethod method) { this.activeMethod = method == null || !knows(method) ? null : method; }

	public Spell readiedSpell() { return readiedSpell; }
	public void setReadiedSpell(Spell spell) { this.readiedSpell = spell; }

	public Spell spellSlot(int slot) { return slot >= 0 && slot < spellWheel.length ? spellWheel[slot] : null; }

	/** Puts {@code spell} in {@code slot} (null empties it); if it was in another slot, the two slots trade places. */
	public void setSpellSlot(int slot, Spell spell) {
		if (slot < 0 || slot >= spellWheel.length) return;
		if (spell != null) {
			for (int i = 0; i < spellWheel.length; i++) if (spellWheel[i] == spell && i != slot) spellWheel[i] = spellWheel[slot];
		}
		spellWheel[slot] = spell;
	}

	/** Puts {@code spell} in the first empty slot, unless it is on the wheel already or the wheel is full. */
	public void addToWheel(Spell spell) {
		for (Spell s : spellWheel) if (s == spell) return;
		for (int i = 0; i < spellWheel.length; i++) {
			if (spellWheel[i] == null) {
				spellWheel[i] = spell;
				return;
			}
		}
	}

	/** The set-aside respawn point (see GhostSystem), or null. */
	public CompoundTag savedRespawn() { return savedRespawn; }
	public void setSavedRespawn(CompoundTag respawn) { this.savedRespawn = respawn == null ? null : respawn.copy(); }

	public double ghostQi() { return ghostQi; }
	public void setGhostQi(double ghostQi) { this.ghostQi = Math.max(0, ghostQi); }

	public double yinCorruption() { return yinCorruption; }
	public void setYinCorruption(double corruption) { this.yinCorruption = Mth.clamp(corruption, 0, 1); }

	public long yinPausedUntil() { return yinPausedUntil; }
	public void setYinPausedUntil(long gameTime) { this.yinPausedUntil = Math.max(0, gameTime); }

	public SoulRiver.Route river() { return river; }
	public int riverFromX() { return riverFromX; }
	public int riverFromZ() { return riverFromZ; }

	/** Sets the journey under way; {@code fromX}, {@code fromZ} are where a journey down set out (ignored going up). */
	public void setRiver(SoulRiver.Route route, int fromX, int fromZ) {
		this.river = route == null ? SoulRiver.Route.NONE : route;
		this.riverFromX = fromX;
		this.riverFromZ = fromZ;
	}

	public long riverCooldownUntil() { return riverCooldownUntil; }
	public void setRiverCooldownUntil(long gameTime) { this.riverCooldownUntil = Math.max(0, gameTime); }

	public double density() { return density; }
	public void setDensity(double density) { this.density = Math.max(0, density); }

	/** A Yin Gathering Pill's boost to the ghost method (0.25 = +25%), while its effect lasts (PillItem, MeditationManager). */
	public double methodBoost() { return methodBoost; }
	public void setMethodBoost(double boost) { this.methodBoost = Math.max(0, boost); }
	private double methodBoost;

	/** The Terrace for Gazing Homeward has shown this ghost where it died (once a death: GhostGate#terrace). */
	public boolean sawHome() { return sawHome; }
	public void setSawHome(boolean saw) { this.sawHome = saw; }
	private boolean sawHome;

	/** Empty when everything is at its default (then PlayerCultivation leaves the compound out altogether). */
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		if (ghostQi > 0) tag.putDouble("GhostQi", ghostQi);
		if (yinCorruption > 0) tag.putDouble("YinCorruption", yinCorruption);
		if (yinPausedUntil > 0) tag.putLong("YinPausedUntil", yinPausedUntil);
		if (river != SoulRiver.Route.NONE) {
			tag.putString("River", river.name());
			tag.putInt("RiverFromX", riverFromX);
			tag.putInt("RiverFromZ", riverFromZ);
		}
		if (riverCooldownUntil > 0) tag.putLong("RiverCooldownUntil", riverCooldownUntil);
		if (ghost) tag.putBoolean("Ghost", true);
		if (boundBelow) tag.putBoolean("BoundBelow", true);
		if (reforming) tag.putBoolean("Reforming", true);
		if (memoryCleansed) tag.putBoolean("MemoryCleansed", true);
		if (reignedKing) tag.putBoolean("ReignedKing", true);
		if (hasDeathPlace()) {
			CompoundTag place = new CompoundTag();
			place.putString("Dimension", deathDimension);
			place.putInt("X", deathX);
			place.putInt("Y", deathY);
			place.putInt("Z", deathZ);
			tag.put("DeathPlace", place);
		}
		if (savedRespawn != null) tag.put("SavedRespawn", savedRespawn.copy());
		if (!methods.isEmpty()) {
			CompoundTag learned = new CompoundTag();
			for (java.util.Map.Entry<GhostMethod, double[]> entry : methods.entrySet()) {
				CompoundTag one = new CompoundTag();
				one.putInt("Layer", (int) entry.getValue()[0]);
				one.putDouble("Progress", entry.getValue()[1]);
				learned.put(entry.getKey().getId(), one);
			}
			tag.put("Methods", learned);
			tag.putInt("MethodLayout", METHOD_LAYOUT);
		}
		if (activeMethod != null) tag.putString("ActiveMethod", activeMethod.getId());
		if (readiedSpell != null) tag.putString("ReadiedSpell", readiedSpell.getId());
		boolean anySlot = false;
		net.minecraft.nbt.ListTag wheel = new net.minecraft.nbt.ListTag();
		for (Spell spell : spellWheel) {
			wheel.add(net.minecraft.nbt.StringTag.valueOf(spell == null ? "" : spell.getId()));
			anySlot |= spell != null;
		}
		if (anySlot) tag.put("SpellWheel", wheel);
		if (methodBoost > 0) tag.putDouble("MethodBoost", methodBoost);
		if (sawHome) tag.putBoolean("SawHome", true);
		return tag;
	}

	/** Reads what {@link #save} wrote; a missing key (an older save, or an empty compound) keeps its default. */
	public void load(CompoundTag tag) {
		setGhostQi(tag.getDouble("GhostQi"));
		setYinCorruption(tag.getDouble("YinCorruption"));
		setYinPausedUntil(tag.getLong("YinPausedUntil"));
		setRiver(SoulRiver.Route.byName(tag.getString("River")), tag.getInt("RiverFromX"), tag.getInt("RiverFromZ"));
		setRiverCooldownUntil(tag.getLong("RiverCooldownUntil"));
		ghost = tag.getBoolean("Ghost");
		boundBelow = tag.getBoolean("BoundBelow");
		reforming = tag.getBoolean("Reforming");
		memoryCleansed = tag.getBoolean("MemoryCleansed");
		reignedKing = tag.getBoolean("ReignedKing");
		CompoundTag place = tag.getCompound("DeathPlace");
		if (place.contains("Dimension")) {
			setDeathPlace(place.getString("Dimension"), new net.minecraft.core.BlockPos(place.getInt("X"), place.getInt("Y"), place.getInt("Z")));
		} else {
			deathDimension = "";
		}
		savedRespawn = tag.contains("SavedRespawn") ? tag.getCompound("SavedRespawn").copy() : null;
		methods.clear();
		CompoundTag learned = tag.getCompound("Methods");
		boolean oldLayout = tag.getInt("MethodLayout") < METHOD_LAYOUT;
		for (String id : learned.getAllKeys()) {
			GhostMethod method = GhostMethod.byId(id);
			CompoundTag one = learned.getCompound(id);
			if (method == null) continue; // an unknown id (a method since removed) is dropped
			int layer = one.getInt("Layer");
			// Saved before the foundations spanned the realms below their own: the same stage of the same realm, the same progress.
			if (oldLayout && method.tier() > 1 && layer > 0) layer = Math.min(GhostMethods.LAYERS, layer) + MethodLayers.PER_REALM * (method.tier() - 1);
			setMethod(method, layer, one.getDouble("Progress"));
		}
		activeMethod = null;
		setActiveMethod(GhostMethod.byId(tag.getString("ActiveMethod")));
		readiedSpell = Spell.byId(tag.getString("ReadiedSpell"));
		java.util.Arrays.fill(spellWheel, null);
		net.minecraft.nbt.ListTag wheel = tag.getList("SpellWheel", net.minecraft.nbt.Tag.TAG_STRING);
		for (int i = 0; i < Math.min(wheel.size(), spellWheel.length); i++) spellWheel[i] = Spell.byId(wheel.getString(i));
		setMethodBoost(tag.getDouble("MethodBoost"));
		sawHome = tag.getBoolean("SawHome");
	}
}
