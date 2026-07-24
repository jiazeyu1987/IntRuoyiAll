import argparse
import json
import re
import sys
from pathlib import Path


FORBIDDEN_TOKENS = [
    "can_download",
    "dcc_directory_access_rule",
    "dcc_directory_permission",
]

ALLOWED_WRITE_TABLES = {
    "dcc_controlled_file",
    "system_user_role",
    "dcc_product_visibility_group",
    "dcc_product_visibility_group_member",
    "dcc_product_visibility_group_product",
}

WRITE_PATTERN = re.compile(r"\b(?:UPDATE|INSERT\s+INTO|DELETE\s+FROM)\s+([`A-Za-z0-9_]+)", re.IGNORECASE)


def normalize_table(token):
    return token.strip("`").lower()


def require_file(path, label):
    if not Path(path).is_file():
        raise ValueError(f"{label} is missing: {path}")


def verify_sql(path):
    text = Path(path).read_text(encoding="utf-8")
    lowered = text.lower()
    for token in FORBIDDEN_TOKENS:
        if token.lower() in lowered:
            raise ValueError(f"Forbidden token found in {path}: {token}")
    for match in WRITE_PATTERN.finditer(text):
        table = normalize_table(match.group(1))
        if table.startswith("tmp_dcc_"):
            continue
        if table not in ALLOWED_WRITE_TABLES:
            raise ValueError(f"Unauthorized write table in {path}: {table}")
    if "START TRANSACTION;" not in text or "COMMIT;" not in text:
        raise ValueError(f"SQL file must contain START TRANSACTION and COMMIT: {path}")


def verify_bundle(bundle_dir):
    bundle = Path(bundle_dir)
    manifest_path = bundle / "manifest.json"
    require_file(manifest_path, "manifest")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("ready") is not True:
        raise ValueError("manifest ready must be true")
    expected_order = ["01-dcc-matrix-confirmed.sql", "02-dcc-product-group-confirmed.sql"]
    if manifest.get("executionOrder") != expected_order:
        raise ValueError(f"executionOrder must be {expected_order}")

    required_paths = {
        "gateResult": manifest.get("gateResult"),
        "matrixSql": manifest.get("matrixSql"),
        "productGroupSql": manifest.get("productGroupSql"),
    }
    input_files = manifest.get("inputFiles") or {}
    for key in ("matrixClassificationCsv", "matrixRoleCsv", "productGroupCsv"):
        required_paths[f"inputFiles.{key}"] = input_files.get(key)

    for label, raw_path in required_paths.items():
        if not raw_path:
            raise ValueError(f"{label} is missing from manifest")
        require_file(raw_path, label)

    verify_sql(required_paths["matrixSql"])
    verify_sql(required_paths["productGroupSql"])
    return {
        "ready": True,
        "bundleDir": str(bundle),
        "checkedSqlFiles": [required_paths["matrixSql"], required_paths["productGroupSql"]],
        "checkedInputFiles": [input_files["matrixClassificationCsv"], input_files["matrixRoleCsv"], input_files["productGroupCsv"]],
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle-dir", required=True)
    parser.add_argument("--output-json")
    args = parser.parse_args()

    try:
        result = verify_bundle(args.bundle_dir)
    except Exception as exc:
        result = {"ready": False, "reasons": [str(exc)]}
        print(json.dumps(result, ensure_ascii=False, indent=2), file=sys.stderr)
        return 1

    output = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output_json:
        Path(args.output_json).parent.mkdir(parents=True, exist_ok=True)
        Path(args.output_json).write_text(output + "\n", encoding="utf-8")
    print(output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
