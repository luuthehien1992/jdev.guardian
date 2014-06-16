
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdev.guardian.service;

import android.app.Activity;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Context;
import android.content.Intent;

import android.os.Vibrator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jdev.cordova.bluetooth.ble.IBeaconPacket;

import static android.content.Context.NOTIFICATION_SERVICE;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.List;

import jdev.guardian.GuardianApp;
import jdev.guardian.R;

import org.apache.cordova.*;

/**
 *
 * @author Jeremy
 */
public abstract class GuardianService extends Service {

    protected static Activity Activity = null;
    protected static CallbackContext CallbackContext = null;
    protected static final Boolean setCallbackLock = true;

    protected static boolean isServiceRunning = false;

    protected boolean isThreadRunning = true;

    protected final long PACKET_TIMEOUT = 2;        // sec
    protected final long VIBRATOR_DELAY = 2;        // sec
    protected final long VIBRATOR_TIME = 1000;      // 1 sec
    protected final long ACTIVE_TIMEOUT = 3;          // sec

    public final static int NOTIFICATION_ID = 141;
    protected final String NOTIFICATION_BLUETOOTH_IS_DISABLE = "Bluetooth is disabled. The service will be stop.";
    protected final int NOTIFICATION_DELAY = 2;    // sec
    protected final String NOTIFICATION_END_SERVICE = "End guardian service";
    protected final String NOTIFICATION_RUNNING = "Guardian service is running";
    protected final String NOTIFICATION_START_SERVICE = "Start guardian service";
    protected final String NOTIFICATION_TITLE = "Guardian service";
    protected final String TAG = "GuardianService";

    protected List<Long> lastTimeActive;
    protected List<Long> notifiDelayList;          // sec
    protected List<List<Double>> rawB;
    protected List<MACInfo> targetList;
    protected List<Long> vibratorDelayList;        // sec

    public static void setCallbackContext(CallbackContext callbackContext) {
        synchronized (setCallbackLock) {
            GuardianService.CallbackContext = callbackContext;
        }
    }

    public static void setActivity(Activity Activity) {
        GuardianService.Activity = Activity;
    }

    protected Notification createNotification(String contentText, String ticker) {
        return createNotification(contentText, ticker, R.drawable.icon);
    }

    protected Notification createNotification(String contentText, String ticker, int iconID) {
        return createNotification(NOTIFICATION_TITLE, contentText, ticker, iconID);
    }

    protected Notification createNotification(String contentTitle, String contentText, String ticker, int iconID) {
        Intent intent = new Intent(getBaseContext(), GuardianApp.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, 0);

        return createNotification(contentTitle, contentText, ticker, iconID, pendingIntent);
    }

    protected Notification createNotification(String contentTitle, String contentText, String ticker, int iconID,
            PendingIntent pendingIntent) {
        Builder builder = new Builder(this);

        builder.setContentTitle(contentTitle);
        builder.setContentText(contentText);
        builder.setTicker(ticker);
        builder.setSmallIcon(iconID);
        builder.setContentIntent(pendingIntent);

        return builder.build();
    }

    protected void showNotification(Notification notification, int id) {
        synchronized (notifiDelayList) {
            if (isTimeOut(notifiDelayList.get(id), NOTIFICATION_DELAY)) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(id, notification);

                notifiDelayList.set(id, System.currentTimeMillis() / 1000);
            }
        }
    }

    protected void showNotification(Notification notification) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    protected void showWarningNotification(String contentText, String ticker) {
        Notification notification = createNotification(contentText, ticker);
        showWarningNotification(notification);
    }

    protected void showNotification(String contentText, String ticker, int id) {
        Notification notification = createNotification(contentText, ticker);

        showNotification(notification, id);
    }

    protected void showWarningNotification(Notification notification) {
        showNotification(notification);

        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VIBRATOR_TIME);
    }

    protected void showWarningNotification(Notification notification, int id) {
        showNotification(notification, id);

        vibrator(id);
    }

    protected void vibrator(int id) {
        synchronized (vibratorDelayList) {
            if (isTimeOut(vibratorDelayList.get(id), VIBRATOR_DELAY)) {
                Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

                v.vibrate(VIBRATOR_TIME);
                vibratorDelayList.set(id, System.currentTimeMillis() / 1000);
            }
        }
    }

    protected void showWarningNotification(String contentText, String ticker, int id) {
        Notification notification = createNotification(contentText, ticker);

        showWarningNotification(notification, id);
    }

    protected void bluetoothDisableCallBack() {
        Result result = new Result(ResultStatus.BluetoothDisabled, new MACInfo(), 0.0);

        callBack(result);
    }

    protected void callBack(Result result) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result.toJson());
        pluginResult.setKeepCallback(true);

        CallbackContext.sendPluginResult(pluginResult);
    }

    protected List<MACInfo> getMACInfoList(List<MACInfo> list) {
        List<MACInfo> MACInfos = new ArrayList<MACInfo>();

        for (MACInfo mACInfo : list) {
            if (mACInfo.isEnable()) {
                MACInfos.add(mACInfo);
            }
        }

        return MACInfos;
    }

    protected List<MACInfo> getMACInfoList(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<MACInfo>>() {
        }
                .getType();

        return gson.fromJson(json, listType);
    }

    protected List<MACInfo> getMACInfos(String json) {
        List<MACInfo> mACInfos = getMACInfoList(json);

        return getMACInfoList(mACInfos);
    }

    protected void initList() {
        lastTimeActive = new ArrayList<Long>();
        notifiDelayList = new ArrayList<Long>();
        vibratorDelayList = new ArrayList<Long>();
        rawB = new ArrayList<List<Double>>();

        for (MACInfo targetList1 : targetList) {
            lastTimeActive.add((Long) System.currentTimeMillis() / 1000);
            notifiDelayList.add((Long) System.currentTimeMillis() / 1000);
            vibratorDelayList.add((Long) System.currentTimeMillis() / 1000);
            rawB.add(new ArrayList<Double>());
        }
    }

    protected boolean isTimeOut(long timeStamp, long sec) {
        long now = System.currentTimeMillis() / 1000;

        return (now - timeStamp > sec);
    }

    public static boolean isServiceRunning() {
        return isServiceRunning;
    }

    protected int isTarget(IBeaconPacket iBeaconPacket) {
        for (int i = 0; i < targetList.size(); i++) {
            MACInfo mACInfo = targetList.get(i);

            if (mACInfo.getMAC().equals(iBeaconPacket.getMac())) {
                return i;
            }
        }

        return -1;
    }

}
