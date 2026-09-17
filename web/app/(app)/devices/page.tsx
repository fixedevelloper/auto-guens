import Link from "next/link";
import { listDevices } from "@/lib/data/devices";
import { formatDate } from "@/lib/format";
import { Badge } from "@/components/badge";
import { ApiError } from "@/lib/api";
import type { DeviceSummary } from "@/lib/types";
import CreateDeviceButton from "./create-device-button";

export const dynamic = "force-dynamic";

export default async function DevicesPage() {
  let devices: DeviceSummary[];
  let error: string | null = null;
  try {
    devices = await listDevices();
  } catch (e) {
    devices = [];
    error = e instanceof ApiError ? e.message : "Erreur de connexion à l'API";
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900">Devices</h1>
      </div>

      <CreateDeviceButton />

      {error && <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-2">ID</th>
              <th className="px-4 py-2">Statut</th>
              <th className="px-4 py-2">Dernière activité</th>
              <th className="px-4 py-2">Créé le</th>
              <th className="px-4 py-2" />
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {devices.map((device) => (
              <tr key={device.id} className="hover:bg-slate-50">
                <td className="px-4 py-2 font-mono text-xs">{device.id}</td>
                <td className="px-4 py-2">
                  <Badge status={device.statut} />
                </td>
                <td className="px-4 py-2 text-slate-500">{formatDate(device.lastSeenAt)}</td>
                <td className="px-4 py-2 text-slate-500">{formatDate(device.createdAt)}</td>
                <td className="px-4 py-2 text-right">
                  <Link href={`/devices/${device.id}`} className="text-slate-600 hover:text-slate-900">
                    Config SIM
                  </Link>
                </td>
              </tr>
            ))}
            {devices.length === 0 && !error && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-slate-400">
                  Aucun device
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
