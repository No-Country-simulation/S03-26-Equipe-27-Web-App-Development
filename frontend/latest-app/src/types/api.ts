export type RoadType = "LOCAL" | "ARTERIAL" | "HIGHWAY" | string;

export interface TrafficRecord {
  id: string;
  timestamp: string;
  roadType: RoadType;
  vehicleVolume: number;
  eventType: string | null;
  weather: string | null;
  streetId: string | null;
  streetOsmWayId: number | null;
  streetName: string | null;
}

export interface CreateTrafficRecordRequest {
  timestamp: string;
  roadType: string;
  vehicleVolume: number;
  eventType: string;
  weather: string;
  streetOsmWayId: number;
}

export interface TrafficStatsResponse {
  labels: string[];
  values: number[];
}

export interface TrafficInsightResponse {
  insights: string[];
}

export interface SimulationRequest {
  recordsToGenerate: number;
  scenarioName: string;
}

export interface MapPoint {
  type: "Feature";
  properties: {
    recordId: string;
    streetId: string;
    streetOsmWayId: number;
    streetName: string;
    vehicleVolume: number;
    trafficLevel: "LOW" | "MEDIUM" | "HIGH";
    color: string;
  };
  geometry: {
    type: "LineString";
    coordinates: [number, number][];
  };
}

export interface MapFeatureCollection {
  type: "FeatureCollection";
  features: MapPoint[];
}

export interface StreetOption {
  id: string;
  osmWayId: number;
  name: string;
}

export interface StreetSearchResponse {
  items: StreetOption[];
  limit: number;
  offset: number;
  total: number;
}
