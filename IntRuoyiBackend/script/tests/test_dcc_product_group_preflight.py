from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_product_group_preflight.py"


def test_preflight_script_is_readonly_and_utf8_safe():
    text = SCRIPT.read_text(encoding="utf-8")

    assert "SELECT JSON_OBJECT" in text
    assert "subprocess.run" in text
    assert "mysql" in text
    assert "UPDATE " not in text.upper()
    assert "DELETE FROM" not in text.upper()
    assert "INSERT INTO" not in text.upper()
    assert "????" not in text


def test_candidate_rows_keep_manual_confirmation_empty():
    import importlib.util

    spec = importlib.util.spec_from_file_location("dcc_product_group_preflight", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)

    rows = module.build_candidate_rows(
        users=[{"tenant_id": 1, "user_id": 200, "username": "dev1", "nickname": "开发一", "dept_id": 136, "dept_name": "新品开发部"}],
        products=[{"tenant_id": 1, "product_master_id": 300, "product_code": "P001", "dcc_product_code": "DCC-P001", "product_name": "产品一", "source": "mdm_product"}],
    )

    assert rows == [{
        "tenant_id": 1,
        "group_code": "",
        "group_name": "",
        "dept_id": 136,
        "dept_name": "新品开发部",
        "user_id": 200,
        "username": "dev1",
        "nickname": "开发一",
        "product_master_id": 300,
        "product_code": "P001",
        "dcc_product_code": "DCC-P001",
        "product_name": "产品一",
        "candidate_source": "mdm_product",
        "manual_confirm": "",
        "confirm_note": "",
    }]


def test_output_fields_match_confirmed_sql_generator_contract():
    import importlib.util

    spec = importlib.util.spec_from_file_location("dcc_product_group_preflight", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)

    assert {"tenant_id", "group_code", "group_name", "dept_id", "user_id", "product_master_id", "manual_confirm"}.issubset(module.OUTPUT_FIELDS)


def test_product_query_accepts_enable_status_used_by_mdm_product():
    text = SCRIPT.read_text(encoding="utf-8")

    assert "p.status = 'ENABLE'" in text
