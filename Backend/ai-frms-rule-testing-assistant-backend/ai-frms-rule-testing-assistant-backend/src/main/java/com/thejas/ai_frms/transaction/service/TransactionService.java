package com.thejas.ai_frms.transaction.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.transaction.dto.BulkTransactionCreateRequest;
import com.thejas.ai_frms.transaction.dto.TransactionCreateRequest;
import com.thejas.ai_frms.transaction.dto.TransactionResponse;
import com.thejas.ai_frms.transaction.dto.TransactionSearchRequest;

import java.util.List;

public interface TransactionService {

    TransactionResponse createTransaction(TransactionCreateRequest request);

    List<TransactionResponse> createBulkTransactions(BulkTransactionCreateRequest request);

    TransactionResponse getTransactionById(Long transactionId);

    PageResponse<TransactionResponse> searchTransactions(TransactionSearchRequest request);

    void deleteTransaction(Long transactionId);
}