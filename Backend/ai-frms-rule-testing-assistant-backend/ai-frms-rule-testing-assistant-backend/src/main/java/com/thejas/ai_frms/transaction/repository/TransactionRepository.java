package com.thejas.ai_frms.transaction.repository;

import com.thejas.ai_frms.transaction.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>, JpaSpecificationExecutor<TransactionEntity> {

    List<TransactionEntity> findBySerialNumber(String serialNumber);

    List<TransactionEntity> findByTrack2Data(String track2Data);

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

    @Query("SELECT t FROM TransactionEntity t WHERE t.track2Data LIKE %:last4 AND t.transactionTime BETWEEN :startTime AND :endTime")
    List<TransactionEntity> findByTrack2DataEndingWithAndTransactionTimeBetween(
            @Param("last4") String last4,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    List<TransactionEntity> findByTidAndTransactionTimeBetween(
            String tid,
            LocalDateTime startTime,
            LocalDateTime endTime
    );
}