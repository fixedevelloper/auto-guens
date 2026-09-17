import "server-only";
import { apiGet } from "@/lib/api";
import type { Operator, UssdTemplate } from "@/lib/types";

const ALL_OPERATORS: Operator[] = ["MTN", "ORANGE"];

function listByOperator(operator: Operator): Promise<UssdTemplate[]> {
  return apiGet<UssdTemplate[]>(`/api/ussd-templates?operator=${operator}`);
}

/** L'API ne liste que par operator (paramètre obligatoire) — on agrège les deux. */
export async function listAllUssdTemplates(): Promise<UssdTemplate[]> {
  const results = await Promise.all(ALL_OPERATORS.map(listByOperator));
  return results.flat().sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}
