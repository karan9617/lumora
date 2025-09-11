package com.example.keyboardai.processor;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ImageSaver {

    private static final String TAG = "ImageSaver";
    private final Context context;

    public ImageSaver(Context context) {
        this.context = context;
    }
    public String saveImage(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null. Cannot save image.");
            return null;
        }

        // Generate a unique file name using UUID to prevent collisions.
        String fileName = "image_" + UUID.randomUUID().toString() + ".png";
        File file = new File(context.getCacheDir(), fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Compress the bitmap into a PNG file. You can also use JPEG with a quality parameter.
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush(); // Ensure all data is written to the file.
            Log.i(TAG, "Image saved successfully at: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error saving image: " + e.getMessage(), e);
            return null;
        }
    }
}

