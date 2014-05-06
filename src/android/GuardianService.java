
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

import android.os.IBinder;
import android.os.Vibrator;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jdev.cordova.bluetooth.ble.BLE;
import jdev.cordova.bluetooth.ble.IBeaconPacket;

import static android.content.Context.NOTIFICATION_SERVICE;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.List;

import jdev.guardian.GuardianApp;
import jdev.guardian.R;
import jdev.guardian.core.Calculator;
import jdev.guardian.core.FIRFilter;
import jdev.guardian.core.KalmanFilter;

import org.apache.cordova.*;

/**
 *
 * @author Jeremy
 */
public class GuardianService extends Service {

    private static Activity Activity = null;
    private static CallbackContext CallbackContext = null;
    private static final Boolean setCallbackLock = true;

    private static boolean isServiceRunning = false;

    private double A1 = 0;
    private double B1 = 0;
    private double maximumRange = 0;
    private double warningRange = 0;
    private boolean isThreadRunning = true;

    private final long PACKET_TIMEOUT = 2;        // sec
    private final long VIBRATOR_DELAY = 2;        // sec
    private final long VIBRATOR_TIME = 1000;      // 1 sec
    private final long ACTIVE_TIMEOUT = 3;          // sec

    public final static int NOTIFICATION_ID = 141;
    private final String NOTIFICATION_BLUETOOTH_IS_DISABLE = "Bluetooth is disabled. The service will be stop.";
    private final int NOTIFICATION_DELAY = 2;    // sec
    private final String NOTIFICATION_END_SERVICE = "End guardian service";
    private final String NOTIFICATION_RUNNING = "Guardian service is running";
    private final String NOTIFICATION_START_SERVICE = "Start guardian service";
    private final String NOTIFICATION_TITLE = "Guardian service";
    private final String TAG = "GuardianService";

    private List<Long> lastTimeActive;
    private List<Long> notifiDelayList;          // sec
    private List<List<Double>> rawB;
    private List<MACInfo> targetList;
    private List<Long> vibratorDelayList;        // sec

    public static void setCallbackContext(CallbackContext callbackContext) {
        synchronized (setCallbackLock) {
            GuardianService.CallbackContext = callbackContext;
        }
    }

    public static void setActivity(Activity Activity) {
        GuardianService.Activity = Activity;
    }

    private Notification createNotification(String contentText, String ticker) {
        return createNotification(contentText, ticker, R.drawable.icon);
    }

    private Notification createNotification(String contentText, String ticker, int iconID) {
        return createNotification(NOTIFICATION_TITLE, contentText, ticker, iconID);
    }

    private Notification createNotification(String contentTitle, String contentText, String ticker, int iconID) {
        Intent intent = new Intent(getBaseContext(), GuardianApp.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, 0);

        return createNotification(contentTitle, contentText, ticker, iconID, pendingIntent);
    }

    private Notification createNotification(String contentTitle, String contentText, String ticker, int iconID,
            PendingIntent pendingIntent) {
        Builder builder = new Builder(this);

        builder.setContentTitle(contentTitle);
        builder.setContentText(contentText);
        builder.setTicker(ticker);
        builder.setSmallIcon(iconID);
        builder.setContentIntent(pendingIntent);

        return builder.build();
    }

    private void showNotification(Notification notification, int id) {
        synchronized (notifiDelayList) {
            if (isTimeOut(notifiDelayList.get(id), NOTIFICATION_DELAY)) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(id, notification);

                notifiDelayList.set(id, System.currentTimeMillis() / 1000);
            }
        }
    }

    private void showNotification(Notification notification) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    private void showWarningNotification(String contentText, String ticker) {
        Notification notification = createNotification(contentText, ticker);
        showWarningNotification(notification);
    }

    private void showNotification(String contentText, String ticker, int id) {
        Notification notification = createNotification(contentText, ticker);

        showNotification(notification, id);
    }

    private void showWarningNotification(Notification notification) {
        showNotification(notification);

        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VIBRATOR_TIME);
    }

    private void showWarningNotification(Notification notification, int id) {
        showNotification(notification, id);

        vibrator(id);
    }

    private void vibrator(int id) {
        synchronized (vibratorDelayList) {
            if (isTimeOut(vibratorDelayList.get(id), VIBRATOR_DELAY)) {
                Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

                v.vibrate(VIBRATOR_TIME);
                vibratorDelayList.set(id, System.currentTimeMillis() / 1000);
            }
        }
    }

    private void showWarningNotification(String contentText, String ticker, int id) {
        Notification notification = createNotification(contentText, ticker);

        showWarningNotification(notification, id);
    }

    private void missingCallBack(MACInfo macInfo) {
        Result result = new Result(ResultStatus.Missing, macInfo, -1);

        callBack(result);
    }

    private void outOfRangeCallBack(MACInfo macInfo, double range) {
        Result result = new Result(ResultStatus.OutOfRange, macInfo, range);

        callBack(result);
    }

    private void inWarningRangeCallBack(MACInfo macInfo, double range) {
        Result result = new Result(ResultStatus.InWarningRange, macInfo, range);

        callBack(result);
    }

    private void inRangeCallBack(MACInfo macInfo, double range) {
        Result result = new Result(ResultStatus.InRange, macInfo, range);

        callBack(result);
    }

    private void bluetoothDisableCallBack() {
        Result result = new Result(ResultStatus.BluetoothDisabled, new MACInfo(), 0.0);

        callBack(result);
    }

    private void callBack(Result result) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result.toJson());
        pluginResult.setKeepCallback(true);

        CallbackContext.sendPluginResult(pluginResult);
    }

    private List<MACInfo> getMACInfoList(List<MACInfo> list) {
        List<MACInfo> MACInfos = new ArrayList<MACInfo>();

        for (MACInfo mACInfo : list) {
            if (mACInfo.isEnable()) {
                MACInfos.add(mACInfo);
            }
        }

        return MACInfos;
    }

    private List<MACInfo> getMACInfoList(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<MACInfo>>() {
        }
                .getType();

        return gson.fromJson(json, listType);
    }

    private List<MACInfo> getMACInfos(String json) {
        List<MACInfo> mACInfos = getMACInfoList(json);

        return getMACInfoList(mACInfos);
    }

    private void initService(Intent intent) {
        isServiceRunning = true;

        String json = intent.getStringExtra("MACList");
        targetList = getMACInfos(json);

        warningRange = intent.getDoubleExtra("WarningRange", -1);
        maximumRange = intent.getDoubleExtra("MaximumRange", -1);
        A1 = intent.getDoubleExtra("A1", -1);
        B1 = intent.getDoubleExtra("B1", -1);

        initList();
    }

    private void initList() {
        rawB = new ArrayList<List<Double>>();
        lastTimeActive = new ArrayList<Long>();
        notifiDelayList = new ArrayList<Long>();
        vibratorDelayList = new ArrayList<Long>();

        for (MACInfo targetList1 : targetList) {
            lastTimeActive.add((Long) System.currentTimeMillis() / 1000);
            notifiDelayList.add((Long) System.currentTimeMillis() / 1000);
            vibratorDelayList.add((Long) System.currentTimeMillis() / 1000);
            rawB.add(new ArrayList<Double>());
        }
    }

    private boolean isTimeOut(long timeStamp, long sec) {
        long now = System.currentTimeMillis() / 1000;

        return (now - timeStamp > sec);
    }

    public static boolean isServiceRunning() {
        return isServiceRunning;
    }

    private int isTarget(IBeaconPacket iBeaconPacket) {
        for (int i = 0; i < targetList.size(); i++) {
            MACInfo mACInfo = targetList.get(i);

            if (mACInfo.getMAC().equals(iBeaconPacket.getMac())) {
                return i;
            }
        }

        return -1;
    }

    private void dispose() {
        isThreadRunning = false;
        isServiceRunning = false;
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        dispose();

        super.onDestroy();

        Notification notification = createNotification(NOTIFICATION_END_SERVICE, NOTIFICATION_END_SERVICE);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);

        Log.d(TAG, "Service End");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Start");

        initService(intent);

        Notification notification = createNotification(NOTIFICATION_RUNNING, NOTIFICATION_START_SERVICE);

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        startForeground(NOTIFICATION_ID, notification);

        Thread thread = new Thread(new GuardianServiceRunnable());

        thread.start();

        return Service.START_FLAG_REDELIVERY;
    }

    private class GuardianServiceRunnable implements Runnable {

        /*
         *  Check time
         */
        private boolean checkIBeaconPacket(IBeaconPacket iBeaconPacket) {
            Log.d(TAG, "IBeaconPacket time: " + iBeaconPacket.getUnixTime() + " - Now: " + (System.currentTimeMillis() / 1000));
            if (iBeaconPacket.getUnixTime() + PACKET_TIMEOUT >= System.currentTimeMillis() / 1000) {
                return true;
            }
            return false;
        }

        private void processLostTarget() {
            for (int i = 0; i < lastTimeActive.size(); i++) {
                Log.d(TAG,
                        "Compare last time: " + lastTimeActive.get(i) + " - " + (long) System.currentTimeMillis() / 1000);

                synchronized (setCallbackLock) {
                    if (isTimeOut(lastTimeActive.get(i), ACTIVE_TIMEOUT)) {
                        MACInfo mACInfo = targetList.get(i);

                        if (CallbackContext == null) {
                            showWarningNotification("Missing " + mACInfo.getName(), "Missing " + mACInfo.getName(), i);
                        } else {
                            missingCallBack(mACInfo);
                        }
                        Log.d(TAG, "Missing " + mACInfo.getMAC() + " - " + mACInfo.getName());
                    }
                }
            }
        }

        private void processOutRangeTarget(IBeaconPacket iBeaconPacket, MACInfo macInfo, int idx) {
            if (rawB.get(idx).size() < 20) {
                rawB.get(idx).add((double) iBeaconPacket.getRssi());
            } else {
                double rssi = FIRFilter.filter(rawB.get(idx), (double) iBeaconPacket.getRssi());

                rawB.get(idx).remove(0);
                rawB.get(idx).add((double) iBeaconPacket.getRssi());

                double logd = Calculator.calculateLogD(rssi, A1, B1);
                logd = KalmanFilter.filter(logd);

                double distance = Calculator.calculateDistance(logd);

                synchronized (setCallbackLock) {
                    if (distance > maximumRange) {
                        if (CallbackContext == null) {
                            showWarningNotification(macInfo.getName() + " " + "Out of range",
                                    macInfo.getName() + " " + "Out of range", idx);
                        } else {
                            outOfRangeCallBack(macInfo, distance);
                        }
                    } else if (distance > warningRange) {
                        if (CallbackContext == null) {
                            showWarningNotification(macInfo.getName() + " " + "In warning range",
                                    macInfo.getName() + " " + "In warning range", idx);
                        } else {
                            inWarningRangeCallBack(macInfo, distance);
                        }
                    } else {
                        if (CallbackContext == null) {
                            showNotification(macInfo.getName() + " " + distance, macInfo.getName() + " " + distance, idx);
                        } else {
                            inRangeCallBack(macInfo, distance);
                        }
                    }
                }
            }
        }

        public void run() {

            Log.d(TAG, "Thread Run");

            BLE ble = new BLE(Activity);

            if (!ble.isEnable()) {
                synchronized (setCallbackLock) {
                    if (CallbackContext == null) {
                        showWarningNotification(NOTIFICATION_BLUETOOTH_IS_DISABLE, NOTIFICATION_BLUETOOTH_IS_DISABLE);
                    } else {
                        bluetoothDisableCallBack();
                    }
                }
                stopSelf();

                return;
            }

            ble.startIbeaconDiscover();

            while (isThreadRunning) {
                try {
                    if (!ble.isEnable()) {
                        synchronized (setCallbackLock) {
                            if (CallbackContext == null) {
                                showWarningNotification(NOTIFICATION_BLUETOOTH_IS_DISABLE, NOTIFICATION_BLUETOOTH_IS_DISABLE);
                            } else {
                                bluetoothDisableCallBack();
                            }
                        }
                        stopSelf();
                        return;
                    }

                    IBeaconPacket iBeaconPacket = ble.getIBeaconPacket();

                    synchronized (iBeaconPacket) {
                        Log.d(TAG, iBeaconPacket.getMac() + " - " + ble.getIBeaconPacket().getUnixTime());

                        int idx = isTarget(iBeaconPacket);

                        if (idx >= 0) {
                            if (checkIBeaconPacket(iBeaconPacket)) {
                                Log.d(TAG, "Set last time: " + idx + " - " + (long) System.currentTimeMillis() / 1000);
                                lastTimeActive.set(idx, (long) System.currentTimeMillis() / 1000);
                                processOutRangeTarget(iBeaconPacket, targetList.get(idx), idx);
                            }
                        }

                        processLostTarget();
                    }
                    Thread.sleep(100);

                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }
            ble.endIbeaconDiscover();

            Log.d(TAG, "Thread End");
        }
    }
}
