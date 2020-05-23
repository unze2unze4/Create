package com.simibubi.create.foundation.config;

public class CCuriosities extends ConfigBase {

	public ConfigInt maxSymmetryWandRange = i(50, 10, "maxSymmetryWandRange", Comments.symmetryRange);
	public ConfigInt zapperUndoLogLength = i(10, 0, "zapperUndoLogLength", Comments.zapperUndoLogLength);
	public ConfigInt lightSourceCountForRefinedRadiance = i(10, 1, "lightSourceCountForRefinedRadiance",
			Comments.refinedRadiance);
	public ConfigBool allowGlassPanesInPartialBlocks = b(true, "allowGlassPanesInPartialBlocks",
			Comments.windowsInBlocks);
	public ConfigBool enableRefinedRadianceRecipe = b(true, "enableRefinedRadianceRecipe",
			Comments.refinedRadianceRecipe);
	public ConfigBool enableShadowSteelRecipe = b(true, "enableShadowSteelRecipe", Comments.shadowSteelRecipe);
	public ConfigFloat cocoaLogGrowthSpeed = f(20, 0, 100, "cocoaLogGrowthSpeed", Comments.cocoa);

	@Override
	public String getName() {
		return "curiosities";
	}

	private static class Comments {
		static String symmetryRange = "The Maximum Distance to an active mirror for the symmetry wand to trigger.";
		static String refinedRadiance = "The amount of Light sources destroyed before Chromatic Compound turns into Refined Radiance.";
		static String refinedRadianceRecipe = "Allow the standard Refined Radiance recipes.";
		static String shadowSteelRecipe = "Allow the standard Shadow Steel recipe.";
		static String windowsInBlocks = "Allow Glass Panes to be put inside Blocks like Stairs, Slabs, Fences etc.";
		static String cocoa = "% of random Ticks causing a Cocoa log to grow.";
		static String zapperUndoLogLength = "The maximum amount of operations, a blockzapper can remember for undoing. (0 to disable undo)";
	}

}
