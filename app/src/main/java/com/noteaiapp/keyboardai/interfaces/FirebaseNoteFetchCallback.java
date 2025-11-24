package com.noteaiapp.keyboardai.interfaces;

import com.noteaiapp.keyboardai.Models.Note;

// but outside of any methods.
public interface FirebaseNoteFetchCallback {
    void onNoteFetched(Note note);
    void onFetchFailed(Exception e);
}