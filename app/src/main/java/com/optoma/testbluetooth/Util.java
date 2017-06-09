package com.optoma.testbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by ken.chou on 09/06/2017.
 */

public class Util {
    public static Method setScanMode;
    public static Method removeBond;

    // https://stackoverflow.com/questions/26341718/connection-to-specific-hid-profile-bluetooth-device
    public static final int INPUT_DEVICE = 4;

    static {
        try {
            setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            removeBond = BluetoothDevice.class.getMethod("removeBond");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    // ugly hack from https://stackoverflow.com/questions/8742760/how-to-programmatically-set-discoverable-time-without-user-confirmation
    // since the method is hidden.
    public static void makeDiscoverable(BluetoothAdapter adapter, int timeOut){
        try {
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeOut);
        } catch (Exception e) {
            Log.e("discoverable", e.getMessage());
        }
    }

    // https://stackoverflow.com/questions/9608140/how-to-unpair-or-delete-paired-bluetooth-device-programmatically-on-android
    public static void removeBond(BluetoothDevice device) {
        try {
            removeBond.invoke(device);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }



}
