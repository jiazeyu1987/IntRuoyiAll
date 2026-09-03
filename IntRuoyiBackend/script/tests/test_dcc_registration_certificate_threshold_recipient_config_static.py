from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CONFIG_SERVICE = ROOT / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/config/DccRegistrationCertificateConfigService.java"
CONFIG_COMMAND = ROOT / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/config/DccRegistrationCertificateReminderConfigUpdateCommand.java"
CONFIG_REQUEST = ROOT / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/config/vo/DccRegistrationCertificateReminderConfigUpdateReqVO.java"
JOB = ROOT / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/job/DccRegistrationCertificateReminderDailyJob.java"
MIGRATION = ROOT / "sql/mysql/20260903_dcc_registration_certificate_threshold_recipient_config.sql"


def read(path: Path) -> str:
    assert path.exists(), f"missing required file: {path}"
    return path.read_text(encoding="utf-8")


def test_threshold_recipient_configuration_is_persisted_and_delivered_per_threshold():
    migration = read(MIGRATION)
    service = read(CONFIG_SERVICE)
    command = read(CONFIG_COMMAND)
    request = read(CONFIG_REQUEST)
    job = read(JOB)

    assert "recipient_user_ids_json" in migration
    assert "thresholdRecipientUserIds" in command
    assert "thresholdRecipientUserIds" in request
    assert "thresholdRecipientUserIds" in service
    assert "getRecipientUserIds" in service
    assert "configService.getRecipientUserIds" in job
    assert "occurrence.thresholdLevel()" in job
    assert "recipientResolver.resolve(" not in job
    assert "DCC_REGISTRATION_CERTIFICATE_REMINDER_VIEW" in migration
    assert "dcc:registration-certificate:query-current" in migration
    assert "dcc:registration-certificate:config:query" in migration
    assert "dcc:registration-certificate:config:update" in migration
    assert "dcc_registration_certificate_approver" in migration
    assert "system_role_menu" in migration
    assert "syncEntitlementClaims" in service
