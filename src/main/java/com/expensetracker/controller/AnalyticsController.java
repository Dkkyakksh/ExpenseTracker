package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.SavingsProgressDTO;
import com.expensetracker.dto.SetBudgetRequestDTO;
import com.expensetracker.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final BudgetService budgetService;

    // GET /api/analytics/savings-progress
    @GetMapping("/savings-progress")
    public ResponseEntity<ApiResponse<SavingsProgressDTO>> getSavingsProgress(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(ApiResponse.ok(budgetService.getSavingsProgress(months)));
    }

    // POST /api/analytics/budget  — set or update a month's planned salary
    @PostMapping("/budget")
    public ResponseEntity<ApiResponse<Void>> setMonthlyBudget(
            @Validated @RequestBody SetBudgetRequestDTO request) {
        budgetService.setMonthlyBudget(request);
        return ResponseEntity.ok(ApiResponse.ok("Budget set successfully"));
    }

    // POST /api/analytics/sweep  — trigger month-end rollover manually
    @PostMapping("/sweep")
    public ResponseEntity<ApiResponse<Void>> triggerSweep() {
        budgetService.performMonthEndSweep();
        return ResponseEntity.ok(ApiResponse.ok("Month-end sweep completed"));
    }
}
