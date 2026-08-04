export type DeliveryStatus = 'PENDING' | 'IN_TRANSIT' | 'DELIVERED' | 'FAILED';

export type DeliveryEventType =
  | 'DELIVERY_CREATED'
  | 'READY_FOR_PICKUP'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'DELIVERY_FAILED';

export interface DeliveryEvent {
  id: number;
  requestId: string;
  eventType: DeliveryEventType;
  status: DeliveryStatus;
  note?: string;
  occurredAt: string;
}

export interface Delivery {
  id: number;
  orderId: number;
  version: number;
  carrier: string;
  trackingCode: string;
  status: DeliveryStatus;
  estimatedDelivery: string;
  deliveredAt?: string;
  events: DeliveryEvent[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateDeliveryRequest {
  carrier: string;
  trackingCode: string;
  estimatedDelivery: string;
}

export interface UpdateDeliveryRequest {
  carrier?: string;
  trackingCode?: string;
  estimatedDelivery?: string;
}

export interface UpdateDeliveryStatusRequest {
  expectedVersion: number;
  status: DeliveryStatus;
  eventType?: DeliveryEventType;
  note?: string;
}

export interface AddDeliveryEventRequest {
  requestId: string;
  eventType: DeliveryEventType;
  note?: string;
}
