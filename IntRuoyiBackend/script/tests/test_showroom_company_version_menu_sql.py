from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_showroom_menu_seed_includes_company_version_between_company_and_product() -> None:
    seed_path = REPO_ROOT / "sql" / "showroom" / "20260519_showroom_menu_seed.sql"
    text = seed_path.read_text(encoding="utf-8")

    company_row = "(980101, '公司信息'"
    company_version_row = "(980118, '公司版本'"
    product_row = "(980102, '产品管理'"

    assert company_row in text
    assert company_version_row in text
    assert product_row in text
    assert text.index(company_row) < text.index(company_version_row) < text.index(product_row)
    assert "'company-version'" in text
    assert "'ShowroomAdminCompanyVersion'" in text


def test_runtime_menu_visibility_patch_copies_company_role_bindings() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260524_showroom_company_version_menu_visibility.sql"
    text = sql_path.read_text(encoding="utf-8")

    required_snippets = [
        "980118",
        "公司版本",
        "company-version",
        "ShowroomAdminCompanyVersion",
        "WHERE `source`.`menu_id` = 980101",
        "`system_role_menu`",
        "`tenant_id`",
        "NOT EXISTS",
        "showroom-menu-fix",
    ]

    for snippet in required_snippets:
        assert snippet in text
