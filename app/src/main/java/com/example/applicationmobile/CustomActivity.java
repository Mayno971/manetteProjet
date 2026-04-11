package com.example.applicationmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.Toast;

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

        inputPseudo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});

        SharedPreferences preferences = getSharedPreferences("ColossusPrefs", MODE_PRIVATE);
        String savedPseudo = preferences.getString("LAST_PSEUDO", "");
        inputPseudo.setText(savedPseudo);

        btnRejoindre.setOnClickListener(v -> {
            pseudoJoueur = inputPseudo.getText().toString().trim();

            if (pseudoJoueur.isEmpty()) {
                Toast.makeText(CustomActivity.this, "Veuillez entrer un pseudo", Toast.LENGTH_SHORT).show();
                return;
            }
            preferences.edit().putString("LAST_PSEUDO", pseudoJoueur).apply();

            Intent intent = new Intent(CustomActivity.this, LobbyActivity.class);
            intent.putExtra("CLASSE_CHOISIE", classeChoisie);
            intent.putExtra("PSEUDO_JOUEUR", pseudoJoueur);
            intent.putExtra("ROOM_CODE", getIntent().getStringExtra("ROOM_CODE"));
            startActivity(intent);
        });
    }
}