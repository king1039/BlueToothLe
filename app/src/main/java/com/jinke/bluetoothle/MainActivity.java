package com.jinke.bluetoothle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter adapter;
    private BluetoothGatt bluetoothGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        openBlueToothLe();
    }

    //打开蓝牙
    private void openBlueToothLe() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (null == adapter) {
            Toast.makeText(this, "没有蓝牙功能", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!adapter.isEnabled()) {
            adapter.enable();
        }
        startScan();
    }

    //开始扫描
    private void startScan() {
        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        for (BluetoothDevice bondedDevice : bondedDevices) {
            if ("你想要接收数据的已配对设备名称".equals(bondedDevice.getName().trim())) {
                connectDevice(bondedDevice);
            }
        }
    }

    //连接设备
    private void connectDevice(BluetoothDevice bondedDevice) {
        bluetoothGatt = bondedDevice.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (BluetoothGatt.STATE_CONNECTED == newState) {
                    bluetoothGatt = gatt;
                    gatt.discoverServices();
                } else if (BluetoothGatt.STATE_DISCONNECTED == newState) {
                    gatt.close();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic character : characteristics) {
                        enableNotification(gatt, service.getUuid(), character.getUuid());
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                byte[] value = characteristic.getValue();
                Log.i("BLE", "receive value ----------------------------");
                for (int i = 0; i < value.length; i++) {
                    Log.i("BLE", "character_value = " + value[i]);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothGatt.disconnect();
    }

    public boolean enableNotification(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID) {
        boolean success = false;
        BluetoothGattService service = gatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = findNotifyCharacteristic(service, characteristicUUID);
            if (characteristic != null) {
                success = gatt.setCharacteristicNotification(characteristic, true);
                if (success) {
                    // 来源：http://stackoverflow.com/questions/38045294/oncharacteristicchanged-not-called-with-ble
                    for (BluetoothGattDescriptor dp : characteristic.getDescriptors()) {
                        if (dp != null) {
                            if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            }
                            gatt.writeDescriptor(dp);
                        }
                    }
                }
            }
        }
        return success;
    }

    private BluetoothGattCharacteristic findNotifyCharacteristic(BluetoothGattService service, UUID characteristicUUID) {
        BluetoothGattCharacteristic characteristic = null;
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                    && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }
        if (characteristic != null)
            return characteristic;
        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                    && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }
        return characteristic;
    }
}
