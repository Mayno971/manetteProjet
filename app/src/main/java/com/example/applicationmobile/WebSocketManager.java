package com.example.applicationmobile;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketManager {

    private WebSocket webSocketTask;
    private final OkHttpClient client;

    public boolean isConnected = false;
    public double healthRatio = 1.0;
    public boolean sessionFermee = false;

    private final String SERVER_URL = "wss://colossus-arena.alwaysdata.net";
    private String uuid;
    private String classeChoisie;

    private final Handler mainHandler;
    private final GameListener listener;

    // Interface mise à jour avec la réception de la configuration
    public interface GameListener {
        void onConnectionChanged(boolean isConnected);
        void onSessionClosed();
        void onStateUpdated(double healthRatio, boolean vibrate);
        void onSkillsConfigReceived(JSONObject config);
    }

    public WebSocketManager(GameListener listener) {
        this.listener = listener;
        this.uuid = UUID.randomUUID().toString();
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Ping automatique toutes les 15 secondes
        this.client = new OkHttpClient.Builder()
                .pingInterval(15, TimeUnit.SECONDS)
                .build();
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void connect(String room, String chosenClass) {
        this.classeChoisie = chosenClass;
        Request request = new Request.Builder().url(SERVER_URL).build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d("COLOSSUS", "Réseau ouvert ! Envoi de JOIN_ROOM...");
                webSocketTask = webSocket;
                webSocket.send("JOIN_ROOM|" + room + "|" + uuid);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                mainHandler.post(() -> handleMessage(text));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                mainHandler.post(() -> disconnect());
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("WebSocketManager", "Erreur réseau : " + t.getMessage());
                mainHandler.post(() -> disconnect());
            }
        });
    }

    public void disconnect() {
        if (webSocketTask != null) {
            webSocketTask.close(1000, "Déconnexion normale");
            webSocketTask = null;
        }
        if (isConnected) {
            isConnected = false;
            if (listener != null) listener.onConnectionChanged(false);
        }
    }

    public void sendMessage(String string) {
        if (webSocketTask != null) webSocketTask.send(string);
    }

    public void sendInput(String key) {
        sendMessage("INPUT|" + key);
    }

    public void sendMove(float x, float y) {
        String xStr = String.format(Locale.US, "%.2f", x);
        String yStr = String.format(Locale.US, "%.2f", y);
        sendMessage("MOVE|" + xStr + "|" + yStr);
    }

    private void handleMessage(String text) {
        String[] parts = text.split("\\|");
        if (parts.length == 0) return;

        String command = parts[0];

        if (command.equals("JOIN_OK")) {
            this.isConnected = true;
            if (listener != null) listener.onConnectionChanged(true);

            sendMessage("INPUT|SET_CLASS|" + this.classeChoisie);
            sendMessage("GET_SKILLS|" + this.classeChoisie);
        }
        else if (command.equals("SKILLS_CONFIG") && parts.length > 1) {
            try {
                JSONObject config = new JSONObject(parts[1]);
                mainHandler.post(() -> {
                    if (listener != null) listener.onSkillsConfigReceived(config);
                });

            } catch (Exception e) {
                Log.e("WebSocketManager", "Erreur JSON Skills: " + e.getMessage());
            }
        }
        else if (command.equals("SESSION_CLOSED")) {
            this.sessionFermee = true;
            if (listener != null) listener.onSessionClosed();
        }
        else if (command.equals("STATE") && parts.length > 1) {
            try {
                JSONObject state = new JSONObject(parts[1]);
                if (state.has("healthRatio")) this.healthRatio = state.getDouble("healthRatio");
                boolean vibrate = state.optBoolean("vibrate", false);
                if (listener != null) listener.onStateUpdated(this.healthRatio, vibrate);
            } catch (Exception e) {
                Log.e("WebSocketManager", "Erreur JSON State: " + e.getMessage());
            }
        }
    }
}