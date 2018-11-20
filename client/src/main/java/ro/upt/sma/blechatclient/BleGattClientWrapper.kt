package ro.upt.sma.blechatclient

import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import java.nio.charset.Charset
import java.util.*

class BleGattClientWrapper(private val context: Context) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

    private var gatt: BluetoothGatt? = null

    interface MessageListener {

        enum class ConnectionStatus {
            CONNECTED, DISCONNECTED
        }

        fun onMessageAdded(message: String)

        fun onMessageSent(message: String)

        fun onConnectionStateChanged(connectionStatus: ConnectionStatus)

        fun onError(message: String)

    }

    fun sendMessage(message: String, messageListener: MessageListener) {
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            messageListener.onError("Bluetooth not enable or available.")
            return
        }

        val bluetoothDevice = gatt!!.device

        if (bluetoothDevice == null) {
            messageListener.onError("Bluetooth device not available")
            return
        }

        if (gatt == null && gatt!!.getConnectionState(bluetoothDevice) != BluetoothAdapter.STATE_CONNECTED) {
            messageListener.onError("Bluetooth device not connected")
            return
        }

        val characteristic = gatt!!.getService(CHAT_SERVICE)
                .getCharacteristic(CHAT_POST_CHARACTERISTIC)

        characteristic.setValue(message.toByteArray(Charset.defaultCharset()))

        val success = gatt!!.writeCharacteristic(characteristic)

        if (success) {
            messageListener.onMessageSent(message)
        } else {
            messageListener.onError("Message was not sent")
        }
    }

    fun registerMessageListener(address: String, messageListener: MessageListener) {
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            messageListener.onError("Bluetooth not enable or available.")
            return
        }

        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)

        if (bluetoothDevice == null) {
            messageListener.onError("Bluetooth device not available")
            return
        }

        this.gatt = bluetoothDevice.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)

                if (newState == STATE_CONNECTED) {
                    // TODO 1: Once connection was established then trigger discover service

                    // TODO 2: Inform the message listener about CONNECTED value status

                } else {
                    // TODO 3: inform message listener about DISCONNECTED status
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)

                // TODO 4: get a handle for CHAT_ROOM_CHARACTERISTIC
                val characteristic: BluetoothGattCharacteristic? = null
                //
                gatt.setCharacteristicNotification(characteristic, true)


                val descriptor = characteristic!!.getDescriptor(CLIENT_CONFIG)
                // TODO 5: write descriptor for same characteristic to enable notifications
            }

            override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic) {
                super.onCharacteristicChanged(gatt, characteristic)

                messageListener.onMessageAdded(
                        String(characteristic.value, Charset.defaultCharset()))
            }
        })
    }

    fun unregisterListener() {
        if (gatt != null) {
            gatt!!.disconnect()
        }
    }

    companion object {

        private val TAG = BleGattClientWrapper::class.java!!.getSimpleName()

        val CHAT_SERVICE: UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
        val CHAT_ROOM_CHARACTERISTIC: UUID = UUID.fromString("0000000a-0000-1000-8000-00805f9b34fb")
        val CHAT_POST_CHARACTERISTIC: UUID = UUID.fromString("0000000b-0000-1000-8000-00805f9b34fb")

        /* Mandatory Client Characteristic Config Descriptor */
        var CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

}
