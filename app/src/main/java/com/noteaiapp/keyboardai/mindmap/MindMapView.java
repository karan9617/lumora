package com.noteaiapp.keyboardai.mindmap;

// MindMapView.java - Advanced custom view for displaying interactive mindmaps
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.ArrayList;
import java.util.List;


public class MindMapView extends View {

    public enum LayoutType {
        RADIAL,      // Circular layout
        TREE,        // Vertical tree
        HIERARCHICAL // Horizontal tree
    }

    public static class Node {
        String text;
        float x;
        float y;
        List<Node> children;
        int level;
        boolean isCollapsed;
        Node parent;
        RectF bounds;

        public Node(String text) {
            this.text = text;
            this.children = new ArrayList<>();
            this.level = 0;
            this.isCollapsed = false;
            this.bounds = new RectF();
        }
    }

    private Node rootNode;
    private Paint paint;
    private Paint textPaint;
    private Paint linePaint;
    private Paint collapsedIndicatorPaint;
    private int[] nodeColors;
    private boolean isPanning = false;
    // Spacing configuration
    private static final float NODE_MARGIN = 120f; // Margin around each node
    private static final float MIN_NODE_WIDTH = 250f; // Minimum node width estimate
    private static final float MIN_NODE_HEIGHT = 150f; // Minimum node height estimate


    // Zoom and pan variables
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private float scaleFactor = 1.0f;
    private float focusX = 0f;
    private float focusY = 0f;
    private float translateX = 0f;
    private float translateY = 0f;

    // Drag variables
    private Node draggedNode = null;
    private float dragStartX = 0f;
    private float dragStartY = 0f;
    private float nodeDragOffsetX = 0f;
    private float nodeDragOffsetY = 0f;
    private boolean isDraggingNode = false;

    // Gesture detectors
    private ScaleGestureDetector scaleDetector;
    private Paint highlightPaint;   // A new paint for drawing the highlight
    private Node selectedNode = null; // To keep track of the currently selected node
    private GestureDetector gestureDetector;

    // Layout type
    private LayoutType currentLayout = LayoutType.RADIAL;

    public MindMapView(Context context) {
        super(context);
        init(context);
    }

    public MindMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);

        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setStyle(Paint.Style.STROKE); // To draw a border
        highlightPaint.setColor(Color.parseColor("#007AFF")); // A nice blue highlight color
        highlightPaint.setStrokeWidth(8f); // A thick, visible border

        collapsedIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        collapsedIndicatorPaint.setColor(Color.WHITE);
        collapsedIndicatorPaint.setTextSize(50f);
        collapsedIndicatorPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        nodeColors = new int[]{
                Color.parseColor("#4CAF50"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#F44336"),
                Color.parseColor("#009688"),
                Color.parseColor("#FF5722")
        };

        // Initialize gesture detectors
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void setNote(String noteText) {
        rootNode = parseNoteToMindMap(noteText);
        resetViewport();
        calculatePositions();
        invalidate();
    }

    public void setLayoutType(LayoutType layoutType) {
        this.currentLayout = layoutType;
        calculatePositions();
        invalidate();
    }

    private void resetViewport() {
        scaleFactor = 1.0f;
        translateX = 0f;
        translateY = 0f;
        matrix.reset();
    }

    private Node parseNoteToMindMap(String noteText) {
        if (noteText == null || noteText.trim().isEmpty()) {
            return new Node("Empty Note");
        }

        String[] lines = noteText.split("\n");
        List<String> nonEmptyLines = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().length() > 0) {
                nonEmptyLines.add(rtrim(line));
            }
        }

        if (nonEmptyLines.isEmpty()) {
            return new Node("Empty Note");
        }

        Node root = new Node(nonEmptyLines.get(0).trim());
        root.level = 0;

        List<Node> stack = new ArrayList<>();
        stack.add(root);

        for (int i = 1; i < nonEmptyLines.size(); i++) {
            String line = nonEmptyLines.get(i);
            int indentLevel = getIndentLevel1(line);
            String nodeText = line.trim();

            Node newNode = new Node(nodeText);
            newNode.level = indentLevel;

            while (stack.size() > indentLevel) {
                stack.remove(stack.size() - 1);
            }

            if (!stack.isEmpty()) {
                Node parent = stack.get(stack.size() - 1);
                newNode.parent = parent;
                parent.children.add(newNode);
            } else {
                newNode.parent = root;
                root.children.add(newNode);
            }

            stack.add(newNode);
        }

        return root;
    }

    private int getIndentLevel1(String line) {
        int spaces = 0;
        while (spaces < line.length() && line.charAt(spaces) == ' ') {
            spaces++;
        }
        return (spaces + 1) / 2;
    }

    private String rtrim(String s) {
        int i = s.length() - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) {
            i--;
        }
        return s.substring(0, i + 1);
    }

    private void calculatePositions() {
        if (rootNode == null) return;

        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) {
            width = 1080;
            height = 1920;
        }

        switch (currentLayout) {
            case RADIAL:
                calculateRadialLayout(width, height);
                break;
            case TREE:
                calculateTreeLayout(width, height);
                break;
            case HIERARCHICAL:
                calculateHierarchicalLayout(width, height);
                break;
        }
    }
    public void zoomIn() {
        // Zoom in by a fixed factor, e.g., 20%
        float newScale = scaleFactor * 1.2f;
        // Apply the new scale, respecting the maximum zoom limit
        scaleFactor = Math.min(newScale, 3.0f); // 3.0f is the max zoom from your ScaleListener
        invalidate(); // Redraw the view with the new scale
    }
    /**
     * Recursively calculates the total bounding box that contains all nodes in the mind map.
     * @param node The current node to process.
     * @param bounds The RectF object to accumulate the total bounds into.
     */
    private void calculateTotalBounds(Node node, RectF bounds) {
        if (node == null) return;

        // Union the current node's bounds with the total bounds
        bounds.union(node.bounds);

        // Recurse through children if not collapsed
        if (!node.isCollapsed) {
            for (Node child : node.children) {
                calculateTotalBounds(child, bounds);
            }
        }
    }
    public void zoomOut() {
        // Zoom out by a fixed factor
        float newScale = scaleFactor / 1.2f;
        // Apply the new scale, respecting the minimum zoom limit
        scaleFactor = Math.max(newScale, 0.2f); // 0.2f is the min zoom from your ScaleListener
        invalidate(); // Redraw the view with the new scale
    }
    private void calculateRadialLayout(int width, int height) {
        rootNode.x = width / 2f;
        rootNode.y = height / 2f;

        positionChildrenRadial(rootNode, Math.min(width, height) * 0.25f, 0, 360);
    }

    private void positionChildrenRadial(Node parent, float radius, float startAngle, float endAngle) {
        if (parent.isCollapsed || parent.children.isEmpty()) return;

        int visibleChildren = parent.children.size();

        // Calculate minimum radius needed to prevent overlap'


        float nodeMargin = 80f; // Margin around each node
        float minNodeSize = 250f + nodeMargin; // Approximate node width/height + margin
        float circumference = 2 * (float)Math.PI * radius;
        float neededCircumference = visibleChildren * minNodeSize * 1.5f; // 1.5x for spacing

        // Increase radius if nodes would overlap
        if (neededCircumference > circumference) {
            radius = neededCircumference / (2 * (float)Math.PI);
        }

        float angleStep = (endAngle - startAngle) / visibleChildren;

        // Ensure minimum angle step to prevent overlap
        float minAngleStep = 15f; // Minimum degrees between nodes
        if (angleStep < minAngleStep && visibleChildren > 1) {
            angleStep = minAngleStep;
        }

        for (int i = 0; i < visibleChildren; i++) {
            Node child = parent.children.get(i);
            float angle = startAngle + angleStep * i + angleStep / 2;
            double angleRad = Math.toRadians(angle - 90);

            child.x = parent.x + (float)(Math.cos(angleRad) * radius);
            child.y = parent.y + (float)(Math.sin(angleRad) * radius);

            if (!child.children.isEmpty()) {
                float childStartAngle = angle - angleStep / 2;
                float childEndAngle = angle + angleStep / 2;

                // Increase radius for each level to spread out more
                float childRadius = radius * 0.85f;

                // Ensure minimum radius for child level
                float minChildRadius = 200f + nodeMargin;
                if (childRadius < minChildRadius) {
                    childRadius = minChildRadius;
                }

                positionChildrenRadial(child, childRadius, childStartAngle, childEndAngle);
            }
        }
    }

    private void calculateTreeLayout(int width, int height) {
        rootNode.x = width / 2f;
        rootNode.y = 150f;

        positionChildrenTree(rootNode, width / 2f, 150f, width * 1.5f, 300f);
    }

    private void positionChildrenTree(Node parent, float centerX, float y, float availableWidth, float verticalSpacing) {
        if (parent.isCollapsed || parent.children.isEmpty()) return;

        int childCount = parent.children.size();

        // Calculate minimum width needed for all children
        float nodeMargin = 80f; // Margin around each node
        float minNodeWidth = 220f + nodeMargin; // Approximate node width with padding + margin
        float totalMinWidth = childCount * minNodeWidth * 1.3f; // 1.3x for spacing

        // Expand available width if needed
        if (totalMinWidth > availableWidth) {
            availableWidth = totalMinWidth;
        }

        float spacing = availableWidth / (childCount + 1);

        // Ensure minimum spacing between nodes
        float minSpacing = 240f + nodeMargin;
        if (spacing < minSpacing) {
            spacing = minSpacing;
            availableWidth = spacing * (childCount + 1);
        }

        float startX = centerX - (availableWidth / 2) + spacing;

        for (int i = 0; i < childCount; i++) {
            Node child = parent.children.get(i);
            child.x = startX + i * spacing;
            child.y = y + verticalSpacing;

            if (!child.children.isEmpty()) {
                // Calculate width needed for this subtree
                int grandchildCount = countVisibleChildren(child);
                float subtreeWidth = spacing * Math.max(1.5f, grandchildCount * 0.8f);

                positionChildrenTree(child, child.x, child.y, subtreeWidth, verticalSpacing);
            }
        }
    }

    private void calculateHierarchicalLayout(int width, int height) {
        rootNode.x = 200f;
        rootNode.y = height / 2f;

        positionChildrenHierarchical(rootNode, 200f, height / 2f, 300f);
    }

    private void positionChildrenHierarchical(Node parent, float x, float centerY, float horizontalSpacing) {
        if (parent.isCollapsed || parent.children.isEmpty()) return;

        int childCount = parent.children.size();

        // Calculate minimum vertical spacing needed
        float nodeMargin = 80f; // Margin around each node
        float minNodeHeight = 120f + nodeMargin; // Approximate node height with padding + margin
        float totalMinHeight = childCount * minNodeHeight * 1.5f; // 1.5x for spacing

        float verticalSpacing = 180f + nodeMargin; // Base spacing between nodes + margin
        float totalHeight = childCount * verticalSpacing;

        // Expand spacing if nodes would overlap
        if (totalMinHeight > totalHeight) {
            verticalSpacing = totalMinHeight / childCount;
            totalHeight = totalMinHeight;
        }

        float startY = centerY - totalHeight / 2;

        for (int i = 0; i < childCount; i++) {
            Node child = parent.children.get(i);
            child.x = x + horizontalSpacing;
            child.y = startY + i * verticalSpacing + verticalSpacing / 2;

            if (!child.children.isEmpty()) {
                // Calculate vertical space needed for this subtree
                int subtreeSize = countVisibleChildren(child);
                float subtreeHeight = Math.max(verticalSpacing, subtreeSize * minNodeHeight);

                positionChildrenHierarchical(child, child.x, child.y, horizontalSpacing);
            }
        }
    }
    private int countVisibleChildren(Node node) {
        if (node.isCollapsed || node.children.isEmpty()) return 0;

        int count = node.children.size();
        for (Node child : node.children) {
            count += countVisibleChildren(child);
        }
        return count;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculatePositions();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (rootNode == null) return;

        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        drawConnections(canvas, rootNode);
        drawNodeRecursive(canvas, rootNode);

        canvas.restore();
    }

    private void drawConnections(Canvas canvas, Node node) {
        if (node.isCollapsed) return;

        for (Node child : node.children) {
            linePaint.setColor(nodeColors[child.level % nodeColors.length]);

            Path path = new Path();
            path.moveTo(node.x, node.y);

            float ctrlX1 = node.x + (child.x - node.x) * 0.5f;
            float ctrlY1 = node.y;
            float ctrlX2 = node.x + (child.x - node.x) * 0.5f;
            float ctrlY2 = child.y;

            path.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, child.x, child.y);
            canvas.drawPath(path, linePaint);

            drawConnections(canvas, child);
        }
    }

    private void drawNodeRecursive(Canvas canvas, Node node) {
        drawNode(canvas, node);

        if (!node.isCollapsed) {
            for (Node child : node.children) {
                drawNodeRecursive(canvas, child);
            }
        }
    }

    private void drawNode(Canvas canvas, Node node) {
        String displayText = node.text;

        List<String> lines = wrapText(displayText, 300f);

        float maxLineWidth = 0;
        for (String line : lines) {
            float lineWidth = textPaint.measureText(line);
            if (lineWidth > maxLineWidth) maxLineWidth = lineWidth;
        }

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float lineHeight = fm.descent - fm.ascent + 10f;
        float totalTextHeight = lineHeight * lines.size();

        float padding = 40f;
        float rectWidth = Math.max(maxLineWidth + padding * 2, 200f);
        float rectHeight = totalTextHeight + padding * 2;

        node.bounds.set(
                node.x - rectWidth / 2,
                node.y - rectHeight / 2,
                node.x + rectWidth / 2,
                node.y + rectHeight / 2
        );

        paint.setColor(nodeColors[node.level % nodeColors.length]);
        canvas.drawRoundRect(node.bounds, 25f, 25f, paint);

        if (node == selectedNode) {
            // 2. If it is, draw the highlight border on top of the regular node.
            // We can slightly inset it for a better look.
            canvas.drawRoundRect(node.bounds, 25f, 25f, highlightPaint);
        }

        float textStartY = node.y - (totalTextHeight / 2) - fm.ascent;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float lineWidth = textPaint.measureText(line);
            float textX = node.x - lineWidth / 2;
            float textY = textStartY + i * lineHeight;
            canvas.drawText(line, textX, textY, textPaint);
        }

        if (!node.children.isEmpty()) {
            String indicator = node.isCollapsed ? "+" : "−";
            float indicatorX = node.bounds.right - 50f;
            float indicatorY = node.bounds.bottom - 25f;
            canvas.drawText(indicator, indicatorX, indicatorY, collapsedIndicatorPaint);
        }
    }

    private List<String> wrapText(String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");

        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float testWidth = textPaint.measureText(testLine);

            if (testWidth > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        if (lines.size() > 4) {
            lines = lines.subList(0, 4);
            String lastLine = lines.get(3);
            if (lastLine.length() > 3) {
                lines.set(3, lastLine.substring(0, Math.min(lastLine.length(), 30)) + "...");
            }
        }

        return lines;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(event);
                isPanning = false;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                isDraggingNode = false;
                draggedNode = null;
                isPanning = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDraggingNode && draggedNode != null) {
                    // If we are in node-drag mode, handle it manually.
                    handleNodeDrag(event);
                } else {
                    // Otherwise, let the gesture detector handle the event.
                    // This will correctly trigger onScroll for panning.
                    gestureDetector.onTouchEvent(event);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (!isDraggingNode && !isPanning) {
                    handleNodeClick(event);
                }
                isDraggingNode = false;
                draggedNode = null;
                isPanning = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                isDraggingNode = false;
                draggedNode = null;
                isPanning = false;
                break;
        }

        return true;
    }

    private void handleTouchDown(MotionEvent event) {
        if (rootNode == null) return;

        // We only need to find out if a node was touched.
        // The GestureDetector will handle the starting coordinates for scrolling.

        float touchX = event.getX();
        float touchY = event.getY();

        // Convert to canvas coordinates to find the node under the touch point
        float canvasX = (touchX - translateX) / scaleFactor;
        float canvasY = (touchY - translateY) / scaleFactor;

        Node touchedNode = findNodeAt(rootNode, canvasX, canvasY);

        if (touchedNode != null) {
            // Prepare for a potential node drag. This is correct.
            draggedNode = touchedNode;
            nodeDragOffsetX = touchedNode.x - canvasX;
            nodeDragOffsetY = touchedNode.y - canvasY;
            isDraggingNode = true;
        } else {
            // If no node is touched, do nothing here. The onScroll will handle panning.
            isDraggingNode = false;
            draggedNode = null;
        }
    }

    private void handleNodeDrag(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        float canvasX = (touchX - translateX) / scaleFactor;
        float canvasY = (touchY - translateY) / scaleFactor;

        draggedNode.x = canvasX + nodeDragOffsetX;
        draggedNode.y = canvasY + nodeDragOffsetY;

        invalidate();
    }

    private void handleNodeClick(MotionEvent event) {
        if (rootNode == null) return;

        // Convert screen touch coordinates to canvas "world" coordinates
        float touchX = event.getX();
        float touchY = event.getY();
        float canvasX = (touchX - translateX) / scaleFactor;
        float canvasY = (touchY - translateY) / scaleFactor;

        Node clickedNode = findNodeAt(rootNode, canvasX, canvasY);

        // --- START: THIS IS THE FIX ---
        // 1. Logic for selecting and deselecting nodes
        if (clickedNode != null) {
            // If the clicked node is the SAME as the already selected node,
            // we check if it has children. If so, we collapse/expand it.
            if (clickedNode == selectedNode && !clickedNode.children.isEmpty()) {
                clickedNode.isCollapsed = !clickedNode.isCollapsed;
                calculatePositions(); // Recalculate layout after state change
            } else {
                // Otherwise, we set the clicked node as the new selected node.
                selectedNode = clickedNode;
            }
        } else {
            // If the user clicked on an empty area of the canvas, deselect any active node.
            selectedNode = null;
        }
        // --- END: THIS IS THE FIX ---

        // Redraw the entire canvas to show the new selection and/or layout changes.
        invalidate();
    }

    private Node findNodeAt(Node node, float x, float y) {
        if (node == null) {
            return null;
        }

        // --- THIS IS THE FIX ---
        // 1. Check children FIRST, but in reverse order (top-most views).
        //    Only check children if the parent is not collapsed.
        if (!node.isCollapsed) {
            for (int i = node.children.size() - 1; i >= 0; i--) {
                Node foundInChild = findNodeAt(node.children.get(i), x, y);
                if (foundInChild != null) {
                    // If a child node was clicked, it has higher priority. Return it immediately.
                    return foundInChild;
                }
            }
        }

        // 2. If no children were clicked (or if the node is collapsed),
        //    check if the current node itself was clicked.
        if (node.bounds.contains(x, y)) {
            return node;
        }
        // --- END OF FIX ---

        // 3. If neither the node nor any of its children were clicked, return null.
        return null;
    }


    // --- NEW, CORRECTED METHOD ---
    public Bitmap exportToImage() {
        if (rootNode == null) return null;

        // 1. Calculate the actual bounds of the entire mind map.
        RectF totalBounds = new RectF();
        calculateTotalBounds(rootNode, totalBounds);

        if (totalBounds.isEmpty()) {
            return null; // Nothing to draw
        }

        // 2. Create a new bitmap with the exact size of the total bounds.
        // Add some padding for a nice margin.
        int padding = 50;
        int bitmapWidth = (int) totalBounds.width() + (padding * 2);
        int bitmapHeight = (int) totalBounds.height() + (padding * 2);

        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Fill the background of the exported image.
        canvas.drawColor(Color.WHITE);

        // 3. Temporarily save the current on-screen transformations.
        float oldTranslateX = this.translateX;
        float oldTranslateY = this.translateY;
        float oldScaleFactor = this.scaleFactor;

        // 4. Reset transformations for the export drawing.
        // We want to draw the mind map from the top-left of our new canvas.
        // We translate it so the top-left corner of the mind map content is at (padding, padding).
        this.translateX = -totalBounds.left + padding;
        this.translateY = -totalBounds.top + padding;
        this.scaleFactor = 1.0f; // No zoom

        // 5. Redraw the entire mind map onto the new canvas with the temporary transformations.
        // This is like calling onDraw() but we do it manually to control the canvas.
        canvas.save();
        canvas.translate(this.translateX, this.translateY);
        canvas.scale(this.scaleFactor, this.scaleFactor);
        drawConnections(canvas, rootNode);
        drawNodeRecursive(canvas, rootNode);
        canvas.restore();

        // 6. IMPORTANT: Restore the original on-screen transformations.
        this.translateX = oldTranslateX;
        this.translateY = oldTranslateY;
        this.scaleFactor = oldScaleFactor;

        // 7. Return the complete, correctly sized bitmap.
        return bitmap;
    }
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));

            float scaleChange = scaleFactor / oldScale;
            translateX = focusX + (translateX - focusX) * scaleChange;
            translateY = focusY + (translateY - focusY) * scaleChange;

            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!isDraggingNode) {
                translateX -= distanceX;
                translateY -= distanceY;
                invalidate();
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    public void resetNodePositions() {
        calculatePositions();
        invalidate();
    }

    public void centerMindMap() {
        if (rootNode == null) return;

        RectF bounds = calculateBoundingBox(rootNode);

        if (bounds.isEmpty()) return;

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (viewWidth == 0 || viewHeight == 0) return;

        float mindMapCenterX = bounds.centerX();
        float mindMapCenterY = bounds.centerY();

        float padding = 100f;
        float scaleX = (viewWidth - padding * 2) / bounds.width();
        float scaleY = (viewHeight - padding * 2) / bounds.height();

        scaleFactor = Math.min(scaleX, scaleY);
        scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 2.0f));

        translateX = viewWidth / 2f - mindMapCenterX * scaleFactor;
        translateY = viewHeight / 2f - mindMapCenterY * scaleFactor;

        invalidate();
    }

    public void centerMindMapAnimated() {
        if (rootNode == null) return;

        RectF bounds = calculateBoundingBox(rootNode);

        if (bounds.isEmpty()) return;

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (viewWidth == 0 || viewHeight == 0) return;

        float mindMapCenterX = bounds.centerX();
        float mindMapCenterY = bounds.centerY();

        float padding = 100f;
        float scaleX = (viewWidth - padding * 2) / bounds.width();
        float scaleY = (viewHeight - padding * 2) / bounds.height();

        float targetScale = Math.min(scaleX, scaleY);
        targetScale = Math.max(0.1f, Math.min(targetScale, 2.0f));

        float targetTranslateX = viewWidth / 2f - mindMapCenterX * targetScale;
        float targetTranslateY = viewHeight / 2f - mindMapCenterY * targetScale;

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300);

        final float startScale = scaleFactor;
        final float startTransX = translateX;
        final float startTransY = translateY;

        float finalTargetScale = targetScale;
        animator.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                float progress = (float) animation.getAnimatedValue();
                scaleFactor = startScale + (finalTargetScale - startScale) * progress;
                translateX = startTransX + (targetTranslateX - startTransX) * progress;
                translateY = startTransY + (targetTranslateY - startTransY) * progress;
                invalidate();
            }
        });

        animator.start();
    }

    private RectF calculateBoundingBox(Node node) {
        RectF bounds = new RectF(node.x, node.y, node.x, node.y);

        if (!node.isCollapsed) {
            for (Node child : node.children) {
                RectF childBounds = calculateBoundingBox(child);
                bounds.union(childBounds);
            }
        }

        bounds.inset(-150f, -100f);
        return bounds;
    }
}