from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_infra_file_content_baseline_uses_longblob() -> None:
    schema_path = REPO_ROOT / "sql" / "mysql" / "ruoyi-vue-pro.sql"
    text = schema_path.read_text(encoding="utf-8")

    assert "CREATE TABLE `infra_file_content`" in text
    assert "`content` longblob NOT NULL COMMENT '文件内容'" in text


def test_infra_file_content_longblob_migration_is_idempotent() -> None:
    migration_path = REPO_ROOT / "sql" / "mysql" / "20260517_infra_file_content_longblob.sql"
    text = migration_path.read_text(encoding="utf-8")

    assert "CREATE PROCEDURE ensure_infra_file_content_longblob" in text
    assert "CALL ensure_infra_file_content_longblob();" in text
    assert "ALTER TABLE `infra_file_content`" in text
    assert "MODIFY COLUMN `content` LONGBLOB NOT NULL COMMENT '文件内容'" in text
