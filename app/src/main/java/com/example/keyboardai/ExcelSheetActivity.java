package com.example.keyboardai;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.keyboardai.adapter.DataCellAdapter;
import com.example.keyboardai.adapter.HeaderAdapter;
import java.util.ArrayList;
import java.util.List;

public class ExcelSheetActivity extends AppCompatActivity {

    private RecyclerView rowHeaderRecyclerView;
    private RecyclerView colHeaderRecyclerView;
    private RecyclerView dataRecyclerView;
    private DataCellAdapter dataAdapter;
    private HeaderAdapter rowHeaderAdapter;
    private HeaderAdapter colHeaderAdapter;
    private List<List<String>> data;
    private final int numColumns = 10;

    // Use these dimensions to control the size of all cells.
    // They are no longer hardcoded in XML.
    private final int cellWidth = 200; // in pixels
    private final int cellHeight = 100; // in pixels

    // Flags to track user-initiated scrolling and prevent infinite loops.
    private boolean isHorizontalScrollUserInitiated = false;
    private boolean isVerticalScrollUserInitiated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel_sheet);

        // Initialize views
        rowHeaderRecyclerView = findViewById(R.id.row_header_recycler_view);
        colHeaderRecyclerView = findViewById(R.id.col_header_recycler_view);
        dataRecyclerView = findViewById(R.id.data_recycler_view);
        Button addRowButton = findViewById(R.id.add_row_button);

        // Initialize data source
        data = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            addNewRow();
        }

        // Set up the row header (1, 2, 3...) RecyclerView
        rowHeaderRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        // Pass the height to the adapter for sizing.
        rowHeaderAdapter = new HeaderAdapter(data.size(), HeaderAdapter.TYPE_ROW, cellWidth, cellHeight);
        rowHeaderRecyclerView.setAdapter(rowHeaderAdapter);

        // Set up the column header (A, B, C...) RecyclerView
        colHeaderRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // Pass the width to the adapter for sizing.
        colHeaderAdapter = new HeaderAdapter(numColumns, HeaderAdapter.TYPE_COLUMN, cellWidth, cellHeight);
        colHeaderRecyclerView.setAdapter(colHeaderAdapter);

        // Set up the main data grid RecyclerView
        GridLayoutManager dataLayoutManager = new GridLayoutManager(this, numColumns);
        dataRecyclerView.setLayoutManager(dataLayoutManager);
        // Pass both width and height to the adapter for sizing.
        dataAdapter = new DataCellAdapter(this, data, numColumns, cellWidth, cellHeight);
        dataRecyclerView.setAdapter(dataAdapter);

        // ---------------------------------------------------------------------------------------------------
        // Critical step: Implement fully bidirectional scrolling synchronization
        // ---------------------------------------------------------------------------------------------------

        // Listen for scroll events on the main data grid and tell the headers to follow.
        dataRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isHorizontalScrollUserInitiated = true;
                    isVerticalScrollUserInitiated = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isHorizontalScrollUserInitiated = false;
                    isVerticalScrollUserInitiated = false;
                }
            }
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (isHorizontalScrollUserInitiated) {
                    colHeaderRecyclerView.scrollBy(dx, 0);
                }
                if (isVerticalScrollUserInitiated) {
                    rowHeaderRecyclerView.scrollBy(0, dy);
                }
            }
        });

        // Listen for horizontal scroll events on the column header and tell the data grid to follow.
        colHeaderRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isHorizontalScrollUserInitiated = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isHorizontalScrollUserInitiated = false;
                }
            }
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (isHorizontalScrollUserInitiated) {
                    dataRecyclerView.scrollBy(dx, 0);
                }
            }
        });

        // Listen for vertical scroll events on the row header and tell the data grid to follow.
        rowHeaderRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isVerticalScrollUserInitiated = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isVerticalScrollUserInitiated = false;
                }
            }
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (isVerticalScrollUserInitiated) {
                    dataRecyclerView.scrollBy(0, dy);
                }
            }
        });

        // Set up the "Add Row" button listener
        addRowButton.setOnClickListener(v -> {
            addNewRow();
            // Notify all adapters of the change
            rowHeaderAdapter.updateRowCount(data.size());
            dataAdapter.notifyItemRangeInserted((data.size() - 1) * numColumns, numColumns);
            Toast.makeText(this, "New row added!", Toast.LENGTH_SHORT).show();
        });
    }

    private void addNewRow() {
        List<String> newRow = new ArrayList<>();
        for (int i = 0; i < numColumns; i++) {
            newRow.add("");
        }
        data.add(newRow);
    }
}
