package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModDamageTypes;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;

import java.util.concurrent.CompletableFuture;

/**
 * Spatial pressure is raw: armor, Protection and Resistance don't reduce it, and it doesn't knock you around. A nascent soul cut
 * from its body, or a body seized by another's soul, dies the same way, and nothing holds that off (a totem included).
 */
public class ModDamageTypeTagProvider extends FabricTagProvider<DamageType> {
	public ModDamageTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, Registries.DAMAGE_TYPE, registriesFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// A soul scattering on the Soul River is raw the same way (and never takes the last of a body's health).
		// The dead's harm, too (a ghost's blow, ghost fire).
		tag(DamageTypeTags.BYPASSES_ARMOR).add(ModDamageTypes.SPATIAL_PRESSURE, ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED, ModDamageTypes.SOUL_SCATTER,
				ModDamageTypes.JUDGE_GAZE, ModDamageTypes.GHOSTLY);
		tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(ModDamageTypes.SPATIAL_PRESSURE, ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED, ModDamageTypes.SOUL_SCATTER,
				ModDamageTypes.JUDGE_GAZE, ModDamageTypes.GHOSTLY);
		tag(DamageTypeTags.BYPASSES_EFFECTS).add(ModDamageTypes.SPATIAL_PRESSURE, ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED, ModDamageTypes.SOUL_SCATTER,
				ModDamageTypes.JUDGE_GAZE, ModDamageTypes.GHOSTLY);
		tag(DamageTypeTags.NO_IMPACT).add(ModDamageTypes.SPATIAL_PRESSURE, ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED, ModDamageTypes.SOUL_SCATTER,
				ModDamageTypes.JUDGE_GAZE, ModDamageTypes.GHOSTLY);
		// A True Immortal's gaze, like a severed soul's death: nothing stands between it and death, a totem included.
		tag(DamageTypeTags.BYPASSES_RESISTANCE).add(ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED, ModDamageTypes.JUDGE_GAZE);
		tag(DamageTypeTags.BYPASSES_INVULNERABILITY).add(ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED, ModDamageTypes.JUDGE_GAZE);
		// One's own golden core detonated: nothing at all holds that death off.
		tag(DamageTypeTags.BYPASSES_ARMOR).add(ModDamageTypes.CORE_DETONATION);
		tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(ModDamageTypes.CORE_DETONATION);
		tag(DamageTypeTags.BYPASSES_EFFECTS).add(ModDamageTypes.CORE_DETONATION);
		tag(DamageTypeTags.NO_IMPACT).add(ModDamageTypes.CORE_DETONATION);
		tag(DamageTypeTags.BYPASSES_RESISTANCE).add(ModDamageTypes.CORE_DETONATION);
		tag(DamageTypeTags.BYPASSES_INVULNERABILITY).add(ModDamageTypes.CORE_DETONATION);
		// The heaven methods' qi goes through armour; Protection and Resistance still turn some of it.
		tag(DamageTypeTags.BYPASSES_ARMOR).add(ModDamageTypes.QI_WOOD, ModDamageTypes.QI_WATER, ModDamageTypes.QI_FIRE, ModDamageTypes.QI_EARTH,
				ModDamageTypes.QI_METAL);
	}
}
