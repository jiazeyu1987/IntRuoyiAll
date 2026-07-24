from pathlib import Path


DOC = Path("docs/recovery/current-dr-readiness.md")


def test_current_dr_readiness_doc_records_latest_blocked_state_and_evidence():
    text = DOC.read_text(encoding="utf-8")

    required_sections = [
        "Scope",
        "Inventory",
        "Backup",
        "Retention",
        "RTO",
        "RPO",
        "Restore",
        "Verification",
        "Blockers",
        "Owners And Next Actions",
    ]
    for section in required_sections:
        assert f"## {section}" in text

    required_markers = [
        "Current Gate: BLOCKED",
        "c26b3067a4",
        "693530e6da",
        "be63394046",
        "rollback-app",
        "promote-backup",
        "with-data",
        "DCC",
        "releasePackageA",
        "recoverySetA",
        "RTO is not formally defined",
        "RPO depends on the selected tested recovery set",
        "must not use old 2026-05-24 evidence as current readiness",
    ]
    for marker in required_markers:
        assert marker in text
