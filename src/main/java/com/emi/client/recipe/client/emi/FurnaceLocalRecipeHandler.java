package com.emi.client.recipe.client.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.recipe.EmiCookingRecipe;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FurnaceLocalRecipeHandler implements StandardRecipeHandler<AbstractFurnaceMenu> {

    @Override
    public List<Slot> getInputSources(AbstractFurnaceMenu menu) {
        // 玩家背包槽位从索引 3 开始 (0:输入, 1:燃料, 2:输出)
        List<Slot> slots = new ArrayList<>();
        for (int i = 3; i < menu.slots.size(); i++) {
            slots.add(menu.slots.get(i));
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(AbstractFurnaceMenu menu) {
        // 炉子的输入槽索引为 0
        List<Slot> slots = new ArrayList<>();
        slots.add(menu.slots.get(0));
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(EmiRecipe recipe, AbstractFurnaceMenu menu) {
        return getCraftingSlots(menu);
    }

    @Override
    public @Nullable Slot getOutputSlot(AbstractFurnaceMenu menu) {
        // 输出槽索引为 2
        return menu.slots.get(2);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        // ★ 核心修复：改为判断 EMI 官方的 EmiCookingRecipe
        return recipe instanceof EmiCookingRecipe;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<AbstractFurnaceMenu> context) {
        // 让 EMI 自己判断原料和燃料是否足够
        return context.getInventory().canCraft(recipe);
    }
}