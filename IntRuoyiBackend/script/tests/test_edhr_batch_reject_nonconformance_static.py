from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MES = ROOT / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes"


def read(relative: str) -> str:
    return (MES / relative).read_text(encoding="utf-8")


def test_controller_exposes_permission_guarded_reject_batch_endpoint() -> None:
    controller = read("controller/admin/pro/batchrecord/MesProEdhrNonconformanceReviewController.java")
    assert '@PostMapping("/reject-batch")' in controller
    assert "mes:pro-production-release:pqc-reject" in controller
    assert "MesProEdhrBatchExecutionRejectReqVO" in controller


def test_reject_service_signs_before_review_and_freeze_writes() -> None:
    service = read("service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java")
    reject_method = service[service.index("rejectBatch(MesProEdhrBatchExecutionRejectReqVO reqVO)"):]
    signature_index = reject_method.index("recordBatchActionSignature")
    review_insert_index = reject_method.index("reviewMapper.insert(review)")
    batch_freeze_index = reject_method.index("BATCH_STATUS_FROZEN", review_insert_index)
    assert signature_index < review_insert_index < batch_freeze_index
    assert "ACTION_NONCONFORMANCE_REJECT" in service
    assert "releaseOwnerRejectSignatureId=" in service


def test_batch_execution_list_excludes_pending_nonconformance_review() -> None:
    mapper = read("dal/mysql/pro/batchrecord/MesProEdhrBatchExecutionMapper.java")
    assert "mes_pro_edhr_nonconformance_review" in mapper
    assert "nr.batch_execution_id = mes_pro_edhr_batch_execution.id" in mapper
    assert "nr.review_status = 'pending_review'" in mapper
