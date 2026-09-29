CREATE TABLE mes_pro_process_pool_active_order (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    work_order_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    route_version_id BIGINT NOT NULL,
    business_status VARCHAR(32) NOT NULL,
    rework_source_active_order_id BIGINT NULL,
    rework_review_id BIGINT NULL,
    deleted BIT NOT NULL DEFAULT 0,
    current_route_version_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN business_status IN ('REWORKED','VERSION_UPGRADED') THEN NULL ELSE route_version_id END),
    CONSTRAINT uk_mes_pp_active_order UNIQUE
        (tenant_id, work_order_id, route_id, current_route_version_id, deleted),
    CONSTRAINT uk_mes_pp_rework_review UNIQUE (tenant_id, rework_review_id, deleted)
)
