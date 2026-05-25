export type CardType = 'BASIC' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE';

export interface CardRequest {
  type: CardType;
  question: string;
  answerJson: Record<string, unknown>;
  explanation: string | null;
}

export interface CardResponse {
  id: number;
  deckId: number;
  type: CardType;
  question: string;
  answerJson: Record<string, unknown>;
  explanation: string | null;
  state: number | null;
}
