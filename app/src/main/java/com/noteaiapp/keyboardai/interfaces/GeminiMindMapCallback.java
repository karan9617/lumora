package com.noteaiapp.keyboardai.interfaces;

public interface GeminiMindMapCallback {
    void onStructureGenerated(String structuredText);
    void onGenerationFailed(Exception e);
}
