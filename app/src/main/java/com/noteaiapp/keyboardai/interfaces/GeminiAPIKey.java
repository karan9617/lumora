package com.noteaiapp.keyboardai.interfaces;

import com.noteaiapp.keyboardai.BuildConfig;

public class GeminiAPIKey {
    public static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    public static final String API_URL_GEMINI = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=";
}
