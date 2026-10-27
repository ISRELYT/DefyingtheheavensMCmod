package com.example.defyingtheheavens.client;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** The mod's advancements (ModAdvancements) written out as data: shown with a toast, told in chat, nothing given (the user's pick). */
public class ModAdvancementProvider extends FabricAdvancementProvider {
	public ModAdvancementProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateAdvancement(Consumer<Advancement> consumer) {
		Map<String, Advancement> made = new HashMap<>();
		for (ModAdvancements.Entry entry : ModAdvancements.all()) {
			Advancement.Builder builder = Advancement.Builder.advancement();
			if (entry.parent() != null) {
				Advancement parent = made.get(entry.parent());
				if (parent == null) throw new IllegalStateException("Advancement " + entry.id() + " before its parent " + entry.parent());
				builder.parent(parent);
			}
			builder.display(entry.icon(), Component.translatable(entry.titleKey()), Component.translatable(entry.descriptionKey()), entry.background(),
					entry.frame(), true, true, entry.hidden());
			for (Map.Entry<String, CriterionTriggerInstance> criterion : entry.criteria().entrySet()) builder.addCriterion(criterion.getKey(), criterion.getValue());
			builder.requirements(entry.anyOf() ? RequirementsStrategy.OR : RequirementsStrategy.AND);
			made.put(entry.id(), builder.save(consumer, entry.location().toString()));
		}
	}
}
