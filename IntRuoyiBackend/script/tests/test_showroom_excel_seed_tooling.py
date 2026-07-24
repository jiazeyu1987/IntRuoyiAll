from __future__ import annotations

import subprocess
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
WORKBOOK_NAME = "展厅产品与描述清单.xlsx"
OLD_MANUAL_PRODUCT_CODE = "manual_product_9a40e30a6c464c9f9a84ea6d24847272"
NORMALIZED_PRODUCT_CODE = "product_166"


def find_showroom_workbook() -> Path:
    resource_dir = REPO_ROOT.parent / "resource"
    workbook = resource_dir / WORKBOOK_NAME
    if workbook.exists():
        return workbook
    raise FileNotFoundError(f"showroom workbook not found: {workbook}")


def test_showroom_excel_seed_script_matches_committed_sql() -> None:
    workbook = find_showroom_workbook()
    script = REPO_ROOT / "sql" / "showroom" / "scripts" / "generate_showroom_excel_seed.py"
    committed_sql = REPO_ROOT / "sql" / "showroom" / "20260519_showroom_excel_seed.sql"

    temp_sql = Path(__file__).with_name("tmp_showroom_seed.sql")
    try:
        subprocess.run(
            [sys.executable, "-X", "utf8", str(script), "--input", str(workbook), "--output", str(temp_sql)],
            check=True,
            cwd=REPO_ROOT,
        )
        generated_text = temp_sql.read_text(encoding="utf-8")
        committed_text = committed_sql.read_text(encoding="utf-8")
        assert generated_text == committed_text
        assert OLD_MANUAL_PRODUCT_CODE not in generated_text
        assert "心内介植入展柜" in generated_text
        assert "Cardiac Intervention Implant Showcase" in generated_text
        assert "心内介植入相关产品" in generated_text
        assert "cardiac interventional implant products" in generated_text
        assert "医疗标准件展柜" in generated_text
        assert "Medical Standard Components Showcase" in generated_text
        assert "医疗器械标准件与基础组件" in generated_text
        assert "standard medical device components" in generated_text
        assert "心内介植入展厅" not in generated_text
        assert "医疗标准件展厅" not in generated_text
        assert f"-- Empty description products: {NORMALIZED_PRODUCT_CODE}:一次性使用射频房间隔穿刺针" in generated_text
        assert f"(26, '{NORMALIZED_PRODUCT_CODE}'" in generated_text
    finally:
        if temp_sql.exists():
            temp_sql.unlink()
