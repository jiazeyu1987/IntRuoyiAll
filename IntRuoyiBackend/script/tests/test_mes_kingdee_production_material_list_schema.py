from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql/mysql/20260613_mes_kingdee_production_material_list.sql"
JOB_SQL = ROOT / "sql/mysql/20260613_mes_kingdee_production_material_list_sync_job.sql"
DO = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/"
    "pro/workorder/MesKingdeeProductionMaterialListDO.java"
)
MAPPER = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/"
    "pro/workorder/MesKingdeeProductionMaterialListMapper.java"
)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_production_material_list_sql_contract():
    sql = read(SQL)

    assert "CREATE TABLE IF NOT EXISTS `mes_kingdee_production_material_list`" in sql
    for column in [
        "`tenant_id`",
        "`source_form_id`",
        "`source_bill_no`",
        "`source_entry_id`",
        "`product_code`",
        "`production_order_no`",
        "`production_order_line_no`",
        "`production_order_status`",
        "`child_material_code`",
        "`child_material_name`",
        "`child_material_specification`",
        "`child_material_type`",
        "`numerator`",
        "`denominator`",
        "`child_unit_name`",
        "`required_quantity`",
        "`issue_method`",
        "`demand_time`",
        "`work_order_id`",
        "`work_order_code`",
        "`work_order_bom_id`",
        "`product_id`",
        "`child_material_id`",
        "`source_modify_time`",
        "`last_sync_time`",
        "`raw_payload`",
    ]:
        assert column in sql

    assert "uk_mes_kingdee_prod_material_list_source" in sql
    assert "`tenant_id`, `source_bill_no`, `production_order_no`, `production_order_line_no`, `child_material_code`" in sql
    assert "idx_mes_kingdee_prod_material_list_order" in sql
    assert "idx_mes_kingdee_prod_material_list_child" in sql
    assert "idx_mes_kingdee_prod_material_list_work_order" in sql


def test_production_material_list_do_contract():
    source = read(DO)

    assert '@TableName("mes_kingdee_production_material_list")' in source
    assert "class MesKingdeeProductionMaterialListDO extends BaseDO" in source
    for field in [
        "private String sourceFormId;",
        "private String sourceBillNo;",
        "private String sourceEntryId;",
        "private String productCode;",
        "private String productionOrderNo;",
        "private Integer productionOrderLineNo;",
        "private String productionOrderStatus;",
        "private String childMaterialCode;",
        "private String childMaterialName;",
        "private String childMaterialSpecification;",
        "private String childMaterialType;",
        "private BigDecimal numerator;",
        "private BigDecimal denominator;",
        "private String childUnitName;",
        "private BigDecimal requiredQuantity;",
        "private String issueMethod;",
        "private LocalDateTime demandTime;",
        "private Long workOrderId;",
        "private String workOrderCode;",
        "private Long workOrderBomId;",
        "private Long productId;",
        "private Long childMaterialId;",
        "private LocalDateTime sourceModifyTime;",
        "private LocalDateTime lastSyncTime;",
        "private String rawPayload;",
    ]:
        assert field in source


def test_production_material_list_mapper_contract():
    source = read(MAPPER)

    assert "interface MesKingdeeProductionMaterialListMapper extends BaseMapperX<MesKingdeeProductionMaterialListDO>" in source
    assert "selectBySourceLine" in source
    assert "selectListByProductionOrderNo" in source
    assert "selectListByWorkOrderId" in source


def test_production_material_list_sync_job_sql_contract():
    sql = read(JOB_SQL)

    assert "kingdeeProductionMaterialListSyncJob" in sql
    assert "每 10 分钟同步 ERP 生产用料清单" in sql
    assert "WHERE NOT EXISTS" in sql
    assert "`handler_name` = 'kingdeeProductionMaterialListSyncJob'" in sql
