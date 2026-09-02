from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_product_catalog_reads_bound_project_code_recognition_json():
    service = (
        ROOT
        / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/productcatalog/DccProductCatalogServiceImpl.java"
    ).read_text(encoding="utf-8")
    resp_vo = (
        ROOT
        / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/productcatalog/vo/DccProductCatalogRespVO.java"
    ).read_text(encoding="utf-8")
    api = (
        ROOT / "../IntRuoyiFronted/src/api/dcc/controlledFile/productCatalog.ts"
    ).read_text(encoding="utf-8")
    vue = (
        ROOT
        / "../IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue"
    ).read_text(encoding="utf-8")

    assert "private DccProjectCodeMapper projectCodeMapper;" in service
    assert "enrichBatchRecordTotalRecognitionJson" in service
    assert "setBatchRecordTotalRecognitionJson" in service
    assert "getBatchRecordTotalRecognitionJson()" in service
    assert "private String batchRecordTotalRecognitionJson;" in resp_vo
    assert "batchRecordTotalRecognitionJson?: string | null" in api
    assert "dcc-product-catalog-copy-recognition-json" in vue
