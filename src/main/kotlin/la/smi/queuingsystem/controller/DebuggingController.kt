package la.smi.queuingsystem.controller

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import la.smi.queuingsystem.Main
import la.smi.queuingsystem.back.manager.MainDispatcher
import la.smi.queuingsystem.back.request.SpecialEvent


class DebuggingController {

    private var model: MainDispatcher? = null
    private lateinit var events: ArrayList<SpecialEvent>
    private lateinit var currentEvent: SpecialEvent
    private var pointer: Int = 0

    private lateinit var systemTime: Text
    private var gens = VBox()
    private var workers = VBox()
    private var buffer = VBox()

    @FXML
    private lateinit var root: BorderPane

    @FXML
    fun onNextStep(actionEvent: ActionEvent) {
        if (model==null) {
//            val node: Node = actionEvent.source as Node
//            val scene = node.scene
//            val stage = scene.window
//            model = stage.userData as MainDispatcher
            model = MainDispatcher(30,5,2,10, 1.0,10.0,0.2)
            model!!.run()
            events = model!!.getResults()
            events.forEach{println(it)}
            initScene(model!!.N_GENS, model!!.N_WORK, model!!.S_BUFF)
        }
        if (pointer != events.size)
            currentEvent=events[pointer++]
        showEvent(currentEvent)
    }

    @FXML
    fun onPrevStep(actionEvent: ActionEvent) {
        if (model!=null) {
            if (pointer!=0){
                currentEvent = events[--pointer]
                showEvent(currentEvent)
            }
        }
    }

    private fun initScene(N_GENS: Int, N_WORKS: Int, S_BUF: Int) {
        systemTime = Text("TIME")
        systemTime.style = "-fx-fill: #f1f1f1; -fx-padding: 10 0 0 0; -fx-font-size: 18"
        BorderPane.setAlignment(systemTime, Pos.TOP_CENTER)

        // Generators blocks
        gens.alignment = Pos.CENTER
        gens.style= "-fx-padding: 0 0 0 40; -fx-spacing: 10"
        gens.prefWidth = 80.0
        for (g in 0 until N_GENS){
            val gen = Button()
            gen.id = "g-${g}"
            gen.text = "[${g}]"
            gen.style = "-fx-background-color: #f1f1f1; -fx-font-size: 14"
            gen.alignment = Pos.TOP_CENTER
            gen.minWidth = 80.0
            gen.minHeight = 40.0
            gen.textAlignment = TextAlignment.JUSTIFY
            gens.children.add(gen)
        }

        // Workers blocks
        workers.alignment = Pos.CENTER
        workers.style= "-fx-padding: 0 40 0 0; -fx-spacing: 10"
        workers.prefWidth = 80.0
        for (w in 0 until N_WORKS){
            val worker = Button()
            worker.id = "w-${w}"
            worker.text = "[${w}]"
            worker.style = "-fx-background-color: #f1f1f1; -fx-font-size: 14;"
            worker.alignment = Pos.TOP_CENTER
            worker.minWidth = 100.0
            worker.minHeight = 100.0
            worker.textAlignment = TextAlignment.JUSTIFY
            workers.children.add(worker)
        }

        // Buffer block
        buffer.alignment = Pos.CENTER
        buffer.style= "-fx-padding: 0 0 0 0; -fx-spacing: 10"
        buffer.prefWidth = 80.0
        for (b in 0 until S_BUF) {
            val buf = Button()
            buf.id = "b-${b}"
            buf.text = "[${b} EMPTY]"
            buf.style = "-fx-background-color: #ffffff; -fx-font-size: 14;"
            buf.alignment = Pos.TOP_CENTER
            buf.minWidth = 120.0
            buf.minHeight = 40.0
            buf.textAlignment = TextAlignment.JUSTIFY
            buffer.children.add(buf)
        }

        val cancel = Button()
        cancel.id = "cancel"
        cancel.text = "-"
        cancel.style = "-fx-background-color: #f1f1f1; -fx-font-size: 14; -fx-padding: 10 0 0 0;"
        cancel.alignment = Pos.TOP_CENTER
        cancel.minWidth = 80.0
        cancel.minHeight = 40.0
        cancel.textAlignment = TextAlignment.JUSTIFY
        buffer.children.add(cancel)

        root.top = systemTime
        root.center = buffer
        root.left = gens
        root.right = workers

    }

    private var prefNode: Button? = null
    private var prevText: String? = null
    private var prevEvent: SpecialEvent.Cause? = null
    private fun showEvent(event: SpecialEvent) {
        systemTime.text = event.time.toString()

        if (event.cause == SpecialEvent.Cause.START) systemTime.text = "Start:${event.time}"
        if (event.cause == SpecialEvent.Cause.STOP) systemTime.text = "Finish:${event.time}"

        if (prefNode != null && prevText!= null) {
            if (prevEvent == SpecialEvent.Cause.GENERATION || prevEvent == SpecialEvent.Cause.CANCEL){
                prefNode!!.style = "-fx-background-color: #f1f1f1; -fx-font-size: 14"
                prefNode!!.text = prevText
            }
            else if(prevEvent == SpecialEvent.Cause.START_OF_PRODUCING) {
                prefNode!!.style = "-fx-background-color: #B4E5BDFF; -fx-font-size: 14"
            }
            else if (prevEvent == SpecialEvent.Cause.FINISH_OF_PRODUCING) {
                prefNode!!.style = "-fx-background-color: #ffffff; -fx-font-size: 14"
                prefNode!!.text = prevText
            }
            else if (prevEvent == SpecialEvent.Cause.EXIT_OF_BUFFERING){
                prefNode!!.style = "-fx-background-color: #ffffff; -fx-font-size: 14"
                prefNode!!.text = prevText
            }
        }

        if (event.cause == SpecialEvent.Cause.GENERATION) {
            val gen: Button = gens.children[event.idDevice] as Button
            prefNode = gen
            prevText = gen.text
            prevEvent = event.cause

            gen.style = "-fx-background-color: #F6EDAEFF; -fx-font-size: 14"
            gen.text = prevText + " ${event.bid!!.id} "
        }
        if (event.cause == SpecialEvent.Cause.START_OF_PRODUCING) {
            val worker: Button = workers.children[event.idDevice] as Button
            prefNode = worker
            prevText = worker.text
            prevEvent = event.cause

            worker.style = "-fx-background-color: #57a566; -fx-font-size: 14"
            worker.text = prevText + " ${event.bid!!.id} "
        }
        if (event.cause == SpecialEvent.Cause.FINISH_OF_PRODUCING) {
            val worker: Button = workers.children[event.idDevice] as Button
            prefNode = worker
            prevText = "[${event.idDevice}]"
            prevEvent = event.cause

            worker.style = "-fx-background-color: #57a566; -fx-font-size: 14"
        }
        if (event.cause == SpecialEvent.Cause.CANCEL) {
            val cancel: Button = buffer.children[buffer.children.size-1] as Button
            prefNode = cancel
            prevText = cancel.text
            prevEvent = event.cause
            cancel.style = "-fx-background-color: #C46D39FF ; -fx-font-size: 14"
            cancel.text = " ${event.bid!!.id} "
        }
        if (event.cause == SpecialEvent.Cause.ENTIRE_OF_BUFFERING) {
            val buf: Button = buffer.children[event.idDevice] as Button
            prefNode = buf
            prevText = buf.text
            prevEvent = event.cause

            buf.style = "-fx-background-color: #F6EDAEFF; -fx-font-size: 14"
            buf.text = "[${event.idDevice}] ${event.bid!!.id} "
        }
        if (event.cause == SpecialEvent.Cause.EXIT_OF_BUFFERING) {
            val buf: Button = buffer.children[event.idDevice] as Button
            prefNode = buf
            prevText = "[${event.idDevice} EMPTY]"
            prevEvent = event.cause

            buf.style = "-fx-background-color: #57a566;; -fx-font-size: 14"
        }
    }

}