package com.example.applicationmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ConnexionActivity extends AppCompatActivity{
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connexion);

        EditText inputCode = findViewById(R.id.input_code);
        Button btnValider = findViewById(R.id.btn_valider);

        inputCode.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

        SharedPreferences prefs = getSharedPreferences("ColossusPrefs", MODE_PRIVATE);
        String lastCode = prefs.getString("LAST_ROOM_CODE","");
        inputCode.setText(lastCode);

        btnValider.setOnClickListener(v -> {
            String code = inputCode.getText().toString().trim().toUpperCase();

            if (code.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer un code", Toast.LENGTH_SHORT).show();
            } else if (code.length() < 4) {
                Toast.makeText(this, "Le code de la salle est trop court", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString("LAST_ROOM_CODE", code).apply();
                Intent intent = new Intent(ConnexionActivity.this, ChoixClasseActivity.class);
                intent.putExtra("ROOM_CODE", code);
                startActivity(intent);
            }
        });
    }
}
