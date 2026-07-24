from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"{relative_path} must exist"
    return path.read_text(encoding="utf-8")


def test_dhr_template_controller_exposes_lifecycle_api_and_permissions() -> None:
    controller = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrDhrTemplateController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-dhr-template")',
        '@GetMapping("/catalog/page")',
        '@PostMapping("/catalog/create")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@PostMapping("/integrity-check")',
        '@PostMapping("/approve")',
        '@PostMapping("/signoff")',
        '@PostMapping("/activate")',
        '@PostMapping("/retire")',
        '@PostMapping("/void")',
        '@GetMapping("/impact/page")',
        "mes:pro-edhr-dhr-template:query",
        "mes:pro-edhr-dhr-template:create",
        "mes:pro-edhr-dhr-template:check",
        "mes:pro-edhr-dhr-template:approve",
        "mes:pro-edhr-dhr-template:signoff",
        "mes:pro-edhr-dhr-template:activate",
        "mes:pro-edhr-dhr-template:retire",
        "mes:pro-edhr-dhr-template:void",
    ]:
        assert fragment in controller


def test_dhr_template_service_blocks_effective_without_binding_review_and_signoff() -> None:
    service_impl = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrDhrTemplateServiceImpl.java"
    )

    for fragment in [
        "REQUIRED_BINDING_TYPES",
        "PRODUCT",
        "ROUTE",
        "PROCESS",
        "BATCH_TYPE",
        "STATUS_PRECHECK_FAILED",
        "STATUS_PENDING_REVIEW",
        "STATUS_APPROVED",
        "STATUS_SIGNOFF_PENDING",
        "STATUS_EFFECTIVE",
        "STATUS_RETIRED",
        "STATUS_OBSOLETE",
        "requireNoIntegrityIssues",
        "requireApproved",
        "requireSignedOff",
        "requireImpactConfirmed",
        "PRO_EDHR_DHR_TEMPLATE_BINDING_REQUIRED",
        "PRO_EDHR_DHR_TEMPLATE_REVIEW_REQUIRED",
        "PRO_EDHR_DHR_TEMPLATE_SIGNOFF_REQUIRED",
        "PRO_EDHR_DHR_TEMPLATE_IMPACT_REQUIRED",
        "signoffEvidenceHash",
        "impactScopeJson",
    ]:
        assert fragment in service_impl

    for forbidden in [
        "catch (Exception",
        "catch (RuntimeException",
        "DEFAULT_SUCCESS",
        "MOCK_SIGNOFF",
        "return true;",
        "setStatus(STATUS_EFFECTIVE);",
    ]:
        assert forbidden not in service_impl


def test_dhr_template_data_objects_mappers_and_java_contract_test_are_present() -> None:
    expected_files = {
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrDhrCatalogDO.java": [
            '@TableName("mes_pro_edhr_dhr_catalog")',
            "private String catalogCode;",
            "private String catalogName;",
            "private Long parentCatalogId;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrDhrTemplateDO.java": [
            '@TableName("mes_pro_edhr_dhr_template")',
            "private String templateCode;",
            "private String reviewStatus;",
            "private String signoffStatus;",
            "private Integer integrityIssueCount;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrDhrTemplateVersionDO.java": [
            '@TableName("mes_pro_edhr_dhr_template_version")',
            "private Long templateId;",
            "private String versionNo;",
            "private String templateSnapshotJson;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrDhrTemplateBindingDO.java": [
            '@TableName("mes_pro_edhr_dhr_template_binding")',
            "private String bindingType;",
            "private String bindingObjectCode;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
        "MesProEdhrDhrTemplateImpactDO.java": [
            '@TableName("mes_pro_edhr_dhr_template_impact")',
            "private String actionType;",
            "private String impactScopeJson;",
            "private Boolean impactConfirmed;",
        ],
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/"
        "MesProEdhrDhrTemplateMapper.java": ["selectByTemplateCode", "selectPage"],
        "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
        "MesProEdhrDhrTemplateLifecycleContractTest.java": [
            "serviceRequiresBindingReviewSignoffAndImpactBeforeLifecycleChanges",
            "controllersExposeDhrTemplateRoutesAndPermissions",
        ],
    }

    for relative_path, fragments in expected_files.items():
        source = read_text(relative_path)
        for fragment in fragments:
            assert fragment in source, f"{relative_path} must contain {fragment}"
