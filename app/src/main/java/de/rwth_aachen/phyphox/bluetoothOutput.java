package de.rwth_aachen.phyphox;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;


//The bluetoothInput class encapsulates a generic serial output to bluetooth devices
public class bluetoothOutput implements Serializable {
    public Vector<dataInput> data = new Vector<>(); //Data-buffers

    private static final UUID btUUID = UUID.fromString("245fb312-a57f-40a1-9c45-9287984f270c");

    transient private BluetoothAdapter btAdapter = null;
    transient BluetoothDevice btDevice = null;
    transient private BluetoothSocket btSocket = null;
    transient private BufferedOutputStream outStream = null;

    private Protocol protocol;
    private String deviceName;
    private String deviceAddress;

    public class bluetoothException extends Exception {
        public bluetoothException(String message) {
            super(message);
        }
    }

    protected bluetoothOutput(String deviceName, String deviceAddress, Vector<dataInput> buffers, Protocol protocol) throws bluetoothException{
        this.protocol = protocol;

        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;

        this.data = buffers;

        findDevice();
    }

    public void findDevice() throws bluetoothException {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null)
            throw new bluetoothException("Could not find a bluetooth adapter.");
        if (!btAdapter.isEnabled())
            throw new bluetoothException("Bluetooth is disabled. Please enable bluetooth and try again");

        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        for (BluetoothDevice d : devices) {
            if (deviceAddress == null || deviceAddress.isEmpty()) {
                if (d.getName().equals(deviceName)) {
                    btDevice = d;
                    break;
                }
            } else {
                if (d.getAddress().equals(deviceAddress)) {
                    btDevice = d;
                    break;
                }
            }
        }

        if (btDevice == null)
            throw new bluetoothException("Bluetooth device not found. (name filter: " + deviceName + ", address filter: " + deviceAddress);
    }

    public void openConnection() throws bluetoothException {
        if (btDevice != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    try {
                        btSocket = (BluetoothSocket) btDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(btDevice, 1);
                        //                    btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(btUUID);
                    } catch (Exception e2) {
                        Log.e("phyphox", "Could not create insecure RfcommSocket for bluetooth device.");
                    }
                }
                if (btSocket == null)
                    btSocket = btDevice.createRfcommSocketToServiceRecord(btUUID);
            } catch (IOException e) {
                throw new bluetoothException("Could not create bluetooth socket.");
            }

            try {
                btSocket.connect();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {

                }
                throw new bluetoothException("Could not open bluetooth connection: " + e.getMessage());
            }
        }

        if (btSocket == null)
            throw new bluetoothException("Bluetooth connection opened successfully, but socket is null.");

        try {
            outStream = new BufferedOutputStream(btSocket.getOutputStream());
        } catch (IOException e) {
            throw new bluetoothException("Could get input stream from bluetooth device.");
        }
    }

    public void reconnect(BluetoothAdapter bta) throws bluetoothException {
        this.btAdapter = bta;
        if (btDevice == null)
            findDevice();
        if (btSocket == null || !btSocket.isConnected())
            openConnection();
    }

    public void closeConnection() {
        try {
            outStream.close();
        } catch (Exception e) {

        } finally {
            outStream = null;
        }

        try {
            btSocket.close();
        } catch (Exception e) {

        } finally {
            btSocket = null;
        }
    }

    //Check if the sensor is available without trying to use it.
    public boolean isAvailable() {
        return (btDevice != null);
    }

    //This is called when new data should be written to the device
    public void sendData() {
        Vector<Vector<Double>> outData = new Vector<>();
        for (dataInput d : data) {
            outData.add(new Vector<Double>());
            Iterator<Double> it = d.getIterator();
            while (it.hasNext()) {
                outData.lastElement().add(it.next());
            }
        }
        protocol.send(outStream, outData);
    }
}