import { Tag } from './tag.model';

export type CardType = 'BASIC' | 'CLOZE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE';

export interface CardRequest {
  type: CardType;
  question: string;
  answerJson: Record<string, unknown>;
  explanation: string | null;
  tagIds: number[];
  newTags?: { name: string; hexColor: string }[];
}

export interface CardResponse {
  id: number;
  deckId: number;
  type: CardType;
  question: string;
  answerJson: Record<string, unknown>;
  explanation: string | null;
  tags: Tag[];
  state: number | null;
}
