package com.thejas.ai_frms.transaction.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.transaction.dto.BulkTransactionCreateRequest;
import com.thejas.ai_frms.transaction.dto.GenerateHistoryRequest;
import com.thejas.ai_frms.transaction.dto.GenerateHistoryResponse;
import com.thejas.ai_frms.transaction.dto.TransactionCreateRequest;
import com.thejas.ai_frms.transaction.dto.TransactionResponse;
import com.thejas.ai_frms.transaction.dto.TransactionSearchRequest;
import com.thejas.ai_frms.transaction.service.TransactionHistoryGeneratorService;
import com.thejas.ai_frms.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for transaction management.
 *
 * Role access:
 *   ADMIN  — create, delete, generate-history, view
 *   TESTER — generate-history, view
 *   VIEWER — view only
 */
@RestController
@RequestMapping(ApiPathConstants.TRANSACTIONS)
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionHistoryGeneratorService historyGeneratorService;

    public TransactionController(
            TransactionService transactionService,
            TransactionHistoryGeneratorService historyGeneratorService) {
        this.transactionService = transactionService;
        this.historyGeneratorService = historyGeneratorService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionCreateRequest request
    ) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<Object>> createBulkTransactions(
            @Valid @RequestBody BulkTransactionCreateRequest request
    ) {
        Object response = transactionService.createBulkTransactions(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transactions created successfully", response));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable Long transactionId
    ) {
        TransactionResponse response = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Transaction fetched successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> searchTransactions(
            @ModelAttribute TransactionSearchRequest request
    ) {
        PageResponse<TransactionResponse> response = transactionService.searchTransactions(request);
        return ResponseEntity.ok(ApiResponse.success("Transactions fetched successfully", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long transactionId) {
        transactionService.deleteTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully"));
    }

    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @PostMapping("/generate-history")
    public ResponseEntity<ApiResponse<GenerateHistoryResponse>> generateHistory(
            @RequestBody GenerateHistoryRequest request
    ) {
        GenerateHistoryResponse response = historyGeneratorService.generateHistory(request);
        return ResponseEntity.ok(ApiResponse.success("History generation completed", response));
    }
}