package com.noteaiapp.keyboardai.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class DrawingView extends View {
    private class TextObject {
        String text;
        float x; // Center X where text should be drawn
        float y; // Center Y where text should be drawn
        int color;
        float size;
        RectF bounds; // Used for touch detection
        float defaultSize; // To calculate scaling
    }
    private List<TextObject> textObjects = new ArrayList<>();
    private TextObject selectedText = null;
    private static final int TEXT_HIT_SLOP = 40; // Pixels for easier text selection
    private Mode currentMode = Mode.DRAW; // New mode tracking for better touch handling

    private enum Mode {
        DRAW, RECTANGLE, SPRAY, ERASER, TEXT_DRAG, TEXT_RESIZE
    }
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
    private Bitmap backgroundImage;

    private float lastTouchX;
    private float lastTouchY;
    private float originalTextSize;

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
            if (backgroundImage != null) {
                mCanvas.drawBitmap(backgroundImage, 0, 0, null);
            }
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

        // Draw all stored text objects
        Paint textPaint = new Paint();
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint selectionPaint = new Paint();
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(3f);
        selectionPaint.setColor(Color.RED);
        selectionPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 5}, 0));

        for (TextObject textObj : textObjects) {
            textPaint.setColor(textObj.color);
            textPaint.setTextSize(textObj.size);
            canvas.drawText(textObj.text, textObj.x, textObj.y, textPaint);

            // Draw selection box and handle if selected
            if (textObj == selectedText) {
                // Calculate text bounds for selection/interaction
                Rect textBounds = new Rect();
                textPaint.getTextBounds(textObj.text, 0, textObj.text.length(), textBounds);

                // Create a padded RectF for better touch area
                float left = textObj.x + textBounds.left - TEXT_HIT_SLOP;
                float top = textObj.y + textBounds.top - TEXT_HIT_SLOP;
                float right = textObj.x + textBounds.right + TEXT_HIT_SLOP;
                float bottom = textObj.y + textBounds.bottom + TEXT_HIT_SLOP;
                textObj.bounds = new RectF(left, top, right, bottom);

                // Draw the dashed selection box
                canvas.drawRect(textObj.bounds, selectionPaint);
            }
        }
    }

    private void touch_start(float x, float y) {
        lastTouchX = x;
        lastTouchY = y;
        selectedText = null; // Deselect by default

        // 1. Check for text selection
        for (TextObject textObj : textObjects) {
            if (textObj.bounds != null && textObj.bounds.contains(x, y)) {
                selectedText = textObj;
                originalTextSize = textObj.size; // Save original size for scaling
                // Set the mode based on where the touch occurred (e.g., top-right corner for resize)
                if (isNearResizeCorner(x, y, textObj.bounds)) {
                    currentMode = Mode.TEXT_RESIZE;
                } else {
                    currentMode = Mode.TEXT_DRAG;
                }
                invalidate();
                return; // Consume touch for text
            }
        }
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
        float dx = x - lastTouchX;
        float dy = y - lastTouchY;

        if (selectedText != null) {
            if (currentMode == Mode.TEXT_DRAG) {
                // Dragging: Update coordinates
                selectedText.x += dx;
                selectedText.y += dy;
            } else if (currentMode == Mode.TEXT_RESIZE) {
                // Resizing: Change size based on distance from the center
                float deltaDist = (float) Math.sqrt(dx * dx + dy * dy);

                // Get vector from center to current touch point
                float vecX = x - selectedText.x;
                float vecY = y - selectedText.y;
                float angle = (float) Math.atan2(vecY, vecX);

                // Use the horizontal movement for scaling (a simple approach)
                float scaleFactor = (x - lastTouchX) / 100f;

                // Simple scaling:
                selectedText.size = Math.max(10f, selectedText.size + scaleFactor * 50); // Ensure min size of 10

            }
            lastTouchX = x;
            lastTouchY = y;
            invalidate();
            return;
        }

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
                float dx2 = (float) (distance * Math.cos(angle));
                float dy2 = (float) (distance * Math.sin(angle));
                mCanvas.drawCircle(x + dx2, y + dy2, defaultStrokeWidth / 4, mPaint);
            }
        } else {
            float dx1 = Math.abs(x - mX);
            float dy1 = Math.abs(y - mY);
            if (dx1 >= TOUCH_TOLERANCE || dy1 >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;
            }
        }
    }

    // Method inside DrawingView
    public void addText(String text, int color, float size) {
        // Find the center of the view
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        TextObject newText = new TextObject();
        newText.text = text;
        newText.color = color;
        newText.defaultSize = size * 2;
        newText.size = newText.defaultSize; // Initial size
        newText.x = centerX;
        newText.y = centerY;

        textObjects.add(newText);
        selectedText = newText; // Select the new text for positioning
        currentMode = Mode.TEXT_DRAG; // Immediately enter drag mode

        // Notify listener since a permanent object was added
        if (mListener != null) {
            mListener.onDrawFinished();
        }

        textObjects.add(newText);
        invalidate(); // Redraw the canvas to show the new text
    }
    private boolean isNearResizeCorner(float x, float y, RectF bounds) {
        if (bounds == null) return false;

        // Define a generous corner area (e.g., 50x50 at the top right)
        return x > bounds.right - TEXT_HIT_SLOP && y < bounds.top + TEXT_HIT_SLOP;
    }

    private void touch_up() {
        if (selectedText != null) {
            // Text interaction is finished, mark as dirty
            if (mListener != null) {
                mListener.onDrawFinished();
            }
            selectedText = null; // Keep text on screen, but deselect for drawing
            currentMode = Mode.DRAW; // Reset mode
            invalidate();
            return;
        }

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
            backgroundImage = null;
            invalidate();
        }
    }

    public byte[] getDrawingData() {
        if (mBitmap == null) {
            return null;
        }

        // 1. Create a copy of the main bitmap to draw text on
        // This ensures the live drawing isn't affected until saved.
        Bitmap finalBitmap = mBitmap.copy(mBitmap.getConfig(), true);
        Canvas finalCanvas = new Canvas(finalBitmap);

        // 2. Render all TextObjects onto the final canvas
        Paint textPaint = new Paint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (TextObject textObj : textObjects) {
            textPaint.setColor(textObj.color);
            textPaint.setTextSize(textObj.size);
            finalCanvas.drawText(textObj.text, textObj.x, textObj.y, textPaint);
        }

        // 3. Compress the final bitmap
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        finalBitmap.recycle(); // Free the temporary bitmap
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
    public void setBackgroundImage(Bitmap image) {
        this.backgroundImage = image;

        // Make a new bitmap to combine the background image and existing drawing
        Bitmap combinedBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas combinedCanvas = new Canvas(combinedBitmap);

        // Calculate scaling and translation to fit the image without stretching
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float scaleFactor = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactor, scaleFactor);

        // Calculate centering offsets
        float dx = (viewWidth - imageWidth * scaleFactor) / 2f;
        float dy = (viewHeight - imageHeight * scaleFactor) / 2f;
        matrix.postTranslate(dx, dy);

        // First, draw the new background image
        combinedCanvas.drawBitmap(image, matrix, null);

        // Then, draw the existing drawing on top of it
        if (mBitmap != null) {
            combinedCanvas.drawBitmap(mBitmap, 0, 0, null);
            // Recycle the old bitmap to free up memory
            mBitmap.recycle();
        }

        // Set the new combined bitmap as the main drawing bitmap
        mBitmap = combinedBitmap;
        // The canvas also needs to be updated to point to the new bitmap
        mCanvas = new Canvas(mBitmap);
        invalidate();
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
