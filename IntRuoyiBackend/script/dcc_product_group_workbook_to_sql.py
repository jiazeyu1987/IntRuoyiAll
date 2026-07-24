import argparse
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EXPORT_SCRIPT = ROOT / "script" / "dcc_product_group_workbook_export.py"
SQL_SCRIPT = ROOT / "script" / "dcc_product_group_confirmed_sql_generator.py"


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


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-xlsx", required=True)
    parser.add_argument("--output-sql", required=True)
    parser.add_argument("--keep-csv")
    args = parser.parse_args()

    output_sql = Path(args.output_sql)
    output_sql.parent.mkdir(parents=True, exist_ok=True)
    temp_csv = None
    try:
        if args.keep_csv:
            confirmed_csv = Path(args.keep_csv)
            confirmed_csv.parent.mkdir(parents=True, exist_ok=True)
        else:
            temp_dir = tempfile.TemporaryDirectory()
            temp_csv = temp_dir
            confirmed_csv = Path(temp_dir.name) / "confirmed-product-groups.csv"

        export_stdout = run_command([
            sys.executable, "-X", "utf8", str(EXPORT_SCRIPT),
            "--input-xlsx", args.input_xlsx,
            "--output-csv", str(confirmed_csv),
        ])
        sql_stdout = run_command([
            sys.executable, "-X", "utf8", str(SQL_SCRIPT),
            "--input-csv", str(confirmed_csv),
            "--output-sql", str(output_sql),
        ])
        print(export_stdout)
        if sql_stdout:
            print(sql_stdout)
    except Exception as exc:
        if output_sql.exists():
            output_sql.unlink()
        print(str(exc), file=sys.stderr)
        return 1
    finally:
        if temp_csv is not None:
            temp_csv.cleanup()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
