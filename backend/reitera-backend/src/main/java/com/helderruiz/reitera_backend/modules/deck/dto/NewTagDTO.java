package com.helderruiz.reitera_backend.modules.deck.dto;

/**
 * Represents a tag the client wants to create on the fly while saving a card.
 * If a tag with the same name already exists for this user, it is reused (find-or-create).
 */
public record NewTagDTO(String name, String hexColor) {}
