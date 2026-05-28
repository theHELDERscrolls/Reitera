export type CardType = 'BASIC' | 'BASIC_REVERSE' | 'CLOZE' | 'MULTIPLE_CHOICE';

export interface CardResponse {
  id: number;
  deckId: number;
  noteId: number;
  type: CardType;
  question: string;
  answerJson: Record<string, unknown>;
  state: number | null;
}
