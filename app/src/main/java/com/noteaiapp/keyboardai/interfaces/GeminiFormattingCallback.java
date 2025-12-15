package com.noteaiapp.keyboardai.interfaces;

public interface GeminiFormattingCallback {
    void onFormattingComplete(String formattedHtml);
    void onFormattingFailed();
}