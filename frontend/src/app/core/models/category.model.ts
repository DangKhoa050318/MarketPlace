export interface CategoryResponse {
  id: number;
  name: string;
  code?: string;
  slug: string;
  description?: string;
  parentId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export type Category = CategoryResponse;

export interface CreateCategoryRequest {
  name: string;
  code: string;
  slug: string;
  description?: string;
  parentId?: number;
}

export interface UpdateCategoryRequest {
  name?: string;
  slug?: string;
  description?: string;
  parentId?: number;
}
