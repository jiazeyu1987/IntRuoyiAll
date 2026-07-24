from __future__ import annotations

from script.e2e.dcc_screenshot_admin_policy_e2e import (
    run_e2e_12_paper_distribution_recovery_export_print,
)


def test_r11_paper_distribution_registration_recovery_export_and_print() -> None:
    evidence = run_e2e_12_paper_distribution_recovery_export_print()

    assert evidence["paper_acknowledged"] is True
    assert evidence["paper_recovered"] is True
    assert evidence["csv_contains_required_fields"] is True
    assert evidence["print_contains_required_fields"] is True
