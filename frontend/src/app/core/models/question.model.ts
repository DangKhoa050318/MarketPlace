export type ModerationStatus = 'VISIBLE' | 'HIDDEN' | 'PENDING_REVIEW' | 'REJECTED';

export interface ProductAnswer {
  id: number;
  questionId: number;
  userId: number;
  userName: string;
  content: string;
  isOfficial: boolean;
  status: ModerationStatus;
  helpfulCount: number;
  isVotedByCurrentUser?: boolean;
  createdAt: string;
}

export interface ProductQuestion {
  id: number;
  productId: number;
  userId: number;
  userName: string;
  content: string;
  status: ModerationStatus;
  helpfulCount: number;
  isVotedByCurrentUser?: boolean;
  answers: ProductAnswer[];
  createdAt: string;
}

export interface CreateQuestionPayload {
  content: string;
}

export interface CreateAnswerPayload {
  content: string;
}
