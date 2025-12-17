package com.noteaiapp.keyboardai.mindmap;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.noteaiapp.keyboardai.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;


public class MindMapActivity extends AppCompatActivity {
    private MindMapView mindMapView;
    private String noteContent = "";
    MaterialButton zoomIn, zoomOut;
    Chip btnHierarchical, btnTree, btnRadial;
    ImageButton btnExport,shareButton;
    private TextView hintTextView;
    private final Handler hintHandler = new Handler(Looper.getMainLooper());
    private Runnable currentHintRunnable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.mindmap_activity);

        // --- Step 1: Find all views ---
        View rootView = findViewById(android.R.id.content);
        View topToolbarCard = findViewById(R.id.top_toolbar_card);
        View layoutControls = findViewById(R.id.layout_controls_container);
        mindMapView = findViewById(R.id.mindMapView);
        hintTextView = findViewById(R.id.hint_text_view);
        btnHierarchical = findViewById(R.id.btnHierarchicalLayout);
        btnTree = findViewById(R.id.btnTreeLayout);
        btnRadial = findViewById(R.id.btnRadialLayout);
        btnExport = findViewById(R.id.btnExport);
        shareButton = findViewById(R.id.shareButton);
        zoomIn = findViewById(R.id.btn_zoom_in);
        zoomOut = findViewById(R.id.btn_zoom_out);

        // --- Step 2: Handle Edge-to-Edge insets ---
        // Your existing code for this is correct.
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            if (topToolbarCard != null) {
                topToolbarCard.setPadding(topToolbarCard.getPaddingLeft(), topInset, topToolbarCard.getPaddingRight(), topToolbarCard.getPaddingBottom());
            }
            if (layoutControls != null) {
                layoutControls.setPadding(layoutControls.getPaddingLeft(), layoutControls.getPaddingTop(), layoutControls.getPaddingRight(), bottomInset);
            }
            return insets;
        });

        // --- Step 3: Get content and set up the Mind Map ---
        CharSequence contentCharSequence = getIntent().getCharSequenceExtra("notecontent");
        if (contentCharSequence != null) {
            noteContent = contentCharSequence.toString();
        }
        mindMapView.setNote(noteContent);

        // --- Step 4: Set up ALL listeners cleanly in one place ---

        shareButton.setOnClickListener(v -> shareMindMap());

        btnExport.setOnClickListener(v -> exportMindMap());

        zoomIn.setOnClickListener(v -> {
            if (mindMapView != null) mindMapView.zoomIn();
        });

        zoomOut.setOnClickListener(v -> {
            if (mindMapView != null) mindMapView.zoomOut();
        });

        btnHierarchical.setOnClickListener(v -> {
            mindMapView.setLayoutType(MindMapView.LayoutType.HIERARCHICAL);
            showAnimatedHint("Hierarchical Layout", 2000); // Show a confirmation hint
        });

        btnTree.setOnClickListener(v -> {
            mindMapView.setLayoutType(MindMapView.LayoutType.TREE);
            showAnimatedHint("Tree Layout", 2000);
        });

        btnRadial.setOnClickListener(v -> {
            mindMapView.setLayoutType(MindMapView.LayoutType.RADIAL);
            showAnimatedHint("Radial Layout", 2000);
        });

        // --- Step 5: Show the initial instruction hint ---
        // This is the only place we should trigger a hint on a timer.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showAnimatedHint("Hint: Drag the nodes to position them.", 5000); // Stays for 5 seconds
        }, 1500); // Wait 1.5 seconds before showing the first hint

    }

    private void exportMindMap() {
        Bitmap bitmap = mindMapView.exportToImage();
        if (bitmap != null) {
            try {
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "mindmap_" + System.currentTimeMillis() + ".png");
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                Toast.makeText(this, "Saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void shareMindMap() {
        Bitmap bitmap = mindMapView.exportToImage();

        if (bitmap == null) {
            Toast.makeText(this, "Failed to generate image for sharing.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 2. Save the bitmap to a temporary cache directory.
            //    This directory is specifically for sharing files with other apps.
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs(); // Create the directory if it doesn't exist.
            FileOutputStream fos = new FileOutputStream(cachePath + "/mindmap_to_share.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            // 3. Get the URI for the saved image using the FileProvider.
            File imageFile = new File(cachePath, "mindmap_to_share.png");
            Uri contentUri = FileProvider.getUriForFile(
                    Objects.requireNonNull(getApplicationContext()),
                    "com.noteaiapp.keyboardai.fileprovider", // This MUST match the 'authorities' in your manifest.
                    imageFile
            );

            if (contentUri == null) {
                throw new IOException("Failed to get content URI.");
            }

            // 4. Create the ACTION_SEND intent to open the share sheet.
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant temporary read permission to other apps.

            // Start the chooser dialog
            startActivity(Intent.createChooser(shareIntent, "Share Mind Map via..."));

        } catch (IOException e) {
            Log.e("MindMapShare", "Error sharing mind map", e);
            Toast.makeText(this, "Sharing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Displays a hint message with a fade-in and fade-out animation.
     * If another hint is already scheduled, it will be replaced.
     * @param message The text to display in the hint.
     * @param displayDuration The time in milliseconds for the hint to stay visible.
     */
// In MindMapActivity.java

// DELETE your old showAnimatedHint method.
// REPLACE it with this new version.

    private void showAnimatedHint(String message, long displayDuration) {
        // If there's an old hint animation running, cancel it
        if (currentHintRunnable != null) {
            hintHandler.removeCallbacks(currentHintRunnable);
            // Also cancel any ongoing animation immediately
            hintTextView.animate().cancel();
        }

        // --- THIS IS THE FIX ---
        // 1. Set the text and make the view visible BUT with alpha 0 (still invisible).
        // This ensures it's part of the layout but transparent.
        hintTextView.setText(message);
        hintTextView.setAlpha(0f);
        hintTextView.setVisibility(View.VISIBLE);

        // 2. Use ViewPropertyAnimator to fade in.
        // This is the modern, recommended way to do simple view animations.
        hintTextView.animate()
                .alpha(1f) // Fade IN to fully opaque
                .setDuration(500) // 0.5 seconds
                .setListener(null); // Clear any previous listeners

        // 3. The runnable will now just handle the fade-out logic.
        currentHintRunnable = () -> {
            hintTextView.animate()
                    .alpha(0f) // Fade OUT to fully transparent
                    .setDuration(500)
                    .withEndAction(() -> {
                        // This runs after the fade-out animation is complete.
                        hintTextView.setVisibility(View.GONE);
                    });
        };

        // 4. Schedule the fade-out runnable to execute after the desired display time.
        hintHandler.postDelayed(currentHintRunnable, displayDuration);
    }

}