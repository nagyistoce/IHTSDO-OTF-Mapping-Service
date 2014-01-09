package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.MapPrinciple;


/**
 * The Map Principle Object for the Jpa Domain
 * @author Patrick
 *
 */
@Entity
@Table(name = "map_principles")
@Audited
@XmlRootElement(name="mapPrinciple")
public class MapPrincipleJpa implements MapPrinciple {

	@Id
	@GeneratedValue
	private Long id;
	
	@Column(nullable = false, length = 255)
	private String name;
	
	@Column(nullable = true, length = 4000)
	private String detail;
	
	@Column(nullable = true, length = 255)
	private String sectionRef;
	
	/** Default constructor */
	public MapPrincipleJpa() {
		// left empty
	}

	/**
	 * Return the id
	 * @return the id
	 */
	@XmlTransient
	@Override
	public Long getId() {
		return this.id;
	}
	
	/**
	 * Returns the id in string form
	 * @return the id in string form
	 */
	@XmlID
	@XmlElement
	public String getObjectId() {
		return id.toString();
	}

	/**
	 * Set the id
	 * @param id the id
	 */
	@Override
	public void setId(Long id) {
		this.id = id;		
	}

	/**
	 * Get the detail
	 * @return the detail
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	public String getDetail() {
		return this.detail;
	}

	/**
	 * Set the detail
	 * @param detail the detail
	 */
	@Override
	public void setDetail(String detail) {
		this.detail = detail;
		
	}

	/**
	 * Get the name
	 * @return the name
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	public String getName() {
		return this.name;
	}

	/**
	 * Set the name
	 * @param name the name
	 */
	@Override
	public void setName(String name) {
		this.name = name;
		
	}

	/**
	 * Get the section reference
	 * @return the section reference
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	public String getSectionRef() {
		return this.sectionRef;
	}

	/**
	 * Set the section reference
	 * @param sectionRef the section reference
	 */
	@Override
	public void setSectionRef(String sectionRef) {
		this.sectionRef = sectionRef;
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((detail == null) ? 0 : detail.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((sectionRef == null) ? 0 : sectionRef.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MapPrincipleJpa other = (MapPrincipleJpa) obj;
		if (detail == null) {
			if (other.detail != null) {
				return false;
			}
		} else if (!detail.equals(other.detail)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (sectionRef == null) {
			if (other.sectionRef != null) {
				return false;
			}
		} else if (!sectionRef.equals(other.sectionRef)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "MapPrincipleJpa [id=" + id + ", name=" + name
				+ ", detail=" + detail + ", sectionRef=" + sectionRef
				+ "]";
	}

}
