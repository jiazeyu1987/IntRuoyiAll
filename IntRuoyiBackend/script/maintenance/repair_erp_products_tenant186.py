"""Repair confirmed stock-import/master duplicates in local tenant 186 only.

Default: validate the complete repair and roll back. --apply commits after verification.
Snapshots contain business rows only, never configuration or credentials.
Inventory rows are ERP snapshots, not movements: keep the newest snapshot per
canonical material/warehouse; never add two snapshots of the same inventory.
"""
import argparse
import json
from collections import defaultdict
from datetime import datetime
from pathlib import Path

from repair_erp_roles_tenant186 import connect, require

TENANT = 186
MARKER = "erp-repair-20261117"


def encoded(value):
    return value.hex() if isinstance(value, bytes) else str(value)


def run(root, output, apply):
    db = connect(root)
    try:
        with db.cursor() as c:
            def query(sql, params=None):
                c.execute(sql, params)
                return c.fetchall()

            require(not query("SELECT JOB_NAME FROM QRTZ_FIRED_TRIGGERS WHERE JOB_NAME LIKE 'kingdee%'"),
                    "Kingdee jobs are executing; do not repair during synchronization")
            db.begin()
            pairs = query("""SELECT d.id old_id,p.id keep_id,d.bar_code
                FROM erp_product d
                JOIN erp_product_category dc ON dc.id=d.category_id AND dc.code='KINGDEE_STOCK_PRODUCT'
                JOIN erp_product p ON p.tenant_id=d.tenant_id AND p.bar_code=d.bar_code
                    AND p.id<>d.id AND p.deleted=0
                JOIN erp_product_category pc ON pc.id=p.category_id AND pc.code<>'KINGDEE_STOCK_PRODUCT'
                JOIN erp_product_unit du ON du.id=d.unit_id
                JOIN erp_product_unit pu ON pu.id=p.unit_id
                WHERE d.tenant_id=186 AND d.deleted=0 AND TRIM(du.name)=TRIM(pu.name)
                FOR UPDATE""")
            duplicates = query("""SELECT bar_code,COUNT(*) n FROM erp_product
                WHERE tenant_id=186 AND deleted=0 GROUP BY bar_code HAVING COUNT(*)>1""")
            require(all(r["n"] == 2 for r in duplicates), "Unexpected duplicate multiplicity")
            require(len(pairs) == len(duplicates), "A duplicate has no unambiguous formal master or matching unit")
            require(len({r["old_id"] for r in pairs}) == len(pairs), "Ambiguous duplicate mapping")
            if not pairs:
                print("VERIFIED: no stock-import/master duplicates remain")
                db.rollback()
                return
            c.execute("CREATE TEMPORARY TABLE erp_repair_map (old_id BIGINT PRIMARY KEY,keep_id BIGINT NOT NULL)")
            c.executemany("INSERT INTO erp_repair_map VALUES (%s,%s)",
                          [(r["old_id"], r["keep_id"]) for r in pairs])
            c.execute("CREATE TEMPORARY TABLE erp_repair_ids (id BIGINT PRIMARY KEY)")
            c.execute("INSERT INTO erp_repair_ids SELECT old_id FROM erp_repair_map")
            c.execute("INSERT INTO erp_repair_ids SELECT DISTINCT keep_id FROM erp_repair_map")
            refs = query("""SELECT TABLE_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'erp\\_%'
                    AND COLUMN_NAME='product_id'""")
            for r in refs:
                table = r["TABLE_NAME"]
                if table == "erp_stock":
                    continue
                require(not query(f"SELECT r.id FROM `{table}` r JOIN erp_repair_map m "
                                  "ON r.product_id=m.old_id WHERE r.tenant_id=186 LIMIT 1"),
                        "Duplicate material has non-stock references: " + table)
            stocks = query("""SELECT s.* FROM erp_stock s JOIN erp_repair_ids p
                ON p.id=s.product_id WHERE s.tenant_id=186 AND s.deleted=0 FOR UPDATE""")
            product_rows = query("""SELECT p.* FROM erp_product p JOIN erp_repair_ids m
                ON m.id=p.id WHERE p.tenant_id=186""")
            mapping = {r["old_id"]: r["keep_id"] for r in pairs}
            buckets = defaultdict(list)
            for row in stocks:
                buckets[(mapping.get(row["product_id"], row["product_id"]), row["warehouse_id"])].append(row)
            keep, archive = [], []
            for key, rows in buckets.items():
                rows.sort(key=lambda r: (r["update_time"], r["create_time"], r["id"]), reverse=True)
                keep.append((key[0], rows[0]))
                archive.extend(rows[1:])
            output.mkdir(parents=True, exist_ok=True)
            snapshot = output / ("products-before-" + datetime.now().strftime("%Y%m%d-%H%M%S-%f") + ".json")
            with snapshot.open("x", encoding="utf-8") as f:
                json.dump({"tenant_id": TENANT, "mode": "apply" if apply else "rollback-validation",
                           "mapping": pairs, "products": product_rows, "stocks": stocks,
                           "archived_stock_ids": [r["id"] for r in archive]}, f,
                          ensure_ascii=True, default=encoded)
            c.executemany("UPDATE erp_stock SET deleted=1,updater=%s WHERE tenant_id=186 AND deleted=0 AND id=%s",
                          [(MARKER, r["id"]) for r in archive])
            remapped = [(pid, MARKER, row["id"]) for pid, row in keep if pid != row["product_id"]]
            c.executemany("UPDATE erp_stock SET product_id=%s,updater=%s WHERE tenant_id=186 AND deleted=0 AND id=%s", remapped)
            c.execute("UPDATE erp_product p JOIN erp_repair_map m ON m.old_id=p.id "
                      "SET p.deleted=1,p.updater=%s WHERE p.tenant_id=186 AND p.deleted=0", (MARKER,))
            require(c.rowcount == len(pairs), "Unexpected archived product count")
            require(not query("""SELECT bar_code FROM erp_product WHERE tenant_id=186 AND deleted=0
                GROUP BY bar_code HAVING COUNT(*)>1 LIMIT 1"""), "Duplicate active product remains")
            after = query("""SELECT s.* FROM erp_stock s JOIN
                (SELECT DISTINCT keep_id FROM erp_repair_map) m ON m.keep_id=s.product_id
                WHERE s.tenant_id=186 AND s.deleted=0""")
            require(len(after) == len(keep), "Incorrect inventory bucket count")
            actual = {(r["product_id"], r["warehouse_id"]): r["count"] for r in after}
            expected = {(pid, r["warehouse_id"]): r["count"] for pid, r in keep}
            require(actual == expected, "Inventory snapshots or quantities changed unexpectedly")
            summary = {"duplicate_products_archived": len(pairs), "stock_references_remapped": len(remapped),
                       "obsolete_stock_snapshots_archived": len(archive), "inventory_buckets_verified": len(keep),
                       "snapshot": str(snapshot), "committed": apply}
            if apply:
                db.commit()
            else:
                db.rollback()
            print(json.dumps(summary, ensure_ascii=True))
    finally:
        db.rollback()
        db.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    run(args.root.resolve(), args.output.resolve(), args.apply)
