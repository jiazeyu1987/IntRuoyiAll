from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MAPPER = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "resources"
    / "mapper"
    / "pro"
    / "processpool"
    / "MesProcessPoolActiveOrderDetailReadMapper.xml"
)


def test_active_order_detail_mapper_uses_snake_case_aliases_for_required_fields():
    source = MAPPER.read_text(encoding="utf-8")

    required_aliases = [
        "AS snapshot_id",
        "AS active_order_id",
        "AS work_order_id",
        "AS work_order_code",
        "AS route_process_id",
        "AS process_id",
        "AS process_name",
        "AS required_quantity",
        "AS submitted_quantity",
        "AS submitted_at",
    ]
    for alias in required_aliases:
        assert alias in source, f"missing stable snake_case alias: {alias}"

    forbidden_aliases = [
        "AS snapshotId",
        "AS activeOrderId",
        "AS workOrderId",
        "AS workOrderCode",
        "AS routeProcessId",
        "AS processId",
        "AS processName",
        "AS requiredQuantity",
        "AS submittedQuantity",
        "AS submittedAt",
    ]
    for alias in forbidden_aliases:
        assert alias not in source, f"camelCase MyBatis alias can lose mapping at runtime: {alias}"
