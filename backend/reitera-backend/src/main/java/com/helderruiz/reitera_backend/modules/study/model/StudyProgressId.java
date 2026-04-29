package com.helderruiz.reitera_backend.modules.study.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite Primary Key for the StudyProgress entity.
 * Ensures a User can only have one unique progress record per Card.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyProgressId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "card_id")
    private Integer cardId;
}