import { ContentType } from './vote.model';
import { ModerationStatus } from './question.model';

export interface ModerationItem {
  id: number;
  targetType: ContentType;
  targetId: number;
  productId: number;
  productName: string;
  authorId: number;
  authorName: string;
  content: string;
  status: ModerationStatus;
  createdAt: string;
}

export interface UpdateModerationStatusPayload {
  targetType: ContentType;
  targetId: number;
  newStatus: ModerationStatus;
  reason?: string;
}

export interface ModerationAuditLog {
  id: number;
  targetType: ContentType;
  targetId: number;
  moderatorId: number;
  moderatorName: string;
  oldStatus?: ModerationStatus;
  newStatus: ModerationStatus;
  reason?: string;
  createdAt: string;
}

export interface ModerationFilter {
  targetType?: ContentType;
  status?: ModerationStatus;
  productId?: number;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}
