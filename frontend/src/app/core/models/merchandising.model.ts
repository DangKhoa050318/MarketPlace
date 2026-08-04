export type MerchandisingEventType = 'IMPRESSION' | 'CLICK';
export type MerchandisingTargetType = 'CAMPAIGN' | 'COLLECTION' | 'BANNER';

export interface RecordMerchandisingEventRequest {
  eventId: string;
  eventType: MerchandisingEventType;
  targetType: MerchandisingTargetType;
  targetId: number;
  sessionId?: string | null;
}

export interface MerchandisingSummaryResponse {
  targetType: MerchandisingTargetType;
  targetId: number;
  impressions: number;
  clicks: number;
  ctr: number;
  attributedOrders: number;
}
