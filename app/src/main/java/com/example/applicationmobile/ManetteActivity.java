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

import org.json.JSONObject;

import java.util.UUID;

public class ManetteActivity extends AppCompatActivity implements WebSocketManager.GameListener {

    // --- VARIABLES JOUEUR ET RÉSEAU ---
    private String pseudoJoueur, classeJoueur, roomCode, playerUuid;
    private WebSocketManager wsManager;

    // --- VARIABLES UI ET VIE ---
    private ProgressBar healthBar;
    private TextView textHealth, textNotification;
    private AlphaAnimation clignotementRouge;
    private int vieActuelle = 100;
    private androidx.cardview.widget.CardView cardNotification;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // --- VARIABLES PARAMÈTRES ---
    private boolean estEnModeGaucher = false, vibrationsActives = true;
    private int memoireTaille = 50, memoireOpacite = 100;

    // --- VARIABLES CAPTEURS ---
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float accActuelle, accPrecedente, secousse;
    private long dernierTempsSecousse = 0, dernierEnvoiJoystick = 0;

    // --- COOLDOWNS DYNAMIQUES (Gérés par le serveur) ---
    private float cdA = 0f, cdB = 0f, cdX = 0f, cdY = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manette);

        // 1. Récupération des données
        Intent intent = getIntent();
        pseudoJoueur = intent.getStringExtra("PSEUDO_JOUEUR");
        if (pseudoJoueur == null) pseudoJoueur = "Joueur";

        String rawRoom = intent.getStringExtra("ROOM_CODE");
        roomCode = (rawRoom != null) ? rawRoom.trim().toUpperCase() : "0000";

        String classeBrute = intent.getStringExtra("CLASSE_CHOISIE");
        classeJoueur = traduireClassePourUnity(classeBrute);

        // 2. Gestion UUID
        SharedPreferences prefs = getSharedPreferences("ColossusPrefs", MODE_PRIVATE);
        playerUuid = prefs.getString("UUID", null);
        if (playerUuid == null) {
            playerUuid = UUID.randomUUID().toString();
            prefs.edit().putString("UUID", playerUuid).apply();
        }

        // 3. Initialisation UI
        cardNotification = findViewById(R.id.card_notification);
        textNotification = findViewById(R.id.text_notification);
        healthBar = findViewById(R.id.health_bar);
        textHealth = findViewById(R.id.text_health);

        clignotementRouge = new AlphaAnimation(1.0f, 0.3f);
        clignotementRouge.setDuration(400);
        clignotementRouge.setRepeatMode(Animation.REVERSE);
        clignotementRouge.setRepeatCount(Animation.INFINITE);

        // 4. Configuration des contrôles fixes
        configurerBoutonsActions();
        configurerBoutonSOS();
        configurerDeconnexion();
        configurerJoystick();
        configurerMenuParametres();

        // 5. Initialisation Capteurs
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accActuelle = SensorManager.GRAVITY_EARTH;
        accPrecedente = SensorManager.GRAVITY_EARTH;

        // 6. Lancement Réseau
        wsManager = new WebSocketManager(this);
        wsManager.setUuid(playerUuid);
        wsManager.connect(roomCode, classeJoueur);
    }

    private String traduireClassePourUnity(String classeBrute) {
        if (classeBrute == null) return "Warrior";
        String input = classeBrute.toUpperCase();
        if (input.contains("GUERRIER") || input.contains("WARRIOR")) return "Warrior";
        if (input.contains("MAGE")) return "Mage";
        if (input.contains("SOIGNEUR") || input.contains("HEALER")) return "Healer";
        if (input.contains("TANK")) return "Tank";
        return "Warrior";
    }

    // ==========================================
    // RÉPONSES DU SERVEUR WEB ET UNITY
    // ==========================================

    @Override
    public void onConnectionChanged(boolean isConnected) {
        if (isConnected) {
            runOnUiThread(() -> Toast.makeText(this, "Connecté à l'arène !", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onSessionClosed() {
        runOnUiThread(() -> {
            Toast.makeText(this, "L'hôte a fermé la partie.", Toast.LENGTH_LONG).show();
            retourEcranAccueil();
        });
    }

    @Override
    public void onStateUpdated(double healthRatio, boolean vibrate) {
        runOnUiThread(() -> {
            int hp = (int) (healthRatio * 100);
            mettreAjourVie(hp);
            if (vibrate) faireVibrer(500);
        });
    }

    // ==========================================
    // MÉTHODES D'ENVOI AU SERVEUR
    // ==========================================

    private void envoyerActionServeur(String action) {
        if (wsManager != null) {
            wsManager.sendInput(action);
        }
    }

    public void envoyerMouvementServeur(float x, float y) {
        if (wsManager != null) {
            wsManager.sendMove(x, y);
        }
    }

    @Override
    public void onSkillsConfigReceived(JSONObject config) {
        runOnUiThread(() -> {
            try {
                // On récupère uniquement les temps de recharge (cd) envoyés par le serveur
                cdA = (float) config.getJSONObject("A").getDouble("cd");
                cdB = (float) config.getJSONObject("B").getDouble("cd");
                cdX = (float) config.getJSONObject("X").getDouble("cd");
                cdY = (float) config.getJSONObject("Y").getDouble("cd");

                // Maintenant que le téléphone connaît les chronos, on configure les boutons
                configurerBoutonsActions();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ==========================================
    // CONFIGURATION DES BOUTONS D'ACTION
    // ==========================================

    private void configurerBoutonsActions() {
        Button btnA = findViewById(R.id.btn_a);
        Button btnB = findViewById(R.id.btn_b);
        Button btnX = findViewById(R.id.btn_x);
        Button btnY = findViewById(R.id.btn_y);

        btnA.setText("A");
        btnB.setText("B");
        btnX.setText("X");
        btnY.setText("Y");

        btnA.setOnClickListener(v -> {
            faireVibrer(30);
            if (wsManager != null) wsManager.sendInput("A");
            if (cdA > 0) lancerCooldown(btnA, cdA);
        });
        btnB.setOnClickListener(v -> {
            faireVibrer(50);
            if (wsManager != null) wsManager.sendInput("B");
            if (cdB > 0) lancerCooldown(btnB, cdB);
        });

        btnX.setOnClickListener(v -> {
            faireVibrer(100);
            if (wsManager != null) wsManager.sendInput("X");
            if (cdX > 0) lancerCooldown(btnX, cdX);
        });

        btnY.setOnClickListener(v -> {
            faireVibrer(200);
            if (wsManager != null) wsManager.sendInput("Y");
            if (cdY > 0) lancerCooldown(btnY, cdY);
        });
    }

    // ==========================================
    // OUTILS UI ET NAVIGATION
    // ==========================================

    private void retourEcranAccueil() {
        Intent retourIntent = new Intent(ManetteActivity.this, ConnexionActivity.class);
        retourIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(retourIntent);
        finish();
    }

    private void configurerJoystick() {
        JoystickView joystickLeft = findViewById(R.id.joystick_left);
        if (joystickLeft != null) {
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
    }

    private void configurerMenuParametres() {
        ImageView btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> {
            ParametreFragment fragment = ParametreFragment.newInstance(estEnModeGaucher, vibrationsActives, memoireTaille, memoireOpacite);
            fragment.show(getSupportFragmentManager(), "Parametres");
        });
    }

    private void configurerBoutonSOS() {
        Button btnSos = findViewById(R.id.btn_sos);
        btnSos.setOnClickListener(v -> {
            faireVibrer(150);
            envoyerActionServeur("SOS_TEAM");
        });
    }

    private void configurerDeconnexion() {
        ImageView btnDisconnect = findViewById(R.id.btn_disconnect);
        btnDisconnect.setOnClickListener(v -> {
            if (wsManager != null) wsManager.disconnect();
            retourEcranAccueil();
        });
    }

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

    private void lancerCooldown(final Button bouton, float tempsEnSecondes) {
        final String texteOriginal = bouton.getText().toString(); // "A", "B", etc.
        bouton.setEnabled(false);
        bouton.setAlpha(0.5f);

        long durationMs = (long) (tempsEnSecondes * 1000L);

        new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Affiche le temps restant en secondes sur le bouton
                int sec = (int) (millisUntilFinished / 1000) + 1;
                bouton.setText(String.valueOf(sec));
            }

            @Override
            public void onFinish() {
                // Remet le bouton dans son état normal
                bouton.setEnabled(true);
                bouton.setAlpha(1.0f);
                bouton.setText(texteOriginal);
            }
        }.start();
    }

    private void mettreAjourVie(int nouvelleVie) {
        vieActuelle = Math.max(0, Math.min(100, nouvelleVie));
        healthBar.setProgress(vieActuelle);
        textHealth.setText(vieActuelle + " / 100 PV");

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

    // ==========================================
    // CYCLE DE VIE ET CAPTEURS
    // ==========================================

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
                        String actionSecousse = classeJoueur.equals("Healer") ? "Secouer la bulle" : "ALLER_AU_HEALER";
                        envoyerActionServeur(actionSecousse);
                    }
                }
            }, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(new SensorEventListener() {
                @Override public void onSensorChanged(SensorEvent event) {}
                @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wsManager != null) {
            wsManager.disconnect();
        }
    }
}