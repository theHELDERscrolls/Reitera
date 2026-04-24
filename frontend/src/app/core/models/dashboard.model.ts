export interface DashboardStats {
  streak: number;
  totalDueToday: number;
  studiedToday: number;
}

export interface LastStudiedDeck {
  deckId: number;
  name: string;
  category: string | null;
  newCount: number;
  dueCount: number;
  relearningCount: number;
  lastStudied: string;
}

export interface DailyStudyCount {
  date: string;
  count: number;
}
