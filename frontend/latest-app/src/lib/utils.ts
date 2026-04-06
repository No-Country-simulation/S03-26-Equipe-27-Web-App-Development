import type { TrafficRecord, TrafficStatsResponse } from "../types/api";

/** Substitui underscores por espaços e aplica Title Case. Ex: "RUSH_HOUR" → "Rush Hour" */
export function formatLabel(value: string): string {
  return value
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/(^|\s)\S/g, (l) => l.toUpperCase());
}

/** Formata número compacto em pt-BR. Ex: 1200 → "1,2 mil" */
export function formatCompactNumber(value: number): string {
  return new Intl.NumberFormat("pt-BR", {
    notation: "compact",
    maximumFractionDigits: 1
  }).format(value);
}

/** Formata timestamp ISO para data curta em pt-BR. */
export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat("pt-BR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(iso));
}

/** Converte ISO para valor de input datetime-local compensando o offset. */
export function formatDatetimeLocalValue(iso: string): string {
  const date = new Date(iso);
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 16);
}

/** Constrói array de {label, value} para gráficos. */
export function buildChartData(stats: TrafficStatsResponse): Array<{ label: string; value: number }> {
  return stats.labels.map((label, index) => ({
    label: formatLabel(label),
    value: stats.values[index] ?? 0
  }));
}

/** Retorna o label do grupo com maior volume. Retorna "Sem dados" se vazio. */
export function getPeakLabel(stats: TrafficStatsResponse): string {
  if (stats.labels.length === 0) return "Sem dados";
  let peakIndex = 0;
  stats.values.forEach((value, index) => {
    if (value > (stats.values[peakIndex] ?? 0)) peakIndex = index;
  });
  return formatLabel(stats.labels[peakIndex] ?? "Desconhecido");
}

/** Calcula volume médio arredondado. Retorna 0 para lista vazia. */
export function getAverageVolume(records: TrafficRecord[]): number {
  if (records.length === 0) return 0;
  return Math.round(
    records.reduce((sum, r) => sum + r.vehicleVolume, 0) / records.length
  );
}

/** Conta regiões únicas não-nulas. */
export function getUniqueRegions(records: TrafficRecord[]): number {
  return new Set(records.map((r) => r.region).filter(Boolean)).size;
}

/** Retorna o timestamp mais recente formatado. */
export function getLatestTimestamp(records: TrafficRecord[]): string {
  if (records.length === 0) return "Sem registros recebidos";
  const latest = [...records].sort((a, b) => b.timestamp.localeCompare(a.timestamp))[0];
  return formatDateTime(latest.timestamp);
}

/** Faz download de um Blob com compatibilidade entre browsers. */
export function downloadBlob(blob: Blob, filename: string): void {
  const href = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = href;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  setTimeout(() => URL.revokeObjectURL(href), 1000);
}
