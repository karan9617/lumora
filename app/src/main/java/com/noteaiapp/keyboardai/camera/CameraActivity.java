package com.noteaiapp.keyboardai.camera;


import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.noteaiapp.keyboardai.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button captureButton;
    private ImageCapture imageCapture;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        Executor mainExecutor = ContextCompat.getMainExecutor(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageCapture
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Select back camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Bind lifecycle
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture
                );

            } catch (Exception e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, mainExecutor);

        captureButton.setOnClickListener(v -> takePhoto());
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(
                getOutputDirectory(),
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        );

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        Executor mainExecutor = ContextCompat.getMainExecutor(this);
        imageCapture.takePicture(
                outputOptions,
                mainExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d("CameraX", "Photo saved: " + photoFile.getAbsolutePath());
                        // 👉 You can send this photo back to Notepad.java using Intent

                        try {
                            InputImage image = InputImage.fromFilePath(CameraActivity.this,
                                    android.net.Uri.fromFile(photoFile));

                            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                            recognizer.process(image)
                                    .addOnSuccessListener(visionText -> {
                                        String resultText = visionText.getText();

                                        Intent resultIntent = new Intent();
                                        resultIntent.putExtra("ocr_text", resultText);
                                        setResult(RESULT_OK, resultIntent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("CameraX", "OCR failed", e);
                                        setResult(RESULT_CANCELED);
                                        finish();
                                    });

                        } catch (IOException e) {
                            e.printStackTrace();
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraX", "Photo capture failed: " + exception.getMessage(), exception);
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
        );
    }

    private File getOutputDirectory() {
        File mediaDir = getExternalMediaDirs()[0];
        if (mediaDir != null && mediaDir.exists())
            return mediaDir;
        else
            return getFilesDir();
    }
}