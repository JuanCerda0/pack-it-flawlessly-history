package dev.noby.history.repository;

import dev.noby.history.entity.PackageHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PackageHistoryRepository extends JpaRepository<PackageHistoryEntity, UUID> {
    List<PackageHistoryEntity> findByPackageIdOrderByOccurredAtAscEventIdAsc(Long packageId);
}
