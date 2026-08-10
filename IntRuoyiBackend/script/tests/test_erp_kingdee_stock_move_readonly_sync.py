from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260810_erp_kingdee_stock_move_readonly_sync.sql"
BACKEND_ROOT = ROOT / "yudao-module-erp" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "erp"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_stock_move_readonly_migration_creates_snapshot_tables_and_job() -> None:
    sql = read(SQL_PATH)

    assert "-- release-migration:" in sql
    assert "dependsOn=20260612_erp_kingdee_sync_runtime" in sql
    assert "CREATE TABLE IF NOT EXISTS" in sql
    assert "erp_kingdee_stock_move" in sql
    assert "erp_kingdee_stock_move_item" in sql
    assert "uk_erp_kingdee_stock_move_source" in sql
    assert "uk_erp_kingdee_stock_move_item_source" in sql
    assert "'kingdeeStockMoveSyncJob'" in sql
    assert "SELECT 5610, '每 10 分钟同步 ERP 金蝶调拨单', 2" in sql


def test_stock_move_readonly_migration_does_not_mutate_local_stock_move_business_table() -> None:
    sql = read(SQL_PATH)
    bt = chr(96)

    forbidden = [
        f"ALTER TABLE {bt}erp_stock_move{bt}",
        f"ALTER TABLE {bt}erp_stock_move_item{bt}",
        f"INSERT INTO {bt}erp_stock_move{bt}",
        f"INSERT INTO {bt}erp_stock_move_item{bt}",
        f"UPDATE {bt}erp_stock_move{bt}",
        f"UPDATE {bt}erp_stock_move_item{bt}",
    ]
    for fragment in forbidden:
        assert fragment not in sql


def test_stock_move_sync_type_and_auto_sync_handler_are_registered() -> None:
    sync_type = read(BACKEND_ROOT / "enums" / "sync" / "ErpKingdeeSyncTypeEnum.java")
    auto_sync_type = read(BACKEND_ROOT / "enums" / "kingdeeautosync" / "ErpKingdeeTableAutoSyncTypeEnum.java")

    assert 'STOCK_MOVE("STOCK_MOVE")' in sync_type
    assert "ErpKingdeeSyncTypeEnum.STOCK_MOVE.getType()" in auto_sync_type
    assert '"金蝶调拨单"' in auto_sync_type
    assert '"kingdeeStockMoveSyncJob"' in auto_sync_type


def test_stock_move_sync_uses_kingdee_direct_transfer_and_never_local_inventory_approval() -> None:
    client = read(BACKEND_ROOT / "service" / "stock" / "sync" / "ErpKingdeeStockMoveClientImpl.java")
    service = read(BACKEND_ROOT / "service" / "stock" / "kingdee" / "ErpKingdeeStockMoveListServiceImpl.java")
    job = read(BACKEND_ROOT / "job" / "stock" / "KingdeeStockMoveSyncJob.java")

    assert 'FORM_ID = "STK_TransferDirect"' in client
    assert "ExecuteBillQuery" in client
    assert "FBillEntry_FEntryID" in client
    assert '"FEntryID"' not in client
    assert "FModifyDate" in client
    assert "ErpKingdeeSyncTypeEnum.STOCK_MOVE" in job
    assert "syncModifiedBetween" in job
    assert "ErpStockMoveService" not in service
    assert "updateStockMoveStatus" not in service
    assert "ErpStockRecordService" not in service
    assert "stockRecordService" not in service
