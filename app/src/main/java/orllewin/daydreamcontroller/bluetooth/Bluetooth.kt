package orllewin.daydreamcontroller.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlin.math.asin


const val SERVICE_UUID = "0000fe55-0000-1000-8000-00805f9b34fb"
const val CHARACTERISTIC = "00000001-1000-1000-8000-00805f9b34fb"

@SuppressLint("MissingPermission")
class Bluetooth(val context: Context, val onMessage: (message: String) -> Unit, val onChange: (event: ControllerEvent) -> Unit){

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
                    decode(bytes)
                }
            }
        })
    }

    val m = MadgwickAHRS(60f, 1f)

    private fun decode(bytes: ByteArray) {
        val controllerEvent = ControllerEvent()
        controllerEvent.bytes = bytes

        val hex = bytes.joinToString(""){ byte ->
            val hexString = Integer.toHexString(byte.toInt() and 0xff)
            if(hexString.length % 2 == 1){
                "0$hexString"
            }else{
                hexString
            }
        }

        controllerEvent.hex = hex

        val hexDigits = hex.chunked(2)

        controllerEvent.hexDigitsSize = hexDigits.size

        val binary = hexDigits.joinToString(""){ pair ->
            Integer.toBinaryString(pair.toInt(16)).padStart(8, '0')
        }

        controllerEvent.binary = binary
        controllerEvent.binarySize = binary.length

        //https://medium.com/hackernoon/how-i-hacked-google-daydream-controller-c4619ef318e4
        val gyroVector = Vector(
            x = Integer.parseInt(binary.substring(13, 26), 2),
            y = Integer.parseInt(binary.substring(26, 39), 2),
            z = Integer.parseInt(binary.substring(39, 52), 2))

        //log("gyroVector: x: ${gyroVector.x}, y: ${gyroVector.y}, z: ${gyroVector.z}")

        val accelerometerVector = Vector(
            x = Integer.parseInt(binary.substring(91, 104), 2),
            y = Integer.parseInt(binary.substring(104, 117), 2),
            z = Integer.parseInt(binary.substring(117, 130), 2))

        m.update(gyroVector.x.toFloat(), gyroVector.y.toFloat(), gyroVector.z.toFloat(), accelerometerVector.x.toFloat(), accelerometerVector.y.toFloat(), accelerometerVector.z.toFloat())
        val q = m.quaternion
        val ww = q[0] * q[0]
        val xx = q[1] * q[1]
        val yy = q[2] * q[2]
        val zz = q[3] * q[3]

        //https://github.com/xonoxitron/ahrs/blob/master/index.js
        val heading = kotlin.math.atan2(2 * (q[1] * q[2] + q[3] * q[0]), xx - yy - zz + ww)
        val pitch =  -asin(2 * (q[1] * q[3] - q[2] * q[0]))
        val roll = kotlin.math.atan2(q[2] * q[3] + q[1] * q[0], -xx - yy + zz + ww)
        //onChange("data: size: ${binary.length}:  $binary")

        val app = binary.substring(145, 146)
        val home = binary.substring(146, 147)
        val volumeUp = binary.substring(147, 148)
        val volumeDown = binary.substring(148, 149)
        val click = binary.substring(149, 150)

        val touchX = binary.substring(129, 137)

        onChange(controllerEvent)

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


    fun clear(){
        characteristic?.let{
            gatt?.setCharacteristicNotification(characteristic, false)
        }
        gatt?.disconnect()
    }
}