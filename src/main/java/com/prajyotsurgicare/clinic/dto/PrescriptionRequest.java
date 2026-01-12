package com.prajyotsurgicare.clinic.dto;

import lombok.Data;

@Data
public class PrescriptionRequest {
    private String textNote;      // डॉक्टरांनी टाईप केलेले
    private String imageBase64;   // Stylus चे ड्रॉइंग
}