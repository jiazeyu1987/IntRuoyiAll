from __future__ import annotations

import script.e2e.dcc_screenshot_admin_policy_e2e as admin_policy
from script.e2e.dcc_screenshot_admin_policy_e2e import (
    run_e2e_11_electronic_distribution_receipt,
    run_e2e_11_recipient_add_sign_gap_probe,
    run_e2e_12_paper_distribution_recovery_export_print,
    run_e2e_13_process_export_print_template_probe,
    run_e2e_14_weak_password_policy,
    run_e2e_15_expired_password_login_policy,
    run_e2e_16_external_file_review_probe,
)


def test_t4_fixture_infra_file_insert_writes_matching_minio_object(monkeypatch) -> None:
    calls: list[tuple[str, object]] = []

    def fake_run_mysql(sql: str, *, batch: bool = True) -> str:
        calls.append(("mysql", (sql, batch)))
        return ""

    monkeypatch.setattr(admin_policy, "run_mysql", fake_run_mysql)
    monkeypatch.setattr(
        admin_policy,
        "_write_minio_fixture_object",
        lambda file_name: calls.append(("minio", file_name)),
        raising=False,
    )

    admin_policy._insert_infra_file(123456, "CODEX_E2E_T4_UNIT_1.pdf.original.pdf")

    assert ("minio", "CODEX_E2E_T4_UNIT_1.pdf.original.pdf") in calls
    assert any(
        kind == "mysql"
        and "/codex-e2e/CODEX_E2E_T4_UNIT_1.pdf.original.pdf" in str(payload)
        for kind, payload in calls
    )


def test_e2e_11_electronic_distribution_recipient_acknowledges_and_records_signature() -> None:
    evidence = run_e2e_11_electronic_distribution_receipt()

    assert evidence["recipient_acknowledged"] is True
    assert evidence["ack_comment_recorded"] is True
    assert evidence["signature_recorded"] is True


def test_e2e_11_recipient_add_sign_gap_is_exposed_without_faking_pass() -> None:
    evidence = run_e2e_11_recipient_add_sign_gap_probe()

    assert evidence["recipient_add_sign_visible"] is True


def test_e2e_12_paper_distribution_recovery_exports_and_prints_complete_receipts() -> None:
    evidence = run_e2e_12_paper_distribution_recovery_export_print()

    assert evidence["paper_acknowledged"] is True
    assert evidence["paper_recovered"] is True
    assert evidence["csv_contains_required_fields"] is True
    assert evidence["print_contains_required_fields"] is True


def test_e2e_13_process_export_print_template_blocker_is_exposed() -> None:
    evidence = run_e2e_13_process_export_print_template_probe()

    assert evidence["dcc_fields_visible"] is True
    assert evidence["dcc_process_export_print_actions"] > 0
    assert evidence["bpm_print_template_visible"] is True


def test_e2e_14_weak_password_create_reset_and_profile_change_are_rejected() -> None:
    evidence = run_e2e_14_weak_password_policy()

    assert evidence["register_weak_password_rejected"] is True
    assert evidence["forgot_weak_password_rejected"] is True
    assert evidence["create_weak_password_rejected"] is True
    assert evidence["reset_weak_password_rejected"] is True
    assert evidence["profile_weak_password_rejected"] is True


def test_e2e_15_expired_password_login_is_rejected_before_business_access() -> None:
    evidence = run_e2e_15_expired_password_login_policy()

    assert evidence["login_rejected"] is True
    assert evidence["business_page_not_reached"] is True


def test_e2e_16_external_file_review_blocker_is_exposed() -> None:
    evidence = run_e2e_16_external_file_review_probe()

    assert evidence["external_review_entry_visible"] is True
