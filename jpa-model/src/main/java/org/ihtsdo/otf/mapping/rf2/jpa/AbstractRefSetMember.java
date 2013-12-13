package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.rf2.RefSetMember;

/**
 * Abstract implementation of {@link RefSetMember} for use with JPA
 */
@MappedSuperclass
@Audited
public abstract class AbstractRefSetMember extends AbstractComponent implements
		RefSetMember {
	
	/** The ref set id */
	@Column ( nullable = false)
	Long refSetId;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long getRefSetId() {
		return this.refSetId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRefSetId(Long refSetId) {
		this.refSetId = refSetId;

	}
}
