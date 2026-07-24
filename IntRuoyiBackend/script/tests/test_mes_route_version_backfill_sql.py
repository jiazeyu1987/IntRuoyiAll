from pathlib import Path


SQL_PATH = Path("sql/mysql/20260614_mes_route_version_backfill.sql")


def test_route_version_backfill_is_idempotent_and_tenant_scoped() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "INSERT INTO `mes_pro_route_version`" in sql
    assert "FROM `mes_pro_route` r" in sql
    assert "LEFT JOIN `mes_pro_route_version` v" in sql
    assert "v.`tenant_id` = r.`tenant_id`" in sql
    assert "v.`route_id` = r.`id`" in sql
    assert "v.`deleted` = b'0'" in sql
    assert "WHERE r.`deleted` = b'0'" in sql
    assert "AND v.`id` IS NULL" in sql
    assert "'V1'" in sql
    assert "b'1'" in sql
    assert "r.`tenant_id`" in sql


def test_route_version_backfill_captures_route_snapshot() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    for field in ("routeId", "routeCode", "routeName", "description", "status", "remark"):
        assert field in sql
    assert "JSON_OBJECT" in sql
