package com.example.keyboardai.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

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
    private OnDrawListener mListener;
    private boolean isErasing = false;
    private float defaultStrokeWidth = 10;
    private int currentColor = Color.BLACK;

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    public interface OnDrawListener {
        void onDrawFinished();
    }

    public void setOnDrawListener(OnDrawListener listener) {
        this.mListener = listener;
    }

    private void setupDrawing() {
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(currentColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(defaultStrokeWidth);
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

        if (w > 0 && h > 0) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }
    }

    public void setColor(int color) {
        this.currentColor = color;
        mPaint.setColor(currentColor);
    }

    public void setStrokeWidth(float width) {
        if (mPaint != null) {
            mPaint.setStrokeWidth(width);
            defaultStrokeWidth = width;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        }

        // Draw the live path with appropriate visual feedback
        if (!mPath.isEmpty()) {
            if (isErasing) {
                // Use a temporary paint for visual feedback while erasing
                Paint eraserVisualPaint = new Paint();
                eraserVisualPaint.setAntiAlias(true);
                eraserVisualPaint.setDither(true);
                eraserVisualPaint.setColor(Color.argb(100, 255, 255, 255)); // Semi-transparent white
                eraserVisualPaint.setStyle(Paint.Style.STROKE);
                eraserVisualPaint.setStrokeJoin(Paint.Join.ROUND);
                eraserVisualPaint.setStrokeCap(Paint.Cap.ROUND);
                eraserVisualPaint.setStrokeWidth(defaultStrokeWidth + 10);
                canvas.drawPath(mPath, eraserVisualPaint);
            } else {
                canvas.drawPath(mPath, mPaint);
            }
        }
    }

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
        if (mListener != null) {
            mListener.onDrawFinished();
        }
    }

    public void setDrawingBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            if (mCanvas == null) {
                mCanvas = new Canvas(mBitmap);
            } else {
                mCanvas.setBitmap(mBitmap);
            }
        }
        mPath.reset();
        invalidate();
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
            mBitmap = loadedBitmap.copy(Bitmap.Config.ARGB_8888, true);
            if (mCanvas == null) {
                mCanvas = new Canvas(mBitmap);
            } else {
                mCanvas.setBitmap(mBitmap);
            }
            mPath.reset();
        }
        invalidate();
    }

    public void loadDrawingFromBytes(byte[] data) {
        if (data != null && data.length > 0) {
            Bitmap loadedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (loadedBitmap != null) {
                this.mBitmap = loadedBitmap.copy(loadedBitmap.getConfig(), true);
                this.mCanvas = new Canvas(this.mBitmap);
                this.mPath.reset();
                invalidate();
            }
        }
    }

    public void setErasing(boolean erasing) {
        isErasing = erasing;
        if (isErasing) {
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            mPaint.setStrokeWidth(defaultStrokeWidth + 10);
        } else {
            mPaint.setXfermode(null);
            mPaint.setStrokeWidth(defaultStrokeWidth);
            mPaint.setColor(currentColor);
        }
    }
}
