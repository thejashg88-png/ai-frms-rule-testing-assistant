package com.thejas.ai_frms.transaction.repository;

import com.thejas.ai_frms.transaction.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>, JpaSpecificationExecutor<TransactionEntity> {

    // Fetch full transaction history by serial number — used by UNUSUAL_AMT baseline calculation
    List<TransactionEntity> findBySerialNumber(String serialNumber);

    // Fetch full history by track2Data (magnetic stripe) — fallback when serialNumber yields nothing
    List<TransactionEntity> findByTrack2Data(String track2Data);

    // Used by INCONSISTENT_MCC rule evaluation to check transaction MCC history
    List<TransactionEntity> findByMccCode(String mccCode);

    // Fetch transactions within a time window by serial number — used by HIGH_FREQ and SEQUENTIAL_TXN
    List<TransactionEntity> findBySerialNumberAndTransactionTimeBetween(
            String serialNumber,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // Fetch window transactions by track2Data — fallback for HIGH_FREQ / SEQUENTIAL_TXN
    List<TransactionEntity> findByTrack2DataAndTransactionTimeBetween(
            String track2Data,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // Used by SEQUENTIAL_TXN for masked card numbers — matches by last 4 digits of track2Data
    @Query("SELECT t FROM TransactionEntity t WHERE t.track2Data LIKE %:last4 AND t.transactionTime BETWEEN :startTime AND :endTime")
    List<TransactionEntity> findByTrack2DataEndingWithAndTransactionTimeBetween(
            @Param("last4") String last4,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Fallback by TID (terminal ID) when no card/serial identifier matches
    List<TransactionEntity> findByTidAndTransactionTimeBetween(
            String tid,
            LocalDateTime startTime,
            LocalDateTime endTime
    );
}