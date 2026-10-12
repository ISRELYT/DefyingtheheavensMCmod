package com.example.defyingtheheavens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/** The mod's creative tab (ModCreativeTab). */
public class CreativeTabGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void theTabHoldsEverythingTheModAdds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(ModCreativeTab.TAB) != null
				&& BuiltInRegistries.CREATIVE_MODE_TAB.getKey(ModCreativeTab.TAB).equals(DefyingTheHeavens.id("main")), "The tab is registered");
		// Built as the client builds it when the inventory opens (a stack listed twice would throw here).
		ModCreativeTab.TAB.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
		Collection<ItemStack> shown = ModCreativeTab.TAB.getDisplayItems();
		int mods = 0;
		for (Item item : BuiltInRegistries.ITEM) {
			if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(DefyingTheHeavens.MOD_ID)) continue;
			mods++;
			helper.assertTrue(shown.stream().anyMatch(stack -> stack.is(item)), "The tab shows " + BuiltInRegistries.ITEM.getKey(item));
		}
		helper.assertTrue(shown.stream().allMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(DefyingTheHeavens.MOD_ID)),
				"and only the mod's");
		helper.assertTrue(shown.stream().anyMatch(stack -> stack.is(ModItems.CULTIVATOR_SPAWN_EGG)), "The spawn egg is in it");
		for (PillGrade grade : PillGrade.values()) {
			helper.assertTrue(shown.stream().anyMatch(stack -> stack.is(ModItems.CORE_PILL) && PillItem.grade(stack) == grade), "Core Pills at " + grade + " grade");
		}
		helper.assertTrue(shown.size() > mods, "Some items show more than once, at each grade or style (" + shown.size() + " stacks, " + mods + " items)");

		JsonObject lang;
		try (InputStream in = CreativeTabGameTests.class.getResourceAsStream("/assets/defying-the-heavens/lang/en_us.json")) {
			helper.assertTrue(in != null, "The English lang file is on the classpath");
			lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
		helper.assertTrue(lang.has(ModLang.CREATIVE_TAB), "The tab's title is in the lang file");
		helper.succeed();
	}
}
