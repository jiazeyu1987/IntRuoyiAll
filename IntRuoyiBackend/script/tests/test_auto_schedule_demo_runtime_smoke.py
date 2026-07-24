import subprocess
from pathlib import Path

import pymysql


REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPT_PATH = REPO_ROOT / "script" / "shell" / "mes-auto-schedule-first-loop-demo.ps1"


def test_complete_demo_can_resync_simulation_date_without_manual_sql() -> None:
    _set_simulation_date("2026-05-16")

    sync = subprocess.run(
        [
            "powershell",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(SCRIPT_PATH),
            "-Action",
            "SyncSimulationDate",
            "-SimulationDate",
            "2026-05-17",
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        cwd=REPO_ROOT,
        check=False,
    )
    assert sync.returncode == 0, sync.stderr or sync.stdout
    assert "Simulation date synced." in sync.stdout
    assert _current_simulation_date() == "2026-05-17"

    replay = subprocess.run(
        [
            "powershell",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(SCRIPT_PATH),
            "-Scenario",
            "Complete",
            "-SimulationDate",
            "2026-05-17",
            "-Action",
            "ReplayAndExercise",
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        cwd=REPO_ROOT,
        check=False,
    )
    assert replay.returncode == 0, replay.stderr or replay.stdout
    assert "Preview/apply API flow passed." in replay.stdout
    assert "Formal task count: 4" in replay.stdout
    assert "Dependency count: 2" in replay.stdout
    assert _current_simulation_date() == "2026-05-17"


def _set_simulation_date(date_text: str) -> None:
    conn = pymysql.connect(
        host="127.0.0.1",
        port=23306,
        user="root",
        password="123456",
        database="ruoyi-vue-pro",
        charset="utf8mb4",
        autocommit=True,
    )
    try:
        with conn.cursor() as cur:
            cur.execute(
                "UPDATE mes_pro_schedule_calendar_simulation SET simulation_date = %s WHERE tenant_id = 1 AND deleted = b'0'",
                (f"{date_text} 00:00:00",),
            )
    finally:
        conn.close()


def _current_simulation_date() -> str:
    conn = pymysql.connect(
        host="127.0.0.1",
        port=23306,
        user="root",
        password="123456",
        database="ruoyi-vue-pro",
        charset="utf8mb4",
        autocommit=True,
    )
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT DATE_FORMAT(simulation_date, '%Y-%m-%d') "
                "FROM mes_pro_schedule_calendar_simulation "
                "WHERE tenant_id = 1 AND deleted = b'0' "
                "ORDER BY id DESC LIMIT 1"
            )
            row = cur.fetchone()
            assert row is not None
            return row[0]
    finally:
        conn.close()
