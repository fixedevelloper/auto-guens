"use client";

import { useActionState, useState } from "react";
import { updateUssdTemplateAction, type UssdTemplateFormState } from "@/lib/ussd-template-actions";
import { formatDate } from "@/lib/format";
import type { UssdTemplate } from "@/lib/types";

const initialState: UssdTemplateFormState = {};

export default function TemplateRow({ template }: { template: UssdTemplate }) {
  const [editing, setEditing] = useState(false);
  const action = updateUssdTemplateAction.bind(null, template.id);
  const [state, formAction, pending] = useActionState(action, initialState);

  // Ferme le formulaire dès que l'action réussit (ajustement pendant le rendu plutôt
  // qu'un effet, pour éviter un aller-retour de rendu superflu).
  const [lastHandledState, setLastHandledState] = useState(state);
  if (state !== lastHandledState) {
    setLastHandledState(state);
    if (state.success && editing) setEditing(false);
  }

  if (!editing) {
    return (
      <tr className={template.actif ? "" : "opacity-50"}>
        <td className="px-4 py-2">{template.operator}</td>
        <td className="px-4 py-2">{template.operationType}</td>
        <td className="px-4 py-2">{template.countryCode}</td>
        <td className="px-4 py-2 font-mono text-xs">{template.template}</td>
        <td className="px-4 py-2">{template.actif ? "actif" : "inactif"}</td>
        <td className="px-4 py-2 text-slate-500">{formatDate(template.updatedAt)}</td>
        <td className="px-4 py-2 text-right">
          <button
            type="button"
            onClick={() => setEditing(true)}
            className="text-xs text-slate-500 hover:text-slate-900"
          >
            Éditer
          </button>
        </td>
      </tr>
    );
  }

  return (
    <tr>
      <td className="px-4 py-2">{template.operator}</td>
      <td className="px-4 py-2">{template.operationType}</td>
      <td className="px-4 py-2">{template.countryCode}</td>
      <td className="px-4 py-2" colSpan={3}>
        <form action={formAction} className="flex flex-wrap items-center gap-2 text-sm">
          <input
            type="text"
            name="template"
            defaultValue={template.template}
            required
            className="flex-1 min-w-48 rounded-md border border-slate-300 px-2 py-1 font-mono text-xs"
          />
          <label className="flex items-center gap-1 text-xs text-slate-500">
            <input type="checkbox" name="actif" defaultChecked={template.actif} />
            actif
          </label>
          <button
            type="submit"
            disabled={pending}
            className="rounded-md bg-slate-900 px-2 py-1 text-xs font-medium text-white hover:bg-slate-800 disabled:opacity-50"
          >
            {pending ? "..." : "Enregistrer"}
          </button>
          <button
            type="button"
            onClick={() => setEditing(false)}
            className="text-xs text-slate-500 hover:text-slate-900"
          >
            Annuler
          </button>
          {state.error && <span className="w-full text-xs text-red-600">{state.error}</span>}
        </form>
      </td>
    </tr>
  );
}
