package com.simibubi.create;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.matrix.MatrixStack.Entry;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.simibubi.create.foundation.utility.ColorHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public enum ScreenResources {

	// Inventories
	PLAYER_INVENTORY("player_inventory.png", 176, 108),
	WAND_SYMMETRY("wand_symmetry.png", 207, 58),
	BLOCKZAPPER("zapper.png", 217, 70),
	TERRAINZAPPER("zapper.png", 0, 70, 217, 105),
	TERRAINZAPPER_INACTIVE_PARAM("zapper.png", 0, 175, 14, 14),

	SCHEMATIC_TABLE("schematic_table.png", 207, 89),
	SCHEMATIC_TABLE_PROGRESS("schematic_table.png", 209, 0, 24, 17),
	SCHEMATIC("schematic.png", 207, 95),

	SCHEMATICANNON_BG("schematicannon.png", 247, 161),
	SCHEMATICANNON_BG_FUEL("schematicannon.png", 247, 161),
	SCHEMATICANNON_PROGRESS("schematicannon.png", 0, 161, 121, 16),
	SCHEMATICANNON_PROGRESS_2("schematicannon.png", 122, 161, 16, 15),
	SCHEMATICANNON_HIGHLIGHT("schematicannon.png", 0, 182, 28, 28),
	SCHEMATICANNON_FUEL("schematicannon.png", 0, 215, 82, 4),
	SCHEMATICANNON_FUEL_CREATIVE("schematicannon.png", 0, 219, 82, 4),

	FLEXCRATE("flex_crate_and_stockpile_switch.png", 125, 129),
	FLEXCRATE_DOUBLE("double_flexcrate.png", 197, 129),
	FLEXCRATE_LOCKED_SLOT("flex_crate_and_stockpile_switch.png", 138, 0, 18, 18),

	STOCKSWITCH("flex_crate_and_stockpile_switch.png", 0, 129, 205, 93),
	STOCKSWITCH_INTERVAL("flex_crate_and_stockpile_switch.png", 0, 222, 198, 17),
	STOCKSWITCH_INTERVAL_END("flex_crate_and_stockpile_switch.png", 0, 239, 198, 17),
	STOCKSWITCH_CURSOR_ON("flex_crate_and_stockpile_switch.png", 218, 129, 8, 21),
	STOCKSWITCH_CURSOR_OFF("flex_crate_and_stockpile_switch.png", 226, 129, 8, 21),
	STOCKSWITCH_BOUND_LEFT("flex_crate_and_stockpile_switch.png", 234, 129, 7, 21),
	STOCKSWITCH_BOUND_RIGHT("flex_crate_and_stockpile_switch.png", 241, 129, 7, 21),

	FILTER("filter.png", 200, 100),
	ATTRIBUTE_FILTER("filter.png", 0, 100, 200, 86),

	SEQUENCER("sequencer.png", 156, 128),
	SEQUENCER_INSTRUCTION("sequencer.png", 14, 47, 131, 18),
	SEQUENCER_WAIT("sequencer.png", 14, 65, 131, 18),
	SEQUENCER_END("sequencer.png", 14, 83, 131, 18),
	SEQUENCER_EMPTY("sequencer.png", 14, 101, 131, 18),

	// JEI
	JEI_SLOT("jei/widgets.png", 18, 18),
	JEI_CHANCE_SLOT("jei/widgets.png", 20, 156, 18, 18),
	JEI_CATALYST_SLOT("jei/widgets.png", 0, 156, 18, 18),
	JEI_ARROW("jei/widgets.png", 19, 10, 42, 10),
	JEI_LONG_ARROW("jei/widgets.png", 19, 0, 71, 10),
	JEI_DOWN_ARROW("jei/widgets.png", 0, 21, 18, 14),
	JEI_LIGHT("jei/widgets.png", 0, 42, 52, 11),
	JEI_QUESTION_MARK("jei/widgets.png", 0, 178, 12, 16),
	JEI_SHADOW("jei/widgets.png", 0, 56, 52, 11),
	BLOCKZAPPER_UPGRADE_RECIPE("jei/widgets.png", 0, 75, 144, 66),

	// Widgets
	PALETTE_BUTTON("palette_picker.png", 0, 236, 20, 20),
	TEXT_INPUT("widgets.png", 0, 28, 194, 47),
	BUTTON("widgets.png", 18, 18),
	BUTTON_HOVER("widgets.png", 18, 0, 18, 18),
	BUTTON_DOWN("widgets.png", 36, 0, 18, 18),
	INDICATOR("widgets.png", 0, 18, 18, 5),
	INDICATOR_WHITE("widgets.png", 18, 18, 18, 5),
	INDICATOR_GREEN("widgets.png", 0, 23, 18, 5),
	INDICATOR_YELLOW("widgets.png", 18, 23, 18, 5),
	INDICATOR_RED("widgets.png", 36, 23, 18, 5),
	GRAY("background.png", 0, 0, 16, 16),

	BLUEPRINT_SLOT("widgets.png", 90, 0, 24, 24),

	// Icons
	I_ADD(0, 0),
	I_TRASH(1, 0),
	I_3x3(2, 0),
	I_TARGET(3, 0),
	I_PRIORITY_VERY_LOW(4, 0),
	I_PRIORITY_LOW(5, 0),
	I_PRIORITY_HIGH(6, 0),
	I_PRIORITY_VERY_HIGH(7, 0),
	I_BLACKLIST(8, 0),
	I_WHITELIST(9, 0),
	I_WHITELIST_OR(10, 0),
	I_WHITELIST_AND(11, 0),
	I_WHITELIST_NOT(12, 0),
	I_RESPECT_NBT(13, 0),
	I_IGNORE_NBT(14, 0),

	I_CONFIRM(0, 1),
	I_NONE(1, 1),
	I_OPEN_FOLDER(2, 1),
	I_REFRESH(3, 1),
	I_ACTIVE(4, 1),
	I_PASSIVE(5, 1),
	I_ROTATE_PLACE(6, 1),
	I_ROTATE_PLACE_RETURNED(7, 1),
	I_ROTATE_NEVER_PLACE(8, 1),
	I_MOVE_PLACE(9, 1),
	I_MOVE_PLACE_RETURNED(10, 1),
	I_MOVE_NEVER_PLACE(11, 1),
	I_CART_ROTATE(12, 1),
	I_CART_ROTATE_PAUSED(13, 1),
	I_CART_ROTATE_LOCKED(14, 1),

	I_DONT_REPLACE(0, 2),
	I_REPLACE_SOLID(1, 2),
	I_REPLACE_ANY(2, 2),
	I_REPLACE_EMPTY(3, 2),
	I_CENTERED(4, 2),
	I_ATTACHED(5, 2),
	I_INSERTED(6, 2),
	I_FILL(7, 2),
	I_PLACE(8, 2),
	I_REPLACE(9, 2),
	I_CLEAR(10, 2),
	I_OVERLAY(11, 2),
	I_FLATTEN(12, 2),

	I_TOOL_DEPLOY(0, 3),
	I_SKIP_TILES(2, 3),
	I_SKIP_MISSING(1, 3),

	I_TOOL_MOVE_XZ(0, 4),
	I_TOOL_MOVE_Y(1, 4),
	I_TOOL_ROTATE(2, 4),
	I_TOOL_MIRROR(3, 4),

	I_PLAY(0, 5),
	I_PAUSE(1, 5),
	I_STOP(2, 5),

	I_PATTERN_SOLID(0, 6),
	I_PATTERN_CHECKERED(1, 6),
	I_PATTERN_CHECKERED_INVERSED(2, 6),
	I_PATTERN_CHANCE_25(3, 6),

	I_PATTERN_CHANCE_50(0, 7),
	I_PATTERN_CHANCE_75(1, 7),
	I_FOLLOW_DIAGONAL(2, 7),
	I_FOLLOW_MATERIAL(3, 7),

	;

	public static final int FONT_COLOR = 0x575F7A;
	public static final ResourceLocation ICON_ATLAS = Create.asResource("textures/gui/icons.png");

	public final ResourceLocation location;
	public int width, height;
	public int startX, startY;

	private ScreenResources(String location, int width, int height) {
		this(location, 0, 0, width, height);
	}

	private ScreenResources(int startX, int startY) {
		this("icons.png", startX * 16, startY * 16, 16, 16);
	}

	private ScreenResources(String location, int startX, int startY, int width, int height) {
		this.location = new ResourceLocation(Create.ID, "textures/gui/" + location);
		this.width = width;
		this.height = height;
		this.startX = startX;
		this.startY = startY;
	}

	@OnlyIn(Dist.CLIENT)
	public void bind() {
		Minecraft.getInstance()
			.getTextureManager()
			.bindTexture(location);
	}

	@OnlyIn(Dist.CLIENT)
	public void draw(AbstractGui screen, int x, int y) {
		bind();
		screen.blit(x, y, startX, startY, width, height);
	}

	@OnlyIn(Dist.CLIENT)
	public void draw(int x, int y) {
		draw(new Screen(null) {
		}, x, y);
	}

	@OnlyIn(Dist.CLIENT)
	public void draw(MatrixStack ms, IRenderTypeBuffer buffer, int color) {
		IVertexBuilder builder = buffer.getBuffer(RenderType.getTextSeeThrough(this.location));
		float sheetSize = 256;
		int i = 15 << 20 | 15 << 4;
		int j = i >> 16 & '\uffff';
		int k = i & '\uffff';
		Entry peek = ms.peek();
		Vec3d rgb = ColorHelper.getRGB(color);

		Vec3d vec4 = new Vec3d(1, 1, 0);
		Vec3d vec3 = new Vec3d(0, 1, 0);
		Vec3d vec2 = new Vec3d(0, 0, 0);
		Vec3d vec1 = new Vec3d(1, 0, 0);

		float u1 = (startX + width) / sheetSize;
		float u2 = startX / sheetSize;
		float v1 = startY / sheetSize;
		float v2 = (startY + height) / sheetSize;

		vertex(peek, builder, j, k, rgb, vec1, u1, v1);
		vertex(peek, builder, j, k, rgb, vec2, u2, v1);
		vertex(peek, builder, j, k, rgb, vec3, u2, v2);
		vertex(peek, builder, j, k, rgb, vec4, u1, v2);
	}

	@OnlyIn(Dist.CLIENT)
	private void vertex(Entry peek, IVertexBuilder builder, int j, int k, Vec3d rgb, Vec3d vec, float u, float v) {
		builder.vertex(peek.getModel(), (float) vec.x, (float) vec.y, (float) vec.z)
			.color((float) rgb.x, (float) rgb.y, (float) rgb.z, 1)
			.texture(u, v)
			.light(j, k)
			.endVertex();
	}

}
