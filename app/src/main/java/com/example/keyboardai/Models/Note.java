package com.example.keyboardai.Models;

import java.io.Serializable;

// Note: The previous conversation assumed Room, but your database is SQLite.
// This model is compatible with both as a simple POJO (Plain Old Java Object),
// but Room annotations have been removed as they are not needed for your SQLite implementation.
public class Note implements Serializable {

    // Unique identifier for the note in the database
    private long id;
    private String title;
    private String content;
    private String date;
    private byte[] drawingData;
    private int color;
    // NEW FIELD: To store the display order of the note
    private int order;

    // Full constructor for loading notes from the database
    public Note(long id, String title, String content, String date, byte[] drawingData, int color) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.drawingData = drawingData;
        this.color = color;
        // The order will be set separately after retrieval
    }

    // Constructor for creating a new note before insertion
    public Note(String title, String content, String date, byte[] drawingData, int color) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.drawingData = drawingData;
        this.color = color;
    }

    // --- Getters and Setters ---

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

    public byte[] getDrawingData() {
        return drawingData;
    }

    public void setDrawingData(byte[] drawingData) {
        this.drawingData = drawingData;
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
}
