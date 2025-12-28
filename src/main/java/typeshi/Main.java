package typeshi;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.io.IOException;


public class Main extends Application {

    private Stage primaryStage;
    private Scene scene;  // One shared scene

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showLoadingScreen();
    }

    private void showLoadingScreen() {
        LoadingScreen loading = new LoadingScreen();

        // Create the scene once here
        scene = new Scene(loading.getRoot(), 800, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("TypeShi");
        primaryStage.show();

        // Auto-load 3 seconds then show home
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> showHomeScreen());
        pause.play();
    }

    // Home screen uses same scene, just swaps root
    private void showHomeScreen() {
        HomeScreen home = new HomeScreen();

        home.getPlayButton().setOnAction(e -> showDifficultyScreen());

        // Handle Multiplayer Click
        home.getMultiplayerButton().setOnAction(e -> handleMultiplayerSelection());

        scene.setRoot(home.getRoot());
    }

    private void handleMultiplayerSelection() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Host", "Host", "Join");
        dialog.setTitle("Multiplayer");
        dialog.setHeaderText("Choose how you want to play:");
        dialog.setContentText("Select mode:");
        dialog.initOwner(primaryStage);

        dialog.showAndWait().ifPresent(choice -> {
            if (choice.equals("Host")) {
                new Thread(() -> {
                    try {
                        MultiplayerServer server = new MultiplayerServer(5000);
                        server.listenForMessages();
                        server.send("START"); // Tell client round is starting
                        // Here you can send actual game data like words to type
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                TextInputDialog ipInput = new TextInputDialog("127.0.0.1");
                ipInput.setTitle("Join Game");
                ipInput.setHeaderText("Enter Host IP");
                ipInput.setContentText("IP Address:");
                ipInput.initOwner(primaryStage);

                ipInput.showAndWait().ifPresent(ip -> {
                    new Thread(() -> {
                        try {
                            MultiplayerClient client = new MultiplayerClient(ip, 5000);
                            client.send("READY"); // Let host know client is ready
                            // You can send typing progress using client.send("PROGRESS:10")
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
            }
        });
    }


    private void handleMultiplayerChoice() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Host", "Host", "Join");
        dialog.setTitle("Multiplayer");
        dialog.setHeaderText("Connection Mode");
        dialog.setContentText("Select how you want to connect:");

        dialog.showAndWait().ifPresent(choice -> {
            if (choice.equals("Host")) {
                // Logic for hosting
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Host Game");
                alert.setHeaderText(null);
                alert.setContentText("Hosting game...");
                alert.showAndWait();
            } else {
                TextInputDialog ipInput = new TextInputDialog("127.0.0.1");
                ipInput.setTitle("Join Game");
                ipInput.setHeaderText("Enter Host IP");
                ipInput.setContentText("IP Address:");

                ipInput.showAndWait().ifPresent(ip -> {
                    // Logic for joining
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Join Game");
                    alert.setHeaderText(null);
                    alert.setContentText("Joining game at " + ip + "...");
                    alert.showAndWait();
                });
            }
        });
    }

    // Difficulty screen with same scene
    private void showDifficultyScreen() {
        DifficultyScreen diff = new DifficultyScreen();

        diff.getBackButton().setOnAction(e -> showHomeScreen());

        diff.getEasyButton().setOnAction(e -> showRound(1, 1, 1));   // mode 1, AI difficulty 1
        diff.getMediumButton().setOnAction(e -> showRound(1, 2, 6)); // mode 2, AI difficulty 6
        diff.getHardButton().setOnAction(e -> showRound(1, 3, 9));   // mode 3, AI difficulty 9

        scene.setRoot(diff.getRoot());   // no new Scene
    }

    // Start round with selected mode + AI difficulty
    private void showRound(int round, int mode, int aiDifficulty) {
        Alert roundPopup = new Alert(Alert.AlertType.INFORMATION);
        roundPopup.setTitle("Round " + round);
        roundPopup.setHeaderText("Get Ready for Round " + round + "!");
        roundPopup.setContentText("The typing challenge will begin shortly.");
        roundPopup.showAndWait();

        UIComponents ui = new UIComponents();
        GameController controller = new GameController(ui);
        controller.setMode(mode);           // 1=Easy, 2=Medium, 3=Hard
        ui.setController(controller);

        scene.setRoot(ui.rootPane);        // keep same Scene, size, fullscreen

        controller.startGame(20, aiDifficulty); // 20 seconds, adjust as you like
    }

    public static void main(String[] args) {
        launch();
    }
}
