package com.optoma.testbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import trikita.anvil.Anvil;
import trikita.anvil.RenderableView;
import static trikita.anvil.DSL.*;


public class MainActivity extends Activity {

    private BluetoothAdapter btAdapter;
    private List<BluetoothDevice> devices = new ArrayList<>();
    public static final String TAG = "TestBluetooth";
    private int state;
    private int scanMode;
    private BluetoothInputDevice bid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        state = btAdapter.getState();
        scanMode = btAdapter.getScanMode();

        setContentView(createView());

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bReciever, filter);

        if(!btAdapter.isDiscovering()) {
            btAdapter.startDiscovery();
        }

        createInputDeviceProfile();
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
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Anvil.render();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Anvil.render();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Log.d("ken", String.format("bluetooth device: %s ( %s )", device.getName(), device.getAddress()));

                    // BluetoothDevice#createRfcommSocketToServiceRecord}
                    // BluetoothAdapter#listenUsingRfcommWithServiceRecord}

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

                            Util.makeDiscoverable(btAdapter, 300);

                            btAdapter.startDiscovery();
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
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final BluetoothDevice bd = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch(bondState) {
                        case BluetoothDevice.BOND_NONE:
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            devices.remove(bd);
                            break;
                    }

                    Anvil.render();
                    break;
                default:
                    break;
            }

        }
    };

    public void createInputDeviceProfile() {
        final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if(profile == Util.INPUT_DEVICE) {
                    bid = (BluetoothInputDevice) proxy;
                    Anvil.render();
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if(profile == Util.INPUT_DEVICE) {
                    bid = null;
                    Anvil.render();
                }
            }
        };

        btAdapter.getProfileProxy(this, profileListener, Util.INPUT_DEVICE);

    }

    private static String getDeviceName(String s) {
        return (s == null) ? "Unknown Device" : s;
    }

    private RenderableView createView() {
        return new RenderableView(this) {
            @Override
            public void view() {
                linearLayout(() -> {
                    orientation(LinearLayout.VERTICAL);

                    linearLayout(() -> {
                        orientation(LinearLayout.HORIZONTAL);

                        // title
                        textView(() -> {
                            textSize(92);
                            text("Bluetooth");
                        });

                        switchView(() -> {
                            checked(btAdapter.isEnabled());
                            enabled(state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_OFF);

                            onCheckedChange((CompoundButton buttonView, boolean isChecked) -> {
                                if(isChecked) {
                                    btAdapter.enable();
                                } else {
                                    btAdapter.disable();
                                }
                            });
                        });

                    });

                    textView(() -> {
                        textSize(64);
                        text(String.format("Discovering: %b", btAdapter.isDiscovering()));
                    });

                    textView(() -> {
                        textSize(64);

                        boolean connectable = false;
                        boolean discoverable = false;

                        switch(scanMode) {
                            case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                                connectable = true;
                                discoverable = true;
                                break;
                            case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                                connectable = true;
                                break;
                        }

                        text("Connectable: "+connectable);
                        text("Discoverable: "+discoverable);
                    });

                    scrollView(() -> {

                        linearLayout(() -> {
                            orientation(LinearLayout.VERTICAL);

                            textView(() -> {
                                textSize(64);
                                text(String.format("Paired Devices (%d):",
                                        btAdapter.getBondedDevices().size()));
                            });

                            if(bid != null) {
                                final List<BluetoothDevice> connected = bid.getConnectedDevices();
                            }

                            for(final BluetoothDevice bd: btAdapter.getBondedDevices()) {
                                linearLayout(() -> {
                                    textView(() -> {
                                        text(String.format(
                                                "%s", getDeviceName(bd.getName())));
                                    });

                                    space(() -> {
                                        weight(1);
                                    });

                                    button(() -> {
                                        text("Unpair");

                                        onClick((view) -> {
                                            Util.removeBond(bd);
                                        });
                                    });

                                    if(bid != null) {
                                        button(() -> {
                                            if(bid.getConnectedDevices().contains(bd)) {
                                                text("Disconnect");
                                                onClick((view) -> {
                                                    bid.disconnect(bd);
                                                    if(bid.getConnectedDevices().isEmpty()) { // TODO
                                                        btAdapter.startDiscovery();
                                                    }
                                                });
                                            } else {
                                                text("Connect");
                                                onClick((view) -> {
                                                    if(btAdapter.isDiscovering()) {
                                                        btAdapter.cancelDiscovery();
                                                    }
                                                    bid.connect(bd);
                                                });
                                            }

                                        });
                                    }

                                });


                            }

                            textView(() -> {
                                textSize(64);
                                text(String.format("Other Devices (%d):", devices.size()));
                            });

                            for(final BluetoothDevice bd : devices) {
                                linearLayout(() -> {
                                    textView(() -> {

                                        final String s = String.format("%s",
                                                getDeviceName(bd.getName()));
                                        text(s);

                                    });

                                    space(() -> {
                                        weight(1);
                                    });

                                    if(bd.getBondState() == BluetoothDevice.BOND_NONE ||
                                            bd.getBondState() == BluetoothDevice.BOND_BONDING) {
                                        button(() -> {
                                            text("Pair");
                                            enabled(bd.getBondState() == BluetoothDevice.BOND_NONE);

                                            onClick((view) -> {
                                                bd.createBond();
                                            });
                                        });
                                    }
                                });

                            }

                        });
                    });
                });
            }
        };
    }

}

