package com.example.applicationmobile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class CustomActivity extends AppCompatActivity {
    private String classeChoisie = "";
    private String pseudoJoueur = "";
    private String couleurChoisie = "E74C3C";

    private String [] listeChapeaux = {
            "Couronne en or",
            "Casque Viking",
            "Chapeau Sorcier",
            "Casque Audio",
            "Auréole Fluo"
    };
    private int indexChapeau = 0;

    private ImageView imagePreview;
    private TextView textCurrentHat;
    private MaterialButton[] colorButtons;

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

            Intent intent = new Intent(CustomActivity.this, LobbyActivity.class);
            intent.putExtra("CLASSE_CHOISIE", classeChoisie);
            intent.putExtra("PSEUDO_JOUEUR", pseudoJoueur);
            intent.putExtra("COULEUR_JOUEUR", couleurChoisie);
            intent.putExtra("CHAPEAU_JOUEUR", listeChapeaux[indexChapeau]);

            startActivity(intent);
        });
    }





}
