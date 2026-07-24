import argparse
import csv
import json
import sys
from pathlib import Path


def read_csv(path):
    with Path(path).open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def collect_tenants(path):
    rows = read_csv(path)
    tenants = set()
    for row in rows:
        value = str(row.get("tenant_id") or "").strip()
        if value:
            tenants.add(value)
    return tenants


def verify_tenant(bundle_dir, allowed_tenant_id):
    bundle = Path(bundle_dir)
    manifest_path = bundle / "manifest.json"
    if not manifest_path.is_file():
        raise ValueError(f"manifest is missing: {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    input_files = manifest.get("inputFiles") or {}
    paths = {
        "matrixClassificationCsv": input_files.get("matrixClassificationCsv"),
        "productGroupCsv": input_files.get("productGroupCsv"),
    }
    actual_tenants = set()
    checked_files = []
    for label, raw_path in paths.items():
        if not raw_path:
            raise ValueError(f"{label} is missing from manifest")
        path = Path(raw_path)
        if not path.is_file():
            raise ValueError(f"{label} file is missing: {path}")
        actual_tenants.update(collect_tenants(path))
        checked_files.append(str(path))
    disallowed = sorted(tenant for tenant in actual_tenants if tenant != str(allowed_tenant_id))
    if disallowed:
        raise ValueError(f"bundle contains tenant_id values not allowed for execution: {disallowed}; allowed={allowed_tenant_id}")
    return {
        "ready": True,
        "allowedTenantId": str(allowed_tenant_id),
        "actualTenantIds": sorted(actual_tenants),
        "checkedFiles": checked_files,
        "roleCsvTenantNote": "matrixRoleCsv has no tenant_id column; role tenant is checked by SQL prechecks and apply verification",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle-dir", required=True)
    parser.add_argument("--allowed-tenant-id", required=True)
    parser.add_argument("--output-json")
    args = parser.parse_args()

    exit_code = 0
    try:
        result = verify_tenant(args.bundle_dir, args.allowed_tenant_id)
    except Exception as exc:
        result = {"ready": False, "reasons": [str(exc)]}
        exit_code = 1

    output = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output_json:
        Path(args.output_json).parent.mkdir(parents=True, exist_ok=True)
        Path(args.output_json).write_text(output + "\n", encoding="utf-8")
    print(output, file=sys.stderr if exit_code else sys.stdout)
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
