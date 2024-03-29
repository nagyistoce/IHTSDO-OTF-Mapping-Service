package org.ihtsdo.otf.mapping.test.other;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowQaPathHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for workflow action on qa path test.
 */
public class WorkflowActionQaPathTest {

  // the content
  /** The concept. */
  private static Concept concept;

  // the mapping objects
  /** The loader. */
  private static MapUser viewer, specialist, loader;

  /** The loader record. */
  private static MapRecord revisionRecord, specRecord, loaderRecord;

  /** The map project. */
  private static MapProject mapProject;

  // the tracking record
  /** The tracking record. */
  private static TrackingRecord trackingRecord;

  // the services
  /** The content service. */
  private static ContentService contentService;

  /** The mapping service. */
  private static MappingService mappingService;

  /** The workflow service. */
  private static WorkflowService workflowService;

  // the workflow handler
  /** The handler. */
  private static WorkflowQaPathHandler handler;

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void init() throws Exception {

    // instantiate the services
    contentService = new ContentServiceJpa();
    mappingService = new MappingServiceJpa();
    workflowService = new WorkflowServiceJpa();

    // instantiate the workflow handler
    handler = new WorkflowQaPathHandler();

    // ensure database is clean
    for (Concept c : contentService.getConcepts().getIterable()) {
      for (Relationship r : c.getRelationships()) {
        contentService.removeRelationship(r.getId());
      }
    }
    for (Concept c : contentService.getConcepts().getIterable()) {
      contentService.removeConcept(c.getId());
    }

    for (MapProject mp : mappingService.getMapProjects().getIterable())
      mappingService.removeMapProject(mp.getId());

    for (MapUser mu : mappingService.getMapUsers().getIterable())
      if (!mu.getUserName().equals("guest")
          && !mu.getUserName().equals("loader")
          && !mu.getUserName().equals("qa")) {
        mappingService.removeMapUser(mu.getId());
      }

    for (TrackingRecord tr : workflowService.getTrackingRecords().getIterable())
      workflowService.removeTrackingRecord(tr.getId());

    concept = new ConceptJpa();
    concept.setActive(true);
    concept.setDefaultPreferredName("Test Concept");
    concept.setDefinitionStatusId(0L);
    concept.setEffectiveTime(new Date());
    concept.setModuleId(0L);
    concept.setTerminology("sourceTerminology");
    concept.setTerminologyVersion("sourceTerminologyVersion");
    concept.setTerminologyId("1");
    concept.setLabel("integration-test");
    contentService.addConcept(concept);

    // instantiate and add the users
    viewer = new MapUserJpa();
    viewer.setApplicationRole(MapUserRole.VIEWER);
    viewer.setEmail("none");
    viewer.setName("Viewer");
    viewer.setUserName("view");
    mappingService.addMapUser(viewer);

    specialist = new MapUserJpa();
    specialist.setApplicationRole(MapUserRole.VIEWER);
    specialist.setEmail("none");
    specialist.setName("Specialist");
    specialist.setUserName("spec");
    mappingService.addMapUser(specialist);

    // instantiate and add the loader user, used for REVISION records
    loader = mappingService.getMapUser("loader");

    // instantiate the project
    mapProject = new MapProjectJpa();
    mapProject.setSourceTerminology("sourceTerminology");
    mapProject.setSourceTerminologyVersion("sourceTerminologyVersion");
    mapProject.setDestinationTerminology("destinationTerminology");
    mapProject
        .setDestinationTerminologyVersion("destinationTerminologyVersion");
    mapProject.setGroupStructure(false);
    mapProject.setMapRefsetPattern(MapRefsetPattern.ExtendedMap);
    mapProject.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
    mapProject.setName("Test Project");
    mapProject.setPropagatedFlag(false);
    mapProject
        .setProjectSpecificAlgorithmHandlerClass("org.ihtsdo.otf.mapping.jpa.handlers.ICD10ProjectSpecificAlgorithmHandler");
    mapProject.setPublic(true);
    mapProject.setRefSetId("refsetId");
    mapProject.setRuleBased(true);
    mapProject.setWorkflowType(WorkflowType.REVIEW_PROJECT);
    mapProject.addMapSpecialist(specialist);
    mapProject.addScopeConcept("1");
    mappingService.addMapProject(mapProject);

    // compute the workflow
    workflowService.computeWorkflow(mapProject);

  }

  /**
   * Test qa needed state.
   *
   * @throws Exception the exception
   */
  @Test
  public void testQaNeededState() throws Exception {
    Logger.getLogger(getClass()).info("TEST qa needed state");

    // clear existing records
    clearMapRecords();
    // create revision and specialist record
    revisionRecord = createRecord(loader, WorkflowStatus.REVISION);
    mappingService.addMapRecord(revisionRecord);

    loaderRecord = createRecord(loader, WorkflowStatus.QA_NEEDED);
    mappingService.addMapRecord(loaderRecord);

    // compute workflow
    getTrackingRecord();

    // Test: assign viewer
    ValidationResult result = testAllActionsForUser(viewer);

    // all actions except cancel should fail
    for (WorkflowAction action : WorkflowAction.values()) {
      switch (action) {
        case ASSIGN_FROM_INITIAL_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case ASSIGN_FROM_SCRATCH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case CANCEL:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case CREATE_QA_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case FINISH_EDITING:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case PUBLISH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case SAVE_FOR_LATER:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case UNASSIGN:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        default:
          break;

      }
    }

    // Test: Loader
    result = testAllActionsForUser(loader);

    // all actions except cancel and UNASSIGN should fail
    for (WorkflowAction action : WorkflowAction.values()) {
      switch (action) {
        case ASSIGN_FROM_INITIAL_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case ASSIGN_FROM_SCRATCH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case CANCEL:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case CREATE_QA_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case FINISH_EDITING:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case PUBLISH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case SAVE_FOR_LATER:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case UNASSIGN:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        default:
          break;

      }
    }

    // Test: Specialist
    result = testAllActionsForUser(specialist);

    // all actions but CANCEL, ASSIGN_FROM_SCRATCH should fail
    for (WorkflowAction action : WorkflowAction.values()) {
      switch (action) {
        case ASSIGN_FROM_INITIAL_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case ASSIGN_FROM_SCRATCH:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case CANCEL:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case CREATE_QA_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case FINISH_EDITING:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case PUBLISH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case SAVE_FOR_LATER:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case UNASSIGN:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        default:
          break;

      }
    }

  }

  /**
   * Test editing state.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEditingState() throws Exception {

    for (WorkflowStatus status : Arrays.asList(WorkflowStatus.QA_NEW,
        WorkflowStatus.QA_IN_PROGRESS)) {

      // clear existing records
      clearMapRecords();

      // create revision, specialist, and lead record
      revisionRecord = createRecord(loader, WorkflowStatus.REVISION);
      mappingService.addMapRecord(revisionRecord);

      loaderRecord = createRecord(loader, WorkflowStatus.QA_NEEDED);
      mappingService.addMapRecord(loaderRecord);

      specRecord = createRecord(specialist, status);
      mappingService.addMapRecord(specRecord);

      // compute workflow
      getTrackingRecord();

      // Test: viewer
      ValidationResult result = testAllActionsForUser(viewer);
      Logger.getLogger(getClass()).info("result=" + result);
      // all actions except cancel and create_qa_record should fail
      for (WorkflowAction action : WorkflowAction.values()) {
        switch (action) {
          case ASSIGN_FROM_INITIAL_RECORD:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case ASSIGN_FROM_SCRATCH:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case CANCEL:
            assertTrue(result.getMessages().contains(action.toString()));
            break;
          case CREATE_QA_RECORD:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case FINISH_EDITING:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case PUBLISH:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case SAVE_FOR_LATER:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case UNASSIGN:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          default:
            break;

        }
      }

      // Test: Specialist
      result = testAllActionsForUser(loader);

      // all actions but CANCEL should fail
      for (WorkflowAction action : WorkflowAction.values()) {
        switch (action) {
          case ASSIGN_FROM_INITIAL_RECORD:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case ASSIGN_FROM_SCRATCH:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case CANCEL:
            assertTrue(result.getMessages().contains(action.toString()));
            break;
          case CREATE_QA_RECORD:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case FINISH_EDITING:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case PUBLISH:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case SAVE_FOR_LATER:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case UNASSIGN:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          default:
            break;

        }
      }

      // Test: specialist
      result = testAllActionsForUser(specialist);

      // all actions but CANCEL, SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN should
      // fail
      for (WorkflowAction action : WorkflowAction.values()) {
        switch (action) {
          case ASSIGN_FROM_INITIAL_RECORD:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case ASSIGN_FROM_SCRATCH:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case CANCEL:
            assertTrue(result.getMessages().contains(action.toString()));
            break;
          case CREATE_QA_RECORD:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case FINISH_EDITING:
            assertTrue(result.getMessages().contains(action.toString()));
            break;
          case PUBLISH:
            assertTrue(result.getErrors().contains(action.toString()));
            break;
          case SAVE_FOR_LATER:
            assertTrue(result.getMessages().contains(action.toString()));
            break;
          case UNASSIGN:
            assertTrue(result.getMessages().contains(action.toString()));
            break;
          default:
            break;

        }
      }
    }

  }

  /**
   * Test finished state.
   *
   * @throws Exception the exception
   */
  @Test
  public void testFinishedState() throws Exception {

    // clear existing records
    clearMapRecords();

    // create revision, specialist, and lead record
    revisionRecord = createRecord(loader, WorkflowStatus.REVISION);
    mappingService.addMapRecord(revisionRecord);

    loaderRecord = createRecord(loader, WorkflowStatus.QA_NEEDED);
    mappingService.addMapRecord(loaderRecord);

    specRecord = createRecord(specialist, WorkflowStatus.QA_RESOLVED);
    mappingService.addMapRecord(specRecord);

    // compute workflow
    getTrackingRecord();

    // Test: assign viewer
    ValidationResult result = testAllActionsForUser(viewer);

    // all actions except cancel should fail
    for (WorkflowAction action : WorkflowAction.values()) {
      switch (action) {
        case ASSIGN_FROM_INITIAL_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case ASSIGN_FROM_SCRATCH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case CANCEL:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case CREATE_QA_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case FINISH_EDITING:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case PUBLISH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case SAVE_FOR_LATER:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case UNASSIGN:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        default:
          break;

      }
    }

    // Test: Loader
    result = testAllActionsForUser(loader);

    // all actions but CANCEL should fail
    for (WorkflowAction action : WorkflowAction.values()) {
      switch (action) {
        case ASSIGN_FROM_INITIAL_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case ASSIGN_FROM_SCRATCH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case CANCEL:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case CREATE_QA_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case FINISH_EDITING:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case PUBLISH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case SAVE_FOR_LATER:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case UNASSIGN:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        default:
          break;

      }
    }

    // Test: assign lead
    result = testAllActionsForUser(specialist);

    // all actions but CANCEL, SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN, PUBLISH
    // should fail
    for (WorkflowAction action : WorkflowAction.values()) {
      switch (action) {
        case ASSIGN_FROM_INITIAL_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case ASSIGN_FROM_SCRATCH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case CANCEL:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case CREATE_QA_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case FINISH_EDITING:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case PUBLISH:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case SAVE_FOR_LATER:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case UNASSIGN:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        default:
          break;

      }
    }

  }

  /**
   * Cleanup.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void cleanup() throws Exception {
    workflowService.clearWorkflowForMapProject(mapProject);
    workflowService.close();

    if (revisionRecord != null)
      mappingService.removeMapRecord(revisionRecord.getId());
    if (specRecord != null)
      mappingService.removeMapRecord(specRecord.getId());
    if (loaderRecord != null)
      mappingService.removeMapRecord(loaderRecord.getId());

    mappingService.removeMapProject(mapProject.getId());
    mappingService.removeMapUser(specialist.getId());
    mappingService.removeMapUser(loader.getId());
    mappingService.close();

    contentService.removeConcept(concept.getId());
    contentService.close();

  }

  /**
   * Returns the tracking record.
   *
   * @throws Exception the exception
   */
  private void getTrackingRecord() throws Exception {
    Logger.getLogger(getClass()).info(
        "Getting tracking record for project " + mapProject.getId()
            + " and concept " + concept.getTerminologyId());
    workflowService.computeWorkflow(mapProject);
    Thread.sleep(1000);
    trackingRecord = workflowService.getTrackingRecord(mapProject, concept);
  }

  /**
   * Clear map records.
   *
   * @throws Exception the exception
   */
  private void clearMapRecords() throws Exception {
    Logger.getLogger(getClass()).info("Clearing map records.");
    for (MapRecord mr : mappingService.getMapRecords().getIterable()) {
      mappingService.removeMapRecord(mr.getId());
    }
    revisionRecord = null;
    specRecord = null;
    loaderRecord = null;
    Thread.sleep(500);
  }

  /**
   * Test all actions for user.
   *
   * @param user the user
   * @return the validation result
   * @throws Exception the exception
   */
  private ValidationResult testAllActionsForUser(MapUser user) throws Exception {
    ValidationResult result = new ValidationResultJpa();

    for (WorkflowAction action : WorkflowAction.values()) {
      ValidationResult actionResult =
          handler.validateTrackingRecordForActionAndUser(trackingRecord,
              action, user);
      if (actionResult.isValid()) {
        Logger.getLogger(getClass()).info(action + " valid");
        result.addMessage(action.toString());
      } else {
        Logger.getLogger(getClass()).info(
            action + " invalid -- " + actionResult.toString());
        result.addError(action.toString());
      }
    }
    return result;
  }

  /**
   * Creates the record.
   *
   * @param user the user
   * @param status the status
   * @return the map record
   */
  private MapRecord createRecord(MapUser user, WorkflowStatus status) {
    MapRecord record = new MapRecordJpa();

    record.setConceptId(concept.getTerminologyId());
    record.setConceptName(concept.getDefaultPreferredName());
    record.setLastModified(new Date().getTime());
    record.setLastModifiedBy(user);
    record.setMapProjectId(mapProject.getId());
    record.setOwner(user);
    record.setTimestamp(new Date().getTime());
    record.setWorkflowStatus(status);
    return record;
  }

}
