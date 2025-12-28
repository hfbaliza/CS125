package typeshi;

import javafx.application.Platform;
import java.util.concurrent.*;

public class GameController {

    private NetworkOpponent networkOpponent;
    private boolean multiplayer;
    private boolean isHost;

    private final UIComponents ui;
    private final WordGenerator wordGenerator;

    private ComputerOpponent computer;
    private ScheduledExecutorService backgroundPool;

    private int remainingSeconds;
    private boolean running = false;
    private int currentDifficulty = 5;

    // Difficulty mode: 1=Easy,2=Medium,3=Hard
    private int mode = 1;

    // Player & computer passages
    private String playerPassage;
    private String computerPassage;

    public GameController(UIComponents ui) {
        this.ui = ui;
        this.wordGenerator = new WordGenerator();
        setupUI();
    }

    private void setupUI() {
        ui.inputField.textProperty().addListener((obs, oldVal, newVal) -> onPlayerType());
        playerPassage = wordGenerator.getRandomPassage();
        computerPassage = wordGenerator.getRandomPassage();
    }

    public void setMode(int mode) { this.mode = mode; }

    public void setMultiplayer(boolean value) { multiplayer = value; }

    public void setHost(boolean value) { isHost = value; }

    public void onComputerFinished() {
        if (!running) return;
        endGame(); // or whatever logic you need when the AI finishes
    }

    // ================= MULTIPLAYER =================

    public void startHostMultiplayer() {
        multiplayer = true;
        isHost = true;

        new Thread(() -> {
            try {
                MultiplayerServer server = new MultiplayerServer(5000);

                // send initial passage to client
                server.send("TEXT:" + playerPassage);

                // start network opponent to handle communication
                networkOpponent = new NetworkOpponent(this, server, null, true);
                new Thread(networkOpponent).start();

                // tell client to start
                server.send("START");

                // start game UI
                Platform.runLater(() -> startGame(60, currentDifficulty));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void startJoinMultiplayer(String ip) {
        multiplayer = true;
        isHost = false;

        new Thread(() -> {
            try {
                MultiplayerClient client = new MultiplayerClient(ip, 5000);

                // listen for messages from host
                client.listenForMessages(msg -> {
                    if (msg.startsWith("TEXT:")) {
                        playerPassage = msg.substring(5);
                    } else if (msg.startsWith("PROGRESS:")) {
                        String[] parts = msg.split(":");
                        int pos = Integer.parseInt(parts[1]);
                        Platform.runLater(() -> ui.
                                opponentProgress.setProgress(
                                (double) pos / playerPassage.length()));
                    } else if (msg.equals("START")) {
                        Platform.runLater(() -> startGame(60, currentDifficulty));
                    }
                });

                // notify host that we are ready
                client.send("READY");

                // start network opponent to send progress
                networkOpponent = new NetworkOpponent(this, null, client, false);
                new Thread(networkOpponent).start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ================= GAME =================

    public void startGame(int durationSeconds, int difficulty) {
        if (running) return;
        running = true;
        remainingSeconds = durationSeconds;
        currentDifficulty = difficulty;

        backgroundPool = Executors.newScheduledThreadPool(2);

        if (!multiplayer) {
            computer = new ComputerOpponent(playerPassage, this, difficulty);
            backgroundPool.scheduleAtFixedRate(computer, 0, 100, TimeUnit.MILLISECONDS);
        }

        // Countdown timer
        backgroundPool.scheduleAtFixedRate(() -> {
            remainingSeconds--;
            Platform.runLater(() -> ui.timerLabel.setText(String.format("00:%02d", remainingSeconds)));
            if (remainingSeconds <= 0) endGame();
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void onPlayerType() {
        if (!running) return;

        String typed = ui.inputField.getText();
        int correctCount = 0;

        for (int i = 0; i < typed.length() && i < playerPassage.length(); i++) {
            if (typed.charAt(i) == playerPassage.charAt(i)) correctCount++;
        }

        double progress = (double) correctCount / playerPassage.length();
        ui.playerProgress.setProgress(progress);

        if (multiplayer && networkOpponent != null) {
            networkOpponent.sendProgress(correctCount, typed.length() - correctCount);
        }

        if (typed.equals(playerPassage)) {
            playerPassage = wordGenerator.getRandomPassage();
            Platform.runLater(() -> ui.inputField.clear());
        }
    }

    public void updateComputerProgress(int position, int errors) {
        Platform.runLater(() -> ui.computerProgress.setProgress((double) position / computerPassage.length()));
    }

    public void updateOpponentProgress(int position, int errors) {
        Platform.runLater(() -> ui.opponentProgress.setProgress((double) position / playerPassage.length()));
    }

    public void endGame() {
        running = false;
        if (backgroundPool != null) backgroundPool.shutdownNow();
    }
}
