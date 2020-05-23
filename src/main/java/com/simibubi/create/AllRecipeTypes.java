package com.simibubi.create;

import java.util.function.Supplier;

import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.content.contraptions.components.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.contraptions.components.crusher.CrushingRecipe;
import com.simibubi.create.content.contraptions.components.fan.SplashingRecipe;
import com.simibubi.create.content.contraptions.components.millstone.MillingRecipe;
import com.simibubi.create.content.contraptions.components.mixer.MixingRecipe;
import com.simibubi.create.content.contraptions.components.press.PressingRecipe;
import com.simibubi.create.content.contraptions.components.saw.CuttingRecipe;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipe;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeSerializer;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeSerializer.IRecipeFactory;
import com.simibubi.create.content.curiosities.tools.SandPaperPolishingRecipe;
import com.simibubi.create.content.curiosities.zapper.blockzapper.BlockzapperUpgradeRecipe;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.event.RegistryEvent;

public enum AllRecipeTypes {

	BLOCKZAPPER_UPGRADE(BlockzapperUpgradeRecipe.Serializer::new, IRecipeType.CRAFTING),
	MECHANICAL_CRAFTING(MechanicalCraftingRecipe.Serializer::new),
	CRUSHING(processingSerializer(CrushingRecipe::new)),
	MILLING(processingSerializer(MillingRecipe::new)),
	SPLASHING(processingSerializer(SplashingRecipe::new)),
	PRESSING(processingSerializer(PressingRecipe::new)),
	CUTTING(processingSerializer(CuttingRecipe::new)),
	MIXING(processingSerializer(MixingRecipe::new)),
	SANDPAPER_POLISHING(processingSerializer(SandPaperPolishingRecipe::new)),
	CONVERSION(processingSerializer(ConversionRecipe::new)),

	;

	public IRecipeSerializer<?> serializer;
	public Supplier<IRecipeSerializer<?>> supplier;
	public IRecipeType<? extends IRecipe<? extends IInventory>> type;

	@SuppressWarnings("unchecked")
	public <T extends IRecipeType<?>> T getType() {
		return (T) type;
	}
	
	private AllRecipeTypes(Supplier<IRecipeSerializer<?>> supplier) {
		this(supplier, null);
	}

	private AllRecipeTypes(Supplier<IRecipeSerializer<?>> supplier,
			IRecipeType<? extends IRecipe<? extends IInventory>> existingType) {
		this.supplier = supplier;
		this.type = existingType;
	}

	public static void register(RegistryEvent.Register<IRecipeSerializer<?>> event) {
		ShapedRecipe.setCraftingSize(9, 9);
		
		for (AllRecipeTypes r : AllRecipeTypes.values()) {
			if (r.type == null)
				r.type = customType(Lang.asId(r.name()));
			
			r.serializer = r.supplier.get();
			ResourceLocation location = new ResourceLocation(Create.ID, Lang.asId(r.name()));
			event.getRegistry().register(r.serializer.setRegistryName(location));
		}
	}
	
	private static <T extends IRecipe<?>> IRecipeType<T> customType(String id) {
		return Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(Create.ID, id), new IRecipeType<T>() {
			public String toString() {
				return Create.ID + ":" + id;
			}
		});
	}
	
	private static Supplier<IRecipeSerializer<?>> processingSerializer(IRecipeFactory<? extends ProcessingRecipe<?>> factory) {
		return () -> new ProcessingRecipeSerializer<>(factory);
	}


}
