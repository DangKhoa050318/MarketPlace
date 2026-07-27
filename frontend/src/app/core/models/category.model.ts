export interface CategoryResponse {
  id: number;
  name: string;
  slug: string;
  createdAt: string;
  updatedAt: string;
}

export type Category = CategoryResponse;

export interface CreateCategoryRequest {
  name: string;
  slug: string;
}

export interface UpdateCategoryRequest {
  name?: string;
  slug?: string;
}
