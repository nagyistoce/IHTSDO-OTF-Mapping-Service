package org.ihtsdo.otf.mapping.pojo;

import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlID;

import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapRecord;

/**
 * Reference implementation of {@link MapRecord}.
 * Includes hibernate tags for persistence
 *
 */
public class MapRecordImpl implements MapRecord {
	
	/** The id. */
	private Long id;
	
	/** The map project id. */
	private Long mapProjectId;

	/** The concept id. */
	private String conceptId;
	
	/** The concept name. */
	private String conceptName;
	
	/** The number of descendant concepts for the concept id */
	private Long countDescendantConcepts;

	/** The notes. */
	private Set<MapNote> mapNotes;
	
	/** The map entries. */
	private List<MapEntry> mapEntries;
	
	/** The map principles */
	private Set<MapPrinciple> mapPrinciples;

	/**
	 * Return the id
	 * @return the id
	 */
	@Override
	public Long getId() {
		return this.id;
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
	 * Returns the id in string form
	 * @return the id in string form
	 */
	@XmlID
	@Override
	public String getObjectId() {
		return id.toString();
	}
	

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#getMapProjectId()
	 */
	@Override
	public Long getMapProjectId() {
		return mapProjectId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#setMapProjectId(java.lang.Long)
	 */
	@Override
	public void setMapProjectId(Long mapProjectId) {
		this.mapProjectId = mapProjectId;
		
	}
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#getConceptId()
	 */
	@Override
	public String getConceptId() {
		return conceptId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#setConceptId(java.lang.String)
	 */
	@Override
	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	@Override
	public String getConceptName() {
		return this.conceptName;
	}

	@Override
	public void setConceptName(String conceptName) {
		this.conceptName = conceptName;
		
	}

	@Override
	public Long getCountDescendantConcepts() {
		return countDescendantConcepts;
	}

	@Override
	public void setCountDescendantConcepts(Long countDescendantConcepts) {
		this.countDescendantConcepts = countDescendantConcepts;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#getNotes()
	 */
	@Override
	public Set<MapNote> getMapNotes() {
		return mapNotes;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#setNotes(java.util.List)
	 */
	@Override
	public void setMapNotes(Set<MapNote> mapNotes) {
		this.mapNotes = mapNotes;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#addNote(org.ihtsdo.otf.mapping.model.MapNote)
	 */
	@Override
	public void addMapNote(MapNote mapNote) {
		mapNotes.add(mapNote);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#removeNote(org.ihtsdo.otf.mapping.model.MapNote)
	 */
	@Override
	public void removeMapNote(MapNote mapNote) {
		mapNotes.remove(mapNote);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#getMapEntries()
	 */
	@Override
	public List<MapEntry> getMapEntries() {
		return mapEntries;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#setMapEntries(java.util.List)
	 */
	@Override
	public void setMapEntries(List<MapEntry> mapEntries) {
		this.mapEntries = mapEntries;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#addMapEntry(org.ihtsdo.otf.mapping.model.MapEntry)
	 */
	@Override
	public void addMapEntry(MapEntry mapEntry) {
		mapEntries.add(mapEntry);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#removeMapEntry(org.ihtsdo.otf.mapping.model.MapEntry)
	 */
	@Override
	public void removeMapEntry(MapEntry mapEntry) {
		mapEntries.remove(mapEntry);
	}
	
	@Override
	public Set<MapPrinciple> getMapPrinciples() {
		return mapPrinciples;
	}

	@Override
	public void setMapPrinciples(Set<MapPrinciple> mapPrinciples) {
		this.mapPrinciples = mapPrinciples;
	}

	@Override
	public void addMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.add(mapPrinciple);
	}

	@Override
	public void removeMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.remove(mapPrinciple);
	}
	
	/**
	 * Function to correctly set the record object for map entries
	 * 
	 * @param record the MapRecord Jpa object from manager.find()
	 */
	@Override
	public void assignToChildren() {
		
		// assign to entries
		for (MapEntry entry : mapEntries) {
			entry.setMapRecord(this);
		}
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((conceptId == null) ? 0 : conceptId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((mapEntries == null) ? 0 : mapEntries.hashCode());
		result = prime * result + ((mapNotes == null) ? 0 : mapNotes.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
		MapRecordImpl other = (MapRecordImpl) obj;
		if (conceptId == null) {
			if (other.conceptId != null) {
				return false;
			}
		} else if (!conceptId.equals(other.conceptId)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (mapEntries == null) {
			if (other.mapEntries != null) {
				return false;
			}
		} else if (!mapEntries.equals(other.mapEntries)) {
			return false;
		}
		if (mapNotes == null) {
			if (other.mapNotes != null) {
				return false;
			}
		} else if (!mapNotes.equals(other.mapNotes)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MapRecordImpl [id=" + id + ", conceptId=" + conceptId
				+ ", mapNotes=" + mapNotes + ", mapEntries=" + mapEntries + "]";
	}

	


}
