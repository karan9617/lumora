package com.noteaiapp.keyboardai.data;

import java.util.*;
import java.util.regex.Pattern;

public class WordTokenizer {
    private String content;
    private static final int MAX_FREQUENCY_LABELS = 3; // Reduced from 5 for better quality
    private static final int MIN_WORD_LENGTH = 3; // Increased minimum length
    private static final int MIN_FREQUENCY = 2; // Only include words that appear multiple times

    // Enhanced label keywords with better categorization
    private static final Map<String, String> LABEL_KEYWORDS = new HashMap<>();
    static {
        // Work & Professional
        LABEL_KEYWORDS.put("meeting", "work");
        LABEL_KEYWORDS.put("project", "work");
        LABEL_KEYWORDS.put("report", "work");
        LABEL_KEYWORDS.put("presentation", "work");
        LABEL_KEYWORDS.put("deadline", "work");
        LABEL_KEYWORDS.put("client", "work");
        LABEL_KEYWORDS.put("conference call", "work");
        LABEL_KEYWORDS.put("budget", "work");
        LABEL_KEYWORDS.put("proposal", "work");

        // Shopping & Groceries
        LABEL_KEYWORDS.put("groceries", "shopping");
        LABEL_KEYWORDS.put("shopping list", "shopping");
        LABEL_KEYWORDS.put("supermarket", "shopping");
        LABEL_KEYWORDS.put("walmart", "shopping");
        LABEL_KEYWORDS.put("target", "shopping");
        LABEL_KEYWORDS.put("buy", "shopping");
        LABEL_KEYWORDS.put("purchase", "shopping");

        // Food items
        LABEL_KEYWORDS.put("milk", "food");
        LABEL_KEYWORDS.put("eggs", "food");
        LABEL_KEYWORDS.put("bread", "food");
        LABEL_KEYWORDS.put("chicken", "food");
        LABEL_KEYWORDS.put("vegetables", "food");
        LABEL_KEYWORDS.put("fruits", "food");

        // Tasks & Reminders
        LABEL_KEYWORDS.put("remind me", "tasks");
        LABEL_KEYWORDS.put("to-do", "tasks");
        LABEL_KEYWORDS.put("don't forget", "tasks");
        LABEL_KEYWORDS.put("remember", "tasks");
        LABEL_KEYWORDS.put("due", "tasks");
        LABEL_KEYWORDS.put("task", "tasks");
        LABEL_KEYWORDS.put("complete", "tasks");

        // Cooking & Recipes
        LABEL_KEYWORDS.put("recipe", "cooking");
        LABEL_KEYWORDS.put("ingredients", "cooking");
        LABEL_KEYWORDS.put("bake", "cooking");
        LABEL_KEYWORDS.put("cook", "cooking");
        LABEL_KEYWORDS.put("oven", "cooking");
        LABEL_KEYWORDS.put("preparation", "cooking");
        LABEL_KEYWORDS.put("serving", "cooking");

        // Travel & Transportation
        LABEL_KEYWORDS.put("flight", "travel");
        LABEL_KEYWORDS.put("hotel", "travel");
        LABEL_KEYWORDS.put("itinerary", "travel");
        LABEL_KEYWORDS.put("packing list", "travel");
        LABEL_KEYWORDS.put("vacation", "travel");
        LABEL_KEYWORDS.put("trip", "travel");
        LABEL_KEYWORDS.put("booking", "travel");
        LABEL_KEYWORDS.put("passport", "travel");

        // Health & Medical
        LABEL_KEYWORDS.put("doctor", "health");
        LABEL_KEYWORDS.put("appointment", "health");
        LABEL_KEYWORDS.put("medicine", "health");
        LABEL_KEYWORDS.put("prescription", "health");
        LABEL_KEYWORDS.put("symptoms", "health");
        LABEL_KEYWORDS.put("hospital", "health");
        LABEL_KEYWORDS.put("medicines", "health");

        // Finance & Money
        LABEL_KEYWORDS.put("budget", "finance");
        LABEL_KEYWORDS.put("expense", "finance");
        LABEL_KEYWORDS.put("bill", "finance");
        LABEL_KEYWORDS.put("payment", "finance");
        LABEL_KEYWORDS.put("invoice", "finance");
        LABEL_KEYWORDS.put("tax", "finance");
        LABEL_KEYWORDS.put("investment", "finance");

        // Education & Learning
        LABEL_KEYWORDS.put("study", "education");
        LABEL_KEYWORDS.put("exam", "education");
        LABEL_KEYWORDS.put("homework", "education");
        LABEL_KEYWORDS.put("assignment", "education");
        LABEL_KEYWORDS.put("course", "education");
        LABEL_KEYWORDS.put("lecture", "education");
    }

    // Expanded stop words list
    private static final Set<String> STOP_WORDS = new HashSet<>();
    static {
        Collections.addAll(STOP_WORDS,
                // Articles & Determiners
                "a", "an", "the", "this", "that", "these", "those",
                // Prepositions
                "in", "on", "at", "by", "for", "with", "without", "to", "from", "of", "about", "into", "onto", "upon",
                // Conjunctions
                "and", "or", "but", "so", "yet", "nor", "because", "since", "although", "while",
                // Pronouns
                "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them", "my", "your", "his", "her", "its", "our", "their",
                // Verbs (common)
                "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might", "can",
                // Adverbs
                "not", "no", "yes", "very", "too", "also", "just", "only", "even", "still", "already", "yet", "again",
                // Others
                "such", "then", "there", "here", "where", "when", "how", "why", "what", "which", "who", "whom", "whose"
        );
    }

    // Pattern for better text cleaning
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]+");

    public WordTokenizer() {}

    public WordTokenizer(String content) {
        this.content = content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    private static final int MIN_LABELS_REQUIRED = 2; // New constant

    public List<String> getTokenizedWords() {
        try {
            Set<String> labels = new LinkedHashSet<>(); // Preserve insertion order

            if (content == null || content.trim().isEmpty()) {
                return getMinimumLabels(labels); // Ensure minimum labels even for empty content
            }

            String lowerCaseContent = content.toLowerCase();

            // Count actual words in content
            int wordCount = countWords(lowerCaseContent);

            // Handle very short content differently
            if (wordCount == 0) {
                return getMinimumLabels(labels); // No words, but still return minimum labels
            } else if (wordCount <= 2) {
                List<String> shortContentLabels = handleShortContent(lowerCaseContent, labels);
                return ensureMinimumLabels(shortContentLabels, lowerCaseContent);
            }

            // Normal processing for longer content
            // Step 1: Check for predefined label keywords (prioritized)
            detectPredefinedLabels(lowerCaseContent, labels);

            // Step 2: Extract high-frequency meaningful words (with relaxed frequency for minimum labels)
            if (labels.size() < MIN_LABELS_REQUIRED + 1) {
                detectFrequencyBasedLabels(lowerCaseContent, labels);
            }

            // Step 3: Ensure we have minimum required labels
            return ensureMinimumLabels(new ArrayList<>(labels), lowerCaseContent);

        } catch (Exception e) {
            // Log the error and return minimum labels instead of empty
            System.err.println("Error in getTokenizedWords: " + e.getMessage());
            e.printStackTrace();
            return getMinimumLabels(new LinkedHashSet<>());
        }
    }

    private List<String> ensureMinimumLabels(List<String> currentLabels, String content) {
        Set<String> labels = new LinkedHashSet<>(currentLabels); // Remove duplicates while preserving order

        // If we already have enough labels, return them
        if (labels.size() >= MIN_LABELS_REQUIRED) {
            return new ArrayList<>(labels);
        }

        // Add general category labels based on content analysis
        labels.addAll(generateGeneralLabels(content, MIN_LABELS_REQUIRED - labels.size()));

        return new ArrayList<>(labels);
    }

    private List<String> getMinimumLabels(Set<String> existingLabels) {
        // For empty or very minimal content, return general categories
        List<String> defaultLabels = Arrays.asList("note", "text");
        existingLabels.addAll(defaultLabels);
        return new ArrayList<>(existingLabels);
    }

    private List<String> generateGeneralLabels(String content, int neededCount) {
        List<String> generalLabels = new ArrayList<>();
        String lowerContent = content.toLowerCase();

        // Content-based general categories
        if (containsNumbers(lowerContent)) {
            generalLabels.add("numbers");
        }

        if (containsQuestionWords(lowerContent)) {
            generalLabels.add("questions");
        }

        if (containsTimeWords(lowerContent)) {
            generalLabels.add("schedule");
        }

        if (containsActionWords(lowerContent)) {
            generalLabels.add("tasks");
        }

        if (lowerContent.length() > 100) {
            generalLabels.add("detailed");
        } else if (lowerContent.length() < 20) {
            generalLabels.add("brief");
        }

        // Length-based categories
        int wordCount = countWords(lowerContent);
        if (wordCount <= 5) {
            generalLabels.add("quick-note");
        } else if (wordCount > 50) {
            generalLabels.add("long-form");
        } else {
            generalLabels.add("standard");
        }

        // Default fallback labels if we still don't have enough
        List<String> fallbackLabels = Arrays.asList(
                "general", "note", "text", "content", "memo", "personal", "misc"
        );

        // Add labels until we have enough
        for (String label : fallbackLabels) {
            if (!generalLabels.contains(label)) {
                generalLabels.add(label);
            }
            if (generalLabels.size() >= neededCount) {
                break;
            }
        }

        // Return only the number we need
        return generalLabels.subList(0, Math.min(generalLabels.size(), neededCount));
    }

    private boolean containsNumbers(String content) {
        return content.matches(".*\\d.*");
    }

    private boolean containsQuestionWords(String content) {
        String[] questionWords = {"what", "when", "where", "why", "how", "who", "which", "?"};
        for (String word : questionWords) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTimeWords(String content) {
        String[] timeWords = {"today", "tomorrow", "yesterday", "monday", "tuesday", "wednesday",
                "thursday", "friday", "saturday", "sunday", "morning", "afternoon",
                "evening", "night", "am", "pm", "o'clock", "time", "schedule",
                "appointment", "meeting", "deadline"};
        for (String word : timeWords) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsActionWords(String content) {
        String[] actionWords = {"do", "make", "get", "buy", "call", "email", "send", "write",
                "read", "check", "visit", "go", "come", "finish", "start",
                "complete", "submit", "review", "update", "create"};
        for (String word : actionWords) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private int countWords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        String cleanedContent = content.trim().replaceAll("\\s+", " ");
        return cleanedContent.split(" ").length;
    }

    private List<String> handleShortContent(String lowerCaseContent, Set<String> labels) {
        // For very short content (1-2 words), be more flexible with matching

        // First, check if any single word matches our predefined keywords
        String[] words = lowerCaseContent.trim().split("\\s+");

        for (String word : words) {
            word = word.replaceAll("[^a-zA-Z]", "").toLowerCase(); // Clean punctuation

            // Check direct keyword matches
            if (LABEL_KEYWORDS.containsKey(word)) {
                labels.add(LABEL_KEYWORDS.get(word));
            }

            // Check partial matches for common short words
            if (word.length() >= 3) {
                for (Map.Entry<String, String> entry : LABEL_KEYWORDS.entrySet()) {
                    if (entry.getKey().contains(word) || word.contains(entry.getKey())) {
                        labels.add(entry.getValue());
                        break; // Only add one match per word
                    }
                }
            }
        }

        // If still no labels found, try to categorize based on word characteristics
        if (labels.isEmpty()) {
            for (String word : words) {
                word = word.replaceAll("[^a-zA-Z]", "").toLowerCase();
                if (word.length() >= 2) {
                    String category = categorizeShortWord(word);
                    if (category != null) {
                        labels.add(category);
                    }
                }
            }
        }

        // If still no labels and content has some meaning, add "quick-note" category
        if (labels.isEmpty() && lowerCaseContent.trim().length() > 0) {
            labels.add("quick-note");
        }

        return new ArrayList<>(labels);
    }

    private String categorizeShortWord(String word) {
        // Common patterns for very short notes
        if (word.matches(".*\\d.*")) {
            return "numbers"; // Contains digits
        }

        // Common single words that suggest categories
        switch (word) {
            case "call":
            case "phone":
                return "tasks";
            case "buy":
            case "get":
                return "shopping";
            case "eat":
            case "lunch":
            case "dinner":
                return "food";
            case "work":
            case "job":
                return "work";
            case "home":
            case "house":
                return "personal";
            case "car":
            case "drive":
                return "transport";
            case "book":
            case "read":
                return "education";
            case "gym":
            case "exercise":
                return "health";
            default:
                return null; // No specific category
        }
    }

    private void detectPredefinedLabels(String lowerCaseContent, Set<String> labels) {
        // Sort by keyword length (longest first) to catch phrases before individual words
        List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(LABEL_KEYWORDS.entrySet());
        sortedEntries.sort((e1, e2) -> Integer.compare(e2.getKey().length(), e1.getKey().length()));

        for (Map.Entry<String, String> entry : sortedEntries) {
            if (lowerCaseContent.contains(entry.getKey())) {
                labels.add(entry.getValue());
                // Optional: Remove the matched phrase to avoid double-matching
                lowerCaseContent = lowerCaseContent.replace(entry.getKey(), " ");
            }
        }
    }

    private void detectFrequencyBasedLabels(String lowerCaseContent, Set<String> labels) {
        // Remove punctuation but keep spaces
        String cleanedContent = lowerCaseContent.replaceAll("[^a-zA-Z\\s]", " ");

        // Extract words using regex for better matching
        List<String> words = new ArrayList<>();
        java.util.regex.Matcher matcher = WORD_PATTERN.matcher(cleanedContent);
        while (matcher.find()) {
            words.add(matcher.group().toLowerCase());
        }

        // Count word frequencies
        Map<String, Integer> wordFrequencyMap = new HashMap<>();
        for (String word : words) {
            if (isValidWord(word, labels)) {
                wordFrequencyMap.put(word, wordFrequencyMap.getOrDefault(word, 0) + 1);
            }
        }

        // Sort by frequency and word quality
        List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(wordFrequencyMap.entrySet());
        sortedWords.sort((e1, e2) -> {
            // Primary sort: frequency
            int freqCompare = e2.getValue().compareTo(e1.getValue());
            if (freqCompare != 0) return freqCompare;

            // Secondary sort: word length (longer words often more meaningful)
            return Integer.compare(e2.getKey().length(), e1.getKey().length());
        });

        // Add top frequency-based labels
        int added = 0;
        int currentLabelCount = labels.size();

        // If we don't have enough labels yet, be more lenient with frequency requirements
        int minFrequency = (currentLabelCount < MIN_LABELS_REQUIRED) ? 1 : MIN_FREQUENCY;

        for (Map.Entry<String, Integer> entry : sortedWords) {
            if (added >= MAX_FREQUENCY_LABELS) break;

            if (entry.getValue() >= minFrequency) {
                labels.add(entry.getKey());
                added++;

                // Stop early if we've reached a good number of labels
                if (currentLabelCount + added >= MIN_LABELS_REQUIRED + 1) {
                    break;
                }
            }
        }
    }

    private boolean isValidWord(String word, Set<String> existingLabels) {
        return word.length() >= MIN_WORD_LENGTH &&
                !STOP_WORDS.contains(word) &&
                !LABEL_KEYWORDS.containsKey(word) && // Don't duplicate predefined keywords
                !existingLabels.contains(word) && // Avoid duplicates
                isAlphabetic(word); // Only alphabetic characters
    }

    private boolean isAlphabetic(String word) {
        return word.matches("[a-zA-Z]+");
    }

    // Helper method to get labels with confidence scores (optional enhancement)
    public Map<String, Double> getLabelsWithConfidence() {
        Map<String, Double> labelConfidence = new HashMap<>();
        List<String> labels = getTokenizedWords();

        for (String label : labels) {
            if (LABEL_KEYWORDS.containsValue(label)) {
                labelConfidence.put(label, 0.9); // High confidence for predefined labels
            } else {
                labelConfidence.put(label, 0.6); // Medium confidence for frequency-based labels
            }
        }

        return labelConfidence;
    }
}