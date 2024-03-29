/**
 * Controller for Map Project Records view
 * 
 * Basic function: 1) Retrieve map project 2) Retrieve concept associated with
 * map project refsetid 3) Retrieve records
 * 
 * Scope functions (accessible from html) - $scope.resetSearch clears query and
 * launches unfiltered record retrieval request - $scope.retrieveRecords
 * launches new record retrieval request based on current
 * paging/filtering/sorting parameters - $scope.to_trusted converts unsafe html
 * into usable/displayable code
 * 
 * Helper functions: - setPagination sets the relevant pagination attributes
 * based on current page - getNRecords retrieves the total number of records
 * available given filtering parameters - getUnmappedDescendants retrieves the
 * unmapped descendants of a concept - applyMapCategoryStyle modifies map
 * entries for display based on MAP_CATEGORY_STYLE - applyRelationshipStyle
 * modifies map entries for display based on RELATIONSHIP_STYLE -
 * constructPfsParameterObj creates a PfsParameter object from current
 * paging/filtering/sorting parameters, consumed by RESTful service
 */

'use strict';

angular
  .module('mapProjectApp.widgets.projectRecords', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('projectRecords', {
      title : 'Project Records',
      description : 'Displays map records for a map project.',
      controller : 'projectRecordsCtrl',
      templateUrl : 'js/widgets/projectRecords/projectRecords.html',
      edit : {}
    });
  })
  .controller(
    'projectRecordsCtrl',
    function($scope, $rootScope, $http, $routeParams, $location, $modal,
      localStorageService, $sce) {

      $scope.page = 'records';

      // the project id, extracted from route params
      $scope.projectId = $routeParams.projectId;

      // status variables
      $scope.unmappedDescendantsPresent = false;
      $scope.mapNotesPresent = false;
      $scope.mapAdvicesPresent = false;

      // error variables
      $scope.errorProject = "";
      $scope.errorConcept = "";
      $scope.errorRecords = "";

      // pagination variables
      $scope.recordsPerPage = 10;
      $scope.recordPage = 1;

      // for collapse directive
      $scope.isCollapsed = true;

      $scope.conversation = null;

      // watch for changes to focus project
      $scope.$on('localStorageModule.notification.setFocusProject', function(
        event, parameters) {
        console.debug("ProjectRecordCtrl:  Detected change in focus project",
          parameters.focusProject);
        $scope.focusProject = parameters.focusProject;
        $scope.getRecordsForProject();
      });

      // retrieve the current global variables
      $scope.focusProject = localStorageService.get('focusProject');
      $scope.mapProjects = localStorageService.get("mapProjects");
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.preferences = localStorageService.get('preferences');

      // once focus project retrieved, retrieve the concept and records
      $scope.userToken = localStorageService.get('userToken');
      $scope.$watch([ 'focusProject', 'userToken' ], function() {

        console.debug('Detected project or user change', $scope.focusProject,
          $scope.userToken);

        // need both focus project and user token set before executing main
        // functions
        if ($scope.focusProject != null && $scope.userToken != null) {
          $http.defaults.headers.common.Authorization = $scope.userToken;
          $scope.projectId = $scope.focusProject.id;
          $scope.getRecordsForProject();
        }
      });

      $scope.getRecordsForProject = function() {

        $scope.project = $scope.focusProject;

        // load first page
        $scope.retrieveRecords(1);
      };

      // Constructs trusted html code from raw/untrusted html code
      $scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

      // function to clear input box and return to initial view
      $scope.resetSearch = function() {
        $scope.query = null;
        $scope.retrieveRecords(1);
      };

      // function to retrieve records for a specified page
      $scope.retrieveRecords = function(page) {

        console.debug('Retrieving records');

        // construct html parameters parameter
        var pfsParameterObj = constructPfsParameterObj(page);

        var query_url;
        if ($scope.currentRole === 'Viewer') {
          query_url = root_mapping + "record/project/id/"
            + $scope.project.objectId + "/published";
        } else if ($scope.currentRole === 'Specialist'
          || $scope.currentRole === 'Lead'
          || $scope.currentRole === 'Administrator') {
          query_url = root_mapping + "record/project/id/"
            + $scope.project.objectId;
        } else {
          console.debug("ERROR: Invalid role detected in retrieveRecords()");
        }

        $rootScope.glassPane++;

        // retrieve map records
        $http({
          url : query_url,
          dataType : "json",
          data : pfsParameterObj,
          method : "POST",
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(
          function(data) {

            $rootScope.glassPane--;
            $scope.records = data.mapRecord;
            $scope.statusRecordLoad = "";

            // set pagination variables
            $scope.nRecords = data.totalCount;
            $scope.numRecordPages = Math.ceil(data.totalCount
              / $scope.recordsPerPage);

          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        }).then(function(data) {

          // check if icon legends are necessary
          $scope.unmappedDescendantsPresent = false;
          $scope.mapNotesPresent = false;
          $scope.mapAdvicesPresent = false;

          // check if any notes or advices are present
          for (var i = 0; i < $scope.records.length; i++) {
            if ($scope.records[i].mapNote.length > 0) {
              $scope.mapNotesPresent = true;
            }
            for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {
              if ($scope.records[i].mapEntry[j].mapAdvice.length > 0) {
                $scope.mapAdvicesPresent = true;
              }
            }
            ;
          }
          ;

          // check relation syle flags
          if ($scope.project.mapRelationStyle === "MAP_CATEGORY_STYLE") {
            applyMapCategoryStyle();
          }

          if ($scope.project.mapRelationStyle === "RELATIONSHIP_STYLE") {
            applyRelationshipStyle();
          }

          // get unmapped descendants (checking done in routine)
          if ($scope.records.length > 0) {
            getUnmappedDescendants(0);
          }
        });
      };

      // Constructs a paging/filtering/sorting parameters object for RESTful
      // consumption
      function constructPfsParameterObj(page) {

        return {
          "startIndex" : (page - 1) * $scope.recordsPerPage,
          "maxResults" : $scope.recordsPerPage,
          "sortField" : null,
          "queryRestriction" : $scope.query
        }; // assigning simply to $scope.query when null produces undefined

      }

      function getUnmappedDescendants(index) {

        // before processing this record, make call to start next async request
        if (index < $scope.records.length - 1) {
          getUnmappedDescendants(index + 1);
        }

        $scope.records[index].unmappedDescendants = [];

        // if descendants below threshold for lower-level concept, check for
        // unmapped
        if ($scope.records[index].countDescendantConcepts < 11) {

          $http(
            {
              url : root_mapping + "concept/id/"
                + $scope.records[index].conceptId + "/"
                + "unmappedDescendants/project/id/" + $scope.project.id,
              dataType : "json",
              method : "GET",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {
            if (data.count > 0)
              $scope.unmappedDescendantsPresent = true;
            $scope.records[index].unmappedDescendants = data.searchResult;
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }

      }
      ;

      function applyMapCategoryStyle() {

        // set the category display text
        $scope.mapRelationStyleText = "Map Category Style";

        // Cycle over all entries. If targetId is blank, show relationName as
        // the target name
        for (var i = 0; i < $scope.records.length; i++) {
          for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {

            if ($scope.records[i].mapEntry[j].targetId === "") {
              $scope.records[i].mapEntry[j].targetName = "\""
                + $scope.records[i].mapEntry[j].relationName + "\"";
            }
          }
        }
      }
      ;

      function applyRelationshipStyle() {

        $scope.mapRelationStyleText = "Relationship Style";

        // Cycle over all entries. Add the relation name to the advice list
        for (var i = 0; i < $scope.records.length; i++) {
          for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {
            if ($scope.records[i].mapEntry[j].targetId === "") {
              // get the object for easy handling
              var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;

              // add the serialized advice
              jsonObj.push({
                "id" : "0",
                "name" : "\"" + $scope.records[i].mapEntry[j].mapRelationName
                  + "\"",
                "detail" : "\"" + $scope.records[i].mapEntry[j].mapRelationName
                  + "\"",
                "objectId" : "0"
              });

              $scope.records[i].mapEntry[j].mapAdvice = jsonObj;
            }
          }
        }
      }
      ;

      // function to change project from the header
      $scope.changeFocusProject = function(mapProject) {
        $scope.focusProject = mapProject;
        console.debug("changing project to " + $scope.focusProject.name);

        // update and broadcast the new focus project
        localStorageService.add('focusProject', $scope.focusProject);
        $rootScope.$broadcast(
          'localStorageModule.notification.setFocusProject', {
            key : 'focusProject',
            focusProject : $scope.focusProject
          });

        // update the user preferences
        $scope.preferences.lastMapProjectId = $scope.focusProject.id;
        localStorageService.add('preferences', $scope.preferences);
        $rootScope.$broadcast(
          'localStorageModule.notification.setUserPreferences', {
            key : 'userPreferences',
            userPreferences : $scope.preferences
          });

      };

      $scope.goToHelp = function() {
        var path;
        if ($scope.page != 'mainDashboard') {
          path = "help/" + $scope.page + "Help.html";
        } else {
          path = "help/" + $scope.currentRole + "DashboardHelp.html";
        }
        console.debug("go to help page " + path);
        // redirect page
        $location.path(path);
      };

      $scope.isEditable = function(record) {

        if (($scope.currentRole === 'Specialist'
          || $scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator')
          && (record.workflowStatus === 'PUBLISHED' || record.workflowStatus === 'READY_FOR_PUBLICATION')) {

          return true;

        } else if ($scope.currentUser.userName === record.owner.userName) {
          return true;
        } else
          return false;
      };

      $scope.editRecord = function(record) {

        console.debug("EditRecord()");
        console.debug(record);

        // check if this record is assigned to the user and not in a publication
        // ready state
        if (record.owner.userName === $scope.currentUser.userName
          && record.workflowStatus != 'PUBLISHED'
          && record.workflowStatus != 'READY_FOR_PUBLICATION') {

          // go to the edit page
          $location.path("/record/recordId/" + id);

          // otherwise, assign this record along the FIX_ERROR_PATH
        } else {

          $rootScope.glassPane++;

          console
            .debug("Edit record clicked, assigning record along FIX_ERROR_PATH");
          $http(
            {
              url : root_workflow + "assignFromRecord/user/id/"
                + $scope.currentUser.userName,
              method : "POST",
              dataType : 'json',
              data : record,
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(
            function(data) {
              console.debug('Assignment successful');
              $http(
                {
                  url : root_workflow + "record/project/id/"
                    + $scope.focusProject.id + "/concept/id/"
                    + record.conceptId + "/user/id/"
                    + $scope.currentUser.userName,
                  method : "GET",
                  dataType : 'json',
                  data : record,
                  headers : {
                    "Content-Type" : "application/json"
                  }
                }).success(function(data) {

                $rootScope.glassPane--;

                // open the record edit view
                $location.path("/record/recordId/" + data.id);
              }).error(function(data, status, headers, config) {
                $rootScope.glassPane--;

                $rootScope.handleHttpError(data, status, headers, config);
              });

            }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
      };

      $scope.truncate = function(string, length) {
        if (length == null)
          length = 100;
        if (string.length > length)
          return string.slice(0, length - 3);
        else
          return string;
      };

      $scope.truncated = function(string, length) {
        if (length == null)
          length = 100;
        if (string.length > length)
          return true;
        else
          return false;
      };

      $scope.openViewerFeedbackModal = function(lrecord, currentUser) {

        console.debug("openViewerFeedbackModal with ", lrecord, currentUser);

        var modalInstance = $modal
          .open({
            templateUrl : 'js/widgets/projectRecords/projectRecordsViewerFeedback.html',
            controller : ViewerFeedbackModalCtrl,
            resolve : {
              record : function() {
                return lrecord;
              },
              currentUser : function() {
                return currentUser;
              }
            }
          });

      };

      var ViewerFeedbackModalCtrl = function($scope, $modalInstance, record) {

        console.debug("Entered modal control", record);

        $scope.record = record;
        $scope.project = localStorageService.get('focusProject');
        $scope.currentUser = localStorageService.get('currentUser');
        $scope.returnRecipients = $scope.project.mapLead;
        $scope.feedbackInput = '';

        $scope.sendFeedback = function(record, feedbackMessage, name, email) {
          console.debug("Sending feedback email", record);

          if (feedbackMessage == null || feedbackMessage == undefined
            || feedbackMessage === '') {
            window.alert("The feedback field cannot be blank. ");
            return;
          }

          if ($scope.currentUser.userName === 'guest'
            && (name == null || name == undefined || name === ''
              || email == null || email == undefined || email === '')) {
            window.alert("Name and email must be provided.");
            return;
          }

          if ($scope.currentUser.userName === 'guest'
            && validateEmail(email) == false) {
            window.alert("Invalid email address provided.");
            return;
          }

          var sList = [ name, email, record.conceptId, record.conceptName, $scope.project.refSetId,
            feedbackMessage ];

          $rootScope.glassPane++;
          $http({
            url : root_workflow + "message",
            dataType : "json",
            method : "POST",
            data : sList,
            headers : {
              "Content-Type" : "application/json"
            }

          }).success(function(data) {
            console.debug("success to sendFeedbackEmail.");
            $rootScope.glassPane--;
            $modalInstance.close();
          }).error(function(data, status, headers, config) {
            $modalInstance.close();
            $scope.recordError = "Error sending feedback email.";
            $rootScope.glassPane--;
            $rootScope.handleHttpError(data, status, headers, config);
          });

        };

        $scope.cancel = function() {
          $modalInstance.dismiss('cancel');
        };

        function validateEmail(email) {
          var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
          return re.test(email);
        }

        $scope.tinymceOptions = {

          menubar : false,
          statusbar : false,
          plugins : "autolink autoresize link image charmap searchreplace lists paste",
          toolbar : "undo redo | styleselect lists | bold italic underline strikethrough | charmap link image",

          setup : function(ed) {

            // added to fake two-way binding from the html
            // element
            // noteInput is not accessible from this javascript
            // for some reason
            ed.on('keyup', function(e) {
              $scope.tinymceContent = ed.getContent();
              $scope.$apply();
            });
          }
        };
      };

    });
