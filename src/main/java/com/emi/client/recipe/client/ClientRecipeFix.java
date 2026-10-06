package com.emi.client.recipe.client;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientRecipeFix implements ModInitializer {
    public static final String MOD_ID = "emi-client-recipe-fix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean emiLoaded = false;

    @Override
    public void onInitialize() {
        emiLoaded = FabricLoader.getInstance().isModLoaded("emi");
        ClientRecipeFixConfig.loadConfig();

        if (emiLoaded) {
            LOGGER.info("[ClientRecipeFix] EMI detected, ready.");
        } else {
            LOGGER.warn("[ClientRecipeFix] EMI not found, mod will do nothing.");
        }
    }
}