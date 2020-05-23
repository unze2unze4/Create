package com.simibubi.create.content.contraptions.relays.advanced.sequencer;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.Lang;

public enum SequencerInstructions {

	TURN_ANGLE("angle", AllGuiTextures.SEQUENCER_INSTRUCTION, true, true, 360, 45, 90),
	TURN_DISTANCE("distance", AllGuiTextures.SEQUENCER_INSTRUCTION, true, true, 50, 5, 5),
	WAIT("duration", AllGuiTextures.SEQUENCER_WAIT, true, false, 600, 20, 10),
	END("", AllGuiTextures.SEQUENCER_END),

	;

	String translationKey;
	String parameterKey;
	boolean hasValueParameter;
	boolean hasSpeedParameter;
	AllGuiTextures background;
	int maxValue;
	int shiftStep;
	int defaultValue;

	private SequencerInstructions(String parameterName, AllGuiTextures background) {
		this(parameterName, background, false, false, -1, -1, -1);
	}

	private SequencerInstructions(String parameterName, AllGuiTextures background, boolean hasValueParameter,
			boolean hasSpeedParameter, int maxValue, int shiftStep, int defaultValue) {
		this.hasValueParameter = hasValueParameter;
		this.hasSpeedParameter = hasSpeedParameter;
		this.background = background;
		this.maxValue = maxValue;
		this.shiftStep = shiftStep;
		this.defaultValue = defaultValue;
		translationKey = "gui.sequenced_gearshift.instruction." + Lang.asId(name());
		parameterKey = translationKey + "." + parameterName;
	}

	static List<String> getOptions() {
		List<String> options = new ArrayList<>();
		for (SequencerInstructions entry : values())
			options.add(Lang.translate(entry.translationKey));
		return options;
	}

	String formatValue(int value) {
		if (this == TURN_ANGLE)
			return value + Lang.translate("generic.unit.degrees");
		if (this == TURN_DISTANCE)
			return value + "m";
		if (this == WAIT) {
			if (value >= 20)
				return (value / 20) + "s";
			return value + "t";
		}
		return "" + value;
	}

}
