package org.ihtsdo.otf.mapping.model;

/**
 * The interface object for MapPrinciple
 * @author Patrick
 *
 */
public interface MapPrinciple {
	
	/**
	 * Returns the id
	 * @return the id
	 */
	public Long getId();
	
	/**
	 * Sets the id
	 * @param id the id
	 */
	public void setId(Long id);
	
	/**
	 * Returns the description
	 * @return the description
	 */
	public String getDescription();
	
	/**
	 * Sets the description
	 * @param description the description
	 */
	public void setDescription(String description);

	/**
	 * Returns the name
	 * @return the name
	 */
	public String getName();
	
	/**
	 * Sets the name
	 * @param name the name
	 */
	public void setName(String name);
	
	/**
	 * Returns the section reference
	 * @return the section reference
	 */
	public String getSectionRef();
	
	/**
	 * Sets the section reference
	 * @param sectionRef the section reference
	 */
	public void setSectionRef(String sectionRef);
}