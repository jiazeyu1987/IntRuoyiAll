from __future__ import annotations

import argparse
import hashlib
import os
import re
from dataclasses import dataclass
from pathlib import Path


REQUIRED_FIELDS = {
    "operationId", "sourceType", "sourceLocator", "domain", "subjectType", "actionType",
    "reasonPolicy", "signaturePolicy", "statePolicy", "retentionClass", "testIds", "owner",
    "applicability",
}
ALLOWED_SOURCE_TYPES = {"SERVICE_METHOD", "JOB", "MIGRATION", "SCRIPT"}
ALLOWED_APPLICABILITY = {"GXP", "NOT_APPLICABLE"}
REQUIRED_HIGH_RISK_DOMAINS = {"EDHR", "DCC", "SIGNATURE", "SYSTEM", "RELEASE"}
SKIPPED_DIRS = {".git", ".idea", ".mvn", "node_modules", "target", "dist", "build", ".pytest_cache"}


@dataclass(frozen=True)
class Operation:
    values: dict[str, str]

    @property
    def operation_id(self) -> str:
        return self.values["operationId"]

    @property
    def source_type(self) -> str:
        return self.values["sourceType"]

    @property
    def source_locator(self) -> str:
        return self.values["sourceLocator"]

    @property
    def domain(self) -> str:
        return self.values["domain"]


def parse_policy(path: Path) -> tuple[str, list[Operation]]:
    text = path.read_text(encoding="utf-8")
    version_match = re.search(r"^policyVersion:\s*(\S+)\s*$", text, re.MULTILINE)
    if not version_match:
        raise SystemExit("policyVersion is required")
    operations: list[Operation] = []
    current: dict[str, str] | None = None
    for raw_line in text.splitlines():
        line = raw_line.rstrip()
        if line.startswith("  - "):
            if current:
                operations.append(Operation(current))
            current = {}
            key, value = line[4:].split(":", 1)
            current[key.strip()] = value.strip()
            continue
        if current is not None and line.startswith("    ") and ":" in line:
            key, value = line.strip().split(":", 1)
            current[key.strip()] = value.strip()
    if current:
        operations.append(Operation(current))
    if not operations:
        raise SystemExit("operations must not be empty")
    return version_match.group(1), operations


def discover_annotations(root: Path) -> dict[str, str]:
    pattern = re.compile(
        r"@GxpWriteOperation\s*\(\s*operationId\s*=\s*\"([^\"]+)\"\s*\)\s*"
        r"(?:@\w+(?:\([^)]*\))?\s*)*public\s+[\w<>, ?\[\]]+\s+(\w+)\s*\(",
        re.MULTILINE,
    )
    result: dict[str, str] = {}
    for source_root in root.glob("*/src/main/java"):
        for current_dir, dir_names, file_names in os.walk(source_root):
            dir_names[:] = [name for name in dir_names if name not in SKIPPED_DIRS]
            for file_name in file_names:
                if not file_name.endswith(".java"):
                    continue
                java_file = Path(current_dir) / file_name
                text = java_file.read_text(encoding="utf-8")
                package_match = re.search(r"^package\s+([\w.]+);", text, re.MULTILINE)
                class_match = re.search(r"\bclass\s+(\w+)", text)
                if not package_match or not class_match:
                    continue
                fqcn = f"{package_match.group(1)}.{class_match.group(1)}"
                for operation_id, method_name in pattern.findall(text):
                    result[operation_id] = f"{fqcn}#{method_name}"
    return result


def source_locator_exists(root: Path, operation: Operation) -> bool:
    locator = operation.source_locator
    if operation.source_type == "SERVICE_METHOD":
        class_name, _, method_name = locator.partition("#")
        if not class_name or not method_name:
            return False
        class_suffix = Path(*class_name.split(".")).with_suffix(".java")
        candidates = [
            source_root / class_suffix
            for source_root in root.glob("*/src/main/java")
            if (source_root / class_suffix).exists()
        ]
        if not candidates:
            return False
        text = candidates[0].read_text(encoding="utf-8")
        return re.search(rf"\b{re.escape(method_name)}\s*\(", text) is not None
    return (root / locator).exists()


def canonical_report(policy_version: str, operations: list[Operation], annotations: dict[str, str]) -> str:
    rows = [
        f"{policy_version}|{op.operation_id}|{op.source_type}|{op.source_locator}|{op.domain}|"
        f"{annotations.get(op.operation_id, '')}"
        for op in sorted(operations, key=lambda item: item.operation_id)
    ]
    return "\n".join(rows)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".")
    parser.add_argument("--policy", default="config/gxp-audit-policy.yaml")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    policy_path = (root / args.policy).resolve()
    policy_version, operations = parse_policy(policy_path)
    annotations = discover_annotations(root)

    errors: list[str] = []
    seen: set[str] = set()
    for operation in operations:
        missing = REQUIRED_FIELDS - operation.values.keys()
        if missing:
            errors.append(f"{operation.operation_id}: missing fields {sorted(missing)}")
        if operation.operation_id in seen:
            errors.append(f"{operation.operation_id}: duplicate operationId")
        seen.add(operation.operation_id)
        if operation.source_type not in ALLOWED_SOURCE_TYPES:
            errors.append(f"{operation.operation_id}: invalid sourceType {operation.source_type}")
        if operation.values.get("applicability") not in ALLOWED_APPLICABILITY:
            errors.append(f"{operation.operation_id}: invalid applicability {operation.values.get('applicability')}")
        if not operation.values.get("owner"):
            errors.append(f"{operation.operation_id}: owner is required")
        if not operation.values.get("testIds", "").startswith("[") or operation.values.get("testIds") == "[]":
            errors.append(f"{operation.operation_id}: testIds must be a non-empty list")
        if not source_locator_exists(root, operation):
            errors.append(f"{operation.operation_id}: sourceLocator does not resolve: {operation.source_locator}")
    for operation_id, locator in annotations.items():
        if operation_id not in seen:
            errors.append(f"{operation_id}: annotated GxP write is missing from policy ({locator})")
        elif next(op for op in operations if op.operation_id == operation_id).source_locator != locator:
            errors.append(f"{operation_id}: annotation locator mismatch: {locator}")
    missing_domains = REQUIRED_HIGH_RISK_DOMAINS - {op.domain for op in operations if op.values.get("applicability") == "GXP"}
    if missing_domains:
        errors.append(f"missing high-risk domains: {sorted(missing_domains)}")
    if errors:
        raise SystemExit("\n".join(errors))

    report = canonical_report(policy_version, operations, annotations)
    print(f"PASS gxp audit coverage gate operations={len(operations)} annotations={len(annotations)} "
          f"sha256={hashlib.sha256(report.encode('utf-8')).hexdigest()}")


if __name__ == "__main__":
    main()
