import { listAllUssdTemplates } from "@/lib/data/ussd-templates";
import { ApiError } from "@/lib/api";
import type { UssdTemplate } from "@/lib/types";
import CreateTemplateForm from "./create-template-form";
import TemplateRow from "./template-row";

export const dynamic = "force-dynamic";

export default async function TemplatesPage() {
  let templates: UssdTemplate[];
  let error: string | null = null;
  try {
    templates = await listAllUssdTemplates();
  } catch (e) {
    templates = [];
    error = e instanceof ApiError ? e.message : "Erreur de connexion à l'API";
  }

  return (
    <div className="space-y-6">
      <h1 className="text-lg font-semibold text-slate-900">Templates USSD</h1>

      <CreateTemplateForm />

      {error && <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-2">Opérateur</th>
              <th className="px-4 py-2">Opération</th>
              <th className="px-4 py-2">Pays</th>
              <th className="px-4 py-2">Template</th>
              <th className="px-4 py-2">État</th>
              <th className="px-4 py-2">Mis à jour</th>
              <th className="px-4 py-2" />
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {templates.map((template) => (
              <TemplateRow key={template.id} template={template} />
            ))}
            {templates.length === 0 && !error && (
              <tr>
                <td colSpan={7} className="px-4 py-6 text-center text-slate-400">
                  Aucun template
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
