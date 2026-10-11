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
		tag(DamageTypeTags.BYPASSES_ARMOR).add(ModDamageTypes.SPATIAL_PRESSURE, ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED);
		tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(ModDamageTypes.SPATIAL_PRESSURE, ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED);
		tag(DamageTypeTags.BYPASSES_EFFECTS).add(ModDamageTypes.SPATIAL_PRESSURE, ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED);
		tag(DamageTypeTags.NO_IMPACT).add(ModDamageTypes.SPATIAL_PRESSURE, ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED);
		tag(DamageTypeTags.BYPASSES_RESISTANCE).add(ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED);
		tag(DamageTypeTags.BYPASSES_INVULNERABILITY).add(ModDamageTypes.SOUL_SEVERED, ModDamageTypes.SOUL_SEIZED);
	}
}
