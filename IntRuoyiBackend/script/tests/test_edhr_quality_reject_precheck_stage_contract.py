from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SERVICE_SOURCE = (
    REPO_ROOT
    / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
    / "MesProEdhrBatchExecutionServiceImpl.java"
).read_text(encoding="utf-8")


def test_quality_reject_requires_release_precheck_stage():
    assert "requireQualityRejectPrecheckStage(batch);" in SERVICE_SOURCE
    assert "private void requireQualityRejectPrecheckStage(MesProEdhrBatchExecutionDO batch)" in SERVICE_SOURCE
    assert "!Objects.equals(batch.getStatus(), BATCH_STATUS_CLOSED)" in SERVICE_SOURCE

    for disallowed_status in [
        "MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED",
        "MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL",
        "MesProEdhrReleaseServiceImpl.STATUS_RELEASED",
    ]:
        assert disallowed_status in SERVICE_SOURCE


def test_quality_reject_keeps_terminal_and_action_lock_guards():
    quality_reject_index = SERVICE_SOURCE.index("public EdhrBatchExecutionRespVO qualityReject")
    stage_guard_index = SERVICE_SOURCE.index("requireQualityRejectPrecheckStage(batch);")
    password_guard_index = SERVICE_SOURCE.index('质量终态拒收必须填写原因和签名密码')

    assert quality_reject_index < stage_guard_index < password_guard_index
    assert "Objects.equals(batch.getStatus(), BATCH_STATUS_ARCHIVED)" in SERVICE_SOURCE
    assert "Objects.equals(batch.getStatus(), BATCH_STATUS_REJECTED)" in SERVICE_SOURCE
    assert "Objects.equals(batch.getStatus(), BATCH_STATUS_VOIDED)" in SERVICE_SOURCE
    assert "requireBatchActionUnlocked(batch.getId());" in SERVICE_SOURCE
