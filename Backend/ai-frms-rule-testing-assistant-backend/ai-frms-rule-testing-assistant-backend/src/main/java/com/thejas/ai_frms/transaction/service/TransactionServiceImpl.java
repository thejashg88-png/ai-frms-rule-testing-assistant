package com.thejas.ai_frms.transaction.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.transaction.dto.BulkTransactionCreateRequest;
import com.thejas.ai_frms.transaction.dto.TransactionCreateRequest;
import com.thejas.ai_frms.transaction.dto.TransactionResponse;
import com.thejas.ai_frms.transaction.dto.TransactionSearchRequest;
import com.thejas.ai_frms.transaction.entity.TransactionEntity;
import com.thejas.ai_frms.transaction.mapper.TransactionMapper;
import com.thejas.ai_frms.transaction.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionCreateRequest request) {
        TransactionEntity entity = TransactionMapper.toEntity(request);
        TransactionEntity savedEntity = transactionRepository.save(entity);

        return TransactionMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional
    public List<TransactionResponse> createBulkTransactions(BulkTransactionCreateRequest request) {
        List<TransactionEntity> entities = request.getTransactions()
                .stream()
                .map(TransactionMapper::toEntity)
                .toList();

        List<TransactionEntity> savedEntities = transactionRepository.saveAll(entities);

        return savedEntities.stream()
                .map(TransactionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long transactionId) {
        TransactionEntity entity = getTransactionEntity(transactionId);
        return TransactionMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> searchTransactions(TransactionSearchRequest request) {
        Pageable pageable = buildPageable(request);

        Page<TransactionResponse> responsePage = transactionRepository
                .findAll(buildSpecification(request), pageable)
                .map(TransactionMapper::toResponse);

        return PageResponse.fromPage(responsePage);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long transactionId) {
        TransactionEntity entity = getTransactionEntity(transactionId);
        transactionRepository.delete(entity);
    }

    private TransactionEntity getTransactionEntity(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
    }

    private Pageable buildPageable(TransactionSearchRequest request) {
        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() <= 0 ? 10 : request.getSize();

        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, request.getSortBy()));
    }

    private Specification<TransactionEntity> buildSpecification(TransactionSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getSerialNumber() != null && !request.getSerialNumber().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("serialNumber"), request.getSerialNumber()));
            }

            if (request.getRrn() != null && !request.getRrn().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("rrn"), request.getRrn()));
            }

            if (request.getTid() != null && !request.getTid().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("tid"), request.getTid()));
            }

            if (request.getMid() != null && !request.getMid().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("mid"), request.getMid()));
            }

            if (request.getMccCode() != null && !request.getMccCode().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("mccCode"), request.getMccCode()));
            }

            if (request.getTransactionType() != null && !request.getTransactionType().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("transactionType"), request.getTransactionType()));
            }

            if (request.getTransactionStatus() != null && !request.getTransactionStatus().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("transactionStatus"), request.getTransactionStatus()));
            }

            if (request.getMinAmount() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), request.getMinAmount()));
            }

            if (request.getMaxAmount() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), request.getMaxAmount()));
            }

            if (request.getFromTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionTime"), request.getFromTime()));
            }

            if (request.getToTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionTime"), request.getToTime()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}