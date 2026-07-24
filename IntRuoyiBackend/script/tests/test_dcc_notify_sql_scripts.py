from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_dcc_notify_template_seed_includes_distribution_training_and_obsolete() -> None:
    seed_path = REPO_ROOT / "sql" / "mysql" / "20260513_dcc_notify_template_seed.sql"
    text = seed_path.read_text(encoding="utf-8")

    required_snippets = [
        "dcc_controlled_file_approved",
        "dcc_controlled_file_rejected",
        "dcc_controlled_file_stamp_failed",
        "dcc_distribution",
        "dcc_training",
        "dcc_obsolete",
        "dcc_task_assigned",
        "dcc_controlled_file_approved",
        "dcc_controlled_file_rejected",
        "dcc_task_timeout",
        "DCC下发通知",
        "DCC培训通知",
        "DCC作废通知",
        "DCC待办通知",
        "DCC待办超时提醒",
        "请及时查阅",
        "请及时完成培训确认",
        "已作废，原因：{reason}",
        "您收到一条 DCC 待办任务",
    ]

    for snippet in required_snippets:
        assert snippet in text

    update_snippets = [
        "UPDATE `system_notify_template`",
        "WHERE `code` = 'dcc_controlled_file_approved'",
        "WHERE `code` = 'dcc_controlled_file_rejected'",
        "WHERE `code` = 'dcc_task_assigned'",
        "WHERE `code` = 'dcc_task_timeout'",
    ]

    for snippet in update_snippets:
        assert snippet in text
