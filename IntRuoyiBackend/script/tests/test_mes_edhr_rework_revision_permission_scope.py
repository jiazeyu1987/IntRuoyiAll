from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICE = ROOT / (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
    "service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java"
)


def test_rework_revision_inherits_execution_permission_context():
    source = SERVICE.read_text(encoding="utf-8")
    start = source.index("private MesProBatchRecordExecutionDO createReworkRevision")
    end = source.index("private String resolveRevisionParentHash", start)
    method = source[start:end]

    required_lines = [
        ".recordCategory(rejected.getRecordCategory())",
        ".validationProfile(rejected.getValidationProfile())",
        ".permissionScopeId(rejected.getPermissionScopeId())",
        ".routeBindingId(rejected.getRouteBindingId())",
        ".routeBindingSnapshotHash(rejected.getRouteBindingSnapshotHash())",
    ]
    missing = [line for line in required_lines if line not in method]

    assert missing == [], (
        "返工修订草稿必须继承原执行的记录类型、校验策略、权限范围与路线绑定上下文，"
        f"否则字段审计会因 BATCH_RECORD_EXECUTION FILL 权限丢失而失败: {missing}"
    )
