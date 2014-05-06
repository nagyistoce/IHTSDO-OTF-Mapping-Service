'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap', 'mapProjectAppDirectives', 'mapProjectAppServices']);

var root_url = "${base.url}/mapping-rest/";

var root_mapping = root_url + "mapping/";
var root_content = root_url + "content/";
var root_metadata = root_url + "metadata/";
var root_validation = root_url + "validation/";
var root_workflow = root_url + "workflow/";

mapProjectAppControllers.run(function($rootScope, $http, localStorageService) {
	$rootScope.glassPane = 0;
	
	/*// retrieve state variables
	utilityService.getStateVariables().then(function (stateVariables) {
		
		// on promise return, set variables
		$scope.focusProject 	= stateVariables.focusProject;
		$scope.currentUser 		= stateVariables.currentUser;
		$scope.currentRole 		= stateVariables.currentRole;
		$scope.metadata 		= stateVariables.metadata;
		$scope.mapProjects 		= stateVariables.mapProjects;
		$scope.mapUsers 		= stateVariables.mapUsers;
		
		// call dependent functions
		// e.g. retrieveRecords(), getProjectDetails(), what have you
	});*/

	
	
	
	// retrieve projects
	$http({
		url: root_mapping + "project/projects",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}	
	}).success(function(data) {
		localStorageService.add('mapProjects', data.mapProject);
		$rootScope.$broadcast('localStorageModule.notification.setMapProjects',{key: 'mapProjects', mapProjects: data.mapProject});  


	});

	// retrieve metadata
	$http({
		url: root_metadata + "terminologies/latest",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}
	}).success(function(response) {
		var keyValuePairs = response.keyValuePair;
		for (var i = 0; i < keyValuePairs.length; i++) {
			console.debug(keyValuePairs[i]);
			$http({
				url: root_metadata + "all/" + keyValuePairs[i].key + "/" + keyValuePairs[i].value,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(metadata) {
				localStorageService.add('metadata', metadata);
			});

		}
	});


	// retrieve users
	$http({
		url: root_mapping + "user/users",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}	
	}).success(function(data) {
		localStorageService.add('mapUsers', data.mapUser);
		$rootScope.$broadcast('localStorageModule.notification.setMapUsers',{key: 'mapUsers', mapUsers: data.mapUsers});  

	});

});


mapProjectAppControllers.controller('ResolveConflictsDashboardCtrl', function ($scope, $routeParams, $rootScope, localStorageService) {

	setModel();

	$scope.focusProject = localStorageService.get('focusProject');

	function setModel() {
		$scope.name = 'ResolveConflictsDashboard';
		if (!$scope.model) {
			$scope.model = {

					structure: "12/6-6/12",
					rows: [{
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "compareRecords",
								title: "Compare Records"
							}]
						}]
					}, { // new row

						columns: [{
							class: 'col-md-6',
							widgets: [{
								type: "mapRecord",
								config: { recordId: $routeParams.recordId},
								title: "Map Record"
							}]
						}, {
							class: 'col-md-6',
							widgets: [{
								type: "mapEntry",
								config: { entry: $scope.entry},
								title: "Map Entry"
							}, {
								type: "terminologyBrowser",
								config: {
									terminology: $scope.focusProject.destinationTerminology,
									terminologyVersion: $scope.focusProject.destinationTerminologyVersion
								},
								title: $scope.focusProject.destinationTerminology + " Terminology Browser"

							}],
						} // end second column
						] // end columns

					}] // end second row


			};
		}
	};

	console.debug("CONTROLLER MODEL");
	console.debug($scope.model);

	// broadcast page to help mechanism  
	$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'resolveConflictsDashboard'});  

	$scope.$on('adfDashboardChanged', function (event, name, model) {
		console.debug("Dashboard change detected by ResolveConflictsDashboard");
		localStorageService.set(name, model);
	});

	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
		console.debug("MapProjectWidgetCtrl: Detected change in focus project");
		$scope.project = parameters.focusProject;

		console.debug($scope.project);
	});	

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('ResolveConflictsDashboardCtrl: Detected project set/change');
		setModel();


	});
});



mapProjectAppControllers.controller('dashboardCtrl', function ($rootScope, $scope, $http, localStorageService) {

	$scope.currentRole = localStorageService.get('currentRole');

	console.debug('in dashboardCtrl');

	// watch for preferences change
	$scope.$on('localStorageModule.notification.setUserPreferences', function(event, parameters) { 	
		console.debug("dashboardCtrl:  Detected change in preferences");
		if (parameters.userPreferences != null && parameters.userPreferences != undefined) {
			$http({
				url: root_mapping + "userPreferences/update",
				dataType: "json",
				data: parameters.userPreferences,
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {
			});
		}
	});

	// on successful user retrieval, construct the dashboard
	$scope.$watch('currentRole', function() {

		console.debug("Setting the dashboard based on role: " + $scope.currentRole);

		/**
		 * Viewer has the following widgets:
		 * - MapProject
		 */
		if ($scope.currentRole === 'Viewer') {
			$scope.model = {

					structure: "12/6-6/12",
					rows: [{
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "mapProject",
								config: {},
								title: "Map Project"
							}]
						}]
					}]
			};
			/**
			 * Specialist has the following widgets:
			 * - MapProject
			 * - WorkAvailable
			 * - AssignedList
			 * - EditedList
			 */
		} else if ($scope.currentRole === 'Specialist') {

			$scope.model = {

					structure: "12/6-6/12",
					rows: [{	
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "mapProject",
								config: {},
								title: "Map Project"
							}]
						}]
					}, {
						columns: [{
							class: 'col-md-6',
							widgets: [{
								type: "workAvailable",
								config: {},
								title: "Available Work"
							}]
						}, {
							class: 'col-md-6',
							widgets: [{
								type: "assignedList",
								config: {},
								title: "Assigned to Me"
							}]
						}]
					}

					, {
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "editedList",
								title: "Recently Edited"
							}]
						}]
					}]
			};

			/**
			 * Lead has the following widgets
			 * -MapProject
			 * - WorkAvailable
			 * - AssignedList
			 * - EditedList
			 * - MetadataList
			 */
		} else if ($scope.currentRole === 'Lead') {

			console.debug("Setting model for lead");

			$scope.model = {

					structure: "12/6-6/12",
					rows: [{
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "mapProject",
								config: {},
								title: "Map Project"
							}]
						}]
					}, {
						columns: [{
							class: 'col-md-6',
							widgets: [{
								type: "workAvailable",
								config: {},
								title: "Available Work"
							}]
						}, {
							class: 'col-md-6',
							widgets: [{
								type: "assignedList",
								config: {},
								title: "Assigned to Me"
							}]
						}]
					}

					, {
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "editedList",
								title: "Recently Edited"
							}]
						}]
					}, {
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "metadataList",
								config: {
									terminology: "SNOMEDCT"
								},
								title: "Metadata"
							}]
						}]
					}]
			};

			console.debug($scope.model);

			/** Admin has the following widgets
			 * - MapProject
			 * - MetadataList
			 * - AdminTools
			 */
		} else if ($scope.currentRole === 'Administrator') {

			$scope.model = {

					structure: "12/6-6/12",
					rows: [{
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "mapProject",
								config: {},
								title: "Map Project"
							}]
						}]
					},{
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "metadataList",
								config: {
									terminology: "SNOMEDCT"
								},
								title: "Metadata"
							}]
						}]
					}]
			};

		} else {
			alert("Invalid role detected by dashboard");
		}

		$scope.$on('adfDashboardChanged', function (event, name, model) {
			console.debug('adfDashboardChanged in DashBoardCtrl');
			console.debug(event);
			console.debug(name);
			console.debug(model);
			$scope.model = model;
		});
	});

});

mapProjectAppControllers.controller('MapRecordDashboardCtrl', function ($scope, $rootScope, $routeParams, $location, localStorageService) {

	$scope.currentRole = localStorageService.get('currentRole');
	$scope.focusProject = localStorageService.get('focusProject');

	setModel();

	function setModel() {
		$scope.name = 'EditingDashboard';
		console.debug("Setting record dashboard model");
		console.debug($scope.model);
		if (!$scope.model) {
			$scope.model = {
					structure: "6-6",                          
					rows: 
						[{
							columns: [{
								class: 'col-md-6',
								widgets: [{
									type: "mapRecord",
									config: { recordId: $routeParams.recordId},
									title: "Map Record"
								}]
							}, {
								class: 'col-md-6',
								widgets: [{
									type: "mapEntry",
									config: { entry: $scope.entry},
									title: "Map Entry"
								}, {
									type: "terminologyBrowser",
									config: { 
										terminology: $scope.focusProject.destinationTerminology,
										terminologyVersion: $scope.focusProject.destinationTerminologyVersion
									},
									title: $scope.focusProject.destinationTerminology + " Terminology Browser"

								}],
							} // end second column
							] // end columns
						}] // end rows
			};

		}
	};
	
	// broadcast page to help mechanism  
	$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'editDashboard'});  

	$scope.$on('adfDashboardChanged', function (event, name, model) {
		console.debug("Dashboard change detected by MapRecordDashboard");
		localStorageService.set(name, model);
	});

	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("RecordDashboardCtrl:  Detected change in focus project");
		
		// set the model to empty
		$scope.model = {
				structure: "6-6",                          
				rows: 
					[{}]
		};
		
		setModel();
		
		console.debug($scope.currentRole);
		
		var path = "";

		if ($scope.currentRole === "Specialist") {
			path = "/specialist/dash";
		} else if ($scope.currentRole === "Lead") {
			path = "/lead/dash";
		} else if ($scope.currentRole === "Administrator") {
			path = "/admin/dash";
		} else if ($scope.currentRole === "Viewer") {
			path = "/viewer/dash";
		}
		console.debug("redirecting to " + path);
		$location.path(path);
	});	

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('RecordDashBoardCtrl:  Detected project set/change');
		setModel();


	});
});

//Navigation
mapProjectAppControllers.controller('LoginCtrl', ['$scope', 'localStorageService', '$rootScope', '$location', '$http',
                                                  function ($scope, localStorageService, $rootScope, $location, $http) {

	// if app has already been loaded, simply retrieve state variables from local storage service
	$scope.mapUsers 	= localStorageService.get('mapUsers');
	$scope.mapProjects 	= localStorageService.get('mapProjects');
	$scope.metadata 	= localStorageService.get('metadata');
	
	// to ensure that variables are set correctly on first load, watch for notifications
	$scope.$on('localStorageModule.notification.setMapProjects', function(event, parameters) { 	
		console.debug("LoginCtrl: initialization complete for MapProjects"); 
		$scope.mapProjects = parameters.mapProjects;
	});
		
	// to ensure that variables are set correctly on first load, watch for notifications
	$scope.$on('localStorageModule.notification.setMapUsers', function(event, parameters) { 	
		console.debug("LoginCtrl: initialization complete for MapUsers");    
		$scope.mapUsers = parameters.mapUsers;
	});
		
	// set the user, role, focus project, and preferences to null (i.e. clear) by broadcasting to rest of app
	$rootScope.$broadcast('localStorageModule.notification.setUser',{key: 'currentUser', currentUser: null});  
	$rootScope.$broadcast('localStorageModule.notification.setRole',{key: 'currentRole', currentRole: null});  
	$rootScope.$broadcast('localStorageModule.notification.setFocusProject', {key: 'focusProject', focusProject: null});
	$rootScope.$broadcast('localStorageModule.notificatoin.setPreferences', {key: 'preferences', preferences: null});

	// broadcast page to help mechanism
	$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'login'});

	// set all local variables to null
	$scope.mapUser = [];
	$scope.error = [];
	$scope.preferences = [];

	// retrieve projects for focus controls
	$http({
		url: root_mapping + "project/projects",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}	
	}).success(function(data) {
		$scope.projects = data.mapProject;
		localStorageService.add('mapProjects', data.mapProject);

	}).error(function(error) {
		$scope.error = $scope.error + "Could not retrieve map projects. "; 

	}).then(function(data) {
		console.debug("broadcasting projects");
		console.debug($scope.projects);
		$rootScope.$broadcast('localStorageModule.notification.setMapProjects',{key: 'mapProjects', mapProjects: $scope.projects});  

	});

	// retrieve metadata
	$http({
		url: root_metadata + "terminologies/latest",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}
	}).success(function(response) {
		var keyValuePairs = response.keyValuePair;
		for (var i = 0; i < keyValuePairs.length; i++) {
			console.debug(keyValuePairs[i]);
			$http({
				url: root_metadata + "all/" + keyValuePairs[i].key + "/" + keyValuePairs[i].value,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(metadata) {
			});

		}
	}).error(function() {
		console.debug("error loading response terminology info");
	});


	// retrieve users
	$http({
		url: root_mapping + "user/users",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}	
	}).success(function(data) {
		$scope.mapUsers = data.mapUser;
		localStorageService.add('mapUsers', data.mapUser);
	}).error(function(error) {
		$scope.error = $scope.error + "Could not retrieve map users. "; 

	});

	// initial values for pick-list
	$scope.roles = [
	                {name:'Viewer', value:1},
	                {name:'Specialist', value:2},
	                {name:'Lead', value:3},
	                {name:'Administrator', value:4}];
	$scope.role = $scope.roles[0];  

	// login button directs to next page based on role selected
	$scope.go = function () {

		console.debug($scope.role);

		var path = "";

		if ($scope.role.name == "Specialist") {
			path = "/specialist/dash";
		} else if ($scope.role.name == "Lead") {
			path = "/lead/dash";
		} else if ($scope.role.name == "Administrator") {
			path = "/admin/dash";
		} else if ($scope.role.name == "Viewer") {
			path = "/viewer/dash";
		}

		// check that user has been selected
		if ($scope.mapUser == null) {
			alert("You must specify a user");
		} else {

			// retrieve the user preferences
			$http({
				url: root_mapping + "userPreferences/" + $scope.mapUser.userName,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {
				console.debug($scope.mapProjects);
				console.debug(data);
				$scope.preferences = data;
				$scope.preferences.lastLogin = new Date().getTime();
				localStorageService.add('preferences', $scope.preferences);
				for (var i = 0; i < $scope.mapProjects.length; i++)  {
					if ($scope.mapProjects[i].id === $scope.preferences.lastMapProjectId) {
						$scope.focusProject = $scope.mapProjects[i];
					}
				}
				console.debug('Last project: ');
				console.debug($scope.focusProject);
				localStorageService.add('focusProject', $scope.focusProject);
				$rootScope.$broadcast('localStorageModule.notification.setPreferences', {key: 'preferences', preferences: $scope.preferences});
				$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  
			});

			// add the user information to local storage
			localStorageService.add('currentUser', $scope.mapUser);
			localStorageService.add('currentRole', $scope.role.name);

			// broadcast the user information to rest of app
			$rootScope.$broadcast('localStorageModule.notification.setUser',{key: 'currentUser', currentUser: $scope.mapUser});
			$rootScope.$broadcast('localStorageModule.notification.setRole',{key: 'currentRole', currentRole: $scope.role.name});

			// redirect page
			$location.path(path);
		}
	};
}]);




//Mapping Services


mapProjectAppControllers.controller('MapProjectListCtrl', 
		function ($scope, $http) {

	// initialize as empty to indicate still initializing database connection
	$scope.projects = [];

	$http({
		url: root_mapping + "project/projects",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}
	}).success(function(data) {
		$scope.projects = data.mapProject;
	}).error(function(error) {
		$scope.error = "Error";
	});

});


/*
 * Controller for retrieving and displaying records associated with a concept
 */
mapProjectAppControllers.controller('RecordConceptListCtrl', ['$scope', '$http', '$routeParams', '$sce', '$rootScope', '$location', 'localStorageService', 
                                                              function ($scope, $http, $routeParams, $sce, $rootScope, $location, localStorageService) {

	// scope variables
	$scope.error = "";		// initially empty
	$scope.conceptId = $routeParams.conceptId;
	$scope.recordsInProject = [];
	$scope.recordsNotInProject = [];
	$scope.recordsInProjectNotFound = false; // set to true after record retrieval returns no records for focus project
	$scope.focusProject = localStorageService.get("focusProject");
	$scope.mapProjects = localStorageService.get("mapProjects");

	// local variables
	var projects = localStorageService.get("mapProjects");


	// retrieve current user and role
	$scope.currentUser = localStorageService.get("currentUser");
	$scope.currentRole = localStorageService.get("currentRole");

	// retrieve focus project on first call
	$scope.focusProject = localStorageService.get("focusProject");

	// watch for changes to focus project
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("RecordConceptListCtrl:  Detected change in focus project");      
		$scope.focusProject = parameters.focusProject;
		$scope.filterRecords();
	});	


	// broadcast page to help mechanism
	$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'concept'});

	// once focus project retrieved, retrieve the concept and records
	$scope.$watch('focusProject', function() {
		
		$scope.recordsInProjectNotFound = false;

		console.debug("RecordConceptCtrl:  Focus Project change");
		
		// retrieve projects information to ensure display handled properly
		$http({
			url: root_mapping + "project/projects",
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			projects = data.mapProject;
		}).error(function(error) {
			$scope.error = $scope.error + "Could not retrieve projects. "; 

		}).then(function() {

			// get all records for this concept
			$scope.getRecordsForConcept();
		});


		// find concept based on source terminology
		$http({
			url: root_content + "concept/" 
			+ $scope.focusProject.sourceTerminology + "/" 
			+ $scope.focusProject.sourceTerminologyVersion 
			+ "/id/" 
			+ $routeParams.conceptId,
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			$scope.concept = data;
			$scope.findUnmappedDescendants();

			// find children based on source terminology
			$http({
				url: root_content + "concept/" 
				+ $scope.focusProject.sourceTerminology + "/" 
				+ $scope.focusProject.sourceTerminologyVersion 
				+ "/id/" 
				+ $routeParams.conceptId
				+ "/children",
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {
				console.debug(data);
				$scope.concept.children = data.searchResult;

			}).error(function(error) {
				$scope.error = $scope.error + "Could not retrieve Concept children. ";    
			});
		}).error(function(error) {
			console.debug("Could not retrieve concept");
			$scope.error = $scope.error + "Could not retrieve Concept. ";    
		});
	});

	// function to return trusted html code (for tooltip content)
	$scope.to_trusted = function(html_code) {
		return $sce.trustAsHtml(html_code);
	};

	$scope.getRecordsForConcept = function() {
		// retrieve all records with this concept id
		$http({
			url: root_mapping + "record/conceptId/" + $routeParams.conceptId,
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			$scope.records = data.mapRecord;
			$scope.filterRecords();
		}).error(function(error) {
			$scope.error = $scope.error + "Could not retrieve records. ";    
		}).then(function() {

			// check relation style flags
			if ($scope.focusProject.mapRelationStyle === "MAP_CATEGORY_STYLE") {
				applyMapCategoryStyle();
			}

			if ($scope.focusProject.mapRelationStyle === "RELATIONSHIP_STYLE") {
				applyRelationshipStyle();
			}
		});
	};

	$scope.isEditable = function(record) {

		console.debug('isEditable');
		console.debug($scope.currentRole);
		console.debug($scope.currentUser);
		console.debug(record.owner);
		if (($scope.currentRole === 'Specialist' ||
				$scope.currentRole === 'Lead' ||
				$scope.currentRole === 'Admin') &&
				(record.workflowStatus === 'PUBLISHED' || record.workflowStatus === 'READY_FOR_PUBLICATION')) {

			return true;

		} else if ($scope.currentUser.userName === record.owner.userName) {
			return true;
		} else return false;
	};

	$scope.filterRecords = function() {
		$scope.recordsInProject = [];
		$scope.recordsNotInProject = [];
		for (var i = 0; i < $scope.records.length; i++) {
			if ($scope.records[i].mapProjectId === $scope.focusProject.id) {
				$scope.recordsInProject.push($scope.records[i]);
			} else {
				$scope.recordsNotInProject.push($scope.records[i]);
			}
		}

		// if no records for this project found, set flag
		if ($scope.recordsInProject.length == 0) $scope.recordsInProjectNotFound = true;
	};

	$scope.getProject = function(record) {
		for (var i = 0; i < projects.length; i++) {
			if (projects[i].id == record.mapProjectId) {
				return projects[i];
			}
		}
		return null;
	};

	$scope.getProjectFromName = function(name) {
		for (var i = 0; i < projects.length; i++) {
			if (projects[i].name === name) {
				return projects[i];
			}
		}
		return null;
	};

	$scope.getProjectName = function(record) {

		for (var i = 0; i < projects.length; i++) {
			if (projects[i].id == record.mapProjectId) {
				return projects[i].name;
			}
		}
		return null;
	};

	$scope.findUnmappedDescendants = function() {


		$http({
			url: root_mapping + "concept/" 
			+ $scope.concept.terminology + "/"
			+ $scope.concept.terminologyVersion + "/"
			+ "id/" + $scope.concept.terminologyId + "/"
			+ "threshold/10",
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			if (data.count > 0) $scope.unmappedDescendantsPresent = true;
			$scope.concept.unmappedDescendants = data.searchResult;
		});
	};

	// given a record, retrieves associated project's ruleBased flag
	$scope.getRuleBasedForRecord = function(record) {
		var project = $scope.getProject(record);
		return project.ruleBased;

	};

	function applyMapCategoryStyle() {

		// Cycle over all entries. If targetId is blank, show relationName as the target name
		for (var i = 0; i < $scope.records.length; i++) {		 
			for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 

				if ($scope.records[i].mapEntry[j].targetId === "") {
					$scope.records[i].mapEntry[j].targetName = "\"" + $scope.records[i].mapEntry[j].relationName + "\"";

				}
			}
		}
	};

	function applyRelationshipStyle() {
		// Cycle over all entries. Add the relation name to the advice list
		for (var i = 0; i < $scope.records.length; i++) {		 
			for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 	 
				if ($scope.records[i].mapEntry[j].targetId === "") {	 
					// get the object for easy handling
					var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;

					// add the serialized advice	
					jsonObj.push({"id":"0", "name": "\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "detail":"\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "objectId":"0"});

					$scope.records[i].mapEntry[j].mapAdvice = jsonObj;
				}
			}
		}
	};

	$scope.createMapRecord = function(project) {

		if (!(project == null) && !(project === "")) {

			// get the project
			var countDescendantConcepts;

			// find concept based on source terminology
			$http({
				url: root_content + "concept/" 
				+ project.sourceTerminology + "/" 
				+ project.sourceTerminologyVersion 
				+ "/id/" 
				+ $scope.conceptId,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {

				$scope.concept = data;

			}).then(function(data) {


				// get descendant count
				$http({
					url: root_content + "concept/" 
					+ project.sourceTerminology + "/" 
					+ project.sourceTerminologyVersion 
					+ "/id/" 
					+ $scope.conceptId
					+ "/descendants",
					dataType: "json",
					method: "GET",
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {    
					countDescendantConcepts = data.count;
				}).error(function(data) {
					countDescendantConcepts = 0;
				}).then(function() {

					// construct the map record
					var record = {
							"id" : "",
							"mapProjectId" : project.id,
							"conceptId" : $scope.concept.terminologyId,
							"conceptName" : $scope.concept.defaultPreferredName,
							"countDescendantConcepts": countDescendantConcepts,
							"mapEntry": [],
							"mapNote": [],
							"mapPrinciple": [],
							"owner" : $scope.currentUser,
							"lastModifiedBy" : $scope.currentUser,
							"workflowStatus" : 'NEW'
					};

					// add the map record
					$http({
						url: root_mapping + "record/add",
						dataType: "json",
						method: "PUT",
						data: record,
						headers: {
							"Content-Type": "application/json"
						}	
					}).success(function(data) {    
						record = data;
					}).error(function(data) {

					}).then(function() {

						$location.path("/record/recordId/" + record.id);
					});
				});
			});
		};	  
	};
}]);




//Mapping Services
mapProjectAppControllers.controller('MapProjectListCtrl', 
		function ($scope, $http) {

	// initialize as empty to indicate still initializing database connection
	$scope.projects = [];

	$http({
		url: root_mapping + "project/projects",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}
	}).success(function(data) {
		$scope.projects = data.mapProject;
	}).error(function(error) {
		$scope.error = "Error";
	});

});

/**
 * Controller for Map Project Records view
 * 
 * Basic function:
 * 1) Retrieve map project
 * 2) Retrieve concept associated with map project refsetid
 * 3) Retrieve records
 * 
 * Scope functions (accessible from html)
 * - $scope.resetSearch 		clears query and launches unfiltered record retrieval request
 * - $scope.retrieveRecords 	launches new record retrieval request based on current paging/filtering/sorting parameters
 * - $scope.to_trusted	 		converts unsafe html into usable/displayable code
 *  
 * Helper functions: 
 * - setPagination				sets the relevant pagination attributes based on current page
 * - getNRecords				retrieves the total number of records available given filtering parameters
 * - getUnmappedDescendants		retrieves the unmapped descendants of a concept
 * - applyMapCategoryStyle		modifies map entries for display based on MAP_CATEGORY_STYLE
 * - applyRelationshipStyle		modifies map entries for display based on RELATIONSHIP_STYLE
 * - constructPfsParameterObj	creates a PfsParameter object from current paging/filtering/sorting parameters, consumed by RESTful service
 */

mapProjectAppControllers.controller('MapProjectRecordCtrl', ['$scope', '$http', '$routeParams', '$sce', '$rootScope', '$location', 'localStorageService',
                                                             function ($scope, $http, $routeParams, $sce, $rootScope, $location, localStorageService) {



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

	// for collapse directive
	$scope.isCollapsed = true;

	// watch for changes to focus project
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("ProjectRecordCtrl:  Detected change in focus project");      
		$scope.focusProject = parameters.focusProject;
	});	

	// broadcast page to help mechanism
	$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'records'});

	// retrieve the current global variables
	$scope.focusProject = localStorageService.get('focusProject');
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');

	$scope.$watch('focusProject', function() {
		$scope.projectId = $scope.focusProject.id;
		$scope.getRecordsForProject();
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
		
		// retrieve pagination information for the upcoming query
		setPagination($scope.recordsPerPage);

		// construct html parameters parameter
		var pfsParameterObj = constructPfsParameterObj(page);
		var query_url = root_mapping + "record/projectId/" + $scope.project.objectId;

		// retrieve map records
		$http({
			url: query_url,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$scope.records = data.mapRecord;
			$scope.statusRecordLoad = "";
			$scope.recordPage = page;

		}).error(function(error) {
			$scope.errorRecord = "Error retrieving map records";
			console.debug("changeRecordPage error");
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

					if ($scope.records[i].mapEntry[j].mapNote.length > 0) {
						$scope.mapNotesPresent = true;
					};
					if ($scope.records[i].mapEntry[j].mapAdvice.length > 0) {
						$scope.mapAdvicesPresent = true;
					}
				};
			};

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

	// Constructs a paging/filtering/sorting parameters object for RESTful consumption
	function constructPfsParameterObj(page) {

		return {"startIndex": (page-1)*$scope.recordsPerPage,
			"maxResults": $scope.recordsPerPage, 
			"sortField": null, 
			"filterString": $scope.query == null ? null : $scope.query};  // assigning simply to $scope.query when null produces undefined

	}

	// function to set the relevant pagination fields
	function setPagination(recordsPerPage) {

		// set scope variable for total records
		getNRecords();

		// set pagination variables
		$scope.recordsPerPage = recordsPerPage;
		$scope.numRecordPages = Math.ceil($scope.nRecords / $scope.recordsPerPage);
	};

	// function query for the number of records associated with full set or query
	function getNRecords() {

		var query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/nRecords";
		var pfsParameterObj = constructPfsParameterObj(1);

		// retrieve the total number of records associated with this map project
		$http({
			url: query_url,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$scope.nRecords = data;

		}).error(function(error) {
			$scope.nRecords = 0;
			console.debug("getNRecords error");
		});
	};

	function getUnmappedDescendants(index) {

		// before processing this record, make call to start next async request
		if (index < $scope.records.length-1) {
			getUnmappedDescendants(index+1);
		}

		$scope.records[index].unmappedDescendants = [];

		// if descendants below threshold for lower-level concept, check for unmapped
		if ($scope.records[index].countDescendantConcepts < 11) {

			$http({
				url: root_mapping + "concept/" 
				+ $scope.project.sourceTerminology + "/"
				+ $scope.project.sourceTerminologyVersion + "/"
				+ "id/" + $scope.records[index].conceptId + "/"
				+ "threshold/10",
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				if (data.count > 0) $scope.unmappedDescendantsPresent = true;
				$scope.records[index].unmappedDescendants = data.searchResult;
			});
		} 

	};

	function applyMapCategoryStyle() {

		// set the category display text
		$scope.mapRelationStyleText = "Map Category Style";

		// Cycle over all entries. If targetId is blank, show relationName as the target name
		for (var i = 0; i < $scope.records.length; i++) {		 
			for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 

				if ($scope.records[i].mapEntry[j].targetId === "") {
					$scope.records[i].mapEntry[j].targetName = "\"" + $scope.records[i].mapEntry[j].relationName + "\"";
				}
			}
		}
	};

	function applyRelationshipStyle() {

		$scope.mapRelationStyleText = "Relationship Style";

		// Cycle over all entries. Add the relation name to the advice list
		for (var i = 0; i < $scope.records.length; i++) {		 
			for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 	 
				if ($scope.records[i].mapEntry[j].targetId === "") {	 
					// get the object for easy handling
					var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;

					// add the serialized advice	
					jsonObj.push({"id":"0", "name": "\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "detail":"\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "objectId":"0"});

					$scope.records[i].mapEntry[j].mapAdvice = jsonObj;
				}
			}
		}
	};


}]);

mapProjectAppControllers.controller('MapProjectDetailCtrl', 
		['$scope', '$http', '$sce', '$rootScope', '$location', 'localStorageService',
		 function ($scope, $http, $sce, $rootScope, $location, localStorageService) {

			// broadcast page to help mechanism
			$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'project'});

			$scope.focusProject = localStorageService.get('focusProject');
			
			// watch for focus project change
			$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
				console.debug("MapProjectDetailCtrl: Detected change in focus project");
				$scope.focusProject = parameters.focusProject;  
			});
			
			$scope.$watch('focusProject', function() {

				console.debug('Formatting project details');


				// apply map type text styling
				if ($scope.focusProject.mapType === "SIMPLE_MAP") $scope.mapTypeText = "Simple Mapping";
				else if ($scope.focusProject.mapType === "COMPLEX_MAP") $scope.mapTypeText = "Complex Mapping";
				else if($scope.focusProject.mapType === "EXTENDED_MAP") $scope.mapTypeText = "Extended Mapping";
				else $scope.mapTypeText = "No mapping type specified";

				// apply relation style text styling
				console.debug($scope.focusProject.mapRelationStyle);
				console.debug($scope.focusProject.mapRelationStyle === "MAP_CATEGORY_STYLE");
				if ($scope.focusProject.mapRelationStyle === "MAP_CATEGORY_STYLE") $scope.mapRelationStyleText = "Map Category Style";
				else if ($scope.focusProject.mapRelationStyle === "RELATIONSHIP_STYLE") $scope.mapRelationStyleText = "Relationship Style";
				else $scope.mapRelationStyleText = "No relation style specified";

				// determine if this project has a principles document
				if ($scope.focusProject.destinationTerminology == "ICD10") {
					$scope.focusProject.mapPrincipleDocumentPath = "doc/";
					$scope.focusProject.mapPrincipleDocument = "ICD10_MappingPersonnelHandbook.docx";
					$scope.focusProject.mapPrincipleDocumentName = "Mapping Personnel Handbook";
				} else {
					$scope.focusProject.mapPrincipleDocument = null;
				}

				// set the scope maps
				$scope.scopeMap = {};
				$scope.scopeExcludedMap = {};
				
				// set pagination variables
				$scope.pageSize = 5;
				$scope.maxSize = 5;
				$scope.getPagedAdvices(1);
				$scope.getPagedRelations(1);
				$scope.getPagedPrinciples(1);
				$scope.getPagedScopeConcepts(1);
				$scope.getPagedScopeExcludedConcepts(1);
				$scope.orderProp = 'id';

			});

			$scope.goMapRecords = function () {
				console.debug($scope.role);

				var path = "/project/records";
					// redirect page
					$location.path(path);
			};


			// function to return trusted html code (for tooltip content)
			$scope.to_trusted = function(html_code) {
				return $sce.trustAsHtml(html_code);
			};



			///////////////////////////////////////////////////////////////
			// Functions to display and filter advices and principles
			// NOTE: This is a workaround due to pagination issues
			///////////////////////////////////////////////////////////////

			// get paged functions
			// - sorts (by id) filtered elements
			// - counts number of filtered elmeents
			// - returns artificial page via slice

			$scope.getPagedAdvices = function (page) {

				$scope.pagedAdvice = $scope.sortByKey($scope.focusProject.mapAdvice, 'id')
				.filter(containsAdviceFilter);
				$scope.pagedAdviceCount = $scope.pagedAdvice.length;
				$scope.pagedAdvice = $scope.pagedAdvice
				.slice((page-1)*$scope.pageSize,
						page*$scope.pageSize);
			};

			$scope.getPagedRelations = function (page) {

				$scope.pagedRelation = $scope.sortByKey($scope.focusProject.mapRelation, 'id')
				.filter(containsRelationFilter);
				$scope.pagedRelationCount = $scope.pagedRelation.length;
				$scope.pagedRelation = $scope.pagedRelation
				.slice((page-1)*$scope.pageSize,
						page*$scope.pageSize);
			};

			$scope.getPagedPrinciples = function (page) {

				$scope.pagedPrinciple = $scope.sortByKey($scope.focusProject.mapPrinciple, 'id')
				.filter(containsPrincipleFilter);
				$scope.pagedPrincipleCount = $scope.pagedPrinciple.length;
				$scope.pagedPrinciple = $scope.pagedPrinciple
				.slice((page-1)*$scope.pageSize,
						page*$scope.pageSize);

				console.debug($scope.pagedPrinciple);
			};

			$scope.getPagedScopeConcepts = function (page) {
				console.debug("Called paged scope concept for page " + page); 
				
				$scope.pagedScopeConcept = $scope.focusProject.scopeConcepts;
				$scope.pagedScopeConceptCount = $scope.pagedScopeConcept.length;
				
				$scope.pagedScopeConcept = $scope.pagedScopeConcept
				.slice((page-1)*$scope.pageSize,
						page*$scope.pageSize);
				
				
				// find concept based on source terminology
				for (var i = 0; i < $scope.pagedScopeConcept.length; i++) {
					$rootScope.glassPane++;
					$http({
						url: root_content + "concept/" 
						+ $scope.focusProject.sourceTerminology +  "/" 
						+ $scope.focusProject.sourceTerminologyVersion 
						+ "/id/" 
						+ $scope.focusProject.scopeConcepts[i],
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}	
					}).success(function(data) {
						$rootScope.glassPane--;
						var obj = {
								key: data.terminologyId,
								concept: data
						};  
						$scope.scopeMap[obj.key] = obj.concept.defaultPreferredName;
					}).error(function(error) {
						$rootScope.glassPane--;
						console.debug("Could not retrieve concept");
						$scope.error = $scope.error + "Could not retrieve Concept. ";    
					});

				}
				
				console.debug($scope.pagedScopeConcept);
			};

			$scope.getPagedScopeExcludedConcepts = function (page) {
				console.debug("Called paged scope excluded concept for page " + page);
				$scope.pagedScopeExcludedConcept = $scope.sortByKey($scope.focusProject.scopeExcludedConcepts, 'id')
				.filter(containsScopeExcludedConceptFilter);
				$scope.pagedScopeExcludedConceptCount = $scope.pagedScopeExcludedConcept.length;
				$scope.pagedScopeExcludedConcept = $scope.pagedScopeExcludedConcept
				.slice((page-1)*$scope.pageSize,
						page*$scope.pageSize);
				
				
				// fill the scope map for these variables
				for (var i = 0; i < $scope.pagedScopeExcludedConcept.length; i++) {
					$rootScope.glassPane++;
					$http({
						url: root_content + "concept/" 
						+ $scope.focusProject.sourceTerminology +  "/" 
						+ $scope.focusProject.sourceTerminologyVersion 
						+ "/id/" 
						+ $scope.focusProject.scopeExcludedConcepts[i],
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}	
					}).success(function(data) {
						$rootScope.glassPane--;
						var obj = {
								key: data.terminologyId,
								concept: data
						};  
						$scope.scopeExcludedMap[obj.key] = obj.concept.defaultPreferredName;
					}).error(function(error) {
						$rootScope.glassPane--;
						console.debug("Could not retrieve concept");
						$scope.error = $scope.error + "Could not retrieve Concept. ";    
					});
				}
				

				console.debug($scope.pagedScopeExcludedConcept);
			};

			// functions to reset the filter and retrieve unfiltered results

			$scope.resetAdviceFilter = function() {
				$scope.adviceFilter = "";
				$scope.getPagedAdvices(1);
			};

			$scope.resetRelationFilter = function() {
				$scope.relationFilter = "";
				$scope.getPagedRelationss(1);
			};

			$scope.resetPrincipleFilter = function() {
				$scope.principleFilter = "";
				$scope.getPagedPrinciples(1);
			};

			$scope.resetScopeConceptFilter = function() {
				$scope.scopeConceptFilter = "";
				$scope.getPagedScopeConcepts(1);
			};		

			$scope.resetScopeExcludedConceptFilter = function() {
				$scope.scopeExcludedConceptFilter = "";
				$scope.getPagedScopeExcludedConcepts(1);
			};	

			// element-specific functions for filtering
			// don't want to search id or objectId

			function containsAdviceFilter(element) {

				// check if advice filter is empty
				if ($scope.adviceFilter === "" || $scope.adviceFilter == null) return true;

				// otherwise check if upper-case advice filter matches upper-case element name or detail
				if ( element.detail.toString().toUpperCase().indexOf( $scope.adviceFilter.toString().toUpperCase()) != -1) return true;
				if ( element.name.toString().toUpperCase().indexOf( $scope.adviceFilter.toString().toUpperCase()) != -1) return true;

				// otherwise return false
				return false;
			}

			function containsRelationFilter(element) {

				// check if relation filter is empty
				if ($scope.relationFilter === "" || $scope.relationFilter == null) return true;

				// otherwise check if upper-case relation filter matches upper-case element name or detail
				if ( element.terminologyId.toString().toUpperCase().indexOf( $scope.relationFilter.toString().toUpperCase()) != -1) return true;
				if ( element.name.toString().toUpperCase().indexOf( $scope.relationFilter.toString().toUpperCase()) != -1) return true;

				// otherwise return false
				return false;
			}

			function containsPrincipleFilter(element) {

				// check if principle filter is empty
				if ($scope.principleFilter === "" || $scope.principleFilter == null) return true;

				// otherwise check if upper-case principle filter matches upper-case element name or detail
				if ( element.principleId.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				if ( element.detail.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				if ( element.name.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				if ( element.sectionRef.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;

				// otherwise return false
				return false;
			}

			function containsScopeConceptFilter(element) {

				// check if scopeConcept filter is empty
				if ($scope.scopeConceptFilter === "" || $scope.scopeConceptFilter == null) return true;

				// otherwise check if upper-case scopeConcept filter matches upper-case element name or detail
				if ( element.scopeConceptId.toString().toUpperCase().indexOf( $scope.scopeConceptFilter.toString().toUpperCase()) != -1) return true;
				if ( element.name.toString().toUpperCase().indexOf( $scope.scopeConceptFilter.toString().toUpperCase()) != -1) return true;

				// otherwise return false
				return false;
			}		

			function containsScopeExcludedConceptFilter(element) {

				// check if scopeConcept filter is empty
				if ($scope.scopeExcludesConceptFilter === "" || $scope.scopeExcludesConceptFilter == null) return true;

				// otherwise check if upper-case scopeConcept filter matches upper-case element name or detail
				if ( element.scopeExcludesConceptId.toString().toUpperCase().indexOf( $scope.scopeExcludesConceptFilter.toString().toUpperCase()) != -1) return true;
				if ( element.name.toString().toUpperCase().indexOf( $scope.scopeExcludesConceptFilter.toString().toUpperCase()) != -1) return true;

				// otherwise return false
				return false;
			}		

			// helper function to sort a JSON array by field

			$scope.sortByKey = function sortById(array, key) {
				return array.sort(function(a, b) {
					var x = a[key]; var y = b[key];
					return ((x < y) ? -1 : ((x > y) ? 1 : 0));
				});
			};



			////////////////////////////////////////////////////////
			// END PRINCIPLE/ADVICE SORTING/FILTERING FUNCTIONS
			////////////////////////////////////////////////////////
		}]);








