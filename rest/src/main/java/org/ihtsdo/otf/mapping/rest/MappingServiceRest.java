package org.ihtsdo.otf.mapping.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.dto.KeyValuePair;
import org.ihtsdo.otf.mapping.dto.KeyValuePairList;
import org.ihtsdo.otf.mapping.dto.KeyValuePairLists;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.MapAgeRangeListJpa;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleListJpa;
import org.ihtsdo.otf.mapping.helpers.MapProjectListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRelationListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.TreePositionListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * REST implementation for mapping service.
 *
 * @author ${author}
 */
@Path("/mapping")
@Api(value = "/mapping", description = "Operations supporting map objects.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class MappingServiceRest extends RootServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link MappingServiceRest}.
   * 
   * @throws Exception the exception
   */
  public MappingServiceRest() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Projects
  // ///////////////////////////////////////////////////

  /**
   * Returns all map projects in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map projects
   * @throws Exception the exception
   */
  @GET
  @Path("/project/projects")
  @ApiOperation(value = "Get map projects.", notes = "Gets a list of all map projects.", response = MapProjectListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProjectListJpa getMapProjects(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/projects");
    String user = "";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER)) {
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to retrieve map projects.")
            .build());
      }

      // instantiate list of projects to return
      MapProjectListJpa mapProjects = new MapProjectListJpa();

      // cycle over projects and verify that this user can view each project
      for (MapProject mapProject : mappingService.getMapProjects()
          .getMapProjects()) {

        // if this user has a role of VIEWER or above for this project (i.e. is
        // not NONE)
        if (!securityService.getMapProjectRoleForToken(authToken,
            mapProject.getId()).equals(MapUserRole.NONE)
            || mapProject.isPublic()) {

          // do not serialize the scope concepts or excludes (retrieval
          // optimization)
          mapProject.setScopeConcepts(null);
          mapProject.setScopeExcludedConcepts(null);

          mapProjects.addMapProject(mapProject);
        }

      }

      // set total count to count for completeness (not a paged list)
      mapProjects.setTotalCount(mapProjects.getCount());

      // sort projects by name
      mapProjects.sortBy(new Comparator<MapProject>() {
        @Override
        public int compare(MapProject o1, MapProject o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });

      // close the mapping service and return the viewable projects
      return mapProjects;

    } catch (Exception e) {
      this.handleException(e, "trying to retrieve map projects", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns the project for a given id (auto-generated) in JSON format.
   *
   * @param mapProjectId the mapProjectId
   * @param authToken the auth token
   * @return the mapProject
   * @throws Exception the exception
   */
  @GET
  @Path("/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get map project by id.", notes = "Gets a map project for the specified parameters.", response = MapProject.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProject getMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/id/" + mapProjectId.toString());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve the map project.")
            .build());

      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      mapProject.getScopeConcepts().size();
      mapProject.getScopeExcludedConcepts().size();
      mapProject.getMapAdvices().size();
      mapProject.getMapRelations().size();
      mapProject.getMapLeads().size();
      mapProject.getMapSpecialists().size();
      mapProject.getMapPrinciples().size();
      mapProject.getPresetAgeRanges().size();
      return mapProject;

    } catch (Exception e) {
      handleException(e, "trying to retrieve the map project", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds a map project.
   *
   * @param mapProject the map project to be added
   * @param authToken the auth token
   * @return returns the added map project object
   * @throws Exception the exception
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/project/add")
  @ApiOperation(value = "Add a map project.", notes = "Adds the specified map project.", response = MapProjectJpa.class)
  public MapProject addMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/add");

    String user = "";
    String project = "";
    MappingService mappingService = new MappingServiceJpa();

    try {

      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add a map project.")
            .build());

      // check that project specific handler exists as a class

      try {
        Class.forName(mapProject.getProjectSpecificAlgorithmHandlerClass());
      } catch (ClassNotFoundException e) {
        throw new LocalException(
            "Adding map project failed -- could not find project specific algorithm handler for class name: "
                + mapProject.getProjectSpecificAlgorithmHandlerClass());
      }

      MapProject mp = mappingService.addMapProject(mapProject);
      project = mp.getName();

      return mp;

    } catch (Exception e) {
      handleException(e, "trying to add a map project", user, project, "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Updates a map project.
   *
   * @param mapProject the map project to be added
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/project/update")
  @ApiOperation(value = "Update a map project.", notes = "Updates specified map project if it already exists.", response = MapProjectJpa.class)
  public void updateMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/update");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      // IF application role = admin
      // OR map project role = lead

      MapUserRole roleProject =
          securityService.getMapProjectRoleForToken(authToken,
              mapProject.getId());
      MapUserRole roleApplication =
          securityService.getApplicationRoleForToken(authToken);

      if (!(roleApplication.hasPrivilegesOf(MapUserRole.ADMINISTRATOR) || roleProject
          .hasPrivilegesOf(MapUserRole.LEAD)))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to udpate a map project.")
            .build());

      // check that project specific handler exists as a class
      try {
        Class.forName(mapProject.getProjectSpecificAlgorithmHandlerClass());
      } catch (ClassNotFoundException e) {
        throw new LocalException(
            "Updating map project failed -- could not find project specific algorithm handler for class name: "
                + mapProject.getProjectSpecificAlgorithmHandlerClass());
      }

      // scope includes and excludes are transient, and must be added to project
      // before update from webapp
      MapProject mapProjectFromDatabase =
          mappingService.getMapProject(mapProject.getId());

      // set the scope concepts and excludes to the contents of the database
      // prior to update
      mapProject.setScopeConcepts(mapProjectFromDatabase.getScopeConcepts());
      mapProject.setScopeExcludedConcepts(mapProjectFromDatabase
          .getScopeExcludedConcepts());

      // update the project and close the service
      mappingService.updateMapProject(mapProject);

    } catch (Exception e) {
      handleException(e, "trying to update a map project", user,
          mapProject.getName(), "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a map project.
   *
   * @param mapProject the map project
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @DELETE
  @Path("/project/delete")
  @ApiOperation(value = "Remove a map project.", notes = "Removes specified map project if it already exists.", response = MapProject.class)
  public void removeMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/delete for " + mapProject.getName());

    String user = "";

    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove a map project.")
            .build());

      mappingService.removeMapProject(mapProject.getId());

    } catch (Exception e) {
      handleException(e, "trying to remove a map project", user,
          mapProject.getName(), "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns all map projects for a lucene query.
   *
   * @param query the string query
   * @param authToken the auth token
   * @return the map projects
   * @throws Exception the exception
   */
  @GET
  @Path("/project/query/{String}")
  @ApiOperation(value = "Find map projects by query.", notes = "Gets a list of search results for map projects matching the lucene query.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findMapProjectsForQuery(
    @ApiParam(value = "Query, e.g. 'SNOMED to ICD10'", required = true) @PathParam("String") String query,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/query/" + query);
    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to find map projects.")
            .build());

      SearchResultList searchResultList =
          mappingService.findMapProjectsForQuery(query, new PfsParameterJpa());
      return searchResultList;

    } catch (Exception e) {
      handleException(e, "trying to find map projects", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns all map projects for a map user.
   *
   * @param mapUserName the map user name
   * @param authToken the auth token
   * @return the map projects
   * @throws Exception the exception
   */
  @GET
  @Path("/project/user/id/{username}")
  @ApiOperation(value = "Get all map projects for a user.", notes = "Gets a list of map projects for the specified user.", response = MapProjectListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProjectListJpa getMapProjectsForUser(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String mapUserName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/user/id/" + mapUserName);
    String user = "";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get the map projects for given user.")
                .build());

      MapUser mapLead = mappingService.getMapUser(mapUserName);
      MapProjectListJpa mapProjects =
          (MapProjectListJpa) mappingService.getMapProjectsForMapUser(mapLead);

      for (MapProject mapProject : mapProjects.getMapProjects()) {
        mapProject.getScopeConcepts().size();
        mapProject.getScopeExcludedConcepts().size();
        mapProject.getMapAdvices().size();
        mapProject.getMapRelations().size();
        mapProject.getMapLeads().size();
        mapProject.getMapSpecialists().size();
        mapProject.getMapPrinciples().size();
        mapProject.getPresetAgeRanges().size();
      }
      mapProjects.sortBy(new Comparator<MapProject>() {
        @Override
        public int compare(MapProject o1, MapProject o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return mapProjects;

    } catch (Exception e) {
      handleException(e, "trying to get the map projects for a given user",
          user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Users
  // ///////////////////////////////////////////////////

  /**
   * Returns all map leads in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map leads
   * @throws Exception the exception
   */
  @GET
  @Path("/user/users")
  @ApiOperation(value = "Get all mapping users.", notes = "Gets all map users.", response = MapUserListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapUserListJpa getMapUsers(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /user/users");
    String user = "";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map users.")
                .build());

      MapUserListJpa mapLeads = (MapUserListJpa) mappingService.getMapUsers();
      mapLeads.sortBy(new Comparator<MapUser>() {
        @Override
        public int compare(MapUser o1, MapUser o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return mapLeads;

    } catch (Exception e) {
      handleException(e, "trying to retrieve a concept", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns the scope concepts for map project.
   *
   * @param projectId the project id
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the scope concepts for map project
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{projectId}/scopeConcepts")
  @ApiOperation(value = "Get scope concepts for a map project.", notes = "Gets a (pageable) list of scope concepts for a map project", response = SearchResultListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getScopeConceptsForMapProject(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";

    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve scope concepts.")
            .build());

      MapProject mapProject = mappingService.getMapProject(projectId);

      SearchResultList results =
          mappingService
              .getScopeConceptsForMapProject(mapProject, pfsParameter);

      return results;

    } catch (Exception e) {
      this.handleException(e, "trying to retrieve scope concepts", user,
          projectName, "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds the scope concept to map project.
   *
   * @param terminologyId the terminology id
   * @param projectId the project id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{projectId}/scopeConcept/add")
  @ApiOperation(value = "Adds a single scope concept to a map project.", notes = "Adds a single scope concept to a map project.", response = Response.class)
  public void addScopeConceptToMapProject(
    @ApiParam(value = "Concept to add, e.g. 100073004", required = true) String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve scope concepts.")
            .build());

      MapProject mapProject = mappingService.getMapProject(projectId);

      mapProject.addScopeConcept(terminologyId);
      mappingService.updateMapProject(mapProject);
    } catch (Exception e) {
      this.handleException(e, "trying to add scope concept to project", user,
          projectName, "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds a list of scope concepts to map project.
   *
   * @param terminologyIds the terminology ids
   * @param projectId the project id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{projectId}/scopeConcepts/add")
  @ApiOperation(value = "Adds a list of scope concepts to a map project.", notes = "Adds a list of scope concepts to a map project.", response = Response.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void addScopeConceptsToMapProject(
    @ApiParam(value = "List of concepts to add, e.g. {'100073004', '100075006'", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve scope concepts.")
            .build());

      MapProject mapProject = mappingService.getMapProject(projectId);

      for (String terminologyId : terminologyIds) {
        mapProject.addScopeConcept(terminologyId);
      }
      mappingService.updateMapProject(mapProject);

    } catch (Exception e) {
      this.handleException(e, "trying to add scope concept to project", user,
          projectName, "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a single scope concept from map project.
   *
   * @param terminologyId the terminology id
   * @param projectId the project id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{projectId}/scopeConcept/remove")
  @ApiOperation(value = "Removes a single scope concept from a map project.", notes = "Removes a single scope concept from a map project.", response = Response.class)
  public void removeScopeConceptFromMapProject(
    @ApiParam(value = "Concept to remove, e.g. 100075006", required = true) String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove scope concepts.")
            .build());

      MapProject mapProject = mappingService.getMapProject(projectId);

      mapProject.removeScopeConcept(terminologyId);
      mappingService.updateMapProject(mapProject);

    } catch (Exception e) {
      this.handleException(e, "trying to remove scope concept from project",
          user, projectName, "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes the scope concept from map project.
   *
   * @param terminologyIds the terminology ids
   * @param projectId the project id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{projectId}/scopeConcepts/remove")
  @ApiOperation(value = "Removes a list of scope concepts from a map project.", notes = "Removes a list of scope concept from a map project.", response = Response.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void removeScopeConceptsFromMapProject(
    @ApiParam(value = "List of concepts to remove, e.g. {'100073004', '100075006'", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";

    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove scope concepts.")
            .build());

      MapProject mapProject = mappingService.getMapProject(projectId);
      for (String terminologyId : terminologyIds) {
        mapProject.removeScopeConcept(terminologyId);
      }
      mappingService.updateMapProject(mapProject);

    } catch (Exception e) {
      this.handleException(e, "trying to remove scope concept from project",
          user, projectName, "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns the scope excluded concepts for map project.
   *
   * @param projectId the project id
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the scope excluded concepts for map project
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts")
  @ApiOperation(value = "Get scope excluded concepts for a map project.", notes = "Gets a (pageable) list of scope excluded concepts for a map project", response = SearchResultListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getScopeExcludedConceptsForMapProject(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve scope excluded concepts.")
                .build());

      MapProject mapProject = mappingService.getMapProject(projectId);

      SearchResultList results =
          mappingService.getScopeExcludedConceptsForMapProject(mapProject,
              pfsParameter);

      return results;

    } catch (Exception e) {
      this.handleException(e, "trying to retrieve scope excluded concepts",
          user, projectName, "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds the scope excluded concept to map project.
   *
   * @param terminologyId the terminology id
   * @param projectId the project id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcept/add")
  @ApiOperation(value = "Adds a single scope excluded concept to a map project.", notes = "Adds a single scope excluded concept to a map project.", response = Response.class)
  public void addScopeExcludedConceptToMapProject(
    @ApiParam(value = "Concept to add, e.g. 100073004", required = true) String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcepts/add");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve scope excluded concepts.")
                .build());

      MapProject mapProject = mappingService.getMapProject(projectId);

      mapProject.addScopeExcludedConcept(terminologyId);
      mappingService.updateMapProject(mapProject);

    } catch (Exception e) {
      this.handleException(e,
          "trying to add scope excluded concept to project", user, projectName,
          "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds a list of scope excluded concepts to map project.
   *
   * @param terminologyIds the terminology ids
   * @param projectId the project id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts/add")
  @ApiOperation(value = "Adds a list of scope excluded concepts to a map project.", notes = "Adds a list of scope excluded concepts to a map project.", response = Response.class)
  public void addScopeExcludedConceptsToMapProject(
    @ApiParam(value = "List of concepts to add, e.g. {'100073004', '100075006'", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcepts/add");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve scope excluded concepts.")
                .build());

      MapProject mapProject = mappingService.getMapProject(projectId);

      for (String terminologyId : terminologyIds) {
        mapProject.addScopeExcludedConcept(terminologyId);
      }
      mappingService.updateMapProject(mapProject);

    } catch (Exception e) {
      this.handleException(e,
          "trying to add scope excluded concept to project", user, projectName,
          "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a single scope excluded concept from map project.
   *
   * @param terminologyId the terminology id
   * @param projectId the project id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcept/remove")
  @ApiOperation(value = "Removes a single scope excluded concept from a map project.", notes = "Removes a single scope excluded concept from a map project.", response = Response.class)
  public void removeScopeExcludedConceptFromMapProject(
    @ApiParam(value = "Concept to remove, e.g. 100075006", required = true) String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcept/remove");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";

    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to remove scope excluded concepts.")
                .build());

      MapProject mapProject = mappingService.getMapProject(projectId);

      mapProject.removeScopeExcludedConcept(terminologyId);
      mappingService.updateMapProject(mapProject);

    } catch (Exception e) {
      this.handleException(e,
          "trying to remove scope excluded concept from project", user,
          projectName, "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a list of scope excluded concepts from map project.
   *
   * @param terminologyIds the terminology ids
   * @param projectId the project id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts/remove")
  @ApiOperation(value = "Removes a list of scope excluded concepts from a map project.", notes = "Removes a list of scope excluded concept from a map project.", response = Response.class)
  public void removeScopeExcludedConceptsFromMapProject(
    @ApiParam(value = "List of concepts to remove, e.g. {'100073004', '100075006'", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcepts/remove");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to remove scope excluded concepts.")
                .build());

      MapProject mapProject = mappingService.getMapProject(projectId);
      for (String terminologyId : terminologyIds) {
        mapProject.removeScopeExcludedConcept(terminologyId);
      }
      mappingService.updateMapProject(mapProject);

    } catch (Exception e) {
      this.handleException(e,
          "trying to remove scope excluded concept from project", user,
          projectName, "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns the user for a given id (auto-generated) in JSON format.
   *
   * @param mapUserName the map user name
   * @param authToken the auth token
   * @return the mapUser
   * @throws Exception the exception
   */
  @GET
  @Path("/user/id/{username}")
  @ApiOperation(value = "Get user by username.", notes = "Gets a user by a username.", response = MapUser.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapUser getMapUser(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String mapUserName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): user/id/" + mapUserName);
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to retrieve a map user.")
            .build());

      MapUser mapUser = mappingService.getMapUser(mapUserName);
      return mapUser;

    } catch (Exception e) {
      handleException(e, "trying to retrieve a map user", mapUserName, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds a map user.
   *
   * @param mapUser the map user
   * @param authToken the auth token
   * @return Response the response
   * @throws Exception the exception
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/user/add")
  @ApiOperation(value = "Add a user.", notes = "Adds the specified user.", response = MapUserJpa.class)
  public MapUser addMapUser(
    @ApiParam(value = "User, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /user/add");
    String user = "";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add a user.").build());

      mappingService.addMapUser(mapUser);
      return mapUser;

    } catch (Exception e) {
      handleException(e, "trying to add a user", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Updates a map user.
   *
   * @param mapUser the map user to be added
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/user/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a user.", notes = "Updates the specified user.", response = MapUserJpa.class)
  public void updateMapUser(
    @ApiParam(value = "User, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /user/update");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to update a user.").build());

      mappingService.updateMapUser(mapUser);

    } catch (Exception e) {
      handleException(e, "trying to update a user", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a map user.
   *
   * @param mapUser the map user
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @DELETE
  @Path("/user/delete")
  @ApiOperation(value = "Remove a user.", notes = "Removes the specified user.", response = MapUser.class)
  public void removeMapUser(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /user/delete for user " + mapUser.getName());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove a user.").build());

      mappingService.removeMapUser(mapUser.getId());

    } catch (Exception e) {
      handleException(e, "trying to remove a user", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Advice
  // ///////////////////////////////////////////////////
  /**
   * Returns all map advices in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map advices
   * @throws Exception the exception
   */
  @GET
  @Path("/advice/advices")
  @ApiOperation(value = "Get all mapping advices.", notes = "Gets a list of all map advices.", response = MapAdviceListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapAdviceListJpa getMapAdvices(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /advice/advices");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve the map advices.")
            .build());

      MapAdviceListJpa mapAdvices =
          (MapAdviceListJpa) mappingService.getMapAdvices();
      mapAdvices.sortBy(new Comparator<MapAdvice>() {
        @Override
        public int compare(MapAdvice o1, MapAdvice o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return mapAdvices;

    } catch (Exception e) {
      handleException(e, "trying to retrieve an advice", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds a map advice.
   *
   * @param mapAdvice the map advice
   * @param authToken the auth token
   * @return Response the response
   * @throws Exception the exception
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/advice/add")
  @ApiOperation(value = "Add an advice.", notes = "Adds the specified map advice.", response = MapAdviceJpa.class)
  public MapAdvice addMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data", required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /advice/add");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add an advice.").build());

      mappingService.addMapAdvice(mapAdvice);
      return mapAdvice;

    } catch (Exception e) {
      handleException(e, "trying to add an advice", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Updates a map advice.
   *
   * @param mapAdvice the map advice to be added
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/advice/update")
  @ApiOperation(value = "Update an advice.", notes = "Updates the specified advice.", response = MapAdviceJpa.class)
  public void updateMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data", required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /advice/update");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to update an advice.")
            .build());

      mappingService.updateMapAdvice(mapAdvice);
    } catch (Exception e) {
      handleException(e, "trying to update an advice", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a map advice.
   *
   * @param mapAdvice the map advice
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @DELETE
  @Path("/advice/delete")
  @ApiOperation(value = "Remove an advice.", notes = "Removes the specified map advice.", response = MapAdviceJpa.class)
  public void removeMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data", required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /advice/delete for user "
            + mapAdvice.getName());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove a user.").build());

      mappingService.removeMapAdvice(mapAdvice.getId());
    } catch (Exception e) {
      LocalException le =
          new LocalException(
              "Unable to delete map advice. This is likely because the advice is being used by a map project or map entry");
      handleException(le, "", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map AgeRange
  // ///////////////////////////////////////////////////
  /**
   * Returns all map age ranges in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map age ranges
   * @throws Exception the exception
   */
  @GET
  @Path("/ageRange/ageRanges")
  @ApiOperation(value = "Get all mapping age ranges.", notes = "Gets a list of all mapping age ranges.", response = MapAgeRangeListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapAgeRangeListJpa getMapAgeRanges(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /ageRange/ageRanges");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map age ranges.")
                .build());

      MapAgeRangeListJpa mapAgeRanges =
          (MapAgeRangeListJpa) mappingService.getMapAgeRanges();
      mapAgeRanges.sortBy(new Comparator<MapAgeRange>() {
        @Override
        public int compare(MapAgeRange o1, MapAgeRange o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return mapAgeRanges;

    } catch (Exception e) {
      handleException(e, "trying to retrieve an age range", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds a map age range.
   *
   * @param mapAgeRange the map ageRange
   * @param authToken the auth token
   * @return Response the response
   * @throws Exception the exception
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/ageRange/add")
  @ApiOperation(value = "Add an age range.", notes = "Adds the specified age range.", response = MapAgeRangeJpa.class)
  public MapAgeRange addMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data", required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /ageRange/add");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add an age range.")
            .build());

      mappingService.addMapAgeRange(mapAgeRange);
      return mapAgeRange;

    } catch (Exception e) {
      handleException(e, "trying to add an age range", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Updates a map age range.
   *
   * @param mapAgeRange the map ageRange to be added
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/ageRange/update")
  @ApiOperation(value = "Update an age range.", notes = "Updates the specified age range.", response = MapAgeRangeJpa.class)
  public void updateMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data", required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /ageRange/update");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to update an age range.")
            .build());

      mappingService.updateMapAgeRange(mapAgeRange);
    } catch (Exception e) {
      handleException(e, "trying to update an age range", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a map age range.
   *
   * @param mapAgeRange the map age range
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @DELETE
  @Path("/ageRange/delete")
  @ApiOperation(value = "Remove an age range.", notes = "Removes the specified age range.", response = MapAgeRangeJpa.class)
  public void removeMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data", required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /ageRange/delete for user "
            + mapAgeRange.getName());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove an age range.")
            .build());

      mappingService.removeMapAgeRange(mapAgeRange.getId());
    } catch (Exception e) {
      handleException(e, "trying to remove an age range", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Relation
  // ///////////////////////////////////////////////////

  /**
   * Returns all map relations in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map relations
   * @throws Exception the exception
   */
  @GET
  @Path("/relation/relations")
  @ApiOperation(value = "Get all relations.", notes = "Gets a list of all map relations.", response = MapRelationListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRelationListJpa getMapRelations(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /relation/relations");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to return the map relations.")
            .build());

      MapRelationListJpa mapRelations =
          (MapRelationListJpa) mappingService.getMapRelations();
      mapRelations.sortBy(new Comparator<MapRelation>() {
        @Override
        public int compare(MapRelation o1, MapRelation o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return mapRelations;
    } catch (Exception e) {
      handleException(e, "trying to return the map relations", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds a map relation.
   *
   * @param mapRelation the map relation
   * @param authToken the auth token
   * @return Response the response
   * @throws Exception the exception
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/relation/add")
  @ApiOperation(value = "Add a map relation.", notes = "Adds the specified map relation.", response = MapRelationJpa.class)
  public MapRelation addMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data", required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /relation/add");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add a relation.")
            .build());

      mappingService.addMapRelation(mapRelation);
      return mapRelation;

    } catch (Exception e) {
      handleException(e, "trying to add a relation", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Updates a map relation.
   *
   * @param mapRelation the map relation to be added
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/relation/update")
  @ApiOperation(value = "Update a map relation.", notes = "Updates the specified map relation.", response = MapRelationJpa.class)
  public void updateMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data", required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /relation/update");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to update a relation.")
            .build());

      mappingService.updateMapRelation(mapRelation);
    } catch (Exception e) {
      handleException(e, "trying to update a relation", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a map relation.
   *
   * @param mapRelation the map relation
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @DELETE
  @Path("/relation/delete")
  @ApiOperation(value = "Remove a map relation.", notes = "Removes the specified map relation.", response = MapRelationJpa.class)
  public void removeMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data", required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /relation/delete for user "
            + mapRelation.getName());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove a relation.")
            .build());

      mappingService.removeMapRelation(mapRelation.getId());
    } catch (Exception e) {
      LocalException le =
          new LocalException(
              "Unable to delete map relation. This is likely because the relation is being used by a map project or map entry");
      handleException(le, "", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Principles
  // ///////////////////////////////////////////////////

  /**
   * Returns all map principles in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map principles
   * @throws Exception the exception
   */
  @GET
  @Path("/principle/principles")
  @ApiOperation(value = "Get all map principles.", notes = "Gets a list of all map principles.", response = MapPrincipleListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapPrincipleListJpa getMapPrinciples(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /principle/principles");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to return the map principles.")
            .build());

      MapPrincipleListJpa mapPrinciples =
          (MapPrincipleListJpa) mappingService.getMapPrinciples();
      mapPrinciples.sortBy(new Comparator<MapPrinciple>() {
        @Override
        public int compare(MapPrinciple o1, MapPrinciple o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return mapPrinciples;
    } catch (Exception e) {
      handleException(e, "trying to return the map principles", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns the map principle for id.
   *
   * @param mapPrincipleId the map principle id
   * @param authToken the auth token
   * @return the map principle for id
   * @throws Exception the exception
   */
  @GET
  @Path("/principle/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get a map principle.", notes = "Gets a map principle for the specified id.", response = MapPrinciple.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapPrinciple getMapPrinciple(
    @ApiParam(value = "Map principle identifer", required = true) @PathParam("id") Long mapPrincipleId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /principle/id/" + mapPrincipleId.toString());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map principle.")
                .build());

      MapPrinciple mapPrinciple =
          mappingService.getMapPrinciple(mapPrincipleId);
      return mapPrinciple;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the map principle", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds a map user principle object.
   *
   * @param mapPrinciple the map user principle object to be added
   * @param authToken the auth token
   * @return result the newly created map user principle object
   * @throws Exception the exception
   */
  @PUT
  @Path("/principle/add")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Add a map principle.", notes = "Adds the specified map principle.", response = MapPrincipleJpa.class)
  public MapPrinciple addMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data", required = true) MapPrincipleJpa mapPrinciple,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /principle/add");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add a map principle.")
            .build());

      MapPrinciple result = mappingService.addMapPrinciple(mapPrinciple);
      return result;
    } catch (Exception e) {
      handleException(e, "trying to add a map principle", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /**
   * Updates a map principle.
   *
   * @param mapPrinciple the map principle
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/principle/update")
  @ApiOperation(value = "Update a map principle.", notes = "Updates the specified map principle.", response = MapPrincipleJpa.class)
  public void updateMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data", required = true) MapPrincipleJpa mapPrinciple,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /principle/update");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to update a map principle.")
                .build());

      mappingService.updateMapPrinciple(mapPrinciple);

    } catch (Exception e) {
      handleException(e, "trying to update a map principle", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a set of map user preferences.
   *
   * @param principle the principle
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @DELETE
  @Path("/principle/delete")
  @ApiOperation(value = "Remove a map principle.", notes = "Removes the specified map principle.")
  public void removeMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data", required = true) MapPrincipleJpa principle,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /principle/delete for id "
            + principle.getId().toString());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to remove a map principle.")
                .build());

      mappingService.removeMapPrinciple(principle.getId());

    } catch (Exception e) {
      LocalException le =
          new LocalException(
              "Unable to delete map principle. This is likely because the principle is being used by a map project or map record");
      handleException(le, "", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map User Preferences
  // ///////////////////////////////////////////////////

  /**
   * Gets a map user preferences object for a specified user.
   *
   * @param username the username
   * @param authToken the auth token
   * @return result the newly created map user preferences object
   * @throws Exception the exception
   */
  @GET
  @Path("/userPreferences/user/id/{username}")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get user preferences.", notes = "Gets user preferences for the specified username.", response = MapUserPreferencesJpa.class)
  public MapUserPreferences getMapUserPreferences(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String username,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call:  /userPreferences/user/id/" + username);

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to return the map user preferences.")
                .build());

      MapUserPreferences result =
          mappingService.getMapUserPreferences(username);

      return result;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the map user preferences", user,
          "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds a map user preferences object.
   *
   * @param mapUserPreferences the map user preferences object to be added
   * @param authToken the auth token
   * @return result the newly created map user preferences object
   * @throws Exception the exception
   */
  @PUT
  @Path("/userPreferences/add")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Add user preferences.", notes = "Adds the specified user preferences.", response = MapUserPreferencesJpa.class)
  public MapUserPreferences addMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data", required = true) MapUserPreferencesJpa mapUserPreferences,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /userPreferences/add");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call -- note that any user must be able to add their
      // own preferences, therefore this /add call only requires VIEWER
      // role
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to add map user preferences.")
            .build());

      MapUserPreferences result =
          mappingService.addMapUserPreferences(mapUserPreferences);

      return result;
    } catch (Exception e) {
      handleException(e, "trying to add map user preferences", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /**
   * Updates a map user preferences object.
   *
   * @param mapUserPreferences the map user preferences
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/userPreferences/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update user preferences.", notes = "Updates the specified user preferences.", response = MapUserPreferencesJpa.class)
  public void updateMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data", required = true) MapUserPreferencesJpa mapUserPreferences,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /userPreferences/update with \n"
            + mapUserPreferences.toString());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call -- note that any user must be able to update their
      // own preferences, therefore this /update call only requires VIEWER
      // role
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to update map user preferences.")
                .build());

      mappingService.updateMapUserPreferences(mapUserPreferences);

    } catch (Exception e) {
      handleException(e, "trying to update map user preferences", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /**
   * Removes a set of map user preferences.
   *
   * @param mapUserPreferences the id of the map user preferences object to be
   *          deleted
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @DELETE
  @Path("/userPreferences/remove")
  @ApiOperation(value = "Remove user preferences.", notes = "Removes specified user preferences.")
  public void removeMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data", required = true) MapUserPreferencesJpa mapUserPreferences,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /userPreferences/remove for id "
            + mapUserPreferences.getId().toString());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call -- note that any user must be able to delete their
      // own preferences, therefore this /remove call only requires VIEWER
      // role
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to remove map user preferences.")
                .build());

      mappingService.removeMapUserPreferences(mapUserPreferences.getId());

    } catch (Exception e) {
      handleException(e, "trying to remove map user preferences", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Record
  // ///////////////////////////////////////////////////

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Record
  // ///////////////////////////////////////////////////

  /**
   * Returns the record for a given id (auto-generated) in JSON format.
   *
   * @param mapRecordId the mapRecordId
   * @param authToken the auth token
   * @return the mapRecord
   * @throws Exception the exception
   */
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get map record by id.", notes = "Gets a map record for the specified id.", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecord getMapRecord(
    @ApiParam(value = "Map record id", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/id/"
            + (mapRecordId == null ? "" : mapRecordId.toString()));

    String user = "";
    MapRecord mapRecord = null;
    MappingService mappingService = new MappingServiceJpa();
    try {

      mapRecord = mappingService.getMapRecord(mapRecordId);

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve the map record.")
            .build());

      // remove notes if this is not a specialist or above
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        mapRecord.setMapNotes(null);
      }

      return mapRecord;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the map record", user,
          mapRecord == null ? "" : mapRecord.getMapProjectId().toString(),
          mapRecordId == null ? "" : mapRecordId.toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Adds a map record.
   *
   * @param mapRecord the map record to be added
   * @param authToken the auth token
   * @return Response the response
   * @throws Exception the exception
   */
  @PUT
  @Path("/record/add")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Add a map record.", notes = "Adds the specified map record.", response = MapRecordJpa.class)
  public MapRecord addMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/add");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add a map record.")
            .build());

      MapRecord result = mappingService.addMapRecord(mapRecord);
      return result;
    } catch (Exception e) {
      handleException(e, "trying to add a map record", user, mapRecord
          .getMapProjectId().toString(), mapRecord.getId().toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Updates a map record.
   *
   * @param mapRecord the map record to be added
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/record/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a map record.", notes = "Updates the specified map record.", response = Response.class)
  public void updateMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/update");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to update the map record.")
            .build());

      mappingService.updateMapRecord(mapRecord);

    } catch (Exception e) {
      handleException(e, "trying to update the map record", user, mapRecord
          .getMapProjectId().toString(), mapRecord.getId().toString());
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a map record given the object.
   *
   * @param mapRecord the map record to delete
   * @param authToken the auth token
   * @return Response the response
   * @throws Exception the exception
   */
  @DELETE
  @Path("/record/delete")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Remove a map record.", notes = "Removes the specified map record.", response = MapRecordJpa.class)
  public Response removeMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/delete with map record id = "
            + mapRecord.toString());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to delete the map record.")
            .build());

      mappingService.removeMapRecord(mapRecord.getId());

      return null;
    } catch (Exception e) {
      handleException(e, "trying to delete the map record", user, mapRecord
          .getMapProjectId().toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Removes a set of map records for a project and a set of terminology ids.
   *
   * @param terminologyIds the terminology ids
   * @param projectId the project id
   * @param authToken the auth token
   * @return Response the response
   * @throws Exception the exception
   */
  @DELETE
  @Path("/record/records/delete/project/id/{projectId}/batch")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Remove a set of map records.", notes = "Removes map records for specified project and a set of concept terminology ids", response = List.class)
  public ValidationResult removeMapRecordsForMapProjectAndTerminologyIds(
    @ApiParam(value = "Terminology ids, in JSON or XML POST data", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/records/delete/project/id/"
            + projectId + "/batch with string argument " + terminologyIds);

    String user = "";
    String projectName = "(not retrieved)";
    // instantiate the needed services
    ContentService contentService = new ContentServiceJpa();
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to delete the map record.")
            .build());

      // validation report to return errors and warnings
      ValidationResult validationResult = new ValidationResultJpa();

      // retrieve the map project
      MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();

      // construct a list of terminology ids to remove
      // initially set to the Api argument
      // (instantiated to avoid concurrent modification errors
      // when modifying the list for descendant concepts)
      List<String> terminologyIdsToRemove = new ArrayList<>(terminologyIds);

      int nRecordsRemoved = 0;
      int nScopeConceptsRemoved = 0;

      validationResult.addMessage(terminologyIds.size()
          + " concepts selected for map record removal");

      // cycle over the terminology ids
      for (String terminologyId : terminologyIdsToRemove) {

        // retrieve all map records for this project and concept
        MapRecordList mapRecordList =
            mappingService.getMapRecordsForProjectAndConcept(projectId,
                terminologyId);

        // check if map records exist
        if (mapRecordList.getCount() == 0) {
          Logger.getLogger(MappingServiceRest.class).warn(
              "No records found for project for concept id " + terminologyId);
          validationResult.addWarning("No records found for concept "
              + terminologyId);
        } else {
          for (MapRecord mapRecord : mapRecordList.getMapRecords()) {
            Logger.getLogger(MappingServiceRest.class).info(
                "Removing map record " + mapRecord.getId() + " for concept "
                    + mapRecord.getConceptId() + ", "
                    + mapRecord.getConceptName());

            // remove the map record
            mappingService.removeMapRecord(mapRecord.getId());

            // increment the counts
            nRecordsRemoved++;
          }
        }

        // if a non-descendant-based project (i.e. enumerated scope), remove
        // scope concept
        if (!mapProject.isScopeDescendantsFlag()) {

          // remove this terminology id from the scope concepts
          if (mapProject.getScopeConcepts().contains(terminologyId)) {
            mapProject.removeScopeConcept(terminologyId);
            nScopeConceptsRemoved++;
          }

          // update the map project
          mappingService.updateMapProject(mapProject);

        }

      }

      // add the counter information to the validation result
      validationResult.addMessage(nRecordsRemoved
          + " records successfully removed");

      // if scope concepts were removed, add a success message
      if (!mapProject.isScopeDescendantsFlag()) {

        validationResult.addMessage(nScopeConceptsRemoved
            + " concepts removed from project scope definition");
      }
      // close the services and return the validation result
      return validationResult;

    } catch (Exception e) {
      handleException(e, "trying to delete map records by terminology id",
          user, terminologyIds.toString(), projectName);
      return null;
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
  }

  /**
   * Returns the records for a given concept id. We don't need to know
   * terminology or version here because we can get it from the corresponding
   * map project.
   *
   * @param conceptId the concept id
   * @param authToken the auth token
   * @return the mapRecords
   * @throws Exception the exception
   */
  @GET
  @Path("/record/concept/id/{conceptId}")
  @ApiOperation(value = "Get map records by concept id.", notes = "Gets a list of map records for the specified concept id.", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsForConceptId(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/concept/id/" + conceptId);

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole applicationRole =
          securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);

      if (!applicationRole.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to find records by the given concept id.")
                .build());

      MapRecordListJpa mapRecordList =
          (MapRecordListJpa) mappingService.getMapRecordsForConcept(conceptId);

      // return records that this user does not have permission to see
      MapUser mapUser =
          mappingService.getMapUser(securityService
              .getUsernameForToken(authToken));
      List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over records and determine if this user can see them
      for (MapRecord mr : mapRecordList.getMapRecords()) {

        // get the user's role for this record's project
        MapUserRole projectRole =
            securityService.getMapProjectRoleForToken(authToken,
                mr.getMapProjectId());

        // remove notes if this is not a specialist or above
        if (!projectRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }

        // System.out.println(projectRole + " " + mr.toString());

        switch (mr.getWorkflowStatus()) {

        // any role can see published
          case PUBLISHED:
            mapRecords.add(mr);
            break;

          // only roles above specialist can see ready_for_publication
          case READY_FOR_PUBLICATION:
            if (projectRole.hasPrivilegesOf(MapUserRole.SPECIALIST))
              mapRecords.add(mr);
            break;
          // otherwise
          // - if lead, add record
          // - if specialist, only add record if owned
          default:
            if (projectRole.hasPrivilegesOf(MapUserRole.LEAD))
              mapRecords.add(mr);
            else if (mr.getOwner().equals(mapUser))
              mapRecords.add(mr);
            break;

        }
      }

      // set the list of records to the filtered object and return
      mapRecordList.setMapRecords(mapRecords);

      return mapRecordList;
    } catch (Exception e) {
      handleException(e, "trying to find records by the given concept id",
          user, "", conceptId);
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Gets the latest map record revision for each map record with given concept
   * id.
   *
   * @param conceptId the concept id
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the map records for concept id historical
   * @throws Exception the exception
   */
  @GET
  @Path("/record/concept/id/{conceptId}/project/id/{id:[0-9][0-9]*}/historical")
  @ApiOperation(value = "Get historical map records by concept id.", notes = "Gets the latest map record revision for each map record with given concept id.", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsForConceptIdHistorical(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/concept/id/" + conceptId
            + "/project/id/" + mapProjectId + "/historical");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole applicationRole =
          securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);

      if (!applicationRole.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to find historical records by the given concept id.")
                .build());

      MapRecordListJpa mapRecordList =
          (MapRecordListJpa) mappingService.getMapRecordRevisionsForConcept(
              conceptId, mapProjectId);

      // return records that this user does not have permission to see
      MapUser mapUser =
          mappingService.getMapUser(securityService
              .getUsernameForToken(authToken));
      List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over records and determine if this user can see them
      for (MapRecord mr : mapRecordList.getMapRecords()) {

        // get the user's role for this record's project
        MapUserRole projectRole =
            securityService.getMapProjectRoleForToken(authToken,
                mr.getMapProjectId());

        // remove notes if this is not a specialist or above
        if (!projectRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }

        // System.out.println(projectRole + " " + mr.toString());

        switch (mr.getWorkflowStatus()) {

        // any role can see published
          case PUBLISHED:
            mapRecords.add(mr);
            break;

          // only roles above specialist can see ready_for_publication
          case READY_FOR_PUBLICATION:
            if (projectRole.hasPrivilegesOf(MapUserRole.SPECIALIST))
              mapRecords.add(mr);
            break;
          // otherwise
          // - if lead, add record
          // - if specialist, only add record if owned
          default:
            if (projectRole.hasPrivilegesOf(MapUserRole.LEAD))
              mapRecords.add(mr);
            else if (mr.getOwner().equals(mapUser))
              mapRecords.add(mr);
            break;

        }
      }

      // set the list of records to the filtered object and return
      mapRecordList.setMapRecords(mapRecords);

      return mapRecordList;
    } catch (Exception e) {
      handleException(e,
          "trying to find historical records by the given concept id", user,
          "", conceptId);
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns delimited page of Published or Ready For Publication MapRecords
   * given a paging/filtering/sorting parameters object.
   *
   * @param mapProjectId the map project id
   * @param pfsParameter the JSON object containing the paging/filtering/sorting
   *          parameters
   * @param authToken the auth token
   * @return the list of map records
   * @throws Exception the exception
   */
  @POST
  @Path("/record/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get published map records by project id.", notes = "Gets a list of map records for the specified map project id that have a workflow status of PUBLISHED or READY_FOR_PUBLICATION.", response = MapRecordListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @CookieParam(value = "userInfo")
  public MapRecordListJpa getPublishedAndReadyForPublicationMapRecordsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/project/id/" + mapProjectId.toString()
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    // execute the service call
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the publication-ready map records for a map project.")
                .build());

      MapRecordListJpa mapRecordList =
          (MapRecordListJpa) mappingService
              .getPublishedAndReadyForPublicationMapRecordsForMapProject(
                  mapProjectId, pfsParameter);

      for (MapRecord mr : mapRecordList.getMapRecords()) {
        // remove notes if this is not a specialist or above
        if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }
      }
      return mapRecordList;
    } catch (Exception e) {
      handleException(e,
          "trying to retrieve the map records for a map project", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /**
   * Returns delimited page of Published or Ready For Publication MapRecords
   * given a paging/filtering/sorting parameters object.
   *
   * @param mapProjectId the map project id
   * @param pfsParameter the JSON object containing the paging/filtering/sorting
   *          parameters
   * @param authToken the auth token
   * @return the list of map records
   * @throws Exception the exception
   */
  @POST
  @Path("/record/project/id/{id:[0-9][0-9]*}/published")
  @ApiOperation(value = "Get published map records by map project id.", notes = "Gets a list of map records for the specified map project id that have a workflow status of PUBLISHED.", response = MapRecordListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @CookieParam(value = "userInfo")
  public MapRecordListJpa getPublishedMapRecordsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/project/id/" + mapProjectId.toString()
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    // execute the service call
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map records for a map project.")
                .build());

      MapRecordListJpa mapRecordList =
          (MapRecordListJpa) mappingService
              .getPublishedMapRecordsForMapProject(mapProjectId, pfsParameter);

      for (MapRecord mr : mapRecordList.getMapRecords()) {
        // remove notes if this is not a specialist or above
        if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }
      }
      return mapRecordList;
    } catch (Exception e) {
      handleException(e,
          "trying to retrieve the map records for a map project", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /**
   * Returns the map record revisions.
   * 
   * NOTE: currently not called, but we are going to want to call this to do
   * history-related stuff thus it is anticipating the future dev and should be
   * kept.
   *
   * @param mapRecordId the map record id
   * @param authToken the auth token
   * @return the map record revisions
   * @throws Exception the exception
   */
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/revisions")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get map record revision history.", notes = "Gets a list of all revisions of a map record for the specified id.", response = MapRecordListJpa.class)
  public MapRecordList getMapRecordRevisions(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/id/" + mapRecordId + "/revisions");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapRecordId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map record revisions.")
                .build());

      MapRecordList revisions =
          mappingService.getMapRecordRevisions(mapRecordId);

      for (MapRecord mr : revisions.getMapRecords()) {
        // remove notes if this is not a specialist or above
        if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }
      }
      return revisions;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the map record revisions", user,
          "", mapRecordId.toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /**
   * Returns the map record using historical revisions if the record no longer
   * exists.
   *
   * @param mapRecordId the map record id
   * @param authToken the auth token
   * @return the map record historical
   * @throws Exception the exception
   */
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/historical")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get latest state of a map record.", notes = "Gets the current form of the map record or its last historical state for the specified map record id.", response = MapRecordListJpa.class)
  public MapRecord getMapRecordHistorical(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/id/" + mapRecordId + "/historical");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map record potentially using historical revisions.")
                .build());

      // try getting the current record
      MapRecord mapRecord = mappingService.getMapRecord(mapRecordId);

      // if no current record, look for revisions
      if (mapRecord == null) {
        mapRecord =
            mappingService.getMapRecordRevisions(mapRecordId).getMapRecords()
                .get(0);
      }

      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        mapRecord.setMapNotes(null);
      }

      return mapRecord;

    } catch (Exception e) {
      handleException(
          e,
          "trying to retrieve the map record potentially using historical revisions",
          user, "", mapRecordId.toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  // //////////////////////////////////////////
  // Relation and Advice Computation
  // /////////////////////////////////////////

  /**
   * Computes a map relation (if any) for a map entry's current state.
   *
   * @param mapEntry the map entry
   * @param authToken the auth token
   * @return Response the response
   * @throws Exception the exception
   */
  @POST
  @Path("/relation/compute")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Compute map relation.", notes = "Gets the computed map relation for the specified map entry.", response = MapRelationJpa.class)
  public MapRelation computeMapRelation(
    @ApiParam(value = "Map entry, in JSON or XML POST data", required = true) MapEntryJpa mapEntry,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /relation/compute");

    String user = "";
    MapRecord mapRecord = null;
    MappingService mappingService = new MappingServiceJpa();
    try {

      // after deserialization, the entry has a dummy map record with id
      // get the actual record
      mapRecord = mappingService.getMapRecord(mapEntry.getMapRecord().getId());
      Logger.getLogger(MappingServiceRest.class).info(
          "  mapEntry.mapRecord.mapProjectId = " + mapRecord.getMapProjectId());

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to compute the map relation.")
            .build());
      }

      // System.out.println(mapRecord.toString());
      if (mapRecord.getMapProjectId() == null) {
        return null;
      }
      // System.out.println("Retrieving project handler");

      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mappingService
              .getMapProject(mapRecord.getMapProjectId()));

      MapRelation mapRelation =
          algorithmHandler.computeMapRelation(mapRecord, mapEntry);
      return mapRelation;

    } catch (Exception e) {
      handleException(e, "trying to compute the map relations", user,
          mapRecord == null ? "" : mapRecord.getMapProjectId().toString(),
          mapRecord == null ? "" : mapRecord.getId().toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Computes a map advice (if any) for a map entry's current state.
   *
   * @param mapEntry the map entry
   * @param authToken the auth token
   * @return Response the response
   * @throws Exception the exception
   */
  @POST
  @Path("/advice/compute")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Compute map advices.", notes = "Gets the computed map advices for the specified map entry.", response = MapAdviceJpa.class)
  public MapAdviceList computeMapAdvice(
    @ApiParam(value = "Map entry, in JSON or XML POST data", required = true) MapEntryJpa mapEntry,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // call log
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /advice/compute");

    String user = "";
    MapRecord mapRecord = null;
    MappingService mappingService = new MappingServiceJpa();
    try {

      // after deserialization, the entry has a dummy map record with id
      // get the actual record
      mapRecord = mappingService.getMapRecord(mapEntry.getMapRecord().getId());
      Logger.getLogger(MappingServiceRest.class).info(
          "  mapEntry.mapRecord.mapProjectId = " + mapRecord.getMapProjectId());

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to compute the map advice.")
                .build());
      }

      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mappingService
              .getMapProject(mapRecord.getMapProjectId()));

      MapAdviceList mapAdviceList =
          algorithmHandler.computeMapAdvice(mapRecord, mapEntry);
      return mapAdviceList;

    } catch (Exception e) {
      handleException(e, "trying to compute the map advice", user,
          mapRecord == null ? "" : mapRecord.getMapProjectId().toString(),
          mapRecord == null ? "" : mapRecord.getId().toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////
  // Role Management Services
  // ///////////////////////////////////////////////
  /**
   * Gets a map user's role for a given map project.
   *
   * @param username the username
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return result the role
   * @throws Exception the exception
   */
  @GET
  @Path("/userRole/user/id/{username}/project/id/{id:[0-9][0-9]*}")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get the user's role for a map project.", notes = "Gets the role for the specified user and map project.", response = SearchResultList.class)
  public MapUserRole getMapUserRoleForMapProject(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String username,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call:  /userRole/user/id" + username + "/project/id/"
            + mapProjectId);

    MappingService mappingService = new MappingServiceJpa();
    try {
      MapUserRole mapUserRole =
          mappingService.getMapUserRoleForMapProject(username, mapProjectId);
      return mapUserRole;
    } catch (Exception e) {
      handleException(e, "trying to get the map user role for a map project",
          username, mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // /////////////////////////
  // Descendant services
  // /////////////////////////

  /**
   * TODO: Make this project specific
   * 
   * Given concept information, returns a ConceptList of descendant concepts
   * without associated map records.
   *
   * @param terminologyId the concept terminology id
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the ConceptList of unmapped descendants
   * @throws Exception the exception
   */
  @GET
  @Path("/concept/id/{terminologyId}/unmappedDescendants/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find unmapped descendants of a concept.", notes = "Gets a list of search results for concepts having unmapped descendants.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getUnmappedDescendantsForConcept(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("id") String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /concept/id/" + terminologyId + "/project/id/"
            + mapProjectId);

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve unmapped descendants for a concept.")
                .build());

      SearchResultList results =
          mappingService.findUnmappedDescendantsForConcept(terminologyId,
              mapProjectId, null);

      return results;

    } catch (Exception e) {
      handleException(e,
          "trying to retrieve unmapped descendants for a concept", user, "",
          terminologyId);
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // /////////////////////////////////////////////////////
  // Tree Position Routines for Terminology Browser
  // /////////////////////////////////////////////////////
  /**
   * Gets tree positions for concept.
   *
   * @param terminologyId the terminology id
   * @param mapProjectId the contextual project of this tree, used for
   *          determining valid codes
   * @param authToken the auth token
   * @return the search result list
   * @throws Exception the exception
   */
  @GET
  @Path("/treePosition/project/id/{mapProjectId}/concept/id/{terminologyId}")
  @ApiOperation(value = "Gets project-specific tree positions with desendants.", notes = "Gets a list of tree positions and their descendants for the specified parameters. Sets flags for valid targets and assigns any terminology notes based on project.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getTreePositionWithDescendantsForConceptAndMapProjecct(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken

  ) throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /treePosition/project/id/"
            + mapProjectId.toString() + "/concept/id/" + terminologyId);

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get the tree positions with descendants.")
                .build());

      MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // get the local tree positions from content service
      TreePositionList treePositions =
          contentService.getTreePositionsWithChildren(terminologyId,
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());
      contentService.computeTreePositionInformation(treePositions);

      // set the valid codes using mapping service

      mappingService.setTreePositionValidCodes(
          treePositions.getTreePositions(), mapProjectId);
      mappingService.setTreePositionTerminologyNotes(
          treePositions.getTreePositions(), mapProjectId);

      return treePositions;
    } catch (Exception e) {
      handleException(e, "trying to get the tree positions with descendants",
          user, mapProjectId.toString(), terminologyId);
      return null;
    } finally {
      contentService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Gets the root-level tree positions for a given terminology and version.
   *
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the search result list
   * @throws Exception the exception
   */
  @GET
  @Path("/treePosition/project/id/{projectId}")
  @ApiOperation(value = "Get root tree positions.", notes = "Gets a list of tree positions at the root of the terminology.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getDestinationRootTreePositionsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /treePosition/project/id/"
            + mapProjectId.toString());

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get the root tree positions for a terminology.")
                .build());

      // set the valid codes using mapping service
      MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // get the root tree positions from content service
      TreePositionList treePositions =
          contentService.getRootTreePositions(
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());
      contentService.computeTreePositionInformation(treePositions);

      mappingService.setTreePositionValidCodes(
          treePositions.getTreePositions(), mapProjectId);

      return treePositions;
    } catch (Exception e) {
      handleException(e,
          "trying to get the root tree positions for a terminology", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
  }

  /**
   * Gets tree positions for concept query.
   *
   * @param query the query
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the root-level trees corresponding to the query
   * @throws Exception the exception
   */
  @GET
  @Path("/treePosition/project/id/{projectId}/query/{query}")
  @ApiOperation(value = "Get tree positions for query.", notes = "Gets a list of tree positions for the specified parameters.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getTreePositionGraphsForQueryAndMapProject(
    @ApiParam(value = "Terminology browser query, e.g. 'cholera'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Mapping): /treePosition/project/id/" + mapProjectId);

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get the tree position graphs for a query.")
                .build());

      MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // get the tree positions from concept service
      TreePositionList treePositions =
          contentService.getTreePositionGraphForQuery(
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion(), query);
      contentService.computeTreePositionInformation(treePositions);

      // set the valid codes using mapping service

      mappingService.setTreePositionValidCodes(
          treePositions.getTreePositions(), mapProjectId);
      mappingService.setTreePositionTerminologyNotes(
          treePositions.getTreePositions(), mapProjectId);

      return treePositions;

    } catch (Exception e) {
      handleException(e, "trying to get the tree position graphs for a query",
          user, mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
  }

  // //////////////////////////////////////////////////
  // Workflow-related routines
  // /////////////////////////////////////////////////

  /**
   * Returns records recently edited for a project and user. Used by editedList
   * widget.
   *
   * @param mapProjectId the map project id
   * @param username the user name
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the recently edited map records
   * @throws Exception the exception
   */
  @POST
  @Path("/record/project/id/{id}/user/id/{username}/edited")
  @ApiOperation(value = "Get map records edited by a user.", notes = "Gets a list of map records for the specified map project and user.", response = MapRecordListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsEditedByMapUser(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") String mapProjectId,
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String username,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/project/id/" + mapProjectId
            + "/user/id" + username + "/edited");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, new Long(
              mapProjectId));
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get the recently edited map records.")
                .build());

      MapRecordListJpa recordList =
          (MapRecordListJpa) mappingService.getRecentlyEditedMapRecords(
              new Long(mapProjectId), username, pfsParameter);
      return recordList;
    } catch (Exception e) {
      handleException(e, "trying to get the recently edited map records", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns the map records that when compared lead to the specified conflict
   * record.
   * 
   * @param mapRecordId the map record id
   * @param authToken the auth token
   * @return map records in conflict for a given conflict lead record
   * @throws Exception the exception
   */
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/conflictOrigins")
  @ApiOperation(value = "Get specialist records for an assigned conflict or review record.", notes = "Gets a list of specialist map records corresponding to a lead conflict or review record.", response = MapRecordListJpa.class)
  public MapRecordList getOriginMapRecordsForConflict(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/id/" + mapRecordId
            + "/conflictOrigins");
    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      MapRecord mapRecord = mappingService.getMapRecord(mapRecordId);
      Logger.getLogger(MappingServiceRest.class).info(
          "  mapRecord.mapProjectId = " + mapRecord.getMapProjectId());

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      // needed at specialist level, so specialists can review on QA_PATH
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the origin map records for a conflict/review.")
                .build());

      MapRecordList records = new MapRecordListJpa();

      records = mappingService.getOriginMapRecordsForConflict(mapRecordId);

      return records;
    } catch (Exception e) {
      handleException(e,
          "trying to retrieve origin records for conflict/review", user, "",
          mapRecordId.toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  // //////////////////////////////////////////////
  // Map Record Validation and Compare Services
  // //////////////////////////////////////////////

  /**
   * Validates a map record.
   *
   * @param mapRecord the map record to be validated
   * @param authToken the auth token
   * @return Response the response
   * @throws Exception the exception
   */
  @POST
  @Path("/validation/record/validate")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Validate a map record.", notes = "Performs validation checks on a map record and returns the validation results.", response = MapRecordJpa.class)
  public ValidationResult validateMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /validation/record/validate for map record id = "
            + mapRecord.getId().toString());

    // get the map project for this record

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to validate a map record.")
            .build());

      MapProject mapProject;
      mapProject = mappingService.getMapProject(mapRecord.getMapProjectId());
      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);

      ValidationResult validationResult =
          algorithmHandler.validateRecord(mapRecord);
      return validationResult;
    } catch (Exception e) {
      handleException(e, "trying to validate a map record", user, mapRecord
          .getMapProjectId().toString(), mapRecord.getId().toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Compare map records and return differences.
   *
   * @param mapRecordId1 the map record id1
   * @param mapRecordId2 the map record id2
   * @param authToken the auth token
   * @return the validation result
   * @throws Exception the exception
   */
  @GET
  @Path("/validation/record/id/{recordId1}/record/id/{recordId2}/compare")
  @ApiOperation(value = "Compare two map records.", notes = "Compares two map records and returns the validation results.", response = ValidationResultJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ValidationResult compareMapRecords(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("recordId1") Long mapRecordId1,
    @ApiParam(value = "Map record id, e.g. 28124", required = true) @PathParam("recordId2") Long mapRecordId2,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /validation/record/id/" + mapRecordId1
            + "record/id/" + mapRecordId1 + "/compare");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {

      MapRecord mapRecord1, mapRecord2;

      mapRecord1 = mappingService.getMapRecord(mapRecordId1);
      mapRecord2 = mappingService.getMapRecord(mapRecordId2);

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord1.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to compare map records.")
            .build());

      MapProject mapProject =
          mappingService.getMapProject(mapRecord1.getMapProjectId());
      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      ValidationResult validationResult =
          algorithmHandler.compareMapRecords(mapRecord1, mapRecord2);

      return validationResult;

    } catch (Exception e) {
      handleException(e, "trying to compare map records", user, "",
          mapRecordId1.toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Is target code valid.
   *
   * @param mapProjectId the map project id
   * @param terminologyId the terminology id
   * @param authToken the auth token
   * @return the concept
   * @throws Exception the exception
   */
  @GET
  @Path("/project/id/{mapProjectId}/concept/{terminologyId}/isValid")
  @ApiOperation(value = "Indicate whether a target code is valid.", notes = "Gets either a valid concept corresponding to the id, or returns null if not valid.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Concept isTargetCodeValid(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/id/" + mapProjectId + "/concept/"
            + terminologyId + "/isValid");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    try {

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to check valid target codes")
            .build());

      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      boolean isValid = algorithmHandler.isTargetCodeValid(terminologyId);

      if (isValid) {
        Concept c =
            contentService.getConcept(terminologyId,
                mapProject.getDestinationTerminology(),
                mapProject.getDestinationTerminologyVersion());
        // Empty descriptions/relationships
        c.setDescriptions(new HashSet<Description>());
        c.setRelationships(new HashSet<Relationship>());
        return c;
      } else {
        return null;
      }

    } catch (Exception e) {
      handleException(e, "trying to compare map records", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      contentService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Upload file.
   *
   * @param fileInputStream the file input stream
   * @param contentDispositionHeader the content disposition header
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the response
   * @throws Exception the exception
   */
  @POST
  @Path("/upload/{mapProjectId}")
  // Swagger does not support this
  @ApiOperation(value = "Upload a mapping handbook file for a project.", notes = "Uploads a mapping handbook file for the specified project.", response = TreePositionListJpa.class)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadMappingHandbookFile(
    @FormDataParam("file") InputStream fileInputStream,
    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
    @PathParam("mapProjectId") Long mapProjectId,
    @HeaderParam("Authorization") String authToken) throws Exception {

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to check valid target codes")
            .build());

      // get destination directory for uploaded file
      Properties config = ConfigUtility.getConfigProperties();

      String docDir = config.getProperty("map.principle.source.document.dir");

      // make sure docDir ends with /doc - validation
      // mkdirs with project id in both dir and archiveDir

      File dir = new File(docDir);
      File archiveDir = new File(docDir + "/archive");

      File projectDir = new File(docDir, mapProjectId.toString());
      projectDir.mkdir();
      File archiveProjectDir = new File(archiveDir, mapProjectId.toString());
      archiveProjectDir.mkdir();

      // compose the name of the stored file
      MapProject mapProject =
          mappingService.getMapProject(new Long(mapProjectId));
      SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");
      String date = dt.format(new Date());

      String extension = "";
      if (contentDispositionHeader.getFileName().indexOf(".") != -1) {
        extension =
            contentDispositionHeader.getFileName().substring(
                contentDispositionHeader.getFileName().lastIndexOf("."));
      }
      String fileName =
          contentDispositionHeader
              .getFileName()
              .substring(0,
                  contentDispositionHeader.getFileName().lastIndexOf("."))
              .replaceAll(" ", "_");
      File file = new File(dir, mapProjectId + "/" + fileName + extension);
      File archiveFile =
          new File(archiveDir, mapProjectId + "/" + fileName + "." + date
              + extension);

      // save the file to the server
      saveFile(fileInputStream, file.getAbsolutePath());
      copyFile(file, archiveFile);

      // update project
      mapProject.setMapPrincipleSourceDocument(mapProjectId + "/" + fileName
          + extension);
      updateMapProject((MapProjectJpa) mapProject, authToken);

      return Response.status(200).entity(mapProjectId + "/" + file.getName())
          .build();
    } catch (Exception e) {
      handleException(e, "trying to upload a file", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns all map projects metadata.
   *
   * @param authToken the auth token
   * @return the map projects metadata
   * @throws Exception the exception
   */
  @GET
  @Path("/mapProject/metadata")
  @ApiOperation(value = "Get metadata for map projects.", notes = "Gets the key-value pairs representing all metadata for the map projects.", response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getMapProjectMetadata(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /mapProject/metadata");

    String user = "";
    MappingService mappingService = new MappingServiceJpa();
    try {
      user = securityService.getUsernameForToken(authToken);

      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map project metadata.")
                .build());

      // call jpa service and get complex map return type
      Map<String, Map<String, String>> mapOfMaps =
          mappingService.getMapProjectMetadata();

      // convert complex map to KeyValuePair objects for easy
      // transformation to
      // XML/JSON
      KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      for (Map.Entry<String, Map<String, String>> entry : mapOfMaps.entrySet()) {
        String metadataType = entry.getKey();
        Map<String, String> metadataPairs = entry.getValue();
        KeyValuePairList keyValuePairList = new KeyValuePairList();
        keyValuePairList.setName(metadataType);
        for (Map.Entry<String, String> pairEntry : metadataPairs.entrySet()) {
          KeyValuePair keyValuePair =
              new KeyValuePair(pairEntry.getKey().toString(),
                  pairEntry.getValue());
          keyValuePairList.addKeyValuePair(keyValuePair);
        }
        keyValuePairLists.addKeyValuePairList(keyValuePairList);
      }
      return keyValuePairLists;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the map project metadata", user,
          "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Save uploaded file to a defined location on the server.
   * 
   * @param uploadedInputStream the uploaded input stream
   * @param serverLocation the server location
   */
  @SuppressWarnings("static-method")
  private void saveFile(InputStream uploadedInputStream, String serverLocation) {
    OutputStream outputStream = null;
    try {
      outputStream = new FileOutputStream(new File(serverLocation));
      int read = 0;
      byte[] bytes = new byte[1024];
      outputStream.close();
      outputStream = new FileOutputStream(new File(serverLocation));
      while ((read = uploadedInputStream.read(bytes)) != -1) {
        outputStream.write(bytes, 0, read);
      }
      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      try {
        if (outputStream != null) {
          outputStream.close();
        }
      } catch (IOException e1) {
        // do nothing
      }
      e.printStackTrace();
    }

  }

  /**
   * Copy file.
   * 
   * @param sourceFile the source file
   * @param destFile the dest file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void copyFile(File sourceFile, File destFile)
    throws IOException {
    if (!destFile.exists()) {
      destFile.createNewFile();
    }
    FileChannel source = null;
    FileChannel destination = null;
    try {
      source = new FileInputStream(sourceFile).getChannel();
      destination = new FileOutputStream(destFile).getChannel();
      destination.transferFrom(source, 0, source.size());
    } finally {
      if (source != null) {
        source.close();
      }
      if (destination != null) {
        destination.close();
      }
    }
  }
}
