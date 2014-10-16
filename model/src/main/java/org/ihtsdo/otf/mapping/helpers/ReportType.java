package org.ihtsdo.otf.mapping.helpers;

public enum ReportType {

	/** The specialist productivity report */
	SPECIALIST_PRODUCTIVITY, 
	
	/** The weekly specialist productivity report */
	SPECIALIST_WEEKLY_PRODUCTIVITY,
	
	/** The monthly specialist productivity report */
	SPECIALIST_MONTHLY_PRODUCTIVITY,
	
	/** The total concepts currently in editing */
	TOTAL_EDITING, 
	
	/** The total concepts with completed mappings */
	TOTAL_MAPPED,
	
	/** The total concepts not yet in editing */
	TOTAL_UNMAPPED;

}