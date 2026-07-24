import argparse
import json
import shutil
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
GATE_SCRIPT = ROOT / "script" / "dcc_view_permission_confirmation_gate.py"
MATRIX_SCRIPT = ROOT / "script" / "dcc_matrix_workbook_to_sql.py"
PRODUCT_GROUP_SCRIPT = ROOT / "script" / "dcc_product_group_workbook_to_sql.py"


def run_command(args):
    cp = subprocess.run(
        args,
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if cp.returncode != 0:
        raise RuntimeError((cp.stderr or cp.stdout).strip())
    return cp.stdout.strip()


def write_manifest(path, gate_result_path, matrix_sql, product_group_sql, input_files):
    gate_result = json.loads(Path(gate_result_path).read_text(encoding="utf-8"))
    manifest = {
        "ready": True,
        "gateResult": str(Path(gate_result_path)),
        "matrixSql": str(Path(matrix_sql)),
        "productGroupSql": str(Path(product_group_sql)),
        "inputFiles": {key: str(Path(value)) for key, value in input_files.items()},
        "confirmedFiles": gate_result["matrix"]["confirmedFiles"],
        "confirmedRoles": gate_result["matrix"]["confirmedRoles"],
        "confirmedProductGroupRows": gate_result["productGroup"]["confirmedProductGroupRows"],
        "executionOrder": [
            str(Path(matrix_sql).name),
            str(Path(product_group_sql).name),
        ],
    }
    Path(path).write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--matrix-workbook", required=True)
    parser.add_argument("--product-group-workbook", required=True)
    parser.add_argument("--output-dir", required=True)
    args = parser.parse_args()

    output_dir = Path(args.output_dir)
    try:
        if output_dir.exists():
            shutil.rmtree(output_dir)
        output_dir.mkdir(parents=True, exist_ok=False)

        gate_result = output_dir / "confirmation-gate-result.json"
        matrix_sql = output_dir / "01-dcc-matrix-confirmed.sql"
        product_group_sql = output_dir / "02-dcc-product-group-confirmed.sql"
        input_dir = output_dir / "inputs"
        matrix_classification_csv = input_dir / "matrix-file-classification.csv"
        matrix_role_csv = input_dir / "matrix-role-members.csv"
        product_group_csv = input_dir / "product-group-bindings.csv"
        manifest = output_dir / "manifest.json"
        input_dir.mkdir(parents=True, exist_ok=False)

        run_command([
            sys.executable, "-X", "utf8", str(GATE_SCRIPT),
            "--matrix-workbook", args.matrix_workbook,
            "--product-group-workbook", args.product_group_workbook,
            "--output-json", str(gate_result),
        ])
        run_command([
            sys.executable, "-X", "utf8", str(MATRIX_SCRIPT),
            "--input-xlsx", args.matrix_workbook,
            "--output-sql", str(matrix_sql),
            "--keep-classification-csv", str(matrix_classification_csv),
            "--keep-role-csv", str(matrix_role_csv),
        ])
        run_command([
            sys.executable, "-X", "utf8", str(PRODUCT_GROUP_SCRIPT),
            "--input-xlsx", args.product_group_workbook,
            "--output-sql", str(product_group_sql),
            "--keep-csv", str(product_group_csv),
        ])
        write_manifest(manifest, gate_result, matrix_sql, product_group_sql, {
            "matrixClassificationCsv": matrix_classification_csv,
            "matrixRoleCsv": matrix_role_csv,
            "productGroupCsv": product_group_csv,
        })
        print(f"bundle_dir={output_dir}")
    except Exception as exc:
        if output_dir.exists():
            shutil.rmtree(output_dir)
        print(str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
