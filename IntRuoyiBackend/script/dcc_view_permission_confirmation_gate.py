import argparse
import json
import tempfile
from pathlib import Path

from dcc_matrix_confirmed_sql_generator import collect_confirmed_files, collect_confirmed_roles, read_csv
from dcc_matrix_workbook_to_sql import export_workbook as export_matrix_workbook
from dcc_product_group_confirmed_sql_generator import collect_confirmed_rows as collect_confirmed_product_groups
from dcc_product_group_workbook_export import load_rows as load_product_group_rows, write_csv as write_product_group_csv


def matrix_summary(matrix_workbook):
    with tempfile.TemporaryDirectory() as temp_dir:
        classification_csv = Path(temp_dir) / "matrix-classification.csv"
        role_csv = Path(temp_dir) / "matrix-roles.csv"
        classification_rows, role_rows = export_matrix_workbook(matrix_workbook, classification_csv, role_csv)
        confirmed_files = collect_confirmed_files(read_csv(classification_csv))
        confirmed_roles = collect_confirmed_roles(read_csv(role_csv))
    reasons = []
    if not confirmed_files and not confirmed_roles:
        reasons.append("matrix workbook has no confirmed classification or role rows")
    return {
        "input": str(matrix_workbook),
        "classificationRows": classification_rows,
        "roleRows": role_rows,
        "confirmedFiles": len(confirmed_files),
        "confirmedRoles": len(confirmed_roles),
        "ready": not reasons,
        "reasons": reasons,
    }


def product_group_summary(product_group_workbook):
    with tempfile.TemporaryDirectory() as temp_dir:
        product_group_csv = Path(temp_dir) / "product-groups.csv"
        confirmed_export_rows = load_product_group_rows(product_group_workbook)
        write_product_group_csv(product_group_csv, confirmed_export_rows)
        confirmed_rows = collect_confirmed_product_groups(read_csv(product_group_csv))
    reasons = []
    if not confirmed_rows:
        reasons.append("product group workbook has no confirmed rows")
    return {
        "input": str(product_group_workbook),
        "confirmedProductGroupRows": len(confirmed_rows),
        "ready": not reasons,
        "reasons": reasons,
    }


def build_result(matrix_workbook=None, product_group_workbook=None):
    if not matrix_workbook and not product_group_workbook:
        raise ValueError("At least one workbook must be provided")
    result = {
        "ready": True,
        "matrix": None,
        "productGroup": None,
        "reasons": [],
    }
    if matrix_workbook:
        result["matrix"] = matrix_summary(Path(matrix_workbook))
    if product_group_workbook:
        result["productGroup"] = product_group_summary(Path(product_group_workbook))
    for section_name in ("matrix", "productGroup"):
        section = result.get(section_name)
        if section and not section["ready"]:
            result["ready"] = False
            result["reasons"].extend(f"{section_name}: {reason}" for reason in section["reasons"])
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--matrix-workbook")
    parser.add_argument("--product-group-workbook")
    parser.add_argument("--output-json")
    args = parser.parse_args()

    try:
        result = build_result(args.matrix_workbook, args.product_group_workbook)
    except Exception as exc:
        result = {
            "ready": False,
            "matrix": None,
            "productGroup": None,
            "reasons": [str(exc)],
        }

    output = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output_json:
        Path(args.output_json).parent.mkdir(parents=True, exist_ok=True)
        Path(args.output_json).write_text(output + "\n", encoding="utf-8")
    print(output)
    return 0 if result["ready"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
