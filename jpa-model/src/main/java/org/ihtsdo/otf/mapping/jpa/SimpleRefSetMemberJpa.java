package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.ihtsdo.otf.mapping.model.SimpleRefSetMember;

/**
 * Concrete implementation of {@link SimpleRefSetMember}.
 */
@Entity
@Table ( name = "simple_refset_members")
public class SimpleRefSetMemberJpa extends AbstractConceptRefSetMember
		implements SimpleRefSetMember {
	
	 /**
     * {@inheritDoc}
     */
	@Override
	public String toString() {
		return String.valueOf(getRefSetId());
	}

}