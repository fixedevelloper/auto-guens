import "server-only";
import { apiGet } from "@/lib/api";
import type { DeviceSummary, SimSlot } from "@/lib/types";

export function listDevices(): Promise<DeviceSummary[]> {
  return apiGet<DeviceSummary[]>("/api/devices");
}

export function getDevice(id: string): Promise<DeviceSummary> {
  return apiGet<DeviceSummary>(`/api/devices/${id}`);
}

export function listSimSlots(deviceId: string): Promise<SimSlot[]> {
  return apiGet<SimSlot[]>(`/api/devices/${deviceId}/sim-slots`);
}
