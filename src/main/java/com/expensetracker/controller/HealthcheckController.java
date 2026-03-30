package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HealthcheckController {

    @GetMapping(value="/ping")
    public ResponseEntity<ApiResponse<Object>> isServiceUp() {
        return ResponseEntity.ok(ApiResponse.ok("Service is up and running"));
    }

}
