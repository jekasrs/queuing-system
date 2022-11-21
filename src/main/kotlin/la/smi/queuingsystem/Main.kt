package la.smi.queuingsystem

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage

class Main : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(Main::class.java.getResource("debug-view.fxml"))
        val scene = Scene(fxmlLoader.load(), 800.0, 500.0)
        stage.title = "Моделирование системы массового обслуживания!"
        stage.scene = scene

        val root = fxmlLoader.getRoot<BorderPane>()
        root.setOnMouseClicked { root.requestFocus() }

        stage.show()
    }
}

fun main() {
    Application.launch(Main::class.java)
}
