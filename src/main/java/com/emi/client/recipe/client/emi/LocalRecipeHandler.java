package com.emi.client.recipe.client.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LocalRecipeHandler<T extends AbstractContainerMenu>
        implements StandardRecipeHandler<T> {

    @Override
    public List<Slot> getInputSources(T menu) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 10; i < menu.slots.size(); i++) {
            slots.add(menu.slots.get(i));
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(T menu) {
        List<Slot> slots = new ArrayList<>();
        if (menu instanceof CraftingMenu) {
            for (int i = 1; i <= 9; i++) {
                slots.add(menu.slots.get(i));
            }
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(EmiRecipe recipe, T menu) {
        List<Slot> slots = new ArrayList<>();
        if (!(menu instanceof CraftingMenu)) return slots;

        if (!(recipe instanceof WrappedEmiRecipe wrapped)) {
            for (int i = 1; i <= 9; i++) slots.add(menu.slots.get(i));
            return slots;
        }

        int rw = Math.max(wrapped.getRecipeWidth(), 1);
        List<EmiIngredient> raw = wrapped.getRawInputs();

        for (int i = 0; i < raw.size(); i++) {
            if (raw.get(i) == null) continue;
            int col = i % rw;
            int row = i / rw;
            if (col >= 3 || row >= 3) continue;
            int slotIdx = row * 3 + col + 1;
            slots.add(menu.slots.get(slotIdx));
        }
        return slots;
    }

    @Override
    public @Nullable Slot getOutputSlot(T menu) {
        if (menu instanceof CraftingMenu) {
            return menu.slots.get(0);
        }
        return null;
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof WrappedEmiRecipe;
    }

    /**
     * 工作台：只有 3x3 以内的配方才算可合成。
     */
    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
        if (recipe instanceof WrappedEmiRecipe wrapped) {
            int rw = Math.max(wrapped.getRecipeWidth(), 1);
            int rh = Math.max(wrapped.getRecipeHeight(), 1);
            if (rw > 3 || rh > 3) return false;
        }
        return context.getInventory().canCraft(recipe);
    }
}