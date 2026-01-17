"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ArrowLeft, Loader2, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Header } from "@/components/layout/header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import * as productsApi from "@/lib/api/products";
import type { ProductUnit, ProductStatus } from "@/types";

const productUnits: { value: ProductUnit; label: string }[] = [
  { value: "KG", label: "Kilogram (KG)" },
  { value: "PIECE", label: "Piece" },
  { value: "TRAY", label: "Tray" },
  { value: "BAG", label: "Bag" },
  { value: "BOTTLE", label: "Bottle" },
  { value: "BOX", label: "Box" },
];

const productStatuses: { value: ProductStatus; label: string }[] = [
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
  { value: "OUT_OF_STOCK", label: "Out of Stock" },
];

const productSchema = z.object({
  name: z.string().min(1, "Product name is required").max(255),
  nameHi: z.string().max(255).optional(),
  description: z.string().max(2000).optional(),
  minOrderQty: z.number().min(0.001, "Min order qty must be greater than 0"),
  maxOrderQty: z.number().min(0.001).optional(),
  status: z.enum(["ACTIVE", "INACTIVE", "OUT_OF_STOCK", "DELETED"]),
});

const priceSchema = z.object({
  basePrice: z.number().min(0.01, "Price must be greater than 0"),
});

type ProductFormData = z.infer<typeof productSchema>;
type PriceFormData = z.infer<typeof priceSchema>;

interface BulkDiscount {
  minQty: number;
  maxQty?: number;
  discountPercent: number;
}

export default function EditProductPage() {
  const router = useRouter();
  const params = useParams();
  const productId = params.productId as string;
  const queryClient = useQueryClient();

  const [bulkDiscounts, setBulkDiscounts] = useState<BulkDiscount[]>([]);
  const [newDiscount, setNewDiscount] = useState<BulkDiscount>({
    minQty: 10,
    discountPercent: 5,
  });

  const {
    data: product,
    isLoading: isProductLoading,
    error: productError,
  } = useQuery({
    queryKey: ["product", productId],
    queryFn: async () => {
      const response = await productsApi.getProduct(productId);
      if (response.success && response.data) {
        return response.data;
      }
      throw new Error(response.message || "Failed to load product");
    },
    enabled: !!productId,
  });

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<ProductFormData>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: "",
      nameHi: "",
      description: "",
      minOrderQty: 1,
      status: "ACTIVE",
    },
  });

  const {
    register: registerPrice,
    handleSubmit: handleSubmitPrice,
    setValue: setValuePrice,
    formState: { errors: priceErrors },
  } = useForm<PriceFormData>({
    resolver: zodResolver(priceSchema),
    defaultValues: {
      basePrice: 0,
    },
  });

  const selectedStatus = watch("status");

  useEffect(() => {
    if (product) {
      reset({
        name: product.name,
        nameHi: product.nameHi || "",
        description: product.description || "",
        minOrderQty: product.minOrderQty,
        maxOrderQty: product.maxOrderQty,
        status: product.status,
      });

      if (product.currentPrice) {
        setValuePrice("basePrice", product.currentPrice.basePrice);
        if (product.currentPrice.bulkDiscountSlabs) {
          setBulkDiscounts(product.currentPrice.bulkDiscountSlabs);
        }
      }
    }
  }, [product, reset, setValuePrice]);

  const updateMutation = useMutation({
    mutationFn: async (data: ProductFormData) => {
      return productsApi.updateProduct(productId, {
        name: data.name,
        nameHi: data.nameHi || undefined,
        description: data.description || undefined,
        minOrderQty: data.minOrderQty,
        maxOrderQty: data.maxOrderQty || undefined,
        status: data.status,
      });
    },
    onSuccess: (response) => {
      if (response.success) {
        queryClient.invalidateQueries({ queryKey: ["seller-products"] });
        queryClient.invalidateQueries({ queryKey: ["product", productId] });
        toast.success("Product updated successfully");
        router.push("/products");
      } else {
        toast.error(response.message || "Failed to update product");
      }
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to update product");
    },
  });

  const updatePriceMutation = useMutation({
    mutationFn: async (data: PriceFormData) => {
      return productsApi.setProductPrice(productId, {
        basePrice: data.basePrice,
        bulkDiscountSlabs: bulkDiscounts.length > 0 ? bulkDiscounts : undefined,
      });
    },
    onSuccess: (response) => {
      if (response.success) {
        queryClient.invalidateQueries({ queryKey: ["seller-products"] });
        queryClient.invalidateQueries({ queryKey: ["product", productId] });
        toast.success("Price updated successfully");
      } else {
        toast.error(response.message || "Failed to update price");
      }
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to update price");
    },
  });

  const onSubmit = (data: ProductFormData) => {
    updateMutation.mutate(data);
  };

  const onSubmitPrice = (data: PriceFormData) => {
    updatePriceMutation.mutate(data);
  };

  const addBulkDiscount = () => {
    if (newDiscount.minQty > 0 && newDiscount.discountPercent >= 0) {
      setBulkDiscounts([...bulkDiscounts, { ...newDiscount }]);
      setNewDiscount({ minQty: 10, discountPercent: 5 });
    }
  };

  const removeBulkDiscount = (index: number) => {
    setBulkDiscounts(bulkDiscounts.filter((_, i) => i !== index));
  };

  if (isProductLoading) {
    return (
      <div>
        <Header title="Edit Product" />
        <div className="flex items-center justify-center py-24">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      </div>
    );
  }

  if (productError || !product) {
    return (
      <div>
        <Header title="Edit Product" />
        <div className="p-6">
          <div className="mb-6">
            <Link href="/products">
              <Button variant="ghost" size="sm">
                <ArrowLeft className="mr-2 h-4 w-4" />
                Back to Products
              </Button>
            </Link>
          </div>
          <Card>
            <CardContent className="p-8 text-center text-destructive">
              {productError instanceof Error
                ? productError.message
                : "Product not found"}
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div>
      <Header title="Edit Product" />

      <div className="p-6">
        <div className="mb-6">
          <Link href="/products">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to Products
            </Button>
          </Link>
        </div>

        <div className="space-y-6 max-w-2xl">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Basic Information</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label>Category</Label>
                  <Input
                    value={product.category?.name || "Unknown"}
                    disabled
                    className="bg-muted"
                  />
                  <p className="text-xs text-muted-foreground">
                    Category cannot be changed after product creation.
                  </p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="name">Product Name *</Label>
                  <Input
                    id="name"
                    {...register("name")}
                    placeholder="e.g., Farm Fresh Eggs"
                  />
                  {errors.name && (
                    <p className="text-sm text-destructive">
                      {errors.name.message}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="nameHi">Product Name (Hindi)</Label>
                  <Input
                    id="nameHi"
                    {...register("nameHi")}
                    placeholder="e.g., ताजे अंडे"
                  />
                </div>

                <div className="space-y-2">
                  <Label>SKU</Label>
                  <Input value={product.sku} disabled className="bg-muted" />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="description">Description</Label>
                  <Textarea
                    id="description"
                    {...register("description")}
                    placeholder="Enter product description..."
                    rows={3}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="status">Status</Label>
                  <Select
                    value={selectedStatus}
                    onValueChange={(value) =>
                      setValue("status", value as ProductStatus)
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select status" />
                    </SelectTrigger>
                    <SelectContent>
                      {productStatuses.map((status) => (
                        <SelectItem key={status.value} value={status.value}>
                          {status.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Quantity Settings</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label>Unit</Label>
                  <Input
                    value={
                      productUnits.find((u) => u.value === product.unit)
                        ?.label || product.unit
                    }
                    disabled
                    className="bg-muted"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="minOrderQty">Min Order Qty *</Label>
                    <Input
                      id="minOrderQty"
                      type="number"
                      step="0.001"
                      {...register("minOrderQty", { valueAsNumber: true })}
                    />
                    {errors.minOrderQty && (
                      <p className="text-sm text-destructive">
                        {errors.minOrderQty.message}
                      </p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="maxOrderQty">Max Order Qty</Label>
                    <Input
                      id="maxOrderQty"
                      type="number"
                      step="0.001"
                      {...register("maxOrderQty", { valueAsNumber: true })}
                    />
                  </div>
                </div>
              </CardContent>
            </Card>

            <div className="flex gap-4">
              <Button type="submit" disabled={updateMutation.isPending}>
                {updateMutation.isPending && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                Save Product Details
              </Button>
              <Link href="/products">
                <Button type="button" variant="outline">
                  Cancel
                </Button>
              </Link>
            </div>
          </form>

          <form onSubmit={handleSubmitPrice(onSubmitPrice)} className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Pricing</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="basePrice">Base Price (INR) *</Label>
                  <Input
                    id="basePrice"
                    type="number"
                    step="0.01"
                    {...registerPrice("basePrice", { valueAsNumber: true })}
                    placeholder="0.00"
                  />
                  {priceErrors.basePrice && (
                    <p className="text-sm text-destructive">
                      {priceErrors.basePrice.message}
                    </p>
                  )}
                </div>

                <div className="space-y-4 pt-4 border-t">
                  <Label>Bulk Discounts</Label>

                  {bulkDiscounts.length > 0 && (
                    <div className="space-y-2">
                      {bulkDiscounts.map((discount, index) => (
                        <div
                          key={index}
                          className="flex items-center justify-between rounded-md border p-3"
                        >
                          <span className="text-sm">
                            {discount.minQty}+ units: {discount.discountPercent}%
                            off
                          </span>
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            onClick={() => removeBulkDiscount(index)}
                          >
                            <Trash2 className="h-4 w-4 text-destructive" />
                          </Button>
                        </div>
                      ))}
                    </div>
                  )}

                  <div className="flex items-end gap-2">
                    <div className="space-y-2">
                      <Label>Min Qty</Label>
                      <Input
                        type="number"
                        value={newDiscount.minQty}
                        onChange={(e) =>
                          setNewDiscount({
                            ...newDiscount,
                            minQty: Number(e.target.value),
                          })
                        }
                        className="w-24"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Discount %</Label>
                      <Input
                        type="number"
                        value={newDiscount.discountPercent}
                        onChange={(e) =>
                          setNewDiscount({
                            ...newDiscount,
                            discountPercent: Number(e.target.value),
                          })
                        }
                        className="w-24"
                      />
                    </div>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={addBulkDiscount}
                    >
                      <Plus className="mr-1 h-4 w-4" />
                      Add
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Button type="submit" disabled={updatePriceMutation.isPending}>
              {updatePriceMutation.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Update Pricing
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
