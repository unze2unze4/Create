package com.simibubi.create.content.contraptions.processing;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

public class ProcessingRecipeSerializer<T extends ProcessingRecipe<?>>
		extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<T> {

	protected final IRecipeFactory<T> factory;

	public ProcessingRecipeSerializer(IRecipeFactory<T> factory) {
		this.factory = factory;
	}

	@SuppressWarnings("deprecation")
	public T read(ResourceLocation recipeId, JsonObject json) {
		String s = JSONUtils.getString(json, "group", "");

		List<ProcessingIngredient> ingredients = new ArrayList<>();
		for (JsonElement e : JSONUtils.getJsonArray(json, "ingredients")) {
			int count = 1;
			if (JSONUtils.hasField((JsonObject) e, "count")) {
				count = JSONUtils.getInt(e.getAsJsonObject().get("count"), "count");
			}
			for(int i = 0; i < count; i++) {
				ingredients.add(ProcessingIngredient.parse(e.getAsJsonObject()));
			}
		}

		List<ProcessingOutput> results = new ArrayList<>();
		for (JsonElement e : JSONUtils.getJsonArray(json, "results")) {
			String s1 = JSONUtils.getString(e.getAsJsonObject().get("item"), "item");
			int i = JSONUtils.getInt(e.getAsJsonObject().get("count"), "count");
			float chance = 1;
			if (JSONUtils.hasField((JsonObject) e, "chance"))
				chance = JSONUtils.getFloat(e.getAsJsonObject().get("chance"), "chance");
			ItemStack itemstack = new ItemStack(Registry.ITEM.getOrDefault(new ResourceLocation(s1)), i);
			results.add(new ProcessingOutput(itemstack, chance));
		}

		int duration = -1;
		if (JSONUtils.hasField(json, "processingTime"))
			duration = JSONUtils.getInt(json, "processingTime");

		return this.factory.create(recipeId, s, ingredients, results, duration);
	}

	public T read(ResourceLocation recipeId, PacketBuffer buffer) {
		String s = buffer.readString(32767);

		List<ProcessingIngredient> ingredients = new ArrayList<>();
		int ingredientCount = buffer.readInt();
		for (int i = 0; i < ingredientCount; i++)
			ingredients.add(ProcessingIngredient.parse(buffer));

		List<ProcessingOutput> results = new ArrayList<>();
		int outputCount = buffer.readInt();
		for (int i = 0; i < outputCount; i++)
			results.add(ProcessingOutput.read(buffer));

		int duration = buffer.readInt();

		return this.factory.create(recipeId, s, ingredients, results, duration);
	}

	public void write(PacketBuffer buffer, T recipe) {
		buffer.writeString(recipe.group);

		buffer.writeInt(recipe.ingredients.size());
		recipe.ingredients.forEach(i -> i.write(buffer));

		buffer.writeInt(recipe.getRollableResults().size());
		recipe.getRollableResults().forEach(i -> i.write(buffer));

		buffer.writeInt(recipe.processingDuration);
	}

	public interface IRecipeFactory<T extends ProcessingRecipe<?>> {
		T create(ResourceLocation id, String group, List<ProcessingIngredient> ingredients, List<ProcessingOutput> results,
				int duration);
	}

}
