package com.saapadu.admin.controller;


import com.saapadu.admin.SignUpRequest;
import com.saapadu.admin.service.DashboardService;
import com.saapadu.admin.service.SignInService;
import com.saapadu.admin.service.SignUpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://192.168.1.1:3000", "http://192.168.1.2:3000", "http://192.168.1.3:3000", "http://192.168.1.4:3000", "http://192.168.1.5:3000", "http://192.168.1.6:3000", "http://192.168.1.7:3000", "http://192.168.1.8:3000", "http://192.168.1.9:3000"})
public class Controller {
    @Autowired
    private SignUpService signUpService;
    @Autowired
    private SignInService signInService;
    @Autowired
    private DashboardService dashboardService;

    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    @PostMapping("/signUp")
    public CompletableFuture<ResponseEntity<Object>> signUp(@RequestBody SignUpRequest request) {
        return signUpService.createOrganization(request)
                // Add a type witness <Object> to guide the compiler to the correct generic type.
                .thenApply(collegeId -> ResponseEntity.<Object>ok(Map.of(
                        "message", "Organization created successfully.",
                        "college_id", collegeId
                )))
                .exceptionally(ex -> {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                            "message", "Error creating organization.",
                            "error", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()
                    ));
                });

    }
    
    @GetMapping("/signIn")
    public CompletableFuture<ResponseEntity<?>> signIn(@RequestParam String collegeName) {
        return signInService.signInWithCollegeName(collegeName)
                .thenApply(collegeId -> {
                    if (collegeId != null) {
                        // College found, return success response with college details
                        // By using a type witness (<Object>), we help the compiler resolve the generic type
                        // to ResponseEntity<?>, which matches the method's return signature.
                        return ResponseEntity.<Object>ok(
                                Map.of("college_id", collegeId, "college_name", collegeName));
                    } else {
                        // College not found
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "College not found"));
                    }
                })
                .exceptionally(ex -> {
                    // Handle exceptions during the async operation
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error during sign-in: " + ex.getCause().getMessage()));
                });
    }

    /**
     * A generic helper to wrap a CompletableFuture result in a ResponseEntity,
     * providing centralized and consistent error handling for dashboard endpoints.
     */
    private <T> CompletableFuture<ResponseEntity<?>> createDashboardResponse(CompletableFuture<T> future) {
        return future.<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Dashboard API error", ex); // Log the full exception for backend debugging
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("message", "Failed to retrieve dashboard data.", "error", ex.getMessage()));
                });
    }

    @GetMapping("/home/total-orders")
    public CompletableFuture<ResponseEntity<?>> fetchTotalOrdersByCollegeId(@RequestParam Long collegeId){
        return createDashboardResponse(dashboardService.getTotalOrdersByCollegeId(collegeId));
    }
    
    @GetMapping("/home/pending-orders")
    public CompletableFuture<ResponseEntity<?>> fetchPendingOrdersByCollegeId(@RequestParam Long collegeId){
        return createDashboardResponse(dashboardService.getPendingOrdersByCollegeId(collegeId));
    }

    @GetMapping("/home/completed-orders")
    public CompletableFuture<ResponseEntity<?>> fetchCompletedOrdersByCollegeId(@RequestParam Long collegeId){
        return createDashboardResponse(dashboardService.getCompletedOrdersByCollegeId(collegeId));
    }

    @GetMapping("/home/total-revenue")
    public CompletableFuture<ResponseEntity<?>> fetchTotalRevenueByCollegeId(@RequestParam Long collegeId){
        return createDashboardResponse(dashboardService.getTotalRevenueByCollegeId(collegeId));
    }
}
