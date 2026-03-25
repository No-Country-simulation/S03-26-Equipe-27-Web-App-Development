package com.smarttrafficflow.backend.domain.insights.service;

import com.smarttrafficflow.backend.api.dto.TrafficInsightResponse;
import com.smarttrafficflow.backend.domain.trafficrecords.entity.TrafficRecord;
import com.smarttrafficflow.backend.domain.trafficrecords.service.TrafficRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InsightService {

    private static final Logger log = LoggerFactory.getLogger(InsightService.class);

    private final TrafficRecordService trafficRecordService;

    public InsightService(TrafficRecordService trafficRecordService) {
        this.trafficRecordService = trafficRecordService;
    }

    public TrafficInsightResponse generateInsights() {
        List<TrafficRecord> records = trafficRecordService.findAllEntities();
        List<String> insights = new ArrayList<>();
        log.info("Generating insights from {} records", records.size());

        if (records.isEmpty()) {
            insights.add("Sem dados suficientes para gerar insights.");
            log.warn("Insight generation requested without data");
            return new TrafficInsightResponse(insights);
        }

        Map<Integer, Integer> byHour = new HashMap<>();
        Map<String, Integer> byRoadType = new HashMap<>();

        for (TrafficRecord record : records) {
            byHour.merge(record.getTimestamp().getHour(), record.getVehicleVolume(), Integer::sum);
            byRoadType.merge(safeValue(record.getRoadType(), "UNKNOWN"), record.getVehicleVolume(), Integer::sum);
        }

        Map.Entry<Integer, Integer> peakHour = byHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        Map.Entry<String, Integer> peakRoadType = byRoadType.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .orElseThrow();

        insights.add("Horario com maior volume agregado: " + peakHour.getKey() + "h.");
        insights.add("Tipo de via com maior volume agregado: " + peakRoadType.getKey() + ".");

        log.info("Generated {} insights (peakHour={}h, peakRoadType={})",
                insights.size(), peakHour.getKey(), peakRoadType.getKey());
        return new TrafficInsightResponse(insights);
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
