"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Search, Pencil, Trash2, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Header } from "@/components/layout/header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { formatCurrency } from "@/lib/utils";
import * as productsApi from "@/lib/api/products";
import type { Product, ProductStatus } from "@/types";

const statusColors: Record<ProductStatus, string> = {
  ACTIVE: "bg-green-100 text-green-800",
  INACTIVE: "bg-gray-100 text-gray-800",
  OUT_OF_STOCK: "bg-yellow-100 text-yellow-800",
  DELETED: "bg-red-100 text-red-800",
};

export default function ProductsPage() {
  const [search, setSearch] = useState("");
  const queryClient = useQueryClient();

  // Fetch products
  const {
    data: products,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["seller-products"],
    queryFn: async () => {
      const response = await productsApi.getProducts();
      if (response.success && response.data) {
        return response.data;
      }
      throw new Error(response.message || "Failed to load products");
    },
  });

  // Delete product mutation
  const deleteMutation = useMutation({
    mutationFn: (productId: string) => productsApi.deleteProduct(productId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-products"] });
      toast.success("Product deleted successfully");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to delete product");
    },
  });

  const handleDelete = (productId: string, productName: string) => {
    if (confirm(`Are you sure you want to delete "${productName}"?`)) {
      deleteMutation.mutate(productId);
    }
  };

  const filteredProducts = products?.filter(
    (p) =>
      p.name.toLowerCase().includes(search.toLowerCase()) ||
      p.sku.toLowerCase().includes(search.toLowerCase())
  ) ?? [];

  return (
    <div>
      <Header title="Products" />

      <div className="p-6 space-y-6">
        {/* Actions bar */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search products..."
              className="pl-10"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <Link href="/products/new">
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              Add Product
            </Button>
          </Link>
        </div>

        {/* Loading state */}
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {/* Error state */}
        {error && (
          <Card>
            <CardContent className="p-8 text-center text-destructive">
              {error instanceof Error ? error.message : "Failed to load products"}
            </CardContent>
          </Card>
        )}

        {/* Products table */}
        {!isLoading && !error && (
          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b bg-muted/50">
                      <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                        Product
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                        SKU
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                        Category
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                        Price
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                        Status
                      </th>
                      <th className="px-4 py-3 text-right text-sm font-medium text-muted-foreground">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredProducts.map((product) => (
                      <tr key={product.id} className="border-b">
                        <td className="px-4 py-3">
                          <div className="font-medium">{product.name}</div>
                          {product.nameHi && (
                            <div className="text-sm text-muted-foreground">
                              {product.nameHi}
                            </div>
                          )}
                        </td>
                        <td className="px-4 py-3 text-sm text-muted-foreground">
                          {product.sku}
                        </td>
                        <td className="px-4 py-3 text-sm">
                          {product.category?.name || "—"}
                        </td>
                        <td className="px-4 py-3 text-sm">
                          {product.currentPrice
                            ? `${formatCurrency(product.currentPrice.basePrice)}/${product.unit}`
                            : "Not set"}
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                              statusColors[product.status] || statusColors.ACTIVE
                            }`}
                          >
                            {product.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <Link href={`/products/${product.id}/edit`}>
                              <Button variant="ghost" size="icon">
                                <Pencil className="h-4 w-4" />
                              </Button>
                            </Link>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => handleDelete(product.id, product.name)}
                              disabled={deleteMutation.isPending}
                            >
                              <Trash2 className="h-4 w-4 text-destructive" />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {filteredProducts.length === 0 && !isLoading && (
                <div className="p-8 text-center text-muted-foreground">
                  {search
                    ? "No products found matching your search."
                    : "No products found. Add your first product to get started."}
                </div>
              )}
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
