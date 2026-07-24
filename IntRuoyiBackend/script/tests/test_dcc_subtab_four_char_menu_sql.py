from pathlib import Path


WORKSPACE_ROOT = Path(__file__).resolve().parents[3]

SQL_FILES = [
    Path("ruoyi-vue-pro/sql/mysql/20260513_dcc_base_schema.sql"),
    Path("ruoyi-vue-pro/sql/mysql/20260516_dcc_training_closed_loop_menu.sql"),
    Path("ruoyi-vue-pro/sql/mysql/20260527_dcc_approval_print_template.sql"),
    Path("ruoyi-vue-pro/sql/mysql/20260529_dcc_audit_menu_permission.sql"),
    Path("ruoyi-vue-pro/sql/mysql/20260529_dcc_training_mine_menu_restore.sql"),
    Path("ruoyi-vue-pro/sql/mysql/20260629_dcc_subtab_four_char_rename.sql"),
    Path("ruoyi-vue-pro/sql/mysql/20260714_dcc_personal_file_decommission.sql"),
    Path("ruoyi-vue-pro/sql/mysql/20260714_dcc_route_upload_approval_rename.sql"),
    Path("ruoyi-vue-pro/sql/mysql/20260722_dcc_upload_browser_label_rename.sql"),
]

EXPECTED_NAMES = [
    "文档目录",
    "文控权限",
    "上传审批",
    "文件上传",
    "受控浏览",
    "我的培训",
    "模板配置",
    "文件审计",
]

REMOVED_NAMES = [
    "DCC目录管理",
    "DCC审批路线",
    "流程路线",
    "文件提交",
    "文件查阅",
    "DCC受控上传",
    "DCC受控浏览",
    "个人文件",
    "DCC我的文件",
    "DCC我的培训",
    "DCC审批打印模板",
    "DCC受控文件审计",
]


def read_text(path: Path) -> str:
    return (WORKSPACE_ROOT / path).read_text(encoding="utf-8")


def test_dcc_menu_sql_uses_four_char_names():
    merged = "\n".join(read_text(path) for path in SQL_FILES)

    for name in EXPECTED_NAMES:
      assert name in merged, f"DCC 菜单 SQL 必须包含新的四字名称：{name}"

    for name in REMOVED_NAMES:
      assert name not in merged, f"DCC 菜单 SQL 不应继续保留旧名称：{name}"

    assert len(set(EXPECTED_NAMES)) == len(EXPECTED_NAMES), "DCC 菜单四字名称必须互不重名"


def test_dcc_upload_browser_label_rename_sql_targets_stable_menu_keys():
    sql = read_text(Path("ruoyi-vue-pro/sql/mysql/20260722_dcc_upload_browser_label_rename.sql"))
    first_line = sql.splitlines()[0]

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=menu; riskLevel=low"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "`id` = 6806" in sql
    assert "`path` = 'controlled-file/upload'" in sql
    assert "`permission` = 'dcc:controlled-file:submit'" in sql
    assert "`id` = 6807" in sql
    assert "`path` = 'controlled-file/browser'" in sql
    assert "`permission` = 'dcc:controlled-file:query'" in sql
