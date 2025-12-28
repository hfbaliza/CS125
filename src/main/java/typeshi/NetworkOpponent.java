package typeshi;

import javafx.application.Platform;

public class NetworkOpponent implements Runnable {

    private final GameController controller;
    private final MultiplayerServer server;
    private final MultiplayerClient client;
    private final boolean isHost;
    private volatile boolean running = true;

    public NetworkOpponent(GameController controller,
                           MultiplayerServer server,
                           MultiplayerClient client,
                           boolean isHost) {
        this.controller = controller;
        this.server = server;
        this.client = client;
        this.isHost = isHost;
    }

    /* ---------- SEND ---------- */

    public void sendProgress(int position, int errors) {
        send("PROGRESS:" + position + ":" + errors);
    }

    public void sendFinished() {
        send("FINISHED");
    }

    private void send(String msg) {
        if (isHost) server.send(msg);
        else client.send(msg);
    }

    private String receive() throws Exception {
        return isHost ? server.receive() : client.receive();
    }

    /* ---------- RECEIVE LOOP ---------- */

    // In NetworkOpponent.java
    @Override
    public void run() {
        try {
            String msg;
            // Added check for 'running' inside the loop condition
            while (running && (msg = receive()) != null) {
                if (msg.startsWith("PROGRESS")) {
                    String[] p = msg.split(":");
                    int position = Integer.parseInt(p[1]);
                    int errors = Integer.parseInt(p[2]);

                    Platform.runLater(() ->
                            controller.updateComputerProgress(position, errors)
                    );
                }

                if (msg.equals("FINISHED")) {
                    Platform.runLater(controller::onComputerFinished);
                }
            }
        } catch (Exception e) {
            if (running) { // Only print error if we didn't stop intentionally
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
    }
}
