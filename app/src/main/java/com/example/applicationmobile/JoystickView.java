package com.example.applicationmobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {

    private Paint basePaint;
    private Paint stickPaint;
    private float centerX, centerY;
    private float baseRadius, stickRadius;
    private float stickX, stickY;

    // Constructeur indispensable pour l'utilisation dans un fichier XML
    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Couleur de la base du joystick (le grand cercle fixe)
        basePaint = new Paint();
        basePaint.setColor(Color.parseColor("#24283B")); // Gris-bleu foncé
        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setAntiAlias(true);

        // Couleur du "stick" (le petit cercle qui bouge)
        stickPaint = new Paint();
        stickPaint.setColor(Color.parseColor("#414868")); // Gris-bleu plus clair
        stickPaint.setStyle(Paint.Style.FILL);
        stickPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Calcule le centre et les tailles de cercles quand la vue est créée
        centerX = w / 2f;
        centerY = h / 2f;
        baseRadius = Math.min(w, h) / 3f;
        stickRadius = Math.min(w, h) / 6f;

        // Place le stick au centre au démarrage
        stickX = centerX;
        stickY = centerY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Dessine la base, puis le stick par-dessus
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
        canvas.drawCircle(stickX, stickY, stickRadius, stickPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            // Calcule la distance entre le doigt et le centre
            float displacement = (float) Math.sqrt(Math.pow(event.getX() - centerX, 2) + Math.pow(event.getY() - centerY, 2));

            if (displacement < baseRadius) {
                // Le doigt est à l'intérieur du grand cercle, le stick suit le doigt
                stickX = event.getX();
                stickY = event.getY();
            } else {
                // Le doigt sort du cercle, on bloque le stick sur le bord
                float ratio = baseRadius / displacement;
                stickX = centerX + (event.getX() - centerX) * ratio;
                stickY = centerY + (event.getY() - centerY) * ratio;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            // Le joueur relâche l'écran, le stick revient au centre !
            stickX = centerX;
            stickY = centerY;
        }

        // Demande à Android de redessiner la vue avec les nouvelles coordonnées
        invalidate();
        return true;
    }
}