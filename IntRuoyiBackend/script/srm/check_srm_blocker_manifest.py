from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


EXPECTED_WAVE_LEVELS = {
    "W0": "L4",
    "W1": "L1",
    "W2": "L2",
    "W3": "L1",
    "W4": "L1",
    "W5": "L1",
    "W6": "L0",
}

EXPECTED_FREEZE_PACKAGES = {
    "W1": "W0-01",
    "W2": "W0-02",
    "W3": "W0-03",
    "W4": "W0-04",
    "W5": "W0-05",
}

EXPECTED_DEPENDENCY_IDS = {f"SRM9-DEP-{index:03d}" for index in range(1, 13)}
EXPECTED_LAUNCH_IDS = {f"SRM9-LAUNCH-{index:03d}" for index in range(1, 6)}
EXPECTED_EXCEL_ITEMS = {str(index) for index in range(1, 13)}
SENSITIVE_KEYS = {"password", "passwd", "token", "cookie", "appsecret", "secret", "credential", "credentials", "secretvalue"}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate SRM srm9 machine-readable blocker manifest.")
    parser.add_argument("--manifest", required=True, help="Path to docs/dependencies/srm9-blocker-manifest.json")
    parser.add_argument("--freeze-pack", required=True, help="Path to docs/srm/srm9-w0-freeze-pack.md")
    parser.add_argument("--landing-plan", required=True, help="Path to docs/srm/srm9-landing-plan.md")
    args = parser.parse_args(argv)

    manifest_path = Path(args.manifest)
    freeze_pack_path = Path(args.freeze_pack)
    landing_plan_path = Path(args.landing_plan)

    missing_paths = [path for path in (manifest_path, freeze_pack_path, landing_plan_path) if not path.exists()]
    if missing_paths:
        for path in missing_paths:
            print(f"Required file not found: {path}", file=sys.stderr)
        return 2

    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        print(f"Manifest JSON is invalid: {exc}", file=sys.stderr)
        return 2

    freeze_pack = freeze_pack_path.read_text(encoding="utf-8")
    landing_plan = landing_plan_path.read_text(encoding="utf-8")

    errors = validate_manifest(manifest, freeze_pack, landing_plan)
    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1

    print("SRM9 blocker manifest valid")
    return 0


def validate_manifest(manifest: dict[str, Any], freeze_pack: str, landing_plan: str) -> list[str]:
    errors: list[str] = []

    if manifest.get("schemaVersion") != 1:
        errors.append("schemaVersion must be 1")

    waves = manifest.get("waves")
    if not isinstance(waves, dict):
        return ["waves must be an object"]

    blockers = manifest.get("blockers")
    if not isinstance(blockers, dict):
        errors.append("blockers must be an object")
        blockers = {}

    for wave, expected_level in EXPECTED_WAVE_LEVELS.items():
        wave_data = waves.get(wave)
        if not isinstance(wave_data, dict):
            errors.append(f"{wave} missing from manifest waves")
            continue

        if wave_data.get("level") != expected_level:
            errors.append(f"{wave} level mismatch: manifest {wave_data.get('level')}, expected {expected_level}")

        landing_line = find_landing_wave_line(landing_plan, wave)
        if landing_line is None:
            errors.append(f"{wave} missing from landing plan")
        elif f"`{expected_level}`" not in landing_line:
            errors.append(f"{wave} landing plan level mismatch: expected {expected_level}")

        excel_items = set(wave_data.get("excelItems", []))
        if not excel_items:
            errors.append(f"{wave} excelItems must not be empty")
        for item in excel_items:
            if f"`{item}`" not in landing_plan:
                errors.append(f"Excel item {item} from {wave} missing from landing plan")

        if wave in EXPECTED_FREEZE_PACKAGES:
            expected_package = EXPECTED_FREEZE_PACKAGES[wave]
            if wave_data.get("freezePackage") != expected_package:
                errors.append(f"{wave} freeze package mismatch: manifest {wave_data.get('freezePackage')}, expected {expected_package}")
            freeze_status = find_package_status(freeze_pack, expected_package)
            if freeze_status is None:
                errors.append(f"{expected_package} status missing from freeze pack")
            elif wave_data.get("freezePackageStatus") != freeze_status:
                errors.append(f"{wave} status mismatch: manifest {wave_data.get('freezePackageStatus')}, freeze pack {freeze_status}")
            if freeze_status in {"BLOCKED", "DEFERRED"} and wave_data.get("status") != "BLOCKED":
                errors.append(f"{wave} must remain BLOCKED while {expected_package} is {freeze_status}")

        if wave in {"W1", "W2", "W3", "W4", "W5", "W6"}:
            ensure_non_empty_list(errors, wave_data, "blockerIds", wave)
            ensure_non_empty_list(errors, wave_data, "requiredEvidence", wave)
            ensure_non_empty_list(errors, wave_data, "disallowedActions", wave)

    manifest_items = collect_manifest_excel_items(waves)
    if manifest_items != EXPECTED_EXCEL_ITEMS:
        errors.append(f"Excel item coverage mismatch: manifest {sorted(manifest_items)}, expected {sorted(EXPECTED_EXCEL_ITEMS)}")

    baseline = manifest.get("regressionBaseline")
    if not isinstance(baseline, dict):
        errors.append("regressionBaseline must be an object")
    else:
        baseline_status = find_package_status(freeze_pack, "W0-06")
        if baseline.get("freezePackageStatus") != baseline_status:
            errors.append(f"W0-06 status mismatch: manifest {baseline.get('freezePackageStatus')}, freeze pack {baseline_status}")
        if set(baseline.get("excelItems", [])) != {"5", "6"}:
            errors.append("W0-06 regression baseline must cover only Excel items 5 and 6")

    for blocker_id in sorted(EXPECTED_DEPENDENCY_IDS | EXPECTED_LAUNCH_IDS):
        if blocker_id not in blockers:
            errors.append(f"Missing blocker id: {blocker_id}")

    referenced_blockers = {
        blocker_id
        for wave_data in waves.values()
        if isinstance(wave_data, dict)
        for blocker_id in wave_data.get("blockerIds", [])
    }
    missing_referenced = sorted(referenced_blockers - set(blockers))
    if missing_referenced:
        errors.append(f"Referenced blocker ids missing definitions: {missing_referenced}")

    secret_hits = find_secret_markers(manifest)
    if secret_hits:
        errors.append(f"Manifest contains forbidden secret marker(s): {sorted(secret_hits)}")

    return errors


def ensure_non_empty_list(errors: list[str], data: dict[str, Any], field: str, wave: str) -> None:
    value = data.get(field)
    if not isinstance(value, list) or not value:
        errors.append(f"{wave} {field} must be a non-empty list")


def find_package_status(text: str, package: str) -> str | None:
    for line in text.splitlines():
        if package not in line:
            continue
        match = re.search(r"`(FROZEN|BLOCKED|DEFERRED)`", line)
        if match:
            return match.group(1)
    return None


def find_landing_wave_line(text: str, wave: str) -> str | None:
    marker = f"| {wave} "
    for line in text.splitlines():
        if line.startswith(marker):
            return line
    return None


def collect_manifest_excel_items(waves: dict[str, Any]) -> set[str]:
    items: set[str] = set()
    for wave_data in waves.values():
        if isinstance(wave_data, dict):
            items.update(str(item) for item in wave_data.get("excelItems", []))
    return items


def find_secret_markers(value: Any) -> set[str]:
    hits: set[str] = set()
    if isinstance(value, dict):
        for key, nested in value.items():
            lowered_key = str(key).lower()
            if lowered_key in SENSITIVE_KEYS:
                if isinstance(nested, str) and nested.strip():
                    hits.add(lowered_key)
                elif nested not in (None, "", [], {}):
                    hits.add(lowered_key)
            hits.update(find_secret_markers(nested))
    elif isinstance(value, list):
        for item in value:
            hits.update(find_secret_markers(item))
    return hits


if __name__ == "__main__":
    raise SystemExit(main())
