package com.example.applicationmobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class ManetteActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manette);

        TextView textClasseJoueur = findViewById(R.id.text_classe_joueur);

        android.widget.ImageView btnDisconnect = findViewById(R.id.btn_disconnect);
        btnDisconnect.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(ManetteActivity.this, ConnexionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        String classeRecuperee = getIntent().getStringExtra("CLASSE_CHOISIE");
        if (classeRecuperee != null){
            textClasseJoueur.setText(classeRecuperee);
        }

        ProgressBar healthBar = findViewById(R.id.top_bar).findViewById(android.R.id.progress);
        Button btnA = findViewById(R.id.btn_a); // Attaque
        Button btnX = findViewById(R.id.btn_x); // Spécial
        Button btnY = findViewById(R.id.btn_y); // Compétence ultime

        btnA.setOnClickListener(v -> {
            faireVibrer(150);
            android.widget.Toast.makeText(this, "Attaque rapide !", android.widget.Toast.LENGTH_SHORT).show();
            // Logique d'envoi de l'attaque au serveur Node.js via WebSocket
        });

        btnX.setOnClickListener(v -> {
            faireVibrer(150);
            // Logique de compétence spéciale avec cooldown
            android.widget.Toast.makeText(this, "Sortilège lancé !", android.widget.Toast.LENGTH_SHORT).show();

            lancerCooldown(btnX, 3);
        });

        btnY.setOnClickListener(v -> {
            faireVibrer(200);
            android.widget.Toast.makeText(this, "COMPÉTENCE ULTIME !!!", android.widget.Toast.LENGTH_SHORT).show();

            // On lance le chronomètre de 10 secondes sur ce bouton !
            lancerCooldown(btnY, 10);
        });
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
                // 4. Quand le chrono est fini, on restaure le bouton à son état normal !
                bouton.setEnabled(true);
                bouton.setAlpha(1.0f);
                bouton.setText(texteOriginal);
            }
        }.start();
    }
}
