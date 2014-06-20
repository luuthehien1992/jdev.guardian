/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdev.guardian.service;

import android.app.*;
import android.content.*;
import static android.content.Context.NOTIFICATION_SERVICE;
import android.os.*;
import android.util.*;
import jdev.cordova.bluetooth.ble.*;
import jdev.guardian.core.*;
import static jdev.guardian.service.GuardianService.CallbackContext;
import static jdev.guardian.service.GuardianService.NOTIFICATION_ID;
import static jdev.guardian.service.GuardianService.isServiceRunning;
import static jdev.guardian.service.GuardianService.setCallbackLock;

/**
 *
 * @author Jeremy
 */
public class GuardianServiceRSSI extends GuardianService {

    protected double imme;
    protected double near;
    protected double mid;
    protected double far;
    protected double deta = 0;

    protected void resultCallBack(MACInfo macInfo, double level) {
        Result result = new Result(ResultStatus.Missing, macInfo, level);

        callBack(result);
    }

    protected void initService(Intent intent) {
        isServiceRunning = true;

        String json = intent.getStringExtra("MACList");
        targetList = getMACInfos(json);

        imme = intent.getDoubleExtra("imme", -1);
        near = intent.getDoubleExtra("near", -1);
        mid = intent.getDoubleExtra("mid", -1);
        far = intent.getDoubleExtra("far", -1);

        initList();
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

    protected void dispose() {
        isThreadRunning = false;
        isServiceRunning = false;
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        dispose();

        super.onDestroy();

        Notification notification = createNotification(NOTIFICATION_END_SERVICE, NOTIFICATION_END_SERVICE);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);

        Log.d(TAG, "Service End");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected class GuardianServiceRunnable implements Runnable {

        /*
         *  Check time
         */
        protected boolean checkIBeaconPacket(IBeaconPacket iBeaconPacket) {
            //Log.d(TAG, "IBeaconPacket time: " + iBeaconPacket.getUnixTime() + " - Now: " + (System.currentTimeMillis() / 1000));
            if (iBeaconPacket.getUnixTime() + PACKET_TIMEOUT >= System.currentTimeMillis() / 1000) {
                return true;
            }
            return false;
        }

        protected void processLostTarget() {
            for (int i = 0; i < lastTimeActive.size(); i++) {
                //Log.d(TAG,
                //       "Compare last time: " + lastTimeActive.get(i) + " - " + (long) System.currentTimeMillis() / 1000);

                synchronized (setCallbackLock) {
                    if (isTimeOut(lastTimeActive.get(i), ACTIVE_TIMEOUT)) {
                        MACInfo mACInfo = targetList.get(i);

                        if (CallbackContext == null) {
                            showWarningNotification("Missing " + mACInfo.getName(), "Missing " + mACInfo.getName(), i);
                        } else {
                            resultCallBack(mACInfo, -1);
                        }
                        //Log.d(TAG, "Missing " + mACInfo.getMAC() + " - " + mACInfo.getName());
                    }
                }
            }
        }

        protected void inRangeCallBack(MACInfo macInfo, double range) {
            Result result = new Result(ResultStatus.InRange, macInfo, range);

            callBack(result);
        }

        protected void processOutRangeTarget(IBeaconPacket iBeaconPacket, MACInfo macInfo, int idx) {
            int i = 0;
            if (rawB.get(idx).size() < 20) {
                rawB.get(idx).add((double) iBeaconPacket.getRssi());
            } else {
                double rssi = FIRFilter.filter(rawB.get(idx), (double) iBeaconPacket.getRssi());

                rawB.get(idx).remove(0);
                rawB.get(idx).add((double) iBeaconPacket.getRssi());

                double level = calculateLevel(rssi);

                synchronized (setCallbackLock) {

                    if (CallbackContext == null) {
                        if (level < 0) {
                            showWarningNotification(macInfo.getName() + " " + "Out of range",
                                    macInfo.getName() + " " + "Out of range", idx);
                        } else {
                            String area = calculateArea(level);

                            showNotification(macInfo.getName() + " " + area, macInfo.getName() + " " + area, idx);
                        }
                    } else {
                        resultCallBack(macInfo, level);
                    }
                }
            }
        }

        protected String calculateArea(double level) {
            if (level == 0) {
                return "Immediately";
            }

            if (level == 1) {
                return "Near";
            }

            if (level == 2) {
                return "Middle";
            }

            return "FAR";
        }

        protected double calculateLevel(double rssi) {
            if (rssi >= imme + imme * 0.1) {
                return 0;
            }

            if (rssi < imme + imme * 0.1 && rssi >= near + near * 0.1) {
                return 1;
            }

            if (rssi < near + near * 0.1 && rssi >= mid + mid * 0.1) {
                return 2;
            }

            return 3;
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
                        //Log.d(TAG, iBeaconPacket.getMac() + " - " + ble.getIBeaconPacket().getUnixTime());

                        int idx = isTarget(iBeaconPacket);

                        if (idx >= 0) {
                            if (checkIBeaconPacket(iBeaconPacket)) {
                                //Log.d(TAG, "Set last time: " + idx + " - " + (long) System.currentTimeMillis() / 1000);
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
