package com.purrbyte.ai.repository;

import com.purrbyte.ai.domain.JdkDocChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JdkDocChunkRepository extends JpaRepository<JdkDocChunk, UUID> {
}
