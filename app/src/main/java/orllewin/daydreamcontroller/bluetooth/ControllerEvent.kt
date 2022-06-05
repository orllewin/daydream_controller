package orllewin.daydreamcontroller.bluetooth

import android.hardware.SensorManager
import kotlin.math.pow

class ControllerEvent {
    var bytes: ByteArray? = null
    var hex: String? = null
    var hexDigitsSize = 0
    var binary: String? = null
    var binarySize = 0


    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)


    override fun toString(): String {
        val gyroscope = gyroscope()
        val magnetometer = magnetometer()
        val accelerometer = accelerometer()
        updateOrientation()
        return  "byte array size: ${bytes?.size}\n" +
                "hex: $hex\n" +
                "hexDigitsSize: $hexDigitsSize\n" +
                "binary: $binary\n" +
                "binarySize: $binarySize\n" +
                "volumeUpPressed: ${volumeUpPressed()}\n" +
                "volumeDownPressed: ${volumeDownPressed()}\n" +
                "appButtonPressed: ${appButtonPressed()}\n" +
                "daydreamButtonPressed: ${daydreamButtonPressed()}\n" +
                "touchpadPressed: ${touchpadPressed()}\n" +
                "touchX: ${touchX()}\n" +
                "touchY: ${touchY()}\n" +
                "gyroscope: ${gyroscope.x},${gyroscope.y},${gyroscope.z}\n" +
                "magnetometer: ${magnetometer.x},${magnetometer.y},${magnetometer.z}\n" +
                "accelerometer: ${accelerometer.x},${accelerometer.y},${accelerometer.z}\n" +
                "orientationAngles: ${orientationAngles[0]},${orientationAngles[1]},${orientationAngles[2]}\n" +
                ""
    }

    fun volumeUpPressed(): Boolean = binary!![147] == '1'
    fun volumeDownPressed(): Boolean = binary!![148] == '1'
    fun appButtonPressed(): Boolean = binary!![149] == '1'
    fun daydreamButtonPressed(): Boolean = binary!![150] == '1'
    fun touchpadPressed(): Boolean = binary!![151] == '1'
    fun touchX(): Int{
        binary?.let{
            return (binaryToDecimal(binary!!.substring(131, 139)))
        } ?: run {
            return -1
        }
    }
    fun touchY(): Int{
        binary?.let{
            return (binaryToDecimal(binary!!.substring(139, 147)))
        } ?: run {
            return -1
        }
    }
    fun gyroscope(): Vector{
        binary?.let{
            return Vector(
                x = Integer.parseInt(binary!!.substring(14, 27),2),
                y = Integer.parseInt(binary!!.substring(27, 40),2),
                z = Integer.parseInt(binary!!.substring(40, 53),2)
            )
        } ?: run {
            return Vector(-1, -1, -1)
        }
    }

    fun magnetometer(): Vector{
        binary?.let{
            return Vector(
                x = Integer.parseInt(binary!!.substring(53, 66),2),
                y = Integer.parseInt(binary!!.substring(66, 79),2),
                z = Integer.parseInt(binary!!.substring(79, 92),2)
            )
        } ?: run {
            return Vector(-1, -1, -1)
        }
    }

    fun accelerometer(): Vector{
        binary?.let{
            return Vector(
                x = Integer.parseInt(binary!!.substring(92, 105),2),
                y = Integer.parseInt(binary!!.substring(105, 118),2),
                z = Integer.parseInt(binary!!.substring(118, 131),2)
            )
        } ?: run {
            return Vector(-1, -1, -1)
        }
    }

    fun updateOrientation(){
        val accelerometer = accelerometer()
        accelerometerReading[0] = accelerometer.x.toFloat()
        accelerometerReading[1] = accelerometer.y.toFloat()
        accelerometerReading[2] = accelerometer.z.toFloat()

        val magnetometer = magnetometer()
        magnetometerReading[0] = magnetometer.x.toFloat()
        magnetometerReading[1] = magnetometer.y.toFloat()
        magnetometerReading[2] = magnetometer.z.toFloat()

        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
    }

    private fun binaryToDecimal(binary: String): Int {
        var num = binary.toLong()
        var decimalNumber = 0
        var i = 0
        var remainder: Long

        while (num.toInt() != 0) {
            remainder = num % 10
            num /= 10
            decimalNumber += (remainder * 2.0.pow(i.toDouble())).toInt()
            ++i
        }
        return decimalNumber
    }
}