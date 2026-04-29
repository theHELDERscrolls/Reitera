package com.helderruiz.reitera_backend.modules.deck.controller;

import com.helderruiz.reitera_backend.modules.deck.dto.CategoryResponseDTO;
import com.helderruiz.reitera_backend.modules.deck.service.CategoryService;
import com.helderruiz.reitera_backend.modules.user.model.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing read-only endpoints for Category data.
 * Returns only the categories used across the authenticated user's own decks.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Returns the distinct categories used by the authenticated user's decks.
     * GET /api/v1/categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getMyCategories(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(categoryService.getMyCategories(user));
    }
}
