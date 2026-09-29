"""Runtime policy must describe the already implemented audited public paths."""
from pathlib import Path

import yaml
import pytest


ROOT = Path(__file__).resolve().parents[3]

@pytest.mark.parametrize("operation,method", [
    ("mes.work-task.entitlement", "entitlements"), ("mes.work-task.notify", "notifications")
])
def test_current_work_task_auxiliary_effects_are_registered(operation, method):
    policy = yaml.safe_load((ROOT / "IntRuoyiBackend/config/gxp-audit-policy.yaml").read_text(encoding="utf-8"))
    operations = {row["operationId"]: row for row in policy["operations"]}
    assert operation in operations, "Current task side effects need their actual audit owner"
    assert operations[operation]["sourceLocators"] == [
        "cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesWorkTaskAuxiliaryAudit#" + method]
    assert operations[operation]["signaturePolicy"] == "NONE"
    assert operations[operation]["statePolicy"] == "PRESENT_TO_PRESENT"
    assert operation in policy["coverageScope"]["includedReferences"]


@pytest.mark.parametrize("operation", ["mes.batch-trace.provision", "mes.batch-trace.provision-failure"])
def test_current_post_commit_trace_transactions_have_independent_registered_operations(operation):
    policy = yaml.safe_load((ROOT / "IntRuoyiBackend/config/gxp-audit-policy.yaml").read_text(encoding="utf-8"))
    operations = {row["operationId"]: row for row in policy["operations"]}
    assert operation in operations, "Post-commit state cannot be attributed to the committed parent audit"
    definition = operations[operation]
    assert definition["sourceLocators"] == [
        "cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceTxCProducer#produce"]
    assert definition["signaturePolicy"] == "NONE"
    assert definition["reasonPolicy"] == "SYSTEM"
    assert definition["statePolicy"] == "PRESENT_TO_PRESENT"
    assert operation in policy["coverageScope"]["includedReferences"]


@pytest.mark.parametrize("operation,method,signature,state", [
    ("mes.employee-production-signature.create", "recordProductionSubmitSignature", "NONE", "ABSENT_TO_PRESENT"),
    ("edhr.field-save-evidence.create", "recordFieldChangeDraftSave", "NONE", "ABSENT_TO_PRESENT"),
    ("edhr.field-save-evidence.bind", "attachFieldChangeSignature", "NONE", "PRESENT_TO_PRESENT"),
    ("mes.production-report.correct", "correct", "REQUIRED", "PRESENT_TO_PRESENT"),
    ("mes.nonconformance.active-order.create", "create", "NONE", "ABSENT_TO_PRESENT"),
])
def test_current_projection_correction_and_unsigned_ncr_operations_are_registered(operation, method, signature, state):
    policy = yaml.safe_load((ROOT / "IntRuoyiBackend/config/gxp-audit-policy.yaml").read_text(encoding="utf-8"))
    operations = {row["operationId"]: row for row in policy["operations"]}
    assert operation in operations
    definition = operations[operation]
    assert any(locator.endswith("#" + method) for locator in definition["sourceLocators"])
    assert definition["signaturePolicy"] == signature
    assert definition["statePolicy"] == state
    assert operation in policy["coverageScope"]["includedReferences"]


@pytest.mark.parametrize("operation,method,state", [
    ("mes.pqc-inspection.correct", "correct", "PRESENT_TO_PRESENT"),
    ("mes.nonconformance.create", "create", "ABSENT_TO_PRESENT"),
    ("mes.nonconformance.concession", "dispose", "PRESENT_TO_PRESENT"),
    ("mes.nonconformance.rework", "dispose", "PRESENT_TO_PRESENT"),
    ("mes.nonconformance.void", "dispose", "PRESENT_TO_PRESENT"),
])
def test_existing_signed_correction_and_ncr_audit_operations_are_loadable(operation, method, state):
    policy = yaml.safe_load((ROOT / "IntRuoyiBackend/config/gxp-audit-policy.yaml").read_text(encoding="utf-8"))
    operations = {row["operationId"]: row for row in policy["operations"]}
    assert operation in operations
    definition = operations[operation]
    assert any(locator.endswith("#" + method) for locator in definition["sourceLocators"])
    assert definition["signaturePolicy"] == "REQUIRED"
    assert definition["reasonPolicy"] == "USER_REQUIRED"
    assert definition["statePolicy"] == state
    assert operation in policy["coverageScope"]["includedReferences"]


def test_implemented_release_and_maintenance_operations_are_registered():
    policy = yaml.safe_load((ROOT / "IntRuoyiBackend/config/gxp-audit-policy.yaml").read_text(encoding="utf-8"))
    operations = {row["operationId"]: row for row in policy["operations"]}
    expected = {
        "mes.market-release.precheck-create": ("precheck", "ABSENT_TO_PRESENT"),
        "mes.market-release.precheck": ("precheck", "PRESENT_TO_PRESENT"),
        "mes.market-release.submit": ("submit", "PRESENT_TO_PRESENT"),
        "mes.market-release.reject": ("reject", "PRESENT_TO_PRESENT"),
        "mes.market-release.withdraw": ("withdraw", "PRESENT_TO_PRESENT"),
        "mes.active-order.rebuild": ("rebuildActiveOrder", "PRESENT_TO_PRESENT"),
        "mes.active-order.remove": ("removeActiveOrder", "PRESENT_TO_PRESENT"),
        "mes.pqc-release.bind-batch": ("applyGenerated", "PRESENT_TO_PRESENT"),
        "mes.active-order.reorder": ("moveActiveOrder", "PRESENT_TO_PRESENT"),
        "mes.active-order.data-cleanup": ("executeDataCleanup", "PRESENT_TO_PRESENT"),
    }
    for key, (method, state) in expected.items():
        assert key in operations, key
        assert any(value.endswith("#" + method) for value in operations[key]["sourceLocators"]), key
        assert operations[key]["statePolicy"] == state
        assert operations[key]["signaturePolicy"] == "NONE", "Do not invent a signature for an unsigned action"
    assert any(value.endswith("#submitForApproval") for value in operations["mes.market-release.submit"]["sourceLocators"])


def test_public_delegators_have_explicit_operation_registration():
    policy = yaml.safe_load((ROOT / "IntRuoyiBackend/config/gxp-audit-policy.yaml").read_text(encoding="utf-8"))
    operations = {row["operationId"]: row for row in policy["operations"]}
    expected = {"edhr.execution.field.update": "saveSystemCellLinkChanges",
                "mes.market-release.approve": "approve", "mes.active-order.complete": "completeForRelease",
                "mes.pqc-release.apply": "applyGenerated", "system.permission.role-menu.assign": "assignTenantRoleMenu",
                "system.permission.user-role.assign": "initializeTenantAdministrator"}
    for operation, method in expected.items():
        assert any(value.endswith("#" + method) for value in operations[operation]["sourceLocators"]), operation
def test_current_snapshot_profiles_are_supported_by_runtime_schema():
    import json
    policy = yaml.safe_load((ROOT / "IntRuoyiBackend/config/gxp-audit-policy.yaml").read_text(encoding="utf-8"))
    schema = json.loads((ROOT / "IntRuoyiBackend/config/gxp-audit-policy.schema.json").read_text(encoding="utf-8"))
    allowed = schema["properties"]["operations"]["items"]["properties"]["snapshotProfile"]["enum"]
    for operation in policy["operations"]:
        assert operation["snapshotProfile"] in allowed, operation["operationId"]
