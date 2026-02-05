package com.example.manettetel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View{
    private float centerX, centerY;
    private float baseRadius, hatRadius;
    private float currentX, currentY;
    private JoystickListener joystickListener;

    public JoystickView(Context context){
        super(context);
        init();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    private void init(){
        setFocusable(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        currentX = centerX;
        currentY = centerY;
        baseRadius = Math.min(w, h) / 3;
        hatRadius = Math.min(w, h) / 6;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint colors = new Paint();
        colors.setARGB(255, 50, 50, 50);
        colors.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, baseRadius, colors);
        colors.setARGB(255, 0, 150, 255); // Bleu
        canvas.drawCircle(currentX, currentY, hatRadius, colors);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            currentX = centerX;
            currentY = centerY;
            if (joystickListener != null) joystickListener.onJoystickMoved(0, 0);
        } else {
            float displacementX = event.getX() - centerX;
            float displacementY = event.getY() - centerY;
            float displacement = (float) Math.sqrt(displacementX * displacementX + displacementY * displacementY);

            if (displacement > baseRadius) {
                float ratio = baseRadius / displacement;
                currentX = centerX + (displacementX * ratio);
                currentY = centerY + (displacementY * ratio);
            } else {
                currentX = event.getX();
                currentY = event.getY();
            }
            if (joystickListener != null) {
                float percentX = (currentX - centerX) / baseRadius;
                float percentY = (currentY - centerY) / baseRadius;
                joystickListener.onJoystickMoved(percentX, percentY);
            }
        }
        invalidate();
        return true;
    }
    public interface JoystickListener {
        void onJoystickMoved(float xPercent, float yPercent);
    }

    public void setJoystickListener(JoystickListener listener) {
        this.joystickListener = listener;
    }


}
