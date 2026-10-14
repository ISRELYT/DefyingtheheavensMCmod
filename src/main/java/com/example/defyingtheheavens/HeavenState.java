package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * The heaven methods a cultivator knows (see HeavenMethods): each one's layer understood (1 to its last), the settling toward the next
 * (HeavenMethods#settle, in cultivation: HeavenMethods#settleCost), and the one meditation cultivates by. Saved by id under one
 * "HeavenMethods" compound in PlayerCultivation, and only when something is known. Saves from before layers settled kept other
 * progress: it is dropped (PlayerCultivation aligns their layers to the realm on load).
 */
public final class HeavenState {
	/** What progress is saved as: 3, settling toward the next layer. Before it, progress through the layer one was on. */
	private static final int UNITS = 3;
	private final Map<HeavenMethod, double[]> methods = new EnumMap<>(HeavenMethod.class);
	private HeavenMethod activeMethod;

	/** The layer understood in {@code method}: 0 if it isn't known. */
	public int layer(HeavenMethod method) {
		double[] entry = methods.get(method);
		return entry == null ? 0 : (int) entry[0];
	}

	public double progress(HeavenMethod method) {
		double[] entry = methods.get(method);
		return entry == null ? 0 : entry[1];
	}

	public boolean knows(HeavenMethod method) { return methods.containsKey(method); }
	public boolean knowsAny() { return !methods.isEmpty(); }

	/** The methods known. */
	public Set<HeavenMethod> known() { return Collections.unmodifiableSet(methods.keySet()); }

	/** Sets a method's layer (1 to its last, HeavenMethod#layers; 0 forgets it) and its progress through it. */
	public void setMethod(HeavenMethod method, int layer, double progress) {
		if (layer <= 0) {
			methods.remove(method);
			if (activeMethod == method) activeMethod = null;
			return;
		}
		methods.put(method, new double[]{Math.min(method.layers(), layer), Math.max(0, progress)});
	}

	public HeavenMethod activeMethod() { return activeMethod; }
	public void setActiveMethod(HeavenMethod method) { this.activeMethod = method == null || !knows(method) ? null : method; }

	/** Forgets everything (the client, leaving a world). */
	public void clear() {
		methods.clear();
		activeMethod = null;
	}

	/** Empty when nothing is known (then PlayerCultivation leaves the compound out). */
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		if (methods.isEmpty()) return tag;
		CompoundTag learned = new CompoundTag();
		for (Map.Entry<HeavenMethod, double[]> entry : methods.entrySet()) {
			CompoundTag one = new CompoundTag();
			one.putInt("Layer", (int) entry.getValue()[0]);
			one.putDouble("Progress", entry.getValue()[1]);
			learned.put(entry.getKey().getId(), one);
		}
		tag.put("Methods", learned);
		if (activeMethod != null) tag.putString("Active", activeMethod.getId());
		tag.putInt("Units", UNITS);
		return tag;
	}

	public void load(CompoundTag tag) {
		methods.clear();
		CompoundTag learned = tag.getCompound("Methods");
		boolean older = tag.getInt("Units") < UNITS;
		for (String id : learned.getAllKeys()) {
			HeavenMethod method = HeavenMethod.byId(id);
			if (method == null) continue; // a method since removed is dropped
			CompoundTag one = learned.getCompound(id);
			setMethod(method, Math.max(1, one.getInt("Layer")), older ? 0 : one.getDouble("Progress"));
		}
		activeMethod = null;
		setActiveMethod(HeavenMethod.byId(tag.getString("Active")));
	}
}
