from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260901_dcc_registration_certificate_business_event_notify_template.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), f"missing SQL migration: {SQL_PATH.relative_to(REPO_ROOT)}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_business_event_notify_template_uses_readable_registration_certificate_fields() -> None:
    sql = _read_sql()

    required = [
        "DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT",
        "产品《{productName}》的国内注册证{eventTitle}",
        "注册证号：{certificateNo}",
        "生效日期：{effectiveDate}",
        "有效期至：{expiryDate}",
        '["eventTitle","productName","certificateNo","effectiveDate","expiryDate"]',
    ]
    for snippet in required:
        assert snippet in sql


def test_business_event_notify_template_hides_internal_event_and_identity_fields() -> None:
    sql = _read_sql()

    forbidden = [
        "{eventType}",
        "{eventKey}",
        "{tenantId}",
        "{ownerCompanyId}",
        "{certificateId}",
        "{versionId}",
        "{actorId}",
        "业务键",
        "版本 {versionId}",
        "租户 {tenantId}",
        "企业 {ownerCompanyId}",
        "操作人 {actorId}",
    ]
    for snippet in forbidden:
        assert snippet not in sql
