from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_showroom_version_center_schema_declares_snapshot_columns_and_bundle_table() -> None:
    schema_path = REPO_ROOT / "sql" / "showroom" / "20260523_showroom_version_center_schema.sql"
    text = schema_path.read_text(encoding="utf-8")

    required_snippets = [
        "ALTER TABLE `showroom_company_revision`",
        "ADD COLUMN IF NOT EXISTS `display_name_snapshot` varchar(255) DEFAULT NULL",
        "ADD COLUMN IF NOT EXISTS `display_name_en_snapshot` varchar(255) DEFAULT NULL",
        "ADD COLUMN IF NOT EXISTS `company_type_snapshot` varchar(32) DEFAULT NULL",
        "CREATE TABLE IF NOT EXISTS `showroom_version_bundle`",
        "`release_preview_asset_version_id` bigint DEFAULT NULL",
        "`narration_zh_version_id` bigint NOT NULL",
        "`narration_en_version_id` bigint NOT NULL",
        "UNIQUE KEY `uk_showroom_version_bundle_revision` (`target_type`, `target_id`, `revision_id`)",
        "UNIQUE KEY `uk_showroom_version_bundle_no` (`target_type`, `target_id`, `revision_no`)",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_showroom_version_center_backfill_declares_fail_fast_skip_rules() -> None:
    backfill_path = REPO_ROOT / "sql" / "showroom" / "20260523_showroom_version_center_backfill.sql"
    text = backfill_path.read_text(encoding="utf-8")

    required_snippets = [
        "INSERT INTO `showroom_version_bundle`",
        "'COMPANY'",
        "'PRODUCT'",
        "`display_name_snapshot` IS NOT NULL",
        "`display_name_en_snapshot` IS NOT NULL",
        "`company_type_snapshot` IS NOT NULL",
        "zh.`candidate_count` = 1",
        "en.`candidate_count` = 1",
        "preview.`candidate_count` = 1",
        "version-center-backfill",
        "existing.`id` IS NULL",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert "UPDATE `showroom_company_revision`" not in text
    assert "UPDATE `showroom_product_revision`" not in text
    assert "DELETE FROM `showroom_version_bundle`" not in text
