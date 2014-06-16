/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdev.guardian.service;

import android.app.*;
import android.content.*;
import static android.content.Context.NOTIFICATION_SERVICE;
import org.apache.cordova.*;
import org.json.*;

/**
 *
 * @author Jeremy
 */
public class ServiceHelper extends CordovaPlugin {

    private static Intent serviceIntent = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Action emAction = Action.fromString(action);

        switch (emAction) {
            case START_RSSI:
                startRSSI(callbackContext, args.getString(0), args.getDouble(1), args.getDouble(2), args.getDouble(3), args.getDouble(4));
                return true;
            case START_METTER:
                startMetter(callbackContext, args.getString(0), args.getDouble(1), args.getDouble(2), args.getDouble(3), args.getDouble(4));
                return true;
            case STOP:
                stop();
                return true;
            case IS_RUNNING:
                isRunning(callbackContext);
                return true;
            case SET_NOTIFICATION_CALLBACK:
                setNotificationCallback(callbackContext);
                return true;
            case REMOVE_NOTIFICATION_CALLBACK:
                removeNotificationCallback();
                return true;
        }
        return false;
    }

    private void removeNotificationCallback() {
        GuardianService.setCallbackContext(null);
    }

    private void setNotificationCallback(CallbackContext callbackContext) {
        GuardianService.setCallbackContext(callbackContext);
    }

    private void isRunning(CallbackContext callbackContext) {
        callbackContext.success(String.valueOf(isRunning()));
    }

    private boolean isRunning() {
        return GuardianService.isServiceRunning();
    }

    private void stop() {
        if (isRunning()) {
            NotificationManager notificationManager = (NotificationManager) cordova.getActivity().getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(GuardianService.NOTIFICATION_ID);
            cordova.getActivity().stopService(serviceIntent);
        }
    }

    private void startRSSI(CallbackContext callbackContext, String macListJson, double imme, double near, double mid, double far) {
        if (!isRunning()) {
            GuardianService.setActivity(cordova.getActivity());
            serviceIntent = new Intent(cordova.getActivity().getBaseContext(), GuardianServiceRSSI.class);

            serviceIntent.putExtra("MACList", macListJson);
            serviceIntent.putExtra("imme", imme);
            serviceIntent.putExtra("near", near);
            serviceIntent.putExtra("mid", mid);
            serviceIntent.putExtra("far", far);

            cordova.getActivity().startService(serviceIntent);
        }
    }

    private void startMetter(CallbackContext callbackContext, String macListJson, double warningRange, double maximumRange, double A1, double B1) {
        if (!isRunning()) {
            GuardianService.setActivity(cordova.getActivity());
            serviceIntent = new Intent(cordova.getActivity().getBaseContext(), GuardianServiceMetter.class);

            serviceIntent.putExtra("MACList", macListJson);
            serviceIntent.putExtra("WarningRange", warningRange);
            serviceIntent.putExtra("MaximumRange", maximumRange);
            serviceIntent.putExtra("A1", A1);
            serviceIntent.putExtra("B1", B1);
            cordova.getActivity().startService(serviceIntent);
        }
    }

    private enum Action {

        START_RSSI("startRSSI"),
        START_METTER("startMetter"),
        STOP("stop"),
        IS_RUNNING("isRunning"),
        SET_NOTIFICATION_CALLBACK("setNotificationCallback"),
        REMOVE_NOTIFICATION_CALLBACK("removeNotificationCallback");

        private final String statusCode;

        private Action(String s) {
            statusCode = s;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public static Action fromString(String action) {
            if (action != null) {
                for (Action emAction : Action.values()) {
                    if (action.equalsIgnoreCase(emAction.statusCode)) {
                        return emAction;
                    }
                }
            }
            return null;
        }
    }
}
