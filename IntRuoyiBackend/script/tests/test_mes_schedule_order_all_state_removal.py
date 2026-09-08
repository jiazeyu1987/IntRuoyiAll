from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260907_mes_schedule_order_all_state_removal.sql"
SCHEDULE_ORDER_DO = ROOT / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes" / "dal" / "dataobject" / "pro" / "scheduleorder" / "MesProScheduleOrderDO.java"
SCHEDULE_ORDER_MAPPER = ROOT / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes" / "dal" / "mysql" / "pro" / "scheduleorder" / "MesProScheduleOrderMapper.java"
SCHEDULE_ORDER_SERVICE = ROOT / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes" / "service" / "pro" / "scheduleorder" / "MesProScheduleOrderServiceImpl.java"
TASK_EXT_MAPPER = ROOT / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes" / "dal" / "mysql" / "pro" / "schedule" / "MesProTaskScheduleExtMapper.java"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_migration_adds_business_removal_columns_without_using_framework_deleted_flag():
    sql = read(MIGRATION)
    assert "dependsOn=20260624_mes_schedule_order_freeze_audit" in sql
    for column in (
        "removed_from_schedule",
        "removed_from_schedule_time",
        "removed_from_schedule_by",
        "removed_from_schedule_reason",
        "removed_from_schedule_status",
        "reentry_blocked",
        "active_work_order_id",
    ):
        assert column in sql
    assert "UPDATE `mes_pro_schedule_order` SET `deleted`" not in sql
    assert "DROP COLUMN" not in sql
    assert "DROP INDEX `uk_mes_pro_schedule_order_work_order_tenant`" in sql
    assert "CREATE UNIQUE INDEX `uk_mes_pro_schedule_order_active_work_order`" in sql
    assert "IF(`removed_from_schedule` = b''0'', `work_order_id`, NULL)" in sql


def test_backend_removes_orders_in_all_states_and_preserves_execution_history():
    service = read(SCHEDULE_ORDER_SERVICE)
    assert "setRemovedFromSchedule(Boolean.TRUE)" in service
    assert "setReentryBlocked(hasProductionFacts)" in service
    assert "MesProTaskStatusEnum.PREPARE" in service
    assert "setStatus(MesProTaskStatusEnum.CANCELED.getStatus())" in service
    assert "scheduleOrderMapper.deleteById" not in service
    assert "排产工单已冻结，不能删除" not in service
    assert "排产工单存在已报工或已完成记录，不能删除" not in service


def test_active_schedule_queries_exclude_removed_orders_but_history_remains_addressable():
    data_object = read(SCHEDULE_ORDER_DO)
    mapper = read(SCHEDULE_ORDER_MAPPER)
    task_ext_mapper = read(TASK_EXT_MAPPER)
    assert "private Boolean removedFromSchedule;" in data_object
    assert "private Boolean reentryBlocked;" in data_object
    assert "selectByIdForUpdate" in mapper
    assert "MesProScheduleOrderDO::getRemovedFromSchedule" in mapper
    assert "selectListByScheduleOrderIds" in task_ext_mapper
    assert "getDeleteImpact" in read(SCHEDULE_ORDER_SERVICE)
