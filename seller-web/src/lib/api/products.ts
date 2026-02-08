import apiClient from "./client";
import type {
  ApiResponse,
  PaginatedResponse,
  Product,
  Category,
  Price,
  CreateProductRequest,
  UpdateProductRequest,
  SetPriceRequest,
} from "@/types";

export async function getProducts(): Promise<ApiResponse<Product[]>> {
  const response = await apiClient.get<ApiResponse<Product[]>>("/seller/products");
  return response.data;
}

export async function getProduct(productId: string): Promise<ApiResponse<Product>> {
  const response = await apiClient.get<ApiResponse<Product>>(
    `/seller/products/${productId}`
  );
  return response.data;
}

export async function createProduct(
  request: CreateProductRequest
): Promise<ApiResponse<Product>> {
  const response = await apiClient.post<ApiResponse<Product>>(
    "/seller/products",
    request
  );
  return response.data;
}

export async function updateProduct(
  productId: string,
  request: UpdateProductRequest
): Promise<ApiResponse<Product>> {
  const response = await apiClient.put<ApiResponse<Product>>(
    `/seller/products/${productId}`,
    request
  );
  return response.data;
}

export async function deleteProduct(productId: string): Promise<ApiResponse<null>> {
  const response = await apiClient.delete<ApiResponse<null>>(
    `/seller/products/${productId}`
  );
  return response.data;
}

export async function setProductPrice(
  productId: string,
  request: SetPriceRequest
): Promise<ApiResponse<Price>> {
  const response = await apiClient.post<ApiResponse<Price>>(
    `/seller/products/${productId}/price`,
    request
  );
  return response.data;
}

export async function getPriceHistory(
  productId: string
): Promise<ApiResponse<Price[]>> {
  const response = await apiClient.get<ApiResponse<Price[]>>(
    `/seller/products/${productId}/prices`
  );
  return response.data;
}

export async function getScheduledPrices(
  productId: string
): Promise<ApiResponse<Price[]>> {
  const response = await apiClient.get<ApiResponse<Price[]>>(
    `/seller/products/${productId}/scheduled-prices`
  );
  return response.data;
}

export async function getCategories(): Promise<ApiResponse<Category[]>> {
  const response = await apiClient.get<ApiResponse<Category[]>>("/categories");
  return response.data;
}

// Product Image Types
export interface ProductImage {
  id: string;
  url: string;
  productId: string;
  displayOrder: number;
  createdAt: string;
}

export interface UploadProductImageResponse {
  id: string;
  url: string;
}

export async function uploadProductImage(
  productId: string,
  file: File,
  onProgress?: (progress: number) => void
): Promise<ApiResponse<UploadProductImageResponse>> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await apiClient.post<ApiResponse<UploadProductImageResponse>>(
    `/seller/products/${productId}/images`,
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          onProgress(progress);
        }
      },
    }
  );
  return response.data;
}

export async function deleteProductImage(
  productId: string,
  imageId: string
): Promise<ApiResponse<null>> {
  const response = await apiClient.delete<ApiResponse<null>>(
    `/seller/products/${productId}/images/${imageId}`
  );
  return response.data;
}

export async function getProductImages(
  productId: string
): Promise<ApiResponse<ProductImage[]>> {
  const response = await apiClient.get<ApiResponse<ProductImage[]>>(
    `/seller/products/${productId}/images`
  );
  return response.data;
}
