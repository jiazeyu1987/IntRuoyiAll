import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICE = (
    ROOT
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
    / "route"
    / "MesProRouteFlowConfigServiceImpl.java"
)
JAVA_TEST = (
    ROOT
    / "yudao-module-mes"
    / "src"
    / "test"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "service"
    / "pro"
    / "route"
    / "MesProRouteFlowConfigServiceImplTest.java"
)


def read(path: Path) -> str:
    assert path.exists(), f"missing expected file: {path}"
    return path.read_text(encoding="utf-8")


def test_flow_config_read_uses_readable_candidate_gate():
    service = read(SERVICE)
    assert "READABLE_CANDIDATE_STATUSES" in service
    assert "STATUS_PENDING_APPROVAL" in service
    assert "STATUS_READY_TO_PUBLISH" in service
    assert re.search(
        r"routeVersionId\s*!=\s*null\)\s*\{\s*return getCandidateRouteFlowProcessConfigList\(\s*"
        r"requireReadableCandidateVersion\(routeVersionId,\s*routeId\),\s*flowConfigType\)",
        service,
    ), "flow-config read endpoint must use a readable candidate gate, not the DRAFT-only write gate"


def test_flow_config_save_remains_draft_only():
    service = read(SERVICE)
    assert re.search(
        r"MesProRouteVersionDO routeVersion\s*=\s*requireDraftCandidateVersion\("
        r"saveReqVO\.getRouteVersionId\(\),\s*route\.getId\(\)\)",
        service,
    ), "flow-config save must remain DRAFT-only and must not reuse the read gate"
    assert re.search(
        r"requireDraftCandidateVersion[\s\S]*STATUS_DRAFT[\s\S]*PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE",
        service,
    ), "non-DRAFT route versions must still be rejected for save"


def test_java_behavior_regression_covers_pending_approval_read():
    java_test = read(JAVA_TEST)
    assert "getRouteFlowProcessConfigList_shouldReadPendingApprovalCandidateUseConfigSnapshot" in java_test
    assert "STATUS_PENDING_APPROVAL" in java_test
    assert "verify(routeCandidateConfigService, never()).saveConfigSnapshot" in java_test

