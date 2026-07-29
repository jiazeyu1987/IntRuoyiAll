from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260726_dcc_codex_test_items_seed.sql"


EXPECTED_CASES = {
    "智能文控受控文件上传审批发布闭环": [
        "上传预览和提交",
        "审批任务流转",
        "受控浏览可见",
        "日志追溯完整",
    ],
    "智能文控受控文件修订版本链闭环": [
        "发起修订",
        "版本链更新",
        "旧版受控收敛",
        "修订日志可追溯",
    ],
    "智能文控作废审批与受控浏览收敛": [
        "发起作废",
        "作废审批完成",
        "浏览与下载收敛",
        "作废日志可追溯",
    ],
    "智能文控受控浏览下载水印与访问日志": [
        "权限过滤正确",
        "预览只读",
        "下载与水印受控",
        "访问日志完整",
    ],
    "智能文控分发培训闭环": [
        "分发任务生成",
        "培训任务生成",
        "签收培训完成",
        "分发培训日志完整",
    ],
    "智能文控项目代码识别分配闭环": [
        "识别任务创建",
        "识别结果可复核",
        "项目代码分配生效",
        "识别审计可追溯",
    ],
}


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def normalized_sql() -> str:
    return read_sql().replace("`", "")


def test_dcc_codex_test_items_seed_declares_release_contract() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260724_system_codex_test_management; type=seed; riskLevel=low\n"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql


def test_dcc_codex_test_items_seed_defines_expected_cases_and_checkpoints() -> None:
    sql = read_sql()

    for case_name, checkpoints in EXPECTED_CASES.items():
        assert case_name in sql
        for checkpoint in checkpoints:
            assert checkpoint in sql

    assert sql.count("智能文控") >= len(EXPECTED_CASES)
    assert "checkpoint_count" in sql
    assert "DCC_CODEX_TEST_ITEMS_SEED_CHECKPOINT_MISSING" in sql


def test_dcc_codex_test_items_seed_matches_test_management_contract() -> None:
    sql = normalized_sql()

    assert "INSERT INTO system_codex_test_case" in sql
    assert "INSERT INTO system_codex_test_checkpoint" in sql
    assert "default_execution_mode" in sql
    assert "project" in sql
    assert "'文控'" in sql
    assert sql.count("'SEQUENTIAL'") >= len(EXPECTED_CASES)
    assert sql.count("b'0'") >= len(EXPECTED_CASES)
    assert "'ENABLE'" in sql
    assert "parallel_safe" in sql
    assert "WHERE NOT EXISTS" in sql


def test_dcc_codex_test_items_seed_temp_tables_match_live_target_collation() -> None:
    sql = normalized_sql().lower()

    for table in (
        "tmp_dcc_codex_test_case_seed",
        "tmp_dcc_codex_test_checkpoint_seed",
    ):
        ddl_start = sql.index(f"create temporary table {table}")
        ddl_end = sql.index(";", ddl_start)
        ddl = sql[ddl_start:ddl_end]

        assert "collate=utf8mb4_0900_ai_ci" in ddl
        assert "utf8mb4_unicode_ci" not in ddl


def test_dcc_codex_test_items_seed_requires_real_paths_and_task_owned_data() -> None:
    sql = read_sql()

    for route in (
        "/dcc/controlled-file/upload",
        "/dcc/controlled-file/approval-tasks",
        "/dcc/controlled-file/browser",
        "/dcc/controlled-file/logs",
        "/dcc/controlled-file/training-mine",
        "/dcc/controlled-file/basic-data",
    ):
        assert route in sql

    assert "任务自有" in sql
    assert "不得使用生产文件" in sql
    assert "不得使用 API-only" in sql
    assert "parallelSafe=false" in sql
