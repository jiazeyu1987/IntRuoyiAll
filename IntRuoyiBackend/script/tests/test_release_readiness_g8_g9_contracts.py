import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "release-readiness" / "validate-g8-g9-confirmations.ps1"
EXAMPLE = (
    ROOT
    / "script"
    / "release-readiness"
    / "templates"
    / "g8-g9-confirmation.example.json"
)


def _run_validator(path: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            "powershell",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(SCRIPT),
            "-ConfirmationPath",
            str(path),
        ],
        cwd=ROOT,
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )


def _valid_confirmation() -> dict:
    return {
        "releaseId": "20260524-int-ruoyi-ops",
        "currentFaultImageTag": "20260524_035800",
        "g8": {
            "rollbackTriggerId": "INC-20260524-001",
            "rollbackTriggerCondition": "frontend login fails while MySQL and MinIO remain trusted",
            "SelectedImageTag": "20260523_221500",
            "imageTagSelectionRule": "Selected from deploy/image-tag.txt for the latest rehearsed backup point, excluding currentFaultImageTag.",
            "releaseOwnerApproval": {
                "ownerName": "release-owner",
                "approvalTime": "2026-05-24T20:00:00+08:00",
                "approvalEvidence": "release ticket INC-20260524-001",
            },
            "backupRecoveryOperatorApproval": {
                "ownerName": "backup-operator",
                "operatorName": "backup-operator",
                "approvalTime": "2026-05-24T20:01:00+08:00",
                "approvalEvidence": "operator approval record",
            },
            "rollbackValidationEvidence": {
                "action": "rollback-app",
                "status": "success",
                "code": "INTBK-0000",
                "context": {"imageTag": "20260523_221500"},
                "logPath": "D:/IntRuoyi-BackupOps/logs/rollback-app.log",
                "reportPath": "D:/IntRuoyi-BackupOps/logs/rollback-app.report.json",
                "backendHealthEvidence": "actuator health HTTP 200",
                "frontendAccessEvidence": "frontend HTTP 200",
            },
        },
        "g9": {
            "restoreTriggerId": "DATA-20260524-001",
            "restoreTriggerCondition": "MySQL data corruption confirmed by data owner",
            "SelectedBackupId": "20260524_180051",
            "backupIdSelectionRule": "Selected same backupId after manifest, checksums, mysql dump, object snapshot and rehearsal evidence passed.",
            "preRestoreSnapshotId": "pre-restore-20260524-200200",
            "dataOwnerApproval": {
                "ownerName": "data-owner",
                "approvalTime": "2026-05-24T20:02:00+08:00",
                "approvalEvidence": "data owner approval record",
            },
            "releaseOwnerApproval": {
                "ownerName": "release-owner",
                "approvalTime": "2026-05-24T20:03:00+08:00",
                "approvalEvidence": "release owner restore approval",
            },
            "backupRecoveryOperatorApproval": {
                "ownerName": "backup-operator",
                "operatorName": "backup-operator",
                "approvalTime": "2026-05-24T20:04:00+08:00",
                "approvalEvidence": "operator restore approval",
            },
            "businessImpactScope": "Restores MySQL and MinIO to backupId 20260524_180051, freezes writes during the recovery window.",
            "restoreValidationEvidence": {
                "action": "restore-data",
                "status": "success",
                "code": "INTBK-0000",
                "context": {
                    "backupId": "20260524_180051",
                    "restorePoint": "20260524_180051",
                    "preRestoreSnapshotId": "pre-restore-20260524-200200",
                    "imageTag": "20260524_035800",
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


def _write_json(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


def _stdout_json(result: subprocess.CompletedProcess[str]) -> dict:
    assert result.stdout, result.stderr
    return json.loads(result.stdout)


def test_missing_required_confirmation_fields_fail_closed(tmp_path: Path) -> None:
    confirmation = _valid_confirmation()
    del confirmation["g8"]["rollbackTriggerCondition"]
    del confirmation["g9"]["preRestoreSnapshotId"]
    path = tmp_path / "missing-fields.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g8.rollbackTriggerCondition is required" in payload["blockedReasons"]
    assert "g9.preRestoreSnapshotId is required" in payload["blockedReasons"]


def test_selected_image_tag_must_not_equal_current_fault_tag(tmp_path: Path) -> None:
    confirmation = _valid_confirmation()
    confirmation["g8"]["SelectedImageTag"] = confirmation["currentFaultImageTag"]
    confirmation["g8"]["rollbackValidationEvidence"]["context"]["imageTag"] = confirmation[
        "currentFaultImageTag"
    ]
    path = tmp_path / "same-image-tag.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g8.SelectedImageTag must not equal currentFaultImageTag" in payload[
        "blockedReasons"
    ]


def test_restore_data_evidence_must_use_same_backup_id_and_pre_restore_snapshot(
    tmp_path: Path,
) -> None:
    confirmation = _valid_confirmation()
    confirmation["g9"]["restoreValidationEvidence"]["context"]["restorePoint"] = (
        "20260524_183322"
    )
    confirmation["g9"]["restoreValidationEvidence"]["context"]["preRestoreSnapshotId"] = (
        "different-pre-restore-snapshot"
    )
    path = tmp_path / "restore-mismatch.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert (
        "g9.restoreValidationEvidence.context.restorePoint must equal g9.SelectedBackupId"
        in payload["blockedReasons"]
    )
    assert (
        "g9.restoreValidationEvidence.context.preRestoreSnapshotId must equal g9.preRestoreSnapshotId"
        in payload["blockedReasons"]
    )


def test_rollback_validation_evidence_must_have_success_status_and_code(
    tmp_path: Path,
) -> None:
    confirmation = _valid_confirmation()
    confirmation["g8"]["rollbackValidationEvidence"]["status"] = "failed"
    confirmation["g8"]["rollbackValidationEvidence"]["code"] = "INTBK-9999"
    path = tmp_path / "rollback-failed-status-code.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g8.rollbackValidationEvidence.status must equal success" in payload[
        "blockedReasons"
    ]
    assert "g8.rollbackValidationEvidence.code must equal INTBK-0000" in payload[
        "blockedReasons"
    ]


def test_restore_validation_evidence_must_have_success_status_and_code(
    tmp_path: Path,
) -> None:
    confirmation = _valid_confirmation()
    confirmation["g9"]["restoreValidationEvidence"]["status"] = "failed"
    confirmation["g9"]["restoreValidationEvidence"]["code"] = "INTBK-9999"
    path = tmp_path / "restore-failed-status-code.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g9.restoreValidationEvidence.status must equal success" in payload[
        "blockedReasons"
    ]
    assert "g9.restoreValidationEvidence.code must equal INTBK-0000" in payload[
        "blockedReasons"
    ]


def test_complete_confirmation_contract_outputs_go(tmp_path: Path) -> None:
    path = tmp_path / "complete.json"
    _write_json(path, _valid_confirmation())

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 0
    assert payload["decision"] == "GO"
    assert payload["blockedReasons"] == []


def test_example_confirmation_stays_blocked_until_real_owners_fill_it() -> None:
    result = _run_validator(EXAMPLE)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g8.rollbackTriggerCondition is required" in payload["blockedReasons"]
    assert "g9.restoreTriggerCondition is required" in payload["blockedReasons"]
