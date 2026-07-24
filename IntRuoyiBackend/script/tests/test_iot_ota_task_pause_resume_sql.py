from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL = (REPO_ROOT / "sql/mysql/ruoyi-vue-pro.sql").read_text(encoding="utf-8")


def test_iot_ota_task_paused_status_seed_exists() -> None:
    assert "'iot_ota_task_status'" in SQL
    assert "(3603, 40, '已暂停', '40', 'iot_ota_task_status', 0, 'warning'" in SQL


def test_iot_ota_task_pause_resume_permissions_exist() -> None:
    assert "'OTA 升级任务暂停'" in SQL
    assert "'iot:ota-task:pause'" in SQL
    assert "'OTA 升级任务继续'" in SQL
    assert "'iot:ota-task:resume'" in SQL
    assert "900184" in SQL
    assert "900185" in SQL
