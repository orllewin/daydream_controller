package orllewin.daydreamcontroller.bluetooth

class ControllerEvent {
    var bytes: ByteArray? = null
    var hex: String? = null
    var hexDigitsSize = 0
    var binary: String? = null
    var binarySize = 0

    override fun toString(): String {
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

    fun binaryToDecimal(binary: String): Int {
        var num = binary.toLong()
        var decimalNumber = 0
        var i = 0
        var remainder: Long

        while (num.toInt() != 0) {
            remainder = num % 10
            num /= 10
            decimalNumber += (remainder * Math.pow(2.0, i.toDouble())).toInt()
            ++i
        }
        return decimalNumber
    }


}