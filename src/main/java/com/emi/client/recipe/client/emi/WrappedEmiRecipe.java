package com.emi.client.recipe.client.emi;

import com.emi.client.recipe.client.ClientRecipeFix;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 已废弃的包装类。目前配方渲染已完全委托给 EMI 官方的 EmiShapedRecipe、
 * EmiShapelessRecipe、EmiCookingRecipe、EmiStonecuttingRecipe 处理。
 * 这里保留它仅仅是为了兼容 LocalRecipeHandler 等处理器中的引用。
 * 后续可安全地移除所有相关处理器。
 */
public class WrappedEmiRecipe implements EmiRecipe {

    public enum Layout { SHAPED, SHAPELESS, SMELTING, SMITHING, STONECUTTING, BREWING, DEFAULT }

    private static final int SLOT = 18;

    private final Identifier id;
    private final EmiRecipeCategory category;
    private final List<EmiIngredient> inputs = new ArrayList<>();
    private final List<EmiStack> outputs = new ArrayList<>();
    private final RecipeHolder<?> backingRecipe;
    private final Layout layout;
    private int recipeWidth = 3;
    private int recipeHeight = 3;

    public WrappedEmiRecipe(RecipeHolder<?> holder, EmiRecipeCategory category) {
        this.id = holder.id().identifier();
        this.category = category;
        this.backingRecipe = holder;

        Recipe<?> recipe = holder.value();
        String cn = recipe.getClass().getSimpleName();

        if (cn.contains("Shaped")) this.layout = Layout.SHAPED;
        else if (cn.contains("Shapeless")) this.layout = Layout.SHAPELESS;
        else if (cn.contains("Smelting") || cn.contains("Blasting") || cn.contains("Smoking") || cn.contains("CampfireCooking")) this.layout = Layout.SMELTING;
        else if (cn.contains("Smithing")) this.layout = Layout.SMITHING;
        else if (cn.contains("Stonecutting")) this.layout = Layout.STONECUTTING;
        else this.layout = Layout.DEFAULT;

        // 尝试通过反射从 display() 解析输入输出（失败则忽略，不影响显示）
        try {
            ContextKeySet keySet = new ContextKeySet.Builder().build();
            ContextMap contextMap = new ContextMap.Builder().create(keySet);

            List<?> displays = recipe.display();
            if (displays != null && !displays.isEmpty()) {
                for (Object display : displays) {
                    // 输入
                    Method ingredientsM = tryMethod(display, "ingredients");
                    if (ingredientsM != null) {
                        List<?> slots = (List<?>) ingredientsM.invoke(display);
                        for (Object slot : slots) inputs.add(resolveSlot(slot, contextMap));
                    } else {
                        Method inputM = tryMethod(display, "input");
                        if (inputM != null) inputs.add(resolveSlot(inputM.invoke(display), contextMap));
                    }

                    // 输出
                    Method resultM = tryMethod(display, "result");
                    if (resultM != null) {
                        Object resultSlot = resultM.invoke(display);
                        Method resolveM = resultSlot.getClass().getMethod("resolveForFirstStack", ContextMap.class);
                        Object stack = resolveM.invoke(resultSlot, contextMap);
                        if (stack instanceof ItemStack is && !is.isEmpty()) {
                            outputs.add(EmiStack.of(is));
                        }
                    }
                }
            }
        } catch (Throwable t) {
            ClientRecipeFix.LOGGER.debug("WrappedEmiRecipe 解析失败（不影响 EMI 原生渲染）：{}", t.toString());
        }
    }

    private static Method tryMethod(Object obj, String name) {
        try { return obj.getClass().getMethod(name); } catch (Throwable t) { return null; }
    }

    private EmiIngredient resolveSlot(Object slot, ContextMap contextMap) {
        if (slot == null) return null;
        try {
            Method resolveM = slot.getClass().getMethod("resolveForStacks", ContextMap.class);
            Object stacks = resolveM.invoke(slot, contextMap);
            if (stacks instanceof List<?> list) {
                List<EmiStack> emiStacks = new ArrayList<>();
                for (Object s : list) {
                    if (s instanceof ItemStack is && !is.isEmpty()) emiStacks.add(EmiStack.of(is));
                }
                return emiStacks.isEmpty() ? null : EmiIngredient.of(emiStacks);
            }
        } catch (Throwable t) {
            // 忽略
        }
        return null;
    }

    // === 供处理器使用的 getter 方法 ===
    public Layout getLayout() { return layout; }
    public List<EmiIngredient> getRawInputs() { return inputs; }
    public int getRecipeWidth() { return recipeWidth; }
    public int getRecipeHeight() { return recipeHeight; }

    // === EmiRecipe 接口实现 ===
    @Override public EmiRecipeCategory getCategory() { return category; }
    @Override public @Nullable Identifier getId() { return id; }

    @Override
    public List<EmiIngredient> getInputs() {
        List<EmiIngredient> nn = new ArrayList<>();
        for (EmiIngredient i : inputs) if (i != null) nn.add(i);
        return nn;
    }

    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public RecipeHolder<?> getBackingRecipe() { return backingRecipe; }

    @Override public int getDisplayWidth() {
        if (layout == Layout.SHAPED || layout == Layout.SHAPELESS) return 3 * SLOT + 4 + 24 + 4 + 20 + 4;
        return 90;
    }

    @Override public int getDisplayHeight() {
        if (layout == Layout.SHAPED || layout == Layout.SHAPELESS) return 3 * SLOT + 4;
        return 40;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        if (layout == Layout.SHAPED || layout == Layout.SHAPELESS) {
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 3; c++)
                    widgets.addSlot(EmiStack.EMPTY, c * SLOT, r * SLOT);
            widgets.addFillingArrow(3 * SLOT + 4, 22, 1000);
            int x = 3 * SLOT + 4 + 24 + 4;
            for (EmiStack out : outputs) {
                widgets.addSlot(out, x, 22).recipeContext(this);
                x += 20;
            }
        } else {
            widgets.addSlot(inputs.isEmpty() || inputs.get(0) == null ? EmiStack.EMPTY : inputs.get(0), 0, 11);
            widgets.addFillingArrow(22, 11, 1000);
            if (!outputs.isEmpty()) widgets.addSlot(outputs.get(0), 50, 11).recipeContext(this);
        }
    }
}