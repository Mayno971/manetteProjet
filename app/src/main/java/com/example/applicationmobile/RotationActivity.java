package com.example.applicationmobile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class RotationActivity extends AppCompatActivity{
    @Override
    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(RotationActivity.this, ConnexionActivity.class));
            finish();
        }, 2000);
    }
}
