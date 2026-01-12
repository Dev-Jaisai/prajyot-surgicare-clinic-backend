package com.prajyotsurgicare.clinic.controller;

import com.prajyotsurgicare.clinic.enums.VisitType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/visit-types")
public class VisitTypeController {

    @GetMapping
    public List<String> getVisitTypes() {

        log.info("Fetching visit types");

        return Arrays.stream(VisitType.values())
                .map(Enum::name)
                .toList();
    }
}
