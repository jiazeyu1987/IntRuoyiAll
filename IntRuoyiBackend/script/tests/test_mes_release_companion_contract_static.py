import os
from pathlib import Path


REPO_ROOT = Path(os.environ.get("BACKEND_REPO_ROOT", Path(__file__).resolve().parents[2]))


def read_source(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"missing source file: {relative_path}"
    return path.read_text(encoding="utf-8")


def test_edhr_batch_execution_response_exposes_release_lock_contract() -> None:
    source = read_source(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/"
        "pro/batchrecord/vo/EdhrBatchExecutionRespVO.java"
    )

    for field in [
        "private Boolean releaseActionLocked;",
        "private String releaseActionLockReason;",
        "private Long pendingVoidChangeEventId;",
        "private String pendingVoidChangeCode;",
        "private String pendingVoidChangeStatus;",
        "private String pendingVoidProcessInstanceId;",
        "private Long pendingVoidRequestedBy;",
        "private LocalDateTime pendingVoidRequestedAt;",
        "private Boolean canWithdrawVoidRequest;",
    ]:
        assert field in source


def test_edhr_batch_task_open_request_uses_work_task_identity() -> None:
    source = read_source(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/"
        "pro/batchrecord/vo/EdhrBatchExecutionTaskOpenReqVO.java"
    )

    assert "private Long workTaskId;" in source


def test_edhr_batch_task_response_exposes_batch_record_version_no() -> None:
    source = read_source(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/"
        "pro/batchrecord/vo/EdhrBatchExecutionTaskRespVO.java"
    )

    assert "private String batchRecordVersionNo;" in source


def test_void_record_change_service_exposes_withdraw_contract() -> None:
    source = read_source(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/"
        "batchrecord/MesProEdhrRecordChangeService.java"
    )

    assert "EdhrRecordChangeRespVO withdrawVoidBatchExecution(EdhrRecordChangeApproveReqVO reqVO);" in source


def test_route_version_mapper_exposes_approval_reset_contract() -> None:
    source = read_source(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/"
        "route/MesProRouteVersionMapper.java"
    )

    assert "import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;" in source
    assert "MesProRouteVersionDO selectActiveByRouteIdForUpdate(Long routeId);" in source
    assert "default int updateApprovalFieldsToDraft(Long id)" in source
    assert ".set(MesProRouteVersionDO::getLifecycleStatus, STATUS_DRAFT)" in source
    assert ".set(MesProRouteVersionDO::getApprovalProcessInstanceId, null)" in source
