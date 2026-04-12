package com.example.applicationmobile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;


public class CustomActivity extends AppCompatActivity {
    private String classeChoisie = "GUERRIER";
    private String pseudoJoueur = "";
    private final String couleurJoueur = "#FFFFFF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom);

        if (getIntent().hasExtra("CLASSE_CHOISIE")) {
            classeChoisie = getIntent().getStringExtra("CLASSE_CHOISIE");
        }

        EditText inputPseudo = findViewById(R.id.input_pseudo);
        MaterialButton btnRejoindre = findViewById(R.id.btn_rejoindre_arene);

        InputFilter alphanumericFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++){
                if (!Character.isLetterOrDigit(source.charAt(i)) && source.charAt(i) != ' '){
                    return "";
                }
            }
            return null;
        };
        inputPseudo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12), alphanumericFilter});

        SharedPreferences preferences = getSharedPreferences("ColossusPrefs", MODE_PRIVATE);
        String savedPseudo = preferences.getString("LAST_PSEUDO", "");
        inputPseudo.setText(savedPseudo);

        activerBouton(btnRejoindre, !savedPseudo.trim().isEmpty());
        inputPseudo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                activerBouton(btnRejoindre, !s.toString().trim().isEmpty());
            }
        });

        inputPseudo.setOnEditorActionListener((v,actionId, event) ->{
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                if (btnRejoindre.isEnabled()){
                    btnRejoindre.performClick();
                }
                return true;
            }
            return false;
        });

        btnRejoindre.setOnClickListener(v -> {
            pseudoJoueur = inputPseudo.getText().toString().trim();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            preferences.edit().putString("LAST_PSEUDO", pseudoJoueur).apply();

            Intent intent = new Intent(CustomActivity.this, LobbyActivity.class);
            intent.putExtra("CLASSE_CHOISIE", classeChoisie);
            intent.putExtra("PSEUDO_JOUEUR", pseudoJoueur);
            intent.putExtra("COULEUR_JOUEUR", couleurJoueur); // On passe la couleur !
            intent.putExtra("ROOM_CODE", getIntent().getStringExtra("ROOM_CODE"));
            startActivity(intent);

        });
    }

    private void activerBouton(MaterialButton bouton, boolean actif){
        bouton.setEnabled(actif);
        bouton.setAlpha(actif ? 1.0f: 0.5f);
    }
}