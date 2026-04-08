import type { ReactNode } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { server } from "../mocks/server";

vi.mock("react-leaflet", () => ({
  MapContainer: ({ children }: { children: ReactNode }) => <div data-testid="map-container">{children}</div>,
  TileLayer: () => <div data-testid="tile-layer" />,
  Polyline: ({ children }: { children: ReactNode }) => <div data-testid="map-polyline">{children}</div>,
  Popup: ({ children }: { children: ReactNode }) => <div>{children}</div>
}));

vi.mock("recharts", () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  AreaChart: () => <div data-testid="area-chart" />,
  BarChart: () => <div data-testid="bar-chart" />,
  CartesianGrid: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
  Bar: () => <div data-testid="bar-series" />,
  Cell: () => null,
  Area: () => null
}));

import App from "../../App";

describe("App", () => {
  beforeEach(() => {
    vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:test");
    vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});
  });

  it("renders the dashboard and connection state", async () => {
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText(/SmartTrafficFlow/i)).toBeInTheDocument();
      expect(screen.getByText(/Conectado/i)).toBeInTheDocument();
      expect(screen.getByText(/Veículos capturados/i)).toBeInTheDocument();
    });
  });

  it("switches to the workspace view", async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(await screen.findByRole("button", { name: /Formulários e registros/i }));

    expect(await screen.findByText(/Adicionar registro de tráfego/i)).toBeInTheDocument();
  });

  it("searches and selects a street before saving a record", async () => {
    const user = userEvent.setup();
    let capturedStreetOsmWayId: number | null = null;
    server.use(
      http.post("http://localhost:8080/api/traffic-records", async ({ request }) => {
        const body = (await request.json()) as { streetOsmWayId: number };
        capturedStreetOsmWayId = body.streetOsmWayId;
        return HttpResponse.json(
          {
            id: "rec-created",
            timestamp: "2024-06-17T08:00:00Z",
            roadType: "ARTERIAL",
            vehicleVolume: 140,
            eventType: null,
            weather: null,
            streetId: "street-created",
            streetOsmWayId: body.streetOsmWayId,
            streetName: "Avenida Central"
          },
          { status: 201 }
        );
      })
    );

    render(<App />);

    await user.click(await screen.findByRole("button", { name: /Formulários e registros/i }));

    const saveButton = await screen.findByRole("button", { name: /Salvar registro/i });
    expect(saveButton).toBeDisabled();

    await user.type(screen.getByRole("searchbox", { name: /Rua/i }), "Aven");

    const option = await screen.findByRole("button", { name: /Avenida Central/i });
    await user.click(option);

    expect(screen.getByText(/Rua selecionada \(OSM\): 101/i)).toBeInTheDocument();
    expect(saveButton).toBeEnabled();

    await user.click(saveButton);

    await waitFor(() => expect(capturedStreetOsmWayId).toBe(101));
  });

  it("applies record selection and refreshes filtered analytics", async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(await screen.findByRole("button", { name: /Formulários e registros/i }));

    await user.click(await screen.findByLabelText(/Selecionar registro rec-1/i));
    await user.click(screen.getByRole("button", { name: /Aplicar seleção/i }));

    expect(await screen.findByText(/1 selecionado\(s\), 1 aplicado\(s\)/i)).toBeInTheDocument();
  });

  it("runs a simulation", async () => {
    const user = userEvent.setup();
    let capturedRecordsToGenerate: number | null = null;
    server.use(
      http.post("http://localhost:8080/api/simulations/generate", async ({ request }) => {
        const body = (await request.json()) as { recordsToGenerate: number; scenarioName: string };
        capturedRecordsToGenerate = body.recordsToGenerate;
        return HttpResponse.json(
          Array.from({ length: body.recordsToGenerate }, (_, index) => ({
            id: `sim-${index + 1}`,
            timestamp: "2024-06-18T08:00:00Z",
            roadType: "LOCAL",
            vehicleVolume: 80 + index,
            eventType: body.scenarioName,
            weather: "SUNNY",
            streetId: `sim-street-${index + 1}`,
            streetOsmWayId: 800 + index,
            streetName: `Rua Simulada ${index + 1}`
          }))
        );
      })
    );

    render(<App />);

    await user.click(await screen.findByRole("button", { name: /Formulários e registros/i }));
    await user.click(await screen.findByRole("button", { name: /Executar simulação/i }));

    await waitFor(() => expect(capturedRecordsToGenerate).toBe(18));
  });

  it("exports csv", async () => {
    const user = userEvent.setup();
    const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});

    render(<App />);

    await user.click(await screen.findByRole("button", { name: /Exportar CSV/i }));

    expect(await screen.findByText(/Exportação preparada no formato CSV/i)).toBeInTheDocument();
    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(click).toHaveBeenCalledTimes(1);
  });

  it("shows backend errors without crashing", async () => {
    server.use(
      http.get("http://localhost:8080/api/traffic-records", () =>
        HttpResponse.json({ message: "backend caiu" }, { status: 500 })
      )
    );

    render(<App />);

    expect(await screen.findByText(/backend caiu/i)).toBeInTheDocument();
    expect(screen.getByText(/Situação/i)).toBeInTheDocument();
  });
});
