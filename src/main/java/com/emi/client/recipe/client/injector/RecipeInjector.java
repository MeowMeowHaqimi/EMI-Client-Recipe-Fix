package com.emi.client.recipe.client.injector;

import com.emi.client.recipe.client.VanillaRecipeLoader;
import com.emi.client.recipe.client.emi.ClientRecipeFixEmiPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

import static com.emi.client.recipe.client.ClientRecipeFix.LOGGER;
import static com.emi.client.recipe.client.ClientRecipeFix.emiLoaded;

public class RecipeInjector {
    public static void performInjection(Minecraft client) {
        // 如果插件初始化时已经注册过，就跳过
        if (ClientRecipeFixEmiPlugin.localRecipesRegistered) {
            LOGGER.info("Local recipes already registered during plugin init, skipping delayed injection.");
            return;
        }

        try {
            ClientPacketListener connection = client.getConnection();
            if (connection == null) {
                LOGGER.warn("No connection, skipping injection.");
                return;
            }

            List<RecipeHolder<?>> recipes = VanillaRecipeLoader.loadVanillaRecipes(
                    connection.registryAccess()
            );
            if (recipes.isEmpty()) {
                LOGGER.warn("No local recipes loaded.");
                return;
            }

            if (emiLoaded) {
                EmiRecipeInjector.injectRecipes(recipes);
                LOGGER.info("Injected {} local recipes into EMI (fallback).", recipes.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to inject recipes", e);
        }
    }
}