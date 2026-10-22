from pathlib import Path

import yaml


WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
POLICY_PATH = WORKSPACE_ROOT / "IntRuoyiBackend" / "config" / "gxp-audit-policy.yaml"
SERVICE_PATH = (
    WORKSPACE_ROOT
    / "IntRuoyiBackend"
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
    / "productionrelease"
    / "pqc"
    / "MesActiveOrderDossierFileService.java"
)


def _operations() -> dict[str, dict[str, object]]:
    policy = yaml.safe_load(POLICY_PATH.read_text(encoding="utf-8"))
    return {item["operationId"]: item for item in policy["operations"]}


def test_d09_policy_registers_upload_and_delete_at_real_service_methods() -> None:
    operations = _operations()

    assert operations["mes.dossier.upload"] == {
        "operationId": "mes.dossier.upload",
        "sourceType": "SERVICE_METHOD",
        "sourceLocators": [
            "cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc."
            "MesActiveOrderDossierFileService#upload"
        ],
        "domain": "MES",
        "subjectType": "DOSSIER_ATTACHMENT",
        "actionType": "CREATE",
        "reasonPolicy": "SYSTEM",
        "signaturePolicy": "NONE",
        "statePolicy": "ABSENT_TO_PRESENT",
        "snapshotProfile": "ATTACHMENT",
        "retentionClass": "GXP_MES_DOSSIER",
        "testIds": ["BDD-D09-UPLOAD", "BDD-D09-AUDIT-FAILURE"],
        "ownerRole": "mes-owner",
        "applicability": "GXP",
    }
    assert operations["mes.dossier.delete"] == {
        "operationId": "mes.dossier.delete",
        "sourceType": "SERVICE_METHOD",
        "sourceLocators": [
            "cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc."
            "MesActiveOrderDossierFileService#delete"
        ],
        "domain": "MES",
        "subjectType": "DOSSIER_ATTACHMENT",
        "actionType": "DELETE",
        "reasonPolicy": "SYSTEM",
        "signaturePolicy": "NONE",
        "statePolicy": "PRESENT_TO_ABSENT",
        "snapshotProfile": "ATTACHMENT",
        "retentionClass": "GXP_MES_DOSSIER",
        "testIds": ["BDD-D09-DELETE", "BDD-D09-AUDIT-FAILURE"],
        "ownerRole": "mes-owner",
        "applicability": "GXP",
    }


def test_d09_real_entry_uses_unified_and_professional_audit_keys() -> None:
    source = SERVICE_PATH.read_text(encoding="utf-8")

    assert "GxpAuditService" in source
    assert '"mes.dossier.upload"' in source
    assert '"mes.dossier.delete"' in source
    assert "attachmentId" in source
    assert "idempotencyKey" in source
    assert "MesProEdhrOperationAuditService" in source
    assert "recordInCallerTransaction" in source
