from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "sql" / "mysql" / "20260720_dcc_obsolete_approval_bpm_seed.sql"


def test_dcc_obsolete_approval_bpm_seed_exists_and_is_tenant_scoped() -> None:
    assert SEED.exists(), "missing DCC obsolete approval BPM seed"
    sql = SEED.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy,20260719_dcc_obsolete_form_policy_seed; type=seed; riskLevel=low\n"
    )
    assert "dcc-controlled-file-obsolete-approval" in sql
    assert "DCC受控文件作废审批" in sql
    assert "'1' AS tenant_id_text" in sql
    assert "'122' AS tenant_id_text" in sql
    assert "1 AS tenant_id" in sql
    assert "122 AS tenant_id" in sql
    assert "dcc-obsolete-approval-deploy-tenant-1" in sql
    assert "dcc-obsolete-approval-deploy-tenant-122" in sql
    assert "dcc-controlled-file-obsolete-approval:1:dcc-obsolete-admin" in sql
    assert "dcc-controlled-file-obsolete-approval:1:dcc-obsolete-test" in sql


def test_dcc_obsolete_approval_bpm_is_single_step_start_user_selected() -> None:
    sql = SEED.read_text(encoding="utf-8")

    assert '<process id="dcc-controlled-file-obsolete-approval"' in sql
    assert '<userTask id="DOC_CONTROL_REVIEW" name="文控审核">' in sql
    assert "<flowable:candidateStrategy>35</flowable:candidateStrategy>" in sql
    assert "<flowable:approveMethod>1</flowable:approveMethod>" in sql
    assert 'id="MATRIX_REVIEW"' not in sql
    assert 'id="MATRIX_APPROVAL"' not in sql
    assert 'id="DOC_CONTROL_APPROVAL"' not in sql
    assert "审核会签" not in sql
    assert "批准" not in sql
    assert "flow_doc_control_review_end" in sql


def test_dcc_obsolete_approval_bpm_seed_wires_model_procdef_and_info() -> None:
    sql = SEED.read_text(encoding="utf-8")

    assert "act_re_deployment" in sql
    assert "act_ge_bytearray" in sql
    assert "act_re_model" in sql
    assert "act_re_procdef" in sql
    assert "bpm_process_definition_info" in sql
    assert "bpm_business_approval_policy" in sql
    assert "effect_executor_code = 'DCC_OBSOLETE'" in sql
    assert "process_definition_key = 'dcc-controlled-file-obsolete-approval'" in sql
    assert "bpm_form_action_policy" not in sql
    assert '"formCustomViewPath":"/dcc/controlled-file/detail"' in sql
    assert "HAS_GRAPHICAL_NOTATION_ = 1" in sql
    assert "DROP TABLE" not in sql.upper()
    assert "DELETE FROM" not in sql.upper()
