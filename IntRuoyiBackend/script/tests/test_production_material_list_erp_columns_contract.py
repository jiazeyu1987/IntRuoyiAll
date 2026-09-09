from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
REPO = ROOT.parent
SQL = ROOT / "sql/mysql/20260909_mes_kingdee_production_material_list_erp_columns.sql"
BASE_SQL = ROOT / "sql/mysql/20260613_mes_kingdee_production_material_list.sql"
ERP_CLIENT = ROOT / "yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/purchase/sync/ErpKingdeeProductionMaterialListClientImpl.java"
ERP_MODEL = ROOT / "yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/purchase/sync/ErpKingdeeProductionMaterialList.java"
MES_DO = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/workorder/MesKingdeeProductionMaterialListDO.java"
MES_SYNC = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/workorder/sync/MesKingdeeProductionMaterialListSyncServiceImpl.java"
MES_RESP = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/workorder/vo/kingdee/MesKingdeeProductionMaterialListRespVO.java"
MES_DETAIL_RESP = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/workorder/vo/kingdee/MesKingdeeProductionMaterialListDetailRespVO.java"
FRONT_API = REPO / "IntRuoyiFronted/src/api/erp/production/material-list/index.ts"
FRONT_ERP_PAGE = REPO / "IntRuoyiFronted/src/views/erp/production/material-list/index.vue"
FRONT_DETAIL_PANEL = REPO / "IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_migration_adds_production_material_list_drawing_number_idempotently() -> None:
    sql = read(SQL)
    assert "release-migration:" in sql
    assert "mes_kingdee_production_material_list" in sql
    assert "COLUMN_NAME = 'drawing_number'" in sql
    assert "ADD COLUMN `drawing_number` varchar(128)" in sql
    assert "information_schema.COLUMNS" in sql
    assert "ADD COLUMN IF NOT EXISTS" not in sql


def test_base_schema_documents_all_four_erp_columns() -> None:
    sql = read(BASE_SQL)
    assert "`drawing_number` varchar(128) DEFAULT NULL COMMENT '图号'" in sql
    assert "`required_quantity` decimal(24,6) NOT NULL COMMENT '应发数量'" in sql
    assert "`demand_time` datetime DEFAULT NULL COMMENT '需求日期'" in sql
    assert "`issue_method` varchar(64) DEFAULT NULL COMMENT '发料方式'" in sql


def test_kingdee_prd_ppbom_client_fetches_all_four_columns() -> None:
    client = read(ERP_CLIENT)
    model = read(ERP_MODEL)
    for field_key in [
        "FMaterialID2.F_PAEZ_TUHAO",
        "FMustQty",
        "FNeedDate",
        "FIssueType",
    ]:
        assert field_key in client
    assert "private String drawingNumber;" in model
    assert ".drawingNumber(optionalText(row, INDEX_DRAWING_NUMBER))" in client
    assert ".requiredQuantity(parseRequiredDecimal(row, INDEX_REQUIRED_QUANTITY, \"FMustQty\"))" in client
    assert ".demandTime(parseDateTime(optionalText(row, INDEX_DEMAND_TIME), \"FNeedDate\"))" in client
    assert ".issueMethod(optionalText(row, INDEX_ISSUE_METHOD))" in client


def test_mes_sync_persists_and_returns_all_four_columns() -> None:
    for path in [MES_DO, MES_RESP, MES_DETAIL_RESP]:
        source = read(path)
        assert "private String drawingNumber;" in source
        assert "private BigDecimal requiredQuantity;" in source
        assert "private LocalDateTime demandTime;" in source
        assert "private String issueMethod;" in source
    sync = read(MES_SYNC)
    assert ".drawingNumber(row.getDrawingNumber())" in sync
    assert ".requiredQuantity(row.getRequiredQuantity())" in sync
    assert ".demandTime(row.getDemandTime())" in sync
    assert ".issueMethod(row.getIssueMethod())" in sync


def test_frontend_types_and_pages_display_all_four_columns() -> None:
    api = read(FRONT_API)
    erp_page = read(FRONT_ERP_PAGE)
    detail_panel = read(FRONT_DETAIL_PANEL)
    assert "drawingNumber: string" in api
    assert "requiredQuantity: number" in api
    assert "demandTime: string" in api
    assert "issueMethod: string" in api
    for label in ["图号", "应发数量", "需求日期", "发料方式"]:
        assert label in erp_page
        assert label in detail_panel
    assert "prop=\"drawingNumber\"" in erp_page
