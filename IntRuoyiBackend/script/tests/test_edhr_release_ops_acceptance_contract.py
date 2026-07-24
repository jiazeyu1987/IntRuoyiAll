import hashlib
import json
import re
import subprocess
from pathlib import Path

import pytest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "release-readiness" / "validate-edhr-production-go-no-go.ps1"
EXAMPLE = (
    ROOT
    / "script"
    / "release-readiness"
    / "templates"
    / "edhr-production-go-no-go.example.json"
)
REQUIRED_EDHR_RELEASE_FEATURE_IDS = [
    "feedback-entry/open-or-create",
    "execution-detail/save-submit",
    "approval-workbench/detail-approve-reject",
    "archive-generate-download",
    "batch-execution",
    "tracking-signature",
    "field-audit",
    "domain-trace",
    "permission-matrix",
    "archive-health/runtime-control",
]
REQUIRED_EDHR_RELEASE_CHECKED_SCRIPTS = [
    "e2e:edhr:approval-tracking:check",
    "e2e:edhr:batch-execution:check",
    "e2e:edhr:tracking-signature:check",
    "e2e:edhr:field-audit:check",
    "e2e:edhr:domain-trace:check",
    "e2e:edhr:permission-matrix:check",
    "e2e:edhr:archive-health:check",
]
REQUIRED_EDHR_RELEASE_CHECKED_E2E_FILES = [
    "tests/e2e/edhr-approval-tracking-real-flow.e2e.js",
    "tests/e2e/edhr-batch-execution-real-flow.e2e.js",
    "tests/e2e/edhr-tracking-signature-real-flow.e2e.js",
    "tests/e2e/edhr-field-audit-real-flow.e2e.js",
    "tests/e2e/edhr-domain-trace-real-flow.e2e.js",
    "tests/e2e/edhr-permission-tenant-matrix.e2e.js",
    "tests/e2e/runtime-control-edhr-archive-health.e2e.js",
]
INVALID_RELEASE_COVERAGE_COMMANDS = [
    "python -X utf8 -m pytest script/tests/test_dcc_screenshot_e2e_suite.py -q",
    "echo node scripts/edhr-release-e2e-coverage-gate.mjs --check",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check was documented elsewhere",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --unknown",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report --unknown",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report -x",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report=--unknown",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report=-x",
    'node scripts/edhr-release-e2e-coverage-gate.mjs --check --report "--unknown"',
    'node scripts/edhr-release-e2e-coverage-gate.mjs --check --report="-x"',
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report '--unknown'",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report='-x'",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report test-results/edhr-release-coverage/report.json;echo",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report=test-results/edhr-release-coverage/report.json;echo",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report test-results/edhr-release-coverage/report.json&&echo",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report=test-results/edhr-release-coverage/report.json|echo",
]
VALID_RELEASE_COVERAGE_COMMANDS = [
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report test-results/edhr-release-coverage/report.json",
    "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report=test-results/edhr-release-coverage/report.json",
    "pnpm e2e:edhr:release:check",
    "pnpm run e2e:edhr:release:check --report test-results/edhr-release-coverage/report.json",
    "npm run e2e:edhr:release:check --report=test-results/edhr-release-coverage/report.json",
]


def _run_validator(path: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            "powershell",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(SCRIPT),
            "-EvidencePath",
            str(path),
        ],
        cwd=ROOT,
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )


def _write_json(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


def _write_text(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def _stdout_json(result: subprocess.CompletedProcess[str]) -> dict:
    assert result.stdout, result.stderr
    return json.loads(result.stdout)


def _approval(role: str) -> dict[str, str]:
    return {
        "ownerName": f"{role}-owner",
        "contact": f"{role}-owner@int-ruoyi.internal",
        "approvalTime": "2026-05-28T18:00:00+08:00",
        "approvalEvidence": f"{role} approval ticket",
        "currentDecision": "GO",
    }


def _valid_g8_g9_confirmation(
    release_id: str,
    backup_id: str,
    image_tag: str,
) -> dict:
    rollback_image_tag = "20260528_120000"
    return {
        "releaseId": release_id,
        "currentFaultImageTag": image_tag,
        "g8": {
            "rollbackTriggerId": "INC-20260528-001",
            "rollbackTriggerCondition": "production release rollback condition approved by release owner",
            "SelectedImageTag": rollback_image_tag,
            "imageTagSelectionRule": "Selected from the latest rehearsed image before the current release.",
            "releaseOwnerApproval": {
                "ownerName": "release-owner",
                "approvalTime": "2026-05-28T18:01:00+08:00",
                "approvalEvidence": "release ticket INC-20260528-001",
            },
            "backupRecoveryOperatorApproval": {
                "ownerName": "backup-operator",
                "operatorName": "backup-operator",
                "approvalTime": "2026-05-28T18:02:00+08:00",
                "approvalEvidence": "operator approval record",
            },
            "rollbackValidationEvidence": {
                "action": "rollback-app",
                "status": "success",
                "code": "INTBK-0000",
                "context": {"imageTag": rollback_image_tag},
                "logPath": "D:/IntRuoyi-BackupOps/logs/rollback-app.log",
                "reportPath": "D:/IntRuoyi-BackupOps/logs/rollback-app.report.json",
                "backendHealthEvidence": "actuator health HTTP 200",
                "frontendAccessEvidence": "frontend HTTP 200",
            },
        },
        "g9": {
            "restoreTriggerId": "DATA-20260528-001",
            "restoreTriggerCondition": "restore condition approved by data owner",
            "SelectedBackupId": backup_id,
            "backupIdSelectionRule": "Selected same backupId after manifest, checksums, object snapshot and rehearsal evidence passed.",
            "preRestoreSnapshotId": "pre-restore-20260528-180200",
            "dataOwnerApproval": {
                "ownerName": "data-owner",
                "approvalTime": "2026-05-28T18:03:00+08:00",
                "approvalEvidence": "data owner approval record",
            },
            "releaseOwnerApproval": {
                "ownerName": "release-owner",
                "approvalTime": "2026-05-28T18:04:00+08:00",
                "approvalEvidence": "release owner restore approval",
            },
            "backupRecoveryOperatorApproval": {
                "ownerName": "backup-operator",
                "operatorName": "backup-operator",
                "approvalTime": "2026-05-28T18:05:00+08:00",
                "approvalEvidence": "operator restore approval",
            },
            "businessImpactScope": "Production data restore scope approved for eDHR release gate.",
            "restoreValidationEvidence": {
                "action": "restore-data",
                "status": "success",
                "code": "INTBK-0000",
                "context": {
                    "backupId": backup_id,
                    "restorePoint": backup_id,
                    "preRestoreSnapshotId": "pre-restore-20260528-180200",
                    "imageTag": image_tag,
                },
                "logPath": "D:/IntRuoyi-BackupOps/logs/restore-data.log",
                "reportPath": "D:/IntRuoyi-BackupOps/logs/restore-data.report.json",
                "mysqlRestoreEvidence": "mysql restore completed",
                "objectRestoreEvidence": "minio restore completed",
                "backendHealthEvidence": "actuator health HTTP 200",
                "frontendAccessEvidence": "frontend HTTP 200",
                "loginValidationEvidence": "Playwright login passed",
                "sampleFileEvidence": "sample file downloaded",
            },
        },
    }


def _valid_g10_g11_confirmation(
    release_id: str = "20260528-edhr-production-release",
) -> dict:
    roles = [
        "releaseOwner",
        "backupRecoveryOperator",
        "dataOwner",
        "acceptanceOwner",
        "alertOwner",
        "releaseGateReviewer",
    ]
    return {
        "releaseId": release_id,
        "g10": {
            "notify": {
                "enabled": True,
                "channel": "webhook",
                "webhook": {
                    "url": "https://ops.int-ruoyi.internal/hooks/prod-release",
                    "timeoutSeconds": 10,
                },
            },
            "alertTarget": "IntRuoyi production ops channel",
            "alertOwner": {
                "ownerName": "alert-owner",
                "contact": "alert-owner@int-ruoyi.internal",
            },
            "routeCoverage": [
                "backup-now",
                "backup-scheduled",
                "rollback-app",
                "restore-data started",
                "restore-data finished",
                "rehearsal",
                "cleanup",
            ],
            "sendEvidencePath": "D:/IntRuoyi-BackupOps/logs/202605/g10-webhook-sent.json",
            "notificationStatus": "sent",
        },
        "g11": {role: _approval(role) for role in roles},
    }


def _valid_e2e_release_coverage_report() -> dict:
    return {
        "schemaVersion": 1,
        "command": "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report test-results/edhr-release-coverage/report.json",
        "status": "passed",
        "mode": "check",
        "realGateClaimed": False,
        "featureCount": len(REQUIRED_EDHR_RELEASE_FEATURE_IDS),
        "features": [
            {"featureId": feature_id, "status": "passed"}
            for feature_id in REQUIRED_EDHR_RELEASE_FEATURE_IDS
        ],
        "checkedScripts": list(REQUIRED_EDHR_RELEASE_CHECKED_SCRIPTS),
        "checkedE2eFiles": list(REQUIRED_EDHR_RELEASE_CHECKED_E2E_FILES),
        "failures": [],
    }


def _valid_evidence(tmp_path: Path) -> Path:
    release_id = "20260528-edhr-production-release"
    image_tag = "20260528_180000"
    backup_id = "BK-20260528-180100"
    archive_id = "EDHR-ARCHIVE-20260528-001"
    g8g9_path = tmp_path / "g8g9-confirmation.json"
    g10g11_path = tmp_path / "g10g11-confirmation.json"
    _write_json(g8g9_path, _valid_g8_g9_confirmation(release_id, backup_id, image_tag))
    _write_json(g10g11_path, _valid_g10_g11_confirmation(release_id))

    object_lock_path = tmp_path / "object-lock-proof.json"
    retention_path = tmp_path / "retention-proof.json"
    legal_hold_path = tmp_path / "legal-hold-proof.json"
    manifest_path = tmp_path / "backup-manifest.json"
    checksums_path = tmp_path / "backup-checksums.sha256"
    rehearsal_report_path = tmp_path / "rehearsal-report.json"
    archive_evidence_path = tmp_path / "archive-evidence.json"
    hash_evidence_path = tmp_path / "hash-evidence.json"
    restore_evidence_path = tmp_path / "restore-validation-evidence.json"
    backend_report_path = tmp_path / "backend-ci-report.json"
    frontend_report_path = tmp_path / "frontend-ci-report.json"
    e2e_report_path = tmp_path / "edhr-release-coverage-report.json"

    _write_json(object_lock_path, {"status": "PASS", "mode": "COMPLIANCE"})
    _write_json(retention_path, {"status": "PASS", "days": 3650})
    _write_json(legal_hold_path, {"status": "PASS", "required": True})
    _write_json(
        manifest_path,
        {
            "backupId": backup_id,
            "currentImageTag": image_tag,
            "artifact": "edhr-production-release",
        },
    )
    _write_text(
        checksums_path,
        f"{backup_id} {manifest_path.name} "
        "8f4a3e5ed7b4db9b0fc6ff0c2b13a2e4c2f16764f675cc8df0f0cc7d9a4e30f2\n",
    )
    _write_json(rehearsal_report_path, {"status": "PASSED", "backupId": backup_id})
    _write_json(archive_evidence_path, {"archiveId": archive_id, "backupId": backup_id})
    _write_json(
        hash_evidence_path,
        {
            "algorithm": "SHA-256",
            "sha256": "8f4a3e5ed7b4db9b0fc6ff0c2b13a2e4c2f16764f675cc8df0f0cc7d9a4e30f2",
        },
    )
    _write_json(
        restore_evidence_path,
        {"status": "success", "validatedArchiveId": archive_id},
    )
    _write_json(backend_report_path, {"status": "passed", "tests": 42, "skipped": 0})
    _write_json(frontend_report_path, {"status": "passed", "tests": 18, "skipped": 0})
    _write_json(e2e_report_path, _valid_e2e_release_coverage_report())

    evidence = {
        "releaseId": release_id,
        "currentImageTag": image_tag,
        "backupId": backup_id,
        "environment": "production",
        "protectedStorageVerifierEvidence": {
            "status": "PASS",
            "objectLock": {
                "enabled": True,
                "mode": "COMPLIANCE",
                "evidencePath": str(object_lock_path),
            },
            "retention": {
                "mode": "COMPLIANCE",
                "days": 3650,
                "evidencePath": str(retention_path),
            },
            "legalHold": {
                "required": True,
                "evidencePath": str(legal_hold_path),
            },
        },
        "backupNowReport": {
            "status": "success",
            "code": "INTBK-0000",
            "currentImageTag": image_tag,
            "backupId": backup_id,
            "manifestPath": str(manifest_path),
            "checksumsPath": str(checksums_path),
        },
        "rehearsalReport": {
            "status": "PASSED",
            "result": "success",
            "backupId": backup_id,
            "reportPath": str(rehearsal_report_path),
            "edhrArchiveEvidence": {
                "archiveId": archive_id,
                "archivePath": "s3://edhr-prod-archive/2026/05/28/archive.json",
                "evidencePath": str(archive_evidence_path),
            },
            "edhrHashEvidence": {
                "algorithm": "SHA-256",
                "sha256": "8f4a3e5ed7b4db9b0fc6ff0c2b13a2e4c2f16764f675cc8df0f0cc7d9a4e30f2",
                "evidencePath": str(hash_evidence_path),
            },
            "restoreValidationEvidence": {
                "status": "success",
                "validatedArchiveId": archive_id,
                "sampleRecord": "batch-record-20260528-001",
                "evidencePath": str(restore_evidence_path),
            },
        },
        "g8g9ConfirmationPath": str(g8g9_path),
        "g10g11ConfirmationPath": str(g10g11_path),
        "ciEvidence": {
            "backendTests": {
                "status": "passed",
                "command": "python -X utf8 -m pytest script/tests/test_edhr_archive_business_health_contract.py -q",
                "reportPath": str(backend_report_path),
            },
            "frontendTests": {
                "status": "passed",
                "command": "pnpm test --run",
                "reportPath": str(frontend_report_path),
            },
            "e2eGates": {
                "status": "passed",
                "command": "node scripts/edhr-release-e2e-coverage-gate.mjs --check --report test-results/edhr-release-coverage/report.json",
                "reportPath": str(e2e_report_path),
            },
        },
    }
    evidence_path = tmp_path / "edhr-production-go-no-go.json"
    _write_json(evidence_path, evidence)
    return evidence_path


def test_missing_evidence_file_outputs_no_go_json(tmp_path: Path) -> None:
    result = _run_validator(tmp_path / "missing-go-no-go.json")
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "EvidencePath does not exist" in payload["blockedReasons"][0]


def test_example_template_stays_no_go_until_real_evidence_is_filled() -> None:
    result = _run_validator(EXAMPLE)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "currentImageTag is required and must not be a placeholder" in payload[
        "blockedReasons"
    ]
    assert "protectedStorageVerifierEvidence.status must equal PASS" in payload[
        "blockedReasons"
    ]


def test_backup_and_rehearsal_must_use_the_same_backup_id(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    evidence["rehearsalReport"]["backupId"] = "BK-20260528-DIFFERENT"
    _write_json(evidence_path, evidence)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "rehearsalReport.backupId must equal backupNowReport.backupId" in payload[
        "blockedReasons"
    ]


def test_critical_evidence_paths_must_exist(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    evidence["protectedStorageVerifierEvidence"]["legalHold"]["evidencePath"] = str(
        tmp_path / "missing-legal-hold-proof.json"
    )
    _write_json(evidence_path, evidence)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert any(
        reason.startswith(
            "protectedStorageVerifierEvidence.legalHold.evidencePath file does not exist"
        )
        for reason in payload["blockedReasons"]
    )


def test_backup_manifest_and_checksums_must_match_release_evidence(
    tmp_path: Path,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    manifest_path = Path(evidence["backupNowReport"]["manifestPath"])
    checksums_path = Path(evidence["backupNowReport"]["checksumsPath"])
    _write_json(
        manifest_path,
        {
            "backupId": "BK-20260528-DIFFERENT",
            "currentImageTag": "20260528_DIFFERENT",
        },
    )
    _write_text(checksums_path, "unrelated checksum evidence\n")

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "backupNowReport.manifestPath backupId must equal backupId" in payload[
        "blockedReasons"
    ]
    assert (
        "backupNowReport.manifestPath currentImageTag must equal currentImageTag"
        in payload["blockedReasons"]
    )
    assert (
        "backupNowReport.checksumsPath must contain at least one SHA-256 checksum item"
        in payload["blockedReasons"]
    )
    assert (
        "backupNowReport.checksumsPath must reference backupId or manifest file name"
        in payload["blockedReasons"]
    )


def test_checksums_cannot_be_unbound_to_current_backup_or_manifest(
    tmp_path: Path,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    checksums_path = Path(evidence["backupNowReport"]["checksumsPath"])
    _write_text(
        checksums_path,
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa unrelated.bin\n",
    )

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert (
        "backupNowReport.checksumsPath must reference backupId or manifest file name"
        in payload["blockedReasons"]
    )


def test_ci_evidence_requires_command_and_report_path(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    del evidence["ciEvidence"]["backendTests"]["command"]
    del evidence["ciEvidence"]["backendTests"]["reportPath"]
    _write_json(evidence_path, evidence)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "ciEvidence.backendTests.command is required and must not be a placeholder" in payload[
        "blockedReasons"
    ]
    assert "ciEvidence.backendTests.reportPath is required and must not be a placeholder" in payload[
        "blockedReasons"
    ]


def test_ci_report_with_skip_flags_blocks_go(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    frontend_report = Path(evidence["ciEvidence"]["frontendTests"]["reportPath"])
    _write_text(frontend_report, "pnpm test passed but used skipTests=true\n")

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "ciEvidence.frontendTests.reportPath must not contain maven.test.skip=true or skipTests" in payload[
        "blockedReasons"
    ]


def test_failed_ci_status_blocks_go(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    evidence["ciEvidence"]["e2eGates"]["status"] = "failed"
    _write_json(evidence_path, evidence)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "ciEvidence.e2eGates.status must equal passed" in payload[
        "blockedReasons"
    ]


def _load_e2e_report(evidence_path: Path) -> tuple[dict, Path, dict]:
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    e2e_report = Path(evidence["ciEvidence"]["e2eGates"]["reportPath"])
    report = json.loads(e2e_report.read_text(encoding="utf-8"))
    return evidence, e2e_report, report


def test_e2e_gate_report_must_be_edhr_release_coverage_json(
    tmp_path: Path,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence, e2e_report, _report = _load_e2e_report(evidence_path)
    _write_json(e2e_report, {"status": "passed", "tests": 9, "skipped": 0})

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "ciEvidence.e2eGates.reportPath.schemaVersion is required and must not be a placeholder" in payload[
        "blockedReasons"
    ]
    assert "ciEvidence.e2eGates.reportPath.features must be a non-empty array" in payload[
        "blockedReasons"
    ]


def test_e2e_gate_report_schema_version_must_match_frontend_contract(
    tmp_path: Path,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    _evidence, e2e_report, report = _load_e2e_report(evidence_path)
    report["schemaVersion"] = "edhr-release-e2e-coverage.v0"
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.reportPath.schemaVersion must equal 1" in payload[
        "blockedReasons"
    ]


def test_e2e_gate_report_must_be_valid_json(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    _evidence, e2e_report, _report = _load_e2e_report(evidence_path)
    _write_text(e2e_report, "not json\n")

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert any(
        reason.startswith("ciEvidence.e2eGates.reportPath must contain valid UTF-8 JSON")
        for reason in payload["blockedReasons"]
    )


def test_e2e_gate_report_command_is_required(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    _evidence, e2e_report, report = _load_e2e_report(evidence_path)
    del report["command"]
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.reportPath.command is required and must not be a placeholder" in payload[
        "blockedReasons"
    ]


@pytest.mark.parametrize("invalid_command", INVALID_RELEASE_COVERAGE_COMMANDS)
def test_e2e_gate_report_command_must_be_canonical_release_check(
    tmp_path: Path,
    invalid_command: str,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    _evidence, e2e_report, report = _load_e2e_report(evidence_path)
    report["command"] = invalid_command
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.reportPath.command must run a canonical eDHR release coverage check command" in payload[
        "blockedReasons"
    ]


@pytest.mark.parametrize("valid_command", VALID_RELEASE_COVERAGE_COMMANDS)
def test_e2e_gate_allows_canonical_release_coverage_commands(
    tmp_path: Path,
    valid_command: str,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence, e2e_report, report = _load_e2e_report(evidence_path)
    evidence["ciEvidence"]["e2eGates"]["command"] = valid_command
    report["command"] = valid_command
    _write_json(evidence_path, evidence)
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 0
    assert payload["decision"] == "GO"


@pytest.mark.parametrize(
    ("field", "value", "expected_blocker"),
    [
        ("mode", "run-real", "ciEvidence.e2eGates.reportPath.mode must equal check"),
        ("realGateClaimed", True, "ciEvidence.e2eGates.reportPath.realGateClaimed must equal false"),
        ("failures", [{"featureId": "tracking-signature"}], "ciEvidence.e2eGates.reportPath.failures must be empty"),
    ],
)
def test_e2e_gate_report_top_level_fields_are_fail_closed(
    tmp_path: Path,
    field: str,
    value: object,
    expected_blocker: str,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    _evidence, e2e_report, report = _load_e2e_report(evidence_path)
    report[field] = value
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert expected_blocker in payload["blockedReasons"]


def test_e2e_gate_report_feature_status_must_be_passed(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    _evidence, e2e_report, report = _load_e2e_report(evidence_path)
    report["features"][0]["status"] = "failed"
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.reportPath feature feedback-entry/open-or-create status must equal passed" in payload[
        "blockedReasons"
    ]


def test_e2e_gate_missing_required_feature_names_blocker(
    tmp_path: Path,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    _evidence, e2e_report, report = _load_e2e_report(evidence_path)
    report["features"] = [
        feature
        for feature in report["features"]
        if feature["featureId"] != "permission-matrix"
    ]
    report["featureCount"] = len(report["features"])
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.reportPath missing required featureId: permission-matrix" in payload[
        "blockedReasons"
    ]


def test_e2e_gate_unexpected_or_duplicate_features_block_go(
    tmp_path: Path,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    _evidence, e2e_report, report = _load_e2e_report(evidence_path)
    report["features"].append({"featureId": "unexpected-future-feature", "status": "passed"})
    report["features"].append({"featureId": "permission-matrix", "status": "passed"})
    report["featureCount"] = len(report["features"])
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.reportPath unexpected featureId: unexpected-future-feature" in payload[
        "blockedReasons"
    ]
    assert "ciEvidence.e2eGates.reportPath duplicate featureId: permission-matrix" in payload[
        "blockedReasons"
    ]


def test_e2e_gate_report_checked_scripts_must_match_required_matrix(
    tmp_path: Path,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    _evidence, e2e_report, report = _load_e2e_report(evidence_path)
    del report["checkedScripts"]
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.reportPath.checkedScripts must be a non-empty array" in payload[
        "blockedReasons"
    ]

    report["checkedScripts"] = [
        script
        for script in REQUIRED_EDHR_RELEASE_CHECKED_SCRIPTS
        if script != "e2e:edhr:field-audit:check"
    ]
    report["checkedScripts"].append("e2e:edhr:unknown:check")
    report["checkedScripts"].append("e2e:edhr:approval-tracking:check")
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.reportPath.checkedScripts unexpected item: e2e:edhr:unknown:check" in payload[
        "blockedReasons"
    ]
    assert "ciEvidence.e2eGates.reportPath.checkedScripts duplicate item: e2e:edhr:approval-tracking:check" in payload[
        "blockedReasons"
    ]
    assert "ciEvidence.e2eGates.reportPath.checkedScripts missing required item: e2e:edhr:field-audit:check" in payload[
        "blockedReasons"
    ]


def test_e2e_gate_report_checked_e2e_files_must_match_required_matrix(
    tmp_path: Path,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    _evidence, e2e_report, report = _load_e2e_report(evidence_path)
    del report["checkedE2eFiles"]
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.reportPath.checkedE2eFiles must be a non-empty array" in payload[
        "blockedReasons"
    ]

    report["checkedE2eFiles"] = [
        file
        for file in REQUIRED_EDHR_RELEASE_CHECKED_E2E_FILES
        if file != "tests/e2e/edhr-field-audit-real-flow.e2e.js"
    ]
    report["checkedE2eFiles"].append("tests/e2e/unknown-edhr-flow.e2e.js")
    report["checkedE2eFiles"].append(
        "tests/e2e/edhr-approval-tracking-real-flow.e2e.js"
    )
    _write_json(e2e_report, report)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.reportPath.checkedE2eFiles unexpected item: tests/e2e/unknown-edhr-flow.e2e.js" in payload[
        "blockedReasons"
    ]
    assert "ciEvidence.e2eGates.reportPath.checkedE2eFiles duplicate item: tests/e2e/edhr-approval-tracking-real-flow.e2e.js" in payload[
        "blockedReasons"
    ]
    assert "ciEvidence.e2eGates.reportPath.checkedE2eFiles missing required item: tests/e2e/edhr-field-audit-real-flow.e2e.js" in payload[
        "blockedReasons"
    ]


@pytest.mark.parametrize("invalid_command", INVALID_RELEASE_COVERAGE_COMMANDS)
def test_e2e_gate_outer_command_must_be_canonical_release_check(
    tmp_path: Path,
    invalid_command: str,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    evidence["ciEvidence"]["e2eGates"]["command"] = invalid_command
    _write_json(evidence_path, evidence)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert "ciEvidence.e2eGates.command must run a canonical eDHR release coverage check command" in payload[
        "blockedReasons"
    ]


def test_g8_g9_confirmation_must_match_top_release_image_and_backup(
    tmp_path: Path,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    g8g9_path = Path(evidence["g8g9ConfirmationPath"])
    confirmation = json.loads(g8g9_path.read_text(encoding="utf-8"))
    confirmation["releaseId"] = "20260528-different-release"
    confirmation["currentFaultImageTag"] = "20260528_999999"
    confirmation["g9"]["SelectedBackupId"] = "BK-20260528-DIFFERENT"
    confirmation["g9"]["restoreValidationEvidence"]["context"][
        "backupId"
    ] = "BK-20260528-DIFFERENT"
    confirmation["g9"]["restoreValidationEvidence"]["context"][
        "restorePoint"
    ] = "BK-20260528-DIFFERENT"
    _write_json(g8g9_path, confirmation)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "G8/G9 releaseId must equal releaseId" in payload["blockedReasons"]
    assert "G8/G9 currentFaultImageTag must equal currentImageTag" in payload[
        "blockedReasons"
    ]
    assert "G8/G9 g9.SelectedBackupId must equal backupId" in payload[
        "blockedReasons"
    ]


def test_g10_g11_confirmation_must_match_top_release(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    g10g11_path = Path(evidence["g10g11ConfirmationPath"])
    confirmation = json.loads(g10g11_path.read_text(encoding="utf-8"))
    confirmation["releaseId"] = "20260528-different-release"
    _write_json(g10g11_path, confirmation)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "G10/G11 releaseId must equal releaseId" in payload["blockedReasons"]


def test_restore_validation_archive_id_must_match_archive_evidence(
    tmp_path: Path,
) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    evidence["rehearsalReport"]["restoreValidationEvidence"][
        "validatedArchiveId"
    ] = "EDHR-ARCHIVE-20260528-DIFFERENT"
    _write_json(evidence_path, evidence)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert (
        "rehearsalReport.restoreValidationEvidence.validatedArchiveId must equal rehearsalReport.edhrArchiveEvidence.archiveId"
        in payload["blockedReasons"]
    )


def test_hash_evidence_requires_sha256_algorithm_and_hex(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    evidence["rehearsalReport"]["edhrHashEvidence"]["algorithm"] = "SHA-1"
    evidence["rehearsalReport"]["edhrHashEvidence"]["sha256"] = "not-a-sha"
    _write_json(evidence_path, evidence)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert "rehearsalReport.edhrHashEvidence.algorithm must equal SHA-256" in payload[
        "blockedReasons"
    ]
    assert (
        "rehearsalReport.edhrHashEvidence.sha256 must be a 64 character hex SHA-256 digest"
        in payload["blockedReasons"]
    )


def test_g10_g11_validator_failure_blocks_production_go(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    g10g11_path = Path(evidence["g10g11ConfirmationPath"])
    invalid_confirmation = _valid_g10_g11_confirmation()
    invalid_confirmation["g11"]["releaseOwner"]["currentDecision"] = "NO-GO"
    _write_json(g10g11_path, invalid_confirmation)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "NO-GO"
    assert any(
        reason.startswith("G10/G11 confirmation failed")
        for reason in payload["blockedReasons"]
    )


def test_complete_synthetic_evidence_outputs_go(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)

    result = _run_validator(evidence_path)
    payload = _stdout_json(result)

    assert result.returncode == 0
    assert payload["decision"] == "GO"
    assert payload["blockedReasons"] == []
    assert payload["readOnly"] is True
    assert payload["sendsWebhook"] is False


def test_validator_is_read_only_and_does_not_modify_input_files(tmp_path: Path) -> None:
    evidence_path = _valid_evidence(tmp_path)
    evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    input_files = [
        evidence_path,
        Path(evidence["g8g9ConfirmationPath"]),
        Path(evidence["g10g11ConfirmationPath"]),
    ]
    before = {
        path: hashlib.sha256(path.read_bytes()).hexdigest()
        for path in input_files
    }

    result = _run_validator(evidence_path)

    assert result.returncode == 0
    after = {
        path: hashlib.sha256(path.read_bytes()).hexdigest()
        for path in input_files
    }
    assert after == before


def test_validator_script_does_not_contain_environment_mutation_or_action_calls() -> None:
    text = SCRIPT.read_text(encoding="utf-8")
    banned_command_invocations = [
        "Invoke-WebRequest",
        "Invoke-RestMethod",
        "Start-Process",
        "Set-Content",
        "Add-Content",
        "Out-File",
        "Remove-Item",
        "Copy-Item",
        "Move-Item",
    ]
    for command in banned_command_invocations:
        assert re.search(rf"(?im)^\s*{re.escape(command)}\b", text) is None

    lower_text = text.lower()
    assert "backup-now.ps1" not in lower_text
    assert "rollback-app.ps1" not in lower_text
    assert "restore-data.ps1" not in lower_text
