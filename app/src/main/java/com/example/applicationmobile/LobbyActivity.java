package com.example.applicationmobile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.InetSocketAddress;

import io.socket.client.Socket;

public class LobbyActivity extends AppCompatActivity {
    private TextView title;
    private TextView stateConnexion;
    private Button btnValider;
    private Button btnRetour;

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
        btnValider = findViewById(R.id.button_valider);
        btnRetour = findViewById(R.id.button_retour);

        Intent intent = getIntent();
        pseudoJoueur = intent.getStringExtra("PSEUDO_JOUEUR");
        classeJoueur = intent.getStringExtra("CLASSE_CHOISIE");
        couleurJoueur = intent.getStringExtra("COULEUR_JOUEUR");

        estConnecte = intent.getBooleanExtra("CONNECTE", true);
        if (classeJoueur == null) classeJoueur = "GUERRIER"; // Sécurité

       title.setText("TUTORIEL : " + classeJoueur.toUpperCase());

        if (estConnecte) {
            stateConnexion.setText("Statut : Connecté à l'arène");
            stateConnexion.setTextColor(Color.parseColor("#52B766")); // Vert
            btnValider.setEnabled(true);
            btnValider.setAlpha(1.0f);
        } else {
            stateConnexion.setText("Statut : Déconnecté");
            stateConnexion.setTextColor(Color.parseColor("#E74C3C")); // Rouge
            btnValider.setEnabled(false); // On bloque le bouton
            btnValider.setAlpha(0.5f);
        }

        btnValider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goManette = new Intent(LobbyActivity.this, ManetteActivity.class);
                // On transfère les précieuses infos à la Manette
                goManette.putExtra("PSEUDO_JOUEUR", pseudoJoueur);
                goManette.putExtra("CLASSE_CHOISIE", classeJoueur);
                goManette.putExtra("COULEUR_JOUEUR", couleurJoueur);
                startActivity(goManette);
                finish(); // On ferme cet écran d'attente pour ne pas y revenir en faisant "Retour"
            }
        });
    }

}
