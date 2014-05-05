package org.ihtsdo.otf.mapping.services;

import java.util.List;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord;

/**
 * Represents a service for answering questions and performing actions
 * related to workflow management.
 */
public interface WorkflowService {

	/**
	 * Gets the workflow tracking record.
	 *
	 * @param mapProject the map project
	 * @param concept the concept
	 * @return the workflow tracking record
	 */
	public WorkflowTrackingRecord getWorkflowTrackingRecord(MapProject mapProject, Concept concept);
	
	/**
	 * Gets the workflow tracking records.
	 *
	 * @return the workflow tracking records
	 */
	public List<WorkflowTrackingRecord> getWorkflowTrackingRecords();
	
	/**
	 * Gets the workflow tracking records for map project.
	 *
	 * @param mapProject the map project
	 * @return the workflow tracking records for map project
	 */
	public List<WorkflowTrackingRecord> getWorkflowTrackingRecordsForMapProject(MapProject mapProject);

	/**
	 * Adds the workflow tracking record.
	 *
	 * @param workflowTrackingRecord the workflow tracking record
	 * @return the workflow tracking record
	 * @throws Exception the exception
	 */
	public WorkflowTrackingRecord addWorkflowTrackingRecord(WorkflowTrackingRecord workflowTrackingRecord) throws Exception;
	
	/**
	 * Update workflow tracking record.
	 *
	 * @param workflowTrackingRecord the workflow tracking record
	 * @throws Exception the exception
	 */
	public void updateWorkflowTrackingRecord(WorkflowTrackingRecord workflowTrackingRecord) throws Exception;
	
	/**
	 * Removes the workflow tracking record.
	 *
	 * @param workflowTrackingRecordId the workflow tracking record id
	 * @throws Exception the exception
	 */
	public void removeWorkflowTrackingRecord(Long workflowTrackingRecordId) throws Exception;
	
	/**
	 * Search Functions.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
     * @param pfsParameter the pfs parameter
	 * @return the search result list
	 */
	public SearchResultList findAvailableWork(MapProject mapProject, MapUser mapUser, PfsParameter pfsParameter);
	
	/**
	 * Find available conflicts.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
     * @param pfsParameter the pfs parameter
	 * @return the search result list
	 */
	public SearchResultList findAvailableConflicts(MapProject mapProject, MapUser mapUser, PfsParameter pfsParameter);
	
	/**
	 * Find assigned concepts.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
     * @param pfsParameter the pfs parameter
	 * @return the search result list
	 */
	public SearchResultList findAssignedWork(MapProject mapProject,
			MapUser mapUser, PfsParameter pfsParameter);

	
	/**
	 * Find assigned conflicts.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
     * @param pfsParameter the pfs parameter
	 * @return the search result list
	 */
	public SearchResultList findAssignedConflicts(MapProject mapProject, MapUser mapUser, PfsParameter pfsParameter);
	
	/**
	 * Find available consensus work.
	 *
	 * @param mapProject the map project
     * @param pfsParameter the pfs parameter
	 * @return the search result list
	 */
	public SearchResultList findAvailableConsensusWork(MapProject mapProject, PfsParameter pfsParameter);
	
	/**
	 * Called by REST services, performs a specific action given a project, concept, and user.
	 *
	 * @param mapProject the map project
	 * @param concept the concept
	 * @param mapUser the map user
	 * @param mapRecord the map record
	 * @param workflowAction the workflow action
	 * @throws Exception the exception
	 */
	public void processWorkflowAction(MapProject mapProject, Concept concept, MapUser mapUser, MapRecord mapRecord, WorkflowAction workflowAction) throws Exception;
	
	
	/**
	 * Synchronize workflow tracking record given the new version and the old version
	 *
	 * @param newTrackingRecord the new record, modified by processWorkflowAction
	 * @param oldTrackingRecord the old record, from the database
	 * @throws Exception 
	 */
	public void synchronizeWorkflowTrackingRecord(WorkflowTrackingRecord newTrackingRecord,
			WorkflowTrackingRecord oldTrackingRecord) throws Exception;


	
	/**
	 * Compute workflow.
	 *
	 * @param mapProject the map project
	 * @throws Exception the exception
	 */
	public void computeWorkflow(MapProject mapProject) throws Exception;
	
	/**
	 * Clear workflow for map project.
	 *
	 * @param mapProject the map project
	 * @throws Exception the exception
	 */
	public void clearWorkflowForMapProject(MapProject mapProject) throws Exception;
	
	
	/**
	 * Closes the manager associated with service.
	 *
	 * @throws Exception the exception
	 */
	public void close() throws Exception;


	/**
	 * Gets the transaction per operation.
	 *
	 * @return the transaction per operation
	 * @throws Exception the exception
	 */
	public boolean getTransactionPerOperation() throws Exception;
	

	/**
	 * Sets the transaction per operation.
	 *
	 * @param transactionPerOperation the transaction per operation
	 * @throws Exception the exception
	 */
	public void setTransactionPerOperation(boolean transactionPerOperation) throws Exception;
	
	/**
	 * Begin transaction.
	 *
	 * @throws Exception the exception
	 */
	public void beginTransaction() throws Exception;
	
	/**
	 * Commit.
	 *
	 * @throws Exception the exception
	 */
	public void commit() throws Exception;


}


