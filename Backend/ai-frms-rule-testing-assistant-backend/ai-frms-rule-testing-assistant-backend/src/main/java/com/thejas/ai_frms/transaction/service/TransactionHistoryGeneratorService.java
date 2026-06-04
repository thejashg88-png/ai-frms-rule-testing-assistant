package com.thejas.ai_frms.transaction.service;

import com.thejas.ai_frms.transaction.dto.GenerateHistoryRequest;
import com.thejas.ai_frms.transaction.dto.GenerateHistoryResponse;

public interface TransactionHistoryGeneratorService {

    GenerateHistoryResponse generateHistory(GenerateHistoryRequest request);
}