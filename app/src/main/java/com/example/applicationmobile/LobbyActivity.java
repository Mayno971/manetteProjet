package com.example.applicationmobile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.InetSocketAddress;

import io.socket.client.Socket;

public class LobbyActivity extends AppCompatActivity {
    private TextView title;
    private TextView stateConnexion;
    private Button btnValider;
    private Button btnRetour;

    private ImageView imgA;
    private ImageView imgB;
    private ImageView imgX;
    private ImageView imgY;


    private TextView descA;
    private TextView descB;
    private TextView descX;
    private TextView descY;

    private String pseudoJoueur;
    private String classeJoueur;
    private String couleurJoueur;

    private boolean estConnecte;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        title = findViewById(R.id.textView_title);
        stateConnexion = findViewById(R.id.textView_stateConnexion);
        descA = findViewById(R.id.textView_desc_A);
        descB = findViewById(R.id.textView_desc_B);
        descX = findViewById(R.id.textView_desc_X);
        descY = findViewById(R.id.textView_desc_Y);

        imgA = findViewById(R.id.imageView_action_A);
        imgB = findViewById(R.id.imageView_action_B);
        imgX = findViewById(R.id.imageView_action_X);
        imgY = findViewById(R.id.imageView_action_Y);

        btnValider = findViewById(R.id.button_valider);
        btnRetour = findViewById(R.id.button_retour);

        Intent intent = getIntent();
        pseudoJoueur = intent.getStringExtra("PSEUDO_JOUEUR");
        classeJoueur = intent.getStringExtra("CLASSE_CHOISIE");
        couleurJoueur = intent.getStringExtra("COULEUR_JOUEUR");

        estConnecte = intent.getBooleanExtra("CONNECTE", true);

        if (classeJoueur == null) classeJoueur = "GUERRIER";

       title.setText("TUTORIEL : " + classeJoueur.toUpperCase());

        if (estConnecte) {
            stateConnexion.setText("Statut : Connecté à l'arène");
            stateConnexion.setTextColor(Color.parseColor("#52B766")); // Vert
            btnValider.setEnabled(true);
            btnValider.setAlpha(1.0f);
        } else {
            stateConnexion.setText("Statut : Déconnecté");
            stateConnexion.setTextColor(Color.parseColor("#E74C3C")); // Rouge
            btnValider.setEnabled(false);
            btnValider.setAlpha(0.5f);
        }

        mettreAjourTutoriel();

        btnValider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goManette = new Intent(LobbyActivity.this, ManetteActivity.class);
                // On transfère les précieuses infos à la Manette
                goManette.putExtra("PSEUDO_JOUEUR", pseudoJoueur);
                goManette.putExtra("CLASSE_CHOISIE", classeJoueur);
                goManette.putExtra("COULEUR_JOUEUR", couleurJoueur);
                startActivity(goManette);
                finish();
            }
        });

        btnRetour.setOnClickListener(v -> {
            Intent goRetour = new Intent(LobbyActivity.this, ChoixClasseActivity.class);
            startActivity(goRetour);
            finish();
        });
    }

    private void mettreAjourTutoriel(){
        switch (classeJoueur.toUpperCase()) {
            case "GUERRIER" :
                descA.setText("Coup d'épée");
                imgA.setImageResource(R.drawable.logo);

                descB.setText("Non assigné");
                descX.setText("Non assigné");
                descY.setText("Aller au healer");
                break;
            case "MAGE" :
                descA.setText("Boule de feu");
                descB.setText("Cube Magique");
                descX.setText("Non Assigné");
                descY.setText("Se projeter vers le soigneur");
                break;
            case "SOIGNEUR" :
                descA.setText("Boule de vie");
                descB.setText("Zone de soin");
                descX.setText("Non assigné");
                descY.setText("Aller au Healer");
                break;
            case "TANK" :
                descA.setText("Bouclier");
                descB.setText("Attraper");
                descX.setText("Non assigné");
                descY.setText("Aller au healer");
                break;
            default:
                descA.setText("Coup");
                descB.setText("Esquive");
                descX.setText("Compétence");
                descY.setText("Ultime");
                break;
        }
    }

}
