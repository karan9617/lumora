package com.noteaiapp.keyboardai.adapter;

public interface ItemTouchHelperAdapter {
    void onItemMove(int fromPosition, int toPosition);
    void onItemsMoved();
}