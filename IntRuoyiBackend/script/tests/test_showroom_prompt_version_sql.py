from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_showroom_prompt_version_mysql_script_declares_prompt_table_seed_and_backfill() -> None:
    script_path = REPO_ROOT / "sql" / "mysql" / "20260524_showroom_prompt_version.sql"
    text = script_path.read_text(encoding="utf-8")

    required_snippets = [
        "CREATE TABLE IF NOT EXISTS `showroom_image_prompt_version`",
        "CALL ensure_showroom_column(",
        "'showroom_product_cover_batch_task'",
        "'prompt_version_id'",
        "INSERT INTO `showroom_image_prompt_version`",
        "'PRODUCT_COVER'",
        "{{product_name_cn}}",
        "{{product_name_en}}",
        "`prompt_version_id` IS NULL",
        "`status` IN ('WAITING', 'RUNNING')",
    ]

    for snippet in required_snippets:
        assert snippet in text
