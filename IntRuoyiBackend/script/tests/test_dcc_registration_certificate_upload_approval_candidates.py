from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICE_PATH = ROOT / "yudao-module-dcc" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "dcc" / "registrationcertificate" / "service" / "approval" / "DccRegistrationCertificateApprovalService.java"


def read_service() -> str:
    assert SERVICE_PATH.exists(), f"Missing service source: {SERVICE_PATH}"
    return SERVICE_PATH.read_text(encoding="utf-8")


def test_upload_approval_candidates_use_registration_manager_role_not_company_scope() -> None:
    source = read_service()

    assert "PermissionApi" in source
    assert "resolveUploadApprovalCandidates" in source
    assert "permissionApi.hasAnyPermissionsInRoles(List.of(roleId), UPLOAD_APPROVAL_PERMISSION)" in source
    assert "permissionApi.getUserRoleIdListByRoleIds(List.of(roleId))" in source
    assert "resolveScopedApprovalCandidates(request, approverRole.getId(), actorId)" in source
    assert "resolveUploadApprovalCandidates(approverRole.getId(), actorId)" in source
