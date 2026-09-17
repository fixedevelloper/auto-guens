import Link from "next/link";
import { notFound } from "next/navigation";
import { getDevice, listSimSlots } from "@/lib/data/devices";
import { formatDate } from "@/lib/format";
import { Badge } from "@/components/badge";
import { ApiError } from "@/lib/api";
import SimSlotForm from "./sim-slot-form";
import ToggleSlotButton from "./toggle-slot-button";

export const dynamic = "force-dynamic";

export default async function DeviceDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  let device;
  let slots;
  try {
    [device, slots] = await Promise.all([getDevice(id), listSimSlots(id)]);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  return (
    <div className="space-y-6">
      <div>
        <Link href="/devices" className="text-sm text-slate-500 hover:text-slate-900">
          ← Devices
        </Link>
      </div>

      <div className="flex items-center justify-between">
        <h1 className="font-mono text-lg font-semibold text-slate-900">{device.id}</h1>
        <Badge status={device.statut} />
      </div>

      <dl className="grid grid-cols-2 gap-x-6 gap-y-2 rounded-lg border border-slate-200 bg-white p-4 text-sm sm:grid-cols-3">
        <div>
          <dt className="text-xs uppercase text-slate-400">Dernière activité</dt>
          <dd className="text-slate-800">{formatDate(device.lastSeenAt)}</dd>
        </div>
        <div>
          <dt className="text-xs uppercase text-slate-400">Créé le</dt>
          <dd className="text-slate-800">{formatDate(device.createdAt)}</dd>
        </div>
      </dl>

      <div className="space-y-3">
        <h2 className="text-sm font-semibold text-slate-900">Puces SIM</h2>

        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-2">Slot</th>
                <th className="px-4 py-2">Opérateur</th>
                <th className="px-4 py-2">Numéro</th>
                <th className="px-4 py-2">État</th>
                <th className="px-4 py-2">Mis à jour</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {slots
                .sort((a, b) => a.slotIndex - b.slotIndex)
                .map((slot) => (
                  <tr key={slot.id} className={slot.actif ? "" : "opacity-50"}>
                    <td className="px-4 py-2">{slot.slotIndex}</td>
                    <td className="px-4 py-2">{slot.operator}</td>
                    <td className="px-4 py-2">{slot.phoneNumberOnSim ?? "—"}</td>
                    <td className="px-4 py-2">{slot.actif ? "actif" : "inactif"}</td>
                    <td className="px-4 py-2 text-slate-500">{formatDate(slot.updatedAt)}</td>
                    <td className="px-4 py-2 text-right">
                      <ToggleSlotButton deviceId={device.id} slotIndex={slot.slotIndex} actif={slot.actif} />
                    </td>
                  </tr>
                ))}
              {slots.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-6 text-center text-slate-400">
                    Aucune puce configurée
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <SimSlotForm deviceId={device.id} />
      </div>
    </div>
  );
}
