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
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedDeque;



public class ManetteActivity extends AppCompatActivity {
    private String pseudoJoueur;
    private String classeJoueur;
    private String couleurJoueur;

    private ProgressBar healthBar; // barre de vie
    private AlphaAnimation clignotementRouge;
    private int vieActuelle = 100;

    // Variables globales pour les paramètres
    private boolean estEnModeGaucher = false;
    private boolean vibrationsActives = true;
    private int memoireTaille = 50;
    private int memoireOpacite = 100;


    // --- VARIABLES POUR LA DÉTECTION DE SECOUSSE (SHAKE) ---
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float accActuelle;
    private float accPrecedente;
    private float secousse;
    private long dernierTempsSecousse = 0; // Pour le Cooldown physique
    private long dernierEnvoiJoystick = 0;

    // VARIABLES RESEAU (TCP)
    private java.net.Socket tcpSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean  isNetworkRunning = false;
    private ConcurrentLinkedDeque<String> messagesSortants = new ConcurrentLinkedDeque<>();
    private String sessionToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recuperation du token
        android.content.SharedPreferences prefs = getSharedPreferences("ColossusPrefs", MODE_PRIVATE);
        String sessionToken = prefs.getString("SESSION_TOKEN", null);

        if (sessionToken == null) {
            sessionToken = java.util.UUID.randomUUID().toString();
            prefs.edit().putString("SESSION_TOKEN", sessionToken).apply();
        }

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

        Button btnSos = findViewById(R.id.btn_sos);

        initialiserReseau();

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
        ImageView btnSettings = findViewById(R.id.btn_settings);

        btnSettings.setOnClickListener(v -> {
            ParametreFragment fragment = new ParametreFragment();

            fragment.setEtatsInitiaux(estEnModeGaucher, vibrationsActives, memoireTaille, memoireOpacite);

            // On écoute ce que le fragment nous dit de faire
            fragment.setListener(new ParametreFragment.ParametresListener() {
                @Override
                public void onTailleBoutonsChangee(float echelle) {
                    btnA.setScaleX(echelle); btnA.setScaleY(echelle);
                    btnB.setScaleX(echelle); btnB.setScaleY(echelle);
                    btnX.setScaleX(echelle); btnX.setScaleY(echelle);
                    btnY.setScaleX(echelle); btnY.setScaleY(echelle);
                    joystickLeft.setScaleX(echelle); joystickLeft.setScaleY(echelle);
                }

                @Override
                public void onOpaciteChangee(float alpha) {
                    memoireOpacite = (int) (alpha * 100); // Sauvegarde
                    // La méthode setAlpha gère la transparence d'un élément (1 = plein, 0.5 = à moitié invisible)
                    btnA.setAlpha(alpha);
                    btnB.setAlpha(alpha);
                    btnX.setAlpha(alpha);
                    btnY.setAlpha(alpha);
                    joystickLeft.setAlpha(alpha);
                }

                @Override
                public void onModeGaucherChange(boolean isGaucher) {
                    estEnModeGaucher = isGaucher; // On sauvegarde le choix

                    androidx.constraintlayout.widget.ConstraintLayout rootManette = findViewById(R.id.root_manette);
                    androidx.constraintlayout.widget.ConstraintSet set = new androidx.constraintlayout.widget.ConstraintSet();
                    set.clone(rootManette);

                    if (isGaucher) {
                        // MODE GAUCHER ACTIVÉ
                        set.setHorizontalBias(R.id.joystick_left, 0.95f);
                        set.setHorizontalBias(R.id.btn_a, 0.20f);
                        // N'oublie pas d'inverser aussi les autres boutons (B, X, Y) si besoin !
                    } else {
                        // MODE DROITIER (Classique)
                        set.setHorizontalBias(R.id.joystick_left, 0.05f);
                        set.setHorizontalBias(R.id.btn_a, 0.95f);
                    }

                    android.transition.TransitionManager.beginDelayedTransition(rootManette);
                    set.applyTo(rootManette);
                }

                @Override
                public void onVibrationsChange(boolean isVibrationActive) {
                    vibrationsActives = isVibrationActive;
                    // Astuce : il faudra juste englober ton code de vibration actuel d'un "if (vibrationsActives) { faireVibrer(); }"
                }
            });

            // On affiche le fragment !
            fragment.show(getSupportFragmentManager(), "Parametres");
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

        androidx.constraintlayout.widget.ConstraintLayout rootManette = findViewById(R.id.root_manette);

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
            Toast.makeText(this, "RÉSURRECTION DE ZONE !!!", Toast.LENGTH_SHORT).show();
            envoyerActionServeur("Secouer la bulle");
        } else {
            Toast.makeText(this, "T'as secoué la manette !", Toast.LENGTH_SHORT).show();
            envoyerActionServeur("SECOUSSE_PHYSIQUE");
        }
    }

    private void initialiserReseau() {
        EnvoiThread threadReseau = new EnvoiThread("172.18.120.232", 7777);
        threadReseau.start();
    }

    private void envoyerInfosJoueur() {try {
        JSONObject data = new JSONObject();
        data.put("type", "nouveau_joueur");
        data.put("pseudo", pseudoJoueur);
        data.put("classe", classeJoueur);
        data.put("couleur", couleurJoueur);
        data.put("token", sessionToken);

        messagesSortants.add(data.toString());
    } catch (JSONException e) {
        e.printStackTrace();
    }
    }

    private void envoyerActionServeur(String typeAction) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "action_joueur");
            data.put("pseudo", pseudoJoueur);
            data.put("action", typeAction);

            messagesSortants.add(data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void envoyerMouvementServeur(float x, float y) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "mouvement_joueur");
            data.put("x", x);
            data.put("y", y);
            data.put("token", sessionToken);

            messagesSortants.add(data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isNetworkRunning = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (tcpSocket != null && !tcpSocket.isClosed()) tcpSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
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

    // Gère la réception d'information du serveur (ex: perte de vie)
    private void traiterMessageServeur(String json) {
        try {
            JSONObject data = new JSONObject(json);
            String type = data.getString("type");

            if (type.equals("update_health")) {
                int nouvelleVie = data.getInt("vie");
                // Mettre à jour l'interface UI (toujours sur le thread principal)
                runOnUiThread(() -> mettreAjourVie(nouvelleVie));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    class EnvoiThread extends Thread {
        private String ipServeur;
        private int portServeur;

        public EnvoiThread(String ip, int port) {
            this.ipServeur = ip;
            this.portServeur = port;
        }

        @Override
        public void run() {
            try {
                // 1. On ouvre la connexion UNE SEULE FOIS
                tcpSocket = new java.net.Socket(ipServeur, portServeur);
                out = new PrintWriter(tcpSocket.getOutputStream(), true);
                in = new BufferedReader(new java.io.InputStreamReader(tcpSocket.getInputStream()));
                isNetworkRunning = true;

                Log.i("RESEAU", "Connecté au serveur Unity !");

                // Met à jour l'interface
                runOnUiThread(() -> {
                    Toast.makeText(ManetteActivity.this, "Connecté à l'arène !", Toast.LENGTH_SHORT).show();
                    envoyerInfosJoueur(); // Envoie les infos d'initialisation
                });

                // 2. SOUS-THREAD : Pour écouter les réponses du serveur (Ex: le boss nous frappe)
                Thread ecouteThread = new Thread(() -> {
                    try {
                        String reponseServeur;
                        // On écoute en boucle
                        while (isNetworkRunning && (reponseServeur = in.readLine()) != null) {
                            final String jsonReponse = reponseServeur;
                            runOnUiThread(() -> traiterMessageServeur(jsonReponse));
                        }
                    } catch (java.io.IOException e) {
                        Log.e("RESEAU", "Erreur de lecture : " + e.getMessage());
                    }
                });
                ecouteThread.start();

                // 3. BOUCLE PRINCIPALE : Envoi des actions de la manette
                while (isNetworkRunning) {
                    String messageAEnvoyer = messagesSortants.poll();

                    if (messageAEnvoyer != null) {
                        out.println(messageAEnvoyer); // Envoi au serveur !
                        // Log.i("RESEAU", "Commande envoyée : " + messageAEnvoyer); // ATTENTION: Ça va spammer ta console avec le Joystick
                    }
                    // Pause de 10ms pour ne pas faire exploser le processeur du téléphone
                    Thread.sleep(10);
                }

                // 4. Fermeture propre quand on quitte l'application
                in.close();
                out.close();
                tcpSocket.close();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("RESEAU", "Erreur globale : " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ManetteActivity.this, "Impossible de joindre l'arène", Toast.LENGTH_SHORT).show());
            }
        }
    }
}
