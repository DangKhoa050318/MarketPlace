export interface ProductVariant {
  id: number;
  productId: number;
  sku: string;
  variantName: string;
  color?: string;
  size?: string;
  price: number;
  imageUrl?: string;
  minStock?: number;
  maxStock?: number;
  reorderPoint?: number;
  reorderQuantity?: number;
  active: boolean;
  version?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductResponse {
  id: number;
  slug: string;
  name: string;
  description?: string;
  price?: number;
  stock?: number;
  categoryId: number;
  categoryName?: string;
  unit?: string;
  imageUrl?: string;
  active: boolean;
  version?: number;
  createdAt?: string;
  updatedAt?: string;
  variants?: ProductVariant[];
}

export interface CreateProductRequest {
  name: string;
  slug?: string;
  description?: string;
  price?: number;
  stock?: number;
  categoryId: number;
  unit?: string;
  imageUrl?: string;
}

export interface UpdateProductRequest {
  name?: string;
  slug?: string;
  description?: string;
  price?: number;
  stock?: number;
  categoryId?: number;
  unit?: string;
  imageUrl?: string;
  active?: boolean;
}

export interface CreateProductVariantRequest {
  sku: string;
  variantName: string;
  color?: string;
  size?: string;
  price: number;
  imageUrl?: string;
  minStock?: number;
  maxStock?: number;
  reorderPoint?: number;
  reorderQuantity?: number;
}

export interface UpdateProductVariantRequest {
  sku?: string;
  variantName?: string;
  color?: string;
  size?: string;
  price?: number;
  imageUrl?: string;
  minStock?: number;
  maxStock?: number;
  reorderPoint?: number;
  reorderQuantity?: number;
  active?: boolean;
}
