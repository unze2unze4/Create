package com.simibubi.create.modules.contraptions.relays.advanced.sequencer;

import java.util.Vector;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllBlocksNew;
import com.simibubi.create.AllPackets;
import com.simibubi.create.ScreenResources;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.GuiGameElement;
import com.simibubi.create.foundation.gui.widgets.ScrollInput;
import com.simibubi.create.foundation.gui.widgets.SelectionScrollInput;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;

public class SequencedGearshiftScreen extends AbstractSimiScreen {

	private final ItemStack renderedItem = new ItemStack(AllBlocksNew.SEQUENCED_GEARSHIFT.get());
	private final ScreenResources background = ScreenResources.SEQUENCER;

	private final String title = Lang.translate("gui.sequenced_gearshift.title");
	private ListNBT compareTag;
	private Vector<Instruction> instructions;
	private BlockPos pos;

	private Vector<Vector<ScrollInput>> inputs;

	public SequencedGearshiftScreen(SequencedGearshiftTileEntity te) {
		this.instructions = te.instructions;
		this.pos = te.getPos();
		compareTag = Instruction.serializeAll(instructions);
	}

	@Override
	protected void init() {
		setWindowSize(background.width + 50, background.height);
		super.init();
		widgets.clear();

		inputs = new Vector<>(5);
		for (int row = 0; row < inputs.capacity(); row++)
			inputs.add(new Vector<>(3));

		for (int row = 0; row < instructions.size(); row++)
			initInputsOfRow(row);
	}

	public void initInputsOfRow(int row) {
		int x = guiLeft + 28;
		int y = guiTop + 29;
		int rowHeight = 18;

		Vector<ScrollInput> rowInputs = inputs.get(row);
		rowInputs.forEach(widgets::remove);
		rowInputs.clear();
		int index = row;
		Instruction instruction = instructions.get(row);

		ScrollInput type =
			new SelectionScrollInput(x, y + rowHeight * row, 50, 14).forOptions(SequencerInstructions.getOptions())
					.calling(state -> instructionUpdated(index, state))
					.setState(instruction.instruction.ordinal())
					.titled(Lang.translate("gui.sequenced_gearshift.instruction"));
		ScrollInput value =
			new ScrollInput(x + 54, y + rowHeight * row, 30, 14).calling(state -> instruction.value = state);
		ScrollInput direction = new SelectionScrollInput(x + 88, y + rowHeight * row, 18, 14)
				.forOptions(InstructionSpeedModifiers.getOptions())
				.calling(state -> instruction.speedModifier = InstructionSpeedModifiers.values()[state])
				.titled(Lang.translate("gui.sequenced_gearshift.speed"));

		rowInputs.add(type);
		rowInputs.add(value);
		rowInputs.add(direction);

		widgets.addAll(rowInputs);
		updateParamsOfRow(row);
	}

	public void updateParamsOfRow(int row) {
		Instruction instruction = instructions.get(row);
		Vector<ScrollInput> rowInputs = inputs.get(row);
		SequencerInstructions def = instruction.instruction;
		boolean hasValue = def.hasValueParameter;
		boolean hasModifier = def.hasSpeedParameter;

		ScrollInput value = rowInputs.get(1);
		value.active = value.visible = hasValue;
		if (hasValue)
			value.withRange(1, def.maxValue + 1)
					.titled(Lang.translate(def.parameterKey))
					.withShiftStep(def.shiftStep)
					.setState(instruction.value)
					.onChanged();
		if (def == SequencerInstructions.WAIT) {
			value.withStepFunction(context -> {
				int v = context.currentValue;
				if (!context.forward)
					v--;
				if (v < 20)
					return context.shift ? 20 : 1;
				return context.shift ? 100 : 20;
			});
		} else
			value.withStepFunction(value.standardStep());

		ScrollInput modifier = rowInputs.get(2);
		modifier.active = modifier.visible = hasModifier;
		if (hasModifier)
			modifier.setState(instruction.speedModifier.ordinal());
	}

	@Override
	protected void renderWindow(int mouseX, int mouseY, float partialTicks) {
		int hFontColor = 0xD3CBBE;
		background.draw(this, guiLeft, guiTop);

		for (int row = 0; row < instructions.capacity(); row++) {
			ScreenResources toDraw = ScreenResources.SEQUENCER_EMPTY;
			int yOffset = toDraw.height * row;

			if (row < instructions.size()) {
				Instruction instruction = instructions.get(row);
				SequencerInstructions def = instruction.instruction;
				def.background.draw(guiLeft + 14, guiTop + 29 + yOffset);

				label(32, 6 + yOffset, Lang.translate(def.translationKey));
				if (def.hasValueParameter) {
					String text = def.formatValue(instruction.value);
					int stringWidth = font.getStringWidth(text);
					label(85 + (12 - stringWidth / 2), 6 + yOffset, text);
				}
				if (def.hasSpeedParameter)
					label(120, 6 + yOffset, instruction.speedModifier.label);

				continue;
			}

			toDraw.draw(guiLeft + 14, guiTop + 29 + yOffset);
		}

		font.drawStringWithShadow(title, guiLeft - 3 + (background.width - font.getStringWidth(title)) / 2, guiTop + 10,
				hFontColor);

		RenderSystem.pushMatrix();
		RenderSystem.translated(guiLeft + background.width + 20, guiTop + 50, 0);
		GuiGameElement.of(renderedItem)
				.scale(5)
				.render();
		RenderSystem.popMatrix();
	}

	private void label(int x, int y, String text) {
		font.drawStringWithShadow(text, guiLeft + x, guiTop + 26 + y, 0xFFFFEE);
	}

	public void sendPacket() {
		ListNBT serialized = Instruction.serializeAll(instructions);
		if (serialized.equals(compareTag))
			return;
		AllPackets.channel.sendToServer(new ConfigureSequencedGearshiftPacket(pos, serialized));
	}

	@Override
	public void removed() {
		sendPacket();
	}

	private void instructionUpdated(int index, int state) {
		SequencerInstructions newValue = SequencerInstructions.values()[state];
		instructions.get(index).instruction = newValue;
		instructions.get(index).value = newValue.defaultValue;
		updateParamsOfRow(index);
		if (newValue == SequencerInstructions.END) {
			for (int i = instructions.size() - 1; i > index; i--) {
				instructions.remove(i);
				Vector<ScrollInput> rowInputs = inputs.get(i);
				rowInputs.forEach(widgets::remove);
				rowInputs.clear();
			}
		} else {
			if (index + 1 < instructions.capacity() && index + 1 == instructions.size()) {
				instructions.add(new Instruction(SequencerInstructions.END));
				initInputsOfRow(index + 1);
			}
		}
	}

}
