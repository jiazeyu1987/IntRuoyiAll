from __future__ import annotations

from script.e2e.dcc_withdrawn_actions_r05_e2e import run_r05_withdrawn_actions_e2e


def test_r05_withdrawn_delete_and_resubmit_real_frontend_path() -> None:
    result = run_r05_withdrawn_actions_e2e()

    assert "R05-delete-withdrawn-flow" in result["passed"]
    assert "R05-resubmit-withdrawn-flow" in result["passed"]
