from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260917_mes_active_order_dossier_file.sql"


def test_active_order_dossier_migration_is_explicit_and_not_batch_owned():
    sql = MIGRATION.read_text(encoding="utf-8")
    lowered = sql.lower()

    assert lowered.startswith("-- release-migration:")
    assert "allowedenvironments=test,backup,prod" in lowered
    assert "dependson=20260808_mes_active_order_release_application" in lowered
    assert "type=schema" in lowered
    assert "risklevel=medium" in lowered
    assert "create table if not exists `mes_pro_process_pool_active_order_dossier_file`" in lowered
    assert "`active_order_id` bigint not null" in lowered
    assert "`category_key` varchar(64) not null" in lowered
    assert "`file_id` bigint not null" in lowered
    assert "`file_url` varchar(1024) not null" in lowered
    assert "`storage_path` varchar(512) not null" in lowered
    assert "`file_name` varchar(255) not null" in lowered
    assert "`content_type` varchar(128) not null" in lowered
    assert "`file_size` bigint not null" in lowered
    assert "`sha256` char(64) not null" in lowered
    assert "`operator_id` bigint not null" in lowered
    assert "`operator_name` varchar(64) not null" in lowered
    assert "`operated_at` datetime not null" in lowered
    assert "batch_execution_id" not in lowered
    assert "batch_task_id" not in lowered
    assert "execution_id" not in lowered
    assert "idx_mes_pp_active_order_dossier_file_owner" in lowered
    assert "idx_mes_pp_active_order_dossier_file_category" in lowered
