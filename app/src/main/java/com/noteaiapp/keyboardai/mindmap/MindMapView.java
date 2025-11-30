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
        String[] lines = noteText.split("\n");
        List<String> nonEmptyLines = new ArrayList<>();

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                nonEmptyLines.add(line);
            }
        }

        if (nonEmptyLines.isEmpty()) {
            return new Node("Empty Note");
        }

        // Intelligent parsing: detect structure type
        StructureType structureType = detectStructureType(nonEmptyLines);

        Node root;

        switch (structureType) {
            case MARKDOWN_HEADERS:
                root = parseMarkdownHeaders(nonEmptyLines);
                break;
            case NUMBERED_LIST:
                root = parseNumberedList(nonEmptyLines);
                break;
            case OUTLINED:
                root = parseOutlinedStructure(nonEmptyLines);
                break;
            case KEY_VALUE:
                root = parseKeyValuePairs(nonEmptyLines);
                break;
            case PARAGRAPHS:
                root = parseIntoParagraphs(nonEmptyLines);
                break;
            case BULLET_INDENT:
            default:
                root = parseBulletIndent(nonEmptyLines);
                break;
        }

        // Smart summarization: extract key concepts
        enhanceWithKeywords(root);

        return root;
    }

    private enum StructureType {
        MARKDOWN_HEADERS,  // # Header, ## Subheader
        NUMBERED_LIST,     // 1. 2. 3. or 1.1, 1.2
        OUTLINED,          // I. A. 1. a.
        KEY_VALUE,         // Key: Value pairs
        PARAGRAPHS,        // Plain paragraphs
        BULLET_INDENT      // Bullet points with indentation
    }

    private StructureType detectStructureType(List<String> lines) {
        int headerCount = 0;
        int numberedCount = 0;
        int outlineCount = 0;
        int keyValueCount = 0;
        int bulletCount = 0;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.matches("^#{1,6}\\s+.+")) headerCount++;
            if (trimmed.matches("^\\d+\\.\\s+.+") || trimmed.matches("^\\d+\\.\\d+.*")) numberedCount++;
            if (trimmed.matches("^[IVX]+\\.\\s+.+") || trimmed.matches("^[A-Z]\\.\\s+.+")) outlineCount++;
            if (trimmed.matches("^[^:]+:\\s*.+")) keyValueCount++;
            if (trimmed.matches("^[-•*]\\s+.+")) bulletCount++;
        }

        int total = lines.size();
        if (headerCount > total * 0.3) return StructureType.MARKDOWN_HEADERS;
        if (numberedCount > total * 0.3) return StructureType.NUMBERED_LIST;
        if (outlineCount > total * 0.3) return StructureType.OUTLINED;
        if (keyValueCount > total * 0.4) return StructureType.KEY_VALUE;
        if (bulletCount > total * 0.3) return StructureType.BULLET_INDENT;

        return StructureType.PARAGRAPHS;
    }

    private Node parseMarkdownHeaders(List<String> lines) {
        Node root = new Node(extractTitle(lines.get(0)));
        root.level = 0;

        List<Node> stack = new ArrayList<>();
        stack.add(root);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (line.matches("^#{1,6}\\s+.+")) {
                int level = 0;
                while (level < line.length() && line.charAt(level) == '#') level++;

                String text = line.substring(level).trim();
                Node node = new Node(text);
                node.level = level;

                // Find correct parent
                while (stack.size() > level) {
                    stack.remove(stack.size() - 1);
                }

                Node parent = stack.get(stack.size() - 1);
                node.parent = parent;
                parent.children.add(node);
                stack.add(node);
            } else if (!line.isEmpty()) {
                // Content under current header
                Node parent = stack.get(stack.size() - 1);
                if (!parent.children.isEmpty() || line.length() > 50) {
                    Node contentNode = new Node(truncate(line, 30));
                    contentNode.level = parent.level + 1;
                    contentNode.parent = parent;
                    parent.children.add(contentNode);
                }
            }
        }

        return root;
    }

    private Node parseNumberedList(List<String> lines) {
        Node root = new Node(extractTitle(lines.get(0)));
        root.level = 0;

        Node currentParent = root;
        int lastLevel = 0;
        List<Node> stack = new ArrayList<>();
        stack.add(root);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (line.matches("^\\d+(\\.\\d+)*\\.?\\s+.+")) {
                String[] parts = line.split("\\s+", 2);
                String number = parts[0];
                String text = parts.length > 1 ? parts[1] : line;

                int level = (int) number.chars().filter(ch -> ch == '.').count();

                Node node = new Node(text);
                node.level = level + 1;

                while (stack.size() > level + 1) {
                    stack.remove(stack.size() - 1);
                }

                Node parent = stack.get(stack.size() - 1);
                node.parent = parent;
                parent.children.add(node);
                stack.add(node);
            }
        }

        return root;
    }

    private Node parseOutlinedStructure(List<String> lines) {
        Node root = new Node(extractTitle(lines.get(0)));
        root.level = 0;

        List<Node> stack = new ArrayList<>();
        stack.add(root);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            int level = 0;
            if (line.matches("^[IVX]+\\.\\s+.+")) level = 1;
            else if (line.matches("^[A-Z]\\.\\s+.+")) level = 2;
            else if (line.matches("^\\d+\\.\\s+.+")) level = 3;
            else if (line.matches("^[a-z]\\.\\s+.+")) level = 4;
            else continue;

            String text = line.replaceFirst("^[IVXa-z0-9]+\\.\\s+", "");
            Node node = new Node(text);
            node.level = level;

            while (stack.size() > level) {
                stack.remove(stack.size() - 1);
            }

            Node parent = stack.get(stack.size() - 1);
            node.parent = parent;
            parent.children.add(node);
            stack.add(node);
        }

        return root;
    }

    private Node parseKeyValuePairs(List<String> lines) {
        Node root = new Node("Key Concepts");
        root.level = 0;

        Node currentCategory = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.contains(":")) {
                String[] parts = trimmed.split(":", 2);
                String key = parts[0].trim();
                String value = parts.length > 1 ? parts[1].trim() : "";

                if (!value.isEmpty()) {
                    Node keyNode = new Node(key);
                    keyNode.level = 1;
                    keyNode.parent = root;
                    root.children.add(keyNode);

                    if (value.length() > 30) {
                        String[] sentences = value.split("[.!?]");
                        for (String sentence : sentences) {
                            if (sentence.trim().length() > 10) {
                                Node valueNode = new Node(truncate(sentence.trim(), 40));
                                valueNode.level = 2;
                                valueNode.parent = keyNode;
                                keyNode.children.add(valueNode);
                            }
                        }
                    } else {
                        Node valueNode = new Node(value);
                        valueNode.level = 2;
                        valueNode.parent = keyNode;
                        keyNode.children.add(valueNode);
                    }

                    currentCategory = keyNode;
                } else {
                    currentCategory = new Node(key);
                    currentCategory.level = 1;
                    currentCategory.parent = root;
                    root.children.add(currentCategory);
                }
            } else if (currentCategory != null) {
                Node item = new Node(truncate(trimmed, 40));
                item.level = 2;
                item.parent = currentCategory;
                currentCategory.children.add(item);
            }
        }

        return root;
    }

    private Node parseIntoParagraphs(List<String> lines) {
        Node root = new Node(extractTitle(lines.get(0)));
        root.level = 0;

        List<String> paragraphs = new ArrayList<>();
        StringBuilder currentPara = new StringBuilder();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (line.isEmpty() && currentPara.length() > 0) {
                paragraphs.add(currentPara.toString());
                currentPara = new StringBuilder();
            } else {
                if (currentPara.length() > 0) currentPara.append(" ");
                currentPara.append(line);
            }
        }
        if (currentPara.length() > 0) paragraphs.add(currentPara.toString());

        for (String para : paragraphs) {
            String[] sentences = para.split("[.!?]+");
            if (sentences.length > 0) {
                Node paraNode = new Node(extractKeyPhrase(sentences[0]));
                paraNode.level = 1;
                paraNode.parent = root;
                root.children.add(paraNode);

                for (int i = 1; i < Math.min(sentences.length, 4); i++) {
                    String sentence = sentences[i].trim();
                    if (sentence.length() > 15) {
                        Node sentNode = new Node(truncate(sentence, 35));
                        sentNode.level = 2;
                        sentNode.parent = paraNode;
                        paraNode.children.add(sentNode);
                    }
                }
            }
        }

        return root;
    }

    private Node parseBulletIndent(List<String> lines) {
        Node root = new Node(extractTitle(lines.get(0)));
        root.level = 0;

        List<NodeWithIndent> nodesWithIndent = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            int indent = getIndentLevel(line);
            String text = line.trim().replaceFirst("^[-•*]\\s*", "");

            nodesWithIndent.add(new NodeWithIndent(text, indent));
        }

        buildHierarchy(root, nodesWithIndent, 0, 0);

        return root;
    }

    private void enhanceWithKeywords(Node node) {
        // Extract keywords from node text (simple version)
        String text = node.text.toLowerCase();

        // Common stop words to filter
        String[] stopWords = {"the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for"};

        for (String stop : stopWords) {
            text = text.replaceAll("\\b" + stop + "\\b", "");
        }

        // Recursively enhance children
        for (Node child : node.children) {
            enhanceWithKeywords(child);
        }
    }

    private String extractTitle(String firstLine) {
        String title = firstLine.trim();
        title = title.replaceFirst("^#{1,6}\\s+", "");
        title = title.replaceFirst("^[IVX0-9]+\\.\\s+", "");
        title = title.replaceFirst("^[-•*]\\s+", "");

        if (title.contains(":")) {
            title = title.split(":")[0];
        }

        return truncate(title, 50);
    }

    private String extractKeyPhrase(String sentence) {
        sentence = sentence.trim();
        String[] words = sentence.split("\\s+");

        if (words.length <= 5) return sentence;

        // Extract first meaningful phrase (up to 5 words)
        StringBuilder phrase = new StringBuilder();
        int count = 0;
        for (String word : words) {
            if (count >= 5) break;
            if (word.length() > 2) {
                if (phrase.length() > 0) phrase.append(" ");
                phrase.append(word);
                count++;
            }
        }

        return phrase.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private static class NodeWithIndent {
        String text;
        int indent;

        NodeWithIndent(String text, int indent) {
            this.text = text;
            this.indent = indent;
        }
    }

    private int getIndentLevel(String line) {
        int spaces = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') spaces++;
            else if (c == '\t') spaces += 4;
            else break;
        }
        return spaces / 2; // 2 spaces = 1 indent level
    }

    private int buildHierarchy(Node parent, List<NodeWithIndent> items, int startIndex, int parentIndent) {
        int i = startIndex;
        while (i < items.size()) {
            NodeWithIndent item = items.get(i);

            if (item.indent <= parentIndent) {
                break; // Return to parent level
            }

            if (item.indent == parentIndent + 1) {
                Node child = new Node(item.text);
                child.level = parent.level + 1;
                child.parent = parent;
                parent.children.add(child);

                // Recursively add children
                i = buildHierarchy(child, items, i + 1, item.indent);
            } else {
                i++;
            }
        }
        return i;
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

    private void calculateRadialLayout(int width, int height) {
        rootNode.x = width / 2f;
        rootNode.y = height / 2f;

        positionChildrenRadial(rootNode, Math.min(width, height) * 0.25f, 0, 360);
    }

    private void positionChildrenRadial(Node parent, float radius, float startAngle, float endAngle) {
        if (parent.isCollapsed || parent.children.isEmpty()) return;

        int visibleChildren = parent.children.size();
        float angleStep = (endAngle - startAngle) / visibleChildren;

        for (int i = 0; i < visibleChildren; i++) {
            Node child = parent.children.get(i);
            float angle = startAngle + angleStep * i + angleStep / 2;
            double angleRad = Math.toRadians(angle - 90);

            child.x = parent.x + (float)(Math.cos(angleRad) * radius);
            child.y = parent.y + (float)(Math.sin(angleRad) * radius);

            // Recursively position grandchildren
            if (!child.children.isEmpty()) {
                float childStartAngle = angle - angleStep / 2;
                float childEndAngle = angle + angleStep / 2;
                positionChildrenRadial(child, radius * 0.7f, childStartAngle, childEndAngle);
            }
        }
    }

    private void calculateTreeLayout(int width, int height) {
        rootNode.x = width / 2f;
        rootNode.y = 150f;

        positionChildrenTree(rootNode, width / 2f, 150f, width, 250f);
    }

    private void positionChildrenTree(Node parent, float centerX, float y, float availableWidth, float verticalSpacing) {
        if (parent.isCollapsed || parent.children.isEmpty()) return;

        int childCount = parent.children.size();
        float spacing = availableWidth / (childCount + 1);
        float startX = centerX - (availableWidth / 2) + spacing;

        for (int i = 0; i < childCount; i++) {
            Node child = parent.children.get(i);
            child.x = startX + i * spacing;
            child.y = y + verticalSpacing;

            // Recursively position children
            if (!child.children.isEmpty()) {
                positionChildrenTree(child, child.x, child.y, spacing * 0.8f, verticalSpacing);
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
        float totalHeight = childCount * 150f;
        float startY = centerY - totalHeight / 2;

        for (int i = 0; i < childCount; i++) {
            Node child = parent.children.get(i);
            child.x = x + horizontalSpacing;
            child.y = startY + i * 150f + 75f;

            // Recursively position children
            if (!child.children.isEmpty()) {
                positionChildrenHierarchical(child, child.x, child.y, horizontalSpacing);
            }
        }
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

        // Save canvas state
        canvas.save();

        // Apply transformations - simplified to only translate and scale
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        // Draw connections and nodes
        drawConnections(canvas, rootNode);
        drawNodeRecursive(canvas, rootNode);

        // Restore canvas state
        canvas.restore();
    }

    private void drawConnections(Canvas canvas, Node node) {
        if (node.isCollapsed) return;

        for (Node child : node.children) {
            linePaint.setColor(nodeColors[child.level % nodeColors.length]);

            // Draw curved connection
            Path path = new Path();
            path.moveTo(node.x, node.y);

            float ctrlX1 = node.x + (child.x - node.x) * 0.5f;
            float ctrlY1 = node.y;
            float ctrlX2 = node.x + (child.x - node.x) * 0.5f;
            float ctrlY2 = child.y;

            path.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, child.x, child.y);
            canvas.drawPath(path, linePaint);

            // Recursively draw child connections
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

        // Multi-line text wrapping
        List<String> lines = wrapText(displayText, 300f); // Max width for text

        float maxLineWidth = 0;
        for (String line : lines) {
            float lineWidth = textPaint.measureText(line);
            if (lineWidth > maxLineWidth) maxLineWidth = lineWidth;
        }

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float lineHeight = fm.descent - fm.ascent + 10f; // Add line spacing
        float totalTextHeight = lineHeight * lines.size();

        float padding = 40f;
        float rectWidth = Math.max(maxLineWidth + padding * 2, 200f); // Min width 200
        float rectHeight = totalTextHeight + padding * 2;

        node.bounds.set(
                node.x - rectWidth / 2,
                node.y - rectHeight / 2,
                node.x + rectWidth / 2,
                node.y + rectHeight / 2
        );

        paint.setColor(nodeColors[node.level % nodeColors.length]);
        canvas.drawRoundRect(node.bounds, 25f, 25f, paint);

        // Draw multi-line text
        float textStartY = node.y - (totalTextHeight / 2) - fm.ascent;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float lineWidth = textPaint.measureText(line);
            float textX = node.x - lineWidth / 2;
            float textY = textStartY + i * lineHeight;
            canvas.drawText(line, textX, textY, textPaint);
        }

        // Draw collapse/expand indicator
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

        // Limit to 4 lines max with ellipsis
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
        // Let scale detector handle pinch gestures
        scaleDetector.onTouchEvent(event);

        // Handle node dragging
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(event);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // Multi-touch detected, stop node dragging
                isDraggingNode = false;
                draggedNode = null;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDraggingNode && draggedNode != null) {
                    handleNodeDrag(event);
                    return true;
                } else if (event.getPointerCount() == 1) {
                    // Single finger pan
                    gestureDetector.onTouchEvent(event);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (!isDraggingNode) {
                    handleNodeClick(event);
                }
                isDraggingNode = false;
                draggedNode = null;
                break;

            case MotionEvent.ACTION_CANCEL:
                isDraggingNode = false;
                draggedNode = null;
                break;
        }

        return true;
    }

    private void handleTouchDown(MotionEvent event) {
        if (rootNode == null) return;

        // Transform touch coordinates to canvas coordinates
        float[] points = new float[]{event.getX(), event.getY()};
        Matrix inverse = new Matrix();
        inverse.postTranslate(-translateX, -translateY);
        inverse.postScale(1/scaleFactor, 1/scaleFactor);
        inverse.mapPoints(points);

        dragStartX = points[0];
        dragStartY = points[1];

        // Check if user touched a node
        Node touchedNode = findNodeAt(rootNode, points[0], points[1]);
        if (touchedNode != null) {
            draggedNode = touchedNode;
            nodeDragOffsetX = touchedNode.x - points[0];
            nodeDragOffsetY = touchedNode.y - points[1];
            isDraggingNode = true;
        }
    }

    private void handleNodeDrag(MotionEvent event) {
        // Transform touch coordinates to canvas coordinates
        float[] points = new float[]{event.getX(), event.getY()};
        Matrix inverse = new Matrix();
        inverse.postTranslate(-translateX, -translateY);
        inverse.postScale(1/scaleFactor, 1/scaleFactor);
        inverse.mapPoints(points);

        // Update dragged node position
        draggedNode.x = points[0] + nodeDragOffsetX;
        draggedNode.y = points[1] + nodeDragOffsetY;

        invalidate();
    }

    private void handleNodeClick(MotionEvent event) {
        if (rootNode == null) return;

        // Transform touch coordinates to canvas coordinates
        float[] points = new float[]{event.getX(), event.getY()};
        Matrix inverse = new Matrix();
        inverse.postTranslate(-translateX, -translateY);
        inverse.postScale(1/scaleFactor, 1/scaleFactor);
        inverse.mapPoints(points);

        Node clickedNode = findNodeAt(rootNode, points[0], points[1]);
        if (clickedNode != null && !clickedNode.children.isEmpty()) {
            clickedNode.isCollapsed = !clickedNode.isCollapsed;
            invalidate();
        }
    }

    private Node findNodeAt(Node node, float x, float y) {
        if (node.bounds.contains(x, y)) {
            return node;
        }

        if (!node.isCollapsed) {
            for (Node child : node.children) {
                Node found = findNodeAt(child, x, y);
                if (found != null) return found;
            }
        }

        return null;
    }

    public Bitmap exportToImage() {
        if (rootNode == null) return null;

        // Create a large bitmap to capture the entire mindmap
        int width = getWidth() > 0 ? getWidth() : 1080;
        int height = getHeight() > 0 ? getHeight() : 1920;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        // Temporarily reset transformations for export
        float oldScale = scaleFactor;
        float oldTransX = translateX;
        float oldTransY = translateY;

        scaleFactor = 1.0f;
        translateX = 0f;
        translateY = 0f;

        draw(canvas);

        // Restore transformations
        scaleFactor = oldScale;
        translateX = oldTransX;
        translateY = oldTransY;

        return bitmap;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float lastScaleFactor = 1.0f;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            lastScaleFactor = scaleFactor;
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));

            // Adjust translation to zoom towards focus point
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
            // Only pan if not dragging a node
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

    // Helper method to enable/disable node dragging
    public void setNodeDraggingEnabled(boolean enabled) {
        // This allows you to toggle between dragging mode and pan mode
        // For now, both are always enabled with smart detection
    }

    // Helper method to reset all node positions to original layout
    public void resetNodePositions() {
        calculatePositions();
        invalidate();
    }

    // Helper method to get current node position (useful for saving state)
    public void saveNodePositions() {
        // You can implement this to save positions to SharedPreferences
        // or return a Map<String, PointF> of node positions
    }

    // Helper method to restore node positions
    public void restoreNodePositions() {
        // Implement this to restore saved positions
    }
}

// Example Activity Implementation
/*
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private MindMapView mindMapView;
    private String noteContent = "Project Planning\n" +
            "  Development\n" +
            "    Frontend\n" +
            "      React components\n" +
            "      State management\n" +
            "    Backend\n" +
            "      API design\n" +
            "      Database schema\n" +
            "  Design\n" +
            "    UI mockups\n" +
            "    User flow\n" +
            "  Testing\n" +
            "    Unit tests\n" +
            "    Integration tests";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mindMapView = findViewById(R.id.mindMapView);
        Button btnGenerate = findViewById(R.id.btnGenerateMindMap);
        Button btnRadial = findViewById(R.id.btnRadialLayout);
        Button btnTree = findViewById(R.id.btnTreeLayout);
        Button btnHierarchical = findViewById(R.id.btnHierarchicalLayout);
        Button btnExport = findViewById(R.id.btnExport);

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
*/

// Layout XML
/*
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <Button
            android:id="@+id/btnGenerateMindMap"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Generate"
            android:layout_margin="4dp"/>

        <Button
            android:id="@+id/btnExport"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Export"
            android:layout_margin="4dp"/>
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <Button
            android:id="@+id/btnRadialLayout"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Radial"
            android:layout_margin="4dp"/>

        <Button
            android:id="@+id/btnTreeLayout"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Tree"
            android:layout_margin="4dp"/>

        <Button
            android:id="@+id/btnHierarchicalLayout"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Hierarchical"
            android:layout_margin="4dp"/>
    </LinearLayout>

    <com.yourpackage.MindMapView
        android:id="@+id/mindMapView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_margin="4dp"
        android:background="#F5F5F5"/>

</LinearLayout>
*/