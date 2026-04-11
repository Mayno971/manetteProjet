package com.example.applicationmobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class CustomActivity extends AppCompatActivity {
    private String classeChoisie = "GUERRIER";
    private String pseudoJoueur = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom);

        if (getIntent().hasExtra("CLASSE_CHOISIE")) {
            classeChoisie = getIntent().getStringExtra("CLASSE_CHOISIE");
        }

        EditText inputPseudo = findViewById(R.id.input_pseudo);
        MaterialButton btnRejoindre = findViewById(R.id.btn_rejoindre_arene);

        btnRejoindre.setOnClickListener(v -> {
            pseudoJoueur = inputPseudo.getText().toString().trim();

            if (pseudoJoueur.isEmpty()) {
                pseudoJoueur = "Joueur inconnu";
            }

            // On va vers le Lobby (Tutoriel), pas directement à la manette !
            Intent intent = new Intent(CustomActivity.this, LobbyActivity.class);
            intent.putExtra("CLASSE_CHOISIE", classeChoisie);
            intent.putExtra("PSEUDO_JOUEUR", pseudoJoueur);
            startActivity(intent);
        });
    }
}