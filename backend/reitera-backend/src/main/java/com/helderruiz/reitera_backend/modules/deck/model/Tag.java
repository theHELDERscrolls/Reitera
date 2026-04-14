package com.helderruiz.reitera_backend.modules.deck.model;

import com.helderruiz.reitera_backend.modules.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a user-owned label that can be attached to Cards.
 * Tags are scoped per user: two users can share the same tag name independently.
 * Orphan cleanup: tags with no cards are deleted automatically by CardService.
 */
@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false, length = 30)
  private String name;

  @Column(name = "hex_color", length = 7)
  private String hexColor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;
}
