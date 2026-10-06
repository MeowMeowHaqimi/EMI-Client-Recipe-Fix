package com.emi.client.recipe.client;

import net.fabricmc.api.ClientModInitializer;

public class ClientRecipeFixClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RecipeEventHandler.registerEvents();
    }
}