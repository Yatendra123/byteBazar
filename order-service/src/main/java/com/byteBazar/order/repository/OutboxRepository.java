package com.byteBazar.order.repository;

import com.byteBazar.order.domain.OutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {
    List<OutboxEntity> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);
}
