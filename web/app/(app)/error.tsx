"use client";

export default function Error({
  error,
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  const message = error.message || "Erreur inconnue";

  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-6 text-sm">
      <h2 className="mb-2 font-semibold text-red-900">Une erreur est survenue</h2>
      <p className="mb-4 break-all text-red-700">{message}</p>
      <button
        onClick={() => retry()}
        className="rounded-md bg-red-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-800"
      >
        Réessayer
      </button>
    </div>
  );
}
