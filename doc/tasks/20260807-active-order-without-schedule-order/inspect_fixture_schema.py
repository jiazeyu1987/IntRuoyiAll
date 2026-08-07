#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import json
from pathlib import Path


WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
BASE_FIXTURE_PATH = (
    WORKSPACE_ROOT
    / "doc"
    / "tasks"
    / "20260805-process-loss-reasons"
    / "acd04_simulate_environment.py"
)
TABLES = [
    "system_tenant",
    "system_users",
    "mes_md_item",
    "mes_pro_process",
    "mes_pro_workstation",
    "mes_pro_route",
    "mes_pro_route_process",
    "mes_pro_route_version",
    "mes_pro_route_product",
    "mes_pro_work_order",
    "mes_qa_inspection_regulation",
    "mes_qa_inspection_regulation_version",
    "mes_qa_inspection_regulation_item",
    "mes_pro_schedule_order",
    "mes_pro_process_pool_active_order",
    "mes_pro_process_pool_active_order_process_snapshot",
    "mes_pqc_inspection_task",
    "mes_pro_process_pool_team_maintenance_audit",
]


def load_base_fixture():
    spec = importlib.util.spec_from_file_location("fixture_base", BASE_FIXTURE_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load fixture helper: {BASE_FIXTURE_PATH}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main() -> int:
    helper = load_base_fixture()
    connection = helper.connect(helper.read_local_mysql_config(WORKSPACE_ROOT))
    output: dict[str, object] = {"tables": {}}
    try:
        with connection.cursor() as cursor:
            for table in TABLES:
                cursor.execute(
                    """
                    SELECT column_name, is_nullable, column_default, column_type
                      FROM information_schema.columns
                     WHERE table_schema = DATABASE()
                       AND table_name = %s
                     ORDER BY ordinal_position
                    """,
                    (table,),
                )
                output["tables"][table] = cursor.fetchall()
            cursor.execute(
                """
                SELECT u.id, u.username, t.id, t.name
                  FROM system_users u
                  JOIN system_tenant t ON t.id = u.tenant_id
                 WHERE u.tenant_id = 122
                   AND u.username = 'acd04lead1'
                   AND u.deleted = b'0'
                   AND t.deleted = b'0'
                """
            )
            output["leader"] = cursor.fetchall()
        print(json.dumps(output, ensure_ascii=False, default=str, indent=2))
        return 0
    finally:
        connection.close()


if __name__ == "__main__":
    raise SystemExit(main())
