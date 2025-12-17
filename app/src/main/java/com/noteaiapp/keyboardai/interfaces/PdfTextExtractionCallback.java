package com.noteaiapp.keyboardai.interfaces;

public interface PdfTextExtractionCallback {
    void onTextExtracted(String text);
    void onExtractionFailed(Exception e);
}