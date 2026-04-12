package com.example.applicationmobile;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class JoystickView extends View {

    private Paint basePaint;
    private Paint stickPaint;
    private Paint shadowPaint;

    private float centerX, centerY;
    private float baseRadius, stickRadius;
    private float stickX, stickY;

    // Utilisé pour l'animation de retour
    private ValueAnimator returnAnimator;

    public interface JoystickListener {
        void onJoystickMoved(float xPercent, float yPercent);
    }
    private JoystickListener joystickCallBack;

    public void setJoystickListener(JoystickListener listener) {
        this.joystickCallBack = listener;
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Désactiver l'accélération matérielle stricte pour permettre les belles ombres douces
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // 1. STYLE DE LA BASE (Le trou du joystick)
        basePaint = new Paint();
        basePaint.setColor(Color.parseColor("#1A1B26")); // Couleur plus sombre pour faire un "creux"
        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setAntiAlias(true);
        // Ajout d'une ombre interne subtile
        basePaint.setShadowLayer(15f, 0, 5f, Color.parseColor("#000000"));

        // 2. STYLE DU STICK (Le bouton qu'on touche)
        stickPaint = new Paint();
        stickPaint.setColor(Color.parseColor("#414868"));
        stickPaint.setStyle(Paint.Style.FILL);
        stickPaint.setAntiAlias(true);
        // Ajout d'une grosse ombre portée pour le relief
        stickPaint.setShadowLayer(20f, 0, 10f, Color.parseColor("#111111"));

        // 3. STYLE DE LA BORDURE DU STICK (Optionnel, pour le faire ressortir)
        shadowPaint = new Paint();
        shadowPaint.setColor(Color.parseColor("#565F89"));
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setStrokeWidth(3f);
        shadowPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;

        // Ajustement des tailles pour laisser de la place aux ombres sans couper le dessin
        baseRadius = Math.min(w, h) / 3f;
        stickRadius = Math.min(w, h) / 6f;

        stickX = centerX;
        stickY = centerY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // On dessine la base, puis le stick, puis sa bordure
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
        canvas.drawCircle(stickX, stickY, stickRadius, stickPaint);
        canvas.drawCircle(stickX, stickY, stickRadius, shadowPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Si le joueur touche le joystick, on coupe l'animation de retour si elle était en cours
            if (returnAnimator != null && returnAnimator.isRunning()) {
                returnAnimator.cancel();
            }
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {

            // OPTIMISATION : Calcul plus rapide de la distance
            float dx = event.getX() - centerX;
            float dy = event.getY() - centerY;
            float displacement = (float) Math.hypot(dx, dy); // Math.hypot est ultra-optimisé en Java

            if (displacement < baseRadius) {
                stickX = event.getX();
                stickY = event.getY();
            } else {
                float ratio = baseRadius / displacement;
                stickX = centerX + dx * ratio;
                stickY = centerY + dy * ratio;
            }

            // Envoi des données (entre -1 et 1)
            if (joystickCallBack != null) {
                float xPercent = (stickX - centerX) / baseRadius;
                float yPercent = -(stickY - centerY) / baseRadius; // Inversé pour que Haut = Positif
                joystickCallBack.onJoystickMoved(xPercent, yPercent);
            }

            invalidate(); // Redessine

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            // 1. On prévient immédiatement le jeu qu'on lâche le stick pour stopper le perso
            if (joystickCallBack != null) {
                joystickCallBack.onJoystickMoved(0f, 0f);
            }

            // 2. On lance l'animation fluide de retour au centre
            resetJoystickAnimation();
        }

        return true;
    }

    // --- L'ANIMATION MAGIQUE ---
    private void resetJoystickAnimation() {
        PropertyValuesHolder xHolder = PropertyValuesHolder.ofFloat("x", stickX, centerX);
        PropertyValuesHolder yHolder = PropertyValuesHolder.ofFloat("y", stickY, centerY);

        returnAnimator = ValueAnimator.ofPropertyValuesHolder(xHolder, yHolder);
        returnAnimator.setDuration(200); // 200 millisecondes (rapide mais visible)

        // OvershootInterpolator fait "dépasser" un peu le stick avant de revenir, comme un élastique
        returnAnimator.setInterpolator(new OvershootInterpolator(1.5f));

        returnAnimator.addUpdateListener(animation -> {
            stickX = (float) animation.getAnimatedValue("x");
            stickY = (float) animation.getAnimatedValue("y");
            invalidate(); // Redessine à chaque étape de l'animation
        });

        returnAnimator.start();
    }
}
