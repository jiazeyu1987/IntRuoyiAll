import json
import re
from pathlib import Path

import jsonschema
import yaml


WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
POLICY_PATH = WORKSPACE_ROOT / "config" / "gxp-audit-policy.yaml"
SCHEMA_PATH = WORKSPACE_ROOT / "config" / "gxp-audit-policy.schema.json"


REQUIRED_OPERATION_FIELDS = {
    "operationId",
    "sourceType",
    "sourceLocator",
    "domain",
    "subject",
    "action",
    "reasonPolicy",
    "signaturePolicy",
    "statePolicy",
    "retentionClass",
    "testIds",
    "owner",
    "applicabilityDecision",
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
    "MES.BATCH_RECORD.UPDATE",
    "DCC.DATA_RELATION.CREATE",
    "SIGNATURE.ELECTRONIC_SIGNATURE.SIGN",
    "SYSTEM.USER_ROLE.ASSIGN",
    "INFRA.SYSTEM_CONFIG.UPDATE",
    "INFRA.RELEASE_MIGRATION.SYSTEM_CHANGE",
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

    assert schema["$id"] == "https://intruoyi.local/schemas/gxp-audit-policy.schema.json"
    assert schema["x-schema-version"] == policy["schemaVersion"]
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
        assert operation["sourceLocator"], operation
        assert operation["owner"], operation
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
        locator = operation["sourceLocator"]
        assert "#" in locator, f"sourceLocator must be exact class#method: {locator}"
        source_ref, member_name = locator.split("#", 1)
        assert source_ref and member_name, f"sourceLocator must include source and member: {locator}"
        if operation["sourceType"] in {"MIGRATION", "OPS_SCRIPT"}:
            source_path = WORKSPACE_ROOT / source_ref
            assert source_path.is_file(), f"sourceLocator script does not exist: {locator}"
            source_text = source_path.read_text(encoding="utf-8", errors="ignore")
            assert re.search(rf"\b{re.escape(member_name)}\s*\(", source_text), (
                f"sourceLocator script member does not exist: {locator}"
            )
        else:
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
    scan_categories = policy["writeBoundaryScan"]["categories"]
    configured_types = {category["sourceType"] for category in scan_categories}

    assert configured_types == set(WRITE_BOUNDARY_PATTERNS), configured_types
    for category in scan_categories:
        assert category["requiredRegistration"] is True
        assert isinstance(category["paths"], list) and category["paths"], category
        assert "expectedMinCandidates" in category, category


def test_write_boundary_scan_finds_current_repo_candidates() -> None:
    policy = _load_policy()
    scan_categories = policy["writeBoundaryScan"]["categories"]

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
