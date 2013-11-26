package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.RefSetMember;

/**
 * Abstract implementation of {@link RefSetMember}.
 */
public abstract class AbstractRefSetMember extends AbstractComponent implements
		RefSetMember {
	
	/** The ref set id */
	Long refSetId;

	/**
	 * Returns the ref set id
	 * @return the ref set id
	 */
	@Override
	public Long getRefSetId() {
		return this.refSetId;
	}

	/** Sets the ref set id
	 * @param refSetId the ref set id
	 */
	@Override
	public void setRefSetId(Long refSetId) {
		this.refSetId = refSetId;

	}
}
