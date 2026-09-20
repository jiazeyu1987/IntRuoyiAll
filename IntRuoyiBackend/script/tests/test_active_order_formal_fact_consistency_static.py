from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
MES = ROOT / "IntRuoyiBackend" / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes"


def read(relative: str) -> str:
    return (MES / relative).read_text(encoding="utf-8")


def test_cross_entry_details_use_formal_projection_without_team_leader_gate():
    interface = read("service/pro/processpool/team/MesTeamLeaderActiveOrderDetailService.java")
    batch_detail = read("service/pro/batchrecord/MesProEdhrBatchActiveOrderDetailService.java")
    pqc_detail = read("service/pro/productionrelease/pqc/MesPqcReleaseOrderDetailService.java")

    assert "getFormalDetail(Long activeOrderId)" in interface
    assert "detailService.getFormalDetail(activeOrderId)" in batch_detail
    assert "detailService.getFormalDetail(application.getActiveOrderId())" in pqc_detail
    assert "detailService.getDetail(activeOrder.getLeaderUserId()" not in batch_detail
    assert "detailService.getDetail(order.getLeaderUserId()" not in pqc_detail


def test_pqc_release_signature_binds_release_application_source():
    signature_service = read("service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java")
    pqc_service = read("service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java")
    pqc_detail = read("service/pro/productionrelease/pqc/MesPqcReleaseOrderDetailService.java")

    assert "recordPqcReleaseSignature(Long actorId, Long executionId, Long releaseApplicationId" in signature_service
    assert '"PQC_RELEASE_APPLICATION", releaseApplicationId' in signature_service
    assert "recordPqcReleaseSignature(\n                actorUserId, batchExecutionId, application.getId()" in pqc_service
    assert '"PQC_RELEASE_APPLICATION", applicationId' in pqc_detail
    assert '"PQC_RELEASE_APPLICATION", null' not in signature_service
    assert '"PQC_RELEASE_APPLICATION", null' not in pqc_detail


def test_pqc_release_approval_does_not_generate_p2_content():
    pqc_service = read("service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java")

    approve_body = pqc_service[pqc_service.index("public MesPqcProductionReleaseDecisionResult approve("):pqc_service.index("@Override", pqc_service.index("public MesPqcProductionReleaseDecisionResult approve(") + 1)]
    assert "requireExistingBatchExecutionId(application)" in approve_body
    assert "dossierPort.write(" not in approve_body
    assert "reportStageInitializer.initializeRequiredReportStage(" not in approve_body
    assert "batchExecutionPort.openOrCreate(" not in approve_body
    assert ".setBatchRecordEvidenceIds(List.of())" in approve_body
    assert ".setProcessInspectionEvidenceIds(List.of())" in approve_body
    assert ".setReportUploadTasks(List.of())" in approve_body


def test_fixed_test_order_reset_clears_temporary_freeze_before_p1():
    service = read("service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java")

    reset_start = service.index("public MesTeamLeaderActiveOrderTestResetResult resetFixedSimulationActiveOrder(")
    reset_end = service.index("@Override", reset_start + 1)
    reset_body = service[reset_start:reset_end]

    assert "workOrderMapper.updateTemporaryFrozenByIds(List.of(workOrder.getId()), Boolean.FALSE)" in reset_body


def test_active_order_operation_facts_use_json_metadata_lookup():
    mapper = read("dal/mysql/pro/batchrecord/MesProEdhrOperationAuditEventMapper.java")
    recorder = read("service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseAuditRecorder.java")
    pqc_service = read("service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java")

    assert "JSON_UNQUOTE(JSON_EXTRACT(metadata_json, '$.activeOrderId')) = {0}" in mapper
    assert '.like(MesProEdhrOperationAuditEventDO::getMetadataJson, "\\"activeOrderId\\":"' not in mapper
    assert 'metadata.put("batchExecutionId", command.getBatchExecutionId())' in recorder
    assert 'metadata.put("signatureId", command.getSignatureId())' in recorder
    assert 'case MesReleaseFlowAuditEventType.PQC_PRODUCTION_RELEASE_APPROVED -> "PQC生产放行"' in recorder
    assert ".setSignatureId(result.getSignatureId())" in pqc_service
