export type RoadType = "LOCAL" | "ARTERIAL" | "HIGHWAY";

export interface TrafficRecord {
  id: string;
  timestamp: string;
  roadType: RoadType;
  vehicleVolume: number;
  eventType: string | null;
  weather: string | null;
  region: string | null;
}

export interface CreateTrafficRecordRequest {
  timestamp: string;
  roadType: RoadType;
  vehicleVolume: number;
  eventType: string;
  weather: string;
  region: string;
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
  region: string;
  lat: number;
  lng: number;
}