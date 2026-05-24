package com.thejas.ai_frms.transaction.mapper;

import com.thejas.ai_frms.common.util.MaskingUtil;
import com.thejas.ai_frms.transaction.dto.TransactionCreateRequest;
import com.thejas.ai_frms.transaction.dto.TransactionResponse;
import com.thejas.ai_frms.transaction.entity.TransactionEntity;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionEntity toEntity(TransactionCreateRequest request) {
        TransactionEntity entity = new TransactionEntity();

        entity.setRrn(request.getRrn());
        entity.setStan(request.getStan());
        entity.setSerialNumber(request.getSerialNumber());
        entity.setTrack2Data(request.getTrack2Data());
        entity.setTid(request.getTid());
        entity.setMid(request.getMid());
        entity.setMccCode(request.getMccCode());
        entity.setAmount(request.getAmount());
        entity.setCurrency(request.getCurrency());
        entity.setTransactionType(request.getTransactionType());
        entity.setResponseCode(request.getResponseCode());
        entity.setResponseMessage(request.getResponseMessage());
        entity.setTransactionStatus(request.getTransactionStatus());
        entity.setTransactionTime(request.getTransactionTime());
        entity.setCreatedBy(request.getCreatedBy());

        return entity;
    }

    public static TransactionResponse toResponse(TransactionEntity entity) {
        TransactionResponse response = new TransactionResponse();

        response.setTransactionId(entity.getTransactionId());
        response.setRrn(entity.getRrn());
        response.setStan(entity.getStan());
        response.setSerialNumber(entity.getSerialNumber());
        response.setMaskedTrack2Data(MaskingUtil.maskTrack2(entity.getTrack2Data()));
        response.setTid(entity.getTid());
        response.setMid(entity.getMid());
        response.setMccCode(entity.getMccCode());
        response.setAmount(entity.getAmount());
        response.setCurrency(entity.getCurrency());
        response.setTransactionType(entity.getTransactionType());
        response.setResponseCode(entity.getResponseCode());
        response.setResponseMessage(entity.getResponseMessage());
        response.setTransactionStatus(entity.getTransactionStatus());
        response.setTransactionTime(entity.getTransactionTime());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}