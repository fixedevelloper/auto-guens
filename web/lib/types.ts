export type Operator = "MTN" | "ORANGE";
export type OperationType = "DEPOSIT" | "WITHDRAW";
export type TransactionStatus =
  | "PENDING"
  | "SENT_TO_DEVICE"
  | "RECEIVED_BY_DEVICE"
  | "EXECUTING"
  | "SUCCESS"
  | "FAILED"
  | "TIMEOUT";
export type DeviceStatus = "ONLINE" | "BUSY" | "OFFLINE";

export interface TransactionSummary {
  id: string;
  operationType: OperationType;
  phone: string;
  operator: Operator;
  countryCode: string;
  amount: string;
  senderName: string;
  statut: TransactionStatus;
  operatorReference: string | null;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface StatusHistoryEntry {
  statut: TransactionStatus;
  receivedAt: string;
  rawSmsContent?: string | null;
  operatorReference?: string | null;
  failureReason?: string | null;
}

export interface TransactionDetail extends TransactionSummary {
  description: string | null;
  deviceId: string | null;
  simSlotUsed: number | null;
  ussdCodeUsed: string | null;
  expiresAt: string | null;
  statusHistory: StatusHistoryEntry[];
}

export interface TransactionFilters {
  operationType?: OperationType;
  operator?: Operator;
  statut?: TransactionStatus;
  from?: string;
  to?: string;
}

export interface DeviceSummary {
  id: string;
  statut: DeviceStatus;
  lastSeenAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DeviceCreated {
  id: string;
  apiKey: string;
  statut: DeviceStatus;
  createdAt: string;
  updatedAt: string;
}

export interface SimSlot {
  id: string;
  deviceId: string;
  slotIndex: number;
  operator: Operator;
  phoneNumberOnSim: string | null;
  actif: boolean;
  updatedAt: string;
}

export interface UssdTemplate {
  id: string;
  operator: Operator;
  operationType: OperationType;
  countryCode: string;
  template: string;
  actif: boolean;
  createdAt: string;
  updatedAt: string;
}
