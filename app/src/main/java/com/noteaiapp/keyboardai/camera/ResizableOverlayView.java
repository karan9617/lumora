package com.noteaiapp.keyboardai.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ResizableOverlayView extends View {
    private Paint borderPaint;
    private Paint shadePaint;
    private RectF rect;
    private float cornerRadius = 40f;
    private float handleRadius = 30f;
    private boolean dragging = false;
    private int activeCorner = -1;

    public ResizableOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        borderPaint = new Paint();
        borderPaint.setColor(Color.CYAN);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5f);

        shadePaint = new Paint();
        shadePaint.setColor(Color.parseColor("#88000000")); // semi-transparent shade

        rect = new RectF(300, 600, 800, 1200); // initial rectangle (adjust as needed)
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw shaded background outside rectangle
        canvas.drawRect(0, 0, getWidth(), rect.top, shadePaint);
        canvas.drawRect(0, rect.bottom, getWidth(), getHeight(), shadePaint);
        canvas.drawRect(0, rect.top, rect.left, rect.bottom, shadePaint);
        canvas.drawRect(rect.right, rect.top, getWidth(), rect.bottom, shadePaint);

        // Draw rectangle border
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint);

        // Draw handles (corners)
        canvas.drawCircle(rect.left, rect.top, handleRadius, borderPaint);
        canvas.drawCircle(rect.right, rect.top, handleRadius, borderPaint);
        canvas.drawCircle(rect.left, rect.bottom, handleRadius, borderPaint);
        canvas.drawCircle(rect.right, rect.bottom, handleRadius, borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                activeCorner = getTouchedCorner(x, y);
                dragging = (activeCorner != -1);
                return dragging;

            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    switch (activeCorner) {
                        case 0: rect.left = x; rect.top = y; break; // top-left
                        case 1: rect.right = x; rect.top = y; break; // top-right
                        case 2: rect.left = x; rect.bottom = y; break; // bottom-left
                        case 3: rect.right = x; rect.bottom = y; break; // bottom-right
                    }
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                dragging = false;
                activeCorner = -1;
                return true;
        }

        return super.onTouchEvent(event);
    }

    private int getTouchedCorner(float x, float y) {
        if (distance(x, y, rect.left, rect.top) < handleRadius * 2) return 0;
        if (distance(x, y, rect.right, rect.top) < handleRadius * 2) return 1;
        if (distance(x, y, rect.left, rect.bottom) < handleRadius * 2) return 2;
        if (distance(x, y, rect.right, rect.bottom) < handleRadius * 2) return 3;
        return -1;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }

    public RectF getSelectedRect() {
        return rect;
    }
}
