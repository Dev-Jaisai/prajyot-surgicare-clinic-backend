package com.prajyotsurgicare.clinic.controller;

import com.prajyotsurgicare.clinic.entity.Visit;
import com.prajyotsurgicare.clinic.enums.VisitStatus;
import com.prajyotsurgicare.clinic.enums.VisitType; // ✅ Import VisitType
import com.prajyotsurgicare.clinic.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final VisitRepository visitRepository;

    // 1. SUMMARY STATS (Cards & Graphs)
    @GetMapping("/summary")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "X-CLINIC-ID", defaultValue = "1") Long clinicId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate today = LocalDate.now();
        if (startDate == null) startDate = YearMonth.from(today).atDay(1);
        if (endDate == null) endDate = YearMonth.from(today).atEndOfMonth();

        Map<String, Object> stats = new HashMap<>();

        // 1. Basic Cards Data (Daily, Monthly, Yearly)
        stats.put("dailyIncome", getCollection(today, today, clinicId, doctorId));
        stats.put("dailyVisits", getVisits(today, today, clinicId, doctorId));

        stats.put("monthlyIncome", getCollection(YearMonth.from(today).atDay(1), YearMonth.from(today).atEndOfMonth(), clinicId, doctorId));
        stats.put("monthlyVisits", getVisits(YearMonth.from(today).atDay(1), YearMonth.from(today).atEndOfMonth(), clinicId, doctorId));

        stats.put("yearlyIncome", getCollection(today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()), clinicId, doctorId));
        stats.put("yearlyVisits", getVisits(today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()), clinicId, doctorId));

        // 2. Filtered Data (For Graphs & Breakdown)
        List<Visit> filteredVisits;
        if (doctorId != null) {
            filteredVisits = visitRepository.findByVisitDateBetweenAndClinicIdAndDoctorIdAndStatus(
                    startDate, endDate, clinicId, doctorId, VisitStatus.COMPLETED);
        } else {
            filteredVisits = visitRepository.findByVisitDateBetweenAndClinicIdAndStatus(
                    startDate, endDate, clinicId, VisitStatus.COMPLETED);
        }

        // 3. Income Breakdown (Consultation vs Procedures)
        double totalConsultation = filteredVisits.stream().mapToDouble(v -> v.getConsultationFee() != null ? v.getConsultationFee() : 0).sum();
        double totalProcedures = filteredVisits.stream().mapToDouble(v -> v.getOtherCharges() != null ? v.getOtherCharges() : 0).sum();

        stats.put("filteredRevenue", totalConsultation + totalProcedures);
        stats.put("incomeBreakdown", Map.of("Consultation", totalConsultation, "Procedures", totalProcedures));

        // 🔥🔥🔥 4. NEW: Patient Ratio Logic (For Pie Chart) 🔥🔥🔥
        long newPatients = filteredVisits.stream()
                .filter(v -> v.getVisitType() != null &&
                        (v.getVisitType() == VisitType.OPD || v.getVisitType() == VisitType.ON_CALL)) // OPD + OnCall = NEW
                .count();

        long followUpPatients = filteredVisits.stream()
                .filter(v -> v.getVisitType() != null && v.getVisitType() == VisitType.FOLLOW_UP) // FollowUp = OLD
                .count();

        stats.put("patientRatio", Map.of("New", newPatients, "FollowUp", followUpPatients));
        // -----------------------------------------------------------

        // 5. Doctor Performance
        Map<String, Double> doctorPerformance = filteredVisits.stream()
                .filter(v -> v.getDoctor() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getDoctor().getName(),
                        Collectors.summingDouble(v -> v.getTotalAmount() != null ? v.getTotalAmount() : 0)
                ));
        stats.put("doctorPerformance", doctorPerformance);

        // 6. Daily Trend (Graph)
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        Map<LocalDate, List<Visit>> visitsByDate = filteredVisits.stream().collect(Collectors.groupingBy(Visit::getVisitDate));

        visitsByDate.forEach((date, visits) -> {
            double dayTotal = visits.stream().mapToDouble(v -> v.getTotalAmount() != null ? v.getTotalAmount() : 0).sum();
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", date);
            dayStat.put("total", dayTotal);
            dayStat.put("count", visits.size());
            dailyTrend.add(dayStat);
        });
        dailyTrend.sort(Comparator.comparing(m -> (LocalDate) m.get("date")));
        stats.put("dailyTrend", dailyTrend);

        return ResponseEntity.ok(stats);
    }

    // 2. DRILL-DOWN LIST API
    @GetMapping("/visit-list")
    public ResponseEntity<List<Map<String, Object>>> getVisitList(
            @RequestHeader(value = "X-CLINIC-ID", defaultValue = "1") Long clinicId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Visit> visits;
        if (doctorId != null) {
            visits = visitRepository.findByVisitDateBetweenAndClinicIdAndDoctorIdAndStatus(
                    startDate, endDate, clinicId, doctorId, VisitStatus.COMPLETED);
        } else {
            visits = visitRepository.findByVisitDateBetweenAndClinicIdAndStatus(
                    startDate, endDate, clinicId, VisitStatus.COMPLETED);
        }

        List<Map<String, Object>> response = visits.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("token", v.getTokenNumber());
            map.put("patientName", v.getPatient().getName());
            map.put("doctorName", v.getDoctor().getName());
            map.put("amount", v.getTotalAmount());
            map.put("visitType", v.getVisitType());
            map.put("date", v.getVisitDate());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // --- Helper Methods ---

    private Double getCollection(LocalDate start, LocalDate end, Long clinicId, Long doctorId) {
        Double collection;
        if (doctorId != null) {
            collection = (start.equals(end))
                    ? visitRepository.getDailyCollectionByDoctor(start, clinicId, doctorId)
                    : visitRepository.getMonthlyCollectionByDoctor(start, end, clinicId, doctorId);
        } else {
            collection = (start.equals(end))
                    ? visitRepository.getDailyCollection(start, clinicId)
                    : visitRepository.getMonthlyCollection(start, end, clinicId);
        }
        return collection != null ? collection : 0.0;
    }

    private long getVisits(LocalDate start, LocalDate end, Long clinicId, Long doctorId) {
        long count;
        // Using Standard Counts (If you haven't updated Repo with 'Physical' queries yet, this works safely)
        if (doctorId != null) {
            count = (start.equals(end))
                    ? visitRepository.countByVisitDateAndClinicIdAndDoctorIdAndStatus(start, clinicId, doctorId, VisitStatus.COMPLETED)
                    : visitRepository.getMonthlyVisitsByDoctor(start, end, clinicId, doctorId);
        } else {
            count = (start.equals(end))
                    ? visitRepository.countByVisitDateAndClinicIdAndStatus(start, clinicId, VisitStatus.COMPLETED)
                    : visitRepository.getMonthlyVisits(start, end, clinicId);
        }
        return count;
    }

    // --- Follow Ups ---

    @GetMapping("/followups")
    public ResponseEntity<List<Map<String, Object>>> getTodayFollowUps(
            @RequestParam Long doctorId,
            @RequestParam Long clinicId) {

        LocalDate today = LocalDate.now();
        List<Visit> followUps = visitRepository.findByFollowUpDateAndClinicIdAndDoctorId(today, clinicId, doctorId);

        List<Map<String, Object>> response = followUps.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("patientName", v.getPatient().getName());
            map.put("mobile", v.getPatient().getMobile());
            map.put("lastDiagnosis", v.getDiagnosis());
            map.put("lastVisitDate", v.getVisitDate());
            map.put("doctorName", v.getDoctor().getName());
            map.put("clinicName", v.getClinic().getName());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}