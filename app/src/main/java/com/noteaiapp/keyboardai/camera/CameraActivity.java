package com.noteaiapp.keyboardai.camera;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.noteaiapp.keyboardai.R;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button captureButton;
    private ResizableOverlayView overlayView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private FirebaseStorage storage; // <-- ADD THIS

    private static final int CAMERA_PERMISSION_CODE = 101;
    private ProgressBar processingProgressBar;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.camera_activity);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        processingProgressBar = findViewById(R.id.processingProgressBar); // <-- ADD THIS
        overlayView = findViewById(R.id.overlayView);
        cameraExecutor = Executors.newSingleThreadExecutor();
        storage = FirebaseStorage.getInstance();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            startCamera();
        }

        captureButton.setOnClickListener(v -> captureAndProcessImage());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e("CameraX", "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureAndProcessImage() {
        if (imageCapture == null) return;

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();

                runOnUiThread(() -> {
                    if (bitmap != null) {
                        processingProgressBar.setVisibility(View.VISIBLE);
                        // Crop to selected rectangle
                        RectF rect = overlayView.getSelectedRect();
                        Bitmap cropped = cropBitmap(bitmap, rect, previewView.getWidth(), previewView.getHeight());
                        uploadImageAndRunOcr(cropped);
                        //runTextRecognition(cropped);
                    } else {
                        Toast.makeText(CameraActivity.this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraX", "Capture failed", exception);
                Toast.makeText(CameraActivity.this, "Capture failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // In CameraActivity.java

    // 1. CREATE THIS NEW METHOD TO HANDLE THE UPLOAD
    private void uploadImageAndRunOcr(Bitmap bitmap) {
        if (bitmap == null) return;

        Toast.makeText(this, "Uploading image, please keep your hands still.", Toast.LENGTH_SHORT).show();

        // Prepare the bitmap for upload
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();
        String filename = UUID.randomUUID().toString() + ".jpg";
        String userId = currentUser.getUid();
        // Create a unique path in Firebase Storage
        StorageReference imageRef = storage.getReference().child("images/" + userId + "/" + filename);

        // Start the upload
        imageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    // Upload successful, now get the permanent download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String imageUrl = downloadUri.toString();
                        Log.d("CameraActivity", "Image uploaded successfully. URL: " + imageUrl);

                        // NOW that we have the URL, we can run OCR and return both results.
                        runTextRecognition(bitmap, imageUrl);

                    }).addOnFailureListener(e -> {
                        Log.e("CameraActivity", "Failed to get download URL", e);
                        processingProgressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to get image URL.", Toast.LENGTH_SHORT).show();
                        finish(); // Finish with a failure
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("CameraActivity", "Image upload failed", e);
                    processingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                    finish(); // Finish with a failure
                });
    }

    // 2. MODIFY runTextRecognition TO ACCEPT THE IMAGE URL
    private void runTextRecognition(Bitmap bitmap, String imageUrl) { // <-- Pass the imageUrl
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage image = InputImage.fromBitmap(bitmap, 0); // Your selection is here, this is correct.

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String resultText = visionText.getText();
                    Log.d("OCR_RESULT", "Detected text: " + resultText);

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("ocr_text", resultText);

                    // --- START: THIS IS THE FIX ---
                    // Now you have the permanent cloud URL to return as the "image_path"
                    resultIntent.putExtra("image_path", imageUrl);
                    // --- END: THIS IS THE FIX ---

                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("OCR", "Failed to read text", e);
                    // Even if OCR fails, we can still return the image URL
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("ocr_text", ""); // Return empty text
                    resultIntent.putExtra("image_path", imageUrl); // Still return the image
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
    }
/*
    private void runTextRecognition(Bitmap bitmap) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String resultText = visionText.getText();
                    Log.d("OCR_RESULT", "Detected text: " + resultText);  // <-- ADD THIS
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("ocr_text", resultText);
                    resultIntent.putExtra("image_path", imagePath); // <-- YOU MUST ADD THIS
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("OCR", "Failed to read text", e);
                    Toast.makeText(CameraActivity.this, "Text recognition failed", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                });
    }*/

    private Bitmap cropBitmap(Bitmap source, RectF overlayRect, int previewWidth, int previewHeight) {
        // Scale overlay rect to bitmap size
        float scaleX = (float) source.getWidth() / previewWidth;
        float scaleY = (float) source.getHeight() / previewHeight;

        int left = (int) (overlayRect.left * scaleX);
        int top = (int) (overlayRect.top * scaleY);
        int width = (int) (overlayRect.width() * scaleX);
        int height = (int) (overlayRect.height() * scaleY);

        left = Math.max(0, left);
        top = Math.max(0, top);
        width = Math.min(source.getWidth() - left, width);
        height = Math.min(source.getHeight() - top, height);

        return Bitmap.createBitmap(source, left, top, width, height);
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        Bitmap bitmap = null;

        if (image.getFormat() == android.graphics.ImageFormat.YUV_420_888) {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                    nv21, android.graphics.ImageFormat.NV21,
                    image.getWidth(), image.getHeight(), null
            );

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
            byte[] jpegBytes = out.toByteArray();
            bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
        } else if (image.getFormat() == android.graphics.ImageFormat.JPEG) {
            // ✅ Directly decode JPEG buffer
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

        // Rotate the bitmap if needed
        if (bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(image.getImageInfo().getRotationDegrees());
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
