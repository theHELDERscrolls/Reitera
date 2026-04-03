export interface DeckRequest {
  title: string;
  description: string;
  isPublic: boolean;
  categoryId: number | null;
  categoryName: string | null;
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
