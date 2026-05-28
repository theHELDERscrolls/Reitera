export type CardType = 'BASIC' | 'BASIC_REVERSE' | 'CLOZE' | 'MULTIPLE_CHOICE';

export interface CardResponse {
  id: number;
  deckId: number;
  type: CardType;
  question: string;
  answerJson: Record<string, unknown>;
  explanation: string | null;
  state: number | null;
}
