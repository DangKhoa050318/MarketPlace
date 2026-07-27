export interface ProductResponse {
  id: number;
  name: string;
  slug: string;
  description: string;
  price: number;
  stock: number;
  categoryId: number;
  categoryName: string;
  imageUrl: string;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductRequest {
  name: string;
  slug: string;
  description?: string;
  price: number;
  stock: number;
  categoryId: number;
  imageUrl?: string;
}

export interface UpdateProductRequest {
  name?: string;
  slug?: string;
  description?: string;
  price?: number;
  stock?: number;
  categoryId?: number;
  imageUrl?: string;
  active?: boolean;
}
