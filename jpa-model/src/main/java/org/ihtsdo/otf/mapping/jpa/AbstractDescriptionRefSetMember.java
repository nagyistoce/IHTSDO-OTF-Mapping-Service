package org.ihtsdo.otf.mapping.jpa;

import org.ihtsdo.otf.mapping.model.Description;
import org.ihtsdo.otf.mapping.model.DescriptionRefSetMember;

import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 * Abstract implementation of {@link DescriptionRefSetMember}.
 */
@MappedSuperclass
public abstract class AbstractDescriptionRefSetMember extends
		AbstractRefSetMember implements DescriptionRefSetMember {
	
	
	/** The Description associated with this element */
	@ManyToOne(cascade = {
			   CascadeType.PERSIST, CascadeType.MERGE
			 }, targetEntity=DescriptionJpa.class)
	private Description description;

	/**
     * {@inheritDoc}
     */
	@Override
	public Description getDescription() {
		return this.description;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public void setDescription(Description description) {
		this.description = description;

	}
}
