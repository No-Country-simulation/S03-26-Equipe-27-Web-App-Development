import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  formatLabel,
  formatCompactNumber,
  buildChartData,
  getPeakLabel,
  getAverageVolume,
  getUniqueRegions,
  getLatestTimestamp,
  downloadBlob
} from "../../lib/utils";
import type { TrafficRecord, TrafficStatsResponse } from "../../types/api";

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeRecord(overrides: Partial<TrafficRecord> = {}): TrafficRecord {
  return {
    id: "id-1",
    timestamp: "2024-06-17T08:00:00+00:00",
    roadType: "ARTERIAL",
    vehicleVolume: 100,
    eventType: null,
    weather: null,
    region: "CENTRO",
    ...overrides
  };
}

function makeStats(labels: string[], values: number[]): TrafficStatsResponse {
  return { labels, values };
}

// ─── formatLabel ──────────────────────────────────────────────────────────────

describe("formatLabel()", () => {

  it("converte RUSH_HOUR → Rush Hour", () => {
    expect(formatLabel("RUSH_HOUR")).toBe("Rush Hour");
  });

  it("converte MONDAY → Monday", () => {
    expect(formatLabel("MONDAY")).toBe("Monday");
  });

  it("converte múltiplos underscores", () => {
    expect(formatLabel("ROAD_TYPE_LOCAL")).toBe("Road Type Local");
  });

  it("converte minúsculo para Title Case", () => {
    expect(formatLabel("arterial")).toBe("Arterial");
  });

  it("trata string vazia sem erro", () => {
    expect(formatLabel("")).toBe("");
  });

  it("trata string de um caractere", () => {
    expect(formatLabel("a")).toBe("A");
  });

  it("preserva números nos labels", () => {
    expect(formatLabel("08")).toBe("08");
  });
});

// ─── formatCompactNumber ──────────────────────────────────────────────────────

describe("formatCompactNumber()", () => {

  it("retorna '0' para zero", () => {
    expect(formatCompactNumber(0)).toBe("0");
  });

  it("retorna número pequeno sem sufixo", () => {
    expect(formatCompactNumber(999)).toMatch(/^999/);
  });

  it("formata milhar de forma compacta", () => {
    // pt-BR: "1,2 mil" ou similar
    expect(formatCompactNumber(1200)).toMatch(/1[,.]2/);
  });

  it("formata milhão de forma compacta", () => {
    expect(formatCompactNumber(1_500_000)).toMatch(/1[,.]5/);
  });
});

// ─── buildChartData ───────────────────────────────────────────────────────────

describe("buildChartData()", () => {

  it("mapeia labels e values em objetos {label, value}", () => {
    const data = buildChartData(makeStats(["08", "17"], [100, 200]));
    expect(data).toHaveLength(2);
    expect(data[0]).toEqual({ label: "08", value: 100 });
    expect(data[1]).toEqual({ label: "17", value: 200 });
  });

  it("aplica formatLabel nos labels", () => {
    const data = buildChartData(makeStats(["RUSH_HOUR"], [50]));
    expect(data[0].label).toBe("Rush Hour");
  });

  it("usa 0 quando value não tem índice correspondente", () => {
    const data = buildChartData(makeStats(["08", "17"], [100]));
    expect(data[1].value).toBe(0);
  });

  it("retorna lista vazia para stats vazio", () => {
    expect(buildChartData(makeStats([], []))).toHaveLength(0);
  });
});

// ─── getPeakLabel ─────────────────────────────────────────────────────────────

describe("getPeakLabel()", () => {

  it("retorna 'Sem dados' para stats vazio", () => {
    expect(getPeakLabel(makeStats([], []))).toBe("Sem dados");
  });

  it("retorna o label do maior valor", () => {
    expect(getPeakLabel(makeStats(["08", "17", "12"], [100, 300, 150]))).toBe("17");
  });

  it("retorna o único elemento disponível", () => {
    expect(getPeakLabel(makeStats(["HIGHWAY"], [500]))).toBe("Highway");
  });

  it("aplica formatLabel no resultado", () => {
    expect(getPeakLabel(makeStats(["ROAD_TYPE"], [999]))).toBe("Road Type");
  });

  it("não lança exceção com valores iguais", () => {
    expect(() =>
      getPeakLabel(makeStats(["A", "B", "C"], [100, 100, 100]))
    ).not.toThrow();
  });
});

// ─── getAverageVolume ─────────────────────────────────────────────────────────

describe("getAverageVolume()", () => {

  it("retorna 0 para lista vazia", () => {
    expect(getAverageVolume([])).toBe(0);
  });

  it("calcula a média corretamente", () => {
    const records = [
      makeRecord({ vehicleVolume: 100 }),
      makeRecord({ vehicleVolume: 200 }),
      makeRecord({ vehicleVolume: 300 })
    ];
    expect(getAverageVolume(records)).toBe(200);
  });

  it("arredonda a média (100+101)/2 = 100.5 → 101", () => {
    const records = [
      makeRecord({ vehicleVolume: 100 }),
      makeRecord({ vehicleVolume: 101 })
    ];
    expect(getAverageVolume(records)).toBe(101);
  });

  it("retorna o próprio valor para um único registro", () => {
    expect(getAverageVolume([makeRecord({ vehicleVolume: 75 })])).toBe(75);
  });
});

// ─── getUniqueRegions ─────────────────────────────────────────────────────────

describe("getUniqueRegions()", () => {

  it("retorna 0 para lista vazia", () => {
    expect(getUniqueRegions([])).toBe(0);
  });

  it("conta regiões únicas corretamente", () => {
    const records = [
      makeRecord({ region: "CENTRO" }),
      makeRecord({ region: "NORTE" }),
      makeRecord({ region: "CENTRO" })
    ];
    expect(getUniqueRegions(records)).toBe(2);
  });

  it("ignora regiões nulas", () => {
    const records = [
      makeRecord({ region: "CENTRO" }),
      makeRecord({ region: null }),
      makeRecord({ region: null })
    ];
    expect(getUniqueRegions(records)).toBe(1);
  });

  it("retorna 0 quando todas as regiões são nulas", () => {
    expect(getUniqueRegions([
      makeRecord({ region: null }),
      makeRecord({ region: null })
    ])).toBe(0);
  });

  it("conta corretamente todas as regiões distintas", () => {
    expect(getUniqueRegions([
      makeRecord({ region: "A" }),
      makeRecord({ region: "B" }),
      makeRecord({ region: "C" })
    ])).toBe(3);
  });
});

// ─── getLatestTimestamp ───────────────────────────────────────────────────────

describe("getLatestTimestamp()", () => {

  it("retorna mensagem de fallback para lista vazia", () => {
    expect(getLatestTimestamp([])).toBe("Sem registros recebidos");
  });

  it("retorna o timestamp do registro mais recente", () => {
    const records = [
      makeRecord({ timestamp: "2024-01-01T08:00:00+00:00" }),
      makeRecord({ timestamp: "2024-06-17T17:00:00+00:00" }), // mais recente
      makeRecord({ timestamp: "2024-03-15T12:00:00+00:00" })
    ];
    // Junho é mais recente — deve aparecer no resultado formatado
    expect(getLatestTimestamp(records)).toMatch(/jun/i);
  });

  it("não muta o array original", () => {
    const records = [
      makeRecord({ timestamp: "2024-01-01T08:00:00+00:00" }),
      makeRecord({ timestamp: "2024-06-17T08:00:00+00:00" })
    ];
    const original = [...records];
    getLatestTimestamp(records);
    expect(records).toEqual(original);
  });

  it("funciona com registro único", () => {
    const result = getLatestTimestamp([makeRecord()]);
    expect(result).not.toBe("Sem registros recebidos");
    expect(result).toBeTruthy();
  });
});

// ─── downloadBlob ─────────────────────────────────────────────────────────────

describe("downloadBlob()", () => {
  let createURLMock: ReturnType<typeof vi.fn>;
  let revokeURLMock: ReturnType<typeof vi.fn>;
  let clickSpy: ReturnType<typeof vi.fn>;
  let appendSpy: ReturnType<typeof vi.spyOn>;
  let removeSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    createURLMock = vi.fn().mockReturnValue("blob:http://localhost/test");
    revokeURLMock = vi.fn();
    clickSpy = vi.fn();

    URL.createObjectURL = createURLMock;
    URL.revokeObjectURL = revokeURLMock;

    appendSpy = vi.spyOn(document.body, "appendChild").mockImplementation(n => n);
    removeSpy = vi.spyOn(document.body, "removeChild").mockImplementation(n => n);

    vi.spyOn(document, "createElement").mockImplementation((tag) => {
      if (tag === "a") {
        return { href: "", download: "", click: clickSpy } as unknown as HTMLAnchorElement;
      }
      return document.createElement(tag);
    });
  });

  afterEach(() => vi.restoreAllMocks());

  it("chama URL.createObjectURL com o blob", () => {
    const blob = new Blob(["test"], { type: "text/plain" });
    downloadBlob(blob, "test.txt");
    expect(createURLMock).toHaveBeenCalledWith(blob);
  });

  it("aciona o clique no anchor", () => {
    downloadBlob(new Blob(["test"]), "test.txt");
    expect(clickSpy).toHaveBeenCalledTimes(1);
  });

  it("adiciona e remove o anchor do DOM (compatibilidade cross-browser)", () => {
    downloadBlob(new Blob(["test"]), "test.txt");
    expect(appendSpy).toHaveBeenCalledTimes(1);
    expect(removeSpy).toHaveBeenCalledTimes(1);
  });
});
