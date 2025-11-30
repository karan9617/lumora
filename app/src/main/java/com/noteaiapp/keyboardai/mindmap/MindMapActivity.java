package com.noteaiapp.keyboardai.mindmap;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.noteaiapp.keyboardai.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MindMapActivity extends AppCompatActivity {
    private MindMapView mindMapView;
    private String noteContent = "";
    MaterialButton zoomIn, zoomOut;
    Chip btnHierarchical, btnTree, btnRadial;
    ImageButton btnExport;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.mindmap_activity);


        View rootView = findViewById(android.R.id.content);
        View topToolbarCard = findViewById(R.id.top_toolbar_card); // Assuming you give your MaterialCardView this ID
        View layoutControls = findViewById(R.id.layout_controls_container); // Give the bottom LinearLayout this ID
        zoomIn = findViewById(R.id.btn_zoom_in);
        zoomOut = findViewById(R.id.btn_zoom_out);
        // 2. Set the OnApplyWindowInsetsListener on the root view of your layout.
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            // Get the insets for the system bars (status bar at top, navigation bar at bottom)
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int leftInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
            int rightInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;

            // 3. Apply the insets as padding or margins to your UI components.

            // Apply top padding to your floating toolbar card to push it down
            if (topToolbarCard != null) {
                topToolbarCard.setPadding(topToolbarCard.getPaddingLeft(), topInset, topToolbarCard.getPaddingRight(), topToolbarCard.getPaddingBottom());
            }

            // Apply bottom padding to your layout controls container to push it up
            if (layoutControls != null) {
                layoutControls.setPadding(layoutControls.getPaddingLeft(), layoutControls.getPaddingTop(), layoutControls.getPaddingRight(), bottomInset);
            }

            // Return the insets to allow the system to continue processing them.
            return insets;
        });

        CharSequence contentCharSequence = getIntent().getCharSequenceExtra("notecontent");
        if (contentCharSequence != null) {
            noteContent = contentCharSequence.toString();
        }
        Log.d("com.noteaiapp.keyboardai","noteContent:"+noteContent);

        mindMapView = findViewById(R.id.mindMapView);
        btnRadial = findViewById(R.id.btnRadialLayout);
         btnTree = findViewById(R.id.btnTreeLayout);
         btnHierarchical = findViewById(R.id.btnHierarchicalLayout);
         btnExport = findViewById(R.id.btnExport);
        mindMapView.setNote(noteContent);


        btnRadial.setOnClickListener(v -> {
            mindMapView.setLayoutType(MindMapView.LayoutType.RADIAL);
        });

        btnTree.setOnClickListener(v -> {
            mindMapView.setLayoutType(MindMapView.LayoutType.TREE);
        });

        btnHierarchical.setOnClickListener(v -> {
            mindMapView.setLayoutType(MindMapView.LayoutType.HIERARCHICAL);
        });

        btnExport.setOnClickListener(v -> {
            exportMindMap();
        });
        zoomIn.setOnClickListener(v -> {
            // Tell the MindMapView to zoom in
            if (mindMapView != null) {
                mindMapView.zoomIn();
            }
        });

        zoomOut.setOnClickListener(v -> {
            // Tell the MindMapView to zoom out
            if (mindMapView != null) {
                mindMapView.zoomOut();
            }
        });
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
}