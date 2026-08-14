import json
import sys
from pathlib import Path

repo_root = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(repo_root / "IntRuoyiBackend" / "script" / "e2e"))

from dcc_screenshot_e2e_helpers import mysql_rows  # noqa: E402


SQL = """
SELECT r.tenant_id,
       r.id AS regulation_id,
       r.product_id,
       r.route_id,
       r.route_version_id,
       r.route_process_id,
       r.process_id,
       p.name,
       r.owner_module,
       r.lifecycle_status,
       r.current_version_id,
       r.regulation_code,
       r.regulation_name,
       COUNT(i.id) AS item_count
  FROM mes_qa_inspection_regulation r
  LEFT JOIN mes_qa_inspection_regulation_item i
    ON i.regulation_version_id = r.current_version_id
   AND i.deleted = 0
   AND i.tenant_id = r.tenant_id
  LEFT JOIN mes_pro_process p
    ON p.id = r.process_id
   AND p.deleted = 0
   AND p.tenant_id = r.tenant_id
 WHERE r.tenant_id = 1
   AND r.deleted = 0
   AND r.route_id = 922119
 GROUP BY r.tenant_id,
          r.id,
          r.product_id,
          r.route_id,
          r.route_version_id,
          r.route_process_id,
          r.process_id,
          p.name,
          r.owner_module,
          r.lifecycle_status,
          r.current_version_id,
          r.regulation_code,
          r.regulation_name
 ORDER BY r.product_id, r.route_process_id, r.id;
"""


def main() -> None:
    print(json.dumps(mysql_rows(SQL), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
