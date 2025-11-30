package com.noteaiapp.keyboardai.mindmap;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.noteaiapp.keyboardai.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MindMapActivity extends AppCompatActivity {
    private MindMapView mindMapView;
    private String noteContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mindmap_activity);

        CharSequence contentCharSequence = getIntent().getCharSequenceExtra("notecontent");
        if (contentCharSequence != null) {
            noteContent = contentCharSequence.toString();
        }

        mindMapView = findViewById(R.id.mindMapView);
        Button btnGenerate = findViewById(R.id.btnGenerateMindMap);
        Button btnRadial = findViewById(R.id.btnRadialLayout);
        Button btnTree = findViewById(R.id.btnTreeLayout);
        Button btnHierarchical = findViewById(R.id.btnHierarchicalLayout);
        Button btnExport = findViewById(R.id.btnExport);
        mindMapView.setNote(noteContent);
        btnGenerate.setOnClickListener(v -> {
            mindMapView.setNote(noteContent);
            Toast.makeText(this, "Tap nodes to expand/collapse", Toast.LENGTH_SHORT).show();
        });

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