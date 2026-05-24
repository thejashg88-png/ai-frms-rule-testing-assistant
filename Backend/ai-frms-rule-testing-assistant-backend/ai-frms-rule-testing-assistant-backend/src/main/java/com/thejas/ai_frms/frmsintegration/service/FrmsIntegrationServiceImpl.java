package com.thejas.ai_frms.frmsintegration.service;

import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.frmsintegration.client.FrmsClient;
import com.thejas.ai_frms.frmsintegration.dto.FrmsRiskEvaluationResponse;
import com.thejas.ai_frms.frmsintegration.dto.FrmsRuleResponse;
import com.thejas.ai_frms.frmsintegration.dto.FrmsTransactionRequest;
import com.thejas.ai_frms.testcase.dto.TestInputData;
import org.springframework.stereotype.Service;

@Service
public class FrmsIntegrationServiceImpl implements FrmsIntegrationService {

    private final FrmsClient frmsClient;

    public FrmsIntegrationServiceImpl(FrmsClient frmsClient) {
        this.frmsClient = frmsClient;
    }

    @Override
    public FrmsRiskEvaluationResponse evaluateRisk(FrmsTransactionRequest request) {
        if (request == null) {
            throw new BadRequestException("FRMS transaction request cannot be null");
        }

        return frmsClient.evaluateRisk(request);
    }

    @Override
    public FrmsRiskEvaluationResponse evaluateRiskFromTestInput(TestInputData inputData) {
        if (inputData == null) {
            throw new BadRequestException("Test input data cannot be null");
        }

        FrmsTransactionRequest request = mapToFrmsTransactionRequest(inputData);

        return frmsClient.evaluateRisk(request);
    }

    @Override
    public FrmsRuleResponse getRuleById(Long ruleId) {
        if (ruleId == null) {
            throw new BadRequestException("Rule id cannot be null");
        }

        return frmsClient.getRuleById(ruleId);
    }

    private FrmsTransactionRequest mapToFrmsTransactionRequest(TestInputData inputData) {
        FrmsTransactionRequest request = new FrmsTransactionRequest();

        request.setRrn(inputData.getRrn());
        request.setStan(inputData.getStan());
        request.setSerialNumber(inputData.getSerialNumber());
        request.setTrack2Data(inputData.getTrack2Data());
        request.setTid(inputData.getTid());
        request.setMid(inputData.getMid());
        request.setMccCode(inputData.getMccCode());
        request.setAmount(inputData.getAmount());
        request.setCurrency(inputData.getCurrency());
        request.setTransactionType(inputData.getTransactionType());
        request.setResponseCode(inputData.getResponseCode());
        request.setResponseMessage(inputData.getResponseMessage());
        request.setTransactionStatus(inputData.getTransactionStatus());
        request.setTransactionTime(inputData.getTransactionTime());

        return request;
    }
}