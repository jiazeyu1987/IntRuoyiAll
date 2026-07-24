from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260626_showroom_product_current_bu_normalization.sql"

BU_ROWS = [
    ("非血管BU", "Non-vascular BU"),
    ("外周血管BU", "Peripheral Vascular BU"),
    ("结构心BU", "Structural Heart BU"),
    ("心血管BU", "Cardiovascular BU"),
    ("神经血管BU", "Neurovascular BU"),
    ("心脏电生理BU", "Cardiac Electrophysiology BU"),
]


def test_product_current_bu_normalization_sql_exists_with_release_header() -> None:
    assert SQL_PATH.exists()
    text = SQL_PATH.read_text(encoding="utf-8")

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260626_showroom_keyword_bu_seed_runtime; type=data; riskLevel=medium"
    )
    assert "showroom_product_current_bu_backup_20260626" in text


def test_product_current_bu_normalization_only_targets_current_revisions() -> None:
    text = SQL_PATH.read_text(encoding="utf-8")

    assert "JOIN `showroom_product` AS `product`" in text
    assert "`product`.`current_revision_id` = `revision`.`id`" in text
    assert "`product`.`deleted` = b'0'" in text
    assert "`revision`.`deleted` = b'0'" in text
    assert "UPDATE `showroom_product_revision` AS `revision`" in text
    assert "JOIN `showroom_product_current_bu_backup_20260626` AS `backup`" in text


def test_product_current_bu_normalization_preserves_empty_bu() -> None:
    text = SQL_PATH.read_text(encoding="utf-8")

    non_empty_guard = (
        "(NULLIF(TRIM(COALESCE(`revision`.`pipeline_layout`, '')), '') IS NOT NULL "
        "OR NULLIF(TRIM(COALESCE(`revision`.`pipeline_layout_en`, '')), '') IS NOT NULL)"
    )
    assert non_empty_guard in text
    assert "空 BU 保持为空" in text


def test_product_current_bu_normalization_contains_all_authorized_bu_mappings() -> None:
    text = SQL_PATH.read_text(encoding="utf-8")

    for name_zh, name_en in BU_ROWS:
        assert name_zh in text
        assert name_en in text

    assert "Non vascular" in text
    assert "Peripheral Vessel" in text
    assert "结构BU" in text
    assert "应为心血管BU" in text
    assert "应为外周血管BU" in text


def test_product_current_bu_normalization_blocks_unknown_non_empty_values() -> None:
    text = SQL_PATH.read_text(encoding="utf-8")

    assert "Unrecognized non-empty showroom product BU value" in text
    assert "WHERE `classified`.`target_zh` IS NULL" in text
    assert "CREATE TEMPORARY TABLE `tmp_showroom_product_current_bu_classified`" in text
    assert "CREATE TEMPORARY TABLE `tmp_showroom_product_current_bu_unknown_guard`" in text
    assert "`must_be_empty` int NOT NULL" in text
    assert "SELECT NULL" in text


def test_product_current_bu_normalization_excludes_tenant_zero_probe_rows_from_guard() -> None:
    text = SQL_PATH.read_text(encoding="utf-8")

    assert "`revision`.`tenant_id` = 0" in text
    assert "TRIM(COALESCE(`revision`.`pipeline_layout_en`, '')) = 'Null value probe'" in text
    assert "NULLIF(TRIM(COALESCE(`revision`.`pipeline_layout`, '')), '') IS NULL" in text
    assert "Null value probe" in text


def test_product_current_bu_normalization_still_guards_unknown_business_rows() -> None:
    text = SQL_PATH.read_text(encoding="utf-8")

    assert "WHERE `classified`.`target_zh` IS NULL" in text
    assert "INSERT INTO `tmp_showroom_product_current_bu_unknown_guard` (`must_be_empty`)" in text
