package com.emi.client.recipe.client.emi;

import com.emi.client.recipe.client.ClientRecipeFix;
import com.emi.client.recipe.client.RecipeEventHandler;
import com.emi.client.recipe.client.VanillaRecipeLoader;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.recipe.EmiCookingRecipe;
import dev.emi.emi.recipe.EmiShapedRecipe;
import dev.emi.emi.recipe.EmiShapelessRecipe;
import dev.emi.emi.recipe.EmiStonecuttingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.List;

@EmiEntrypoint
public class ClientRecipeFixEmiPlugin implements EmiPlugin {

    public static boolean localRecipesRegistered = false;
    private static EmiRegistry recipeRegistry;

    @Override
    public void register(EmiRegistry registry) {
        recipeRegistry = registry;
        ClientRecipeFix.LOGGER.info("EMI plugin initialized.");

        // ★ 核心修复：如果服务器已经同步了配方，就跳过本地加载，避免重复
        if (RecipeEventHandler.serverRecipesReceived) {
            ClientRecipeFix.LOGGER.info("Server recipes already received, skipping local recipe loading.");
            return;
        }

        // 加载本地配方
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) {
                ClientRecipeFix.LOGGER.warn("Level not ready, skip.");
                return;
            }

            List<RecipeHolder<?>> recipes = VanillaRecipeLoader.loadVanillaRecipes(
                    client.level.registryAccess()
            );
            if (recipes.isEmpty()) {
                ClientRecipeFix.LOGGER.warn("No local recipes loaded.");
                return;
            }

            int count = 0;
            for (RecipeHolder<?> holder : recipes) {
                try {
                    var recipe = holder.value();

                    // 过滤特殊配方，避免 EMI 内部渲染 Tooltip 崩溃
                    if (recipe instanceof CustomRecipe) continue;

                    // 1. 工作台：有序合成
                    if (recipe instanceof ShapedRecipe shaped) {
                        registry.addRecipe(new EmiShapedRecipe(shaped));
                    }
                    // 2. 工作台：无序合成
                    else if (recipe instanceof ShapelessRecipe shapeless) {
                        registry.addRecipe(new EmiShapelessRecipe(shapeless));
                    }
                    // 3. 熔炉
                    else if (recipe instanceof SmeltingRecipe smelting) {
                        registry.addRecipe(new EmiCookingRecipe(smelting, VanillaEmiRecipeCategories.SMELTING, 200, false));
                    }
                    // 4. 高炉
                    else if (recipe instanceof BlastingRecipe blasting) {
                        registry.addRecipe(new EmiCookingRecipe(blasting, VanillaEmiRecipeCategories.BLASTING, 100, false));
                    }
                    // 5. 烟熏炉
                    else if (recipe instanceof SmokingRecipe smoking) {
                        registry.addRecipe(new EmiCookingRecipe(smoking, VanillaEmiRecipeCategories.SMOKING, 100, false));
                    }
                    // 6. 营火
                    else if (recipe instanceof CampfireCookingRecipe campfire) {
                        registry.addRecipe(new EmiCookingRecipe(campfire, VanillaEmiRecipeCategories.CAMPFIRE_COOKING, 600, false));
                    }
                    // 7. 切石机
                    else if (recipe instanceof StonecutterRecipe stonecutting) {
                        registry.addRecipe(new EmiStonecuttingRecipe(stonecutting));
                    }
                    count++;
                } catch (Exception e) {
                    ClientRecipeFix.LOGGER.warn("注入配方 {} 失败", holder.id(), e);
                }
            }
            localRecipesRegistered = true;
            ClientRecipeFix.LOGGER.info("Registered {} local recipes.", count);
        } catch (Exception e) {
            ClientRecipeFix.LOGGER.error("Failed to register local recipes.", e);
        }
    }

    public static void addRecipes(List<dev.emi.emi.api.recipe.EmiRecipe> recipes) {
        if (recipeRegistry == null) return;
        for (var recipe : recipes) {
            recipeRegistry.addRecipe(recipe);
        }
    }
}