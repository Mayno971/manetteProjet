package com.example.applicationmobile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;



public class ManetteActivity extends AppCompatActivity {
    private Socket mSocket;
    private String pseudoJoueur;
    private String classeJoueur;
    private String couleurJoueur;

    private ProgressBar healthBar; // barre de vie
    private AlphaAnimation clignotementRouge;
    private int vieActuelle = 100;

    // --- VARIABLES POUR LA DÉTECTION DE SECOUSSE (SHAKE) ---
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float accActuelle;
    private float accPrecedente;
    private float secousse;
    private long dernierTempsSecousse = 0; // Pour le Cooldown physique
    private long dernierEnvoiJoystick = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.content.SharedPreferences prefs = getSharedPreferences("ColossusPrefs", MODE_PRIVATE);
        String sessionToken = prefs.getString("SESSION_TOKEN", null);

        if (sessionToken == null) {
            sessionToken = java.util.UUID.randomUUID().toString();
            prefs.edit().putString("SESSION_TOKEN", sessionToken).apply();
        }
        final String finalToken = sessionToken;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manette);

        ImageView btnDisconnect = findViewById(R.id.btn_disconnect);

        Intent intent = getIntent();
        pseudoJoueur = intent.getStringExtra("PSEUDO_JOUEUR");
        classeJoueur = intent.getStringExtra("CLASSE_CHOISIE");
        couleurJoueur = intent.getStringExtra("COULEUR_JOUEUR");

        if (pseudoJoueur == null || pseudoJoueur.isEmpty()) {
            pseudoJoueur = "Joueur Inconnu";
        }
        if (classeJoueur == null) classeJoueur = "⚔️ GUERRIER";
        if (couleurJoueur == null) couleurJoueur = "#FFFFFF";

        TextView textPlayerInfo = findViewById(R.id.text_player_info);
        com.google.android.material.card.MaterialCardView indicatorColor = findViewById(R.id.indicator_color);

        textPlayerInfo.setText(pseudoJoueur.toUpperCase() + " - " + classeJoueur);
        try {
            if (couleurJoueur != null && couleurJoueur.startsWith("#")) {
                indicatorColor.setCardBackgroundColor(android.graphics.Color.parseColor(couleurJoueur));
            } else {
                indicatorColor.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));
            }
        } catch (IllegalArgumentException e) {
            indicatorColor.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));
        }

        healthBar = findViewById(R.id.health_bar);

        clignotementRouge = new AlphaAnimation(1.0f, 0.3f);
        clignotementRouge.setDuration(400);
        clignotementRouge.setRepeatMode(Animation.REVERSE);
        clignotementRouge.setRepeatCount(Animation.INFINITE);

        android.widget.Button btnSos = findViewById(R.id.btn_sos);

        initialiserReseau();
        org.json.JSONObject data = new org.json.JSONObject();
        try {
            data.put("pseudo", pseudoJoueur);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        try {
            data.put("classe", classeJoueur);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        try {
            data.put("token", finalToken); // LA CLÉ MAGIQUE !
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        mSocket.emit("nouveau_joueur", data);

        btnDisconnect.setOnClickListener(v -> {
            Intent retourIntent = new Intent(ManetteActivity.this, ConnexionActivity.class);
            retourIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(retourIntent);
            finish();
        });

        btnSos.setOnClickListener(v -> {
            faireVibrer(150);
            android.widget.Toast.makeText(this, "Signal envoyé", Toast.LENGTH_SHORT).show();
            lancerCooldown(btnSos, 10);
        });

        String nomAttaqueA = "Attaque de base";
        String nomAttaqueB = "Esquive";
        String nomAttaqueX = "Compétence Spéciale";
        String nomAttaqueY = "Attaque Ultime";

        int cooldownB = 2;
        int cooldownX = 5;
        int cooldownY = 15;

        // On écrase les variables par défaut avec les sorts du rôle sélectionné
        switch (classeJoueur) {
            case "GUERRIER":
                nomAttaqueA = "Coup d'épée";
                nomAttaqueB = "Roulade d'esquive";
                nomAttaqueX = "Charge furieuse";
                nomAttaqueY = "Tourbillon de lames";
                cooldownX = 6;
                cooldownY = 12;
                break;
            case "MAGE":
                nomAttaqueA = "Boule d'énergie";
                nomAttaqueB = "Téléportation (Blink)";
                nomAttaqueX = "Mur de glace";
                nomAttaqueY = "Pluie de Météores";
                cooldownX = 8;
                cooldownY = 20;
                break;
            case "SOIGNEUR":
                nomAttaqueA = "Orbe de lumière";
                nomAttaqueB = "Soin rapide";
                nomAttaqueX = "Aura de guérison";
                nomAttaqueY = "Secouer la Bulle (Résurrection)";
                cooldownX = 10;
                cooldownY = 30;
                break;
            case "TANK":
                nomAttaqueA = "Coup de marteau";
                nomAttaqueB = "Provocation du Boss";
                nomAttaqueX = "Lancer de rocher";
                nomAttaqueY = "Dôme protecteur géant";
                cooldownX = 5;
                cooldownY = 18;
                break;
        }

        Button btnA = findViewById(R.id.btn_a);
        Button btnB = findViewById(R.id.btn_b);
        Button btnX = findViewById(R.id.btn_x);
        Button btnY = findViewById(R.id.btn_y);

        final String finalNomA = nomAttaqueA;
        final String finalNomB = nomAttaqueB;
        final String finalNomX = nomAttaqueX;
        final String finalNomY = nomAttaqueY;
        final int finalCdB = cooldownB;
        final int finalCdX = cooldownX;
        final int finalCdY = cooldownY;

        // Bouton A (Attaque de base - Rapide, petit retour haptique)
        btnA.setOnClickListener(v -> {
            faireVibrer(30);
            android.widget.Toast.makeText(this, finalNomA, Toast.LENGTH_SHORT);
            envoyerActionServeur(finalNomA);
        });

        // Bouton B (Esquive ou Action de classe)
        btnB.setOnClickListener(v -> {
            faireVibrer(50);
            mettreAjourVie(vieActuelle - 15);
            android.widget.Toast.makeText(this, finalNomB, android.widget.Toast.LENGTH_SHORT).show();
            envoyerActionServeur(finalNomB);
            lancerCooldown(btnB, finalCdB);
        });

        // Bouton X (Sortilège Spécial)
        btnX.setOnClickListener(v -> {
            faireVibrer(100);
            android.widget.Toast.makeText(this, finalNomX, android.widget.Toast.LENGTH_SHORT).show();
            envoyerActionServeur(finalNomX);
            lancerCooldown(btnX, finalCdX);
        });

        // Bouton Y (L'Ultime - Grosse vibration)
        btnY.setOnClickListener(v -> {
            faireVibrer(200);
            android.widget.Toast.makeText(this, "ULTIME : " + finalNomY.toUpperCase(), android.widget.Toast.LENGTH_SHORT).show();
            envoyerActionServeur(finalNomY);
            lancerCooldown(btnY, finalCdY);
        });

        JoystickView joystickLeft = findViewById(R.id.joystick_left);

        joystickLeft.setJoystickListener(new JoystickView.JoystickListener() {
            @Override
            public void onJoystickMoved(float xPercent, float yPercent) {
                // 1. LA DEADZONE (Zone morte de 5%)
                // Si le pouce bouge de moins de 5%, on considère que le joueur est à l'arrêt.
                if (Math.abs(xPercent) < 0.05f && Math.abs(yPercent) < 0.05f) {
                    xPercent = 0f;
                    yPercent = 0f;
                }

                // 2. LIMITATION D'ENVOI (Rate Limiting)
                long tempsActuel = System.currentTimeMillis();

                // On autorise l'envoi SEULEMENT si :
                // - Le joueur a lâché le joystick (x=0 et y=0, pour que le perso s'arrête instantanément)
                // - OU s'il s'est écoulé plus de 50 millisecondes (soit max 20 requêtes par seconde)
                if ((xPercent == 0f && yPercent == 0f) || (tempsActuel - dernierEnvoiJoystick > 50)) {

                    envoyerMouvementServeur(xPercent, yPercent);
                    dernierEnvoiJoystick = tempsActuel;
                }
            }
        });

        // MENU DES OPTIONS
        android.widget.ImageView btnSettings = findViewById(R.id.btn_settings);
        android.view.View panelSettings = findViewById(R.id.panel_settings);
        android.widget.SeekBar seekSize = findViewById(R.id.seek_size);

        btnSettings.setOnClickListener(v -> {
            if (panelSettings.getVisibility() == android.view.View.VISIBLE) {
                panelSettings.setVisibility(android.view.View.GONE);
            } else {
                panelSettings.setVisibility(android.view.View.VISIBLE);
            }
        });

        seekSize.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                float echelle = 0.6f + (progress / 100f) * 0.8f;

                // On applique cette taille à tous les boutons !
                btnA.setScaleX(echelle); btnA.setScaleY(echelle);
                btnB.setScaleX(echelle); btnB.setScaleY(echelle);
                btnX.setScaleX(echelle); btnX.setScaleY(echelle);
                btnY.setScaleX(echelle); btnY.setScaleY(echelle);

                // Et même au Joystick pour les gros pouces !
                joystickLeft.setScaleX(echelle);
                joystickLeft.setScaleY(echelle);
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {} // Inutile ici
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {} // Inutile ici
        });

        // ========================
        // DETECTION DE SECOUSSE
        // ========================
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        accActuelle = SensorManager.GRAVITY_EARTH;
        accPrecedente = SensorManager.GRAVITY_EARTH;
        secousse = 0.00f;

        // ========================================================
        // MODE GAUCHER (Inversion de l'interface)
        // ========================================================
        androidx.constraintlayout.widget.ConstraintLayout rootManette = findViewById(R.id.root_manette);
        com.google.android.material.materialswitch.MaterialSwitch switchGaucher = findViewById(R.id.switch_gaucher);

        switchGaucher.setOnCheckedChangeListener((buttonView, isChecked) -> {
            androidx.constraintlayout.widget.ConstraintSet set = new androidx.constraintlayout.widget.ConstraintSet();
            set.clone(rootManette); // On "photographie" l'interface actuelle

            if (isChecked) {
                // MODE GAUCHER ACTIVÉ
                // On pousse le Joystick à 95% vers la droite
                set.setHorizontalBias(R.id.joystick_left, 0.95f);
                // On pousse le Bouton A vers la gauche.
                // (On met 0.20f au lieu de 0.05f pour laisser la place au bouton X de s'afficher sans sortir de l'écran)
                set.setHorizontalBias(R.id.btn_a, 0.20f);
            } else {
                // MODE DROITIER (Classique)
                // On remet le Joystick à 5% à gauche
                set.setHorizontalBias(R.id.joystick_left, 0.05f);
                // On remet le Bouton A à 95% à droite
                set.setHorizontalBias(R.id.btn_a, 0.95f);
            }

            // La ligne magique : Android va créer une animation fluide pour le déplacement !
            android.transition.TransitionManager.beginDelayedTransition(rootManette);

            // On applique les modifications
            set.applyTo(rootManette);
        });

    }

    // ECOUTEUR PHYSIQUE (DETECTION DE MOUVEMENT)
    private final SensorEventListener ecouteurSecousse = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // Recupere les forces tri-dimmensionnel du téléphone
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            accPrecedente = accActuelle;
            accActuelle = (float) Math.sqrt((double) (x*x + y*y + z*z));
            float delta = accActuelle - accPrecedente;
            secousse = secousse * 0.9f + delta;

            if (secousse > 12) {
                long tempsActuel = System.currentTimeMillis();

                if (tempsActuel - dernierTempsSecousse > 3000) {
                    dernierTempsSecousse = tempsActuel;
                    declencherActionSecousse();
                }
            }
        }
    };

    private void declencherActionSecousse() {
        faireVibrer(300);

        if (classeJoueur.contains("SOIGNEUR")) {
            android.widget.Toast.makeText(this, "RÉSURRECTION DE ZONE !!!", android.widget.Toast.LENGTH_SHORT).show();
            envoyerActionServeur("Secouer la bulle");
        } else {
            android.widget.Toast.makeText(this, "T'as secoué la manette !", android.widget.Toast.LENGTH_SHORT).show();
            envoyerActionServeur("SECOUSSE_PHYSIQUE");
        }
    }

    private void initialiserReseau() {
        try {
            mSocket = IO.socket("http://10.0.2.2:3000");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mSocket.on(Socket.EVENT_CONNECT, args -> {
           runOnUiThread(() -> {
               android.widget.Toast.makeText(this, "Connecté à l'arène !", Toast.LENGTH_SHORT).show();
           });
           envoyerInfosJoueur();
        });
            mSocket.connect();
    }

    private void envoyerInfosJoueur() {
        if (mSocket == null || !mSocket.connected()) return;

        try {
            JSONObject data = new JSONObject();
            data.put("pseudo", pseudoJoueur);
            data.put("classe", classeJoueur);
            data.put("couleur", couleurJoueur);

            mSocket.emit("nouveau_joueur", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void envoyerActionServeur(String typeAction) {
        if (mSocket == null || !mSocket.connected()) return;

        try {
            JSONObject data = new JSONObject();
            data.put("pseudo", pseudoJoueur);
            data.put("action", typeAction);

            mSocket.emit("action_joueur", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void envoyerMouvementServeur(float x, float y) {
        if (mSocket == null || !mSocket.connected()) return;

        try {
            org.json.JSONObject data = new org.json.JSONObject();
            data.put("x", x);
            data.put("y", y);

            // On l'envoie dans le tuyau "mouvement_joueur" que l'on a codé dans Node.js
            mSocket.emit("mouvement_joueur", data);
        }  catch (org.json.JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off(); // Arrête d'écouter
        }
    }

    private void faireVibrer(int dureeMilliSecondes){
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                vibrator.vibrate(VibrationEffect.createOneShot(dureeMilliSecondes, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(dureeMilliSecondes);
            }
        }
    }

    private  void lancerCooldown(final android.widget.Button bouton, int tempsEnSecondes) {
        final CharSequence texteOriginal = bouton.getText();

        bouton.setEnabled(false);
        bouton.setAlpha(0.5f);

        new android.os.CountDownTimer(tempsEnSecondes * 1000L, 1000){
            @Override
            public void onTick(long millisUntilFinished){
                int secondesRestantes = (int) (millisUntilFinished / 1000) + 1;
                bouton.setText(String.valueOf(secondesRestantes));
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

        if (vieActuelle <= 20 && vieActuelle > 0) {
            healthBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E74C3C")));

            if (healthBar.getAnimation() == null) {
                healthBar.startAnimation(clignotementRouge);
                faireVibrer(500);
            }
        } else if (vieActuelle > 20) {
            healthBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#52B766")));
            healthBar.clearAnimation();
        }

        if (vieActuelle == 0) {
            healthBar.clearAnimation();
            healthBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#000000")));
            android.widget.Toast.makeText(this, "GAME OVER", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // On rallume le capteur quand le joueur est sur l'écran
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(ecouteurSecousse, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // On coupe le capteur si l'appli est réduite
        if (sensorManager != null) {
            sensorManager.unregisterListener(ecouteurSecousse);
        }
    }
}
