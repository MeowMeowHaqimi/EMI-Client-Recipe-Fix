package com.emi.client.recipe.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import com.emi.client.recipe.client.injector.RecipeInjector;

import static com.emi.client.recipe.client.ClientRecipeFix.LOGGER;
import static com.emi.client.recipe.client.ClientRecipeFix.emiLoaded;

public class RecipeEventHandler {
    private static int ticksUntilInjection = -1;
    public static boolean serverRecipesReceived = false;

    public static void registerEvents() {
        if (!emiLoaded) {
            LOGGER.warn("EMI not detected! No injection will be performed.");
            return;
        }

        LOGGER.info("EMI detected, enabling local recipe fallback.");

        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> {
            serverRecipesReceived = true;
            LOGGER.info("Server sent synchronized recipes, overriding local ones.");
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            serverRecipesReceived = false;
            LOGGER.info("Joined server, local recipe injection in {} ticks",
                    ClientRecipeFixConfig.injectionDelayTicks);
            ticksUntilInjection = ClientRecipeFixConfig.injectionDelayTicks;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ticksUntilInjection = -1;
            serverRecipesReceived = false;
            LOGGER.info("Disconnected from server.");
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (ticksUntilInjection > 0) {
                ticksUntilInjection--;
            } else if (ticksUntilInjection == 0) {
                ticksUntilInjection = -1;
                if (!serverRecipesReceived) {
                    LOGGER.info("No server recipes received, injecting local recipes.");
                    RecipeInjector.performInjection(mc);
                } else {
                    LOGGER.info("Server recipes already received, skipping local injection.");
                }
            }
        });
    }
}