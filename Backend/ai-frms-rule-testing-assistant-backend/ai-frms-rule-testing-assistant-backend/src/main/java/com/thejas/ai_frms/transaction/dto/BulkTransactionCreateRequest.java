package com.thejas.ai_frms.transaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BulkTransactionCreateRequest {

    @NotEmpty(message = "Transaction list cannot be empty")
    @Valid
    private List<TransactionCreateRequest> transactions;

    public List<TransactionCreateRequest> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionCreateRequest> transactions) {
        this.transactions = transactions;
    }
}