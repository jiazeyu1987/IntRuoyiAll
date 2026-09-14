import json
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPT_PATH = REPO_ROOT / "script" / "p0" / "verify_p0_completion_gate.py"
TASK_ID = "20260803-p0-production-execution-loop-implementation"
MIGRATION_POLICY_RELATIVE_PATH = f"doc/tasks/{TASK_ID}/migration-policy-evidence.md"
REAL_E2E_RESULT_RELATIVE_PATH = "IntRuoyiFronted/test-results/p0-production-execution-loop-real/result.json"
TARGET_REQUEST_LABELS = [
    "FRONTLINE_SUBMIT_ENDPOINT",
    "PQC_SUBMIT_ENDPOINT",
    "TEAM_LEADER_REVIEW_ENDPOINT",
    "TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT",
    "PRODUCTION_EXECUTION_TRACE_ENDPOINT",
]
TARGET_REQUEST_ENDPOINTS = {
    "FRONTLINE_SUBMIT_ENDPOINT": "/mes/pro/feedback/frontline/submit",
    "PQC_SUBMIT_ENDPOINT": "/mes/pro/feedback/frontline/device-account/pqc/submit",
    "TEAM_LEADER_REVIEW_ENDPOINT": "/mes/pro/process-pool/team-leader/submission/review",
    "TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT": "/mes/pro/process-pool/team-leader/submission/allocation/confirm",
    "PRODUCTION_EXECUTION_TRACE_ENDPOINT": "/mes/pro/process-pool/team-leader/production-execution/trace",
}
TARGET_REQUEST_METHODS = {
    "FRONTLINE_SUBMIT_ENDPOINT": "POST",
    "PQC_SUBMIT_ENDPOINT": "POST",
    "TEAM_LEADER_REVIEW_ENDPOINT": "POST",
    "TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT": "POST",
    "PRODUCTION_EXECUTION_TRACE_ENDPOINT": "GET",
}
TARGET_RESPONSE_IDENTITIES = {
    "FRONTLINE_SUBMIT_ENDPOINT": ("processPoolEventId", 900001),
    "PQC_SUBMIT_ENDPOINT": ("pqcEventId", 910001),
    "TEAM_LEADER_REVIEW_ENDPOINT": ("reviewId", 920001),
    "TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT": ("reviewId", 930001),
    "PRODUCTION_EXECUTION_TRACE_ENDPOINT": ("processPoolEventId", 900001),
}
REQUIRED_CLOSURE_ANSWERS = [
    "who",
    "device",
    "process",
    "quantity",
    "quality",
    "signature",
    "workOrder",
    "review",
    "batchRecord",
]
EXPECTED_BROWSER_ROUTE_SKELETON = [
    "/login",
    "/mes/pro/process-pool/team-leader",
    "/mes/pro/feedback/edhr-batch-production-fill",
    "/mes/pro/feedback/edhr-batch-pqc-fill",
    "/mes/pro/process-pool/timeline",
]


def _run_gate(task_dir: Path, runtime_verifier: Path | None = None) -> subprocess.CompletedProcess[str]:
    command = [sys.executable, "-X", "utf8", str(SCRIPT_PATH), "--task-dir", str(task_dir)]
    if runtime_verifier is not None:
        command.extend(["--runtime-verifier", str(runtime_verifier)])
    return subprocess.run(
        command,
        cwd=REPO_ROOT,
        text=True,
        capture_output=True,
        encoding="utf-8",
        errors="replace",
    )


def _write(path: Path, body: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(body, encoding="utf-8")


def _write_runtime_verifier(path: Path, status: str = "PASS") -> None:
    _write(
        path,
        "\n".join(
            [
                "import json",
                "import sys",
                f"print(json.dumps({{'status': {status!r}, 'blockers': []}}))",
                f"sys.exit(0 if {status!r} == 'PASS' else 2)",
            ]
        )
        + "\n",
    )


def _write_real_e2e_result(
    root: Path,
    status: str = "PASS",
    generated_at: str = "2026-08-03T12:00:00.000Z",
    frontend_url: str = "http://127.0.0.1:8092",
    backend_url: str = "http://127.0.0.1:48092",
    tenant: str = "p0-test-tenant",
    username: str = "p0-e2e-user",
    run_id: str = "20260803-gate",
    data_prefix: str = "P0-EXEC-20260803-gate",
    device_account_id: int = 1001,
    batch_record_report_id: int = 7001,
    batch_record_definition_id: int = 7002,
    batch_record_version_id: int = 7003,
    schema_migration_id: str = "20260803_mes_process_pool_event_idempotency",
    migration_policy_evidence: str = MIGRATION_POLICY_RELATIVE_PATH,
    process_pool_event_id: int = 900001,
    closure_process_pool_event_id: int = 900001,
    closure_complete: bool = True,
    closure_issues: list[str] | None = None,
    closure_answer_overrides: dict[str, dict[str, object]] | None = None,
    closure_same_source_checks: list[dict[str, object]] | None = None,
    closure_blockers: list[dict[str, object]] | None = None,
    target_request_label_overrides: dict[str, str] | None = None,
    target_request_url_overrides: dict[str, str] | None = None,
    target_requests_override: list[dict[str, object]] | None = None,
    target_response_identity_overrides: dict[str, int] | None = None,
    browser_preflight_url: str = "http://127.0.0.1:8092/mes/pro/process-pool/team-leader",
    browser_preflight_route_steps: list[object] | None = None,
    browser_diagnostics: dict[str, list[dict[str, str]]] | None = None,
    idempotency_key_overrides: dict[str, str | None] | None = None,
    duplicate_flag_overrides: dict[str, bool | None] | None = None,
    runtime_migration_overrides: dict[str, object] | None = None,
) -> None:
    idempotency_key_overrides = idempotency_key_overrides or {}
    duplicate_flag_overrides = duplicate_flag_overrides or {}
    runtime_migration = {
        "status": "PASS",
        "runtime": {"host": "127.0.0.1", "database": "p0"},
        "blockers": [],
        "requiredColumns": [
            {"table": "mes_pro_process_pool_event", "column": "process_pool_event_id"},
        ],
        "requiredIndexes": [
            {"table": "mes_pro_process_pool_event", "index": "uk_p0_event_idempotency"},
        ],
        "historicalChecks": [
            {"check": "p0_history", "count": 0},
        ],
    }
    if runtime_migration_overrides:
        runtime_migration.update(runtime_migration_overrides)
    closure_answer_overrides = closure_answer_overrides or {}
    closure_answers = {}
    for answer_key in REQUIRED_CLOSURE_ANSWERS:
        closure_answers[answer_key] = {
            "sourceIds": {"source": f"{answer_key}-source"},
            "sameSource": True,
            "readOnlyVerificationEntries": [f"{answer_key}-verification"],
        }
        closure_answers[answer_key].update(closure_answer_overrides.get(answer_key, {}))
    if closure_same_source_checks is None:
        closure_same_source_checks = [
            {"checkKey": answer_key, "passed": True}
            for answer_key in REQUIRED_CLOSURE_ANSWERS
        ]
    if closure_blockers is None:
        closure_blockers = []
    target_requests = [
        {
            "label": (target_request_label_overrides or {}).get(label, label),
            "url": (target_request_url_overrides or {}).get(
                label,
                f"{backend_url}{TARGET_REQUEST_ENDPOINTS[label]}",
            ),
            "method": TARGET_REQUEST_METHODS[label],
            "httpStatus": 200,
            "businessCode": 0,
        }
        for label in TARGET_REQUEST_LABELS
    ]
    if target_requests_override is not None:
        target_requests = target_requests_override
    target_response_identities = {
        label: {
            "field": TARGET_RESPONSE_IDENTITIES[label][0],
            "value": (target_response_identity_overrides or {}).get(label, TARGET_RESPONSE_IDENTITIES[label][1]),
            "sourceRequestLabel": label,
        }
        for label in TARGET_REQUEST_LABELS
    }
    if browser_preflight_route_steps is None:
        browser_preflight_route_steps = [
            {"route": route, "status": "visible"}
            for route in EXPECTED_BROWSER_ROUTE_SKELETON
        ]
    _write(
        root / REAL_E2E_RESULT_RELATIVE_PATH,
        json.dumps(
            {
                "status": status,
                "generatedAt": generated_at,
                "frontendUrl": frontend_url,
                "backendUrl": backend_url,
                "tenant": tenant,
                "username": username,
                "runId": run_id,
                "dataPrefix": data_prefix,
                "deviceAccountId": device_account_id,
                "batchRecordReportId": batch_record_report_id,
                "batchRecordDefinitionId": batch_record_definition_id,
                "batchRecordVersionId": batch_record_version_id,
                "schemaMigrationId": schema_migration_id,
                "migrationPolicyEvidence": migration_policy_evidence,
                "submitIdempotencyKey": idempotency_key_overrides.get(
                    "submitIdempotencyKey",
                    "submit-key",
                ),
                "pqcIdempotencyKey": idempotency_key_overrides.get(
                    "pqcIdempotencyKey",
                    "pqc-key",
                ),
                "confirmIdempotencyKey": idempotency_key_overrides.get(
                    "confirmIdempotencyKey",
                    "confirm-key",
                ),
                "processPoolEventId": process_pool_event_id,
                "duplicateProductionSubmitVerified": duplicate_flag_overrides.get(
                    "duplicateProductionSubmitVerified",
                    True,
                ),
                "duplicatePqcSubmitVerified": duplicate_flag_overrides.get(
                    "duplicatePqcSubmitVerified",
                    True,
                ),
                "duplicateConfirmRejected": duplicate_flag_overrides.get(
                    "duplicateConfirmRejected",
                    True,
                ),
                "closureEvidence": {
                    "processPoolEventId": closure_process_pool_event_id,
                    "complete": closure_complete,
                    "answers": closure_answers,
                    "sameSourceChecks": closure_same_source_checks,
                    "blockers": closure_blockers,
                },
                "closureEvidenceIssues": closure_issues or [],
                "targetRequests": target_requests,
                "targetRequestEvidenceFlushed": True,
                "targetResponseIdentities": target_response_identities,
                "browserPreflight": {
                    "currentUrl": browser_preflight_url,
                    "routeSteps": browser_preflight_route_steps,
                },
                "browserDiagnostics": browser_diagnostics
                or {
                    "pageErrors": [],
                    "consoleErrors": [],
                    "targetRequestFailures": [],
                },
                "runtimeMigration": runtime_migration,
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
    )


def _completion_pass_evidence(
    include_completion_metadata: bool = True,
    include_target_requests: bool = True,
    include_browser_diagnostics: bool = True,
    include_generated_at: bool = True,
    generated_at: str | None = None,
    frontend_url: str = "http://127.0.0.1:8092",
    backend_url: str = "http://127.0.0.1:48092",
    browser_preflight_url: str = "http://127.0.0.1:8092/mes/pro/process-pool/team-leader",
    target_request_url_overrides: dict[str, str] | None = None,
    target_request_method_overrides: dict[str, str] | None = None,
    target_request_status_overrides: dict[str, int] | None = None,
    target_request_business_code_overrides: dict[str, int] | None = None,
    target_response_identity_overrides: dict[str, int | str] | None = None,
    closure_process_pool_event_id: int = 900001,
    closure_complete: bool = True,
    closure_same_source_checks: int = 9,
    closure_blockers: int = 0,
    closure_issue_lines: list[str] | None = None,
) -> str:
    answers = "\n".join(
        f"- answers.{key}: sourceIds=`1`, sameSource=`true`, readOnlyVerificationEntries=`1`"
        for key in [
            "who",
            "device",
            "process",
            "quantity",
            "quality",
            "signature",
            "workOrder",
            "review",
            "batchRecord",
        ]
    )
    metadata = ""
    if include_completion_metadata:
        metadata = f"""- Frontend: `{frontend_url}`
- Backend: `{backend_url}`
- Tenant: `p0-test-tenant`
- User: `p0-e2e-user`
- Run ID: `20260803-gate`
- Data Prefix: `P0-EXEC-20260803-gate`
- Device Account ID: `1001`
- Batch Record Binding: report=`7001`, definition=`7002`, version=`7003`
- Schema Migration ID: `20260803_mes_process_pool_event_idempotency`
- Migration Policy Evidence: `{MIGRATION_POLICY_RELATIVE_PATH}`
- Submit Idempotency Key Configured: `true`
- PQC Idempotency Key Configured: `true`
- Confirm Idempotency Key Configured: `true`
"""
    target_request_lines = ""
    if include_target_requests:
        lines = []
        for label in TARGET_REQUEST_LABELS:
            url = (target_request_url_overrides or {}).get(label, f"{backend_url}{TARGET_REQUEST_ENDPOINTS[label]}")
            method = (target_request_method_overrides or {}).get(label, TARGET_REQUEST_METHODS[label])
            http_status = (target_request_status_overrides or {}).get(label, 200)
            business_code = (target_request_business_code_overrides or {}).get(label, 0)
            identity_field, identity_value = TARGET_RESPONSE_IDENTITIES[label]
            identity_value = (target_response_identity_overrides or {}).get(label, identity_value)
            lines.append(f"- Target Request {label} Hit: `true`")
            lines.append(f"- Target Request {label} URL: `{url}`")
            lines.append(f"- Target Request {label} Method: `{method}`")
            lines.append(f"- Target Request {label} HTTP Status: `{http_status}`")
            lines.append(f"- Target Request {label} Business Code: `{business_code}`")
            lines.append(f"- Target Response {label} {identity_field}: `{identity_value}`")
        target_request_lines = "\n".join(lines)
    browser_diagnostic_lines = ""
    if include_browser_diagnostics:
        browser_diagnostic_lines = "\n".join(
            [
                "- Browser Page Errors: `0`",
                "- Browser Console Errors: `0`",
                "- Target Request Failures: `0`",
            ]
        )
    if generated_at is None:
        generated_at = datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")
    generated_at_line = f"- Generated At: `{generated_at}`" if include_generated_at else ""
    closure_issues = "\n".join(f"- Closure Issue: {line}" for line in closure_issue_lines or [])
    return f"""# P0 生产执行主闭环真实 E2E 证据

- Status: `PASS`
{generated_at_line}
{metadata.rstrip()}
- processPoolEventId: `900001`
- Duplicate Production Submit Verified: `true`
- Duplicate PQC Submit Verified: `true`
- Duplicate FIFO Confirm Rejected: `true`
- Browser Preflight: `{browser_preflight_url}`
- Route Preflight Steps: `5`
{target_request_lines}
{browser_diagnostic_lines}

## Closure Evidence Packet

- processPoolEventId: `{closure_process_pool_event_id}`
- complete: `{'true' if closure_complete else 'false'}`
- sameSourceChecks: `{closure_same_source_checks}`
- blockers: `{closure_blockers}`
{answers}
{closure_issues}

## Runtime Migration

- Status: `PASS`
- Runtime: `{{"host":"127.0.0.1","database":"p0"}}`
- Required Columns: `1`
- Required Indexes: `1`
- Historical Checks: `1`
- Required Proof: 浏览器写入前必须运行 `verify_p0_runtime_migration.py` 并返回 `PASS`。
"""


def test_p0_completion_gate_blocks_current_unfinished_evidence():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"

    result = _run_gate(REPO_ROOT.parent / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation")
    payload = json.loads(result.stdout)
    codes = {blocker["code"] for blocker in payload["blockers"]}

    assert result.returncode == 2
    assert payload["status"] == "BLOCKED"
    assert "P0_COMPLETION_REAL_E2E_NOT_PASS" in codes
    assert "P0_TDD_EVIDENCE_GAP" not in codes
    assert "P0_COMPLETION_RUNTIME_MIGRATION_NOT_PASS" in codes
    assert payload["tddEvidence"]["status"] == "PASS"
    assert payload["tddEvidence"]["m2OriginalRedFound"] is True


def test_p0_completion_gate_passes_only_when_all_required_evidence_passes():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- BDD: 复核必须要求电子签名 -> Given 班组长没有签名 When 复核或确认分配 Then 必须拒绝。",
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL，`Tests run: 8, Failures: 2`；无签名仍可复核或确认分配，证明班组长复核尚未要求电子签名。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")
        _write(task_dir / "p0-real-e2e-evidence.md", _completion_pass_evidence(generated_at=generated_at))
        _write_real_e2e_result(root, generated_at=generated_at)

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)

        assert result.returncode == 0
        assert payload["status"] == "PASS"
        assert payload["blockers"] == []
        assert payload["realE2e"]["status"] == "PASS"
        assert payload["runtimeMigration"]["status"] == "PASS"
        assert payload["tddEvidence"]["status"] == "PASS"


def test_p0_completion_gate_requires_allowed_runtime_url_pair():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(
                frontend_url="http://127.0.0.1:8092",
                backend_url="http://127.0.0.1:48081",
            ),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_RUNTIME_URL_PAIR_INVALID" in codes


def test_p0_completion_gate_requires_migration_policy_pass_evidence():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate BLOCKED\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(task_dir / "p0-real-e2e-evidence.md", _completion_pass_evidence())

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_MIGRATION_POLICY_EVIDENCE_NOT_PASS" in codes


def test_p0_completion_gate_requires_browser_preflight_under_frontend_url():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(
                frontend_url="http://127.0.0.1:8092",
                backend_url="http://127.0.0.1:48092",
                browser_preflight_url="http://127.0.0.1:8081/mes/pro/process-pool/team-leader",
            ),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_BROWSER_PREFLIGHT_URL_MISMATCH" in codes


def test_p0_completion_gate_requires_target_requests_under_backend_url():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(
                target_request_url_overrides={
                    "FRONTLINE_SUBMIT_ENDPOINT": "http://127.0.0.1:48081/mes/pro/feedback/frontline/submit",
                },
            ),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_TARGET_REQUEST_URL_MISMATCH" in codes


def test_p0_completion_gate_requires_target_request_http_methods():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(
                target_request_method_overrides={
                    "FRONTLINE_SUBMIT_ENDPOINT": "GET",
                },
            ),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_TARGET_REQUEST_METHOD_MISMATCH" in codes


def test_p0_completion_gate_requires_target_request_http_status_success():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(
                target_request_status_overrides={
                    "PQC_SUBMIT_ENDPOINT": 500,
                },
            ),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_TARGET_REQUEST_HTTP_STATUS_NOT_OK" in codes


def test_p0_completion_gate_requires_target_request_business_code_success():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(
                target_request_business_code_overrides={
                    "TEAM_LEADER_REVIEW_ENDPOINT": 500,
                },
            ),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_TARGET_REQUEST_BUSINESS_CODE_NOT_OK" in codes


def test_p0_completion_gate_requires_target_response_identity_matches_fresh_root():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(
                target_response_identity_overrides={
                    "FRONTLINE_SUBMIT_ENDPOINT": 123,
                },
            ),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_TARGET_RESPONSE_PROCESS_POOL_EVENT_MISMATCH" in codes


def test_p0_completion_gate_requires_closure_packet_complete_and_same_root():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(
                closure_process_pool_event_id=123,
                closure_complete=False,
                closure_same_source_checks=0,
                closure_blockers=1,
            ),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_CLOSURE_PROCESS_POOL_EVENT_MISMATCH" in codes
        assert "P0_COMPLETION_CLOSURE_NOT_COMPLETE" in codes
        assert "P0_COMPLETION_CLOSURE_SAME_SOURCE_CHECKS_INCOMPLETE" in codes
        assert "P0_COMPLETION_CLOSURE_BLOCKERS_PRESENT" in codes


def test_p0_completion_gate_rejects_closure_issue_lines_even_when_summary_counts_pass():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(
                closure_issue_lines=[
                    "CLOSURE_EVIDENCE_SAME_SOURCE_FAILED: sameSourceChecks.quality 未通过。"
                ],
            ),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_CLOSURE_ISSUE_PRESENT" in codes


def test_p0_completion_gate_requires_real_e2e_result_json_for_pass():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(task_dir / "p0-real-e2e-evidence.md", _completion_pass_evidence())

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_generated_at_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at="2026-08-03T12:00:00.000Z"),
        )
        _write_real_e2e_result(root, generated_at="2026-08-03T12:05:00.000Z")

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_GENERATED_AT_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_runtime_urls_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            frontend_url="http://127.0.0.1:8081",
            backend_url="http://127.0.0.1:48081",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_FRONTEND_URL_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_BACKEND_URL_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_browser_preflight_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            browser_preflight_url="http://127.0.0.1:8081/mes/pro/process-pool/team-leader",
            browser_preflight_route_steps=["login", "team-leader-workbench"],
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_BROWSER_PREFLIGHT_URL_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_BROWSER_PREFLIGHT_ROUTE_STEPS_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_browser_preflight_route_skeleton():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            browser_preflight_route_steps=[
                {"route": "/login", "status": "authenticated"},
                {"route": "/mes/pro/process-pool/team-leader", "status": "visible"},
                {"route": "/mes/pro/process-pool/team-leader", "status": "visible"},
                {"route": "/mes/pro/process-pool/not-pqc", "status": "visible"},
                {"route": "/mes/pro/process-pool/not-trace", "status": "visible"},
            ],
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_BROWSER_PREFLIGHT_ROUTE_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_run_metadata_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            tenant="p0-old-tenant",
            username="p0-old-user",
            run_id="old-run",
            data_prefix="P0-EXEC-old-run",
            device_account_id=2002,
            batch_record_report_id=8001,
            batch_record_definition_id=8002,
            batch_record_version_id=8003,
            schema_migration_id="old_schema_migration",
            migration_policy_evidence="doc/tasks/20260803-p0-production-execution-loop-implementation/old-policy.md",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TENANT_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_USER_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_RUN_ID_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_DATA_PREFIX_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_DEVICE_ACCOUNT_ID_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_BATCH_RECORD_BINDING_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_SCHEMA_MIGRATION_ID_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_MIGRATION_POLICY_EVIDENCE_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_requests_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_request_url_overrides={
                "FRONTLINE_SUBMIT_ENDPOINT": "http://127.0.0.1:48081/mes/pro/feedback/frontline/submit",
            },
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_URL_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_request_business_code_present():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        target_requests = [
            {
                "label": label,
                "url": f"http://127.0.0.1:48092{TARGET_REQUEST_ENDPOINTS[label]}",
                "method": TARGET_REQUEST_METHODS[label],
                "httpStatus": 200,
                "businessCode": 0,
            }
            for label in TARGET_REQUEST_LABELS
        ]
        target_requests[0].pop("businessCode")
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_requests_override=target_requests,
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_BUSINESS_CODE_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_request_url_present():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        target_requests = [
            {
                "label": label,
                "url": f"http://127.0.0.1:48092{TARGET_REQUEST_ENDPOINTS[label]}",
                "method": TARGET_REQUEST_METHODS[label],
                "httpStatus": 200,
                "businessCode": 0,
            }
            for label in TARGET_REQUEST_LABELS
        ]
        target_requests[0].pop("url")
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_requests_override=target_requests,
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_URL_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_request_label_present():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        target_requests = [
            {
                "label": label,
                "url": f"http://127.0.0.1:48092{TARGET_REQUEST_ENDPOINTS[label]}",
                "method": TARGET_REQUEST_METHODS[label],
                "httpStatus": 200,
                "businessCode": 0,
            }
            for label in TARGET_REQUEST_LABELS
        ]
        target_requests[0].pop("label")
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_requests_override=target_requests,
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_LABEL_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_request_items_to_be_objects():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        target_requests = [
            {
                "label": label,
                "url": f"http://127.0.0.1:48092{TARGET_REQUEST_ENDPOINTS[label]}",
                "method": TARGET_REQUEST_METHODS[label],
                "httpStatus": 200,
                "businessCode": 0,
            }
            for label in TARGET_REQUEST_LABELS
        ]
        target_requests[0] = "FRONTLINE_SUBMIT_ENDPOINT"
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_requests_override=target_requests,
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_OBJECT_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_request_http_status_present():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        target_requests = [
            {
                "label": label,
                "url": f"http://127.0.0.1:48092{TARGET_REQUEST_ENDPOINTS[label]}",
                "method": TARGET_REQUEST_METHODS[label],
                "httpStatus": 200,
                "businessCode": 0,
            }
            for label in TARGET_REQUEST_LABELS
        ]
        target_requests[0].pop("httpStatus")
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_requests_override=target_requests,
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_HTTP_STATUS_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_request_method_present():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        target_requests = [
            {
                "label": label,
                "url": f"http://127.0.0.1:48092{TARGET_REQUEST_ENDPOINTS[label]}",
                "method": TARGET_REQUEST_METHODS[label],
                "httpStatus": 200,
                "businessCode": 0,
            }
            for label in TARGET_REQUEST_LABELS
        ]
        target_requests[0].pop("method")
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_requests_override=target_requests,
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_METHOD_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_request_labels_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_request_label_overrides={
                "FRONTLINE_SUBMIT_ENDPOINT": "PQC_SUBMIT_ENDPOINT",
            },
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_LABEL_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_requests_to_be_unique():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        target_requests = [
            {
                "label": label,
                "url": f"http://127.0.0.1:48092{TARGET_REQUEST_ENDPOINTS[label]}",
                "method": TARGET_REQUEST_METHODS[label],
                "httpStatus": 200,
                "businessCode": 0,
            }
            for label in TARGET_REQUEST_LABELS
        ]
        target_requests.append(dict(target_requests[0]))
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_requests_override=target_requests,
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_DUPLICATE" in codes


def test_p0_completion_gate_rejects_unexpected_real_e2e_result_target_requests():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        target_requests = [
            {
                "label": label,
                "url": f"http://127.0.0.1:48092{TARGET_REQUEST_ENDPOINTS[label]}",
                "method": TARGET_REQUEST_METHODS[label],
                "httpStatus": 200,
                "businessCode": 0,
            }
            for label in TARGET_REQUEST_LABELS
        ]
        target_requests.append(
            {
                "label": "UNEXPECTED_BACKGROUND_ENDPOINT",
                "url": "http://127.0.0.1:48092/mes/pro/process-pool/team-leader/background-refresh",
                "method": "GET",
                "httpStatus": 200,
                "businessCode": 0,
            }
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_requests_override=target_requests,
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_UNEXPECTED" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_request_count_exactly_five():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        target_requests = [
            {
                "label": label,
                "url": f"http://127.0.0.1:48092{TARGET_REQUEST_ENDPOINTS[label]}",
                "method": TARGET_REQUEST_METHODS[label],
                "httpStatus": 200,
                "businessCode": 0,
            }
            for label in TARGET_REQUEST_LABELS[:-1]
        ]
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_requests_override=target_requests,
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_COUNT_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_response_identities_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            target_response_identity_overrides={
                "PRODUCTION_EXECUTION_TRACE_ENDPOINT": 123,
            },
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_VALUE_MISMATCH" in codes


def test_p0_completion_gate_rejects_unexpected_real_e2e_result_target_response_identity():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(root, generated_at=generated_at)
        result_path = root / REAL_E2E_RESULT_RELATIVE_PATH
        result_data = json.loads(result_path.read_text(encoding="utf-8"))
        result_data["targetResponseIdentities"]["UNEXPECTED_BACKGROUND_ENDPOINT"] = {
            "field": "processPoolEventId",
            "value": 900001,
        }
        _write(
            result_path,
            json.dumps(result_data, ensure_ascii=False, indent=2) + "\n",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_UNEXPECTED" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_response_identity_count_exactly_five():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(root, generated_at=generated_at)
        result_path = root / REAL_E2E_RESULT_RELATIVE_PATH
        result_data = json.loads(result_path.read_text(encoding="utf-8"))
        result_data["targetResponseIdentities"].pop("PQC_SUBMIT_ENDPOINT")
        _write(
            result_path,
            json.dumps(result_data, ensure_ascii=False, indent=2) + "\n",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_COUNT_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_response_identity_items_to_be_objects():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(root, generated_at=generated_at)
        result_path = root / REAL_E2E_RESULT_RELATIVE_PATH
        result_data = json.loads(result_path.read_text(encoding="utf-8"))
        result_data["targetResponseIdentities"]["PQC_SUBMIT_ENDPOINT"] = "PQC_SUBMIT_ENDPOINT"
        _write(
            result_path,
            json.dumps(result_data, ensure_ascii=False, indent=2) + "\n",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_OBJECT_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_response_identity_field_present():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(root, generated_at=generated_at)
        result_path = root / REAL_E2E_RESULT_RELATIVE_PATH
        result_data = json.loads(result_path.read_text(encoding="utf-8"))
        result_data["targetResponseIdentities"]["PQC_SUBMIT_ENDPOINT"].pop("field")
        _write(
            result_path,
            json.dumps(result_data, ensure_ascii=False, indent=2) + "\n",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_FIELD_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_response_identity_value_present():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(root, generated_at=generated_at)
        result_path = root / REAL_E2E_RESULT_RELATIVE_PATH
        result_data = json.loads(result_path.read_text(encoding="utf-8"))
        result_data["targetResponseIdentities"]["PQC_SUBMIT_ENDPOINT"].pop("value")
        _write(
            result_path,
            json.dumps(result_data, ensure_ascii=False, indent=2) + "\n",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_VALUE_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_response_identity_source_request_label_present():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(root, generated_at=generated_at)
        result_path = root / REAL_E2E_RESULT_RELATIVE_PATH
        result_data = json.loads(result_path.read_text(encoding="utf-8"))
        result_data["targetResponseIdentities"]["PQC_SUBMIT_ENDPOINT"].pop("sourceRequestLabel")
        _write(
            result_path,
            json.dumps(result_data, ensure_ascii=False, indent=2) + "\n",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_SOURCE_REQUEST_LABEL_MISSING" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_response_identity_source_request_label():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(root, generated_at=generated_at)
        result_path = root / REAL_E2E_RESULT_RELATIVE_PATH
        result_data = json.loads(result_path.read_text(encoding="utf-8"))
        result_data["targetResponseIdentities"]["PQC_SUBMIT_ENDPOINT"]["sourceRequestLabel"] = "FRONTLINE_SUBMIT_ENDPOINT"
        _write(
            result_path,
            json.dumps(result_data, ensure_ascii=False, indent=2) + "\n",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_REQUEST_LABEL_MISMATCH" in codes


def test_p0_completion_gate_requires_target_response_identity_sources_to_match_observed_target_requests():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(root, generated_at=generated_at)
        result_path = root / REAL_E2E_RESULT_RELATIVE_PATH
        result_data = json.loads(result_path.read_text(encoding="utf-8"))
        for item in result_data["targetRequests"]:
            if item["label"] == "PQC_SUBMIT_ENDPOINT":
                item["label"] = "PQC_SUBMIT_ENDPOINT_DIAGNOSTIC_COPY"
                break
        _write(
            result_path,
            json.dumps(result_data, ensure_ascii=False, indent=2) + "\n",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_REQUEST_SET_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_target_request_evidence_flushed():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(root, generated_at=generated_at)
        result_path = root / REAL_E2E_RESULT_RELATIVE_PATH
        result_data = json.loads(result_path.read_text(encoding="utf-8"))
        result_data["targetRequestEvidenceFlushed"] = False
        _write(
            result_path,
            json.dumps(result_data, ensure_ascii=False, indent=2) + "\n",
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_EVIDENCE_NOT_FLUSHED" in codes


def test_p0_completion_gate_requires_real_e2e_result_browser_diagnostics_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            browser_diagnostics={
                "pageErrors": [],
                "consoleErrors": [{"text": "unexpected console error"}],
                "targetRequestFailures": [],
            },
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_BROWSER_DIAGNOSTICS_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_idempotency_and_duplicate_evidence_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            idempotency_key_overrides={
                "submitIdempotencyKey": None,
            },
            duplicate_flag_overrides={
                "duplicatePqcSubmitVerified": False,
            },
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_IDEMPOTENCY_EVIDENCE_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_DUPLICATE_EVIDENCE_MISMATCH" in codes


def test_p0_completion_gate_requires_real_e2e_result_runtime_migration_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            runtime_migration_overrides={
                "status": "BLOCKED",
                "blockers": ["missing runtime migration"],
                "requiredColumns": [],
            },
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_RUNTIME_MIGRATION_STATUS_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_RUNTIME_MIGRATION_COLUMNS_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_RUNTIME_MIGRATION_BLOCKERS_PRESENT" in codes


def test_p0_completion_gate_requires_real_e2e_result_closure_answers_to_match_markdown():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            closure_answer_overrides={
                "who": {
                    "sourceIds": {},
                    "sameSource": False,
                    "readOnlyVerificationEntries": [],
                },
            },
            closure_same_source_checks=[
                {"checkKey": "who", "passed": True},
            ],
            closure_blockers=[
                {"code": "CLOSURE_EVIDENCE_MISSING_SOURCE"},
            ],
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_ANSWER_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_SAME_SOURCE_CHECKS_MISMATCH" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_BLOCKERS_MISMATCH" in codes


def test_p0_completion_gate_rejects_real_e2e_result_failed_closure_checks_and_answer_blockers():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        generated_at = "2026-08-03T12:00:00.000Z"
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(generated_at=generated_at),
        )
        _write_real_e2e_result(
            root,
            generated_at=generated_at,
            closure_answer_overrides={
                "who": {
                    "blockers": [
                        {"code": "CLOSURE_EVIDENCE_MISSING_SOURCE"},
                    ],
                },
            },
            closure_same_source_checks=[
                {
                    "checkKey": answer_key,
                    "passed": answer_key != "who",
                }
                for answer_key in REQUIRED_CLOSURE_ANSWERS
            ],
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_SAME_SOURCE_CHECK_FAILED" in codes
        assert "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_ANSWER_BLOCKERS_PRESENT" in codes


def test_p0_completion_gate_requires_current_status_token_not_body_mentions():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(
            task_dir / "task.md",
            "\n".join(
                [
                    "## Current Status",
                    "",
                    "in_progress - 仍不能标记 completed，也不能进入 ready_for_closeout。",
                    "",
                ]
            ),
        )
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(task_dir / "p0-real-e2e-evidence.md", _completion_pass_evidence())

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_TASK_STATUS_NOT_READY" in codes


def test_p0_completion_gate_requires_run_identity_and_formal_binding_evidence():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(task_dir / "p0-real-e2e-evidence.md", _completion_pass_evidence(False))

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_RUN_ID_MISSING" in codes
        assert "P0_COMPLETION_BATCH_RECORD_BINDING_MISSING" in codes
        assert "P0_COMPLETION_MIGRATION_POLICY_EVIDENCE_MISSING" in codes


def test_p0_completion_gate_requires_all_target_request_hits():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(task_dir / "p0-real-e2e-evidence.md", _completion_pass_evidence(include_target_requests=False))

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_TARGET_REQUEST_NOT_PROVEN" in codes


def test_p0_completion_gate_requires_clean_browser_diagnostics():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(include_browser_diagnostics=False),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_BROWSER_DIAGNOSTICS_MISSING" in codes


def test_p0_completion_gate_requires_generated_at_timestamp():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        _write(
            task_dir / "p0-real-e2e-evidence.md",
            _completion_pass_evidence(include_generated_at=False),
        )

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_GENERATED_AT_MISSING" in codes


def test_p0_completion_gate_rejects_stale_generated_at_timestamp():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        stale_evidence = _completion_pass_evidence(generated_at="2020-01-01T00:00:00.000Z")
        _write(task_dir / "p0-real-e2e-evidence.md", stale_evidence)

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_GENERATED_AT_STALE" in codes


def test_p0_completion_gate_rejects_future_generated_at_timestamp():
    assert SCRIPT_PATH.exists(), "P0 completion gate script is required"
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        task_dir = root / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
        runtime_verifier = root / "verify_runtime.py"
        _write_runtime_verifier(runtime_verifier, "PASS")
        _write(root / MIGRATION_POLICY_RELATIVE_PATH, "release migration policy gate PASS\n")
        _write(task_dir / "task.md", "## Current Status\n\nready_for_closeout\n")
        _write(
            task_dir / "execution-log.md",
            "\n".join(
                [
                    "- RED: `MesP0TeamLeaderReviewSignatureServiceTest` -> FAIL；无签名仍可复核或确认分配。",
                    "- GREEN: `MesP0TeamLeaderReviewSignatureServiceTest` -> PASS。",
                ]
            ),
        )
        future_evidence = _completion_pass_evidence(generated_at="2999-01-01T00:00:00.000Z")
        _write(task_dir / "p0-real-e2e-evidence.md", future_evidence)

        result = _run_gate(task_dir, runtime_verifier)
        payload = json.loads(result.stdout)
        codes = {blocker["code"] for blocker in payload["blockers"]}

        assert result.returncode == 2
        assert payload["status"] == "BLOCKED"
        assert "P0_COMPLETION_GENERATED_AT_FUTURE" in codes


if __name__ == "__main__":
    test_p0_completion_gate_blocks_current_unfinished_evidence()
    test_p0_completion_gate_passes_only_when_all_required_evidence_passes()
    test_p0_completion_gate_requires_allowed_runtime_url_pair()
    test_p0_completion_gate_requires_migration_policy_pass_evidence()
    test_p0_completion_gate_requires_browser_preflight_under_frontend_url()
    test_p0_completion_gate_requires_target_requests_under_backend_url()
    test_p0_completion_gate_requires_target_request_http_methods()
    test_p0_completion_gate_requires_target_request_http_status_success()
    test_p0_completion_gate_requires_target_request_business_code_success()
    test_p0_completion_gate_requires_target_response_identity_matches_fresh_root()
    test_p0_completion_gate_requires_closure_packet_complete_and_same_root()
    test_p0_completion_gate_rejects_closure_issue_lines_even_when_summary_counts_pass()
    test_p0_completion_gate_requires_real_e2e_result_json_for_pass()
    test_p0_completion_gate_requires_real_e2e_result_generated_at_to_match_markdown()
    test_p0_completion_gate_requires_real_e2e_result_runtime_urls_to_match_markdown()
    test_p0_completion_gate_requires_real_e2e_result_browser_preflight_to_match_markdown()
    test_p0_completion_gate_requires_real_e2e_result_browser_preflight_route_skeleton()
    test_p0_completion_gate_requires_real_e2e_result_run_metadata_to_match_markdown()
    test_p0_completion_gate_requires_real_e2e_result_target_requests_to_match_markdown()
    test_p0_completion_gate_requires_real_e2e_result_target_request_business_code_present()
    test_p0_completion_gate_requires_real_e2e_result_target_request_url_present()
    test_p0_completion_gate_requires_real_e2e_result_target_request_label_present()
    test_p0_completion_gate_requires_real_e2e_result_target_request_items_to_be_objects()
    test_p0_completion_gate_requires_real_e2e_result_target_request_http_status_present()
    test_p0_completion_gate_requires_real_e2e_result_target_request_method_present()
    test_p0_completion_gate_requires_real_e2e_result_target_request_labels_to_match_markdown()
    test_p0_completion_gate_requires_real_e2e_result_target_requests_to_be_unique()
    test_p0_completion_gate_rejects_unexpected_real_e2e_result_target_requests()
    test_p0_completion_gate_requires_real_e2e_result_target_request_count_exactly_five()
    test_p0_completion_gate_requires_real_e2e_result_target_response_identities_to_match_markdown()
    test_p0_completion_gate_rejects_unexpected_real_e2e_result_target_response_identity()
    test_p0_completion_gate_requires_real_e2e_result_target_response_identity_count_exactly_five()
    test_p0_completion_gate_requires_real_e2e_result_target_response_identity_items_to_be_objects()
    test_p0_completion_gate_requires_real_e2e_result_target_response_identity_field_present()
    test_p0_completion_gate_requires_real_e2e_result_target_response_identity_value_present()
    test_p0_completion_gate_requires_real_e2e_result_target_response_identity_source_request_label_present()
    test_p0_completion_gate_requires_real_e2e_result_target_response_identity_source_request_label()
    test_p0_completion_gate_requires_target_response_identity_sources_to_match_observed_target_requests()
    test_p0_completion_gate_requires_real_e2e_result_target_request_evidence_flushed()
    test_p0_completion_gate_requires_real_e2e_result_browser_diagnostics_to_match_markdown()
    test_p0_completion_gate_requires_real_e2e_result_idempotency_and_duplicate_evidence_to_match_markdown()
    test_p0_completion_gate_requires_real_e2e_result_runtime_migration_to_match_markdown()
    test_p0_completion_gate_requires_real_e2e_result_closure_answers_to_match_markdown()
    test_p0_completion_gate_rejects_real_e2e_result_failed_closure_checks_and_answer_blockers()
    test_p0_completion_gate_requires_current_status_token_not_body_mentions()
    test_p0_completion_gate_requires_run_identity_and_formal_binding_evidence()
    test_p0_completion_gate_requires_all_target_request_hits()
    test_p0_completion_gate_requires_clean_browser_diagnostics()
    test_p0_completion_gate_requires_generated_at_timestamp()
    test_p0_completion_gate_rejects_stale_generated_at_timestamp()
    test_p0_completion_gate_rejects_future_generated_at_timestamp()
    print("PASS: P0 completion gate contract")
