package com.example.applicationmobile;

import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.OrientationEventListener;
import androidx.appcompat.app.AppCompatActivity;

public class RotationActivity extends AppCompatActivity{
    private OrientationEventListener ecouteurOrientation;
    @Override
    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation);

        ecouteurOrientation = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int angle){
                if (angle == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;
                }
                if ((angle >= 70 && angle <= 110) || (angle >= 250 && angle <= 290)) {
                    ecouteurOrientation.disable();

                    Intent intent = new Intent(RotationActivity.this, ConnexionActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ecouteurOrientation != null && ecouteurOrientation.canDetectOrientation()){
            ecouteurOrientation.enable();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ecouteurOrientation != null){
            ecouteurOrientation.disable();
        }
    }
}
