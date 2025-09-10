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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.Random;

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
    private boolean isSprayPaint = false;
    private boolean isRectangleMode = false; // New variable for rectangle mode
    private float defaultStrokeWidth = 10;
    private int currentColor = Color.BLACK;

    private Random random = new Random();

    // Variables for drawing shapes
    private float startX, startY, endX, endY;
    private Rect currentRect;

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

        // Draw the live path if it's not a shape or spray paint
        if (!isSprayPaint && !isRectangleMode && !mPath.isEmpty()) {
            if (isErasing) {
                Paint eraserVisualPaint = new Paint();
                eraserVisualPaint.setAntiAlias(true);
                eraserVisualPaint.setDither(true);
                eraserVisualPaint.setColor(Color.argb(100, 255, 255, 255));
                eraserVisualPaint.setStyle(Paint.Style.STROKE);
                eraserVisualPaint.setStrokeJoin(Paint.Join.ROUND);
                eraserVisualPaint.setStrokeCap(Paint.Cap.ROUND);
                eraserVisualPaint.setStrokeWidth(defaultStrokeWidth + 10);
                canvas.drawPath(mPath, eraserVisualPaint);
            } else {
                canvas.drawPath(mPath, mPaint);
            }
        }

        // Draw the live rectangle if in rectangle mode
        if (isRectangleMode && currentRect != null) {
            canvas.drawRect(currentRect, mPaint);
        }
    }

    private void touch_start(float x, float y) {
        if (isRectangleMode) {
            startX = x;
            startY = y;
            currentRect = new Rect();
        } else if (!isSprayPaint) {
            mPath.reset();
            mPath.moveTo(x, y);
        }
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        if (isRectangleMode) {
            endX = x;
            endY = y;
            // Update the live rectangle
            currentRect.set((int) Math.min(startX, endX), (int) Math.min(startY, endY), (int) Math.max(startX, endX), (int) Math.max(startY, endY));
        } else if (isSprayPaint) {
            mPaint.setStyle(Paint.Style.FILL);
            int sprayDensity = 5;
            float sprayRadius = defaultStrokeWidth * 1.5f;
            for (int i = 0; i < sprayDensity; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * sprayRadius;
                float dx = (float) (distance * Math.cos(angle));
                float dy = (float) (distance * Math.sin(angle));
                mCanvas.drawCircle(x + dx, y + dy, defaultStrokeWidth / 4, mPaint);
            }
        } else {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;
            }
        }
    }

    private void touch_up() {
        if (isRectangleMode) {
            // Draw the final rectangle onto the canvas bitmap
            if (currentRect != null) {
                mCanvas.drawRect(currentRect, mPaint);
                currentRect = null; // Clear the live rectangle
            }
        } else if (!isSprayPaint) {
            mPath.lineTo(mX, mY);
            mCanvas.drawPath(mPath, mPaint);
            mPath.reset();
        }

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
        isSprayPaint = false;
        isRectangleMode = false;
        if (isErasing) {
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            mPaint.setStrokeWidth(defaultStrokeWidth + 10);
        } else {
            mPaint.setXfermode(null);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(defaultStrokeWidth);
            mPaint.setColor(currentColor);
        }
    }

    /**
     * Sets the drawing mode to spray paint.
     */
    public void setSprayPaint(boolean sprayPaint) {
        isSprayPaint = sprayPaint;
        isErasing = false;
        isRectangleMode = false; // Turn off rectangle mode
        if (isSprayPaint) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setXfermode(null);
        } else {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setXfermode(null);
        }
    }

    /**
     * Sets the drawing mode to rectangle.
     */
    public void setRectangleMode(boolean rectangleMode) {
        this.isRectangleMode = rectangleMode;
        isSprayPaint = false; // Turn off spray paint
        isErasing = false; // Turn off erasing
        if (isRectangleMode) {
            // Set the paint style for drawing the rectangle outline
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setXfermode(null);
        } else {
            // Restore default settings if needed
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setXfermode(null);
        }
    }
}
