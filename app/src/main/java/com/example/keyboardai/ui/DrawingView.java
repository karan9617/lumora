package com.example.keyboardai.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;

public class DrawingView extends View {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint mPaint;
    private static final float TOUCH_TOLERANCE = 4;
    private float mX, mY;

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(10);
    }
    /**
     * Gets the current drawing as a Bitmap.
     * @return A Bitmap of the drawing canvas.
     */
    public Bitmap getDrawingBitmap() {
        if (mBitmap != null) {
            return mBitmap;
        }
        return null;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // **CRITICAL FIX**: Only create the bitmap if dimensions are valid.
        if (w > 0 && h > 0) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }
    }
    public void setColor(int color) {
        if (mPaint != null) {
            mPaint.setColor(color);
        }
    }
    public void setStrokeWidth(float width) {
        if (mPaint != null) {
            mPaint.setStrokeWidth(width);
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        }
        canvas.drawPath(mPath, mPaint);
    }

    // ... (rest of your touch_start, touch_move, touch_up methods) ...
    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touch_up() {
        mPath.lineTo(mX, mY);
        mCanvas.drawPath(mPath, mPaint);
        mPath.reset();
    }
    public void setDrawingBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            // Create a mutable copy of the bitmap so we can draw on it.
            mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            // Set the canvas to draw on this new bitmap
            if (mCanvas == null) {
                mCanvas = new Canvas(mBitmap);
            } else {
                mCanvas.setBitmap(mBitmap);
            }
        }

        // Reset the path so old lines aren't drawn over the new bitmap
        mPath.reset();

        invalidate(); // Redraw the view with the new bitmap
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mBitmap == null) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }

    public void clearDrawing() {
        if (mBitmap != null) {
            mBitmap.eraseColor(Color.TRANSPARENT);
            invalidate();
        }
    }

    public byte[] getDrawingData() {
        if (mBitmap == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public void setDrawingData(byte[] data) {
        if (data == null || data.length == 0) {
            clearDrawing();
            return;
        }
        Bitmap loadedBitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length);
        if (loadedBitmap != null) {
            mBitmap = loadedBitmap.copy(Bitmap.Config.ARGB_8888, true); // Create a mutable copy
            if (mCanvas == null) {
                mCanvas = new Canvas(mBitmap);
            } else {
                mCanvas.setBitmap(mBitmap);
            }
        }
        invalidate();
    }
}