# Historial de PackIt

Microservicio Spring Boot 4.1.2 y Java 21. Consume eventos de Paquetes desde RabbitMQ y conserva una línea de tiempo inmutable en su propia base PostgreSQL.

## Ejecutar en local

1. Inicia PostgreSQL y RabbitMQ. Crea la base `packit_history`, por ejemplo con `createdb -h localhost -U postgres packit_history`.
2. Desde este directorio, ejecuta `./mvnw spring-boot:run`. Flyway crea `package_history` al iniciar. La API escucha en `http://localhost:8082`.
3. Configura `HISTORY_DB_URL`, `HISTORY_DB_USERNAME`, `HISTORY_DB_PASSWORD`, `HISTORY_RABBITMQ_HOST`, `HISTORY_RABBITMQ_PORT`, `HISTORY_RABBITMQ_USERNAME` y `HISTORY_RABBITMQ_PASSWORD` si los valores locales predeterminados no sirven. Usa `COGNITO_ISSUER_URI` para el pool de Cognito y `HISTORY_CORS_ALLOWED_ORIGINS` para el frontend. `HISTORY_CONSUMER_ENABLED=false` detiene el consumo durante pruebas aisladas.

La aplicación declara `history.package` y `history.package.dlq` como colas classic durables, junto con los exchanges y bindings necesarios. Paquetes publica en `packit.package.events` con claves `package.*`. Historial confirma cada mensaje después de guardar el evento o reconocer un `eventId` repetido. Los mensajes inválidos y los que agotan tres intentos van a la DLQ.

## Consultar

`GET /api/v1/packages/{packageId}/history` devuelve los eventos por fecha y `eventId`. Requiere un access token de Cognito cuyo grupo sea `ADMIN` o `SUPERVISOR`.

```bash
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8082/api/v1/packages/42/history
```

## Pruebas

- `./mvnw test`: validación, persistencia, deduplicación, consumo y autorización. Requiere PostgreSQL local; la prueba normal no necesita RabbitMQ.
- `./mvnw -Dhistory.rabbit.integration=true -Dtest=HistoryRabbitIntegrationTests test`: prueba optativa de consumo y deduplicación contra RabbitMQ y PostgreSQL locales.

El contrato JSON de eventos está en `../pack-it-flawlessly-backend/docs/message-contracts.md`.
