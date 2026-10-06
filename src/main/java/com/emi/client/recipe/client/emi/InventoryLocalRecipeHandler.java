package com.emi.client.recipe.client.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InventoryLocalRecipeHandler implements StandardRecipeHandler<InventoryMenu> {

    /** 背包合成最大 2x2 */
    private static final int MAX_W = 2;
    private static final int MAX_H = 2;

    @Override
    public List<Slot> getInputSources(InventoryMenu menu) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 9; i < menu.slots.size(); i++) {
            slots.add(menu.slots.get(i));
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(InventoryMenu menu) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            slots.add(menu.slots.get(i));
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(EmiRecipe recipe, InventoryMenu menu) {
        List<Slot> slots = new ArrayList<>();
        if (!(recipe instanceof WrappedEmiRecipe wrapped)) {
            return getCraftingSlots(menu);
        }

        int rw = Math.max(wrapped.getRecipeWidth(), 1);
        int rh = Math.max(wrapped.getRecipeHeight(), 1);
        if (rw > MAX_W || rh > MAX_H) return slots;

        List<EmiIngredient> raw = wrapped.getRawInputs();
        for (int i = 0; i < raw.size(); i++) {
            if (raw.get(i) == null) continue;
            int col = i % rw;
            int row = i / rw;
            if (col >= MAX_W || row >= MAX_H) continue;
            int slotIdx = row * MAX_W + col + 1;
            slots.add(menu.slots.get(slotIdx));
        }
        return slots;
    }

    @Override
    public @Nullable Slot getOutputSlot(InventoryMenu menu) {
        return menu.slots.get(0);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof WrappedEmiRecipe;
    }

    /**
     * ★ 关键：把"放不下"的配方过滤掉。
     * 背包只有 2x2，任何 3x3 的配方在这里都不应显示为可合成。
     */
    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context) {
        if (recipe instanceof WrappedEmiRecipe wrapped) {
            int rw = Math.max(wrapped.getRecipeWidth(), 1);
            int rh = Math.max(wrapped.getRecipeHeight(), 1);
            if (rw > MAX_W || rh > MAX_H) {
                return false;  // 装不下，不算可合成
            }
        }
        // 尺寸没问题，再检查材料够不够
        return context.getInventory().canCraft(recipe);
    }
}