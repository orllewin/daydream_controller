package orllewin.daydreamcontroller.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import java.math.BigInteger
import java.util.*


const val SERVICE_UUID = "0000fe55-0000-1000-8000-00805f9b34fb"
const val CHARACTERISTIC = "00000001-1000-1000-8000-00805f9b34fb"

@SuppressLint("MissingPermission")
class Bluetooth(val context: Context, val onMessage: (message: String) -> Unit, val onChange: (message: String) -> Unit){

    private val manager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = manager.adapter
    private var controller: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    @SuppressLint("MissingPermission")
    fun logDevices() {
        val pairedDevices: Set<BluetoothDevice>? = adapter!!.bondedDevices
        pairedDevices?.let{
            if(pairedDevices.isEmpty()){
                log("No paired Bluetooth devices\n")
                onMessage("No paired Bluetooth devices\n")
            }else{
                log("Paired devices:\n")
                onMessage("Paired devices:")
                pairedDevices.forEach { device ->
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    log("Paired BT device: $deviceName ($deviceHardwareAddress)\n")
                    onMessage("$deviceHardwareAddress: $deviceName")
                }
                log("-----------------------------\n")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun findController(onFindController: (success: Boolean, message: String?) -> Unit){
        val pairedDevices: Set<BluetoothDevice>? = adapter!!.bondedDevices

        pairedDevices?.let{
            if(pairedDevices.isEmpty()){
                onFindController(false, "No paired Bluetooth devices")
            }else{
                pairedDevices.forEach { device ->
                    val name = device.name
                    if(name == "Daydream controller"){
                        controller = device
                        onFindController(true, null)
                    }
                }

                if(controller == null){
                    onFindController(false, "Could not find 'Daydream controller'")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(){
        gatt = controller!!.connectGatt(context, true, object: BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        onMessage("Connected to GATT Server")
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> onMessage("Disconnected from GATT Server")
                }

            }
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                gatt?.services?.forEachIndexed { index, service ->
                    onMessage("Service $index: ${service.uuid}")
                    log("Service $index: ${service.uuid}")
                    if(SERVICE_UUID == service.uuid.toString()){
                        startListening(service)
                    }
                }

            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                characteristic?.value?.let { bytes ->
                    log("onCharacteristicChanged $bytes")
                    onChange("onCharacteristicChanged $bytes")
                }
            }
        })



    }

    private fun startListening(service: BluetoothGattService?) {
        onMessage("\nstartListening: ${service?.uuid}")

        service?.characteristics?.forEach { characteristic ->
            if(characteristic.uuid.toString() == CHARACTERISTIC){
                onMessage("\nFound characteristic: ${characteristic.uuid}")
                this.characteristic = characteristic
            }
        }

        characteristic?.let{

            val success = gatt?.setCharacteristicNotification(characteristic, true)

            characteristic!!.descriptors.forEach { descriptor ->
                onMessage("descriptor: ${descriptor.uuid}: ${descriptor.describeContents()}")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt?.writeDescriptor(descriptor)
            }

            onMessage("Requesting characteristic notifications, success: $success")
        }
    }

    private fun log(message: String){
        println("DDreamController: $message")
    }


    fun cancel(){
        characteristic?.let{
            gatt?.setCharacteristicNotification(characteristic, false)
        }

        //todo - unregister bt
    }
}