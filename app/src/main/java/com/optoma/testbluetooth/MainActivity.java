package com.optoma.testbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private static final Map<Integer, Integer> supportedProfiles = new HashMap<>();
    static {
        supportedProfiles.put(0x1124, Util.INPUT_DEVICE);
        supportedProfiles.put(0x110b, BluetoothProfile.A2DP);
        supportedProfiles.put(0x111e, BluetoothProfile.HEADSET);
//        supportedProfiles.put(0x110e, Util.AVRCP_CONTROLLER);
    };

    private BluetoothAdapter adapter;
    private List<BluetoothDevice> unbondedDevices = new ArrayList<>();
    private Set<BluetoothDevice> connectedDevices = new HashSet<>();
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
        filter.addAction(BluetoothDevice.ACTION_UUID);
        registerReceiver(bReciever, filter);

        createProfiles();

        connectToBondedDevices();

        if(!adapter.isDiscovering()) {
            adapter.startDiscovery();
        }
    }

    private void connectToBondedDevices() {
        for(BluetoothDevice device : adapter.getBondedDevices()) {
            device.fetchUuidsWithSdp();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bReciever);
        for(int p : profiles.keySet()) {
            adapter.closeProfileProxy(p, profiles.get(p));
        }
        profiles.clear();
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
                    final BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(!unbondedDevices.contains(device)) {
                        unbondedDevices.add(device);
                    }
                    Log.d("ken", "found "+device);
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
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    final int currentState = intent.getIntExtra(
                            BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final int previousState = intent.getIntExtra(
                            BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    final BluetoothDevice bd =
                            (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (currentState) {
                        case BluetoothDevice.BOND_NONE:
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            unbondedDevices.remove(bd);
                            bd.fetchUuidsWithSdp(); // TODO: necessary?

                            break;
                    }

                    Anvil.render();
                    break;
                }

                // TODO: reliable?
                case BluetoothDevice.ACTION_ACL_CONNECTED: {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    connectedDevices.add(device);
                    Log.d("ken", device+" connected!");
                    Anvil.render();
                    break;
                }
                case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                case BluetoothDevice.ACTION_ACL_DISCONNECTED: {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    connectedDevices.remove(device);
                    Log.d("ken", device+" disconnected!");
                    Anvil.render();
                    break;
                }

//                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
//                case BluetoothInputDevice.ACTION_CONNECTION_STATE_CHANGED:
//                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED: {
//                    final int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
//                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    switch(state) {
//                        case BluetoothProfile.STATE_CONNECTED:
//                            connectedDevices.add(device);
//                            Log.d("ken", "device " + device.getName() + " connected");
//                            break;
//                        case BluetoothProfile.STATE_DISCONNECTED:
//                            connectedDevices.remove(device);
//                            Log.d("ken", "device " + device.getName() +" disconnected");
//                            break;
//                    }
//                    Anvil.render();
//                    break;
//                }
                case BluetoothDevice.ACTION_UUID: {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    final Parcelable[] pUuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                    Log.d("ken", "got action uuid for "+device);
                    connect(device, pUuids);

                    break;
                }



                default:
                    break;
            }

        }
    };

    // TODO: make it lazier?
    public void createProfiles() {

        final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if(supportedProfiles.values().contains(profile)) {
                    profiles.put(profile, proxy);
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if(profiles.containsKey(profile)) {
                    profiles.remove(profile);
                }
            }
        };

        for(int p : supportedProfiles.values()) {
            adapter.getProfileProxy(this, profileListener, p);
        }

    }

    // see https://www.bluetooth.com/specifications/assigned-numbers/service-discovery
    private void connect(BluetoothDevice device, Parcelable[] pUuids) { // TODO: failed?
        if(adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
        if(pUuids!=null) {
            for (int i = 0; i < pUuids.length; i++) {

                final Integer uuid16 = getUUID16((ParcelUuid) pUuids[i]);

                if(supportedProfiles.containsKey(uuid16)) {

                    final Integer p = supportedProfiles.get(uuid16);
                    if(profiles.containsKey(p)) {
                        final BluetoothProfile bp = profiles.get(p);
                        if(bp.getConnectionState(device) != BluetoothProfile.STATE_CONNECTED) {
                            if (Util.connect(bp, device)) {
                                return; // return on first success
                            }
                        }
                    }
                }
            }
        }
    }

    // see https://stackoverflow.com/questions/36212020/how-can-i-convert-a-bluetooth-16-bit-service-uuid-into-a-128-bit-uuid
    private int getUUID16(ParcelUuid pUuid) {
        final UUID uuid = pUuid.getUuid();
        final long left64 = uuid.getMostSignificantBits();
        return (int)((left64 >>> 32) & 0x0000ffff);
    }

    private void disconnect(BluetoothDevice device) {
        for(BluetoothProfile profile : profiles.values()) {
            if (profile.getConnectedDevices().contains(device)) {
                Util.disconnect(profile, device);
            }
        }
        Util.removeBond(device);
    }

    // TODO: not reliable
    // is connected by any profile
    private boolean isConnected(BluetoothDevice device) {
        // return connectedDevices.contains(device);
        for(BluetoothProfile profile : profiles.values()) {
            if (profile.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                return true;
            }
        }
        return false;
    }

    private boolean isConnectable() {
        boolean connectable = false;

        switch(adapter.getScanMode()) {
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                connectable = true;
                break;
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                connectable = true;
                break;
        }
        return connectable;
    }

    private boolean isDiscoverable() {
        boolean discoverable = false;

        switch(adapter.getScanMode()) {
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                discoverable = true;
                break;

        }
        return discoverable;
    }

    private RenderableView render() {
        return new RenderableView(this) {
            @Override
            public void view() {
                linearLayout(() -> {
                    orientation(LinearLayout.VERTICAL);
                    padding(dip(12), dip(12));

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
                        textSize(48);
                        text(String.format("Discovering: %b", adapter.isDiscovering()));
                    });

                    textView(() -> {
                        textSize(48);
                        text("Connectable: "+isConnectable());
                    });

                    textView(() -> {
                        textSize(48);
                        text("Discoverable: "+isDiscoverable());
                    });


                    scrollView(() -> {

                        linearLayout(() -> {
                            orientation(LinearLayout.VERTICAL);

                            textView(() -> {
                                textSize(64);
                                text(String.format("Devices (%d):",
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
                    text(String.format("%s (%s)",
                            getDeviceName(device.getName()), getDeviceClass(device)));


                    //text(String.format("%s %s",
                    //        getDeviceName(device.getName()),
                    //        isConnected(device) ? "(V)" : "(X)")
                    //);
                });

                space(() -> {
                    weight(1);
                });

                button(() -> {
                    text("Remove");

                    // enabled(isConnected(device));

                    onClick((view) -> {
                        disconnect(device);
                    });
                });


//                button(() -> {
//                    if(isConnected(device)) {
//                        text("Disconnect");
//                        onClick((view) -> {
//                            disconnect(device);
//                        });
//                    } else {
//                        text("Connect");
//                        onClick((view) -> {
//                            connect(device);
//                        });
//                    }
//
//                });


            });


        }
    }

    private void renderUnpairedDevices() {
        for(final BluetoothDevice device : unbondedDevices) {
            linearLayout(() -> {
                textView(() -> {

                    final String s = String.format("%s (%s)",
                            getDeviceName(device.getName()), getDeviceClass(device));
                    text(s);

                });

                space(() -> {
                    weight(1);
                });

                if(device.getBondState() == BluetoothDevice.BOND_NONE ||
                        device.getBondState() == BluetoothDevice.BOND_BONDING) {
                    button(() -> {
                        text("Add");
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

    private static String getDeviceClass(BluetoothDevice device) {
        if(device == null) return "Unknown";
        final int major = device.getBluetoothClass().getMajorDeviceClass();
        switch(major) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
                return "Audio/Video";
            case BluetoothClass.Device.Major.COMPUTER:
                return "Computer";
            case BluetoothClass.Device.Major.HEALTH:
                return "Health";
            case BluetoothClass.Device.Major.IMAGING:
                return "Imaging";
            case BluetoothClass.Device.Major.MISC:
                return "Misc";
            case BluetoothClass.Device.Major.NETWORKING:
                return "Networking";
            case BluetoothClass.Device.Major.PERIPHERAL:
                return "Peripheral";
            case BluetoothClass.Device.Major.PHONE:
                return "Phone";
            case BluetoothClass.Device.Major.TOY:
                return "Toy";
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                return "Uncategorized";
            case BluetoothClass.Device.Major.WEARABLE:
                return "Wearable";
            default:
                return "Unknown";
        }
    }


}

