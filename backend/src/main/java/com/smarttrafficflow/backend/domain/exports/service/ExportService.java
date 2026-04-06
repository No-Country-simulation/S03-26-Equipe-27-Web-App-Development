package com.smarttrafficflow.backend.domain.exports.service;

import com.smarttrafficflow.backend.api.dto.TrafficRecordResponse;
import com.smarttrafficflow.backend.domain.trafficrecords.service.TrafficRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final TrafficRecordService trafficRecordService;

    public ExportService(TrafficRecordService trafficRecordService) {
        this.trafficRecordService = trafficRecordService;
    }

    public String exportCsv() {
        List<TrafficRecordResponse> records = trafficRecordService.findAll();
        log.info("Exporting {} records to CSV", records.size());

        StringBuilder sb = new StringBuilder();
        sb.append("id,timestamp,roadType,vehicleVolume,eventType,weather,region\n");

        for (TrafficRecordResponse record : records) {
            sb.append(escapeCsv(record.id().toString())).append(',')
                    .append(escapeCsv(record.timestamp().toString())).append(',')
                    .append(escapeCsv(record.roadType())).append(',')
                    .append(record.vehicleVolume()).append(',')
                    .append(escapeCsv(record.eventType())).append(',')
                    .append(escapeCsv(record.weather())).append(',')
                    .append(escapeCsv(record.region()))
                    .append('\n');
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        boolean needsQuoting = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");
        if (needsQuoting) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}