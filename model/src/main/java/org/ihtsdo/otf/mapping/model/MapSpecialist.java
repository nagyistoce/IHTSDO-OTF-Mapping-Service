package org.ihtsdo.otf.mapping.model;


/**
 * Represents a map specialist role.
 *
 */
public interface MapSpecialist  {
	
	/**
	 * Returns the id.
	 *
	 * @return the id
	 */
	public Long getId();

	/**
	 * Sets the id.
	 *
	 * @param id the id
	 */
	public void setId(Long id);
	
	/**
	 * Returns the user name.
	 *
	 * @return the user name
	 */
	public String getUserName();
	
	/**
	 * Sets the user name.
	 *
	 * @param username the user name
	 */
	public void setUserName(String username);
	
	/**
	 * Returns the name.
	 *
	 * @return the name
	 */
	public String getName();
	
	/**
	 * Sets the name.
	 *
	 * @param name the name
	 */
	public void setName(String name);

}
