export interface Product {
  id: string;
  sellerId: string;
  sellerName: string;
  category: Category;
  name: string;
  nameHi?: string;
  sku: string;
  description?: string;
  unit: ProductUnit;
  minOrderQty: number;
  maxOrderQty?: number;
  imageUrls: string[];
  attributes?: Record<string, unknown>;
  status: ProductStatus;
  currentPrice?: Price;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: string;
  code: string;
  name: string;
  nameHi?: string;
  hsnCode?: string;
  description?: string;
  gstRate: number;
}

export interface Price {
  id: string;
  productId: string;
  basePrice: number;
  bulkDiscountSlabs?: BulkDiscountSlab[];
  effectiveFrom: string;
  effectiveTo?: string;
  isCurrentlyActive: boolean;
  isScheduled: boolean;
  createdAt: string;
}

export interface BulkDiscountSlab {
  minQty: number;
  maxQty?: number;
  discountPercent: number;
}

export type ProductUnit = "KG" | "PIECE" | "TRAY" | "BAG" | "BOTTLE" | "BOX";
export type ProductStatus = "ACTIVE" | "INACTIVE" | "OUT_OF_STOCK" | "DELETED";

export interface CreateProductRequest {
  categoryCode: string;
  name: string;
  nameHi?: string;
  sku: string;
  description?: string;
  unit: ProductUnit;
  minOrderQty: number;
  maxOrderQty?: number;
  imageUrls?: string[];
  attributes?: Record<string, unknown>;
  basePrice: number;
  bulkDiscountSlabs?: BulkDiscountSlab[];
}

export interface UpdateProductRequest {
  name?: string;
  nameHi?: string;
  description?: string;
  minOrderQty?: number;
  maxOrderQty?: number;
  imageUrls?: string[];
  attributes?: Record<string, unknown>;
  status?: ProductStatus;
}

export interface SetPriceRequest {
  basePrice: number;
  bulkDiscountSlabs?: BulkDiscountSlab[];
  effectiveFrom?: string;
}
