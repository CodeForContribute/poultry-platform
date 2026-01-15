export interface Order {
  id: string;
  orderNumber: string;
  buyerId: string;
  buyerName: string;
  sellerId: string;
  sellerName: string;
  type: OrderType;
  status: OrderStatus;
  subtotal: number;
  discountAmount: number;
  gstAmount: number;
  deliveryCharge: number;
  totalAmount: number;
  platformFee: number;
  deliveryDate?: string;
  deliverySlot?: string;
  deliveryAddress: DeliveryAddress;
  deliveryInstructions?: string;
  items: OrderItem[];
  version: number;
  expiresAt?: string;
  cancelledAt?: string;
  cancelledBy?: string;
  cancellationReason?: string;
  createdAt: string;
  updatedAt: string;
  allowedNextStates: OrderStatus[];
  canCancel: boolean;
}

export interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
  discountPercent?: number;
  discountAmount: number;
  gstPercent: number;
  gstAmount: number;
  lineTotal: number;
  deliveredQuantity?: number;
  status: OrderItemStatus;
}

export interface DeliveryAddress {
  label?: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  pincode: string;
  landmark?: string;
  latitude?: number;
  longitude?: number;
  contactName: string;
  contactPhone: string;
}

export type OrderType = "SPOT" | "PREORDER";

export type OrderStatus =
  | "DRAFT"
  | "PLACED"
  | "SELLER_CONFIRMED"
  | "SELLER_REJECTED"
  | "PAYMENT_PENDING"
  | "PAYMENT_FAILED"
  | "PAID"
  | "DISPATCHED"
  | "DELIVERED"
  | "SETTLED"
  | "CANCELLED_BY_BUYER"
  | "REFUND_INITIATED"
  | "REFUNDED";

export type OrderItemStatus = "PENDING" | "CONFIRMED" | "DELIVERED" | "CANCELLED";

export interface UpdateOrderStatusRequest {
  status?: OrderStatus;
  reason?: string;
}
