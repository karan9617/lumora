package com.example.keyboardai.processor;

import java.util.Stack;

public class WordProcessor {
    private Stack<String> history = new Stack<>();
    public void processWord(String spokenText,StringBuilder resultBuilder){
        history.push(resultBuilder.toString());
        spokenText = spokenText.toLowerCase().trim();
        if (spokenText.equals("undo")) {
            // If the history stack is not empty, pop the last state and revert to it.
            if (!history.empty()) {
                String lastState = history.pop();
                resultBuilder.setLength(0); // Clear the current builder
                resultBuilder.append(lastState); // Append the last saved state
            }
            return; // We handled the command, so exit
        }

        if (spokenText.equals("go to next line") || spokenText.equals("new line") || spokenText.equals("next line")) {
            resultBuilder.append("\n");
        } else if (spokenText.equals("make a bullet point") || spokenText.equals("bullet point")) {
            resultBuilder.append("• ");
        } else if (spokenText.equals("add comma") || spokenText.equals("comma")) {
            resultBuilder.append(", ");
        } else if (spokenText.equals("add period") || spokenText.equals("period")) {
            resultBuilder.append(".");
        }
        else if (spokenText.equals("add question mark") || spokenText.equals("question mark")) {
            resultBuilder.append("?");
        } else if (spokenText.equals("add exclamation point") || spokenText.equals("exclamation point")) {
            resultBuilder.append("!");
        }
        else if (spokenText.equals("delete last word")) {
            String currentText = resultBuilder.toString().trim();
            if (currentText.length() > 0) {
                int lastSpaceIndex = currentText.lastIndexOf(" ");
                if (lastSpaceIndex != -1) {
                    resultBuilder.setLength(lastSpaceIndex + 1);
                } else {
                    resultBuilder.setLength(0); // Clear the whole text if it's a single word
                }
            }
        }

        else if (spokenText.equals("delete last word")) {
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
