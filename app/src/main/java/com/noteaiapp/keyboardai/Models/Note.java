package com.noteaiapp.keyboardai.Models;

import java.io.Serializable;
public class Note implements Serializable {

    // Unique identifier for the note in the database
    private long id;
    private String title;
    private String content;
    private String date;
    private int color;
    // NEW FIELD: To store the display order of the note
    private int order;
    // NEW FIELD: To determine if the note is pinned
    private boolean isPinned;
    // NEW FIELD: To store the file path of the saved image
    private String imagePath;
    // NEW FIELD: For selection state (not stored in database)
    private boolean isSelected = false;
    private String fontFamily;
    private String fontColor;
    public Note(){}

    // Full constructor for loading notes from the database
    public Note(long id, String title, String content, String date, int color, int order, boolean isPinned, String imagePath) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.color = color;
        this.order = order;
        this.isPinned = isPinned;
        this.imagePath = imagePath;
        this.isSelected = false; // Default to not selected
    }

    // Constructor for creating a new note before insertion
    public Note(String title, String content, String date, int color, boolean isPinned, String imagePath) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.color = color;
        this.isPinned = isPinned;
        this.imagePath = imagePath;
        this.isSelected = false; // Default to not selected
    }
    public Note(String title, String content, String date, int color, int order, boolean isPinned, String imagePath) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.color = color;
        this.order = order;
        this.isPinned = isPinned;
        this.imagePath = imagePath;
        this.isSelected = false; // Default to not selected
    }
    public Note(String title, String content, String date, int color, int order, boolean isPinned, String imagePath, String folder) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.color = color;
        this.order = order;
        this.isPinned = isPinned;
        this.imagePath = imagePath;
        this.isSelected = false; // Default to not selected

    }
    public Note(String title, String content, String date, int color, int order, String fontColor, boolean isPinned, String imagePath) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.color = color;
        this.order = order;
        this.isPinned = isPinned;
        this.imagePath = imagePath;
        this.isSelected = false; // Default to not selected
        this.fontColor = fontColor;
    }
    public Note(long id, String title, String content, String date, int color, int order, String fontColor, boolean isPinned, String imagePath) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.color = color;
        this.order = order;
        this.isPinned = isPinned;
        this.imagePath = imagePath;
        this.isSelected = false; // Default to not selected
        this.fontColor = fontColor;
    }
    // --- Getters and Setters ---


    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public void setFontColor(String getFontColor) {
        this.fontColor = getFontColor;
    }

    public String getFontColor() {
        return fontColor;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }


    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    // NEW: Getter and setter for the 'order' field
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    // NEW: Getter and setter for the 'isPinned' field
    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    // NEW: Getter and setter for the 'imagePath' field
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { this.isSelected = selected; }

}