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
