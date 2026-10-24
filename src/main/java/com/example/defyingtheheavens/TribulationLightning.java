package com.example.defyingtheheavens;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/**
 * Carries the owning storm's height so distant bolts and nearby observers agree on their length, and how much thicker than
 * vanilla's it is drawn (TribulationManager#boltScale: the greater the tribulation, the thicker the bolt).
 */
public class TribulationLightning extends LightningBolt {
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(
            TribulationLightning.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(
            TribulationLightning.class, EntityDataSerializers.FLOAT);
    /** The Netherworld's tribulation's yin lightning (the ghost path's): drawn cyan. */
    private static final EntityDataAccessor<Boolean> GHOST = SynchedEntityData.defineId(
            TribulationLightning.class, EntityDataSerializers.BOOLEAN);
    /**
     * One of strikes in quick succession: drawn and heard, but without vanilla's flash of the whole sky (LightningBoltMixin), so a
     * storm's many bolts don't strobe it.
     */
    private static final EntityDataAccessor<Boolean> DIM = SynchedEntityData.defineId(
            TribulationLightning.class, EntityDataSerializers.BOOLEAN);
    /**
     * A spell's bolt in a colour of its own (0xRRGGBB; 0: the tribulation's blue, or the ghost's cyan): Spring Thunder of the Azure
     * Emperor's jade (WoodSpells).
     */
    private static final EntityDataAccessor<Integer> TINT = SynchedEntityData.defineId(
            TribulationLightning.class, EntityDataSerializers.INT);

    public TribulationLightning(EntityType<? extends LightningBolt> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HEIGHT, TribulationCloud.BASE_HEIGHT_ABOVE_PLAYER);
        entityData.define(SCALE, 1.0f);
        entityData.define(GHOST, false);
        entityData.define(DIM, false);
        entityData.define(TINT, 0);
    }

    public void setTint(int rgb) { entityData.set(TINT, rgb & 0xFFFFFF); }

    public int tint() { return entityData.get(TINT); }

    public void setGhost(boolean ghost) { entityData.set(GHOST, ghost); }

    public boolean isGhost() { return entityData.get(GHOST); }

    public void setDim(boolean dim) { entityData.set(DIM, dim); }

    public boolean isDim() { return entityData.get(DIM); }

    /** Call after positioning the strike, before spawning it on the server. */
    public void setCloudBaseY(double cloudY) {
        entityData.set(HEIGHT, (float) Math.max(1.0, cloudY - getY()));
    }

    public float renderHeight() { return entityData.get(HEIGHT); }

    /** How much thicker than vanilla's bolt it is drawn (its zig-zag stays vanilla's); set before spawning it. */
    public void setScale(float scale) {
        entityData.set(SCALE, Math.max(0.1f, scale));
    }

    public float renderScale() { return entityData.get(SCALE); }
}
