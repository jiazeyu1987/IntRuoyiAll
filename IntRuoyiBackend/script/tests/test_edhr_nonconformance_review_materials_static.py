from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICE = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java"
REQ = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrNonconformanceReviewDisposeReqVO.java"
PAGE = ROOT.parent / "IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue"


def test_backend_dispose_uses_material_list_not_single_unique_url():
    service = SERVICE.read_text(encoding="utf-8")
    req = REQ.read_text(encoding="utf-8")

    assert "private List<ReviewMaterialReqVO> reviewMaterials" in req
    assert "private List<ReviewMaterialEventReqVO> reviewMaterialEvents" in req
    assert "resolveReviewMaterials(" in service
    assert "reviewMaterialEvents" in service
    assert "files.size() != 1" not in service
    assert "orderByDesc(FileDO::getId)" in service
    assert "reviewMaterialsJson" in service


def test_frontend_tracks_upload_delete_and_submits_material_list():
    page = PAGE.read_text(encoding="utf-8")

    assert "reviewMaterialUrls" in page
    assert ':limit="5"' in page
    assert "reviewMaterialEvents" in page
    assert "buildReviewMaterials" in page
    assert "reviewMaterials:" in page
    assert "reviewMaterialEvents:" in page
    assert "reviewMaterialUrl: disposeForm.reviewMaterialUrl," not in page
