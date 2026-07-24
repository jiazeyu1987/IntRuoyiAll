from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql/mysql/20260717_dcc_applicant_rework_bpmn_contract.sql"


def test_dcc_applicant_rework_bpmn_migration_exists_and_is_release_scanned() -> None:
    assert MIGRATION.exists(), "missing DCC applicant rework BPMN migration"
    sql = MIGRATION.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260518_dcc_approval_task_name_fix; type=data; riskLevel=medium\n"
    )
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "dcc-controlled-file-approval process definition is missing" in sql


def test_dcc_applicant_rework_bpmn_has_return_node_and_original_flow_resume() -> None:
    sql = MIGRATION.read_text(encoding="utf-8")

    assert "<userTask id=\"APPLICANT_REWORK\" name=\"申请人修改\">" in sql
    assert "<flowable:candidateStrategy>36</flowable:candidateStrategy>" in sql
    assert "<flowable:approveMethod>1</flowable:approveMethod>" in sql
    assert (
        "<sequenceFlow id=\"flow_applicant_rework_doc_control_review\" "
        "sourceRef=\"APPLICANT_REWORK\" targetRef=\"DOC_CONTROL_REVIEW\" />"
    ) in sql
    assert "sourceRef=\"startEvent\" targetRef=\"DOC_CONTROL_REVIEW\"" in sql
    assert "sourceRef=\"DOC_CONTROL_APPROVAL\" targetRef=\"endEvent\"" in sql


def test_dcc_applicant_rework_bpmn_updates_runtime_model_and_deployment_bytes_only() -> None:
    sql = MIGRATION.read_text(encoding="utf-8")

    assert "UPDATE ACT_GE_BYTEARRAY b\nJOIN ACT_RE_MODEL m" in sql
    assert "m.KEY_ = 'dcc-controlled-file-approval'" in sql
    assert "UPDATE ACT_GE_BYTEARRAY b\nJOIN ACT_RE_PROCDEF d" in sql
    assert "d.KEY_ = 'dcc-controlled-file-approval'" in sql
    assert "SET b.BYTES_ = CONVERT(@dcc_controlled_file_approval_bpmn USING BINARY)" in sql
    assert "dcc_category_approval_matrix" not in sql
    assert "dcc_category_approval_route" not in sql
    assert "DROP TABLE" not in sql.upper()
    assert "DELETE FROM" not in sql.upper()
