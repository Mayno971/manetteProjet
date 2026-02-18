package com.example.applicationmobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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
        Button btnA = findViewById(R.id.btn_a); // Attaque [cite: 52]
        Button btnX = findViewById(R.id.btn_x); // Spécial [cite: 53]

        btnA.setOnClickListener(v -> {
            // Logique d'envoi de l'attaque au serveur Node.js via WebSocket
            Toast.makeText(this, "Attaque envoyée !", Toast.LENGTH_SHORT).show();
        });

        btnX.setOnClickListener(v -> {
            // Logique de compétence spéciale avec cooldown
            Toast.makeText(this, "Compétence Spéciale !", Toast.LENGTH_SHORT).show();
        });
    }
}
