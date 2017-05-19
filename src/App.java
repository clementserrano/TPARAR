import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by clementserrano on 15/05/2017.
 */
public class App extends Application {

    private Stage primaryStage;
    private AnchorPane gui;

    @FXML
    TextField pumpkinIP;
    @FXML
    TextField fichierLocal;
    @FXML
    TextField fichierDistant;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            this.primaryStage = primaryStage;
            this.primaryStage.setTitle("Client");

            // Permet l'arrêt du programme lorsque la fenêtre est quitée
            this.primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    Platform.exit();
                    System.exit(0);
                }
            });

            // Chargement du rootLayout
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("GUI.fxml"));
            gui = (AnchorPane) loader.load();

            // Affichage du rootLayout
            Scene scene = new Scene(gui);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleReceive() throws IOException {
        int retour = new Client().receiveFile(InetAddress.getByName(pumpkinIP.getText()), 69, fichierDistant.getText(), fichierLocal.getText());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reçu terminé");
        alert.setHeaderText(null);
        if (retour > 0) {
            alert.setContentText("Erreur de transfert intervenue sur le serveur");
        } else if (retour < 0) {
            alert.setContentText("Erreur locale (impossible d'accéder ou répertoire pour créer un fichier)");
        } else {
            alert.setContentText("Le transfert s'est bien déroulé");
        }
        alert.showAndWait();
    }

    @FXML
    public void handleSend() throws IOException {
        int retour = new Client().sendFile(InetAddress.getByName(pumpkinIP.getText()), 69, fichierLocal.getText());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Envoi terminé");
        alert.setHeaderText(null);
        if (retour > 0) {
            alert.setContentText("Erreur de transfert intervenue sur le serveur.");
        } else if (retour < 0) {
            alert.setContentText("Le choix du fichier local n'est pas valide.");
        } else {
            alert.setContentText("Le transfert s'est bien deroulé.");
        }
        alert.showAndWait();
    }

    @FXML
    public void handleOpen() throws IOException {
        Desktop.getDesktop().open(new File("data"));
    }

    public static void main(String[] args) {
        launch(args);
    }

}
