package com.optoma.testbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import trikita.anvil.Anvil;
import trikita.anvil.RenderableView;
import static trikita.anvil.DSL.*;

//      ,----------------remove-------------------.
//     v                                           \
// [bonded]<--unbond--[bonded]<--disconnect--[connected]
//      \                                          ^
//       \________________add_____________________/

public class MainActivity extends Activity {

    public static final String TAG = "TestBluetooth";
    private static final int[] supportedProfiles = new int[] {
            // Util.INPUT_DEVICE,
            BluetoothProfile.A2DP,
            // BluetoothProfile.HEADSET
    };

    private BluetoothAdapter adapter;
    private List<BluetoothDevice> unbondedDevices = new ArrayList<>();
    private List<BluetoothDevice> connectedDevices = new ArrayList<>();
    private Map<Integer, BluetoothProfile> profiles = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = BluetoothAdapter.getDefaultAdapter();

        setContentView(render());

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothInputDevice.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(bReciever, filter);

        if(!adapter.isDiscovering()) {
            adapter.startDiscovery();
        }

        createProfiles();
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
                case BluetoothDevice.ACTION_FOUND: {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Log.d("ken", String.format("bluetooth device: %s ( %s )", device.getName(), device.getAddress()));

                    // BluetoothDevice#createRfcommSocketToServiceRecord}
                    // BluetoothAdapter#listenUsingRfcommWithServiceRecord}

                    unbondedDevices.add(device);
                    Anvil.render();
                    break;
                }
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    final int state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_ON:
                            Log.d("ken", "bluetooth on");
                            unbondedDevices.clear();

                            Util.makeDiscoverable(adapter, 300);

                            adapter.startDiscovery();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            Log.d("ken", "bluetooth off");
                            unbondedDevices.clear();
                            break;
                        case BluetoothAdapter.ERROR:
                            Log.d("ken", "bluetooth error");
                            unbondedDevices.clear();
                            break;
                    }
                    Anvil.render();
                    break;
                }
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    switch (adapter.getScanMode()) {
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
                    final int currentState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    final BluetoothDevice bd = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (currentState) {
                        case BluetoothDevice.BOND_NONE:
                            if (previousState == BluetoothDevice.BOND_BONDED) {
                                unbondedDevices.add(bd);
                            }
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            unbondedDevices.remove(bd);
                            connect(bd); // connect automatically once bonded
                            break;
                    }

                    Anvil.render();
                    break;

                case BluetoothDevice.ACTION_ACL_CONNECTED:
                case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.d("ken", "got ACL event!");
                    break;
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                case BluetoothInputDevice.ACTION_CONNECTION_STATE_CHANGED:
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED: {
                    final int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch(state) {
                        case BluetoothProfile.STATE_CONNECTED:
                            connectedDevices.add(device);
                            Log.d("ken", "device " + device.getName() + " connected");
                            break;
                        case BluetoothProfile.STATE_DISCONNECTED:
                            connectedDevices.remove(device);
                            Log.d("ken", "device " + device.getName() +" disconnected");
                            break;
                    }
                    Anvil.render();
                    break;
                }


                default:
                    break;
            }

        }
    };

    public void createProfiles() {

        final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                for(int i = 0; i<supportedProfiles.length; i++) {
                    if(supportedProfiles[i] == profile) {
                        profiles.put(profile, proxy);
                        Anvil.render();
                        return;
                    }
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if(profiles.containsKey(profile)) {
                    profiles.remove(profile);
                    Anvil.render();
                }
            }
        };

        for(int i = 0; i<supportedProfiles.length; i++) {
            adapter.getProfileProxy(this, profileListener, supportedProfiles[i]);
        }

    }

    private void connect(BluetoothDevice device) {
        if(adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
        for(BluetoothProfile profile : profiles.values()) {
            if(Util.connect(profile, device)) return;
        }
        Log.d("ken", "couldn't connect!");
    }

    private void disconnect(BluetoothDevice device) {
        for(BluetoothProfile profile : profiles.values()) {
            if (Util.disconnect(profile, device)) return;
        }
    }

    private boolean isConnected(BluetoothDevice device) {
        return connectedDevices.contains(device);
    }

    private RenderableView render() {
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
                            checked(adapter.isEnabled());
                            enabled(adapter.getState() == BluetoothAdapter.STATE_ON ||
                                    adapter.getState() == BluetoothAdapter.STATE_OFF);

                            onCheckedChange((CompoundButton buttonView, boolean isChecked) -> {
                                if(isChecked) {
                                    adapter.enable();
                                } else {
                                    adapter.disable();
                                }
                            });
                        });

                    });

                    textView(() -> {
                        textSize(64);
                        text(String.format("Discovering: %b", adapter.isDiscovering()));
                    });

                    textView(() -> {
                        textSize(64);

                        boolean connectable = false;
                        boolean discoverable = false;

                        switch(adapter.getScanMode()) {
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
                                        adapter.getBondedDevices().size()));
                            });

                            renderPairedDevices();

                            textView(() -> {
                                textSize(64);
                                text(String.format("Other Devices (%d):", unbondedDevices.size()));
                            });

                            renderUnpairedDevices();

                            editText(() -> {

                            });

                        });
                    });
                });
            }
        };
    }

    private void renderPairedDevices() {
        for(final BluetoothDevice device: adapter.getBondedDevices()) {
            linearLayout(() -> {
                textView(() -> {
                    text(String.format(
                            "%s", getDeviceName(device.getName())));
                });

                space(() -> {
                    weight(1);
                });

                button(() -> {
                    text("Unpair");

                    onClick((view) -> {
                        Util.removeBond(device);
                    });
                });


                button(() -> {
                    if(isConnected(device)) {
                        text("Disconnect");
                        onClick((view) -> {
                            disconnect(device);
                        });
                    } else {
                        text("Connect");
                        onClick((view) -> {
                            connect(device);
                        });
                    }

                });


            });


        }
    }

    private void renderUnpairedDevices() {
        for(final BluetoothDevice device : unbondedDevices) {
            linearLayout(() -> {
                textView(() -> {

                    final String s = String.format("%s",
                            getDeviceName(device.getName()));
                    text(s);

                });

                space(() -> {
                    weight(1);
                });

                if(device.getBondState() == BluetoothDevice.BOND_NONE ||
                        device.getBondState() == BluetoothDevice.BOND_BONDING) {
                    button(() -> {
                        text("Pair");
                        enabled(device.getBondState() == BluetoothDevice.BOND_NONE);

                        onClick((view) -> {
                            device.createBond();
                        });
                    });
                }
            });

        }
    }

    private static String getDeviceName(String s) {
        return (s == null) ? "Unknown Device" : s;
    }


}

