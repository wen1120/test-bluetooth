package com.optoma.testbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import trikita.anvil.Anvil;
import trikita.anvil.RenderableView;
import static trikita.anvil.DSL.*;


public class MainActivity extends Activity {

    private BluetoothAdapter bTAdapter;
    private boolean waiting = false;
    private List<BluetoothDevice> devices = new ArrayList<>();
    public static final String TAG = "TestBluetooth";
    private int state;
    private int scanMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bTAdapter = BluetoothAdapter.getDefaultAdapter();

        state = bTAdapter.getState();
        scanMode = bTAdapter.getScanMode();

        setContentView(new RenderableView(this) {
            @Override
            public void view() {
                linearLayout(() -> {
                    orientation(LinearLayout.VERTICAL);

                    textView(() -> {
                        textSize(80);
                        text(String.format("discovering: %b", bTAdapter.isDiscovering()));
                    });

                    textView(() -> {
                        textSize(80);

                        switch(scanMode) {
                            case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                                text("scan mode: connectable & discoverable");
                                break;
                            case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                                text("scan mode: connectable");
                                break;
                            case BluetoothAdapter.SCAN_MODE_NONE:
                                text("scan mode: none");
                                break;
                            default:
                                text("scan mode: ???");
                                break;
                        }

                    });

                    switchView(() -> {
                        checked(bTAdapter.isEnabled());
                        enabled(!waiting);

                        onCheckedChange((CompoundButton buttonView, boolean isChecked) -> {
                            if(isChecked) {
                                bTAdapter.enable();
                                waiting = true;
                            } else {
                                bTAdapter.disable();
                                waiting = true;
                            }
                        });
                    });

                    scrollView(() -> {
                        linearLayout(() -> {
                            orientation(LinearLayout.VERTICAL);

                            for(BluetoothDevice bd : devices) {
                                button(() -> {
                                    final String s = String.format("%s ( %s )", bd.getName(), bd.getAddress());
                                    text(s);
                                });
                            }

                        });
                    });
                });
            }
        });

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(bReciever, filter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bReciever);
    }

    private final BroadcastReceiver bReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch(action) {
                case BluetoothDevice.ACTION_FOUND:
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d("ken", String.format("bluetooth device: %s ( %s )", device.getName(), device.getAddress()));
                    devices.add(device);
                    Anvil.render();
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch(state) {
                        case BluetoothAdapter.STATE_ON:
                            Log.d("ken", "bluetooth on");
                            devices.clear();

                            Util.makeDiscoverable(bTAdapter, 300);

                            bTAdapter.startDiscovery();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            Log.d("ken", "bluetooth off");
                            devices.clear();
                            break;
                        case BluetoothAdapter.ERROR:
                            Log.d("ken", "bluetooth error");
                            devices.clear();
                            break;
                    }
                    waiting = false;
                    Anvil.render();
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    scanMode = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                    switch(scanMode) {
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            break;
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            break;
                    }
                    Anvil.render();
                    break;
                default:
                    break;
            }

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }


}

