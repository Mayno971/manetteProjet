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
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        basePaint = new Paint();
        basePaint.setColor(Color.parseColor("#1A1B26"));
        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setAntiAlias(true);
        basePaint.setShadowLayer(15f, 0, 5f, Color.parseColor("#000000"));

        stickPaint = new Paint();
        stickPaint.setColor(Color.parseColor("#414868"));
        stickPaint.setStyle(Paint.Style.FILL);
        stickPaint.setAntiAlias(true);
        stickPaint.setShadowLayer(20f, 0, 10f, Color.parseColor("#111111"));

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

        baseRadius = Math.min(w, h) / 3f;
        stickRadius = Math.min(w, h) / 6f;

        stickX = centerX;
        stickY = centerY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
        canvas.drawCircle(stickX, stickY, stickRadius, stickPaint);
        canvas.drawCircle(stickX, stickY, stickRadius, shadowPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (returnAnimator != null && returnAnimator.isRunning()) {
                returnAnimator.cancel();
            }
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {

            float dx = event.getX() - centerX;
            float dy = event.getY() - centerY;
            float displacement = (float) Math.hypot(dx, dy);

            if (displacement < baseRadius) {
                stickX = event.getX();
                stickY = event.getY();
            } else {
                float ratio = baseRadius / displacement;
                stickX = centerX + dx * ratio;
                stickY = centerY + dy * ratio;
            }
            if (joystickCallBack != null) {
                float xPercent = (stickX - centerX) / baseRadius;
                float yPercent = -(stickY - centerY) / baseRadius;
                joystickCallBack.onJoystickMoved(xPercent, yPercent);
            }

            invalidate();

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (joystickCallBack != null) {
                joystickCallBack.onJoystickMoved(0f, 0f);
            }

            resetJoystickAnimation();
        }

        return true;
    }

    private void resetJoystickAnimation() {
        PropertyValuesHolder xHolder = PropertyValuesHolder.ofFloat("x", stickX, centerX);
        PropertyValuesHolder yHolder = PropertyValuesHolder.ofFloat("y", stickY, centerY);

        returnAnimator = ValueAnimator.ofPropertyValuesHolder(xHolder, yHolder);
        returnAnimator.setDuration(200);
        returnAnimator.setInterpolator(new OvershootInterpolator(1.5f));

        returnAnimator.addUpdateListener(animation -> {
            stickX = (float) animation.getAnimatedValue("x");
            stickY = (float) animation.getAnimatedValue("y");
            invalidate();
        });

        returnAnimator.start();
    }
}
