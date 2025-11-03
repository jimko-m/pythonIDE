package com.pythonide.editor;

import android.text.Editable;
import android.util.Log;

import java.util.Stack;

/**
 * معالج التراجع وإعادة التنفيذ (Undo/Redo Manager)
 * يوفر إمكانية التراجع وإعادة التنفيذ للتغييرات في محرر الكود
 */
public class UndoRedoManager {
    
    private static final int MAX_UNDO_STEPS = 100;
    
    private CodeEditText codeEditText;
    private Stack<TextSnapshot> undoStack = new Stack<>();
    private Stack<TextSnapshot> redoStack = new Stack<>();
    private String lastSnapshot = "";
    private boolean isUndoRedo = false;
    
    public UndoRedoManager(CodeEditText codeEditText) {
        this.codeEditText = codeEditText;
        setupTextWatcher();
    }
    
    private void setupTextWatcher() {
        codeEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing during undo/redo
                if (isUndoRedo) return;
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing during undo/redo
                if (isUndoRedo) return;
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing during undo/redo
                if (isUndoRedo) return;
                
                String currentText = s.toString();
                
                // Only save snapshot if text actually changed
                if (!currentText.equals(lastSnapshot)) {
                    saveSnapshot(currentText);
                    lastSnapshot = currentText;
                    
                    // Clear redo stack when new changes are made
                    redoStack.clear();
                }
            }
        });
    }
    
    /**
     * حفظ لقطة شاشة للنص الحالي
     */
    private void saveSnapshot(String text) {
        if (undoStack.size() >= MAX_UNDO_STEPS) {
            undoStack.remove(0); // Remove oldest entry
        }
        
        undoStack.push(new TextSnapshot(text, System.currentTimeMillis()));
        
        Log.d("UndoRedoManager", "Saved snapshot. Undo stack size: " + undoStack.size());
    }
    
    /**
     * تنفيذ عملية التراجع
     */
    public void undo() {
        if (undoStack.size() <= 1) {
            Log.d("UndoRedoManager", "Nothing to undo");
            return;
        }
        
        isUndoRedo = true;
        
        try {
            // Move current state to redo stack
            TextSnapshot currentState = undoStack.pop();
            redoStack.push(currentState);
            
            // Restore previous state
            TextSnapshot previousState = undoStack.peek();
            codeEditText.setText(previousState.text);
            
            Log.d("UndoRedoManager", "Undo performed. Undo stack: " + undoStack.size() + 
                ", Redo stack: " + redoStack.size());
            
        } finally {
            isUndoRedo = false;
        }
    }
    
    /**
     * تنفيذ عملية إعادة التنفيذ
     */
    public void redo() {
        if (redoStack.isEmpty()) {
            Log.d("UndoRedoManager", "Nothing to redo");
            return;
        }
        
        isUndoRedo = true;
        
        try {
            // Move from redo stack to undo stack
            TextSnapshot stateToRestore = redoStack.pop();
            undoStack.push(stateToRestore);
            
            // Restore the text
            codeEditText.setText(stateToRestore.text);
            
            Log.d("UndoRedoManager", "Redo performed. Undo stack: " + undoStack.size() + 
                ", Redo stack: " + redoStack.size());
            
        } finally {
            isUndoRedo = false;
        }
    }
    
    /**
     * التحقق من إمكانية التراجع
     */
    public boolean canUndo() {
        return undoStack.size() > 1;
    }
    
    /**
     * التحقق من إمكانية إعادة التنفيذ
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * مسح سجل التراجع وإعادة التنفيذ
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        saveSnapshot(codeEditText.getText().toString());
        Log.d("UndoRedoManager", "History cleared");
    }
    
    /**
     * الحصول على عدد خطوات التراجع المتاحة
     */
    public int getUndoStepCount() {
        return Math.max(0, undoStack.size() - 1); // -1 because current state is always in stack
    }
    
    /**
     * الحصول على عدد خطوات إعادة التنفيذ المتاحة
     */
    public int getRedoStepCount() {
        return redoStack.size();
    }
    
    /**
     * تحديث snapshot بعد تغيير النص
     */
    public void onTextChanged(String text) {
        if (!isUndoRedo && !text.equals(lastSnapshot)) {
            saveSnapshot(text);
            lastSnapshot = text;
        }
    }
    
    /**
     * فئة لتخزين حالة النص
     */
    private static class TextSnapshot {
        public final String text;
        public final long timestamp;
        
        public TextSnapshot(String text, long timestamp) {
            this.text = text;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return "TextSnapshot{length=" + text.length() + ", timestamp=" + timestamp + "}";
        }
    }
}