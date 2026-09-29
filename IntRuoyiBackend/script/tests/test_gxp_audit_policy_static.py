import json
import hashlib
import importlib.util
import re
import subprocess
import sys
import tempfile
from pathlib import Path

import jsonschema
import pytest
import yaml


WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
RUNTIME_SOURCE_ROOT = WORKSPACE_ROOT / "IntRuoyiBackend"
POLICY_PATH = RUNTIME_SOURCE_ROOT / "config" / "gxp-audit-policy.yaml"
SCHEMA_PATH = RUNTIME_SOURCE_ROOT / "config" / "gxp-audit-policy.schema.json"


REQUIRED_OPERATION_FIELDS = {
    "operationId",
    "sourceType",
    "sourceLocators",
    "domain",
    "subjectType",
    "actionType",
    "reasonPolicy",
    "signaturePolicy",
    "statePolicy",
    "retentionClass",
    "testIds",
    "ownerRole",
    "applicability",
}


WRITE_BOUNDARY_PATTERNS = {
    "CONTROLLER": re.compile(r"@(PostMapping|PutMapping|DeleteMapping|PatchMapping)\b"),
    "DOMAIN_SERVICE": re.compile(r"\b(create|update|delete|submit|approve|publish|void|assign)[A-Z]\w*\s*\("),
    "JOB": re.compile(r"(@Scheduled\b|implements\s+Job\b|extends\s+QuartzJobBean\b)"),
    "MESSAGE_CONSUMER": re.compile(r"(@RabbitListener\b|@KafkaListener\b|RocketMQListener\b)"),
    "IMPORT": re.compile(r"\b(import|upload|parse|sync)[A-Z]\w*\s*\("),
    "EXTERNAL_SYNC": re.compile(r"\b(sync|push|pull)[A-Z]\w*\s*\("),
    "MIGRATION": re.compile(r"\b(INSERT|UPDATE|DELETE|ALTER|CREATE|DROP)\b", re.IGNORECASE),
    "OPS_SCRIPT": re.compile(r"\b(insert|update|delete|alter|create|drop|deploy|release|migration)\b", re.IGNORECASE),
}

FIRST_HIGH_RISK_OPERATION_IDS = {
    "mes.active-order.add",
    "mes.production.submit",
    "mes.pqc.submit",
    "mes.active-order.complete",
    "mes.pqc.production-release.approve",
    "mes.market-release.approve",
}


def _load_policy() -> dict[str, object]:
    assert POLICY_PATH.exists(), "config/gxp-audit-policy.yaml must exist"
    payload = yaml.safe_load(POLICY_PATH.read_text(encoding="utf-8"))
    assert isinstance(payload, dict), "GxP audit policy must be a YAML object"
    return payload


def _load_schema() -> dict[str, object]:
    assert SCHEMA_PATH.exists(), "config/gxp-audit-policy.schema.json must exist"
    payload = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    assert isinstance(payload, dict), "GxP audit policy schema must be a JSON object"
    return payload


def test_policy_matches_versioned_schema() -> None:
    schema = _load_schema()
    policy = _load_policy()

    assert schema["$id"] == "https://int.ruoyi.local/schema/gxp-audit-policy.v2"
    assert schema["properties"]["schemaVersion"]["const"] == policy["schemaVersion"]
    jsonschema.validate(policy, schema)


def test_registered_operations_have_required_fields_and_unique_ids() -> None:
    policy = _load_policy()
    operations = policy["operations"]
    assert isinstance(operations, list)
    assert operations, "M0 policy must seed the registry with concrete GxP operations"

    operation_ids: list[str] = []
    for operation in operations:
        assert REQUIRED_OPERATION_FIELDS.issubset(operation), operation
        assert operation["operationId"], operation
        assert operation["sourceLocators"], operation
        assert operation["ownerRole"], operation
        assert operation["testIds"], operation
        operation_ids.append(operation["operationId"])

    assert len(operation_ids) == len(set(operation_ids)), "operationId must be unique"


def test_policy_seeds_first_high_risk_gxp_operations() -> None:
    policy = _load_policy()
    operation_ids = {operation["operationId"] for operation in policy["operations"]}

    assert FIRST_HIGH_RISK_OPERATION_IDS.issubset(operation_ids)


def test_registered_operations_resolve_to_exact_source_locator() -> None:
    policy = _load_policy()

    for operation in policy["operations"]:
        for locator in operation["sourceLocators"]:
            if operation["sourceType"] in {"MIGRATION", "SCRIPT"}:
                source_ref, separator, member_name = locator.partition("#")
                source_path = RUNTIME_SOURCE_ROOT / source_ref
                assert source_path.is_file(), f"sourceLocator script does not exist: {locator}"
                if separator:
                    source_text = source_path.read_text(encoding="utf-8", errors="ignore")
                    assert re.search(rf"\b{re.escape(member_name)}\s*\(", source_text), (
                        f"sourceLocator script member does not exist: {locator}"
                    )
            else:
                assert "#" in locator, f"sourceLocator must be exact class#method: {locator}"
                source_ref, member_name = locator.split("#", 1)
                assert source_ref and member_name, f"sourceLocator must include source and member: {locator}"
                relative_path = Path(*source_ref.split(".")).with_suffix(".java")
                candidates = [
                    path for pattern in (
                        f"IntRuoyiBackend/*/src/main/java/{relative_path.as_posix()}",
                        f"IntRuoyiBackend/yudao-framework/*/src/main/java/{relative_path.as_posix()}",
                    )
                    for path in WORKSPACE_ROOT.glob(pattern)
                    if path.is_file()
                ]
                assert candidates, f"sourceLocator class does not exist: {locator}"
                class_text = candidates[0].read_text(encoding="utf-8", errors="ignore")
                assert re.search(rf"\b{re.escape(member_name)}\s*\(", class_text), (
                    f"sourceLocator method does not exist: {locator}"
                )

def test_write_boundary_scan_policy_covers_required_source_types() -> None:
    policy = _load_policy()
    assert policy["coverageScope"]["writeBoundaryScan"]["registrationMode"] == "REGISTERED_OR_APPROVED_EXCLUSION"
    approved_exclusions_file = policy["coverageScope"]["writeBoundaryScan"]["approvedExclusionsFile"]
    assert isinstance(approved_exclusions_file, str) and approved_exclusions_file
    assert (WORKSPACE_ROOT / approved_exclusions_file).is_file()
    scan_categories = policy["coverageScope"]["writeBoundaryScan"]["categories"]
    configured_types = {category["sourceType"] for category in scan_categories}

    assert configured_types == set(WRITE_BOUNDARY_PATTERNS), configured_types
    for category in scan_categories:
        assert category["requiredRegistration"] is True
        assert isinstance(category["paths"], list) and category["paths"], category
        assert "expectedMinCandidates" in category, category
        disposition = category["unregisteredDisposition"]
        assert disposition["decision"] == "FAIL", category
        assert disposition["reasonCode"], category
        assert disposition["reason"], category


def test_approved_exclusion_inventory_is_explicit_and_current() -> None:
    policy = _load_policy()
    inventory_path = WORKSPACE_ROOT / policy["coverageScope"]["writeBoundaryScan"]["approvedExclusionsFile"]
    records = [
        json.loads(line)
        for line in inventory_path.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]
    assert records
    keys = {(record["sourceType"], record["candidate"]) for record in records}
    assert len(keys) == len(records)
    for record in records:
        assert record["decision"] == "APPROVED_EXCLUSION"
        assert record["candidate"]
        assert record["sourceType"] in WRITE_BOUNDARY_PATTERNS
        assert re.fullmatch(r"[0-9a-f]{64}", record["candidateSha256"])
        assert record["reasonCode"]
        assert record["reason"]
        assert record["approvedBy"]
        assert record["approvalReference"]
        assert record["candidate"] in record["reason"]
        assert record["candidateSha256"] in record["reason"]


def test_coverage_gate_rejects_unlisted_boundary_candidate(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    module_path = RUNTIME_SOURCE_ROOT / "script" / "gxp_audit_coverage_gate.py"
    module_spec = importlib.util.spec_from_file_location("gxp_audit_coverage_gate", module_path)
    assert module_spec and module_spec.loader
    gate = importlib.util.module_from_spec(module_spec)
    sys.modules[module_spec.name] = gate
    module_spec.loader.exec_module(gate)

    candidate = tmp_path / "NewWriteService.java"
    candidate.write_text("class NewWriteService { void saveNewRecord() {} }\n", encoding="utf-8")
    (tmp_path / "approved.jsonl").write_text("", encoding="utf-8")
    categories = [
        {
            "sourceType": source_type,
            "requiredRegistration": True,
            "paths": ["unused"],
            "expectedMinCandidates": 0,
            "unregisteredDisposition": {
                "decision": "FAIL",
                "reasonCode": "UNREGISTERED_CANDIDATE",
                "reason": "An unlisted candidate must stop the gate.",
            },
        }
        for source_type in sorted(WRITE_BOUNDARY_PATTERNS)
    ]
    policy = {
        "coverageScope": {"writeBoundaryScan": {
            "registrationMode": "REGISTERED_OR_APPROVED_EXCLUSION",
            "approvedExclusionsFile": "approved.jsonl",
            "categories": categories,
        }}
    }
    monkeypatch.setattr(gate, "registered_source_files", lambda root, operations: set())
    monkeypatch.setattr(
        gate,
        "discover_boundary_candidates",
        lambda root, category: [candidate] if category["sourceType"] == "DOMAIN_SERVICE" else [],
    )

    with pytest.raises(SystemExit, match="not present in approved exclusion inventory"):
        gate.validate_boundary_scan(tmp_path, policy, [])


def test_coverage_gate_rejects_changed_approved_candidate(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    module_path = RUNTIME_SOURCE_ROOT / "script" / "gxp_audit_coverage_gate.py"
    module_spec = importlib.util.spec_from_file_location("gxp_audit_coverage_gate_hash", module_path)
    assert module_spec and module_spec.loader
    gate = importlib.util.module_from_spec(module_spec)
    sys.modules[module_spec.name] = gate
    module_spec.loader.exec_module(gate)

    candidate = tmp_path / "ExistingWriteService.java"
    candidate.write_text("class ExistingWriteService { void saveExistingRecord() {} }\n", encoding="utf-8")
    inventory_record = {
        "approvalReference": "APPROVED-BASELINE-1",
        "approvedBy": "qa-owner",
        "candidate": "ExistingWriteService.java",
        "candidateSha256": "0" * 64,
        "decision": "APPROVED_EXCLUSION",
        "reason": "candidate=ExistingWriteService.java; candidateSha256=" + "0" * 64,
        "reasonCode": "NON_GXP",
        "sourceType": "DOMAIN_SERVICE",
    }
    (tmp_path / "approved.jsonl").write_text(json.dumps(inventory_record) + "\n", encoding="utf-8")
    categories = [
        {
            "sourceType": source_type,
            "requiredRegistration": True,
            "paths": ["unused"],
            "expectedMinCandidates": 0,
            "unregisteredDisposition": {
                "decision": "FAIL",
                "reasonCode": "UNREGISTERED_CANDIDATE",
                "reason": "An unlisted candidate must stop the gate.",
            },
        }
        for source_type in sorted(WRITE_BOUNDARY_PATTERNS)
    ]
    policy = {
        "coverageScope": {"writeBoundaryScan": {
            "registrationMode": "REGISTERED_OR_APPROVED_EXCLUSION",
            "approvedExclusionsFile": "approved.jsonl",
            "categories": categories,
        }}
    }
    monkeypatch.setattr(gate, "registered_source_files", lambda root, operations: set())
    monkeypatch.setattr(
        gate,
        "discover_boundary_candidates",
        lambda root, category: [candidate] if category["sourceType"] == "DOMAIN_SERVICE" else [],
    )

    with pytest.raises(SystemExit, match="SHA-256 changed"):
        gate.validate_boundary_scan(tmp_path, policy, [])


def test_write_boundary_scan_finds_current_repo_candidates() -> None:
    policy = _load_policy()
    scan_categories = policy["coverageScope"]["writeBoundaryScan"]["categories"]

    for category in scan_categories:
        source_type = category["sourceType"]
        pattern = WRITE_BOUNDARY_PATTERNS[source_type]
        candidate_count = 0
        for path_pattern in category["paths"]:
            for candidate in WORKSPACE_ROOT.glob(path_pattern):
                if not candidate.is_file():
                    continue
                text = candidate.read_text(encoding="utf-8", errors="ignore")
                if pattern.search(text):
                    candidate_count += 1
        assert candidate_count >= category["expectedMinCandidates"], (
            source_type,
            candidate_count,
            category["expectedMinCandidates"],
        )


def test_coverage_gate_reports_current_unregistered_methods_even_on_failure(tmp_path: Path) -> None:
    gate = RUNTIME_SOURCE_ROOT / "script" / "gxp_audit_coverage_gate.py"
    with tempfile.TemporaryDirectory(dir=tmp_path) as temporary_directory:
        boundary_report = Path(temporary_directory) / "boundary-exclusions.jsonl"
        result = subprocess.run(
            [
                sys.executable,
                str(gate),
                "--root",
                str(WORKSPACE_ROOT),
                "--policy",
                "IntRuoyiBackend/config/gxp-audit-policy.yaml",
                "--boundary-report",
                str(boundary_report),
            ],
            cwd=WORKSPACE_ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            check=False,
        )
        # The real repository has open coverage gaps. Scanner correctness must not
        # require fabricated approvals to turn the release gate green.
        assert result.returncode == 1, result.stderr or result.stdout
        assert "FAIL gxp audit coverage gate" in result.stderr
        records = [
            json.loads(line)
            for line in boundary_report.read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]
        entries = [record for record in records if record["decision"] == "UNREGISTERED_ENTRY"]
        assert any(record["sourceLocator"].endswith("MesTeamLeaderActiveOrderServiceImpl#removeActiveOrder")
                   for record in entries)
        assert any(record["sourceLocator"].endswith("MesTeamLeaderActiveOrderServiceImpl#executeDataCleanup")
                   for record in entries)
        assert all(record["line"] > 0 and record["signature"] for record in entries)
        exclusion_records = [record for record in records if record["decision"] == "APPROVED_EXCLUSION"]
        assert exclusion_records
        required_fields = {
            "candidate",
            "decision",
            "reasonCode",
            "reason",
            "approvedBy",
            "approvalReference",
        }
        assert all(required_fields.issubset(record) for record in exclusion_records)
        assert all(record["decision"] == "APPROVED_EXCLUSION" for record in exclusion_records)
        assert all(
            record["candidateSha256"] == hashlib.sha256(
                (WORKSPACE_ROOT / Path(record["candidate"])).read_bytes()
            ).hexdigest()
            for record in exclusion_records
        )
        assert all(record["candidate"] in record["reason"] for record in exclusion_records)
        assert all(record["candidateSha256"] in record["reason"] for record in exclusion_records)
        assert len({record["reason"] for record in exclusion_records}) == len(exclusion_records)
