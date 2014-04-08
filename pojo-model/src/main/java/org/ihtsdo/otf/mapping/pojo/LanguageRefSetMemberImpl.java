package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.LanguageRefSetMember;

/**
 * Concrete implementation of {@link LanguageRefSetMember}.
 */
public class LanguageRefSetMemberImpl extends AbstractDescriptionRefSetMember
		implements LanguageRefSetMember {
	
	/** the acceptability id */
	private Long acceptabilityId;

	/** returns the acceptability id
	 * @return the acceptability id
	 */
	@Override
	public Long getAcceptabilityId() {
		return this.acceptabilityId;
	}

	/** sets the acceptability id
	 * @param acceptabilityId the acceptability id
	 */
	@Override
	public void setAcceptabilityId(Long acceptabilityId) {
		this.acceptabilityId = acceptabilityId;

	}
}