from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
FRONTEND_ROOT = ROOT.parent


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_frontend_has_readonly_kingdee_stock_move_api_and_page() -> None:
    api = read(FRONTEND_ROOT / "src" / "api" / "erp" / "stock" / "kingdeeStockMove" / "index.ts")
    page = read(FRONTEND_ROOT / "src" / "views" / "erp" / "stock" / "kingdeeStockMove" / "index.vue")

    assert "/erp/kingdee-stock-move/page" in api
    assert "getStockMovePage" in api
    assert "金蝶调拨单" in page
    assert "调拨单号" in page
    assert "调出仓库" in page
    assert "调入仓库" in page
    assert "新增" not in page
    assert "审批" not in page
    assert "反审批" not in page
    assert "删除" not in page


def test_frontend_registers_stock_move_in_sync_monitor_and_profile_job_config() -> None:
    sync_page = read(FRONTEND_ROOT / "src" / "views" / "erp" / "sync" / "index.vue")
    profile = read(FRONTEND_ROOT / "src" / "views" / "Profile" / "components" / "ProfileErpTableAutoSyncSetting.vue")

    assert "STOCK_MOVE" in sync_page
    assert "kingdeeStockMoveSyncJob" in sync_page
    assert "金蝶调拨单" in sync_page
    assert "STOCK_MOVE" in profile
    assert "kingdeeStockMoveSyncJob" in profile
    assert "金蝶调拨单" in profile
    assert "kingdee-table-auto-sync" not in profile
