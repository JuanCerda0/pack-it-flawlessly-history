package dev.noby.history.mapper;

import dev.noby.history.dto.PackageHistoryResponse;
import dev.noby.history.entity.PackageHistoryEntity;
import org.springframework.stereotype.Component;

@Component
public class PackageHistoryMapper {

    public PackageHistoryResponse toResponse(PackageHistoryEntity entity) {
        return new PackageHistoryResponse(entity.getEventId(), entity.getPackageId(), entity.getEventType(),
                entity.getOccurredAt(), entity.getActorId(), entity.getBeforeData(), entity.getAfterData());
    }
}
