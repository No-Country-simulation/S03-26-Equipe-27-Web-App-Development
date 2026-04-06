package com.smarttrafficflow.backend.domain.trafficrecords.repository;

import com.smarttrafficflow.backend.domain.trafficrecords.entity.TrafficRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TrafficRecordRepository extends JpaRepository<TrafficRecord, UUID> {

    @Query("SELECT DISTINCT r.region FROM TrafficRecord r WHERE r.region IS NOT NULL AND r.region <> ''")
    List<String> findDistinctRegions();
}