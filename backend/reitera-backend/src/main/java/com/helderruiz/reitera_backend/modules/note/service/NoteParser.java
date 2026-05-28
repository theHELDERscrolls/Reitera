package com.helderruiz.reitera_backend.modules.note.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless parser that converts Markdown note content into card data.
 * Detection priority: CLOZE > MULTIPLE_CHOICE > BASIC_REVERSE > BASIC > UNKNOWN.
 */
@Component
public class NoteParser {

    public enum NoteType {BASIC, BASIC_REVERSE, CLOZE, MULTIPLE_CHOICE, UNKNOWN}

    public record CardData(String question, Map<String, Object> answerJson, int ordinal) {
    }

    private static final Pattern CLOZE_PATTERN = Pattern.compile("\\{\\{c(\\d+)::(.*?)\\}\\}", Pattern.DOTALL);

    public NoteType detectType(String content) {
        if (content == null || content.isBlank()) return NoteType.UNKNOWN;

        List<String> trimmedLines = Arrays.stream(content.split("\n"))
                .map(String::trim)
                .toList();

        if (CLOZE_PATTERN.matcher(content).find()) return NoteType.CLOZE;

        if (content.contains("- [x]") || content.contains("- [ ]")) return NoteType.MULTIPLE_CHOICE;

        if (trimmedLines.contains("---") && trimmedLines.contains("<->")) return NoteType.BASIC_REVERSE;

        if (trimmedLines.contains("---")) return NoteType.BASIC;

        return NoteType.UNKNOWN;
    }

    public List<CardData> parse(String content, NoteType type) {
        return switch (type) {
            case BASIC -> parseBasic(content);
            case BASIC_REVERSE -> parseBasicReverse(content);
            case CLOZE -> parseCloze(content);
            case MULTIPLE_CHOICE -> parseMultipleChoice(content);
            case UNKNOWN -> List.of();
        };
    }

    // BASIC

    private List<CardData> parseBasic(String content) {
        String[] parts = splitOnSeparator(content, "---");

        if (parts == null) return List.of();

        String question = parts[0].trim();
        String answer = parts[1].trim();

        return List.of(new CardData(question, Map.of("answer", answer), 0));
    }

    // BASIC_REVERSE

    private List<CardData> parseBasicReverse(String content) {
        String stripped = content.replaceAll("(?m)^<->\\s*$", "").trim();
        String[] parts = splitOnSeparator(stripped, "---");

        if (parts == null) return List.of();

        String front = parts[0].trim();
        String back = parts[1].trim();

        return List.of(
                new CardData(front, Map.of("answer", back), 0),
                new CardData(back, Map.of("answer", front), 1)
        );
    }

    // CLOZE

    private List<CardData> parseCloze(String content) {
        Map<Integer, List<String>> byIndex = new LinkedHashMap<>();
        Matcher m = CLOZE_PATTERN.matcher(content);

        while (m.find()) {
            int idx = Integer.parseInt(m.group(1));
            String txt = m.group(2).trim();
            byIndex.computeIfAbsent(idx, k -> new ArrayList<>()).add(txt);
        }

        List<Integer> sortedIndices = new ArrayList<>(byIndex.keySet());
        Collections.sort(sortedIndices);

        List<CardData> cards = new ArrayList<>();

        for (int ordinal = 0; ordinal < sortedIndices.size(); ordinal++) {
            int targetIndex = sortedIndices.get(ordinal);
            String answer = String.join(", ", byIndex.get(targetIndex));

            String question = buildClozeQuestion(content, targetIndex);

            cards.add(new CardData(question, Map.of("clozeIndex", targetIndex, "answer", answer), ordinal));
        }

        return cards;
    }

    private String buildClozeQuestion(String content, int targetIndex) {
        StringBuilder result = new StringBuilder();
        Matcher m = CLOZE_PATTERN.matcher(content);
        int lastEnd = 0;

        while (m.find()) {
            result.append(content, lastEnd, m.start());
            int idx = Integer.parseInt(m.group(1));
            result.append(idx == targetIndex ? "[...]" : m.group(2).trim());
            lastEnd = m.end();
        }

        result.append(content, lastEnd, content.length());

        return result.toString().trim();
    }

    // MULTIPLE_CHOICE

    private List<CardData> parseMultipleChoice(String content) {
        List<String> lines = Arrays.stream(content.split("\n"))
                .map(String::trim)
                .toList();

        int firstOption = -1;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("- [")) {
                firstOption = i;
                break;
            }
        }

        if (firstOption < 0) return List.of();

        String question = String.join("\n", lines.subList(0, firstOption)).trim();

        List<String> options = new ArrayList<>();
        int correctIndex = 0;

        for (int i = firstOption; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("- [x] ") || line.startsWith("- [X] ")) {
                correctIndex = options.size();
                options.add(line.substring(6).trim());
            } else if (line.startsWith("- [ ] ")) {
                options.add(line.substring(6).trim());
            }
        }

        if (options.size() < 2) return List.of();

        Map<String, Object> answerJson = new LinkedHashMap<>();

        answerJson.put("options", options);
        answerJson.put("correctIndex", correctIndex);

        return List.of(new CardData(question, answerJson, 0));
    }

    /**
     * Splits content on the first line that equals exactly the separator after trimming.
     * Returns null if the separator is not found or if either part is blank.
     */
    private String[] splitOnSeparator(String content, String separator) {
        String[] lines = content.split("\n");
        int sepLine = -1;

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals(separator)) {
                sepLine = i;
                break;
            }
        }

        if (sepLine < 0) return null;

        String before = String.join("\n", Arrays.copyOfRange(lines, 0, sepLine));
        String after = String.join("\n", Arrays.copyOfRange(lines, sepLine + 1, lines.length));

        if (before.isBlank() || after.isBlank()) return null;

        return new String[]{before, after};
    }
}
