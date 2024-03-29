'use strict';

angular
  .module('mapProjectApp.widgets.mapEntry', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('mapEntry', {
      title : 'Map Entry',
      description : 'Edit module for a map entry',
      controller : 'mapEntryWidgetCtrl',
      templateUrl : 'js/widgets/mapEntry/mapEntry.html',
      edit : {}
    });
  })
  .controller(
    'mapEntryWidgetCtrl',
    function($scope, $rootScope, $q, $http, $routeParams, $modal, $location,
      $anchorScroll, localStorageService) {

      // for this widget, the only local storage service variable used is
      // user
      // token
      $scope.userToken = localStorageService.get('userToken');

      // watch for entry change
      $scope.$on('mapRecordWidget.notification.changeSelectedEntry', function(
        event, parameters) {
        console.debug("MapEntryWidget: Detected change in selected entry",
          parameters.entry);
        console.debug(parameters);
        $scope.entry = parameters.entry;
        $scope.record = parameters.record;
        $scope.project = parameters.project;

        console.debug("MapEntryWidget: mapRecord = ", $scope.record);

        // get the allowable advices
        $scope.allowableAdvices = getAllowableAdvices(parameters.entry,
          parameters.project.mapAdvice);
        sortByKey($scope.allowableAdvices, 'detail');
        $scope.allowableMapRelations = getAllowableRelations(parameters.entry,
          parameters.project.mapRelation);

        // set the rule to null if a non-rule-based project
        // added to catch any badly constructed rules from other widgets
        if ($scope.project.ruleBased == false)
          $scope.entry.rule = null;

        // compute relation and advice IFF a target or entry has
        // been set
        // attempt to autocompute the map relation, then update the
        // entry
        $scope.computeParameters(false);

      });

      // watch for entry deletion from map record page
      $scope
        .$on(
          'mapRecordWidget.notification.deleteSelectedEntry',
          function(event, parameters) {
            console
              .debug("MapEntryWidget: Detected delete notification from MapRecordWidget");
            console.debug(parameters);

            // if the currently viewed entry is the one being viewed,
            // clear the
            // displayed entry
            if (($scope.entry.localId === parameters.entry.localId
              && $scope.entry.localId != null && $scope.entry.localId != "")
              || ($scope.entry.id === parameters.entry.id
                && $scope.entry.id != null && $scope.entry.id != "")) {
              $scope.entry = null;
            }

          });

      // local variables
      $scope.isTargetOpen = true;
      $scope.isParametersOpen = true;
      $scope.localErrorRule = "";

      $scope.$watch('userToken', function() {

        $http.defaults.headers.common.Authorization = $scope.userToken;

      });

      // ///////////////////////////////////////
      // Save, Cancel, and Delete Functions //
      // //////////////////////////////////////

      // broadcasts an update from the map entry to the map record widget
      function updateEntry() {
        $rootScope.$broadcast(
          'mapEntryWidget.notification.modifySelectedEntry', {
            action : 'save',
            entry : angular.copy($scope.entry),
            record : $scope.record,
            project : $scope.project
          });
      }

      $scope.setTarget = function(targetCode) {

        $scope.getValidTargetError = "";

        // if target code is empty, compute parameters and return
        if (targetCode == null || targetCode == undefined || targetCode === '') {
          console.debug("Setting to empty target");
          $scope.entry.targetId = '';
          $scope.entry.targetName = 'No target';
          $scope.computeParameters(true);
          return;
        }

        $rootScope.glassPane++;

        $http(
          {
            url : root_mapping + "project/id/" + $scope.project.id
              + "/concept/" + targetCode + "/isValid",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(function(data) {
          $rootScope.glassPane--;

          // if target found and valid
          if (data) {
            $scope.entry.targetId = data.terminologyId;
            $scope.entry.targetName = data.defaultPreferredName;

            // attempt to autocompute the map relation, then update the
            // entry
            $scope.computeParameters(false);

          } else {

            $scope.getValidTargetError = targetCode + " is not a valid target";
            $scope.entry.targetName = null;

          }

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;

          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // watch for concept selection from terminology browser
      $scope
        .$on(
          'terminologyBrowser.selectConcept',
          function(event, parameters) {
            console
              .debug("MapEntryWidget: Detected selectConcept from terminologyBrowser");
            console.debug(parameters);

            // get the relative position of the inside of the map entry widget

            var rect = document.getElementById('mapEntryWidgetTop')
              .getBoundingClientRect();

            // scroll to (mapEntry left, mapEntry top + scroll offset -
            // header/widget header width)
            window.scrollTo(rect.left, rect.top + window.pageYOffset - 90);

            $scope.entry.targetId = parameters.concept.terminologyId;
            $scope.entry.targetName = parameters.concept.defaultPreferredName;

            // clear the relation and advices
            $scope.entry.mapRelation = null;
            $scope.entry.mapAdvice = [];

            // attempt to autocompute the map relation, then update the
            // entry
            $scope.computeParameters(false);

            // get the allowable advices and relations
            $scope.allowableAdvices = getAllowableAdvices($scope.entry,
              $scope.project.mapAdvice);
            sortByKey($scope.allowableAdvices, 'detail');
            $scope.allowableMapRelations = getAllowableRelations($scope.entry,
              $scope.project.mapRelation);
          });

      $scope.clearTargetConcept = function(entry) {
        console.debug("clearTargetConcept() called");
        entry.targetId = null;
        entry.targetName = null;
        entry.mapRelation = null;
        entry.mapAdvice = [];

        $rootScope.$broadcast('mapEntryWidget.notification.clearTargetConcept');

        // get the allowable advices and relations
        $scope.allowableAdvices = getAllowableAdvices($scope.entry,
          $scope.project.mapAdvice);
        sortByKey($scope.allowableAdvices, 'detail');
        $scope.allowableMapRelations = getAllowableRelations($scope.entry,
          $scope.project.mapRelation);

        // if project rule based, reset rule to TRUE
        if ($scope.project.ruleBased == true) {
          entry.rule = 'TRUE';
        }

        // attempt to autocompute the map relation, then update the entry
        $scope.computeParameters(false);

        // update the entry
        updateEntry();
      };

      function computeRelation(entry) {

        var deferred = $q.defer();

        // ensure mapRelation is deserializable
        if (entry.mapRelation === '' || entry.mapRelation === undefined)
          entry.mapRelation = null;

        $rootScope.glassPane++;
        $http({
          url : root_mapping + "relation/compute",
          dataType : "json",
          data : entry,
          method : "POST",
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(
          function(data) {

            if (data) {

              entry.mapRelation = data;

              console.debug("MapRelation computed: ", entry.mapRelation);

              // get the allowable advices and relations
              $scope.allowableAdvices = getAllowableAdvices(entry,
                $scope.project.mapAdvice);
              sortByKey($scope.allowableAdvices, 'detail');
              $scope.allowableMapRelations = getAllowableRelations(entry,
                $scope.project.mapRelation);
              $rootScope.glassPane--;

              // return the promise
              deferred.resolve(entry);
            } else {
              $rootScope.glassPane--;
              console.debug("No map relation computed for this entry");
              deferred.resolve(entry);
            }
          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);

          // reject the promise
          deferred.reject();

        });

        return deferred.promise;
      }

      function computeAdvice(entry) {

        var deferred = $q.defer();

        // ensure mapAdvice is deserializable
        if (entry.mapAdvice === '' || entry.mapAdvice === undefined)
          entry.mapAdvice = [];

        $rootScope.glassPane++;

        $http({
          url : root_mapping + "advice/compute",
          dataType : "json",
          data : entry,
          method : "POST",
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(
          function(data) {

            if (data) {

              entry.mapAdvice = data.mapAdvice;

              console.debug("Map advices computed: ", entry.mapAdvice);

              // get the allowable advices and relations
              $scope.allowableAdvices = getAllowableAdvices(entry,
                $scope.project.mapAdvice);
              sortByKey($scope.allowableAdvices, 'detail');
              $scope.allowableMapRelations = getAllowableRelations(entry,
                $scope.project.mapRelation);
              $rootScope.glassPane--;
              // return the promise
              deferred.resolve(entry);
            } else {
              $rootScope.glassPane--;
              console.debug("No map advice computed");
              deferred.resolve(entry);
            }
          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;

          $rootScope.handleHttpError(data, status, headers, config);

          // reject the promise
          deferred.reject();

        });

        return deferred.promise;
      }

      // ////////////////////////////////////
      // Rule Modal Constructor Functions //
      // ////////////////////////////////////

      // scope level function to open the modal constructor
      $scope.openRuleConstructor = function() {

        // clear any error regarding rule construction
        $scope.localErrorRule = "";

        var modalInstance = $modal.open({
          templateUrl : 'partials/rule-modal.html',
          controller : RuleConstructorModalCtrl,
          resolve : {
            presetAgeRanges : function() {
              return angular.copy($scope.project.mapAgeRange);
            },
            entry : function() {
              return angular.copy($scope.entry);
            }
          }
        });

        modalInstance.result
          .then(function(rule) {

            // set to true if rule returned with no value and display an
            // error
            if (rule == null || rule == undefined || rule === '') {
              rule = 'TRUE';
              $scope.localErrorRule = "Invalid rule constructed, setting rule to TRUE";
            }

            $scope.entry.rule = rule;
            $scope.entry.ruleSummary = $scope.getRuleSummary($scope.entry);

            // compute relation and advice (if any), then update entry
            $scope.computeParameters(false);
          });
      };

      // Returns a summary string for the entry rule type
      $scope.getRuleSummary = function(entry) {

        var ruleSummary = "";

        // first, rule summary
        if ($scope.project.ruleBased == true) {
          if (entry.rule.toUpperCase().indexOf("TRUE") != -1)
            ruleSummary += "[TRUE] ";
          else if (entry.rule.toUpperCase().indexOf("FEMALE") != -1)
            ruleSummary += "[FEMALE] ";
          else if (entry.rule.toUpperCase().indexOf("MALE") != -1)
            ruleSummary += "[MALE] ";
          else if (entry.rule.toUpperCase().indexOf("AGE") != -1) {

            var lowerBound = entry.rule.match(/(>= \d+ [a-zA-Z]*)/);
            var upperBound = entry.rule.match(/(< \d+ [a-zA-Z]*)/);

            ruleSummary += '[AGE ';

            if (lowerBound != null && lowerBound != '' && lowerBound.length > 0) {
              ruleSummary += lowerBound[0];
              if (upperBound != null && upperBound != ''
                && upperBound.length > 0)
                ruleSummary += ' AND ';
            }
            if (upperBound != null && upperBound != '' && upperBound.length > 0)
              ruleSummary += upperBound[0];

            ruleSummary += '] ';
          }
        }

        return ruleSummary;

      };

      // controller for the modal
      var RuleConstructorModalCtrl = function($scope, $http, $modalInstance,
        presetAgeRanges, entry) {

        $scope.ruleError = '';

        $scope.customAgeRange = {
          "name" : "",
          "lowerValue" : "",
          "lowerInclusive" : "false",
          "lowerUnits" : "years",
          "upperValue" : "",
          "upperInclusive" : "false",
          "upperUnits" : "years"
        };

        $scope.presetAgeRanges = presetAgeRanges;

        initializePresetAgeRanges();

        console.debug($scope.presetAgeRanges);
        console.debug(entry.rule);

        $scope.ruleCategories = [ 'TRUE', 'Gender - Male', 'Gender - Female',
          'Age - Chronological', 'Age - At Onset' ];

        if (entry != null && entry.rule != null) {
          if (entry.rule.indexOf('Male') > -1)
            $scope.ruleCategory = 'Gender - Male';
          else if (entry.rule.indexOf('Female') > -1)
            $scope.ruleCategory = 'Gender - Female';
          else if (entry.rule.indexOf('chronological') > -1)
            $scope.ruleCategory = 'Age - Chronological';
          else if (entry.rule.indexOf('onset') > -1)
            $scope.ruleCategory = 'Age - At Onset';
          else
            $scope.ruleCategory = 'TRUE';
        } else
          $scope.ruleCategory = 'TRUE';

        $scope.rule = entry.rule;

        $scope.saveRule = function() {
          $modalInstance.close($scope.rule, $scope.ruleSummary);
        };

        $scope.cancelRule = function() {
          $modalInstance.dismiss('cancel');
        };

        // alter the rule category, and construct
        $scope.changeRuleCategory = function(ruleCategory) {

          $scope.ageRange = null;
          $scope.constructRule(ruleCategory, null);
        };

        // construct actual text of rule based on category and age range (if
        // supplied)
        $scope.constructRule = function(ruleCategory, ageRange) {

          console.debug("Constructing rule", ruleCategory, ageRange);

          // clear the rule
          $scope.rule = "";

          // clear the rule error
          $scope.ruleError = "";

          // if a true rule
          if (ruleCategory === "TRUE") {
            $scope.rule = "TRUE";
          }

          // if a male gender rule
          else if (ruleCategory === "Gender - Male") {
            $scope.rule = "IFA 248153007 | Male (finding) |";
          }

          // if a female gender rule
          else if (ruleCategory === "Gender - Female") {
            $scope.rule = "IFA 248152002 | Female (finding) |";
          }

          // if an age range rule
          else if (ruleCategory === "Age - Chronological"
            || ruleCategory === "Age - At Onset") {

            // if age range not yet specified, do not construct rule
            if (ageRange == null || ageRange == undefined)
              return;

            // determine if lower and upper values are complete by checking
            // for null values
            var lowerValueValid = ageRange.lowerValue != "-1"
              && ageRange.lowerValue != undefined
              && ageRange.lowerValue != null && ageRange.lowerValue != "";
            var upperValueValid = ageRange.upperValue != "-1"
              && ageRange.upperValue != undefined
              && ageRange.lowerValue != null && ageRange.upperValue != "";

            // stop if neither value has been fully specified
            if (!lowerValueValid && !upperValueValid)
              return;

            console.debug("Validating age range", lowerValueValid,
              upperValueValid);

            // initialize calculated values (in days)
            var lowerValue = -1;
            var upperValue = -1;

            // calculate lower value based on units and verify greater than
            // zero
            if (lowerValueValid) {

              switch (ageRange.lowerUnits) {
              case "days":
                lowerValue = parseFloat(ageRange.lowerValue, 10);
                break;
              case "months":
                lowerValue = parseFloat(ageRange.lowerValue, 10) * 30;
                break;
              case "years":
                lowerValue = parseFloat(ageRange.lowerValue, 10) * 365;
                break;
              default:
                $scope.ruleError += "Unexpected error determining lower units\n";
              }

              if (lowerValue <= 0) {
                $scope.ruleError = "Lower bound value must be greater than zero\n";
                return;
              }
            }
            if (upperValueValid) {

              switch (ageRange.upperUnits) {
              case "days":
                upperValue = parseFloat(ageRange.upperValue, 10)
                break;
              case "months":
                upperValue = parseFloat(ageRange.upperValue, 10) * 30;
                break;
              case "years":
                upperValue = parseFloat(ageRange.upperValue, 10) * 365;
                break;
              default:
                $scope.ruleError += "Unexpected error determining upper units\n";
              }

              if (upperValue <= 0) {
                $scope.ruleError = "Upper bound value must be greater than zero\n";
                return;
              }
            }

            console.debug("Values", lowerValue, upperValue);

            // if both specified, check that upper value is greater than
            // lower value
            if (lowerValueValid && upperValueValid && lowerValue >= upperValue) {
              $scope.ruleError += "Upper bound value must be greater than lower bound value";
            }

            // base text for both lower and upper value sections
            var ruleText = (ruleCategory === "Age - Chronological") ? "IFA 424144002 | Current chronological age (observable entity)"
              : "IFA 445518008 | Age at onset of clinical finding (observable entity)";

            if (lowerValueValid) {
              $scope.rule += ruleText + " | "
                + (ageRange.lowerInclusive == true ? ">=" : ">") + " "
                + parseFloat(ageRange.lowerValue, 10).toFixed(1) + " "
                + ageRange.lowerUnits;
            }

            if (lowerValueValid && upperValueValid) {
              $scope.rule += " AND ";
            }

            if (upperValueValid) {
              $scope.rule += ruleText + " | "
                + (ageRange.upperInclusive == true ? "<=" : "<") + " "
                + parseFloat(ageRange.upperValue, 10).toFixed(1) + " "
                + ageRange.upperUnits;

            }
          } else
            $scope.rule = null;
        };

        $scope.constructRuleAgeHelper = function(ruleCategory, ageRange) {
          $scope.constructRule($scope.ruleCategory);
        };

        function initializePresetAgeRanges() {

          // set the preset age range strings
          for (var i = 0; i < $scope.presetAgeRanges.length; i++) {
            var presetAgeRangeStr = $scope.presetAgeRanges[i].name + ", ";

            if ($scope.presetAgeRanges[i].lowerValue != null
              && $scope.presetAgeRanges[i].lowerValue != "-1") {
              presetAgeRangeStr += ($scope.presetAgeRanges[i].lowerInclusive == true ? ">="
                : ">")
                + " "
                + $scope.presetAgeRanges[i].lowerValue
                + " "
                + $scope.presetAgeRanges[i].lowerUnits;
            }

            if ($scope.presetAgeRanges[i].lowerValue != null
              && $scope.presetAgeRanges[i].lowerValue != "-1"
              && $scope.presetAgeRanges[i].upperValue != null
              && $scope.presetAgeRanges[i].upperValue != "-1") {

              presetAgeRangeStr += " and ";
            }

            if ($scope.presetAgeRanges[i].upperValue != null
              && $scope.presetAgeRanges[i].upperValue != "-1") {

              presetAgeRangeStr += ($scope.presetAgeRanges[i].upperInclusive == true ? "<="
                : "<")
                + " "
                + $scope.presetAgeRanges[i].upperValue
                + " "
                + $scope.presetAgeRanges[i].upperUnits;
            }

            $scope.presetAgeRanges[i].stringName = presetAgeRangeStr;
          }
          ;
        }
        ;

      };

      // /////////////////////
      // Advice functions ///
      // /////////////////////

      // validates and adds advice to a map entry
      $scope.addEntryAdvice = function(entry, advice) {

        console.debug('ADDING ADVICE');
        // check if advice valid
        if (advice == '') {
          $scope.errorAddAdvice = "Advice cannot be empty";
        } else if (advice == null) {
          $scope.errorAddAdvice = "This advice is not found in allowable advices for this project";
        } else {
          $scope.errorAddAdvice = "";

          // check if this advice is already present
          var advicePresent = false;
          for (var i = 0; i < entry.mapAdvice.length; i++) {
            if (advice.id === entry.mapAdvice[i].id)
              advicePresent = true;
          }

          if (advicePresent == true) {
            $scope.errorAddAdvice = "This advice " + advice.detail
              + " is already attached to this entry";
          } else {
            $scope.entry['mapAdvice'].push(advice);
            $scope.adviceInput = "?";
          }
        }

        updateEntry();
      };

      // removes advice from a map entry
      $scope.removeEntryAdvice = function(entry, advice) {

        var confirmRemove = true;
        if (advice.isComputed == true) {
          confirmRemove = confirm("The advice you are removing was automatically computed for this entry.  Are you sure you want to do this?");
        }

        if (confirmRemove) {

          console.debug('Removing advice:');
          console.debug(advice);
          console.debug('Advices found:');
          for (var i = 0; i < entry['mapAdvice'].length; i++) {
            console.debug(entry['mapAdvice'][i]);
          }

          entry.mapAdvice = removeJsonElement(entry.mapAdvice, advice);

          console.debug('Advices after removal:');
          for (var i = 0; i < entry['mapAdvice'].length; i++) {
            console.debug(entry['mapAdvice'][i]);
          }

          updateEntry();
        }

      };

      // ///////////////////////
      // Relation functions ///
      // ///////////////////////

      $scope.selectMapRelation = function(mapRelation) {
        $scope.entry.mapRelation = mapRelation;

        // clear advice on relation change
        $scope.entry.mapAdvice = [];

        // compute advice (if any), then update entry
        computeAdvice($scope.entry).then(function() {
          updateEntry();
        });

      };

      $scope.clearMapRelation = function(mapRelation) {
        $scope.entry.mapRelation = null;

        $scope.computeParameters(false);
      };

      $scope.setNullTarget = function() {
        // open glass pane (setNullTarget1)
        $rootScope.glassPane++;
        $scope.entry.targetId = "";
        $scope.entry.targetName = 'No target';
        $scope.computeParameters(true);
        // close glass pane (setNullTarget1)
        $rootScope.glassPane--;
      };

      /**
       * Function to compute relation and advice for an entry Parameter:
       * ignoreNullValues false: if both target and relation are null, do
       * nothing true: make computation API calls regardless of target and
       * relation
       */
      $scope.computeParameters = function(ignoreNullValues) {

        var targetNotNull = $scope.entry.targetId != null
          && $scope.entry.targetId != undefined && $scope.entry.targetId != "";
        var relationNotNull = $scope.entry.mapRelation != null
          && $scope.entry.mapRelation != undefined
          && $scope.entry.mapRelation != "";

        console.debug("Computing parameters", targetNotNull, relationNotNull);

        // either target or relation must be non-null to compute
        // relation/advice
        if (targetNotNull || relationNotNull || ignoreNullValues) {

          computeRelation($scope.entry).then(function() {
            console.debug('Relation computed');
            computeAdvice($scope.entry).then(function() {
              console.debug('Advice computed');
              updateEntry();
            });
          });

          // set these to null for consistency
        } else {
          console.debug("Setting parameters to null");
          $scope.entry.targetId = null;
          $scope.entry.mapRelation = null;
        }
      };

      // Function for MapAdvice and MapRelations, returns allowable lists
      // based
      // on null target and element properties
      function getAllowableAdvices(entry, advices) {

        var allowableAdvices = [];

        // if target is null (i.e. not valid or empty), return empty list
        if (entry.targetId != null) {
          var nullTarget = (entry.targetId === "");

          for (var i = 0; i < advices.length; i++) {

            // do not add computed advices
            if (advices[i].isComputed == false) {

              // if empty target and allowable for null target OR
              // if valid target and not allowable for null target
              if ((nullTarget == true && advices[i].isAllowableForNullTarget == true)
                || (nullTarget == false && advices[i].isAllowableForNullTarget == false)) {

                // check that this advice is not already present on the
                // entry
                var advicePresent = false;
                for (var j = 0; j < entry.mapAdvice.length; j++) {
                  if (entry.mapAdvice[j].id === advices[i].id)
                    advicePresent = true;
                }

                // add advice if not already present
                if (advicePresent == false)
                  allowableAdvices.push(advices[i]);
              }
            }
          }
        }

        return allowableAdvices;
      }
      ;

      // Function for MapAdvice and MapRelations, returns allowable lists
      // based
      // on null target and element properties
      function getAllowableRelations(entry, relations) {

        var allowableRelations = [];

        // if the target is null (i.e. neither valid or empty)
        // return an empty list, otherwise calculate
        if (entry.targetId != null) {

          var nullTarget = entry.targetId === "";

          for (var i = 0; i < relations.length; i++) {

            if (relations[i].isComputed == false) {

              // console.debug(" Not computed");

              if ((nullTarget == true && relations[i].isAllowableForNullTarget == true)
                || (nullTarget == false && relations[i].isAllowableForNullTarget == false)) {

                // console.debug(" Valid");

                // handle a specific case where the name is more descriptive
                // than
                // the abbreviation
                relations[i].displayName = (relations[i].abbreviation === 'none' ? relations[i].name
                  : relations[i].abbreviation);

                allowableRelations.push(relations[i]);
              }
            }
          }
        }
        return allowableRelations;
      }
      ;

      // sort and return an array by string key
      function sortByKey(array, key) {
        return array.sort(function(a, b) {
          var x = a[key];
          var y = b[key];
          return ((x < y) ? -1 : ((x > y) ? 1 : 0));
        });
      }
      ;

      function removeJsonElement(array, elem) {

        console.debug("Removing element");
        var newArray = [];
        for (var i = 0; i < array.length; i++) {
          if (array[i].id != elem.id) {
            newArray.push(array[i]);
          }
        }
        return newArray;
      }

    });
