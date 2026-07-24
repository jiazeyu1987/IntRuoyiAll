from pathlib import Path


def test_run_dcc_isolated_e2e_powershell_wrapper_delegates_to_task_script() -> None:
    script_path = Path(__file__).resolve().parents[1] / "deploy" / "run-dcc-manual-release-isolated-e2e.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "20260518-dcc-isolated-e2e-ops-hardening" in text
    assert "run_isolated_dcc_manual_release_e2e.ps1" in text
    assert "[switch]$KeepRunning" in text
    assert "Missing delegated isolated E2E runner" in text
    assert "& powershell @argsList" in text


def test_run_dcc_isolated_e2e_bat_wrapper_exposes_menu_and_keep_running_mode() -> None:
    bat_path = Path(__file__).resolve().parents[1] / "deploy" / "run-dcc-manual-release-isolated-e2e.bat"
    text = bat_path.read_text(encoding="utf-8")

    assert 'set "PS1=%SCRIPT_DIR%run-dcc-manual-release-isolated-e2e.ps1"' in text
    assert 'if /i "%~1"=="keep-running"' in text
    assert "1. Run and auto-stop" in text
    assert "2. Run and keep environment running" in text
    assert "3. Cancel" in text
    assert 'powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%" -KeepRunning' in text


def test_stop_dcc_isolated_e2e_wrappers_delegate_to_task_script() -> None:
    ps1_path = Path(__file__).resolve().parents[1] / "deploy" / "stop-dcc-manual-release-isolated-e2e.ps1"
    bat_path = Path(__file__).resolve().parents[1] / "deploy" / "stop-dcc-manual-release-isolated-e2e.bat"
    ps1_text = ps1_path.read_text(encoding="utf-8")
    bat_text = bat_path.read_text(encoding="utf-8")

    assert "20260518-dcc-isolated-e2e-ops-hardening" in ps1_text
    assert "stop_isolated_dcc_manual_release_env.ps1" in ps1_text
    assert 'set "PS1=%SCRIPT_DIR%stop-dcc-manual-release-isolated-e2e.ps1"' in bat_text
    assert 'if /i "%~1"=="cancel"' in bat_text
    assert 'powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%"' in bat_text
