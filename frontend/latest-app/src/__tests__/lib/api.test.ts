import { describe, it, expect } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "../mocks/server";
import {
  getTrafficRecords,
  createTrafficRecord,
  getTrafficStats,
  getTrafficInsights,
  getTrafficMap,
  generateSimulation,
  getExport,
  ApiError
} from "../../lib/api";

const BASE = "http://localhost:8080/api";

// ─── getTrafficRecords ─────────────────────────────────────────────────────────

describe("getTrafficRecords()", () => {

  it("deve retornar lista de registros", async () => {
    const records = await getTrafficRecords();
    expect(records).toHaveLength(2);
    expect(records[0].roadType).toBe("ARTERIAL");
    expect(records[1].roadType).toBe("HIGHWAY");
  });

  it("deve retornar lista vazia quando API retorna []", async () => {
    server.use(http.get(`${BASE}/traffic-records`, () => HttpResponse.json([])));
    expect(await getTrafficRecords()).toHaveLength(0);
  });

  it("deve lançar ApiError quando API retorna 500", async () => {
    server.use(http.get(`${BASE}/traffic-records`, () =>
      HttpResponse.json({}, { status: 500 })
    ));
    await expect(getTrafficRecords()).rejects.toBeInstanceOf(ApiError);
  });

  it("deve expor o status HTTP no ApiError", async () => {
    server.use(http.get(`${BASE}/traffic-records`, () =>
      HttpResponse.json({}, { status: 503 })
    ));
    const err = await getTrafficRecords().catch(e => e) as ApiError;
    expect(err.status).toBe(503);
  });
});

// ─── createTrafficRecord ───────────────────────────────────────────────────────

describe("createTrafficRecord()", () => {
  const payload = {
    timestamp: "2024-06-17T08:00:00+00:00",
    roadType: "ARTERIAL" as const,
    vehicleVolume: 150,
    eventType: "RUSH",
    weather: "SUNNY",
    region: "CENTRO"
  };

  it("deve criar registro e retornar o objeto criado", async () => {
    const record = await createTrafficRecord(payload);
    expect(record.id).toBeDefined();
    expect(record.roadType).toBe("ARTERIAL");
  });

  it("deve lançar ApiError quando backend retorna 400", async () => {
    server.use(http.post(`${BASE}/traffic-records`, () =>
      HttpResponse.json({ code: "VALIDATION_ERROR" }, { status: 400 })
    ));
    await expect(createTrafficRecord(payload)).rejects.toBeInstanceOf(ApiError);
  });

  it("deve enviar Content-Type: application/json", async () => {
    let capturedType = "";
    server.use(http.post(`${BASE}/traffic-records`, ({ request }) => {
      capturedType = request.headers.get("content-type") ?? "";
      return HttpResponse.json({ ...payload, id: "test" }, { status: 201 });
    }));
    await createTrafficRecord(payload);
    expect(capturedType).toContain("application/json");
  });
});

// ─── getTrafficStats ───────────────────────────────────────────────────────────

describe("getTrafficStats()", () => {

  it("deve retornar labels e values para groupBy=hour", async () => {
    const stats = await getTrafficStats("hour");
    expect(stats.labels).toBeInstanceOf(Array);
    expect(stats.values).toBeInstanceOf(Array);
    expect(stats.labels).toHaveLength(stats.values.length);
  });

  it("deve codificar groupBy com espaço na URL", async () => {
    let capturedUrl = "";
    server.use(http.get(`${BASE}/traffic-stats`, ({ request }) => {
      capturedUrl = request.url;
      return HttpResponse.json({ labels: [], values: [] });
    }));
    await getTrafficStats("road Type");
    expect(capturedUrl).toContain("groupBy=road%20Type");
  });

  it("deve lançar ApiError para groupBy inválido (400)", async () => {
    await expect(getTrafficStats("invalid")).rejects.toBeInstanceOf(ApiError);
  });
});

// ─── getTrafficInsights ────────────────────────────────────────────────────────

describe("getTrafficInsights()", () => {

  it("deve retornar lista de insights", async () => {
    const response = await getTrafficInsights();
    expect(response.insights.length).toBeGreaterThan(0);
  });

  it("deve retornar lista vazia quando backend retorna []", async () => {
    server.use(http.get(`${BASE}/traffic-insights`, () =>
      HttpResponse.json({ insights: [] })
    ));
    const response = await getTrafficInsights();
    expect(response.insights).toHaveLength(0);
  });
});

// ─── getTrafficMap ─────────────────────────────────────────────────────────────

describe("getTrafficMap()", () => {

  it("deve retornar lista de pontos com lat e lng", async () => {
    const points = await getTrafficMap();
    expect(points).toHaveLength(2);
    expect(points[0]).toMatchObject({
      region: expect.any(String),
      lat:    expect.any(Number),
      lng:    expect.any(Number)
    });
  });
});

// ─── generateSimulation ────────────────────────────────────────────────────────

describe("generateSimulation()", () => {

  it("deve retornar lista com a quantidade correta de registros", async () => {
    const records = await generateSimulation({ recordsToGenerate: 5, scenarioName: "Teste" });
    expect(records).toHaveLength(5);
  });

  it("deve lançar ApiError quando limite é excedido (400)", async () => {
    await expect(
      generateSimulation({ recordsToGenerate: 501, scenarioName: "Excesso" })
    ).rejects.toBeInstanceOf(ApiError);
  });
});

// ─── getExport ─────────────────────────────────────────────────────────────────

describe("getExport()", () => {

  it("deve retornar Blob para formato csv", async () => {
    const blob = await getExport("csv");
    expect(blob).toBeInstanceOf(Blob);
    expect(blob.size).toBeGreaterThan(0);
  });

  it("deve lançar ApiError quando backend retorna 400", async () => {
    server.use(http.get(`${BASE}/exports`, () =>
      HttpResponse.json({ code: "INVALID_ARGUMENT" }, { status: 400 })
    ));
    await expect(getExport("csv")).rejects.toBeInstanceOf(ApiError);
  });
});

// ─── ApiError ─────────────────────────────────────────────────────────────────

describe("ApiError", () => {

  it("deve ter name='ApiError'", () => {
    expect(new ApiError("msg", 404).name).toBe("ApiError");
  });

  it("deve expor o status HTTP", () => {
    expect(new ApiError("Not Found", 404).status).toBe(404);
  });

  it("deve ser instância de Error", () => {
    expect(new ApiError("erro", 500)).toBeInstanceOf(Error);
  });

  it("deve ter message correta", () => {
    expect(new ApiError("mensagem teste", 400).message).toBe("mensagem teste");
  });
});
