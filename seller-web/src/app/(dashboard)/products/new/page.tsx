"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation } from "@tanstack/react-query";
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
import type { ProductUnit } from "@/types";

const productUnits: { value: ProductUnit; label: string }[] = [
  { value: "KG", label: "Kilogram (KG)" },
  { value: "PIECE", label: "Piece" },
  { value: "TRAY", label: "Tray" },
  { value: "BAG", label: "Bag" },
  { value: "BOTTLE", label: "Bottle" },
  { value: "BOX", label: "Box" },
];

const bulkDiscountSchema = z.object({
  minQty: z.coerce.number().min(1, "Min qty must be at least 1"),
  maxQty: z.coerce.number().optional(),
  discountPercent: z.coerce.number().min(0).max(100, "Discount must be 0-100%"),
});

const productSchema = z.object({
  categoryCode: z.string().min(1, "Category is required"),
  name: z.string().min(1, "Product name is required").max(255),
  nameHi: z.string().max(255).optional(),
  sku: z
    .string()
    .min(1, "SKU is required")
    .max(50)
    .regex(
      /^[A-Za-z0-9-_]+$/,
      "SKU can only contain letters, numbers, hyphens and underscores"
    ),
  description: z.string().max(2000).optional(),
  unit: z.enum(["KG", "PIECE", "TRAY", "BAG", "BOTTLE", "BOX"]),
  minOrderQty: z.coerce.number().min(0.001, "Min order qty must be greater than 0"),
  maxOrderQty: z.coerce.number().min(0.001).optional(),
  basePrice: z.coerce.number().min(0.01, "Price must be greater than 0"),
});

type ProductFormData = z.infer<typeof productSchema>;

interface BulkDiscount {
  minQty: number;
  maxQty?: number;
  discountPercent: number;
}

export default function CreateProductPage() {
  const router = useRouter();
  const [bulkDiscounts, setBulkDiscounts] = useState<BulkDiscount[]>([]);
  const [newDiscount, setNewDiscount] = useState<BulkDiscount>({
    minQty: 10,
    discountPercent: 5,
  });

  // Fetch categories
  const { data: categoriesResponse, isLoading: isCategoriesLoading } = useQuery({
    queryKey: ["categories"],
    queryFn: async () => {
      const response = await productsApi.getCategories();
      if (response.success && response.data) {
        return response.data;
      }
      return [];
    },
  });

  const categories = categoriesResponse ?? [];

  // Form setup
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<ProductFormData>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      unit: "KG",
      minOrderQty: 1,
      basePrice: 0,
    },
  });

  const selectedUnit = watch("unit");

  // Create product mutation
  const createMutation = useMutation({
    mutationFn: async (data: ProductFormData) => {
      const request = {
        ...data,
        bulkDiscountSlabs:
          bulkDiscounts.length > 0
            ? bulkDiscounts.map((d) => ({
                minQty: d.minQty,
                maxQty: d.maxQty,
                discountPercent: d.discountPercent,
              }))
            : undefined,
      };
      return productsApi.createProduct(request);
    },
    onSuccess: (response) => {
      if (response.success) {
        toast.success("Product created successfully");
        router.push("/products");
      } else {
        toast.error(response.message || "Failed to create product");
      }
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to create product");
    },
  });

  const onSubmit = (data: ProductFormData) => {
    createMutation.mutate(data);
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

  return (
    <div>
      <Header title="Create Product" />

      <div className="p-6">
        <div className="mb-6">
          <Link href="/products">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to Products
            </Button>
          </Link>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6 max-w-2xl">
          {/* Basic Information */}
          <Card>
            <CardHeader>
              <CardTitle>Basic Information</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Category */}
              <div className="space-y-2">
                <Label htmlFor="categoryCode">Category *</Label>
                <Select
                  onValueChange={(value) => setValue("categoryCode", value)}
                  disabled={isCategoriesLoading}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select a category" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((cat) => (
                      <SelectItem key={cat.id} value={cat.code}>
                        {cat.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.categoryCode && (
                  <p className="text-sm text-destructive">
                    {errors.categoryCode.message}
                  </p>
                )}
              </div>

              {/* Product Name */}
              <div className="space-y-2">
                <Label htmlFor="name">Product Name *</Label>
                <Input id="name" {...register("name")} placeholder="e.g., Farm Fresh Eggs" />
                {errors.name && (
                  <p className="text-sm text-destructive">{errors.name.message}</p>
                )}
              </div>

              {/* Hindi Name */}
              <div className="space-y-2">
                <Label htmlFor="nameHi">Product Name (Hindi)</Label>
                <Input
                  id="nameHi"
                  {...register("nameHi")}
                  placeholder="e.g., ताजे अंडे"
                />
              </div>

              {/* SKU */}
              <div className="space-y-2">
                <Label htmlFor="sku">SKU *</Label>
                <Input
                  id="sku"
                  {...register("sku")}
                  placeholder="e.g., EGG-FARM-001"
                />
                {errors.sku && (
                  <p className="text-sm text-destructive">{errors.sku.message}</p>
                )}
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Textarea
                  id="description"
                  {...register("description")}
                  placeholder="Enter product description..."
                  rows={3}
                />
              </div>
            </CardContent>
          </Card>

          {/* Quantity & Pricing */}
          <Card>
            <CardHeader>
              <CardTitle>Quantity & Pricing</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Unit */}
              <div className="space-y-2">
                <Label htmlFor="unit">Unit *</Label>
                <Select
                  value={selectedUnit}
                  onValueChange={(value) => setValue("unit", value as ProductUnit)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select unit" />
                  </SelectTrigger>
                  <SelectContent>
                    {productUnits.map((unit) => (
                      <SelectItem key={unit.value} value={unit.value}>
                        {unit.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="grid grid-cols-2 gap-4">
                {/* Min Order Qty */}
                <div className="space-y-2">
                  <Label htmlFor="minOrderQty">Min Order Qty *</Label>
                  <Input
                    id="minOrderQty"
                    type="number"
                    step="0.001"
                    {...register("minOrderQty")}
                  />
                  {errors.minOrderQty && (
                    <p className="text-sm text-destructive">
                      {errors.minOrderQty.message}
                    </p>
                  )}
                </div>

                {/* Max Order Qty */}
                <div className="space-y-2">
                  <Label htmlFor="maxOrderQty">Max Order Qty</Label>
                  <Input
                    id="maxOrderQty"
                    type="number"
                    step="0.001"
                    {...register("maxOrderQty")}
                  />
                </div>
              </div>

              {/* Base Price */}
              <div className="space-y-2">
                <Label htmlFor="basePrice">Base Price (INR) *</Label>
                <Input
                  id="basePrice"
                  type="number"
                  step="0.01"
                  {...register("basePrice")}
                  placeholder="0.00"
                />
                {errors.basePrice && (
                  <p className="text-sm text-destructive">
                    {errors.basePrice.message}
                  </p>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Bulk Discounts */}
          <Card>
            <CardHeader>
              <CardTitle>Bulk Discounts</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Existing discounts */}
              {bulkDiscounts.length > 0 && (
                <div className="space-y-2">
                  {bulkDiscounts.map((discount, index) => (
                    <div
                      key={index}
                      className="flex items-center justify-between rounded-md border p-3"
                    >
                      <span className="text-sm">
                        {discount.minQty}+ units: {discount.discountPercent}% off
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

              {/* Add new discount */}
              <div className="flex items-end gap-2">
                <div className="space-y-2">
                  <Label>Min Qty</Label>
                  <Input
                    type="number"
                    value={newDiscount.minQty}
                    onChange={(e) =>
                      setNewDiscount({ ...newDiscount, minQty: Number(e.target.value) })
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
                <Button type="button" variant="outline" onClick={addBulkDiscount}>
                  <Plus className="mr-1 h-4 w-4" />
                  Add
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                Add bulk discount slabs to offer discounts on larger orders.
              </p>
            </CardContent>
          </Card>

          {/* Submit */}
          <div className="flex gap-4">
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Create Product
            </Button>
            <Link href="/products">
              <Button type="button" variant="outline">
                Cancel
              </Button>
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
