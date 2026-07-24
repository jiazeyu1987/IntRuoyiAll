from __future__ import annotations

from script.e2e.dcc_screenshot_e2e_helpers import assert_services_ready, ensure_e2e_baseline, mysql_scalar


def test_dcc_screenshot_e2e_prerequisites_are_ready() -> None:
    assert_services_ready()
    ensure_e2e_baseline()

    assert mysql_scalar("SELECT COUNT(*) AS c FROM dcc_file_category WHERE tenant_id=122 AND name='体系文件' AND deleted=0;") == "1"
    assert mysql_scalar("SELECT COUNT(*) AS c FROM dcc_file_category WHERE tenant_id=122 AND name='技术文件-DHF' AND deleted=0;") == "1"
    assert mysql_scalar("SELECT COUNT(*) AS c FROM dcc_file_category WHERE tenant_id=122 AND name='技术文件-DMR' AND deleted=0;") == "1"
    assert mysql_scalar("SELECT COUNT(*) AS c FROM dcc_category_approval_route_node WHERE tenant_id=122 AND route_id IN (906301,906302,906303);") == "12"
