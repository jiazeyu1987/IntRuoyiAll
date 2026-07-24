from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_showroom_menu_seed_includes_prompt_between_product_and_hall() -> None:
    seed_path = REPO_ROOT / "sql" / "showroom" / "20260519_showroom_menu_seed.sql"
    text = seed_path.read_text(encoding="utf-8")

    product_row = "(980102, '产品管理'"
    prompt_row = "(980119, '提示管理'"
    hall_row = "(980103, '展厅管理'"

    assert product_row in text
    assert prompt_row in text
    assert hall_row in text
    assert text.index(product_row) < text.index(prompt_row) < text.index(hall_row)
    assert "'prompt'" in text
    assert "'ShowroomAdminPrompt'" in text


def test_runtime_prompt_menu_visibility_patch_copies_product_role_bindings() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260524_showroom_prompt_menu_visibility.sql"
    text = sql_path.read_text(encoding="utf-8")

    required_snippets = [
        "980119",
        "提示管理",
        "prompt",
        "ShowroomAdminPrompt",
        "WHERE `source`.`menu_id` = 980102",
        "`system_role_menu`",
        "`tenant_id`",
        "NOT EXISTS",
        "showroom-menu-fix",
    ]

    for snippet in required_snippets:
        assert snippet in text
