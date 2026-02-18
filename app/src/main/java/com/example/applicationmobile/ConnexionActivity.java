package com.example.applicationmobile;

import android.content.Intent;
import android.os.Bundle;
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

        btnValider.setOnClickListener(v -> {
            String code = inputCode.getText().toString();
            if (!code.isEmpty()){
                startActivity(new Intent(ConnexionActivity.this, ChoixClasseActivity.class));
            } else {
                Toast.makeText(this, "Veuillez entrez un code", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
