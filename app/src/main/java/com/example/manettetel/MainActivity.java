package com.example.manettetel;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {
    private static final String SERVER_IP = "193.55.29.172";
    private static final int SERVER_PORT = 4444;
    private long lastLeftJoyTime = 0;
    private long lastRightJoyTime = 0;
    private static final int SEND_INTERVAL = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupButton(R.id.btnUp, "UP");
        setupButton(R.id.btnDown, "DOWN");
        setupButton(R.id.btnLeft, "LEFT");
        setupButton(R.id.btnRight, "RIGHT");

        setupButton(R.id.btnA, "BTN_A");
        setupButton(R.id.btnB, "BTN_B");
        setupButton(R.id.btnX, "BTN_X");
        setupButton(R.id.btnY, "BTN_Y");

        JoystickView joystickLeft = findViewById(R.id.joystickLeft);
        joystickLeft.setJoystickListener(new JoystickView.JoystickListener() {
            @Override
            public void onJoystickMoved(float xPercent, float yPercent) {

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLeftJoyTime > SEND_INTERVAL) {
                    // Format : L_JOY:0.50,-0.20
                    String msg = "L_JOY:" + formatFloat(xPercent) + "," + formatFloat(yPercent);
                    sendMessage(msg);
                    lastLeftJoyTime = currentTime;
                }
            }
        });

        JoystickView joystickRight = findViewById(R.id.joystickRight);
        joystickRight.setJoystickListener(new JoystickView.JoystickListener() {
            @Override
            public void onJoystickMoved(float xPercent, float yPercent) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRightJoyTime > SEND_INTERVAL) {
                    // Format : R_JOY:0.50,-0.20
                    String msg = "R_JOY:" + formatFloat(xPercent) + "," + formatFloat(yPercent);
                    sendMessage(msg);
                    lastRightJoyTime = currentTime;
                }
            }
        });
    }

    private void setupButton(int buttonId, final String messageToSend) {
        Button btn = findViewById(buttonId);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(messageToSend);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private String formatFloat(float value) {
        return String.format("%.2f", value).replace(",", ".");
    }

    private void sendMessage(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    InetAddress address = InetAddress.getByName(SERVER_IP);
                    byte[] data = message.getBytes();

                    DatagramPacket packet = new DatagramPacket(data, data.length, address, SERVER_PORT);
                    socket.send(packet);

                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
