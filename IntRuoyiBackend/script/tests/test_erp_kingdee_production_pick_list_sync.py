from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260813_erp_kingdee_production_pick_list_sync.sql"
BACKEND_ROOT = ROOT / "yudao-module-erp" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "erp"
FRONTEND_ROOT = ROOT.parent / "IntRuoyiFronted"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_production_pick_list_migration_creates_readonly_snapshot_tables_and_job() -> None:
    sql = read(SQL_PATH)

    assert "-- release-migration:" in sql
    assert "dependsOn=20260612_erp_kingdee_sync_runtime" in sql
    assert "CREATE TABLE IF NOT EXISTS erp_kingdee_production_pick_list" in sql
    assert "CREATE TABLE IF NOT EXISTS erp_kingdee_production_pick_list_item" in sql
    assert "uk_erp_kingdee_prod_pick_list_source" in sql
    assert "uk_erp_kingdee_prod_pick_list_item_source" in sql
    assert "'kingdeeProductionPickListSyncJob'" in sql
    assert "SELECT '每 10 分钟同步 ERP 生产领料单列表', 2" in sql
    assert "production parent menu must exist exactly once" in sql
    assert "menu id 6032 is occupied" in sql
    assert "pick-list sibling path is occupied" in sql
    assert "menu id 6033 is occupied" in sql
    assert "erp:production-pick-list:query is occupied" in sql
    assert sql.index("CALL preflight_erp_production_pick_list_menu();") < sql.index(
        "CREATE TABLE IF NOT EXISTS erp_kingdee_production_pick_list"
    )


def test_production_pick_list_job_migration_uses_handler_name_business_key() -> None:
    sql = read(SQL_PATH)
    job_sql = sql.split("INSERT INTO infra_job", 1)[1].split("SET @erp_production_parent_menu_id", 1)[0]

    assert "id, name" not in job_sql
    assert "SELECT 56" not in job_sql
    assert "WHERE handler_name = 'kingdeeProductionPickListSyncJob'" in job_sql


def test_production_pick_list_registered_as_independent_sync_type() -> None:
    sync_type = read(BACKEND_ROOT / "enums" / "sync" / "ErpKingdeeSyncTypeEnum.java")
    auto_sync_type = read(BACKEND_ROOT / "enums" / "kingdeeautosync" / "ErpKingdeeTableAutoSyncTypeEnum.java")
    profile_component = read(FRONTEND_ROOT / "src" / "views" / "Profile" / "components" / "ProfileErpTableAutoSyncSetting.vue")
    sync_page = read(FRONTEND_ROOT / "src" / "views" / "erp" / "sync" / "index.vue")

    assert 'PRODUCTION_PICK_LIST("PRODUCTION_PICK_LIST")' in sync_type
    assert "ErpKingdeeSyncTypeEnum.PRODUCTION_PICK_LIST.getType()" in auto_sync_type
    assert '"生产领料单列表"' in auto_sync_type
    assert '"kingdeeProductionPickListSyncJob"' in auto_sync_type
    assert "syncType: 'PRODUCTION_PICK_LIST'" in profile_component
    assert "handlerName: 'kingdeeProductionPickListSyncJob'" in profile_component
    assert "localTabName: 'ERP生产领料单列表'" in profile_component
    assert "type: 'PRODUCTION_PICK_LIST'" in sync_page
    assert "handlerName: 'kingdeeProductionPickListSyncJob'" in sync_page


def test_production_pick_list_uses_prd_pickmtrl_and_not_material_list() -> None:
    client = read(BACKEND_ROOT / "service" / "production" / "sync" / "ErpKingdeeProductionPickListClientImpl.java")
    service = read(BACKEND_ROOT / "service" / "production" / "kingdee" / "ErpKingdeeProductionPickListServiceImpl.java")
    job = read(BACKEND_ROOT / "job" / "production" / "KingdeeProductionPickListSyncJob.java")

    assert 'FORM_ID = "PRD_PickMtrl"' in client
    assert "ExecuteBillQuery" in client
    assert "FEntity_FEntryID" in client
    assert "FActualQty" in client
    assert "FAppQty" in client
    assert "FMoBillNo" in client
    assert "FStockId.FNumber" in client
    assert "ErpKingdeeSyncTypeEnum.PRODUCTION_PICK_LIST" in job
    assert "syncModifiedBetween" in job
    assert "PRODUCTION_MATERIAL_LIST" not in client
    assert "PRD_PPBOM" not in client
    assert "MesKingdeeProductionMaterialList" not in service
    assert "ErpStockMoveService" not in service
    assert "ErpStockRecordService" not in service


def test_production_pick_list_snapshots_are_tenant_scoped() -> None:
    header = read(
        BACKEND_ROOT
        / "dal"
        / "dataobject"
        / "production"
        / "kingdee"
        / "ErpKingdeeProductionPickListDO.java"
    )
    item = read(
        BACKEND_ROOT
        / "dal"
        / "dataobject"
        / "production"
        / "kingdee"
        / "ErpKingdeeProductionPickListItemDO.java"
    )

    assert "extends TenantBaseDO" in header
    assert "extends TenantBaseDO" in item


def test_production_pick_list_frontend_page_is_readonly_and_manual_syncs_own_handler() -> None:
    api = read(FRONTEND_ROOT / "src" / "api" / "erp" / "production" / "pick-list" / "index.ts")
    page = read(FRONTEND_ROOT / "src" / "views" / "erp" / "production" / "pick-list" / "index.vue")

    assert "/erp/production-pick-list/page" in api
    assert "ErpProductionPickListApi.getPage" in page
    assert "ErpKingdeeSyncApi.runIncrementalSyncJob('kingdeeProductionPickListSyncJob')" in page
    assert "生产领料单号" in page
    assert "生产订单编号" in page
    assert "物料编码" in page
    assert "实发数量" in page
    assert "申请数量" in page
    assert "仓库" in page
    assert "新增" not in page
    assert "审核" not in page
    assert "submitProductionPickList" not in page
