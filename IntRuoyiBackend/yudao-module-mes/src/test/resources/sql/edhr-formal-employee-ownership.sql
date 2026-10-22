CREATE TABLE mes_pro_process_pool_team_employee_profile (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    leader_user_id BIGINT NOT NULL,
    system_user_id BIGINT,
    employee_type VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    deleted BOOLEAN NOT NULL,
    enabled_formal_user_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN deleted=false AND enabled=true AND system_user_id IS NOT NULL THEN system_user_id ELSE NULL END),
    CONSTRAINT uk_mes_pp_employee_enabled_user UNIQUE (tenant_id, enabled_formal_user_id)
);
