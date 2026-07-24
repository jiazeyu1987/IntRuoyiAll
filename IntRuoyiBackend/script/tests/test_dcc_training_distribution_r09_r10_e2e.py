from __future__ import annotations

from script.e2e.dcc_training_distribution_r09_r10_e2e import run_r09_r10_training_distribution_e2e


def test_r09_r10_training_record_gate_and_single_file_distribution_recipients() -> None:
    result = run_r09_r10_training_distribution_e2e()

    assert "R09" in result["passed"]
    assert "R10" in result["passed"]
    assert result["checks"]["trainingGate"]["status"] == "PENDING_APPLICANT_TRAINING_RECORD"
    assert result["checks"]["trainingUpload"]["status"] == "PENDING_DOC_CONTROL_APPROVAL"
    assert result["checks"]["fourthApproval"]["finalFile"]["status"] == "ACTIVE"
    assert result["checks"]["fourthApproval"]["recipient"]["user_id"] == "910204"
    assert result["checks"]["recipientReceipt"]["acknowledgedRecipient"]["acknowledged_at"] not in (
        None,
        "",
        "NULL",
    )
