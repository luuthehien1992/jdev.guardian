/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdev.cordova.bluetooth.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

/**
 *
 * @author Jeremy
 */
public class BLE {

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothManager bluetoothManager = null;
    private IBeaconPacket iBeaconPacket = new IBeaconPacket(0, "AA", 0, 1); // create dummy object
    private Activity activity = null;

    public BLE(Activity activity) {
        this.activity = activity;
        bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    public boolean isDiscovering() {
        return mBluetoothAdapter.isDiscovering();
    }

    public void startIbeaconDiscover() {
        mBluetoothAdapter.startLeScan(ibeaconDiscoverCallback);
    }

    public boolean isEnable() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public void enable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, 0);
    }

    public void endIbeaconDiscover() {
        mBluetoothAdapter.stopLeScan(ibeaconDiscoverCallback);
    }

    public IBeaconPacket getIBeaconPacket() {
        synchronized (iBeaconPacket) {
            return iBeaconPacket;
        }
    }

    private boolean isIBeaconPacket(byte[] scanRecord) {
        try {
            if (scanRecord[2] == (byte) 0x4C && scanRecord[3] == (byte) 0x00 && scanRecord[4] == (byte) 0x02 && scanRecord[5] == (byte) 0x15) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private final BluetoothAdapter.LeScanCallback ibeaconDiscoverCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, final int rssi,
                final byte[] scanRecord) {

            if (isIBeaconPacket(scanRecord)) {
                synchronized (iBeaconPacket) {
                    long timestamp = System.currentTimeMillis() / 1000;
                    iBeaconPacket = new IBeaconPacket(rssi, device.getAddress(), scanRecord[26], timestamp);
                }
            }
        }
    };

}
