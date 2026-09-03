from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
REGISTRATION_ROOT = ROOT / "yudao-module-dcc" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "dcc" / "registrationcertificate"

USER_FACING_SERVICES = [
    REGISTRATION_ROOT / "service" / "upload" / "DccRegistrationCertificateUploadService.java",
    REGISTRATION_ROOT / "service" / "query" / "DccRegistrationCertificateQueryServiceImpl.java",
    REGISTRATION_ROOT / "service" / "certificate" / "DccRegistrationCertificatePrerequisiteValidator.java",
    REGISTRATION_ROOT / "service" / "accessrequest" / "DccRegistrationCertificateAccessRequestService.java",
    REGISTRATION_ROOT / "service" / "accesspolicy" / "DccRegistrationCertificateAccessPolicyService.java",
    REGISTRATION_ROOT / "service" / "supportingdocument" / "DccRegistrationCertificateSupportingDocumentService.java",
]
QUERY_MAPPER = REGISTRATION_ROOT / "dal" / "mysql" / "DccRegistrationCertificateQueryMapper.java"


def test_registration_certificate_user_paths_do_not_call_company_scope_api() -> None:
    for path in USER_FACING_SERVICES:
        text = path.read_text(encoding="utf-8")
        assert "companyScopeApi.getEnabledCompanyIdsForUser" not in text, path
        assert "companyScopeApi.validateUserCompanyAccess" not in text, path


def test_registration_certificate_query_mapper_filters_by_tenant_not_authorized_companies() -> None:
    text = QUERY_MAPPER.read_text(encoding="utf-8")

    assert "c.owner_company_id IN" not in text
    assert "collection=\"companyIds\"" not in text
    assert "WHERE c.tenant_id = #{tenantId}" in text
