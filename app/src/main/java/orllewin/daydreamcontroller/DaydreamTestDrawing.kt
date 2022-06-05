package orllewin.daydreamcontroller

import coracle.Drawing
import orllewin.daydreamcontroller.bluetooth.ControllerEvent

class DaydreamTestDrawing: Drawing() {

    private var event: ControllerEvent? = null
    private val controllerDiameter = 255

    private var volumeUpPressed = false
    private var volumeDownPressed = false

    override fun setup() {
        strokeWeight(2)
        noStroke()
    }

    fun draw(event: ControllerEvent) {
        this.event = event
    }

    override fun draw() {
        drawController()
        event?.let{ event ->
            val touchX = event.touchX()
            val touchY = event.touchY()
            if(touchX + touchY > 0) {
                fill(0xffffff)
                circle((width / 2) - 128 + touchX, touchY + (controllerDiameter / 2), 20)
            }

            if(event.touchpadPressed()){
                noFill()
                stroke(0xffffff)
                circle((width / 2) - 128 + touchX, touchY + (controllerDiameter / 2), 30)
                noStroke()
            }

            volumeUpPressed = event.volumeUpPressed()
            volumeDownPressed = event.volumeDownPressed()

            if(event.appButtonPressed()){
                noStroke()
                fill(0xffffff)
                circle(width/2, (controllerDiameter * 2) - controllerDiameter/5, (controllerDiameter/5) - 10)

                noFill()
                stroke(0xffffff)
                circle(width/2, (controllerDiameter * 2) - controllerDiameter/5, (controllerDiameter/5))

                noStroke()
            }

            if(event.daydreamButtonPressed()){
                noStroke()
                fill(0xffffff)
                circle(width/2, (controllerDiameter * 2) + controllerDiameter/3, (controllerDiameter/5) - 10)

                noFill()
                stroke(0xffffff)
                circle(width/2, (controllerDiameter * 2) + controllerDiameter/3, (controllerDiameter/5))

                noStroke()
            }
        }
    }


    private fun drawController(){

        if(volumeUpPressed){
            noStroke()
            fill(0x000000)
            circle((width/2) + (controllerDiameter/2), controllerDiameter, 20)

            noFill()
            stroke(0x000000)
            circle((width/2) + (controllerDiameter/2), controllerDiameter, 30)

            noStroke()
        }

        if(volumeDownPressed){
            noStroke()
            fill(0x000000)
            circle((width/2) + (controllerDiameter/2), controllerDiameter + (controllerDiameter/2), 20)

            noFill()
            stroke(0x000000)
            circle((width/2) + (controllerDiameter/2), controllerDiameter + (controllerDiameter/2), 30)

            noStroke()
        }

        fill(0x666666)
        circle(width/2, controllerDiameter, controllerDiameter/2)
        rect(width/2 - controllerDiameter/2, controllerDiameter, width/2 + controllerDiameter/2, controllerDiameter * 3)
        circle(width/2, (controllerDiameter * 3), controllerDiameter/2)

        fill(0x343434)
        circle(width/2, controllerDiameter, (controllerDiameter/2)-10)
        circle(width/2, (controllerDiameter * 2) - controllerDiameter/5, controllerDiameter/5)
        circle(width/2, (controllerDiameter * 2) + controllerDiameter/3, controllerDiameter/5)
    }
}