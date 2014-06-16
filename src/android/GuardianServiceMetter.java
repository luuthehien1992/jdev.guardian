/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdev.guardian.service;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import jdev.cordova.bluetooth.ble.*;
import jdev.guardian.core.*;
import static jdev.guardian.service.GuardianService.NOTIFICATION_ID;
import static jdev.guardian.service.GuardianService.isServiceRunning;
import static jdev.guardian.service.GuardianService.setCallbackLock;

/**
 *
 * @author Jeremy
 */
public class GuardianServiceMetter extends GuardianService {

    protected double A1 = 0;
    protected double B1 = 0;
    protected double maximumRange = 0;
    protected double warningRange = 0;

    protected void missingCallBack(MACInfo macInfo) {
        Result result = new Result(ResultStatus.Missing, macInfo, -1);

        callBack(result);
    }

    protected void outOfRangeCallBack(MACInfo macInfo, double range) {
        Result result = new Result(ResultStatus.OutOfRange, macInfo, range);

        callBack(result);
    }

    protected void inWarningRangeCallBack(MACInfo macInfo, double range) {
        Result result = new Result(ResultStatus.InWarningRange, macInfo, range);

        callBack(result);
    }

    protected void inRangeCallBack(MACInfo macInfo, double range) {
        Result result = new Result(ResultStatus.InRange, macInfo, range);

        callBack(result);
    }

    protected void initService(Intent intent) {
        isServiceRunning = true;

        String json = intent.getStringExtra("MACList");
        targetList = getMACInfos(json);

        warningRange = intent.getDoubleExtra("WarningRange", -1);
        maximumRange = intent.getDoubleExtra("MaximumRange", -1);
        A1 = intent.getDoubleExtra("A1", -1);
        B1 = intent.getDoubleExtra("B1", -1);

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
                            missingCallBack(mACInfo);
                        }
                        //Log.d(TAG, "Missing " + mACInfo.getMAC() + " - " + mACInfo.getName());
                    }
                }
            }
        }

        protected void processOutRangeTarget(IBeaconPacket iBeaconPacket, MACInfo macInfo, int idx) {
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
