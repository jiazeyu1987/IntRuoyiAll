from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
MES_JAVA = ROOT / "IntRuoyiBackend" / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes"
SQL = ROOT / "IntRuoyiBackend" / "sql" / "mysql"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_nonconformance_review_has_formal_active_order_identity():
    migration = read(SQL / "20260920_mes_edhr_nonconformance_review_active_order_fact.sql")
    data_object = read(MES_JAVA / "dal/dataobject/pro/batchrecord/MesProEdhrNonconformanceReviewDO.java")
    response = read(MES_JAVA / "controller/admin/pro/batchrecord/vo/MesProEdhrNonconformanceReviewRespVO.java")

    assert "`active_order_id` bigint" in migration
    assert "idx_mes_edhr_ncr_active_order" in migration
    assert "private Long activeOrderId;" in data_object
    assert "private Long activeOrderId;" in response


def test_nonconformance_review_mapper_reads_by_active_order():
    mapper = read(MES_JAVA / "dal/mysql/pro/batchrecord/MesProEdhrNonconformanceReviewMapper.java")

    assert "selectListByActiveOrderId(Long activeOrderId)" in mapper
    assert "MesProEdhrNonconformanceReviewDO::getActiveOrderId" in mapper
    assert "selectListByBatchExecutionId(Long batchExecutionId)" in mapper


def test_domain_trace_reads_ncr_by_active_order_not_only_batch_execution():
    service = read(MES_JAVA / "service/pro/batchrecord/MesProBatchRecordDomainTraceServiceImpl.java")
    method_start = service.index("private List<MesProEdhrNonconformanceReviewDO> selectNonconformanceReviews(")
    method_end = service.index("private String buildNonconformanceReviewTraceSnapshotJson", method_start)
    method_body = service[method_start:method_end]

    assert "resolveUniqueActiveOrderId(execution)" in method_body
    assert "selectListByActiveOrderId" in method_body
    assert "return nonconformanceReviewMapper.selectListByBatchExecutionId" not in method_body


def test_team_leader_review_signatures_are_scoped_to_process_pool_event():
    signature_service = read(MES_JAVA / "service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java")
    allocation_service = read(MES_JAVA / "service/pro/processpool/team/MesReportAllocationCommandService.java")
    submission_review_service = read(MES_JAVA / "service/pro/processpool/team/MesTeamLeaderSubmissionReviewServiceImpl.java")
    report_confirmation_service = read(MES_JAVA / "service/pro/processpool/team/MesTeamLeaderReportConfirmationServiceImpl.java")

    assert "recordTeamLeaderReviewSignature(Long actorId, String password, String comment," in signature_service
    assert "reviewSourceType, reviewSourceId, reviewSourceName" in signature_service
    assert '"PROCESS_POOL_EVENT", event.getId(), "生产报工组长复核"' in allocation_service
    assert '"PROCESS_POOL_EVENT", event.getId(), "生产报工组长驳回"' in allocation_service
    assert '"PROCESS_POOL_EVENT", event.getId(), "提交记录组长复核"' in submission_review_service
    assert '"PROCESS_POOL_EVENT", event.getId(), "报工确认组长复核"' in report_confirmation_service


def test_active_order_operation_fact_exposes_nonconformance_evidence():
    detail_model = read(
        MES_JAVA
        / "service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java"
    )
    detail_response = read(
        MES_JAVA
        / "controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java"
    )
    detail_service = read(
        MES_JAVA
        / "service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java"
    )
    review_service = read(
        MES_JAVA
        / "service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java"
    )

    for source in (detail_model, detail_response):
        assert "private String nonconformanceReason;" in source
        assert "private String reviewMaterialUrl;" in source
        assert "private Long reviewMaterialFileId;" in source
        assert "private String reviewOpinion;" in source
        assert "private String disposition;" in source
        assert "private String qaSignature;" in source
        assert "private Long qaUserId;" in source

    for key in (
        "nonconformanceReason",
        "reviewMaterialUrl",
        "reviewMaterialFileId",
        "reviewOpinion",
        "disposition",
        "qaSignature",
        "qaUserId",
        "signatureId",
    ):
        assert f'"{key}"' in review_service
        assert (
            f'metadata.getString("{key}")' in detail_service
            or (key == "reviewMaterialFileId" and 'metadata.getLong("reviewMaterialFileId")' in detail_service)
            or (key == "qaUserId" and 'metadata.getLong("qaUserId")' in detail_service)
            or (key == "signatureId" and "signatureIdOf(metadata)" in detail_service)
        )


def test_nonconformance_review_material_uses_protected_online_preview_file_id():
    migration = read(SQL / "20260921_mes_edhr_nonconformance_review_material_preview.sql")
    data_object = read(MES_JAVA / "dal/dataobject/pro/batchrecord/MesProEdhrNonconformanceReviewDO.java")
    response = read(MES_JAVA / "controller/admin/pro/batchrecord/vo/MesProEdhrNonconformanceReviewRespVO.java")
    mapper = read(MES_JAVA / "dal/mysql/pro/batchrecord/MesProEdhrNonconformanceReviewMapper.java")
    review_service = read(MES_JAVA / "service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java")
    provider = read(
        MES_JAVA
        / "service/pro/batchrecord/MesEdhrNonconformanceReviewMaterialBusinessFileAccessProvider.java"
    )

    assert "`review_material_file_id` bigint" in migration
    assert "idx_mes_edhr_ncr_review_material_file" in migration
    assert "private Long reviewMaterialFileId;" in data_object
    assert "private Long reviewMaterialFileId;" in response
    assert "selectListByReviewMaterialFileId(Long fileId)" in mapper
    assert "resolveReviewMaterials(reqVO.getReviewMaterials()" in review_service
    assert "reviewMaterials.primaryFileId()" in review_service
    assert ".setReviewMaterialFileId(reviewMaterialFileId)" in review_service
    assert 'metadata.put("reviewMaterialFileId", reviewMaterialFileId)' in review_service
    assert 'snapshot.put("reviewMaterialFileId", update.getReviewMaterialFileId())' in review_service
    assert "BusinessFileAccessProvider" in provider
    assert 'BUSINESS_TYPE = "MES_EDHR_NONCONFORMANCE_REVIEW_MATERIAL"' in provider
    assert "BusinessFileAccessOperation.PREVIEW" in provider
    assert "BusinessFileAccessOperation.ONLYOFFICE_PREVIEW" in provider


def test_nonconformance_review_exposes_active_order_detail_read_model():
    controller = read(MES_JAVA / "controller/admin/pro/batchrecord/MesProEdhrNonconformanceReviewController.java")
    service = read(MES_JAVA / "service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java")
    api = read(ROOT / "IntRuoyiFronted" / "src" / "api" / "mes" / "pro" / "edhr" / "nonconformanceReview.ts")

    assert '@GetMapping("/active-order-detail")' in controller
    assert "mes:pro-edhr-nonconformance-review:query" in controller
    assert "toActiveOrderDetailRespVO" in controller
    assert "nonconformanceReviewService.getActiveOrderDetail(reviewId)" in controller
    assert "MesTeamLeaderActiveOrderDetailService" in service
    assert "Long activeOrderId = requireActiveOrderId(review.getActiveOrderId())" in service
    assert "activeOrderDetailService.getFormalDetail(activeOrderId)" in service
    assert "getNonconformanceReviewActiveOrderDetail" in api
    assert "`${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/active-order-detail`" in api


def test_active_order_rework_release_application_does_not_lock_next_p3():
    release_application_mapper = read(
        MES_JAVA / "dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderReleaseApplicationMapper.java"
    )
    active_order_service = read(
        MES_JAVA / "service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java"
    )
    generation_service = read(
        MES_JAVA / "service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseGenerationService.java"
    )

    assert "isReworkClosedReleaseApplication" in active_order_service
    assert "NONCONFORMANCE_REWORK" in active_order_service
    assert "reworkClosedActiveOrderIds" in active_order_service
    assert "selectReworkClosedApplicationIds" in active_order_service
    assert "reworkClosedApplicationIds.contains(application.getId())" in active_order_service
    assert "!reworkClosedActiveOrderIds.contains(application.getActiveOrderId())" in active_order_service
    assert "latestReworkCycleId" in generation_service
    assert "selectLatestReworkClosedByActiveOrderId" in generation_service
    assert "requestExisting != null && !isReworkClosedReleaseApplication(requestExisting)" in generation_service
    assert "isReworkClosedReleaseApplication(businessExisting) ? null : businessExisting" in generation_service
    assert "mes_pro_edhr_nonconformance_review" in release_application_mapper
    assert "r.review_status = 'closed' AND r.disposition = 'rework'" in release_application_mapper
    assert "source_type = 'PQC_RELEASE'" in release_application_mapper


def test_pqc_release_nonconformance_review_uses_release_application_identity():
    page = read(
        ROOT
        / "IntRuoyiFronted"
        / "src"
        / "views"
        / "mes"
        / "pro"
        / "production-release"
        / "PqcProductionReleasePage.vue"
    )
    service = read(
        MES_JAVA / "service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java"
    )

    start = page.index("const openNonconformanceReview =")
    end = page.index("\n\nconst hasActiveOrderDetail", start)
    block = page[start:end]

    assert "sourceType: SOURCE_TYPE_PQC_RELEASE" in block
    assert "sourceId: row.applicationId" in block
    assert "batchExecutionId" not in block
    assert "SOURCE_TYPE_PQC_RELEASE.equals(sourceType)" in service
    assert "validatePqcReleaseReviewBatchExecutionId" in service


def test_pqc_release_nonconformance_entry_filters_pending_reviews_by_current_application():
    page = read(
        ROOT
        / "IntRuoyiFronted"
        / "src"
        / "views"
        / "mes"
        / "pro"
        / "edhr-nonconformance"
        / "NonconformanceReviewPage.vue"
    )
    api = read(
        ROOT
        / "IntRuoyiFronted"
        / "src"
        / "api"
        / "mes"
        / "pro"
        / "edhr"
        / "nonconformanceReview.ts"
    )
    req_vo = read(
        MES_JAVA
        / "controller/admin/pro/batchrecord/vo/MesProEdhrNonconformanceReviewPageReqVO.java"
    )
    mapper = read(MES_JAVA / "dal/mysql/pro/batchrecord/MesProEdhrNonconformanceReviewMapper.java")

    assert "sourceId?: EdhrRouteId" in api
    assert "private Long sourceId;" in req_vo
    assert "MesProEdhrNonconformanceReviewDO::getSourceId, reqVO.getSourceId()" in mapper
    assert "const buildPendingReviewQuery = ()" in page
    assert "query.sourceType = entryForm.sourceType" in page
    assert "query.sourceId = entrySourceId.value" in page
    assert "getPendingNonconformanceReviewPage(buildPendingReviewQuery())" in page
    assert "selectedReview.value = undefined" in page
