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

        // Recuperer la classe envoyée par l'ecran choix classe précedent
        if (getIntent().hasExtra("CLASSE_CHOISIE")) {
            classeChoisie = getIntent().getStringExtra("CLASSE_CHOISIE");
        }

        // Lier les évènements visuels au code
        imagePreview = findViewById(R.id.image_preview_character);
        textCurrentHat = findViewById(R.id.text_current_hat);
        EditText inputPseudo = findViewById(R.id.input_pseudo);
        MaterialButton btnRejoindre = findViewById(R.id.btn_rejoindre_arene);

        // Initialisation des couleurs et chapeaux
        setupColorSelection();
        setupHatNavigation();

        btnRejoindre.setOnClickListener(v -> {
            pseudoJoueur = inputPseudo.getText().toString().trim();

            if (pseudoJoueur.isEmpty()) {
                pseudoJoueur = "Joueur inconnu";
            }

            Intent intent = new Intent(CustomActivity.this, ManetteActivity.class);
            intent.putExtra("CLASSE_CHOISIE", classeChoisie);
            intent.putExtra("PSEUDO_JOUEUR", pseudoJoueur);
            intent.putExtra("COULEUR_JOUEUR", couleurChoisie);
            intent.putExtra("CHAPEAU_JOUEUR", listeChapeaux[indexChapeau]);

            startActivity(intent);
        });
    }

    private void setupColorSelection() {
        colorButtons = new MaterialButton[]{
                findViewById(R.id.color_red),
                findViewById(R.id.color_blue),
                findViewById(R.id.color_green),
                findViewById(R.id.color_yellow),
                findViewById(R.id.color_purple)
        };
        String[] colorCodes = {"#E74C3C", "#3498DB", "#2ECC71", "#F1C40F", "#9B59B6"};

        for (int i=0; i < colorButtons.length; i++) {
            final int index = i;
            colorButtons[i].setOnClickListener(v ->{
                couleurChoisie = colorCodes[index];
                imagePreview.setColorFilter(Color.parseColor(couleurChoisie));
                for (MaterialButton btn : colorButtons){
                    btn.setStrokeWidth(0);
                }
                colorButtons[index].setStrokeWidth(8);
                colorButtons[index].setStrokeColorResource(android.R.color.white);
            });
        }
    }

    private void setupHatNavigation() {
        ImageButton btnPrev = findViewById(R.id.btn_prev_hat);
        ImageButton btnNext = findViewById(R.id.btn_next_hat);

        textCurrentHat.setText(listeChapeaux[indexChapeau]);

        btnNext.setOnClickListener(v -> {
            indexChapeau++;
            if (indexChapeau >= listeChapeaux.length) indexChapeau = 0; // Boucle au début
            textCurrentHat.setText(listeChapeaux[indexChapeau]);
        });

        btnPrev.setOnClickListener(v -> {
            indexChapeau--;
            if (indexChapeau < 0) indexChapeau = listeChapeaux.length - 1; // Boucle à la fin
            textCurrentHat.setText(listeChapeaux[indexChapeau]);
        });
    }
}
