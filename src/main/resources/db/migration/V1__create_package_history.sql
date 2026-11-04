CREATE TABLE package_history (
    event_id UUID PRIMARY KEY,
    package_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    actor_id VARCHAR(255),
    before_data JSONB,
    after_data JSONB NOT NULL,
    received_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_package_history_package_time
    ON package_history (package_id, occurred_at, event_id);
