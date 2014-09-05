'use strict';


//Declare app level module
var mapProjectApp = angular.module('mapProjectApp', ['ngRoute',
                                                     'mapProjectAppControllers',
                                                     'adf',  
                                                     'mapProjectApp.widgets.metadataList',
                                                     'mapProjectApp.widgets.mapProject',
                                                     'mapProjectApp.widgets.mapRecord',
                                                     'mapProjectApp.widgets.mapEntry',  
                                                     'mapProjectApp.widgets.assignedList', 
                                                     'mapProjectApp.widgets.editedList',  
                                                     'mapProjectApp.widgets.workAvailable',
                                                     'mapProjectApp.widgets.terminologyBrowser',
                                                     'mapProjectApp.widgets.compareRecords',
                                                     'mapProjectApp.widgets.projectDetails',
                                                     'mapProjectApp.widgets.projectRecords',
                                                     'mapProjectApp.widgets.projectAdmin',
                                                     'mapProjectApp.widgets.recordConcept',
                                                     'mapProjectApp.widgets.recordSummary',
                                                     'mapProjectApp.widgets.recordAdmin',
                                                     'mapProjectApp.widgets.feedback',
                                                     'mapProjectApp.widgets.feedbackConversation',
                                                     'mapProjectApp.widgets.mapProjectAdmin',
                                                     'LocalStorageModule',
                                                     'ngCookies',
                                                     'ui.tinymce',
                                                     'angularjs-dropdown-multiselect'
                                                     ])
                                                     .value('prefix', '')
                                                     .config(function(dashboardProvider){

                                                    	 dashboardProvider
                                                    	 .structure('6-6', {
                                                    		 rows: [{
                                                    			 columns: [{
                                                    				 styleClass: 'col-md-6'
                                                    			 }, {
                                                    				 styleClass: 'col-md-6'
                                                    			 }]
                                                    		 }]
                                                    	 })
                                                    	 .structure('4-8', {
                                                    		 rows: [{
                                                    			 columns: [{
                                                    				 styleClass: 'col-md-4',
                                                    				 widgets: []
                                                    			 }, {
                                                    				 styleClass: 'col-md-8',
                                                    				 widgets: []
                                                    			 }]
                                                    		 }]
                                                    	 })
                                                    	 .structure('12/4-4-4', {
                                                    		 rows: [{
                                                    			 columns: [{
                                                    				 styleClass: 'col-md-12'
                                                    			 }]
                                                    		 }, {
                                                    			 columns: [{
                                                    				 styleClass: 'col-md-4'
                                                    			 }, {
                                                    				 styleClass: 'col-md-4'
                                                    			 }, {
                                                    				 styleClass: 'col-md-4'
                                                    			 }]
                                                    		 }]
                                                    	 })
                                                    	 .structure('12/6-6', {
                                                    		 rows: [{
                                                    			 columns: [{
                                                    				 styleClass: 'col-md-12'
                                                    			 }]
                                                    		 }, {
                                                    			 columns: [{
                                                    				 styleClass: 'col-md-6'
                                                    			 }, {
                                                    				 styleClass: 'col-md-6'
                                                    			 }]
                                                    		 }]
                                                    	 })
                                                    	 .structure('12/6-6/12', {
                                                    		 rows: [{
                                                    			 columns: [{
                                                    				 styleClass: 'col-md-12'
                                                    			 }]
                                                    		 }, {
                                                    			 columns: [{
                                                    				 styleClass: 'col-md-6'
                                                    			 }, {
                                                    				 styleClass: 'col-md-6'
                                                    			 }]
                                                    		 }, {
                                                    			 columns: [{
                                                    				 styleClass: 'col-md-12'
                                                    			 }]
                                                    		 }]
                                                    	 });

                                                     });


mapProjectApp.config(['$routeProvider',
                      function($routeProvider) {

	//////////////////////////////
	// DASHBOARDS
	//////////////////////////////

	$routeProvider.when('/:role/dash', {
		templateUrl: 'partials/otf-dashboard.html',
		controller: 'dashboardCtrl'
	});

	//////////////////////////////
	// MAPPING SERVICES
	//////////////////////////////

	$routeProvider.when('/project/records', {
		templateUrl: 'partials/otf-dashboard.html',
		controller: 'ProjectRecordsDashboardCtrl'
	});

	$routeProvider.when('/project/details', {
		templateUrl: 'partials/otf-dashboard.html', 
		controller: 'ProjectDetailsDashboardCtrl'
	});

	$routeProvider.when('/record/conceptId/:conceptId', {
		templateUrl: 'partials/otf-dashboard.html',
		controller: 'RecordConceptDashboardCtrl'
	});
	
	$routeProvider.when('/conversation/recordId/:recordId', {
		templateUrl: 'partials/otf-dashboard.html',
		controller: 'FeedbackConversationsDashboardCtrl'
	});

	$routeProvider.when('/record/recordId/:recordId', {
		templateUrl: 'partials/otf-dashboard.html',
		controller: 'MapRecordDashboardCtrl'
	});

	$routeProvider.when('/record/conflicts/:recordId', {
		templateUrl: 'partials/otf-dashboard.html',
		controller: 'ResolveConflictsDashboardCtrl'
	});
	
	$routeProvider.when('/record/review/:recordId', {
		templateUrl: 'partials/otf-dashboard.html',
		controller: 'ResolveConflictsDashboardCtrl'
	});



	//////////////////////////////
	// HELP PAGES
	//////////////////////////////

	$routeProvider.when('/help/:type', {
		templateUrl: function(params){ return 'partials/doc/' + params.type; }
	});


	///////////////////////////////
	// HOME and ERROR ROUTES
	///////////////////////////////

	$routeProvider.when('/', {
		templateUrl: 'partials/login.html',
		controller: 'LoginCtrl'
	});

	$routeProvider.otherwise({
		redirectTo: 'partials/error.html'
	});
}]);