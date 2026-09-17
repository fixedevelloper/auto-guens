"use server";

import { revalidatePath } from "next/cache";
import { apiPost, apiPut, ApiError } from "@/lib/api";
import type { OperationType, Operator, UssdTemplate } from "@/lib/types";

export interface UssdTemplateFormState {
  error?: string;
  success?: boolean;
}

export async function createUssdTemplateAction(
  _prevState: UssdTemplateFormState,
  formData: FormData
): Promise<UssdTemplateFormState> {
  const operator = String(formData.get("operator") ?? "") as Operator;
  const operationType = String(formData.get("operationType") ?? "") as OperationType;
  const countryCode = String(formData.get("countryCode") ?? "").trim().toUpperCase();
  const template = String(formData.get("template") ?? "").trim();

  if (operator !== "MTN" && operator !== "ORANGE") {
    return { error: "Opérateur invalide" };
  }
  if (operationType !== "DEPOSIT" && operationType !== "WITHDRAW") {
    return { error: "Type d'opération invalide" };
  }
  if (!/^[A-Z]{2}$/.test(countryCode)) {
    return { error: "countryCode doit être un code ISO à 2 lettres (ex: CM)" };
  }
  if (!template) {
    return { error: "Le template est obligatoire" };
  }

  try {
    await apiPost<UssdTemplate>("/api/ussd-templates", { operator, operationType, countryCode, template });
    revalidatePath("/templates");
    return { success: true };
  } catch (e) {
    return { error: e instanceof ApiError ? e.message : "Erreur inconnue" };
  }
}

export async function updateUssdTemplateAction(
  id: string,
  _prevState: UssdTemplateFormState,
  formData: FormData
): Promise<UssdTemplateFormState> {
  const template = String(formData.get("template") ?? "").trim();
  const actif = formData.get("actif") === "on";

  if (!template) {
    return { error: "Le template est obligatoire" };
  }

  try {
    await apiPut<UssdTemplate>(`/api/ussd-templates/${id}`, { template, actif });
    revalidatePath("/templates");
    return { success: true };
  } catch (e) {
    return { error: e instanceof ApiError ? e.message : "Erreur inconnue" };
  }
}
