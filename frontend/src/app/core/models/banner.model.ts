export type BannerStatus = 'DRAFT' | 'PUBLISHED';

export interface BannerResponse {
  id: number;
  title: string;
  imageUrlDesktop: string;
  imageUrlMobile?: string;
  altText: string;
  targetUrl?: string;
  position: string;
  status: BannerStatus;
  displayOrder: number;
  startsAt?: string;
  endsAt?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateBannerRequest {
  title: string;
  imageUrlDesktop: string;
  imageUrlMobile?: string | null;
  altText: string;
  targetUrl?: string | null;
  position: string;
  displayOrder?: number | null;
  startsAt?: string | null;
  endsAt?: string | null;
}

export type UpdateBannerRequest = CreateBannerRequest;
