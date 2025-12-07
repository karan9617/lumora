package com.noteaiapp.keyboardai.Models;

import android.graphics.Bitmap;

public class ChatMessage {
    private String message;
    private boolean isUser;
    private Bitmap image;
    private boolean isImage; // New flag to identify message type

    private String type;
    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public Bitmap getImage() {
        return image;
    }
    public void setIsImageFlag(boolean isImage){
        this.isImage = isImage;
    }
    public boolean getImageFlag(){
        return isImage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUser(boolean user) {
        isUser = user;
    }

    public boolean isUser() {
        return isUser;
    }
}