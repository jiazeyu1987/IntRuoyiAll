from pathlib import Path


def test_auto_schedule_demo_ps1_defaults_to_active_backend_port() -> None:
    script_path = Path(__file__).resolve().parents[1] / "shell" / "mes-auto-schedule-first-loop-demo.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "[string]$BaseUrl = 'http://127.0.0.1:48081/admin-api'" in text, (
        "demo replay script should default to the active local backend port 48081"
    )
    assert "Resolve-PreviewStartTime" in text, (
        "demo replay script should derive preview start time dynamically instead of relying only on a stale hard-coded timestamp"
    )
    assert "[string]$DbHost = '127.0.0.1'" in text
    assert "[int]$DbPort = 23306" in text
    assert "Invoke-MySqlViaPython" in text, (
        "demo replay script should support direct TCP MySQL access when docker is unavailable"
    )


def test_auto_schedule_demo_seed_sql_uses_dynamic_simulation_date() -> None:
    sql_path = (
        Path(__file__).resolve().parents[2] / "sql" / "mysql" / "mes-auto-schedule-first-loop-demo-data.sql"
    )
    text = sql_path.read_text(encoding="utf-8")

    assert "SET @sim_date = (" in text, "seed SQL should derive the schedule date from the current simulation date"
    assert "date_shift_mode_by_date_json" in text, (
        "seed SQL should update the calendar rule override so weekend simulation dates still expose a schedulable day"
    )
    assert "@sim_date_start" in text, "seed SQL should reuse a computed datetime value instead of hard-coding a historical date"


def test_auto_schedule_demo_ps1_supports_complete_scenario() -> None:
    script_path = Path(__file__).resolve().parents[1] / "shell" / "mes-auto-schedule-first-loop-demo.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "[ValidateSet('Minimal', 'Complete')]" in text or "[ValidateSet(\"Minimal\", \"Complete\")]" in text, (
        "demo replay script should expose a complete scenario switch"
    )
    assert "mes-auto-schedule-complete-demo-data.sql" in text, (
        "demo replay script should know how to load the complete scenario seed SQL"
    )
    assert "mes-auto-schedule-complete-demo-clean.sql" in text, (
        "demo replay script should know how to clean the complete scenario seed SQL"
    )


def test_complete_demo_seed_sql_contains_multi_work_order_multi_line_data() -> None:
    sql_path = (
        Path(__file__).resolve().parents[2] / "sql" / "mysql" / "mes-auto-schedule-complete-demo-data.sql"
    )
    assert sql_path.exists(), "complete demo SQL should exist"
    text = sql_path.read_text(encoding="utf-8")

    assert "AUTO-WO-002" in text, "complete demo seed should include a second work order"
    assert "AUTO-PROC-02" in text, "complete demo seed should include a second process"
    assert "AUTO-LINE-02" in text, "complete demo seed should include a second production line"
    assert "AUTO-WS-02" in text, "complete demo seed should include a second workstation"
    assert "@sim_date_plus_1" in text, "complete demo seed should include next-day capacity for cross-day scheduling"


def test_minimal_demo_cleanup_clears_complete_scenario_owned_ids() -> None:
    sql_path = (
        Path(__file__).resolve().parents[2] / "sql" / "mysql" / "mes-auto-schedule-first-loop-demo-clean.sql"
    )
    text = sql_path.read_text(encoding="utf-8")

    assert "900082" in text, "minimal cleanup should remove the complete scenario's second work order too"
    assert "900023" in text, "minimal cleanup should remove the complete scenario's second route process too"
    assert "900051" in text, "minimal cleanup should remove the complete scenario's second workstation too"


def test_auto_schedule_demo_ps1_supports_explicit_simulation_date_sync() -> None:
    script_path = Path(__file__).resolve().parents[1] / "shell" / "mes-auto-schedule-first-loop-demo.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "[string]$SimulationDate = ''" in text, (
        "demo replay script should accept an explicit simulation date parameter"
    )
    assert "Sync-SimulationDateIfRequested" in text, (
        "demo replay script should synchronize the simulation date when the caller requests one"
    )
    assert "mes_pro_schedule_calendar_simulation" in text, (
        "demo replay script should update the schedule calendar simulation table instead of requiring manual SQL"
    )


def test_auto_schedule_demo_ps1_supports_standalone_simulation_date_sync_action() -> None:
    script_path = Path(__file__).resolve().parents[1] / "shell" / "mes-auto-schedule-first-loop-demo.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "'SyncSimulationDate'" in text, (
        "demo replay script should expose a standalone simulation-date sync action"
    )
    assert "Simulation date synced." in text, (
        "standalone simulation-date sync should produce an explicit success message"
    )
