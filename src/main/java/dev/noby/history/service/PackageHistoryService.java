package dev.noby.history.service;

import dev.noby.history.dto.HistoryEventType;
import dev.noby.history.dto.PackageHistoryMessage;
import dev.noby.history.dto.PackageHistoryResponse;
import dev.noby.history.entity.PackageHistoryEntity;
import dev.noby.history.exception.InvalidHistoryMessageException;
import dev.noby.history.mapper.PackageHistoryMapper;
import dev.noby.history.repository.PackageHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PackageHistoryService {

    private final PackageHistoryRepository repository;
    private final PackageHistoryMapper mapper;
    private final JsonMapper jsonMapper;

    public PackageHistoryService(PackageHistoryRepository repository, PackageHistoryMapper mapper,
                                 JsonMapper jsonMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.jsonMapper = jsonMapper;
    }

    public PackageHistoryMessage parse(byte[] body) {
        PackageHistoryMessage message;
        try {
            message = jsonMapper.readValue(new String(body, StandardCharsets.UTF_8), PackageHistoryMessage.class);
        } catch (JacksonException error) {
            throw new InvalidHistoryMessageException("El mensaje no cumple el contrato JSON de Historial", error);
        }
        validate(message);
        return message;
    }

    @Transactional
    public void record(PackageHistoryMessage message) {
        if (repository.existsById(message.eventId())) {
            return;
        }
        repository.saveAndFlush(new PackageHistoryEntity(message.eventId(), message.packageId(), message.type(),
                message.occurredAt(), message.actorId(), message.before(), message.after()));
    }

    public List<PackageHistoryResponse> getHistory(Long packageId) {
        return repository.findByPackageIdOrderByOccurredAtAscEventIdAsc(packageId)
                .stream().map(mapper::toResponse).toList();
    }

    private void validate(PackageHistoryMessage message) {
        if (message == null || message.schemaVersion() != 1 || message.eventId() == null
                || message.packageId() == null || message.packageId() <= 0
                || message.occurredAt() == null || message.type() == null || message.after() == null) {
            throw new InvalidHistoryMessageException("Faltan campos requeridos o schemaVersion no es 1");
        }
        if (message.actorId() != null && message.actorId().length() > 255) {
            throw new InvalidHistoryMessageException("actorId excede 255 caracteres");
        }
        HistoryEventType type = message.type();
        if (type == HistoryEventType.PACKAGE_STATUS_CHANGED) {
            requireString(message.before(), "status");
            requireString(message.after(), "status");
        } else if (type == HistoryEventType.PACKAGE_ITEM_ADDED
                || type == HistoryEventType.PACKAGE_ITEM_UPDATED) {
            requireItem(message.after());
            if (type == HistoryEventType.PACKAGE_ITEM_UPDATED) {
                requireItem(message.before());
            }
        } else {
            requireString(message.after(), "name");
            if (type == HistoryEventType.PACKAGE_CREATED) {
                requireString(message.after(), "status");
            }
            if (type == HistoryEventType.PACKAGE_UPDATED) {
                requireString(message.before(), "name");
            }
        }
    }

    private void requireItem(Map<String, Object> details) {
        if (details == null || !(details.get("itemId") instanceof Number itemId)
                || itemId.longValue() <= 0) {
            throw new InvalidHistoryMessageException("El cambio de producto requiere itemId válido");
        }
        requireString(details, "productName");
        if (!(details.get("quantity") instanceof Number quantity) || quantity.intValue() <= 0) {
            throw new InvalidHistoryMessageException("El cambio de producto requiere quantity positiva");
        }
    }

    private void requireString(Map<String, Object> details, String field) {
        if (details == null || !(details.get(field) instanceof String value) || value.isBlank()) {
            throw new InvalidHistoryMessageException("Falta el campo " + field);
        }
    }
}
