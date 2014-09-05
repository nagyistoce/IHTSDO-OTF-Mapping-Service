package org.ihtsdo.otf.mapping.workflow;

import java.util.Set;

// TODO: Auto-generated Javadoc
/**
 * The Interface WorkflowException.
 */
public interface WorkflowException {

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId();

	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	public void setId(Long id);

	/**
	 * Gets the terminology id.
	 *
	 * @return the terminology id
	 */
	public String getTerminologyId();

	/**
	 * Sets the terminology id.
	 *
	 * @param terminologyId the new terminology id
	 */
	public void setTerminologyId(String terminologyId);

	/**
	 * Gets the terminology.
	 *
	 * @return the terminology
	 */
	public String getTerminology();

	/**
	 * Sets the terminology.
	 *
	 * @param terminology the new terminology
	 */
	public void setTerminology(String terminology);

	/**
	 * Gets the terminology version.
	 *
	 * @return the terminology version
	 */
	public String getTerminologyVersion();

	/**
	 * Sets the terminology version.
	 *
	 * @param string the new terminology version
	 */
	public void setTerminologyVersion(String string);

	/**
	 * Gets the false conflict map record ids.
	 *
	 * @return the false conflict map record ids
	 */
	public Set<Long> getFalseConflictMapRecordIds();

	/**
	 * Sets the false conflict map record ids.
	 *
	 * @param falseConflictMapRecordIds the new false conflict map record ids
	 */
	public void setFalseConflictMapRecordIds(Set<Long> falseConflictMapRecordIds);
	
	public void addFalseConflictMapRecordId(Long id);
	
	public void removeFalseConflictMapRecordId(Long id);

	/**
	 * Gets the map project id.
	 *
	 * @return the map project id
	 */
	public Long getMapProjectId();

	/**
	 * Sets the map project id.
	 *
	 * @param mapProjectId the new map project id
	 */
	public void setMapProjectId(Long mapProjectId);

	/** 
	 * Returns whether this WorkflowException contains any information
	 * @return true if this workflow exception contains any information
	 */
	public boolean isEmpty();

}