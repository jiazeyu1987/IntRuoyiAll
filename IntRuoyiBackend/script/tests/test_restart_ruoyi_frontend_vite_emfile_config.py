from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
RESTART_SCRIPT = REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"


def test_frontend_restart_uses_windows_safe_vite_settings() -> None:
    script = RESTART_SCRIPT.read_text(encoding="utf-8")
    frontend_start = script.index("function Start-Frontend")
    backend_start = script.index("function Start-Backend")
    frontend_block = script[frontend_start:backend_start]

    optimize_setting = "`$env:VITE_OPTIMIZE_PROFILE = 'windows-safe'"
    threadpool_setting = "`$env:UV_THREADPOOL_SIZE = '1'"
    pnpm_start = "pnpm dev -- --strictPort"

    assert optimize_setting in frontend_block
    assert threadpool_setting in frontend_block
    assert frontend_block.index(optimize_setting) < frontend_block.index(pnpm_start)
    assert frontend_block.index(threadpool_setting) < frontend_block.index(pnpm_start)
