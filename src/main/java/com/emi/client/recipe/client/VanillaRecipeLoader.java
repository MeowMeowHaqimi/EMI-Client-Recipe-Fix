package com.emi.client.recipe.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static com.emi.client.recipe.client.ClientRecipeFix.LOGGER;

public class VanillaRecipeLoader {
    public static List<RecipeHolder<?>> loadVanillaRecipes(HolderLookup.Provider registries) {
        List<RecipeHolder<?>> recipes = new ArrayList<>();

        try {
            URI jarUri = RecipeManager.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path jarPath = Path.of(jarUri);

            LOGGER.info("Loading recipes from: {}", jarPath);

            if (Files.isDirectory(jarPath)) {
                Path recipeDir = jarPath.resolve("data/minecraft/recipe");
                if (Files.isDirectory(recipeDir)) {
                    loadFromDirectory(recipeDir, registries, recipes);
                } else {
                    LOGGER.warn("Recipe dir not found at {}", recipeDir);
                }
            } else {
                try (FileSystem fs = FileSystems.newFileSystem(jarPath)) {
                    Path recipeDir = fs.getPath("data/minecraft/recipe");
                    if (Files.isDirectory(recipeDir)) {
                        loadFromDirectory(recipeDir, registries, recipes);
                    } else {
                        LOGGER.warn("Recipe dir not found in JAR");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to locate recipe JAR", e);
        }

        LOGGER.info("Loaded {} vanilla recipes", recipes.size());
        return recipes;
    }

    private static void loadFromDirectory(Path recipeDir, HolderLookup.Provider registries,
                                          List<RecipeHolder<?>> recipes) {
        try (Stream<Path> paths = Files.walk(recipeDir)) {
            List<Path> jsonFiles = paths.filter(p -> p.toString().endsWith(".json")).toList();
            LOGGER.info("Found {} recipe files", jsonFiles.size());

            var ops = registries.createSerializationContext(JsonOps.INSTANCE);

            // 加载标签以展开配方中的 #tag
            Path tagDir = recipeDir.getParent().resolve("tags/item");
            Map<String, List<String>> itemTags = loadItemTags(tagDir);
            LOGGER.info("Loaded {} item tags", itemTags.size());

            for (Path jsonFile : jsonFiles) {
                String relativePath = recipeDir.relativize(jsonFile).toString();
                String recipeIdPath = relativePath.substring(0, relativePath.length() - 5)
                        .replace('\\', '/');
                Identifier recipeId = Identifier.fromNamespaceAndPath("minecraft", recipeIdPath);

                try {
                    ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipeId);
                    try (InputStream is = Files.newInputStream(jsonFile);
                         InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                        // 展开标签引用，并强制转换为纯字符串数组
                        expandTagReferences(json, itemTags);

                        Recipe<?> recipe = Recipe.CODEC.parse(ops, json).getOrThrow();
                        RecipeHolder<?> holder = new RecipeHolder<>(key, recipe);
                        recipes.add(holder);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse recipe {}: {}", recipeId, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to walk recipe directory", e);
        }
    }

    private static Map<String, List<String>> loadItemTags(Path tagDir) {
        Map<String, List<String>> rawTags = new HashMap<>();

        if (!Files.isDirectory(tagDir)) {
            LOGGER.warn("Item tag dir not found at {}", tagDir);
            return Collections.emptyMap();
        }

        try (Stream<Path> paths = Files.walk(tagDir)) {
            List<Path> tagFiles = paths.filter(p -> p.toString().endsWith(".json")).toList();

            for (Path tagFile : tagFiles) {
                String relativePath = tagDir.relativize(tagFile).toString();
                String tagName = "minecraft:" + relativePath
                        .substring(0, relativePath.length() - 5)
                        .replace('\\', '/');

                try (InputStream is = Files.newInputStream(tagFile);
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray values = json.getAsJsonArray("values");
                    List<String> entries = getStrings(values);
                    rawTags.put(tagName, entries);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load item tag {}: {}", tagName, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to walk item tag directory", e);
        }

        Map<String, List<String>> resolved = new HashMap<>();
        for (String tagName : rawTags.keySet()) {
            resolveTag(tagName, rawTags, resolved, new HashSet<>());
        }
        return resolved;
    }

    private static List<String> getStrings(JsonArray values) {
        List<String> entries = new ArrayList<>();
        if (values == null) return entries;
        for (JsonElement entry : values) {
            if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                entries.add(entry.getAsString());
            } else if (entry.isJsonObject()) {
                JsonObject entryObj = entry.getAsJsonObject();
                if (entryObj.has("id")) {
                    entries.add(entryObj.get("id").getAsString());
                }
            }
        }
        return entries;
    }

    private static List<String> resolveTag(String tagName,
                                           Map<String, List<String>> rawTags,
                                           Map<String, List<String>> resolvedCache,
                                           Set<String> resolving) {
        if (resolvedCache.containsKey(tagName)) return resolvedCache.get(tagName);
        if (resolving.contains(tagName)) {
            LOGGER.warn("Circular tag reference detected for {}", tagName);
            return Collections.emptyList();
        }

        resolving.add(tagName);
        List<String> raw = rawTags.get(tagName);
        if (raw == null) {
            resolving.remove(tagName);
            return Collections.emptyList();
        }

        List<String> resolved = new ArrayList<>();
        for (String entry : raw) {
            if (entry.startsWith("#")) {
                resolved.addAll(resolveTag(entry.substring(1), rawTags, resolvedCache, resolving));
            } else {
                resolved.add(entry);
            }
        }

        resolving.remove(tagName);
        resolvedCache.put(tagName, resolved);
        return resolved;
    }

    private static void expandTagReferences(JsonObject obj, Map<String, List<String>> itemTags) {
        for (String key : new ArrayList<>(obj.keySet())) {
            JsonElement value = obj.get(key);
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                String str = value.getAsString();
                if (str.startsWith("#") && str.contains(":")) {
                    List<String> items = itemTags.get(str.substring(1));
                    if (items != null && !items.isEmpty()) {
                        replaceJsonValue(obj, key, items);
                    }
                }
            } else if (value.isJsonArray()) {
                JsonArray array = value.getAsJsonArray();
                List<String> flatItems = new ArrayList<>();
                boolean valid = true;
                for (JsonElement el : array) {
                    if (el.isJsonObject()) {
                        JsonObject elObj = el.getAsJsonObject();
                        if (elObj.has("item")) {
                            String itemName = elObj.get("item").getAsString();
                            if (itemName.startsWith("#")) {
                                List<String> items = itemTags.get(itemName.substring(1));
                                if (items != null) flatItems.addAll(items);
                                else valid = false;
                            } else {
                                flatItems.add(itemName);
                            }
                        } else if (elObj.has("tag")) {
                            String tagName = elObj.get("tag").getAsString();
                            List<String> items = itemTags.get(tagName);
                            if (items != null) flatItems.addAll(items);
                            else valid = false;
                        } else {
                            valid = false;
                        }
                    } else if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                        String itemName = el.getAsString();
                        if (itemName.startsWith("#")) {
                            List<String> items = itemTags.get(itemName.substring(1));
                            if (items != null) flatItems.addAll(items);
                            else valid = false;
                        } else {
                            flatItems.add(itemName);
                        }
                    } else {
                        valid = false;
                    }
                }
                if (valid && !flatItems.isEmpty()) {
                    replaceJsonValue(obj, key, flatItems);
                } else {
                    expandTagReferencesInArray(array, itemTags);
                }
            } else if (value.isJsonObject()) {
                JsonObject subObj = value.getAsJsonObject();
                if (subObj.has("tag")) {
                    String tagName = subObj.get("tag").getAsString();
                    List<String> items = itemTags.get(tagName);
                    if (items != null && !items.isEmpty()) {
                        replaceJsonValue(obj, key, items);
                    }
                } else if (subObj.has("item")) {
                    String itemStr = subObj.get("item").getAsString();
                    if (itemStr.startsWith("#")) {
                        List<String> items = itemTags.get(itemStr.substring(1));
                        if (items != null && !items.isEmpty()) {
                            replaceJsonValue(obj, key, items);
                        }
                    }
                } else {
                    expandTagReferences(subObj, itemTags);
                }
            }
        }
    }

    /**
     * ★ 核心修复：直接生成纯字符串数组，格式为 ["minecraft:xxx", ...]
     * 完全符合 Minecraft 26.2 的 Ingredient 编码要求。
     */
    private static void replaceJsonValue(JsonObject obj, String key, List<String> items) {
        JsonArray array = new JsonArray();
        for (String item : items) {
            array.add(item);
        }
        obj.add(key, array);
    }

    private static void expandTagReferencesInArray(JsonArray array, Map<String, List<String>> itemTags) {
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String str = element.getAsString();
                if (str.startsWith("#") && str.contains(":")) {
                    List<String> items = itemTags.get(str.substring(1));
                    if (items != null && !items.isEmpty()) {
                        JsonArray replacement = new JsonArray();
                        for (String item : items) {
                            replacement.add(item);
                        }
                        array.set(i, replacement);
                    }
                }
            } else if (element.isJsonObject()) {
                JsonObject subObj = element.getAsJsonObject();
                if (subObj.has("tag")) {
                    String tagName = subObj.get("tag").getAsString();
                    List<String> items = itemTags.get(tagName);
                    if (items != null && !items.isEmpty()) {
                        JsonArray replacement = new JsonArray();
                        for (String item : items) {
                            replacement.add(item);
                        }
                        array.set(i, replacement);
                    }
                } else {
                    expandTagReferences(subObj, itemTags);
                }
            } else if (element.isJsonArray()) {
                expandTagReferencesInArray(element.getAsJsonArray(), itemTags);
            }
        }
    }
}