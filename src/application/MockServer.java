package application;

import static javafx.beans.binding.Bindings.when;

import java.util.Random;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MockServer extends Application {

    @Override
    public void start(Stage primaryStage) {
        ListView<String> connectedClients = new ListView<>();

        Server server = new Server(clientName -> {
            connectedClients.getItems().add(clientName);
        } , clientName -> {
            connectedClients.getItems().remove(clientName);
        });

        Button startStopButton = new Button();
        startStopButton.setOnAction(e -> {
            if (server.isRunning()) {
                server.cancel();
            } else {
                server.restart();
            }
        });

        startStopButton.textProperty().bind(
                when(server.runningProperty()).
                then("Stop Server").
                otherwise("Start Server"));

        BorderPane.setAlignment(startStopButton, Pos.CENTER);
        BorderPane.setMargin(startStopButton, new Insets(10));

        BorderPane root = new BorderPane(connectedClients);
        root.setTop(startStopButton);
        Scene scene = new Scene(root, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static class Server extends Service<Void> {

        private final Consumer<String> connectedNotification;
        private final Consumer<String> disconnectedNotification;

        public Server(Consumer<String> connectedNotification, Consumer<String> disconnectedNotification) {
            this.connectedNotification = connectedNotification;
            this.disconnectedNotification = disconnectedNotification;
        }

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {

                private int clientCount = 0;

                @Override
                protected Void call() throws Exception {

                    Random rng = new Random();

                    while (!isCancelled()) {
                        try {
                            Thread.sleep(rng.nextInt(3000) + 1000);
                        } catch (InterruptedException exc) {
                            if (isCancelled()) {
                                break;
                            } else {
                                Thread.currentThread().interrupt();
                            }
                        }
                        final String clientName = "Client " + (++clientCount);
                        final int connectTime = rng.nextInt(3000) + 4000;
                        Thread clientThread = new Thread(() -> {
                            try {
                                Thread.sleep(connectTime);
                            } catch (InterruptedException exc) {
                                exc.printStackTrace();
                            }
                            Platform.runLater(() -> {
                                disconnectedNotification.accept(clientName);
                            });
                        });
                        clientThread.setDaemon(true);
                        Platform.runLater(() -> {
                            connectedNotification.accept(clientName);
                            Alert alert = new Alert(AlertType.INFORMATION);
                            alert.setContentText(clientName + " connected");
                            alert.show();
                        });
                        clientThread.start();
                    }

                    return null;
                }

            };
        }

    }

    public static void main(String[] args) {
        launch(args);
    }
}