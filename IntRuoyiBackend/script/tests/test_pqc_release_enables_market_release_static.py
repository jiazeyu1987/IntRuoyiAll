from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def test_pqc_release_hands_active_order_facts_to_manager_release_transaction():
    service = read(
        "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
        "service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java"
    )
    assert "MesProductionReleaseFormalFactSnapshots" in service
    assert "initializeManagerReleaseStage" in service
    assert "handoffReportsToManager" in service


def test_manager_release_accepts_active_order_fact_snapshot_without_legacy_reports():
    approval = read(
        "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
        "service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceImpl.java"
    )
    assert "MesProductionReleaseFormalFactSnapshots.isActiveOrderFactsSnapshot" in approval
    assert "recomputeActiveOrderFactsSnapshot" in approval


def test_pqc_approval_persists_manager_release_pending_status_for_active_order_facts():
    mapper = read(
        "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
        "dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderReleaseApplicationMapper.java"
    )
    approve_sql_start = mapper.index("int approveFromPending")
    approve_sql = mapper[mapper.rfind("@Update", 0, approve_sql_start):approve_sql_start]
    assert "SET application_status = 'MANAGER_RELEASE_PENDING'" in approve_sql
    assert "AND application_status = 'PQC_RELEASE_PENDING'" in approve_sql


def test_manager_handoff_links_transaction_after_pqc_manager_pending_transition():
    mapper = read(
        "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
        "dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderReleaseApplicationMapper.java"
    )
    handoff_sql_start = mapper.index("int handoffReportsToManager")
    handoff_sql = mapper[mapper.rfind("@Update", 0, handoff_sql_start):handoff_sql_start]
    assert "application_status IN ('REPORT_UPLOAD_PENDING', 'MANAGER_RELEASE_PENDING')" in handoff_sql
