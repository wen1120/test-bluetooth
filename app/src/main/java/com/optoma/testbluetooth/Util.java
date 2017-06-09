package com.optoma.testbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Created by ken.chou on 09/06/2017.
 */

public class Util {
    public static Method setScanMode;

    static {
        try {
            setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void makeDiscoverable(BluetoothAdapter adapter, int timeOut){
        try {
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeOut);
        } catch (Exception e) {
            Log.e("discoverable", e.getMessage());
        }
    }

}
