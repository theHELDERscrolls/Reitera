export interface DeckRequest {
  title: string;
  description: string;
  isPublic: boolean;
  categoryId: number | null;
  categoryName: string | null;
}

export interface DeckStats {
  totalCards: number;
  newCards: number;
  learningCards: number;
  reviewCards: number;
  relearningCards: number;
  dueCards: number;
}

export interface DeckResponse {
  id: number;
  title: string;
  description: string;
  isPublic: boolean;
  authorName: string;
  categoryId: number | null;
  categoryName: string | null;
  createdAt: string;
  updatedAt: string;
}
