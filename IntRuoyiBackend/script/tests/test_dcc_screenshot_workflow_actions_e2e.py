from __future__ import annotations

from script.e2e.dcc_screenshot_workflow_actions_e2e import run_t3_workflow_actions_e2e


def test_dcc_screenshot_t3_workflow_actions_and_fourth_node_e2e() -> None:
    result = run_t3_workflow_actions_e2e()

    assert "E2E-06" in result["passed"]
    assert "E2E-08" in result["passed"]
    assert "E2E-09" in result["passed"]
    assert "E2E-10" in result["passed"]
    assert "E2E-07" in result["passed"]
    assert result["blocked"] == []
    assert result["checks"]["trainingGate"]["file"]["status"] == "PENDING_APPLICANT_TRAINING_RECORD"
    assert result["checks"]["trainingUpload"]["file"]["status"] == "PENDING_DOC_CONTROL_APPROVAL"
    assert "培训记录" not in result["checks"]["fourthNode"]["dialogText"]
