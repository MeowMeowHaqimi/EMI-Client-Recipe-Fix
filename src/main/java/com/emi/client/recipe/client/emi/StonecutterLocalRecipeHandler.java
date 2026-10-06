package com.emi.client.recipe.client.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StonecutterLocalRecipeHandler implements StandardRecipeHandler<StonecutterMenu> {

    @Override
    public List<Slot> getInputSources(StonecutterMenu menu) {
        // 玩家背包槽位从索引 3 开始
        List<Slot> slots = new ArrayList<>();
        for (int i = 3; i < menu.slots.size(); i++) {
            slots.add(menu.slots.get(i));
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(StonecutterMenu menu) {
        // 切石机输入槽索引为 0
        List<Slot> slots = new ArrayList<>();
        slots.add(menu.slots.get(0));
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(EmiRecipe recipe, StonecutterMenu menu) {
        return getCraftingSlots(menu);
    }

    @Override
    public @Nullable Slot getOutputSlot(StonecutterMenu menu) {
        // 输出槽索引为 1
        return menu.slots.get(1);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        // 只处理你的 WrappedEmiRecipe 中布局为 STONECUTTING 的配方
        return recipe instanceof WrappedEmiRecipe wrapped && wrapped.getLayout() == WrappedEmiRecipe.Layout.STONECUTTING;
    }
}