from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260710_mes_pro_replan_explanation_snapshot.sql"
INITIAL_SCHEMA = ROOT / "sql" / "mysql" / "ruoyi-vue-pro.sql"
EXPECTED_RELEASE_METADATA = (
    "-- release-migration: allowedEnvironments=test,backup,prod; "
    "dependsOn=; type=schema; riskLevel=medium"
)


def test_replan_explanation_snapshot_release_metadata_contract():
    migration = MIGRATION.read_text(encoding="utf-8")

    assert migration.splitlines()[0] == EXPECTED_RELEASE_METADATA


def test_replan_explanation_snapshot_schema_contract():
    required_fragments = (
        "mes_pro_replan_explanation_snapshot",
        "`request_id` varchar(64) NOT NULL",
        "`trigger_source` varchar(32) NOT NULL",
        "`capacity_mode` varchar(32) NOT NULL",
        "`request_start_time` datetime NOT NULL",
        "`applied_at` datetime NOT NULL",
        "`snapshot_json` longtext NOT NULL",
        "`tenant_id` bigint NOT NULL",
        "uk_mes_pro_replan_explanation_request",
        "idx_mes_pro_replan_explanation_latest",
    )

    for path in (MIGRATION, INITIAL_SCHEMA):
        text = path.read_text(encoding="utf-8")
        for fragment in required_fragments:
            assert fragment in text, f"{path.name} missing schema contract: {fragment}"


def test_replan_explanation_snapshot_is_unique_per_tenant_and_request():
    migration = MIGRATION.read_text(encoding="utf-8")

    assert (
        "UNIQUE KEY `uk_mes_pro_replan_explanation_request` "
        "(`tenant_id`, `request_id`, `deleted`)"
    ) in migration
    assert (
        "KEY `idx_mes_pro_replan_explanation_latest` "
        "(`tenant_id`, `applied_at` DESC, `id` DESC)"
    ) in migration
