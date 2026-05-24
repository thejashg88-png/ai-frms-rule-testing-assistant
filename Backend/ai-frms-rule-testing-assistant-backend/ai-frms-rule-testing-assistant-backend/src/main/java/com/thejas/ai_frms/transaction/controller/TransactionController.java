package com.thejas.ai_frms.transaction.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.transaction.dto.BulkTransactionCreateRequest;
import com.thejas.ai_frms.transaction.dto.TransactionCreateRequest;
import com.thejas.ai_frms.transaction.dto.TransactionResponse;
import com.thejas.ai_frms.transaction.dto.TransactionSearchRequest;
import com.thejas.ai_frms.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPathConstants.TRANSACTIONS)
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionCreateRequest request
    ) {
        TransactionResponse response = transactionService.createTransaction(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", response));
    }

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

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long transactionId) {
        transactionService.deleteTransaction(transactionId);

        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully"));
    }
}