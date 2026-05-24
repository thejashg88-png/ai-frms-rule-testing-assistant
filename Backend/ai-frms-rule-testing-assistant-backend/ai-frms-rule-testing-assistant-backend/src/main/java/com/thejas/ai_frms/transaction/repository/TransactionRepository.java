package com.thejas.ai_frms.transaction.repository;

import com.thejas.ai_frms.transaction.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>, JpaSpecificationExecutor<TransactionEntity> {

    List<TransactionEntity> findBySerialNumber(String serialNumber);

    List<TransactionEntity> findByMccCode(String mccCode);

    List<TransactionEntity> findBySerialNumberAndTransactionTimeBetween(
            String serialNumber,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    List<TransactionEntity> findByTrack2DataAndTransactionTimeBetween(
            String track2Data,
            LocalDateTime startTime,
            LocalDateTime endTime
    );
}