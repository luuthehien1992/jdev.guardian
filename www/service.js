 var ServiceHelper = function() {
    };

    ServiceHelper.prototype.startMetter = function(macInfos, warningRange, maximumRange, A1, B1) {
        cordova.exec(
                null, // success callback function
                null, // error callback function
                'ServiceHelper', // mapped to our native Java class
                'startMetter', // with this action name
                [
                    macInfos,
                    warningRange,
                    maximumRange,
                    A1,
                    B1
                ]
                );
    };

    ServiceHelper.prototype.startRSSI = function(macInfos, imme, near, mid, far) {
        cordova.exec(
                null, // success callback function
                null, // error callback function
                'ServiceHelper', // mapped to our native Java class
                'startRSSI', // with this action name
                [
                    macInfos,
                    imme,
                    near,
                    mid,
                    far
                ]
                );
    };

    ServiceHelper.prototype.stop = function() {
        cordova.exec(
                null, // success callback function
                null, // error callback function
                'ServiceHelper', // mapped to our native Java class
                'stop', // with this action name
                [
                ]
                );
    };

    ServiceHelper.prototype.isRunning = function(successCallback) {
        cordova.exec(
                successCallback, // success callback function
                null, // error callback function
                'ServiceHelper', // mapped to our native Java class
                'isRunning', // with this action name
                [
                ]
                );
    };


    ServiceHelper.prototype.setNotificationCallback = function(successCallback) {
        cordova.exec(
                successCallback, // success callback function
                null, // error callback function
                'ServiceHelper', // mapped to our native Java class
                'setNotificationCallback', // with this action name
                [
                ]
                );
    };

    ServiceHelper.prototype.removeNotificationCallback = function() {
        cordova.exec(
                null, // success callback function
                null, // error callback function
                'ServiceHelper', // mapped to our native Java class
                'removeNotificationCallback', // with this action name
                [
                ]
                );
    };

    var service = new ServiceHelper();
    module.exports = service;