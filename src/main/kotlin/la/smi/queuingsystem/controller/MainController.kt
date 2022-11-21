package la.smi.queuingsystem.controller

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.stage.Stage
import la.smi.queuingsystem.Main
import la.smi.queuingsystem.back.manager.MainDispatcher


class MainController {
    @FXML
    private lateinit var edit_text_bids: TextField

    @FXML
    private lateinit var edit_text_gens: TextField

    @FXML
    private lateinit var edit_text_works: TextField

    @FXML
    private lateinit var edit_text_buff: TextField

    @FXML
    private lateinit var edit_text_alpha: TextField

    @FXML
    private lateinit var edit_text_beta: TextField

    @FXML
    private lateinit var edit_text_lambda: TextField

    private lateinit var model: MainDispatcher

    @FXML
    fun onStartModelling(actionEvent: ActionEvent) {
        var N_BIDS = 0
        var N_GENS = 0
        var N_WORK = 0
        var S_BUFF = 0

        var alpha = 0.0
        var beta = 0.0
        var lambda = 0.0

        var alert: Alert? = null
        try {
            N_BIDS = edit_text_bids.text.toString().toInt()
        } catch (e: NumberFormatException) {
            edit_text_bids.text = ""
            alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Incorrect Format!"
            alert.headerText = null
            alert.contentText = "Not a number in Bids"
        }
        try {
            N_GENS = edit_text_gens.text.toString().toInt()
        } catch (e: NumberFormatException) {
            edit_text_gens.text = ""
            alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Incorrect Format!"
            alert.headerText = null
            alert.contentText = "Not a number in Generators"
        }
        try {
            N_WORK = edit_text_works.text.toString().toInt()
        } catch (e: NumberFormatException) {
            edit_text_works.text = ""
            alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Incorrect Format!"
            alert.headerText = null
            alert.contentText = "Not a number in Workers"
        }
        try {
            S_BUFF = edit_text_buff.text.toString().toInt()
        } catch (e: NumberFormatException) {
            edit_text_buff.text = ""
            alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Incorrect Format!"
            alert.headerText = null
            alert.contentText = "Not a number in Size of Buffer"
        }
        try {
            alpha = edit_text_alpha.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            edit_text_alpha.text = ""
            alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Incorrect Format!"
            alert.headerText = null
            alert.contentText = "Not a number in alpha! Ex: 1.2"
        }
        try {
            beta = edit_text_beta.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            edit_text_beta.text = ""
            alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Incorrect Format!"
            alert.headerText = null
            alert.contentText = "Not a number in beta! Ex: 1.2"
        }
        try {
            lambda = edit_text_lambda.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            edit_text_lambda.text = ""
            alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Incorrect Format!"
            alert.headerText = null
            alert.contentText = "Not a number in lambda! Ex: 1.2"
        }

        if (alert != null) {
            alert.show()
        } else {
            model = MainDispatcher(N_BIDS, N_GENS, N_WORK, S_BUFF, alpha, beta, lambda)
            model.run()
            startSceneStepByStep(actionEvent)
        }
    }

    private fun startSceneStepByStep(actionEvent: ActionEvent) {
        val scene: Parent = FXMLLoader.load(Main::class.java.getResource("debug-view.fxml"))
        val stage: Stage = (actionEvent.source as Button).scene.window as Stage
        stage.userData = model
        stage.scene = Scene(scene)
        stage.show()
    }
}
