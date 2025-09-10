package com.example.keyboardai.processor;

public class WordProcessor {
    public void processWord(String spokenText,StringBuilder resultBuilder){
        if (spokenText.equals("go to next line") || spokenText.equals("new line") || spokenText.equals("next line")) {
            resultBuilder.append("\n");
        } else if (spokenText.equals("make a bullet point") || spokenText.equals("bullet point")) {
            resultBuilder.append("\n• ");
        } else if (spokenText.equals("add comma") || spokenText.equals("comma")) {
            resultBuilder.append(", ");
        } else if (spokenText.equals("add period") || spokenText.equals("period")) {
            resultBuilder.append(".");
        } else if (spokenText.equals("delete last word")) {
            String currentText = resultBuilder.toString().trim();
            if (currentText.length() > 0) {
                int lastSpaceIndex = currentText.lastIndexOf(" ");
                if (lastSpaceIndex != -1) {
                    resultBuilder.setLength(lastSpaceIndex + 1);
                } else {
                    resultBuilder.setLength(0); // Clear the whole text if it's a single word
                }
            }
        } else if (spokenText.equals("delete last word")) {
            String currentText = resultBuilder.toString().trim();
            if (currentText.length() > 0) {
                int lastSpaceIndex = currentText.lastIndexOf(" ");
                if (lastSpaceIndex != -1) {
                    resultBuilder.setLength(lastSpaceIndex + 1);
                } else {
                    resultBuilder.setLength(0); // Clear the whole text if it's a single word
                }
            }
        } else if (spokenText.equals("done") || spokenText.equals("stop") || spokenText.equals("that's it")) {
            return;
        }
        else{
            resultBuilder.append(spokenText);
        }
    }
}
