from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_repo_text(relative_path: str) -> str:
    return (REPO_ROOT / relative_path).read_text(encoding="utf-8")


def test_dcc_governance_sql_uses_current_distribution_training_labels() -> None:
    sql = read_repo_text("sql/mysql/20260515_dcc_governance_split_menu.sql")
    assert "文件分发" in sql
    assert "培训规则" in sql
    assert "DCC下发" not in sql
    assert "DCC培训" not in sql


def test_dcc_navigation_and_print_template_scripts_use_current_labels() -> None:
    navigation = read_repo_text("script/e2e/dcc_screenshot_navigation_e2e.py")
    assert "文件上传" in navigation
    assert "受控浏览" in navigation
    assert "文件提交" not in navigation
    assert "文件查阅" not in navigation
    assert "个人文件" not in navigation
    assert "DCC受控上传" not in navigation
    assert "DCC受控浏览" not in navigation
    assert "DCC我的文件" not in navigation

    print_template = read_repo_text("script/e2e/dcc_approval_print_template_r12_e2e.py")
    assert "模板配置" in print_template
    assert "DCC审批打印模板" not in print_template
