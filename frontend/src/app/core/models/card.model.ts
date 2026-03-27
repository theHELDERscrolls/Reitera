export type CardType = 'BASIC' | 'CLOZE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE';

export interface TagSummary {
  id: number;
  name: string;
  hexColor: string;
}

export interface CardRequest {
  type: CardType;
  question: string;
  answerJson: Record<string, unknown>;
  explanation: string | null;
  tagIds: number[];
}

export interface CardResponse {
  id: number;
  deckId: number;
  type: CardType;
  question: string;
  answerJson: Record<string, unknown>;
  explanation: string | null;
  tags: TagSummary[];
}
