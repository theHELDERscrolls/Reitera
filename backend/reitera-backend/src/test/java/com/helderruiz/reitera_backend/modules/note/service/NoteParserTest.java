package com.helderruiz.reitera_backend.modules.note.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.helderruiz.reitera_backend.modules.note.service.NoteParser.NoteType.*;
import static org.assertj.core.api.Assertions.assertThat;

class NoteParserTest {

    private NoteParser parser;

    @BeforeEach
    void setUp() {
        parser = new NoteParser();
    }

    @Test
    void detectType_nullContent_returnsUnknown() {
        assertThat(parser.detectType(null)).isEqualTo(UNKNOWN);
    }

    @Test
    void detectType_blankContent_returnsUnknown() {
        assertThat(parser.detectType("   ")).isEqualTo(UNKNOWN);
    }

    @Test
    void detectType_separatorOnly_returnsBasic() {
        assertThat(parser.detectType("What year?\n\n---\n\n1936")).isEqualTo(BASIC);
    }

    @Test
    void detectType_separatorWithReverseLine_returnsBasicReverse() {
        assertThat(parser.detectType("Term\n\n---\n\nDefinition\n\n<->")).isEqualTo(BASIC_REVERSE);
    }

    @Test
    void detectType_clozePattern_returnsCloze() {
        assertThat(parser.detectType("The war ended in {{c1::1939}}.")).isEqualTo(CLOZE);
    }

    @Test
    void detectType_mcMarkers_returnsMultipleChoice() {
        assertThat(parser.detectType("Which side won?\n\n- [x] Bando Nacional\n- [ ] Republicanos")).isEqualTo(MULTIPLE_CHOICE);
    }

    @Test
    void detectType_noMarkers_returnsUnknown() {
        assertThat(parser.detectType("Just plain text with no markers")).isEqualTo(UNKNOWN);
    }

    @Test
    void detectType_clozeHasPriorityOverMultipleChoice() {
        String content = "{{c1::answer}}\n- [x] Option A\n- [ ] Option B";
        assertThat(parser.detectType(content)).isEqualTo(CLOZE);
    }

    @Test
    void hasConflict_basicType_alwaysReturnsFalse() {
        assertThat(parser.hasConflict("Q\n\n---\n\nA", BASIC)).isFalse();
    }

    @Test
    void hasConflict_basicReverseType_alwaysReturnsFalse() {
        assertThat(parser.hasConflict("Q\n\n---\n\nA\n\n<->", BASIC_REVERSE)).isFalse();
    }

    @Test
    void hasConflict_cleanCloze_returnsFalse() {
        assertThat(parser.hasConflict("The war ended in {{c1::1939}}.", CLOZE)).isFalse();
    }

    @Test
    void hasConflict_clozeWithMcMarkers_returnsTrue() {
        assertThat(parser.hasConflict("{{c1::answer}}\n- [x] Option", CLOZE)).isTrue();
    }

    @Test
    void hasConflict_clozeWithSeparator_returnsTrue() {
        assertThat(parser.hasConflict("{{c1::answer}}\n\n---\n\nother", CLOZE)).isTrue();
    }

    @Test
    void hasConflict_cleanMultipleChoice_returnsFalse() {
        assertThat(parser.hasConflict("Which side won?\n\n- [x] A\n- [ ] B", MULTIPLE_CHOICE)).isFalse();
    }

    @Test
    void hasConflict_multipleChoiceWithSeparator_returnsTrue() {
        assertThat(parser.hasConflict("Question\n\n---\n\n- [x] A\n- [ ] B", MULTIPLE_CHOICE)).isTrue();
    }

    @Test
    void parse_basicNote_returnsOneCardWithQuestionAndAnswer() {
        String content = "What year did the war begin?\n\n---\n\n1936";
        List<NoteParser.CardData> cards = parser.parse(content, BASIC);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().question()).isEqualTo("What year did the war begin?");
        assertThat(cards.getFirst().answerJson()).containsEntry("answer", "1936");
        assertThat(cards.getFirst().ordinal()).isEqualTo(0);
    }

    @Test
    void parse_basicReverseNote_returnsTwoCardsWithSwappedSides() {
        String content = "Front\n\n---\n\nBack\n\n<->";
        List<NoteParser.CardData> cards = parser.parse(content, BASIC_REVERSE);

        assertThat(cards).hasSize(2);
        assertThat(cards.get(0).question()).isEqualTo("Front");
        assertThat(cards.get(0).answerJson()).containsEntry("answer", "Back");
        assertThat(cards.get(1).question()).isEqualTo("Back");
        assertThat(cards.get(1).answerJson()).containsEntry("answer", "Front");
    }

    @Test
    void parse_clozeWithTwoDeletions_returnsTwoCards() {
        String content = "The war began in {{c1::1936}} and ended in {{c2::1939}}.";
        List<NoteParser.CardData> cards = parser.parse(content, CLOZE);

        assertThat(cards).hasSize(2);
        assertThat(cards.get(0).answerJson()).containsEntry("answer", "1936");
        assertThat(cards.get(0).question()).contains("[...]").contains("1939");
        assertThat(cards.get(1).answerJson()).containsEntry("answer", "1939");
        assertThat(cards.get(1).question()).contains("1936").contains("[...]");
    }

    @Test
    void parse_clozeWithRepeatedIndex_combinesAnswersWithComma() {
        String content = "{{c1::Madrid}} is the capital of {{c1::Spain}}.";
        List<NoteParser.CardData> cards = parser.parse(content, CLOZE);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().answerJson().get("answer").toString())
                .contains("Madrid")
                .contains("Spain");
    }

    @Test
    void parse_multipleChoiceNote_returnsOneCardWithOptionsAndCorrectIndex() {
        String content = "Which side won?\n\n- [x] Bando Nacional\n- [ ] Bando Republicano\n- [ ] Neither";
        List<NoteParser.CardData> cards = parser.parse(content, MULTIPLE_CHOICE);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().question()).isEqualTo("Which side won?");

        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) cards.getFirst().answerJson().get("options");
        assertThat(options).containsExactly("Bando Nacional", "Bando Republicano", "Neither");
        assertThat(cards.getFirst().answerJson()).containsEntry("correctIndex", 0);
    }

    @Test
    void parse_unknownType_returnsEmptyList() {
        assertThat(parser.parse("plain text", UNKNOWN)).isEmpty();
    }
}
