from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260612_mes_edhr_final_inspection_dossier.sql"
OQC_FINISH_PERMISSION_SQL_PATH = (
    REPO_ROOT / "sql" / "mysql" / "20260612_mes_qc_oqc_finish_permission.sql"
)
DO_PATH = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "dal"
    / "dataobject"
    / "pro"
    / "batchrecord"
    / "MesProEdhrBatchDossierItemDO.java"
)
MAPPER_PATH = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "dal"
    / "mysql"
    / "pro"
    / "batchrecord"
    / "MesProEdhrBatchDossierItemMapper.java"
)
SERVICE_IMPL_PATH = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "service"
    / "pro"
    / "batchrecord"
    / "MesProEdhrBatchExecutionServiceImpl.java"
)
DOSSIER_CONSTANTS_PATH = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "enums"
    / "pro"
    / "MesProEdhrDossierConstants.java"
)


def read_text(path: Path) -> str:
    assert path.exists(), f"{path.relative_to(REPO_ROOT)} 必须存在。"
    return path.read_text(encoding="utf-8")


def test_final_inspection_dossier_migration_declares_batch_dossier_contract() -> None:
    text = read_text(SQL_PATH)
    normalized = " ".join(text.lower().split())
    upper_text = text.upper()

    assert "create table if not exists `mes_pro_edhr_batch_dossier_item`" in normalized
    for column in [
        "`batch_execution_id` bigint not null",
        "`item_type` varchar(32) not null",
        "`item_key` varchar(64) not null",
        "`item_name` varchar(128) not null",
        "`required_flag` bit(1) not null default b'1'",
        "`item_status` varchar(32) not null",
        "`source_doc_type` varchar(32) default null",
        "`source_doc_id` bigint default null",
        "`source_doc_code` varchar(128) default null",
        "`source_doc_status` varchar(64) default null",
        "`source_doc_result` varchar(64) default null",
        "`source_doc_hash` char(64) default null",
        "`completed_at` datetime default null",
        "`verified_at` datetime default null",
        "`blocker_code` varchar(128) default null",
        "`blocker_message` varchar(512) default null",
        "`tenant_id` bigint not null default 0",
    ]:
        assert column in normalized

    for index in [
        "unique key `uk_mes_pro_edhr_batch_dossier_item`",
        "key `idx_mes_pro_edhr_batch_dossier_batch`",
        "key `idx_mes_pro_edhr_batch_dossier_source`",
    ]:
        assert index in normalized

    assert "mes_pro_edhr_batch_execution" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "DROP TABLE" not in upper_text
    assert "TRUNCATE TABLE" not in upper_text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
    assert "INSERT IGNORE" not in upper_text


def test_final_inspection_dossier_backend_model_and_gate_contract() -> None:
    do_text = read_text(DO_PATH)
    mapper_text = read_text(MAPPER_PATH)
    service_text = read_text(SERVICE_IMPL_PATH)
    constants_text = read_text(DOSSIER_CONSTANTS_PATH)

    for field in [
        "private Long batchExecutionId;",
        "private String itemType;",
        "private String itemKey;",
        "private String itemName;",
        "private Boolean requiredFlag;",
        "private String itemStatus;",
        "private String sourceDocType;",
        "private Long sourceDocId;",
        "private String sourceDocCode;",
        "private String sourceDocStatus;",
        "private String sourceDocResult;",
        "private String sourceDocHash;",
        "private LocalDateTime completedAt;",
        "private LocalDateTime verifiedAt;",
        "private String blockerCode;",
        "private String blockerMessage;",
        "private Long tenantId;",
    ]:
        assert field in do_text

    assert "selectListByBatchExecutionId" in mapper_text
    assert "selectRequiredFinalInspection" in mapper_text
    assert 'ITEM_TYPE_FINAL_INSPECTION = "FINAL_INSPECTION"' in constants_text
    assert 'ITEM_STATUS_COMPLETED = "COMPLETED"' in constants_text
    assert "MesProEdhrDossierConstants.ITEM_TYPE_FINAL_INSPECTION" in service_text
    assert "MesProEdhrDossierConstants.ITEM_STATUS_COMPLETED" in service_text
    assert "createDefaultDossierItems" in service_text
    assert "collectFinalInspectionDossierBlockers" in service_text
    assert "toArchiveDossierItemManifest" in service_text


def test_oqc_finish_permission_migration_declares_required_button_permission() -> None:
    text = read_text(OQC_FINISH_PERMISSION_SQL_PATH)
    normalized = " ".join(text.lower().split())
    upper_text = text.upper()

    assert "mes:qc-oqc:finish" in text
    assert "mes:qc-oqc:update" in text
    assert "insert into system_menu" in normalized
    assert "insert into system_role_menu" in normalized
    assert "signal sqlstate '45000'" in normalized
    assert "DROP TABLE" not in upper_text
    assert "TRUNCATE TABLE" not in upper_text
