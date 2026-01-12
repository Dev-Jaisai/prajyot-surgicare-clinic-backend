package com.prajyotsurgicare.clinic.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class AnalyticsStats {
    private Double totalRevenue;
    private Double consultationRevenue;
    private Double procedureRevenue;
    private Map<String, Long> procedureCounts; // e.g. "X-Ray": 10
    private Map<String, Long> doctorPatientCounts; // e.g. "Dr. Nikhil": 50
    private Map<String, Double> doctorRevenue; // e.g. "Dr. Nikhil": 25000
}