package com.emi.client.recipe.client.injector;

import com.emi.client.recipe.client.emi.ClientRecipeFixEmiPlugin;
import com.emi.client.recipe.client.emi.WrappedEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

public class EmiRecipeInjector {
    public static void injectRecipes(List<RecipeHolder<?>> recipes) {
        List<EmiRecipe> emiRecipes = new ArrayList<>();

        for (RecipeHolder<?> holder : recipes) {
            try {
                Object recipe = holder.value();
                String cn = recipe.getClass().getSimpleName();

                // ★ 核心修复：使用 EMI 原版分类
                EmiRecipeCategory targetCategory = VanillaEmiRecipeCategories.CRAFTING;

                if (cn.contains("Smelting")) {
                    targetCategory = VanillaEmiRecipeCategories.SMELTING;
                } else if (cn.contains("Blasting")) {
                    targetCategory = VanillaEmiRecipeCategories.BLASTING;
                } else if (cn.contains("Smoking")) {
                    targetCategory = VanillaEmiRecipeCategories.SMOKING;
                } else if (cn.contains("CampfireCooking")) {
                    targetCategory = VanillaEmiRecipeCategories.CAMPFIRE_COOKING;
                } else if (cn.contains("Stonecutting")) {
                    targetCategory = VanillaEmiRecipeCategories.STONECUTTING;
                } else if (cn.contains("Smithing")) {
                    targetCategory = VanillaEmiRecipeCategories.SMITHING;
                }

                EmiRecipe emiRecipe = new WrappedEmiRecipe(holder, targetCategory);
                emiRecipes.add(emiRecipe);
            } catch (Exception e) {
                // 单个失败不影响其他
            }
        }

        ClientRecipeFixEmiPlugin.addRecipes(emiRecipes);
    }
}