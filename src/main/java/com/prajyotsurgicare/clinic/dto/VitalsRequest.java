package com.prajyotsurgicare.clinic.dto;

import lombok.Data;

@Data
public class VitalsRequest {
    private String bp;
    private String temperature;
    private String pulse;
    private String weight;
}