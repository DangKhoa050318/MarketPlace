export type CampaignStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface CampaignResponse {
  id: number;
  name: string;
  description?: string;
  status: CampaignStatus;
  startsAt?: string;
  endsAt?: string;
  promotionCodeId?: number;
  couponCode?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateCampaignRequest {
  name: string;
  description?: string | null;
  startsAt?: string | null;
  endsAt?: string | null;
  promotionCodeId?: number | null;
}

export type UpdateCampaignRequest = CreateCampaignRequest;
