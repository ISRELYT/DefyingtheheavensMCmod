package com.example.defyingtheheavens;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/** Carries the owning storm's height so distant bolts and nearby observers agree on their length. */
public class TribulationLightning extends LightningBolt {
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(
            TribulationLightning.class, EntityDataSerializers.FLOAT);

    public TribulationLightning(EntityType<? extends LightningBolt> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HEIGHT, TribulationCloud.BASE_HEIGHT_ABOVE_PLAYER);
    }

    /** Call after positioning the strike, before spawning it on the server. */
    public void setCloudBaseY(double cloudY) {
        entityData.set(HEIGHT, (float) Math.max(1.0, cloudY - getY()));
    }

    public float renderHeight() { return entityData.get(HEIGHT); }
}
