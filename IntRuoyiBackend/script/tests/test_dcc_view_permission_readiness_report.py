import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_view_permission_readiness_report.py"


def write_gate(path, ready):
    payload = {
        "ready": ready,
        "matrix": {
            "confirmedFiles": 1 if ready else 0,
            "confirmedRoles": 1 if ready else 0,
        },
        "productGroup": {
            "confirmedProductGroupRows": 1 if ready else 0,
        },
        "reasons": [] if ready else [
            "matrix: matrix workbook has no confirmed classification or role rows",
            "productGroup: product group workbook has no confirmed rows",
        ],
    }
    path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")


def run_report(tmp_path, ready):
    gate = tmp_path / "gate.json"
    output = tmp_path / "report.md"
    write_gate(gate, ready)
    cp = subprocess.run(
        [
            "python", "-X", "utf8", str(SCRIPT),
            "--gate-json", str(gate),
            "--output-md", str(output),
            "--matrix-workbook", "matrix.xlsx",
            "--product-group-workbook", "product.xlsx",
            "--bundle-output-dir", "bundle-output",
        ],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    return cp, output


def test_report_lists_business_todo_when_gate_is_not_ready(tmp_path):
    cp, output = run_report(tmp_path, ready=False)

    assert cp.returncode == 0, cp.stderr
    text = output.read_text(encoding="utf-8")
    assert "未就绪，禁止生成或执行 SQL" in text
    assert "matrix workbook has no confirmed" in text
    assert "manual_confirm_category_code" in text
    assert "主管角色候选" in text
    assert "候选明细" in text
    assert "dcc_view_permission_confirmation_gate.py" in text
    assert " \\\n" not in text


def test_report_lists_bundle_command_when_gate_is_ready(tmp_path):
    cp, output = run_report(tmp_path, ready=True)

    assert cp.returncode == 0, cp.stderr
    text = output.read_text(encoding="utf-8")
    assert "可生成 SQL 包" in text
    assert "dcc_view_permission_sql_bundle.py" in text
    assert "主矩阵文件归类确认：1" in text
    assert "产品组绑定确认：1" in text
    assert " \\\n" not in text
