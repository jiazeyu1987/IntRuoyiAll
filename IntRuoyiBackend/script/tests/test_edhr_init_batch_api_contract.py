from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"{relative_path} must exist"
    return path.read_text(encoding="utf-8")


def test_edhr_init_batch_backend_files_exist() -> None:
    for relative_path in [
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrInitBatchController.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrInitBatchService.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrInitBatchServiceImpl.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrInitBatchMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrInitManifestMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrInitIssueMapper.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrInitBatchDO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrInitManifestDO.java",
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrInitIssueDO.java",
    ]:
        read_text(relative_path)


def test_edhr_init_batch_controller_exposes_first_slice_only() -> None:
    source = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrInitBatchController.java"
    )

    for fragment in [
        '@RequestMapping("/mes/pro/edhr-init-batch")',
        '@GetMapping("/page")',
        '@GetMapping("/get")',
        '@PostMapping("/create")',
        '@PostMapping("/upload")',
        '@PostMapping("/precheck")',
        '@GetMapping("/issue/page")',
        "mes:pro-edhr-init-batch:query",
        "mes:pro-edhr-init-batch:create",
        "mes:pro-edhr-init-batch:precheck",
    ]:
        assert fragment in source

    for forbidden in [
        "/commit-import",
        "/rollback-request",
        "/signoff",
    ]:
        assert forbidden not in source


def test_edhr_init_batch_service_contract_is_fail_fast_and_idempotent() -> None:
    source = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrInitBatchServiceImpl.java"
    )

    for fragment in [
        "STATUS_DRAFT",
        "STATUS_PRECHECK_FAILED",
        "STATUS_PRECHECK_PASSED",
        "ISSUE_CODE_MISSING_MANIFEST",
        "ISSUE_LEVEL_BLOCKER",
        "selectByBatchAndHash",
        "closeOpenByBatchId",
        "manifestHash",
        "customerFieldDictionaryConfirmed",
        "tenantAuthorizationConfirmed",
        "backupRestoreEvidenceConfirmed",
    ]:
        assert fragment in source

    for forbidden in [
        "commitImport",
        "rollbackRequest",
        "return true;",
    ]:
        assert forbidden not in source


def test_edhr_init_batch_do_and_vo_declare_traceability_fields() -> None:
    batch_do = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrInitBatchDO.java"
    )
    manifest_do = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrInitManifestDO.java"
    )
    issue_do = read_text(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrInitIssueDO.java"
    )

    for fragment in [
        '@TableName("mes_pro_edhr_init_batch")',
        "targetEnvironment",
        "targetTenantId",
        "dataVersion",
        "ownerUserId",
        "approvalOwnerUserId",
        "blockingIssueCount",
    ]:
        assert fragment in batch_do

    for fragment in [
        '@TableName("mes_pro_edhr_init_manifest")',
        "initBatchId",
        "packageType",
        "manifestHash",
        "checksumJson",
        "manifestJson",
    ]:
        assert fragment in manifest_do

    for fragment in [
        '@TableName("mes_pro_edhr_init_issue")',
        "issueCode",
        "issueLevel",
        "sourceFileName",
        "sourceRowNo",
        "sourceFieldName",
        "responsibleName",
        "impactScopeJson",
    ]:
        assert fragment in issue_do
