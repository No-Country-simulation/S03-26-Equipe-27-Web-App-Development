package com.smarttrafficflow.backend.domain.analytics;

import com.smarttrafficflow.backend.api.dto.TrafficStatsResponse;
import com.smarttrafficflow.backend.domain.trafficrecords.entity.TrafficRecord;
import com.smarttrafficflow.backend.domain.trafficrecords.service.TrafficRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final TrafficRecordService trafficRecordService;

    public AnalyticsService(TrafficRecordService trafficRecordService) {
        this.trafficRecordService = trafficRecordService;
    }

    public TrafficStatsResponse getStats(String groupBy) {
        List<TrafficRecord> records = trafficRecordService.findAllEntities();
        log.info("Analytics requested with groupBy={} using {} records", groupBy, records.size());
        Map<String, Integer> aggregated = new LinkedHashMap<>();

        for (TrafficRecord record : records) {
            String key = switch (groupBy) {
                case "hour" -> String.format("%02d", record.getTimestamp().getHour());
                case "weekday" -> normalizeWeekday(record.getTimestamp().getDayOfWeek());
                case "roadType" -> safeValue(record.getRoadType(), "UNKNOWN");
                default -> {
                    log.warn("Analytics requested with invalid groupBy={}", groupBy);
                    throw new IllegalArgumentException("groupBy invalido: " + groupBy);
                }
            };

            aggregated.merge(key, record.getVehicleVolume(), Integer::sum);
        }

        log.info("Analytics generated {} buckets for groupBy={}", aggregated.size(), groupBy);
        return new TrafficStatsResponse(new ArrayList<>(aggregated.keySet()), new ArrayList<>(aggregated.values()));
    }

    private String normalizeWeekday(DayOfWeek dayOfWeek) {
        return dayOfWeek.name();
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
