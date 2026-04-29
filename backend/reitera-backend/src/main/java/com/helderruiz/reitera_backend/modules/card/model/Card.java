package com.helderruiz.reitera_backend.modules.card.model;

import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.model.Tag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Column(nullable = false, length = 50)
    private String type; // e.g., "BASIC", "MULTIPLE_CHOICE", "TRUE_FALSE"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    /**
     * Native PostgreSQL JSONB mapping.
     * Allows storing dynamic key-value pairs for different card types.
     * Hibernate 6 automatically serializes/deserializes this Java Map into a Postgres JSONB column.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> answerJson;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    /**
     * Many-to-Many relationship with Tags.
     * Allows cross-deck studying by grouping cards through shared tags.
     * Hibernate will automatically manage the intermediate 'card_tags' table.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "card_tags",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new java.util.HashSet<>();
}