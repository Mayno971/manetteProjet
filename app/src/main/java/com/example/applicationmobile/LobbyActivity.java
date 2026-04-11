package com.example.applicationmobile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LobbyActivity extends AppCompatActivity {

    private String pseudoJoueur;
    private String classeJoueur;
    private String couleurJoueur;
    private String roomCode;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        TextView title = findViewById(R.id.textView_title);
        TextView stateConnexion = findViewById(R.id.textView_stateConnexion);
        Button btnValider = findViewById(R.id.button_valider);
        Button btnRetour = findViewById(R.id.button_retour);

        // 1. Récupération de TOUTES les données de l'écran précédent
        Intent intent = getIntent();
        pseudoJoueur = intent.getStringExtra("PSEUDO_JOUEUR");
        classeJoueur = intent.getStringExtra("CLASSE_CHOISIE");
        couleurJoueur = intent.getStringExtra("COULEUR_JOUEUR");
        roomCode = intent.getStringExtra("ROOM_CODE"); // On n'oublie pas le code !

        boolean estConnecte = intent.getBooleanExtra("CONNECTE", true);

        if (classeJoueur == null) classeJoueur = "GUERRIER";

        title.setText("TUTORIEL : " + classeJoueur.toUpperCase());

        // 2. Gestion de l'affichage de l'état de connexion
        if (estConnecte) {
            stateConnexion.setText("Statut : Prêt pour l'arène");
            stateConnexion.setTextColor(Color.parseColor("#52B766")); // Vert
            btnValider.setEnabled(true);
            btnValider.setAlpha(1.0f);
        } else {
            stateConnexion.setText("Statut : Serveur injoignable");
            stateConnexion.setTextColor(Color.parseColor("#E74C3C")); // Rouge
            btnValider.setEnabled(false);
            btnValider.setAlpha(0.5f);
        }

        // Appel de la méthode pour changer les textes et icônes
        mettreAjourTutoriel();

        // 3. Remplacement par des Lambdas (Code plus moderne et lisible)
        btnValider.setOnClickListener(v -> {
            Intent goManette = new Intent(LobbyActivity.this, ManetteActivity.class);
            goManette.putExtra("PSEUDO_JOUEUR", pseudoJoueur);
            goManette.putExtra("CLASSE_CHOISIE", classeJoueur);
            goManette.putExtra("COULEUR_JOUEUR", couleurJoueur);
            goManette.putExtra("ROOM_CODE", roomCode); // On transmet à la Manette
            startActivity(goManette);
            finish();
        });

        btnRetour.setOnClickListener(v -> {
            Intent goRetour = new Intent(LobbyActivity.this, ChoixClasseActivity.class);
            // SÉCURITÉ : On renvoie le code de la salle en arrière pour ne pas le perdre !
            goRetour.putExtra("ROOM_CODE", roomCode);
            startActivity(goRetour);
            finish();
        });
    }

    private void mettreAjourTutoriel(){
        TextView descA = findViewById(R.id.textView_desc_A);
        TextView descB = findViewById(R.id.textView_desc_B);
        TextView descX = findViewById(R.id.textView_desc_X);
        TextView descY = findViewById(R.id.textView_desc_Y);

        ImageView imgA = findViewById(R.id.imageView_action_A);
        ImageView imgB = findViewById(R.id.imageView_action_B);
        ImageView imgX = findViewById(R.id.imageView_action_X);
        ImageView imgY = findViewById(R.id.imageView_action_Y);

        // 4. On définit des images par défaut AVANT le switch pour éviter les oublis
        imgA.setImageResource(R.drawable.logo);
        imgB.setImageResource(R.drawable.baseline_pending_actions_24);
        imgX.setImageResource(R.drawable.baseline_pending_actions_24);
        imgY.setImageResource(R.drawable.baseline_pending_actions_24);

        switch (classeJoueur.toUpperCase()) {
            case "GUERRIER" :
                descA.setText("Coup d'épée");
                descB.setText("Non assigné");
                descX.setText("Non assigné");
                descY.setText("Aller au healer");
                break;
            case "MAGE" :
                descA.setText("Boule de feu");
                descB.setText("Cube Magique");
                descX.setText("Non Assigné");
                descY.setText("Aller au soigneur");
                break;
            case "SOIGNEUR" :
                descA.setText("Boule de vie");
                descB.setText("Zone de soin");
                descX.setText("Non assigné");
                descY.setText("Aller au healer");
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