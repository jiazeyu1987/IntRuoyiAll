import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "release-readiness" / "validate-g10-g11-confirmations.ps1"
EXAMPLE = (
    ROOT
    / "script"
    / "release-readiness"
    / "templates"
    / "g10-g11-confirmation.example.json"
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


def _approval(role: str) -> dict[str, str]:
    return {
        "ownerName": f"{role}-owner",
        "contact": f"{role}-owner@int-ruoyi.internal",
        "approvalTime": "2026-05-24T20:00:00+08:00",
        "approvalEvidence": f"{role} approval ticket",
        "currentDecision": "GO",
    }


def _valid_confirmation() -> dict:
    roles = [
        "releaseOwner",
        "backupRecoveryOperator",
        "dataOwner",
        "acceptanceOwner",
        "alertOwner",
        "releaseGateReviewer",
    ]
    return {
        "releaseId": "20260524-int-ruoyi-ops",
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


def _write_json(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


def _stdout_json(result: subprocess.CompletedProcess[str]) -> dict:
    assert result.stdout, result.stderr
    return json.loads(result.stdout)


def test_g10_missing_webhook_route_evidence_fails_closed(tmp_path: Path) -> None:
    confirmation = _valid_confirmation()
    confirmation["g10"]["notify"]["enabled"] = False
    confirmation["g10"]["notify"]["channel"] = "pending"
    confirmation["g10"]["notify"]["webhook"]["url"] = ""
    del confirmation["g10"]["alertTarget"]
    del confirmation["g10"]["sendEvidencePath"]
    path = tmp_path / "g10-missing-route.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g10.notify.enabled must be true" in payload["blockedReasons"]
    assert "g10.notify.channel must be webhook" in payload["blockedReasons"]
    assert "g10.notify.webhook.url is required" in payload["blockedReasons"]
    assert "g10.alertTarget is required" in payload["blockedReasons"]
    assert "g10.sendEvidencePath is required" in payload["blockedReasons"]


def test_g10_bad_notification_statuses_are_blocked(tmp_path: Path) -> None:
    for status in ["disabled", "pending", "unsupported", "failed"]:
        confirmation = _valid_confirmation()
        confirmation["g10"]["notificationStatus"] = status
        path = tmp_path / f"g10-{status}.json"
        _write_json(path, confirmation)

        result = _run_validator(path)
        payload = _stdout_json(result)

        assert result.returncode == 2
        assert payload["decision"] == "BLOCKED"
        assert "g10.notificationStatus must be sent" in payload["blockedReasons"]


def test_g10_requires_complete_route_coverage(tmp_path: Path) -> None:
    confirmation = _valid_confirmation()
    confirmation["g10"]["routeCoverage"] = ["backup-now", "restore-data started"]
    path = tmp_path / "g10-route-coverage.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g10.routeCoverage missing backup-scheduled" in payload["blockedReasons"]
    assert "g10.routeCoverage missing restore-data finished" in payload[
        "blockedReasons"
    ]


def test_g10_example_webhook_url_is_blocked(tmp_path: Path) -> None:
    confirmation = _valid_confirmation()
    confirmation["g10"]["notify"]["webhook"]["url"] = (
        "https://ops.example.test/hooks/int-ruoyi"
    )
    path = tmp_path / "g10-example-webhook.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g10.notify.webhook.url must be a real http/https URL" in payload[
        "blockedReasons"
    ]


def test_placeholder_confirmation_tokens_are_blocked(tmp_path: Path) -> None:
    confirmation = _valid_confirmation()
    confirmation["g10"]["alertTarget"] = "pending production ops channel"
    confirmation["g10"]["sendEvidencePath"] = "未指定（BLOCKED）"
    confirmation["g11"]["dataOwner"]["approvalEvidence"] = "placeholder"
    path = tmp_path / "placeholder-tokens.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g10.alertTarget must not be a placeholder" in payload["blockedReasons"]
    assert "g10.sendEvidencePath must not be a placeholder" in payload[
        "blockedReasons"
    ]
    assert "g11.dataOwner.approvalEvidence must not be a placeholder" in payload[
        "blockedReasons"
    ]


def test_g11_missing_required_owner_fields_fail_closed(tmp_path: Path) -> None:
    confirmation = _valid_confirmation()
    del confirmation["g11"]["releaseOwner"]["contact"]
    confirmation["g11"]["alertOwner"]["currentDecision"] = "BLOCKED"
    del confirmation["g11"]["releaseGateReviewer"]["approvalEvidence"]
    path = tmp_path / "g11-missing-fields.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g11.releaseOwner.contact is required" in payload["blockedReasons"]
    assert "g11.alertOwner.currentDecision must be GO" in payload["blockedReasons"]
    assert "g11.releaseGateReviewer.approvalEvidence is required" in payload[
        "blockedReasons"
    ]


def test_g11_owner_candidates_are_not_approvals(tmp_path: Path) -> None:
    confirmation = _valid_confirmation()
    confirmation["g11"] = {
        "prodOwnerCandidates": [
            {"candidateName": "jiazeyu"},
            {"candidateName": "tangbin"},
        ]
    }
    path = tmp_path / "g11-candidates-only.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g11.prodOwnerCandidates are not approvals" in payload["blockedReasons"]
    assert "g11.releaseOwner.ownerName is required" in payload["blockedReasons"]
    assert "g11.releaseGateReviewer.ownerName is required" in payload["blockedReasons"]


def test_g11_owner_candidates_with_complete_role_approvals_can_go(tmp_path: Path) -> None:
    confirmation = _valid_confirmation()
    confirmation["g11"]["releaseOwner"]["ownerName"] = "jiazeyu"
    confirmation["g11"]["backupRecoveryOperator"]["ownerName"] = "tangbin"
    confirmation["g11"]["prodOwnerCandidates"] = [
        {"candidateName": "jiazeyu"},
        {"candidateName": "tangbin"},
    ]
    path = tmp_path / "g11-candidates-approved.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 0
    assert payload["decision"] == "GO"
    assert payload["blockedReasons"] == []


def test_g11_candidate_name_alone_is_not_approval_evidence(tmp_path: Path) -> None:
    confirmation = _valid_confirmation()
    confirmation["g11"]["releaseOwner"]["ownerName"] = "jiazeyu"
    confirmation["g11"]["releaseOwner"]["approvalEvidence"] = "jiazeyu"
    confirmation["g11"]["prodOwnerCandidates"] = [
        {"candidateName": "jiazeyu"},
        {"candidateName": "tangbin"},
    ]
    path = tmp_path / "g11-candidate-evidence.json"
    _write_json(path, confirmation)

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g11.releaseOwner.approvalEvidence must not be only a candidate name" in payload[
        "blockedReasons"
    ]


def test_complete_confirmation_contract_outputs_go(tmp_path: Path) -> None:
    path = tmp_path / "complete-g10-g11.json"
    _write_json(path, _valid_confirmation())

    result = _run_validator(path)
    payload = _stdout_json(result)

    assert result.returncode == 0
    assert payload["decision"] == "GO"
    assert payload["blockedReasons"] == []


def test_example_confirmation_stays_blocked_until_real_inputs_are_filled() -> None:
    result = _run_validator(EXAMPLE)
    payload = _stdout_json(result)

    assert result.returncode == 2
    assert payload["decision"] == "BLOCKED"
    assert "g10.notify.enabled must be true" in payload["blockedReasons"]
    assert "g11.releaseOwner.ownerName is required" in payload["blockedReasons"]
    assert "g11.prodOwnerCandidates are not approvals" in payload["blockedReasons"]
