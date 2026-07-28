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

export interface StorefrontProduct {
  id: number;
  slug: string;
  name: string;
  description?: string;
  categoryId: number;
  categoryName: string;
  unit: string;
  imageUrl?: string;
  minPrice: number;
  maxPrice: number;
  availableStock: number;
  variantCount: number;
  createdAt: string;
}

export interface ProductCatalogQuery {
  q?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  page: number;
  size: number;
  sortBy: 'createdAt' | 'name' | 'price' | 'stock';
  sortDir: 'ASC' | 'DESC';
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
