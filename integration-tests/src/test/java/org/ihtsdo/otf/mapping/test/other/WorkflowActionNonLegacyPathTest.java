package org.ihtsdo.otf.mapping.test.other;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;

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
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowNonLegacyPathHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for workflow actions on non legacy path.
 */
public class WorkflowActionNonLegacyPathTest {

  // the content
  /** The concept. */
  private static Concept concept;

  // the mapping objects
  /** The lead. */
  private static MapUser viewer, specialist, specialist2, lead;

  /** The lead record. */
  private static MapRecord specRecord, specRecord2, leadRecord;

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
  private static WorkflowNonLegacyPathHandler handler;

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void init() throws Exception {

    System.out.println("Initialization");

    // instantiate the services
    contentService = new ContentServiceJpa();
    mappingService = new MappingServiceJpa();
    workflowService = new WorkflowServiceJpa();

    // instantiate the workflow handler
    handler = new WorkflowNonLegacyPathHandler();

    // ensure database is clean
    for (Concept c : contentService.getConcepts().getIterable())
      contentService.removeConcept(c.getId());

    for (MapProject mp : mappingService.getMapProjects().getIterable())
      mappingService.removeMapProject(mp.getId());

    for (MapRecord mp : mappingService.getMapRecords().getIterable())
      mappingService.removeMapRecord(mp.getId());

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

    specialist2 = new MapUserJpa();
    specialist2.setApplicationRole(MapUserRole.VIEWER);
    specialist2.setEmail("none");
    specialist2.setName("Specialist2");
    specialist2.setUserName("spec2");
    mappingService.addMapUser(specialist2);

    lead = new MapUserJpa();
    lead.setApplicationRole(MapUserRole.VIEWER);
    lead.setEmail("none");
    lead.setName("Lead");
    lead.setUserName("lead");
    mappingService.addMapUser(lead);

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
    mapProject.addMapSpecialist(specialist2);
    mapProject.addMapLead(lead);
    mapProject.addScopeConcept("1");
    mappingService.addMapProject(mapProject);

  }

  /**
   * Test initial state.
   *
   * @throws Exception the exception
   */
  @Test
  public void testInitialState() throws Exception {

    // clear existing records
    clearMapRecords();

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

    // Test: Specialist
    result = testAllActionsForUser(specialist);

    // all actions but CANCEL and ASSIGN_FROM_SCRATCH should fail
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

    // Test: Specialist2
    result = testAllActionsForUser(specialist2);

    // all actions but CANCEL and ASSIGN_FROM_SCRATCH should fail
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

    // Test: assign lead
    result = testAllActionsForUser(lead);

    // all actions but CANCEL and ASSIGN_FROM_SCRATCH should fail
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
   * Test first specialist editing state.
   *
   * @throws Exception the exception
   */
  @Test
  public void testFirstSpecialistEditingState() throws Exception {

    // same test for both NEW and EDITING_IN_PROGRESS
    for (WorkflowStatus status : Arrays.asList(WorkflowStatus.NEW,
        WorkflowStatus.EDITING_IN_PROGRESS)) {

      // clear existing records
      clearMapRecords();

      // compute workflow
      getTrackingRecord();

      // create specialist record
      specRecord = createRecord(specialist, status);
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

      // Test: Specialist
      result = testAllActionsForUser(specialist);

      // all actions but SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN should fail
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

      // Test: Specialist
      result = testAllActionsForUser(specialist2);

      // all actions but Cancel and ASSIGN_FROM_SCRATCH should fail
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

      // Test: assign lead
      result = testAllActionsForUser(lead);

      // all actions but CANCEL should fail
      // all actions but Cancel and ASSIGN_FROM_SCRATCH should fail
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

  }

  /**
   * Test second specialist editing state.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSecondSpecialistEditingState() throws Exception {

    // first specialist
    for (WorkflowStatus status1 : Arrays.asList(WorkflowStatus.NEW,
        WorkflowStatus.EDITING_IN_PROGRESS, WorkflowStatus.EDITING_DONE)) {
      for (WorkflowStatus status2 : Arrays.asList(WorkflowStatus.NEW,
          WorkflowStatus.EDITING_IN_PROGRESS)) {

        // clear existing records
        clearMapRecords();

        // compute workflow
        getTrackingRecord();

        // create specialist record
        specRecord = createRecord(specialist, status1);
        mappingService.addMapRecord(specRecord);

        // create second specialist record
        specRecord2 = createRecord(specialist2, status2);
        mappingService.addMapRecord(specRecord2);

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

        // Test: Specialist
        result = testAllActionsForUser(specialist);

        // all actions but SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN should fail
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

        // Test: Specialist
        result = testAllActionsForUser(specialist2);

        // all actions but SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN should fail
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

        // Test: assign lead
        result = testAllActionsForUser(lead);

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
      }
    }

  }

  /**
   * Test conflict detected state.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConflictDetectedState() throws Exception {

    // clear existing records
    clearMapRecords();

    // compute workflow
    getTrackingRecord();

    // create specialist record
    specRecord = createRecord(specialist, WorkflowStatus.CONFLICT_DETECTED);
    mappingService.addMapRecord(specRecord);

    // create specialist2 record
    specRecord2 = createRecord(specialist2, WorkflowStatus.CONFLICT_DETECTED);
    mappingService.addMapRecord(specRecord2);

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

    // Test: Specialist
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

    // Test: Specialist2
    result = testAllActionsForUser(specialist2);

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

    // Test: Lead
    result = testAllActionsForUser(lead);

    // all actions but CANCEL and ASSIGN_FROM_SCRATCH should fail
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
   * Test lead editing state.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLeadEditingState() throws Exception {

    for (WorkflowStatus status : Arrays.asList(WorkflowStatus.CONFLICT_NEW,
        WorkflowStatus.CONFLICT_IN_PROGRESS)) {

      // clear existing records
      clearMapRecords();

      // compute workflow
      getTrackingRecord();

      // create specialist record
      specRecord = createRecord(specialist, WorkflowStatus.CONFLICT_DETECTED);
      mappingService.addMapRecord(specRecord);

      // create specialist2 record
      specRecord2 = createRecord(specialist2, WorkflowStatus.CONFLICT_DETECTED);
      mappingService.addMapRecord(specRecord2);

      leadRecord = createRecord(lead, status);
      mappingService.addMapRecord(leadRecord);

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

      // Test: Specialist
      result = testAllActionsForUser(specialist);

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

      // Test: Specialist2
      result = testAllActionsForUser(specialist2);

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

      // Test: Lead
      result = testAllActionsForUser(lead);

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
   * Test lead finished state.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLeadFinishedState() throws Exception {

    // clear existing records
    clearMapRecords();

    // compute workflow
    getTrackingRecord();

    // create specialist record
    specRecord = createRecord(specialist, WorkflowStatus.CONFLICT_DETECTED);
    mappingService.addMapRecord(specRecord);

    // create specialist2 record
    specRecord2 = createRecord(specialist2, WorkflowStatus.CONFLICT_DETECTED);
    mappingService.addMapRecord(specRecord2);

    leadRecord = createRecord(lead, WorkflowStatus.CONFLICT_RESOLVED);
    mappingService.addMapRecord(leadRecord);

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

    // Test: Specialist
    result = testAllActionsForUser(specialist);

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

    // Test: Specialist2
    result = testAllActionsForUser(specialist2);

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

    // Test: Lead
    result = testAllActionsForUser(lead);

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

    System.out.println("Cleanup.");

    workflowService.clearWorkflowForMapProject(mapProject);
    workflowService.close();

    if (specRecord != null)
      mappingService.removeMapRecord(specRecord.getId());
    if (specRecord2 != null)
      mappingService.removeMapRecord(specRecord2.getId());
    if (leadRecord != null)
      mappingService.removeMapRecord(leadRecord.getId());

    mappingService.removeMapProject(mapProject.getId());
    mappingService.removeMapUser(specialist.getId());
    mappingService.removeMapUser(specialist2.getId());
    mappingService.removeMapUser(lead.getId());
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
    System.out.println("Getting tracking record for project "
        + mapProject.getId() + " and concept " + concept.getTerminologyId());
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
    System.out.println("Clearing map records.");
    for (MapRecord mr : mappingService.getMapRecords().getIterable()) {
      mappingService.removeMapRecord(mr.getId());
    }
    specRecord = null;
    specRecord2 = null;
    leadRecord = null;
    Thread.sleep(1000);
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
        result.addMessage(action.toString());
      } else {
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
