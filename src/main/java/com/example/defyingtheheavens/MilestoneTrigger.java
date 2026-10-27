package com.example.defyingtheheavens;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

/**
 * The mod's one criterion trigger ({@code defying-the-heavens:milestone}): a milestone of the path reached, named by a string
 * ("realm/nascent_soul", "spell/fire", "ghost/king": Milestones). An advancement asks for it with {@code {"milestone": "<name>"}}; the
 * mod's code fires it where the milestone happens, and Milestones' sweep where it is a standing state.
 */
public class MilestoneTrigger extends SimpleCriterionTrigger<MilestoneTrigger.TriggerInstance> {
	static final ResourceLocation ID = DefyingTheHeavens.id("milestone");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
		return new TriggerInstance(player, GsonHelper.getAsString(json, "milestone"));
	}

	/** {@code player} has reached {@code milestone}: every advancement asking for it has that criterion met. */
	public void trigger(ServerPlayer player, String milestone) {
		trigger(player, instance -> instance.milestone.equals(milestone));
	}

	public static class TriggerInstance extends AbstractCriterionTriggerInstance {
		final String milestone;

		public TriggerInstance(ContextAwarePredicate player, String milestone) {
			super(ID, player);
			this.milestone = milestone;
		}

		/** The criterion "has reached {@code milestone}", for the advancements' data (ModAdvancements). */
		public static TriggerInstance reached(String milestone) {
			return new TriggerInstance(ContextAwarePredicate.ANY, milestone);
		}

		public String milestone() {
			return milestone;
		}

		@Override
		public JsonObject serializeToJson(SerializationContext context) {
			JsonObject json = super.serializeToJson(context);
			json.addProperty("milestone", milestone);
			return json;
		}
	}
}
