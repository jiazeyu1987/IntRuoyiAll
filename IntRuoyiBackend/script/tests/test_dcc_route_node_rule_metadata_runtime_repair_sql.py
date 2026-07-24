from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
RUNTIME_REPAIR_SQL = REPO_ROOT / "sql/mysql/20260515_dcc_runtime_schema_repair.sql"

REQUIRED_ROUTE_NODE_METADATA_COLUMNS = [
    "stage_type",
    "subject_label",
    "marker",
    "subject_type",
    "subject_id",
    "subject_name",
    "subject_department_path",
    "rule_remark",
]


def test_runtime_repair_backfills_route_node_rule_metadata_columns() -> None:
    sql = RUNTIME_REPAIR_SQL.read_text(encoding="utf-8")

    assert "'dcc_category_approval_route_node'" in sql
    for column in REQUIRED_ROUTE_NODE_METADATA_COLUMNS:
        assert f"'{column}'" in sql
        assert f"ADD COLUMN `{column}`" in sql
