from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260612_mes_edhr_final_archive_work_task.sql"
SERVICE_PATH = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "service"
    / "pro"
    / "batchrecord"
    / "MesProEdhrWorkTaskService.java"
)
SERVICE_IMPL_PATH = SERVICE_PATH.with_name("MesProEdhrWorkTaskServiceImpl.java")
WORK_TASK_DO_PATH = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "dal"
    / "dataobject"
    / "pro"
    / "batchrecord"
    / "MesProEdhrWorkTaskDO.java"
)
ASSIGNMENT_RULE_DO_PATH = WORK_TASK_DO_PATH.with_name("MesProEdhrWorkTaskAssignmentRuleDO.java")
STATS_VO_PATH = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "controller"
    / "admin"
    / "pro"
    / "batchrecord"
    / "vo"
    / "MesProEdhrWorkTaskStatsRespVO.java"
)


def read_text(path: Path) -> str:
    assert path.exists(), f"{path.relative_to(REPO_ROOT)} 必须存在。"
    return path.read_text(encoding="utf-8")


def test_final_archive_work_task_migration_declares_scope_and_archive_contract() -> None:
    text = read_text(SQL_PATH)
    upper_text = text.upper()

    for required in [
        "mes_pro_edhr_work_task_assignment_rule",
        "`scope_type` varchar(32) NOT NULL",
        "`scope_id` bigint DEFAULT NULL",
        "ROUTE_PROCESS",
        "ROUTE",
        "`task_type` = 'ARCHIVE'",
        "mes_pro_edhr_work_task",
        "`business_scope_type` varchar(32) NOT NULL",
        "`business_scope_id` bigint NOT NULL",
        "BATCH_TASK",
        "BATCH_ARCHIVE",
        "idx_mes_pro_edhr_work_task_active_scope",
        "MES_EDHR_ARCHIVE_TASK_ASSIGNED",
        "最终归档",
        "workTaskId",
        "SIGNAL SQLSTATE '45000'",
    ]:
        assert required in text

    assert "ON DUPLICATE KEY UPDATE" not in upper_text
    assert "INSERT IGNORE INTO `SYSTEM_NOTIFY_TEMPLATE`" not in upper_text
    assert "自动派给管理员" not in text


def test_final_archive_work_task_backend_model_contract() -> None:
    service_text = read_text(SERVICE_PATH)
    service_impl_text = read_text(SERVICE_IMPL_PATH)
    work_task_do_text = read_text(WORK_TASK_DO_PATH)
    assignment_rule_do_text = read_text(ASSIGNMENT_RULE_DO_PATH)
    stats_vo_text = read_text(STATS_VO_PATH)

    assert 'String TASK_TYPE_ARCHIVE = "ARCHIVE";' in service_text
    assert "private String businessScopeType;" in work_task_do_text
    assert "private Long businessScopeId;" in work_task_do_text
    assert "private String scopeType;" in assignment_rule_do_text
    assert "private Long scopeId;" in assignment_rule_do_text
    assert "private Long archiveCount;" in stats_vo_text
    assert ".setArchiveCount(workTaskMapper.countMy(userId, TASK_TYPE_ARCHIVE" in service_impl_text
    assert "TASK_TYPE_ARCHIVE.equals(taskType)" in service_impl_text
    assert "MES_EDHR_ARCHIVE_TASK_ASSIGNED" in service_impl_text
