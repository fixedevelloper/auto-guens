"use server";

import { revalidatePath } from "next/cache";
import { apiPost, apiPut, ApiError } from "@/lib/api";
import { listSimSlots } from "@/lib/data/devices";
import type { DeviceCreated, Operator } from "@/lib/types";

export interface CreateDeviceState {
  device?: DeviceCreated;
  error?: string;
}

export async function createDeviceAction(): Promise<CreateDeviceState> {
  try {
    const device = await apiPost<DeviceCreated>("/api/devices");
    revalidatePath("/devices");
    return { device };
  } catch (e) {
    return { error: e instanceof ApiError ? e.message : "Erreur inconnue" };
  }
}

export interface SimSlotFormState {
  error?: string;
  success?: boolean;
}

/** Ajoute/remplace un slot : ré-envoie l'ensemble des slots actifs (POST replaceAll côté
 * API désactive tout slot omis), en fusionnant avec celui soumis. */
export async function upsertSimSlotAction(
  deviceId: string,
  _prevState: SimSlotFormState,
  formData: FormData
): Promise<SimSlotFormState> {
  const slotIndex = Number(formData.get("slotIndex"));
  const operator = String(formData.get("operator") ?? "") as Operator;
  const phoneNumberOnSim = String(formData.get("phoneNumberOnSim") ?? "").trim() || null;

  if (!Number.isInteger(slotIndex) || slotIndex < 0) {
    return { error: "Index de slot invalide" };
  }
  if (operator !== "MTN" && operator !== "ORANGE") {
    return { error: "Opérateur invalide" };
  }

  try {
    const existing = await listSimSlots(deviceId);
    const items = existing
      .filter((slot) => slot.actif && slot.slotIndex !== slotIndex)
      .map((slot) => ({
        slotIndex: slot.slotIndex,
        operator: slot.operator,
        phoneNumberOnSim: slot.phoneNumberOnSim,
      }));
    items.push({ slotIndex, operator, phoneNumberOnSim });
    items.sort((a, b) => a.slotIndex - b.slotIndex);

    await apiPost(`/api/devices/${deviceId}/sim-slots`, items);
    revalidatePath(`/devices/${deviceId}`);
    return { success: true };
  } catch (e) {
    return { error: e instanceof ApiError ? e.message : "Erreur inconnue" };
  }
}

export async function setSimSlotActiveAction(deviceId: string, slotIndex: number, actif: boolean): Promise<void> {
  await apiPut(`/api/devices/${deviceId}/sim-slots/${slotIndex}`, { actif });
  revalidatePath(`/devices/${deviceId}`);
}
