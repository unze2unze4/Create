package com.simibubi.create;

import static com.simibubi.create.foundation.block.connected.CTSpriteShifter.getCT;
import static com.simibubi.create.foundation.block.connected.CTSpriteShifter.CTType.HORIZONTAL;
import static com.simibubi.create.foundation.block.connected.CTSpriteShifter.CTType.OMNIDIRECTIONAL;
import static com.simibubi.create.foundation.block.connected.CTSpriteShifter.CTType.VERTICAL;

import java.util.IdentityHashMap;
import java.util.Map;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter.CTType;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import com.simibubi.create.foundation.block.render.SpriteShifter;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.modules.palettes.PaletteBlockPatterns;
import com.simibubi.create.modules.palettes.PaletteBlockPatterns.CTs;
import com.simibubi.create.modules.palettes.PaletteStoneVariants;

import net.minecraft.block.WoodType;

public class AllSpriteShifts {

	static final Map<WoodType, CTSpriteShiftEntry> 
		WOODEN_WINDOWS = new IdentityHashMap<>();
	static final Map<PaletteStoneVariants, Map<PaletteBlockPatterns.CTs, CTSpriteShiftEntry>> 
		PALETTE_VARIANT_PATTERNS = new IdentityHashMap<>();

	public static final CTSpriteShiftEntry 
		FRAMED_GLASS = getCT(OMNIDIRECTIONAL, "palettes/framed_glass", "palettes/framed_glass"),
		HORIZONTAL_FRAMED_GLASS = getCT(HORIZONTAL, "palettes/framed_glass", "palettes/horizontal_framed_glass"),
		VERTICAL_FRAMED_GLASS = getCT(VERTICAL, "palettes/framed_glass", "palettes/vertical_framed_glass"),
		ORNATE_IRON_WINDOW = vertical("palettes/ornate_iron_window"),
		ORNATE_GOLD_WINDOW = vertical("palettes/ornate_gold_window");

	public static final CTSpriteShiftEntry 
		CRAFTER_FRONT = getCT(CTType.OMNIDIRECTIONAL, "crafter_top", "brass_casing"),
		CRAFTER_SIDE = getCT(CTType.VERTICAL, "crafter_side"),
		CRAFTER_OTHERSIDE = getCT(CTType.HORIZONTAL, "crafter_side");

	public static final CTSpriteShiftEntry 
		CHASSIS = getCT(CTType.OMNIDIRECTIONAL, "translation_chassis_top"),
		CHASSIS_STICKY = getCT(CTType.OMNIDIRECTIONAL, "translation_chassis_top_sticky");

	public static final SpriteShiftEntry 
		BELT = SpriteShifter.get("block/belt", "block/belt_animated"),
		CRAFTER_THINGIES = SpriteShifter.get("block/crafter_thingies", "block/crafter_thingies");

	static {
		populateMaps();
	}

	//

	public static CTSpriteShiftEntry getWoodenWindow(WoodType woodType) {
		return WOODEN_WINDOWS.get(woodType);
	}

	public static CTSpriteShiftEntry getVariantPattern(PaletteStoneVariants variant, PaletteBlockPatterns.CTs texture) {
		return PALETTE_VARIANT_PATTERNS.get(variant)
			.get(texture);
	}

	//

	private static void populateMaps() {
		WoodType.stream()
			.forEach(woodType -> WOODEN_WINDOWS.put(woodType, vertical("palettes/" + woodType.getName() + "_window")));

		for (PaletteStoneVariants paletteStoneVariants : PaletteStoneVariants.values()) {
			String variantName = Lang.asId(paletteStoneVariants.name());
			IdentityHashMap<CTs, CTSpriteShiftEntry> map = new IdentityHashMap<>();
			PALETTE_VARIANT_PATTERNS.put(paletteStoneVariants, map);

			for (PaletteBlockPatterns.CTs texture : PaletteBlockPatterns.CTs.values()) {
				String textureName = Lang.asId(texture.name());
				String target = "palettes/" + variantName + "/" + textureName;
				map.put(texture, getCT(texture.type, target));
			}
		}
	}

	//

	static CTSpriteShiftEntry omni(String name) {
		return getCT(OMNIDIRECTIONAL, name);
	}

	static CTSpriteShiftEntry vertical(String name) {
		return getCT(VERTICAL, name);
	}

	static CTSpriteShiftEntry horizontal(String name) {
		return getCT(HORIZONTAL, name);
	}

}
