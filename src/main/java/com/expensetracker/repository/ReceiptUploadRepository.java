package com.expensetracker.repository;

import com.expensetracker.entities.ReceiptUploadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptUploadRepository extends JpaRepository<ReceiptUploadEntity, String> {

    List<ReceiptUploadEntity> findByRequestId(String requestId);
}
