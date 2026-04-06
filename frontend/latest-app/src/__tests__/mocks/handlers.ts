import { http, HttpResponse } from "msw";
import type {
  TrafficRecord,
  TrafficStatsResponse,
  TrafficInsightResponse,
  MapPoint
} from "../../types/api";

const BASE = "http://localhost:8080/api";

// ─── Dados reutilizáveis nos testes ───────────────────────────────────────────

export const mockRecord1: TrafficRecord = {
  id: "00000000-0000-0000-0000-000000000001",
  timestamp: "2024-06-17T08:00:00+00:00",
  roadType: "ARTERIAL",
  vehicleVolume: 120,
  eventType: "RUSH_HOUR",
  weather: "SUNNY",
  region: "CENTRO"
};

export const mockRecord2: TrafficRecord = {
  id: "00000000-0000-0000-0000-000000000002",
  timestamp: "2024-06-17T17:30:00+00:00",
  roadType: "HIGHWAY",
  vehicleVolume: 250,
  eventType: null,
  weather: "RAIN",
  region: "NORTE"
};

export const mockStats: TrafficStatsResponse = {
  labels: ["08", "17", "12"],
  values: [120, 250, 80]
};

export const mockInsights: TrafficInsightResponse = {
  insights: [
    "Horario com maior volume agregado: 17h.",
    "Tipo de via com maior volume agregado: HIGHWAY."
  ]
};

export const mockMapPoints: MapPoint[] = [
  { region: "CENTRO", lat: -23.5430, lng: -46.6393 },
  { region: "NORTE",  lat: -23.4900, lng: -46.6260 }
];

// ─── Handlers padrão (happy path) ─────────────────────────────────────────────

export const handlers = [

  http.get(`${BASE}/traffic-records`, () =>
    HttpResponse.json([mockRecord1, mockRecord2])
  ),

  http.post(`${BASE}/traffic-records`, async ({ request }) => {
    const body = await request.json() as Record<string, unknown>;
    const created: TrafficRecord = {
      id: "00000000-0000-0000-0000-000000000099",
      timestamp: body.timestamp as string,
      roadType: body.roadType as "LOCAL" | "ARTERIAL" | "HIGHWAY",
      vehicleVolume: body.vehicleVolume as number,
      eventType: (body.eventType as string) || null,
      weather:    (body.weather    as string) || null,
      region:     (body.region     as string) || null
    };
    return HttpResponse.json(created, { status: 201 });
  }),

  http.get(`${BASE}/traffic-stats`, ({ request }) => {
    const url    = new URL(request.url);
    const groupBy = url.searchParams.get("groupBy");
    if (!["hour", "weekday", "roadType"].includes(groupBy ?? "")) {
      return HttpResponse.json(
        { code: "INVALID_ARGUMENT", message: "groupBy inválido" },
        { status: 400 }
      );
    }
    return HttpResponse.json(mockStats);
  }),

  http.get(`${BASE}/traffic-insights`, () =>
    HttpResponse.json(mockInsights)
  ),

  http.get(`${BASE}/traffic-map`, () =>
    HttpResponse.json(mockMapPoints)
  ),

  http.post(`${BASE}/simulations/generate`, async ({ request }) => {
    const body = await request.json() as { recordsToGenerate: number; scenarioName: string };
    if (body.recordsToGenerate > 500) {
      return HttpResponse.json(
        { code: "VALIDATION_ERROR", message: "Máximo 500" },
        { status: 400 }
      );
    }
    const generated: TrafficRecord[] = Array.from(
      { length: body.recordsToGenerate },
      (_, i) => ({
        id: `00000000-0000-0000-0000-${String(i).padStart(12, "0")}`,
        timestamp: new Date().toISOString(),
        roadType: "LOCAL",
        vehicleVolume: 100 + i,
        eventType: body.scenarioName,
        weather: "SUNNY",
        region: "DEFAULT_REGION"
      })
    );
    return HttpResponse.json(generated, { status: 201 });
  }),

  http.get(`${BASE}/exports`, ({ request }) => {
    const url    = new URL(request.url);
    const format = url.searchParams.get("format");
    if (format === "csv") {
      return new HttpResponse(
        "id,timestamp,roadType,vehicleVolume,eventType,weather,region\n" +
        "00000000,2024-06-17T08:00:00Z,ARTERIAL,120,RUSH_HOUR,SUNNY,CENTRO\n",
        { headers: {
          "Content-Type": "text/plain",
          "Content-Disposition": "attachment; filename=traffic-data.csv"
        }}
      );
    }
    if (format === "json") {
      return HttpResponse.json([{ status: "ok" }]);
    }
    return HttpResponse.json(
      { code: "INVALID_ARGUMENT", message: "Formato inválido" },
      { status: 400 }
    );
  })
];
