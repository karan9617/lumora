package com.example.keyboardai.data;

import android.util.Patterns;

public class EmailValidator {
    public static boolean isValidEmail(CharSequence email) {
        // Checks the provided CharSequence against a standard email regex pattern
        if (email == null) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}