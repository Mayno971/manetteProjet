package com.example.applicationmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChoixClasseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choix_classe);

        findViewById(R.id.btn_guerrier).setOnClickListener(v -> lancerManette("GUERRIER"));
        findViewById(R.id.btn_mage).setOnClickListener(v -> lancerManette("MAGE"));
        findViewById(R.id.btn_soigneur).setOnClickListener(v -> lancerManette("SOIGNEUR"));
        findViewById(R.id.btn_tank).setOnClickListener(v -> lancerManette("TANK"));
    }

    private void lancerManette(String nomClasse){
        android.widget.Toast.makeText(this, "Bouton cliqué : " + nomClasse, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ChoixClasseActivity.this, ManetteActivity.class);
        intent.putExtra("CLASSE_CHOISIE", nomClasse);
        startActivity(intent);
    }
}
