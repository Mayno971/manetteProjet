package com.example.applicationmobile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.IO;
import io.socket.client.Socket;

public class ManetteActivity extends AppCompatActivity {

    // --- VARIABLES JOUEUR ---
    private String pseudoJoueur, classeJoueur, couleurJoueur, roomCode, sessionToken;

    // --- VARIABLES UI & VIE ---
    private ProgressBar healthBar;
    private AlphaAnimation clignotementRouge;
    private int vieActuelle = 100;
    private androidx.cardview.widget.CardView cardNotification;
    private TextView textNotification;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // --- VARIABLES PARAMÈTRES ---
    private boolean estEnModeGaucher = false;
    private boolean vibrationsActives = true;
    private int memoireTaille = 50, memoireOpacite = 100;

    // --- VARIABLES SECOUSSE ---
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float accActuelle, accPrecedente, secousse;
    private long dernierTempsSecousse = 0, dernierEnvoiJoystick = 0;

    // --- RÉSEAU ---
    private Socket mSocket;
    private Timer heartbeatTimer; // NOUVEAU : Timer pour le maintien de connexion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manette);

        // 1. Récupération des données du Joueur
        SharedPreferences prefs = getSharedPreferences("ColossusPrefs", MODE_PRIVATE);
        sessionToken = prefs.getString("SESSION_TOKEN", java.util.UUID.randomUUID().toString());
        prefs.edit().putString("SESSION_TOKEN", sessionToken).apply();

        Intent intent = getIntent();
        pseudoJoueur = intent.getStringExtra("PSEUDO_JOUEUR");
        classeJoueur = intent.getStringExtra("CLASSE_CHOISIE");
        couleurJoueur = intent.getStringExtra("COULEUR_JOUEUR");
        roomCode = intent.getStringExtra("ROOM_CODE");

        if (pseudoJoueur == null) pseudoJoueur = "Joueur Inconnu";
        if (classeJoueur == null) classeJoueur = "GUERRIER";
        if (roomCode == null) roomCode = "0000";

        // 2. Initialisation UI
        cardNotification = findViewById(R.id.card_notification);
        textNotification = findViewById(R.id.text_notification);
        healthBar = findViewById(R.id.health_bar);

        clignotementRouge = new AlphaAnimation(1.0f, 0.3f);
        clignotementRouge.setDuration(400);
        clignotementRouge.setRepeatMode(Animation.REVERSE);
        clignotementRouge.setRepeatCount(Animation.INFINITE);

        initialiserReseau();
        configurerBoutonsActions();
        configurerBoutonSOS();
        configurerDeconnexion();
        configurerJoystick();
        configurerMenuParametres();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accActuelle = SensorManager.GRAVITY_EARTH;
        accPrecedente = SensorManager.GRAVITY_EARTH;
    }

    private void initialiserReseau() {
        try {
            mSocket = IO.socket("http://192.168.1.XX:3000"); // Ton IP locale
        } catch (URISyntaxException e) { return; }

        mSocket.on(Socket.EVENT_CONNECT, args -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Connecté à l'arène !", Toast.LENGTH_SHORT).show();
                envoyerInfosJoueur();
                demarrerHeartbeat(); // NOUVEAU : On lance le battement de cœur
            });
        });

        // NOUVEAU : Reconnexion automatique et statut
        mSocket.on(Socket.EVENT_DISCONNECT, args -> {
            runOnUiThread(() -> {
                stopperHeartbeat();
                Toast.makeText(this, "Connexion perdue. Tentative de reconnexion...", Toast.LENGTH_LONG).show();
            });
        });

        // NOUVEAU : L'hôte ferme la session (Retour Hub)
        mSocket.on("session_closed", args -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "L'hôte a fermé la partie.", Toast.LENGTH_LONG).show();
                retourEcranAccueil();
            });
        });

        // Mise à jour de la vie et de l'état (Comme la version Web)
        mSocket.on("update_state", args -> {
            if (args.length > 0) {
                try {
                    JSONObject state = (JSONObject) args[0];

                    // Ratio de vie (0.0 à 1.0)
                    if (state.has("healthRatio")) {
                        int hp = (int) (state.getDouble("healthRatio") * 100);
                        runOnUiThread(() -> mettreAjourVie(hp));
                    }

                    // NOUVEAU : Le serveur ordonne une grosse vibration (Ex: Dégâts du boss)
                    if (state.has("vibrate") && state.getBoolean("vibrate")) {
                        runOnUiThread(() -> faireVibrer(500));
                    }

                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        mSocket.on("notification", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String msg = data.getString("message");
                    String type = data.getString("type");
                    runOnUiThread(() -> afficherNotification(msg, type));
                } catch (JSONException e) { e.printStackTrace(); }
            }
        });

        mSocket.connect();
    }

    // ==========================================
    // NOUVELLES FONCTIONS RÉSEAU AVANCÉES
    // ==========================================

    private void demarrerHeartbeat() {
        stopperHeartbeat(); // Sécurité
        heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mSocket != null && mSocket.connected()) {
                    try {
                        JSONObject data = new JSONObject();
                        data.put("room", roomCode);
                        data.put("token", sessionToken);
                        mSocket.emit("heartbeat", data);
                    } catch (JSONException e) { e.printStackTrace(); }
                }
            }
        }, 1000, 1000); // Envoi toutes les secondes (1000ms)
    }

    private void stopperHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    private void retourEcranAccueil() {
        Intent retourIntent = new Intent(ManetteActivity.this, ConnexionActivity.class);
        retourIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(retourIntent);
        finish();
    }

    // ==========================================
    // MÉTHODES D'ENVOI EXISTANTES
    // ==========================================

    private void envoyerInfosJoueur() {
        try {
            JSONObject data = new JSONObject();
            data.put("pseudo", pseudoJoueur);
            data.put("classe", classeJoueur);
            data.put("couleur", couleurJoueur);
            data.put("token", sessionToken);
            data.put("room", roomCode);
            mSocket.emit("nouveau_joueur", data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void envoyerActionServeur(String typeAction) {
        if (mSocket == null || !mSocket.connected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("pseudo", pseudoJoueur);
            data.put("action", typeAction);
            data.put("token", sessionToken);
            mSocket.emit("action_joueur", data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    public void envoyerMouvementServeur(float x, float y) {
        if (mSocket == null || !mSocket.connected()) return;
        try {
            JSONObject data = new JSONObject();
            data.put("x", x);
            data.put("y", y);
            data.put("token", sessionToken);
            mSocket.emit("mouvement_joueur", data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    // ==========================================
    // CONFIGURATION DES BOUTONS
    // ==========================================

    private void configurerBoutonsActions() {
        Button btnA = findViewById(R.id.btn_a);
        Button btnB = findViewById(R.id.btn_b);
        Button btnX = findViewById(R.id.btn_x);
        Button btnY = findViewById(R.id.btn_y);

        String nomA = "Attaque de base", nomB = "Esquive", nomX = "Compétence", nomY = "Ultime";
        int cdB = 2, cdX = 5, cdY = 15;

        switch (classeJoueur.replace("⚔️ ", "")) {
            case "GUERRIER":
                nomA = "Coup d'épée"; nomB = "Roulade"; nomX = "Charge"; nomY = "Tourbillon";
                cdX = 6; cdY = 12; break;
            case "MAGE":
                nomA = "Boule d'énergie"; nomB = "Blink"; nomX = "Mur de glace"; nomY = "Météores";
                cdX = 8; cdY = 20; break;
            case "SOIGNEUR":
                nomA = "Lumière"; nomB = "Soin rapide"; nomX = "Aura"; nomY = "Résurrection";
                cdX = 10; cdY = 30; break;
            case "TANK":
                nomA = "Marteau"; nomB = "Taunt"; nomX = "Rocher"; nomY = "Dôme";
                cdX = 5; cdY = 18; break;
        }

        final String fA = nomA, fB = nomB, fX = nomX, fY = nomY;
        final int finalCdB = cdB, finalCdX = cdX, finalCdY = cdY;

        btnA.setOnClickListener(v -> { faireVibrer(30); envoyerActionServeur(fA); });
        btnB.setOnClickListener(v -> { faireVibrer(50); envoyerActionServeur(fB); lancerCooldown(btnB, finalCdB); });
        btnX.setOnClickListener(v -> { faireVibrer(100); envoyerActionServeur(fX); lancerCooldown(btnX, finalCdX); });
        btnY.setOnClickListener(v -> { faireVibrer(200); envoyerActionServeur(fY); lancerCooldown(btnY, finalCdY); });
    }

    private void configurerJoystick() {
        JoystickView joystickLeft = findViewById(R.id.joystick_left);
        joystickLeft.setJoystickListener((xPercent, yPercent) -> {
            if (Math.abs(xPercent) < 0.05f && Math.abs(yPercent) < 0.05f) {
                xPercent = 0f; yPercent = 0f;
            }
            long tempsActuel = System.currentTimeMillis();
            if ((xPercent == 0f && yPercent == 0f) || (tempsActuel - dernierEnvoiJoystick > 50)) {
                envoyerMouvementServeur(xPercent, yPercent);
                dernierEnvoiJoystick = tempsActuel;
            }
        });
    }

    private void configurerMenuParametres() {
        ImageView btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> {
            ParametreFragment fragment = ParametreFragment.newInstance(
                    estEnModeGaucher, vibrationsActives, memoireTaille, memoireOpacite
            );
            // Ton code existant pour le ParametreFragment reste ici (identique à avant)
            fragment.show(getSupportFragmentManager(), "Parametres");
        });
    }

    private void configurerBoutonSOS() {
        Button btnSos = findViewById(R.id.btn_sos);
        btnSos.setOnClickListener(v -> {
            faireVibrer(150);
            envoyerActionServeur("SOS_TEAM");
            lancerCooldown(btnSos, 10);
        });
    }

    private void configurerDeconnexion() {
        ImageView btnDisconnect = findViewById(R.id.btn_disconnect);
        btnDisconnect.setOnClickListener(v -> retourEcranAccueil());
    }

    // ==========================================
    // OUTILS UI ET PHYSIQUE
    // ==========================================

    private void faireVibrer(int dureeMilliSecondes) {
        if (!vibrationsActives) return;
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(dureeMilliSecondes, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(dureeMilliSecondes);
            }
        }
    }

    private void lancerCooldown(final Button bouton, int tempsEnSecondes) {
        final String texteOriginal = bouton.getText().toString();
        bouton.setEnabled(false);
        bouton.setAlpha(0.5f);
        new CountDownTimer((tempsEnSecondes * 1000L) + 500, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int sec = (int) (millisUntilFinished / 1000);
                if (sec > 0) bouton.setText(String.valueOf(sec));
            }
            @Override
            public void onFinish() {
                bouton.setEnabled(true);
                bouton.setAlpha(1.0f);
                bouton.setText(texteOriginal);
            }
        }.start();
    }

    private void mettreAjourVie(int nouvelleVie) {
        vieActuelle = Math.max(0, Math.min(100, nouvelleVie));
        healthBar.setProgress(vieActuelle);

        if (vieActuelle > 50) {
            healthBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#52B766")));
            healthBar.clearAnimation();
        } else if (vieActuelle > 20) {
            healthBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F39C12")));
            healthBar.clearAnimation();
        } else if (vieActuelle > 0) {
            healthBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E74C3C")));
            if (healthBar.getAnimation() == null) {
                healthBar.startAnimation(clignotementRouge);
                faireVibrer(500);
            }
        } else {
            healthBar.clearAnimation();
            healthBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#000000")));
        }
    }

    public void afficherNotification(String message, String typeAlerte) {
        textNotification.setText(message);
        textNotification.setTextColor(typeAlerte.equalsIgnoreCase("MORT") ? Color.parseColor("#E74C3C") :
                typeAlerte.equalsIgnoreCase("SOIN") ? Color.parseColor("#52B766") : Color.parseColor("#F1C40F"));

        if (typeAlerte.equalsIgnoreCase("MORT")) faireVibrer(300);

        cardNotification.setVisibility(View.VISIBLE);
        cardNotification.setAlpha(0f);
        cardNotification.animate().alpha(1f).setDuration(300).start();

        mainHandler.removeCallbacksAndMessages(null);
        mainHandler.postDelayed(() -> cardNotification.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> cardNotification.setVisibility(View.GONE)).start(), 3000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(new SensorEventListener() {
                @Override public void onAccuracyChanged(Sensor s, int a) {}
                @Override public void onSensorChanged(SensorEvent e) {
                    float x = e.values[0], y = e.values[1], z = e.values[2];
                    accPrecedente = accActuelle;
                    accActuelle = (float) Math.sqrt(x*x + y*y + z*z);
                    secousse = secousse * 0.9f + (accActuelle - accPrecedente);
                    if (secousse > 12 && System.currentTimeMillis() - dernierTempsSecousse > 3000) {
                        dernierTempsSecousse = System.currentTimeMillis();
                        faireVibrer(300);
                        envoyerActionServeur(classeJoueur.contains("SOIGNEUR") ? "Secouer la bulle" : "SECOUSSE_PHYSIQUE");
                    }
                }
            }, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopperHeartbeat();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
    }
}