export type PublishStatus = 'DRAFT' | 'PUBLISHED';

export interface CollectionItemResponse {
  productId: number;
  displayOrder: number;
  productName?: string;
  productSlug?: string;
  imageUrl?: string;
}

export interface CollectionResponse {
  id: number;
  name: string;
  slug: string;
  description?: string;
  status: PublishStatus;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
  items: CollectionItemResponse[];
}

export interface CreateCollectionRequest {
  name: string;
  slug: string;
  description?: string | null;
}

export type UpdateCollectionRequest = CreateCollectionRequest;

export interface AddCollectionItemRequest {
  productId: number;
}

export interface ReorderCollectionItemsRequest {
  productIds: number[];
}
