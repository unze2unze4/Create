package com.simibubi.create.foundation.data;

public enum AllLangPartials {
	
	TEMPORARY("We aren't in Registrate yet"),
	ADVANCEMENTS("Advancements"),
	MESSAGES("UI & Messages"),
	TOOLTIPS("Item Descriptions"),
	
	;
	
	private String display;

	private AllLangPartials(String display) {
		this.display = display;
	}

	public String getDisplay() {
		return display;
	}

}
