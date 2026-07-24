import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_view_permission_rollout_runbook.py"


def test_rollout_runbook_contains_required_gate_sequence(tmp_path):
    output = tmp_path / "runbook.md"
    cp = subprocess.run(
        [
            "python", "-X", "utf8", str(SCRIPT),
            "--output-md", str(output),
            "--matrix-workbook", "matrix.xlsx",
            "--product-group-workbook", "product.xlsx",
            "--bundle-dir", "bundle",
            "--gate-json", "gate.json",
            "--mysql-command", "mysql testdb",
        ],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )

    assert cp.returncode == 0, cp.stderr
    text = output.read_text(encoding="utf-8")
    required = [
        "dcc_view_permission_confirmation_gate.py",
        "dcc_view_permission_sql_bundle.py",
        "dcc_view_permission_sql_bundle_verify.py",
        "01-dcc-matrix-confirmed.sql",
        "02-dcc-product-group-confirmed.sql",
        "dcc_view_permission_apply_verify.py",
        "真实用户路径验证",
    ]
    positions = [text.index(item) for item in required]
    assert positions == sorted(positions)


def test_rollout_runbook_forbids_unsafe_shortcuts(tmp_path):
    output = tmp_path / "runbook.md"
    subprocess.run(
        ["python", "-X", "utf8", str(SCRIPT), "--output-md", str(output)],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )

    text = output.read_text(encoding="utf-8")
    assert "禁止跳过确认闸门" in text
    assert "禁止未做静态校验就执行 SQL" in text
    assert "不得在正式环境执行 SQL" in text
    assert "确认闸门" in text
    assert "<database>" not in text
