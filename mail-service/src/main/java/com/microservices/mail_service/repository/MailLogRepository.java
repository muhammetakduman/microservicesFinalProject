package com.microservices.mail_service.repository;

import com.microservices.mail_service.entity.MailLog;
import com.microservices.mail_service.entity.MailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MailLogRepository extends JpaRepository<MailLog, Long> {
    List<MailLog> findTop50ByOrderByCreatedAtDesc();
    List<MailLog> findByStatus(MailStatus status);
}

