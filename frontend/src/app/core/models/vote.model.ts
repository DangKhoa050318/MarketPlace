export type ContentType = 'REVIEW' | 'QUESTION' | 'ANSWER';

export interface VotePayload {
  targetType: ContentType;
  targetId: number;
}

export interface VoteResponse {
  targetType: ContentType;
  targetId: number;
  helpfulCount: number;
  isVoted: boolean;
}
