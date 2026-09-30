from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def _read(relative: str) -> str:
    path = REPO_ROOT / relative
    assert path.exists(), f"missing file: {relative}"
    return path.read_text(encoding="utf-8")


def test_attachment_schema_and_mapper_contract_present() -> None:
    sql = _read("sql/mysql/20260920_dcc_controlled_file_attachment.sql")
    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260911_dcc_upload_ticket_category_guard; type=schema; riskLevel=medium"
    )
    for token in [
        "CREATE TABLE IF NOT EXISTS `dcc_controlled_file_attachment`",
        "`controlled_file_id` bigint NOT NULL",
        "`storage_file_id` bigint NOT NULL",
        "`original_file_name` varchar(512) NOT NULL",
        "`content_type` varchar(255) DEFAULT NULL",
        "`file_size` bigint DEFAULT NULL",
        "`file_sha256` varchar(64) DEFAULT NULL",
        "`sort_no` int NOT NULL DEFAULT 0",
        "KEY `idx_dcc_file_attachment_file` (`tenant_id`, `controlled_file_id`, `deleted`)",
    ]:
        assert token in sql

    data_object = _read(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/"
        "DccControlledFileAttachmentDO.java"
    )
    mapper = _read(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/"
        "DccControlledFileAttachmentMapper.java"
    )
    assert '@TableName("dcc_controlled_file_attachment")' in data_object
    assert "selectListByControlledFileId" in mapper


def test_attachment_api_contract_is_explicit_and_not_related_file_fallback() -> None:
    submit_req = _read(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/"
        "DccControlledFileSubmitReqVO.java"
    )
    resp_vo = _read(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/"
        "DccControlledFileRespVO.java"
    )
    controller = _read(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/"
        "DccControlledFileController.java"
    )
    policy = _read(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/"
        "DccControlledFileUploadTypePolicy.java"
    )
    workflow = _read(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/"
        "DccControlledFileWorkflowServiceImpl.java"
    )
    query = _read(
        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/"
        "DccControlledFileQueryServiceImpl.java"
    )

    assert "private List<DccControlledFileAttachmentUploadTicketReqVO> attachmentUploadTickets;" in submit_req
    assert "private List<DccControlledFileAttachmentRespVO> attachments;" in resp_vo
    assert 'PURPOSE_ATTACHMENT = "ATTACHMENT"' in policy
    assert "PURPOSE_ATTACHMENT.equals(normalized)" in policy
    assert "bindSubmitAttachments" in workflow
    assert "attachmentService.listAttachments(file.getId())" in query
    assert "/{id:\\d+}/attachments/{attachmentId:\\d+}/preview-metadata" in controller
    assert "/{id:\\d+}/attachments/{attachmentId:\\d+}/preview" in controller
    assert "relatedFiles" in resp_vo
    assert "attachments" in resp_vo
