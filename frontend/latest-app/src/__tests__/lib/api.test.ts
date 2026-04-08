import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "../mocks/server";
import {
  ApiError,
  createTrafficRecord,
  generateSimulation,
  getExport,
  getTrafficInsights,
  getTrafficMap,
  getTrafficRecords,
  getTrafficStats,
  searchStreets
} from "../../lib/api";

const BASE = "http://localhost:8080/api";

describe("api client", () => {
  it("loads traffic records", async () => {
    const records = await getTrafficRecords();
    expect(records).toHaveLength(2);
    expect(records[0]?.streetName).toBe("Avenida Central");
  });

  it("creates a traffic record using streetOsmWayId", async () => {
    const record = await createTrafficRecord({
      timestamp: "2024-06-17T08:00:00Z",
      roadType: "ARTERIAL",
      vehicleVolume: 120,
      eventType: "RUSH_HOUR",
      weather: "SUNNY",
      streetOsmWayId: 101
    });

    expect(record.streetOsmWayId).toBe(101);
  });

  it("loads stats without a selection using GET", async () => {
    const stats = await getTrafficStats("hour");
    expect(stats.labels).toEqual(["08", "17", "12"]);
  });

  it("loads filtered stats using POST", async () => {
    const stats = await getTrafficStats("hour", ["rec-1"]);
    expect(stats.values).toEqual([120, 250, 80]);
  });

  it("loads insights without a selection using GET", async () => {
    const insights = await getTrafficInsights();
    expect(insights.insights).toHaveLength(2);
  });

  it("loads filtered insights using POST", async () => {
    const insights = await getTrafficInsights(["rec-1"]);
    expect(insights.insights[0]).toMatch(/17h/);
  });

  it("returns an empty feature collection when no record ids are passed", async () => {
    await expect(getTrafficMap([])).resolves.toEqual({ type: "FeatureCollection", features: [] });
  });

  it("loads filtered map data using POST", async () => {
    const mapData = await getTrafficMap(["rec-1"]);
    expect(mapData.features[0]?.properties.streetName).toBe("Avenida Central");
  });

  it("searches streets", async () => {
    const response = await searchStreets("Aven");
    expect(response.items[0]?.osmWayId).toBe(101);
  });

  it("generates simulation records", async () => {
    const records = await generateSimulation({ recordsToGenerate: 3, scenarioName: "Teste" });
    expect(records).toHaveLength(3);
  });

  it("downloads csv export as blob", async () => {
    const blob = await getExport("csv");
    expect(blob).toBeInstanceOf(Blob);
  });

  it("surfaces API errors and status", async () => {
    server.use(
      http.get(`${BASE}/traffic-records`, () => HttpResponse.json({ message: "falhou" }, { status: 503 }))
    );

    await expect(getTrafficRecords()).rejects.toMatchObject({
      message: "falhou",
      status: 503
    } satisfies Partial<ApiError>);
  });
});
